package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.crypto.RandomDomainRegistry
import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * The canonical reducer.
 *
 * It is a pure function of `(snapshot, event)`. That is the property everything
 * else in R001 rests on: `reduce(s0, orderedEvents)` can only equal
 * `replay(s0, journal)` byte for byte if folding an event depends on nothing
 * outside its two arguments — no wall clock, no ambient state, no iteration
 * order, no floating point.
 *
 * The reducer is single-threaded by invariant INV-0002. Nothing here is
 * synchronized, because the correct fix for concurrent access is not to make the
 * reducer thread-safe but to stop calling it from two threads.
 */
public object CanonicalReducer {

    /** Canonical variable IDs for saturation attribution. Immutable, never reused. */
    public const val VAR_NUMERIC_A: Int = 1
    public const val VAR_NUMERIC_B: Int = 2
    public const val VAR_MATERIAL_UNITS: Int = 3
    public const val VAR_LOGICAL_TIME: Int = 4

    /**
     * Bound for the quantized random draw: a fixed-point value in `[0.0, 1.0]`
     * inclusive. Draws are quantized before they touch canonical state because a
     * raw 64-bit draw folded into a fixed-point accumulator would saturate almost
     * immediately, and saturation inside a normal operating range is a
     * qualification failure rather than a curiosity.
     */
    public const val RANDOM_DRAW_BOUND: Int = 1_000_001

    public fun reduce(
        snapshot: CanonicalSnapshot,
        event: CanonicalEvent,
        ctx: ArithmeticContext,
    ): CanonicalSnapshot {
        val advanced = when (event.type) {
            CanonicalEventType.ADVANCE_TIME -> {
                if (event.operandA < 0L) {
                    throw IllegalArgumentException(
                        "ADVANCE_TIME operand must not be negative; canonical logical time " +
                            "never runs backwards (got ${event.operandA})",
                    )
                }
                ctx.forVariable(VAR_LOGICAL_TIME)
                snapshot.copy(logicalTime = FixedPoint.satAdd(snapshot.logicalTime, event.operandA, ctx))
            }

            CanonicalEventType.APPLY_DELTA -> {
                ctx.forVariable(VAR_NUMERIC_A)
                snapshot.copy(numericA = FixedPoint.satAdd(snapshot.numericA, event.operandA, ctx))
            }

            CanonicalEventType.APPLY_DECAY -> {
                ctx.forVariable(VAR_NUMERIC_B)
                snapshot.copy(numericB = FixedPoint.satDecay(snapshot.numericB, event.operandA, ctx))
            }

            CanonicalEventType.APPLY_INTERPOLATION -> {
                ctx.forVariable(VAR_NUMERIC_A)
                snapshot.copy(
                    numericA = FixedPoint.satInterpolate(
                        snapshot.numericA,
                        event.operandA,
                        event.operandB,
                        ctx,
                    ),
                )
            }

            CanonicalEventType.DRAW_RANDOM -> {
                val domainId = RandomDomainRegistry.require(event.operandA.toInt())
                val stream = snapshot.substreamFor(domainId).copy()
                val drawn = stream.nextIntBelow(RANDOM_DRAW_BOUND).toLong()
                ctx.forVariable(VAR_NUMERIC_B)
                snapshot
                    .withSubstream(stream)
                    .copy(numericB = FixedPoint.satAdd(snapshot.numericB, drawn, ctx))
            }

            CanonicalEventType.MATERIAL_INTERACTION -> {
                ctx.forVariable(VAR_MATERIAL_UNITS)
                snapshot.copy(
                    materialUnits = FixedPoint.satAdd(snapshot.materialUnits, event.operandA, ctx),
                )
            }
        }

        return advanced.copy(lastCommitSequence = event.logicalEventId)
    }

    /** Folds an ordered event sequence. Order is the caller's contract, not a hint. */
    public fun reduceAll(
        snapshot: CanonicalSnapshot,
        events: List<CanonicalEvent>,
        ctx: ArithmeticContext,
    ): CanonicalSnapshot {
        var current = snapshot
        for (event in events) {
            ctx.logicalTime = current.logicalTime
            current = reduce(current, event, ctx)
        }
        return current
    }
}
