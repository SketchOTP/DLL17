package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * Exact integer scalings used by continuity logic.
 *
 * These are **not** `Fixed64` operations and no rounding mode applies to them.
 * They are exact rational scalings of a non-negative millisecond duration,
 * evaluated in `i64` with truncation, which for non-negative operands is also
 * the floor. Doing them as two chained `Fixed64` operations would round twice
 * and make a chunked reconciliation disagree with an unchunked one — the exact
 * class of drift R002 has to rule out.
 */
public object ContinuityMath {

    /** Circadian period. Presentation-independent; a phase, not a timezone. */
    public const val CIRCADIAN_PERIOD_MILLIS: Long = 86_400_000L

    /**
     * Largest duration these scalings accept.
     *
     * Beyond this the product could overflow `i64`. Passing more is a caller
     * defect — reconciliation chunks are at most 15 minutes — so it throws
     * rather than saturating. Saturation is for canonical arithmetic; this is an
     * argument that cannot be right.
     */
    public const val MAX_SCALABLE_MILLIS: Long = 9_000_000_000_000L

    /** `perMinute` scaled by `millis`, exactly, in `i64`. */
    public fun scaleByDuration(perMinute: Long, millis: Long): Long {
        if (millis < 0L) {
            throw IllegalArgumentException("duration must not be negative, got $millis")
        }
        if (millis > MAX_SCALABLE_MILLIS) {
            throw IllegalArgumentException(
                "duration $millis exceeds the scalable maximum $MAX_SCALABLE_MILLIS; " +
                    "reconcile in chunks rather than in one span",
            )
        }
        if (perMinute < 0L) {
            throw IllegalArgumentException("per-minute rate must not be negative, got $perMinute")
        }
        return perMinute * millis / ContinuityContract.MILLIS_PER_MINUTE
    }

    /** Phase advance for `millis`, in `Fixed64`, already reduced into `[0,1)`. */
    public fun circadianAdvance(millis: Long): Long {
        if (millis < 0L) {
            throw IllegalArgumentException("duration must not be negative, got $millis")
        }
        val withinPeriod = millis % CIRCADIAN_PERIOD_MILLIS
        return withinPeriod * FixedPoint.ONE / CIRCADIAN_PERIOD_MILLIS
    }

    /** Adds a phase delta and wraps into `[0,1)`. */
    public fun advancePhase(phase: Long, delta: Long): Long = (phase + delta) % FixedPoint.ONE
}

/**
 * The canonical continuity reducer.
 *
 * Pure in `(state, event)`, single-threaded, and free of every ambient input —
 * no clock, no file, no device property. A reconciliation is therefore a value
 * that can be replayed, and a restart in the middle of one resumes rather than
 * guesses.
 *
 * Several handlers throw on inputs the contract forbids. Those throws are not
 * runtime conditions: the planner is required to emit only eligible events, so a
 * throw here is an implementation defect being surfaced at the earliest possible
 * point rather than a state that must be handled.
 */
public object ContinuityReducer {

    public const val VAR_RESERVE_A: Int = 11
    public const val VAR_RESERVE_B: Int = 12
    public const val VAR_DEVELOPMENTAL_PROGRESS: Int = 13
    public const val VAR_WALL_CLOCK_AGE: Int = 14

    public fun reduce(
        state: ContinuityState,
        event: ContinuityEvent,
        ctx: ArithmeticContext,
    ): ContinuityState {
        if (state.identity.quarantined && event.type != ContinuityEventType.QUARANTINE_ENTERED) {
            throw IllegalStateException(
                "quarantined state accepted no canonical event; a copied organism must not " +
                    "advance (event ${event.type})",
            )
        }
        val advanced = when (event.type) {
            ContinuityEventType.ANCHOR_WRITTEN -> {
                val anchor = event.anchor ?: throw IllegalArgumentException(
                    "ANCHOR_WRITTEN carries no anchor payload",
                )
                if (anchor.anchorSequence < state.anchor.anchorSequence) {
                    throw IllegalArgumentException(
                        "anchor sequence ${anchor.anchorSequence} is below the durable " +
                            "${state.anchor.anchorSequence}; anchors never move backwards",
                    )
                }
                state.copy(anchor = anchor)
            }

            ContinuityEventType.CLOCK_ANOMALY_DETECTED ->
                // An anomaly downgrades confidence and records evidence. It creates
                // no elapsed biology and applies no punitive jump, which is why
                // nothing but the anchor's confidence field changes here.
                state.copy(anchor = state.anchor.copy(timeConfidence = TimeConfidence.ANOMALOUS))

            ContinuityEventType.VERIFIED_TIME_ADVANCED -> {
                val millis = requireNonNegative(event.operandA, "verified elapsed")
                ctx.forVariable(VAR_WALL_CLOCK_AGE)
                state.copy(
                    wallClockAgeMillis = state.wallClockAgeMillis + millis,
                    verifiedTimeTotalMillis = state.verifiedTimeTotalMillis + millis,
                    circadianPhase = ContinuityMath.advancePhase(
                        state.circadianPhase,
                        ContinuityMath.circadianAdvance(millis),
                    ),
                )
            }

            ContinuityEventType.BASELINE_METABOLISM_APPLIED ->
                applyDrain(state, requireNonNegative(event.operandA, "baseline metabolism"), ctx)

            ContinuityEventType.BLIND_CREDIT_REPLENISHED -> {
                val granted = requireNonNegative(event.operandA, "credit granted")
                val newAvailable = minOf(
                    state.credit.availableMillis + granted,
                    ContinuityContract.BLIND_DECAY_CREDIT_MAX_MILLIS,
                )
                state.copy(
                    credit = state.credit.copy(
                        availableMillis = newAvailable,
                        carriedRemainder = event.operandB,
                        grantedInWindowMillis = state.credit.grantedInWindowMillis + granted,
                    ),
                )
            }

            ContinuityEventType.BLIND_CREDIT_CONSUMED -> {
                val consumed = requireNonNegative(event.operandA, "credit consumed")
                if (consumed > state.credit.availableMillis) {
                    throw IllegalStateException(
                        "consumption of $consumed exceeds available credit " +
                            "${state.credit.availableMillis}; credit is never overdrawn",
                    )
                }
                // Blind time advances chronological age and circadian phase and
                // drains reserves — and deliberately does **not** advance
                // verifiedTimeTotalMillis, because that counter is what gates
                // replenishment, debt eligibility and rearm. Letting an
                // unverifiable interval feed it would let a reboot loop
                // manufacture the verified time needed to earn more blind credit.
                val withTime = state.copy(
                    wallClockAgeMillis = state.wallClockAgeMillis + consumed,
                    circadianPhase = ContinuityMath.advancePhase(
                        state.circadianPhase,
                        ContinuityMath.circadianAdvance(consumed),
                    ),
                    credit = state.credit.copy(
                        availableMillis = state.credit.availableMillis - consumed,
                        consumedForBootPresent = true,
                        consumedForBoot = event.operandB,
                    ),
                )
                applyDrain(withTime, consumed, ctx)
            }

            ContinuityEventType.UNRESOLVED_TIME_DEBT_ACCRUED -> {
                val accrued = requireNonNegative(event.operandA, "debt accrued")
                val forgiven = requireNonNegative(event.operandB, "debt forgiven at accrual")
                val outstanding = state.debt.outstandingBaselineEquivMillis + accrued
                if (outstanding > ContinuityContract.DEBT_GLOBAL_CAP_BASELINE_EQUIV_MILLIS) {
                    throw IllegalStateException(
                        "accrual would raise outstanding debt to $outstanding, above the global " +
                            "cap; excess must be forgiven at accrual, never retained",
                    )
                }
                state.copy(
                    debt = state.debt.copy(
                        state = if (state.debt.state == DebtState.PAUSED_LOW_RESERVE) {
                            DebtState.PAUSED_LOW_RESERVE
                        } else {
                            DebtState.ACCRUED
                        },
                        outstandingBaselineEquivMillis = outstanding,
                        forgivenBaselineEquivMillis =
                            state.debt.forgivenBaselineEquivMillis + forgiven,
                        oldestAccrualVerifiedMillis =
                            if (state.debt.outstandingBaselineEquivMillis == 0L) {
                                state.verifiedTimeTotalMillis
                            } else {
                                state.debt.oldestAccrualVerifiedMillis
                            },
                    ),
                )
            }

            ContinuityEventType.METABOLIC_ADJUSTMENT_APPLIED -> {
                val amount = requireNonNegative(event.operandA, "debt adjustment")
                if (amount > state.debt.outstandingBaselineEquivMillis) {
                    throw IllegalStateException(
                        "adjustment $amount exceeds outstanding debt " +
                            "${state.debt.outstandingBaselineEquivMillis}",
                    )
                }
                if (amount > ContinuityContract.DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS) {
                    throw IllegalStateException(
                        "adjustment $amount exceeds the per-chunk cap " +
                            "${ContinuityContract.DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS}",
                    )
                }
                // The per-verified-day window rolls here rather than on a timer,
                // so the cap depends only on verified time and not on when the
                // application happened to be running.
                val dayElapsed =
                    state.verifiedTimeTotalMillis - state.debt.dayWindowStartVerifiedMillis
                val dayRolls = dayElapsed >= ContinuityContract.DEBT_VERIFIED_DAY_MILLIS
                val dayCollectedBefore =
                    if (dayRolls) 0L else state.debt.collectedInDayBaselineEquivMillis
                if (dayCollectedBefore + amount >
                    ContinuityContract.DEBT_PER_VERIFIED_DAY_CAP_BASELINE_EQUIV_MILLIS
                ) {
                    throw IllegalStateException(
                        "adjustment $amount would exceed the per-verified-day cap",
                    )
                }
                val drained = applyDrain(state, amount, ctx)
                drained.copy(
                    debt = drained.debt.copy(
                        state = DebtState.COLLECTING,
                        outstandingBaselineEquivMillis =
                            drained.debt.outstandingBaselineEquivMillis - amount,
                        collectedBaselineEquivMillis =
                            drained.debt.collectedBaselineEquivMillis + amount,
                        dayWindowStartVerifiedMillis = if (dayRolls) {
                            drained.verifiedTimeTotalMillis
                        } else {
                            drained.debt.dayWindowStartVerifiedMillis
                        },
                        collectedInDayBaselineEquivMillis = dayCollectedBefore + amount,
                    ),
                )
            }

            ContinuityEventType.DEBT_PAUSED_LOW_RESERVE -> state.copy(
                debt = state.debt.copy(
                    state = DebtState.PAUSED_LOW_RESERVE,
                    // Pausing clears any armed rearm: care that happened before
                    // the pause cannot pre-authorize collection after it.
                    abundanceStablePresent = false,
                    abundanceStableSinceVerifiedMillis = 0L,
                    rearmEffectivePresent = false,
                    rearmEffectiveAtVerifiedMillis = 0L,
                ),
            )

            ContinuityEventType.DEBT_ABUNDANCE_STABILITY_UPDATED -> state.copy(
                debt = state.debt.copy(
                    abundanceStablePresent = event.operandB == 1L,
                    abundanceStableSinceVerifiedMillis =
                        if (event.operandB == 1L) event.operandA else 0L,
                ),
            )

            ContinuityEventType.DEBT_REARM_ARMED -> state.copy(
                debt = state.debt.copy(
                    rearmEffectivePresent = true,
                    rearmEffectiveAtVerifiedMillis = event.operandA,
                ),
            )

            ContinuityEventType.DEBT_FORGIVEN -> {
                val forgiven = requireNonNegative(event.operandA, "debt forgiven")
                if (forgiven > state.debt.outstandingBaselineEquivMillis) {
                    throw IllegalStateException("forgiving more debt than is outstanding")
                }
                val remaining = state.debt.outstandingBaselineEquivMillis - forgiven
                state.copy(
                    debt = state.debt.copy(
                        state = if (remaining == 0L) DebtState.FORGIVEN else state.debt.state,
                        outstandingBaselineEquivMillis = remaining,
                        forgivenBaselineEquivMillis =
                            state.debt.forgivenBaselineEquivMillis + forgiven,
                    ),
                )
            }

            ContinuityEventType.EXTENDED_ABSENCE_RECONCILED -> state

            ContinuityEventType.DURABILITY_SAFE_HOLD_ENTERED -> state.copy(
                admissionState = DurabilityAdmissionState.READ_ONLY_SURVIVAL,
                presentationState = DurabilityPresentationState.TEMPORAL_DESYNC,
                safeHoldActive = true,
            )

            ContinuityEventType.DURABILITY_SAFE_HOLD_EXITED -> {
                if (!state.safeHoldActive) {
                    throw IllegalStateException("safe hold exit without an active hold")
                }
                state.copy(
                    presentationState = DurabilityPresentationState.RECOVERY_RECONCILIATION,
                    safeHoldActive = false,
                )
            }

            ContinuityEventType.PLATFORM_DEEP_SUSPEND_ENTERED -> state.copy(
                platformState = PlatformProtectionState.PLATFORM_DEEP_SUSPEND,
            )

            ContinuityEventType.PLATFORM_RECOVERY_COMPLETED -> state.copy(
                platformState = PlatformProtectionState.NORMAL,
            )

            ContinuityEventType.PLATFORM_STATE_CHANGED -> state.copy(
                platformState = PlatformProtectionState.fromOrdinal(event.operandA.toInt()),
            )

            ContinuityEventType.PRESENTATION_STATE_CHANGED -> state.copy(
                presentationState = DurabilityPresentationState.fromOrdinal(event.operandA.toInt()),
            )

            ContinuityEventType.ADMISSION_STATE_CHANGED -> state.copy(
                admissionState = DurabilityAdmissionState.fromOrdinal(event.operandA.toInt()),
            )

            ContinuityEventType.RECOVERY_PERFORMED -> state

            ContinuityEventType.RECOVERY_GAP_DECLARED -> state.copy(
                identity = state.identity.copy(identityEpoch = event.operandB.toInt()),
                gapProvenance = GapProvenance.LOST_TO_HARDWARE_FAULT,
            )

            ContinuityEventType.SNAPSHOT_CREATED -> state.copy(
                identity = state.identity.copy(
                    lastProtectedSequence = maxOf(
                        state.identity.lastProtectedSequence,
                        event.operandA,
                    ),
                    lastProtectedVerifiedMillis = state.verifiedTimeTotalMillis,
                ),
            )

            ContinuityEventType.MIGRATION_PERFORMED -> state

            ContinuityEventType.GENERATION_FLIPPED -> {
                if (event.operandA <= state.generationId) {
                    throw IllegalArgumentException(
                        "generation IDs are immutable and increasing; ${event.operandA} does not " +
                            "follow ${state.generationId}",
                    )
                }
                state.copy(generationId = event.operandA)
            }

            ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED -> state.copy(
                activeExperienceTicks =
                    state.activeExperienceTicks + requireNonNegative(event.operandA, "ticks"),
            )

            ContinuityEventType.RESERVE_RESTORED -> {
                ctx.forVariable(VAR_RESERVE_A)
                val restoredA = FixedPoint.clamp(
                    FixedPoint.satAdd(state.reserveA, event.operandA, ctx),
                    0L,
                    FixedPoint.ONE,
                    ctx,
                )
                ctx.forVariable(VAR_RESERVE_B)
                val restoredB = FixedPoint.clamp(
                    FixedPoint.satAdd(state.reserveB, event.operandB, ctx),
                    0L,
                    FixedPoint.ONE,
                    ctx,
                )
                state.copy(reserveA = restoredA, reserveB = restoredB)
            }

            ContinuityEventType.BOOT_OBSERVED -> {
                val anomalous = event.operandB == 1L
                // The velocity window is measured in *verified* time, and blind
                // reboots do not advance verified time. That is the point: a
                // cluster of reboots with no verified time between them is
                // exactly the implausible pattern being detected, and it shows up
                // here as a window that never elapses.
                val windowElapsed =
                    state.verifiedTimeTotalMillis - state.credit.bootWindowStartVerifiedMillis
                val windowExpired =
                    windowElapsed >= ContinuityContract.BOOT_VELOCITY_WINDOW_MILLIS
                state.copy(
                    credit = state.credit.copy(
                        bootWindowStartVerifiedMillis = if (windowExpired) {
                            state.verifiedTimeTotalMillis
                        } else {
                            state.credit.bootWindowStartVerifiedMillis
                        },
                        bootsInWindow = if (windowExpired) 1 else state.credit.bootsInWindow + 1,
                        consumedForBootPresent = false,
                        consumedForBoot = 0L,
                    ),
                    gapProvenance = if (anomalous) {
                        GapProvenance.BOOT_VELOCITY_ANOMALY
                    } else {
                        state.gapProvenance
                    },
                )
            }

            ContinuityEventType.QUARANTINE_ENTERED ->
                state.copy(identity = state.identity.copy(quarantined = true))

            ContinuityEventType.GAP_PROVENANCE_LABELLED -> state.copy(
                gapProvenance = GapProvenance.fromOrdinal(event.operandA.toInt()),
            )

            ContinuityEventType.PASSIVE_DEVELOPMENT_APPLIED -> {
                val millis = requireNonNegative(event.operandA, "passive development")
                if (millis > ContinuityContract.MODE_C_MAX_PASSIVE_DEVELOPMENT_MILLIS) {
                    throw IllegalStateException(
                        "passive development of $millis exceeds the per-absence cap " +
                            "${ContinuityContract.MODE_C_MAX_PASSIVE_DEVELOPMENT_MILLIS}",
                    )
                }
                ctx.forVariable(VAR_DEVELOPMENTAL_PROGRESS)
                state.copy(
                    developmentalProgress = FixedPoint.satAdd(
                        state.developmentalProgress,
                        ContinuityMath.scaleByDuration(
                            ContinuityContract.FIXTURE_PASSIVE_DEVELOPMENT_PER_MINUTE,
                            millis,
                        ),
                        ctx,
                    ),
                )
            }
        }

        return advanced.copy(lastCommitSequence = event.logicalEventId)
    }

    /** Folds an ordered event sequence. Order is the caller's contract, not a hint. */
    public fun reduceAll(
        state: ContinuityState,
        events: List<ContinuityEvent>,
        ctx: ArithmeticContext,
    ): ContinuityState {
        var current = state
        for (event in events) current = reduce(current, event, ctx)
        return current
    }

    /**
     * Applies `baselineEquivMillis` of metabolism to both neutral reserves.
     *
     * Clamping at zero rather than saturating below it is deliberate: a reserve
     * is a fraction of capacity, and a negative fraction is not a smaller value
     * but a meaningless one.
     */
    private fun applyDrain(
        state: ContinuityState,
        baselineEquivMillis: Long,
        ctx: ArithmeticContext,
    ): ContinuityState {
        val drain = ContinuityMath.scaleByDuration(
            ContinuityContract.FIXTURE_RESERVE_DRAIN_PER_MINUTE,
            baselineEquivMillis,
        )
        ctx.forVariable(VAR_RESERVE_A)
        val nextA = FixedPoint.clamp(
            FixedPoint.satSubtract(state.reserveA, drain, ctx),
            0L,
            FixedPoint.ONE,
            ctx,
        )
        ctx.forVariable(VAR_RESERVE_B)
        val nextB = FixedPoint.clamp(
            FixedPoint.satSubtract(state.reserveB, drain, ctx),
            0L,
            FixedPoint.ONE,
            ctx,
        )
        return state.copy(reserveA = nextA, reserveB = nextB)
    }

    private fun requireNonNegative(value: Long, what: String): Long {
        if (value < 0L) {
            throw IllegalArgumentException("$what must not be negative, got $value")
        }
        return value
    }
}
