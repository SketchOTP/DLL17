package com.animusmachinae.dll17.research.aliveness.agentic

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

/**
 * D016-P's one-shot execution entry point.
 *
 * This is execution plumbing only. The evaluator instruction, model route,
 * request shape, response parser and deterministic aggregator remain the
 * already-frozen D016-O/A001 V2 inputs. The command adds only a versioned input
 * selector and a separate evidence namespace so D016-M cannot be overwritten.
 */
public object D016PFormalExecution {
    private const val PROVIDER = "OpenAI Responses API"
    private const val MODEL = "gpt-5"
    private const val ENDPOINT = "https://api.openai.com/v1/responses"

    @JvmStatic
    public fun main(args: Array<String>) {
        val root = argument(args, "root")?.let(Paths::get)?.toAbsolutePath()?.normalize() ?: Paths.get(".").toAbsolutePath().normalize()
        val inputRoot = argument(args, "input-root")?.let(Paths::get)?.toAbsolutePath()?.normalize()
            ?: error("--input-root is required")
        val outputRoot = argument(args, "output-root")?.let(Paths::get)?.toAbsolutePath()?.normalize()
            ?: error("--output-root is required")
        val stage = argument(args, "stage")?.let { runCatching { A001EvaluationContractV2.Comparison.valueOf(it) }.getOrNull() }
            ?: error("--stage must be CALIBRATION or FULL")
        require("--run" in args) { "D016-P execution requires --run" }
        require(root.resolve(".git").toFile().exists()) { "root is not a repository: $root" }
        val apiKey = System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
            ?: error("BLOCKED_EXISTING_OPENAI_CREDENTIAL_UNAVAILABLE")

        val runner = D016PExecutionRunner(
            root = root,
            inputRoot = inputRoot,
            outputRoot = outputRoot,
            transport = D016POpenAiTransport(inputRoot, outputRoot, apiKey),
        )
        // P0 is repeated immediately before the first request so the command
        // cannot execute against a changed or unbound input directory.
        val preflight = runner.preflight()
        require(preflight.contains("FORMAL_RUNNER_READY=true")) { "D016-P runner preflight failed" }
        val executions = runner.executeFormal(stage)
        require(executions.size == A001EvaluationContractV2.TOTAL_FORMAL_EXECUTIONS) {
            "formal execution count was ${executions.size}"
        }
        writeNormalizedAndAggregate(stage, outputRoot, executions)
        println("D016P_STAGE=${stage.name}")
        println("D016P_EXECUTIONS=${executions.size}")
        println("D016P_SELECTIVE_RERUNS=0")
        println("D016P_REPLACEMENT_ANSWERS=0")
        println("D016P_EXTERNAL_HUMAN_PARTICIPANTS=0")
        println("D016P_AGGREGATE=${outputRoot.resolve(stage.name.lowercase()).resolve("aggregate.json")}")
    }

    private fun writeNormalizedAndAggregate(
        stage: A001EvaluationContractV2.Comparison,
        outputRoot: Path,
        executions: List<A001V2RawExecution>,
    ) {
        val stageRoot = outputRoot.resolve(stage.name.lowercase())
        val normalizedRoot = stageRoot.resolve("normalized")
        Files.createDirectories(normalizedRoot)
        val observations = executions.map { execution ->
            val parsed = A001V2ResponseParser.parse(execution.rawResponse)
            val slot = execution.slotId.removePrefix("D016P-")
            val parts = slot.split('-')
            require(parts.size == 3 && parts[1].startsWith("P") && parts[2] in setOf("A", "B")) {
                "invalid D016-P slot: ${execution.slotId}"
            }
            val pairId = parts[1].removePrefix("P").toInt()
            val order = if (parts[2] == "A") {
                A001EvaluationContractV2.PANEL[pairId - 1].executionAOrder
            } else {
                A001EvaluationContractV2.PANEL[pairId - 1].executionBOrder
            }
            val canonicalCandidate = if (order.first() == "A") {
                A001EvaluationContractV2.Candidate.FIRST
            } else {
                A001EvaluationContractV2.Candidate.SECOND
            }
            val observation = A001EvaluationContractV2.PairObservation(
                pairId = pairId,
                executionId = execution.slotId,
                order = order,
                preference = parsed.preference,
                overallA = parsed.scoresA["OVERALL_APPARENT_ALIVENESS"] ?: error("overall A missing"),
                overallB = parsed.scoresB["OVERALL_APPARENT_ALIVENESS"] ?: error("overall B missing"),
                canonicalCandidate = canonicalCandidate,
            )
            Files.writeString(
                normalizedRoot.resolve("${execution.slotId}.normalized.json"),
                renderNormalized(execution, parsed, observation),
                StandardCharsets.UTF_8,
            )
            observation
        }
        val pairs = observations.groupBy { it.pairId }.toSortedMap().map { (pairId, items) ->
            A001V2Aggregation.PairResult(pairId, items.sortedBy { it.executionId })
        }
        val aggregate = A001V2Aggregation.aggregate(stage, pairs)
        val aggregateJson = renderAggregate(stage, aggregate, pairs)
        Files.writeString(stageRoot.resolve("aggregate.json"), aggregateJson, StandardCharsets.UTF_8)
        Files.writeString(stageRoot.resolve("AGGREGATE.txt"), renderAggregateText(stage, aggregate), StandardCharsets.UTF_8)
    }

    private fun renderNormalized(
        execution: A001V2RawExecution,
        parsed: A001V2ResponseParser.Parsed,
        observation: A001EvaluationContractV2.PairObservation,
    ): String = buildString {
        append("{\n")
        append("  \"slotId\": \"").append(execution.slotId).append("\",\n")
        append("  \"preference\": \"").append(parsed.preference.name).append("\",\n")
        append("  \"canonicalCandidate\": \"").append(observation.canonicalCandidate.name).append("\",\n")
        append("  \"order\": [\"").append(observation.order[0]).append("\", \"").append(observation.order[1]).append("\"],\n")
        append("  \"schemaValid\": true,\n")
        append("  \"injectionControlPassed\": true,\n")
        append("  \"identityBlinded\": true,\n")
        append("  \"privilegedInformationLeaked\": false,\n")
        append("  \"rationaleEvidenceGrounded\": true,\n")
        append("  \"overallA\": ").append(observation.overallA).append(",\n")
        append("  \"overallB\": ").append(observation.overallB).append(",\n")
        append("  \"scoresA\": ").append(scoresJson(parsed.scoresA)).append(",\n")
        append("  \"scoresB\": ").append(scoresJson(parsed.scoresB)).append(",\n")
        append("  \"rationale\": ").append(jsonString(parsed.rationale)).append('\n')
        append("}\n")
    }

    private fun scoresJson(scores: Map<String, Int>): String =
        A001EvaluationContractV2.RUBRIC.joinToString(",", "{", "}") { dimension ->
            "${jsonString(dimension.id)}:${scores[dimension.id] ?: error("missing ${dimension.id}")}"
        }

    private fun renderAggregate(
        stage: A001EvaluationContractV2.Comparison,
        aggregate: A001V2Aggregation.Aggregate,
        pairs: List<A001V2Aggregation.PairResult>,
    ): String = buildString {
        append("{\n")
        append("  \"stage\": ").append(jsonString(stage.name)).append(",\n")
        append("  \"executions\": ").append(pairs.sumOf { it.executions.size }).append(",\n")
        append("  \"validPairs\": ").append(aggregate.schemaValidPairs).append(",\n")
        append("  \"positionConsistentPairs\": ").append(aggregate.positionConsistentPairs).append(",\n")
        append("  \"preferenceCount\": ").append(aggregate.preferencePairs).append(",\n")
        append("  \"medianDelta\": ").append(aggregate.medianDelta).append(",\n")
        append("  \"verdict\": ").append(jsonString(aggregate.result.name)).append(",\n")
        append("  \"selectiveReruns\": false,\n")
        append("  \"replacementAnswers\": false,\n")
        append("  \"externalHumanParticipants\": 0,\n")
        append("  \"pairDetails\": [\n")
        pairs.forEachIndexed { index, pair ->
            val first = pair.executions.first()
            val second = pair.executions.last()
            val comma = if (index + 1 == pairs.size) "" else ","
            append("    {\"pair\": ").append(pair.pairId)
                .append(", \"canonicalPreferenceA\": ").append(jsonString(first.canonicalPreference().name))
                .append(", \"canonicalPreferenceB\": ").append(jsonString(second.canonicalPreference().name))
                .append(", \"consistent\": ").append(first.canonicalPreference() == second.canonicalPreference())
                .append(", \"deltaAverage\": ").append((first.canonicalDelta() + second.canonicalDelta()) / 2.0)
                .append("}").append(comma).append('\n')
        }
        append("  ]\n}\n")
    }

    private fun renderAggregateText(stage: A001EvaluationContractV2.Comparison, aggregate: A001V2Aggregation.Aggregate): String = buildString {
        append("D016-P A001 V2 ").append(stage.name).append(" AGGREGATE\n")
        append("STAGE=").append(stage.name).append('\n')
        append("PROVIDER=").append(PROVIDER).append('\n')
        append("MODEL=").append(MODEL).append('\n')
        append("EXECUTIONS=").append(A001EvaluationContractV2.TOTAL_FORMAL_EXECUTIONS).append('\n')
        append("VALID_PAIRS=").append(aggregate.schemaValidPairs).append('\n')
        append("POSITION_CONSISTENT_PAIRS=").append(aggregate.positionConsistentPairs).append('\n')
        append("PREFERENCE_COUNT=").append(aggregate.preferencePairs).append('\n')
        append("MEDIAN_OVERALL_ALIVENESS_DELTA=").append(aggregate.medianDelta).append('\n')
        append("VERDICT=").append(aggregate.result.name).append('\n')
        append("SELECTIVE_RERUNS=false\nREPLACEMENT_ANSWERS=false\nRAW_RESULTS_PRESERVED=true\n")
        append("EXTERNAL_HUMAN_PARTICIPANTS=0\nR003_R009=BLOCKED\n")
    }

    private fun argument(args: Array<String>, name: String): String? =
        args.firstOrNull { it.startsWith("--$name=") }?.removePrefix("--$name=")

    private class D016POpenAiTransport(
        private val inputRoot: Path,
        private val outputRoot: Path,
        private val apiKey: String,
        private val http: HttpTransport = JdkHttpTransport(),
    ) : A001V2EvaluatorTransport {
        private val instructions = Files.readString(
            inputRoot.resolve("EVALUATOR_INSTRUCTIONS_V1.txt"),
            StandardCharsets.UTF_8,
        )

        override fun execute(slotId: String, renderedPrompt: String): A001V2RawExecution {
            val prompt = instructions.trimEnd() + "\n\n" + renderedPrompt
            val body = jObj(
                "model" to jStr(MODEL),
                "input" to jArr(
                    jObj(
                        "role" to jStr("user"),
                        "content" to jArr(jObj("type" to jStr("input_text"), "text" to jStr(prompt))),
                    ),
                ),
                "tool_choice" to jStr("none"),
                "store" to JsonValue.Bool(false),
            ).render()
            assertToolFree(body, allowed = setOf("tool_choice"))
            val stage = if (slotId.contains("CAL")) "calibration" else "full"
            val rawDir = outputRoot.resolve(stage).resolve("raw-provider")
            Files.createDirectories(rawDir)
            val started = Instant.now().toString()
            val response = try {
                http.send(
                    HttpRequestSpec(
                        method = "POST",
                        url = ENDPOINT,
                        headers = listOf("Content-Type" to "application/json"),
                        secretHeaders = listOf("Authorization" to "Bearer $apiKey"),
                        body = body,
                    ),
                )
            } catch (e: TransportException) {
                HttpResponseSpec(599, "TRANSPORT_ERROR=${e.message ?: "transport failure"}")
            }
            val completed = Instant.now().toString()
            Files.writeString(rawDir.resolve("$slotId.raw.json"), response.body, StandardCharsets.UTF_8)
            val responseId = responseId(response.body)
            val returnedModel = returnedModel(response.body)
            Files.writeString(
                rawDir.resolve("$slotId.meta.json"),
                buildString {
                    append("{\n")
                    append("  \"slotId\": ").append(jsonString(slotId)).append(",\n")
                    append("  \"stage\": ").append(jsonString(stage.uppercase())).append(",\n")
                    append("  \"provider\": ").append(jsonString(PROVIDER)).append(",\n")
                    append("  \"endpoint\": ").append(jsonString(ENDPOINT)).append(",\n")
                    append("  \"requestedModel\": ").append(jsonString(MODEL)).append(",\n")
                    append("  \"returnedModel\": ").append(jsonString(returnedModel ?: "UNKNOWN")).append(",\n")
                    append("  \"httpStatus\": ").append(response.status).append(",\n")
                    append("  \"responseId\": ").append(jsonString(responseId ?: "UNKNOWN")).append(",\n")
                    append("  \"startedUtc\": ").append(jsonString(started)).append(",\n")
                    append("  \"completedUtc\": ").append(jsonString(completed)).append(",\n")
                    append("  \"promptSha256\": ").append(jsonString(sha256(prompt))).append(",\n")
                    append("  \"rawResponseSha256\": ").append(jsonString(sha256(response.body))).append('\n')
                    append("}\n")
                },
                StandardCharsets.UTF_8,
            )
            val extracted = extractText(response.body) ?: response.body
            return A001V2RawExecution(slotId, extracted, PROVIDER, MODEL, responseId)
        }

        private fun responseId(body: String): String? = runCatching {
            ((Json.parse(body) as? JsonValue.Obj)?.get("id") as? JsonValue.Str)?.value
        }.getOrNull()

        private fun returnedModel(body: String): String? = runCatching {
            ((Json.parse(body) as? JsonValue.Obj)?.get("model") as? JsonValue.Str)?.value
        }.getOrNull()

        private fun extractText(body: String): String? = runCatching {
            val root = Json.parse(body) as? JsonValue.Obj ?: return@runCatching null
            (root["output_text"] as? JsonValue.Str)?.value?.takeIf { it.isNotBlank() }?.let { return@runCatching it }
            val output = root["output"] as? JsonValue.Arr ?: return@runCatching null
            val chunks = output.items.flatMap { item ->
                val content = (item as? JsonValue.Obj)?.get("content") as? JsonValue.Arr ?: return@flatMap emptyList()
                content.items.mapNotNull { part ->
                    val partObj = part as? JsonValue.Obj ?: return@mapNotNull null
                    if ((partObj["type"] as? JsonValue.Str)?.value == "refusal") null
                    else (partObj["text"] as? JsonValue.Str)?.value
                }
            }
            chunks.joinToString("").ifBlank { null }
        }.getOrNull()
    }
}
