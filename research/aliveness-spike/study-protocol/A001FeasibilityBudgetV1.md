# A001FeasibilityBudgetV1

- Status: `READY_FOR_HUMAN_EVIDENCE`
- Version: 1
- Executable portion: `research/aliveness-spike/analysis/.../A001FeasibilityBudget.kt`
- Worked output: `research/aliveness-spike/evidence/A001_ACTIVATION_DRY_RUN.txt`

Turns the one number the variance pilot may release into the powered sample for
Attempt 1, and then answers one question: does it fit the owner's ceiling?

It answers `A001_NOT_FEASIBLE` rather than trimming the sample. An underpowered
scored attempt is worse than no attempt — it consumes one of three, and a null
result from it is uninterpretable.

---

## Frozen

| Decision | Value |
|---|---|
| Primary endpoint | `PairedAlivenessDifference = gradedAlivenessScore(FULL) − gradedAlivenessScore(ScriptedPetBaselineV1)` |
| Instrument | `GradedAlivenessInstrumentV1`, 0–100, one item per creature |
| Design | Paired, within-rater, one primary pair per rater |
| Session duration | 600 s per creature |
| Two-sided alpha | 0.05 |
| Power | 0.80 |
| Minimally worthwhile paired difference | 10.0 points |
| Pilot SD inflation factor | 1.25 |
| Variance source | `BlindVariancePilotV1`, released quantity `pairedDifferenceSd` only |
| Power model | Exact noncentral-t, not a normal approximation. At these sample sizes the two differ by whole participants. |
| Ablation arms | Powered independently at the most conservative Holm step, `alpha / 3`. Powering them at the uncorrected alpha would produce a family significant before correction and not after. |
| Rater pools | Disjoint, so total participants = primary pairs + 3 × per-arm pairs |
| Hard gate | required powered sample > frozen owner ceiling → `A001_NOT_FEASIBLE` → redesign before scored data |
| Underpowered studies | not a legitimate scored attempt; cannot be used to claim PASS or FAIL |

### Why 1.25 and 36 agree

The frozen inflation factor is exactly the one-sided 95% upper confidence bound
on a standard deviation at 35 degrees of freedom — that is, at 36 analysable
pairs. At 35 pairs the bound is 1.253 and the frozen factor no longer covers it.

That is why the pilot is registered at 36, over-recruits to replace dropouts,
and reports itself protocol-invalid below 36 rather than releasing a number the
inflation factor cannot defend. `BlindVariancePilotSealTest` verifies the two
constants against each other numerically rather than taking the arithmetic on
trust.

### Per-participant schedule

The participant-hour figure is built from the protocol's own itemized schedule,
not a round number:

| Step | Seconds |
|---|---|
| Consent | 300 |
| Briefing | 120 |
| Two sessions at 600 s | 1200 |
| Two instrument administrations | 240 |
| Secondary items | 60 |
| Debrief | 300 |
| **Total per participant** | **2220 (37 minutes)** |

These are protocol design values, not measurements. The owner may replace them
with observed session times once any session has been run.

---

## Blocked

Two inputs, and only two. Neither can be defaulted, and the calculator returns a
blocking state rather than a number when either is missing.

| Input | Block | Why it cannot be invented |
|---|---|---|
| `pairedDifferenceSd` | `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD` | The pilot has not run. Guessing it would set the sample size by assumption. |
| `maxFundableParticipants`, `maxParticipantHours` | `BLOCKED_SPEC_STUDY_BUDGET` | A funding and scheduling decision belonging to the owner. |

Everything else that was blocked under D008 is now frozen: the minimally
worthwhile difference, alpha, power, the variance inflation rule, the exclusion
and technical-failure rules, and the instrument wording and anchors.

## States

| State | Meaning |
|---|---|
| `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD` | No pilot release, or a release that is not protocol-valid |
| `BLOCKED_SPEC_STUDY_BUDGET` | The powered requirement is computable; no ceiling exists to test it against |
| `A001_FEASIBLE` | The requirement fits both halves of the ceiling |
| `A001_NOT_FEASIBLE` | It exceeds either half. Redesign; do not trim. |

Either half of the ceiling can block on its own: a study that fits the
participant count but not the participant-hours is still infeasible.
