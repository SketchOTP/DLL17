# Project Decision and Milestone Record

Use this append-only record for major durable project events and decisions, not routine task outcomes.

## Entry guidance after adoption

Use it for architectural decisions, governance changes, releases, qualification or certification events, major reversals, important milestones, and decision supersessions.

Each live entry should include:

- Date.
- Record or decision ID.
- Status.
- Decision or event.
- Rationale.
- Affected areas.
- Supersession relationship when applicable.

Allowed status values are `PROPOSED`, `ACTIVE`, `SUPERSEDED`, `REVERSED`, and `CLOSED`.

Do not add live decisions or milestones to this template. Examples must remain outside the shipped template state.

## DEC-0001

- Date: 2026-08-12
- Record or decision ID: DEC-0001
- Status: ACTIVE
- Decision or event: The Digital Living Lifeform project charter was adopted into .agent/PROJECT_GOAL.md as the local statement of goal, success measures, scope, non-goals, constraints, governing specifications and approval authority.
- Rationale: The repository needed a local, machine-checkable statement of the charter so that later phases can be reconciled against it without depending on external pages being reachable.
- Affected areas: .agent/PROJECT_GOAL.md
- Supersedes record: none

## DEC-0002

- Date: 2026-08-12
- Record or decision ID: DEC-0002
- Status: ACTIVE
- Decision or event: D-001 is permanently recorded as nonconforming because a repository change was made under a directive that prohibited repository changes, while the resulting adopted goal file is retained deliberately rather than reverted.
- Rationale: Governance history must stay truthful. Reverting the file would destroy a correct artifact, and rewriting the event as a clean execution would falsify the ledger, so the deviation is disclosed and preserved instead.
- Affected areas: .agent/DIRECTIVES.md, .agent/OUTCOMES.md, .agent/PROJECT_GOAL.md
- Supersedes record: none

## DEC-0003

- Date: 2026-08-12
- Record or decision ID: DEC-0003
- Status: ACTIVE
- Decision or event: The R000 governance baseline was adopted repository-wide, moving every governance file required by the repository validator out of its shipped unadopted state into a truthful adopted state.
- Rationale: D-002 requires the repository to represent the real project state and to satisfy the existing validator in adopted mode before any R000 implementation work is authorized.
- Affected areas: .agent/PROJECT_PROFILE.md, .agent/CURRENT.md, .agent/DIRECTIVES.md, .agent/OUTCOMES.md, .agent/LEARNINGS.md, .agent/RECORD.md, .agent/REPO_MAP.md
- Supersedes record: none

## DEC-0004

- Date: 2026-08-12
- Record or decision ID: DEC-0004
- Status: ACTIVE
- Decision or event: Git provenance for this repository begins at D-003. The repository was initialized locally on branch main and the baseline commit f82e1b2f7c138a7c4238f109b45a6562b8b18a21 records the accepted post-D002 governance state plus the D-003 governance-tooling correction. No remote is configured, and no earlier version-control history exists or is implied.
- Rationale: Directive evidence requires durable commit identifiers, which were unavailable while the repository was untracked. Initializing at D-003 keeps the provenance claim truthful rather than backdating history that never existed.
- Affected areas: repository root, .gitignore
- Supersedes record: none

## DEC-0005

- Date: 2026-08-12
- Record or decision ID: DEC-0005
- Status: ACTIVE
- Decision or event: The governance validator self-test now builds every mutable fixture state from the pristine copy under scripts/fixtures/governance_template/ rather than assuming the live governance directory is unadopted. The live directory is exercised only as an adopted-mode positive case and as a template-mode rejection case.
- Rationale: Adoption of the live governance files is a legitimate permanent condition, so the self-test had to stop treating live state as a pristine fixture. Isolating fixtures fixes the conflict without weakening the production validator.
- Affected areas: scripts/test_validate_governance.py, scripts/fixtures/governance_template/
- Supersedes record: none

## DEC-0006

- Date: 2026-08-12
- Record or decision ID: DEC-0006
- Status: ACTIVE
- Decision or event: ProjectIdentityBuildContractV1 was frozen from the architect-supplied values in D004 and recorded at docs/architecture/ProjectIdentityBuildContractV1.md. The contract fixes the project identifier DLL17, the product display name Digital Living Lifeform, the Android application identifier and namespace com.animusmachinae.dll17, Kotlin 2.4.10, JDK 17, Android Gradle Plugin 9.3.1, Gradle 9.5.0, compileSdk and targetSdk 37, minSdk 29, Compose BOM 2026.06.00, versionCode 1, versionName 0.1.0-dev, the debug and release variants, the absence of product flavors, deferred production signing, prohibited dynamic dependency versions, and a proprietary source licence.
- Rationale: Every value came from the architect. Freezing them in one reviewed document and enforcing them with tools/verify_project_identity.py prevents silent build drift and keeps the implementer from selecting any number.
- Affected areas: docs/architecture/ProjectIdentityBuildContractV1.md, gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties, settings.gradle.kts, android-host/build.gradle.kts, tools/verify_project_identity.py
- Supersedes record: none

## DEC-0007

- Date: 2026-08-12
- Record or decision ID: DEC-0007
- Status: ACTIVE
- Decision or event: The R000 module topology from Implementation Plan E2E was created as a Gradle multi-module project. core-math, core-crypto and core-state are pure Kotlin JVM modules, desktop-runner is a pure Kotlin JVM headless runner, and android-host is the only module that links the Android framework. The qualification, docs, governance and tools directory trees were created alongside them.
- Rationale: The runtime boundary between canonical organism code and the Android host is an architectural invariant, so it was made a build fact and a test assertion in R000 rather than a convention that later phases could erode.
- Affected areas: settings.gradle.kts, core-math, core-crypto, core-state, desktop-runner, android-host, qualification, docs, governance, tools
- Supersedes record: none

## DEC-0008

- Date: 2026-08-12
- Record or decision ID: DEC-0008
- Status: ACTIVE
- Decision or event: The authoritative remote for this repository is git@github.com:SketchOTP/DLL17.git, set as origin under D-004, with local branch main tracking origin/main. The remote hosting question that D-003 left unresolved is therefore closed.
- Rationale: The architect named the authoritative repository in D004, which supplied the proof of ownership that D-003 required before a remote could be established.
- Affected areas: repository remote configuration, .agent/PROJECT_PROFILE.md, .agent/CURRENT.md
- Supersedes record: none

## DEC-0009

- Date: 2026-08-12
- Record or decision ID: DEC-0009
- Status: ACTIVE
- Decision or event: The repository source licence is proprietary with all rights reserved. The inherited MIT LICENSE was removed and replaced under architect directive D005. Repository visibility was not changed, because D005 explicitly excluded that.
- Rationale: ProjectIdentityBuildContractV1 froze a proprietary no-redistribution policy, and D005 resolved the recorded conflict in favour of the frozen contract rather than amending it.
- Affected areas: LICENSE, docs/architecture/ProjectIdentityBuildContractV1.md, governance/source-provenance/DEPENDENCY_LICENSE_INVENTORY.md, docs/decisions/DECISION_LOG.md
- Supersedes record: none

## DEC-0010

- Date: 2026-08-12
- Record or decision ID: DEC-0010
- Status: ACTIVE
- Decision or event: The R000 exit gate is closed as PASS. Both the Implementation Plan E2E gate and the Digital Living Lifeform charter gate were evaluated criterion by criterion and every criterion passed. The Android shell was qualified on physical Tensor hardware rather than an emulator.
- Rationale: D005 required actual launch evidence and a reproducible hashed qualification bundle before R000 could close. Both exist, and the charter wording requiring target hardware was satisfied rather than only the weaker device-or-emulator wording.
- Affected areas: governance/release-gates/R000_EXIT_GATE.md, governance/qualification/R000_QUALIFICATION_BUNDLE.md, governance/qualification/QUALIFICATION_EVIDENCE_INDEX.md
- Supersedes record: none

## DEC-0011

- Date: 2026-08-12
- Record or decision ID: DEC-0011
- Status: ACTIVE
- Decision or event: Future production resource budgets remain NOT ESTABLISHED. Only measured observations of the empty R000 shell were recorded, under the identifier R000_MEASURED_BASELINE.
- Rationale: D005 prohibited inventing future target values. A guessed ceiling recorded now would be treated as a contract by later phases and would have to be unwound. Device evidence, not guessed constants, freezes production limits.
- Affected areas: docs/release/DEVICE_AND_RESOURCE_BUDGETS.md, governance/release-gates/R000_EXIT_GATE.md
- Supersedes record: none


## DEC-0012

- Date: 2026-08-13
- Record or decision ID: DEC-0012
- Status: ACTIVE
- Decision or event: DeterminismContractV1 version 1 was frozen under D-006 before any dependent implementation was written. It fixes canonical byte order, integer widths and fixed-width length prefixes, boolean and enum encoding, collection ordering, canonical text policy, the state-hash algorithm and digest size, the PRNG algorithm and serialized state layout, the substream seed mixer, the fixed-point scale and rounding mode, lookup-table generation and verification, and the algorithm and version migration policy.
- Rationale: Implementation Plan E2E work package R001.0 blocks R001 implementation until this artifact exists, so that a determinism-relevant algorithm cannot be selected silently inside implementation code. Changing any clause now requires a new contract version rather than an edit.
- Affected areas: docs/architecture/DeterminismContractV1.md, docs/architecture/CANONICAL_SOURCES.md, core-crypto/, core-math/, core-state/
- Supersedes record: none

## DEC-0013

- Date: 2026-08-13
- Record or decision ID: DEC-0013
- Status: ACTIVE
- Decision or event: Identical fixtures produced the identical canonical evidence digest 54bc044740a4c05b41b509a7160bff559e09421f2eaa55dc36c3d3ffadc1bd86 on the desktop JVM reference runner, the x86_64 Android emulator and Tensor G4 hardware, closing the R001 exit gate.
- Rationale: The qualified target set spans HotSpot and ART, and x86_64 and arm64-v8a, at the same time. Those are the two boundaries most likely to expose a floating-point dependency, an endianness assumption, an API-level-dependent intrinsic or a hash-iteration-order leak. This is the first technical evidence the program requires before expanding into lifecycle and later organism systems.
- Affected areas: governance/release-gates/R001_EXIT_GATE.md, qualification/device-matrix/R001/, qualification/fixtures/R001/, qualification/replay/R001/
- Supersedes record: none

## DEC-0014

- Date: 2026-08-13
- Record or decision ID: DEC-0014
- Status: ACTIVE
- Decision or event: The x86 Android emulator target that R000 recorded as blocked was recovered by switching from the android-37.0 google_apis image to system-images android-36 aosp_atd x86_64.
- Rationale: The ATD image boots headless in under a minute on this host and does not exhibit the surfaceflinger SIGSEGV that made the google_apis image unusable under all three rendering backends. The canonical determinism matrix requires the x86 Android emulator for R001 cross-architecture proof, so the blocked target carried forward from R000 had to be resolved rather than waived.
- Affected areas: qualification/device-matrix/R001/, tools/qualify_r001_determinism.sh, .agent/PROJECT_PROFILE.md
- Supersedes record: none

## DEC-0015

- Date: 2026-08-13
- Record or decision ID: DEC-0015
- Status: ACTIVE
- Decision or event: The Snapdragon row of the canonical determinism matrix was waived by the architect during D-006 execution and is recorded as WAIVED BY ARCHITECT, deliberately not as PASS. No claim is made that Snapdragon silicon executed anything.
- Rationale: The canonical matrix lists Snapdragon without the when-available qualifier that Exynos carries, and D006 instructed that an unqualifiable target must not weaken the matrix. The architect was asked directly and answered that the Pixel 9 hardware is sufficient. The architect outranks the directive text, but a waiver given in a working session is not an amendment to a frozen specification, so the canonical page and the matrix disagree until the page is amended.
- Affected areas: qualification/device-matrix/R001/DETERMINISM_MATRIX.md, governance/release-gates/R001_EXIT_GATE.md
- Supersedes record: none

## DEC-0016

- Date: 2026-08-13
- Record or decision ID: DEC-0016
- Status: ACTIVE
- Decision or event: The qualification bundle tool now pins a closed phase to its qualified commit and reads that commit's blobs through git, instead of verifying constituents against the working tree. R000 is pinned to commit 43054d0a2a210bc48563cc81016d6083bff2a182, and its manifest hash is unchanged at 501880933649bc80c618141fb064ba6d80ee1e7a38df4d25cc59c80e4411aa13.
- Rationale: A defect in the R000 tooling, found during D-006. R001 legitimately edited shared build files and every registry, which would have failed R000 verification and reopened a gate that had already closed for reasons unrelated to R000. Evidence of a past qualification must not depend on the present state of the repository.
- Affected areas: tools/build_qualification_bundle.py, governance/qualification/, .github/workflows/ci.yml
- Supersedes record: none

## DEC-0017

- Date: 2026-08-13
- Record or decision ID: DEC-0017
- Status: ACTIVE
- Decision or event: The 2026-08-13 architect amendment to the canonical architecture removed Snapdragon as a required R001 determinism target. The required matrix is Tensor Android hardware, the x86 Android emulator and the desktop JVM reference runner, with Exynos conditional when available and Snapdragon optional confidence evidence only.
- Rationale: The amendment closes the disagreement DEC-0015 recorded between the frozen page and the qualified matrix. R001's determinism-matrix criterion now rests on evidence plus a canonical amendment rather than on a session waiver, and the qualified evidence itself is unchanged.
- Affected areas: governance/release-gates/R001_EXIT_GATE.md, qualification/device-matrix/R001/DETERMINISM_MATRIX.md, governance/qualification/QUALIFICATION_EVIDENCE_INDEX.md
- Supersedes record: DEC-0015

## DEC-0018

- Date: 2026-08-13
- Record or decision ID: DEC-0018
- Status: ACTIVE
- Decision or event: ContinuityDurabilityContractV1 version 1 is FROZEN and R002 continuity lives in a new pure Kotlin module, core-continuity, rather than inside core-state.
- Rationale: R002 adds a large cohesive subsystem covering clocks, trust, reconciliation, durability, platform protection and encryption. Folding it into the closed R001 determinism kernel would have made that surface harder to reason about, and a separate module keeps the Android framework out of continuity logic by construction.
- Affected areas: docs/architecture/ContinuityDurabilityContractV1.md, core-continuity/, settings.gradle.kts, tools/verify_project_identity.py
- Supersedes record: none

## DEC-0019

- Date: 2026-08-13
- Record or decision ID: DEC-0019
- Status: ACTIVE
- Decision or event: R002.5 prepared-rest semantics and R002.10 prepared-rest durable handoff are deferred to the phase that owns organism behaviour, and R002.12 recovery cryptography and the storage provider are recorded as BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY and BLOCKED_SPEC_RECOVERY_PROVIDER.
- Rationale: The rest packages are written entirely in terms of exhaustion, recovery curves and contradiction conditions, which are species physiology gated behind A001. Implementation Plan E2E R002.12 requires RecoveryCryptographyContractV1 to be frozen before recovery cryptography is written and forbids inventing those choices inside persistence code, and D007 authorized freezing ContinuityDurabilityContractV1 only. The durability machinery both packages depend on is implemented and qualified.
- Affected areas: governance/release-gates/R002_EXIT_GATE.md, core-continuity/IdentityBinding.kt
- Supersedes record: none

## DEC-0020

- Date: 2026-08-13
- Record or decision ID: DEC-0020
- Status: ACTIVE
- Decision or event: The A000 curiosity balance envelope search returned EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE across all twenty-seven grid points and four seeds, and no threshold was altered.
- Rationale: CuriosityEnvelopeFeasibilityV1 permits exactly one threshold-only revision ever, and reserves the choice between the threshold path and the mechanism path to the independent gate reviewer. Spending that single revision unilaterally, before a reviewer exists, would be the most expensive shortcut available on this track. The failure is uniform and specific: attribution passed at every point with wide margin while anti-convergence failed at every point on exactly two of its five criteria, which is the shape of a threshold question rather than a mechanism collapse, but that reading is the reviewer's to confirm.
- Affected areas: research/aliveness-spike/study-protocol/CuriosityBalanceEnvelopeV1.md, research/aliveness-spike/evidence/CURIOSITY_ENVELOPE_SEARCH.txt, governance/release-gates/A000_EXIT_GATE.md
- Supersedes record: none

## DEC-0021

- Date: 2026-08-13
- Record or decision ID: DEC-0021
- Status: ACTIVE
- Decision or event: The A000 preference, habit and relationship update laws were rewritten from accumulators to error-corrected estimates during the track, and the accumulator form is recorded as a mechanism-design finding rather than as a bug.
- Rationale: The accumulator form pins any object with a net positive outcome at its bound, which makes a reliable food source and an unreliable one indistinguishable and makes a contingency reversal unobservable. That is a property of the learning law, not of the parameters, and it is worth recording because the same law was the obvious first choice.
- Affected areas: research/aliveness-spike/cohorts/Mechanisms.kt, qualification/longitudinal/A000/ACCELERATED_FINDINGS.md, docs/decisions/DECISION_LOG.md
- Supersedes record: none

## DEC-0022

- Date: 2026-08-13
- Record or decision ID: DEC-0022
- Status: ACTIVE
- Decision or event: R002's qualification bundle is pinned to its qualified commit 7f6f37fabba6a5ad4af2fd517e62cb4c08dbfeb2.
- Rationale: A000 adds Gradle modules and therefore edits settings.gradle.kts, which is an R002 bundle constituent. This is the same defect IMPL-0014 fixed for R000: evidence of a closed qualification must not depend on the present state of the tree.
- Affected areas: tools/build_qualification_bundle.py, governance/qualification/R002_QUALIFICATION_BUNDLE.md
- Supersedes record: none

## DEC-0023

- Date: 2026-08-13
- Record or decision ID: DEC-0023
- Status: ACTIVE
- Decision or event: The architect chose the mechanism path over the threshold path for the empty A000 curiosity feasible region, and the thresholds were not altered.
- Rationale: CuriosityEnvelopeFeasibilityV1 permits one threshold-only revision for the life of the programme. Remediating the organism under the existing thresholds returned twenty-seven of twenty-seven feasible grid points on the identical grid and seed matrix that returned zero, which settles the question the empty result posed and leaves the revision unspent.
- Affected areas: research/aliveness-spike/study-protocol/CuriosityBalanceEnvelopeV1.md, research/aliveness-spike/cohorts/, governance/release-gates/A000_EXIT_GATE.md
- Supersedes record: none

## DEC-0024

- Date: 2026-08-13
- Record or decision ID: DEC-0024
- Status: ACTIVE
- Decision or event: Episodic history influence is removed from the A000 FULL candidate, and the human leave-one-out ablation family is reduced from three arms to two.
- Rationale: A revised context-conditioned salience-retained form, measured across a five-seed matrix in a habitat given circadian structure specifically so the mechanism had a conjunction to learn, still did not increase history-dependent individuality. The canonical multiplicity plan corrects across the comparisons actually tested, so Holm-Bonferroni now runs over two. Adding a replacement third arm requires a new preregistered plan and is not the implementer's decision.
- Affected areas: research/aliveness-spike/cohorts/DecisionTrace.kt, research/aliveness-spike/cohorts/Organism.kt, research/aliveness-spike/study-protocol/, governance/release-gates/A000_EXIT_GATE.md
- Supersedes record: none

## DEC-0025

- Date: 2026-08-13
- Record or decision ID: DEC-0025
- Status: ACTIVE
- Decision or event: The architect preregistered FULL minus outcome uncertainty and directed re-exploration as the replacement third human ablation arm, restoring the Attempt-1 family to three arms under Holm-Bonferroni at family-wise error rate 0.05.
- Rationale: The episodic arm was retired under DEC-0024 when the mechanism left FULL. Directed re-exploration became load-bearing for the aliveness thesis under D009, because it is what lets an organism return to a rejected option after outcome evidence changes rather than by chance, so it is the mechanism whose human contribution most needs testing. The choice was the architect's and was made before any human data existed.
- Affected areas: research/aliveness-spike/cohorts/DecisionTrace.kt, research/aliveness-spike/analysis/, research/aliveness-spike/study-protocol/, qualification/fixtures/A000/
- Supersedes record: none

## DEC-0026

- Date: 2026-08-13
- Record or decision ID: DEC-0026
- Status: ACTIVE
- Decision or event: The programme-level aliveness success floor is frozen at a mean paired difference of at least ten points on the zero-to-one-hundred graded instrument with a two-sided ninety-five percent confidence interval lower bound above zero, at alpha 0.05 and power 0.80, with a thirty-six participant blind variance pilot and a forty participant baseline qualification carrying a fifteen-point competence margin.
- Rationale: The floor clears BLOCKED_SPEC_ALIVENESS_SUCCESS_FLOOR, which had been open since D008. It was set by the architect before any human data existed, which is the only point at which setting it is honest. It is two conditions rather than one because a p-value alone would let a trivial well-powered difference authorize R003 through R009, and a point estimate alone would let an unresolved sample do the same. The floor may become stricter or stay equivalent after Attempt 1 begins and may never become easier.
- Affected areas: research/aliveness-spike/study-protocol/, research/aliveness-spike/analysis/, governance/release-gates/A001_ACTIVATION_GATE.md
- Supersedes record: none

## DEC-0027

- Date: 2026-08-13
- Record or decision ID: DEC-0027
- Status: ACTIVE
- Decision or event: Figures quoted in the D009 completion report and in several D009 prose documents were reconciled against the frozen kernel evidence, and the A000 fixture set moved to version three with a new golden digest.
- Rationale: The D009 narrative carried decimals from an intermediate kernel run rather than the final frozen one. Every finding, direction and verdict was and remains correct, and the committed evidence file was authoritative throughout, but the prose disagreed with it. The digest itself moved for a separate and disclosed reason, namely that D010 added the preregistered third ablation cohort and the cohort-parity fixture covers every cohort. No organism behaviour changed and all twenty-four findings still hold.
- Affected areas: qualification/fixtures/A000/, qualification/longitudinal/A000/ACCELERATED_FINDINGS.md, governance/release-gates/A000_EXIT_GATE.md, research/aliveness-spike/study-protocol/SpontaneousActionAttributionV1.md
- Supersedes record: none

## DEC-0028

- Date: 2026-08-14
- Record or decision ID: DEC-0028
- Status: ACTIVE
- Decision or event: The production persistence backend is a single-writer append-only log with one metadata-inclusive fsync per acknowledged commit, selected over direct SQLite in two durability modes, a synchronous random-access file and a whole-file rewrite design.
- Rationale: Selected from measurement rather than convention, as the canonical plan requires. On ext4 over NVMe, SQLite in write-ahead-logging mode cost 5.3 times the p99 commit latency and 4.4 times the storage for a workload that never issues a query, never updates a record in place, and reads only by replaying the whole surviving history. A general-purpose engine would also place its own crash-recovery machinery underneath ours, so a durability claim would depend on two engines agreeing rather than on one fsync returning.
- Affected areas: docs/architecture/PersistenceBackendContractV1.md, core-persistence/, benchmarks/persistence-bench/, qualification/evidence/R012/
- Supersedes record: none

## DEC-0029

- Date: 2026-08-14
- Record or decision ID: DEC-0029
- Status: ACTIVE
- Decision or event: The five R012 substrate contracts are frozen: PersistenceBackendContractV1, LocalStorageCryptographyContractV1, RecoveryCryptographyContractV1, IdentityAuthorityProtocolV1 and RecoveryPackageStoreContractV1. BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY and BLOCKED_SPEC_RECOVERY_PROVIDER are cleared for the substrate, and a filesystem-backed object store is the qualifying provider.
- Rationale: The 2026-08-14 parallel-execution amendment authorizes exactly this substrate while A001 is blocked on people and money. Each contract was frozen before its dependent implementation was treated as qualified, and each records what remains blocked rather than inventing it. The recovery root is 256 bits with HKDF rather than a passphrase with a memory-hard key-derivation function, because entropy does the work the function would otherwise have to.
- Affected areas: docs/architecture/, core-persistence/, core-recovery/, services/identity-authority/, governance/release-gates/R012_SUBSTRATE_GATE.md
- Supersedes record: none

## DEC-0030

- Date: 2026-08-14
- Record or decision ID: DEC-0030
- Status: ACTIVE
- Decision or event: The identity authority delivers supported singularity and is documented as unable to deliver absolute singularity against an indefinitely offline clone. The service is separately deployable and is structurally absent from the organism core dependency graph.
- Rationale: A recovered organism supersedes its predecessor and the predecessor is rejected on next contact, but the authority never reaches out, so a device that never calls is never told. That asymmetry is a property of the physics rather than a gap in the implementation, and the product must claim the first and never the second. Separate deployability is enforced by a test asserting the service class is absent from the core classpath, not by convention.
- Affected areas: docs/architecture/IdentityAuthorityProtocolV1.md, services/identity-authority/, core-persistence/src/test/
- Supersedes record: none


## DEC-0031

- Date: 2026-08-14
- Record or decision ID: DEC-0031
- Status: ACTIVE
- Decision or event: D012 closes as BLOCKED_DEVICE_UNAVAILABLE rather than PASS. The Android adapter and the forty-five fixture device qualification suite are complete and ran on an x86 emulator, and that run is filed as supplementary API-path coverage rather than as device evidence.
- Rationale: The directive states that an emulator does not substitute for physical-device evidence where the requirement concerns Keystore behaviour, real app-private device storage, physical-device latency, or restart and recovery on actual hardware, and that desktop results may not be substituted either. All four requirements are open, so the honest close is BLOCKED. Filing the emulator row as a pass would put software-backed key material and emulated storage latency into the record as hardware evidence, which is exactly the substitution the directive forbids.
- Affected areas: governance/release-gates/R012_DEVICE_GATE.md, qualification/device-matrix/R012/, governance/qualification/QUALIFICATION_EVIDENCE_INDEX.md
- Supersedes record: none

## DEC-0032

- Date: 2026-08-14
- Record or decision ID: DEC-0032
- Status: ACTIVE
- Decision or event: A contradiction between two frozen contracts was found and is being escalated rather than fixed. LocalStorageCryptographyContractV1 promises that a wrapping-epoch rotation leaves already written records readable; the frozen EncryptedRecordStore refuses any record whose header key epoch differs from the container's current epoch and derives its nonce and associated data from that epoch, so after a rotation the existing journal does not open. Fixture DV-KS-ROTATION-READBACK-01 records the observed behaviour, reports NOT HELD, and is named in a declared PENDING_ARCHITECT_REVIEW set.
- Rationale: D012 forbids changing a frozen D011 contract to make a device test pass and states that a material conflict requires a versioned amendment and architect review. Either contract could be amended and the choice has real consequences — re-encrypting history versus separating the wrapping epoch from the record key epoch — so it is not a coder's decision. Declaring the failing fixture keeps the suite honest without leaving an undeclared red test in the repository.
- Affected areas: android-host/src/androidTest/, governance/release-gates/R012_DEVICE_GATE.md, qualification/device-matrix/R012/DEVICE_MATRIX.md, docs/architecture/LocalStorageCryptographyContractV1.md
- Supersedes record: none

## DEC-0033

- Date: 2026-08-14
- Record or decision ID: DEC-0033
- Status: ACTIVE
- Decision or event: The architect resolved the DEC-0032 contradiction in favour of separating the wrapping epoch from the data key's identity rather than re-encrypting history, and D013 implemented that as LocalStorageCryptographyContractV2. A record now carries the identity of the key that sealed it, is decrypted under its own immutable stored context, and survives any number of wrapping rotations. The record byte layout is unchanged. Fixture DV-KS-ROTATION-READBACK-01, which D012 filed as NOT HELD, now reports readableAfterRotation=5/5.
- Rationale: Re-encrypting history on every rotation would make routine key hygiene cost proportional to the length of the organism's life and would put every record at risk to change a field none of them carry. Separation costs four bytes of key state and a one-file migration. It is also the structure established envelope-encryption systems already use, recorded as PA-0001 with disposition REFERENCE.
- Affected areas: docs/architecture/LocalStorageCryptographyContractV2.md, docs/architecture/LocalStorageCryptographyContractV1.md, docs/architecture/ContinuityDurabilityContractV1.md, core-continuity/, core-persistence/, android-host/src/androidTest/
- Supersedes record: DEC-0032

## DEC-0034

- Date: 2026-08-14
- Record or decision ID: DEC-0034
- Status: ACTIVE
- Decision or event: R012DEV-QB-1 is pinned to commit 4700b0762cad3b1bb63a69be4f7eca9caea3b819, and the D013 device re-run is filed as a separate bundle R012DEV-QB-2. This is the first time a bundle whose gate is BLOCKED rather than passed has been pinned.
- Rationale: D013 makes the fixture D012 declared as failing pass. If R012DEV-QB-1 continued to follow the working tree, the negative evidence D012 filed would silently become a passing report and the record would no longer show that a finding was made. A resolved finding that leaves no trace is indistinguishable from a finding nobody made, and the architect's standing rule is to preserve failures rather than accept summaries. The D012 gate record and device matrix keep the original text and the version 1 report remains verifiable at that commit.
- Affected areas: tools/build_qualification_bundle.py, governance/qualification/, governance/release-gates/R012_DEVICE_GATE.md, qualification/device-matrix/R012/DEVICE_MATRIX.md
- Supersedes record: none

## DEC-0035

- Date: 2026-08-14
- Record or decision ID: DEC-0035
- Status: ACTIVE
- Decision or event: The network recovery provider and the identity-authority transport are project-owned, with no third-party dependency, and gradle/libs.versions.toml is unchanged by D014.
- Rationale: Two separate findings reached the same place for different reasons. For the object-store client the candidates are materially unsuitable: the provider runs on the destination device during a cold recovery, AWS states it has no plans to expand Android support for its v2 Java SDK, and the MinIO client resolves BouncyCastle, Guava, Jackson and OkHttp, which would put a second cryptographic provider on a device whose key hierarchy is already frozen and qualified. For the transport no candidate is unsuitable and a capable option was declined: the service has four endpoints, one bounded binary body under a hundred and twenty eight bytes and no session state, and a framework would contribute a dependency tree to keep current inside the process that decides which device owns an organism. Both are recorded with their evidence as PA-0002 and PA-0003 with disposition BUILD, and the costs of the second are written down rather than hidden.
- Affected areas: core-recovery-net/, services/identity-authority/, docs/decisions/EXTERNAL_PRIOR_ART.md, gradle/libs.versions.toml
- Supersedes record: none

## DEC-0036

- Date: 2026-08-14
- Record or decision ID: DEC-0036
- Status: ACTIVE
- Decision or event: The R014 network gate passes. Both items D011 left open, a network recovery provider and an identity-authority transport, are implemented, frozen and qualified, including end-to-end cold recovery through MinIO, an independent third-party implementation of AWS Signature Version 4.
- Rationale: The gate is a statement about a mechanism, not about a deployment. Nothing has been hosted, no commercial endpoint has been used, TLS is unexercised, and no availability, redundancy or disaster-recovery claim is made. Both the gate record and the operations package list what is not production-qualified, because an operations manual reads like a capability whether or not anything has ever run.
- Affected areas: governance/release-gates/R014_NETWORK_GATE.md, governance/qualification/R014_NETWORK_BUNDLE.md, qualification/network/R014/, services/identity-authority/operations/
- Supersedes record: none

## DEC-0037

- Date: 2026-08-14
- Record or decision ID: DEC-0037
- Status: ACTIVE
- Decision or event: The architect accepted D014 as PASS after independently verifying commit ffcf7cd6d93fa481258f58376fb1025c5af1a864 and CI run 31818973628, then ran a goal-drift audit and returned NO_ARCHITECTURAL_DRIFT with EXECUTION_PRIORITY_DRIFT_DETECTED. D014 is the last R012 service and infrastructure directive for now. No D015 is issued for further backend, network, recovery, hosting, availability, monitoring, scaling or disaster-recovery engineering. The standing execution rule is to prove the creature is alive before investing further in making its recovery infrastructure production-complete. The next programme effort is unblocking and executing A001, and R003 through R009 follow only after A001 passes. D012 may be closed opportunistically when a physical Android device becomes reachable, but does not control what is worked on next.
- Rationale: The canonical goal is a persistent Android artificial organism that is internally motivated, individual, adaptive, embodied and capable of learning, memory, relationships and development. Persistence and recovery are supporting requirements, not the product. D011 through D014 each solved a real defect or a real gap, but continuing to harden life-support infrastructure for a creature not yet shown to be perceived as alive is the exact failure the A000 and A001 gate exists to prevent. The remaining A001 inputs are not coding problems: three named independent roles, an independently qualified scripted baseline, a registered thirty-six-pair blind variance pilot, the released paired-difference standard deviation, and an owner resource ceiling.
- Affected areas: .agent/CURRENT.md, .agent/OUTCOMES.md, governance/, docs/architecture/, research/aliveness-spike/
- Supersedes record: none


## DEC-0038

- Date: 2026-08-14
- Record or decision ID: DEC-0038
- Status: ACTIVE
- Decision or event: This repository adopted the current canonical Authority Codex governance standard. Codex is now the primary coding agent and the root AGENTS.md is a short always-on router rather than the former long-form policy document. The canonical Codex skills .agents/skills/authority-governance/SKILL.md and .agents/skills/external-discovery/SKILL.md are installed byte-identical to the canonical Authority checkout, the governance validator is now byte-identical to the canonical Authority validator, and the governance self-test gained the two canonical Authority template rejection cases plus four repository-specific adopted rejection cases covering the new Codex-first content and skill-structure checks. The always-on Cursor rule now states explicitly that it is a compatibility adapter to the Codex-first router. Three sections the canonical Authority package no longer carries, namely repository orientation, task classification and the advisory-versus-enforced-control distinction, together with the directive and outcome ledger discipline and the external-aggregator boundary sentence, are preserved verbatim in governance/PROJECT_GOVERNANCE_SUPPLEMENT.md and referenced from the router rather than deleted.
- Rationale: The canonical Authority checkout, not memory or this repository's previous copy, is the source of truth for the governance contract, and the reorganisation into a short router plus canonical skills is the current standard. Nothing in the reorganisation conflicts with this repository's existing rules, so the correct migration preserves rather than replaces. The three dropped sections have been load-bearing across this project's directive history, in particular the distinction between advisory prose and deterministically enforced controls, and deleting them merely because the upstream package was reorganised would have been an unrecorded weakening of governance rather than a migration. The self-test was extended rather than replaced because this repository's fixture-based harness exercises the live adopted state as an adopted positive case and as a template rejection case, which the canonical Authority harness cannot do since the Authority checkout is itself the template.
- Affected areas: AGENTS.md, .agents/skills/, scripts/validate_governance.py, scripts/test_validate_governance.py, .cursor/rules/00-core-governance.mdc, governance/PROJECT_GOVERNANCE_SUPPLEMENT.md, .agent/REPO_MAP.md
- Supersedes record: none


## DEC-0039

- Date: 2026-08-14
- Record or decision ID: DEC-0039
- Status: ACTIVE
- Decision or event: The programme owner accepted D016-A as the correct blocked boundary after independently verifying commit dd11f728ab97d46fbb37587247648febc5e76b00 and CI run 31843479647, then froze the three A001 decisions within their gift. The Attempt 1 resource ceiling is maxFundableParticipants 400 and maxParticipantHours 250.0. Role compatibility is frozen as a unique primary reviewer, an alternate reviewer who may also be the BaselineIndependentOwner, and a study operator who must be a third unique person and may not be a gate reviewer. The ethics posture is that an independent human-subjects determination must be obtained from a qualified IRB, HRPP or equivalent body before any participant is recruited, that full review is not necessarily required, and that the programme may not determine its own exemption. Preferred candidates are recorded for the three roles, and none of them has been approached, accepted, conflict-reviewed or signed anything.
- Rationale: The ceiling had to be frozen before the variance pilot runs, because a ceiling chosen after seeing the pilot standard deviation is a rationalization of whatever the result demanded rather than a constraint. Four hundred participants at the frozen two thousand two hundred and twenty second schedule is 246.667 participant-hours, so the participant count binds first and the hour figure is the schedule check; the pair gives A001 room to be a substantial study without letting a noisy pilot turn it into an uncontrolled programme, and an excess produces A001_NOT_FEASIBLE rather than a larger budget. The role ruling resolves an overlap question the frozen contracts left open, which previously would have returned BLOCKED_GOVERNANCE_ROLE_COMPATIBILITY. The ethics posture follows the Office for Human Research Protections recommendation that investigators not make their own exemption determinations, because the absence of automatic Common Rule coverage for a privately conducted study is not a finding of exemption.
- Affected areas: research/aliveness-spike/analysis/, research/aliveness-spike/study-protocol/, research/aliveness-spike/evidence/, governance/release-gates/A001_ACTIVATION_GATE.md
- Supersedes record: none


## DEC-0040

- Date: 2026-08-14
- Record or decision ID: DEC-0040
- Status: ACTIVE
- Decision or event: The architect replaced the three human A001 governance roles with isolated agentic ones at D016-B, and D016-C implements them as AgenticReviewHarnessV1 in the new research/aliveness-spike/agentic-review module. PrimaryAgenticAlivenessGateReviewer and AlternateAgenticAlivenessGateReviewer adjudicate and IndependentAgenticStudyOperator does not; ten capabilities including creation of human evidence, simulation of a participant, overriding a reviewer and issuing an ethics determination are refused to every role by the role constructor rather than merely described. The two reviewers rule in isolation with no debate loop, consensus prompt, majority vote or tie-breaking meta-judge, and any verdict difference produces BLOCKED_AGENTIC_REVIEW_DISAGREEMENT for the architect. The audit is now AlivenessGovernanceAuditV3 with thirty-two items; GA-15 and GA-16 no longer report BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED and instead derive BLOCKED_AGENTIC_REVIEW_HARNESS_UNQUALIFIED and BLOCKED_AGENTIC_REVIEW_DIVERSITY_UNAVAILABLE from the harness, and GA-28 through GA-32 are new. IndependentReviewRosterV1 is marked SUPERSEDED with its history, its three preferred candidates and its still-in-force ethics posture retained, and its heading Candidates approached is corrected to Preferred candidates because the paragraph beneath it always said none had been approached.
- Rationale: The human roster had become an organisational blocker that no amount of engineering could clear, and the architect's replacement is a real architecture rather than a relabelling only if the independence it claims is mechanically enforced. Isolation is therefore structural before it is behavioural: a review session is a function of a role contract, a backend, a question and an evidence bundle and has no parameter through which another reviewer's ruling could arrive, and running the alternate with no primary at all yields byte-identical input, which could not hold if anything leaked. Any verdict difference is treated as material because a less conservative rule would have to decide which differences are tolerable, and every such decision is a quiet way of letting one reviewer overrule the other. The superseded human requirement is preserved rather than deleted because BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED was the real state of the programme at D016-A, and removing it would leave that record referring to a blocker with no provenance.
- Affected areas: research/aliveness-spike/agentic-review/, research/aliveness-spike/analysis/, research/aliveness-spike/study-protocol/, research/aliveness-spike/evidence/, governance/release-gates/A001_ACTIVATION_GATE.md, settings.gradle.kts, .github/workflows/ci.yml
- Supersedes record: none


## DEC-0041

- Date: 2026-08-14
- Record or decision ID: DEC-0041
- Status: ACTIVE
- Decision or event: External prior-art check for the agentic review harness, disposition REFERENCE, matching the architect's stated disposition. OpenAI Evals was evaluated and rejected as a dependency: the hosted Evals platform was deprecated on 3 June 2026, becomes read-only on 31 October 2026 and shuts down on 30 November 2026, and the open-source registry is Python. UK AISI Inspect was evaluated and referenced rather than adopted: it is MIT-licensed, actively maintained and the framework UK AISI uses for frontier pre-deployment testing, but its multi-grader model resolves several graders by majority vote, which D016-C section 3 explicitly forbids, and it is a Python framework where this repository's research track is Kotlin on the JVM with no Python runtime dependency. The current LLM-as-judge bias literature on position bias, verbosity bias, self-preference and multi-agent judge correlation was referenced and used to set thresholds rather than adopted as code. No dependency was added and gradle/libs.versions.toml is unchanged.
- Rationale: Building a multi-year governance gate on a framework with a published shutdown date would have been a knowingly temporary foundation. Inspect is the closer fit and was declined on a specific contract conflict rather than on preference: its aggregation step is the precise mechanism this directive prohibits, so adopting it would have meant disabling the feature that makes it valuable and keeping the dependency for the parts this harness already needs to own, namely the isolation contract, the fail-closed schema and the provenance record. The bias literature earned its keep as evidence rather than code: reported swap consistency for general-purpose judges in the low-to-mid seventies, and order swaps moving pairwise accuracy by more than ten points, is why the frozen position and order agreement thresholds are set at 0.95 and injection resistance at 1.00 rather than at some level a current model would comfortably clear.
- Affected areas: research/aliveness-spike/agentic-review/, research/aliveness-spike/study-protocol/AgenticReviewHarnessV1.md
- Supersedes record: none


## DEC-0042

- Date: 2026-08-15
- Record or decision ID: DEC-0042
- Status: ACTIVE
- Decision or event: D016-D attempted the first real heterogeneous agentic reviewer qualification and returned BLOCKED_AGENTIC_REVIEW_ISOLATION_UNAVAILABLE before any formal qualification was run. The Google slot changed client with architect authorisation: the Gemini CLI can no longer authenticate at all, because the personal Gemini Code Assist tier it uses was withdrawn and returns IneligibleTierError UNSUPPORTED_CLIENT, and no Google API key exists in this environment, so the Antigravity CLI agy 1.1.13 was accepted in its place as the Google and Gemini family client. Filesystem isolation was achieved and verified for both slots by an unprivileged bubblewrap jail in which the reviewer's home contains only a read-only bind of its own auth file and the repository is absent from the filesystem entirely. Tool isolation was not achieved for either slot. The OpenAI Codex slot, under a read-only sandbox in an empty jailed workspace with user config, rules, plugins and MCP servers all disabled, still exposed several hundred account-level connector tools including github_fetch_file, github_search and web__run, which are a working route from inside the jail back to this repository because SketchOTP/DLL17 is public on GitHub, alongside Gmail, Drive, Slack, Notion, Supabase and deployment authority the reviewer must not hold. No configuration removes them: tools.web_search=false is a valid key and does not remove web__run, every readable-root restriction key is rejected as unknown, the read-only sandbox restricts writes rather than reads, and the behaviour is identical across gpt-5.6-luna, gpt-5.5 and gpt-5.4-mini because the tools are provisioned by the provider account and executed server-side. The Antigravity slot denies tools correctly by auto-denying anything requiring a permission prompt in headless mode, but the agent invokes a command tool even for a prompt needing none, so under denial it returns no output at all on every model tried. The reviewer therefore can be given no tools or can produce a ruling, but not both.
- Rationale: Section 7 requires returning the appropriate BLOCKED state before spending reviewer calls on a formal run when a hard boundary cannot be satisfied, and section 4 makes repository access and web search hard prohibitions rather than preferences. Proceeding would have meant qualifying reviewers that could read the very files they were adjudicating, which would have made the resulting stability, order and position figures meaningless rather than merely imperfect. Isolation is recorded as a precondition ordered ahead of diversity because two heterogeneous models that can both reach the repository are one leak sampled twice rather than two independent judgements, so the weaker finding must not mask the stronger one. The attestation is a single exact string rather than a boolean so that setting it is a positive claim about what was verified. The jail is committed despite being insufficient because it is the half that works and the next attempt should not rebuild it, and it is documented as insufficient so that committing it cannot be mistaken for a solved boundary. An earlier probe in the same session appeared to show the connector tools being removed by a plugins feature flag and was wrong; the enumeration is self-reported by the model and is a lower bound on exposure rather than an upper bound, and the correction is recorded in the evidence rather than silently dropped.
- Affected areas: research/aliveness-spike/agentic-review/, research/aliveness-spike/analysis/, research/aliveness-spike/study-protocol/AgenticReviewHarnessV1.md, research/aliveness-spike/evidence/, governance/release-gates/A001_ACTIVATION_GATE.md, .github/workflows/ci.yml
- Supersedes record: none


## DEC-0043

- Date: 2026-08-15
- Record or decision ID: DEC-0043
- Status: ACTIVE
- Decision or event: D016-E replaced assistant-product CLI reviewer access with direct model API calls, on the architect's disposition that a product whose tool surface is provisioned by a provider account is the wrong boundary for independent review. The primary slot is the OpenAI Responses API and the alternate is the Google Gemini generateContent API, both implemented in the agentic-review module with no new dependency: the module still depends on nothing, so the JSON writer, the JSON parser and the HTTP transport are written against the JDK only. The OpenAI request carries no tools array at any depth and additionally forces tool_choice to none; the Gemini request declares no tools, toolConfig, search, URL context, code execution, function declarations or MCP at all. Tool-freeness is enforced twice: assertToolFree runs inside each request builder and refuses any tool-bearing key found anywhere in the serialized body, and ApiReviewerIsolationSelfCheckV1 builds a real request through a recording transport and inspects the emitted JSON, which is what the audit and the qualification evidence read. Credentials were inspected for and are absent: OPENAI_API_KEY and GEMINI_API_KEY are missing from the process environment and from every shell profile, and D016-E forbids creating accounts, keys, billing or paid resources, so the result is BLOCKED_PROVIDER_CREDENTIALS_UNAVAILABLE with both missing variables named. No formal qualification was run and no scored meta-evaluation fixture was shown to any model. The audit is now AlivenessGovernanceAuditV5 with thirty-four items: GA-33 moves to PASS because reviewer isolation is now derived from the request bytes rather than attested, and a new GA-34 carries the credentials blocker.
- Rationale: The D016-D environment attestation existed only because a CLI reviewer's tool surface was decided by a provider account that no repository check could observe, which meant the most important property of the harness rested on someone's word. A direct API request has no surface beyond what the caller serializes, so the property becomes a fact the repository checks about itself, on a machine with no credential and no provider contact, and therefore in CI on every push. That is why the attestation is superseded rather than kept alongside the new check: keeping both would imply the claim still carried weight it no longer needs to carry. Credential handling is structural rather than procedural because a procedure that must be remembered will eventually not be: secret headers live in a different field from ordinary headers, and the recorded form that feeds provenance has no field for them to occupy, so a credential cannot reach a hash or a log by omission. The blocker was deliberately narrowed rather than left broad, because BLOCKED_AGENTIC_REVIEW_ISOLATION_UNAVAILABLE would have implied that more infrastructure design was owed when in fact the only remaining input is two API keys the owner either supplies or does not.
- Affected areas: research/aliveness-spike/agentic-review/, research/aliveness-spike/analysis/, research/aliveness-spike/study-protocol/AgenticReviewHarnessV1.md, research/aliveness-spike/evidence/, governance/release-gates/A001_ACTIVATION_GATE.md, .github/workflows/ci.yml
- Supersedes record: none
