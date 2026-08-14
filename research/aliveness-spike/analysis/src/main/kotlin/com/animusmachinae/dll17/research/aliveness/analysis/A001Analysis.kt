package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort

/**
 * The preregistered A001 Attempt-1 analysis, written before any human data
 * exists so that no choice in it can be made after seeing a result.
 *
 * The whole point of authoring this now is that every judgement call — which
 * pairs are excluded, what counts as a completed session, how a missing score is
 * treated, which comparison is primary, how multiplicity is corrected, and what
 * separates a pass from a near miss — is fixed in code and testable against
 * synthetic fixtures. After the first real participant, changing any of it is a
 * material change under `MaterialChangeEligibilityV1`.
 *
 * Nothing in this file has ever seen a human score.
 */
public object A001Analysis {

    public const val ANALYSIS_ID: String = "A001AnalysisV1"
    public const val ANALYSIS_VERSION: Int = 1

    // ------------------------------------------------------------------ input

    /**
     * One rater's paired observation: the FULL creature and one comparator,
     * seen in counterbalanced order in a single sitting.
     */
    public class PairRecord(
        public val participantId: String,
        /** The comparator arm. `SCRIPTED_PET_BASELINE` for the primary endpoint. */
        public val comparator: Cohort,
        public val fullScore: Double?,
        public val comparatorScore: Double?,
        public val fullCompletedFraction: Double = 1.0,
        public val comparatorCompletedFraction: Double = 1.0,
        public val technicalFailure: Boolean = false,
        /** True if this person took part in the variance pilot or baseline qualification. */
        public val priorNonScoredPool: Boolean = false,
        /** Secondary endpoint. Null when not administered or not answered. */
        public val forcedChoicePreferredFull: Boolean? = null,
    )

    public enum class ExclusionReason {
        DUPLICATE_PARTICIPANT,
        PRIOR_NON_SCORED_POOL,
        TECHNICAL_FAILURE,
        INCOMPLETE_SESSION,
        MISSING_SCORE,
        SCORE_OUT_OF_RANGE,
    }

    public class Exclusion(
        public val participantId: String,
        public val comparator: Cohort,
        public val reason: ExclusionReason,
        public val detail: String,
    )

    public class ScreenedSet(
        public val included: List<PairRecord>,
        public val excluded: List<Exclusion>,
    )

    /**
     * Preregistered screening.
     *
     * Complete-case by design: a pair with a missing or unusable half is
     * excluded whole, never imputed and never carried forward as a one-sided
     * observation. Imputation on an endpoint this small would be a decision made
     * after seeing which half went missing.
     *
     * Order matters and is fixed: identity problems first, then protocol
     * violations, then data problems, so a pair is reported under the first
     * reason that disqualified it rather than the most flattering one.
     */
    public fun screen(records: List<PairRecord>): ScreenedSet {
        val included = ArrayList<PairRecord>()
        val excluded = ArrayList<Exclusion>()
        val seen = HashSet<String>()
        for (r in records) {
            val key = r.participantId + "/" + r.comparator.cohortId
            val reason: Pair<ExclusionReason, String>? = when {
                !seen.add(key) ->
                    ExclusionReason.DUPLICATE_PARTICIPANT to "participant appears twice in this arm"

                r.priorNonScoredPool ->
                    ExclusionReason.PRIOR_NON_SCORED_POOL to
                        "took part in a variance pilot or baseline qualification pool"

                r.technicalFailure ->
                    ExclusionReason.TECHNICAL_FAILURE to "session flagged as a technical failure"

                r.fullCompletedFraction < A001StudyContract.MINIMUM_COMPLETED_SESSION_FRACTION ||
                    r.comparatorCompletedFraction <
                    A001StudyContract.MINIMUM_COMPLETED_SESSION_FRACTION ->
                    ExclusionReason.INCOMPLETE_SESSION to
                        "completed " + Statistics.d3(r.fullCompletedFraction) + "/" +
                        Statistics.d3(r.comparatorCompletedFraction) + " of the required " +
                        Statistics.d3(A001StudyContract.MINIMUM_COMPLETED_SESSION_FRACTION)

                r.fullScore == null || r.comparatorScore == null ->
                    ExclusionReason.MISSING_SCORE to "one or both scores were not recorded"

                outOfRange(r.fullScore) || outOfRange(r.comparatorScore) ->
                    ExclusionReason.SCORE_OUT_OF_RANGE to "a score fell outside 0-100"

                else -> null
            }
            if (reason == null) included.add(r) else {
                excluded.add(Exclusion(r.participantId, r.comparator, reason.first, reason.second))
            }
        }
        return ScreenedSet(included, excluded)
    }

    private fun outOfRange(score: Double): Boolean =
        score < A001StudyContract.SCORE_MIN || score > A001StudyContract.SCORE_MAX

    // ----------------------------------------------------------- paired test

    public class PairedResult(
        public val comparator: Cohort,
        public val n: Int,
        public val meanDifference: Double,
        public val sd: Double,
        public val standardError: Double,
        public val degreesOfFreedom: Double,
        public val t: Double,
        public val p: Double,
        public val ciLow: Double,
        public val ciHigh: Double,
    )

    /** Paired difference FULL − comparator, with a two-sided 95% interval. */
    public fun pairedTest(records: List<PairRecord>, comparator: Cohort): PairedResult {
        val arm = records.filter { it.comparator == comparator }
        require(arm.size >= 2) {
            "the paired test for ${comparator.cohortId} needs at least two complete pairs"
        }
        val differences = DoubleArray(arm.size) { arm[it].fullScore!! - arm[it].comparatorScore!! }
        val n = differences.size
        val mean = Statistics.mean(differences)
        val sd = Statistics.sampleSd(differences)
        val df = (n - 1).toDouble()
        val se = sd / StrictMath.sqrt(n.toDouble())
        val t = if (se == 0.0) {
            if (mean == 0.0) 0.0 else StrictMath.copySign(Double.MAX_VALUE, mean)
        } else {
            mean / se
        }
        val p = if (se == 0.0 && mean == 0.0) 1.0 else Statistics.studentTTwoSidedP(t, df)
        val crit = Statistics.studentTQuantile(
            1.0 - (1.0 - A001StudyContract.CONFIDENCE_LEVEL) / 2.0,
            df,
        )
        return PairedResult(
            comparator = comparator,
            n = n,
            meanDifference = mean,
            sd = sd,
            standardError = se,
            degreesOfFreedom = df,
            t = t,
            p = p,
            ciLow = mean - crit * se,
            ciHigh = mean + crit * se,
        )
    }

    // -------------------------------------------------------- primary verdict

    /**
     * The four ways Attempt 1 can end, kept separate on purpose.
     *
     * A statistically significant +3 point difference and a +14 point difference
     * whose interval crosses zero are both failures, but they fail for opposite
     * reasons and imply opposite next steps. Collapsing them into one FAIL is
     * how a programme talks itself into a fourth attempt.
     */
    public enum class PrimaryClassification {
        /** Mean at or above the floor and the interval excludes zero. */
        PASS,

        /** Interval excludes zero, but the effect is below the +10 floor. */
        FAIL_NOT_PRACTICALLY_MEANINGFUL,

        /** Mean at or above the floor, but the interval includes zero. */
        FAIL_IMPRECISE,

        /** Neither condition met. */
        FAIL_NULL_OR_NEGATIVE,
    }

    public fun classifyPrimary(result: PairedResult): PrimaryClassification {
        val practical = result.meanDifference >= A001StudyContract.MINIMUM_PAIRED_DIFFERENCE
        val precise = result.ciLow > 0.0
        return when {
            practical && precise -> PrimaryClassification.PASS
            precise -> PrimaryClassification.FAIL_NOT_PRACTICALLY_MEANINGFUL
            practical -> PrimaryClassification.FAIL_IMPRECISE
            else -> PrimaryClassification.FAIL_NULL_OR_NEGATIVE
        }
    }

    // ------------------------------------------------------------- secondary

    public class ForcedChoiceResult(
        public val n: Int,
        public val preferredFull: Int,
        public val proportion: Double,
        public val p: Double,
    )

    /**
     * Secondary. Reported always, and decisive never: a forced choice cannot
     * rescue a failed primary endpoint, and this function has no way to say that
     * it did.
     */
    public fun forcedChoice(records: List<PairRecord>, comparator: Cohort): ForcedChoiceResult {
        val answers = records
            .filter { it.comparator == comparator }
            .mapNotNull { it.forcedChoicePreferredFull }
        val n = answers.size
        val preferred = answers.count { it }
        return ForcedChoiceResult(
            n = n,
            preferredFull = preferred,
            proportion = if (n == 0) Double.NaN else preferred.toDouble() / n,
            p = Statistics.binomialTwoSidedP(preferred, n),
        )
    }

    // ------------------------------------------------------------- ablations

    public class AblationResult(
        public val comparator: Cohort,
        public val paired: PairedResult,
        public val rawP: Double,
        public val holmAdjustedP: Double,
        public val significant: Boolean,
    ) {
        /** What the arm licenses. Retention is earned, not assumed. */
        public val verdict: String
            get() = if (significant) "MECHANISM_CONTRIBUTION_DEMONSTRATED" else
                "MECHANISM_CONTRIBUTION_NOT_DEMONSTRATED"
    }

    /**
     * Holm-Bonferroni across the preregistered family, at FWER 0.05.
     *
     * Holm rather than Bonferroni because it is uniformly more powerful at the
     * same family-wise error rate, and the family is small enough that the
     * difference is worth having. The step-down enforces monotone adjusted
     * p-values, so an arm can never be declared significant while a
     * smaller-p arm is not.
     */
    public fun ablations(records: List<PairRecord>): List<AblationResult> {
        val arms = A001StudyContract.ABLATION_FAMILY
        val paired = arms.map { pairedTest(records, it) }
        val m = arms.size
        val order = paired.indices.sortedBy { paired[it].p }
        val adjusted = DoubleArray(m)
        var running = 0.0
        for ((rank, index) in order.withIndex()) {
            val candidate = (m - rank) * paired[index].p
            running = StrictMath.max(running, StrictMath.min(1.0, candidate))
            adjusted[index] = running
        }
        return arms.indices.map { i ->
            AblationResult(
                comparator = arms[i],
                paired = paired[i],
                rawP = paired[i].p,
                holmAdjustedP = adjusted[i],
                significant = adjusted[i] <= A001StudyContract.ABLATION_FAMILY_ALPHA,
            )
        }
    }

    // ---------------------------------------------------------------- report

    public class AttemptResult(
        public val screened: ScreenedSet,
        public val primary: PairedResult,
        public val classification: PrimaryClassification,
        public val secondary: ForcedChoiceResult,
        public val ablations: List<AblationResult>,
    ) {
        public val attemptOutcome: String
            get() = when (classification) {
                PrimaryClassification.PASS -> "A001_ATTEMPT_1_PASS"
                else -> "A001_ATTEMPT_1_FAIL"
            }

        public fun render(): String = buildString {
            append(ANALYSIS_ID).append(" v").append(ANALYSIS_VERSION)
            append("  protocol=").append(A001StudyContract.PROTOCOL_ID)
            append(" v").append(A001StudyContract.PROTOCOL_VERSION).append('\n')
            append("screening: included=").append(screened.included.size)
            append(" excluded=").append(screened.excluded.size).append('\n')
            for (reason in ExclusionReason.entries) {
                val count = screened.excluded.count { it.reason == reason }
                if (count > 0) append("  ").append(reason.name).append('=').append(count).append('\n')
            }
            append('\n')
            append("PRIMARY  ").append(A001StudyContract.PRIMARY_ENDPOINT_ID)
            append(" (FULL - ").append(primary.comparator.cohortId).append(")\n")
            append("  n=").append(primary.n)
            append(" mean=").append(Statistics.d3(primary.meanDifference))
            append(" sd=").append(Statistics.d3(primary.sd))
            append(" t=").append(Statistics.d3(primary.t))
            append(" df=").append(Statistics.d3(primary.degreesOfFreedom))
            append(" p=").append(Statistics.d6(primary.p)).append('\n')
            append("  95% CI [").append(Statistics.d3(primary.ciLow))
            append(", ").append(Statistics.d3(primary.ciHigh)).append("]\n")
            append("  rule: mean >= ")
            append(Statistics.d3(A001StudyContract.MINIMUM_PAIRED_DIFFERENCE))
            append(" AND ciLow > 0\n")
            append("  practicalFloorMet=")
            append(primary.meanDifference >= A001StudyContract.MINIMUM_PAIRED_DIFFERENCE)
            append(" intervalExcludesZero=").append(primary.ciLow > 0.0).append('\n')
            append("  CLASSIFICATION=").append(classification.name).append('\n')
            append('\n')
            append("SECONDARY  forced choice (cannot rescue a failed primary)\n")
            append("  n=").append(secondary.n)
            append(" preferredFULL=").append(secondary.preferredFull)
            append(" proportion=").append(
                if (secondary.n == 0) "n/a" else Statistics.d3(secondary.proportion),
            )
            append(" exactP=").append(Statistics.d6(secondary.p)).append('\n')
            append('\n')
            append("ABLATION FAMILY  Holm-Bonferroni, FWER=")
            append(Statistics.d3(A001StudyContract.ABLATION_FAMILY_ALPHA))
            append(", arms=").append(ablations.size).append('\n')
            for (a in ablations) {
                append("  ").append(a.comparator.cohortId).append('\n')
                append("    n=").append(a.paired.n)
                append(" mean=").append(Statistics.d3(a.paired.meanDifference))
                append(" 95% CI [").append(Statistics.d3(a.paired.ciLow))
                append(", ").append(Statistics.d3(a.paired.ciHigh)).append(']')
                append(" rawP=").append(Statistics.d6(a.rawP))
                append(" holmP=").append(Statistics.d6(a.holmAdjustedP)).append('\n')
                append("    ").append(a.verdict).append('\n')
            }
            append('\n')
            append("ATTEMPT_OUTCOME=").append(attemptOutcome).append('\n')
        }
    }

    /**
     * The whole preregistered pipeline: screen, test the primary endpoint,
     * classify it, report the secondary, then the corrected family.
     */
    public fun analyze(records: List<PairRecord>): AttemptResult {
        val screened = screen(records)
        val primary = pairedTest(screened.included, Cohort.SCRIPTED_PET_BASELINE)
        return AttemptResult(
            screened = screened,
            primary = primary,
            classification = classifyPrimary(primary),
            secondary = forcedChoice(screened.included, Cohort.SCRIPTED_PET_BASELINE),
            ablations = ablations(screened.included),
        )
    }
}
