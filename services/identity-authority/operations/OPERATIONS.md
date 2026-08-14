# Identity authority — operations baseline

- Status: `BASELINE`
- Written under: D014
- Service: `IdentityAuthorityServiceV1` behind `IdentityAuthorityHttpServerV1`
- Contract: `IdentityAuthorityTransportContractV1`

The minimum an operator needs to run this service on purpose rather than by
accident. Read the section headed **Not production-qualified** before deploying
anything: several things this document describes are procedures, not proven
capabilities, and the difference matters.

---

## What this service is

One process that answers four endpoints and owns one number per organism: the
identity epoch. It decides which device is the current holder of an organism
after a recovery. It holds no organism content and could not decrypt any if it
did.

If it is down, **new recovery activation is unavailable and nothing else is
affected.** Already-activated organisms carry on with no dependency on it. That
is the property that sets the whole operational posture: this is not a service
whose availability an organism's life depends on, and it must never become one.

## Configuration

Read from the environment and nowhere else. There is no configuration file the
process discovers on its own, so a misconfigured deployment fails to start rather
than quietly coming up pointing at the wrong store.

| Variable | Required | Meaning |
|---|---|---|
| `DLL17_AUTHORITY_STORE` | yes | Directory holding `authority.dll17` |
| `DLL17_AUTHORITY_PORT` | yes | TCP port to bind on `127.0.0.1` |

The server binds loopback only. Exposure is the reverse proxy's job, which is
also where TLS terminates.

### Reverse proxy: required, not recommended

`com.sun.net.httpserver` has no TLS worth using in production, no HTTP/2 and no
request-rate protection. A proxy in front must supply:

- TLS termination with a current certificate
- a request body limit at or below 1024 bytes
- a connection and request rate limit
- a request timeout
- `X-Forwarded-*` handling if any of it is logged

## Secrets

**This service holds no secret of its own.** There is no API key, no signing key
and no database password, because there is no database and the caller's
credential is the activation proof — a MAC the caller computes from a recovery
root the service never sees.

What it holds is a per-organism *verification key*, supplied at registration. It
is not a secret in the usual sense: possessing it does not let anyone activate
anything, because activation needs a MAC over a server-issued nonce.

| Item | Owner | Rotation |
|---|---|---|
| TLS certificate | the proxy operator | per the certificate authority's lifetime |
| Verification keys | the organism, at registration | see below |

### Key rotation

There is no rotation procedure for a verification key, and this is a genuine gap
rather than an omission. Rotating one would mean letting a caller replace the
credential that decides organism ownership, which is exactly the operation the
`register` conflict rule exists to prevent. Designing it needs a directive.

Recovery root rotation is likewise not designed; see `RecoveryCryptographyContractV1`.

## Health and readiness

| Endpoint | Meaning | Depends on durable state |
|---|---|---|
| `GET /healthz` | the process is running | **no** |
| `GET /readyz` | an epoch advance could be persisted right now | yes |

Point the load balancer at `/readyz` and the process supervisor at `/healthz`.
The split is deliberate: a liveness check that fails when the disk fails removes
the process from rotation exactly when an operator needs it to diagnose the disk.

When durable state is unusable, every protocol endpoint answers `503`. It refuses
rather than degrading, because an authority that grants an activation it cannot
write down has told two devices they hold the same organism.

## Durable data store

One file: `$DLL17_AUTHORITY_STORE/authority.dll17`, canonical envelope, schema
`261` v1. Written through `authority.staging`, `fsync`ed, then atomically
renamed. There is no database, no connection pool and no migration engine.

**One instance only.** The compare-and-swap is a process-level lock over a single
file. Two instances against one store would both believe they held the lock, and
the failure mode is two devices owning one organism. Horizontal scaling is not a
tuning exercise here; it is a redesign, and it is listed as blocked.

## Backup contract

| Property | Value |
|---|---|
| What to back up | `authority.dll17` only |
| Consistency | the atomic rename means any copy of the file is a complete past state |
| Frequency | after any activation; a schedule is a deployment decision |
| Encryption at rest | the operator's responsibility; the file is not encrypted by the service |
| Retention | operator decision; the file is small and grows only with organism count |

### The restore hazard, stated plainly

Restoring an **older** copy of `authority.dll17` moves epochs *backwards*. A
device that was superseded would stop being superseded, and two devices could
then believe they hold the same organism — the exact harm the authority exists to
prevent.

So the restore procedure is not "put the backup back":

1. Stop the service.
2. Compare the backup's epoch for each organism against any known current epoch.
3. Restore only if the backup is not behind. If it is behind, the correct action
   is to keep the newer state and accept the loss of whatever else was in the gap.
4. Start the service and confirm `/readyz`.
5. Verify with a `heartbeat` for a known organism that the epoch is what you
   expect *before* announcing the service as recovered.

An operator who cannot establish step 2 should treat the restore as unsafe and
escalate rather than proceed.

## Upgrade and migration

The store schema is `261` v1 and has never changed. If it ever does:

1. the new build must read v1 and write the new version;
2. the migration must be a pure function of the decoded state, so it is idempotent;
3. it must stage, `fsync` and rename, so a crash leaves either the old readable
   state or the complete new one;
4. an unknown *future* version must be refused rather than guessed at.

That is the same discipline `LocalStorageCryptographyContractV2` used for the
schema `231` v1 to v2 migration, and the reasoning is identical: reinterpreting
an unknown layout is how a downgrade turns into a birth.

Rolling upgrade is not available — see the single-instance constraint. The
procedure is: drain, stop, deploy, start, verify `/readyz`.

## Logging and privacy

The access log holds exactly a path, an organism id, an outcome name and a
request id, is bounded at 4096 lines, and its shape is asserted by
`FX-NET-PRIVACY-LOG-01`. Nothing else may be added: an unstructured field is how
organism content eventually reaches a log aggregator.

`AuthorityResponse.detail` is generated for diagnostics and deliberately never
crosses the wire.

Operators must not enable proxy request-body logging. The bodies are short, but
they contain activation proofs.

## Incident runbook

| Symptom | First check | Likely cause | Action |
|---|---|---|---|
| `/readyz` failing, `/healthz` passing | store directory writable? | full or read-only disk | free space or remount; the service recovers with no restart |
| All activations `503` | `/readyz` | as above | as above |
| A user reports both devices working | `heartbeat` from each device | authority never contacted, or an older store was restored | see the restore hazard; a cloned offline device cannot be invalidated — this is the known limit of *supported* singularity |
| `EPOCH_CONFLICT` on a legitimate recovery | the organism's current epoch | a previous activation already consumed the epoch | the caller must request `current + 1`; there is no override, by design |
| Repeated `RATE_LIMITED` | failed proof count | wrong recovery secret, or an attack | 5 failures per 10 minutes is the frozen limit; it clears on its own |
| `409` on `register` | existing record's device fingerprint | a superseded device trying to re-register | refuse; this is the rule working |

**There is no administrative override and none should be added.** An endpoint
that could force an epoch is an endpoint that can steal an organism.

## Deployment artifacts

- `Dockerfile` — builds the service on the pinned JDK 17 and runs as a non-root
  user with a read-only root filesystem and one writable volume.
- `compose.yaml` — a reproducible local deployment, one instance, one volume,
  with the health and readiness checks wired to the right endpoints.

Both are reproducible local deployments. Neither is a production topology: there
is no TLS, no proxy and no secret management in them, because a compose file that
looked production-shaped would invite being used as one.

---

## Not production-qualified

Stated explicitly, because everything above reads like an operations manual and
some of it has never been exercised.

| Claim | State |
|---|---|
| Production hosting | **Not deployed.** Nothing here has run outside a developer machine |
| High availability | **Not available.** Single instance by design; a second instance breaks correctness |
| Any SLA | **None.** No availability target is measured or offered |
| Geographic redundancy | **None.** One store, one host |
| Disaster recovery | **Procedure only.** The restore procedure above has never been executed against a real incident |
| Backup verification | **Not automated.** No check confirms a backup is restorable |
| TLS | **Not exercised.** Both qualification runs are loopback plaintext HTTP |
| Load, capacity or latency | **Not measured.** No threshold is derived and none may be |
| Monitoring, alerting, paging | **None configured** |
| Rolling upgrade | **Not possible** under the single-instance constraint |
| Verification-key rotation | **Not designed.** Requires a directive |
