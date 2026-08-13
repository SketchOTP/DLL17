# AlivenessGovernanceAuditV1

- Status: `FROZEN`
- Version: 1
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

| State | Count |
|---|---|
| `PASS` | 6 |
| `NOT_APPLICABLE_PRE_ATTEMPT` | 6 |
| `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE` | 1 |
| `BLOCKED` | 7 |

```
A001_PROGRAM_STATE = ALIVENESS_UNTESTED
A001_ACTIVATION    = BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED
```

`GovernanceAuditTest` additionally asserts that no item depending on human
evidence claims `PASS`, so the audit cannot drift into optimism as the code
around it changes.
