# ThreadAndDispatcherMap

- Registry version: `V1`
- Status: `SCAFFOLD_PARTIAL`
- Created: R000 (directive D004)
- Owner gate: the phase that introduces each execution context

Authoritative ownership map for every execution context. The canonical reducer
remains single-threaded; that is an invariant, not a tuning choice.

## Required contexts

| Context | Owner | R000 state |
|---|---|---|
| UI / render | `android-host` | Exists as the Android main thread hosting the R000 shell. No organism rendering. |
| Reducer | `core-state` | Not created. Single-threaded when it exists; this is an invariant. |
| Persistence writer | `android-host` | Not created. R002. |
| Compactor | `android-host` | Not created. R002. |
| Reconciliation | not yet assigned | Not created. |
| Sensor normalization | `android-host` | Not created. |
| Diagnostics | not yet assigned | Not created. |
| Model / TTS | `android-host` | Not created. Read-only verbalization boundary applies when it exists. |
| Platform protection | `android-host` | Not created. |

## Entries

Only the Android main thread exists in R000, and it does nothing but compose a
static informational surface. `desktop-runner` executes on the JVM main thread
and terminates; it owns no long-lived context.

Every other row above is a declared future owner, not an implemented context.
