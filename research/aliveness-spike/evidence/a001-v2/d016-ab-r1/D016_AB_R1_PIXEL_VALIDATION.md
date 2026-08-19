# D016-AB-R1 physical validation

- Date: 2026-08-19
- Device: Pixel 9 Pro XL, serial `49121FDAS0025V`
- APK: `android-host-debug.apk`
- APK SHA-256: `52defd1d6505884122b844a6636d8028489ef98efd34bfc6028108c08a896817`
- APK source commit: `67b7508f62ce66d93ea063988ae03c7a4d55352c`
- Install: PASSED after removing only the prior differently-signed `com.animusmachinae.dll17` debug package
- Activity: `com.animusmachinae.dll17/.android.DebugAlivenessActivity`

## Physical observations

- The APK was exercised with real handheld movement through Android `SensorManager`; no synthetic samples or diagnostic task were used.
- The 45.018-second runtime recording shows an initial `waiting for movement` period, followed by visible organism-attended `WALKING` and `STILL` transitions with `ORIENT`.
- No rapid STILL/WALKING/RUNNING flapping was visible in the recording; transitions were sparse and stabilized.
- The attended transitions produced `speech=NONE` and no utterance, demonstrating that world evidence does not force speech.
- No `VOCALIZE` action occurred during this bounded run, so actual TTS playback was not exercised; this is recorded as not observed, not as a TTS failure.
- The app remained foreground after recording. The captured logcat contains no `FATAL EXCEPTION` or `ANR in` entries for the run.

## Bounded battery observation

- Window: one 45.018-second runtime recording; no all-day extrapolation.
- Before: level `78%`, voltage `4158mV`, temperature `30.0C`.
- After: level `78%`, voltage `4164mV`, temperature `30.1C`.
- Battery statistics were reset before the window and captured after it. Android did not expose a package-specific mAh estimate in this short interval; no energy claim is made.

## Evidence files

- `D016_AB_R1_PIXEL_RUNTIME.mp4` — SHA-256 `c34638eb585dc66f585b43ceb36a097105b115a51258c2113f5b727c5f2c44ed`, duration `45.018322s`
- `D016_AB_R1_PIXEL_FINAL.png` — SHA-256 `d34cb2115a5c7ccbbacf6c241ba9b8e9c794815840be68be5cbaf5a55ff38ef0`
- `D016_AB_R1_LOGCAT.txt` — SHA-256 `8721cb63b636db7c8bb646ba4e97647b084b4cd18cf5fb1178205bd4179e9b61`
- `D016_AB_R1_BATTERYSTATS.txt` — SHA-256 `2c840b10cc5c7549a78e809ff4fdaff5991e622dabe434f8717a044ccfaeeb60`
- `D016_AB_R1_BATTERY_AFTER.txt` — SHA-256 `8c47b6d9559a4db337c0270941ec359002f998f2c9e4983d7fed25607f7827be`

This is physical implementation evidence only. It does not claim A001 success
and does not unblock R003-R009.
