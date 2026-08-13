# R002 replay and recovery evidence

Two claims are recorded here, and they are different claims.

**Replay equivalence.** Folding an event sequence directly and replaying the same
sequence from the durable medium produce byte-identical canonical state.

**Recovery correctness.** After a restart, the recovered state is exactly the
last durably acknowledged one — nothing later, nothing invented, and nothing
already shown to the user missing.

## Why reconciliation is replayable at all

Reconciliation does not mutate state. It **plans a deterministic event sequence**
and folds that sequence through the ordinary reducer. Two properties follow from
that shape and are very hard to get any other way:

- `reduce == replay` holds for a reconciliation, because the reconciliation *is*
  a list of canonical events in the journal;
- a reconciliation interrupted at any chunk resumes from its cursor and produces
  exactly the sequence an uninterrupted run would have produced.

A reconciliation that wrote state directly would leave nothing in the journal to
replay, and a restart in the middle of one would be unrecoverable by
construction.

## Replay equivalence

| Fixture | Elapsed reconciled | Chunks | Events journalled | Direct vs replayed |
|---|---|---|---|---|
| `FX-REPLAY-EQUIVALENCE-01` | 30 hours verified | 600 | 1,203 | identical |

The replayed state is reconstructed from the encrypted durable records alone,
decrypted, decoded and folded from genesis. Nothing in memory survives into the
comparison.

## Slice independence

`FX-SLICED-RESUME-01` runs the same eight-hour absence twice: once uninterrupted,
once one chunk at a time. `OfflineReconciliationTest` repeats it at slice sizes
1, 2, 7, 64 and 10,000.

| Property | Result |
|---|---|
| Event count | identical at every slice size |
| Event bytes, position by position | identical at every slice size |
| Final state hash | identical at every slice size |

This is what makes "reconciliation yields between bounded batches" implementable.
A slice boundary that changed the result would make the canonical requirement
that reconciliation stay off the UI thread impossible to satisfy honestly.

## Recovery boundaries

| Boundary | Recovered state | Presentations emitted on recovery |
|---|---|---|
| Before the durable write starts | last acknowledged | 0 |
| During the durable write (torn tail) | last acknowledged | 0 |
| After durable acknowledgement | includes the transition | 0 |
| During checkpoint install | last acknowledged; nothing pruned | 0 |
| After checkpoint install, before prune | checkpoint plus tail | 0 |
| During a durability safe hold | last acknowledged; hold still active | 0 |
| During deep suspend, anchor committed | includes the suspend anchor | 0 |
| During deep suspend, anchor failed | last acknowledged | 0 |

Recovery emits **zero** presentations on every row. That is the at-most-once
guarantee carried forward from R001: the presentation token is consumed when the
durable write starts, so a process that dies mid-animation recovers the canonical
interaction and emits no delayed reaction.

## Discarded work

Canonical work after the last durable acknowledgement is discarded. Because Class
W visibility is gated on acknowledgement, discarding it cannot erase a mutation
already shown to the user — the property `FX-PANIC-WITNESS-01` establishes
directly by making three witnessed transitions visible, then failing the suspend
anchor, then confirming every one of them still exists in the recovered history.

The maximum rollback exposure is therefore bounded by the Class O uncommitted
window, frozen at 1,000 ms, and contains no user-visible material mutation by
construction.

## Reproducing

```
./gradlew :core-continuity:test
./gradlew :desktop-runner:run
tools/qualify_r002_continuity.sh <adb-serial> <target-label>
```
