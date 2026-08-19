package com.animusmachinae.dll17.android

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.Cohorts
import com.animusmachinae.dll17.research.aliveness.Fx
import com.animusmachinae.dll17.research.aliveness.Habitat
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionEvent
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.OutcomeModel
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import com.animusmachinae.dll17.research.aliveness.SpikeRuntime
import com.animusmachinae.dll17.research.aliveness.StepRecord

/**
 * D016-X/Y/Z owner/debug bridge. It owns timing and event normalization, but
 * never chooses an organism action or synthesizes a presentation frame.
 */
internal class OwnerAlivenessHarness(
    seed: Long = DEFAULT_SEED,
) : DebugExperienceHarness {
    private val fx = Fx.counting().first
    private val habitat = Habitat(seed, HabitatCondition.CONTROLLED_NOVELTY)
    private val organism = Cohorts.create(Cohort.FULL, seed, fx)
    private val runtime = SpikeRuntime(
        runId = "D016-AA-OWNER-PIXEL",
        agent = organism,
        habitat = habitat,
        outcomes = OutcomeModel(),
        fx = fx,
        traceEveryDecision = false,
        attributionSampleEvery = 0,
    )
    private val pending = ArrayList<InteractionEvent>()

    internal var tick: Long = 0L
        private set

    override var frame: SpikeExpressionContract.ExpressionFrame = initialFrame()
        private set

    override val showManualControls: Boolean = true

    override val statusText: String = "screen-companion research scaffold"

    internal val presentObjects: List<HabitatObject>
        get() = HabitatObject.ALL.filter(habitat::isPresent)

    /** Diagnostics for local validation only. Never rendered by the owner UI. */
    internal val deliveredInteractions = ArrayList<InteractionEvent>()

    override fun submit(kind: InteractionKind, target: HabitatObject?) {
        val person = target?.takeIf { it.kind == com.animusmachinae.dll17.research.aliveness.ObjectKind.SOCIAL }
        pending += InteractionEvent(tick, kind, target, person)
    }

    override fun advance(): StepRecord {
        val events = pending.toList()
        pending.clear()
        deliveredInteractions += events
        val record = runtime.step(tick, events)
        frame = record.frame
        tick += 1L
        return record
    }

    private companion object {
        const val DEFAULT_SEED: Long = 20260818L

        fun initialFrame(): SpikeExpressionContract.ExpressionFrame =
            SpikeExpressionContract.frameFor(
                SpikeExpressionContract.PresentationInput(
                    action = com.animusmachinae.dll17.research.aliveness.SpikeAction.IDLE_VARIATION,
                    target = null,
                    intensity = 0L,
                    valence = 0L,
                    tick = 0L,
                ),
            )
    }
}

internal fun OwnerInteraction.toEvent(): Pair<InteractionKind, HabitatObject?> = when (this) {
    OwnerInteraction.TOUCH -> InteractionKind.TOUCH to HabitatObject.PERSON_ALPHA
    OwnerInteraction.CALL -> InteractionKind.CALL to HabitatObject.PERSON_ALPHA
    OwnerInteraction.OFFER_FOOD -> InteractionKind.OFFER_FOOD to HabitatObject.FOOD_TROUGH
    OwnerInteraction.PRESENT_OBJECT -> InteractionKind.PRESENT_OBJECT to HabitatObject.PLAY_BALL
    OwnerInteraction.WITHDRAW_ATTENTION -> InteractionKind.WITHDRAW_ATTENTION to null
    OwnerInteraction.STARTLE -> InteractionKind.STARTLE to HabitatObject.AVERSIVE_BUZZER
}

internal enum class OwnerInteraction {
    TOUCH,
    CALL,
    OFFER_FOOD,
    PRESENT_OBJECT,
    WITHDRAW_ATTENTION,
    STARTLE,
}

internal const val OWNER_TICK_MILLIS: Long = SpikeContract.VIEWER_TICK_MILLIS.toLong()
