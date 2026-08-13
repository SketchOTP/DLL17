# Qualification evidence index

| Phase | Gate | Evidence bundle | Status |
|---|---|---|---|
| R000 | Greenfield project initialization | `governance/qualification/R000_QUALIFICATION_BUNDLE.md` | `PASS` — closed under D005. Gate record: `governance/release-gates/R000_EXIT_GATE.md`. |
| R001 | Deterministic fixed-point spike | none | Blocked until `DeterminismContractV1` is frozen. |
| A000 | Aliveness spike | none | Not started in this repository. |
| A001 | Aliveness gate | none | Blocked behind A000. |

## R000 evidence layout

| Path | Contents |
|---|---|
| `governance/qualification/R000_QUALIFICATION_BUNDLE.md` | Hashed manifest binding the whole R000 claim. Verified in CI. |
| `qualification/evidence/R000/` | Governance validation, self-test, identity check, build, runner, Android assembly, toolchain environment. |
| `qualification/device-matrix/R000/` | Device matrix and raw Android install, launch, visible-state, terminate and relaunch evidence, including screenshots and logcat. |
| `docs/release/DEVICE_AND_RESOURCE_BUDGETS.md` | `R000_MEASURED_BASELINE` values and the budgets deliberately left unfrozen. |

## Reproducing

```
python3 tools/build_qualification_bundle.py --verify
```

CI runs this on every push and pull request. Device evidence is reproduced by
rerunning `tools/qualify_r000_android.sh <serial>` against a connected target;
timings and process IDs will differ between runs.
