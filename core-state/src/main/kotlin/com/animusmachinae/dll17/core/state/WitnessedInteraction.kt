package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.math.ArithmeticContext

/**
 * The Class W staged witnessed-interaction protocol and the commit visibility
 * invariant, per Implementation Plan E2E work packages R001.6 and R001.9.
 *
 * The invariant this file exists to make true:
 *
 * ```
 * materialPresentationAllowed(event)
 *     only if durableCommitAcknowledged(event.commitFrame)
 * ```
 *
 * The subtle half is not the durability. It is *at-most-once*: the presentation
 * token is consumed when the durable append **starts**, not when the animation
 * finishes. A process that dies mid-animation therefore recovers the canonical
 * interaction and emits no delayed reaction, because the token is already spent.
 * Consuming it at the end would produce exactly the failure the architecture
 * names — a smile arriving minutes late because animation completion was
 * uncertain.
 */

/**
 * Every boundary at which Implementation Plan E2E requires a crash to be
 * survivable. Ordinals are immutable.
 */
public enum class CrashBoundary(public val ordinal32: Int) {
    NONE(0),
    BEFORE_CLASSIFICATION(1),
    AFTER_CANDIDATE_REDUCTION(2),
    BEFORE_RECEIPT_WRITE(3),
    DURING_RECEIPT_WRITE(4),
    AFTER_DURABLE_RECEIPT_ACKNOWLEDGEMENT(5),
    BEFORE_CANONICAL_PUBLICATION(6),
    BEFORE_FINAL_SEMANTIC_PRESENTATION(7),
    AFTER_PRESENTATION_TOKEN_CONSUMPTION(8),
    ;

    public companion object {
        /** Every boundary a qualification sweep must cover. */
        public fun injectable(): List<CrashBoundary> = entries.filter { it != NONE }
    }
}

/** What the presentation layer was told, in order. Noncanonical. */
public enum class PresentationKind(public val ordinal32: Int) {
    /** Class E: the interface received input. Asserts nothing else. */
    EPHEMERAL_ACKNOWLEDGEMENT(1),

    /**
     * Semantically reversible anticipation: orienting, leaning, neutral contact
     * settle. Permitted while durability is pending precisely because it can be
     * withdrawn without having claimed anything.
     */
    REVERSIBLE_ANTICIPATION(2),

    /** The final semantic reaction. Permitted only after durable acknowledgement. */
    FINAL_SEMANTIC(3),

    /** Neutral cancel or interruption, used when persistence fails. */
    NEUTRAL_CANCELLATION(4),
}

/** One presentation instruction. */
public data class PresentationRecord(
    val kind: PresentationKind,
    val logicalEventId: Long,
)

/**
 * Presentation sink that enforces the commit visibility invariant itself.
 *
 * Enforcement lives here rather than in the caller on purpose. An invariant that
 * depends on every call site remembering to check it is a convention; an
 * invariant that the sink refuses to violate is a control.
 */
public class GatedPresentationSink(private val journal: DurableJournal) {
    private val records: MutableList<PresentationRecord> = ArrayList()

    public fun log(): List<PresentationRecord> = records.toList()

    public fun ephemeralAcknowledgement(logicalEventId: Long) {
        records.add(PresentationRecord(PresentationKind.EPHEMERAL_ACKNOWLEDGEMENT, logicalEventId))
    }

    public fun reversibleAnticipation(logicalEventId: Long) {
        records.add(PresentationRecord(PresentationKind.REVERSIBLE_ANTICIPATION, logicalEventId))
    }

    public fun neutralCancellation(logicalEventId: Long) {
        records.add(PresentationRecord(PresentationKind.NEUTRAL_CANCELLATION, logicalEventId))
    }

    /**
     * The gated call. Refuses unless the frame is durably acknowledged.
     *
     * @throws IllegalStateException if the commit is not acknowledged.
     */
    public fun finalSemantic(logicalEventId: Long, commitSequence: Long) {
        if (!journal.isAcknowledged(commitSequence)) {
            throw IllegalStateException(
                "commit visibility invariant violated: final semantic presentation for event " +
                    "$logicalEventId requested before commit frame $commitSequence was durably " +
                    "acknowledged",
            )
        }
        records.add(PresentationRecord(PresentationKind.FINAL_SEMANTIC, logicalEventId))
    }

    public fun countOf(kind: PresentationKind): Int = records.count { it.kind == kind }

    public fun finalSemanticCountFor(logicalEventId: Long): Int =
        records.count { it.kind == PresentationKind.FINAL_SEMANTIC && it.logicalEventId == logicalEventId }
}

/** Result of running one interaction through the staged protocol. */
public class InteractionOutcome(
    public val published: CanonicalSnapshot,
    public val committedSequence: Long?,
    public val receipt: MaterialInteractionReceipt?,
    public val crashedAt: CrashBoundary,
)

/**
 * The staged Class W pipeline.
 *
 * `published` is the only canonical state anyone outside this class may observe.
 * The candidate produced in stage 4 is deliberately held in a local: a candidate
 * that were assigned to `published` before durable acknowledgement would be
 * exactly the uncommitted material mutation the invariant forbids.
 */
public class WitnessedInteractionPipeline(
    genesis: CanonicalSnapshot,
    public val journal: DurableJournal,
) {
    public var published: CanonicalSnapshot = genesis
        private set

    public val presentation: GatedPresentationSink = GatedPresentationSink(journal)

    private var nextSequence: Long = 1L

    /** Receipts whose token was spent, so recovery can prove it never re-fires. */
    private val spentReceipts: MutableList<MaterialInteractionReceipt> = ArrayList()

    public fun spentReceipts(): List<MaterialInteractionReceipt> = spentReceipts.toList()

    /**
     * Runs one event through the staged protocol, optionally dying at [crashAt].
     *
     * @throws SimulatedProcessDeath at the requested boundary.
     */
    public fun submit(
        event: CanonicalEvent,
        ctx: ArithmeticContext,
        crashAt: CrashBoundary = CrashBoundary.NONE,
    ): InteractionOutcome {
        // Stage 1 — Class E acknowledgement. Immediate, noncanonical, and it
        // survives every later failure because it claimed nothing.
        presentation.ephemeralAcknowledgement(event.logicalEventId)

        if (crashAt == CrashBoundary.BEFORE_CLASSIFICATION) throw SimulatedProcessDeath(crashAt)

        // Stage 2 — normalize and classify.
        val durability = event.durabilityClass

        // Non-witnessed traffic does not need the staged protocol.
        if (durability != DurabilityClass.WITNESSED) {
            return submitUnwitnessed(event, ctx)
        }

        // Stage 3 — compute the candidate WITHOUT publishing it.
        val preHash = published.stateHash()
        ctx.logicalTime = published.logicalTime
        val candidate = CanonicalReducer.reduce(published, event, ctx)

        if (crashAt == CrashBoundary.AFTER_CANDIDATE_REDUCTION) throw SimulatedProcessDeath(crashAt)

        // While durability is pending, only reversible anticipation is allowed.
        presentation.reversibleAnticipation(event.logicalEventId)

        // Stage 4 — build the receipt.
        val sequence = nextSequence
        val receipt = MaterialInteractionReceipt(
            logicalEventId = event.logicalEventId,
            engineContractVersion = CanonicalSnapshot.ENGINE_CONTRACT_VERSION,
            eventContractVersion = CanonicalEvent.EVENT_CONTRACT_VERSION,
            normalizedInput = event,
            preInputStateHash = preHash,
            replaySequence = sequence,
        )

        if (crashAt == CrashBoundary.BEFORE_RECEIPT_WRITE) throw SimulatedProcessDeath(crashAt)

        // Stage 5 — durable append. The token is consumed as the write STARTS.
        receipt.consumePresentationToken()
        spentReceipts.add(receipt)

        val frame = CommitFrame(
            sequence = sequence,
            event = event,
            preStateHash = preHash,
            postStateHash = candidate.stateHash(),
            receiptDigest = receipt.canonicalBytes(),
        )

        try {
            journal.append(frame, interrupt = crashAt == CrashBoundary.DURING_RECEIPT_WRITE)
        } catch (death: SimulatedProcessDeath) {
            // Persistence failed. No Class W semantic claim may be made.
            presentation.neutralCancellation(event.logicalEventId)
            throw death
        }

        // Stage 6 — durable acknowledgement.
        nextSequence += 1L

        if (crashAt == CrashBoundary.AFTER_DURABLE_RECEIPT_ACKNOWLEDGEMENT) {
            throw SimulatedProcessDeath(crashAt)
        }

        if (crashAt == CrashBoundary.BEFORE_CANONICAL_PUBLICATION) throw SimulatedProcessDeath(crashAt)

        // Stage 7 — publish the deterministic material transition.
        published = candidate

        if (crashAt == CrashBoundary.BEFORE_FINAL_SEMANTIC_PRESENTATION) throw SimulatedProcessDeath(crashAt)

        // Stage 8 — final semantic reaction, gated on durable acknowledgement.
        presentation.finalSemantic(event.logicalEventId, sequence)

        if (crashAt == CrashBoundary.AFTER_PRESENTATION_TOKEN_CONSUMPTION) throw SimulatedProcessDeath(crashAt)

        return InteractionOutcome(published, sequence, receipt, CrashBoundary.NONE)
    }

    private fun submitUnwitnessed(
        event: CanonicalEvent,
        ctx: ArithmeticContext,
    ): InteractionOutcome {
        val preHash = published.stateHash()
        ctx.logicalTime = published.logicalTime
        val next = CanonicalReducer.reduce(published, event, ctx)
        val sequence = nextSequence
        journal.append(
            CommitFrame(
                sequence = sequence,
                event = event,
                preStateHash = preHash,
                postStateHash = next.stateHash(),
                receiptDigest = null,
            ),
        )
        nextSequence += 1L
        published = next
        return InteractionOutcome(published, sequence, null, CrashBoundary.NONE)
    }
}

/**
 * Recovery after process death.
 *
 * Recovery sees the durable medium and nothing else — no in-memory candidate, no
 * pipeline instance, no knowledge of how far an animation got. It rebuilds
 * canonical state by replaying acknowledged frames and emits **no** final
 * semantic presentation, ever. That absence is the at-most-once guarantee.
 */
public object InteractionRecovery {

    public class RecoveryResult(
        public val snapshot: CanonicalSnapshot,
        public val framesRecovered: Int,
        public val finalPresentationsEmitted: Int,
    )

    public fun recover(
        genesis: CanonicalSnapshot,
        durableRecords: List<ByteArray>,
        ctx: ArithmeticContext,
    ): RecoveryResult {
        val journal = DurableJournal.fromRawBytes(durableRecords)
        val replayed = ReplayKernel.replay(genesis, journal, ctx)
        if (!replayed.isConsistent) {
            throw IllegalStateException(
                "durable journal diverges from the reducer at sequence ${replayed.divergedAtSequence}",
            )
        }
        // Deliberately zero: recovery restores history, it does not perform it.
        return RecoveryResult(replayed.snapshot, replayed.framesApplied, 0)
    }
}
