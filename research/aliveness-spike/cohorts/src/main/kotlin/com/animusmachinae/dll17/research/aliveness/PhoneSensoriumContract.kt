package com.animusmachinae.dll17.research.aliveness

/**
 * D016-AB research contract. Android/platform facts are not organism facts.
 * They cross this boundary only after quantization, provenance tagging and
 * explicit uncertainty/capability metadata.
 */
public object PhoneSensoriumContract {
    public const val CONTRACT_ID: String = "PhoneSensoriumContractV1"
    public const val CONTRACT_VERSION: Int = 1
}

public enum class SensoriumMode {
    BACKGROUND_LOW_AWARENESS,
    FOREGROUND_SENSORIUM,
    ACTIVE_CONVERSATION,
    CAPABILITY_DEGRADED,
}

public enum class ObservationFamily {
    MOVEMENT_ACTIVITY,
    DEVICE_ORIENTATION,
    SIGNIFICANT_MOTION,
    AMBIENT_LIGHT,
    PROXIMITY,
    TRUSTED_TIME,
    LOCATION_PLACE,
    FOREGROUND_CAMERA,
    BOUNDED_AUDITORY,
    OWNER_SPEECH,
    CALENDAR_CONTEXT,
    CAPABILITY_STATE,
}

public enum class ActivityBand {
    STILL,
    WALKING,
    RUNNING,
    VEHICLE,
    UNKNOWN,
}

public enum class MotionBand { NONE, LOW, MODERATE, HIGH }

public enum class OrientationBucket { UNKNOWN, FACE_UP, FACE_DOWN, PORTRAIT, LANDSCAPE }

public enum class AmbientLightBand { UNKNOWN, DARK, DIM, BRIGHT, GLARE }

public enum class ProximityBand { UNKNOWN, NEAR, FAR }

public enum class PermissionState { GRANTED, DENIED, NOT_REQUESTED, NOT_APPLICABLE }

public enum class CapabilityState { AVAILABLE, UNAVAILABLE, RESTRICTED, NOT_REQUESTED }

public enum class ObservationProvenance {
    ANDROID_SENSOR_MANAGER,
    ANDROID_ACTIVITY_RECOGNITION,
    ANDROID_SYSTEM_CLOCK,
    ANDROID_LOCATION,
    ANDROID_CAMERA_ANALYZER,
    ANDROID_AUDIO_ANALYZER,
    ANDROID_SPEECH_RECOGNIZER,
    ANDROID_CALENDAR_PROVIDER,
    REPLAY_FIXTURE,
}

/** Metadata required for every observation family, including absent families. */
public data class ObservationMeta(
    public val capturedAtMillis: Long,
    public val sequence: Long,
    public val freshUntilMillis: Long,
    public val confidencePpm: Int,
    public val uncertaintyPpm: Int,
    public val provenance: ObservationProvenance,
    public val permission: PermissionState,
    public val capability: CapabilityState,
    public val sensoriumMode: SensoriumMode,
) {
    init {
        require(sequence >= 0L)
        require(confidencePpm in 0..1_000_000)
        require(uncertaintyPpm in 0..1_000_000)
        require(freshUntilMillis >= capturedAtMillis)
    }
}

/**
 * Canonical boundary value. It contains no raw sensor samples and no classifier
 * assertion about emotion, memory, relationship, intention or action.
 */
public data class WorldObservation(
    public val family: ObservationFamily,
    public val activityFrom: ActivityBand? = null,
    public val activityTo: ActivityBand? = null,
    public val motionBand: MotionBand? = null,
    public val orientation: OrientationBucket? = null,
    public val significantMotion: Boolean? = null,
    public val ambientLight: AmbientLightBand? = null,
    public val proximity: ProximityBand? = null,
    public val meta: ObservationMeta,
) {
    init {
        require(activityFrom != null || activityTo != null || family != ObservationFamily.MOVEMENT_ACTIVITY)
        require(activityFrom == null || activityTo == null || activityFrom != activityTo)
    }

    public fun signature(): String = listOf(
        family.name,
        activityFrom?.name ?: "-",
        activityTo?.name ?: "-",
        motionBand?.name ?: "-",
        orientation?.name ?: "-",
        significantMotion?.toString() ?: "-",
        ambientLight?.name ?: "-",
        proximity?.name ?: "-",
        meta.capturedAtMillis,
        meta.sequence,
        meta.freshUntilMillis,
        meta.confidencePpm,
        meta.uncertaintyPpm,
        meta.provenance.name,
        meta.permission.name,
        meta.capability.name,
        meta.sensoriumMode.name,
    ).joinToString("|")
}

/** Platform adapter input. It is deliberately not a WorldObservation. */
public data class MotionSample(
    public val timestampNanos: Long,
    public val linearAccelerationMagnitudeMilliG: Int,
)

/**
 * Small deterministic platform interpretation for the research slice. The
 * thresholds are diagnostic, not a claim that this replaces Activity
 * Recognition for production. It emits only transitions, not every raw sample.
 */
public class MotionObservationNormalizer(
    private val mode: SensoriumMode = SensoriumMode.FOREGROUND_SENSORIUM,
) {
    private var previous: ActivityBand = ActivityBand.STILL
    private var sequence: Long = 0L

    public fun normalize(sample: MotionSample): WorldObservation? {
        val next = when {
            sample.linearAccelerationMagnitudeMilliG < 180 -> ActivityBand.STILL
            sample.linearAccelerationMagnitudeMilliG < 850 -> ActivityBand.WALKING
            else -> ActivityBand.RUNNING
        }
        if (next == previous) return null
        val old = previous
        previous = next
        val capturedAtMillis = sample.timestampNanos / 1_000_000L
        val confidence = when (next) {
            ActivityBand.STILL -> 900_000
            ActivityBand.WALKING -> 700_000
            ActivityBand.RUNNING -> 650_000
            ActivityBand.VEHICLE, ActivityBand.UNKNOWN -> 400_000
        }
        val motion = when (next) {
            ActivityBand.STILL -> MotionBand.NONE
            ActivityBand.WALKING -> MotionBand.MODERATE
            ActivityBand.RUNNING -> MotionBand.HIGH
            ActivityBand.VEHICLE -> MotionBand.MODERATE
            ActivityBand.UNKNOWN -> MotionBand.LOW
        }
        return WorldObservation(
            family = ObservationFamily.MOVEMENT_ACTIVITY,
            activityFrom = old,
            activityTo = next,
            motionBand = motion,
            meta = ObservationMeta(
                capturedAtMillis = capturedAtMillis,
                sequence = sequence++,
                freshUntilMillis = capturedAtMillis + 3_000L,
                confidencePpm = confidence,
                uncertaintyPpm = 1_000_000 - confidence,
                provenance = ObservationProvenance.ANDROID_SENSOR_MANAGER,
                permission = PermissionState.NOT_APPLICABLE,
                capability = CapabilityState.AVAILABLE,
                sensoriumMode = mode,
            ),
        )
    }

    public fun reset() {
        previous = ActivityBand.STILL
        sequence = 0L
    }
}
