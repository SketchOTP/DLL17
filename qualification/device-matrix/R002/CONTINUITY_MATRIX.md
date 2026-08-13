# R002 cross-target continuity matrix

Continuity is canonical work, so it carries the same cross-target obligation the
deterministic core does: identical fixtures must produce **byte-identical**
canonical outputs and identical canonical state hashes on every required target.
This file records what each target actually computed.

A target passes only if its `R002_EVIDENCE_DIGEST` equals the frozen golden
constant `R002QualificationKernel.GOLDEN_EVIDENCE_DIGEST` **and** its
`R001_EVIDENCE_DIGEST` still equals R001's. Executing without crashing is not
passing.

- Fixture set: `R002-FIXTURES-V1` version 1
- Continuity contract: `ContinuityDurabilityContractV1` version 1
- Determinism contract: `DeterminismContractV1` version 1, unchanged
- Golden evidence digest:
  `556bbe49df16595f748a487f78a17a83866eb2a018814f69ee469d7976d58d21`
- R001 regression digest:
  `54bc044740a4c05b41b509a7160bff559e09421f2eaa55dc36c3d3ffadc1bd86`
- Run date: 2026-08-13 (UTC)

## Results

| Canonical target | Platform | Architecture / hardware | Fixture set | R002 digest | R001 digest | Status |
|---|---|---|---|---|---|---|
| Desktop JVM reference runner | OpenJDK 17.0.20, Linux | `x86_64` host | `R002-FIXTURES-V1` v1 | `556bbe49…76d58d21` | `54bc0447…adc1bd86` | `PASS` |
| x86 Android emulator | Android 16, API 36, AOSP ATD | `x86_64`, emulated (`ranchu`) | `R002-FIXTURES-V1` v1 | `556bbe49…76d58d21` | `54bc0447…adc1bd86` | `PASS` |
| Tensor Android hardware | Android 16, API 36, Pixel 9 Pro XL | `arm64-v8a`, Google Tensor G4 | `R002-FIXTURES-V1` v1 | `556bbe49…76d58d21` | `54bc0447…adc1bd86` | `PASS` |
| Exynos Android hardware | — | — | — | not executed | not executed | `CONDITIONAL — canonical "when available"` |
| Snapdragon Android hardware | — | — | — | not executed | not executed | `OPTIONAL — not a gate target since the 2026-08-13 amendment` |

Build fingerprints:

| Target | Fingerprint |
|---|---|
| x86 emulator | `Android/sdk_slim_x86_64/emu64x:16/BE2A.250530.027/13847098:userdebug/test-keys` |
| Tensor hardware | `google/komodo/komodo:16/CP1A.260505.005/15081906:user/release-keys` |

Full per-target records, including the complete kernel output each target
produced, are in the sibling files `desktop_jvm.txt`, `x86_emulator.txt` and
`tensor_device.txt`.

## Per-section agreement

All three passing targets reported every section digest identically:

| Section | Digest |
|---|---|
| trust | `32c553602ec91f167d6d0eaa418dc1ac49dd291491cee0d969c2b920cb83ddaa` |
| reconciliation | `4d0237959e543f7c99192549f97989d8a9d7fa418fb3b63560133cd88148379d` |
| debt | `591ec340b36eb158dc1a861891d08227da4628cee407f27ba151bbbf44cc57db` |
| durability | `c73563ca850c05fd629f75a7758bb3ba77e36ba212047edf92f292474bd1023b` |
| encryption | `6db8d74c470473b369c2009f8b4e36cad76f9f8b052aa030000ac4db8bb79e44` |
| replay | `712c72e954725d4d83415c385debe86350895244e08bac7c6b691f324b2c531c` |

Nine instrumented tests ran on each Android target: four R002 and five R001.

## What the passing rows establish

| Axis | Coverage |
|---|---|
| Runtime | HotSpot JVM **and** Android ART |
| Instruction set | `x86_64` **and** `arm64-v8a` |
| Silicon | Desktop x86, emulated x86, Google Tensor G4 |

The two most likely sources of cross-target divergence in continuity logic are
integer scaling of durations and the AEAD's 26-bit limb arithmetic, and both
cross a 32-bit-versus-64-bit register boundary between these targets. Neither
diverged.

## R001 is re-qualified, not assumed

Every Android target ran the R001 kernel in the same instrumented session and
reproduced R001's frozen digest unchanged. That is deliberate: D007 requires R001
determinism to remain green, and a continuity change that perturbed the
deterministic core would be a regression rather than a feature. Asserting it in
the same run means the two claims cannot drift apart between qualifications.

## Reproducing

```
./gradlew :desktop-runner:run
tools/qualify_r002_continuity.sh emulator-5556 x86_emulator
tools/qualify_r002_continuity.sh <phone-serial> tensor_device
```
