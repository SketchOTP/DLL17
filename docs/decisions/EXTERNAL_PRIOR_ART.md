# External prior-art record

Checks of the external landscape performed before resolving a design question,
with an explicit disposition for each. A finding recorded here has **not** been
adopted unless its disposition says so.

| Disposition | Meaning |
|---|---|
| `REFERENCE` | The prior art informed our own design. No dependency, no code, no vendored spec |
| `ADOPT` | A named external contract or dependency was taken on, with its version pinned |
| `REJECT` | Considered and not used, with the reason recorded |
| `WRAP` | An external dependency taken on, but reached only through a project-owned interface |
| `BUILD` | Implemented in this project after finding the candidates materially unsuitable |

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

---

## PA-0002 — S3-compatible object storage clients for the JVM

- Recorded: 2026-08-14
- Directive: D014
- Disposition: **`BUILD`**
- Informs: `S3RecoveryPackageStore`, `S3Signing`, decision `IMPL-0077`

### The question

`RecoveryPackageStoreContractV1` was frozen under D011 with a filesystem
provider and an explicit note that a network provider is "a later integration
that must satisfy this same contract". D014 required that integration, and
required checking the external landscape before writing a line of it.

### Candidates

| Project | Overlap | Licence | Maintenance | Integration cost | Disposition |
|---|---|---|---|---|---|
| AWS SDK for Java v2, `software.amazon.awssdk:s3` | Complete. The reference implementation of the API | Apache-2.0 | Active, first-party | High | `REJECT` |
| MinIO Java client, `io.minio:minio` | Complete, and explicitly vendor-neutral across S3-compatible stores | Apache-2.0 | Active | High | `REJECT` |
| Apache jclouds `blobstore` | Broader than needed; abstracts many clouds | Apache-2.0 | Low activity | High | `REJECT` |
| A maintained HTTP client plus our own signing | Partial — supplies transport only | varies | Active | Medium | `REJECT` |
| Implement the four operations directly | Exact | — | ours | Low | **`BUILD`** |

### Why the two serious candidates are materially unsuitable

Both are good libraries, and on a server either would be the obvious answer.
The deciding fact is *where this code has to run*: the destination device, during
a cold recovery, is the case the whole provider exists for.

- **Android is not a supported target for the AWS SDK v2.** AWS states it has no
  plans to expand Android support in the v2 Java SDK; the v1-lineage Android SDK
  it points to instead reached end of life. Building the organism's recovery path
  on a client whose vendor does not support the platform is not a dependency, it
  is a deferred outage.
- **Both bring a second cryptographic provider onto the device.** The MinIO
  client resolves BouncyCastle, Guava, Jackson, OkHttp, commons-compress and
  Snappy. `IMPL-0008` and `IMPL-0018` made SHA-256, HMAC, HKDF and
  ChaCha20-Poly1305 project-owned precisely because *which implementation runs* is
  not something a standard pins down, and a key a device can derive today and
  cannot derive after an update is unrecoverable data loss. That argument does not
  weaken because the bytes in question are a backup.
- **The frozen dependency policy is exact pins.** Either candidate adds tens of
  transitive artifacts to `gradle/libs.versions.toml`, each of which becomes a
  version this project is on the hook for.

### Why `BUILD` is not the usual bad idea

The scope actually needed is four operations — `PUT`, `GET`, `DELETE` and
`ListObjectsV2` — plus SigV4, which is HMAC-SHA-256 over a canonical request
using primitives this project already owns and has already qualified. There is no
credential provider chain, no region resolution, no service model, no XML data
binding and no retry framework: those are the parts of an SDK that are large, and
none of them is required here. Transport is `HttpURLConnection`, which exists on
both the JVM and Android, unlike `java.net.http`.

The two checks that keep this honest are both enforced rather than asserted:

- `AndroidApiSurfaceTest` reads the module's compiled classes back and fails if
  anything reaches outside the Android API 29 surface — so the justification for
  refusing the SDK cannot quietly stop being true.
- The qualification runs against **MinIO**, an independent third-party
  implementation of AWS SigV4. It authenticated every request. That is what
  distinguishes a compatible signer from a self-consistent one, and no test
  written inside this repository could establish it.

### What was given up

Multipart upload, presigned URLs, streaming payload signing, server-side copy,
bucket lifecycle management and every other S3 feature. None is reachable from
the frozen provider contract. A recovery package above the single-request ceiling
is refused before it is sent (`FX-NET-MULTIPART-NOT-USED-01`) rather than
silently taking a code path that does not exist.

**No dependency was adopted. `gradle/libs.versions.toml` is unchanged by D014.**

---

## PA-0003 — JVM HTTP frameworks for the identity-authority transport

- Recorded: 2026-08-14
- Directive: D014
- Disposition: **`BUILD`**
- Informs: `IdentityAuthorityHttpServer`, `IdentityAuthorityTransportContractV1`,
  decision `IMPL-0078`

### The question

`IdentityAuthorityProtocolV1` was frozen under D011 with transport listed as
`Blocked` — "not frozen; a deployment decision, and correctness is provable
without one". D014 required the transport.

### Candidates

| Project | Overlap | Licence | Maintenance | Integration cost | Disposition |
|---|---|---|---|---|---|
| Ktor server | Complete, Kotlin-native, coroutine-based | Apache-2.0 | Active, JetBrains | Medium | `REJECT` |
| Javalin | Complete, small, Jetty underneath | Apache-2.0 | Active | Medium | `REJECT` |
| http4k | Complete, functional, server-agnostic | Apache-2.0 | Active | Medium | `REJECT` |
| Spring Boot | Far broader than needed | Apache-2.0 | Active | High | `REJECT` |
| `com.sun.net.httpserver` (`jdk.httpserver`) | Exact | JDK | Ships with the pinned runtime | Low | **`BUILD`** |
| Cloudflare Durable Objects (single-owner stateful pattern) | Architectural only | — | — | — | `REFERENCE` |

### Why the JDK server

Unlike PA-0002, no candidate here is unsuitable. Ktor or Javalin would work, and
the honest statement of this decision is that a capable option was declined
rather than ruled out.

What decided it is the shape of the service. It has four endpoints, one bounded
binary body, no routing beyond a path match, no templating, no serialization
layer, no dependency injection and no session state — the request bodies are
canonical bytes the project already encodes, and the largest legal one is under
128 bytes. A framework would contribute a dependency tree to keep current on a
service whose entire job is to be trustworthy about *one number*, and every
artifact in that tree is code running inside the process that decides which
device owns an organism.

The costs are real and are written down rather than hidden: no HTTP/2, no TLS
termination worth using in-process, and connection handling that assumes a
reverse proxy in front. `services/identity-authority/operations/OPERATIONS.md`
states the reverse proxy as a requirement.

### The `REFERENCE` finding

Cloudflare Durable Objects were checked as prior art for the *pattern* rather
than as a candidate dependency: a single-owner stateful object serializing
mutations to one key is the same shape as an epoch compare-and-swap, and its
existence confirms that serializing every mutation for one organism is the
conventional answer rather than a local shortcut. Recorded as `REFERENCE`. No
vendor was adopted, and the service remains deployable anywhere a JVM runs.

**No dependency was adopted. `gradle/libs.versions.toml` is unchanged by D014.**
