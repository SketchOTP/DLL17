package com.animusmachinae.dll17.core.recovery.net

import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.recovery.ProviderException
import com.animusmachinae.dll17.core.recovery.ProviderOutcome
import com.animusmachinae.dll17.core.recovery.RecoveryPackageStoreContract
import com.animusmachinae.dll17.core.recovery.RecoveryPackageStoreV1
import com.animusmachinae.dll17.core.recovery.StoredObjectReceipt
import com.animusmachinae.dll17.core.recovery.StoredObjectSummary
import java.util.Base64

/**
 * `S3_COMPATIBLE_OBJECT_STORE_V1` — the network recovery provider.
 *
 * It implements the frozen `RecoveryPackageStoreContractV1` over the
 * S3-compatible object API and adds nothing to it. The contract is the reason
 * this class is small: a provider stores bytes and confirms what it stored, and
 * is never canonical authority for anything else.
 *
 * ### Vendor neutrality is structural
 *
 * There is no vendor name in this file, no per-vendor branch, and no vendor
 * capability probe. Everything that differs between AWS S3, Cloudflare R2,
 * Backblaze B2, MinIO, Ceph RGW and Garage is a value in [S3ObjectStoreConfig].
 * The one switch that exists, `pathStyle`, is an addressing choice rather than a
 * vendor flag.
 *
 * ### What crosses the boundary
 *
 * Encrypted package bytes and a name derived from the organism id and package
 * sequence. Nothing else. The provider cannot decrypt a package, cannot say
 * whether one is current, and cannot learn anything about the organism from what
 * it holds beyond the fact that an organism with that id has a backup — which is
 * exactly what a destination device must be able to discover cold. Asserted by
 * `FX-NET-PRIVACY-PAYLOAD-01`.
 *
 * ### Integrity
 *
 * Every upload carries `x-amz-checksum-sha256`. That is not decoration: it makes
 * the *server* verify the payload and refuse a corrupted one, so a `200` is
 * itself the integrity receipt rather than a claim we would have to re-read the
 * object to check. The returned ETag is recorded for the operator's audit and is
 * deliberately **not** trusted as an integrity signal, because ETag stops being a
 * content hash the moment an object is multipart-uploaded or server-side
 * encrypted with a managed key.
 */
public class S3RecoveryPackageStore(
    private val config: S3ObjectStoreConfig,
) : RecoveryPackageStoreV1 {

    override val providerId: String = PROVIDER_ID

    public companion object {
        public const val PROVIDER_ID: String = "S3_COMPATIBLE_OBJECT_STORE_V1"
        public const val CHECKSUM_HEADER: String = "x-amz-checksum-sha256"
        public const val CHECKSUM_ALGORITHM_HEADER: String = "x-amz-sdk-checksum-algorithm"

        /**
         * A recovery package larger than this is refused before it is sent.
         *
         * Single-request upload is the whole story here (see [S3Signing]), and a
         * bounded ceiling is what keeps it true. Refusing early is better than
         * discovering the provider's own limit halfway through an upload.
         */
        public const val MAX_PACKAGE_BYTES: Int = 64 * 1024 * 1024
    }

    /** Provider-side detail from the last mutating call. Evidence, never contract. */
    public class ResponseDetail(
        public val httpStatus: Int,
        public val eTag: String?,
        public val serverChecksumSha256: String?,
        public val requestId: String?,
    )

    public var lastResponseDetail: ResponseDetail? = null
        private set

    // ------------------------------------------------------------- contract

    override fun put(
        organismId: Long,
        packageSequence: Long,
        bytes: ByteArray,
    ): StoredObjectReceipt {
        if (bytes.size > MAX_PACKAGE_BYTES) {
            throw ProviderException(
                ProviderOutcome.REJECTED,
                "package of ${bytes.size} bytes exceeds the $MAX_PACKAGE_BYTES byte single-request ceiling",
            )
        }
        val name = RecoveryPackageStoreContract.objectName(organismId, packageSequence)
        val checksum = Base64.getEncoder().encodeToString(
            com.animusmachinae.dll17.core.crypto.Sha256.digest(bytes),
        )
        // Retrying is safe because `put` is idempotent on the object key: the
        // caller genuinely cannot tell whether an ambiguous failure landed, so
        // the contract requires the retry rather than merely permitting it.
        val response = attempt("put") {
            ObjectStoreHttp.send(
                config = config,
                method = "PUT",
                objectKey = config.objectKey(name),
                queryParameters = emptyList(),
                body = bytes,
                // `content-length` is deliberately not signed. It is a
                // restricted header that the connection sets for itself, so a
                // signature covering it would be computed over a value this
                // process does not fully control.
                extraHeaders = mapOf(
                    "content-type" to "application/octet-stream",
                    CHECKSUM_ALGORITHM_HEADER to "SHA256",
                    CHECKSUM_HEADER to checksum,
                ),
            )
        }
        if (response.status !in 200..299) throw failure("put $name", response)
        lastResponseDetail = ResponseDetail(
            httpStatus = response.status,
            eTag = response.header("etag"),
            serverChecksumSha256 = response.header(CHECKSUM_HEADER),
            requestId = response.header("x-amz-request-id"),
        )
        // A server that echoes a checksum must echo the one we sent. A mismatch
        // means the object in the bucket is not the object we uploaded, which is
        // a refusal rather than something to reconcile later.
        val echoed = response.header(CHECKSUM_HEADER)
        if (echoed != null && echoed != checksum) {
            throw ProviderException(
                ProviderOutcome.PARTIAL_WRITE,
                "provider stored a different payload: sent $checksum, provider reported $echoed",
            )
        }
        return StoredObjectReceipt(
            // Derived from the bytes rather than from the provider, so the
            // receipt means the same thing for every provider and
            // `RecoveryPoint.receiptConfirms` stays computable by the caller.
            receiptIdentifier = CanonicalHash.hex(CanonicalHash.ofEnvelope(bytes)).take(32),
            objectName = name,
            objectSizeBytes = bytes.size.toLong(),
            checksumHex = CanonicalHash.hex(CanonicalHash.ofEnvelope(bytes)),
            packageSequence = packageSequence,
        )
    }

    override fun get(organismId: Long, packageSequence: Long): ByteArray {
        val name = RecoveryPackageStoreContract.objectName(organismId, packageSequence)
        val response = attempt("get") {
            ObjectStoreHttp.send(config, "GET", config.objectKey(name), emptyList(), ByteArray(0))
        }
        if (response.status == 404) {
            throw ProviderException(
                ProviderOutcome.NOT_FOUND,
                "no stored package for sequence $packageSequence",
            )
        }
        if (response.status !in 200..299) throw failure("get $name", response)
        return response.body
    }

    override fun list(organismId: Long): List<StoredObjectSummary> {
        val prefix = config.keyPrefix + "dll17-%016x-".format(organismId)
        val summaries = ArrayList<StoredObjectSummary>()
        var continuationToken: String? = null
        do {
            val query = buildList {
                add("list-type" to "2")
                add("prefix" to prefix)
                continuationToken?.let { add("continuation-token" to it) }
            }
            val response = attempt("list") {
                ObjectStoreHttp.send(config, "GET", null, query, ByteArray(0))
            }
            if (response.status !in 200..299) throw failure("list", response)
            val xml = String(response.body, Charsets.UTF_8)
            for ((key, size) in ObjectListing.contents(xml)) {
                val name = key.removePrefix(config.keyPrefix)
                val sequence = ObjectListing.sequenceOf(name) ?: continue
                summaries += StoredObjectSummary(name, sequence, size)
            }
            continuationToken =
                if (ObjectListing.truncated(xml)) ObjectListing.nextToken(xml) else null
        } while (continuationToken != null)
        return summaries.sortedBy { it.packageSequence }
    }

    override fun currentPoint(organismId: Long): StoredObjectSummary? =
        list(organismId).lastOrNull()

    override fun delete(organismId: Long, packageSequence: Long) {
        val name = RecoveryPackageStoreContract.objectName(organismId, packageSequence)
        val response = attempt("delete") {
            ObjectStoreHttp.send(config, "DELETE", config.objectKey(name), emptyList(), ByteArray(0))
        }
        // 404 is a success: deleting something already gone must converge, or a
        // retried revocation fails forever on its second attempt.
        if (response.status !in 200..299 && response.status != 404) {
            throw failure("delete $name", response)
        }
        lastResponseDetail = ResponseDetail(
            httpStatus = response.status,
            eTag = null,
            serverChecksumSha256 = null,
            requestId = response.header("x-amz-request-id"),
        )
    }

    // ------------------------------------------------------------- internals

    /**
     * Retries only what is safe to retry, and only for failures that can be
     * transient. Every operation on this interface is idempotent by design, so
     * the risk a retry policy usually carries — a duplicated side effect — does
     * not exist here. An authentication refusal is never retried: repeating a
     * rejected credential is how an account gets locked.
     */
    private fun attempt(operation: String, call: () -> HttpResponse): HttpResponse {
        var lastFailure: ProviderException? = null
        for (attempt in 1..config.maxAttempts) {
            val response = try {
                call()
            } catch (provider: ProviderException) {
                lastFailure = provider
                if (attempt == config.maxAttempts) throw provider
                continue
            }
            val transient = response.status == 429 || response.status in 500..599
            if (!transient || attempt == config.maxAttempts) return response
        }
        throw lastFailure ?: ProviderException(
            ProviderOutcome.PROVIDER_UNAVAILABLE,
            "$operation exhausted ${config.maxAttempts} attempts",
        )
    }

    private fun failure(operation: String, response: HttpResponse): ProviderException {
        val outcome = when (response.status) {
            400, 411, 412, 413, 422 -> ProviderOutcome.REJECTED
            401, 403 -> ProviderOutcome.REJECTED
            404 -> ProviderOutcome.NOT_FOUND
            408 -> ProviderOutcome.NETWORK_INTERRUPTED
            429 -> ProviderOutcome.PROVIDER_UNAVAILABLE
            in 500..599 -> ProviderOutcome.PROVIDER_UNAVAILABLE
            else -> ProviderOutcome.REJECTED
        }
        // The provider's error document is quoted only by its S3 error code. A
        // provider is an untrusted party and its free text has no business in
        // this organism's logs.
        val code = ObjectListing.errorCode(String(response.body, Charsets.UTF_8))
        return ProviderException(
            outcome,
            "$operation refused by ${config.bucket}: HTTP ${response.status}" +
                (code?.let { " ($it)" } ?: ""),
        )
    }
}

/**
 * The smallest possible reader for the two S3 documents this provider sees.
 *
 * A full XML parser is available in the JDK and is not used, for a reason worth
 * writing down: `javax.xml` pulls in entity resolution, and an entity resolver
 * pointed at an untrusted document from a party that stores our backups is a
 * server-side request forgery primitive. This reader cannot resolve anything,
 * cannot expand anything, and cannot be talked into fetching a URL.
 */
internal object ObjectListing {

    fun contents(xml: String): List<Pair<String, Long>> {
        val out = ArrayList<Pair<String, Long>>()
        var index = 0
        while (true) {
            val start = xml.indexOf("<Contents>", index)
            if (start < 0) break
            val end = xml.indexOf("</Contents>", start)
            if (end < 0) break
            val block = xml.substring(start, end)
            val key = element(block, "Key")
            val size = element(block, "Size")?.toLongOrNull()
            if (key != null && size != null) out += key to size
            index = end + 1
        }
        return out
    }

    fun truncated(xml: String): Boolean = element(xml, "IsTruncated") == "true"

    fun nextToken(xml: String): String? = element(xml, "NextContinuationToken")

    fun errorCode(xml: String): String? = element(xml, "Code")

    /** `dll17-<16 hex>-<12 digits>.pkg` — the frozen name, parsed back. */
    fun sequenceOf(objectName: String): Long? {
        if (!objectName.startsWith("dll17-") || !objectName.endsWith(".pkg")) return null
        val body = objectName.removePrefix("dll17-").removeSuffix(".pkg")
        val dash = body.indexOf('-')
        if (dash != 16) return null
        return body.substring(dash + 1).toLongOrNull()
    }

    private fun element(xml: String, name: String): String? {
        val open = xml.indexOf("<$name>")
        if (open < 0) return null
        val close = xml.indexOf("</$name>", open)
        if (close < 0) return null
        return unescape(xml.substring(open + name.length + 2, close))
    }

    private fun unescape(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
}
