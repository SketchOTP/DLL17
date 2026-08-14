package com.animusmachinae.dll17.core.recovery.net

import com.animusmachinae.dll17.core.recovery.AuthorityOutcome
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityProtocol
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityTransport
import com.animusmachinae.dll17.core.recovery.RecoveryRoot
import com.animusmachinae.dll17.core.recovery.TransportFailure
import com.animusmachinae.dll17.core.recovery.TransportFault
import com.animusmachinae.dll17.services.identity.IdentityAuthorityHttpServer
import com.animusmachinae.dll17.services.identity.IdentityAuthorityService
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `IdentityAuthorityTransportContractV1`.
 *
 * The governing question in every test here is the same one: **did the transport
 * change a protocol decision?** It must not be able to. Epochs, challenges,
 * leases, replay protection, rate limiting, idempotency and activation outcomes
 * belong to `IdentityAuthorityProtocolV1`, and this layer only carries them.
 */
class IdentityAuthorityTransportTest {

    private lateinit var directory: File
    private lateinit var service: IdentityAuthorityService
    private lateinit var server: IdentityAuthorityHttpServer
    private lateinit var client: HttpIdentityAuthorityClient

    private val organism = 0x0D14_0001L
    private val device = 0x11L
    private val otherDevice = 0x22L
    private val root = RecoveryRoot(ByteArray(32) { (it * 7 + 3).toByte() })

    private var clockMillis: Long = 1_000_000L

    @BeforeTest
    fun start() {
        directory = Files.createTempDirectory("dll17-authority-transport").toFile()
        service = IdentityAuthorityService(directory)
        server = IdentityAuthorityHttpServer(service, directory, clock = { clockMillis })
        server.start()
        client = HttpIdentityAuthorityClient("http://127.0.0.1:${server.port}")
    }

    @AfterTest
    fun stop() {
        server.stop()
        directory.deleteRecursively()
    }

    private fun verificationKey() = root.authorityProofKey(organism)

    private fun proofFor(epoch: Int, nonce: ByteArray, fingerprint: Long = device) =
        IdentityAuthorityProtocol.activationProof(root, organism, nonce, epoch, fingerprint)

    private fun register() =
        client.register(organism, verificationKey(), device, clockMillis)

    // ------------------------------------------------------ protocol carriage

    @Test
    fun `registration over HTTP establishes epoch one`() {
        val response = register()
        assertEquals(AuthorityOutcome.REGISTERED, response.outcome)
        assertEquals(1, response.currentEpoch)
        assertEquals(organism, response.organismId)
    }

    @Test
    fun `a full activation over HTTP advances the epoch exactly once`() {
        register()
        val challenge = client.challenge(organism, clockMillis)
        assertEquals(AuthorityOutcome.CHALLENGE_ISSUED, challenge.outcome)
        val granted = client.activate(
            organism, 2, challenge.nonce, proofFor(2, challenge.nonce), device, clockMillis,
        )
        assertEquals(AuthorityOutcome.ACTIVATION_GRANTED, granted.outcome)
        assertEquals(2, granted.currentEpoch)
        assertEquals(2, service.record(organism)?.currentEpoch)
    }

    @Test
    fun `a duplicate activation request does not consume a second epoch`() {
        register()
        val challenge = client.challenge(organism, clockMillis)
        val proof = proofFor(2, challenge.nonce)
        val first = client.activate(organism, 2, challenge.nonce, proof, device, clockMillis)
        val second = client.activate(organism, 2, challenge.nonce, proof, device, clockMillis)
        assertEquals(AuthorityOutcome.ACTIVATION_GRANTED, first.outcome)
        // The retry is safe because the protocol makes it safe, not because the
        // transport deduplicated anything.
        assertEquals(AuthorityOutcome.ALREADY_GRANTED, second.outcome)
        assertEquals(2, service.record(organism)?.currentEpoch)
    }

    @Test
    fun `a replayed nonce is refused`() {
        register()
        val challenge = client.challenge(organism, clockMillis)
        client.activate(organism, 2, challenge.nonce, proofFor(2, challenge.nonce), device, clockMillis)
        val replayed = client.activate(
            organism, 3, challenge.nonce, proofFor(3, challenge.nonce), device, clockMillis,
        )
        assertEquals(AuthorityOutcome.CHALLENGE_INVALID, replayed.outcome)
        assertEquals(2, service.record(organism)?.currentEpoch)
    }

    @Test
    fun `an expired nonce is refused after the transport carried it`() {
        register()
        val challenge = client.challenge(organism, clockMillis)
        clockMillis += IdentityAuthorityProtocol.CHALLENGE_VALIDITY_MILLIS + 1
        val late = client.activate(
            organism, 2, challenge.nonce, proofFor(2, challenge.nonce), device, clockMillis,
        )
        assertEquals(AuthorityOutcome.CHALLENGE_INVALID, late.outcome)
    }

    @Test
    fun `a forged proof is rejected and rate limiting still applies over HTTP`() {
        register()
        repeat(IdentityAuthorityProtocol.MAX_FAILED_PROOFS) {
            val challenge = client.challenge(organism, clockMillis)
            val rejected = client.activate(
                organism, 2, challenge.nonce, ByteArray(32) { 0x5A }, device, clockMillis,
            )
            assertEquals(AuthorityOutcome.PROOF_REJECTED, rejected.outcome)
        }
        val limited = client.challenge(organism, clockMillis)
        assertEquals(AuthorityOutcome.RATE_LIMITED, limited.outcome)
        assertEquals(429, client.lastHttpStatus)
    }

    @Test
    fun `two destinations racing over HTTP produce exactly one winner`() {
        register()
        val challengeA = client.challenge(organism, clockMillis)
        val challengeB = client.challenge(organism, clockMillis)
        val clientB = HttpIdentityAuthorityClient("http://127.0.0.1:${server.port}")

        val pool = Executors.newFixedThreadPool(2)
        val results = pool.invokeAll(
            listOf(
                Callable {
                    client.activate(
                        organism, 2, challengeA.nonce, proofFor(2, challengeA.nonce), device, clockMillis,
                    )
                },
                Callable {
                    clientB.activate(
                        organism, 2, challengeB.nonce,
                        proofFor(2, challengeB.nonce, otherDevice), otherDevice, clockMillis,
                    )
                },
            ),
        ).map { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(
            1,
            results.count { it.outcome == AuthorityOutcome.ACTIVATION_GRANTED },
            "exactly one destination may win: ${results.map { it.outcome }}",
        )
        assertEquals(2, service.record(organism)?.currentEpoch)
    }

    @Test
    fun `a superseded device learns through heartbeat over HTTP`() {
        register()
        val challenge = client.challenge(organism, clockMillis)
        client.activate(
            organism, 2, challenge.nonce, proofFor(2, challenge.nonce, otherDevice),
            otherDevice, clockMillis,
        )
        val beat = client.heartbeat(organism, 1, device, clockMillis)
        assertEquals(AuthorityOutcome.SUPERSEDED, beat.outcome)
        assertEquals(409, client.lastHttpStatus)
    }

    @Test
    fun `an unregistered organism is UNKNOWN_ORGANISM rather than a transport error`() {
        val response = client.challenge(0xDEADL, clockMillis)
        assertEquals(AuthorityOutcome.UNKNOWN_ORGANISM, response.outcome)
        assertEquals(404, client.lastHttpStatus)
    }

    // ----------------------------------------------------- transport behaviour

    @Test
    fun `the client cannot name the current time`() {
        register()
        val challenge = client.challenge(organism, clockMillis)
        // The client passes a `nowMillis` far in the future. If the transport
        // carried it, the nonce would already have expired at the server. It
        // does not, so the activation succeeds under the server's clock.
        val granted = client.activate(
            organism, 2, challenge.nonce, proofFor(2, challenge.nonce), device,
            clockMillis + 100L * IdentityAuthorityProtocol.CHALLENGE_VALIDITY_MILLIS,
        )
        assertEquals(AuthorityOutcome.ACTIVATION_GRANTED, granted.outcome)
    }

    @Test
    fun `the response never carries the service's free-text detail`() {
        register()
        val response = client.challenge(organism, clockMillis)
        assertEquals("", response.detail, "detail is a server-side diagnostic and must not cross the wire")
    }

    @Test
    fun `a malformed body is a transport failure and not a protocol outcome`() {
        val status = raw(
            IdentityAuthorityTransport.PATH_ACTIVATE,
            "POST",
            ByteArray(48) { 0x7F },
            IdentityAuthorityTransport.MEDIA_TYPE,
            "0".repeat(32),
        )
        assertEquals(TransportFailure.MALFORMED.httpStatus, status.first)
        assertTrue(status.second.isEmpty(), "a transport refusal must not be decodable as a protocol response")
    }

    @Test
    fun `a body for the wrong endpoint is refused`() {
        register()
        val status = raw(
            IdentityAuthorityTransport.PATH_HEARTBEAT,
            "POST",
            IdentityAuthorityTransport.encodeChallenge(organism),
            IdentityAuthorityTransport.MEDIA_TYPE,
            "0".repeat(32),
        )
        assertEquals(400, status.first)
    }

    @Test
    fun `an oversized body is refused rather than truncated`() {
        val status = raw(
            IdentityAuthorityTransport.PATH_CHALLENGE,
            "POST",
            ByteArray(IdentityAuthorityTransport.MAX_REQUEST_BYTES + 512),
            IdentityAuthorityTransport.MEDIA_TYPE,
            "0".repeat(32),
        )
        assertEquals(TransportFailure.TOO_LARGE.httpStatus, status.first)
    }

    @Test
    fun `the wrong media type and the wrong method are both refused`() {
        assertEquals(
            TransportFailure.UNSUPPORTED_MEDIA_TYPE.httpStatus,
            raw(
                IdentityAuthorityTransport.PATH_CHALLENGE, "POST",
                IdentityAuthorityTransport.encodeChallenge(organism), "application/json", "0".repeat(32),
            ).first,
        )
        assertEquals(
            TransportFailure.METHOD_NOT_ALLOWED.httpStatus,
            raw(
                IdentityAuthorityTransport.PATH_CHALLENGE, "GET",
                ByteArray(0), IdentityAuthorityTransport.MEDIA_TYPE, "0".repeat(32),
            ).first,
        )
    }

    @Test
    fun `a missing or malformed request id is refused`() {
        assertEquals(
            TransportFailure.MISSING_REQUEST_ID.httpStatus,
            raw(
                IdentityAuthorityTransport.PATH_CHALLENGE, "POST",
                IdentityAuthorityTransport.encodeChallenge(organism),
                IdentityAuthorityTransport.MEDIA_TYPE, null,
            ).first,
        )
        assertEquals(
            TransportFailure.MISSING_REQUEST_ID.httpStatus,
            raw(
                IdentityAuthorityTransport.PATH_CHALLENGE, "POST",
                IdentityAuthorityTransport.encodeChallenge(organism),
                IdentityAuthorityTransport.MEDIA_TYPE, "not-hex",
            ).first,
        )
    }

    @Test
    fun `an unreachable authority is a transport fault and never a refusal`() {
        val gone = HttpIdentityAuthorityClient("http://127.0.0.1:1", connectTimeoutMillis = 500)
        val fault = assertFailsWith<TransportFault> { gone.challenge(organism, clockMillis) }
        assertEquals(TransportFailure.TRANSPORT_UNAVAILABLE, fault.failure)
    }

    @Test
    fun `liveness stays up when durable state is unusable but readiness does not`() {
        register()
        assertTrue(client.ready())
        assertTrue(directory.setWritable(false))
        try {
            assertEquals(200, statusOf(IdentityAuthorityTransport.PATH_HEALTH))
            assertFalse(client.ready(), "readiness must fail when an epoch could not be persisted")
            // The dangerous failure is granting an activation that cannot be
            // written down: two devices would then hold the same organism.
            val refused = raw(
                IdentityAuthorityTransport.PATH_ACTIVATE, "POST",
                IdentityAuthorityTransport.encodeActivate(organism, 2, ByteArray(16), ByteArray(32), device),
                IdentityAuthorityTransport.MEDIA_TYPE, "0".repeat(32),
            )
            assertEquals(TransportFailure.DURABLE_STATE_UNAVAILABLE.httpStatus, refused.first)
        } finally {
            directory.setWritable(true)
        }
        assertEquals(1, service.record(organism)?.currentEpoch)
    }

    @Test
    fun `a restarted service still holds the epoch it granted`() {
        register()
        val challenge = client.challenge(organism, clockMillis)
        client.activate(organism, 2, challenge.nonce, proofFor(2, challenge.nonce), device, clockMillis)
        server.stop()

        val revived = IdentityAuthorityService(directory)
        val revivedServer = IdentityAuthorityHttpServer(revived, directory, clock = { clockMillis })
        revivedServer.start()
        try {
            val revivedClient = HttpIdentityAuthorityClient("http://127.0.0.1:${revivedServer.port}")
            assertEquals(2, revivedClient.heartbeat(organism, 2, device, clockMillis).currentEpoch)
            // A device still holding the pre-recovery epoch is still superseded.
            assertEquals(
                AuthorityOutcome.SUPERSEDED,
                revivedClient.heartbeat(organism, 1, otherDevice, clockMillis).outcome,
            )
        } finally {
            revivedServer.stop()
        }
    }

    @Test
    fun `the access log carries no organism content`() {
        register()
        client.challenge(organism, clockMillis)
        val log = server.accessLogSnapshot()
        assertTrue(log.isNotEmpty())
        for (line in log) {
            assertTrue(
                Regex("^/\\S+ organism=-?\\d+ outcome=[A-Z_]+ request=\\S+$").matches(line),
                "unexpected log shape: $line",
            )
        }
    }

    @Test
    fun `the request id is echoed so an operator can correlate a call`() {
        register()
        assertTrue(IdentityAuthorityTransport.isWellFormedRequestId(client.lastRequestId))
    }

    // ------------------------------------------------------------- internals

    private fun statusOf(path: String): Int {
        val connection = URL("http://127.0.0.1:${server.port}$path").openConnection() as HttpURLConnection
        return try {
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    /** Sends a request the typed client would refuse to construct. */
    private fun raw(
        path: String,
        method: String,
        body: ByteArray,
        contentType: String,
        requestId: String?,
    ): Pair<Int, ByteArray> {
        val connection = URL("http://127.0.0.1:${server.port}$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.setRequestProperty("content-type", contentType)
            requestId?.let {
                connection.setRequestProperty(IdentityAuthorityTransport.REQUEST_ID_HEADER, it)
            }
            if (body.isNotEmpty() && method == "POST") {
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            status to (stream?.use { it.readBytes() } ?: ByteArray(0))
        } finally {
            connection.disconnect()
        }
    }
}
