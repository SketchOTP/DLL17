package com.animusmachinae.dll17.android

import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.SpikeAction
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import com.animusmachinae.dll17.research.aliveness.StepRecord
import kotlin.math.sqrt

/** Normalized, presentation-only position inside the owner habitat. */
internal data class ScenePoint(val x: Float, val y: Float)

/**
 * One-to-one embodiment vocabulary for the canonical SpikeAction set.
 *
 * These values describe how Android depicts an action the organism already
 * selected. They never feed back into the runtime and cannot select, replace or
 * reward an action.
 */
internal enum class EmbodiedBehavior {
    INSPECTING,
    ORIENTING,
    APPROACHING,
    RETREATING,
    SEEKING_OWNER,
    RESPONDING_TO_TOUCH,
    VOCALIZING,
    EXPLORING,
    PLAYING,
    EATING,
    RESTING,
    SLEEPING,
    RESUMING,
    RETRYING,
    IDLING,
}

internal data class OwnerEmbodimentFrame(
    val tick: Long,
    val action: SpikeAction,
    val target: HabitatObject?,
    val behavior: EmbodiedBehavior,
    val position: ScenePoint,
    val facing: Float,
    val expression: SpikeExpressionContract.Expression,
    val gazeTarget: HabitatObject?,
    val motionAmplitude: Long,
    val vocalizing: Boolean,
    val microMovement: String,
)

/**
 * D016-Y owner-only presentation adapter.
 *
 * It consumes the actual StepRecord so action identity is not lost at the
 * ExpressionFrame boundary. Its only mutable state is screen-space position;
 * canonical organism state, action selection and outcomes remain untouched.
 */
internal class OwnerEmbodimentAdapter(
    initialFrame: SpikeExpressionContract.ExpressionFrame,
) {
    private var position = ScenePoint(0.50f, 0.57f)
    private var facing = 1f

    internal var current: OwnerEmbodimentFrame = OwnerEmbodimentFrame(
        tick = 0L,
        action = SpikeAction.IDLE_VARIATION,
        target = null,
        behavior = EmbodiedBehavior.IDLING,
        position = position,
        facing = facing,
        expression = initialFrame.expression,
        gazeTarget = initialFrame.gazeTarget,
        motionAmplitude = initialFrame.motionAmplitude,
        vocalizing = initialFrame.vocalizing,
        microMovement = initialFrame.microMovement,
    )
        private set

    internal fun consume(record: StepRecord): OwnerEmbodimentFrame {
        val action = record.choice.action
        val target = record.choice.target
        val destination = destinationFor(action, target, record.tick)
        val previous = position

        position = when (action) {
            SpikeAction.APPROACH,
            SpikeAction.EXPLORE,
            SpikeAction.PLAY,
            SpikeAction.EAT,
            SpikeAction.RETRY,
            SpikeAction.RESUME_INTERRUPTED,
            -> moveToward(position, destination, 0.075f)

            SpikeAction.SEEK_INTERACTION -> moveToward(position, OWNER_EDGE, 0.070f)
            SpikeAction.REST, SpikeAction.SLEEP -> moveToward(position, BED, 0.055f)
            SpikeAction.WITHDRAW -> moveAway(position, destination, 0.085f)
            else -> position
        }

        val gaze = record.frame.gazeTarget ?: target
        val gazePoint = gaze?.let(::scenePointFor)
        val horizontalIntent = when {
            position.x != previous.x -> position.x - previous.x
            gazePoint != null -> gazePoint.x - position.x
            else -> 0f
        }
        if (horizontalIntent > 0.005f) facing = 1f
        if (horizontalIntent < -0.005f) facing = -1f

        current = OwnerEmbodimentFrame(
            tick = record.tick,
            action = action,
            target = target,
            behavior = embodiedBehaviorFor(action),
            position = position,
            facing = facing,
            expression = record.frame.expression,
            gazeTarget = gaze,
            motionAmplitude = record.frame.motionAmplitude,
            vocalizing = record.frame.vocalizing,
            microMovement = record.frame.microMovement,
        )
        return current
    }
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

internal fun scenePointFor(target: HabitatObject): ScenePoint = when (target) {
    HabitatObject.FOOD_TROUGH, HabitatObject.FOOD_CACHE -> FOOD_BOWL
    HabitatObject.SHELTER -> BED
    HabitatObject.PERSON_ALPHA, HabitatObject.PERSON_BETA -> OWNER_EDGE
    HabitatObject.PLAY_BALL -> BALL
    HabitatObject.PLAY_CUBE -> ScenePoint(0.70f, 0.64f)
    HabitatObject.PLAY_CHIME -> ScenePoint(0.16f, 0.38f)
    HabitatObject.PLAY_MIRROR -> ScenePoint(0.84f, 0.43f)
    HabitatObject.AVERSIVE_BUZZER -> ScenePoint(0.12f, 0.18f)
    HabitatObject.SEALED_GATE -> ScenePoint(0.88f, 0.24f)
    HabitatObject.NOVEL_SLOT -> ScenePoint(0.18f, 0.25f)
}

private fun destinationFor(action: SpikeAction, target: HabitatObject?, tick: Long): ScenePoint = when {
    action == SpikeAction.REST || action == SpikeAction.SLEEP -> ScenePoint(0.68f, 0.38f)
    action == SpikeAction.SEEK_INTERACTION || action == SpikeAction.RESPOND_TO_TOUCH -> OWNER_EDGE
    action == SpikeAction.EAT && target != null -> approachPointFor(target)
    action == SpikeAction.PLAY && target != null -> approachPointFor(target)
    action == SpikeAction.APPROACH && target != null -> approachPointFor(target)
    action == SpikeAction.RETRY && target != null -> approachPointFor(target)
    action == SpikeAction.RESUME_INTERRUPTED && target != null -> approachPointFor(target)
    target != null -> scenePointFor(target)
    action == SpikeAction.EXPLORE -> if ((tick / 6L) % 2L == 0L) {
        ScenePoint(0.28f, 0.42f)
    } else {
        ScenePoint(0.72f, 0.52f)
    }
    else -> ScenePoint(0.50f, 0.57f)
}

private fun approachPointFor(target: HabitatObject): ScenePoint {
    val targetPoint = scenePointFor(target)
    val horizontalOffset = if (targetPoint.x < 0.50f) 0.16f else -0.16f
    return ScenePoint(targetPoint.x + horizontalOffset, targetPoint.y - 0.02f).clamped()
}

private fun moveToward(from: ScenePoint, to: ScenePoint, step: Float): ScenePoint {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val distance = sqrt(dx * dx + dy * dy)
    if (distance <= step || distance == 0f) return to.clamped()
    return ScenePoint(from.x + dx / distance * step, from.y + dy / distance * step).clamped()
}

private fun moveAway(from: ScenePoint, threat: ScenePoint, step: Float): ScenePoint {
    val dx = from.x - threat.x
    val dy = from.y - threat.y
    val distance = sqrt(dx * dx + dy * dy)
    val safeDx = if (distance == 0f) 1f else dx / distance
    val safeDy = if (distance == 0f) 0f else dy / distance
    return ScenePoint(from.x + safeDx * step, from.y + safeDy * step).clamped()
}

private fun ScenePoint.clamped(): ScenePoint = ScenePoint(
    x.coerceIn(0.16f, 0.84f),
    y.coerceIn(0.28f, 0.78f),
)

/** Direct scene hit testing; decorative habitat elements deliberately return null. */
internal fun interactionAt(
    normalizedTap: ScenePoint,
    petPosition: ScenePoint,
): OwnerInteraction? = when {
    normalizedTap.distanceTo(petPosition) <= 0.17f -> OwnerInteraction.TOUCH
    normalizedTap.distanceTo(FOOD_BOWL) <= 0.12f -> OwnerInteraction.OFFER_FOOD
    normalizedTap.distanceTo(BALL) <= 0.12f -> OwnerInteraction.PRESENT_OBJECT
    else -> null
}

private fun ScenePoint.distanceTo(other: ScenePoint): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}

internal val FOOD_BOWL = ScenePoint(0.22f, 0.72f)
internal val BED = ScenePoint(0.78f, 0.34f)
internal val BALL = ScenePoint(0.78f, 0.72f)
internal val OWNER_EDGE = ScenePoint(0.50f, 0.80f)
