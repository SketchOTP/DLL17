# Repository Map

## Entry points

- `AGENTS.md` — canonical cross-agent repository policy; the precedence, classification, validation and reporting rules all other adapters defer to.
- `CLAUDE.md` — Claude Code adapter; imports AGENTS.md and adds no duplicate policy.
- `GEMINI.md` — Gemini adapter; imports AGENTS.md and adds no duplicate policy.
- `COMMANDMENTS_OF_THE_CODE.md` — condensed coding principles referenced by the policy layer.
- `.agent/PROJECT_GOAL.md` — adopted Digital Living Lifeform charter; the local statement of goal, success measures, scope, non-goals and constraints.
- `.agent/CURRENT.md` — mutable snapshot of the active directive, phase, gate state and last validation.

## Core modules

- `.` — no organism implementation modules exist yet; at the R000 baseline this repository contains governance, policy, tooling and rule files only.

## Interfaces and contracts

- `.agent/DIRECTIVES.md` — append-only ledger of directives issued by the user, with the field schema the validator enforces.
- `.agent/OUTCOMES.md` — append-only closure ledger; every outcome references one directive identifier.
- `.agent/RECORD.md` — append-only record of durable decisions and milestones.
- `.agent/LEARNINGS.md` — append-only durable, evidence-backed project knowledge.
- `.agent/PROJECT_PROFILE.md` — verified identity, stack, tooling and command facts for this repository.

## Tests and validation

- `scripts/validate_governance.py` — the only executable validation in the repository; checks required files, Cursor rules, policy content and the adopted or unadopted state of the governance files.
- `scripts/test_validate_governance.py` — validator self-test; exercises both validator modes with positive and rejection cases built from an isolated fixture.
- `scripts/fixtures/governance_template/.agent/` — pristine unadopted governance files used as the fixture base for the self-test; never the live project state.

## Configuration

- `.gitignore` — source-control exclusions for bytecode caches, local environments, scratch files and the forbidden migration report.
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

## External integration points

- `.cursor/mcp.json` — declares the external servers; a named integration may be used only when its configured server is present and verified.
- `.cursor/skills/mimir/SKILL.md` — the vault memory workflow used for cross-session knowledge outside this repository.

## Areas that must not be edited manually

- `.agent/DIRECTIVES.md` — historical entries are append-only; corrections are appended, never rewritten.
- `.agent/OUTCOMES.md` — historical entries are append-only; corrections are appended, never rewritten.
- `.agent/RECORD.md` — historical entries are append-only; supersession is recorded by a new entry.
- `.agent/LEARNINGS.md` — historical entries are append-only; supersession is recorded by a new entry.
