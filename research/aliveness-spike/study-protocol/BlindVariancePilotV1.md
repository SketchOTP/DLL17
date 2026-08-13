# BlindVariancePilotV1

- Status: `READY_FOR_HUMAN_EVIDENCE`
- Version: 1 (draft; not activated)

The powered paired endpoint needs the standard deviation of paired score
differences between FULL and the qualified baseline. Obtaining it means running
the real task, which is why the leakage rules below exist.

## Frozen now

| Decision | Value |
|---|---|
| Registration | `VARIANCE_ONLY, NON_SCORED`, registered before the first pilot participant |
| Frozen before the first participant | FULL candidate hash, qualified `ScriptedPetBaselineV1` hash, viewer/interaction/session contract hashes |
| Participant eligibility | pilot participants are permanently ineligible for all scored A001 and human-ablation pools |
| Operator | an independent study operator or a sealed analysis pipeline |
| Released to the FULL team | `pairedDifferenceSD` and a protocol-valid/protocol-invalid flag, and nothing else |
| Withheld from the FULL team | mean paired difference, sign or direction, cohort means, per-participant data, subgroup effects, forced-choice results, comparative comments, raw recordings |
| Sealing duration | raw and outcome-revealing pilot data remain sealed through all scored attempts |
| Status immutability | a pilot can never be promoted into scored evidence; a scored study can never be demoted into a pilot after an unfavourable result |
| Leakage | any unauthorized comparative disclosure is `A001_GOVERNANCE_BREACH`; it is not cured by relabelling the data or by rerunning another pilot on the same candidate |
| Repetition | a technically invalid pilot may be repeated only after the independent reviewer certifies that no comparative outcome information reached the FULL team |

## Blocked

- **`pairedDifferenceSD`.** The single number this pilot exists to produce.
- **Pilot sample size.** Depends on the precision wanted on that SD.
- **Independent operator.** Unassigned.
