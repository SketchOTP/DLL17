package com.animusmachinae.dll17.research.aliveness.agentic

/**
 * How a real reviewer is configured.
 *
 * Deliberately not hard-coded to any commercial model name. A reviewer is
 * declared by environment, so the qualified configuration is a recorded fact
 * about a run rather than a constant compiled into a governance harness that is
 * expected to outlive today's model line-up.
 */
public class ReviewerConfiguration(
    public val slot: String,
    public val provider: String?,
    public val modelId: String?,
    public val modelFamily: String?,
    public val modelSnapshot: String?,
    public val credentialPresent: Boolean,
    public val toolDenialAttestation: String?,
) {
    public val complete: Boolean
        get() = !provider.isNullOrBlank() && !modelId.isNullOrBlank() &&
            !modelFamily.isNullOrBlank() && credentialPresent

    /**
     * Whether this slot's reviewer has been shown to reach nothing but its frozen
     * bundle.
     *
     * D016-D established that a jailed filesystem is not sufficient on its own.
     * A hosted assistant can carry server-side tools its client cannot remove, and
     * a repository-reading tool provisioned by the provider defeats an evidence
     * boundary enforced only locally. So the attestation must name both halves,
     * and an unattested slot is never treated as isolated.
     */
    public val isolationAttested: Boolean
        get() = toolDenialAttestation == REQUIRED_ATTESTATION

    public fun render(): String =
        "$slot provider=${provider ?: UNKNOWN} model=${modelId ?: UNKNOWN} " +
            "family=${modelFamily ?: UNKNOWN} snapshot=${modelSnapshot ?: UNKNOWN} " +
            "credential=${if (credentialPresent) "present" else "absent"} " +
            "complete=$complete isolationAttested=$isolationAttested " +
            "toolDenial=${toolDenialAttestation ?: UNKNOWN}"

    public companion object {
        /**
         * The only accepted attestation value. It is deliberately a single exact
         * string rather than a boolean, so that setting it is a positive claim
         * about what was verified and cannot be satisfied by an incidental
         * truthy value.
         */
        public const val REQUIRED_ATTESTATION: String = "VERIFIED_NO_REPOSITORY_NO_WEB"

        /** The environment contract, documented in AgenticReviewHarnessV1.md. */
        public fun discover(slot: String, env: (String) -> String?): ReviewerConfiguration {
            val prefix = "A001_${slot}_REVIEWER"
            val credentialVar = env("${prefix}_CREDENTIAL_ENV")
            return ReviewerConfiguration(
                slot = slot,
                provider = env("${prefix}_PROVIDER"),
                modelId = env("${prefix}_MODEL"),
                modelFamily = env("${prefix}_FAMILY"),
                modelSnapshot = env("${prefix}_SNAPSHOT"),
                credentialPresent = !credentialVar.isNullOrBlank() &&
                    !env(credentialVar).isNullOrBlank(),
                toolDenialAttestation = env("${prefix}_TOOL_DENIAL"),
            )
        }
    }
}

/**
 * `AgenticReviewQualificationV1`.
 *
 * Produces the committed qualification evidence and derives the harness's
 * governance state from what it actually observed. The state is computed from the
 * results, never declared beside them: if the mechanics stop holding, or a real
 * diverse reviewer pair stops being available, the state moves on its own.
 *
 * Output is deterministic. No wall-clock time, no random seed and no machine
 * identity appears in it, so CI can regenerate it and diff it byte for byte.
 */
public object AgenticReviewQualification {

    public const val QUALIFICATION_ID: String = "AgenticReviewQualificationV1"

    public const val STATE_QUALIFIED: String = "AGENTIC_REVIEW_HARNESS_QUALIFIED"
    public const val STATE_UNQUALIFIED: String = "BLOCKED_AGENTIC_REVIEW_HARNESS_UNQUALIFIED"
    public const val STATE_DIVERSITY_UNAVAILABLE: String =
        "BLOCKED_AGENTIC_REVIEW_DIVERSITY_UNAVAILABLE"
    public const val STATE_ISOLATION_UNAVAILABLE: String =
        "BLOCKED_AGENTIC_REVIEW_ISOLATION_UNAVAILABLE"

    @JvmStatic
    public fun main(args: Array<String>) {
        println(render(System::getenv))
    }

    /** The two reviewer slots, as discovered from the environment. */
    public fun configurations(env: (String) -> String?): List<ReviewerConfiguration> = listOf(
        ReviewerConfiguration.discover("PRIMARY", env),
        ReviewerConfiguration.discover("ALTERNATE", env),
    )

    /** True only when both slots are fully configured with a present credential. */
    public fun realReviewersAvailable(env: (String) -> String?): Boolean =
        configurations(env).all { it.complete }

    /**
     * True only when both slots carry the exact tool-denial attestation.
     *
     * Nothing in this repository can verify the claim, because the boundary being
     * attested is a property of a provider account rather than of a checkout. What
     * the repository can do is refuse to proceed without the claim, and refuse to
     * accept any value other than the one that names what had to be verified.
     */
    public fun isolationAvailable(env: (String) -> String?): Boolean =
        configurations(env).all { it.isolationAttested }

    /**
     * The harness mechanics: does every frozen fixture produce its expected
     * governance outcome? This is the half that does not need a model.
     */
    public fun mechanicsHold(results: List<MetaResult> = MetaEvaluationSuite.run()): Boolean =
        results.isNotEmpty() && results.all { it.held }

    /**
     * The state, derived.
     *
     * Qualification requires every half. The mechanics can hold completely and
     * the harness still be unqualified, because a reviewer whose stability,
     * order-sensitivity and injection resistance have never been measured on a
     * real model is not a qualified reviewer — it is an untested one.
     *
     * Isolation is checked before diversity, and deliberately so. Two
     * heterogeneous models that can both read the repository they are adjudicating
     * are not two independent reviewers; they are one leak sampled twice. There is
     * no useful sense in which such a pair is closer to qualified than a single
     * isolated reviewer would be, so the weaker finding must not mask the stronger
     * one.
     */
    public fun state(
        env: (String) -> String?,
        results: List<MetaResult> = MetaEvaluationSuite.run(),
    ): String = when {
        !mechanicsHold(results) -> STATE_UNQUALIFIED
        !isolationAvailable(env) -> STATE_ISOLATION_UNAVAILABLE
        !realReviewersAvailable(env) -> STATE_DIVERSITY_UNAVAILABLE
        else -> STATE_QUALIFIED
    }

    public fun render(env: (String) -> String?): String = buildString {
        val results = MetaEvaluationSuite.run()
        val configs = configurations(env)

        append("AGENTIC_REVIEW_QUALIFICATION=").append(QUALIFICATION_ID).append('\n')
        append("harness=").append(AgenticReviewHarness.HARNESS_ID)
        append(" v").append(AgenticReviewHarness.HARNESS_VERSION)
        append("  suite=").append(MetaEvaluationSuite.SUITE_ID)
        append(" v").append(MetaEvaluationSuite.SUITE_VERSION)
        append("  schema=").append(RulingParser.SCHEMA_ID)
        append(" parserVersion=").append(RulingParser.PARSER_VERSION).append('\n')
        append("DATA_CLASS=NO_HUMAN_DATA — this harness governs a human study and contains\n")
        append("           none of its data. No participant has been recruited or scored.\n\n")

        append("================================================================\n")
        append("ROLE CONTRACTS\n\n")
        for (contract in AgenticRoleContracts.ALL) {
            append("  ").append(contract.describe()).append('\n')
        }
        append("  authorityBoundaryHolds=").append(AgenticRoleContracts.authorityBoundaryHolds())
        append(" (reviewers adjudicate; the operator does not)\n")
        append("  forbiddenToEveryRole=")
        append(FORBIDDEN_TO_EVERY_ROLE.map { it.name }.sorted().joinToString(","))
        append("\n\n")

        append("================================================================\n")
        append("REVIEWER CONFIGURATION\n\n")
        for (config in configs) append("  ").append(config.render()).append('\n')
        append('\n')
        val diversity = DiversityPolicy.evaluate(
            CompliantScriptedReviewer().descriptor,
            CompliantScriptedReviewer("compliant-scripted-reviewer-b").descriptor,
        )
        append("  ").append(DiversityPolicy.POLICY_ID).append('\n')
        append("  diversitySatisfied=").append(diversity.satisfied).append('\n')
        append("  detail: ").append(diversity.detail).append('\n')
        append("  note:   the pair evaluated above is the in-repository fixture pair, which is\n")
        append("          the only pair this environment can construct. It is reported so the\n")
        append("          refusal is visible rather than implied.\n\n")

        append(QualificationThresholds.render()).append('\n')

        append("================================================================\n")
        append("META-EVALUATION\n\n")
        for (result in results) {
            append(if (result.held) "  HELD     " else "  NOT_HELD ")
            append(result.fixture.id.padEnd(8))
            append(result.fixture.description).append('\n')
            append("           expected=").append(result.fixture.expected).append('\n')
            append("           observed=").append(result.observed).append('\n')
            append("           ").append(result.detail).append('\n')
        }
        append('\n')
        append("fixtures=").append(results.size)
        append(" held=").append(results.count { it.held })
        append(" notHeld=").append(results.count { !it.held }).append('\n')
        append("mechanicsHold=").append(mechanicsHold(results)).append("\n\n")

        append("================================================================\n")
        append("WHAT THIS RUN DID NOT MEASURE\n\n")
        append("  Every reviewer exercised above is a deterministic in-repository fixture,\n")
        append("  not a language model. Repeated-run stability, position and order\n")
        append("  sensitivity, abstention rate and injection resistance are properties of a\n")
        append("  model, and no model was executed. The frozen thresholds above therefore\n")
        append("  have nothing to be applied to yet, which is exactly why they can be\n")
        append("  trusted not to have been fitted to a result.\n\n")

        append("================================================================\n")
        append("ISOLATION PRECONDITION\n\n")
        append("  A reviewer slot counts as isolated only when its environment carries\n")
        append("  A001_{SLOT}_REVIEWER_TOOL_DENIAL=")
        append(ReviewerConfiguration.REQUIRED_ATTESTATION).append(".\n")
        append("  D016-D established why this is a separate precondition rather than part\n")
        append("  of the diversity check: a hosted assistant can carry provider-side tools\n")
        append("  that no client flag and no operating-system jail can remove, and a\n")
        append("  repository-reading tool among them defeats an evidence boundary that is\n")
        append("  enforced only on the local filesystem. See\n")
        append("  evidence/AGENTIC_REVIEW_ISOLATION_PREFLIGHT.txt.\n\n")

        append("================================================================\n")
        append("STATE\n\n")
        append("AGENTIC_REVIEW_STATE=").append(state(env, results)).append('\n')
        append("MECHANICS_QUALIFIED=").append(mechanicsHold(results)).append('\n')
        append("REVIEWER_ISOLATION_ATTESTED=").append(isolationAvailable(env)).append('\n')
        append("REAL_REVIEWERS_AVAILABLE=").append(realReviewersAvailable(env)).append('\n')
        append("HUMAN_PARTICIPANT_DATA=0 records; agents govern the study and never replace\n")
        append("                       the people whose perception A001 measures\n")
    }
}
