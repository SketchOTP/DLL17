# EventCatalog

- Registry version: `V1`
- Status: `POPULATED_R001`
- Created: R000 (directive D004)
- Owner gate: `DeterminismContractV1` freeze, then R001

Authoritative list of every normalized event the canonical reducer accepts.
Event IDs are immutable numeric identifiers and are never reused.

## Required columns

| Column | Meaning |
|---|---|
| Event ID | Immutable numeric identifier |
| Schema version | Version of the event payload schema |
| Source / evidence class | Origin and evidence classification |
| Payload | Exact payload fields and types |
| Quantization | How continuous inputs are quantized before the reducer |
| Ordering / watermark policy | Ordering guarantee and watermark rule |
| Duplicate / late-event rule | Deduplication and late-arrival behavior |
| Durability class | Persistence guarantee required before acknowledgment |
| Reducer handler | Handler that folds the event into canonical state |
| Diagnostics | Diagnostic counters and failure signals |

## Entries

Event IDs are immutable numeric identifiers and are never reused. Events carry
only integers: anything continuous is quantized before it reaches the reducer,
which is what makes cross-architecture byte identity achievable at all.

| Event ID | Name | Schema version | Source / evidence class | Payload | Quantization | Ordering | Duplicate / late rule | Durability class | Reducer handler | Diagnostics |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `ADVANCE_TIME` | 1 | qualification fixture | `operandA` = ticks (`i64`, non-negative) | integral ticks | journal order | refused if negative | Class O | `logicalTime` via `satAdd` | saturation record |
| 2 | `APPLY_DELTA` | 1 | qualification fixture | `operandA` = `Fixed64` delta | fixed-point, scale `1_000_000` | journal order | idempotent only by sequence | Class O | `numericA` via `satAdd` | saturation record |
| 3 | `APPLY_DECAY` | 1 | qualification fixture | `operandA` = `Fixed64` retention in `[0, 1]` | fixed-point, clamped | journal order | idempotent only by sequence | Class O | `numericB` via `satDecay` | saturation record on clamp |
| 4 | `APPLY_INTERPOLATION` | 1 | qualification fixture | `operandA` = target, `operandB` = factor in `[0, 1]` | fixed-point, factor clamped | journal order | idempotent only by sequence | Class O | `numericA` via `satInterpolate` | saturation record on clamp |
| 5 | `DRAW_RANDOM` | 1 | qualification fixture | `operandA` = registered domain ID | draw quantized to `[0, 1.0]` fixed-point | journal order | **not** idempotent: it advances a counter | Class O | `numericB` via `satAdd`, substream counter advanced | unregistered domain is a fault |
| 6 | `MATERIAL_INTERACTION` | 1 | qualification fixture | `operandA` = `Fixed64` material units | fixed-point | journal order | at-most-once via presentation token | **Class W** | `materialUnits` via `satAdd` | receipt digest, presentation token state |

Every event above is a **qualification event**. R001 defines no sensor, no
tactile input and no organism stimulus; those belong to the phases that own
perception and behaviour.

`DRAW_RANDOM` is the one event that is not a pure function of the snapshot's
scalar fields alone — it advances a substream counter. That counter is canonical
state and is serialized, which is exactly why replay reproduces it.

`MATERIAL_INTERACTION` is the only Class W event. It may not become visible
before its commit frame is durably acknowledged, and its final semantic
presentation is at most once.

Executable form: `core-state`, `CanonicalEventType` and `CanonicalReducer`.


---

## R002 additions — `ContinuityEventType` (`core-continuity`)

Canonical schema `212`. Event IDs are immutable and never reused. Every payload
is integers plus an optional anchor record; nothing continuous or textual
reaches the reducer.

Shared columns for every row below: schema version `1`; ordering is journal
order; the duplicate rule is that a sequence is consumed whether or not its
write completed, so a retry is a new event rather than a replayed one;
diagnostics are the saturation record plus the per-fixture continuity evidence.

| Event ID | Name | Source / evidence class | Payload | Durability class | Reducer handler |
|---|---|---|---|---|---|
| 1 | `ANCHOR_WRITTEN` | durable barrier | anchor record | Class W | replaces `anchor` |
| 2 | `CLOCK_ANOMALY_DETECTED` | same-boot comparison | `a` = skew ms | Class O | downgrades confidence to `ANOMALOUS` |
| 3 | `VERIFIED_TIME_ADVANCED` | qualified elapsed time | `a` = ms | Class O | `wallClockAge`, `verifiedTimeTotal`, `circadianPhase` |
| 4 | `BASELINE_METABOLISM_APPLIED` | reconciliation chunk | `a` = baseline-equivalent ms | Class O | drains both reserves |
| 5 | `BLIND_CREDIT_REPLENISHED` | verified elapsed time | `a` = granted, `b` = carry | Class O | credit ledger |
| 6 | `BLIND_CREDIT_CONSUMED` | unverifiable interval | `a` = consumed, `b` = boot identity | Class W | debits credit, advances chronology, drains reserves |
| 7 | `UNRESOLVED_TIME_DEBT_ACCRUED` | unverifiable excess | `a` = accrued, `b` = forgiven at accrual | Class O | debt ledger |
| 8 | `METABOLIC_ADJUSTMENT_APPLIED` | eligible Mode B/C chunk | `a` = baseline-equivalent ms | Class O | bounded collection |
| 9 | `DEBT_PAUSED_LOW_RESERVE` | safety-floor projection | none | Class O | pauses collection, clears rearm |
| 10 | `DEBT_REARM_ARMED` | abundance stability satisfied | `a` = effective-at verified ms | Class O | arms a future rearm |
| 11 | `DEBT_FORGIVEN` | retention horizon | `a` = forgiven | Class O | discharges outstanding debt |
| 12 | `EXTENDED_ABSENCE_RECONCILED` | Mode C completion | `a` = elapsed ms | Class O | record only |
| 13 | `DURABILITY_SAFE_HOLD_ENTERED` | admission threshold | none | Class W | read-only survival, `TEMPORAL_DESYNC` |
| 14 | `DURABILITY_SAFE_HOLD_EXITED` | capacity restored | none | Class W | recovery reconciliation |
| 15 | `PLATFORM_DEEP_SUSPEND_ENTERED` | critical platform condition | `a` = reason class | Class W | deep-suspend state |
| 16 | `PLATFORM_RECOVERY_COMPLETED` | hysteresis satisfied | none | Class W | returns to `NORMAL` |
| 17 | `PLATFORM_STATE_CHANGED` | platform controller | `a` = state ordinal | Class O | platform state |
| 18 | `RECOVERY_PERFORMED` | restart | `a` = resumed sequence | Class W | record only |
| 19 | `RECOVERY_GAP_DECLARED` | cold recovery | `a` = unavailable ms, `b` = new epoch | Class W | epoch, provenance |
| 20 | `SNAPSHOT_CREATED` | verified checkpoint | `a` = sequence | Class W | protected recovery point |
| 21 | `MIGRATION_PERFORMED` | version boundary | `a` = from, `b` = to | Class W | record only |
| 22 | `GENERATION_FLIPPED` | soft threshold | `a` = new generation | Class O | generation ID |
| 23 | `ADMISSION_STATE_CHANGED` | admission controller | `a` = state ordinal | Class O | admission state |
| 24 | `ACTIVE_EXPERIENCE_ADVANCED` | foreground activity | `a` = ticks | Class O | active experience |
| 25 | `RESERVE_RESTORED` | neutral R002 care fixture | `a`, `b` = restorations | Class O | clamped reserve increase |
| 26 | `BOOT_OBSERVED` | boot identity change | `a` = boot identity, `b` = anomaly flag | Class O | boot velocity window |
| 27 | `QUARANTINE_ENTERED` | device-binding failure | none | Class W | absorbing quarantine |
| 28 | `GAP_PROVENANCE_LABELLED` | diagnostics | `a` = provenance ordinal | Class O | label only |
| 29 | `PASSIVE_DEVELOPMENT_APPLIED` | verified absence only | `a` = ms, capped per absence | Class O | developmental progress |
| 30 | `PRESENTATION_STATE_CHANGED` | durability presentation | `a` = state ordinal | Class O | presentation state |
| 31 | `DEBT_ABUNDANCE_STABILITY_UPDATED` | reserve observation | `a` = since verified ms, `b` = presence | Class O | abundance stability tracking |

Class W events are the ones whose visibility is gated on durable acknowledgement:
blind-credit consumption, anchors, hold transitions, suspend and recovery,
checkpoints, migration, quarantine and recovery gaps. Everything else is Class O.

Executable form: `core-continuity`, `ContinuityEventType` and `ContinuityReducer`.
