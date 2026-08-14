# RecoveryPackageStoreContractV1

- Status: `FROZEN`
- Version: 1
- Frozen under: D011
- Executable portion: `core-recovery/.../RecoveryPackageStore.kt`
- Qualifying providers: `FILESYSTEM_OBJECT_STORE_V1`,
  `S3_COMPATIBLE_OBJECT_STORE_V1` (added under D014)
- Conformance suites: `RecoveryPackageStoreConformanceTest`,
  `S3ProviderNetworkConformanceTest`, `R014NetworkQualificationKernel`

Provider-neutral cold-package storage. Deliberately small: a provider stores
bytes and confirms what it stored.

---

## What a provider is never allowed to be

Canonical authority for anything beyond the bytes and the sequence of a package.
It cannot say what the organism is, when it lived, or whether a package is
current — the manifest says that, and the manifest is authenticated with a key
the provider does not have.

**Ordinary local organism operation must not depend on any of this.** Every
operation may fail, and every failure is an ordinary outcome that leaves the
local organism untouched. Asserted by `FX-RECOVERY-PROVIDER-OUTAGE-01`: with the
provider refusing every call, local commits still succeed and still read back.

## Frozen interface

| Operation | Semantics |
|---|---|
| `put` | Idempotent on `(organismId, packageSequence)`. A retry after an ambiguous failure must be safe, because the caller genuinely cannot tell whether the first attempt landed. |
| `get` | Byte-identical retrieval, or `NOT_FOUND`. |
| `list` | Objects for one organism, ordered by sequence. Another organism's objects are invisible. |
| `currentPoint` | Highest stored sequence, or null for providers that cannot list. |
| `delete` | Idempotent, so a retried revocation converges rather than failing. |

### Object naming

`dll17-<organismId:016x>-<packageSequence:012d>.pkg` — fixed, because a
destination device has to find a package cold, with no local catalogue.

### Receipt

Receipt identifier, object name, object size, checksum, package sequence. All
five, because `RecoveryPoint.receiptConfirms` requires all five to match before
freshness advances — any one of them alone can be satisfied by a provider that
stored something else.

### Outcomes

`OK`, `NETWORK_INTERRUPTED`, `PROVIDER_UNAVAILABLE`, `PARTIAL_WRITE`,
`NOT_FOUND`, `REJECTED`, `CANCELLED`. Never conflated with corruption: a provider
failure and a damaged package mean different things and lead to different user
outcomes.

---

## The qualifying provider

`FILESYSTEM_OBJECT_STORE_V1` — a directory-backed object store.

It is a real provider, not a mock: it writes real bytes through a staging file
and an atomic rename, it survives process death, and it is what the end-to-end
cold-recovery qualification actually runs against.

What it is not is a **network** provider. A user-chosen cloud backend is a later
integration that must satisfy this same contract and pass the same conformance
suite, and selecting one is a product decision requiring credentials and an
owner. The conformance suite is written against the interface rather than the
implementation precisely so that "we tested the filesystem one" cannot be an
argument for skipping it.

### Failure injection

The provider carries explicit switches for outage, partial write and injected
failure. Every interesting property of a recovery provider is a statement about a
failure, and a failure that needs a real network outage to reproduce is a
property nobody re-tests.

---

---

## The network provider, added under D014

The paragraph above anticipated this and set the terms: a network provider "must
satisfy this same contract and pass the same conformance suite". It does, and the
contract itself is **unchanged** — no version bump, no new operation, no new
outcome. A second implementation of a frozen interface is not an amendment to it.

`S3_COMPATIBLE_OBJECT_STORE_V1` implements the five operations over the
S3-compatible object API. Vendor neutrality is structural rather than declared:
there is no vendor name in the implementation and no per-vendor branch, and
everything that differs between providers is a configuration value. See
`docs/operations/RECOVERY_PROVIDER_CONFIGURATION.md`.

Two things the network provider must do that the filesystem one did not have to:

- **Make the far end verify the payload.** Every upload carries
  `x-amz-checksum-sha256`, so the server refuses a corrupted object and a `200`
  is an integrity receipt rather than a promise. The returned ETag is recorded
  for the operator and deliberately not trusted, because ETag stops being a
  content hash the moment an object is multipart-uploaded or encrypted with a
  managed key.
- **Keep the receipt provider-independent.** The receipt is derived from the
  bytes, not from the provider, so `RecoveryPoint.receiptConfirms` means the same
  thing for both. A provider-derived identifier would make freshness depend on
  which backend the user happened to choose. Asserted by
  `S3ProviderNetworkConformanceTest`, which compares both providers' receipts for
  the same payload.

Multipart upload is not implemented, and a package above 64 MiB is refused before
it is sent rather than silently taking a path that does not exist.

Qualified against the in-repository endpoint (38 fixtures) and against MinIO
`RELEASE.2025-09-07T16-13-09Z` (33 fixtures). See
`qualification/network/R014/ENDPOINT_MATRIX.md`.

---

## Blocked

| Item | State |
|---|---|
| Production network provider **selection** | `BLOCKED_SPEC_RECOVERY_PROVIDER_SELECTION` — D014 built the mechanism; choosing and paying for an endpoint remains a product decision needing an owner, credentials and a privacy review |
| Multipart upload | Not implemented, not needed, and refused rather than approximated |
| Any commercial endpoint | `NOT RUN` — deploying billable cloud resources needs separate authorization |
| Provider-side retention and revocation policy | Follows the selected provider |
| Upload scheduling and background attempt policy | R012 product UX, outside the parallel amendment |
