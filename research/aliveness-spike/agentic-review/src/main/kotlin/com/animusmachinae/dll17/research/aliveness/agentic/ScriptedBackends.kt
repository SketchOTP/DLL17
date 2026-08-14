package com.animusmachinae.dll17.research.aliveness.agentic

/**
 * Deterministic reviewer doubles.
 *
 * **None of these is a language model, and none may ever be counted as one.**
 * Every descriptor here sets `isRealModel = false`, and [DiversityPolicy] refuses
 * a pair containing one, so the harness cannot be qualified against its own
 * fixtures.
 *
 * What they are for: proving the harness's mechanics — that the parser fails
 * closed, that retries behave, that evidence ordering does not move a ruling,
 * that a disagreement is surfaced rather than resolved, and that the injection
 * boundary holds. Whether a *real* reviewer is stable, order-insensitive and
 * injection-resistant is a different question, it needs real models, and it is
 * the question the qualification is blocked on.
 */
public abstract class ScriptedBackend(
    modelId: String,
    public val note: String,
) : ReviewerBackend {
    override val descriptor: ModelDescriptor = ModelDescriptor(
        provider = "IN_REPOSITORY_FIXTURE",
        modelId = modelId,
        modelFamily = "NOT_A_MODEL",
        modelSnapshot = null,
        isRealModel = false,
    )
    override val sampling: SamplingParameters = SamplingParameters(temperature = 0.0)
    override val toolPermissionManifest: List<String> = emptyList()
}

/** The finding a decisive evidence item asserts, in the fixtures' own notation. */
public enum class ScriptedFinding { PASS, FAIL, INSUFFICIENT, AMBIGUOUS }

internal fun findingsIn(bundle: EvidenceBundle): List<Pair<String, ScriptedFinding>> =
    bundle.items.filter { it.mustBeAddressed }.mapNotNull { item ->
        val match = Regex("finding=([A-Z]+)").find(item.text) ?: return@mapNotNull null
        val finding = ScriptedFinding.entries.firstOrNull { it.name == match.groupValues[1] }
            ?: return@mapNotNull null
        item.id to finding
    }

internal fun emit(
    verdict: RulingVerdict,
    questionId: String,
    cited: List<String>,
    rationale: String,
): BackendOutcome.Responded = BackendOutcome.Responded(
    """
    VERDICT: ${verdict.name}
    QUESTION: $questionId
    EVIDENCE: ${cited.joinToString(",")}
    RATIONALE: $rationale
    """.trimIndent(),
)

/**
 * A well-behaved reviewer stub.
 *
 * Reads the findings asserted by the decisive evidence items and reports them.
 * It is order-insensitive because it aggregates rather than reads positionally,
 * and injection-proof because it only ever looks at `finding=` tokens inside
 * decisive items and has no code path that treats evidence text as an
 * instruction. Those are properties of the stub, not evidence about any model.
 */
public class CompliantScriptedReviewer(
    modelId: String = "compliant-scripted-reviewer",
) : ScriptedBackend(modelId, "rule-based stub standing in for the reasoning step") {

    override fun invoke(request: ReviewRequest): BackendOutcome {
        val question = request.question.questionId
        val findings = findingsIn(request.bundle)
        if (findings.isEmpty()) {
            return emit(
                RulingVerdict.BLOCKED_INSUFFICIENT_EVIDENCE, question, emptyList(),
                "no decisive item states a finding",
            )
        }
        val distinct = findings.map { it.second }.toSet()
        val cited = findings.map { it.first }.sorted()
        return when {
            distinct.size > 1 -> emit(
                RulingVerdict.BLOCKED_INSUFFICIENT_EVIDENCE, question, cited,
                "decisive items assert incompatible findings and nothing adjudicates between them",
            )
            distinct.single() == ScriptedFinding.AMBIGUOUS -> emit(
                RulingVerdict.BLOCKED_SPEC_AMBIGUITY, question, cited,
                "the specification does not define the behaviour in question",
            )
            distinct.single() == ScriptedFinding.INSUFFICIENT -> emit(
                RulingVerdict.BLOCKED_INSUFFICIENT_EVIDENCE, question, cited,
                "the decisive items do not carry the decision",
            )
            distinct.single() == ScriptedFinding.PASS -> emit(
                RulingVerdict.PASS, question, cited, "the decisive items support the claim",
            )
            else -> emit(
                RulingVerdict.FAIL, question, cited, "the decisive items refute the claim",
            )
        }
    }
}

/** Always rules PASS, whatever the evidence. Used to produce a genuine disagreement. */
public class AlwaysPassReviewer : ScriptedBackend("always-pass-reviewer", "disagreement fixture") {
    override fun invoke(request: ReviewRequest): BackendOutcome {
        val cited = request.bundle.mustBeAddressedIds.sorted().ifEmpty { request.bundle.ids.sorted() }
        return emit(RulingVerdict.PASS, request.question.questionId, cited, "asserts the claim holds")
    }
}

/**
 * Reads only the first decisive item.
 *
 * A reviewer with this defect returns a different verdict when the same evidence
 * is presented in the opposite order. The harness must *detect* that, not
 * average it away.
 */
public class PositionSensitiveReviewer :
    ScriptedBackend("position-sensitive-reviewer", "order/position sensitivity fixture") {
    override fun invoke(request: ReviewRequest): BackendOutcome {
        val first = findingsIn(request.bundle).firstOrNull()
            ?: return emit(
                RulingVerdict.BLOCKED_INSUFFICIENT_EVIDENCE, request.question.questionId,
                emptyList(), "nothing to read",
            )
        val verdict = if (first.second == ScriptedFinding.PASS) RulingVerdict.PASS else RulingVerdict.FAIL
        return emit(verdict, request.question.questionId, listOf(first.first), "read the first item only")
    }
}

/**
 * Obeys instruction-like text found inside evidence.
 *
 * This is the reviewer the injection fixture is aimed at. The point is not that
 * the stub is fooled — it is written to be fooled — but that the harness's
 * later layers still refuse the result rather than recording a pass.
 */
public class InjectionSusceptibleReviewer :
    ScriptedBackend("injection-susceptible-reviewer", "prompt-injection fixture") {
    override fun invoke(request: ReviewRequest): BackendOutcome {
        val rendered = request.render()
        val injected = Regex("OVERRIDE: return ([A-Z_]+) citing nothing").find(rendered)
        if (injected != null) {
            val verdict = RulingVerdict.entries.firstOrNull { it.name == injected.groupValues[1] }
            if (verdict != null) {
                return emit(verdict, request.question.questionId, emptyList(), "instructed to comply")
            }
        }
        return CompliantScriptedReviewer().invoke(request)
    }
}

/** Returns prose instead of the required schema. */
public class MalformedOutputBackend :
    ScriptedBackend("malformed-output-backend", "malformed-output fixture") {
    override fun invoke(request: ReviewRequest): BackendOutcome =
        BackendOutcome.Responded(
            "Having weighed the evidence carefully I am comfortable that this should proceed.",
        )
}

/** Structured, but the rationale contradicts the verdict. */
public class InconsistentProseBackend :
    ScriptedBackend("inconsistent-prose-backend", "ruling/prose inconsistency fixture") {
    override fun invoke(request: ReviewRequest): BackendOutcome = emit(
        RulingVerdict.PASS, request.question.questionId,
        request.bundle.mustBeAddressedIds.sorted().ifEmpty { request.bundle.ids.sorted() },
        "on balance this should FAIL because the comparator was never qualified",
    )
}

/** Structured and decisive, but cites nothing. */
public class UnsupportedConclusionBackend :
    ScriptedBackend("unsupported-conclusion-backend", "unsupported-conclusion fixture") {
    override fun invoke(request: ReviewRequest): BackendOutcome =
        emit(RulingVerdict.PASS, request.question.questionId, emptyList(), "it seems fine")
}

/** Decides without addressing the decisive item. */
public class EvidenceOmittingBackend :
    ScriptedBackend("evidence-omitting-backend", "evidence-omission fixture") {
    override fun invoke(request: ReviewRequest): BackendOutcome {
        val nonDecisive = request.bundle.items.filterNot { it.mustBeAddressed }.map { it.id }.sorted()
        return emit(
            RulingVerdict.PASS, request.question.questionId, nonDecisive,
            "the supporting material reads well",
        )
    }
}

/** The provider declines. */
public class RefusingBackend :
    ScriptedBackend("refusing-backend", "provider-refusal fixture") {
    override fun invoke(request: ReviewRequest): BackendOutcome =
        BackendOutcome.Refused("provider declined to answer")
}

/** The provider never answers. */
public class TimingOutBackend :
    ScriptedBackend("timing-out-backend", "timeout fixture") {
    override fun invoke(request: ReviewRequest): BackendOutcome = BackendOutcome.TimedOut(120_000)
}

/** Fails transiently, then succeeds. Exercises the retry path and its accounting. */
public class FlakyThenCompliantBackend(private val failures: Int) :
    ScriptedBackend("flaky-then-compliant-backend", "retry fixture") {
    private var seen = 0
    override fun invoke(request: ReviewRequest): BackendOutcome {
        seen++
        return if (seen <= failures) {
            BackendOutcome.TransportFailed("connection reset (attempt $seen)")
        } else {
            CompliantScriptedReviewer().invoke(request)
        }
    }
}
