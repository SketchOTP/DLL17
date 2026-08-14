# R012 substrate device matrix

The Android half of the R012 persistence, cryptography and recovery substrate.
D011 qualified the substrate on the desktop JVM against a real ext4 filesystem
and proved the fault matrix with real child processes; this file records what
*Android targets* did with the same substrate through the production adapter.

- Fixture set: `R012-DEVICE-FIXTURES-V1` version 2
- Directive: D012, re-run under D013
- Adapter: `AndroidKeystoreDeviceKeyContainer`, `AndroidPersistenceLocations`,
  `AndroidLocalKeyBootstrap`
- Backend: `SEGMENTED_APPEND_LOG_V1`, unchanged, in app-private storage
- Run date: 2026-08-14 (UTC), version 1 under D012 and version 2 under D013

## Results

| Target | Platform | Architecture / hardware | Storage | Fixtures | Evidence digest | Status |
|---|---|---|---|---|---|---|
| x86 Android emulator, v2 (D013) | Android 16, API 36, AOSP ATD | `x86_64`, emulated (`ranchu`) | app-private `filesDir`, ext4 | **46 / 46 held** | `bac5989e…209f9d74` | `SUPPLEMENTARY — emulator, not device evidence` |
| x86 Android emulator, v1 (D012) | Android 16, API 36, AOSP ATD | `x86_64`, emulated (`ranchu`) | app-private `filesDir`, ext4 | 44 / 45 held | `ac38a91d…797196a9` | `SUPERSEDED — retained at commit 4700b076, bundle R012DEV-QB-1` |
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

## The one fixture that did not hold, and what came of it

`DV-KS-ROTATION-READBACK-01` **now holds**: `readableAfterRotation=5/5`,
`refusal=none`. The architect resolved the contradiction on 2026-08-14 with the
epoch-separation amendment, and D013 implemented it as
`LocalStorageCryptographyContractV2`: the wrapping epoch and the data key's
identity are separate, a record carries the second, and a rotation rewrites no
history. Version 2 also adds `DV-KS-V1-MIGRATION-01`, which migrates a V1
organism under real Keystore material and reads it back across a later rotation
— `foundV1=true dataKeyId=1 readableAfterMigrationAndRotation=6/6`.

The fixture identifier, question and threshold are unchanged from D012. It was
not relabelled, weakened or removed; the code under it was corrected.

The version 1 report is retained at commit
`4700b0762cad3b1bb63a69be4f7eca9caea3b819` and is verified by `R012DEV-QB-1`,
which is pinned there for that purpose. The D012 record below is kept as written.

## The one fixture that did not hold (D012, as recorded)

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
