# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-13T23:20:00-04:00`

## Active state after adoption

- Local directive ID: D-010
- External directive ID: D010
- Objective: Prepare the complete A001 Attempt-1 study and governance package so human work can begin as soon as the named reviewer roster and the owner resource ceiling exist, without collecting any human outcome data.
- Current status: `COMPLETE`
- Acceptance: AlivenessStudyProtocolV1 is frozen and executable with exact instrument wording and anchors; the ten-point and ninety-five-percent-confidence-interval rule is encoded exactly; the baseline qualification package and the blind variance pilot are operationally ready; a synthetic dry run proves the FULL-facing pilot channel releases only the paired-difference standard deviation; the analysis pipeline passes synthetic positive and negative fixtures; the feasibility calculator is complete with only the owner ceiling and the real pilot standard deviation unresolved; the reviewer onboarding package is complete and truthfully unassigned; the governance audit prevents premature activation; participant information and privacy materials are complete; no human outcome data is collected; no R003 through R009 production implementation is introduced; prior qualifications remain green; reproducible evidence exists; CI passes; HEAD matches origin/main; and the worktree is clean.
- Current phase: A000 complete and remediated. The A001 activation package is prepared and the gate is correctly shut. R003 through R009 remain blocked behind A001.
- Expected or actual touched areas: research/aliveness-spike/study-protocol/, research/aliveness-spike/analysis/, research/aliveness-spike/accelerated-sim/, research/aliveness-spike/cohorts/, research/aliveness-spike/evidence/, governance/release-gates/, governance/qualification/, docs/decisions/, docs/invariants/, qualification/fixtures/A000/, qualification/longitudinal/A000/, qualification/evidence/A001PRE/, tools/build_qualification_bundle.py, .github/workflows/ci.yml, .agent/
- Immediate next action: Hold for architect review of the D-010 completion report; this snapshot is awaiting reset to IDLE once that review closes. The two outstanding external inputs are three eligible reviewer and baseline-owner names, and the maximum human-study participant and resource budget. Do not begin D011.

## Temporary task-relevant facts

- The A001 activation package is complete: AlivenessStudyProtocolV1 and GradedAlivenessInstrumentV1 are frozen, and the participant information, consent, privacy and reviewer onboarding materials exist.
- The programme success floor is frozen at a mean paired difference of at least ten points with a two-sided ninety-five percent confidence interval lower bound above zero, at alpha 0.05 and power 0.80. Recorded as DEC-0026.
- The human ablation family is three arms again. The architect preregistered FULL minus outcome uncertainty and directed re-exploration as the replacement. Recorded as DEC-0025.
- The variance-pilot information barrier is enforced by type visibility rather than by policy, and two synthetic pilots with opposite outcomes release byte-identical output.
- The registered pilot size of thirty-six pairs is the smallest one the frozen 1.25 inflation factor can defend, which is checked numerically in a test rather than asserted.
- The feasibility calculator uses exact noncentral-t power and powers ablation arms at the corrected alpha divided by three.
- The activation audit has twenty-seven items and five outstanding blockers, and the activation state is derived from the items rather than declared beside them.
- The A000 fixture set moved to version three with digest 9462e43622c414db47c28a2e79452455bc0d6642396dd5ca8d65bae208b3114a, because the cohort-parity fixture covers the new preregistered ablation cohort. No organism behaviour changed and all twenty-four findings still hold.
- Several D009 prose figures were reconciled against the frozen kernel evidence. Every finding, direction and verdict was and remains correct. Recorded as DEC-0027.
- The A000 bundle is now pinned to its qualified commit 4a2b1e4c and its manifest hash is unchanged at 2c7cd508504ab29b0857a676e6f86c0deeb717ad32e796a1ef76b7abe324f822.
- The A001PRE bundle is A001PRE-QB-1 with manifest hash 0125bb36425b8ae0a815fe92d86a8f834084b3cacb111d467a8b5742edeacbdd over forty-seven constituents.
- No human outcome data exists anywhere in the repository, and every synthetic figure is marked SYNTHETIC where it appears.
- No production organism behaviour exists. Nothing under research/ is depended on by any production module.
- D-001 remains recorded as nonconforming, and D-002 through D-009 remain recorded as accepted and complete.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify, python3 tools/generate_lookup_tables.py --check, ./gradlew clean build, ./gradlew :desktop-runner:run, ./gradlew :research:aliveness-spike:accelerated-sim:run, ./gradlew :research:aliveness-spike:accelerated-sim:baselineManifest, and ./gradlew :research:aliveness-spike:analysis:a001DryRun
- Result: `PASSED`

## Risks

- A001 remains blocked on five inputs, none of which code can supply: an unqualified scripted baseline, an unregistered variance pilot, the paired-difference standard deviation that pilot would release, three unassigned reviewer roles, and an owner resource ceiling.
- The graded instrument is frozen but not cognitively pretested, and was written by a party with an interest in the outcome. The independent reviewer may require pretesting before Attempt 1.
- The powered sample size is unknown until the pilot runs, so A001_NOT_FEASIBLE remains a real possible outcome.
- The per-participant schedule used for the participant-hour figure is a protocol design estimate, not a measurement.
- Every access control in DataHandlingAndPrivacyV1 except the variance-pilot barrier is written guidance rather than an enforced control, and no prose prevents an action.
- No institutional review board, ethics committee or data-protection review has seen this study, none is claimed, and whether one is required is undetermined.
- The scripted baseline still has no human competence qualification, and none is claimed.
- The habitat remains abstract, and now carries circadian structure that was added specifically to make a mechanism testable.
- Cycle regularity and single-action occupancy remain constructs invented for this track, and the envelope result has turned on both in both directions.
- The accelerated kernel takes about three minutes and the envelope search about eight. The kernel and the A001 dry run run in CI; the envelope search does not, and its result is committed evidence rather than a reproduced check.
- The repository is public while carrying a proprietary licence, and an MIT grant was published for the revisions between the initial commit and the D-005 commit.
- Governance history contains a permanent nonconformance at D-001 that must not be rewritten by later work. Recorded as DEC-0002.

## Blockers

- A001 cannot begin. The activation audit reports BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED, BLOCKED_VARIANCE_PILOT_NOT_REGISTERED, BLOCKED_SPEC_PAIRED_DIFFERENCE_SD, BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED and BLOCKED_SPEC_STUDY_BUDGET. The curiosity-envelope and success-floor blockers are cleared.

## Pending decisions

- Who fills PrimaryIndependentAlivenessGateReviewer, AlternateIndependentAlivenessGateReviewer and BaselineIndependentOwner.
- The maximum fundable participants and participant-hours for A001.
- Whether the graded instrument must be cognitively pretested before Attempt 1.
- Whether this study requires external ethical or institutional approval, and in which jurisdiction it runs.
- When to freeze RecoveryCryptographyContractV1, which unblocks R002.12 recovery cryptography and the identity-epoch authority.
- Which production storage provider to select, and which storage engine adopts the durable medium interface.
- Whether the repository should remain public now that the licence is proprietary.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
