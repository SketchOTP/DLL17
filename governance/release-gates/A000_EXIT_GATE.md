# A000 exit gate

- Track: A000 — disposable aliveness spike research track
- Directives: D008 (build), D009 (mechanism remediation)
- Fixture set: `A000-FIXTURES-V1` version 3
- Golden evidence digest:
  `9462e43622c414db47c28a2e79452455bc0d6642396dd5ca8d65bae208b3114a`
- Evaluated: 2026-08-13, reconfirmed 2026-08-13 under D010

> **Two corrections recorded under D010.**
>
> 1. The fixture set moved to version 3 and the digest changed, because D010
>    added the preregistered third human ablation arm and the cohort-parity
>    fixture covers every cohort. All 24 findings still hold and no organism
>    behaviour changed: the only altered readout is `cohorts=6` becoming
>    `cohorts=7`.
> 2. Several figures quoted in this gate and in the D009 completion report came
>    from an intermediate kernel run rather than the frozen one. Every finding,
>    direction and verdict was and remains correct; the decimals below are now
>    reconciled against `qualification/fixtures/A000/A000_REPORT.txt`.

A000 is a research track, not a production phase. Its job is to find out whether
the organism hypothesis is worth testing on people, and a negative answer is a
successful A000. D008 built the track and returned five negative findings and an
empty curiosity feasible region. D009 revised the candidate against those
findings under **unchanged** thresholds.

---

## Required A000 artifacts

| Artifact | Status |
|---|---|
| Accelerated simulator | Built. 24 named fixtures over roughly 3,400 virtual days per run. |
| Real-time observer viewer | Built. Blinded, fixed-duration, one shared presentation contract, normalized study-event log. |
| Abstract habitat | 12 abstract affordances, now with circadian structure so context-conditioned memory has something to condition on. |
| Cohorts | Seven after D010, all runnable. |
| Decision traces | `SpikeDecisionTraceV1`, emitted for every scored autonomous action. |
| Coalition attribution | Exact Shapley over six groups, 64 coalitions per scored action. |
| Curiosity balance envelope | Evaluated twice: 27 grid points × 4 seeds, before and after remediation. |
| Baseline coverage manifest | Added under D010. Generated from the comparator implementation itself. |

---

## Accelerated findings

**24 findings, 24 held.** Every readout is in
`qualification/fixtures/A000/A000_REPORT.txt` and contributes to the digest.

### The four D008 failures, resolved

| Requirement | D008 | D009 |
|---|---|---|
| Mean population differentiation | 0.052 | **0.166** |
| Closest-pair differentiation | 0.019, below the 0.050 floor | **0.077**, above it |
| Final-window diversity | 0.034, below the 0.050 floor | **0.136**, above it |
| History-derived divergence | 0.325 | **0.610** |
| Re-sampling a rejected option | never | **0.0 → 31.0 eats/day**, first return 7 ticks after reversal |
| Episodic contribution | reduced divergence | mechanism **removed** from FULL |
| Curiosity feasible region | `EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE`, 0 of 27 | **`NON_EMPTY_FEASIBLE_REGION`, 27 of 27 robust** |
| Anti-convergence, occupancy | 0.468, above the 0.450 ceiling | **0.361** |
| Anti-convergence, cycle regularity | 0.924, above the 0.550 ceiling | **0.366** |
| Anti-convergence, entropy | 1.589, below the 1.600 floor | **2.735** |

### Full finding list

| Finding | Result |
|---|---|
| `AX-BOUNDEDNESS-01` | 365 days × 3 conditions: zero overflow, constant 1,150-slot footprint, all values in bounds |
| `AX-DETERMINISM-01` | Two identical runs agree on signature, per-tick actions and attribution |
| `AX-DIFFERENTIATION-01` | mean pairwise TV 0.166, 6/6 distinct signatures |
| `AX-DIFFERENTIATION-02` | closest pair 0.077, widest 0.296 |
| `AX-PREFERENCE-01` | pooled over 4 seeds: mean preference 0.399 for the high-payoff object, 0.000 for the null-payoff one |
| `AX-PREFERENCE-REVERSAL-01` | adopted preference 0.663 → 0.025 across the reversal |
| `AX-PREFERENCE-REVERSAL-02` | the alternative rises 0.000 → 0.748 |
| `AX-AVOIDANCE-01` | peak fear 0.648, 12,006 ticks above the avoidance threshold |
| `AX-EXTINCTION-01` | 0.401 → 0.192 once the object becomes safe |
| `AX-HABITUATION-01` | 0.400 at exposure end, 0.071 after five days of non-exposure |
| `AX-DISHABITUATION-01` | a causal change releases the remaining trace |
| `AX-SENSITIZATION-01` | 0.000 → 0.320 on one strong negative event |
| `AX-HABIT-01` | habit follows contingency: 0.816 → 0.004 on the source that stops paying, 0.000 → 0.735 on the one that starts |
| `AX-REEXPLORATION-01` | 0.0 → 31.0 eats/day at the rejected source; first return 7 ticks after the reversal |
| `AX-REEXPLORATION-02` | a real reallocation: 27.6 → 0.0 at the devalued source, and preference follows (0.026 against 0.727) |
| `AX-EPISODIC-01` | mean action divergence 0.610 over 5 seeds under matched present stimuli |
| `AX-EPISODIC-02` | adding episodic recall back does not help; disposition `REMOVED` |
| `AX-ANTICONVERGENCE-01` | all five criteria pass: entropy 2.735, 9.71 objects/day, occupancy 0.361, 138.8 revisits/day, regularity 0.366 |
| `AX-ANTICONVERGENCE-02` | mechanism load-bearing: 9.71 objects/day with it, 6.10 without |
| `AX-NOVELTY-SATURATION-01` | no permanent saturation over 180 static days |
| `AX-CONVERGENCE-01` | mean final-window TV 0.136, closest pair 0.044 |
| `AX-ATTRIBUTION-01` | substantive 0.891 against a 0.700 floor; oscillator/tie-break-only 0.028 against a 0.200 ceiling, over 2,154 scored actions |
| `AX-TRACE-01` | every scored spontaneous action carries a causal trace |
| `AX-COHORT-PARITY-01` | all seven cohorts render through the frozen contract using only its vocabulary |

---

## Curiosity balance envelope

`CURIOSITY_ENVELOPE_FEASIBILITY_RESULT = NON_EMPTY_FEASIBLE_REGION`

27 of 27 grid points jointly feasible on every seed; 27 robust under the
one-step perturbation rule. **No threshold was changed.** The searched grid, the
seed matrix, the fixture and the requirements are identical to D008's. What
changed is the organism.

The programme's single allowed threshold-only revision has not been spent.

---

## Episodic history: removed

Revised first, then removed on evidence. The revision made it
context-conditioned, changed it to contribute only the residual over the
context-free preference, gave it salience-based rather than recency-based
retention, and gave the habitat circadian structure so a context-conditioned
memory had something to learn. Measured across a five-seed matrix, adding it
back to FULL still did not increase history-dependent individuality.

History dependence itself is unaffected and much stronger than under D008:
preference, habit, skill, fear, relationship value and outcome uncertainty are
all history-derived, and matched-stimulus divergence rose from 0.325 to 0.610.

The human leave-one-out family was two arms at the close of D009. The architect
preregistered `FULL − outcome uncertainty / directed re-exploration` as the
replacement third arm under D010, so Holm-Bonferroni corrects over three.

---

## What A000 still has not established

1. **No human evidence exists.** Nothing here says FULL appears more alive than
   the scripted baseline.
2. **The scripted baseline is not competence-qualified**, and was not weakened:
   its measures are within noise of D008 and it remains far stronger than the
   degraded control.
3. **No reviewer is assigned.** All three roster roles are blank. This remains
   true after D010, which prepared the onboarding package but cannot supply
   people.
4. **The habitat is abstract.** Twelve affordances, no space, no navigation, no
   sensors.
5. **The measures are the ones chosen.** Cycle regularity and single-action
   occupancy are constructs invented for this track, and the envelope result
   turns on both — in both directions, now.

---

## A000 = COMPLETE

Both required artifacts exist, six cohorts run through one presentation
contract, exact coalition attribution is executable and evidenced, all 24
accelerated findings hold, and the joint curiosity envelope is non-empty under
unchanged thresholds.

A001 cannot begin. After D010 the activation gate names five outstanding
blockers, none of which code can clear: an unqualified baseline, an unregistered
variance pilot, the SD that pilot would release, three unassigned reviewer roles,
and an owner resource ceiling.
