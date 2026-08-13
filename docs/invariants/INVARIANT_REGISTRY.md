# Invariant registry

Invariants may not be tuned away. Each entry names the invariant, its source and
where it is enforced today.

| ID | Invariant | Source | Enforcement in R000 |
|---|---|---|---|
| INV-0001 | Core modules never link the Android framework. | Implementation Plan E2E, section 2 | Enforced by build wiring and by a classpath test in each `core-*` module. |
| INV-0002 | The canonical reducer is single-threaded. | Implementation Plan E2E, `ThreadAndDispatcherMap` | Not yet applicable; no reducer exists. Recorded in the registry. |
| INV-0003 | No executable logic depends on a number that is absent from `ParameterRegistry`. | Implementation Plan E2E, section 1 | Trivially held; R000 implements no equation. |
| INV-0004 | Dependency versions are exact pins; dynamic versions are prohibited. | `ProjectIdentityBuildContractV1` | Enforced by `tools/verify_project_identity.py`. |
| INV-0005 | No organism implementation or state is inherited into this repository. | Implementation Plan E2E, R000 exit gate | Repository is greenfield; Git provenance begins at D-003 with no imported source. |
