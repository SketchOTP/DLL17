package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter

/**
 * Version boundaries for continuity state, contract section 15.3.
 *
 * Production never replays an old journal through a newer behavior engine. The
 * old-version state is reconstructed with its **original decoder**, hash- and
 * coverage-verified, checkpointed, and only then migrated by a pure function
 * that runs no behavior logic and draws no randomness.
 *
 * The version-zero format below never shipped. It exists so that the migration
 * *mechanism* is qualified before a real migration is ever needed — the one
 * point at which a migration bug is cheap.
 */
public object ContinuityMigration {

    public const val LEGACY_SCHEMA_VERSION: Int = 0

    /**
     * Encodes a synthetic version-zero artifact.
     *
     * Version zero carries only the fields a first release would have had:
     * identity, the four clocks and the two reserves. Everything the current
     * version adds is supplied by the migration with an explicit default, which
     * is what makes the migration total rather than best-effort.
     */
    public fun encodeLegacyV0(
        organismId: Long,
        deviceFingerprint: Long,
        wallClockAgeMillis: Long,
        reserveA: Long,
        reserveB: Long,
    ): ByteArray = CanonicalEnvelope.wrap(
        ContinuityState.SCHEMA_ID,
        LEGACY_SCHEMA_VERSION,
        CanonicalWriter(64)
            .putI64(organismId)
            .putI64(deviceFingerprint)
            .putI64(wallClockAgeMillis)
            .putI64(reserveA)
            .putI64(reserveB)
            .toByteArray(),
    )

    /**
     * Migrates any supported artifact to the current version.
     *
     * Unknown and future versions are refused outright, and downgrade is
     * prohibited. Guessing across a version boundary is how a decoder turns a
     * field it does not understand into a field it silently misreads.
     */
    public fun migrateToCurrent(bytes: ByteArray): ContinuityState {
        val contents = CanonicalEnvelope.unwrap(bytes)
        if (contents.payloadSchemaId != ContinuityState.SCHEMA_ID) {
            throw IllegalArgumentException(
                "expected continuity schema ${ContinuityState.SCHEMA_ID}, " +
                    "got ${contents.payloadSchemaId}",
            )
        }
        return when (contents.payloadSchemaVersion) {
            ContinuityState.SCHEMA_VERSION -> ContinuityState.decode(bytes)
            LEGACY_SCHEMA_VERSION -> migrateV0ToV1(contents.payload)
            else -> throw IllegalArgumentException(
                "continuity schema version ${contents.payloadSchemaVersion} is unknown to this " +
                    "build; a future version is refused rather than guessed, and downgrade is " +
                    "prohibited",
            )
        }
    }

    /**
     * The v0 to v1 migration.
     *
     * Pure: it reads the old fields, supplies contract defaults for everything
     * added since, and executes no reducer, no behavior selection and no random
     * draw. Running it twice on the same input produces the same bytes.
     */
    private fun migrateV0ToV1(payload: ByteArray): ContinuityState {
        val reader = CanonicalReader(payload)
        val organismId = reader.readI64()
        val deviceFingerprint = reader.readI64()
        val wallClockAgeMillis = reader.readI64()
        val reserveA = reader.readI64()
        val reserveB = reader.readI64()
        reader.requireExhausted()

        return ContinuityState.genesis(organismId, deviceFingerprint, reserveA, reserveB).copy(
            wallClockAgeMillis = wallClockAgeMillis,
        )
    }
}
