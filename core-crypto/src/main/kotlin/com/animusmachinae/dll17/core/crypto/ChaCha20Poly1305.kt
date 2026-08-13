package com.animusmachinae.dll17.core.crypto

/**
 * `AEAD_CHACHA20_POLY1305_V1` — the authenticated encryption primitive frozen by
 * `ContinuityDurabilityContractV1` section 13.2, implemented from RFC 8439.
 *
 * **Why this is implemented here rather than taken from `javax.crypto`.** The
 * reasoning is identical to [Sha256]: the algorithm is standardized, but *which
 * implementation runs* is not. Provider resolution differs between a desktop JDK
 * and Android, moves as Conscrypt evolves, and can be reordered at runtime. An
 * encrypted durable record that a device can write but a later build of the same
 * app cannot read is a data-loss bug that no amount of testing on one image will
 * surface.
 *
 * ChaCha20-Poly1305 rather than AES-GCM because it is add-rotate-xor only. There
 * are no S-boxes, no generated tables, and no hardware-acceleration path whose
 * absence changes behaviour — so one implementation is correct on every target,
 * including devices with no AES acceleration at all.
 *
 * The cost of owning it is paid by verifying against the RFC 8439 published
 * vectors *and* differentially against `javax.crypto` wherever that provider
 * happens to offer the algorithm.
 *
 * This primitive sits **below** the canonical byte layer. Canonical hashing,
 * replay and cross-target determinism operate on plaintext canonical bytes and
 * are unaffected by it.
 */
public object ChaCha20Poly1305 {

    public const val ALGORITHM_ID: String = "AEAD_CHACHA20_POLY1305_V1"
    public const val KEY_SIZE: Int = 32
    public const val NONCE_SIZE: Int = 12
    public const val TAG_SIZE: Int = 16

    /** Raised when a record fails authentication. Never carries plaintext. */
    public class AuthenticationFailure(message: String) : RuntimeException(message)

    /**
     * Seals [plaintext], returning `ciphertext || tag`.
     *
     * The caller owns nonce uniqueness. The contract derives the nonce from the
     * durable record sequence and key epoch precisely so that this method never
     * needs a randomness source.
     */
    public fun seal(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        requireSizes(key, nonce)
        val oneTimeKey = block(key, nonce, 0).copyOf(32)
        val ciphertext = xorKeyStream(key, nonce, 1, plaintext)
        val tag = Poly1305.mac(oneTimeKey, macData(aad, ciphertext))
        return ciphertext + tag
    }

    /**
     * Opens `ciphertext || tag`, or throws [AuthenticationFailure].
     *
     * Failure is always an exception and never a partial or best-effort result.
     * The contract maps it to `STORAGE_FAULT`: a record that does not
     * authenticate is not a record that can be partially trusted.
     */
    public fun open(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        sealed: ByteArray,
    ): ByteArray {
        requireSizes(key, nonce)
        if (sealed.size < TAG_SIZE) {
            throw AuthenticationFailure("sealed record is shorter than its authentication tag")
        }
        val ciphertext = sealed.copyOfRange(0, sealed.size - TAG_SIZE)
        val presentedTag = sealed.copyOfRange(sealed.size - TAG_SIZE, sealed.size)
        val oneTimeKey = block(key, nonce, 0).copyOf(32)
        val expectedTag = Poly1305.mac(oneTimeKey, macData(aad, ciphertext))
        if (!constantTimeEquals(expectedTag, presentedTag)) {
            throw AuthenticationFailure("authentication tag mismatch")
        }
        return xorKeyStream(key, nonce, 1, ciphertext)
    }

    private fun requireSizes(key: ByteArray, nonce: ByteArray) {
        if (key.size != KEY_SIZE) {
            throw IllegalArgumentException("key must be $KEY_SIZE bytes, got ${key.size}")
        }
        if (nonce.size != NONCE_SIZE) {
            throw IllegalArgumentException("nonce must be $NONCE_SIZE bytes, got ${nonce.size}")
        }
    }

    /**
     * RFC 8439 section 2.8: `AAD || pad16 || ciphertext || pad16 || lengths`.
     *
     * The padding and the trailing lengths are what stop an attacker from moving
     * bytes between the associated data and the ciphertext without detection.
     */
    private fun macData(aad: ByteArray, ciphertext: ByteArray): ByteArray {
        val aadPad = (16 - (aad.size % 16)) % 16
        val ctPad = (16 - (ciphertext.size % 16)) % 16
        val out = ByteArray(aad.size + aadPad + ciphertext.size + ctPad + 16)
        var at = 0
        aad.copyInto(out, at); at += aad.size + aadPad
        ciphertext.copyInto(out, at); at += ciphertext.size + ctPad
        writeLongLE(out, at, aad.size.toLong()); at += 8
        writeLongLE(out, at, ciphertext.size.toLong())
        return out
    }

    private fun writeLongLE(out: ByteArray, offset: Int, value: Long) {
        for (i in 0 until 8) {
            out[offset + i] = ((value ushr (8 * i)) and 0xFF).toByte()
        }
    }

    /** Comparison whose duration does not depend on where the first difference is. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var difference = 0
        for (i in a.indices) difference = difference or (a[i].toInt() xor b[i].toInt())
        return difference == 0
    }

    // ------------------------------------------------------------------ ChaCha20

    private const val ROUNDS = 20

    /** RFC 8439 section 2.3: one 64-byte keystream block. */
    internal fun block(key: ByteArray, nonce: ByteArray, counter: Int): ByteArray {
        val state = IntArray(16)
        // "expand 32-byte k", as four little-endian constants.
        state[0] = 0x61707865
        state[1] = 0x3320646E
        state[2] = 0x79622D32
        state[3] = 0x6B206574
        for (i in 0 until 8) state[4 + i] = readIntLE(key, i * 4)
        state[12] = counter
        for (i in 0 until 3) state[13 + i] = readIntLE(nonce, i * 4)

        val working = state.copyOf()
        repeat(ROUNDS / 2) {
            // Column rounds.
            quarterRound(working, 0, 4, 8, 12)
            quarterRound(working, 1, 5, 9, 13)
            quarterRound(working, 2, 6, 10, 14)
            quarterRound(working, 3, 7, 11, 15)
            // Diagonal rounds.
            quarterRound(working, 0, 5, 10, 15)
            quarterRound(working, 1, 6, 11, 12)
            quarterRound(working, 2, 7, 8, 13)
            quarterRound(working, 3, 4, 9, 14)
        }

        val out = ByteArray(64)
        for (i in 0 until 16) {
            writeIntLE(out, i * 4, working[i] + state[i])
        }
        return out
    }

    private fun quarterRound(s: IntArray, a: Int, b: Int, c: Int, d: Int) {
        s[a] += s[b]; s[d] = Integer.rotateLeft(s[d] xor s[a], 16)
        s[c] += s[d]; s[b] = Integer.rotateLeft(s[b] xor s[c], 12)
        s[a] += s[b]; s[d] = Integer.rotateLeft(s[d] xor s[a], 8)
        s[c] += s[d]; s[b] = Integer.rotateLeft(s[b] xor s[c], 7)
    }

    private fun xorKeyStream(
        key: ByteArray,
        nonce: ByteArray,
        initialCounter: Int,
        input: ByteArray,
    ): ByteArray {
        val out = ByteArray(input.size)
        var offset = 0
        var counter = initialCounter
        while (offset < input.size) {
            val keyStream = block(key, nonce, counter)
            val span = minOf(64, input.size - offset)
            for (i in 0 until span) {
                out[offset + i] = (input[offset + i].toInt() xor keyStream[i].toInt()).toByte()
            }
            offset += span
            counter += 1
        }
        return out
    }

    private fun readIntLE(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun writeIntLE(out: ByteArray, offset: Int, value: Int) {
        out[offset] = (value and 0xFF).toByte()
        out[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        out[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        out[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    // ----------------------------------------------------------------- Poly1305

    /**
     * RFC 8439 section 2.5, in the five-limb 26-bit representation.
     *
     * The limb split exists because the accumulator is a 130-bit number and the
     * JVM's widest integer is 64 bits. Five 26-bit limbs keep every intermediate
     * product below 2^63, so the whole MAC runs in ordinary signed `Long`
     * arithmetic with no overflow and no `BigInteger`.
     */
    private object Poly1305 {

        private const val LIMB_MASK = 0x3FFFFFFL

        fun mac(oneTimeKey: ByteArray, message: ByteArray): ByteArray {
            // r is clamped exactly as the RFC requires; the cleared bits are what
            // keep the 26-bit limb products inside 64 bits.
            val t0 = readIntLE(oneTimeKey, 0).toLong() and 0xFFFFFFFFL
            val t1 = readIntLE(oneTimeKey, 4).toLong() and 0xFFFFFFFFL
            val t2 = readIntLE(oneTimeKey, 8).toLong() and 0xFFFFFFFFL
            val t3 = readIntLE(oneTimeKey, 12).toLong() and 0xFFFFFFFFL

            val r0 = t0 and 0x3FFFFFFL
            val r1 = ((t0 ushr 26) or (t1 shl 6)) and 0x3FFFF03L
            val r2 = ((t1 ushr 20) or (t2 shl 12)) and 0x3FFC0FFL
            val r3 = ((t2 ushr 14) or (t3 shl 18)) and 0x3F03FFFL
            val r4 = (t3 ushr 8) and 0x00FFFFFL

            val s1 = r1 * 5L
            val s2 = r2 * 5L
            val s3 = r3 * 5L
            val s4 = r4 * 5L

            var h0 = 0L
            var h1 = 0L
            var h2 = 0L
            var h3 = 0L
            var h4 = 0L

            var offset = 0
            while (offset < message.size) {
                val span = minOf(16, message.size - offset)
                // A short final block is padded with an explicit 0x01 byte rather
                // than the implicit 2^128 bit, which is what makes a truncated
                // message impossible to confuse with a shorter genuine one.
                val chunk = ByteArray(17)
                message.copyInto(chunk, 0, offset, offset + span)
                chunk[span] = 1

                val c0 = readIntLE(chunk, 0).toLong() and 0xFFFFFFFFL
                val c1 = readIntLE(chunk, 4).toLong() and 0xFFFFFFFFL
                val c2 = readIntLE(chunk, 8).toLong() and 0xFFFFFFFFL
                val c3 = readIntLE(chunk, 12).toLong() and 0xFFFFFFFFL
                val c4 = chunk[16].toLong() and 0xFFL

                h0 += c0 and 0x3FFFFFFL
                h1 += ((c0 ushr 26) or (c1 shl 6)) and 0x3FFFFFFL
                h2 += ((c1 ushr 20) or (c2 shl 12)) and 0x3FFFFFFL
                h3 += ((c2 ushr 14) or (c3 shl 18)) and 0x3FFFFFFL
                h4 += (c3 ushr 8) or (c4 shl 24)

                var d0 = h0 * r0 + h1 * s4 + h2 * s3 + h3 * s2 + h4 * s1
                var d1 = h0 * r1 + h1 * r0 + h2 * s4 + h3 * s3 + h4 * s2
                var d2 = h0 * r2 + h1 * r1 + h2 * r0 + h3 * s4 + h4 * s3
                var d3 = h0 * r3 + h1 * r2 + h2 * r1 + h3 * r0 + h4 * s4
                var d4 = h0 * r4 + h1 * r3 + h2 * r2 + h3 * r1 + h4 * r0

                var carry = d0 ushr 26; h0 = d0 and LIMB_MASK
                d1 += carry; carry = d1 ushr 26; h1 = d1 and LIMB_MASK
                d2 += carry; carry = d2 ushr 26; h2 = d2 and LIMB_MASK
                d3 += carry; carry = d3 ushr 26; h3 = d3 and LIMB_MASK
                d4 += carry; carry = d4 ushr 26; h4 = d4 and LIMB_MASK
                // Reduction modulo 2^130 - 5: the bits above 130 fold back in
                // multiplied by 5.
                h0 += carry * 5L; carry = h0 ushr 26; h0 = h0 and LIMB_MASK
                h1 += carry

                offset += span
            }

            // Final carry propagation.
            var carry = h1 ushr 26; h1 = h1 and LIMB_MASK
            h2 += carry; carry = h2 ushr 26; h2 = h2 and LIMB_MASK
            h3 += carry; carry = h3 ushr 26; h3 = h3 and LIMB_MASK
            h4 += carry; carry = h4 ushr 26; h4 = h4 and LIMB_MASK
            h0 += carry * 5L; carry = h0 ushr 26; h0 = h0 and LIMB_MASK
            h1 += carry

            // h + 5, to decide whether a final subtraction of 2^130 - 5 applies.
            var g0 = h0 + 5L; carry = g0 ushr 26; g0 = g0 and LIMB_MASK
            var g1 = h1 + carry; carry = g1 ushr 26; g1 = g1 and LIMB_MASK
            var g2 = h2 + carry; carry = g2 ushr 26; g2 = g2 and LIMB_MASK
            var g3 = h3 + carry; carry = g3 ushr 26; g3 = g3 and LIMB_MASK
            val g4 = h4 + carry - (1L shl 26)

            // Select without branching: mask is all-ones when g underflowed, so
            // the timing of the MAC does not depend on the accumulator's value.
            val mask = g4 shr 63
            val inverse = mask.inv()
            h0 = (h0 and mask) or (g0 and inverse)
            h1 = (h1 and mask) or (g1 and inverse)
            h2 = (h2 and mask) or (g2 and inverse)
            h3 = (h3 and mask) or (g3 and inverse)
            h4 = (h4 and mask) or (g4 and inverse)

            // Back to four 32-bit words, then add the second half of the key.
            val f0 = ((h0) or (h1 shl 26)) and 0xFFFFFFFFL
            val f1 = ((h1 ushr 6) or (h2 shl 20)) and 0xFFFFFFFFL
            val f2 = ((h2 ushr 12) or (h3 shl 14)) and 0xFFFFFFFFL
            val f3 = ((h3 ushr 18) or (h4 shl 8)) and 0xFFFFFFFFL

            val out = ByteArray(16)
            var accumulator = f0 + (readIntLE(oneTimeKey, 16).toLong() and 0xFFFFFFFFL)
            writeIntLE(out, 0, accumulator.toInt())
            accumulator = f1 + (readIntLE(oneTimeKey, 20).toLong() and 0xFFFFFFFFL) + (accumulator ushr 32)
            writeIntLE(out, 4, accumulator.toInt())
            accumulator = f2 + (readIntLE(oneTimeKey, 24).toLong() and 0xFFFFFFFFL) + (accumulator ushr 32)
            writeIntLE(out, 8, accumulator.toInt())
            accumulator = f3 + (readIntLE(oneTimeKey, 28).toLong() and 0xFFFFFFFFL) + (accumulator ushr 32)
            writeIntLE(out, 12, accumulator.toInt())
            return out
        }
    }
}
