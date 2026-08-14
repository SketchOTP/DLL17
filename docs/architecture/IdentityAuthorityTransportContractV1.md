# IdentityAuthorityTransportContractV1

- Status: `FROZEN`
- Version: 1
- Frozen under: D014
- Carries: `IdentityAuthorityProtocolV1`, unchanged
- Executable portion: `core-recovery/.../IdentityAuthorityTransport.kt`,
  `core-recovery-net/.../HttpIdentityAuthorityClient.kt`,
  `services/identity-authority/.../IdentityAuthorityHttpServer.kt`
- Qualification evidence: `qualification/network/R014/R014_REPORT.txt`,
  sections `AUTHORITY_TRANSPORT` and `PRIVACY`

The network surface of the identity authority. `IdentityAuthorityProtocolV1`
listed transport as `Blocked` — "a deployment decision, and correctness is
provable without one". This unblocks it and changes nothing the protocol decided.

---

## The transport is not the protocol

Epochs, challenges, leases, replay protection, rate limiting, idempotency and
activation outcomes belong to `IdentityAuthorityProtocolV1`. This contract
**carries** them and has no vocabulary for any of them. A transport that could
change one would be a second definition of the same rule, and two definitions of
one rule eventually disagree about who owns an organism.

The consequence is checked rather than promised: every protocol property D011
qualified in-process was re-qualified over HTTP and produced the same outcome —
`FX-NET-AUTH-ACTIVATE-01`, `-DUPLICATE-ACTIVATE-01`, `-REPLAY-01`, `-RACE-01`,
`-SUPERSEDED-01`, `-STALE-01`, `-RESTART-01`.

## Endpoints

| Method | Path | Body schema | Purpose |
|---|---|---|---|
| `POST` | `/v1/register` | `251` v1 | Birth-time registration |
| `POST` | `/v1/challenge` | `252` v1 | Single-use nonce |
| `POST` | `/v1/activate` | `253` v1 | Epoch compare-and-swap |
| `POST` | `/v1/heartbeat` | `254` v1 | How a superseded device learns |
| `GET` | `/healthz` | — | Liveness |
| `GET` | `/readyz` | — | Readiness |

The schema id is taken from the **path**, not from the body, so a body claiming
to be an activation cannot be delivered to the heartbeat handler.

## Wire format

Canonical envelope bytes, media type `application/vnd.dll17.authority.v1`.

Not JSON, and the reason is not taste. The project already owns a canonical codec
with a frozen byte layout and `AuthorityResponse` already encodes to it, so the
wire format needs no dependency, no parser to harden and no argument about how a
field renders. It also fixes the message size, which is what makes the body
ceiling a defence rather than a guess.

```
register  : i64 organismId ‖ bytes verificationKey ‖ i64 deviceFingerprint
challenge : i64 organismId
activate  : i64 organismId ‖ i32 requestedEpoch ‖ bytes nonce ‖ bytes proof ‖ i64 deviceFingerprint
heartbeat : i64 organismId ‖ i32 claimedEpoch ‖ i64 deviceFingerprint
response  : AuthorityResponse.canonicalBytes()   — schema 253 v1
```

| Parameter | Value |
|---|---|
| Maximum request body | 1024 bytes |
| Request id header | `X-DLL17-Request-Id`, 32 lowercase hex characters, required |
| Trailing bytes after a decoded message | refused |
| Unknown schema version | refused, never guessed |

The largest legal request is an `activate` at well under 128 bytes. A ceiling an
order of magnitude above that means an attacker cannot make the service allocate
on their behalf.

## Two rules that are security properties, not conveniences

### The clock is the server's

`IdentityAuthorityClient` takes a `nowMillis` because an in-process caller must
supply one. **No request encoding carries it.** A client that could name the
current time over the network could expire nothing, outlive every lease and step
around the rate limiter. Qualified by `FX-NET-AUTH-STALE-01` and by the transport
test that hands the client a time far in the future and watches the server ignore
it.

### The body is authoritative, the status is advisory

Every protocol outcome returns a decodable canonical body. The HTTP status
mirrors it so an operator's proxy, dashboard and log aggregator can tell a
granted activation from a refused one without decoding anything.

| Outcome | Status |
|---|---|
| `REGISTERED`, `CHALLENGE_ISSUED`, `ACTIVATION_GRANTED`, `ALREADY_GRANTED` | `200` |
| `EPOCH_CONFLICT`, `SUPERSEDED` | `409` |
| `PROOF_REJECTED` | `403` |
| `CHALLENGE_INVALID` | `400` |
| `RATE_LIMITED` | `429` |
| `UNKNOWN_ORGANISM` | `404` |

A client that trusted the status over the body would be trusting whatever the
last proxy in the chain decided to rewrite.

## Transport failures are not protocol outcomes

| Failure | Status |
|---|---|
| `MALFORMED`, `UNSUPPORTED_VERSION`, `MISSING_REQUEST_ID` | `400` |
| `NOT_FOUND` | `404` |
| `METHOD_NOT_ALLOWED` | `405` |
| `TOO_LARGE` | `413` |
| `UNSUPPORTED_MEDIA_TYPE` | `415` |
| `DURABLE_STATE_UNAVAILABLE`, `TRANSPORT_UNAVAILABLE` | `503` |
| `TIMED_OUT` | `504` |

A transport refusal carries **no body**, so it is not decodable as a protocol
response and a client can never mistake one for the other. "The service could not
be reached" and "the service refused you" lead to different user outcomes and
different operator actions; `ColdRecoveryActivation` already distinguishes
`AUTHORITY_UNAVAILABLE` from `AUTHORITY_REFUSED`, and this contract keeps that
distinction reachable.

## Concurrency

Every protocol call runs under one lock. The authority's whole purpose is an
atomic compare-and-swap on an epoch, and the cheapest correct way to keep that
atomic under concurrent requests is to have no concurrency inside it. A handful
of requests per organism per *lifetime* makes the throughput cost worthless and
the reasoning saved considerable. Qualified by `FX-NET-AUTH-RACE-01`: two
destinations, two threads, two sockets, exactly one winner.

## Durable-state failure

`/readyz` fails and every protocol endpoint answers `503` when the store
directory is not writable. This guards the dangerous failure specifically: an
authority that grants an activation it cannot persist has told two devices they
hold the same organism.

`/healthz` deliberately does **not** depend on durable state. A liveness check
that fails when the disk fails removes the process from rotation exactly when an
operator needs to reach it to find out why.

## Privacy

`AuthorityResponse.detail` is not carried. It is free text the service writes for
its own diagnostics, and free text is the one field shape that could smuggle
something across a boundary this contract exists to keep narrow. Qualified by
`FX-NET-AUTH-NO-DETAIL-01`.

The access log holds a path, an organism id, an outcome name and a request id,
and its shape is asserted by `FX-NET-PRIVACY-LOG-01`. The authority's durable
state is unchanged from `IdentityAuthorityProtocolV1` and still has no field an
organism could fit in; `FX-NET-PRIVACY-AUTHORITY-01` reads the store file back
after a full exchange and looks.

## Blocked

| Item | State |
|---|---|
| TLS termination | Not in-process. A reverse proxy is required; see `services/identity-authority/operations/OPERATIONS.md` |
| Authentication of the *caller* beyond the activation proof | Not frozen. The proof is the credential; there is no account model and none is designed |
| HTTP/2, connection reuse, keep-alive tuning | Not implemented; the JDK server does not offer them |
| Production rate-limit and timeout tuning | The frozen values are protocol defaults, not measured production limits |
| Multi-instance deployment | **Not supported.** The lock is per process and the store is one file. Running two instances against one store would break the compare-and-swap |
