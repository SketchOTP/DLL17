package com.animusmachinae.dll17.android

import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.SpikeAction
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import com.animusmachinae.dll17.research.aliveness.StepRecord

/** Normalized presentation-only point inside the phone screen. */
internal data class ScenePoint(val x: Float, val y: Float)

/** One-to-one body-language vocabulary for the canonical action set. */
internal enum class EmbodiedBehavior {
    INSPECTING, ORIENTING, APPROACHING, RETREATING, SEEKING_OWNER,
    RESPONDING_TO_TOUCH, VOCALIZING, EXPLORING, PLAYING, EATING,
    RESTING, SLEEPING, RESUMING, RETRYING, IDLING,
}

internal enum class CompanionCue { ATTENTION, ALERT, FOOD, PLAY, SLEEP }

internal data class OwnerEmbodimentFrame(
    val tick: Long,
    val action: SpikeAction,
    val target: HabitatObject?,
    val behavior: EmbodiedBehavior,
    val position: ScenePoint,
    val facing: Float,
    val depthScale: Float,
    val bodyLean: Float,
    val expression: SpikeExpressionContract.Expression,
    val gazeTarget: HabitatObject?,
    val motionAmplitude: Long,
    val vocalizing: Boolean,
    val microMovement: String,
    val cue: CompanionCue?,
)

/**
 * D016-Z owner-only presentation adapter.
 *
 * Every pose is derived from the actual selected action. Position is a small
 * local body-language offset around screen center, never a habitat destination.
 * This adapter has no organism input and cannot select or reward an action.
 */
internal class OwnerEmbodimentAdapter(initialFrame: SpikeExpressionContract.ExpressionFrame) {
    internal var current: OwnerEmbodimentFrame = frameFor(
        tick = 0L,
        action = SpikeAction.IDLE_VARIATION,
        target = null,
        frame = initialFrame,
    )
        private set

    internal fun consume(record: StepRecord): OwnerEmbodimentFrame {
        current = frameFor(record.tick, record.choice.action, record.choice.target, record.frame)
        return current
    }
}

private fun frameFor(
    tick: Long,
    action: SpikeAction,
    target: HabitatObject?,
    frame: SpikeExpressionContract.ExpressionFrame,
): OwnerEmbodimentFrame {
    val pose = poseFor(action, tick)
    return OwnerEmbodimentFrame(
        tick = tick,
        action = action,
        target = target,
        behavior = embodiedBehaviorFor(action),
        position = pose.position,
        facing = facingFor(action, target, tick),
        depthScale = pose.depthScale,
        bodyLean = pose.bodyLean,
        expression = frame.expression,
        gazeTarget = frame.gazeTarget ?: target,
        motionAmplitude = frame.motionAmplitude,
        vocalizing = frame.vocalizing,
        microMovement = frame.microMovement,
        cue = cueFor(action),
    )
}

private data class CompanionPose(
    val position: ScenePoint,
    val depthScale: Float,
    val bodyLean: Float,
)

private fun poseFor(action: SpikeAction, tick: Long): CompanionPose = when (action) {
    SpikeAction.OBSERVE -> CompanionPose(ScenePoint(0.50f, 0.52f), 1.00f, -3f)
    SpikeAction.ORIENT -> CompanionPose(ScenePoint(0.50f, 0.52f), 1.00f, 5f)
    SpikeAction.APPROACH -> CompanionPose(ScenePoint(0.50f, 0.50f), 1.10f, -5f)
    SpikeAction.WITHDRAW -> CompanionPose(ScenePoint(0.50f, 0.56f), 0.90f, 7f)
    SpikeAction.SEEK_INTERACTION -> CompanionPose(ScenePoint(0.50f, 0.49f), 1.13f, -7f)
    SpikeAction.RESPOND_TO_TOUCH -> CompanionPose(ScenePoint(0.48f, 0.51f), 1.06f, -9f)
    SpikeAction.VOCALIZE -> CompanionPose(ScenePoint(0.50f, 0.51f), 1.04f, -2f)
    SpikeAction.EXPLORE -> CompanionPose(
        ScenePoint(if ((tick / 4L) % 2L == 0L) 0.47f else 0.53f, 0.52f),
        0.99f,
        if ((tick / 4L) % 2L == 0L) -4f else 4f,
    )
    SpikeAction.PLAY -> CompanionPose(ScenePoint(0.50f, 0.53f), 1.04f, -8f)
    SpikeAction.EAT -> CompanionPose(ScenePoint(0.50f, 0.56f), 1.00f, 10f)
    SpikeAction.REST -> CompanionPose(ScenePoint(0.50f, 0.57f), 0.97f, 0f)
    SpikeAction.SLEEP -> CompanionPose(ScenePoint(0.50f, 0.58f), 0.94f, 0f)
    SpikeAction.RESUME_INTERRUPTED -> CompanionPose(ScenePoint(0.51f, 0.52f), 1.05f, -4f)
    SpikeAction.RETRY -> CompanionPose(ScenePoint(0.49f, 0.52f), 1.02f, 8f)
    SpikeAction.IDLE_VARIATION -> CompanionPose(ScenePoint(0.50f, 0.53f), 1.00f, 0f)
}

private fun facingFor(action: SpikeAction, target: HabitatObject?, tick: Long): Float = when {
    action == SpikeAction.RESPOND_TO_TOUCH || action == SpikeAction.SEEK_INTERACTION -> 1f
    target in LEFT_TARGETS -> -1f
    target != null -> 1f
    action == SpikeAction.EXPLORE && (tick / 4L) % 2L == 0L -> -1f
    else -> 1f
}

private fun cueFor(action: SpikeAction): CompanionCue? = when (action) {
    SpikeAction.SEEK_INTERACTION, SpikeAction.RESPOND_TO_TOUCH -> CompanionCue.ATTENTION
    SpikeAction.WITHDRAW, SpikeAction.RETRY -> CompanionCue.ALERT
    SpikeAction.EAT -> CompanionCue.FOOD
    SpikeAction.PLAY -> CompanionCue.PLAY
    SpikeAction.SLEEP -> CompanionCue.SLEEP
    else -> null
}

internal fun embodiedBehaviorFor(action: SpikeAction): EmbodiedBehavior = when (action) {
    SpikeAction.OBSERVE -> EmbodiedBehavior.INSPECTING
    SpikeAction.ORIENT -> EmbodiedBehavior.ORIENTING
    SpikeAction.APPROACH -> EmbodiedBehavior.APPROACHING
    SpikeAction.WITHDRAW -> EmbodiedBehavior.RETREATING
    SpikeAction.SEEK_INTERACTION -> EmbodiedBehavior.SEEKING_OWNER
    SpikeAction.RESPOND_TO_TOUCH -> EmbodiedBehavior.RESPONDING_TO_TOUCH
    SpikeAction.VOCALIZE -> EmbodiedBehavior.VOCALIZING
    SpikeAction.EXPLORE -> EmbodiedBehavior.EXPLORING
    SpikeAction.PLAY -> EmbodiedBehavior.PLAYING
    SpikeAction.EAT -> EmbodiedBehavior.EATING
    SpikeAction.REST -> EmbodiedBehavior.RESTING
    SpikeAction.SLEEP -> EmbodiedBehavior.SLEEPING
    SpikeAction.RESUME_INTERRUPTED -> EmbodiedBehavior.RESUMING
    SpikeAction.RETRY -> EmbodiedBehavior.RETRYING
    SpikeAction.IDLE_VARIATION -> EmbodiedBehavior.IDLING
}

/** The creature is the direct scene target; a background tap withdraws attention. */
internal fun interactionAt(normalizedTap: ScenePoint, petPosition: ScenePoint): OwnerInteraction =
    if (
        normalizedTap.x in (petPosition.x - 0.30f)..(petPosition.x + 0.30f) &&
        normalizedTap.y in (petPosition.y - 0.30f)..(petPosition.y + 0.30f)
    ) OwnerInteraction.TOUCH else OwnerInteraction.WITHDRAW_ATTENTION

private val LEFT_TARGETS = setOf(
    HabitatObject.FOOD_TROUGH,
    HabitatObject.FOOD_CACHE,
    HabitatObject.PLAY_CHIME,
    HabitatObject.NOVEL_SLOT,
    HabitatObject.AVERSIVE_BUZZER,
)
