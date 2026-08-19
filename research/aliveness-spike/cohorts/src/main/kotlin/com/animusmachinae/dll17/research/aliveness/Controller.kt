package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * One candidate action with its utility decomposed by coalition group.
 *
 * The decomposition is the whole point. `CoalitionValueFunctionV1` requires the
 * controller to be recomputed from the identical pre-action state with only a
 * coalition's contributions enabled; making the contributions additive and
 * per-group means that recomputation is exact rather than approximate, and the
 * 64 coalition evaluations cost 64 sums instead of 64 full controller runs.
 */
public class Proposal(
    public val action: SpikeAction,
    public val target: HabitatObject?,
    public val tier: Int,
    public val base: Long,
    public val contributions: LongArray,
    /**
     * Utility under the grand coalition, computed once at construction.
     * Recomputing it inside a comparator turned selection into the dominant
     * cost of the whole simulator.
     */
    public val full: Long,
) {
    init {
        require(contributions.size == MechanismGroup.COUNT)
    }

    /** Utility under a coalition expressed as a bitmask over `MechanismGroup`. */
    public fun utility(mask: Int, fx: Fx): Long {
        var total = base
        for (g in 0 until MechanismGroup.COUNT) {
            if ((mask shr g) and 1 == 1) total = fx.add(total, contributions[g])
        }
        return total
    }

    public fun fullUtility(fx: Fx): Long = full

    public val key: String
        get() = if (target == null) action.name else "${action.name}@${target.name}"

    public companion object {
        public const val FULL_MASK: Int = (1 shl MechanismGroup.COUNT) - 1
    }
}

/** The outcome of one selection, including everything the trace needs. */
public class Decision(
    public val tick: Long,
    public val winner: Proposal,
    public val winningTier: Int,
    public val eligible: List<Proposal>,
    public val discardedLowerTier: List<Proposal>,
    public val runnerUpMargin: Long,
    public val tieBreakDetermined: Boolean,
    public val tieBreakDraw: Long?,
    public val commitmentContinuation: Boolean,
    public val opportunityPromoted: Boolean,
    public val spontaneous: Boolean,
)

/**
 * Hierarchical action control (canonical §7). A universal flat score is
 * prohibited: the highest active tier is determined first, every lower-tier
 * proposal is discarded, and only candidates inside the winning tier are scored.
 */
public class Controller(private val fx: Fx) {

    public fun propose(
        state: OrganismState,
        habitat: Habitat,
        tick: Long,
    ): List<Proposal> {
        val out = ArrayList<Proposal>(48)

        // Tier 5 always has a candidate so the organism is never proposal-starved.
        out += build(SpikeAction.IDLE_VARIATION, null, 5, state, habitat, tick, IDLE_BASE)

        if (state.rest < SpikeContract.LOW_REST) {
            out += build(SpikeAction.REST, null, tierForRest(state), state, habitat, tick)
            out += build(SpikeAction.SLEEP, null, tierForSleep(state), state, habitat, tick)
        }

        // Resumption is offered only briefly after an interruption. A permanently
        // available Tier 2 candidate would outrank every ordinary need forever.
        if (state.interruptedAction != null &&
            tick - state.interruptedAtTick <= RESUMPTION_WINDOW_TICKS
        ) {
            out += build(SpikeAction.RESUME_INTERRUPTED, state.interruptedTarget, 2, state, habitat, tick)
        }

        for (obj in HabitatObject.ALL) {
            if (!habitat.isPresent(obj)) continue
            val affordances = habitat.affordances(obj)
            for (action in affordances) {
                if (!eligible(action, obj, state, habitat, tick)) continue
                out += build(action, obj, tierOf(action, obj, state, habitat, tick), state, habitat, tick)
            }
        }

        state.pendingStimulus?.takeIf { it.activeAt(tick) }?.let { stimulus ->
            when (stimulus.kind) {
                InteractionKind.TOUCH -> out += build(
                    SpikeAction.RESPOND_TO_TOUCH,
                    stimulus.target ?: HabitatObject.PERSON_ALPHA,
                    2, state, habitat, tick,
                )
                InteractionKind.STARTLE -> out += build(
                    SpikeAction.WITHDRAW,
                    stimulus.target ?: HabitatObject.AVERSIVE_BUZZER,
                    0, state, habitat, tick,
                )
                InteractionKind.CALL,
                InteractionKind.OFFER_FOOD,
                InteractionKind.PRESENT_OBJECT,
                -> out += build(
                    SpikeAction.ORIENT,
                    stimulus.target,
                    2, state, habitat, tick,
                )
                InteractionKind.WITHDRAW_ATTENTION -> Unit
            }
        }

        // D016-AB world evidence is separate from owner input. The observation
        // raises bounded salience, then the organism chooses whether attention
        // is warranted from its own state.
        if (state.pendingWorldObservation != null) {
            out += build(
                SpikeAction.ORIENT,
                null,
                2,
                state,
                habitat,
                tick,
                state.worldObservationSalience,
            )
        }

        return out
    }

    /**
     * Selection. Steps 1–5 of the canonical algorithm, plus the single permitted
     * adjacent-tier opportunity promotion.
     */
    public fun select(
        proposals: List<Proposal>,
        state: OrganismState,
        habitat: Habitat,
        tick: Long,
        tieBreak: SpikeRandom,
    ): Decision {
        require(proposals.isNotEmpty()) { "controller must always have a candidate" }

        // An active intention continues unless safety, critical physiology, or
        // one newly arrived salient owner event interrupts it. The event is
        // consumed after that one selection; its bounded response commitment
        // then continues without fresh arbitration every tick.
        val committed = state.committedAction
        if (state.has(Mechanism.TIERED_COMMITMENT) &&
            committed != null &&
            state.commitmentRemaining > 0 &&
            proposals.none { it.tier <= 1 } &&
            state.pendingStimulus?.activeAt(tick) != true &&
            state.pendingWorldObservation == null
        ) {
            val continuation = proposals.firstOrNull {
                it.action == committed && it.target == state.committedTarget
            }
            if (continuation != null) {
                return Decision(
                    tick = tick,
                    winner = continuation,
                    winningTier = continuation.tier,
                    eligible = listOf(continuation),
                    discardedLowerTier = emptyList(),
                    runnerUpMargin = FixedPoint.ZERO,
                    tieBreakDetermined = false,
                    tieBreakDraw = null,
                    commitmentContinuation = true,
                    opportunityPromoted = false,
                    spontaneous = false,
                )
            }
        }

        val highestTier = proposals.minOf { it.tier }

        // Adjacent-tier opportunity promotion: Tier 4 into a bounded Tier 3
        // window. Permitted only with no Tier 0/1 proposal, comfortable
        // physiology and a safe action, and it cannot chain across tiers.
        var promoted = false
        var winningTier = highestTier
        if (highestTier == 3 &&
            proposals.none { it.tier <= 1 } &&
            physiologyComfortable(state)
        ) {
            val bestTier4 = proposals.filter { it.tier == 4 && safeTarget(it.target, habitat) }
                .maxByOrNull { it.full }
            val bestTier3 = proposals.filter { it.tier == 3 }.maxByOrNull { it.full }
            if (bestTier4 != null && bestTier3 != null &&
                bestTier4.full > fx.add(bestTier3.full, OPPORTUNITY_MARGIN) &&
                tick >= state.opportunityWindowUntilTick
            ) {
                promoted = true
                winningTier = 4
                state.opportunityWindowUntilTick =
                    tick + SpikeContract.OPPORTUNITY_WINDOW_TICKS * 3L
            }
        }

        val inTier = proposals.filter { it.tier == winningTier }
        val discarded = proposals.filter { it.tier != winningTier }

        val ranked = inTier.sortedWith(
            compareByDescending<Proposal> { it.full }
                .thenBy { it.action.ordinal }
                .thenBy { it.target?.ordinal0 ?: -1 },
        )
        val best = ranked[0]
        val runnerUp = ranked.getOrNull(1)
        val margin = if (runnerUp == null) {
            FixedPoint.ONE
        } else {
            fx.sub(best.full, runnerUp.full)
        }

        // Bounded deterministic tie-breaking. The curiosity substream is consulted
        // only after every biological and learned-value term has been evaluated,
        // and only among candidates that are genuinely near-equal.
        var winner = best
        var tieBroken = false
        var draw: Long? = null
        if (margin < SpikeContract.MINIMUM_WINNER_MARGIN) {
            val threshold = fx.sub(best.full, SpikeContract.MINIMUM_WINNER_MARGIN)
            val nearEqual = ranked.filter { it.full >= threshold }
            if (nearEqual.size > 1) {
                val d = tieBreak.nextLong()
                draw = d
                winner = nearEqual[Math.floorMod(d, nearEqual.size.toLong()).toInt()]
                tieBroken = winner !== best || nearEqual.size > 1
            }
        }

        val spontaneous = winningTier >= 4 &&
            tick - state.lastInteractionTick > SPONTANEITY_QUIET_TICKS &&
            !promoted

        return Decision(
            tick = tick,
            winner = winner,
            winningTier = winningTier,
            eligible = ranked,
            discardedLowerTier = discarded,
            runnerUpMargin = margin,
            tieBreakDetermined = tieBroken,
            tieBreakDraw = draw,
            commitmentContinuation = false,
            opportunityPromoted = promoted,
            spontaneous = spontaneous,
        )
    }

    // ------------------------------------------------------------- utility

    private fun build(
        action: SpikeAction,
        target: HabitatObject?,
        tier: Int,
        state: OrganismState,
        habitat: Habitat,
        tick: Long,
        base: Long = FixedPoint.ZERO,
    ): Proposal {
        val c = LongArray(MechanismGroup.COUNT)
        c[MechanismGroup.PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE.groupOrdinal] =
            physiology(action, target, state, habitat, tick)
        c[MechanismGroup.LEARNED_PREFERENCE.groupOrdinal] =
            learnedValence(action, target, state)
        c[MechanismGroup.EPISODIC_OR_HISTORY.groupOrdinal] =
            episodic(action, target, state, habitat, tick)
        c[MechanismGroup.HABIT_OR_EXPECTANCY.groupOrdinal] =
            habitExpectancy(action, target, state)
        c[MechanismGroup.SOCIAL_OR_RELATIONSHIP_HISTORY.groupOrdinal] =
            relationship(action, target, state)
        c[MechanismGroup.CURIOSITY_OSCILLATOR.groupOrdinal] =
            curiosity(action, target, state, tick)
        var full = base
        for (g in 0 until MechanismGroup.COUNT) full = fx.add(full, c[g])
        return Proposal(action, target, tier, base, c, full)
    }

    private fun physiology(
        action: SpikeAction,
        target: HabitatObject?,
        s: OrganismState,
        habitat: Habitat,
        tick: Long,
    ): Long {
        val deficitEnergy = fx.sub(FixedPoint.ONE, s.energy)
        val deficitRest = fx.sub(FixedPoint.ONE, s.rest)
        val deficitSocial = fx.sub(FixedPoint.ONE, s.social)
        val deficitSafety = fx.sub(FixedPoint.ONE, s.safety)

        var v = when (action) {
            SpikeAction.EAT -> fx.mul(deficitEnergy, W_EAT)
            SpikeAction.SLEEP -> {
                val circadian = if (habitat.isNight(tick)) W_NIGHT_BONUS else W_DAY_PENALTY
                fx.mul(fx.mul(deficitRest, W_SLEEP), circadian)
            }
            SpikeAction.REST -> fx.mul(deficitRest, W_REST)
            SpikeAction.WITHDRAW -> fx.mul(fx.mul(deficitSafety, W_WITHDRAW), s.traits.caution)
            SpikeAction.SEEK_INTERACTION, SpikeAction.VOCALIZE ->
                fx.mul(fx.mul(deficitSocial, W_SOCIAL), s.traits.sociability)
            SpikeAction.RESPOND_TO_TOUCH -> fx.mul(W_TOUCH, s.traits.sociability)
            // The epistemic actions are deliberately *not* interchangeable. Giving
            // them one shared value made them exactly equal on every object, so
            // the tie-break substream chose between them and the trace could not
            // say why the organism looked rather than played.
            SpikeAction.APPROACH -> {
                val social = if (target?.kind == ObjectKind.SOCIAL) {
                    fx.mul(deficitSocial, W_APPROACH)
                } else {
                    FixedPoint.ZERO
                }
                fx.add(
                    social,
                    epistemicDrive(s, W_EPISTEMIC_APPROACH, inverse(s.traits.caution)),
                )
            }
            // Each epistemic action is modulated by a different part of the
            // organism's state, so which one wins changes over a day instead of
            // being a fixed ranking. A fixed ranking made one action the
            // successor of everything, which is what the cycle-regularity
            // measure was reporting.
            SpikeAction.OBSERVE ->
                // Cheap. Preferred when the organism is depleted or stressed.
                // Watching is what a cautious, curious organism does.
                fx.mul(
                    epistemicDrive(
                        s, W_EPISTEMIC_OBSERVE, blend(s.traits.curiosity, s.traits.caution),
                    ),
                    fx.sub(FixedPoint.of(1L, 400_000L), fx.mul(s.energy, W_OBSERVE_ENERGY_TAPER)),
                )
            SpikeAction.ORIENT ->
                // Attentional. Rises with arousal.
                fx.mul(
                    epistemicDrive(s, W_EPISTEMIC_ORIENT, s.traits.curiosity),
                    fx.add(FixedPoint.of(0L, 600_000L), s.arousal),
                )
            SpikeAction.EXPLORE ->
                // Costly. Requires energy to be worth it.
                // Going and finding out is what a bold, curious organism does.
                fx.mul(
                    epistemicDrive(
                        s, W_EPISTEMIC_EXPLORE, blend(s.traits.curiosity, inverse(s.traits.caution)),
                    ),
                    s.energy,
                )
            SpikeAction.PLAY ->
                // Requires rest and appetite, and carries a social component.
                fx.mul(
                    fx.add(
                        epistemicDrive(
                            s, W_EPISTEMIC_PLAY, blend(s.traits.curiosity, s.traits.sociability),
                        ),
                        fx.mul(s.traits.sociability, W_PLAY_SOCIAL),
                    ),
                    fx.mul(s.rest, fx.add(FixedPoint.of(0L, 550_000L), s.arousal)),
                )
            SpikeAction.RETRY -> fx.mul(s.traits.persistence, W_RETRY)
            SpikeAction.RESUME_INTERRUPTED -> fx.mul(s.traits.persistence, W_RESUME)
            SpikeAction.IDLE_VARIATION -> fx.mul(fx.sub(FixedPoint.ONE, s.arousal), W_IDLE)
        }

        // Doing the same kind of thing over and over is worth less each time.
        v = fx.sub(v, fx.mul(s.actionSatiation[action.ordinal], W_SATIATION))

        // Cost and risk, per the canonical bounded utility model.
        //
        // Risk here is *felt* risk — low safety after something bad happened —
        // and deliberately not "this object is labelled UNSAFE in the habitat".
        // An organism that innately avoids the aversive object can never acquire
        // a conditioned fear, which would make the avoidance/extinction arm
        // untestable. Danger avoidance is therefore learned, through the
        // conditioned-fear mechanism, and shows up in the learned-valence group
        // where the attribution analysis can see it.
        if (target != null) {
            if (s.safety < SpikeContract.CRITICAL_SAFETY && action != SpikeAction.WITHDRAW) {
                v = fx.sub(v, fx.mul(s.traits.caution, W_RISK))
            }
            if (action == SpikeAction.APPROACH || action == SpikeAction.EXPLORE) {
                v = fx.sub(v, fx.mul(fx.sub(FixedPoint.ONE, s.energy), W_MOVEMENT_COST))
            }
        }
        return fx.signed(v)
    }

    /**
     * Trait-blended epistemic drive.
     *
     * Scaling every epistemic action by curiosity alone shifted how *much* an
     * organism investigated but never *how*, so two individuals with different
     * personalities still spent their days on the same mix of action types and
     * the closest pair of organisms was nearly indistinguishable. Each action
     * now draws on a different blend, so a cautious watcher and a bold explorer
     * have visibly different budgets.
     */
    private fun epistemicDrive(s: OrganismState, weight: Long, disposition: Long): Long =
        fx.mul(fx.mul(disposition, weight), fx.sub(FixedPoint.ONE, s.stress))

    private fun blend(a: Long, b: Long): Long = (a + b) / 2L

    private fun inverse(v: Long): Long = fx.sub(FixedPoint.ONE, v)

    private fun learnedValence(
        action: SpikeAction,
        target: HabitatObject?,
        s: OrganismState,
    ): Long {
        if (target == null) return FixedPoint.ZERO
        val i = target.ordinal0
        var v = FixedPoint.ZERO

        if (s.has(Mechanism.PREFERENCE_LEARNING)) {
            v = fx.add(v, fx.mul(s.preference[i], W_PREFERENCE))
        }
        if (s.has(Mechanism.CONDITIONED_FEAR)) {
            val sign = if (action == SpikeAction.WITHDRAW) FixedPoint.ONE else -FixedPoint.ONE
            v = fx.add(v, fx.mul(fx.mul(s.fear[i], W_FEAR), sign))
        }
        if (epistemic(action)) {
            if (s.has(Mechanism.HABITUATION)) {
                v = fx.sub(v, fx.mul(s.habituation[i], W_HABITUATION))
            }
            if (s.has(Mechanism.SENSITIZATION)) {
                v = fx.add(v, fx.mul(s.sensitization[i], W_SENSITIZATION))
            }
        }
        return fx.signed(v)
    }

    /**
     * Context-conditioned episodic recall, revised under D009.
     *
     * The D008 form averaged recent outcomes for the target regardless of
     * context, which is exactly what `preference` already computes. Two copies
     * of one estimate cannot differentiate anything, and the measurement agreed:
     * removing the mechanism *increased* history divergence.
     *
     * This form recalls only episodes whose context matches the present one and
     * contributes the **residual** over the context-free preference. It can
     * therefore only carry what preference does not: "this goes well here, in
     * the mornings, even though I am lukewarm about it in general."
     */
    private fun episodic(
        action: SpikeAction,
        target: HabitatObject?,
        s: OrganismState,
        habitat: Habitat,
        tick: Long,
    ): Long {
        if (!s.has(Mechanism.EPISODIC_HISTORY) || target == null) return FixedPoint.ZERO
        val context = MechanismUpdates.contextBucket(habitat, tick)
        var sum = 0L
        var count = 0
        for (e in s.episodes) {
            if (e == null) continue
            if (e.target != target) continue
            if (e.context != context) continue
            if (tick - e.tick > SpikeContract.EPISODIC_RECENCY_WINDOW_TICKS) continue
            val weight = if (e.action == action) FixedPoint.ONE else FixedPoint.of(0L, 350_000L)
            sum = fx.add(sum, fx.mul(e.valence, weight))
            count += 1
        }
        if (count == 0) return FixedPoint.ZERO
        val mean = fx.div(sum, FixedPoint.fromInt(count))
        val baseline = if (s.has(Mechanism.PREFERENCE_LEARNING)) {
            s.preference[target.ordinal0]
        } else {
            FixedPoint.ZERO
        }
        val residual = fx.sub(mean, baseline)
        return fx.signed(fx.mul(residual, SpikeContract.EPISODIC_WEIGHT))
    }

    /**
     * Habit, competence and information value. All three are statements about
     * what *doing this here* is worth, which is why they share a coalition
     * group: separating them would push the frozen group set past the
     * exact-enumeration ceiling for no analytic gain.
     */
    private fun habitExpectancy(
        action: SpikeAction,
        target: HabitatObject?,
        s: OrganismState,
    ): Long {
        if (target == null) return FixedPoint.ZERO
        val k = s.index(action, target)
        var v = FixedPoint.ZERO

        if (s.has(Mechanism.HABIT_EXPECTANCY)) {
            val h = s.habit[k]
            v = fx.add(v, fx.mul(h, W_HABIT))
            v = fx.add(v, fx.mul(h, fx.mul(s.rewardExpectancy, W_EXPECTANCY)))
        }
        if (s.has(Mechanism.SKILL_PROFICIENCY)) {
            // Competence makes an organism better at what it has practised, and
            // therefore more inclined to do it. Two organisms that practised
            // different things end up spending their days differently, which is
            // the only individuality a homeostatic drive model leaves room for.
            v = fx.add(v, fx.mul(s.skill[k], W_SKILL))
        }
        if (s.has(Mechanism.OUTCOME_UNCERTAINTY)) {
            // Directed exploration. A stale or contradicted estimate is worth
            // refreshing; a stressed organism has less appetite for it, and a
            // disappointed one has more. The second half is what turns a
            // devalued option into an actual switch rather than a slow drift:
            // when what used to work stops working, alternatives get checked.
            val frustration = fx.sub(FixedPoint.ONE, s.rewardExpectancy)
            val appetite = fx.mul(
                fx.sub(FixedPoint.ONE, s.stress),
                fx.add(FixedPoint.of(0L, 550_000L), frustration),
            )
            // Curiosity about something frightening is not worth acting on.
            val safeToInvestigate = if (s.has(Mechanism.CONDITIONED_FEAR)) {
                fx.sub(FixedPoint.ONE, s.fear[target.ordinal0])
            } else {
                FixedPoint.ONE
            }
            v = fx.add(
                v,
                fx.mul(fx.mul(fx.mul(s.uncertainty[k], W_INFORMATION), appetite), safeToInvestigate),
            )
        }
        return fx.signed(v)
    }

    private fun relationship(
        action: SpikeAction,
        target: HabitatObject?,
        s: OrganismState,
    ): Long {
        if (!s.has(Mechanism.RELATIONSHIP_DIFFERENTIATION)) return FixedPoint.ZERO
        if (target == null || target.kind != ObjectKind.SOCIAL) return FixedPoint.ZERO
        val r = s.relationship[target.ordinal0]
        // Relationship steers *which person* an interaction is aimed at. Weighting
        // plain approach as heavily as interaction turned a liked person into a
        // permanent attractor that starved every other object of attention.
        val weight = when (action) {
            SpikeAction.SEEK_INTERACTION, SpikeAction.RESPOND_TO_TOUCH -> W_RELATIONSHIP_STRONG
            SpikeAction.VOCALIZE, SpikeAction.PLAY -> W_RELATIONSHIP
            SpikeAction.WITHDRAW -> -W_RELATIONSHIP
            else -> W_RELATIONSHIP_WEAK
        }
        return fx.signed(fx.mul(r, weight))
    }

    /**
     * Curiosity: object-specific fixed-point phase drift plus recent-inspection
     * inhibition. Context modulates the output amplitude and may add a bounded
     * absolute shift; it never enters the phase accumulator or its time input.
     */
    private fun curiosity(
        action: SpikeAction,
        target: HabitatObject?,
        s: OrganismState,
        tick: Long,
    ): Long {
        if (target == null || !epistemic(action)) return FixedPoint.ZERO
        val i = target.ordinal0
        var v = FixedPoint.ZERO

        if (s.has(Mechanism.CURIOSITY_PHASE_DRIFT)) {
            val period = CuriosityWave.periodFor(s.seed, i, s.curiosity.periods)
            val phase = CuriosityWave.phase(s.seed, i, tick, period)
            val wave = CuriosityWave.triangle(phase)
            v = fx.add(s.curiosity.baseFloor, fx.mul(s.contextAmplitude[i], wave))
            v = fx.add(v, s.absoluteShift[i])
            v = fx.mul(v, s.traits.curiosity)
        }
        if (s.has(Mechanism.RECENT_INSPECTION_INHIBITION)) {
            v = fx.sub(v, s.inhibition[i])
        }
        return fx.signed(v)
    }

    // --------------------------------------------------------- eligibility

    private fun eligible(
        action: SpikeAction,
        target: HabitatObject,
        s: OrganismState,
        habitat: Habitat,
        tick: Long,
    ): Boolean {
        if (tick < s.refractoryUntilTick[action.ordinal]) return false
        if (Controller.epistemic(action) &&
            tick < s.engagementRefractoryUntil[target.ordinal0]
        ) {
            return false
        }
        if (tick < s.suppressedUntilTick[s.index(action, target)]) return false
        if (habitat.safetyOf(target) == Safety.BLOCKED && action != SpikeAction.OBSERVE &&
            action != SpikeAction.RETRY
        ) {
            return false
        }
        // Conditioned avoidance: an object above the fear threshold stops being
        // approachable, which is what makes avoidance observable rather than a
        // hidden number.
        // Avoidance covers attending, not only approaching. Leaving observation
        // eligible let directed exploration walk the organism back into a known
        // harm dozens of times a day: uncertainty about the feared object kept
        // growing precisely because it was being avoided, which made it the most
        // information-rich thing in the habitat.
        if (s.has(Mechanism.CONDITIONED_FEAR) &&
            s.fear[target.ordinal0] >= s.traits.avoidanceThreshold &&
            Controller.epistemic(action)
        ) {
            return false
        }
        // Withdrawal needs something to withdraw from. Otherwise it is a
        // permanently available zero-value candidate that wins by default
        // whenever every genuine option is momentarily unattractive.
        if (action == SpikeAction.WITHDRAW && !withdrawalWarranted(target, s, tick)) return false
        if (action == SpikeAction.EAT && s.energy > SpikeContract.LOW_ENERGY) return false
        // RETRY is a strategy for a goal that is currently failing, not a
        // standalone activity. Without this it is permanently eligible at Tier 3
        // and Tier 4 never becomes the winning tier at all.
        if (action == SpikeAction.RETRY) {
            val goalActive = s.energy < SpikeContract.LOW_ENERGY
            val recentFailure = s.failureCount[s.index(SpikeAction.EAT, target)] > 0 ||
                s.failureCount[s.index(SpikeAction.RETRY, target)] > 0
            if (!goalActive || !recentFailure) return false
        }
        return true
    }

    private fun tierOf(
        action: SpikeAction,
        target: HabitatObject,
        s: OrganismState,
        habitat: Habitat,
        tick: Long,
    ): Int {
        // Tier 0 is reserved for actual danger: safety already damaged, or an
        // object the organism has learned to fear. A habitat label alone does
        // not promote withdrawal, for the reason given in `physiology`.
        if (action == SpikeAction.WITHDRAW && withdrawalWarranted(target, s, tick)) return 0
        if (action == SpikeAction.EAT && s.energy < SpikeContract.CRITICAL_ENERGY) return 1
        if (action == SpikeAction.RETRY && s.energy < SpikeContract.CRITICAL_ENERGY) return 1
        if (action == SpikeAction.RESPOND_TO_TOUCH) return 2
        if (action == SpikeAction.EAT) return 3
        if (action == SpikeAction.SEEK_INTERACTION || action == SpikeAction.VOCALIZE) {
            return if (s.social < s.traits.socialNeedThreshold) 3 else 4
        }
        if (action == SpikeAction.APPROACH) {
            return if (s.social < s.traits.socialNeedThreshold &&
                target.kind == ObjectKind.SOCIAL
            ) 3 else 4
        }
        return action.tier
    }

    /**
     * Withdrawal is a *reaction*, not a standing posture. Making a learned fear
     * permanently warrant Tier 0 withdrawal produces an organism that withdraws
     * forever from an object it is no longer near; the durable expression of
     * that fear is the ineligibility of approach, not perpetual retreat.
     */
    /**
     * Withdrawal is a bounded reaction to a live threat, on every route into it.
     *
     * Damaged safety is not on its own grounds for retreat from everything
     * present, and it is not grounds for retreat that outlasts the event: a
     * single punishment used to license Tier 0 withdrawal for as long as safety
     * took to recover, which was most of a day. The durable expression of a
     * learned fear is that approach stays ineligible, not that the organism
     * spends its life backing away.
     */
    private fun withdrawalWarranted(target: HabitatObject, s: OrganismState, tick: Long): Boolean {
        val window = SpikeContract.WITHDRAW_REACTION_TICKS
        if (s.lastThreatTarget == target && tick - s.lastThreatTick <= window) return true
        if (!s.has(Mechanism.CONDITIONED_FEAR)) return false
        if (s.fear[target.ordinal0] < s.traits.avoidanceThreshold) return false
        return tick - s.lastInspectedTick[target.ordinal0] <= window
    }

    /**
     * Rest is a Tier 3 concern only when rest is actually low. Promoting it for
     * the whole night regardless of need was the single largest cause of the
     * D008 anti-convergence failure: Tier 3 outranks Tier 4, so every organism
     * spent every night asleep, one action took nearly half the tick budget,
     * and the successor of any action was almost always the same one.
     * A nocturnal preference belongs in the utility term, where it competes.
     */
    private fun tierForRest(s: OrganismState): Int = when {
        s.rest < SpikeContract.CRITICAL_REST -> 1
        s.rest < SpikeContract.LOW_REST -> 3
        else -> 4
    }

    private fun tierForSleep(s: OrganismState): Int = tierForRest(s)

    private fun physiologyComfortable(s: OrganismState): Boolean =
        s.energy > SpikeContract.LOW_ENERGY &&
            s.rest > SpikeContract.LOW_REST &&
            s.safety > SpikeContract.CRITICAL_SAFETY

    private fun safeTarget(target: HabitatObject?, habitat: Habitat): Boolean =
        target == null || habitat.safetyOf(target) == Safety.SAFE

    public companion object {
        public fun epistemic(action: SpikeAction): Boolean = when (action) {
            SpikeAction.OBSERVE, SpikeAction.ORIENT, SpikeAction.EXPLORE,
            SpikeAction.PLAY, SpikeAction.APPROACH,
            -> true
            else -> false
        }

        private const val SPONTANEITY_QUIET_TICKS = 3L
        private const val RESUMPTION_WINDOW_TICKS = 8L

        private val IDLE_BASE = FixedPoint.of(0L, 12_000L)
        private val OPPORTUNITY_MARGIN = FixedPoint.of(0L, 90_000L)

        private val W_EAT = FixedPoint.of(0L, 900_000L)
        private val W_SLEEP = FixedPoint.of(0L, 880_000L)
        private val W_REST = FixedPoint.of(0L, 520_000L)
        private val W_NIGHT_BONUS = FixedPoint.of(1L, 150_000L)
        private val W_DAY_PENALTY = FixedPoint.of(0L, 500_000L)
        private val W_WITHDRAW = FixedPoint.of(0L, 950_000L)
        private val W_SOCIAL = FixedPoint.of(0L, 620_000L)
        private val W_TOUCH = FixedPoint.of(0L, 780_000L)
        private val W_APPROACH = FixedPoint.of(0L, 180_000L)
        private val W_EPISTEMIC_OBSERVE = FixedPoint.of(0L, 200_000L)
        private val W_EPISTEMIC_ORIENT = FixedPoint.of(0L, 150_000L)
        private val W_EPISTEMIC_EXPLORE = FixedPoint.of(0L, 300_000L)
        private val W_EPISTEMIC_PLAY = FixedPoint.of(0L, 220_000L)
        private val W_EPISTEMIC_APPROACH = FixedPoint.of(0L, 120_000L)
        private val W_PLAY_SOCIAL = FixedPoint.of(0L, 150_000L)
        private val W_OBSERVE_ENERGY_TAPER = FixedPoint.of(0L, 600_000L)
        private val W_RETRY = FixedPoint.of(0L, 140_000L)
        private val W_RESUME = FixedPoint.of(0L, 300_000L)
        private val W_IDLE = FixedPoint.of(0L, 60_000L)
        private val W_SATIATION = FixedPoint.of(0L, 380_000L)
        private val W_RISK = FixedPoint.of(0L, 600_000L)
        private val W_MOVEMENT_COST = FixedPoint.of(0L, 45_000L)

        private val W_PREFERENCE = FixedPoint.of(0L, 550_000L)
        private val W_FEAR = FixedPoint.of(0L, 850_000L)
        private val W_HABITUATION = FixedPoint.of(0L, 400_000L)
        private val W_SENSITIZATION = FixedPoint.of(0L, 300_000L)
        private val W_HABIT = FixedPoint.of(0L, 450_000L)
        private val W_EXPECTANCY = FixedPoint.of(0L, 200_000L)
        private val W_SKILL = FixedPoint.of(0L, 330_000L)
        private val W_INFORMATION = FixedPoint.of(0L, 400_000L)
        private val W_RELATIONSHIP = FixedPoint.of(0L, 400_000L)
        private val W_RELATIONSHIP_STRONG = FixedPoint.of(0L, 620_000L)
        private val W_RELATIONSHIP_WEAK = FixedPoint.of(0L, 80_000L)
    }
}
