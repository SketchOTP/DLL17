package com.animusmachinae.dll17.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Canonical codec tests, per `DeterminismContractV1` sections 1 to 5 and 10.
 *
 * These mostly test refusals. A codec that accepts two byte sequences for the
 * same value, or one byte sequence for two different values, defeats every other
 * determinism precaution in the project, so the interesting assertions are about
 * what the codec will not do.
 */
class CanonicalEncodingTest {

    @Test
    fun integersAreBigEndian() {
        assertEquals(
            listOf(0x01, 0x02, 0x03, 0x04),
            CanonicalWriter().putI32(0x01020304).toByteArray().map { it.toInt() and 0xFF },
        )
        assertEquals(
            listOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08),
            CanonicalWriter().putI64(0x0102030405060708L).toByteArray().map { it.toInt() and 0xFF },
        )
    }

    @Test
    fun lengthPrefixesAreFixedWidthWithExactlyOneEncodingPerValue() {
        val encoded = CanonicalWriter().putBytes(byteArrayOf(9)).toByteArray()
        assertEquals(5, encoded.size, "4-byte prefix plus one payload byte")
        assertEquals(
            listOf(0x00, 0x00, 0x00, 0x01, 0x09),
            encoded.map { it.toInt() and 0xFF },
        )
    }

    @Test
    fun booleansAcceptOnlyZeroAndOne() {
        assertTrue(CanonicalReader(byteArrayOf(1)).readBool())
        assertTrue(!CanonicalReader(byteArrayOf(0)).readBool())
        assertFailsWith<CanonicalCodecException> { CanonicalReader(byteArrayOf(2)).readBool() }
        assertFailsWith<CanonicalCodecException> { CanonicalReader(byteArrayOf(-1)).readBool() }
    }

    @Test
    fun identifiersAreRestrictedToTheAsciiSubset() {
        assertTrue(CanonicalIdentifier.isLegal("Domain-1_v.2"))
        assertTrue(!CanonicalIdentifier.isLegal(""))
        assertTrue(!CanonicalIdentifier.isLegal("a".repeat(65)))
        // Everything below is a real cross-platform hazard, not a stylistic rule.
        assertTrue(!CanonicalIdentifier.isLegal("café"), "non-ASCII invites normalization drift")
        assertTrue(!CanonicalIdentifier.isLegal("a b"), "whitespace")
        assertTrue(!CanonicalIdentifier.isLegal("emoji😀"), "astral plane")
        assertFailsWith<CanonicalCodecException> { CanonicalWriter().putIdentifier("bad/char") }
    }

    @Test
    fun identifiersRoundTripExactly() {
        val encoded = CanonicalWriter().putIdentifier("R001-FIXTURES-V1").toByteArray()
        assertEquals("R001-FIXTURES-V1", CanonicalReader(encoded).readIdentifier())
    }

    @Test
    fun canonicalMapsSortByKeyBytesRegardlessOfInsertionOrder() {
        val entries = listOf(
            CanonicalMapEntry(byteArrayOf(3), byteArrayOf(30)),
            CanonicalMapEntry(byteArrayOf(1), byteArrayOf(10)),
            CanonicalMapEntry(byteArrayOf(2), byteArrayOf(20)),
        )
        val forward = CanonicalWriter().putCanonicalMap(entries).toByteArray()
        val reversed = CanonicalWriter().putCanonicalMap(entries.reversed()).toByteArray()
        val shuffled = CanonicalWriter().putCanonicalMap(
            listOf(entries[1], entries[0], entries[2]),
        ).toByteArray()

        assertTrue(forward.contentEquals(reversed))
        assertTrue(forward.contentEquals(shuffled))

        val decoded = CanonicalReader(forward).readCanonicalMap()
        assertEquals(listOf(1, 2, 3), decoded.map { it.key.single().toInt() })
    }

    @Test
    fun keyOrderingIsUnsignedNotSigned() {
        // 0x80 is negative as a signed byte and larger than 0x01 as unsigned.
        // Signed comparison here would silently reorder half the key space.
        val entries = listOf(
            CanonicalMapEntry(byteArrayOf(0x80.toByte()), byteArrayOf(1)),
            CanonicalMapEntry(byteArrayOf(0x01), byteArrayOf(2)),
        )
        val decoded = CanonicalReader(
            CanonicalWriter().putCanonicalMap(entries).toByteArray(),
        ).readCanonicalMap()
        assertEquals(0x01, decoded[0].key.single().toInt() and 0xFF)
        assertEquals(0x80, decoded[1].key.single().toInt() and 0xFF)
    }

    @Test
    fun duplicateMapKeysAreAFaultNotAMerge() {
        assertFailsWith<CanonicalCodecException> {
            CanonicalWriter().putCanonicalMap(
                listOf(
                    CanonicalMapEntry(byteArrayOf(1), byteArrayOf(10)),
                    CanonicalMapEntry(byteArrayOf(1), byteArrayOf(20)),
                ),
            )
        }
    }

    @Test
    fun anOutOfOrderMapIsRejectedOnRead() {
        // Hand-built bytes with descending keys: a well-formed length prefix but
        // a non-canonical ordering. A lenient reader would accept this and let
        // two byte sequences mean the same state.
        val bytes = CanonicalWriter()
            .beginSequence(2)
            .putBytes(byteArrayOf(2)).putBytes(byteArrayOf(20))
            .putBytes(byteArrayOf(1)).putBytes(byteArrayOf(10))
            .toByteArray()
        assertFailsWith<CanonicalCodecException> { CanonicalReader(bytes).readCanonicalMap() }
    }

    @Test
    fun truncatedInputIsAFault() {
        assertFailsWith<CanonicalCodecException> { CanonicalReader(byteArrayOf(0, 0)).readI32() }
        assertFailsWith<CanonicalCodecException> {
            CanonicalReader(byteArrayOf(0, 0, 0, 8, 1, 2)).readBytes()
        }
    }

    @Test
    fun trailingBytesAreAFault() {
        val reader = CanonicalReader(byteArrayOf(0, 0, 0, 1))
        reader.readI32()
        reader.requireExhausted()

        val withTrailer = CanonicalReader(byteArrayOf(0, 0, 0, 1, 99))
        withTrailer.readI32()
        assertFailsWith<CanonicalCodecException> { withTrailer.requireExhausted() }
    }

    @Test
    fun envelopeRoundTripsAndCarriesItsVersions() {
        val payload = byteArrayOf(7, 7, 7)
        val envelope = CanonicalEnvelope.wrap(201, 1, payload)
        assertEquals(CanonicalEnvelope.HEADER_SIZE + payload.size, envelope.size)

        val decoded = CanonicalEnvelope.unwrap(envelope)
        assertEquals(CanonicalEnvelope.FORMAT_VERSION, decoded.envelopeFormatVersion)
        assertEquals(CanonicalEnvelope.CONTRACT_VERSION, decoded.determinismContractVersion)
        assertEquals(201, decoded.payloadSchemaId)
        assertEquals(1, decoded.payloadSchemaVersion)
        assertTrue(payload.contentEquals(decoded.payload))
    }

    @Test
    fun envelopeRefusesForeignMagicAndUnknownVersions() {
        assertFailsWith<CanonicalCodecException> {
            CanonicalEnvelope.unwrap(ByteArray(CanonicalEnvelope.HEADER_SIZE))
        }
        val envelope = CanonicalEnvelope.wrap(201, 1, byteArrayOf(1))
        // Bump the envelope format version in place.
        envelope[5] = 9
        assertFailsWith<CanonicalCodecException> { CanonicalEnvelope.unwrap(envelope) }
    }

    @Test
    fun envelopeRefusesADeclaredLengthThatDoesNotMatchTheBody() {
        val envelope = CanonicalEnvelope.wrap(201, 1, byteArrayOf(1, 2, 3))
        envelope[CanonicalEnvelope.HEADER_SIZE - 1] = 9 // claim 9 payload bytes
        assertFailsWith<CanonicalCodecException> { CanonicalEnvelope.unwrap(envelope) }
    }
}
