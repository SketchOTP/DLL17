package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/** What the habitat did back. Valence drives every learning update. */
public data class Outcome(
    public val success: Boolean,
    public val valence: Long,
    public val strongNegative: Boolean,
    public val safeEvidence: Boolean,
    public val personResponded: HabitatObject?,
)

/**
 * Deterministic outcome schedule.
 *
 * Nothing here draws from a random stream. Reliability, attentiveness and the
 * safety of the aversive object are all functions of logical time, so a "history
 * effect" observed later is a real consequence of the organism's own choices
 * rather than of a different noise sequence.
 */
public class OutcomeModel(
    /** After this tick the aversive object stops punishing: safe evidence for extinction. */
    public val aversiveSafeFromTick: Long = Long.MAX_VALUE,
    /** After this tick food reliability is reversed: habit weakening and preference reversal. */
    public val contingencyReversalTick: Long = Long.MAX_VALUE,
    /** After this tick the neglectful person becomes attentive. */
    public val betaBecomesAttentiveTick: Long = Long.MAX_VALUE,
    /**
     * When true the currently unreliable food source never succeeds at all,
     * so which source an individual adopts is a property of the protocol
     * rather than of that individual's early tie-breaks.
     */
    public val strictFoodContingency: Boolean = false,
    /**
     * Swaps which half of the day each person keeps. Two conditioning histories
     * that differ only in *when* someone responds produce identical context-free
     * preferences and different context-conditioned memories, which is the only
     * way to measure what episodic recall contributes over preference alone.
     */
    public val socialHoursShifted: Boolean = false,
) {

    public fun resolve(
        action: SpikeAction,
        target: HabitatObject?,
        tick: Long,
        state: OrganismState,
    ): Outcome {
        if (target == null) {
            return Outcome(true, NEUTRAL, false, false, null)
        }
        return when (target.kind) {
            ObjectKind.RESOURCE -> resolveFood(action, target, tick)
            ObjectKind.SOCIAL -> resolveSocial(action, target, tick, state)
            ObjectKind.AVERSIVE -> resolveAversive(action, tick)
            ObjectKind.BLOCKED -> Outcome(false, MILD_NEGATIVE, false, false, null)
            ObjectKind.PLAY, ObjectKind.NOVELTY -> resolvePlay(action, target, tick)
            ObjectKind.SHELTER -> Outcome(true, MILD_POSITIVE, false, true, null)
        }
    }

    private fun resolveFood(action: SpikeAction, target: HabitatObject, tick: Long): Outcome {
        if (action != SpikeAction.EAT && action != SpikeAction.RETRY) {
            return Outcome(true, NEUTRAL, false, false, null)
        }
        val reversed = tick >= contingencyReversalTick
        val reliable = when (target) {
            HabitatObject.FOOD_TROUGH -> !reversed
            HabitatObject.FOOD_CACHE -> reversed
            else -> false
        }
        // Even a reliable source fails occasionally, on a fixed schedule, so
        // "reliable" is a learnable statistic rather than a constant.
        val success = when {
            reliable -> tick % 7L != 0L
            strictFoodContingency -> false
            else -> tick % 3L == 0L
        }
        return Outcome(
            success = success,
            valence = if (success) STRONG_POSITIVE else MILD_NEGATIVE,
            strongNegative = false,
            safeEvidence = true,
            personResponded = null,
        )
    }

    private fun resolveSocial(
        action: SpikeAction,
        target: HabitatObject,
        tick: Long,
        state: OrganismState,
    ): Outcome {
        // People keep hours. Alpha responds mostly in the first half of the day
        // and beta mostly in the second, which gives context-conditioned memory
        // something real to condition on. Without any circadian structure in the
        // habitat, an episodic mechanism has no conjunction to learn and can
        // only duplicate the context-free preference — which is exactly what the
        // D008 measurement showed it doing.
        val quarter = ((tick % SpikeContract.TICKS_PER_VIRTUAL_DAY) *
            4L / SpikeContract.TICKS_PER_VIRTUAL_DAY).toInt()
        val alphaHours = if (socialHoursShifted) {
            quarter == 0 || quarter == 3
        } else {
            quarter == 1 || quarter == 2
        }
        val betaHours = !alphaHours
        val attentive = when (target) {
            HabitatObject.PERSON_ALPHA -> alphaHours && (tick / 37L) % 5L != 4L
            HabitatObject.PERSON_BETA ->
                if (tick >= betaBecomesAttentiveTick) {
                    betaHours && (tick / 37L) % 5L != 4L
                } else {
                    betaHours && (tick / 37L) % 5L == 0L
                }
            else -> false
        }
        return when (action) {
            SpikeAction.SEEK_INTERACTION, SpikeAction.VOCALIZE, SpikeAction.PLAY ->
                if (attentive) {
                    Outcome(true, STRONG_POSITIVE, false, true, target)
                } else {
                    Outcome(false, MILD_NEGATIVE, false, false, null)
                }
            SpikeAction.RESPOND_TO_TOUCH -> Outcome(true, STRONG_POSITIVE, false, true, target)
            SpikeAction.WITHDRAW -> Outcome(true, NEUTRAL, false, false, null)
            // Passive observation of a person carries no relationship consequence.
            // Only an attempted interaction can succeed or be ignored, which is
            // what makes an attentive and an inattentive person distinguishable.
            else -> Outcome(true, NEUTRAL, false, attentive, null)
        }
    }

    private fun resolveAversive(action: SpikeAction, tick: Long): Outcome {
        val nowSafe = tick >= aversiveSafeFromTick
        return when {
            // Retreat is relief, not reward. Making withdrawal feel good gave the
            // aversive object a *positive* learned preference through the very
            // avoidance it caused, which then drew attention back to it.
            action == SpikeAction.WITHDRAW -> Outcome(true, NEUTRAL, false, false, null)
            // The aversive object punishes *attention*, not only contact. That is
            // what makes conditioned avoidance reachable at all: an organism with
            // no innate knowledge of danger has to look at something before it
            // can learn to avoid it.
            nowSafe -> Outcome(true, NEUTRAL, false, true, null)
            else -> Outcome(false, STRONG_NEGATIVE, true, false, null)
        }
    }

    private fun resolvePlay(action: SpikeAction, target: HabitatObject, tick: Long): Outcome {
        if (action == SpikeAction.WITHDRAW) return Outcome(true, NEUTRAL, false, false, null)
        // Play objects differ in intrinsic payoff so a preference can form at all.
        val payoff = when (target) {
            HabitatObject.PLAY_BALL -> STRONG_POSITIVE
            HabitatObject.PLAY_CUBE -> MILD_POSITIVE
            // The chime only rings in the small hours, so an organism that
            // learned "the chime is worth visiting at night" knows something its
            // context-free preference cannot express.
            HabitatObject.PLAY_CHIME ->
                if ((tick % SpikeContract.TICKS_PER_VIRTUAL_DAY) * 4L /
                    SpikeContract.TICKS_PER_VIRTUAL_DAY == 3L
                ) {
                    STRONG_POSITIVE
                } else {
                    NEUTRAL
                }
            HabitatObject.PLAY_MIRROR -> NEUTRAL
            else -> MILD_POSITIVE
        }
        val engaged = action == SpikeAction.PLAY || action == SpikeAction.EXPLORE
        return Outcome(true, if (engaged) payoff else NEUTRAL, false, true, null)
    }

    public companion object {
        public val STRONG_POSITIVE: Long = FixedPoint.of(0L, 800_000L)
        public val MILD_POSITIVE: Long = FixedPoint.of(0L, 300_000L)
        public val NEUTRAL: Long = FixedPoint.ZERO
        public val MILD_NEGATIVE: Long = -FixedPoint.of(0L, 300_000L)
        public val STRONG_NEGATIVE: Long = -FixedPoint.of(0L, 850_000L)
    }
}
