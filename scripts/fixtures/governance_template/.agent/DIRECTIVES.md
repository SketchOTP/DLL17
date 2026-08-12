# Project Directive Ledger Template

After adoption, this append-only ledger records project directives issued by the user to the AI coder. The adopted project records those directives locally.

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
