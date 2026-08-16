package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.agentic.AdversarialAuditContract
import com.animusmachinae.dll17.research.aliveness.agentic.AgenticRole
import com.animusmachinae.dll17.research.aliveness.agentic.AuditorAuthorityPolicy
import com.animusmachinae.dll17.research.aliveness.agentic.AuditorFinding
import com.animusmachinae.dll17.research.aliveness.agentic.DispositionedFinding
import com.animusmachinae.dll17.research.aliveness.agentic.FindingDisposition
import com.animusmachinae.dll17.research.aliveness.agentic.ViolationCode
import com.animusmachinae.dll17.research.aliveness.agentic.sha256

/**
 * `A001GateAdjudicatorV1` — D016-I.
 *
 * The gate authority A001 has never actually had.
 *
 * Until now the plan was for the gate outcome to be a judgement: first three
 * named humans, then, from D016-B, two isolated language models. D016-H measured
 * the second of those against thresholds frozen before any model had run, and it
 * failed all seven — most seriously, it obeyed an instruction embedded in the
 * material it was reviewing in one trial of four. The architect's disposition was
 * that this is a fact about using a judge at all, not about which judge, and that
 * continuing to shop for a better one would be programme drift.
 *
 * So the outcome is computed here instead, and this file is a pure function.
 * Given the same [GateEvidence] it returns the same [GateRuling], byte for byte,
 * with no clock, no randomness, no network, no environment and no model in the
 * path. Everything it decides was preregistered: the thresholds come from
 * [A001StudyContract], the screening and the statistics from [A001Analysis], the
 * powering from [A001FeasibilityBudget], the attempt budget from [SpikeContract].
 * This file adds no new threshold and relaxes none.
 *
 * ## What agents can and cannot do here
 *
 * Auditor findings are accepted, and they are the last thing consulted. The
 * outcome is computed first, from the evidence; only afterwards is each finding
 * given a disposition. A finding naming a machine-checkable [ViolationCode] is
 * checked *by this file against the evidence*, never taken on the auditor's word,
 * and every code is checked on every run whether or not an auditor mentioned it —
 * so a correct finding is already reflected in the outcome before it is read, and
 * an invented one is inert.
 *
 * The single exception is the one the architect asked for: a concern an auditor
 * could not express in the frozen vocabulary suspends an otherwise-passing gate
 * as [GateOutcome.A001_PASS_PENDING_ARCHITECT_REVIEW]. It cannot manufacture a
 * failure, and it cannot touch a gate that is already blocked or failed. Agents
 * can stop; they cannot start, rescue or overturn.
 */
public object A001GateAdjudicator {

    public const val ADJUDICATOR_ID: String = "A001GateAdjudicatorV1"
    public const val ADJUDICATOR_VERSION: Int = 1
    /** D016-J owner-delegated determination; this is not an IRB approval. */
    public const val ETHICS_DETERMINATION_ID: String =
        "D016-J-OWNER-DELEGATED-APPROVED-WITH-CONDITIONS"

    // ------------------------------------------------------------------ inputs

    /**
     * The independent baseline qualification result.
     *
     * A strong comparator is the whole load-bearing structure of A001: the
     * programme's claim is not that people liked the organism but that they rated
     * it above something that was itself independently shown to be good. A weak
     * baseline would make the primary endpoint trivially passable, which is why
     * this is a prerequisite rather than a covariate.
     */
    public class BaselineQualification(
        public val instrumentId: String,
        public val participants: Int,
        public val meanMargin: Double,
        public val marginCiLow: Double,
    )

    /** One scored attempt's declared identity and its human data. */
    public class AttemptRecord(
        public val attemptNumber: Int,
        public val protocolId: String,
        public val protocolVersion: Int,
        public val instrumentId: String,
        public val analysisVersion: Int,
        public val records: List<A001Analysis.PairRecord>,
        /**
         * What the submitter says the outcome was, if anything.
         *
         * Never trusted, and present only so that a disagreement with the
         * recomputation becomes a detectable violation rather than an argument.
         */
        public val claimedOutcome: String? = null,
    )

    /**
     * Everything the gate is allowed to consider.
     *
     * There is no field here for a reviewer verdict, a model ruling, a
     * recommendation or an override, because there is no point in the computation
     * at which one could be consumed.
     */
    public class GateEvidence(
        public val baseline: BaselineQualification?,
        public val pilot: BlindVariancePilot.PilotRelease?,
        public val ceiling: A001FeasibilityBudget.OwnerCeiling?,
        /** Identifier of the independent human-subjects determination, if obtained. */
        public val ethicsDeterminationId: String?,
        public val attempts: List<AttemptRecord>,
        public val auditorFindings: List<AuditorFinding> = emptyList(),
    ) {
        /**
         * A canonical, order-stable serialization of everything that can affect
         * the outcome.
         *
         * This is what makes the adjudication replayable rather than merely
         * repeatable: two parties holding the same evidence can each compute this
         * string, compare hashes, and know they adjudicated the same thing. Pair
         * records are sorted, so a data set that arrives in a different order
         * hashes identically and — as `A001GateAdjudicatorTest` asserts — decides
         * identically.
         *
         * Auditor findings are deliberately excluded from the hash. They are not
         * evidence about the organism, they cannot change the computed outcome,
         * and including them would make an identical study hash differently
         * depending on what a model happened to say about it.
         */
        public fun canonicalForm(): String = buildString {
            append(ADJUDICATOR_ID).append(" v").append(ADJUDICATOR_VERSION).append('\n')
            append("baseline=")
            if (baseline == null) append("ABSENT\n") else {
                append(baseline.instrumentId)
                append(" n=").append(baseline.participants)
                append(" margin=").append(Statistics.d6(baseline.meanMargin))
                append(" ciLow=").append(Statistics.d6(baseline.marginCiLow)).append('\n')
            }
            append("pilot=")
            if (pilot == null) append("ABSENT\n") else {
                append("sd=").append(Statistics.d6(pilot.pairedDifferenceSd))
                append(" valid=").append(pilot.protocolValid).append('\n')
            }
            append("ceiling=")
            if (ceiling == null) append("ABSENT\n") else {
                append("participants=").append(ceiling.maxFundableParticipants)
                append(" hours=").append(Statistics.d6(ceiling.maxParticipantHours)).append('\n')
            }
            append("ethics=").append(ethicsDeterminationId ?: "ABSENT").append('\n')
            for (attempt in attempts.sortedBy { it.attemptNumber }) {
                append("attempt=").append(attempt.attemptNumber)
                append(" protocol=").append(attempt.protocolId)
                append(" v").append(attempt.protocolVersion)
                append(" instrument=").append(attempt.instrumentId)
                append(" analysisVersion=").append(attempt.analysisVersion)
                append(" claimed=").append(attempt.claimedOutcome ?: "NONE").append('\n')
                val sorted = attempt.records.sortedWith(
                    compareBy({ it.comparator.cohortId }, { it.participantId }),
                )
                for (r in sorted) {
                    append("  ").append(r.participantId)
                    append(' ').append(r.comparator.cohortId)
                    append(" full=").append(r.fullScore?.let { Statistics.d6(it) } ?: "NA")
                    append(" comp=").append(r.comparatorScore?.let { Statistics.d6(it) } ?: "NA")
                    append(" cf=").append(Statistics.d6(r.fullCompletedFraction))
                    append('/').append(Statistics.d6(r.comparatorCompletedFraction))
                    append(" tech=").append(r.technicalFailure)
                    append(" prior=").append(r.priorNonScoredPool)
                    append(" fc=").append(r.forcedChoicePreferredFull?.toString() ?: "NA")
                    append('\n')
                }
            }
        }

        /** Identifies the adjudicated evidence set in the ruling and in provenance. */
        public fun evidenceHash(): String = sha256(canonicalForm())
    }

    // -------------------------------------------------- frozen threshold guard

    /**
     * A second, independent copy of every frozen decision threshold.
     *
     * This exists to be redundant. The values are read from [A001StudyContract]
     * at every other point in the pipeline; here they are written out again as
     * literals so that editing the contract silently no longer silently works.
     * Any divergence raises [ViolationCode.THRESHOLD_WEAKENED_AFTER_FREEZE], and
     * the gate cannot pass while it diverges — including in the direction that
     * would make passing easier, which is the direction that matters.
     *
     * Two copies of a constant is normally a defect. It is the point here: the
     * failure mode being defended against is a single well-intentioned edit, and
     * a duplicate that must be edited in step is exactly what catches one.
     */
    public val FROZEN_DECISION_THRESHOLDS: List<Triple<String, Double, Double>> = listOf(
        Triple("minimumPairedDifference", A001StudyContract.MINIMUM_PAIRED_DIFFERENCE, 10.0),
        Triple("confidenceLevel", A001StudyContract.CONFIDENCE_LEVEL, 0.95),
        Triple("primaryAlpha", A001StudyContract.PRIMARY_ALPHA, 0.05),
        Triple("primaryPower", A001StudyContract.PRIMARY_POWER, 0.80),
        Triple("pilotSdInflation", A001StudyContract.PILOT_SD_INFLATION, 1.25),
        Triple(
            "minimumCompletedSessionFraction",
            A001StudyContract.MINIMUM_COMPLETED_SESSION_FRACTION,
            0.90,
        ),
        Triple(
            "baselineCompetenceMargin",
            A001StudyContract.BASELINE_COMPETENCE_MARGIN,
            15.0,
        ),
        Triple("ablationFamilyAlpha", A001StudyContract.ABLATION_FAMILY_ALPHA, 0.05),
        Triple(
            "variancePilotParticipants",
            A001StudyContract.VARIANCE_PILOT_PARTICIPANTS.toDouble(),
            36.0,
        ),
        Triple(
            "baselineQualificationParticipants",
            A001StudyContract.BASELINE_QUALIFICATION_PARTICIPANTS.toDouble(),
            40.0,
        ),
        Triple(
            "maxScoredAttempts",
            SpikeContract.MAX_SCORED_A001_ATTEMPTS.toDouble(),
            3.0,
        ),
    )

    public fun driftedThresholds(): List<String> =
        FROZEN_DECISION_THRESHOLDS.filter { it.second != it.third }
            .map { "${it.first} is ${Statistics.d6(it.second)}, frozen at ${Statistics.d6(it.third)}" }

    // ----------------------------------------------------------------- outcome

    public enum class GateOutcome {
        /** A prerequisite is missing. No attempt may be scored. */
        A001_BLOCKED,

        /** Every prerequisite met, the attempt cleared the frozen rule. */
        A001_PASS,

        /**
         * The attempt cleared the frozen rule, and an auditor raised a concern it
         * could not express in the frozen vocabulary. Suspended for the Architect.
         */
        A001_PASS_PENDING_ARCHITECT_REVIEW,

        /** The attempt failed and the programme has attempts remaining. */
        A001_ATTEMPT_FAILED_ATTEMPTS_REMAIN,

        /** The attempt failed and the three-attempt budget is exhausted. */
        ALIVENESS_PROGRAM_STOP,
    }

    /** One prerequisite, checked. */
    public class StageResult(
        public val stageId: String,
        public val requirement: String,
        public val satisfied: Boolean,
        public val detail: String,
        /** The blocking token contributed when unsatisfied. */
        public val blockingState: String? = null,
    )

    public class GateRuling(
        public val outcome: GateOutcome,
        /** The outcome as computed from evidence alone, before any finding was read. */
        public val computedOutcome: GateOutcome,
        public val evidenceHash: String,
        public val stages: List<StageResult>,
        public val violations: List<ViolationCode>,
        public val attemptsConsumed: Int,
        public val attemptsRemaining: Int,
        public val analysis: A001Analysis.AttemptResult?,
        public val feasibility: A001FeasibilityBudget.Result,
        public val dispositions: List<DispositionedFinding>,
    ) {
        public val blockers: List<String>
            get() = stages.filterNot { it.satisfied }.mapNotNull { it.blockingState }.distinct()

        /** True only for the two outcomes that permit the programme to proceed. */
        public val passed: Boolean
            get() = outcome == GateOutcome.A001_PASS ||
                outcome == GateOutcome.A001_PASS_PENDING_ARCHITECT_REVIEW

        public fun render(): String = buildString {
            append("================================================================\n")
            append("A001 GATE ADJUDICATION (").append(ADJUDICATOR_ID)
            append(" v").append(ADJUDICATOR_VERSION).append(")\n\n")
            append("  evidenceHash=").append(evidenceHash).append('\n')
            append("  deterministic: no clock, no randomness, no network, no model\n\n")

            append("  PREREQUISITE STAGES\n")
            for (stage in stages) {
                append("    ").append(if (stage.satisfied) "SATISFIED " else "BLOCKED   ")
                append(stage.stageId).append('\n')
                append("      requirement: ").append(stage.requirement).append('\n')
                append("      detail:      ").append(stage.detail).append('\n')
                stage.blockingState?.takeIf { !stage.satisfied }?.let {
                    append("      blocks:      ").append(it).append('\n')
                }
            }
            append('\n')
            append("  VIOLATIONS INDEPENDENTLY DERIVED FROM EVIDENCE\n")
            if (violations.isEmpty()) {
                append("    none\n")
            } else {
                for (v in violations) append("    ").append(v.name).append('\n')
            }
            append('\n')

            analysis?.let {
                append("  RECOMPUTED ATTEMPT ANALYSIS\n\n")
                append(it.render().prependIndent("    ")).append('\n')
            } ?: append("  RECOMPUTED ATTEMPT ANALYSIS: none — no attempt was adjudicable\n\n")

            append("  AUDITOR FINDINGS\n")
            if (dispositions.isEmpty()) {
                append("    none submitted\n")
            } else {
                for (d in dispositions) {
                    append("    ").append(d.disposition.name.padEnd(34))
                    append(d.finding.describe()).append('\n')
                    append("      claim:  ").append(d.finding.claim).append('\n')
                    append("      result: ").append(d.detail).append('\n')
                }
            }
            append('\n')
            append("  findingsSubmitted=").append(dispositions.size)
            append(" upheld=").append(
                dispositions.count { it.disposition == FindingDisposition.UPHELD_BLOCKING },
            )
            append(" notConfirmed=").append(
                dispositions.count { it.disposition == FindingDisposition.NOT_CONFIRMED },
            )
            append(" returnedToArchitect=").append(
                dispositions.count {
                    it.disposition == FindingDisposition.AMBIGUOUS_RETURNED_TO_ARCHITECT
                },
            ).append('\n')
            append("  outcomeBeforeFindings=").append(computedOutcome.name).append('\n')
            append("  outcomeAfterFindings=").append(outcome.name).append('\n')
            append("  findingsChangedTheOutcome=").append(outcome != computedOutcome)
            if (outcome != computedOutcome) {
                append(" — an ambiguous finding suspended a pass for the Architect,\n")
                append("    which is the only effect an auditor is permitted to have and is\n")
                append("    available in one direction only\n")
            } else {
                append(" — an upheld finding restates a violation already\n")
                append("    derived above, and an unconfirmed one is inert\n")
            }
            append('\n')

            append("  attemptsConsumed=").append(attemptsConsumed)
            append(" attemptsRemaining=").append(attemptsRemaining)
            append(" budget=").append(SpikeContract.MAX_SCORED_A001_ATTEMPTS).append('\n')
            for (b in blockers) append("  blocker: ").append(b).append('\n')
            append("  A001_GATE_OUTCOME=").append(outcome.name).append('\n')
        }
    }

    // ------------------------------------------------------------- violations

    /**
     * Every machine-checkable violation, tested against the evidence.
     *
     * Every code is evaluated on every run. That is what makes an auditor
     * unnecessary to correctness: the adjudicator is not waiting to be told where
     * to look, and a finding can only ever agree or disagree with a check that
     * already ran.
     */
    public fun deriveViolations(evidence: GateEvidence): List<ViolationCode> {
        val found = LinkedHashSet<ViolationCode>()
        val attempt = evidence.attempts.maxByOrNull { it.attemptNumber }

        if (evidence.baseline == null) {
            found += ViolationCode.BASELINE_NOT_INDEPENDENTLY_QUALIFIED
        } else {
            if (evidence.baseline.participants <
                A001StudyContract.BASELINE_QUALIFICATION_PARTICIPANTS ||
                evidence.baseline.instrumentId != A001StudyContract.BASELINE_INSTRUMENT_ID
            ) {
                found += ViolationCode.BASELINE_NOT_INDEPENDENTLY_QUALIFIED
            }
            if (evidence.baseline.meanMargin < A001StudyContract.BASELINE_COMPETENCE_MARGIN ||
                evidence.baseline.marginCiLow <= 0.0
            ) {
                found += ViolationCode.BASELINE_MARGIN_BELOW_FLOOR
            }
        }

        if (evidence.pilot == null || !evidence.pilot.protocolValid) {
            found += ViolationCode.PILOT_NOT_PROTOCOL_VALID
        }

        val feasibility = A001FeasibilityBudget.compute(evidence.pilot, evidence.ceiling)
        if (feasibility.state != A001FeasibilityBudget.FeasibilityState.A001_FEASIBLE) {
            found += ViolationCode.FEASIBILITY_NOT_ESTABLISHED
        }

        if (evidence.ethicsDeterminationId.isNullOrBlank()) {
            found += ViolationCode.ETHICS_DETERMINATION_ABSENT
        }

        if (driftedThresholds().isNotEmpty()) {
            found += ViolationCode.THRESHOLD_WEAKENED_AFTER_FREEZE
        }

        if (evidence.attempts.size > SpikeContract.MAX_SCORED_A001_ATTEMPTS ||
            evidence.attempts.any { it.attemptNumber > SpikeContract.MAX_SCORED_A001_ATTEMPTS } ||
            evidence.attempts.map { it.attemptNumber }.distinct().size != evidence.attempts.size
        ) {
            found += ViolationCode.ATTEMPT_BUDGET_EXCEEDED
        }

        if (attempt != null) {
            if (attempt.protocolId != A001StudyContract.PROTOCOL_ID ||
                attempt.protocolVersion != A001StudyContract.PROTOCOL_VERSION
            ) {
                found += ViolationCode.PROTOCOL_VERSION_MISMATCH
            }
            if (attempt.instrumentId != A001StudyContract.INSTRUMENT_ID) {
                found += ViolationCode.INSTRUMENT_MISMATCH
            }
            if (attempt.analysisVersion != A001Analysis.ANALYSIS_VERSION) {
                found += ViolationCode.ANALYSIS_VERSION_MISMATCH
            }

            // The screening rules are reapplied here rather than trusted. A pair
            // that should have been excluded but reached the analysed set is the
            // single most consequential thing an auditor could be pointed at, and
            // it is exactly the thing an auditor's word must not settle.
            val screened = A001Analysis.screen(attempt.records)
            for (record in screened.included) {
                if (record.priorNonScoredPool) {
                    found += ViolationCode.NON_SCORED_POOL_PARTICIPANT_ANALYSED
                }
                if (record.fullCompletedFraction <
                    A001StudyContract.MINIMUM_COMPLETED_SESSION_FRACTION ||
                    record.comparatorCompletedFraction <
                    A001StudyContract.MINIMUM_COMPLETED_SESSION_FRACTION
                ) {
                    found += ViolationCode.INCOMPLETE_SESSION_ANALYSED
                }
                val full = record.fullScore
                val comp = record.comparatorScore
                if (full == null || comp == null ||
                    full < A001StudyContract.SCORE_MIN || full > A001StudyContract.SCORE_MAX ||
                    comp < A001StudyContract.SCORE_MIN || comp > A001StudyContract.SCORE_MAX
                ) {
                    found += ViolationCode.SCORE_OUT_OF_RANGE_ANALYSED
                }
            }
            val keys = screened.included.map { it.participantId + "/" + it.comparator.cohortId }
            if (keys.size != keys.distinct().size) {
                found += ViolationCode.DUPLICATE_PARTICIPANT_ANALYSED
            }

            val primaryPairs = screened.included.count {
                it.comparator == Cohort.SCRIPTED_PET_BASELINE
            }
            feasibility.requiredPairsPrimary?.let { required ->
                if (primaryPairs < required) found += ViolationCode.SAMPLE_BELOW_POWERED_REQUIREMENT
            }

            if (attempt.claimedOutcome != null && primaryPairs >= 2) {
                val recomputed = A001Analysis.analyze(attempt.records).attemptOutcome
                if (attempt.claimedOutcome != recomputed) {
                    found += ViolationCode.CLAIMED_OUTCOME_DISAGREES_WITH_RECOMPUTATION
                }
            }
        }

        // Stable, declaration-ordered, so the rendered evidence is diffable.
        return ViolationCode.entries.filter { it in found }
    }

    // ------------------------------------------------------------ adjudication

    /**
     * The whole gate, computed.
     *
     * Order is fixed and meaningful: prerequisites first, then the attempt, then
     * the attempt budget, then — last, and only ever to suspend — the auditors.
     */
    public fun adjudicate(evidence: GateEvidence): GateRuling {
        val violations = deriveViolations(evidence)
        val feasibility = A001FeasibilityBudget.compute(evidence.pilot, evidence.ceiling)
        val attempt = evidence.attempts.maxByOrNull { it.attemptNumber }

        val stages = ArrayList<StageResult>()

        stages += StageResult(
            "AJ-01",
            "Every frozen A001 decision threshold still holds its frozen value",
            ViolationCode.THRESHOLD_WEAKENED_AFTER_FREEZE !in violations,
            if (driftedThresholds().isEmpty()) {
                "all ${FROZEN_DECISION_THRESHOLDS.size} thresholds match the values frozen " +
                    "before any human data existed"
            } else {
                "drifted: " + driftedThresholds().joinToString("; ")
            },
            blockingState = "BLOCKED_FROZEN_THRESHOLD_DRIFT",
        )

        stages += StageResult(
            "AJ-02",
            "A strong scripted baseline is independently qualified",
            ViolationCode.BASELINE_NOT_INDEPENDENTLY_QUALIFIED !in violations &&
                ViolationCode.BASELINE_MARGIN_BELOW_FLOOR !in violations,
            evidence.baseline?.let {
                "instrument=${it.instrumentId} n=${it.participants} " +
                    "margin=${Statistics.d3(it.meanMargin)} ciLow=${Statistics.d3(it.marginCiLow)} " +
                    "required n>=${A001StudyContract.BASELINE_QUALIFICATION_PARTICIPANTS} " +
                    "margin>=${Statistics.d3(A001StudyContract.BASELINE_COMPETENCE_MARGIN)} ciLow>0"
            } ?: "no baseline qualification evidence exists",
            blockingState = "BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED",
        )

        stages += StageResult(
            "AJ-03",
            "BlindVariancePilotV1 released a protocol-valid result",
            ViolationCode.PILOT_NOT_PROTOCOL_VALID !in violations,
            evidence.pilot?.let {
                "protocolValid=${it.protocolValid} sd=${Statistics.d6(it.pairedDifferenceSd)}; " +
                    "direction, means and per-participant differences are sealed and are not " +
                    "inputs here"
            } ?: "no pilot release exists",
            blockingState = "BLOCKED_VARIANCE_PILOT_NOT_REGISTERED",
        )

        stages += StageResult(
            "AJ-04",
            "The powered requirement is computable and fits the frozen owner ceiling",
            ViolationCode.FEASIBILITY_NOT_ESTABLISHED !in violations,
            "state=${feasibility.state.name} " +
                "requiredPairsPrimary=${feasibility.requiredPairsPrimary ?: "UNAVAILABLE"} " +
                "totalParticipants=${feasibility.totalRequiredParticipants ?: "UNAVAILABLE"}",
            blockingState = when (feasibility.state) {
                A001FeasibilityBudget.FeasibilityState.BLOCKED_SPEC_PAIRED_DIFFERENCE_SD ->
                    "BLOCKED_SPEC_PAIRED_DIFFERENCE_SD"
                A001FeasibilityBudget.FeasibilityState.BLOCKED_SPEC_STUDY_BUDGET ->
                    "BLOCKED_SPEC_STUDY_BUDGET"
                else -> "A001_NOT_FEASIBLE"
            },
        )

        stages += StageResult(
            "AJ-05",
            "An independent human-subjects determination exists",
            ViolationCode.ETHICS_DETERMINATION_ABSENT !in violations,
            evidence.ethicsDeterminationId?.takeIf { it.isNotBlank() }
                ?.let { "determination=$it" }
                ?: "no independent determination exists; the programme may not " +
                "self-determine exemption and no agent may issue one",
            blockingState = "BLOCKED_ETHICS_DETERMINATION_ABSENT",
        )

        stages += StageResult(
            "AJ-06",
            "The scored attempt declares the frozen protocol, instrument and analysis",
            attempt != null &&
                ViolationCode.PROTOCOL_VERSION_MISMATCH !in violations &&
                ViolationCode.INSTRUMENT_MISMATCH !in violations &&
                ViolationCode.ANALYSIS_VERSION_MISMATCH !in violations,
            attempt?.let {
                "protocol=${it.protocolId} v${it.protocolVersion} instrument=${it.instrumentId} " +
                    "analysisVersion=${it.analysisVersion}"
            } ?: "no scored attempt exists",
            blockingState = if (attempt == null) {
                "BLOCKED_NO_SCORED_ATTEMPT"
            } else {
                "BLOCKED_ATTEMPT_DECLARES_UNFROZEN_INSTRUMENTS"
            },
        )

        stages += StageResult(
            "AJ-07",
            "The analysed set obeys the preregistered exclusion rules",
            ViolationCode.NON_SCORED_POOL_PARTICIPANT_ANALYSED !in violations &&
                ViolationCode.DUPLICATE_PARTICIPANT_ANALYSED !in violations &&
                ViolationCode.INCOMPLETE_SESSION_ANALYSED !in violations &&
                ViolationCode.SCORE_OUT_OF_RANGE_ANALYSED !in violations,
            if (attempt == null) {
                "no scored attempt exists"
            } else {
                val screened = A001Analysis.screen(attempt.records)
                "screening reapplied here rather than trusted: included=" +
                    "${screened.included.size} excluded=${screened.excluded.size}"
            },
            blockingState = "BLOCKED_EXCLUSION_RULES_VIOLATED",
        )

        stages += StageResult(
            "AJ-08",
            "The attempt meets the powered sample the pilot implies",
            ViolationCode.SAMPLE_BELOW_POWERED_REQUIREMENT !in violations,
            if (attempt == null) "no scored attempt exists" else {
                val n = A001Analysis.screen(attempt.records).included
                    .count { it.comparator == Cohort.SCRIPTED_PET_BASELINE }
                "analysedPrimaryPairs=$n required=" +
                    "${feasibility.requiredPairsPrimary ?: "UNAVAILABLE"}; an underpowered " +
                    "attempt still consumes one of the three, so it is refused rather than run"
            },
            blockingState = "BLOCKED_ATTEMPT_UNDERPOWERED",
        )

        stages += StageResult(
            "AJ-09",
            "The scored attempts fit the three-attempt programme budget",
            ViolationCode.ATTEMPT_BUDGET_EXCEEDED !in violations,
            "attempts=${evidence.attempts.size} numbers=" +
                evidence.attempts.map { it.attemptNumber }.sorted().joinToString(",")
                    .ifEmpty { "none" } +
                " budget=${SpikeContract.MAX_SCORED_A001_ATTEMPTS}",
            blockingState = "BLOCKED_ATTEMPT_BUDGET_EXCEEDED",
        )

        stages += StageResult(
            "AJ-10",
            "The submitted outcome claim, if any, agrees with the recomputation",
            ViolationCode.CLAIMED_OUTCOME_DISAGREES_WITH_RECOMPUTATION !in violations,
            attempt?.claimedOutcome?.let { "claimed=$it" }
                ?: "no outcome was claimed; the computation stands alone",
            blockingState = "BLOCKED_CLAIMED_OUTCOME_DISAGREES",
        )

        val prerequisitesMet = stages.all { it.satisfied }
        val attemptsConsumed = evidence.attempts.size
        val attemptsRemaining =
            StrictMath.max(0, SpikeContract.MAX_SCORED_A001_ATTEMPTS - attemptsConsumed)

        val analysis = if (prerequisitesMet && attempt != null) {
            A001Analysis.analyze(attempt.records)
        } else {
            null
        }

        // The outcome, computed before a single finding is read.
        val computed = when {
            !prerequisitesMet -> GateOutcome.A001_BLOCKED
            analysis == null -> GateOutcome.A001_BLOCKED
            analysis.classification == A001Analysis.PrimaryClassification.PASS ->
                GateOutcome.A001_PASS
            attemptsRemaining > 0 -> GateOutcome.A001_ATTEMPT_FAILED_ATTEMPTS_REMAIN
            else -> GateOutcome.ALIVENESS_PROGRAM_STOP
        }

        val dispositions = evidence.auditorFindings.map { finding ->
            val code = finding.violationCode
            when {
                code == null -> DispositionedFinding(
                    finding,
                    FindingDisposition.AMBIGUOUS_RETURNED_TO_ARCHITECT,
                    "no machine-checkable violation code; returned to the Architect rather " +
                        "than scored, and suspends a pass rather than creating a failure",
                )

                code in violations -> DispositionedFinding(
                    finding,
                    FindingDisposition.UPHELD_BLOCKING,
                    "independently re-derived from the evidence by ${code.name}'s own check; " +
                        "already reflected in the outcome before this finding was read",
                )

                else -> DispositionedFinding(
                    finding,
                    FindingDisposition.NOT_CONFIRMED,
                    "checked against the evidence and not present; inert, and preserved only " +
                        "as a record of what the auditor claimed",
                )
            }
        }

        // The one permitted agent effect, and it only ever subtracts. An ambiguous
        // finding suspends a pass; it cannot touch a block or a failure, and there
        // is no branch here that improves an outcome.
        val ambiguous = dispositions.any {
            it.disposition == FindingDisposition.AMBIGUOUS_RETURNED_TO_ARCHITECT
        }
        val outcome = if (computed == GateOutcome.A001_PASS && ambiguous) {
            GateOutcome.A001_PASS_PENDING_ARCHITECT_REVIEW
        } else {
            computed
        }

        return GateRuling(
            outcome = outcome,
            computedOutcome = computed,
            evidenceHash = evidence.evidenceHash(),
            stages = stages,
            violations = violations,
            attemptsConsumed = attemptsConsumed,
            attemptsRemaining = attemptsRemaining,
            analysis = analysis,
            feasibility = feasibility,
            dispositions = dispositions,
        )
    }

    /**
     * The programme's actual position: no evidence at all.
     *
     * Constructed from nothing rather than from placeholders, so the gate's real
     * current answer is computed by the same function that will compute the real
     * one, rather than asserted in prose beside it.
     */
    public fun currentEvidence(): GateEvidence = GateEvidence(
        baseline = null,
        pilot = null,
        ceiling = A001FeasibilityBudget.FROZEN_OWNER_CEILING,
        ethicsDeterminationId = ETHICS_DETERMINATION_ID,
        attempts = emptyList(),
        auditorFindings = emptyList(),
    )

    /**
     * A fully-populated synthetic evidence set, used only to exercise replay.
     *
     * It has to be complete, because the interesting replay property is about a
     * gate that actually reaches an outcome; the programme's real evidence is
     * empty and would prove only that nothing replays as nothing. Every value is
     * synthetic and none of it is human data.
     */
    public fun replayFixture(): GateEvidence {
        val n = 60
        val raw = DoubleArray(n) { Statistics.normalQuantile((it + 0.5) / n) }
        val rawSd = Statistics.sampleSd(raw)
        fun arm(prefix: String, comparator: Cohort, mean: Double) = (0 until n).map { i ->
            val d = mean + 18.0 * raw[i] / rawSd
            A001Analysis.PairRecord(
                participantId = "$prefix-${(i + 1).toString().padStart(3, '0')}",
                comparator = comparator,
                fullScore = StrictMath.min(100.0, StrictMath.max(0.0, 50.0 + d / 2.0)),
                comparatorScore = StrictMath.min(100.0, StrictMath.max(0.0, 50.0 - d / 2.0)),
                forcedChoicePreferredFull = d > 0.0,
            )
        }
        val records = arm("RPL", Cohort.SCRIPTED_PET_BASELINE, 16.0) +
            A001StudyContract.ABLATION_FAMILY.mapIndexed { index, cohort ->
                arm("RPL-A${index + 1}", cohort, 12.0 - index * 5.0)
            }.flatten()
        return GateEvidence(
            baseline = BaselineQualification(
                A001StudyContract.BASELINE_INSTRUMENT_ID,
                A001StudyContract.BASELINE_QUALIFICATION_PARTICIPANTS,
                18.0,
                6.0,
            ),
            pilot = BlindVariancePilot.release(
                (0 until A001StudyContract.VARIANCE_PILOT_PARTICIPANTS).map { i ->
                    val q = Statistics.normalQuantile(
                        (i + 0.5) / A001StudyContract.VARIANCE_PILOT_PARTICIPANTS,
                    )
                    BlindVariancePilot.PilotRecord("RPL-PILOT-$i", 50.0 + q * 5.0, 50.0 - q * 5.0)
                },
            ),
            ceiling = A001FeasibilityBudget.FROZEN_OWNER_CEILING,
            ethicsDeterminationId = "SYNTHETIC-DETERMINATION",
            attempts = listOf(
                AttemptRecord(
                    1,
                    A001StudyContract.PROTOCOL_ID,
                    A001StudyContract.PROTOCOL_VERSION,
                    A001StudyContract.INSTRUMENT_ID,
                    A001Analysis.ANALYSIS_VERSION,
                    records,
                ),
            ),
        )
    }

    /**
     * The replay property, checked rather than claimed.
     *
     * Two things must hold and they are different: the same evidence must produce
     * the same ruling, and the same evidence *presented in a different order* must
     * produce the same ruling. The second is the one that matters, because a
     * pipeline that quietly depends on record order is one whose result can be
     * moved by whoever exports the data.
     *
     * That is also the exact failure the D016-H reviewer exhibited — its verdict
     * moved when the same evidence was reordered, on 5 of 13 fixtures. This is the
     * property the deterministic adjudicator has and the judge did not.
     */
    public fun replaysIdentically(): Boolean {
        val fixture = replayFixture()
        val reordered = GateEvidence(
            baseline = fixture.baseline,
            pilot = fixture.pilot,
            ceiling = fixture.ceiling,
            ethicsDeterminationId = fixture.ethicsDeterminationId,
            attempts = fixture.attempts.map {
                AttemptRecord(
                    it.attemptNumber, it.protocolId, it.protocolVersion, it.instrumentId,
                    it.analysisVersion, it.records.reversed(), it.claimedOutcome,
                )
            },
            auditorFindings = fixture.auditorFindings,
        )
        val a = adjudicate(fixture).render()
        return a == adjudicate(fixture).render() &&
            a == adjudicate(reordered).render() &&
            fixture.evidenceHash() == reordered.evidenceHash() &&
            adjudicate(currentEvidence()).render() == adjudicate(currentEvidence()).render()
    }

    public fun render(): String = buildString {
        append("A001_GATE_ADJUDICATION=").append(ADJUDICATOR_ID).append('\n')
        append("DATA_CLASS=NO_HUMAN_DATA — the live adjudication below runs against the\n")
        append("           programme's actual evidence, which is empty. No participant has\n")
        append("           been recruited or scored.\n\n")
        append("authority=").append(AdversarialAuditContract.DIRECTIVE)
        append(": human evidence determines aliveness, frozen math determines\n")
        append("          PASS/FAIL, agents audit and do not adjudicate\n\n")
        append(AuditorAuthorityPolicy.render())

        append("================================================================\n")
        append("REPLAY\n\n")
        append("  replaysIdentically=").append(replaysIdentically()).append('\n')
        append("  checked three ways: the same evidence twice, the same evidence with its\n")
        append("  pair records reversed, and the programme's real (empty) evidence twice.\n")
        append("  Order-invariance is the property the D016-H judge did not have — its\n")
        append("  verdict moved under reordering on 5 of 13 fixtures.\n\n")

        append("LIVE ADJUDICATION — the programme's actual evidence\n\n")
        append(adjudicate(currentEvidence()).render())
        append('\n')

        append("================================================================\n")
        append("SYNTHETIC REPLAY FIXTURE   [SYNTHETIC — no human has rated anything]\n\n")
        append("  Shown because the live adjudication above is blocked before it reaches an\n")
        append("  outcome, and a gate that has only ever returned BLOCKED demonstrates\n")
        append("  nothing about how it decides. This fixture carries a complete synthetic\n")
        append("  evidence set through every stage to a computed outcome. It is engineering\n")
        append("  evidence about the adjudicator and is not evidence about the organism.\n\n")
        append(adjudicate(replayFixture()).render())
        append('\n')

        append("================================================================\n")
        append("AGENT AUTHORITY, DEMONSTRATED   [SYNTHETIC]\n\n")
        append("  The same passing fixture, with auditor findings attached. An invented\n")
        append("  violation is checked and discarded; a concern with no checkable code\n")
        append("  suspends the pass for the Architect. Neither can create, rescue or\n")
        append("  overturn an outcome.\n\n")
        val withFindings = replayFixture().let { base ->
            GateEvidence(
                base.baseline, base.pilot, base.ceiling, base.ethicsDeterminationId,
                base.attempts,
                listOf(
                    AuditorFinding(
                        "F-01", AgenticRole.ALTERNATE_AUDITOR,
                        ViolationCode.BASELINE_MARGIN_BELOW_FLOOR,
                        listOf("EV-BASELINE"),
                        "the baseline margin looks inadequate to me",
                    ),
                    AuditorFinding(
                        "F-02", AgenticRole.PRIMARY_AUDITOR, null,
                        listOf("EV-PRIMARY"),
                        "the effect is real but I am uneasy about the interaction length",
                    ),
                ),
            )
        }
        append(adjudicate(withFindings).render())
    }

    @JvmStatic
    public fun main(args: Array<String>) {
        println(render())
    }
}
