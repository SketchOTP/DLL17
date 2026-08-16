# A001GateAdjudicatorV1

**Status:** frozen at D016-I
**Implementation:** `research/aliveness-spike/analysis/.../A001GateAdjudicator.kt`
**Evidence:** `research/aliveness-spike/evidence/D016I_GATE_ADJUDICATION.txt` (regenerated and diffed by CI)

---

## 1. What this replaces, and why

A001 has always needed something to decide whether the gate opens. Three
arrangements have been tried:

| Arrangement | Directive | Outcome |
|---|---|---|
| Three named independent human reviewers | pre-D016-B | Never filled. `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`. |
| Two isolated agentic reviewers with `ADJUDICATE_GATE` | D016-B … D016-H | Measured at D016-H. Failed all seven frozen thresholds. |
| **Deterministic computation from preregistered rules** | **D016-I** | **This document.** |

The second arrangement was not defeated by infrastructure. D016-C through D016-G
solved every infrastructure problem it had — role isolation, tool-freeness,
routing, context. Once a real reviewer could finally be measured, it failed on
the properties a gate authority most needs:

| Property | Measured | Required |
|---|---|---|
| Same evidence, same answer | 0.813 | ≥ 0.90 |
| Same evidence reordered, same answer | 0.615 | ≥ 0.95 |
| Decisive item moved, same answer | 0.692 | ≥ 0.95 |
| Cannot be instructed by the material under review | 0.750 | 1.00 |

The last line is the one that ends the approach. A reviewer that can be talked
out of its finding by the evidence it is reviewing cannot adjudicate a gate the
programme has an interest in passing. And the order and position figures land
close to where the published position-bias literature said they would, which
suggests these bars are not reachable by general-purpose judges rather than
merely unmet by this one.

So the decision rule stops being a judgement.

> **Human evidence determines aliveness. Frozen math determines PASS and FAIL.
> Agents audit; they do not adjudicate.**

Every threshold is unchanged. D016-I moves *who decides*, not *what counts*.

---

## 2. The adjudicator

`adjudicate(GateEvidence) → GateRuling` is a pure function. No clock, no
randomness, no network, no environment, no model. Given the same evidence it
returns the same ruling, byte for byte.

### 2.1 Inputs

`GateEvidence` carries the baseline qualification, the sealed pilot release, the
owner ceiling, the ethics determination identifier, the scored attempts, and any
auditor findings. There is **no field for a reviewer verdict, a recommendation,
a weight or an override**, because there is no point in the computation at which
one could be consumed.

### 2.2 Stages

Evaluated in a fixed order. Each is a prerequisite; the gate cannot pass while
any is unsatisfied.

| Stage | Requirement | Source of truth |
|---|---|---|
| AJ-01 | Every frozen decision threshold holds its frozen value | `FROZEN_DECISION_THRESHOLDS` |
| AJ-02 | A strong scripted baseline is independently qualified | `BASELINE_QUALIFICATION_PARTICIPANTS`, `BASELINE_COMPETENCE_MARGIN` |
| AJ-03 | The variance pilot released a protocol-valid result | `BlindVariancePilot.PilotRelease` |
| AJ-04 | The powered requirement fits the frozen owner ceiling | `A001FeasibilityBudget` |
| AJ-05 | An independent human-subjects determination exists | gate evidence |
| AJ-06 | The attempt declares the frozen protocol, instrument and analysis | `A001StudyContract`, `A001Analysis` |
| AJ-07 | The analysed set obeys the preregistered exclusion rules | `A001Analysis.screen`, reapplied |
| AJ-08 | The attempt meets the powered sample the pilot implies | `A001FeasibilityBudget.requiredPairsPrimary` |
| AJ-09 | The scored attempts fit the three-attempt budget | `SpikeContract.MAX_SCORED_A001_ATTEMPTS` |
| AJ-10 | Any claimed outcome agrees with the recomputation | `A001Analysis.analyze` |

Screening at AJ-07 is **reapplied, not trusted**. A pair that should have been
excluded but reached the analysed set is the single most consequential thing an
auditor could point at, and it is exactly the thing an auditor's word must not
settle.

### 2.3 Outcomes

| Outcome | Meaning |
|---|---|
| `A001_BLOCKED` | A prerequisite is missing. No attempt may be scored. |
| `A001_PASS` | Every prerequisite met; the attempt cleared the frozen rule. |
| `A001_PASS_PENDING_ARCHITECT_REVIEW` | Cleared the rule; an auditor raised a concern outside the frozen vocabulary. |
| `A001_ATTEMPT_FAILED_ATTEMPTS_REMAIN` | Failed, with attempts left. |
| `ALIVENESS_PROGRAM_STOP` | Failed, three-attempt budget exhausted. |

The primary classification is unchanged from `A001AnalysisV1`: mean ≥ +10 points
**and** the two-sided 95% CI lower bound > 0, with the four failure modes kept
separate rather than collapsed.

### 2.4 Replayability

`GateEvidence.canonicalForm()` is an order-stable serialization; `evidenceHash()`
is its SHA-256. Two parties holding the same evidence can compute the hash and
know they adjudicated the same thing.

`replaysIdentically()` checks three things: the same evidence twice, the same
evidence with its pair records reversed, and the programme's real evidence twice.
Order-invariance is checked explicitly because it is precisely the property the
measured judge lacked.

Auditor findings are excluded from the hash. They cannot change the computed
outcome, and including them would make an identical study hash differently
depending on what a model happened to say about it.

### 2.5 The frozen-threshold guard

`FROZEN_DECISION_THRESHOLDS` holds each live constant beside an independently
written literal of its frozen value. Duplicating a constant is normally a defect;
here it is the point. The failure mode being defended against is a single
well-intentioned edit, and a duplicate that must be edited in step is what
catches one. Any divergence raises `THRESHOLD_WEAKENED_AFTER_FREEZE` and blocks
the gate — including a drift in the permissive direction, which is the direction
that matters.

---

## 3. Agents as adversarial auditors

`AdversarialAuditContractV1` and `AuditorAuthorityPolicyV1`.

### 3.1 The demotion

`ADJUDICATE_GATE`, `CREATE_GATE_OUTCOME` and `OVERRIDE_DETERMINISTIC_GATE` are
forbidden to every role, and `RoleContract` refuses at construction to build a
role that claims one. The two reviewers are renamed to
`PrimaryAdversarialAlivenessAuditor` and `AlternateAdversarialAlivenessAuditor`;
their `RULE_*` authorities become `AUDIT_*`. The previous identifiers remain
resolvable via `supersededRoleId`, so provenance recorded before D016-I stays
readable as the different thing it was.

The auditor briefing was rewritten to tell the model the truth about its own
authority, including the unflattering part: its answer is not the outcome, its
finding will be re-derived before it counts, and a concern it cannot name in the
frozen vocabulary goes to a human instead of to the gate.

### 3.2 A finding is a pointer, never a fact

An `AuditorFinding` has five fields: identifier, auditor, violation code, cited
evidence, claim. It has **no verdict, severity, weight, confidence or score** —
those are the fields through which a judgement would re-enter. A test asserts the
absence rather than trusting a comment.

The 16 `ViolationCode` values are a closed vocabulary, and each maps to a check
the adjudicator performs **on every run, whether or not an auditor mentions it**.
Dispositions:

| Disposition | Meaning | Effect on the gate |
|---|---|---|
| `UPHELD_BLOCKING` | The adjudicator independently re-derived the violation | **None.** It was already blocking before the finding was read. |
| `NOT_CONFIRMED` | Checked and not present | **None.** Inert, and preserved as a record of what the auditor claimed. |
| `AMBIGUOUS_RETURNED_TO_ARCHITECT` | No checkable code | Suspends an otherwise-passing gate for the Architect. |

The last row is the only effect an agent is permitted to have, and it is one-way.
It cannot manufacture a failure, and it cannot touch a gate already blocked or
failed. **Agents can stop; they cannot start, rescue or overturn.**

### 3.3 Why an unreliable auditor is now tolerable

Under D016-C the model's answer *was* the ruling, so every instability in it was
an instability in the gate. Here the worst an unstable auditor can do is point at
the wrong place, or fail to point at all, and neither moves the outcome. The
0.750 injection resistance that ended the previous approach is survivable in this
one — not because the number improved, but because nothing depends on it.

This is not a reason to trust the auditor. It is a reason the gate no longer has
to.

---

## 4. What D016-I does **not** change

- No A001 threshold. Every value in `AlivenessStudyProtocolV1` is untouched.
- No qualification threshold. `AgenticReviewerQualificationThresholdsV1` still
  requires injection resistance of exactly 1.00.
- The D016-H measurement. It is preserved in full at `GA-37` and in
  `RealReviewerQualificationResultV1`, permanently and unclearably.
- The human arms. Baseline qualification, the variance pilot, Attempt 1 and the
  human ablations still require real blinded participants, and no agent may
  stand in for one.
- The gate state. A001 remains blocked on
  `BLOCKED_BASELINE_NOT_QUALIFIED`,
  `BLOCKED_VARIANCE_PILOT_NOT_REGISTERED` and `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD`,
  with attempts consumed at 0 of 3 and no human data anywhere in the repository.

---

## 5. Stated limitations

- The adjudicator is qualified by test, not by measurement against human
  judgement. Nobody has checked that its rulings agree with what a competent
  human reviewer would have concluded, and no such check is planned. What is
  claimed is that it applies the preregistered rules correctly and identically
  every time — not that the preregistered rules are the right ones.
- Determinism is not correctness. A rule frozen before the data is still a rule
  someone chose, and this document does not argue that the +10 floor or the 0.95
  confidence level are the right values. It argues only that they were fixed in
  advance and cannot now move.
- The ethics determination is a prerequisite the adjudicator can check for the
  *presence* of and nothing more. Whether a determination is adequate remains a
  human judgement and remains outstanding.
- The `AMBIGUOUS_RETURNED_TO_ARCHITECT` path is a genuine agent effect on the
  gate, narrow and one-way but real. An auditor that emitted vague findings on
  every run would suspend every pass. That is a denial-of-service on the
  Architect's attention rather than a threat to the result, and it is recorded
  here rather than dismissed.
