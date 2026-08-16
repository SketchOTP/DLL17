# IndependentReviewOnboardingV1

> **D016-K current state:** `BaselineIndependentOwner` is retired. Baseline qualification is determined only by the frozen 40-person blinded experiment and `A001GateAdjudicatorV1`; no person, model or reviewer may replace or override it. Older role text below is superseded planning history.

- Status: `READY_FOR_HUMAN_EVIDENCE`
- Version: 1
- Roster: [IndependentReviewRosterV1](IndependentReviewRosterV1.md) —
  `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`

The complete onboarding package for the three roles A001 cannot start without.
Everything a candidate needs in order to accept, decline, or decline on grounds
of conflict, is here. **No name appears anywhere in this package and no
placeholder person has been invented.**

Filling the roster requires three real, eligible people. It is the last thing
standing between the programme and Attempt 1 that code cannot supply.

---

## The three roles

| Role | Decides |
|---|---|
| `PrimaryIndependentAlivenessGateReviewer` | Whether an attempt may start, whether a change is material, whether an attempt's result stands, and whether the A-track is in governance breach |
| `AlternateIndependentAlivenessGateReviewer` | The same, when the primary is unavailable or recused |
| `BaselineIndependentOwner` | Whether `ScriptedPetBaselineV1` is a competent comparator, and whether it must be strengthened before it is used to judge FULL |

---

## Eligibility

### Gate reviewers, primary and alternate

A candidate is eligible only if all of the following are true.

| # | Requirement |
|---|---|
| 1 | Did not implement FULL and did not author any of its load-bearing mechanisms |
| 2 | Did not author `ScriptedPetBaselineV1` or `DegradedScriptedControlV1` |
| 3 | Sits outside the direct management or reporting chain of the FULL programme lead and team, where organizationally possible |
| 4 | Has no bonus, performance target, funding milestone, release KPI or equivalent incentive contingent on A001 passing or on the product shipping |
| 5 | Is able to read a preregistered protocol and a statistical analysis plan well enough to tell a preregistered decision from a post-hoc one |
| 6 | Is available for the whole of Attempt 1, including its analysis and adjudication |

If no internal person satisfies 1–4, the role must be filled externally. The
primary and alternate must not be the same person and must not share the
incentive structure that would disqualify one of them.

### Baseline independent owner

| # | Requirement |
|---|---|
| 1 | Did not implement FULL and did not author any of its mechanisms |
| 2 | Has authority to reject the baseline or require it to be strengthened, and to have that decision stand |
| 3 | Has no incentive contingent on A001 passing |
| 4 | May be, but need not be, the alternate gate reviewer — provided doing so does not make one person the only check on the comparison |

---

## Declarations to be signed before Attempt 1

### 1. Conflict of interest declaration

To be completed by each named person. Every item is answered explicitly; a blank
is not an answer.

- Any financial interest in the product, the programme or the organization
  shipping it, including equity, options and deferred compensation.
- Any compensation, bonus, target or milestone that moves if A001 passes.
- Any management or reporting relationship to the FULL programme lead or team,
  in either direction.
- Any authorship of, or contribution to, FULL, the scripted baseline, the
  degraded control, the study protocol or the instrument.
- Any close collaboration with a member of the FULL team within the previous two
  years.
- Any other interest a reasonable observer would consider capable of affecting
  the judgement required.

A declared conflict does not automatically disqualify. An undeclared one is
`A001_GOVERNANCE_BREACH`.

### 2. Independence declaration

> I confirm that my judgement in this role is not contingent on any outcome of
> A001; that no part of my compensation, standing or work depends on the study
> passing; and that I am free to return an unfavourable ruling without adverse
> consequence to me.

### 3. Authority acknowledgement

> I understand and accept that in this role I may, without the consent of the
> FULL team, the schedule owner or the study operator:
>
> - block the start of a scored attempt;
> - rule a proposed change `NOT_MATERIAL` and so refuse a further attempt;
> - reject the scripted baseline or require it to be strengthened;
> - require cognitive pretesting of the instrument before Attempt 1;
> - uphold `ALIVENESS_PROGRAM_STOP` after three scored failures;
> - declare `A001_GOVERNANCE_BREACH` and place the A-track in `GOVERNANCE_HOLD`.
>
> I understand that a hold I place cannot be cleared by the FULL team.

### 4. Signed acceptance record

| Field | Entry |
|---|---|
| Role | |
| Name | |
| Affiliation | |
| Internal or external | |
| Conflict declaration | attached, dated |
| Independence declaration | attached, dated |
| Authority acknowledgement | attached, dated |
| Accepted on | |
| Signature | |
| Countersigned by study owner | |

The completed record is filed alongside this document and is a prerequisite for
`AlivenessGovernanceAuditV2` item GA-16.

---

## Breach authority

The named primary reviewer may declare `A001_GOVERNANCE_BREACH` without anyone
else's consent. A declaration immediately:

- freezes recruitment and all scored sessions;
- freezes unblinding and any release to the FULL team;
- blocks new preregistration;
- preserves all raw data and prior rulings;
- marks the A-track `GOVERNANCE_HOLD`.

The FULL team cannot self-clear a hold. Only the primary reviewer, or the
alternate acting under a documented recusal, may lift one.

---

## Replacement and recusal

**Replacement is permitted only for documented cause:** departure, incapacity,
sustained unavailability, a newly discovered conflict, or voluntary ethical
recusal.

**Explicitly not cause:** disagreement with a `NOT_MATERIAL` ruling; refusal to
approve a new attempt; an unfavourable baseline or A001 ruling; schedule
pressure; or a preference for a different statistical or architectural opinion
after seeing results.

The preregistered alternate is used first. Any out-of-roster appointment
requires documented exceptional cause, a fresh conflict review and independent
governance approval. A replacement inherits prior valid rulings and cannot
reopen them merely because the reviewer changed.

**Recusal process.** A reviewer who discovers a conflict mid-attempt notifies
the study owner in writing, states the conflict, and stands down from the
affected decisions only. The alternate takes those decisions. A recusal is
recorded and does not vacate rulings already made before the conflict arose.

---

## What a new reviewer should read first, in order

1. `AlivenessProgramGateV1` — the three-attempt budget and the success floor.
2. `AlivenessStudyProtocolV1` — the study they are gating.
3. `GradedAlivenessInstrumentV1` — the exact words participants hear.
4. `BaselineCoverageManifestV1` (`evidence/BASELINE_COVERAGE_MANIFEST.txt`) — the
   full disclosure of the comparator.
5. `BaselineQualificationProtocolV1` — how the comparator earns its place.
6. `BlindVariancePilotV1` — the information barrier and what it releases.
7. `A001FeasibilityBudgetV1` — whether a powered attempt is affordable at all.
8. `MaterialChangeEligibilityV1` — what would justify Attempt 2.
9. `evidence/GOVERNANCE_AUDIT.txt` — the current activation state and its blockers.
10. `evidence/negative/D008/NEGATIVE_EVIDENCE.md` — the rejected candidate, kept
    deliberately.

## Unresolved

| Field | State |
|---|---|
| `PrimaryIndependentAlivenessGateReviewer` | unassigned |
| `AlternateIndependentAlivenessGateReviewer` | unassigned |
| `BaselineIndependentOwner` | unassigned |
| Signed conflict declarations | none can exist |
| Signed independence declarations | none can exist |
| Signed authority acknowledgements | none can exist |
| Reviewer contact detail given to participants | none |
