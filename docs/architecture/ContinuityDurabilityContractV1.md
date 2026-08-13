# ContinuityDurabilityContractV1

- Contract ID: `ContinuityDurabilityContractV1`
- Contract version: `1`
- Status: `FROZEN`
- Frozen under: directive D007, phase R002
- Owner gate: R002. Changing any frozen clause requires a new contract version,
  an architect amendment, regenerated R002 golden vectors, and retention of the
  superseded vectors.
- Depends on: `DeterminismContractV1` version 1, `FROZEN`, unchanged by this
  contract.

This artifact freezes every decision that determines whether one organism keeps a
truthful identity and a truthful biological history across Android lifecycle
interruption, process death, reboot, long absence, storage pressure, thermal and
power emergencies, interrupted writes, and restart. It is the R002 analogue of
`DeterminismContractV1`: it exists **before** the dependent implementation, and
no file in `core-continuity` may select a continuity-relevant algorithm,
ordering, threshold or record layout that is not named here.

`DeterminismContractV1` decides how canonical bytes are formed. This contract
decides which bytes are allowed to become durable, in what order, under what
evidence, and what may be shown to the user before they are. The two are
disjoint by construction: nothing here changes an encoding, a hash, a PRNG or a
rounding mode.

The candidate evaluation behind each choice is recorded in section 18 and in
`docs/decisions/DECISION_LOG.md` entries `IMPL-0016` through `IMPL-0027`.

---

## 1. Scope, authority, and the organism boundary

### 1.1 What this contract governs

| In scope | Out of scope |
|---|---|
| The four canonical clocks and their units | What any clock means biologically |
| Durable time anchors and their record layout | Physiology, drives, affect |
| Time-confidence classification and clock anomalies | Action selection, learning, memory |
| Blind-decay credit and its replenishment rule | Species baselines and rates |
| Unresolved-time debt and bounded reconciliation | Torpor, distress, critical care |
| Offline reconciliation modes, chunking and ordering | What is reconciled, beyond neutral fixtures |
| Journal generations, snapshots, compaction, pruning | Room, SQLite or any specific storage engine |
| Durability admission states and their exact orderings | Rendering, animation, UI toolkit choice |
| Platform protection states and their exact orderings | Thermal policy of the device itself |
| The encrypted-record boundary | Recovery cryptography (see 1.3) |
| Restart, recovery and version boundaries | Cross-device roaming and live transfer |

### 1.2 The organism boundary

R002 implements **no organism behavior**. Where continuity logic needs state to
act on, it acts on neutral R002 fixture state — bounded reserves named
`reserveA` and `reserveB` with no physiological meaning, and a drain rate that
exists only to make a reconciliation observable. Naming one of them `energy`
would be an R003 decision behind the A001 aliveness gate, and this contract does
not make it.

Every parameter introduced by this contract that a later organism phase will
replace is marked in `ParameterRegistry` with the scope `R002 fixture only`.
There are exactly two of them:

| Fixture parameter | Frozen value | Unit |
|---|---|---|
| `FIXTURE_RESERVE_DRAIN_PER_MINUTE` | `Fixed64` `0.001000` | reserve fraction per baseline-equivalent minute |
| `FIXTURE_PASSIVE_DEVELOPMENT_PER_MINUTE` | `Fixed64` `0.000100` | progress units per verified minute |

Passive development is granted only from verified time. Blind credit and
anomalous intervals grant none, in either direction.

### 1.3 What this contract deliberately does not decide

The following are named here only so that a later phase cannot claim they were
silently settled:

- **Recovery cryptography.** Recovery-secret entropy and encoding, mnemonic
  wordlist, KDF, key wrapping, package manifest signatures, the identity-epoch
  challenge-response, replay protection and revocation semantics belong to
  `RecoveryCryptographyContractV1`, which is **not frozen** and which D007 did
  not authorize this phase to freeze. Implementation Plan E2E R002.12 forbids
  inventing those choices inside persistence or UI code, and this phase does not.
- **The recovery storage provider.** No integrated production provider has been
  selected, so provider-specific implementation is `BLOCKED_SPEC_RECOVERY_PROVIDER`
  exactly as R002.12 permits. Current-device organism correctness does not depend
  on it.
- **Species metabolism.** Baseline rates, thresholds, and every physiological
  consequence of elapsed time.
- **Storage engine.** This contract constrains ordering and durability semantics.
  It does not choose Room, SQLite, or a file format, and every rule here is
  stated so that it can be satisfied by any medium that can acknowledge a write.

---

## 2. Canonical time model

### 2.1 The four clocks

| Clock | Canonical type | Unit | Advances from |
|---|---|---|---|
| `wallClockAge` | `i64` | milliseconds since organism genesis | Display metadata only |
| `activeExperienceTicks` | `i64` | canonical ticks | Foreground canonical activity only |
| `developmentalProgress` | `Fixed64` | progress units | Evidence-bearing contributions only |
| `circadianPhase` | `Fixed64` | phase in `[0, 1)` | Any qualified elapsed time |

The four are separate fields with separate update rules. They are never derived
from one another, and no single elapsed interval advances all four.

**Why four and not one.** A single clock makes "the app was closed for a month"
and "the organism lived for a month" the same statement. They are not, and every
exploit this phase must defeat is a consequence of conflating them.

### 2.2 Canonical duration unit

Every canonical duration is an `i64` count of **milliseconds**. Durations are
never `Fixed64`, never floating point, and never a wall-clock timestamp
difference computed at the point of use.

Biological-equivalent quantities — the metabolic adjustment budget in particular
— are `i64` milliseconds of **species-baseline-equivalent** metabolism, not
elapsed milliseconds. The distinction is load-bearing: the global cap in
section 6 is a budget of biology, not a ledger of time.

### 2.3 Prohibited time sources in canonical logic

| Prohibited | Reason |
|---|---|
| `System.currentTimeMillis()` as an elapsed-duration authority | User- and network-settable; the primary spoof vector |
| Any wall-clock difference as reward-bearing time | Same |
| Timezone or locale in any canonical computation | Presentation only; never creates duration |
| `System.nanoTime()` across a process boundary | Undefined origin across processes |
| A device uptime reading not paired with a boot identity | Cannot distinguish reboot from rollback |

Wall-clock readings are recorded in anchors as display metadata and as anomaly
evidence. That is the whole of their canonical role.

---

## 3. Durable time anchors

### 3.1 Record layout

`DurableTimeAnchor`, canonical schema ID `210`, schema version `1`:

| Field | Type | Meaning |
|---|---|---|
| `anchorSequence` | `i64` | Monotonic, never reused, never decreasing |
| `wallClockUtcMillis` | `i64` | Display metadata and anomaly evidence only |
| `elapsedRealtimeMillis` | `i64` | Monotonic device uptime including deep sleep |
| `bootIdentityPresent` | `bool` | Whether a boot identity was obtainable |
| `bootIdentity` | `i64` | Boot identity when present, `0` otherwise |
| `logicalTime` | `i64` | Canonical logical time at the anchor |
| `timeConfidence` | enum `i32` | Section 4 classification |
| `authenticatedTimePresent` | `bool` | Whether authenticated time was available |
| `authenticatedTimeMillis` | `i64` | Authenticated UTC when present, `0` otherwise |

Absent optional values are encoded as an explicit `false` plus a zero, never as
a sentinel inside the value field. A sentinel would make "no boot identity" and
"boot identity zero" the same bytes.

### 3.2 Anchor obligations

- An anchor is written on every durable barrier, on entry to and exit from every
  durability hold, on entry to platform deep suspend, and at the commit cadence
  in section 9.
- An anchor is durable only when the write is acknowledged. A partially written
  anchor is not an anchor.
- Recovery begins at the **last durably acknowledged anchor**, never at a newer
  in-memory state, and never at an anchor whose write was interrupted.

---

## 4. Time confidence and clock anomalies

### 4.1 Classification

`TimeConfidence`, immutable ordinals:

| Ordinal | Name | Condition | Grants |
|---|---|---|---|
| `1` | `VERIFIED_MONOTONIC` | Boot identity unchanged and `elapsedRealtime` not less than the stored value | Full qualified elapsed time |
| `2` | `AUTHENTICATED` | An authenticated external time observation establishes the interval | Full qualified elapsed time |
| `3` | `UNVERIFIED_REBOOT` | Boot identity changed, or `elapsedRealtime` moved backwards, or no anchor is comparable | Blind-decay credit only, per section 5 |
| `4` | `ANOMALOUS` | Same-boot comparison exceeded the tolerance in 4.2 | Blind-decay credit only, and diagnostics |

Ordinals are immutable and are never reused. Classification is a pure function of
the stored anchor and the current observation; it consults nothing else.

### 4.2 Same-boot normalization and the anomaly rule

Within one boot, `elapsedRealtime` is the elapsed-duration authority because it
is monotonic and includes deep sleep. The wall clock is compared against it only
to detect anomalies:

```
wallDelta      = observed.wallClockUtcMillis - anchor.wallClockUtcMillis
elapsedDelta   = observed.elapsedRealtimeMillis - anchor.elapsedRealtimeMillis
skew           = abs(wallDelta - elapsedDelta)
skew > WALL_ELAPSED_SKEW_TOLERANCE_MILLIS  →  ClockAnomalyDetected
```

`WALL_ELAPSED_SKEW_TOLERANCE_MILLIS` is frozen at `120000`.

A detected anomaly:

- **downgrades** time confidence to `ANOMALOUS`;
- **blocks** reward-bearing, developmental, learning, relationship, evolution and
  cooldown inference for that interval;
- **does not** create elapsed biology;
- **does not** create negative elapsed time;
- **does not** apply a punitive physiological jump.

An anomaly is evidence that the wall clock moved, not evidence that the organism
lived. Backward wall movement clamps the derived duration to zero and never
produces a negative interval.

### 4.3 Boot and power evidence

| State | Formed from | Effect |
|---|---|---|
| `BootVelocityAnomaly` | More than `BOOT_VELOCITY_MAX_BOOTS` boot identity changes inside `BOOT_VELOCITY_WINDOW_MILLIS` of verified time | Zero further blind progression; diagnostics |
| `PROBABLE_POWER_LOSS` | A prior critical battery observation **plus** compatible later boot or exit evidence | Provenance label only |
| `LIKELY_PLATFORM_FORCED_SUSPEND` | A valid `PlatformPanicWitness` covering the gap | Diagnostics and presentation only |
| `LOST_TO_HARDWARE_FAULT` | An unknowable rollback gap | Provenance label only |

None of these four states creates, refunds, forgives or fabricates canonical
state. A low-battery latch alone never proves cause; a single signal never
becomes an inference.

Repeated launches within one boot never consume blind-decay credit again. The
consumption record is keyed by boot identity, not by launch.

---

## 5. Blind-decay credit

### 5.1 The budget

`blindDecayCredit` is a persisted `i64` millisecond budget.

| Parameter | Frozen value |
|---|---|
| `BLIND_DECAY_CREDIT_MAX_MILLIS` | `14400000` (4 hours) |
| Replenishment ratio | 1 millisecond of credit per 6 milliseconds of verified elapsed time |
| Replenishment cap | `14400000` credit milliseconds per `86400000` verified milliseconds |

### 5.2 Replenishment rule

Replenishment uses integer division with an explicit carried remainder:

```
pool      = carriedRemainder + verifiedElapsedMillis
granted   = pool / 6
carried   = pool % 6
```

The remainder is carried in durable state rather than discarded. Discarding it
would make one 6 ms interval and six 1 ms intervals grant different credit, which
is a drift an adversary can farm by fragmenting sessions.

Credit replenishes **only** from `VERIFIED_MONOTONIC` or `AUTHENTICATED` elapsed
time. Wall-clock movement, boot count increments, reboots, reinstalls and
timezone changes replenish nothing.

### 5.3 Consumption rule

On an interval classified `UNVERIFIED_REBOOT` or `ANOMALOUS`:

1. Compute the requested duration. A plausible wall-clock delta may **reduce**
   the request below the available credit; it may never raise it.
2. Consume `min(request, availableCredit)` milliseconds.
3. Durably reduce the credit **before** the reconciled presentation is revealed.
4. Apply that much protected passive metabolism, and nothing else.
5. Record any validated excess as `UnresolvedTimeDebt` (section 6), which is
   evidence of uncertainty and not an immediately collectible penalty.

Step 3's ordering is the anti-exploit: a process death between reveal and debit
must not refund the credit. Credit is spent when the durable write is
acknowledged, and the reveal follows the write.

### 5.4 What blind progression may and may not touch

| May affect | May never affect |
|---|---|
| Protected passive metabolism within protected-life bounds | Active experience |
| Circadian phase | Development or life-stage transition |
| Rest pressure | Learning, habituation recovery |
| | Relationship change |
| | Evolution readiness |
| | Cooldown or time-based unlocks |
| | Reward of any kind |

---

## 6. Unresolved-time debt and bounded reconciliation

### 6.1 States

`DebtState`, immutable ordinals: `1` `IDLE`, `2` `ACCRUED`, `3` `COLLECTING`,
`4` `DebtPausedLowReserve`, `5` `FORGIVEN`.

### 6.2 The global cap

| Parameter | Frozen value |
|---|---|
| `DEBT_GLOBAL_CAP_BASELINE_EQUIV_MILLIS` | `259200000` (72 hours baseline-equivalent) |
| `DEBT_PER_CHUNK_CAP_BASELINE_EQUIV_MILLIS` | `900000` (15 minutes) |
| `DEBT_PER_VERIFIED_DAY_CAP_BASELINE_EQUIV_MILLIS` | `21600000` (6 hours) |
| `DEBT_RETENTION_HORIZON_MILLIS` | `2592000000` (30 days of verified time) |

The cap applies across active, paused and newly accrued debt together. Newly
validated unresolved time may fill only the remaining headroom; every millisecond
beyond the cap is **permanently forgiven at accrual** and recorded as
non-collectible audit evidence. It is never retained in a shadow ledger.

### 6.3 Eligibility

Debt adjustment is applied **exclusively** inside a later verified Mode B or
Mode C reconciliation chunk. It is prohibited during:

foreground interaction, Mode A, feeding, play, tactile processing,
rehabilitation, social interaction, and any other foreground action.

Foreground metabolism is always baseline with no debt surcharge. Returning to
the foreground can never increase biological burn.

### 6.4 The safety floor and hysteresis

| Parameter | Frozen value |
|---|---|
| `DEBT_SAFETY_FLOOR` | `Fixed64` `0.200000` of reserve capacity |
| `DEBT_ABUNDANCE_REARM` | `Fixed64` `0.800000` of reserve capacity |
| `DEBT_REARM_STABILITY_MILLIS` | `3600000` (1 hour of verified time) |
| `DEBT_REARM_GRACE_MILLIS` | `1800000` (30 minutes) |
| `DEBT_POST_REVEAL_COLLAPSE_MARGIN` | `Fixed64` `0.050000` |

Before **every** adjustment substep, the projected post-adjustment value of each
affected reserve is computed. If any reserve is at or below `DEBT_SAFETY_FLOOR`,
or the substep would cross it, apply **zero** further adjustment and enter
`DebtPausedLowReserve`.

While paused:

- ordinary verified metabolism continues normally — only uncertainty collection
  pauses, so holding a mid-level reserve buys no immortality;
- incremental care does not rearm collection;
- rearm requires **every** affected reserve at or above `DEBT_ABUNDANCE_REARM`,
  held stable for `DEBT_REARM_STABILITY_MILLIS`;
- rearm becomes effective only in a later eligible Mode B or C chunk, after
  `DEBT_REARM_GRACE_MILLIS`, and never inside the same care event that restored
  the reserve;
- once rearmed, per-chunk and per-day caps remain in force and the next
  low-reserve crossing pauses again.

Debt adjustment may never independently trigger torpor, severe distress, injury
or a critical-tier transition, and may never leave a reserve inside
`DEBT_POST_REVEAL_COLLAPSE_MARGIN` of collapse at reveal.

### 6.5 Forgiveness

Budget that cannot be safely collected within `DEBT_RETENTION_HORIZON_MILLIS` of
verified time is expired with audit evidence. There is no permanent invisible
liability and no delayed punishment bomb. Resolved intervals grant zero
development, learning, reward, cooldown, evolution or relationship credit in
either direction.

---

## 7. Offline reconciliation modes

### 7.1 Mode selection

Selection is a pure function of the qualified elapsed duration:

| Mode | Interval | Behavior |
|---|---|---|
| `MODE_A` | `0` to `300000` ms | Ordinary cadence replay; continuity-preserving bridge |
| `MODE_B` | `300000` to `259200000` ms | Bounded multi-rate chunking |
| `MODE_C` | beyond `259200000` ms | Mode B over the first 72 hours, then bounded projection |

### 7.2 Chunk schedule

Frozen, and evaluated against the **offset from the start of the absence**, not
against the remaining duration:

```
offset in [0, 21600000)          →  60000 ms chunks    (0–6 h)
offset in [21600000, 86400000)   →  300000 ms chunks   (6–24 h)
offset in [86400000, 259200000)  →  900000 ms chunks   (24–72 h)
```

A final partial chunk carries the remainder and is never rounded up. Rounding up
would let an adversary manufacture free biology by choosing absence lengths.

### 7.3 Deterministic ordering inside a chunk

Every chunk applies exactly this order, and reconciliation is a pure function of
`(anchor, elapsedEvidence, contract)`:

```
1. advance wallClockAge
2. advance circadianPhase
3. apply verified baseline metabolism
4. clamp and validate reserves
5. apply debt adjustment if and only if eligible (section 6)
6. clamp and validate reserves again
7. emit chunk diagnostics
```

Debt adjustment after baseline metabolism, with a clamp on both sides, is what
makes the safety floor checkable rather than approximately observed.

### 7.4 Mode C bounds

- Simulate the first 72 hours through Mode B chunks.
- Project slow state toward a safe bounded equilibrium thereafter.
- Cap all passive drift.
- Advance chronological age and circadian phase.
- Grant no more than `259200000` ms of passive developmental progress per
  uninterrupted absence.
- Synthesize no interactions, learning, relationships, memories or skills.
- Permit no life-stage transition from passive progress alone.
- Record `ExtendedAbsenceReconciled`.

### 7.5 Execution and gating

- Reconciliation is canonical work and never runs on the Android UI thread.
- The canonical interaction gate is closed for the whole of reconciliation and
  opens only after the final durable commit.
- Inputs received while the gate is closed are **discarded, not queued**. They
  consume no item and produce no canonical effect.
- Environmental context uses latest-value latches, sampled once after commit.
- Reconciliation yields between bounded batches, preserving a deterministic
  cursor. `RECONCILIATION_SLICE_CHUNKS` is frozen at `64` chunks per slice.
- An interrupted reconciliation leaves the last committed snapshot authoritative.
  Resuming from a cursor must produce the same result as an uninterrupted run —
  this is a qualification criterion, not an aspiration.

---

## 8. Durability classes and visibility

Class E, O and W are inherited unchanged from `DeterminismContractV1` and R001.
This contract adds the parameters R001 deliberately left unfrozen:

| Parameter | Frozen value | Basis |
|---|---|---|
| `CLASS_O_COMMIT_CADENCE_MILLIS` | `500` | Midpoint of the canonical 250–1000 ms band |
| `CLASS_O_MAX_UNCOMMITTED_WINDOW_MILLIS` | `1000` | Upper edge of the same band |
| `PANIC_WITNESS_ATTEMPT_DEADLINE_MILLIS` | `20` | R001 measured p99 of 16,215 ns, with ~1200× headroom |

The visibility invariant is unchanged and absolute:

```
no Class W or other material mutation becomes user-visible
until its CommitFrame is durably acknowledged
```

Irreversible boundaries — identity recovery, evolution, life-stage transition,
torpor transition, schema migration, safe-hold entry and exit, deep-suspend entry
— require an explicit durable barrier before the final user-visible transition.

---

## 9. Journal generations, snapshots and compaction

### 9.1 Generations

- Journal generations are logical partitions inside one schema, identified by
  immutable `i64` `generationId` values.
- They are **not** separate files, connections or renamed tables.
- The generation flip is an in-memory increment. No transaction is required
  merely to flip.
- Every commit frame is stamped with the generation active when it was emitted.

### 9.2 Thresholds

| Parameter | Frozen value |
|---|---|
| `JOURNAL_BYTE_BUDGET` | `8388608` (8 MiB) |
| Soft flip threshold | `JOURNAL_BYTE_BUDGET * 9 / 10` |
| `EMERGENCY_DURABILITY_RESERVE_BYTES` | `65536` |

The reserve is preallocated during healthy operation and is spent only on the
pressure marker, a critical transition, one safe checkpoint or anchor, and entry
into a durable hold.

### 9.3 Compaction

1. The compactor reads a **sealed** generation and builds the candidate
   checkpoint outside the writer lock.
2. The checkpoint is durably written, hash-verified, and coverage-verified.
3. Only then are older journal pages pruned.
4. If compaction fails or the process dies, both the sealed and active journals
   remain recoverable. No covered data is deleted.
5. Compaction never invents or recomputes canonical state. A canonical
   compaction proposal must return through the reducer with matching input-state
   and generation hashes.
6. If the active generation approaches its hard limit before the sealed
   generation is compacted, optional maintenance is suspended and backpressure
   preserves canonical transitions rather than deleting uncheckpointed history.

### 9.4 Snapshots

Every checkpoint records the application build, engine-contract version, schema
version, event-contract version and random-contract version that produced it.
Recovery uses the latest verified checkpoint plus the bounded transitions after
it. Full replay from genesis is a qualification-fixture capability, not a
production storage requirement.

---

## 10. Durability admission

### 10.1 States

`DurabilityAdmissionState`, immutable ordinals: `1` `OPEN`, `2` `PRESSURE`,
`3` `READ_ONLY_SURVIVAL`, `4` `STORAGE_FAULT`.

### 10.2 Entry ordering — this order is normative

```
capacity or compaction safety threshold crossed
→ close mutation-producing admission
→ attempt emergency-reserve CommitFrame
→ DurabilitySafeHoldEntered committed
→ stop canonical advancement and publication
→ TEMPORAL_DESYNC presentation
```

- Inputs rejected before reducer admission consume no item and produce no
  relationship, learning, memory, physiology or action commitment.
- Once `DurabilitySafeHoldEntered` is durable there is **no hidden canonical RAM
  simulation**. Only non-canonical repair, navigation, settings and diagnostic UI
  remains active.
- If the emergency safe-hold commit itself cannot be made, enter `STORAGE_FAULT`
  from the last already-durable anchor and expose recovery UI. A newer state is
  never presented as canonical.

### 10.3 Exit ordering — also normative

```
storage self-test and reserve check
→ DurabilitySafeHoldExited
→ close organism interaction gate
→ reconcile elapsed evidence from the last durable anchor
→ commit reconciled result
→ reveal final canonical state
→ reopen mutation admission
```

The exit event does not erase the held interval. Elapsed biology is handled
exclusively by the trusted-time and offline-reconciliation rules, so a hold is
never a way to skip time.

### 10.4 STORAGE_FAULT

Used for persistent full storage, corruption, failed fsync, or unrecoverable
writer error. Read-only presentation, repair and export UX, no foreground retry
storm, and no canonical mutation accepted until a durability self-test and
checkpoint write both succeed.

---

## 11. Presentation states during a durability hold

`DurabilityPresentationState`, immutable ordinals: `1` `TEMPORAL_DESYNC`,
`2` `STORAGE_REPAIR_REQUIRED`, `3` `RECOVERY_RECONCILIATION`.

`STASIS_PROJECTION` is **superseded and prohibited**. Its ordinal is retired and
never reused.

`TEMPORAL_DESYNC` rules:

- Communicates that reliable contact is lost and that biological time may still
  be passing.
- The last durable status may be shown only as historical telemetry, labelled
  with its durable timestamp or elapsed age.
- Prohibited semantics: cryosleep, frozen time, paused life, suspended
  animation, stasis, or any autonomous-life presentation.
- Prohibited behavior: autonomous locomotion, exploration, feeding, play,
  emotional response, or anything implying current canonical observation.
- Mutation-producing inputs are rejected without consuming items.

On recovery: close the gate, reconcile from the last durable anchor, commit,
then reveal through a qualified found-state bridge. A threshold crossed during
the disconnected interval is presented as having occurred while contact was
unavailable. A healthy organism is never depicted immediately before hidden
biological consequences are applied.

`verifiedElapsedBiology` is never truncated merely because reconnect happens near
or after a critical threshold. Stopping reconciliation at the start of a distress
window is explicitly prohibited: it would convert deliberate storage exhaustion
into a guaranteed last-second rescue.

---

## 12. Platform protection

### 12.1 States

`PlatformProtectionState`, immutable ordinals: `1` `NORMAL`, `2` `RESOURCE_SHED`,
`3` `PLATFORM_DEEP_SUSPEND`, `4` `PLATFORM_RECOVERY`.

Platform protection has absolute priority over in-world rendering.

### 12.2 RESOURCE_SHED

Progressively disable expensive work in a frozen order, so that shedding is
reproducible rather than whatever the renderer happened to drop first:

```
1. particles
2. post-processing
3. shadows
4. camera capture
5. microphone analysis
6. high-frequency sensors
7. physics detail
8. pathfinding frequency
9. audio synthesis
10. animation complexity
11. frame-rate cap and 2D status surface
```

Optional interactions whose execution would increase thermal or power load are
rejected. Canonical correctness is unaffected by which shed level is active.

### 12.3 PLATFORM_DEEP_SUSPEND — normative ordering

```
qualified critical platform condition
→ close mutation-producing admission
→ attempt ONE bounded PlatformDeepSuspendEntered canonical anchor
→ if that anchor fails, attempt ONE preallocated PlatformPanicWitness overwrite
→ immediately stop and release renderer, physics, pathing, IK, camera, mic,
  audio, nonessential sensors, particles
→ cease periodic foreground simulation
→ permit background or termination
```

- **No retry loop.** At most one anchor attempt and at most one witness attempt.
- `SharedPreferences.apply()` is prohibited for this role. Synchronous `commit()`
  is not the preferred panic path either: it can block and is not independent of
  storage health.
- If the anchor succeeds it is authoritative and the witness is unnecessary.
- If both writes fail, recovery still begins at the last durable anchor.
- No resident background reducer exists during deep suspend. Canonical state does
  not advance without durable recovery guarantees.
- Deep suspend grants no biological pause, reward, rest, development or
  relationship credit.

### 12.4 PLATFORM_RECOVERY

```
thermal/power below reentry threshold for THERMAL_REENTRY_HYSTERESIS_MILLIS
→ enter PLATFORM_RECOVERY on the 2D surface only
→ bounded elapsed reconciliation on the computation dispatcher
→ commit recovered state
→ reinitialize renderer, physics, pathfinding, sensors incrementally
→ reveal the qualified found-state
```

`THERMAL_REENTRY_HYSTERESIS_MILLIS` is frozen at `60000`.

### 12.5 The panic witness

Inherited from R001 and unchanged: preallocated, fixed size, write-once per
attempt, excluded from canonical hashing and replay, and a **recovery hint
only**. It may classify a gap as `LIKELY_PLATFORM_FORCED_SUSPEND`. It may not
reconstruct interactions or invent canonical state. Failure to write it is
tolerated.

---

## 13. The encrypted-record boundary

### 13.1 Position of the boundary

```
canonical plaintext bytes  →  state hash, replay, determinism   (unencrypted)
                           →  encrypted record  →  durable medium
```

Encryption sits **below** the canonical byte layer and **above** the medium.
Canonical hashing, replay and cross-target determinism operate on plaintext
canonical bytes and are unaffected by encryption. This position is deliberate: a
ciphertext-level hash would make the state hash depend on key material and
destroy the R001 cross-target guarantee.

### 13.2 Algorithm

| Clause | Value |
|---|---|
| Identifier | `AEAD_CHACHA20_POLY1305_V1` |
| Specification | RFC 8439 |
| Key length | 32 bytes |
| Nonce length | 12 bytes |
| Tag length | 16 bytes |
| Implementation | Project-owned, pure Kotlin, no provider resolution |

**Why project-owned, and why ChaCha20-Poly1305.** The reasoning is the same as
for SHA-256 in `DeterminismContractV1` section 6: the algorithm is standardized
but *which implementation runs* is not, and provider resolution differs between a
desktop JDK and Android and moves as Conscrypt evolves. ChaCha20-Poly1305 is
integer-only, table-free, constant-shape, has published RFC 8439 test vectors,
and needs no AES hardware path — so one implementation is correct on every target
this program supports, including devices without AES-NI-equivalent acceleration.

### 13.3 Nonce discipline

The nonce is **derived, not random**:

```
nonce = u32(keyEpoch) || u64(recordSequence)      // 12 bytes, big-endian
```

`recordSequence` is the monotonic durable sequence, never reused within a key
epoch. A key rotation increments `keyEpoch`, which makes reuse across epochs
impossible even if sequences restart.

**Why derived.** A single-writer append-only log has a monotonic sequence
already. Deriving the nonce from it makes nonce reuse structurally impossible
rather than probabilistically unlikely, removes a randomness source from the
durable write path, and keeps the write path deterministic — which is what allows
an encrypted record to be reproduced exactly in a fixture.

### 13.4 Associated data

The AAD binds every record to its place in history:

```
AAD = u32(schemaId) || u32(schemaVersion) || i64(generationId)
   || i64(recordSequence) || i64(organismId) || u32(keyEpoch)
```

A record moved to another sequence, generation, organism or epoch fails
authentication. Relocation is therefore detected rather than silently accepted.

### 13.5 Failure semantics

| Condition | Result |
|---|---|
| Tag mismatch | `STORAGE_FAULT`. Never a silent skip, never a partial read |
| Unknown key epoch | Refuse. Never guess |
| Missing key container | Refuse and quarantine (section 14) |
| Truncated record | `STORAGE_FAULT` |

The key never appears in canonical state, in a hash input, in a log, in
diagnostics or in a qualification artifact. The `KeyContainer` interface is
provider-neutral; Android Keystore is one implementation of it and is not
canonical authority.

---

## 14. Identity binding, backup exclusion, and quarantine

### 14.1 Backup and transfer exclusion

Canonical database, journals, checkpoints, identity metadata, wrapped keys,
recovery and transfer state, and canonical preferences are excluded from Android
Auto Backup and from automatic device-to-device transfer for **every** supported
API level:

- `android:allowBackup="false"` explicitly.
- `android:fullBackupContent` rules for API 30 and lower.
- `android:dataExtractionRules` with both `cloud-backup` and `device-transfer`
  sections for API 31 and higher.

Non-canonical preferences live in a separate store and may be backed up only when
explicitly included.

### 14.2 Identity record

Canonical identity records at minimum: organism ID, identity epoch, lineage hash,
and active-device public-key fingerprint.

### 14.3 Quarantine

`COPIED_STATE_QUARANTINE` is entered when canonical state is present but the
device key container cannot unwrap it, or the recorded device fingerprint does
not match the local one. A quarantined copy does **not** boot as a second valid
organism, does not advance any clock, and does not emit canonical events. It
exposes only explanation and recovery UI.

### 14.4 Recovery freshness

A scheduled or background upload does not advance the protected recovery point.
Freshness advances only after the provider confirms the package **and** the
application verifies receipt identifier, expected size, checksum and sequence.

| Parameter | Frozen value |
|---|---|
| `RECOVERY_STALE_WARNING_MILLIS` | `86400000` (24 hours) |
| `RECOVERY_CRITICAL_WARNING_MILLIS` | `604800000` (7 days) |

`RecoveryPackageStore` is a provider-neutral interface. With no production
provider selected, every provider-specific implementation is
`BLOCKED_SPEC_RECOVERY_PROVIDER`, and recovery cryptography is blocked behind the
unfrozen `RecoveryCryptographyContractV1`. Current-device correctness does not
depend on either.

---

## 15. Restart, recovery and version boundaries

### 15.1 Restart

```
last durably acknowledged anchor
→ verify checkpoint hash and journal coverage
→ replay bounded transitions after the checkpoint
→ classify elapsed evidence (section 4)
→ reconcile (section 7)
→ commit
→ open the interaction gate
```

Canonical work after the last durable acknowledgement is **discarded**. Because
Class W visibility is gated, discarding it cannot erase a mutation already shown
to the user. That is the property qualification must prove, and the maximum
rollback exposure must contain no user-visible material mutation.

### 15.2 Recovery never invents

Recovery restores the last valid durable state. It does not interpolate missing
history, fabricate events, refund unproven items, or forgive verified metabolism.
Where a gap is unknowable it is labelled as such and left empty.

### 15.3 Version boundaries

Unchanged from `DeterminismContractV1` section 10 and charter section 15: an old
journal is never replayed through a newer behavior engine; the old-version state
is reconstructed with its original decoder, hash-verified, checkpointed, then
migrated by a pure function that runs no behavior logic and draws no randomness;
a new generation starts under the new engine contract. Unknown and future
versions are refused. Downgrade is prohibited.

---

## 16. Canonical events added by this contract

| Event | Class | Meaning |
|---|---|---|
| `ClockAnomalyDetected` | O | Same-boot skew exceeded tolerance |
| `BlindCreditConsumed` | W | Blind decay applied and credit durably debited |
| `UnresolvedTimeDebtAccrued` | O | Uncertainty recorded, with forgiven excess |
| `MetabolicAdjustmentApplied` | O | Bounded debt collection inside an eligible chunk |
| `DebtPausedLowReserve` | O | Collection paused at the safety floor |
| `DebtForgiven` | O | Budget expired past the retention horizon |
| `ExtendedAbsenceReconciled` | O | Mode C completion |
| `DurabilitySafeHoldEntered` | W | Entry into a durability hold |
| `DurabilitySafeHoldExited` | W | Exit from a durability hold |
| `PlatformDeepSuspendEntered` | W | Bounded suspend anchor |
| `PlatformRecoveryCompleted` | W | Recovery committed |
| `RecoveryPerformed` | W | Restart recovery committed |
| `RecoveryGapDeclared` | W | Known unavailable interval declared |
| `SnapshotCreated` | W | Verified checkpoint installed |
| `MigrationPerformed` | W | Pure schema migration committed |

All are recorded in `EventCatalog` with immutable IDs.

---

## 17. What this contract does not decide

- Any physiological rate, threshold, equation or consequence.
- Which reserves an organism has, or what they mean.
- Any UI layout, animation, copy string or asset.
- Room, SQLite, file formats, or any concrete storage engine.
- Recovery cryptography, the mnemonic encoding, the KDF, or the identity-epoch
  protocol — `RecoveryCryptographyContractV1`, not frozen.
- The production recovery storage provider — `BLOCKED_SPEC_RECOVERY_PROVIDER`.
- Anything in `DeterminismContractV1`, which this contract inherits unchanged.

---

## 18. Decision record

### 18.1 Milliseconds as the canonical duration unit

Considered: `Fixed64` seconds, nanoseconds, and a dedicated duration type.
Nanoseconds overflow `i64` at ~292 years, which is inside the range a "one year
absence" fixture plus a lineage age can reach when summed. `Fixed64` seconds
would put durations into saturating arithmetic where a clamp is silently
plausible; a duration that saturates should be a loud failure, not a clamp.
Milliseconds as plain `i64` gives ~292 million years of range, matches every
Android time API, and keeps durations outside the saturating domain entirely.

### 18.2 Derived rather than random nonces

Considered: a random 96-bit nonce per record, and a random 192-bit nonce with
XChaCha20. Random nonces require a CSPRNG in the durable write path, which is a
provider dependency of exactly the kind R001 eliminated, and they make a fixture
irreproducible. The single-writer log already has a monotonic sequence, so
derivation gives a strictly stronger uniqueness guarantee — structural rather
than probabilistic — at no cost. The key-epoch prefix closes the only remaining
reuse path.

### 18.3 ChaCha20-Poly1305 over AES-GCM

Considered: AES-256-GCM via `javax.crypto`, AES-GCM-SIV, and ChaCha20-Poly1305.
AES-GCM through a provider reintroduces the provider-resolution problem and, on
devices without hardware AES, is both slower and harder to implement in constant
time in pure Kotlin. A table-driven pure-Kotlin AES would introduce exactly the
kind of generated-table determinism surface that section 11 of
`DeterminismContractV1` exists to control. ChaCha20-Poly1305 is add-rotate-xor
only, has no tables, has published RFC 8439 vectors, and can be differentially
checked against `javax.crypto` on every target without depending on it.

### 18.4 Carried remainder in credit replenishment

Considered: truncating division, rounding to nearest, and rational accumulation.
Truncation lets an adversary farm a systematic loss by fragmenting sessions;
rounding to nearest lets them farm a systematic *gain* the same way. A carried
remainder makes the grant a pure function of total verified time regardless of
how it was divided, which is the only variant with no fragmentation strategy.

### 18.5 Debt cap as biology, not as a time ledger

Considered: capping elapsed time, capping collected adjustment, and capping both.
Capping elapsed time makes the cap's meaning depend on the species rate, so a
later physiology change would silently alter an already-qualified safety
property. Capping the biological-equivalent budget keeps the guarantee — "at most
72 hours of baseline metabolism can ever be owed" — stable across every future
rate change.

### 18.6 Forgiveness at accrual rather than at collection

Considered: retaining excess in a shadow ledger and forgiving it later.
Retention means a long absence creates a liability the user cannot see and cannot
discharge, which is the delayed punishment bomb the canonical architecture
prohibits by name. Forgiving at accrual makes the outstanding balance always
equal to what could actually be collected.

### 18.7 Chunk boundaries measured from the start of absence

Considered: measuring from the end, and adapting chunk size to remaining time.
Both make the chunk sequence depend on the total, so two absences that overlap in
real time reconcile the same interval with different chunk boundaries and
different rounding — a cross-fixture determinism failure. Measuring from the
start makes chunk boundaries a property of the timeline, not of the query.

### 18.8 Discarding rather than queueing gated inputs

Considered: queueing inputs received while the interaction gate is closed.
A queued input is applied against a canonical state the user never saw, which
breaks the correspondence between what was on screen and what was intended.
Discarding is the only option that keeps "the creature received this" honest, and
it is why rejected inputs must also consume no item.

### 18.9 Retiring `STASIS_PROJECTION` rather than reusing its ordinal

The canonical architecture superseded stasis presentation with `TEMPORAL_DESYNC`
for a substantive reason: stasis implies biology stopped, and biology did not
stop. Reusing ordinal `1` for the replacement would make an old durable record
decode as the new meaning. The ordinal is retired.

### 18.10 A single normative ordering for each hold and suspend

Considered: describing the orderings as guidance and letting the implementation
choose. Every one of these orderings encodes an anti-exploit — debit before
reveal, anchor before release, commit before reopen — and an implementation free
to reorder them is free to reintroduce the exploit. They are normative, they are
tested boundary by boundary, and a reordering is a contract violation rather than
a refactor.

### 18.11 Freezing the three parameters R001 left unestablished

R001 recorded `CLASS_O_COMMIT_CADENCE_MILLIS`, the maximum uncommitted window and
the panic-witness attempt deadline as `NOT ESTABLISHED` because no evidence
existed. R002 now has both the canonical band (250–1000 ms, stated in the charter)
and the R001 measured panic-witness p99 of 16,215 ns. The values in section 8 are
taken from those two sources rather than invented, and each is
`QUALIFICATION_TARGET` rather than `FROZEN_INVARIANT` so that device evidence can
move them without a contract break.

### 18.12 Neutral fixture reserves instead of early physiology

Considered: implementing a minimal energy variable so that debt collection has
something to act on. That is an R003 decision gated behind A001, and a "minimal"
physiological variable chosen for the convenience of a durability test is exactly
how an unqualified organism assumption becomes load-bearing. `reserveA` and
`reserveB` carry no meaning, and every threshold expressed against them is a
fraction of capacity rather than a species value.
