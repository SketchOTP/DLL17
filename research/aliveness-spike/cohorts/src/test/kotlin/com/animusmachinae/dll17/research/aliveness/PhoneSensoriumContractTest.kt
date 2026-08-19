package com.animusmachinae.dll17.research.aliveness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PhoneSensoriumContractTest {
    @Test
    fun `motion normalizer emits only quantized ordered transitions`() {
        val normalizer = MotionObservationNormalizer()

        assertEquals(null, normalizer.normalize(MotionSample(1_000_000_000L, 40)))
        val walking = assertNotNull(normalizer.normalize(MotionSample(1_200_000_000L, 400)))
        val running = assertNotNull(normalizer.normalize(MotionSample(1_400_000_000L, 1_100)))

        assertEquals(ActivityBand.STILL, walking.activityFrom)
        assertEquals(ActivityBand.WALKING, walking.activityTo)
        assertEquals(ActivityBand.WALKING, running.activityFrom)
        assertEquals(ActivityBand.RUNNING, running.activityTo)
        assertEquals(0L, walking.meta.sequence)
        assertEquals(1L, running.meta.sequence)
        assertTrue(walking.meta.uncertaintyPpm > 0)
        assertEquals(ObservationProvenance.ANDROID_SENSOR_MANAGER, running.meta.provenance)
    }

    @Test
    fun `identical normalized observations and prior state replay identically`() {
        val normalizerA = MotionObservationNormalizer()
        val normalizerB = MotionObservationNormalizer()
        val samples = listOf(
            MotionSample(1_000_000_000L, 40),
            MotionSample(1_200_000_000L, 400),
            MotionSample(1_400_000_000L, 1_100),
        )
        val observationsA = samples.mapNotNull(normalizerA::normalize)
        val observationsB = samples.mapNotNull(normalizerB::normalize)
        assertEquals(observationsA.map(WorldObservation::signature), observationsB.map(WorldObservation::signature))

        val left = PhoneBodyRuntime(20260819L).replay(observationsA)
        val right = PhoneBodyRuntime(20260819L).replay(observationsB)
        assertEquals(
            left.map { it.record.choice.action to it.record.choice.target },
            right.map { it.record.choice.action to it.record.choice.target },
        )
        assertEquals(left.map { it.stateSignature }, right.map { it.stateSignature })
        assertEquals(left.map { it.utterance }, right.map { it.utterance })
        assertTrue(left.any { it.attentionSelected })
    }

    @Test
    fun `speech is generated from a semantic frame and not a direct sensor sentence`() {
        val observation = MotionObservationNormalizer().normalize(MotionSample(1_400_000_000L, 1_100))
            ?: error("expected a movement transition")
        val step = PhoneBodyRuntime(20260819L).step(observation)

        assertTrue(step.attentionSelected)
        val frame = assertNotNull(step.speechFrame)
        assertEquals(SpeechAct.QUESTION, frame.act)
        assertEquals(SpeechTopic.UNUSUAL_MOVEMENT, frame.topic)
        assertEquals(observation.meta.sequence, frame.evidenceSequence)
        assertNotNull(step.utterance)
        assertTrue(step.utterance!!.contains("moving"))
    }
}
