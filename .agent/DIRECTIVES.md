# Project Directive Ledger

This append-only ledger records project directives issued by the user to the AI coder. The adopted project records those directives locally.

ANIMUS ONE may copy or aggregate the resulting governance files for centralized visibility. ANIMUS ONE does not issue, approve, modify, execute, reconcile, or close directives.

## Entry schema after adoption

Use one live entry for each accepted project task. Keep examples outside this file; do not add a heading beginning with a live directive ID until adoption.

```markdown
## <local-directive-id>

- Issued: <ISO-8601 timestamp with timezone>
- Issuer: User
- External directive: <ID or none>
- Objective: <requested observable result>
- Scope: <authorized areas>
- Exclusions: <prohibited or out-of-scope work>
- Acceptance: <observable completion condition>
- Risk class: <LOW | NORMAL | HIGH | DESTRUCTIVE>
- Relationship: <new | resumes | amends | supersedes>
- Related directive: <local directive ID or none>
- Status at issuance: ISSUED


```

Do not record execution results here. Do not rewrite historical entries after adoption. Append corrections, amendments, and supersessions referencing the original entry.

## D-001

- Issued: 2026-08-12T19:37:10-04:00
- Issuer: User
- External directive: D001
- Objective: Acknowledge the Digital Living Lifeform project charter, restate the project objective, confirm canonical authority, confirm the current execution gate state and the non-negotiable boundaries, and return a completion report establishing the R000 execution baseline.
- Scope: Reading the canonical Digital Living Lifeform Notion page and the Implementation Plan E2E page, and returning one Markdown completion report.
- Exclusions: Any repository, source-code, build, configuration or infrastructure change; implementation proposals; architecture modifications; plans for future phases.
- Acceptance: A completion report containing the required sections, with an explicit confirmation that no repository change was made.
- Risk class: LOW
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED



## D-002

- Issued: 2026-08-12T19:41:43-04:00
- Issuer: User
- External directive: D002
- Objective: Establish the repository governance layer as a truthful, fully adopted R000 execution baseline in which every governance file required by the existing validator is adopted, the validator passes repository-wide in ADOPTED mode, and the recorded state matches the canonical gate state without invention.
- Scope: The governance files under .agent/ only, retaining the adopted .agent/PROJECT_GOAL.md from the D-001 event unless factual correction is required.
- Exclusions: R001 implementation; DeterminismContractV1; organism physiology; behavior or action-controller implementation; learning; memory; relationships; development; AR or spatial implementation; production persistence implementation; A000 behavioral experiments; any non-governance source, build or infrastructure change; erasing or rewriting the D-001 noncompliance; proposing or beginning D003.
- Acceptance: python3 scripts/validate_governance.py --mode ADOPTED returns zero validation errors, every required governance file is in a truthful adopted state, the D-001 nonconformance remains recorded, and no non-governance change was introduced.
- Risk class: NORMAL
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED

## D-003

- Issued: 2026-08-12T19:55:43-04:00
- Issuer: User
- External directive: D003
- Objective: Establish a clean durable source-control and provenance baseline for the greenfield repository, and repair the governance validator self-test so the governance tooling is internally consistent after adoption.
- Scope: Local Git initialization, exclusion rules, one baseline commit, scripts/test_validate_governance.py and the minimum supporting governance-test fixtures, and the governance records needed to capture the result.
- Exclusions: Android or product implementation modules; organism behavior; physiology; learning or memory systems; persistence implementation; DeterminismContractV1; R001 implementation; A000 behavioral mechanisms; creating or publishing a remote repository; rewriting governance history or implying Git provenance existed before D-003; weakening the production adopted validator to make its tests pass; beginning or proposing D004.
- Acceptance: The repository is a valid local Git repository with a durable baseline commit and a clean final worktree, the adopted-mode governance validation and the governance self-test both return zero errors, the repaired self-test carries meaningful positive and rejection coverage, and the D-001 and D-002 records remain truthful.
- Risk class: NORMAL
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED

## D-004

- Issued: 2026-08-12T20:21:55-04:00
- Issuer: User
- External directive: D004
- Objective: Build the buildable greenfield R000 project skeleton for the Digital Living Lifeform, freeze ProjectIdentityBuildContractV1, create the mandatory R000 registries, establish CI, and publish the work to the authoritative GitHub repository.
- Scope: Gradle multi-module project structure per Implementation Plan E2E, pure Kotlin JVM core modules, headless desktop runner, minimal Android shell, the seven mandatory R000 registries, the frozen project identity and build contract, a root validation and build path, a GitHub Actions workflow, the GitHub remote and first push, and the governance records needed to capture the result.
- Exclusions: Physiology; drives; action selection; learning; memory; relationships; development or evolution; persistence semantics; Torpor and rehabilitation; AR behavior; RouteEvidence behavior; DeterminismContractV1; R001 algorithms; A000 organism mechanisms; beginning or proposing D005.
- Acceptance: ProjectIdentityBuildContractV1 exists and matches the directive exactly, the module boundaries match Implementation Plan E2E, the pure Kotlin JVM modules compile with no Android dependency, the Android shell builds and provides a minimal launchable surface, all seven mandatory registries exist with no invented future organism entries, governance adopted validation and the governance self-test pass, the full root build and test passes, Android debug assembly passes, GitHub CI exists, the work is pushed to SketchOTP/DLL17, and the final worktree is clean.
- Risk class: NORMAL
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED

## D-005

- Issued: 2026-08-12T20:40:16-04:00
- Issuer: User
- External directive: D005
- Objective: Complete the remaining R000 work and close the R000 exit gate, using the buildable project created under D-004 as the baseline. This is the final R000 directive.
- Scope: Resolve the repository licence conflict in favour of the frozen proprietary policy, obtain an Android execution target and produce actual install, launch, visible-state, terminate and relaunch evidence, measure and record the R000 runtime baseline from the current shell, produce a reproducible hashed R000 qualification evidence bundle, evaluate and close the R000 exit gate, and push the qualified commit with passing GitHub CI.
- Exclusions: DeterminismContractV1 algorithms; fixed-point arithmetic; canonical serializer; canonical state hashing; PRNG and substreams; physiology; drives; action selection; learning; memory; relationships; development; persistence semantics; Torpor; A000 organism mechanisms; AR behavior; RouteEvidence behavior; any R001 implementation; changing repository visibility; beginning or proposing D006.
- Acceptance: The MIT and proprietary conflict is resolved in favour of the frozen policy, the Android shell is installed and actually launched on an Android target, the visible R000 shell state is verified, terminate and relaunch succeed without crash, measured R000 runtime and resource baseline evidence exists, no future production budgets are fabricated, a hashed and reproducible R000 qualification evidence bundle exists, all existing governance, build and boundary tests still pass, GitHub CI passes for the qualified commit, every R000 exit-gate criterion is PASS, no R001 or organism functionality is introduced, local HEAD and origin/main match, and the final worktree is clean.
- Risk class: NORMAL
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED

## D-006

- Issued: 2026-08-13T05:30:00-04:00
- Issuer: User
- External directive: D006
- Objective: Implement and qualify the R001 deterministic core as the foundation every later organism mechanism depends on, freezing DeterminismContractV1 before writing any dependent implementation and qualifying the result against the canonical R001 exit gate.
- Scope: Freeze DeterminismContractV1 with every algorithm, format and version decision the canonical pages require; implement canonical fixed-point numeric semantics, saturation and diagnostics, canonical byte representation and serialization, canonical hashing, versioned domain-separated deterministic randomness and substreams, lookup-table generation and verification, golden vectors and replay fixtures, the reducer and replay kernel, durability classes and the Class W staged witnessed interaction, the commit visibility invariant, the panic witness, the assisted-payload zero-physics interface, and deterministic migration and version boundaries; update the mandatory registries with the R001 facts made real; prove identical canonical bytes and hashes across the required qualification targets; and push a qualified commit with passing GitHub CI.
- Exclusions: Physiology; drives; action selection; learning; memory; relationships; development and evolution; Torpor; resource and care systems; R002 persistence semantics; AR behavior; RouteEvidence behavior; dialogue; all other R002 and later organism behavior; beginning R002; proposing D007.
- Acceptance: DeterminismContractV1 is frozen and complete, the deterministic core conforms to it, numeric semantics and saturation and canonical bytes and hashing and deterministic randomness and lookup and version behavior and golden vectors and replay and migration all pass, the mandatory registries carry the R001 facts without speculative later entries, identical fixtures produce byte-identical canonical outputs and hashes across every target required by the canonical R001 matrix, R001 qualification evidence is reproducible and bound to the frozen contract and qualified commit, existing R000 and governance and build and CI qualifications remain green, every R001 exit-gate criterion is evaluated and passes, the work is pushed to SketchOTP/DLL17, local HEAD equals origin/main, GitHub CI passes, and the worktree is clean.
- Risk class: HIGH
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED


## D-007

- Issued: 2026-08-13T11:05:00-04:00
- Issuer: User
- External directive: D007
- Objective: Complete the R002 continuity, durability, trusted-time and reconciliation foundation so that one organism preserves truthful identity and biological continuity through Android lifecycle interruption, process death, reboot, long absence, storage pressure, thermal and power emergencies, interrupted writes, and restart, without depending on continuous Android background execution.
- Scope: Freeze ContinuityDurabilityContractV1; implement and qualify the four-clock model, durable time anchors, time-confidence classification, clock anomaly handling, bounded blind-decay credit, trusted-time debt and bounded reconciliation, durability classes, material interaction durability boundaries, journal and snapshot recovery, restart and replay correctness, compaction behaviour, storage-pressure handling, durability safe-hold behaviour, platform resource-shed and deep-suspend and recovery ordering, deterministic offline reconciliation modes and ordering, and the encrypted-record boundary; and push a qualified commit with passing GitHub CI.
- Exclusions: R003 physiology; birth; critical-care biology; R004 action selection; AffordanceWorld behavior; RouteEvidence behavior; R005 embodiment; R006 learning; R007 memory; R008 relationships; R009 development and evolution; dialogue; LLM behavior; beginning D008.
- Acceptance: ContinuityDurabilityContractV1 is frozen and the implementation conforms to it, every R002 work package in Implementation Plan E2E is completed, every R002 exit-gate criterion passes, process-death and restart fixtures preserve exactly the required durable state, uncommitted material mutations are never exposed after crash or restart, reboot and clock-manipulation exploit fixtures pass, blind-credit and trusted-time reconciliation remain bounded, storage-pressure and durability-safe-hold behaviour pass qualification, platform suspend and recovery ordering passes qualification, journal and snapshot and replay and recovery and compaction remain deterministic, R001 deterministic qualification remains green, a reproducible R002 qualification evidence bundle exists, GitHub CI passes for the qualified commit, the work is committed and pushed to main, local HEAD equals origin/main, the final worktree is clean, and no R003 or later organism behavior was introduced.
- Risk class: HIGH
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED

## D-008

- Issued: 2026-08-13T15:40:00-04:00
- Issuer: User
- External directive: D008
- Objective: Build and qualify the complete disposable A000 Aliveness Spike so the programme can begin establishing whether the organism hypothesis itself is any good, this being the critical path because R003 through R009 remain blocked until A001 passes.
- Scope: Re-read the current canonical A000 and A001 sections and all later amendments; build the deterministic accelerated simulator and the real-time observer viewer; implement the abstract habitat and the authorized A000 FULL candidate mechanism set; implement and make independently runnable the FULL, ScriptedPetBaselineV1, DegradedScriptedControlV1 and three leave-one-out cohorts through one frozen presentation contract; implement SpikeDecisionTraceV1 and exact coalition attribution; execute the CuriosityBalanceEnvelopeV1 feasibility analysis; create or freeze every pre-A001 research contract that can legitimately be completed before human evidence exists; run enough deterministic accelerated histories to establish technical viability; produce a reproducible A000 evidence bundle; and push a commit with passing GitHub CI.
- Exclusions: Inheriting or importing production organism state, R002 continuity state, persistence, lifecycle behavior, production schemas, the production renderer, the Android organism implementation, qualification evidence as executable behavior, or R003 through R009 production mechanism implementations; fabricating any value requiring real human evidence; claiming the scripted baseline has passed its human competence qualification; beginning A001 scored human data collection; beginning D009.
- Acceptance: The A000 research project is buildable and runnable and isolated from production with only the authorized R001 dependency, the accelerated simulator produces reproducible runs, the real-time viewer exists, all required cohorts run through the same presentation contract, FULL contains the complete authorized candidate mechanism set, SpikeDecisionTraceV1 exists for autonomous actions, exact coalition attribution is executable and evidenced, CuriosityBalanceEnvelopeV1 has been actually evaluated, accelerated histories produce a documented viability or failure result, long-run behavior remains bounded, no human result is fabricated, pre-A001 contracts are frozen where legitimately possible and explicitly blocked where real evidence is still required, a reproducible A000 evidence bundle exists, existing R000 and R001 and R002 qualifications remain green, GitHub CI passes, the work is committed and pushed to main, local HEAD equals origin/main, the worktree is clean, and no R003 through R009 production organism mechanism was introduced.
- Risk class: NORMAL
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED

## D-009

- Issued: 2026-08-13T19:10:00-04:00
- Issuer: User
- External directive: D009
- Objective: Revise the disposable A000 FULL candidate in response to the accepted D008 negative evidence, so the organism is ready for A001, without altering the curiosity-envelope thresholds and without consuming the programme's single allowed threshold-only revision.
- Scope: Resolve the long-run population convergence and closest-pair differentiation failures; give the organism bounded reconsideration and re-sampling of a previously rejected option after outcome evidence changes; either materially revise the episodic mechanism so it demonstrably contributes to history-dependent individuality or remove it from FULL and update the ablation family; revise curiosity and anti-convergence behaviour so at least one common configuration returns NON_EMPTY_FEASIBLE_REGION under the existing frozen thresholds; re-run the complete accelerated qualification suite and the complete joint envelope search; add targeted fixtures where necessary; preserve all D008 failed results and configurations as durable negative evidence; and push a commit with passing GitHub CI.
- Exclusions: Altering the curiosity-envelope thresholds; consuming the threshold-only revision; weakening, removing or redefining existing technical floors; weakening ScriptedPetBaselineV1 or DegradedScriptedControlV1 or SpikeExpressionContractV1; baseline human competence qualification; BlindVariancePilotV1 execution; A001 recruitment; A001 scored sessions; A001 result analysis; R003 through R009 production implementation; beginning D010.
- Acceptance: The revised candidate remains bounded and numerically stable, existing long-run differentiation and diversity floors pass, previously rejected alternatives can be boundedly re-explored after contingency or devaluation changes, the episodic mechanism either demonstrates a positive contribution or is removed from FULL, CuriosityBalanceEnvelopeV1 returns NON_EMPTY_FEASIBLE_REGION, curiosity thresholds remain unchanged, substantive-spontaneity attribution still passes, all prior negative evidence remains preserved, research and production isolation remains intact, a reproducible revised A000 evidence bundle exists, R000 and R001 and R002 qualification remains green, GitHub CI passes, the work is committed and pushed to main, local HEAD equals origin/main, the final worktree is clean, and no R003 through R009 production mechanism is introduced.
- Risk class: NORMAL
- Relationship: amends
- Related directive: D-008
- Status at issuance: ISSUED

## D-010

- Issued: 2026-08-13T21:30:00-04:00
- Issuer: User
- External directive: D010
- Objective: Prepare the complete A001 Attempt-1 study and governance package so actual human work can begin immediately once the named independent-review roster and the feasible participant and resource budget are supplied, without collecting any human outcome data.
- Scope: Finalize and freeze AlivenessStudyProtocolV1 with the exact participant-facing instrument wording and anchors and the exact primary decision rule; freeze the Attempt-1 human mechanism family as three arms under Holm-Bonferroni at family-wise error rate 0.05; finalize BaselineQualificationProtocolV1 at forty participants with a fifteen-point competence margin and prepare the baseline behaviour and coverage manifest; finalize the executable thirty-six participant blind variance pilot and prove on synthetic data that the FULL-facing channel releases only the paired-difference standard deviation; finalize the A001FeasibilityBudgetV1 calculator on the frozen power constants; finalize and test the complete A001 analysis package on synthetic fixtures; prepare the independent reviewer onboarding package; update the executable governance audit so it blocks premature activation; prepare the participant information, consent and privacy materials; produce reproducible D010 evidence; and push a commit with passing GitHub CI.
- Exclusions: Recruiting participants; executing baseline human qualification; executing the blind variance pilot; collecting human study data; starting A001 Attempt 1; inspecting real human outcome data; implementing R003 through R009 production organism mechanisms; inventing the maximum fundable participants or participant-hours or cost ceiling; inserting placeholder people or invented names into the roster; claiming institutional, ethical or other external approval that does not exist; beginning D011.
- Acceptance: AlivenessStudyProtocolV1 is frozen and executable, the exact instrument wording and anchors are frozen, the ten-point and ninety-five-percent-confidence-interval programme rule is encoded exactly, the baseline qualification package is operationally ready, BlindVariancePilotV1 is operationally ready, a synthetic dry run proves the FULL-facing pilot channel releases only the paired-difference standard deviation, the A001 analysis pipeline passes synthetic positive and negative fixtures, the feasibility calculator is complete, only the actual owner resource ceiling and the real pilot standard deviation remain unresolved in that calculator, the independent reviewer onboarding package is complete and truthfully unassigned, the governance audit correctly prevents premature A001 activation, the participant information and privacy materials are complete, no human outcome data is collected, no R003 through R009 production implementation is introduced, existing R000 and R001 and R002 and A000 qualification remains green, reproducible D010 evidence exists, GitHub CI passes, the work is committed and pushed to main, local HEAD equals origin/main, and the final worktree is clean.
- Risk class: NORMAL
- Relationship: amends
- Related directive: D-009
- Status at issuance: ISSUED

## D-011

- Issued: 2026-08-14T09:00:00-04:00
- Issuer: User
- External directive: D011
- Objective: Advance the bounded R012 persistence, recovery and identity substrate authorized by the 2026-08-14 parallel-execution amendment while A001 remains externally blocked, so that infrastructure the canonical plan already requires is qualified rather than idling.
- Scope: Re-read the canonical architecture, the R012 section of Implementation Plan E2E and the parallel-execution amendment; freeze PersistenceBackendContractV1, LocalStorageCryptographyContractV1, RecoveryCryptographyContractV1, IdentityAuthorityProtocolV1 and at least one concrete RecoveryPackageStore provider contract; select a production persistence backend from measured evidence and qualify it against the frozen R002 durability semantics; implement and qualify local encrypted persistence with key rotation and interrupted rewrap; implement and qualify recovery cryptography and end-to-end cold recovery; implement and qualify the identity-epoch authority; implement one concrete recovery-package provider; exercise the selected backend against real failure behaviour including process death, interrupted journal, snapshot and compaction writes, corruption and full storage; collect real latency and storage measurements; prove that persistence, encryption, recovery and provider choice do not change canonical bytes or hashes; keep R000, R001, R002, A000 and A001PRE green; produce a reproducible qualification bundle; and push a commit with passing GitHub CI.
- Exclusions: A001 participant recruitment; baseline human qualification; variance-pilot execution; scored human data; R003 physiology, birth and care; R004 action selection, AffordanceWorld and RouteEvidence; R005 embodiment; R006 learning; R007 memory; R008 relationships; R009 development and evolution; dialogue; LLM behaviour; full R012 UX, sensors, notifications and product completion; inventing organism behaviour to test storage; fabricating production limits; beginning D012.
- Acceptance: All five contracts are frozen before dependent behaviour is treated as qualified; a concrete production backend is selected from measured evidence and preserves the frozen R002 durability semantics; local encrypted persistence, key rotation and interrupted rewrap are qualified; corruption produces detection or quarantine rather than a silent reset; a concrete recovery-package provider exists; end-to-end supported cold recovery succeeds; corrupt, stale, duplicate and unauthorized recovery paths are refused correctly; identity epoch and authority semantics pass qualification; copied-state quarantine works; provider or network failure cannot strand ordinary local operation; canonical serialization and hashing remain independent of persistence, encryption and recovery; real backend latency and storage measurements are recorded; prior qualifications remain green; a reproducible D011 qualification bundle exists; GitHub CI passes; the work is committed and pushed to main under the required commit identity; local HEAD equals origin/main; the final worktree is clean; and no A001 human work or R003 through R009 organism mechanism is introduced.
- Risk class: NORMAL
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED


## D-012

- Issued: 2026-08-14T13:10:00-04:00
- Issuer: User
- External directive: D012
- Objective: Close the Android-device half of the R012 persistence and cryptography substrate by implementing the Android Keystore adapter and the app-private storage integration against the D011-frozen contracts, and qualifying them on physical Android hardware.
- Scope: Re-read the canonical architecture, the Implementation Plan E2E R012 persistence and cryptography requirements and every D011-frozen contract; implement the Android production adapter for the frozen local-key semantics using Android Keystore; integrate the qualified append-log backend into Android app-private storage through a thin host adapter; qualify Keystore wrapping-key creation, lookup, non-exportability, wrapped data-key creation, decryption after process restart, persistence across process death, interrupted rewrap recovery, epoch advancement, key deletion, unreadability after key destruction and the absence of silent identity or data-key generation; qualify device journal creation, append, metadata-inclusive force, snapshot creation and reopen, replay, compaction, application restart and repeated process death and recovery; reproduce a device fault matrix; verify backup and device-transfer exclusion from the built or installed package; prove canonical bytes and hashes are identical before persistence, after encrypted persistence, after process death, after restart and after recovery; collect real device measurement distributions; reverify R000, R001, R002, A000, A001PRE and D011 evidence; produce a reproducible D012 evidence bundle; and push a commit with passing GitHub CI.
- Exclusions: A001 recruitment; baseline human qualification; variance-pilot execution; scored human data; R003 physiology; birth and care; R004 action selection and AffordanceWorld; R005 embodiment; R006 learning; R007 memory; R008 relationships; R009 development and evolution; network RecoveryPackageStore deployment; identity-authority production deployment and operations; full R012 product user experience and sensor completion; changing a frozen D011 contract to make a device test pass; deriving production thresholds merely because measurements exist; substituting desktop or emulator results for physical-device evidence; beginning D013.
- Acceptance: Android Keystore integration is implemented against the frozen contracts; Android app-private persistence uses the frozen backend contract; the Keystore-backed key lifecycle survives ordinary process death and restart; interrupted rewrap behaviour is qualified; key destruction or refusal does not silently create a fresh organism; device journal and snapshot recovery pass; process death before acknowledgement cannot create history; process death after acknowledgement preserves history; corrupt records are detected and quarantined correctly; canonical bytes and hashes are identical before and after device persistence and recovery; backup and device-transfer exclusion is verified from the built or installed package; real physical-device performance evidence exists; R000, R001, R002, A000, A001PRE and D011 remain green; a reproducible D012 evidence bundle exists; GitHub CI passes on the qualified commit; the work is committed and pushed under the required commit identity; local HEAD equals origin/main; the final worktree is clean; and no A001 human work or R003 through R009 organism behaviour is introduced.
- Risk class: NORMAL
- Relationship: resumes
- Related directive: D-011
- Status at issuance: ISSUED


## D-013

- Issued: 2026-08-14T16:40:00-04:00
- Issuer: User
- External directive: D013
- Objective: Implement the 2026-08-14 local-storage key-epoch separation amendment as a versioned successor contract, so that an ordinary wrapping-key rotation no longer makes the organism's existing journal unreadable, without disturbing unaffected qualified persistence, recovery, determinism or A-track work.
- Scope: Create a versioned successor to LocalStorageCryptographyContractV1 separating the device wrapping epoch, the data-encryption-key identity and the immutable per-record encryption context; make ordinary wrapping rotation advance the wrapping epoch, rewrap the same data key, leave historical ciphertext and canonical history intact, preserve readability of records written under earlier wrapping epochs, permit old and new records to coexist and never require full-history re-encryption; decrypt historical records using the currently recovered data key and the record's own immutable authenticated metadata; keep an actual data-key rotation out of scope as a distinct future capability; define and qualify a deterministic, idempotent, replay-safe and crash-safe compatibility and migration rule for existing V1 encrypted records; add targeted regression fixtures for rotation, interrupted rewrap, authentication and context tampering and V1 compatibility; make the previously failing DV-KS-ROTATION-READBACK-01 pass without deleting, weakening, bypassing or relabelling it; prove canonical bytes and hashes remain independent of wrapping epoch, wrapping key, Keystore material, data-key wrap representation, record ciphertext, nonce, storage location and migration state; record the external prior-art check with disposition REFERENCE; reverify R000, R001, R002, A000, A001PRE, R012-QB-1 and the D012 device-independent evidence; and push a commit with passing GitHub CI.
- Exclusions: Claiming a D012 physical-device pass; fabricating hardware-backed or StrongBox evidence; altering SEGMENTED_APPEND_LOG_V1; replacing the qualified ChaCha20-Poly1305 and HKDF implementation; adopting Tink, Google Cloud KMS or another runtime dependency; implementing data-encryption-key rotation, key rings, historical-key retention or re-encryption migration; implementing a production network recovery provider; deploying the identity-authority service; executing A001 human work; implementing R003 through R009 organism mechanisms; beginning D014.
- Acceptance: Versioned epoch semantics are frozen and internally consistent; the wrapping epoch and the data-key and record context are no longer conflated; ordinary wrapping rotation preserves the same data key; historical records survive one and multiple wrapping rotations; mixed old and new records remain readable after restart; no full-history rewrite occurs during ordinary wrapping rotation; interrupted rewrap remains crash-safe; V1 compatibility and migration are deterministic, idempotent and crash-safe; DV-KS-ROTATION-READBACK-01 passes; canonical bytes and hashes remain unchanged; all unaffected pinned qualifications remain valid; the external prior-art disposition is recorded as REFERENCE; GitHub CI passes; the work is committed and pushed to main under the required commit identity; local HEAD equals origin/main; the final worktree is clean; and no A001 human work or R003 through R009 production mechanism is introduced.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-011
- Status at issuance: ISSUED


## D-014

- Issued: 2026-08-14T20:30:00-04:00
- Issuer: User
- External directive: D014
- Objective: Implement and qualify the remaining network-facing recovery and identity-authority substrate — a vendor-neutral S3-compatible recovery package provider, a versioned identity-authority network transport, and a minimum service operations package — without making ordinary organism operation dependent on a cloud service.
- Scope: Re-read the latest architecture, Implementation Plan E2E R012, the D011 through D013 contracts and amendments and the current external prior-art record; implement RecoveryPackageStore over an S3-compatible object-storage interface with vendor-neutral endpoint, region and bucket configuration, externally supplied credentials, no vendor-specific organism logic and encrypted package bytes only; evaluate serious maintained JVM S3 client candidates before choosing a dependency and record the disposition with licence, maintenance status, dependency size, compatibility and testability; qualify idempotent put, conditional replacement, exact retrieval, integrity receipt, interrupted upload, failed replacement, duplicate request, stale sequence, deletion, wrong bucket, authentication failure, timeout and endpoint outage; add a versioned network transport around IdentityAuthorityProtocolV1 that redefines no epoch, challenge, lease, replay, rate-limit, idempotency or activation semantics; perform a lightweight external framework check before selecting the transport; freeze a transport contract covering endpoints, message schemas, request identifiers, authentication carriage, idempotency, outcome mapping, malformed input, size limits, timeout, replay, duplicates, rate-limit response, concurrency, server restart, durable-state failure and logging privacy; create the minimum deployable operations package covering configuration, secret ownership, health and readiness, durable store configuration, backup contract, restore procedure, log privacy rules, credential rotation, upgrade and migration, an incident runbook and a reproducible local deployment; run end-to-end qualification through an actual compatible network endpoint proving upload, retrieval, verification, identity challenge and proof, atomic epoch advancement and cold recovery; prove provider outage cannot block ordinary local commits, authority outage cannot corrupt local state, retries cannot consume duplicate epochs, competing activations have exactly one winner, restart preserves authority epoch state and stale or replayed requests are refused; prove no canonical plaintext or prohibited organism state crosses the network boundary; prove canonical bytes and hashes are independent of provider, bucket, ETag, request identifier, server timing, HTTP status, region, hostname, retries and network ordering; reverify R000, R001, R002, A000, A001PRE, R012-QB-1, R012DEV-QB-1, R012-QB-2 and R012DEV-QB-2; and push a commit with passing GitHub CI.
- Exclusions: Executing A001 human studies; implementing R003 through R009 organism mechanisms; claiming physical-device qualification without hardware; deploying billable commercial cloud resources without separate authorization; weakening existing recovery, identity, persistence or cryptography contracts; rewriting historical evidence; making any unsupported production hosting, high-availability, SLA, geographic-redundancy or disaster-recovery claim; beginning D015.
- Acceptance: A vendor-neutral network recovery provider is implemented; external S3 client candidates are evaluated and the disposition recorded; a versioned identity-authority network transport exists; transport and framework candidates are evaluated and the disposition recorded; recovery-provider conformance passes against a real compatible network endpoint; the transport preserves all existing protocol semantics; race, idempotency, replay and restart qualification passes; end-to-end network cold recovery passes; a provider or authority outage cannot strand ordinary local life; no canonical plaintext reaches the recovery provider; canonical bytes and hashes remain independent of network and provider behaviour; operations and deployment artifacts exist with no unsupported production claims; prior qualification bundles remain valid; GitHub CI passes; the work is committed and pushed to main under the required commit identity; local HEAD equals origin/main; the final worktree is clean; D012 remains accurately blocked; and no A001 human work or R003 through R009 production mechanism is introduced.
- Risk class: HIGH
- Relationship: resumes
- Related directive: D-011
- Status at issuance: ISSUED


## D-015

- Issued: 2026-08-14T14:58:00-04:00
- Issuer: User
- External directive: D015
- Objective: Commit and push only the already-prepared governance-memory changes that record the D014 closure and the goal-drift audit, introducing no engineering work of any kind.
- Scope: Commit and push the governance-memory records stating that D014 is accepted and closed, that the audit result is NO_ARCHITECTURAL_DRIFT with EXECUTION_PRIORITY_DRIFT_DETECTED, that persistence, recovery, network and service infrastructure is supporting work rather than the product objective, that no further optional backend, network, recovery, hosting, availability, monitoring, scaling or disaster-recovery work is authorized at the current gate state, that A001 is the next programme effort, that D012 may close opportunistically on real physical Android hardware but does not set programme priority, that the five machine-checked A001 blockers are unchanged, and that the identity-authority single-instance constraint is a correctness property rather than a scaling limitation; run the governance validator, the governance self-test and qualification bundle verification before committing; stop with BLOCKED_SCOPE_CONTAMINATION if the changed-file set contains anything outside the intended governance-memory files; and push to main under the required commit identity.
- Exclusions: Any implementation, architecture, dependency, qualification, test-fixture, service, human-study or R003 through R009 organism change; including unrelated changes in the commit; beginning D016.
- Acceptance: Changed files are limited to the intended governance records; the governance validator passes; the governance self-test passes; all qualification bundles verify unchanged; no engineering source, configuration, contract or implementation evidence is modified; the commit is pushed to main; the author identity is correct; local HEAD equals origin/main; the final worktree is clean; and GitHub CI passes if the push triggers it.
- Risk class: LOW
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED


## D-016

- Issued: 2026-08-14T16:45:00-04:00
- Issuer: User
- External directive: D016
- Objective: Execute the complete A001 aliveness gate and determine, from preregistered independent human evidence, whether the remediated adaptive FULL organism produces a meaningfully stronger blinded impression of aliveness than a strong independently qualified scripted pet, and therefore whether R003 through R009 production organism implementation may begin.
- Scope: Sixteen ordered phases. A001.0 populate and activate the three roster roles and the independent study operator, verify frozen independence, obtain the four signed governance records and the reviewer contact details, and obtain the six reviewer rulings; A001.1 freeze the owner resource ceiling maxFundableParticipants and maxParticipantHours before the powered sample is known; A001.2 qualify ScriptedPetBaselineV1 against DegradedScriptedControlV1 with forty independent participants at a plus fifteen point competence margin with a confidence interval lower bound above zero, then freeze and hash the qualified comparator; A001.3 register BlindVariancePilotV1 as VARIANCE_ONLY and NON_SCORED and freeze the candidate, baseline, viewer, contracts and instrument before pilot participant one; A001.4 run the pilot to thirty-six analysable pairs releasing only pairedDifferenceSd and protocolValid; A001.5 compute feasibility at alpha 0.05 two-sided, power 0.80, minimally worthwhile difference 10.0 and pilot standard-deviation inflation 1.25 using exact noncentral-t, powering each of the three ablation arms at alpha over three; A001.6 freeze the Attempt 1 preregistration with exact hashes; A001.7 run the paired within-rater scored attempt at six hundred seconds per creature with frozen counterbalancing, blinded CREATURE labels, session-start equivalence and exactly six permitted interactions; A001.8 and A001.9 apply GradedAlivenessInstrumentV1 and the two secondary questions verbatim; A001.10 screen exclusions in the fixed six-reason order, complete case only; A001.11 classify the primary result as PASS, FAIL_NOT_PRACTICALLY_MEANINGFUL, FAIL_IMPRECISE or FAIL_NULL_OR_NEGATIVE against the frozen rule of mean at least plus ten and confidence interval lower bound above zero; A001.12 run the three disjoint-pool human mechanism ablations under Holm-Bonferroni at family-wise error rate 0.05; A001.13 obtain independent adjudication; A001.14 record the programme result and preserve the evidence package; A001.15 apply the material-change eligibility rules to any Attempt 2 or Attempt 3. Report at each hard boundary as D016-A through D016-G, and push repository governance and evidence changes for architect verification.
- Exclusions: Beginning R003 through R009; continuing optional R012 infrastructure hardening; creating placeholder names or signatures; claiming institutional review board, ethics board or institutional approval that does not exist; guessing undefined behaviour instead of returning a BLOCKED_SPEC state; weakening ScriptedPetBaselineV1; weakening the plus ten success floor; exposing pilot outcome direction to the FULL team; reusing baseline or pilot participants in scored pools; merging participant pools; replacing preregistered statistics after seeing results; changing exclusions after seeing results; running an underpowered scored attempt; erasing failed attempts; modifying FULL because a human result looks unfavourable; and beginning D017.
- Acceptance: Twenty-one criteria, all required for final PASS. Roles validly assigned; declarations signed; reviewer pretest, ethics and baseline rulings recorded; owner ceilings frozen; baseline independently qualified; pilot registered before participant one; thirty-six valid blind pilot pairs obtained; only permitted release fields reaching the FULL team; feasibility computed from the actual released standard deviation; the powered study fitting the pre-frozen ceiling; Attempt 1 preregistered before scored participant one; scored pools disjoint and correctly screened; the primary study reaching its powered analysable sample; three powered ablation arms reaching their targets; the frozen analysis executing without post-data modification; independent adjudication complete; complete evidence preserved; no governance breach; the programme state correctly updated; governance and evidence changes pushed with green CI; and no R003 through R009 production implementation performed inside D016.
- Risk class: HIGH
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED


## D-GOV-001

- Issued: 2026-08-14T17:10:00-04:00
- Issuer: User
- External directive: none
- Objective: Bring this repository into compliance with the current canonical Authority Codex governance standard, making Codex the primary coding-agent operating model, while preserving all existing project-specific work, history, architecture, evidence, directives, qualified results and repository behaviour.
- Scope: Inspect the canonical Authority checkout at /home/sketch/Projects/authority as the source of truth rather than remembered rules; read its AGENTS.md, COMMANDMENTS_OF_THE_CODE.md, complete .agent contract, both Codex skills, governance validator, governance tests and compatibility adapters; replace the long-form root AGENTS.md with the canonical short Codex-first router that points to the detailed contract; install the canonical .agents/skills/authority-governance/SKILL.md and .agents/skills/external-discovery/SKILL.md verbatim; align the governance validator and the governance self-test with the current canonical Authority implementation without weakening either; update the Cursor always-on rule to defer explicitly to the Codex-first router; preserve every existing .agent record, adapter and project-specific rule by merging rather than replacing; determine the adoption state from evidence rather than declaring it; and validate the result.
- Exclusions: Modifying the canonical Authority repository; replacing existing .agent files with Authority template contents; erasing, rewriting, reordering, reformatting, truncating, flattening or resetting existing project state; fabricating goals, directives, results, validation outcomes, decisions, owners, commands, remotes, architecture, adoption status or historical events; deleting compatibility adapters or useful project-specific rules; weakening tests to force a pass; and altering application behaviour, architecture, dependencies, deployment, infrastructure, APIs, qualified work or unrelated source code.
- Acceptance: The canonical Authority checkout is inspected and used as the reference; the root AGENTS.md is the Codex-first router and preserves the root and nested inheritance model; the complete eight-file .agent contract is present and its existing content preserved; both canonical Codex skills are installed and valid; the validator and self-test enforce the Codex-first structure deterministically and neither is weakened; compatibility adapters are preserved and defer to the router; the reported adoption state is supported by evidence; no unrelated project file or behaviour is changed; the canonical Authority repository is unmodified; and every validation state is reported honestly.
- Risk class: NORMAL
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED


## D-016-C

- Issued: 2026-08-14T20:05:00-04:00
- Issuer: User
- External directive: D016-C
- Objective: Implement and qualify the architect's D016-B decision to replace the previously planned external human reviewer and study-operator governance roles with isolated agentic roles, so that A001 governance stops being blocked on the availability of three academics, without changing what A001 measures or who supplies the perception it measures.
- Scope: Three agentic role contracts named PrimaryAgenticAlivenessGateReviewer, AlternateAgenticAlivenessGateReviewer and IndependentAgenticStudyOperator, with reviewer authority separated from operator authority; mandatory first-pass isolation between the two reviewers with no debate loop, consensus prompt, majority vote or tie-breaking meta-judge, and a machine-enforced disagreement state returned to the architect; machine-enforced reviewer heterogeneity across model families and preferably providers, configured rather than compiled in; durable provenance recording role, contract version, provider, model identity and snapshot where exposed, prompt hash, tool-permission hash, evidence bundle hash, sampling parameters, parser and schema version, raw response hash, retry count and reasons, and execution time as non-decision metadata; a machine-validated structured ruling schema with five verdicts of which one is a pass, and fail-closed behaviour for malformed output, missing fields, parser failure, provider refusal, transport failure, timeout after permitted retries, evidence omission, unsupported conclusions and rulings contradicted by their own prose; an evidence boundary that treats repository files, participant text, logs and model output as data rather than instructions, with adversarial injection fixtures; a frozen meta-evaluation suite with thresholds declared before results, covering the eighteen enumerated governance situations plus regression cases drawn from this programme's own adjudicated history; and the corresponding updates to the A001 governance audit, the activation gate, the study protocol documents, the .agent records and CI.
- Exclusions: Beginning R003 through R009; consuming Attempt 1; recruiting human participants; running any human study including baseline qualification, BlindVariancePilotV1, Attempt 1 and the human ablations; changing organism behaviour or the A000 FULL candidate; weakening the scripted baseline or the plus ten success floor; replacing human scores with model-generated scores; resuming R012 network or recovery expansion; fabricating model or provider credentials, reviewer executions, ethics approval or human evidence; exposing sealed pilot information; deleting the superseded human-review requirement or its candidate history without provenance; and creating D017.
- Acceptance: Twenty-seven criteria. All three role contracts exist with reviewer and operator authority separated; both reviewers execute in isolated contexts and neither can see the other's first-pass output; reviewer heterogeneity is machine-enforced and demonstrated; available provenance is durably recorded; rulings use a validated structured schema; parsing and failure behaviour fails closed; evidence injection fixtures hold; meta-evaluation fixtures hold at their frozen thresholds; repeated-trial stability and position and order sensitivity meet the frozen requirements; disagreement generates its blocking state; the study operator can neither adjudicate the gate nor fabricate human evidence; the pilot seal remains intact; the four hundred participant and two hundred and fifty hour ceiling is unchanged; attempts consumed remains zero of three; the programme state remains ALIVENESS_UNTESTED; human scored recruitment remains shut; the external ethics determination remains unresolved rather than fabricated; the superseded human-review and candidate history is preserved; existing qualification evidence remains valid or is explicitly requalified; governance validation passes; full CI passes on the pushed commit; no R003 through R009 implementation is introduced; and no real human data is collected.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016
- Status at issuance: ISSUED


## D-016-D

- Issued: 2026-08-15T00:05:00-04:00
- Issuer: User
- External directive: D016-D
- Objective: Complete the missing real-model qualification of AgenticReviewHarnessV1 by executing two genuinely heterogeneous agentic reviewers already authenticated on this workstation, using the accepted D016-C mechanics unchanged.
- Scope: Codex reviewer backend, Gemini reviewer backend, invocation isolation, tool-denial configuration, provenance capture, qualification runner, qualification evidence, tests, the A001 governance audit, the A001 gate state, .agent synchronization, and CI deterministic validation where possible.
- Exclusions: Baseline human qualification; participant recruitment; BlindVariancePilotV1 execution; Attempt 1; human ablation studies; R003 through R009; organism changes; A000 FULL changes; new infrastructure work; creating new API keys; exposing, printing, copying or committing credentials; changing billing, plans or subscriptions; tuning thresholds after seeing results; rewriting fixtures after seeing results; rerunning a failed formal qualification until it passes; substituting Claude into either adjudicating reviewer slot; beginning human A001 execution; and creating D017.
- Acceptance: Twenty criteria, all required. Real Codex and Gemini reviewer executions occurred; the pair is genuinely heterogeneous; both executed in isolated fresh sessions; Codex remained inside the frozen evidence boundary; the Google reviewer had all external tools denied; reviewers could not see each other's outputs; project-controlled provenance is complete; provider-hidden provenance is honestly marked unavailable; all frozen meta-evaluation cases were executed; both reviewers meet every frozen threshold; disagreement handling remains fail-closed; the operator remains non-adjudicating; no human evidence was generated; attempts remain zero of three; the programme remains ALIVENESS_UNTESTED; human recruitment remains blocked; governance validation passes; full relevant CI passes; and the exact CI head SHA equals the pushed qualification commit.
- Risk class: HIGH
- Relationship: resumes
- Related directive: D-016-C
- Status at issuance: ISSUED


## D-016-E

- Issued: 2026-08-15T05:30:00-04:00
- Issuer: User
- External directive: D016-E
- Objective: Replace assistant-product CLI reviewer access with direct model API calls where the project controls the complete tool surface, using the OpenAI Responses API for the primary slot and the Google Gemini API for the alternate.
- Scope: OpenAI Responses and Gemini generateContent reviewer backends; a mockable HTTP transport; request construction proven to carry no tools; request provenance covering provider, requested model, API version, tool surface, prompt hash, evidence hash, sanitized request hash, schema and parser versions, exposed parameters, response identifier, raw response hash, retries and final ruling; credential discovery by presence only; the A001 governance audit; the A001 gate state; qualification evidence; tests; .agent synchronization; and CI.
- Exclusions: Using Codex CLI, Gemini CLI, Antigravity, Claude Code, ChatGPT or any other assistant-product interface for the formal reviewers; creating accounts, API keys, billing, subscriptions or paid resources; printing, copying, committing or otherwise exposing secrets; showing formal scored fixtures to any model without both credentials present; lowering thresholds; tuning prompts; switching models; changing fixtures; rerunning a failed formal qualification until it passes; baseline human qualification; participant recruitment; BlindVariancePilotV1 execution; Attempt 1; human ablations; R003 through R009; and creating D017.
- Acceptance: Both API transports implemented and fully tested against mock transports; their serialized requests proven to contain no tools, with OpenAI additionally forcing tool_choice=none and Gemini declaring no tool fields at all; the D016-C role contracts, evidence isolation, ruling schema, disagreement behaviour, fixtures and frozen thresholds preserved unchanged; provider-hidden internals recorded as UNOBSERVABLE_PROVIDER_CONTROL_PLANE rather than fabricated; credentials inspected for but never created or exposed; and either the frozen qualification executed once unchanged when both credentials exist, or BLOCKED_PROVIDER_CREDENTIALS_UNAVAILABLE returned naming exactly which credentials are missing, with no formal scored fixture shown to any model.
- Risk class: HIGH
- Relationship: resumes
- Related directive: D-016-D
- Status at issuance: ISSUED


## D-016-F

- Issued: 2026-08-15T07:00:00-04:00
- Issuer: User
- External directive: D016-F
- Objective: Stop treating OpenAI-versus-Google provider diversity as a project requirement, route both isolated A001 reviewer roles through the owner's Paragon model router, and evaluate whether the resulting independent reviewer executions satisfy the frozen qualification criteria.
- Scope: A Paragon reviewer backend over the router's OpenAI-compatible request form; a minimal non-scored connectivity and protocol preflight; routing metadata capture; retirement of the provider-credential and provider or model-family diversity blockers; state derived from actual router reachability and qualification; the A001 governance audit; the A001 gate state; qualification evidence; tests; .agent synchronization; and CI.
- Exclusions: Redesigning the harness; exposing formal scored fixtures during protocol discovery; committing, logging, hashing, printing or persisting the router credential in the repository, in .agent, in Notion, in evidence, in CI or in shell profiles; bypassing Paragon to inspect its internal routing logic; modifying fixtures, expected results, thresholds, role prompts or parser behaviour after seeing formal results; rerunning a failed formal qualification until it passes; resolving reviewer disagreement automatically; participant recruitment; baseline human qualification; BlindVariancePilotV1 execution; Attempt 1; human ablations; R003 through R009; organism changes; A000 FULL changes; and creating D017.
- Acceptance: An exact PASS, BLOCKED or FAIL state; the Paragon integration reported with endpoint, requested model, protocol form, confirmation that the credential was not persisted, and tool-surface proof; per-slot reviewer executions with role contract, request hash, evidence hash, raw-response hash, routing metadata or PARAGON_ROUTING_UNOBSERVABLE, and structured ruling; full metric results against the unchanged frozen thresholds; proof that reviewer calls were independent and neither saw the other's ruling; the A001 state reported with audit totals, outstanding blockers, attempts consumed, programme state, recruitment state, human-data count and ethics state; all relevant validation run; and the result committed and pushed to main with the exact CI head SHA equal to the pushed commit.
- Risk class: HIGH
- Relationship: resumes
- Related directive: D-016-E
- Status at issuance: ISSUED


## D-016-G

- Issued: 2026-08-15T08:20:00-04:00
- Issuer: User
- External directive: D016-G
- Objective: Keep Paragon as the sole reviewer gateway for both A001 reviewer roles and move A001 review onto a Paragon route that performs plain model inference rather than delegating to a tool-enabled coding or assistant CLI, then execute the frozen qualification.
- Scope: Route and profile discovery on the owner's router; a Paragon plain-inference reviewer backend; a bounded non-scored routed-boundary preflight; the real-model qualification runner against the unchanged frozen thresholds; the A001 governance audit; the A001 gate state; qualification evidence; tests; .agent synchronization; and CI.
- Exclusions: Returning to direct OpenAI or Google APIs; requiring provider or model-family diversity; redesigning the A001 harness; bypassing Paragon; repeating filesystem jails, CLI sandboxing or further sandboxing investigations; exposing any frozen scored qualification fixture during route discovery or preflight; committing, printing, logging, hashing, storing in .agent, writing to Notion or saving in shell profiles the router credential; altering thresholds, fixtures, expected outcomes, role prompts, parser rules, evidence or qualification logic after observing formal results; rerunning a failed formal qualification until it passes; resolving reviewer disagreement automatically; participant recruitment; baseline human qualification; BlindVariancePilotV1 execution; Attempt 1; human ablations; R003 through R009; modifying A000 FULL; and creating D017.
- Acceptance: An exact PASS, BLOCKED or FAIL state; the Paragon route reported with route or profile used, request mode, whether routing metadata was observable and the routed model or provider if exposed; the routed-boundary proof reported from bounded non-scored shell, filesystem, repository and web probes; if the boundary passed, complete primary and alternate metrics, pair-level result, fixture-level failures and abstentions, order, position, injection and stability results and disagreement cases; the A001 state reported with audit totals, outstanding blockers, attempts consumed, programme state, recruitment state, human-data count and ethics state; all relevant validation run; and the result committed and pushed to main with the exact CI head SHA equal to the pushed commit.
- Risk class: HIGH
- Relationship: resumes
- Related directive: D-016-F
- Status at issuance: ISSUED


## D-016-H

- Issued: 2026-08-15T09:30:00-04:00
- Issuer: User
- External directive: D016-H
- Objective: Fix Paragon's per-model context metadata ingestion so the already-proven tool-free HTTP route accepts the reviewer request, then run the frozen real reviewer qualification once.
- Scope: Carrying the actual per-model context_length or equivalent from the provider model catalog into Paragon; a regression test for that ingestion; verification that the route accepts a synthetic reviewer-sized request; one execution of the frozen real reviewer qualification; the A001 governance audit; the A001 gate state; qualification evidence; tests; .agent synchronization; and CI.
- Exclusions: Faking a provider-wide context window; weakening the large-context safety gate; altering the review classifier for this project; reopening the previous provider, credential or sandbox work; exposing any frozen scored qualification fixture outside the single formal run; committing, printing, logging, hashing or persisting the router credential; altering thresholds, fixtures, expected outcomes, role prompts, parser rules or qualification logic after observing the formal result; rerunning a failed formal qualification until it passes; participant recruitment; baseline human qualification; BlindVariancePilotV1 execution; Attempt 1; human ablations; R003 through R009; modifying A000 FULL; and creating D017.
- Acceptance: The per-model context metadata is carried from the provider's own declaration rather than invented; the tool-free route accepts a synthetic reviewer-sized request; the frozen qualification is executed exactly once against unchanged thresholds; the result is preserved whether it passes or fails; the A001 state is reported with audit totals, outstanding blockers, attempts consumed, programme state, recruitment state, human-data count and ethics state; all relevant validation is run; and the result is committed and pushed to main with the exact CI head SHA equal to the pushed commit.
- Risk class: HIGH
- Relationship: resumes
- Related directive: D-016-G
- Status at issuance: ISSUED


## D-016-I

- Issued: 2026-08-15T11:20:00-04:00
- Issuer: User
- External directive: D016-I
- Objective: Convert A001 to deterministic gate adjudication with agentic adversarial audit, so that human evidence determines aliveness, frozen math determines PASS and FAIL, and agents audit without adjudicating.
- Scope: A deterministic A001GateAdjudicatorV1 built from the already-frozen protocol and thresholds; deterministic and replayable baseline qualification, pilot validity, feasibility, exclusions, multiplicity, Attempt 1, mechanism arms, three-attempt accounting and final A001 outcome; reclassification of the Paragon agentic reviewers as adversarial auditors; a closed machine-checkable violation vocabulary with ambiguous findings returned to the Architect; permanent preservation of the D016-H measurement as negative evidence; the A001 governance audit; the A001 gate state; qualification evidence; tests; .agent synchronization; and CI.
- Exclusions: Changing any existing A001 threshold; permitting an agent to create a PASS, create a FAIL, rescue a result or override the deterministic gate; tuning around the D016-H result or re-running it; reopening the provider, credential, sandbox, routing or provider-diversity work; collecting human data; recruiting participants; baseline human qualification; BlindVariancePilotV1 execution; Attempt 1; human ablations; R003 through R009; modifying A000 FULL; and creating D017.
- Acceptance: A deterministic adjudicator computing the full A001 outcome from one canonical evidence record with no clock, randomness, network, environment or model in the path; replayability proven over identical and reordered evidence; every existing A001 threshold unchanged and guarded against drift; the reviewers reclassified with gate-adjudicating authority forbidden to every role and refused by the role constructor; agent findings incapable of creating, rescuing or overriding an outcome, with concrete findings re-derived from the evidence and ambiguous ones returned to the Architect; the D016-H failure preserved and never reported as cleared; the A001 state reported with audit totals, outstanding blockers, attempts consumed, programme state, recruitment state, human-data count and ethics state; all relevant validation run; and the result committed and pushed to main with the exact CI head SHA equal to the pushed commit.
- Risk class: HIGH
- Relationship: resumes
- Related directive: D-016-H
- Status at issuance: ISSUED


## D-016-J

- Issued: 2026-08-16T02:25:00-04:00
- Issuer: User
- External directive: D016-J
- Objective: Encode the owner-delegated A001 ethics determination and correct the minimum participant protections required before baseline human-evidence recruitment.
- Scope: Ethics determination, U.S. adult eligibility, consent/privacy separation and retention, owner-contact enforcement, participant materials, A001 governance state, focused tests/evidence, `.agent/` synchronization and CI.
- Exclusions: IRB or exemption claims; changing thresholds, sample sizes, analysis, pilot seal, adjudicator rules, organism behavior, Paragon/model infrastructure, scored A001 recruitment, participant recruitment inside the coding task, or creating D017.
- Acceptance: Determination encoded without formal-approval claim; 18+ U.S.-adult self-consent eligibility enforced; prisoners and people lacking legally effective consent excluded; signed consent separated and retained three years after the A001 programme; obsolete independent-reviewer contact removed; real owner contact required before session; incomplete-disclosure authorization, debrief, payment/withdrawal and privacy protections preserved; scope-change triggers explicit; ethics stage accepted by `A001GateAdjudicatorV1`; the three substantive human-evidence blockers unchanged; scored recruitment blocked; attempts `0/3`; human data `0`; validation green; exact pushed SHA and CI recorded.
- Risk class: HIGH
- Relationship: resumes
- Related directive: D-016-I
- Status at issuance: ISSUED


## D-016-J-R1

- Issued: 2026-08-16T03:00:00-04:00
- Issuer: User
- External directive: D016-J-R1
- Objective: Close CI and participant-protection contradictions from D016-J without weakening the no-fabricated-human-evidence invariant.
- Scope: Remove the StudyOperator public-surface regression; replace exact numeric age and country values with an 18+ U.S.-adult eligibility attestation; synchronize consent, enrollment and study-data treatment across privacy, backup and incident controls; mark the obsolete independent-IRB posture as superseded history without claiming IRB approval; correct the D016-J outcome record; and verify green Governance plus Build/Test on the exact corrective SHA.
- Exclusions: No participants, models, Paragon, organism work, thresholds, sample sizes, analysis, pilot seal, reviewer adjudication or reviewer/model infrastructure changes; do not resolve BaselineIndependentOwner; do not issue D016-K.
- Acceptance: The operator boundary test passes without permitting fabricated human evidence; exact age and demographics are not collected; all three record classes remain separately controlled; old IRB language is historical and superseded; 900de18 is recorded as corrective-required rather than accepted; Governance and Build/Test are green on the exact pushed SHA; A001 remains ALIVENESS_UNTESTED with attempts 0/3 and human data 0.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-J
- Status at issuance: ISSUED


## D-016-K

- Issued: 2026-08-16T05:20:00-04:00
- Issuer: User
- External directive: D016-K
- Objective: Retire `BaselineIndependentOwner` entirely and freeze deterministic baseline qualification before any human data collection.
- Scope: A machine-checked freeze manifest covering the comparator, protocol, instrument, presentation, exclusions and analysis; deterministic adjudicator enforcement of its hash; current governance and study-protocol language; generated evidence; tests; `.agent/` synchronization and CI.
- Exclusions: No participants, recruitment, scored A001 sessions, human data, model or reviewer adjudication, Paragon, organism behavior, thresholds, sample sizes, statistical analysis changes, participant protections or D016-L.
- Acceptance: The frozen 40-person blinded `ScriptedPetBaselineV1` versus `DegradedScriptedControlV1` experiment is the sole qualification authority; PASS requires mean paired competence difference >= +15 and 95% CI lower bound > 0; no person, model, reviewer or override can replace it; Governance and Build/Test are green on the exact pushed SHA; A001 remains ALIVENESS_UNTESTED with attempts 0/3 and human data 0.
- Risk class: HIGH
- Relationship: resumes
- Related directive: D-016-J
- Status at issuance: ISSUED


## D-016-K-R1

- Issued: 2026-08-16T09:50:00-04:00
- Issuer: User
- External directive: D016-K-R1
- Objective: Synchronize retired baseline-owner semantics and frozen evidence, and bind the canonical manifest bytes to the gate hash.
- Scope: The stale A001StudyContractTest assertions; active versus historical protocol and onboarding prose; repository-wide semantic classification; the manifest SHA binding verifier; dependent generated evidence; `.agent/` synchronization and exact-SHA CI.
- Exclusions: No participants, recruitment, human data, scored A001 attempt, threshold/sample/power/statistics changes, organism/mechanism work, Paragon/model/reviewer architecture work, participant-protection changes, replacement authority or D016-L.
- Acceptance: Five unresolved real-world inputs and two unassigned reviewer roles are tested; BaselineIndependentOwner has no active authority; the manifest bytes hash exactly to the value consumed by A001GateAdjudicatorV1; generated evidence is produced by canonical generators; Governance and full Build/Test are green on the exact pushed SHA; A001 remains ALIVENESS_UNTESTED with attempts 0/3 and human data 0.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-K
- Status at issuance: ISSUED


## D-016-K-R2

- Issued: 2026-08-16T12:00:00-04:00
- Issuer: User
- External directive: D016-K-R2
- Objective: Make the frozen-baseline verifier portable in Linux CI and synchronize the live ethics state with D016-J.
- Scope: Replace the Windows-only Python launcher in the A001 activation workflow; correct `.agent/CURRENT.md`; append the truthful R1 CI failure and R2 state; and obtain exact-SHA CI.
- Exclusions: No baseline freeze manifest or hash changes; no threshold, sample-size, power, statistical-analysis, participant-protection, organism, reviewer/model, recruitment, human-data, A001-attempt or D016-L changes.
- Acceptance: The Linux workflow invokes an available Python 3 command; current records state that the D016-J owner-delegated determination exists and AJ-05 is satisfied while formal IRB approval, federal exemption and Common Rule/institutional coverage remain unclaimed or unestablished; the R1 failure remains preserved; Governance and Build/Test are green on the exact pushed SHA; and A001 remains ALIVENESS_UNTESTED with attempts 0/3 and human data 0.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-K-R1
- Status at issuance: ISSUED


## D-016-K-R3

- Issued: 2026-08-16T16:30:00-04:00
- Issuer: User
- External directive: D016-K-R3
- Objective: Canonicalize cross-platform baseline freeze bytes and remove residual comparator-protocol drift without changing A001 science or architecture.
- Scope: Prove the local-versus-CI byte/EOL mismatch; establish and machine-check a UTF-8/LF canonical representation for every pinned freeze text file and the manifest hash; correct stale ScriptedPetBaselineV1 qualification and freeze prose; recompute affected hashes and dependent evidence; synchronize `.agent/` records; and obtain exact-SHA CI.
- Exclusions: No comparator behavior, control behavior, 40-person design, blinding, randomization, session duration, instrument, thresholds, exclusions, analysis, participant protections, ethics determination, owner ceilings, A001 thresholds or attempt budget, organism mechanisms, auditor/model architecture, recruitment, participants, human data, scored attempt or D016-L.
- Acceptance: The byte mismatch cause is proven; canonical freeze bytes verify identically across Windows and Linux; no active pre-D016-K owner/reviewer semantics remain; freeze and manifest-to-gate verification pass; Governance and full Build/Test are green on the exact pushed SHA; A001 remains ALIVENESS_UNTESTED with attempts 0/3, human data 0 and recruitment blocked.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-K-R2
- Status at issuance: ISSUED


## D-016-K-R4

- Issued: 2026-08-16T18:30:00-04:00
- Issuer: User
- External directive: D016-K-R4
- Objective: Diagnose and stabilize A001 generated-evidence byte identity after the same exact-SHA R3 failure reproduced twice.
- Scope: Prove the Git-object versus working-tree and encoding transformation for A001_ACTIVATION_DRY_RUN.txt; inspect effective Git attributes, filters and EOL configuration; correct only the proven generated-evidence encoding defect; verify adjacent generated artifacts; preserve the R3 canonical freeze; synchronize `.agent/` records; and obtain exact-SHA CI.
- Exclusions: No baseline science, comparator or control behavior, study design, thresholds, exclusions, analysis, protections, ethics determination, owner ceilings, attempt accounting, organism, reviewer/model architecture, recruitment, participants, human data, scored attempt, D016-L or unrelated cleanup.
- Acceptance: The transformation cause is demonstrated; the correction makes A001 generated evidence byte-identical on Linux; baseline coverage and gate evidence regenerate identically; the freeze and manifest binding remain valid; Governance and full Build/Test are green on the exact pushed SHA; and A001 remains ALIVENESS_UNTESTED with attempts 0/3, human data 0 and recruitment blocked.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-K-R3
- Status at issuance: ISSUED


## D-016-L

- Issued: 2026-08-16T20:00:00-04:00
- Issuer: User
- External directive: D016-L
- Objective: Rebase active A001 execution to AI-agent qualification followed by one owner-only real-device Pixel acceptance gate.
- Scope: A001EvaluationContractV2, AgentObservationHarnessV1, frozen 12-pair/24-execution panel, deterministic aggregation, synthetic adversarial fixtures, OwnerPixelAlivenessAcceptanceV1, active governance reconciliation and generated dry-run evidence.
- Exclusions: No formal AI evaluation, Pixel review, external human participants, recruitment, consent, compensation, human-population inference, organism behavior, comparator behavior, D016-H rerun, reviewer bureaucracy or R003-R009 implementation.
- Acceptance: V2 contract and boundary are machine-visible; the panel, rubric, validity and aggregation thresholds are frozen; owner FAIL cannot be overridden; generated state reports zero executions/reviews/participants and R003-R009 blocked; exact-SHA Governance and Build/Test are green.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-K-R4
- Status at issuance: ISSUED


## D-016-M-R1

- Issued: 2026-08-16T21:00:00-04:00
- Issuer: User
- External directive: D016-M-R1
- Objective: Build and freeze the formal A001 V2 observation and execution substrate without making AI model calls.
- Scope: A faithful outward-observation generator from the existing ViewerSession presentation surface; frozen A001ObservationProtocolV1; 12 calibration and 12 qualification neutral bundles; manifest and evaluator-instruction hashes; strict formal runner, response parser and one-shot slot ledger; offline preflight evidence; tests; governance synchronization and CI.
- Exclusions: Any AI/network/model call; formal calibration or FULL qualification; Pixel review; human recruitment or data; organism/comparator behavior; V2 thresholds, rubric, validity rules or panel size; D016-H rerun; provider additions; R003-R009; unrelated cleanup; modification of the pre-existing `.gitignore` change.
- Acceptance: The outward source is documented; exactly 24 matched neutral bundles are reproducible and leakage-tested; A/B reversal is a rendering-only transformation; the formal manifest and instruction hash are reproducible; the runner refuses arbitrary/unhashed data and malformed responses and preserves one-shot slot history; preflight makes zero model calls; D016-M remains blocked pending formal execution; zero AI/Pixel/human activity; exact-SHA Governance and Build/Test green.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-L
- Status at issuance: ISSUED


## D-016-M

- Issued: 2026-08-17T00:50:00-04:00
- Issuer: User
- External directive: D016-M
- Objective: Execute the frozen A001 V2 AI qualification and stop at the resulting deterministic decision.
- Scope: Run the accepted calibration panel, apply its frozen validity and aggregation rules, run the isolated FULL qualification panel only if calibration passes, preserve every raw and normalized slot result, and record the formal outcome and provenance.
- Exclusions: No evaluator, rubric, bundle, threshold, panel-size, aggregator, organism, comparator, provider or runner changes; no selective reruns, replacement answers or manual repair; no Pixel review, deployment, human recruitment, human participants, R003-R009 or modification of the pre-existing `.gitignore` change.
- Acceptance: The frozen manifest is reverified; calibration and conditional FULL execution follow the 24-slot counterbalanced plan; every slot is preserved; the deterministic result is one of the frozen D016-M end states; exact-SHA Governance and Build/Test are green; and Pixel reviews and external human participants remain zero.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-M-R1
- Status at issuance: ISSUED


## D-016-N

- Issued: 2026-08-17T01:30:00-04:00
- Issuer: User
- External directive: D016-N
- Objective: Restore salient-event interruptibility and first-contact agency after the valid D016-M AI qualification failure.
- Scope: Correct D009 sleep semantics; add bounded pending stimuli; route normalized interaction kinds to awareness responses; interrupt voluntary commitments lawfully; add state-dependent agency fixtures; preserve the D016-M failure and produce an offline 12-case cold-encounter diagnostic.
- Exclusions: No evaluator, rubric, prompt, threshold, panel, baseline, D016-M evidence, AI call, Pixel review, human participant, recruitment, R003-R009, or unrelated architecture change.
- Acceptance: All D016-N organism and regression fixtures pass; the diagnostic reports next-tick visible acknowledgement for salient inputs, bounded stimulus storage and lifetime, fresh rest and sleep occupancy of zero across 96 ticks, zero AI/Pixel/human activity, and exact-SHA Governance and Build/Test are green.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-M
- Status at issuance: ISSUED


## D-016-O

- Issued: 2026-08-17T02:30:00-04:00
- Issuer: User
- External directive: D016-O
- Objective: Freeze the materially changed D016-N FULL organism as `A001_FULL_D016N_V1` under the exact unchanged A001 V2 evaluation protocol for a future requalification.
- Scope: Preserve D016-M evidence; regenerate the 12 calibration and 12 D016-N FULL neutral observation bundles from candidate SHA `684579130bef5c820f3db9534ffb744654ebf3b4`; require byte-identical calibration and materially different FULL bundles; create a versioned formal-input manifest with candidate/source hashes; run offline preflight; and verify exact-SHA Governance and Build/Test.
- Exclusions: No AI/model/network call, formal scoring, Pixel review, organism/comparator/evaluator/protocol/rubric/threshold/panel/aggregator change, D016-M overwrite, human recruitment, participants, R003-R009, or modification of the pre-existing `.gitignore` change. Stop after D016-O.
- Acceptance: Candidate identity and source hashes are bound; D016-M manifest/bundles remain untouched; the new namespace is reproducible; calibration matches D016-M; revised FULL differs; evaluator instruction SHA and protocol are unchanged; offline preflight reports zero AI/Pixel/human activity and R003-R009 blocked; exact-SHA Governance and Build/Test are green.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-N
- Status at issuance: ISSUED


## D-016-P

- Issued: 2026-08-17T07:55:00-04:00
- Issuer: User
- External directive: D016-P
- Objective: Perform one frozen A001 V2 requalification for A001_FULL_D016N_V1 and follow the deterministic calibration-gated FAIL or PASS branch through its authorized terminal state.
- Scope: Verify D016-O readiness; execute exactly 24 OpenAI gpt-5 calibration calls; execute exactly 24 additional FULL calls only after calibration PASS; preserve raw and normalized evidence, immutable slot provenance and deterministic aggregates; on AI FAIL or INVALID produce the bounded postmortem and stop; on AI PASS preserve a formal checkpoint, build the separate research-only Pixel host, validate and optionally stage only the exact CI APK, and stop at the owner verdict boundary.
- Exclusions: No evaluator, provider, model, prompt, observation bundle, rubric, threshold, aggregator, candidate-source or production android-host change; no selective reruns, replacement answers, human recruitment, participant study, automated owner interaction or verdict, Gemini, Paragon, paid service, or R003-R009 implementation. The pre-existing `.gitignore` modification remains untouched and uncommitted.
- Acceptance: D016-O bundle and candidate hashes remain bound; calibration is valid and meets its frozen rule before FULL; total formal calls are at most 48; every raw response is preserved before normalization; exact-SHA Governance and Build/Test are green; and the reached branch's terminal state is truthfully recorded.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-O
- Status at issuance: ISSUED


## D-016-Q

- Issued: 2026-08-17T09:10:00-04:00
- Issuer: User
- External directive: D016-Q
- Objective: Perform one fresh A001 V2 requalification of the unchanged D016-O candidate, using a capacity sentinel first and following the authorized calibration, FULL, research-only Pixel-host, and owner-verdict branches.
- Scope: Preserve D016-M/D016-O/D016-P history; use a fresh d016-q namespace and fresh slots; execute at most one non-formal capacity sentinel, 24 fresh calibration calls, and 24 fresh FULL calls only after calibration PASS; preserve raw evidence and deterministic aggregates; build and optionally stage the research-only owner Pixel host only after AI PASS.
- Exclusions: No D016-P output reuse, evaluator/rubric/threshold/protocol/model/provider/candidate change, selective rerun, replacement answer, organism modification during scoring, human recruitment, paid capacity purchase, Gemini, Paragon, production persistence, production R003-R009 implementation, destructive Pixel troubleshooting, automated owner interaction or verdict, or modification of the pre-existing `.gitignore`.
- Acceptance: Starting SHA and frozen hashes remain bound; the capacity sentinel precedes formal execution; fresh formal evidence reaches a truthful terminal branch; exact-SHA Governance and Build/Test are green for terminal evidence; the project stops at the furthest authorized state.
- Risk class: HIGH
- Relationship: amends
- Related directive: D-016-P
- Status at issuance: ISSUED
