# ThreadAndDispatcherMap

- Registry version: `V1`
- Status: `POPULATED_R001`
- Created: R000 (directive D004)
- Owner gate: the phase that introduces each execution context

Authoritative ownership map for every execution context. The canonical reducer
remains single-threaded; that is an invariant, not a tuning choice.

## Required contexts

| Context | Owner | State after R001 |
|---|---|---|
| UI / render | `android-host` | Android main thread hosting the unchanged R000 shell. No organism rendering. |
| Reducer | `core-state` | **Exists.** `CanonicalReducer` is a pure function and is single-threaded by invariant INV-0002. Nothing in it is synchronized, deliberately: the correct response to concurrent access is to stop calling it from two threads, not to make it thread-safe. |
| Persistence writer | `android-host` | Not created. R001 uses an in-process append-only durable journal to prove its invariants; real persistence semantics are R002. |
| Compactor | `android-host` | Not created. R002. |
| Reconciliation | not yet assigned | Not created. R002. |
| Sensor normalization | `android-host` | Not created. |
| Diagnostics | `core-math` | **Exists in part.** `SaturationObserver` receives records on the calling thread; it is noncanonical and never influences a reduction. |
| Model / TTS | `android-host` | Not created. |
| Platform protection | `android-host` | Partially specified. `PlatformPanicWitness` exists and must never be written on the Android UI thread; R001 does not yet wire a caller. |

## Entries

R001 adds no long-lived execution context. The reducer, the journal and the
replay kernel all execute on the caller's thread and hold no background work.

The Android instrumented determinism test runs on the instrumentation thread,
which is not the UI thread. That is incidental to the test rather than a declared
context.

`desktop-runner` executes on the JVM main thread and terminates; it owns no
long-lived context.


---

## R002 additions

| Context | Owner | State after R002 |
|---|---|---|
| Persistence writer | `core-continuity` | **Exists as a contract.** `SingleWriterActor` serializes every durable write and refuses reentry. It is a discipline, not a thread: a real dispatcher may implement it, and a violation fails loudly at the call site rather than producing an interleaved journal in one timing window. |
| Compactor | `core-continuity` | **Exists.** `ContinuityJournal.compact` builds and verifies the candidate checkpoint outside the writer, then submits only the bounded install through the writer, then prunes. |
| Reconciliation | `core-continuity` | **Exists.** `Reconciliation` is a resumable pure computation with an explicit cursor. It never runs on the Android UI thread, and it yields between bounded batches without changing its result. |
| Platform protection | `core-continuity` | **Exists.** `PlatformProtectionController` performs at most one anchor attempt and at most one panic-witness attempt. The witness must never be written on the Android UI thread. |
| Interaction gate | `core-continuity` | **Exists.** `InteractionGate` discards rather than queues while reconciliation is pending, and counts what it discarded so that "consumed no item" is checkable. |

No context here is bound to a thread by `core-continuity` itself. The module is
pure Kotlin and holds no dispatcher; binding these contexts to real Android
threads is the hosting phase's work.
