package com.animusmachinae.dll17.research.aliveness.agentic

/** The only evaluator-visible surface: ordinary outward observations. */
public class AgentObservation(
    public val neutralLabel: String,
    public val visibleTranscript: List<String>,
    public val interactionVocabulary: Set<String>,
) {
    init {
        require(neutralLabel == "A" || neutralLabel == "B")
        require(visibleTranscript.all { it.isNotBlank() })
        require(interactionVocabulary.all { it.isNotBlank() })
    }
}

/**
 * D016-L boundary. Its constructor has no cohort, source, state, mechanism,
 * debug, expected-outcome, or repository-context parameter. Candidate labels
 * are neutral and the returned request contains only the two outward surfaces.
 */
public object AgentObservationHarnessV1 {
    public const val HARNESS_ID: String = "AgentObservationHarnessV1"
    public const val PRIVILEGED_INFORMATION_LEAKAGE_ALLOWED: Boolean = false
    public const val IDENTITY_BLINDING_REQUIRED: Boolean = true

    public class BlindedObservationRequest(
        public val candidateA: AgentObservation,
        public val candidateB: AgentObservation,
        public val rubric: List<A001EvaluationContractV2.RubricDimension>,
    ) {
        init {
            require(candidateA.neutralLabel == "A")
            require(candidateB.neutralLabel == "B")
            require(rubric == A001EvaluationContractV2.RUBRIC)
        }

        public fun render(): String = buildString {
            append("[BLINDED OBSERVATION DATA]\n")
            append("A visible behavior:\n")
            candidateA.visibleTranscript.forEach { append("- ").append(it).append('\n') }
            append("A interactions: ").append(candidateA.interactionVocabulary.sorted()).append('\n')
            append("B visible behavior:\n")
            candidateB.visibleTranscript.forEach { append("- ").append(it).append('\n') }
            append("B interactions: ").append(candidateB.interactionVocabulary.sorted()).append('\n')
            append("[END BLINDED OBSERVATION DATA]\n")
            rubric.forEachIndexed { index, dimension ->
                append("${index + 1}. ${dimension.id}: ${dimension.question}\n")
            }
            append("PAIRWISE_PREFERENCE = A | B | TIE | ABSTAIN\n")
            append("EVIDENCE_GROUNDED_RATIONALE = visible behavior only")
        }
    }

    public fun request(first: AgentObservation, second: AgentObservation): BlindedObservationRequest {
        require(first.neutralLabel == "A" && second.neutralLabel == "B")
        return BlindedObservationRequest(first, second, A001EvaluationContractV2.RUBRIC)
    }
}
