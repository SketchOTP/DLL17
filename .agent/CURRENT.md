# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-14T20:05:00-04:00`

## Active state after adoption

- Local directive ID: D-013
- External directive ID: D013
- Objective: Implement the 2026-08-14 key-epoch separation amendment as a versioned successor contract, so an ordinary wrapping-key rotation no longer makes the organism's existing journal unreadable.
- Current status: `COMPLETE`
- Acceptance: Versioned epoch semantics frozen and internally consistent; the wrapping epoch and the data-key and record context no longer conflated; ordinary rotation preserves the same data key; history survives one and many rotations; mixed old and new records readable after restart; no full-history rewrite; interrupted rewrap crash-safe; V1 migration deterministic, idempotent and crash-safe; DV-KS-ROTATION-READBACK-01 passes; canonical bytes and hashes unchanged; unaffected pinned qualifications still valid; prior art recorded as REFERENCE; CI passes; HEAD matches origin/main; the worktree is clean.
- Current phase: A000 complete and remediated; the A001 activation package is prepared and its gate is correctly shut; the R012 substrate is qualified on the desktop under the corrected V2 cryptography contract, and its Android adapter is implemented but unqualified on hardware. R003 through R009 remain blocked behind A001.
- Expected or actual touched areas: docs/architecture/, core-continuity/, core-persistence/, android-host/src/androidTest/, tools/build_qualification_bundle.py, docs/decisions/, docs/invariants/, governance/release-gates/, governance/qualification/, qualification/, .agent/
- Immediate next action: Hold for architect review of the D-013 completion report; this snapshot is awaiting reset to IDLE once that review lands. One external input is outstanding for the R012 device gate: a physical Android device reachable to adb. The A001 inputs are unchanged. Do not begin D014.

## Temporary task-relevant facts

- Five R012 contracts are frozen. LocalStorageCryptographyContractV1 is SUPERSEDED by LocalStorageCryptographyContractV2, which also amends sections 13.3 through 13.5 of ContinuityDurabilityContractV1. The other four are unchanged. Recorded as DEC-0029 and DEC-0033.
- The selected backend is SEGMENTED_APPEND_LOG_V1, a single-writer append-only log with one metadata-inclusive fsync per acknowledged commit. Recorded as DEC-0028.
- The R012 kernel is R012-FIXTURES-V1 version 2 with digest 0da0d889840c0bafe6554735cb9670d27f862870478589518383f0734aece6a5, fifty-five fixtures, all held, checked by CI. Version 1 had forty-two and digest 48bd44a3.
- The corrected substrate bundle is R012-QB-2 with manifest hash ce35c62e7cd8be371cb5907953d132a38d472335a3d25bca1d5c8885fdf7a8f8 over twenty-five constituents, and its gate record is R012_SUBSTRATE_GATE_V2.md.
- The R012 substrate bundle is R012-QB-1 with manifest hash 644ee8e99d6192aee59aab7497c3532f90a06d61bc20eae5871216147bccf9f7 over forty constituents, now pinned to commit afd0ecdb21bd20a00d4f3b6ae69d31e61890707c. Recorded as IMPL-0071.
- The Android device bundle is R012DEV-QB-2 with manifest hash 0a6ce70a3df04c18f1c94c4b7925dc34184ae18f9fbb90b3ea5e2c0b003a2ab8 over twenty constituents. R012DEV-QB-1, manifest hash 851005132d78a9d92fd653d2081f1f17ca9304b494dfb0d2445efd9fdb44274b, is pinned to commit 4700b0762cad3b1bb63a69be4f7eca9caea3b819 so D012's negative evidence stays verifiable. Recorded as DEC-0034 and IMPL-0076.
- The device kernel is R012-DEVICE-FIXTURES-V1 version 2 with digest bac5989e7c7cbd48f8174e7736ca462e4451424d7161384fa2d5d487209f9d74, forty-six fixtures, all held on an x86 emulator. Version 1 had forty-five with forty-four held.
- No physical Android device is reachable. The emulator run is supplementary API-path coverage and is filed as such, not as device evidence. Recorded as DEC-0031.
- The emulator reported SOFTWARE key backing, so hardware-backed and StrongBox Keystore behaviour is unqualified.
- The Android key container is an AndroidKeyStore HMAC-SHA256 key, because an AES key cannot supply a root secret that is stable across restarts. Recorded as IMPL-0066 and L-0039.
- Opening the container never generates missing material, and a birth requires both the key state and the container material to be absent. Recorded as IMPL-0067 and INV-0055.
- Backup and device-transfer exclusion is verified from the built debug and release packages by following the merged manifest's own resource references, and is now a CI step. Recorded as IMPL-0068 and INV-0056.
- Fixture DV-KS-ROTATION-READBACK-01 now holds with readableAfterRotation five of five. The contradiction it recorded was resolved by the architect in favour of epoch separation and implemented as LocalStorageCryptographyContractV2. Recorded as DEC-0033.
- A record is decrypted under its own immutable stored context, never under the container's current wrapping epoch. The epoch pre-check was removed and the guarantee left to the AEAD tag, which refuses four forged header fields and a foreign data key. Recorded as IMPL-0073, INV-0058 and L-0042.
- Key state migrates from schema 231 version 1 to version 2 by adding a data-key identity to one small file. No journal record is rewritten, because the initial wrapping epoch and the initial data-key identity are both 1. Recorded as IMPL-0074, INV-0059 and L-0041.
- Data-encryption-key rotation is not implemented. dataKeyId exists so the layout need not change again, and a wrapping rotation must never be turned into a DEK rotation. Recorded as IMPL-0075.
- The external prior-art check on envelope encryption is recorded as PA-0001 with disposition REFERENCE. No dependency was adopted and gradle/libs.versions.toml is unchanged.
- The R012 performance harness could not run on Android at all until its filesystem probe stopped depending on Files.getFileStore, which Android refuses. Recorded as IMPL-0070 and L-0038.
- Killing a process leaves the page cache intact, so power loss is not proven by any test here and is disclosed as unproven. Recorded as L-0035.
- No human outcome data exists anywhere in the repository and no R003 through R009 organism mechanism exists.
- D-001 remains recorded as nonconforming, D-002 through D-011 remain recorded as accepted and complete, and D-012 remains recorded as BLOCKED_DEVICE_UNAVAILABLE.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify, python3 tools/generate_lookup_tables.py --check, python3 tools/verify_backup_exclusion.py, ./gradlew clean build, ./gradlew :desktop-runner:run, ./gradlew :desktop-runner:r012Qualification, ./gradlew :desktop-runner:r012Performance, ./gradlew :research:aliveness-spike:accelerated-sim:run, ./gradlew :research:aliveness-spike:analysis:a001DryRun, and ./gradlew :android-host:connectedDebugAndroidTest on an x86 emulator
- Result: `PASSED`

## Risks

- No physical Android device is reachable, so Keystore hardware or StrongBox backing, real device flash behaviour, physical-device latency and on-hardware restart are unqualified. The emulator run does not substitute for them.
- Power-loss durability is not proven by any test here. It rests on the force-per-commit policy, which is stated rather than measured.
- The measured latencies are desktop and emulator figures. No production threshold is derived from them and none may be until device evidence exists.
- Data-encryption-key rotation has no design. If one is ever needed, it must be frozen separately and must not be reached by widening wrapping rotation.
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
