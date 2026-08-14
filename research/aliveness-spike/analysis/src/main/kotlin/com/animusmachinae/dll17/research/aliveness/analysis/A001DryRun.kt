package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort

/**
 * Synthetic dry run of the whole A001 activation package.
 *
 * **Every number this produces is synthetic.** No human has rated anything. The
 * datasets below are constructed to hit specific decision paths — a pass, each
 * distinct way of failing, an ablation family that survives correction and one
 * that does not, and a pilot whose release cannot betray its direction. They
 * exist to show that the pipeline behaves as preregistered *before* it can be
 * tempted by a real result.
 *
 * The output is marked `SYNTHETIC` on every scenario and is filed under
 * `research/aliveness-spike/evidence/`, never under `qualification/`. It is
 * engineering evidence that the analysis works. It is not scientific evidence
 * about the organism, and it must never be cited as any.
 */
public object A001DryRun {

    public const val DRY_RUN_ID: String = "A001DryRunV1"

    @JvmStatic
    public fun main(args: Array<String>) {
        println(render())
    }

    /**
     * Deterministic synthetic paired differences with an approximately known
     * mean and spread.
     *
     * Standard-normal quantiles at the midpoints of `n` equal-probability
     * intervals, rather than a random draw: the fixture is then a fixed property
     * of `(n, mean, sd)` and a reader can reproduce it exactly.
     */
    private fun differences(n: Int, mean: Double, sd: Double): DoubleArray {
        val raw = DoubleArray(n) { Statistics.normalQuantile((it + 0.5) / n) }
        val rawSd = Statistics.sampleSd(raw)
        return DoubleArray(n) { mean + sd * raw[it] / rawSd }
    }

    /** Turns paired differences into two in-range scores around a common midpoint. */
    private fun records(
        prefix: String,
        comparator: Cohort,
        n: Int,
        mean: Double,
        sd: Double,
        forcedChoice: Boolean = true,
    ): List<A001Analysis.PairRecord> {
        val d = differences(n, mean, sd)
        return (0 until n).map { i ->
            val full = clamp(50.0 + d[i] / 2.0)
            val comp = clamp(50.0 - d[i] / 2.0)
            A001Analysis.PairRecord(
                participantId = "$prefix-${(i + 1).toString().padStart(3, '0')}",
                comparator = comparator,
                fullScore = full,
                comparatorScore = comp,
                forcedChoicePreferredFull = if (forcedChoice) full > comp else null,
            )
        }
    }

    private fun clamp(v: Double): Double =
        StrictMath.min(A001StudyContract.SCORE_MAX, StrictMath.max(A001StudyContract.SCORE_MIN, v))

    /** A scenario: a name, a dataset, and the classification it is built to produce. */
    private class Scenario(
        val name: String,
        val intent: String,
        val records: List<A001Analysis.PairRecord>,
    )

    private fun ablationArms(
        prefix: String,
        means: List<Double>,
        sd: Double,
        n: Int,
    ): List<A001Analysis.PairRecord> =
        A001StudyContract.ABLATION_FAMILY.mapIndexed { index, arm ->
            records("$prefix-A${index + 1}", arm, n, means[index], sd, forcedChoice = false)
        }.flatten()

    private fun scenarios(): List<Scenario> = listOf(
        Scenario(
            "PRIMARY_PASS",
            "a clear effect above the +10 floor with an interval well clear of zero, " +
                "and an ablation family where two arms survive Holm correction and one does not",
            records("PASS", Cohort.SCRIPTED_PET_BASELINE, 60, 16.0, 18.0) +
                ablationArms("PASS", listOf(14.0, 12.0, 2.0), 18.0, 60),
        ),
        Scenario(
            "PRIMARY_FAIL_NOT_PRACTICALLY_MEANINGFUL",
            "statistically unambiguous and substantively trivial: the interval excludes " +
                "zero, and the effect is a quarter of the floor",
            records("SMALL", Cohort.SCRIPTED_PET_BASELINE, 200, 2.5, 9.0) +
                ablationArms("SMALL", listOf(1.0, 0.5, 0.2), 9.0, 60),
        ),
        Scenario(
            "PRIMARY_FAIL_IMPRECISE",
            "practically large and statistically unresolved: the point estimate clears " +
                "the floor and the interval still contains zero",
            records("WIDE", Cohort.SCRIPTED_PET_BASELINE, 14, 12.0, 34.0) +
                ablationArms("WIDE", listOf(3.0, 2.0, 1.0), 34.0, 14),
        ),
        Scenario(
            "PRIMARY_FAIL_NULL_OR_NEGATIVE",
            "the outright negative result: the scripted baseline is rated more alive " +
                "than FULL",
            records("NEG", Cohort.SCRIPTED_PET_BASELINE, 60, -6.0, 16.0) +
                ablationArms("NEG", listOf(0.5, -1.0, 0.0), 16.0, 60),
        ),
        Scenario(
            "SCREENING_AND_MISSING_DATA",
            "every exclusion route exercised at once, with the surviving complete cases " +
                "analysed and nothing imputed",
            screeningFixture(),
        ),
    )

    /** One record per exclusion reason, plus enough clean pairs to analyse. */
    private fun screeningFixture(): List<A001Analysis.PairRecord> {
        val clean = records("SCR", Cohort.SCRIPTED_PET_BASELINE, 40, 14.0, 17.0) +
            ablationArms("SCR", listOf(11.0, 9.0, 3.0), 17.0, 40)
        val dirty = listOf(
            A001Analysis.PairRecord(
                "SCR-001", Cohort.SCRIPTED_PET_BASELINE, 80.0, 40.0,
            ),
            A001Analysis.PairRecord(
                "SCR-901", Cohort.SCRIPTED_PET_BASELINE, 90.0, 20.0, priorNonScoredPool = true,
            ),
            A001Analysis.PairRecord(
                "SCR-902", Cohort.SCRIPTED_PET_BASELINE, 88.0, 22.0, technicalFailure = true,
            ),
            A001Analysis.PairRecord(
                "SCR-903", Cohort.SCRIPTED_PET_BASELINE, 85.0, 25.0,
                fullCompletedFraction = 0.40,
            ),
            A001Analysis.PairRecord(
                "SCR-904", Cohort.SCRIPTED_PET_BASELINE, null, 30.0,
            ),
            A001Analysis.PairRecord(
                "SCR-905", Cohort.SCRIPTED_PET_BASELINE, 140.0, 30.0,
            ),
        )
        return clean + dirty
    }

    public fun render(): String = buildString {
        append("A001_DRY_RUN=").append(DRY_RUN_ID).append('\n')
        append("DATA_CLASS=SYNTHETIC — no human has rated anything; this is engineering\n")
        append("           evidence that the preregistered pipeline behaves as written,\n")
        append("           and is not scientific evidence about the organism.\n")
        append("protocol=").append(A001StudyContract.PROTOCOL_ID)
        append(" v").append(A001StudyContract.PROTOCOL_VERSION)
        append("  analysis=").append(A001Analysis.ANALYSIS_ID)
        append(" v").append(A001Analysis.ANALYSIS_VERSION).append("\n\n")

        append(GradedAlivenessInstrument.render()).append('\n')

        for (scenario in scenarios()) {
            append("================================================================\n")
            append("SCENARIO ").append(scenario.name).append("   [SYNTHETIC]\n")
            append("intent: ").append(scenario.intent).append("\n\n")
            append(A001Analysis.analyze(scenario.records).render())
            append('\n')
        }

        append("================================================================\n")
        append("SEALED VARIANCE-PILOT CHANNEL   [SYNTHETIC]\n")
        append("Two pilots built to disagree completely about direction and to share a\n")
        append("dispersion. If the released output can tell them apart, the barrier leaks.\n\n")
        val favourable = pilotRecords(+22.0, 14.0)
        val unfavourable = pilotRecords(-22.0, 14.0)
        val releaseA = BlindVariancePilot.release(favourable)
        val releaseB = BlindVariancePilot.release(unfavourable)
        append("pilot A (synthetic mean difference +22.000):\n").append(releaseA.render())
        append("pilot B (synthetic mean difference -22.000):\n").append(releaseB.render())
        append("identicalRelease=").append(releaseA.render() == releaseB.render()).append('\n')
        append("releasedFields=")
        append(
            BlindVariancePilot.PilotRelease::class.java.declaredFields
                .filterNot { it.isSynthetic }
                .joinToString(",") { it.name },
        )
        append("\n\n")

        append("================================================================\n")
        append("FEASIBILITY BUDGET\n\n")
        append("with no pilot and no ceiling (the real current state):\n")
        append(A001FeasibilityBudget.compute(null, null).render())
        append('\n')
        append("with a SYNTHETIC pilot release and still no owner ceiling:\n")
        append(A001FeasibilityBudget.compute(releaseA, null).render())
        append('\n')
        append("with a SYNTHETIC pilot release and the REAL frozen owner ceiling:\n")
        append(
            A001FeasibilityBudget.compute(
                releaseA,
                A001FeasibilityBudget.FROZEN_OWNER_CEILING,
            ).render(),
        )
        append('\n')
        append("with a SYNTHETIC pilot release and a SYNTHETIC tight ceiling:\n")
        append(
            A001FeasibilityBudget.compute(
                releaseA,
                A001FeasibilityBudget.OwnerCeiling(40, 20.0),
            ).render(),
        )
        append('\n')

        append("================================================================\n")
        append("ACTIVATION AUDIT\n\n")
        append(AlivenessGovernanceAudit.render(AlivenessGovernanceAudit.audit()))
    }

    private fun pilotRecords(mean: Double, sd: Double): List<BlindVariancePilot.PilotRecord> {
        val n = A001StudyContract.VARIANCE_PILOT_PARTICIPANTS
        val d = differences(n, mean, sd)
        return (0 until n).map { i ->
            BlindVariancePilot.PilotRecord(
                participantId = "PILOT-${(i + 1).toString().padStart(3, '0')}",
                fullScore = clamp(50.0 + d[i] / 2.0),
                baselineScore = clamp(50.0 - d[i] / 2.0),
            )
        }
    }
}
