# SpikeExpressionContractV1

- Status: `FROZEN`
- Version: 1
- Implementation: `research/aliveness-spike/cohorts/.../Expression.kt`
- Frozen under: D008

The presentation system every cohort renders through. It exists to make one
claim defensible: a difference a rater perceives is a difference in what the
controller *decided*, never a difference in how richly it was *drawn*.

## The structural guarantee

`SpikeExpressionContract` has no cohort parameter, no mechanism parameter and no
access to organism state. Its input is a five-field normalized value that a
scripted controller fills as completely as FULL does:

```
PresentationInput(action, target, intensity, valence, tick)
```

`SpikeIsolationTest` fails the build if the word `Cohort` appears anywhere in
the contract source, and if the viewer's rendering classes reference cohort or
mechanism identity. Blinding is therefore a property of the type system and the
build, not of anybody's discipline.

## Frozen vocabulary

| Dimension | Frozen set |
|---|---|
| Posture | `STAND`, `CROUCH`, `LEAN_IN`, `LEAN_AWAY`, `LIE_DOWN`, `PLAY_BOW` |
| Expression | `NEUTRAL`, `ALERT`, `CONTENT`, `WARY`, `TIRED`, `EXCITED`, `WITHDRAWN` |
| Micro-movement library | `settle`, `weight-shift`, `ear-flick`, `slow-blink`, `tail-curl`, `breath-deep`, `look-away`, `small-step`, `head-tilt`, `stretch` |
| Gaze | bearing of the attention target; absent target gazes forward |
| Vocalization | boolean, driven by `VOCALIZE` only |
| Motion amplitude | fixed-point `[0, 1]`, clamped per posture |

## Frozen timing

| Parameter | Value |
|---|---|
| Frame cadence | 50 ms |
| Organism tick in the viewer | 200 ms |
| Micro-movement hold | 4 organism ticks |
| Primary session duration | 600 s per creature |

The micro-movement index is a function of logical tick and action ordinal only,
so idle variety has identical richness for every cohort by construction.

## Camera, layout, habitat rendering

One camera, one layout, one colour mapping keyed by object kind. The habitat is
drawn as a ring of abstract affordances with the attention target outlined. No
cohort-specific asset, animation curve or layout exists.

## Clip-generation pipeline

Passive clips, when generated, are produced from `ExpressionFrame.signature()`
sampled at the frozen frame cadence. Because that signature is the complete
render input, two cohorts producing the same frame sequence produce identical
clips. Passive clips are secondary diagnostics; the primary A001 endpoint uses
live paired sessions.

## What this contract does not decide

Creature art direction beyond the shapes above, audio, haptics, and any
production embodiment or rig. R005 owns embodiment; this is a research viewer.
