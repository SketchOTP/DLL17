# R014 network endpoint matrix

Where the R014 fixture set has actually been run, and what each run proves.
The two runs are recorded separately and are **not** averaged: one shows the
behaviour under faults we can inject, the other shows the protocol is real.

| Endpoint | Kind | Fixtures | Held | Digest | Evidence |
|---|---|---|---|---|---|
| In-repository qualification endpoint | loopback HTTP, ours | 38 | 38 | `efbd6f1caa060da228f72a96cef9e3a2a290c7503f685270e2bbb2b7c7da1501` | `R014_REPORT.txt` |
| MinIO `RELEASE.2025-09-07T16-13-09Z` | third-party S3 implementation | 33 | 33 | `cc9497ff4817abd8a94bcade5f97318424174670a2629100baa1b1aa3d192545` | `R014_EXTERNAL_ENDPOINT_REPORT.txt` |
| AWS S3, Cloudflare R2, Backblaze B2 | commercial | — | — | — | `NOT RUN` |

## The external run

- Image: `quay.io/minio/minio:latest`, digest
  `sha256:14cea493d9a34af32f524e538b8346cf79f3321eff8e708c1e2960462bd8936e`
- Version: `RELEASE.2025-09-07T16-13-09Z`, commit `07c3a429bfed433e49018cb0f78a52145d4bedeb`
- Endpoint: `http://127.0.0.1:19000`, bucket `dll17-recovery`, region `us-east-1`
- Addressing: path-style
- Credentials: supplied through the documented environment variables, generated
  for this run, never committed

### What it proves that the in-repository endpoint cannot

MinIO implements AWS Signature Version 4 independently of this project. It
authenticated every request, verified every `x-amz-checksum-sha256`, and answered
every listing. That is the evidence that the signer in `S3Signing` is
AWS-compatible rather than merely self-consistent — a property no test written
inside this repository could establish about itself.

It also carried the whole end-to-end flow: an encrypted package uploaded,
retrieved byte-identically, verified, an epoch advanced through the authority
transport, and a cold device restored with a canonical state hash identical to
the source's.

### Why 33 fixtures rather than 38

Five fixtures need a *fault* injected into the endpoint — a forced 5xx, a rate
limit, a full outage, and the request-line inspection behind the metadata privacy
check. MinIO cannot be made to fail on command, so those five do not run against
it. They are qualified against the in-repository endpoint, which exists for
exactly this reason.

Two properties do carry across both runs and did hold in both:
`FX-NET-PROVIDER-OUTAGE-LOCAL-LIFE-01` and
`FX-NET-AUTHORITY-OUTAGE-LOCAL-STATE-01` use a genuinely unreachable socket,
refused by the operating system rather than simulated.

## Not qualified

| Item | State |
|---|---|
| AWS S3, Cloudflare R2, Backblaze B2 or any commercial endpoint | `NOT RUN` — D014 excludes deploying billable cloud resources without separate authorization |
| TLS to a real certificate chain | `NOT RUN` — both runs are loopback plaintext HTTP; the code path uses `javax.net.ssl` through `HttpURLConnection` and is unexercised |
| Throughput, latency or cost at production package sizes | `NOT MEASURED` — no threshold is derived and none may be until a real endpoint and a real package-size distribution exist |
| The provider on Android | `BLOCKED_DEVICE_UNAVAILABLE` — the API surface is enforced by `AndroidApiSurfaceTest`, but nothing here ran on a device |
