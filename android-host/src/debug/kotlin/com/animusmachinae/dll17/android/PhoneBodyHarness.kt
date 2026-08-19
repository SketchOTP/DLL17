package com.animusmachinae.dll17.android

import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.MotionObservationNormalizer
import com.animusmachinae.dll17.research.aliveness.MotionSample
import com.animusmachinae.dll17.research.aliveness.PhoneBodyRuntime
import com.animusmachinae.dll17.research.aliveness.SpikeAction
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import com.animusmachinae.dll17.research.aliveness.StepRecord
import com.animusmachinae.dll17.research.aliveness.WorldObservation

/** D016-AB phone-as-body harness. Debug APK only, no production dependency. */
internal class PhoneBodyHarness(
    private val onUtterance: (String) -> Unit,
) : DebugExperienceHarness {
    private val runtime = PhoneBodyRuntime(seed = 20260819L)
    private val normalizer = MotionObservationNormalizer()
    private var pendingObservation: WorldObservation? = null
    private var lastSpeechSequence: Long = Long.MIN_VALUE

    internal var tick: Long = 0L
        private set

    override var frame: SpikeExpressionContract.ExpressionFrame = initialFrame()
        private set

    override val showManualControls: Boolean = false

    override var statusText: String = "phone-body sensorium • waiting for movement"
        private set

    internal var lastObservation: WorldObservation? = null
        private set

    internal fun submitSensorSample(timestampNanos: Long, magnitudeMilliG: Int) {
        val observation = normalizer.normalize(MotionSample(timestampNanos, magnitudeMilliG)) ?: return
        pendingObservation = observation
        lastObservation = observation
        statusText = "phone-body sensorium • ${observation.activityFrom} → ${observation.activityTo}"
    }

    override fun submit(kind: InteractionKind, target: HabitatObject?) {
        // The D016-AB proof deliberately has no pet-game controls. Retain this
        // method for the shared debug UI contract, but do not synthesize input.
    }

    override fun advance(): StepRecord {
        val observation = pendingObservation
        pendingObservation = null
        val step = runtime.step(observation)
        frame = step.record.frame
        if (step.attentionSelected && observation != null) {
            statusText = "phone-body sensorium • organism attended to ${observation.activityTo} • ${step.record.choice.action}"
        }
        val speech = step.utterance
        if (speech != null && observation != null && observation.meta.sequence != lastSpeechSequence) {
            lastSpeechSequence = observation.meta.sequence
            onUtterance(speech)
        }
        tick += 1L
        return step.record
    }

    private companion object {
        fun initialFrame(): SpikeExpressionContract.ExpressionFrame =
            SpikeExpressionContract.frameFor(
                SpikeExpressionContract.PresentationInput(
                    action = SpikeAction.IDLE_VARIATION,
                    target = null,
                    intensity = 0L,
                    valence = 0L,
                    tick = 0L,
                ),
            )
    }
}
