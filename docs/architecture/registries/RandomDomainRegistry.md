# RandomDomainRegistry

- Registry version: `V1`
- Status: `POPULATED_R001`
- Created: R000 (directive D004)
- Owner gate: `DeterminismContractV1` freeze, then R001

Authoritative list of every isolated random domain. Every draw in the organism
belongs to exactly one registered domain so that replay is reproducible and one
subsystem's consumption cannot shift another's stream.

## Required columns

| Column | Meaning |
|---|---|
| Domain ID | Immutable numeric identifier, never reused |
| Algorithm / version | Generator algorithm and its version |
| Persisted state requirements | What must survive process death |
| Derivation contract | How the substream is derived from the root seed |

## Entries

Domain IDs are immutable integers and are never reused. A domain's substream seed
is a pure function of `(masterSeed, contractVersion, domainId)` and of nothing
else, so registering a new domain cannot perturb any existing stream. That is a
structural property of `SUBSTREAM_DERIVE_V1`, not a test result.

| Domain ID | Name | Algorithm / version | Persisted state requirements | Derivation contract |
|---|---|---|---|---|
| 1 | `DOMAIN_QUALIFICATION_PRIMARY` | `PRNG_SPLITMIX64_V1` | `seed` and `counter`, 16 bytes, canonical | `SUBSTREAM_DERIVE_V1(masterSeed, contractVersion, 1)` |
| 2 | `DOMAIN_QUALIFICATION_SECONDARY` | `PRNG_SPLITMIX64_V1` | `seed` and `counter`, 16 bytes, canonical | `SUBSTREAM_DERIVE_V1(masterSeed, contractVersion, 2)` |
| 3 | `DOMAIN_QUALIFICATION_LATE_INSERT` | `PRNG_SPLITMIX64_V1` | `seed` and `counter`, 16 bytes, canonical | `SUBSTREAM_DERIVE_V1(masterSeed, contractVersion, 3)` |

All three are **qualification domains**, not organism domains. R001 draws
randomness only to prove that substreams are isolated, reproducible and
recoverable. Domain 3 exists specifically to be inserted after domains 1 and 2
have been consumed, which is the only way to demonstrate that insertion leaves
prior streams untouched.

No organism domain may be registered until the phase that owns the behaviour
opens. Adding one is additive and safe by construction, but it is still a
canonical-architecture change rather than an implementation detail.

Executable form: `core-crypto`, `RandomDomainRegistry`. The serialized layout is
frozen in `DeterminismContractV1` section 7.3.


---

## R002 additions

None. R002 introduces **no** random domain and draws no randomness anywhere.

That is a deliberate property rather than an omission. Reconciliation, recovery
and migration must all be pure functions of their inputs, and a random draw
inside any of them would make a replayed reconciliation diverge from the original
one. The AEAD nonce that an encrypted record needs is derived from the durable
sequence and key epoch precisely so that the durable write path needs no
randomness source either.
