package com.animusmachinae.dll17.research.aliveness.sim

import com.animusmachinae.dll17.core.math.FixedPoint
import com.animusmachinae.dll17.research.aliveness.AttributionClass
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.SpikeAction
import com.animusmachinae.dll17.research.aliveness.SpikeContract

/**
 * Accumulates the A000.1 measures over a run.
 *
 * Every counter is fixed-size. A measurement apparatus that grew with run length
 * would make "no unbounded state growth" impossible to check honestly, since the
 * harness would be the thing growing.
 */
public class RunMeasures(public val windowTicks: Int) {

    public val actionCounts: LongArray = LongArray(SpikeAction.ALL.size)
    public val windowActionCounts: LongArray = LongArray(SpikeAction.ALL.size)
    public val inspectionCounts: LongArray = LongArray(HabitatObject.COUNT)
    public val windowInspectionCounts: LongArray = LongArray(HabitatObject.COUNT)

    /** First-order transition counts, for the cycle-regularity measure. */
    private val transitions = LongArray(SpikeAction.ALL.size * SpikeAction.ALL.size)
    private var previousAction: SpikeAction? = null

    public var ticks: Long = 0L
        private set
    public var windowTicksSeen: Long = 0L
        private set

    public val attributionCounts: LongArray = LongArray(AttributionClass.entries.size)
    public var spontaneousScored: Long = 0L
        private set
    public var tieBreakDetermined: Long = 0L
        private set

    /** Per-day distinct-object accounting, kept as a running mean. */
    private var daysCompleted: Long = 0L
    private var distinctThisDay = BooleanArray(HabitatObject.COUNT)
    private var distinctDaySum: Long = 0L
    private var revisitsThisDay: Long = 0L
    private var revisitDaySum: Long = 0L
    private val lastInspectedDay = LongArray(HabitatObject.COUNT) { -1L }
    private var previousEpistemicTarget: HabitatObject? = null

    public fun record(
        tick: Long,
        action: SpikeAction,
        target: HabitatObject?,
        epistemic: Boolean,
        totalTicks: Long,
    ) {
        ticks += 1
        actionCounts[action.ordinal] += 1
        previousAction?.let { prev ->
            transitions[prev.ordinal * SpikeAction.ALL.size + action.ordinal] += 1
        }
        previousAction = action

        if (tick >= totalTicks - windowTicks) {
            windowTicksSeen += 1
            windowActionCounts[action.ordinal] += 1
            if (epistemic && target != null) windowInspectionCounts[target.ordinal0] += 1
        }

        if (epistemic && target != null) {
            val i = target.ordinal0
            inspectionCounts[i] += 1
            // A revisit is a new *bout*: attention returning to an object it has
            // already visited today, not every tick spent looking at it.
            val newBout = previousEpistemicTarget != target
            if (newBout) {
                val day = tick / SpikeContract.TICKS_PER_VIRTUAL_DAY
                if (lastInspectedDay[i] == day) revisitsThisDay += 1
                lastInspectedDay[i] = day
            }
            distinctThisDay[i] = true
            previousEpistemicTarget = target
        }

        if ((tick + 1) % SpikeContract.TICKS_PER_VIRTUAL_DAY == 0L) {
            daysCompleted += 1
            distinctDaySum += distinctThisDay.count { it }.toLong()
            revisitDaySum += revisitsThisDay
            distinctThisDay = BooleanArray(HabitatObject.COUNT)
            revisitsThisDay = 0L
        }
    }

    public fun recordAttribution(cls: AttributionClass, tieBreak: Boolean) {
        attributionCounts[cls.ordinal] += 1
        spontaneousScored += 1
        if (tieBreak) tieBreakDetermined += 1
    }

    // ------------------------------------------------------------- readouts

    /** Shannon entropy over action types, in bits. */
    public fun actionEntropyBits(): Double = entropyOf(actionCounts)

    public fun windowActionEntropyBits(): Double = entropyOf(windowActionCounts)

    /** Highest share held by any single action in the final window. */
    public fun maxWindowOccupancy(): Double {
        val total = windowActionCounts.sum()
        if (total == 0L) return 0.0
        return windowActionCounts.max().toDouble() / total
    }

    /** Share of the final window spent in inactivity: idle, rest or sleep. */
    public fun windowInactivityShare(): Double {
        val total = windowActionCounts.sum()
        if (total == 0L) return 0.0
        val inactive = windowActionCounts[SpikeAction.IDLE_VARIATION.ordinal] +
            windowActionCounts[SpikeAction.REST.ordinal] +
            windowActionCounts[SpikeAction.SLEEP.ordinal]
        return inactive.toDouble() / total
    }

    public fun distinctObjectsInspectedPerDay(): Double =
        if (daysCompleted == 0L) 0.0 else distinctDaySum.toDouble() / daysCompleted

    public fun revisitationsPerDay(): Double =
        if (daysCompleted == 0L) 0.0 else revisitDaySum.toDouble() / daysCompleted

    /**
     * Cycle regularity: the mean probability of the most likely *next different*
     * action. Self-transitions are excluded deliberately — action commitment
     * holds a choice for several ticks by design, so counting those made every
     * committed controller look like a rigid A → B → C rotation regardless of
     * what it actually chose next.
     */
    public fun cycleRegularity(): Double {
        var weighted = 0.0
        var total = 0L
        for (from in SpikeAction.ALL.indices) {
            val row = SpikeAction.ALL.indices.map {
                if (it == from) 0L else transitions[from * SpikeAction.ALL.size + it]
            }
            val rowTotal = row.sum()
            if (rowTotal == 0L) continue
            weighted += row.max().toDouble()
            total += rowTotal
        }
        return if (total == 0L) 0.0 else weighted / total
    }

    /**
     * Highest share held by any single *waking* action. Sleep dominating a
     * nocturnal creature's tick budget is biology, not a dead loop, so the dead
     * loop measure is reported both ways.
     */
    public fun maxWakingOccupancy(): Double {
        var total = 0L
        var max = 0L
        for (a in SpikeAction.ALL) {
            if (a == SpikeAction.SLEEP || a == SpikeAction.REST) continue
            val c = windowActionCounts[a.ordinal]
            total += c
            if (c > max) max = c
        }
        return if (total == 0L) 0.0 else max.toDouble() / total
    }

    public fun substantiveSpontaneityRate(): Double {
        if (spontaneousScored == 0L) return 0.0
        var substantive = 0L
        for (c in AttributionClass.entries) {
            if (c.substantive) substantive += attributionCounts[c.ordinal]
        }
        return substantive.toDouble() / spontaneousScored
    }

    public fun oscillatorTieBreakOnlyRate(): Double {
        if (spontaneousScored == 0L) return 0.0
        val hollow = attributionCounts[AttributionClass.CURIOSITY_OSCILLATOR_ONLY.ordinal] +
            attributionCounts[AttributionClass.RANDOM_TIEBREAK_ONLY.ordinal]
        return hollow.toDouble() / spontaneousScored
    }

    /** Normalized action distribution over the whole run, for divergence measures. */
    public fun actionDistribution(): DoubleArray {
        val total = actionCounts.sum()
        if (total == 0L) return DoubleArray(actionCounts.size)
        return DoubleArray(actionCounts.size) { actionCounts[it].toDouble() / total }
    }

    public fun windowActionDistribution(): DoubleArray {
        val total = windowActionCounts.sum()
        if (total == 0L) return DoubleArray(windowActionCounts.size)
        return DoubleArray(windowActionCounts.size) { windowActionCounts[it].toDouble() / total }
    }

    private fun entropyOf(counts: LongArray): Double {
        val total = counts.sum()
        if (total == 0L) return 0.0
        var h = 0.0
        for (c in counts) {
            if (c == 0L) continue
            val p = c.toDouble() / total
            h -= p * (StrictMath.log(p) / LN2)
        }
        return h
    }

    public companion object {
        private val LN2 = StrictMath.log(2.0)

        /** Total-variation distance between two action distributions. */
        public fun totalVariation(a: DoubleArray, b: DoubleArray): Double {
            var sum = 0.0
            for (i in a.indices) sum += StrictMath.abs(a[i] - b[i])
            return sum / 2.0
        }

        /** Format a fixed-point value the same way everywhere in the evidence. */
        public fun fx(raw: Long): String {
            val negative = raw < 0L
            val magnitude = if (negative) -raw else raw
            return (if (negative) "-" else "") + (magnitude / FixedPoint.SCALE) + "." +
                (magnitude % FixedPoint.SCALE).toString().padStart(6, '0')
        }

        /** Format a measured double deterministically, for digesting. */
        public fun d6(value: Double): String = String.format(java.util.Locale.ROOT, "%.6f", value)
    }
}
