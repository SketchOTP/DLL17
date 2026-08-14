# Qualification evidence index

| Phase | Gate | Evidence bundle | Status |
|---|---|---|---|
| R000 | Greenfield project initialization | `governance/qualification/R000_QUALIFICATION_BUNDLE.md` | `PASS` — closed under D005. Gate record: `governance/release-gates/R000_EXIT_GATE.md`. |
| R001 | Deterministic fixed-point spike | `governance/qualification/R001_QUALIFICATION_BUNDLE.md` | `PASS` — closed under D006. The determinism-matrix criterion rests on evidence plus the 2026-08-13 canonical amendment that made Snapdragon optional. Gate record: `governance/release-gates/R001_EXIT_GATE.md`. |
| R002 | Lifecycle, durability, trusted time, reconciliation | `governance/qualification/R002_QUALIFICATION_BUNDLE.md` | `PASS` — closed under D007. Gate record: `governance/release-gates/R002_EXIT_GATE.md`. |
| A000 | Aliveness spike | `governance/qualification/A000_QUALIFICATION_BUNDLE.md` | `COMPLETE` under D008 and D009. Research track, not a production gate. D008 returned five negative findings and an empty curiosity feasible region; D009 remediated the candidate under unchanged thresholds and all 24 findings now hold. Gate record: `governance/release-gates/A000_EXIT_GATE.md`. Pinned to `4a2b1e4c7ce1326b5c8d5b08d873df7f581186d7`. |
| A001-PRE | Activation package | `governance/qualification/A001_ACTIVATION_PACKAGE_BUNDLE.md` | `COMPLETE` under D010. The study protocol, instrument, comparator manifest, sealed pilot, feasibility calculator, analysis pipeline, participant materials and reviewer onboarding, all prepared without human data. Gate record: `governance/release-gates/A001_ACTIVATION_GATE.md`. |
| R012-SUB | Persistence, recovery and identity substrate | `governance/qualification/R012_SUBSTRATE_BUNDLE.md` | `COMPLETE` for the scope the 2026-08-14 parallel amendment authorized, under D011. Not the R012 product exit gate. Superseded on one point by R012-SUB-V2. Gate record: `governance/release-gates/R012_SUBSTRATE_GATE.md`. Pinned to `afd0ecdb21bd20a00d4f3b6ae69d31e61890707c`. |
| R012-SUB-V2 | Local-storage epoch separation | `governance/qualification/R012_SUBSTRATE_BUNDLE_V2.md` | `PASS` under D013, 55 / 55 fixtures. Implements `LocalStorageCryptographyContractV2`, which supersedes V1 and amends `ContinuityDurabilityContractV1` sections 13.3–13.5. Gate record: `governance/release-gates/R012_SUBSTRATE_GATE_V2.md`. |
| R012-DEV | R012 substrate on Android hardware | `governance/qualification/R012_DEVICE_BUNDLE.md` | `BLOCKED_DEVICE_UNAVAILABLE` under D012. The Android Keystore adapter, the app-private storage adapter and the 45-fixture device suite are complete and ran on an emulator (44 / 45 held); no physical device was reachable, and an emulator does not substitute for one. Gate record: `governance/release-gates/R012_DEVICE_GATE.md`. Pinned to `4700b0762cad3b1bb63a69be4f7eca9caea3b819` so the declared failure stays verifiable. |
| R012-DEV-V2 | R012 substrate on Android, after epoch separation | `governance/qualification/R012_DEVICE_BUNDLE_V2.md` | Still `BLOCKED_DEVICE_UNAVAILABLE` under D013 — no physical device was reachable to D013 either. The device suite re-ran at fixture set version 2 with 46 / 46 held, including `DV-KS-ROTATION-READBACK-01`, which D012 had filed as `NOT HELD`. |
| A001 | Aliveness gate | none | `BLOCKED`. Five outstanding blockers, none clearable by code: unqualified baseline, unregistered variance pilot, no released paired-difference SD, three unassigned reviewer roles, no owner resource ceiling. |

## R000 evidence layout

| Path | Contents |
|---|---|
| `governance/qualification/R000_QUALIFICATION_BUNDLE.md` | Hashed manifest binding the whole R000 claim. Verified in CI against its pinned commit. |
| `qualification/evidence/R000/` | Governance validation, self-test, identity check, build, runner, Android assembly, toolchain environment. |
| `qualification/device-matrix/R000/` | Device matrix and raw Android install, launch, visible-state, terminate and relaunch evidence, including screenshots and logcat. |
| `docs/release/DEVICE_AND_RESOURCE_BUDGETS.md` | `R000_MEASURED_BASELINE` values and the budgets deliberately left unfrozen. |

## R001 evidence layout

| Path | Contents |
|---|---|
| `governance/qualification/R001_QUALIFICATION_BUNDLE.md` | Hashed manifest binding the whole R001 claim, including the frozen contract and every canonical source file. Verified in CI. |
| `docs/architecture/DeterminismContractV1.md` | The frozen contract itself, with the candidate decision record. |
| `qualification/fixtures/R001/` | Frozen golden vectors for `R001-FIXTURES-V1` and the desktop reference report. |
| `qualification/replay/R001/` | Replay equivalence per fixture and the eight-boundary crash recovery sweep. |
| `qualification/device-matrix/R001/` | Cross-target determinism matrix and per-target records for the desktop JVM, the x86_64 emulator and Tensor hardware. |
| `qualification/evidence/R001/` | Governance validation, self-test, identity check, build, runner, lookup-table check, toolchain environment. |
| `docs/release/DEVICE_AND_RESOURCE_BUDGETS.md` | `R001_MEASURED_BASELINE` panic-witness measurements and the budgets still unfrozen. |

## How the two kinds of evidence differ

R000 and R001 evidence must not be read the same way.

R000's device evidence is a record of observations that varied between runs.
Timings and process IDs differ on every launch, and the committed artifacts are
what was actually seen rather than a claim that identical logs will recur.

R001's determinism evidence is expected to be **byte-identical** on every run and
every target. Nothing timing-dependent enters it. A single differing hex digit in
`R001_EVIDENCE_DIGEST` is a qualification failure, not noise. That is the entire
claim the phase makes.

## Reproducing

```
python3 tools/build_qualification_bundle.py --verify
python3 tools/generate_lookup_tables.py --check
./gradlew build
./gradlew :desktop-runner:run
tools/qualify_r001_determinism.sh <adb-serial> <target-label>
```

CI runs the first four on every push and pull request, and fails the build if the
CI runner's own JVM does not reproduce the frozen golden digest. Device evidence
is reproduced by running the harness against a connected target.

## A note on closed phases

Once a phase's gate closes, its bundle is pinned in
`tools/build_qualification_bundle.py` to the commit that was qualified, and
verification reads that commit's blobs rather than the working tree. A closed
gate must not be breakable by a later phase editing a shared build file or
registry. R000 is pinned to `43054d0a2a210bc48563cc81016d6083bff2a182`, R001 to
`e442e1478deed9e70f5f2b547c92071ba8bce6ff`, R002 to
`7f6f37fabba6a5ad4af2fd517e62cb4c08dbfeb2` and A000 to
`4a2b1e4c7ce1326b5c8d5b08d873df7f581186d7`. A000 was pinned under D010 for
exactly this reason: D010 adds the preregistered third ablation cohort to the
kernel and reconciles figures in the gate record, both of which are A000
constituents.

## A000 evidence layout

A000 evidence must not be read as production qualification. It is research
evidence about whether the organism hypothesis is worth testing on people, and
the track is disposable: nothing under `research/` may be copied into production
organism modules. Findings cross into production only through the canonical
adoption process — measured finding, preregistered evidence, adopted
architecture amendment, newly frozen production contract.

| Path | Contents |
|---|---|
| `governance/qualification/A000_QUALIFICATION_BUNDLE.md` | Hashed manifest binding the research contracts, implementation, isolation boundary and evidence. |
| `research/aliveness-spike/study-protocol/` | The twenty A000/A001 research contracts with their exact `FROZEN` / `READY_FOR_HUMAN_EVIDENCE` / `BLOCKED_*` status. |
| `qualification/fixtures/A000/` | `A000-FIXTURES-V1` golden vectors and the full kernel report including sample decision traces. |
| `qualification/longitudinal/A000/` | The accelerated research findings, positive and negative. |
| `research/aliveness-spike/evidence/` | Curiosity-envelope feasibility search output, the executable governance audit, the baseline coverage manifest, the synthetic A001 dry run, and the preserved D008 negative evidence. |
| `qualification/evidence/A000/` | Governance validation, identity check, build, kernel run, toolchain environment. |

## Why A000 has no pass/fail gate

R000, R001 and R002 each answer "does the implementation meet a frozen
contract?", which has a yes or no. A000 asks "is this hypothesis worth
testing?", which does not.

Under D008 five of twenty-two findings did not hold and the joint
curiosity-envelope feasible set was empty. Those were results, recorded with
their configurations, not failures to be hidden — and they are retained in full
under `research/aliveness-spike/evidence/negative/D008/`. Under D009 the
candidate was revised against them, under unchanged thresholds, and all
twenty-four findings hold with twenty-seven of twenty-seven feasible grid
points. Both records stand; the second does not erase the first.

## R012 substrate evidence layout

| Path | Contents |
|---|---|
| `governance/qualification/R012_SUBSTRATE_BUNDLE.md` | Hashed manifest binding the five frozen contracts, the implementation, the qualification suites and the measured evidence. |
| `docs/architecture/PersistenceBackendContractV1.md` and the four other R012 contracts | The frozen contracts themselves, with their decision records and their blocked items. |
| `qualification/fixtures/R012/R012_REPORT.txt` | The 42-fixture kernel report, including the real-process-death fault matrix. |
| `qualification/evidence/R012/backend_benchmark.txt` | Six candidate backends measured in isolation on ext4/NVMe. The selection evidence. |
| `qualification/evidence/R012/performance.txt` | Measured latency distributions across three repetitions, encrypted-record overhead, package size and duration. |
| `qualification/evidence/R012/` | Governance validation, identity check, build, toolchain environment. |

R012 substrate evidence is desktop-JVM evidence against a real ext4/NVMe
filesystem. The Android half is a separate bundle, below.

## R012 device evidence layout

| Path | Contents |
|---|---|
| `governance/qualification/R012_DEVICE_BUNDLE.md` | Hashed manifest binding the Android adapter, the exclusion rules and verifier, the device suite and the target evidence. |
| `qualification/device-matrix/R012/DEVICE_MATRIX.md` | Per-target results, what the emulator row does and does not establish, and the declared fixture failure. |
| `qualification/device-matrix/R012/x86_emulator_qualification.txt` | The complete 45-fixture device kernel report the emulator produced. |
| `qualification/device-matrix/R012/x86_emulator_performance.txt` | Measured latency distributions on the emulator target. Measurements only; no threshold is derived. |
| `qualification/evidence/R012/backup_exclusion.txt` | Backup and device-transfer exclusion read from the built debug and release packages. |

D012 closed as `BLOCKED_DEVICE_UNAVAILABLE`: the Android Keystore adapter and the
device suite exist and run, and **no physical Android device was reachable**, so
Keystore hardware backing, real device flash, physical-device latency and
on-hardware restart remain unqualified. D013 re-ran the suite after correcting the
contract defect the suite found, and it is still blocked on the same input.

## Why R012 has two substrate bundles and two device bundles

D012 found a defect in a contract that R012-QB-1 had already been qualified
against, and D013 corrected it. Two ways of recording that were available.

The bundles could have been rebuilt in place, which would make the record read as
though the substrate had always been correct and as though
`DV-KS-ROTATION-READBACK-01` had always passed. Instead both original bundles are
pinned to the commits they were qualified at — including `R012DEV-QB-1`, whose
gate is *blocked* rather than passed — and the corrected work is a successor.

The cost is two extra bundles. What it buys is that a reader can still see, from
verifiable artifacts rather than from prose, that a fixture found a real defect in
a frozen contract and that the defect was escalated before it was fixed.

## A001 pre-activation evidence layout

| Path | Contents |
|---|---|
| `governance/qualification/A001_ACTIVATION_PACKAGE_BUNDLE.md` | Hashed manifest binding the protocol, instrument, comparator manifest, sealed pilot, feasibility calculator, analysis pipeline, participant materials and reviewer onboarding. |
| `research/aliveness-spike/evidence/A001_ACTIVATION_DRY_RUN.txt` | **Synthetic.** Every branch of the preregistered analysis, the sealed pilot channel, the feasibility calculator and the activation audit. Engineering evidence that the pipeline works; never evidence about the organism. |
| `research/aliveness-spike/evidence/BASELINE_COVERAGE_MANIFEST.txt` | The complete disclosure of the scripted comparator, generated from its implementation. |
| `research/aliveness-spike/evidence/GOVERNANCE_AUDIT.txt` | The 27-item activation audit and its five outstanding blockers. |
| `qualification/evidence/A001PRE/` | Governance validation, identity check, build, kernel reproduction, toolchain environment. |

**No human outcome data exists anywhere in this repository.** Everything in the
A001 pre-activation layout is either a specification or synthetic fixture data,
and the synthetic data is marked as such on every line it appears.
