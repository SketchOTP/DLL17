# ParameterRegistry

- Registry version: `V1`
- Status: `POPULATED_R001`
- Created: R000 (directive D004)
- Owner gate: per-parameter, recorded in the `Owner gate` column

Authoritative list of every numeric parameter that executable logic depends on.

**No-guessing rule.** Words such as "initial", "target", "approximately",
"near" or "provisional" in any specification do not authorize the implementer to
select a number. A named value must exist here, with an exact value, before any
executable logic depends on it. Qualification may change a tuning target only
through an explicit parameter-version update with fixtures. Invariants may not be
tuned away.

## Required columns

| Column | Meaning |
|---|---|
| Parameter ID | Immutable identifier |
| Units | Exact unit |
| Exact current value | The single authoritative value |
| Status | `FROZEN_INVARIANT`, `QUALIFICATION_TARGET`, `SPECIES_TUNING`, or `PLATFORM_MEASURED` |
| Owner gate | Phase or gate that may change the value |
| Legal change process | How a change is authorized |
| Migration consequence | Effect on persisted state and replay |

## Entries

Every number below is a **determinism constant**, not an organism parameter. R001
still implements no physiological equation, threshold or rate, so it still
depends on no tuning value. These entries exist because the no-guessing rule
applies to any number executable logic depends on, including mechanism
constants, and because a later phase must be able to see which of them are
frozen forever and which were chosen by this phase.

| Parameter ID | Units | Exact current value | Status | Owner gate | Legal change process | Migration consequence |
|---|---|---|---|---|---|---|
| `FIXED_POINT_SCALE` | fixed-point units per whole unit | `1000000` | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump plus regenerated golden vectors | Every persisted `Fixed64` changes meaning; full migration |
| `FIXED_POINT_MAX` | `Fixed64` raw | `9223372036854775807` | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump | Saturation results change |
| `FIXED_POINT_MIN` | `Fixed64` raw | `-9223372036854775807` | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump | Saturation results change; symmetry with MAX is load-bearing |
| `ROUNDING_MODE` | mode ID | `ROUND_HALF_AWAY_FROM_ZERO_V1` | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump | Every rescaling operation changes; full replay divergence |
| `STATE_HASH_ALGORITHM` | algorithm ID | `HASH_SHA256_V1` | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump | Every stored state hash changes |
| `STATE_HASH_DIGEST_BYTES` | bytes | `32` | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump | Stored digests change width |
| `PRNG_ALGORITHM` | algorithm ID | `PRNG_SPLITMIX64_V1` | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump | Every substream output changes |
| `PRNG_GAMMA` | `i64` | `-7046029254386353131` (unsigned `0x9E3779B97F4A7C15`) | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump | Every draw changes |
| `SUBSTREAM_MIXER` | mixer ID | `SUBSTREAM_DERIVE_V1` | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump | Every substream seed changes |
| `CANONICAL_ENVELOPE_FORMAT_VERSION` | version ordinal | `1` | `FROZEN_INVARIANT` | `DeterminismContractV1` | New envelope format version | Older artifacts refused unless migrated |
| `DETERMINISM_CONTRACT_VERSION` | version ordinal | `1` | `FROZEN_INVARIANT` | `DeterminismContractV1` | New contract artifact | Artifacts from another version are refused, never guessed |
| `CANONICAL_IDENTIFIER_MAX_BYTES` | bytes | `64` | `FROZEN_INVARIANT` | `DeterminismContractV1` | Contract version bump | Longer identifiers become encodable; older artifacts unaffected |
| `RANDOM_DRAW_BOUND` | draw range, exclusive upper bound | `1000001` | `QUALIFICATION_TARGET` | R001, then the phase that first draws for organism purposes | Change requires new golden vectors for every fixture that draws | Every `DRAW_RANDOM` fold changes; replay divergence |
| `LOOKUP_UNIT_RAMP_STEPS` | intervals | `256` | `QUALIFICATION_TARGET` | R001 | Regenerate the table and its digest | Table digest changes; verification fails until regenerated |
| `PANIC_WITNESS_RECORD_BYTES` | bytes | `24` | `FROZEN_INVARIANT` | R001 | Format version bump | Older witness records become unreadable; they are safe to lose |
| `PANIC_WITNESS_WRITE_P99_NS_TENSOR` | nanoseconds | `12614` | `PLATFORM_MEASURED` | re-measured whenever the supported-device matrix changes | New measurement on the device matrix | None; noncanonical |
| `PANIC_WITNESS_WRITE_P99_NS_X86_EMULATOR` | nanoseconds | `16215` | `PLATFORM_MEASURED` | re-measured whenever the supported-device matrix changes | New measurement on the device matrix | None; noncanonical |
| `PANIC_WITNESS_ATTEMPT_DEADLINE_NS` | nanoseconds | **`NOT ESTABLISHED`** | `PLATFORM_MEASURED` | the phase that first writes a panic witness in production | Must be chosen from measured device behaviour, never assumed | None; noncanonical |

Two of these were chosen by this phase rather than inherited from the canonical
pages, and are marked `QUALIFICATION_TARGET` rather than `FROZEN_INVARIANT` for
that reason:

- `RANDOM_DRAW_BOUND` quantizes a raw 64-bit draw into `[0.0, 1.0]` fixed-point
  before it touches canonical state. A raw draw folded into an accumulator would
  saturate almost immediately, and saturation inside a normal operating range is
  a qualification failure. The bound is exclusive, so the inclusive value range
  is `0 .. 1000000`.
- `LOOKUP_UNIT_RAMP_STEPS` is the resolution of the single mechanism-proof lookup
  table. It has no behavioural meaning.

Neither is a physiological budget, a rate, a threshold or a tuning value. No
organism parameter exists yet, and none may be added until the phase that owns
the subsystem opens.

### The panic-witness deadline is measured, not assumed

The architect's 2026-08-07 correction is explicit that `PlatformPanicWitness`
has **no** universal 2.0 ms requirement and that its attempt deadline must be
established empirically on the supported-device matrix. R001 therefore records
what two targets actually did — 2,000 samples each, on Tensor G4 hardware and on
the x86 emulator — and deliberately leaves
`PANIC_WITNESS_ATTEMPT_DEADLINE_NS` as `NOT ESTABLISHED`.

Measuring a write cost and choosing a deadline are different acts. The
measurements are evidence; a deadline is a commitment that also has to account
for the platform's behaviour during an actual critical suspend, which is not
what this benchmark observed. Writing a number down now, on the strength of a
benchmark taken while the process was healthy, would be exactly the fabricated
constant the correction was issued to prevent.

### Class O batching is deliberately unfrozen

Implementation Plan E2E work package R001.9 states that Class O cadence and the
maximum tolerated uncommitted window are measured and frozen *by qualification*.
R001 implements the durability classes but freezes neither value, because both
are properties of real persistence on a real device, and R001's durable medium
is an in-process byte log by design. They are recorded as `NOT ESTABLISHED` in
`docs/release/DEVICE_AND_RESOURCE_BUDGETS.md` and belong to R002.

The R000 toolchain and identity values remain out of scope here; they are frozen
in `docs/architecture/ProjectIdentityBuildContractV1.md`.


---

## R002 additions

Every value below is quoted from `ContinuityDurabilityContractV1`. Values the
canonical architecture states directly are marked as such in the rationale
column; values this phase selected are `QUALIFICATION_TARGET` so that device
evidence can move them without a contract break.

| Parameter ID | Units | Exact current value | Status | Owner gate | Legal change process | Migration consequence |
|---|---|---|---|---|---|---|
| `WALL_ELAPSED_SKEW_TOLERANCE_MILLIS` | ms | `120000` | `QUALIFICATION_TARGET` | R002 | Contract version bump plus regenerated R002 vectors | Changes which intervals classify as anomalous |
| `BLIND_DECAY_CREDIT_MAX_MILLIS` | ms | `14400000` | `QUALIFICATION_TARGET` | R002 | As above | Persisted credit is reclamped |
| `BLIND_CREDIT_REPLENISH_DIVISOR` | ratio | `6` | `QUALIFICATION_TARGET` | R002 | As above | Changes the credit earned per verified hour |
| `BLIND_CREDIT_REPLENISH_CAP_MILLIS` | ms | `14400000` | `QUALIFICATION_TARGET` | R002 | As above | Window accounting is reset |
| `BLIND_CREDIT_REPLENISH_WINDOW_MILLIS` | ms | `86400000` | `QUALIFICATION_TARGET` | R002 | As above | As above |
| `BOOT_VELOCITY_WINDOW_MILLIS` | ms | `3600000` | `QUALIFICATION_TARGET` | R002 | As above | None |
| `BOOT_VELOCITY_MAX_BOOTS` | count | `5` | `QUALIFICATION_TARGET` | R002 | As above | None |
| `DEBT_GLOBAL_CAP_BASELINE_EQUIV_MILLIS` | baseline-equivalent ms | `259200000` | `FROZEN_INVARIANT` | canonical architecture | Architect amendment | Outstanding debt is reclamped and excess forgiven |
| `DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS` | baseline-equivalent ms | `900000` | `QUALIFICATION_TARGET` | R002 | Contract version bump | Changes collection rate only |
| `DEBT_PER_VERIFIED_DAY_CAP_BASELINE_EQUIV_MILLIS` | baseline-equivalent ms | `21600000` | `QUALIFICATION_TARGET` | R002 | As above | As above |
| `DEBT_RETENTION_HORIZON_MILLIS` | ms of verified time | `2592000000` | `QUALIFICATION_TARGET` | R002 | As above | Changes when debt is forgiven |
| `DEBT_SAFETY_FLOOR` | `Fixed64` fraction | `0.200000` | `QUALIFICATION_TARGET` | R002 | As above | Canonical architecture states "initial qualification target near 20%" |
| `DEBT_ABUNDANCE_REARM` | `Fixed64` fraction | `0.800000` | `QUALIFICATION_TARGET` | R002 | As above | Canonical architecture states "initial target near 80%" |
| `DEBT_REARM_STABILITY_MILLIS` | ms of verified time | `3600000` | `QUALIFICATION_TARGET` | R002 | As above | None |
| `DEBT_REARM_GRACE_MILLIS` | ms | `1800000` | `QUALIFICATION_TARGET` | R002 | As above | None |
| `DEBT_POST_REVEAL_COLLAPSE_MARGIN` | `Fixed64` fraction | `0.050000` | `QUALIFICATION_TARGET` | R002 | As above | None |
| `MODE_A_MAX_MILLIS` | ms | `300000` | `FROZEN_INVARIANT` | canonical architecture | Architect amendment | Changes mode selection |
| `MODE_B_MAX_MILLIS` | ms | `259200000` | `FROZEN_INVARIANT` | canonical architecture | Architect amendment | Changes mode selection |
| `CHUNK_TIER_1_SIZE_MILLIS` | ms | `60000` | `FROZEN_INVARIANT` | canonical architecture | Architect amendment | Changes chunk boundaries and therefore rounding |
| `CHUNK_TIER_2_SIZE_MILLIS` | ms | `300000` | `FROZEN_INVARIANT` | canonical architecture | Architect amendment | As above |
| `CHUNK_TIER_3_SIZE_MILLIS` | ms | `900000` | `FROZEN_INVARIANT` | canonical architecture | Architect amendment | As above |
| `MODE_C_MAX_PASSIVE_DEVELOPMENT_MILLIS` | ms | `259200000` | `FROZEN_INVARIANT` | canonical architecture | Architect amendment | Changes the passive development cap |
| `RECONCILIATION_SLICE_CHUNKS` | chunks | `64` | `QUALIFICATION_TARGET` | R002 | Contract version bump | None — slicing cannot change the result |
| `CLASS_O_COMMIT_CADENCE_MILLIS` | ms | `500` | `QUALIFICATION_TARGET` | R002 | Contract version bump | None. Midpoint of the canonical 250–1000 ms band; was `NOT ESTABLISHED` at R001 |
| `CLASS_O_MAX_UNCOMMITTED_WINDOW_MILLIS` | ms | `1000` | `QUALIFICATION_TARGET` | R002 | As above | None. Upper edge of the same band |
| `PANIC_WITNESS_ATTEMPT_DEADLINE_MILLIS` | ms | `20` | `QUALIFICATION_TARGET` | R002 | As above | None. Derived from the R001 measured p99 of 16,215 ns with roughly 1200× headroom |
| `JOURNAL_BYTE_BUDGET` | bytes | `8388608` | `QUALIFICATION_TARGET` | R002 | As above | Changes when generations flip |
| `EMERGENCY_DURABILITY_RESERVE_BYTES` | bytes | `65536` | `QUALIFICATION_TARGET` | R002 | As above | Changes when read-only survival begins |
| `THERMAL_REENTRY_HYSTERESIS_MILLIS` | ms | `60000` | `QUALIFICATION_TARGET` | R002 | As above | None |
| `RECOVERY_STALE_WARNING_MILLIS` | ms | `86400000` | `QUALIFICATION_TARGET` | R002 | As above | None. Canonical architecture states "near 24 hours" |
| `RECOVERY_CRITICAL_WARNING_MILLIS` | ms | `604800000` | `QUALIFICATION_TARGET` | R002 | As above | None. Canonical architecture states "near seven days" |
| `FIXTURE_RESERVE_DRAIN_PER_MINUTE` | `Fixed64` fraction per minute | `0.001000` | `QUALIFICATION_TARGET` | R002 | Replaced by R003 | **R002 fixture only.** Not physiology; replaced by `SpeciesBaselineV1` behind the A001 gate |
| `FIXTURE_PASSIVE_DEVELOPMENT_PER_MINUTE` | `Fixed64` progress per minute | `0.000100` | `QUALIFICATION_TARGET` | R002 | Replaced by R003 | **R002 fixture only.** As above |

### Still `NOT ESTABLISHED` after R002

R001's three unestablished parameters are now frozen above. What remains
unestablished is everything belonging to phases that have not opened: every
species baseline, every physiological rate and threshold, and every recovery
cryptography parameter, which is blocked behind the unfrozen
`RecoveryCryptographyContractV1`.
