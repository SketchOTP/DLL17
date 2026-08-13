package com.animusmachinae.dll17.core.crypto

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verification of the project's own SHA-256.
 *
 * `DeterminismContractV1` section 6 removes `MessageDigest` from the canonical
 * path because provider resolution differs across platforms. That trade buys
 * reproducibility at the cost of owning a cryptographic primitive, and the way
 * to pay that cost honestly is to check the implementation two independent ways:
 *
 * 1. against the vectors published in FIPS PUB 180-4, which were computed by
 *    somebody else long before this project existed;
 * 2. differentially against `MessageDigest` on whatever platform the test is
 *    running on — which is exactly the platform whose provider we refused to
 *    depend on, so agreement means we removed the dependency without changing
 *    the answer.
 *
 * A defect would have to be present identically in this implementation, in the
 * published vectors, and in the platform provider.
 */
class Sha256Test {

    private fun hex(bytes: ByteArray) = CanonicalHash.hex(bytes)

    @Test
    fun matchesThePublishedFips1804Vectors() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hex(Sha256.digest(ByteArray(0))),
            "empty string",
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hex(Sha256.digest("abc".toByteArray(Charsets.US_ASCII))),
            "abc",
        )
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hex(
                Sha256.digest(
                    ("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
                        .toByteArray(Charsets.US_ASCII),
                ),
            ),
            "448-bit multi-block vector",
        )
        assertEquals(
            "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            hex(Sha256.digest(ByteArray(1_000_000) { 'a'.code.toByte() })),
            "one million 'a' characters",
        )
    }

    @Test
    fun agreesWithThePlatformProviderOnEveryLengthAcrossTheBlockBoundary() {
        val platform = MessageDigest.getInstance("SHA-256")
        // Lengths 0..200 cover both padding cases: the length field fitting in
        // the final block, and the extra block that a 56-to-63 byte tail forces.
        for (length in 0..200) {
            val input = ByteArray(length) { index -> (index * 31 + 7).toByte() }
            platform.reset()
            assertEquals(
                hex(platform.digest(input)),
                hex(Sha256.digest(input)),
                "length $length",
            )
        }
    }

    @Test
    fun agreesWithThePlatformProviderOnLargeAndAdversarialInputs() {
        val platform = MessageDigest.getInstance("SHA-256")
        var state = 0x0123456789ABCDEFL
        for (trial in 0 until 64) {
            state = state * 6364136223846793005L + 1442695040888963407L
            val length = ((state ushr 33) % 5000L).toInt()
            val input = ByteArray(length) { index ->
                ((state ushr (index % 8 * 8)) and 0xFF).toByte()
            }
            platform.reset()
            assertEquals(hex(platform.digest(input)), hex(Sha256.digest(input)), "trial $trial")
        }
    }

    @Test
    fun isPureAcrossRepeatedCalls() {
        val input = "determinism".toByteArray(Charsets.US_ASCII)
        val first = hex(Sha256.digest(input))
        repeat(100) { assertEquals(first, hex(Sha256.digest(input))) }
    }

    @Test
    fun canonicalStateHashIsDomainSeparated() {
        val payload = byteArrayOf(1, 2, 3)
        val envelope = CanonicalEnvelope.wrap(1, 1, payload)
        val tagged = CanonicalHash.ofEnvelope(envelope)
        val untagged = Sha256.digest(envelope)
        assertTrue(
            !tagged.contentEquals(untagged),
            "the canonical state hash must not be a bare digest of the envelope",
        )
        assertEquals(CanonicalHash.DIGEST_SIZE, tagged.size)
    }

    @Test
    fun hexIsLowercaseAsciiIndependentOfLocale() {
        val previous = java.util.Locale.getDefault()
        try {
            // Locales with non-ASCII digit shaping are exactly why String.format
            // is banned in this codebase.
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ar-EG-u-nu-arab"))
            assertEquals("00ff7f80", CanonicalHash.hex(byteArrayOf(0, -1, 127, -128)))
        } finally {
            java.util.Locale.setDefault(previous)
        }
    }
}
