package com.animusmachinae.dll17.research.aliveness.analysis

/**
 * `A001FeasibilityBudgetV1`.
 *
 * Turns the one number the blind variance pilot is allowed to release into the
 * powered sample size for Attempt 1, and then answers a single question: does
 * that sample fit inside the owner's resource ceiling?
 *
 * It answers `A001_NOT_FEASIBLE` rather than trimming the sample, because an
 * underpowered scored attempt is worse than no attempt — it consumes one of the
 * three, and a null result from it is uninterpretable.
 *
 * Two inputs are deliberately absent and cannot be defaulted: the released
 * `pairedDifferenceSd`, which requires the pilot to have run, and the owner's
 * ceiling, which is a funding decision. Calling this without them returns a
 * blocking state, never a number.
 */
public object A001FeasibilityBudget {

    public const val BUDGET_ID: String = "A001FeasibilityBudgetV1"
    public const val BUDGET_VERSION: Int = 1

    // --------------------------------------------- frozen procedure schedule

    /**
     * The per-participant schedule from `AlivenessStudyProtocolV1`, itemized so
     * a reader can see what the participant-hour figure is made of rather than
     * taking a round number on trust. These are protocol design values, not
     * measurements, and the owner may replace them with observed session times
     * once any session has been run.
     */
    public const val CONSENT_SECONDS: Int = 300
    public const val BRIEFING_SECONDS: Int = 120
    public const val INSTRUMENT_SECONDS_PER_ADMINISTRATION: Int = 120
    public const val SECONDARY_ITEMS_SECONDS: Int = 60
    public const val DEBRIEF_SECONDS: Int = 300

    /** Total scheduled time for one participant, both creatures included. */
    public const val PARTICIPANT_SECONDS: Int =
        CONSENT_SECONDS + BRIEFING_SECONDS +
            2 * A001StudyContract.SESSION_SECONDS +
            2 * INSTRUMENT_SECONDS_PER_ADMINISTRATION +
            SECONDARY_ITEMS_SECONDS + DEBRIEF_SECONDS

    // ----------------------------------------------------------------- states

    public enum class FeasibilityState {
        /** The pilot has not run, or released an invalid result. */
        BLOCKED_SPEC_PAIRED_DIFFERENCE_SD,

        /** No owner resource ceiling exists. */
        BLOCKED_SPEC_STUDY_BUDGET,

        /** The powered requirement fits the ceiling. */
        A001_FEASIBLE,

        /** The powered requirement exceeds the ceiling. Redesign before scoring. */
        A001_NOT_FEASIBLE,
    }

    /**
     * The owner's resource ceiling. There is no default constructor value and no
     * fallback: this object exists only when a human has decided the numbers.
     */
    public class OwnerCeiling(
        public val maxFundableParticipants: Int,
        public val maxParticipantHours: Double,
    )

    public class Result(
        public val state: FeasibilityState,
        public val releasedSd: Double?,
        public val inflatedSd: Double?,
        public val requiredPairsPrimary: Int?,
        public val requiredPairsPerAblationArm: Int?,
        public val totalRequiredParticipants: Int?,
        public val totalRequiredParticipantHours: Double?,
        public val ceiling: OwnerCeiling?,
        public val detail: String,
    ) {
        public fun render(): String = buildString {
            append(BUDGET_ID).append(" v").append(BUDGET_VERSION).append('\n')
            append("  alpha=").append(Statistics.d3(A001StudyContract.PRIMARY_ALPHA))
            append(" power=").append(Statistics.d3(A001StudyContract.PRIMARY_POWER))
            append(" minimumWorthwhileDifference=")
            append(Statistics.d3(A001StudyContract.MINIMUM_PAIRED_DIFFERENCE))
            append(" sdInflation=").append(Statistics.d3(A001StudyContract.PILOT_SD_INFLATION))
            append('\n')
            append("  releasedSd=").append(releasedSd?.let { Statistics.d6(it) } ?: "UNAVAILABLE")
            append(" inflatedSd=").append(inflatedSd?.let { Statistics.d6(it) } ?: "UNAVAILABLE")
            append('\n')
            append("  requiredPairsPrimary=").append(requiredPairsPrimary ?: "UNAVAILABLE")
            append(" requiredPairsPerAblationArm=")
            append(requiredPairsPerAblationArm ?: "UNAVAILABLE").append('\n')
            append("  totalRequiredParticipants=")
            append(totalRequiredParticipants ?: "UNAVAILABLE")
            append(" totalRequiredParticipantHours=")
            append(totalRequiredParticipantHours?.let { Statistics.d3(it) } ?: "UNAVAILABLE")
            append('\n')
            append("  ownerCeiling=")
            if (ceiling == null) {
                append("UNRESOLVED\n")
            } else {
                append("participants=").append(ceiling.maxFundableParticipants)
                append(" participantHours=").append(Statistics.d3(ceiling.maxParticipantHours))
                append('\n')
            }
            append("  STATE=").append(state.name).append('\n')
            append("  detail: ").append(detail).append('\n')
        }
    }

    /**
     * Compute the powered budget.
     *
     * Each ablation arm is powered independently at the family-wise corrected
     * level, using the most conservative Holm step (alpha / m). Powering the
     * arms at the uncorrected alpha would produce a family that is significant
     * before correction and not after, which is the failure mode multiplicity
     * correction exists to prevent.
     */
    public fun compute(
        release: BlindVariancePilot.PilotRelease?,
        ceiling: OwnerCeiling?,
    ): Result {
        if (release == null || !release.protocolValid || release.pairedDifferenceSd.isNaN() ||
            release.pairedDifferenceSd <= 0.0
        ) {
            return Result(
                state = FeasibilityState.BLOCKED_SPEC_PAIRED_DIFFERENCE_SD,
                releasedSd = release?.pairedDifferenceSd,
                inflatedSd = null,
                requiredPairsPrimary = null,
                requiredPairsPerAblationArm = null,
                totalRequiredParticipants = null,
                totalRequiredParticipantHours = null,
                ceiling = ceiling,
                detail = if (release == null) {
                    "no pilot release exists; BlindVariancePilotV1 has not run"
                } else {
                    "the pilot release is not protocol-valid or carries no usable SD"
                },
            )
        }

        val inflated = release.pairedDifferenceSd * A001StudyContract.PILOT_SD_INFLATION
        val primaryPairs = Statistics.pairedTSampleSize(
            delta = A001StudyContract.MINIMUM_PAIRED_DIFFERENCE,
            sd = inflated,
            alpha = A001StudyContract.PRIMARY_ALPHA,
            power = A001StudyContract.PRIMARY_POWER,
        )
        val arms = A001StudyContract.ABLATION_FAMILY.size
        val ablationPairs = Statistics.pairedTSampleSize(
            delta = A001StudyContract.MINIMUM_PAIRED_DIFFERENCE,
            sd = inflated,
            alpha = A001StudyContract.ABLATION_FAMILY_ALPHA / arms,
            power = A001StudyContract.PRIMARY_POWER,
        )
        // Separate rater pools: nobody rates the primary pair and an ablation pair.
        val participants = primaryPairs + arms * ablationPairs
        val hours = participants.toDouble() * PARTICIPANT_SECONDS / 3600.0

        if (ceiling == null) {
            return Result(
                state = FeasibilityState.BLOCKED_SPEC_STUDY_BUDGET,
                releasedSd = release.pairedDifferenceSd,
                inflatedSd = inflated,
                requiredPairsPrimary = primaryPairs,
                requiredPairsPerAblationArm = ablationPairs,
                totalRequiredParticipants = participants,
                totalRequiredParticipantHours = hours,
                ceiling = null,
                detail = "the powered requirement is computable, but no owner resource " +
                    "ceiling exists to compare it against",
            )
        }

        val fits = participants <= ceiling.maxFundableParticipants &&
            hours <= ceiling.maxParticipantHours
        return Result(
            state = if (fits) FeasibilityState.A001_FEASIBLE else FeasibilityState.A001_NOT_FEASIBLE,
            releasedSd = release.pairedDifferenceSd,
            inflatedSd = inflated,
            requiredPairsPrimary = primaryPairs,
            requiredPairsPerAblationArm = ablationPairs,
            totalRequiredParticipants = participants,
            totalRequiredParticipantHours = hours,
            ceiling = ceiling,
            detail = if (fits) {
                "the powered requirement fits the frozen owner ceiling"
            } else {
                "the powered requirement exceeds the frozen owner ceiling; Attempt 1 " +
                    "must be redesigned rather than run underpowered"
            },
        )
    }
}
