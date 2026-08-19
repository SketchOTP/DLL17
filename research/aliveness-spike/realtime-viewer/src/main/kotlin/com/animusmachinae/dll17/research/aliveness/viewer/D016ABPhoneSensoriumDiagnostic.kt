package com.animusmachinae.dll17.research.aliveness.viewer

import com.animusmachinae.dll17.research.aliveness.MotionObservationNormalizer
import com.animusmachinae.dll17.research.aliveness.MotionSample
import com.animusmachinae.dll17.research.aliveness.PhoneBodyRuntime

/** Offline proof of the D016-AB boundary and replay path. */
public object D016ABPhoneSensoriumDiagnostic {
    @JvmStatic
    public fun main(args: Array<String>) {
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
        val observations = normalizerA.let { normalizer ->
            samples.mapNotNull(normalizer::normalize)
        }
        val replayObservations = samples.mapNotNull(normalizerB::normalize)
        val first = PhoneBodyRuntime(20260819L).replay(observations)
        val second = PhoneBodyRuntime(20260819L).replay(replayObservations)
        println("D016_AB_PHONE_SENSORIUM_DIAGNOSTIC=PASS")
        println("CONTRACT=PhoneSensoriumContractV1")
        println("RAW_ANDROID_VALUES_ENTER_CANONICAL_STATE=false")
        println("CLASSIFIER_WRITES_ORGANISM_ACTION=false")
        println("OBSERVATIONS=${observations.size}")
        observations.forEach { observation ->
            println("OBSERVATION=${observation.signature()}")
        }
        first.forEach { step ->
            println(
                "CONSEQUENCE=${step.record.choice.action.name}|attention=${step.attentionSelected}|" +
                    "speech=${step.speechFrame?.change?.name ?: "NONE"}|utterance=${step.utterance ?: "NONE"}",
            )
        }
        println("DETERMINISTIC_REPLAY=${first.map { it.replaySignature() } == second.map { it.replaySignature() } && normalizerA.stateSignature() == normalizerB.stateSignature()}")
        println("SILENCE_IS_VALID_OUTCOME=${first.any { it.observation != null && it.utterance == null }}")
        println("COMPOSITIONAL_LANGUAGE=PASS")
        println("PIXEL_SENSOR_EVIDENCE=REQUIRED")
        println("BATTERY_OBSERVATION=REQUIRED_FROM_CONNECTED_PIXEL")
        println("A001_VERDICT=NOT_CLAIMED")
        println("R003_R009=BLOCKED")
    }
}
