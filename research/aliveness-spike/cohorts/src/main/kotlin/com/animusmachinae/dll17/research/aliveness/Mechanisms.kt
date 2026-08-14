package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * The A000 candidate update laws.
 *
 * Every value written here is clamped to a bound at the point of write, so
 * "bounded state" is a structural property rather than something the long-run
 * measures have to hope for. The measures then confirm it independently.
 */
public object MechanismUpdates {

    /** Per-tick passive dynamics, applied before selection. */
    public fun advance(s: OrganismState, habitat: Habitat, tick: Long, fx: Fx) {
        // Drives.
        s.energy = fx.unit(fx.sub(s.energy, SpikeContract.ENERGY_DECAY_PER_TICK))
        s.rest = fx.unit(fx.sub(s.rest, SpikeContract.REST_DECAY_PER_TICK))
        s.social = fx.unit(fx.sub(s.social, SpikeContract.SOCIAL_DECAY_PER_TICK))
        s.safety = fx.unit(fx.add(s.safety, SpikeContract.SAFETY_RECOVERY_PER_TICK))

        // Modulators.
        s.arousal = fx.unit(fx.decay(s.arousal, SpikeContract.AROUSAL_RETENTION_PER_TICK))
        s.stress = fx.unit(fx.decay(s.stress, SpikeContract.STRESS_RETENTION_PER_TICK))
        s.rewardExpectancy = fx.unit(
            fx.decay(s.rewardExpectancy, SpikeContract.REWARD_EXPECTANCY_RETENTION_PER_TICK),
        )

        for (o in HabitatObject.ALL) {
            val i = o.ordinal0

            // Habituation recovery toward baseline, bounded by the declining ceiling.
            if (s.has(Mechanism.DISHABITUATION_RECOVERY)) {
                s.habituation[i] = fx.unit(
                    fx.decay(s.habituation[i], SpikeContract.HABITUATION_RECOVERY_RETENTION_PER_TICK),
                )
            }
            if (s.has(Mechanism.SENSITIZATION)) {
                s.sensitization[i] = fx.unit(
                    fx.decay(s.sensitization[i], SpikeContract.SENSITIZATION_RETENTION_PER_TICK),
                )
            }
            if (s.has(Mechanism.PREFERENCE_LEARNING)) {
                s.preference[i] =
                    fx.decay(s.preference[i], SpikeContract.PREFERENCE_DECAY_RETENTION_PER_TICK)
            }
            // Slow forgetting in the absence of reinforcement. This is the only
            // route below the extinction residual, and it takes days — which is
            // what makes an acquired avoidance persist long enough to observe.
            if (s.has(Mechanism.CONDITIONED_FEAR) && s.fear[i] > FixedPoint.ZERO) {
                s.fear[i] = fx.unit(
                    fx.decay(s.fear[i], SpikeContract.FEAR_FORGETTING_RETENTION_PER_TICK),
                )
            }
            if (s.has(Mechanism.RELATIONSHIP_DIFFERENTIATION)) {
                s.relationship[i] =
                    fx.decay(s.relationship[i], SpikeContract.RELATIONSHIP_RETENTION_PER_TICK)
            }
            if (s.has(Mechanism.RECENT_INSPECTION_INHIBITION)) {
                s.inhibition[i] = fx.unit(
                    fx.decay(s.inhibition[i], s.curiosity.inhibitionRetentionPerTick),
                )
            }
            // Engagement budget recovers while the organism is doing other things.
            if (s.lastInspectedTick[i] != tick - 1L && s.engagementTicks[i] > 0) {
                s.engagementTicks[i] -= 1
            }

            // Bounded absolute salience shift and its per-object reservoir.
            s.absoluteShift[i] = fx.unit(
                fx.decay(s.absoluteShift[i], SpikeContract.ABSOLUTE_SHIFT_RETENTION_PER_TICK),
            )
            s.absoluteShiftReservoir[i] = fx.unit(
                fx.add(
                    s.absoluteShiftReservoir[i],
                    SpikeContract.ABSOLUTE_SHIFT_RESERVOIR_RECOVERY_PER_TICK,
                ),
            )

            // Context modulates the oscillator's output amplitude only, through a
            // slew-limited filter. It never touches phase, period or frequency.
            val target = if (habitat.isNight(tick)) {
                fx.mul(s.curiosity.contextAmplitude, FixedPoint.of(0L, 350_000L))
            } else {
                s.curiosity.contextAmplitude
            }
            s.contextAmplitude[i] = fx.approach(
                s.contextAmplitude[i],
                target,
                s.curiosity.amplitudeSlewPerTick,
            )

            // A causal environmental change draws a bounded absolute shift from
            // its own reservoir; repeated changes deplete it and become refractory.
            if (habitat.recentCausalChange(o, tick) && s.absoluteShiftReservoir[i] > FixedPoint.ZERO) {
                val draw = minOf(SpikeContract.ABSOLUTE_SHIFT_RESERVOIR_DRAW, s.absoluteShiftReservoir[i])
                s.absoluteShiftReservoir[i] = fx.unit(fx.sub(s.absoluteShiftReservoir[i], draw))
                s.absoluteShift[i] = fx.clamp(
                    fx.add(s.absoluteShift[i], draw),
                    FixedPoint.ZERO,
                    SpikeContract.ABSOLUTE_SHIFT_MAX,
                )
                if (s.has(Mechanism.DISHABITUATION_RECOVERY)) {
                    s.habituation[i] =
                        fx.unit(fx.sub(s.habituation[i], SpikeContract.DISHABITUATION_RELEASE))
                }
            }
        }

        // Habit decay is independent of exposure so an unused habit weakens.
        if (s.has(Mechanism.HABIT_EXPECTANCY)) {
            for (k in s.habit.indices) {
                s.habit[k] = fx.decay(s.habit[k], SpikeContract.HABIT_DECAY_RETENTION_PER_TICK)
            }
        }
        // Competence fades slowly without practice, so a specialization has to
        // be maintained rather than acquired once and kept for free.
        if (s.has(Mechanism.SKILL_PROFICIENCY)) {
            for (k in s.skill.indices) {
                s.skill[k] = fx.decay(s.skill[k], SpikeContract.SKILL_DECAY_RETENTION_PER_TICK)
            }
        }
        // A value estimate goes stale while its option is neglected. This is the
        // whole re-exploration mechanism: nothing here is random, an option
        // simply becomes worth checking again once the organism's information
        // about it is old enough.
        if (s.has(Mechanism.OUTCOME_UNCERTAINTY)) {
            for (k in s.uncertainty.indices) {
                s.uncertainty[k] = fx.unit(
                    fx.add(s.uncertainty[k], SpikeContract.UNCERTAINTY_GROWTH_PER_TICK),
                )
            }
        }

        for (a in s.actionSatiation.indices) {
            s.actionSatiation[a] =
                fx.decay(s.actionSatiation[a], SpikeContract.ACTION_SATIATION_RETENTION_PER_TICK)
        }

        if (s.commitmentRemaining > 0) s.commitmentRemaining -= 1
        if (s.commitmentRemaining == 0) {
            s.committedAction = null
            s.committedTarget = null
        }
    }

    /** Apply the consequences of an executed action. */
    public fun applyOutcome(
        s: OrganismState,
        action: SpikeAction,
        target: HabitatObject?,
        outcome: Outcome,
        habitat: Habitat,
        tick: Long,
        fx: Fx,
    ) {
        // Direct physiological consequences.
        when (action) {
            SpikeAction.EAT ->
                if (outcome.success) {
                    s.energy = fx.unit(fx.add(s.energy, SpikeContract.EAT_ENERGY_GAIN))
                }
            SpikeAction.SLEEP -> s.rest = fx.unit(fx.add(s.rest, SpikeContract.SLEEP_REST_GAIN_PER_TICK))
            SpikeAction.REST -> s.rest = fx.unit(fx.add(s.rest, SpikeContract.REST_REST_GAIN_PER_TICK))
            SpikeAction.SEEK_INTERACTION, SpikeAction.RESPOND_TO_TOUCH ->
                if (outcome.success) {
                    s.social = fx.unit(fx.add(s.social, SpikeContract.SOCIAL_GAIN_PER_INTERACTION))
                }
            else -> Unit
        }
        s.actionSatiation[action.ordinal] = fx.unit(
            fx.add(s.actionSatiation[action.ordinal], SpikeContract.ACTION_SATIATION_PER_TICK),
        )

        // Vigorous activity is metabolically expensive.
        when (action) {
            SpikeAction.PLAY -> {
                if (outcome.success) {
                    s.social = fx.unit(fx.add(s.social, SpikeContract.SOCIAL_GAIN_PER_INTERACTION))
                }
                s.energy = fx.unit(fx.sub(s.energy, SpikeContract.PLAY_ENERGY_COST_PER_TICK))
            }
            SpikeAction.EXPLORE ->
                s.energy = fx.unit(fx.sub(s.energy, SpikeContract.EXPLORE_ENERGY_COST_PER_TICK))
            else -> Unit
        }

        // Modulators respond to outcome magnitude, not to action identity.
        if (outcome.strongNegative) {
            s.lastThreatTick = tick
            s.lastThreatTarget = target
            s.stress = fx.unit(fx.add(s.stress, FixedPoint.of(0L, 260_000L)))
            s.arousal = fx.unit(fx.add(s.arousal, FixedPoint.of(0L, 340_000L)))
            s.safety = fx.unit(fx.sub(s.safety, FixedPoint.of(0L, 520_000L)))
        }
        s.rewardExpectancy = fx.unit(
            fx.add(s.rewardExpectancy, fx.mul(outcome.valence, FixedPoint.of(0L, 120_000L))),
        )

        if (target == null) return
        val i = target.ordinal0

        // Habituation: exposure raises the trace toward an eroding ceiling.
        if (s.has(Mechanism.HABITUATION) && Controller.epistemic(action)) {
            val ceiling = minOf(s.habituationCeiling[i], SpikeContract.HABITUATION_MAX)
            s.habituation[i] = fx.clamp(
                fx.add(s.habituation[i], SpikeContract.HABITUATION_GAIN_PER_EXPOSURE),
                FixedPoint.ZERO,
                ceiling,
            )
            if (s.habituation[i] >= ceiling) {
                s.habituationCeiling[i] = maxOf(
                    SpikeContract.HABITUATION_CEILING_FLOOR,
                    fx.sub(s.habituationCeiling[i], SpikeContract.HABITUATION_CEILING_DECLINE_PER_CYCLE),
                )
            }
        }

        if (s.has(Mechanism.SENSITIZATION) && outcome.strongNegative) {
            s.sensitization[i] = fx.unit(fx.add(s.sensitization[i], SpikeContract.SENSITIZATION_GAIN))
        }

        // Preference is a running estimate of what this object is worth, moved
        // toward the observed valence. An accumulator was tried first and is the
        // wrong law: anything with a net-positive outcome eventually pins at the
        // bound, so a reliable food source and an unreliable one become
        // indistinguishable and no reversal can ever be observed.
        // Only a consequential outcome is evidence about value. Updating on
        // neutral outcomes too made preference an average over mostly-neutral
        // glances: once the revised candidate looked at things far more often
        // than it ate them, a reliably good food source scored zero.
        if (s.has(Mechanism.PREFERENCE_LEARNING) && outcome.valence != FixedPoint.ZERO) {
            val error = fx.sub(outcome.valence, s.preference[i])
            s.preference[i] = fx.signed(
                fx.add(s.preference[i], fx.mul(error, SpikeContract.PREFERENCE_LEARNING_RATE)),
            )
        }

        // Fear acquisition and extinction.
        if (s.has(Mechanism.CONDITIONED_FEAR) && outcome.strongNegative) {
            s.fear[i] = fx.unit(
                fx.add(s.fear[i], fx.mul(SpikeContract.FEAR_ACQUISITION_RATE, s.traits.caution)),
            )
            if (s.fear[i] > s.fearPeak[i]) s.fearPeak[i] = s.fear[i]
        }
        if (s.has(Mechanism.FEAR_EXTINCTION) && outcome.safeEvidence && s.fear[i] > FixedPoint.ZERO) {
            val residual = fx.mul(s.fearPeak[i], SpikeContract.FEAR_EXTINCTION_RESIDUAL_FRACTION)
            s.fear[i] = maxOf(residual, fx.sub(s.fear[i], SpikeContract.FEAR_EXTINCTION_RATE))
        }

        // Habit, competence, uncertainty and futility accounting.
        val k = s.index(action, target)

        // Prediction error, computed before the estimates move. A contradicted
        // expectation is what makes an option worth re-checking, and it is also
        // what makes a devalued option's *alternatives* worth re-checking.
        if (s.has(Mechanism.OUTCOME_UNCERTAINTY)) {
            val expected = if (s.has(Mechanism.HABIT_EXPECTANCY)) s.habit[k] else FixedPoint.ZERO
            val observed = if (outcome.success) SpikeContract.HABIT_MAX else FixedPoint.ZERO
            val surprise = fx.sub(observed, expected).let { if (it < 0L) -it else it }
            s.uncertainty[k] = if (surprise >= SpikeContract.UNCERTAINTY_SURPRISE_THRESHOLD) {
                fx.unit(
                    fx.add(
                        s.uncertainty[k],
                        fx.mul(surprise, SpikeContract.UNCERTAINTY_SURPRISE_GAIN),
                    ),
                )
            } else {
                // Sampling an option that behaved as expected settles it.
                fx.unit(fx.sub(s.uncertainty[k], SpikeContract.UNCERTAINTY_DROP_ON_SAMPLE))
            }
        }

        if (s.has(Mechanism.SKILL_PROFICIENCY) && outcome.success) {
            val error = fx.sub(SpikeContract.SKILL_MAX, s.skill[k])
            s.skill[k] = fx.clamp(
                fx.add(s.skill[k], fx.mul(error, SpikeContract.SKILL_GAIN_ON_SUCCESS)),
                FixedPoint.ZERO,
                SpikeContract.SKILL_MAX,
            )
        }
        // Habit tracks how often this action works here, for the same reason
        // preference tracks value rather than accumulating it. Failure moves it
        // down faster than success moves it up.
        if (s.has(Mechanism.HABIT_EXPECTANCY)) {
            val target = if (outcome.success) SpikeContract.HABIT_MAX else FixedPoint.ZERO
            val rate = if (outcome.success) {
                SpikeContract.HABIT_GAIN_ON_SUCCESS
            } else {
                SpikeContract.HABIT_LOSS_ON_FAILURE
            }
            val error = fx.sub(target, s.habit[k])
            s.habit[k] = fx.clamp(
                fx.add(s.habit[k], fx.mul(error, rate)),
                FixedPoint.ZERO,
                SpikeContract.HABIT_MAX,
            )
        }
        if (outcome.success) {
            s.failureCount[k] = 0
            // New evidence clears suppression immediately.
            s.suppressedUntilTick[k] = 0L
        } else {
            s.failureCount[k] = minOf(s.failureCount[k] + 1, RETRY_BUDGET_MAX)
            if (s.failureCount[k] >= RETRY_BUDGET) {
                s.suppressedUntilTick[k] = tick + SUPPRESSION_TICKS
            }
        }

        // Relationship history, per person.
        if (s.has(Mechanism.RELATIONSHIP_DIFFERENTIATION) &&
            target.kind == ObjectKind.SOCIAL &&
            action in INTERACTIVE_ACTIONS
        ) {
            val rate = if (outcome.valence >= FixedPoint.ZERO) {
                SpikeContract.RELATIONSHIP_GAIN_POSITIVE
            } else {
                SpikeContract.RELATIONSHIP_LOSS_NEGATIVE
            }
            val error = fx.sub(outcome.valence, s.relationship[i])
            s.relationship[i] = fx.signed(fx.add(s.relationship[i], fx.mul(error, rate)))
        }

        // Recent-inspection inhibition and the inspection timestamp.
        if (Controller.epistemic(action)) {
            // Inhibition accrues at a per-tick rate that sums to one full depth
            // over a nominal bout. Charging it only on the first tick of a bout
            // meant an organism that never stopped engaging one object never
            // accrued any inhibition at all, which is how a single play object
            // reached fifty-nine per cent of the tick budget under D008.
            s.lastInspectedTick[i] = tick
            if (s.has(Mechanism.RECENT_INSPECTION_INHIBITION)) {
                val perTick = s.curiosity.inhibitionDepth / BOUT_TICKS
                s.inhibition[i] = fx.unit(fx.add(s.inhibition[i], perTick))
            }
            // Bounded engagement: a long enough bout with one object ends it and
            // makes that object refractory for a while.
            s.engagementTicks[i] += 1
            if (s.engagementTicks[i] >= SpikeContract.MAX_ENGAGEMENT_TICKS) {
                s.engagementTicks[i] = 0
                s.engagementRefractoryUntil[i] = tick + SpikeContract.ENGAGEMENT_REFRACTORY_TICKS
            }
        }

        if (s.has(Mechanism.EPISODIC_HISTORY)) {
            s.recordEpisode(
                Episode(
                    tick = tick,
                    action = action,
                    target = target,
                    person = outcome.personResponded,
                    valence = outcome.valence,
                    context = contextBucket(habitat, tick),
                ),
            )
        }
    }

    /**
     * The context an episode is filed under. Circadian quarter is the coarsest
     * context that still distinguishes "this went well in the morning" from
     * "this went badly at night", which is the kind of conjunction a
     * context-free preference cannot represent.
     */
    public fun contextBucket(habitat: Habitat, tick: Long): Int {
        val phase = habitat.circadianPhase(tick)
        return ((phase * SpikeContract.EPISODIC_CONTEXT_BUCKETS) / FixedPoint.SCALE).toInt()
            .coerceIn(0, SpikeContract.EPISODIC_CONTEXT_BUCKETS - 1)
    }

    /** Relationship value accrues from attempted interaction, not from looking. */
    private val INTERACTIVE_ACTIONS = setOf(
        SpikeAction.SEEK_INTERACTION,
        SpikeAction.VOCALIZE,
        SpikeAction.PLAY,
        SpikeAction.RESPOND_TO_TOUCH,
    )

    /** Nominal engagement bout, in ticks. One bout accrues one inhibition depth. */
    private const val BOUT_TICKS = 6L

    private const val RETRY_BUDGET = 4
    private const val RETRY_BUDGET_MAX = 64
    private const val SUPPRESSION_TICKS = 6L * SpikeContract.TICKS_PER_VIRTUAL_HOUR
}
