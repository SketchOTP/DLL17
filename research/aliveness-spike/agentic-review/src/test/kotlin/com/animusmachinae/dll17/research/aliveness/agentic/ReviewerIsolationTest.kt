package com.animusmachinae.dll17.research.aliveness.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The isolation proof.
 *
 * The claim under test is that the primary and alternate reviewers make their
 * first-pass judgements without either being able to observe the other. Three
 * independent lines of evidence, because one would be easy to fool:
 *
 * 1. **Surface.** No type in the review path has a parameter through which a
 *    ruling could be passed to a reviewer.
 * 2. **Bytes.** The alternate's request contains no substring of the primary's
 *    output, checked against a recording backend that captures exactly what it
 *    was handed.
 * 3. **Order independence.** Running the alternate first produces byte-identical
 *    input. If anything from the primary's pass leaked into the alternate's
 *    prompt, running the alternate first would have to change it.
 */
class ReviewerIsolationTest {

    /** Captures exactly the bytes it was given, and answers like a compliant reviewer. */
    private class RecordingBackend(
        private val delegate: ReviewerBackend = CompliantScriptedReviewer(),
    ) : ScriptedBackend("recording-backend", "isolation witness") {
        val seen = mutableListOf<String>()
        override fun invoke(request: ReviewRequest): BackendOutcome {
            seen += request.render()
            return delegate.invoke(request)
        }
    }

    private val question = ReviewQuestion("Q-PRIMARY-OUTCOME", "Does the result satisfy the rule?")
    private val bundle = EvidenceBundle(
        "B-ISO",
        listOf(
            EvidenceItem("EV-01", EvidenceKind.STATISTICAL_RESULT, "mean 14.0; finding=PASS", true),
            EvidenceItem("EV-02", EvidenceKind.LOG_EXTRACT, "no deviations"),
        ),
    )

    @Test
    fun `no type in the review path can be handed another reviewer's ruling`() {
        // A reviewer receives exactly one object. If it cannot carry a ruling,
        // no amount of orchestration can deliver one.
        val fields = ReviewRequest::class.java.declaredFields
            .filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .sorted()
        assertEquals(listOf("bundle", "question", "roleContract"), fields)

        // And the session that builds it takes nothing else either.
        val runParams = IsolatedReviewSession::class.java.methods
            .single { it.name == "run" && it.parameterCount == 5 }
            .parameterTypes
            .map { it.simpleName }
        assertFalse(
            runParams.any { it.contains("Ruling") || it.contains("Outcome") },
            "a review session accepts a ruling-shaped parameter: $runParams",
        )
    }

    @Test
    fun `the alternate's input contains nothing from the primary's output`() {
        val primary = RecordingBackend()
        val alternate = RecordingBackend()
        val review = AgenticReviewHarness.review(question, bundle, primary, alternate)

        val primaryRuling = review.primary as RulingOutcome.Ruled
        val alternateInput = alternate.seen.single()

        assertFalse(alternateInput.contains(primaryRuling.ruling.rationale))
        assertFalse(alternateInput.contains(primaryRuling.provenance.decisionRelevantDigest()))
        assertFalse(alternateInput.contains("PrimaryAgenticAlivenessGateReviewer"))
        // The word PASS legitimately appears in the output schema, so the check
        // that matters is that no *ruling* line does.
        assertFalse(alternateInput.contains("VERDICT: PASS"))
    }

    @Test
    fun `running the alternate first produces byte-identical input`() {
        val forwardAlternate = RecordingBackend()
        AgenticReviewHarness.review(question, bundle, RecordingBackend(), forwardAlternate)

        // The alternate alone, with no primary having run at all.
        val soloAlternate = RecordingBackend()
        IsolatedReviewSession.run(
            AgenticRoleContracts.ALTERNATE, soloAlternate, question, bundle,
        )

        assertEquals(
            soloAlternate.seen.single(),
            forwardAlternate.seen.single(),
            "the alternate's prompt depends on whether the primary ran first, so something leaks",
        )
    }

    @Test
    fun `each reviewer receives the same evidence and differs only in its own role contract`() {
        val primary = RecordingBackend()
        val alternate = RecordingBackend()
        AgenticReviewHarness.review(question, bundle, primary, alternate)

        val p = primary.seen.single()
        val a = alternate.seen.single()
        assertTrue(p.contains(bundle.render()))
        assertTrue(a.contains(bundle.render()))
        assertTrue(p.contains(AgenticRoleContracts.PRIMARY.instructions.trim()))
        assertTrue(a.contains(AgenticRoleContracts.ALTERNATE.instructions.trim()))
        assertFalse(a.contains(AgenticRoleContracts.PRIMARY.instructions.trim()))
    }

    @Test
    fun `the harness has no consensus debate or tie-breaking path`() {
        // compare() is total over two sealed outcomes and consults nothing else:
        // there is no third model, no re-prompt and no vote. Any verdict
        // difference is returned as a disagreement for the architect.
        val fail = IsolatedReviewSession.run(
            AgenticRoleContracts.PRIMARY, CompliantScriptedReviewer(), question,
            EvidenceBundle(
                "B-X",
                listOf(EvidenceItem("EV-01", EvidenceKind.STATISTICAL_RESULT, "finding=FAIL", true)),
            ),
        )
        val pass = IsolatedReviewSession.run(
            AgenticRoleContracts.ALTERNATE, AlwaysPassReviewer(), question, bundle,
        )
        assertEquals(
            AgenticReviewHarness.STATE_DISAGREEMENT,
            AgenticReviewHarness.compare(fail, pass),
        )
        assertEquals(
            AgenticReviewHarness.STATE_DISAGREEMENT,
            AgenticReviewHarness.compare(pass, fail),
            "the comparison must not depend on which reviewer is asked first",
        )
    }
}
