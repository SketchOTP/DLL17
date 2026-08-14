package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/** One agent's committed choice for a tick. */
public class AgentChoice(
    public val action: SpikeAction,
    public val target: HabitatObject?,
    public val tier: Int,
    public val decision: Decision?,
)

/**
 * The controller interface every cohort implements.
 *
 * The runtime, the habitat, the outcome model and the presentation contract are
 * all shared; a cohort is exactly the difference in how the next action is
 * chosen. That is the only way the study can attribute a rater's judgement to
 * the mechanism set rather than to the surrounding engineering.
 */
public interface SpikeAgent {
    public val cohort: Cohort
    public fun decide(habitat: Habitat, tick: Long): AgentChoice
    public fun applyOutcome(choice: AgentChoice, outcome: Outcome, habitat: Habitat, tick: Long)
    public fun receive(event: InteractionEvent, tick: Long)
    public fun advance(habitat: Habitat, tick: Long)
    public fun presentation(choice: AgentChoice, tick: Long): SpikeExpressionContract.PresentationInput
}

/** FULL and the three human-rated leave-one-out arms. */
public class OrganismAgent(
    override val cohort: Cohort,
    public val seed: Long,
    public val fx: Fx,
    public val curiosity: CuriosityParameters = CuriosityParameters.DEFAULT,
) : SpikeAgent {

    public val state: OrganismState = OrganismState(seed, cohort.mechanisms, curiosity)
    public val controller: Controller = Controller(fx)
    private val tieBreak = SpikeRandom(seed, SpikeRandomDomain.CURIOSITY_TIE_BREAK)

    public var lastProposals: List<Proposal> = emptyList()
        private set

    override fun advance(habitat: Habitat, tick: Long) {
        MechanismUpdates.advance(state, habitat, tick, fx)
    }

    override fun decide(habitat: Habitat, tick: Long): AgentChoice {
        val proposals = controller.propose(state, habitat, tick)
        lastProposals = proposals
        val decision = controller.select(proposals, state, habitat, tick, tieBreak)
        val winner = decision.winner
        if (!decision.commitmentContinuation && state.has(Mechanism.TIERED_COMMITMENT)) {
            // Only a voluntary activity is worth resuming. Recording a retreat or
            // a reflex as resumable created a punishment loop: the organism
            // withdrew from the aversive object, was interrupted, "resumed" the
            // withdrawal by engaging the object again, and was punished for it —
            // eighty-seven times a virtual day.
            val resumable = state.committedAction != null &&
                state.commitmentRemaining > 0 &&
                state.committedTier >= 3 &&
                state.committedAction !in NON_RESUMABLE
            if (resumable) {
                state.interruptedAction = state.committedAction
                state.interruptedTarget = state.committedTarget
                state.interruptedAtTick = tick
            }
            state.committedAction = winner.action
            state.committedTarget = winner.target
            state.committedTier = decision.winningTier
            state.commitmentRemaining = when (winner.action) {
                SpikeAction.SLEEP -> SpikeContract.COMMITMENT_TICKS_SLEEP
                else -> SpikeContract.COMMITMENT_TICKS_DEFAULT
            }
        }
        if (winner.action == SpikeAction.RESUME_INTERRUPTED) {
            state.interruptedAction = null
            state.interruptedTarget = null
        }
        when (winner.action) {
            SpikeAction.VOCALIZE ->
                state.refractoryUntilTick[winner.action.ordinal] =
                    tick + SpikeContract.REFRACTORY_TICKS_VOCALIZE
            SpikeAction.SEEK_INTERACTION ->
                state.refractoryUntilTick[winner.action.ordinal] =
                    tick + SpikeContract.REFRACTORY_TICKS_SEEK_INTERACTION
            else -> Unit
        }
        if (winner.action == SpikeAction.RESPOND_TO_TOUCH) state.pendingTouchFrom = null
        return AgentChoice(winner.action, winner.target, decision.winningTier, decision)
    }

    override fun applyOutcome(
        choice: AgentChoice,
        outcome: Outcome,
        habitat: Habitat,
        tick: Long,
    ) {
        MechanismUpdates.applyOutcome(state, choice.action, choice.target, outcome, habitat, tick, fx)
    }

    override fun receive(event: InteractionEvent, tick: Long) {
        state.lastInteractionTick = tick
        when (event.kind) {
            InteractionKind.TOUCH -> {
                state.pendingTouchFrom = event.personId ?: HabitatObject.PERSON_ALPHA
                state.arousal = fx.unit(fx.add(state.arousal, FixedPoint.of(0L, 200_000L)))
            }
            InteractionKind.CALL ->
                state.arousal = fx.unit(fx.add(state.arousal, FixedPoint.of(0L, 150_000L)))
            InteractionKind.OFFER_FOOD ->
                state.energy = fx.unit(fx.sub(state.energy, FixedPoint.of(0L, 40_000L)))
            InteractionKind.PRESENT_OBJECT -> event.target?.let { o ->
                state.absoluteShift[o.ordinal0] = fx.clamp(
                    fx.add(state.absoluteShift[o.ordinal0], SpikeContract.ABSOLUTE_SHIFT_RESERVOIR_DRAW),
                    FixedPoint.ZERO,
                    SpikeContract.ABSOLUTE_SHIFT_MAX,
                )
                if (state.has(Mechanism.DISHABITUATION_RECOVERY)) {
                    state.habituation[o.ordinal0] = fx.unit(
                        fx.sub(state.habituation[o.ordinal0], SpikeContract.DISHABITUATION_RELEASE),
                    )
                }
            }
            InteractionKind.WITHDRAW_ATTENTION ->
                state.social = fx.unit(fx.sub(state.social, FixedPoint.of(0L, 60_000L)))
            InteractionKind.STARTLE -> {
                state.arousal = fx.unit(fx.add(state.arousal, FixedPoint.of(0L, 450_000L)))
                state.safety = fx.unit(fx.sub(state.safety, FixedPoint.of(0L, 300_000L)))
            }
        }
    }

    override fun presentation(
        choice: AgentChoice,
        tick: Long,
    ): SpikeExpressionContract.PresentationInput {
        val intensity = fx.unit(
            fx.add(fx.mul(state.arousal, FixedPoint.of(0L, 700_000L)), fx.mul(state.stress, FixedPoint.of(0L, 300_000L))),
        )
        val valence = fx.signed(
            fx.sub(
                fx.mul(state.rewardExpectancy, FixedPoint.of(1L, 200_000L)),
                fx.add(fx.mul(state.stress, FixedPoint.of(0L, 800_000L)), FixedPoint.of(0L, 400_000L)),
            ),
        )
        return SpikeExpressionContract.PresentationInput(
            action = choice.action,
            target = choice.target,
            intensity = intensity,
            valence = valence,
            tick = tick,
        )
    }
}

private val NON_RESUMABLE = setOf(
    SpikeAction.WITHDRAW,
    SpikeAction.RESPOND_TO_TOUCH,
    SpikeAction.RESUME_INTERRUPTED,
    SpikeAction.RETRY,
)

/**
 * `ScriptedPetBaselineV1` — the primary external comparator.
 *
 * This is deliberately competent. It has the full interaction vocabulary, broad
 * authored contingencies, context sensitivity to time of day and object
 * presence, genuine idle variety, and short refractory behaviour. What it does
 * not have is any state that persists a *learned* consequence: no preference,
 * no conditioned fear, no habit strength, no episodic history, no relationship
 * accumulation. Its timers are authored, not acquired.
 */
public open class ScriptedPetAgent(
    override val cohort: Cohort,
    public val seed: Long,
    protected val fx: Fx,
    protected val degraded: Boolean = false,
) : SpikeAgent {

    protected val idle: SpikeRandom = SpikeRandom(seed, SpikeRandomDomain.SCRIPTED_IDLE_VARIETY)

    /** Authored, non-learned drive analogues so the creature eats and sleeps. */
    protected var hunger: Long = FixedPoint.of(0L, 300_000L)
    protected var fatigue: Long = FixedPoint.of(0L, 300_000L)
    protected var attentionWant: Long = FixedPoint.of(0L, 300_000L)
    protected var excitement: Long = FixedPoint.of(0L, 300_000L)

    private var pendingKind: InteractionKind? = null
    private var pendingTarget: HabitatObject? = null
    private var pendingPerson: HabitatObject? = null
    private var reactionUntilTick: Long = 0L
    private var lastActionTick: Long = 0L
    private var lastScriptAdvanceTick: Long = Long.MIN_VALUE / 4
    private var lastAction: SpikeAction? = null
    private val cooldownUntil = LongArray(SpikeAction.ALL.size)
    private var scriptCursor: Int = 0

    override fun advance(habitat: Habitat, tick: Long) {
        hunger = fx.unit(fx.add(hunger, FixedPoint.of(0L, 300L)))
        fatigue = fx.unit(fx.add(fatigue, if (habitat.isNight(tick)) FixedPoint.of(0L, 420L) else FixedPoint.of(0L, 200L)))
        attentionWant = fx.unit(fx.add(attentionWant, FixedPoint.of(0L, 260L)))
        excitement = fx.unit(fx.decay(excitement, FixedPoint.of(0L, 994_000L)))
    }

    override fun decide(habitat: Habitat, tick: Long): AgentChoice {
        // Authored contingency table, evaluated in priority order. A scripted pet
        // is allowed to look purposeful; what it is not allowed to do is learn.
        val reacting = tick < reactionUntilTick
        val kind = pendingKind

        if (kind != null && reacting) {
            val choice = reactionFor(kind, tick)
            if (choice != null) return choice
        }
        if (!reacting) {
            pendingKind = null
            pendingTarget = null
            pendingPerson = null
        }

        if (hunger > HUNGER_ACT && habitat.isPresent(HabitatObject.FOOD_TROUGH)) {
            return scripted(SpikeAction.EAT, HabitatObject.FOOD_TROUGH, 3, tick)
        }
        if (fatigue > FATIGUE_ACT && (degraded || habitat.isNight(tick))) {
            return scripted(SpikeAction.SLEEP, null, 3, tick)
        }
        if (!degraded && fatigue > FATIGUE_REST) {
            return scripted(SpikeAction.REST, null, 3, tick)
        }
        if (attentionWant > ATTENTION_ACT && tick >= cooldownUntil[SpikeAction.SEEK_INTERACTION.ordinal]) {
            val person = if (degraded) {
                HabitatObject.PERSON_ALPHA
            } else {
                // Context sensitivity: alternate between the two social identities.
                if ((tick / 601L) % 2L == 0L) HabitatObject.PERSON_ALPHA else HabitatObject.PERSON_BETA
            }
            cooldownUntil[SpikeAction.SEEK_INTERACTION.ordinal] =
                tick + SpikeContract.REFRACTORY_TICKS_SEEK_INTERACTION
            return scripted(SpikeAction.SEEK_INTERACTION, person, 3, tick)
        }
        if (!degraded && habitat.isPresent(HabitatObject.NOVEL_SLOT) &&
            habitat.recentCausalChange(HabitatObject.NOVEL_SLOT, tick)
        ) {
            return scripted(SpikeAction.OBSERVE, HabitatObject.NOVEL_SLOT, 4, tick)
        }

        // Idle and play rotation. The strong baseline rotates over a wide script
        // with variety; the degraded control cycles a short one.
        val script = if (degraded) DEGRADED_SCRIPT else BASELINE_SCRIPT
        val holdTicks = if (degraded) 40L else 12L
        if (tick - lastScriptAdvanceTick >= holdTicks) {
            lastScriptAdvanceTick = tick
            scriptCursor = if (degraded) {
                (scriptCursor + 1) % script.size
            } else {
                // Variety without learning: a seeded draw over the authored script.
                idle.nextInt(script.size)
            }
        }
        val (action, target) = script[scriptCursor]
        val resolvedTarget = target?.takeIf { habitat.isPresent(it) }
        if (target != null && resolvedTarget == null) {
            return scripted(SpikeAction.IDLE_VARIATION, null, 5, tick)
        }
        return scripted(action, resolvedTarget, action.tier, tick)
    }

    private fun reactionFor(kind: InteractionKind, tick: Long): AgentChoice? = when (kind) {
        InteractionKind.TOUCH ->
            scripted(SpikeAction.RESPOND_TO_TOUCH, pendingPerson ?: HabitatObject.PERSON_ALPHA, 2, tick)
        InteractionKind.CALL ->
            if (degraded) null
            else scripted(SpikeAction.ORIENT, pendingPerson ?: HabitatObject.PERSON_ALPHA, 2, tick)
        InteractionKind.OFFER_FOOD ->
            scripted(SpikeAction.EAT, HabitatObject.FOOD_TROUGH, 2, tick)
        InteractionKind.PRESENT_OBJECT ->
            if (degraded) null
            else scripted(SpikeAction.OBSERVE, pendingTarget ?: HabitatObject.PLAY_BALL, 2, tick)
        InteractionKind.WITHDRAW_ATTENTION ->
            if (degraded) null else scripted(SpikeAction.IDLE_VARIATION, null, 5, tick)
        InteractionKind.STARTLE ->
            scripted(SpikeAction.WITHDRAW, pendingTarget ?: HabitatObject.AVERSIVE_BUZZER, 0, tick)
    }

    private fun scripted(
        action: SpikeAction,
        target: HabitatObject?,
        tier: Int,
        tick: Long,
    ): AgentChoice {
        lastAction = action
        lastActionTick = tick
        return AgentChoice(action, target, tier, null)
    }

    override fun applyOutcome(
        choice: AgentChoice,
        outcome: Outcome,
        habitat: Habitat,
        tick: Long,
    ) {
        // Authored consequences only. Nothing here persists as learned value.
        when (choice.action) {
            SpikeAction.EAT -> if (outcome.success) hunger = fx.unit(fx.sub(hunger, FixedPoint.of(0L, 340_000L)))
            SpikeAction.SLEEP -> fatigue = fx.unit(fx.sub(fatigue, FixedPoint.of(0L, 2_600L)))
            SpikeAction.REST -> fatigue = fx.unit(fx.sub(fatigue, FixedPoint.of(0L, 900L)))
            SpikeAction.SEEK_INTERACTION, SpikeAction.RESPOND_TO_TOUCH, SpikeAction.PLAY ->
                if (outcome.success) {
                    attentionWant = fx.unit(fx.sub(attentionWant, FixedPoint.of(0L, 260_000L)))
                    excitement = fx.unit(fx.add(excitement, FixedPoint.of(0L, 300_000L)))
                }
            else -> Unit
        }
        if (outcome.strongNegative) excitement = fx.unit(fx.add(excitement, FixedPoint.of(0L, 400_000L)))
    }

    override fun receive(event: InteractionEvent, tick: Long) {
        pendingKind = event.kind
        pendingTarget = event.target
        pendingPerson = event.personId
        reactionUntilTick = tick + if (degraded) 4L else 10L
        excitement = fx.unit(fx.add(excitement, FixedPoint.of(0L, 250_000L)))
    }

    override fun presentation(
        choice: AgentChoice,
        tick: Long,
    ): SpikeExpressionContract.PresentationInput = SpikeExpressionContract.PresentationInput(
        action = choice.action,
        target = choice.target,
        intensity = excitement,
        valence = fx.signed(
            fx.sub(
                fx.mul(fx.sub(FixedPoint.ONE, hunger), FixedPoint.of(0L, 800_000L)),
                FixedPoint.of(0L, 300_000L),
            ),
        ),
        tick = tick,
    )

    protected companion object {
        val HUNGER_ACT: Long = FixedPoint.of(0L, 520_000L)
        val FATIGUE_ACT: Long = FixedPoint.of(0L, 620_000L)
        val FATIGUE_REST: Long = FixedPoint.of(0L, 450_000L)
        val ATTENTION_ACT: Long = FixedPoint.of(0L, 560_000L)

        /** Broad authored idle/play coverage: the strong baseline. */
        val BASELINE_SCRIPT: List<Pair<SpikeAction, HabitatObject?>> = listOf(
            SpikeAction.PLAY to HabitatObject.PLAY_BALL,
            SpikeAction.EXPLORE to HabitatObject.PLAY_CUBE,
            SpikeAction.OBSERVE to HabitatObject.PLAY_CHIME,
            SpikeAction.ORIENT to HabitatObject.PLAY_MIRROR,
            SpikeAction.EXPLORE to HabitatObject.SHELTER,
            SpikeAction.OBSERVE to HabitatObject.PERSON_ALPHA,
            SpikeAction.ORIENT to HabitatObject.PERSON_BETA,
            SpikeAction.VOCALIZE to HabitatObject.PERSON_ALPHA,
            SpikeAction.IDLE_VARIATION to null,
            SpikeAction.OBSERVE to HabitatObject.FOOD_TROUGH,
            SpikeAction.WITHDRAW to HabitatObject.AVERSIVE_BUZZER,
            SpikeAction.PLAY to HabitatObject.PLAY_CUBE,
            SpikeAction.EXPLORE to HabitatObject.PLAY_BALL,
            SpikeAction.OBSERVE to HabitatObject.SEALED_GATE,
        )

        /** Deliberately narrow: the degraded control used only for baseline qualification. */
        val DEGRADED_SCRIPT: List<Pair<SpikeAction, HabitatObject?>> = listOf(
            SpikeAction.IDLE_VARIATION to null,
            SpikeAction.OBSERVE to HabitatObject.PLAY_BALL,
            SpikeAction.IDLE_VARIATION to null,
        )
    }
}

/** Factory so every runner constructs cohorts the same way. */
public object Cohorts {
    public fun create(
        cohort: Cohort,
        seed: Long,
        fx: Fx,
        curiosity: CuriosityParameters = CuriosityParameters.DEFAULT,
    ): SpikeAgent = when (cohort) {
        Cohort.SCRIPTED_PET_BASELINE -> ScriptedPetAgent(cohort, seed, fx, degraded = false)
        Cohort.DEGRADED_SCRIPTED_CONTROL -> ScriptedPetAgent(cohort, seed, fx, degraded = true)
        else -> OrganismAgent(cohort, seed, fx, curiosity)
    }
}
