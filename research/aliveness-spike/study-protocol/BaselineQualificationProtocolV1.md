# BaselineQualificationProtocolV1

- Status: `READY_FOR_HUMAN_EVIDENCE`
- Version: 1 (draft; not activated)

Establishes that `ScriptedPetBaselineV1` is a genuinely competent comparator
before it is used to judge FULL. The FULL team may not self-certify it.

## Frozen now

| Decision | Value |
|---|---|
| Comparator | `ScriptedPetBaselineV1` versus `DegradedScriptedControlV1` |
| Design | paired live sessions, order randomized and counterbalanced |
| Session duration | as `SpikeExpressionContractV1`: 600 s per creature |
| Presentation | identical, one contract, blinded labels |
| Rater pool | pilot-only, permanently excluded from all scored A001 and ablation pools |
| Endpoint | the same anchored graded aliveness instrument used for the primary endpoint, scored as a paired difference |
| Direction | the qualification passes only if the strong baseline beats the degraded control |
| Owner | `BaselineIndependentOwner`, who may reject or strengthen the baseline |
| Ordering | exclusions, technical-failure rules and analysis frozen before pilot data |
| Post-qualification | hash and pin the contingency set, script, hold windows, interaction coverage manifest, parameters and expression-contract version |

## Objective pre-evidence

Not a substitute for the human endpoint, but recorded because it is available
now. Over 40 virtual days on a matched seed and habitat, the strong baseline
produced 9.0 distinct objects inspected per day, 2.66 bits of action entropy and
a 0.39 inactivity share; the degraded control produced 1.0, 1.36 bits and 0.70.
The intended competence gap is real in the objective measures.

## Blocked

- **Minimum winning margin.** Requires a defensible effect size on the graded
  instrument. `BLOCKED_SPEC_BASELINE_COMPETENCE_MARGIN`.
- **Sample size.** Follows from the margin and the pilot variance.
- **Rater recruitment.** No participants exist.
- **`BaselineIndependentOwner`.** Unassigned; see `IndependentReviewRosterV1`.
