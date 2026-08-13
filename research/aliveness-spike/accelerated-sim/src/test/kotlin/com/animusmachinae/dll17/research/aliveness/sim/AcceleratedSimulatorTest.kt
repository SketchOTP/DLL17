package com.animusmachinae.dll17.research.aliveness.sim

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Fast structural checks on the simulator.
 *
 * The full A000 fixture set takes minutes and is verified by comparing its
 * digest against the frozen constant in CI, in the same way R001 and R002 are
 * verified. These tests exist so that a build breaks in seconds rather than
 * minutes when something obvious goes wrong.
 */
class AcceleratedSimulatorTest {

    private fun config(cohort: Cohort, seed: Long, days: Int = 3) = RunConfig(
        runId = "unit", cohort = cohort, seed = seed, virtualDays = days,
        condition = HabitatCondition.STATIC, attributionSampleEvery = 10,
    )

    @Test
    fun `a run is reproducible bit for bit`() {
        val a = AcceleratedSimulator.run(config(Cohort.FULL, 5L), recordActions = true)
        val b = AcceleratedSimulator.run(config(Cohort.FULL, 5L), recordActions = true)
        assertEquals(a.finalStateSignature, b.finalStateSignature)
        assertTrue(a.actionsByTick!!.contentEquals(b.actionsByTick!!))
        assertEquals(a.measures.actionCounts.toList(), b.measures.actionCounts.toList())
    }

    @Test
    fun `different seeds produce different organisms`() {
        val a = AcceleratedSimulator.run(config(Cohort.FULL, 5L))
        val b = AcceleratedSimulator.run(config(Cohort.FULL, 6L))
        assertNotEquals(a.finalStateSignature, b.finalStateSignature)
    }

    @Test
    fun `no arithmetic overflow occurs in a short run of any cohort`() {
        for (cohort in Cohort.entries) {
            val r = AcceleratedSimulator.run(config(cohort, 12L))
            assertEquals(0L, r.overflowCount, "${cohort.cohortId} overflowed arithmetic")
        }
    }

    @Test
    fun `state footprint does not grow with run length`() {
        val short = AcceleratedSimulator.run(config(Cohort.FULL, 8L, days = 1))
        val long = AcceleratedSimulator.run(config(Cohort.FULL, 8L, days = 10))
        assertEquals(short.stateFootprintSlots, long.stateFootprintSlots)
    }

    @Test
    fun `every scored spontaneous action is attributed`() {
        val r = AcceleratedSimulator.run(
            RunConfig(
                "unit-attr", Cohort.FULL, 21L, 5, HabitatCondition.STATIC,
                attributionSampleEvery = 3, keepTraces = 5,
            ),
        )
        assertTrue(r.measures.spontaneousScored > 0L, "no spontaneous action was scored")
        assertEquals(
            r.measures.spontaneousScored,
            r.measures.attributionCounts.sum(),
            "a scored action was not classified",
        )
        assertTrue(r.traces.isNotEmpty())
        assertTrue(r.traces.all { it.attribution != null && it.spontaneous })
    }

    @Test
    fun `the golden digest constant is a real digest`() {
        assertEquals(64, A000QualificationKernel.GOLDEN_EVIDENCE_DIGEST.length)
        assertTrue(A000QualificationKernel.GOLDEN_EVIDENCE_DIGEST.all { it in "0123456789abcdef" })
        assertNotEquals(
            "0".repeat(64),
            A000QualificationKernel.GOLDEN_EVIDENCE_DIGEST,
            "the digest placeholder was never replaced with a measured value",
        )
    }
}
