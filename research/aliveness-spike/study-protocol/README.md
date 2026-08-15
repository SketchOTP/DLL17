# A000/A001 research contracts

Disposable research contracts for the Aliveness track. Nothing here is a
production contract, and freezing a mechanism here does not authorize its
R003–R009 production equivalent.

Each document carries one of three statuses:

| Status | Meaning |
|---|---|
| `FROZEN` | Complete and binding. Changing it creates a new version. |
| `READY_FOR_HUMAN_EVIDENCE` | Every decision that can be made without human data is made and frozen; the named empirical values are the only gaps. |
| `BLOCKED_SPEC_*` / `BLOCKED_GOVERNANCE_*` | Cannot be completed at all until a named external input exists. |

| Contract | Status |
|---|---|
| [SpikeExpressionContractV1](SpikeExpressionContractV1.md) | `FROZEN` |
| [SpikeDecisionTraceV1](SpikeDecisionTraceV1.md) | `FROZEN` |
| [MechanismCoalitionSetV2](MechanismCoalitionSetV2.md) | `FROZEN` (v2 under D009; membership changed, groups and value function unchanged) |
| [CoalitionValueFunctionV1](CoalitionValueFunctionV1.md) | `FROZEN` |
| [SpontaneousActionAttributionV1](SpontaneousActionAttributionV1.md) | `FROZEN` |
| [CuriosityBalanceEnvelopeV1](CuriosityBalanceEnvelopeV1.md) | `FROZEN` — thresholds unchanged since D008; result now `NON_EMPTY_FEASIBLE_REGION` |
| [MechanismRevisionD009](MechanismRevisionD009.md) | `FROZEN` |
| [ScriptedPetBaselineV1](ScriptedPetBaselineV1.md) | `FROZEN` (implementation); competence unqualified |
| [MaterialChangeEligibilityV1](MaterialChangeEligibilityV1.md) | `FROZEN` |
| [AlivenessGovernanceAuditV1](AlivenessGovernanceAuditV1.md) | `FROZEN` (v2 under D010: now the activation gate) |
| [AlivenessStudyProtocolV1](AlivenessStudyProtocolV1.md) | `FROZEN` (D010) |
| [GradedAlivenessInstrumentV1](GradedAlivenessInstrumentV1.md) | `FROZEN` (D010) |
| [AlivenessProgramGateV1](AlivenessProgramGateV1.md) | `READY_FOR_HUMAN_EVIDENCE` — success floor now frozen |
| [BaselineQualificationProtocolV1](BaselineQualificationProtocolV1.md) | `READY_FOR_HUMAN_EVIDENCE` |
| [BlindVariancePilotV1](BlindVariancePilotV1.md) | `READY_FOR_HUMAN_EVIDENCE` |
| [A001FeasibilityBudgetV1](A001FeasibilityBudgetV1.md) | `READY_FOR_HUMAN_EVIDENCE` — was `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD` |
| [ParticipantInformationAndConsentV1](ParticipantInformationAndConsentV1.md) | `READY_FOR_HUMAN_EVIDENCE` |
| [DataHandlingAndPrivacyV1](DataHandlingAndPrivacyV1.md) | `READY_FOR_HUMAN_EVIDENCE` |
| [IndependentReviewOnboardingV1](IndependentReviewOnboardingV1.md) | `READY_FOR_HUMAN_EVIDENCE` |
| [IndependentReviewRosterV1](IndependentReviewRosterV1.md) | `SUPERSEDED` by AgenticReviewHarnessV1 (D016-C); history retained |
| [AgenticReviewHarnessV1](AgenticReviewHarnessV1.md) | `BLOCKED_AGENTIC_REVIEW_HARNESS_UNQUALIFIED` — measured at D016-H and failed all seven thresholds; `SUPERSEDED` as gate authority by A001GateAdjudicatorV1 (D016-I), retained as the adversarial-auditor harness |
| [A001GateAdjudicatorV1](A001GateAdjudicatorV1.md) | `FROZEN` — the deterministic A001 gate authority (D016-I) |

Under D010 the attempt-specific protocol, the instrument, the participant
materials and the reviewer onboarding package were authored and frozen. What
remains blocked still needs something other than code: a registered variance
pilot, the SD it releases, an independent baseline qualification, and — since
D016-B replaced the human reviewer roles with agentic ones — two credentialed
heterogeneous reviewer models against which the frozen qualification thresholds
can actually be measured. The owner resource ceiling was frozen at D016-A.

The agentic roles govern and operate the study. They do not replace the people
whose perception A001 measures, and every human arm is still blocked on real
blinded participants and on an independent human-subjects determination.
