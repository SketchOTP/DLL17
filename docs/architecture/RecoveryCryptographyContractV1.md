# RecoveryCryptographyContractV1

- Status: `FROZEN`
- Version: 1
- Frozen under: D011
- Executable portion: `core-recovery/.../RecoveryCryptography.kt`
- Qualification evidence: `qualification/fixtures/R012/R012_REPORT.txt`

Every value in this document was `BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY` under R002
and is now frozen. The canonical requirement (Digital Living Lifeform, R002.12)
is exact: recovery-secret entropy, encoding and checksum; key types; KDF and
wrapping algorithms; authenticated-encryption format; rotation and reissue
policy; package manifest and MAC format; the challenge-response proof used by the
identity authority; replay protection; and failure and revocation semantics.

---

## The recovery root

| Item | Frozen value |
|---|---|
| Entropy | 256 bits |
| Source | host-supplied cryptographically strong entropy (noncanonical evidence, as at birth) |
| Encoding | `BASE32_GROUPS_OF_FOUR_V1` — RFC 4648 base32, no padding, upper case, groups of four separated by `-` |
| Checksum | 20 bits, truncated SHA-256 of the root, appended before encoding |
| Encoded length | 55 symbols in 14 groups |
| Decoding | case-insensitive, separator-insensitive |

### Why 256 bits and not a memory-hard KDF

A user-chosen passphrase needs an expensive KDF because it carries perhaps 40
bits of entropy. A generated 256-bit root does not. Making the user wait on an
Argon2 pass over a full-entropy secret buys nothing an attacker cannot already
ignore, and it costs a slow, battery-hungry step in the one flow where the user
is already frightened. The entropy does the work the KDF would otherwise have to.

### Why base32 and not a word list

A word list is easier to read aloud and needs 2,048 curated words *per language*.
Base32 is unambiguous, language-neutral and needs no curation. This is a
presentation decision, and the contract deliberately fixes the **bits** rather
than the rendering — a word-list encoder can be added later without changing
anything cryptographic.

### What the user must be told

Frozen wording, kept in code rather than in a screen so a UI cannot quietly
promise more than the cryptography delivers:

> This phrase unlocks a backup you have already made. It does not contain your
> creature's memories. Without a current encrypted backup, the phrase alone
> cannot bring lost history back.

## Frozen key derivation

Three keys, three purposes, three domain separation strings. HKDF-SHA-256 with
the organism id as salt:

| Derived key | Info string | Used for |
|---|---|---|
| Package key | `DLL17-RECOVERY-PACKAGE-V1` | AEAD over the package contents |
| Manifest MAC key | `DLL17-RECOVERY-MANIFEST-MAC-V1` | HMAC over the plaintext manifest |
| Authority proof key | `DLL17-IDENTITY-AUTHORITY-PROOF-V1` | The activation challenge response |

`Hkdf.derive` refuses an empty info string. Two keys derived from one root for
different purposes must be unrelated, and that string is the only thing making
them so.

---

## The cold package

```
package (schema 241 v1)
├── manifest (schema 242 v1)          ← plaintext
├── manifest MAC (HMAC-SHA-256)
└── ciphertext (ChaCha20-Poly1305)    ← AAD = manifest identity fields
     └── checkpoint bytes + journal tail above the checkpoint
```

### Manifest contents

Organism id, identity epoch, package sequence, checkpoint sequence, last
protected logical time, engine/event/random contract versions, ciphertext length,
ciphertext hash, lineage hash.

Plaintext on purpose: a provider and a destination device must be able to check
identity, epoch, sequence and size **before** anyone has a key, so a stale or
foreign package is refused without a decryption attempt. It carries no organism
content, and its MAC means a provider that rewrites it is detected.

### Verification order

Frozen, and each step refuses a different attack:

1. **manifest MAC** — a rewritten manifest;
2. **ciphertext length and hash** — a substituted body;
3. **AEAD open** — a wrong key, or a manifest and body from different packages.

Binding the manifest's identity fields into the AAD is what stops a provider from
serving package A's ciphertext under package B's manifest.

### Nonce

Derived from `(identityEpoch, packageSequence)`. Never random.

### Failure states

| State | Meaning |
|---|---|
| `RECOVERY_SECRET_MALFORMED` | Wrong length or an invalid symbol |
| `RECOVERY_SECRET_CHECKSUM_FAILED` | Almost certainly a transcription error |
| `RECOVERY_PACKAGE_MALFORMED` | Not a package |
| `RECOVERY_PACKAGE_UNAUTHENTIC` | Wrong secret, or a forged manifest |
| `RECOVERY_PACKAGE_CORRUPT` | Body does not match its declared length or hash |
| `RECOVERY_PACKAGE_STALE` | Identity epoch below the epoch already activated |
| `RECOVERY_PACKAGE_DUPLICATE` | Already-stored package sequence; idempotent |

**Identity may not go backwards.** History may — that is what `RecoveryGapDeclared`
exists for — but a package below the activated epoch is refused, or a superseded
device could reclaim the organism with an old backup.

---

## Reissue and rotation

The recovery root is the user's, and the product cannot rotate it unilaterally.
Reissue is therefore an explicit user action that generates a new root, rewraps
nothing locally (local keys are separate by construction), and invalidates every
previously created package because the package key changes with the root. Old
packages remain decryptable **only** by the old phrase, which is why revocation
at the provider is part of `RecoveryPackageStoreContractV1` rather than optional.

---

## What recovery must never do

**Recovery never reruns old behaviour to reconstruct history.** A package carries
a checkpoint and the journal tail above it; restoring means writing those bytes,
not replaying the organism forward through time it did not live. Where history is
missing, `RecoveryGapDeclared` records the recovered point, the known unavailable
interval and the new epoch — and nothing else. No memories, no relationships, no
transactions are invented for the gap.

## Blocked

| Item | State |
|---|---|
| Word-list encoding of the recovery root | Not frozen; a presentation layer over the same bits |
| Recovery-secret handoff UX | R012 product UX, outside the parallel amendment |
| Provider-side revocation policy for a reissued root | Requires a selected network provider |
