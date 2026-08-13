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
| [MechanismCoalitionSetV1](MechanismCoalitionSetV1.md) | `FROZEN` |
| [CoalitionValueFunctionV1](CoalitionValueFunctionV1.md) | `FROZEN` |
| [SpontaneousActionAttributionV1](SpontaneousActionAttributionV1.md) | `FROZEN` |
| [CuriosityBalanceEnvelopeV1](CuriosityBalanceEnvelopeV1.md) | `FROZEN` |
| [ScriptedPetBaselineV1](ScriptedPetBaselineV1.md) | `FROZEN` (implementation); competence unqualified |
| [MaterialChangeEligibilityV1](MaterialChangeEligibilityV1.md) | `FROZEN` |
| [AlivenessGovernanceAuditV1](AlivenessGovernanceAuditV1.md) | `FROZEN` |
| [AlivenessProgramGateV1](AlivenessProgramGateV1.md) | `READY_FOR_HUMAN_EVIDENCE` |
| [BaselineQualificationProtocolV1](BaselineQualificationProtocolV1.md) | `READY_FOR_HUMAN_EVIDENCE` |
| [BlindVariancePilotV1](BlindVariancePilotV1.md) | `READY_FOR_HUMAN_EVIDENCE` |
| [A001FeasibilityBudgetV1](A001FeasibilityBudgetV1.md) | `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD` |
| [IndependentReviewRosterV1](IndependentReviewRosterV1.md) | `BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED` |

`AlivenessStudyProtocolV1` is not authored here. It is attempt-specific, it
depends on `A001FeasibilityBudgetV1`, and D008 did not authorize it.
