package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint
import com.animusmachinae.dll17.core.math.SaturationEvent
import com.animusmachinae.dll17.core.math.SaturationObserver
import com.animusmachinae.dll17.core.math.SaturationOp

/**
 * Thin ergonomic wrapper over the frozen R001 fixed-point library.
 *
 * The spike does not reimplement arithmetic; every operation below delegates to
 * `core-math` so that research behaviour uses the same rounding, saturation and
 * bound semantics R001 qualified. The wrapper exists only so mechanism code
 * reads as arithmetic rather than as a sequence of context-threading calls.
 */
public class Fx(public val ctx: ArithmeticContext) {

    public fun add(a: Long, b: Long): Long = FixedPoint.satAdd(a, b, ctx)

    public fun sub(a: Long, b: Long): Long = FixedPoint.satSubtract(a, b, ctx)

    /**
     * Multiplication with a fast path.
     *
     * The frozen library computes every product through an exact 128-bit
     * intermediate whose division is a bit-by-bit loop. That is the right choice
     * for canonical arithmetic and the wrong cost for a simulator that evaluates
     * tens of millions of products. When both magnitudes are small enough that
     * the product cannot overflow a `Long`, the same result is available
     * directly — same rounding, half away from zero — and everything else falls
     * through to the library. `SpikeNumericsTest` proves the two agree.
     */
    public fun mul(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        val absA = if (a < 0L) -a else a
        val absB = if (b < 0L) -b else b
        if (absA != Long.MIN_VALUE && absB != Long.MIN_VALUE && absA <= FAST_LIMIT / absB) {
            val magnitude = (absA * absB + FixedPoint.SCALE / 2L) / FixedPoint.SCALE
            return if ((a < 0L) != (b < 0L)) -magnitude else magnitude
        }
        return FixedPoint.satMultiplyScaled(a, b, ctx)
    }

    /** Division with the same fast path and the same fallback. */
    public fun div(a: Long, b: Long): Long {
        if (b == 0L || a == 0L) return FixedPoint.satDivide(a, b, ctx)
        val absA = if (a < 0L) -a else a
        val absB = if (b < 0L) -b else b
        if (absA != Long.MIN_VALUE && absA <= FAST_LIMIT / FixedPoint.SCALE) {
            val magnitude = (absA * FixedPoint.SCALE + absB / 2L) / absB
            return if ((a < 0L) != (b < 0L)) -magnitude else magnitude
        }
        return FixedPoint.satDivide(a, b, ctx)
    }

    public fun clamp(v: Long, lo: Long, hi: Long): Long = FixedPoint.clamp(v, lo, hi, ctx)

    public fun unit(v: Long): Long = FixedPoint.clamp(v, FixedPoint.ZERO, FixedPoint.ONE, ctx)

    public fun signed(v: Long): Long =
        FixedPoint.clamp(v, -FixedPoint.ONE, FixedPoint.ONE, ctx)

    public fun decay(v: Long, retention: Long): Long = FixedPoint.satDecay(v, retention, ctx)

    public fun lerp(a: Long, b: Long, t: Long): Long = FixedPoint.satInterpolate(a, b, t, ctx)

    /** Move `value` toward `target` by at most `step`. Used for slew limiting. */
    public fun approach(value: Long, target: Long, step: Long): Long = when {
        value < target -> minOf(target, add(value, step))
        value > target -> maxOf(target, sub(value, step))
        else -> value
    }

    public companion object {
        /**
         * Below this magnitude a product is exact in a `Long`. Chosen with head
         * room rather than at `Long.MAX_VALUE` so the guard itself cannot
         * overflow while being evaluated.
         */
        public const val FAST_LIMIT: Long = Long.MAX_VALUE / 4L

        public fun counting(): Pair<Fx, SaturationCounter> {
            val counter = SaturationCounter()
            return Fx(ArithmeticContext.unattributed(counter)) to counter
        }
    }
}

/**
 * Counts saturation events without retaining them.
 *
 * Long accelerated runs are the point of the spike, so retaining every event
 * would itself be unbounded growth. The count is the evidence: a mechanism set
 * that never saturates over a thousand virtual days has demonstrated numeric
 * stability, and a nonzero count is a finding rather than a crash.
 */
public class SaturationCounter : SaturationObserver {

    /**
     * Range clamps. These are the mechanism working: a drive pinned at its
     * bound clamps on every tick, and that is intended, not instability.
     */
    public var clampCount: Long = 0L
        private set

    /**
     * Arithmetic overflow. This is the number that matters. A nonzero value
     * means an intermediate result left the representable range, which is a
     * numeric-stability finding regardless of what the behaviour looked like.
     */
    public var overflowCount: Long = 0L
        private set

    public var firstOverflowOp: SaturationOp? = null
        private set

    override fun onSaturation(event: SaturationEvent) {
        if (event.operation == SaturationOp.CLAMP) {
            clampCount += 1L
        } else {
            if (overflowCount == 0L) firstOverflowOp = event.operation
            overflowCount += 1L
        }
    }

    public fun reset() {
        clampCount = 0L
        overflowCount = 0L
        firstOverflowOp = null
    }
}

/**
 * SplitMix64. Deterministic, seedable, and split into independent substreams so
 * a draw in one domain cannot shift the sequence observed by another.
 *
 * Only `SpikeRandomDomain.CURIOSITY_TIE_BREAK` may influence FULL's selection,
 * and only after every biological and learned-value term has been evaluated.
 */
public class SpikeRandom(seed: Long, domain: SpikeRandomDomain) {
    private var state: Long = mix(seed * 0x9E3779B97F4A7C15uL.toLong() + domain.domainId * 0x632BE59BD9B4E019L)

    public var drawCount: Long = 0L
        private set

    public fun nextLong(): Long {
        drawCount += 1L
        state += 0x9E3779B97F4A7C15uL.toLong()
        return mix(state)
    }

    /** Uniform in `[0, bound)`. Rejection-free modulo bias is acceptable in research code. */
    public fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        val v = nextLong() ushr 1
        return (v % bound.toLong()).toInt()
    }

    /** Uniform fixed-point value in `[0, 1)`. */
    public fun nextUnit(): Long = ((nextLong() ushr 1) % FixedPoint.SCALE)

    private companion object {
        fun mix(z0: Long): Long {
            var z = z0
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }
}

/**
 * Fixed-point triangle wave over a per-object phase accumulator.
 *
 * Canonical §9 requires integer triangle waves or versioned lookup tables and
 * forbids floating-point trigonometry. It also requires that phase, period and
 * frequency derive only from the immutable organism seed, stable object
 * identity, versioned domain IDs and monotonically advancing logical time —
 * context never enters the accumulator or its time input.
 */
public object CuriosityWave {

    /** Immutable phase function. Context is structurally absent from the signature. */
    public fun phase(seed: Long, objectOrdinal: Int, logicalTick: Long, periodTicks: Int): Long {
        val offset = phaseOffset(seed, objectOrdinal, periodTicks)
        val position = Math.floorMod(logicalTick + offset, periodTicks.toLong())
        return position * FixedPoint.SCALE / periodTicks
    }

    /** Triangle wave in `[-1, 1]`, evaluated by integer arithmetic only. */
    public fun triangle(phaseUnit: Long): Long {
        val p = Math.floorMod(phaseUnit, FixedPoint.SCALE)
        val quarter = FixedPoint.SCALE / 4L
        return when {
            p < quarter -> p * 4L
            p < 3L * quarter -> FixedPoint.SCALE * 2L - p * 4L
            else -> p * 4L - FixedPoint.SCALE * 4L
        }
    }

    /**
     * Period assignment: each object takes a period from the co-prime set, keyed
     * by stable identity and the immutable seed so two objects rarely share one.
     */
    public fun periodFor(
        seed: Long,
        objectOrdinal: Int,
        periods: IntArray = SpikeContract.CURIOSITY_PERIODS_TICKS,
    ): Int {
        val index = Math.floorMod(hash(seed, objectOrdinal), periods.size.toLong()).toInt()
        return periods[index]
    }

    private fun phaseOffset(seed: Long, objectOrdinal: Int, periodTicks: Int): Long =
        Math.floorMod(hash(seed, objectOrdinal + 977), periodTicks.toLong())

    private fun hash(seed: Long, ordinal: Int): Long {
        var z = seed * 0x9E3779B97F4A7C15uL.toLong() + ordinal * 0x7FEB352DL
        z = (z xor (z ushr 29)) * -0x40A7B892E31B1A47L
        z = (z xor (z ushr 32))
        return z and Long.MAX_VALUE
    }
}

/**
 * The jointly frozen curiosity/inspection parameterization.
 *
 * `CuriosityBalanceEnvelopeV1` binds anti-convergence and attribution to *one*
 * parameter set. Making the parameters an explicit value rather than global
 * constants is what allows the feasibility search to prove that the same
 * parameterization satisfied both requirements, instead of one configuration
 * quietly passing each test.
 */
public class CuriosityParameters(
    public val baseFloor: Long = SpikeContract.CURIOSITY_BASE_FLOOR,
    public val contextAmplitude: Long = SpikeContract.CURIOSITY_CONTEXT_AMPLITUDE,
    public val amplitudeSlewPerTick: Long = SpikeContract.CURIOSITY_AMPLITUDE_SLEW_PER_TICK,
    public val inhibitionDepth: Long = SpikeContract.INSPECTION_INHIBITION_DEPTH,
    public val inhibitionRetentionPerTick: Long = SpikeContract.INSPECTION_INHIBITION_RETENTION_PER_TICK,
    public val periods: IntArray = SpikeContract.CURIOSITY_PERIODS_TICKS,
) {
    /** Stable identity of this parameterization, recorded with every result. */
    public fun parameterHash(): String {
        var h = -0x340d631b7bdddcdbL
        fun mix(v: Long) {
            h = (h xor v) * 0x100000001B3L
        }
        mix(baseFloor); mix(contextAmplitude); mix(amplitudeSlewPerTick)
        mix(inhibitionDepth); mix(inhibitionRetentionPerTick)
        for (p in periods) mix(p.toLong())
        return java.lang.Long.toHexString(h)
    }

    public fun describe(): String =
        "baseFloor=$baseFloor amplitude=$contextAmplitude slew=$amplitudeSlewPerTick " +
            "inhibitionDepth=$inhibitionDepth inhibitionRetention=$inhibitionRetentionPerTick " +
            "periods=${periods.joinToString(",")}"

    public companion object {
        public val DEFAULT: CuriosityParameters = CuriosityParameters()
    }
}
