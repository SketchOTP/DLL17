package com.animusmachinae.dll17.research.aliveness.analysis

/**
 * The statistical primitives the A001 analysis needs, implemented here rather
 * than pulled in as a dependency.
 *
 * Two reasons. The research track is allowed exactly one production dependency
 * (the frozen R001 fixed-point library) and no statistical library is on that
 * list; and a preregistered decision rule that turns on a p-value should be
 * readable end to end by the reviewer who has to adjudicate it.
 *
 * Everything here is `StrictMath`, so a value computed on one machine is the
 * value computed on another and a released analysis digest reproduces.
 *
 * These are ordinary textbook algorithms — Lentz's continued fraction for the
 * incomplete beta, the series/continued-fraction pair for the incomplete gamma,
 * and a one-dimensional integral for noncentral-t power. Each is checked against
 * published reference values in `StatisticsTest`.
 */
public object Statistics {

    // ------------------------------------------------------------ descriptive

    public fun mean(values: DoubleArray): Double {
        require(values.isNotEmpty()) { "mean of an empty sample" }
        var sum = 0.0
        for (v in values) sum += v
        return sum / values.size
    }

    /** Sample standard deviation, denominator n − 1. */
    public fun sampleSd(values: DoubleArray): Double {
        require(values.size >= 2) { "a standard deviation needs at least two observations" }
        val m = mean(values)
        var sum = 0.0
        for (v in values) {
            val d = v - m
            sum += d * d
        }
        return StrictMath.sqrt(sum / (values.size - 1))
    }

    // ---------------------------------------------------------- special funcs

    private val LANCZOS = doubleArrayOf(
        676.5203681218851, -1259.1392167224028, 771.32342877765313,
        -176.61502916214059, 12.507343278686905, -0.13857109526572012,
        9.9843695780195716e-6, 1.5056327351493116e-7,
    )

    /** Log of the gamma function. Lanczos, g = 7, n = 9. */
    public fun logGamma(x: Double): Double {
        require(x > 0.0) { "logGamma is defined here for x > 0 only" }
        if (x < 0.5) {
            // Reflection, so the series is only ever evaluated where it converges well.
            return StrictMath.log(Math.PI / StrictMath.sin(Math.PI * x)) - logGamma(1.0 - x)
        }
        val z = x - 1.0
        var a = 0.99999999999980993
        for (i in LANCZOS.indices) a += LANCZOS[i] / (z + i + 1.0)
        val t = z + LANCZOS.size - 0.5
        return 0.5 * StrictMath.log(2.0 * Math.PI) + (z + 0.5) * StrictMath.log(t) - t +
            StrictMath.log(a)
    }

    private const val EPSILON = 1.0e-15
    private const val TINY = 1.0e-300

    /** Regularized incomplete beta I_x(a, b). */
    public fun betaRegularized(x: Double, a: Double, b: Double): Double {
        if (x <= 0.0) return 0.0
        if (x >= 1.0) return 1.0
        val front = StrictMath.exp(
            logGamma(a + b) - logGamma(a) - logGamma(b) +
                a * StrictMath.log(x) + b * StrictMath.log(1.0 - x),
        )
        // The continued fraction converges quickly only on one side of the mode.
        return if (x < (a + 1.0) / (a + b + 2.0)) {
            front * betaContinuedFraction(x, a, b) / a
        } else {
            1.0 - betaRegularized(1.0 - x, b, a)
        }
    }

    private fun betaContinuedFraction(x: Double, a: Double, b: Double): Double {
        // Modified Lentz.
        val qab = a + b
        val qap = a + 1.0
        val qam = a - 1.0
        var c = 1.0
        var d = 1.0 - qab * x / qap
        if (StrictMath.abs(d) < TINY) d = TINY
        d = 1.0 / d
        var h = d
        for (m in 1..300) {
            val m2 = 2 * m
            var aa = m * (b - m) * x / ((qam + m2) * (a + m2))
            d = 1.0 + aa * d
            if (StrictMath.abs(d) < TINY) d = TINY
            c = 1.0 + aa / c
            if (StrictMath.abs(c) < TINY) c = TINY
            d = 1.0 / d
            h *= d * c
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
            d = 1.0 + aa * d
            if (StrictMath.abs(d) < TINY) d = TINY
            c = 1.0 + aa / c
            if (StrictMath.abs(c) < TINY) c = TINY
            d = 1.0 / d
            val del = d * c
            h *= del
            if (StrictMath.abs(del - 1.0) < EPSILON) break
        }
        return h
    }

    /** Regularized lower incomplete gamma P(a, x). */
    public fun gammaP(a: Double, x: Double): Double {
        require(a > 0.0)
        if (x <= 0.0) return 0.0
        return if (x < a + 1.0) gammaSeries(a, x) else 1.0 - gammaContinuedFraction(a, x)
    }

    private fun gammaSeries(a: Double, x: Double): Double {
        var ap = a
        var sum = 1.0 / a
        var del = sum
        for (n in 1..1000) {
            ap += 1.0
            del *= x / ap
            sum += del
            if (StrictMath.abs(del) < StrictMath.abs(sum) * EPSILON) break
        }
        return sum * StrictMath.exp(-x + a * StrictMath.log(x) - logGamma(a))
    }

    private fun gammaContinuedFraction(a: Double, x: Double): Double {
        var b = x + 1.0 - a
        var c = 1.0 / TINY
        var d = 1.0 / b
        var h = d
        for (i in 1..1000) {
            val an = -i * (i - a)
            b += 2.0
            d = an * d + b
            if (StrictMath.abs(d) < TINY) d = TINY
            c = b + an / c
            if (StrictMath.abs(c) < TINY) c = TINY
            d = 1.0 / d
            val del = d * c
            h *= del
            if (StrictMath.abs(del - 1.0) < EPSILON) break
        }
        return h * StrictMath.exp(-x + a * StrictMath.log(x) - logGamma(a))
    }

    // ------------------------------------------------------------ distributions

    /** Standard normal CDF. */
    public fun normalCdf(z: Double): Double {
        if (z == 0.0) return 0.5
        val p = gammaP(0.5, z * z / 2.0)
        return if (z > 0.0) 0.5 * (1.0 + p) else 0.5 * (1.0 - p)
    }

    /** Standard normal quantile, by bisection on the CDF. */
    public fun normalQuantile(p: Double): Double {
        require(p > 0.0 && p < 1.0)
        var lo = -40.0
        var hi = 40.0
        repeat(200) {
            val mid = 0.5 * (lo + hi)
            if (normalCdf(mid) < p) lo = mid else hi = mid
        }
        return 0.5 * (lo + hi)
    }

    /** Chi-square quantile with `df` degrees of freedom, by bisection. */
    public fun chiSquareQuantile(p: Double, df: Double): Double {
        require(p > 0.0 && p < 1.0 && df > 0.0)
        var lo = 0.0
        var hi = df * 100.0 + 100.0
        repeat(300) {
            val mid = 0.5 * (lo + hi)
            if (gammaP(df / 2.0, mid / 2.0) < p) lo = mid else hi = mid
        }
        return 0.5 * (lo + hi)
    }

    /** Student t CDF with `df` degrees of freedom. */
    public fun studentTCdf(t: Double, df: Double): Double {
        require(df > 0.0)
        val x = df / (df + t * t)
        val tail = 0.5 * betaRegularized(x, df / 2.0, 0.5)
        return if (t > 0.0) 1.0 - tail else tail
    }

    /** Two-sided p-value for a t statistic. */
    public fun studentTTwoSidedP(t: Double, df: Double): Double =
        2.0 * studentTCdf(-StrictMath.abs(t), df)

    /** Student t quantile: the `p`-th percentile, by bisection on the CDF. */
    public fun studentTQuantile(p: Double, df: Double): Double {
        require(p > 0.0 && p < 1.0)
        var lo = -1.0e3
        var hi = 1.0e3
        repeat(300) {
            val mid = 0.5 * (lo + hi)
            if (studentTCdf(mid, df) < p) lo = mid else hi = mid
        }
        return 0.5 * (lo + hi)
    }

    /**
     * Noncentral t CDF, by the standard one-dimensional mixture representation
     *
     *     P(T' <= t) = integral over s of Phi(t*s - ncp) * density of
     *                  s = sqrt(chi-square_df / df)
     *
     * evaluated by composite Simpson over a range that covers the chi-square
     * density to well beyond machine precision. Used for exact power rather than
     * a normal approximation, because the paired samples here are small enough
     * that the approximation and the truth differ by whole participants.
     */
    public fun noncentralTCdf(t: Double, df: Double, ncp: Double): Double {
        val sMean = 1.0
        val sSpread = 12.0 / StrictMath.sqrt(2.0 * df) + 12.0 / df
        val lo = StrictMath.max(1.0e-9, sMean - sSpread)
        val hi = sMean + sSpread
        val n = 2000
        val h = (hi - lo) / n
        var sum = 0.0
        for (i in 0..n) {
            val s = lo + i * h
            val weight = when {
                i == 0 || i == n -> 1.0
                i % 2 == 1 -> 4.0
                else -> 2.0
            }
            sum += weight * normalCdf(t * s - ncp) * sqrtChiSquareOverDfPdf(s, df)
        }
        val value = sum * h / 3.0
        return StrictMath.min(1.0, StrictMath.max(0.0, value))
    }

    /** Density of s = sqrt(chi-square_df / df). */
    private fun sqrtChiSquareOverDfPdf(s: Double, df: Double): Double {
        if (s <= 0.0) return 0.0
        // chi-square density at x = df*s^2, times |dx/ds| = 2*df*s.
        val x = df * s * s
        val logChi = (df / 2.0 - 1.0) * StrictMath.log(x) - x / 2.0 -
            (df / 2.0) * StrictMath.log(2.0) - logGamma(df / 2.0)
        return StrictMath.exp(logChi) * 2.0 * df * s
    }

    /**
     * Power of a two-sided one-sample (equivalently paired) t-test.
     *
     * `n` pairs, true mean difference `delta`, standard deviation `sd`.
     */
    public fun pairedTPower(n: Int, delta: Double, sd: Double, alpha: Double): Double {
        require(n >= 2)
        val df = (n - 1).toDouble()
        val ncp = delta / sd * StrictMath.sqrt(n.toDouble())
        val crit = studentTQuantile(1.0 - alpha / 2.0, df)
        val upper = 1.0 - noncentralTCdf(crit, df, ncp)
        val lower = noncentralTCdf(-crit, df, ncp)
        return upper + lower
    }

    /**
     * Smallest number of complete pairs whose power reaches `power`.
     *
     * Searched upward from 2 rather than solved, because the answer is an
     * integer and the monotone search cannot land on the wrong side of a
     * rounding rule.
     */
    public fun pairedTSampleSize(
        delta: Double,
        sd: Double,
        alpha: Double,
        power: Double,
        maxN: Int = 100_000,
    ): Int {
        require(delta > 0.0 && sd > 0.0)
        var n = 2
        while (n <= maxN) {
            if (pairedTPower(n, delta, sd, alpha) >= power) return n
            n++
        }
        throw IllegalStateException("no sample size below $maxN reaches power $power")
    }

    // ------------------------------------------------------------- binomial

    private fun binomialPmf(k: Int, n: Int, p: Double): Double {
        val logC = logGamma((n + 1).toDouble()) - logGamma((k + 1).toDouble()) -
            logGamma((n - k + 1).toDouble())
        return StrictMath.exp(
            logC + k * StrictMath.log(p) + (n - k) * StrictMath.log(1.0 - p),
        )
    }

    /**
     * Exact two-sided binomial test against p = 0.5, by the method of small
     * p-values: sum every outcome no more likely than the observed one.
     */
    public fun binomialTwoSidedP(successes: Int, trials: Int): Double {
        require(trials >= 0 && successes in 0..trials)
        if (trials == 0) return 1.0
        val observed = binomialPmf(successes, trials, 0.5)
        val tolerance = observed * (1.0 + 1.0e-9)
        var p = 0.0
        for (k in 0..trials) {
            val pk = binomialPmf(k, trials, 0.5)
            if (pk <= tolerance) p += pk
        }
        return StrictMath.min(1.0, p)
    }

    // ------------------------------------------------------------- formatting

    /**
     * Six-decimal fixed formatting, so evidence text is byte-reproducible.
     * `Locale.ROOT` because a decimal comma in an evidence file would change the
     * digest depending on who ran it.
     */
    public fun d6(value: Double): String =
        String.format(java.util.Locale.ROOT, "%.6f", value)

    /** Three-decimal fixed formatting for reported scores. */
    public fun d3(value: Double): String =
        String.format(java.util.Locale.ROOT, "%.3f", value)
}
