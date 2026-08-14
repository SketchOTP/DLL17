# External prior-art record

Checks of the external landscape performed before resolving a design question,
with an explicit disposition for each. A finding recorded here has **not** been
adopted unless its disposition says so.

| Disposition | Meaning |
|---|---|
| `REFERENCE` | The prior art informed our own design. No dependency, no code, no vendored spec |
| `ADOPT` | A named external contract or dependency was taken on, with its version pinned |
| `REJECT` | Considered and not used, with the reason recorded |

---

## PA-0001 — Envelope encryption: separating data keys from wrapping keys

- Recorded: 2026-08-14
- Directive: D013
- Disposition: **`REFERENCE`**
- Informs: `LocalStorageCryptographyContractV2`, decision `IMPL-0072`

### The question

D012 found that a rotation of the device wrapping material made every existing
journal record unreadable, because one number served as both the wrapping epoch
and the record's key context. Before designing a correction, the question was
whether the established answer to "rotate the key that protects the key" is
already settled outside this project, and whether our instinct — separate the two
— is the ordinary one or an idiosyncratic one.

### Sources considered

| Source | What it establishes |
|---|---|
| Google Tink, envelope-encryption documentation | The DEK/KEK split is the standard structure: a data-encryption key encrypts payloads, a separate key-encryption key protects the DEK |
| Google Cloud KMS, CMEK key-rotation documentation | Rotation patterns in which DEKs are rewrapped, or an older KEK version stays usable, **without rewriting the underlying application data** |
| RFC 8439 (already adopted for the AEAD itself) | Nothing new here; confirms nonce/AAD discipline is orthogonal to which key wraps which |

### Problem overlap

High on the structure, partial on the mechanics.

The overlap is the core insight: a record's readability must depend on the key
that sealed it, not on the current state of the material protecting that key.
That is exactly the separation V2 introduces, and it tells us our correction is
the conventional one rather than a local invention.

Where the overlap stops: KMS systems are network services with key rings,
multiple live key versions and server-side re-encryption jobs. This organism has
one device, one data key, no network in the local path, and a hard requirement
that ordinary operation never contacts a service. Their retention and migration
machinery answers a question we do not have.

### Why `REFERENCE` and not `ADOPT`

Adopting Tink or a KMS client would mean taking a runtime dependency and
provider resolution into the one code path where neither is acceptable. The
reasoning is unchanged from `IMPL-0008` and `IMPL-0018`, which is why SHA-256,
ChaCha20-Poly1305, HKDF and HMAC are project-owned: the algorithms are
standardized, but *which implementation runs* is not, and a key a device can
derive today and cannot derive after an OS update is unrecoverable data loss.
That argument applies with more force to the key hierarchy than to any single
primitive.

The existing ChaCha20-Poly1305 and HKDF implementation is already qualified and
is unchanged by D013. The correction is a change to which value feeds a nonce and
an AAD — a few lines — and it needs no library.

**No new dependency was adopted. `gradle/libs.versions.toml` is unchanged by
D013.**
