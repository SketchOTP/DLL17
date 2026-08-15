package com.animusmachinae.dll17.research.aliveness.agentic

/**
 * The HTTP layer for direct model-API reviewers.
 *
 * D016-D established that an assistant-product CLI is the wrong boundary for
 * independent review: its tool surface is provisioned by the provider account
 * and executed server-side, so the caller cannot remove it. D016-E replaces that
 * with direct API calls, where the request the project serializes IS the whole
 * tool surface, and can therefore be inspected and proven tool-free.
 *
 * The transport is an interface so the request builders can be tested exhaustively
 * without a credential and without contacting a provider. That is not a
 * convenience: it is what allows the tool-free property to be a machine-checked
 * fact about this repository rather than a claim about a run nobody can repeat.
 */

/** The name recorded wherever a secret would otherwise appear. */
public const val REDACTED: String = "REDACTED"

/** Recorded in provenance to state that the request carried no tool surface at all. */
public const val TOOLS_NONE: String = "TOOLS=NONE"

/**
 * One outbound HTTP request.
 *
 * [secretHeaders] is separate from [headers] and is never rendered, never
 * hashed and never logged. Keeping the split in the type means a caller cannot
 * accidentally place a credential somewhere that reaches provenance: there is no
 * field in the recorded form for it to land in.
 */
public class HttpRequestSpec(
    public val method: String,
    public val url: String,
    public val headers: List<Pair<String, String>>,
    public val secretHeaders: List<Pair<String, String>>,
    public val body: String,
) {
    /**
     * The request as it is recorded and hashed: every secret header present by
     * name, with its value replaced. The body is included verbatim, because a
     * reviewer request body carries evidence and instructions and never a key.
     */
    public fun sanitized(): String = buildString {
        append(method).append(' ').append(url).append('\n')
        for ((name, value) in headers) append(name).append(": ").append(value).append('\n')
        for ((name, _) in secretHeaders) append(name).append(": ").append(REDACTED).append('\n')
        append('\n').append(body)
    }

    public fun sanitizedHash(): String = sha256(sanitized())
}

/** One inbound HTTP response. */
public class HttpResponseSpec(
    public val status: Int,
    public val body: String,
)

/** What a transport can do. Implementations must not retry; retry policy is the harness's. */
public interface HttpTransport {
    public fun send(request: HttpRequestSpec): HttpResponseSpec
}

/** Raised by a transport that could not complete a request at all. */
public class TransportException(message: String) : RuntimeException(message)

/**
 * A transport that records what it was asked to send and replays a scripted
 * response. The only transport used by the tests, and by construction the only
 * one that can run without a credential.
 */
public class RecordingTransport(
    private val responses: List<HttpResponseSpec>,
    private val failWith: String? = null,
) : HttpTransport {
    public constructor(response: HttpResponseSpec) : this(listOf(response))

    private val _sent = mutableListOf<HttpRequestSpec>()
    public val sent: List<HttpRequestSpec> get() = _sent

    /** The single request, when exactly one was made. */
    public fun onlyRequest(): HttpRequestSpec = _sent.single()

    override fun send(request: HttpRequestSpec): HttpResponseSpec {
        _sent += request
        if (failWith != null) throw TransportException(failWith)
        return responses.getOrElse(_sent.size - 1) { responses.last() }
    }
}

/**
 * The real transport, over the JDK's own HTTP client.
 *
 * Constructed only when a credential exists. It is deliberately thin: everything
 * that decides what a reviewer is asked lives in the request builders, which are
 * fully tested against [RecordingTransport], so this class adds no behaviour that
 * escapes that coverage.
 */
public class JdkHttpTransport(
    private val timeoutSeconds: Long = 120,
) : HttpTransport {
    override fun send(request: HttpRequestSpec): HttpResponseSpec {
        val client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(timeoutSeconds))
            .build()
        val builder = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(request.url))
            .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
        for ((name, value) in request.headers) builder.header(name, value)
        for ((name, value) in request.secretHeaders) builder.header(name, value)
        builder.method(
            request.method,
            java.net.http.HttpRequest.BodyPublishers.ofString(request.body),
        )
        return try {
            val response = client.send(
                builder.build(),
                java.net.http.HttpResponse.BodyHandlers.ofString(),
            )
            HttpResponseSpec(response.statusCode(), response.body())
        } catch (e: java.io.IOException) {
            throw TransportException("transport failure: ${e.javaClass.simpleName}")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw TransportException("transport interrupted")
        }
    }
}
