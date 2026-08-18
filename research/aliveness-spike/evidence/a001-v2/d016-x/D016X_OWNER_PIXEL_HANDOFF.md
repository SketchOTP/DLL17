# D016-X Owner Pixel Aliveness Handoff

`D016-W-R1` remains closed as pre-execution evidence with zero formal model executions and no organism conclusion. D016-X supersedes the AI evaluator path for forward execution and stops at the owner verdict boundary.

## Frozen organism input

- Candidate: `A001_FULL_D016N_V1`
- Candidate Git SHA: `684579130bef5c820f3db9534ffb744654ebf3b4`
- Implementation starting HEAD: `086ffbc9a84f9dfe18d01349af4489de1451e95b`
- AI evaluator calls in D016-X: `0`
- Comparator displayed: `false`
- R003-R009: `BLOCKED`

## Integration boundary

- Android dependency scope: debug-only `research:aliveness-spike:cohorts`
- FULL construction: `Cohorts.create(Cohort.FULL, seed, fx)`
- Runtime: `SpikeRuntime` with `HabitatCondition.CONTROLLED_NOVELTY`
- Tick pacing: one organism tick every `200` milliseconds
- Presentation input: `SpikeExpressionContract.ExpressionFrame` only
- Android-selected organism actions: `false`
- Cloud service required: `false`

## Owner interaction mapping

| Owner action | Existing organism event |
| --- | --- |
| tap creature | `TOUCH` |
| tap person | `CALL` |
| tap food | `OFFER_FOOD` |
| tap play object | `PRESENT_OBJECT` |
| tap Look away | `WITHDRAW_ATTENTION` |
| tap red object | `STARTLE` |

## Local validation

- D016-X harness unit tests: `PASSED`
- Debug APK assembly: `PASSED`
- Release APK assembly: `PASSED`
- Debug APK SHA-256: `C00FEFA53510F67A1EA7A561099C745EA0AD8BC46E1A0446E8D602AF7E148FE6`
- Full Android unit suite: `PARTIAL`, one unrelated pre-existing persistence-path assertion failed in `AndroidLocalKeyBootstrapTest`
- Physical/emulated Android device connected: `false`
- Pixel install: `BLOCKED_DEVICE_UNAVAILABLE`
- Pixel launch: `BLOCKED_DEVICE_UNAVAILABLE`
- Owner aliveness verdict: `NOT RUN`

No A001 PASS or FAIL is claimed by this handoff.

## D016-X-R1 physical deployment

The device-unavailable entries above are the historical initial D016-X handoff. D016-X-R1 completed the owner deployment without changing the source implementation or frozen organism input.

- `D016_X_STATUS=OWNER_READY_FOR_EVALUATION`
- Pixel: `Google Pixel 9 Pro XL`, serial `49121FDAS0025V`, Android API `36`
- Atlas ADB endpoint: authorized physical device; emulator excluded
- APK: `android-host/build/outputs/apk/debug/android-host-debug.apk`
- APK SHA-256: `C00FEFA53510F67A1EA7A561099C745EA0AD8BC46E1A0446E8D602AF7E148FE6`
- Install: `SUCCESS` via replacement install; application data was not cleared
- Normal launcher: `SUCCESS`; `MainActivity` automatically entered `DebugAlivenessActivity`
- Runtime process: remained alive as `com.animusmachinae.dll17` (PID `22720`) with `DebugAlivenessActivity` foreground
- Autonomous ticking: `VERIFIED`; screenshots captured three seconds apart differed (`1742a7cc68bb1e2a8da28cdd80a01ebe1b44eb4d917e205a753574ea8d0523a5` and `8f22355ac036ed5dc361294a193f73fb590636c1de114c7ed58b5bcc27228dd7`)
- TOUCH: `VERIFIED`; creature tap at `(500,995)` produced a subsequent changed frame (`9fae3bdf86f78231736655ffd3a200cc847046529c160ffd39f73ed9fa673094`)
- SHOW BALL (`PRESENT_OBJECT`): `VERIFIED`; owner control tap at `(816,1996)` produced a subsequent changed frame (`1c8d20e73e88c0a66297013d37ded211f61b4dba1c01f311a79f36559961d71b`)
- Crash/logcat check: `NO_MATCHING_ANDROIDRUNTIME_OR_FATAL_EXCEPTION`
- Screenshot evidence: `C:\Users\sketc\AppData\Local\Temp\d016x-pixel-after.png`, `C:\Users\sketc\AppData\Local\Temp\d016x-pixel-touch.png`, `C:\Users\sketc\AppData\Local\Temp\d016x-pixel-show-ball.png`
- Source changes during R1: `NONE`
- Owner aliveness verdict: `NOT RUN`; the device is handed off at the owner-verdict boundary

No A001 PASS or FAIL is claimed. R003-R009 remain blocked.
