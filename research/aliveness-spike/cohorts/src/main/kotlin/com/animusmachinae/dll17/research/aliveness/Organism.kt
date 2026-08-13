package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * The A000 candidate mechanism set, enumerated so a cohort is defined by which
 * mechanisms it carries rather than by a separate implementation.
 *
 * These are research mechanisms. Their presence here does not qualify or
 * authorize the corresponding R003–R009 production contracts.
 */
public enum class Mechanism {
    HABITUATION,
    DISHABITUATION_RECOVERY,
    SENSITIZATION,
    PREFERENCE_LEARNING,
    CONDITIONED_FEAR,
    FEAR_EXTINCTION,
    HABIT_EXPECTANCY,
    EPISODIC_HISTORY,
    RELATIONSHIP_DIFFERENTIATION,
    CURIOSITY_PHASE_DRIFT,
    RECENT_INSPECTION_INHIBITION,
    TIERED_COMMITMENT;

    public companion object {
        public val FULL_SET: Set<Mechanism> = entries.toSet()
    }
}

/**
 * `MechanismCoalitionSetV1`. Exactly six groups, so exhaustive coalition
 * evaluation stays at 2^6 = 64 recomputations per scored action and no sampling
 * approximation is required.
 */
public enum class MechanismGroup(public val groupOrdinal: Int) {
    PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE(0),
    LEARNED_PREFERENCE(1),
    EPISODIC_OR_HISTORY(2),
    HABIT_OR_EXPECTANCY(3),
    SOCIAL_OR_RELATIONSHIP_HISTORY(4),
    CURIOSITY_OSCILLATOR(5);

    public companion object {
        public val ALL: List<MechanismGroup> = entries.toList()
        public const val COUNT: Int = 6

        /** Every group except the oscillator carries substantive causal mass. */
        public val SUBSTANTIVE: List<MechanismGroup> = ALL.filter { it != CURIOSITY_OSCILLATOR }

        /** Which mechanisms feed which coalition group. */
        public fun groupOf(mechanism: Mechanism): MechanismGroup = when (mechanism) {
            Mechanism.HABITUATION,
            Mechanism.DISHABITUATION_RECOVERY,
            Mechanism.SENSITIZATION,
            Mechanism.PREFERENCE_LEARNING,
            Mechanism.CONDITIONED_FEAR,
            Mechanism.FEAR_EXTINCTION,
            -> LEARNED_PREFERENCE

            Mechanism.EPISODIC_HISTORY -> EPISODIC_OR_HISTORY
            Mechanism.HABIT_EXPECTANCY -> HABIT_OR_EXPECTANCY
            Mechanism.RELATIONSHIP_DIFFERENTIATION -> SOCIAL_OR_RELATIONSHIP_HISTORY

            Mechanism.CURIOSITY_PHASE_DRIFT,
            Mechanism.RECENT_INSPECTION_INHIBITION,
            -> CURIOSITY_OSCILLATOR

            Mechanism.TIERED_COMMITMENT -> PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE
        }
    }
}

/** One bounded autobiographical episode. */
public data class Episode(
    public val tick: Long,
    public val action: SpikeAction,
    public val target: HabitatObject?,
    public val person: HabitatObject?,
    public val valence: Long,
)

/**
 * Immutable per-organism personality. Derived from the seed so two organisms
 * with different seeds are genuinely different individuals rather than the same
 * individual with different noise.
 */
public class Traits(seed: Long) {
    public val curiosity: Long = derive(seed, 1)
    public val sociability: Long = derive(seed, 2)
    public val caution: Long = derive(seed, 3)
    public val persistence: Long = derive(seed, 4)

    private companion object {
        /** Uniform in `[0.25, 0.85]` so no organism is degenerate on any axis. */
        fun derive(seed: Long, index: Int): Long {
            var z = seed * 0x9E3779B97F4A7C15uL.toLong() + index * 0x632BE59BD9B4E019L
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            z = z xor (z ushr 31)
            val unit = (z and Long.MAX_VALUE) % 600_001L
            return FixedPoint.of(0L, 250_000L + unit)
        }
    }
}

/**
 * Mutable organism state. Every array is fixed-length and every value bounded,
 * so state size is constant regardless of history length — which is the property
 * the boundedness measure checks rather than assumes.
 */
public class OrganismState(
    public val seed: Long,
    public val mechanisms: Set<Mechanism>,
    public val curiosity: CuriosityParameters = CuriosityParameters.DEFAULT,
) {
    public val traits: Traits = Traits(seed)

    // ------------------------------------------------------------- drives
    public var energy: Long = FixedPoint.of(0L, 700_000L)
    public var rest: Long = FixedPoint.of(0L, 700_000L)
    public var safety: Long = FixedPoint.ONE
    public var social: Long = FixedPoint.of(0L, 600_000L)

    // --------------------------------------------------------- modulators
    public var arousal: Long = FixedPoint.of(0L, 300_000L)
    public var stress: Long = FixedPoint.of(0L, 150_000L)
    public var rewardExpectancy: Long = FixedPoint.of(0L, 400_000L)

    // ------------------------------------------------------------ learning
    public val habituation: LongArray = LongArray(HabitatObject.COUNT)
    public val habituationCeiling: LongArray = LongArray(HabitatObject.COUNT) { FixedPoint.ONE }
    public val sensitization: LongArray = LongArray(HabitatObject.COUNT)
    public val preference: LongArray = LongArray(HabitatObject.COUNT)
    public val fear: LongArray = LongArray(HabitatObject.COUNT)
    public val fearPeak: LongArray = LongArray(HabitatObject.COUNT)
    public val relationship: LongArray = LongArray(HabitatObject.COUNT)
    public val inhibition: LongArray = LongArray(HabitatObject.COUNT)
    public val absoluteShift: LongArray = LongArray(HabitatObject.COUNT)
    public val absoluteShiftReservoir: LongArray = LongArray(HabitatObject.COUNT) { FixedPoint.ONE }
    public val contextAmplitude: LongArray =
        LongArray(HabitatObject.COUNT) { curiosity.contextAmplitude }
    public val lastInspectedTick: LongArray = LongArray(HabitatObject.COUNT) { Long.MIN_VALUE / 4 }

    /** Habit strength per (action, object). Fixed-size matrix; never grows. */
    public val habit: LongArray = LongArray(SpikeAction.ALL.size * HabitatObject.COUNT)

    /** Bounded retry/futility accounting per (action, object). */
    public val failureCount: IntArray = IntArray(SpikeAction.ALL.size * HabitatObject.COUNT)
    public val suppressedUntilTick: LongArray =
        LongArray(SpikeAction.ALL.size * HabitatObject.COUNT)

    /** Bounded episodic ring buffer. */
    public val episodes: Array<Episode?> = arrayOfNulls(SpikeContract.EPISODIC_CAPACITY)
    public var episodeWriteIndex: Int = 0
        private set
    public var episodeCount: Int = 0
        private set

    // ---------------------------------------------------------- commitment
    public var committedAction: SpikeAction? = null
    public var committedTarget: HabitatObject? = null
    public var commitmentRemaining: Int = 0
    public var interruptedAction: SpikeAction? = null
    public var interruptedTarget: HabitatObject? = null
    public var interruptedAtTick: Long = Long.MIN_VALUE / 4
    public val refractoryUntilTick: LongArray = LongArray(SpikeAction.ALL.size)
    public var opportunityWindowUntilTick: Long = 0L

    /** Set by the runtime when a normalized user input arrives. */
    public var pendingTouchFrom: HabitatObject? = null
    public var lastInteractionTick: Long = Long.MIN_VALUE / 4

    public fun has(m: Mechanism): Boolean = m in mechanisms

    public fun index(action: SpikeAction, target: HabitatObject): Int =
        action.ordinal * HabitatObject.COUNT + target.ordinal0

    public fun recordEpisode(e: Episode) {
        episodes[episodeWriteIndex] = e
        episodeWriteIndex = (episodeWriteIndex + 1) % SpikeContract.EPISODIC_CAPACITY
        if (episodeCount < SpikeContract.EPISODIC_CAPACITY) episodeCount += 1
    }

    /** Total bytes of mutable state. Constant by construction; measured, not assumed. */
    public fun stateFootprintSlots(): Int =
        habituation.size + habituationCeiling.size + sensitization.size + preference.size +
            fear.size + relationship.size + inhibition.size + absoluteShift.size +
            absoluteShiftReservoir.size + contextAmplitude.size + lastInspectedTick.size +
            habit.size + failureCount.size + suppressedUntilTick.size +
            SpikeContract.EPISODIC_CAPACITY + refractoryUntilTick.size

    /** Canonical-ish digest of the behavioural state, used for determinism checks. */
    public fun stateSignature(): Long {
        var h = 0xCBF29CE484222325uL.toLong()
        fun mix(v: Long) {
            h = (h xor v) * 0x100000001B3L
        }
        mix(energy); mix(rest); mix(safety); mix(social)
        mix(arousal); mix(stress); mix(rewardExpectancy)
        for (a in habituation) mix(a)
        for (a in sensitization) mix(a)
        for (a in preference) mix(a)
        for (a in fear) mix(a)
        for (a in relationship) mix(a)
        for (a in inhibition) mix(a)
        for (a in habit) mix(a)
        for (a in absoluteShift) mix(a)
        mix(episodeCount.toLong())
        for (e in episodes) mix(e?.valence ?: 0L)
        return h
    }
}
