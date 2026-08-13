package com.animusmachinae.dll17.core.crypto

/**
 * SHA-256 (FIPS PUB 180-4), implemented in this project rather than obtained from
 * `java.security.MessageDigest`.
 *
 * `DeterminismContractV1` section 6 explains why. In short: SHA-256's *output* is
 * standardized, but *which implementation runs* is not. `MessageDigest` resolves
 * through the installed provider list, which differs between a desktop JDK and
 * Android, moves as Conscrypt evolves, and can be reordered at runtime. Canonical
 * organism identity must not depend on any of that.
 *
 * The obvious risk of a hand-written digest is that it is subtly wrong. That is
 * answered by differential testing, not by confidence: the test suite checks this
 * implementation against the published FIPS 180-4 vectors and, separately,
 * against `MessageDigest` on every qualification target. A defect would have to
 * exist identically in both.
 *
 * No state is shared between calls; [digest] is pure.
 */
public object Sha256 {

    public const val DIGEST_LENGTH: Int = 32
    private const val BLOCK_LENGTH: Int = 64

    private val K = intArrayOf(
        0x428a2f98.toInt(), 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
        0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
        0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
        0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
        0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
        0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
        0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
        0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt(),
    )

    private val INITIAL_STATE = intArrayOf(
        0x6a09e667, 0xbb67ae85.toInt(), 0x3c6ef372, 0xa54ff53a.toInt(),
        0x510e527f, 0x9b05688c.toInt(), 0x1f83d9ab, 0x5be0cd19,
    )

    public fun digest(input: ByteArray): ByteArray {
        val h = INITIAL_STATE.copyOf()

        // Padding: 0x80, then zeros, then the 64-bit big-endian bit length.
        val bitLength = input.size.toLong() * 8L
        val paddedLength = ((input.size + 9 + BLOCK_LENGTH - 1) / BLOCK_LENGTH) * BLOCK_LENGTH
        val message = ByteArray(paddedLength)
        input.copyInto(message, 0)
        message[input.size] = 0x80.toByte()
        var shift = 56
        var at = paddedLength - 8
        while (shift >= 0) {
            message[at++] = (bitLength ushr shift).toByte()
            shift -= 8
        }

        val w = IntArray(64)
        var blockStart = 0
        while (blockStart < paddedLength) {
            for (index in 0 until 16) {
                val base = blockStart + index * 4
                w[index] = ((message[base].toInt() and 0xFF) shl 24) or
                    ((message[base + 1].toInt() and 0xFF) shl 16) or
                    ((message[base + 2].toInt() and 0xFF) shl 8) or
                    (message[base + 3].toInt() and 0xFF)
            }
            for (index in 16 until 64) {
                val a = w[index - 15]
                val b = w[index - 2]
                val s0 = rotateRight(a, 7) xor rotateRight(a, 18) xor (a ushr 3)
                val s1 = rotateRight(b, 17) xor rotateRight(b, 19) xor (b ushr 10)
                w[index] = w[index - 16] + s0 + w[index - 7] + s1
            }

            var a = h[0]
            var b = h[1]
            var c = h[2]
            var d = h[3]
            var e = h[4]
            var f = h[5]
            var g = h[6]
            var hh = h[7]

            for (index in 0 until 64) {
                val s1 = rotateRight(e, 6) xor rotateRight(e, 11) xor rotateRight(e, 25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = hh + s1 + ch + K[index] + w[index]
                val s0 = rotateRight(a, 2) xor rotateRight(a, 13) xor rotateRight(a, 22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj

                hh = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            h[0] += a
            h[1] += b
            h[2] += c
            h[3] += d
            h[4] += e
            h[5] += f
            h[6] += g
            h[7] += hh

            blockStart += BLOCK_LENGTH
        }

        val out = ByteArray(DIGEST_LENGTH)
        for (index in 0 until 8) {
            val value = h[index]
            out[index * 4] = (value ushr 24).toByte()
            out[index * 4 + 1] = (value ushr 16).toByte()
            out[index * 4 + 2] = (value ushr 8).toByte()
            out[index * 4 + 3] = value.toByte()
        }
        return out
    }

    private fun rotateRight(value: Int, bits: Int): Int =
        (value ushr bits) or (value shl (32 - bits))
}
