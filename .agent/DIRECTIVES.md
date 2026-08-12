# Project Directive Ledger

This append-only ledger records project directives issued by the user to the AI coder. The adopted project records those directives locally.

ANIMUS ONE may copy or aggregate the resulting governance files for centralized visibility. ANIMUS ONE does not issue, approve, modify, execute, reconcile, or close directives.

## Entry schema after adoption

Use one live entry for each accepted project task. Keep examples outside this file; do not add a heading beginning with a live directive ID until adoption.

```markdown
## <local-directive-id>

- Issued: <ISO-8601 timestamp with timezone>
- Issuer: User
- External directive: <ID or none>
- Objective: <requested observable result>
- Scope: <authorized areas>
- Exclusions: <prohibited or out-of-scope work>
- Acceptance: <observable completion condition>
- Risk class: <LOW | NORMAL | HIGH | DESTRUCTIVE>
- Relationship: <new | resumes | amends | supersedes>
- Related directive: <local directive ID or none>
- Status at issuance: ISSUED
```

Do not record execution results here. Do not rewrite historical entries after adoption. Append corrections, amendments, and supersessions referencing the original entry.

## D-001

- Issued: 2026-08-12T19:37:10-04:00
- Issuer: User
- External directive: D001
- Objective: Acknowledge the Digital Living Lifeform project charter, restate the project objective, confirm canonical authority, confirm the current execution gate state and the non-negotiable boundaries, and return a completion report establishing the R000 execution baseline.
- Scope: Reading the canonical Digital Living Lifeform Notion page and the Implementation Plan E2E page, and returning one Markdown completion report.
- Exclusions: Any repository, source-code, build, configuration or infrastructure change; implementation proposals; architecture modifications; plans for future phases.
- Acceptance: A completion report containing the required sections, with an explicit confirmation that no repository change was made.
- Risk class: LOW
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED

## D-002

- Issued: 2026-08-12T19:41:43-04:00
- Issuer: User
- External directive: D002
- Objective: Establish the repository governance layer as a truthful, fully adopted R000 execution baseline in which every governance file required by the existing validator is adopted, the validator passes repository-wide in ADOPTED mode, and the recorded state matches the canonical gate state without invention.
- Scope: The governance files under .agent/ only, retaining the adopted .agent/PROJECT_GOAL.md from the D-001 event unless factual correction is required.
- Exclusions: R001 implementation; DeterminismContractV1; organism physiology; behavior or action-controller implementation; learning; memory; relationships; development; AR or spatial implementation; production persistence implementation; A000 behavioral experiments; any non-governance source, build or infrastructure change; erasing or rewriting the D-001 noncompliance; proposing or beginning D003.
- Acceptance: python3 scripts/validate_governance.py --mode ADOPTED returns zero validation errors, every required governance file is in a truthful adopted state, the D-001 nonconformance remains recorded, and no non-governance change was introduced.
- Risk class: NORMAL
- Relationship: new
- Related directive: none
- Status at issuance: ISSUED
