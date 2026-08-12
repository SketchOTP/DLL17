# Project Outcome Ledger Template

After adoption, this append-only ledger records results for project directives. Every live outcome must reference one local directive ID.

## Entry schema after adoption

Use live outcome headings only after adoption. The following schema is instructional and is not a live entry:

```markdown
## <local-directive-id> - <outcome-state>

- Outcome ID: <unique outcome record ID>
- Supersedes outcome: <outcome ID or none>
- Closed: <ISO-8601 timestamp with timezone>
- Acceptance: <MET | PARTIAL | NOT MET>
- Summary: <concise result>
- Changed areas: <paths or none>
- Validation:
  - <command or check> - <PASSED | FAILED | NOT RUN | NOT APPLICABLE | BLOCKED>
- Remaining risks: <risks or none>
- Blockers: <blockers or none>
- Follow-up directive: <ID or none>
```

Allowed adopted-project outcome states: `COMPLETE`, `PARTIAL`, `BLOCKED`, `FAILED`, `CANCELLED`, `SUPERSEDED`. Do not rewrite earlier entries; append corrections referencing the original.
