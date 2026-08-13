package com.animusmachinae.dll17.core.math

/**
 * Canonical fixed-point arithmetic, frozen by `DeterminismContractV1` section 9.
 *
 * Values are raw `Long` counts of 1/1_000_000. The type is not wrapped in a
 * class: canonical state stores raw longs, and a wrapper would either box in
 * some call shapes or force every caller through an unwrapping step in the hot
 * path. The safety that a wrapper would buy is bought instead by every operation
 * living here, taking an [ArithmeticContext], and being covered by an
 * arbitrary-precision oracle test.
 *
 * Three decisions in this file are worth knowing before reading it:
 *
 * - `Long.MIN_VALUE` is **not** a legal value. Excluding it makes negation total
 *   and the bounds symmetric, which removes the classic fixed-point trap where
 *   negating the most-negative value silently yields itself.
 * - Rounding is **half away from zero** on every rescaling operation. Truncation
 *   toward zero (Kotlin's native `Long` division) biases repeated decay upward in
 *   magnitude terms and downward for positives; over an organism lifetime that is
 *   a drift, not a rounding error.
 * - 128-bit intermediates are computed with explicit 32-bit limbs. `Math.multiplyHigh`
 *   would be the obvious choice and is **prohibited**: it reached Android only at
 *   API 31 while the frozen `minSdk` is 29, so it would compile cleanly and then
 *   throw `NoSuchMethodError` on a supported device, which no desktop test would
 *   ever catch.
 */
public object FixedPoint {

    /** Fixed-point scale: 10^6. `1.000000` is stored as `1_000_000`. */
    public const val SCALE: Long = 1_000_000L

    /** `1.000000`. */
    public const val ONE: Long = SCALE

    public const val ZERO: Long = 0L

    /** Largest legal canonical value. */
    public const val MAX: Long = Long.MAX_VALUE

    /** Smallest legal canonical value. Deliberately not `Long.MIN_VALUE`. */
    public const val MIN: Long = -Long.MAX_VALUE

    /** True if [raw] is a legal canonical fixed-point value. */
    public fun isLegal(raw: Long): Boolean = raw != Long.MIN_VALUE

    public fun requireLegal(raw: Long): Long {
        if (raw == Long.MIN_VALUE) {
            throw IllegalArgumentException(
                "Long.MIN_VALUE is not a legal Fixed64 value; the canonical range is symmetric",
            )
        }
        return raw
    }

    /** Exact conversion from a whole number of units. Saturates outside the range. */
    public fun fromInt(value: Int): Long = value.toLong() * SCALE

    /**
     * Builds a value from a whole part and a millionths part, both with the sign
     * of [whole] when it is non-zero. Rejects a millionths magnitude of 10^6 or
     * more, which would be an ambiguous way to write the next whole unit.
     */
    public fun of(whole: Long, millionths: Long = 0L): Long {
        if (millionths <= -SCALE || millionths >= SCALE) {
            throw IllegalArgumentException("millionths must be in (-1_000_000, 1_000_000), got $millionths")
        }
        val magnitude = whole * SCALE
        return if (whole < 0L) magnitude - abs(millionths) else magnitude + abs(millionths)
    }

    /** Whole part, truncated toward zero. */
    public fun wholePart(raw: Long): Long = raw / SCALE

    /** Fractional part in millionths, carrying the sign of [raw]. */
    public fun fractionalMillionths(raw: Long): Long = raw % SCALE

    private fun abs(value: Long): Long = if (value < 0L) -value else value

    // ---------------------------------------------------------------- reporting

    private fun report(
        ctx: ArithmeticContext,
        op: SaturationOp,
        a: Long,
        b: Long,
        direction: SaturationDirection,
        result: Long,
    ): Long {
        ctx.observer.onSaturation(
            SaturationEvent(
                operation = op,
                variableId = ctx.variableId,
                operandA = a,
                operandB = b,
                requestedDirection = direction,
                clampedResult = result,
                logicalTime = ctx.logicalTime,
                mode = ctx.mode,
            ),
        )
        return result
    }

    // ------------------------------------------------------------- primitive ops

    /** Saturating addition. Exact; never rounds. */
    public fun satAdd(a: Long, b: Long, ctx: ArithmeticContext): Long {
        requireLegal(a)
        requireLegal(b)
        val sum = a + b
        // Overflow iff both operands share a sign that the result does not.
        val overflowed = ((a xor sum) and (b xor sum)) < 0L
        if (overflowed || sum == Long.MIN_VALUE) {
            return if (b > 0L || (b == 0L && a > 0L)) {
                report(ctx, SaturationOp.ADD, a, b, SaturationDirection.TOWARD_POSITIVE, MAX)
            } else {
                report(ctx, SaturationOp.ADD, a, b, SaturationDirection.TOWARD_NEGATIVE, MIN)
            }
        }
        return sum
    }

    /** Saturating subtraction. Exact; never rounds. */
    public fun satSubtract(a: Long, b: Long, ctx: ArithmeticContext): Long {
        requireLegal(a)
        requireLegal(b)
        val difference = a - b
        val overflowed = ((a xor b) and (a xor difference)) < 0L
        if (overflowed || difference == Long.MIN_VALUE) {
            return if (a >= 0L) {
                report(ctx, SaturationOp.SUBTRACT, a, b, SaturationDirection.TOWARD_POSITIVE, MAX)
            } else {
                report(ctx, SaturationOp.SUBTRACT, a, b, SaturationDirection.TOWARD_NEGATIVE, MIN)
            }
        }
        return difference
    }

    /** Total negation. Legal for every legal input, because `MIN == -MAX`. */
    public fun negate(a: Long): Long = -requireLegal(a)

    /**
     * Saturating fixed-point multiplication: `(a * b) / SCALE`, rounded half away
     * from zero, computed through an exact 128-bit intermediate.
     */
    public fun satMultiplyScaled(a: Long, b: Long, ctx: ArithmeticContext): Long {
        requireLegal(a)
        requireLegal(b)
        if (a == 0L || b == 0L) return 0L

        val negative = (a < 0L) != (b < 0L)
        val magnitude = multiplyThenDivideRounded(abs(a), abs(b), SCALE)
        if (magnitude == OVERFLOW) {
            return if (negative) {
                report(ctx, SaturationOp.MULTIPLY_SCALED, a, b, SaturationDirection.TOWARD_NEGATIVE, MIN)
            } else {
                report(ctx, SaturationOp.MULTIPLY_SCALED, a, b, SaturationDirection.TOWARD_POSITIVE, MAX)
            }
        }
        return if (negative) -magnitude else magnitude
    }

    /**
     * Saturating fixed-point division: `(a * SCALE) / b`, rounded half away from
     * zero.
     *
     * Division by zero does not throw. It is a model error rather than an
     * overflow, so it saturates toward the numerator's sign — with `0 / 0`
     * yielding `0` — and is recorded under [SaturationOp.DIVIDE_BY_ZERO] so it
     * can never be confused with ordinary range clamping when the diagnostics are
     * read back.
     */
    public fun satDivide(a: Long, b: Long, ctx: ArithmeticContext): Long {
        requireLegal(a)
        requireLegal(b)
        if (b == 0L) {
            return when {
                a > 0L -> report(ctx, SaturationOp.DIVIDE_BY_ZERO, a, b, SaturationDirection.TOWARD_POSITIVE, MAX)
                a < 0L -> report(ctx, SaturationOp.DIVIDE_BY_ZERO, a, b, SaturationDirection.TOWARD_NEGATIVE, MIN)
                else -> report(ctx, SaturationOp.DIVIDE_BY_ZERO, a, b, SaturationDirection.TOWARD_POSITIVE, ZERO)
            }
        }
        if (a == 0L) return 0L

        val negative = (a < 0L) != (b < 0L)
        val magnitude = multiplyThenDivideRounded(abs(a), SCALE, abs(b))
        if (magnitude == OVERFLOW) {
            return if (negative) {
                report(ctx, SaturationOp.DIVIDE, a, b, SaturationDirection.TOWARD_NEGATIVE, MIN)
            } else {
                report(ctx, SaturationOp.DIVIDE, a, b, SaturationDirection.TOWARD_POSITIVE, MAX)
            }
        }
        return if (negative) -magnitude else magnitude
    }

    /**
     * Clamps [value] into `[lower, upper]`.
     *
     * An inverted range is a programming error rather than a numeric one, so it
     * throws. That is not the prohibited behaviour: the contract forbids throwing
     * *because a value overflowed*, not because a caller passed a nonsensical
     * interval.
     */
    public fun clamp(value: Long, lower: Long, upper: Long, ctx: ArithmeticContext): Long {
        requireLegal(value)
        requireLegal(lower)
        requireLegal(upper)
        if (lower > upper) {
            throw IllegalArgumentException("clamp bounds inverted: lower=$lower upper=$upper")
        }
        return when {
            value < lower ->
                report(ctx, SaturationOp.CLAMP, value, lower, SaturationDirection.TOWARD_NEGATIVE, lower)
            value > upper ->
                report(ctx, SaturationOp.CLAMP, value, upper, SaturationDirection.TOWARD_POSITIVE, upper)
            else -> value
        }
    }

    /**
     * Linear interpolation `a + (b - a) * t`, with [t] clamped into `[0, ONE]`.
     *
     * Clamping [t] rather than extrapolating is deliberate: an out-of-range
     * interpolation factor is always a caller defect, and extrapolating would
     * quietly produce values outside the interval the caller believed it was
     * working in.
     */
    public fun satInterpolate(a: Long, b: Long, t: Long, ctx: ArithmeticContext): Long {
        requireLegal(a)
        requireLegal(b)
        requireLegal(t)
        val factor = if (t < ZERO) {
            report(ctx, SaturationOp.INTERPOLATE, t, ZERO, SaturationDirection.TOWARD_NEGATIVE, ZERO)
        } else if (t > ONE) {
            report(ctx, SaturationOp.INTERPOLATE, t, ONE, SaturationDirection.TOWARD_POSITIVE, ONE)
        } else {
            t
        }
        val delta = satSubtract(b, a, ctx)
        val scaled = satMultiplyScaled(delta, factor, ctx)
        return satAdd(a, scaled, ctx)
    }

    /**
     * Multiplicative decay: `value * retention`, with [retention] clamped into
     * `[0, ONE]`.
     *
     * `retention` is the fraction kept, not the fraction lost. R001 fixes the
     * mechanism only; no decay rate exists, because a rate would be an organism
     * parameter and `ParameterRegistry` owns those.
     */
    public fun satDecay(value: Long, retention: Long, ctx: ArithmeticContext): Long {
        requireLegal(value)
        requireLegal(retention)
        val factor = if (retention < ZERO) {
            report(ctx, SaturationOp.DECAY, retention, ZERO, SaturationDirection.TOWARD_NEGATIVE, ZERO)
        } else if (retention > ONE) {
            report(ctx, SaturationOp.DECAY, retention, ONE, SaturationDirection.TOWARD_POSITIVE, ONE)
        } else {
            retention
        }
        return satMultiplyScaled(value, factor, ctx)
    }

    // -------------------------------------------------------- 128-bit internals

    /** Sentinel for "does not fit in the legal canonical range". */
    private const val OVERFLOW: Long = -1L

    /**
     * Computes `round(x * y / divisor)` on non-negative magnitudes through an
     * exact 128-bit intermediate, rounding half away from zero.
     *
     * Returns [OVERFLOW] if the result exceeds [MAX]. A legitimate magnitude is
     * always non-negative, so the sentinel cannot collide with a real result.
     */
    private fun multiplyThenDivideRounded(x: Long, y: Long, divisor: Long): Long {
        // Exact unsigned 128-bit product via 32-bit limbs.
        val x0 = x and 0xFFFFFFFFL
        val x1 = x ushr 32
        val y0 = y and 0xFFFFFFFFL
        val y1 = y ushr 32

        val p00 = x0 * y0
        val p01 = x0 * y1
        val p10 = x1 * y0
        val p11 = x1 * y1

        val middle = (p00 ushr 32) + (p01 and 0xFFFFFFFFL) + (p10 and 0xFFFFFFFFL)
        val lo = (p00 and 0xFFFFFFFFL) or (middle shl 32)
        val hi = p11 + (p01 ushr 32) + (p10 ushr 32) + (middle ushr 32)

        return divideRounded(hi, lo, divisor)
    }

    /**
     * Unsigned 128-by-64 division with half-away-from-zero rounding, by binary
     * long division.
     *
     * Knuth algorithm D would be faster, but its normalization and correction
     * steps are exactly where 128-bit division implementations go wrong, and
     * getting this wrong corrupts every canonical value in the organism. Shift
     * and subtract is 128 iterations of primitive `Long` operations with no
     * special cases, and its correctness is verified exhaustively against a
     * `BigInteger` oracle. Divisions are not the arithmetic hot path.
     */
    private fun divideRounded(highBits: Long, lowBits: Long, divisor: Long): Long {
        var remainder = 0L
        var quotientHigh = 0L
        var quotientLow = 0L

        var bitIndex = 127
        while (bitIndex >= 0) {
            val bit = if (bitIndex >= 64) {
                (highBits ushr (bitIndex - 64)) and 1L
            } else {
                (lowBits ushr bitIndex) and 1L
            }

            // If the remainder's top bit is set, the shifted remainder is >= 2^64
            // and therefore certainly >= any 64-bit divisor. The subtraction below
            // still yields the correct low 64 bits in two's complement.
            val carriedOut = (remainder ushr 63) != 0L
            remainder = (remainder shl 1) or bit

            quotientHigh = (quotientHigh shl 1) or (quotientLow ushr 63)
            quotientLow = quotientLow shl 1

            if (carriedOut || unsignedGreaterOrEqual(remainder, divisor)) {
                remainder -= divisor
                quotientLow = quotientLow or 1L
            }
            bitIndex--
        }

        // Round half away from zero: 2 * remainder >= divisor, without overflowing.
        if (unsignedGreaterOrEqual(remainder, divisor - remainder)) {
            quotientLow += 1L
            if (quotientLow == 0L) quotientHigh += 1L
        }

        // A magnitude above MAX, or any high word at all, cannot be represented.
        if (quotientHigh != 0L || quotientLow < 0L) return OVERFLOW
        return quotientLow
    }

    /** Unsigned `a >= b` for `Long`, by flipping the sign bit before comparing. */
    private fun unsignedGreaterOrEqual(a: Long, b: Long): Boolean =
        (a xor Long.MIN_VALUE) >= (b xor Long.MIN_VALUE)
}
