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
        assertEquals("PrimaryAgenticAlivenessGateReviewer", p.roleId)
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

    @Test
    fun `with no configured reviewers the harness reports diversity unavailable`() {
        assertFalse(AgenticReviewQualification.realReviewersAvailable(env()))
        assertEquals(
            AgenticReviewQualification.STATE_DIVERSITY_UNAVAILABLE,
            AgenticReviewQualification.state(env(), results),
        )
    }

    @Test
    fun `a declared reviewer without a present credential is not available`() {
        val partial = env(
            "A001_PRIMARY_REVIEWER_PROVIDER" to "acme",
            "A001_PRIMARY_REVIEWER_MODEL" to "m-1",
            "A001_PRIMARY_REVIEWER_FAMILY" to "fam-a",
            "A001_PRIMARY_REVIEWER_CREDENTIAL_ENV" to "ACME_KEY",
        )
        assertFalse(AgenticReviewQualification.realReviewersAvailable(partial))
    }

    @Test
    fun `qualification requires the mechanics as well as real reviewers`() {
        val fullyConfigured = env(
            "A001_PRIMARY_REVIEWER_PROVIDER" to "acme",
            "A001_PRIMARY_REVIEWER_MODEL" to "m-1",
            "A001_PRIMARY_REVIEWER_FAMILY" to "fam-a",
            "A001_PRIMARY_REVIEWER_CREDENTIAL_ENV" to "ACME_KEY",
            "ACME_KEY" to "value",
            "A001_ALTERNATE_REVIEWER_PROVIDER" to "borealis",
            "A001_ALTERNATE_REVIEWER_MODEL" to "m-9",
            "A001_ALTERNATE_REVIEWER_FAMILY" to "fam-b",
            "A001_ALTERNATE_REVIEWER_CREDENTIAL_ENV" to "BOREALIS_KEY",
            "BOREALIS_KEY" to "value",
        )
        assertTrue(AgenticReviewQualification.realReviewersAvailable(fullyConfigured))
        assertEquals(
            AgenticReviewQualification.STATE_QUALIFIED,
            AgenticReviewQualification.state(fullyConfigured, results),
        )
        // and a broken mechanic outranks an available reviewer pair
        val broken = results.take(1).map {
            MetaResult(it.fixture, "SOMETHING_ELSE", "forced")
        }
        assertEquals(
            AgenticReviewQualification.STATE_UNQUALIFIED,
            AgenticReviewQualification.state(fullyConfigured, broken),
        )
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
                    AgenticReviewQualification.STATE_DIVERSITY_UNAVAILABLE,
            ),
        )
    }
}
