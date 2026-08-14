# AlivenessGovernanceAuditV2

- Status: `FROZEN`
- Version: 2 (extended under D010 from a pre-activation checklist to the activation gate)
- Executable portion: `research/aliveness-spike/analysis/.../AlivenessGovernanceAudit.kt`
- Current output: `research/aliveness-spike/evidence/GOVERNANCE_AUDIT.txt`

A research-governance check, separate from organism runtime regression. It never
marks an item `PASS` by exercising organism code: `GovernanceAuditTest` asserts
the audit is a pure function of contract state and returns identical results on
repeated calls.

## Item states

| State | Meaning |
|---|---|
| `PASS` | Verified automatically from repository and contract state |
| `NOT_APPLICABLE_PRE_ATTEMPT` | No scored attempt exists, so there is nothing to check |
| `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE` | Human judgement that cannot be automated |
| `BLOCKED` | Blocked on a named missing artefact |

## Current state

27 items. Every prerequisite for opening human scored recruitment is enumerated,
and a missing one names its own blocking state rather than defaulting to ready.

| State | Count |
|---|---|
| `PASS` | 17 |
| `NOT_APPLICABLE_PRE_ATTEMPT` | 1 |
| `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE` | 2 |
| `BLOCKED` | 7 |

```
A001_PROGRAM_STATE       = ALIVENESS_UNTESTED
A001_ACTIVATION          = BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED
HUMAN_SCORED_RECRUITMENT = BLOCKED
OUTSTANDING_BLOCKERS     = 5
```

The five distinct blocking states, in resolution order:

| Blocker | Needs |
|---|---|
| `BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED` | 40 participants and an assigned owner |
| `BLOCKED_VARIANCE_PILOT_NOT_REGISTERED` | An independent operator and a reviewer |
| `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD` | The pilot to have run |
| `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED` | Three real, eligible people |
| `BLOCKED_SPEC_STUDY_BUDGET` | An owner funding decision |

The activation state and the recruitment gate are **derived from the items**
rather than declared beside them, so the gate cannot fall out of agreement with
its own checklist. `GovernanceAuditTest` proves it both ways: remove every
blocking item and the gate opens; leave one and it does not.

`GovernanceAuditTest` additionally asserts that no item depending on human
evidence claims `PASS`, so the audit cannot drift into optimism as the code
around it changes.
