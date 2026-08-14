package com.animusmachinae.dll17.research.aliveness.sim

import com.animusmachinae.dll17.research.aliveness.AttributionClass
import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.Cohorts
import com.animusmachinae.dll17.research.aliveness.Fx
import com.animusmachinae.dll17.research.aliveness.Habitat
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionEvent
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.OrganismAgent
import com.animusmachinae.dll17.research.aliveness.OutcomeModel
import com.animusmachinae.dll17.research.aliveness.SpikeAction
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.SpikeDecisionTrace
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import com.animusmachinae.dll17.research.aliveness.SpikeRuntime
import java.security.MessageDigest

/** One named A000 finding. `held` is the research expectation, not a pass/fail gate. */
public class Finding(
    public val id: String,
    public val question: String,
    public val readout: String,
    public val held: Boolean,
)

/**
 * The A000 accelerated qualification kernel.
 *
 * It answers whether the candidate mechanism architecture is technically viable
 * before human testing. Negative results are valid output: a finding that did
 * not hold is reported as such and its configuration is preserved, because the
 * point of the spike is to find out, not to demonstrate.
 *
 * The digest covers every reported readout, so a later change to the mechanisms
 * that silently alters behaviour cannot pass unnoticed.
 */
public object A000QualificationKernel {

    public const val FIXTURE_SET_ID: String = "A000-FIXTURES-V1"
    public const val FIXTURE_SET_VERSION: Int = 3

    /**
     * The digest reproduced by a clean run of this kernel. Recomputed and pasted
     * back after the fixtures were frozen; CI fails if it drifts.
     */
    public const val GOLDEN_EVIDENCE_DIGEST: String =
        "9462e43622c414db47c28a2e79452455bc0d6642396dd5ca8d65bae208b3114a"

    private const val DAY = SpikeContract.TICKS_PER_VIRTUAL_DAY

    @JvmStatic
    public fun main(args: Array<String>) {
        val result = run()
        println(result.render())
        if (args.any { it == "--traces" }) {
            println()
            println("== SAMPLE DECISION TRACES (audit evidence; never shown to a rater)")
            for (trace in result.sampleTraces) {
                println(trace.render())
            }
        }
    }

    public class KernelResult(
        public val findings: List<Finding>,
        public val sections: Map<String, String>,
        public val sampleTraces: List<SpikeDecisionTrace>,
    ) {
        public fun digest(): String {
            val canonical = buildString {
                append(FIXTURE_SET_ID).append('|').append(FIXTURE_SET_VERSION).append('\n')
                for (f in findings) {
                    append(f.id).append('|').append(f.readout).append('|').append(f.held).append('\n')
                }
            }
            return sha256Hex(canonical.toByteArray(Charsets.UTF_8))
        }

        public fun render(): String = buildString {
            append("A000_FIXTURE_SET=").append(FIXTURE_SET_ID).append('\n')
            append("A000_FIXTURE_VERSION=").append(FIXTURE_SET_VERSION).append('\n')
            for ((name, body) in sections) {
                append("\n== ").append(name).append('\n').append(body)
            }
            append("\n== FINDINGS\n")
            for (f in findings) {
                append(if (f.held) "  HELD     " else "  NOT HELD ")
                append(f.id).append("  ").append(f.question).append('\n')
                append("           ").append(f.readout).append('\n')
            }
            append("\nheld=").append(findings.count { it.held })
            append(" notHeld=").append(findings.count { !it.held })
            append(" total=").append(findings.size).append('\n')
            append("A000_EVIDENCE_DIGEST=").append(digest()).append('\n')
        }
    }

    public fun run(): KernelResult {
        val findings = ArrayList<Finding>()
        val sections = LinkedHashMap<String, String>()
        val traces = ArrayList<SpikeDecisionTrace>()

        sections["BOUNDEDNESS"] = boundedness(findings)
        sections["DETERMINISM"] = determinism(findings)
        sections["DIFFERENTIATION"] = differentiation(findings)
        sections["PREFERENCE"] = preference(findings)
        sections["AVOIDANCE_AND_EXTINCTION"] = avoidance(findings)
        sections["HABITUATION"] = habituation(findings)
        sections["HABIT_AND_EXPECTANCY"] = habit(findings)
        sections["RE_EXPLORATION"] = reExploration(findings)
        sections["EPISODIC_HISTORY"] = episodic(findings)
        sections["ANTI_CONVERGENCE"] = antiConvergence(findings)
        sections["POPULATION_DIVERSITY"] = diversity(findings)
        sections["ATTRIBUTION"] = attribution(findings, traces)
        sections["COHORT_PARITY"] = cohortParity(findings)

        return KernelResult(findings, sections, traces)
    }

    // ---------------------------------------------------------- boundedness

    private fun boundedness(out: MutableList<Finding>): String {
        val sb = StringBuilder()
        var allHeld = true
        val readouts = ArrayList<String>()
        for ((seed, condition) in listOf(
            101L to HabitatCondition.STATIC,
            202L to HabitatCondition.CONTROLLED_NOVELTY,
            303L to HabitatCondition.SHIFTING_CONTEXT,
        )) {
            val r = AcceleratedSimulator.run(
                RunConfig("AX-BOUNDEDNESS", Cohort.FULL, seed, LONG_RUN_DAYS, condition),
            )
            val agent = organism(Cohort.FULL, seed)
            val footprintAtStart = agent.state.stateFootprintSlots()
            val inBounds = boundsIntact(r)
            val held = r.overflowCount == 0L &&
                r.stateFootprintSlots == footprintAtStart &&
                inBounds
            allHeld = allHeld && held
            sb.append("  seed=").append(seed).append(" condition=").append(condition.name)
                .append(" days=").append(LONG_RUN_DAYS)
                .append(" overflow=").append(r.overflowCount)
                .append(" clamps=").append(r.clampCount)
                .append(" slotsStart=").append(footprintAtStart)
                .append(" slotsEnd=").append(r.stateFootprintSlots)
                .append(" boundsIntact=").append(inBounds).append('\n')
            readouts += "$seed:${r.overflowCount}:${r.stateFootprintSlots}:$inBounds"
        }
        out += Finding(
            id = "AX-BOUNDEDNESS-01",
            question = "Does state stay bounded and arithmetic stable over $LONG_RUN_DAYS virtual days?",
            readout = readouts.joinToString(","),
            held = allHeld,
        )
        return sb.toString()
    }

    private fun boundsIntact(r: RunResult): Boolean {
        // Every learning array is written through a clamp, so this checks the
        // clamps rather than trusting them.
        val s = r.series
        fun ok(v: Long, lo: Long, hi: Long) = v in lo..hi
        val one = com.animusmachinae.dll17.core.math.FixedPoint.ONE
        return s.fearAversive.all { ok(it, 0L, one) } &&
            s.preferenceBall.all { ok(it, -one, one) } &&
            s.preferenceTrough.all { ok(it, -one, one) } &&
            s.habitEatTrough.all { ok(it, 0L, one) } &&
            s.habituationMirror.all { ok(it, 0L, one) } &&
            s.relationshipAlpha.all { ok(it, -one, one) }
    }

    // ---------------------------------------------------------- determinism

    private fun determinism(out: MutableList<Finding>): String {
        fun config(id: String) = RunConfig(
            id, Cohort.FULL, 4242L, 30, HabitatCondition.CONTROLLED_NOVELTY,
            attributionSampleEvery = 20,
        )
        val a = AcceleratedSimulator.run(config("AX-DET-A"), recordActions = true)
        val b = AcceleratedSimulator.run(config("AX-DET-B"), recordActions = true)
        val sameSignature = a.finalStateSignature == b.finalStateSignature
        val sameActions = a.actionsByTick!!.contentEquals(b.actionsByTick!!)
        val sameAttribution =
            a.measures.attributionCounts.contentEquals(b.measures.attributionCounts)
        val held = sameSignature && sameActions && sameAttribution
        out += Finding(
            "AX-DETERMINISM-01",
            "Do two runs of the identical configuration agree exactly?",
            "signature=$sameSignature actions=$sameActions attribution=$sameAttribution",
            held,
        )
        return "  stateSignature=${java.lang.Long.toHexString(a.finalStateSignature)}" +
            " ticksCompared=${a.actionsByTick.size}" +
            " scoredActions=${a.measures.spontaneousScored}\n"
    }

    // ------------------------------------------------------ differentiation

    private fun differentiation(out: MutableList<Finding>): String {
        val seeds = longArrayOf(11L, 23L, 37L, 51L, 67L, 83L)
        val results = seeds.map {
            AcceleratedSimulator.run(
                RunConfig("AX-DIFF", Cohort.FULL, it, MEDIUM_RUN_DAYS, HabitatCondition.STATIC),
            )
        }
        val sb = StringBuilder()
        var minTv = Double.MAX_VALUE
        var maxTv = 0.0
        var sum = 0.0
        var pairs = 0
        for (i in results.indices) {
            for (j in i + 1 until results.size) {
                val tv = RunMeasures.totalVariation(
                    results[i].measures.actionDistribution(),
                    results[j].measures.actionDistribution(),
                )
                minTv = minOf(minTv, tv)
                maxTv = maxOf(maxTv, tv)
                sum += tv
                pairs += 1
            }
        }
        val distinctSignatures = results.map { it.finalStateSignature }.distinct().size
        val mean = sum / pairs
        sb.append("  seeds=").append(seeds.joinToString(","))
            .append(" days=").append(MEDIUM_RUN_DAYS).append('\n')
        sb.append("  pairwiseTV min=").append(RunMeasures.d6(minTv))
            .append(" mean=").append(RunMeasures.d6(mean))
            .append(" max=").append(RunMeasures.d6(maxTv)).append('\n')
        sb.append("  distinctFinalStateSignatures=").append(distinctSignatures)
            .append('/').append(results.size).append('\n')
        out += Finding(
            "AX-DIFFERENTIATION-01",
            "Do different seeds produce measurably different organisms on average?",
            "meanTV=${RunMeasures.d6(mean)} distinctStateSignatures=$distinctSignatures/${results.size}",
            mean >= MIN_PAIRWISE_TV && distinctSignatures == results.size,
        )
        // Reported separately and deliberately kept. The average pair is well
        // above the floor; the *closest* pair is not. Individuality here is a
        // property of the population, not a guarantee about any two organisms.
        out += Finding(
            "AX-DIFFERENTIATION-02",
            "Is every pair of organisms distinguishable, not only the average pair?",
            "minTV=${RunMeasures.d6(minTv)} maxTV=${RunMeasures.d6(maxTv)} " +
                "floor=${RunMeasures.d6(MIN_PAIRWISE_TV)}",
            minTv >= MIN_PAIRWISE_TV,
        )
        return sb.toString()
    }

    // ------------------------------------------------------------ preference

    private fun preference(out: MutableList<Finding>): String {
        // Forced exposure. An observational run answers a different question:
        // an organism that never engaged an object has no preference about it,
        // and comparing two never-engaged objects measures nothing. Presenting
        // each play object in rotation makes the comparison a controlled one.
        val playObjects = listOf(
            HabitatObject.PLAY_BALL, HabitatObject.PLAY_CUBE,
            HabitatObject.PLAY_CHIME, HabitatObject.PLAY_MIRROR,
        )
        val exposure: (Long) -> List<InteractionEvent> = { tick ->
            if (tick % 30L == 0L) {
                val obj = playObjects[((tick / 30L) % playObjects.size).toInt()]
                listOf(InteractionEvent(tick, InteractionKind.PRESENT_OBJECT, obj, null))
            } else {
                emptyList()
            }
        }
        // Pooled over a seed matrix. A single organism may simply never engage a
        // given object, and one individual's omission is not evidence about the
        // learning law.
        val seeds = longArrayOf(777L, 881L, 993L, 1_117L)
        var ballTotal = 0L
        var mirrorTotal = 0L
        var ballEngaged = 0L
        var mirrorEngaged = 0L
        var seedsEngagingBoth = 0
        for (seed in seeds) {
            val (fx, _) = Fx.counting()
            val habitat = Habitat(seed, HabitatCondition.STATIC)
            val agent = OrganismAgent(Cohort.FULL, seed, fx)
            val runtime = SpikeRuntime("AX-PREF", agent, habitat, OutcomeModel(), fx)
            val engaged = HashMap<HabitatObject, Long>()
            for (tick in 0 until MEDIUM_RUN_DAYS.toLong() * DAY) {
                val rec = runtime.step(tick, exposure(tick))
                val t = rec.choice.target
                if (t != null && (rec.choice.action == SpikeAction.PLAY ||
                        rec.choice.action == SpikeAction.EXPLORE)
                ) {
                    engaged[t] = (engaged[t] ?: 0L) + 1L
                }
            }
            val b = engaged[HabitatObject.PLAY_BALL] ?: 0L
            val m = engaged[HabitatObject.PLAY_MIRROR] ?: 0L
            ballEngaged += b
            mirrorEngaged += m
            if (b > 0L && m > 0L) seedsEngagingBoth += 1
            ballTotal += agent.state.preference[HabitatObject.PLAY_BALL.ordinal0]
            mirrorTotal += agent.state.preference[HabitatObject.PLAY_MIRROR.ordinal0]
        }
        val ball = ballTotal / seeds.size
        val mirror = mirrorTotal / seeds.size
        out += Finding(
            "AX-PREFERENCE-01",
            "Under matched exposure, does preference track outcome payoff?",
            "meanBall=${RunMeasures.fx(ball)} meanMirror=${RunMeasures.fx(mirror)} " +
                "seedsEngagingBoth=$seedsEngagingBoth/${seeds.size} " +
                "engagedBall=$ballEngaged engagedMirror=$mirrorEngaged",
            // The guard is at the matrix level, matching the pooled design: a
            // single organism specializing hard enough to ignore one object
            // says nothing about the learning law, but a matrix in which
            // neither object was ever engaged would say nothing either.
            ballEngaged > 0L && mirrorEngaged > 0L && ball > mirror,
        )
        val r = AcceleratedSimulator.run(
            RunConfig("AX-PREF-OBS", Cohort.FULL, 777L, MEDIUM_RUN_DAYS, HabitatCondition.STATIC),
        )

        // Contingency reversal: the reliable source becomes unreliable halfway.
        val reversalTick = (MEDIUM_RUN_DAYS / 2).toLong() * DAY
        val rev = AcceleratedSimulator.run(
            RunConfig(
                "AX-PREF-REVERSAL", Cohort.FULL, 777L, MEDIUM_RUN_DAYS, HabitatCondition.STATIC,
                OutcomeModel(
                    contingencyReversalTick = reversalTick,
                    strictFoodContingency = true,
                ),
            ),
        )
        val mid = MEDIUM_RUN_DAYS / 2 - 1
        // Which source this individual actually adopted before the reversal is
        // its own choice, not the fixture's. The claim under test is that
        // whichever it adopted loses value when the contingency flips.
        val troughFirst = rev.series.preferenceTrough[mid] >= rev.series.preferenceCache[mid]
        val adoptedMid = if (troughFirst) rev.series.preferenceTrough[mid] else rev.series.preferenceCache[mid]
        val adoptedEnd = if (troughFirst) rev.series.preferenceTrough.last() else rev.series.preferenceCache.last()
        val otherMid = if (troughFirst) rev.series.preferenceCache[mid] else rev.series.preferenceTrough[mid]
        val otherEnd = if (troughFirst) rev.series.preferenceCache.last() else rev.series.preferenceTrough.last()
        out += Finding(
            "AX-PREFERENCE-REVERSAL-01",
            "Does the adopted preference weaken once its contingency reverses?",
            "adopted=${if (troughFirst) "FOOD_TROUGH" else "FOOD_CACHE"} " +
                "adoptedMid=${RunMeasures.fx(adoptedMid)} adoptedEnd=${RunMeasures.fx(adoptedEnd)}",
            adoptedEnd < adoptedMid,
        )
        // Recorded separately because the two halves of "relearning" turned out
        // to be different claims. Devaluation works; *switching* to the
        // alternative does not follow from it, because nothing drives the
        // organism to re-sample a source it had already written off.
        out += Finding(
            "AX-PREFERENCE-REVERSAL-02",
            "Does the organism switch to the alternative once the adopted one is devalued?",
            "otherMid=${RunMeasures.fx(otherMid)} otherEnd=${RunMeasures.fx(otherEnd)}",
            otherEnd > otherMid,
        )
        return "  static: ball=${RunMeasures.fx(ball)} cube/mirror baseline=${RunMeasures.fx(mirror)}\n" +
            "  reversal at day ${MEDIUM_RUN_DAYS / 2}\n"
    }

    // ------------------------------------------------- avoidance / extinction

    private fun avoidance(out: MutableList<Finding>): String {
        // A conditioning protocol, not a hope: the aversive object is presented
        // on a fixed schedule during acquisition so the organism actually meets
        // it, then made safe and left alone so extinction can be observed.
        val acquisitionDays = 20
        val safeFrom = acquisitionDays.toLong() * DAY
        val totalDays = 60
        val (fx, sat) = Fx.counting()
        val habitat = Habitat(909L, HabitatCondition.STATIC)
        val agent = OrganismAgent(Cohort.FULL, 909L, fx)
        val runtime = SpikeRuntime(
            "AX-AVOIDANCE", agent, habitat,
            OutcomeModel(aversiveSafeFromTick = safeFrom), fx,
        )
        val buzzer = HabitatObject.AVERSIVE_BUZZER.ordinal0
        var peak = 0L
        var avoidanceTicks = 0L
        var punishedEvents = 0L
        val daily = ArrayList<Long>()

        for (tick in 0 until totalDays.toLong() * DAY) {
            val present = tick < safeFrom && tick % 180L == 0L
            val events = if (present) {
                listOf(
                    InteractionEvent(
                        tick, InteractionKind.PRESENT_OBJECT,
                        HabitatObject.AVERSIVE_BUZZER, null,
                    ),
                )
            } else {
                emptyList()
            }
            val record = runtime.step(tick, events)
            if (record.outcome.strongNegative) punishedEvents += 1
            val fear = agent.state.fear[buzzer]
            if (fear > peak) peak = fear
            if (fear >= SpikeContract.FEAR_AVOIDANCE_THRESHOLD) avoidanceTicks += 1
            if ((tick + 1) % DAY == 0L) daily += fear
        }

        val acquired = peak >= SpikeContract.FEAR_AVOIDANCE_THRESHOLD
        val atSafe = daily[acquisitionDays - 1]
        val atEnd = daily.last()
        val extinguished = atEnd < atSafe

        out += Finding(
            "AX-AVOIDANCE-01",
            "Does repeated punishment produce a conditioned avoidance?",
            "peakFear=${RunMeasures.fx(peak)} punishedEvents=$punishedEvents " +
                "avoidanceTicks=$avoidanceTicks",
            acquired && punishedEvents > 0L && avoidanceTicks > 0L,
        )
        out += Finding(
            "AX-EXTINCTION-01",
            "Does the fear decline once the object becomes safe?",
            "fearAtSafe=${RunMeasures.fx(atSafe)} fearAtEnd=${RunMeasures.fx(atEnd)} " +
                "residualFloor=${RunMeasures.fx(agent.state.fearPeak[buzzer])}",
            extinguished,
        )
        return buildString {
            append("  acquisitionDays=").append(acquisitionDays)
            append(" totalDays=").append(totalDays)
            append(" overflow=").append(sat.overflowCount).append('\n')
            append("  fear by day: ")
            append(daily.filterIndexed { i, _ -> i % 5 == 0 }.joinToString(" ") { RunMeasures.fx(it) })
            append('\n')
        }
    }

    // ----------------------------------------------------------- habituation

    private fun habituation(out: MutableList<Finding>): String {
        val (fx, _) = Fx.counting()
        val habitat = Habitat(555L, HabitatCondition.STATIC)
        val agent = OrganismAgent(Cohort.FULL, 555L, fx)
        val runtime = SpikeRuntime("AX-HABIT", agent, habitat, OutcomeModel(), fx)
        val mirror = HabitatObject.PLAY_MIRROR.ordinal0

        // The habituation *mechanism* is exercised directly here, for two
        // reasons. Behaviourally the organism stops visiting a habituated object,
        // so an observational fixture measures the decay rather than the
        // build-up; and presenting the object to force exposure fires
        // dishabituation, which is the mechanism this fixture is not testing.
        // Whether habituation changes behaviour is answered by
        // AX-ANTICONVERGENCE-02, not here.
        var peak = 0L
        val exposureDays = 20
        // A day of ordinary life first, so the exposure lands on a realistic
        // state rather than on genesis values.
        for (tick in 0 until DAY.toLong()) runtime.step(tick)
        var exposureTick = DAY.toLong()
        repeat(200) {
            com.animusmachinae.dll17.research.aliveness.MechanismUpdates.applyOutcome(
                agent.state, SpikeAction.EXPLORE, HabitatObject.PLAY_MIRROR,
                com.animusmachinae.dll17.research.aliveness.Outcome(
                    true, OutcomeModel.NEUTRAL, false, true, null,
                ),
                habitat, exposureTick, fx,
            )
            if (agent.state.habituation[mirror] > peak) peak = agent.state.habituation[mirror]
            exposureTick += 1
        }
        val atExposureEnd = agent.state.habituation[mirror]

        // Non-exposure: the recovery recurrence runs without the object being
        // inspected. Advancing the mechanisms directly isolates recovery from
        // any behavioural confound.
        var recoveryTick = exposureTick
        val recoveryTicks = 5L * DAY
        repeat(recoveryTicks.toInt()) {
            com.animusmachinae.dll17.research.aliveness.MechanismUpdates.advance(
                agent.state, habitat, recoveryTick, fx,
            )
            recoveryTick += 1
        }
        val afterRecovery = agent.state.habituation[mirror]
        val recovered = afterRecovery < atExposureEnd && afterRecovery >= 0L

        out += Finding(
            "AX-HABITUATION-01",
            "Does repeated exposure build a trace that recovers during non-exposure?",
            "peak=${RunMeasures.fx(peak)} atExposureEnd=${RunMeasures.fx(atExposureEnd)} " +
                "after5Days=${RunMeasures.fx(afterRecovery)}",
            peak > 0L && recovered,
        )

        // Dishabituation: a causal environmental change releases part of the trace.
        val before = agent.state.habituation[mirror]
        habitat.lastChangeTick[mirror] = recoveryTick
        com.animusmachinae.dll17.research.aliveness.MechanismUpdates.advance(
            agent.state, habitat, recoveryTick, fx,
        )
        val after = agent.state.habituation[mirror]
        out += Finding(
            "AX-DISHABITUATION-01",
            "Does a causal environmental change release habituation?",
            "before=${RunMeasures.fx(before)} after=${RunMeasures.fx(after)}",
            after < before,
        )

        // Sensitization: a strong negative event raises responsiveness.
        val sensitizationBefore = agent.state.sensitization[HabitatObject.AVERSIVE_BUZZER.ordinal0]
        com.animusmachinae.dll17.research.aliveness.MechanismUpdates.applyOutcome(
            agent.state, SpikeAction.EXPLORE, HabitatObject.AVERSIVE_BUZZER,
            com.animusmachinae.dll17.research.aliveness.Outcome(
                false, OutcomeModel.STRONG_NEGATIVE, true, false, null,
            ),
            habitat, recoveryTick, fx,
        )
        val sensitizationAfter = agent.state.sensitization[HabitatObject.AVERSIVE_BUZZER.ordinal0]
        out += Finding(
            "AX-SENSITIZATION-01",
            "Does a strong negative event raise responsiveness?",
            "before=${RunMeasures.fx(sensitizationBefore)} after=${RunMeasures.fx(sensitizationAfter)}",
            sensitizationAfter > sensitizationBefore,
        )
        return "  exposureDays=$exposureDays recoveryDays=5\n"
    }

    // ------------------------------------------------------ habit/expectancy

    private fun habit(out: MutableList<Finding>): String {
        val days = 60
        val reversal = 30L * DAY
        val r = AcceleratedSimulator.run(
            RunConfig(
                "AX-HABIT-FORM", Cohort.FULL, 313L, days, HabitatCondition.STATIC,
                OutcomeModel(contingencyReversalTick = reversal, strictFoodContingency = true),
            ),
        )
        // The claim is that habit follows the contingency, stated without
        // assuming which source this individual adopted. FOOD_TROUGH is reliable
        // before the reversal and FOOD_CACHE after it, so habit for the cache
        // must rise across the reversal and habit for the trough must not.
        val troughMid = r.series.habitEatTrough[29]
        val troughEnd = r.series.habitEatTrough.last()
        val cacheMid = r.series.habitEatCache[29]
        val cacheEnd = r.series.habitEatCache.last()
        val formed = maxOf(r.series.habitEatTrough.max(), r.series.habitEatCache.max()) > 0L
        out += Finding(
            "AX-HABIT-01",
            "Does habit strength follow the contingency rather than accumulate?",
            "troughMid=${RunMeasures.fx(troughMid)} troughEnd=${RunMeasures.fx(troughEnd)} " +
                "cacheMid=${RunMeasures.fx(cacheMid)} cacheEnd=${RunMeasures.fx(cacheEnd)} " +
                "formed=$formed",
            formed && cacheEnd > cacheMid && troughEnd <= troughMid,
        )
        return "  days=$days reversalDay=30\n"
    }

    // ------------------------------------------------------ re-exploration

    /**
     * A controlled re-exploration protocol, added under D009.
     *
     * The observational reversal fixture cannot answer this question, because
     * which source an individual adopted before the reversal is that
     * individual's own choice: an organism that happened to adopt the source
     * which later *improves* has nothing to switch away from. Here the
     * unreliable source never succeeds at all, so the organism must adopt the
     * trough, must reject the cache, and the reversal then asks the actual
     * question — can a rejected option be reconsidered when the evidence
     * changes?
     */
    private fun reExploration(out: MutableList<Finding>): String {
        val days = 60
        val reversal = 30L * DAY
        val (fx, _) = Fx.counting()
        val habitat = Habitat(4545L, HabitatCondition.STATIC)
        val agent = OrganismAgent(Cohort.FULL, 4545L, fx)
        val runtime = SpikeRuntime(
            "AX-REEXPLORATION", agent, habitat,
            OutcomeModel(contingencyReversalTick = reversal, strictFoodContingency = true), fx,
        )
        var troughBefore = 0L
        var cacheBefore = 0L
        var troughAfter = 0L
        var cacheAfter = 0L
        var firstCacheReturnTick = -1L
        val lateWindowStart = 50L * DAY

        for (tick in 0 until days.toLong() * DAY) {
            val record = runtime.step(tick)
            if (record.choice.action != SpikeAction.EAT) continue
            val target = record.choice.target ?: continue
            val late = tick >= lateWindowStart
            when (target) {
                HabitatObject.FOOD_TROUGH -> if (tick < reversal) troughBefore++ else if (late) troughAfter++
                HabitatObject.FOOD_CACHE -> {
                    if (tick < reversal) {
                        cacheBefore++
                    } else {
                        if (firstCacheReturnTick < 0L) firstCacheReturnTick = tick
                        if (late) cacheAfter++
                    }
                }
                else -> Unit
            }
        }

        val beforeDays = 30.0
        val lateDays = 10.0
        val cacheBeforeRate = cacheBefore / beforeDays
        val cacheAfterRate = cacheAfter / lateDays
        val troughBeforeRate = troughBefore / beforeDays
        val troughAfterRate = troughAfter / lateDays
        val delayTicks = if (firstCacheReturnTick < 0L) -1L else firstCacheReturnTick - reversal

        out += Finding(
            "AX-REEXPLORATION-01",
            "Can a previously rejected option be re-sampled after the evidence changes?",
            "cacheEatsPerDayBefore=${RunMeasures.d6(cacheBeforeRate)} " +
                "cacheEatsPerDayAfter=${RunMeasures.d6(cacheAfterRate)} " +
                "firstReturnTicksAfterReversal=$delayTicks",
            cacheAfterRate > cacheBeforeRate && firstCacheReturnTick >= 0L,
        )
        out += Finding(
            "AX-REEXPLORATION-02",
            "Is the switch a real reallocation rather than indiscriminate sampling?",
            "troughEatsPerDayBefore=${RunMeasures.d6(troughBeforeRate)} " +
                "troughEatsPerDayAfter=${RunMeasures.d6(troughAfterRate)} " +
                "preferenceTrough=${RunMeasures.fx(agent.state.preference[HabitatObject.FOOD_TROUGH.ordinal0])} " +
                "preferenceCache=${RunMeasures.fx(agent.state.preference[HabitatObject.FOOD_CACHE.ordinal0])}",
            troughAfterRate < troughBeforeRate &&
                agent.state.preference[HabitatObject.FOOD_CACHE.ordinal0] >
                agent.state.preference[HabitatObject.FOOD_TROUGH.ordinal0],
        )
        return "  seed=4545 days=$days reversalDay=30 strictFoodContingency=true\n"
    }

    // ----------------------------------------------------------- episodic

    private fun episodic(out: MutableList<Finding>): String {
        // Pooled over a seed matrix. The D008 comparison rested on one pair of
        // organisms, which is not enough to tell a mechanism's contribution from
        // a coin flip in either direction.
        val seeds = longArrayOf(1_234L, 2_345L, 3_456L, 4_567L, 5_678L)
        var fullSum = 0.0
        var ablatedSum = 0.0
        var fullTargetSum = 0.0
        var seedsWhereEpisodicHelps = 0
        for (seed in seeds) {
            val full = AcceleratedSimulator.historyDependence(Cohort.FULL, seed, 20, 5)
            // FULL no longer carries the mechanism, so the comparison cohort
            // adds it back. The question is whether putting it in helps.
            val withEpisodic = AcceleratedSimulator.historyDependence(
                Cohort.FULL_PLUS_EPISODIC_HISTORY, seed, 20, 5,
            )
            fullSum += full.actionDivergenceRate
            fullTargetSum += full.targetDivergenceRate
            ablatedSum += withEpisodic.actionDivergenceRate
            if (withEpisodic.actionDivergenceRate > full.actionDivergenceRate) {
                seedsWhereEpisodicHelps += 1
            }
        }
        val n = seeds.size
        val fullMean = fullSum / n
        val ablatedMean = ablatedSum / n
        out += Finding(
            "AX-EPISODIC-01",
            "Do divergent histories change later behaviour under matched present stimuli?",
            "meanActionDivergence=${RunMeasures.d6(fullMean)} " +
                "meanTargetDivergence=${RunMeasures.d6(fullTargetSum / n)} seeds=$n",
            fullMean > MIN_HISTORY_DIVERGENCE,
        )
        // Reported as the reason the mechanism is out of FULL, not as a target
        // to be met. It holds when adding episodic recall back makes no positive
        // difference, which is the finding D009 acted on.
        out += Finding(
            "AX-EPISODIC-02",
            "Was removing episodic recall from FULL the right call?",
            "meanFULL=${RunMeasures.d6(fullMean)} meanFULLplusEpisodic=${RunMeasures.d6(ablatedMean)} " +
                "seedsWhereAddingItHelps=$seedsWhereEpisodicHelps/$n disposition=REMOVED",
            !(ablatedMean > fullMean && seedsWhereEpisodicHelps * 2 > n),
        )
        return "  conditioningDays=20 probeDays=5 seeds=${seeds.joinToString(",")}\n"
    }

    // ------------------------------------------------------ anti-convergence

    private fun antiConvergence(out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val results = LinkedHashMap<Cohort, RunResult>()
        for (cohort in listOf(Cohort.FULL, Cohort.FULL_MINUS_CURIOSITY_ANTICONVERGENCE)) {
            val r = AcceleratedSimulator.run(
                RunConfig(
                    "AX-ANTICONV", cohort, 606L, STATIC_HABITAT_DAYS, HabitatCondition.STATIC,
                    windowDays = 20,
                ),
            )
            results[cohort] = r
            sb.append("  ").append(cohort.cohortId)
                .append(" entropy=").append(RunMeasures.d6(r.measures.windowActionEntropyBits()))
                .append(" maxOccupancy=").append(RunMeasures.d6(r.measures.maxWindowOccupancy()))
                .append(" inactivity=").append(RunMeasures.d6(r.measures.windowInactivityShare()))
                .append(" wakingOccupancy=").append(RunMeasures.d6(r.measures.maxWakingOccupancy()))
                .append(" distinctObjects/day=")
                .append(RunMeasures.d6(r.measures.distinctObjectsInspectedPerDay()))
                .append(" revisits/day=").append(RunMeasures.d6(r.measures.revisitationsPerDay()))
                .append(" cycleRegularity=").append(RunMeasures.d6(r.measures.cycleRegularity()))
                .append('\n')
        }
        val full = results[Cohort.FULL]!!
        val m = full.measures
        val entropyOk = m.windowActionEntropyBits() >=
            SpikeContract.MIN_ACTION_TYPE_ENTROPY_BITS.toDouble() / 1_000_000.0
        val distinctOk = m.distinctObjectsInspectedPerDay() >=
            SpikeContract.MIN_DISTINCT_OBJECTS_INSPECTED_PER_DAY.toDouble() / 1_000_000.0
        val occupancyOk = m.maxWindowOccupancy() <=
            SpikeContract.MAX_SINGLE_ACTION_OCCUPANCY.toDouble() / 1_000_000.0
        val revisitOk = m.revisitationsPerDay() >=
            SpikeContract.MIN_REVISITATION_RATE_PER_DAY.toDouble() / 1_000_000.0
        val regularityOk = m.cycleRegularity() <=
            SpikeContract.MAX_CYCLE_REGULARITY.toDouble() / 1_000_000.0

        out += Finding(
            "AX-ANTICONVERGENCE-01",
            "Does FULL avoid behavioural death in a static habitat over $STATIC_HABITAT_DAYS days?",
            "entropy=${RunMeasures.d6(m.windowActionEntropyBits())}/$entropyOk " +
                "distinct=${RunMeasures.d6(m.distinctObjectsInspectedPerDay())}/$distinctOk " +
                "occupancy=${RunMeasures.d6(m.maxWindowOccupancy())}/$occupancyOk " +
                "wakingOccupancy=${RunMeasures.d6(m.maxWakingOccupancy())} " +
                "revisits=${RunMeasures.d6(m.revisitationsPerDay())}/$revisitOk " +
                "regularity=${RunMeasures.d6(m.cycleRegularity())}/$regularityOk",
            entropyOk && distinctOk && occupancyOk && revisitOk && regularityOk,
        )

        val ablated = results[Cohort.FULL_MINUS_CURIOSITY_ANTICONVERGENCE]!!.measures
        out += Finding(
            "AX-ANTICONVERGENCE-02",
            "Is the anti-convergence mechanism load-bearing on objective measures?",
            "fullDistinct=${RunMeasures.d6(m.distinctObjectsInspectedPerDay())} " +
                "ablatedDistinct=${RunMeasures.d6(ablated.distinctObjectsInspectedPerDay())} " +
                "fullRevisits=${RunMeasures.d6(m.revisitationsPerDay())} " +
                "ablatedRevisits=${RunMeasures.d6(ablated.revisitationsPerDay())}",
            ablated.distinctObjectsInspectedPerDay() < m.distinctObjectsInspectedPerDay(),
        )

        val saturated = full.series.habituationMirror.last() >=
            SpikeContract.HABITUATION_MAX && m.distinctObjectsInspectedPerDay() < 1.0
        out += Finding(
            "AX-NOVELTY-SATURATION-01",
            "Does the organism escape permanent novelty saturation?",
            "habituationAtEnd=${RunMeasures.fx(full.series.habituationMirror.last())} " +
                "distinctObjects/day=${RunMeasures.d6(m.distinctObjectsInspectedPerDay())}",
            !saturated,
        )
        return sb.toString()
    }

    // ----------------------------------------------------- population spread

    private fun diversity(out: MutableList<Finding>): String {
        val seeds = longArrayOf(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L)
        val windows = seeds.map {
            AcceleratedSimulator.run(
                RunConfig(
                    "AX-DIVERSITY", Cohort.FULL, it, MEDIUM_RUN_DAYS, HabitatCondition.STATIC,
                    windowDays = 20,
                ),
            ).measures.windowActionDistribution()
        }
        var sum = 0.0
        var min = Double.MAX_VALUE
        var pairs = 0
        for (i in windows.indices) {
            for (j in i + 1 until windows.size) {
                val tv = RunMeasures.totalVariation(windows[i], windows[j])
                sum += tv
                min = minOf(min, tv)
                pairs += 1
            }
        }
        val mean = sum / pairs
        out += Finding(
            "AX-CONVERGENCE-01",
            "Does the population avoid collapsing onto one long-run policy?",
            "meanFinalWindowTV=${RunMeasures.d6(mean)} minTV=${RunMeasures.d6(min)} pairs=$pairs",
            mean >= MIN_POPULATION_TV,
        )
        return "  seeds=${seeds.joinToString(",")} windowDays=20\n"
    }

    // ------------------------------------------------------------ attribution

    private fun attribution(
        out: MutableList<Finding>,
        traces: MutableList<SpikeDecisionTrace>,
    ): String {
        val r = AcceleratedSimulator.run(
            RunConfig(
                "AX-ATTRIBUTION", Cohort.FULL, 8080L, MEDIUM_RUN_DAYS, HabitatCondition.STATIC,
                attributionSampleEvery = 5,
                keepTraces = 3,
            ),
        )
        traces += r.traces
        val m = r.measures
        val substantive = m.substantiveSpontaneityRate()
        val hollow = m.oscillatorTieBreakOnlyRate()
        val required = SpikeContract.REQUIRED_SUBSTANTIVE_SPONTANEITY_RATE.toDouble() / 1_000_000.0
        val ceiling = SpikeContract.MAX_OSCILLATOR_TIEBREAK_ONLY_RATE.toDouble() / 1_000_000.0

        val sb = StringBuilder()
        sb.append("  scoredSpontaneousActions=").append(m.spontaneousScored)
            .append(" tieBreakDetermined=").append(m.tieBreakDetermined).append('\n')
        for (c in AttributionClass.entries) {
            val n = m.attributionCounts[c.ordinal]
            if (n > 0L) sb.append("    ").append(c.name).append('=').append(n).append('\n')
        }
        out += Finding(
            "AX-ATTRIBUTION-01",
            "Is spontaneous behaviour substantively caused rather than oscillator or tie-break noise?",
            "substantiveRate=${RunMeasures.d6(substantive)}(>=${RunMeasures.d6(required)}) " +
                "oscillatorTieBreakOnlyRate=${RunMeasures.d6(hollow)}(<=${RunMeasures.d6(ceiling)}) " +
                "scored=${m.spontaneousScored}",
            m.spontaneousScored > 0L && substantive >= required && hollow <= ceiling,
        )
        out += Finding(
            "AX-TRACE-01",
            "Does every scored spontaneous action carry a causal decision trace?",
            "tracesRetained=${r.traces.size} " +
                "allHaveAttribution=${r.traces.all { it.attribution != null }} " +
                "allSpontaneous=${r.traces.all { it.spontaneous }}",
            r.traces.isNotEmpty() && r.traces.all { it.attribution != null && it.spontaneous },
        )
        return sb.toString()
    }

    // --------------------------------------------------------- cohort parity

    private fun cohortParity(out: MutableList<Finding>): String {
        val sb = StringBuilder()
        var parity = true
        val postures = HashSet<String>()
        val expressions = HashSet<String>()
        val micro = HashSet<String>()
        for (cohort in Cohort.entries) {
            val (fx, _) = Fx.counting()
            val habitat = Habitat(4321L, HabitatCondition.CONTROLLED_NOVELTY)
            val agent = Cohorts.create(cohort, 4321L, fx)
            val runtime = SpikeRuntime("AX-PARITY", agent, habitat, OutcomeModel(), fx)
            val signatures = HashSet<String>()
            val cohortPostures = HashSet<String>()
            for (tick in 0 until PARITY_DAYS.toLong() * DAY) {
                val events = if (tick % 400L == 0L) {
                    listOf(
                        InteractionEvent(
                            tick, InteractionKind.TOUCH, null, HabitatObject.PERSON_ALPHA,
                        ),
                    )
                } else {
                    emptyList()
                }
                val record = runtime.step(tick, events)
                signatures += record.frame.signature()
                cohortPostures += record.frame.posture.name
                postures += record.frame.posture.name
                expressions += record.frame.expression.name
                micro += record.frame.microMovement
            }
            sb.append("  ").append(cohort.cohortId)
                .append(" distinctFrames=").append(signatures.size)
                .append(" postures=").append(cohortPostures.size)
                .append(" mechanisms=").append(cohort.mechanisms.size)
                .append('\n')
            if (signatures.isEmpty()) parity = false
        }
        val vocabularyRespected =
            postures.all { p -> SpikeExpressionContract.Posture.entries.any { it.name == p } } &&
                expressions.all { e ->
                    SpikeExpressionContract.Expression.entries.any { it.name == e }
                } &&
                micro.all { it in SpikeExpressionContract.MICRO_MOVEMENTS }
        out += Finding(
            "AX-COHORT-PARITY-01",
            "Does every cohort render through the same frozen presentation contract?",
            "cohorts=${Cohort.entries.size} posturesUsed=${postures.size} " +
                "expressionsUsed=${expressions.size} microUsed=${micro.size} " +
                "vocabularyRespected=$vocabularyRespected",
            parity && vocabularyRespected,
        )
        return sb.toString()
    }

    // ----------------------------------------------------------------- utils

    private fun organism(cohort: Cohort, seed: Long): OrganismAgent {
        val (fx, _) = Fx.counting()
        return OrganismAgent(cohort, seed, fx)
    }

    public fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) sb.append(String.format(java.util.Locale.ROOT, "%02x", b))
        return sb.toString()
    }

    private const val LONG_RUN_DAYS = 365
    private const val MEDIUM_RUN_DAYS = 60
    private const val STATIC_HABITAT_DAYS = 180
    private const val PARITY_DAYS = 3

    private const val MIN_PAIRWISE_TV = 0.05
    private const val MIN_HISTORY_DIVERGENCE = 0.05
    private const val MIN_POPULATION_TV = 0.05
}
