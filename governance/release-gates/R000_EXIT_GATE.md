# R000 exit gate

Evaluated under architect directive D005, the final R000 directive.

Two canonical pages state an R000 exit gate. Both are evaluated here, because
neither supersedes the other and passing only one would be a partial claim.

- `Implementation Plan E2E`, section "R000 exit gate".
- `Digital Living Lifeform`, section 18, "R000 — Greenfield project
  initialization / Exit gate".

Qualified evidence: `governance/qualification/R000_QUALIFICATION_BUNDLE.md`.

## Implementation Plan E2E — R000 exit gate

| # | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | Android shell launches on at least one target device or emulator | `PASS` | Installed and launched on a Pixel 9 Pro XL (Tensor, API 36). Cold launch `Status: ok`, 512 ms; visible state verified; terminate and relaunch clean. `qualification/device-matrix/R000/qualification_run.log`. |
| 2 | Desktop headless runner executes | `PASS` | `./gradlew :desktop-runner:run` exits 0 and prints the module inventory. `qualification/evidence/R000/desktop_runner.txt`. |
| 3 | All modules compile in their intended runtime boundary | `PASS` | `./gradlew clean build`, 119 tasks, all five modules, 15 tests. Each `core-*` module asserts the Android framework is absent from its classpath. `qualification/evidence/R000/gradle_build.txt`. |
| 4 | Governance and provenance files are committed | `PASS` | `docs/`, `governance/`, `qualification/` and `.agent/` are tracked. Governance validation passes in ADOPTED mode. `qualification/evidence/R000/governance_validation.txt`. |
| 5 | Device matrix and provisional budgets are recorded | `PASS` | `qualification/device-matrix/R000/DEVICE_MATRIX.md` and `docs/release/DEVICE_AND_RESOURCE_BUDGETS.md`. See the note below on the form this takes. |
| 6 | CI produces a clean evidence bundle | `PASS` | `tools/build_qualification_bundle.py` produces a hashed manifest; CI verifies it with `--verify` on every push and pull request and uploads the evidence as an artifact. `.github/workflows/ci.yml`. |
| 7 | The repository contains no inherited organism implementation or state | `PASS` | `governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md`. Every module marker still declares `CANONICAL_LOGIC_IMPLEMENTED = false`, asserted in test. |

## Digital Living Lifeform charter — R000 exit gate

| # | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | The new repository builds from a clean checkout | `PASS` | `./gradlew clean build` succeeds; CI builds the repository from a fresh checkout on a hosted runner with no local state. |
| 2 | The Android shell launches on target hardware | `PASS` | Physical Pixel 9 Pro XL, not an emulator. This satisfies the charter's stricter "target hardware" wording, not only the E2E "device/emulator" wording. |
| 3 | The headless test runner executes | `PASS` | `./gradlew :desktop-runner:run`; module test suites run under `./gradlew build`. |
| 4 | Repository structure and governance files are committed | `PASS` | Structure matches Implementation Plan E2E section 2. |
| 5 | Performance budgets and supported-device matrix are recorded | `PASS` | Measured baseline and device matrix recorded. See the note below. |
| 6 | Source-provenance ledger confirms zero inheritance | `PASS` | `governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md`. |

## Note on the budget criterion

Both gates ask for recorded budgets. Directive D005 explicitly prohibits
inventing future production budget values and states that they "remain
explicitly unfrozen until measured evidence and later qualification justify
them."

These are reconciled as the architect directed, not by implementer waiver:

- **Measured** values are recorded as `R000_MEASURED_BASELINE` — APK size,
  installed size, cold launch, relaunch, PSS/RSS breakdown, idle CPU.
- **Future production budgets** are recorded as `NOT ESTABLISHED`, each with the
  phase that owns it.

Measured observations are stronger evidence than the provisional hypotheses the
canonical pages contemplate, so this criterion is recorded as `PASS`. The
architect may overrule that reading; if the intent was that guessed provisional
numbers must exist, this criterion becomes `BLOCKED` and the gate reopens. The
decision is recorded as IMPL-0006 and is deliberately visible rather than
absorbed.

## Outstanding, and not counted as gate criteria

- **x86 emulator could not complete a run.** `surfaceflinger` crashes inside the
  `android-37.0` x86_64 emulator image on this host under all three rendering
  backends, taking `system_server` with it. No fatal exception was attributed to
  this application. R000 does not require the emulator, because hardware
  qualification is stronger, but **R001 does** — the canonical determinism
  matrix requires the x86 emulator as a cross-architecture target. This must be
  resolved before R001 closes.
- **Presentation inset.** The shell draws under the status bar. It is cosmetic,
  it is visible in `shell_launch1.png`, and it was deliberately not fixed:
  presentation is governed by `PresentationContractCatalog`, which is empty by
  design in R000.
- **Repository is public** while the licence is proprietary. D005 excluded
  changing visibility. Recorded in the contract and the licence inventory.

## Result

**R000 = PASS**

R000 is closed. The next hard gate is the `DeterminismContractV1` freeze,
followed by R001 deterministic fixed-point and replay qualification. No R001
implementation may begin until that contract is frozen.
