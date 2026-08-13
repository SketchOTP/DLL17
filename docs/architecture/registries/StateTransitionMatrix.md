# StateTransitionMatrix

- Registry version: `V1`
- Status: `SCAFFOLD_EMPTY`
- Created: R000 (directive D004)
- Owner gate: the phase that introduces each controller

Authoritative list of every multi-state controller in the organism. Every legal
and illegal transition must be represented here. Null or implicit destinations
are prohibited.

## Required columns

| Column | Meaning |
|---|---|
| Controller | Named multi-state controller |
| States | Complete state set |
| Legal transitions | Source state, destination state, trigger |
| Guards | Conditions that must hold for the transition |
| Timeout source | Clock or event that drives timeouts |
| Preemption | Which transitions may interrupt which |
| Failure / terminal states | Non-recoverable and absorbing states |
| Durability boundary | What must be persisted before the transition is observable |

## Entries

None. R000 contains no multi-state controller.

The Android shell has no state machine: it renders a single fixed R000 surface
and holds no state that survives the activity.
