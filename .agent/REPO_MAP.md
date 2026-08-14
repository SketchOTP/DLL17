# Repository Map

## Entry points

- `AGENTS.md` — canonical cross-agent repository policy; the precedence, classification, validation and reporting rules all other adapters defer to.
- `CLAUDE.md` — Claude Code adapter; imports AGENTS.md and adds no duplicate policy.
- `GEMINI.md` — Gemini adapter; imports AGENTS.md and adds no duplicate policy.
- `COMMANDMENTS_OF_THE_CODE.md` — condensed coding principles referenced by the policy layer.
- `.agent/PROJECT_GOAL.md` — adopted Digital Living Lifeform charter; the local statement of goal, success measures, scope, non-goals and constraints.
- `.agent/CURRENT.md` — mutable snapshot of the active directive, phase, gate state and last validation.

## Core modules

- `core-math/` — pure Kotlin JVM module holding `Fixed64` semantics, saturating arithmetic, bounded interpolation and decay, saturation diagnostics, and digest-verified lookup tables. Canonical logic since R001.
- `core-crypto/` — pure Kotlin JVM module holding the canonical byte codec and envelope, the project's own SHA-256, the canonical state hash, and counter-based PRNG substreams with domain-separated derivation. Canonical logic since R001.
- `core-state/` — pure Kotlin JVM module holding the canonical snapshot, the pure single-threaded reducer, normalized events, durability classes, the durable journal and replay kernel, the Class W staged protocol, the panic witness, the assisted-payload interface, schema migration, and the R001 qualification kernel. Canonical logic since R001; still no organism behaviour.
- `core-continuity/` — pure Kotlin JVM module holding the four clocks, durable time anchors, time-confidence classification, blind-decay credit, the unresolved-time debt ledger, offline reconciliation, generation-partitioned journalling and compaction, durability admission and safe hold, platform protection, the encrypted-record boundary, identity binding, version boundaries, and the R002 qualification kernel. Canonical logic since R002; still no organism behaviour.
- `desktop-runner/` — pure Kotlin JVM headless runner and the desktop determinism matrix target; runs the R001 qualification kernel and emits `R001_EVIDENCE_DIGEST`.
- `android-host/` — the only module that links the Android framework; the unchanged R000 Compose shell, the R012 Android adapter (`persistence/`: the Keystore key container, the app-private storage layout, the startup key-resolution decision table), a debug-only crash process, and the instrumented suites that qualify determinism, continuity and the R012 substrate on real ART.

## Interfaces and contracts

- `.agent/DIRECTIVES.md` — append-only ledger of directives issued by the user, with the field schema the validator enforces.
- `.agent/OUTCOMES.md` — append-only closure ledger; every outcome references one directive identifier.
- `.agent/RECORD.md` — append-only record of durable decisions and milestones.
- `.agent/LEARNINGS.md` — append-only durable, evidence-backed project knowledge.
- `.agent/PROJECT_PROFILE.md` — verified identity, stack, tooling and command facts for this repository.
- `docs/architecture/ProjectIdentityBuildContractV1.md` — frozen project identity, toolchain and dependency policy.
- `docs/architecture/DeterminismContractV1.md` — frozen canonical byte format, hashing, randomness, fixed-point and migration decisions, with the candidate decision record.
- `docs/architecture/registries/` — the seven mandatory registries; populated with R001 determinism facts and no invented organism entries.
- `docs/architecture/CANONICAL_SOURCES.md` — pointers to the authoritative external specifications.
- `docs/invariants/INVARIANT_REGISTRY.md` — invariants and where each is enforced today.
- `docs/decisions/DECISION_LOG.md` — implementation decisions that are not architect directives.
- `governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md` — proof that the repository is greenfield.
- `governance/release-gates/R000_EXIT_GATE.md` — honest per-criterion status of the R000 exit gate.
- `governance/release-gates/R001_EXIT_GATE.md` — per-criterion status of all three canonical R001 exit gates, including the one criterion resting on an architect waiver.

## Tests and validation

- `scripts/validate_governance.py` — the only executable validation in the repository; checks required files, Cursor rules, policy content and the adopted or unadopted state of the governance files.
- `scripts/test_validate_governance.py` — validator self-test; exercises both validator modes with positive and rejection cases built from an isolated fixture.
- `scripts/fixtures/governance_template/.agent/` — pristine unadopted governance files used as the fixture base for the self-test; never the live project state.
- `tools/verify_project_identity.py` — checks the build files against the frozen project identity contract.
- `tools/qualify_r000_android.sh` — installs, launches, verifies, terminates and relaunches the shell on a connected Android target and records the evidence.
- `tools/verify_backup_exclusion.py` — reads the built debug and release packages with `aapt2`, follows the merged manifest's own resource references, and asserts that canonical state is excluded from cloud backup and device transfer. Exits 2 rather than 0 when the tooling or the package is absent.
- `tools/build_qualification_bundle.py` — builds and verifies the hashed per-phase qualification bundles. Closed phases are pinned to their qualified commit and read through git, so a later phase cannot break a gate that already closed.
- `tools/generate_lookup_tables.py` — generates the canonical lookup tables and, as an independent Python implementation of the codec and digest, cross-checks the Kotlin canonical encoder and SHA-256.
- `tools/qualify_r001_determinism.sh` — runs the determinism matrix against a connected Android target and records what that target computed.
- `.github/workflows/ci.yml` — GitHub Actions workflow running governance validation, the governance self-test, the identity check, module tests, the full build and the Android debug assembly.
- `qualification/` — evidence directories for fixtures, replay, fault injection, longitudinal runs, the device matrix and red-team findings.
- `qualification/evidence/R000/` — recorded governance, identity, build, runner, Android assembly and toolchain evidence for the R000 gate.
- `qualification/evidence/R001/` — recorded governance, identity, build, runner, lookup-table and toolchain evidence for the R001 gate.
- `qualification/fixtures/R001/` — frozen golden vectors and the desktop reference report for the R001 fixture set.
- `qualification/replay/R001/` — replay equivalence and the eight-boundary crash recovery sweep.
- `qualification/device-matrix/R000/` — device matrix and raw Android install, launch, visible-state, terminate and relaunch evidence including screenshots and logcat.
- `qualification/device-matrix/R012/` — the Android device matrix for the R012 substrate, the emulator's complete 45-fixture report and its measurements. The physical-device row is `BLOCKED_DEVICE_UNAVAILABLE`.
- `qualification/device-matrix/R001/` — the cross-target determinism matrix and per-target records for the desktop JVM, the x86_64 emulator and Tensor hardware.
- `governance/qualification/R000_QUALIFICATION_BUNDLE.md` — hashed manifest binding the R000 qualification claim; verified in CI against its pinned commit.
- `governance/qualification/R001_QUALIFICATION_BUNDLE.md` — hashed manifest binding the R001 qualification claim; verified in CI.

## Configuration

- `.gitignore` — source-control exclusions for bytecode caches, local environments, scratch files, the forbidden migration report, build outputs, IDE state and Android artifacts.
- `settings.gradle.kts` — root project name, repository policy and the five included modules.
- `build.gradle.kts` — root build script declaring the pinned plugins without applying them.
- `gradle/libs.versions.toml` — the version catalog; every dependency version is an exact pin.
- `gradle/wrapper/gradle-wrapper.properties` — pins the Gradle distribution to the contracted version.
- `gradle.properties` — explicit JVM, parallelism, caching and AndroidX build settings.
- `.serena/.gitignore` — pre-existing exclusion of Serena local overrides and cache.
- `.cursor/mcp.json` — external navigation and memory server declarations for Cursor.
- `.cursor/MCP.md` — documentation of those external integrations.
- `.cursor/rules/00-core-governance.mdc` — the single always-applied Cursor policy adapter.
- `.cursor/rules/02-mimir.mdc` — conditional rule for the Mimir memory integration.
- `.cursor/rules/03-serena.mdc` — conditional rule for the Serena navigation integration.
- `.cursor/rules/04-cocoindex-code.mdc` — conditional rule for the code-index integration.
- `.cursor/rules/05-animus-project-directives.mdc` — conditional rule covering directive visibility aggregation.
- `.cursor/rules/06-storage-archive.mdc` — conditional rule for storage and archive handling.
- `.cursor/skills/mimir/SKILL.md` — the Mimir workflow definition the validator requires.
- `.serena/project.yml` — Serena project configuration.

## Generated areas

- `.serena/project.local.yml` — tool-written local state; not hand-authored project source.
- `build/` — Gradle build outputs in every module; untracked.
- `.gradle/` — Gradle project cache; untracked.

## External integration points

- `.cursor/mcp.json` — declares the external servers; a named integration may be used only when its configured server is present and verified.
- `.cursor/skills/mimir/SKILL.md` — the vault memory workflow used for cross-session knowledge outside this repository.

## Areas that must not be edited manually

- `.agent/DIRECTIVES.md` — historical entries are append-only; corrections are appended, never rewritten.
- `.agent/OUTCOMES.md` — historical entries are append-only; corrections are appended, never rewritten.
- `.agent/RECORD.md` — historical entries are append-only; supersession is recorded by a new entry.
- `.agent/LEARNINGS.md` — historical entries are append-only; supersession is recorded by a new entry.
