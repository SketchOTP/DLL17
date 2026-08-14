package com.animusmachinae.dll17.research.aliveness.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves the information barrier rather than describing it.
 *
 * Synthetic data only, and deliberately so: the seal has to be demonstrated
 * before a real pilot exists, because after one exists it is too late to find
 * out that the channel leaked.
 */
class BlindVariancePilotSealTest {

    private fun differences(n: Int, mean: Double, sd: Double): DoubleArray {
        val raw = DoubleArray(n) { Statistics.normalQuantile((it + 0.5) / n) }
        val rawSd = Statistics.sampleSd(raw)
        return DoubleArray(n) { mean + sd * raw[it] / rawSd }
    }

    private fun pilot(mean: Double, sd: Double, n: Int = 36): List<BlindVariancePilot.PilotRecord> {
        val d = differences(n, mean, sd)
        return (0 until n).map { i ->
            BlindVariancePilot.PilotRecord(
                participantId = "S-$i",
                fullScore = 50.0 + d[i] / 2.0,
                baselineScore = 50.0 - d[i] / 2.0,
            )
        }
    }

    @Test
    fun `the released type declares only the permitted fields`() {
        val declared = BlindVariancePilot.PilotRelease::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()
        assertEquals(setOf("pairedDifferenceSd", "protocolValid"), declared)
    }

    @Test
    fun `the released type exposes no accessor beyond the permitted fields`() {
        val accessors = BlindVariancePilot.PilotRelease::class.java.declaredMethods
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()
        assertEquals(setOf("getPairedDifferenceSd", "getProtocolValid", "render"), accessors)
    }

    @Test
    fun `two pilots with opposite directions release byte identical output`() {
        val favourable = BlindVariancePilot.release(pilot(+22.0, 14.0))
        val unfavourable = BlindVariancePilot.release(pilot(-22.0, 14.0))
        assertEquals(favourable.pairedDifferenceSd, unfavourable.pairedDifferenceSd, 1e-12)
        assertEquals(favourable.render(), unfavourable.render())
    }

    @Test
    fun `the released SD is the sample SD of the paired differences`() {
        val records = pilot(7.0, 11.0)
        val expected = Statistics.sampleSd(
            DoubleArray(records.size) { records[it].fullScore!! - records[it].baselineScore!! },
        )
        assertEquals(expected, BlindVariancePilot.release(records).pairedDifferenceSd, 1e-12)
    }

    @Test
    fun `a pilot short of the registered pair count is not protocol valid`() {
        assertTrue(BlindVariancePilot.release(pilot(5.0, 12.0, 36)).protocolValid)
        assertFalse(BlindVariancePilot.release(pilot(5.0, 12.0, 35)).protocolValid)
    }

    @Test
    fun `unusable sessions are dropped before the SD and can invalidate the pilot`() {
        val usable = pilot(5.0, 12.0, 36)
        val withFailure = usable.dropLast(1) + BlindVariancePilot.PilotRecord(
            "S-fail", 80.0, 20.0, technicalFailure = true,
        )
        val release = BlindVariancePilot.release(withFailure)
        assertFalse(release.protocolValid, "35 analysable pairs is short of the registered 36")
        // The failed session contributed a 60-point difference; if it had reached
        // the estimator the SD would have moved sharply.
        assertEquals(
            Statistics.sampleSd(
                DoubleArray(35) { usable[it].fullScore!! - usable[it].baselineScore!! },
            ),
            release.pairedDifferenceSd,
            1e-12,
        )
    }

    @Test
    fun `the registered pilot size is the smallest one the frozen inflation factor covers`() {
        // The architect froze a 1.25 inflation factor on the pilot SD. That is the
        // one-sided 95% upper confidence bound on a standard deviation at 35
        // degrees of freedom — exactly 36 pairs. One pair fewer and the frozen
        // factor no longer covers the bound, which is why a short pilot is
        // reported invalid rather than merely noted.
        fun upperBoundFactor(pairs: Int): Double {
            val df = (pairs - 1).toDouble()
            return StrictMath.sqrt(df / Statistics.chiSquareQuantile(0.05, df))
        }
        val atRegistered = upperBoundFactor(A001StudyContract.VARIANCE_PILOT_PARTICIPANTS)
        val atOneFewer = upperBoundFactor(A001StudyContract.VARIANCE_PILOT_PARTICIPANTS - 1)
        assertTrue(
            atRegistered <= A001StudyContract.PILOT_SD_INFLATION,
            "at 36 pairs the bound is $atRegistered, above the frozen 1.25",
        )
        assertTrue(
            atOneFewer > A001StudyContract.PILOT_SD_INFLATION,
            "at 35 pairs the bound is $atOneFewer, which 1.25 would still cover",
        )
    }

    @Test
    fun `the release names what it withholds`() {
        val rendered = BlindVariancePilot.release(pilot(5.0, 12.0)).render()
        for (withheld in listOf("mean difference", "sign", "cohort means", "raw recordings")) {
            assertTrue(rendered.contains(withheld), "release does not name $withheld as withheld")
        }
        assertFalse(rendered.contains("mean="), "the release must not carry a mean")
    }
}
