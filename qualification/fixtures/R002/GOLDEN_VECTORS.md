# R002 golden vectors

Frozen fixture set for the continuity and durability qualification.

- Fixture set: `R002-FIXTURES-V1` version 1
- Continuity contract: `ContinuityDurabilityContractV1` version 1
- Determinism contract: `DeterminismContractV1` version 1, unchanged
- Golden evidence digest:
  `556bbe49df16595f748a487f78a17a83866eb2a018814f69ee469d7976d58d21`
- Executable form: `core-continuity`, `R002QualificationKernel`

Every fixture is a compiled-in constant. The kernel reads no clock, no file, no
environment variable and no device property, which is what makes its digest
comparable between a desktop JVM, an emulator and physical hardware.

## Per-fixture results

| Fixture | Area | Final state hash | Chunks | Events | Result | What it establishes |
|---|---|---|---|---|---|---|
| `FX-VERIFIED-ABSENCE-01` | reconciliation | `8f1f35bf05da0dce…` | 360 | 723 | `PASS` | six verified hours reconciled in 360 chunks |
| `FX-LONG-ABSENCE-01` | reconciliation | `4b2bd0a258a7830b…` | 768 | 1541 | `PASS` | thirty verified days, bounded chunking then projection |
| `FX-SLICED-RESUME-01` | reconciliation | `237be255a8cb4ded…` | 384 | 771 | `PASS` | single-chunk slices reproduce the uninterrupted sequence exactly |
| `FX-REBOOT-01` | trusted-time | `b0c8bec3c6738754…` | 0 | 4 | `PASS` | reboot consumed 3600000ms of credit and accrued 7200000ms of debt |
| `FX-REPEATED-REBOOT-01` | trusted-time | `c8db6d463cb256a9…` | 0 | 0 | `PASS` | eight reboots consumed 14400000ms total, velocity anomaly=true |
| `FX-CLOCK-BACKWARD-01` | clock-anomaly | `5f43b795974e4d96…` | 0 | 3 | `PASS` | backward wall movement produced no negative time and no development |
| `FX-CLOCK-FORWARD-01` | clock-anomaly | `08bd85a70ec5790f…` | 0 | 3 | `PASS` | a ten-day wall jump generated no reward-bearing progression |
| `FX-DEBT-FLOOR-01` | debt | `fe2b3fb07942be62…` | 0 | 0 | `PASS` | collection paused at the floor and did not rearm on the restoring care event |
| `FX-DEBT-CAP-01` | debt | `3824ef91e61ee03b…` | 0 | 4 | `PASS` | one year of uncertainty capped at 72h with 31276200000ms forgiven at accrual |
| `FX-DEATH-BEFORE-COMMIT-01` | restart-recovery | `082e47e3b3308b43…` | 0 | 1 | `PASS` | an interrupted witnessed write left no trace and no visible mutation |
| `FX-DEATH-AFTER-COMMIT-01` | restart-recovery | `3ecdf6a8be0dcf46…` | 0 | 2 | `PASS` | an acknowledged transition survived process death exactly |
| `FX-INTERRUPTED-SNAPSHOT-01` | compaction | `95014d59f7b81b84…` | 0 | 9 | `PASS` | a failed checkpoint install pruned nothing: checkpoint install failed, nothing pruned: simulated write failure at sequence 10 |
| `FX-COMPACTION-01` | compaction | `60206065bba3364f…` | 0 | 0 | `PASS` | compaction installed a verified checkpoint and recovery reproduced the state |
| `FX-STORAGE-PRESSURE-01` | storage-pressure | `97490eb4fe8bfdbd…` | 0 | 1 | `PASS` | pressure then read-only survival, entered in the normative order |
| `FX-SAFE-HOLD-EXIT-01` | safe-hold | `e5a2999f1fb656f3…` | 0 | 1349 | `PASS` | the held interval was reconciled in full before admission reopened |
| `FX-STORAGE-FAULT-01` | storage-fault | `a6e1ed8006ac8423…` | 0 | 0 | `PASS` | a failed emergency commit fell back to the last durable anchor |
| `FX-DEEP-SUSPEND-01` | platform-deep-suspend | `a459137ab56b485a…` | 0 | 1 | `PASS` | a successful anchor made the panic witness unnecessary |
| `FX-PANIC-WITNESS-01` | platform-deep-suspend | `4497bf77bc8f9c71…` | 0 | 0 | `PASS` | anchor failed, one witness attempt, no visible material mutation lost |
| `FX-BOTH-WRITES-FAIL-01` | platform-deep-suspend | `082e47e3b3308b43…` | 0 | 0 | `PASS` | both writes failed and recovery still began at the last durable anchor |
| `FX-PLATFORM-RECOVERY-01` | platform-recovery | `b020b737e8378cf1…` | 0 | 244 | `PASS` | hysteresis refused an early restart and the later one reconciled two hours |
| `FX-ENCRYPTED-RECORD-01` | encrypted-record | `375445abe94120d1…` | 0 | 4 | `PASS` | records authenticate, leak no canonical plaintext, and fail loudly when corrupted |
| `FX-QUARANTINE-01` | identity-binding | `705513f0290dfbe8…` | 0 | 1 | `PASS` | a copied database did not boot as a second organism |
| `FX-MIGRATION-01` | version-boundary | `9c8abd84595b8cf6…` | 0 | 0 | `PASS` | v0 migrated deterministically and a future version was refused |
| `FX-REPLAY-EQUIVALENCE-01` | replay-determinism | `677d8e77345ed1d1…` | 600 | 1203 | `PASS` | a thirty-hour reconciliation replayed from the durable medium byte for byte |

## Per-section digests

| Section | Digest | What it covers |
|---|---|---|
| trust | `32c553602ec91f167d6d0eaa418dc1ac49dd291491cee0d969c2b920cb83ddaa` | Confidence classification across every anchor and observation shape, including backward and forward wall movement, boot changes and uptime resets |
| reconciliation | `4d0237959e543f7c99192549f97989d8a9d7fa418fb3b63560133cd88148379d` | Mode selection and chunk size at every boundary and either side of it, plus circadian phase advance |
| debt | `591ec340b36eb158dc1a861891d08227da4628cee407f27ba151bbbf44cc57db` | Credit replenishment including the fragmentation-equivalence property, and consumption under every refusal condition |
| durability | `c73563ca850c05fd629f75a7758bb3ba77e36ba212047edf92f292474bd1023b` | Thresholds, both normative orderings, the shed order, every state ordinal, and the prohibited-vocabulary check |
| encryption | `6db8d74c470473b369c2009f8b4e36cad76f9f8b052aa030000ac4db8bb79e44` | AEAD seal and open across every length spanning the block and MAC-remainder boundaries |
| replay | `712c72e954725d4d83415c385debe86350895244e08bac7c6b691f324b2c531c` | The composite of every fixture's final state hash |

Per-section digests exist so that a cross-target failure names the subsystem
that diverged rather than only reporting a different total.

## Why the digest, not the outcome

A fixture that merely executes proves nothing. Each one asserts a specific
defence — that a reboot loop earns nothing, that a wall-clock jump grants no
progression, that a torn write leaves no trace — and the digest binds the exact
canonical state each defence produced. A target that runs the kernel and
computes a different digest has failed, whether or not its own assertions passed.

## Reproducing

```
./gradlew :desktop-runner:run
./gradlew :core-continuity:test
tools/qualify_r002_continuity.sh <adb-serial> <target-label>
```
