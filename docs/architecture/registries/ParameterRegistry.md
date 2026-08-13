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
