# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-14T11:40:00-04:00`

## Active state after adoption

- Local directive ID: D-011
- External directive ID: D011
- Objective: Advance the bounded R012 persistence, recovery and identity substrate authorized by the parallel-execution amendment while A001 remains blocked on people and money.
- Current status: `COMPLETE`
- Acceptance: The five R012 substrate contracts are frozen before dependent behaviour is treated as qualified; the persistence backend is selected from measured evidence and preserves the frozen R002 durability semantics; local encrypted persistence, rotation and interrupted rewrap are qualified; corruption produces detection or quarantine rather than a silent reset; a concrete recovery-package provider exists and end-to-end cold recovery succeeds; corrupt, stale, duplicate and unauthorized recovery paths are refused; identity epoch and authority semantics pass; copied-state quarantine works; provider failure cannot strand local operation; canonical bytes and hashes are independent of persistence, encryption and recovery; real latency and storage measurements are recorded; prior qualifications remain green; a reproducible bundle exists; CI passes; HEAD matches origin/main; and the worktree is clean.
- Current phase: A000 complete and remediated; the A001 activation package is prepared and its gate is correctly shut; the R012 substrate authorized by the parallel amendment is qualified. R003 through R009 remain blocked behind A001.
- Expected or actual touched areas: core-persistence/, core-recovery/, services/identity-authority/, core-crypto/, desktop-runner/, benchmarks/persistence-bench/, docs/architecture/, docs/decisions/, docs/invariants/, governance/release-gates/, governance/qualification/, qualification/fixtures/R012/, qualification/evidence/R012/, settings.gradle.kts, gradle/libs.versions.toml, tools/, .github/workflows/ci.yml, .agent/
- Immediate next action: Hold for architect review of the D-011 completion report; this snapshot is awaiting reset to IDLE once that review closes. The A001 external inputs are unchanged: three eligible reviewer and baseline-owner names, and the maximum human-study participant and resource budget. An Android device or emulator is now also needed to close the device half of the R012 substrate. Do not begin D012.

## Temporary task-relevant facts

- Five R012 contracts are frozen: PersistenceBackendContractV1, LocalStorageCryptographyContractV1, RecoveryCryptographyContractV1, IdentityAuthorityProtocolV1 and RecoveryPackageStoreContractV1. Recorded as DEC-0029.
- The selected backend is SEGMENTED_APPEND_LOG_V1, a single-writer append-only log with one metadata-inclusive fsync per acknowledged commit, chosen over direct SQLite in two modes, a synchronous random-access file and a whole-file rewrite. Recorded as DEC-0028.
- SQLite in write-ahead-logging mode cost 5.3 times the p99 commit latency and 4.4 times the storage of the selected backend on ext4 over NVMe.
- The R012 kernel is R012-FIXTURES-V1 version 1 with digest 48bd44a31a3a952cf884b358c5e587b93a24875f1d36d7625e6b4b7d5f62127f, forty-two fixtures, all held, checked by CI.
- The fault matrix uses real child JVMs killed with Runtime.halt. Process death after commit, torn frames, interrupted compaction and snapshot writes, corruption, full storage, refused writes and eight consecutive restarts are all covered.
- Killing a process leaves the page cache intact, so power loss is not proven by any test here and is disclosed as unproven. Recorded as L-0035.
- The worst defect found and fixed was a corrupt first frame being reported as an empty journal, which would have presented a corrupt installation as a device with no organism on it. Recorded as L-0036 and INV-0048.
- The first backend benchmark landed on tmpfs and reported two-microsecond fsyncs; both harnesses now refuse to run there. Recorded as L-0034.
- Cold recovery works end to end from package plus secret alone, restoring twenty tail records under a new identity epoch on a destination device with new local key material, and declaring a recovery gap rather than inventing one.
- The identity authority is separately deployable and structurally absent from the organism core classpath, asserted by ModuleBoundaryTest rather than by convention. Recorded as DEC-0030.
- No Android device or emulator was reachable, so the Android Keystore container and device-level backend qualification are BLOCKED_DEVICE_UNAVAILABLE.
- The R012 substrate bundle is R012-QB-1 with manifest hash 3461b5c3cbec01a0acaa47e4b6e840a6ba7dcfb9eca65d35c161f61279583d2d over forty constituents.
- No human outcome data exists anywhere in the repository and no R003 through R009 organism mechanism exists.
- D-001 remains recorded as nonconforming, and D-002 through D-010 remain recorded as accepted and complete.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify, python3 tools/generate_lookup_tables.py --check, ./gradlew clean build, ./gradlew :desktop-runner:run, ./gradlew :desktop-runner:r012Qualification, ./gradlew :desktop-runner:r012Performance, ./gradlew :benchmarks:persistence-bench:run, ./gradlew :research:aliveness-spike:accelerated-sim:run, and ./gradlew :research:aliveness-spike:analysis:a001DryRun
- Result: `PASSED`

## Risks

- No Android device or emulator was reachable under D011, so the Android Keystore key container and device-level backend, corruption and full-storage qualification remain outstanding.
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
- The device half of the R012 substrate cannot close without an Android device or emulator: BLOCKED_DEVICE_UNAVAILABLE.

## Pending decisions

- Who fills PrimaryIndependentAlivenessGateReviewer, AlternateIndependentAlivenessGateReviewer and BaselineIndependentOwner.
- The maximum fundable participants and participant-hours for A001.
- Whether the graded instrument must be cognitively pretested before Attempt 1.
- Whether this study requires external ethical or institutional approval, and in which jurisdiction it runs.
- Which production network recovery provider to select, which requires an owner, credentials and a privacy review.
- Where the identity authority is deployed, and who owns its backup and incident procedures.
- Whether the repository should remain public now that the licence is proprietary.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
