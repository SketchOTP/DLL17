# CuriosityBalanceEnvelopeV1

- Status: `FROZEN`
- Version: 1
- Attribution version: `SpontaneousActionAttributionV1` v1 / `CoalitionValueFunctionV1` v1
- Feasibility result: `EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE`
- Evidence: `research/aliveness-spike/evidence/CURIOSITY_ENVELOPE_SEARCH.txt`

The curiosity oscillator is both the anti-convergence mechanism and the most
likely source of hollow pseudo-spontaneity. This contract binds both constraints
to one parameterization so neither can be satisfied at the other's expense.

## Frozen parameter space

| Parameter | Grid |
|---|---|
| Curiosity base floor | 0.02, 0.04, 0.08 |
| Context amplitude | 0.08, 0.15, 0.26 |
| Recent-inspection inhibition depth | 0.06, 0.12, 0.22 |
| Inhibition retention per tick | 0.998600 (fixed) |
| Amplitude slew per tick | 0.002000 (fixed) |
| Co-prime periods (ticks) | 1009, 1493, 2161, 2909, 3701 (fixed) |

27 grid points. Seed matrix `{1001, 2003, 3005, 4007}`. Static habitat, 40
virtual days per run, 15-day measurement window, attribution sampled 1 in 5.

## Frozen requirements

Anti-convergence, all five:

| Requirement | Threshold |
|---|---|
| Window action-type entropy | ≥ 1.600 bits |
| Distinct objects inspected per day | ≥ 2.500 |
| Maximum single-action occupancy | ≤ 0.450 |
| Revisitation bouts per day | ≥ 3.000 |
| Cycle regularity | ≤ 0.550 |

Attribution, both:

| Requirement | Threshold |
|---|---|
| `SubstantiveSpontaneityRate` | ≥ 0.700 |
| `OscillatorTieBreakOnlyRate` | ≤ 0.200 |

Every threshold above was frozen in source before the search ran.

## Joint evaluation

Both readouts come from the **same run** at each grid point and seed. There is
no path by which anti-convergence could be measured on one oscillator
configuration and attribution on another, because only one run exists per pair.

## Robustness rule

A feasible point counts only if it is feasible on every seed **and** at least
half of its immediate grid neighbours are feasible. An isolated point that
vanishes under a one-step perturbation is not an operating region.

## Result

```
feasiblePoints  = 0 / 27
robustPoints    = 0
attributionOnly = 27 / 27
antiConvergenceOnly = 0 / 27

CURIOSITY_ENVELOPE_FEASIBILITY_RESULT = EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE
```

The failure is uniform and specific. Attribution passed at every point with wide
margin. Anti-convergence failed at every point on exactly two of its five
criteria — maximum single-action occupancy (measured 0.462–0.477 against a 0.450
ceiling) and cycle regularity (0.803–0.876 against a 0.550 ceiling) — while
entropy, distinct objects per day and revisitation passed at every point.

Under `CuriosityEnvelopeFeasibilityV1` an empty set does not by itself decide
between an incompatible threshold pair and a mechanism failure. That decision
belongs to the independent gate reviewer, so the search reports the candidate
result and stops. The thresholds have not been altered, and altering them is not
the implementer's decision to make.
