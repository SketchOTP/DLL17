package com.animusmachinae.dll17.research.aliveness.analysis

/**
 * `BlindVariancePilotV1`, sealed analysis path.
 *
 * The scored attempt needs one number from the pilot — the standard deviation of
 * paired FULL-minus-baseline differences — and must not learn anything else,
 * because the FULL team decides how to power, run and interpret the scored
 * attempt, and a team that knows the pilot leaned favourable will make those
 * decisions differently even in good faith.
 *
 * The barrier is structural rather than procedural. [PilotRelease] is the only
 * type this file makes public, and it carries exactly two fields. There is no
 * accessor anywhere in the module for the mean, the sign, the cohort means, the
 * per-participant differences, or the forced-choice counts, because the type
 * that would hold them is private and never escapes [release].
 *
 * `BlindVariancePilotSealTest` proves both halves of that: the released type
 * declares only the permitted fields, and two pilots with opposite outcomes and
 * matching dispersion release byte-identical output.
 */
public object BlindVariancePilot {

    public const val PILOT_ID: String = "BlindVariancePilotV1"
    public const val PILOT_VERSION: Int = 1

    /** Registration status. A pilot is variance-only and can never be promoted. */
    public const val REGISTRATION: String = "VARIANCE_ONLY, NON_SCORED"

    /**
     * One pilot participant's paired sitting.
     *
     * The input type is permitted to hold the scores — someone has to compute
     * the SD. It is the *output* that is constrained, and the input never leaves
     * the operator's side of the barrier.
     */
    public class PilotRecord(
        public val participantId: String,
        public val fullScore: Double?,
        public val baselineScore: Double?,
        public val fullCompletedFraction: Double = 1.0,
        public val baselineCompletedFraction: Double = 1.0,
        public val technicalFailure: Boolean = false,
    )

    /**
     * Everything the FULL team is permitted to receive from the pilot.
     *
     * Two fields, both of which are invariant to the direction of the result:
     * a standard deviation is unchanged if every difference flips sign, and the
     * validity flag depends only on how many pairs were analyzable.
     */
    public class PilotRelease internal constructor(
        public val pairedDifferenceSd: Double,
        public val protocolValid: Boolean,
    ) {
        public fun render(): String = buildString {
            append(PILOT_ID).append(" v").append(PILOT_VERSION).append(" RELEASE\n")
            append("  pairedDifferenceSd=").append(Statistics.d6(pairedDifferenceSd)).append('\n')
            append("  protocolValid=").append(protocolValid).append('\n')
            append("  withheld: mean difference, sign, cohort means, per-participant\n")
            append("            differences, subgroup effects, forced-choice results,\n")
            append("            comparative comments, raw recordings\n")
        }
    }

    /**
     * Analysable pairs required for the release to be protocol-valid.
     *
     * Equal to the registered pilot size: the pilot over-recruits to replace
     * dropouts, and replacement is preregistered and outcome-independent.
     *
     * The number is not arbitrary and is not the implementer's. The architect
     * froze a 1.25 inflation factor on the pilot SD, and 1.25 is the one-sided
     * 95% upper confidence bound on a standard deviation at exactly 35 degrees
     * of freedom — that is, at 36 pairs. At 35 pairs the bound is 1.253 and the
     * frozen factor no longer covers it. `BlindVariancePilotSealTest` verifies
     * that numerically rather than taking it on trust.
     */
    public const val REQUIRED_ANALYZED_PAIRS: Int = A001StudyContract.VARIANCE_PILOT_PARTICIPANTS

    /**
     * The sealed analysis, held privately. It is computed in full because the
     * independent operator legitimately needs it; it is simply never returned.
     */
    private class SealedAnalysis(records: List<PilotRecord>) {
        val differences: DoubleArray
        val analyzedPairs: Int

        init {
            val usable = records.filter { r ->
                !r.technicalFailure &&
                    r.fullScore != null && r.baselineScore != null &&
                    r.fullCompletedFraction >= A001StudyContract.MINIMUM_COMPLETED_SESSION_FRACTION &&
                    r.baselineCompletedFraction >=
                    A001StudyContract.MINIMUM_COMPLETED_SESSION_FRACTION &&
                    r.fullScore in A001StudyContract.SCORE_MIN..A001StudyContract.SCORE_MAX &&
                    r.baselineScore in A001StudyContract.SCORE_MIN..A001StudyContract.SCORE_MAX
            }
            differences = DoubleArray(usable.size) { usable[it].fullScore!! - usable[it].baselineScore!! }
            analyzedPairs = usable.size
        }
    }

    /**
     * The only path across the barrier.
     *
     * Everything computed inside is discarded when this function returns except
     * the two released fields.
     */
    public fun release(records: List<PilotRecord>): PilotRelease {
        val sealed = SealedAnalysis(records)
        val valid = sealed.analyzedPairs >= REQUIRED_ANALYZED_PAIRS
        val sd = if (sealed.analyzedPairs >= 2) Statistics.sampleSd(sealed.differences) else Double.NaN
        return PilotRelease(sd, valid)
    }
}
