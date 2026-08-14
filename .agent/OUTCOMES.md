# Project Outcome Ledger

This append-only ledger records results for project directives. Every live outcome must reference one local directive ID.

## Entry schema after adoption

Use live outcome headings only after adoption. The following schema is instructional and is not a live entry:

```markdown
## <local-directive-id> - <outcome-state>

- Outcome ID: <unique outcome record ID>
- Supersedes outcome: <outcome ID or none>
- Closed: <ISO-8601 timestamp with timezone>
- Acceptance: <MET | PARTIAL | NOT MET>
- Summary: <concise result>
- Changed areas: <paths or none>
- Validation:
  - <command or check> - <PASSED | FAILED | NOT RUN | NOT APPLICABLE | BLOCKED>
- Remaining risks: <risks or none>
- Blockers: <blockers or none>
- Follow-up directive: <ID or none>
```

Allowed adopted-project outcome states: `COMPLETE`, `PARTIAL`, `BLOCKED`, `FAILED`, `CANCELLED`, `SUPERSEDED`. Do not rewrite earlier entries; append corrections referencing the original.

## D-001 - FAILED

- Outcome ID: O-0001
- Supersedes outcome: none
- Closed: 2026-08-12T19:43:05-04:00
- Acceptance: NOT MET
- Summary: D-001 was executed nonconformingly. Its acceptance condition required a report-only response with no repository change, but the same session also modified .agent/PROJECT_GOAL.md to adopt the charter. The adopted goal file is factually correct and is retained deliberately, and the deviation was disclosed in the D-001 report, so the directive is recorded as nonconforming rather than as a clean execution. This closure entry is retrospective and was written during D-002.
- Changed areas: .agent/PROJECT_GOAL.md
- Validation:
  - governance validator in adopted mode during D-001 - NOT RUN
  - targeted check that the validator reported no PROJECT_GOAL.md error during D-001 - PASSED
- Remaining risks: The charter was recorded in the repository under a directive that prohibited repository change, so the governance history must retain this deviation permanently rather than presenting D-001 as compliant.
- Blockers: none
- Follow-up directive: D-002

## D-002 - COMPLETE

- Outcome ID: O-0002
- Supersedes outcome: none
- Closed: 2026-08-12T19:43:05-04:00
- Acceptance: MET
- Summary: Every governance file required by the repository validator was adopted with truthful project facts, the recorded gate state matches the canonical R000 state, the D-001 nonconformance is preserved, and no non-governance file was changed.
- Changed areas: .agent/PROJECT_PROFILE.md, .agent/CURRENT.md, .agent/DIRECTIVES.md, .agent/OUTCOMES.md, .agent/LEARNINGS.md, .agent/RECORD.md, .agent/REPO_MAP.md
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - FAILED
  - manual review of the changed governance files against the canonical charter and the observed repository contents - PASSED
- Remaining risks: The shipped validator self-test asserts that this repository is a clean unadopted fixture, so it raises an assertion error now that the repository is adopted. Repairing it would be a script change outside the D-002 scope. See L-0002.
- Blockers: none
- Follow-up directive: none

## D-003 - COMPLETE

- Outcome ID: O-0003
- Supersedes outcome: none
- Closed: 2026-08-12T19:55:43-04:00
- Acceptance: MET
- Summary: The repository was initialized as a local Git repository on branch main with a baseline commit that captures the accepted post-D002 governance state plus the governance-tooling correction, and the validator self-test was repaired to build every mutable state from a pristine fixture instead of assuming the live governance files are unadopted. The production validator was not modified.
- Changed areas: .gitignore, scripts/test_validate_governance.py, scripts/fixtures/governance_template/, .agent/DIRECTIVES.md, .agent/OUTCOMES.md, .agent/RECORD.md, .agent/LEARNINGS.md, .agent/CURRENT.md, .agent/PROJECT_PROFILE.md, .agent/REPO_MAP.md
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - git status --short after the final commit reporting a clean worktree - PASSED
- Remaining risks: The repository has no remote, so the baseline exists on one machine only until a later directive resolves remote hosting.
- Blockers: none
- Follow-up directive: none

## D-004 - COMPLETE

- Outcome ID: O-0004
- Supersedes outcome: none
- Closed: 2026-08-12T20:31:27-04:00
- Acceptance: MET
- Summary: The greenfield R000 project skeleton was built. ProjectIdentityBuildContractV1 was frozen from the architect values and is enforced by tools/verify_project_identity.py. The Implementation Plan E2E module topology was created with core-math, core-crypto and core-state as pure Kotlin JVM modules, desktop-runner as a headless JVM runner and android-host as the only Android-linked module. All seven mandatory R000 registries exist as versioned scaffolds with no invented organism entries. A GitHub Actions workflow runs governance validation, the governance self-test, the identity check, module tests, the full build and the Android debug assembly, and it passed on a hosted runner. The work is pushed to the authoritative remote on branch main. No organism behavior, DeterminismContractV1 or R001 algorithm was implemented.
- Changed areas: settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/, gradlew, core-math/, core-crypto/, core-state/, desktop-runner/, android-host/, docs/, governance/, qualification/, tools/, .github/workflows/ci.yml, .gitignore, LICENSE, scripts/test_validate_governance.py, .agent/DIRECTIVES.md, .agent/OUTCOMES.md, .agent/RECORD.md, .agent/LEARNINGS.md, .agent/CURRENT.md, .agent/PROJECT_PROFILE.md, .agent/REPO_MAP.md
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py - PASSED
  - ./gradlew build covering all five modules and fifteen tests - PASSED
  - ./gradlew :desktop-runner:run headless execution - PASSED
  - ./gradlew :android-host:assembleDebug producing a debug APK - PASSED
  - GitHub Actions run 31654374955 on branch main, both jobs - PASSED
- Remaining risks: The Android shell has never been launched, because no device or emulator is available on the build host, so the R000 exit gate in Implementation Plan E2E stays open along with device budgets and a hashed CI evidence bundle. The repository is public and carries an MIT LICENSE created by the owner, which contradicts the proprietary no-redistribution clause the architect froze in the same directive; that conflict is recorded and left for the architect.
- Blockers: none
- Follow-up directive: none

## D-005 - COMPLETE

- Outcome ID: O-0005
- Supersedes outcome: none
- Closed: 2026-08-13T01:20:00-04:00
- Acceptance: MET
- Summary: R000 is closed as PASS. The inherited MIT licence was replaced with a proprietary all-rights-reserved notice matching the frozen contract. The Android shell was installed and actually launched on physical Tensor hardware, a Pixel 9 Pro XL on Android 16, with all six expected shell strings verified on screen through the accessibility tree, and with a clean terminate and relaunch and no crash attributed to the package. Measured runtime observations were recorded as R000_MEASURED_BASELINE and every future production budget was left explicitly NOT ESTABLISHED. A hashed qualification bundle binding thirty-seven constituents was produced and is verified in CI. Both the Implementation Plan E2E gate and the charter gate were evaluated criterion by criterion and all criteria passed. No organism behavior, DeterminismContractV1 or R001 algorithm was implemented.
- Changed areas: LICENSE, docs/architecture/ProjectIdentityBuildContractV1.md, docs/decisions/DECISION_LOG.md, docs/release/DEVICE_AND_RESOURCE_BUDGETS.md, governance/release-gates/R000_EXIT_GATE.md, governance/qualification/QUALIFICATION_EVIDENCE_INDEX.md, governance/qualification/R000_QUALIFICATION_BUNDLE.md, governance/source-provenance/DEPENDENCY_LICENSE_INVENTORY.md, governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md, qualification/evidence/R000/, qualification/device-matrix/R000/, tools/qualify_r000_android.sh, tools/build_qualification_bundle.py, .github/workflows/ci.yml, .agent/DIRECTIVES.md, .agent/OUTCOMES.md, .agent/RECORD.md, .agent/LEARNINGS.md, .agent/CURRENT.md, .agent/PROJECT_PROFILE.md, .agent/REPO_MAP.md
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering thirty-seven constituents - PASSED
  - ./gradlew clean build covering all five modules and fifteen tests - PASSED
  - ./gradlew :desktop-runner:run headless execution - PASSED
  - ./gradlew :android-host:assembleDebug producing a byte-reproducible debug APK - PASSED
  - tools/qualify_r000_android.sh on a physical Pixel 9 Pro XL covering install, cold launch, visible state, terminate and relaunch - PASSED
- Remaining risks: The x86 Android emulator could not complete a run because the android-37.0 image crashes surfaceflinger on this host under all three rendering backends. R000 does not require it, but the canonical determinism matrix does require it for R001, so it must be resolved before R001 closes. The repository remains public while carrying a proprietary licence, because D005 excluded changing visibility, and an MIT grant was published for the earlier revisions. The R000 shell draws under the status bar, which is cosmetic and was left unfixed because presentation is governed by the empty PresentationContractCatalog.
- Blockers: none
- Follow-up directive: none

## D-006 - COMPLETE

- Outcome ID: O-0006
- Supersedes outcome: none
- Closed: 2026-08-13T10:40:00-04:00
- Acceptance: MET
- Summary: R001 is closed as PASS, with one exit-gate criterion resting on an architect waiver recorded under remaining risks. DeterminismContractV1 version 1 was frozen before any dependent implementation was written, fixing canonical byte order, integer widths and fixed-width length prefixes, boolean and enum encoding, collection ordering, an ASCII-only canonical identifier policy, the SHA-256 state hash with domain separation, counter-based SplitMix64 randomness with domain-separated substream derivation, the fixed-point scale and half-away-from-zero rounding, lookup-table generation and digest verification, and the migration policy. The deterministic core was implemented across core-crypto, core-math and core-state and covered by ninety-seven JVM tests plus five Android instrumented tests. Identical fixtures produced the byte-identical evidence digest 54bc044740a4c05b41b509a7160bff559e09421f2eaa55dc36c3d3ffadc1bd86 on the desktop JVM reference runner, on the x86_64 Android emulator and on Tensor G4 hardware, spanning both a HotSpot-to-ART boundary and an x86-to-arm64 boundary. All three canonical R001 exit gates were evaluated criterion by criterion and every criterion passed. No organism behavior was implemented.
- Changed areas: docs/architecture/DeterminismContractV1.md, docs/architecture/CANONICAL_SOURCES.md, docs/architecture/registries/, docs/decisions/DECISION_LOG.md, docs/invariants/INVARIANT_REGISTRY.md, docs/release/DEVICE_AND_RESOURCE_BUDGETS.md, core-crypto/, core-math/, core-state/, desktop-runner/, android-host/, governance/release-gates/R001_EXIT_GATE.md, governance/qualification/R001_QUALIFICATION_BUNDLE.md, governance/source-provenance/, qualification/, tools/generate_lookup_tables.py, tools/build_qualification_bundle.py, tools/qualify_r001_determinism.sh, gradle/libs.versions.toml, .github/workflows/ci.yml, .agent/
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering R000 at its pinned commit and R001 with fifty-five constituents - PASSED
  - python3 tools/generate_lookup_tables.py --check - PASSED
  - ./gradlew clean build covering all five modules and ninety-seven JVM tests - PASSED
  - ./gradlew :desktop-runner:run reproducing the frozen golden digest - PASSED
  - tools/qualify_r001_determinism.sh on the x86_64 Android emulator - PASSED
  - tools/qualify_r001_determinism.sh on a physical Pixel 9 Pro XL with Tensor G4 - PASSED
- Remaining risks: The Snapdragon row of the canonical determinism matrix was waived by the architect during execution and is recorded as WAIVED BY ARCHITECT rather than PASS. The canonical architecture page still lists Snapdragon as required without the when-available qualifier that Exynos carries, so the matrix and the specification disagree until the canonical page is amended; that amendment is the architect's act. No Snapdragon or Exynos evidence exists in either direction. The panic-witness attempt deadline, the Class O batching cadence and the maximum tolerated uncommitted window are all deliberately NOT ESTABLISHED, because each would be a commitment derived from a healthy-process benchmark or from persistence semantics that belong to R002. R001's durable medium is an in-process append-only byte log rather than a database, which is sufficient for every R001 invariant but means storage-layer failure modes are untested. The repository remains public while carrying a proprietary licence.
- Blockers: none
- Follow-up directive: none


## D-007 - COMPLETE

- Outcome ID: O-0007
- Supersedes outcome: none
- Closed: 2026-08-13T13:20:00-04:00
- Acceptance: MET
- Summary: R002 is closed as PASS against both canonical exit gates. ContinuityDurabilityContractV1 version 1 was frozen in its own commit before any dependent implementation existed, fixing the four-clock model and canonical duration unit, the durable anchor layout, time-confidence classification and the clock anomaly rule, blind-decay credit with a carried replenishment remainder, the unresolved-time debt cap and safety floor and hysteresis and forgiveness horizon, the offline mode boundaries and chunk schedule and per-chunk ordering, journal generations and compaction, the durability admission states with normative entry and exit orderings, the platform protection states with a single-attempt suspend ordering, the TEMPORAL_DESYNC presentation rules, the encrypted-record boundary with derived nonces, and the restart and version-boundary rules. A new pure Kotlin module core-continuity implements all of it, together with a project-owned RFC 8439 ChaCha20-Poly1305 in core-crypto. One hundred and eighty-nine JVM tests and nine Android instrumented tests pass. Twenty-four continuity fixtures, including the full failure and exploit matrix, produced the byte-identical evidence digest 556bbe49df16595f748a487f78a17a83866eb2a018814f69ee469d7976d58d21 on the desktop JVM reference runner, on the x86_64 Android emulator and on Tensor G4 hardware, and every one of those targets reproduced the unchanged R001 digest in the same instrumented session. No organism behavior was implemented; the two reserves are neutral R002 fixtures.
- Changed areas: docs/architecture/ContinuityDurabilityContractV1.md, docs/architecture/CANONICAL_SOURCES.md, docs/architecture/registries/, docs/decisions/DECISION_LOG.md, docs/invariants/INVARIANT_REGISTRY.md, docs/release/DEVICE_AND_RESOURCE_BUDGETS.md, core-continuity/, core-crypto/, desktop-runner/, android-host/, governance/release-gates/, governance/qualification/, governance/source-provenance/, qualification/, tools/build_qualification_bundle.py, tools/qualify_r002_continuity.sh, tools/verify_project_identity.py, settings.gradle.kts, .github/workflows/ci.yml, .agent/
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py covering six modules - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering R000 and R001 at their pinned commits and R002 with fifty-eight constituents - PASSED
  - python3 tools/generate_lookup_tables.py --check - PASSED
  - ./gradlew clean build covering six modules and one hundred and eighty-nine JVM tests - PASSED
  - ./gradlew :desktop-runner:run reproducing both frozen golden digests - PASSED
  - tools/qualify_r002_continuity.sh on the x86_64 Android emulator - PASSED
  - tools/qualify_r002_continuity.sh on a physical Pixel 9 Pro XL with Tensor G4 - PASSED
- Remaining risks: Two Implementation Plan E2E work packages are not complete and the reasons are canonical rather than unfinished work. R002.5 and R002.10 prepared-rest semantics are written entirely in terms of exhaustion, recovery and contradiction conditions, which are species physiology gated behind A001; the durability machinery they depend on is implemented and qualified but the biology is deferred. R002.12 recovery cryptography and the storage provider are recorded as BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY and BLOCKED_SPEC_RECOVERY_PROVIDER, because Implementation Plan E2E requires RecoveryCryptographyContractV1 to be frozen first and D007 authorized freezing ContinuityDurabilityContractV1 only. D007 acceptance criterion three requires every work package to be complete while D007's own enumerated scope names none of those packages; that conflict is recorded in the gate record and is the architect's to resolve. Real storage failure modes remain untested because the durable medium is an in-process byte log with fault injection rather than a database. Android backup exclusion is asserted structurally rather than exercised through the platform transport. No frame-time measurement exists because no organism rendering is wired. Exynos remains canonically conditional and Snapdragon optional, with no evidence either way. The repository remains public while carrying a proprietary licence.
- Blockers: none
- Follow-up directive: none

## D-008 - COMPLETE

- Outcome ID: O-0008
- Supersedes outcome: none
- Closed: 2026-08-13T18:05:00-04:00
- Acceptance: MET
- Summary: The A000 aliveness spike is built, isolated and evidenced. Four disposable Gradle modules under research/aliveness-spike depend only on the frozen R001 fixed-point library, and a source-level isolation test enforces that because a Gradle declaration cannot: core-math publishes core-crypto as api, so the import list of every spike source is what is actually checked. The accelerated simulator runs twenty-two named fixtures over roughly two thousand nine hundred virtual days and reproduces the evidence digest 4765e6d587347688841d34c95b5b9caede8cbf44084335302e1475c7aeaa8fc9. Seventeen findings held and five did not, and all five are preserved with their configurations rather than tuned away. The real-time Swing viewer runs blinded fixed-duration sessions with a normalized study event log, and a headless test asserts that no session accessor exposes its cohort and that every cohort shares one label, one duration and one tick rate. All six cohorts render through SpikeExpressionContractV1, which has no cohort parameter. SpikeDecisionTraceV1 is emitted for every scored autonomous action, and exact Shapley attribution over six frozen mechanism groups classified one thousand three hundred and sixty-three scored spontaneous actions with a substantive rate of 0.946 against a 0.700 floor and an oscillator or tie-break only rate of 0.010 against a 0.200 ceiling. The CuriosityBalanceEnvelopeV1 joint feasibility search evaluated twenty-seven grid points on a four-seed matrix, taking both readouts from the same run at each point, and returned EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE with zero feasible points. Fifteen A000 and A001 research contracts are written with exact status: nine FROZEN, three READY_FOR_HUMAN_EVIDENCE, and three blocked on named missing inputs. No human value was fabricated.
- Changed areas: research/aliveness-spike/, settings.gradle.kts, governance/release-gates/A000_EXIT_GATE.md, governance/qualification/, docs/decisions/DECISION_LOG.md, docs/invariants/INVARIANT_REGISTRY.md, docs/architecture/CANONICAL_SOURCES.md, qualification/fixtures/A000/, qualification/longitudinal/A000/, qualification/evidence/A000/, tools/build_qualification_bundle.py, .github/workflows/ci.yml, .agent/
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py covering six production modules - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering R000 and R001 and R002 at their pinned commits and A000 live - PASSED
  - python3 tools/generate_lookup_tables.py --check - PASSED
  - ./gradlew clean build covering ten modules and two hundred and thirty-three JVM tests - PASSED
  - ./gradlew :desktop-runner:run reproducing both frozen production digests - PASSED
  - ./gradlew :research:aliveness-spike:accelerated-sim:run reproducing the frozen A000 digest - PASSED
  - ./gradlew :research:aliveness-spike:analysis:run producing the envelope feasibility result - PASSED
- Remaining risks: The joint curiosity envelope feasible set is empty at every grid point, failing on exactly two of five anti-convergence criteria while attribution passes everywhere, and only the independent gate reviewer may decide between the threshold-revision and mechanism-revision paths. IndependentReviewRosterV1 is BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED on all three roles, which blocks A001 outright. The scripted baseline is implemented and frozen but its human competence qualification has not been run and is not claimed. The population partially converges toward a common long-run policy and the closest pair of organisms is barely distinguishable. Removing episodic history increased history dependence rather than reducing it. Devaluation works but switching to the alternative does not follow from it. Over a short window the scripted baseline produces far more surface variety than FULL, so the primary comparison will not be an easy win. The habitat is abstract with twelve affordances and no space, sensors or navigation. Cycle regularity and single-action occupancy are constructs invented for this track and the envelope result turns on both.
- Blockers: A001 cannot begin. Two blockers are structural rather than technical: the reviewer roster is unassigned, and the empty feasible set requires the reviewer's determination.
- Follow-up directive: none

## D-009 - COMPLETE

- Outcome ID: O-0009
- Supersedes outcome: none
- Closed: 2026-08-13T20:40:00-04:00
- Acceptance: MET
- Summary: All four accepted D008 failures are resolved under unchanged curiosity thresholds, and the programme's single allowed threshold-only revision remains unspent. Mean population differentiation rose from 0.052 to 0.163, the closest pair from 0.019 to 0.074, final-window diversity from 0.034 to 0.103 and matched-stimulus history divergence from 0.325 to 0.610. A controlled reversal protocol shows a rejected food source re-sampled twenty ticks after the contingency flipped, rising from 0.13 to 32.5 uses a day while the devalued source fell from 31.1 to zero. The episodic mechanism was revised into a context-conditioned salience-retained form, given circadian habitat structure to learn from, measured across a five-seed matrix, and then removed from FULL because it still did not contribute; the ablation family is now two arms and the cohort is retained inverted so the negative result stays reproducible. The joint curiosity envelope returns NON_EMPTY_FEASIBLE_REGION with twenty-seven of twenty-seven grid points feasible and robust, against zero of twenty-seven under D008, on the identical grid, seed matrix, fixture and thresholds. All twenty-four accelerated findings hold. The two scripted comparators and the presentation contract are unchanged and the baseline is within noise of its D008 measures.
- Changed areas: research/aliveness-spike/, governance/release-gates/A000_EXIT_GATE.md, governance/qualification/, docs/decisions/DECISION_LOG.md, docs/invariants/INVARIANT_REGISTRY.md, qualification/fixtures/A000/, qualification/longitudinal/A000/, qualification/evidence/A000/, tools/build_qualification_bundle.py, .agent/
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py covering six production modules - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering R000 and R001 and R002 at their pinned commits and A000 live - PASSED
  - python3 tools/generate_lookup_tables.py --check - PASSED
  - ./gradlew clean build covering ten modules - PASSED
  - ./gradlew :desktop-runner:run reproducing both frozen production digests - PASSED
  - ./gradlew :research:aliveness-spike:accelerated-sim:run reproducing the revised A000 digest - PASSED
  - ./gradlew :research:aliveness-spike:analysis:run returning NON_EMPTY_FEASIBLE_REGION - PASSED
- Remaining risks: A001 remains blocked because IndependentReviewRosterV1 names nobody in any of its three roles, which no amount of code resolves. The human leave-one-out family is now two arms rather than three, and choosing a replacement third arm requires a new preregistered plan. The scripted baseline still has no human competence qualification. Substantive spontaneity attribution fell from 0.946 to 0.877 while staying well clear of its 0.700 floor, which is the expected direction for an organism spending far more time in genuinely optional behaviour but is worth watching. Two learning laws and one memory mechanism were changed on the strength of accelerated measures alone, with no human data. The habitat remains abstract with twelve affordances and no space, navigation or sensors, and now carries circadian structure that was added specifically to make a mechanism testable. Cycle regularity and single-action occupancy remain constructs invented for this track, and the envelope result has now turned on both in both directions.
- Blockers: A001 cannot begin while the reviewer roster is unassigned.
- Follow-up directive: none

## D-010 - COMPLETE

- Outcome ID: O-0010
- Supersedes outcome: none
- Closed: 2026-08-13T23:40:00-04:00
- Acceptance: MET
- Summary: The A001 Attempt-1 activation package is complete and no human outcome data was collected. AlivenessStudyProtocolV1 and GradedAlivenessInstrumentV1 are frozen with exact participant-facing wording, five anchors and the two-part decision rule of a mean paired difference of at least ten points together with a two-sided ninety-five percent confidence interval lower bound above zero. The human ablation family is three arms under Holm-Bonferroni at family-wise error rate 0.05, the third being the architect-preregistered FULL minus outcome uncertainty and directed re-exploration. BaselineQualificationProtocolV1 is powered at forty participants with a fifteen-point competence margin and is accompanied by a coverage manifest generated from the comparator implementation itself. BlindVariancePilotV1 is operationally ready at thirty-six analysable pairs with an information barrier enforced by type visibility, proven by reflecting over the released type and by showing that two synthetic pilots with opposite outcomes release byte-identical output. A001FeasibilityBudgetV1 computes the powered sample from exact noncentral-t power, inflates the pilot standard deviation by the frozen factor, powers each ablation arm at the corrected alpha divided by three, and returns a blocking state rather than a number when either the standard deviation or the owner ceiling is missing. A001AnalysisV1 was exercised on synthetic fixtures covering a pass, a statistically significant but not practically meaningful failure, a practically large but imprecise failure, an outright negative result, every exclusion route, and an ablation arm that is significant before correction and not after. Participant information, consent, debrief and data-handling materials describe the actual procedure and claim no external approval. The reviewer onboarding package is complete and names nobody. AlivenessGovernanceAuditV2 has twenty-seven items and derives its activation state and recruitment gate from them. Separately, figures quoted in the D-009 completion report and in several D-009 prose documents came from an intermediate kernel run rather than the frozen one and were reconciled against qualification/fixtures/A000/A000_REPORT.txt; every finding, direction and verdict in O-0009 was and remains correct, and the corrected values are mean population differentiation 0.166 rather than 0.163, closest pair 0.077 rather than 0.074, final-window diversity 0.136 rather than 0.103, substantive spontaneity attribution 0.891 over 2,154 scored actions rather than 0.877 over 2,089, static-habitat entropy 2.735 rather than 2.714, occupancy 0.361 rather than 0.357, cycle regularity 0.366 rather than 0.369, and a re-explored source rising to 31.0 uses a day seven ticks after the reversal rather than 32.5 uses a day after twenty ticks, all recorded as DEC-0027.
- Changed areas: research/aliveness-spike/study-protocol/, research/aliveness-spike/analysis/, research/aliveness-spike/accelerated-sim/, research/aliveness-spike/cohorts/, research/aliveness-spike/evidence/, governance/release-gates/A001_ACTIVATION_GATE.md, governance/release-gates/A000_EXIT_GATE.md, governance/qualification/, docs/decisions/DECISION_LOG.md, docs/invariants/INVARIANT_REGISTRY.md, qualification/fixtures/A000/, qualification/longitudinal/A000/, qualification/evidence/A001PRE/, tools/build_qualification_bundle.py, .github/workflows/ci.yml, .agent/
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py covering six production modules - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering R000 and R001 and R002 and A000 at their pinned commits and A001PRE live - PASSED
  - python3 tools/generate_lookup_tables.py --check - PASSED
  - ./gradlew clean build covering ten modules and two hundred and eighty-five JVM tests - PASSED
  - ./gradlew :desktop-runner:run reproducing both frozen production digests - PASSED
  - ./gradlew :research:aliveness-spike:accelerated-sim:run reproducing the A000 digest with all twenty-four findings held - PASSED
  - ./gradlew :research:aliveness-spike:accelerated-sim:baselineManifest - PASSED
  - ./gradlew :research:aliveness-spike:analysis:a001DryRun exercising every preregistered decision path on synthetic fixtures - PASSED
- Remaining risks: A001 remains blocked on five inputs that no code can supply, namely an independently qualified scripted baseline, a registered variance pilot, the paired-difference standard deviation that pilot would release, three named eligible reviewers, and an owner resource ceiling. The graded instrument is frozen but not cognitively pretested and was written by a party with an interest in the outcome. The powered sample size is unknown until the pilot runs, so A001_NOT_FEASIBLE remains a real possible outcome. The per-participant schedule behind the participant-hour figure is a design estimate rather than a measurement. Every access control in the data-handling package except the variance-pilot barrier is written guidance rather than an enforced control. No institutional review board, ethics committee or data-protection review has seen this study, none is claimed, and whether one is required is undetermined. The habitat remains abstract. The envelope search still runs outside CI and its result is committed evidence rather than a reproduced check.
- Blockers: A001 cannot begin. The activation audit reports BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED, BLOCKED_VARIANCE_PILOT_NOT_REGISTERED, BLOCKED_SPEC_PAIRED_DIFFERENCE_SD, BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED and BLOCKED_SPEC_STUDY_BUDGET.
- Follow-up directive: none

## D-011 - COMPLETE

- Outcome ID: O-0011
- Supersedes outcome: none
- Closed: 2026-08-14T11:55:00-04:00
- Acceptance: MET
- Summary: The bounded R012 persistence, recovery and identity substrate is implemented and qualified. Five contracts are frozen before their dependent implementation was treated as qualified: PersistenceBackendContractV1, LocalStorageCryptographyContractV1, RecoveryCryptographyContractV1, IdentityAuthorityProtocolV1 and RecoveryPackageStoreContractV1. The production backend was selected from six candidates benchmarked in isolation on a real ext4 filesystem over NVMe, where direct SQLite in write-ahead-logging mode cost 5.3 times the p99 commit latency and 4.4 times the storage of the selected single-writer append-only log for a workload that never issues a query. Local persistence is authenticated ciphertext with a random wrapped data key, and rotation, interrupted rewrap, device mismatch, container refusal and copied-state quarantine are all qualified with no path that produces a fresh key. Recovery cryptography freezes a 256-bit root with a checksummed base32 encoding, HKDF derivation, and a plaintext MAC-protected manifest bound into the ciphertext associated data; end-to-end cold recovery restores an organism on a destination device from package plus secret alone, and corrupt, stale, wrong-key, duplicate and interrupted paths are all refused correctly. The identity authority advances epochs by atomic compare-and-swap under a nonce-bound proof, spends nonces even on failure, rate limits, tells a superseded device on next contact, survives its own restart, and stores only identity metadata. A filesystem-backed recovery provider passes a conformance suite written against the interface. The forty-two fixture kernel holds on every fixture, including a fault matrix driven by real child JVMs killed with Runtime.halt.
- Changed areas: core-persistence/, core-recovery/, services/identity-authority/, core-crypto/, desktop-runner/, benchmarks/persistence-bench/, docs/architecture/, docs/decisions/DECISION_LOG.md, docs/invariants/INVARIANT_REGISTRY.md, governance/release-gates/R012_SUBSTRATE_GATE.md, governance/qualification/, qualification/fixtures/R012/, qualification/evidence/R012/, settings.gradle.kts, gradle/libs.versions.toml, tools/, .github/workflows/ci.yml, .agent/
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py covering eight modules - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering R000 and R001 and R002 and A000 at their pinned commits and R012 and A001PRE live - PASSED
  - python3 tools/generate_lookup_tables.py --check - PASSED
  - ./gradlew clean build covering fourteen modules and three hundred and sixty JVM tests - PASSED
  - ./gradlew :desktop-runner:run reproducing both frozen production digests - PASSED
  - ./gradlew :desktop-runner:r012Qualification with forty-two of forty-two fixtures held - PASSED
  - ./gradlew :desktop-runner:r012Performance - PASSED
  - ./gradlew :benchmarks:persistence-bench:run - PASSED
  - ./gradlew :research:aliveness-spike:accelerated-sim:run reproducing the A000 digest - PASSED
  - ./gradlew :research:aliveness-spike:analysis:a001DryRun - PASSED
- Remaining risks: No Android device or emulator was reachable, so the Android Keystore key container and device-level backend, corruption and full-storage qualification remain outstanding and are recorded as BLOCKED_DEVICE_UNAVAILABLE. Power-loss durability is not proven by any test, because killing a process leaves the operating system page cache intact; the claim rests on the force-per-commit policy and is stated as such rather than implied to be measured. The measured latencies are reference-machine figures and no production threshold is derived from them. The qualifying recovery provider is filesystem-backed, and selecting a network provider is a product decision needing an owner, credentials and a privacy review. The identity authority has no transport, hosting, backup or incident procedure. A001 remains blocked on the same five external inputs as at the close of D-010.
- Blockers: The device half of the R012 substrate cannot close without an Android device or emulator. A001 cannot begin without three named reviewers, an independently qualified baseline, a registered variance pilot, the released paired-difference standard deviation and an owner resource ceiling.
- Follow-up directive: none


## D-012 - BLOCKED

- Outcome ID: O-0012
- Supersedes outcome: none
- Closed: 2026-08-14T16:40:00-04:00
- Acceptance: NOT MET
- Summary: The Android production adapter for the R012 substrate is implemented against the D011-frozen contracts and the device qualification suite is complete, but no physical Android device was reachable, so D012 closes as BLOCKED_DEVICE_UNAVAILABLE rather than PASS. The Keystore container is an AndroidKeyStore HMAC-SHA256 key whose root secret is a derived value over a fixed label, chosen because an AES key cannot supply a secret that is stable across restarts. Opening never generates missing material and a birth requires both the key state and the container material to be absent, so a lost keystore quarantines instead of producing a second organism. Canonical state resolves to app-private storage under a path the shipped backup and device-transfer rules exclude, and that exclusion is now verified from the built debug and release packages by following the merged manifest's own resource references. A forty-five fixture device kernel covers the Keystore lifecycle, device persistence, a fault matrix driven by a real Android process killed with Runtime.halt in its own process, installed-package backup exclusion, and canonical determinism across persistence, process death, restart and cold recovery. It ran on an x86 emulator with forty-four of forty-five fixtures held. The one that did not hold records a contradiction between two frozen contracts rather than a defect in this directive's code, and is declared rather than silenced.
- Changed areas: android-host/, core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/R012PerformanceHarness.kt, tools/build_qualification_bundle.py, tools/verify_backup_exclusion.py, docs/decisions/DECISION_LOG.md, docs/invariants/INVARIANT_REGISTRY.md, governance/release-gates/R012_DEVICE_GATE.md, governance/qualification/, qualification/device-matrix/R012/, qualification/evidence/R012/, .github/workflows/ci.yml, .agent/
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py covering eight modules - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering seven bundles - PASSED
  - python3 tools/generate_lookup_tables.py --check - PASSED
  - python3 tools/verify_backup_exclusion.py against the built debug and release packages - PASSED
  - ./gradlew clean build covering fourteen modules and three hundred and seventy seven JVM tests - PASSED
  - ./gradlew :desktop-runner:run reproducing both frozen production digests - PASSED
  - ./gradlew :desktop-runner:r012Qualification with forty-two of forty-two fixtures held - PASSED
  - ./gradlew :desktop-runner:r012Performance - PASSED
  - ./gradlew :research:aliveness-spike:accelerated-sim:run reproducing the A000 digest - PASSED
  - ./gradlew :research:aliveness-spike:analysis:a001DryRun byte identical to the committed evidence - PASSED
  - ./gradlew :android-host:connectedDebugAndroidTest on an x86 emulator, twelve tests, forty-four of forty-five device fixtures held - PASSED on an emulator, which is supplementary coverage and not physical-device evidence
  - Physical-device Keystore, storage, latency and restart qualification - BLOCKED
- Remaining risks: No physical Android device was reachable, so Keystore hardware or StrongBox backing, real device flash behaviour, physical-device latency and on-hardware restart are unqualified; the emulator reported software-backed key material and its storage is not device flash. Sudden power-loss durability remains unproven and unclaimed, because killing a process leaves the operating system page cache intact. No production threshold is derived from any measurement. Fixture DV-KS-ROTATION-READBACK-01 records that after a wrapping-epoch rotation the existing journal does not open, which contradicts the rotation promise in LocalStorageCryptographyContractV1 and needs a versioned amendment and architect review rather than a device-side patch. A001 remains blocked on the same five external inputs.
- Blockers: No physical Android device is reachable to this environment. The frozen-contract contradiction recorded by DV-KS-ROTATION-READBACK-01 requires an architect decision.
- Follow-up directive: none

## D-013 - COMPLETE

- Outcome ID: O-0013
- Supersedes outcome: none
- Closed: 2026-08-14T20:05:00-04:00
- Acceptance: MET
- Summary: The 2026-08-14 key-epoch separation amendment is implemented and frozen as LocalStorageCryptographyContractV2, which supersedes V1 and amends sections 13.3 through 13.5 of ContinuityDurabilityContractV1. The wrapping epoch, the data-key identity and a record's immutable encryption context are now three separate quantities, and only the last decides whether a record can be read. An ordinary wrapping rotation advances the epoch, rewraps the same data key, rewrites no journal byte, and leaves every already-written record readable through one rotation, through five, mixed with newer records, and across a restart. The record byte layout is unchanged, so no journal is rewritten and no canonical byte or hash moves; key state migrates from schema 231 version 1 to version 2 by adding a data-key identity to one small file, deterministically, idempotently, and crash-safely at both durable boundaries under real process death. Removing the epoch pre-check removed no guarantee: the context is inside the associated data, and four forged header fields and a foreign data key are all refused by the tag. Thirteen desktop fixtures and one device fixture were added, and the previously failing DV-KS-ROTATION-READBACK-01 now holds with readableAfterRotation five of five, unchanged in identifier, question and threshold. The external prior-art check is recorded as PA-0001 with disposition REFERENCE and no dependency was adopted.
- Changed areas: docs/architecture/LocalStorageCryptographyContractV2.md, docs/architecture/LocalStorageCryptographyContractV1.md, docs/architecture/ContinuityDurabilityContractV1.md, core-continuity/, core-persistence/, android-host/src/androidTest/, tools/build_qualification_bundle.py, docs/decisions/, docs/invariants/INVARIANT_REGISTRY.md, governance/qualification/, governance/release-gates/, qualification/, .agent/
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py covering eight modules - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering nine bundles - PASSED
  - python3 tools/generate_lookup_tables.py --check - PASSED
  - python3 tools/verify_backup_exclusion.py against the built debug and release packages - PASSED
  - ./gradlew clean build covering fourteen modules and three hundred and eighty six JVM tests - PASSED
  - ./gradlew :desktop-runner:run reproducing both frozen production digests unchanged - PASSED
  - ./gradlew :desktop-runner:r012Qualification with fifty five of fifty five fixtures held - PASSED
  - ./gradlew :research:aliveness-spike:accelerated-sim:run reproducing the A000 digest unchanged - PASSED
  - ./gradlew :research:aliveness-spike:analysis:a001DryRun byte identical to the committed evidence - PASSED
  - ./gradlew :android-host:connectedDebugAndroidTest on an x86 emulator, twelve tests, forty six of forty six device fixtures held - PASSED on an emulator, which is supplementary coverage and not physical-device evidence
  - Physical-device Keystore, storage, latency and restart qualification - BLOCKED, unchanged from D012
- Remaining risks: D012 remains BLOCKED_DEVICE_UNAVAILABLE. No physical Android device was reachable to D013 either, so Keystore hardware or StrongBox backing, real device flash behaviour, physical-device latency and on-hardware restart are still unqualified, and resolving a fixture is not qualifying hardware. Sudden power-loss durability remains unproven and unclaimed, because killing a process leaves the operating system page cache intact. Data-encryption-key rotation is not implemented and needs a separately frozen design before it is; a wrapping rotation must never be quietly turned into one. No production threshold is derived from any measurement. A001 remains blocked on the same five external inputs.
- Blockers: No physical Android device is reachable to this environment.
- Follow-up directive: none

## D-014 - COMPLETE

- Outcome ID: O-0014
- Supersedes outcome: none
- Closed: 2026-08-14T21:40:00-04:00
- Acceptance: MET
- Summary: The network half of the R012 substrate is implemented and qualified. S3_COMPATIBLE_OBJECT_STORE_V1 implements the unchanged RecoveryPackageStoreContractV1 over an S3-compatible object API with a vendor-neutral endpoint, region, bucket and key prefix, externally supplied credentials and no vendor branch anywhere in the implementation. IdentityAuthorityTransportContractV1 is frozen and carries IdentityAuthorityProtocolV1 without redefining an epoch, challenge, lease, replay rule, rate limit, idempotency rule or activation outcome; every property D011 qualified in process was re-qualified over HTTP and produced the same outcome. Both were built rather than adopted, and the reasons are recorded as PA-0002 and PA-0003 with disposition BUILD: the provider has to run on the destination device during a cold recovery, where AWS does not support its v2 Java SDK and the MinIO client would place a second cryptographic provider on a device whose key hierarchy is already frozen, while the authority is a four-endpoint service whose entire job is to be trustworthy about one number. The signer's compatibility is not self-asserted: MinIO, an independent implementation of AWS Signature Version 4, authenticated every request and carried the whole end-to-end cold recovery. Thirty eight fixtures hold against the in-repository qualification endpoint and thirty three against MinIO, and the two runs are recorded separately rather than averaged because five fixtures need a fault MinIO cannot be told to produce. A recovery-provider outage leaves twenty five local commits succeeding and readable, an authority outage leaves local state intact and stays distinct from a refusal, a duplicated activation consumes no second epoch, two destinations racing on two sockets produce exactly one winner, a restarted authority still holds the epoch it granted, a planted plaintext canary is absent from everything the provider holds and from every request line it saw, and a restored organism's canonical state hash equals its source's across two different object keys. The operations package covers configuration, secret ownership, health and readiness, the durable store, backup, restore, upgrade, migration, log privacy and an incident runbook, with an explicit list of eleven things that are not production-qualified. No dependency was adopted and gradle/libs.versions.toml is unchanged.
- Changed areas: core-recovery-net/, services/s3-qualification-endpoint/, services/identity-authority/, core-recovery/, desktop-runner/, docs/architecture/IdentityAuthorityTransportContractV1.md, docs/architecture/RecoveryPackageStoreContractV1.md, docs/architecture/IdentityAuthorityProtocolV1.md, docs/operations/, docs/decisions/, docs/invariants/INVARIANT_REGISTRY.md, tools/build_qualification_bundle.py, governance/qualification/, governance/release-gates/, qualification/network/R014/, qualification/evidence/R014/, .github/workflows/ci.yml, settings.gradle.kts, .agent/
- Validation:
  - python3 scripts/validate_governance.py --mode ADOPTED - PASSED
  - python3 scripts/test_validate_governance.py - PASSED
  - python3 tools/verify_project_identity.py covering eight modules - PASSED
  - python3 tools/build_qualification_bundle.py --verify covering ten bundles - PASSED
  - python3 tools/generate_lookup_tables.py --check - PASSED
  - python3 tools/verify_backup_exclusion.py against the built debug and release packages - PASSED
  - ./gradlew clean build covering sixteen modules and four hundred and twenty nine JVM tests - PASSED
  - ./gradlew :desktop-runner:run reproducing both frozen production digests unchanged - PASSED
  - ./gradlew :desktop-runner:r012Qualification with fifty five of fifty five fixtures held - PASSED
  - ./gradlew :desktop-runner:r014NetworkQualification against the in-repository endpoint with thirty eight of thirty eight fixtures held, digest reproduced twice - PASSED
  - ./gradlew :desktop-runner:r014NetworkQualification against MinIO RELEASE.2025-09-07T16-13-09Z with thirty three of thirty three fixtures held - PASSED
  - ./gradlew :research:aliveness-spike:accelerated-sim:run reproducing the A000 digest unchanged - PASSED
  - ./gradlew :research:aliveness-spike:analysis:a001DryRun byte identical to the committed evidence - PASSED
  - Physical-device Keystore, storage, latency and restart qualification - BLOCKED, unchanged from D012
  - Production deployment of the identity authority - NOT RUN, and no hosting, availability, redundancy or disaster-recovery claim is made
  - Qualification against a commercial object store - NOT RUN, excluded by the directive
  - TLS against a real certificate chain - NOT RUN, both endpoint runs are loopback plaintext HTTP
- Remaining risks: D012 remains BLOCKED_DEVICE_UNAVAILABLE and no physical Android device was reachable to D014 either; the network provider is written to the Android API 29 surface and that surface is enforced by a test that reads the compiled classes back, but nothing in D014 ran on a device. The identity authority is not deployed anywhere, so its backup, restore, upgrade and incident procedures are written and unexercised, and it cannot be run as more than one instance without breaking the epoch compare-and-swap. TLS is unexercised. No commercial endpoint has been used, so provider-specific behaviour, cost and throughput are unknown and no threshold is derived. Multipart upload is not implemented and a package above sixty four mebibytes is refused rather than split. Recovery provider selection for the product remains a decision needing an owner, credentials and a privacy review. Verification-key rotation is not designed. Sudden power-loss durability remains unproven and unclaimed. A001 remains blocked on the same five external inputs.
- Blockers: No physical Android device is reachable to this environment. No production environment, owner or credentials exist for the identity authority or for a commercial object store.
- Follow-up directive: none
