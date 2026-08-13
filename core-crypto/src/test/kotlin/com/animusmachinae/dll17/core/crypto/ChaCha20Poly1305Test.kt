package com.animusmachinae.dll17.core.crypto

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * `AEAD_CHACHA20_POLY1305_V1` against RFC 8439's published vectors, and
 * differentially against whatever provider the running platform happens to
 * offer.
 *
 * The published vectors are the primary evidence: they prove the implementation
 * is right rather than merely self-consistent. The differential check is
 * secondary and deliberately tolerant of the provider being absent, because the
 * entire reason this primitive is owned in-project is that provider availability
 * cannot be assumed.
 */
class ChaCha20Poly1305Test {

    private fun hex(spec: String): ByteArray {
        val cleaned = spec.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        require(cleaned.length % 2 == 0) { "odd hex length" }
        return ByteArray(cleaned.length / 2) { index ->
            cleaned.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun toHex(bytes: ByteArray): String =
        bytes.joinToString("") { byte ->
            val value = byte.toInt() and 0xFF
            "0123456789abcdef"[value ushr 4].toString() + "0123456789abcdef"[value and 15]
        }

    @Test
    fun `chacha20 block function matches RFC 8439 section 2_3_2`() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = hex("000000090000004a00000000")
        val produced = ChaCha20Poly1305.block(key, nonce, 1)

        val expected = hex(
            """
            10 f1 e7 e4 d1 3b 59 15 50 0f dd 1f a3 20 71 c4
            c7 d1 f4 c7 33 c0 68 03 04 22 aa 9a c3 d4 6c 4e
            d2 82 64 46 07 9f aa 09 14 c2 d7 05 d9 8b 02 a2
            b5 12 9c d1 de 16 4e b9 cb d0 83 e8 a2 50 3c 4e
            """.trimIndent(),
        )
        assertContentEquals(expected, produced, "ChaCha20 block function diverged from RFC 8439")
    }

    @Test
    fun `aead matches RFC 8439 section 2_8_2`() {
        val plaintext = (
            "Ladies and Gentlemen of the class of '99: If I could offer you " +
                "only one tip for the future, sunscreen would be it."
            ).toByteArray(Charsets.US_ASCII)
        val aad = hex("50515253c0c1c2c3c4c5c6c7")
        val key = ByteArray(32) { (0x80 + it).toByte() }
        val nonce = hex("070000004041424344454647")

        val expectedCiphertext = hex(
            """
            d31a8d34 648e60db 7b86afbc 53ef7ec2 a4aded51 296e08fe a9e2b5a7 36ee62d6
            3dbea45e 8ca96712 82fafb69 da92728b 1a71de0a 9e060b29 05d6a5b6 7ecd3b36
            92ddbd7f 2d778b8c 9803aee3 28091b58 fab324e4 fad67594 5585808b 4831d7bc
            3ff4def0 8e4b7a9d e576d265 86cec64b 6116
            """.trimIndent(),
        )
        val expectedTag = hex("1ae10b594f09e26a7e902ecbd0600691")

        val sealed = ChaCha20Poly1305.seal(key, nonce, aad, plaintext)
        assertEquals(
            toHex(expectedCiphertext + expectedTag),
            toHex(sealed),
            "AEAD output diverged from the RFC 8439 vector",
        )
        assertContentEquals(plaintext, ChaCha20Poly1305.open(key, nonce, aad, sealed))
    }

    @Test
    fun `empty plaintext and empty aad still authenticate`() {
        val key = ByteArray(32) { (it * 7 + 1).toByte() }
        val nonce = ByteArray(12) { (it * 3).toByte() }
        val sealed = ChaCha20Poly1305.seal(key, nonce, ByteArray(0), ByteArray(0))
        assertEquals(ChaCha20Poly1305.TAG_SIZE, sealed.size)
        assertEquals(0, ChaCha20Poly1305.open(key, nonce, ByteArray(0), sealed).size)
    }

    @Test
    fun `every single-bit change anywhere fails authentication`() {
        val key = ByteArray(32) { (it + 3).toByte() }
        val nonce = ByteArray(12) { (it + 11).toByte() }
        val aad = "generation=7 sequence=42".toByteArray(Charsets.US_ASCII)
        val plaintext = ByteArray(37) { (it * 5).toByte() }
        val sealed = ChaCha20Poly1305.seal(key, nonce, aad, plaintext)

        for (index in sealed.indices) {
            for (bit in 0 until 8) {
                val corrupted = sealed.copyOf()
                corrupted[index] = (corrupted[index].toInt() xor (1 shl bit)).toByte()
                assertFailsWith<ChaCha20Poly1305.AuthenticationFailure>(
                    "corrupting byte $index bit $bit was accepted",
                ) {
                    ChaCha20Poly1305.open(key, nonce, aad, corrupted)
                }
            }
        }
    }

    @Test
    fun `changed associated data fails authentication`() {
        val key = ByteArray(32) { (it + 9).toByte() }
        val nonce = ByteArray(12) { it.toByte() }
        val plaintext = "durable record".toByteArray(Charsets.US_ASCII)
        val sealed = ChaCha20Poly1305.seal(key, nonce, "sequence=1".toByteArray(), plaintext)

        // This is the record-relocation case the contract's AAD binding exists
        // to detect: same key, same nonce, same bytes, different position.
        assertFailsWith<ChaCha20Poly1305.AuthenticationFailure> {
            ChaCha20Poly1305.open(key, nonce, "sequence=2".toByteArray(), sealed)
        }
    }

    @Test
    fun `truncated record fails rather than returning a partial result`() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { it.toByte() }
        val sealed = ChaCha20Poly1305.seal(key, nonce, ByteArray(0), ByteArray(64) { it.toByte() })
        for (length in 0 until sealed.size) {
            assertFailsWith<ChaCha20Poly1305.AuthenticationFailure> {
                ChaCha20Poly1305.open(key, nonce, ByteArray(0), sealed.copyOf(length))
            }
        }
    }

    @Test
    fun `round trips across every length spanning multiple keystream blocks`() {
        val random = Random(seed = 20260813)
        val key = ByteArray(32) { (it * 11).toByte() }
        val nonce = ByteArray(12) { (it * 13).toByte() }
        // 0 through 200 crosses the 64-byte block boundary three times and covers
        // every Poly1305 final-block remainder.
        for (length in 0..200) {
            val plaintext = random.nextBytes(length)
            val aad = random.nextBytes(length % 17)
            val sealed = ChaCha20Poly1305.seal(key, nonce, aad, plaintext)
            assertEquals(length + ChaCha20Poly1305.TAG_SIZE, sealed.size)
            assertContentEquals(plaintext, ChaCha20Poly1305.open(key, nonce, aad, sealed))
        }
    }

    @Test
    fun `agrees with the platform provider when one offers the algorithm`() {
        val provider = try {
            Cipher.getInstance("ChaCha20-Poly1305")
        } catch (unavailable: Exception) {
            // Deliberately not a failure. The reason this primitive is owned in
            // project is precisely that provider availability is not a constant.
            println("platform provides no ChaCha20-Poly1305; differential check skipped")
            return
        }

        val random = Random(seed = 991)
        var compared = 0
        for (trial in 0 until 32) {
            val key = random.nextBytes(32)
            val nonce = random.nextBytes(12)
            val aad = random.nextBytes(random.nextInt(0, 40))
            val plaintext = random.nextBytes(random.nextInt(0, 300))

            provider.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key, "ChaCha20"),
                IvParameterSpec(nonce),
            )
            provider.updateAAD(aad)
            val theirs = provider.doFinal(plaintext)
            val ours = ChaCha20Poly1305.seal(key, nonce, aad, plaintext)
            assertContentEquals(theirs, ours, "provider disagreed on trial $trial")
            compared += 1
        }
        assertTrue(compared > 0)
    }
}
