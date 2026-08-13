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
