# LocalStorageCryptographyContractV2

- Status: `FROZEN`
- Version: 2
- Frozen under: D013
- Supersedes: `LocalStorageCryptographyContractV1` version 1, `SUPERSEDED`
- Amends: `ContinuityDurabilityContractV1` sections 13.3, 13.4 and 13.5
- Executable portion: `core-persistence/.../LocalStorageCryptography.kt`,
  `core-continuity/.../DurableStore.kt`
- Qualification evidence: `qualification/fixtures/R012/R012_REPORT.txt`, section
  `EPOCH_SEPARATION`; `qualification/device-matrix/R012/x86_emulator_qualification.txt`

The encrypted local-record boundary and the key lifecycle around it. V1's
structure, algorithms and failure taxonomy are carried forward unchanged. This
version corrects exactly one thing, and everything below follows from it.

---

## What V1 got wrong

V1 used a single number, `keyEpoch`, for two unrelated quantities:

- how many times the **device wrapping material** had been rotated;
- which **data key** a record was sealed under.

Those two move on completely different schedules. Rotating the container material
is routine key hygiene, and V1 promised — in this document — that it "rewraps one
key rather than re-encrypting every record ever written". But the record path
derived its nonce and AAD from the same number, and refused any record whose
stored value differed from the container's current one. So the first rotation
made the entire journal unreadable.

That is not a tolerable failure. Section "the rule that outranks the rest" says a
cryptographic failure never creates a new organism; a rotation that orphans every
record leads to a next startup that finds no readable history, which is the same
harm arriving by a different road.

**Found by:** `DV-KS-ROTATION-READBACK-01`, D012, reporting
`readableAfterRotation=0/5`. It reproduced on the desktop; D011 missed it because
its rotation fixture checked that the data key was unchanged and never then read
a record back.

---

## The three quantities, kept apart

| Quantity | Lives in | Changes when | Reaches a record |
|---|---|---|---|
| `wrappingEpoch` | key state, `KeyContainer.keyEpoch` | container material is rotated | **never** |
| `dataKeyId` | key state, `KeyContainer.dataKeyId` | the DEK itself is rotated | at write time |
| record encryption context | the record's own plaintext header | never — it is immutable | it *is* the record's |

A record is decrypted using **the currently recovered DEK** and **its own stored
context**. Not the container's current wrapping epoch, which is mutable state the
record has no relationship to.

### Frozen record rules

```
context = the dataKeyId in force when the record was sealed   (u32, immutable)

nonce   = u32(context) || u64(recordSequence)                 // 12 bytes
AAD     = u32(schemaId) || u32(schemaVersion) || i64(generationId)
        || i64(recordSequence) || i64(organismId) || u32(context)
```

The record byte layout is **unchanged from V1**. The field is in the same
position and is the same width; what changed is where its value comes from.

| Field | Frozen value |
|---|---|
| Key state schema | `231` v2 (v1 readable, migrated) |
| Initial wrapping epoch | 1 |
| Initial `dataKeyId` | 1 |
| Record schema | `213` v1, unchanged |
| Record nonce | derived from `(context, sequence)`, never random |
| Wrap nonce | derived from `(wrappingEpoch, organismId)`, never random |
| Record AAD | schema id, schema version, generation, sequence, organism id, context |
| Wrap AAD | organism id, device fingerprint, wrapping epoch |

Nonce uniqueness now rests on the sequence being monotonic and never reused under
one data key, which the single-writer append-only journal already guarantees. The
context prefix keeps uniqueness intact across an eventual data-key rotation as
well.

## Why removing the epoch pre-check is not a weakening

V1 refused a record whose stored epoch differed from the container's current one,
before attempting decryption. Removing that check removes no guarantee, because
the context is **inside the AAD**:

- a record whose context is rewritten fails authentication;
- a record sealed under a different data key fails authentication;
- a record moved to another sequence, generation or organism fails
  authentication, exactly as in V1.

What the pre-check actually did was refuse *honest* records for carrying a value
that had legitimately changed. What refuses a record now is the cryptography
rather than a comparison against mutable state — which is the stronger of the
two, and the only one that was ever load-bearing.

Qualified by `FX-EPOCH-CONTEXT-AUTHENTICATED-01` (four forged header fields, all
refused) and `FX-EPOCH-FOREIGN-KEY-01`.

## Wrapping rotation

Unchanged from V1: two durable steps, staging file, atomic rename, resolved at
every open. What V2 adds is that it now does what V1 said it did.

| Guarantee | Fixture |
|---|---|
| History readable after one rotation | `FX-EPOCH-ROTATE-ONCE-01`, `DV-KS-ROTATION-READBACK-01` |
| History readable after repeated rotations, mixed with newer records, across restart | `FX-EPOCH-ROTATE-MANY-01` |
| No journal byte is rewritten | `FX-EPOCH-NO-REWRITE-01` |
| The DEK and its identity are untouched | `FX-EPOCH-DATAKEY-STABLE-01` |
| An unusable pending wrap is abandoned, leaving the organism intact | `FX-EPOCH-ABANDON-01` |
| An interrupted rotation resolves at the next open | `FX-CRYPTO-REWRAP-RESUME-01`, `DV-FLT-PENDING-REWRAP-01` |

An abandoned rotation consumes an epoch number and costs nothing else.

## DEK rotation is not implemented

`dataKeyId` exists so that an actual data-key rotation *can* be designed later
without another layout change. It is not implemented, and nothing here should be
read as implying it is. A real DEK rotation needs a separately frozen design for
key-ring handling, historical-key retention, re-encryption and migration,
compaction interaction, and recovery compatibility — none of which is decided
here. `dataKeyId` is `1` for every organism in existence.

**A wrapping rotation must never be turned into a DEK rotation.** They have
opposite costs: one rewraps 32 bytes, the other rewrites a life.

## V1 compatibility

| Artifact | Treatment |
|---|---|
| Existing encrypted records | **Not touched.** Read through their own stored context |
| Existing key state (schema 231 v1) | Migrated in place to v2 at the next open |
| Canonical plaintext | Byte-identical before and after |
| Canonical state hashes | Unchanged. Encryption has never been inside them |

The migration adds `dataKeyId = 1` and rewrites one small file. It is:

- **deterministic** — a pure function of the decoded state, with no clock, no
  randomness and no device read in it;
- **idempotent** — migrating twice produces the same bytes as once
  (`FX-V1-MIGRATION-IDEMPOTENT-01`);
- **crash-safe** — one staging write, one atomic rename, two boundaries:

| Death at | Recovers as |
|---|---|
| before the rename | the readable V1 state; the next open migrates again to identical bytes |
| after the rename | the complete migrated V2 state; the next open does nothing |

There is no third state. Qualified under real process death by
`FX-V1-MIGRATION-CRASH-STAGED-01` and `FX-V1-MIGRATION-CRASH-RENAMED-01`, and on
Keystore material by `DV-KS-V1-MIGRATION-01`.

Every V1 organism has exactly one data key and has never rotated it, because V1
had no way to. That is why the migrated identity is the initial one, always.

Key state carrying a schema version this build does not recognise is **refused**,
not guessed at (`FX-V1-MIGRATION-FUTURE-REFUSED-01`). Reinterpreting an unknown
layout is how a downgrade turns into a birth.

---

## Carried forward from V1, unchanged

The rule that outranks the rest — **a cryptographic failure never creates a new
organism** — and with it: the frozen algorithms and their in-project
implementations; the key structure and HKDF derivation; what stays outside the
ciphertext; the three-fault taxonomy (`CONTAINER_UNAVAILABLE`, `DEVICE_MISMATCH`,
`WRAPPED_KEY_UNAUTHENTIC`); quarantine as a file that survives restart, with key
state retained; and the deletion order, wrapping material first.

`KeyFault.EPOCH_MISMATCH` is retained in the enum and is no longer reachable from
the record path. Removing an ordinal from a canonical enum is a byte-layout
change, and it would buy nothing.

## Blocked

| Item | State |
|---|---|
| Data-encryption-key rotation | Not designed, not implemented, not authorized |
| StrongBox / TEE attestation policy | Not frozen; requires device capability evidence |
| Rotation cadence | Not frozen; a cadence is a parameter and no device evidence exists |
| Android Keystore hardware qualification | `BLOCKED_DEVICE_UNAVAILABLE` under D012, unchanged by D013 |
