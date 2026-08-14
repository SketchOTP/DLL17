# R014 network gate — recovery provider and identity-authority transport

- Phase: R012 network substrate
- Directive: D014
- Fixture set: `R014-NETWORK-FIXTURES-V1` version 1
- Evaluated: 2026-08-14

**Gate state: `PASS`.**

| Run | Endpoint | Fixtures | Held | Digest |
|---|---|---|---|---|
| Primary | in-repository qualification endpoint | 38 | **38** | `efbd6f1caa060da228f72a96cef9e3a2a290c7503f685270e2bbb2b7c7da1501` |
| External | MinIO `RELEASE.2025-09-07T16-13-09Z` | 33 | **33** | `cc9497ff4817abd8a94bcade5f97318424174670a2629100baa1b1aa3d192545` |

Both runs are recorded and neither is averaged into the other. See
`qualification/network/R014/ENDPOINT_MATRIX.md` for what each one proves and why
the counts differ.

---

## What this gate closes

Two items that D011 left explicitly open.

| Item | Was | Now |
|---|---|---|
| Network recovery provider | "a later integration that must satisfy this same contract" | `S3_COMPATIBLE_OBJECT_STORE_V1`, conformant, qualified on a third-party endpoint |
| Identity-authority transport | `Blocked` — "a deployment decision" | `IdentityAuthorityTransportContractV1`, frozen |

## Provider conformance

| Requirement | Fixture | Result |
|---|---|---|
| Byte-identical round trip | `FX-NET-ROUNDTRIP-01` | 4096 bytes identical |
| Receipt confirms all five fields | `FX-NET-RECEIPT-01` | checksum, size, sequence, name |
| The server verifies the payload | `FX-NET-INTEGRITY-RECEIPT-01` | `x-amz-checksum-sha256` accepted |
| Idempotent put | `FX-NET-IDEMPOTENT-PUT-01` | one object, stable checksum |
| Conditional replacement | `FX-NET-CONDITIONAL-REPLACE-01` | superseded in place |
| Ordered listing and current point | `FX-NET-LISTING-ORDER-01` | ordered; pagination covered by test |
| Missing object | `FX-NET-NOT-FOUND-01` | `NOT_FOUND` |
| Idempotent delete | `FX-NET-IDEMPOTENT-DELETE-01` | converges |
| Wrong bucket | `FX-NET-WRONG-BUCKET-01` | refused, not created |
| Authentication failure | `FX-NET-AUTH-FAILURE-01` | `REJECTED`, never retried |
| Timeout | `FX-NET-TIMEOUT-01` | `NETWORK_INTERRUPTED` |
| Multipart not reachable | `FX-NET-MULTIPART-NOT-USED-01` | refused before it is sent |
| Transient failure retried | `FX-NET-RETRY-CONVERGES-01` | converges, no duplicate |
| Interrupted upload / failed replacement | `FX-NET-FAILED-REPLACEMENT-01` | last confirmed object intact |
| Rate limit | `FX-NET-RATE-LIMIT-01` | `PROVIDER_UNAVAILABLE` |
| Endpoint outage | `FX-NET-PROVIDER-OUTAGE-UNIFORM-01` | every operation, same outcome |

## Authority transport

| Requirement | Fixture | Result |
|---|---|---|
| Registration unchanged | `FX-NET-AUTH-REGISTER-01` | epoch 1 |
| Activation advances exactly once | `FX-NET-AUTH-ACTIVATE-01` | epoch 2 |
| Duplicate request consumes no epoch | `FX-NET-AUTH-DUPLICATE-ACTIVATE-01` | `ALREADY_GRANTED` |
| Replayed nonce refused | `FX-NET-AUTH-REPLAY-01` | `CHALLENGE_INVALID` |
| Racing destinations | `FX-NET-AUTH-RACE-01` | 2 threads, 2 sockets, **1 winner** |
| Superseded device informed | `FX-NET-AUTH-SUPERSEDED-01` | `SUPERSEDED` |
| Server clock, not the caller's | `FX-NET-AUTH-STALE-01` | `CHALLENGE_INVALID` |
| Free text stays off the wire | `FX-NET-AUTH-NO-DETAIL-01` | not carried |
| Restart preserves the granted epoch | `FX-NET-AUTH-RESTART-01` | 3 → 3 |

Malformed bodies, wrong-endpoint bodies, oversized bodies, wrong media type,
wrong method, missing request id and durable-state failure are covered by
`IdentityAuthorityTransportTest` and all refuse without a decodable body.

## End to end

`FX-NET-E2E-UPLOAD-01` → `-RETRIEVE-01` → `-COLD-RECOVERY-01` →
`-RESTORED-READABLE-01`: an encrypted package uploaded to the network provider,
retrieved byte-identically, verified, the epoch advanced through the network
authority, and a cold device restored with 20 records and its checkpoint. Held on
both endpoints.

## Outage isolation

| Requirement | Fixture | Result |
|---|---|---|
| Provider outage does not block ordinary local commits | `FX-NET-PROVIDER-OUTAGE-LOCAL-LIFE-01` | 25 commits, 25 read back |
| Authority outage does not corrupt local state | `FX-NET-AUTHORITY-OUTAGE-LOCAL-STATE-01` | `TRANSPORT_UNAVAILABLE`, 25 records intact |

Both use a genuinely unreachable socket, refused by the operating system, and
both held against the external endpoint too.

## Privacy

| Requirement | Fixture | Result |
|---|---|---|
| Provider holds only ciphertext | `FX-NET-PRIVACY-PAYLOAD-01` | plaintext canary absent |
| Only contract-required metadata crosses | `FX-NET-PRIVACY-METADATA-01` | 59 requests, no plaintext |
| Authority store holds no organism content | `FX-NET-PRIVACY-AUTHORITY-01` | absent after a full exchange |
| Access log is structured only | `FX-NET-PRIVACY-LOG-01` | shape asserted |

## Determinism

| Requirement | Fixture | Result |
|---|---|---|
| Restored canonical hash equals the source's | `FX-NET-DETERMINISM-ROUNDTRIP-01` | equal |
| Independent of object key, request id, server timing | `FX-NET-DETERMINISM-PROVIDER-INDEPENDENT-01` | equal across two different keys |
| No provider vocabulary in a canonical hash input | `FX-NET-DETERMINISM-NO-PROVIDER-VOCABULARY-01` | absent |

Canonical hashing has never been downstream of transport, which is why a network
provider moves nothing. R001 `54bc0447…`, R002 `556bbe49…` and A000 `9462e436…`
are unchanged.

---

## Not closed by this gate

| Item | State |
|---|---|
| Production hosting of the authority | **Not deployed.** Procedures exist; nothing has run outside a developer machine |
| High availability, SLA, geographic redundancy, disaster recovery | **Not claimed.** Single instance by design |
| TLS | **Not exercised.** Both runs are loopback plaintext HTTP |
| Any commercial object store | `NOT RUN` |
| Load, capacity, latency, cost | `NOT MEASURED`. No threshold derived |
| Provider selection for the product | `BLOCKED_SPEC_RECOVERY_PROVIDER_SELECTION` |
| Anything on physical Android hardware | `BLOCKED_DEVICE_UNAVAILABLE`, unchanged by D014 |
