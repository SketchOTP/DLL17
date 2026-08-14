package com.animusmachinae.dll17.research.aliveness.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Everything that is not a well-formed, supported, consistent ruling must fail
 * closed. The property that matters is not that each case produces the right
 * error name — it is that *none* of them can produce a pass.
 */
class FailClosedTest {

    private val question = ReviewQuestion("Q-PRIMARY-OUTCOME", "Does the result satisfy the rule?")
    private val bundle = EvidenceBundle(
        "B-FC",
        listOf(
            EvidenceItem("EV-01", EvidenceKind.STATISTICAL_RESULT, "mean 14.0; finding=PASS", true),
            EvidenceItem("EV-02", EvidenceKind.LOG_EXTRACT, "session log"),
        ),
    )

    private fun run(backend: ReviewerBackend, policy: RetryPolicy = RetryPolicy()) =
        IsolatedReviewSession.run(AgenticRoleContracts.PRIMARY, backend, question, bundle, policy)

    private fun parse(raw: String) = RulingParser.parse(raw, question, bundle)

    @Test
    fun `no failing backend can ever produce a pass`() {
        val failing = listOf(
            MalformedOutputBackend(), RefusingBackend(), TimingOutBackend(),
            InconsistentProseBackend(), UnsupportedConclusionBackend(), EvidenceOmittingBackend(),
        )
        for (backend in failing) {
            val outcome = run(backend)
            assertIs<RulingOutcome.FailedClosed>(
                outcome, "${backend.descriptor.modelId} did not fail closed",
            )
            assertFalse(outcome.isPass(), "${backend.descriptor.modelId} produced a pass")
        }
    }

    @Test
    fun `each failure mode is reported as itself rather than collapsed`() {
        assertEquals(FailureMode.MALFORMED_OUTPUT, mode(MalformedOutputBackend()))
        assertEquals(FailureMode.PROVIDER_REFUSAL, mode(RefusingBackend()))
        assertEquals(FailureMode.TIMEOUT_AFTER_PERMITTED_RETRIES, mode(TimingOutBackend()))
        assertEquals(FailureMode.INCONSISTENT_RULING_AND_PROSE, mode(InconsistentProseBackend()))
        assertEquals(FailureMode.UNSUPPORTED_CONCLUSION, mode(UnsupportedConclusionBackend()))
        assertEquals(FailureMode.EVIDENCE_OMISSION, mode(EvidenceOmittingBackend()))
    }

    private fun mode(backend: ReviewerBackend): FailureMode =
        (run(backend) as RulingOutcome.FailedClosed).mode

    @Test
    fun `a missing field is refused rather than defaulted`() {
        val result = parse("VERDICT: PASS\nQUESTION: Q-PRIMARY-OUTCOME\nEVIDENCE: EV-01")
        assertIs<RulingParser.ParseResult.Failed>(result)
        assertEquals(FailureMode.MISSING_REQUIRED_FIELD, result.mode)
    }

    @Test
    fun `an unknown verdict is refused rather than mapped to the nearest one`() {
        val result = parse(
            "VERDICT: PROBABLY_PASS\nQUESTION: Q-PRIMARY-OUTCOME\nEVIDENCE: EV-01\n" +
                "RATIONALE: close enough",
        )
        assertIs<RulingParser.ParseResult.Failed>(result)
        assertEquals(FailureMode.UNPARSEABLE_VERDICT, result.mode)
    }

    @Test
    fun `a ruling on a different question is refused`() {
        val result = parse(
            "VERDICT: PASS\nQUESTION: Q-SOMETHING-ELSE\nEVIDENCE: EV-01\nRATIONALE: fine",
        )
        assertIs<RulingParser.ParseResult.Failed>(result)
        assertEquals(FailureMode.QUESTION_MISMATCH, result.mode)
    }

    @Test
    fun `citing evidence that is not in the bundle is refused`() {
        val result = parse(
            "VERDICT: PASS\nQUESTION: Q-PRIMARY-OUTCOME\nEVIDENCE: EV-99\nRATIONALE: fine",
        )
        assertIs<RulingParser.ParseResult.Failed>(result)
        assertEquals(FailureMode.UNSUPPORTED_CONCLUSION, result.mode)
    }

    @Test
    fun `a non-deciding verdict may legitimately cite nothing`() {
        val result = parse(
            "VERDICT: BLOCKED_INSUFFICIENT_EVIDENCE\nQUESTION: Q-PRIMARY-OUTCOME\n" +
                "EVIDENCE: NONE\nRATIONALE: the interval is missing from every artefact",
        )
        assertIs<RulingParser.ParseResult.Parsed>(result)
        assertEquals(RulingVerdict.BLOCKED_INSUFFICIENT_EVIDENCE, result.ruling.verdict)
    }

    @Test
    fun `abstention and blocked verdicts are never passes`() {
        for (verdict in RulingVerdict.entries.filter { it != RulingVerdict.PASS }) {
            assertFalse(verdict.isPass, "$verdict is treated as a pass")
        }
        assertTrue(RulingVerdict.PASS.isPass)
    }

    @Test
    fun `a transient failure is retried and the retries are recorded`() {
        val outcome = run(FlakyThenCompliantBackend(2), RetryPolicy(maxAttempts = 3))
        assertIs<RulingOutcome.Ruled>(outcome)
        assertEquals(RulingVerdict.PASS, outcome.ruling.verdict)
        assertEquals(2, outcome.provenance.retryCount)
        assertEquals(
            listOf(FailureMode.TRANSPORT_FAILURE, FailureMode.TRANSPORT_FAILURE),
            outcome.provenance.retryReasons,
        )
    }

    @Test
    fun `retries run out rather than continuing until the answer is acceptable`() {
        val outcome = run(FlakyThenCompliantBackend(5), RetryPolicy(maxAttempts = 3))
        assertIs<RulingOutcome.FailedClosed>(outcome)
        assertEquals(FailureMode.TRANSPORT_FAILURE, outcome.mode)
        assertEquals(2, outcome.provenance.retryCount)
    }

    @Test
    fun `a refusal is not retried`() {
        assertFalse(FailureMode.PROVIDER_REFUSAL.retryable)
        val outcome = run(RefusingBackend(), RetryPolicy(maxAttempts = 5))
        assertIs<RulingOutcome.FailedClosed>(outcome)
        assertEquals(0, outcome.provenance.retryCount)
    }

    @Test
    fun `a well-formed ruling is never re-rolled`() {
        // There is no retryable failure mode that a successful parse can produce,
        // so no path exists that asks a reviewer again after it has ruled.
        var calls = 0
        val counting = object : ScriptedBackend("counting", "retry-abuse probe") {
            override fun invoke(request: ReviewRequest): BackendOutcome {
                calls++
                return CompliantScriptedReviewer().invoke(request)
            }
        }
        val outcome = run(counting, RetryPolicy(maxAttempts = 5))
        assertIs<RulingOutcome.Ruled>(outcome)
        assertEquals(1, calls, "the harness re-asked a reviewer that had already ruled")
    }
}
