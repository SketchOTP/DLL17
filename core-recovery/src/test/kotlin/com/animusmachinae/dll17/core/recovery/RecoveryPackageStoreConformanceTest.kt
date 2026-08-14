package com.animusmachinae.dll17.core.recovery

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The provider conformance suite.
 *
 * Written against the interface rather than the implementation on purpose: when
 * a network provider is eventually selected, it has to pass exactly this, and
 * "we tested the filesystem one" will not be an argument for skipping it.
 */
class RecoveryPackageStoreConformanceTest {

    private val root: File = Files.createTempDirectory("dll17-provider-test").toFile()
    private val organism = 0x0D11L

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    private fun store(name: String) =
        FilesystemRecoveryPackageStore(File(root, name).apply { mkdirs() })

    private fun bytes(seed: Int, size: Int = 512) = ByteArray(size) { ((it * seed) % 251).toByte() }

    @Test
    fun `stored bytes come back byte-identically`() {
        val provider = store("roundtrip")
        provider.put(organism, 1L, bytes(3))
        assertTrue(provider.get(organism, 1L).contentEquals(bytes(3)))
    }

    @Test
    fun `the receipt describes the exact object stored`() {
        val provider = store("receipt")
        val payload = bytes(5)
        val receipt = provider.put(organism, 7L, payload)
        assertEquals(payload.size.toLong(), receipt.objectSizeBytes)
        assertEquals(7L, receipt.packageSequence)
        assertTrue(receipt.receiptIdentifier.isNotEmpty())
        assertTrue(receipt.checksumHex.isNotEmpty())
    }

    @Test
    fun `putting the same package twice is idempotent`() {
        val provider = store("idempotent")
        val first = provider.put(organism, 1L, bytes(3))
        val second = provider.put(organism, 1L, bytes(3))
        assertEquals(first.checksumHex, second.checksumHex)
        assertEquals(1, provider.list(organism).size)
    }

    @Test
    fun `a replacement at the same sequence supersedes rather than accumulates`() {
        val provider = store("replace")
        provider.put(organism, 1L, bytes(3))
        provider.put(organism, 1L, bytes(9))
        assertEquals(1, provider.list(organism).size)
        assertTrue(provider.get(organism, 1L).contentEquals(bytes(9)))
    }

    @Test
    fun `listing and the current point are ordered by sequence`() {
        val provider = store("listing")
        listOf(3L, 1L, 2L).forEach { provider.put(organism, it, bytes(it.toInt() + 1)) }
        assertEquals(listOf(1L, 2L, 3L), provider.list(organism).map { it.packageSequence })
        assertEquals(3L, provider.currentPoint(organism)?.packageSequence)
    }

    @Test
    fun `objects belonging to another organism are invisible`() {
        val provider = store("isolation")
        provider.put(organism, 1L, bytes(3))
        provider.put(organism + 1, 1L, bytes(5))
        assertEquals(1, provider.list(organism).size)
        assertEquals(1, provider.list(organism + 1).size)
        assertTrue(provider.get(organism, 1L).contentEquals(bytes(3)))
    }

    @Test
    fun `a missing object is NOT_FOUND rather than an empty result`() {
        val provider = store("missing")
        assertEquals(
            ProviderOutcome.NOT_FOUND,
            assertFailsWith<ProviderException> { provider.get(organism, 99L) }.outcome,
        )
        assertNull(provider.currentPoint(organism))
    }

    @Test
    fun `an interrupted upload leaves the previous object intact`() {
        val provider = store("interrupted")
        provider.put(organism, 1L, bytes(3))
        provider.truncateNextWrite = true
        assertEquals(
            ProviderOutcome.PARTIAL_WRITE,
            assertFailsWith<ProviderException> { provider.put(organism, 1L, bytes(9)) }.outcome,
        )
        assertTrue(
            provider.get(organism, 1L).contentEquals(bytes(3)),
            "the confirmed package must survive a failed replacement",
        )
    }

    @Test
    fun `a retry after an ambiguous failure converges`() {
        val provider = store("retry")
        provider.failNextWith = ProviderOutcome.NETWORK_INTERRUPTED
        assertFailsWith<ProviderException> { provider.put(organism, 1L, bytes(3)) }
        // The caller cannot know whether the first attempt landed, so the retry
        // must be safe either way.
        val receipt = provider.put(organism, 1L, bytes(3))
        assertEquals(1, provider.list(organism).size)
        assertEquals(bytes(3).size.toLong(), receipt.objectSizeBytes)
    }

    @Test
    fun `an outage fails every operation without corrupting stored objects`() {
        val provider = store("outage")
        provider.put(organism, 1L, bytes(3))
        provider.unavailable = true
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
        provider.unavailable = false
        assertTrue(provider.get(organism, 1L).contentEquals(bytes(3)))
    }

    @Test
    fun `deletion is idempotent so a retried revocation converges`() {
        val provider = store("delete")
        provider.put(organism, 1L, bytes(3))
        provider.delete(organism, 1L)
        provider.delete(organism, 1L)
        assertEquals(0, provider.list(organism).size)
    }

    @Test
    fun `object names are stable so a cold device can find a package`() {
        val a = RecoveryPackageStoreContract.objectName(organism, 4L)
        val b = RecoveryPackageStoreContract.objectName(organism, 4L)
        assertEquals(a, b)
        assertTrue(a.endsWith(".pkg"))
    }
}
