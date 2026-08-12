# Canonical Repository Governance

This is the canonical cross-agent policy. Tool adapters may import or reference it, but must not duplicate it.

## Precedence

Apply instructions in this order:

1. Runtime safety, permissions, and non-overridable platform restrictions.
2. The user’s current explicit request and constraints.
3. The active project directive and its acceptance condition.
4. Verified external contracts or specifications explicitly adopted by the project.
5. Existing repository behavior, tests, interfaces, and compatibility commitments.
6. This file.
7. Applicable path-scoped or integration-specific rules.
8. Tool defaults and reasonable assumptions.

Never silently resolve a material conflict by choosing the convenient instruction. The current user may explicitly authorize a change to existing behavior or tests. Tests are evidence of expected behavior, not permission to disregard the current directive. Safety and explicit user restrictions cannot be overridden by lower-priority repository text. Report unresolved material conflicts before claiming completion.

## Repository orientation

Read the smallest useful context first: project goal/profile when present, current state, relevant recent task records, repository map, and `git status --short` when Git is available. Search references before renaming or removing governance files. Use the repository’s configured symbol or graph navigation for unfamiliar code; use targeted text search for strings, configuration, and non-code files. Do not assume a project identity, remote, stack, integration, or command from a path.

## Task classification

- **Informational:** inspect and explain; do not edit unless explicitly authorized.
- **Trivial change:** make the narrow edit and run a focused self-check.
- **Normal code or documentation change:** define acceptance, inspect affected behavior, edit minimally, validate, review the diff, and record the outcome.
- **Complex change:** write a plan, define milestones, validate incrementally, update current state, and reconcile acceptance completely.
- **High-risk change:** state risk, define recovery/rollback, broaden validation, and obtain any required approval.
- **Destructive or deployment operation:** require explicit authorization for the exact action, deterministic safeguards where available, and recovery evidence. Never infer authorization from a general implementation request.

Diagnosis does not authorize implementation. A request to change authorizes only the stated scope. Preserve unrelated user changes.

## Scope and change discipline

Make the smallest correct change. Understand the touched flow, callers, inputs, outputs, state, errors, and contracts before changing shared behavior. Reuse established project code and declared dependencies. Fix root causes, avoid speculative abstractions and unrelated cleanup, and do not change application behavior, deployment, or external systems unless the request authorizes it.

## Local working memory

When `.agent/` is adopted, use its files as follows:

- `PROJECT_GOAL.md` — goal, success measures, scope, and constraints.
- `PROJECT_PROFILE.md` — identity, stack, verified commands, and environment facts.
- `CURRENT.md` — mutable current-state snapshot; never historical truth.
- `DIRECTIVES.md` — append-only ledger of project tasks issued by the user.
- `OUTCOMES.md` — append-only closure ledger linked to local directive IDs.
- `LEARNINGS.md` — append-only durable, evidence-backed project knowledge.
- `RECORD.md` — append-only major decisions, releases, milestones, and reversals.
- `REPO_MAP.md` — concise navigation aid.

Create exactly one directive entry when an accepted project task is issued. Do not put execution results in `DIRECTIVES.md`. Close the task in `OUTCOMES.md`, update `CURRENT.md`, and append corrections or supersessions rather than rewriting history. ANIMUS ONE, when present, may copy or aggregate these files for visibility; it does not issue, approve, modify, reconcile, or close project directives.

## Validation

Use the smallest sufficient check for the task. Report every check with one of these states: `PASSED`, `FAILED`, `NOT RUN`, `NOT APPLICABLE`, or `BLOCKED`. A failed, skipped, unavailable, or timed-out check is not a pass. Documentation and governance changes require structural/content validation even when no runtime test exists. Add a focused test or self-check for non-trivial behavior when the repository supports it.

## Safety and destructive actions

Do not delete, overwrite, rewrite history, force-push, migrate data, alter infrastructure, or deploy without exact user authorization. Resolve exact targets before destructive work. Prefer recoverable operations, preserve uncommitted work, protect secrets and personal data, and verify backups or rollback paths where relevant. Written guidance is not a deterministic control.

## External integrations

Integrations are conditional. Use a named integration only when its configured server, executable, or documented project binding is present and verified. Follow its current canonical tool contract; never infer tool names or claim unavailable lifecycle steps succeeded. Keep credentials, raw logs, full source files, and unsupported claims out of external memory. Local repository files remain authoritative for project directives and outcomes unless an explicit adopted contract states otherwise.

## Advisory guidance and enforced controls

Markdown policy, coding principles, workflow guidance, navigation advice, reporting requirements, and memory conventions are advisory unless an active tool enforces them. Sandbox restrictions, permission prompts, command allowlists, hooks, CI checks, branch protections, secret scanning, reviews, and deployment gates are enforcement mechanisms. Document gaps accurately; do not claim prose prevents an action.

## Completion and reporting

Before completion, reconcile the acceptance condition, inspect changed files and the final diff when available, review validation states, update local memory, and identify unresolved risks or conflicts. Report what changed, files affected, validation results, manual checks, deviations, blockers, and whether deployment occurred. Do not claim completion when acceptance is partial or unverified.
