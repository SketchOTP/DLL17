package com.animusmachinae.dll17.core.crypto

/**
 * HMAC-SHA-256, HKDF and PBKDF2, implemented in project for the same reason
 * SHA-256 and ChaCha20-Poly1305 are (IMPL-0008, IMPL-0018): the algorithms are
 * standardized but which provider implementation runs is not, and it differs
 * between a desktop JDK and Android and moves as Conscrypt evolves.
 *
 * A key that a device can derive today and cannot derive after an OS update is
 * unrecoverable data loss, and the recovery path is exactly where that must
 * never happen.
 *
 * Verified against the published test vectors: RFC 4231 for HMAC-SHA-256,
 * RFC 5869 appendix A for HKDF, RFC 6070 / RFC 7914 for PBKDF2.
 */
public object HmacSha256 {

    public const val ALGORITHM_ID: String = "HMAC_SHA256_V1"
    public const val BLOCK_SIZE: Int = 64
    public const val DIGEST_SIZE: Int = Sha256.DIGEST_LENGTH

    private const val IPAD: Byte = 0x36
    private const val OPAD: Byte = 0x5c

    public fun mac(key: ByteArray, message: ByteArray): ByteArray {
        val block = ByteArray(BLOCK_SIZE)
        // A key longer than the block is hashed first; a shorter one is zero
        // padded. Both are the standard construction and both matter: skipping
        // the long-key case silently changes the MAC for long keys only.
        val normalized = if (key.size > BLOCK_SIZE) Sha256.digest(key) else key
        normalized.copyInto(block)

        val inner = ByteArray(BLOCK_SIZE)
        val outer = ByteArray(BLOCK_SIZE)
        for (i in 0 until BLOCK_SIZE) {
            inner[i] = (block[i].toInt() xor IPAD.toInt()).toByte()
            outer[i] = (block[i].toInt() xor OPAD.toInt()).toByte()
        }

        val innerDigest = Sha256.digest(inner + message)
        return Sha256.digest(outer + innerDigest)
    }

    /** Constant-time comparison. Never use `contentEquals` on a MAC. */
    public fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var difference = 0
        for (i in a.indices) difference = difference or (a[i].toInt() xor b[i].toInt())
        return difference == 0
    }
}

/** HKDF-SHA-256, RFC 5869. */
public object Hkdf {

    public const val ALGORITHM_ID: String = "HKDF_SHA256_V1"

    public fun extract(salt: ByteArray, inputKeyMaterial: ByteArray): ByteArray =
        HmacSha256.mac(if (salt.isEmpty()) ByteArray(HmacSha256.DIGEST_SIZE) else salt, inputKeyMaterial)

    public fun expand(pseudoRandomKey: ByteArray, info: ByteArray, length: Int): ByteArray {
        val blocks = (length + HmacSha256.DIGEST_SIZE - 1) / HmacSha256.DIGEST_SIZE
        if (blocks > 255) throw IllegalArgumentException("HKDF cannot expand to $length bytes")
        val output = ByteArray(length)
        var previous = ByteArray(0)
        var written = 0
        for (counter in 1..blocks) {
            previous = HmacSha256.mac(pseudoRandomKey, previous + info + byteArrayOf(counter.toByte()))
            val take = minOf(previous.size, length - written)
            previous.copyInto(output, written, 0, take)
            written += take
        }
        return output
    }

    /**
     * The whole HKDF, with the domain separation this project requires.
     *
     * `info` is mandatory rather than optional. Two keys derived from the same
     * root for different purposes must be unrelated, and the only thing making
     * them unrelated is that string.
     */
    public fun derive(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: String,
        length: Int = 32,
    ): ByteArray {
        if (info.isEmpty()) throw IllegalArgumentException("HKDF info must name the purpose")
        return expand(extract(salt, inputKeyMaterial), info.toByteArray(Charsets.US_ASCII), length)
    }
}

/**
 * PBKDF2-HMAC-SHA-256, RFC 8018.
 *
 * Used only where the input is a user-transcribed secret. Where the input is
 * already a full-entropy key, HKDF is used instead: iterating a hash over a
 * 256-bit random root buys nothing and costs the user a wait on every recovery.
 */
public object Pbkdf2HmacSha256 {

    public const val ALGORITHM_ID: String = "PBKDF2_HMAC_SHA256_V1"

    public fun derive(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        length: Int,
    ): ByteArray {
        if (iterations < 1) throw IllegalArgumentException("iterations must be positive")
        val blocks = (length + HmacSha256.DIGEST_SIZE - 1) / HmacSha256.DIGEST_SIZE
        val output = ByteArray(length)
        var written = 0
        for (block in 1..blocks) {
            val indexed = salt + byteArrayOf(
                (block ushr 24).toByte(),
                (block ushr 16).toByte(),
                (block ushr 8).toByte(),
                block.toByte(),
            )
            var u = HmacSha256.mac(password, indexed)
            val accumulator = u.copyOf()
            for (iteration in 2..iterations) {
                u = HmacSha256.mac(password, u)
                for (i in accumulator.indices) {
                    accumulator[i] = (accumulator[i].toInt() xor u[i].toInt()).toByte()
                }
            }
            val take = minOf(accumulator.size, length - written)
            accumulator.copyInto(output, written, 0, take)
            written += take
        }
        return output
    }
}
