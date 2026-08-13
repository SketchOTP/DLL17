# Canonical sources

The authoritative specifications for this program are external Notion pages.
This repository implements them; it does not define them.

| Source | Role |
|---|---|
| `Digital Living Lifeform` (Notion) | Canonical organism architecture. Highest specification authority. |
| `Implementation Plan E2E` (Notion) | Phase plan, module topology, registry requirements, exit gates. |
| `Architect Directives & Outcomes` (Notion) | Issued directives and their recorded outcomes. |
| `.agent/DIRECTIVES.md` | Local append-only mirror of directives issued to this repository. |
| `docs/architecture/ProjectIdentityBuildContractV1.md` | Frozen repository identity, toolchain pins and dependency policy. Derived from the canonical pages under R000. |
| `docs/architecture/DeterminismContractV1.md` | Frozen canonical byte format, hashing, randomness, fixed-point and migration decisions. Derived from the canonical pages under R001. |
| `docs/architecture/ContinuityDurabilityContractV1.md` | Frozen clocks, trusted elapsed time, reconciliation, durability, platform protection and encrypted-record decisions. Derived from the canonical pages under R002. |

Rules:

- A value that appears in neither the canonical pages nor a registry may not be
  invented by the implementer.
- Any architecture change made during implementation must be recorded as an
  explicit amendment and must reopen all affected regression gates.

## Amendments observed by this repository

| Date | Amendment | Effect here |
|---|---|---|
| 2026-08-07 | Architecture freeze and R000/R001 implementation handoff | R001 non-negotiable assertions and the eight-criterion R001 gate; the panic witness has no universal 2.0 ms requirement and must be measured |
| 2026-08-12 | Aliveness-first validation | R003 and later organism mechanisms are gated behind A001 |
| 2026-08-13 | R001 determinism target matrix amendment | Snapdragon is no longer a required R001 gate target; the required matrix is Tensor, x86 emulator and desktop JVM, with Exynos conditional |

Contracts not yet frozen, and therefore not yet derivable:

| Contract | Status | Consequence |
|---|---|---|
| `RecoveryCryptographyContractV1` | Not frozen | Recovery cryptography, the mnemonic encoding, the KDF and the identity-epoch protocol are `BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY` and may not be invented inside implementation code |
| `SpeciesBaselineV1`, `CriticalCareContractV1` | Not frozen, gated behind A001 | No physiology exists to implement |

