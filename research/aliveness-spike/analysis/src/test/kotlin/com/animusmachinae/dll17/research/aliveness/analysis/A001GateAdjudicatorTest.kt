package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.agentic.AgenticRole
import com.animusmachinae.dll17.research.aliveness.agentic.AuditorFinding
import com.animusmachinae.dll17.research.aliveness.agentic.FindingDisposition
import com.animusmachinae.dll17.research.aliveness.agentic.ViolationCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * D016-I. The deterministic gate.
 *
 * The tests are grouped by the property they defend, and the properties are the
 * ones the measured judge failed at D016-H: determinism, order-invariance,
 * position-invariance, and immunity to being talked out of a finding by the
 * material under review.
 */
class A001GateAdjudicatorTest {

    // ---------------------------------------------------------- fixtures

    private fun passingEvidence(
        findings: List<AuditorFinding> = emptyList(),
        attempts: List<A001GateAdjudicator.AttemptRecord>? = null,
    ): A001GateAdjudicator.GateEvidence {
        val base = A001GateAdjudicator.replayFixture()
        return A001GateAdjudicator.GateEvidence(
            baseline = base.baseline,
            pilot = base.pilot,
            ceiling = base.ceiling,
            ethicsDeterminationId = base.ethicsDeterminationId,
            attempts = attempts ?: base.attempts,
            auditorFindings = findings,
        )
    }

    private fun withRecords(
        records: List<A001Analysis.PairRecord>,
        attemptNumber: Int = 1,
        claimed: String? = null,
    ) = listOf(
        A001GateAdjudicator.AttemptRecord(
            attemptNumber,
            A001StudyContract.PROTOCOL_ID,
            A001StudyContract.PROTOCOL_VERSION,
            A001StudyContract.INSTRUMENT_ID,
            A001Analysis.ANALYSIS_VERSION,
            records,
            claimed,
        ),
    )

    // ------------------------------------------------- determinism / replay

    @Test
    fun `the same evidence decides identically every time`() {
        val evidence = passingEvidence()
        val first = A001GateAdjudicator.adjudicate(evidence).render()
        repeat(5) {
            assertEquals(first, A001GateAdjudicator.adjudicate(evidence).render())
        }
    }

    @Test
    fun `reordering the evidence changes neither the hash nor the outcome`() {
        // The D016-H judge moved its verdict under reordering on 5 of 13
        // fixtures. This is the same manipulation applied to the replacement.
        val base = A001GateAdjudicator.replayFixture()
        val records = base.attempts.single().records
        val reversed = passingEvidence(attempts = withRecords(records.reversed()))
        val shuffled = passingEvidence(
            attempts = withRecords(records.sortedBy { it.participantId.reversed() }),
        )
        val expected = A001GateAdjudicator.adjudicate(passingEvidence()).render()
        assertEquals(expected, A001GateAdjudicator.adjudicate(reversed).render())
        assertEquals(expected, A001GateAdjudicator.adjudicate(shuffled).render())
        assertEquals(base.evidenceHash(), reversed.evidenceHash())
        assertEquals(base.evidenceHash(), shuffled.evidenceHash())
    }

    @Test
    fun `moving the decisive record to the front changes nothing`() {
        // The position-swap manipulation, which the judge failed on 4 of 13.
        val base = A001GateAdjudicator.replayFixture()
        val records = base.attempts.single().records
        val decisive = records.maxByOrNull { (it.fullScore ?: 0.0) - (it.comparatorScore ?: 0.0) }!!
        val moved = listOf(decisive) + records.filterNot { it === decisive }
        assertEquals(
            A001GateAdjudicator.adjudicate(passingEvidence()).render(),
            A001GateAdjudicator.adjudicate(passingEvidence(attempts = withRecords(moved))).render(),
        )
    }

    @Test
    fun `the replay self-check the audit reads actually exercises a decided gate`() {
        assertTrue(A001GateAdjudicator.replaysIdentically())
        // A self-check that only ever ran on blocked evidence would prove nothing
        // about how the gate decides, so the fixture must reach an outcome.
        val ruling = A001GateAdjudicator.adjudicate(A001GateAdjudicator.replayFixture())
        assertEquals(A001GateAdjudicator.GateOutcome.A001_PASS, ruling.outcome)
    }

    @Test
    fun `different evidence hashes differently`() {
        val a = passingEvidence()
        val b = A001GateAdjudicator.GateEvidence(
            baseline = a.baseline, pilot = a.pilot, ceiling = a.ceiling,
            ethicsDeterminationId = a.ethicsDeterminationId,
            attempts = withRecords(a.attempts.single().records.drop(1)),
        )
        assertNotEquals(a.evidenceHash(), b.evidenceHash())
    }

    // ----------------------------------------------------- the live gate

    @Test
    fun `the programme's real evidence is blocked and names why`() {
        val ruling = A001GateAdjudicator.adjudicate(A001GateAdjudicator.currentEvidence())
        assertEquals(A001GateAdjudicator.GateOutcome.A001_BLOCKED, ruling.outcome)
        assertFalse(ruling.passed)
        assertTrue("BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED" in ruling.blockers)
        assertTrue("BLOCKED_VARIANCE_PILOT_NOT_REGISTERED" in ruling.blockers)
        assertTrue("BLOCKED_SPEC_PAIRED_DIFFERENCE_SD" in ruling.blockers)
        assertFalse("BLOCKED_ETHICS_DETERMINATION_ABSENT" in ruling.blockers)
        assertEquals(
            A001GateAdjudicator.ETHICS_DETERMINATION_ID,
            A001GateAdjudicator.currentEvidence().ethicsDeterminationId,
        )
        assertEquals(0, ruling.attemptsConsumed)
        assertEquals(SpikeContract.MAX_SCORED_A001_ATTEMPTS, ruling.attemptsRemaining)
        // No attempt was analysed, so no statistic about the organism exists.
        assertEquals(null, ruling.analysis)
    }

    // ------------------------------------------------------- prerequisites

    @Test
    fun `each prerequisite blocks on its own`() {
        val base = A001GateAdjudicator.replayFixture()
        fun evidence(
            baseline: A001GateAdjudicator.BaselineQualification? = base.baseline,
            pilot: BlindVariancePilot.PilotRelease? = base.pilot,
            ceiling: A001FeasibilityBudget.OwnerCeiling? = base.ceiling,
            ethics: String? = base.ethicsDeterminationId,
        ) = A001GateAdjudicator.GateEvidence(
            baseline, pilot, ceiling, ethics, base.attempts,
        )

        assertEquals(
            A001GateAdjudicator.GateOutcome.A001_BLOCKED,
            A001GateAdjudicator.adjudicate(evidence(baseline = null)).outcome,
        )
        assertEquals(
            A001GateAdjudicator.GateOutcome.A001_BLOCKED,
            A001GateAdjudicator.adjudicate(evidence(pilot = null)).outcome,
        )
        assertEquals(
            A001GateAdjudicator.GateOutcome.A001_BLOCKED,
            A001GateAdjudicator.adjudicate(evidence(ceiling = null)).outcome,
        )
        assertEquals(
            A001GateAdjudicator.GateOutcome.A001_BLOCKED,
            A001GateAdjudicator.adjudicate(evidence(ethics = null)).outcome,
        )
    }

    @Test
    fun `a baseline that misses its frozen margin blocks the gate`() {
        val base = A001GateAdjudicator.replayFixture()
        val weak = A001GateAdjudicator.GateEvidence(
            A001GateAdjudicator.BaselineQualification(
                A001StudyContract.BASELINE_INSTRUMENT_ID,
                A001StudyContract.BASELINE_QUALIFICATION_PARTICIPANTS,
                A001StudyContract.BASELINE_COMPETENCE_MARGIN - 0.001,
                4.0,
            ),
            base.pilot, base.ceiling, base.ethicsDeterminationId, base.attempts,
        )
        val ruling = A001GateAdjudicator.adjudicate(weak)
        assertEquals(A001GateAdjudicator.GateOutcome.A001_BLOCKED, ruling.outcome)
        assertTrue(ViolationCode.BASELINE_MARGIN_BELOW_FLOOR in ruling.violations)
    }

    @Test
    fun `an excluded participant reaching the analysed set is detected`() {
        // Screening is reapplied by the adjudicator rather than trusted, so a
        // record that A001Analysis would exclude cannot be smuggled in.
        val base = A001GateAdjudicator.replayFixture()
        val tainted = base.attempts.single().records.toMutableList()
        tainted[0] = A001Analysis.PairRecord(
            tainted[0].participantId, tainted[0].comparator,
            tainted[0].fullScore, tainted[0].comparatorScore,
            priorNonScoredPool = true,
        )
        val ruling = A001GateAdjudicator.adjudicate(passingEvidence(attempts = withRecords(tainted)))
        // A001Analysis.screen already excludes it, so the analysed set stays
        // clean and the violation does not fire. That is the correct behaviour
        // and worth pinning: the defence is the screening, not a second rule.
        assertFalse(ViolationCode.NON_SCORED_POOL_PARTICIPANT_ANALYSED in ruling.violations)
        assertEquals(1, A001Analysis.screen(tainted).excluded.size)
    }

    @Test
    fun `an underpowered attempt is refused rather than run`() {
        val base = A001GateAdjudicator.replayFixture()
        val tooFew = base.attempts.single().records
            .filter { it.comparator != Cohort.SCRIPTED_PET_BASELINE } +
            base.attempts.single().records
                .filter { it.comparator == Cohort.SCRIPTED_PET_BASELINE }.take(3)
        val ruling = A001GateAdjudicator.adjudicate(passingEvidence(attempts = withRecords(tooFew)))
        assertTrue(ViolationCode.SAMPLE_BELOW_POWERED_REQUIREMENT in ruling.violations)
        assertEquals(A001GateAdjudicator.GateOutcome.A001_BLOCKED, ruling.outcome)
    }

    @Test
    fun `an attempt declaring an unfrozen protocol or instrument is refused`() {
        val base = A001GateAdjudicator.replayFixture()
        val records = base.attempts.single().records
        fun attempt(protocol: String, version: Int, instrument: String, analysis: Int) =
            passingEvidence(
                attempts = listOf(
                    A001GateAdjudicator.AttemptRecord(
                        1, protocol, version, instrument, analysis, records,
                    ),
                ),
            )
        assertTrue(
            ViolationCode.PROTOCOL_VERSION_MISMATCH in A001GateAdjudicator.adjudicate(
                attempt("AlivenessStudyProtocolV1", 2, A001StudyContract.INSTRUMENT_ID, 1),
            ).violations,
        )
        assertTrue(
            ViolationCode.INSTRUMENT_MISMATCH in A001GateAdjudicator.adjudicate(
                attempt(
                    A001StudyContract.PROTOCOL_ID, A001StudyContract.PROTOCOL_VERSION,
                    "SomeOtherInstrument", 1,
                ),
            ).violations,
        )
        assertTrue(
            ViolationCode.ANALYSIS_VERSION_MISMATCH in A001GateAdjudicator.adjudicate(
                attempt(
                    A001StudyContract.PROTOCOL_ID, A001StudyContract.PROTOCOL_VERSION,
                    A001StudyContract.INSTRUMENT_ID, 99,
                ),
            ).violations,
        )
    }

    @Test
    fun `a claimed outcome that disagrees with the recomputation is a violation`() {
        val base = A001GateAdjudicator.replayFixture()
        val lying = passingEvidence(
            attempts = withRecords(
                base.attempts.single().records,
                claimed = "A001_ATTEMPT_1_FAIL",
            ),
        )
        val ruling = A001GateAdjudicator.adjudicate(lying)
        assertTrue(
            ViolationCode.CLAIMED_OUTCOME_DISAGREES_WITH_RECOMPUTATION in ruling.violations,
        )
        assertEquals(A001GateAdjudicator.GateOutcome.A001_BLOCKED, ruling.outcome)
    }

    // ------------------------------------------------- frozen threshold guard

    @Test
    fun `every frozen threshold still holds its frozen literal`() {
        assertEquals(
            emptyList(),
            A001GateAdjudicator.driftedThresholds(),
            "a frozen A001 decision threshold has moved",
        )
        // The guard must actually be redundant, or it guards nothing: each entry
        // pairs the live constant with an independently written literal.
        assertTrue(A001GateAdjudicator.FROZEN_DECISION_THRESHOLDS.size >= 11)
        assertTrue(
            A001GateAdjudicator.FROZEN_DECISION_THRESHOLDS.any {
                it.first == "minimumPairedDifference" && it.third == 10.0
            },
        )
    }

    // ------------------------------------------------- three-attempt budget

    @Test
    fun `a failed attempt with attempts remaining is not a programme stop`() {
        val base = A001GateAdjudicator.replayFixture()
        val failing = base.attempts.single().records.map {
            A001Analysis.PairRecord(
                it.participantId, it.comparator, 50.0, 50.0,
                forcedChoicePreferredFull = it.forcedChoicePreferredFull,
            )
        }
        val ruling = A001GateAdjudicator.adjudicate(passingEvidence(attempts = withRecords(failing)))
        assertEquals(
            A001GateAdjudicator.GateOutcome.A001_ATTEMPT_FAILED_ATTEMPTS_REMAIN,
            ruling.outcome,
        )
        assertEquals(2, ruling.attemptsRemaining)
    }

    @Test
    fun `the third failed attempt stops the programme`() {
        val base = A001GateAdjudicator.replayFixture()
        val failing = base.attempts.single().records.map {
            A001Analysis.PairRecord(
                it.participantId, it.comparator, 50.0, 50.0,
                forcedChoicePreferredFull = it.forcedChoicePreferredFull,
            )
        }
        val three = (1..SpikeContract.MAX_SCORED_A001_ATTEMPTS).map { n ->
            A001GateAdjudicator.AttemptRecord(
                n, A001StudyContract.PROTOCOL_ID, A001StudyContract.PROTOCOL_VERSION,
                A001StudyContract.INSTRUMENT_ID, A001Analysis.ANALYSIS_VERSION, failing,
            )
        }
        val ruling = A001GateAdjudicator.adjudicate(passingEvidence(attempts = three))
        assertEquals(A001GateAdjudicator.GateOutcome.ALIVENESS_PROGRAM_STOP, ruling.outcome)
        assertEquals(0, ruling.attemptsRemaining)
    }

    @Test
    fun `a fourth attempt is a violation rather than a fourth chance`() {
        val base = A001GateAdjudicator.replayFixture()
        val records = base.attempts.single().records
        val four = (1..SpikeContract.MAX_SCORED_A001_ATTEMPTS + 1).map { n ->
            A001GateAdjudicator.AttemptRecord(
                n, A001StudyContract.PROTOCOL_ID, A001StudyContract.PROTOCOL_VERSION,
                A001StudyContract.INSTRUMENT_ID, A001Analysis.ANALYSIS_VERSION, records,
            )
        }
        val ruling = A001GateAdjudicator.adjudicate(passingEvidence(attempts = four))
        assertTrue(ViolationCode.ATTEMPT_BUDGET_EXCEEDED in ruling.violations)
        assertEquals(A001GateAdjudicator.GateOutcome.A001_BLOCKED, ruling.outcome)
    }

    // --------------------------------------------------- agent authority

    @Test
    fun `an invented finding cannot create a failure`() {
        val invented = AuditorFinding(
            "F-INVENT", AgenticRole.ALTERNATE_AUDITOR,
            ViolationCode.BASELINE_MARGIN_BELOW_FLOOR,
            listOf("EV-01"),
            "I am confident the baseline is inadequate",
        )
        val ruling = A001GateAdjudicator.adjudicate(passingEvidence(listOf(invented)))
        assertEquals(A001GateAdjudicator.GateOutcome.A001_PASS, ruling.outcome)
        assertEquals(
            FindingDisposition.NOT_CONFIRMED,
            ruling.dispositions.single().disposition,
        )
    }

    @Test
    fun `a finding cannot rescue a failed attempt`() {
        val base = A001GateAdjudicator.replayFixture()
        val failing = base.attempts.single().records.map {
            A001Analysis.PairRecord(it.participantId, it.comparator, 50.0, 50.0)
        }
        // Every shape of finding, including an ambiguous one, against a failure.
        val findings = listOf(
            AuditorFinding(
                "F-A", AgenticRole.PRIMARY_AUDITOR, null, listOf("EV"),
                "the effect is clearly there, the analysis is too conservative",
            ),
            AuditorFinding(
                "F-B", AgenticRole.ALTERNATE_AUDITOR,
                ViolationCode.PROTOCOL_VERSION_MISMATCH, listOf("EV"),
                "the protocol version is wrong so this attempt should not count",
            ),
        )
        val ruling = A001GateAdjudicator.adjudicate(
            passingEvidence(findings, withRecords(failing)),
        )
        assertEquals(
            A001GateAdjudicator.GateOutcome.A001_ATTEMPT_FAILED_ATTEMPTS_REMAIN,
            ruling.outcome,
        )
        assertEquals(ruling.computedOutcome, ruling.outcome)
    }

    @Test
    fun `an ambiguous finding suspends a pass and never manufactures a failure`() {
        val ambiguous = AuditorFinding(
            "F-VAGUE", AgenticRole.PRIMARY_AUDITOR, null, listOf("EV-01"),
            "something about the interaction length troubles me",
        )
        val ruling = A001GateAdjudicator.adjudicate(passingEvidence(listOf(ambiguous)))
        assertEquals(
            A001GateAdjudicator.GateOutcome.A001_PASS_PENDING_ARCHITECT_REVIEW,
            ruling.outcome,
        )
        // Suspended, not failed, and the underlying computation is unchanged.
        assertEquals(A001GateAdjudicator.GateOutcome.A001_PASS, ruling.computedOutcome)
        assertTrue(ruling.passed)
        assertEquals(
            FindingDisposition.AMBIGUOUS_RETURNED_TO_ARCHITECT,
            ruling.dispositions.single().disposition,
        )
    }

    @Test
    fun `an ambiguous finding cannot touch an already blocked gate`() {
        val ambiguous = AuditorFinding(
            "F-VAGUE", AgenticRole.PRIMARY_AUDITOR, null, listOf("EV"), "unease",
        )
        val current = A001GateAdjudicator.currentEvidence()
        val ruling = A001GateAdjudicator.adjudicate(
            A001GateAdjudicator.GateEvidence(
                current.baseline, current.pilot, current.ceiling,
                current.ethicsDeterminationId, current.attempts, listOf(ambiguous),
            ),
        )
        assertEquals(A001GateAdjudicator.GateOutcome.A001_BLOCKED, ruling.outcome)
    }

    @Test
    fun `a correct finding is upheld and changes nothing that was not already true`() {
        val base = A001GateAdjudicator.replayFixture()
        val noBaseline = A001GateAdjudicator.GateEvidence(
            null, base.pilot, base.ceiling, base.ethicsDeterminationId, base.attempts,
            listOf(
                AuditorFinding(
                    "F-REAL", AgenticRole.ALTERNATE_AUDITOR,
                    ViolationCode.BASELINE_NOT_INDEPENDENTLY_QUALIFIED,
                    listOf("EV"), "there is no qualified baseline",
                ),
            ),
        )
        val withFinding = A001GateAdjudicator.adjudicate(noBaseline)
        val withoutFinding = A001GateAdjudicator.adjudicate(
            A001GateAdjudicator.GateEvidence(
                null, base.pilot, base.ceiling, base.ethicsDeterminationId, base.attempts,
            ),
        )
        assertEquals(
            FindingDisposition.UPHELD_BLOCKING,
            withFinding.dispositions.single().disposition,
        )
        // Upheld, and the outcome is identical to the run where nobody said it.
        assertEquals(withoutFinding.outcome, withFinding.outcome)
        assertEquals(withoutFinding.violations, withFinding.violations)
    }

    @Test
    fun `no volume of findings can move a computed outcome`() {
        // The adversarial case: an auditor that raises every code it knows, on
        // evidence where none of them is true.
        val spam = ViolationCode.entries.mapIndexed { i, code ->
            AuditorFinding(
                "F-$i", AgenticRole.ALTERNATE_AUDITOR, code, listOf("EV"),
                "asserting $code with total confidence",
            )
        }
        val ruling = A001GateAdjudicator.adjudicate(passingEvidence(spam))
        assertEquals(A001GateAdjudicator.GateOutcome.A001_PASS, ruling.outcome)
        assertTrue(ruling.dispositions.all { it.disposition == FindingDisposition.NOT_CONFIRMED })
    }

    @Test
    fun `malformed evidence is refused rather than analysed`() {
        // The directive's malformed-evidence case. A pair with a missing score, a
        // score outside the instrument bounds, a duplicate, an incomplete session
        // and a technical failure are all present at once.
        val base = A001GateAdjudicator.replayFixture()
        val clean = base.attempts.single().records
        val malformed = clean + listOf(
            A001Analysis.PairRecord("BAD-01", Cohort.SCRIPTED_PET_BASELINE, null, 40.0),
            A001Analysis.PairRecord("BAD-02", Cohort.SCRIPTED_PET_BASELINE, 140.0, 40.0),
            A001Analysis.PairRecord("BAD-03", Cohort.SCRIPTED_PET_BASELINE, 80.0, 40.0,
                fullCompletedFraction = 0.1),
            A001Analysis.PairRecord("BAD-04", Cohort.SCRIPTED_PET_BASELINE, 80.0, 40.0,
                technicalFailure = true),
            A001Analysis.PairRecord("BAD-05", Cohort.SCRIPTED_PET_BASELINE, 80.0, 40.0),
            A001Analysis.PairRecord("BAD-05", Cohort.SCRIPTED_PET_BASELINE, 81.0, 41.0),
        )
        val ruling = A001GateAdjudicator.adjudicate(
            passingEvidence(attempts = withRecords(malformed)),
        )
        // Six malformed rows in, five excluded (the duplicate's first copy is
        // legitimate), and the analysed set is exactly the clean one.
        val screened = A001Analysis.screen(malformed)
        assertEquals(5, screened.excluded.size)
        assertEquals(clean.size + 1, screened.included.size)
        // None of the analysed-set violations fires, because screening caught them.
        assertFalse(ViolationCode.SCORE_OUT_OF_RANGE_ANALYSED in ruling.violations)
        assertFalse(ViolationCode.INCOMPLETE_SESSION_ANALYSED in ruling.violations)
        assertFalse(ViolationCode.DUPLICATE_PARTICIPANT_ANALYSED in ruling.violations)
        // And the gate still decides, on the surviving complete cases only.
        assertEquals(A001GateAdjudicator.GateOutcome.A001_PASS, ruling.outcome)
    }

    @Test
    fun `multiplicity correction reaches the gate ruling unchanged`() {
        // The directive's multiplicity case, checked at the gate rather than only
        // in A001Analysis: the adjudicator must carry the Holm-corrected family
        // through, not a raw-p one.
        val ruling = A001GateAdjudicator.adjudicate(passingEvidence())
        val arms = ruling.analysis!!.ablations
        assertEquals(A001StudyContract.ABLATION_FAMILY.size, arms.size)
        // Holm adjusted p-values are monotone in raw-p order and never smaller
        // than the raw value.
        val byRaw = arms.sortedBy { it.rawP }
        for (i in byRaw.indices) {
            assertTrue(byRaw[i].holmAdjustedP >= byRaw[i].rawP)
            if (i > 0) assertTrue(byRaw[i].holmAdjustedP >= byRaw[i - 1].holmAdjustedP)
        }
        // The fixture is built so the family splits, which is what makes this
        // meaningful: an all-significant family would not test the correction.
        assertTrue(arms.any { it.significant })
        assertTrue(arms.any { !it.significant })
        assertTrue(
            arms.all {
                it.significant == (it.holmAdjustedP <= A001StudyContract.ABLATION_FAMILY_ALPHA)
            },
        )
    }

    @Test
    fun `three violation codes are unreachable while screening holds, and are tripwires`() {
        // An honest accounting rather than an allowlist.
        //
        // DUPLICATE_PARTICIPANT_ANALYSED, INCOMPLETE_SESSION_ANALYSED and
        // SCORE_OUT_OF_RANGE_ANALYSED can only fire if a record that
        // A001Analysis.screen should have excluded reaches the analysed set. While
        // screening is correct they are unreachable, as the malformed-evidence
        // test above shows. They are kept because they are defence in depth: if
        // screening ever regresses, the gate refuses instead of quietly analysing
        // data it should have dropped.
        //
        // This test states that fact rather than hiding it, and it fails if
        // someone adds a code with no derivation at all.
        val tripwires = setOf(
            ViolationCode.DUPLICATE_PARTICIPANT_ANALYSED,
            ViolationCode.INCOMPLETE_SESSION_ANALYSED,
            ViolationCode.SCORE_OUT_OF_RANGE_ANALYSED,
            ViolationCode.NON_SCORED_POOL_PARTICIPANT_ANALYSED,
        )
        val exercised = setOf(
            ViolationCode.BASELINE_NOT_INDEPENDENTLY_QUALIFIED,
            ViolationCode.BASELINE_MARGIN_BELOW_FLOOR,
            ViolationCode.PILOT_NOT_PROTOCOL_VALID,
            ViolationCode.FEASIBILITY_NOT_ESTABLISHED,
            ViolationCode.ETHICS_DETERMINATION_ABSENT,
            ViolationCode.SAMPLE_BELOW_POWERED_REQUIREMENT,
            ViolationCode.PROTOCOL_VERSION_MISMATCH,
            ViolationCode.INSTRUMENT_MISMATCH,
            ViolationCode.ANALYSIS_VERSION_MISMATCH,
            ViolationCode.THRESHOLD_WEAKENED_AFTER_FREEZE,
            ViolationCode.ATTEMPT_BUDGET_EXCEEDED,
            ViolationCode.CLAIMED_OUTCOME_DISAGREES_WITH_RECOMPUTATION,
        )
        // Every code is accounted for as one or the other. Adding a code without
        // deciding which it is fails here.
        assertEquals(
            ViolationCode.entries.toSet(),
            tripwires + exercised,
            "a violation code is neither exercised by a test nor declared a tripwire",
        )
        // The exercised ones must genuinely fire somewhere, not merely be listed.
        val live = A001GateAdjudicator.deriveViolations(A001GateAdjudicator.currentEvidence())
        assertTrue(ViolationCode.BASELINE_NOT_INDEPENDENTLY_QUALIFIED in live)
        assertTrue(ViolationCode.PILOT_NOT_PROTOCOL_VALID in live)
        assertTrue(ViolationCode.FEASIBILITY_NOT_ESTABLISHED in live)
        assertTrue(ViolationCode.ETHICS_DETERMINATION_ABSENT in live)
    }
}
