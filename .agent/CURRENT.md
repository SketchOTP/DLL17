# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-14T16:40:00-04:00`

## Active state after adoption

- Local directive ID: D-012
- External directive ID: D012
- Objective: Close the Android-device half of the R012 persistence and cryptography substrate by implementing the Keystore adapter and app-private storage integration against the D011-frozen contracts and qualifying them on physical Android hardware.
- Current status: `BLOCKED`
- Acceptance: Keystore integration implemented against the frozen contracts; app-private persistence on the frozen backend; the Keystore key lifecycle survives process death and restart; interrupted rewrap qualified; key destruction never silently creates a fresh organism; device journal and snapshot recovery pass; death before acknowledgement creates no history and death after it preserves history; corrupt records detected and quarantined; canonical bytes and hashes identical before and after device persistence and recovery; backup and device-transfer exclusion verified from the built or installed package; real physical-device performance evidence exists; prior qualifications green; a reproducible D012 bundle exists; CI passes; HEAD matches origin/main; the worktree is clean.
- Current phase: A000 complete and remediated; the A001 activation package is prepared and its gate is correctly shut; the R012 substrate is qualified on the desktop and its Android adapter is implemented but unqualified on hardware. R003 through R009 remain blocked behind A001.
- Expected or actual touched areas: android-host/, core-persistence/src/main/kotlin/.../R012PerformanceHarness.kt, tools/, docs/decisions/, docs/invariants/, governance/release-gates/, governance/qualification/, qualification/device-matrix/R012/, qualification/evidence/R012/, .github/workflows/ci.yml, .agent/
- Immediate next action: Hold for architect review of the D-012 completion report. Two external inputs are now outstanding: a physical Android device reachable to adb, and an architect decision on the frozen-contract contradiction recorded by DV-KS-ROTATION-READBACK-01. The A001 inputs are unchanged. Do not begin D013.

## Temporary task-relevant facts

- Five R012 contracts are frozen: PersistenceBackendContractV1, LocalStorageCryptographyContractV1, RecoveryCryptographyContractV1, IdentityAuthorityProtocolV1 and RecoveryPackageStoreContractV1. Recorded as DEC-0029. None was changed by D-012.
- The selected backend is SEGMENTED_APPEND_LOG_V1, a single-writer append-only log with one metadata-inclusive fsync per acknowledged commit. Recorded as DEC-0028.
- The R012 kernel is R012-FIXTURES-V1 version 1 with digest 48bd44a31a3a952cf884b358c5e587b93a24875f1d36d7625e6b4b7d5f62127f, forty-two fixtures, all held, checked by CI.
- The R012 substrate bundle is R012-QB-1 with manifest hash 644ee8e99d6192aee59aab7497c3532f90a06d61bc20eae5871216147bccf9f7 over forty constituents, now pinned to commit afd0ecdb21bd20a00d4f3b6ae69d31e61890707c. Recorded as IMPL-0071.
- The Android device bundle is R012DEV-QB-1 with manifest hash 851005132d78a9d92fd653d2081f1f17ca9304b494dfb0d2445efd9fdb44274b over twenty constituents.
- The device kernel is R012-DEVICE-FIXTURES-V1 version 1 with digest ac38a91d98d5e422a1e1077be151e5a90b290efa89f91fa217435461797196a9, forty-five fixtures, forty-four held on an x86 emulator.
- No physical Android device is reachable. The emulator run is supplementary API-path coverage and is filed as such, not as device evidence. Recorded as DEC-0031.
- The emulator reported SOFTWARE key backing, so hardware-backed and StrongBox Keystore behaviour is unqualified.
- The Android key container is an AndroidKeyStore HMAC-SHA256 key, because an AES key cannot supply a root secret that is stable across restarts. Recorded as IMPL-0066 and L-0039.
- Opening the container never generates missing material, and a birth requires both the key state and the container material to be absent. Recorded as IMPL-0067 and INV-0055.
- Backup and device-transfer exclusion is verified from the built debug and release packages by following the merged manifest's own resource references, and is now a CI step. Recorded as IMPL-0068 and INV-0056.
- Fixture DV-KS-ROTATION-READBACK-01 does not hold: after a wrapping-epoch rotation the existing journal does not open, contradicting the rotation promise in LocalStorageCryptographyContractV1. Escalated rather than patched. Recorded as DEC-0032 and L-0037.
- The R012 performance harness could not run on Android at all until its filesystem probe stopped depending on Files.getFileStore, which Android refuses. Recorded as IMPL-0070 and L-0038.
- Killing a process leaves the page cache intact, so power loss is not proven by any test here and is disclosed as unproven. Recorded as L-0035.
- No human outcome data exists anywhere in the repository and no R003 through R009 organism mechanism exists.
- D-001 remains recorded as nonconforming, and D-002 through D-011 remain recorded as accepted and complete.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify, python3 tools/generate_lookup_tables.py --check, python3 tools/verify_backup_exclusion.py, ./gradlew clean build, ./gradlew :desktop-runner:run, ./gradlew :desktop-runner:r012Qualification, ./gradlew :desktop-runner:r012Performance, ./gradlew :research:aliveness-spike:accelerated-sim:run, ./gradlew :research:aliveness-spike:analysis:a001DryRun, and ./gradlew :android-host:connectedDebugAndroidTest on an x86 emulator
- Result: `PASSED`

## Risks

- No physical Android device is reachable, so Keystore hardware or StrongBox backing, real device flash behaviour, physical-device latency and on-hardware restart are unqualified. The emulator run does not substitute for them.
- Two frozen contracts contradict each other on what a wrapping-epoch rotation does to existing records. Until the architect resolves it, a rotation in production would make the journal unreadable.
- Power-loss durability is not proven by any test here. It rests on the force-per-commit policy, which is stated rather than measured.
- The measured latencies are reference-machine figures on a desktop NVMe device. No production threshold is derived from them and none may be until device evidence exists.
- The qualifying recovery provider is filesystem-backed. A network provider is a product decision needing an owner, credentials and a privacy review, and must pass the same conformance suite.
- The identity authority has no transport, hosting, backup or incident procedure. Those are BLOCKED_SPEC_SERVICE_OPERATIONS.
- A001 remains blocked on five inputs that no code can supply: an independently qualified scripted baseline, a registered variance pilot, the paired-difference standard deviation, three named eligible reviewers, and an owner resource ceiling.
- The graded instrument is frozen but not cognitively pretested, and was written by a party with an interest in the outcome.
- No institutional review board, ethics committee or data-protection review has seen the A001 study, none is claimed, and whether one is required is undetermined.
- The habitat remains abstract, and carries circadian structure added specifically to make a mechanism testable.
- The A000 envelope search still runs outside CI and its result is committed evidence rather than a reproduced check.
- The repository is public while carrying a proprietary licence, and an MIT grant was published for the revisions between the initial commit and the D-005 commit.
- Governance history contains a permanent nonconformance at D-001 that must not be rewritten by later work. Recorded as DEC-0002.

## Blockers

- A001 cannot begin. The activation audit reports BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED, BLOCKED_VARIANCE_PILOT_NOT_REGISTERED, BLOCKED_SPEC_PAIRED_DIFFERENCE_SD, BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED and BLOCKED_SPEC_STUDY_BUDGET.
- The device half of the R012 substrate cannot close without a physical Android device: BLOCKED_DEVICE_UNAVAILABLE. The suite is written and the adapter is implemented; the missing input is hardware.
- Fixture DV-KS-ROTATION-READBACK-01 needs an architect decision on which frozen contract is amended.

## Pending decisions

- Who fills PrimaryIndependentAlivenessGateReviewer, AlternateIndependentAlivenessGateReviewer and BaselineIndependentOwner.
- The maximum fundable participants and participant-hours for A001.
- Whether the graded instrument must be cognitively pretested before Attempt 1.
- Whether this study requires external ethical or institutional approval, and in which jurisdiction it runs.
- Which production network recovery provider to select, which requires an owner, credentials and a privacy review.
- Where the identity authority is deployed, and who owns its backup and incident procedures.
- Whether key-epoch rotation re-encrypts history or the wrapping epoch is separated from the record key epoch.
- Whether the repository should remain public now that the licence is proprietary.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
