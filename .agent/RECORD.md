# Project Decision and Milestone Record

Use this append-only record for major durable project events and decisions, not routine task outcomes.

## Entry guidance after adoption

Use it for architectural decisions, governance changes, releases, qualification or certification events, major reversals, important milestones, and decision supersessions.

Each live entry should include:

- Date.
- Record or decision ID.
- Status.
- Decision or event.
- Rationale.
- Affected areas.
- Supersession relationship when applicable.

Allowed status values are `PROPOSED`, `ACTIVE`, `SUPERSEDED`, `REVERSED`, and `CLOSED`.

Do not add live decisions or milestones to this template. Examples must remain outside the shipped template state.

## DEC-0001

- Date: 2026-08-12
- Record or decision ID: DEC-0001
- Status: ACTIVE
- Decision or event: The Digital Living Lifeform project charter was adopted into .agent/PROJECT_GOAL.md as the local statement of goal, success measures, scope, non-goals, constraints, governing specifications and approval authority.
- Rationale: The repository needed a local, machine-checkable statement of the charter so that later phases can be reconciled against it without depending on external pages being reachable.
- Affected areas: .agent/PROJECT_GOAL.md
- Supersedes record: none

## DEC-0002

- Date: 2026-08-12
- Record or decision ID: DEC-0002
- Status: ACTIVE
- Decision or event: D-001 is permanently recorded as nonconforming because a repository change was made under a directive that prohibited repository changes, while the resulting adopted goal file is retained deliberately rather than reverted.
- Rationale: Governance history must stay truthful. Reverting the file would destroy a correct artifact, and rewriting the event as a clean execution would falsify the ledger, so the deviation is disclosed and preserved instead.
- Affected areas: .agent/DIRECTIVES.md, .agent/OUTCOMES.md, .agent/PROJECT_GOAL.md
- Supersedes record: none

## DEC-0003

- Date: 2026-08-12
- Record or decision ID: DEC-0003
- Status: ACTIVE
- Decision or event: The R000 governance baseline was adopted repository-wide, moving every governance file required by the repository validator out of its shipped unadopted state into a truthful adopted state.
- Rationale: D-002 requires the repository to represent the real project state and to satisfy the existing validator in adopted mode before any R000 implementation work is authorized.
- Affected areas: .agent/PROJECT_PROFILE.md, .agent/CURRENT.md, .agent/DIRECTIVES.md, .agent/OUTCOMES.md, .agent/LEARNINGS.md, .agent/RECORD.md, .agent/REPO_MAP.md
- Supersedes record: none

## DEC-0004

- Date: 2026-08-12
- Record or decision ID: DEC-0004
- Status: ACTIVE
- Decision or event: Git provenance for this repository begins at D-003. The repository was initialized locally on branch main and the baseline commit f82e1b2f7c138a7c4238f109b45a6562b8b18a21 records the accepted post-D002 governance state plus the D-003 governance-tooling correction. No remote is configured, and no earlier version-control history exists or is implied.
- Rationale: Directive evidence requires durable commit identifiers, which were unavailable while the repository was untracked. Initializing at D-003 keeps the provenance claim truthful rather than backdating history that never existed.
- Affected areas: repository root, .gitignore
- Supersedes record: none

## DEC-0005

- Date: 2026-08-12
- Record or decision ID: DEC-0005
- Status: ACTIVE
- Decision or event: The governance validator self-test now builds every mutable fixture state from the pristine copy under scripts/fixtures/governance_template/ rather than assuming the live governance directory is unadopted. The live directory is exercised only as an adopted-mode positive case and as a template-mode rejection case.
- Rationale: Adoption of the live governance files is a legitimate permanent condition, so the self-test had to stop treating live state as a pristine fixture. Isolating fixtures fixes the conflict without weakening the production validator.
- Affected areas: scripts/test_validate_governance.py, scripts/fixtures/governance_template/
- Supersedes record: none

## DEC-0006

- Date: 2026-08-12
- Record or decision ID: DEC-0006
- Status: ACTIVE
- Decision or event: ProjectIdentityBuildContractV1 was frozen from the architect-supplied values in D004 and recorded at docs/architecture/ProjectIdentityBuildContractV1.md. The contract fixes the project identifier DLL17, the product display name Digital Living Lifeform, the Android application identifier and namespace com.animusmachinae.dll17, Kotlin 2.4.10, JDK 17, Android Gradle Plugin 9.3.1, Gradle 9.5.0, compileSdk and targetSdk 37, minSdk 29, Compose BOM 2026.06.00, versionCode 1, versionName 0.1.0-dev, the debug and release variants, the absence of product flavors, deferred production signing, prohibited dynamic dependency versions, and a proprietary source licence.
- Rationale: Every value came from the architect. Freezing them in one reviewed document and enforcing them with tools/verify_project_identity.py prevents silent build drift and keeps the implementer from selecting any number.
- Affected areas: docs/architecture/ProjectIdentityBuildContractV1.md, gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties, settings.gradle.kts, android-host/build.gradle.kts, tools/verify_project_identity.py
- Supersedes record: none

## DEC-0007

- Date: 2026-08-12
- Record or decision ID: DEC-0007
- Status: ACTIVE
- Decision or event: The R000 module topology from Implementation Plan E2E was created as a Gradle multi-module project. core-math, core-crypto and core-state are pure Kotlin JVM modules, desktop-runner is a pure Kotlin JVM headless runner, and android-host is the only module that links the Android framework. The qualification, docs, governance and tools directory trees were created alongside them.
- Rationale: The runtime boundary between canonical organism code and the Android host is an architectural invariant, so it was made a build fact and a test assertion in R000 rather than a convention that later phases could erode.
- Affected areas: settings.gradle.kts, core-math, core-crypto, core-state, desktop-runner, android-host, qualification, docs, governance, tools
- Supersedes record: none

## DEC-0008

- Date: 2026-08-12
- Record or decision ID: DEC-0008
- Status: ACTIVE
- Decision or event: The authoritative remote for this repository is git@github.com:SketchOTP/DLL17.git, set as origin under D-004, with local branch main tracking origin/main. The remote hosting question that D-003 left unresolved is therefore closed.
- Rationale: The architect named the authoritative repository in D004, which supplied the proof of ownership that D-003 required before a remote could be established.
- Affected areas: repository remote configuration, .agent/PROJECT_PROFILE.md, .agent/CURRENT.md
- Supersedes record: none

## DEC-0009

- Date: 2026-08-12
- Record or decision ID: DEC-0009
- Status: ACTIVE
- Decision or event: The repository source licence is proprietary with all rights reserved. The inherited MIT LICENSE was removed and replaced under architect directive D005. Repository visibility was not changed, because D005 explicitly excluded that.
- Rationale: ProjectIdentityBuildContractV1 froze a proprietary no-redistribution policy, and D005 resolved the recorded conflict in favour of the frozen contract rather than amending it.
- Affected areas: LICENSE, docs/architecture/ProjectIdentityBuildContractV1.md, governance/source-provenance/DEPENDENCY_LICENSE_INVENTORY.md, docs/decisions/DECISION_LOG.md
- Supersedes record: none

## DEC-0010

- Date: 2026-08-12
- Record or decision ID: DEC-0010
- Status: ACTIVE
- Decision or event: The R000 exit gate is closed as PASS. Both the Implementation Plan E2E gate and the Digital Living Lifeform charter gate were evaluated criterion by criterion and every criterion passed. The Android shell was qualified on physical Tensor hardware rather than an emulator.
- Rationale: D005 required actual launch evidence and a reproducible hashed qualification bundle before R000 could close. Both exist, and the charter wording requiring target hardware was satisfied rather than only the weaker device-or-emulator wording.
- Affected areas: governance/release-gates/R000_EXIT_GATE.md, governance/qualification/R000_QUALIFICATION_BUNDLE.md, governance/qualification/QUALIFICATION_EVIDENCE_INDEX.md
- Supersedes record: none

## DEC-0011

- Date: 2026-08-12
- Record or decision ID: DEC-0011
- Status: ACTIVE
- Decision or event: Future production resource budgets remain NOT ESTABLISHED. Only measured observations of the empty R000 shell were recorded, under the identifier R000_MEASURED_BASELINE.
- Rationale: D005 prohibited inventing future target values. A guessed ceiling recorded now would be treated as a contract by later phases and would have to be unwound. Device evidence, not guessed constants, freezes production limits.
- Affected areas: docs/release/DEVICE_AND_RESOURCE_BUDGETS.md, governance/release-gates/R000_EXIT_GATE.md
- Supersedes record: none
