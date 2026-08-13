package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unresolved-time debt: the global cap, the safety floor, the hysteresis and the
 * forgiveness horizon.
 *
 * The property being defended is that a long absence can never become a delayed
 * punishment. Every mechanism here exists to make the worst case bounded,
 * visible, and eventually discharged whether or not the user does anything.
 */
class DebtReconciliationTest {

    private val hour = 3_600_000L
    private val day = 86_400_000L

    private fun ctx() = ArithmeticContext.unattributed()

    private fun stateWithDebt(
        outstanding: Long,
        reserve: Long = FixedPoint.ONE,
        debtState: DebtState = DebtState.ACCRUED,
    ): ContinuityState = ContinuityState.genesis(1L, 1L).copy(
        reserveA = reserve,
        reserveB = reserve,
        debt = DebtLedgerState.genesis().copy(
            state = debtState,
            outstandingBaselineEquivMillis = outstanding,
        ),
    )

    @Test
    fun `accrual beyond the global cap is refused by the reducer`() {
        // The planner forgives excess at accrual. If it ever failed to, the
        // reducer refuses rather than quietly retaining an over-cap balance.
        assertFailsWith<IllegalStateException> {
            ContinuityReducer.reduce(
                stateWithDebt(ContinuityContract.DEBT_GLOBAL_CAP_BASELINE_EQUIV_MILLIS),
                ContinuityEvent.of(1L, ContinuityEventType.UNRESOLVED_TIME_DEBT_ACCRUED, a = 1L),
                ctx(),
            )
        }
    }

    @Test
    fun `a chunk may never collect more than the per-chunk cap`() {
        assertFailsWith<IllegalStateException> {
            ContinuityReducer.reduce(
                stateWithDebt(10L * hour),
                ContinuityEvent.of(
                    1L,
                    ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED,
                    a = ContinuityContract.DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS + 1L,
                ),
                ctx(),
            )
        }
    }

    @Test
    fun `a verified day may never collect more than the per-day cap`() {
        var state = stateWithDebt(72L * hour)
        var id = 1L
        var collected = 0L
        // Collect at the per-chunk cap repeatedly inside one verified day.
        while (collected + ContinuityContract.DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS <=
            ContinuityContract.DEBT_PER_VERIFIED_DAY_CAP_BASELINE_EQUIV_MILLIS
        ) {
            state = ContinuityReducer.reduce(
                state,
                ContinuityEvent.of(
                    id++,
                    ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED,
                    a = ContinuityContract.DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS,
                ),
                ctx(),
            )
            collected += ContinuityContract.DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS
        }
        assertEquals(
            ContinuityContract.DEBT_PER_VERIFIED_DAY_CAP_BASELINE_EQUIV_MILLIS,
            state.debt.collectedInDayBaselineEquivMillis,
        )
        assertFailsWith<IllegalStateException> {
            ContinuityReducer.reduce(
                state,
                ContinuityEvent.of(id, ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED, a = 1L),
                ctx(),
            )
        }
    }

    @Test
    fun `the day window rolls on verified time rather than on a timer`() {
        var state = stateWithDebt(72L * hour)
        var id = 1L
        val perChunk = ContinuityContract.DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS
        repeat(
            (ContinuityContract.DEBT_PER_VERIFIED_DAY_CAP_BASELINE_EQUIV_MILLIS / perChunk).toInt(),
        ) {
            state = ContinuityReducer.reduce(
                state,
                ContinuityEvent.of(id++, ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED, a = perChunk),
                ctx(),
            )
        }
        state = ContinuityReducer.reduce(
            state,
            ContinuityEvent.of(id++, ContinuityEventType.VERIFIED_TIME_ADVANCED, a = day),
            ctx(),
        )
        val next = ContinuityReducer.reduce(
            state,
            ContinuityEvent.of(id, ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED, a = 1L),
            ctx(),
        )
        assertEquals(1L, next.debt.collectedInDayBaselineEquivMillis)
    }

    @Test
    fun `collection pauses at the safety floor rather than crossing it`() {
        val atFloor = stateWithDebt(
            outstanding = 24L * hour,
            reserve = ContinuityContract.DEBT_SAFETY_FLOOR,
        )
        val observation = ClockObservation(1_000_000L, 1_000L, true, 1L)
        val anchored = ContinuityReducer.reduce(
            atFloor,
            ContinuityEvent.of(
                1L,
                ContinuityEventType.ANCHOR_WRITTEN,
                anchor = DurableTimeAnchor(
                    1L, 1_000_000L, 1_000L, true, 1L, 0L,
                    TimeConfidence.VERIFIED_MONOTONIC, false, 0L,
                ),
            ),
            ctx(),
        )
        val later = ClockObservation(1_000_000L + 2L * hour, 1_000L + 2L * hour, true, 1L)
        val plan = Reconciliation.runToCompletion(
            anchored,
            later,
            ClockTrust.classify(anchored.anchor, later),
            ctx(),
        )
        assertTrue(plan.events.any { it.type == ContinuityEventType.DEBT_PAUSED_LOW_RESERVE })
        assertEquals(DebtState.PAUSED_LOW_RESERVE, plan.finalState.debt.state)
        // Ordinary verified metabolism continued; only collection paused.
        assertTrue(plan.finalState.reserveA < ContinuityContract.DEBT_SAFETY_FLOOR)
        assertEquals(24L * hour, plan.finalState.debt.outstandingBaselineEquivMillis)
        assertEquals(observation.bootIdentity, 1L)
    }

    @Test
    fun `incremental care does not rearm collection`() {
        var state = stateWithDebt(24L * hour, reserve = 0L, debtState = DebtState.PAUSED_LOW_RESERVE)
        // From nothing to a third of capacity: better, but nowhere near abundance.
        state = ContinuityReducer.reduce(
            state,
            ContinuityEvent.of(
                1L,
                ContinuityEventType.RESERVE_RESTORED,
                a = FixedPoint.of(0, 300_000),
                b = FixedPoint.of(0, 300_000),
            ),
            ctx(),
        )
        assertEquals(DebtState.PAUSED_LOW_RESERVE, state.debt.state)
        assertFalse(state.debt.rearmEffectivePresent)
    }

    @Test
    fun `pausing clears any armed rearm`() {
        // Care that happened before a pause must not pre-authorize collection
        // after it.
        var state = stateWithDebt(24L * hour)
        state = ContinuityReducer.reduce(
            state,
            ContinuityEvent.of(1L, ContinuityEventType.DEBT_REARM_ARMED, a = 500L),
            ctx(),
        )
        assertTrue(state.debt.rearmEffectivePresent)
        state = ContinuityReducer.reduce(
            state,
            ContinuityEvent.of(2L, ContinuityEventType.DEBT_PAUSED_LOW_RESERVE),
            ctx(),
        )
        assertFalse(state.debt.rearmEffectivePresent)
        assertFalse(state.debt.abundanceStablePresent)
    }

    @Test
    fun `rearm requires abundance held for the stability interval`() {
        val anchor = DurableTimeAnchor(
            1L, 1_000_000L, 1_000L, true, 1L, 0L,
            TimeConfidence.VERIFIED_MONOTONIC, false, 0L,
        )
        var state = stateWithDebt(
            outstanding = 24L * hour,
            reserve = FixedPoint.ONE,
            debtState = DebtState.PAUSED_LOW_RESERVE,
        ).copy(anchor = anchor)

        // A short verified absence: abundance is observed but not yet stable.
        val short = ClockObservation(1_000_000L + 10L * 60_000L, 1_000L + 10L * 60_000L, true, 1L)
        val shortPlan = Reconciliation.runToCompletion(
            state, short, ClockTrust.classify(state.anchor, short), ctx(),
        )
        assertTrue(shortPlan.events.none { it.type == ContinuityEventType.DEBT_REARM_ARMED })
        assertTrue(shortPlan.finalState.debt.abundanceStablePresent)

        // A long one: stability satisfied, so rearm is armed for later.
        state = shortPlan.finalState
        val long = ClockObservation(
            1_000_000L + 10L * 60_000L + 3L * hour,
            1_000L + 10L * 60_000L + 3L * hour,
            true,
            1L,
        )
        val longPlan = Reconciliation.runToCompletion(
            state, long, ClockTrust.classify(state.anchor, long), ctx(),
        )
        assertTrue(longPlan.events.any { it.type == ContinuityEventType.DEBT_REARM_ARMED })
    }

    @Test
    fun `rearm becomes effective only after the grace interval`() {
        val armed = stateWithDebt(24L * hour, debtState = DebtState.PAUSED_LOW_RESERVE).copy(
            debt = DebtLedgerState.genesis().copy(
                state = DebtState.PAUSED_LOW_RESERVE,
                outstandingBaselineEquivMillis = 24L * hour,
                abundanceStablePresent = true,
                abundanceStableSinceVerifiedMillis = 0L,
                rearmEffectivePresent = true,
                rearmEffectiveAtVerifiedMillis = ContinuityContract.DEBT_REARM_GRACE_MILLIS,
            ),
        )
        assertTrue(
            armed.debt.rearmEffectiveAtVerifiedMillis > armed.verifiedTimeTotalMillis,
            "rearm must take effect strictly later than the care event that armed it",
        )
    }

    @Test
    fun `debt past the retention horizon is forgiven rather than retained`() {
        val stale = stateWithDebt(24L * hour).copy(
            verifiedTimeTotalMillis = ContinuityContract.DEBT_RETENTION_HORIZON_MILLIS + day,
            debt = DebtLedgerState.genesis().copy(
                state = DebtState.ACCRUED,
                outstandingBaselineEquivMillis = 24L * hour,
                oldestAccrualVerifiedMillis = 0L,
            ),
        )
        val forgiven = ContinuityReducer.reduce(
            stale,
            ContinuityEvent.of(1L, ContinuityEventType.DEBT_FORGIVEN, a = 24L * hour),
            ctx(),
        )
        assertEquals(0L, forgiven.debt.outstandingBaselineEquivMillis)
        assertEquals(24L * hour, forgiven.debt.forgivenBaselineEquivMillis)
        assertEquals(DebtState.FORGIVEN, forgiven.debt.state)
    }

    @Test
    fun `resolved debt grants no development in either direction`() {
        val before = stateWithDebt(24L * hour)
        val after = ContinuityReducer.reduce(
            before,
            ContinuityEvent.of(1L, ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED, a = 600_000L),
            ctx(),
        )
        assertEquals(before.developmentalProgress, after.developmentalProgress)
        assertEquals(before.activeExperienceTicks, after.activeExperienceTicks)
    }

    @Test
    fun `collection never takes a reserve below zero`() {
        var state = stateWithDebt(72L * hour, reserve = FixedPoint.of(0, 300_000))
        var id = 1L
        repeat(50) {
            state = ContinuityReducer.reduce(
                state,
                ContinuityEvent.of(
                    id++,
                    ContinuityEventType.BASELINE_METABOLISM_APPLIED,
                    a = 15L * 60_000L,
                ),
                ctx(),
            )
            assertTrue(state.reserveA >= 0L)
            assertTrue(state.reserveB >= 0L)
        }
    }
}
