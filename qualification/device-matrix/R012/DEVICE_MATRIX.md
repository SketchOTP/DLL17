# R012 substrate device matrix

The Android half of the R012 persistence, cryptography and recovery substrate.
D011 qualified the substrate on the desktop JVM against a real ext4 filesystem
and proved the fault matrix with real child processes; this file records what
*Android targets* did with the same substrate through the production adapter.

- Fixture set: `R012-DEVICE-FIXTURES-V1` version 1
- Directive: D012
- Adapter: `AndroidKeystoreDeviceKeyContainer`, `AndroidPersistenceLocations`,
  `AndroidLocalKeyBootstrap`
- Backend: `SEGMENTED_APPEND_LOG_V1`, unchanged, in app-private storage
- Run date: 2026-08-14 (UTC)

## Results

| Target | Platform | Architecture / hardware | Storage | Fixtures | Evidence digest | Status |
|---|---|---|---|---|---|---|
| x86 Android emulator | Android 16, API 36, AOSP ATD | `x86_64`, emulated (`ranchu`) | app-private `filesDir`, ext4 | 44 / 45 held | `ac38a91d…797196a9` | `SUPPLEMENTARY — emulator, not device evidence` |
| Tensor Android hardware | — | Pixel 9 Pro XL / Tensor G4 | — | not executed | — | `BLOCKED_DEVICE_UNAVAILABLE` |
| Exynos Android hardware | — | — | — | not executed | — | `CONDITIONAL — canonical "when available"` |
| Snapdragon Android hardware | — | — | — | not executed | — | `OPTIONAL — not a gate target since the 2026-08-13 amendment` |

Build fingerprints:

| Target | Fingerprint |
|---|---|
| x86 emulator | `Android/sdk_slim_x86_64/emu64x:16/BE2A.250530.027/13847098:userdebug/test-keys` |

Full per-target records are in `x86_emulator_qualification.txt` and
`x86_emulator_performance.txt`.

## What the emulator row does and does not establish

D012 is explicit that an emulator provides supplementary API-path coverage and
does not substitute for physical-device evidence where the requirement concerns
Keystore behaviour, real app-private device storage, physical-device latency, or
restart and recovery on actual hardware. All four of those requirements are
therefore **open**, and the emulator row is filed as `SUPPLEMENTARY` rather than
as a pass so that nobody can later read it as one.

| Established by the emulator row | Not established |
|---|---|
| The Keystore API path is exercised end to end: create, look up, non-export, wrap, rotate, delete, refuse | That a **hardware-backed or StrongBox** key behaves the same. The emulator reported `SOFTWARE` |
| The adapter resolves and uses real app-private storage on ext4 | That real device flash, with its own FTL, wear levelling and write amplification, behaves the same |
| A real Android process was killed with `Runtime.halt` while holding storage open, in a `:crash` process | That a device power cut at the same instant produces the same result. It cannot: killing a process leaves the page cache intact |
| The fault matrix, the material rule and canonical determinism hold on ART | Any production latency or footprint threshold. Emulated storage latency is not device latency |
| The installed package really has Auto Backup disabled and really excludes canonical state in both rule sets | — |

## The one fixture that did not hold

`DV-KS-ROTATION-READBACK-01` — after the wrapping epoch advances, records
written under the previous epoch are refused, `readableAfterRotation=0/5`.

This is not a device finding and not a defect introduced by D012. It is a
contradiction between two frozen contracts:

- `LocalStorageCryptographyContractV1` says rotation "rewraps one key rather
  than re-encrypting every record ever written", and that records already
  written stay readable.
- `EncryptedRecordStore`, frozen under `ContinuityDurabilityContractV1`, refuses
  any record whose header epoch differs from the container's current epoch, and
  derives the nonce and AAD from that same epoch.

Both are frozen and they cannot both be satisfied. D012 forbids changing a
frozen contract to make a device test pass, so the fixture records the observed
behaviour, reports `NOT HELD`, and is named in
`R012DeviceQualificationKernel.PENDING_ARCHITECT_REVIEW` so the failure is
declared rather than discovered. Resolution requires a versioned amendment and
architect review.

## Reproducing

```
$ANDROID_HOME/emulator/emulator -avd <avd> -no-window -no-audio &
./gradlew :android-host:connectedDebugAndroidTest
```

The kernel logs its complete report under the tags `DLL17-R012-device` and
`DLL17-R012-perf`, and also writes it to the target's external files directory.
Logcat is the reliable record: Gradle uninstalls the package after the run, and
app-private evidence goes with it.
