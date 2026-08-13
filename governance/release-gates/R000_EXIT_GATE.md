# R000 exit gate

Implementation Plan E2E states that R000 passes only when every criterion below
is met. This record states the honest status of each. It is a status record, not
a claim that the gate is closed.

| Criterion | Status | Evidence |
|---|---|---|
| Android shell launches on at least one target device or emulator | `NOT MET` | The shell assembles and packages, but no device or emulator has run it. No Android device or emulator is available on the build host. |
| Desktop headless runner executes | `MET` | `./gradlew :desktop-runner:run` prints the module inventory and exits 0. |
| All modules compile in their intended runtime boundary | `MET` | `./gradlew build` compiles all five modules; each `core-*` module asserts the absence of the Android framework in test. |
| Governance and provenance files are committed | `MET` | `docs/`, `governance/` and `.agent/` are tracked. |
| Device matrix and provisional budgets are recorded | `NOT MET` | `docs/release/DEVICE_AND_RESOURCE_BUDGETS.md` deliberately records no numbers; every value would be guessed. |
| CI produces a clean evidence bundle | `PARTIAL` | `.github/workflows/ci.yml` runs governance validation, governance self-tests, JVM tests, the full build and the Android debug assembly. It does not yet emit a hashed evidence bundle. |
| The repository contains no inherited organism implementation or state | `MET` | `governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md`. |

The R000 exit gate is therefore **open**. Closing it requires a device or
emulator run, recorded budgets, and an evidence-bundle step in CI. Directive
D004 scoped the buildable foundation, not the gate closure.
