package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * A000 disposable aliveness spike — frozen research identifiers and constants.
 *
 * This is research code. Nothing here is a production contract, and the presence
 * of a mechanism in this file does not authorize its R003–R009 production
 * equivalent. The only production dependency is the frozen R001 fixed-point
 * numeric library (`core-math`).
 *
 * Every parameter below is a *research* tuning value. Where the canonical
 * architecture named a value (the 48-hour habituation half-life, the six-group
 * coalition ceiling, `alpha = 0.05`) it is used as named. Where it did not, the
 * value exists to make the mechanism testable and is recorded in
 * `SPIKE_PARAMETER_HASH` so a run can be tied to the parameterization that
 * produced it.
 */
public object SpikeContract {

    public const val SPIKE_ID: String = "A000-ALIVENESS-SPIKE"
    public const val SPIKE_VERSION: Int = 1

    /** Contract identifiers frozen under D008. */
    public const val EXPRESSION_CONTRACT_ID: String = "SpikeExpressionContractV1"
    public const val DECISION_TRACE_CONTRACT_ID: String = "SpikeDecisionTraceV1"
    public const val COALITION_SET_ID: String = "MechanismCoalitionSetV1"
    public const val COALITION_VALUE_FUNCTION_ID: String = "CoalitionValueFunctionV1"
    public const val ATTRIBUTION_CONTRACT_ID: String = "SpontaneousActionAttributionV1"
    public const val CURIOSITY_ENVELOPE_ID: String = "CuriosityBalanceEnvelopeV1"
    public const val SCRIPTED_BASELINE_ID: String = "ScriptedPetBaselineV1"
    public const val DEGRADED_CONTROL_ID: String = "DegradedScriptedControlV1"

    // ---------------------------------------------------------------- time

    /** One tick is one virtual minute. Chosen so a virtual day is 1,440 ticks. */
    public const val TICKS_PER_VIRTUAL_DAY: Int = 1_440
    public const val TICKS_PER_VIRTUAL_HOUR: Int = 60

    // ------------------------------------------------------------ drives

    /** Per-tick drive pressure. Fixed-point, applied with saturating arithmetic. */
    public val ENERGY_DECAY_PER_TICK: Long = FixedPoint.of(0L, 260L)
    public val REST_DECAY_PER_TICK: Long = FixedPoint.of(0L, 190L)
    public val SOCIAL_DECAY_PER_TICK: Long = FixedPoint.of(0L, 150L)
    /**
     * A scare should pass in a couple of virtual hours. At the D008 rate a single
     * punishment took most of a day to clear, so an organism that explored the
     * aversive object at all spent most of its life below the safety threshold.
     */
    public val SAFETY_RECOVERY_PER_TICK: Long = FixedPoint.of(0L, 4_000L)

    /**
     * Vigorous activity costs energy. Without this, play is free and an organism
     * that enjoys it can play for half of its life; with it, a long bout makes
     * the organism hungry and the day organizes itself into a cycle.
     */
    public val PLAY_ENERGY_COST_PER_TICK: Long = FixedPoint.of(0L, 1_300L)
    public val EXPLORE_ENERGY_COST_PER_TICK: Long = FixedPoint.of(0L, 700L)

    public val EAT_ENERGY_GAIN: Long = FixedPoint.of(0L, 34_000L)
    public val SLEEP_REST_GAIN_PER_TICK: Long = FixedPoint.of(0L, 2_600L)
    public val REST_REST_GAIN_PER_TICK: Long = FixedPoint.of(0L, 900L)
    public val SOCIAL_GAIN_PER_INTERACTION: Long = FixedPoint.of(0L, 26_000L)

    /** Tier 1 critical thresholds. */
    public val CRITICAL_ENERGY: Long = FixedPoint.of(0L, 180_000L)
    public val CRITICAL_REST: Long = FixedPoint.of(0L, 120_000L)
    public val CRITICAL_SAFETY: Long = FixedPoint.of(0L, 250_000L)

    /**
     * Tier 3 ordinary-need thresholds. `LOW_SOCIAL` is scaled per organism by
     * sociability (see `Traits.socialNeedThreshold`): metabolic needs are
     * species-level, but how much company an individual needs is not, and
     * identical thresholds across a population force identical time budgets.
     */
    public val LOW_ENERGY: Long = FixedPoint.of(0L, 520_000L)
    public val LOW_REST: Long = FixedPoint.of(0L, 480_000L)
    public val LOW_SOCIAL: Long = FixedPoint.of(0L, 500_000L)
    public val SOCIAL_THRESHOLD_TRAIT_SPAN: Long = FixedPoint.of(0L, 900_000L)

    // ------------------------------------------------------- modulators

    public val AROUSAL_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 994_000L)
    public val STRESS_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 997_000L)
    public val REWARD_EXPECTANCY_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 998_000L)

    // ------------------------------------------------------ habituation

    /**
     * Canonical §9 names a partial-recovery half-life "near 48 hours" and states
     * it is subject to simulation rather than treated as a biological constant.
     * 48 virtual hours is 2,880 ticks; the per-tick retention below halves a
     * trace over that interval.
     */
    public const val HABITUATION_HALF_LIFE_TICKS: Int = 48 * TICKS_PER_VIRTUAL_HOUR
    public val HABITUATION_RECOVERY_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 999_759L)
    public val HABITUATION_GAIN_PER_EXPOSURE: Long = FixedPoint.of(0L, 155_000L)
    public val HABITUATION_MAX: Long = FixedPoint.of(0L, 940_000L)

    /** Declining recovery ceiling: each cycle restores slightly less (§9). */
    public val HABITUATION_CEILING_DECLINE_PER_CYCLE: Long = FixedPoint.of(0L, 22_000L)
    public val HABITUATION_CEILING_FLOOR: Long = FixedPoint.of(0L, 400_000L)

    /** Dishabituation on a qualifying context change or prediction error. */
    public val DISHABITUATION_RELEASE: Long = FixedPoint.of(0L, 300_000L)

    /** Sensitization from a strong negative event, and its decay. */
    public val SENSITIZATION_GAIN: Long = FixedPoint.of(0L, 320_000L)
    public val SENSITIZATION_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 999_100L)

    // ------------------------------------------------- preference / fear

    public val PREFERENCE_LEARNING_RATE: Long = FixedPoint.of(0L, 120_000L)
    public val PREFERENCE_DECAY_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 999_950L)
    public val PREFERENCE_BOUND: Long = FixedPoint.ONE

    public val FEAR_ACQUISITION_RATE: Long = FixedPoint.of(0L, 400_000L)

    /** Extinction on explicit safe evidence: fast, and bounded below. */
    public val FEAR_EXTINCTION_RATE: Long = FixedPoint.of(0L, 90_000L)

    /**
     * Extinction is not erasure. Safe evidence cannot drive a fear below this
     * fraction of its historical peak; only the slow forgetting term below can,
     * and only over days without reinforcement.
     */
    public val FEAR_EXTINCTION_RESIDUAL_FRACTION: Long = FixedPoint.of(0L, 300_000L)

    /** Slow forgetting in the absence of reinforcement. Half-life ~3 virtual days. */
    public val FEAR_FORGETTING_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 999_850L)

    public val FEAR_AVOIDANCE_THRESHOLD: Long = FixedPoint.of(0L, 350_000L)

    /** How long after attending a feared object withdrawal stays a live reaction. */
    public const val WITHDRAW_REACTION_TICKS: Int = 5

    // ---------------------------------------------------- habit / expectancy

    public val HABIT_GAIN_ON_SUCCESS: Long = FixedPoint.of(0L, 90_000L)
    public val HABIT_LOSS_ON_FAILURE: Long = FixedPoint.of(0L, 140_000L)
    public val HABIT_DECAY_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 999_880L)
    public val HABIT_MAX: Long = FixedPoint.of(0L, 900_000L)

    // -------------------------------------------------- skill proficiency

    /**
     * Canonical §9 item 8: repeated validated attempts improve bounded
     * competence. Added under D009. Skill is what makes two organisms that
     * lived different lives end up *good at different things*, which is the
     * only route to long-run individuality that survives a homeostatic drive
     * model — every organism must eat and sleep about as much as every other.
     */
    public val SKILL_MAX: Long = FixedPoint.of(0L, 850_000L)
    public val SKILL_GAIN_ON_SUCCESS: Long = FixedPoint.of(0L, 55_000L)
    public val SKILL_DECAY_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 999_940L)

    // --------------------------------------- outcome uncertainty / re-sampling

    /**
     * Per action-and-object uncertainty about what an option is currently
     * worth. It falls when the option is sampled, rises slowly while it is
     * neglected, and jumps when an outcome contradicts the expectation.
     *
     * This is the directed-exploration term canonical §9 names alongside
     * prediction error and affordance validity. It is deliberately *not*
     * random action noise: an option is re-sampled because the organism's
     * estimate of it has gone stale or been contradicted, and the trace says so.
     */
    public val UNCERTAINTY_INITIAL: Long = FixedPoint.ONE
    public val UNCERTAINTY_DROP_ON_SAMPLE: Long = FixedPoint.of(0L, 340_000L)
    public val UNCERTAINTY_GROWTH_PER_TICK: Long = FixedPoint.of(0L, 130L)
    public val UNCERTAINTY_SURPRISE_GAIN: Long = FixedPoint.of(0L, 900_000L)
    public val UNCERTAINTY_SURPRISE_THRESHOLD: Long = FixedPoint.of(0L, 250_000L)

    // ------------------------------------------------------------ episodic

    /** Bounded autobiographical ring. Bounded by construction, not by policy. */
    public const val EPISODIC_CAPACITY: Int = 64

    /**
     * Revised under D009. The D008 form recalled a context-free mean valence
     * for the target, which is what `preference` already is, so it acted as a
     * second copy of the same estimate and measurably *reduced* history
     * divergence. The revised form recalls only episodes whose context matches
     * the present one, and contributes the **residual** over the context-free
     * preference — by construction it can only carry what preference cannot.
     */
    public val EPISODIC_WEIGHT: Long = FixedPoint.of(0L, 520_000L)
    public const val EPISODIC_RECENCY_WINDOW_TICKS: Int = 36 * TICKS_PER_VIRTUAL_HOUR

    /** Circadian quarter granularity for episodic context matching. */
    public const val EPISODIC_CONTEXT_BUCKETS: Int = 4

    // --------------------------------------------------------- relationship

    public val RELATIONSHIP_GAIN_POSITIVE: Long = FixedPoint.of(0L, 70_000L)
    public val RELATIONSHIP_LOSS_NEGATIVE: Long = FixedPoint.of(0L, 110_000L)
    public val RELATIONSHIP_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 999_970L)

    // ------------------------------------------------------------ curiosity

    /**
     * Object-specific low-frequency phase drift. Periods are pairwise co-prime
     * (canonical §9: "multiple bounded co-prime periods ... so priorities do not
     * collapse into one obvious A → B → C cycle"), expressed in ticks.
     */
    public val CURIOSITY_PERIODS_TICKS: IntArray = intArrayOf(1_009, 1_493, 2_161, 2_909, 3_701)

    public val CURIOSITY_BASE_FLOOR: Long = FixedPoint.of(0L, 40_000L)
    public val CURIOSITY_CONTEXT_AMPLITUDE: Long = FixedPoint.of(0L, 150_000L)
    public val CURIOSITY_AMPLITUDE_SLEW_PER_TICK: Long = FixedPoint.of(0L, 2_000L)

    /** Recent-inspection inhibition: depth and its recovery. */
    /**
     * Inhibition depth is deliberately of the same order as the epistemic value
     * it suppresses. An inhibition much larger than the curiosity envelope does
     * not produce rotation — it produces an organism that stops inspecting
     * anything at all, which the first anti-convergence run showed directly.
     */
    public val INSPECTION_INHIBITION_DEPTH: Long = FixedPoint.of(0L, 120_000L)
    public val INSPECTION_INHIBITION_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 998_600L)

    /** Bounded additive salience for causal environmental change (§9). */
    public val ABSOLUTE_SHIFT_MAX: Long = FixedPoint.of(0L, 220_000L)
    public val ABSOLUTE_SHIFT_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 996_000L)
    public val ABSOLUTE_SHIFT_RESERVOIR_DRAW: Long = FixedPoint.of(0L, 180_000L)
    public val ABSOLUTE_SHIFT_RESERVOIR_RECOVERY_PER_TICK: Long = FixedPoint.of(0L, 700L)

    // ------------------------------------------------------------ selection

    /** Minimum winner margin: below this the tie-break substream decides. */
    public val MINIMUM_WINNER_MARGIN: Long = FixedPoint.of(0L, 4_000L)

    public const val COMMITMENT_TICKS_DEFAULT: Int = 6
    public const val COMMITMENT_TICKS_SLEEP: Int = 240
    public const val REFRACTORY_TICKS_VOCALIZE: Int = 45
    public const val REFRACTORY_TICKS_SEEK_INTERACTION: Int = 90
    public const val OPPORTUNITY_WINDOW_TICKS: Int = 20

    /**
     * Bounded engagement. Canonical §7 lists refractory periods among the
     * stability mechanisms; this is the one that keeps a single well-liked
     * object from taking the whole day. Recent-inspection inhibition alone
     * cannot do it: under continuous engagement every object saturates, and a
     * uniformly saturated term stops discriminating between them.
     */
    public const val MAX_ENGAGEMENT_TICKS: Int = 15

    /**
     * Action satiation. Diminishing marginal utility for *doing the same kind of
     * thing*, as distinct from doing it to the same object. Without it, an
     * organism whose personality favours one activity spends half its life on
     * that activity across every object in the habitat, which the per-object
     * engagement bound cannot reach.
     */
    public val ACTION_SATIATION_PER_TICK: Long = FixedPoint.of(0L, 22_000L)
    public val ACTION_SATIATION_RETENTION_PER_TICK: Long = FixedPoint.of(0L, 997_000L)
    public const val ENGAGEMENT_REFRACTORY_TICKS: Int = 90

    // ----------------------------------------------------------- attribution

    /**
     * Frozen `SpontaneousActionAttributionV1` thresholds. These bind the
     * classification of a scored spontaneous action and are referenced by
     * `CuriosityBalanceEnvelopeV1`.
     */
    public val SUBSTANTIVE_SHARE_FLOOR: Long = FixedPoint.of(0L, 500_000L)
    public val DOMINANT_GROUP_SHARE: Long = FixedPoint.of(0L, 500_000L)
    public val MIXED_SUBSTANTIVE_MIN_SHARE: Long = FixedPoint.of(0L, 150_000L)
    public val OSCILLATOR_DOMINANCE_SHARE: Long = FixedPoint.of(0L, 500_000L)

    /** Envelope requirements, evaluated on the identical parameter/seed hashes. */
    public val REQUIRED_SUBSTANTIVE_SPONTANEITY_RATE: Long = FixedPoint.of(0L, 700_000L)
    public val MAX_OSCILLATOR_TIEBREAK_ONLY_RATE: Long = FixedPoint.of(0L, 200_000L)

    /** Anti-convergence requirements for the static-habitat behavioural-death fixture. */
    public val MIN_ACTION_TYPE_ENTROPY_BITS: Long = FixedPoint.of(1L, 600_000L)
    public val MIN_DISTINCT_OBJECTS_INSPECTED_PER_DAY: Long = FixedPoint.of(2L, 500_000L)
    public val MAX_SINGLE_ACTION_OCCUPANCY: Long = FixedPoint.of(0L, 450_000L)
    public val MIN_REVISITATION_RATE_PER_DAY: Long = FixedPoint.of(3L, 0L)
    public val MAX_CYCLE_REGULARITY: Long = FixedPoint.of(0L, 550_000L)

    // ---------------------------------------------------------- study design

    /** Canonical: family-wise error rate for the human leave-one-out family. */
    public const val ABLATION_FAMILY_ALPHA_MILLIONTHS: Long = 50_000L

    /** Canonical: maximum scored A001 attempts under one foundational hypothesis. */
    public const val MAX_SCORED_A001_ATTEMPTS: Int = 3

    /** Canonical: exact exhaustive coalition evaluation requires 2^k <= 64. */
    public const val MAX_EXACT_COALITION_GROUPS: Int = 6

    /** Live paired session duration targeted by the canonical protocol. */
    public const val PRIMARY_SESSION_SECONDS: Int = 600

    /** Viewer frame cadence, identical for every cohort. */
    public const val EXPRESSION_FRAME_MILLIS: Int = 50

    /** Real-time viewer ticks: one organism tick per this many wall milliseconds. */
    public const val VIEWER_TICK_MILLIS: Int = 200
}

/** Seeded random-domain identifiers. Every draw is recorded in the trace. */
public enum class SpikeRandomDomain(public val domainId: Int) {
    /** The only domain permitted to influence selection, and only at near-equal utility. */
    CURIOSITY_TIE_BREAK(1),

    /** Habitat-side only: never reaches organism selection. */
    HABITAT_NOVELTY(2),

    /** Scripted-cohort idle variety. Never present in FULL. */
    SCRIPTED_IDLE_VARIETY(3),
}
