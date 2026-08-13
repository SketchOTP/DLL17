# A000 golden fixtures

- Fixture set: `A000-FIXTURES-V1` version 1
- Golden evidence digest:
  `4765e6d587347688841d34c95b5b9caede8cbf44084335302e1475c7aeaa8fc9`
- Compiled into `A000QualificationKernel.GOLDEN_EVIDENCE_DIGEST`
- Full report with sample decision traces: `A000_REPORT.txt`

The digest covers every finding identifier, every readout string and every
held/not-held verdict. A mechanism change that silently alters behaviour moves
the digest and fails CI, whether or not it changes a verdict.

## What the digest does not cover

It does not cover the *thresholds*. A finding that stops holding still moves the
digest, which is the point: the record is of what happened, not of whether the
result was liked.

## Reproducing

```
./gradlew :research:aliveness-spike:accelerated-sim:run              # ~3 min
./gradlew :research:aliveness-spike:accelerated-sim:run --args=--traces
```

Requires no device, no network and no Android SDK: the spike is desktop JVM
only, and its arithmetic is the frozen R001 fixed-point library.

## Fixture inventory

| Section | Fixtures |
|---|---|
| Boundedness | 3 runs of 365 virtual days across static, controlled-novelty and shifting-context habitats |
| Determinism | 2 identical 30-day runs compared per tick |
| Differentiation | 6 seeds × 60 days, 15 pairwise comparisons |
| Preference | forced-exposure protocol, 60 days; observational reversal, 60 days |
| Avoidance and extinction | 60-day conditioning protocol with scheduled presentations for the first 20 days |
| Habituation | 200 direct exposures, then 5 days of isolated recovery, then a dishabituation event and a sensitization event |
| Habit | 60 days with a contingency reversal at day 30 |
| Episodic history | 20 conditioning days + 5 matched probe days, run for FULL and for the episodic ablation |
| Anti-convergence | 180 static days for FULL and for the curiosity ablation |
| Population diversity | 8 seeds × 60 days, 28 pairwise final-window comparisons |
| Attribution | 60 days, 1-in-5 sampling, 1,363 scored spontaneous actions |
| Cohort parity | all 6 cohorts × 3 days with scheduled interactions |

## The determinism claim

`AX-DETERMINISM-01` compares two runs of the identical configuration on state
signature, the complete per-tick action sequence and the attribution
distribution. Determinism here rests on the same foundation R001 qualified:
integer fixed-point arithmetic, no floating point in any organism computation,
and seeded substream-isolated randomness confined to near-equal tie-breaking.

Floating point appears only in the *measurement* layer — entropy, total
variation and rate readouts — and uses `StrictMath` so those figures are
reproducible too.
