# Current State

## Lifecycle

- Status: `ADOPTED`
- Last updated: `2026-08-13T20:40:00-04:00`

## Active state after adoption

- Local directive ID: D-009
- External directive ID: D009
- Objective: Revise the disposable A000 FULL candidate against the accepted D008 negative evidence, under unchanged curiosity-envelope thresholds and without consuming the programme’s single allowed threshold-only revision.
- Current status: `COMPLETE`
- Acceptance: The revised candidate stays bounded and numerically stable; the long-run differentiation and diversity floors pass; rejected alternatives can be boundedly re-explored after contingency or devaluation change; the episodic mechanism either contributes or is removed from FULL; CuriosityBalanceEnvelopeV1 returns NON_EMPTY_FEASIBLE_REGION with thresholds unchanged; substantive-spontaneity attribution still passes; all prior negative evidence remains preserved; research and production isolation remains intact; a reproducible revised A000 evidence bundle exists; R000, R001 and R002 remain green; CI passes; HEAD matches origin/main; the worktree is clean; and no R003 through R009 production mechanism is introduced.
- Current phase: A000 complete and remediated. A001 is the next gate and cannot begin. R003 through R009 remain blocked behind A001.
- Expected or actual touched areas: research/aliveness-spike/, settings.gradle.kts, governance/release-gates/, governance/qualification/, docs/decisions/, docs/invariants/, docs/architecture/, qualification/fixtures/A000/, qualification/longitudinal/A000/, qualification/evidence/A000/, tools/build_qualification_bundle.py, .github/workflows/ci.yml, .agent/
- Immediate next action: Hold for architect review of the D-009 completion report; this snapshot is awaiting reset to IDLE once that review closes. The outstanding architect decisions are the reviewer roster, the aliveness success floor and whether to preregister a replacement third ablation arm. Do not begin D010.

## Temporary task-relevant facts

- The revised A000 evidence digest is 65efd37541b66a5bd30bacb5c8176abd8cba7832f00029ab3e9afd8589dc81fc for fixture set version 2, compiled into A000QualificationKernel.GOLDEN_EVIDENCE_DIGEST and checked by CI.
- All twenty-four accelerated findings hold. Under D008 five did not.
- The joint curiosity envelope returns NON_EMPTY_FEASIBLE_REGION with twenty-seven of twenty-seven grid points feasible and robust, against zero of twenty-seven under D008, on the identical grid and seed matrix.
- No curiosity threshold was altered and the programme's single allowed threshold-only revision remains unspent. Recorded as DEC-0023.
- Mean population differentiation rose from 0.052 to 0.163, closest-pair from 0.019 to 0.074, final-window diversity from 0.034 to 0.103 and matched-stimulus history divergence from 0.325 to 0.610.
- Re-exploration is evidenced by a controlled reversal protocol in which a rejected food source went from 0.13 to 32.5 uses a day while the devalued source went from 31.1 to zero, with the first return twenty ticks after the flip.
- Episodic history was revised and then removed from FULL because the revision still did not contribute. The cohort is retained inverted as FULL+episodic-history so the negative result stays reproducible. Recorded as DEC-0024.
- The human leave-one-out ablation family is two arms rather than three, and Holm-Bonferroni now corrects across two.
- Anti-convergence is enforced by three small independent bounds rather than one large one, namely action satiation, a per-object engagement refractory and a metabolic cost for vigorous activity.
- Substantive spontaneity attribution fell from 0.946 to 0.877 against an unchanged 0.700 floor, which is the expected direction for an organism spending far more time in genuinely optional behaviour.
- The two scripted comparators and the presentation contract are unchanged, and the baseline is within noise of its D008 measures while remaining far stronger than the degraded control.
- All D008 failed results and the rejected configuration are preserved in research/aliveness-spike/evidence/negative/D008/ and are bundle constituents.
- No production organism behaviour exists. Nothing under research/ is depended on by any production module.
- D-001 remains recorded as nonconforming, and D-002 through D-008 remain recorded as accepted and complete.

## Last validation after adoption

- Command or check: python3 scripts/validate_governance.py --mode ADOPTED, python3 scripts/test_validate_governance.py, python3 tools/verify_project_identity.py, python3 tools/build_qualification_bundle.py --verify, python3 tools/generate_lookup_tables.py --check, ./gradlew clean build, ./gradlew :desktop-runner:run, ./gradlew :research:aliveness-spike:accelerated-sim:run, and ./gradlew :research:aliveness-spike:analysis:run
- Result: `PASSED`

## Risks

- A001 remains blocked because IndependentReviewRosterV1 names nobody in any of its three roles, which no amount of code resolves.
- The human ablation family is now two arms. Choosing a replacement third arm requires a new preregistered plan and is not the implementer's decision.
- The scripted baseline still has no human competence qualification, and none is claimed.
- Two learning laws and one memory mechanism were changed on the strength of accelerated measures alone, with no human data.
- The habitat remains abstract, and now carries circadian structure that was added specifically to make the episodic mechanism testable.
- Cycle regularity and single-action occupancy remain constructs invented for this track, and the envelope result has now turned on both in both directions.
- The accelerated kernel takes about three minutes and the envelope search about eight. The kernel runs in CI; the envelope search does not, and its result is committed evidence rather than a reproduced check.
- The repository is public while carrying a proprietary licence, and an MIT grant was published for the revisions between the initial commit and the D-005 commit.
- Governance history contains a permanent nonconformance at D-001 that must not be rewritten by later work. Recorded as DEC-0002.

## Blockers

- A001 cannot begin while IndependentReviewRosterV1 is unassigned. The curiosity-envelope blocker is cleared: the feasible region is non-empty under unchanged thresholds.

## Pending decisions

- Whether to preregister a replacement third human ablation arm now that the episodic arm has been retired.
- Who fills PrimaryIndependentAlivenessGateReviewer, AlternateIndependentAlivenessGateReviewer and BaselineIndependentOwner.
- The programme-level aliveness success floor, which is a value judgement about what complexity is worth and is not derivable from A000 evidence.
- When to freeze RecoveryCryptographyContractV1, which unblocks R002.12 recovery cryptography and the identity-epoch authority.
- Which production storage provider to select, and which storage engine adopts the durable medium interface.
- Whether the repository should remain public now that the licence is proprietary.

## Status vocabulary

Allowed adopted-project statuses: `IDLE`, `PLANNING`, `IN_PROGRESS`, `VALIDATING`, `BLOCKED`, `COMPLETE`. `CURRENT.md` is mutable and never replaces historical ledgers. Reset it to `IDLE` when an adopted task closes.
