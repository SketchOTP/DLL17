package com.animusmachinae.dll17.research.aliveness.agentic

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * D016-P-only execution wrapper. The D016-M-R1 runner is hash-bound by the
 * D016-O manifest and therefore remains untouched. This additive wrapper uses
 * the same frozen parser and contract types while selecting the D016-O input
 * namespace and writing immutable D016-P slots to a separate namespace.
 */
public class D016PExecutionRunner(
    private val root: Path,
    private val inputRoot: Path,
    private val outputRoot: Path,
    private val transport: A001V2EvaluatorTransport,
) {
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
        val manifestText = Files.readString(manifestPath(), StandardCharsets.UTF_8)
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

    public fun executeFormal(stage: A001EvaluationContractV2.Comparison): List<A001V2RawExecution> {
        val prefix = if (stage == A001EvaluationContractV2.Comparison.CALIBRATION) "CAL" else "FULL"
        val directory = outputRoot.resolve(stage.name.lowercase()).resolve("raw")
        val ledger = D016PSlotLedger(directory)
        val bundles = boundBundles(
            if (stage == A001EvaluationContractV2.Comparison.CALIBRATION) "calibration" else "qualification",
            "$prefix-P",
            manifestBundleHashes(),
        )
        val outputs = mutableListOf<A001V2RawExecution>()
        for (bundle in bundles) {
            for (order in listOf('A', 'B')) {
                val slot = "D016P-${bundle.first}-$order"
                ledger.claim(slot)
                val rendered = if (order == 'A') bundle.second else reverseBundle(bundle.second)
                val execution = transport.execute(slot, rendered)
                require(execution.slotId == slot) { "transport returned wrong slot" }
                ledger.persistRaw(execution)
                A001V2ResponseParser.parse(execution.rawResponse)
                outputs += execution
            }
        }
        require(outputs.size == A001EvaluationContractV2.TOTAL_FORMAL_EXECUTIONS)
        return outputs
    }

    private fun manifestBundleHashes(): Map<String, String> {
        val manifest = Json.parse(Files.readString(manifestPath(), StandardCharsets.UTF_8)) as? JsonValue.Obj
            ?: error("formal manifest is not an object")
        return listOf("calibration_bundles", "qualification_bundles").flatMap { key ->
            val array = manifest[key] as? JsonValue.Arr ?: error("manifest missing $key")
            array.items.map { item ->
                val entry = item as? JsonValue.Obj ?: error("manifest bundle entry is not an object")
                val path = (entry["path"] as? JsonValue.Str)?.value ?: error("manifest bundle path missing")
                val hash = (entry["sha256"] as? JsonValue.Str)?.value ?: error("manifest bundle hash missing")
                path to hash
            }
        }.toMap()
    }

    private fun manifestPath(): Path = Files.list(inputRoot).use { stream ->
        stream.filter { it.fileName.toString().endsWith("FORMAL_INPUT_MANIFEST.json") }
            .toList().singleOrNull()
            ?: error("formal input manifest missing or ambiguous in $inputRoot")
    }

    private fun boundBundles(directoryName: String, prefix: String, boundHashes: Map<String, String>): List<Pair<String, String>> {
        val directory = inputRoot.resolve(directoryName)
        val files = Files.list(directory).use { stream ->
            stream.filter { it.fileName.toString().endsWith(".txt") }.sorted().toList()
        }
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

    private class D016PSlotLedger(private val directory: Path) {
        init { Files.createDirectories(directory) }

        fun claim(slotId: String) {
            require(slotId.matches(Regex("D016P-(?:CAL|FULL)-P(?:0[1-9]|1[0-2])-[AB]"))) {
                "invalid D016-P slot: $slotId"
            }
            require(Files.notExists(directory.resolve("$slotId.claimed"))) {
                "D016-P slot already claimed: $slotId"
            }
            Files.writeString(directory.resolve("$slotId.claimed"), "CLAIMED\n", StandardCharsets.UTF_8)
        }

        fun persistRaw(execution: A001V2RawExecution) {
            val raw = directory.resolve("${execution.slotId}.raw.txt")
            require(Files.notExists(raw)) { "D016-P raw response already exists: ${execution.slotId}" }
            Files.writeString(raw, execution.rawResponse, StandardCharsets.UTF_8)
            Files.writeString(
                directory.resolve("${execution.slotId}.meta.txt"),
                "provider=${execution.provider}\nrequestedModel=${execution.requestedModel}\n" +
                    "executionId=${execution.executionId ?: UNKNOWN}\nrawResponseSha256=${sha256(execution.rawResponse)}\n",
                StandardCharsets.UTF_8,
            )
        }
    }
}
