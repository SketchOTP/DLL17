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
and capability state. The D016-AB vertical slice implements movement/activity
only.

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
| Place/location | Fused Location/geofencing | Contract only, permission and background limits remain explicit |
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
