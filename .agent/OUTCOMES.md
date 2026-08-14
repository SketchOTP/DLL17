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

