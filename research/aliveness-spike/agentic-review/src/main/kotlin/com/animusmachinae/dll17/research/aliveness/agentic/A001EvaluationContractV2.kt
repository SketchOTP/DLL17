package com.animusmachinae.dll17.research.aliveness.agentic

/** D016-L: the forward A001 population and acceptance contract. */
public object A001EvaluationContractV2 {
    public const val CONTRACT_ID: String = "A001EvaluationContractV2"
    public const val CONTRACT_VERSION: Int = 2
    public const val EVALUATION_POPULATION: String = "AI_AGENTS_ONLY"
    public const val EXTERNAL_HUMAN_PARTICIPANTS: String = "PROHIBITED"
    public const val OWNER_PIXEL_REVIEWERS: Int = 1
    public const val OWNER_PIXEL_REVIEWER: String = "OWNER_ONLY"
    public const val OWNER_PIXEL_ACCEPTANCE_REQUIRED: Boolean = true
    public const val AI_RESULTS_CANNOT_OVERRIDE_OWNER_FAIL: Boolean = true
    public const val GENERAL_HUMAN_POPULATION_INFERENCE: Boolean = false
    public const val R003_R009_BLOCKED_UNTIL_A001_V2_PASS: Boolean = true
    public const val TOTAL_PAIRS: Int = 12
    public const val TOTAL_FORMAL_EXECUTIONS: Int = 24
    public const val MIN_SCHEMA_VALID_PAIRS: Int = 11
    public const val MIN_POSITION_CONSISTENT_PAIRS: Int = 10
    public const val MIN_PREFERENCE_PAIRS: Int = 9
    public const val MIN_MEDIAN_DELTA: Double = 10.0

    public val RUBRIC: List<RubricDimension> = listOf(
        RubricDimension("APPARENT_AUTONOMY", "Does it appear to initiate behavior from ongoing state?"),
        RubricDimension("BEHAVIORAL_COHERENCE", "Do actions appear connected to prior events and condition?"),
        RubricDimension("ADAPTIVE_RESPONSIVENESS", "Does it change meaningfully in response to experience?"),
        RubricDimension("INDIVIDUALITY_AND_HISTORY", "Does accumulated experience make this instance distinct?"),
        RubricDimension("SPONTANEOUS_SENSIBLE_ACTIVITY", "Does it act unexpectedly without becoming random or cyclic?"),
        RubricDimension("OVERALL_APPARENT_ALIVENESS", "How much does it appear to have an ongoing life of its own?"),
    )

    public class RubricDimension(public val id: String, public val question: String)

    public enum class Comparison { CALIBRATION, FULL }
    public enum class Candidate { FIRST, SECOND }
    public enum class Preference { A, B, TIE, ABSTAIN }

    public class PanelPair(public val pairId: Int, public val executionAOrder: List<String>, public val executionBOrder: List<String>)

    /** Frozen counterbalanced plan; each pair is two isolated executions. */
    public val PANEL: List<PanelPair> = (1..TOTAL_PAIRS).map { id ->
        if (id % 2 == 1) PanelPair(id, listOf("A", "B"), listOf("B", "A"))
        else PanelPair(id, listOf("B", "A"), listOf("A", "B"))
    }

    public class PairObservation(
        public val pairId: Int,
        public val executionId: String,
        public val order: List<String>,
        public val preference: Preference,
        public val overallA: Int,
        public val overallB: Int,
        public val schemaValid: Boolean = true,
        public val injectionControlPassed: Boolean = true,
        public val identityBlinded: Boolean = true,
        public val privilegedInformationLeaked: Boolean = false,
        public val rationaleEvidenceGrounded: Boolean = true,
        /** Which neutral label represents the canonical strong/FULL candidate. */
        public val canonicalCandidate: Candidate,
    ) {
        init {
            require(pairId in 1..TOTAL_PAIRS)
            require(order.size == 2 && order.toSet() == setOf("A", "B"))
            require(overallA in 0..100 && overallB in 0..100)
        }

        public fun canonicalPreference(): Preference = when (preference) {
            Preference.TIE -> Preference.TIE
            Preference.ABSTAIN -> Preference.ABSTAIN
            Preference.A -> if (canonicalCandidate == Candidate.FIRST) Preference.A else Preference.B
            Preference.B -> if (canonicalCandidate == Candidate.FIRST) Preference.B else Preference.A
        }

        public fun canonicalDelta(): Int = if (canonicalCandidate == Candidate.FIRST) {
            overallA - overallB
        } else {
            overallB - overallA
        }
    }
}
