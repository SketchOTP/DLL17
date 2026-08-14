package com.animusmachinae.dll17.core.recovery.net

import com.animusmachinae.dll17.core.recovery.AuthorityResponse
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityClient
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityProtocol
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityTransport
import com.animusmachinae.dll17.core.recovery.TransportFailure
import com.animusmachinae.dll17.core.recovery.TransportFault
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.SecureRandom

/**
 * The device-side identity-authority client, over HTTP.
 *
 * It implements the same [IdentityAuthorityClient] the in-process service does,
 * which is the point: `ColdRecoveryActivation` cannot tell the difference, and
 * every property D011 qualified about the recovery flow was qualified against
 * that interface rather than against a particular implementation.
 *
 * ### Retries are the caller's business, not this class's
 *
 * There is no retry loop here, and that is deliberate. `register`, `challenge`
 * and `heartbeat` are safe to repeat; `activate` is safe to repeat *only* because
 * the protocol makes it idempotent through the epoch compare-and-swap and
 * `ALREADY_GRANTED`. Burying a retry inside the transport would hide which of
 * those two facts a given retry is relying on. The qualification proves the
 * protocol survives duplicates (`FX-NET-AUTH-DUPLICATE-ACTIVATE-01`); the
 * transport just does not invent extra ones.
 *
 * ### Failures do not become refusals
 *
 * An unreachable authority raises [TransportFault]. It never becomes an
 * [com.animusmachinae.dll17.core.recovery.AuthorityOutcome], because
 * `ColdRecoveryActivation` distinguishes `AUTHORITY_UNAVAILABLE` from
 * `AUTHORITY_REFUSED` and a client that collapsed the two would make a network
 * outage look like a rejected identity.
 */
public class HttpIdentityAuthorityClient(
    private val baseUrl: String,
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 20_000,
    private val requestIds: () -> String = ::randomRequestId,
) : IdentityAuthorityClient {

    public companion object {
        private val RANDOM = SecureRandom()

        public fun randomRequestId(): String {
            val bytes = ByteArray(IdentityAuthorityTransport.REQUEST_ID_LENGTH / 2)
            RANDOM.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    /** The request id the last call sent. Correlation evidence, never canonical. */
    public var lastRequestId: String? = null
        private set

    public var lastHttpStatus: Int = 0
        private set

    init {
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "authority base URL must be an absolute http(s) URL"
        }
    }

    /** True when the transport is unencrypted. Production deployment must refuse this. */
    public val plaintextTransport: Boolean get() = baseUrl.startsWith("http://")

    override fun register(
        organismId: Long,
        verificationKey: ByteArray,
        deviceFingerprint: Long,
        nowMillis: Long,
    ): AuthorityResponse = call(
        IdentityAuthorityTransport.PATH_REGISTER,
        IdentityAuthorityTransport.encodeRegister(organismId, verificationKey, deviceFingerprint),
    )

    override fun challenge(organismId: Long, nowMillis: Long): AuthorityResponse = call(
        IdentityAuthorityTransport.PATH_CHALLENGE,
        IdentityAuthorityTransport.encodeChallenge(organismId),
    )

    override fun activate(
        organismId: Long,
        requestedEpoch: Int,
        nonce: ByteArray,
        proof: ByteArray,
        deviceFingerprint: Long,
        nowMillis: Long,
    ): AuthorityResponse = call(
        IdentityAuthorityTransport.PATH_ACTIVATE,
        IdentityAuthorityTransport.encodeActivate(
            organismId, requestedEpoch, nonce, proof, deviceFingerprint,
        ),
    )

    override fun heartbeat(
        organismId: Long,
        claimedEpoch: Int,
        deviceFingerprint: Long,
        nowMillis: Long,
    ): AuthorityResponse = call(
        IdentityAuthorityTransport.PATH_HEARTBEAT,
        IdentityAuthorityTransport.encodeHeartbeat(organismId, claimedEpoch, deviceFingerprint),
    )

    /** `GET /readyz`. True only when the service reports its durable state usable. */
    public fun ready(): Boolean = try {
        val connection = open(IdentityAuthorityTransport.PATH_READY, "GET")
        try {
            connection.responseCode == 200
        } finally {
            connection.disconnect()
        }
    } catch (io: IOException) {
        false
    }

    // ------------------------------------------------------------- internals

    private fun open(path: String, method: String): HttpURLConnection {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = connectTimeoutMillis
        connection.readTimeout = readTimeoutMillis
        connection.instanceFollowRedirects = false
        return connection
    }

    private fun call(path: String, body: ByteArray): AuthorityResponse {
        val requestId = requestIds()
        lastRequestId = requestId
        val connection = try {
            open(path, "POST")
        } catch (io: IOException) {
            throw TransportFault(TransportFailure.TRANSPORT_UNAVAILABLE, "cannot reach the authority: ${io.message}")
        }
        try {
            connection.setRequestProperty("content-type", IdentityAuthorityTransport.MEDIA_TYPE)
            connection.setRequestProperty(IdentityAuthorityTransport.REQUEST_ID_HEADER, requestId)
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }

            val status = connection.responseCode
            lastHttpStatus = status
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.use { it.readBytes() } ?: ByteArray(0)

            // A protocol refusal carries a decodable body; a transport failure
            // does not. That is the only test needed, and it is the reason the
            // body is authoritative and the status advisory.
            return try {
                IdentityAuthorityTransport.decodeResponse(payload)
            } catch (undecodable: RuntimeException) {
                throw TransportFault(
                    transportFailureFor(status),
                    "authority returned HTTP $status with no decodable protocol response",
                )
            }
        } catch (timeout: SocketTimeoutException) {
            throw TransportFault(TransportFailure.TIMED_OUT, "authority timed out: ${timeout.message}")
        } catch (refused: ConnectException) {
            throw TransportFault(
                TransportFailure.TRANSPORT_UNAVAILABLE, "authority refused the connection: ${refused.message}",
            )
        } catch (unknown: UnknownHostException) {
            throw TransportFault(
                TransportFailure.TRANSPORT_UNAVAILABLE, "authority host is not resolvable: ${unknown.message}",
            )
        } catch (io: IOException) {
            throw TransportFault(
                TransportFailure.TRANSPORT_UNAVAILABLE, "authority call failed: ${io.message}",
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun transportFailureFor(status: Int): TransportFailure = when (status) {
        400 -> TransportFailure.MALFORMED
        404 -> TransportFailure.NOT_FOUND
        405 -> TransportFailure.METHOD_NOT_ALLOWED
        413 -> TransportFailure.TOO_LARGE
        415 -> TransportFailure.UNSUPPORTED_MEDIA_TYPE
        503 -> TransportFailure.DURABLE_STATE_UNAVAILABLE
        504 -> TransportFailure.TIMED_OUT
        else -> TransportFailure.TRANSPORT_UNAVAILABLE
    }
}

/** Kept for symmetry with [IdentityAuthorityProtocol]; the protocol id this client speaks. */
public val HttpIdentityAuthorityClient.protocolId: String
    get() = IdentityAuthorityProtocol.PROTOCOL_ID
