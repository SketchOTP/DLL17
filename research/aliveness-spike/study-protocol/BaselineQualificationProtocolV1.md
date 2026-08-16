# BaselineQualificationProtocolV1

> **HISTORICAL / SUPERSEDED_FOR_FORWARD_EXECUTION (D016-L).** The human
> baseline qualification is retained as V1 evidence and is not a V2 prerequisite.

- Status: `READY_FOR_HUMAN_EVIDENCE`
- Version: 1
- Coverage manifest: `research/aliveness-spike/evidence/BASELINE_COVERAGE_MANIFEST.txt`
- Architect pre-data disposition: `APPROVED_FOR_HUMAN_QUALIFICATION_NOT_YET_QUALIFIED`

## D016-J participant scope

Recruitment under this protocol is limited to U.S. adults age 18+ who can
provide their own informed consent. Prisoners and people unable to provide
legally effective consent are excluded. A real study-owner contact and the
compensation terms must be supplied through the booking/consent channel before
any session begins. Signed consent is stored separately as an identifiable
record and retained for three years after completion of the A001 human-study
programme before secure destruction.

Establishes that `ScriptedPetBaselineV1` is a genuinely competent comparator
before it is used to judge FULL. The FULL team may not self-certify it.

The point is adversarial. If the baseline is weak, a FULL win means nothing; the
whole force of a positive A001 comes from the comparator being hard to beat.

---

## Frozen

| Decision | Value |
|---|---|
| Comparator | `ScriptedPetBaselineV1` versus `DegradedScriptedControlV1` |
| Participants | 40, independent |
| Exclusion | Permanently excluded from all scored A001 and human-ablation pools |
| Design | Paired live sessions, order randomized and counterbalanced |
| Session duration | 600 s per creature, as `SpikeExpressionContractV1` |
| Presentation | Identical, one contract, blinded labels |
| Endpoint | `BaselineCompetenceInstrumentV1`, anchored 0–100, scored as a paired difference: strong baseline minus degraded control |
| Required margin | mean `>= +15.0` points |
| Precision | two-sided 95% CI lower bound `> 0` |
| Direction | The qualification passes only if the strong baseline beats the degraded control. A tie or a reversal fails. |
| Analysis | The same preregistered pipeline as the primary endpoint: complete-case, fixed screening order, paired t interval |
| Decision authority | The deterministic `A001GateAdjudicatorV1` reads only the sealed 40-person human result; no owner, reviewer, model or override exists |
| Ordering | Exclusions, technical-failure rules and analysis frozen before pilot data |
| Post-qualification | Hash and pin the contingency set, script, hold windows, interaction coverage manifest, parameters and expression-contract version |

## What competence means here

The instrument assesses three things, and only these three. It does **not** ask
how alive the creature seemed — that is the A001 endpoint, and asking it here
would contaminate the comparison.

| Dimension | Question asked |
|---|---|
| Contingent responsiveness | Did it react to what you did, promptly and in a way that fitted what you did? |
| Behavioural coherence | Did what it did follow sensibly from what came before, rather than jumping about at random? |
| Surface behavioural breadth | How many different things did it do? |

Each is rated 0–100 with anchors; the competence score is their mean. Frozen
before data, like everything else.

## Coverage manifest

`BaselineCoverageManifestV1` is generated from the implementation itself, so it
cannot drift away from the thing it describes. It discloses, in full:

- the four authored drive thresholds;
- the seven-rule contingency table in priority order;
- the reaction for each of the six interaction kinds, for both scripted cohorts,
  and which three the degraded control ignores;
- both idle/play scripts entry by entry, with their hold windows and advance
  rules;
- surface coverage: 12 of 15 actions and 11 of 12 objects for the strong
  baseline, against 8 and 5 for the degraded control;
- the expression contract version.

The coverage manifest is the evidence basis for exposing the comparator to the
frozen human qualification. The Architect's pre-data disposition is
`APPROVED_FOR_HUMAN_QUALIFICATION_NOT_YET_QUALIFIED`; it is not a qualification
result. It creates no owner, reviewer, model or discretionary override.

Qualification remains determined solely from the sealed 40-person result by
`A001GateAdjudicatorV1`.

## Objective pre-evidence

Not a substitute for the human endpoint, and recorded only because it is
available now. Matched seed, habitat, window and probe, from
`BASELINE_COVERAGE_MANIFEST.txt`:

| Cohort | Entropy | Objects/day | Occupancy | Inactivity | Regularity |
|---|---|---|---|---|---|
| `ScriptedPetBaselineV1` | 2.629 | 9.18 | 0.344 | 0.392 | 0.633 |
| `DegradedScriptedControlV1` | 1.377 | 1.00 | 0.584 | 0.703 | 0.775 |
| FULL | 2.763 | 10.15 | 0.341 | 0.243 | 0.358 |

The intended competence gap between the two scripted cohorts is real in the
objective measures, and the strong baseline is unchanged from its D008 form.

That FULL leads on every objective measure is **not** evidence that it will win
the human comparison, and must not be cited as any. Objective breadth is not
apparent aliveness; if it were, A001 would be unnecessary.

## Blocked

- **Participants.** None exist.
- **The qualification itself.** It is determined only by the frozen 40-person blinded experiment and remains `BLOCKED_BASELINE_NOT_QUALIFIED` until that result exists.
  The baseline is implemented, frozen, fully disclosed and powered — and it is
  not qualified, and nothing in this repository may claim otherwise.
