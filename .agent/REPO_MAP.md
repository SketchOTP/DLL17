# Repository Map

## Entry points

- `AGENTS.md` — canonical cross-agent repository policy; the precedence, classification, validation and reporting rules all other adapters defer to.
- `CLAUDE.md` — Claude Code adapter; imports AGENTS.md and adds no duplicate policy.
- `GEMINI.md` — Gemini adapter; imports AGENTS.md and adds no duplicate policy.
- `COMMANDMENTS_OF_THE_CODE.md` — condensed coding principles referenced by the policy layer.
- `.agent/PROJECT_GOAL.md` — adopted Digital Living Lifeform charter; the local statement of goal, success measures, scope, non-goals and constraints.
- `.agent/CURRENT.md` — mutable snapshot of the active directive, phase, gate state and last validation.

## Core modules

- `core-math/` — pure Kotlin JVM module for fixed-point types, saturating arithmetic, bounded interpolation and lookup tables; an R000 marker only, no canonical logic.
- `core-crypto/` — pure Kotlin JVM module for canonical hashing, byte codecs and deterministic random-domain derivation; an R000 marker only, no canonical logic.
- `core-state/` — pure Kotlin JVM module for canonical state, the reducer, normalized events and migrations; an R000 marker only, no canonical logic.
- `desktop-runner/` — pure Kotlin JVM headless runner; reports the module inventory and R000 readiness and exits.
- `android-host/` — the only module that links the Android framework; a launchable Compose shell that displays build and phase identity.

## Interfaces and contracts

- `.agent/DIRECTIVES.md` — append-only ledger of directives issued by the user, with the field schema the validator enforces.
- `.agent/OUTCOMES.md` — append-only closure ledger; every outcome references one directive identifier.
- `.agent/RECORD.md` — append-only record of durable decisions and milestones.
- `.agent/LEARNINGS.md` — append-only durable, evidence-backed project knowledge.
- `.agent/PROJECT_PROFILE.md` — verified identity, stack, tooling and command facts for this repository.
- `docs/architecture/ProjectIdentityBuildContractV1.md` — frozen project identity, toolchain and dependency policy.
- `docs/architecture/registries/` — the seven mandatory R000 registries; scaffolds with no invented organism entries.
- `docs/architecture/CANONICAL_SOURCES.md` — pointers to the authoritative external specifications.
- `docs/invariants/INVARIANT_REGISTRY.md` — invariants and where each is enforced today.
- `docs/decisions/DECISION_LOG.md` — implementation decisions that are not architect directives.
- `governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md` — proof that the repository is greenfield.
- `governance/release-gates/R000_EXIT_GATE.md` — honest per-criterion status of the R000 exit gate.

## Tests and validation

- `scripts/validate_governance.py` — the only executable validation in the repository; checks required files, Cursor rules, policy content and the adopted or unadopted state of the governance files.
- `scripts/test_validate_governance.py` — validator self-test; exercises both validator modes with positive and rejection cases built from an isolated fixture.
- `scripts/fixtures/governance_template/.agent/` — pristine unadopted governance files used as the fixture base for the self-test; never the live project state.
- `tools/verify_project_identity.py` — checks the build files against the frozen project identity contract.
- `.github/workflows/ci.yml` — GitHub Actions workflow running governance validation, the governance self-test, the identity check, module tests, the full build and the Android debug assembly.
- `qualification/` — evidence directories for fixtures, replay, fault injection, longitudinal runs, the device matrix and red-team findings; empty in R000.

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
