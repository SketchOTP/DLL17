# IndependentReviewRosterV1

- Status: **SUPERSEDED** by [AgenticReviewHarnessV1](AgenticReviewHarnessV1.md)
- Superseded: 2026-08-14, by the D016-B architect decision, implemented at D016-C
- Historical status: `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`
- Version: 1 (draft; never activated)

## Supersession notice

The three governance roles this document describes are no longer filled by
people. D016-B replaced them with isolated agentic roles, and D016-C implements
and machine-checks that architecture:

| This document's role | Replaced by |
|---|---|
| `PrimaryIndependentAlivenessGateReviewer` | `PrimaryAgenticAlivenessGateReviewer` |
| `AlternateIndependentAlivenessGateReviewer` | `AlternateAgenticAlivenessGateReviewer` |
| `BaselineIndependentOwner` | folded into the alternate agentic reviewer |
| Independent human-study operator | `IndependentAgenticStudyOperator` |

The A001 activation audit no longer reports
`BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`. It now reports
`BLOCKED_AGENTIC_REVIEW_HARNESS_UNQUALIFIED` and
`BLOCKED_AGENTIC_REVIEW_DIVERSITY_UNAVAILABLE` in its place.

**Everything below this notice is retained as superseded planning history and is
not a current requirement.** It is kept rather than deleted because the earlier
blocker was a real state of the programme with a real disposition, and deleting
the requirement would leave the D016-A record referring to a blocker with no
provenance. Nothing below has been altered to agree with the new architecture,
and the one correction made is marked as a correction.

Two things this supersession does **not** change: A001 still measures human
perception and still needs real blinded participants, and an independent
human-subjects determination is still required before anyone is recruited. See
[AgenticReviewHarnessV1](AgenticReviewHarnessV1.md).

---

## Historical record (superseded)

```
IndependentReviewRosterV1
- PrimaryIndependentAlivenessGateReviewer:   <unassigned>
- AlternateIndependentAlivenessGateReviewer: <unassigned>
- BaselineIndependentOwner:                  <unassigned>
```

A001 remained `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED` while any required name
was blank. All three stayed blank for the whole life of this document. No
placeholder name was ever entered.

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

## Preferred candidates — not appointments

> **Correction, 2026-08-14 (D016-C).** This heading previously read "Candidates
> approached — not appointments", which contradicted the paragraph directly
> beneath it: none of these people was ever approached. The heading was wrong and
> is corrected here rather than left standing. Nothing else in this section has
> been changed, and the candidate names are retained as superseded planning
> history now that the roles are agentic.

The owner identified preferred candidates. **None of them has been
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

## Frozen ethics posture — superseded by D016-J

The 2026-08-14 posture below is retained as historical provenance only. D016-J
superseded its requirement for an independent IRB, HRPP or equivalent
determination. The current project record carries the owner-delegated
`APPROVED_WITH_PRE_RECRUITMENT_CONDITIONS` determination for minimal-risk,
benign behavioral research with U.S. adults who attest that they are 18+ and
can provide their own legally effective consent.

This does **not** claim formal IRB approval, federal exemption or established
Common Rule coverage. The approved scope excludes prisoners, people lacking
consent capacity, non-U.S. participants, recording, biometrics, health data,
sensitive demographics and participant-device collection. A material scope
change stops recruitment and requires a new review.

The agentic reviewers remain auditors, not an IRB or HRPP, and
`ISSUE_ETHICS_DETERMINATION` remains forbidden to every agentic role.

The historical `GA-26` requirement and “no determination exists” wording below
describe the pre-D016-J state and are not current activation requirements.

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
