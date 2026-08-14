# MechanismCoalitionSetV2

- Status: `FROZEN`
- Version: 2 (supersedes v1; v1 remains the version D008 evidence was produced under)
- Implementation: `research/aliveness-spike/cohorts/.../Organism.kt` (`MechanismGroup`)

The causal mechanism groups used for scored spontaneity attribution.

## The six frozen groups

| Ordinal | Group | Mechanisms it carries |
|---|---|---|
| 0 | `PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE` | drives, modulators, traits, circadian state, tiered commitment |
| 1 | `LEARNED_PREFERENCE` | preference formation, conditioned fear, extinction, habituation, sensitization |
| 2 | `EPISODIC_OR_HISTORY` | bounded episodic recall — **carried by no mechanism in FULL after D009** |
| 3 | `HABIT_OR_EXPECTANCY` | habit strength, reward expectancy, **skill proficiency**, **outcome uncertainty** |
| 4 | `SOCIAL_OR_RELATIONSHIP_HISTORY` | per-person relationship value |
| 5 | `CURIOSITY_OSCILLATOR` | object-specific phase drift, recent-inspection inhibition, absolute salience shift |

`k = 6`, so `2^k = 64` and exact exhaustive enumeration is required. The initial
A-track does not use a sampled approximation, and adding a seventh group would
require a separate reviewed approximation and error-bound contract.

## Why grouping is coarser than the mechanism list

The cohort ablations operate on individual mechanisms; the coalition set
operates on groups. They are deliberately different granularities. Ablating
preference learning must not also remove conditioned fear, but for causal
attribution both are stimulus-value learning, and separating them would push the
group count past the exact-enumeration ceiling.

## What changed in v2, and what did not

Two mechanisms added under D009 — skill proficiency and outcome uncertainty —
join `HABIT_OR_EXPECTANCY`. All three are statements about what *doing this here*
is worth, and giving each its own group would push the set past the
exact-enumeration ceiling for no analytic gain.

Episodic recall left FULL under D009, so `EPISODIC_OR_HISTORY` is now carried by
no mechanism in the qualified set. The group is **retained** rather than removed:
the `FULL+episodic-history` cohort still exercises it, and dropping a group would
change the game's shape.

Unchanged in v2: the six groups themselves, their ordinals, the coalition
ordering, `CoalitionValueFunctionV1`, and every attribution threshold in
`SpontaneousActionAttributionV1`. Membership moved; the game did not. Attribution
rates before and after are therefore comparable in kind, and both are reported.

## Coalition ordering

Coalitions are enumerated as bitmasks over the ordinals above, ascending. The
ordering is recorded even though the Shapley weights depend only on coalition
size, so a future implementation can be compared directly rather than
approximately.
