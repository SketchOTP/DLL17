# ParameterRegistry

- Registry version: `V1`
- Status: `SCAFFOLD_EMPTY`
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

None. R000 implements no equation, threshold or rate, so it legitimately
depends on no parameter.

The R000 toolchain and identity values are not parameters in this sense; they
are frozen in `docs/architecture/ProjectIdentityBuildContractV1.md`.
