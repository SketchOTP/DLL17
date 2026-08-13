package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * `SpikeExpressionContractV1` — the frozen presentation system.
 *
 * Every cohort renders through this and only this. The contract deliberately
 * takes a small normalized input that a scripted controller can fill as
 * completely as FULL can, so a difference a rater sees is a difference in what
 * the controller decided, never a difference in how richly it was drawn.
 *
 * The type carries no cohort field. That is not an oversight: a presentation
 * layer that cannot see which controller produced a frame cannot express one
 * more vividly than another.
 */
public object SpikeExpressionContract {

    public const val CONTRACT_ID: String = SpikeContract.EXPRESSION_CONTRACT_ID
    public const val CONTRACT_VERSION: Int = 1

    /** Frozen: the shared idle/micro-movement library. */
    public val MICRO_MOVEMENTS: List<String> = listOf(
        "settle", "weight-shift", "ear-flick", "slow-blink", "tail-curl",
        "breath-deep", "look-away", "small-step", "head-tilt", "stretch",
    )

    /** Frozen: the shared expression vocabulary. */
    public enum class Expression { NEUTRAL, ALERT, CONTENT, WARY, TIRED, EXCITED, WITHDRAWN }

    /** Frozen: the shared posture vocabulary. */
    public enum class Posture { STAND, CROUCH, LEAN_IN, LEAN_AWAY, LIE_DOWN, PLAY_BOW }

    /**
     * The normalized presentation input. Both FULL and the scripted cohorts fill
     * every field; nothing here is derived from mechanism internals.
     */
    public class PresentationInput(
        public val action: SpikeAction,
        public val target: HabitatObject?,
        /** Overall activation in `[0, 1]`. */
        public val intensity: Long,
        /** Affective sign in `[-1, 1]`. */
        public val valence: Long,
        /** Monotonic logical tick, used only for the shared micro-movement phase. */
        public val tick: Long,
    )

    /** One rendered frame. This is the only thing the viewer ever receives. */
    public class ExpressionFrame(
        public val posture: Posture,
        public val expression: Expression,
        public val gazeTarget: HabitatObject?,
        public val motionAmplitude: Long,
        public val vocalizing: Boolean,
        public val microMovement: String,
        public val attentionObject: HabitatObject?,
    ) {
        public fun signature(): String =
            "${posture.name}|${expression.name}|${gazeTarget?.name ?: "-"}|" +
                "$motionAmplitude|$vocalizing|$microMovement|${attentionObject?.name ?: "-"}"
    }

    /** The frozen action-to-expression mapping. Pure, total, cohort-blind. */
    public fun frameFor(input: PresentationInput): ExpressionFrame {
        val posture = when (input.action) {
            SpikeAction.SLEEP, SpikeAction.REST -> Posture.LIE_DOWN
            SpikeAction.WITHDRAW -> Posture.LEAN_AWAY
            SpikeAction.APPROACH, SpikeAction.SEEK_INTERACTION,
            SpikeAction.RESPOND_TO_TOUCH, SpikeAction.EAT,
            -> Posture.LEAN_IN
            SpikeAction.PLAY -> Posture.PLAY_BOW
            SpikeAction.OBSERVE, SpikeAction.ORIENT -> Posture.CROUCH
            else -> Posture.STAND
        }

        val expression = when {
            input.action == SpikeAction.SLEEP -> Expression.TIRED
            input.action == SpikeAction.WITHDRAW -> Expression.WITHDRAWN
            input.valence <= NEGATIVE_WARY -> Expression.WARY
            input.intensity >= HIGH_INTENSITY && input.valence > FixedPoint.ZERO -> Expression.EXCITED
            input.valence >= POSITIVE_CONTENT -> Expression.CONTENT
            input.action == SpikeAction.OBSERVE || input.action == SpikeAction.ORIENT ->
                Expression.ALERT
            input.intensity <= LOW_INTENSITY -> Expression.NEUTRAL
            else -> Expression.ALERT
        }

        val amplitude = when (input.action) {
            SpikeAction.SLEEP -> FixedPoint.of(0L, 30_000L)
            SpikeAction.REST -> FixedPoint.of(0L, 90_000L)
            SpikeAction.IDLE_VARIATION -> FixedPoint.of(0L, 180_000L)
            SpikeAction.PLAY, SpikeAction.EXPLORE -> FixedPoint.of(0L, 850_000L)
            else -> FixedPoint.clamp(
                input.intensity,
                FixedPoint.of(0L, 200_000L),
                FixedPoint.of(0L, 950_000L),
                com.animusmachinae.dll17.core.math.ArithmeticContext.unattributed(),
            )
        }

        // The micro-movement phase advances on logical time and the action, so
        // idle variety is identical in richness for every cohort.
        val microIndex = Math.floorMod(
            input.tick / MICRO_MOVEMENT_HOLD_TICKS + input.action.ordinal * 3L,
            MICRO_MOVEMENTS.size.toLong(),
        ).toInt()

        return ExpressionFrame(
            posture = posture,
            expression = expression,
            gazeTarget = input.target,
            motionAmplitude = amplitude,
            vocalizing = input.action == SpikeAction.VOCALIZE,
            microMovement = MICRO_MOVEMENTS[microIndex],
            attentionObject = input.target,
        )
    }

    private const val MICRO_MOVEMENT_HOLD_TICKS = 4L
    private val HIGH_INTENSITY = FixedPoint.of(0L, 700_000L)
    private val LOW_INTENSITY = FixedPoint.of(0L, 250_000L)
    private val POSITIVE_CONTENT = FixedPoint.of(0L, 250_000L)
    private val NEGATIVE_WARY = -FixedPoint.of(0L, 250_000L)
}
