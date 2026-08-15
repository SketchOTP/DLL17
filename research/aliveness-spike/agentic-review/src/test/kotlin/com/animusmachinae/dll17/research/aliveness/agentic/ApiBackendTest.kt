package com.animusmachinae.dll17.research.aliveness.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * D016-E: the formal reviewers are direct API calls whose entire tool surface is
 * the request this repository serializes. These tests are the proof of that, and
 * they run without a credential and without contacting a provider, which is the
 * point — the property has to be checkable by anyone with the checkout.
 */
class ApiBackendTest {

    private val question = ReviewQuestion(
        "Q-BASELINE",
        "Is ScriptedPetBaselineV1 an adequate comparator?",
    )

    private val bundle = EvidenceBundle(
        bundleId = "B-1",
        items = listOf(
            EvidenceItem("E1", EvidenceKind.GOVERNANCE_RECORD, "the decisive item", true),
            EvidenceItem("E2", EvidenceKind.IMPLEMENTATION_FACT, "a supporting item"),
        ),
    )

    private fun request(contract: RoleContract = AgenticRoleContracts.PRIMARY) =
        ReviewRequest(contract, question, bundle)

    private fun ok(body: String) = RecordingTransport(HttpResponseSpec(200, body))

    private val openAiOk = """
        {"id":"resp_123","output":[{"content":[{"type":"output_text","text":"VERDICT: PASS"}]}]}
    """.trimIndent()

    private val geminiOk = """
        {"responseId":"gen_456","candidates":[{"content":{"parts":[{"text":"VERDICT: PASS"}]}}]}
    """.trimIndent()

    // ------------------------------------------------------------ tool surface

    @Test
    fun `the openai request carries no tools and forces tool_choice none`() {
        val transport = ok(openAiOk)
        val backend = OpenAiResponsesBackend("m-1", transport, "sk-not-a-real-key")
        backend.invoke(request())

        val body = Json.parse(transport.onlyRequest().body) as JsonValue.Obj
        // No tool array at any depth...
        assertFalse("tools" in body.keysDeep(), "the request must not declare tools")
        assertFalse("functions" in body.keysDeep())
        // ...and the refusal is explicit rather than left to a provider default.
        assertEquals("none", (body["tool_choice"] as JsonValue.Str).value)
        // The only tool-bearing key present is the one that says none.
        assertEquals(setOf("tool_choice"), body.keysDeep().intersect(TOOL_BEARING_KEYS))
    }

    @Test
    fun `the gemini request declares no tools at all`() {
        val transport = ok(geminiOk)
        val backend = GeminiGenerateContentBackend("m-9", transport, "not-a-real-key")
        backend.invoke(request())

        val body = Json.parse(transport.onlyRequest().body) as JsonValue.Obj
        // Gemini makes tools optional, so tool-free is simply the field's absence.
        assertEquals(emptySet(), body.keysDeep().intersect(TOOL_BEARING_KEYS))
    }

    @Test
    fun `the self check proves both backends tool free and is what the state reads`() {
        val findings = ApiReviewerIsolationSelfCheck.findings()
        assertEquals(2, findings.size)
        assertTrue(findings.all { it.toolFree }, "a backend serialized a tool surface")
        assertTrue(ApiReviewerIsolationSelfCheck.holds())
    }

    @Test
    fun `a request carrying any tool bearing key is refused before it is sent`() {
        for (key in listOf("tools", "googleSearch", "urlContext", "codeExecution", "functions")) {
            val body = JsonValue.Obj(listOf(key to jArr())).render()
            val failure = assertFailsWith<ToolSurfaceException> { assertToolFree(body) }
            assertTrue(failure.message!!.contains(key))
        }
    }

    @Test
    fun `a tool bearing key nested deep in the request is still caught`() {
        // The check walks the whole tree, because a tool declaration inside a
        // nested config object is exactly as effective as a top-level one.
        val body = jObj(
            "contents" to jArr(jObj("parts" to jArr(jObj("googleSearch" to jObj())))),
        ).render()
        assertFailsWith<ToolSurfaceException> { assertToolFree(body) }
    }

    // ------------------------------------------------------------- credentials

    @Test
    fun `no credential value ever reaches the recorded request form`() {
        val secret = "sk-this-value-must-never-be-recorded"
        val transport = ok(openAiOk)
        OpenAiResponsesBackend("m-1", transport, secret).invoke(request())

        val http = transport.onlyRequest()
        val recorded = http.sanitized()
        assertFalse(secret in recorded, "the credential leaked into the recorded request")
        assertTrue("Authorization: $REDACTED" in recorded)
        // ...and the same holds for the hash input, which is the recorded form.
        assertFalse(secret in http.sanitizedHash())
    }

    @Test
    fun `the gemini credential header is redacted too`() {
        val secret = "AIza-this-must-never-be-recorded"
        val transport = ok(geminiOk)
        GeminiGenerateContentBackend("m-9", transport, secret).invoke(request())
        val recorded = transport.onlyRequest().sanitized()
        assertFalse(secret in recorded)
        assertTrue("x-goog-api-key: $REDACTED" in recorded)
    }

    // ------------------------------------------------------------- the evidence

    @Test
    fun `the frozen role contract and the evidence bundle are what gets sent`() {
        val transport = ok(openAiOk)
        OpenAiResponsesBackend("m-1", transport, "k").invoke(request())
        val body = Json.parse(transport.onlyRequest().body) as JsonValue.Obj
        val text = (
            (((body["input"] as JsonValue.Arr).items[0] as JsonValue.Obj)["content"]
                as JsonValue.Arr).items[0] as JsonValue.Obj
            )["text"] as JsonValue.Str

        assertTrue(text.value.contains(AgenticRoleContracts.PRIMARY.role.roleId))
        assertTrue(text.value.contains("E1"))
        assertTrue(text.value.contains(question.text))
    }

    @Test
    fun `the alternate reviewer request is identical whatever the primary did`() {
        // The API path must not weaken D016-C's isolation property: there is no
        // parameter through which another reviewer's output could arrive, so the
        // bytes cannot depend on it.
        val a = RecordingTransport(HttpResponseSpec(200, geminiOk))
        val b = RecordingTransport(HttpResponseSpec(200, geminiOk))
        val alternate = request(AgenticRoleContracts.ALTERNATE)

        GeminiGenerateContentBackend("m-9", a, "k").invoke(alternate)
        OpenAiResponsesBackend("m-1", ok(openAiOk), "k").invoke(request())
        GeminiGenerateContentBackend("m-9", b, "k").invoke(alternate)

        assertEquals(a.onlyRequest().body, b.onlyRequest().body)
    }

    // ---------------------------------------------------------------- outcomes

    @Test
    fun `a successful response yields its text and its response id`() {
        val transport = ok(openAiOk)
        val backend = OpenAiResponsesBackend("m-1", transport, "k")
        val outcome = backend.invoke(request())
        assertTrue(outcome is BackendOutcome.Responded)
        assertEquals("VERDICT: PASS", (outcome as BackendOutcome.Responded).rawText)
        assertEquals("resp_123", backend.lastResponseId)
    }

    @Test
    fun `a gemini response yields its text and its response id`() {
        val transport = ok(geminiOk)
        val backend = GeminiGenerateContentBackend("m-9", transport, "k")
        val outcome = backend.invoke(request())
        assertEquals("VERDICT: PASS", (outcome as BackendOutcome.Responded).rawText)
        assertEquals("gen_456", backend.lastResponseId)
    }

    @Test
    fun `an openai refusal part is never returned as if it were a ruling`() {
        val refusal = """{"id":"r","output":[{"content":[{"type":"refusal","refusal":"no"}]}]}"""
        val outcome = OpenAiResponsesBackend("m-1", ok(refusal), "k").invoke(request())
        assertTrue(outcome is BackendOutcome.Refused)
    }

    @Test
    fun `a client error is a refusal and a server error is a transport failure`() {
        // Only one of these is retryable, so conflating them would let the harness
        // retry a provider that has declined.
        val refused = OpenAiResponsesBackend(
            "m-1", RecordingTransport(HttpResponseSpec(400, "{}")), "k",
        ).invoke(request())
        assertTrue(refused is BackendOutcome.Refused)

        val failed = OpenAiResponsesBackend(
            "m-1", RecordingTransport(HttpResponseSpec(503, "{}")), "k",
        ).invoke(request())
        assertTrue(failed is BackendOutcome.TransportFailed)
    }

    @Test
    fun `an unparseable response fails closed rather than becoming an empty ruling`() {
        val outcome = OpenAiResponsesBackend("m-1", ok("not json at all"), "k").invoke(request())
        assertTrue(outcome is BackendOutcome.TransportFailed)
    }

    @Test
    fun `a transport exception becomes a transport failure and never a verdict`() {
        val outcome = OpenAiResponsesBackend(
            "m-1", RecordingTransport(emptyList(), failWith = "connection reset"), "k",
        ).invoke(request())
        assertTrue(outcome is BackendOutcome.TransportFailed)
    }

    // -------------------------------------------------------------- provenance

    @Test
    fun `provenance records the tool surface and leaves hidden internals unknown`() {
        val transport = ok(openAiOk)
        val backend = OpenAiResponsesBackend("m-1", transport, "k")
        backend.invoke(request())
        val http = transport.onlyRequest()

        val provenance = ApiRequestProvenance(
            provider = backend.descriptor.provider,
            requestedModelId = backend.descriptor.modelId,
            apiEndpoint = backend.endpoint,
            apiVersion = backend.apiVersion,
            toolSurface = TOOLS_NONE,
            promptHash = sha256(request().render()),
            evidenceBundleHash = sha256(bundle.render()),
            sanitizedRequestHash = http.sanitizedHash(),
            schemaId = RulingParser.SCHEMA_ID,
            parserVersion = RulingParser.PARSER_VERSION,
            exposedParameters = backend.sampling.render(),
            responseId = backend.lastResponseId,
            rawResponseHash = sha256(openAiOk),
            retryCount = 0,
            retryReasons = emptyList(),
        )
        val rendered = provenance.render()
        assertTrue(rendered.contains(TOOLS_NONE))
        assertTrue(rendered.contains(UNOBSERVABLE_PROVIDER_CONTROL_PLANE))
        assertNotNull(provenance.responseId)
    }

    // -------------------------------------------------------------------- json

    @Test
    fun `the json round trip preserves the characters a ruling can contain`() {
        val awkward = "quote\" backslash\\ newline\n tab\t brace} bracket] unicodeé"
        val encoded = jStr(awkward).render()
        assertEquals(awkward, (Json.parse(encoded) as JsonValue.Str).value)
    }

    @Test
    fun `malformed json raises rather than returning an empty object`() {
        for (bad in listOf("{", "{\"a\":}", "[1,]", "", "{\"a\":1}trailing")) {
            assertFailsWith<JsonParseException> { Json.parse(bad) }
        }
    }
}
