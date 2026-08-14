# A001 activation gate

- Track: A001 — human aliveness comparison, Attempt 1 of a maximum of 3
- Directive: D010 (activation package prepared without human data)
- Program state: `ALIVENESS_UNTESTED`, attempts consumed 0
- Activation state: `BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED`
- Human scored recruitment: **BLOCKED**
- Evaluated: 2026-08-13

This gate is not closed and D010 did not attempt to close it. D010's job was to
make everything ready that can be made ready without people, so that what
remains is visibly a shortage of reviewers and money rather than a shortage of
work.

**No human outcome data exists.** No participant has been recruited, no session
has been run, and nothing in this repository may claim otherwise.

---

## What D010 completed

| Artifact | State |
|---|---|
| `AlivenessStudyProtocolV1` | `FROZEN` — design, procedure, endpoints, exclusions, analysis, classification |
| `GradedAlivenessInstrumentV1` | `FROZEN` — exact wording and five anchors, verbatim |
| Primary decision rule | Encoded exactly: mean ≥ +10.0 **and** two-sided 95% CI lower bound > 0 |
| Human ablation family | Three arms, Holm-Bonferroni at FWER 0.05 |
| `BaselineQualificationProtocolV1` | Powered at 40 participants, +15.0 margin, complete |
| `BaselineCoverageManifestV1` | Generated from the comparator implementation itself |
| `BlindVariancePilotV1` | Operationally ready at 36 pairs, sealed channel proven |
| `A001FeasibilityBudgetV1` | Calculator complete; exact noncentral-t power |
| `A001AnalysisV1` | Preregistered and exercised on synthetic fixtures |
| `ParticipantInformationAndConsentV1` | Information sheet, consent form, debrief |
| `DataHandlingAndPrivacyV1` | Collection, separation, retention, access, publication |
| `IndependentReviewOnboardingV1` | Eligibility, three declarations, acceptance record, recusal |
| `AlivenessGovernanceAuditV2` | 27 items; activation state derived from them |

## The activation audit

27 items. As evaluated at D010 on 2026-08-13: 17 `PASS`, 1
`NOT_APPLICABLE_PRE_ATTEMPT`, 2 `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE`, 7
`BLOCKED`. The current counts are in the D016-A resumption record below.

Current output: `research/aliveness-spike/evidence/GOVERNANCE_AUDIT.txt`.

The activation state and the recruitment gate are computed from the items, not
declared beside them. `GovernanceAuditTest` proves it in both directions: remove
every blocking item and the gate opens; leave one and it does not.

## The outstanding blockers

None of these can be cleared by writing code.

| # | Blocker | What it needs |
|---|---|---|
| 1 | `BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED` | 40 participants and an assigned `BaselineIndependentOwner` |
| 2 | `BLOCKED_VARIANCE_PILOT_NOT_REGISTERED` | An independent operator and a reviewer to register the pilot |
| 3 | `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD` | The pilot to have run and released its one number |
| 4 | `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED` | Three real, eligible people |
| ~~5~~ | ~~`BLOCKED_SPEC_STUDY_BUDGET`~~ | **Cleared 2026-08-14.** The owner froze 400 participants and 250 participant-hours. See the D016-A resumption below. |

Two further items require signed human judgement rather than an artifact:
adjudication of material change for Attempts 2 and 3, and whether any external
ethical or institutional approval is required. **No IRB, institutional or
ethics-board approval exists, and none is claimed.**

## What is deliberately still unknown

1. **The powered sample size.** It is a function of the pilot SD, and the pilot
   has not run. The calculator returns a blocking state rather than a number.
2. **Whether the study is affordable at all.** `A001_NOT_FEASIBLE` is a real
   possible outcome, and an underpowered attempt is not a legitimate substitute.
3. **Whether the instrument works.** It is frozen but not cognitively pretested,
   and the reviewer may require pretesting before Attempt 1.
4. **Whether FULL beats the baseline.** A000 says the mechanisms are real,
   bounded and attributable. It says nothing about how they look to a person.

## Sequencing

The order below is not a preference; each step needs the one before it.

1. Assign the roster. Everything else is gated on it.
2. Reviewer decides on the baseline, the instrument and any external approval.
3. Run the baseline qualification: 40 participants, +15.0 margin.
4. Register and run the variance pilot: 36 pairs, sealed.
5. Release `pairedDifferenceSd`. Compute the powered budget.
6. Compare against the owner ceiling, frozen at 400 participants and 250
   participant-hours on 2026-08-14. Feasible or not feasible.
7. Only then: Attempt 1.

## Gate state

**`A001_ACTIVATION = BLOCKED`.** Not started, and correctly so.

Four blockers now, not five: see the D016-A resumption record below.

---

## D016-A — governance activation attempt

- Directive: D016 (execute the complete A001 aliveness gate)
- Evaluated: 2026-08-14
- Boundary: `D016-A`, Phase A001.0
- Disposition: **`BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`**

D016 instructed execution of all sixteen A001 phases. Phase A001.0 was not
entered, and no later phase was reached. Nothing in the gate state above
changed, because nothing that could change it can be produced by writing code.

| Required role | State |
|---|---|
| `PrimaryIndependentAlivenessGateReviewer` | unassigned |
| `AlternateIndependentAlivenessGateReviewer` | unassigned |
| `BaselineIndependentOwner` | unassigned |
| Independent human-study operator (`BlindVariancePilotV1`) | unnamed |

No conflict-of-interest declaration, independence declaration, authority
acknowledgement or signed role acceptance can exist while no person is named,
so none was produced. None of the six reviewer rulings was taken. **No
placeholder name, signature, ruling or approval was created**, and no IRB,
ethics-board or institutional approval is claimed.

Two eligibility facts the roster owner needs before naming anyone:

1. `IndependentReviewOnboardingV1` requires that the primary and alternate have
   no incentive contingent on A001 passing or on the product shipping, and sit
   outside the FULL programme's direct reporting chain. It states that if no
   internal person satisfies conditions 1–4 the role must be filled externally.
2. One role overlap is explicitly permitted — the `BaselineIndependentOwner`
   may be the alternate gate reviewer, provided that does not make one person
   the only check on the comparison. The study operator's overlap is not
   addressed by any frozen contract, so a proposed overlap there returns
   `BLOCKED_GOVERNANCE_ROLE_COMPATIBILITY` rather than an inferred permission.

`BLOCKED_SPEC_STUDY_BUDGET` is independently outstanding and is the only
blocker clearable without recruiting anyone: `maxFundableParticipants` and
`maxParticipantHours` are owner decisions, and A001.1 requires them frozen
*before* the pilot result exists so the result cannot influence the ceiling.

Attempts consumed remains `0 / 3`. Programme state remains
`ALIVENESS_UNTESTED`. No participant was recruited, no session was run, and no
human outcome data exists anywhere in this repository. Recorded as O-0016.

---

## D016-A resumption — owner decisions synchronized

- Directive: D016, boundary `D016-A`
- Recorded: 2026-08-14
- Disposition: **`BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`** (unchanged)
- Machine state: 27 items, 18 `PASS`, 1 `NOT_APPLICABLE_PRE_ATTEMPT`, 2
  `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE`, 6 blocking, `OUTSTANDING_BLOCKERS=4`

The programme owner supplied the three decisions that were within their gift,
and they are now held in the repository rather than only in the planning
record. Recorded as DEC-0039.

### Cleared: `BLOCKED_SPEC_STUDY_BUDGET`

`maxFundableParticipants = 400`, `maxParticipantHours = 250.0`, frozen before
the variance pilot has run and therefore before the powered requirement is
knowable. The value lives in `A001FeasibilityBudget.FROZEN_OWNER_CEILING` and
`GA-24` reads it rather than restating it, so the audit cannot report a ceiling
the calculator does not use. At the frozen 2220-second schedule, 400
participants is 246.667 participant-hours, so the participant count binds first
and a test asserts that stays true.

If the powered requirement later exceeds either half, the result is
`A001_NOT_FEASIBLE` and a redesign. The ceiling is not a target to be raised.

### Frozen: role compatibility

Primary reviewer unique; alternate reviewer may also be `BaselineIndependentOwner`;
study operator a third unique person who may not be a gate reviewer. Minimum
three distinct people. This resolves the overlap question the frozen contracts
left open, so `BLOCKED_GOVERNANCE_ROLE_COMPATIBILITY` no longer applies to the
intended structure.

### Frozen: ethics posture

An independent human-subjects determination must be obtained from a qualified
IRB, HRPP or equivalent body before any participant is recruited. Full review is
not necessarily required; **self-determined exemption is prohibited**. `GA-26`
carries the posture and stays `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE`: no
determination exists, and none is claimed.

### Still blocking

The roster is still blank. Three candidates have been identified and recorded in
`IndependentReviewRosterV1` as candidates — none approached, none accepted, none
conflict-reviewed, none signed. No placeholder name was entered into the roster.

Attempts consumed remains `0 / 3`. Programme state remains
`ALIVENESS_UNTESTED`. No participant was recruited, no session was run, and no
human outcome data exists. Recorded as O-0017, superseding O-0016.
