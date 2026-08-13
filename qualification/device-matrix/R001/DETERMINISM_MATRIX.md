# R001 cross-target determinism matrix

The canonical architecture requires that identical normalized-event fixtures
produce **byte-identical canonical outputs and identical canonical state hashes**
on every target below. This file records what each target actually computed.

A target passes only if its `R001_EVIDENCE_DIGEST` equals the frozen golden
constant `R001QualificationKernel.GOLDEN_EVIDENCE_DIGEST`. Executing without
crashing is not passing.

- Fixture set: `R001-FIXTURES-V1` version 1
- Determinism contract: `DeterminismContractV1` version 1
- Golden evidence digest:
  `54bc044740a4c05b41b509a7160bff559e09421f2eaa55dc36c3d3ffadc1bd86`
- Run date: 2026-08-13 (UTC)

## Results

| Canonical target | Platform | Architecture / hardware | Fixture set | Evidence digest | Status |
|---|---|---|---|---|---|
| Desktop JVM reference runner | OpenJDK 17.0.20, Linux | `x86_64` host | `R001-FIXTURES-V1` v1 | `54bc0447…adc1bd86` | `PASS` |
| x86 Android emulator | Android 16, API 36, AOSP ATD | `x86_64`, emulated | `R001-FIXTURES-V1` v1 | `54bc0447…adc1bd86` | `PASS` |
| Tensor Android hardware | Android 16, API 36, Pixel 9 Pro XL | `arm64-v8a`, Google Tensor G4 | `R001-FIXTURES-V1` v1 | `54bc0447…adc1bd86` | `PASS` |
| Exynos Android hardware | — | — | — | not executed | `CONDITIONAL — canonical "when available"` |
| Snapdragon Android hardware | — | — | — | not executed | `OPTIONAL — not a gate target since the 2026-08-13 amendment` |

Full per-target records, including build fingerprints and the complete kernel
output each target produced, are in the sibling files `desktop_jvm.txt`,
`x86_emulator.txt` and `tensor_device.txt`.

## What the passing rows actually establish

The three passing targets are not three variations of one environment. They
differ along the two axes that matter:

| Axis | Coverage |
|---|---|
| Runtime | HotSpot JVM **and** Android ART |
| Instruction set | `x86_64` **and** `arm64-v8a` |
| Silicon | Desktop x86, emulated x86, Google Tensor G4 |

So the byte identity demonstrated here spans a JVM-to-ART boundary and an
x86-to-ARM boundary simultaneously. Those are the two boundaries most likely to
expose a latent floating-point dependency, an endianness assumption, an
intrinsic that differs by API level, or a hash-iteration-order leak. None
appeared.

## Per-section digests

Each target reports these independently, so a future cross-target failure names
the frozen decision that leaked rather than only reporting that the totals
differed.

| Section | Digest | Covers |
|---|---|---|
| Fixed-point | `1cb3b944aa46e05f322c91d3f724ab4f0094bb59c03e333577b9cb9a9f00d967` | Saturating arithmetic across the full operand sweep, including rounding ties and both bounds |
| Random | `b3ba5f87e4f0fd3a9f932f83ed6752a1e051dcfac2ffaeef041e9bacabc34017` | Substream derivation, draws, bounded draws, serialization round trip |
| Lookup | `951d66b7863bbf706dcedec396b02032bbf5d34dc2b17f2438bf5f4ef6eb19ab` | Generated table contents and their digest verification |
| Serialization | `552473e9afb48fbe61921bceedf1096aa1213dd98eb714c380bfc8bae79a43f5` | Snapshot encode/decode stability and the version-0 migration path |

All three passing targets reported all four values identically.

## Snapdragon — no longer a gate target

D006 was executed while the canonical determinism matrix listed Snapdragon
without the `when available` qualifier that Exynos carries, and no Snapdragon
device was available. The architect answered during execution that the Pixel 9
was sufficient, and this file originally recorded that as `WAIVED BY ARCHITECT`
rather than `PASS`, with the mismatch between matrix and specification left
open.

The **2026-08-13 architect amendment** to the canonical architecture page closed
the mismatch:

> Snapdragon qualification is no longer a required R001 gate target. […]
> Snapdragon Android hardware — optional additional evidence only; absence of a
> Snapdragon run does not block R001.

The required matrix is now Tensor hardware, the x86 Android emulator and the
desktop JVM reference runner, with Exynos conditional when available.

No Snapdragon evidence exists in either direction. The amendment removed the
requirement to test it; it did not create a result. A production support claim
covering Snapdragon devices would still want the row closed.

## Exynos — canonically conditional

The canonical text says Exynos is required "when available". No Exynos hardware
was available. Per that wording the gate remains explicitly conditional, and the
canonical architecture already states that it must be closed before production
support claims include that device class.

## Reproducing

```
./gradlew :desktop-runner:run
tools/qualify_r001_determinism.sh <adb-serial> <target-label>
```

Unlike the R000 device evidence, this evidence is expected to be **identical**
on every rerun and every target. Timings and process IDs do not appear in it. A
single differing hex digit is a qualification failure, not noise.

## Logcat handling

Device output is filtered to this project's own logging tag. The full logcat
buffer of a physical phone inventories its owner's installed applications and
their account and telephony activity; none of that is evidence about
determinism, and this repository is public.
