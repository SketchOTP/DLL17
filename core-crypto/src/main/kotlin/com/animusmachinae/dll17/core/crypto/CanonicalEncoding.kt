package com.animusmachinae.dll17.core.crypto

/**
 * Canonical byte encoding, frozen by `DeterminismContractV1` sections 1 to 5 and 10.
 *
 * Every rule here exists because some ordinary alternative is not reproducible
 * across a desktop JVM and Android ART. The encoder is deliberately narrow: it
 * refuses inputs it cannot represent in exactly one way rather than choosing a
 * representation on the caller's behalf.
 */

/** Raised whenever canonical bytes cannot be produced or read unambiguously. */
public class CanonicalCodecException(message: String) : RuntimeException(message)

/**
 * Canonical identifier rules, contract section 5.
 *
 * Identifiers are ASCII-only so that no Unicode normalization table, locale or
 * ICU version can participate in canonical bytes. This is policy, not a
 * limitation to work around.
 */
public object CanonicalIdentifier {
    public const val MIN_LENGTH: Int = 1
    public const val MAX_LENGTH: Int = 64

    public fun isLegal(value: String): Boolean {
        if (value.length < MIN_LENGTH || value.length > MAX_LENGTH) return false
        for (index in value.indices) {
            val c = value[index]
            val ok = (c in 'A'..'Z') || (c in 'a'..'z') || (c in '0'..'9') ||
                c == '.' || c == '_' || c == '-'
            if (!ok) return false
        }
        return true
    }

    public fun require(value: String): String {
        if (!isLegal(value)) {
            throw CanonicalCodecException(
                "identifier is not canonical: length must be $MIN_LENGTH..$MAX_LENGTH and " +
                    "characters must be A-Z a-z 0-9 . _ - (got ${value.length} chars)",
            )
        }
        return value
    }

    /** ASCII bytes of [value]. Safe only after [require]; every legal char is one byte. */
    public fun asciiBytes(value: String): ByteArray {
        require(value)
        val out = ByteArray(value.length)
        for (index in value.indices) {
            out[index] = value[index].code.toByte()
        }
        return out
    }
}

/**
 * Append-only canonical byte writer. Big-endian everywhere, fixed-width length
 * prefixes, no variable-length integers.
 */
public class CanonicalWriter(initialCapacity: Int = 64) {
    private var buffer: ByteArray = ByteArray(if (initialCapacity < 16) 16 else initialCapacity)
    private var length: Int = 0

    public val size: Int get() = length

    private fun ensure(extra: Int) {
        val needed = length + extra
        if (needed <= buffer.size) return
        var capacity = buffer.size
        while (capacity < needed) capacity = capacity shl 1
        buffer = buffer.copyOf(capacity)
    }

    public fun putI8(value: Int): CanonicalWriter {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw CanonicalCodecException("i8 out of range: $value")
        }
        ensure(1)
        buffer[length++] = value.toByte()
        return this
    }

    public fun putU8(value: Int): CanonicalWriter {
        if (value < 0 || value > 0xFF) throw CanonicalCodecException("u8 out of range: $value")
        ensure(1)
        buffer[length++] = value.toByte()
        return this
    }

    public fun putI16(value: Int): CanonicalWriter {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw CanonicalCodecException("i16 out of range: $value")
        }
        ensure(2)
        buffer[length++] = (value ushr 8).toByte()
        buffer[length++] = value.toByte()
        return this
    }

    public fun putU16(value: Int): CanonicalWriter {
        if (value < 0 || value > 0xFFFF) throw CanonicalCodecException("u16 out of range: $value")
        ensure(2)
        buffer[length++] = (value ushr 8).toByte()
        buffer[length++] = value.toByte()
        return this
    }

    public fun putI32(value: Int): CanonicalWriter {
        ensure(4)
        buffer[length++] = (value ushr 24).toByte()
        buffer[length++] = (value ushr 16).toByte()
        buffer[length++] = (value ushr 8).toByte()
        buffer[length++] = value.toByte()
        return this
    }

    /** Unsigned 32-bit. Negative values and values above 2^31-1 are rejected. */
    public fun putU32(value: Int): CanonicalWriter {
        if (value < 0) throw CanonicalCodecException("u32 out of range: $value")
        return putI32(value)
    }

    public fun putI64(value: Long): CanonicalWriter {
        ensure(8)
        var shift = 56
        while (shift >= 0) {
            buffer[length++] = (value ushr shift).toByte()
            shift -= 8
        }
        return this
    }

    public fun putBool(value: Boolean): CanonicalWriter = putU8(if (value) 1 else 0)

    /**
     * Enum encoding, contract section 3: the immutable numeric ordinal from the
     * owning registry, never the Kotlin declaration order and never the name.
     */
    public fun putEnum(registryOrdinal: Int): CanonicalWriter = putI32(registryOrdinal)

    public fun putRawBytes(value: ByteArray): CanonicalWriter {
        ensure(value.size)
        value.copyInto(buffer, length)
        length += value.size
        return this
    }

    /** Length-prefixed opaque byte string. */
    public fun putBytes(value: ByteArray): CanonicalWriter {
        putU32(value.size)
        return putRawBytes(value)
    }

    /** Length-prefixed canonical ASCII identifier. */
    public fun putIdentifier(value: String): CanonicalWriter =
        putBytes(CanonicalIdentifier.asciiBytes(value))

    /** Element count of a canonical sequence. Declared element order is preserved. */
    public fun beginSequence(count: Int): CanonicalWriter {
        if (count < 0) throw CanonicalCodecException("sequence count out of range: $count")
        return putU32(count)
    }

    /**
     * Canonical map, contract section 4. Entries are sorted ascending by unsigned
     * lexicographic comparison of the serialized key bytes, so no hash-table
     * iteration order can reach the canonical bytes. Duplicate keys are a fault.
     */
    public fun putCanonicalMap(entries: List<CanonicalMapEntry>): CanonicalWriter {
        val sorted = entries.sortedWith(CanonicalMapEntry.KEY_ORDER)
        for (index in 1 until sorted.size) {
            if (compareUnsignedBytes(sorted[index - 1].key, sorted[index].key) == 0) {
                throw CanonicalCodecException("duplicate key in canonical map at index $index")
            }
        }
        beginSequence(sorted.size)
        for (entry in sorted) {
            putBytes(entry.key)
            putBytes(entry.value)
        }
        return this
    }

    public fun toByteArray(): ByteArray = buffer.copyOf(length)
}

/** One entry of a canonical map, already serialized to bytes. */
public class CanonicalMapEntry(public val key: ByteArray, public val value: ByteArray) {
    public companion object {
        public val KEY_ORDER: Comparator<CanonicalMapEntry> =
            Comparator { a, b -> compareUnsignedBytes(a.key, b.key) }
    }
}

/** Unsigned lexicographic byte comparison; shorter is smaller on a common prefix. */
public fun compareUnsignedBytes(a: ByteArray, b: ByteArray): Int {
    val shared = if (a.size < b.size) a.size else b.size
    for (index in 0 until shared) {
        val left = a[index].toInt() and 0xFF
        val right = b[index].toInt() and 0xFF
        if (left != right) return if (left < right) -1 else 1
    }
    return a.size.compareTo(b.size)
}

/** Strict canonical byte reader. Every malformed input is a fault, never a default. */
public class CanonicalReader(private val source: ByteArray, private var offset: Int = 0) {

    public val position: Int get() = offset
    public val remaining: Int get() = source.size - offset

    private fun take(count: Int): Int {
        if (count < 0 || remaining < count) {
            throw CanonicalCodecException(
                "truncated canonical input: need $count byte(s) at offset $offset, have $remaining",
            )
        }
        val start = offset
        offset += count
        return start
    }

    public fun readU8(): Int {
        val at = take(1)
        return source[at].toInt() and 0xFF
    }

    public fun readI8(): Int {
        val at = take(1)
        return source[at].toInt()
    }

    public fun readU16(): Int {
        val at = take(2)
        return ((source[at].toInt() and 0xFF) shl 8) or (source[at + 1].toInt() and 0xFF)
    }

    public fun readI32(): Int {
        val at = take(4)
        return ((source[at].toInt() and 0xFF) shl 24) or
            ((source[at + 1].toInt() and 0xFF) shl 16) or
            ((source[at + 2].toInt() and 0xFF) shl 8) or
            (source[at + 3].toInt() and 0xFF)
    }

    public fun readU32(): Int {
        val value = readI32()
        if (value < 0) throw CanonicalCodecException("u32 field exceeds Int.MAX_VALUE")
        return value
    }

    public fun readI64(): Long {
        val at = take(8)
        var value = 0L
        for (index in 0 until 8) {
            value = (value shl 8) or (source[at + index].toLong() and 0xFF)
        }
        return value
    }

    public fun readBool(): Boolean = when (val raw = readU8()) {
        0 -> false
        1 -> true
        // Deliberately not String.format: that is locale-sensitive, and this file
        // is the one place where locale sensitivity must never become a habit.
        else -> throw CanonicalCodecException("boolean byte must be 0x00 or 0x01, got $raw")
    }

    public fun readEnum(): Int = readI32()

    public fun readBytes(): ByteArray {
        val size = readU32()
        val at = take(size)
        return source.copyOfRange(at, at + size)
    }

    public fun readIdentifier(): String {
        val bytes = readBytes()
        val builder = StringBuilder(bytes.size)
        for (byte in bytes) {
            val code = byte.toInt() and 0xFF
            if (code > 0x7F) throw CanonicalCodecException("identifier contains a non-ASCII byte")
            builder.append(code.toChar())
        }
        return CanonicalIdentifier.require(builder.toString())
    }

    public fun readSequenceCount(): Int = readU32()

    public fun readCanonicalMap(): List<CanonicalMapEntry> {
        val count = readSequenceCount()
        val entries = ArrayList<CanonicalMapEntry>(count)
        var previous: ByteArray? = null
        for (index in 0 until count) {
            val key = readBytes()
            val value = readBytes()
            val prior = previous
            if (prior != null && compareUnsignedBytes(prior, key) >= 0) {
                throw CanonicalCodecException(
                    "canonical map keys are not strictly ascending at entry $index",
                )
            }
            previous = key
            entries.add(CanonicalMapEntry(key, value))
        }
        return entries
    }

    public fun requireExhausted() {
        if (remaining != 0) {
            throw CanonicalCodecException("$remaining trailing byte(s) after canonical payload")
        }
    }
}
