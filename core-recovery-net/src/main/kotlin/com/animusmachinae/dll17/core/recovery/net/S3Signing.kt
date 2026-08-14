package com.animusmachinae.dll17.core.recovery.net

import com.animusmachinae.dll17.core.crypto.HmacSha256
import com.animusmachinae.dll17.core.crypto.Sha256

/**
 * AWS Signature Version 4 request signing, in the narrow form this project needs.
 *
 * ### Why this is here rather than a dependency
 *
 * D014 required an external landscape check before hand-building anything, and
 * one was performed and recorded as `PA-0002`. The finding was that every
 * serious S3 client is materially unsuitable for the one place this code has to
 * run: the destination device during a cold recovery. AWS states it has no plans
 * to expand Android support for the v2 Java SDK; the MinIO client resolves
 * BouncyCastle, Guava, Jackson and OkHttp, which puts a **second cryptographic
 * provider** on a device whose key hierarchy is already frozen and qualified.
 * That is the same argument that made SHA-256, HMAC, HKDF and ChaCha20-Poly1305
 * project-owned under `IMPL-0008` and `IMPL-0018`.
 *
 * What is built here is correspondingly narrow. SigV4 is HMAC-SHA-256 over a
 * canonical request; both primitives already exist in `core-crypto` and are
 * already qualified. This file signs, and does nothing else — there is no
 * credential provider chain, no region resolution, no retry policy, no XML data
 * binding and no service model. Those are the parts of an SDK that are large,
 * and this project does not need any of them.
 *
 * Qualified against the published AWS SigV4 test suite vectors in
 * `S3SigningTest`, so "we wrote our own signer" is a claim with evidence behind
 * it rather than an assertion.
 *
 * ### What is deliberately not implemented
 *
 * Chunked/streaming payload signing, multipart upload, presigned URLs,
 * `STREAMING-AWS4-HMAC-SHA256-PAYLOAD`, and session-token-less anonymous access.
 * A recovery package is a single bounded object written in one request. Nothing
 * in the frozen `RecoveryPackageStoreContractV1` needs multipart, so multipart is
 * not used, and D014's multipart qualification requirement is therefore
 * `NOT APPLICABLE` rather than skipped.
 */
public object S3Signing {

    public const val ALGORITHM: String = "AWS4-HMAC-SHA256"
    public const val SERVICE: String = "s3"
    public const val TERMINATOR: String = "aws4_request"

    /** The header carrying the payload digest. S3 requires it on every request. */
    public const val CONTENT_SHA256_HEADER: String = "x-amz-content-sha256"
    public const val DATE_HEADER: String = "x-amz-date"

    /** SHA-256 of the empty byte string, the payload digest for GET and DELETE. */
    public const val EMPTY_PAYLOAD_SHA256: String =
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    public fun hex(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()

    public fun sha256Hex(payload: ByteArray): String = hex(Sha256.digest(payload))

    /**
     * RFC 3986 encoding as S3 expects it in a canonical request.
     *
     * The slash exemption is the one place the URI path and a query parameter
     * differ: object keys may contain slashes and those stay literal, while a
     * slash inside a query value must be escaped.
     */
    public fun uriEncode(value: String, encodeSlash: Boolean): String {
        val out = StringBuilder(value.length + 16)
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt().toChar()
            when {
                c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' ||
                    c == '_' || c == '-' || c == '~' || c == '.' -> out.append(c)
                c == '/' -> out.append(if (encodeSlash) "%2F" else "/")
                else -> {
                    val v = byte.toInt() and 0xFF
                    out.append('%').append(HEX_UPPER[v ushr 4]).append(HEX_UPPER[v and 0x0F])
                }
            }
        }
        return out.toString()
    }

    private val HEX_UPPER = "0123456789ABCDEF".toCharArray()

    /**
     * A request as the signer sees it.
     *
     * Header names are lower-cased and header values trimmed on construction,
     * because the canonical request is defined over the normalized form and a
     * signer that normalizes at signing time but not at sending time produces a
     * signature the server cannot reproduce.
     */
    public class CanonicalRequest(
        public val method: String,
        public val path: String,
        public val queryParameters: List<Pair<String, String>>,
        headers: Map<String, String>,
        public val payloadSha256Hex: String,
    ) {
        public val headers: Map<String, String> =
            headers.entries
                .associate { it.key.lowercase() to it.value.trim().replace(WHITESPACE, " ") }
                .toSortedMap()

        public val signedHeaders: String get() = headers.keys.joinToString(";")

        public fun canonicalQueryString(): String =
            queryParameters
                .map { uriEncode(it.first, true) to uriEncode(it.second, true) }
                .sortedWith(compareBy({ it.first }, { it.second }))
                .joinToString("&") { "${it.first}=${it.second}" }

        public fun canonicalPath(): String =
            if (path.isEmpty()) "/" else uriEncode(path, false)

        public fun render(): String = buildString {
            append(method).append('\n')
            append(canonicalPath()).append('\n')
            append(canonicalQueryString()).append('\n')
            for ((name, value) in headers) append(name).append(':').append(value).append('\n')
            append('\n')
            append(signedHeaders).append('\n')
            append(payloadSha256Hex)
        }

        private companion object {
            private val WHITESPACE = Regex("\\s+")
        }
    }

    /** `<yyyyMMdd>/<region>/s3/aws4_request`. */
    public fun credentialScope(dateStamp: String, region: String): String =
        "$dateStamp/$region/$SERVICE/$TERMINATOR"

    public fun stringToSign(
        amzDate: String,
        dateStamp: String,
        region: String,
        canonical: CanonicalRequest,
    ): String = buildString {
        append(ALGORITHM).append('\n')
        append(amzDate).append('\n')
        append(credentialScope(dateStamp, region)).append('\n')
        append(sha256Hex(canonical.render().toByteArray(Charsets.UTF_8)))
    }

    /** The four-step key derivation. Each step keys the next; none is reversible. */
    public fun signingKey(secretAccessKey: String, dateStamp: String, region: String): ByteArray {
        val initial = ("AWS4$secretAccessKey").toByteArray(Charsets.UTF_8)
        val date = HmacSha256.mac(initial, dateStamp.toByteArray(Charsets.UTF_8))
        val regional = HmacSha256.mac(date, region.toByteArray(Charsets.UTF_8))
        val service = HmacSha256.mac(regional, SERVICE.toByteArray(Charsets.UTF_8))
        return HmacSha256.mac(service, TERMINATOR.toByteArray(Charsets.UTF_8))
    }

    /**
     * Returns the complete `Authorization` header value.
     *
     * [amzDate] is `yyyyMMdd'T'HHmmss'Z'` and [dateStamp] is its first eight
     * characters. They are passed in rather than read from a clock so the signer
     * itself is a pure function: a signature is reproducible from its inputs,
     * which is what makes the published test vectors usable as qualification.
     */
    public fun authorizationHeader(
        accessKeyId: String,
        secretAccessKey: String,
        region: String,
        amzDate: String,
        dateStamp: String,
        canonical: CanonicalRequest,
    ): String {
        val signature = hex(
            HmacSha256.mac(
                signingKey(secretAccessKey, dateStamp, region),
                stringToSign(amzDate, dateStamp, region, canonical).toByteArray(Charsets.UTF_8),
            ),
        )
        return "$ALGORITHM Credential=$accessKeyId/${credentialScope(dateStamp, region)}, " +
            "SignedHeaders=${canonical.signedHeaders}, Signature=$signature"
    }

    /**
     * The server side of the same computation, used by the in-repository
     * qualification endpoint to *verify* a signature.
     *
     * Sharing this function between signer and verifier would make the
     * qualification circular, so the endpoint recomputes the canonical request
     * from what actually arrived over the socket and only reuses the primitive
     * derivation below. A signature that verifies therefore proves the bytes on
     * the wire were the bytes signed.
     */
    public fun signatureFor(
        secretAccessKey: String,
        region: String,
        amzDate: String,
        dateStamp: String,
        canonicalRequestRendering: String,
    ): String = hex(
        HmacSha256.mac(
            signingKey(secretAccessKey, dateStamp, region),
            buildString {
                append(ALGORITHM).append('\n')
                append(amzDate).append('\n')
                append(credentialScope(dateStamp, region)).append('\n')
                append(sha256Hex(canonicalRequestRendering.toByteArray(Charsets.UTF_8)))
            }.toByteArray(Charsets.UTF_8),
        ),
    )
}
