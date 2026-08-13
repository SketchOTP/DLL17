package com.animusmachinae.dll17.core.continuity

import kotlin.math.abs

/**
 * One platform reading of the clocks, as observed at startup or at a barrier.
 *
 * This is the **only** place a platform time value enters continuity logic, and
 * it enters as evidence rather than as authority. Everything downstream consumes
 * a classified [ElapsedEvidence], never a raw reading, which is what makes the
 * whole subsystem testable off-device and impossible to accidentally trust.
 */
public class ClockObservation(
    public val wallClockUtcMillis: Long,
    public val elapsedRealtimeMillis: Long,
    public val bootIdentityPresent: Boolean,
    public val bootIdentity: Long,
    public val authenticatedTimePresent: Boolean = false,
    public val authenticatedTimeMillis: Long = 0L,
) {
    override fun toString(): String =
        "ClockObservation(wall=$wallClockUtcMillis elapsed=$elapsedRealtimeMillis " +
            "boot=${if (bootIdentityPresent) bootIdentity.toString() else "absent"})"
}

/**
 * The classified result of comparing an anchor against an observation.
 *
 * [verifiedMillis] and [unverifiedMillis] are never both non-zero: an interval
 * is either qualified elapsed time or it is not, and blurring the two is how a
 * spoofable reading turns into biology.
 */
public class ElapsedEvidence(
    public val confidence: TimeConfidence,
    /** Qualified elapsed time. Zero unless the confidence grants it. */
    public val verifiedMillis: Long,
    /** Believed but unverifiable interval. Requests blind credit; grants nothing itself. */
    public val unverifiedMillis: Long,
    public val skewMillis: Long,
    public val anomaly: Boolean,
    public val bootChanged: Boolean,
) {
    override fun toString(): String =
        "ElapsedEvidence($confidence verified=${verifiedMillis}ms " +
            "unverified=${unverifiedMillis}ms skew=${skewMillis}ms anomaly=$anomaly)"
}

/**
 * Classification of an interval, contract section 4.
 *
 * Pure in `(anchor, observation)`. It reads no clock of its own, which is the
 * only reason a "one year absence" fixture is expressible at all.
 */
public object ClockTrust {

    public fun classify(
        anchor: DurableTimeAnchor,
        observation: ClockObservation,
    ): ElapsedEvidence {
        val bootChanged = !(
            anchor.bootIdentityPresent &&
                observation.bootIdentityPresent &&
                anchor.bootIdentity == observation.bootIdentity
            )

        // Authenticated time outranks everything: it establishes a real interval
        // that survives a reboot, which is the one thing monotonic uptime cannot
        // do.
        if (anchor.authenticatedTimePresent && observation.authenticatedTimePresent) {
            val interval = observation.authenticatedTimeMillis - anchor.authenticatedTimeMillis
            return ElapsedEvidence(
                confidence = TimeConfidence.AUTHENTICATED,
                // Backward authenticated movement clamps to zero. A negative
                // interval is not a shorter interval; it is a broken source.
                verifiedMillis = maxOf(0L, interval),
                unverifiedMillis = 0L,
                skewMillis = 0L,
                anomaly = false,
                bootChanged = bootChanged,
            )
        }

        val uptimeWentBackwards = observation.elapsedRealtimeMillis < anchor.elapsedRealtimeMillis
        if (bootChanged || uptimeWentBackwards) {
            // Nothing about this interval is verifiable. The wall clock may
            // suggest a duration, and that suggestion may only ever *reduce* a
            // request; it can never establish one.
            val wallDelta = observation.wallClockUtcMillis - anchor.wallClockUtcMillis
            return ElapsedEvidence(
                confidence = TimeConfidence.UNVERIFIED_REBOOT,
                verifiedMillis = 0L,
                unverifiedMillis = maxOf(0L, wallDelta),
                skewMillis = 0L,
                anomaly = false,
                bootChanged = bootChanged,
            )
        }

        val elapsedDelta = observation.elapsedRealtimeMillis - anchor.elapsedRealtimeMillis
        val wallDelta = observation.wallClockUtcMillis - anchor.wallClockUtcMillis
        val skew = abs(wallDelta - elapsedDelta)

        if (skew > ContinuityContract.WALL_ELAPSED_SKEW_TOLERANCE_MILLIS) {
            // Contract section 4.1: an anomalous interval grants blind credit
            // only. This is deliberately stricter than treating monotonic uptime
            // as authoritative regardless of the wall clock: it can only ever
            // grant *less* progression, never more, and it costs nothing an
            // attacker could want.
            return ElapsedEvidence(
                confidence = TimeConfidence.ANOMALOUS,
                verifiedMillis = 0L,
                unverifiedMillis = elapsedDelta,
                skewMillis = skew,
                anomaly = true,
                bootChanged = false,
            )
        }

        return ElapsedEvidence(
            confidence = TimeConfidence.VERIFIED_MONOTONIC,
            verifiedMillis = elapsedDelta,
            unverifiedMillis = 0L,
            skewMillis = skew,
            anomaly = false,
            bootChanged = false,
        )
    }

    /** Builds the anchor to be written for [observation] under [evidence]. */
    public fun anchorFor(
        previous: DurableTimeAnchor,
        observation: ClockObservation,
        evidence: ElapsedEvidence,
        logicalTime: Long,
    ): DurableTimeAnchor = DurableTimeAnchor(
        anchorSequence = previous.anchorSequence + 1L,
        wallClockUtcMillis = observation.wallClockUtcMillis,
        elapsedRealtimeMillis = observation.elapsedRealtimeMillis,
        bootIdentityPresent = observation.bootIdentityPresent,
        bootIdentity = observation.bootIdentity,
        logicalTime = logicalTime,
        timeConfidence = evidence.confidence,
        authenticatedTimePresent = observation.authenticatedTimePresent,
        authenticatedTimeMillis = observation.authenticatedTimeMillis,
    )
}

/** The result of a replenishment computation. */
public class ReplenishmentResult(
    public val grantedMillis: Long,
    public val carriedRemainder: Long,
    public val windowReset: Boolean,
)

/**
 * Blind-decay credit arithmetic, contract section 5.
 *
 * Every function here is pure and takes the ledger explicitly, so the exploit
 * cases — repeated reboots, fragmented sessions, a boot that already spent its
 * credit — are expressible as ordinary unit tests rather than as device
 * scenarios nobody can rerun.
 */
public object BlindCredit {

    /**
     * Credit earned by `verifiedMillis` of verified elapsed time.
     *
     * The remainder is carried rather than discarded. Discarding it would make
     * six one-millisecond intervals grant less than one six-millisecond
     * interval, and an attacker who can fragment a session can exploit any such
     * asymmetry in whichever direction favours them.
     */
    public fun replenish(
        ledger: BlindCreditLedger,
        verifiedTimeTotalBefore: Long,
        verifiedMillis: Long,
    ): ReplenishmentResult {
        if (verifiedMillis < 0L) {
            throw IllegalArgumentException("verified interval must not be negative")
        }
        val windowElapsed = verifiedTimeTotalBefore - ledger.windowStartVerifiedMillis
        val windowReset = windowElapsed >= ContinuityContract.BLIND_CREDIT_REPLENISH_WINDOW_MILLIS
        val grantedInWindow = if (windowReset) 0L else ledger.grantedInWindowMillis

        val pool = ledger.carriedRemainder + verifiedMillis
        val uncapped = pool / ContinuityContract.BLIND_CREDIT_REPLENISH_DIVISOR
        val carried = pool % ContinuityContract.BLIND_CREDIT_REPLENISH_DIVISOR

        val windowHeadroom =
            (ContinuityContract.BLIND_CREDIT_REPLENISH_CAP_MILLIS - grantedInWindow)
                .coerceAtLeast(0L)
        // Excess beyond the window cap is dropped rather than carried. Carrying
        // it would turn the cap into a delay instead of a limit.
        val granted = minOf(uncapped, windowHeadroom)

        return ReplenishmentResult(granted, carried, windowReset)
    }

    /**
     * How much blind decay this interval may actually consume.
     *
     * Returns zero when the boot already spent its credit, when a boot-velocity
     * anomaly is in force, or when the budget is exhausted. Each of those is a
     * separate exploit and each is closed here rather than at a call site.
     */
    public fun consumable(
        ledger: BlindCreditLedger,
        requestedMillis: Long,
        bootIdentity: Long,
        bootVelocityAnomaly: Boolean,
    ): Long {
        if (requestedMillis <= 0L) return 0L
        if (bootVelocityAnomaly) return 0L
        if (ledger.consumedForBootPresent && ledger.consumedForBoot == bootIdentity) return 0L
        return minOf(requestedMillis, ledger.availableMillis)
    }

    /** Whether observing a new boot now constitutes a velocity anomaly. */
    public fun bootVelocityAnomaly(
        ledger: BlindCreditLedger,
        verifiedTimeTotal: Long,
    ): Boolean {
        val windowElapsed = verifiedTimeTotal - ledger.bootWindowStartVerifiedMillis
        if (windowElapsed >= ContinuityContract.BOOT_VELOCITY_WINDOW_MILLIS) return false
        return ledger.bootsInWindow + 1 > ContinuityContract.BOOT_VELOCITY_MAX_BOOTS
    }
}
