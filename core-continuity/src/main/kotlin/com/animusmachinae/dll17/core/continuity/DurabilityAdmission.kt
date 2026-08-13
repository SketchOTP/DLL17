package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.state.DurabilityClass

/**
 * Durability admission and the safe-hold orderings, contract section 10.
 *
 * The orderings are returned as an explicit ordered list of step labels rather
 * than merely performed. Every one of them encodes an anti-exploit — debit
 * before reveal, commit before reopen — so a qualification has to be able to
 * assert the order itself, not just the end state. A reordering here is a
 * contract violation, not a refactor.
 */
public object SafeHoldSteps {
    public const val CLOSE_MUTATION_ADMISSION: String = "close-mutation-producing-admission"
    public const val ATTEMPT_EMERGENCY_COMMIT: String = "attempt-emergency-reserve-commit-frame"
    public const val COMMIT_HOLD_ENTERED: String = "commit-DurabilitySafeHoldEntered"
    public const val STOP_CANONICAL_ADVANCEMENT: String = "stop-canonical-advancement"
    public const val TEMPORAL_DESYNC: String = "present-TEMPORAL_DESYNC"
    public const val ENTER_STORAGE_FAULT: String = "enter-STORAGE_FAULT-from-last-durable-anchor"

    public const val STORAGE_SELF_TEST: String = "storage-self-test-and-reserve-check"
    public const val COMMIT_HOLD_EXITED: String = "commit-DurabilitySafeHoldExited"
    public const val CLOSE_INTERACTION_GATE: String = "close-organism-interaction-gate"
    public const val RECONCILE: String = "reconcile-from-last-durable-anchor"
    public const val COMMIT_RECONCILED: String = "commit-reconciled-result"
    public const val REVEAL: String = "reveal-final-canonical-state"
    public const val REOPEN_ADMISSION: String = "reopen-mutation-admission"

    /** The exact entry ordering, for assertion. */
    public val ENTRY_ORDER: List<String> = listOf(
        CLOSE_MUTATION_ADMISSION,
        ATTEMPT_EMERGENCY_COMMIT,
        COMMIT_HOLD_ENTERED,
        STOP_CANONICAL_ADVANCEMENT,
        TEMPORAL_DESYNC,
    )

    /** The exact exit ordering, for assertion. */
    public val EXIT_ORDER: List<String> = listOf(
        STORAGE_SELF_TEST,
        COMMIT_HOLD_EXITED,
        CLOSE_INTERACTION_GATE,
        RECONCILE,
        COMMIT_RECONCILED,
        REVEAL,
        REOPEN_ADMISSION,
    )
}

/** What a hold transition actually did, in order. */
public class HoldTransition(
    public val steps: List<String>,
    public val state: ContinuityState,
    public val events: List<ContinuityEvent>,
    public val succeeded: Boolean,
    public val detail: String,
)

/**
 * The durability admission controller.
 *
 * Admission is evaluated from durable capacity alone. It deliberately does not
 * consult the reducer, the UI or the platform: a mutation is admitted because
 * there is room to make it durable, and for no other reason.
 */
public class DurabilityAdmissionController(
    private val journal: ContinuityJournal,
    private val store: EncryptedRecordStore,
    private val gate: InteractionGate,
) {
    /** Contract section 9.2 thresholds, evaluated against live usage. */
    public fun evaluate(currentState: ContinuityState): DurabilityAdmissionState {
        if (currentState.admissionState == DurabilityAdmissionState.STORAGE_FAULT) {
            return DurabilityAdmissionState.STORAGE_FAULT
        }
        val used = store.usedBytes
        val capacity = store.capacityBytes
        val reserveFloor = capacity - ContinuityContract.EMERGENCY_DURABILITY_RESERVE_BYTES
        return when {
            used >= reserveFloor -> DurabilityAdmissionState.READ_ONLY_SURVIVAL
            used >= ContinuityContract.JOURNAL_SOFT_FLIP_BYTES -> DurabilityAdmissionState.PRESSURE
            else -> DurabilityAdmissionState.OPEN
        }
    }

    /**
     * Enters a durability hold in the normative order.
     *
     * If the emergency commit itself cannot be made, the controller enters
     * `STORAGE_FAULT` from the last already-durable anchor rather than
     * presenting a newer in-memory state as canonical.
     */
    public fun enterSafeHold(
        state: ContinuityState,
        ctx: ArithmeticContext,
    ): HoldTransition {
        val steps = ArrayList<String>()
        val events = ArrayList<ContinuityEvent>()

        steps += SafeHoldSteps.CLOSE_MUTATION_ADMISSION
        gate.close()

        steps += SafeHoldSteps.ATTEMPT_EMERGENCY_COMMIT
        val entered = ContinuityEvent.of(
            state.lastCommitSequence + 1L,
            ContinuityEventType.DURABILITY_SAFE_HOLD_ENTERED,
            durability = DurabilityClass.WITNESSED,
        )
        val next = try {
            journal.append(entered)
            steps += SafeHoldSteps.COMMIT_HOLD_ENTERED
            events += entered
            ContinuityReducer.reduce(state, entered, ctx)
        } catch (failure: RuntimeException) {
            steps += SafeHoldSteps.ENTER_STORAGE_FAULT
            val faulted = ContinuityEvent.of(
                state.lastCommitSequence + 1L,
                ContinuityEventType.ADMISSION_STATE_CHANGED,
                a = DurabilityAdmissionState.STORAGE_FAULT.ordinal32.toLong(),
            )
            // Not journaled: the medium is what failed. The last durable anchor
            // remains authoritative and the newer state is not presented.
            return HoldTransition(
                steps = steps,
                state = ContinuityReducer.reduce(state, faulted, ctx).copy(
                    presentationState = DurabilityPresentationState.STORAGE_REPAIR_REQUIRED,
                ),
                events = emptyList(),
                succeeded = false,
                detail = "emergency safe-hold commit failed: ${failure.message}",
            )
        }

        steps += SafeHoldSteps.STOP_CANONICAL_ADVANCEMENT
        steps += SafeHoldSteps.TEMPORAL_DESYNC

        return HoldTransition(steps, next, events, true, "hold entered")
    }

    /**
     * Leaves a durability hold in the normative order.
     *
     * The exit does not erase the held interval: elapsed biology is handled by
     * the ordinary trusted-time and reconciliation rules, so a hold is never a
     * way to skip time.
     */
    public fun exitSafeHold(
        state: ContinuityState,
        observation: ClockObservation,
        ctx: ArithmeticContext,
    ): HoldTransition {
        val steps = ArrayList<String>()
        val events = ArrayList<ContinuityEvent>()

        steps += SafeHoldSteps.STORAGE_SELF_TEST
        if (!store.selfTest()) {
            return HoldTransition(
                steps = steps,
                state = state.copy(
                    admissionState = DurabilityAdmissionState.STORAGE_FAULT,
                    presentationState = DurabilityPresentationState.STORAGE_REPAIR_REQUIRED,
                ),
                events = emptyList(),
                succeeded = false,
                detail = "durability self-test failed; no canonical mutation is accepted",
            )
        }

        val exited = ContinuityEvent.of(
            state.lastCommitSequence + 1L,
            ContinuityEventType.DURABILITY_SAFE_HOLD_EXITED,
            durability = DurabilityClass.WITNESSED,
        )
        journal.append(exited)
        steps += SafeHoldSteps.COMMIT_HOLD_EXITED
        events += exited
        var current = ContinuityReducer.reduce(state, exited, ctx)

        steps += SafeHoldSteps.CLOSE_INTERACTION_GATE
        gate.close()

        steps += SafeHoldSteps.RECONCILE
        val evidence = ClockTrust.classify(current.anchor, observation)
        val plan = Reconciliation.runToCompletion(current, observation, evidence, ctx)
        current = plan.finalState

        steps += SafeHoldSteps.COMMIT_RECONCILED
        journal.appendBatch(plan.events)
        events += plan.events

        val reopened = ContinuityEvent.of(
            current.lastCommitSequence + 1L,
            ContinuityEventType.ADMISSION_STATE_CHANGED,
            a = DurabilityAdmissionState.OPEN.ordinal32.toLong(),
        )
        journal.append(reopened)
        events += reopened
        current = ContinuityReducer.reduce(current, reopened, ctx)

        steps += SafeHoldSteps.REVEAL
        steps += SafeHoldSteps.REOPEN_ADMISSION
        gate.open()

        return HoldTransition(steps, current, events, true, "hold exited and reconciled")
    }
}

/**
 * `TEMPORAL_DESYNC` presentation rules, contract section 11.
 *
 * Modelled as a refusing sink for the same reason `GatedPresentationSink` is: an
 * invariant every call site has to remember is a convention, and an invariant
 * the sink refuses to violate is a control. The prohibited vocabulary is checked
 * by name because the architecture prohibits it by name.
 */
public class DurabilityPresentation(private var state: DurabilityPresentationState) {

    private val emitted: MutableList<String> = ArrayList()

    public val log: List<String> get() = emitted.toList()
    public val currentState: DurabilityPresentationState get() = state

    public fun transitionTo(next: DurabilityPresentationState) {
        state = next
    }

    /** Historical telemetry, always labelled with its durable age. */
    public fun showLastDurableStatus(elapsedAgeMillis: Long) {
        emitted += "historical-telemetry age=${elapsedAgeMillis}ms"
    }

    /** The honest contact-lost message. */
    public fun showContactInterrupted() {
        requireDesync("contact-interrupted message")
        emitted += "contact-interrupted: biological time may still be passing"
    }

    /**
     * Any presentation implying the organism is currently alive and observed.
     *
     * Refused during a hold. This is the single most important rule in section
     * 11: an interface that animates a healthy organism through an interval with
     * no durable transitions is lying about biology it did not observe.
     */
    public fun showAutonomousLife(description: String) {
        if (state != DurabilityPresentationState.RECOVERY_RECONCILIATION) {
            throw IllegalStateException(
                "autonomous-life presentation ($description) is prohibited in $state; " +
                    "the organism may not appear to act while contact is lost",
            )
        }
        emitted += "autonomous-life: $description"
    }

    private fun requireDesync(what: String) {
        if (state != DurabilityPresentationState.TEMPORAL_DESYNC) {
            throw IllegalStateException("$what requires TEMPORAL_DESYNC, not $state")
        }
    }

    public companion object {
        /**
         * Vocabulary the architecture prohibits during a durability hold.
         *
         * Checked as text because the prohibition is about what the user is told,
         * and a copy string is the only place that can go wrong.
         */
        public val PROHIBITED_VOCABULARY: List<String> = listOf(
            "cryosleep",
            "frozen time",
            "frozen-time",
            "paused life",
            "paused-life",
            "suspended animation",
            "suspended-animation",
            "stasis",
        )

        /** True when a copy string is safe to show during a hold. */
        public fun copyIsHonest(text: String): Boolean {
            val lowered = text.lowercase()
            return PROHIBITED_VOCABULARY.none { lowered.contains(it) }
        }
    }
}
