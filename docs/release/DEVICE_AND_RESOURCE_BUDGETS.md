# Device and resource budgets

This file holds two strictly separate things: what has been **measured**, and
what remains **unfrozen**. A measurement is evidence. A budget is a commitment.
R000 produces the first and deliberately produces none of the second.

## R000_MEASURED_BASELINE

Observations of the current empty R000 shell on real hardware. These are facts
about a foundation with no organism in it, recorded so later phases can measure
growth against a real starting point.

Target: Pixel 9 Pro XL (`komodo`), Google Tensor, `arm64-v8a`, Android 16,
API 36, `user` build. Full target record in
`qualification/device-matrix/R000/DEVICE_MATRIX.md`.

Method: `tools/qualify_r000_android.sh`, run 2026-08-13 UTC. Raw evidence in
`qualification/device-matrix/R000/`.

| ID | Measurement | Value | Source |
|---|---|---|---|
| `R000_MEASURED_BASELINE.apk_size_bytes` | Debug APK size | 29,245,211 bytes (27.9 MiB) | `qualification_run.log` |
| `R000_MEASURED_BASELINE.apk_sha256` | Debug APK digest | `8bc93994407648e72211da89c002421c03a4e9503ced49966c9e869e9f7c7784` | `toolchain_environment.txt` |
| `R000_MEASURED_BASELINE.installed_code_kb` | Installed code footprint on device | 18,743 KB (18.3 MiB) | `qualification_run.log` |
| `R000_MEASURED_BASELINE.cold_launch_total_ms` | `am start -W` TotalTime, cold | 422 ms | `qualification_run.log` |
| `R000_MEASURED_BASELINE.cold_launch_wait_ms` | `am start -W` WaitTime, cold | 426 ms | `qualification_run.log` |
| `R000_MEASURED_BASELINE.relaunch_total_ms` | TotalTime after force-stop | 349 ms | `qualification_run.log` |
| `R000_MEASURED_BASELINE.relaunch_wait_ms` | WaitTime after force-stop | 352 ms | `qualification_run.log` |
| `R000_MEASURED_BASELINE.total_pss_kb` | Total PSS at rest after launch | 84,897 KB (82.9 MiB) | `meminfo_launch1.txt` |
| `R000_MEASURED_BASELINE.total_rss_kb` | Total RSS at rest after launch | 187,852 KB | `meminfo_launch1.txt` |
| `R000_MEASURED_BASELINE.java_heap_pss_kb` | Java heap PSS | 11,668 KB | `meminfo_launch1.txt` |
| `R000_MEASURED_BASELINE.native_heap_pss_kb` | Native heap PSS | 8,052 KB | `meminfo_launch1.txt` |
| `R000_MEASURED_BASELINE.code_pss_kb` | Code PSS | 35,592 KB | `meminfo_launch1.txt` |
| `R000_MEASURED_BASELINE.graphics_pss_kb` | Graphics PSS | 8,072 KB | `meminfo_launch1.txt` |
| `R000_MEASURED_BASELINE.cpu_percent_at_rest` | CPU share, idle shell after launch | 0.0 % | `qualification_run.log` (`top`) |
| `R000_MEASURED_BASELINE.cpu_time_at_capture` | Accumulated CPU time at capture | 1.52 s | `qualification_run.log` (`top`) |

Run-to-run variance is real and was observed. Across repeated runs on the same
device, cold launch ranged roughly 350–610 ms and total PSS ranged roughly
62–114 MB, depending on what else the phone was doing and on whether graphics
buffers were still resident. These are single-sample observations of one device,
not a distribution. Treat them as an order of magnitude, not a specification.

Reading these honestly:

- Code dominates. That is Compose and the Android framework, not organism logic,
  of which there is none.
- The 0.0 % idle CPU figure is a single `top` sample of a static screen. It is
  evidence that the shell does no background work, not a sustained profile.
- Storage: the app's private data directory could not be measured. The
  qualification device is a `user` build with no root, so `/data/data` is not
  readable. Installed code size is recorded instead. An empty shell writes no
  persistent state, and persistence semantics do not exist until R002.
- Battery and thermal: **not measured.** A single sub-second launch produces no
  meaningful battery or thermal signal. Claiming one would be fabrication.

## Deliberately unfrozen

Every value below remains **`NOT ESTABLISHED`** by architect directive D005. No
number is assigned. Each is frozen only when the phase that owns the subsystem
can state it against measured device evidence, and each then becomes a
`PLATFORM_MEASURED` entry in `ParameterRegistry`.

| Budget | Status | Owning phase |
|---|---|---|
| Organism CPU budget | `NOT ESTABLISHED` | R001+ |
| Organism memory budget | `NOT ESTABLISHED` | R001+ |
| Thermal limits | `NOT ESTABLISHED` | later qualification |
| Battery budget | `NOT ESTABLISHED` | later qualification |
| Persistence latency | `NOT ESTABLISHED` | R002 |
| Frame-time / rendering budget | `NOT ESTABLISHED` | later qualification |
| Final storage ceiling | `NOT ESTABLISHED` | later qualification |
| Canonical reducer latency | `NOT ESTABLISHED` | R001 |
| Class W commit latency | `NOT ESTABLISHED` | R002 |
| Reconciliation wall time | `NOT ESTABLISHED` | R002+ |
| Sensor cadence | `NOT ESTABLISHED` | later qualification |
| Local speech latency | `NOT ESTABLISHED` | later qualification |
| Asset footprint | `NOT ESTABLISHED` | later qualification |
| Warm startup | `NOT ESTABLISHED` | later qualification |

The distinction matters: a guessed ceiling written down today would be treated
as a contract by later phases and would have to be unwound. Device evidence, not
guessed constants, freezes production limits.
