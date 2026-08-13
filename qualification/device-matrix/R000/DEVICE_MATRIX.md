# R000 device matrix

Targets the R000 shell has actually been executed on. A row is added only after
a recorded qualification run, never in anticipation.

## Qualified in R000

| Field | Value |
|---|---|
| Role | R000 qualification target |
| Model | Pixel 9 Pro XL |
| Device / hardware | `komodo` |
| SoC family | Google Tensor |
| ABI | `arm64-v8a` |
| Android release | 16 |
| API level | 36 |
| Build fingerprint | `google/komodo/komodo:16/CP1A.260505.005/15081906:user/release-keys` |
| Build type | `user` (release-keys, not rooted) |
| Result | `PASS` — install, cold launch, visible state, terminate, relaunch |
| Evidence | `qualification_run.log`, `ui_hierarchy_launch1.xml`, `ui_hierarchy_launch2.xml`, `shell_launch1.png`, `shell_launch2.png`, `meminfo_launch1.txt`, `logcat_launch1.txt`, `logcat_launch2.txt` |
| Logcat handling | Filtered to this package plus crash blocks. The full buffer of a personal phone inventories the owner's applications and telephony activity and is never written to the repository. |
| Run date | 2026-08-13 (UTC) |

The device is physical hardware belonging to the project owner, attached over
USB and authorized for debugging by the owner for this run.

Note on API level: the shell targets API 37 and declares `minSdk 29`. It was
qualified on an API 36 device, which is inside the supported range. R000 does
not claim API 37 runtime qualification.

## Canonical determinism matrix, for reference

The canonical architecture requires identical canonical bytes and hashes on
Tensor, Snapdragon, Exynos where available, the x86 Android emulator, and the
desktop JVM reference runner. That matrix governs **R001** determinism
qualification, not R000. R000 proves only that the shell runs.

| Canonical target | R000 status |
|---|---|
| Tensor Android hardware | Executed (Pixel 9 Pro XL, above) |
| Snapdragon Android hardware | Not executed in R000 |
| Exynos Android hardware | Not executed in R000 |
| x86 Android emulator | Attempted, blocked — see below |
| Desktop JVM reference runner | Executed (`./gradlew :desktop-runner:run`) |

## x86 emulator, attempted and blocked

An `android-37.0;google_apis;x86_64` AVD was created and booted on the build
host under KVM. It could not complete a qualification run: `surfaceflinger`
repeatedly crashed with SIGSEGV inside `RegionSamplingThread::threadMain`,
which took down `system_server` and produced broken-pipe failures from the
activity service. The fault reproduced under three rendering backends
(`swiftshader_indirect`, `guest`, `swangle_indirect`).

The crash is in the emulator's own graphics stack, not in this application: no
fatal exception was ever attributed to `com.animusmachinae.dll17` during those
runs, and one launch attempt did reach `Status: ok` before the surface stack
failed underneath it.

This is recorded as a platform fact, not an application defect, and not as a
passed criterion. The emulator remains required for R001 determinism
qualification, where it is a cross-architecture check rather than a launch
check, so it must be resolved before R001 closes.
