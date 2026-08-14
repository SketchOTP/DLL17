package com.animusmachinae.dll17.research.aliveness.agentic

/**
 * The only verdicts a gate-critical ruling may carry.
 *
 * `ABSTAIN` and both `BLOCKED_*` values are legitimate, well-formed rulings. None
 * of them is a pass: [isPass] is the single place that decides what counts, so
 * "the reviewer did not say no" can never be read as "the reviewer said yes".
 */
public enum class RulingVerdict {
    PASS,
    FAIL,
    BLOCKED_INSUFFICIENT_EVIDENCE,
    BLOCKED_SPEC_AMBIGUITY,
    ABSTAIN,
    ;

    public val isPass: Boolean get() = this == PASS
}

/** Why a reviewer's output was refused. Every one of these fails closed. */
public enum class FailureMode {
    MALFORMED_OUTPUT,
    MISSING_REQUIRED_FIELD,
    UNPARSEABLE_VERDICT,
    QUESTION_MISMATCH,
    PROVIDER_REFUSAL,
    TRANSPORT_FAILURE,
    TIMEOUT_AFTER_PERMITTED_RETRIES,
    EVIDENCE_OMISSION,
    UNSUPPORTED_CONCLUSION,
    INCONSISTENT_RULING_AND_PROSE,
    ;

    /**
     * Whether another attempt is permitted.
     *
     * A refusal is not retried, because retrying a refusal is asking the same
     * question until the answer changes. A well-formed ruling is never retried
     * at all — the harness has no path that re-rolls a verdict it dislikes.
     */
    public val retryable: Boolean
        get() = this in setOf(
            MALFORMED_OUTPUT,
            MISSING_REQUIRED_FIELD,
            UNPARSEABLE_VERDICT,
            TRANSPORT_FAILURE,
            TIMEOUT_AFTER_PERMITTED_RETRIES,
        )
}

/** A validated ruling. Only the parser may produce one. */
public class StructuredRuling internal constructor(
    public val verdict: RulingVerdict,
    public val questionId: String,
    public val citedEvidenceIds: List<String>,
    public val rationale: String,
)

/**
 * The outcome of asking one reviewer one question.
 *
 * There are exactly two shapes, and only one of them can carry a verdict. Nothing
 * in this package can turn a [FailedClosed] into a pass, because [FailedClosed]
 * has no verdict field to set.
 */
public sealed interface RulingOutcome {
    public val provenance: RulingProvenance

    public class Ruled(
        public val ruling: StructuredRuling,
        override val provenance: RulingProvenance,
    ) : RulingOutcome

    public class FailedClosed(
        public val mode: FailureMode,
        public val detail: String,
        override val provenance: RulingProvenance,
    ) : RulingOutcome
}

/** True only for a well-formed ruling whose verdict is PASS. */
public fun RulingOutcome.isPass(): Boolean =
    this is RulingOutcome.Ruled && ruling.verdict.isPass

/** The verdict name for reporting. A fail-closed outcome reports its failure, not a verdict. */
public fun RulingOutcome.label(): String = when (this) {
    is RulingOutcome.Ruled -> ruling.verdict.name
    is RulingOutcome.FailedClosed -> "FAILED_CLOSED:${mode.name}"
}

/**
 * The strict parser.
 *
 * Everything that is not unambiguously a well-formed, evidence-supported,
 * internally consistent ruling on the question that was actually asked becomes a
 * [RulingOutcome.FailedClosed]. There is no lenient path and no default.
 */
public object RulingParser {

    public const val PARSER_VERSION: Int = 1
    public const val SCHEMA_ID: String = "AgenticRulingSchemaV1"

    private val REQUIRED_KEYS = listOf("VERDICT", "QUESTION", "EVIDENCE", "RATIONALE")

    /** All verdict names, longest first, so `BLOCKED_SPEC_AMBIGUITY` is not seen as a prefix. */
    private val VERDICT_TOKENS = RulingVerdict.entries.map { it.name }.sortedByDescending { it.length }

    public fun parse(
        raw: String,
        question: ReviewQuestion,
        bundle: EvidenceBundle,
    ): ParseResult {
        val fields = mutableMapOf<String, String>()
        for (line in raw.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val key = REQUIRED_KEYS.firstOrNull { trimmed.startsWith("$it:") }
                ?: return ParseResult.Failed(
                    FailureMode.MALFORMED_OUTPUT,
                    "unexpected line outside the required schema: ${trimmed.take(60)}",
                )
            if (fields.containsKey(key)) {
                return ParseResult.Failed(
                    FailureMode.MALFORMED_OUTPUT,
                    "field $key appears more than once",
                )
            }
            fields[key] = trimmed.removePrefix("$key:").trim()
        }

        val missing = REQUIRED_KEYS.filterNot { fields.containsKey(it) }
        if (missing.isNotEmpty()) {
            return ParseResult.Failed(
                FailureMode.MISSING_REQUIRED_FIELD,
                "missing required field(s): ${missing.joinToString(",")}",
            )
        }

        val verdictText = fields.getValue("VERDICT")
        val verdict = RulingVerdict.entries.firstOrNull { it.name == verdictText }
            ?: return ParseResult.Failed(
                FailureMode.UNPARSEABLE_VERDICT,
                "not a verdict in $SCHEMA_ID: '$verdictText'",
            )

        if (fields.getValue("QUESTION") != question.questionId) {
            return ParseResult.Failed(
                FailureMode.QUESTION_MISMATCH,
                "ruled on '${fields.getValue("QUESTION")}' but was asked '${question.questionId}'",
            )
        }

        val rationale = fields.getValue("RATIONALE")
        if (rationale.isBlank()) {
            return ParseResult.Failed(
                FailureMode.MISSING_REQUIRED_FIELD,
                "RATIONALE is empty",
            )
        }

        val cited = fields.getValue("EVIDENCE")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.uppercase() != "NONE" }

        val unknown = cited.filterNot { it in bundle.ids }
        if (unknown.isNotEmpty()) {
            return ParseResult.Failed(
                FailureMode.UNSUPPORTED_CONCLUSION,
                "cites evidence not in the bundle: ${unknown.joinToString(",")}",
            )
        }

        // A decision needs support. Only the two decisive verdicts require it:
        // an abstention or a blocked ruling is precisely a statement that the
        // evidence did not carry the decision.
        if (verdict == RulingVerdict.PASS || verdict == RulingVerdict.FAIL) {
            if (cited.isEmpty()) {
                return ParseResult.Failed(
                    FailureMode.UNSUPPORTED_CONCLUSION,
                    "${verdict.name} cites no evidence",
                )
            }
            if (bundle.mustBeAddressedIds.isNotEmpty() &&
                cited.none { it in bundle.mustBeAddressedIds }
            ) {
                return ParseResult.Failed(
                    FailureMode.EVIDENCE_OMISSION,
                    "${verdict.name} addresses none of the decisive items " +
                        bundle.mustBeAddressedIds.sorted().joinToString(","),
                )
            }
        }

        // The prose must not announce a different verdict from the structured
        // field. A model that writes PASS in the field and explains why it fails
        // has not produced a ruling anyone should act on.
        val proseVerdicts = VERDICT_TOKENS.filter { token ->
            Regex("\\b$token\\b").containsMatchIn(rationale.uppercase())
        }
        val contradicting = proseVerdicts.filter { it != verdict.name }
        if (contradicting.isNotEmpty()) {
            return ParseResult.Failed(
                FailureMode.INCONSISTENT_RULING_AND_PROSE,
                "verdict is ${verdict.name} but the rationale asserts " +
                    contradicting.joinToString(","),
            )
        }

        return ParseResult.Parsed(
            StructuredRuling(verdict, question.questionId, cited, rationale),
        )
    }

    public sealed interface ParseResult {
        public class Parsed(public val ruling: StructuredRuling) : ParseResult
        public class Failed(
            public val mode: FailureMode,
            public val detail: String,
        ) : ParseResult
    }
}
