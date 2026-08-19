# D016-AC Context Validation

- Date: 2026-08-19
- Parent accepted physical sensorium HEAD: `55406018e26d4c8f8fe8af52a23a2aee41c64ae3`
- Candidate debug APK SHA-256: `96CABA8B27578B3CCF54DE28113FABBBE02725E63C898D3E4A10128856D23584`

## Local deterministic validation

- D016-AC cohort tests: `PASS`
- Expected repeated place/time context: `PASS`
- History-free novel context: `PASS`
- Familiar place at unusual time: `PASS`
- Same observation under different histories: `PASS`
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
