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
| `docs/architecture/PersistenceBackendContractV1.md` | Frozen production storage backend, layout, framing, acknowledgement, compaction and capacity semantics. Derived under D011. |
| `docs/architecture/LocalStorageCryptographyContractV1.md` | Frozen local record encryption, key wrapping, rotation, interrupted rewrap and quarantine. Derived under D011. |
| `docs/architecture/RecoveryCryptographyContractV1.md` | Frozen recovery root, encoding, KDF, cold-package format and failure semantics. Derived under D011. |
| `docs/architecture/IdentityAuthorityProtocolV1.md` | Frozen epoch authority protocol, proof, replay protection and the supported-singularity claim. Derived under D011. |
| `docs/architecture/RecoveryPackageStoreContractV1.md` | Frozen provider-neutral cold-package storage interface and conformance requirements. Derived under D011. |

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
| 2026-08-13 | R002 execution-boundary amendment | Recovery cryptography, the identity authority, the recovery provider and production backend selection are R012 gates and do not block R002 |
| 2026-08-13 | A001 Attempt-1 candidate and preregistration amendments | The three-arm human ablation family and the frozen Attempt-1 constants |
| 2026-08-14 | Parallel execution amendment while A001 is externally blocked | A bounded subset of R012 substrate work is authorized in parallel: backend selection, local storage cryptography, recovery cryptography, identity-authority protocol, recovery-package storage. Not product UX, sensors, notifications, dialogue or any R003–R009 mechanism |

Contracts not yet frozen, and therefore not yet derivable:

| Contract | Status | Consequence |
|---|---|---|
| `RecoveryCryptographyContractV1` | **Frozen under D011** | `BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY` is cleared. The recovery root, encoding, checksum, KDF, package format and identity-epoch protocol are frozen and qualified |
| `SpeciesBaselineV1`, `CriticalCareContractV1` | Not frozen, gated behind A001 | No physiology exists to implement |

## A000/A001 sources reviewed under D008

| Page | Sections |
|---|---|
| Implementation Plan E2E | Section 0.6 required per-phase contract freezes; the whole `A000/A001 — Parallel Aliveness Research Track` section: purpose, A000.1 required research modules, A000.2 abstract habitat, A000.3 candidate mechanisms, A000.4 human leave-one-out ablations, A000.4a multiplicity and rater exposure, A000.5 decision-explanation evidence and falsifiable spontaneity attribution, `A001 — Preregistered human aliveness gate`, `AlivenessProgramGateV1`, `ScriptedPetBaselineV1` and independent competence qualification, primary interaction model, primary endpoint and feasibility budget, `BlindVariancePilotV1`, `A001FeasibilityBudgetV1`, A001 governance audit, A001 pass/fail consequence |
| Digital Living Lifeform | Sections 6, 7, 8 and 9 for the organism model, hierarchical action control, affordances and foundational learning including habituation recovery, curiosity preservation and the behavioural-death tests; section 20 decision explanation contract; and the amendments of 2026-08-12 covering the parallel aliveness spike track, the research/production inheritance boundary, the falsifiable aliveness gate and strong-baseline comparator, the independently qualified scripted baseline and causal-spontaneity classification, the blind variance pilot and coalition attribution and joint curiosity balance and fixed independent-review roster, and the final A-track mathematical closure with `CoalitionValueFunctionV1`, `CuriosityEnvelopeFeasibilityV1` and independent reviewer breach authority |

The 2026-08-13 R002 phase-boundary clarification was also read, and closes the
prepared-rest, recovery-cryptography and persistence-backend questions D007 left
open.

