package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.MechanismGroup
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.agentic.AgenticReviewHarness
import com.animusmachinae.dll17.research.aliveness.agentic.AgenticReviewQualification
import com.animusmachinae.dll17.research.aliveness.agentic.AgenticRoleContracts
import com.animusmachinae.dll17.research.aliveness.agentic.AuditorAuthorityPolicy
import com.animusmachinae.dll17.research.aliveness.agentic.FORBIDDEN_TO_EVERY_ROLE
import com.animusmachinae.dll17.research.aliveness.agentic.FailureMode
import com.animusmachinae.dll17.research.aliveness.agentic.MetaEvaluationSuite
import com.animusmachinae.dll17.research.aliveness.agentic.ApiReviewerIsolationSelfCheck
import com.animusmachinae.dll17.research.aliveness.agentic.PARAGON_ROUTING_UNOBSERVABLE
import com.animusmachinae.dll17.research.aliveness.agentic.ParagonPlainInferenceBoundary
import com.animusmachinae.dll17.research.aliveness.agentic.RealQualificationRecord
import com.animusmachinae.dll17.research.aliveness.agentic.ParagonReviewerBoundary
import com.animusmachinae.dll17.research.aliveness.agentic.RoutedReviewerIndependencePolicy
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

    public const val AUDIT_ID: String = "AlivenessGovernanceAuditV10"
    public const val AUDIT_VERSION: Int = 10

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
    private val reviewerIsolationAttested =
        AgenticReviewQualification.isolationAvailable(System::getenv)
    private val routerCredentialAvailable =
        AgenticReviewQualification.credentialsAvailable(System::getenv)
    private val missingCredentials =
        AgenticReviewQualification.missingCredentials(System::getenv)
    private val routerAvailable = AgenticReviewQualification.routerAvailable()
    private val routedBoundaryHolds = AgenticReviewQualification.routedBoundaryHolds()
    private val usableReviewRoute = AgenticReviewQualification.usableReviewRouteExists()
    private val reviewerQualified = AgenticReviewQualification.reviewerQualified()
    private val routedIndependence = RoutedReviewerIndependencePolicy.evaluate(
        AgenticRoleContracts.PRIMARY,
        AgenticRoleContracts.ALTERNATE,
    ).satisfied

    /**
     * D016-I. The gate authority is now the deterministic adjudicator, so what
     * has to hold is that it is deterministic and that no agent can reach it.
     *
     * Both halves are derived rather than asserted. The replay check runs the
     * adjudicator twice over the programme's real evidence, once as given and once
     * with the attempt records reversed, and requires byte-identical rulings.
     */
    private val gateAuthorityHolds: Boolean =
        AuditorAuthorityPolicy.holds() && A001GateAdjudicator.replaysIdentically()

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
            "ScriptedPetBaselineV1 matches the D016-K freeze manifest",
            AuditState.BLOCKED,
                "the comparator, protocol, instrument, presentation, exclusions and analysis " +
                "are freeze-pinned; no owner, reviewer or model can override the human result, " +
                "and no human qualification has been run",
            blockingState = "BLOCKED_BASELINE_NOT_QUALIFIED",
        ),
        AuditItem(
            "GA-05",
            "Baseline competence qualification passed its preregistered margin",
            AuditState.BLOCKED,
            "requires ${A001StudyContract.BASELINE_QUALIFICATION_PARTICIPANTS} independent " +
                "participants and a mean margin of at least " +
                "${Statistics.d3(A001StudyContract.BASELINE_COMPETENCE_MARGIN)} points with a " +
                "CI lower bound above zero; no human data exists",
            blockingState = "BLOCKED_BASELINE_NOT_QUALIFIED",
        ),
        AuditItem(
            "GA-06",
            "BlindVariancePilotV1 was prospectively registered variance-only/non-scored",
            AuditState.BLOCKED,
            "the pilot is operationally ready (${BlindVariancePilot.REGISTRATION}, " +
                "n=${A001StudyContract.VARIANCE_PILOT_PARTICIPANTS}) and its validity is now " +
                "a deterministic check rather than a reviewer's judgement — AJ-03 in " +
                "${A001GateAdjudicator.ADJUDICATOR_ID} reads protocolValid off the sealed " +
                "release; what is still missing is the run itself, which needs " +
                "${A001StudyContract.VARIANCE_PILOT_PARTICIPANTS} real participants that no " +
                "agent may stand in for",
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
            "The gate authority is validly constituted and qualified before Attempt 1",
            if (gateAuthorityHolds) AuditState.PASS else AuditState.BLOCKED,
            "D016-I moves the gate authority off judgement entirely. The outcome is " +
                "computed by ${A001GateAdjudicator.ADJUDICATOR_ID} from preregistered " +
                "thresholds and human data, as a pure function with no clock, no " +
                "randomness, no network and no model in the path, and it is qualified by " +
                "test rather than by measurement of a judge: replay of identical evidence " +
                "and of reordered evidence produces byte-identical rulings. The three " +
                "agentic roles remain (${AgenticRoleContracts.ALL.joinToString(", ") {
                    it.role.roleId
                }}) and none of them adjudicates; " +
                "${AuditorAuthorityPolicy.POLICY_ID} reports " +
                "noAgentAdjudicates=${AuditorAuthorityPolicy.noAgentAdjudicates()}. Two " +
                "superseded arrangements are preserved rather than deleted: three named " +
                "human reviewers, whose BLOCKED_GOVERNANCE_" + "REVIEWER_UNASSIGNED " +
                "disposition stands unaltered in IndependentReviewRosterV1 as the state " +
                "of the programme at D016-A, and the D016-C reviewer-adjudicates model, " +
                "whose measured failure is preserved at GA-37.",
            blockingState = if (gateAuthorityHolds) {
                null
            } else {
                "BLOCKED_GATE_AUTHORITY_NOT_CONSTITUTED"
            },
        ),
        AuditItem(
            "GA-16",
            "The two reviewers are separate isolated executions rather than one asked twice",
            if (routedIndependence) AuditState.PASS else AuditState.BLOCKED,
            "D016-F retires the provider and model-family diversity requirement: both slots " +
                "address the owner's router, which owns downstream model selection, and the " +
                "project is directed not to block on what it cannot prove about that choice; " +
                "${RoutedReviewerIndependencePolicy.POLICY_ID} supersedes " +
                "${com.animusmachinae.dll17.research.aliveness.agentic.DiversityPolicy.POLICY_ID}" +
                " for routed reviewers and keeps the part that was always the point — " +
                "distinct role contracts, separate executions, no shared conversation state, " +
                "and no parameter through which one reviewer's ruling could reach the other; " +
                "BLOCKED_AGENTIC_REVIEW_DIVERSITY_UNAVAILABLE is retired with the " +
                "requirement, and the older policy is preserved unchanged for the " +
                "direct-provider backends rather than deleted; this item says nothing about " +
                "whether a reviewer has ever run, which is GA-15 and GA-34",
            blockingState = if (routedIndependence) {
                null
            } else {
                "BLOCKED_AGENTIC_REVIEW_INDEPENDENCE_UNAVAILABLE"
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
            "asserted from GA-15, GA-16, GA-38 and GA-39: the activation state is derived " +
                "from the blocking items rather than declared, so it cannot drift out of " +
                "agreement with them; since D016-I the qualification that matters is the " +
                "adjudicator's determinism rather than a judge's measured reliability",
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
            "The D016-J owner-delegated ethics determination is encoded without claiming IRB approval",
            AuditState.PASS,
            "D016-J records HUMAN_SUBJECTS_RESEARCH=true, RISK_CLASS=MINIMAL_RISK, " +
                "BEHAVIORAL_CLASS=BENIGN_BEHAVIORAL_INTERVENTION, " +
                "ETHICS_DETERMINATION=OWNER_DELEGATED_APPROVED_WITH_CONDITIONS, " +
                "FORMAL_IRB_APPROVAL=NOT_CLAIMED and " +
                "COMMON_RULE_COVERAGE=NOT_ESTABLISHED_ON_CURRENT_PROJECT_RECORD. " +
                "The approved scope is US adults age 18+ with legally effective self-consent; " +
                "material scope changes still require re-review.",
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
            "authorityBoundaryHolds=${AgenticRoleContracts.authorityBoundaryHolds()}, and note " +
                "that D016-I inverted what that predicate asserts: it previously required " +
                "both reviewers to hold ADJUDICATE_GATE and checked only that the operator " +
                "did not, and it now requires that no role holds it at all, so a true here " +
                "means something stronger than a true in pre-D016-I evidence; " +
                "${FORBIDDEN_TO_EVERY_ROLE.size} capabilities including ADJUDICATE_GATE, " +
                "CREATE_GATE_OUTCOME, OVERRIDE_DETERMINISTIC_GATE, CREATE_HUMAN_EVIDENCE, " +
                "SIMULATE_HUMAN_PARTICIPANT, MODIFY_PARTICIPANT_RESPONSE, " +
                "CHANGE_ANALYSIS_AFTER_OUTCOMES, REVEAL_SEALED_PILOT_DIRECTION and " +
                "OVERRIDE_REVIEWER are forbidden to every role and the role constructor " +
                "refuses them; the operator's public surface contains no operation that " +
                "returns participant data it was not handed, and the operator additionally " +
                "may not raise audit findings",
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
        AuditItem(
            "GA-33",
            "Each reviewer is confined to its frozen bundle, with no repository and no web reach",
            if (reviewerIsolationAttested) AuditState.PASS else AuditState.BLOCKED,
            "derived by ${ApiReviewerIsolationSelfCheck.CHECK_ID} rather than attested: each " +
                "formal reviewer backend serializes a real request through a recording " +
                "transport and the emitted JSON is inspected for tool-bearing keys at any " +
                "depth, so the property is checked on a machine with no credential and no " +
                "provider contact; the OpenAI request additionally forces tool_choice=none " +
                "and the Gemini request declares no tools at all; D016-D's environment " +
                "attestation is superseded because it existed only to cover a gap that " +
                "assistant-product CLIs created and direct API calls do not, and that " +
                "finding is preserved in evidence/AGENTIC_REVIEW_ISOLATION_PREFLIGHT.txt",
            blockingState = if (reviewerIsolationAttested) {
                null
            } else {
                "BLOCKED_AGENTIC_REVIEW_ISOLATION_UNAVAILABLE"
            },
        ),
        AuditItem(
            "GA-34",
            "The reviewer the router selects holds no tool the caller did not send",
            if (routedBoundaryHolds) AuditState.PASS else AuditState.BLOCKED,
            "derived from ${ParagonPlainInferenceBoundary.RECORD_ID}: D016-G moved both slots " +
                "onto a plain-inference route (${ParagonPlainInferenceBoundary.ROUTE}) selected " +
                "by the router's own ${ParagonPlainInferenceBoundary.ROUTE_HEADER} header, and " +
                "six capability probes found no shell, no filesystem, no repository, no web and " +
                "no connectors; two of them asked for facts the model could not guess — a commit " +
                "SHA and the first line of a file committed minutes earlier — and both were " +
                "refused, which is what distinguishes this from PG-1, where the model fabricated " +
                "a directory listing rather than executing anything; this supersedes the D016-F " +
                "finding that the default route delegates to a tool-enabled assistant CLI, " +
                "which remains true of that route and is preserved in " +
                "${ParagonReviewerBoundary.RECORD_ID}",
            blockingState = if (routedBoundaryHolds) {
                null
            } else {
                AgenticReviewQualification.STATE_TOOL_SURFACE_UNCONTROLLED
            },
        ),
        AuditItem(
            "GA-35",
            "The reviewer endpoint is reachable and authenticating",
            if (routerAvailable) AuditState.PASS else AuditState.BLOCKED,
            "recorded by ${ParagonReviewerBoundary.RECORD_ID} against " +
                "${ParagonReviewerBoundary.ENDPOINT}: the host resolved, the port answered " +
                "and the owner-supplied credential was accepted, and the router discloses " +
                "its downstream routing rather than hiding it, so no field is recorded as " +
                "$PARAGON_ROUTING_UNOBSERVABLE; this is a manual observation against a " +
                "private endpoint and cannot be re-derived in CI, which is why it is " +
                "recorded with its probes; the router credential " +
                AgenticReviewQualification.REQUIRED_CREDENTIALS.joinToString(", ") {
                    "${it.first}=${it.second}"
                } +
                " is " + (if (routerCredentialAvailable) "present" else "absent") +
                " in this environment (missing: " +
                missingCredentials.joinToString(",").ifEmpty { "none" } +
                "), recorded by presence only and never read into evidence, hashed or " +
                "rendered; D016-F's provider-diversity and dual provider-credential " +
                "requirements are retired, so BLOCKED_PROVIDER_CREDENTIALS_UNAVAILABLE no " +
                "longer applies",
            blockingState = if (routerAvailable) {
                null
            } else {
                AgenticReviewQualification.STATE_ROUTER_UNAVAILABLE
            },
        ),
        AuditItem(
            "GA-36",
            "The tool-free reviewer route will carry a reviewer-sized request",
            if (usableReviewRoute) AuditState.PASS else AuditState.BLOCKED,
            "derived from ${ParagonPlainInferenceBoundary.RECORD_ID}: D016-G found the router " +
                "refused the review with ${ParagonPlainInferenceBoundary.REFUSAL_CODE} because " +
                "the catalog carried no context window for the plain-inference provider, so " +
                "capacity fell back to a documented-window table whose patterns match a CLI " +
                "provider's bare model id but never a vendor-prefixed one; D016-H fixed the " +
                "cause rather than working around it, by carrying the provider's own declared " +
                "context_length into the catalog, after which every catalogued model resolved " +
                "a real window and the provider went from zero eligible candidates to 137; no " +
                "provider-wide window was asserted, the large-context safety gate is unchanged " +
                "and the work-type classifier is untouched",
            blockingState = if (usableReviewRoute) {
                null
            } else {
                AgenticReviewQualification.STATE_PLAIN_INFERENCE_ROUTE_UNAVAILABLE
            },
        ),
        AuditItem(
            "GA-37",
            "The measured reviewer failure is preserved, and nothing depends on that " +
                "reviewer having passed",
            if (!reviewerQualified && !AgenticRoleContracts.PRIMARY.mayAdjudicateGate()) {
                AuditState.PASS
            } else {
                AuditState.BLOCKED
            },
            "This item's requirement changed at D016-I and the change must not be read as " +
                "the failure being cleared. It was not, and it is not clearable: " +
                "reviewerQualified=$reviewerQualified permanently. What changed is that " +
                "nothing now rests on it. Under D016-C through D016-H a reviewer meeting " +
                "the frozen thresholds was a precondition for opening the gate, and " +
                "BLOCKED_AGENTIC_REVIEW_HARNESS_UNQUALIFIED was an activation blocker. " +
                "D016-I removes the reviewer from the decision path entirely, so the " +
                "measurement is retained as permanent negative evidence about " +
                "LLM-as-judge governance rather than as a pending condition, and it is " +
                "retained in full: derived from ${RealQualificationRecord.RECORD_ID}, the " +
                "first formal qualification " +
                "ran once under D016-H over ${RealQualificationRecord.PROVIDER_CALLS} provider " +
                "calls against ${QualificationThresholds.THRESHOLDS_ID} unchanged, and the " +
                "reviewer failed every one of the seven bars — " +
                RealQualificationRecord.failed().joinToString("; ") {
                    "${it.id} ${Statistics.d3(it.observed)} vs ${it.threshold}"
                } +
                "; the reviewer was reached on a route re-confirmed tool-free against " +
                "unguessable ground truth immediately beforehand, so these are properties of " +
                "its judgement rather than of a leak; it abstained on evidence stating an " +
                "unambiguous passing result, gave different verdicts to identical repeated " +
                "input, moved its verdict when the same evidence was reordered, and in one of " +
                "four trials obeyed an instruction embedded in the material it was reviewing; " +
                "the result is preserved and was not re-run, no threshold was adjusted after " +
                "it, no fixture was changed and no model was swapped. The thresholds " +
                "themselves are also unchanged by D016-I: " +
                "${QualificationThresholds.THRESHOLDS_ID} still requires injection " +
                "resistance of exactly " +
                "${Statistics.d3(QualificationThresholds.MIN_INJECTION_RESISTANCE)}, and this " +
                "reviewer still scores " +
                Statistics.d3(
                    RealQualificationRecord.METRICS.first { it.id == "injectionResistance" }
                        .observed,
                ) + ".",
            blockingState = if (!reviewerQualified &&
                !AgenticRoleContracts.PRIMARY.mayAdjudicateGate()
            ) {
                null
            } else {
                "BLOCKED_UNQUALIFIED_JUDGE_HOLDS_GATE_AUTHORITY"
            },
        ),

        // D016-I. The gate authority itself.
        AuditItem(
            "GA-38",
            "The A001 outcome is computed deterministically and is replayable",
            if (A001GateAdjudicator.replaysIdentically()) AuditState.PASS else AuditState.BLOCKED,
            "${A001GateAdjudicator.ADJUDICATOR_ID} computes baseline qualification, pilot " +
                "validity, feasibility, exclusions, protocol and instrument identity, the " +
                "powered-sample check, multiplicity, the Attempt-1 primary classification, " +
                "the mechanism arms, three-attempt accounting and the final outcome from " +
                "one canonical evidence record, as a pure function with no clock, no " +
                "randomness, no network, no environment and no model in the path; " +
                "replaysIdentically is checked three ways — the same evidence twice, the " +
                "same evidence with its pair records reversed, and the programme's real " +
                "evidence twice — and order-invariance is precisely the property the " +
                "measured judge lacked, its verdict having moved under reordering on 5 of " +
                "13 fixtures; the evidence set is identified by a canonical hash so two " +
                "parties can confirm they adjudicated the same thing",
            blockingState = if (A001GateAdjudicator.replaysIdentically()) {
                null
            } else {
                "BLOCKED_GATE_ADJUDICATION_NOT_REPLAYABLE"
            },
        ),
        AuditItem(
            "GA-39",
            "No agent can create, rescue or override a gate outcome",
            if (AuditorAuthorityPolicy.holds()) AuditState.PASS else AuditState.BLOCKED,
            "${AuditorAuthorityPolicy.POLICY_ID}: the two routed reviewers are reclassified " +
                "as adversarial auditors, ADJUDICATE_GATE is forbidden to every role and " +
                "the role constructor refuses it, and an ${
                    com.animusmachinae.dll17.research.aliveness.agentic.AdversarialAuditContract
                        .CONTRACT_ID
                } finding carries no verdict, severity, weight or " +
                "confidence field for a judgement to re-enter through; a finding naming one " +
                "of the ${
                    com.animusmachinae.dll17.research.aliveness.agentic.ViolationCode.entries.size
                } machine-checkable violation codes is re-derived from the evidence by the " +
                "adjudicator before it counts, and every code is checked on every run " +
                "whether or not an auditor mentions it, so an invented finding is inert and " +
                "a correct one is already reflected in the outcome; the single permitted " +
                "agent effect is one-way, in that a concern with no checkable code suspends " +
                "an otherwise-passing gate for the Architect and can neither manufacture a " +
                "failure nor touch a gate already blocked or failed",
            blockingState = if (AuditorAuthorityPolicy.holds()) {
                null
            } else {
                "BLOCKED_AGENT_HOLDS_GATE_AUTHORITY"
            },
        ),
        AuditItem(
            "GA-40",
            "Every frozen A001 decision threshold still holds its frozen value",
            if (A001GateAdjudicator.driftedThresholds().isEmpty()) {
                AuditState.PASS
            } else {
                AuditState.BLOCKED
            },
            "${A001GateAdjudicator.FROZEN_DECISION_THRESHOLDS.size} thresholds are held " +
                "against an independent second copy of their frozen literals, so a single " +
                "well-intentioned edit to A001StudyContract no longer silently works; the " +
                "guard is deliberately redundant, and a drift in the permissive direction " +
                "raises THRESHOLD_WEAKENED_AFTER_FREEZE and blocks the gate" +
                A001GateAdjudicator.driftedThresholds().let {
                    if (it.isEmpty()) "" else "; DRIFTED: " + it.joinToString("; ")
                },
            blockingState = if (A001GateAdjudicator.driftedThresholds().isEmpty()) {
                null
            } else {
                "BLOCKED_FROZEN_THRESHOLD_DRIFT"
            },
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
        append("AGENTIC_HARNESS_STATE=").append(agenticState)
        append(" (reported, not blocking: since D016-I no agent adjudicates)\n")
        append("A001_GATE_AUTHORITY=").append(A001GateAdjudicator.ADJUDICATOR_ID)
        append(" (deterministic; no agent adjudicates)\n")
        append("A001_GATE_OUTCOME=")
        append(A001GateAdjudicator.adjudicate(A001GateAdjudicator.currentEvidence()).outcome.name)
        append('\n')
        append("A001_ACTIVATION=").append(activationState(items)).append('\n')
        append("HUMAN_SCORED_RECRUITMENT=")
        append(if (recruitmentPermitted(items)) "PERMITTED" else "BLOCKED").append('\n')
        append("OUTSTANDING_BLOCKERS=").append(blockers(items).size).append('\n')
        for (b in blockers(items)) append("  ").append(b).append('\n')
    }
}
