# Qualification evidence index

| Phase | Gate | Evidence bundle | Status |
|---|---|---|---|
| R000 | Greenfield project initialization | `governance/qualification/R000_QUALIFICATION_BUNDLE.md` | `PASS` — closed under D005. Gate record: `governance/release-gates/R000_EXIT_GATE.md`. |
| R001 | Deterministic fixed-point spike | `governance/qualification/R001_QUALIFICATION_BUNDLE.md` | `PASS` — closed under D006, with one criterion resting on an architect waiver. Gate record: `governance/release-gates/R001_EXIT_GATE.md`. |
| R002 | Lifecycle, durability, trusted time, reconciliation | none | Blocked until `ContinuityDurabilityContractV1` is frozen. |
| A000 | Aliveness spike | none | Not started in this repository. Unblocked by R001: it may now depend on the frozen fixed-point numeric library. |
| A001 | Aliveness gate | none | Blocked behind A000. |

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
registry. R000 is pinned to `43054d0a2a210bc48563cc81016d6083bff2a182`.
