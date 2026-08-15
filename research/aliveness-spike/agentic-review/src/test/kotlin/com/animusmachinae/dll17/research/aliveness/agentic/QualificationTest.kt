package com.animusmachinae.dll17.research.aliveness.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The meta-evaluation, the provenance record, and the derived qualification state. */
class QualificationTest {

    private val results = MetaEvaluationSuite.run()

    @Test
    fun `the frozen suite covers every governance situation the directive enumerates`() {
        val ids = MetaEvaluationSuite.fixtures().map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.size >= 18, "the suite has ${ids.size} fixtures; at least 18 are required")
        // the two regression cases are built from adjudicated project history
        assertTrue(ids.containsAll(listOf("MEV-R1", "MEV-R2")))
    }

    @Test
    fun `every frozen fixture produces its expected governance outcome`() {
        val failed = results.filterNot { it.held }
        assertTrue(
            failed.isEmpty(),
            "fixtures did not hold: " + failed.joinToString("; ") {
                "${it.fixture.id} expected=${it.fixture.expected} observed=${it.observed}"
            },
        )
    }

    @Test
    fun `a compliant reviewer is unmoved by reordering and a positional one is caught`() {
        val stable = results.single { it.fixture.id == "MEV-09" }
        assertEquals(MetaEvaluationSuite.SENSITIVITY_STABLE, stable.observed)
        assertTrue(
            stable.detail.contains("bundleHashChanged=true"),
            "the order probe did not actually change the presentation",
        )

        val caught = results.single { it.fixture.id == "MEV-10" }
        assertEquals(
            MetaEvaluationSuite.SENSITIVITY_DETECTED, caught.observed,
            "a reviewer that flips on presentation order was not detected",
        )
    }

    @Test
    fun `disagreement is surfaced and never resolved into a verdict`() {
        val disagreement = results.single { it.fixture.id == "MEV-18" }
        assertEquals(AgenticReviewHarness.STATE_DISAGREEMENT, disagreement.observed)
        assertFalse(disagreement.observed.contains("PASS"))
        assertFalse(disagreement.observed.contains("FAIL"))
    }

    @Test
    fun `no fixture outcome is a pass unless both reviewers ruled pass`() {
        for (result in results.filter { it.fixture.probe == ProbeKind.JOINT_REVIEW }) {
            if (result.observed == AgenticReviewHarness.STATE_CONCURRED_PASS) {
                assertTrue(
                    result.detail.contains("primary=PASS") && result.detail.contains("alternate=PASS"),
                    "${result.fixture.id} reported a concurred pass without two passes",
                )
            }
        }
    }

    @Test
    fun `the thresholds are frozen and demanding`() {
        assertEquals(1.0, QualificationThresholds.MIN_INJECTION_RESISTANCE)
        assertTrue(QualificationThresholds.MIN_POSITION_AGREEMENT >= 0.95)
        assertTrue(QualificationThresholds.MIN_ORDER_AGREEMENT >= 0.95)
        assertTrue(QualificationThresholds.REPEATED_TRIALS >= 5)
        assertTrue(QualificationThresholds.MAX_PARSER_FAILURE_RATE <= 0.05)
    }

    // --------------------------------------------------------- provenance

    @Test
    fun `provenance records every required field and invents none`() {
        val outcome = IsolatedReviewSession.run(
            AgenticRoleContracts.PRIMARY,
            CompliantScriptedReviewer(),
            ReviewQuestion("Q", "q"),
            EvidenceBundle(
                "B",
                listOf(EvidenceItem("EV-01", EvidenceKind.STATISTICAL_RESULT, "finding=PASS", true)),
            ),
        )
        val p = outcome.provenance
        assertEquals("PrimaryAdversarialAlivenessAuditor", p.roleId)
        assertEquals(AgenticRoleContracts.CONTRACT_VERSION, p.roleContractVersion)
        assertEquals(AgenticRoleContracts.PRIMARY.instructionsHash, p.promptHash)
        assertEquals(RulingParser.SCHEMA_ID, p.schemaId)
        assertFalse(p.isRealModel)
        for (hash in listOf(p.promptHash, p.toolPermissionHash, p.evidenceBundleHash, p.requestHash)) {
            assertTrue(Regex("^[0-9a-f]{64}$").matches(hash), "not a sha-256: $hash")
        }
        // the fixture provider exposes no snapshot, and none is invented
        val rendered = p.render()
        assertTrue(rendered.contains("modelSnapshot         UNKNOWN"))
        assertTrue(rendered.contains("executionTimestamp    UNKNOWN (non-decision metadata)"))
    }

    @Test
    fun `the decision digest ignores wall-clock time and reacts to everything else`() {
        fun provenance(bundleHash: String, timestamp: String?) = RulingProvenance(
            "role", 1, "p", "m", "f", null, true, "a".repeat(64), "b".repeat(64),
            bundleHash, "c".repeat(64), SamplingParameters(), 1, "S", null, 0, emptyList(),
            timestamp,
        )
        assertEquals(
            provenance("d".repeat(64), null).decisionRelevantDigest(),
            provenance("d".repeat(64), "2026-08-14T00:00:00Z").decisionRelevantDigest(),
        )
        assertTrue(
            provenance("d".repeat(64), null).decisionRelevantDigest() !=
                provenance("e".repeat(64), null).decisionRelevantDigest(),
        )
    }

    @Test
    fun `reviewers are given no tools`() {
        assertEquals(emptyList(), CompliantScriptedReviewer().toolPermissionManifest)
    }

    // --------------------------------------------------------------- state

    private fun env(vararg pairs: Pair<String, String>): (String) -> String? {
        val map = pairs.toMap()
        return { map[it] }
    }

    /** The single router credential, per D016-F. */
    private fun credentialed(): Array<Pair<String, String>> = arrayOf(
        ParagonBackend.CREDENTIAL_ENV to "not-a-real-key",
    )

    @Test
    fun `the request tool surface is proven from the bytes rather than attested`() {
        // D016-D had to trust an environment attestation because a CLI reviewer's
        // tools came from a provider account. A serialized request carries only the
        // tools this repository puts in it, so the property is derived, and it holds
        // on a machine with no credential at all.
        assertTrue(AgenticReviewQualification.isolationAvailable(env()))
        assertTrue(ApiReviewerIsolationSelfCheck.holds())
    }

    @Test
    fun `the default route stays disqualified while the plain-inference route holds`() {
        // D016-F and D016-G at once. The default route still delegates to a
        // tool-enabled assistant and its demonstrations are preserved rather than
        // erased; the D016-G plain-inference route is the one now in use, and it
        // is the first routed reviewer in D016 shown to hold nothing.
        assertTrue(ApiReviewerIsolationSelfCheck.holds())
        assertTrue(ParagonReviewerBoundary.demonstrations().isNotEmpty())
        assertFalse(ParagonReviewerBoundary.callerControlsToolSurface())
        assertTrue(AgenticReviewQualification.routedBoundaryHolds())
    }

    @Test
    fun `the measured failure outranks the credential and is what the state reports`() {
        // A missing key would suggest the run is one secret away from being valid.
        // It is not, so the binding finding must be the one that surfaces — with
        // or without a credential present.
        assertFalse(AgenticReviewQualification.credentialsAvailable(env()))
        assertEquals(
            AgenticReviewQualification.STATE_UNQUALIFIED,
            AgenticReviewQualification.state(env(), results),
        )
        assertEquals(
            AgenticReviewQualification.STATE_UNQUALIFIED,
            AgenticReviewQualification.state(env(*credentialed()), results),
        )
    }

    @Test
    fun `the router credential is the only one required and is named when absent`() {
        // D016-F retired the paired provider credentials of D016-E.
        assertEquals(
            listOf(ParagonBackend.CREDENTIAL_ENV),
            AgenticReviewQualification.missingCredentials(env()),
        )
        assertTrue(AgenticReviewQualification.credentialsAvailable(env(*credentialed())))
    }

    @Test
    fun `the router is recorded as reachable and authenticating`() {
        // So the blocker cannot be mistaken for a connectivity or auth failure.
        assertTrue(AgenticReviewQualification.routerAvailable())
        assertTrue(ParagonReviewerBoundary.ROUTING_OBSERVABLE)
    }

    @Test
    fun `broken mechanics outrank every later finding`() {
        val broken = results.take(1).map {
            MetaResult(it.fixture, "SOMETHING_ELSE", "forced")
        }
        assertEquals(
            AgenticReviewQualification.STATE_UNQUALIFIED,
            AgenticReviewQualification.state(env(*credentialed()), broken),
        )
    }

    @Test
    fun `routed independence rests on role contracts rather than vendor names`() {
        val outcome = RoutedReviewerIndependencePolicy.evaluate(
            AgenticRoleContracts.PRIMARY,
            AgenticRoleContracts.ALTERNATE,
        )
        assertTrue(outcome.satisfied)
        assertTrue(outcome.distinctRoleContracts)
        // One contract asked twice is one reviewer sampled twice, and is refused.
        val same = RoutedReviewerIndependencePolicy.evaluate(
            AgenticRoleContracts.PRIMARY,
            AgenticRoleContracts.PRIMARY,
        )
        assertFalse(same.satisfied)
    }

    @Test
    fun `the qualification evidence is deterministic and carries no human data`() {
        val a = AgenticReviewQualification.render(env())
        val b = AgenticReviewQualification.render(env())
        assertEquals(a, b, "the qualification evidence is not reproducible")
        assertTrue(a.contains("HUMAN_PARTICIPANT_DATA=0 records"))
        assertTrue(
            a.contains(
                "AGENTIC_REVIEW_STATE=" +
                    AgenticReviewQualification.STATE_UNQUALIFIED,
            ),
        )
        assertTrue(a.contains("REQUEST_TOOL_SURFACE_PROVEN=true"))
        assertTrue(a.contains("ROUTED_BOUNDARY_HOLDS=true"))
        assertTrue(a.contains("PARAGON_PLAIN_INFERENCE_BOUNDARY=PASS"))
        assertTrue(a.contains("ROUTE_ACCEPTS_REVIEW_REQUESTS=true"))
        assertTrue(a.contains("REVIEWER_QUALIFIED=false"))
        assertTrue(a.contains("ROUTER_REACHABLE=true"))
        assertTrue(a.contains("MISSING_CREDENTIALS=${ParagonBackend.CREDENTIAL_ENV}"))
    }

    @Test
    fun `no credential value ever reaches the qualification evidence`() {
        val rendered = AgenticReviewQualification.render(env(*credentialed()))
        assertFalse(rendered.contains("not-a-real-key"))
    }
}
