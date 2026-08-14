# AlivenessProgramGateV1

- Status: `READY_FOR_HUMAN_EVIDENCE`
- Version: 1 (draft; not activated)

## Frozen now

| Decision | Value |
|---|---|
| Maximum scored A001 attempts under this foundational hypothesis | 3 |
| Material-change definition | `MaterialChangeEligibilityV1` v1 |
| Negative-attempt retention | every failed attempt is retained durably and may not be deleted, reclassified as pilot data, or selectively pooled into a later pass |
| Terminal state | `ALIVENESS_PROGRAM_STOP` after three scored failures |
| Program states | `ALIVENESS_UNTESTED → A001_ATTEMPT_1 → PASS \| REVISE → A001_ATTEMPT_2 → PASS \| REVISE → A001_ATTEMPT_3 → PASS \| ALIVENESS_PROGRAM_STOP` |
| Consequence of stop | R003–R018 organism and product progression remain blocked; R000–R002 engineering evidence remains valid as infrastructure evidence; no Attempt 4 inside this hypothesis |
| Floor direction | the program-level success floor may become stricter or stay equivalent, never easier, after Attempt 1 begins |
| Current program state | `ALIVENESS_UNTESTED`, attempts consumed 0 |
| Human leave-one-out family | Three arms: `FULL − curiosity anti-convergence`, `FULL − preference learning`, `FULL − outcome uncertainty / directed re-exploration`. The episodic arm was retired under D009 when the mechanism left FULL; the architect preregistered the third arm under D010. Holm-Bonferroni corrects across the three. |
| **Programme success floor** | mean `PairedAlivenessDifference >= +10.0` on the 0–100 graded instrument, **and** the two-sided 95% CI lower bound `> 0`. Frozen by the architect under D010, before any human data existed. |
| Alpha / power | 0.05 two-sided / 0.80 |

## The success floor, and why it is two conditions

`BLOCKED_SPEC_ALIVENESS_SUCCESS_FLOOR` is **cleared**. The architect set the
floor under D010, before any human data existed, which is the only moment at
which setting it is honest.

It is deliberately two conditions rather than one. A p-value alone would let a
trivial but well-powered difference authorize R003–R009; a point estimate alone
would let an unresolved sample do the same. Requiring both means the programme
can only continue on an effect that is large enough to matter *and* resolved
enough to believe.

Ten points on a 0–100 scale is roughly the gap between two adjacent anchor
descriptions being the better fit. It is a value judgement about what the
complexity is worth, and no amount of A000 evidence produces it.

The floor may become stricter or stay equivalent after Attempt 1 begins. It may
never become easier. `AlivenessGovernanceAuditV2` item GA-03 checks that it is
frozen and unweakened; `A001StudyContractTest` pins the constants so relaxing
one is a visible, deliberate edit.
