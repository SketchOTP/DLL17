package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.state.DurabilityClass

/**
 * Deterministic bounded offline reconciliation, contract section 7.
 *
 * Reconciliation never mutates state directly. It **plans an event sequence**,
 * folds that sequence with the ordinary reducer to know what the next chunk
 * should see, and hands the sequence to the caller to journal. Two properties
 * follow from that shape and would be very hard to get any other way:
 *
 * - `reduce == replay` holds for a reconciliation, because the reconciliation
 *   *is* a list of canonical events;
 * - a reconciliation interrupted at any chunk resumes from its cursor and
 *   produces exactly the sequence an uninterrupted run would have produced,
 *   because the cursor is the only mutable thing and every chunk is a pure
 *   function of the state before it.
 */
public class ReconciliationPlan(
    public val mode: ReconciliationMode,
    public val events: List<ContinuityEvent>,
    public val chunksApplied: Int,
    public val finalState: ContinuityState,
)

/**
 * A resumable reconciliation.
 *
 * Deterministic in `(state, observation, evidence)`. It reads no clock; the
 * observation is supplied by the caller and is the only platform input.
 */
public class Reconciliation private constructor(
    private val observation: ClockObservation,
    private val evidence: ElapsedEvidence,
    public val mode: ReconciliationMode,
    private val initialVerifiedTotal: Long,
    private val ctx: ArithmeticContext,
    private var state: ContinuityState,
) {
    private enum class Phase { PROLOGUE, CHUNKS, PROJECTION, EPILOGUE, DONE }

    private var phase: Phase = Phase.PROLOGUE
    private var offsetMillis: Long = 0L
    private var nextEventId: Long = state.lastCommitSequence + 1L
    private var chunks: Int = 0

    public val isComplete: Boolean get() = phase == Phase.DONE
    public val currentState: ContinuityState get() = state
    public val chunksApplied: Int get() = chunks
    public val cursorOffsetMillis: Long get() = offsetMillis

    /** Total verified milliseconds that will be chunked. Excludes Mode C projection. */
    private val chunkedMillis: Long =
        if (evidence.confidence.grantsVerifiedTime) {
            minOf(evidence.verifiedMillis, ContinuityContract.MODE_B_MAX_MILLIS)
        } else {
            0L
        }

    /**
     * Advances by at most [maxChunks] chunks and returns the events produced.
     *
     * Yielding is not an optimisation. The canonical architecture requires
     * reconciliation to run off the UI thread and to yield between bounded
     * batches; a slice boundary that changed the result would make that
     * requirement unimplementable.
     */
    public fun advance(maxChunks: Int = ContinuityContract.RECONCILIATION_SLICE_CHUNKS): List<ContinuityEvent> {
        if (maxChunks <= 0) throw IllegalArgumentException("slice must contain at least one chunk")
        val produced = ArrayList<ContinuityEvent>()

        if (phase == Phase.PROLOGUE) {
            produced += prologue()
            phase = Phase.CHUNKS
        }

        if (phase == Phase.CHUNKS) {
            var inSlice = 0
            while (offsetMillis < chunkedMillis && inSlice < maxChunks) {
                produced += chunk()
                inSlice += 1
            }
            if (offsetMillis >= chunkedMillis) phase = Phase.PROJECTION
            if (inSlice >= maxChunks && phase == Phase.CHUNKS) return produced
        }

        if (phase == Phase.PROJECTION) {
            produced += projection()
            phase = Phase.EPILOGUE
        }

        if (phase == Phase.EPILOGUE) {
            produced += epilogue()
            phase = Phase.DONE
        }
        return produced
    }

    private fun emit(
        type: ContinuityEventType,
        a: Long = 0L,
        b: Long = 0L,
        durability: DurabilityClass = DurabilityClass.ORDINARY,
        anchor: DurableTimeAnchor? = null,
    ): ContinuityEvent {
        val event = ContinuityEvent.of(nextEventId, type, a, b, durability, anchor)
        nextEventId += 1L
        state = ContinuityReducer.reduce(state, event, ctx)
        return event
    }

    // ---------------------------------------------------------------- prologue

    private fun prologue(): List<ContinuityEvent> {
        val produced = ArrayList<ContinuityEvent>()
        var bootAnomaly = false

        if (evidence.bootChanged && observation.bootIdentityPresent) {
            bootAnomaly = BlindCredit.bootVelocityAnomaly(state.credit, state.verifiedTimeTotalMillis)
            produced += emit(
                ContinuityEventType.BOOT_OBSERVED,
                a = observation.bootIdentity,
                b = if (bootAnomaly) 1L else 0L,
            )
        }

        if (evidence.anomaly) {
            produced += emit(ContinuityEventType.CLOCK_ANOMALY_DETECTED, a = evidence.skewMillis)
        }

        if (!evidence.confidence.grantsVerifiedTime) {
            val consumable = BlindCredit.consumable(
                ledger = state.credit,
                requestedMillis = evidence.unverifiedMillis,
                bootIdentity = observation.bootIdentity,
                bootVelocityAnomaly = bootAnomaly,
            )
            if (consumable > 0L) {
                // Class W: the debit must be durably acknowledged before the
                // reconciled state is revealed, or a crash between reveal and
                // write would refund the credit.
                produced += emit(
                    ContinuityEventType.BLIND_CREDIT_CONSUMED,
                    a = consumable,
                    b = observation.bootIdentity,
                    durability = DurabilityClass.WITNESSED,
                )
            }
            val excess = evidence.unverifiedMillis - consumable
            if (excess > 0L) {
                val headroom = (
                    ContinuityContract.DEBT_GLOBAL_CAP_BASELINE_EQUIV_MILLIS -
                        state.debt.outstandingBaselineEquivMillis
                    ).coerceAtLeast(0L)
                val accrued = minOf(excess, headroom)
                // Excess beyond the cap is forgiven at accrual, never retained.
                // A retained balance the user cannot see or discharge is exactly
                // the delayed punishment bomb the architecture prohibits.
                produced += emit(
                    ContinuityEventType.UNRESOLVED_TIME_DEBT_ACCRUED,
                    a = accrued,
                    b = excess - accrued,
                )
            }
        }
        return produced
    }

    // ------------------------------------------------------------------ chunks

    private fun chunk(): List<ContinuityEvent> {
        val produced = ArrayList<ContinuityEvent>()
        val size = minOf(chunkSizeAt(offsetMillis), chunkedMillis - offsetMillis)

        produced += emit(ContinuityEventType.VERIFIED_TIME_ADVANCED, a = size)
        produced += emit(ContinuityEventType.BASELINE_METABOLISM_APPLIED, a = size)
        produced += debtStep()

        offsetMillis += size
        chunks += 1
        return produced
    }

    /**
     * The bounded debt-collection step, contract section 6.4.
     *
     * Ineligible in Mode A by construction: foreground and brief-absence
     * cadences always run at baseline, so returning to the app can never
     * increase biological burn.
     */
    private fun debtStep(): List<ContinuityEvent> {
        if (mode == ReconciliationMode.MODE_A) return emptyList()
        if (state.debt.outstandingBaselineEquivMillis <= 0L) return emptyList()

        val produced = ArrayList<ContinuityEvent>()
        val now = state.verifiedTimeTotalMillis
        val abundant = state.reserveA >= ContinuityContract.DEBT_ABUNDANCE_REARM &&
            state.reserveB >= ContinuityContract.DEBT_ABUNDANCE_REARM

        if (state.debt.state == DebtState.PAUSED_LOW_RESERVE) {
            if (abundant) {
                if (!state.debt.abundanceStablePresent) {
                    produced += emit(
                        ContinuityEventType.DEBT_ABUNDANCE_STABILITY_UPDATED,
                        a = now,
                        b = 1L,
                    )
                } else if (
                    !state.debt.rearmEffectivePresent &&
                    now - state.debt.abundanceStableSinceVerifiedMillis >=
                    ContinuityContract.DEBT_REARM_STABILITY_MILLIS
                ) {
                    // Effective in the future, so the care event that restored the
                    // reserve can never be the one that pays for collection.
                    produced += emit(
                        ContinuityEventType.DEBT_REARM_ARMED,
                        a = now + ContinuityContract.DEBT_REARM_GRACE_MILLIS,
                    )
                }
            } else if (state.debt.abundanceStablePresent) {
                produced += emit(ContinuityEventType.DEBT_ABUNDANCE_STABILITY_UPDATED, a = 0L, b = 0L)
            }

            val rearmed = state.debt.rearmEffectivePresent &&
                now >= state.debt.rearmEffectiveAtVerifiedMillis
            if (!rearmed) return produced
        }

        val dayElapsed = now - state.debt.dayWindowStartVerifiedMillis
        val dayCollected = if (dayElapsed >= ContinuityContract.DEBT_VERIFIED_DAY_MILLIS) {
            0L
        } else {
            state.debt.collectedInDayBaselineEquivMillis
        }
        val dayHeadroom =
            (ContinuityContract.DEBT_PER_VERIFIED_DAY_CAP_BASELINE_EQUIV_MILLIS - dayCollected)
                .coerceAtLeast(0L)

        val amount = minOf(
            state.debt.outstandingBaselineEquivMillis,
            ContinuityContract.DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS,
            dayHeadroom,
        )
        if (amount <= 0L) return produced

        // Project every affected reserve *before* applying anything. Checking
        // afterwards would mean the floor had already been crossed.
        val drain = ContinuityMath.scaleByDuration(
            ContinuityContract.FIXTURE_RESERVE_DRAIN_PER_MINUTE,
            amount,
        )
        val atOrBelowFloor = state.reserveA <= ContinuityContract.DEBT_SAFETY_FLOOR ||
            state.reserveB <= ContinuityContract.DEBT_SAFETY_FLOOR
        val wouldCross = state.reserveA - drain < ContinuityContract.DEBT_SAFETY_FLOOR ||
            state.reserveB - drain < ContinuityContract.DEBT_SAFETY_FLOOR
        val insideCollapseMargin =
            state.reserveA - drain < ContinuityContract.DEBT_POST_REVEAL_COLLAPSE_MARGIN ||
                state.reserveB - drain < ContinuityContract.DEBT_POST_REVEAL_COLLAPSE_MARGIN

        if (atOrBelowFloor || wouldCross || insideCollapseMargin) {
            if (state.debt.state != DebtState.PAUSED_LOW_RESERVE) {
                produced += emit(ContinuityEventType.DEBT_PAUSED_LOW_RESERVE)
            }
            return produced
        }

        produced += emit(ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED, a = amount)
        return produced
    }

    // -------------------------------------------------------------- projection

    private fun projection(): List<ContinuityEvent> {
        if (mode != ReconciliationMode.MODE_C) return emptyList()
        if (!evidence.confidence.grantsVerifiedTime) return emptyList()

        val produced = ArrayList<ContinuityEvent>()
        val remaining = evidence.verifiedMillis - ContinuityContract.MODE_B_MAX_MILLIS
        if (remaining > 0L) {
            // Beyond 72 hours the projection advances chronological age and
            // circadian phase only. Passive drift is capped, which is why no
            // metabolism event accompanies this advance.
            produced += emit(ContinuityEventType.VERIFIED_TIME_ADVANCED, a = remaining)
        }
        produced += emit(
            ContinuityEventType.EXTENDED_ABSENCE_RECONCILED,
            a = evidence.verifiedMillis,
        )
        return produced
    }

    // ---------------------------------------------------------------- epilogue

    private fun epilogue(): List<ContinuityEvent> {
        val produced = ArrayList<ContinuityEvent>()

        if (evidence.confidence.grantsVerifiedTime && evidence.verifiedMillis > 0L) {
            val replenishment = BlindCredit.replenish(
                ledger = state.credit,
                verifiedTimeTotalBefore = initialVerifiedTotal,
                verifiedMillis = evidence.verifiedMillis,
            )
            produced += emit(
                ContinuityEventType.BLIND_CREDIT_REPLENISHED,
                a = replenishment.grantedMillis,
                b = replenishment.carriedRemainder,
            )
            produced += emit(
                ContinuityEventType.PASSIVE_DEVELOPMENT_APPLIED,
                a = minOf(
                    evidence.verifiedMillis,
                    ContinuityContract.MODE_C_MAX_PASSIVE_DEVELOPMENT_MILLIS,
                ),
            )
        }

        if (state.debt.outstandingBaselineEquivMillis > 0L &&
            state.verifiedTimeTotalMillis - state.debt.oldestAccrualVerifiedMillis >
            ContinuityContract.DEBT_RETENTION_HORIZON_MILLIS
        ) {
            produced += emit(
                ContinuityEventType.DEBT_FORGIVEN,
                a = state.debt.outstandingBaselineEquivMillis,
            )
        }

        produced += emit(
            ContinuityEventType.ANCHOR_WRITTEN,
            durability = DurabilityClass.WITNESSED,
            anchor = ClockTrust.anchorFor(
                previous = state.anchor,
                observation = observation,
                evidence = evidence,
                logicalTime = state.wallClockAgeMillis,
            ),
        )
        return produced
    }

    public companion object {

        /** Chunk size at an offset measured from the start of the absence. */
        public fun chunkSizeAt(offsetMillis: Long): Long = when {
            offsetMillis < ContinuityContract.CHUNK_TIER_1_END_MILLIS ->
                ContinuityContract.CHUNK_TIER_1_SIZE_MILLIS
            offsetMillis < ContinuityContract.CHUNK_TIER_2_END_MILLIS ->
                ContinuityContract.CHUNK_TIER_2_SIZE_MILLIS
            else -> ContinuityContract.CHUNK_TIER_3_SIZE_MILLIS
        }

        public fun begin(
            state: ContinuityState,
            observation: ClockObservation,
            evidence: ElapsedEvidence,
            ctx: ArithmeticContext,
        ): Reconciliation {
            // Mode is reported from the larger of the two intervals so that a
            // month-long unverifiable gap is still described as extended absence.
            // Chunking, however, is driven strictly by verified time.
            val reportable = maxOf(evidence.verifiedMillis, evidence.unverifiedMillis)
            return Reconciliation(
                observation = observation,
                evidence = evidence,
                mode = ReconciliationMode.forElapsed(reportable),
                initialVerifiedTotal = state.verifiedTimeTotalMillis,
                ctx = ctx,
                state = state,
            )
        }

        /** Convenience: run to completion in one call. */
        public fun runToCompletion(
            state: ContinuityState,
            observation: ClockObservation,
            evidence: ElapsedEvidence,
            ctx: ArithmeticContext,
            sliceChunks: Int = ContinuityContract.RECONCILIATION_SLICE_CHUNKS,
        ): ReconciliationPlan {
            val reconciliation = begin(state, observation, evidence, ctx)
            val events = ArrayList<ContinuityEvent>()
            while (!reconciliation.isComplete) {
                events += reconciliation.advance(sliceChunks)
            }
            return ReconciliationPlan(
                mode = reconciliation.mode,
                events = events,
                chunksApplied = reconciliation.chunksApplied,
                finalState = reconciliation.currentState,
            )
        }
    }
}

/**
 * The canonical interaction gate.
 *
 * Non-canonical by itself, but it enforces a canonical rule: inputs received
 * while reconciliation is pending are **discarded, not queued**. A queued input
 * would be applied against a state the user never saw, which breaks the
 * correspondence between what was on screen and what was intended — and a
 * rejected input must also consume no item, which is why rejection is counted
 * here rather than silently dropped.
 */
public class InteractionGate {
    private var open: Boolean = false
    private var discarded: Int = 0
    private var admitted: Int = 0

    public val isOpen: Boolean get() = open
    public val discardedCount: Int get() = discarded
    public val admittedCount: Int get() = admitted

    public fun close() {
        open = false
    }

    public fun open() {
        open = true
    }

    /** Returns true when the input may reach the reducer. */
    public fun admit(admissionState: DurabilityAdmissionState): Boolean {
        if (!open || !admissionState.admitsMutation) {
            discarded += 1
            return false
        }
        admitted += 1
        return true
    }
}
