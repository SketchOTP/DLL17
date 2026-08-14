# R012 persistence, recovery and identity substrate gate

- Phase: R012 — bounded parallel substrate authorized by the 2026-08-14
  parallel-execution amendment while A001 is externally blocked
- Directive: D011
- Contracts frozen: `PersistenceBackendContractV1`,
  `LocalStorageCryptographyContractV1`, `RecoveryCryptographyContractV1`,
  `IdentityAuthorityProtocolV1`, `RecoveryPackageStoreContractV1`
- Qualified fixture set: `R012-FIXTURES-V1` version 1
- Golden evidence digest:
  `48bd44a31a3a952cf884b358c5e587b93a24875f1d36d7625e6b4b7d5f62127f`
- Evaluated: 2026-08-14

**This is not the R012 exit gate.** The R012 exit gate covers the whole Android
product — sensors, UX, notifications, assets, onboarding — and almost none of
that is authorized while A001 is blocked. This gate covers exactly the substrate
the amendment authorized, and it says so rather than implying more.

---

## What the amendment authorized, and what was done

| Authorized | State |
|---|---|
| Production persistence-backend selection and qualification | Done. Selected from measurement. |
| Local storage cryptography | Done, including rotation and interrupted rewrap. |
| Recovery cryptography | Done. `BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY` is cleared. |
| Identity-authority protocol | Done, with a separately deployable service. |
| Recovery-package storage abstraction and one implementation | Done, with a provider conformance suite. |
| Concrete persistence/recovery plumbing to qualify the above | Done. |

| Not authorized, not done | |
|---|---|
| R012 product completion, UX, sensors, notifications, dialogue | Untouched |
| Any R003–R009 production organism mechanism | Untouched |
| A001 human work | Untouched |

---

## Qualification result

**42 fixtures, 42 held, 0 not held.** Full report:
`qualification/fixtures/R012/R012_REPORT.txt`.

| Section | Fixtures | Result |
|---|---|---|
| Backend | 6 | all held |
| Cryptography | 6 | all held |
| Recovery | 11 | all held |
| Identity authority | 8 | all held |
| Fault matrix | 8 | all held |
| Determinism | 3 | all held |

### Backend selection

Six candidates benchmarked in isolation on ext4/NVMe. `SEGMENTED_APPEND_LOG_V1`
selected: SQLite in WAL mode cost 5.3× the p99 commit latency and 4.4× the
storage for a workload that never issues a query. Evidence:
`qualification/evidence/R012/backend_benchmark.txt`.

The first benchmark run was discarded: the system temp directory on this machine
is tmpfs, where `fsync` is a no-op and every candidate looked two microseconds
fast. Both harnesses now refuse to run on tmpfs.

### Fault matrix, with real process death

Child JVMs killed with `Runtime.halt`, which skips shutdown hooks and finalizers.

| Fault | Result |
|---|---|
| Process death after acknowledged commit | 25/25 records recovered |
| Process death mid-frame | torn frame dropped, next append clean |
| Interrupted compaction | staging discarded, 15/15 records intact |
| Interrupted snapshot | previous checkpoint still authoritative |
| Corruption before the tail | fault raised, no silent truncation |
| Corrupt first frame | fault raised, **not** reported as an empty journal |
| Full storage | write refused, history readable and compactable |
| Write refused | existing history readable |
| Eight consecutive restarts | identical history each time |

### Determinism

Canonical hash identical across different device secrets, different data keys and
different ciphertext; identical across a full cold-recovery round trip; checkpoint
hash independent of storage location. R001 and R002 digests unchanged.

---

## What this gate does **not** establish

1. **No Android device evidence exists.** No device or emulator was reachable
   under D011. The backend, the key container and the fault matrix are qualified
   on the desktop JVM against a real ext4/NVMe filesystem, and the Android
   Keystore implementation of `DeviceKeyContainer` is `BLOCKED_DEVICE_UNAVAILABLE`.
2. **Power loss is not proven.** Killing a process does not empty the OS page
   cache, so no test here distinguishes "forced" from "written but not forced".
   The durability claim rests on forcing every commit before acknowledging it —
   a policy — and the fault matrix proves the recovery logic around it.
3. **No production thresholds are derived.** The measured latencies are
   reference-machine figures. Deriving a production limit from them would require
   device evidence that does not exist.
4. **The qualifying recovery provider is filesystem-backed.** It is a real
   provider and it is what end-to-end cold recovery runs against; it is not a
   network provider, and selecting one is a product decision with an owner.
5. **The authority has no deployment.** Transport, hosting, backup and incident
   procedures are `BLOCKED_SPEC_SERVICE_OPERATIONS`.

## Gate state

**R012 substrate: `COMPLETE` for the authorized scope.** The R012 phase gate
remains open, and A001 remains the gate that blocks R003–R009.
