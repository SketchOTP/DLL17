# D016-AC Context Validation

- Date: 2026-08-19
- Parent accepted physical sensorium HEAD: `2e791f96c6cffd73d6f9045250218201df1efe19`
- D016-AC-R1 local candidate debug APK SHA-256: `FC3DE21554E33FA4BCB771DBE7D4D1F700A293AD5E8C4C1DF403DC02D6436D2F`

## D016-AC review correction

- D016-AC review disposition: `PARTIAL_IMPLEMENTATION_REVIEW_FAILED_CONTEXT_PROPAGATION`
- D016-AC-R1 local disposition: `ACCEPTED`
- Current context: the latest valid derived context is carried as bounded
  research state for fewer than eight subsequent normalized observation
  sequences. It is applied to compatible later observations, including
  movement, and expires at the exclusive sequence deadline. Unknown place
  clears the current context.
- Learning authority: only `VERIFIED_MONOTONIC` and `AUTHENTICATED` time may
  train or age routine memory. `ANOMALOUS`, `UNAVAILABLE` and
  `UNVERIFIED_REBOOT` can produce a bounded non-learning interpretation from
  existing memory but cannot create, reinforce or decay expectations.
- Movement fixture: the same `WALKING -> RUNNING` observation receives
  `EXPECTED_CONTEXT` after repeated morning history and
  `FAMILIAR_BUT_UNUSUAL` after the same place is established as morning-familiar
  and observed in the evening. The unusual case has measurably higher salience;
  no context value selects an action.

## Local deterministic validation

- D016-AC cohort tests: `PASS`
- Expected repeated place/time context: `PASS`
- History-free novel context: `PASS`
- Familiar place at unusual time: `PASS`
- Same observation under different histories: `PASS`
- Fresh current context carried onto identical movement: `PASS`
- Expected-vs-unusual identical-movement salience difference: `PASS`
- Expired context no longer carried onto movement: `PASS`
- Anomalous/unverified-reboot time cannot train routine memory: `PASS`
- Permission-denied/unknown-place degradation: `PASS`
- Stable opaque place identity and no raw-coordinate signature: `PASS`
- Fixed capacity, saturated counts and decay: `PASS`
- Identical context replay: `PASS`
- Existing D016-AB cohort fixtures: `PASS`
- Debug APK assembly: `PASS`
- Local toolchain note: the workstation has JDK 21 and no JDK 17; validation used the installed JDK 21 temporarily while repository build files remained pinned to JDK 17. CI remains the authoritative JDK-17 check.

## Physical Pixel boundary

- ADB device enumeration on Atlas: `EMPTY`
- Atlas USB enumeration: `NO GOOGLE/PIXEL DEVICE`
- Local ADB enumeration: `EMPTY`
- APK installation: `NOT RUN`
- Trusted-time observation on Pixel: `NOT RUN`
- Real coarse-place observation: `NOT RUN`
- Raw-coordinate discard proof from device run: `NOT RUN`
- Permission-denied device run: `NOT RUN`
- Runtime stability and bounded battery observation: `NOT RUN`

The physical requirement is blocked by device availability, not by a location
or time implementation failure. No raw latitude, longitude, accuracy or
provider value is present in `WorldObservation`, context state, status text or
this evidence record. No A001 verdict is claimed; `R003-R009` remain blocked.
