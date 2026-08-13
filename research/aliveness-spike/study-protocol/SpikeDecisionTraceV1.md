# SpikeDecisionTraceV1

- Status: `FROZEN`
- Version: 1
- Implementation: `research/aliveness-spike/cohorts/.../DecisionTrace.kt`

Every scored autonomous action records a trace. Traces are audit evidence: they
are never visible to a rater and cannot influence viewer behaviour, which is
structural rather than promised — the viewer module has no reference to this
type.

## Recorded fields

| Field | Content |
|---|---|
| `runId`, `organismId`, `tick` | run and organism identity, logical time |
| `cohort` | which controller produced it (audit side only) |
| `winningTier` | the tier that survived tier arbitration |
| `eligible` | every candidate in the winning tier, with utility and per-group decomposition |
| `rejectedLowerTier` | every discarded proposal with the tier that discarded it |
| `driveSnapshot` | energy, rest, safety, social |
| `modulatorSnapshot` | arousal, stress, reward expectancy |
| `traitSnapshot` | curiosity, sociability, caution, persistence |
| `curiosityPhase` | oscillator phase for the target object |
| `inspectionInhibition` | recent-inspection inhibition for the target |
| `commitmentRemaining`, `refractoryActive` | commitment and refractory state |
| `opportunityPromoted` | whether the adjacent-tier opportunity window applied |
| `runnerUpMargin` | winning margin over the runner-up |
| `tieBreakDetermined`, `tieBreakDomain`, `tieBreakDraw` | the only permitted random-domain input, recorded separately from utility |
| `expectedOutcomeClass` | `RESOURCE_GAIN`, `REST_GAIN`, `SOCIAL_GAIN`, `THREAT_REDUCTION`, `EPISTEMIC_GAIN`, `NO_CHANGE_EXPECTED` |
| `spontaneous` | whether this action qualifies as spontaneous |
| `attribution` | the exact coalition attribution, when scored |

## Per-group decomposition

Each candidate carries six contributions, one per `MechanismCoalitionSetV1`
group. This is what makes coalition recomputation exact rather than approximate:
utility under a coalition is the sum of that coalition's contributions, so
recomputing the controller with only a coalition enabled is a sum, not a rerun.

## Spontaneity

An action is spontaneous when its winning tier is 4 or 5, no rater input arrived
within the preceding three ticks, and it is not a commitment continuation.

## Qualification rule

```
observable spontaneous action
AND no valid causal SpikeDecisionTrace
→ NOT EVIDENCE OF ALIVENESS
→ trace/implementation failure
```

Sample traces are in `qualification/fixtures/A000/A000_REPORT.txt`.
