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


---

## R002 additions — `ContinuityState` (`core-continuity`)

Canonical schema `211`. Serialization order is the order of the rows below.
`reserveA` and `reserveB` are **neutral R002 fixtures**, not physiology: they
exist so that a reconciliation has something observable to act on, and R003
replaces them behind the A001 gate.

| Field ID | Owner module | Type | Units / scale | Bounds | Initial-value rule | Order | Hash | Allowed transitions | Offline behaviour | Migration rule |
|---|---|---|---|---|---|---|---|---|---|---|
| `identity.organismId` | `core-continuity` | `i64` | identifier | any | supplied at genesis | 1 | yes | never changes | unchanged | carried forward |
| `identity.identityEpoch` | `core-continuity` | `i32` | epoch | `>= 1` | `1` | 2 | yes | advanced only by recovery activation | unchanged | carried forward |
| `identity.lineageHash` | `core-continuity` | 32 bytes | digest | fixed length | derived from organism ID | 3 | yes | never changes | unchanged | carried forward |
| `identity.deviceFingerprint` | `core-continuity` | `i64` | identifier | any | active device key | 4 | yes | changes only on recovery activation | unchanged | carried forward |
| `identity.quarantined` | `core-continuity` | `bool` | — | `0` or `1` | `false` | 5 | yes | `QUARANTINE_ENTERED` only | absorbing; no event is accepted afterwards | carried forward |
| `identity.lastProtectedSequence` | `core-continuity` | `i64` | sequence | non-decreasing | `0` | 6 | yes | `SNAPSHOT_CREATED` | unchanged | reset to `0` |
| `identity.lastProtectedVerifiedMillis` | `core-continuity` | `i64` | ms | non-negative | `0` | 7 | yes | `SNAPSHOT_CREATED` | unchanged | reset to `0` |
| `wallClockAgeMillis` | `core-continuity` | `i64` | ms | non-negative | `0` | 8 | yes | verified time, blind credit | advances only from qualified or credited time | carried forward |
| `activeExperienceTicks` | `core-continuity` | `i64` | ticks | non-negative | `0` | 9 | yes | `ACTIVE_EXPERIENCE_ADVANCED` | never advances offline | reset to `0` |
| `developmentalProgress` | `core-continuity` | `Fixed64` | progress units | non-negative | `0` | 10 | yes | verified passive development only | capped per absence; zero from blind or anomalous time | reset to `0` |
| `circadianPhase` | `core-continuity` | `Fixed64` | phase | `[0, 1)` | `0` | 11 | yes | any qualified or credited elapsed time | wraps within the 24-hour period | reset to `0` |
| `verifiedTimeTotalMillis` | `core-continuity` | `i64` | ms | non-negative | `0` | 12 | yes | `VERIFIED_TIME_ADVANCED` only | never advanced by blind credit | reset to `0` |
| `reserveA` | `core-continuity` | `Fixed64` | fraction of capacity | `[0, 1]` clamped | `1.0` | 13 | yes | metabolism, debt adjustment, restoration | drains per reconciliation chunk | supplied by migration |
| `reserveB` | `core-continuity` | `Fixed64` | fraction of capacity | `[0, 1]` clamped | `1.0` | 14 | yes | as `reserveA` | as `reserveA` | supplied by migration |
| `anchor` | `core-continuity` | record (schema `210`) | — | sequence non-decreasing | genesis anchor | 15 | yes | `ANCHOR_WRITTEN` | the point recovery resumes from | reset to genesis |
| `credit` | `core-continuity` | record | ms and counters | `<= 4 h` available | zero credit | 16 | yes | replenish, consume, boot observation | the only currency an unverifiable gap can spend | reset to zero |
| `debt` | `core-continuity` | record | baseline-equivalent ms | `<= 72 h` outstanding | idle | 17 | yes | accrue, collect, pause, rearm, forgive | collected only in Mode B/C chunks | reset to idle |
| `admissionState` | `core-continuity` | enum `i32` | — | registry ordinal | `OPEN` | 18 | yes | admission controller | survives restart | reset to `OPEN` |
| `presentationState` | `core-continuity` | enum `i32` | — | registry ordinal | `RECOVERY_RECONCILIATION` | 19 | yes | hold entry and exit | survives restart | reset |
| `platformState` | `core-continuity` | enum `i32` | — | registry ordinal | `NORMAL` | 20 | yes | platform controller | survives restart | reset to `NORMAL` |
| `safeHoldActive` | `core-continuity` | `bool` | — | `0` or `1` | `false` | 21 | yes | hold entry and exit | survives restart | reset to `false` |
| `generationId` | `core-continuity` | `i64` | generation | strictly increasing | `1` | 22 | yes | `GENERATION_FLIPPED` | survives restart | reset to `1` |
| `lastCommitSequence` | `core-continuity` | `i64` | sequence | non-negative | `0` | 23 | yes | every event | survives restart | reset to `0` |
| `gapProvenance` | `core-continuity` | enum `i32` | — | registry ordinal | `NONE` | 24 | yes | provenance labelling | a label only; changes nothing else | reset to `NONE` |

Optional values inside `anchor`, `credit` and `debt` carry an explicit presence
flag rather than a sentinel, so "absent" and "zero" never produce the same bytes.
