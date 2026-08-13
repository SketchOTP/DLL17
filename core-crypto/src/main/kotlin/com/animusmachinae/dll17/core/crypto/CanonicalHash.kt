package com.animusmachinae.dll17.core.crypto

/**
 * Canonical state hash and canonical envelope, frozen by `DeterminismContractV1`
 * sections 6 and 10.
 */
public object CanonicalHash {
    public const val ALGORITHM_ID: String = "HASH_SHA256_V1"
    public const val DIGEST_SIZE: Int = Sha256.DIGEST_LENGTH

    /**
     * Domain separation tag. Hashing raw canonical bytes without a tag would let
     * a byte sequence that is valid in two different roles produce the same
     * digest in both; the tag makes the role part of the preimage.
     */
    public const val DOMAIN_TAG: String = "DLL17-STATE-HASH-V1"

    private val TAGGED_PREFIX: ByteArray =
        CanonicalWriter(32).putIdentifier(DOMAIN_TAG).toByteArray()

    /** Canonical state hash over a complete canonical envelope, header included. */
    public fun ofEnvelope(envelopeBytes: ByteArray): ByteArray {
        val preimage = ByteArray(TAGGED_PREFIX.size + envelopeBytes.size)
        TAGGED_PREFIX.copyInto(preimage, 0)
        envelopeBytes.copyInto(preimage, TAGGED_PREFIX.size)
        return Sha256.digest(preimage)
    }

    private const val HEX = "0123456789abcdef"

    /**
     * Lowercase hex. Written by hand rather than with `String.format`, which is
     * locale-sensitive: `%x` under a locale with non-ASCII digits does not
     * produce ASCII hex.
     */
    public fun hex(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            out.append(HEX[value ushr 4])
            out.append(HEX[value and 0x0F])
        }
        return out.toString()
    }
}

/** A decoded canonical envelope header plus its payload. */
public class CanonicalEnvelopeContents(
    public val envelopeFormatVersion: Int,
    public val determinismContractVersion: Int,
    public val payloadSchemaId: Int,
    public val payloadSchemaVersion: Int,
    public val payload: ByteArray,
)

/**
 * The single wrapper every canonical artifact is written inside.
 *
 * Refusing an unknown version is deliberate. A decoder that best-effort parses a
 * future artifact will silently produce a state that never existed, which is
 * worse than refusing to start.
 */
public object CanonicalEnvelope {
    public val MAGIC: ByteArray = byteArrayOf(
        'D'.code.toByte(), 'L'.code.toByte(), '1'.code.toByte(), '7'.code.toByte(),
    )
    public const val FORMAT_VERSION: Int = 1
    public const val CONTRACT_VERSION: Int = 1
    public const val HEADER_SIZE: Int = 22

    public fun wrap(payloadSchemaId: Int, payloadSchemaVersion: Int, payload: ByteArray): ByteArray =
        CanonicalWriter(HEADER_SIZE + payload.size)
            .putRawBytes(MAGIC)
            .putU16(FORMAT_VERSION)
            .putI32(CONTRACT_VERSION)
            .putI32(payloadSchemaId)
            .putI32(payloadSchemaVersion)
            .putU32(payload.size)
            .putRawBytes(payload)
            .toByteArray()

    public fun unwrap(bytes: ByteArray): CanonicalEnvelopeContents {
        if (bytes.size < HEADER_SIZE) {
            throw CanonicalCodecException("canonical envelope shorter than its header")
        }
        for (index in MAGIC.indices) {
            if (bytes[index] != MAGIC[index]) {
                throw CanonicalCodecException("not a canonical artifact: magic mismatch")
            }
        }
        val reader = CanonicalReader(bytes, MAGIC.size)
        val formatVersion = reader.readU16()
        if (formatVersion != FORMAT_VERSION) {
            throw CanonicalCodecException(
                "unsupported envelope format version $formatVersion; this build understands $FORMAT_VERSION",
            )
        }
        val contractVersion = reader.readI32()
        if (contractVersion != CONTRACT_VERSION) {
            throw CanonicalCodecException(
                "artifact was written under determinism contract version $contractVersion; " +
                    "this build implements $CONTRACT_VERSION and will not guess",
            )
        }
        val schemaId = reader.readI32()
        val schemaVersion = reader.readI32()
        val payloadLength = reader.readU32()
        if (reader.remaining != payloadLength) {
            throw CanonicalCodecException(
                "declared payload length $payloadLength does not match ${reader.remaining} remaining byte(s)",
            )
        }
        val payload = bytes.copyOfRange(reader.position, reader.position + payloadLength)
        return CanonicalEnvelopeContents(
            formatVersion,
            contractVersion,
            schemaId,
            schemaVersion,
            payload,
        )
    }
}
