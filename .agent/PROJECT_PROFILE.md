# Project Profile

## Lifecycle

- Status: `ADOPTED`
- Last verified: `2026-08-13T10:40:00-04:00`

## Identity

- Project name or identifier: Digital Living Lifeform (repository directory `DLL17`)
- Purpose: Implementation repository for the Digital Living Lifeform program. It holds the governance baseline, the R000 greenfield project skeleton from D-004, and the R001 deterministic core from D-006. No organism behavior is implemented yet.
- Repository root: `/home/sketch/Projects/DLL17`
- Verified remote: `git@github.com:SketchOTP/DLL17.git`, set as origin under D-004. Local branch `main` tracks `origin/main`. Git provenance begins at baseline commit `f82e1b2f7c138a7c4238f109b45a6562b8b18a21`, created under D-003.
- Maturity or current phase: R001 closed as PASS under D-006. DeterminismContractV1 version 1 is frozen and the deterministic core is qualified byte-identically on the desktop JVM, the x86_64 Android emulator and Tensor hardware. No organism logic exists. The next hard gate is the ContinuityDurabilityContractV1 freeze followed by R002.

## Languages and runtimes

- Kotlin 2.4.10, used by every source module. Pinned in `gradle/libs.versions.toml`.
- Kotlin Gradle DSL (`.gradle.kts`), used by all build scripts.
- Python 3.14.4, verified with `python3 --version`. Used by the governance validator, its test script and the project identity checker.
- Markdown, used for all governance, policy, contract and registry files.
- XML, used for the Android manifest and resources.
- YAML, used for the GitHub Actions workflow.
- JSON, used for `.cursor/mcp.json`.
- Cursor rule files (`.mdc`) under `.cursor/rules/`.

## Tools

- Build: Gradle 9.5.0 through the checked-in wrapper, with Android Gradle Plugin 9.3.1 and JDK 17. Run `./gradlew build` from the repository root.
- Test: `./gradlew build` runs the module test suites; `python3 scripts/test_validate_governance.py` runs the governance validator self-test against an isolated pristine fixture under `scripts/fixtures/governance_template/`. Both pass as of the last verified date.
- Lint: Android Lint runs as part of `./gradlew build` for `android-host`. No linter is configured for the pure JVM modules at the last verified date.
- Type-check: none present. No type checker is configured in this repository at the last verified date.
- Packaging: `./gradlew :android-host:assembleDebug` produces a debug APK. Production signing is deferred, so no release packaging is configured.
- Source control is Git on branch `main`, established locally under D-003 and connected to the GitHub remote under D-004. Excluded from tracking are Python bytecode caches, local virtual environments, operating-system and editor scratch files, the development-only migration report that the validator forbids in the tracked tree, Gradle and Kotlin build outputs and caches, `local.properties`, IDE state, and Android build artifacts. Serena local overrides remain excluded by the pre-existing `.serena/.gitignore`.
- Preferred navigation/indexing: direct file reading and text search. `.serena/project.yml` and `.cursor/mcp.json` declare optional external navigation servers whose availability is not verified from within this repository.

## Verified commands

- `python3 scripts/validate_governance.py --mode ADOPTED` — repository-wide governance validation in adopted mode. Executed on 2026-08-12.
- The validator also accepts an unadopted mode for the clean distributable fixture state. That mode no longer applies to this repository because the repository is now adopted, so its exact flag is deliberately not recorded here as a runnable project command.
- `python3 scripts/test_validate_governance.py` — validator self-test script. Executed on 2026-08-12.
- `python3 tools/verify_project_identity.py` — checks the build files against the frozen ProjectIdentityBuildContractV1. Executed on 2026-08-12.
- `./gradlew build` — compiles every module and runs every module test suite. Executed on 2026-08-12.
- `./gradlew :desktop-runner:run` — runs the headless JVM runner. Executed on 2026-08-12.
- `./gradlew :android-host:assembleDebug` — builds the debug APK. Executed on 2026-08-12.
- `python3 tools/build_qualification_bundle.py --verify` — verifies the hashed R000 qualification evidence bundle against the working tree. Executed on 2026-08-12.
- `tools/qualify_r000_android.sh` followed by the adb serial of a connected target — runs the Android install, launch, visible-state, terminate and relaunch qualification. Executed on 2026-08-12 against a physical Pixel 9 Pro XL.
- `python3 tools/generate_lookup_tables.py --check` — verifies the generated lookup table matches its generator, and cross-checks the Kotlin codec and SHA-256 against the Python implementation. Executed on 2026-08-13.
- `tools/qualify_r001_determinism.sh` followed by an adb serial and a target label — runs the instrumented determinism matrix on that target and records what it computed. Executed on 2026-08-13 against the x86_64 emulator and a physical Pixel 9 Pro XL.
- `./gradlew :android-host:connectedDebugAndroidTest` — runs the Android instrumented tests on connected targets; set ANDROID_SERIAL to select one. Executed on 2026-08-13.
- Project-specific commands: the Gradle and Python commands listed above. No organism run, replay or qualification command exists in this repository at the last verified date.

## Constraints

- Platform/compatibility: the program targets Android as host, sensor and evidence source, persistence environment and presentation surface, with a deterministic core required to reproduce byte-identical canonical results across the qualified JVM and Android hardware matrix. The build host carries no Java, Gradle or Android SDK by default; the toolchain used for D-004 was installed into the user home at `~/.local/toolchains` and `~/Android/Sdk`, and builds need `JAVA_HOME` and `ANDROID_HOME` pointed at those paths. Android qualification uses a physical Pixel 9 Pro XL attached over USB and authorized by the owner. The android-37.0 google_apis x86_64 emulator image is unusable on this host because it crashes surfaceflinger under all three rendering backends; the working emulator target is the system-images android-36 aosp_atd x86_64 ATD image, AVD name dll17_r001_atd, which boots headless in under a minute.
- Security: no credentials, secrets or personal data are stored in this repository. Raw logs, full source files, secrets and unsupported claims must be kept out of external memory systems.
- Data handling: governance ledgers in `.agent/` are append-only for history and must not be rewritten. Local repository files remain authoritative for project directives and outcomes.
- Deployment: no deployment, release or distribution mechanism exists in this repository. No deployment has occurred.
