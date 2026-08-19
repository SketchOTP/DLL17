package com.animusmachinae.dll17.research.aliveness

import kotlin.math.floor

/** Coarse, bounded time concepts. No wall-clock epoch enters organism state. */
public enum class TimeOfDayBucket {
    NIGHT,
    MORNING,
    AFTERNOON,
    EVENING,
}

public enum class DayPattern {
    WEEKDAY,
    WEEKEND,
}

public enum class CircadianContext {
    REST_HOURS,
    MORNING_RISE,
    DAYTIME,
    EVENING,
}

/** The trust class is evidence metadata, never an action or reward authority. */
public enum class TimeTrustClass {
    VERIFIED_MONOTONIC,
    AUTHENTICATED,
    UNVERIFIED_REBOOT,
    ANOMALOUS,
    UNAVAILABLE,
}

public data class TrustedTimeObservation(
    public val localDayPattern: DayPattern,
    public val timeOfDay: TimeOfDayBucket,
    public val circadianContext: CircadianContext,
    public val trust: TimeTrustClass,
    public val confidencePpm: Int,
) {
    init {
        require(confidencePpm in 0..1_000_000)
    }

    public fun signature(): String = listOf(
        localDayPattern.name,
        timeOfDay.name,
        circadianContext.name,
        trust.name,
        confidencePpm,
    ).joinToString("|")

    public companion object {
        public fun fromLocal(
            hourOfDay: Int,
            isWeekend: Boolean,
            trust: TimeTrustClass,
            confidencePpm: Int,
        ): TrustedTimeObservation {
            require(hourOfDay in 0..23)
            val bucket = when (hourOfDay) {
                in 0..4 -> TimeOfDayBucket.NIGHT
                in 5..11 -> TimeOfDayBucket.MORNING
                in 12..17 -> TimeOfDayBucket.AFTERNOON
                else -> TimeOfDayBucket.EVENING
            }
            val circadian = when (bucket) {
                TimeOfDayBucket.NIGHT -> CircadianContext.REST_HOURS
                TimeOfDayBucket.MORNING -> CircadianContext.MORNING_RISE
                TimeOfDayBucket.AFTERNOON -> CircadianContext.DAYTIME
                TimeOfDayBucket.EVENING -> CircadianContext.EVENING
            }
            return TrustedTimeObservation(
                localDayPattern = if (isWeekend) DayPattern.WEEKEND else DayPattern.WEEKDAY,
                timeOfDay = bucket,
                circadianContext = circadian,
                trust = trust,
                confidencePpm = confidencePpm,
            )
        }
    }
}

/** Opaque identity only. Latitude/longitude never cross the research boundary. */
public data class CoarsePlaceIdentity(public val value: String) {
    init {
        require(value.matches(Regex("UNKNOWN_PLACE|PLACE_[a-z0-9]{8,16}")))
    }

    public val isUnknown: Boolean
        get() = value == UNKNOWN.value

    public companion object {
        public val UNKNOWN: CoarsePlaceIdentity = CoarsePlaceIdentity("UNKNOWN_PLACE")
    }
}

/** A normalized place evidence value; it contains no coordinates or names. */
public data class CoarsePlaceObservation(public val identity: CoarsePlaceIdentity) {
    public fun signature(): String = identity.value
}

/** Stable coarse-cell identity for the Android adapter and deterministic fixtures. */
public object CoarsePlaceNormalizer {
    public const val GRID_DECIMAL_PLACES: Int = 3

    public fun fromCoordinates(latitude: Double, longitude: Double): CoarsePlaceIdentity {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
        require(latitude.isFinite() && longitude.isFinite())
        val scale = 1_000L
        val latCell = floor(latitude * scale).toLong()
        val lonCell = floor(longitude * scale).toLong()
        var hash = 0xCBF29CE484222325uL.toLong()
        fun mix(value: Long) {
            hash = (hash xor value) * 0x100000001B3L
        }
        mix(latCell)
        mix(lonCell)
        val opaque = (hash and Long.MAX_VALUE).toString(36).padStart(8, '0').takeLast(16)
        return CoarsePlaceIdentity("PLACE_$opaque")
    }

    /** Test-only opaque identity; the label is never a real-world place name. */
    public fun fixture(slot: Int): CoarsePlaceIdentity {
        require(slot >= 1)
        val opaque = slot.toLong().toString(36).padStart(8, '0').takeLast(16)
        return CoarsePlaceIdentity("PLACE_$opaque")
    }
}

public enum class ContextInterpretation {
    EXPECTED_CONTEXT,
    FAMILIAR_CONTEXT,
    FAMILIAR_BUT_UNUSUAL,
    NOVEL_CONTEXT,
    UNKNOWN_CONTEXT,
}

public data class RoutineContext(
    public val interpretation: ContextInterpretation,
    public val place: CoarsePlaceIdentity,
    public val timeOfDay: TimeOfDayBucket?,
    public val dayPattern: DayPattern?,
    public val timeTrust: TimeTrustClass?,
    public val learningEligible: Boolean,
    public val placeVisitCount: Int,
    public val matchingRoutineCount: Int,
    public val recentVisit: Boolean,
    public val familiarityPpm: Int,
) {
    public fun signature(): String = listOf(
        interpretation.name,
        place.value,
        timeOfDay?.name ?: "-",
        dayPattern?.name ?: "-",
        timeTrust?.name ?: "-",
        learningEligible,
        placeVisitCount,
        matchingRoutineCount,
        recentVisit,
        familiarityPpm,
    ).joinToString("|")
}

/**
 * The latest derived context carried between compatible observations. The
 * sequence deadline is explicit so place/time belief cannot follow the
 * organism indefinitely or depend on wall-clock scheduling.
 */
public data class CurrentRoutineContext(
    public val context: RoutineContext,
    public val sourceSequence: Long,
    public val expiresAtSequenceExclusive: Long,
) {
    init {
        require(sourceSequence >= 0L)
        require(expiresAtSequenceExclusive > sourceSequence)
    }

    public fun isFreshAt(sequence: Long): Boolean =
        sequence >= sourceSequence && sequence < expiresAtSequenceExclusive

    public fun signature(): String = listOf(
        context.signature(),
        sourceSequence,
        expiresAtSequenceExclusive,
    ).joinToString("|")
}

/**
 * Fixed-size place/time memory. Counts saturate and old evidence decays on a
 * deterministic interval; there is no route history and no unbounded map.
 */
public class BoundedRoutineContextMemory(
    private val capacity: Int = MAX_ENTRIES,
) {
    private data class Entry(
        val place: CoarsePlaceIdentity,
        val timeOfDay: TimeOfDayBucket,
        val dayPattern: DayPattern,
        var visits: Int,
        var lastSequence: Long,
    )

    private val entries: Array<Entry?> = arrayOfNulls(capacity)
    private var observations: Long = 0L

    public fun observe(
        place: CoarsePlaceObservation,
        time: TrustedTimeObservation?,
        sequence: Long,
    ): RoutineContext {
        require(sequence >= 0L)
        if (place.identity.isUnknown || time == null || time.trust == TimeTrustClass.UNAVAILABLE) {
            return RoutineContext(
                interpretation = ContextInterpretation.UNKNOWN_CONTEXT,
                place = CoarsePlaceIdentity.UNKNOWN,
                timeOfDay = time?.timeOfDay,
                dayPattern = time?.localDayPattern,
                timeTrust = time?.trust,
                learningEligible = false,
                placeVisitCount = 0,
                matchingRoutineCount = 0,
                recentVisit = false,
                familiarityPpm = 0,
            )
        }

        val exact = find(place.identity, time)
        val placeVisitCount = entries.filterNotNull()
            .filter { it.place == place.identity }
            .sumOf { it.visits }
        val interpretation = when {
            placeVisitCount == 0 -> ContextInterpretation.NOVEL_CONTEXT
            exact == null -> ContextInterpretation.FAMILIAR_BUT_UNUSUAL
            exact.visits >= EXPECTED_VISITS -> ContextInterpretation.EXPECTED_CONTEXT
            else -> ContextInterpretation.FAMILIAR_CONTEXT
        }
        val result = RoutineContext(
            interpretation = interpretation,
            place = place.identity,
            timeOfDay = time.timeOfDay,
            dayPattern = time.localDayPattern,
            timeTrust = time.trust,
            learningEligible = time.trust in TRAINING_TRUST_CLASSES,
            placeVisitCount = placeVisitCount,
            matchingRoutineCount = exact?.visits ?: 0,
            recentVisit = exact?.let { sequence - it.lastSequence <= RECENT_SEQUENCE_WINDOW } ?: false,
            familiarityPpm = (placeVisitCount * 1_000_000 / MAX_VISITS).coerceAtMost(1_000_000),
        )

        // Only monotonic or authenticated time can train routine expectations.
        // UNVERIFIED_REBOOT, ANOMALOUS and UNAVAILABLE time may still produce a
        // bounded temporary interpretation from existing entries, but they do
        // not create, reinforce or age the learned routine memory.
        if (time.trust in TRAINING_TRUST_CLASSES) {
            record(place.identity, time, sequence)
            observations += 1L
            if (observations % DECAY_PERIOD == 0L) decay()
        }
        return result
    }

    public fun entryCount(): Int = entries.count { it != null }

    public fun maxStoredVisits(): Int = entries.filterNotNull().maxOfOrNull { it.visits } ?: 0

    /** Complete bounded state for deterministic replay. */
    public fun stateSignature(): Long {
        var hash = 0xCBF29CE484222325uL.toLong()
        fun mix(value: Long) {
            hash = (hash xor value) * 0x100000001B3L
        }
        mix(observations)
        for (entry in entries) {
            if (entry == null) {
                mix(0L)
            } else {
                entry.place.value.forEach { mix(it.code.toLong()) }
                mix(entry.timeOfDay.ordinal.toLong())
                mix(entry.dayPattern.ordinal.toLong())
                mix(entry.visits.toLong())
                mix(entry.lastSequence)
            }
        }
        return hash
    }

    private fun find(place: CoarsePlaceIdentity, time: TrustedTimeObservation): Entry? =
        entries.firstOrNull {
            it?.place == place && it.timeOfDay == time.timeOfDay && it.dayPattern == time.localDayPattern
        }

    private fun record(place: CoarsePlaceIdentity, time: TrustedTimeObservation, sequence: Long) {
        val existing = find(place, time)
        if (existing != null) {
            existing.visits = (existing.visits + 1).coerceAtMost(MAX_VISITS)
            existing.lastSequence = sequence
            return
        }
        val empty = entries.indexOfFirst { it == null }
        if (empty >= 0) {
            entries[empty] = Entry(place, time.timeOfDay, time.localDayPattern, 1, sequence)
            return
        }
        val weakest = entries.indices.minWithOrNull(
            compareBy<Int> { entries[it]?.visits ?: Int.MAX_VALUE }
                .thenBy { entries[it]?.lastSequence ?: Long.MAX_VALUE },
        ) ?: return
        entries[weakest] = Entry(place, time.timeOfDay, time.localDayPattern, 1, sequence)
    }

    private fun decay() {
        for (index in entries.indices) {
            val entry = entries[index] ?: continue
            entry.visits -= 1
            if (entry.visits <= 0) entries[index] = null
        }
    }

    public companion object {
        public val TRAINING_TRUST_CLASSES: Set<TimeTrustClass> = setOf(
            TimeTrustClass.VERIFIED_MONOTONIC,
            TimeTrustClass.AUTHENTICATED,
        )
        public const val MAX_ENTRIES: Int = 32
        public const val MAX_VISITS: Int = 8
        public const val EXPECTED_VISITS: Int = 3
        public const val RECENT_SEQUENCE_WINDOW: Long = 8L
        public const val DECAY_PERIOD: Long = 64L
    }
}
