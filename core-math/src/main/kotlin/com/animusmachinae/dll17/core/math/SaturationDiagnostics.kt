package com.animusmachinae.dll17.core.math

/**
 * Saturation diagnostics, required by the canonical architecture's saturation
 * policy and by Implementation Plan E2E work package R001.2.
 *
 * Saturation is a survival guard: canonical arithmetic never throws because a
 * value overflowed, because an organism that crashes on an unexpected number is
 * worse than one that clamps and records evidence. But a clamp that nobody sees
 * is a silent model failure, so every clamp emits a record naming the operation,
 * both operands, the variable, the logical time, the requested direction, the
 * clamped result, and whether it happened in the foreground or during
 * reconciliation.
 *
 * Diagnostics are **noncanonical**. They never enter the canonical state hash and
 * never influence a reduction. Two runs that saturate identically produce
 * identical canonical bytes whether or not anybody was listening.
 */

/** The operation that saturated. Ordinals are immutable and never reused. */
public enum class SaturationOp(public val ordinal32: Int) {
    ADD(1),
    SUBTRACT(2),
    MULTIPLY_SCALED(3),
    DIVIDE(4),
    INTERPOLATE(5),
    DECAY(6),
    CLAMP(7),
    NEGATE(8),

    /**
     * Division by zero. Not an overflow, but it must not throw either: it
     * saturates toward the numerator's sign and is recorded under its own
     * operation so it can never be mistaken for ordinary range clamping.
     */
    DIVIDE_BY_ZERO(9),
}

/** Which bound the exact result ran past. */
public enum class SaturationDirection(public val ordinal32: Int) {
    TOWARD_POSITIVE(1),
    TOWARD_NEGATIVE(2),
}

/** Execution context, so reconciliation storms are distinguishable from foreground drift. */
public enum class ExecutionMode(public val ordinal32: Int) {
    FOREGROUND(1),
    RECONCILIATION(2),
}

/** One saturation record. */
public data class SaturationEvent(
    val operation: SaturationOp,
    val variableId: Int,
    val operandA: Long,
    val operandB: Long,
    val requestedDirection: SaturationDirection,
    val clampedResult: Long,
    val logicalTime: Long,
    val mode: ExecutionMode,
)

/** Sink for saturation records. */
public fun interface SaturationObserver {
    public fun onSaturation(event: SaturationEvent)
}

/** Discards records. Used where saturation is expected and already asserted. */
public object NoopSaturationObserver : SaturationObserver {
    override fun onSaturation(event: SaturationEvent) {
        // Intentionally empty.
    }
}

/** Collects records for tests, qualification evidence and diagnostics. */
public class RecordingSaturationObserver : SaturationObserver {
    private val events: MutableList<SaturationEvent> = ArrayList()

    override fun onSaturation(event: SaturationEvent) {
        events.add(event)
    }

    public fun recorded(): List<SaturationEvent> = events.toList()
    public val count: Int get() = events.size
    public fun clear() {
        events.clear()
    }
}

/**
 * Per-step arithmetic context.
 *
 * It is allocated once per reduction step rather than once per operation, so the
 * arithmetic hot path stays allocation-free while every record still carries the
 * variable and logical time the canonical architecture requires.
 */
public class ArithmeticContext(
    public var variableId: Int,
    public var logicalTime: Long,
    public val mode: ExecutionMode,
    public val observer: SaturationObserver,
) {
    public fun forVariable(id: Int): ArithmeticContext {
        variableId = id
        return this
    }

    public companion object {
        /**
         * Context for arithmetic that is not attributed to a canonical variable,
         * such as unit tests of the primitives themselves.
         */
        public fun unattributed(observer: SaturationObserver = NoopSaturationObserver): ArithmeticContext =
            ArithmeticContext(UNATTRIBUTED_VARIABLE, 0L, ExecutionMode.FOREGROUND, observer)

        public const val UNATTRIBUTED_VARIABLE: Int = 0
    }
}
