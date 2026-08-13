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

