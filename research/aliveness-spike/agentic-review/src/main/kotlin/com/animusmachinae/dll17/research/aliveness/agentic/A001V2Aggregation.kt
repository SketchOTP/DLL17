package com.animusmachinae.dll17.research.aliveness.agentic

/** Fail-closed, order-independent Stage 1 aggregation. */
public object A001V2Aggregation {
    public enum class Result { CALIBRATION_PASS, AI_EVALUATOR_CALIBRATION_FAIL, A001_AI_QUALIFICATION_PASS, A001_AI_QUALIFICATION_FAIL, A001_AI_PANEL_INVALID }

    public class PairResult(
        public val pairId: Int,
        public val executions: List<A001EvaluationContractV2.PairObservation>,
    )

    public class Aggregate(
        public val result: Result,
        public val schemaValidPairs: Int,
        public val positionConsistentPairs: Int,
        public val preferencePairs: Int,
        public val medianDelta: Double,
        public val reason: String,
    )

    public fun aggregate(comparison: A001EvaluationContractV2.Comparison, input: List<PairResult>): Aggregate {
        val pairs = input.sortedBy { it.pairId }
        val valid = pairs.filter { pair ->
            pair.executions.size == 2 && pair.executions.all {
                it.schemaValid && it.injectionControlPassed && it.identityBlinded &&
                    !it.privilegedInformationLeaked && it.rationaleEvidenceGrounded
            }
        }
        val consistent = valid.filter { pair ->
            val observations = pair.executions.sortedBy { it.executionId }
            observations[0].canonicalPreference() == observations[1].canonicalPreference() &&
                observations[0].canonicalPreference() != A001EvaluationContractV2.Preference.TIE &&
                observations[0].canonicalPreference() != A001EvaluationContractV2.Preference.ABSTAIN
        }
        val preferencePairs = consistent.count { it.executions.first().canonicalPreference() == A001EvaluationContractV2.Preference.A }
        val deltas = consistent.map { pair -> pair.executions.map { it.canonicalDelta() }.average() }.sorted()
        val median = if (deltas.isEmpty()) 0.0 else if (deltas.size % 2 == 1) deltas[deltas.size / 2] else (deltas[deltas.size / 2 - 1] + deltas[deltas.size / 2]) / 2.0
        val panelInvalid = pairs.size != A001EvaluationContractV2.TOTAL_PAIRS ||
            valid.size < A001EvaluationContractV2.MIN_SCHEMA_VALID_PAIRS ||
            consistent.size < A001EvaluationContractV2.MIN_POSITION_CONSISTENT_PAIRS ||
            pairs.any { it.executions.any { e -> !e.injectionControlPassed || !e.identityBlinded || e.privilegedInformationLeaked } }
        if (panelInvalid) return Aggregate(Result.A001_AI_PANEL_INVALID, valid.size, consistent.size, preferencePairs, median, "panel validity rule failed")
        val pass = preferencePairs >= A001EvaluationContractV2.MIN_PREFERENCE_PAIRS && median >= A001EvaluationContractV2.MIN_MEDIAN_DELTA
        return if (comparison == A001EvaluationContractV2.Comparison.CALIBRATION) {
            Aggregate(if (pass) Result.CALIBRATION_PASS else Result.AI_EVALUATOR_CALIBRATION_FAIL, valid.size, consistent.size, preferencePairs, median, "calibration thresholds")
        } else {
            Aggregate(if (pass) Result.A001_AI_QUALIFICATION_PASS else Result.A001_AI_QUALIFICATION_FAIL, valid.size, consistent.size, preferencePairs, median, "FULL qualification thresholds")
        }
    }
}
