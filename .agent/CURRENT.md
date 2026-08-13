# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-13T18:05:00-04:00`

## Active state after adoption

- Local directive ID: D-008
- External directive ID: D008
- Objective: Build and qualify the complete disposable A000 Aliveness Spike, isolated from production, so the programme can begin establishing whether the organism hypothesis itself is any good.
- Current status: `COMPLETE`
- Acceptance: A buildable, runnable, isolated A000 research project using only the authorized R001 dependency; a deterministic accelerated simulator and a real-time viewer; all six cohorts running through one presentation contract; the full authorized candidate mechanism set in FULL; SpikeDecisionTraceV1 for autonomous actions; executable and evidenced exact coalition attribution; an actually evaluated CuriosityBalanceEnvelopeV1; a documented viability or failure result from accelerated histories; bounded long-run behaviour; no fabricated human result; pre-A001 contracts frozen where legitimately possible and explicitly blocked where real evidence is required; a reproducible A000 evidence bundle; R000, R001 and R002 still green; CI passing; HEAD matching origin/main; a clean worktree; and no R003 through R009 production mechanism introduced.
- Current phase: A000 complete. A001 is the next gate and cannot begin. R003 through R009 remain blocked behind A001.
- Expected or actual touched areas: research/aliveness-spike/, settings.gradle.kts, governance/release-gates/, governance/qualification/, docs/decisions/, docs/invariants/, docs/architecture/, qualification/fixtures/A000/, qualification/longitudinal/A000/, qualification/evidence/A000/, tools/build_qualification_bundle.py, .github/workflows/ci.yml, .agent/
- Immediate next action: Hold for architect review of the D-008 completion report; this snapshot is awaiting reset to IDLE once that review closes. Two decisions belong to the architect, namely the empty curiosity-envelope feasible set and the unassigned reviewer roster. Do not begin D009.

## Temporary task-relevant facts

- The A000 evidence digest is 4765e6d587347688841d34c95b5b9caede8cbf44084335302e1475c7aeaa8fc9, compiled into A000QualificationKernel.GOLDEN_EVIDENCE_DIGEST and checked by CI.
- Of twenty-two accelerated findings, seventeen held and five did not, and all five negative findings are preserved with their configurations rather than tuned away.
- The five negative findings are that the closest pair of organisms is barely distinguishable, that preference devaluation works but switching to the alternative does not follow from it, that removing episodic history increased history dependence rather than reducing it, that the static habitat produces high behavioural regularity over one hundred and eighty days, and that the population partially converges toward a common long-run policy.
- The curiosity balance envelope search returned EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE. Zero of twenty-seven grid points were jointly feasible. Attribution passed at every point with wide margin; anti-convergence failed at every point on exactly two of five criteria, namely maximum single-action occupancy and cycle regularity.
- No threshold was altered. CuriosityEnvelopeFeasibilityV1 permits one threshold-only revision ever and reserves the choice to the independent reviewer.
- Fifteen research contracts exist, of which nine are FROZEN, three are READY_FOR_HUMAN_EVIDENCE and three are blocked. The blocked ones are A001FeasibilityBudgetV1 on BLOCKED_SPEC_PAIRED_DIFFERENCE_SD, IndependentReviewRosterV1 on BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED, and the programme success floor inside AlivenessProgramGateV1 on BLOCKED_SPEC_ALIVENESS_SUCCESS_FLOOR.
- Research isolation is enforced at source level rather than by Gradle, because core-math publishes core-crypto as api and therefore puts it on the compile classpath regardless. SpikeIsolationTest reads every spike source's import list.
- Two learning laws were rewritten during the track from accumulators to error-corrected estimates. The accumulator form cannot represent reliability at all, which is recorded as a mechanism-design finding.
- Two hundred and thirty-three JVM tests pass across ten modules, of which one hundred and eighty-nine are production and forty-four are research.
- R002's qualification bundle is now pinned to its qualified commit 7f6f37fabba6a5ad4af2fd517e62cb4c08dbfeb2, because A000 edits settings.gradle.kts.
- No production organism behaviour exists. R003 through R009 remain unimplemented, and nothing under research/ is depended on by any production module.
- D-001 remains recorded as nonconforming, and D-002 through D-007 remain recorded as accepted and complete.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify, python3 tools/generate_lookup_tables.py --check, ./gradlew clean build, ./gradlew :desktop-runner:run, ./gradlew :research:aliveness-spike:accelerated-sim:run, and ./gradlew :research:aliveness-spike:analysis:run
- Result: `PASSED`

## Risks

- The joint curiosity envelope feasible set is empty everywhere. Whether that is an incompatible threshold pair or a mechanism failure is the independent reviewer's determination, and there is no reviewer.
- IndependentReviewRosterV1 names nobody in any of its three roles, which blocks A001 outright and cannot be resolved by writing code.
- The scripted baseline is implemented and objectively stronger than the degraded control, but its human competence qualification has not been run and is not claimed.
- Over a short observation window the scripted baseline produces far more surface variety than FULL, because it rotates a broad authored script while FULL commits to actions and sleeps. Whether variety or coherent motivation reads as more alive is exactly what A001 exists to answer, but the primary comparison will not be an easy win.
- The A000 habitat is abstract, with twelve affordances and no space, navigation or sensors. Behaviour observed here is behaviour in that habitat.
- Cycle regularity and single-action occupancy are constructs invented for this track, and the envelope result turns on both of them.
- The accelerated kernel takes about three minutes and the envelope search about five. The kernel runs in CI; the envelope search does not, and its result is committed evidence rather than a reproduced check.
- The repository is public while carrying a proprietary licence, and an MIT grant was published for the revisions between the initial commit and the D-005 commit.
- Governance history contains a permanent nonconformance at D-001 that must not be rewritten by later work. Recorded as DEC-0002.

## Blockers

- A001 cannot begin. Both blockers are structural rather than technical, namely that the reviewer roster is unassigned and that the empty curiosity-envelope feasible set requires the reviewer's threshold-versus-mechanism determination.

## Pending decisions

- Whether the empty curiosity-envelope feasible set is a threshold incompatibility or a mechanism failure. One threshold-only revision is available, ever.
- Who fills PrimaryIndependentAlivenessGateReviewer, AlternateIndependentAlivenessGateReviewer and BaselineIndependentOwner.
- The programme-level aliveness success floor, which is a value judgement about what complexity is worth and is not derivable from A000 evidence.
- Whether episodic history should be simplified or removed, given that removing it increased history dependence.
- Whether the mechanism set needs a re-exploration drive, given that devaluation does not lead to switching.
- When to freeze RecoveryCryptographyContractV1, which unblocks R002.12 recovery cryptography and the identity-epoch authority.
- Which production storage provider to select, and which storage engine adopts the durable medium interface.
- Whether the repository should remain public now that the licence is proprietary.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
