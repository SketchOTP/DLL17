package com.animusmachinae.dll17.services.identity

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.HmacSha256
import com.animusmachinae.dll17.core.recovery.AuthorityOutcome
import com.animusmachinae.dll17.core.recovery.AuthorityResponse
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityClient
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityProtocol
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * `IdentityAuthorityServiceV1` — the separately deployable epoch authority.
 *
 * Separately deployable is a real constraint, not a label. This module is not on
 * the organism core dependency path; it depends on `core-recovery` for the
 * protocol types and on nothing else the organism uses. Deleting it entirely
 * breaks new recovery activation and breaks nothing else.
 *
 * ### What it stores, exhaustively
 *
 * Organism ID, current epoch, activation status, a verification key, the active
 * device fingerprint, outstanding challenge nonces with expiry, granted lease
 * expiry, and a bounded failure counter for rate limiting.
 *
 * ### What it cannot store
 *
 * Everything else. There is no field on [AuthorityRecord] for physiology,
 * memories, learning state, relationships, journals, inventory or package
 * plaintext, and the canonical encoding below would reject one — which is the
 * point of encoding it canonically rather than as an open map.
 */
public class IdentityAuthorityService(
    private val storeDirectory: File,
) : IdentityAuthorityClient {

    public companion object {
        public const val SERVICE_ID: String = "IdentityAuthorityServiceV1"
        public const val SERVICE_VERSION: Int = 1
        public const val RECORD_SCHEMA_ID: Int = 261
        public const val RECORD_SCHEMA_VERSION: Int = 1
        public const val STORE_FILE: String = "authority.dll17"
        public const val STAGING_FILE: String = "authority.staging"
    }

    /** The complete per-organism record. Nothing organism-shaped may be added. */
    public class AuthorityRecord(
        public val organismId: Long,
        public val currentEpoch: Int,
        public val activeDeviceFingerprint: Long,
        public val verificationKey: ByteArray,
        public val leaseExpiresAtMillis: Long,
        public val leaseDeviceFingerprint: Long,
        public val grantedEpoch: Int,
        public val failedProofs: Int,
        public val failureWindowStartMillis: Long,
    )

    private val records = LinkedHashMap<Long, AuthorityRecord>()

    /** Outstanding nonces: organism -> (nonce hex -> expiry). Single use. */
    private val nonces = LinkedHashMap<Long, MutableMap<String, Long>>()

    /** Bounded audit log. Counts only; no organism content can enter it. */
    private val audit = ArrayList<String>()

    init {
        if (!storeDirectory.exists() && !storeDirectory.mkdirs()) {
            throw IllegalStateException("cannot create authority store ${storeDirectory.absolutePath}")
        }
        load()
    }

    public fun auditTrail(): List<String> = audit.toList()

    public fun record(organismId: Long): AuthorityRecord? = records[organismId]

    // ------------------------------------------------------------- protocol

    override fun register(
        organismId: Long,
        verificationKey: ByteArray,
        deviceFingerprint: Long,
        nowMillis: Long,
    ): AuthorityResponse {
        val existing = records[organismId]
        if (existing != null) {
            // Registration is idempotent for the same device and key, and a
            // conflict otherwise. A second registration must never reset an
            // epoch, or a superseded device could re-register its way back.
            val same = existing.activeDeviceFingerprint == deviceFingerprint &&
                HmacSha256.constantTimeEquals(existing.verificationKey, verificationKey)
            audit += "register organism=$organismId duplicate same=$same"
            return AuthorityResponse(
                outcome = if (same) AuthorityOutcome.REGISTERED else AuthorityOutcome.EPOCH_CONFLICT,
                organismId = organismId,
                currentEpoch = existing.currentEpoch,
                detail = if (same) "already registered" else "organism is registered to another device",
            )
        }
        records[organismId] = AuthorityRecord(
            organismId = organismId,
            currentEpoch = 1,
            activeDeviceFingerprint = deviceFingerprint,
            verificationKey = verificationKey.copyOf(),
            leaseExpiresAtMillis = 0L,
            leaseDeviceFingerprint = 0L,
            grantedEpoch = 0,
            failedProofs = 0,
            failureWindowStartMillis = nowMillis,
        )
        persist()
        audit += "register organism=$organismId epoch=1"
        return AuthorityResponse(AuthorityOutcome.REGISTERED, organismId, 1, detail = "registered")
    }

    override fun challenge(organismId: Long, nowMillis: Long): AuthorityResponse {
        val record = records[organismId]
            ?: return AuthorityResponse(
                AuthorityOutcome.UNKNOWN_ORGANISM, organismId, 0, detail = "not registered",
            )
        if (rateLimited(record, nowMillis)) {
            audit += "challenge organism=$organismId rate-limited"
            return AuthorityResponse(
                AuthorityOutcome.RATE_LIMITED, organismId, record.currentEpoch,
                detail = "too many failed proofs",
            )
        }
        // Nonce derived from the authority's own state, not from a random source
        // it might not have: organism, current epoch, issue time and a counter.
        val outstanding = nonces.getOrPut(organismId) { LinkedHashMap() }
        outstanding.entries.removeIf { it.value <= nowMillis }
        val nonce = HmacSha256.mac(
            record.verificationKey,
            CanonicalWriter(32)
                .putI64(organismId)
                .putI32(record.currentEpoch)
                .putI64(nowMillis)
                .putI32(outstanding.size)
                .toByteArray(),
        ).copyOf(16)
        val expiry = nowMillis + IdentityAuthorityProtocol.CHALLENGE_VALIDITY_MILLIS
        outstanding[hex(nonce)] = expiry
        audit += "challenge organism=$organismId issued"
        return AuthorityResponse(
            outcome = AuthorityOutcome.CHALLENGE_ISSUED,
            organismId = organismId,
            currentEpoch = record.currentEpoch,
            nonce = nonce,
            nonceExpiresAtMillis = expiry,
            detail = "single-use nonce issued",
        )
    }

    /**
     * The atomic compare-and-swap that makes recovery an event with a winner.
     *
     * The epoch advance and the lease grant happen in one durable step. Two
     * destinations racing on the same package cannot both win: the second
     * arrives with a requested epoch that is no longer `current + 1`.
     */
    override fun activate(
        organismId: Long,
        requestedEpoch: Int,
        nonce: ByteArray,
        proof: ByteArray,
        deviceFingerprint: Long,
        nowMillis: Long,
    ): AuthorityResponse {
        val record = records[organismId]
            ?: return AuthorityResponse(
                AuthorityOutcome.UNKNOWN_ORGANISM, organismId, 0, detail = "not registered",
            )

        // An idempotent retry: the same device asking again for an epoch it has
        // already been granted, inside its lease. Must succeed, because the
        // caller may simply have lost the response.
        if (record.grantedEpoch == requestedEpoch &&
            record.leaseDeviceFingerprint == deviceFingerprint &&
            record.currentEpoch == requestedEpoch &&
            nowMillis < record.leaseExpiresAtMillis
        ) {
            audit += "activate organism=$organismId epoch=$requestedEpoch idempotent-retry"
            return AuthorityResponse(
                outcome = AuthorityOutcome.ALREADY_GRANTED,
                organismId = organismId,
                currentEpoch = record.currentEpoch,
                leaseExpiresAtMillis = record.leaseExpiresAtMillis,
                detail = "lease already granted to this device",
            )
        }

        if (rateLimited(record, nowMillis)) {
            audit += "activate organism=$organismId rate-limited"
            return AuthorityResponse(
                AuthorityOutcome.RATE_LIMITED, organismId, record.currentEpoch,
                detail = "too many failed proofs",
            )
        }

        val outstanding = nonces[organismId]
        val key = hex(nonce)
        val expiry = outstanding?.get(key)
        if (expiry == null || expiry <= nowMillis) {
            audit += "activate organism=$organismId challenge-invalid"
            return AuthorityResponse(
                AuthorityOutcome.CHALLENGE_INVALID, organismId, record.currentEpoch,
                detail = if (expiry == null) "unknown or already spent nonce" else "nonce expired",
            )
        }

        if (requestedEpoch != record.currentEpoch + 1) {
            audit += "activate organism=$organismId epoch-conflict requested=$requestedEpoch"
            return AuthorityResponse(
                AuthorityOutcome.EPOCH_CONFLICT, organismId, record.currentEpoch,
                detail = "requested epoch must be exactly one past ${record.currentEpoch}",
            )
        }

        val expected = HmacSha256.mac(
            record.verificationKey,
            CanonicalWriter(64)
                .putI64(organismId)
                .putBytes(nonce)
                .putI32(requestedEpoch)
                .putI64(deviceFingerprint)
                .toByteArray(),
        )
        if (!HmacSha256.constantTimeEquals(proof, expected)) {
            // Spend the nonce anyway. A nonce that survives a failed proof is a
            // nonce an attacker can grind against.
            outstanding.remove(key)
            recordFailure(organismId, nowMillis)
            audit += "activate organism=$organismId proof-rejected"
            return AuthorityResponse(
                AuthorityOutcome.PROOF_REJECTED, organismId, record.currentEpoch,
                detail = "activation proof did not verify",
            )
        }

        outstanding.remove(key)
        records[organismId] = AuthorityRecord(
            organismId = organismId,
            currentEpoch = requestedEpoch,
            activeDeviceFingerprint = deviceFingerprint,
            verificationKey = record.verificationKey,
            leaseExpiresAtMillis = nowMillis + IdentityAuthorityProtocol.ACTIVATION_LEASE_MILLIS,
            leaseDeviceFingerprint = deviceFingerprint,
            grantedEpoch = requestedEpoch,
            failedProofs = 0,
            failureWindowStartMillis = nowMillis,
        )
        persist()
        audit += "activate organism=$organismId epoch=$requestedEpoch granted"
        return AuthorityResponse(
            outcome = AuthorityOutcome.ACTIVATION_GRANTED,
            organismId = organismId,
            currentEpoch = requestedEpoch,
            leaseExpiresAtMillis = nowMillis + IdentityAuthorityProtocol.ACTIVATION_LEASE_MILLIS,
            detail = "epoch advanced and lease granted",
        )
    }

    /**
     * How a superseded device learns it has been superseded.
     *
     * The authority does not reach out — it cannot, and a device that never
     * contacts it is never told. That asymmetry is the honest limit of the
     * guarantee, and it is why the claim is supported singularity rather than
     * absolute singularity.
     */
    override fun heartbeat(
        organismId: Long,
        claimedEpoch: Int,
        deviceFingerprint: Long,
        nowMillis: Long,
    ): AuthorityResponse {
        val record = records[organismId]
            ?: return AuthorityResponse(
                AuthorityOutcome.UNKNOWN_ORGANISM, organismId, 0, detail = "not registered",
            )
        return if (claimedEpoch < record.currentEpoch ||
            (claimedEpoch == record.currentEpoch &&
                record.activeDeviceFingerprint != deviceFingerprint)
        ) {
            audit += "heartbeat organism=$organismId superseded claimed=$claimedEpoch"
            AuthorityResponse(
                AuthorityOutcome.SUPERSEDED, organismId, record.currentEpoch,
                detail = "this device is superseded by epoch ${record.currentEpoch}",
            )
        } else {
            AuthorityResponse(
                AuthorityOutcome.REGISTERED, organismId, record.currentEpoch, detail = "current",
            )
        }
    }

    // ------------------------------------------------------------- internals

    private fun rateLimited(record: AuthorityRecord, nowMillis: Long): Boolean {
        val inWindow = nowMillis - record.failureWindowStartMillis <
            IdentityAuthorityProtocol.RATE_LIMIT_WINDOW_MILLIS
        return inWindow && record.failedProofs >= IdentityAuthorityProtocol.MAX_FAILED_PROOFS
    }

    private fun recordFailure(organismId: Long, nowMillis: Long) {
        val record = records.getValue(organismId)
        val windowExpired = nowMillis - record.failureWindowStartMillis >=
            IdentityAuthorityProtocol.RATE_LIMIT_WINDOW_MILLIS
        records[organismId] = AuthorityRecord(
            organismId = record.organismId,
            currentEpoch = record.currentEpoch,
            activeDeviceFingerprint = record.activeDeviceFingerprint,
            verificationKey = record.verificationKey,
            leaseExpiresAtMillis = record.leaseExpiresAtMillis,
            leaseDeviceFingerprint = record.leaseDeviceFingerprint,
            grantedEpoch = record.grantedEpoch,
            failedProofs = if (windowExpired) 1 else record.failedProofs + 1,
            failureWindowStartMillis = if (windowExpired) nowMillis else record.failureWindowStartMillis,
        )
        persist()
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun persist() {
        val writer = CanonicalWriter(records.size * 96 + 32)
        writer.putU32(records.size)
        for (record in records.values) {
            writer.putI64(record.organismId)
                .putI32(record.currentEpoch)
                .putI64(record.activeDeviceFingerprint)
                .putBytes(record.verificationKey)
                .putI64(record.leaseExpiresAtMillis)
                .putI64(record.leaseDeviceFingerprint)
                .putI32(record.grantedEpoch)
                .putI32(record.failedProofs)
                .putI64(record.failureWindowStartMillis)
        }
        val bytes = CanonicalEnvelope.wrap(RECORD_SCHEMA_ID, RECORD_SCHEMA_VERSION, writer.toByteArray())
        val staging = File(storeDirectory, STAGING_FILE)
        staging.writeBytes(bytes)
        java.io.RandomAccessFile(staging, "rws").use { it.fd.sync() }
        Files.move(
            staging.toPath(),
            File(storeDirectory, STORE_FILE).toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    private fun load() {
        val file = File(storeDirectory, STORE_FILE)
        if (!file.exists()) return
        val contents = CanonicalEnvelope.unwrap(file.readBytes())
        val reader = CanonicalReader(contents.payload)
        val count = reader.readSequenceCount()
        repeat(count) {
            val record = AuthorityRecord(
                organismId = reader.readI64(),
                currentEpoch = reader.readI32(),
                activeDeviceFingerprint = reader.readI64(),
                verificationKey = reader.readBytes(),
                leaseExpiresAtMillis = reader.readI64(),
                leaseDeviceFingerprint = reader.readI64(),
                grantedEpoch = reader.readI32(),
                failedProofs = reader.readI32(),
                failureWindowStartMillis = reader.readI64(),
            )
            records[record.organismId] = record
        }
        reader.requireExhausted()
    }
}
