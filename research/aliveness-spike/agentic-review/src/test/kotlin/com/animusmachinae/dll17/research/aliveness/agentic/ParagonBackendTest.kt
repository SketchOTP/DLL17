package com.animusmachinae.dll17.research.aliveness.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * D016-F. Two separable claims are tested here, and keeping them separate is the
 * point of the directive's outcome.
 *
 * The first is about the request this project sends to the router, which is
 * checkable from the bytes and holds. The second is about what the router hands
 * the prompt to, which is not checkable from this repository and does not hold.
 * A test suite that blurred them would let a tool-free request stand in as proof
 * of a tool-free reviewer, which is precisely the mistake D016-F found.
 */
class ParagonBackendTest {

    private fun request(): ReviewRequest = ReviewRequest(
        roleContract = AgenticRoleContracts.PRIMARY,
        question = ReviewQuestion("Q-1", "Does the frozen evidence support activation?"),
        bundle = EvidenceBundle(
            bundleId = "B-1",
            items = listOf(
                EvidenceItem("E1", EvidenceKind.GOVERNANCE_RECORD, "frozen item", true),
            ),
        ),
    )

    private fun backend(
        response: HttpResponseSpec,
        recorder: RecordingTransport = RecordingTransport(response),
    ): Pair<ParagonBackend, RecordingTransport> {
        val b = ParagonBackend(
            modelId = "paragon",
            transport = recorder,
            apiKey = "test-key-not-real",
        )
        return b to recorder
    }

    private fun okBody(content: String): String =
        """{"id":"chatcmpl-1","object":"chat.completion","model":"paragon",""" +
            """"paragon":{"provider":"codex","routedProvider":"codex","fallback":false},""" +
            """"choices":[{"index":0,"message":{"role":"assistant","content":"$content"},""" +
            """"finish_reason":"stop"}],""" +
            """"usage":{"paragon_usage_source":"provider_cli_structured"}}"""

    // ------------------------------------------------- the request we send

    @Test
    fun `the paragon request carries no tools and forces tool_choice none`() {
        val (b, t) = backend(HttpResponseSpec(200, okBody("VERDICT: PASS")))
        b.invoke(request())
        val body = t.onlyRequest().body
        val keys = (Json.parse(body) as JsonValue.Obj).keysDeep()
        assertEquals(setOf("tool_choice"), keys.intersect(TOOL_BEARING_KEYS))
        val choice = (Json.parse(body) as JsonValue.Obj)["tool_choice"]
        assertEquals("none", (choice as JsonValue.Str).value)
    }

    @Test
    fun `the paragon backend appears in the isolation self-check and is tool-free`() {
        val finding = ApiReviewerIsolationSelfCheck.findings().single { it.label == "paragon-router" }
        assertTrue(finding.toolFree)
        assertTrue(finding.endpoint.endsWith("/chat/completions"))
    }

    @Test
    fun `the role contract and the evidence actually reach the request`() {
        val (b, t) = backend(HttpResponseSpec(200, okBody("VERDICT: PASS")))
        b.invoke(request())
        val body = t.onlyRequest().body
        assertTrue(body.contains("PrimaryAgenticAlivenessGateReviewer"))
        assertTrue(body.contains("frozen item"))
    }

    @Test
    fun `the credential never appears in the recorded or hashed request form`() {
        val (b, t) = backend(HttpResponseSpec(200, okBody("VERDICT: PASS")))
        b.invoke(request())
        val spec = t.onlyRequest()
        assertFalse(spec.sanitized().contains("test-key-not-real"))
        assertTrue(spec.sanitized().contains(REDACTED))
        assertFalse(spec.sanitizedHash().contains("test-key-not-real"))
    }

    // ------------------------------------------------- routing observability

    @Test
    fun `routing metadata is recorded when the router exposes it`() {
        val (b, _) = backend(HttpResponseSpec(200, okBody("VERDICT: PASS")))
        b.invoke(request())
        val routing = b.lastRouting!!
        assertTrue(routing.observable)
        assertEquals("codex", routing.routedProvider)
        assertEquals("provider_cli_structured", routing.usageSource)
        assertFalse(routing.render().contains(PARAGON_ROUTING_UNOBSERVABLE))
    }

    @Test
    fun `routing is reported unobservable rather than invented when absent`() {
        val bare = """{"id":"x","choices":[{"message":{"content":"VERDICT: PASS"}}]}"""
        val (b, _) = backend(HttpResponseSpec(200, bare))
        b.invoke(request())
        val routing = b.lastRouting!!
        assertFalse(routing.observable)
        assertEquals(PARAGON_ROUTING_UNOBSERVABLE, routing.render())
    }

    // ------------------------------------------------- response handling

    @Test
    fun `a refusal is never returned as a ruling`() {
        val refusal = """{"id":"x","choices":[{"message":{"content":"","refusal":"declined"}}]}"""
        val (b, _) = backend(HttpResponseSpec(200, refusal))
        assertTrue(b.invoke(request()) is BackendOutcome.Refused)
    }

    @Test
    fun `a 4xx is a refusal and a 5xx is a transport failure`() {
        val (b1, _) = backend(HttpResponseSpec(400, "{}"))
        assertTrue(b1.invoke(request()) is BackendOutcome.Refused)
        val (b2, _) = backend(HttpResponseSpec(503, "{}"))
        assertTrue(b2.invoke(request()) is BackendOutcome.TransportFailed)
    }

    @Test
    fun `an unparseable response fails closed rather than producing a verdict`() {
        val (b, _) = backend(HttpResponseSpec(200, "not json at all"))
        assertTrue(b.invoke(request()) is BackendOutcome.TransportFailed)
    }

    // ------------------------------------------------- the routed boundary

    @Test
    fun `the routed boundary does not hold and names the probes that show it`() {
        assertFalse(ParagonReviewerBoundary.callerControlsToolSurface())
        val shown = ParagonReviewerBoundary.demonstrations().map { it.id }
        assertEquals(listOf("PB-3", "PB-4"), shown)
    }

    @Test
    fun `the blocking probe is the one made under an explicit refusal of tools`() {
        // PB-4 is the load-bearing one: PB-3 alone could be argued away as a
        // request that simply never said no. PB-4 said no in both available ways
        // and was overridden anyway, which is what makes this a boundary the
        // caller cannot reach rather than a configuration mistake.
        val pb4 = ParagonReviewerBoundary.PROBES.single { it.id == "PB-4" }
        assertTrue(pb4.requestForm.contains("tool_choice=none"))
        assertTrue(pb4.requestForm.contains("tools=[]"))
        assertTrue(pb4.demonstratesToolAccess)
    }

    @Test
    fun `self-reported enumeration is recorded but is never load-bearing`() {
        // D016-D's lesson: an enumeration is a lower bound on exposure, so the
        // finding must not rest on one. PB-2 is recorded and excluded.
        val pb2 = ParagonReviewerBoundary.PROBES.single { it.id == "PB-2" }
        assertFalse(pb2.demonstratesToolAccess)
    }

    @Test
    fun `the boundary record is reachable but not caller-controlled`() {
        // The distinction the report rests on: this is not a connectivity blocker.
        assertTrue(ParagonReviewerBoundary.ENDPOINT_REACHABLE)
        assertTrue(ParagonReviewerBoundary.AUTHENTICATION_ACCEPTED)
        assertFalse(ParagonReviewerBoundary.callerControlsToolSurface())
    }

    @Test
    fun `no credential appears anywhere in the boundary record`() {
        // Asserted structurally rather than against the literal secret, because a
        // test that names the value would itself commit it. The record carries an
        // endpoint and probe prose and has no field a credential could occupy.
        val rendered = ParagonReviewerBoundary.render()
        assertFalse(rendered.contains("Bearer"))
        assertFalse(rendered.contains("Authorization"))
        assertFalse(rendered.contains("api_key", ignoreCase = true))
    }

    @Test
    fun `no formal fixture was ever sent to the router`() {
        // D016-F forbids showing scored fixtures during protocol discovery. The
        // probes are recorded with what they asked, so this is checkable rather
        // than merely promised.
        for (probe in ParagonReviewerBoundary.PROBES) {
            assertFalse(probe.asked.contains("VERDICT"))
            assertNull(
                MetaEvaluationSuite.run().firstOrNull { probe.asked.contains(it.fixture.id) },
            )
        }
    }
}
