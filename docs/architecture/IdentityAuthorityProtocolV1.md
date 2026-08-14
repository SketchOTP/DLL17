# IdentityAuthorityProtocolV1

- Status: `FROZEN`
- Version: 1
- Frozen under: D011
- Executable portion: `core-recovery/.../IdentityAuthorityProtocol.kt`
- Service implementation: `services/identity-authority/` (separately deployable)
- Qualification evidence: `qualification/fixtures/R012/R012_REPORT.txt`

The minimal online authority that makes a cold recovery an *event with a winner*,
so that after a recovery the old device cannot keep acting as the same organism.

---

## The honest guarantee

This protocol delivers **supported singularity**: a recovered organism supersedes
its predecessor, and the predecessor is rejected the next time it contacts the
authority.

It does **not** deliver absolute singularity. A device that is cloned and then
kept permanently offline cannot be invalidated by any online authority, because
there is nothing to invalidate it through. The authority never reaches out; a
device that never calls is never told.

The product must claim the first and must never claim the second. This asymmetry
is a property of the physics, not a gap in the implementation.

## Daily operation never touches it

Normal local runtime never calls the authority for cognition, physiology, memory,
action selection or ordinary startup. If the service is permanently unavailable,
new recovery activation is unavailable and an already activated organism is
completely unaffected.

This is enforced structurally, not by convention: `services/identity-authority`
is outside the organism core dependency graph, and `ModuleBoundaryTest` asserts
that the service class is **not** on `core-persistence`'s classpath. The R012
kernel takes an authority as a parameter for the same reason.

---

## What the authority stores, exhaustively

Organism id, current epoch, active device fingerprint, verification key, lease
expiry, lease device fingerprint, granted epoch, failed-proof counter, failure
window start.

That list is the whole record type, and a test asserts the declared field set
exactly. There is no field for physiology, memories, learning, relationships,
journals, inventory or package plaintext — and the authority could not decrypt
them if there were, because it never receives the recovery root.

## Frozen parameters

| Parameter | Value |
|---|---|
| Challenge validity | 5 minutes |
| Activation lease | 15 minutes |
| Failed proofs before rate limiting | 5 |
| Rate-limit window | 10 minutes |
| Challenge nonce | 16 bytes, single use, derived from authority state |

## Frozen proof

```
proof = HMAC-SHA-256(
    key     = HKDF(recoveryRoot, salt = organismId, info = DLL17-IDENTITY-AUTHORITY-PROOF-V1),
    message = organismId ‖ nonce ‖ requestedEpoch ‖ deviceFingerprint
)
```

It proves possession of the recovery root without revealing it, is bound to one
nonce so it cannot be replayed, is bound to the requested epoch so a captured
proof cannot be reused for a later activation, and is bound to the device so a
captured proof is useless to anyone else.

## Frozen operations

| Operation | Semantics |
|---|---|
| `register` | Birth-time. Establishes epoch 1. Idempotent for the same device and key; a **conflict** for a different device, so a superseded device cannot re-register its way back. |
| `challenge` | Issues a single-use nonce with an expiry. Rate limited. |
| `activate` | Atomic compare-and-swap: the requested epoch must be exactly `current + 1`. On success the epoch advances and a lease is granted, in one durable step. |
| `heartbeat` | How a superseded device learns. Not required for local operation; its failure is not an error. |

### Outcomes

`REGISTERED`, `CHALLENGE_ISSUED`, `ACTIVATION_GRANTED`, `EPOCH_CONFLICT`,
`PROOF_REJECTED`, `CHALLENGE_INVALID`, `RATE_LIMITED`, `SUPERSEDED`,
`UNKNOWN_ORGANISM`, `ALREADY_GRANTED`.

### Replay protection, in three layers

1. A nonce is single-use and is **spent even on a failed proof** — a nonce that
   survives a failure is a nonce an attacker can grind against.
2. A nonce expires.
3. The epoch compare-and-swap means a replayed activation for an already-consumed
   epoch is an `EPOCH_CONFLICT`, not a second advance.

`ALREADY_GRANTED` is the deliberate exception: the *same* device asking again for
an epoch it already holds, inside its lease, must succeed, because the caller may
simply have lost the response and an idempotent retry has to be safe.

### Racing destinations

Two devices recovering from the same package cannot both win. The second arrives
with a requested epoch that is no longer `current + 1` and is refused.

---

## Recovery flow, frozen order

1. Destination creates a device-bound key.
2. User supplies the recovery secret and the encrypted cold package.
3. Destination **verifies the package** — integrity, organism, epoch floor.
4. Authority atomically advances the epoch and grants a lease.
5. Destination rewraps local keys, durably commits the recovered state plus
   `RecoveryGapDeclared` where applicable, and begins under the new epoch.
6. A prior device that later contacts the authority is rejected.

The order is the security property. Verifying before asking means a corrupt or
foreign package never consumes an epoch; advancing the epoch before writing local
state means a crash mid-restore cannot leave two devices believing they hold the
current epoch.

## Blocked

| Item | State |
|---|---|
| Transport (HTTP surface, TLS, auth) | Not frozen; a deployment decision, and correctness is provable without one |
| Hosting, backup, rotation and incident procedures for the authority database | `BLOCKED_SPEC_SERVICE_OPERATIONS` — requires an owner and an environment |
| Production rate-limit tuning | The frozen values are protocol defaults, not measured production limits |
