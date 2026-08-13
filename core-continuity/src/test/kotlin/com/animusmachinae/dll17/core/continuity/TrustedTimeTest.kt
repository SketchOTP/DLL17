package com.animusmachinae.dll17.core.continuity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The four clocks, durable anchors, time confidence and blind-decay credit.
 *
 * Every test here is an exploit rehearsal. The question is never "does the
 * arithmetic work" but "can a user with full control of the device wall clock,
 * the reboot button and the install button manufacture biology".
 */
class TrustedTimeTest {

    private val hour = 3_600_000L
    private val day = 86_400_000L

    private fun anchor(
        wall: Long = 1_000_000_000_000L,
        elapsed: Long = 500_000L,
        boot: Long = 7L,
        bootPresent: Boolean = true,
    ) = DurableTimeAnchor(
        anchorSequence = 1L,
        wallClockUtcMillis = wall,
        elapsedRealtimeMillis = elapsed,
        bootIdentityPresent = bootPresent,
        bootIdentity = boot,
        logicalTime = 0L,
        timeConfidence = TimeConfidence.VERIFIED_MONOTONIC,
        authenticatedTimePresent = false,
        authenticatedTimeMillis = 0L,
    )

    @Test
    fun `same boot with agreeing clocks is verified monotonic`() {
        val evidence = ClockTrust.classify(
            anchor(),
            ClockObservation(1_000_000_000_000L + hour, 500_000L + hour, true, 7L),
        )
        assertEquals(TimeConfidence.VERIFIED_MONOTONIC, evidence.confidence)
        assertEquals(hour, evidence.verifiedMillis)
        assertEquals(0L, evidence.unverifiedMillis)
    }

    @Test
    fun `a boot identity change is never verified time`() {
        val evidence = ClockTrust.classify(
            anchor(),
            ClockObservation(1_000_000_000_000L + 30L * day, 1_000L, true, 8L),
        )
        assertEquals(TimeConfidence.UNVERIFIED_REBOOT, evidence.confidence)
        assertEquals(0L, evidence.verifiedMillis)
        assertTrue(evidence.bootChanged)
    }

    @Test
    fun `uptime moving backwards without a boot change is still unverifiable`() {
        // A device that reports lower uptime under the same boot identity is
        // either lying or broken. Either way the interval is not evidence.
        val evidence = ClockTrust.classify(
            anchor(elapsed = 10_000_000L),
            ClockObservation(1_000_000_000_000L + hour, 5_000L, true, 7L),
        )
        assertEquals(TimeConfidence.UNVERIFIED_REBOOT, evidence.confidence)
        assertEquals(0L, evidence.verifiedMillis)
    }

    @Test
    fun `backward wall movement never produces a negative interval`() {
        val evidence = ClockTrust.classify(
            anchor(),
            ClockObservation(1_000_000_000_000L - 10L * hour, 1_000L, true, 8L),
        )
        assertEquals(0L, evidence.verifiedMillis)
        assertEquals(0L, evidence.unverifiedMillis)
    }

    @Test
    fun `skew inside tolerance is not an anomaly and skew outside it is`() {
        val inside = ClockTrust.classify(
            anchor(),
            ClockObservation(
                1_000_000_000_000L + hour + ContinuityContract.WALL_ELAPSED_SKEW_TOLERANCE_MILLIS,
                500_000L + hour,
                true,
                7L,
            ),
        )
        assertFalse(inside.anomaly)

        val outside = ClockTrust.classify(
            anchor(),
            ClockObservation(
                1_000_000_000_000L + hour +
                    ContinuityContract.WALL_ELAPSED_SKEW_TOLERANCE_MILLIS + 1L,
                500_000L + hour,
                true,
                7L,
            ),
        )
        assertTrue(outside.anomaly)
        assertEquals(TimeConfidence.ANOMALOUS, outside.confidence)
        assertEquals(0L, outside.verifiedMillis)
    }

    @Test
    fun `authenticated time outranks a reboot`() {
        val stored = anchor().copy(authenticatedTimePresent = true, authenticatedTimeMillis = 1_000L)
        val evidence = ClockTrust.classify(
            stored,
            ClockObservation(
                wallClockUtcMillis = 0L,
                elapsedRealtimeMillis = 0L,
                bootIdentityPresent = true,
                bootIdentity = 99L,
                authenticatedTimePresent = true,
                authenticatedTimeMillis = 1_000L + 6L * hour,
            ),
        )
        assertEquals(TimeConfidence.AUTHENTICATED, evidence.confidence)
        assertEquals(6L * hour, evidence.verifiedMillis)
    }

    @Test
    fun `credit replenishes one millisecond per six of verified time`() {
        val result = BlindCredit.replenish(BlindCreditLedger.genesis(), 0L, 6L * hour)
        assertEquals(hour, result.grantedMillis)
        assertEquals(0L, result.carriedRemainder)
    }

    @Test
    fun `fragmenting a session cannot change the credit it earns`() {
        // The exploit: split one interval into many so that truncation either
        // loses or gains a millisecond every time. The carried remainder makes
        // the grant a pure function of the total.
        var fragmented = BlindCreditLedger.genesis()
        var granted = 0L
        repeat(600) {
            val step = BlindCredit.replenish(fragmented, 0L, 1L)
            granted += step.grantedMillis
            fragmented = fragmented.copy(
                carriedRemainder = step.carriedRemainder,
                grantedInWindowMillis = fragmented.grantedInWindowMillis + step.grantedMillis,
            )
        }
        val contiguous = BlindCredit.replenish(BlindCreditLedger.genesis(), 0L, 600L)
        assertEquals(contiguous.grantedMillis, granted)
    }

    @Test
    fun `credit never exceeds the standing maximum`() {
        var ledger = BlindCreditLedger.genesis()
        val ctx = com.animusmachinae.dll17.core.math.ArithmeticContext.unattributed()
        var state = ContinuityState.genesis(1L, 1L)
        repeat(40) {
            val result = BlindCredit.replenish(ledger, 0L, 24L * hour)
            state = ContinuityReducer.reduce(
                state,
                ContinuityEvent.of(
                    state.lastCommitSequence + 1L,
                    ContinuityEventType.BLIND_CREDIT_REPLENISHED,
                    a = result.grantedMillis,
                    b = result.carriedRemainder,
                ),
                ctx,
            )
            ledger = state.credit
        }
        assertEquals(ContinuityContract.BLIND_DECAY_CREDIT_MAX_MILLIS, ledger.availableMillis)
    }

    @Test
    fun `wall clock movement and reboots replenish nothing`() {
        // Replenishment takes verified milliseconds. There is no code path that
        // hands it a wall-clock delta, and this test exists so that adding one
        // would have to delete an assertion rather than just compile.
        val ledger = BlindCreditLedger.genesis()
        assertEquals(0L, BlindCredit.replenish(ledger, 0L, 0L).grantedMillis)
    }

    @Test
    fun `a boot that already spent its credit cannot spend it again`() {
        val ledger = BlindCreditLedger.genesis().copy(
            availableMillis = 4L * hour,
            consumedForBootPresent = true,
            consumedForBoot = 11L,
        )
        assertEquals(0L, BlindCredit.consumable(ledger, hour, 11L, bootVelocityAnomaly = false))
        assertEquals(hour, BlindCredit.consumable(ledger, hour, 12L, bootVelocityAnomaly = false))
    }

    @Test
    fun `a velocity anomaly stops all further blind progression`() {
        val ledger = BlindCreditLedger.genesis().copy(availableMillis = 4L * hour)
        assertEquals(0L, BlindCredit.consumable(ledger, hour, 12L, bootVelocityAnomaly = true))
    }

    @Test
    fun `blind consumption advances chronology but never verified time`() {
        val ctx = com.animusmachinae.dll17.core.math.ArithmeticContext.unattributed()
        val state = ContinuityState.genesis(1L, 1L).copy(
            credit = BlindCreditLedger.genesis().copy(availableMillis = 4L * hour),
        )
        val next = ContinuityReducer.reduce(
            state,
            ContinuityEvent.of(
                1L,
                ContinuityEventType.BLIND_CREDIT_CONSUMED,
                a = 2L * hour,
                b = 5L,
            ),
            ctx,
        )
        assertEquals(2L * hour, next.wallClockAgeMillis)
        // If blind time fed the verified counter, a reboot loop could earn the
        // verified time needed to buy more blind time.
        assertEquals(0L, next.verifiedTimeTotalMillis)
        assertEquals(2L * hour, next.credit.availableMillis)
        assertEquals(0L, next.developmentalProgress)
    }

    @Test
    fun `the four clocks advance independently`() {
        val ctx = com.animusmachinae.dll17.core.math.ArithmeticContext.unattributed()
        val state = ContinuityState.genesis(1L, 1L)
        val advanced = ContinuityReducer.reduce(
            state,
            ContinuityEvent.of(1L, ContinuityEventType.VERIFIED_TIME_ADVANCED, a = 12L * hour),
            ctx,
        )
        assertEquals(12L * hour, advanced.wallClockAgeMillis)
        assertEquals(12L * hour, advanced.verifiedTimeTotalMillis)
        assertTrue(advanced.circadianPhase > 0L)
        // Verified absence advances chronology and phase. It does not advance
        // active experience, and it does not by itself advance development.
        assertEquals(0L, advanced.activeExperienceTicks)
        assertEquals(0L, advanced.developmentalProgress)
    }

    @Test
    fun `circadian phase always stays inside its period`() {
        val ctx = com.animusmachinae.dll17.core.math.ArithmeticContext.unattributed()
        var state = ContinuityState.genesis(1L, 1L)
        for (step in 1..50) {
            state = ContinuityReducer.reduce(
                state,
                ContinuityEvent.of(
                    step.toLong(),
                    ContinuityEventType.VERIFIED_TIME_ADVANCED,
                    a = 7L * hour + step,
                ),
                ctx,
            )
            assertTrue(state.circadianPhase >= 0L)
            assertTrue(state.circadianPhase < com.animusmachinae.dll17.core.math.FixedPoint.ONE)
        }
    }

    @Test
    fun `an anchor round trips through canonical bytes`() {
        val original = anchor().copy(
            authenticatedTimePresent = true,
            authenticatedTimeMillis = 999L,
            timeConfidence = TimeConfidence.ANOMALOUS,
        )
        val reader = com.animusmachinae.dll17.core.crypto.CanonicalReader(
            com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
                .unwrap(original.canonicalBytes()).payload,
        )
        val decoded = DurableTimeAnchor.readFrom(reader)
        assertTrue(original.canonicalBytes().contentEquals(decoded.canonicalBytes()))
    }

    @Test
    fun `an absent boot identity is not encoded as zero`() {
        // "No boot identity" and "boot identity zero" must not produce the same
        // bytes, or a device that cannot report one looks like a device that
        // reported the first boot ever.
        val absent = anchor(bootPresent = false, boot = 0L)
        val presentZero = anchor(bootPresent = true, boot = 0L)
        assertFalse(absent.canonicalBytes().contentEquals(presentZero.canonicalBytes()))
    }
}
