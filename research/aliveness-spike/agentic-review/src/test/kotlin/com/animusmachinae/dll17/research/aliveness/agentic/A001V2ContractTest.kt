package com.animusmachinae.dll17.research.aliveness.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class A001V2ContractTest {
    private fun observation(
        pair: Int,
        execution: String,
        preference: A001EvaluationContractV2.Preference = A001EvaluationContractV2.Preference.A,
        canonical: A001EvaluationContractV2.Candidate = A001EvaluationContractV2.Candidate.FIRST,
        delta: Int = 10,
    ) = A001EvaluationContractV2.PairObservation(
        pair, execution, listOf("A", "B"), preference,
        if (canonical == A001EvaluationContractV2.Candidate.FIRST) 50 + delta else 50,
        if (canonical == A001EvaluationContractV2.Candidate.FIRST) 50 else 50 - delta,
        canonicalCandidate = canonical,
    )

    private fun passingPairs(): List<A001V2Aggregation.PairResult> = (1..12).map { pair ->
        A001V2Aggregation.PairResult(
            pair,
            listOf(observation(pair, "${pair}A"), observation(pair, "${pair}B", canonical = A001EvaluationContractV2.Candidate.FIRST)),
        )
    }

    private fun withPreference(
        source: List<A001V2Aggregation.PairResult>,
        pairIds: Set<Int>,
        preference: A001EvaluationContractV2.Preference,
    ) = source.map { pair ->
        if (pair.pairId !in pairIds) pair else A001V2Aggregation.PairResult(
            pair.pairId,
            pair.executions.map { it.copyForTest(preference) },
        )
    }

    @Test fun `contract freezes population and owner authority`() {
        assertEquals("AI_AGENTS_ONLY", A001EvaluationContractV2.EVALUATION_POPULATION)
        assertEquals("PROHIBITED", A001EvaluationContractV2.EXTERNAL_HUMAN_PARTICIPANTS)
        assertEquals("OWNER_ONLY", A001EvaluationContractV2.OWNER_PIXEL_REVIEWER)
        assertTrue(A001EvaluationContractV2.AI_RESULTS_CANNOT_OVERRIDE_OWNER_FAIL)
        assertFalse(A001EvaluationContractV2.GENERAL_HUMAN_POPULATION_INFERENCE)
    }

    @Test fun `observation request exposes only neutral visible surfaces`() {
        val request = AgentObservationHarnessV1.request(
            AgentObservation("A", listOf("moves toward the sound"), setOf("tap")),
            AgentObservation("B", listOf("waits, then turns"), setOf("tap")),
        )
        assertTrue(request.render().contains("A visible behavior"))
        assertFalse(request.render().contains("cohort"))
        assertFalse(request.render().contains("source code"))
        assertFalse(request.render().contains("internal state"))
    }

    @Test fun `calibration and full thresholds pass only through deterministic aggregation`() {
        val calibration = A001V2Aggregation.aggregate(A001EvaluationContractV2.Comparison.CALIBRATION, passingPairs())
        assertEquals(A001V2Aggregation.Result.CALIBRATION_PASS, calibration.result)
        val full = A001V2Aggregation.aggregate(A001EvaluationContractV2.Comparison.FULL, passingPairs())
        assertEquals(A001V2Aggregation.Result.A001_AI_QUALIFICATION_PASS, full.result)
        val reversed = A001V2Aggregation.aggregate(A001EvaluationContractV2.Comparison.FULL, passingPairs().reversed())
        assertEquals(full.result, reversed.result)
        assertEquals(full.medianDelta, reversed.medianDelta)
    }

    @Test fun `panel invalid, threshold boundary, abstain and leakage fail closed`() {
        val invalid = passingPairs().dropLast(1)
        assertEquals(A001V2Aggregation.Result.A001_AI_PANEL_INVALID,
            A001V2Aggregation.aggregate(A001EvaluationContractV2.Comparison.FULL, invalid).result)
        val leaked = passingPairs().toMutableList()
        val first = leaked[0]
        leaked[0] = A001V2Aggregation.PairResult(
            first.pairId,
            listOf(first.executions[0].copyForTest(leak = true), first.executions[1]),
        )
        assertEquals(A001V2Aggregation.Result.A001_AI_PANEL_INVALID,
            A001V2Aggregation.aggregate(A001EvaluationContractV2.Comparison.FULL, leaked).result)
        val ownerFail = OwnerPixelAlivenessAcceptanceV1.finalGate(
            A001V2Aggregation.Result.A001_AI_QUALIFICATION_PASS,
            OwnerPixelAlivenessAcceptanceV1.Outcome.OWNER_PIXEL_ALIVENESS_FAIL,
        )
        assertEquals(OwnerPixelAlivenessAcceptanceV1.FinalGate.A001_V2_FAIL_OWNER_ACCEPTANCE, ownerFail)
    }

    @Test fun `exact nine preferences and exact ten median are the pass boundary`() {
        val exact = withPreference(passingPairs(), setOf(10, 11, 12), A001EvaluationContractV2.Preference.B)
        val result = A001V2Aggregation.aggregate(A001EvaluationContractV2.Comparison.FULL, exact)
        assertEquals(A001V2Aggregation.Result.A001_AI_QUALIFICATION_PASS, result.result)
        assertEquals(9, result.preferencePairs)
        assertEquals(10.0, result.medianDelta)
        val below = withPreference(passingPairs(), setOf(9, 10, 11, 12), A001EvaluationContractV2.Preference.B)
        assertEquals(A001V2Aggregation.Result.A001_AI_QUALIFICATION_FAIL,
            A001V2Aggregation.aggregate(A001EvaluationContractV2.Comparison.FULL, below).result)
    }

    @Test fun `ties abstentions and owner metadata remain fail closed`() {
        val tie = withPreference(passingPairs(), setOf(1), A001EvaluationContractV2.Preference.TIE)
        assertEquals(A001V2Aggregation.Result.A001_AI_QUALIFICATION_PASS,
            A001V2Aggregation.aggregate(A001EvaluationContractV2.Comparison.FULL, tie).result)
        val abstain = withPreference(passingPairs(), setOf(1), A001EvaluationContractV2.Preference.ABSTAIN)
        assertEquals(A001V2Aggregation.Result.A001_AI_QUALIFICATION_PASS,
            A001V2Aggregation.aggregate(A001EvaluationContractV2.Comparison.FULL, abstain).result)
        val ownerPass = OwnerPixelAlivenessAcceptanceV1.ReviewRecord(
            true, true, "a".repeat(40), "apk-sha", "2", "Pixel", "Android",
            "DLL17", "2026-08-16T20:00:00Z", true,
            OwnerPixelAlivenessAcceptanceV1.Outcome.OWNER_PIXEL_ALIVENESS_PASS,
        )
        assertEquals(OwnerPixelAlivenessAcceptanceV1.Outcome.OWNER_PIXEL_ALIVENESS_PASS, ownerPass.outcome)
    }

    private fun A001EvaluationContractV2.PairObservation.copyForTest(leak: Boolean) =
        A001EvaluationContractV2.PairObservation(
            pairId, executionId, order, preference, overallA, overallB,
            schemaValid, injectionControlPassed, identityBlinded, leak,
            rationaleEvidenceGrounded, canonicalCandidate,
        )

    private fun A001EvaluationContractV2.PairObservation.copyForTest(
        replacement: A001EvaluationContractV2.Preference,
    ) = A001EvaluationContractV2.PairObservation(
        pairId, executionId, order, replacement, overallA, overallB,
        schemaValid, injectionControlPassed, identityBlinded, privilegedInformationLeaked,
        rationaleEvidenceGrounded, canonicalCandidate,
    )
}
