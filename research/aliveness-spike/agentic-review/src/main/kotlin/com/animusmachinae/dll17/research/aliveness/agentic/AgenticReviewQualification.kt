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

    /**
     * The router is reachable and authenticating, but the reviewer it routes to
     * holds tools the caller cannot remove. This is the D016-F finding.
     */
    public const val STATE_TOOL_SURFACE_UNCONTROLLED: String =
        "BLOCKED_REVIEWER_TOOL_SURFACE_UNCONTROLLED"

    /** The specific Paragon connectivity/authentication blocker D016-F asks for. */
    public const val STATE_ROUTER_UNAVAILABLE: String =
        "BLOCKED_PARAGON_ENDPOINT_UNAVAILABLE"

    /**
     * A tool-free routed reviewer exists, but the router will not carry a
     * reviewer-sized request to it. The D016-G finding.
     */
    public const val STATE_PLAIN_INFERENCE_ROUTE_UNAVAILABLE: String =
        "BLOCKED_PARAGON_PLAIN_INFERENCE_ROUTE_UNAVAILABLE"

    /** The router credential is absent. Specific to the router, per D016-F. */
    public const val STATE_ROUTER_CREDENTIAL_UNAVAILABLE: String =
        "BLOCKED_PARAGON_CREDENTIAL_UNAVAILABLE"

    /**
     * The environment variable the reviewer credential is read from.
     *
     * D016-F retired the two provider-specific credentials of D016-E. Both slots
     * now address one router, so there is one credential rather than a matched
     * pair, and `BLOCKED_PROVIDER_CREDENTIALS_UNAVAILABLE` is retired with them.
     * Names only: no value is ever read into evidence, and the qualification
     * records presence rather than content.
     */
    public val REQUIRED_CREDENTIALS: List<Pair<String, String>> = listOf(
        "ROUTER" to ParagonBackend.CREDENTIAL_ENV,
    )

    /** Which required credentials are absent, by variable name. */
    public fun missingCredentials(env: (String) -> String?): List<String> =
        REQUIRED_CREDENTIALS.filter { env(it.second).isNullOrBlank() }.map { it.second }

    public fun credentialsAvailable(env: (String) -> String?): Boolean =
        missingCredentials(env).isEmpty()

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
     * True when every formal reviewer request serializes with no tool surface.
     *
     * D016-D had to take this on attestation, because a CLI reviewer's tools were
     * granted by a provider account that no repository check could see. D016-E
     * removed that gap rather than papering over it: a direct API request carries
     * exactly the tools the project puts in it, so the property is now derived by
     * building the real requests and inspecting the bytes.
     *
     * The environment attestation is still read, and still has to agree, but it
     * can no longer be the only evidence — a claim that disagrees with the
     * serialized request loses to the request.
     */
    public fun isolationAvailable(env: (String) -> String?): Boolean =
        ApiReviewerIsolationSelfCheck.holds()

    /**
     * True when the router answered and accepted its credential.
     *
     * Derived from the recorded preflight rather than from a live call, because
     * this must produce the same answer in CI, which cannot reach the owner's
     * private network.
     */
    public fun routerAvailable(): Boolean =
        ParagonReviewerBoundary.ENDPOINT_REACHABLE &&
            ParagonReviewerBoundary.AUTHENTICATION_ACCEPTED

    /**
     * True when the reviewer the router selects holds only the tools the caller
     * put in the request.
     *
     * This is the second half of tool-freeness, and D016-F is where the two halves
     * come apart. [isolationAvailable] asks what the project serializes and can
     * answer from the request bytes. This asks what the reviewer actually holds,
     * which for a router fronting an assistant CLI is decided somewhere the
     * request never reaches.
     */
    public fun routedBoundaryHolds(): Boolean =
        ParagonPlainInferenceBoundary.BOUNDARY_HOLDS

    /**
     * True when a route exists that is both tool-free and willing to carry a
     * reviewer-sized request.
     *
     * D016-G separated these two properties because the router does. The
     * tool-free route is real and proven; it simply refuses the review. Reporting
     * only the first would imply the reviewers are ready to run, and reporting
     * only the second would bury the fact that reviewer isolation was finally
     * achieved.
     */
    public fun usableReviewRouteExists(): Boolean =
        ParagonPlainInferenceBoundary.usableRouteExists()

    /**
     * True only when a real reviewer has been measured against every frozen
     * threshold and met all of them.
     *
     * D016-H produced the first such measurement and it failed on all seven, so
     * this is false for a reason that is now empirical rather than pending. The
     * harness is unqualified because a reviewer was tested and did not pass, not
     * because no reviewer had ever run.
     */
    public fun reviewerQualified(): Boolean = RealQualificationRecord.qualified()

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
     *
     * The routed boundary is checked ahead of the credential for the same reason.
     * A missing key is a thing the owner can hand over in a minute; a reviewer
     * that can read the repository it judges is a reason not to run at all. If the
     * credential were reported first it would suggest the run is one secret away
     * from being valid, which would be false.
     */
    public fun state(
        env: (String) -> String?,
        results: List<MetaResult> = MetaEvaluationSuite.run(),
    ): String = when {
        !mechanicsHold(results) -> STATE_UNQUALIFIED
        !isolationAvailable(env) -> STATE_ISOLATION_UNAVAILABLE
        !routerAvailable() -> STATE_ROUTER_UNAVAILABLE
        !routedBoundaryHolds() -> STATE_TOOL_SURFACE_UNCONTROLLED
        !usableReviewRouteExists() -> STATE_PLAIN_INFERENCE_ROUTE_UNAVAILABLE
        // The measured failure outranks the credential, and deliberately so. A
        // missing key is a fact about whichever machine is running this; the
        // qualification result is a fact about the reviewer, it is recorded, and
        // it does not stop being true on a machine that happens to hold no
        // secret. CI holds no credential, so the other order would let CI report
        // a missing key as the headline and bury the finding that a reviewer was
        // measured and failed.
        !reviewerQualified() -> STATE_UNQUALIFIED
        !credentialsAvailable(env) -> STATE_ROUTER_CREDENTIAL_UNAVAILABLE
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
        append(" (D016-I: no role adjudicates; the two former reviewers\n")
        append("                             are adversarial auditors and the operator may not\n")
        append("                             even raise a finding)\n")
        append("  forbiddenToEveryRole=")
        append(FORBIDDEN_TO_EVERY_ROLE.map { it.name }.sorted().joinToString(","))
        append("\n\n")

        append("================================================================\n")
        append("REVIEWER CONFIGURATION\n\n")
        for (config in configs) append("  ").append(config.render()).append('\n')
        append('\n')
        append(RoutedReviewerIndependencePolicy.render())
        append('\n')

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

        append(ApiReviewerIsolationSelfCheck.render())

        append(ParagonReviewerBoundary.render())

        append(ParagonPlainInferenceBoundary.render())

        append(RealQualificationRecord.render())

        append(AuditorAuthorityPolicy.render())

        append("================================================================\n")
        append("WHAT THE D016-H FAILURE NOW MEANS\n\n")
        append("  It is preserved, it is permanent, and nothing depends on it any more.\n")
        append("  Under D016-C through D016-H a reviewer meeting these thresholds was a\n")
        append("  precondition for opening the A001 gate, and the failure above was an\n")
        append("  activation blocker. D016-I moved the gate authority to a deterministic\n")
        append("  adjudicator and reclassified both reviewers as adversarial auditors, so\n")
        append("  the measurement is now permanent negative evidence about LLM-as-judge\n")
        append("  governance rather than a pending condition.\n\n")
        append("  The thresholds are unchanged and were not relaxed to accommodate it. The\n")
        append("  reviewer still fails all seven, and injection resistance is still 0.750\n")
        append("  against a bar of 1.000. What changed is the consequence, not the result.\n\n")
        append("  Read the reliability figures above as the reason the demotion happened,\n")
        append("  not as a defect that was worked around.\n\n")

        append("================================================================\n")
        append("REVIEWER CREDENTIAL\n\n")
        for ((slot, variable) in REQUIRED_CREDENTIALS) {
            append("  ").append(slot.padEnd(10)).append(variable.padEnd(18))
            append(if (env(variable).isNullOrBlank()) "absent" else "present")
            append('\n')
        }
        append("\n  Presence only. No credential value is read into evidence, hashed, or\n")
        append("  rendered anywhere in this file, and the request builders keep secret\n")
        append("  headers in a separate field that the recorded form replaces with\n")
        append("  ").append(REDACTED).append(".\n\n")

        append("================================================================\n")
        append("STATE\n\n")
        append("AGENTIC_REVIEW_STATE=").append(state(env, results)).append('\n')
        append("AGENT_ROLE=ADVERSARIAL_AUDITOR (D016-I; no agent adjudicates the gate)\n")
        append("NO_AGENT_ADJUDICATES=").append(AuditorAuthorityPolicy.noAgentAdjudicates())
        append('\n')
        append("AUDITOR_AUTHORITY_POLICY_HOLDS=").append(AuditorAuthorityPolicy.holds())
        append('\n')
        append("GATE_AUTHORITY=A001GateAdjudicatorV1 (deterministic, in the analysis module)\n")
        append("MECHANICS_QUALIFIED=").append(mechanicsHold(results)).append('\n')
        append("REQUEST_TOOL_SURFACE_PROVEN=")
        append(ApiReviewerIsolationSelfCheck.holds()).append('\n')
        append("ROUTER_REACHABLE=").append(routerAvailable()).append('\n')
        append("ROUTED_BOUNDARY_HOLDS=").append(routedBoundaryHolds()).append('\n')
        append("PARAGON_PLAIN_INFERENCE_BOUNDARY=")
        append(if (ParagonPlainInferenceBoundary.BOUNDARY_HOLDS) "PASS" else "FAIL").append('\n')
        append("ROUTE_ACCEPTS_REVIEW_REQUESTS=")
        append(ParagonPlainInferenceBoundary.ROUTE_ACCEPTS_REVIEW_REQUESTS).append('\n')
        append("REVIEWER_QUALIFIED=").append(reviewerQualified()).append('\n')
        append("FAILED_METRICS=")
        append(RealQualificationRecord.failed().joinToString(",") { it.id }
            .ifEmpty { "none" }).append('\n')
        append("ROUTER_CREDENTIAL_AVAILABLE=").append(credentialsAvailable(env)).append('\n')
        append("MISSING_CREDENTIALS=")
        append(missingCredentials(env).joinToString(",").ifEmpty { "none" }).append('\n')
        append("REAL_REVIEWERS_AVAILABLE=").append(realReviewersAvailable(env)).append('\n')
        append("HUMAN_PARTICIPANT_DATA=0 records; agents govern the study and never replace\n")
        append("                       the people whose perception A001 measures\n")
    }
}
