# A000 exit gate

- Track: A000 — disposable aliveness spike research track
- Directive: D008
- Fixture set: `A000-FIXTURES-V1` version 1
- Golden evidence digest:
  `4765e6d587347688841d34c95b5b9caede8cbf44084335302e1475c7aeaa8fc9`
- Evaluated: 2026-08-13

A000 is a research track, not a production phase. It has no pass/fail exit gate
in the sense R000, R001 and R002 do: its job is to find out whether the organism
hypothesis is worth testing on humans, and a negative answer is a successful
A000. What follows is therefore a record of what the track produced and what it
established, not a list of criteria that had to pass.

---

## Required A000 artifacts

| Artifact | Status |
|---|---|
| Accelerated simulator | Built. `research/aliveness-spike/accelerated-sim/`. Deterministic, seeded, 22 named fixtures over roughly 2,900 virtual days per full run. |
| Real-time observer viewer | Built. `research/aliveness-spike/realtime-viewer/`. Swing, blinded, fixed-duration, one shared presentation contract, normalized study-event log. |
| Abstract habitat | Built. 12 abstract affordances covering resource, shelter, two social identities, four play objects, one aversive object, one blocked affordance and one controlled-novelty slot. |
| Cohorts | Six, all runnable. |
| Decision traces | `SpikeDecisionTraceV1`, emitted for every scored autonomous action. |
| Coalition attribution | Exact Shapley over six groups, 64 coalitions per scored action. |
| Curiosity balance envelope | Evaluated. 27 grid points × 4 seeds. |

---

## Accelerated findings

22 findings. 17 held, 5 did not. Every readout is in
`qualification/fixtures/A000/A000_REPORT.txt` and every one contributes to the
digest above.

### Held

| Finding | Result |
|---|---|
| `AX-BOUNDEDNESS-01` | 365 virtual days × 3 habitat conditions: zero arithmetic overflow, constant 751-slot state footprint, every learned value inside its bound. |
| `AX-DETERMINISM-01` | Two runs of the identical configuration agree on state signature, per-tick action sequence and attribution counts. |
| `AX-DIFFERENTIATION-01` | Mean pairwise total-variation distance 0.052 over six seeds; 6/6 distinct final state signatures. |
| `AX-PREFERENCE-01` | Under matched forced exposure, preference tracks payoff: 0.791 for the high-payoff object against 0.000 for the null-payoff object. |
| `AX-PREFERENCE-REVERSAL-01` | The adopted preference falls from 0.208 to 0.090 once its contingency reverses. |
| `AX-AVOIDANCE-01` | Conditioned avoidance acquired to a peak fear of 0.447, with 1,632 ticks spent above the avoidance threshold. |
| `AX-EXTINCTION-01` | Fear declines once the object becomes safe, bounded below by the extinction residual and then by slow forgetting. |
| `AX-HABITUATION-01` | Trace builds to 0.940, sits at 0.400 at the end of exposure, recovers to 0.071 after five days of non-exposure. |
| `AX-DISHABITUATION-01` | A causal environmental change releases the remaining trace. |
| `AX-SENSITIZATION-01` | A strong negative event raises responsiveness from 0.000 to 0.320. |
| `AX-HABIT-01` | Habit follows the contingency: the source that becomes reliable gains (0.291 → 0.572) and the one that stops being reliable does not. |
| `AX-EPISODIC-01` | Divergent histories change later behaviour under matched present stimuli: 32.5% of probe ticks differ in action, 40.2% in target. |
| `AX-ANTICONVERGENCE-02` | The anti-convergence mechanism is load-bearing on objective measures: 5.06 distinct objects per day with it, 1.00 without; 32.1 revisitation bouts per day with it, 0.00 without. |
| `AX-NOVELTY-SATURATION-01` | No permanent novelty saturation over 180 static days. |
| `AX-ATTRIBUTION-01` | `SubstantiveSpontaneityRate` 0.946 against a 0.700 floor; `OscillatorTieBreakOnlyRate` 0.010 against a 0.200 ceiling, over 1,363 scored spontaneous actions. |
| `AX-TRACE-01` | Every scored spontaneous action carries a trace with a causal attribution. |
| `AX-COHORT-PARITY-01` | All six cohorts render through the frozen contract using only its vocabulary. |

### Did not hold

These are results, not defects. Each is preserved with its configuration.

| Finding | Result | Reading |
|---|---|---|
| `AX-DIFFERENTIATION-02` | Closest pair of organisms differs by only 0.019 total variation, against a 0.050 floor. Widest pair differs by 0.103. | Individuality is a property of the population, not a guarantee about any two organisms. Two seeds can land on nearly the same action budget. |
| `AX-PREFERENCE-REVERSAL-02` | The devalued preference falls, but the alternative source is never re-sampled: it stays at −0.010 across the reversal. | Devaluation works; **switching** does not follow from it. Nothing in the candidate set drives re-exploration of an option already written off. This is a real gap in the mechanism set. |
| `AX-EPISODIC-02` | Removing episodic history *increases* history-dependent divergence, 0.403 against FULL's 0.325. | Episodic recall is acting as a stabiliser rather than a differentiator. Under Principle 11 this is evidence to simplify or remove the mechanism, not to protect it. |
| `AX-ANTICONVERGENCE-01` | Over 180 static days: entropy 1.589 against a 1.600 floor, maximum single-action occupancy 0.468 against a 0.450 ceiling, cycle regularity 0.924 against a 0.550 ceiling. Distinct objects per day (5.06) and revisitations per day (32.1) passed. | The organism does not die behaviourally, but it becomes highly regular. Two of the five criteria are the same two that fail across the entire envelope grid. |
| `AX-CONVERGENCE-01` | Mean final-window total variation across eight organisms is 0.034 against a 0.050 floor; the closest pair is 0.006. | The population partially converges toward a common long-run policy. Combined with `AX-DIFFERENTIATION-02` this is the clearest limitation A000 found. |

---

## Curiosity balance envelope

`CURIOSITY_ENVELOPE_FEASIBILITY_RESULT = EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE`

27 grid points over curiosity base floor, context amplitude and inspection
inhibition depth, each evaluated on a four-seed matrix, each producing both
readouts **from the same run**.

| Outcome | Count |
|---|---|
| Jointly feasible | 0 / 27 |
| Robustly feasible | 0 |
| Attribution requirement satisfied | 27 / 27 |
| Anti-convergence requirement satisfied | 0 / 27 |

The failure is uniform and specific. Attribution passed everywhere with wide
margin (substantive 0.922–0.966, oscillator-only 0.005–0.029). Anti-convergence
failed everywhere on exactly two of five criteria — maximum single-action
occupancy 0.462–0.477 against a 0.450 ceiling, and cycle regularity 0.803–0.876
against a 0.550 ceiling — while entropy, distinct objects per day and
revisitation passed at every point.

Under `CuriosityEnvelopeFeasibilityV1` an empty set does not by itself
distinguish an incompatible threshold pair from a mechanism failure, and that
determination belongs to the independent gate reviewer. The thresholds were
frozen in source before the search ran and have not been altered. No
parameterization was selected.

---

## What A000 did not establish

1. **No human evidence exists.** No participant has seen either cohort. Nothing
   here says FULL appears more alive than the scripted baseline; A000 cannot
   answer that question and did not try.
2. **The scripted baseline is not competence-qualified.** It is implemented,
   frozen and objectively stronger than the degraded control, but its
   competence endpoint requires a pilot rater pool and an independent owner.
3. **No reviewer is assigned.** `IndependentReviewRosterV1` is
   `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED` on all three roles.
4. **The habitat is abstract.** Twelve affordances, no space, no navigation, no
   sensors. Behaviour observed here is behaviour in that habitat.
5. **The measures are the ones chosen.** Cycle regularity and single-action
   occupancy in particular are constructs invented for this track, and the
   envelope result turns on both of them.

---

## A000 = COMPLETE, with a decision required before A001

Both required artifacts exist, all six cohorts run through one presentation
contract, exact coalition attribution is executable and evidenced, the
accelerated histories produced a documented viability result including five
negative findings, and the curiosity envelope was actually evaluated rather than
asserted.

A001 cannot begin. Two blockers are structural rather than technical: the
reviewer roster is unassigned, and the empty curiosity-envelope feasible set
requires the independent reviewer's threshold-versus-mechanism determination.
