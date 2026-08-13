# SpontaneousActionAttributionV1

- Status: `FROZEN`
- Version: 1
- Depends on: `MechanismCoalitionSetV1` v1, `CoalitionValueFunctionV1` v1

## Classes

```
PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE
LEARNED_PREFERENCE
EPISODIC_OR_HISTORY
HABIT_OR_EXPECTANCY
SOCIAL_OR_RELATIONSHIP_HISTORY
CURIOSITY_OSCILLATOR_ONLY
RANDOM_TIEBREAK_ONLY
MIXED_SUBSTANTIVE
OTHER_NON_SUBSTANTIVE
```

The first five and `MIXED_SUBSTANTIVE` count as substantive.

## Frozen thresholds

| Threshold | Value |
|---|---|
| Substantive share floor | 0.50 |
| Dominant single-group share | 0.50 |
| Mixed-substantive minimum per-group share | 0.15 |
| Oscillator dominance share | 0.50 |

## Classification order

1. Total positive Shapley mass is zero → `RANDOM_TIEBREAK_ONLY` if a tie-break decided the action, else `OTHER_NON_SUBSTANTIVE`.
2. A tie-break decided it and substantive share is below the floor → `RANDOM_TIEBREAK_ONLY`.
3. Oscillator share at or above dominance and substantive share below the floor → `CURIOSITY_OSCILLATOR_ONLY`.
4. Substantive share below the floor → `OTHER_NON_SUBSTANTIVE`.
5. A single substantive group at or above the dominant share → that group's class.
6. At least two substantive groups each at or above the mixed minimum → `MIXED_SUBSTANTIVE`.
7. Otherwise → `OTHER_NON_SUBSTANTIVE`.

Overdetermination reaches step 6 and counts as substantive, which is the point
of moving from single knockouts to coalition attribution: an action supported
independently by three mechanisms is well explained, not unexplained.

## Rates

```
SubstantiveSpontaneityRate =
  spontaneous scored actions in a substantive class / all spontaneous scored actions

OscillatorTieBreakOnlyRate =
  spontaneous scored actions in CURIOSITY_OSCILLATOR_ONLY or RANDOM_TIEBREAK_ONLY
  / all spontaneous scored actions
```

## Measured on the A000 candidate

Over 1,363 scored spontaneous actions (60 virtual days, static habitat,
1-in-5 sampling): `SubstantiveSpontaneityRate = 0.946`,
`OscillatorTieBreakOnlyRate = 0.010`. Across the whole 27-point envelope grid
and 4-seed matrix the substantive rate ranged 0.922–0.966 and the
oscillator/tie-break-only rate 0.005–0.029.
