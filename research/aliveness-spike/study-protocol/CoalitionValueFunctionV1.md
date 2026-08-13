# CoalitionValueFunctionV1

- Status: `FROZEN`
- Version: 1
- Implementation: `research/aliveness-spike/cohorts/.../Attribution.kt`

## The game

For an observed spontaneous action `a*`, the frozen group set `M`, and a
coalition `S ⊆ M`, the controller is recomputed from the identical pre-action
state with only `S`'s contributions enabled.

```
margin(S) = utility(a* | S) − max utility(a ≠ a* | S)

v(∅)      = 0
v(S)      = max(0, margin(S))     for non-empty S
```

## Frozen decisions

| Question | Decision |
|---|---|
| Equality at zero margin | counts as a tie, value `0`, not a win |
| Losing coalitions | contribute exactly `0`, never a negative magnitude |
| Tie-breaking | excluded from the utility game entirely; recorded separately in the trace |
| Candidate set | fixed from the pre-action state; a coalition changes contributions, never eligibility |
| Arithmetic | frozen R001 fixed point throughout; coalition sums use saturating addition |
| Shapley formula | exact enumeration, `φ_g = Σ_{S ⊆ M\{g}} |S|!(k−|S|−1)!/k! · [v(S∪{g}) − v(S)]` |
| Weight arithmetic | integer weights `{120, 24, 12, 12, 24, 120}` over a common denominator `k! = 720`, divided once at the end, rounded half away from zero |
| Negative individual contributions | retained and reported; a mechanism that suppresses the observed action shows a negative `φ_g` |
| Attribution-share normalization when total positive mass is zero | shares are `0`; the action classifies as `RANDOM_TIEBREAK_ONLY` if a tie-break decided it, otherwise `OTHER_NON_SUBSTANTIVE` |

## Why the candidate set is fixed

Disabling a group's *contribution* is not the same as claiming the organism
never had that candidate. Eligibility, tier membership and refractory state are
properties of the pre-action state, so they are held constant and only the
utility terms vary. Any other choice makes `v` ill-defined across coalitions,
because the coalitions would be comparing different games.

## Version discipline

A change to the value function, to `v(∅)`, to coalition grouping, or to the
approximation method creates a new attribution-contract version and invalidates
direct comparison with prior attribution thresholds unless an explicit
equivalence analysis is adopted.
