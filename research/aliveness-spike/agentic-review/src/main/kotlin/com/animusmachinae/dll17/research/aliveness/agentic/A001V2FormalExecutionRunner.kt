package com.animusmachinae.dll17.research.aliveness.agentic

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** A response after raw provider bytes have been persisted. */
public class A001V2RawExecution(
    public val slotId: String,
    public val rawResponse: String,
    public val provider: String,
    public val requestedModel: String,
    public val executionId: String?,
)

/** A transport supplied by the future D016-M execution command. */
public fun interface A001V2EvaluatorTransport {
    public fun execute(slotId: String, renderedPrompt: String): A001V2RawExecution
}

/** A persistent slot ledger that cannot silently replace a completed slot. */
public class A001V2SlotLedger(private val directory: Path) {
    init { Files.createDirectories(directory) }

    public fun claim(slotId: String) {
        require(slotId.matches(Regex("(?:CAL|FULL)-P(?:0[1-9]|1[0-2])-[AB]"))) {
            "invalid formal slot: $slotId"
        }
        val marker = directory.resolve("$slotId.claimed")
        require(Files.notExists(marker)) { "formal slot already claimed: $slotId" }
        Files.writeString(marker, "CLAIMED\n", StandardCharsets.UTF_8)
    }

    public fun persistRaw(execution: A001V2RawExecution) {
        val raw = directory.resolve("${execution.slotId}.raw.txt")
        require(Files.notExists(raw)) { "formal raw response already exists: ${execution.slotId}" }
        Files.writeString(raw, execution.rawResponse, StandardCharsets.UTF_8)
        Files.writeString(
            directory.resolve("${execution.slotId}.meta.txt"),
            "provider=${execution.provider}\nrequestedModel=${execution.requestedModel}\n" +
                "executionId=${execution.executionId ?: UNKNOWN}\nrawResponseSha256=${sha256(execution.rawResponse)}\n",
            StandardCharsets.UTF_8,
        )
    }
}

/** Strict parser for the frozen A001 V2 evaluator response schema. */
public object A001V2ResponseParser {
    public const val SCHEMA_ID: String = "A001V2EvaluatorResponseV1"

    public class Parsed(
        public val preference: A001EvaluationContractV2.Preference,
        public val scoresA: Map<String, Int>,
        public val scoresB: Map<String, Int>,
        public val rationale: String,
    )

    public fun parse(raw: String): Parsed {
        val root = Json.parse(raw) as? JsonValue.Obj ?: fail("response must be an object")
        val allowed = setOf("scores", "PAIRWISE_PREFERENCE", "EVIDENCE_GROUNDED_RATIONALE")
        require(root.entries.map { it.first }.toSet() == allowed) { "response keys do not match frozen schema" }
        val scores = root["scores"] as? JsonValue.Obj ?: fail("scores missing")
        val a = parseScores(scores["A"])
        val b = parseScores(scores["B"])
        val preference = when ((root["PAIRWISE_PREFERENCE"] as? JsonValue.Str)?.value) {
            "A" -> A001EvaluationContractV2.Preference.A
            "B" -> A001EvaluationContractV2.Preference.B
            "TIE" -> A001EvaluationContractV2.Preference.TIE
            "ABSTAIN" -> A001EvaluationContractV2.Preference.ABSTAIN
            else -> fail("invalid pairwise preference")
        }
        val rationale = (root["EVIDENCE_GROUNDED_RATIONALE"] as? JsonValue.Str)?.value
            ?: fail("rationale missing")
        require(rationale.isNotBlank() && !rationale.contains('\n')) { "rationale must be one line" }
        return Parsed(preference, a, b, rationale)
    }

    private fun parseScores(value: JsonValue?): Map<String, Int> {
        val obj = value as? JsonValue.Obj ?: fail("candidate scores missing")
        val ids = A001EvaluationContractV2.RUBRIC.map { it.id }.toSet()
        require(obj.entries.map { it.first }.toSet() == ids) { "rubric keys do not match frozen schema" }
        return obj.entries.associate { (id, score) ->
            val literal = (score as? JsonValue.Num)?.literal ?: fail("score is not numeric")
            val number = literal.toIntOrNull() ?: fail("score is not an integer")
            require(number in 0..100) { "score outside 0..100" }
            id to number
        }
    }

    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
}

/**
 * D016-M-R1 formal runner substrate. Preflight is the only mode used here and
 * is strictly offline. The transport-injected execution method is intentionally
 * separate so a future formal command cannot accidentally call a model during
 * input preparation.
 */
public class A001V2FormalExecutionRunner(
    private val root: Path,
    private val transport: A001V2EvaluatorTransport? = null,
    ) {
    private val inputRoot = root.resolve("research/aliveness-spike/evidence/a001-v2/formal-input")
    private val forbiddenIdentity = listOf(
        "full", "scriptedpetbaselinev1", "degradedscriptedcontrolv1", "baseline",
        "degraded", "learning", "scripted", "agentic", "candidate type",
        "source path", "repository path",
    )

    public fun preflight(): String {
        val boundHashes = manifestBundleHashes()
        val calibration = boundBundles("calibration", "CAL-P", boundHashes)
        val qualification = boundBundles("qualification", "FULL-P", boundHashes)
        require(calibration.size == A001EvaluationContractV2.TOTAL_PAIRS)
        require(qualification.size == A001EvaluationContractV2.TOTAL_PAIRS)
        val instruction = inputRoot.resolve("EVALUATOR_INSTRUCTIONS_V1.txt")
        require(Files.exists(instruction)) { "evaluator instruction missing" }
        val manifest = inputRoot.resolve("A001_V2_FORMAL_INPUT_MANIFEST.json")
        require(Files.exists(manifest)) { "formal input manifest missing" }
        val manifestText = Files.readString(manifest, StandardCharsets.UTF_8)
        require(manifestText.contains("A001EvaluationContractV2")) { "manifest is not V2-bound" }
        require(manifestText.contains("formal_input_bundle_sha256")) { "manifest has no bundle hash" }
        val prompt = Files.readString(instruction, StandardCharsets.UTF_8)
        require(prompt.contains("PAIRWISE_PREFERENCE") && prompt.contains("OVERALL_APPARENT_ALIVENESS"))
        calibration.forEach { require(reverseBundle(reverseBundle(it.second)) == it.second) }
        qualification.forEach { require(reverseBundle(reverseBundle(it.second)) == it.second) }
        return buildString {
            append("FORMAL_RUNNER_READY=true\n")
            append("FORMAL_INPUT_MANIFEST=").append(sha256(manifestText)).append('\n')
            append("EVALUATOR_INSTRUCTION_SHA256=").append(sha256(prompt)).append('\n')
            append("CALIBRATION_OBSERVATION_CASES=").append(calibration.size).append('\n')
            append("QUALIFICATION_OBSERVATION_CASES=").append(qualification.size).append('\n')
            append("OBSERVATION_IDENTITY_LEAKAGE=false\n")
            append("PRIVILEGED_INFORMATION_LEAKAGE=false\n")
            append("NETWORK_MODEL_CALLS=0\n")
        }
    }

    /** Execute exactly the frozen slots through an injected transport. */
    public fun executeFormal(stage: A001EvaluationContractV2.Comparison): List<A001V2RawExecution> {
        val activeTransport = transport ?: error("formal execution requires an explicit transport")
        val prefix = if (stage == A001EvaluationContractV2.Comparison.CALIBRATION) "CAL" else "FULL"
        val directory = root.resolve("research/aliveness-spike/evidence/a001-v2/${prefix.lowercase()}/raw")
        val ledger = A001V2SlotLedger(directory)
        val bundles = boundBundles(
            prefix.lowercase().let { if (it == "cal") "calibration" else "qualification" },
            "$prefix-P",
            manifestBundleHashes(),
        )
        val outputs = mutableListOf<A001V2RawExecution>()
        for (bundle in bundles) {
            for (order in listOf('A', 'B')) {
                val slot = "${bundle.first}-$order"
                ledger.claim(slot)
                val rendered = if (order == 'A') bundle.second else reverseBundle(bundle.second)
                val execution = activeTransport.execute(slot, rendered)
                require(execution.slotId == slot) { "transport returned wrong slot" }
                ledger.persistRaw(execution)
                A001V2ResponseParser.parse(execution.rawResponse)
                outputs += execution
            }
        }
        return outputs
    }

    private fun manifestBundleHashes(): Map<String, String> {
        val manifest = inputRoot.resolve("A001_V2_FORMAL_INPUT_MANIFEST.json")
        val root = Json.parse(Files.readString(manifest, StandardCharsets.UTF_8)) as? JsonValue.Obj
            ?: error("formal manifest is not an object")
        val arrays = listOf("calibration_bundles", "qualification_bundles")
        return arrays.flatMap { key ->
            val array = root[key] as? JsonValue.Arr ?: error("manifest missing $key")
            array.items.map { item ->
                val entry = item as? JsonValue.Obj ?: error("manifest bundle entry is not an object")
                val path = (entry["path"] as? JsonValue.Str)?.value ?: error("manifest bundle path missing")
                val hash = (entry["sha256"] as? JsonValue.Str)?.value ?: error("manifest bundle hash missing")
                path to hash
            }
        }.toMap()
    }

    private fun boundBundles(directoryName: String, prefix: String, boundHashes: Map<String, String>): List<Pair<String, String>> {
        val directory = inputRoot.resolve(directoryName)
        val files = Files.list(directory).use { stream -> stream.filter { it.fileName.toString().endsWith(".txt") }.sorted().toList() }
        return files.map { file ->
            val text = Files.readString(file, StandardCharsets.UTF_8)
            require(file.fileName.toString().startsWith(prefix)) { "unexpected bundle name" }
            val relative = root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/')
            require(boundHashes[relative] == sha256(text)) { "bundle is not manifest-bound: $relative" }
            require(forbiddenIdentity.none { text.lowercase().contains(it) }) {
                "identity or privileged information leaked into ${file.fileName}"
            }
            require(text.startsWith("A001_OBSERVATION_BUNDLE_V1\n")) { "invalid observation bundle" }
            require(text.contains("CREATURE_A\n") && text.contains("CREATURE_B\n")) { "neutral labels missing" }
            require(text.endsWith("END_BUNDLE\n")) { "bundle is not complete" }
            file.fileName.toString().removeSuffix(".txt") to text
        }
    }

    private fun reverseBundle(text: String): String {
        val markerA = "CREATURE_A\n"
        val markerB = "CREATURE_B\n"
        val aStart = text.indexOf(markerA)
        val bStart = text.indexOf(markerB)
        require(aStart >= 0 && bStart > aStart)
        val header = text.substring(0, aStart)
        val a = text.substring(aStart + markerA.length, bStart)
        val b = text.substring(bStart + markerB.length, text.length - "END_BUNDLE\n".length)
        return header + markerA + b + markerB + a + "END_BUNDLE\n"
    }

    public companion object {
        @JvmStatic
        public fun main(args: Array<String>) {
            val root = args.firstOrNull { it.startsWith("--root=") }
                ?.removePrefix("--root=")?.let(Paths::get) ?: Paths.get(".")
            require(args.any { it == "--preflight" }) {
                "R1 permits only --preflight; formal calls require a future D016-M execution directive"
            }
            print(A001V2FormalExecutionRunner(root.toAbsolutePath().normalize()).preflight())
        }
    }
}
