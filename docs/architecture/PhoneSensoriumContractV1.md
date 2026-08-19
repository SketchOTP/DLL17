# PhoneSensoriumContractV1

## D016-AB architecture amendment

The phone is the organism's embodied vessel. The screen is a visual interface
to the organism, not its isolated habitat. The existing A000 `Habitat` remains a
research affordance/action substrate and is not the claimed model of the owner's
real world.

The boundary is:

```text
raw Android signal
  -> platform interpretation
  -> quantized WorldObservation
  -> organism salience and arousal
  -> organism-selected attention/intention
  -> semantic speech frame
  -> bounded local utterance/TTS
```

Raw values, Android classifier labels and presentation timing are never
canonical organism truth. In particular, a camera or audio classifier may
produce uncertain evidence, but it may not write an emotion, memory,
relationship, intention or action directly.

## Versioned contract

`PhoneSensoriumContractV1` uses one `ObservationMeta` for every observation
family. The metadata carries:

- capture time and monotonic sequence order;
- freshness deadline;
- confidence and uncertainty in parts per million;
- provenance;
- runtime permission state;
- capability availability/restriction;
- sensorium mode.

The supported observation families are movement/activity, orientation,
significant motion, light, proximity, trusted time, coarse location/place,
foreground camera, bounded auditory evidence, owner speech, calendar context,
and capability state. D016-AB implemented movement/activity. D016-AC adds a
research-only trusted-time and coarse-place context slice.

D016-AC's time boundary carries only local day pattern, coarse time-of-day
bucket, circadian context and the trust class produced from the existing R002
clock evidence rules. Wall-clock time describes when something appears to be
happening; it does not grant elapsed biological time or become reward,
development or persistence authority.

D016-AC's place boundary quantizes one current Android location reading into a
stable opaque local coarse-place identity. Raw latitude, longitude, accuracy
and provider values are discarded before `WorldObservation` is created. A
denied, unavailable or stale location produces `UNKNOWN_PLACE` with explicit
permission/capability metadata.

The bounded routine learner stores at most 32 place/time/day patterns, caps
each count at 8, records only the latest sequence for recency and decrements
counts on a deterministic 64-observation decay boundary. It derives
`EXPECTED_CONTEXT`, `FAMILIAR_CONTEXT`, `FAMILIAR_BUT_UNUSUAL`,
`NOVEL_CONTEXT` or `UNKNOWN_CONTEXT` from current evidence plus that bounded
history. Context adds salience only; it cannot select `ORIENT`, `VOCALIZE` or
any other action.

## Sensorium modes

| Mode | Meaning | D016-AB status |
| --- | --- | --- |
| `BACKGROUND_LOW_AWARENESS` | Durable state and low-power callbacks only | Contracted, not implemented here |
| `FOREGROUND_SENSORIUM` | Visible activity may register approved sensors | Implemented with `SensorManager` |
| `ACTIVE_CONVERSATION` | Explicit bounded speech window | Contracted, not implemented here |
| `CAPABILITY_DEGRADED` | Missing permission, sensor or platform capability | Contracted and replayable |

## Android capability and restriction matrix

| Evidence | Candidate Android boundary | D016-AB disposition |
| --- | --- | --- |
| Walking/running transitions | Activity Recognition transitions or normalized motion sensors | Use foreground `SensorManager` proof; production may adopt Activity Recognition for lower power |
| Orientation/significant motion/light/proximity | `SensorManager` | Contract only |
| Place/location | Fused Location/geofencing | D016-AC uses one debug foreground current/last-known location read, quantized to an opaque place; geofencing remains future work |
| Camera observations | CameraX `ImageAnalysis` while visible/authorized | Contract only |
| Ambient audio | Bounded foreground capture | Contract only |
| Owner speech | Bounded `SpeechRecognizer`, on-device when available | Contract only |
| Speech expression | Android `TextToSpeech` | D016-AB uses local TTS after an organism speech frame |
| Calendar context | Calendar provider with explicit permission | Contract only |

Android 9+ restricts continuous sensor delivery to background apps, so an
always-present product cannot assume raw accelerometer/gyroscope access without
an appropriate foreground service. Android 12+ restricts background foreground-
service starts, and Android 14+ applies while-in-use checks for camera,
microphone and location service types. The forward design therefore treats
continuous presence as a user-visible capability with explicit degradation,
not as an invisible process assumption.

## D016-AB vertical slice

The debug-only Pixel path is:

```text
Pixel linear acceleration
  -> MotionObservationNormalizer
  -> WorldObservation(MOVEMENT_ACTIVITY)
  -> OrganismAgent salience/arousal
  -> organism-owned IGNORE / ATTEND / INTERRUPT arbitration
  -> selected attention or communicative action
  -> optional OrganismSpeechFrame
  -> bounded compositional ChildlikeUtteranceGenerator
  -> local TextToSpeech only when VOCALIZE is selected
```

The normalizer emits only stabilized transitions such as `STILL -> WALKING` or
`WALKING -> RUNNING`, using three-sample debounce and bounded hysteresis. World
evidence can be ignored, attended, or allowed to interrupt a bounded intention;
the platform adapter never selects an action. Speech is optional: an ORIENT
response is silent unless the organism separately selects VOCALIZE. The semantic
frame carries change, activity, intensity, question, affect, urgency, context
and uncertainty, and the local renderer composes short childlike language from
bounded fragments rather than looking up a complete sentence by topic.
Identical normalized observations plus the same starting organism and adapter
state replay byte-identically in the D016-AB tests.

This is research evidence only. It does not qualify A001, authorize R003-R009,
or establish all-day battery viability.

## D016-AC contextual slice

The debug Pixel path adds two independent evidence boundaries:

```text
System wall time + elapsedRealtime + boot count
  -> existing ClockTrust classification
  -> TrustedTimeObservation(day/time bucket, circadian context, trust class)

one current coarse Android location
  -> quantized opaque PLACE identity
  -> CoarsePlaceObservation(permission/capability metadata)

time + opaque place + bounded routine history
  -> organism-derived context belief
  -> salience modulation only
  -> existing D016-AB organism arbitration
```

The context learner is research-only and fixed-size. It does not retain raw
coordinates, route history, place names, calendar data, maps, prediction
models or continuous background location. Identical normalized evidence and
identical bounded history replay identically; the same current place/time can
be novel without history, expected after repeated history, or familiar-but-
unusual at a materially different time. The latest valid derived context is
carried as a bounded `CurrentRoutineContext` for fewer than eight subsequent
normalized observation sequences, then expires. Compatible movement and future
sensor observations may inherit that current context before organism salience
is evaluated; a context value never selects an action. Only
`VERIFIED_MONOTONIC` and `AUTHENTICATED` time can train or age routine memory.
`ANOMALOUS`, `UNAVAILABLE` and `UNVERIFIED_REBOOT` time may yield a bounded
non-learning interpretation from existing memory but cannot create,
reinforce or decay a routine expectation.
