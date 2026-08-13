# EventCatalog

- Registry version: `V1`
- Status: `SCAFFOLD_EMPTY`
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

None. R000 defines no events.

Entries may not be added until `DeterminismContractV1` is frozen and R001 opens.
