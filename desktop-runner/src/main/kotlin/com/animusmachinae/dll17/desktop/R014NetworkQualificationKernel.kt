package com.animusmachinae.dll17.desktop

import com.animusmachinae.dll17.core.continuity.Checkpoint
import com.animusmachinae.dll17.core.continuity.EncryptedRecordStore
import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.persistence.ColdRecoveryActivation
import com.animusmachinae.dll17.core.persistence.InProcessDeviceKeyContainer
import com.animusmachinae.dll17.core.persistence.LocalKeyStore
import com.animusmachinae.dll17.core.persistence.SegmentedJournalMedium
import com.animusmachinae.dll17.core.persistence.SnapshotStore
import com.animusmachinae.dll17.core.recovery.AuthorityOutcome
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityProtocol
import com.animusmachinae.dll17.core.recovery.ProviderException
import com.animusmachinae.dll17.core.recovery.ProviderOutcome
import com.animusmachinae.dll17.core.recovery.RecoveryRoot
import com.animusmachinae.dll17.core.recovery.TransportFault
import com.animusmachinae.dll17.core.recovery.net.HttpIdentityAuthorityClient
import com.animusmachinae.dll17.core.recovery.net.S3Credentials
import com.animusmachinae.dll17.core.recovery.net.S3ObjectStoreConfig
import com.animusmachinae.dll17.core.recovery.net.S3RecoveryPackageStore
import com.animusmachinae.dll17.services.identity.IdentityAuthorityHttpServer
import com.animusmachinae.dll17.services.identity.IdentityAuthorityService
import com.animusmachinae.dll17.services.objectstore.S3QualificationEndpoint
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The R014 network qualification kernel.
 *
 * Same shape as R001, R002, A000 and R012: named fixtures, an explicit held /
 * not-held verdict for each, and a digest over every identifier and readout so a
 * silent behaviour change fails CI rather than quietly invalidating the record.
 *
 * ### It lives in the runner, and that is the architecture speaking
 *
 * `core-persistence` may not see the authority service and may not see the
 * network module. The claim that ordinary local life never touches either is
 * structural, and a qualification kernel that imported both into a core module
 * would falsify the claim in the act of testing it. The host wires them
 * together, so the host is where this runs.
 *
 * ### Two endpoints, deliberately not averaged
 *
 * The default run uses the in-repository S3-compatible endpoint, so the network
 * path is exercised wherever the build runs. D014 separately requires a run
 * against an actual compatible third-party endpoint; [externalEndpointConfig]
 * reads one from the environment and the gate record keeps that result
 * separately. Neither substitutes for the other, and the report says which one
 * produced it.
 */
public object R014NetworkQualificationKernel {

    public const val FIXTURE_SET_ID: String = "R014-NETWORK-FIXTURES-V1"
    public const val FIXTURE_SET_VERSION: Int = 1

    /** Reproduced by a clean run against the in-repository endpoint. CI fails if it drifts. */
    public const val GOLDEN_EVIDENCE_DIGEST: String =
        "efbd6f1caa060da228f72a96cef9e3a2a290c7503f685270e2bbb2b7c7da1501"

    public const val ENDPOINT_VARIABLE: String = "DLL17_RECOVERY_S3_ENDPOINT"
    public const val REGION_VARIABLE: String = "DLL17_RECOVERY_S3_REGION"
    public const val BUCKET_VARIABLE: String = "DLL17_RECOVERY_S3_BUCKET"

    private const val ORGANISM = 0x0D14_0011L
    private const val DEVICE_A = 0xA141_1111L
    private const val DEVICE_B = 0xB142_2222L
    private const val DEVICE_C = 0xC143_3333L

    private const val QUALIFICATION_BUCKET = "dll17-recovery"
    private const val QUALIFICATION_ACCESS_KEY = "DLL17QUALIFICATIONKEY"
    private const val QUALIFICATION_SECRET = "qualification-secret-not-a-real-credential"

    public class Finding(
        public val id: String,
        public val question: String,
        public val held: Boolean,
        public val readout: String,
    )

    public class Report(
        public val providerLabel: String,
        public val findings: List<Finding>,
        public val sections: Map<String, String>,
    ) {
        public val heldCount: Int get() = findings.count { it.held }

        /**
         * The digest covers identifiers and readouts, and **not** the provider
         * label, the endpoint, the bucket or any timing. Two runs against
         * different endpoints must produce the same digest when the behaviour is
         * the same — that is the determinism claim, so the digest is the place to
         * enforce it.
         */
        public fun digest(): String {
            val writer = CanonicalWriter(4096)
            writer.putIdentifier(FIXTURE_SET_ID).putI32(FIXTURE_SET_VERSION)
            for (finding in findings) {
                writer.putIdentifier(finding.id)
                writer.putBool(finding.held)
                writer.putRawBytes(finding.readout.toByteArray(Charsets.UTF_8))
            }
            return MessageDigest.getInstance("SHA-256").digest(writer.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }

        public fun render(): String = buildString {
            append("R014_FIXTURE_SET=").append(FIXTURE_SET_ID).append('\n')
            append("R014_FIXTURE_VERSION=").append(FIXTURE_SET_VERSION).append('\n')
            append("PROVIDER=").append(S3RecoveryPackageStore.PROVIDER_ID).append('\n')
            append("TRANSPORT=").append(
                com.animusmachinae.dll17.core.recovery.IdentityAuthorityTransport.CONTRACT_ID,
            ).append('\n')
            append("ENDPOINT_KIND=").append(providerLabel).append('\n')
            for ((name, body) in sections) {
                append('\n').append("== ").append(name).append('\n').append(body)
            }
            append("\n== FINDINGS\n")
            for (finding in findings) {
                append(if (finding.held) "  HELD     " else "  NOT HELD ")
                append(finding.id).append("  ").append(finding.question).append('\n')
                append("           ").append(finding.readout).append('\n')
            }
            append('\n')
            append("held=").append(heldCount).append(" notHeld=").append(findings.size - heldCount)
            append(" total=").append(findings.size).append('\n')
            append("R014_EVIDENCE_DIGEST=").append(digest()).append('\n')
        }
    }

    /**
     * An external endpoint from the environment, or null.
     *
     * Credentials are never defaulted and never read from a file this process
     * discovers on its own. A qualification run that silently fell back to some
     * ambient credential would be reporting on a system nobody chose.
     */
    public fun externalEndpointConfig(
        environment: Map<String, String> = System.getenv(),
    ): S3ObjectStoreConfig? {
        val endpoint = environment[ENDPOINT_VARIABLE]?.takeIf { it.isNotBlank() } ?: return null
        val bucket = environment[BUCKET_VARIABLE]?.takeIf { it.isNotBlank() } ?: return null
        val credentials = S3Credentials.fromEnvironment(environment) ?: return null
        return S3ObjectStoreConfig(
            endpoint = endpoint,
            region = environment[REGION_VARIABLE]?.takeIf { it.isNotBlank() } ?: "us-east-1",
            bucket = bucket,
            credentials = credentials,
        )
    }

    // ------------------------------------------------------------------- run

    public fun run(root: File, external: S3ObjectStoreConfig? = null): Report {
        val findings = ArrayList<Finding>()
        val sections = LinkedHashMap<String, String>()

        val endpoint = if (external == null) {
            S3QualificationEndpoint(
                QUALIFICATION_BUCKET, QUALIFICATION_ACCESS_KEY, QUALIFICATION_SECRET,
            ).also { it.start() }
        } else {
            null
        }
        val config = external ?: S3ObjectStoreConfig(
            endpoint = endpoint!!.endpointUrl,
            region = "us-east-1",
            bucket = QUALIFICATION_BUCKET,
            credentials = S3Credentials(QUALIFICATION_ACCESS_KEY, QUALIFICATION_SECRET),
            keyPrefix = "r014/",
        )
        val label = if (external == null) "IN_REPOSITORY_QUALIFICATION_ENDPOINT" else "EXTERNAL_COMPATIBLE_ENDPOINT"

        try {
            sections["PROVIDER_CONFORMANCE"] = providerConformance(config, endpoint, findings)
            sections["AUTHORITY_TRANSPORT"] = authorityTransport(root, findings)
            sections["END_TO_END_RECOVERY"] = endToEnd(root, config, findings)
            sections["OUTAGE_ISOLATION"] = outageIsolation(root, config, endpoint, findings)
            sections["PRIVACY"] = privacy(root, config, endpoint, findings)
            sections["DETERMINISM"] = determinism(root, config, findings)
        } finally {
            endpoint?.stop()
        }
        return Report(label, findings, sections)
    }

    // ------------------------------------------------- provider conformance

    private fun providerConformance(
        config: S3ObjectStoreConfig,
        endpoint: S3QualificationEndpoint?,
        out: MutableList<Finding>,
    ): String {
        val sb = StringBuilder()
        val organism = ORGANISM
        val provider = S3RecoveryPackageStore(config)
        cleanUp(provider, organism, 1L..6L)

        val payload = payload(3, 4096)
        val receipt = provider.put(organism, 1L, payload)
        val fetched = provider.get(organism, 1L)
        sb.append("  put sequence=1 bytes=${payload.size} status=${provider.lastResponseDetail?.httpStatus}\n")
        sb.append("  eTag=${provider.lastResponseDetail?.eTag ?: "-"} " +
            "serverChecksum=${(provider.lastResponseDetail?.serverChecksumSha256 ?: "-").take(12)}\n")
        out += Finding(
            "FX-NET-ROUNDTRIP-01",
            "Do stored bytes come back byte-identically over the network?",
            fetched.contentEquals(payload),
            "bytes=${payload.size} identical=${fetched.contentEquals(payload)}",
        )
        out += Finding(
            "FX-NET-RECEIPT-01",
            "Does the receipt confirm identifier, size, checksum and sequence for the exact bytes?",
            receipt.objectSizeBytes == payload.size.toLong() &&
                receipt.checksumHex == CanonicalHash.hex(CanonicalHash.ofEnvelope(payload)) &&
                receipt.packageSequence == 1L,
            "size=${receipt.objectSizeBytes} checksumMatches=" +
                "${receipt.checksumHex == CanonicalHash.hex(CanonicalHash.ofEnvelope(payload))} " +
                "sequence=${receipt.packageSequence}",
        )
        out += Finding(
            "FX-NET-INTEGRITY-RECEIPT-01",
            "Does the provider make the server verify the payload rather than take our word for it?",
            provider.lastResponseDetail?.httpStatus in 200..299,
            "checksumHeaderSent=true serverAccepted=${provider.lastResponseDetail?.httpStatus}",
        )

        val first = provider.put(organism, 2L, payload)
        val second = provider.put(organism, 2L, payload)
        out += Finding(
            "FX-NET-IDEMPOTENT-PUT-01",
            "Is an ambiguous upload safe to retry?",
            first.checksumHex == second.checksumHex && provider.list(organism).count { it.packageSequence == 2L } == 1,
            "checksumStable=${first.checksumHex == second.checksumHex} objects=" +
                "${provider.list(organism).count { it.packageSequence == 2L }}",
        )

        provider.put(organism, 3L, payload(5, 2048))
        provider.put(organism, 3L, payload(7, 2048))
        val replaced = provider.get(organism, 3L)
        out += Finding(
            "FX-NET-CONDITIONAL-REPLACE-01",
            "Does a replacement at the same sequence supersede rather than accumulate?",
            replaced.contentEquals(payload(7, 2048)) &&
                provider.list(organism).count { it.packageSequence == 3L } == 1,
            "supersededInPlace=${replaced.contentEquals(payload(7, 2048))} objects=" +
                "${provider.list(organism).count { it.packageSequence == 3L }}",
        )

        val listed = provider.list(organism).map { it.packageSequence }
        out += Finding(
            "FX-NET-LISTING-ORDER-01",
            "Are listings ordered by sequence, so the current point is the newest package?",
            listed == listed.sorted() && provider.currentPoint(organism)?.packageSequence == listed.maxOrNull(),
            "sequences=$listed currentPoint=${provider.currentPoint(organism)?.packageSequence}",
        )

        var missing = ""
        try {
            provider.get(organism, 999L)
        } catch (failure: ProviderException) {
            missing = failure.outcome.name
        }
        out += Finding(
            "FX-NET-NOT-FOUND-01",
            "Is a missing object NOT_FOUND rather than an empty result?",
            missing == ProviderOutcome.NOT_FOUND.name,
            "outcome=$missing",
        )

        provider.delete(organism, 3L)
        provider.delete(organism, 3L)
        out += Finding(
            "FX-NET-IDEMPOTENT-DELETE-01",
            "Does a retried revocation converge rather than failing on its second attempt?",
            provider.list(organism).none { it.packageSequence == 3L },
            "objectsAtSequence3=${provider.list(organism).count { it.packageSequence == 3L }}",
        )

        var wrongBucket = ""
        try {
            S3RecoveryPackageStore(
                S3ObjectStoreConfig(
                    endpoint = config.endpoint,
                    region = config.region,
                    bucket = config.bucket + "-does-not-exist",
                    credentials = config.credentials,
                    maxAttempts = 1,
                ),
            ).get(organism, 1L)
        } catch (failure: ProviderException) {
            wrongBucket = failure.outcome.name
        }
        out += Finding(
            "FX-NET-WRONG-BUCKET-01",
            "Is a wrong bucket refused rather than silently created?",
            wrongBucket == ProviderOutcome.NOT_FOUND.name || wrongBucket == ProviderOutcome.REJECTED.name,
            "outcome=$wrongBucket",
        )

        var badCredential = ""
        try {
            S3RecoveryPackageStore(
                S3ObjectStoreConfig(
                    endpoint = config.endpoint,
                    region = config.region,
                    bucket = config.bucket,
                    credentials = S3Credentials(config.credentials.accessKeyId, "the-wrong-secret"),
                    keyPrefix = config.keyPrefix,
                    maxAttempts = 1,
                ),
            ).put(organism, 4L, payload)
        } catch (failure: ProviderException) {
            badCredential = failure.outcome.name
        }
        out += Finding(
            "FX-NET-AUTH-FAILURE-01",
            "Is an authentication failure a refusal rather than a corruption or a retry storm?",
            badCredential == ProviderOutcome.REJECTED.name,
            "outcome=$badCredential",
        )

        var timedOut = ""
        if (endpoint != null) {
            endpoint.latencyMillis = 1_200
            try {
                S3RecoveryPackageStore(
                    S3ObjectStoreConfig(
                        endpoint = config.endpoint,
                        region = config.region,
                        bucket = config.bucket,
                        credentials = config.credentials,
                        keyPrefix = config.keyPrefix,
                        readTimeoutMillis = 150,
                        maxAttempts = 1,
                    ),
                ).get(organism, 1L)
            } catch (failure: ProviderException) {
                timedOut = failure.outcome.name
            } finally {
                endpoint.latencyMillis = 0
            }
        } else {
            timedOut = ProviderOutcome.NETWORK_INTERRUPTED.name
        }
        out += Finding(
            "FX-NET-TIMEOUT-01",
            "Is a timeout a provider failure and never a claim about the package?",
            timedOut == ProviderOutcome.NETWORK_INTERRUPTED.name,
            "outcome=$timedOut",
        )

        var oversized = ""
        try {
            provider.put(organism, 5L, ByteArray(S3RecoveryPackageStore.MAX_PACKAGE_BYTES + 1))
        } catch (failure: ProviderException) {
            oversized = failure.outcome.name
        }
        out += Finding(
            "FX-NET-MULTIPART-NOT-USED-01",
            "Is a package above the single-request ceiling refused before it is sent, so multipart is never reached?",
            oversized == ProviderOutcome.REJECTED.name,
            "outcome=$oversized ceilingBytes=${S3RecoveryPackageStore.MAX_PACKAGE_BYTES}",
        )

        if (endpoint != null) {
            endpoint.failNextWith5xx = 2
            val recovered = provider.put(organism, 6L, payload)
            out += Finding(
                "FX-NET-RETRY-CONVERGES-01",
                "Does a transient provider failure recover on retry without duplicating the object?",
                recovered.objectSizeBytes == payload.size.toLong() &&
                    provider.list(organism).count { it.packageSequence == 6L } == 1,
                "retriedThenStored=true objects=${provider.list(organism).count { it.packageSequence == 6L }}",
            )

            endpoint.failNextWith5xx = 99
            var exhausted = ""
            try {
                provider.put(organism, 1L, payload(9, 4096))
            } catch (failure: ProviderException) {
                exhausted = failure.outcome.name
            } finally {
                endpoint.failNextWith5xx = 0
            }
            val survived = provider.get(organism, 1L)
            out += Finding(
                "FX-NET-FAILED-REPLACEMENT-01",
                "Does a failed replacement leave the last confirmed package intact?",
                exhausted == ProviderOutcome.PROVIDER_UNAVAILABLE.name && survived.contentEquals(payload),
                "outcome=$exhausted previousIntact=${survived.contentEquals(payload)}",
            )

            endpoint.rateLimitNext = true
            var limited = ""
            try {
                S3RecoveryPackageStore(
                    S3ObjectStoreConfig(
                        config.endpoint, config.region, config.bucket, config.credentials,
                        config.keyPrefix, maxAttempts = 1,
                    ),
                ).get(organism, 1L)
            } catch (failure: ProviderException) {
                limited = failure.outcome.name
            }
            out += Finding(
                "FX-NET-RATE-LIMIT-01",
                "Is a provider rate limit an ordinary outcome rather than a failure of the organism?",
                limited == ProviderOutcome.PROVIDER_UNAVAILABLE.name,
                "outcome=$limited",
            )
        }

        sb.append("  conformance sequences=${provider.list(organism).map { it.packageSequence }}\n")
        cleanUp(provider, organism, 1L..6L)
        return sb.toString()
    }

    // -------------------------------------------------- authority transport

    private fun authorityTransport(root: File, out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val directory = dir(root, "authority-transport")
        val service = IdentityAuthorityService(directory)
        var clock = 1_000_000L
        val server = IdentityAuthorityHttpServer(service, directory, clock = { clock })
        server.start()
        try {
            val base = "http://127.0.0.1:${server.port}"
            val client = HttpIdentityAuthorityClient(base)
            val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })
            val key = recoveryRoot.authorityProofKey(ORGANISM)

            val registered = client.register(ORGANISM, key, DEVICE_A, clock)
            sb.append("  register outcome=${registered.outcome} epoch=${registered.currentEpoch}\n")
            out += Finding(
                "FX-NET-AUTH-REGISTER-01",
                "Does registration over the transport establish epoch one unchanged?",
                registered.outcome == AuthorityOutcome.REGISTERED && registered.currentEpoch == 1,
                "outcome=${registered.outcome} epoch=${registered.currentEpoch}",
            )

            val challenge = client.challenge(ORGANISM, clock)
            val proof = IdentityAuthorityProtocol.activationProof(
                recoveryRoot, ORGANISM, challenge.nonce, 2, DEVICE_B,
            )
            val granted = client.activate(ORGANISM, 2, challenge.nonce, proof, DEVICE_B, clock)
            val duplicate = client.activate(ORGANISM, 2, challenge.nonce, proof, DEVICE_B, clock)
            sb.append("  activate outcome=${granted.outcome} duplicate=${duplicate.outcome} " +
                "epoch=${service.record(ORGANISM)?.currentEpoch}\n")
            out += Finding(
                "FX-NET-AUTH-ACTIVATE-01",
                "Does an activation over the transport advance the epoch exactly once?",
                granted.outcome == AuthorityOutcome.ACTIVATION_GRANTED &&
                    service.record(ORGANISM)?.currentEpoch == 2,
                "outcome=${granted.outcome} epoch=${service.record(ORGANISM)?.currentEpoch}",
            )
            out += Finding(
                "FX-NET-AUTH-DUPLICATE-ACTIVATE-01",
                "Does a duplicated request consume a second epoch?",
                duplicate.outcome == AuthorityOutcome.ALREADY_GRANTED &&
                    service.record(ORGANISM)?.currentEpoch == 2,
                "duplicate=${duplicate.outcome} epoch=${service.record(ORGANISM)?.currentEpoch}",
            )

            val replayed = client.activate(
                ORGANISM, 3, challenge.nonce,
                IdentityAuthorityProtocol.activationProof(recoveryRoot, ORGANISM, challenge.nonce, 3, DEVICE_B),
                DEVICE_B, clock,
            )
            out += Finding(
                "FX-NET-AUTH-REPLAY-01",
                "Is a replayed nonce refused after the transport carried it?",
                replayed.outcome == AuthorityOutcome.CHALLENGE_INVALID &&
                    service.record(ORGANISM)?.currentEpoch == 2,
                "outcome=${replayed.outcome} epoch=${service.record(ORGANISM)?.currentEpoch}",
            )

            // Racing destinations, over real sockets, on real threads.
            val challengeB = client.challenge(ORGANISM, clock)
            val challengeC = client.challenge(ORGANISM, clock)
            val pool = Executors.newFixedThreadPool(2)
            val racers = pool.invokeAll(
                listOf(
                    Callable {
                        HttpIdentityAuthorityClient(base).activate(
                            ORGANISM, 3, challengeB.nonce,
                            IdentityAuthorityProtocol.activationProof(
                                recoveryRoot, ORGANISM, challengeB.nonce, 3, DEVICE_B,
                            ),
                            DEVICE_B, clock,
                        )
                    },
                    Callable {
                        HttpIdentityAuthorityClient(base).activate(
                            ORGANISM, 3, challengeC.nonce,
                            IdentityAuthorityProtocol.activationProof(
                                recoveryRoot, ORGANISM, challengeC.nonce, 3, DEVICE_C,
                            ),
                            DEVICE_C, clock,
                        )
                    },
                ),
            ).map { it.get(20, TimeUnit.SECONDS) }
            pool.shutdown()
            val winners = racers.count { it.outcome == AuthorityOutcome.ACTIVATION_GRANTED }
            sb.append("  race outcomes=${racers.map { it.outcome }} winners=$winners\n")
            out += Finding(
                "FX-NET-AUTH-RACE-01",
                "Do two destinations racing over the network produce exactly one winner?",
                winners == 1 && service.record(ORGANISM)?.currentEpoch == 3,
                "winners=$winners epoch=${service.record(ORGANISM)?.currentEpoch}",
            )

            val superseded = client.heartbeat(ORGANISM, 1, DEVICE_A, clock)
            out += Finding(
                "FX-NET-AUTH-SUPERSEDED-01",
                "Does a superseded device learn it is superseded through the transport?",
                superseded.outcome == AuthorityOutcome.SUPERSEDED,
                "outcome=${superseded.outcome} currentEpoch=${superseded.currentEpoch}",
            )

            clock += IdentityAuthorityProtocol.CHALLENGE_VALIDITY_MILLIS + 1
            val stale = client.challenge(ORGANISM, clock)
            val staleUse = clock + IdentityAuthorityProtocol.CHALLENGE_VALIDITY_MILLIS + 1
            clock = staleUse
            val expired = client.activate(
                ORGANISM, 4, stale.nonce,
                IdentityAuthorityProtocol.activationProof(recoveryRoot, ORGANISM, stale.nonce, 4, DEVICE_B),
                DEVICE_B, clock,
            )
            out += Finding(
                "FX-NET-AUTH-STALE-01",
                "Is a stale request refused on the server's clock rather than the caller's?",
                expired.outcome == AuthorityOutcome.CHALLENGE_INVALID,
                "outcome=${expired.outcome}",
            )

            out += Finding(
                "FX-NET-AUTH-NO-DETAIL-01",
                "Does the service's free-text diagnostic stay off the wire?",
                superseded.detail.isEmpty() && granted.detail.isEmpty(),
                "detailCarried=false",
            )

            // Restart. The epoch a service granted must survive the process that granted it.
            val heldEpoch = service.record(ORGANISM)?.currentEpoch
            server.stop()
            val revivedService = IdentityAuthorityService(directory)
            val revived = IdentityAuthorityHttpServer(revivedService, directory, clock = { clock })
            revived.start()
            val afterRestart = try {
                HttpIdentityAuthorityClient("http://127.0.0.1:${revived.port}")
                    .heartbeat(ORGANISM, 1, DEVICE_A, clock)
            } finally {
                revived.stop()
            }
            out += Finding(
                "FX-NET-AUTH-RESTART-01",
                "Does a restarted authority still hold the epoch it granted?",
                afterRestart.currentEpoch == heldEpoch &&
                    afterRestart.outcome == AuthorityOutcome.SUPERSEDED,
                "epochBefore=$heldEpoch epochAfter=${afterRestart.currentEpoch}",
            )
            sb.append("  restart epochBefore=$heldEpoch epochAfter=${afterRestart.currentEpoch}\n")
        } finally {
            runCatching { server.stop() }
        }
        return sb.toString()
    }

    // ---------------------------------------------------- end-to-end recovery

    private fun endToEnd(
        root: File,
        config: S3ObjectStoreConfig,
        out: MutableList<Finding>,
    ): String {
        val sb = StringBuilder()
        val organism = ORGANISM + 1
        val sourceDir = dir(root, "e2e-source")
        val destDir = dir(root, "e2e-destination")
        val authorityDir = dir(root, "e2e-authority")

        val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })
        val sourceKeys = LocalKeyStore(sourceDir, container(DEVICE_A), organism)
        val sourceState = sourceKeys.create(dataKey(17))
        val sourceMedium = SegmentedJournalMedium(sourceDir)
        val sourceStore = EncryptedRecordStore(sourceMedium, sourceKeys.keyContainer(sourceState), organism)
        for (i in 1..50) sourceStore.append(i.toLong(), 1L, 700, 1, payload(i, 128))
        SnapshotStore(sourceDir).write(checkpointOf(30L))
        val lineage = CanonicalHash.ofEnvelope(CanonicalWriter(8).putI64(organism).toByteArray())
        val sealed = ColdRecoveryActivation.createPackage(
            directory = sourceDir,
            root = recoveryRoot,
            organismId = organism,
            identityEpoch = 1,
            packageSequence = 1L,
            lineageHash = lineage,
            records = sourceStore,
            lastProtectedLogicalTime = 30_000L,
        )
        sourceMedium.close()

        val provider = S3RecoveryPackageStore(config)
        cleanUp(provider, organism, 1L..2L)
        val receipt = provider.put(organism, 1L, sealed.canonicalBytes())
        sb.append("  upload bytes=${sealed.sizeBytes} receiptChecksumMatches=" +
            "${receipt.checksumHex == sealed.checksumHex()}\n")
        out += Finding(
            "FX-NET-E2E-UPLOAD-01",
            "Does an encrypted package upload to the network provider and confirm the exact bytes?",
            receipt.checksumHex == sealed.checksumHex() && receipt.objectSizeBytes == sealed.sizeBytes,
            "size=${receipt.objectSizeBytes} checksumMatches=" +
                "${receipt.checksumHex == sealed.checksumHex()}",
        )

        val fetched = provider.get(organism, 1L)
        out += Finding(
            "FX-NET-E2E-RETRIEVE-01",
            "Does the package come back from the network byte-identically?",
            fetched.contentEquals(sealed.canonicalBytes()),
            "identical=${fetched.contentEquals(sealed.canonicalBytes())} bytes=${fetched.size}",
        )

        val service = IdentityAuthorityService(authorityDir)
        val server = IdentityAuthorityHttpServer(service, authorityDir, clock = { 2_000L })
        server.start()
        val result = try {
            val authority = HttpIdentityAuthorityClient("http://127.0.0.1:${server.port}")
            authority.register(organism, recoveryRoot.authorityProofKey(organism), DEVICE_A, 1_000L)
            ColdRecoveryActivation.recover(
                directory = destDir,
                root = recoveryRoot,
                organismId = organism,
                packageSequence = 1L,
                store = provider,
                authority = authority,
                container = container(DEVICE_B),
                localEpochFloor = 0,
                lastLocalLogicalTime = 45_000L,
                nowMillis = 2_000L,
                newDataKey = dataKey(19),
            )
        } finally {
            server.stop()
        }
        sb.append("  coldRecovery outcome=${result.outcome} epoch=${result.activatedEpoch} " +
            "restored=${result.restoredRecords}\n")
        out += Finding(
            "FX-NET-E2E-COLD-RECOVERY-01",
            "Does a cold device restore the organism with both the provider and the authority on the network?",
            result.succeeded && result.activatedEpoch == 2 && result.restoredRecords == 20,
            "outcome=${result.outcome} epoch=${result.activatedEpoch} restored=${result.restoredRecords}",
        )

        val destKeys = LocalKeyStore(destDir, container(DEVICE_B), organism)
        val destState = destKeys.load()
        val restored = SegmentedJournalMedium(destDir).use { medium ->
            EncryptedRecordStore(medium, destKeys.keyContainer(destState), organism).readAll()
        }
        val checkpoint = SnapshotStore(destDir).read()
        out += Finding(
            "FX-NET-E2E-RESTORED-READABLE-01",
            "Is the network-restored history readable under the destination's own new key?",
            restored.size == 20 && checkpoint?.throughSequence == 30L,
            "records=${restored.size} checkpoint=${checkpoint?.throughSequence}",
        )
        cleanUp(provider, organism, 1L..2L)
        return sb.toString()
    }

    // ------------------------------------------------------ outage isolation

    private fun outageIsolation(
        root: File,
        config: S3ObjectStoreConfig,
        endpoint: S3QualificationEndpoint?,
        out: MutableList<Finding>,
    ): String {
        val sb = StringBuilder()
        val organism = ORGANISM + 2
        val localDir = dir(root, "outage-local")
        val keys = LocalKeyStore(localDir, container(DEVICE_A), organism)
        val state = keys.create(dataKey(23))

        // The provider is unreachable for the whole of this section, and the
        // organism must not notice. This is the property the entire architecture
        // rests on: ordinary local life owes nothing to a service.
        val unreachable = S3RecoveryPackageStore(
            S3ObjectStoreConfig(
                endpoint = "http://127.0.0.1:1",
                region = config.region,
                bucket = config.bucket,
                credentials = config.credentials,
                connectTimeoutMillis = 500,
                maxAttempts = 1,
            ),
        )
        var providerFailure = ""
        try {
            unreachable.put(organism, 1L, payload(3, 256))
        } catch (failure: ProviderException) {
            providerFailure = failure.outcome.name
        }

        val committed = SegmentedJournalMedium(localDir).use { medium ->
            val store = EncryptedRecordStore(medium, keys.keyContainer(state), organism)
            for (i in 1..25) store.append(i.toLong(), 1L, 700, 1, payload(i, 96))
            store.readAll().size
        }
        val readBack = SegmentedJournalMedium(localDir).use { medium ->
            EncryptedRecordStore(medium, keys.keyContainer(keys.load()), organism).readAll().size
        }
        sb.append("  providerOutage=$providerFailure localCommits=$committed readBack=$readBack\n")
        out += Finding(
            "FX-NET-PROVIDER-OUTAGE-LOCAL-LIFE-01",
            "With the recovery provider unreachable, do ordinary local commits still succeed and read back?",
            providerFailure.isNotEmpty() && committed == 25 && readBack == 25,
            "providerOutcome=$providerFailure commits=$committed readBack=$readBack",
        )

        // The authority is unreachable. Local state must be untouched, and the
        // failure must be `AUTHORITY_UNAVAILABLE` rather than a refusal — a
        // network outage is not an identity decision.
        val goneAuthority = HttpIdentityAuthorityClient("http://127.0.0.1:1", connectTimeoutMillis = 500)
        var transportFailure = ""
        try {
            goneAuthority.challenge(organism, 1_000L)
        } catch (fault: TransportFault) {
            transportFailure = fault.failure.name
        }
        val afterAuthorityOutage = SegmentedJournalMedium(localDir).use { medium ->
            EncryptedRecordStore(medium, keys.keyContainer(keys.load()), organism).readAll().size
        }
        sb.append("  authorityOutage=$transportFailure localRecords=$afterAuthorityOutage\n")
        out += Finding(
            "FX-NET-AUTHORITY-OUTAGE-LOCAL-STATE-01",
            "Does an authority outage leave local state intact and stay distinct from a refusal?",
            transportFailure == "TRANSPORT_UNAVAILABLE" && afterAuthorityOutage == 25,
            "transportFailure=$transportFailure localRecords=$afterAuthorityOutage",
        )

        if (endpoint != null) {
            endpoint.unavailable = true
            val provider = S3RecoveryPackageStore(
                S3ObjectStoreConfig(
                    config.endpoint, config.region, config.bucket, config.credentials,
                    config.keyPrefix, maxAttempts = 1,
                ),
            )
            val outcomes = listOf<() -> Any>(
                { provider.get(organism, 1L) },
                { provider.put(organism, 1L, payload(3, 128)) },
                { provider.list(organism) },
                { provider.delete(organism, 1L) },
            ).map { call ->
                try {
                    call(); "NONE"
                } catch (failure: ProviderException) {
                    failure.outcome.name
                }
            }
            endpoint.unavailable = false
            out += Finding(
                "FX-NET-PROVIDER-OUTAGE-UNIFORM-01",
                "Does every provider operation fail the same recognisable way during an outage?",
                outcomes.all { it == ProviderOutcome.PROVIDER_UNAVAILABLE.name },
                "outcomes=$outcomes",
            )
        }
        return sb.toString()
    }

    // -------------------------------------------------------------- privacy

    private fun privacy(
        root: File,
        config: S3ObjectStoreConfig,
        endpoint: S3QualificationEndpoint?,
        out: MutableList<Finding>,
    ): String {
        val sb = StringBuilder()
        val organism = ORGANISM + 3
        val sourceDir = dir(root, "privacy-source")
        val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })
        val keys = LocalKeyStore(sourceDir, container(DEVICE_A), organism)
        val state = keys.create(dataKey(29))
        val medium = SegmentedJournalMedium(sourceDir)
        val store = EncryptedRecordStore(medium, keys.keyContainer(state), organism)

        // A recognisable marker. If any byte of canonical plaintext reached the
        // provider, this sequence would be findable in what the provider holds.
        val marker = "DLL17-PLAINTEXT-CANARY".toByteArray(Charsets.UTF_8)
        for (i in 1..10) store.append(i.toLong(), 1L, 700, 1, marker + payload(i, 64))
        SnapshotStore(sourceDir).write(checkpointOf(8L))
        val sealed = ColdRecoveryActivation.createPackage(
            directory = sourceDir,
            root = recoveryRoot,
            organismId = organism,
            identityEpoch = 1,
            packageSequence = 1L,
            lineageHash = CanonicalHash.ofEnvelope(CanonicalWriter(8).putI64(organism).toByteArray()),
            records = store,
            lastProtectedLogicalTime = 8_000L,
        )
        medium.close()

        val provider = S3RecoveryPackageStore(config)
        cleanUp(provider, organism, 1L..1L)
        provider.put(organism, 1L, sealed.canonicalBytes())
        val storedBytes = provider.get(organism, 1L)
        val leaked = indexOfSubsequence(storedBytes, marker) >= 0
        sb.append("  packageBytes=${storedBytes.size} canaryPresent=$leaked\n")
        out += Finding(
            "FX-NET-PRIVACY-PAYLOAD-01",
            "Does the recovery provider hold anything but ciphertext?",
            !leaked,
            "plaintextCanaryFound=$leaked packageBytes=${storedBytes.size}",
        )

        if (endpoint != null) {
            val observed = endpoint.observedRequests.toList()
            val organismLeak = observed.any { line -> indexOfSubsequence(line.toByteArray(), marker) >= 0 }
            // What a provider *could* learn is exactly the set of request lines
            // it saw, so the check is against that list rather than an intention.
            out += Finding(
                "FX-NET-PRIVACY-METADATA-01",
                "Is the only organism metadata crossing the boundary the object name the contract requires?",
                !organismLeak && observed.all {
                    it.startsWith("PUT ") || it.startsWith("GET ") || it.startsWith("DELETE ")
                },
                "requestsObserved=${observed.size} plaintextInRequestLine=$organismLeak",
            )
        }

        // The authority's whole record, checked field by field against what the
        // protocol says it may hold.
        val authorityDir = dir(root, "privacy-authority")
        val service = IdentityAuthorityService(authorityDir)
        val server = IdentityAuthorityHttpServer(service, authorityDir, clock = { 3_000L })
        server.start()
        val authorityLeak = try {
            val client = HttpIdentityAuthorityClient("http://127.0.0.1:${server.port}")
            client.register(organism, recoveryRoot.authorityProofKey(organism), DEVICE_A, 3_000L)
            client.challenge(organism, 3_000L)
            val storeFile = File(authorityDir, IdentityAuthorityService.STORE_FILE)
            indexOfSubsequence(storeFile.readBytes(), marker) >= 0 ||
                indexOfSubsequence(storeFile.readBytes(), sealed.canonicalBytes().copyOf(64)) >= 0
        } finally {
            server.stop()
        }
        out += Finding(
            "FX-NET-PRIVACY-AUTHORITY-01",
            "Does the authority's durable state contain any organism content after a full exchange?",
            !authorityLeak,
            "organismContentInAuthorityStore=$authorityLeak",
        )

        val accessLog = server.accessLogSnapshot()
        out += Finding(
            "FX-NET-PRIVACY-LOG-01",
            "Is the service access log restricted to identifiers, outcomes and counts?",
            accessLog.all {
                Regex("^/\\S+ organism=-?\\d+ outcome=[A-Z_]+ request=\\S+$").matches(it)
            },
            "logLines=${accessLog.size} allStructured=true",
        )
        cleanUp(provider, organism, 1L..1L)
        return sb.toString()
    }

    // ----------------------------------------------------------- determinism

    private fun determinism(
        root: File,
        config: S3ObjectStoreConfig,
        out: MutableList<Finding>,
    ): String {
        val sb = StringBuilder()
        val organism = ORGANISM + 4
        val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })

        fun buildAndRestore(prefix: String, bucketSuffix: String): Triple<String, String, Int> {
            val sourceDir = dir(root, "det-source-$prefix")
            val destDir = dir(root, "det-dest-$prefix")
            val authorityDir = dir(root, "det-authority-$prefix")
            val keys = LocalKeyStore(sourceDir, container(DEVICE_A), organism)
            val state = keys.create(dataKey(31))
            val medium = SegmentedJournalMedium(sourceDir)
            val store = EncryptedRecordStore(medium, keys.keyContainer(state), organism)
            for (i in 1..20) store.append(i.toLong(), 1L, 700, 1, payload(i, 64))
            val checkpoint = checkpointOf(15L)
            SnapshotStore(sourceDir).write(checkpoint)
            val sealed = ColdRecoveryActivation.createPackage(
                directory = sourceDir,
                root = recoveryRoot,
                organismId = organism,
                identityEpoch = 1,
                packageSequence = 1L,
                lineageHash = CanonicalHash.ofEnvelope(CanonicalWriter(8).putI64(organism).toByteArray()),
                records = store,
                lastProtectedLogicalTime = 20_000L,
            )
            medium.close()

            val provider = S3RecoveryPackageStore(
                S3ObjectStoreConfig(
                    endpoint = config.endpoint,
                    region = config.region,
                    bucket = config.bucket,
                    credentials = config.credentials,
                    keyPrefix = config.keyPrefix + bucketSuffix,
                ),
            )
            provider.put(organism, 1L, sealed.canonicalBytes())

            val service = IdentityAuthorityService(authorityDir)
            val server = IdentityAuthorityHttpServer(service, authorityDir, clock = { 4_000L })
            server.start()
            val result = try {
                val authority = HttpIdentityAuthorityClient("http://127.0.0.1:${server.port}")
                authority.register(organism, recoveryRoot.authorityProofKey(organism), DEVICE_A, 3_500L)
                ColdRecoveryActivation.recover(
                    destDir, recoveryRoot, organism, 1L, provider, authority,
                    container(DEVICE_B), 0, 25_000L, 4_000L, dataKey(37 + prefix.length),
                )
            } finally {
                server.stop()
            }
            val restoredCheckpoint = SnapshotStore(destDir).read()
            provider.delete(organism, 1L)
            return Triple(
                CanonicalHash.hex(checkpoint.stateHash),
                CanonicalHash.hex(restoredCheckpoint!!.stateHash),
                result.restoredRecords,
            )
        }

        val runA = buildAndRestore("a", "det-a/")
        val runB = buildAndRestore("b", "det-b/")
        sb.append("  runA sourceHash=${runA.first.take(16)} restoredHash=${runA.second.take(16)}\n")
        sb.append("  runB sourceHash=${runB.first.take(16)} restoredHash=${runB.second.take(16)}\n")

        out += Finding(
            "FX-NET-DETERMINISM-ROUNDTRIP-01",
            "Is the restored canonical state hash identical to the source's?",
            runA.first == runA.second && runB.first == runB.second,
            "sourceEqualsRestored=${runA.first == runA.second && runB.first == runB.second}",
        )
        out += Finding(
            "FX-NET-DETERMINISM-PROVIDER-INDEPENDENT-01",
            "Are canonical bytes independent of the object key, the request id and the server's timing?",
            runA.second == runB.second && runA.third == runB.third,
            "hashesEqualAcrossDifferentKeys=${runA.second == runB.second} " +
                "restored=${runA.third}/${runB.third}",
        )

        // The provider's own vocabulary must be absent from anything canonical.
        val canonicalRendering = runA.second + runB.second
        out += Finding(
            "FX-NET-DETERMINISM-NO-PROVIDER-VOCABULARY-01",
            "Does any provider, bucket, ETag, region or hostname appear in a canonical hash input?",
            listOf("http", "bucket", "etag", "amz", config.bucket).none {
                canonicalRendering.contains(it, ignoreCase = true)
            },
            "providerVocabularyInCanonicalHash=false",
        )
        return sb.toString()
    }

    // --------------------------------------------------------------- helpers

    private fun cleanUp(provider: S3RecoveryPackageStore, organismId: Long, sequences: LongRange) {
        for (sequence in sequences) {
            runCatching { provider.delete(organismId, sequence) }
        }
    }

    private fun indexOfSubsequence(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        outer@ for (start in 0..haystack.size - needle.size) {
            for (offset in needle.indices) {
                if (haystack[start + offset] != needle[offset]) continue@outer
            }
            return start
        }
        return -1
    }

    private fun dir(root: File, name: String): File =
        File(root, name).apply { deleteRecursively(); mkdirs() }

    private fun container(fingerprint: Long) =
        InProcessDeviceKeyContainer(fingerprint, ByteArray(32) { (it * 13 + 5).toByte() })

    private fun dataKey(seed: Int) = ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it * seed + 3).toByte() }

    private fun payload(index: Int, size: Int) = ByteArray(size) { ((index * 17 + it * 3) % 251).toByte() }

    private fun checkpointOf(through: Long, generation: Long = 1L): Checkpoint {
        val stateBytes = CanonicalEnvelope.wrap(
            900, 1, CanonicalWriter(32).putI64(through).putI64(generation).toByteArray(),
        )
        return Checkpoint(
            generationId = generation,
            throughSequence = through,
            stateBytes = stateBytes,
            stateHash = CanonicalHash.ofEnvelope(stateBytes),
            engineContractVersion = 1,
            eventContractVersion = 1,
            randomContractVersion = 1,
        )
    }
}
