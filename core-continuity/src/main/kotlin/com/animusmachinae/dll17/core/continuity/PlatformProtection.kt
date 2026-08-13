package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.state.DurabilityClass
import com.animusmachinae.dll17.core.state.PlatformPanicWitness

/**
 * Platform thermal and power protection, contract section 12.
 *
 * Platform protection has absolute priority over in-world rendering. The one
 * rule that everything else here serves: **no retry loop**. A device in a
 * thermal or imminent-power-loss emergency must not be handed a write loop, so
 * there is at most one canonical anchor attempt and at most one witness attempt,
 * and both are allowed to fail.
 */

/** Subsystems that must stop when deep suspend is entered. Order is frozen. */
public enum class Subsystem(public val ordinal32: Int) {
    RENDERER(1),
    PHYSICS(2),
    PATHFINDER(3),
    INVERSE_KINEMATICS(4),
    CAMERA(5),
    MICROPHONE(6),
    AUDIO_ENGINE(7),
    NONESSENTIAL_SENSORS(8),
    PARTICLES(9),
    PERIODIC_FOREGROUND_SIMULATION(10),
}

/**
 * Progressive resource shedding, contract section 12.2.
 *
 * The order is frozen so that shedding is reproducible rather than "whatever the
 * renderer happened to drop first". Canonical correctness is unaffected by which
 * level is active, which is exactly why this class holds no canonical state.
 */
public class ResourceShedController {
    private var level: Int = 0

    public val currentLevel: Int get() = level
    public val shedSteps: List<String> get() = SHED_ORDER.take(level)

    public fun shedTo(newLevel: Int) {
        if (newLevel < 0 || newLevel > SHED_ORDER.size) {
            throw IllegalArgumentException("shed level must lie in 0..${SHED_ORDER.size}")
        }
        level = newLevel
    }

    public fun restoreFully() {
        level = 0
    }

    /** Whether an optional interaction that raises thermal load may run. */
    public fun admitsOptionalLoad(): Boolean = level == 0

    public companion object {
        public val SHED_ORDER: List<String> = listOf(
            "particles",
            "post-processing",
            "shadows",
            "camera-capture",
            "microphone-analysis",
            "high-frequency-sensors",
            "physics-detail",
            "pathfinding-frequency",
            "audio-synthesis",
            "animation-complexity",
            "frame-rate-cap-and-2d-surface",
        )
    }
}

/** Tracks which subsystems are running. Non-canonical. */
public class SubsystemRegistry {
    private val running: MutableSet<Subsystem> = Subsystem.entries.toMutableSet()
    private val releaseOrder: MutableList<Subsystem> = ArrayList()

    public val runningSubsystems: Set<Subsystem> get() = running.toSet()
    public val releasedInOrder: List<Subsystem> get() = releaseOrder.toList()

    public fun releaseAll() {
        for (subsystem in Subsystem.entries) {
            if (running.remove(subsystem)) releaseOrder += subsystem
        }
    }

    public fun restartIncrementally(): List<Subsystem> {
        val restarted = ArrayList<Subsystem>()
        for (subsystem in Subsystem.entries) {
            if (running.add(subsystem)) restarted += subsystem
        }
        return restarted
    }
}

/** Ordered record of a deep-suspend attempt. */
public class DeepSuspendOutcome(
    public val steps: List<String>,
    public val anchorCommitted: Boolean,
    public val witnessAttempted: Boolean,
    public val witnessWritten: Boolean,
    public val state: ContinuityState,
    public val events: List<ContinuityEvent>,
    public val lastDurableSequence: Long,
)

public object DeepSuspendSteps {
    public const val CLOSE_ADMISSION: String = "close-mutation-producing-admission"
    public const val ATTEMPT_ANCHOR: String = "attempt-one-bounded-PlatformDeepSuspendEntered-anchor"
    public const val ATTEMPT_WITNESS: String = "attempt-one-preallocated-PlatformPanicWitness"
    public const val RELEASE_SUBSYSTEMS: String = "release-renderer-physics-sensors-audio"
    public const val CEASE_SIMULATION: String = "cease-periodic-foreground-simulation"
    public const val PERMIT_BACKGROUND: String = "permit-background-or-termination"

    public const val HYSTERESIS: String = "verify-thermal-power-hysteresis"
    public const val ENTER_RECOVERY: String = "enter-PLATFORM_RECOVERY-on-2d-surface"
    public const val RECONCILE: String = "bounded-elapsed-reconciliation"
    public const val COMMIT: String = "commit-recovered-state"
    public const val RESTART_SUBSYSTEMS: String = "reinitialize-subsystems-incrementally"
    public const val REVEAL: String = "reveal-qualified-found-state"
}

/**
 * The deep-suspend and recovery controller.
 *
 * The panic witness is a **recovery hint only**. It is excluded from canonical
 * hashing and replay, it cannot reconstruct interactions, and failing to write
 * it is tolerated — the last durable canonical anchor stays authoritative either
 * way.
 */
public class PlatformProtectionController(
    private val journal: ContinuityJournal,
    private val subsystems: SubsystemRegistry,
    private val gate: InteractionGate,
    private val witness: PlatformPanicWitness = PlatformPanicWitness(
        attemptDeadlineNanos = ContinuityContract.PANIC_WITNESS_ATTEMPT_DEADLINE_MILLIS * 1_000_000L,
    ),
) {
    public val panicWitness: PlatformPanicWitness get() = witness

    /**
     * Enters deep suspend in the normative order.
     *
     * `suspendReasonOrdinal` is a class of platform condition, not a diegetic
     * hazard: a thermal emergency is a platform-protection event and must never
     * become part of the organism's story.
     */
    public fun enterDeepSuspend(
        state: ContinuityState,
        suspendReasonOrdinal: Int,
        ctx: ArithmeticContext,
        monotonicNanos: Long = 0L,
    ): DeepSuspendOutcome {
        val steps = ArrayList<String>()
        val events = ArrayList<ContinuityEvent>()

        steps += DeepSuspendSteps.CLOSE_ADMISSION
        gate.close()

        steps += DeepSuspendSteps.ATTEMPT_ANCHOR
        val anchorEvent = ContinuityEvent.of(
            state.lastCommitSequence + 1L,
            ContinuityEventType.PLATFORM_DEEP_SUSPEND_ENTERED,
            a = suspendReasonOrdinal.toLong(),
            durability = DurabilityClass.WITNESSED,
        )
        var anchorCommitted = false
        var next = state
        try {
            journal.append(anchorEvent)
            anchorCommitted = true
            events += anchorEvent
            next = ContinuityReducer.reduce(state, anchorEvent, ctx)
        } catch (failure: RuntimeException) {
            // Deliberately swallowed. Retrying under critical heat or power
            // pressure is the failure mode this ordering exists to prevent, and
            // the last durable anchor remains authoritative without it.
            anchorCommitted = false
        }

        var witnessAttempted = false
        var witnessWritten = false
        if (!anchorCommitted) {
            steps += DeepSuspendSteps.ATTEMPT_WITNESS
            witnessAttempted = true
            witnessWritten = witness.write(
                reasonOrdinal = suspendReasonOrdinal,
                lastDurableSequence = journal.lastSequence,
                monotonicNanos = monotonicNanos,
            )
        }

        steps += DeepSuspendSteps.RELEASE_SUBSYSTEMS
        subsystems.releaseAll()
        steps += DeepSuspendSteps.CEASE_SIMULATION
        steps += DeepSuspendSteps.PERMIT_BACKGROUND

        // Even when the anchor failed, the in-memory platform state reflects the
        // suspend so that nothing keeps simulating. It is not canonical until a
        // write succeeds, which is why the journal is unchanged in that branch.
        if (!anchorCommitted) {
            next = state.copy(platformState = PlatformProtectionState.PLATFORM_DEEP_SUSPEND)
        }

        return DeepSuspendOutcome(
            steps = steps,
            anchorCommitted = anchorCommitted,
            witnessAttempted = witnessAttempted,
            witnessWritten = witnessWritten,
            state = next,
            events = events,
            lastDurableSequence = journal.lastSequence,
        )
    }

    /**
     * Recovers from deep suspend.
     *
     * Returns null when the hysteresis interval has not elapsed: re-entering the
     * renderer while the device is still hot is the thing hysteresis exists to
     * stop, and returning a partially recovered state would invite the caller to
     * proceed anyway.
     */
    public fun recoverFromDeepSuspend(
        state: ContinuityState,
        observation: ClockObservation,
        belowReentryThresholdForMillis: Long,
        ctx: ArithmeticContext,
    ): PlatformRecoveryOutcome? {
        if (belowReentryThresholdForMillis < ContinuityContract.THERMAL_REENTRY_HYSTERESIS_MILLIS) {
            return null
        }
        val steps = ArrayList<String>()
        steps += DeepSuspendSteps.HYSTERESIS
        steps += DeepSuspendSteps.ENTER_RECOVERY

        var current = state.copy(platformState = PlatformProtectionState.PLATFORM_RECOVERY)
        gate.close()

        steps += DeepSuspendSteps.RECONCILE
        val evidence = ClockTrust.classify(current.anchor, observation)
        val plan = Reconciliation.runToCompletion(current, observation, evidence, ctx)
        current = plan.finalState

        steps += DeepSuspendSteps.COMMIT
        journal.appendBatch(plan.events)

        val completed = ContinuityEvent.of(
            current.lastCommitSequence + 1L,
            ContinuityEventType.PLATFORM_RECOVERY_COMPLETED,
            durability = DurabilityClass.WITNESSED,
        )
        journal.append(completed)
        current = ContinuityReducer.reduce(current, completed, ctx)

        steps += DeepSuspendSteps.RESTART_SUBSYSTEMS
        val restarted = subsystems.restartIncrementally()

        steps += DeepSuspendSteps.REVEAL
        gate.open()

        return PlatformRecoveryOutcome(
            steps = steps,
            state = current,
            events = plan.events + completed,
            restartedSubsystems = restarted,
            reconciliationMode = plan.mode,
        )
    }
}

/** Ordered record of a platform recovery. */
public class PlatformRecoveryOutcome(
    public val steps: List<String>,
    public val state: ContinuityState,
    public val events: List<ContinuityEvent>,
    public val restartedSubsystems: List<Subsystem>,
    public val reconciliationMode: ReconciliationMode,
)
