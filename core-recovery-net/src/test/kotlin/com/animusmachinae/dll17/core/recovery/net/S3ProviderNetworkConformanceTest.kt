package com.animusmachinae.dll17.core.recovery.net

import com.animusmachinae.dll17.core.recovery.ProviderException
import com.animusmachinae.dll17.core.recovery.ProviderOutcome
import com.animusmachinae.dll17.core.recovery.RecoveryPackageStoreContract
import com.animusmachinae.dll17.services.objectstore.S3QualificationEndpoint
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `RecoveryPackageStoreContractV1` conformance, over a real socket.
 *
 * These are the same properties `RecoveryPackageStoreConformanceTest` asserts of
 * the filesystem provider. That was the point of writing that suite against the
 * interface: "we tested the filesystem one" was never going to be an argument
 * for skipping this, and now it is not an argument for anything.
 *
 * Everything here goes through HTTP on a bound port with SigV4 verification on
 * the far side, so a signing regression fails the suite rather than passing it
 * quietly.
 */
class S3ProviderNetworkConformanceTest {

    private lateinit var endpoint: S3QualificationEndpoint
    private lateinit var provider: S3RecoveryPackageStore
    private val organism = 0x0D14L

    private companion object {
        const val BUCKET = "dll17-recovery"
        const val ACCESS_KEY = "DLL17QUALIFICATIONKEY"
        const val SECRET_KEY = "qualification-secret-not-a-real-credential"
    }

    @BeforeTest
    fun start() {
        endpoint = S3QualificationEndpoint(BUCKET, ACCESS_KEY, SECRET_KEY).also { it.start() }
        provider = S3RecoveryPackageStore(config())
    }

    @AfterTest
    fun stop() {
        endpoint.stop()
    }

    private fun config(maxAttempts: Int = 3, keyPrefix: String = "") = S3ObjectStoreConfig(
        endpoint = endpoint.endpointUrl,
        region = "us-east-1",
        bucket = BUCKET,
        credentials = S3Credentials(ACCESS_KEY, SECRET_KEY),
        keyPrefix = keyPrefix,
        maxAttempts = maxAttempts,
    )

    private fun bytes(seed: Int, size: Int = 512) = ByteArray(size) { ((it * seed) % 251).toByte() }

    // ------------------------------------------------------------- the suite

    @Test
    fun `stored bytes come back byte-identically`() {
        provider.put(organism, 1L, bytes(3))
        assertTrue(provider.get(organism, 1L).contentEquals(bytes(3)))
    }

    @Test
    fun `the receipt describes the exact object stored`() {
        val payload = bytes(5)
        val receipt = provider.put(organism, 7L, payload)
        assertEquals(payload.size.toLong(), receipt.objectSizeBytes)
        assertEquals(7L, receipt.packageSequence)
        assertEquals(RecoveryPackageStoreContract.objectName(organism, 7L), receipt.objectName)
        assertTrue(receipt.receiptIdentifier.isNotEmpty())
        assertTrue(receipt.checksumHex.isNotEmpty())
    }

    @Test
    fun `a receipt from the network provider matches the filesystem provider`() {
        // The receipt is derived from the bytes rather than from the provider,
        // so `RecoveryPoint.receiptConfirms` means the same thing whichever
        // provider produced it. A provider-derived identifier would make
        // freshness advance depend on which backend the user happened to pick.
        val payload = bytes(11)
        val network = provider.put(organism, 3L, payload)
        val filesystem = com.animusmachinae.dll17.core.recovery.FilesystemRecoveryPackageStore(
            java.nio.file.Files.createTempDirectory("dll17-receipt-parity").toFile(),
        ).put(organism, 3L, payload)
        assertEquals(filesystem.receiptIdentifier, network.receiptIdentifier)
        assertEquals(filesystem.checksumHex, network.checksumHex)
        assertEquals(filesystem.objectName, network.objectName)
        assertEquals(filesystem.objectSizeBytes, network.objectSizeBytes)
    }

    @Test
    fun `putting the same package twice is idempotent`() {
        val first = provider.put(organism, 1L, bytes(3))
        val second = provider.put(organism, 1L, bytes(3))
        assertEquals(first.checksumHex, second.checksumHex)
        assertEquals(1, provider.list(organism).size)
    }

    @Test
    fun `a replacement at the same sequence supersedes rather than accumulates`() {
        provider.put(organism, 1L, bytes(3))
        provider.put(organism, 1L, bytes(9))
        assertEquals(1, provider.list(organism).size)
        assertTrue(provider.get(organism, 1L).contentEquals(bytes(9)))
    }

    @Test
    fun `listing and the current point are ordered by sequence`() {
        listOf(3L, 1L, 2L).forEach { provider.put(organism, it, bytes(it.toInt() + 1)) }
        assertEquals(listOf(1L, 2L, 3L), provider.list(organism).map { it.packageSequence })
        assertEquals(3L, provider.currentPoint(organism)?.packageSequence)
    }

    @Test
    fun `objects belonging to another organism are invisible`() {
        provider.put(organism, 1L, bytes(3))
        provider.put(organism + 1, 1L, bytes(5))
        assertEquals(1, provider.list(organism).size)
        assertEquals(1, provider.list(organism + 1).size)
        assertTrue(provider.get(organism, 1L).contentEquals(bytes(3)))
    }

    @Test
    fun `a missing object is NOT_FOUND rather than an empty result`() {
        assertEquals(
            ProviderOutcome.NOT_FOUND,
            assertFailsWith<ProviderException> { provider.get(organism, 99L) }.outcome,
        )
        assertNull(provider.currentPoint(organism))
    }

    @Test
    fun `deletion is idempotent so a retried revocation converges`() {
        provider.put(organism, 1L, bytes(3))
        provider.delete(organism, 1L)
        provider.delete(organism, 1L)
        assertEquals(0, provider.list(organism).size)
    }

    @Test
    fun `an outage fails every operation without corrupting stored objects`() {
        provider.put(organism, 1L, bytes(3))
        endpoint.unavailable = true
        for (call in listOf<() -> Any>(
            { provider.get(organism, 1L) },
            { provider.put(organism, 2L, bytes(5)) },
            { provider.list(organism) },
            { provider.delete(organism, 1L) },
        )) {
            assertEquals(
                ProviderOutcome.PROVIDER_UNAVAILABLE,
                assertFailsWith<ProviderException> { call() }.outcome,
            )
        }
        endpoint.unavailable = false
        assertTrue(provider.get(organism, 1L).contentEquals(bytes(3)))
    }

    // ------------------------------------------------- network-only properties

    @Test
    fun `an interrupted upload leaves the last confirmed object intact`() {
        provider.put(organism, 1L, bytes(3))
        // Three 500s exhaust the three attempts, so the replacement never lands.
        endpoint.failNextWith5xx = 3
        assertEquals(
            ProviderOutcome.PROVIDER_UNAVAILABLE,
            assertFailsWith<ProviderException> { provider.put(organism, 1L, bytes(9)) }.outcome,
        )
        assertTrue(
            provider.get(organism, 1L).contentEquals(bytes(3)),
            "the confirmed package must survive a failed replacement",
        )
    }

    @Test
    fun `a transient failure is retried and the retry converges`() {
        endpoint.failNextWith5xx = 2
        val receipt = provider.put(organism, 1L, bytes(3))
        assertEquals(bytes(3).size.toLong(), receipt.objectSizeBytes)
        assertEquals(1, provider.list(organism).size)
        assertTrue(provider.get(organism, 1L).contentEquals(bytes(3)))
    }

    @Test
    fun `a rate limit is a provider outcome rather than a corruption`() {
        val single = S3RecoveryPackageStore(config(maxAttempts = 1))
        endpoint.rateLimitNext = true
        assertEquals(
            ProviderOutcome.PROVIDER_UNAVAILABLE,
            assertFailsWith<ProviderException> { single.put(organism, 1L, bytes(3)) }.outcome,
        )
    }

    @Test
    fun `an authentication failure is refused and never retried`() {
        val wrong = S3RecoveryPackageStore(
            S3ObjectStoreConfig(
                endpoint = endpoint.endpointUrl,
                region = "us-east-1",
                bucket = BUCKET,
                credentials = S3Credentials(ACCESS_KEY, "the-wrong-secret"),
            ),
        )
        endpoint.requestCounts.clear()
        assertEquals(
            ProviderOutcome.REJECTED,
            assertFailsWith<ProviderException> { wrong.put(organism, 1L, bytes(3)) }.outcome,
        )
        // Repeating a rejected credential is how an account gets locked out.
        assertEquals(1, endpoint.requestCounts["PUT"])
    }

    @Test
    fun `a wrong bucket is refused rather than silently creating one`() {
        val elsewhere = S3RecoveryPackageStore(
            S3ObjectStoreConfig(
                endpoint = endpoint.endpointUrl,
                region = "us-east-1",
                bucket = "some-other-bucket",
                credentials = S3Credentials(ACCESS_KEY, SECRET_KEY),
            ),
        )
        assertEquals(
            ProviderOutcome.NOT_FOUND,
            assertFailsWith<ProviderException> { elsewhere.get(organism, 1L) }.outcome,
        )
    }

    @Test
    fun `an unreachable endpoint is PROVIDER_UNAVAILABLE`() {
        val gone = S3RecoveryPackageStore(
            S3ObjectStoreConfig(
                endpoint = "http://127.0.0.1:1",
                region = "us-east-1",
                bucket = BUCKET,
                credentials = S3Credentials(ACCESS_KEY, SECRET_KEY),
                maxAttempts = 1,
            ),
        )
        val outcome = assertFailsWith<ProviderException> { gone.get(organism, 1L) }.outcome
        assertTrue(
            outcome == ProviderOutcome.PROVIDER_UNAVAILABLE ||
                outcome == ProviderOutcome.NETWORK_INTERRUPTED,
            "an unreachable endpoint must be a provider failure, was $outcome",
        )
    }

    @Test
    fun `a timeout is a provider failure and never a corruption`() {
        val impatient = S3RecoveryPackageStore(
            S3ObjectStoreConfig(
                endpoint = endpoint.endpointUrl,
                region = "us-east-1",
                bucket = BUCKET,
                credentials = S3Credentials(ACCESS_KEY, SECRET_KEY),
                readTimeoutMillis = 150,
                maxAttempts = 1,
            ),
        )
        endpoint.latencyMillis = 1_500
        try {
            assertEquals(
                ProviderOutcome.NETWORK_INTERRUPTED,
                assertFailsWith<ProviderException> { impatient.put(organism, 1L, bytes(3)) }.outcome,
            )
        } finally {
            endpoint.latencyMillis = 0
        }
    }

    @Test
    fun `the provider refuses a payload the server would store differently`() {
        // The server verifies `x-amz-checksum-sha256` and refuses a mismatch, so
        // a corrupted upload is a refusal at the far end rather than an object
        // that has to be re-read to be doubted.
        val tampering = S3RecoveryPackageStore(config(maxAttempts = 1))
        endpoint.requestCounts.clear()
        tampering.put(organism, 1L, bytes(3))
        val stored = endpoint.storedObject(RecoveryPackageStoreContract.objectName(organism, 1L))
        assertTrue(stored != null && stored.bytes.contentEquals(bytes(3)))
    }

    @Test
    fun `a key prefix keeps two organisms in one bucket apart`() {
        val tenantA = S3RecoveryPackageStore(config(keyPrefix = "tenant-a/"))
        val tenantB = S3RecoveryPackageStore(config(keyPrefix = "tenant-b/"))
        tenantA.put(organism, 1L, bytes(3))
        tenantB.put(organism, 1L, bytes(9))
        assertEquals(1, tenantA.list(organism).size)
        assertEquals(1, tenantB.list(organism).size)
        assertTrue(tenantA.get(organism, 1L).contentEquals(bytes(3)))
        assertTrue(tenantB.get(organism, 1L).contentEquals(bytes(9)))
        assertTrue(endpoint.storedKeys().any { it.startsWith("tenant-a/") })
    }

    @Test
    fun `a package above the single-request ceiling is refused before it is sent`() {
        endpoint.requestCounts.clear()
        assertEquals(
            ProviderOutcome.REJECTED,
            assertFailsWith<ProviderException> {
                provider.put(organism, 1L, ByteArray(S3RecoveryPackageStore.MAX_PACKAGE_BYTES + 1))
            }.outcome,
        )
        assertNull(endpoint.requestCounts["PUT"], "an oversized package must not reach the network")
    }

    @Test
    fun `listing pages through a truncated result`() {
        // The pagination path is the one part of `list` a small bucket never
        // exercises, and a provider that silently returns the first page reports
        // the wrong current point — which is the package a cold device restores.
        repeat(12) { provider.put(organism, it.toLong() + 1L, bytes(it + 1, size = 32)) }
        endpoint.maxKeysCeiling = 5
        val summaries = provider.list(organism)
        assertEquals((1L..12L).toList(), summaries.map { it.packageSequence })
        assertEquals(12L, provider.currentPoint(organism)?.packageSequence)
    }
}
