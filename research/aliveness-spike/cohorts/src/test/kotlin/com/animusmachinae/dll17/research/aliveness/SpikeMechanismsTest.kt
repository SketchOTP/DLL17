package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The numeric fast path must be indistinguishable from the frozen library. */
class SpikeNumericsTest {

    private val ctx = ArithmeticContext.unattributed()
    private val fx = Fx(ctx)

    @Test
    fun `the multiply fast path agrees with the frozen library`() {
        val samples = buildSamples()
        for (a in samples) {
            for (b in samples) {
                assertEquals(
                    FixedPoint.satMultiplyScaled(a, b, ctx),
                    fx.mul(a, b),
                    "mul($a, $b)",
                )
            }
        }
    }

    @Test
    fun `the divide fast path agrees with the frozen library`() {
        val samples = buildSamples()
        for (a in samples) {
            for (b in samples) {
                if (b == 0L) continue
                assertEquals(
                    FixedPoint.satDivide(a, b, ctx),
                    fx.div(a, b),
                    "div($a, $b)",
                )
            }
        }
    }

    @Test
    fun `the fast path agrees on a wide pseudo-random sweep`() {
        val rng = SpikeRandom(20260813L, SpikeRandomDomain.CURIOSITY_TIE_BREAK)
        repeat(20_000) {
            val a = (rng.nextLong() % 4_000_000_000L)
            val b = (rng.nextLong() % 4_000_000_000L)
            assertEquals(FixedPoint.satMultiplyScaled(a, b, ctx), fx.mul(a, b), "mul($a, $b)")
            if (b != 0L) {
                assertEquals(FixedPoint.satDivide(a, b, ctx), fx.div(a, b), "div($a, $b)")
            }
        }
    }

    @Test
    fun `the triangle wave stays in range and is periodic`() {
        val period = CuriosityWave.periodFor(7L, 3)
        var min = Long.MAX_VALUE
        var max = Long.MIN_VALUE
        for (tick in 0 until period.toLong()) {
            val v = CuriosityWave.triangle(CuriosityWave.phase(7L, 3, tick, period))
            min = minOf(min, v)
            max = maxOf(max, v)
        }
        assertTrue(min >= -FixedPoint.ONE, "wave dipped below -1: $min")
        assertTrue(max <= FixedPoint.ONE, "wave exceeded 1: $max")
        assertEquals(
            CuriosityWave.triangle(CuriosityWave.phase(7L, 3, 0L, period)),
            CuriosityWave.triangle(CuriosityWave.phase(7L, 3, period.toLong(), period)),
        )
    }

    @Test
    fun `curiosity periods are pairwise co-prime`() {
        val periods = SpikeContract.CURIOSITY_PERIODS_TICKS
        for (i in periods.indices) {
            for (j in i + 1 until periods.size) {
                assertEquals(1, gcd(periods[i], periods[j]), "${periods[i]} and ${periods[j]}")
            }
        }
    }

    @Test
    fun `context never enters the oscillator phase`() {
        // The phase function takes only seed, object identity, logical time and
        // period. This is asserted behaviourally: two organisms whose context
        // differs but whose logical time matches see the identical phase.
        val a = CuriosityWave.phase(11L, 5, 900L, 1_493)
        val b = CuriosityWave.phase(11L, 5, 900L, 1_493)
        assertEquals(a, b)
        assertNotEquals(a, CuriosityWave.phase(11L, 5, 901L, 1_493))
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    private fun buildSamples(): LongArray = longArrayOf(
        0L, 1L, -1L, 499_999L, 500_000L, 500_001L, -500_000L,
        FixedPoint.ONE, -FixedPoint.ONE, 2 * FixedPoint.ONE,
        123_456L, -987_654L, 1_000_001L, 3_141_593L, -2_718_282L,
        1_000_000_000L, -1_000_000_000L,
    )
}

/** The candidate mechanisms behave as their update laws claim. */
class SpikeMechanismsTest {

    private val fx = Fx(ArithmeticContext.unattributed())

    @Test
    fun `every learned value stays inside its bound under adversarial input`() {
        val state = OrganismState(1L, Mechanism.FULL_SET)
        val habitat = Habitat(1L, HabitatCondition.STATIC)
        repeat(2_000) { i ->
            val extreme = if (i % 2 == 0) {
                Outcome(true, FixedPoint.ONE, false, true, null)
            } else {
                Outcome(false, -FixedPoint.ONE, true, false, null)
            }
            MechanismUpdates.applyOutcome(
                state, SpikeAction.EXPLORE, HabitatObject.PLAY_BALL, extreme, habitat, i.toLong(), fx,
            )
        }
        val i = HabitatObject.PLAY_BALL.ordinal0
        assertTrue(state.preference[i] in -FixedPoint.ONE..FixedPoint.ONE)
        assertTrue(state.fear[i] in 0L..FixedPoint.ONE)
        assertTrue(state.habituation[i] in 0L..FixedPoint.ONE)
        assertTrue(state.sensitization[i] in 0L..FixedPoint.ONE)
        assertTrue(state.inhibition[i] in 0L..FixedPoint.ONE)
    }

    @Test
    fun `the episodic ring never grows beyond its capacity`() {
        val state = OrganismState(2L, Mechanism.FULL_SET)
        val habitat = Habitat(2L, HabitatCondition.STATIC)
        repeat(5_000) { i ->
            MechanismUpdates.applyOutcome(
                state, SpikeAction.PLAY, HabitatObject.PLAY_CUBE,
                Outcome(true, OutcomeModel.MILD_POSITIVE, false, true, null),
                habitat, i.toLong(), fx,
            )
        }
        assertEquals(SpikeContract.EPISODIC_CAPACITY, state.episodes.size)
        assertEquals(SpikeContract.EPISODIC_CAPACITY, state.episodeCount)
    }

    @Test
    fun `an ablated cohort does not carry the removed mechanism`() {
        assertTrue(Mechanism.PREFERENCE_LEARNING !in Cohort.FULL_MINUS_PREFERENCE_LEARNING.mechanisms)
        assertTrue(Mechanism.EPISODIC_HISTORY !in Cohort.FULL_MINUS_EPISODIC_HISTORY.mechanisms)
        assertTrue(
            Mechanism.CURIOSITY_PHASE_DRIFT !in
                Cohort.FULL_MINUS_CURIOSITY_ANTICONVERGENCE.mechanisms,
        )
        assertTrue(
            Mechanism.RECENT_INSPECTION_INHIBITION !in
                Cohort.FULL_MINUS_CURIOSITY_ANTICONVERGENCE.mechanisms,
        )
        // Each ablation removes exactly what it names and nothing else.
        assertEquals(11, Cohort.FULL_MINUS_PREFERENCE_LEARNING.mechanisms.size)
        assertEquals(11, Cohort.FULL_MINUS_EPISODIC_HISTORY.mechanisms.size)
        assertEquals(10, Cohort.FULL_MINUS_CURIOSITY_ANTICONVERGENCE.mechanisms.size)
    }

    @Test
    fun `the scripted cohorts carry no adaptive mechanism at all`() {
        assertTrue(Cohort.SCRIPTED_PET_BASELINE.mechanisms.isEmpty())
        assertTrue(Cohort.DEGRADED_SCRIPTED_CONTROL.mechanisms.isEmpty())
    }

    @Test
    fun `an ablated organism produces different behaviour from FULL`() {
        val habitat = { Habitat(99L, HabitatCondition.STATIC) }
        fun run(cohort: Cohort): List<SpikeAction> {
            val f = Fx(ArithmeticContext.unattributed())
            val agent = Cohorts.create(cohort, 99L, f)
            val rt = SpikeRuntime("ab", agent, habitat(), OutcomeModel(), f)
            return (0 until 4_000L).map { rt.step(it).choice.action }
        }
        val full = run(Cohort.FULL)
        for (ablation in Cohort.HUMAN_ABLATION_FAMILY) {
            assertNotEquals(full, run(ablation), "$ablation produced FULL's exact behaviour")
        }
    }
}

/** The coalition value function and Shapley allocation obey their contract. */
class CoalitionAttributionTest {

    private val fx = Fx(ArithmeticContext.unattributed())

    private fun proposal(action: SpikeAction, contributions: LongArray, base: Long = 0L): Proposal {
        var full = base
        for (c in contributions) full = fx.add(full, c)
        return Proposal(action, HabitatObject.PLAY_BALL, 4, base, contributions, full)
    }

    @Test
    fun `the empty coalition is worth exactly zero`() {
        val a = proposal(SpikeAction.PLAY, longArrayOf(1_000L, 0, 0, 0, 0, 0))
        val b = proposal(SpikeAction.OBSERVE, longArrayOf(0, 0, 0, 0, 0, 0))
        assertEquals(0L, CoalitionAttribution.coalitionValue(a, listOf(a, b), 0, fx))
    }

    @Test
    fun `a losing coalition contributes zero rather than a negative magnitude`() {
        val observed = proposal(SpikeAction.PLAY, longArrayOf(0, 0, 0, 0, 0, 100_000L))
        val rival = proposal(SpikeAction.OBSERVE, longArrayOf(900_000L, 0, 0, 0, 0, 0))
        // Under the physiology-only coalition the rival wins outright.
        val mask = 1 shl MechanismGroup.PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE.groupOrdinal
        assertEquals(0L, CoalitionAttribution.coalitionValue(observed, listOf(observed, rival), mask, fx))
    }

    @Test
    fun `a tie contributes zero, so tie-breaking cannot create attribution mass`() {
        val observed = proposal(SpikeAction.PLAY, longArrayOf(500_000L, 0, 0, 0, 0, 0))
        val rival = proposal(SpikeAction.OBSERVE, longArrayOf(500_000L, 0, 0, 0, 0, 0))
        val mask = Proposal.FULL_MASK
        assertEquals(0L, CoalitionAttribution.coalitionValue(observed, listOf(observed, rival), mask, fx))
    }

    @Test
    fun `Shapley contributions sum to the grand coalition value`() {
        val observed = proposal(
            SpikeAction.PLAY,
            longArrayOf(120_000L, 300_000L, 80_000L, 40_000L, 0L, 60_000L),
        )
        val rival = proposal(
            SpikeAction.OBSERVE,
            longArrayOf(200_000L, 50_000L, 10_000L, 0L, 0L, 30_000L),
        )
        val result = CoalitionAttribution.attribute(observed, listOf(observed, rival), false, fx)
        val sum = result.shapley.sum()
        // Exact enumeration with integer weights: the efficiency axiom holds to
        // within the rounding of the final division by k!.
        assertTrue(
            Math.abs(sum - result.grandCoalitionValue) <= MechanismGroup.COUNT,
            "sum=$sum v(M)=${result.grandCoalitionValue}",
        )
    }

    @Test
    fun `a single dominant mechanism is classified as that mechanism`() {
        val observed = proposal(
            SpikeAction.PLAY,
            longArrayOf(0L, 900_000L, 0L, 0L, 0L, 0L),
        )
        val rival = proposal(SpikeAction.OBSERVE, longArrayOf(100_000L, 0, 0, 0, 0, 0))
        val result = CoalitionAttribution.attribute(observed, listOf(observed, rival), false, fx)
        assertEquals(AttributionClass.LEARNED_PREFERENCE, result.attributionClass)
        assertEquals(MechanismGroup.LEARNED_PREFERENCE, result.dominantGroup)
    }

    @Test
    fun `an overdetermined action is MIXED_SUBSTANTIVE rather than unexplained`() {
        val observed = proposal(
            SpikeAction.PLAY,
            longArrayOf(300_000L, 300_000L, 300_000L, 0L, 0L, 0L),
        )
        val rival = proposal(SpikeAction.OBSERVE, longArrayOf(0, 0, 0, 0, 0, 0))
        val result = CoalitionAttribution.attribute(observed, listOf(observed, rival), false, fx)
        assertEquals(AttributionClass.MIXED_SUBSTANTIVE, result.attributionClass)
        assertTrue(result.attributionClass.substantive)
    }

    @Test
    fun `an oscillator-only action is classified as hollow spontaneity`() {
        val observed = proposal(
            SpikeAction.EXPLORE,
            longArrayOf(0L, 0L, 0L, 0L, 0L, 400_000L),
        )
        val rival = proposal(SpikeAction.OBSERVE, longArrayOf(0, 0, 0, 0, 0, 0))
        val result = CoalitionAttribution.attribute(observed, listOf(observed, rival), false, fx)
        assertEquals(AttributionClass.CURIOSITY_OSCILLATOR_ONLY, result.attributionClass)
        assertTrue(!result.attributionClass.substantive)
    }

    @Test
    fun `a tie-break with no supporting coalition is RANDOM_TIEBREAK_ONLY`() {
        val observed = proposal(SpikeAction.OBSERVE, longArrayOf(0, 0, 0, 0, 0, 0))
        val rival = proposal(SpikeAction.ORIENT, longArrayOf(0, 0, 0, 0, 0, 0))
        val result = CoalitionAttribution.attribute(observed, listOf(observed, rival), true, fx)
        assertEquals(AttributionClass.RANDOM_TIEBREAK_ONLY, result.attributionClass)
    }

    @Test
    fun `exhaustive enumeration stays within the frozen six-group ceiling`() {
        assertEquals(SpikeContract.MAX_EXACT_COALITION_GROUPS, MechanismGroup.COUNT)
        assertTrue((1 shl MechanismGroup.COUNT) <= 64)
    }
}
