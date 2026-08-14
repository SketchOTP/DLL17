package com.animusmachinae.dll17.research.aliveness.agentic

/** The value recorded for any provenance field the provider does not actually expose. */
public const val UNKNOWN: String = "UNKNOWN"

/**
 * Identity of the thing that produced a ruling.
 *
 * [modelSnapshot] is nullable and renders as [UNKNOWN] when absent. Several
 * providers expose only a moving alias; recording that alias as if it were a
 * pinned snapshot would be a claim of reproducibility the provider does not
 * support.
 *
 * [isRealModel] distinguishes an actual language model from a deterministic test
 * double. The diversity policy refuses to count doubles, so the harness cannot be
 * qualified by pretending its own fixtures are reviewers.
 */
public class ModelDescriptor(
    public val provider: String,
    public val modelId: String,
    public val modelFamily: String,
    public val modelSnapshot: String? = null,
    public val isRealModel: Boolean = true,
) {
    public fun render(): String =
        "provider=$provider model=$modelId family=$modelFamily " +
            "snapshot=${modelSnapshot ?: UNKNOWN} realModel=$isRealModel"
}

/** Sampling configuration. Any field the caller does not control stays null and renders UNKNOWN. */
public class SamplingParameters(
    public val temperature: Double? = null,
    public val topP: Double? = null,
    public val maxOutputTokens: Int? = null,
    public val seed: Long? = null,
) {
    public fun render(): String =
        "temperature=${temperature ?: UNKNOWN} topP=${topP ?: UNKNOWN} " +
            "maxOutputTokens=${maxOutputTokens ?: UNKNOWN} seed=${seed ?: UNKNOWN}"
}

/** What a backend can return. A backend cannot return a ruling: only the parser makes those. */
public sealed interface BackendOutcome {
    public class Responded(public val rawText: String) : BackendOutcome
    public class Refused(public val reason: String) : BackendOutcome
    public class TimedOut(public val afterMillis: Long) : BackendOutcome
    public class TransportFailed(public val reason: String) : BackendOutcome
}

/**
 * A reviewer backend.
 *
 * The interface takes a fully-rendered [ReviewRequest] and returns text. It has
 * no access to the harness, to another reviewer, or to any prior outcome, and
 * there is no parameter through which one could be supplied.
 */
public interface ReviewerBackend {
    public val descriptor: ModelDescriptor
    public val sampling: SamplingParameters

    /**
     * Exactly which tools this backend may use. Hashed into provenance. The
     * reviewers in this harness are given none: a reviewer that could read the
     * repository could reach evidence outside its frozen bundle.
     */
    public val toolPermissionManifest: List<String>

    public fun invoke(request: ReviewRequest): BackendOutcome
}

/** The hash of a tool-permission manifest, order-independent. */
public fun ReviewerBackend.toolPermissionHash(): String =
    sha256(toolPermissionManifest.sorted().joinToString("\n").ifEmpty { "NO_TOOLS" })

/**
 * The durable record of one ruling attempt.
 *
 * [executionTimestamp] is non-decision metadata and is deliberately excluded from
 * [decisionRelevantDigest]: a governance record whose identity changed with the
 * clock could not be compared across runs, and nothing about a ruling should
 * depend on when it happened.
 */
public class RulingProvenance(
    public val roleId: String,
    public val roleContractVersion: Int,
    public val provider: String,
    public val modelId: String,
    public val modelFamily: String,
    public val modelSnapshot: String?,
    public val isRealModel: Boolean,
    public val promptHash: String,
    public val toolPermissionHash: String,
    public val evidenceBundleHash: String,
    public val requestHash: String,
    public val sampling: SamplingParameters,
    public val parserVersion: Int,
    public val schemaId: String,
    public val rawResponseHash: String?,
    public val retryCount: Int,
    public val retryReasons: List<FailureMode>,
    public val executionTimestamp: String?,
) {
    /** Everything that identifies the ruling attempt, excluding wall-clock time. */
    public fun decisionRelevantDigest(): String = sha256(
        listOf(
            roleId, roleContractVersion.toString(), provider, modelId, modelFamily,
            modelSnapshot ?: UNKNOWN, isRealModel.toString(), promptHash,
            toolPermissionHash, evidenceBundleHash, requestHash, sampling.render(),
            parserVersion.toString(), schemaId, rawResponseHash ?: UNKNOWN,
            retryCount.toString(), retryReasons.joinToString(","),
        ).joinToString(" "),
    )

    public fun render(indent: String = "    "): String = buildString {
        fun line(k: String, v: String) = append(indent).append(k.padEnd(22)).append(v).append('\n')
        line("roleId", roleId)
        line("roleContractVersion", roleContractVersion.toString())
        line("provider", provider)
        line("modelId", modelId)
        line("modelFamily", modelFamily)
        line("modelSnapshot", modelSnapshot ?: UNKNOWN)
        line("isRealModel", isRealModel.toString())
        line("promptHash", promptHash)
        line("toolPermissionHash", toolPermissionHash)
        line("evidenceBundleHash", evidenceBundleHash)
        line("requestHash", requestHash)
        line("sampling", sampling.render())
        line("parser", "$schemaId parserVersion=$parserVersion")
        line("rawResponseHash", rawResponseHash ?: UNKNOWN)
        line("retryCount", retryCount.toString())
        line("retryReasons", retryReasons.joinToString(",").ifEmpty { "none" })
        line("executionTimestamp", executionTimestamp ?: "$UNKNOWN (non-decision metadata)")
        line("decisionDigest", decisionRelevantDigest())
    }
}
