# Project Profile

## Lifecycle

- Status: `ADOPTED`
- Last verified: `2026-08-12T19:55:43-04:00`

## Identity

- Project name or identifier: Digital Living Lifeform (repository directory `DLL17`)
- Purpose: Governance and specification baseline for the Digital Living Lifeform program. No organism implementation code exists in this repository yet.
- Repository root: `/home/sketch/Projects/DLL17`
- Verified remote: none. The repository is a local Git repository on branch `main` with provenance beginning at baseline commit `f82e1b2f7c138a7c4238f109b45a6562b8b18a21`, created under D-003. No remote is configured, and remote hosting is left unresolved for a later directive.
- Maturity or current phase: R000 greenfield project initialization. Governance baseline only.

## Languages and runtimes

- Python 3.14.4, verified with `python3 --version`. Used only by the governance validator and its test script.
- Markdown, used for all governance, policy and ledger files.
- JSON, used for `.cursor/mcp.json`.
- Cursor rule files (`.mdc`) under `.cursor/rules/`.
- No Android, Kotlin, Java, Gradle or JVM toolchain is present in this repository at the last verified date.

## Tools

- Build: none present. No build system, package manifest or dependency file exists in this repository at the last verified date.
- Test: `python3 scripts/test_validate_governance.py`, present and passing as of the last verified date. It validates the governance validator against an isolated pristine fixture under `scripts/fixtures/governance_template/`, plus positive and rejection cases for both validator modes.
- Lint: none present. No linter is configured in this repository at the last verified date.
- Type-check: none present. No type checker is configured in this repository at the last verified date.
- Packaging: none present. No packaging configuration exists in this repository at the last verified date.
- Source control is local Git on branch `main`, established under D-003. Excluded from tracking are Python bytecode caches, local virtual environments, operating-system and editor scratch files, and the development-only migration report that the validator forbids in the tracked tree. Serena local overrides remain excluded by the pre-existing `.serena/.gitignore`.
- Preferred navigation/indexing: direct file reading and text search. `.serena/project.yml` and `.cursor/mcp.json` declare optional external navigation servers whose availability is not verified from within this repository.

## Verified commands

- `python3 scripts/validate_governance.py --mode ADOPTED` — repository-wide governance validation in adopted mode. Executed on 2026-08-12.
- The validator also accepts an unadopted mode for the clean distributable fixture state. That mode no longer applies to this repository because the repository is now adopted, so its exact flag is deliberately not recorded here as a runnable project command.
- `python3 scripts/test_validate_governance.py` — validator self-test script. Executed on 2026-08-12.
- Project-specific commands: none beyond the governance commands listed above. No organism build, test or run command exists in this repository at the last verified date.

## Constraints

- Platform/compatibility: the program targets Android as host, sensor and evidence source, persistence environment and presentation surface, with a deterministic core required to reproduce byte-identical canonical results across the qualified JVM and Android hardware matrix. None of that target toolchain is present in this repository yet.
- Security: no credentials, secrets or personal data are stored in this repository. Raw logs, full source files, secrets and unsupported claims must be kept out of external memory systems.
- Data handling: governance ledgers in `.agent/` are append-only for history and must not be rewritten. Local repository files remain authoritative for project directives and outcomes.
- Deployment: no deployment, release or distribution mechanism exists in this repository. No deployment has occurred.
