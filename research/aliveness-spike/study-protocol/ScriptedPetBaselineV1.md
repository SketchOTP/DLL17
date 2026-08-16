# ScriptedPetBaselineV1

- Status: `FROZEN` (implementation and rule set); competence **not** qualified
- Version: 1
- Implementation: `research/aliveness-spike/cohorts/.../Agents.kt` (`ScriptedPetAgent`)

The primary external comparator. FULL must beat something a person could
plausibly enjoy, so this is deliberately competent.

## Parity with FULL

Identical by construction, because they are the same objects rather than
matching ones:

| Dimension | Shared how |
|---|---|
| Creature appearance, camera, layout | one `SpikeExpressionContractV1` |
| Expression and micro-movement vocabulary | one frozen library |
| Action-to-presentation mapping | one pure function |
| Interaction vocabulary | one `InteractionKind` set |
| Affordances and habitat | one `Habitat` |
| Outcome model | one `OutcomeModel` |
| Timing and frame policy | one `SpikeRuntime`, one tick rate |
| Session duration and blinding | one `ViewerSession` |

`ViewerSessionTest` asserts that every cohort advances organism time at the
identical rate, presents the identical label and duration, and renders only
frozen vocabulary.

## What it has

- Authored drive analogues (hunger, fatigue, attention want, excitement) so it
  eats, sleeps, rests and seeks company visibly.
- A 14-entry authored idle and play script with seeded variety, covering every
  play object, both people, the shelter, the food source, the sealed gate and
  the aversive object.
- Context sensitivity: time of day gates sleep, object presence gates targets,
  and it alternates between the two social identities.
- Reactions to all six interaction kinds, with a reaction hold window.
- Short refractory behaviour on interaction seeking.

## What it deliberately does not have

No persistent learned preference, no conditioned fear, no extinction, no habit
strength, no episodic history, no relationship accumulation, no habituation, no
curiosity oscillator. `SpikeMechanismsTest` asserts its mechanism set is empty.
Its timers are authored, not acquired.

## Unchanged under D009

Neither scripted cohort was modified. The habitat and outcome-model changes made
under D009 — people keeping hours, the chime ringing at night, the metabolic cost
of vigorous activity — apply identically to every cohort, which is what parity
requires. Measured on a matched seed, habitat and window, the baseline's
behaviour is within noise of its D008 form: entropy 2.629 against 2.657, 9.18
distinct objects per day against 9.00, occupancy 0.344 against 0.344, inactivity
0.392 against 0.390. It was not weakened.

## DegradedScriptedControlV1

The deliberately weaker control used only for baseline competence
qualification: a 3-entry script, a 40-tick hold instead of 12, no context
sensitivity, no novelty response, and it ignores three of the six interaction
kinds. Measured over 40 virtual days it produced 1 distinct object inspected per
day and 0.70 inactivity share, against the strong baseline's 9 objects per day
and 0.39.

## Competence qualification status

**`APPROVED_FOR_HUMAN_QUALIFICATION_NOT_YET_QUALIFIED`.** Competence is
established only by the frozen 40-person blinded paired human qualification of
this comparator against `DegradedScriptedControlV1`. This is not a pilot-only
rater pool. No independently owned margin, `BaselineIndependentOwner`,
reviewer, model, committee or other discretionary qualification authority
exists; the deterministic result from that human experiment is the sole
qualification authority.

No human data exists, and this baseline has not passed qualification. If this
frozen version fails, that failure is preserved. No person, model, reviewer or
committee may rescue or override it.

## Freeze and versioning

The comparator, qualification protocol, competence instrument, presentation,
exclusions and analysis are frozen and hash-pinned before qualification human
data. The same version is used across scored attempts unless an interface or
parity change forces a new documented baseline version. Any material
strengthening requires fresh independent human qualification participants.
It may not reduce a previously qualified competence floor. The qualification
rule is mean paired competence difference at least `+15` with a 95% confidence
interval lower bound above zero.
