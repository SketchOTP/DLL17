package com.animusmachinae.dll17.research.aliveness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneSensoriumContractTest {
    @Test
    fun `motion normalizer emits stabilized ordered transitions and resists jitter`() {
        val normalizer = MotionObservationNormalizer()
        val samples = listOf(
            MotionSample(1_000_000_000L, 40),
            MotionSample(1_100_000_000L, 260),
            MotionSample(1_200_000_000L, 180),
            MotionSample(1_300_000_000L, 260),
            MotionSample(1_400_000_000L, 260),
            MotionSample(1_500_000_000L, 260),
            MotionSample(1_600_000_000L, 1_100),
            MotionSample(1_700_000_000L, 900),
            MotionSample(1_800_000_000L, 1_100),
            MotionSample(1_900_000_000L, 1_100),
            MotionSample(2_000_000_000L, 1_100),
        )
        val observations = samples.mapNotNull(normalizer::normalize)

        assertEquals(2, observations.size)
        val walking = observations[0]
        val running = observations[1]
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
    fun `identical adapter and normalized observations replay all D016-AB state identically`() {
        val samples = listOf(
            MotionSample(1_000_000_000L, 40),
            MotionSample(1_100_000_000L, 400),
            MotionSample(1_200_000_000L, 400),
            MotionSample(1_300_000_000L, 400),
            MotionSample(1_400_000_000L, 1_100),
            MotionSample(1_500_000_000L, 1_100),
            MotionSample(1_600_000_000L, 1_100),
        )
        val normalizerA = MotionObservationNormalizer()
        val normalizerB = MotionObservationNormalizer()
        val observationsA = samples.mapNotNull(normalizerA::normalize)
        val observationsB = samples.mapNotNull(normalizerB::normalize)
        assertEquals(observationsA.map(WorldObservation::signature), observationsB.map(WorldObservation::signature))
        assertEquals(normalizerA.stateSignature(), normalizerB.stateSignature())

        val left = PhoneBodyRuntime(20260819L).replay(observationsA)
        val right = PhoneBodyRuntime(20260819L).replay(observationsB)
        assertEquals(left.map(PhoneBodyStep::replaySignature), right.map(PhoneBodyStep::replaySignature))
    }

    @Test
    fun `low salience observation can be ignored while an intention continues`() {
        val fx = Fx.counting().first
        val agent = OrganismAgent(Cohort.FULL, 11L, fx)
        val habitat = Habitat(11L, HabitatCondition.CONTROLLED_NOVELTY)
        agent.state.committedAction = SpikeAction.IDLE_VARIATION
        agent.state.committedTarget = null
        agent.state.committedTier = 5
        agent.state.commitmentRemaining = 5

        agent.receiveWorldObservation(observation(ActivityBand.STILL, ActivityBand.WALKING, MotionBand.LOW, 100_000), 0L)
        assertEquals(WorldAttentionLevel.IGNORE, agent.state.worldAttentionLevel)
        val choice = agent.decide(habitat, 0L)

        assertEquals(SpikeAction.IDLE_VARIATION, choice.action)
        assertTrue(choice.decision?.commitmentContinuation == true)
        assertFalse(agent.lastProposals.any { it.action == SpikeAction.ORIENT && it.target == null })
    }

    @Test
    fun `relevant observation can produce organism-owned attention`() {
        val fx = Fx.counting().first
        val agent = OrganismAgent(Cohort.FULL, 12L, fx)
        val habitat = Habitat(12L, HabitatCondition.CONTROLLED_NOVELTY)

        agent.receiveWorldObservation(observation(ActivityBand.STILL, ActivityBand.WALKING, MotionBand.MODERATE, 700_000), 0L)
        assertEquals(WorldAttentionLevel.ATTEND, agent.state.worldAttentionLevel)
        val choice = agent.decide(habitat, 0L)

        assertEquals(SpikeAction.ORIENT, choice.action)
        assertEquals(3, choice.tier)
    }

    @Test
    fun `strong observation can interrupt a bounded intention`() {
        val fx = Fx.counting().first
        val agent = OrganismAgent(Cohort.FULL, 13L, fx)
        val habitat = Habitat(13L, HabitatCondition.CONTROLLED_NOVELTY)
        agent.state.committedAction = SpikeAction.IDLE_VARIATION
        agent.state.committedTarget = null
        agent.state.committedTier = 5
        agent.state.commitmentRemaining = 5

        agent.receiveWorldObservation(
            observation(ActivityBand.WALKING, ActivityBand.RUNNING, MotionBand.HIGH, 700_000, significantMotion = true),
            0L,
        )
        assertEquals(WorldAttentionLevel.INTERRUPT, agent.state.worldAttentionLevel)
        val choice = agent.decide(habitat, 0L)

        assertEquals(SpikeAction.ORIENT, choice.action)
        assertEquals(2, choice.tier)
        assertEquals(SpikeAction.IDLE_VARIATION, agent.state.interruptedAction)
    }

    @Test
    fun `world evidence does not guarantee speech`() {
        val step = PhoneBodyRuntime(20260819L).step(
            observation(ActivityBand.STILL, ActivityBand.WALKING, MotionBand.MODERATE, 700_000),
        )

        assertTrue(step.attentionSelected)
        assertNull(step.speechFrame)
        assertNull(step.utterance)
    }

    @Test
    fun `bounded grammar composes materially different semantic frames`() {
        val base = OrganismSpeechFrame(
            act = SpeechAct.QUESTION,
            subject = SpeechSubject.WORLD,
            change = ObservedChange.SPEED_INCREASE,
            activity = ActivityConcept.RUNNING,
            intensity = SpeechIntensity.HIGH,
            question = QuestionConcept.CAUSE,
            affect = SpeechAffect.CURIOUS,
            urgency = SpeechUrgency.LOW,
            knownContext = KnownContext.PHONE_MOVING,
            uncertaintyPpm = 200_000,
            evidenceSequence = 1L,
        )
        val destination = base.copy(
            change = ObservedChange.ACTIVITY_STARTED,
            activity = ActivityConcept.WALKING,
            intensity = SpeechIntensity.MODERATE,
            question = QuestionConcept.DESTINATION,
        )
        val notice = base.copy(
            act = SpeechAct.NOTICE,
            change = ObservedChange.BECAME_STILL,
            activity = ActivityConcept.STILL,
            intensity = SpeechIntensity.LOW,
            question = QuestionConcept.STATE,
            affect = SpeechAffect.UNCERTAIN,
        )

        val outputs = listOf(base, destination, notice).map(ChildlikeUtteranceGenerator::render)
        assertEquals(3, outputs.toSet().size)
        assertEquals("hey... why we going fast?", outputs[0])
        assertEquals("hey... where we going?", outputs[1])
        assertEquals("um... we stopped.", outputs[2])
    }

    private fun observation(
        from: ActivityBand,
        to: ActivityBand,
        motion: MotionBand,
        confidence: Int,
        significantMotion: Boolean = false,
    ): WorldObservation = WorldObservation(
        family = ObservationFamily.MOVEMENT_ACTIVITY,
        activityFrom = from,
        activityTo = to,
        motionBand = motion,
        significantMotion = significantMotion,
        meta = ObservationMeta(
            capturedAtMillis = 1_000L,
            sequence = 0L,
            freshUntilMillis = 4_000L,
            confidencePpm = confidence,
            uncertaintyPpm = 1_000_000 - confidence,
            provenance = ObservationProvenance.REPLAY_FIXTURE,
            permission = PermissionState.NOT_APPLICABLE,
            capability = CapabilityState.AVAILABLE,
            sensoriumMode = SensoriumMode.FOREGROUND_SENSORIUM,
        ),
    )
}
