package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * `SpikeDecisionTraceV1`.
 *
 * Traces are audit evidence. They are never visible to a rater and cannot
 * influence viewer behaviour — the viewer is handed `ExpressionFrame` values and
 * has no reference to this type at all.
 */
public class SpikeDecisionTrace(
    public val runId: String,
    public val organismId: String,
    public val tick: Long,
    public val cohort: Cohort,
    public val winningTier: Int,
    public val selectedAction: SpikeAction,
    public val selectedTarget: HabitatObject?,
    public val expectedOutcomeClass: ExpectedOutcome,
    public val eligible: List<TracedProposal>,
    public val rejectedLowerTier: List<TracedProposal>,
    public val driveSnapshot: LongArray,
    public val modulatorSnapshot: LongArray,
    public val traitSnapshot: LongArray,
    public val curiosityPhase: Long,
    public val inspectionInhibition: Long,
    public val commitmentRemaining: Int,
    public val refractoryActive: Boolean,
    public val opportunityPromoted: Boolean,
    public val runnerUpMargin: Long,
    public val tieBreakDetermined: Boolean,
    public val tieBreakDomain: SpikeRandomDomain?,
    public val tieBreakDraw: Long?,
    public val spontaneous: Boolean,
    public val attribution: AttributionResult?,
) {
    /** One candidate as recorded, with its per-group utility decomposition. */
    public class TracedProposal(
        public val action: SpikeAction,
        public val target: HabitatObject?,
        public val tier: Int,
        public val utility: Long,
        public val contributions: LongArray,
        public val rejectionReason: String?,
    )

    public fun render(): String = buildString {
        append("SpikeDecisionTraceV1 run=").append(runId)
        append(" organism=").append(organismId)
        append(" tick=").append(tick)
        append(" cohort=").append(cohort.name).append('\n')
        append("  selected: ").append(selectedAction.name)
        selectedTarget?.let { append('@').append(it.name) }
        append("  tier=").append(winningTier)
        append("  expected=").append(expectedOutcomeClass.name).append('\n')
        append("  drives energy=").append(fx(driveSnapshot[0]))
        append(" rest=").append(fx(driveSnapshot[1]))
        append(" safety=").append(fx(driveSnapshot[2]))
        append(" social=").append(fx(driveSnapshot[3])).append('\n')
        append("  modulators arousal=").append(fx(modulatorSnapshot[0]))
        append(" stress=").append(fx(modulatorSnapshot[1]))
        append(" rewardExpectancy=").append(fx(modulatorSnapshot[2])).append('\n')
        append("  curiosityPhase=").append(fx(curiosityPhase))
        append(" inspectionInhibition=").append(fx(inspectionInhibition))
        append(" commitment=").append(commitmentRemaining)
        append(" refractory=").append(refractoryActive)
        append(" promoted=").append(opportunityPromoted).append('\n')
        append("  margin=").append(fx(runnerUpMargin))
        append(" tieBreak=").append(tieBreakDetermined)
        tieBreakDomain?.let { append(" domain=").append(it.name) }
        tieBreakDraw?.let { append(" draw=").append(it) }
        append(" spontaneous=").append(spontaneous).append('\n')
        append("  eligible:\n")
        for (p in eligible.take(12)) {
            append("    ").append(p.action.name)
            p.target?.let { append('@').append(it.name) }
            append(" tier=").append(p.tier).append(" u=").append(fx(p.utility))
            append(" [")
            for (g in MechanismGroup.ALL) {
                append(g.name.take(4)).append('=').append(fx(p.contributions[g.groupOrdinal]))
                if (g.groupOrdinal < MechanismGroup.COUNT - 1) append(' ')
            }
            append(']')
            p.rejectionReason?.let { append(" rejected: ").append(it) }
            append('\n')
        }
        append("  discardedLowerTier=").append(rejectedLowerTier.size).append('\n')
        attribution?.let { a ->
            append("  attribution class=").append(a.attributionClass.name)
            append(" substantiveShare=").append(fx(a.substantiveShare))
            append(" oscillatorShare=").append(fx(a.oscillatorShare))
            append(" v(M)=").append(fx(a.grandCoalitionValue)).append('\n')
            append("    shapley:")
            for (g in MechanismGroup.ALL) {
                append(' ').append(g.name).append('=').append(fx(a.shapley[g.groupOrdinal]))
            }
            append('\n')
        }
    }

    private fun fx(raw: Long): String {
        val negative = raw < 0L
        val magnitude = if (negative) -raw else raw
        val whole = magnitude / FixedPoint.SCALE
        val frac = magnitude % FixedPoint.SCALE
        return (if (negative) "-" else "") + whole + "." + frac.toString().padStart(6, '0')
    }
}

/** Immediate expected outcome class recorded with every trace. */
public enum class ExpectedOutcome {
    RESOURCE_GAIN,
    REST_GAIN,
    SOCIAL_GAIN,
    THREAT_REDUCTION,
    EPISTEMIC_GAIN,
    NO_CHANGE_EXPECTED,
}

/** Which cohort produced a decision. Never exposed to a rater. */
public enum class Cohort(public val cohortId: String, public val blindedLabel: String) {
    FULL("FULL", "CREATURE"),
    SCRIPTED_PET_BASELINE("ScriptedPetBaselineV1", "CREATURE"),
    DEGRADED_SCRIPTED_CONTROL("DegradedScriptedControlV1", "CREATURE"),
    FULL_MINUS_CURIOSITY_ANTICONVERGENCE("FULL-curiosity-anticonvergence", "CREATURE"),
    FULL_MINUS_PREFERENCE_LEARNING("FULL-preference-learning", "CREATURE"),

    /**
     * FULL **plus** episodic history, not minus.
     *
     * The mechanism was removed from FULL under D009 after a revised form still
     * failed to add history-dependent individuality across a five-seed matrix.
     * The cohort is retained inverted so the negative result stays reproducible
     * and the mechanism can be re-tested if a later design gives it something to
     * contribute.
     */
    FULL_PLUS_EPISODIC_HISTORY("FULL+episodic-history", "CREATURE");

    /** The mechanism set this cohort carries. Scripted cohorts carry none. */
    public val mechanisms: Set<Mechanism>
        get() = when (this) {
            FULL -> Mechanism.QUALIFIED_SET
            SCRIPTED_PET_BASELINE, DEGRADED_SCRIPTED_CONTROL -> emptySet()
            FULL_MINUS_CURIOSITY_ANTICONVERGENCE ->
                Mechanism.QUALIFIED_SET - Mechanism.CURIOSITY_PHASE_DRIFT -
                    Mechanism.RECENT_INSPECTION_INHIBITION
            FULL_MINUS_PREFERENCE_LEARNING ->
                Mechanism.QUALIFIED_SET - Mechanism.PREFERENCE_LEARNING
            FULL_PLUS_EPISODIC_HISTORY ->
                Mechanism.QUALIFIED_SET + Mechanism.EPISODIC_HISTORY
        }

    public val scripted: Boolean
        get() = this == SCRIPTED_PET_BASELINE || this == DEGRADED_SCRIPTED_CONTROL

    public companion object {
        /**
         * The preregistered human-rated leave-one-out arms.
         *
         * Two, not three: the episodic arm was retired under D009 when the
         * mechanism left FULL. The canonical multiplicity plan corrects across
         * the comparisons *actually tested in the attempt*, so Holm-Bonferroni
         * now runs over two. Adding a replacement third arm requires a new
         * preregistered plan and is not the implementer's decision.
         */
        public val HUMAN_ABLATION_FAMILY: List<Cohort> = listOf(
            FULL_MINUS_CURIOSITY_ANTICONVERGENCE,
            FULL_MINUS_PREFERENCE_LEARNING,
        )
    }
}
