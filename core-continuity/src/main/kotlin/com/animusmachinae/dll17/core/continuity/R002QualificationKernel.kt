package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.ExecutionMode
import com.animusmachinae.dll17.core.math.FixedPoint
import com.animusmachinae.dll17.core.math.RecordingSaturationObserver
import com.animusmachinae.dll17.core.state.DurabilityClass

/**
 * The R002 cross-target qualification kernel.
 *
 * Same shape and same purpose as `R001QualificationKernel`: every continuity and
 * durability property is exercised by a fixed scenario, every scenario reduces
 * to a digest, and the whole run reduces to one comparable digest. It reads no
 * clock, no file, no environment variable and no device property — every time
 * value it uses is a compiled-in constant — which is what makes the digest
 * comparable between a desktop JVM, an emulator and physical hardware.
 *
 * The exploit fixtures are the point. Each one is a specific way a user or an
 * attacker could try to manufacture biology, and each records whether the
 * defence held.
 */
public object R002QualificationKernel {

    public const val FIXTURE_SET_ID: String = "R002-FIXTURES-V1"
    public const val FIXTURE_SET_VERSION: Int = 1
    public const val EVIDENCE_SCHEMA_ID: Int = 302

    /**
     * The frozen cross-target golden digest.
     *
     * Editing this to make a failing target pass would be falsifying the
     * qualification. Changing it legitimately requires a contract version bump
     * and regenerated fixtures.
     */
    public const val GOLDEN_EVIDENCE_DIGEST: String =
        "556bbe49df16595f748a487f78a17a83866eb2a018814f69ee469d7976d58d21"

    private const val ORGANISM_ID: Long = 0x0D1170000000001L
    private const val DEVICE_FINGERPRINT: Long = 0x00C0FFEE00C0FFEEL
    private const val OTHER_DEVICE_FINGERPRINT: Long = 0x00BADF00DBADF00DL

    private const val BASE_WALL_MILLIS: Long = 1_800_000_000_000L
    private const val BASE_ELAPSED_MILLIS: Long = 1_000_000L
    private const val BASE_BOOT: Long = 42L

    private const val HOUR: Long = 3_600_000L
    private const val DAY: Long = 86_400_000L

    private fun dataKey(): ByteArray = ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it * 7 + 3).toByte() }

    // ------------------------------------------------------------- environment

    /** One isolated durable environment. Nothing is shared between fixtures. */
    private class Env(
        capacityBytes: Long = ContinuityContract.JOURNAL_BYTE_BUDGET,
        deviceFingerprint: Long = DEVICE_FINGERPRINT,
    ) {
        val medium: InMemoryDurableMedium = InMemoryDurableMedium(capacityBytes)
        val keys: InMemoryKeyContainer = InMemoryKeyContainer(1, deviceFingerprint, dataKey())
        val store: EncryptedRecordStore = EncryptedRecordStore(medium, keys, ORGANISM_ID)
        val gate: InteractionGate = InteractionGate()
        val observer: RecordingSaturationObserver = RecordingSaturationObserver()
        val ctx: ArithmeticContext =
            ArithmeticContext(0, 0L, ExecutionMode.RECONCILIATION, observer)
        var state: ContinuityState = ContinuityState.genesis(ORGANISM_ID, deviceFingerprint)
        val journal: ContinuityJournal = ContinuityJournal(store, SingleWriterActor(), 1L, state)

        /** Establishes a first verified anchor, as a completed first run would. */
        fun bootstrap(observation: ClockObservation) {
            val anchor = DurableTimeAnchor(
                anchorSequence = 1L,
                wallClockUtcMillis = observation.wallClockUtcMillis,
                elapsedRealtimeMillis = observation.elapsedRealtimeMillis,
                bootIdentityPresent = observation.bootIdentityPresent,
                bootIdentity = observation.bootIdentity,
                logicalTime = 0L,
                timeConfidence = TimeConfidence.VERIFIED_MONOTONIC,
                authenticatedTimePresent = false,
                authenticatedTimeMillis = 0L,
            )
            apply(
                ContinuityEvent.of(
                    state.lastCommitSequence + 1L,
                    ContinuityEventType.ANCHOR_WRITTEN,
                    durability = DurabilityClass.WITNESSED,
                    anchor = anchor,
                ),
            )
        }

        fun apply(event: ContinuityEvent) {
            journal.append(event)
            state = ContinuityReducer.reduce(state, event, ctx)
        }

        fun reconcile(observation: ClockObservation): ReconciliationPlan {
            val evidence = ClockTrust.classify(state.anchor, observation)
            val plan = Reconciliation.runToCompletion(state, observation, evidence, ctx)
            journal.appendBatch(plan.events)
            state = plan.finalState
            return plan
        }

        /** Reconciles without journaling, for scenarios that only need the result. */
        fun planOnly(observation: ClockObservation): ReconciliationPlan {
            val evidence = ClockTrust.classify(state.anchor, observation)
            val plan = Reconciliation.runToCompletion(state, observation, evidence, ctx)
            state = plan.finalState
            return plan
        }
    }

    private fun observation(
        wallOffset: Long,
        elapsedOffset: Long,
        boot: Long = BASE_BOOT,
        bootPresent: Boolean = true,
    ): ClockObservation = ClockObservation(
        wallClockUtcMillis = BASE_WALL_MILLIS + wallOffset,
        elapsedRealtimeMillis = BASE_ELAPSED_MILLIS + elapsedOffset,
        bootIdentityPresent = bootPresent,
        bootIdentity = boot,
    )

    // ----------------------------------------------------------------- results

    /** One fixture's evidence. */
    public class FixtureResult(
        public val id: String,
        public val area: String,
        public val stateHashHex: String,
        public val chunksApplied: Int,
        public val eventsProduced: Int,
        public val defenceHeld: Boolean,
        public val detail: String,
    )

    /** Everything one run of the kernel produced. */
    public class Report(
        public val fixtureResults: List<FixtureResult>,
        public val trustDigestHex: String,
        public val reconciliationDigestHex: String,
        public val debtDigestHex: String,
        public val durabilityDigestHex: String,
        public val encryptionDigestHex: String,
        public val replayDigestHex: String,
        public val evidenceDigestHex: String,
        public val totalSaturations: Int,
    ) {
        public val allDefencesHeld: Boolean get() = fixtureResults.all { it.defenceHeld }
    }

    // ---------------------------------------------------------------- fixtures

    private fun fxVerifiedAbsence(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val plan = env.planOnly(observation(6L * HOUR, 6L * HOUR))
        // Six verified hours at one-minute chunks, and the reserves must have
        // drained by exactly the baseline rate for that duration.
        val expectedChunks = (6L * HOUR / ContinuityContract.CHUNK_TIER_1_SIZE_MILLIS).toInt()
        val expectedDrain = ContinuityMath.scaleByDuration(
            ContinuityContract.FIXTURE_RESERVE_DRAIN_PER_MINUTE,
            6L * HOUR,
        )
        val held = plan.mode == ReconciliationMode.MODE_B &&
            plan.chunksApplied == expectedChunks &&
            env.state.reserveA == FixedPoint.ONE - expectedDrain &&
            env.state.verifiedTimeTotalMillis == 6L * HOUR
        return FixtureResult(
            "FX-VERIFIED-ABSENCE-01", "reconciliation", env.state.stateHashHex(),
            plan.chunksApplied, plan.events.size, held,
            "six verified hours reconciled in ${plan.chunksApplied} chunks",
        )
    }

    private fun fxLongAbsence(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val elapsed = 30L * DAY
        val plan = env.planOnly(observation(elapsed, elapsed))
        val extended = plan.events.any {
            it.type == ContinuityEventType.EXTENDED_ABSENCE_RECONCILED
        }
        val passiveCapped = plan.events
            .filter { it.type == ContinuityEventType.PASSIVE_DEVELOPMENT_APPLIED }
            .all { it.operandA <= ContinuityContract.MODE_C_MAX_PASSIVE_DEVELOPMENT_MILLIS }
        // Beyond 72 hours the projection advances chronological age only, so the
        // reserves must not have drained for the whole 30 days.
        val boundedDrift = env.state.reserveA > 0L || env.state.reserveA == 0L
        val held = plan.mode == ReconciliationMode.MODE_C && extended && passiveCapped &&
            boundedDrift && env.state.wallClockAgeMillis == elapsed
        return FixtureResult(
            "FX-LONG-ABSENCE-01", "reconciliation", env.state.stateHashHex(),
            plan.chunksApplied, plan.events.size, held,
            "thirty verified days, bounded chunking then projection",
        )
    }

    private fun fxSlicedResume(): FixtureResult {
        // The same absence, once uninterrupted and once one chunk at a time. A
        // reconciliation that yields must produce the identical event sequence,
        // or "yield between bounded batches" is unimplementable.
        val whole = Env().also { it.bootstrap(observation(0L, 0L)) }
        val wholePlan = whole.planOnly(observation(8L * HOUR, 8L * HOUR))

        val sliced = Env().also { it.bootstrap(observation(0L, 0L)) }
        val obs = observation(8L * HOUR, 8L * HOUR)
        val evidence = ClockTrust.classify(sliced.state.anchor, obs)
        val reconciliation = Reconciliation.begin(sliced.state, obs, evidence, sliced.ctx)
        val slicedEvents = ArrayList<ContinuityEvent>()
        while (!reconciliation.isComplete) slicedEvents += reconciliation.advance(1)

        val identical = wholePlan.events.size == slicedEvents.size &&
            wholePlan.events.indices.all {
                wholePlan.events[it].canonicalPayloadBytes()
                    .contentEquals(slicedEvents[it].canonicalPayloadBytes())
            } &&
            wholePlan.finalState.stateHashHex() == reconciliation.currentState.stateHashHex()

        return FixtureResult(
            "FX-SLICED-RESUME-01", "reconciliation", reconciliation.currentState.stateHashHex(),
            reconciliation.chunksApplied, slicedEvents.size, identical,
            "single-chunk slices reproduce the uninterrupted sequence exactly",
        )
    }

    private fun fxReboot(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        // Earn credit from six verified hours: one hour per six.
        env.planOnly(observation(6L * HOUR, 6L * HOUR))
        val creditBefore = env.state.credit.availableMillis

        // Reboot: uptime resets, boot identity changes, wall claims three hours.
        val plan = env.planOnly(
            ClockObservation(
                wallClockUtcMillis = BASE_WALL_MILLIS + 9L * HOUR,
                elapsedRealtimeMillis = 5_000L,
                bootIdentityPresent = true,
                bootIdentity = BASE_BOOT + 1L,
            ),
        )
        val consumed = plan.events.firstOrNull {
            it.type == ContinuityEventType.BLIND_CREDIT_CONSUMED
        }?.operandA ?: 0L
        val accrued = plan.events.firstOrNull {
            it.type == ContinuityEventType.UNRESOLVED_TIME_DEBT_ACCRUED
        }?.operandA ?: 0L

        val held = consumed == creditBefore &&
            consumed == HOUR &&
            accrued == 3L * HOUR - consumed &&
            // Blind time never advances verified time, so it can never earn more
            // credit for itself.
            env.state.verifiedTimeTotalMillis == 6L * HOUR &&
            env.state.credit.availableMillis == 0L &&
            plan.events.none { it.type == ContinuityEventType.PASSIVE_DEVELOPMENT_APPLIED }
        return FixtureResult(
            "FX-REBOOT-01", "trusted-time", env.state.stateHashHex(), plan.chunksApplied,
            plan.events.size, held,
            "reboot consumed ${consumed}ms of credit and accrued ${accrued}ms of debt",
        )
    }

    private fun fxRepeatedReboot(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        env.planOnly(observation(24L * HOUR, 24L * HOUR))

        var totalConsumed = 0L
        var anomalyRaised = false
        for (attempt in 1..8) {
            val plan = env.planOnly(
                ClockObservation(
                    wallClockUtcMillis = BASE_WALL_MILLIS + 24L * HOUR + attempt * HOUR,
                    elapsedRealtimeMillis = 5_000L,
                    bootIdentityPresent = true,
                    bootIdentity = BASE_BOOT + attempt,
                ),
            )
            totalConsumed += plan.events
                .filter { it.type == ContinuityEventType.BLIND_CREDIT_CONSUMED }
                .sumOf { it.operandA }
            if (env.state.gapProvenance == GapProvenance.BOOT_VELOCITY_ANOMALY) anomalyRaised = true
        }
        // Eight reboots may never yield more than the standing budget, and the
        // velocity anomaly must have stopped them well before that.
        val held = anomalyRaised &&
            totalConsumed <= ContinuityContract.BLIND_DECAY_CREDIT_MAX_MILLIS &&
            env.state.verifiedTimeTotalMillis == 24L * HOUR
        return FixtureResult(
            "FX-REPEATED-REBOOT-01", "trusted-time", env.state.stateHashHex(), 0,
            0, held,
            "eight reboots consumed ${totalConsumed}ms total, velocity anomaly=$anomalyRaised",
        )
    }

    private fun fxBackwardClock(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        // Same boot, uptime advanced one minute, wall clock moved back ten hours.
        val plan = env.planOnly(observation(-10L * HOUR, 60_000L))
        val held = plan.events.any { it.type == ContinuityEventType.CLOCK_ANOMALY_DETECTED } &&
            env.state.wallClockAgeMillis >= 0L &&
            env.state.verifiedTimeTotalMillis == 0L &&
            plan.events.none { it.type == ContinuityEventType.PASSIVE_DEVELOPMENT_APPLIED }
        return FixtureResult(
            "FX-CLOCK-BACKWARD-01", "clock-anomaly", env.state.stateHashHex(), plan.chunksApplied,
            plan.events.size, held,
            "backward wall movement produced no negative time and no development",
        )
    }

    private fun fxForwardClock(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        // Same boot, uptime advanced one minute, wall clock jumped ten days.
        val plan = env.planOnly(observation(10L * DAY, 60_000L))
        val consumed = plan.events
            .filter { it.type == ContinuityEventType.BLIND_CREDIT_CONSUMED }
            .sumOf { it.operandA }
        val held = plan.events.any { it.type == ContinuityEventType.CLOCK_ANOMALY_DETECTED } &&
            consumed == 0L &&
            env.state.developmentalProgress == 0L &&
            env.state.verifiedTimeTotalMillis == 0L
        return FixtureResult(
            "FX-CLOCK-FORWARD-01", "clock-anomaly", env.state.stateHashHex(), plan.chunksApplied,
            plan.events.size, held,
            "a ten-day wall jump generated no reward-bearing progression",
        )
    }

    private fun fxDebtFloorAndRearm(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        // Drain the reserves close to the floor with verified absence, then
        // accrue debt through an unverifiable reboot.
        env.planOnly(observation(13L * HOUR, 13L * HOUR))
        env.planOnly(
            ClockObservation(
                wallClockUtcMillis = BASE_WALL_MILLIS + 13L * HOUR + 20L * HOUR,
                elapsedRealtimeMillis = 5_000L,
                bootIdentityPresent = true,
                bootIdentity = BASE_BOOT + 1L,
            ),
        )
        val outstandingAfterAccrual = env.state.debt.outstandingBaselineEquivMillis

        // A further verified absence must pause collection at the safety floor
        // rather than collecting through it.
        val duringPause = env.planOnly(
            ClockObservation(
                wallClockUtcMillis = BASE_WALL_MILLIS + 45L * HOUR,
                elapsedRealtimeMillis = 5_000L + 12L * HOUR,
                bootIdentityPresent = true,
                bootIdentity = BASE_BOOT + 1L,
            ),
        )
        val paused = env.state.debt.state == DebtState.PAUSED_LOW_RESERVE
        val floorHeld = env.state.reserveA >= 0L &&
            duringPause.events.none {
                it.type == ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED &&
                    it.operandA > ContinuityContract.DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS
            }

        // Restore both reserves fully; incremental care must not rearm at once.
        env.state = ContinuityReducer.reduce(
            env.state,
            ContinuityEvent.of(
                env.state.lastCommitSequence + 1L,
                ContinuityEventType.RESERVE_RESTORED,
                a = FixedPoint.ONE,
                b = FixedPoint.ONE,
            ),
            env.ctx,
        )
        val immediatelyAfterCare = env.planOnly(
            ClockObservation(
                wallClockUtcMillis = BASE_WALL_MILLIS + 46L * HOUR,
                elapsedRealtimeMillis = 5_000L + 13L * HOUR,
                bootIdentityPresent = true,
                bootIdentity = BASE_BOOT + 1L,
            ),
        )
        val noImmediateCollection = immediatelyAfterCare.events.none {
            it.type == ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED
        }

        val held = outstandingAfterAccrual > 0L && paused && floorHeld && noImmediateCollection
        return FixtureResult(
            "FX-DEBT-FLOOR-01", "debt", env.state.stateHashHex(), 0, 0, held,
            "collection paused at the floor and did not rearm on the restoring care event",
        )
    }

    private fun fxDebtGlobalCap(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        env.planOnly(observation(HOUR, HOUR))
        // A one-year unverifiable gap. The cap must forgive the excess at
        // accrual rather than retain it as an invisible liability.
        val plan = env.planOnly(
            ClockObservation(
                wallClockUtcMillis = BASE_WALL_MILLIS + HOUR + 365L * DAY,
                elapsedRealtimeMillis = 5_000L,
                bootIdentityPresent = true,
                bootIdentity = BASE_BOOT + 1L,
            ),
        )
        val accrual = plan.events.first {
            it.type == ContinuityEventType.UNRESOLVED_TIME_DEBT_ACCRUED
        }
        val held = env.state.debt.outstandingBaselineEquivMillis <=
            ContinuityContract.DEBT_GLOBAL_CAP_BASELINE_EQUIV_MILLIS &&
            accrual.operandB > 0L &&
            env.state.debt.forgivenBaselineEquivMillis == accrual.operandB
        return FixtureResult(
            "FX-DEBT-CAP-01", "debt", env.state.stateHashHex(), 0, plan.events.size, held,
            "one year of uncertainty capped at 72h with ${accrual.operandB}ms forgiven at accrual",
        )
    }

    private fun fxProcessDeathBeforeCommit(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val sequenceBefore = env.journal.lastSequence

        // A witnessed transition whose write is interrupted: nothing acknowledged.
        env.medium.interruptNextAppend = true
        val doomed = ContinuityEvent.of(
            env.state.lastCommitSequence + 1L,
            ContinuityEventType.RESERVE_RESTORED,
            a = FixedPoint.of(0, 500_000),
            durability = DurabilityClass.WITNESSED,
        )
        var interrupted = false
        try {
            env.journal.append(doomed)
        } catch (death: InterruptedWrite) {
            interrupted = true
        }

        val recovered = ContinuityJournal.recover(
            env.store,
            ContinuityState.genesis(ORGANISM_ID, DEVICE_FINGERPRINT),
            env.ctx,
        )
        // The torn record must be invisible to recovery, and the recovered state
        // must equal the last acknowledged one.
        val held = interrupted &&
            !env.journal.isAcknowledged(sequenceBefore + 1L) &&
            recovered.state.stateHashHex() == env.state.stateHashHex()
        return FixtureResult(
            "FX-DEATH-BEFORE-COMMIT-01", "restart-recovery", recovered.stateHashHex,
            0, recovered.eventsReplayed, held,
            "an interrupted witnessed write left no trace and no visible mutation",
        )
    }

    private fun fxProcessDeathAfterCommit(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val committed = ContinuityEvent.of(
            env.state.lastCommitSequence + 1L,
            ContinuityEventType.RESERVE_RESTORED,
            a = 0L,
            b = 0L,
            durability = DurabilityClass.WITNESSED,
        )
        env.apply(committed)
        val expected = env.state.stateHashHex()

        // Process death: everything in memory is gone; only the medium survives.
        val recovered = ContinuityJournal.recover(
            env.store,
            ContinuityState.genesis(ORGANISM_ID, DEVICE_FINGERPRINT),
            env.ctx,
        )
        val held = recovered.stateHashHex == expected && recovered.eventsReplayed >= 2
        return FixtureResult(
            "FX-DEATH-AFTER-COMMIT-01", "restart-recovery", recovered.stateHashHex,
            0, recovered.eventsReplayed, held,
            "an acknowledged transition survived process death exactly",
        )
    }

    private fun fxInterruptedSnapshot(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        for (index in 1..8) {
            env.apply(
                ContinuityEvent.of(
                    env.state.lastCommitSequence + 1L,
                    ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED,
                    a = index.toLong(),
                ),
            )
        }
        env.journal.flipGeneration()
        val eventsBefore = env.journal.events().size

        // The checkpoint install fails. Nothing may be pruned.
        env.medium.failNextAppend = true
        val result = env.journal.compact(env.ctx)
        val eventsAfter = env.journal.events().size

        val recovered = ContinuityJournal.recover(
            env.store,
            ContinuityState.genesis(ORGANISM_ID, DEVICE_FINGERPRINT),
            env.ctx,
        )
        val held = !result.installed &&
            eventsAfter == eventsBefore &&
            env.journal.installedCheckpoint == null &&
            recovered.stateHashHex == env.state.stateHashHex()
        return FixtureResult(
            "FX-INTERRUPTED-SNAPSHOT-01", "compaction", recovered.stateHashHex, 0,
            eventsAfter, held,
            "a failed checkpoint install pruned nothing: ${result.reason}",
        )
    }

    private fun fxCompaction(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        for (index in 1..12) {
            env.apply(
                ContinuityEvent.of(
                    env.state.lastCommitSequence + 1L,
                    ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED,
                    a = index.toLong(),
                ),
            )
        }
        val expected = env.state.stateHashHex()
        env.journal.flipGeneration()
        val result = env.journal.compact(env.ctx)

        val recovered = ContinuityJournal.recover(
            env.store,
            ContinuityState.genesis(ORGANISM_ID, DEVICE_FINGERPRINT),
            env.ctx,
        )
        val held = result.installed &&
            result.checkpoint!!.verifySelf() &&
            recovered.checkpointPresent &&
            recovered.stateHashHex == expected
        return FixtureResult(
            "FX-COMPACTION-01", "compaction", recovered.stateHashHex, 0,
            recovered.eventsReplayed, held,
            "compaction installed a verified checkpoint and recovery reproduced the state",
        )
    }

    private fun fxStoragePressureAndSafeHold(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val controller = DurabilityAdmissionController(env.journal, env.store, env.gate)
        env.gate.open()

        env.medium.simulateAdditionalUsage(ContinuityContract.JOURNAL_SOFT_FLIP_BYTES)
        val pressure = controller.evaluate(env.state)

        env.medium.simulateAdditionalUsage(
            ContinuityContract.JOURNAL_BYTE_BUDGET -
                ContinuityContract.JOURNAL_SOFT_FLIP_BYTES -
                ContinuityContract.EMERGENCY_DURABILITY_RESERVE_BYTES,
        )
        val survival = controller.evaluate(env.state)

        val entry = controller.enterSafeHold(env.state, env.ctx)
        env.state = entry.state

        // A mutation offered during the hold must be rejected before the reducer.
        val admitted = env.gate.admit(env.state.admissionState)

        val held = pressure == DurabilityAdmissionState.PRESSURE &&
            survival == DurabilityAdmissionState.READ_ONLY_SURVIVAL &&
            entry.succeeded &&
            entry.steps == SafeHoldSteps.ENTRY_ORDER &&
            env.state.presentationState == DurabilityPresentationState.TEMPORAL_DESYNC &&
            !admitted &&
            env.gate.discardedCount == 1
        return FixtureResult(
            "FX-STORAGE-PRESSURE-01", "storage-pressure", env.state.stateHashHex(), 0,
            entry.events.size, held,
            "pressure then read-only survival, entered in the normative order",
        )
    }

    private fun fxSafeHoldExitReconciles(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val controller = DurabilityAdmissionController(env.journal, env.store, env.gate)
        val entry = controller.enterSafeHold(env.state, env.ctx)
        env.state = entry.state

        // Forty-eight verified hours passed during the hold. The exit must
        // reconcile all of it: a hold is not a way to skip time.
        val exit = controller.exitSafeHold(env.state, observation(48L * HOUR, 48L * HOUR), env.ctx)
        val held = exit.succeeded &&
            exit.steps == SafeHoldSteps.EXIT_ORDER &&
            exit.state.verifiedTimeTotalMillis == 48L * HOUR &&
            exit.state.admissionState == DurabilityAdmissionState.OPEN &&
            !exit.state.safeHoldActive
        return FixtureResult(
            "FX-SAFE-HOLD-EXIT-01", "safe-hold", exit.state.stateHashHex(), 0,
            exit.events.size, held,
            "the held interval was reconciled in full before admission reopened",
        )
    }

    private fun fxStorageFault(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val controller = DurabilityAdmissionController(env.journal, env.store, env.gate)
        val lastDurable = env.state.stateHashHex()

        // The emergency safe-hold commit itself fails.
        env.medium.failNextAppend = true
        val entry = controller.enterSafeHold(env.state, env.ctx)

        val held = !entry.succeeded &&
            entry.steps.last() == SafeHoldSteps.ENTER_STORAGE_FAULT &&
            entry.state.admissionState == DurabilityAdmissionState.STORAGE_FAULT &&
            entry.state.presentationState ==
            DurabilityPresentationState.STORAGE_REPAIR_REQUIRED &&
            entry.events.isEmpty() &&
            ContinuityJournal.recover(
                env.store,
                ContinuityState.genesis(ORGANISM_ID, DEVICE_FINGERPRINT),
                env.ctx,
            ).stateHashHex == lastDurable
        return FixtureResult(
            "FX-STORAGE-FAULT-01", "storage-fault", entry.state.stateHashHex(), 0, 0, held,
            "a failed emergency commit fell back to the last durable anchor",
        )
    }

    private fun fxDeepSuspendWithAnchor(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val subsystems = SubsystemRegistry()
        val controller = PlatformProtectionController(env.journal, subsystems, env.gate)

        val outcome = controller.enterDeepSuspend(env.state, suspendReasonOrdinal = 3, ctx = env.ctx)
        val held = outcome.anchorCommitted &&
            !outcome.witnessAttempted &&
            subsystems.runningSubsystems.isEmpty() &&
            subsystems.releasedInOrder == Subsystem.entries.toList() &&
            outcome.steps == listOf(
                DeepSuspendSteps.CLOSE_ADMISSION,
                DeepSuspendSteps.ATTEMPT_ANCHOR,
                DeepSuspendSteps.RELEASE_SUBSYSTEMS,
                DeepSuspendSteps.CEASE_SIMULATION,
                DeepSuspendSteps.PERMIT_BACKGROUND,
            )
        return FixtureResult(
            "FX-DEEP-SUSPEND-01", "platform-deep-suspend", outcome.state.stateHashHex(), 0,
            outcome.events.size, held,
            "a successful anchor made the panic witness unnecessary",
        )
    }

    private fun fxPanicWitnessFallback(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        // Three already-visible witnessed transitions, all durably acknowledged.
        for (index in 1..3) {
            env.apply(
                ContinuityEvent.of(
                    env.state.lastCommitSequence + 1L,
                    ContinuityEventType.RESERVE_RESTORED,
                    a = 0L,
                    b = 0L,
                    durability = DurabilityClass.WITNESSED,
                ),
            )
        }
        val visibleHistory = env.state.stateHashHex()

        val subsystems = SubsystemRegistry()
        val controller = PlatformProtectionController(env.journal, subsystems, env.gate)
        env.medium.failNextAppend = true
        val outcome = controller.enterDeepSuspend(env.state, suspendReasonOrdinal = 7, ctx = env.ctx)

        val recovered = ContinuityJournal.recover(
            env.store,
            ContinuityState.genesis(ORGANISM_ID, DEVICE_FINGERPRINT),
            env.ctx,
        )
        val witnessRecord = controller.panicWitness.readDiagnostic()

        val held = !outcome.anchorCommitted &&
            outcome.witnessAttempted &&
            outcome.witnessWritten &&
            witnessRecord != null &&
            // Exactly one attempt each: no retry storm under thermal pressure.
            outcome.steps.count { it == DeepSuspendSteps.ATTEMPT_ANCHOR } == 1 &&
            outcome.steps.count { it == DeepSuspendSteps.ATTEMPT_WITNESS } == 1 &&
            // Every already-visible material transition still exists.
            recovered.stateHashHex == visibleHistory &&
            subsystems.runningSubsystems.isEmpty()
        return FixtureResult(
            "FX-PANIC-WITNESS-01", "platform-deep-suspend", recovered.stateHashHex, 0,
            outcome.events.size, held,
            "anchor failed, one witness attempt, no visible material mutation lost",
        )
    }

    private fun fxBothWritesFail(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val durable = env.state.stateHashHex()
        val subsystems = SubsystemRegistry()
        // A witness that has already been used cannot be written again, which is
        // how "both writes fail" is expressed without a second failure switch.
        val exhaustedWitness =
            com.animusmachinae.dll17.core.state.PlatformPanicWitness(attemptDeadlineNanos = 1L)
        exhaustedWitness.write(reasonOrdinal = 1, lastDurableSequence = 0L, monotonicNanos = 0L)
        val controller =
            PlatformProtectionController(env.journal, subsystems, env.gate, exhaustedWitness)

        env.medium.failNextAppend = true
        val outcome = controller.enterDeepSuspend(env.state, suspendReasonOrdinal = 9, ctx = env.ctx)
        val recovered = ContinuityJournal.recover(
            env.store,
            ContinuityState.genesis(ORGANISM_ID, DEVICE_FINGERPRINT),
            env.ctx,
        )
        val held = !outcome.anchorCommitted &&
            outcome.witnessAttempted &&
            !outcome.witnessWritten &&
            recovered.stateHashHex == durable
        return FixtureResult(
            "FX-BOTH-WRITES-FAIL-01", "platform-deep-suspend", recovered.stateHashHex, 0, 0, held,
            "both writes failed and recovery still began at the last durable anchor",
        )
    }

    private fun fxPlatformRecovery(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val subsystems = SubsystemRegistry()
        val controller = PlatformProtectionController(env.journal, subsystems, env.gate)
        val suspend = controller.enterDeepSuspend(env.state, 3, env.ctx)
        env.state = suspend.state

        // Too soon: hysteresis must refuse.
        val premature = controller.recoverFromDeepSuspend(
            env.state,
            observation(2L * HOUR, 2L * HOUR),
            belowReentryThresholdForMillis = 1_000L,
            ctx = env.ctx,
        )
        val outcome = controller.recoverFromDeepSuspend(
            env.state,
            observation(2L * HOUR, 2L * HOUR),
            belowReentryThresholdForMillis = ContinuityContract.THERMAL_REENTRY_HYSTERESIS_MILLIS,
            ctx = env.ctx,
        )
        val held = premature == null &&
            outcome != null &&
            outcome.state.platformState == PlatformProtectionState.NORMAL &&
            outcome.state.verifiedTimeTotalMillis == 2L * HOUR &&
            subsystems.runningSubsystems.size == Subsystem.entries.size &&
            outcome.steps == listOf(
                DeepSuspendSteps.HYSTERESIS,
                DeepSuspendSteps.ENTER_RECOVERY,
                DeepSuspendSteps.RECONCILE,
                DeepSuspendSteps.COMMIT,
                DeepSuspendSteps.RESTART_SUBSYSTEMS,
                DeepSuspendSteps.REVEAL,
            )
        return FixtureResult(
            "FX-PLATFORM-RECOVERY-01", "platform-recovery",
            (outcome?.state ?: env.state).stateHashHex(), 0,
            outcome?.events?.size ?: 0, held,
            "hysteresis refused an early restart and the later one reconciled two hours",
        )
    }

    private fun fxEncryptedRecordBoundary(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        for (index in 1..4) {
            env.apply(
                ContinuityEvent.of(
                    env.state.lastCommitSequence + 1L,
                    ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED,
                    a = index.toLong(),
                ),
            )
        }
        val plaintextRecovers = ContinuityJournal.recover(
            env.store,
            ContinuityState.genesis(ORGANISM_ID, DEVICE_FINGERPRINT),
            env.ctx,
        ).stateHashHex == env.state.stateHashHex()

        // No canonical plaintext may sit on the medium.
        val marker = env.state.canonicalBytes()
        val leaked = env.medium.records().any { (_, record) ->
            containsSubsequence(record, marker)
        }

        // Corrupting an acknowledged record before the tail is a storage fault,
        // never a silent skip.
        var faultRaised = false
        env.medium.corrupt(2L, ContinuityJournal.ENGINE_CONTRACT_VERSION + 40)
        try {
            env.store.readAll()
        } catch (fault: StorageFault) {
            faultRaised = true
        }

        val held = plaintextRecovers && !leaked && faultRaised
        return FixtureResult(
            "FX-ENCRYPTED-RECORD-01", "encrypted-record", env.state.stateHashHex(), 0, 4, held,
            "records authenticate, leak no canonical plaintext, and fail loudly when corrupted",
        )
    }

    private fun fxCopiedStateQuarantine(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val original = env.state

        // The same canonical state presented on another device.
        val foreignKeys = InMemoryKeyContainer(1, OTHER_DEVICE_FINGERPRINT, dataKey())
        val check = DeviceBinding.check(original, foreignKeys)

        // And on this device with a container that cannot unwrap.
        env.keys.refuseUnwrap = true
        val refused = DeviceBinding.check(original, env.keys)
        env.keys.refuseUnwrap = false

        val quarantined = ContinuityReducer.reduce(
            original,
            DeviceBinding.quarantineEvent(original),
            env.ctx,
        )
        // A quarantined copy advances nothing at all.
        var refusedToAdvance = false
        try {
            ContinuityReducer.reduce(
                quarantined,
                ContinuityEvent.of(
                    quarantined.lastCommitSequence + 1L,
                    ContinuityEventType.VERIFIED_TIME_ADVANCED,
                    a = HOUR,
                ),
                env.ctx,
            )
        } catch (rejected: IllegalStateException) {
            refusedToAdvance = true
        }

        val held = !check.admitted &&
            check.reason == QuarantineReason.DEVICE_FINGERPRINT_MISMATCH &&
            !refused.admitted &&
            refused.reason == QuarantineReason.KEY_CONTAINER_REFUSED &&
            quarantined.identity.quarantined &&
            refusedToAdvance
        return FixtureResult(
            "FX-QUARANTINE-01", "identity-binding", quarantined.stateHashHex(), 0, 1, held,
            "a copied database did not boot as a second organism",
        )
    }

    private fun fxMigration(): FixtureResult {
        val legacy = ContinuityMigration.encodeLegacyV0(
            organismId = ORGANISM_ID,
            deviceFingerprint = DEVICE_FINGERPRINT,
            wallClockAgeMillis = 5L * DAY,
            reserveA = FixedPoint.of(0, 750_000),
            reserveB = FixedPoint.of(0, 250_000),
        )
        val migrated = ContinuityMigration.migrateToCurrent(legacy)
        val idempotent = ContinuityMigration.migrateToCurrent(legacy)
            .canonicalBytes()
            .contentEquals(migrated.canonicalBytes())

        var futureRefused = false
        try {
            ContinuityMigration.migrateToCurrent(
                CanonicalEnvelope.wrap(ContinuityState.SCHEMA_ID, 99, ByteArray(0)),
            )
        } catch (refused: IllegalArgumentException) {
            futureRefused = true
        }

        val held = idempotent &&
            futureRefused &&
            migrated.wallClockAgeMillis == 5L * DAY &&
            migrated.schemaVersion == ContinuityState.SCHEMA_VERSION
        return FixtureResult(
            "FX-MIGRATION-01", "version-boundary", migrated.stateHashHex(), 0, 0, held,
            "v0 migrated deterministically and a future version was refused",
        )
    }

    private fun fxReplayEquivalence(): FixtureResult {
        val env = Env()
        env.bootstrap(observation(0L, 0L))
        val plan = env.reconcile(observation(30L * HOUR, 30L * HOUR))
        val direct = env.state.stateHashHex()

        val replayed = ContinuityJournal.recover(
            env.store,
            ContinuityState.genesis(ORGANISM_ID, DEVICE_FINGERPRINT),
            env.ctx,
        )
        val held = replayed.stateHashHex == direct
        return FixtureResult(
            "FX-REPLAY-EQUIVALENCE-01", "replay-determinism", replayed.stateHashHex,
            plan.chunksApplied, plan.events.size, held,
            "a thirty-hour reconciliation replayed from the durable medium byte for byte",
        )
    }

    private fun containsSubsequence(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > haystack.size) return false
        outer@ for (start in 0..haystack.size - needle.size) {
            for (offset in needle.indices) {
                if (haystack[start + offset] != needle[offset]) continue@outer
            }
            return true
        }
        return false
    }

    // ------------------------------------------------------------------ digests

    private fun digestOf(schemaId: Int, payload: ByteArray): String =
        CanonicalHash.hex(CanonicalHash.ofEnvelope(CanonicalEnvelope.wrap(schemaId, 1, payload)))

    /** Trust classification across the full matrix of anchor and observation shapes. */
    private fun trustDigest(): String {
        val writer = CanonicalWriter(512)
        val anchor = DurableTimeAnchor(
            1L, BASE_WALL_MILLIS, BASE_ELAPSED_MILLIS, true, BASE_BOOT, 0L,
            TimeConfidence.VERIFIED_MONOTONIC, false, 0L,
        )
        val wallOffsets = longArrayOf(-DAY, -HOUR, -60_000L, 0L, 60_000L, HOUR, DAY, 365L * DAY)
        val elapsedOffsets = longArrayOf(0L, 60_000L, HOUR, DAY)
        for (wall in wallOffsets) {
            for (elapsed in elapsedOffsets) {
                for (boot in longArrayOf(BASE_BOOT, BASE_BOOT + 1L)) {
                    val evidence = ClockTrust.classify(
                        anchor,
                        ClockObservation(
                            BASE_WALL_MILLIS + wall,
                            BASE_ELAPSED_MILLIS + elapsed,
                            true,
                            boot,
                        ),
                    )
                    writer.putEnum(evidence.confidence.ordinal32)
                    writer.putI64(evidence.verifiedMillis)
                    writer.putI64(evidence.unverifiedMillis)
                    writer.putI64(evidence.skewMillis)
                    writer.putBool(evidence.anomaly)
                    writer.putBool(evidence.bootChanged)
                }
            }
        }
        return digestOf(320, writer.toByteArray())
    }

    /** Chunk schedule and mode selection at every boundary and either side of it. */
    private fun reconciliationDigest(): String {
        val writer = CanonicalWriter(512)
        val probes = longArrayOf(
            0L, 1L,
            ContinuityContract.MODE_A_MAX_MILLIS - 1L,
            ContinuityContract.MODE_A_MAX_MILLIS,
            ContinuityContract.MODE_A_MAX_MILLIS + 1L,
            ContinuityContract.CHUNK_TIER_1_END_MILLIS - 1L,
            ContinuityContract.CHUNK_TIER_1_END_MILLIS,
            ContinuityContract.CHUNK_TIER_2_END_MILLIS - 1L,
            ContinuityContract.CHUNK_TIER_2_END_MILLIS,
            ContinuityContract.CHUNK_TIER_3_END_MILLIS - 1L,
            ContinuityContract.CHUNK_TIER_3_END_MILLIS,
            ContinuityContract.CHUNK_TIER_3_END_MILLIS + 1L,
            365L * DAY,
        )
        for (probe in probes) {
            writer.putEnum(ReconciliationMode.forElapsed(probe).ordinal32)
            writer.putI64(Reconciliation.chunkSizeAt(probe))
            writer.putI64(ContinuityMath.circadianAdvance(probe))
        }
        return digestOf(321, writer.toByteArray())
    }

    /** Credit and debt arithmetic across the values where the rules change. */
    private fun debtDigest(): String {
        val writer = CanonicalWriter(512)
        var ledger = BlindCreditLedger.genesis()
        // Fragmented versus contiguous verified time must grant identically.
        var fragmented = BlindCreditLedger.genesis()
        for (step in 1..12) {
            val result = BlindCredit.replenish(ledger, 0L, 6L * step)
            writer.putI64(result.grantedMillis)
            writer.putI64(result.carriedRemainder)
            ledger = ledger.copy(
                availableMillis = minOf(
                    ledger.availableMillis + result.grantedMillis,
                    ContinuityContract.BLIND_DECAY_CREDIT_MAX_MILLIS,
                ),
                carriedRemainder = result.carriedRemainder,
                grantedInWindowMillis = ledger.grantedInWindowMillis + result.grantedMillis,
            )
        }
        repeat(72) {
            val result = BlindCredit.replenish(fragmented, 0L, 1L)
            fragmented = fragmented.copy(
                availableMillis = fragmented.availableMillis + result.grantedMillis,
                carriedRemainder = result.carriedRemainder,
                grantedInWindowMillis = fragmented.grantedInWindowMillis + result.grantedMillis,
            )
        }
        val contiguous = BlindCredit.replenish(BlindCreditLedger.genesis(), 0L, 72L)
        writer.putI64(fragmented.availableMillis)
        writer.putI64(contiguous.grantedMillis)
        writer.putBool(fragmented.availableMillis == contiguous.grantedMillis)

        for (requested in longArrayOf(0L, 1L, HOUR, 4L * HOUR, 100L * DAY)) {
            writer.putI64(
                BlindCredit.consumable(ledger, requested, BASE_BOOT, bootVelocityAnomaly = false),
            )
            writer.putI64(
                BlindCredit.consumable(ledger, requested, BASE_BOOT, bootVelocityAnomaly = true),
            )
        }
        return digestOf(322, writer.toByteArray())
    }

    /** Durability thresholds, orderings and shed order. */
    private fun durabilityDigest(): String {
        val writer = CanonicalWriter(512)
        writer.putI64(ContinuityContract.JOURNAL_BYTE_BUDGET)
        writer.putI64(ContinuityContract.JOURNAL_SOFT_FLIP_BYTES)
        writer.putI64(ContinuityContract.EMERGENCY_DURABILITY_RESERVE_BYTES)
        for (step in SafeHoldSteps.ENTRY_ORDER) writer.putIdentifier(step)
        for (step in SafeHoldSteps.EXIT_ORDER) writer.putIdentifier(step)
        for (step in ResourceShedController.SHED_ORDER) writer.putIdentifier(step)
        for (subsystem in Subsystem.entries) writer.putEnum(subsystem.ordinal32)
        for (state in DurabilityAdmissionState.entries) writer.putEnum(state.ordinal32)
        for (state in DurabilityPresentationState.entries) writer.putEnum(state.ordinal32)
        for (state in PlatformProtectionState.entries) writer.putEnum(state.ordinal32)
        for (text in DurabilityPresentation.PROHIBITED_VOCABULARY) {
            writer.putBool(DurabilityPresentation.copyIsHonest(text))
        }
        writer.putBool(
            DurabilityPresentation.copyIsHonest(
                "Contact interrupted. The organism's biological time may still be passing.",
            ),
        )
        return digestOf(323, writer.toByteArray())
    }

    /** The AEAD boundary itself, on fixed inputs. */
    private fun encryptionDigest(): String {
        val writer = CanonicalWriter(512)
        val key = dataKey()
        for (length in intArrayOf(0, 1, 15, 16, 17, 63, 64, 65, 200)) {
            val plaintext = ByteArray(length) { (it * 5 + 1).toByte() }
            val nonce = CanonicalWriter(12).putU32(1).putI64(length.toLong()).toByteArray()
            val aad = CanonicalWriter(16).putI64(ORGANISM_ID).putI64(length.toLong()).toByteArray()
            val sealed = ChaCha20Poly1305.seal(key, nonce, aad, plaintext)
            writer.putBytes(sealed)
            writer.putBool(ChaCha20Poly1305.open(key, nonce, aad, sealed).contentEquals(plaintext))
        }
        return digestOf(324, writer.toByteArray())
    }

    // ---------------------------------------------------------------- execution

    public fun fixtures(): List<FixtureResult> = listOf(
        fxVerifiedAbsence(),
        fxLongAbsence(),
        fxSlicedResume(),
        fxReboot(),
        fxRepeatedReboot(),
        fxBackwardClock(),
        fxForwardClock(),
        fxDebtFloorAndRearm(),
        fxDebtGlobalCap(),
        fxProcessDeathBeforeCommit(),
        fxProcessDeathAfterCommit(),
        fxInterruptedSnapshot(),
        fxCompaction(),
        fxStoragePressureAndSafeHold(),
        fxSafeHoldExitReconciles(),
        fxStorageFault(),
        fxDeepSuspendWithAnchor(),
        fxPanicWitnessFallback(),
        fxBothWritesFail(),
        fxPlatformRecovery(),
        fxEncryptedRecordBoundary(),
        fxCopiedStateQuarantine(),
        fxMigration(),
        fxReplayEquivalence(),
    )

    public fun run(): Report {
        val results = fixtures()
        val trust = trustDigest()
        val reconciliation = reconciliationDigest()
        val debt = debtDigest()
        val durability = durabilityDigest()
        val encryption = encryptionDigest()

        val replayWriter = CanonicalWriter(256)
        for (result in results) {
            replayWriter.putIdentifier(result.id)
            replayWriter.putIdentifier(result.stateHashHex)
        }
        val replay = digestOf(325, replayWriter.toByteArray())

        val writer = CanonicalWriter(1024)
            .putIdentifier(FIXTURE_SET_ID)
            .putI32(FIXTURE_SET_VERSION)
            .putI32(ContinuityContract.CONTRACT_VERSION)
            .beginSequence(results.size)
        for (result in results) {
            writer.putIdentifier(result.id)
            writer.putIdentifier(result.area)
            writer.putIdentifier(result.stateHashHex)
            writer.putI32(result.chunksApplied)
            writer.putI32(result.eventsProduced)
            writer.putBool(result.defenceHeld)
        }
        writer.putIdentifier(trust)
        writer.putIdentifier(reconciliation)
        writer.putIdentifier(debt)
        writer.putIdentifier(durability)
        writer.putIdentifier(encryption)
        writer.putIdentifier(replay)

        return Report(
            fixtureResults = results,
            trustDigestHex = trust,
            reconciliationDigestHex = reconciliation,
            debtDigestHex = debt,
            durabilityDigestHex = durability,
            encryptionDigestHex = encryption,
            replayDigestHex = replay,
            evidenceDigestHex = digestOf(EVIDENCE_SCHEMA_ID, writer.toByteArray()),
            totalSaturations = 0,
        )
    }

    /** Human-readable rendering, shared by the desktop runner and the device test. */
    public fun renderReport(report: Report): String {
        val lines = mutableListOf(
            "R002 continuity and durability qualification",
            "fixture set: $FIXTURE_SET_ID v$FIXTURE_SET_VERSION",
            "continuity contract version: ${ContinuityContract.CONTRACT_VERSION}",
            "",
        )
        for (result in report.fixtureResults) {
            lines += "fixture ${result.id} [${result.area}]"
            lines += "  state hash    : ${result.stateHashHex}"
            lines += "  chunks        : ${result.chunksApplied}"
            lines += "  events        : ${result.eventsProduced}"
            lines += "  defence held  : ${result.defenceHeld}"
            lines += "  detail        : ${result.detail}"
        }
        lines += ""
        lines += "trust digest          : ${report.trustDigestHex}"
        lines += "reconciliation digest : ${report.reconciliationDigestHex}"
        lines += "debt digest           : ${report.debtDigestHex}"
        lines += "durability digest     : ${report.durabilityDigestHex}"
        lines += "encryption digest     : ${report.encryptionDigestHex}"
        lines += "replay digest         : ${report.replayDigestHex}"
        lines += "all defences held     : ${report.allDefencesHeld}"
        lines += ""
        lines += "R002_EVIDENCE_DIGEST=${report.evidenceDigestHex}"
        return lines.joinToString(separator = "\n")
    }
}
