package com.animusmachinae.dll17.core.recovery

import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecoveryCryptographyTest {

    private val organism = 0x0D11L
    private val root = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })
    private val lineage = CanonicalHash.ofEnvelope(CanonicalWriter(8).putI64(organism).toByteArray())

    private fun contents(count: Int = 5) = RecoveryContents(
        checkpointBytes = ByteArray(128) { (it * 3).toByte() },
        journalTail = (1..count).map { it.toLong() to ByteArray(48) { b -> (it * 7 + b).toByte() } },
    )

    private fun sealPackage(
        epoch: Int = 1,
        sequence: Long = 1L,
        body: RecoveryContents = contents(),
    ): ColdRecoveryPackage.Sealed = ColdRecoveryPackage.create(
        root = root,
        manifestTemplate = { bytes, hash ->
            RecoveryManifest(organism, epoch, sequence, 10L, 1_000L, 1, 1, 1, bytes, hash, lineage)
        },
        contents = body,
        organismId = organism,
    )

    // ------------------------------------------------------------ the secret

    @Test
    fun `the recovery secret round-trips through its encoding`() {
        val encoded = root.encode()
        assertEquals(root.bytes.toList(), RecoveryRoot.decode(encoded).bytes.toList())
        assertTrue(encoded.contains('-'), "the encoding is grouped for transcription")
    }

    @Test
    fun `a mistyped character is caught by the checksum`() {
        val encoded = root.encode().toCharArray()
        encoded[0] = if (encoded[0] == 'A') 'B' else 'A'
        val failure = assertFailsWith<RecoveryFailure> { RecoveryRoot.decode(String(encoded)) }
        assertEquals("RECOVERY_SECRET_CHECKSUM_FAILED", failure.state)
    }

    @Test
    fun `a truncated or over-long secret is rejected before the checksum`() {
        val encoded = root.encode()
        assertEquals(
            "RECOVERY_SECRET_MALFORMED",
            assertFailsWith<RecoveryFailure> { RecoveryRoot.decode(encoded.dropLast(2)) }.state,
        )
        assertEquals(
            "RECOVERY_SECRET_MALFORMED",
            assertFailsWith<RecoveryFailure> { RecoveryRoot.decode(encoded + "AA") }.state,
        )
    }

    @Test
    fun `decoding is case and separator insensitive`() {
        val encoded = root.encode()
        assertEquals(
            root.bytes.toList(),
            RecoveryRoot.decode(encoded.lowercase().replace("-", " ")).bytes.toList(),
        )
    }

    @Test
    fun `each derived key has one purpose and they are unrelated`() {
        val keys = listOf(
            root.packageKey(organism),
            root.manifestMacKey(organism),
            root.authorityProofKey(organism),
        )
        assertEquals(3, keys.map { it.toList() }.distinct().size)
        // And a different organism gets different keys from the same root.
        assertFalse(root.packageKey(organism).contentEquals(root.packageKey(organism + 1)))
    }

    @Test
    fun `the user disclosure says the phrase is not the history`() {
        assertTrue(RecoveryRoot.USER_DISCLOSURE.contains("does not"))
        assertTrue(RecoveryRoot.USER_DISCLOSURE.contains("memories"))
    }

    // ----------------------------------------------------------- the package

    @Test
    fun `a package round-trips its contents exactly`() {
        val sealed = sealPackage()
        val opened = ColdRecoveryPackage.open(root, sealed)
        assertEquals(5, opened.journalTail.size)
        assertTrue(opened.checkpointBytes.contentEquals(contents().checkpointBytes))
        opened.journalTail.forEachIndexed { index, (sequence, body) ->
            assertEquals(contents().journalTail[index].first, sequence)
            assertTrue(contents().journalTail[index].second.contentEquals(body))
        }
    }

    @Test
    fun `a package parses back from its canonical bytes`() {
        val sealed = sealPackage()
        val parsed = ColdRecoveryPackage.parse(sealed.canonicalBytes())
        assertEquals(sealed.manifest.packageSequence, parsed.manifest.packageSequence)
        assertEquals(5, ColdRecoveryPackage.open(root, parsed).journalTail.size)
    }

    @Test
    fun `the wrong recovery secret cannot open a package`() {
        val other = RecoveryRoot(ByteArray(32) { (it * 5 + 2).toByte() })
        assertEquals(
            "RECOVERY_PACKAGE_UNAUTHENTIC",
            assertFailsWith<RecoveryFailure> { ColdRecoveryPackage.open(other, sealPackage()) }.state,
        )
    }

    @Test
    fun `a flipped ciphertext bit is caught`() {
        val sealed = sealPackage()
        val damaged = ColdRecoveryPackage.Sealed(
            sealed.manifest,
            sealed.manifestMac,
            sealed.ciphertext.copyOf().also { it[10] = (it[10].toInt() xor 1).toByte() },
        )
        assertEquals(
            "RECOVERY_PACKAGE_CORRUPT",
            assertFailsWith<RecoveryFailure> { ColdRecoveryPackage.open(root, damaged) }.state,
        )
    }

    @Test
    fun `a rewritten manifest is caught by its MAC`() {
        val sealed = sealPackage()
        val forged = RecoveryManifest(
            organism, 99, sealed.manifest.packageSequence, sealed.manifest.checkpointSequence,
            sealed.manifest.lastProtectedLogicalTime, 1, 1, 1,
            sealed.manifest.ciphertextBytes, sealed.manifest.ciphertextHash, lineage,
        )
        assertEquals(
            "RECOVERY_PACKAGE_UNAUTHENTIC",
            assertFailsWith<RecoveryFailure> {
                ColdRecoveryPackage.open(
                    root, ColdRecoveryPackage.Sealed(forged, sealed.manifestMac, sealed.ciphertext),
                )
            }.state,
        )
    }

    @Test
    fun `a body from one package cannot be served under another package's manifest`() {
        // The manifest is the AEAD's associated data, so this is refused by the
        // cipher itself rather than by a comparison somebody could forget.
        val first = sealPackage(sequence = 1L, body = contents(3))
        val second = sealPackage(sequence = 2L, body = contents(7))
        val swapped = ColdRecoveryPackage.Sealed(
            first.manifest,
            first.manifestMac,
            second.ciphertext,
        )
        val failure = assertFailsWith<RecoveryFailure> { ColdRecoveryPackage.open(root, swapped) }
        assertTrue(failure.state.startsWith("RECOVERY_PACKAGE_"))
    }

    @Test
    fun `a malformed envelope is rejected without a decryption attempt`() {
        assertEquals(
            "RECOVERY_PACKAGE_MALFORMED",
            assertFailsWith<RecoveryFailure> {
                ColdRecoveryPackage.parse(ByteArray(32) { it.toByte() })
            }.state,
        )
    }

    @Test
    fun `the package checksum and size describe the actual bytes`() {
        val sealed = sealPackage()
        assertEquals(sealed.canonicalBytes().size.toLong(), sealed.sizeBytes)
        assertEquals(
            CanonicalHash.hex(CanonicalHash.ofEnvelope(sealed.canonicalBytes())),
            sealed.checksumHex(),
        )
    }
}
