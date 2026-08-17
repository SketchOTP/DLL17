package com.animusmachinae.dll17.research.aliveness.agentic

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Instant

/**
 * D016-Q execution boundary. This is additive and writes only to d016-q.
 * D016-P evidence and the D016-O-bound execution substrate remain immutable.
 */
public object D016QFormalExecution {
    private const val PROVIDER = "OpenAI Responses API"
    private const val MODEL = "gpt-5"
    private const val ENDPOINT = "https://api.openai.com/v1/responses"
    private const val BUNDLE_SHA = "f6f543b3d1cf499b1015c4d66b005915d364a7d0d0b784605c249f13d0592c69"
    private const val INSTRUCTION_SHA = "92147a2ade86db8d602b991b8bbd4099e16f008d1fab9b0f84e15652c6a568a4"
    private const val CANDIDATE_ID = "A001_FULL_D016N_V1"
    private const val CANDIDATE_SHA = "684579130bef5c820f3db9534ffb744654ebf3b4"

    @JvmStatic
    public fun main(args: Array<String>) {
        val mode = argument(args, "mode") ?: error("--mode is required")
        val root = argument(args, "root")?.let(Paths::get)?.toAbsolutePath()?.normalize()
            ?: Paths.get(".").toAbsolutePath().normalize()
        val inputRoot = argument(args, "input-root")?.let(Paths::get)?.toAbsolutePath()?.normalize()
            ?: error("--input-root is required")
        val outputRoot = argument(args, "output-root")?.let(Paths::get)?.toAbsolutePath()?.normalize()
            ?: root.resolve("research/aliveness-spike/evidence/a001-v2/d016-q")
        require(root.resolve(".git").toFile().exists()) { "root is not a repository: $root" }
        when (mode) {
            "preflight" -> print(preflight(root, inputRoot))
            "sentinel" -> {
                val key = System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
                    ?: error("BLOCKED_EXISTING_OPENAI_CREDENTIAL_UNAVAILABLE")
                println(capacitySentinel(outputRoot, key))
            }
            "run" -> {
                require("--run" in args) { "D016-Q formal execution requires --run" }
                val stage = argument(args, "stage")?.let {
                    runCatching { A001EvaluationContractV2.Comparison.valueOf(it) }.getOrNull()
                } ?: error("--stage must be CALIBRATION or FULL")
                val key = System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
                    ?: error("BLOCKED_EXISTING_OPENAI_CREDENTIAL_UNAVAILABLE")
                require(preflight(root, inputRoot).contains("D016Q_FORMAL_RUNNER_READY=true"))
                val sentinel = outputRoot.resolve("capacity-sentinel/CAPACITY_SENTINEL.txt")
                require(Files.readString(sentinel, StandardCharsets.UTF_8).contains("CAPACITY_SENTINEL_RESULT=AVAILABLE")) {
                    "D016-Q capacity sentinel is not available"
                }
                val count = executeStage(inputRoot, outputRoot, stage, key)
                println("D016Q_STAGE=${stage.name}")
                println("D016Q_EXECUTIONS_ATTEMPTED=$count")
                println("D016Q_SELECTIVE_RERUNS=0")
                println("D016Q_REPLACEMENT_ANSWERS=0")
            }
            else -> error("unknown D016-Q mode: $mode")
        }
    }

    private fun preflight(root: Path, inputRoot: Path): String {
        val manifestPath = Files.list(inputRoot).use { stream ->
            stream.filter { it.fileName.toString().endsWith("FORMAL_INPUT_MANIFEST.json") }.toList().singleOrNull()
                ?: error("formal input manifest missing or ambiguous")
        }
        val manifest = Json.parse(Files.readString(manifestPath, StandardCharsets.UTF_8)) as? JsonValue.Obj
            ?: error("formal input manifest is not an object")
        require((manifest["candidate_identity"] as? JsonValue.Str)?.value == CANDIDATE_ID)
        require((manifest["candidate_git_sha"] as? JsonValue.Str)?.value == CANDIDATE_SHA)
        require((manifest["formal_input_bundle_sha256"] as? JsonValue.Str)?.value == BUNDLE_SHA)
        require((manifest["evaluator_instruction_sha256"] as? JsonValue.Str)?.value == INSTRUCTION_SHA)
        require((manifest["protocol_id"] as? JsonValue.Str)?.value == "A001ObservationProtocolV1")
        require((manifest["provider"] as? JsonValue.Str)?.value == PROVIDER)
        require((manifest["model"] as? JsonValue.Str)?.value == MODEL)
        require((manifest["panel_pairs"] as? JsonValue.Num)?.literal == "12")
        require((manifest["formal_executions_per_stage"] as? JsonValue.Num)?.literal == "24")
        require((manifest["min_schema_valid_pairs"] as? JsonValue.Num)?.literal == "11")
        require((manifest["min_position_consistent_pairs"] as? JsonValue.Num)?.literal == "10")
        require((manifest["min_preference_pairs"] as? JsonValue.Num)?.literal == "9")
        require((manifest["min_median_overall_aliveness_delta"] as? JsonValue.Num)?.literal == "10.0")
        val instruction = inputRoot.resolve("EVALUATOR_INSTRUCTIONS_V1.txt")
        require(sha256Bytes(Files.readAllBytes(instruction)) == INSTRUCTION_SHA)
        val bundleHashes = bundleHashes(manifest)
        val calibration = checkBundles(inputRoot.resolve("calibration"), "CAL-P", bundleHashes)
        val full = checkBundles(inputRoot.resolve("qualification"), "FULL-P", bundleHashes)
        return buildString {
            append("D016Q_FORMAL_RUNNER_READY=true\n")
            append("CANDIDATE_ID=").append(CANDIDATE_ID).append('\n')
            append("CANDIDATE_SOURCE_SHA=").append(CANDIDATE_SHA).append('\n')
            append("FORMAL_INPUT_BUNDLE_SHA256=").append(BUNDLE_SHA).append('\n')
            append("EVALUATOR_INSTRUCTION_SHA256=").append(INSTRUCTION_SHA).append('\n')
            append("CALIBRATION_CASES=").append(calibration).append('\n')
            append("FULL_CASES=").append(full).append('\n')
            append("OBSERVATION_PROTOCOL=A001ObservationProtocolV1\n")
            append("PROVIDER=").append(PROVIDER).append('\n')
            append("MODEL=").append(MODEL).append('\n')
            append("NETWORK_MODEL_CALLS=0\n")
        }
    }

    private fun bundleHashes(manifest: JsonValue.Obj): Map<String, String> =
        listOf("calibration_bundles", "qualification_bundles").flatMap { key ->
            val array = manifest[key] as? JsonValue.Arr ?: error("manifest missing $key")
            array.items.map { item ->
                val entry = item as? JsonValue.Obj ?: error("manifest bundle entry is not an object")
                val path = (entry["path"] as? JsonValue.Str)?.value ?: error("manifest bundle path missing")
                val hash = (entry["sha256"] as? JsonValue.Str)?.value ?: error("manifest bundle hash missing")
                Paths.get(path).fileName.toString() to hash
            }
        }.toMap()

    private fun checkBundles(directory: Path, prefix: String, hashes: Map<String, String>): Int {
        val files = Files.list(directory).use { stream -> stream.filter { it.fileName.toString().endsWith(".txt") }.sorted().toList() }
        require(files.size == A001EvaluationContractV2.TOTAL_PAIRS)
        files.forEach { file ->
            require(file.fileName.toString().startsWith(prefix))
            require(hashes[file.fileName.toString()] == sha256Bytes(Files.readAllBytes(file)))
            val text = Files.readString(file, StandardCharsets.UTF_8)
            require(text.startsWith("A001_OBSERVATION_BUNDLE_V1\n"))
            require(text.contains("CREATURE_A\n") && text.contains("CREATURE_B\n"))
            require(text.endsWith("END_BUNDLE\n"))
        }
        return files.size
    }

    private fun capacitySentinel(outputRoot: Path, apiKey: String): String {
        val directory = outputRoot.resolve("capacity-sentinel")
        Files.createDirectories(directory)
        val marker = directory.resolve("CAPACITY_SENTINEL.txt")
        require(Files.notExists(marker)) { "capacity sentinel already attempted" }
        val body = jObj(
            "model" to jStr(MODEL),
            "input" to jArr(jObj("role" to jStr("user"), "content" to jArr(jObj("type" to jStr("input_text"), "text" to jStr("capacity sentinel"))))),
            "tool_choice" to jStr("none"),
            "store" to JsonValue.Bool(false),
        ).render()
        assertToolFree(body, allowed = setOf("tool_choice"))
        val started = Instant.now().toString()
        var status = 599
        var responseId = "UNKNOWN"
        var result = "UNAVAILABLE"
        var reason = "transport failure"
        try {
            val response = JdkHttpTransport().send(
                HttpRequestSpec(
                    method = "POST",
                    url = ENDPOINT,
                    headers = listOf("Content-Type" to "application/json"),
                    secretHeaders = listOf("Authorization" to "Bearer $apiKey"),
                    body = body,
                ),
            )
            status = response.status
            responseId = runCatching { ((Json.parse(response.body) as? JsonValue.Obj)?.get("id") as? JsonValue.Str)?.value }.getOrNull() ?: "UNKNOWN"
            result = if (response.status in 200..299) "AVAILABLE" else "UNAVAILABLE"
            reason = if (result == "AVAILABLE") "http_success" else classifyCapacityFailure(response.body)
        } catch (e: TransportException) {
            reason = "transport_failure"
        }
        val text = buildString {
            append("CAPACITY_SENTINEL_ATTEMPTED=true\n")
            append("CAPACITY_SENTINEL_CALLS=1\n")
            append("CAPACITY_SENTINEL_RESULT=").append(result).append('\n')
            append("PROVIDER=").append(PROVIDER).append('\n')
            append("MODEL=").append(MODEL).append('\n')
            append("HTTP_STATUS=").append(status).append('\n')
            append("RESPONSE_ID=").append(responseId).append('\n')
            append("REASON=").append(reason).append('\n')
            append("STARTED_UTC=").append(started).append('\n')
            append("SCIENTIFIC_EVIDENCE=false\n")
        }
        Files.writeString(marker, text, StandardCharsets.UTF_8)
        return text
    }

    private fun executeStage(inputRoot: Path, outputRoot: Path, stage: A001EvaluationContractV2.Comparison, apiKey: String): Int {
        val stageName = stage.name.lowercase()
        val inputDirectory = inputRoot.resolve(if (stage == A001EvaluationContractV2.Comparison.CALIBRATION) "calibration" else "qualification")
        val prefix = if (stage == A001EvaluationContractV2.Comparison.CALIBRATION) "CAL-P" else "FULL-P"
        val stageRoot = outputRoot.resolve(stageName)
        val raw = stageRoot.resolve("raw")
        val rawProvider = stageRoot.resolve("raw-provider")
        val ledger = outputRoot.resolve("ledger")
        Files.createDirectories(raw)
        Files.createDirectories(rawProvider)
        Files.createDirectories(ledger)
        val bundles = Files.list(inputDirectory).use { stream -> stream.filter { it.fileName.toString().startsWith(prefix) && it.fileName.toString().endsWith(".txt") }.sorted().toList() }
        require(bundles.size == A001EvaluationContractV2.TOTAL_PAIRS)
        var attempted = 0
        for ((index, bundle) in bundles.withIndex()) {
            val pair = index + 1
            val text = Files.readString(bundle, StandardCharsets.UTF_8)
            for (order in listOf('A', 'B')) {
                val slot = "D016Q-${if (stage == A001EvaluationContractV2.Comparison.CALIBRATION) "CAL" else "FULL"}-P${"%02d".format(pair)}-$order"
                val marker = ledger.resolve("$slot.claimed")
                require(Files.notExists(marker)) { "D016-Q slot already claimed: $slot" }
                Files.writeString(marker, "CLAIMED\n", StandardCharsets.UTF_8)
                val rendered = if (order == 'A') text else reverseBundle(text)
                val result = sendFormal(slot, inputRoot, rendered, rawProvider, apiKey)
                Files.writeString(raw.resolve("$slot.raw.txt"), result.extracted, StandardCharsets.UTF_8)
                Files.writeString(raw.resolve("$slot.meta.txt"), "provider=$PROVIDER\nrequestedModel=$MODEL\nhttpStatus=${result.status}\nresponseId=${result.responseId}\nrawResponseSha256=${sha256Bytes(result.extracted.toByteArray(StandardCharsets.UTF_8))}\n", StandardCharsets.UTF_8)
                attempted++
                if (result.hardCapacity) {
                    Files.writeString(outputRoot.resolve("D016Q_STOP_AFTER_CAPACITY.txt"), "STOP_AFTER_CAPACITY=true\nSLOT=$slot\n", StandardCharsets.UTF_8)
                    return attempted
                }
            }
        }
        return attempted
    }

    private data class FormalResult(val extracted: String, val status: Int, val responseId: String, val hardCapacity: Boolean)

    private fun sendFormal(slot: String, inputRoot: Path, rendered: String, rawProvider: Path, apiKey: String): FormalResult {
        val instructions = Files.readString(inputRoot.resolve("EVALUATOR_INSTRUCTIONS_V1.txt"), StandardCharsets.UTF_8)
        val prompt = instructions.trimEnd() + "\n\n" + rendered
        val body = jObj(
            "model" to jStr(MODEL),
            "input" to jArr(jObj("role" to jStr("user"), "content" to jArr(jObj("type" to jStr("input_text"), "text" to jStr(prompt))))),
            "tool_choice" to jStr("none"),
            "store" to JsonValue.Bool(false),
        ).render()
        assertToolFree(body, allowed = setOf("tool_choice"))
        val response = try {
            JdkHttpTransport().send(HttpRequestSpec("POST", ENDPOINT, listOf("Content-Type" to "application/json"), listOf("Authorization" to "Bearer $apiKey"), body))
        } catch (e: TransportException) {
            HttpResponseSpec(599, "TRANSPORT_ERROR")
        }
        Files.writeString(rawProvider.resolve("$slot.raw.json"), response.body, StandardCharsets.UTF_8)
        val responseId = runCatching { ((Json.parse(response.body) as? JsonValue.Obj)?.get("id") as? JsonValue.Str)?.value }.getOrNull() ?: "UNKNOWN"
        Files.writeString(rawProvider.resolve("$slot.meta.json"), "{\n  \"slotId\": \"$slot\",\n  \"provider\": \"$PROVIDER\",\n  \"requestedModel\": \"$MODEL\",\n  \"httpStatus\": ${response.status},\n  \"responseId\": \"$responseId\",\n  \"promptSha256\": \"${sha256Bytes(prompt.toByteArray(StandardCharsets.UTF_8))}\",\n  \"rawResponseSha256\": \"${sha256Bytes(response.body.toByteArray(StandardCharsets.UTF_8))}\"\n}\n", StandardCharsets.UTF_8)
        val hard = response.status == 402 || response.body.lowercase().let { it.contains("insufficient_quota") || it.contains("credit_balance_exhausted") || it.contains("no credits remaining") || it.contains("billing") && it.contains("quota") }
        val extracted = extractText(response.body) ?: response.body
        return FormalResult(extracted, response.status, responseId, hard)
    }

    private fun classifyCapacityFailure(body: String): String = body.lowercase().let {
        when {
            it.contains("insufficient_quota") || it.contains("credit_balance_exhausted") || it.contains("no credits remaining") -> "insufficient_quota"
            it.contains("billing") -> "billing_unavailable"
            else -> "unavailable"
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

    private fun extractText(body: String): String? = runCatching {
        val root = Json.parse(body) as? JsonValue.Obj ?: return@runCatching null
        (root["output_text"] as? JsonValue.Str)?.value?.takeIf { it.isNotBlank() }?.let { return@runCatching it }
        val output = root["output"] as? JsonValue.Arr ?: return@runCatching null
        val chunks = output.items.flatMap { item ->
            val content = (item as? JsonValue.Obj)?.get("content") as? JsonValue.Arr ?: return@flatMap emptyList()
            content.items.mapNotNull { part ->
                val obj = part as? JsonValue.Obj ?: return@mapNotNull null
                if ((obj["type"] as? JsonValue.Str)?.value == "refusal") null else (obj["text"] as? JsonValue.Str)?.value
            }
        }
        chunks.joinToString("").ifBlank { null }
    }.getOrNull()

    private fun argument(args: Array<String>, name: String): String? = args.firstOrNull { it.startsWith("--$name=") }?.removePrefix("--$name=")

    private fun sha256Bytes(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
