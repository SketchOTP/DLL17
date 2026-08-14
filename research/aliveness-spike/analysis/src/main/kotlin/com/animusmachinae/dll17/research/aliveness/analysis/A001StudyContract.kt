package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.SpikeContract

/**
 * `AlivenessStudyProtocolV1` — the architect-frozen constants for A001 Attempt 1.
 *
 * Every value in this file was set by the architect and is preregistered. None
 * of them is derived from data, because no human data exists; and none of them
 * may move after the first scored participant. The three that could be argued
 * about later — the +10 point floor, the +15 point baseline competence margin,
 * and the two fixed sample sizes — are exactly the ones that would be quietly
 * relaxed if they lived anywhere less visible than a frozen constant.
 *
 * Deliberately absent: the maximum fundable participants and participant-hours,
 * and the paired-difference standard deviation. Those require an owner decision
 * and real pilot data respectively. `A001FeasibilityBudget` refuses to produce a
 * number without them rather than defaulting.
 */
public object A001StudyContract {

    public const val PROTOCOL_ID: String = "AlivenessStudyProtocolV1"
    public const val PROTOCOL_VERSION: Int = 1
    public const val INSTRUMENT_ID: String = "GradedAlivenessInstrumentV1"
    public const val ATTEMPT: Int = 1

    // -------------------------------------------------------- primary endpoint

    /**
     * `PairedAlivenessDifference = score(FULL) − score(ScriptedPetBaselineV1)`,
     * on the 0–100 graded instrument, within rater.
     */
    public const val PRIMARY_ENDPOINT_ID: String = "PairedAlivenessDifference"

    /** The programme success floor. Attempt 1 passes only at or above this. */
    public const val MINIMUM_PAIRED_DIFFERENCE: Double = 10.0

    /** Two-sided confidence level for the primary interval. */
    public const val CONFIDENCE_LEVEL: Double = 0.95

    public const val PRIMARY_ALPHA: Double = 0.05
    public const val PRIMARY_POWER: Double = 0.80

    /**
     * Applied to the pilot SD before powering, so a pilot that happened to draw
     * an unusually tight sample cannot underpower the scored attempt.
     */
    public const val PILOT_SD_INFLATION: Double = 1.25

    /** Instrument bounds. */
    public const val SCORE_MIN: Double = 0.0
    public const val SCORE_MAX: Double = 100.0

    // -------------------------------------------------------------- procedure

    /** Seconds of live interaction per creature. Matches the expression contract. */
    public const val SESSION_SECONDS: Int = SpikeContract.PRIMARY_SESSION_SECONDS

    /** Minimum share of the session a rater must complete for the pair to score. */
    public const val MINIMUM_COMPLETED_SESSION_FRACTION: Double = 0.90

    // -------------------------------------------------------- variance pilot

    /** Preregistered, fixed, and not a function of any outcome. */
    public const val VARIANCE_PILOT_PARTICIPANTS: Int = 36

    // --------------------------------------------------- baseline qualification

    public const val BASELINE_QUALIFICATION_PARTICIPANTS: Int = 40
    public const val BASELINE_COMPETENCE_MARGIN: Double = 15.0
    public const val BASELINE_INSTRUMENT_ID: String = "BaselineCompetenceInstrumentV1"

    // ------------------------------------------------------- ablation family

    /** Family-wise error rate across the human leave-one-out arms. */
    public const val ABLATION_FAMILY_ALPHA: Double = 0.05

    public val ABLATION_FAMILY: List<Cohort> = Cohort.HUMAN_ABLATION_FAMILY

    // ------------------------------------------------------------- unresolved

    /**
     * The inputs that cannot be frozen here, named so that a reader can see the
     * gap rather than infer it from an absence.
     */
    public val UNRESOLVED_REAL_WORLD_INPUTS: List<String> = listOf(
        "pairedDifferenceSD — released only by BlindVariancePilotV1, which has not run",
        "maxFundableParticipants — an owner funding decision",
        "maxParticipantHours — an owner scheduling decision",
        "PrimaryIndependentAlivenessGateReviewer — unassigned",
        "AlternateIndependentAlivenessGateReviewer — unassigned",
        "BaselineIndependentOwner — unassigned",
    )
}

/**
 * `GradedAlivenessInstrumentV1`.
 *
 * One item, administered once per creature immediately after that creature's
 * session and before the next one starts. One item and not a battery, because
 * the primary endpoint is a difference of two administrations of the *same*
 * question, and every extra item is another place for the two administrations
 * to diverge for reasons that have nothing to do with the creature.
 *
 * The wording below is frozen exactly as it will be read. It is not pretested —
 * see the protocol document, which records that as a stated limitation rather
 * than a resolved question.
 */
public object GradedAlivenessInstrument {

    public const val INSTRUMENT_ID: String = A001StudyContract.INSTRUMENT_ID
    public const val INSTRUMENT_VERSION: Int = 1

    /** Read aloud, or displayed, identically before every administration. */
    public const val PREAMBLE: String =
        "You have just spent ten minutes with this creature. There are no right " +
            "or wrong answers, and you are not being tested. We are asking about " +
            "your impression of the creature, not about the graphics, the sound, " +
            "or the software."

    /** The single scored question. */
    public const val ITEM: String =
        "How alive did this creature seem to you? Move the slider to any point " +
            "between 0 and 100."

    /** The endpoints and midpoint, shown on the scale itself. */
    public val ANCHORS: List<Anchor> = listOf(
        Anchor(
            0,
            "Not alive at all",
            "It seemed like a moving picture or a machine running a sequence. " +
                "Nothing it did came from the creature itself.",
        ),
        Anchor(
            25,
            "Slightly alive",
            "It reacted to me sometimes, but what it did next never seemed to " +
                "come from anything of its own.",
        ),
        Anchor(
            50,
            "Somewhat alive",
            "It seemed to have some states of its own — it wanted things at some " +
                "moments and not others — but I could usually see what it would do.",
        ),
        Anchor(
            75,
            "Mostly alive",
            "It seemed to have its own interests and moods. It sometimes did " +
                "things I did not expect, and those things still made sense for it.",
        ),
        Anchor(
            100,
            "Completely alive",
            "It seemed like a creature with its own inner life, going about its " +
                "own business, that I happened to be visiting.",
        ),
    )

    /**
     * Administered after the second creature only. Secondary, and it cannot
     * rescue a failed primary endpoint.
     */
    public const val FORCED_CHOICE_ITEM: String =
        "Thinking about both creatures you met: which one seemed more alive to " +
            "you? (First / Second)"

    /**
     * A distinctiveness endpoint, kept separate from the forced choice because
     * it asks a different question and must not be pooled with it.
     */
    public const val DISTINCTIVENESS_ITEM: String =
        "Did the two creatures seem like the same kind of creature, or two " +
            "different kinds? (Same / Different)"

    /** One labelled point on the scale. */
    public class Anchor(
        public val value: Int,
        public val label: String,
        public val description: String,
    )

    public fun render(): String = buildString {
        append(INSTRUMENT_ID).append(" v").append(INSTRUMENT_VERSION).append('\n')
        append("PREAMBLE: ").append(PREAMBLE).append("\n\n")
        append("ITEM: ").append(ITEM).append("\n\n")
        append("ANCHORS (0-100, continuous slider; anchors shown, intermediate values allowed)\n")
        for (a in ANCHORS) {
            append("  ").append(a.value.toString().padStart(3))
            append("  ").append(a.label).append('\n')
            append("       ").append(a.description).append('\n')
        }
        append('\n')
        append("SECONDARY (after the second creature only)\n")
        append("  ").append(FORCED_CHOICE_ITEM).append('\n')
        append("  ").append(DISTINCTIVENESS_ITEM).append('\n')
    }
}
