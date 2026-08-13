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

## Immediate consequence

`CuriosityEnvelopeFeasibilityV1` gives the primary reviewer a decision to make
as soon as one exists: the A000 joint feasibility search returned
`EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE`, and only the reviewer may choose
between the threshold-revision path and the mechanism-revision path.
