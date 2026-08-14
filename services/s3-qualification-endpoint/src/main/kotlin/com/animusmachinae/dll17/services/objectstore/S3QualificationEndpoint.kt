package com.animusmachinae.dll17.services.objectstore

import com.animusmachinae.dll17.core.crypto.HmacSha256
import com.animusmachinae.dll17.core.crypto.Sha256
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * An S3-compatible object endpoint, for qualification.
 *
 * ### What it is for
 *
 * Proving that the recovery provider's *network path* works: real sockets, a
 * real HTTP request line, real SigV4 verification over the bytes that actually
 * arrived, and real failure injection at the points where a network provider
 * fails and a filesystem one cannot. That check has to run anywhere the build
 * runs, including CI, where reaching a third-party object store is neither
 * available nor appropriate.
 *
 * ### What it is not for
 *
 * Standing in for a real provider. D014 requires qualification against an actual
 * compatible endpoint separately, and `R014_NETWORK_GATE.md` records the two
 * runs separately and does not average them. This endpoint implements the S3
 * subset the frozen provider contract uses and nothing more; a real provider has
 * behaviours it does not model, which is exactly why the separate run exists.
 *
 * ### The signature check is independent
 *
 * Nothing here calls the signer under test. The canonical request is rebuilt from
 * the request line, headers and body as received, and the signature recomputed
 * from the derivation below. A verifier that shared code with its subject would
 * agree with it by construction and prove nothing.
 */
public class S3QualificationEndpoint(
    private val bucket: String,
    private val accessKeyId: String,
    private val secretAccessKey: String,
    private val region: String = "us-east-1",
    port: Int = 0,
    threads: Int = 8,
) {

    public companion object {
        public const val ENDPOINT_ID: String = "S3_QUALIFICATION_ENDPOINT_V1"
        private const val ALGORITHM = "AWS4-HMAC-SHA256"
    }

    /** One stored object. Bytes plus what the provider is entitled to read back. */
    public class StoredObject(
        public val bytes: ByteArray,
        public val eTag: String,
        public val checksumSha256Base64: String,
    )

    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", port), 32)
    private val executor: ThreadPoolExecutor =
        Executors.newFixedThreadPool(threads) as ThreadPoolExecutor
    private val objects = LinkedHashMap<String, StoredObject>()
    private val lock = Any()

    // ------------------------------------------------------- failure injection

    /** Refuses every request with 503. Models a provider outage. */
    public var unavailable: Boolean = false

    /** Fails the next N mutating requests with 500, then serves normally. */
    public var failNextWith5xx: Int = 0

    /** Answers the next request with 429 and a `Retry-After`. */
    public var rateLimitNext: Boolean = false

    /** Closes the connection mid-body. Models an interrupted transfer. */
    public var truncateNextResponse: Boolean = false

    /** Delays every response by this many milliseconds. Models a slow provider. */
    public var latencyMillis: Long = 0L

    /** Refuses every signature, whatever it is. Models a bad credential. */
    public var refuseAllSignatures: Boolean = false

    /**
     * Caps the page size regardless of what the client asked for.
     *
     * Real endpoints do this, and a bucket small enough to fit in one page is
     * exactly the bucket that never exercises the pagination path. A provider
     * that silently returns only the first page reports the wrong current
     * point — which is the package a cold device would restore.
     */
    public var maxKeysCeiling: Int = 1000

    /** Counts of requests seen, by method. Evidence for the retry matrix. */
    public val requestCounts: MutableMap<String, Int> = LinkedHashMap()

    /**
     * Every request line the endpoint has seen, with the request body's size.
     *
     * Used by the privacy qualification: what a provider *could* learn is
     * exactly what is in this list, so the check asserts against the list rather
     * than against an intention.
     */
    public val observedRequests: MutableList<String> = ArrayList()

    public val port: Int get() = server.address.port
    public val endpointUrl: String get() = "http://127.0.0.1:$port"

    public fun storedObject(key: String): StoredObject? = synchronized(lock) { objects[key] }

    public fun storedKeys(): List<String> = synchronized(lock) { objects.keys.toList() }

    public fun start() {
        server.createContext("/") { exchange -> handle(exchange) }
        server.executor = executor
        server.start()
    }

    public fun stop(delaySeconds: Int = 0) {
        server.stop(delaySeconds)
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    // ------------------------------------------------------------- handling

    private fun handle(exchange: HttpExchange) {
        try {
            val body = exchange.requestBody.use { it.readBytes() }
            val method = exchange.requestMethod
            val rawPath = exchange.requestURI.rawPath
            val rawQuery = exchange.requestURI.rawQuery ?: ""

            synchronized(lock) {
                requestCounts[method] = (requestCounts[method] ?: 0) + 1
                observedRequests += "$method $rawPath?$rawQuery bytes=${body.size}"
            }

            if (latencyMillis > 0) Thread.sleep(latencyMillis)
            if (unavailable) return error(exchange, 503, "ServiceUnavailable")
            if (rateLimitNext) {
                rateLimitNext = false
                exchange.responseHeaders.add("Retry-After", "1")
                return error(exchange, 429, "SlowDown")
            }
            if (failNextWith5xx > 0) {
                failNextWith5xx -= 1
                return error(exchange, 500, "InternalError")
            }
            if (!verifySignature(exchange, method, rawPath, rawQuery, body)) {
                return error(exchange, 403, "SignatureDoesNotMatch")
            }

            // Path-style addressing only: `/bucket` or `/bucket/key`.
            val trimmed = rawPath.removePrefix("/")
            val slash = trimmed.indexOf('/')
            val requestBucket = if (slash < 0) trimmed else trimmed.substring(0, slash)
            val key = if (slash < 0) "" else URLDecoder.decode(trimmed.substring(slash + 1), "UTF-8")
            if (requestBucket != bucket) return error(exchange, 404, "NoSuchBucket")

            when {
                method == "PUT" && key.isNotEmpty() -> put(exchange, key, body)
                method == "GET" && key.isNotEmpty() -> get(exchange, key)
                method == "DELETE" && key.isNotEmpty() -> delete(exchange, key)
                method == "GET" && key.isEmpty() -> list(exchange, rawQuery)
                else -> error(exchange, 405, "MethodNotAllowed")
            }
        } catch (unexpected: Exception) {
            error(exchange, 500, "InternalError")
        }
    }

    private fun put(exchange: HttpExchange, key: String, body: ByteArray) {
        val digest = Sha256.digest(body)
        val checksum = Base64.getEncoder().encodeToString(digest)
        val declared = exchange.requestHeaders.getFirst("x-amz-checksum-sha256")
        // The reason the provider sends a checksum at all: a real S3-compatible
        // endpoint verifies it and refuses a payload that does not match, which
        // makes a 200 an integrity receipt rather than a promise.
        if (declared != null && declared != checksum) {
            return error(exchange, 400, "BadDigest")
        }
        val eTag = "\"" + hex(digest).take(32) + "\""
        synchronized(lock) { objects[key] = StoredObject(body.copyOf(), eTag, checksum) }
        exchange.responseHeaders.add("ETag", eTag)
        exchange.responseHeaders.add("x-amz-checksum-sha256", checksum)
        exchange.responseHeaders.add("x-amz-request-id", "qual-" + hex(digest).take(16))
        respond(exchange, 200, ByteArray(0), "application/xml")
    }

    private fun get(exchange: HttpExchange, key: String) {
        val stored = synchronized(lock) { objects[key] } ?: return error(exchange, 404, "NoSuchKey")
        exchange.responseHeaders.add("ETag", stored.eTag)
        exchange.responseHeaders.add("x-amz-checksum-sha256", stored.checksumSha256Base64)
        if (truncateNextResponse) {
            truncateNextResponse = false
            // Declare the full length and then send half of it, so the client
            // sees the transfer die the way a real interrupted download does.
            exchange.sendResponseHeaders(200, stored.bytes.size.toLong())
            exchange.responseBody.write(stored.bytes, 0, stored.bytes.size / 2)
            exchange.close()
            return
        }
        respond(exchange, 200, stored.bytes, "application/octet-stream")
    }

    private fun delete(exchange: HttpExchange, key: String) {
        synchronized(lock) { objects.remove(key) }
        // S3 answers 204 whether or not the key existed. Idempotent deletion is
        // what lets a retried revocation converge.
        respond(exchange, 204, ByteArray(0), "application/xml")
    }

    private fun list(exchange: HttpExchange, rawQuery: String) {
        val parameters = rawQuery.split('&').filter { it.isNotEmpty() }.associate {
            val eq = it.indexOf('=')
            if (eq < 0) {
                URLDecoder.decode(it, "UTF-8") to ""
            } else {
                URLDecoder.decode(it.substring(0, eq), "UTF-8") to
                    URLDecoder.decode(it.substring(eq + 1), "UTF-8")
            }
        }
        if (parameters["list-type"] != "2") return error(exchange, 400, "InvalidArgument")
        val prefix = parameters["prefix"] ?: ""
        val after = parameters["continuation-token"]
        val maxKeys = minOf(parameters["max-keys"]?.toIntOrNull() ?: 1000, maxKeysCeiling)

        val matching = synchronized(lock) {
            objects.keys.filter { it.startsWith(prefix) }.sorted()
        }.let { keys -> if (after == null) keys else keys.filter { it > after } }
        val page = matching.take(maxKeys)
        val truncated = matching.size > page.size

        val xml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            append("<ListBucketResult><Name>").append(bucket).append("</Name>")
            append("<Prefix>").append(escape(prefix)).append("</Prefix>")
            append("<KeyCount>").append(page.size).append("</KeyCount>")
            append("<IsTruncated>").append(truncated).append("</IsTruncated>")
            if (truncated) {
                append("<NextContinuationToken>").append(escape(page.last()))
                    .append("</NextContinuationToken>")
            }
            for (key in page) {
                val size = synchronized(lock) { objects[key]?.bytes?.size ?: 0 }
                append("<Contents><Key>").append(escape(key)).append("</Key>")
                append("<Size>").append(size).append("</Size></Contents>")
            }
            append("</ListBucketResult>")
        }
        respond(exchange, 200, xml.toByteArray(Charsets.UTF_8), "application/xml")
    }

    // ------------------------------------------------------------- signature

    /**
     * Rebuilds the canonical request from what arrived and recomputes the
     * signature. Independent of the signer under test by construction.
     */
    private fun verifySignature(
        exchange: HttpExchange,
        method: String,
        rawPath: String,
        rawQuery: String,
        body: ByteArray,
    ): Boolean {
        if (refuseAllSignatures) return false
        val authorization = exchange.requestHeaders.getFirst("Authorization") ?: return false
        if (!authorization.startsWith("$ALGORITHM ")) return false

        val parts = authorization.removePrefix("$ALGORITHM ").split(",").map { it.trim() }
        val credential = parts.firstOrNull { it.startsWith("Credential=") }
            ?.removePrefix("Credential=") ?: return false
        val signedHeaders = parts.firstOrNull { it.startsWith("SignedHeaders=") }
            ?.removePrefix("SignedHeaders=") ?: return false
        val presented = parts.firstOrNull { it.startsWith("Signature=") }
            ?.removePrefix("Signature=") ?: return false

        val credentialParts = credential.split("/")
        if (credentialParts.size != 5) return false
        if (credentialParts[0] != accessKeyId) return false
        val dateStamp = credentialParts[1]
        val credentialRegion = credentialParts[2]
        if (credentialRegion != region) return false

        val amzDate = exchange.requestHeaders.getFirst("x-amz-date") ?: return false
        val declaredPayloadHash =
            exchange.requestHeaders.getFirst("x-amz-content-sha256") ?: return false
        // The payload hash is signed, so checking it against the body proves the
        // body was not altered in flight rather than merely that a hash was sent.
        if (declaredPayloadHash != hex(Sha256.digest(body))) return false

        val canonicalHeaders = StringBuilder()
        for (name in signedHeaders.split(";")) {
            val value = exchange.requestHeaders.getFirst(name) ?: return false
            canonicalHeaders.append(name).append(':')
                .append(value.trim().replace(Regex("\\s+"), " ")).append('\n')
        }
        val canonicalQuery = rawQuery.split('&').filter { it.isNotEmpty() }.sorted().joinToString("&")
        val canonicalRequest = buildString {
            append(method).append('\n')
            append(rawPath.ifEmpty { "/" }).append('\n')
            append(canonicalQuery).append('\n')
            append(canonicalHeaders).append('\n')
            append(signedHeaders).append('\n')
            append(declaredPayloadHash)
        }
        val stringToSign = buildString {
            append(ALGORITHM).append('\n')
            append(amzDate).append('\n')
            append("$dateStamp/$region/s3/aws4_request").append('\n')
            append(hex(Sha256.digest(canonicalRequest.toByteArray(Charsets.UTF_8))))
        }
        var key = HmacSha256.mac(
            "AWS4$secretAccessKey".toByteArray(Charsets.UTF_8),
            dateStamp.toByteArray(Charsets.UTF_8),
        )
        key = HmacSha256.mac(key, region.toByteArray(Charsets.UTF_8))
        key = HmacSha256.mac(key, "s3".toByteArray(Charsets.UTF_8))
        key = HmacSha256.mac(key, "aws4_request".toByteArray(Charsets.UTF_8))
        val expected = hex(HmacSha256.mac(key, stringToSign.toByteArray(Charsets.UTF_8)))
        return HmacSha256.constantTimeEquals(
            expected.toByteArray(Charsets.UTF_8),
            presented.toByteArray(Charsets.UTF_8),
        )
    }

    // ------------------------------------------------------------- internals

    private fun error(exchange: HttpExchange, status: Int, code: String) {
        val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>$code</Code></Error>"
        respond(exchange, status, xml.toByteArray(Charsets.UTF_8), "application/xml")
    }

    private fun respond(exchange: HttpExchange, status: Int, body: ByteArray, contentType: String) {
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(status, if (body.isEmpty()) -1L else body.size.toLong())
        if (body.isNotEmpty()) exchange.responseBody.use { it.write(body) }
        exchange.close()
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }
}
