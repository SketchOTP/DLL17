package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter

/**
 * `PlatformPanicWitness`, per Implementation Plan E2E work package R001.7.
 *
 * A best-effort note, written once per critical suspend attempt, saying only
 * "something abrupt happened here". It is deliberately the weakest durable thing
 * in the system:
 *
 * - preallocated and fixed-size, so writing it allocates nothing at the moment
 *   the platform is already taking the process away;
 * - written at most once per attempt, never retried;
 * - **noncanonical** and excluded from every state hash;
 * - safe to lose entirely;
 * - unable to reconstruct or alter committed organism history.
 *
 * The last property is the one that matters and is the reason this class exposes
 * no way to feed its contents into a reducer. If the witness could influence
 * replay, a lost witness would change canonical state, and "safe to lose
 * entirely" would be false.
 *
 * The architect's 2026-08-07 correction is respected here: there is no 2.0 ms
 * constant. The attempt deadline is an empirical, device-measured value, so this
 * class takes it as a parameter and records what it observed rather than
 * asserting a budget.
 */
public class PlatformPanicWitness(
    /** Empirically chosen per-device deadline. R001 measures it; it is not a constant. */
    public val attemptDeadlineNanos: Long,
) {
    /** Fixed record size. The buffer is allocated once, at construction. */
    private val buffer: ByteArray = ByteArray(RECORD_SIZE)

    private var written: Boolean = false
    private var lastWriteDurationNanos: Long = -1L

    public val hasWritten: Boolean get() = written
    public val lastWriteNanos: Long get() = lastWriteDurationNanos

    /**
     * One bounded, non-retrying, best-effort write.
     *
     * Returns false if a write has already been attempted for this instance. It
     * does not throw on failure: throwing during a critical suspend would turn a
     * lost diagnostic into a crash.
     */
    public fun write(
        reasonOrdinal: Int,
        lastDurableSequence: Long,
        monotonicNanos: Long,
        clock: () -> Long = { 0L },
    ): Boolean {
        if (written) return false
        written = true
        val start = clock()
        val encoded = CanonicalWriter(RECORD_SIZE)
            .putI32(FORMAT_VERSION)
            .putI32(reasonOrdinal)
            .putI64(lastDurableSequence)
            .putI64(monotonicNanos)
            .toByteArray()
        // Fixed size by construction; copy into the preallocated buffer only.
        encoded.copyInto(buffer, 0, 0, if (encoded.size < RECORD_SIZE) encoded.size else RECORD_SIZE)
        lastWriteDurationNanos = clock() - start
        return true
    }

    /** Diagnostic read-back. Returns noncanonical fields; nothing here feeds a reducer. */
    public fun readDiagnostic(): PanicWitnessRecord? {
        if (!written) return null
        val reader = CanonicalReader(buffer)
        val formatVersion = reader.readI32()
        val reason = reader.readI32()
        val lastDurableSequence = reader.readI64()
        val monotonicNanos = reader.readI64()
        return PanicWitnessRecord(formatVersion, reason, lastDurableSequence, monotonicNanos)
    }

    /** Bytes of the record, for evidence capture only. Never a hash input. */
    public fun rawBytes(): ByteArray = buffer.copyOf()

    public companion object {
        public const val FORMAT_VERSION: Int = 1

        /** 4 + 4 + 8 + 8 bytes. Fixed forever within this format version. */
        public const val RECORD_SIZE: Int = 24
    }
}

/** Decoded panic-witness contents. Noncanonical. */
public data class PanicWitnessRecord(
    val formatVersion: Int,
    val reasonOrdinal: Int,
    val lastDurableSequence: Long,
    val monotonicNanos: Long,
)
