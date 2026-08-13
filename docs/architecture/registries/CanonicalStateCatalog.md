# CanonicalStateCatalog

- Registry version: `V1`
- Status: `POPULATED_R001`
- Created: R000 (directive D004)
- Owner gate: `DeterminismContractV1` freeze, then R001

Authoritative list of every canonical state field. A field that is not in this
catalog is not canonical state and may not participate in the reducer, the
canonical hash, serialization, or migration.

## Required columns

| Column | Meaning |
|---|---|
| Field ID | Immutable identifier |
| Owner module | Module that owns the field |
| Type | Canonical type, fixed-point where numeric |
| Units / scale | Exact unit and fixed-point scale |
| Bounds | Legal range, saturation behavior |
| Initial-value rule | How the field is initialized at birth |
| Serialization order | Position in the canonical byte layout |
| Hash inclusion | Whether the field enters the canonical state hash |
| Allowed transitions | Legal changes and their sources |
| Offline behavior | Behavior across process death and elapsed real time |
| Migration rule | Behavior when the schema version changes |

## Entries

The fields below are the **R001 determinism kernel**, not an organism. Each one
exists to exercise a frozen determinism property. None carries physiological,
affective or behavioural meaning, and the neutral names are deliberate: naming a
field `hunger` would be an organism decision, and organism decisions belong to
R003 and later behind the A001 gate.

Serialization order is the order of the rows. Every field enters the canonical
state hash; R001 defines no canonical field that is excluded from it.

| Field ID | Owner module | Type | Units / scale | Bounds | Initial-value rule | Order | Hash | Allowed transitions | Offline behaviour | Migration rule |
|---|---|---|---|---|---|---|---|---|---|---|
| `schemaVersion` | `core-state` | `i32` | version ordinal | `1` | frozen at `1` | header | yes | only by migration | unchanged | governs migration |
| `logicalTime` | `core-state` | `i64` | canonical ticks | `0 .. Fixed64.MAX` | `0` at genesis | 1 | yes | `ADVANCE_TIME` only, never decreasing | unchanged by absence; R002 owns elapsed-time reconciliation | carried forward |
| `masterSeed` | `core-state` | `i64` | opaque | any `i64` | supplied at genesis | 2 | yes | never changes after genesis | unchanged | carried forward |
| `randomContractVersion` | `core-state` | `i32` | version ordinal | `1` | `DeterminismContractV1` version | 3 | yes | only by contract version bump | unchanged | carried forward |
| `numericA` | `core-state` | `Fixed64` | dimensionless, scale `1_000_000` | `Fixed64.MIN .. MAX`, saturating | `0` at genesis | 4 | yes | `APPLY_DELTA`, `APPLY_INTERPOLATION` | unchanged | `0` when introduced |
| `numericB` | `core-state` | `Fixed64` | dimensionless, scale `1_000_000` | `Fixed64.MIN .. MAX`, saturating | `0` at genesis | 5 | yes | `APPLY_DECAY`, `DRAW_RANDOM` | unchanged | `0` when introduced |
| `materialUnits` | `core-state` | `Fixed64` | dimensionless, scale `1_000_000` | `Fixed64.MIN .. MAX`, saturating | `0` at genesis | 6 | yes | `MATERIAL_INTERACTION` (Class W only) | unchanged | `0` when introduced |
| `lastCommitSequence` | `core-state` | `i64` | frame sequence | `0 ..` | `0` at genesis | 7 | yes | set to the folded event's logical ID | unchanged | `0` when introduced |
| `substreams` | `core-crypto` | canonical map of `RandomSubstream` | 24 bytes per entry | one entry per registered domain | domains 1 and 2 derived at genesis | 8 | yes | `DRAW_RANDOM` advances one counter; insertion is additive | counters survive process death | new domains derived, never drawn |

Notes that matter for later phases:

- `substreams` is serialized as a **canonical map keyed by domain ID**, sorted by
  serialized key bytes. It is never a `Map` in canonical code: `HashMap`
  iteration order is not guaranteed to agree between a desktop JDK and ART.
- `materialUnits` is the only field a Class W transition may touch, which is what
  makes the commit visibility invariant testable on a single field.
- `Long.MIN_VALUE` is not a legal value for any `Fixed64` field. The range is
  symmetric so that negation is total.

Adding a field here is a canonical-architecture change, not an implementation
detail.
