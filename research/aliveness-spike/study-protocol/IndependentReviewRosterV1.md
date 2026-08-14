# IndependentReviewRosterV1

- Status: `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`
- Version: 1 (draft; not activated)

```
IndependentReviewRosterV1
- PrimaryIndependentAlivenessGateReviewer:   <unassigned>
- AlternateIndependentAlivenessGateReviewer: <unassigned>
- BaselineIndependentOwner:                  <unassigned>
```

A001 remains `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED` while any required name is
blank. All three are blank. This is the single most consequential blocker on the
A-track, and it cannot be resolved by writing code.

## Frozen role-compatibility ruling

Decided by the programme owner on 2026-08-14, recorded as DEC-0039. This
resolves the overlap question the frozen contracts previously left open, so
`BLOCKED_GOVERNANCE_ROLE_COMPATIBILITY` no longer applies to the intended
structure.

| Role | Constraint |
|---|---|
| `PrimaryIndependentAlivenessGateReviewer` | Must be a unique independent person. No overlap with any other role. |
| `AlternateIndependentAlivenessGateReviewer` | May be the same person as `BaselineIndependentOwner`. |
| `BaselineIndependentOwner` | May be the same person as the alternate reviewer. |
| Independent human-study operator | Must be a third unique person, and may not also be a gate reviewer. |

Three distinct people minimum. The pre-existing condition on the permitted
overlap still stands: it must not make one person the only check on the
comparison, which is why the primary is required to be unique.

## Candidates approached — not appointments

The owner has identified preferred candidates. **None of them has been
approached, none has accepted, none has completed conflict review, and none has
signed anything.** They are recorded here as intent, and this table confers no
role, no authority and no ruling. The roster above stays blank until a real
acceptance and a signed record exist.

| Role | Candidate | Why |
|---|---|---|
| Primary reviewer | Prof. Rachel McDonnell, Trinity College Dublin | Researches how motion, behaviour, personality and embodiment of virtual characters are perceived by humans — unusually close to what this gate measures. |
| Alternate reviewer + baseline owner | Prof. Guy Hoffman, Cornell | Human-robot interaction, robotic companions, non-anthropomorphic social robots and how people interpret autonomous behaviour; suited to challenge whether the scripted comparator is genuinely competent rather than a straw man. |
| Independent study operator | Prof. Andreea Bobu, MIT | Autonomous agents interacting with people, alignment with human expectations, and controlled user studies; methodological background for the sealed pilot without being a gate reviewer. |

Institution-published contact addresses, recorded so the approach can be made
and audited: `ramcdonn@tcd.ie`, `hoffman@cornell.edu`, `abobu@mit.edu`.

If a candidate declines, the roster stays blank rather than falling through to
whoever is available. Eligibility conditions 1–4 of
[IndependentReviewOnboardingV1](IndependentReviewOnboardingV1.md) apply to every
replacement candidate equally.

## Frozen ethics posture

Decided by the programme owner on 2026-08-14, recorded as DEC-0039.

Before any participant is recruited, an independent human-subjects or ethics
determination must be obtained from a qualified IRB, HRPP or equivalent body.
The determination need not result in full review — but **the programme may not
determine its own exemption**. OHRP recommends that investigators not make their
own exemption determinations because of the conflict of interest inherent in
doing so, and the Common Rule does not automatically cover every privately
conducted study, so an absence of automatic coverage is not a finding of
exemption.

`GA-26` carries this posture and remains
`REQUIRES_SIGNED_GOVERNANCE_EVIDENCE`: no determination exists yet, and none is
claimed.

## Frozen independence requirements

The primary and alternate gate reviewers must:

- not implement FULL or author its load-bearing mechanisms;
- not author the scored scripted baseline they adjudicate against FULL;
- sit outside the direct management or reporting chain of the FULL programme
  lead and team where organizationally possible;
- have no bonus, performance target, funding milestone, release KPI or
  equivalent incentive contingent on A001 passing or on the product shipping;
- disclose material financial, managerial, authorship or close-collaboration
  conflicts before Attempt 1;
- hold explicit authority to return `NOT_MATERIAL`, reject a baseline, block an
  attempt, or uphold `ALIVENESS_PROGRAM_STOP` without the FULL team's approval.

If no internal person satisfies these conditions, the role must be external.

## Frozen replacement discipline

Replacement of the primary is permitted only for documented cause: departure,
incapacity, sustained unavailability, newly discovered conflict, or voluntary
ethical recusal.

Explicitly **not** cause: disagreement with a `NOT_MATERIAL` ruling, refusal to
approve a new attempt, an unfavourable baseline or A001 ruling, schedule
pressure, or a preference for a different statistical or architectural opinion
after seeing results.

The preregistered alternate is used first. Any out-of-roster appointment
requires documented exceptional cause, a fresh conflict review and independent
governance approval. A replacement inherits prior valid rulings and cannot
reopen them merely because the reviewer changed.

## Frozen breach authority

The named primary reviewer may declare `A001_GOVERNANCE_BREACH` without the
consent of the FULL team, the schedule owner or the study operator. A
declaration immediately freezes recruitment and scored sessions, freezes
unblinding and release to the FULL team, blocks new preregistration, preserves
all raw data and rulings, and marks the A-track `GOVERNANCE_HOLD`. The FULL team
cannot self-clear a hold.

## Onboarding

The complete onboarding package — eligibility, the three declarations, the
signed acceptance record, the reading order, and the recusal process — is
[IndependentReviewOnboardingV1](IndependentReviewOnboardingV1.md). It contains
no names and no placeholder people.

## What is waiting for the reviewer

Decisions that exist now and cannot be taken by anyone else:

1. Whether `ScriptedPetBaselineV1` is a fair comparator, on the full disclosure
   in `evidence/BASELINE_COVERAGE_MANIFEST.txt`.
2. Whether `GradedAlivenessInstrumentV1` may be used without cognitive
   pretesting, given that it was written by a party with an interest in the
   outcome.
3. Whether the variance pilot may be registered, and who operates it.
4. Whether this study needs external ethical or institutional approval.

The A000 curiosity-envelope question that was waiting for the reviewer under
D008 no longer is: the empty feasible region was resolved by revising the
mechanism under unchanged thresholds, and the programme's single allowed
threshold-only revision remains unspent.
