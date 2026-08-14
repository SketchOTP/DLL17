package com.animusmachinae.dll17.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Published vectors only. A key-derivation function that is merely
 * self-consistent is a key-derivation function that will one day disagree with
 * every other implementation of the same name, and the first symptom is
 * unrecoverable data.
 */
class KeyDerivationTest {

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    private fun unhex(s: String) = ByteArray(s.length / 2) {
        ((Character.digit(s[it * 2], 16) shl 4) or Character.digit(s[it * 2 + 1], 16)).toByte()
    }

    @Test
    fun `HMAC-SHA-256 matches RFC 4231 test case 1`() {
        assertEquals(
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
            hex(HmacSha256.mac(ByteArray(20) { 0x0b }, "Hi There".toByteArray())),
        )
    }

    @Test
    fun `HMAC-SHA-256 matches RFC 4231 test case 2`() {
        assertEquals(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
            hex(HmacSha256.mac("Jefe".toByteArray(), "what do ya want for nothing?".toByteArray())),
        )
    }

    @Test
    fun `HMAC-SHA-256 matches RFC 4231 test case 6, a key longer than the block`() {
        assertEquals(
            "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54",
            hex(
                HmacSha256.mac(
                    ByteArray(131) { 0xaa.toByte() },
                    "Test Using Larger Than Block-Size Key - Hash Key First".toByteArray(),
                ),
            ),
        )
    }

    @Test
    fun `HKDF matches RFC 5869 appendix A1`() {
        val ikm = ByteArray(22) { 0x0b }
        val salt = unhex("000102030405060708090a0b0c")
        val info = unhex("f0f1f2f3f4f5f6f7f8f9")
        val prk = Hkdf.extract(salt, ikm)
        assertEquals(
            "077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5",
            hex(prk),
        )
        assertEquals(
            "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf" +
                "34007208d5b887185865",
            hex(Hkdf.expand(prk, info, 42)),
        )
    }

    @Test
    fun `HKDF matches RFC 5869 appendix A3, empty salt and info`() {
        val prk = Hkdf.extract(ByteArray(0), ByteArray(22) { 0x0b })
        assertEquals(
            "19ef24a32c717b167f33a91d6f648bdf96596776afdb6377ac434c1c293ccb04",
            hex(prk),
        )
        assertEquals(
            "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d" +
                "9d201395faa4b61a96c8",
            hex(Hkdf.expand(prk, ByteArray(0), 42)),
        )
    }

    @Test
    fun `HKDF derive requires a purpose so two keys are never accidentally equal`() {
        val ikm = ByteArray(32) { it.toByte() }
        val a = Hkdf.derive(ikm, ByteArray(8), "PURPOSE-A")
        val b = Hkdf.derive(ikm, ByteArray(8), "PURPOSE-B")
        assertFalse(a.contentEquals(b))
        var rejected = false
        try {
            Hkdf.derive(ikm, ByteArray(8), "")
        } catch (expected: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected, "an empty info string must be refused")
    }

    @Test
    fun `PBKDF2-HMAC-SHA-256 matches the published vector`() {
        // RFC 7914 section 11, cross-checked against a second independent
        // implementation rather than against this one.
        assertEquals(
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc",
            hex(Pbkdf2HmacSha256.derive("passwd".toByteArray(), "salt".toByteArray(), 1, 32)),
        )
        assertEquals(
            "4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56",
            hex(Pbkdf2HmacSha256.derive("Password".toByteArray(), "NaCl".toByteArray(), 80000, 32)),
        )
    }

    @Test
    fun `constant time comparison rejects length and content differences`() {
        assertTrue(HmacSha256.constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 3)))
        assertFalse(HmacSha256.constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 4)))
        assertFalse(HmacSha256.constantTimeEquals(byteArrayOf(1, 2), byteArrayOf(1, 2, 3)))
    }
}
