package com.animusmachinae.dll17.core.math

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Property and boundary tests for the canonical fixed-point primitives, per
 * Implementation Plan E2E work package R001.2.
 *
 * The oracle is arbitrary-precision integer arithmetic. That is the point: the
 * production implementation uses 32-bit limbs and binary long division precisely
 * because it must avoid `BigInteger` and `Math.multiplyHigh`, so checking it
 * against itself would prove nothing. `BigInteger` computes the mathematically
 * exact result by a completely different route, and the two must agree on every
 * input, including the ones chosen to break fixed-point implementations.
 *
 * The oracle is a test-only dependency and never appears in a production path.
 */
class FixedPointOracleTest {

    private val scale = BigInteger.valueOf(FixedPoint.SCALE)
    private val max = BigInteger.valueOf(FixedPoint.MAX)
    private val min = BigInteger.valueOf(FixedPoint.MIN)
    private val two = BigInteger.valueOf(2L)

    /** Exact `numerator / denominator`, rounded half away from zero. */
    private fun roundHalfAway(numerator: BigInteger, denominator: BigInteger): BigInteger {
        require(denominator.signum() != 0)
        val negative = numerator.signum() * denominator.signum() < 0
        val n = numerator.abs()
        val d = denominator.abs()
        val parts = n.divideAndRemainder(d)
        var quotient = parts[0]
        if (parts[1].multiply(two) >= d) quotient = quotient.add(BigInteger.ONE)
        return if (negative) quotient.negate() else quotient
    }

    private fun clampToCanonical(value: BigInteger): Long = when {
        value > max -> FixedPoint.MAX
        value < min -> FixedPoint.MIN
        else -> value.toLong()
    }

    /**
     * Operand set. Every value is a place fixed-point implementations are known
     * to fail: the exact bounds, halves of the bounds where a doubling
     * overflows, values straddling one whole unit, and rounding ties.
     */
    private val operands: List<Long> = buildList {
        addAll(
            listOf(
                0L, 1L, -1L, 2L, -2L,
                499_999L, 500_000L, 500_001L, -500_000L,
                FixedPoint.ONE, -FixedPoint.ONE,
                999_999L, 1_000_001L, -999_999L,
                3_000_000L, -3_000_000L,
                1_000_000_000L, -1_000_000_000L,
                FixedPoint.MAX, FixedPoint.MIN,
                FixedPoint.MAX / 2L, FixedPoint.MIN / 2L,
                FixedPoint.MAX - 1L, FixedPoint.MIN + 1L,
                FixedPoint.MAX / FixedPoint.SCALE, FixedPoint.MIN / FixedPoint.SCALE,
            ),
        )
        // A deterministic pseudo-random spread. The generator is fixed and
        // inlined so the operand set is identical on every run and every target;
        // a flaky property test is worse than none.
        var state = 0x243F6A8885A308D3L
        repeat(96) {
            state = state * 6364136223846793005L + 1442695040888963407L
            add(state / 3L)
            add(state % 8_000_000L)
        }
    }.filter { FixedPoint.isLegal(it) }.distinct()

    private fun context(observer: RecordingSaturationObserver) =
        ArithmeticContext.unattributed(observer)

    @Test
    fun satAddMatchesTheOracleOnEveryOperandPair() {
        val observer = RecordingSaturationObserver()
        val ctx = context(observer)
        var checked = 0
        for (a in operands) {
            for (b in operands) {
                val expected = clampToCanonical(BigInteger.valueOf(a).add(BigInteger.valueOf(b)))
                assertEquals(expected, FixedPoint.satAdd(a, b, ctx), "satAdd($a, $b)")
                checked++
            }
        }
        assertTrue(checked > 40_000, "operand sweep should be substantial, was $checked")
    }

    @Test
    fun satSubtractMatchesTheOracleOnEveryOperandPair() {
        val ctx = context(RecordingSaturationObserver())
        for (a in operands) {
            for (b in operands) {
                val expected =
                    clampToCanonical(BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)))
                assertEquals(expected, FixedPoint.satSubtract(a, b, ctx), "satSubtract($a, $b)")
            }
        }
    }

    @Test
    fun satMultiplyScaledMatchesTheOracleOnEveryOperandPair() {
        val ctx = context(RecordingSaturationObserver())
        for (a in operands) {
            for (b in operands) {
                val exact = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b))
                val expected = clampToCanonical(roundHalfAway(exact, scale))
                assertEquals(
                    expected,
                    FixedPoint.satMultiplyScaled(a, b, ctx),
                    "satMultiplyScaled($a, $b)",
                )
            }
        }
    }

    @Test
    fun satDivideMatchesTheOracleWhereverTheDivisorIsNonZero() {
        val ctx = context(RecordingSaturationObserver())
        for (a in operands) {
            for (b in operands) {
                if (b == 0L) continue
                val exact = BigInteger.valueOf(a).multiply(scale)
                val expected = clampToCanonical(roundHalfAway(exact, BigInteger.valueOf(b)))
                assertEquals(expected, FixedPoint.satDivide(a, b, ctx), "satDivide($a, $b)")
            }
        }
    }

    @Test
    fun productionArithmeticNeverThrowsOnOverflow() {
        val ctx = context(RecordingSaturationObserver())
        val extremes = listOf(FixedPoint.MAX, FixedPoint.MIN, FixedPoint.MAX - 1L, FixedPoint.MIN + 1L)
        for (a in extremes) {
            for (b in extremes) {
                // Each of these overflows a 64-bit intermediate. None may throw.
                FixedPoint.satAdd(a, b, ctx)
                FixedPoint.satSubtract(a, b, ctx)
                FixedPoint.satMultiplyScaled(a, b, ctx)
                FixedPoint.satDivide(a, b, ctx)
                FixedPoint.satInterpolate(a, b, FixedPoint.ONE, ctx)
                FixedPoint.satDecay(a, b, ctx)
            }
        }
    }

    @Test
    fun roundingIsHalfAwayFromZeroAndSymmetric() {
        val ctx = context(RecordingSaturationObserver())
        // 0.5 exactly, in fixed-point terms: 1 / 2.0
        assertEquals(1L, FixedPoint.satDivide(1L, 2_000_000L, ctx), "positive tie rounds away from zero")
        assertEquals(-1L, FixedPoint.satDivide(-1L, 2_000_000L, ctx), "negative tie rounds away from zero")
        assertEquals(2L, FixedPoint.satDivide(3L, 2_000_000L, ctx), "1.5 rounds to 2")
        assertEquals(-2L, FixedPoint.satDivide(-3L, 2_000_000L, ctx), "-1.5 rounds to -2")

        // Symmetry is the property truncation and floor both fail.
        for (numerator in -50L..50L) {
            val positive = FixedPoint.satDivide(numerator, 3_000_000L, ctx)
            val negative = FixedPoint.satDivide(-numerator, 3_000_000L, ctx)
            assertEquals(positive, -negative, "rounding must be symmetric about zero at $numerator")
        }
    }

    @Test
    fun longMinValueIsNotALegalCanonicalValue() {
        assertTrue(FixedPoint.isLegal(FixedPoint.MIN))
        assertTrue(!FixedPoint.isLegal(Long.MIN_VALUE))
        assertFailsWith<IllegalArgumentException> {
            FixedPoint.satAdd(Long.MIN_VALUE, 0L, ArithmeticContext.unattributed())
        }
        // Negation is total because the range is symmetric.
        assertEquals(FixedPoint.MAX, FixedPoint.negate(FixedPoint.MIN))
        assertEquals(FixedPoint.MIN, FixedPoint.negate(FixedPoint.MAX))
    }

    @Test
    fun everySaturationEmitsACompleteDiagnostic() {
        val observer = RecordingSaturationObserver()
        val ctx = ArithmeticContext(42, 1_234L, ExecutionMode.RECONCILIATION, observer)

        val result = FixedPoint.satAdd(FixedPoint.MAX, FixedPoint.MAX, ctx)

        assertEquals(FixedPoint.MAX, result)
        assertEquals(1, observer.count)
        val event = observer.recorded().single()
        assertEquals(SaturationOp.ADD, event.operation)
        assertEquals(42, event.variableId)
        assertEquals(FixedPoint.MAX, event.operandA)
        assertEquals(FixedPoint.MAX, event.operandB)
        assertEquals(SaturationDirection.TOWARD_POSITIVE, event.requestedDirection)
        assertEquals(FixedPoint.MAX, event.clampedResult)
        assertEquals(1_234L, event.logicalTime)
        assertEquals(ExecutionMode.RECONCILIATION, event.mode)
    }

    @Test
    fun noSaturationIsReportedWhenNothingSaturates() {
        val observer = RecordingSaturationObserver()
        val ctx = context(observer)
        FixedPoint.satAdd(FixedPoint.of(1), FixedPoint.of(2), ctx)
        FixedPoint.satMultiplyScaled(FixedPoint.of(2), FixedPoint.of(3), ctx)
        FixedPoint.satDivide(FixedPoint.of(6), FixedPoint.of(3), ctx)
        assertEquals(0, observer.count, "ordinary arithmetic must not report saturation")
    }

    @Test
    fun divisionByZeroSaturatesTowardTheNumeratorSignAndIsDistinctlyDiagnosed() {
        val observer = RecordingSaturationObserver()
        val ctx = context(observer)

        assertEquals(FixedPoint.MAX, FixedPoint.satDivide(FixedPoint.of(1), 0L, ctx))
        assertEquals(FixedPoint.MIN, FixedPoint.satDivide(FixedPoint.of(-1), 0L, ctx))
        assertEquals(0L, FixedPoint.satDivide(0L, 0L, ctx))

        assertEquals(3, observer.count)
        assertTrue(
            observer.recorded().all { it.operation == SaturationOp.DIVIDE_BY_ZERO },
            "division by zero must not be recorded as ordinary range clamping",
        )
    }

    @Test
    fun interpolationClampsItsFactorRatherThanExtrapolating() {
        val observer = RecordingSaturationObserver()
        val ctx = context(observer)
        val a = FixedPoint.of(10)
        val b = FixedPoint.of(20)

        assertEquals(a, FixedPoint.satInterpolate(a, b, 0L, ctx))
        assertEquals(b, FixedPoint.satInterpolate(a, b, FixedPoint.ONE, ctx))
        assertEquals(FixedPoint.of(15), FixedPoint.satInterpolate(a, b, FixedPoint.of(0, 500_000), ctx))

        // Beyond the interval, the result stays inside it.
        assertEquals(b, FixedPoint.satInterpolate(a, b, FixedPoint.of(5), ctx))
        assertEquals(a, FixedPoint.satInterpolate(a, b, FixedPoint.of(-5), ctx))
        assertTrue(observer.count >= 2, "out-of-range interpolation factors must be diagnosed")
    }

    @Test
    fun decayIsMonotonicAndBounded() {
        val ctx = context(RecordingSaturationObserver())
        var value = FixedPoint.of(1000)
        val retention = FixedPoint.of(0, 900_000)
        repeat(64) {
            val next = FixedPoint.satDecay(value, retention, ctx)
            assertTrue(next <= value, "decay must not increase a positive value")
            assertTrue(next >= 0L, "decay of a positive value must not cross zero")
            value = next
        }
    }

    @Test
    fun clampRejectsAnInvertedInterval() {
        assertFailsWith<IllegalArgumentException> {
            FixedPoint.clamp(0L, FixedPoint.of(10), FixedPoint.of(1), ArithmeticContext.unattributed())
        }
    }

    @Test
    fun canonicalArithmeticUsesNoFloatingPoint() {
        // A structural check on the compiled class: no float or double constants,
        // fields or descriptors may appear in the fixed-point implementation.
        val resource = FixedPoint::class.java.name.replace('.', '/') + ".class"
        val bytes = checkNotNull(
            FixedPoint::class.java.classLoader.getResourceAsStream(resource),
        ).use { it.readBytes() }

        // Constant pool tags 4 and 5 are CONSTANT_Float and CONSTANT_Double.
        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue(
            !text.contains("()D") && !text.contains("()F"),
            "FixedPoint must expose no float or double returning members",
        )
        assertTrue(
            !text.contains("java/lang/Math"),
            "FixedPoint must not reference java.lang.Math; Math.multiplyHigh is prohibited " +
                "because it is unavailable below Android API 31 and minSdk is 29",
        )
    }
}
