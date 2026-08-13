# R002 exit gate

- Phase: R002 — lifecycle, durability, trusted time and reconciliation
- Directive: D007
- Contract: `ContinuityDurabilityContractV1` version 1, `FROZEN`
- Qualified fixture set: `R002-FIXTURES-V1` version 1
- Golden evidence digest:
  `556bbe49df16595f748a487f78a17a83866eb2a018814f69ee469d7976d58d21`
- Evaluated: 2026-08-13

Two canonical gates govern R002 and both are evaluated criterion by criterion
below: the **Implementation Plan E2E** gate with nine criteria, and the
**Digital Living Lifeform** charter section 18 gate. A criterion passes only if
evidence exists for it; "the code is present" is not evidence.

---

## Gate A — Implementation Plan E2E, section 5 "R002 exit gate"

| # | Criterion | Result | Evidence |
|---|---|---|---|
| 1 | No lifecycle callback is required for correctness | `PASS` | Nothing in `core-continuity` consumes an Android lifecycle callback. Recovery begins from the last durable anchor and reconstructs the elapsed interval from clock evidence supplied by the caller; the module does not link the Android framework at all. `FX-DEATH-AFTER-COMMIT-01` recovers with no callback in the picture. |
| 2 | No background job is required for continuity | `PASS` | There is no scheduler, no `WorkManager` reference and no resident reducer. Elapsed time is reconstructed at startup from the anchor, not accumulated by a running process. `FX-LONG-ABSENCE-01` reconciles thirty days that nothing observed. |
| 3 | Repeated reboot cannot speed-run or freeze biology beyond documented protected-life rules | `PASS` | `FX-REPEATED-REBOOT-01`: eight reboots raised `BootVelocityAnomaly` and could not exceed the standing four-hour budget in total. Blind time does not advance verified time, so the loop cannot earn the verified time needed to refill itself. Nor can it freeze biology: `FX-DEBT-CAP-01` shows the unverifiable interval still accrues bounded debt. |
| 4 | Reconciliation produces identical results on reference and device fixtures | `PASS` | Desktop JVM, x86_64 emulator and Tensor G4 all produced digest `556bbe49…76d58d21` and all six section digests identically. `CONTINUITY_MATRIX.md`. |
| 5 | UI thread remains responsive during reconciliation and persistence pressure | `PASS`, with a stated limit | Structurally: reconciliation is a resumable pure computation with an explicit cursor that yields between bounded batches, and slicing at 1, 2, 7, 64 and 10,000 chunks produces the identical sequence. `core-continuity` cannot touch a UI thread because it cannot see one. **Limit:** no frame-time measurement was taken, because R002 wires no organism rendering to be measured. |
| 6 | No visible material mutation is lost after process death | `PASS` | `FX-PANIC-WITNESS-01` makes three witnessed transitions visible, fails the suspend anchor, and confirms every one still exists in the recovered history. `FX-DEATH-BEFORE-COMMIT-01` confirms the converse: an unacknowledged write leaves no trace and was never visible. |
| 7 | `TEMPORAL_DESYNC` never depicts active life | `PASS` | `DurabilityPresentation` refuses autonomous-life output outside `RECOVERY_RECONCILIATION`, and the prohibited vocabulary — cryosleep, frozen time, paused life, suspended animation, stasis — is rejected by name. `STASIS_PROJECTION` is retired and its ordinal never reused. |
| 8 | Thermal or platform suspension cannot corrupt canonical history | `PASS` | `FX-DEEP-SUSPEND-01`, `FX-PANIC-WITNESS-01`, `FX-BOTH-WRITES-FAIL-01`: at most one anchor attempt and one witness attempt, no retry loop, and in every branch recovery resumes from the last durable anchor without fabricating state. |
| 9 | Long-absence runtime is bounded and supported without scheduled Android work | `PASS` | A one-year absence applies exactly the same 768 chunks as a 72-hour absence; past the bounded window the projection advances chronology only. Asserted directly by `runtime past the bounded window does not grow with the absence`. |

---

## Gate B — charter section 18, "R002 — Lifecycle and elapsed-time reconciliation"

The charter states the gate as: *valid, bounded, deterministic state after five
minutes, six hours, 72 hours, 30 days, six months and one year; runtime must
remain bounded and independent of Android scheduled work.*

Each of the six durations is asserted directly by
`the canonical absence ladder produces valid bounded deterministic state`.

| Absence | Valid | Bounded | Deterministic |
|---|---|---|---|
| 5 minutes | reserves and phase inside bounds, chronology exact | 5 chunks | two runs byte-identical |
| 6 hours | as above | 360 chunks | byte-identical |
| 72 hours | as above | 768 chunks | byte-identical |
| 30 days | as above | 768 chunks | byte-identical |
| 6 months | as above | 768 chunks | byte-identical |
| 1 year | as above | 768 chunks | byte-identical |

| Charter clause | Result |
|---|---|
| Valid state at every listed absence | `PASS` |
| Bounded state at every listed absence | `PASS` |
| Deterministic state at every listed absence | `PASS` |
| Runtime bounded | `PASS` — chunk count stops growing at the 72-hour window |
| Runtime independent of Android scheduled work | `PASS` — no scheduler exists in the continuity path |

---

## Work package completion, Implementation Plan E2E section 5

| Package | Result |
|---|---|
| R002.1 — four clocks and durable anchors | Complete |
| R002.2 — same-boot and reboot trust | Complete |
| R002.3 — unresolved-time debt | Complete |
| R002.4 — offline modes A, B and C | Complete |
| R002.5 — lifecycle handoff | Durability half complete; rest-peek and rest semantics deferred, see below |
| R002.6 — journal and persistence foundation | Complete |
| R002.7 — storage pressure | Complete |
| R002.8 — `TEMPORAL_DESYNC` and reconnect | Complete |
| R002.9 — thermal and power protection | Complete |
| R002.10 — prepared-rest durable handoff | Deferred, see below |
| R002.11 — boot and power evidence classification | Complete |
| R002.12 — device binding, backup exclusion, cold recovery | Partially complete; two canonical blocked states, see below |

### R002.5 and R002.10 — prepared rest

Both packages are written in terms of **rest**: `RestEnvironmentPrepared`,
`RestHandoffCommitted`, rest dwell, exhaustion targets, `RestPeekObservationWindow`,
sensor quarantine, and the physiological consequences of a validated rest
session. Every one of those is an organism concept — a creature that rests,
becomes exhausted, and recovers — and the canonical architecture gates organism
mechanisms behind A001 and R003.

What R002 owed those packages, and delivered, is the **durability machinery** they
depend on: durable handoff records that survive process death, same-boot
monotonic evidence that can retroactively qualify an interval, exit signals
treated as diagnostic confidence rather than intent, and the rule that a
committed durable record is not downgraded merely because the process died. All
of that is implemented and qualified.

What R002 did not do is define what rest *is*. Implementing
`RestEnvironmentPrepared` here would have required choosing an exhaustion model,
a recovery curve and a set of contradiction conditions — species physiology,
before A001. That is the failure mode D007's scope boundary exists to prevent, so
the rest semantics are recorded as deferred rather than silently invented.

### R002.12 — recovery

| Element | Status |
|---|---|
| Android Auto Backup and device-transfer exclusion, every supported API level | Complete and asserted structurally |
| Device-bound key container as a provider-neutral interface | Complete |
| `COPIED_STATE_QUARANTINE` | Complete and qualified |
| Canonical identity record: organism ID, epoch, lineage hash, device fingerprint | Complete |
| Recovery-freshness accounting; provider confirmation as sole authority | Complete |
| `RecoveryPackageStore` provider-neutral interface | Complete |
| `RecoveryGapDeclared` canonical event | Complete |
| Provider-specific implementation | `BLOCKED_SPEC_RECOVERY_PROVIDER` |
| Recovery-secret entropy, mnemonic, KDF, key wrapping, package signature, epoch challenge-response, revocation | `BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY` |

Both blocked states are canonical. Implementation Plan E2E R002.12 requires a
reviewed `RecoveryCryptographyContractV1` to be frozen **before** recovery
cryptography or the epoch authority is implemented, and explicitly forbids
inventing those choices inside persistence or UI code. D007 authorized freezing
`ContinuityDurabilityContractV1` and no other contract, so this phase did not
freeze it. The canonical page also states that with no production provider
selected, provider-specific implementation is `BLOCKED_SPEC_RECOVERY_PROVIDER`
and *"current-device organism correctness remains unaffected"*.

---

## The scope conflict, stated rather than resolved quietly

D007 acceptance criterion 3 requires **every** R002 work package to be complete.
D007's own enumerated scope — its "R002 Must Establish" list, its qualification
areas, and its failure and exploit matrix — names none of prepared rest, device
binding, cold recovery, recovery packages or identity epochs.

On the enumerated scope, R002 is complete. On the blanket criterion, two packages
are partial for the reasons above: one because completing it requires physiology
that A001 gates, and one because completing it requires a contract D007 did not
authorize this phase to freeze.

This gate record reports `PASS` because both canonical exit gates — the E2E
nine-criterion gate and the charter gate, which are what `R002 = PASS` is defined
against in D007's own report specification — pass on evidence, and because the
two incomplete packages are blocked by canonical rules rather than by unfinished
work. **If the architect reads criterion 3 as governing, R002 is `BLOCKED` on
R002.5, R002.10 and R002.12, and every other criterion and all evidence above
stands unchanged.**

---

## Regression: R001 and R000 remain green

| Phase | Check | Result |
|---|---|---|
| R001 | Golden digest on desktop JVM | `54bc0447…adc1bd86`, unchanged |
| R001 | Golden digest on x86 emulator | unchanged, same instrumented session |
| R001 | Golden digest on Tensor hardware | unchanged, same instrumented session |
| R001 | Qualification bundle at pinned commit `e442e147` | verified |
| R000 | Qualification bundle at pinned commit `43054d0a` | verified, manifest unchanged |

R001's bundle is now pinned to its qualified commit, so the annotations this
phase made to `R001_EXIT_GATE.md` and `DETERMINISM_MATRIX.md` — recording the
2026-08-13 Snapdragon amendment — cannot disturb the evidence R001 closed on.

---

## Outstanding, and not counted as gate criteria

1. **Prepared-rest semantics are deferred to the phase that owns organism
   behaviour.** The durability machinery exists; the biology does not.
2. **Recovery cryptography and the storage provider are canonically blocked.**
   No contract, no provider, no implementation, nothing to qualify.
3. **Real storage failure modes are untested.** The durable medium is an
   in-process byte log with explicit fault injection. Filesystem-level torn
   writes, `fsync` that lies, and database corruption belong to the phase that
   adopts a storage engine.
4. **Android backup exclusion is asserted structurally, not exercised.** The
   platform's backup transport cannot be driven from a test on current Android
   releases; the failure mode being defended against is a missing declaration,
   which is what the structural check catches.
5. **No frame-time or UI-responsiveness measurement exists.** R002 wires no
   organism rendering, so there is nothing to measure yet. The structural
   guarantee — reconciliation yields and cannot see a UI thread — is what R002
   can honestly claim.
6. **Exynos remains canonically conditional and Snapdragon optional.** Neither
   has evidence in either direction.
7. **The repository is still public while carrying a proprietary licence**, with
   the historical MIT grant published for revisions before `43054d0`. Unchanged
   from R000 and R001; a legal question for the copyright holder.

---

**R002 = PASS**

Both canonical exit gates pass on evidence across three targets. The scope
conflict in acceptance criterion 3 is recorded above and is the architect's to
resolve; if criterion 3 governs, this reads `BLOCKED` and nothing else changes.
