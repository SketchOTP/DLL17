# StateTransitionMatrix

- Registry version: `V1`
- Status: `POPULATED_R001`
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

R001 introduces exactly one multi-state controller: the Class W staged witnessed
interaction. It is listed here because it has real states, real guards and a real
durability boundary, and because the whole point of the R001.6 and R001.9 work
packages is that its transitions are not implicit.

### Controller: `WitnessedInteractionPipeline`

| Property | Value |
|---|---|
| Controller | `WitnessedInteractionPipeline` (`core-state`) |
| States | `RECEIVED`, `CLASSIFIED`, `CANDIDATE_COMPUTED`, `RECEIPT_BUILT`, `DURABLE_WRITE_STARTED`, `DURABLY_ACKNOWLEDGED`, `PUBLISHED`, `FINAL_PRESENTED`, `CANCELLED` |
| Failure / terminal states | `CANCELLED` is absorbing for that interaction. It is reached whenever the durable write does not acknowledge |
| Timeout source | None. R001 defines no timeout; a timeout would be a parameter and no device evidence exists to set one |
| Preemption | None. One interaction is staged at a time in R001 |

| From | To | Trigger | Guard | Durability boundary |
|---|---|---|---|---|
| — | `RECEIVED` | input arrives | none | none; Class E acknowledgement is immediate and noncanonical |
| `RECEIVED` | `CLASSIFIED` | normalization completes | event is well-formed | none |
| `CLASSIFIED` | `CANDIDATE_COMPUTED` | reducer runs on a copy | durability class is `WITNESSED` | **candidate is not published** |
| `CLASSIFIED` | `PUBLISHED` | reducer runs | durability class is `EPHEMERAL` or `ORDINARY` | ordinary frame appended |
| `CANDIDATE_COMPUTED` | `RECEIPT_BUILT` | receipt constructed | pre-input state hash captured | none |
| `RECEIPT_BUILT` | `DURABLE_WRITE_STARTED` | append begins | **presentation token consumed here** | token spent before the write, not after |
| `DURABLE_WRITE_STARTED` | `DURABLY_ACKNOWLEDGED` | append returns normally | frame fully written | acknowledged |
| `DURABLE_WRITE_STARTED` | `CANCELLED` | append interrupted | nothing acknowledged | nothing recoverable, by design |
| `DURABLY_ACKNOWLEDGED` | `PUBLISHED` | candidate assigned to published state | commit frame acknowledged | required |
| `PUBLISHED` | `FINAL_PRESENTED` | final semantic reaction | `durableCommitAcknowledged(commitFrame)` | required; the sink refuses otherwise |

Two transitions are deliberately absent and their absence is the invariant:

- There is **no** edge from `CANDIDATE_COMPUTED` or `RECEIPT_BUILT` to
  `PUBLISHED`. Material state cannot become visible before durable
  acknowledgement.
- There is **no** edge from any state into `FINAL_PRESENTED` on recovery.
  Recovery restores canonical history and emits no presentation at all, because
  the presentation token was already consumed when the write started.

The Android shell still has no state machine: it renders a single fixed surface
and holds no state that survives the activity.
