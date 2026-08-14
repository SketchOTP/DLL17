package com.animusmachinae.dll17.core.recovery

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.HmacSha256

/**
 * `IdentityAuthorityProtocolV1`.
 *
 * The wire protocol only. The service that implements it is deployed separately
 * (`services/identity-authority`) and is deliberately not on the organism core
 * dependency graph: **normal local operation never calls it.** If it is
 * permanently unavailable, new recovery activation is unavailable and an already
 * activated organism is completely unaffected.
 *
 * What the authority is for: making a recovery activation an *event* with a
 * winner, so that after a cold recovery the old device cannot keep acting as the
 * same organism. What it is not for: knowing anything about the organism. It
 * stores an ID, an epoch, activation status, key metadata, challenge nonces and
 * bounded audit data. It never sees physiology, memories, learning, relationships,
 * journals, inventory or recovery-package plaintext, and it could not decrypt
 * them if it did.
 *
 * ### The honest guarantee
 *
 * This protocol delivers **supported singularity**: a recovered organism
 * supersedes its predecessor, and the predecessor is rejected the next time it
 * contacts the authority. It does not deliver absolute singularity. A device
 * that is cloned and then kept permanently offline cannot be invalidated by any
 * online authority, because there is nothing to invalidate it through. The
 * product must claim the first and never the second.
 */
public object IdentityAuthorityProtocol {

    public const val PROTOCOL_ID: String = "IdentityAuthorityProtocolV1"
    public const val PROTOCOL_VERSION: Int = 1

    public const val REGISTER_SCHEMA_ID: Int = 251
    public const val CHALLENGE_SCHEMA_ID: Int = 252
    public const val ACTIVATE_SCHEMA_ID: Int = 253
    public const val HEARTBEAT_SCHEMA_ID: Int = 254
    public const val SCHEMA_VERSION: Int = 1

    /** A challenge nonce is single-use and expires. Both are replay protection. */
    public const val CHALLENGE_VALIDITY_MILLIS: Long = 5L * 60L * 1000L

    /** An activation lease bounds how long a destination may take to commit. */
    public const val ACTIVATION_LEASE_MILLIS: Long = 15L * 60L * 1000L

    /** Failed proofs tolerated per organism inside the window before refusal. */
    public const val MAX_FAILED_PROOFS: Int = 5
    public const val RATE_LIMIT_WINDOW_MILLIS: Long = 10L * 60L * 1000L

    /**
     * The proof a destination device presents.
     *
     * A MAC over the authority's nonce and the requested epoch, keyed by a
     * secret derived from the recovery root. It proves possession of the root
     * without revealing it, is bound to one nonce so it cannot be replayed, and
     * is bound to the epoch so a captured proof cannot be reused for a later
     * activation.
     */
    public fun activationProof(
        root: RecoveryRoot,
        organismId: Long,
        nonce: ByteArray,
        requestedEpoch: Int,
        deviceFingerprint: Long,
    ): ByteArray = HmacSha256.mac(
        root.authorityProofKey(organismId),
        CanonicalWriter(64)
            .putI64(organismId)
            .putBytes(nonce)
            .putI32(requestedEpoch)
            .putI64(deviceFingerprint)
            .toByteArray(),
    )
}

/** Authority outcomes. Every one of them is a refusal reason or a grant. */
public enum class AuthorityOutcome(public val ordinal32: Int) {
    REGISTERED(1),
    CHALLENGE_ISSUED(2),
    ACTIVATION_GRANTED(3),

    /** The requested epoch is not exactly one past the current epoch. */
    EPOCH_CONFLICT(4),

    /** The proof did not verify under the stored verification key. */
    PROOF_REJECTED(5),

    /** The nonce is unknown, already spent, or expired. */
    CHALLENGE_INVALID(6),

    /** Too many failed proofs for this organism inside the window. */
    RATE_LIMITED(7),

    /** The caller is acting for an epoch the authority has already superseded. */
    SUPERSEDED(8),

    /** The organism is not registered. */
    UNKNOWN_ORGANISM(9),

    /** The same activation was already granted; the prior lease is returned. */
    ALREADY_GRANTED(10),
}

/** The authority's reply. Carries no organism content by construction. */
public class AuthorityResponse(
    public val outcome: AuthorityOutcome,
    public val organismId: Long,
    public val currentEpoch: Int,
    public val nonce: ByteArray = ByteArray(0),
    public val nonceExpiresAtMillis: Long = 0L,
    public val leaseExpiresAtMillis: Long = 0L,
    public val detail: String = "",
) {
    public val granted: Boolean
        get() = outcome == AuthorityOutcome.ACTIVATION_GRANTED ||
            outcome == AuthorityOutcome.ALREADY_GRANTED

    public fun canonicalBytes(): ByteArray = CanonicalEnvelope.wrap(
        IdentityAuthorityProtocol.ACTIVATE_SCHEMA_ID,
        IdentityAuthorityProtocol.SCHEMA_VERSION,
        CanonicalWriter(96)
            .putI32(outcome.ordinal32)
            .putI64(organismId)
            .putI32(currentEpoch)
            .putBytes(nonce)
            .putI64(nonceExpiresAtMillis)
            .putI64(leaseExpiresAtMillis)
            .toByteArray(),
    )

    public companion object {
        public fun decode(bytes: ByteArray): AuthorityResponse {
            val contents = CanonicalEnvelope.unwrap(bytes)
            val reader = CanonicalReader(contents.payload)
            val ordinal = reader.readI32()
            val response = AuthorityResponse(
                outcome = AuthorityOutcome.entries.first { it.ordinal32 == ordinal },
                organismId = reader.readI64(),
                currentEpoch = reader.readI32(),
                nonce = reader.readBytes(),
                nonceExpiresAtMillis = reader.readI64(),
                leaseExpiresAtMillis = reader.readI64(),
            )
            reader.requireExhausted()
            return response
        }
    }
}

/**
 * The authority as seen by a device. Deliberately an interface: the transport is
 * a product decision, and correctness must be provable without one.
 */
public interface IdentityAuthorityClient {
    /** Birth-time registration of the initial epoch and verification key. */
    public fun register(
        organismId: Long,
        verificationKey: ByteArray,
        deviceFingerprint: Long,
        nowMillis: Long,
    ): AuthorityResponse

    /** Requests a single-use nonce for an activation attempt. */
    public fun challenge(organismId: Long, nowMillis: Long): AuthorityResponse

    /** Atomically advances the epoch and grants a lease, if the proof verifies. */
    public fun activate(
        organismId: Long,
        requestedEpoch: Int,
        nonce: ByteArray,
        proof: ByteArray,
        deviceFingerprint: Long,
        nowMillis: Long,
    ): AuthorityResponse

    /**
     * A device reporting the epoch it believes it holds.
     *
     * This is how a superseded device finds out. It is not required for local
     * operation and its failure is not an error.
     */
    public fun heartbeat(
        organismId: Long,
        claimedEpoch: Int,
        deviceFingerprint: Long,
        nowMillis: Long,
    ): AuthorityResponse
}
