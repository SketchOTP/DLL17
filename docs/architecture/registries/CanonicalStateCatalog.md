# CanonicalStateCatalog

- Registry version: `V1`
- Status: `SCAFFOLD_EMPTY`
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

None. R000 defines no canonical state.

Entries may not be added until `DeterminismContractV1` is frozen and R001 opens.
Adding a field here is a canonical-architecture change, not an implementation
detail.
