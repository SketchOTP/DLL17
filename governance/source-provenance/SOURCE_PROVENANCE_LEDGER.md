# Source provenance ledger

Proves that this repository is greenfield: no organism implementation or state
was inherited from an earlier project.

| Fact | Evidence |
|---|---|
| Git history begins in this repository | Baseline commit `f82e1b2f7c138a7c4238f109b45a6562b8b18a21`, created under directive D-003. No history was imported, grafted or backdated. |
| No inherited organism source | Before D-004 the repository contained only governance, policy and validator files. Every Kotlin file in the repository was authored under D-004 and implements no organism behavior. |
| No inherited assets | The only authored asset is `android-host/src/main/res/drawable/ic_launcher_foreground.xml`, a placeholder vector authored under D-004. |
| Third-party code | Present only as declared, exactly pinned Gradle dependencies. No third-party source is vendored into this repository. |
| Binary files are generated evidence only | The repository's only binary files are `qualification/device-matrix/R000/shell_launch1.png` and `shell_launch2.png`, screenshots captured from the qualification device under D-005 by `tools/qualify_r000_android.sh`. They are recorded observations, not imported assets. |
| Qualification evidence is first-party | Everything under `qualification/evidence/R000/` and `qualification/device-matrix/R000/` was produced by running this repository's own tooling against this repository's own build. No qualification result was imported from any other project. |

## Zero-inheritance restated at R000 close

At the R000 exit gate, every Kotlin source file in this repository was authored
under D-004 or later, every module marker still declares
`CANONICAL_LOGIC_IMPLEMENTED = false`, and no organism schema, state, fixture or
qualification result exists to have been inherited. The greenfield claim is
therefore not merely historical: there is no organism implementation in the
repository to inherit anything into.

No copied code or asset may enter this repository without an entry in this
ledger and explicit architect approval.
