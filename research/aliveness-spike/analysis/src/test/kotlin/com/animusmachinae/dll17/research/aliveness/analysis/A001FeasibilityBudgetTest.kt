package com.animusmachinae.dll17.research.aliveness.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class A001FeasibilityBudgetTest {

    private fun release(sd: Double, valid: Boolean = true): BlindVariancePilot.PilotRelease {
        val n = A001StudyContract.VARIANCE_PILOT_PARTICIPANTS
        val raw = DoubleArray(n) { Statistics.normalQuantile((it + 0.5) / n) }
        val rawSd = Statistics.sampleSd(raw)
        val records = (0 until if (valid) n else n - 2).map { i ->
            val d = 5.0 + sd * raw[i] / rawSd
            BlindVariancePilot.PilotRecord("S-$i", 50.0 + d / 2.0, 50.0 - d / 2.0)
        }
        return BlindVariancePilot.release(records)
    }

    @Test
    fun `no pilot means no number`() {
        val result = A001FeasibilityBudget.compute(null, null)
        assertEquals(
            A001FeasibilityBudget.FeasibilityState.BLOCKED_SPEC_PAIRED_DIFFERENCE_SD,
            result.state,
        )
        assertNull(result.requiredPairsPrimary)
        assertNull(result.totalRequiredParticipants)
    }

    @Test
    fun `an invalid pilot release is refused rather than used`() {
        val result = A001FeasibilityBudget.compute(release(12.0, valid = false), null)
        assertEquals(
            A001FeasibilityBudget.FeasibilityState.BLOCKED_SPEC_PAIRED_DIFFERENCE_SD,
            result.state,
        )
        assertNull(result.requiredPairsPrimary)
    }

    @Test
    fun `a valid pilot without an owner ceiling computes the sample but stays blocked`() {
        val result = A001FeasibilityBudget.compute(release(14.0), null)
        assertEquals(
            A001FeasibilityBudget.FeasibilityState.BLOCKED_SPEC_STUDY_BUDGET,
            result.state,
        )
        assertTrue(result.requiredPairsPrimary!! > 0)
        assertNull(result.ceiling)
    }

    @Test
    fun `the SD is inflated by the frozen factor before powering`() {
        val r = release(14.0)
        val result = A001FeasibilityBudget.compute(r, null)
        val inflated = result.inflatedSd!!
        assertEquals(
            r.pairedDifferenceSd * A001StudyContract.PILOT_SD_INFLATION,
            inflated,
            1e-12,
        )
        assertEquals(
            Statistics.pairedTSampleSize(
                A001StudyContract.MINIMUM_PAIRED_DIFFERENCE,
                inflated,
                A001StudyContract.PRIMARY_ALPHA,
                A001StudyContract.PRIMARY_POWER,
            ),
            result.requiredPairsPrimary,
        )
    }

    @Test
    fun `ablation arms are powered at the corrected level and so need more pairs`() {
        val result = A001FeasibilityBudget.compute(release(14.0), null)
        val primary = result.requiredPairsPrimary!!
        val perArm = result.requiredPairsPerAblationArm!!
        assertTrue(
            perArm > primary,
            "an arm tested at alpha/3 cannot need fewer pairs than one tested at alpha",
        )
        assertEquals(
            primary + A001StudyContract.ABLATION_FAMILY.size * perArm,
            result.totalRequiredParticipants,
        )
    }

    @Test
    fun `a requirement that exceeds the ceiling is not feasible and is not trimmed`() {
        val r = release(14.0)
        val tight = A001FeasibilityBudget.compute(
            r,
            A001FeasibilityBudget.OwnerCeiling(40, 20.0),
        )
        assertEquals(A001FeasibilityBudget.FeasibilityState.A001_NOT_FEASIBLE, tight.state)
        val generous = A001FeasibilityBudget.compute(
            r,
            A001FeasibilityBudget.OwnerCeiling(100_000, 1.0e9),
        )
        assertEquals(A001FeasibilityBudget.FeasibilityState.A001_FEASIBLE, generous.state)
        // The requirement is a property of the pilot and the frozen constants,
        // not of the budget it is compared against.
        assertEquals(tight.totalRequiredParticipants, generous.totalRequiredParticipants)
    }

    @Test
    fun `either half of the ceiling can make the attempt infeasible on its own`() {
        val r = release(14.0)
        val computed = A001FeasibilityBudget.compute(r, null)
        val participants = computed.totalRequiredParticipants!!
        val hours = computed.totalRequiredParticipantHours!!
        assertEquals(
            A001FeasibilityBudget.FeasibilityState.A001_NOT_FEASIBLE,
            A001FeasibilityBudget.compute(
                r,
                A001FeasibilityBudget.OwnerCeiling(participants - 1, hours + 100.0),
            ).state,
        )
        assertEquals(
            A001FeasibilityBudget.FeasibilityState.A001_NOT_FEASIBLE,
            A001FeasibilityBudget.compute(
                r,
                A001FeasibilityBudget.OwnerCeiling(participants + 100, hours - 1.0),
            ).state,
        )
    }

    @Test
    fun `participant hours are built from the itemized protocol schedule`() {
        assertEquals(
            A001FeasibilityBudget.CONSENT_SECONDS +
                A001FeasibilityBudget.BRIEFING_SECONDS +
                2 * A001StudyContract.SESSION_SECONDS +
                2 * A001FeasibilityBudget.INSTRUMENT_SECONDS_PER_ADMINISTRATION +
                A001FeasibilityBudget.SECONDARY_ITEMS_SECONDS +
                A001FeasibilityBudget.DEBRIEF_SECONDS,
            A001FeasibilityBudget.PARTICIPANT_SECONDS,
        )
        val result = A001FeasibilityBudget.compute(release(14.0), null)
        assertEquals(
            result.totalRequiredParticipants!! *
                A001FeasibilityBudget.PARTICIPANT_SECONDS / 3600.0,
            result.totalRequiredParticipantHours!!,
            1e-9,
        )
    }

    @Test
    fun `the rendered budget names the unresolved inputs rather than defaulting them`() {
        val rendered = A001FeasibilityBudget.compute(null, null).render()
        assertTrue(rendered.contains("ownerCeiling=UNRESOLVED"))
        assertTrue(rendered.contains("releasedSd=UNAVAILABLE"))
        assertTrue(rendered.contains("STATE=BLOCKED_SPEC_PAIRED_DIFFERENCE_SD"))
    }
}
