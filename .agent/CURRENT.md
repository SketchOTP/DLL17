# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-12T20:21:55-04:00`

## Active state after adoption

- Local directive ID: D-004
- External directive ID: D004
- Objective: Build the buildable greenfield R000 project skeleton, freeze ProjectIdentityBuildContractV1, create the mandatory R000 registries, establish CI, and publish the work to the authoritative GitHub repository.
- Current status: `COMPLETE`
- Acceptance: The frozen identity contract, the E2E module boundaries, Android-free core modules, a buildable and launchable Android shell, all seven mandatory registries with no invented organism entries, passing governance validation and self-tests, a passing root build and Android debug assembly, an existing CI workflow, the work pushed to SketchOTP/DLL17, and a clean final worktree.
- Current phase: R000 greenfield project initialization.
- Expected or actual touched areas: settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/, core-math/, core-crypto/, core-state/, desktop-runner/, android-host/, docs/, governance/, qualification/, tools/, .github/workflows/ci.yml, .gitignore, scripts/test_validate_governance.py, .agent/
- Immediate next action: Hold for architect review of the D-004 completion report; this snapshot is awaiting reset to IDLE once that review closes.

## Temporary task-relevant facts

- Branch main tracks origin/main at git@github.com:SketchOTP/DLL17.git. The R000 project commit is 6370986, the merge of the owner-created initial commit is 8b2f155, and the pushed head before this record was de69ee0.
- GitHub Actions run 31654374955 passed both jobs on a hosted runner.
- The project builds with Gradle 9.5.0, JDK 17, Kotlin 2.4.10, Android Gradle Plugin 9.3.1, compileSdk and targetSdk 37, minSdk 29 and Compose BOM 2026.06.00.
- The build host had no Java, Gradle or Android SDK before D-004. The toolchain lives at ~/.local/toolchains and ~/Android/Sdk and requires JAVA_HOME and ANDROID_HOME.
- No Android device or emulator is available on this host, so the shell has been assembled and packaged but never launched.
- The R000 exit gate defined by Implementation Plan E2E is still open. A device launch, recorded device and resource budgets, and a hashed CI evidence bundle remain outstanding.
- Gate state recorded from the canonical charter. R000 greenfield project initialization is authorized now.
- A000 aliveness spike harness and study scaffolding may proceed in parallel.
- The next production hard gate is the DeterminismContractV1 freeze followed by R001 deterministic fixed-point and replay qualification.
- R002 continuity and durability may proceed only after R001 passes.
- R003 through R009 production organism mechanisms are additionally blocked until A001 passes.
- No organism behavior exists. Every module contains structural markers only.
- D-001 remains recorded as nonconforming, and D-002 and D-003 remain recorded as accepted and complete.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, ./gradlew build and ./gradlew :android-host:assembleDebug
- Result: `PASSED`

## Risks

- The Android shell has never run. Packaging success is not launch success, and the R000 exit gate cannot close until a device or emulator run exists.
- The repository is public and carries an MIT LICENSE created by the owner, which contradicts the proprietary no-redistribution clause frozen in ProjectIdentityBuildContractV1. Recorded, not resolved.
- Governance history contains a permanent nonconformance at D-001 that must not be rewritten by later work. Recorded as DEC-0002.

## Blockers

- None. R001 and later production phases remain gated as described above, which is the intended program state rather than a blocker on this directive.

## Pending decisions

- When and how to obtain a device or emulator so the R000 exit gate can be closed.
- Whether to place the canonical charter pages under any local mirror so that the external specification survives independently of the hosted pages.
- Whether CI should emit a hashed evidence bundle before R001 or as part of R001 qualification.
- Which of the MIT LICENSE or the proprietary contract clause is authoritative, and whether the repository should stay public.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
