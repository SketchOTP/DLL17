package com.animusmachinae.dll17.research.aliveness.sim

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.ScriptedPetAgent
import com.animusmachinae.dll17.research.aliveness.SpikeAction
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract

/**
 * The complete disclosure of `ScriptedPetBaselineV1`, for the independent owner
 * who has to decide whether it is a fair comparator.
 *
 * `BaselineQualificationProtocolV1` requires the contingency set, script, hold
 * windows, interaction coverage, parameters and expression-contract version to
 * be hashed and pinned. This emits all of it from the implementation itself, so
 * the manifest cannot drift away from the thing it describes: change the
 * baseline and this output changes with it.
 *
 * It also carries the objective comparison between the strong baseline, the
 * degraded control and FULL. Those numbers were previously produced by an ad-hoc
 * diagnostic and quoted in prose, which meant nothing checked them.
 *
 * None of this is a competence qualification. Objective breadth is not apparent
 * aliveness, and only people can supply the latter.
 */
public object BaselineCoverageManifest {

    public const val MANIFEST_ID: String = "BaselineCoverageManifestV1"

    /** Matched probe: same seed, same habitat, same window, for every cohort. */
    private const val PROBE_SEED: Long = 606L
    private const val PROBE_DAYS: Int = 40
    private const val PROBE_WINDOW_DAYS: Int = 15

    @JvmStatic
    public fun main(args: Array<String>) {
        println(render())
    }

    public fun render(): String = buildString {
        append("BASELINE_COVERAGE_MANIFEST=").append(MANIFEST_ID).append('\n')
        append("comparator=").append(SpikeContract.SCRIPTED_BASELINE_ID)
        append("  control=").append(SpikeContract.DEGRADED_CONTROL_ID).append('\n')
        append("expressionContract=").append(SpikeContract.EXPRESSION_CONTRACT_ID)
        append(" v").append(SpikeExpressionContract.CONTRACT_VERSION).append('\n')
        append("qualificationStatus=NOT_INDEPENDENTLY_QUALIFIED\n\n")

        append("== AUTHORED DRIVE ANALOGUES (not learned; no outcome persists as value)\n")
        append("  hungerActThreshold=").append(fx(ScriptedPetAgent.HUNGER_ACT)).append('\n')
        append("  fatigueActThreshold=").append(fx(ScriptedPetAgent.FATIGUE_ACT)).append('\n')
        append("  fatigueRestThreshold=").append(fx(ScriptedPetAgent.FATIGUE_REST)).append('\n')
        append("  attentionActThreshold=").append(fx(ScriptedPetAgent.ATTENTION_ACT)).append('\n')
        append('\n')

        append("== AUTHORED CONTINGENCY TABLE (priority order, strong baseline)\n")
        for ((index, rule) in CONTINGENCIES.withIndex()) {
            append("  ").append((index + 1).toString().padStart(2)).append(". ").append(rule)
            append('\n')
        }
        append('\n')

        append("== INTERACTION COVERAGE\n")
        append("  kinds=").append(InteractionKind.entries.size).append('\n')
        for (kind in InteractionKind.entries) {
            val strong = REACTIONS[kind] ?: "-"
            val degraded = if (kind in DEGRADED_IGNORES) "IGNORED" else strong
            append("  ").append(kind.name.padEnd(20))
            append("strong=").append(strong.padEnd(25))
            append("degraded=").append(degraded).append('\n')
        }
        append("  reactionWindowTicks strong=10 degraded=4\n")
        append('\n')

        append("== IDLE AND PLAY SCRIPT\n")
        append("  strong: entries=").append(ScriptedPetAgent.BASELINE_SCRIPT.size)
        append(" holdTicks=12 advance=seeded draw over the whole script\n")
        for ((action, target) in ScriptedPetAgent.BASELINE_SCRIPT) {
            append("    ").append(action.name)
            target?.let { append('@').append(it.name) }
            append('\n')
        }
        append("  degraded: entries=").append(ScriptedPetAgent.DEGRADED_SCRIPT.size)
        append(" holdTicks=40 advance=fixed cycle\n")
        for ((action, target) in ScriptedPetAgent.DEGRADED_SCRIPT) {
            append("    ").append(action.name)
            target?.let { append('@').append(it.name) }
            append('\n')
        }
        append('\n')

        append("== SURFACE COVERAGE\n")
        val strongActions = coverage(ScriptedPetAgent.BASELINE_SCRIPT, REACTION_ACTIONS_STRONG)
        val degradedActions = coverage(ScriptedPetAgent.DEGRADED_SCRIPT, REACTION_ACTIONS_DEGRADED)
        append("  strong: distinctActions=").append(strongActions.size)
        append('/').append(SpikeAction.ALL.size)
        append(" distinctObjects=").append(objects(ScriptedPetAgent.BASELINE_SCRIPT).size)
        append('/').append(HabitatObject.entries.size).append('\n')
        append("  degraded: distinctActions=").append(degradedActions.size)
        append('/').append(SpikeAction.ALL.size)
        append(" distinctObjects=").append(objects(ScriptedPetAgent.DEGRADED_SCRIPT).size)
        append('/').append(HabitatObject.entries.size).append('\n')
        append('\n')

        append("== MATCHED OBJECTIVE PROBE\n")
        append("  seed=").append(PROBE_SEED).append(" days=").append(PROBE_DAYS)
        append(" windowDays=").append(PROBE_WINDOW_DAYS)
        append(" condition=").append(HabitatCondition.CONTROLLED_NOVELTY.name).append('\n')
        append("  cohort".padEnd(32)).append("entropy   objects/d  occupancy inactivity regularity")
        append(" revisits/d\n")
        for (cohort in listOf(
            Cohort.SCRIPTED_PET_BASELINE,
            Cohort.DEGRADED_SCRIPTED_CONTROL,
            Cohort.FULL,
        )) {
            val m = AcceleratedSimulator.run(
                RunConfig(
                    "baseline-manifest", cohort, PROBE_SEED, PROBE_DAYS,
                    HabitatCondition.CONTROLLED_NOVELTY, windowDays = PROBE_WINDOW_DAYS,
                ),
            ).measures
            append("  ").append(cohort.cohortId.padEnd(30))
            append(RunMeasures.d6(m.windowActionEntropyBits())).append("  ")
            append(RunMeasures.d6(m.distinctObjectsInspectedPerDay())).append("  ")
            append(RunMeasures.d6(m.maxWindowOccupancy())).append("  ")
            append(RunMeasures.d6(m.windowInactivityShare())).append("  ")
            append(RunMeasures.d6(m.cycleRegularity())).append("  ")
            append(RunMeasures.d6(m.revisitationsPerDay())).append('\n')
        }
        append('\n')
        append("NOTE: objective breadth is not apparent aliveness. This manifest exists so\n")
        append("      the independent owner can see exactly what the comparator is, and\n")
        append("      reject or strengthen it, before any person rates anything.\n")
    }

    private fun coverage(
        script: List<Pair<SpikeAction, HabitatObject?>>,
        reactions: Set<SpikeAction>,
    ): Set<SpikeAction> =
        script.map { it.first }.toSet() + reactions + SCRIPTED_DRIVE_ACTIONS

    private fun objects(script: List<Pair<SpikeAction, HabitatObject?>>): Set<HabitatObject> =
        script.mapNotNull { it.second }.toSet() + HabitatObject.FOOD_TROUGH +
            HabitatObject.PERSON_ALPHA + HabitatObject.PERSON_BETA + HabitatObject.NOVEL_SLOT

    private val SCRIPTED_DRIVE_ACTIONS = setOf(
        SpikeAction.EAT, SpikeAction.SLEEP, SpikeAction.REST, SpikeAction.SEEK_INTERACTION,
    )

    private val REACTION_ACTIONS_STRONG = setOf(
        SpikeAction.RESPOND_TO_TOUCH, SpikeAction.ORIENT, SpikeAction.EAT,
        SpikeAction.OBSERVE, SpikeAction.IDLE_VARIATION, SpikeAction.WITHDRAW,
    )

    private val REACTION_ACTIONS_DEGRADED = setOf(
        SpikeAction.RESPOND_TO_TOUCH, SpikeAction.EAT, SpikeAction.WITHDRAW,
    )

    private val DEGRADED_IGNORES = setOf(
        InteractionKind.CALL, InteractionKind.PRESENT_OBJECT, InteractionKind.WITHDRAW_ATTENTION,
    )

    private val REACTIONS: Map<InteractionKind, String> = mapOf(
        InteractionKind.TOUCH to "RESPOND_TO_TOUCH@person",
        InteractionKind.CALL to "ORIENT@person",
        InteractionKind.OFFER_FOOD to "EAT@FOOD_TROUGH",
        InteractionKind.PRESENT_OBJECT to "OBSERVE@target",
        InteractionKind.WITHDRAW_ATTENTION to "IDLE_VARIATION",
        InteractionKind.STARTLE to "WITHDRAW@target",
    )

    private val CONTINGENCIES: List<String> = listOf(
        "reacting to a live interaction event -> the reaction for that kind",
        "hunger above threshold AND food present -> EAT@FOOD_TROUGH (tier 3)",
        "fatigue above threshold AND night -> SLEEP (tier 3)",
        "fatigue above rest threshold -> REST (tier 3, strong baseline only)",
        "attention want above threshold AND not in refractory -> SEEK_INTERACTION, " +
            "alternating between the two people (strong) or always PERSON_ALPHA (degraded)",
        "a causal change at the novel slot -> OBSERVE@NOVEL_SLOT (tier 4, strong only)",
        "otherwise -> the idle/play script entry currently held",
    )

    private fun fx(raw: Long): String = RunMeasures.fx(raw)
}
