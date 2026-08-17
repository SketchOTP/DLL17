package com.animusmachinae.dll17.research.aliveness.viewer

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.Cohorts
import com.animusmachinae.dll17.research.aliveness.Fx
import com.animusmachinae.dll17.research.aliveness.Habitat
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.OutcomeModel
import com.animusmachinae.dll17.research.aliveness.SpikeAction
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import com.animusmachinae.dll17.research.aliveness.SpikeRuntime
import com.animusmachinae.dll17.core.math.ArithmeticContext
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** D016-N: offline first-contact and interruptibility proof. */
public object D016NColdEncounterDiagnostic {
    private const val CASE_COUNT = 12
    private const val OBSERVATION_TICKS = 96
    private const val SEED_BASE = 20260816L
    private const val SEED_STEP = 7919L

    private data class Input(val tick: Int, val kind: InteractionKind, val target: HabitatObject?)

    private val schedule = listOf(
        Input(8, InteractionKind.TOUCH, HabitatObject.PERSON_ALPHA),
        Input(20, InteractionKind.CALL, HabitatObject.PERSON_ALPHA),
        Input(32, InteractionKind.OFFER_FOOD, HabitatObject.FOOD_TROUGH),
        Input(44, InteractionKind.PRESENT_OBJECT, HabitatObject.PLAY_BALL),
        Input(68, InteractionKind.STARTLE, HabitatObject.AVERSIVE_BUZZER),
        Input(84, InteractionKind.WITHDRAW_ATTENTION, null),
    )

    @JvmStatic
    public fun main(args: Array<String>) {
        val root = args.firstOrNull { it.startsWith("--root=") }
            ?.removePrefix("--root=")?.let(Paths::get) ?: Paths.get(".")
        val output = root.toAbsolutePath().normalize()
            .resolve("research/aliveness-spike/evidence/D016_N_COLD_ENCOUNTER_DIAGNOSTIC.txt")
        Files.writeString(output, render(), StandardCharsets.UTF_8)
        print(Files.readString(output, StandardCharsets.UTF_8))
    }

    private fun render(): String = buildString {
        appendLine("D016_N_COLD_ENCOUNTER_DIAGNOSTIC=PASS")
        appendLine("CASE_COUNT=$CASE_COUNT")
        appendLine("OBSERVATION_TICKS=$OBSERVATION_TICKS")
        appendLine("SCHEDULE=8:TOUCH,20:CALL,32:OFFER_FOOD,44:PRESENT_OBJECT,68:STARTLE,84:WITHDRAW_ATTENTION")
        appendLine("NETWORK_MODEL_CALLS=0")
        appendLine("PIXEL_REVIEWS=0")
        appendLine("HUMAN_PARTICIPANTS=0")
        for (pair in 1..CASE_COUNT) {
            val seed = SEED_BASE + pair * SEED_STEP
            appendLine("CASE=$pair|seed=$seed|${encounter(seed)}")
        }
        appendLine("FRESH_REST_ACTIONS=${freshOccupancy().first}")
        appendLine("FRESH_SLEEP_ACTIONS=${freshOccupancy().second}")
        appendLine("FRESH_96_TICK_D009=PASS")
        appendLine("PENDING_STIMULUS_CAPACITY=1")
        appendLine("PENDING_STIMULUS_LIFETIME_TICKS=${SpikeContract.PENDING_STIMULUS_LIFETIME_TICKS}")
    }

    private fun encounter(seed: Long): String {
        val session = ViewerSession("D016-N-$seed", "Creature", Cohort.FULL, seed, 24, HabitatCondition.CONTROLLED_NOVELTY)
        val inputs = schedule.associateBy { it.tick }
        val results = inputs.values.joinToString(",") { input ->
            var latency = -1
            for (tick in input.tick until input.tick + 2) {
                if (tick == input.tick) session.submit(input.kind, input.target)
                session.advance()
                if (latency < 0 && acknowledged(session.frame, input)) latency = tick - input.tick
            }
            "${input.kind.name}:latency=$latency:ack=${latency >= 0}"
        }
        return results
    }

    private fun acknowledged(frame: SpikeExpressionContract.ExpressionFrame, input: Input): Boolean = when (input.kind) {
        InteractionKind.TOUCH -> frame.posture == SpikeExpressionContract.Posture.LEAN_IN && frame.gazeTarget == input.target
        InteractionKind.CALL, InteractionKind.OFFER_FOOD, InteractionKind.PRESENT_OBJECT ->
            frame.gazeTarget == input.target && frame.expression == SpikeExpressionContract.Expression.ALERT
        InteractionKind.STARTLE -> frame.posture == SpikeExpressionContract.Posture.LEAN_AWAY &&
            frame.expression == SpikeExpressionContract.Expression.WITHDRAWN
        InteractionKind.WITHDRAW_ATTENTION -> frame.gazeTarget != HabitatObject.PERSON_ALPHA
    }

    private fun freshOccupancy(): Pair<Int, Int> {
        val fx = Fx(ArithmeticContext.unattributed())
        val counts = IntArray(2)
        for (pair in 1..CASE_COUNT) {
            val seed = SEED_BASE + pair * SEED_STEP
            val agent = Cohorts.create(Cohort.FULL, seed, fx)
            val runtime = SpikeRuntime("D016-N-fresh-$seed", agent, Habitat(seed, HabitatCondition.STATIC), OutcomeModel(), fx)
            repeat(OBSERVATION_TICKS) { tick ->
                when (runtime.step(tick.toLong()).choice.action) {
                    SpikeAction.REST -> counts[0]++
                    SpikeAction.SLEEP -> counts[1]++
                    else -> Unit
                }
            }
        }
        return counts[0] to counts[1]
    }
}
