# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-14T21:40:00-04:00`

## Active state after adoption

- Local directive ID: D-014
- External directive ID: D014
- Objective: Implement and qualify the remaining network-facing recovery and identity-authority substrate — a vendor-neutral S3-compatible recovery provider, a versioned identity-authority transport, and a minimum service operations package — without making ordinary organism operation depend on a cloud service.
- Current status: `COMPLETE`
- Acceptance: A vendor-neutral network recovery provider implemented and conformant; S3 client and transport-framework candidates evaluated with dispositions recorded; a versioned identity-authority transport frozen that preserves every protocol semantic; conformance passing against a real compatible endpoint; race, idempotency, replay and restart qualification passing; end-to-end network cold recovery passing; provider or authority outage unable to strand ordinary local life; no canonical plaintext reaching the provider; canonical bytes and hashes independent of network behaviour; operations artifacts present with no unsupported production claims; prior bundles still valid; CI passes; HEAD matches origin/main; the worktree is clean.
- Current phase: A000 complete and remediated; the A001 activation package is prepared and its gate is correctly shut; the R012 substrate is qualified on the desktop under the corrected V2 cryptography contract; the R012 network substrate is qualified against an in-repository endpoint and against MinIO; the Android adapter is implemented but unqualified on hardware and the identity authority is undeployed. R003 through R009 remain blocked behind A001.
- Expected or actual touched areas: core-recovery-net/, services/s3-qualification-endpoint/, services/identity-authority/, core-recovery/, desktop-runner/, docs/architecture/, docs/operations/, docs/decisions/, docs/invariants/, tools/build_qualification_bundle.py, governance/release-gates/, governance/qualification/, qualification/network/, qualification/evidence/R014/, .github/workflows/ci.yml, settings.gradle.kts, .agent/
- Immediate next action: Hold for architect review of the D-014 completion report; this snapshot is awaiting reset to IDLE once that review lands. Two external inputs are outstanding: a physical Android device reachable to adb for the R012 device gate, and an owner, environment and credentials for a production object store and a deployed identity authority. The A001 inputs are unchanged. Do not begin D015.

## Temporary task-relevant facts

- Five R012 contracts are frozen. LocalStorageCryptographyContractV1 is SUPERSEDED by LocalStorageCryptographyContractV2, which also amends sections 13.3 through 13.5 of ContinuityDurabilityContractV1. The other four are unchanged. Recorded as DEC-0029 and DEC-0033.
- The selected backend is SEGMENTED_APPEND_LOG_V1, a single-writer append-only log with one metadata-inclusive fsync per acknowledged commit. Recorded as DEC-0028.
- The R012 kernel is R012-FIXTURES-V1 version 2 with digest 0da0d889840c0bafe6554735cb9670d27f862870478589518383f0734aece6a5, fifty-five fixtures, all held, checked by CI. Version 1 had forty-two and digest 48bd44a3.
- The corrected substrate bundle is R012-QB-2 with manifest hash 6ee9b2409d4867e587809c9e0ff1ceec95f0d1bfc7d4ced48f5b804aa8da8491 over twenty-five constituents, and its gate record is R012_SUBSTRATE_GATE_V2.md. It is now pinned to commit 43be3c89fa55d050394565e2117bdac4a73d43e6. The value ce35c62e recorded here under D013 was a stale intermediate hash; the committed bundle file and the verifier have always agreed on 6ee9b240.
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
- The network recovery provider is S3_COMPATIBLE_OBJECT_STORE_V1 in core-recovery-net, implementing the unchanged RecoveryPackageStoreContractV1. There is no vendor name and no vendor branch in it; endpoint, region, bucket and key prefix are configuration. Recorded as IMPL-0077 and PA-0002 with disposition BUILD.
- IdentityAuthorityTransportContractV1 is frozen and carries IdentityAuthorityProtocolV1 unchanged. Canonical envelope bodies, four POST endpoints, a one kibibyte ceiling, a required request identifier, and an advisory HTTP status over an authoritative body. Recorded as IMPL-0078 and PA-0003 with disposition BUILD.
- The transport carries no client clock and no free-text detail, and a transport refusal has no body at all. Recorded as IMPL-0079 and L-0045.
- The R014 network kernel is R014-NETWORK-FIXTURES-V1 version 1 with digest efbd6f1caa060da228f72a96cef9e3a2a290c7503f685270e2bbb2b7c7da1501, thirty eight fixtures, all held against the in-repository endpoint, checked by CI. Against MinIO RELEASE.2025-09-07T16-13-09Z thirty three of thirty three held with digest cc9497ff4817abd8a94bcade5f97318424174670a2629100baa1b1aa3d192545; five fixtures need an injected fault MinIO cannot produce. Recorded as IMPL-0081 and L-0043.
- The R014 network bundle is R014-QB-1 with manifest hash e5a50506e421bfce8c71a123710af541e44b1c78c1044e3fed5933110ea195df over thirty-three constituents, and its gate record is R014_NETWORK_GATE.md. R012-QB-2 is now pinned to commit 43be3c89fa55d050394565e2117bdac4a73d43e6 because D014 legitimately edits three of its constituents. Recorded as IMPL-0082.
- MinIO is an independent implementation of AWS Signature Version 4 and authenticated every request, which is the evidence that the project-owned signer is compatible rather than merely self-consistent.
- AndroidApiSurfaceTest reads the network module's compiled classes back and refuses any java or javax package absent from Android API 29, so the reason the AWS SDK was declined cannot quietly stop being true. Recorded as L-0044.
- The identity authority is deployable but not deployed. Its operations package covers configuration, secrets, health and readiness, backup, restore, upgrade, migration, log privacy and an incident runbook, and lists eleven things that are not production-qualified.
- The authority cannot run as more than one instance, because the epoch compare-and-swap is a process-level lock over one file.
- The R012 performance harness could not run on Android at all until its filesystem probe stopped depending on Files.getFileStore, which Android refuses. Recorded as IMPL-0070 and L-0038.
- Killing a process leaves the page cache intact, so power loss is not proven by any test here and is disclosed as unproven. Recorded as L-0035.
- No human outcome data exists anywhere in the repository and no R003 through R009 organism mechanism exists.
- D-001 remains recorded as nonconforming, D-002 through D-011 remain recorded as accepted and complete, and D-012 remains recorded as BLOCKED_DEVICE_UNAVAILABLE.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify covering ten bundles, python3 tools/generate_lookup_tables.py --check, python3 tools/verify_backup_exclusion.py, ./gradlew clean build covering sixteen modules and four hundred and twenty nine JVM tests, ./gradlew :desktop-runner:run, ./gradlew :desktop-runner:r012Qualification, ./gradlew :desktop-runner:r014NetworkQualification against both the in-repository endpoint and MinIO, ./gradlew :research:aliveness-spike:accelerated-sim:run, and ./gradlew :research:aliveness-spike:analysis:a001DryRun
- Result: `PASSED`

## Risks

- No physical Android device is reachable, so Keystore hardware or StrongBox backing, real device flash behaviour, physical-device latency and on-hardware restart are unqualified. The emulator run does not substitute for them.
- Power-loss durability is not proven by any test here. It rests on the force-per-commit policy, which is stated rather than measured.
- The measured latencies are desktop and emulator figures. No production threshold is derived from them and none may be until device evidence exists.
- Data-encryption-key rotation has no design. If one is ever needed, it must be frozen separately and must not be reached by widening wrapping rotation.
- The network recovery provider has never run against a commercial object store and its TLS path is unexercised; both qualification runs are loopback plaintext HTTP. Provider selection for the product still needs an owner, credentials and a privacy review.
- Multipart upload is not implemented. A recovery package above sixty four mebibytes is refused before it is sent rather than split.
- The identity authority has a transport and written operations procedures, and is deployed nowhere. Its backup, restore, upgrade and incident procedures have never been exercised, no availability, redundancy or disaster-recovery claim is made, and verification-key rotation is not designed.
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
- Which production network recovery provider to select, and who owns its credentials and privacy review. D014 built the mechanism; the choice is still open.
- Where the identity authority is deployed, who operates it, and who owns the backup and incident procedures now written for it.
- Whether the recovery provider needs multipart upload, which depends on a package-size distribution that does not exist yet.
- Whether the repository should remain public now that the licence is proprietary.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
