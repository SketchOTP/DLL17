# A000 accelerated research findings

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
