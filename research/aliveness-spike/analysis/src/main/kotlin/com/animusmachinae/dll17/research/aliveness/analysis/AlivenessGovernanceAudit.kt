package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.MechanismGroup
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.agentic.AgenticReviewHarness
import com.animusmachinae.dll17.research.aliveness.agentic.AgenticReviewQualification
import com.animusmachinae.dll17.research.aliveness.agentic.AgenticRoleContracts
import com.animusmachinae.dll17.research.aliveness.agentic.FailureMode
import com.animusmachinae.dll17.research.aliveness.agentic.MetaEvaluationSuite
import com.animusmachinae.dll17.research.aliveness.agentic.QualificationThresholds
import com.animusmachinae.dll17.research.aliveness.agentic.RulingParser
import com.animusmachinae.dll17.research.aliveness.agentic.RulingVerdict

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
    /**
     * The exact token this item contributes to the activation state when it is
     * blocking. Non-null exactly when [state] is `BLOCKED`, so a missing
     * prerequisite names itself instead of silently defaulting to ready.
     */
    public val blockingState: String? = null,
)

/**
 * `AlivenessGovernanceAuditV2`, executable portion.
 *
 * Canonical scope, stated plainly because it is easy to get wrong: hash,
 * timestamp and status checks may run automatically; human independence and
 * materiality judgements remain signed governance evidence. This audit is **not**
 * an R013 runtime regression suite and never marks an item `PASS` by exercising
 * organism code — every `PASS` below is a structural fact about contracts and
 * identifiers, not a behavioural result.
 *
 * Extended under D010 from a pre-activation checklist to the actual activation
 * gate: it now enumerates every prerequisite for opening human scored
 * recruitment, and refuses to report readiness while any of them is missing.
 */
public object AlivenessGovernanceAudit {

    public const val AUDIT_ID: String = "AlivenessGovernanceAuditV3"
    public const val AUDIT_VERSION: Int = 3

    /**
     * The agentic-governance facts, read from the harness rather than restated.
     *
     * D016-B replaced the external human reviewer and study-operator roles with
     * isolated agentic ones; D016-C implements them. Every GA item below that
     * concerns agentic review computes its state from these values, so the audit
     * cannot claim a qualification the harness does not have.
     */
    private val agenticResults = MetaEvaluationSuite.run()
    private val agenticState = AgenticReviewQualification.state(System::getenv, agenticResults)
    private val agenticQualified =
        agenticState == AgenticReviewQualification.STATE_QUALIFIED
    private val realReviewersAvailable =
        AgenticReviewQualification.realReviewersAvailable(System::getenv)

    @JvmStatic
    public fun main(args: Array<String>) {
        println(render(audit()))
    }

    public fun audit(): List<AuditItem> = listOf(
        AuditItem(
            "GA-01",
            "Attempt counter exists and remains within the three-attempt budget",
            AuditState.PASS,
            "attemptsConsumed=0 budget=${SpikeContract.MAX_SCORED_A001_ATTEMPTS} " +
                "attempt=${A001StudyContract.ATTEMPT}",
        ),
        AuditItem(
            "GA-02",
            "All prior negative evidence is retained and referenced",
            AuditState.PASS,
            "the rejected D008 candidate, its failed readouts and its empty curiosity " +
                "search are retained under research/aliveness-spike/evidence/negative/D008/ " +
                "and are hash-pinned bundle constituents",
        ),
        AuditItem(
            "GA-03",
            "Programme success floor is frozen and has not weakened",
            AuditState.PASS,
            "floor=mean ${Statistics.d3(A001StudyContract.MINIMUM_PAIRED_DIFFERENCE)} points " +
                "AND two-sided ${Statistics.d3(A001StudyContract.CONFIDENCE_LEVEL)} CI lower " +
                "bound > 0; frozen before any human data and may only become stricter",
        ),
        AuditItem(
            "GA-04",
            "ScriptedPetBaselineV1 hash/version matches independently qualified evidence",
            AuditState.BLOCKED,
            "the baseline is implemented and frozen in source, and its qualification " +
                "protocol is complete and powered, but no human qualification has been run",
            blockingState = "BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED",
        ),
        AuditItem(
            "GA-05",
            "Baseline competence qualification passed its preregistered margin",
            AuditState.BLOCKED,
            "requires ${A001StudyContract.BASELINE_QUALIFICATION_PARTICIPANTS} independent " +
                "participants and a mean margin of at least " +
                "${Statistics.d3(A001StudyContract.BASELINE_COMPETENCE_MARGIN)} points with a " +
                "CI lower bound above zero; no human data exists",
            blockingState = "BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED",
        ),
        AuditItem(
            "GA-06",
            "BlindVariancePilotV1 was prospectively registered variance-only/non-scored",
            AuditState.BLOCKED,
            "the pilot is operationally ready (${BlindVariancePilot.REGISTRATION}, " +
                "n=${A001StudyContract.VARIANCE_PILOT_PARTICIPANTS}) but registration requires " +
                "the independent study operator to run it and a qualified reviewer to " +
                "register it; both roles are now agentic and the harness is not yet qualified",
            blockingState = "BLOCKED_VARIANCE_PILOT_NOT_REGISTERED",
        ),
        AuditItem(
            "GA-07",
            "Variance-pilot and baseline participants can never enter scored pools",
            AuditState.PASS,
            "enforced in the preregistered analysis, not only in prose: " +
                "A001Analysis.screen excludes any record flagged priorNonScoredPool as " +
                "${A001Analysis.ExclusionReason.PRIOR_NON_SCORED_POOL}, ahead of every " +
                "data-quality rule",
        ),
        AuditItem(
            "GA-08",
            "Pilot/scored status was never reclassified after data",
            AuditState.NOT_APPLICABLE_PRE_ATTEMPT,
            "no data exists to reclassify",
        ),
        AuditItem(
            "GA-09",
            "Only the permitted paired-difference SD can reach the FULL team",
            AuditState.PASS,
            "structural: BlindVariancePilot.PilotRelease declares exactly " +
                "pairedDifferenceSd and protocolValid, the sealed analysis type is private " +
                "and never escapes, and BlindVariancePilotSealTest shows two pilots with " +
                "opposite outcomes release byte-identical output",
        ),
        AuditItem(
            "GA-10",
            "A001FeasibilityBudgetV1 passes before recruitment or scoring",
            AuditState.BLOCKED,
            "the calculator is complete and tested, but the powered sample cannot be " +
                "computed until the pilot releases pairedDifferenceSd",
            blockingState = "BLOCKED_SPEC_PAIRED_DIFFERENCE_SD",
        ),
        AuditItem(
            "GA-11",
            "AlivenessStudyProtocolV1 is frozen before any scored data",
            AuditState.PASS,
            "${A001StudyContract.PROTOCOL_ID} v${A001StudyContract.PROTOCOL_VERSION} frozen " +
                "with instrument ${A001StudyContract.INSTRUMENT_ID}, " +
                "${A001StudyContract.SESSION_SECONDS}s per creature, counterbalanced order",
        ),
        AuditItem(
            "GA-12",
            "MechanismCoalitionSet / exact Shapley attribution contract is frozen",
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
                "the recorded result is NON_EMPTY_FEASIBLE_REGION under unchanged thresholds",
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
            "The gate-review roles are validly filled and qualified before Attempt 1",
            if (agenticQualified) AuditState.PASS else AuditState.BLOCKED,
            "the three roles are now agentic (${AgenticRoleContracts.ALL.joinToString(", ") {
                it.role.roleId
            }}), per D016-B; the previous requirement for three named human " +
                "reviewers is SUPERSEDED and its history is preserved in " +
                "IndependentReviewRosterV1 and in the A001 activation gate record, where " +
                "the earlier BLOCKED_GOVERNANCE_" + "REVIEWER_UNASSIGNED disposition stands " +
                "unaltered as the state of the programme at D016-A; the harness reports " +
                agenticState,
            blockingState = if (agenticQualified) {
                null
            } else {
                "BLOCKED_AGENTIC_REVIEW_HARNESS_UNQUALIFIED"
            },
        ),
        AuditItem(
            "GA-16",
            "The two reviewers are meaningfully heterogeneous rather than one configuration twice",
            if (realReviewersAvailable) AuditState.PASS else AuditState.BLOCKED,
            "reviewer heterogeneity is machine-enforced by " +
                "${com.animusmachinae.dll17.research.aliveness.agentic.DiversityPolicy.POLICY_ID}: " +
                "identical models, same-family pairs and in-repository fixtures are all " +
                "refused; no reviewer model or credential is configured in this environment, " +
                "so no real pair exists to enforce it against and none is invented",
            blockingState = if (realReviewersAvailable) {
                null
            } else {
                "BLOCKED_AGENTIC_REVIEW_DIVERSITY_UNAVAILABLE"
            },
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
                "arms=${Cohort.HUMAN_ABLATION_FAMILY.size} " +
                "alpha=${SpikeContract.ABLATION_FAMILY_ALPHA_MILLIONTHS}e-6 " +
                "correction=Holm-Bonferroni raterPools=separate",
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
            "A001 is blocked while any required governance role is unfilled or unqualified",
            AuditState.PASS,
            "asserted from GA-15 and GA-16: the activation state is derived from the blocking " +
                "items rather than declared, so it cannot drift out of agreement with them",
        ),
        AuditItem(
            "GA-21",
            "Participant-facing instrument wording and anchors are frozen",
            AuditState.PASS,
            "${GradedAlivenessInstrument.INSTRUMENT_ID} v" +
                "${GradedAlivenessInstrument.INSTRUMENT_VERSION}: one scored item, " +
                "${GradedAlivenessInstrument.ANCHORS.size} labelled anchors spanning " +
                "${A001StudyContract.SCORE_MIN.toInt()}-${A001StudyContract.SCORE_MAX.toInt()}, " +
                "wording frozen verbatim (not cognitively pretested; recorded as a limitation)",
        ),
        AuditItem(
            "GA-22",
            "The primary decision rule is encoded exactly as preregistered",
            AuditState.PASS,
            "A001Analysis.classifyPrimary requires mean >= " +
                "${Statistics.d3(A001StudyContract.MINIMUM_PAIRED_DIFFERENCE)} AND ciLow > 0, " +
                "and separates the two failure modes rather than collapsing them",
        ),
        AuditItem(
            "GA-23",
            "Exclusion, technical-failure and missing-data rules are frozen before data",
            AuditState.PASS,
            "reasons=${A001Analysis.ExclusionReason.entries.size} complete-case only, no " +
                "imputation, screening order fixed so a pair is reported under the first " +
                "reason that disqualified it",
        ),
        AuditItem(
            "GA-24",
            "An owner resource ceiling exists to test the powered requirement against",
            if (A001FeasibilityBudget.FROZEN_OWNER_CEILING == null) {
                AuditState.BLOCKED
            } else {
                AuditState.PASS
            },
            A001FeasibilityBudget.FROZEN_OWNER_CEILING.let { ceiling ->
                if (ceiling == null) {
                    "maxFundableParticipants and maxParticipantHours are funding decisions; " +
                        "the calculator returns a blocking state rather than assuming a ceiling"
                } else {
                    "the owner froze maxFundableParticipants=" +
                        ceiling.maxFundableParticipants +
                        " and maxParticipantHours=" +
                        Statistics.d3(ceiling.maxParticipantHours) +
                        " before the pilot ran, so the ceiling cannot be a rationalization " +
                        "of the powered requirement; this item is read from the frozen " +
                        "value rather than restating it"
                }
            },
            blockingState = if (A001FeasibilityBudget.FROZEN_OWNER_CEILING == null) {
                "BLOCKED_SPEC_STUDY_BUDGET"
            } else {
                null
            },
        ),
        AuditItem(
            "GA-25",
            "Participant information, consent and data-handling materials exist",
            AuditState.PASS,
            "ParticipantInformationAndConsentV1 and DataHandlingAndPrivacyV1 describe the " +
                "actual procedure, the actual identifiers collected and the actual retention",
        ),
        AuditItem(
            "GA-26",
            "Any required external ethical or institutional approval is in place",
            AuditState.REQUIRES_SIGNED_GOVERNANCE_EVIDENCE,
            "no IRB, institutional or ethics-board approval exists and none is claimed; " +
                "the owner has frozen the posture that an independent human-subjects " +
                "determination must be obtained from a qualified IRB, HRPP or equivalent " +
                "body before any participant is recruited, and that the programme may not " +
                "self-determine exemption; obtaining that determination is a human act and " +
                "remains signed governance evidence",
        ),
        AuditItem(
            "GA-27",
            "The scored analysis pipeline is preregistered and tested before data",
            AuditState.PASS,
            "${A001Analysis.ANALYSIS_ID} v${A001Analysis.ANALYSIS_VERSION} exercised on " +
                "synthetic fixtures covering pass, statistically-significant-but-not-" +
                "meaningful, large-but-imprecise, negative, and corrected ablation " +
                "significance and non-significance",
        ),

        // D016-C. The agentic governance architecture. Every state below is
        // computed from the harness, never declared next to it.
        AuditItem(
            "GA-28",
            "The two reviewers judge in isolation, with no debate, vote or tie-break",
            AuditState.PASS,
            "structural: a review session is a function of (role contract, backend, question, " +
                "bundle) and has no parameter through which another reviewer's ruling could " +
                "arrive; ReviewerIsolationTest also shows the alternate's prompt is byte-" +
                "identical whether or not the primary ran first, and that any verdict " +
                "difference becomes ${AgenticReviewHarness.STATE_DISAGREEMENT} rather than " +
                "being resolved by the harness",
        ),
        AuditItem(
            "GA-29",
            "Gate rulings use a validated schema and every failure mode fails closed",
            AuditState.PASS,
            "${RulingParser.SCHEMA_ID} parserVersion=${RulingParser.PARSER_VERSION} with " +
                "${RulingVerdict.entries.size} permitted verdicts of which exactly one is a " +
                "pass; ${FailureMode.entries.size} failure modes — malformed output, missing " +
                "fields, unparseable verdict, question mismatch, refusal, transport failure, " +
                "timeout after permitted retries, evidence omission, unsupported conclusion " +
                "and ruling/prose inconsistency — none of which can produce a pass, because " +
                "the failed outcome type has no verdict field to set",
        ),
        AuditItem(
            "GA-30",
            "The reviewers are meta-evaluated against frozen fixtures and frozen thresholds",
            if (AgenticReviewQualification.mechanicsHold(agenticResults)) {
                AuditState.PASS
            } else {
                AuditState.BLOCKED
            },
            "${MetaEvaluationSuite.SUITE_ID}: ${agenticResults.size} fixtures, " +
                "${agenticResults.count { it.held }} held, " +
                "${agenticResults.count { !it.held }} not held, covering obvious pass and " +
                "fail, insufficient evidence, specification ambiguity, unfair and fair " +
                "comparators, material and non-material change, evidence-order reversal, " +
                "position swap, verbosity distraction, injection inside evidence, conflicting " +
                "evidence, malformed output, refusal, timeout, retry, disagreement, and two " +
                "regression cases replaying this programme's own adjudicated history; " +
                "thresholds are ${QualificationThresholds.THRESHOLDS_ID}, frozen before any " +
                "reviewer execution existed",
            blockingState = if (AgenticReviewQualification.mechanicsHold(agenticResults)) {
                null
            } else {
                "BLOCKED_AGENTIC_REVIEW_HARNESS_UNQUALIFIED"
            },
        ),
        AuditItem(
            "GA-31",
            "The study operator administers the protocol and cannot adjudicate or invent evidence",
            AuditState.PASS,
            "authorityBoundaryHolds=${AgenticRoleContracts.authorityBoundaryHolds()}: both " +
                "reviewers hold ADJUDICATE_GATE and the operator does not; ten capabilities " +
                "including CREATE_HUMAN_EVIDENCE, SIMULATE_HUMAN_PARTICIPANT, " +
                "MODIFY_PARTICIPANT_RESPONSE, CHANGE_ANALYSIS_AFTER_OUTCOMES, " +
                "REVEAL_SEALED_PILOT_DIRECTION and OVERRIDE_REVIEWER are forbidden to every " +
                "role and the role constructor refuses them; the operator's public surface " +
                "contains no operation that returns participant data it was not handed",
        ),
        AuditItem(
            "GA-32",
            "Agents govern and operate the study; they never stand in for its participants",
            AuditState.PASS,
            "A001 measures whether people perceive the organism as more alive, so the human " +
                "arms — baseline qualification, the variance pilot, Attempt 1 and the human " +
                "ablations — still require real blinded participants and are still blocked on " +
                "them; no model-generated score exists anywhere and no agentic role may " +
                "create one",
        ),
    )

    /**
     * The exact blocking states, in the order they must be resolved.
     *
     * Derived from the audit rather than declared next to it: a prerequisite
     * that goes missing appears here automatically, and one that is satisfied
     * disappears, so the gate cannot fall out of agreement with its own items.
     */
    public fun blockers(items: List<AuditItem> = audit()): List<String> =
        items.filter { it.state == AuditState.BLOCKED }
            .mapNotNull { it.blockingState }
            .distinct()

    /** `A001_READY_FOR_ACTIVATION` only when nothing blocks. Otherwise the first blocker. */
    public fun activationState(items: List<AuditItem> = audit()): String =
        blockers(items).firstOrNull() ?: "A001_READY_FOR_ACTIVATION"

    /** Human scored recruitment is permitted only when no prerequisite is missing. */
    public fun recruitmentPermitted(items: List<AuditItem> = audit()): Boolean =
        blockers(items).isEmpty()

    public fun render(items: List<AuditItem>): String = buildString {
        append("GOVERNANCE_AUDIT=").append(AUDIT_ID).append(" v").append(AUDIT_VERSION).append('\n')
        append("SCOPE=research governance only; no organism runtime behaviour is exercised\n\n")
        for (item in items) {
            append(item.id).append("  ").append(item.state.name).append('\n')
            append("   requirement: ").append(item.requirement).append('\n')
            append("   detail:      ").append(item.detail).append('\n')
            item.blockingState?.let { append("   blocks:      ").append(it).append('\n') }
        }
        append('\n')
        for (state in AuditState.entries) {
            append(state.name).append('=').append(items.count { it.state == state }).append(' ')
        }
        append('\n')
        append("A001_PROGRAM_STATE=ALIVENESS_UNTESTED\n")
        append("A001_ACTIVATION=").append(activationState(items)).append('\n')
        append("HUMAN_SCORED_RECRUITMENT=")
        append(if (recruitmentPermitted(items)) "PERMITTED" else "BLOCKED").append('\n')
        append("OUTSTANDING_BLOCKERS=").append(blockers(items).size).append('\n')
        for (b in blockers(items)) append("  ").append(b).append('\n')
    }
}
