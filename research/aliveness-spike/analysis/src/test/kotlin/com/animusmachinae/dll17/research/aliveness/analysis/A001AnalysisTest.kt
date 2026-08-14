package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Synthetic fixtures only. Every dataset here is constructed to hit a specific
 * branch of the preregistered decision rule; none of it is evidence about the
 * organism, and none of it may ever be cited as such.
 */
class A001AnalysisTest {

    private fun pairs(
        prefix: String,
        comparator: Cohort,
        n: Int,
        mean: Double,
        sd: Double,
    ): List<A001Analysis.PairRecord> {
        val raw = DoubleArray(n) { Statistics.normalQuantile((it + 0.5) / n) }
        val rawSd = Statistics.sampleSd(raw)
        return (0 until n).map { i ->
            val d = mean + sd * raw[i] / rawSd
            A001Analysis.PairRecord(
                participantId = "$prefix-$i",
                comparator = comparator,
                fullScore = 50.0 + d / 2.0,
                comparatorScore = 50.0 - d / 2.0,
                forcedChoicePreferredFull = d > 0.0,
            )
        }
    }

    private fun family(prefix: String, means: List<Double>, sd: Double, n: Int) =
        A001StudyContract.ABLATION_FAMILY.mapIndexed { i, arm ->
            pairs("$prefix-a$i", arm, n, means[i], sd)
        }.flatten()

    // ------------------------------------------------------- primary endpoint

    @Test
    fun `a clear effect above the floor with a tight interval passes`() {
        val result = A001Analysis.analyze(
            pairs("p", Cohort.SCRIPTED_PET_BASELINE, 60, 16.0, 18.0) +
                family("p", listOf(14.0, 12.0, 2.0), 18.0, 60),
        )
        assertEquals(A001Analysis.PrimaryClassification.PASS, result.classification)
        assertTrue(result.primary.ciLow > 0.0)
        assertTrue(result.primary.meanDifference >= A001StudyContract.MINIMUM_PAIRED_DIFFERENCE)
        assertEquals("A001_ATTEMPT_1_PASS", result.attemptOutcome)
    }

    @Test
    fun `a statistically unambiguous but trivial effect fails the practical floor`() {
        val result = A001Analysis.analyze(
            pairs("s", Cohort.SCRIPTED_PET_BASELINE, 200, 2.5, 9.0) +
                family("s", listOf(1.0, 0.5, 0.2), 9.0, 60),
        )
        assertTrue(result.primary.ciLow > 0.0, "fixture must be statistically significant")
        assertTrue(result.primary.p < 0.05)
        assertEquals(
            A001Analysis.PrimaryClassification.FAIL_NOT_PRACTICALLY_MEANINGFUL,
            result.classification,
        )
        assertEquals("A001_ATTEMPT_1_FAIL", result.attemptOutcome)
    }

    @Test
    fun `a practically large but imprecise effect fails`() {
        val result = A001Analysis.analyze(
            pairs("w", Cohort.SCRIPTED_PET_BASELINE, 14, 12.0, 34.0) +
                family("w", listOf(3.0, 2.0, 1.0), 34.0, 14),
        )
        assertTrue(
            result.primary.meanDifference >= A001StudyContract.MINIMUM_PAIRED_DIFFERENCE,
            "fixture must clear the practical floor",
        )
        assertTrue(result.primary.ciLow <= 0.0, "fixture interval must contain zero")
        assertEquals(A001Analysis.PrimaryClassification.FAIL_IMPRECISE, result.classification)
    }

    @Test
    fun `an outright negative result fails`() {
        val result = A001Analysis.analyze(
            pairs("n", Cohort.SCRIPTED_PET_BASELINE, 60, -6.0, 16.0) +
                family("n", listOf(0.5, -1.0, 0.0), 16.0, 60),
        )
        assertEquals(
            A001Analysis.PrimaryClassification.FAIL_NULL_OR_NEGATIVE,
            result.classification,
        )
        assertTrue(result.primary.meanDifference < 0.0)
    }

    @Test
    fun `the forced choice cannot change the primary classification`() {
        val base = pairs("f", Cohort.SCRIPTED_PET_BASELINE, 60, 2.0, 20.0) +
            family("f", listOf(1.0, 1.0, 1.0), 20.0, 60)
        val overwhelming = base.map {
            if (it.comparator == Cohort.SCRIPTED_PET_BASELINE) {
                A001Analysis.PairRecord(
                    it.participantId, it.comparator, it.fullScore, it.comparatorScore,
                    forcedChoicePreferredFull = true,
                )
            } else {
                it
            }
        }
        val a = A001Analysis.analyze(base)
        val b = A001Analysis.analyze(overwhelming)
        assertEquals(a.classification, b.classification)
        assertNotEquals(a.secondary.preferredFull, b.secondary.preferredFull)
        assertTrue(b.secondary.p < 0.001, "the fixture's forced choice is unanimous")
        assertEquals("A001_ATTEMPT_1_FAIL", b.attemptOutcome)
    }

    // ------------------------------------------------------------- screening

    @Test
    fun `every exclusion reason is applied and nothing is imputed`() {
        val clean = pairs("c", Cohort.SCRIPTED_PET_BASELINE, 20, 14.0, 17.0)
        val dirty = listOf(
            A001Analysis.PairRecord("c-0", Cohort.SCRIPTED_PET_BASELINE, 80.0, 40.0),
            A001Analysis.PairRecord(
                "x1", Cohort.SCRIPTED_PET_BASELINE, 90.0, 20.0, priorNonScoredPool = true,
            ),
            A001Analysis.PairRecord(
                "x2", Cohort.SCRIPTED_PET_BASELINE, 88.0, 22.0, technicalFailure = true,
            ),
            A001Analysis.PairRecord(
                "x3", Cohort.SCRIPTED_PET_BASELINE, 85.0, 25.0, fullCompletedFraction = 0.4,
            ),
            A001Analysis.PairRecord("x4", Cohort.SCRIPTED_PET_BASELINE, null, 30.0),
            A001Analysis.PairRecord("x5", Cohort.SCRIPTED_PET_BASELINE, 140.0, 30.0),
        )
        val screened = A001Analysis.screen(clean + dirty)
        assertEquals(20, screened.included.size)
        assertEquals(6, screened.excluded.size)
        assertEquals(
            A001Analysis.ExclusionReason.entries.toSet(),
            screened.excluded.map { it.reason }.toSet(),
        )
    }

    @Test
    fun `identity and protocol violations outrank data quality in the screening order`() {
        // A pilot participant whose session also failed technically is reported
        // under the pool violation, which is the more serious finding.
        val screened = A001Analysis.screen(
            listOf(
                A001Analysis.PairRecord(
                    "z", Cohort.SCRIPTED_PET_BASELINE, null, null,
                    technicalFailure = true, priorNonScoredPool = true,
                ),
            ),
        )
        assertEquals(
            A001Analysis.ExclusionReason.PRIOR_NON_SCORED_POOL,
            screened.excluded.single().reason,
        )
    }

    // ------------------------------------------------------------- ablations

    @Test
    fun `Holm correction is applied across the preregistered three arm family`() {
        assertEquals(3, A001StudyContract.ABLATION_FAMILY.size)
        val result = A001Analysis.analyze(
            pairs("h", Cohort.SCRIPTED_PET_BASELINE, 60, 16.0, 18.0) +
                family("h", listOf(14.0, 12.0, 2.0), 18.0, 60),
        )
        assertEquals(3, result.ablations.size)
        for (a in result.ablations) {
            assertTrue(a.holmAdjustedP >= a.rawP, "${a.comparator.cohortId} adjusted below raw")
        }
        // Two strong arms survive; the near-null arm does not.
        assertEquals(
            2,
            result.ablations.count { it.significant },
            "fixture is built so exactly two arms survive correction",
        )
        assertTrue(result.ablations.last().holmAdjustedP > A001StudyContract.ABLATION_FAMILY_ALPHA)
    }

    @Test
    fun `Holm adjusted p values are monotone in the raw p order`() {
        val result = A001Analysis.analyze(
            pairs("m", Cohort.SCRIPTED_PET_BASELINE, 40, 12.0, 18.0) +
                family("m", listOf(6.0, 4.0, 2.0), 18.0, 40),
        )
        val ordered = result.ablations.sortedBy { it.rawP }
        for (i in 1 until ordered.size) {
            assertTrue(
                ordered[i].holmAdjustedP >= ordered[i - 1].holmAdjustedP,
                "Holm adjusted p values must not decrease as raw p increases",
            )
        }
    }

    @Test
    fun `a borderline arm is significant uncorrected and not significant after correction`() {
        // Built so the smallest raw p sits between alpha and alpha/3.
        val records = pairs("b", Cohort.SCRIPTED_PET_BASELINE, 40, 12.0, 18.0) +
            family("b", listOf(6.4, 0.2, 0.1), 18.0, 40)
        val ablations = A001Analysis.ablations(A001Analysis.screen(records).included)
        val strongest = ablations.minByOrNull { it.rawP }!!
        assertTrue(
            strongest.rawP < A001StudyContract.ABLATION_FAMILY_ALPHA,
            "fixture must be significant before correction (raw p=${strongest.rawP})",
        )
        assertTrue(
            strongest.holmAdjustedP > A001StudyContract.ABLATION_FAMILY_ALPHA,
            "fixture must lose significance after correction (holm p=${strongest.holmAdjustedP})",
        )
        assertTrue(ablations.none { it.significant })
    }

    @Test
    fun `mechanism retention is reported as earned or not demonstrated`() {
        val result = A001Analysis.analyze(
            pairs("v", Cohort.SCRIPTED_PET_BASELINE, 60, 16.0, 18.0) +
                family("v", listOf(14.0, 12.0, 2.0), 18.0, 60),
        )
        val rendered = result.render()
        assertTrue(rendered.contains("MECHANISM_CONTRIBUTION_DEMONSTRATED"))
        assertTrue(rendered.contains("MECHANISM_CONTRIBUTION_NOT_DEMONSTRATED"))
        assertTrue(rendered.contains("FULL-outcome-uncertainty"))
    }

    // --------------------------------------------------------------- rendering

    @Test
    fun `the rendered report states the decision rule and both of its halves`() {
        val rendered = A001Analysis.analyze(
            pairs("r", Cohort.SCRIPTED_PET_BASELINE, 30, 16.0, 18.0) +
                family("r", listOf(8.0, 6.0, 1.0), 18.0, 30),
        ).render()
        assertTrue(rendered.contains("rule: mean >= 10.000 AND ciLow > 0"))
        assertTrue(rendered.contains("practicalFloorMet="))
        assertTrue(rendered.contains("intervalExcludesZero="))
        assertTrue(rendered.contains("cannot rescue a failed primary"))
    }
}
