package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

public enum class SpeechAct { QUESTION, NOTICE }

public enum class SpeechSubject { OWNER, WORLD }

public enum class ObservedChange {
    SPEED_INCREASE,
    SPEED_DECREASE,
    ACTIVITY_STARTED,
    BECAME_STILL,
    CONTEXT_CHANGED,
    CAPABILITY_LOST,
}

public enum class ActivityConcept { STILL, WALKING, RUNNING, VEHICLE, UNKNOWN }

public enum class QuestionConcept { CAUSE, DESTINATION, STATE }

public enum class SpeechIntensity { LOW, MODERATE, HIGH }

public enum class KnownContext { PHONE_MOVING, OWNER_PRESENT, UNKNOWN }

public enum class SpeechAffect { CURIOUS, ALERT, UNCERTAIN }

public enum class SpeechUrgency { LOW, MEDIUM }

/** Semantic output of the organism, before bounded local language rendering. */
public data class OrganismSpeechFrame(
    public val act: SpeechAct,
    public val subject: SpeechSubject,
    public val change: ObservedChange,
    public val activity: ActivityConcept,
    public val intensity: SpeechIntensity,
    public val question: QuestionConcept,
    public val affect: SpeechAffect,
    public val urgency: SpeechUrgency,
    public val knownContext: KnownContext,
    public val uncertaintyPpm: Int,
    public val evidenceSequence: Long,
)

/** Bounded grammar; it has fragments, not observation-to-sentence entries. */
public object ChildlikeUtteranceGenerator {
    public fun render(frame: OrganismSpeechFrame): String {
        val opener = when (frame.affect) {
            SpeechAffect.CURIOUS -> "hey"
            SpeechAffect.ALERT -> "whoa"
            SpeechAffect.UNCERTAIN -> "um"
        }
        return when (frame.act) {
            SpeechAct.QUESTION -> when (frame.question) {
                QuestionConcept.CAUSE -> "$opener... why ${subjectVerb(frame)}?"
                QuestionConcept.DESTINATION -> "$opener... where we going?"
                QuestionConcept.STATE -> "$opener... what happened?"
            }
            SpeechAct.NOTICE -> "$opener... ${noticePhrase(frame)}."
        }
    }

    private fun subjectVerb(frame: OrganismSpeechFrame): String = when (frame.activity) {
        ActivityConcept.RUNNING -> if (frame.change == ObservedChange.SPEED_INCREASE) {
            "we going fast"
        } else {
            "we running"
        }
        ActivityConcept.WALKING -> if (frame.change == ObservedChange.SPEED_INCREASE) {
            "we going quicker"
        } else {
            "we walking"
        }
        ActivityConcept.STILL -> "we stopped"
        ActivityConcept.VEHICLE -> "we riding"
        ActivityConcept.UNKNOWN -> "we moving"
    }

    private fun noticePhrase(frame: OrganismSpeechFrame): String = when (frame.change) {
        ObservedChange.BECAME_STILL -> "we stopped"
        ObservedChange.SPEED_INCREASE -> "we going faster"
        ObservedChange.SPEED_DECREASE -> "we slowing down"
        ObservedChange.ACTIVITY_STARTED -> "we started moving"
        ObservedChange.CONTEXT_CHANGED -> "something changed"
        ObservedChange.CAPABILITY_LOST -> "something is missing"
    }
}

public data class PhoneBodyStep(
    public val record: StepRecord,
    public val observation: WorldObservation?,
    public val attentionSelected: Boolean,
    public val worldObservationSalience: Long,
    public val speechFrame: OrganismSpeechFrame?,
    public val utterance: String?,
    public val stateSignature: Long,
) {
    /** Complete output boundary for deterministic D016-AB replay. */
    public fun replaySignature(): String = listOf(
        stateSignature,
        record.choice.action.name,
        record.choice.target?.name ?: "-",
        attentionSelected,
        observation?.signature() ?: "-",
        speechFrame?.toString() ?: "-",
        utterance ?: "-",
    ).joinToString("|")
}

/**
 * Research-only bridge from normalized phone evidence into the existing FULL
 * organism. It does not expose raw Android data to the reducer and never lets a
 * classifier write an action, emotion or sentence directly.
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
    private val routineContext = BoundedRoutineContextMemory()
    private var latestTrustedTime: TrustedTimeObservation? = null
    private var currentContext: CurrentRoutineContext? = null
    private var tick: Long = 0L

    public fun step(observation: WorldObservation? = null): PhoneBodyStep {
        val enriched = normalizeContext(observation)
        if (enriched != null) organism.receiveWorldObservation(enriched, tick)
        val record = runtime.step(tick)
        // ORIENT is silent by default. Speech requires the organism to choose
        // its communicative VOCALIZE action; world evidence alone cannot talk.
        val speechFrame = if (enriched != null && record.choice.action == SpikeAction.VOCALIZE) {
            speechFrame(enriched)
        } else {
            null
        }
        val result = PhoneBodyStep(
            record = record,
            observation = enriched,
            attentionSelected = enriched != null &&
                record.choice.action in setOf(SpikeAction.ORIENT, SpikeAction.VOCALIZE),
            worldObservationSalience = organism.state.worldObservationSalience,
            speechFrame = speechFrame,
            utterance = speechFrame?.let(ChildlikeUtteranceGenerator::render),
            stateSignature = researchStateSignature(),
        )
        tick += 1L
        return result
    }

    public fun replay(observations: List<WorldObservation>): List<PhoneBodyStep> =
        observations.map(::step)

    /** Research-only state digest, including bounded context memory and time. */
    public fun researchStateSignature(): Long {
        var hash = organism.state.researchStateSignature()
        fun mix(value: Long) {
            hash = (hash xor value) * 0x100000001B3L
        }
        mix(routineContext.stateSignature())
        latestTrustedTime?.signature()?.forEach { mix(it.code.toLong()) }
        currentContext?.signature()?.forEach { mix(it.code.toLong()) }
        return hash
    }

    private fun normalizeContext(observation: WorldObservation?): WorldObservation? {
        if (observation == null) return null
        if (observation.trustedTime != null) latestTrustedTime = observation.trustedTime
        if (observation.family == ObservationFamily.LOCATION_PLACE && observation.place != null) {
            val context = observation.context ?: routineContext.observe(
                place = observation.place,
                time = observation.trustedTime ?: latestTrustedTime,
                sequence = observation.meta.sequence,
            )
            currentContext = if (context.interpretation == ContextInterpretation.UNKNOWN_CONTEXT) {
                null
            } else {
                CurrentRoutineContext(
                    context = context,
                    sourceSequence = observation.meta.sequence,
                    expiresAtSequenceExclusive = observation.meta.sequence + CURRENT_CONTEXT_TTL_SEQUENCES,
                )
            }
            return observation.copy(context = context)
        }

        val carried = currentContext
        if (carried != null && carried.isFreshAt(observation.meta.sequence)) {
            // Context is evidence attached to the observation; it never enters
            // the action path and the organism still owns salience/arbitration.
            return observation.copy(context = carried.context)
        }
        if (carried != null) currentContext = null
        return observation
    }

    public companion object {
        /** Fresh for fewer than eight subsequent normalized observations. */
        public const val CURRENT_CONTEXT_TTL_SEQUENCES: Long = 8L
    }

    private fun speechFrame(observation: WorldObservation): OrganismSpeechFrame {
        val affect = when {
            organism.state.arousal >= FixedPoint.of(0L, 700_000L) -> SpeechAffect.ALERT
            organism.state.arousal >= FixedPoint.of(0L, 400_000L) -> SpeechAffect.CURIOUS
            else -> SpeechAffect.UNCERTAIN
        }
        val change = when {
            observation.activityFrom == ActivityBand.STILL && observation.activityTo != ActivityBand.STILL ->
                ObservedChange.ACTIVITY_STARTED
            observation.activityFrom == ActivityBand.WALKING && observation.activityTo == ActivityBand.RUNNING ->
                ObservedChange.SPEED_INCREASE
            observation.activityFrom == ActivityBand.RUNNING && observation.activityTo == ActivityBand.WALKING ->
                ObservedChange.SPEED_DECREASE
            observation.activityTo == ActivityBand.STILL -> ObservedChange.BECAME_STILL
            else -> ObservedChange.CONTEXT_CHANGED
        }
        val activity = when (observation.activityTo) {
            ActivityBand.STILL -> ActivityConcept.STILL
            ActivityBand.WALKING -> ActivityConcept.WALKING
            ActivityBand.RUNNING -> ActivityConcept.RUNNING
            ActivityBand.VEHICLE -> ActivityConcept.VEHICLE
            null, ActivityBand.UNKNOWN -> ActivityConcept.UNKNOWN
        }
        return OrganismSpeechFrame(
            act = SpeechAct.QUESTION,
            subject = SpeechSubject.WORLD,
            change = change,
            activity = activity,
            intensity = when (observation.motionBand) {
                MotionBand.HIGH -> SpeechIntensity.HIGH
                MotionBand.MODERATE -> SpeechIntensity.MODERATE
                else -> SpeechIntensity.LOW
            },
            question = when {
                observation.activityTo == ActivityBand.STILL -> QuestionConcept.STATE
                change == ObservedChange.SPEED_INCREASE -> QuestionConcept.CAUSE
                else -> QuestionConcept.DESTINATION
            },
            affect = affect,
            urgency = if (affect == SpeechAffect.ALERT) SpeechUrgency.MEDIUM else SpeechUrgency.LOW,
            knownContext = KnownContext.PHONE_MOVING,
            uncertaintyPpm = observation.meta.uncertaintyPpm,
            evidenceSequence = observation.meta.sequence,
        )
    }
}
