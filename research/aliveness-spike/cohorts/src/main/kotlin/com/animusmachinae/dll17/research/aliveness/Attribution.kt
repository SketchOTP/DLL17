package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/** The canonical attribution classes. Ordinals are immutable research IDs. */
public enum class AttributionClass(public val classOrdinal: Int) {
    PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE(1),
    LEARNED_PREFERENCE(2),
    EPISODIC_OR_HISTORY(3),
    HABIT_OR_EXPECTANCY(4),
    SOCIAL_OR_RELATIONSHIP_HISTORY(5),
    CURIOSITY_OSCILLATOR_ONLY(6),
    RANDOM_TIEBREAK_ONLY(7),
    MIXED_SUBSTANTIVE(8),
    OTHER_NON_SUBSTANTIVE(9);

    /** True when this class counts toward `SubstantiveSpontaneityRate`. */
    public val substantive: Boolean
        get() = classOrdinal in 1..5 || this == MIXED_SUBSTANTIVE
}

/** Exact coalition attribution for one scored spontaneous action. */
public class AttributionResult(
    public val shapley: LongArray,
    public val totalPositiveMass: Long,
    public val substantiveMass: Long,
    public val oscillatorMass: Long,
    public val substantiveShare: Long,
    public val oscillatorShare: Long,
    public val tieBreakOutcomeDetermining: Boolean,
    public val dominantGroup: MechanismGroup?,
    public val attributionClass: AttributionClass,
    public val grandCoalitionValue: Long,
)

/**
 * `CoalitionValueFunctionV1` and `SpontaneousActionAttributionV1`.
 *
 * The value function is the clamped winning margin. A coalition under which the
 * observed action would not have won contributes zero, never a negative
 * magnitude — so tie-breaking and losing margins cannot manufacture attribution
 * mass. Shapley contributions then allocate the grand coalition's positive
 * winning support across the six frozen mechanism groups, which is what lets an
 * overdetermined action count as substantive rather than unexplained.
 */
public object CoalitionAttribution {

    /** `k!` for the frozen six-group set. Exact enumeration only. */
    private const val FACTORIAL_K: Long = 720L

    private val SUBSET_WEIGHT: LongArray = longArrayOf(120L, 24L, 12L, 12L, 24L, 120L)

    /** `v(S)` for a non-empty coalition; `v(∅) = 0` by contract. */
    public fun coalitionValue(
        observed: Proposal,
        candidates: List<Proposal>,
        mask: Int,
        fx: Fx,
    ): Long {
        if (mask == 0) return FixedPoint.ZERO
        val own = observed.utility(mask, fx)
        var bestOther = Long.MIN_VALUE
        for (p in candidates) {
            if (p === observed) continue
            val u = p.utility(mask, fx)
            if (u > bestOther) bestOther = u
        }
        if (bestOther == Long.MIN_VALUE) return FixedPoint.ZERO
        val margin = fx.sub(own, bestOther)
        // Equality at zero margin is a tie, not a win: it contributes no value.
        return if (margin > FixedPoint.ZERO) margin else FixedPoint.ZERO
    }

    /**
     * Exact Shapley contributions. With `k = 6` this is 64 coalition evaluations,
     * which the canonical plan requires rather than a sampled approximation.
     */
    public fun attribute(
        observed: Proposal,
        candidates: List<Proposal>,
        tieBreakOutcomeDetermining: Boolean,
        fx: Fx,
    ): AttributionResult {
        val k = MechanismGroup.COUNT
        val values = LongArray(1 shl k)
        for (mask in values.indices) {
            values[mask] = coalitionValue(observed, candidates, mask, fx)
        }

        val shapley = LongArray(k)
        for (g in 0 until k) {
            var weighted = 0L
            val bit = 1 shl g
            for (mask in values.indices) {
                if (mask and bit != 0) continue
                val size = Integer.bitCount(mask)
                val delta = values[mask or bit] - values[mask]
                weighted += SUBSET_WEIGHT[size] * delta
            }
            shapley[g] = divideRoundHalfAway(weighted, FACTORIAL_K)
        }

        var totalPositive = 0L
        var substantive = 0L
        for (g in 0 until k) {
            val v = shapley[g]
            if (v <= 0L) continue
            totalPositive += v
            if (MechanismGroup.ALL[g] != MechanismGroup.CURIOSITY_OSCILLATOR) substantive += v
        }
        val oscillator = maxOf(0L, shapley[MechanismGroup.CURIOSITY_OSCILLATOR.groupOrdinal])

        val substantiveShare =
            if (totalPositive == 0L) FixedPoint.ZERO else fx.div(substantive, totalPositive)
        val oscillatorShare =
            if (totalPositive == 0L) FixedPoint.ZERO else fx.div(oscillator, totalPositive)

        var dominant: MechanismGroup? = null
        if (totalPositive > 0L) {
            for (g in MechanismGroup.SUBSTANTIVE) {
                val share = fx.div(maxOf(0L, shapley[g.groupOrdinal]), totalPositive)
                if (share >= SpikeContract.DOMINANT_GROUP_SHARE) {
                    dominant = g
                    break
                }
            }
        }

        val cls = classify(
            totalPositive = totalPositive,
            substantiveShare = substantiveShare,
            oscillatorShare = oscillatorShare,
            shapley = shapley,
            dominant = dominant,
            tieBreakOutcomeDetermining = tieBreakOutcomeDetermining,
            fx = fx,
        )

        return AttributionResult(
            shapley = shapley,
            totalPositiveMass = totalPositive,
            substantiveMass = substantive,
            oscillatorMass = oscillator,
            substantiveShare = substantiveShare,
            oscillatorShare = oscillatorShare,
            tieBreakOutcomeDetermining = tieBreakOutcomeDetermining,
            dominantGroup = dominant,
            attributionClass = cls,
            grandCoalitionValue = values[values.size - 1],
        )
    }

    private fun classify(
        totalPositive: Long,
        substantiveShare: Long,
        oscillatorShare: Long,
        shapley: LongArray,
        dominant: MechanismGroup?,
        tieBreakOutcomeDetermining: Boolean,
        fx: Fx,
    ): AttributionClass {
        // No coalition supports the observed action at all: the tie-break, or
        // nothing identifiable, produced it. Either way it is not substantive.
        if (totalPositive == 0L) {
            return if (tieBreakOutcomeDetermining) {
                AttributionClass.RANDOM_TIEBREAK_ONLY
            } else {
                AttributionClass.OTHER_NON_SUBSTANTIVE
            }
        }
        if (tieBreakOutcomeDetermining && substantiveShare < SpikeContract.SUBSTANTIVE_SHARE_FLOOR) {
            return AttributionClass.RANDOM_TIEBREAK_ONLY
        }
        if (oscillatorShare >= SpikeContract.OSCILLATOR_DOMINANCE_SHARE &&
            substantiveShare < SpikeContract.SUBSTANTIVE_SHARE_FLOOR
        ) {
            return AttributionClass.CURIOSITY_OSCILLATOR_ONLY
        }
        if (substantiveShare < SpikeContract.SUBSTANTIVE_SHARE_FLOOR) {
            return AttributionClass.OTHER_NON_SUBSTANTIVE
        }
        if (dominant != null) {
            return when (dominant) {
                MechanismGroup.PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE ->
                    AttributionClass.PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE
                MechanismGroup.LEARNED_PREFERENCE -> AttributionClass.LEARNED_PREFERENCE
                MechanismGroup.EPISODIC_OR_HISTORY -> AttributionClass.EPISODIC_OR_HISTORY
                MechanismGroup.HABIT_OR_EXPECTANCY -> AttributionClass.HABIT_OR_EXPECTANCY
                MechanismGroup.SOCIAL_OR_RELATIONSHIP_HISTORY ->
                    AttributionClass.SOCIAL_OR_RELATIONSHIP_HISTORY
                MechanismGroup.CURIOSITY_OSCILLATOR -> AttributionClass.CURIOSITY_OSCILLATOR_ONLY
            }
        }
        // No single group dominates. Overdetermination counts as substantive
        // when at least two substantive groups each carry qualified mass.
        val qualified = MechanismGroup.SUBSTANTIVE.count { g ->
            val share = fx.div(maxOf(0L, shapley[g.groupOrdinal]), totalPositive)
            share >= SpikeContract.MIXED_SUBSTANTIVE_MIN_SHARE
        }
        return if (qualified >= 2) {
            AttributionClass.MIXED_SUBSTANTIVE
        } else {
            AttributionClass.OTHER_NON_SUBSTANTIVE
        }
    }

    private fun divideRoundHalfAway(numerator: Long, denominator: Long): Long {
        val half = denominator / 2L
        return if (numerator >= 0L) {
            (numerator + half) / denominator
        } else {
            -((-numerator + half) / denominator)
        }
    }
}
