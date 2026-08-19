package com.animusmachinae.dll17.research.aliveness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class D016ACContextTest {
    private val morning = TrustedTimeObservation.fromLocal(
        hourOfDay = 8,
        isWeekend = false,
        trust = TimeTrustClass.VERIFIED_MONOTONIC,
        confidencePpm = 900_000,
    )
    private val evening = TrustedTimeObservation.fromLocal(
        hourOfDay = 20,
        isWeekend = false,
        trust = TimeTrustClass.VERIFIED_MONOTONIC,
        confidencePpm = 900_000,
    )

    @Test
    fun `repeated place and morning history becomes expected`() {
        val memory = BoundedRoutineContextMemory()
        val place = CoarsePlaceObservation(CoarsePlaceNormalizer.fixture(1))

        repeat(3) { sequence -> memory.observe(place, morning, sequence.toLong()) }
        val result = memory.observe(place, morning, 3L)

        assertEquals(ContextInterpretation.EXPECTED_CONTEXT, result.interpretation)
        assertEquals(3, result.matchingRoutineCount)
        assertTrue(result.familiarityPpm > 0)
    }

    @Test
    fun `same current observation is novel without lived history`() {
        val memory = BoundedRoutineContextMemory()
        val result = memory.observe(
            CoarsePlaceObservation(CoarsePlaceNormalizer.fixture(1)),
            morning,
            0L,
        )

        assertEquals(ContextInterpretation.NOVEL_CONTEXT, result.interpretation)
        assertEquals(0, result.placeVisitCount)
    }

    @Test
    fun `familiar place at unusual time is distinct from novel`() {
        val memory = BoundedRoutineContextMemory()
        val place = CoarsePlaceObservation(CoarsePlaceNormalizer.fixture(1))
        repeat(3) { sequence -> memory.observe(place, morning, sequence.toLong()) }

        val result = memory.observe(place, evening, 3L)

        assertEquals(ContextInterpretation.FAMILIAR_BUT_UNUSUAL, result.interpretation)
        assertTrue(result.placeVisitCount >= 3)
        assertEquals(0, result.matchingRoutineCount)
    }

    @Test
    fun `permission denied degrades to unknown without learning`() {
        val memory = BoundedRoutineContextMemory()
        val result = memory.observe(
            CoarsePlaceObservation(CoarsePlaceIdentity.UNKNOWN),
            morning,
            0L,
        )

        assertEquals(ContextInterpretation.UNKNOWN_CONTEXT, result.interpretation)
        assertEquals(0, memory.entryCount())
    }

    @Test
    fun `coarse place identity is stable and contains no raw coordinates`() {
        val first = CoarsePlaceNormalizer.fromCoordinates(40.7128, -74.0060)
        val second = CoarsePlaceNormalizer.fromCoordinates(40.7128, -74.0060)
        val observation = CoarsePlaceObservation(first)

        assertEquals(first, second)
        assertTrue(first.value.startsWith("PLACE_"))
        assertTrue(!observation.signature().contains("40.7128"))
        assertTrue(!observation.signature().contains("-74.006"))
    }

    @Test
    fun `context memory is bounded and decays`() {
        val memory = BoundedRoutineContextMemory()
        repeat(100) { index ->
            memory.observe(
                CoarsePlaceObservation(CoarsePlaceNormalizer.fixture(index + 1)),
                morning,
                index.toLong(),
            )
        }

        assertTrue(memory.entryCount() <= BoundedRoutineContextMemory.MAX_ENTRIES)
        assertTrue(memory.maxStoredVisits() <= BoundedRoutineContextMemory.MAX_VISITS)
    }

    @Test
    fun `identical context histories replay identically`() {
        val place = CoarsePlaceObservation(CoarsePlaceNormalizer.fixture(4))
        val observations = buildList {
            add(timeObservation(morning, 0L))
            add(locationObservation(place, morning, 1L))
            add(locationObservation(place, morning, 2L))
            add(locationObservation(place, evening, 3L))
        }

        val left = PhoneBodyRuntime(20260819L).replay(observations)
        val right = PhoneBodyRuntime(20260819L).replay(observations)

        assertEquals(left.map(PhoneBodyStep::replaySignature), right.map(PhoneBodyStep::replaySignature))
        assertEquals(left.last().stateSignature, right.last().stateSignature)
    }

    @Test
    fun `same observation has different meaning under different histories`() {
        val place = CoarsePlaceObservation(CoarsePlaceNormalizer.fixture(7))
        val current = locationObservation(place, morning, 10L)
        val noHistory = PhoneBodyRuntime(1L).step(current)

        val withHistoryRuntime = PhoneBodyRuntime(1L)
        withHistoryRuntime.step(locationObservation(place, morning, 1L))
        withHistoryRuntime.step(locationObservation(place, morning, 2L))
        withHistoryRuntime.step(locationObservation(place, morning, 3L))
        val withHistory = withHistoryRuntime.step(current)

        assertNotEquals(
            noHistory.observation?.context?.interpretation,
            withHistory.observation?.context?.interpretation,
        )
        assertEquals(ContextInterpretation.NOVEL_CONTEXT, noHistory.observation?.context?.interpretation)
        assertEquals(ContextInterpretation.EXPECTED_CONTEXT, withHistory.observation?.context?.interpretation)
    }

    @Test
    fun `fresh context persists onto identical movement and changes salience`() {
        val place = CoarsePlaceObservation(CoarsePlaceNormalizer.fixture(8))
        val movement = movementObservation(4L)

        val expectedRuntime = PhoneBodyRuntime(11L)
        repeat(3) { sequence -> expectedRuntime.step(locationObservation(place, morning, sequence.toLong())) }
        val expectedPlace = expectedRuntime.step(locationObservation(place, morning, 3L))
        val expectedMovement = expectedRuntime.step(movement)

        val unusualRuntime = PhoneBodyRuntime(11L)
        repeat(3) { sequence -> unusualRuntime.step(locationObservation(place, morning, sequence.toLong())) }
        val unusualPlace = unusualRuntime.step(locationObservation(place, evening, 3L))
        val unusualMovement = unusualRuntime.step(movement)

        assertEquals(ContextInterpretation.EXPECTED_CONTEXT, expectedPlace.observation?.context?.interpretation)
        assertEquals(ContextInterpretation.EXPECTED_CONTEXT, expectedMovement.observation?.context?.interpretation)
        assertEquals(ContextInterpretation.FAMILIAR_BUT_UNUSUAL, unusualPlace.observation?.context?.interpretation)
        assertEquals(ContextInterpretation.FAMILIAR_BUT_UNUSUAL, unusualMovement.observation?.context?.interpretation)
        assertEquals(movement.signature(), expectedMovement.observation?.copy(context = null)?.signature())
        assertEquals(movement.signature(), unusualMovement.observation?.copy(context = null)?.signature())
        assertTrue(unusualMovement.worldObservationSalience > expectedMovement.worldObservationSalience)
        assertNotEquals(expectedMovement.worldObservationSalience, unusualMovement.worldObservationSalience)
    }

    @Test
    fun `current context expires before a later movement`() {
        val place = CoarsePlaceObservation(CoarsePlaceNormalizer.fixture(9))
        val runtime = PhoneBodyRuntime(12L)
        runtime.step(locationObservation(place, morning, 0L))

        val expired = runtime.step(movementObservation(PhoneBodyRuntime.CURRENT_CONTEXT_TTL_SEQUENCES))

        assertEquals(null, expired.observation?.context)
    }

    @Test
    fun `uncertain time never trains routine expectations`() {
        val place = CoarsePlaceObservation(CoarsePlaceNormalizer.fixture(10))
        val memory = BoundedRoutineContextMemory()
        val anomalous = morning.copy(trust = TimeTrustClass.ANOMALOUS)
        val reboot = morning.copy(trust = TimeTrustClass.UNVERIFIED_REBOOT)

        repeat(4) { sequence -> memory.observe(place, anomalous, sequence.toLong()) }
        repeat(4) { sequence -> memory.observe(place, reboot, (sequence + 4).toLong()) }

        assertEquals(0, memory.entryCount())
        assertEquals(false, memory.observe(place, anomalous, 8L).learningEligible)
        assertEquals(false, memory.observe(place, reboot, 9L).learningEligible)
    }

    private fun timeObservation(time: TrustedTimeObservation, sequence: Long): WorldObservation =
        WorldObservation(
            family = ObservationFamily.TRUSTED_TIME,
            trustedTime = time,
            meta = meta(sequence),
        )

    private fun locationObservation(
        place: CoarsePlaceObservation,
        time: TrustedTimeObservation,
        sequence: Long,
    ): WorldObservation = WorldObservation(
        family = ObservationFamily.LOCATION_PLACE,
        trustedTime = time,
        place = place,
        meta = meta(sequence),
    )

    private fun movementObservation(sequence: Long): WorldObservation = WorldObservation(
        family = ObservationFamily.MOVEMENT_ACTIVITY,
        activityFrom = ActivityBand.WALKING,
        activityTo = ActivityBand.RUNNING,
        motionBand = MotionBand.LOW,
        significantMotion = false,
        meta = meta(sequence).copy(confidencePpm = 600_000),
    )

    private fun meta(sequence: Long): ObservationMeta = ObservationMeta(
        capturedAtMillis = sequence * 1_000L,
        sequence = sequence,
        freshUntilMillis = sequence * 1_000L + 30_000L,
        confidencePpm = 900_000,
        uncertaintyPpm = 100_000,
        provenance = ObservationProvenance.REPLAY_FIXTURE,
        permission = PermissionState.GRANTED,
        capability = CapabilityState.AVAILABLE,
        sensoriumMode = SensoriumMode.FOREGROUND_SENSORIUM,
    )
}
