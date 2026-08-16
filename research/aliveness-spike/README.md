# A000 aliveness spike

Disposable research code. Not production.

## Active evaluation contract

Forward A001 execution is governed by `A001EvaluationContractV2`: AI-agent
qualification followed by one owner-only Pixel acceptance review. External
human participants and population-level human inference are prohibited. The
prior V1 human-study documents remain historical and are explicitly superseded
for forward execution.

This track exists to answer one question: do the proposed internal-state,
action, learning, memory and individuality mechanisms create an observer-visible
impression of autonomous life strong enough to justify the downstream production
complexity? R003 through R009 are blocked until A001 answers it.

## Layout

| Directory | Contents |
|---|---|
| `cohorts/` | Habitat, candidate mechanisms, tiered controller, decision trace, coalition attribution, presentation contract, all six cohorts |
| `accelerated-sim/` | Deterministic accelerated simulator, measures, the A000 qualification kernel |
| `realtime-viewer/` | Blinded fixed-duration Swing observer and the standardized paired session |
| `analysis/` | Curiosity-envelope feasibility search and the executable governance audit |
| `study-protocol/` | The fifteen A000/A001 research contracts |
| `evidence/` | Envelope search and governance audit output |

## Isolation

The only production dependency permitted by the canonical plan is the frozen
R001 fixed-point numeric library, and that is what these modules use.
`core-crypto` arrives transitively because `core-math` publishes it as `api` for
lookup-table verification; no spike source imports it, and `SpikeIsolationTest`
fails the build if one ever does.

Nothing under `research/` is depended on by any production module, and spike
source, state and schemas are never copied into production organism modules.
Findings cross into production only through the canonical adoption process:

```
measured finding
+ preregistered evidence
+ adopted architecture amendment
+ newly frozen production contract
```

The presence of a mechanism here does **not** qualify or authorize its
R003–R009 production equivalent.

## Running

```
./gradlew :research:aliveness-spike:accelerated-sim:run              # ~3 min, prints the A000 digest
./gradlew :research:aliveness-spike:accelerated-sim:run --args=--traces
./gradlew :research:aliveness-spike:analysis:run                     # ~5 min, envelope feasibility
./gradlew :research:aliveness-spike:analysis:governanceAudit
./gradlew :research:aliveness-spike:agentic-review:agenticReviewQualification
./gradlew :research:aliveness-spike:realtime-viewer:run --args=--pair
```

Desktop JVM only. No device, no network, no Android SDK.

## Status

A000 is complete. D008 built the track and returned five negative findings and an
empty curiosity feasible region; D009 remediated the candidate against those
findings under unchanged thresholds. All 24 accelerated findings now hold and the
joint feasible region is 27 of 27 grid points.

The rejected D008 candidate's results are retained in `evidence/negative/D008/`
and are not superseded.

A001 is blocked: `IndependentReviewRosterV1` names nobody.
