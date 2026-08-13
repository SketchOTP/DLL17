package com.animusmachinae.dll17.core.math

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalWriter

/**
 * A versioned integer lookup table bound to the digest of its own canonical
 * serialization, per `DeterminismContractV1` section 11.
 *
 * The point of the digest is that nobody reads a table. A bad merge, a truncated
 * generator run or a well-meant hand edit inside a block of hundreds of constants
 * is invisible to code review, and the symptom would be a slow behavioural drift
 * rather than a failure. Binding the table to a digest converts that class of
 * corruption into a hard startup failure.
 */
public class LookupTable(
    public val tableId: String,
    public val tableVersion: Int,
    public val expectedDigestHex: String,
    private val values: LongArray,
) {
    public val size: Int get() = values.size

    /** Table value at [index]. Out-of-range access is a defect, not a clamp. */
    public fun valueAt(index: Int): Long {
        if (index < 0 || index >= values.size) {
            throw IndexOutOfBoundsException(
                "lookup index $index outside $tableId of size ${values.size}",
            )
        }
        return values[index]
    }

    /** Canonical serialization of the table, envelope included. */
    public fun canonicalBytes(): ByteArray {
        val payload = CanonicalWriter(16 + values.size * 8)
            .putIdentifier(tableId)
            .beginSequence(values.size)
        for (value in values) payload.putI64(value)
        return CanonicalEnvelope.wrap(SCHEMA_ID, tableVersion, payload.toByteArray())
    }

    public fun computedDigestHex(): String =
        CanonicalHash.hex(CanonicalHash.ofEnvelope(canonicalBytes()))

    /**
     * Verifies the embedded digest.
     *
     * A mismatch throws. It is never a warning, never a fallback and never a
     * silent regeneration: a table that does not match its digest is not a table
     * whose contents anyone knows.
     */
    public fun verify() {
        val actual = computedDigestHex()
        if (actual != expectedDigestHex) {
            throw IllegalStateException(
                "lookup table $tableId v$tableVersion failed digest verification: " +
                    "expected $expectedDigestHex, computed $actual",
            )
        }
    }

    private var verified: Boolean = false

    /** Verifies once, on first production use. */
    public fun verifyOnce() {
        if (verified) return
        verify()
        verified = true
    }

    public companion object {
        /** Canonical payload schema ID for a serialized lookup table. */
        public const val SCHEMA_ID: Int = 101
    }
}
