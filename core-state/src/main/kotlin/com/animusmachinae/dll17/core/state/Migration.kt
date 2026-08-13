package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.RandomDomainRegistry
import com.animusmachinae.dll17.core.crypto.RandomSubstream

/**
 * Deterministic schema migration, per `DeterminismContractV1` section 12.5.
 *
 * Three rules make a migration safe to run on a device that has been offline for
 * a month:
 *
 * 1. **Migrations are pure.** No randomness is drawn, no clock is read, and no
 *    old behaviour logic is re-executed. A migration reconstructs durable state;
 *    it does not re-simulate history it never witnessed.
 * 2. **New random domains are derived, not drawn.** A domain introduced by a
 *    migration gets its seed from `SUBSTREAM_DERIVE_V1` applied to the existing
 *    master seed. No existing substream is advanced, so a migrated organism
 *    produces exactly the draws it would have produced.
 * 3. **Unknown versions are refused.** A future artifact is never
 *    best-effort parsed. Downgrade is prohibited outright.
 *
 * The version-0 snapshot below is a **mechanism proof**. No organism has ever
 * been persisted under it. It exists because a migration framework with no
 * migration in it proves nothing, and inventing a fake organism schema would
 * have been worse.
 */
public object SnapshotMigration {

    /** The oldest schema this build can read at all. */
    public const val OLDEST_SUPPORTED_SCHEMA_VERSION: Int = 0

    /** The schema this build writes. */
    public const val CURRENT_SCHEMA_VERSION: Int = CanonicalSnapshot.SCHEMA_VERSION

    /**
     * Migrates any supported snapshot artifact to the current schema.
     *
     * @throws IllegalArgumentException for a future version, an unsupported past
     *   version, or a downgrade attempt.
     */
    public fun migrateToCurrent(bytes: ByteArray): CanonicalSnapshot {
        val contents = CanonicalEnvelope.unwrap(bytes)
        if (contents.payloadSchemaId != CanonicalSnapshot.SCHEMA_ID) {
            throw IllegalArgumentException(
                "artifact schema ${contents.payloadSchemaId} is not a canonical snapshot",
            )
        }
        return when (val version = contents.payloadSchemaVersion) {
            CURRENT_SCHEMA_VERSION -> CanonicalSnapshot.decode(bytes)
            0 -> migrateV0ToV1(contents.payload)
            in Int.MIN_VALUE until OLDEST_SUPPORTED_SCHEMA_VERSION -> throw IllegalArgumentException(
                "snapshot schema version $version predates the oldest supported version " +
                    "$OLDEST_SUPPORTED_SCHEMA_VERSION and cannot be migrated",
            )
            else -> throw IllegalArgumentException(
                "snapshot schema version $version is newer than this build's " +
                    "$CURRENT_SCHEMA_VERSION; refusing to guess at a future format",
            )
        }
    }

    /**
     * Version 0 held only a logical clock, a master seed and one numeric value.
     *
     * The migration fills the fields that version 1 added and derives the
     * substreams that version 1 requires. Every filled value is a documented
     * constant rather than a computed guess, because a migration that invents
     * plausible history is indistinguishable from corruption.
     */
    private fun migrateV0ToV1(payload: ByteArray): CanonicalSnapshot {
        val reader = CanonicalReader(payload)
        val logicalTime = reader.readI64()
        val masterSeed = reader.readI64()
        val numericA = reader.readI64()
        reader.requireExhausted()

        return CanonicalSnapshot(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            logicalTime = logicalTime,
            masterSeed = masterSeed,
            randomContractVersion = CanonicalEnvelope.CONTRACT_VERSION,
            numericA = numericA,
            // Version 0 had no such field. Zero is the documented initial value,
            // not an estimate of what it might have been.
            numericB = 0L,
            materialUnits = 0L,
            lastCommitSequence = 0L,
            // Derived, never drawn. No existing stream is advanced by this.
            substreams = listOf(
                RandomSubstream.derive(masterSeed, RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY),
                RandomSubstream.derive(masterSeed, RandomDomainRegistry.DOMAIN_QUALIFICATION_SECONDARY),
            ),
        )
    }

    /**
     * Writes a version-0 artifact. Test and qualification support only: this
     * build never persists version 0.
     */
    public fun encodeLegacyV0(logicalTime: Long, masterSeed: Long, numericA: Long): ByteArray {
        val payload = CanonicalWriter(24)
            .putI64(logicalTime)
            .putI64(masterSeed)
            .putI64(numericA)
            .toByteArray()
        return CanonicalEnvelope.wrap(CanonicalSnapshot.SCHEMA_ID, 0, payload)
    }

    /** Writes an artifact claiming a future schema version, to prove refusal. */
    public fun encodeFutureVersion(version: Int): ByteArray =
        CanonicalEnvelope.wrap(CanonicalSnapshot.SCHEMA_ID, version, ByteArray(0))
}
