package com.animusmachinae.dll17.research.aliveness.viewer

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.Cohorts
import com.animusmachinae.dll17.research.aliveness.Fx
import com.animusmachinae.dll17.research.aliveness.Habitat
import com.animusmachinae.dll17.research.aliveness.HabitatCondition
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionEvent
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.OutcomeModel
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import com.animusmachinae.dll17.research.aliveness.SpikeRuntime

/** One normalized study event, as recorded for later analysis. */
public class StudyEvent(
    public val sessionId: String,
    public val elapsedMillis: Long,
    public val tick: Long,
    public val kind: InteractionKind,
    public val target: HabitatObject?,
)

/**
 * A blinded observation session.
 *
 * The session owns the cohort and never exposes it. There is no accessor, no
 * `toString` that names it, and no field the UI can read: a viewer that cannot
 * discover which controller it is driving cannot leak that to a rater, whether
 * through a label, a timing difference or a developer's convenience getter.
 *
 * This class is deliberately free of any UI dependency so the blinding, the
 * timing policy and the event log can be tested headlessly.
 */
public class ViewerSession(
    public val sessionId: String,
    /** What the rater sees. Identical for every cohort by construction. */
    public val displayLabel: String,
    cohort: Cohort,
    seed: Long,
    public val durationSeconds: Int = SpikeContract.PRIMARY_SESSION_SECONDS,
    condition: HabitatCondition = HabitatCondition.CONTROLLED_NOVELTY,
) {
    private val fx: Fx = Fx.counting().first
    private val habitat = Habitat(seed, condition)
    private val agent = Cohorts.create(cohort, seed, fx)
    private val runtime = SpikeRuntime(
        runId = sessionId,
        agent = agent,
        habitat = habitat,
        outcomes = OutcomeModel(),
        fx = fx,
        traceEveryDecision = false,
        attributionSampleEvery = 0,
    )

    private val pending = ArrayList<InteractionEvent>()
    private val log = ArrayList<StudyEvent>()

    public var tick: Long = 0L
        private set

    public var elapsedMillis: Long = 0L
        private set

    /** The only thing the UI is given. */
    public var frame: SpikeExpressionContract.ExpressionFrame =
        SpikeExpressionContract.frameFor(
            SpikeExpressionContract.PresentationInput(
                action = com.animusmachinae.dll17.research.aliveness.SpikeAction.IDLE_VARIATION,
                target = null,
                intensity = 0L,
                valence = 0L,
                tick = 0L,
            ),
        )
        private set

    /** Objects currently present, so the viewer can draw the habitat. */
    public fun presentObjects(): List<HabitatObject> = HabitatObject.ALL.filter { habitat.isPresent(it) }

    public val complete: Boolean
        get() = elapsedMillis >= durationSeconds * 1_000L

    /** A rater input. Recorded as a normalized study event and queued for the next tick. */
    public fun submit(kind: InteractionKind, target: HabitatObject?) {
        val person = target?.takeIf { it.kind == com.animusmachinae.dll17.research.aliveness.ObjectKind.SOCIAL }
        pending += InteractionEvent(tick, kind, target, person)
        log += StudyEvent(sessionId, elapsedMillis, tick, kind, target)
    }

    /**
     * Advance by one organism tick. The caller controls wall-clock pacing; the
     * session controls how much organism time that is, identically for every
     * cohort.
     */
    public fun advance() {
        val events = if (pending.isEmpty()) emptyList() else ArrayList(pending)
        pending.clear()
        val record = runtime.step(tick, events)
        frame = record.frame
        tick += 1
        elapsedMillis += SpikeContract.VIEWER_TICK_MILLIS.toLong()
    }

    public fun studyEvents(): List<StudyEvent> = log.toList()

    /** The frame log, for the shared passive-clip pipeline. */
    public fun frameSignature(): String = frame.signature()
}

/**
 * A standardized live paired session: two blinded instances, order counterbalanced.
 *
 * The pairing lives here rather than in the UI so that the order rule, the
 * matched starting condition and the shared seed are properties of the protocol
 * rather than of whoever wired the window.
 */
public class PairedSession(
    public val pairId: String,
    firstCohort: Cohort,
    secondCohort: Cohort,
    seed: Long,
    /** Counterbalancing: even pair indices see the first cohort first. */
    orderIndex: Int,
    durationSeconds: Int = SpikeContract.PRIMARY_SESSION_SECONDS,
) {
    private val forwardOrder = orderIndex % 2 == 0

    public val first: ViewerSession = ViewerSession(
        sessionId = "$pairId-1",
        displayLabel = "Creature A",
        cohort = if (forwardOrder) firstCohort else secondCohort,
        seed = seed,
        durationSeconds = durationSeconds,
    )

    public val second: ViewerSession = ViewerSession(
        sessionId = "$pairId-2",
        displayLabel = "Creature B",
        cohort = if (forwardOrder) secondCohort else firstCohort,
        seed = seed,
        durationSeconds = durationSeconds,
    )

    /** Both instances start from the same matched condition and seed. */
    public val matchedSeed: Long = seed
}
