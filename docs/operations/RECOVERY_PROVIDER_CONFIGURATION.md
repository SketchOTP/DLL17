# Network recovery provider — configuration and secret ownership

- Status: `BASELINE`
- Written under: D014
- Provider: `S3_COMPATIBLE_OBJECT_STORE_V1`
- Contract: `RecoveryPackageStoreContractV1`, unchanged

The recovery provider is not a service this project deploys. It is a bucket
somebody else runs, that this organism writes encrypted bytes to. That asymmetry
shapes everything below.

---

## Configuration schema

| Field | Required | Meaning |
|---|---|---|
| `endpoint` | yes | Absolute `https://` URL of the S3-compatible API |
| `region` | yes | Signing region. `us-east-1` where a provider has no regions |
| `bucket` | yes | Bucket name |
| `keyPrefix` | no | Key prefix, for sharing one bucket between installations |
| `pathStyle` | no | `true` (default) addresses `endpoint/bucket/key` |
| `connectTimeoutMillis` | no | 10 000 |
| `readTimeoutMillis` | no | 30 000 |
| `maxAttempts` | no | 3, including the first |

`pathStyle` is the only compatibility switch and it is an addressing choice, not
a vendor flag: virtual-host addressing needs a wildcard DNS name under the
endpoint, which self-hosted endpoints usually do not have.

**There is no vendor field, and there must never be one.** AWS S3, Cloudflare R2,
Backblaze B2, MinIO, Ceph RGW and Garage are all reached by changing `endpoint`
and `region`. A vendor branch in the provider would be the first step to a
provider that only works with one.

## Credentials

Supplied externally, always. There is no default, no embedded value, and no file
the provider reads on its own.

| Variable | Required |
|---|---|
| `DLL17_RECOVERY_S3_ACCESS_KEY_ID` | yes |
| `DLL17_RECOVERY_S3_SECRET_ACCESS_KEY` | yes |
| `DLL17_RECOVERY_S3_SESSION_TOKEN` | only for temporary credentials |

`S3Credentials` keeps the secret behind an internal accessor and renders it as
`REDACTED`, so a logged configuration, a crash dump or an exception message
cannot carry it. `S3ObjectStoreConfig.toString()` never reaches for it.

### Least privilege

The credential needs exactly four actions, scoped to the bucket and the key
prefix: `PutObject`, `GetObject`, `DeleteObject` and `ListBucket`. Nothing needs
bucket creation, policy management, lifecycle configuration or access to any
other prefix.

### Rotation

Rotating the object-store credential is safe and needs no coordination with the
organism: replace the environment values and restart. No stored object depends on
which credential wrote it, because the package is encrypted with a key the
provider never has.

This is *not* true of the recovery root, which is a different secret with no
rotation procedure. See `RecoveryCryptographyContractV1`.

## What the provider is allowed to see

| Item | Crosses the boundary |
|---|---|
| Encrypted package bytes | yes |
| Object name `dll17-<organismId>-<sequence>.pkg` | yes, by design — a cold device must find it |
| Object size and upload time | yes, unavoidably |
| SHA-256 of the ciphertext, as `x-amz-checksum-sha256` | yes |
| Canonical plaintext, physiology, memories, journals | **no** — `FX-NET-PRIVACY-PAYLOAD-01` |
| The recovery root, the package key, any local key | **no** — the provider could not decrypt a package if it tried |

An observer with full access to the bucket learns that an organism with a given
id exists and has backups at given sequences and times. That is the cost of cold
recovery being possible at all, and it is the whole cost.

### Transport

`https` in production. The provider exposes `plaintextTransport` so a caller can
refuse an `http://` endpoint; both qualification runs used loopback plaintext,
which is acceptable for a local endpoint and for nothing else.

## Provider-side policy the operator owns

| Item | Owner |
|---|---|
| Versioning and object lock | the operator; the provider contract does not require either |
| Lifecycle expiry | the operator — **expiring the newest object destroys the only cold-recovery path** |
| Retention and legal hold | the operator |
| Server-side encryption | optional; the payload is already encrypted, so this is defence in depth |
| Cross-region replication | the operator |
| Bucket policy and public-access blocking | the operator; the bucket must not be public |

## Not qualified

| Claim | State |
|---|---|
| Any commercial endpoint | `NOT RUN` — D014 excludes deploying billable cloud resources without separate authorization |
| TLS against a real certificate chain | `NOT RUN` |
| Multipart upload | **Not implemented.** A package over 64 MiB is refused before it is sent |
| Throughput, cost or latency at production package sizes | `NOT MEASURED`; no threshold is derived |
| Upload scheduling and background retry policy | Not designed. R012 product UX, still outside this work |
| Provider selection for the product | Still `BLOCKED_SPEC_RECOVERY_PROVIDER_SELECTION` — D014 built the mechanism, not the product decision |
| The provider running on Android | `BLOCKED_DEVICE_UNAVAILABLE`. `AndroidApiSurfaceTest` enforces the API surface; nothing has run on a device |
