# BlindVariancePilotV1

> **HISTORICAL / SUPERSEDED_FOR_FORWARD_EXECUTION (D016-L).** The participant
> variance pilot is retained as V1 evidence and is not part of active A001 V2.

- Status: `READY_FOR_HUMAN_EVIDENCE`

## D016-J participant scope

Pilot recruitment remains blocked until the pilot is registered. When opened,
participants must be U.S. adults age 18+ capable of providing their own informed
consent; prisoners and people unable to provide legally effective consent are
excluded. A real study-owner contact and compensation terms must be supplied
before consent. Pilot participants remain permanently excluded from scored A001.
- Version: 1
- Executable portion: `research/aliveness-spike/analysis/.../BlindVariancePilot.kt`
- Seal evidence: `BlindVariancePilotSealTest`,
  `research/aliveness-spike/evidence/A001_ACTIVATION_DRY_RUN.txt`

The powered paired endpoint needs the standard deviation of paired differences
between FULL and the qualified baseline. Obtaining it means running the real
task with real people, which is why the barrier below exists and why it is
enforced in code rather than in prose.

---

## Frozen

| Decision | Value |
|---|---|
| Registration | `VARIANCE_ONLY, NON_SCORED`, registered before the first pilot participant |
| Participants | 36 analysable pairs, over-recruited to replace dropouts |
| Replacement | Preregistered and outcome-independent: replacements fill the target and are never chosen after inspecting scores |
| Frozen before the first participant | FULL candidate hash, qualified `ScriptedPetBaselineV1` hash, viewer/interaction/session contract hashes |
| Procedure | Identical to `AlivenessStudyProtocolV1`: same instrument, same 600 s sessions, same counterbalancing, same six interactions |
| Participant eligibility | Pilot participants are permanently ineligible for all scored A001 and human-ablation pools |
| Operator | An independent study operator, running the sealed analysis path |
| Released to the FULL team | `pairedDifferenceSd` and a protocol-valid flag. Nothing else. |
| Withheld from the FULL team | Mean paired difference, sign or direction, cohort means, per-participant data, subgroup effects, forced-choice results, comparative comments, raw recordings |
| Sealing duration | Raw and outcome-revealing pilot data remain sealed through all scored attempts |
| Status immutability | A pilot can never be promoted into scored evidence; a scored study can never be demoted into a pilot after an unfavourable result |
| Leakage | Any unauthorized comparative disclosure is `A001_GOVERNANCE_BREACH`; it is not cured by relabelling the data or by rerunning another pilot on the same candidate |
| Repetition | A technically invalid pilot may be repeated only after the independent reviewer certifies that no comparative outcome information reached the FULL team |

### Why 36

36 analysable pairs is the smallest pilot the frozen 1.25 SD inflation factor
can defend. 1.25 is the one-sided 95% upper confidence bound on a standard
deviation at 35 degrees of freedom; at 35 pairs the bound is 1.253 and the
factor no longer covers it. Below 36 the pilot reports itself
`protocolValid=false` and the feasibility calculator refuses the release rather
than powering off a number it cannot stand behind.

---

## The released schema

```
BlindVariancePilotV1 v1 RELEASE
  pairedDifferenceSd=<six decimals>
  protocolValid=<true|false>
```

Two fields. Both are invariant to the direction of the result: a standard
deviation is unchanged if every difference flips sign, and the validity flag
depends only on how many pairs were analysable.

## How the barrier is enforced

Structurally, not procedurally.

| Control | Mechanism |
|---|---|
| The release carries only permitted fields | `PilotRelease` declares exactly `pairedDifferenceSd` and `protocolValid`; the test enumerates its declared fields and accessors by reflection and fails on any other |
| The full analysis cannot escape | The sealed analysis type is `private` inside `BlindVariancePilot` and is never returned by any function |
| Direction cannot be inferred | Two pilots built to disagree completely — one strongly favourable, one strongly unfavourable, matched on dispersion — produce byte-identical released output. The test asserts the equality. |
| Unusable sessions cannot leak through the estimator | Technical failures, incomplete sessions, missing and out-of-range scores are dropped before the SD, and dropping them below 36 invalidates the pilot |

All of this is demonstrated on explicitly synthetic data. **Synthetic pilot data
is never recorded as study evidence**, appears only under
`research/aliveness-spike/evidence/`, and is marked `SYNTHETIC` on every line of
the dry run.

## Blocked

- **`pairedDifferenceSd`.** The single number this pilot exists to produce. No
  pilot has run.
- **Independent operator.** Unassigned; see `IndependentReviewRosterV1`.
- **Registration.** Requires an operator and a reviewer, so the pilot is
  `BLOCKED_VARIANCE_PILOT_NOT_REGISTERED` however ready the code is.
