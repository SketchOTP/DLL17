# CuriosityBalanceEnvelopeV1

- Status: `FROZEN`
- Version: 1
- **Thresholds unchanged since D008.** The programme's single allowed
  threshold-only revision has not been spent and remains available.
- Attribution version: `SpontaneousActionAttributionV1` (thresholds unchanged) /
  `MechanismCoalitionSetV2` / `CoalitionValueFunctionV1` v1
- Feasibility result: `NON_EMPTY_FEASIBLE_REGION`
- Selected parameter hash: `8dcecc7b0adb4a52`
- Evidence: `research/aliveness-spike/evidence/CURIOSITY_ENVELOPE_SEARCH.txt`
- Superseded result: `EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE` under the D008
  candidate, retained in `research/aliveness-spike/evidence/negative/D008/`

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
feasiblePoints  = 27 / 27
robustPoints    = 27
attributionOnly = 27 / 27
antiConvergenceOnly = 27 / 27

CURIOSITY_ENVELOPE_FEASIBILITY_RESULT = NON_EMPTY_FEASIBLE_REGION
```

Every grid point satisfies both requirements on every seed, and every point is
robust under the one-step perturbation rule. The feasible region is the whole
searched space rather than an isolated corner of it, which is the difference
between a parameterization that works and one that was found by looking.

| Measure | Range across the grid | Requirement |
|---|---|---|
| Action-type entropy (bits) | 2.723 – 2.895 | ≥ 1.600 |
| Distinct objects inspected / day | 9.27 – 9.88 | ≥ 2.500 |
| Maximum single-action occupancy | 0.219 – 0.338 | ≤ 0.450 |
| Revisitation bouts / day | 112 – 166 | ≥ 3.000 |
| Cycle regularity | 0.254 – 0.345 | ≤ 0.550 |
| `SubstantiveSpontaneityRate` | 0.885 – 0.921 | ≥ 0.700 |
| `OscillatorTieBreakOnlyRate` | 0.024 – 0.048 | ≤ 0.200 |

The two criteria that failed at every point under D008 — maximum single-action
occupancy and cycle regularity — now clear their thresholds by wide margins, and
they did so without either threshold moving. The failure was in the mechanism.

## Selected parameterization

```
baseFloor           = 0.020000
contextAmplitude    = 0.080000
amplitudeSlew       = 0.002000
inhibitionDepth     = 0.060000
inhibitionRetention = 0.998600
periods             = 1009, 1493, 2161, 2909, 3701
parameterHash       = 8dcecc7b0adb4a52
```

Chosen as the most central robust point by distance from the grid edges, so the
operating point sits inside its region rather than on a boundary. With the whole
grid feasible this is a formality, but the rule is the one that would have
mattered had it not been.

## What made the difference

Not the oscillator. The curiosity parameters that were searched are the same
ones searched under D008, over the same grid and the same seed matrix. What
changed is the organism around them: rest stopped outranking everything at
night, activity acquired a metabolic cost and a satiation term, engagement with
one object became bounded, and a resumption defect that spent the budget
re-entering a known harm was fixed.

The D008 result was recorded as `EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE`
because an implementer may not decide between the threshold path and the
mechanism path. The architect took the mechanism path. It was the right call:
the thresholds were reachable.
