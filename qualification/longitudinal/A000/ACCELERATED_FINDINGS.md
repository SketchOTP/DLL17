# A000 accelerated research findings

> **Superseded content notice.** This document described the D008 candidate,
> which was rejected. It now describes the D009 candidate. The D008 results are
> retained verbatim in `research/aliveness-spike/evidence/negative/D008/` and are
> not superseded by anything here.

What the candidate mechanism architecture actually did, over roughly 2,900
virtual days per full kernel run. Numbers are from
`qualification/fixtures/A000/A000_REPORT.txt`.

## Boundedness and stability

| Property | Result |
|---|---|
| Arithmetic overflow over 365 days × 3 conditions | 0 |
| State footprint, start versus end | 751 slots, unchanged |
| Learned values outside their bounds | none |
| Range clamps | ~900k per 365-day run, all intended: a drive pinned at its bound clamps every tick |

The clamp count is reported separately from overflow deliberately. Counting
clamps as instability would have made a healthy organism look broken; counting
overflow as normal would have hidden the failure that matters.

## Behavioural differentiation

Six seeds, 60 static days each. Mean pairwise total-variation distance between
action distributions 0.052, range 0.019–0.103, with six distinct final state
signatures.

The distribution is what carries individuality. The closest pair sits below the
0.050 floor, so two organisms can end up behaviourally similar even with
different traits and different histories.

## Learning

| Mechanism | Evidence |
|---|---|
| Preference formation | 0.791 for the high-payoff play object against 0.000 for the null-payoff one, under matched exposure |
| Preference weakening | adopted food preference 0.208 → 0.090 across a contingency reversal |
| Conditioned avoidance | fear peaked at 0.447; 1,632 ticks above the avoidance threshold, during which approach, play and explore were ineligible |
| Extinction | fear declines on safe evidence, bounded below at 30% of its historical peak; slow forgetting then takes it lower over days |
| Habituation | 0.940 peak, 0.400 at exposure end, 0.071 after five days of non-exposure |
| Dishabituation | a causal environmental change releases the remaining trace |
| Sensitization | 0.000 → 0.320 on one strong negative event |
| Habit | tracks contingency: the newly reliable source went 0.291 → 0.572, the newly unreliable one did not rise |
| Relationship | attentive person 0.997, inattentive person −0.011, from interaction outcomes only |

Two of the three learning laws were rewritten during A000. Preference and habit
were first written as accumulators, which pinned anything with net-positive
outcome at the bound and destroyed all discrimination between a reliable and an
unreliable source. Both are now error-corrected estimates moving toward the
observed outcome. The accumulator form is a mechanism-design finding in its own
right: it cannot represent reliability at all.

## History dependence

Two organisms with the same seed — identical traits, identical tie-break stream
— conditioned on different food contingencies and different social
responsiveness, then placed in an identical probe:

| Measure | FULL | FULL − episodic |
|---|---|---|
| Probe ticks differing in action | 32.5% | 40.3% |
| Probe ticks differing in target | 40.2% | — |

History dependence is real and large. But removing episodic recall *increased*
it. Episodic influence is acting as a stabiliser, pulling differently-conditioned
organisms back toward common behaviour, which is the opposite of its intended
role in the aliveness thesis.

## Anti-convergence

180 static days, no new objects, no changes.

| Measure | FULL | FULL − curiosity anti-convergence | Requirement |
|---|---|---|---|
| Distinct objects inspected / day | 5.06 | 1.00 | ≥ 2.50 |
| Revisitation bouts / day | 32.1 | 0.00 | ≥ 3.00 |
| Action-type entropy (bits) | 1.589 | — | ≥ 1.600 |
| Max single-action occupancy | 0.468 | — | ≤ 0.450 |
| Max waking-action occupancy | 0.784 | — | not a frozen requirement |
| Cycle regularity | 0.924 | — | ≤ 0.550 |

The mechanism is unambiguously load-bearing: without it the organism inspects
one object and never returns to anything. With it, the organism keeps exploring
but becomes highly regular about how.

## Spontaneity attribution

1,363 scored spontaneous actions, exact Shapley over six mechanism groups, 64
coalition evaluations each.

| Class | Share |
|---|---|
| `MIXED_SUBSTANTIVE` | dominant |
| `SOCIAL_OR_RELATIONSHIP_HISTORY`, `EPISODIC_OR_HISTORY`, `HABIT_OR_EXPECTANCY`, `LEARNED_PREFERENCE`, `PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE` | the remainder of the substantive mass |
| `CURIOSITY_OSCILLATOR_ONLY` + `RANDOM_TIEBREAK_ONLY` | 1.0% |

`SubstantiveSpontaneityRate = 0.946`, `OscillatorTieBreakOnlyRate = 0.010`.

Most spontaneous actions are overdetermined: several mechanisms independently
support the same choice. That is why coalition attribution matters — single
knockouts would have classified those actions as unexplained.

Surprise without randomness holds on this evidence. Spontaneous revisitation is
attributable to internal state, history and learning, not to the oscillator and
not to tie-breaking.

## Population convergence

Eight organisms, different seeds, 60 static days, compared on their final
20-day action distributions. Mean pairwise total variation 0.034 against a 0.050
floor; closest pair 0.006.

Long-run policies partially converge. Together with the closest-pair
differentiation result, this is the most substantive limitation A000 found: the
mechanism set produces individual histories but not reliably individual long-run
behaviour.

## Cohort comparison, three days with scheduled interaction

| Cohort | Distinct expression frames | Postures used | Mechanisms |
|---|---|---|---|
| FULL | 99 | 4 | 12 |
| `ScriptedPetBaselineV1` | 1,606 | 6 | 0 |
| `DegradedScriptedControlV1` | 405 | 4 | 0 |
| FULL − curiosity anti-convergence | 97 | 5 | 10 |
| FULL − preference learning | 1,164 | 6 | 11 |
| FULL − episodic history | 158 | 5 | 11 |

Recorded because it is uncomfortable and relevant. Over a short window the
scripted baseline produces far more surface variety than FULL, because it rotates
a broad authored script while FULL commits to actions and sleeps through the
night. Whether surface variety or coherent motivation reads as more alive to a
person is exactly the question A001 exists to answer, and A000 cannot settle it.
It does mean the primary comparison will not be an easy win.

---

# D009 candidate — what changed and what it did

## The four remediated failures

| Measure | D008 | D009 | Floor / ceiling |
|---|---|---|---|
| Mean population differentiation | 0.052 | 0.163 | ≥ 0.050 |
| Closest-pair differentiation | 0.019 | 0.074 | ≥ 0.050 |
| Final-window diversity | 0.034 | 0.103 | ≥ 0.050 |
| Matched-stimulus history divergence | 0.325 | 0.610 | ≥ 0.050 |
| Rejected option re-sampled, eats/day | 0.00 | 32.5 | > before |
| Action-type entropy, static habitat | 1.589 | 2.714 | ≥ 1.600 |
| Maximum single-action occupancy | 0.468 | 0.357 | ≤ 0.450 |
| Cycle regularity | 0.924 | 0.369 | ≤ 0.550 |
| Distinct objects inspected / day | 5.06 | 9.74 | ≥ 2.500 |
| Curiosity feasible points | 0 / 27 | 27 / 27 | ≥ 1 robust |

## Where the individuality comes from

Not from noise. The organism draws on one seeded random substream, consulted
only among candidates that are already near-equal after every biological and
learned term, and it accounts for 2.7% of scored spontaneous actions.

It comes from three compounding sources:

1. **Skill.** Competence grows with validated practice and fades with disuse, so
   an organism becomes good at what it happened to do and then does more of it.
2. **Differential trait blends.** Each epistemic action draws on a different pair
   of traits, so a cautious watcher and a bold explorer produce different
   *action-type* budgets rather than different amounts of the same budget.
3. **Per-organism need thresholds.** How much company an individual needs before
   it becomes a Tier 3 concern is a property of that individual.

The history-dependence fixture isolates this from constitution entirely: two
organisms with the *same seed* — identical traits, identical tie-break stream —
diverge on 61% of probe ticks after different lived histories.

## How re-exploration works

An option's uncertainty falls when it is sampled and behaves as expected, rises
slowly while it is neglected, and jumps when an outcome contradicts the
expectation. Exploration appetite rises with frustration, falls with stress, and
is damped by fear of the specific object.

In the controlled reversal protocol the organism ate at the reliable source 31.1
times a day and at the rejected one 0.13. Twenty ticks after the contingency
flipped it returned to the rejected source, and by the final window it was eating
there 32.5 times a day and at the old source not at all. Preference followed:
0.036 for the abandoned source against 0.681 for the readopted one.

Nothing in that sequence is random. The trace names the driver.

## Why episodic history was removed

It was revised substantially first: context-conditioned recall matched on
circadian quarter, contributing only the residual over the context-free
preference; salience-based retention so a strongly-valenced early experience
survives instead of being overwritten by the present; and circadian structure
added to the habitat so a context-conditioned memory had a real conjunction to
learn.

Measured over five seeds, adding it back to FULL changed matched-stimulus
divergence from 0.610 to 0.605 and helped in three seeds of five. That is not a
positive contribution, and the directive is explicit that a mechanism is not kept
because episodic memory is theoretically desirable.

The likely reason, recorded because it may matter to R007: everything the
mechanism could contribute in this habitat, the context-free preference already
carried. Episodic memory needs conjunctions that a scalar per-object value cannot
represent, and a twelve-affordance habitat with one contingency per object does
not have many.

## Cohort comparison after remediation

Forty virtual days, matched seed and habitat, fifteen-day window.

| Cohort | Entropy | Objects/day | Occupancy | Inactivity | Regularity |
|---|---|---|---|---|---|
| FULL | 2.763 | 10.15 | 0.341 | 0.243 | 0.358 |
| `ScriptedPetBaselineV1` | 2.629 | 9.18 | 0.344 | 0.392 | 0.633 |
| `DegradedScriptedControlV1` | 1.377 | 1.00 | 0.584 | 0.703 | 0.775 |

Under D008 the scripted baseline produced more surface variety than FULL over a
short window. It no longer does: FULL now leads on every diversity measure while
the baseline is unchanged from its D008 form. Whether that difference reads as
*aliveness* to a person is still the question A000 cannot answer.
