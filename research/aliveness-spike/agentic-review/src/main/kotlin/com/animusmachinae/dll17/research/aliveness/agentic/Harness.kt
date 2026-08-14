package com.animusmachinae.dll17.research.aliveness.agentic

/** How many attempts a transient failure is allowed. */
public class RetryPolicy(public val maxAttempts: Int = 3) {
    init {
        require(maxAttempts >= 1)
    }
}

/**
 * One reviewer, one question, in its own context.
 *
 * The whole isolation guarantee lives in this signature: a session is a pure
 * function of a role contract, a backend, a question and a bundle. There is no
 * parameter for another reviewer's ruling, reasoning, transcript or vote, so
 * there is no code path by which one could arrive. The harness runs two of these
 * and cannot make them talk to each other because the type does not permit it.
 */
public object IsolatedReviewSession {

    public fun run(
        roleContract: RoleContract,
        backend: ReviewerBackend,
        question: ReviewQuestion,
        bundle: EvidenceBundle,
        retryPolicy: RetryPolicy = RetryPolicy(),
    ): RulingOutcome {
        val request = ReviewRequest(roleContract, question, bundle)
        val retryReasons = mutableListOf<FailureMode>()
        var lastFailure: Pair<FailureMode, String> = FailureMode.TRANSPORT_FAILURE to "no attempt ran"
        var lastRawHash: String? = null

        for (attempt in 1..retryPolicy.maxAttempts) {
            val failure: Pair<FailureMode, String>
            when (val outcome = backend.invoke(request)) {
                is BackendOutcome.Responded -> {
                    lastRawHash = sha256(outcome.rawText)
                    when (val parsed = RulingParser.parse(outcome.rawText, question, bundle)) {
                        is RulingParser.ParseResult.Parsed ->
                            return RulingOutcome.Ruled(
                                parsed.ruling,
                                provenance(roleContract, backend, request, bundle,
                                    lastRawHash, retryReasons),
                            )
                        is RulingParser.ParseResult.Failed ->
                            failure = parsed.mode to parsed.detail
                    }
                }
                is BackendOutcome.Refused ->
                    failure = FailureMode.PROVIDER_REFUSAL to outcome.reason
                is BackendOutcome.TimedOut ->
                    failure = FailureMode.TIMEOUT_AFTER_PERMITTED_RETRIES to
                        "no response after ${outcome.afterMillis}ms"
                is BackendOutcome.TransportFailed ->
                    failure = FailureMode.TRANSPORT_FAILURE to outcome.reason
            }

            lastFailure = failure
            if (!failure.first.retryable || attempt == retryPolicy.maxAttempts) break
            retryReasons += failure.first
        }

        return RulingOutcome.FailedClosed(
            lastFailure.first,
            lastFailure.second,
            provenance(roleContract, backend, request, bundle, lastRawHash, retryReasons),
        )
    }

    private fun provenance(
        roleContract: RoleContract,
        backend: ReviewerBackend,
        request: ReviewRequest,
        bundle: EvidenceBundle,
        rawHash: String?,
        retryReasons: List<FailureMode>,
    ): RulingProvenance = RulingProvenance(
        roleId = roleContract.role.roleId,
        roleContractVersion = roleContract.contractVersion,
        provider = backend.descriptor.provider,
        modelId = backend.descriptor.modelId,
        modelFamily = backend.descriptor.modelFamily,
        modelSnapshot = backend.descriptor.modelSnapshot,
        isRealModel = backend.descriptor.isRealModel,
        promptHash = roleContract.instructionsHash,
        toolPermissionHash = backend.toolPermissionHash(),
        evidenceBundleHash = bundle.hash(),
        requestHash = request.hash(),
        sampling = backend.sampling,
        parserVersion = RulingParser.PARSER_VERSION,
        schemaId = RulingParser.SCHEMA_ID,
        rawResponseHash = rawHash,
        retryCount = retryReasons.size,
        retryReasons = retryReasons,
        executionTimestamp = null,
    )
}

/** Whether a pair of reviewer configurations is heterogeneous enough to count as two reviewers. */
public class DiversityOutcome(
    public val satisfied: Boolean,
    public val differentProvider: Boolean,
    public val differentFamily: Boolean,
    public val bothReal: Boolean,
    public val detail: String,
)

/**
 * `AgenticReviewerDiversityPolicyV1`.
 *
 * Two sessions of the same configuration are two samples, not two reviewers.
 * Independent judgement requires that the reviewers can actually be wrong in
 * different ways, so the policy requires different model families, and treats a
 * shared provider as a recorded weakness rather than a silent one.
 */
public object DiversityPolicy {

    public const val POLICY_ID: String = "AgenticReviewerDiversityPolicyV1"

    public fun evaluate(primary: ModelDescriptor, alternate: ModelDescriptor): DiversityOutcome {
        val bothReal = primary.isRealModel && alternate.isRealModel
        val differentProvider = !primary.provider.equals(alternate.provider, ignoreCase = true)
        val differentFamily = !primary.modelFamily.equals(alternate.modelFamily, ignoreCase = true)
        val sameModel = primary.modelId.equals(alternate.modelId, ignoreCase = true)

        val detail = when {
            !bothReal ->
                "at least one configured reviewer is not a real language model " +
                    "(primary realModel=${primary.isRealModel}, " +
                    "alternate realModel=${alternate.isRealModel}); deterministic doubles " +
                    "qualify the harness mechanics and can never qualify the reviewers"
            sameModel ->
                "both reviewers are ${primary.modelId}; two calls to one configuration are " +
                    "two samples of the same judgement, not two judgements"
            !differentFamily ->
                "both reviewers are in model family ${primary.modelFamily}; same-family " +
                    "reviewers correlate and do not provide independent review"
            !differentProvider ->
                "different families (${primary.modelFamily} vs ${alternate.modelFamily}) but a " +
                    "shared provider ${primary.provider}; permitted, and recorded as a " +
                    "weaker form of independence than cross-provider review"
            else ->
                "different providers (${primary.provider} vs ${alternate.provider}) and " +
                    "different families (${primary.modelFamily} vs ${alternate.modelFamily})"
        }

        return DiversityOutcome(
            satisfied = bothReal && !sameModel && differentFamily,
            differentProvider = differentProvider,
            differentFamily = differentFamily,
            bothReal = bothReal,
            detail = detail,
        )
    }
}

/** The joint result of an isolated two-reviewer review. */
public class JointReview(
    public val questionId: String,
    public val primary: RulingOutcome,
    public val alternate: RulingOutcome,
    public val state: String,
) {
    public fun render(): String = buildString {
        append("QUESTION ").append(questionId).append('\n')
        append("  primary   ").append(primary.label()).append('\n')
        append(primary.provenance.render("      "))
        append("  alternate ").append(alternate.label()).append('\n')
        append(alternate.provenance.render("      "))
        append("  JOINT     ").append(state).append('\n')
    }
}

/**
 * `AgenticReviewHarnessV1`.
 *
 * Runs both reviewers in isolation, seals both outcomes, and only then compares
 * them. There is no debate loop, no consensus prompt, no majority vote and no
 * tie-breaking meta-judge: a material disagreement is returned to the architect
 * as a blocking state, because the disagreement is the finding.
 */
public object AgenticReviewHarness {

    public const val HARNESS_ID: String = "AgenticReviewHarnessV1"
    public const val HARNESS_VERSION: Int = 1

    public const val STATE_CONCURRED_PASS: String = "AGENTIC_REVIEW_CONCURRED_PASS"
    public const val STATE_CONCURRED_FAIL: String = "AGENTIC_REVIEW_CONCURRED_FAIL"
    public const val STATE_CONCURRED_NON_RULING: String = "AGENTIC_REVIEW_CONCURRED_NON_RULING"
    public const val STATE_DISAGREEMENT: String = "BLOCKED_AGENTIC_REVIEW_DISAGREEMENT"
    public const val STATE_FAILED_CLOSED: String = "BLOCKED_AGENTIC_REVIEW_FAILED_CLOSED"

    public fun review(
        question: ReviewQuestion,
        bundle: EvidenceBundle,
        primaryBackend: ReviewerBackend,
        alternateBackend: ReviewerBackend,
        retryPolicy: RetryPolicy = RetryPolicy(),
    ): JointReview {
        // Sealed on assignment. Neither call can observe the other: the second
        // session is constructed from the same four inputs as the first, and
        // `primary` is not among them.
        val primary = IsolatedReviewSession.run(
            AgenticRoleContracts.PRIMARY, primaryBackend, question, bundle, retryPolicy,
        )
        val alternate = IsolatedReviewSession.run(
            AgenticRoleContracts.ALTERNATE, alternateBackend, question, bundle, retryPolicy,
        )
        return JointReview(question.questionId, primary, alternate, compare(primary, alternate))
    }

    /**
     * Mechanical comparison of two sealed rulings.
     *
     * Any difference in verdict is material. A less conservative rule would have
     * to decide which differences are tolerable, and every such decision is a
     * quiet way of letting one reviewer overrule the other.
     */
    public fun compare(primary: RulingOutcome, alternate: RulingOutcome): String {
        if (primary is RulingOutcome.FailedClosed || alternate is RulingOutcome.FailedClosed) {
            return STATE_FAILED_CLOSED
        }
        val p = (primary as RulingOutcome.Ruled).ruling.verdict
        val a = (alternate as RulingOutcome.Ruled).ruling.verdict
        if (p != a) return STATE_DISAGREEMENT
        return when (p) {
            RulingVerdict.PASS -> STATE_CONCURRED_PASS
            RulingVerdict.FAIL -> STATE_CONCURRED_FAIL
            else -> STATE_CONCURRED_NON_RULING
        }
    }

    /** A gate opens only on a concurred pass by two qualified, diverse reviewers. */
    public fun gateOpens(review: JointReview, diversity: DiversityOutcome): Boolean =
        diversity.satisfied && review.state == STATE_CONCURRED_PASS
}
