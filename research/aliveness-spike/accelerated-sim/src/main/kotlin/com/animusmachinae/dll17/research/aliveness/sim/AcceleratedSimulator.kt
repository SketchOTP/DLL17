package com.animusmachinae.dll17.research.aliveness.sim

import com.animusmachinae.dll17.research.aliveness.AttributionClass
import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.Cohorts
import com.animusmachinae.dll17.research.aliveness.Controller
import com.animusmachinae.dll17.research.aliveness.CuriosityParameters
import com.animusmachinae.dll17.research.aliveness.Fx
import com.animusmachinae.dll17.research.aliveness.Habitat
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionEvent
import com.animusmachinae.dll17.research.aliveness.OrganismAgent
import com.animusmachinae.dll17.research.aliveness.OutcomeModel
import com.animusmachinae.dll17.research.aliveness.SaturationCounter
import com.animusmachinae.dll17.research.aliveness.SpikeAction
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.SpikeDecisionTrace
import com.animusmachinae.dll17.research.aliveness.SpikeRuntime

/** One accelerated run's configuration. */
public class RunConfig(
    public val runId: String,
    public val cohort: Cohort,
    public val seed: Long,
    public val virtualDays: Int,
    public val condition: HabitatCondition = HabitatCondition.CONTROLLED_NOVELTY,
    public val outcomes: OutcomeModel = OutcomeModel(),
    public val curiosity: CuriosityParameters = CuriosityParameters.DEFAULT,
    public val attributionSampleEvery: Int = 0,
    public val windowDays: Int = 5,
    public val keepTraces: Int = 0,
    public val interactions: (Long) -> List<InteractionEvent> = { emptyList() },
) {
    public val totalTicks: Long
        get() = virtualDays.toLong() * SpikeContract.TICKS_PER_VIRTUAL_DAY
}

/** Learning-state samples taken at day boundaries. */
public class LearningSeries {
    public val fearAversive: MutableList<Long> = ArrayList()
    public val preferenceBall: MutableList<Long> = ArrayList()
    public val preferenceMirror: MutableList<Long> = ArrayList()
    public val preferenceTrough: MutableList<Long> = ArrayList()
    public val preferenceCache: MutableList<Long> = ArrayList()
    public val habitEatTrough: MutableList<Long> = ArrayList()
    public val habitEatCache: MutableList<Long> = ArrayList()
    public val habituationMirror: MutableList<Long> = ArrayList()
    public val relationshipAlpha: MutableList<Long> = ArrayList()
    public val relationshipBeta: MutableList<Long> = ArrayList()
}

/** Everything one accelerated run produced. */
public class RunResult(
    public val config: RunConfig,
    public val measures: RunMeasures,
    public val series: LearningSeries,
    public val clampCount: Long,
    public val overflowCount: Long,
    public val stateFootprintSlots: Int,
    public val finalStateSignature: Long,
    public val traces: List<SpikeDecisionTrace>,
    public val actionsByTick: IntArray?,
)

/**
 * The deterministic accelerated simulator.
 *
 * Given the same config it produces the same result, every time, on any JVM: the
 * organism's arithmetic is the frozen R001 fixed-point library and the only
 * randomness is seeded and substream-isolated.
 */
public object AcceleratedSimulator {

    public fun run(config: RunConfig, recordActions: Boolean = false): RunResult {
        val (fx, saturation) = Fx.counting()
        return runWith(config, fx, saturation, recordActions)
    }

    public fun runWith(
        config: RunConfig,
        fx: Fx,
        saturation: SaturationCounter,
        recordActions: Boolean = false,
    ): RunResult {
        val habitat = Habitat(config.seed, config.condition)
        val agent = Cohorts.create(config.cohort, config.seed, fx, config.curiosity)
        val runtime = SpikeRuntime(
            runId = config.runId,
            agent = agent,
            habitat = habitat,
            outcomes = config.outcomes,
            fx = fx,
            traceEveryDecision = false,
            attributionSampleEvery = config.attributionSampleEvery,
        )

        val total = config.totalTicks
        val measures = RunMeasures(config.windowDays * SpikeContract.TICKS_PER_VIRTUAL_DAY)
        val series = LearningSeries()
        val traces = ArrayList<SpikeDecisionTrace>()
        val actions = if (recordActions) IntArray(total.toInt()) else null

        for (tick in 0 until total) {
            val record = runtime.step(tick, config.interactions(tick))
            measures.record(
                tick = tick,
                action = record.choice.action,
                target = record.choice.target,
                epistemic = Controller.epistemic(record.choice.action),
                totalTicks = total,
            )
            record.attribution?.let {
                measures.recordAttribution(it.attributionClass, it.tieBreakOutcomeDetermining)
            }
            record.trace?.let { if (traces.size < config.keepTraces) traces += it }
            actions?.set(tick.toInt(), record.choice.action.ordinal)

            if ((tick + 1) % SpikeContract.TICKS_PER_VIRTUAL_DAY == 0L) {
                sample(agent, series)
            }
        }

        val organism = agent as? OrganismAgent
        return RunResult(
            config = config,
            measures = measures,
            series = series,
            clampCount = saturation.clampCount,
            overflowCount = saturation.overflowCount,
            stateFootprintSlots = organism?.state?.stateFootprintSlots() ?: 0,
            finalStateSignature = organism?.state?.stateSignature() ?: 0L,
            traces = traces,
            actionsByTick = actions,
        )
    }

    private fun sample(agent: com.animusmachinae.dll17.research.aliveness.SpikeAgent, s: LearningSeries) {
        val organism = agent as? OrganismAgent ?: return
        val st = organism.state
        s.fearAversive += st.fear[HabitatObject.AVERSIVE_BUZZER.ordinal0]
        s.preferenceBall += st.preference[HabitatObject.PLAY_BALL.ordinal0]
        s.preferenceMirror += st.preference[HabitatObject.PLAY_MIRROR.ordinal0]
        s.preferenceTrough += st.preference[HabitatObject.FOOD_TROUGH.ordinal0]
        s.preferenceCache += st.preference[HabitatObject.FOOD_CACHE.ordinal0]
        s.habitEatTrough += st.habit[st.index(SpikeAction.EAT, HabitatObject.FOOD_TROUGH)]
        s.habitEatCache += st.habit[st.index(SpikeAction.EAT, HabitatObject.FOOD_CACHE)]
        s.habituationMirror += st.habituation[HabitatObject.PLAY_MIRROR.ordinal0]
        s.relationshipAlpha += st.relationship[HabitatObject.PERSON_ALPHA.ordinal0]
        s.relationshipBeta += st.relationship[HabitatObject.PERSON_BETA.ordinal0]
    }

    /**
     * History dependence under matched present stimuli.
     *
     * Two organisms with the *same seed* — therefore identical traits and
     * identical tie-break stream — are given different controlled histories, then
     * placed in an identical probe. Any behavioural difference in the probe is
     * attributable to what they learned, because nothing else about them differs.
     */
    public fun historyDependence(
        cohort: Cohort,
        seed: Long,
        conditioningDays: Int,
        probeDays: Int,
    ): HistoryDependenceResult {
        val (fxA, satA) = Fx.counting()
        val (fxB, satB) = Fx.counting()

        val agentA = Cohorts.create(cohort, seed, fxA)
        val agentB = Cohorts.create(cohort, seed, fxB)
        val habitatA = Habitat(seed, HabitatCondition.STATIC)
        val habitatB = Habitat(seed, HabitatCondition.STATIC)

        // The two histories must differ in something the organism reliably
        // encounters. Food and social response qualify; the aversive object does
        // not, because an organism may simply never engage it. History A: the
        // trough is the reliable source and only alpha responds. History B: the
        // cache is reliable, beta responds too, and the aversive object is safe.
        val runtimeA = SpikeRuntime("hist-A", agentA, habitatA, OutcomeModel(), fxA)
        val runtimeB = SpikeRuntime(
            "hist-B", agentB, habitatB,
            OutcomeModel(
                aversiveSafeFromTick = 0L,
                contingencyReversalTick = 0L,
                betaBecomesAttentiveTick = 0L,
                socialHoursShifted = true,
            ),
            fxB,
        )

        val conditioningTicks = conditioningDays.toLong() * SpikeContract.TICKS_PER_VIRTUAL_DAY
        for (tick in 0 until conditioningTicks) {
            runtimeA.step(tick)
            runtimeB.step(tick)
        }

        // Identical probe: same outcome model, same stimuli, same ticks.
        val probeOutcomes = OutcomeModel()
        val probeA = SpikeRuntime("probe-A", agentA, habitatA, probeOutcomes, fxA)
        val probeB = SpikeRuntime("probe-B", agentB, habitatB, probeOutcomes, fxB)
        val probeTicks = probeDays.toLong() * SpikeContract.TICKS_PER_VIRTUAL_DAY

        var differing = 0L
        var differingTarget = 0L
        for (offset in 0 until probeTicks) {
            val tick = conditioningTicks + offset
            val a = probeA.step(tick)
            val b = probeB.step(tick)
            if (a.choice.action != b.choice.action) differing += 1
            if (a.choice.target != b.choice.target) differingTarget += 1
        }

        return HistoryDependenceResult(
            probeTicks = probeTicks,
            differingActionTicks = differing,
            differingTargetTicks = differingTarget,
            overflowA = satA.overflowCount,
            overflowB = satB.overflowCount,
        )
    }

    public class HistoryDependenceResult(
        public val probeTicks: Long,
        public val differingActionTicks: Long,
        public val differingTargetTicks: Long,
        public val overflowA: Long,
        public val overflowB: Long,
    ) {
        public val actionDivergenceRate: Double
            get() = differingActionTicks.toDouble() / probeTicks

        public val targetDivergenceRate: Double
            get() = differingTargetTicks.toDouble() / probeTicks
    }

    /** Share of scored spontaneous actions that landed in each attribution class. */
    public fun attributionBreakdown(measures: RunMeasures): Map<AttributionClass, Long> =
        AttributionClass.entries.associateWith { measures.attributionCounts[it.ordinal] }
}
