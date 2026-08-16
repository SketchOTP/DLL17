package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * These constants are the ones a programme under schedule pressure would relax.
 * Pinning them in a test means relaxing one is a visible, deliberate edit.
 */
class A001StudyContractTest {

    @Test
    fun `the architect frozen constants are exactly as preregistered`() {
        assertEquals(10.0, A001StudyContract.MINIMUM_PAIRED_DIFFERENCE)
        assertEquals(0.95, A001StudyContract.CONFIDENCE_LEVEL)
        assertEquals(0.05, A001StudyContract.PRIMARY_ALPHA)
        assertEquals(0.80, A001StudyContract.PRIMARY_POWER)
        assertEquals(1.25, A001StudyContract.PILOT_SD_INFLATION)
        assertEquals(36, A001StudyContract.VARIANCE_PILOT_PARTICIPANTS)
        assertEquals(40, A001StudyContract.BASELINE_QUALIFICATION_PARTICIPANTS)
        assertEquals(15.0, A001StudyContract.BASELINE_COMPETENCE_MARGIN)
        assertEquals(0.05, A001StudyContract.ABLATION_FAMILY_ALPHA)
    }

    @Test
    fun `the family wide alpha agrees with the frozen spike contract`() {
        assertEquals(
            SpikeContract.ABLATION_FAMILY_ALPHA_MILLIONTHS / 1_000_000.0,
            A001StudyContract.ABLATION_FAMILY_ALPHA,
        )
    }

    @Test
    fun `the session duration is the one the expression contract renders`() {
        assertEquals(SpikeContract.PRIMARY_SESSION_SECONDS, A001StudyContract.SESSION_SECONDS)
        assertEquals(600, A001StudyContract.SESSION_SECONDS)
    }

    @Test
    fun `the preregistered ablation family is three armed and excludes the scripted cohorts`() {
        assertEquals(3, A001StudyContract.ABLATION_FAMILY.size)
        assertEquals(
            listOf(
                Cohort.FULL_MINUS_CURIOSITY_ANTICONVERGENCE,
                Cohort.FULL_MINUS_PREFERENCE_LEARNING,
                Cohort.FULL_MINUS_OUTCOME_UNCERTAINTY,
            ),
            A001StudyContract.ABLATION_FAMILY,
        )
        assertTrue(A001StudyContract.ABLATION_FAMILY.none { it.scripted })
        assertTrue(A001StudyContract.ABLATION_FAMILY.none { it == Cohort.FULL })
    }

    @Test
    fun `each ablation arm removes exactly what its name says and nothing else`() {
        val full = Cohort.FULL.mechanisms
        for (arm in A001StudyContract.ABLATION_FAMILY) {
            val removed = full - arm.mechanisms
            assertTrue(removed.isNotEmpty(), "${arm.cohortId} removes nothing")
            assertTrue(
                arm.mechanisms.all { it in full },
                "${arm.cohortId} adds a mechanism FULL does not carry",
            )
        }
        assertEquals(
            setOf(com.animusmachinae.dll17.research.aliveness.Mechanism.OUTCOME_UNCERTAINTY),
            full - Cohort.FULL_MINUS_OUTCOME_UNCERTAINTY.mechanisms,
        )
    }

    @Test
    fun `the instrument is one item with monotone anchors spanning the whole scale`() {
        val anchors = GradedAlivenessInstrument.ANCHORS
        assertEquals(5, anchors.size)
        assertEquals(A001StudyContract.SCORE_MIN.toInt(), anchors.first().value)
        assertEquals(A001StudyContract.SCORE_MAX.toInt(), anchors.last().value)
        for (i in 1 until anchors.size) {
            assertTrue(anchors[i].value > anchors[i - 1].value, "anchors are not increasing")
        }
        assertTrue(anchors.all { it.label.isNotBlank() && it.description.isNotBlank() })
    }

    @Test
    fun `the instrument asks about the creature and not about the software`() {
        assertTrue(GradedAlivenessInstrument.ITEM.contains("How alive did this creature seem"))
        assertTrue(GradedAlivenessInstrument.PREAMBLE.contains("not about the graphics"))
        // The rater must never be told a cohort exists.
        val text = GradedAlivenessInstrument.render().lowercase()
        for (leak in listOf("full", "baseline", "scripted", "cohort", "ablation", "organism")) {
            assertFalse(text.contains(leak), "the instrument text leaks the word '$leak'")
        }
    }

    @Test
    fun `the unresolved inputs are named rather than left to inference`() {
        val unresolved = A001StudyContract.UNRESOLVED_REAL_WORLD_INPUTS
        assertEquals(5, unresolved.size)
        assertTrue(unresolved.any { it.startsWith("pairedDifferenceSD") })
        assertTrue(unresolved.any { it.startsWith("maxFundableParticipants") })
        assertTrue(unresolved.count { it.contains("unassigned") } == 2)
        assertTrue(unresolved.none { it.contains("BaselineIndependentOwner") })
    }
}
