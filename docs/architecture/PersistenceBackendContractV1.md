# PersistenceBackendContractV1

- Status: `FROZEN`
- Version: 1
- Frozen under: D011
- Executable portion: `core-persistence/.../PersistenceBackend.kt`
- Selection evidence: `qualification/evidence/R012/backend_benchmark.txt`
- Qualification evidence: `qualification/fixtures/R012/R012_REPORT.txt`

The production storage stack. Derived from Implementation Plan E2E R012.8, which
requires this contract to be frozen *before* production storage is wired, and
which permits benchmarking candidates in isolation but forbids choosing one
implicitly inside production code.

---

## The selected backend

```
BACKEND_ID = SEGMENTED_APPEND_LOG_V1
```

A single-writer append-only log on the filesystem, with copy-forward compaction,
an `fsync` on every acknowledged commit, and authenticated encryption applied
above it by `LocalStorageCryptographyContractV1`.

**Not Room. Not SQLite.** The canonical plan warns specifically against choosing
a backend because it is conventional, and the measurement below is why the
conventional answer lost.

### What the workload actually is

R002 already fixed it, and it is not a database workload:

| Property | Consequence |
|---|---|
| Single writer, no concurrent writers | No locking, no contention, no connection pool |
| Append-only; records are never updated in place | No page rewrites, no rollback journal of our own |
| Never queried; read only in sequence order | No index, no planner, no query language |
| Whole surviving history replayed at startup | A scan is the access pattern, not a fallback |
| Pruned wholesale below a checkpoint | Deletion is truncation, not row-by-row work |
| Acknowledgement must mean survives process death | One `fsync`, and it must be ours |

### The measurement

Reference machine, ext4 on NVMe, 2,000 commits of 512 bytes after 200 warmup
commits. Full output in `qualification/evidence/R012/backend_benchmark.txt`.

| Candidate | mean µs | p50 | p99 | max | replay ms | footprint |
|---|---|---|---|---|---|---|
| **C1 append log, `force(true)`** | **414** | **386** | **656** | 6451 | 20.6 | **1,152,800** |
| C2 append log, `force(false)` | 410 | 385 | 639 | 3537 | 11.7 | 1,152,800 |
| C3 `RandomAccessFile("rwd")` | 437 | 403 | 736 | 3391 | 26.0 | 1,152,800 |
| C4 whole-file rewrite per commit | 2955 | 1521 | 17984 | 77252 | 1.6 | 1,152,800 |
| C5 SQLite WAL, `synchronous=FULL` | 571 | 430 | 3491 | 17387 | 10.2 | 5,082,640 |
| C6 SQLite DELETE, `synchronous=FULL` | 2182 | 1312 | 16102 | 31584 | 3.7 | 1,298,432 |

### Why C1

- **Tail latency.** SQLite in WAL mode costs 5.3× the p99 and 2.6× the worst
  case. p99 is the number that decides whether a witnessed commit stalls a frame.
- **Footprint.** SQLite in WAL mode used 4.4× the storage for the same records —
  5.08 MB against a frozen 8 MB journal budget, so 62% of the budget would be
  spent on engine overhead before the organism stored anything.
- **One crash-recovery story.** A general-purpose engine has its own durability
  machinery underneath ours; a durability claim would then rest on two engines
  agreeing rather than on one `fsync` returning.
- **`force(true)` over `force(false)` at no measurable cost.** C2 was 1% faster
  and does not force metadata. A growing file's length *is* metadata, so C2 can
  acknowledge a record whose length update was lost. The safer option was free.

C4 is rejected outright: its cost grows with history, and by 2,000 records it was
already 7× worse with an 18 ms p99.

### What this measurement is not

It is a desktop NVMe measurement on ext4. **Android flash is different**, and no
Android device figures exist because no device was available to this directive.
The *shape* of the argument — no queries, single writer, replay-only reads — is a
property of the workload and not of the device, and it is what selects the
backend. The latencies below are reference figures, and no production threshold
is derived from them.

---

## Frozen layout

| Item | Value |
|---|---|
| Journal file | `journal.dll17` |
| Compaction staging | `journal.compacting` |
| Checkpoint file | `checkpoint.dll17` |
| Checkpoint staging | `checkpoint.staging` |
| Key state | `keystate.dll17` (+ `keystate.staging`) |
| Quarantine marker | `quarantine.dll17` |
| Frame magic | `0x444C3137`, present at both ends of every frame |
| Frame header | 16 bytes: magic, length, sequence |
| Frame trailer | 8 bytes: length, magic |
| Maximum record | 1 MiB |
| Journal byte budget | inherited from `ContinuityDurabilityContractV1` — 8 MiB |
| Emergency reserve | 512 KiB |
| Commit force policy | `FileChannel.force(true)` — data **and** metadata |

The length appears twice on purpose. A torn tail must be detectable without
trusting the file length, because the file length is metadata the filesystem may
have updated before the body reached the device.

---

## Frozen semantics

### Acknowledgement

`append` returns normally only after the frame's bytes and the file's metadata
have been forced. That is the entire durability promise, and it is the boundary
`ContinuityDurabilityContractV1` requires.

Class O records may be batched through `appendWithoutForce` + `forceNow`. That is
a durability *class* decision made by the caller, not a storage optimisation:
losing the tail of ordinary progress is recoverable and losing a witnessed
transition is not. Measured cost of batching, reference machine: 412 µs per
record unbatched, 12.5 µs per record at a batch of 64.

### Torn tail versus corruption

The distinction is load-bearing and is frozen as follows.

| Observation | Classification |
|---|---|
| Fewer bytes remaining than a frame header | Torn tail — truncate |
| Declared length exceeds the remaining bytes | Torn tail — truncate |
| Wrong magic, anywhere | **Corruption** — fault |
| Impossible length, anywhere | **Corruption** — fault |
| Trailer present but mismatched | **Corruption** — fault |
| Sequence not advancing | **Corruption** — fault |

A partial write cannot produce a wrong magic or an impossible length, because the
magic is the first thing written and a length is either wholly present or wholly
absent. Treating a broken *first* frame as "an empty journal" is specifically
prohibited: it would present a corrupt installation as a device with no organism
on it, which is the worst available failure.

### Compaction

Copy-forward: survivors are written to a staging file, forced, then atomically
renamed over the journal. At no instant does one file hold a partially rewritten
history. **An unrenamed staging file is discarded, never adopted** — only the
rename makes it authoritative.

### Capacity

An exhausted medium refuses the write and stays **readable and compactable**.
Capacity exhaustion is deliberately not a sticky fault: a full journal that could
not be read or compacted would have no exit except losing history, which is what
the emergency reserve and this rule exist to prevent.

### Self-test

Required before leaving `STORAGE_FAULT`. It writes a probe, forces it, reads it
back and removes it — the same path a commit takes. A self-test that only checked
free space would pass on a device that has stopped accepting writes.

---

## Measured reference performance

From `qualification/evidence/R012/performance.txt`, three repetitions reported in
full rather than a best run.

| Measure | Value |
|---|---|
| Class W commit, mean | 406–497 µs across three runs |
| Class W commit, p99 | 662–856 µs |
| Class O at batch 64, per record | 12.5 µs |
| Checkpoint write, mean | 900 µs |
| Replay, 1,500 records | 49.7 ms |
| Compaction, 1,500 → 750 records | 11.0 ms |
| Encrypted-record overhead | 74 bytes per record, ratio 1.264 |
| Cold package, 750-record tail | 205,456 bytes, 30.0 ms to create, 2.8 ms to open |

**No production threshold is derived from these.** Deriving one would require
Android device measurements that do not exist, and the canonical plan forbids
fabricating production limits.

---

## Blocked

| Item | State |
|---|---|
| Android device backend qualification | `BLOCKED_DEVICE_UNAVAILABLE` — no device or emulator was reachable under D011 |
| Production latency and footprint thresholds | Not derived; require device evidence |
| Read-only survival under a real read-only filesystem mount | Exercised by revoking write permission on the journal, not by remounting |
