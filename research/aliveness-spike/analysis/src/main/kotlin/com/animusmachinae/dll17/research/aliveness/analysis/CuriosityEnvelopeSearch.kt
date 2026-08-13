package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.core.math.FixedPoint
import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.CuriosityParameters
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.sim.AcceleratedSimulator
import com.animusmachinae.dll17.research.aliveness.sim.RunConfig
import com.animusmachinae.dll17.research.aliveness.sim.RunMeasures

/** The canonical result type for the joint feasibility search. */
public enum class CuriosityEnvelopeFeasibilityResult {
    NON_EMPTY_FEASIBLE_REGION,

    /**
     * Emptiness that may be an incompatible threshold pair. Which of the two
     * empty outcomes actually applies is the independent reviewer's decision
     * under `CuriosityEnvelopeFeasibilityV1`, not the implementer's, so the
     * search never returns `EMPTY_MECHANISM_FAILURE` on its own authority.
     */
    EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE,

    EMPTY_MECHANISM_FAILURE,
}

/** One grid point's aggregated outcome across the whole seed matrix. */
public class GridPoint(
    public val index: Int,
    public val parameters: CuriosityParameters,
    public val entropyBits: Double,
    public val distinctObjectsPerDay: Double,
    public val maxOccupancy: Double,
    public val maxWakingOccupancy: Double,
    public val revisitsPerDay: Double,
    public val cycleRegularity: Double,
    public val substantiveRate: Double,
    public val oscillatorTieBreakOnlyRate: Double,
    public val scoredActions: Long,
) {
    public val entropyOk: Boolean get() = entropyBits >= threshold(SpikeContract.MIN_ACTION_TYPE_ENTROPY_BITS)
    public val distinctOk: Boolean
        get() = distinctObjectsPerDay >= threshold(SpikeContract.MIN_DISTINCT_OBJECTS_INSPECTED_PER_DAY)
    public val occupancyOk: Boolean
        get() = maxOccupancy <= threshold(SpikeContract.MAX_SINGLE_ACTION_OCCUPANCY)
    public val revisitOk: Boolean
        get() = revisitsPerDay >= threshold(SpikeContract.MIN_REVISITATION_RATE_PER_DAY)
    public val regularityOk: Boolean
        get() = cycleRegularity <= threshold(SpikeContract.MAX_CYCLE_REGULARITY)

    /** Requirement 1: no pathological behavioural collapse in the static habitat. */
    public val antiConvergenceOk: Boolean
        get() = entropyOk && distinctOk && occupancyOk && revisitOk && regularityOk

    public val substantiveOk: Boolean
        get() = substantiveRate >= threshold(SpikeContract.REQUIRED_SUBSTANTIVE_SPONTANEITY_RATE)
    public val oscillatorOk: Boolean
        get() = oscillatorTieBreakOnlyRate <= threshold(SpikeContract.MAX_OSCILLATOR_TIEBREAK_ONLY_RATE)

    /** Requirement 2: inside the substantive-versus-oscillator attribution envelope. */
    public val attributionOk: Boolean
        get() = scoredActions > 0L && substantiveOk && oscillatorOk

    /** Both requirements, on the identical parameterization and seed matrix. */
    public val jointlyFeasible: Boolean
        get() = antiConvergenceOk && attributionOk

    public fun row(): String = buildString {
        append(String.format(java.util.Locale.ROOT, "  %3d ", index))
        append("floor=").append(pad(parameters.baseFloor))
        append(" amp=").append(pad(parameters.contextAmplitude))
        append(" inhib=").append(pad(parameters.inhibitionDepth))
        append(" | H=").append(RunMeasures.d6(entropyBits)).append(flag(entropyOk))
        append(" obj=").append(RunMeasures.d6(distinctObjectsPerDay)).append(flag(distinctOk))
        append(" occ=").append(RunMeasures.d6(maxOccupancy)).append(flag(occupancyOk))
        append(" rev=").append(RunMeasures.d6(revisitsPerDay)).append(flag(revisitOk))
        append(" reg=").append(RunMeasures.d6(cycleRegularity)).append(flag(regularityOk))
        append(" | sub=").append(RunMeasures.d6(substantiveRate)).append(flag(substantiveOk))
        append(" osc=").append(RunMeasures.d6(oscillatorTieBreakOnlyRate)).append(flag(oscillatorOk))
        append(" | anti=").append(if (antiConvergenceOk) "Y" else "n")
        append(" attr=").append(if (attributionOk) "Y" else "n")
        append(" joint=").append(if (jointlyFeasible) "YES" else "no")
    }

    private fun flag(ok: Boolean) = if (ok) "+" else "-"

    private fun pad(raw: Long) = RunMeasures.fx(raw)

    private companion object {
        fun threshold(raw: Long): Double = raw.toDouble() / FixedPoint.SCALE
    }
}

/**
 * `CuriosityEnvelopeFeasibilityV1`.
 *
 * The two requirements are evaluated on the **same parameter hash and the same
 * seed matrix**, in the same pass, from the same runs. That is the whole point:
 * running anti-convergence on one oscillator configuration and attribution on
 * another would let a parameterization pass both tests without ever satisfying
 * them simultaneously, which is exactly what the canonical amendment forbids.
 */
public object CuriosityEnvelopeSearch {

    public const val CONTRACT_ID: String = SpikeContract.CURIOSITY_ENVELOPE_ID
    public const val CONTRACT_VERSION: Int = 1

    /** Frozen before the search ran. Perturbing these after seeing results is prohibited. */
    public val BASE_FLOORS: LongArray = longArrayOf(
        FixedPoint.of(0L, 20_000L),
        FixedPoint.of(0L, 40_000L),
        FixedPoint.of(0L, 80_000L),
    )
    public val AMPLITUDES: LongArray = longArrayOf(
        FixedPoint.of(0L, 80_000L),
        FixedPoint.of(0L, 150_000L),
        FixedPoint.of(0L, 260_000L),
    )
    public val INHIBITION_DEPTHS: LongArray = longArrayOf(
        FixedPoint.of(0L, 60_000L),
        FixedPoint.of(0L, 120_000L),
        FixedPoint.of(0L, 220_000L),
    )

    public val SEED_MATRIX: LongArray = longArrayOf(1_001L, 2_003L, 3_005L, 4_007L)

    public const val SEARCH_DAYS: Int = 40
    public const val WINDOW_DAYS: Int = 15
    public const val ATTRIBUTION_SAMPLE_EVERY: Int = 5

    public class SearchResult(
        public val points: List<GridPoint>,
        public val result: CuriosityEnvelopeFeasibilityResult,
        public val robustPoints: List<GridPoint>,
        public val selected: GridPoint?,
    ) {
        public fun render(): String = buildString {
            append("CURIOSITY_ENVELOPE_CONTRACT=").append(CONTRACT_ID).append('\n')
            append("CURIOSITY_ENVELOPE_VERSION=").append(CONTRACT_VERSION).append('\n')
            append("ATTRIBUTION_CONTRACT=").append(SpikeContract.ATTRIBUTION_CONTRACT_ID)
                .append(" / ").append(SpikeContract.COALITION_VALUE_FUNCTION_ID).append('\n')
            append("SEED_MATRIX=").append(SEED_MATRIX.joinToString(",")).append('\n')
            append("SEARCH_DAYS=").append(SEARCH_DAYS)
                .append(" WINDOW_DAYS=").append(WINDOW_DAYS)
                .append(" HABITAT=STATIC\n")
            append("GRID_POINTS=").append(points.size).append('\n')
            append("\nRequirements, frozen before the search:\n")
            append("  anti-convergence: entropy>=").append(fx(SpikeContract.MIN_ACTION_TYPE_ENTROPY_BITS))
            append(" distinctObjects/day>=").append(fx(SpikeContract.MIN_DISTINCT_OBJECTS_INSPECTED_PER_DAY))
            append(" maxOccupancy<=").append(fx(SpikeContract.MAX_SINGLE_ACTION_OCCUPANCY))
            append(" revisits/day>=").append(fx(SpikeContract.MIN_REVISITATION_RATE_PER_DAY))
            append(" cycleRegularity<=").append(fx(SpikeContract.MAX_CYCLE_REGULARITY)).append('\n')
            append("  attribution: substantive>=").append(fx(SpikeContract.REQUIRED_SUBSTANTIVE_SPONTANEITY_RATE))
            append(" oscillatorOrTieBreakOnly<=").append(fx(SpikeContract.MAX_OSCILLATOR_TIEBREAK_ONLY_RATE))
            append('\n')
            append("\nGrid (each row is the mean over the whole seed matrix):\n")
            for (p in points) append(p.row()).append('\n')
            append("\nfeasiblePoints=").append(points.count { it.jointlyFeasible })
            append(" robustPoints=").append(robustPoints.size).append('\n')
            append("antiConvergenceOnly=").append(points.count { it.antiConvergenceOk })
            append(" attributionOnly=").append(points.count { it.attributionOk }).append('\n')
            selected?.let {
                append("SELECTED_PARAMETERIZATION=").append(it.parameters.describe()).append('\n')
                append("SELECTED_PARAMETER_HASH=").append(it.parameters.parameterHash()).append('\n')
            }
            append("CURIOSITY_ENVELOPE_FEASIBILITY_RESULT=").append(result.name).append('\n')
        }

        private fun fx(raw: Long) = RunMeasures.fx(raw)
    }

    @JvmStatic
    public fun main(args: Array<String>) {
        println(search().render())
    }

    public fun search(): SearchResult {
        val points = ArrayList<GridPoint>()
        var index = 0
        for (floor in BASE_FLOORS) {
            for (amplitude in AMPLITUDES) {
                for (inhibition in INHIBITION_DEPTHS) {
                    val parameters = CuriosityParameters(
                        baseFloor = floor,
                        contextAmplitude = amplitude,
                        inhibitionDepth = inhibition,
                    )
                    points += evaluate(index++, parameters)
                }
            }
        }

        // Robustness: a feasible point counts only if its immediate neighbours
        // in the grid are feasible too. An isolated point that vanishes under a
        // one-step perturbation is not an operating region.
        val robust = points.filter { it.jointlyFeasible && neighboursFeasible(points, it) }
        val result = when {
            robust.isNotEmpty() -> CuriosityEnvelopeFeasibilityResult.NON_EMPTY_FEASIBLE_REGION
            else -> CuriosityEnvelopeFeasibilityResult.EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE
        }
        // The selected point is the most central robust one, chosen by distance
        // from the grid edges so the operating point sits inside its region.
        val selected = robust.minByOrNull { edgeDistance(it.index) }
        return SearchResult(points, result, robust, selected)
    }

    private fun evaluate(index: Int, parameters: CuriosityParameters): GridPoint {
        var entropy = 0.0
        var distinct = 0.0
        var occupancy = 0.0
        var wakingOccupancy = 0.0
        var revisits = 0.0
        var regularity = 0.0
        var substantive = 0.0
        var oscillator = 0.0
        var scored = 0L

        for (seed in SEED_MATRIX) {
            // One run per seed produces *both* readouts. They cannot come from
            // different parameterizations because there is only one run.
            val r = AcceleratedSimulator.run(
                RunConfig(
                    runId = "ENVELOPE-$index-$seed",
                    cohort = Cohort.FULL,
                    seed = seed,
                    virtualDays = SEARCH_DAYS,
                    condition = HabitatCondition.STATIC,
                    curiosity = parameters,
                    attributionSampleEvery = ATTRIBUTION_SAMPLE_EVERY,
                    windowDays = WINDOW_DAYS,
                ),
            )
            val m = r.measures
            entropy += m.windowActionEntropyBits()
            distinct += m.distinctObjectsInspectedPerDay()
            occupancy += m.maxWindowOccupancy()
            wakingOccupancy += m.maxWakingOccupancy()
            revisits += m.revisitationsPerDay()
            regularity += m.cycleRegularity()
            substantive += m.substantiveSpontaneityRate()
            oscillator += m.oscillatorTieBreakOnlyRate()
            scored += m.spontaneousScored
        }

        val n = SEED_MATRIX.size.toDouble()
        return GridPoint(
            index = index,
            parameters = parameters,
            entropyBits = entropy / n,
            distinctObjectsPerDay = distinct / n,
            maxOccupancy = occupancy / n,
            maxWakingOccupancy = wakingOccupancy / n,
            revisitsPerDay = revisits / n,
            cycleRegularity = regularity / n,
            substantiveRate = substantive / n,
            oscillatorTieBreakOnlyRate = oscillator / n,
            scoredActions = scored,
        )
    }

    /** Grid index decomposes as floor * 9 + amplitude * 3 + inhibition. */
    private fun neighboursFeasible(points: List<GridPoint>, point: GridPoint): Boolean {
        val f = point.index / 9
        val a = (point.index / 3) % 3
        val i = point.index % 3
        var neighbours = 0
        var feasible = 0
        for ((df, da, di) in NEIGHBOUR_STEPS) {
            val nf = f + df
            val na = a + da
            val ni = i + di
            if (nf !in 0..2 || na !in 0..2 || ni !in 0..2) continue
            neighbours += 1
            if (points[nf * 9 + na * 3 + ni].jointlyFeasible) feasible += 1
        }
        return neighbours > 0 && feasible * 2 >= neighbours
    }

    private fun edgeDistance(index: Int): Int {
        val f = index / 9
        val a = (index / 3) % 3
        val i = index % 3
        return minOf(f, 2 - f) + minOf(a, 2 - a) + minOf(i, 2 - i)
    }

    private val NEIGHBOUR_STEPS = listOf(
        Triple(-1, 0, 0), Triple(1, 0, 0),
        Triple(0, -1, 0), Triple(0, 1, 0),
        Triple(0, 0, -1), Triple(0, 0, 1),
    )
}
