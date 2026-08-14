# LocalStorageCryptographyContractV1

- Status: `FROZEN`
- Version: 1
- Frozen under: D011
- Executable portion: `core-persistence/.../LocalStorageCryptography.kt`
- Qualification evidence: `qualification/fixtures/R012/R012_REPORT.txt`

The encrypted local-record boundary and the key lifecycle around it. Derived from
Implementation Plan E2E R012.17, whose structural rule is fixed and is restated
here only so the implementation can be checked against it.

---

## The rule that outranks the rest

**A cryptographic failure never creates a new organism.**

Every failure path refuses or quarantines. None returns a fresh key, because a
fresh key silently orphans every existing record and the next startup looks like
a birth. The failure to avoid is not "data is unreadable" — it is "the product
cheerfully replaced someone's creature with a new one".

---

## Frozen algorithms

| Purpose | Algorithm |
|---|---|
| Record encryption | `AEAD_CHACHA20_POLY1305_V1` |
| Data-key wrapping | `AEAD_CHACHA20_POLY1305_V1` |
| Wrapping-key derivation | `HKDF_SHA256_V1` |
| Domain separation string | `DLL17-LOCAL-WRAP-V1` |

HKDF, HMAC-SHA-256 and PBKDF2 are implemented in project for the reason SHA-256
and ChaCha20-Poly1305 already were (IMPL-0008, IMPL-0018): the algorithms are
standardized, provider *resolution* is not, and a key a device can derive today
and cannot derive after an OS update is unrecoverable data loss. They are checked
against RFC 4231, RFC 5869 and RFC 7914 vectors.

## Frozen key structure

```
device key container  (Android Keystore; non-exportable)
        │  root secret
        ▼
  HKDF(salt = organismId ‖ keyEpoch, info = DLL17-LOCAL-WRAP-V1)
        │  wrapping key, 32 bytes
        ▼
  AEAD-wrapped data-encryption key   ← persisted in keystate.dll17
        │  unwrapped in memory only
        ▼
  per-record AEAD  (nonce = keyEpoch ‖ sequence, AAD = schema ‖ generation ‖
                    sequence ‖ organismId ‖ keyEpoch)
```

The data key is **random and never derived from the device secret**. That is what
makes rotation cheap: rotating the container material rewraps one key rather than
re-encrypting every record ever written.

| Field | Frozen value |
|---|---|
| Key state schema | `231` v1 |
| Initial key epoch | 1 |
| Record nonce | derived from `(keyEpoch, sequence)`, never random |
| Wrap nonce | derived from `(keyEpoch, organismId)`, never random |
| Record AAD | schema id, schema version, generation, sequence, organism id, key epoch |
| Wrap AAD | organism id, device fingerprint, key epoch |

Derived nonces rather than random ones: the sequence is monotonic within an
epoch and the epoch prefixes it, so reuse is structurally impossible rather than
probabilistically unlikely, and the write path needs no randomness source at all.

## What stays outside the ciphertext

Only what a reader must have *before* it can decrypt: schema id and version,
generation id, sequence, key epoch. Every one of those is bound into the AAD, so
a record cannot be reinterpreted at another journal position, under another
generation, or for another organism — the decryption simply fails.

There is no plaintext canonical payload anywhere on the medium. Asserted directly
by `FX-CRYPTO-AT-REST-01` and by a unit test that scans the raw journal bytes.

---

## Frozen lifecycle

### Rotation

Two durable steps, in this order:

1. write key state carrying both the current wrap and a `pending` wrap under the
   new epoch, and force it;
2. write key state with the new wrap promoted and the pending fields cleared, and
   force it.

Both writes go through a staging file and an atomic rename, so a death *during*
either leaves the previous state intact, and a death *between* them leaves step
one's file.

> **Open conflict, recorded 2026-08-14 under D012. Not resolved here.**
>
> The claim above that already-written records "stay readable" across a rotation
> is contradicted by the frozen `EncryptedRecordStore`, which refuses any record
> whose header key epoch differs from the container's current epoch and derives
> its nonce and AAD from that epoch. After a rotation the existing journal does
> not open at all. Evidence: fixture `DV-KS-ROTATION-READBACK-01`,
> `readableAfterRotation=0/5`.
>
> Both behaviours are frozen and they cannot both hold. Resolving it means either
> re-encrypting history on rotation or separating the *wrapping* epoch from the
> *record* key epoch, and the choice has real consequences, so it requires a
> versioned amendment and architect review. No value in this document has been
> changed on account of it.

### Interrupted rewrap

Resolved at every open, deterministically:

- pending wrap opens the data key → promote it;
- pending wrap does not open → abandon it, keep the current wrap.

Abandoning is safe precisely because the data key never changed: a failed
rotation costs an epoch number and nothing else.

### Failure taxonomy

Three distinct faults, never conflated, because they mean different things to the
user and imply different next steps:

| Fault | Meaning |
|---|---|
| `CONTAINER_UNAVAILABLE` | The container exists but will not release material — a copied database, or Keystore loss |
| `DEVICE_MISMATCH` | The key state is bound to a different device |
| `WRAPPED_KEY_UNAUTHENTIC` | The wrapped key does not authenticate under the derived wrapping key |

### Quarantine

`COPIED_STATE_QUARANTINE` is a **file** (`quarantine.dll17`), not an in-memory
flag, because the whole point is to survive the restart a copied database would
otherwise use to try again. It records a canonical reason identifier and free-text
detail. It is cleared only by a successful cold recovery — never by a retry.

Key state is retained through a quarantine. Deleting it would destroy the only
thing a later recovery could verify against.

### Deletion order

Wrapping material first, then local files. Reversing the order leaves a window in
which the data is recoverable and the key is not yet gone.

---

## Blocked

| Item | State |
|---|---|
| Android Keystore-backed `DeviceKeyContainer` | Implemented under D012 as `AndroidKeystoreDeviceKeyContainer`, an HMAC-SHA256 keystore key. Qualified on an emulator; `BLOCKED_DEVICE_UNAVAILABLE` on physical hardware |
| StrongBox / TEE attestation policy | Not frozen; requires device capability evidence. The adapter requests StrongBox, reports what the platform grants, and requires nothing |
| Rotation cadence | Not frozen; a cadence is a parameter and no device evidence exists |
