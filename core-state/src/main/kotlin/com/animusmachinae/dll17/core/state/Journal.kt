package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.math.ArithmeticContext

/**
 * The durable journal and the replay kernel, per Implementation Plan E2E work
 * package R001.5.
 *
 * **Scope note.** R001 owns the *contract* — append-only ordering, durable
 * acknowledgement, and `reduce == replay` byte identity. It does not own
 * persistence *semantics*: journals, compaction thresholds, encrypted-record
 * boundaries and reconciliation ordering are R002's
 * `ContinuityDurabilityContractV1`. The durable medium here is therefore an
 * in-process append-only byte log, which is enough to prove every R001 invariant
 * — a frame is either fully acknowledged in the byte log or it is not — without
 * pre-empting decisions that belong to a later phase.
 */

/**
 * One durably appended commit frame.
 *
 * Both the pre-state and post-state hashes are recorded. Only the post-state
 * hash is strictly needed to detect divergence, but carrying the pre-state hash
 * makes a replay able to prove *where* a divergence began rather than only that
 * the end differed.
 */
public class CommitFrame(
    public val sequence: Long,
    public val event: CanonicalEvent,
    public val preStateHash: ByteArray,
    public val postStateHash: ByteArray,
    public val receiptDigest: ByteArray?,
) {
    public fun canonicalBytes(): ByteArray {
        val payload = CanonicalWriter(128)
            .putI64(sequence)
            .putBytes(event.canonicalPayloadBytes())
            .putBytes(preStateHash)
            .putBytes(postStateHash)
            .putBool(receiptDigest != null)
        if (receiptDigest != null) payload.putBytes(receiptDigest)
        return CanonicalEnvelope.wrap(SCHEMA_ID, SCHEMA_VERSION, payload.toByteArray())
    }

    override fun toString(): String =
        "CommitFrame(#$sequence ${event.type} ${CanonicalHash.hex(postStateHash).take(16)})"

    public companion object {
        public const val SCHEMA_ID: Int = 203
        public const val SCHEMA_VERSION: Int = 1

        public fun decode(bytes: ByteArray): CommitFrame {
            val contents = CanonicalEnvelope.unwrap(bytes)
            if (contents.payloadSchemaId != SCHEMA_ID) {
                throw IllegalArgumentException(
                    "expected commit frame schema $SCHEMA_ID, got ${contents.payloadSchemaId}",
                )
            }
            val reader = CanonicalReader(contents.payload)
            val sequence = reader.readI64()
            val event = CanonicalEvent.decodePayload(reader.readBytes())
            val preHash = reader.readBytes()
            val postHash = reader.readBytes()
            val hasReceipt = reader.readBool()
            val receipt = if (hasReceipt) reader.readBytes() else null
            reader.requireExhausted()
            return CommitFrame(sequence, event, preHash, postHash, receipt)
        }
    }
}

/** Raised when a fault-injection point fires. Never thrown in production paths. */
public class SimulatedProcessDeath(public val boundary: CrashBoundary) :
    RuntimeException("simulated process death at $boundary")

/**
 * Append-only durable journal.
 *
 * A frame becomes durable only when [append] returns normally. A frame whose
 * write is interrupted leaves no trace: the record is built completely in memory
 * and added to the acknowledged log in one step, which models a medium where a
 * torn write is not acknowledged. That is the weakest assumption that still
 * makes "last durably acknowledged frame" a well-defined thing to recover to.
 */
public class DurableJournal {
    private val acknowledged: MutableList<ByteArray> = ArrayList()

    /** Frames written but deliberately not acknowledged, for fault injection. */
    private var interruptedWrites: Int = 0

    public val size: Int get() = acknowledged.size
    public val interruptedWriteCount: Int get() = interruptedWrites

    public fun append(frame: CommitFrame, interrupt: Boolean = false) {
        val encoded = frame.canonicalBytes()
        if (interrupt) {
            // The write started and did not complete. Nothing is acknowledged,
            // so nothing is recoverable, which is the correct outcome.
            interruptedWrites += 1
            throw SimulatedProcessDeath(CrashBoundary.DURING_RECEIPT_WRITE)
        }
        acknowledged.add(encoded)
    }

    public fun isAcknowledged(sequence: Long): Boolean =
        frames().any { it.sequence == sequence }

    public fun frames(): List<CommitFrame> = acknowledged.map { CommitFrame.decode(it) }

    public fun lastSequence(): Long = frames().lastOrNull()?.sequence ?: 0L

    /** The durable medium's raw bytes. Recovery is allowed to see only this. */
    public fun rawBytes(): List<ByteArray> = acknowledged.map { it.copyOf() }

    public companion object {
        /** Rebuilds a journal from the durable medium alone, as recovery would. */
        public fun fromRawBytes(records: List<ByteArray>): DurableJournal {
            val journal = DurableJournal()
            for (record in records) {
                // Decoding here is deliberate: a corrupt record must fail at
                // recovery rather than propagate into canonical state.
                CommitFrame.decode(record)
                journal.acknowledged.add(record.copyOf())
            }
            return journal
        }
    }
}

/** Outcome of a replay, carrying enough detail to locate a divergence. */
public class ReplayResult(
    public val snapshot: CanonicalSnapshot,
    public val framesApplied: Int,
    public val divergedAtSequence: Long?,
) {
    public val isConsistent: Boolean get() = divergedAtSequence == null
}

/** The replay kernel. */
public object ReplayKernel {

    /**
     * Replays [journal] onto [genesis] using the same reducer that produced it.
     *
     * Using the same reducer is the point. A replay implemented by a second code
     * path would only prove that two implementations agree, which is a weaker and
     * less useful claim than proving that the journal is a faithful record of
     * what the reducer did.
     */
    public fun replay(
        genesis: CanonicalSnapshot,
        journal: DurableJournal,
        ctx: ArithmeticContext,
    ): ReplayResult {
        var current = genesis
        var applied = 0
        for (frame in journal.frames()) {
            if (!current.stateHash().contentEquals(frame.preStateHash)) {
                return ReplayResult(current, applied, frame.sequence)
            }
            ctx.logicalTime = current.logicalTime
            current = CanonicalReducer.reduce(current, frame.event, ctx)
            if (!current.stateHash().contentEquals(frame.postStateHash)) {
                return ReplayResult(current, applied, frame.sequence)
            }
            applied += 1
        }
        return ReplayResult(current, applied, null)
    }
}
