package com.animusmachinae.dll17.research.aliveness.agentic

/**
 * Direct model-API reviewer backends, per D016-E.
 *
 * The governing property of this file: the serialized request IS the reviewer's
 * entire tool surface. There is no account-level connector, no server-side web
 * tool and no client-side agent loop, because there is no agent — there is one
 * HTTP request carrying instructions and evidence, and one response carrying
 * text.
 *
 * Both builders are checked by [assertToolFree] before the request leaves, so a
 * future edit that adds a tool field fails at run time as well as in the tests.
 * The check is on the serialized JSON rather than on the builder's intent,
 * because the serialized form is what the provider actually receives.
 */

/** Every key that would introduce a tool surface, in either provider's schema. */
public val TOOL_BEARING_KEYS: Set<String> = setOf(
    "tools",
    "tool_choice",
    "tool_resources",
    "functions",
    "function_call",
    "toolConfig",
    "tool_config",
    "functionDeclarations",
    "function_declarations",
    "googleSearch",
    "google_search",
    "googleSearchRetrieval",
    "urlContext",
    "url_context",
    "codeExecution",
    "code_execution",
    "retrieval",
    "mcpServers",
    "mcp_servers",
)

/** Raised when a request would carry a tool surface. Never caught by this module. */
public class ToolSurfaceException(message: String) : RuntimeException(message)

/**
 * Refuses any request body carrying a tool-bearing key.
 *
 * [allowed] exists for exactly one case: the OpenAI Responses request must carry
 * `tool_choice` in order to say `none`. Suppressing a key by permitting it
 * silently would defeat the check, so the permitted key's *value* is verified
 * too, by the caller, and the permission is named at the call site.
 */
public fun assertToolFree(body: String, allowed: Set<String> = emptySet()) {
    val parsed = Json.parse(body)
    val obj = parsed as? JsonValue.Obj
        ?: throw ToolSurfaceException("request body is not a JSON object")
    val offending = obj.keysDeep().intersect(TOOL_BEARING_KEYS) - allowed
    if (offending.isNotEmpty()) {
        throw ToolSurfaceException(
            "request carries tool-bearing keys: ${offending.sorted().joinToString(",")}",
        )
    }
}

/**
 * The durable record of one API reviewer request, per D016-E section 11.
 *
 * Everything here is project-controlled and observable. Provider internals —
 * hidden system instructions, backend routing, undisclosed sampling — are
 * recorded as [UNOBSERVABLE_PROVIDER_CONTROL_PLANE] rather than guessed, and
 * their absence is not treated as a qualification failure.
 */
public const val UNOBSERVABLE_PROVIDER_CONTROL_PLANE: String =
    "UNOBSERVABLE_PROVIDER_CONTROL_PLANE"

public class ApiRequestProvenance(
    public val provider: String,
    public val requestedModelId: String,
    public val apiEndpoint: String,
    public val apiVersion: String,
    public val toolSurface: String,
    public val promptHash: String,
    public val evidenceBundleHash: String,
    public val sanitizedRequestHash: String,
    public val schemaId: String,
    public val parserVersion: Int,
    public val exposedParameters: String,
    public val responseId: String?,
    public val rawResponseHash: String?,
    public val retryCount: Int,
    public val retryReasons: List<FailureMode>,
    public val hiddenProvenance: String = UNOBSERVABLE_PROVIDER_CONTROL_PLANE,
) {
    public fun render(indent: String = "    "): String = buildString {
        fun line(k: String, v: String) = append(indent).append(k.padEnd(24)).append(v).append('\n')
        line("provider", provider)
        line("requestedModelId", requestedModelId)
        line("apiEndpoint", apiEndpoint)
        line("apiVersion", apiVersion)
        line("toolSurface", toolSurface)
        line("promptHash", promptHash)
        line("evidenceBundleHash", evidenceBundleHash)
        line("sanitizedRequestHash", sanitizedRequestHash)
        line("schema", "$schemaId parserVersion=$parserVersion")
        line("exposedParameters", exposedParameters)
        line("responseId", responseId ?: UNKNOWN)
        line("rawResponseHash", rawResponseHash ?: UNKNOWN)
        line("retryCount", retryCount.toString())
        line("retryReasons", retryReasons.joinToString(",").ifEmpty { "none" })
        line("hiddenProvenance", hiddenProvenance)
    }
}

/** Shared behaviour of the two API reviewer backends. */
public abstract class ApiReviewerBackend(
    override val descriptor: ModelDescriptor,
    override val sampling: SamplingParameters,
    protected val transport: HttpTransport,
) : ReviewerBackend {

    /** No tools, in the only sense that matters: none is ever serialized. */
    override val toolPermissionManifest: List<String> = emptyList()

    /** The last request built, kept so provenance can record what was actually sent. */
    public var lastRequest: HttpRequestSpec? = null
        protected set

    public var lastResponseId: String? = null
        protected set

    protected abstract fun buildRequest(request: ReviewRequest): HttpRequestSpec

    protected abstract fun extractText(body: String): String?

    protected abstract fun extractResponseId(body: String): String?

    override fun invoke(request: ReviewRequest): BackendOutcome {
        val http = buildRequest(request)
        lastRequest = http
        val response = try {
            transport.send(http)
        } catch (e: TransportException) {
            return BackendOutcome.TransportFailed(e.message ?: "transport failure")
        }
        if (response.status !in 200..299) {
            // A provider refusal and a server error are different failures, and the
            // harness retries only one of them. 4xx is the provider declining; 5xx
            // is transport-class trouble that a retry may legitimately clear.
            return if (response.status in 400..499) {
                BackendOutcome.Refused("provider returned HTTP ${response.status}")
            } else {
                BackendOutcome.TransportFailed("provider returned HTTP ${response.status}")
            }
        }
        lastResponseId = try {
            extractResponseId(response.body)
        } catch (_: JsonParseException) {
            null
        }
        val text = try {
            extractText(response.body)
        } catch (e: JsonParseException) {
            return BackendOutcome.TransportFailed("unparseable response: ${e.message}")
        }
        return if (text.isNullOrBlank()) {
            BackendOutcome.Refused("provider returned no text content")
        } else {
            BackendOutcome.Responded(text)
        }
    }
}

/**
 * OpenAI Responses API reviewer.
 *
 * Carries no `tools` array at all, and additionally sets `tool_choice` to `none`,
 * as D016-E section 1 requires. Both, not either: omitting the array leaves the
 * decision to the provider's default, and forcing the choice makes the intent
 * explicit in the request the provider receives.
 */
public class OpenAiResponsesBackend(
    private val modelId: String,
    transport: HttpTransport,
    private val apiKey: String,
    sampling: SamplingParameters = SamplingParameters(),
    private val baseUrl: String = "https://api.openai.com",
    modelFamily: String = "openai-responses",
) : ApiReviewerBackend(
    descriptor = ModelDescriptor(
        provider = "openai",
        modelId = modelId,
        modelFamily = modelFamily,
        modelSnapshot = null,
        isRealModel = true,
    ),
    sampling = sampling,
    transport = transport,
) {
    public val endpoint: String = "$baseUrl/v1/responses"
    public val apiVersion: String = "v1"

    override fun buildRequest(request: ReviewRequest): HttpRequestSpec {
        val entries = mutableListOf<Pair<String, JsonValue>>(
            "model" to jStr(modelId),
            "input" to jArr(
                jObj(
                    "role" to jStr("user"),
                    "content" to jArr(
                        jObj(
                            "type" to jStr("input_text"),
                            "text" to jStr(request.render()),
                        ),
                    ),
                ),
            ),
            // The explicit refusal of tool use required by D016-E section 1.
            "tool_choice" to jStr("none"),
            "store" to JsonValue.Bool(false),
        )
        sampling.temperature?.let { entries += "temperature" to jNum(it) }
        sampling.topP?.let { entries += "top_p" to jNum(it) }
        sampling.maxOutputTokens?.let { entries += "max_output_tokens" to jNum(it) }

        val body = JsonValue.Obj(entries).render()
        // `tool_choice` is permitted only so that it can say `none`; the value is
        // checked here rather than assumed, so a future edit cannot turn the one
        // allowed key into a way of enabling tools.
        assertToolFree(body, allowed = setOf("tool_choice"))
        val choice = (Json.parse(body) as JsonValue.Obj)["tool_choice"]
        if ((choice as? JsonValue.Str)?.value != "none") {
            throw ToolSurfaceException("tool_choice must be \"none\", was ${choice?.render()}")
        }

        return HttpRequestSpec(
            method = "POST",
            url = endpoint,
            headers = listOf("Content-Type" to "application/json"),
            secretHeaders = listOf("Authorization" to "Bearer $apiKey"),
            body = body,
        )
    }

    override fun extractResponseId(body: String): String? =
        ((Json.parse(body) as? JsonValue.Obj)?.get("id") as? JsonValue.Str)?.value

    override fun extractText(body: String): String? {
        val root = Json.parse(body) as? JsonValue.Obj ?: return null
        // The convenience field first, then the structured form it summarizes.
        (root["output_text"] as? JsonValue.Str)?.let { return it.value }
        val output = root["output"] as? JsonValue.Arr ?: return null
        val chunks = mutableListOf<String>()
        for (item in output.items) {
            val obj = item as? JsonValue.Obj ?: continue
            val content = obj["content"] as? JsonValue.Arr ?: continue
            for (part in content.items) {
                val partObj = part as? JsonValue.Obj ?: continue
                // A refusal is not text. Returning it as if it were a ruling would
                // let a declined request reach the parser as content.
                if ((partObj["type"] as? JsonValue.Str)?.value == "refusal") continue
                (partObj["text"] as? JsonValue.Str)?.let { chunks += it.value }
            }
        }
        return chunks.joinToString("").ifBlank { null }
    }
}

/**
 * Google Gemini `generateContent` reviewer.
 *
 * Carries no `tools` and no `toolConfig` at all. The Gemini schema makes tools
 * optional, so the tool-free request is simply the request with the field absent
 * — which is why [assertToolFree] permits nothing here.
 */
public class GeminiGenerateContentBackend(
    private val modelId: String,
    transport: HttpTransport,
    private val apiKey: String,
    sampling: SamplingParameters = SamplingParameters(),
    private val baseUrl: String = "https://generativelanguage.googleapis.com",
    private val apiVersionSegment: String = "v1beta",
    modelFamily: String = "google-gemini",
) : ApiReviewerBackend(
    descriptor = ModelDescriptor(
        provider = "google",
        modelId = modelId,
        modelFamily = modelFamily,
        modelSnapshot = null,
        isRealModel = true,
    ),
    sampling = sampling,
    transport = transport,
) {
    public val endpoint: String = "$baseUrl/$apiVersionSegment/models/$modelId:generateContent"
    public val apiVersion: String = apiVersionSegment

    override fun buildRequest(request: ReviewRequest): HttpRequestSpec {
        val generationConfig = mutableListOf<Pair<String, JsonValue>>()
        sampling.temperature?.let { generationConfig += "temperature" to jNum(it) }
        sampling.topP?.let { generationConfig += "topP" to jNum(it) }
        sampling.maxOutputTokens?.let { generationConfig += "maxOutputTokens" to jNum(it) }

        val entries = mutableListOf<Pair<String, JsonValue>>(
            "contents" to jArr(
                jObj(
                    "role" to jStr("user"),
                    "parts" to jArr(jObj("text" to jStr(request.render()))),
                ),
            ),
        )
        if (generationConfig.isNotEmpty()) {
            entries += "generationConfig" to JsonValue.Obj(generationConfig)
        }

        val body = JsonValue.Obj(entries).render()
        assertToolFree(body)

        return HttpRequestSpec(
            method = "POST",
            url = endpoint,
            headers = listOf("Content-Type" to "application/json"),
            secretHeaders = listOf("x-goog-api-key" to apiKey),
            body = body,
        )
    }

    override fun extractResponseId(body: String): String? =
        ((Json.parse(body) as? JsonValue.Obj)?.get("responseId") as? JsonValue.Str)?.value

    override fun extractText(body: String): String? {
        val root = Json.parse(body) as? JsonValue.Obj ?: return null
        val candidates = root["candidates"] as? JsonValue.Arr ?: return null
        val first = candidates.items.firstOrNull() as? JsonValue.Obj ?: return null
        val content = first["content"] as? JsonValue.Obj ?: return null
        val parts = content["parts"] as? JsonValue.Arr ?: return null
        val chunks = parts.items.mapNotNull {
            ((it as? JsonValue.Obj)?.get("text") as? JsonValue.Str)?.value
        }
        return chunks.joinToString("").ifBlank { null }
    }
}

/**
 * `ApiReviewerIsolationSelfCheckV1`.
 *
 * Proves the tool-free property by building a real request through each backend
 * and inspecting the bytes that would be sent. It runs in the ordinary
 * qualification path and therefore in CI, on a machine with no credential,
 * because [RecordingTransport] never contacts a provider.
 *
 * This is what replaces D016-D's environment attestation. That attestation
 * existed because a CLI reviewer's tool surface was decided by a provider
 * account, which no repository check could see. A direct API request has no such
 * surface: what the project serializes is the whole of it, so the property
 * becomes a fact this repository can check about itself rather than a claim
 * someone has to vouch for.
 */
public object ApiReviewerIsolationSelfCheck {

    public const val CHECK_ID: String = "ApiReviewerIsolationSelfCheckV1"

    /** A request shaped like a real one; its content is irrelevant to tool-freeness. */
    private fun probeRequest(): ReviewRequest = ReviewRequest(
        roleContract = AgenticRoleContracts.PRIMARY,
        question = ReviewQuestion("SELF-CHECK", "Is the serialized request free of tools?"),
        bundle = EvidenceBundle(
            bundleId = "SELF-CHECK-BUNDLE",
            items = listOf(
                EvidenceItem("E1", EvidenceKind.GOVERNANCE_RECORD, "probe item", true),
            ),
        ),
    )

    /** One backend's result: the serialized body and whether it carried any tool key. */
    public class Finding(
        public val label: String,
        public val endpoint: String,
        public val toolFree: Boolean,
        public val detail: String,
    )

    public fun findings(): List<Finding> {
        val request = probeRequest()
        val results = mutableListOf<Finding>()

        val openAi = OpenAiResponsesBackend(
            modelId = "self-check-model",
            transport = RecordingTransport(HttpResponseSpec(200, "{}")),
            apiKey = "unused-by-the-recording-transport",
        )
        results += evaluate("openai-responses", openAi.endpoint, request) {
            openAi.invoke(it)
            openAi.lastRequest
        }

        val gemini = GeminiGenerateContentBackend(
            modelId = "self-check-model",
            transport = RecordingTransport(HttpResponseSpec(200, "{}")),
            apiKey = "unused-by-the-recording-transport",
        )
        results += evaluate("google-gemini", gemini.endpoint, request) {
            gemini.invoke(it)
            gemini.lastRequest
        }
        return results
    }

    private fun evaluate(
        label: String,
        endpoint: String,
        request: ReviewRequest,
        build: (ReviewRequest) -> HttpRequestSpec?,
    ): Finding {
        val http = try {
            build(request)
        } catch (e: ToolSurfaceException) {
            return Finding(label, endpoint, false, "refused at build: ${e.message}")
        } ?: return Finding(label, endpoint, false, "no request was built")

        val keys = (Json.parse(http.body) as JsonValue.Obj).keysDeep()
        val toolKeys = keys.intersect(TOOL_BEARING_KEYS)
        // `tool_choice` is present in the OpenAI request for the sole purpose of
        // saying `none`, so it is reported explicitly rather than hidden.
        val offending = toolKeys - setOf("tool_choice")
        val choice = (Json.parse(http.body) as JsonValue.Obj)["tool_choice"]
        val choiceValue = (choice as? JsonValue.Str)?.value
        val choiceOk = choice == null || choiceValue == "none"
        val detail = buildString {
            append("topLevelKeys=").append(
                (Json.parse(http.body) as JsonValue.Obj).entries.joinToString("|") { it.first },
            )
            append("  toolBearingKeys=")
            append(if (toolKeys.isEmpty()) "none" else toolKeys.sorted().joinToString(","))
            if (choice != null) append("  tool_choice=").append(choiceValue ?: UNKNOWN)
            if (!http.secretHeaders.isEmpty()) {
                append("  secretHeaders=")
                append(http.secretHeaders.joinToString(",") { it.first })
                append(" (redacted, never hashed)")
            }
        }
        return Finding(label, endpoint, offending.isEmpty() && choiceOk, detail)
    }

    /** True only when every backend serializes a request with no tool surface. */
    public fun holds(): Boolean = findings().all { it.toolFree }

    public fun render(): String = buildString {
        append("================================================================\n")
        append("REVIEWER TOOL SURFACE (").append(CHECK_ID).append(")\n\n")
        for (finding in findings()) {
            append(if (finding.toolFree) "  TOOL_FREE " else "  CARRIES_TOOLS ")
            append(finding.label).append('\n')
            append("             endpoint=").append(finding.endpoint).append('\n')
            append("             ").append(finding.detail).append('\n')
        }
        append("\n  toolSurfaceProven=").append(holds()).append('\n')
        append("  method: each backend serializes a real request through a recording\n")
        append("          transport and the emitted JSON is inspected for tool-bearing\n")
        append("          keys at any depth. No credential and no provider is involved,\n")
        append("          so this check runs in CI exactly as it runs here.\n\n")
    }
}
