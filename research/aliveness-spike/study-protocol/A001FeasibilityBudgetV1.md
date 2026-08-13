# A001FeasibilityBudgetV1

- Status: `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD`
- Version: 1 (draft; not activated)

## Frozen now

| Decision | Value |
|---|---|
| Primary endpoint | `PairedAlivenessDifference = gradedAlivenessScore(FULL) − gradedAlivenessScore(ScriptedPetBaselineV1)` |
| Instrument | one anchored graded aliveness scale, 0–100, administered after each standardized live session |
| Secondary | binary forced choice (`which appeared more alive?`); `same creature / different creature?` is a separate distinctiveness endpoint |
| Design | paired, within-rater, one primary pair per rater |
| Session duration | 600 s per creature |
| Variance source | `BlindVariancePilotV1`, released quantity `pairedDifferenceSD` only |
| Hard gate | `required powered sample > frozen feasible budget → A001_NOT_FEASIBLE → redesign before scored data` |
| Underpowered studies | not a legitimate scored attempt; cannot be used to claim PASS or FAIL |

## Blocked inputs

Every one of these requires evidence or an owner that does not exist:

| Input | Block |
|---|---|
| `pairedDifferenceSD` | `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD` — the pilot has not run |
| Minimally worthwhile paired difference | `BLOCKED_SPEC_MINIMALLY_WORTHWHILE_DIFFERENCE` — a value judgement, not a measurement |
| Maximum fundable participants and participant-hours | `BLOCKED_SPEC_STUDY_BUDGET` — a funding decision |
| Expected technical and exclusion reserve | derived from pilot attrition, which does not exist |
| Alpha and power targets | conventionally 0.05 and 0.80, but not frozen here because they interact with the budget decision |
| Variance inflation rule | depends on the pilot's precision |
| Resulting powered sample size | the output; computable only from the above |

Exact wording and anchors for the graded instrument are also unfrozen: writing
them without cognitive pretesting would fix a measurement instrument on
guesswork.
