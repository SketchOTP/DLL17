package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

public enum class SpeechAct { QUESTION, NOTICE }

public enum class SpeechSubject { OWNER, WORLD }

public enum class SpeechTopic { UNUSUAL_MOVEMENT, NEW_CONTEXT, CAPABILITY_LOST }

public enum class SpeechAffect { CURIOUS, ALERT, UNCERTAIN }

public enum class SpeechUrgency { LOW, MEDIUM }

/** Semantic output of the organism, before language rendering. */
public data class OrganismSpeechFrame(
    public val act: SpeechAct,
    public val subject: SpeechSubject,
    public val topic: SpeechTopic,
    public val affect: SpeechAffect,
    public val urgency: SpeechUrgency,
    public val evidenceSequence: Long,
)

/** Constrained local language output. It is not an organism controller. */
public object ChildlikeUtteranceGenerator {
    public fun render(frame: OrganismSpeechFrame): String {
        val opener = when (frame.affect) {
            SpeechAffect.CURIOUS -> "hey"
            SpeechAffect.ALERT -> "whoa"
            SpeechAffect.UNCERTAIN -> "um"
        }
        val question = when (frame.topic) {
            SpeechTopic.UNUSUAL_MOVEMENT -> when (frame.act) {
                SpeechAct.QUESTION -> "why are we moving"
                SpeechAct.NOTICE -> "we are moving"
            }
            SpeechTopic.NEW_CONTEXT -> "where are we"
            SpeechTopic.CAPABILITY_LOST -> "something is missing"
        }
        return when (frame.act) {
            SpeechAct.QUESTION -> "$opener... $question?"
            SpeechAct.NOTICE -> "$opener... $question."
        }
    }
}

public data class PhoneBodyStep(
    public val record: StepRecord,
    public val observation: WorldObservation?,
    public val attentionSelected: Boolean,
    public val speechFrame: OrganismSpeechFrame?,
    public val utterance: String?,
    public val stateSignature: Long,
)

/**
 * Research-only bridge from normalized phone evidence into the existing FULL
 * organism. It does not expose raw Android data to the reducer and never lets a
 * classifier write an action or emotion directly.
 */
public class PhoneBodyRuntime(
    seed: Long,
    private val fx: Fx = Fx.counting().first,
) {
    private val habitat = Habitat(seed, HabitatCondition.CONTROLLED_NOVELTY)
    private val organism = OrganismAgent(Cohort.FULL, seed, fx)
    private val runtime = SpikeRuntime(
        runId = "D016-AB-PHONE-BODY",
        agent = organism,
        habitat = habitat,
        outcomes = OutcomeModel(),
        fx = fx,
        traceEveryDecision = false,
        attributionSampleEvery = 0,
    )
    private var tick: Long = 0L

    public fun step(observation: WorldObservation? = null): PhoneBodyStep {
        if (observation != null) organism.receiveWorldObservation(observation, tick)
        val record = runtime.step(tick)
        val speechFrame = if (
            observation != null &&
            record.choice.action in setOf(SpikeAction.ORIENT, SpikeAction.VOCALIZE)
        ) {
            speechFrame(observation)
        } else {
            null
        }
        val result = PhoneBodyStep(
            record = record,
            observation = observation,
            attentionSelected = observation != null &&
                record.choice.action in setOf(SpikeAction.ORIENT, SpikeAction.VOCALIZE),
            speechFrame = speechFrame,
            utterance = speechFrame?.let(ChildlikeUtteranceGenerator::render),
            stateSignature = organism.state.stateSignature(),
        )
        tick += 1L
        return result
    }

    public fun replay(observations: List<WorldObservation>): List<PhoneBodyStep> =
        observations.map(::step)

    private fun speechFrame(observation: WorldObservation): OrganismSpeechFrame {
        val affect = when {
            organism.state.arousal >= FixedPoint.of(0L, 700_000L) -> SpeechAffect.ALERT
            organism.state.arousal >= FixedPoint.of(0L, 400_000L) -> SpeechAffect.CURIOUS
            else -> SpeechAffect.UNCERTAIN
        }
        return OrganismSpeechFrame(
            act = SpeechAct.QUESTION,
            subject = SpeechSubject.OWNER,
            topic = when (observation.family) {
                ObservationFamily.MOVEMENT_ACTIVITY -> SpeechTopic.UNUSUAL_MOVEMENT
                else -> SpeechTopic.NEW_CONTEXT
            },
            affect = affect,
            urgency = if (affect == SpeechAffect.ALERT) SpeechUrgency.MEDIUM else SpeechUrgency.LOW,
            evidenceSequence = observation.meta.sequence,
        )
    }
}
