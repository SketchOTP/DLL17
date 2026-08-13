# Source provenance ledger

Proves that this repository is greenfield: no organism implementation or state
was inherited from an earlier project.

| Fact | Evidence |
|---|---|
| Git history begins in this repository | Baseline commit `f82e1b2f7c138a7c4238f109b45a6562b8b18a21`, created under directive D-003. No history was imported, grafted or backdated. |
| No inherited organism source | Before D-004 the repository contained only governance, policy and validator files. Every Kotlin file in the repository was authored under D-004 and implements no organism behavior. |
| No inherited assets | The only binary-equivalent asset is `android-host/src/main/res/drawable/ic_launcher_foreground.xml`, a placeholder vector authored under D-004. |
| Third-party code | Present only as declared, exactly pinned Gradle dependencies. No third-party source is vendored into this repository. |

No copied code or asset may enter this repository without an entry in this
ledger and explicit architect approval.
