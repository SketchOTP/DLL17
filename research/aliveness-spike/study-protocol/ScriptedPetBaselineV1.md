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

## DegradedScriptedControlV1

The deliberately weaker control used only for baseline competence
qualification: a 3-entry script, a 40-tick hold instead of 12, no context
sensitivity, no novelty response, and it ignores three of the six interaction
kinds. Measured over 40 virtual days it produced 1 distinct object inspected per
day and 0.70 inactivity share, against the strong baseline's 9 objects per day
and 0.39.

## Competence qualification status

**Not qualified.** `BaselineQualificationProtocolV1` requires a pilot-only human
rater pool and an independently owned margin. No human data exists, and
`BaselineIndependentOwner` is unassigned. D008 explicitly does not claim this
baseline has passed its competence qualification, and this document does not
either.

## Freeze and versioning

After competence qualification, the contingency table, script, hold windows,
thresholds and expression-contract version are hashed and pinned. The same
qualified version is used across scored attempts unless an interface or parity
change forces a new one; a new version requires independent review and
requalification and may not reduce the previously qualified competence floor.
