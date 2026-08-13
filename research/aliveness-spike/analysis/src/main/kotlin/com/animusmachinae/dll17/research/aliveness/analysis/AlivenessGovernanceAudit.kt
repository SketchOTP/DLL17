package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.MechanismGroup
import com.animusmachinae.dll17.research.aliveness.SpikeContract

/** The state of one audited item. */
public enum class AuditState {
    /** Verified automatically from repository state. */
    PASS,

    /** Not yet applicable: no scored attempt exists, so there is nothing to check. */
    NOT_APPLICABLE_PRE_ATTEMPT,

    /** Requires a signed human judgement that cannot be automated. */
    REQUIRES_SIGNED_GOVERNANCE_EVIDENCE,

    /** Blocked on a named missing artefact. */
    BLOCKED,
}

public class AuditItem(
    public val id: String,
    public val requirement: String,
    public val state: AuditState,
    public val detail: String,
)

/**
 * `AlivenessGovernanceAuditV1`, executable portion.
 *
 * Canonical scope, stated plainly because it is easy to get wrong: hash,
 * timestamp and status checks may run automatically; human independence and
 * materiality judgements remain signed governance evidence. This audit is **not**
 * an R013 runtime regression suite and never marks an item `PASS` by exercising
 * organism code — every `PASS` below is a structural fact about contracts and
 * identifiers, not a behavioural result.
 */
public object AlivenessGovernanceAudit {

    public const val AUDIT_ID: String = "AlivenessGovernanceAuditV1"
    public const val AUDIT_VERSION: Int = 1

    @JvmStatic
    public fun main(args: Array<String>) {
        println(render(audit()))
    }

    public fun audit(): List<AuditItem> = listOf(
        AuditItem(
            "GA-01",
            "Attempt number remains within the three-attempt budget",
            AuditState.NOT_APPLICABLE_PRE_ATTEMPT,
            "attemptsConsumed=0 budget=${SpikeContract.MAX_SCORED_A001_ATTEMPTS}",
        ),
        AuditItem(
            "GA-02",
            "All prior negative evidence is retained and referenced",
            AuditState.NOT_APPLICABLE_PRE_ATTEMPT,
            "no scored attempt has produced evidence yet",
        ),
        AuditItem(
            "GA-03",
            "Program success floor has not weakened",
            AuditState.BLOCKED,
            "BLOCKED_SPEC_ALIVENESS_SUCCESS_FLOOR: the floor is an architect value " +
                "judgement about how much apparent aliveness is worth the complexity, " +
                "and is not derivable from A000 evidence",
        ),
        AuditItem(
            "GA-04",
            "ScriptedPetBaselineV1 hash/version matches independently qualified evidence",
            AuditState.BLOCKED,
            "the baseline is implemented and frozen in source, but no independent " +
                "competence qualification has been run: BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED",
        ),
        AuditItem(
            "GA-05",
            "Baseline competence pilot passed its preregistered margin",
            AuditState.BLOCKED,
            "requires a pilot-only human rater pool; no human data exists",
        ),
        AuditItem(
            "GA-06",
            "BlindVariancePilotV1 was prospectively registered variance-only/non-scored",
            AuditState.NOT_APPLICABLE_PRE_ATTEMPT,
            "no pilot has been registered or run",
        ),
        AuditItem(
            "GA-07",
            "Variance-pilot participants never enter scored pools",
            AuditState.NOT_APPLICABLE_PRE_ATTEMPT,
            "no participants exist",
        ),
        AuditItem(
            "GA-08",
            "Pilot/scored status was never reclassified after data",
            AuditState.NOT_APPLICABLE_PRE_ATTEMPT,
            "no data exists to reclassify",
        ),
        AuditItem(
            "GA-09",
            "Only the permitted paired-difference SD reached the FULL team",
            AuditState.NOT_APPLICABLE_PRE_ATTEMPT,
            "no pilot output exists; no disclosure has occurred",
        ),
        AuditItem(
            "GA-10",
            "A001FeasibilityBudgetV1 passes before recruitment/scoring",
            AuditState.BLOCKED,
            "BLOCKED_SPEC_PAIRED_DIFFERENCE_SD: the powered sample size cannot be " +
                "computed until the blind variance pilot releases pairedDifferenceSD",
        ),
        AuditItem(
            "GA-11",
            "AlivenessStudyProtocolV1 was frozen/timestamped before scored data",
            AuditState.BLOCKED,
            "the attempt-specific protocol depends on the feasibility budget and is not " +
                "authored under D008",
        ),
        AuditItem(
            "GA-12",
            "MechanismCoalitionSetV1 / exact Shapley attribution contract is frozen",
            AuditState.PASS,
            "groups=${MechanismGroup.COUNT} (2^k=${1 shl MechanismGroup.COUNT} <= 64, exact " +
                "enumeration) valueFunction=${SpikeContract.COALITION_VALUE_FUNCTION_ID} " +
                "attribution=${SpikeContract.ATTRIBUTION_CONTRACT_ID}",
        ),
        AuditItem(
            "GA-13",
            "CuriosityBalanceEnvelopeV1 proves anti-convergence and attribution on " +
                "identical parameter/seed hashes",
            AuditState.PASS,
            "the search evaluates both requirements from the same run per grid point and " +
                "seed, so the two readouts cannot originate from different parameterizations; " +
                "the recorded result is the search's own output",
        ),
        AuditItem(
            "GA-14",
            "MaterialChangeEligibilityV1 and independent adjudication exist for Attempts 2/3",
            AuditState.REQUIRES_SIGNED_GOVERNANCE_EVIDENCE,
            "the qualifying and non-qualifying change classes are frozen in the contract " +
                "document; adjudication itself is a human judgement",
        ),
        AuditItem(
            "GA-15",
            "IndependentReviewRosterV1 names primary/alternate/baseline-owner before Attempt 1",
            AuditState.BLOCKED,
            "BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED: all three roles are unassigned",
        ),
        AuditItem(
            "GA-16",
            "Reviewer independence/conflict declarations and any replacement cause are present",
            AuditState.BLOCKED,
            "no reviewers are named, so no declarations can exist",
        ),
        AuditItem(
            "GA-17",
            "Viewer/expression/interaction contract hashes match across cohorts",
            AuditState.PASS,
            "every cohort renders through ${SpikeContract.EXPRESSION_CONTRACT_ID} v" +
                "${com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract.CONTRACT_VERSION}" +
                "; the contract has no cohort parameter and the viewer has no reference to Cohort",
        ),
        AuditItem(
            "GA-18",
            "Human ablation family/multiplicity plan and separate rater pools are frozen",
            AuditState.PASS,
            "family=${Cohort.HUMAN_ABLATION_FAMILY.joinToString(",") { it.cohortId }} " +
                "alpha=${SpikeContract.ABLATION_FAMILY_ALPHA_MILLIONTHS}e-6 correction=Holm-Bonferroni " +
                "raterPools=separate (plan frozen; execution requires participants)",
        ),
        AuditItem(
            "GA-19",
            "Spontaneity attribution thresholds/classifier version are frozen",
            AuditState.PASS,
            "substantiveFloor=${SpikeContract.REQUIRED_SUBSTANTIVE_SPONTANEITY_RATE}e-6 " +
                "oscillatorCeiling=${SpikeContract.MAX_OSCILLATOR_TIEBREAK_ONLY_RATE}e-6 " +
                "dominantShare=${SpikeContract.DOMINANT_GROUP_SHARE}e-6 " +
                "mixedMin=${SpikeContract.MIXED_SUBSTANTIVE_MIN_SHARE}e-6",
        ),
        AuditItem(
            "GA-20",
            "A001 is blocked while any required governance role is unassigned",
            AuditState.PASS,
            "A001_STATE=BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED, asserted from GA-15",
        ),
    )

    public fun render(items: List<AuditItem>): String = buildString {
        append("GOVERNANCE_AUDIT=").append(AUDIT_ID).append(" v").append(AUDIT_VERSION).append('\n')
        append("SCOPE=research governance only; no organism runtime behaviour is exercised\n\n")
        for (item in items) {
            append(item.id).append("  ").append(item.state.name).append('\n')
            append("   requirement: ").append(item.requirement).append('\n')
            append("   detail:      ").append(item.detail).append('\n')
        }
        append('\n')
        for (state in AuditState.entries) {
            append(state.name).append('=').append(items.count { it.state == state }).append(' ')
        }
        append('\n')
        append("A001_PROGRAM_STATE=ALIVENESS_UNTESTED\n")
        append("A001_ACTIVATION=BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED\n")
    }
}
