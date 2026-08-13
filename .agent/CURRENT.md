# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-13T10:40:00-04:00`

## Active state after adoption

- Local directive ID: D-006
- External directive ID: D006
- Objective: Implement and qualify the R001 deterministic core, freezing DeterminismContractV1 before writing any dependent implementation and qualifying the result against the canonical R001 exit gate.
- Current status: `COMPLETE`
- Acceptance: DeterminismContractV1 frozen and complete, the deterministic core conforming to it, every determinism area qualified, the mandatory registries carrying the R001 facts without speculative later entries, byte-identical canonical outputs across the required targets, reproducible evidence bound to the frozen contract and qualified commit, R000 and governance and build and CI qualifications still green, every R001 exit-gate criterion evaluated and passing, the work pushed to SketchOTP/DLL17, HEAD matching origin/main, CI passing, and a clean worktree.
- Current phase: R001 closed. The next hard gate is R002 lifecycle, durability, trusted time and reconciliation, which requires ContinuityDurabilityContractV1 to be frozen first.
- Expected or actual touched areas: docs/architecture/, docs/decisions/, docs/invariants/, docs/release/, core-crypto/, core-math/, core-state/, desktop-runner/, android-host/, governance/, qualification/, tools/, gradle/, .github/workflows/ci.yml, .agent/
- Immediate next action: Hold for architect review of the D-006 completion report; this snapshot is awaiting reset to IDLE once that review closes. Do not begin D007 or any R002 implementation.

## Temporary task-relevant facts

- R001 is closed as PASS. All three canonical R001 exit gates were evaluated criterion by criterion: the Implementation Plan E2E gate with eleven criteria, the charter section 18 gate with four, and the 2026-08-07 charter amendment gate with eight.
- DeterminismContractV1 version 1 is FROZEN at docs/architecture/DeterminismContractV1.md. Changing any clause requires a new contract version and regenerated golden vectors, never an edit.
- The frozen cross-target golden evidence digest is 54bc044740a4c05b41b509a7160bff559e09421f2eaa55dc36c3d3ffadc1bd86. It is compiled into core-state as R001QualificationKernel.GOLDEN_EVIDENCE_DIGEST and asserted by tests on every target.
- Byte-identical output was produced by three determinism matrix targets, namely the desktop JVM reference runner, the x86_64 Android emulator on API 36, and Tensor G4 hardware on a Pixel 9 Pro XL. That set spans HotSpot and ART, and x86_64 and arm64-v8a, at the same time.
- The Snapdragon matrix row was waived by the architect during execution and is recorded as WAIVED BY ARCHITECT, not PASS. The canonical page still lists it as required, so the matrix and the specification disagree until the page is amended.
- The x86 emulator target that R000 recorded as blocked is recovered. The fix was switching to the system-images android-36 aosp_atd x86_64 ATD image; the android-37.0 google_apis image remains unusable on this host.
- The frozen algorithm identifiers are HASH_SHA256_V1, PRNG_SPLITMIX64_V1, SUBSTREAM_DERIVE_V1, ROUND_HALF_AWAY_FROM_ZERO_V1 and LOOKUP_GENERATOR_V1.
- SHA-256 is implemented inside core-crypto rather than taken from java.security.MessageDigest, because provider resolution is not standardized across platforms. It is verified against FIPS 180-4 vectors and differentially against MessageDigest on every target.
- Math.multiplyHigh is prohibited in canonical arithmetic: it reached Android only at API 31 while minSdk is 29, so it would compile and then fail on a supported device.
- The panic-witness write cost was measured on two targets, at a p99 of 12,614 ns on Tensor and 16,215 ns on the emulator over 2,000 samples each. No attempt deadline was frozen from those measurements.
- Class O batching cadence, the maximum tolerated uncommitted window and the panic-witness attempt deadline are all deliberately NOT ESTABLISHED and belong to R002 or later.
- R001's durable medium is an in-process append-only byte log. R001 owns the durability contract; persistence semantics are R002's ContinuityDurabilityContractV1.
- The qualification bundle tool now pins a closed phase to its qualified commit and verifies it through git, so a later phase cannot break a gate that already closed. R000 is pinned to commit 43054d0a2a210bc48563cc81016d6083bff2a182 with its manifest hash unchanged.
- No organism behavior exists. Physiology, drives, action selection, learning, memory, relationships, development, Torpor, AR and RouteEvidence remain unimplemented.
- A000 aliveness spike harness and study scaffolding may now depend on the frozen R001 fixed-point numeric library.
- R002 continuity and durability is unlocked by this result. R003 through R009 production organism mechanisms remain additionally blocked until A001 passes.
- D-001 remains recorded as nonconforming, and D-002 through D-005 remain recorded as accepted and complete.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify, python3 tools/generate_lookup_tables.py --check, ./gradlew clean build, ./gradlew :desktop-runner:run, and tools/qualify_r001_determinism.sh on both the x86_64 emulator and a physical Pixel 9 Pro XL
- Result: `PASSED`

## Risks

- The Snapdragon determinism row has no evidence in either direction. The architect waiver removes the requirement to test it; it does not create a result. If production support claims later include Snapdragon devices, the row still needs closing.
- Exynos remains canonically conditional and unqualified, which the canonical architecture already states must be closed before production support claims include that device class.
- Determinism is proven against an in-process durable journal. Real storage failure modes, torn writes at the filesystem layer and database semantics are untested and belong to R002.
- No attempt deadline exists for the panic witness. The measurements were taken while the process was healthy, which is the opposite of the situation the witness exists for.
- The repository is public while carrying a proprietary licence, and an MIT grant was published for the revisions between the initial commit and the D-005 commit.
- Governance history contains a permanent nonconformance at D-001 that must not be rewritten by later work. Recorded as DEC-0002.

## Blockers

- None. R002 and later production phases remain gated behind the ContinuityDurabilityContractV1 freeze, which is the intended program state rather than a blocker on this directive.

## Pending decisions

- Whether to amend the canonical determinism matrix to match the architect's Snapdragon waiver, or to reinstate the requirement and reopen that row.
- Whether the repository should remain public now that the licence is proprietary.
- When to freeze ContinuityDurabilityContractV1, which is the gate that unblocks R002.
- Whether to place the canonical charter pages under a local mirror so the external specification survives independently of the hosted pages.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
