package com.animusmachinae.dll17.research.aliveness.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The statistics here decide whether a programme continues, so they are checked
 * against published reference values rather than against themselves.
 */
class StatisticsTest {

    private fun close(expected: Double, actual: Double, tolerance: Double, what: String) {
        assertTrue(
            StrictMath.abs(expected - actual) <= tolerance,
            "$what: expected $expected, got $actual",
        )
    }

    @Test
    fun `log gamma matches known values`() {
        // log(sqrt(pi))
        close(0.5723649429247001, Statistics.logGamma(0.5), 1e-12, "logGamma(0.5)")
        // log(4!) = log(24)
        close(StrictMath.log(24.0), Statistics.logGamma(5.0), 1e-12, "logGamma(5)")
    }

    @Test
    fun `the normal distribution matches published quantiles`() {
        close(0.9750021048517795, Statistics.normalCdf(1.96), 1e-12, "Phi(1.96)")
        close(0.5, Statistics.normalCdf(0.0), 1e-15, "Phi(0)")
        close(1.959963984540054, Statistics.normalQuantile(0.975), 1e-9, "z_0.975")
        close(0.8416212335729143, Statistics.normalQuantile(0.80), 1e-9, "z_0.80")
    }

    @Test
    fun `student t quantiles match the published table`() {
        close(2.2281388519649385, Statistics.studentTQuantile(0.975, 10.0), 1e-9, "t_0.975,10")
        close(2.0452296421327034, Statistics.studentTQuantile(0.975, 29.0), 1e-9, "t_0.975,29")
        close(12.706204736432095, Statistics.studentTQuantile(0.975, 1.0), 1e-8, "t_0.975,1")
        close(1.9599639845400545, Statistics.studentTQuantile(0.975, 1.0e9), 1e-5, "t -> z")
    }

    @Test
    fun `two sided t p values invert the quantiles`() {
        close(0.05, Statistics.studentTTwoSidedP(2.2281388519649385, 10.0), 1e-9, "p at t crit")
        close(1.0, Statistics.studentTTwoSidedP(0.0, 10.0), 1e-12, "p at t=0")
    }

    @Test
    fun `chi square quantiles match the published table`() {
        close(22.46504, Statistics.chiSquareQuantile(0.05, 35.0), 1e-4, "chi2_0.05,35")
        close(16.04707, Statistics.chiSquareQuantile(0.025, 29.0), 1e-4, "chi2_0.025,29")
    }

    @Test
    fun `paired t power reproduces published sample sizes`() {
        // Standard two-tailed one-sample t-test sample sizes at alpha=0.05,
        // power=0.80, for Cohen's d of 0.2, 0.5 and 0.8.
        assertEquals(199, Statistics.pairedTSampleSize(0.2, 1.0, 0.05, 0.80))
        assertEquals(34, Statistics.pairedTSampleSize(0.5, 1.0, 0.05, 0.80))
        assertEquals(15, Statistics.pairedTSampleSize(0.8, 1.0, 0.05, 0.80))
    }

    @Test
    fun `power is monotone in n and the returned size is the smallest sufficient one`() {
        val n = Statistics.pairedTSampleSize(0.5, 1.0, 0.05, 0.80)
        assertTrue(Statistics.pairedTPower(n, 0.5, 1.0, 0.05) >= 0.80)
        assertTrue(Statistics.pairedTPower(n - 1, 0.5, 1.0, 0.05) < 0.80)
    }

    @Test
    fun `the exact binomial test matches a hand computation`() {
        // 8 of 10, two-sided: 2 * (C(10,8) + C(10,9) + C(10,10)) / 2^10.
        close(112.0 / 1024.0, Statistics.binomialTwoSidedP(8, 10), 1e-12, "binom(8,10)")
        close(1.0, Statistics.binomialTwoSidedP(5, 10), 1e-12, "binom(5,10)")
        close(2.0 / 1024.0, Statistics.binomialTwoSidedP(10, 10), 1e-12, "binom(10,10)")
    }

    @Test
    fun `descriptive statistics use the n minus one denominator`() {
        val v = doubleArrayOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        close(5.0, Statistics.mean(v), 1e-12, "mean")
        close(StrictMath.sqrt(32.0 / 7.0), Statistics.sampleSd(v), 1e-12, "sd")
    }

    @Test
    fun `formatting is locale independent and fixed width`() {
        assertEquals("1.500000", Statistics.d6(1.5))
        assertEquals("-0.250", Statistics.d3(-0.25))
    }
}
