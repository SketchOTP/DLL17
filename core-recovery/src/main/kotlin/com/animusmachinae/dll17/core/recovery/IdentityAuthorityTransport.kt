package com.animusmachinae.dll17.core.recovery

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter

/**
 * `IdentityAuthorityTransportContractV1` — the wire encoding and endpoint map
 * for `IdentityAuthorityProtocolV1`.
 *
 * ### The transport is not the protocol
 *
 * `IdentityAuthorityProtocolV1` stays authoritative and is not amended by this
 * file. Epochs, challenges, leases, replay protection, rate limiting, idempotency
 * and activation outcomes are decided by the protocol implementation and merely
 * *carried* here. A transport that could change any of them would be a second
 * definition of the same rule, and the two would eventually disagree.
 *
 * The consequence worth stating plainly: this codec has no notion of who wins an
 * activation. It moves four request shapes and one response shape.
 *
 * ### Why canonical bytes rather than JSON
 *
 * The project already owns a canonical codec with a frozen byte layout, and
 * `AuthorityResponse` already encodes to it. Reusing it means the wire format
 * needs no new dependency, no parser to harden, and no ambiguity about how a
 * field is rendered. It also fixes the message size, which is what makes the
 * body ceiling below a real defence rather than a guess.
 *
 * ### The clock is the server's
 *
 * [IdentityAuthorityClient] takes `nowMillis` because an in-process caller has to
 * supply one. **No request encoding here carries it.** A client that could name
 * the current time over the network could expire nothing, outlive every lease and
 * step around the rate limiter, so the server reads its own clock and the field
 * simply never crosses the wire.
 */
public object IdentityAuthorityTransport {

    public const val CONTRACT_ID: String = "IdentityAuthorityTransportContractV1"
    public const val CONTRACT_VERSION: Int = 1

    /** The protocol this transport carries, unchanged. */
    public const val CARRIES: String = "IdentityAuthorityProtocolV1"

    public const val MEDIA_TYPE: String = "application/vnd.dll17.authority.v1"

    /** Client-supplied correlation identifier. 32 lowercase hex characters. */
    public const val REQUEST_ID_HEADER: String = "x-dll17-request-id"
    public const val REQUEST_ID_LENGTH: Int = 32

    public const val PATH_REGISTER: String = "/v1/register"
    public const val PATH_CHALLENGE: String = "/v1/challenge"
    public const val PATH_ACTIVATE: String = "/v1/activate"
    public const val PATH_HEARTBEAT: String = "/v1/heartbeat"
    public const val PATH_HEALTH: String = "/healthz"
    public const val PATH_READY: String = "/readyz"

    /**
     * The largest body the service will read.
     *
     * The biggest legal request is an `activate` carrying a 16-byte nonce and a
     * 32-byte proof, which is under 128 bytes. One kibibyte is already an order
     * of magnitude of headroom, and a ceiling this tight means an attacker cannot
     * make the service allocate on their behalf.
     */
    public const val MAX_REQUEST_BYTES: Int = 1024

    public fun isWellFormedRequestId(value: String?): Boolean =
        value != null && value.length == REQUEST_ID_LENGTH &&
            value.all { it in '0'..'9' || it in 'a'..'f' }

    // ------------------------------------------------------------- requests

    public fun encodeRegister(
        organismId: Long,
        verificationKey: ByteArray,
        deviceFingerprint: Long,
    ): ByteArray = CanonicalEnvelope.wrap(
        IdentityAuthorityProtocol.REGISTER_SCHEMA_ID,
        IdentityAuthorityProtocol.SCHEMA_VERSION,
        CanonicalWriter(64)
            .putI64(organismId)
            .putBytes(verificationKey)
            .putI64(deviceFingerprint)
            .toByteArray(),
    )

    public fun encodeChallenge(organismId: Long): ByteArray = CanonicalEnvelope.wrap(
        IdentityAuthorityProtocol.CHALLENGE_SCHEMA_ID,
        IdentityAuthorityProtocol.SCHEMA_VERSION,
        CanonicalWriter(16).putI64(organismId).toByteArray(),
    )

    public fun encodeActivate(
        organismId: Long,
        requestedEpoch: Int,
        nonce: ByteArray,
        proof: ByteArray,
        deviceFingerprint: Long,
    ): ByteArray = CanonicalEnvelope.wrap(
        IdentityAuthorityProtocol.ACTIVATE_SCHEMA_ID,
        IdentityAuthorityProtocol.SCHEMA_VERSION,
        CanonicalWriter(96)
            .putI64(organismId)
            .putI32(requestedEpoch)
            .putBytes(nonce)
            .putBytes(proof)
            .putI64(deviceFingerprint)
            .toByteArray(),
    )

    public fun encodeHeartbeat(
        organismId: Long,
        claimedEpoch: Int,
        deviceFingerprint: Long,
    ): ByteArray = CanonicalEnvelope.wrap(
        IdentityAuthorityProtocol.HEARTBEAT_SCHEMA_ID,
        IdentityAuthorityProtocol.SCHEMA_VERSION,
        CanonicalWriter(32)
            .putI64(organismId)
            .putI32(claimedEpoch)
            .putI64(deviceFingerprint)
            .toByteArray(),
    )

    /** A decoded request. One class rather than four, because the server dispatches on path. */
    public class Request(
        public val schemaId: Int,
        public val organismId: Long,
        public val requestedEpoch: Int = 0,
        public val claimedEpoch: Int = 0,
        public val verificationKey: ByteArray = ByteArray(0),
        public val nonce: ByteArray = ByteArray(0),
        public val proof: ByteArray = ByteArray(0),
        public val deviceFingerprint: Long = 0L,
    )

    /**
     * Decodes a request, or throws [TransportFault].
     *
     * [expectedSchemaId] is passed by the server from the *path*, so a body
     * claiming to be an activation cannot be delivered to the heartbeat handler
     * by naming a different schema.
     */
    public fun decodeRequest(expectedSchemaId: Int, bytes: ByteArray): Request {
        if (bytes.size > MAX_REQUEST_BYTES) {
            throw TransportFault(TransportFailure.TOO_LARGE, "request exceeds $MAX_REQUEST_BYTES bytes")
        }
        val contents = try {
            CanonicalEnvelope.unwrap(bytes)
        } catch (malformed: RuntimeException) {
            throw TransportFault(TransportFailure.MALFORMED, malformed.message ?: "unreadable request")
        }
        if (contents.payloadSchemaId != expectedSchemaId) {
            throw TransportFault(
                TransportFailure.MALFORMED,
                "body schema ${contents.payloadSchemaId} does not match the endpoint",
            )
        }
        if (contents.payloadSchemaVersion != IdentityAuthorityProtocol.SCHEMA_VERSION) {
            throw TransportFault(
                TransportFailure.UNSUPPORTED_VERSION,
                "schema version ${contents.payloadSchemaVersion} is not implemented by this build",
            )
        }
        val reader = CanonicalReader(contents.payload)
        return try {
            val request = when (expectedSchemaId) {
                IdentityAuthorityProtocol.REGISTER_SCHEMA_ID -> Request(
                    schemaId = expectedSchemaId,
                    organismId = reader.readI64(),
                    verificationKey = reader.readBytes(),
                    deviceFingerprint = reader.readI64(),
                )
                IdentityAuthorityProtocol.CHALLENGE_SCHEMA_ID -> Request(
                    schemaId = expectedSchemaId,
                    organismId = reader.readI64(),
                )
                IdentityAuthorityProtocol.ACTIVATE_SCHEMA_ID -> Request(
                    schemaId = expectedSchemaId,
                    organismId = reader.readI64(),
                    requestedEpoch = reader.readI32(),
                    nonce = reader.readBytes(),
                    proof = reader.readBytes(),
                    deviceFingerprint = reader.readI64(),
                )
                IdentityAuthorityProtocol.HEARTBEAT_SCHEMA_ID -> Request(
                    schemaId = expectedSchemaId,
                    organismId = reader.readI64(),
                    claimedEpoch = reader.readI32(),
                    deviceFingerprint = reader.readI64(),
                )
                else -> throw TransportFault(TransportFailure.MALFORMED, "unknown endpoint schema")
            }
            // Trailing bytes are a refusal, not something to ignore. A decoder
            // that tolerates them accepts two different messages as the same one.
            reader.requireExhausted()
            request
        } catch (fault: TransportFault) {
            throw fault
        } catch (malformed: RuntimeException) {
            throw TransportFault(TransportFailure.MALFORMED, malformed.message ?: "unreadable request")
        }
    }

    // ------------------------------------------------------------- responses

    /**
     * The advisory HTTP status for a protocol outcome.
     *
     * Advisory is the operative word: **the body is authoritative**. The status
     * exists so an operator's dashboard, proxy and log aggregator can tell a
     * granted activation from a refused one without decoding a canonical
     * envelope. A client that trusted the status over the body would be trusting
     * whatever the last proxy in the chain decided to rewrite.
     */
    public fun httpStatusFor(outcome: AuthorityOutcome): Int = when (outcome) {
        AuthorityOutcome.REGISTERED -> 200
        AuthorityOutcome.CHALLENGE_ISSUED -> 200
        AuthorityOutcome.ACTIVATION_GRANTED -> 200
        AuthorityOutcome.ALREADY_GRANTED -> 200
        AuthorityOutcome.EPOCH_CONFLICT -> 409
        AuthorityOutcome.SUPERSEDED -> 409
        AuthorityOutcome.PROOF_REJECTED -> 403
        AuthorityOutcome.CHALLENGE_INVALID -> 400
        AuthorityOutcome.RATE_LIMITED -> 429
        AuthorityOutcome.UNKNOWN_ORGANISM -> 404
    }

    /**
     * The response body.
     *
     * `AuthorityResponse.detail` is **not** carried. It is free text written by
     * the service for its own diagnostics, and free text is the one field shape
     * that could smuggle something across a boundary this contract exists to keep
     * narrow. A client that needs to know what happened has the outcome enum.
     */
    public fun encodeResponse(response: AuthorityResponse): ByteArray = response.canonicalBytes()

    public fun decodeResponse(bytes: ByteArray): AuthorityResponse = AuthorityResponse.decode(bytes)
}

/** Transport-level failures. Disjoint from [AuthorityOutcome] on purpose. */
public enum class TransportFailure(public val ordinal32: Int, public val httpStatus: Int) {
    MALFORMED(1, 400),
    TOO_LARGE(2, 413),
    UNSUPPORTED_MEDIA_TYPE(3, 415),
    UNSUPPORTED_VERSION(4, 400),
    METHOD_NOT_ALLOWED(5, 405),
    NOT_FOUND(6, 404),
    MISSING_REQUEST_ID(7, 400),
    DURABLE_STATE_UNAVAILABLE(8, 503),
    TRANSPORT_UNAVAILABLE(9, 503),
    TIMED_OUT(10, 504),
}

/**
 * A transport failure.
 *
 * Deliberately not an [AuthorityOutcome]. "The service could not be reached" and
 * "the service refused you" lead to different user outcomes and different
 * operator actions, and a type that could express both as one value would
 * eventually be used to conflate them.
 */
public class TransportFault(
    public val failure: TransportFailure,
    message: String,
) : RuntimeException(message)
