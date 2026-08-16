# A001ObservationProtocolV1

Frozen by D016-M-R1 before any model execution.

The observation source is the existing `ViewerSession` outward presentation
surface. The generator records only rendered posture, expression, gaze target,
motion amplitude, vocalization, micro-movement, visible objects, and the timing
of the six supported interaction classes. It does not read or serialize
organism state, decision traces, mechanisms, cohort identifiers, or internal
learning values.

Each matched case uses the same seed, controlled-novelty habitat, 96 ticks at
250 ms per tick, and this schedule:

| Tick | Input |
|---:|---|
| 8 | touch person alpha |
| 20 | call person alpha |
| 32 | offer food at food trough |
| 44 | show ball |
| 68 | startle at aversive buzzer |
| 84 | withdraw attention |

All other ticks are deliberate no-input observation windows. The interaction
schedule, seed domain, object availability, duration, timing and presentation
contract are identical for both candidates in a case. The candidate is the
only intended variable.

There are exactly 12 calibration cases and 12 qualification cases. A canonical
case is generated once; the later A/B reversal is a rendering transformation
of that same case and does not rerun either candidate.
