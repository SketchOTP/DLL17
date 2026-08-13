package com.animusmachinae.dll17.research.aliveness

/** Everything one tick produced. */
public class StepRecord(
    public val tick: Long,
    public val choice: AgentChoice,
    public val outcome: Outcome,
    public val frame: SpikeExpressionContract.ExpressionFrame,
    public val trace: SpikeDecisionTrace?,
    public val attribution: AttributionResult?,
    public val spontaneous: Boolean,
)

/**
 * The shared tick loop.
 *
 * Every cohort runs through this identical runtime: same habitat, same outcome
 * model, same presentation contract, same timing. Only `agent.decide` differs.
 */
public class SpikeRuntime(
    public val runId: String,
    public val agent: SpikeAgent,
    public val habitat: Habitat,
    public val outcomes: OutcomeModel,
    public val fx: Fx,
    /** Emit a trace for every decision. Off for long accelerated runs. */
    public val traceEveryDecision: Boolean = false,
    /** Score exact coalition attribution for one in every N spontaneous actions. */
    public val attributionSampleEvery: Int = 0,
) {
    public var spontaneousSeen: Long = 0L
        private set
    public var spontaneousScored: Long = 0L
        private set

    public fun step(tick: Long, interactions: List<InteractionEvent> = emptyList()): StepRecord {
        habitat.advance(tick)
        agent.advance(habitat, tick)
        for (event in interactions) agent.receive(event, tick)

        val choice = agent.decide(habitat, tick)
        val outcome = outcomes.resolve(choice.action, choice.target, tick, organismStateOrNull())
        agent.applyOutcome(choice, outcome, habitat, tick)

        val frame = SpikeExpressionContract.frameFor(agent.presentation(choice, tick))

        val decision = choice.decision
        val spontaneous = decision?.spontaneous == true
        if (spontaneous) spontaneousSeen += 1

        var attribution: AttributionResult? = null
        if (spontaneous && decision != null && attributionSampleEvery > 0 &&
            spontaneousSeen % attributionSampleEvery == 0L
        ) {
            attribution = CoalitionAttribution.attribute(
                observed = decision.winner,
                candidates = decision.eligible,
                tieBreakOutcomeDetermining = decision.tieBreakDetermined,
                fx = fx,
            )
            spontaneousScored += 1
        }

        val trace = if (decision != null && (traceEveryDecision || attribution != null)) {
            buildTrace(decision, choice, attribution, tick)
        } else {
            null
        }

        return StepRecord(tick, choice, outcome, frame, trace, attribution, spontaneous)
    }

    private fun organismStateOrNull(): OrganismState =
        (agent as? OrganismAgent)?.state ?: SCRIPTED_PLACEHOLDER

    private fun buildTrace(
        decision: Decision,
        choice: AgentChoice,
        attribution: AttributionResult?,
        tick: Long,
    ): SpikeDecisionTrace {
        val organism = agent as OrganismAgent
        val s = organism.state
        val target = choice.target
        val curiosityPhase = if (target != null) {
            val period = CuriosityWave.periodFor(s.seed, target.ordinal0, s.curiosity.periods)
            CuriosityWave.phase(s.seed, target.ordinal0, tick, period)
        } else {
            0L
        }
        return SpikeDecisionTrace(
            runId = runId,
            organismId = "${agent.cohort.cohortId}#${s.seed}",
            tick = tick,
            cohort = agent.cohort,
            winningTier = decision.winningTier,
            selectedAction = choice.action,
            selectedTarget = target,
            expectedOutcomeClass = expectedOutcome(choice.action),
            eligible = decision.eligible.map {
                SpikeDecisionTrace.TracedProposal(
                    action = it.action,
                    target = it.target,
                    tier = it.tier,
                    utility = it.fullUtility(fx),
                    contributions = it.contributions,
                    rejectionReason = if (it === decision.winner) null else "lower utility in winning tier",
                )
            },
            rejectedLowerTier = decision.discardedLowerTier.map {
                SpikeDecisionTrace.TracedProposal(
                    action = it.action,
                    target = it.target,
                    tier = it.tier,
                    utility = it.fullUtility(fx),
                    contributions = it.contributions,
                    rejectionReason = "tier ${it.tier} discarded below winning tier ${decision.winningTier}",
                )
            },
            driveSnapshot = longArrayOf(s.energy, s.rest, s.safety, s.social),
            modulatorSnapshot = longArrayOf(s.arousal, s.stress, s.rewardExpectancy),
            traitSnapshot = longArrayOf(
                s.traits.curiosity, s.traits.sociability, s.traits.caution, s.traits.persistence,
            ),
            curiosityPhase = curiosityPhase,
            inspectionInhibition = if (target != null) s.inhibition[target.ordinal0] else 0L,
            commitmentRemaining = s.commitmentRemaining,
            refractoryActive = tick < s.refractoryUntilTick[choice.action.ordinal],
            opportunityPromoted = decision.opportunityPromoted,
            runnerUpMargin = decision.runnerUpMargin,
            tieBreakDetermined = decision.tieBreakDetermined,
            tieBreakDomain = if (decision.tieBreakDetermined) {
                SpikeRandomDomain.CURIOSITY_TIE_BREAK
            } else {
                null
            },
            tieBreakDraw = decision.tieBreakDraw,
            spontaneous = decision.spontaneous,
            attribution = attribution,
        )
    }

    private fun expectedOutcome(action: SpikeAction): ExpectedOutcome = when (action) {
        SpikeAction.EAT -> ExpectedOutcome.RESOURCE_GAIN
        SpikeAction.REST, SpikeAction.SLEEP -> ExpectedOutcome.REST_GAIN
        SpikeAction.SEEK_INTERACTION, SpikeAction.RESPOND_TO_TOUCH,
        SpikeAction.VOCALIZE, SpikeAction.PLAY,
        -> ExpectedOutcome.SOCIAL_GAIN
        SpikeAction.WITHDRAW -> ExpectedOutcome.THREAT_REDUCTION
        SpikeAction.OBSERVE, SpikeAction.ORIENT, SpikeAction.EXPLORE, SpikeAction.APPROACH ->
            ExpectedOutcome.EPISTEMIC_GAIN
        else -> ExpectedOutcome.NO_CHANGE_EXPECTED
    }

    private companion object {
        /**
         * Scripted cohorts have no organism state, and the outcome model only
         * reads it for symmetry. One shared inert instance keeps the outcome
         * model identical across cohorts instead of branching on cohort identity.
         */
        val SCRIPTED_PLACEHOLDER = OrganismState(0L, emptySet())
    }
}
