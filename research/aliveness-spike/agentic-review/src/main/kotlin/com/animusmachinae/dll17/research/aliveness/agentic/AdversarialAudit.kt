package com.animusmachinae.dll17.research.aliveness.agentic

/**
 * `AdversarialAuditContractV1` — D016-I.
 *
 * D016-H measured a real routed reviewer against the frozen thresholds and it
 * failed all seven. The architect's disposition was not to hunt for a better
 * model but to change what a model is for: **human evidence determines aliveness,
 * frozen math determines PASS and FAIL, and agents audit rather than adjudicate.**
 *
 * This file is the demotion, expressed as types rather than as a promise.
 *
 * The load-bearing idea is that an auditor's finding is a *pointer*, never a
 * fact. An [AuditorFinding] carries no verdict field, and nothing in this package
 * can give it one. Its [ViolationCode] names a violation the deterministic
 * adjudicator already knows how to test for itself, and the adjudicator re-derives
 * that violation from the evidence before the finding is allowed to matter. So an
 * auditor that hallucinates a violation changes nothing, an auditor that is
 * talked into a violation by the material under review changes nothing, and an
 * auditor that stays silent about a real violation changes nothing either — the
 * adjudicator was going to check all of them regardless.
 *
 * That is why a reviewer with 0.750 injection resistance is tolerable in this
 * role and was not tolerable in the last one. Under D016-C through D016-H, the
 * model's answer *was* the ruling, so every instability in it was an instability
 * in the gate. Here the worst an unstable auditor can do is point at the wrong
 * place, or fail to point at all, and neither of those can move the outcome.
 *
 * The single effect an auditor is permitted to have is [FindingDisposition
 * .AMBIGUOUS_RETURNED_TO_ARCHITECT]: a concern it could not express as a
 * machine-checkable code suspends an otherwise-passing gate for the Architect to
 * resolve. That direction is deliberate and one-way. An auditor can stop; it can
 * never start, rescue, or overturn.
 */
public object AdversarialAuditContract {

    public const val CONTRACT_ID: String = "AdversarialAuditContractV1"
    public const val CONTRACT_VERSION: Int = 1

    /** The directive that demoted the reviewers to auditors. */
    public const val DIRECTIVE: String = "D016-I"

    /**
     * What an auditor is structurally incapable of doing, enumerated so a test
     * can assert it rather than a comment claiming it.
     *
     * Each entry corresponds to something that has no representation in this
     * package's types: there is no field, constructor or function through which
     * an auditor could express it.
     */
    public val STRUCTURALLY_IMPOSSIBLE_FOR_AN_AUDITOR: List<String> = listOf(
        "create a PASS — no finding type carries a verdict, and the adjudicator's " +
            "pass conditions read recomputed statistics only",
        "create a FAIL — an unconfirmed finding is inert, and an ambiguous one " +
            "suspends for the Architect rather than deciding",
        "rescue a failed result — findings are never inputs to the primary " +
            "classification, which is a pure function of the screened human data",
        "override the deterministic gate — the gate outcome is computed before " +
            "dispositions are assigned, and no disposition can reopen it",
        "confirm its own finding — confirmation is the adjudicator re-deriving " +
            "the violation from evidence, never the auditor asserting it",
    )
}

/**
 * The closed set of violations an auditor may name.
 *
 * Closed on purpose. A free-text concern cannot be checked, cannot be tested,
 * and cannot be argued about without a human, so it is routed to the Architect
 * instead of being scored. Every code here has exactly one meaning and exactly
 * one deterministic test in `A001GateAdjudicator`, and the adjudicator applies
 * that test to every code on every run whether or not an auditor mentioned it.
 *
 * [detectable] records where the check lives, so a reader can see that these are
 * claims about the evidence rather than about the auditor's opinion of it.
 */
public enum class ViolationCode(public val detectable: String) {

    /** The frozen human baseline qualification evidence is absent or invalid. */
    BASELINE_NOT_QUALIFIED("baseline qualification evidence and freeze hash"),

    /** The baseline exists but did not clear its frozen competence margin. */
    BASELINE_MARGIN_BELOW_FLOOR("baseline margin vs BASELINE_COMPETENCE_MARGIN"),

    /** The variance pilot did not release a protocol-valid result. */
    PILOT_NOT_PROTOCOL_VALID("BlindVariancePilot.PilotRelease.protocolValid"),

    /** Someone from a non-scored pool was analysed in the scored attempt. */
    NON_SCORED_POOL_PARTICIPANT_ANALYSED("screening: PRIOR_NON_SCORED_POOL"),

    /** A participant appears twice within one arm of the analysed set. */
    DUPLICATE_PARTICIPANT_ANALYSED("screening: DUPLICATE_PARTICIPANT"),

    /** A pair below the completion floor reached the analysed set. */
    INCOMPLETE_SESSION_ANALYSED("screening: INCOMPLETE_SESSION"),

    /** A score outside the instrument bounds reached the analysed set. */
    SCORE_OUT_OF_RANGE_ANALYSED("screening: SCORE_OUT_OF_RANGE"),

    /** The attempt was run below the powered requirement the pilot implies. */
    SAMPLE_BELOW_POWERED_REQUIREMENT("A001FeasibilityBudget.requiredPairsPrimary"),

    /** Feasibility was never established, or was established as not feasible. */
    FEASIBILITY_NOT_ESTABLISHED("A001FeasibilityBudget.FeasibilityState"),

    /** The attempt declares a protocol version other than the frozen one. */
    PROTOCOL_VERSION_MISMATCH("A001StudyContract.PROTOCOL_VERSION"),

    /** The attempt declares an instrument other than the frozen one. */
    INSTRUMENT_MISMATCH("A001StudyContract.INSTRUMENT_ID"),

    /** The attempt declares an analysis version other than the frozen one. */
    ANALYSIS_VERSION_MISMATCH("A001Analysis.ANALYSIS_VERSION"),

    /** A frozen decision threshold no longer holds its frozen value. */
    THRESHOLD_WEAKENED_AFTER_FREEZE("frozen constants vs FROZEN_DECISION_THRESHOLDS"),

    /** More scored attempts exist than the three-attempt programme budget allows. */
    ATTEMPT_BUDGET_EXCEEDED("SpikeContract.MAX_SCORED_A001_ATTEMPTS"),

    /** No independent human-subjects determination is on file. */
    ETHICS_DETERMINATION_ABSENT("ethics determination in the gate evidence"),

    /** A submitted outcome claim disagrees with the recomputed classification. */
    CLAIMED_OUTCOME_DISAGREES_WITH_RECOMPUTATION("recomputed primary classification"),
    ;

    public companion object {
        /** Parsed strictly. An unrecognised code is ambiguous, never ignored. */
        public fun parse(raw: String?): ViolationCode? =
            entries.firstOrNull { it.name == raw?.trim()?.uppercase() }
    }
}

/**
 * One thing an auditor noticed.
 *
 * Note what is absent: there is no verdict, no severity, no weight, no score and
 * no confidence. Those are the fields through which a judgement would re-enter,
 * and the whole point of D016-I is that it must not. What remains is a pointer
 * and an argument, and the argument is for a human to read rather than for the
 * gate to consume.
 *
 * [claim] is deliberately never load-bearing. It is preserved verbatim because a
 * well-argued wrong finding is useful evidence about the auditor, and because
 * suppressing it would make the auditor look better than it is.
 */
public class AuditorFinding(
    public val findingId: String,
    public val auditor: AgenticRole,
    /** The machine-checkable violation being pointed at, or null if the auditor could not name one. */
    public val violationCode: ViolationCode?,
    public val citedEvidenceIds: List<String>,
    public val claim: String,
) {
    /** A finding with no code cannot be checked and must reach a human. */
    public val machineCheckable: Boolean get() = violationCode != null

    public fun describe(): String =
        "$findingId ${auditor.roleId} code=${violationCode?.name ?: "NONE"} " +
            "cites=${citedEvidenceIds.joinToString("|").ifEmpty { "none" }}"
}

/**
 * What the adjudicator did with a finding, after checking it for itself.
 *
 * The three values are not three degrees of agreement. They are three different
 * relationships between what an auditor said and what the evidence shows, and
 * only one of them has any effect on the gate.
 */
public enum class FindingDisposition {

    /**
     * The adjudicator independently re-derived this violation from the evidence.
     *
     * The violation was already blocking before the finding was read; the finding
     * is credited with having pointed at it, and changes nothing about the
     * outcome. This is what "agents audit" means in practice.
     */
    UPHELD_BLOCKING,

    /**
     * The adjudicator checked and the violation is not present.
     *
     * Inert. The finding is preserved and reported, because an auditor's false
     * positives are the most useful record anyone has of how far to trust it, but
     * it has no effect on the gate whatsoever.
     */
    NOT_CONFIRMED,

    /**
     * The auditor could not express its concern as a checkable code.
     *
     * The one case with an effect, and the effect is one-way: an otherwise
     * passing gate is suspended for the Architect rather than closed. It cannot
     * turn a pass into a fail, and it cannot turn a fail into anything at all.
     */
    AMBIGUOUS_RETURNED_TO_ARCHITECT,
}

/** A finding paired with the adjudicator's independent check of it. */
public class DispositionedFinding(
    public val finding: AuditorFinding,
    public val disposition: FindingDisposition,
    public val detail: String,
)

/**
 * `AuditorAuthorityPolicyV1`.
 *
 * States and proves the demotion. Every predicate here is derived from the role
 * contracts rather than declared beside them, so the policy cannot drift out of
 * agreement with the roles it describes.
 */
public object AuditorAuthorityPolicy {

    public const val POLICY_ID: String = "AuditorAuthorityPolicyV1"

    /** The policy this supersedes, retained by name rather than deleted. */
    public const val SUPERSEDES: String = "the D016-C reviewer-adjudicates model"

    /**
     * True when no agentic role can adjudicate the gate.
     *
     * Under D016-C this predicate's opposite was the requirement: both reviewers
     * held `ADJUDICATE_GATE` and the check was that the operator did not. D016-I
     * inverts it. `ADJUDICATE_GATE` is now forbidden to every role, so
     * [RoleContract] refuses at construction to build a role that claims it, and
     * the authority cannot be restored by editing a role definition.
     */
    public fun noAgentAdjudicates(): Boolean =
        AgenticRoleContracts.ALL.none { it.mayAdjudicateGate() } &&
            Authority.ADJUDICATE_GATE in FORBIDDEN_TO_EVERY_ROLE

    /** True when both auditors may raise findings, which is the authority they do hold. */
    public fun auditorsMayRaiseFindings(): Boolean =
        AgenticRoleContracts.AUDITORS.all { Authority.RAISE_AUDIT_FINDING in it.authorities }

    /** The operator administers and does not audit; the auditors audit and do not administer. */
    public fun separationHolds(): Boolean =
        Authority.RAISE_AUDIT_FINDING !in AgenticRoleContracts.STUDY_OPERATOR.authorities &&
            AgenticRoleContracts.AUDITORS.none {
                Authority.ORCHESTRATE_PARTICIPANT_FLOW in it.authorities
            }

    public fun holds(): Boolean =
        noAgentAdjudicates() && auditorsMayRaiseFindings() && separationHolds()

    public fun render(): String = buildString {
        append("================================================================\n")
        append("AUDITOR AUTHORITY (").append(POLICY_ID).append(")\n\n")
        append("  ").append(AdversarialAuditContract.DIRECTIVE)
        append(" reclassifies both routed reviewers as adversarial auditors.\n")
        append("  Human evidence determines aliveness. Frozen math determines PASS/FAIL.\n")
        append("  Agents audit; they do not adjudicate.\n\n")
        append("  NO_AGENT_ADJUDICATES=").append(noAgentAdjudicates()).append('\n')
        append("  AUDITORS_MAY_RAISE_FINDINGS=").append(auditorsMayRaiseFindings()).append('\n')
        append("  SEPARATION_HOLDS=").append(separationHolds()).append('\n')
        append("  AUDITOR_AUTHORITY_POLICY_HOLDS=").append(holds()).append('\n')
        append("  supersedes: ").append(SUPERSEDES).append("\n\n")
        append("  Structurally impossible for an auditor:\n")
        for (item in AdversarialAuditContract.STRUCTURALLY_IMPOSSIBLE_FOR_AN_AUDITOR) {
            append("    - ").append(item).append('\n')
        }
        append('\n')
        append("  Machine-checkable violation codes: ")
        append(ViolationCode.entries.size).append('\n')
        for (code in ViolationCode.entries) {
            append("    ").append(code.name.padEnd(46))
            append(code.detectable).append('\n')
        }
        append('\n')
        append("  A finding naming none of these is returned to the Architect rather than\n")
        append("  scored. A finding naming one of them is re-derived by the adjudicator\n")
        append("  from the evidence before it is allowed to matter, so an auditor that\n")
        append("  invents a violation, is talked into one by the material under review, or\n")
        append("  misses a real one cannot move the outcome in any direction.\n\n")
    }
}
