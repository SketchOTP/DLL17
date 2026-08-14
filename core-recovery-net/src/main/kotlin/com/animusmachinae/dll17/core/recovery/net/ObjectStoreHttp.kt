package com.animusmachinae.dll17.core.recovery.net

import com.animusmachinae.dll17.core.recovery.ProviderException
import com.animusmachinae.dll17.core.recovery.ProviderOutcome
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Where the recovery packages live, and who we are to the provider.
 *
 * Vendor-neutral by construction: an endpoint, a region, a bucket and a key
 * prefix. Nothing here names a vendor, and nothing in the provider branches on
 * one. AWS S3, Cloudflare R2, Backblaze B2, MinIO, Garage and Ceph RGW are all
 * reached by changing [endpoint] and [region] and nothing else.
 *
 * [pathStyle] is the one genuine compatibility switch rather than a vendor flag:
 * virtual-host addressing requires a wildcard DNS name under the endpoint, which
 * self-hosted and local endpoints usually do not have.
 */
public class S3ObjectStoreConfig(
    public val endpoint: String,
    public val region: String,
    public val bucket: String,
    public val credentials: S3Credentials,
    public val keyPrefix: String = "",
    public val pathStyle: Boolean = true,
    public val connectTimeoutMillis: Int = 10_000,
    public val readTimeoutMillis: Int = 30_000,
    /** Total attempts for an idempotent operation, including the first. */
    public val maxAttempts: Int = 3,
) {
    init {
        require(endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            "endpoint must be an absolute http(s) URL"
        }
        require(bucket.isNotEmpty()) { "bucket must be set" }
        require(region.isNotEmpty()) { "region must be set" }
        require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
    }

    /** True when the transport is unencrypted. Callers must refuse this in production. */
    public val plaintextTransport: Boolean get() = endpoint.startsWith("http://")

    public fun objectKey(objectName: String): String = keyPrefix + objectName

    /**
     * Renders without the secret.
     *
     * A configuration that prints its own secret when logged is a credential
     * leak waiting for a stack trace, so [S3Credentials] has no accessible
     * rendering at all and this one does not reach for it.
     */
    override fun toString(): String =
        "S3ObjectStoreConfig(endpoint=$endpoint, region=$region, bucket=$bucket, " +
            "keyPrefix=$keyPrefix, pathStyle=$pathStyle, accessKeyId=${credentials.accessKeyId})"
}

/**
 * Credentials, supplied externally.
 *
 * There is no default, no embedded value and no file this class reads on its
 * own: something outside the organism has to hand these in. [secretAccessKey] is
 * deliberately not a `val` on the public surface and [toString] never renders it,
 * so a logged config, a crash dump or an exception message cannot carry it.
 */
public class S3Credentials(
    public val accessKeyId: String,
    secretAccessKey: String,
    public val sessionToken: String? = null,
) {
    private val secret: String = secretAccessKey

    /** Used by the signer only. Not part of any rendering, log or receipt. */
    internal fun secretAccessKey(): String = secret

    override fun toString(): String = "S3Credentials(accessKeyId=$accessKeyId, secret=REDACTED)"

    public companion object {
        public const val ACCESS_KEY_VARIABLE: String = "DLL17_RECOVERY_S3_ACCESS_KEY_ID"
        public const val SECRET_KEY_VARIABLE: String = "DLL17_RECOVERY_S3_SECRET_ACCESS_KEY"
        public const val SESSION_TOKEN_VARIABLE: String = "DLL17_RECOVERY_S3_SESSION_TOKEN"

        /** Reads credentials from the environment, or null when they are absent. */
        public fun fromEnvironment(environment: Map<String, String> = System.getenv()): S3Credentials? {
            val id = environment[ACCESS_KEY_VARIABLE]?.takeIf { it.isNotBlank() } ?: return null
            val secret = environment[SECRET_KEY_VARIABLE]?.takeIf { it.isNotBlank() } ?: return null
            return S3Credentials(id, secret, environment[SESSION_TOKEN_VARIABLE]?.takeIf { it.isNotBlank() })
        }
    }
}

/** One HTTP response, read whole. Recovery packages are bounded, so this is safe. */
public class HttpResponse(
    public val status: Int,
    public val headers: Map<String, String>,
    public val body: ByteArray,
) {
    public fun header(name: String): String? = headers[name.lowercase()]
}

/**
 * The HTTP layer.
 *
 * `HttpURLConnection` rather than `java.net.http.HttpClient`, and that is a
 * deliberate portability choice rather than an old habit: `java.net.http` does
 * not exist on Android, and this code has to run on the destination device
 * during a cold recovery. `javax.net.ssl` supplies TLS on both platforms.
 *
 * Every failure becomes a [ProviderException] carrying a [ProviderOutcome]. The
 * frozen contract requires a provider failure and a damaged package to remain
 * different things, so nothing here ever reports corruption.
 */
public object ObjectStoreHttp {

    private val AMZ_DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    public fun amzDate(instant: Instant): String = AMZ_DATE.format(instant)

    public fun send(
        config: S3ObjectStoreConfig,
        method: String,
        objectKey: String?,
        queryParameters: List<Pair<String, String>>,
        body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap(),
        now: Instant = Instant.now(),
    ): HttpResponse {
        val amzDate = amzDate(now)
        val dateStamp = amzDate.substring(0, 8)

        val base = URL(config.endpoint)
        val hostHeader = if (base.port == -1) base.host else "${base.host}:${base.port}"
        val basePath = base.path.trimEnd('/')
        val path = if (config.pathStyle) {
            "$basePath/${config.bucket}" + (objectKey?.let { "/$it" } ?: "")
        } else {
            basePath + (objectKey?.let { "/$it" } ?: "/")
        }
        val host = if (config.pathStyle) hostHeader else "${config.bucket}.$hostHeader"

        val payloadHash = S3Signing.sha256Hex(body)
        val headers = LinkedHashMap<String, String>()
        headers["host"] = host
        headers[S3Signing.CONTENT_SHA256_HEADER] = payloadHash
        headers[S3Signing.DATE_HEADER] = amzDate
        config.credentials.sessionToken?.let { headers["x-amz-security-token"] = it }
        extraHeaders.forEach { (k, v) -> headers[k.lowercase()] = v }

        val canonical = S3Signing.CanonicalRequest(
            method = method,
            path = path.ifEmpty { "/" },
            queryParameters = queryParameters,
            headers = headers,
            payloadSha256Hex = payloadHash,
        )
        val authorization = S3Signing.authorizationHeader(
            accessKeyId = config.credentials.accessKeyId,
            secretAccessKey = config.credentials.secretAccessKey(),
            region = config.region,
            amzDate = amzDate,
            dateStamp = dateStamp,
            canonical = canonical,
        )

        val query = canonical.canonicalQueryString()
        val scheme = base.protocol
        val authority = if (config.pathStyle) hostHeader else host
        val url = URL("$scheme://$authority$path" + if (query.isEmpty()) "" else "?$query")

        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = config.connectTimeoutMillis
            connection.readTimeout = config.readTimeoutMillis
            connection.instanceFollowRedirects = false
            // `Host` is set by the connection itself and must not be set twice.
            headers.filterKeys { it != "host" }
                .forEach { (name, value) -> connection.setRequestProperty(name, value) }
            connection.setRequestProperty("Authorization", authorization)
            if (body.isNotEmpty()) {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.use { it.readBytes() } ?: ByteArray(0)
            val responseHeaders = LinkedHashMap<String, String>()
            connection.headerFields.forEach { (name, values) ->
                if (name != null && values.isNotEmpty()) {
                    responseHeaders[name.lowercase()] = values.first()
                }
            }
            return HttpResponse(status, responseHeaders, payload)
        } catch (timeout: SocketTimeoutException) {
            throw ProviderException(
                ProviderOutcome.NETWORK_INTERRUPTED,
                "timed out talking to the object store: ${timeout.message}",
            )
        } catch (unknown: UnknownHostException) {
            throw ProviderException(
                ProviderOutcome.PROVIDER_UNAVAILABLE,
                "object store endpoint is not resolvable: ${unknown.message}",
            )
        } catch (io: IOException) {
            throw ProviderException(
                ProviderOutcome.NETWORK_INTERRUPTED,
                "object store request failed: ${io.message}",
            )
        } finally {
            connection.disconnect()
        }
    }
}
