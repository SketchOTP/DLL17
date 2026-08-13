package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.state.DurabilityClass

/**
 * The single writer, contract section 9.1.
 *
 * Modelled rather than threaded, on purpose: the property that matters is that
 * no two durable writes interleave, and a reentrancy check proves that at every
 * call site in every test. A real dispatcher can satisfy this interface; a
 * violation of it fails loudly here instead of producing an interleaved journal
 * on one device in one timing window.
 */
public class SingleWriterActor {
    private var inWrite: Boolean = false
    private var writes: Int = 0

    public val writeCount: Int get() = writes

    public fun <T> write(block: () -> T): T {
        if (inWrite) {
            throw IllegalStateException(
                "reentrant durable write: the persistence writer is single by contract",
            )
        }
        inWrite = true
        try {
            writes += 1
            return block()
        } finally {
            inWrite = false
        }
    }
}

/**
 * Class O batching and Class W immediacy, contract section 8.
 *
 * Class E never reaches here at all: an ephemeral acknowledgement asserts only
 * that the interface received input, and journaling it would make a ripple part
 * of canonical history.
 */
public class CommitBatcher(
    private val cadenceMillis: Long = ContinuityContract.CLASS_O_COMMIT_CADENCE_MILLIS,
    private val maxWindowMillis: Long = ContinuityContract.CLASS_O_MAX_UNCOMMITTED_WINDOW_MILLIS,
) {
    private val pending: MutableList<ContinuityEvent> = ArrayList()
    private var windowOpenedAtMillis: Long = -1L

    public val pendingCount: Int get() = pending.size

    /** Returns the batch to write now, or an empty list to keep batching. */
    public fun offer(event: ContinuityEvent, nowMillis: Long): List<ContinuityEvent> {
        if (event.durabilityClass == DurabilityClass.EPHEMERAL) return emptyList()

        if (event.durabilityClass == DurabilityClass.WITNESSED) {
            // A witnessed transition cannot wait for a cadence: its presentation
            // is gated on the acknowledgement, so batching it would stall the
            // organism's visible response behind an unrelated timer.
            val batch = pending + event
            pending.clear()
            windowOpenedAtMillis = -1L
            return batch
        }

        if (pending.isEmpty()) windowOpenedAtMillis = nowMillis
        pending += event

        val openFor = nowMillis - windowOpenedAtMillis
        if (openFor >= cadenceMillis || openFor >= maxWindowMillis) {
            val batch = pending.toList()
            pending.clear()
            windowOpenedAtMillis = -1L
            return batch
        }
        return emptyList()
    }

    /** Drains whatever is pending. Used on backgrounding and before any barrier. */
    public fun flush(): List<ContinuityEvent> {
        val batch = pending.toList()
        pending.clear()
        windowOpenedAtMillis = -1L
        return batch
    }
}

/** The outcome of a compaction cycle. */
public class CompactionResult(
    public val checkpoint: Checkpoint?,
    public val prunedThroughSequence: Long,
    public val installed: Boolean,
    public val reason: String,
)

/**
 * The generation-partitioned append-only journal, contract section 9.
 *
 * Generations are logical partitions inside one store, identified by immutable
 * increasing IDs. The flip is an in-memory increment and requires no
 * transaction, which is what makes it usable in the reducer's path without
 * putting a database write there.
 */
public class ContinuityJournal(
    private val store: EncryptedRecordStore,
    private val writer: SingleWriterActor = SingleWriterActor(),
    initialGenerationId: Long = 1L,
    private var baseState: ContinuityState,
) {
    private var nextSequence: Long = 1L
    private var currentGeneration: Long = initialGenerationId
    private var sealedGeneration: Long? = null
    private var installed: Checkpoint? = null
    private val acknowledged: MutableSet<Long> = LinkedHashSet()
    private val generationOf: MutableMap<Long, Long> = LinkedHashMap()

    public val generationId: Long get() = currentGeneration
    public val sealedGenerationId: Long? get() = sealedGeneration
    public val lastSequence: Long get() = nextSequence - 1L
    public val installedCheckpoint: Checkpoint? get() = installed
    public val usedBytes: Long get() = store.usedBytes
    public val writeCount: Int get() = writer.writeCount

    public fun isAcknowledged(sequence: Long): Boolean = acknowledged.contains(sequence)

    /**
     * Appends one event and returns its durable sequence.
     *
     * The sequence is consumed whether or not the write completes. Reusing it
     * after a failure would reuse an AEAD nonce, which is the one mistake this
     * design cannot tolerate.
     */
    public fun append(event: ContinuityEvent): Long {
        val sequence = nextSequence
        nextSequence += 1L
        writer.write {
            store.append(
                sequence = sequence,
                generationId = currentGeneration,
                schemaId = ContinuityEvent.SCHEMA_ID,
                schemaVersion = ContinuityEvent.SCHEMA_VERSION,
                plaintext = event.canonicalPayloadBytes(),
            )
        }
        acknowledged += sequence
        generationOf[sequence] = currentGeneration
        maybeFlipGeneration()
        return sequence
    }

    /** Appends a batch under one writer entry, as the writer actor coalesces them. */
    public fun appendBatch(events: List<ContinuityEvent>): List<Long> {
        val sequences = ArrayList<Long>(events.size)
        writer.write {
            for (event in events) {
                val sequence = nextSequence
                nextSequence += 1L
                store.append(
                    sequence = sequence,
                    generationId = currentGeneration,
                    schemaId = ContinuityEvent.SCHEMA_ID,
                    schemaVersion = ContinuityEvent.SCHEMA_VERSION,
                    plaintext = event.canonicalPayloadBytes(),
                )
                acknowledged += sequence
                generationOf[sequence] = currentGeneration
                sequences += sequence
            }
        }
        maybeFlipGeneration()
        return sequences
    }

    /** The generation flip: an in-memory increment at the soft threshold. */
    private fun maybeFlipGeneration() {
        if (store.usedBytes < ContinuityContract.JOURNAL_SOFT_FLIP_BYTES) return
        if (sealedGeneration == currentGeneration) return
        sealedGeneration = currentGeneration
        currentGeneration += 1L
    }

    /** Forces a flip, for fixtures that must exercise a sealed generation. */
    public fun flipGeneration(): Long {
        sealedGeneration = currentGeneration
        currentGeneration += 1L
        return currentGeneration
    }

    /** Every event record still present, in sequence order. */
    public fun events(): List<Pair<Long, ContinuityEvent>> = store.readAll()
        .filter { it.schemaId == ContinuityEvent.SCHEMA_ID }
        .map { it.sequence to ContinuityEvent.decodePayload(it.payload) }

    /**
     * One compaction cycle, contract section 9.3.
     *
     * The candidate is built and verified **outside** the writer. Only the
     * install is a durable write, and pruning happens only after that install is
     * acknowledged. If anything fails, both generations remain recoverable and
     * no covered data is deleted — which is why every early return here reports
     * a reason instead of throwing.
     */
    public fun compact(ctx: ArithmeticContext): CompactionResult {
        val sealed = sealedGeneration
            ?: return CompactionResult(null, 0L, false, "no sealed generation to compact")

        val covered = events().filter { (sequence, _) ->
            sequence > (installed?.throughSequence ?: 0L)
        }
        val inSealed = covered.filter { (sequence, _) -> sequenceGeneration(sequence) <= sealed }
        if (inSealed.isEmpty()) {
            return CompactionResult(null, 0L, false, "sealed generation covers no events")
        }

        // Built outside the writer lock, exactly as the contract requires.
        var candidateState = baseState
        for ((_, event) in inSealed) {
            candidateState = ContinuityReducer.reduce(candidateState, event, ctx)
        }
        val throughSequence = inSealed.last().first
        val candidate = Checkpoint(
            generationId = sealed,
            throughSequence = throughSequence,
            stateBytes = candidateState.canonicalBytes(),
            stateHash = candidateState.stateHash(),
            engineContractVersion = ENGINE_CONTRACT_VERSION,
            eventContractVersion = ContinuityEvent.EVENT_CONTRACT_VERSION,
            randomContractVersion = ContinuityContract.CONTRACT_VERSION,
        )

        if (!candidate.verifySelf()) {
            return CompactionResult(null, 0L, false, "candidate checkpoint failed self-verification")
        }
        // Coverage verification: the checkpoint must account for every sequence
        // it claims to cover, with no gaps. Pruning on an incomplete checkpoint
        // is how history disappears.
        val expected = inSealed.map { it.first }
        if (expected.last() != throughSequence || expected.size != expected.distinct().size) {
            return CompactionResult(null, 0L, false, "coverage verification failed")
        }

        val checkpointSequence = nextSequence
        nextSequence += 1L
        try {
            writer.write {
                store.append(
                    sequence = checkpointSequence,
                    generationId = sealed,
                    schemaId = Checkpoint.SCHEMA_ID,
                    schemaVersion = Checkpoint.SCHEMA_VERSION,
                    plaintext = candidate.canonicalBytes(),
                )
            }
        } catch (failure: RuntimeException) {
            // Nothing pruned. Both the sealed and active journals remain
            // recoverable, which is the whole point of pruning last.
            return CompactionResult(
                null,
                0L,
                false,
                "checkpoint install failed, nothing pruned: ${failure.message}",
            )
        }
        acknowledged += checkpointSequence

        store.prune(throughSequence)
        installed = candidate
        baseState = candidateState
        sealedGeneration = null
        return CompactionResult(candidate, throughSequence, true, "installed")
    }

    /** Which generation a sequence was stamped with when it was written. */
    private fun sequenceGeneration(sequence: Long): Long =
        generationOf[sequence] ?: currentGeneration

    public companion object {
        public const val ENGINE_CONTRACT_VERSION: Int = 1

        /**
         * Recovers the last durable state from the medium alone.
         *
         * Recovery sees only what was acknowledged. Anything after the last
         * acknowledged record is discarded, and because Class W visibility is
         * gated on acknowledgement, discarding it cannot erase something the
         * user was already shown.
         */
        public fun recover(
            store: EncryptedRecordStore,
            genesis: ContinuityState,
            ctx: ArithmeticContext,
        ): RecoveredState {
            val records = store.readAll()
            val lastCheckpoint = records.lastOrNull { it.schemaId == Checkpoint.SCHEMA_ID }
            val checkpoint = lastCheckpoint?.let { Checkpoint.decode(it.payload) }

            var state = if (checkpoint != null) {
                if (!checkpoint.verifySelf()) {
                    throw StorageFault("installed checkpoint failed hash verification")
                }
                ContinuityState.decode(checkpoint.stateBytes)
            } else {
                genesis
            }

            val from = checkpoint?.throughSequence ?: 0L
            var applied = 0
            for (record in records) {
                if (record.schemaId != ContinuityEvent.SCHEMA_ID) continue
                if (record.sequence <= from) continue
                state = ContinuityReducer.reduce(
                    state,
                    ContinuityEvent.decodePayload(record.payload),
                    ctx,
                )
                applied += 1
            }
            return RecoveredState(
                state = state,
                fromCheckpointSequence = from,
                eventsReplayed = applied,
                checkpointPresent = checkpoint != null,
                stateHashHex = CanonicalHash.hex(state.stateHash()),
            )
        }
    }
}

/** What recovery reconstructed, and from where. */
public class RecoveredState(
    public val state: ContinuityState,
    public val fromCheckpointSequence: Long,
    public val eventsReplayed: Int,
    public val checkpointPresent: Boolean,
    public val stateHashHex: String,
)
