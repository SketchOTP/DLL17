# R012 substrate device gate

- Phase: R012 device qualification — the Android half of the substrate the
  2026-08-14 parallel-execution amendment authorized
- Directive: D012
- Fixture set: `R012-DEVICE-FIXTURES-V1` version 1
- Evidence digest (emulator target):
  `ac38a91d98d5e422a1e1077be151e5a90b290efa89f91fa217435461797196a9`
- Evaluated: 2026-08-14
- Amended: 2026-08-14 under D013 — see "The declared fixture failure" below

**Gate state: `BLOCKED_DEVICE_UNAVAILABLE`.**

No physical Android device was reachable to this directive. D012 states the
consequence plainly and this gate follows it: an emulator does not substitute
for physical-device evidence, and desktop results may not be presented as device
results. Every requirement that concerns Android Keystore behaviour, real
app-private device storage, physical-device latency, or restart and recovery on
actual hardware is therefore open.

---

## What was completed

| Requirement | State |
|---|---|
| Android Keystore adapter implemented against the frozen contracts | Done |
| App-private storage adapter for `SEGMENTED_APPEND_LOG_V1` | Done |
| Startup key-resolution decision table, with birth refused on cryptographic failure | Done, and qualified in ordinary CI |
| Device qualification kernel, 45 fixtures | Done |
| Real Android process death in a `:crash` process | Done |
| Backup and device-transfer exclusion verified from the **built** package | Done, both variants |
| Canonical determinism across persistence, process death, restart and recovery | Done on the available target |
| Emulator execution as supplementary API-path coverage | Done, 44 / 45 held |

## What is blocked

| Requirement | State |
|---|---|
| Keystore behaviour on hardware-backed or StrongBox material | `BLOCKED_DEVICE_UNAVAILABLE` — the emulator reported `SOFTWARE` |
| Real device flash persistence qualification | `BLOCKED_DEVICE_UNAVAILABLE` |
| Physical-device latency and footprint measurements | `BLOCKED_DEVICE_UNAVAILABLE` |
| Restart and recovery on actual hardware | `BLOCKED_DEVICE_UNAVAILABLE` |
| Production thresholds | Not derived, and not derivable from emulator figures |
| Sudden power-loss durability | Not proven by any test here, and not claimed |

## The declared fixture failure — resolved under D013

**Status: resolved.** The architect's 2026-08-14 epoch-separation amendment chose
separating the wrapping epoch from the data key's identity over re-encrypting
history, and D013 implemented it as `LocalStorageCryptographyContractV2`. The
device suite was re-run at fixture set version 2: `DV-KS-ROTATION-READBACK-01`
now holds with `readableAfterRotation=5/5`, and 46 of 46 fixtures hold.

That re-run is filed separately as `R012DEV-QB-2`. This gate record and
`R012DEV-QB-1` are pinned to D012's commit and keep the failure exactly as it was
reported, because a resolved finding that leaves no trace is indistinguishable
from a finding nobody made. The original text follows unchanged.

**The gate state above is unchanged: `BLOCKED_DEVICE_UNAVAILABLE`.** D013 had no
physical device either, and resolving a fixture is not qualifying hardware.

---

`DV-KS-ROTATION-READBACK-01` reports `NOT HELD`. It records a contradiction
between `LocalStorageCryptographyContractV1` (rotation rewraps one key and
leaves existing records readable) and the frozen `EncryptedRecordStore`, which
refuses any record written under a different key epoch and derives its nonce and
AAD from the current one. After a wrapping rotation the existing journal does not
open.

D012 forbids changing a frozen contract to make a device test pass. The fixture
therefore states what the code does, fails, and is declared in
`PENDING_ARCHITECT_REVIEW`. This needs a versioned amendment and architect
review. It is a substrate defect rather than an Android one: it reproduces on the
desktop too, and D011 did not find it because its rotation fixture checked that
the data key was unchanged without then reading a record back.

## What closing this gate requires

1. A physical Android device, reachable to `adb`. The Pixel 9 Pro XL / Tensor G4
   used for the R001 and R002 matrices is an acceptable target.
2. `./gradlew :android-host:connectedDebugAndroidTest` against it, with the
   report captured from logcat and filed in the device matrix.
3. ~~An architect decision on `DV-KS-ROTATION-READBACK-01`.~~ Done, D013.

One input is outstanding, and it is hardware. The suite is written, the adapter
is implemented, and the contract defect the suite found has been corrected.
