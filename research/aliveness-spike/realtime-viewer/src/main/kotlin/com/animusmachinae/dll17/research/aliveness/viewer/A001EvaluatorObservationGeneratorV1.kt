package com.animusmachinae.dll17.research.aliveness.viewer

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * D016-M-R1: deterministic projection of the existing user-visible viewer
 * surface. This class may read a cohort internally to generate a case, but the
 * rendered bundle contains only what [ViewerSession] exposes to an ordinary
 * viewer: presentation fields, visible objects, and input timing.
 */
public object A001EvaluatorObservationGeneratorV1 {
    public const val GENERATOR_ID: String = "A001EvaluatorObservationGeneratorV1"
    public const val GENERATOR_VERSION: Int = 1
    public const val PROTOCOL_ID: String = "A001ObservationProtocolV1"
    public const val CASE_COUNT: Int = 12
    public const val OBSERVATION_TICKS: Int = 96
    public const val TICK_MILLIS: Int = 250
    public const val SEED_BASE: Long = 20260816L
    public const val SEED_STEP: Long = 7919L

    private data class ScheduledInput(
        val tick: Int,
        val kind: InteractionKind,
        val target: HabitatObject?,
    )

    private val schedule: List<ScheduledInput> = listOf(
        ScheduledInput(8, InteractionKind.TOUCH, HabitatObject.PERSON_ALPHA),
        ScheduledInput(20, InteractionKind.CALL, HabitatObject.PERSON_ALPHA),
        ScheduledInput(32, InteractionKind.OFFER_FOOD, HabitatObject.FOOD_TROUGH),
        ScheduledInput(44, InteractionKind.PRESENT_OBJECT, HabitatObject.PLAY_BALL),
        ScheduledInput(68, InteractionKind.STARTLE, HabitatObject.AVERSIVE_BUZZER),
        ScheduledInput(84, InteractionKind.WITHDRAW_ATTENTION, null),
    )

    public data class GeneratedCase(
        val caseId: String,
        val seed: Long,
        val renderedBundle: String,
    )

    /** Generate all 24 canonical neutral bundles. No network or model calls. */
    public fun generateAll(root: Path): List<GeneratedCase> {
        val output = root.resolve("research/aliveness-spike/evidence/a001-v2/formal-input")
        val sets = listOf(
            Triple("calibration", Cohort.SCRIPTED_PET_BASELINE, Cohort.DEGRADED_SCRIPTED_CONTROL),
            Triple("qualification", Cohort.FULL, Cohort.SCRIPTED_PET_BASELINE),
        )
        val cases = mutableListOf<GeneratedCase>()
        for ((setName, first, second) in sets) {
            val directory = output.resolve(setName)
            Files.createDirectories(directory)
            for (pair in 1..CASE_COUNT) {
                val caseId = if (setName == "calibration") "CAL-P${pair.toString().padStart(2, '0')}" else "FULL-P${pair.toString().padStart(2, '0')}"
                val seed = SEED_BASE + pair * SEED_STEP
                val rendered = renderCase(caseId, seed, first, second)
                val file = directory.resolve("$caseId.txt")
                Files.writeString(file, rendered, StandardCharsets.UTF_8)
                cases += GeneratedCase(caseId, seed, rendered)
            }
        }
        return cases
    }

    private fun renderCase(caseId: String, seed: Long, first: Cohort, second: Cohort): String {
        val firstLines = observe("A", "$caseId-A", first, seed)
        val secondLines = observe("B", "$caseId-B", second, seed)
        val neutralCaseId = caseId.substringAfter('-')
        return buildString {
            append("A001_OBSERVATION_BUNDLE_V1\n")
            append("PROTOCOL=").append(PROTOCOL_ID).append('\n')
            append("CASE=").append(neutralCaseId).append('\n')
            append("SEED_DOMAIN=matched-opaque-" ).append(seed).append('\n')
            append("DURATION_TICKS=").append(OBSERVATION_TICKS).append('\n')
            append("TICK_MILLIS=").append(TICK_MILLIS).append('\n')
            append("CREATURE_A\n")
            firstLines.forEach { append(it).append('\n') }
            append("CREATURE_B\n")
            secondLines.forEach { append(it).append('\n') }
            append("END_BUNDLE\n")
        }
    }

    private fun observe(
        label: String,
        sessionId: String,
        cohort: Cohort,
        seed: Long,
    ): List<String> {
        val session = ViewerSession(
            sessionId = sessionId,
            displayLabel = "Creature $label",
            cohort = cohort,
            seed = seed,
            durationSeconds = OBSERVATION_TICKS * TICK_MILLIS / 1_000,
            condition = HabitatCondition.CONTROLLED_NOVELTY,
        )
        val inputs = schedule.associateBy { it.tick }
        return (0 until OBSERVATION_TICKS).map { tick ->
            inputs[tick]?.let { input -> session.submit(input.kind, input.target) }
            session.advance()
            val frame = session.frame
            val input = inputs[tick]?.let { "${it.kind.name}:${it.target?.name ?: "NONE"}" } ?: "NONE"
            val visibleObjects = session.presentObjects().joinToString(",") { it.name }
            "t=$tick|input=$input|posture=${frame.posture.name}|expression=${frame.expression.name}|" +
                "gaze=${frame.gazeTarget?.name ?: "NONE"}|motion=${frame.motionAmplitude}|" +
                "vocal=${frame.vocalizing}|micro=${frame.microMovement}|attention=${frame.attentionObject?.name ?: "NONE"}|" +
                "objects=$visibleObjects"
        }
    }

    @JvmStatic
    public fun main(args: Array<String>) {
        val root = args.firstOrNull { it.startsWith("--root=") }
            ?.removePrefix("--root=")?.let(Paths::get) ?: Paths.get(".")
        val cases = generateAll(root.toAbsolutePath().normalize())
        println("A001_OBSERVATIONS_GENERATED=${cases.size}")
        println("PROTOCOL=$PROTOCOL_ID")
        println("GENERATOR=$GENERATOR_ID-v$GENERATOR_VERSION")
        println("NETWORK_MODEL_CALLS=0")
    }
}
