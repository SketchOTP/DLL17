# Project Learnings

Use this append-only file for durable, verified project knowledge only.

## Entry guidance after adoption

Each live entry should include:

- Learning ID.
- Date.
- Fact or lesson.
- Evidence location.
- Confidence: `VERIFIED` or `PROVISIONAL`.
- Scope.
- Supersedes or superseded-by reference when applicable.

Do not add live entries to this template. Exclude temporary narration, raw logs, full source files, secrets, unsupported guesses, and facts already obvious from stable project documentation.

## L-0001

- Learning ID: L-0001
- Date: 2026-08-12
- Fact or lesson: The repository validator constrains governance vocabulary. Directive headings must match the identifier form beginning with the letter D and a hyphen, and outcome states are restricted to COMPLETE, PARTIAL, BLOCKED, FAILED, CANCELLED and SUPERSEDED, so an externally requested state such as NONCONFORMING must be expressed as the nearest allowed state plus an explicit narrative in the outcome summary.
- Evidence location: scripts/validate_governance.py, DIRECTIVE_HEADING and OUTCOME_STATUSES definitions near the top of the file.
- Confidence: VERIFIED
- Scope: All future governance ledger entries in this repository.
- Supersedes learning: none

## L-0002

- Learning ID: L-0002
- Date: 2026-08-12
- Fact or lesson: The shipped validator self-test copies this repository and asserts that the copy satisfies the clean unadopted fixture state, so it raises an assertion error permanently once the repository is adopted. Adoption and that self-test cannot both succeed without changing the script, which is outside governance-baseline scope.
- Evidence location: scripts/test_validate_governance.py, the clean fixture assertion in main; observed by executing the script on 2026-08-12.
- Confidence: VERIFIED
- Scope: Repository validation strategy and any future directive that touches scripts/.
- Supersedes learning: none

## L-0003

- Learning ID: L-0003
- Date: 2026-08-12
- Fact or lesson: This repository is not under version control. No Git repository, remote, branch or commit identifier exists at the repository root, so change history for governance work is available only through the local append-only ledgers.
- Evidence location: git status --short and git rev-parse HEAD executed at the repository root on 2026-08-12, both reporting that this is not a Git repository.
- Confidence: VERIFIED
- Scope: All change-evidence reporting for this repository until version control is introduced.
- Supersedes learning: none
