package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * The canonical R002 continuity state.
 *
 * **This is not an organism.** `reserveA` and `reserveB` are neutral bounded
 * quantities that exist so a reconciliation has something observable to act on.
 * Naming either of them `energy` would be an R003 decision gated behind A001,
 * and R002 does not make it. Every threshold expressed against them is a
 * fraction of capacity, never a species value.
 *
 * Every field is either an `i64` millisecond duration or a `Fixed64` fraction.
 * The two never mix in one expression, because a millisecond count that drifted
 * into the saturating domain would clamp silently where it should fail loudly.
 */

/**
 * A durable time anchor, contract section 3.1. Canonical schema `210`.
 *
 * Optional values carry an explicit presence flag rather than a sentinel. A
 * sentinel would make "no boot identity" and "boot identity zero" the same
 * bytes, and the whole point of this record is to distinguish evidence that is
 * absent from evidence that is zero.
 */
public class DurableTimeAnchor(
    public val anchorSequence: Long,
    public val wallClockUtcMillis: Long,
    public val elapsedRealtimeMillis: Long,
    public val bootIdentityPresent: Boolean,
    public val bootIdentity: Long,
    public val logicalTime: Long,
    public val timeConfidence: TimeConfidence,
    public val authenticatedTimePresent: Boolean,
    public val authenticatedTimeMillis: Long,
) {
    public fun writeTo(writer: CanonicalWriter): CanonicalWriter = writer
        .putI64(anchorSequence)
        .putI64(wallClockUtcMillis)
        .putI64(elapsedRealtimeMillis)
        .putBool(bootIdentityPresent)
        .putI64(if (bootIdentityPresent) bootIdentity else 0L)
        .putI64(logicalTime)
        .putEnum(timeConfidence.ordinal32)
        .putBool(authenticatedTimePresent)
        .putI64(if (authenticatedTimePresent) authenticatedTimeMillis else 0L)

    public fun canonicalBytes(): ByteArray =
        CanonicalEnvelope.wrap(SCHEMA_ID, SCHEMA_VERSION, writeTo(CanonicalWriter(80)).toByteArray())

    public fun copy(
        anchorSequence: Long = this.anchorSequence,
        wallClockUtcMillis: Long = this.wallClockUtcMillis,
        elapsedRealtimeMillis: Long = this.elapsedRealtimeMillis,
        bootIdentityPresent: Boolean = this.bootIdentityPresent,
        bootIdentity: Long = this.bootIdentity,
        logicalTime: Long = this.logicalTime,
        timeConfidence: TimeConfidence = this.timeConfidence,
        authenticatedTimePresent: Boolean = this.authenticatedTimePresent,
        authenticatedTimeMillis: Long = this.authenticatedTimeMillis,
    ): DurableTimeAnchor = DurableTimeAnchor(
        anchorSequence, wallClockUtcMillis, elapsedRealtimeMillis, bootIdentityPresent,
        bootIdentity, logicalTime, timeConfidence, authenticatedTimePresent, authenticatedTimeMillis,
    )

    override fun toString(): String =
        "DurableTimeAnchor(#$anchorSequence elapsed=$elapsedRealtimeMillis " +
            "boot=${if (bootIdentityPresent) bootIdentity.toString() else "absent"} $timeConfidence)"

    public companion object {
        public const val SCHEMA_ID: Int = 210
        public const val SCHEMA_VERSION: Int = 1

        public fun readFrom(reader: CanonicalReader): DurableTimeAnchor = DurableTimeAnchor(
            anchorSequence = reader.readI64(),
            wallClockUtcMillis = reader.readI64(),
            elapsedRealtimeMillis = reader.readI64(),
            bootIdentityPresent = reader.readBool(),
            bootIdentity = reader.readI64(),
            logicalTime = reader.readI64(),
            timeConfidence = TimeConfidence.fromOrdinal(reader.readEnum()),
            authenticatedTimePresent = reader.readBool(),
            authenticatedTimeMillis = reader.readI64(),
        )

        /** The genesis anchor: no prior boot, no prior uptime, nothing verified. */
        public fun genesis(): DurableTimeAnchor = DurableTimeAnchor(
            anchorSequence = 0L,
            wallClockUtcMillis = 0L,
            elapsedRealtimeMillis = 0L,
            bootIdentityPresent = false,
            bootIdentity = 0L,
            logicalTime = 0L,
            timeConfidence = TimeConfidence.VERIFIED_MONOTONIC,
            authenticatedTimePresent = false,
            authenticatedTimeMillis = 0L,
        )
    }
}

/** Blind-decay credit accounting, contract section 5. */
public class BlindCreditLedger(
    public val availableMillis: Long,
    /** Carried division remainder, so fragmenting sessions cannot farm drift. */
    public val carriedRemainder: Long,
    public val windowStartVerifiedMillis: Long,
    public val grantedInWindowMillis: Long,
    /** Boot identity whose consumption is already recorded, if any. */
    public val consumedForBootPresent: Boolean,
    public val consumedForBoot: Long,
    public val bootWindowStartVerifiedMillis: Long,
    public val bootsInWindow: Int,
) {
    public fun writeTo(writer: CanonicalWriter): CanonicalWriter = writer
        .putI64(availableMillis)
        .putI64(carriedRemainder)
        .putI64(windowStartVerifiedMillis)
        .putI64(grantedInWindowMillis)
        .putBool(consumedForBootPresent)
        .putI64(if (consumedForBootPresent) consumedForBoot else 0L)
        .putI64(bootWindowStartVerifiedMillis)
        .putI32(bootsInWindow)

    public fun copy(
        availableMillis: Long = this.availableMillis,
        carriedRemainder: Long = this.carriedRemainder,
        windowStartVerifiedMillis: Long = this.windowStartVerifiedMillis,
        grantedInWindowMillis: Long = this.grantedInWindowMillis,
        consumedForBootPresent: Boolean = this.consumedForBootPresent,
        consumedForBoot: Long = this.consumedForBoot,
        bootWindowStartVerifiedMillis: Long = this.bootWindowStartVerifiedMillis,
        bootsInWindow: Int = this.bootsInWindow,
    ): BlindCreditLedger = BlindCreditLedger(
        availableMillis, carriedRemainder, windowStartVerifiedMillis, grantedInWindowMillis,
        consumedForBootPresent, consumedForBoot, bootWindowStartVerifiedMillis, bootsInWindow,
    )

    public companion object {
        public fun readFrom(reader: CanonicalReader): BlindCreditLedger = BlindCreditLedger(
            availableMillis = reader.readI64(),
            carriedRemainder = reader.readI64(),
            windowStartVerifiedMillis = reader.readI64(),
            grantedInWindowMillis = reader.readI64(),
            consumedForBootPresent = reader.readBool(),
            consumedForBoot = reader.readI64(),
            bootWindowStartVerifiedMillis = reader.readI64(),
            bootsInWindow = reader.readI32(),
        )

        /**
         * Genesis starts with zero credit, not a full budget.
         *
         * A full starting budget would be free blind progression for the first
         * reboot after install, which is exactly the thing the credit model
         * exists to deny.
         */
        public fun genesis(): BlindCreditLedger = BlindCreditLedger(
            availableMillis = 0L,
            carriedRemainder = 0L,
            windowStartVerifiedMillis = 0L,
            grantedInWindowMillis = 0L,
            consumedForBootPresent = false,
            consumedForBoot = 0L,
            bootWindowStartVerifiedMillis = 0L,
            bootsInWindow = 0,
        )
    }
}

/** Unresolved-time debt accounting, contract section 6. */
public class DebtLedgerState(
    public val state: DebtState,
    public val outstandingBaselineEquivMillis: Long,
    public val collectedBaselineEquivMillis: Long,
    public val forgivenBaselineEquivMillis: Long,
    public val oldestAccrualVerifiedMillis: Long,
    public val dayWindowStartVerifiedMillis: Long,
    public val collectedInDayBaselineEquivMillis: Long,
    public val abundanceStablePresent: Boolean,
    public val abundanceStableSinceVerifiedMillis: Long,
    public val rearmEffectivePresent: Boolean,
    public val rearmEffectiveAtVerifiedMillis: Long,
) {
    public fun writeTo(writer: CanonicalWriter): CanonicalWriter = writer
        .putEnum(state.ordinal32)
        .putI64(outstandingBaselineEquivMillis)
        .putI64(collectedBaselineEquivMillis)
        .putI64(forgivenBaselineEquivMillis)
        .putI64(oldestAccrualVerifiedMillis)
        .putI64(dayWindowStartVerifiedMillis)
        .putI64(collectedInDayBaselineEquivMillis)
        .putBool(abundanceStablePresent)
        .putI64(if (abundanceStablePresent) abundanceStableSinceVerifiedMillis else 0L)
        .putBool(rearmEffectivePresent)
        .putI64(if (rearmEffectivePresent) rearmEffectiveAtVerifiedMillis else 0L)

    public fun copy(
        state: DebtState = this.state,
        outstandingBaselineEquivMillis: Long = this.outstandingBaselineEquivMillis,
        collectedBaselineEquivMillis: Long = this.collectedBaselineEquivMillis,
        forgivenBaselineEquivMillis: Long = this.forgivenBaselineEquivMillis,
        oldestAccrualVerifiedMillis: Long = this.oldestAccrualVerifiedMillis,
        dayWindowStartVerifiedMillis: Long = this.dayWindowStartVerifiedMillis,
        collectedInDayBaselineEquivMillis: Long = this.collectedInDayBaselineEquivMillis,
        abundanceStablePresent: Boolean = this.abundanceStablePresent,
        abundanceStableSinceVerifiedMillis: Long = this.abundanceStableSinceVerifiedMillis,
        rearmEffectivePresent: Boolean = this.rearmEffectivePresent,
        rearmEffectiveAtVerifiedMillis: Long = this.rearmEffectiveAtVerifiedMillis,
    ): DebtLedgerState = DebtLedgerState(
        state, outstandingBaselineEquivMillis, collectedBaselineEquivMillis,
        forgivenBaselineEquivMillis, oldestAccrualVerifiedMillis, dayWindowStartVerifiedMillis,
        collectedInDayBaselineEquivMillis, abundanceStablePresent,
        abundanceStableSinceVerifiedMillis, rearmEffectivePresent, rearmEffectiveAtVerifiedMillis,
    )

    public companion object {
        public fun readFrom(reader: CanonicalReader): DebtLedgerState = DebtLedgerState(
            state = DebtState.fromOrdinal(reader.readEnum()),
            outstandingBaselineEquivMillis = reader.readI64(),
            collectedBaselineEquivMillis = reader.readI64(),
            forgivenBaselineEquivMillis = reader.readI64(),
            oldestAccrualVerifiedMillis = reader.readI64(),
            dayWindowStartVerifiedMillis = reader.readI64(),
            collectedInDayBaselineEquivMillis = reader.readI64(),
            abundanceStablePresent = reader.readBool(),
            abundanceStableSinceVerifiedMillis = reader.readI64(),
            rearmEffectivePresent = reader.readBool(),
            rearmEffectiveAtVerifiedMillis = reader.readI64(),
        )

        public fun genesis(): DebtLedgerState = DebtLedgerState(
            state = DebtState.IDLE,
            outstandingBaselineEquivMillis = 0L,
            collectedBaselineEquivMillis = 0L,
            forgivenBaselineEquivMillis = 0L,
            oldestAccrualVerifiedMillis = 0L,
            dayWindowStartVerifiedMillis = 0L,
            collectedInDayBaselineEquivMillis = 0L,
            abundanceStablePresent = false,
            abundanceStableSinceVerifiedMillis = 0L,
            rearmEffectivePresent = false,
            rearmEffectiveAtVerifiedMillis = 0L,
        )
    }
}

/** Identity binding and recovery freshness, contract sections 14.2 and 14.4. */
public class IdentityState(
    public val organismId: Long,
    public val identityEpoch: Int,
    public val lineageHash: ByteArray,
    public val deviceFingerprint: Long,
    public val quarantined: Boolean,
    public val lastProtectedSequence: Long,
    public val lastProtectedVerifiedMillis: Long,
) {
    init {
        if (lineageHash.size != CanonicalHash.DIGEST_SIZE) {
            throw IllegalArgumentException(
                "lineage hash must be ${CanonicalHash.DIGEST_SIZE} bytes, got ${lineageHash.size}",
            )
        }
    }

    public fun writeTo(writer: CanonicalWriter): CanonicalWriter = writer
        .putI64(organismId)
        .putI32(identityEpoch)
        .putBytes(lineageHash)
        .putI64(deviceFingerprint)
        .putBool(quarantined)
        .putI64(lastProtectedSequence)
        .putI64(lastProtectedVerifiedMillis)

    public fun copy(
        organismId: Long = this.organismId,
        identityEpoch: Int = this.identityEpoch,
        lineageHash: ByteArray = this.lineageHash,
        deviceFingerprint: Long = this.deviceFingerprint,
        quarantined: Boolean = this.quarantined,
        lastProtectedSequence: Long = this.lastProtectedSequence,
        lastProtectedVerifiedMillis: Long = this.lastProtectedVerifiedMillis,
    ): IdentityState = IdentityState(
        organismId, identityEpoch, lineageHash, deviceFingerprint, quarantined,
        lastProtectedSequence, lastProtectedVerifiedMillis,
    )

    public companion object {
        public fun readFrom(reader: CanonicalReader): IdentityState = IdentityState(
            organismId = reader.readI64(),
            identityEpoch = reader.readI32(),
            lineageHash = reader.readBytes(),
            deviceFingerprint = reader.readI64(),
            quarantined = reader.readBool(),
            lastProtectedSequence = reader.readI64(),
            lastProtectedVerifiedMillis = reader.readI64(),
        )

        public fun genesis(organismId: Long, deviceFingerprint: Long): IdentityState = IdentityState(
            organismId = organismId,
            identityEpoch = 1,
            lineageHash = CanonicalHash.ofEnvelope(
                CanonicalEnvelope.wrap(219, 1, CanonicalWriter(8).putI64(organismId).toByteArray()),
            ),
            deviceFingerprint = deviceFingerprint,
            quarantined = false,
            lastProtectedSequence = 0L,
            lastProtectedVerifiedMillis = 0L,
        )
    }
}

/**
 * The full canonical continuity snapshot. Canonical schema `211`.
 *
 * Immutable for the same reason `CanonicalSnapshot` is: it makes
 * `reduce == replay` a comparison of values rather than of histories, and it
 * removes any possibility of observing a half-applied reconciliation chunk.
 */
public class ContinuityState(
    public val schemaVersion: Int,
    public val identity: IdentityState,
    public val wallClockAgeMillis: Long,
    public val activeExperienceTicks: Long,
    public val developmentalProgress: Long,
    public val circadianPhase: Long,
    public val verifiedTimeTotalMillis: Long,
    public val reserveA: Long,
    public val reserveB: Long,
    public val anchor: DurableTimeAnchor,
    public val credit: BlindCreditLedger,
    public val debt: DebtLedgerState,
    public val admissionState: DurabilityAdmissionState,
    public val presentationState: DurabilityPresentationState,
    public val platformState: PlatformProtectionState,
    public val safeHoldActive: Boolean,
    public val generationId: Long,
    public val lastCommitSequence: Long,
    public val gapProvenance: GapProvenance,
) {
    init {
        if (circadianPhase < 0L || circadianPhase >= FixedPoint.ONE) {
            throw IllegalArgumentException("circadian phase must lie in [0,1), got $circadianPhase")
        }
        requireReserve(reserveA, "reserveA")
        requireReserve(reserveB, "reserveB")
        if (wallClockAgeMillis < 0L) {
            throw IllegalArgumentException("wall clock age must not be negative")
        }
        if (verifiedTimeTotalMillis < 0L) {
            throw IllegalArgumentException("verified time total must not be negative")
        }
    }

    private fun requireReserve(value: Long, name: String) {
        if (value < 0L || value > FixedPoint.ONE) {
            throw IllegalArgumentException("$name must lie in [0,1], got $value")
        }
    }

    public fun copy(
        schemaVersion: Int = this.schemaVersion,
        identity: IdentityState = this.identity,
        wallClockAgeMillis: Long = this.wallClockAgeMillis,
        activeExperienceTicks: Long = this.activeExperienceTicks,
        developmentalProgress: Long = this.developmentalProgress,
        circadianPhase: Long = this.circadianPhase,
        verifiedTimeTotalMillis: Long = this.verifiedTimeTotalMillis,
        reserveA: Long = this.reserveA,
        reserveB: Long = this.reserveB,
        anchor: DurableTimeAnchor = this.anchor,
        credit: BlindCreditLedger = this.credit,
        debt: DebtLedgerState = this.debt,
        admissionState: DurabilityAdmissionState = this.admissionState,
        presentationState: DurabilityPresentationState = this.presentationState,
        platformState: PlatformProtectionState = this.platformState,
        safeHoldActive: Boolean = this.safeHoldActive,
        generationId: Long = this.generationId,
        lastCommitSequence: Long = this.lastCommitSequence,
        gapProvenance: GapProvenance = this.gapProvenance,
    ): ContinuityState = ContinuityState(
        schemaVersion, identity, wallClockAgeMillis, activeExperienceTicks, developmentalProgress,
        circadianPhase, verifiedTimeTotalMillis, reserveA, reserveB, anchor, credit, debt,
        admissionState, presentationState, platformState, safeHoldActive, generationId,
        lastCommitSequence, gapProvenance,
    )

    public fun canonicalBytes(): ByteArray {
        val body = CanonicalWriter(320)
        identity.writeTo(body)
        body.putI64(wallClockAgeMillis)
            .putI64(activeExperienceTicks)
            .putI64(developmentalProgress)
            .putI64(circadianPhase)
            .putI64(verifiedTimeTotalMillis)
            .putI64(reserveA)
            .putI64(reserveB)
        anchor.writeTo(body)
        credit.writeTo(body)
        debt.writeTo(body)
        body.putEnum(admissionState.ordinal32)
            .putEnum(presentationState.ordinal32)
            .putEnum(platformState.ordinal32)
            .putBool(safeHoldActive)
            .putI64(generationId)
            .putI64(lastCommitSequence)
            .putEnum(gapProvenance.ordinal32)
        return CanonicalEnvelope.wrap(SCHEMA_ID, schemaVersion, body.toByteArray())
    }

    public fun stateHash(): ByteArray = CanonicalHash.ofEnvelope(canonicalBytes())

    public fun stateHashHex(): String = CanonicalHash.hex(stateHash())

    override fun equals(other: Any?): Boolean =
        other is ContinuityState && canonicalBytes().contentEquals(other.canonicalBytes())

    override fun hashCode(): Int = canonicalBytes().contentHashCode()

    override fun toString(): String =
        "ContinuityState(age=${wallClockAgeMillis}ms verified=${verifiedTimeTotalMillis}ms " +
            "a=$reserveA b=$reserveB ${debt.state} $admissionState $platformState " +
            "hash=${stateHashHex().take(16)})"

    public companion object {
        public const val SCHEMA_ID: Int = 211
        public const val SCHEMA_VERSION: Int = 1

        public fun genesis(
            organismId: Long,
            deviceFingerprint: Long,
            reserveA: Long = FixedPoint.ONE,
            reserveB: Long = FixedPoint.ONE,
        ): ContinuityState = ContinuityState(
            schemaVersion = SCHEMA_VERSION,
            identity = IdentityState.genesis(organismId, deviceFingerprint),
            wallClockAgeMillis = 0L,
            activeExperienceTicks = 0L,
            developmentalProgress = 0L,
            circadianPhase = 0L,
            verifiedTimeTotalMillis = 0L,
            reserveA = reserveA,
            reserveB = reserveB,
            anchor = DurableTimeAnchor.genesis(),
            credit = BlindCreditLedger.genesis(),
            debt = DebtLedgerState.genesis(),
            admissionState = DurabilityAdmissionState.OPEN,
            presentationState = DurabilityPresentationState.RECOVERY_RECONCILIATION,
            platformState = PlatformProtectionState.NORMAL,
            safeHoldActive = false,
            generationId = 1L,
            lastCommitSequence = 0L,
            gapProvenance = GapProvenance.NONE,
        )

        public fun decode(bytes: ByteArray): ContinuityState {
            val contents = CanonicalEnvelope.unwrap(bytes)
            if (contents.payloadSchemaId != SCHEMA_ID) {
                throw IllegalArgumentException(
                    "expected continuity schema $SCHEMA_ID, got ${contents.payloadSchemaId}",
                )
            }
            if (contents.payloadSchemaVersion != SCHEMA_VERSION) {
                throw IllegalArgumentException(
                    "continuity schema version ${contents.payloadSchemaVersion} requires migration; " +
                        "decode never guesses across versions",
                )
            }
            val reader = CanonicalReader(contents.payload)
            val identity = IdentityState.readFrom(reader)
            val wallClockAgeMillis = reader.readI64()
            val activeExperienceTicks = reader.readI64()
            val developmentalProgress = reader.readI64()
            val circadianPhase = reader.readI64()
            val verifiedTimeTotalMillis = reader.readI64()
            val reserveA = reader.readI64()
            val reserveB = reader.readI64()
            val anchor = DurableTimeAnchor.readFrom(reader)
            val credit = BlindCreditLedger.readFrom(reader)
            val debt = DebtLedgerState.readFrom(reader)
            val admission = DurabilityAdmissionState.fromOrdinal(reader.readEnum())
            val presentation = DurabilityPresentationState.fromOrdinal(reader.readEnum())
            val platform = PlatformProtectionState.fromOrdinal(reader.readEnum())
            val safeHold = reader.readBool()
            val generationId = reader.readI64()
            val lastCommitSequence = reader.readI64()
            val provenance = GapProvenance.fromOrdinal(reader.readEnum())
            reader.requireExhausted()

            return ContinuityState(
                contents.payloadSchemaVersion, identity, wallClockAgeMillis, activeExperienceTicks,
                developmentalProgress, circadianPhase, verifiedTimeTotalMillis, reserveA, reserveB,
                anchor, credit, debt, admission, presentation, platform, safeHold, generationId,
                lastCommitSequence, provenance,
            )
        }
    }
}
