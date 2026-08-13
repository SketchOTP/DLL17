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


---

## R002 additions

### `DurabilityAdmissionState` (`core-continuity`)

| Property | Value |
|---|---|
| States | `OPEN`, `PRESSURE`, `READ_ONLY_SURVIVAL`, `STORAGE_FAULT` |
| Legal transitions | `OPEN ↔ PRESSURE` on the soft threshold; `PRESSURE → READ_ONLY_SURVIVAL` on the reserve floor; any state `→ STORAGE_FAULT` on a failed emergency commit or failed self-test; `STORAGE_FAULT → OPEN` only after a self-test **and** a successful checkpoint write |
| Guards | Durable capacity only. Admission never consults the reducer, the UI or the platform |
| Timeout source | None. Admission is level-triggered by capacity, not by elapsed time |
| Preemption | `STORAGE_FAULT` preempts everything |
| Failure / terminal states | `STORAGE_FAULT` is absorbing until a self-test and checkpoint both succeed |
| Durability boundary | `DurabilitySafeHoldEntered` must be durable before publication stops; `DurabilitySafeHoldExited` must be durable before reconciliation begins |

Entry ordering is normative: close admission → attempt the emergency-reserve
frame → commit `DurabilitySafeHoldEntered` → stop canonical advancement →
present `TEMPORAL_DESYNC`. Exit ordering is normative: self-test → commit
`DurabilitySafeHoldExited` → close the interaction gate → reconcile from the last
durable anchor → commit → reveal → reopen admission.

### `PlatformProtectionState` (`core-continuity`)

| Property | Value |
|---|---|
| States | `NORMAL`, `RESOURCE_SHED`, `PLATFORM_DEEP_SUSPEND`, `PLATFORM_RECOVERY` |
| Legal transitions | `NORMAL ↔ RESOURCE_SHED` on thermal or power pressure; either `→ PLATFORM_DEEP_SUSPEND` on a qualified critical condition; `PLATFORM_DEEP_SUSPEND → PLATFORM_RECOVERY` only after the hysteresis interval; `PLATFORM_RECOVERY → NORMAL` after the committed reveal |
| Guards | `THERMAL_REENTRY_HYSTERESIS_MILLIS` below the reentry threshold before recovery may begin |
| Timeout source | The hysteresis interval, measured by the caller and supplied explicitly |
| Preemption | Platform protection preempts all in-world rendering |
| Failure / terminal states | None. A failed anchor and a failed witness both leave the last durable anchor authoritative |
| Durability boundary | At most **one** `PlatformDeepSuspendEntered` attempt and at most **one** panic-witness attempt. No retry loop under thermal or power pressure |

### `DebtState` (`core-continuity`)

| Property | Value |
|---|---|
| States | `IDLE`, `ACCRUED`, `COLLECTING`, `PAUSED_LOW_RESERVE`, `FORGIVEN` |
| Legal transitions | `IDLE → ACCRUED` on accrual; `ACCRUED ↔ COLLECTING` in eligible Mode B/C chunks; either `→ PAUSED_LOW_RESERVE` at the safety floor; `PAUSED_LOW_RESERVE → COLLECTING` only after abundance stability, the grace interval, and a later eligible chunk; any `→ FORGIVEN` past the retention horizon |
| Guards | Both reserves at or above the abundance threshold, held for the stability interval; the rearm effective time strictly in the future |
| Timeout source | Verified time only. Wall-clock movement drives nothing here |
| Preemption | The safety floor preempts collection unconditionally |
| Failure / terminal states | None. Debt is always eventually discharged, by collection or by forgiveness |
| Durability boundary | Blind-credit consumption is Class W and is debited before the reconciled reveal |
