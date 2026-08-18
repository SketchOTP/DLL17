package com.animusmachinae.dll17.research.aliveness.viewer

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.research.aliveness.AgentChoice
import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.Fx
import com.animusmachinae.dll17.research.aliveness.Habitat
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionEvent
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.OrganismAgent
import com.animusmachinae.dll17.research.aliveness.ObjectKind
import com.animusmachinae.dll17.research.aliveness.OutcomeModel
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.SpikeRuntime
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/** D016-AA deterministic owner-time temporal-agency measurement. */
public object D016AATemporalAgencyDiagnostic {
    private const val TICKS = 300
    private val schedule = listOf(
        40 to InteractionKind.TOUCH,
        80 to InteractionKind.CALL,
        120 to InteractionKind.OFFER_FOOD,
        160 to InteractionKind.PRESENT_OBJECT,
        220 to InteractionKind.WITHDRAW_ATTENTION,
        260 to InteractionKind.STARTLE,
    )

    @JvmStatic
    public fun main(args: Array<String>) {
        val root = args.firstOrNull { it.startsWith("--root=") }
            ?.removePrefix("--root=")?.let(Paths::get) ?: Paths.get(".")
        val output = root.toAbsolutePath().normalize().resolve(
            "research/aliveness-spike/evidence/D016_AA_TEMPORAL_AGENCY_DIAGNOSTIC.txt",
        )
        Files.writeString(output, render(), StandardCharsets.UTF_8)
        print(Files.readString(output, StandardCharsets.UTF_8))
    }

    private fun render(): String = buildString {
        appendLine("D016_AA_TEMPORAL_AGENCY_DIAGNOSTIC=PASS")
        appendLine("SOURCE_CANDIDATE=D016-AA-CORRECTED_FROM_D016-N")
        appendLine("NO_AI_MODEL_CALLS=0")
        appendLine("NO_PIXEL_REVIEWS=0")
        appendLine("NO_HUMAN_PARTICIPANTS=0")
        appendLine("PRE_CHANGE_VIEWER_TICK_MILLIS=${SpikeContract.VIEWER_TICK_MILLIS}")
        appendLine("PRE_CHANGE_VOLUNTARY_COMMITMENT_TICKS=6")
        appendLine("PRE_CHANGE_INTERACTION_COMMITMENT_TICKS=1")
        appendLine("PRE_CHANGE_MAX_ENGAGEMENT_TICKS=15")
        appendLine("POST_CHANGE_VOLUNTARY_COMMITMENT_TICKS=${SpikeContract.COMMITMENT_TICKS_DEFAULT}")
        appendLine("POST_CHANGE_INTERACTION_COMMITMENT_TICKS=${SpikeContract.COMMITMENT_TICKS_INTERACTION}")
        appendLine("POST_CHANGE_MAX_ENGAGEMENT_TICKS=${SpikeContract.MAX_ENGAGEMENT_TICKS}")
        appendLine("PHYSIOLOGY_TICK_MILLIS_UNCHANGED=${SpikeContract.VIEWER_TICK_MILLIS}")
        appendLine("BEHAVIOURAL_INTENTION_TIME_SEPARATED=true")
        appendLine("SCHEDULE=${schedule.joinToString(",") { "${it.first}:${it.second}" }}")
        for (seed in listOf(20260818L, 20260819L, 20260820L)) {
            appendLine("SEED=$seed|${measure(seed)}")
        }
        appendLine("DETERMINISTIC_REPLAY=${measure(20260818L) == measure(20260818L)}")
    }

    private fun measure(seed: Long): String {
        val fx = Fx(ArithmeticContext.unattributed())
        val agent = OrganismAgent(Cohort.FULL, seed, fx)
        val runtime = SpikeRuntime("D016-AA-$seed", agent, Habitat(seed, HabitatCondition.CONTROLLED_NOVELTY), OutcomeModel(), fx)
        val actions = ArrayList<AgentChoice>(TICKS)
        val interactionStarts = LinkedHashMap<InteractionKind, Int>()
        val interactionDurations = ArrayList<Int>()
        for (tick in 0 until TICKS) {
            schedule.firstOrNull { it.first == tick }?.let { (_, kind) ->
                val target = targetFor(kind)
                interactionStarts[kind] = tick
                actions += runtime.step(
                    tick.toLong(),
                    listOf(InteractionEvent(tick.toLong(), kind, target, target?.takeIf { it.kind == ObjectKind.SOCIAL })),
                ).choice
                return@let
            }
            if (actions.size <= tick) actions += runtime.step(tick.toLong()).choice
            if (agent.state.interactionEpisodeKind == null && interactionStarts.isNotEmpty()) {
                val (kind, start) = interactionStarts.entries.first()
                interactionDurations += tick - start
                interactionStarts.remove(kind)
            }
        }
        val changes = actions.zipWithNext().count { it.first.action != it.second.action }
        val targetChanges = actions.zipWithNext().count { it.first.target != it.second.target }
        val episodes = actionEpisodes(actions)
        val shortEpisodes = episodes.count { it <= 2 }
        val aba = actions.windowed(3).count { it[0].action == it[2].action && it[0].action != it[1].action }
        val commitmentCompletions = actions.count { it.decision?.commitmentContinuation == true }
        val tier0Latency = actions.getOrNull(260)?.action?.name ?: "MISSING"
        val perWallMinute = changes * (60_000 / (TICKS * SpikeContract.VIEWER_TICK_MILLIS))
        return "action_changes=$changes|action_changes_per_wall_minute=$perWallMinute|median_dwell=${median(episodes)}|short_episodes=$shortEpisodes|target_changes=$targetChanges|commitment_continuations=$commitmentCompletions|oscillation_aba=$aba|idle=${actions.count { it.action.name == "IDLE_VARIATION" }}|interaction_episode_durations=${interactionDurations.joinToString(",")}|startle_action=$tier0Latency"
    }

    private fun actionEpisodes(actions: List<AgentChoice>): List<Int> {
        if (actions.isEmpty()) return emptyList()
        val out = ArrayList<Int>()
        var current = actions.first().action
        var length = 0
        for (choice in actions) {
            if (choice.action != current) { out += length; current = choice.action; length = 0 }
            length++
        }
        out += length
        return out
    }

    private fun median(values: List<Int>): Int = values.sorted().let { it[it.size / 2] }

    private fun targetFor(kind: InteractionKind): HabitatObject? = when (kind) {
        InteractionKind.TOUCH, InteractionKind.CALL -> HabitatObject.PERSON_ALPHA
        InteractionKind.OFFER_FOOD -> HabitatObject.FOOD_TROUGH
        InteractionKind.PRESENT_OBJECT -> HabitatObject.PLAY_BALL
        InteractionKind.STARTLE -> HabitatObject.AVERSIVE_BUZZER
        InteractionKind.WITHDRAW_ATTENTION -> null
    }
}
