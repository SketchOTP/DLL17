# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-13T01:20:00-04:00`

## Active state after adoption

- Local directive ID: D-005
- External directive ID: D005
- Objective: Complete the remaining R000 work and close the R000 exit gate, resolving the licence conflict, producing actual Android launch evidence, measuring the R000 runtime baseline, and building a reproducible hashed qualification evidence bundle.
- Current status: `COMPLETE`
- Acceptance: The licence conflict resolved in favour of the frozen proprietary policy, the shell installed and actually launched on an Android target with the visible state verified and a clean terminate and relaunch, measured baseline evidence recorded with no fabricated future budgets, a hashed reproducible qualification bundle, all existing tests still passing, GitHub CI passing for the qualified commit, every R000 exit-gate criterion PASS, no R001 or organism functionality introduced, HEAD matching origin/main, and a clean worktree.
- Current phase: R000 closed. The next hard gate is the DeterminismContractV1 freeze, followed by R001.
- Expected or actual touched areas: LICENSE, docs/, governance/, qualification/, tools/, .github/workflows/ci.yml, .agent/
- Immediate next action: Hold for architect review of the D-005 completion report; this snapshot is awaiting reset to IDLE once that review closes. Do not begin D006 or any R001 implementation.

## Temporary task-relevant facts

- R000 is closed as PASS. Both the Implementation Plan E2E exit gate and the Digital Living Lifeform charter exit gate were evaluated criterion by criterion and every criterion passed.
- The Android shell was qualified on physical hardware, a Pixel 9 Pro XL on Android 16 API 36 arm64-v8a, which satisfies the charter wording requiring target hardware rather than only the weaker device-or-emulator wording.
- Measured R000 baseline on that device: APK 29,245,211 bytes, installed code 18,735 KB, cold launch 512 ms, relaunch 603 ms, total PSS 110,612 KB, idle CPU 0.0 percent.
- Every future production budget is deliberately NOT ESTABLISHED. No number was invented.
- The qualification bundle binds thirty-seven constituents by SHA-256 with manifest hash e2290aea61abc2fd82c96db43653cf953daeeffa193c55af98268306a9654556, and CI verifies it on every push and pull request.
- The debug APK is byte-identical across a clean rebuild on this toolchain.
- The x86 Android emulator could not complete a qualification run. The android-37.0 google_apis x86_64 image crashes surfaceflinger under all three rendering backends on this host. R001 requires that target, so it must be resolved before R001 closes.
- The repository source licence is now proprietary with all rights reserved. The repository is still public, because D005 excluded changing visibility.
- The build host toolchain lives at ~/.local/toolchains and ~/Android/Sdk and requires JAVA_HOME and ANDROID_HOME.
- A000 aliveness spike harness and study scaffolding may proceed in parallel.
- R002 continuity and durability may proceed only after R001 passes.
- R003 through R009 production organism mechanisms are additionally blocked until A001 passes.
- No organism behavior exists. Every module contains structural markers only.
- D-001 remains recorded as nonconforming, and D-002, D-003 and D-004 remain recorded as accepted and complete.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify, ./gradlew clean build, ./gradlew :android-host:assembleDebug and tools/qualify_r000_android.sh on a physical device
- Result: `PASSED`

## Risks

- The x86 Android emulator is unusable on this host for the android-37.0 image. The canonical determinism matrix requires it for R001 cross-architecture qualification.
- The repository is public while carrying a proprietary licence, and an MIT grant was published for the revisions between the initial commit and the D-005 commit. Whether that historical grant remains effective is a legal question for the copyright holder.
- Determinism has been proven on exactly one hardware family. R001 requires identical canonical bytes across the full matrix, and nothing measured so far predicts that.
- Governance history contains a permanent nonconformance at D-001 that must not be rewritten by later work. Recorded as DEC-0002.

## Blockers

- None. R001 and later production phases remain gated behind the DeterminismContractV1 freeze, which is the intended program state rather than a blocker on this directive.

## Pending decisions

- Whether the repository should remain public now that the licence is proprietary.
- How to obtain a working x86 Android emulator target, or an alternative cross-architecture target, before R001 determinism qualification.
- Whether to place the canonical charter pages under any local mirror so that the external specification survives independently of the hosted pages.
- When to freeze DeterminismContractV1, which is the gate that unblocks R001.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
