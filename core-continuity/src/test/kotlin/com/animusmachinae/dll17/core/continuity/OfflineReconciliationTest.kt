package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mode selection, chunking, slicing and the interaction gate.
 *
 * The load-bearing claim is that reconciliation is a *value*: the same inputs
 * produce the same event sequence regardless of how the work was divided, how
 * often it yielded, or whether it was interrupted.
 */
class OfflineReconciliationTest {

    private val minute = 60_000L
    private val hour = 3_600_000L
    private val day = 86_400_000L

    private fun ctx() = ArithmeticContext.unattributed()

    private companion object {
        /** 6 h of one-minute chunks + 18 h of five-minute + 48 h of fifteen-minute. */
        const val MAX_CHUNKS = 360 + 216 + 192
    }

    private fun anchored(): ContinuityState {
        val state = ContinuityState.genesis(1L, 1L)
        return ContinuityReducer.reduce(
            state,
            ContinuityEvent.of(
                1L,
                ContinuityEventType.ANCHOR_WRITTEN,
                anchor = DurableTimeAnchor(
                    anchorSequence = 1L,
                    wallClockUtcMillis = 1_000_000L,
                    elapsedRealtimeMillis = 1_000L,
                    bootIdentityPresent = true,
                    bootIdentity = 3L,
                    logicalTime = 0L,
                    timeConfidence = TimeConfidence.VERIFIED_MONOTONIC,
                    authenticatedTimePresent = false,
                    authenticatedTimeMillis = 0L,
                ),
            ),
            ctx(),
        )
    }

    private fun after(millis: Long) = ClockObservation(
        wallClockUtcMillis = 1_000_000L + millis,
        elapsedRealtimeMillis = 1_000L + millis,
        bootIdentityPresent = true,
        bootIdentity = 3L,
    )

    private fun reconcile(state: ContinuityState, millis: Long): ReconciliationPlan {
        val observation = after(millis)
        val evidence = ClockTrust.classify(state.anchor, observation)
        return Reconciliation.runToCompletion(state, observation, evidence, ctx())
    }

    @Test
    fun `mode selection is exact at every boundary`() {
        assertEquals(ReconciliationMode.MODE_A, ReconciliationMode.forElapsed(0L))
        assertEquals(
            ReconciliationMode.MODE_A,
            ReconciliationMode.forElapsed(ContinuityContract.MODE_A_MAX_MILLIS),
        )
        assertEquals(
            ReconciliationMode.MODE_B,
            ReconciliationMode.forElapsed(ContinuityContract.MODE_A_MAX_MILLIS + 1L),
        )
        assertEquals(
            ReconciliationMode.MODE_B,
            ReconciliationMode.forElapsed(ContinuityContract.MODE_B_MAX_MILLIS),
        )
        assertEquals(
            ReconciliationMode.MODE_C,
            ReconciliationMode.forElapsed(ContinuityContract.MODE_B_MAX_MILLIS + 1L),
        )
    }

    @Test
    fun `chunk size is a function of the offset from the start of the absence`() {
        assertEquals(minute, Reconciliation.chunkSizeAt(0L))
        assertEquals(minute, Reconciliation.chunkSizeAt(6L * hour - 1L))
        assertEquals(5L * minute, Reconciliation.chunkSizeAt(6L * hour))
        assertEquals(5L * minute, Reconciliation.chunkSizeAt(24L * hour - 1L))
        assertEquals(15L * minute, Reconciliation.chunkSizeAt(24L * hour))
        assertEquals(15L * minute, Reconciliation.chunkSizeAt(100L * day))
    }

    @Test
    fun `a final partial chunk carries the remainder and is never rounded up`() {
        val plan = reconcile(anchored(), 90_000L)
        val advances = plan.events.filter {
            it.type == ContinuityEventType.VERIFIED_TIME_ADVANCED
        }
        assertEquals(90_000L, advances.sumOf { it.operandA })
        assertEquals(listOf(60_000L, 30_000L), advances.map { it.operandA })
    }

    @Test
    fun `slicing at every granularity produces the identical sequence`() {
        val base = anchored()
        val observation = after(9L * hour)
        val evidence = ClockTrust.classify(base.anchor, observation)
        val reference = Reconciliation.runToCompletion(base, observation, evidence, ctx())

        for (slice in intArrayOf(1, 2, 7, 64, 10_000)) {
            val run = Reconciliation.begin(base, observation, evidence, ctx())
            val events = ArrayList<ContinuityEvent>()
            while (!run.isComplete) events += run.advance(slice)
            assertEquals(reference.events.size, events.size, "slice=$slice changed the event count")
            for (index in events.indices) {
                assertTrue(
                    reference.events[index].canonicalPayloadBytes()
                        .contentEquals(events[index].canonicalPayloadBytes()),
                    "slice=$slice diverged at event $index",
                )
            }
            assertEquals(reference.finalState.stateHashHex(), run.currentState.stateHashHex())
        }
    }

    @Test
    fun `mode C caps passive development and projects the remainder`() {
        val plan = reconcile(anchored(), 200L * day)
        assertEquals(ReconciliationMode.MODE_C, plan.mode)
        val passive = plan.events.single {
            it.type == ContinuityEventType.PASSIVE_DEVELOPMENT_APPLIED
        }
        assertEquals(ContinuityContract.MODE_C_MAX_PASSIVE_DEVELOPMENT_MILLIS, passive.operandA)
        assertTrue(plan.events.any { it.type == ContinuityEventType.EXTENDED_ABSENCE_RECONCILED })
        assertEquals(200L * day, plan.finalState.wallClockAgeMillis)
    }

    @Test
    fun `a year of absence does not drain more than the bounded window`() {
        val oneYear = reconcile(anchored(), 365L * day)
        val seventyTwoHours = reconcile(anchored(), 72L * hour)
        // Beyond the bounded window, drift is capped: a year and three days
        // arrive at the same reserve.
        assertEquals(seventyTwoHours.finalState.reserveA, oneYear.finalState.reserveA)
    }

    @Test
    fun `mode A never collects debt`() {
        var state = anchored().copy(
            debt = DebtLedgerState.genesis().copy(
                state = DebtState.ACCRUED,
                outstandingBaselineEquivMillis = 10L * hour,
            ),
        )
        val plan = reconcile(state, 4L * 60_000L)
        assertEquals(ReconciliationMode.MODE_A, plan.mode)
        assertTrue(
            plan.events.none { it.type == ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED },
            "foreground-adjacent cadence must always run at baseline",
        )
        state = plan.finalState
        assertEquals(10L * hour, state.debt.outstandingBaselineEquivMillis)
    }

    @Test
    fun `returning to the foreground never increases biological burn`() {
        // Two paths over the same elapsed time: one reconciled offline with debt
        // outstanding, one with no debt at all. The offline path may collect
        // bounded debt; the foreground path may not, so the foreground reserve
        // can never be lower than the offline one for the same interval.
        val withDebt = anchored().copy(
            debt = DebtLedgerState.genesis().copy(
                state = DebtState.ACCRUED,
                outstandingBaselineEquivMillis = 6L * hour,
            ),
        )
        val foreground = reconcile(withDebt, 3L * 60_000L)
        val baseline = reconcile(anchored(), 3L * 60_000L)
        assertEquals(baseline.finalState.reserveA, foreground.finalState.reserveA)
    }

    @Test
    fun `a closed gate discards inputs and consumes nothing`() {
        val gate = InteractionGate()
        gate.close()
        repeat(5) { assertFalse(gate.admit(DurabilityAdmissionState.OPEN)) }
        assertEquals(5, gate.discardedCount)
        assertEquals(0, gate.admittedCount)

        gate.open()
        assertTrue(gate.admit(DurabilityAdmissionState.OPEN))
        // Even an open gate refuses when durability cannot admit the mutation.
        assertFalse(gate.admit(DurabilityAdmissionState.READ_ONLY_SURVIVAL))
        assertFalse(gate.admit(DurabilityAdmissionState.STORAGE_FAULT))
        assertEquals(1, gate.admittedCount)
    }

    @Test
    fun `reserves never leave their bounds across a long reconciliation`() {
        val plan = reconcile(anchored(), 72L * hour)
        assertTrue(plan.finalState.reserveA in 0L..FixedPoint.ONE)
        assertTrue(plan.finalState.reserveB in 0L..FixedPoint.ONE)
    }

    @Test
    fun `the canonical absence ladder produces valid bounded deterministic state`() {
        // The charter's R002 exit gate names exactly these six durations. Each
        // must produce a state that is valid (inside every bound), bounded (the
        // chunk count does not grow without limit) and deterministic (two runs
        // agree byte for byte).
        val ladder = linkedMapOf(
            "5 minutes" to 5L * 60_000L,
            "6 hours" to 6L * hour,
            "72 hours" to 72L * hour,
            "30 days" to 30L * day,
            "6 months" to 182L * day,
            "1 year" to 365L * day,
        )
        var previousChunks = 0
        for ((label, elapsed) in ladder) {
            val first = reconcile(anchored(), elapsed)
            val second = reconcile(anchored(), elapsed)

            assertEquals(first.finalState.stateHashHex(), second.finalState.stateHashHex(), label)
            assertTrue(first.finalState.reserveA in 0L..FixedPoint.ONE, label)
            assertTrue(first.finalState.reserveB in 0L..FixedPoint.ONE, label)
            assertTrue(first.finalState.circadianPhase in 0L until FixedPoint.ONE, label)
            assertEquals(elapsed, first.finalState.wallClockAgeMillis, label)
            assertEquals(elapsed, first.finalState.verifiedTimeTotalMillis, label)
            // Bounded: past the 72-hour window the chunk count stops growing, so
            // a one-year absence costs no more work than a three-day one.
            assertTrue(first.chunksApplied >= previousChunks, label)
            assertTrue(first.chunksApplied <= MAX_CHUNKS, "$label produced ${first.chunksApplied} chunks")
            previousChunks = minOf(first.chunksApplied, MAX_CHUNKS)
        }
    }

    @Test
    fun `runtime past the bounded window does not grow with the absence`() {
        // The charter requires long-absence runtime to stay bounded without any
        // scheduled Android work. Chunk count is the proxy for that work.
        val threeDays = reconcile(anchored(), 72L * hour)
        val oneYear = reconcile(anchored(), 365L * day)
        assertEquals(threeDays.chunksApplied, oneYear.chunksApplied)
    }

    @Test
    fun `scaling refuses a duration that could overflow rather than saturating`() {
        // Saturation is for canonical arithmetic. An argument this large is a
        // caller defect, and clamping it would hide the defect.
        val failure = runCatching {
            ContinuityMath.scaleByDuration(1_000L, ContinuityMath.MAX_SCALABLE_MILLIS + 1L)
        }
        assertTrue(failure.isFailure)
    }
}
