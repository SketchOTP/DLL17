package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * The executable form of `ContinuityDurabilityContractV1`.
 *
 * Every number and every ordinal in this file is quoted from the frozen contract
 * document at `docs/architecture/ContinuityDurabilityContractV1.md`. Nothing in
 * `core-continuity` may introduce a continuity-relevant constant that is not
 * declared here, for the same reason `DeterminismContractV1` forbids an
 * algorithm chosen inside implementation code: a threshold picked at its call
 * site is a threshold nobody reviewed.
 *
 * Durations are `i64` milliseconds throughout — never `Fixed64`, never a
 * timestamp difference computed at the point of use. Fractions of a reserve are
 * `Fixed64`. The two units never mix in one expression.
 */
public object ContinuityContract {

    public const val CONTRACT_ID: String = "ContinuityDurabilityContractV1"
    public const val CONTRACT_VERSION: Int = 1

    // ------------------------------------------------------------ section 4.2

    /** Tolerated `|wallDelta - elapsedDelta|` within one boot before an anomaly. */
    public const val WALL_ELAPSED_SKEW_TOLERANCE_MILLIS: Long = 120_000L

    // -------------------------------------------------------------- section 5

    public const val BLIND_DECAY_CREDIT_MAX_MILLIS: Long = 14_400_000L

    /** One millisecond of credit per six milliseconds of verified elapsed time. */
    public const val BLIND_CREDIT_REPLENISH_DIVISOR: Long = 6L

    /** Cap on credit granted per rolling window of verified time. */
    public const val BLIND_CREDIT_REPLENISH_CAP_MILLIS: Long = 14_400_000L
    public const val BLIND_CREDIT_REPLENISH_WINDOW_MILLIS: Long = 86_400_000L

    // ------------------------------------------------------------ section 4.3

    public const val BOOT_VELOCITY_WINDOW_MILLIS: Long = 3_600_000L
    public const val BOOT_VELOCITY_MAX_BOOTS: Int = 5

    // -------------------------------------------------------------- section 6

    public const val DEBT_GLOBAL_CAP_BASELINE_EQUIV_MILLIS: Long = 259_200_000L
    public const val DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS: Long = 900_000L
    public const val DEBT_PER_VERIFIED_DAY_CAP_BASELINE_EQUIV_MILLIS: Long = 21_600_000L
    public const val DEBT_RETENTION_HORIZON_MILLIS: Long = 2_592_000_000L
    public const val DEBT_VERIFIED_DAY_MILLIS: Long = 86_400_000L

    /** Fraction of reserve capacity at or below which collection pauses. */
    public val DEBT_SAFETY_FLOOR: Long = FixedPoint.of(0L, 200_000L)

    /** Fraction every affected reserve must reach before collection may rearm. */
    public val DEBT_ABUNDANCE_REARM: Long = FixedPoint.of(0L, 800_000L)

    public const val DEBT_REARM_STABILITY_MILLIS: Long = 3_600_000L
    public const val DEBT_REARM_GRACE_MILLIS: Long = 1_800_000L

    /** A reveal may never leave a reserve inside this margin of collapse. */
    public val DEBT_POST_REVEAL_COLLAPSE_MARGIN: Long = FixedPoint.of(0L, 50_000L)

    // -------------------------------------------------------------- section 7

    public const val MODE_A_MAX_MILLIS: Long = 300_000L
    public const val MODE_B_MAX_MILLIS: Long = 259_200_000L

    /** Chunk schedule boundaries, measured from the start of the absence. */
    public const val CHUNK_TIER_1_END_MILLIS: Long = 21_600_000L
    public const val CHUNK_TIER_2_END_MILLIS: Long = 86_400_000L
    public const val CHUNK_TIER_3_END_MILLIS: Long = 259_200_000L

    public const val CHUNK_TIER_1_SIZE_MILLIS: Long = 60_000L
    public const val CHUNK_TIER_2_SIZE_MILLIS: Long = 300_000L
    public const val CHUNK_TIER_3_SIZE_MILLIS: Long = 900_000L

    public const val MODE_C_MAX_PASSIVE_DEVELOPMENT_MILLIS: Long = 259_200_000L

    /** Chunks applied before reconciliation must yield to its caller. */
    public const val RECONCILIATION_SLICE_CHUNKS: Int = 64

    // -------------------------------------------------------------- section 8

    public const val CLASS_O_COMMIT_CADENCE_MILLIS: Long = 500L
    public const val CLASS_O_MAX_UNCOMMITTED_WINDOW_MILLIS: Long = 1_000L
    public const val PANIC_WITNESS_ATTEMPT_DEADLINE_MILLIS: Long = 20L

    // -------------------------------------------------------------- section 9

    public const val JOURNAL_BYTE_BUDGET: Long = 8_388_608L
    public const val EMERGENCY_DURABILITY_RESERVE_BYTES: Long = 65_536L

    /** Soft flip threshold: nine tenths of the budget, integer arithmetic only. */
    public val JOURNAL_SOFT_FLIP_BYTES: Long = JOURNAL_BYTE_BUDGET * 9L / 10L

    // ------------------------------------------------------------- section 12

    public const val THERMAL_REENTRY_HYSTERESIS_MILLIS: Long = 60_000L

    // ------------------------------------------------------------- section 14

    public const val RECOVERY_STALE_WARNING_MILLIS: Long = 86_400_000L
    public const val RECOVERY_CRITICAL_WARNING_MILLIS: Long = 604_800_000L

    // ---------------------------------------------------- R002 fixture values

    /**
     * Neutral fixture drain, in `Fixed64` reserve fraction per minute of
     * baseline-equivalent metabolism.
     *
     * This is **not** physiology. It exists so that a reconciliation has an
     * observable effect, and it is named so that no later phase can mistake it
     * for a species value. R003 replaces it behind the A001 gate.
     */
    public val FIXTURE_RESERVE_DRAIN_PER_MINUTE: Long = FixedPoint.of(0L, 1_000L)

    /**
     * Neutral fixture passive development, in `Fixed64` progress units per minute
     * of qualified verified absence. Also not physiology, and granted only from
     * verified time — never from blind credit and never from an anomalous
     * interval.
     */
    public val FIXTURE_PASSIVE_DEVELOPMENT_PER_MINUTE: Long = FixedPoint.of(0L, 100L)

    /** One minute, in milliseconds, for converting the drain above. */
    public const val MILLIS_PER_MINUTE: Long = 60_000L
}

/**
 * How much a stored anchor can be trusted about the interval since it was
 * written. Ordinals are immutable and never reused.
 */
public enum class TimeConfidence(public val ordinal32: Int) {
    /** Boot identity unchanged and monotonic uptime did not move backwards. */
    VERIFIED_MONOTONIC(1),

    /** An authenticated external observation established the interval. */
    AUTHENTICATED(2),

    /** Reboot, uptime reset, or no comparable anchor. Blind credit only. */
    UNVERIFIED_REBOOT(3),

    /** Same boot, but the wall clock disagreed beyond tolerance. */
    ANOMALOUS(4),
    ;

    /** Whether this classification may grant full qualified elapsed biology. */
    public val grantsVerifiedTime: Boolean
        get() = this == VERIFIED_MONOTONIC || this == AUTHENTICATED

    public companion object {
        public fun fromOrdinal(value: Int): TimeConfidence = entries.firstOrNull {
            it.ordinal32 == value
        } ?: throw IllegalArgumentException("unknown time confidence ordinal $value")
    }
}

/** Debt lifecycle. Ordinals immutable. */
public enum class DebtState(public val ordinal32: Int) {
    IDLE(1),
    ACCRUED(2),
    COLLECTING(3),
    PAUSED_LOW_RESERVE(4),
    FORGIVEN(5),
    ;

    public companion object {
        public fun fromOrdinal(value: Int): DebtState = entries.firstOrNull {
            it.ordinal32 == value
        } ?: throw IllegalArgumentException("unknown debt state ordinal $value")
    }
}

/** Offline reconciliation mode. Selection is a pure function of duration. */
public enum class ReconciliationMode(public val ordinal32: Int) {
    MODE_A(1),
    MODE_B(2),
    MODE_C(3),
    ;

    public companion object {
        public fun fromOrdinal(value: Int): ReconciliationMode = entries.firstOrNull {
            it.ordinal32 == value
        } ?: throw IllegalArgumentException("unknown reconciliation mode ordinal $value")

        /** Contract section 7.1. */
        public fun forElapsed(millis: Long): ReconciliationMode = when {
            millis < 0L -> throw IllegalArgumentException(
                "elapsed duration must not be negative; backward movement clamps to zero " +
                    "before mode selection (got $millis)",
            )
            millis <= ContinuityContract.MODE_A_MAX_MILLIS -> MODE_A
            millis <= ContinuityContract.MODE_B_MAX_MILLIS -> MODE_B
            else -> MODE_C
        }
    }
}

/** Contract section 10.1. */
public enum class DurabilityAdmissionState(public val ordinal32: Int) {
    OPEN(1),
    PRESSURE(2),
    READ_ONLY_SURVIVAL(3),
    STORAGE_FAULT(4),
    ;

    /** Whether a mutation-producing input may reach the reducer at all. */
    public val admitsMutation: Boolean get() = this == OPEN || this == PRESSURE

    public companion object {
        public fun fromOrdinal(value: Int): DurabilityAdmissionState = entries.firstOrNull {
            it.ordinal32 == value
        } ?: throw IllegalArgumentException("unknown admission state ordinal $value")
    }
}

/**
 * Contract section 11.
 *
 * `STASIS_PROJECTION` is superseded and its ordinal is retired. It is absent
 * rather than deprecated: leaving it in the enum would let an old durable record
 * decode as a presentation the architecture now prohibits by name.
 */
public enum class DurabilityPresentationState(public val ordinal32: Int) {
    TEMPORAL_DESYNC(1),
    STORAGE_REPAIR_REQUIRED(2),
    RECOVERY_RECONCILIATION(3),
    ;

    public companion object {
        /** The ordinal `STASIS_PROJECTION` held. Never reused. */
        public const val RETIRED_STASIS_PROJECTION_ORDINAL: Int = 0

        public fun fromOrdinal(value: Int): DurabilityPresentationState = entries.firstOrNull {
            it.ordinal32 == value
        } ?: throw IllegalArgumentException("unknown presentation state ordinal $value")
    }
}

/** Contract section 12.1. */
public enum class PlatformProtectionState(public val ordinal32: Int) {
    NORMAL(1),
    RESOURCE_SHED(2),
    PLATFORM_DEEP_SUSPEND(3),
    PLATFORM_RECOVERY(4),
    ;

    public companion object {
        public fun fromOrdinal(value: Int): PlatformProtectionState = entries.firstOrNull {
            it.ordinal32 == value
        } ?: throw IllegalArgumentException("unknown platform state ordinal $value")
    }
}

/**
 * Boot and power provenance labels, contract section 4.3.
 *
 * Every one of these is a *label*. None of them creates, refunds, forgives or
 * fabricates canonical state, which is why they are a separate type from the
 * events that do.
 */
public enum class GapProvenance(public val ordinal32: Int) {
    NONE(1),
    BOOT_VELOCITY_ANOMALY(2),
    PROBABLE_POWER_LOSS(3),
    LIKELY_PLATFORM_FORCED_SUSPEND(4),
    LOST_TO_HARDWARE_FAULT(5),
    ;

    public companion object {
        public fun fromOrdinal(value: Int): GapProvenance = entries.firstOrNull {
            it.ordinal32 == value
        } ?: throw IllegalArgumentException("unknown gap provenance ordinal $value")
    }
}
