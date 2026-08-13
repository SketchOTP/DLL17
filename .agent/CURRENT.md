# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-13T13:20:00-04:00`

## Active state after adoption

- Local directive ID: D-007
- External directive ID: D007
- Objective: Complete the R002 continuity, durability, trusted-time and reconciliation foundation, freezing ContinuityDurabilityContractV1 before any dependent behaviour is treated as qualified and qualifying the result against the canonical R002 exit gate.
- Current status: `COMPLETE`
- Acceptance: ContinuityDurabilityContractV1 frozen and conformed to, every R002 exit-gate criterion passing, process-death and restart fixtures preserving exactly the required durable state, no uncommitted material mutation exposed after crash or restart, reboot and clock-manipulation exploit fixtures passing, blind credit and trusted-time reconciliation bounded, storage pressure and durability safe hold and platform suspend and recovery ordering qualified, journal and snapshot and replay and recovery and compaction deterministic, R001 still green, a reproducible R002 evidence bundle, CI passing, HEAD matching origin/main, a clean worktree, and no R003 or later organism behaviour introduced.
- Current phase: R002 closed. The next gates are A000 and A001 on the aliveness track; R003 and later organism phases remain blocked behind A001.
- Expected or actual touched areas: docs/architecture/, docs/decisions/, docs/invariants/, docs/release/, core-continuity/, core-crypto/, desktop-runner/, android-host/, governance/, qualification/, tools/, settings.gradle.kts, .github/workflows/ci.yml, .agent/
- Immediate next action: Hold for architect review of the D-007 completion report; this snapshot is awaiting reset to IDLE once that review closes. Do not begin D008.

## Temporary task-relevant facts

- R002 is closed as PASS against both canonical gates, namely the Implementation Plan E2E nine-criterion gate and the charter section 18 gate covering five minutes, six hours, seventy-two hours, thirty days, six months and one year.
- ContinuityDurabilityContractV1 version 1 is FROZEN at docs/architecture/ContinuityDurabilityContractV1.md. It was committed alone, ahead of the implementation, so the freeze ordering is verifiable from git history rather than asserted.
- The frozen cross-target continuity digest is 556bbe49df16595f748a487f78a17a83866eb2a018814f69ee469d7976d58d21, compiled into core-continuity as R002QualificationKernel.GOLDEN_EVIDENCE_DIGEST.
- Three targets produced byte-identical output, namely the desktop JVM reference runner, the x86_64 Android emulator on API 36 and Tensor G4 hardware on a Pixel 9 Pro XL. Each Android target also reproduced the unchanged R001 digest in the same instrumented session.
- Canonical durations are i64 milliseconds throughout and never Fixed64, so a duration can never clamp silently where it should fail loudly.
- Blind decay credit is earned only from verified or authenticated elapsed time, carries its division remainder so session fragmentation cannot farm drift, and is keyed by boot identity so a relaunch spends nothing further.
- Blind consumption advances chronological age and circadian phase but never verifiedTimeTotalMillis, which is what stops a reboot loop from earning the verified time needed to buy more credit.
- Uncertainty debt is capped at seventy-two baseline-equivalent hours and any excess is forgiven at accrual rather than retained, so there is no invisible liability.
- Reconciliation plans a canonical event sequence and folds it through the ordinary reducer, so reduce equals replay holds for a reconciliation and an interruption at any chunk resumes to the identical sequence.
- AEAD_CHACHA20_POLY1305_V1 is implemented in core-crypto from RFC 8439 for the same reason SHA-256 is owned, and its nonce is derived from the key epoch and the durable sequence rather than drawn randomly.
- Encryption sits below the canonical byte layer, so canonical hashing and replay operate on plaintext and no determinism claim depends on a key.
- Three parameters R001 left NOT ESTABLISHED are now frozen, namely the Class O cadence at five hundred milliseconds, the maximum uncommitted window at one thousand milliseconds, and the panic-witness attempt deadline at twenty milliseconds. None was invented: two come from the canonical band and one from R001's own measurement.
- The 2026-08-13 architect amendment removed Snapdragon as a required R001 target, which closed the disagreement DEC-0015 recorded. R001's gate record and determinism matrix are updated, and R001's bundle is pinned to its qualified commit so those annotations cannot disturb the evidence.
- Two E2E work packages are incomplete for canonical reasons. R002.5 and R002.10 prepared-rest semantics need physiology that A001 gates. R002.12 recovery cryptography and the storage provider are BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY and BLOCKED_SPEC_RECOVERY_PROVIDER.
- No organism behavior exists. Physiology, drives, action selection, learning, memory, relationships, development, Torpor, AR and RouteEvidence remain unimplemented, and a structural test rejects organism vocabulary in the continuity module.
- D-001 remains recorded as nonconforming, and D-002 through D-006 remain recorded as accepted and complete.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify, python3 tools/generate_lookup_tables.py --check, ./gradlew clean build, ./gradlew :desktop-runner:run, and tools/qualify_r002_continuity.sh on both the x86_64 emulator and a physical Pixel 9 Pro XL
- Result: `PASSED`

## Risks

- D007 acceptance criterion three requires every R002 work package to be complete, while D007's own enumerated scope names none of the prepared-rest or recovery packages. On the enumerated scope R002 is complete; on the blanket criterion two packages are partial. The gate record states both readings rather than choosing the convenient one.
- Real storage failure modes are untested. The durable medium is an in-process byte log with explicit fault injection, which proves every ordering rule but is not a database.
- Android backup and device-transfer exclusion is asserted structurally rather than exercised, because the platform backup transport cannot be driven from a test on current Android releases.
- No frame-time or UI responsiveness measurement exists, because R002 wires no organism rendering to measure. The structural guarantee is that reconciliation yields and cannot see a UI thread.
- The panic-witness attempt deadline is derived from a healthy-process benchmark, which is the opposite of the condition the witness exists for. The chosen headroom is a response to that uncertainty rather than a removal of it.
- Exynos remains canonically conditional and Snapdragon optional, with no evidence in either direction.
- The repository is public while carrying a proprietary licence, and an MIT grant was published for the revisions between the initial commit and the D-005 commit.
- Governance history contains a permanent nonconformance at D-001 that must not be rewritten by later work. Recorded as DEC-0002.

## Blockers

- None. R003 and later organism phases remain gated behind A001, which is the intended program state rather than a blocker on this directive.

## Pending decisions

- Whether D007 acceptance criterion three governs the two partially complete work packages, which would make R002 BLOCKED rather than PASS without changing any evidence.
- When to freeze RecoveryCryptographyContractV1, which is what unblocks recovery cryptography and the identity-epoch authority.
- Which production storage provider to select, which is what unblocks BLOCKED_SPEC_RECOVERY_PROVIDER.
- Which storage engine adopts the durable medium interface, which is what makes persistence latency and Class W commit latency measurable.
- Whether the repository should remain public now that the licence is proprietary.
- Whether to place the canonical charter pages under a local mirror so the external specification survives independently of the hosted pages.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
