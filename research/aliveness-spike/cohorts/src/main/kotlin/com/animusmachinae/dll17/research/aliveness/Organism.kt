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
    SKILL_PROFICIENCY,
    OUTCOME_UNCERTAINTY,
    EPISODIC_HISTORY,
    RELATIONSHIP_DIFFERENTIATION,
    CURIOSITY_PHASE_DRIFT,
    RECENT_INSPECTION_INHIBITION,
    TIERED_COMMITMENT;

    public companion object {
        /** Every mechanism the A000 track has implemented. */
        public val FULL_SET: Set<Mechanism> = entries.toSet()

        /**
         * The mechanisms FULL actually carries after D009.
         *
         * `EPISODIC_HISTORY` is excluded. A revised, context-conditioned,
         * salience-retained form was measured against a five-seed matrix and
         * still did not add history-dependent individuality, so it was removed
         * rather than kept because episodic memory is theoretically desirable.
         * History dependence itself is unaffected — preference, habit, skill,
         * fear, relationship value and outcome uncertainty are all history-derived.
         */
        public val QUALIFIED_SET: Set<Mechanism> = FULL_SET - EPISODIC_HISTORY
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

            Mechanism.HABIT_EXPECTANCY,
            Mechanism.SKILL_PROFICIENCY,
            Mechanism.OUTCOME_UNCERTAINTY,
            -> HABIT_OR_EXPECTANCY
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
    /**
     * The context this episode happened in. Recall matches on it, which is what
     * separates episodic memory from a second copy of `preference`.
     */
    public val context: Int,
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

    /**
     * How much company this individual needs before company becomes a Tier 3
     * concern. Metabolic thresholds stay species-level; this one does not,
     * because identical thresholds across a population force identical time
     * budgets and therefore near-identical action distributions.
     */
    public val socialNeedThreshold: Long =
        SpikeContract.LOW_SOCIAL * (500_000L + sociability * SpikeContract.SOCIAL_THRESHOLD_TRAIT_SPAN /
            FixedPoint.SCALE) / FixedPoint.SCALE

    /** A cautious individual writes something off as dangerous sooner. */
    public val avoidanceThreshold: Long =
        SpikeContract.FEAR_AVOIDANCE_THRESHOLD * (1_400_000L - caution) / FixedPoint.SCALE

    private companion object {
        /** Uniform in `[0.15, 0.90]` so no organism is degenerate on any axis. */
        fun derive(seed: Long, index: Int): Long {
            var z = seed * 0x9E3779B97F4A7C15uL.toLong() + index * 0x632BE59BD9B4E019L
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            z = z xor (z ushr 31)
            val unit = (z and Long.MAX_VALUE) % 750_001L
            return FixedPoint.of(0L, 150_000L + unit)
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

    /** Consecutive engagement budget and its refractory deadline, per object. */
    public val engagementTicks: IntArray = IntArray(HabitatObject.COUNT)
    public val engagementRefractoryUntil: LongArray = LongArray(HabitatObject.COUNT)

    /** Habit strength per (action, object). Fixed-size matrix; never grows. */
    public val habit: LongArray = LongArray(SpikeAction.ALL.size * HabitatObject.COUNT)

    /** Bounded competence per (action, object). Grows with validated practice. */
    public val skill: LongArray = LongArray(SpikeAction.ALL.size * HabitatObject.COUNT)

    /** How stale or contradicted this option's value estimate is. */
    public val uncertainty: LongArray =
        LongArray(SpikeAction.ALL.size * HabitatObject.COUNT) { SpikeContract.UNCERTAINTY_INITIAL }

    /** Bounded retry/futility accounting per (action, object). */
    public val failureCount: IntArray = IntArray(SpikeAction.ALL.size * HabitatObject.COUNT)
    public val suppressedUntilTick: LongArray =
        LongArray(SpikeAction.ALL.size * HabitatObject.COUNT)

    /** Bounded episodic ring buffer. */
    public val episodes: Array<Episode?> = arrayOfNulls(SpikeContract.EPISODIC_CAPACITY)
    public var episodeCount: Int = 0
        private set

    // ---------------------------------------------------------- commitment
    public var committedAction: SpikeAction? = null
    public var committedTarget: HabitatObject? = null
    public var committedTier: Int = 5
    public var commitmentRemaining: Int = 0
    public var interruptedAction: SpikeAction? = null
    public var interruptedTarget: HabitatObject? = null
    public var interruptedAtTick: Long = Long.MIN_VALUE / 4
    public val refractoryUntilTick: LongArray = LongArray(SpikeAction.ALL.size)

    /** Diminishing marginal utility per action kind. */
    public val actionSatiation: LongArray = LongArray(SpikeAction.ALL.size)
    public var opportunityWindowUntilTick: Long = 0L

    /** When and at what a threatening outcome last occurred. */
    public var lastThreatTick: Long = Long.MIN_VALUE / 4
    public var lastThreatTarget: HabitatObject? = null

    /** Set by the runtime when a normalized user input arrives. */
    public var pendingTouchFrom: HabitatObject? = null
    /** Fixed-size salience buffer: the newest event replaces the old one. */
    public var pendingStimulus: PendingStimulus? = null
    public var lastInteractionTick: Long = Long.MIN_VALUE / 4

    public fun has(m: Mechanism): Boolean = m in mechanisms

    public fun index(action: SpikeAction, target: HabitatObject): Int =
        action.ordinal * HabitatObject.COUNT + target.ordinal0

    /**
     * Bounded episodic admission by salience, not recency.
     *
     * A pure ring buffer of the last N events cannot hold history: in a matched
     * probe both organisms refill it with the same present, so the mechanism
     * converges them instead of distinguishing them — which is what the D008
     * measurement showed. Evicting the least consequential episode instead lets
     * a strongly-valenced early experience persist and stay individual.
     */
    public fun recordEpisode(e: Episode) {
        if (episodeCount < SpikeContract.EPISODIC_CAPACITY) {
            episodes[episodeCount] = e
            episodeCount += 1
            return
        }
        val salience = if (e.valence < 0L) -e.valence else e.valence
        var weakestIndex = -1
        var weakestSalience = Long.MAX_VALUE
        var weakestTick = Long.MAX_VALUE
        for (index in episodes.indices) {
            val candidate = episodes[index] ?: continue
            val candidateSalience =
                if (candidate.valence < 0L) -candidate.valence else candidate.valence
            if (candidateSalience < weakestSalience ||
                (candidateSalience == weakestSalience && candidate.tick < weakestTick)
            ) {
                weakestIndex = index
                weakestSalience = candidateSalience
                weakestTick = candidate.tick
            }
        }
        if (weakestIndex >= 0 && salience >= weakestSalience) {
            episodes[weakestIndex] = e
        }
    }

    /** Total bytes of mutable state. Constant by construction; measured, not assumed. */
    public fun stateFootprintSlots(): Int =
        habituation.size + habituationCeiling.size + sensitization.size + preference.size +
            fear.size + relationship.size + inhibition.size + absoluteShift.size +
            absoluteShiftReservoir.size + contextAmplitude.size + lastInspectedTick.size +
            engagementTicks.size + engagementRefractoryUntil.size +
            habit.size + skill.size + uncertainty.size +
            failureCount.size + suppressedUntilTick.size +
            SpikeContract.EPISODIC_CAPACITY + refractoryUntilTick.size + actionSatiation.size

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
        for (a in skill) mix(a)
        for (a in uncertainty) mix(a)
        for (a in absoluteShift) mix(a)
        for (a in actionSatiation) mix(a)
        mix(episodeCount.toLong())
        for (e in episodes) mix(e?.valence ?: 0L)
        return h
    }
}
