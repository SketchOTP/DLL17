package com.animusmachinae.dll17.research.aliveness.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * D016-G. The two findings are tested separately because they disagree, and the
 * value of the directive's outcome is in keeping them apart: a tool-free reviewer
 * was finally obtained, and it still could not be asked the question.
 */
class PlainInferenceBoundaryTest {

    @Test
    fun `the routed reviewer holds no external capability`() {
        assertTrue(ParagonPlainInferenceBoundary.BOUNDARY_HOLDS)
        assertTrue(ParagonPlainInferenceBoundary.PROBES.none { it.provesToolAccess })
        assertTrue(AgenticReviewQualification.routedBoundaryHolds())
    }

    @Test
    fun `the boundary rests on probes whose answers could not be guessed`() {
        // PG-1 produced a confident fabrication. A finding resting on it would be
        // resting on the absence of evidence. PG-5 and PG-6 ask for a commit SHA
        // and a just-committed file's first line, which only real access could
        // produce, and both were refused.
        val decisive = ParagonPlainInferenceBoundary.PROBES.filter { it.groundTruthKnown }
        assertTrue(decisive.map { it.id }.containsAll(listOf("PG-5", "PG-6")))
        val pg1 = ParagonPlainInferenceBoundary.PROBES.single { it.id == "PG-1" }
        assertTrue(pg1.observed.contains("fabricated"))
        assertFalse(pg1.provesToolAccess)
    }

    @Test
    fun `a tool enumeration is recorded but never load-bearing`() {
        val pg4 = ParagonPlainInferenceBoundary.PROBES.single { it.id == "PG-4" }
        assertFalse(pg4.groundTruthKnown)
    }

    @Test
    fun `the route became usable only after the ingestion fix, and the history is kept`() {
        // D016-H cleared this. The four D016-G refusals stay recorded rather than
        // being deleted, because the reason the route works now is exactly the
        // reason it did not work then.
        assertTrue(ParagonPlainInferenceBoundary.ROUTE_ACCEPTS_REVIEW_REQUESTS)
        assertTrue(ParagonPlainInferenceBoundary.usableRouteExists())
        assertTrue(AgenticReviewQualification.usableReviewRouteExists())
        assertEquals(4, ParagonPlainInferenceBoundary.ROUTE_ATTEMPTS.count { !it.usable })
        assertEquals(
            listOf("RE-5"),
            ParagonPlainInferenceBoundary.ROUTE_ATTEMPTS.filter { it.usable }.map { it.id },
        )
    }

    @Test
    fun `the state names the measured qualification failure, not a superseded blocker`() {
        // Both the tool-surface and route blockers are genuinely cleared, so
        // reporting either would be false. The harness is unqualified now because
        // a reviewer was measured and failed, which is a different and stronger
        // statement than never having run one.
        assertEquals(
            AgenticReviewQualification.STATE_UNQUALIFIED,
            AgenticReviewQualification.state(
                { if (it == ParagonBackend.CREDENTIAL_ENV) "present" else null },
                MetaEvaluationSuite.run(),
            ),
        )
    }

    @Test
    fun `the router refusal is recorded by its own code rather than paraphrased`() {
        assertEquals(
            "routing.unknownContextForLargeRequest",
            ParagonPlainInferenceBoundary.REFUSAL_CODE,
        )
        val re3 = ParagonPlainInferenceBoundary.ROUTE_ATTEMPTS.single { it.id == "RE-3" }
        // Automatic routing does answer — with the route D016-F disqualified. An
        // available answer is not a usable one, and this must not read as success.
        assertFalse(re3.usable)
    }

    @Test
    fun `the causes are observed facts and the remedies are the owner's to make`() {
        assertEquals(3, ParagonPlainInferenceBoundary.CAUSES.size)
        assertEquals(3, ParagonPlainInferenceBoundary.REMEDIES.size)
        assertTrue(
            ParagonPlainInferenceBoundary.ESTIMATED_REQUIRED_CONTEXT_TOKENS >
                ParagonPlainInferenceBoundary.ACTUAL_REQUEST_TOKENS_APPROX * 10,
            "the recorded demand should dwarf the real request; that is the finding",
        )
    }

    @Test
    fun `no credential appears in the boundary record`() {
        val rendered = ParagonPlainInferenceBoundary.render()
        assertFalse(rendered.contains("Bearer"))
        assertFalse(rendered.contains("Authorization"))
    }

    // ------------------------------------------------- the unrun runner

    @Test
    fun `the model-evaluable fixture set excludes deliberately broken doubles`() {
        val evaluable = RealReviewerQualification.modelEvaluable().map { it.id }
        val excluded = RealReviewerQualification.excluded().map { it.id }
        assertTrue(evaluable.isNotEmpty())
        assertTrue(excluded.isNotEmpty())
        // Disjoint and exhaustive: no fixture may be silently dropped.
        assertEquals(
            MetaEvaluationSuite.fixtures().size,
            evaluable.size + excluded.size,
        )
        assertTrue(evaluable.intersect(excluded.toSet()).isEmpty())
    }

    @Test
    fun `the runner reads the frozen thresholds rather than restating them`() {
        // A metric that carried its own bar could be relaxed without touching the
        // frozen file. These must be the same objects the thresholds define.
        val calls = mutableListOf<Int>()
        val runner = RealReviewerQualification(
            primaryFactory = { CompliantScriptedReviewer().also { calls += 1 } },
            alternateFactory = { CompliantScriptedReviewer().also { calls += 1 } },
        )
        val result = runner.run()
        val byId = result.metrics.associateBy { it.id }
        assertEquals(
            QualificationThresholds.MIN_EXPECTED_OUTCOME_RATE,
            byId.getValue("expectedOutcomeRate").threshold,
        )
        assertEquals(
            QualificationThresholds.MIN_INJECTION_RESISTANCE,
            byId.getValue("injectionResistance").threshold,
        )
        assertEquals(
            QualificationThresholds.MAX_ABSTENTION_RATE,
            byId.getValue("abstentionRate").threshold,
        )
        assertTrue(calls.isNotEmpty())
    }

    @Test
    fun `a metric with a worse value than its bar is not met`() {
        val high = Metric("x", 0.80, 0.95, higherIsBetter = true, sampleSize = 10, detail = "")
        val low = Metric("y", 0.40, 0.20, higherIsBetter = false, sampleSize = 10, detail = "")
        assertFalse(high.met)
        assertFalse(low.met)
        assertTrue(Metric("z", 0.96, 0.95, true, 10, "").met)
    }
}
