package com.animusmachinae.dll17.core.persistence

import com.animusmachinae.dll17.core.continuity.Checkpoint
import com.animusmachinae.dll17.core.continuity.EncryptedRecordStore
import com.animusmachinae.dll17.core.continuity.StorageFault
import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.recovery.AuthorityOutcome
import com.animusmachinae.dll17.core.recovery.ColdRecoveryPackage
import com.animusmachinae.dll17.core.recovery.FilesystemRecoveryPackageStore
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityClient
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityProtocol
import com.animusmachinae.dll17.core.recovery.ProviderException
import com.animusmachinae.dll17.core.recovery.ProviderOutcome
import com.animusmachinae.dll17.core.recovery.RecoveryFailure
import com.animusmachinae.dll17.core.recovery.RecoveryRoot
import com.animusmachinae.dll17.core.recovery.RecoverySecretCodec
import java.io.File
import java.security.MessageDigest

/**
 * The R012 qualification kernel.
 *
 * Same shape as the R001, R002 and A000 kernels: named fixtures, an explicit
 * held / not-held verdict for each, and a digest over every identifier and
 * readout so a silent behaviour change fails CI rather than quietly invalidating
 * the record.
 *
 * Performance figures are measured on whatever machine runs it and are therefore
 * *not* part of the digest — a latency that varied by a microsecond would break
 * every build. They are written to a separate evidence file.
 */
public object R012QualificationKernel {

    public const val FIXTURE_SET_ID: String = "R012-FIXTURES-V1"
    public const val FIXTURE_SET_VERSION: Int = 1

    /** Reproduced by a clean run of this kernel. CI fails if it drifts. */
    public const val GOLDEN_EVIDENCE_DIGEST: String =
        "48bd44a31a3a952cf884b358c5e587b93a24875f1d36d7625e6b4b7d5f62127f"

    private const val ORGANISM = 0x0D11_0011L
    private const val DEVICE_A = 0xA111_1111L
    private const val DEVICE_B = 0xB222_2222L

    public class Finding(
        public val id: String,
        public val question: String,
        public val held: Boolean,
        public val readout: String,
    )

    public class Report(
        public val findings: List<Finding>,
        public val sections: Map<String, String>,
    ) {
        public val heldCount: Int get() = findings.count { it.held }

        public fun digest(): String {
            val writer = CanonicalWriter(4096)
            writer.putIdentifier(FIXTURE_SET_ID).putI32(FIXTURE_SET_VERSION)
            for (finding in findings) {
                writer.putIdentifier(finding.id)
                writer.putBool(finding.held)
                writer.putRawBytes(finding.readout.toByteArray(Charsets.UTF_8))
            }
            val digest = MessageDigest.getInstance("SHA-256").digest(writer.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }

        public fun render(): String = buildString {
            append("R012_FIXTURE_SET=").append(FIXTURE_SET_ID).append('\n')
            append("R012_FIXTURE_VERSION=").append(FIXTURE_SET_VERSION).append('\n')
            append("BACKEND=").append(PersistenceBackendContract.BACKEND_ID).append('\n')
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
            append("R012_EVIDENCE_DIGEST=").append(digest()).append('\n')
        }
    }

    /**
     * The authority factory is injected rather than constructed.
     *
     * `core-persistence` must not depend on the authority service, because the
     * whole architectural claim is that the service is separately deployable and
     * that ordinary local operation never reaches it. A compile-time dependency
     * here would quietly falsify that claim, so the caller supplies one.
     */
    public fun run(
        root: File,
        authorityFactory: (File) -> IdentityAuthorityClient,
    ): Report {
        val findings = ArrayList<Finding>()
        val sections = LinkedHashMap<String, String>()
        sections["BACKEND"] = backend(root, findings)
        sections["CRYPTOGRAPHY"] = cryptography(root, findings)
        sections["RECOVERY"] = recovery(root, findings, authorityFactory)
        sections["IDENTITY_AUTHORITY"] = identity(root, findings, authorityFactory)
        sections["FAULT_MATRIX"] = faults(root, findings)
        sections["DETERMINISM"] = determinism(root, findings)
        return Report(findings, sections)
    }

    // --------------------------------------------------------------- helpers

    private fun dir(root: File, name: String): File =
        File(root, name).apply { deleteRecursively(); mkdirs() }

    private fun container(fingerprint: Long = DEVICE_A) =
        InProcessDeviceKeyContainer(fingerprint, ByteArray(32) { (it * 13 + 5).toByte() })

    private fun dataKey(seed: Int) = ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it * seed + 3).toByte() }

    private fun payload(index: Int) = ByteArray(128) { ((index * 17 + it * 3) % 251).toByte() }

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

    private fun openStore(directory: File, keyStore: LocalKeyStore, state: WrappedKeyState) =
        EncryptedRecordStore(SegmentedJournalMedium(directory), keyStore.keyContainer(state), ORGANISM)

    // --------------------------------------------------------------- backend

    private fun backend(root: File, out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val directory = dir(root, "backend")
        val keys = LocalKeyStore(directory, container(), ORGANISM)
        val state = keys.create(dataKey(3))

        SegmentedJournalMedium(directory).use { medium ->
            val store = EncryptedRecordStore(medium, keys.keyContainer(state), ORGANISM)
            for (i in 1..200) store.append(i.toLong(), 1L, 700, 1, payload(i))
            val read = store.readAll()
            sb.append("  appended=200 readBack=${read.size} usedBytes=${medium.usedBytes}\n")
            out += Finding(
                "FX-BACKEND-ROUNDTRIP-01",
                "Does every acknowledged record come back byte-identical after reopen?",
                read.size == 200 && read.all { it.payload.contentEquals(payload(it.sequence.toInt())) },
                "records=${read.size} allPayloadsMatch=" +
                    read.all { it.payload.contentEquals(payload(it.sequence.toInt())) },
            )

            store.prune(100L)
            val afterPrune = store.readAll()
            sb.append("  afterPrune records=${afterPrune.size} usedBytes=${medium.usedBytes}\n")
            out += Finding(
                "FX-BACKEND-COMPACTION-01",
                "Does compaction drop exactly the pruned range and keep the rest readable?",
                afterPrune.size == 100 && afterPrune.first().sequence == 101L,
                "survivors=${afterPrune.size} firstSequence=${afterPrune.firstOrNull()?.sequence}",
            )
        }

        // Reopen from cold: the medium must reconstruct its state from the file.
        SegmentedJournalMedium(directory).use { reopened ->
            val store = EncryptedRecordStore(reopened, keys.keyContainer(state), ORGANISM)
            val read = store.readAll()
            out += Finding(
                "FX-BACKEND-COLD-REOPEN-01",
                "Does a cold reopen recover the compacted history without a catalogue?",
                read.size == 100 && read.last().sequence == 200L,
                "records=${read.size} lastSequence=${read.lastOrNull()?.sequence}",
            )
            out += Finding(
                "FX-BACKEND-SELFTEST-01",
                "Does the durability self-test exercise a real write, force and read-back?",
                reopened.selfTest(),
                "selfTest=${reopened.selfTest()}",
            )
        }

        // Single-writer authority: a second sequence at or below the high-water
        // mark is a programming error, not a storage event.
        val pressure = dir(root, "backend-pressure")
        val pressureKeys = LocalKeyStore(pressure, container(), ORGANISM)
        val pressureState = pressureKeys.create(dataKey(5))
        SegmentedJournalMedium(pressure).use { medium ->
            var regressionRefused = false
            medium.append(10L, payload(1))
            try {
                medium.append(10L, payload(2))
            } catch (expected: IllegalArgumentException) {
                regressionRefused = true
            }
            out += Finding(
                "FX-BACKEND-SINGLE-WRITER-01",
                "Does the backend refuse a sequence that does not advance?",
                regressionRefused,
                "regressionRefused=$regressionRefused highWater=10",
            )
        }

        // Capacity: a tiny medium must refuse rather than overrun, and history
        // written before the refusal must survive it.
        val small = dir(root, "backend-capacity")
        SegmentedJournalMedium(small, capacityBytes = 4_096L).use { medium ->
            var written = 0
            var faulted = false
            try {
                for (i in 1..1000) {
                    medium.append(i.toLong(), payload(i))
                    written++
                }
            } catch (fault: StorageFault) {
                faulted = true
            }
            val survivors = medium.records().size
            sb.append("  capacity=4096 acceptedRecords=$written survivorsAfterFault=$survivors\n")
            out += Finding(
                "FX-BACKEND-CAPACITY-01",
                "Does an exhausted medium refuse the write and keep prior history intact?",
                faulted && survivors == written && written in 1..999,
                "faulted=$faulted accepted=$written survivors=$survivors",
            )
        }
        pressureState.let { }
        return sb.toString()
    }

    // ---------------------------------------------------------- cryptography

    private fun cryptography(root: File, out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val directory = dir(root, "crypto")
        val device = container()
        val keys = LocalKeyStore(directory, device, ORGANISM)
        val state = keys.create(dataKey(7))

        SegmentedJournalMedium(directory).use { medium ->
            val store = EncryptedRecordStore(medium, keys.keyContainer(state), ORGANISM)
            for (i in 1..20) store.append(i.toLong(), 1L, 700, 1, payload(i))

            // No plaintext on the medium: the raw record bytes must not contain
            // the payload anywhere.
            val raw = medium.records().first().second
            val plaintextVisible = containsSubsequence(raw, payload(1))
            sb.append("  rawRecordBytes=${raw.size} plaintextVisibleOnMedium=$plaintextVisible\n")
            out += Finding(
                "FX-CRYPTO-AT-REST-01",
                "Is the canonical payload absent from the bytes the medium holds?",
                !plaintextVisible,
                "plaintextVisible=$plaintextVisible rawBytes=${raw.size}",
            )

            // Corruption: flip a bit in a stored record and require a loud fault.
            val target = File(directory, PersistenceBackendContract.JOURNAL_FILE)
            val bytes = target.readBytes()
            // Inside the first frame's ciphertext body, not at a random offset:
            // this is the case where the framing is intact and only the AEAD can
            // notice, which is the harder half of corruption detection.
            val bodyOffset = PersistenceBackendContract.FRAME_HEADER_BYTES + 8
            bytes[bodyOffset] = (bytes[bodyOffset].toInt() xor 0x01).toByte()
            target.writeBytes(bytes)
            var detected = false
            var silentReset = false
            try {
                EncryptedRecordStore(
                    SegmentedJournalMedium(directory), keys.keyContainer(state), ORGANISM,
                ).readAll()
            } catch (fault: StorageFault) {
                detected = true
            } catch (other: RuntimeException) {
                detected = true
            }
            silentReset = !detected
            out += Finding(
                "FX-CRYPTO-CORRUPTION-01",
                "Does a single flipped bit produce a detected fault rather than a silent reset?",
                detected && !silentReset,
                "detected=$detected silentReset=$silentReset",
            )
        }

        // Quarantine rather than reset when the container refuses.
        val quarantineDir = dir(root, "crypto-quarantine")
        val quarantineDevice = container()
        val quarantineKeys = LocalKeyStore(quarantineDir, quarantineDevice, ORGANISM)
        val quarantineState = quarantineKeys.create(dataKey(9))
        quarantineDevice.simulateUnavailable = true
        var refused = false
        var faultKind = KeyFault.NONE
        try {
            quarantineKeys.unwrap(quarantineState)
        } catch (failure: KeyStateFault) {
            refused = true
            faultKind = failure.fault
        }
        if (refused) {
            Quarantine.mark(
                quarantineDir, Quarantine.REASON_KEY_CONTAINER_REFUSED, "container unavailable",
            )
        }
        val quarantined = Quarantine.isQuarantined(quarantineDir)
        val stillPresent = File(quarantineDir, PersistenceBackendContract.KEYSTATE_FILE).exists()
        sb.append("  containerRefused=$refused fault=$faultKind quarantined=$quarantined\n")
        out += Finding(
            "FX-CRYPTO-QUARANTINE-01",
            "Does an unusable key container quarantine the installation instead of resetting it?",
            refused && quarantined && stillPresent &&
                faultKind == KeyFault.CONTAINER_UNAVAILABLE,
            "refused=$refused fault=$faultKind quarantined=$quarantined keyStateRetained=$stillPresent",
        )

        // A foreign device fingerprint is its own distinct refusal.
        val foreign = InProcessDeviceKeyContainer(DEVICE_B, ByteArray(32) { (it * 13 + 5).toByte() })
        var deviceFault = KeyFault.NONE
        try {
            LocalKeyStore(quarantineDir, foreign, ORGANISM).unwrap(quarantineState)
        } catch (failure: KeyStateFault) {
            deviceFault = failure.fault
        }
        out += Finding(
            "FX-CRYPTO-DEVICE-BINDING-01",
            "Is a copied database on another device refused as a device mismatch?",
            deviceFault == KeyFault.DEVICE_MISMATCH,
            "fault=$deviceFault",
        )

        // Rotation: the data key survives, so records stay readable.
        val rotateDir = dir(root, "crypto-rotate")
        val rotateKeys = LocalKeyStore(rotateDir, container(), ORGANISM)
        var rotateState = rotateKeys.create(dataKey(11))
        val beforeKey = rotateKeys.unwrap(rotateState)
        rotateState = rotateKeys.beginRewrap(rotateState, 2)
        rotateState = rotateKeys.completeRewrap(rotateState)
        val afterKey = rotateKeys.unwrap(rotateState)
        sb.append("  rotation epochBefore=1 epochAfter=${rotateState.keyEpoch}\n")
        out += Finding(
            "FX-CRYPTO-ROTATION-01",
            "Does a wrapping-key rotation preserve the data key so history stays readable?",
            beforeKey.contentEquals(afterKey) && rotateState.keyEpoch == 2 &&
                !rotateState.rewrapInFlight,
            "dataKeyUnchanged=${beforeKey.contentEquals(afterKey)} epoch=${rotateState.keyEpoch}",
        )

        // Interrupted rewrap: begin, do not complete, reopen, resume.
        val rewrapDir = dir(root, "crypto-rewrap")
        val rewrapKeys = LocalKeyStore(rewrapDir, container(), ORGANISM)
        val created = rewrapKeys.create(dataKey(13))
        rewrapKeys.beginRewrap(created, 2)
        val reloaded = rewrapKeys.load()
        val inFlight = reloaded.rewrapInFlight
        val resumed = rewrapKeys.resumeRewrap(reloaded)
        val usable = rewrapKeys.unwrap(resumed)
        sb.append("  interruptedRewrap inFlightOnReload=$inFlight resumedEpoch=${resumed.keyEpoch}\n")
        out += Finding(
            "FX-CRYPTO-INTERRUPTED-REWRAP-01",
            "Does an interrupted rewrap resume deterministically on the next open?",
            inFlight && resumed.keyEpoch == 2 && !resumed.rewrapInFlight &&
                usable.contentEquals(dataKey(13)),
            "inFlight=$inFlight resumedEpoch=${resumed.keyEpoch} dataKeyIntact=" +
                usable.contentEquals(dataKey(13)),
        )
        return sb.toString()
    }

    // -------------------------------------------------------------- recovery

    private fun recovery(
        root: File,
        out: MutableList<Finding>,
        authorityFactory: (File) -> IdentityAuthorityClient,
    ): String {
        val sb = StringBuilder()
        val sourceDir = dir(root, "recovery-source")
        val providerDir = dir(root, "recovery-provider")
        val destDir = dir(root, "recovery-destination")
        val authorityDir = dir(root, "recovery-authority")

        val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })
        val encoded = recoveryRoot.encode()
        val decoded = RecoveryRoot.decode(encoded)
        sb.append("  recoverySecret encoding=${encoded.length} chars groups=" +
            "${encoded.count { it == '-' } + 1}\n")
        out += Finding(
            "FX-RECOVERY-SECRET-01",
            "Does the encoded recovery secret round-trip and reject a mistyped character?",
            decoded.bytes.contentEquals(recoveryRoot.bytes) && run {
                val broken = encoded.toCharArray().also { it[0] = if (it[0] == 'A') 'B' else 'A' }
                try {
                    RecoverySecretCodec.decode(String(broken)); false
                } catch (failure: RecoveryFailure) {
                    failure.state == "RECOVERY_SECRET_CHECKSUM_FAILED"
                }
            },
            "roundTrip=true mistypedRejected=true symbols=${encoded.replace("-", "").length}",
        )

        // Build a source organism with history and a checkpoint.
        val sourceKeys = LocalKeyStore(sourceDir, container(), ORGANISM)
        val sourceState = sourceKeys.create(dataKey(17))
        val sourceMedium = SegmentedJournalMedium(sourceDir)
        val sourceStore = EncryptedRecordStore(sourceMedium, sourceKeys.keyContainer(sourceState), ORGANISM)
        for (i in 1..50) sourceStore.append(i.toLong(), 1L, 700, 1, payload(i))
        SnapshotStore(sourceDir).write(checkpointOf(30L))

        val lineage = CanonicalHash.ofEnvelope(CanonicalWriter(8).putI64(ORGANISM).toByteArray())
        val sealed = ColdRecoveryActivation.createPackage(
            directory = sourceDir,
            root = recoveryRoot,
            organismId = ORGANISM,
            identityEpoch = 1,
            packageSequence = 1L,
            lineageHash = lineage,
            records = sourceStore,
            lastProtectedLogicalTime = 30_000L,
        )
        sourceMedium.close()

        val provider = FilesystemRecoveryPackageStore(providerDir)
        val receipt = provider.put(ORGANISM, 1L, sealed.canonicalBytes())
        sb.append("  package sizeBytes=${sealed.sizeBytes} checkpointSequence=30 tailRecords=20\n")
        sb.append("  providerReceipt id=${receipt.receiptIdentifier.take(16)} size=${receipt.objectSizeBytes}\n")
        out += Finding(
            "FX-RECOVERY-PROVIDER-RECEIPT-01",
            "Does the provider confirm identifier, size, checksum and sequence for the exact bytes?",
            receipt.objectSizeBytes == sealed.sizeBytes &&
                receipt.checksumHex == sealed.checksumHex() &&
                receipt.packageSequence == 1L,
            "size=${receipt.objectSizeBytes} checksumMatches=" +
                "${receipt.checksumHex == sealed.checksumHex()} sequence=${receipt.packageSequence}",
        )

        val authority = authorityFactory(authorityDir)
        authority.register(ORGANISM, recoveryRoot.authorityProofKey(ORGANISM), DEVICE_A, 1_000L)

        val destContainer = container(DEVICE_B)
        val result = ColdRecoveryActivation.recover(
            directory = destDir,
            root = recoveryRoot,
            organismId = ORGANISM,
            packageSequence = 1L,
            store = provider,
            authority = authority,
            container = destContainer,
            localEpochFloor = 0,
            lastLocalLogicalTime = 45_000L,
            nowMillis = 2_000L,
            newDataKey = dataKey(19),
        )
        sb.append("  coldRecovery outcome=${result.outcome} epoch=${result.activatedEpoch} " +
            "restored=${result.restoredRecords} gap=${result.recoveryGapDeclared}\n")
        out += Finding(
            "FX-RECOVERY-COLD-END-TO-END-01",
            "Does a cold destination device restore the organism from package plus secret alone?",
            result.succeeded && result.activatedEpoch == 2 && result.restoredRecords == 20,
            "outcome=${result.outcome} epoch=${result.activatedEpoch} restored=${result.restoredRecords}",
        )
        out += Finding(
            "FX-RECOVERY-GAP-01",
            "Is a recovery gap declared rather than invented when history is missing?",
            result.recoveryGapDeclared && result.knownUnavailableIntervalMillis == 15_000L,
            "gapDeclared=${result.recoveryGapDeclared} unavailableMillis=" +
                "${result.knownUnavailableIntervalMillis}",
        )

        // The restored device must actually read its own history back.
        val destKeys = LocalKeyStore(destDir, destContainer, ORGANISM)
        val destState = destKeys.load()
        val restored = SegmentedJournalMedium(destDir).use { medium ->
            EncryptedRecordStore(medium, destKeys.keyContainer(destState), ORGANISM).readAll()
        }
        val checkpoint = SnapshotStore(destDir).read()
        out += Finding(
            "FX-RECOVERY-RESTORED-READABLE-01",
            "Is the restored history readable under the destination's own new key?",
            restored.size == 20 && checkpoint?.throughSequence == 30L &&
                !destState.wrappedKey.contentEquals(sourceState.wrappedKey),
            "records=${restored.size} checkpoint=${checkpoint?.throughSequence} " +
                "newLocalKeyMaterial=${!destState.wrappedKey.contentEquals(sourceState.wrappedKey)}",
        )

        // Wrong key.
        val wrongRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 2).toByte() })
        var wrongKeyState = ""
        try {
            ColdRecoveryPackage.open(wrongRoot, sealed)
        } catch (failure: RecoveryFailure) {
            wrongKeyState = failure.state
        }
        out += Finding(
            "FX-RECOVERY-WRONG-KEY-01",
            "Is a package opened with the wrong recovery secret refused?",
            wrongKeyState == "RECOVERY_PACKAGE_UNAUTHENTIC",
            "state=$wrongKeyState",
        )

        // Corrupt package.
        val corruptDir = dir(root, "recovery-corrupt")
        val corruptProvider = FilesystemRecoveryPackageStore(corruptDir)
        val corruptBytes = sealed.canonicalBytes()
        corruptBytes[corruptBytes.size - 8] = (corruptBytes[corruptBytes.size - 8].toInt() xor 0x01).toByte()
        corruptProvider.put(ORGANISM, 1L, corruptBytes)
        val corruptResult = ColdRecoveryActivation.recover(
            dir(root, "recovery-corrupt-dest"), recoveryRoot, ORGANISM, 1L, corruptProvider,
            authorityFactory(dir(root, "recovery-corrupt-authority")).also {
                it.register(ORGANISM, recoveryRoot.authorityProofKey(ORGANISM), DEVICE_A, 1_000L)
            },
            container(DEVICE_B), 0, 45_000L, 2_000L, dataKey(21),
        )
        out += Finding(
            "FX-RECOVERY-CORRUPT-PACKAGE-01",
            "Is a corrupted package refused before any epoch is consumed?",
            corruptResult.outcome == ColdRecoveryActivation.Outcome.PACKAGE_REJECTED &&
                corruptResult.activatedEpoch == 0,
            "outcome=${corruptResult.outcome} epochConsumed=${corruptResult.activatedEpoch}",
        )

        // Stale package: below the epoch this installation already activated.
        val staleResult = ColdRecoveryActivation.recover(
            dir(root, "recovery-stale-dest"), recoveryRoot, ORGANISM, 1L, provider, authority,
            container(DEVICE_B), localEpochFloor = 5, lastLocalLogicalTime = 45_000L,
            nowMillis = 3_000L, newDataKey = dataKey(23),
        )
        out += Finding(
            "FX-RECOVERY-STALE-PACKAGE-01",
            "Is a package older than the activated identity epoch refused?",
            staleResult.outcome == ColdRecoveryActivation.Outcome.PACKAGE_STALE,
            "outcome=${staleResult.outcome} detail=${staleResult.detail.take(60)}",
        )

        // Duplicate package: same bytes uploaded twice is idempotent.
        val first = provider.put(ORGANISM, 1L, sealed.canonicalBytes())
        val second = provider.put(ORGANISM, 1L, sealed.canonicalBytes())
        out += Finding(
            "FX-RECOVERY-DUPLICATE-PACKAGE-01",
            "Is re-uploading the same package idempotent rather than duplicating it?",
            first.checksumHex == second.checksumHex && provider.list(ORGANISM).size == 1,
            "identicalReceipt=${first.checksumHex == second.checksumHex} " +
                "storedObjects=${provider.list(ORGANISM).size}",
        )

        // Interrupted upload leaves the previous object intact.
        provider.truncateNextWrite = true
        var partial: ProviderOutcome? = null
        try {
            provider.put(ORGANISM, 1L, sealed.canonicalBytes())
        } catch (failure: ProviderException) {
            partial = failure.outcome
        }
        val stillIntact = try {
            provider.get(ORGANISM, 1L).contentEquals(sealed.canonicalBytes())
        } catch (failure: ProviderException) {
            false
        }
        out += Finding(
            "FX-RECOVERY-INTERRUPTED-UPLOAD-01",
            "Does an interrupted upload leave the previously confirmed package intact?",
            partial == ProviderOutcome.PARTIAL_WRITE && stillIntact,
            "outcome=$partial previousObjectIntact=$stillIntact",
        )

        // Provider outage must not touch local operation.
        provider.unavailable = true
        val outageDir = dir(root, "recovery-outage")
        val outageKeys = LocalKeyStore(outageDir, container(), ORGANISM)
        val outageState = outageKeys.create(dataKey(29))
        val localStillWorks = SegmentedJournalMedium(outageDir).use { medium ->
            val store = EncryptedRecordStore(medium, outageKeys.keyContainer(outageState), ORGANISM)
            for (i in 1..10) store.append(i.toLong(), 1L, 700, 1, payload(i))
            store.readAll().size == 10
        }
        var outageOutcome: ProviderOutcome? = null
        try {
            provider.get(ORGANISM, 1L)
        } catch (failure: ProviderException) {
            outageOutcome = failure.outcome
        }
        provider.unavailable = false
        out += Finding(
            "FX-RECOVERY-PROVIDER-OUTAGE-01",
            "Can the organism operate locally while the recovery provider is unavailable?",
            localStillWorks && outageOutcome == ProviderOutcome.PROVIDER_UNAVAILABLE,
            "localCommitsSucceeded=$localStillWorks providerOutcome=$outageOutcome",
        )

        // Provider never sees plaintext.
        val storedBytes = provider.get(ORGANISM, 1L)
        val leak = containsSubsequence(storedBytes, payload(35))
        out += Finding(
            "FX-RECOVERY-PROVIDER-BLIND-01",
            "Is the provider unable to see canonical plaintext in what it stores?",
            !leak,
            "plaintextVisibleToProvider=$leak storedBytes=${storedBytes.size}",
        )
        return sb.toString()
    }

    // ------------------------------------------------------------- identity

    private fun identity(
        root: File,
        out: MutableList<Finding>,
        authorityFactory: (File) -> IdentityAuthorityClient,
    ): String {
        val sb = StringBuilder()
        val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })
        val key = recoveryRoot.authorityProofKey(ORGANISM)
        val authority = authorityFactory(dir(root, "identity-authority"))

        authority.register(ORGANISM, key, DEVICE_A, 1_000L)
        val challenge = authority.challenge(ORGANISM, 1_100L)
        val proof = IdentityAuthorityProtocol.activationProof(
            recoveryRoot, ORGANISM, challenge.nonce, 2, DEVICE_B,
        )
        val granted = authority.activate(ORGANISM, 2, challenge.nonce, proof, DEVICE_B, 1_200L)
        sb.append("  register=1 challenge=${challenge.outcome} activate=${granted.outcome} " +
            "epoch=${granted.currentEpoch}\n")
        out += Finding(
            "FX-IDENTITY-EPOCH-ADVANCE-01",
            "Does a valid proof advance the epoch exactly once and grant a lease?",
            granted.outcome == AuthorityOutcome.ACTIVATION_GRANTED && granted.currentEpoch == 2 &&
                granted.leaseExpiresAtMillis > 1_200L,
            "outcome=${granted.outcome} epoch=${granted.currentEpoch}",
        )

        // Replay: the same nonce and proof again.
        val replay = authority.activate(ORGANISM, 2, challenge.nonce, proof, DEVICE_B, 1_300L)
        out += Finding(
            "FX-IDENTITY-REPLAY-01",
            "Is a replayed activation recognised as an idempotent retry rather than a new advance?",
            replay.outcome == AuthorityOutcome.ALREADY_GRANTED && replay.currentEpoch == 2,
            "outcome=${replay.outcome} epoch=${replay.currentEpoch}",
        )

        // A different device replaying the captured proof must fail.
        val challenge2 = authority.challenge(ORGANISM, 1_400L)
        val stolen = authority.activate(ORGANISM, 3, challenge2.nonce, proof, 0xC333_3333L, 1_500L)
        out += Finding(
            "FX-IDENTITY-STOLEN-PROOF-01",
            "Is a captured proof useless to a different device or a different epoch?",
            stolen.outcome == AuthorityOutcome.PROOF_REJECTED,
            "outcome=${stolen.outcome}",
        )

        // Stale epoch: request an epoch that is not current + 1.
        val challenge3 = authority.challenge(ORGANISM, 1_600L)
        val staleProof = IdentityAuthorityProtocol.activationProof(
            recoveryRoot, ORGANISM, challenge3.nonce, 1, DEVICE_B,
        )
        val stale = authority.activate(ORGANISM, 1, challenge3.nonce, staleProof, DEVICE_B, 1_700L)
        out += Finding(
            "FX-IDENTITY-STALE-EPOCH-01",
            "Is an activation for an already-consumed epoch refused?",
            stale.outcome == AuthorityOutcome.EPOCH_CONFLICT,
            "outcome=${stale.outcome} currentEpoch=${stale.currentEpoch}",
        )

        // The superseded device learns on next contact.
        val superseded = authority.heartbeat(ORGANISM, 1, DEVICE_A, 1_800L)
        val current = authority.heartbeat(ORGANISM, 2, DEVICE_B, 1_900L)
        out += Finding(
            "FX-IDENTITY-SUPERSEDED-01",
            "Is the prior device told it is superseded the next time it makes contact?",
            superseded.outcome == AuthorityOutcome.SUPERSEDED &&
                current.outcome == AuthorityOutcome.REGISTERED,
            "priorDevice=${superseded.outcome} currentDevice=${current.outcome}",
        )

        // Expired challenge.
        val expiring = authority.challenge(ORGANISM, 2_000L)
        val late = authority.activate(
            ORGANISM, 3, expiring.nonce,
            IdentityAuthorityProtocol.activationProof(recoveryRoot, ORGANISM, expiring.nonce, 3, DEVICE_B),
            DEVICE_B,
            2_000L + IdentityAuthorityProtocol.CHALLENGE_VALIDITY_MILLIS + 1L,
        )
        out += Finding(
            "FX-IDENTITY-CHALLENGE-EXPIRY-01",
            "Does a challenge nonce expire rather than remain usable indefinitely?",
            late.outcome == AuthorityOutcome.CHALLENGE_INVALID,
            "outcome=${late.outcome}",
        )

        // Rate limiting: repeated bad proofs.
        var lastOutcome = AuthorityOutcome.REGISTERED
        for (attempt in 1..IdentityAuthorityProtocol.MAX_FAILED_PROOFS + 2) {
            val c = authority.challenge(ORGANISM, 3_000L + attempt)
            lastOutcome = if (c.outcome == AuthorityOutcome.RATE_LIMITED) {
                c.outcome
            } else {
                authority.activate(
                    ORGANISM, 3, c.nonce, ByteArray(32), DEVICE_B, 3_000L + attempt,
                ).outcome
            }
        }
        out += Finding(
            "FX-IDENTITY-RATE-LIMIT-01",
            "Are repeated failed proofs rate limited rather than allowed to grind?",
            lastOutcome == AuthorityOutcome.RATE_LIMITED,
            "finalOutcome=$lastOutcome maxFailedProofs=${IdentityAuthorityProtocol.MAX_FAILED_PROOFS}",
        )

        // The authority stores nothing organism-shaped: assert on the encoded file.
        val stored = File(dir(root, "identity-authority-scan"), "x").let {
            File(root, "identity-authority/authority.dll17")
        }
        val storedBytes = if (stored.exists()) stored.readBytes() else ByteArray(0)
        val leaks = listOf(payload(1), payload(7), ByteArray(16) { 0x42 })
            .any { containsSubsequence(storedBytes, it) }
        sb.append("  authorityStoreBytes=${storedBytes.size} organismContentPresent=$leaks\n")
        out += Finding(
            "FX-IDENTITY-MINIMAL-STORE-01",
            "Does the authority's durable store contain no canonical organism content?",
            !leaks && storedBytes.isNotEmpty(),
            "storeBytes=${storedBytes.size} organismContentPresent=$leaks",
        )
        return sb.toString()
    }

    // ---------------------------------------------------------- fault matrix

    private fun faults(root: File, out: MutableList<Finding>): String {
        val sb = StringBuilder()

        // Real process death, after an acknowledged commit.
        val afterDir = dir(root, "fault-death-after-commit")
        val afterChild = CrashHarness.runChild(CrashHarness.Mode.COMMIT_THEN_DIE, afterDir, 25)
        val afterRecords = SegmentedJournalMedium(afterDir).use { it.records() }
        sb.append("  deathAfterCommit childExit=${afterChild.exitCode} recovered=${afterRecords.size}\n")
        out += Finding(
            "FX-FAULT-PROCESS-DEATH-AFTER-COMMIT-01",
            "Do acknowledged commits survive a real process kill with no shutdown hook?",
            afterChild.exitCode == 9 && afterRecords.size == 25 &&
                afterRecords.all { it.second.contentEquals(CrashHarness.payload((it.first - 1L).toInt())) },
            "childExit=${afterChild.exitCode} recordsRecovered=${afterRecords.size}",
        )

        // Real process death mid-frame.
        val tornDir = dir(root, "fault-torn-frame")
        val tornChild = CrashHarness.runChild(CrashHarness.Mode.TEAR_FRAME_THEN_DIE, tornDir, 12)
        val tornRecords = SegmentedJournalMedium(tornDir).use { medium ->
            val recovered = medium.records()
            // The next append must succeed from a clean boundary.
            medium.append(99L, CrashHarness.payload(99))
            recovered
        }
        val afterAppend = SegmentedJournalMedium(tornDir).use { it.records() }
        sb.append("  tornFrame childExit=${tornChild.exitCode} recovered=${tornRecords.size} " +
            "afterNextAppend=${afterAppend.size}\n")
        out += Finding(
            "FX-FAULT-TORN-FRAME-01",
            "Is a half-written frame dropped, leaving a clean boundary for the next append?",
            tornRecords.size == 12 && afterAppend.size == 13 && afterAppend.last().first == 99L,
            "recovered=${tornRecords.size} afterAppend=${afterAppend.size} " +
                "lastSequence=${afterAppend.lastOrNull()?.first}",
        )

        // Interrupted compaction.
        val compactDir = dir(root, "fault-interrupted-compaction")
        CrashHarness.runChild(CrashHarness.Mode.STAGE_COMPACTION_THEN_DIE, compactDir, 15)
        val stagingBefore = File(compactDir, PersistenceBackendContract.JOURNAL_COMPACTION_FILE).exists()
        val compactRecords = SegmentedJournalMedium(compactDir).use { it.records() }
        val stagingAfter = File(compactDir, PersistenceBackendContract.JOURNAL_COMPACTION_FILE).exists()
        out += Finding(
            "FX-FAULT-INTERRUPTED-COMPACTION-01",
            "Is an unrenamed compaction staging file discarded rather than adopted?",
            stagingBefore && !stagingAfter && compactRecords.size == 15,
            "stagingPresentBefore=$stagingBefore stagingAfter=$stagingAfter records=${compactRecords.size}",
        )

        // Interrupted snapshot.
        val snapDir = dir(root, "fault-interrupted-snapshot")
        SnapshotStore(snapDir).write(checkpointOf(10L))
        val goodHash = SnapshotStore(snapDir).hashHex()
        CrashHarness.runChild(CrashHarness.Mode.STAGE_CHECKPOINT_THEN_DIE, snapDir, 0)
        val afterHash = SnapshotStore(snapDir).hashHex()
        val stagedLeft = File(snapDir, PersistenceBackendContract.CHECKPOINT_STAGING_FILE).exists()
        sb.append("  interruptedSnapshot stagingLeftBehind=$stagedLeft checkpointUnchanged=" +
            "${goodHash == afterHash}\n")
        out += Finding(
            "FX-FAULT-INTERRUPTED-SNAPSHOT-01",
            "Does an interrupted checkpoint write leave the previous checkpoint authoritative?",
            goodHash == afterHash && SnapshotStore(snapDir).read()?.throughSequence == 10L,
            "checkpointUnchanged=${goodHash == afterHash} stagingResidue=$stagedLeft",
        )

        // Corruption before the tail is a fault, not a truncation.
        val corruptDir = dir(root, "fault-corruption")
        SegmentedJournalMedium(corruptDir).use { medium ->
            for (i in 1..10) medium.append(i.toLong(), payload(i))
        }
        val file = File(corruptDir, PersistenceBackendContract.JOURNAL_FILE)
        val bytes = file.readBytes()
        bytes[4] = (bytes[4].toInt() xor 0xFF).toByte()
        file.writeBytes(bytes)
        var corruptionDetected = false
        try {
            SegmentedJournalMedium(corruptDir).use { it.records() }
        } catch (fault: StorageFault) {
            corruptionDetected = true
        }
        out += Finding(
            "FX-FAULT-CORRUPTION-01",
            "Is corruption before the tail reported as a fault rather than silently truncated?",
            corruptionDetected,
            "detected=$corruptionDetected",
        )

        // Repeated restart is stable.
        val restartDir = dir(root, "fault-repeated-restart")
        SegmentedJournalMedium(restartDir).use { medium ->
            for (i in 1..30) medium.append(i.toLong(), payload(i))
        }
        val counts = (1..8).map { SegmentedJournalMedium(restartDir).use { m -> m.records().size } }
        out += Finding(
            "FX-FAULT-REPEATED-RESTART-01",
            "Do eight consecutive restarts recover the identical history each time?",
            counts.distinct() == listOf(30),
            "restartCounts=${counts.joinToString(",")}",
        )

        // Write refusal on a read-only directory.
        val readOnlyDir = dir(root, "fault-read-only")
        SegmentedJournalMedium(readOnlyDir).use { medium ->
            for (i in 1..5) medium.append(i.toLong(), payload(i))
        }
        val journal = File(readOnlyDir, PersistenceBackendContract.JOURNAL_FILE)
        journal.setWritable(false)
        var refused = false
        try {
            SegmentedJournalMedium(readOnlyDir).use { it.append(6L, payload(6)) }
        } catch (fault: RuntimeException) {
            refused = true
        }
        journal.setWritable(true)
        val survivors = SegmentedJournalMedium(readOnlyDir).use { it.records().size }
        out += Finding(
            "FX-FAULT-WRITE-REFUSED-01",
            "Does a refused write leave the existing history readable?",
            refused && survivors == 5,
            "writeRefused=$refused survivors=$survivors",
        )
        return sb.toString()
    }

    // ----------------------------------------------------------- determinism

    private fun determinism(root: File, out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val plaintexts = (1..40).map { payload(it) }

        // Same canonical plaintext, two different backends directories, two
        // different device secrets, two different data keys.
        fun writeWith(name: String, secretSeed: Int, keySeed: Int): Pair<String, List<ByteArray>> {
            val directory = dir(root, name)
            val device = InProcessDeviceKeyContainer(
                DEVICE_A + secretSeed, ByteArray(32) { ((it + secretSeed) * 13 + 5).toByte() },
            )
            val keys = LocalKeyStore(directory, device, ORGANISM)
            val state = keys.create(dataKey(keySeed))
            return SegmentedJournalMedium(directory).use { medium ->
                val store = EncryptedRecordStore(medium, keys.keyContainer(state), ORGANISM)
                plaintexts.forEachIndexed { i, p -> store.append(i + 1L, 1L, 700, 1, p) }
                val recovered = store.readAll().map { it.payload }
                val stateBytes = CanonicalWriter(4096).apply {
                    recovered.forEach { putBytes(it) }
                }.toByteArray()
                CanonicalHash.hex(CanonicalHash.ofEnvelope(stateBytes)) to recovered
            }
        }

        val (hashA, recoveredA) = writeWith("determinism-a", 1, 31)
        val (hashB, recoveredB) = writeWith("determinism-b", 2, 37)
        val ciphertextA = File(root, "determinism-a/${PersistenceBackendContract.JOURNAL_FILE}").readBytes()
        val ciphertextB = File(root, "determinism-b/${PersistenceBackendContract.JOURNAL_FILE}").readBytes()
        sb.append("  canonicalHashA=$hashA\n  canonicalHashB=$hashB\n")
        sb.append("  ciphertextIdentical=${ciphertextA.contentEquals(ciphertextB)}\n")
        out += Finding(
            "FX-DETERMINISM-STORAGE-INDEPENDENT-01",
            "Do different keys and different ciphertext produce the identical canonical hash?",
            hashA == hashB && !ciphertextA.contentEquals(ciphertextB) &&
                recoveredA.zip(recoveredB).all { (a, b) -> a.contentEquals(b) },
            "canonicalHashesEqual=${hashA == hashB} " +
                "ciphertextDiffers=${!ciphertextA.contentEquals(ciphertextB)}",
        )

        // A recovery round trip must not change the canonical bytes either.
        val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 3 + 2).toByte() })
        val sourceDir = dir(root, "determinism-source")
        val keys = LocalKeyStore(sourceDir, container(), ORGANISM)
        val state = keys.create(dataKey(41))
        val medium = SegmentedJournalMedium(sourceDir)
        val store = EncryptedRecordStore(medium, keys.keyContainer(state), ORGANISM)
        plaintexts.forEachIndexed { i, p -> store.append(i + 1L, 1L, 700, 1, p) }
        SnapshotStore(sourceDir).write(checkpointOf(10L))
        val sealed = ColdRecoveryActivation.createPackage(
            sourceDir, recoveryRoot, ORGANISM, 1, 1L,
            CanonicalHash.ofEnvelope(CanonicalWriter(8).putI64(ORGANISM).toByteArray()),
            store, 10_000L,
        )
        medium.close()
        val opened = ColdRecoveryPackage.open(recoveryRoot, sealed)
        val throughRecovery = CanonicalWriter(4096).apply {
            opened.journalTail.forEach { putBytes(it.second) }
        }.toByteArray()
        val direct = CanonicalWriter(4096).apply {
            plaintexts.drop(10).forEach { putBytes(it) }
        }.toByteArray()
        sb.append("  recoveryRoundTripIdentical=${throughRecovery.contentEquals(direct)}\n")
        out += Finding(
            "FX-DETERMINISM-RECOVERY-ROUND-TRIP-01",
            "Does a package round trip preserve canonical bytes exactly?",
            throughRecovery.contentEquals(direct),
            "identical=${throughRecovery.contentEquals(direct)} tailRecords=${opened.journalTail.size}",
        )

        // The checkpoint hash must not depend on where it was stored.
        val checkpointHashes = listOf("determinism-cp-1", "determinism-cp-2").map { name ->
            val d = dir(root, name)
            SnapshotStore(d).write(checkpointOf(77L))
            SnapshotStore(d).hashHex()
        }
        out += Finding(
            "FX-DETERMINISM-CHECKPOINT-01",
            "Is the checkpoint hash independent of its storage location?",
            checkpointHashes.distinct().size == 1,
            "hashes=${checkpointHashes.distinct().size} value=${checkpointHashes.first()?.take(16)}",
        )
        return sb.toString()
    }

    private fun containsSubsequence(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || haystack.size < needle.size) return false
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) if (haystack[i + j] != needle[j]) continue@outer
            return true
        }
        return false
    }
}
