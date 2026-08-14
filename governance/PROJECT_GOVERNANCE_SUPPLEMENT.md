# Project Governance Supplement

Repository-specific governance that the canonical Authority package does not
carry. `AGENTS.md` is the Codex-first router and
`.agents/skills/authority-governance/SKILL.md` is the canonical governance
workflow; both take precedence over this file. Nothing here weakens, replaces,
or competes with them.

## Why this file exists

The Authority governance package was reorganized into a short always-on
`AGENTS.md` router plus canonical Codex skills. Three sections that this
repository had operated under since adoption are not carried by either the new
router or the canonical skill: repository orientation, task classification, and
the advisory-versus-enforced distinction. They do not conflict with Authority,
they have been load-bearing across this project's directive history, and they
are preserved here verbatim rather than deleted. Recorded as DEC-0038.

## Repository orientation

Read the smallest useful context first: project goal/profile when present, current state, relevant recent task records, repository map, and `git status --short` when Git is available. Search references before renaming or removing governance files. Use the repository's configured symbol or graph navigation for unfamiliar code; use targeted text search for strings, configuration, and non-code files. Do not assume a project identity, remote, stack, integration, or command from a path.

## Task classification

- **Informational:** inspect and explain; do not edit unless explicitly authorized.
- **Trivial change:** make the narrow edit and run a focused self-check.
- **Normal code or documentation change:** define acceptance, inspect affected behavior, edit minimally, validate, review the diff, and record the outcome.
- **Complex change:** write a plan, define milestones, validate incrementally, update current state, and reconcile acceptance completely.
- **High-risk change:** state risk, define recovery/rollback, broaden validation, and obtain any required approval.
- **Destructive or deployment operation:** require explicit authorization for the exact action, deterministic safeguards where available, and recovery evidence. Never infer authorization from a general implementation request.

Diagnosis does not authorize implementation. A request to change authorizes only the stated scope. Preserve unrelated user changes.

## Advisory guidance and enforced controls

Markdown policy, coding principles, workflow guidance, navigation advice, reporting requirements, and memory conventions are advisory unless an active tool enforces them. Sandbox restrictions, permission prompts, command allowlists, hooks, CI checks, branch protections, secret scanning, reviews, and deployment gates are enforcement mechanisms. Document gaps accurately; do not claim prose prevents an action.

## Directive and outcome ledger discipline

Create exactly one `.agent/DIRECTIVES.md` entry when an accepted project task is issued, and do not put execution results in it. Close the task in `.agent/OUTCOMES.md`, update `.agent/CURRENT.md`, and append corrections or supersessions rather than rewriting history. ANIMUS ONE, when present, may copy or aggregate these files for visibility; it does not issue, approve, modify, reconcile, or close project directives.
