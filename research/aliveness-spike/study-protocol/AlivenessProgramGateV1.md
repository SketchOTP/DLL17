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
| Human leave-one-out family | Two arms after D009: `FULL − curiosity anti-convergence` and `FULL − preference learning`. The episodic arm was retired when the mechanism left FULL. Holm-Bonferroni corrects across the comparisons actually tested, so the family-wise correction now runs over two. Adding a replacement third arm requires a new preregistered plan. |

## Blocked

**`BLOCKED_SPEC_ALIVENESS_SUCCESS_FLOOR`** — the program-level minimum success
criterion. This is the number that says how much more alive FULL must appear
than the strong scripted baseline to justify building R003–R009. It is a
value judgement about what complexity is worth, and no amount of A000 evidence
produces it. Inventing one here would be exactly the fabrication D008 forbids,
and it would also be the number most likely to be quietly relaxed later.

Setting it is the architect's decision, and it must be set before Attempt 1.
