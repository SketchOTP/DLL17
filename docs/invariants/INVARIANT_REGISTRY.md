# Invariant registry

Invariants may not be tuned away. Each entry names the invariant, its source and
where it is enforced today.

| ID | Invariant | Source | Enforcement |
|---|---|---|---|
| INV-0001 | Core modules never link the Android framework. | Implementation Plan E2E, section 2 | Enforced by build wiring and by a classpath test in each `core-*` module. |
| INV-0002 | The canonical reducer is single-threaded. | Implementation Plan E2E, `ThreadAndDispatcherMap` | `CanonicalReducer` is a pure function with no synchronization and no background work. Purity is asserted by test. |
| INV-0003 | No executable logic depends on a number that is absent from `ParameterRegistry`. | Implementation Plan E2E, section 1 | Every R001 determinism constant is registered, including the two this phase chose. No organism parameter exists. |
| INV-0004 | Dependency versions are exact pins; dynamic versions are prohibited. | `ProjectIdentityBuildContractV1` | Enforced by `tools/verify_project_identity.py`. |
| INV-0005 | No organism implementation or state is inherited into this repository. | Implementation Plan E2E, R000 exit gate | Repository is greenfield; Git provenance begins at D-003 with no imported source. R001 copied no third-party source. |
| INV-0006 | Canonical arithmetic never uses `Float`, `Double` or a native transcendental function. | Canonical architecture, determinism boundary | `FixedPoint` is integer-only; a structural test asserts the compiled class exposes no float or double member and does not reference `java.lang.Math`. |
| INV-0007 | Production arithmetic never throws because a value overflowed. | Canonical architecture, production arithmetic | Every saturating operation clamps and emits a diagnostic; asserted across all four extremes of the range. |
| INV-0008 | `reduce(s0, orderedEvents)` equals `replay(s0, durableJournal)` in canonical bytes and state hash. | Charter 2026-08-07, R001 non-negotiable assertions | Asserted for every qualification fixture, comparing bytes and hex digests rather than object equality. |
| INV-0009 | No Class W material mutation becomes visible before its commit frame is durably acknowledged. | Charter, commit visibility invariant | Enforced by `GatedPresentationSink`, which refuses the call; verified at all eight crash boundaries. |
| INV-0010 | A Class W final semantic presentation happens at most once, including across recovery. | Charter 2026-08-04, at-most-once reaction | The presentation token is consumed when the durable write starts; recovery emits no presentation at all. |
| INV-0011 | Adding a random domain cannot change the output of any existing substream. | Canonical architecture, randomness contract | Structural: a substream seed is a pure function of `(masterSeed, contractVersion, domainId)`. Verified by insertion test. |
| INV-0012 | `PlatformPanicWitness` is noncanonical, excluded from state hashes, and safe to lose entirely. | Charter, panic-witness boundedness | Verified structurally: its bytes do not appear in canonical serialization, and replay is identical with and without it. |
| INV-0013 | An assisted payload can never participate in the physics solver. | Charter, assisted-payload physics invariant | Enforced by the type system: `AssistedPayload` declares both physics properties `final` at `false`; asserted by reflection. |
| INV-0014 | An artifact from an unknown or future contract or schema version is refused, never best-effort parsed. | `DeterminismContractV1` section 12.5 | Decoder and migration both refuse; asserted by test. |
| INV-0015 | Canonical state contains no unordered collection and no natural-language text. | `DeterminismContractV1` sections 4 and 5 | Canonical maps sort by serialized key bytes; identifiers are restricted to an ASCII subset. |
| INV-0016 | Wall-clock movement alone can never grant reward-bearing elapsed time. | Canonical architecture, trusted elapsed-time model | Only `VERIFIED_MONOTONIC` and `AUTHENTICATED` intervals produce verified time; a wall-clock delta may only ever *reduce* a blind-credit request. |
| INV-0017 | Blind progression never advances verified time, development, learning, relationships, evolution or cooldown. | Canonical architecture, rebooted or unverifiable intervals | `BLIND_CREDIT_CONSUMED` advances chronology, circadian phase and reserves only; asserted by test. |
| INV-0018 | Repeated reboot cannot generate additional progression. | Canonical architecture, R002.2 | Credit is keyed by boot identity, replenishes only from verified time, and a boot-velocity anomaly forces zero further blind decay. |
| INV-0019 | Outstanding uncertainty debt is globally capped and always eventually discharged. | Canonical architecture, trusted-time debt reconciliation | Excess is forgiven at accrual rather than retained; anything uncollected past the retention horizon is forgiven with audit evidence. |
| INV-0020 | Debt collection never crosses the reserve safety floor and never rearms on the care event that restored the reserve. | Canonical architecture, `DebtPausedLowReserve` | Every substep is projected before it is applied; rearm requires abundance held for the stability interval and takes effect only after the grace interval, in a later eligible chunk. |
| INV-0021 | Returning to the foreground never increases biological burn. | Canonical architecture, offline amortization | Debt adjustment is structurally impossible in Mode A and in every foreground path; asserted by test. |
| INV-0022 | A reconciliation interrupted at any chunk resumes to the identical event sequence. | Canonical architecture, time slicing | The cursor is the only mutable state and every chunk is a pure function of the state before it; asserted at five slice granularities. |
| INV-0023 | Inputs received while the interaction gate is closed are discarded, never queued, and consume nothing. | Canonical architecture, interaction gating during reconciliation | `InteractionGate` refuses and counts; a queued input would be applied against a state the user never saw. |
| INV-0024 | Recovery begins at the last durably acknowledged record and invents nothing. | `ContinuityDurabilityContractV1` section 15 | A torn tail is skipped as never-acknowledged; a structurally complete record that fails authentication is a `STORAGE_FAULT`, never a silent skip. |
| INV-0025 | A failed checkpoint install prunes nothing. | Canonical architecture, retention and double-buffered checkpoint model | The candidate is built and verified outside the writer; pruning happens only after the install is acknowledged. |
| INV-0026 | Once a durability hold is durable, no canonical state advances in memory. | Canonical architecture, `READ_ONLY_SURVIVAL` | Publication stops at the committed hold anchor; only non-canonical repair and navigation UI remains. |
| INV-0027 | A durability hold or platform suspend never skips elapsed biology and never depicts autonomous life. | Canonical architecture, `TEMPORAL_DESYNC` and reconnect | The held interval is reconciled through the ordinary trusted-time rules on exit; `DurabilityPresentation` refuses autonomous-life output during a hold. |
| INV-0028 | At most one deep-suspend anchor attempt and at most one panic-witness attempt are made, and both may fail. | Charter, platform deep suspend | No retry loop exists under thermal or power pressure; the last durable anchor stays authoritative either way. |
| INV-0029 | An AEAD nonce is never reused within a key epoch. | `ContinuityDurabilityContractV1` section 13.3 | The nonce is derived from `(keyEpoch, durable sequence)` and a sequence is consumed even by a failed write, so reuse is structurally impossible. |
| INV-0030 | Canonical determinism does not depend on any key. | `ContinuityDurabilityContractV1` section 13.1 | Encryption sits below the canonical byte layer; state hashes and replay operate on plaintext canonical bytes. |
| INV-0031 | A copied canonical database does not boot as a second organism. | Canonical architecture, device cryptographic binding | Device binding is checked before any event is folded, and a quarantined state accepts no canonical event at all. |
| INV-0032 | Canonical state is excluded from Android backup and device transfer at every supported API level. | Canonical architecture, Android backup isolation | `allowBackup=false` plus both `fullBackupContent` and `dataExtractionRules`, with cloud-backup and device-transfer sections; asserted structurally. |
| INV-0033 | R002 introduces no randomness. | `ContinuityDurabilityContractV1` section 7.3 | Reconciliation, recovery and migration are pure functions; the encrypted write path derives its nonce rather than drawing one. |

| INV-0034 | No spike source imports any production package except the frozen R001 fixed-point library. | Canonical research/production inheritance boundary | `SpikeIsolationTest` reads every spike source's import list and fails on anything outside `com.animusmachinae.dll17.core.math`. |
| INV-0035 | No production module reaches into the research track. | Canonical research/production inheritance boundary | `SpikeIsolationTest` scans every production module's sources and build files for references to the aliveness spike. |
| INV-0036 | The presentation layer cannot see which controller produced a frame. | `SpikeExpressionContractV1` | `SpikeExpressionContract` has no cohort parameter; the viewer's rendering classes have no reference to `Cohort` or `Mechanism`; both are asserted by source scan. |
| INV-0037 | A viewer session never exposes its cohort. | Canonical blinding requirement for the primary interaction model | `ViewerSession` has no accessor at all, asserted by reflection over the class surface; every cohort shares one label, one duration and one tick rate. |
| INV-0038 | A losing or tied coalition contributes exactly zero attribution mass. | `CoalitionValueFunctionV1` | `v(S) = max(0, margin(S))` with equality at zero counting as a tie; tie-breaking is held outside the utility game entirely. |
| INV-0039 | Spontaneity attribution uses exact enumeration, never sampling. | `MechanismCoalitionSetV1` | Six frozen groups, `2^6 = 64` coalitions evaluated per scored action; a seventh group would require a separate reviewed approximation contract. |

| INV-0040 | Every re-sampling of a previously rejected option has a citable non-random cause. | `MechanismRevisionD009` | Directed exploration is driven by per-option outcome uncertainty and prediction error; the only random domain reaches selection at near-equal utility and is recorded separately in the trace. |
| INV-0041 | No single action or object can take an unbounded share of the organism's time. | `MechanismRevisionD009` | Three independent bounds: action satiation, a per-object engagement refractory, and a metabolic cost for vigorous activity. |
| INV-0042 | A mechanism that does not measurably earn its place is removed from the candidate, not retained. | Canonical Principle 11 | Episodic history was revised, measured over a five-seed matrix, and removed when the revision still did not contribute. |

| INV-0043 | The quantity released from a non-scored pilot to the team that runs the scored study must be invariant to the direction of the pilot's result. | `BlindVariancePilotV1` | Enforced by type visibility and proven by two pilots with opposite outcomes releasing byte-identical output. |
| INV-0044 | A study prerequisite that is missing names its own blocking state; the activation gate is derived from the prerequisites and never declared beside them. | `AlivenessGovernanceAuditV2` | A declared gate drifts permissive. `GovernanceAuditTest` proves the derivation opens and closes correctly. |
| INV-0045 | A programme success floor may become stricter or stay equivalent after an attempt begins. It may never become easier. | `AlivenessProgramGateV1` | Frozen at +10.0 points with a CI condition before any human data existed, and pinned by test. |
| INV-0046 | Synthetic fixture data may demonstrate that an analysis works. It may never be recorded, cited or bundled as scientific evidence about the organism. | D010 | Every dry-run scenario is marked `SYNTHETIC`, and the output is filed under research evidence rather than under qualification. |

| INV-0047 | A cryptographic or key-container failure never produces a fresh key or an empty journal. It refuses or quarantines. | `LocalStorageCryptographyContractV1` | A fresh key orphans every existing record and the next startup looks like a birth. |
| INV-0048 | Structural damage to a durable frame is corruption wherever it appears; only a shortfall of bytes at the end is a torn tail. | `PersistenceBackendContractV1` | A partial write cannot forge a magic or a length. |
| INV-0049 | A staging file becomes authoritative only through an atomic rename; an unrenamed one is discarded, never adopted. | `PersistenceBackendContractV1` | Applies to journal compaction, checkpoints and key state alike. |
| INV-0050 | Identity may not move backwards. History may. | `RecoveryCryptographyContractV1` | A package below the activated epoch is refused, or a superseded device could reclaim the organism with an old backup. |
| INV-0051 | Recovery never reruns old behaviour to reconstruct history; a gap is declared, never filled in. | `RecoveryCryptographyContractV1` | The package carries a checkpoint and a journal tail, not a script to replay. |
| INV-0052 | No storage provider is canonical authority beyond confirming the bytes and sequence of a package, and ordinary local operation never depends on one. | `RecoveryPackageStoreContractV1` | Enforced by qualification with the provider refusing every call. |
| INV-0053 | The identity authority stores identity metadata only, and normal local runtime never calls it. | `IdentityAuthorityProtocolV1` | Enforced structurally: the service is outside the organism core dependency graph and a test asserts its absence from the core classpath. |
| INV-0054 | Canonical serialized bytes and canonical state hashes are independent of backend, key, ciphertext, nonce, storage location, recovery package and provider. | `DeterminismContractV1` | Encryption sits below the canonical byte layer; qualification proves the hash is identical under different keys and different ciphertext. |
| INV-0055 | Opening a device key container never generates missing material, and an organism may be created only when both the key state and the container material are absent. | `LocalStorageCryptographyContractV1` | Enforced by `AndroidLocalKeyBootstrap` and its decision-table test; every other combination opens or quarantines. |
| INV-0056 | Canonical state on Android lives only in app-private storage under a path the shipped backup and device-transfer rules exclude. | `ContinuityDurabilityContractV1` s14.1 | Enforced by `AndroidPersistenceLocations`, by a layout-coverage fixture, and by `tools/verify_backup_exclusion.py` reading the built package. |
| INV-0057 | An ordinary rotation of the device wrapping material rewrites no journal byte and leaves every already-written record readable. | `LocalStorageCryptographyContractV2` | Enforced by `FX-EPOCH-ROTATE-ONCE-01`, `FX-EPOCH-ROTATE-MANY-01`, `FX-EPOCH-NO-REWRITE-01` and `DV-KS-ROTATION-READBACK-01`. |
| INV-0058 | A record's encryption context is immutable, is bound into its AAD, and is the only key identity used to read it. The container's current wrapping epoch never decides whether a record may be read. | `LocalStorageCryptographyContractV2` | Enforced by `FX-EPOCH-CONTEXT-AUTHENTICATED-01` and `FX-EPOCH-FOREIGN-KEY-01`; four forged header fields refused, foreign data key refused. |
| INV-0059 | Migrating key state from V1 to V2 is deterministic, idempotent and crash-safe, and recovers either the readable pre-migration state or the complete migrated state — never a half-state. | `LocalStorageCryptographyContractV2` | Enforced by `FX-V1-MIGRATION-IDEMPOTENT-01` and the two `Runtime.halt` boundary fixtures `FX-V1-MIGRATION-CRASH-STAGED-01` and `FX-V1-MIGRATION-CRASH-RENAMED-01`. |
| INV-0060 | A recovery provider receives encrypted package bytes and the contract's object name, and nothing else. No canonical plaintext, no key and no organism content crosses the boundary. | `RecoveryPackageStoreContractV1` | Enforced by `FX-NET-PRIVACY-PAYLOAD-01`, which plants a plaintext canary and searches what the provider holds, and `FX-NET-PRIVACY-METADATA-01`, which searches every request line the endpoint observed. |
| INV-0061 | An outage of the recovery provider or of the identity authority leaves ordinary local life untouched, and is never reported as a refusal. | `RecoveryPackageStoreContractV1`, `IdentityAuthorityTransportContractV1` | Enforced by `FX-NET-PROVIDER-OUTAGE-LOCAL-LIFE-01` and `FX-NET-AUTHORITY-OUTAGE-LOCAL-STATE-01`, both against a genuinely unreachable socket. |
| INV-0062 | The transport carries no protocol decision of its own: every epoch, challenge, lease, replay, rate-limit and idempotency outcome is the protocol's, unchanged over HTTP. | `IdentityAuthorityTransportContractV1` | Enforced by the nine `FX-NET-AUTH-*` fixtures, which re-qualify over HTTP every property D011 qualified in process. |
| INV-0063 | The identity epoch advances at most once per activation however a request is duplicated, retried or raced, and a restarted authority still holds the epoch it granted. | `IdentityAuthorityProtocolV1` | Enforced by `FX-NET-AUTH-DUPLICATE-ACTIVATE-01`, `FX-NET-AUTH-RACE-01` (two threads, two sockets, one winner) and `FX-NET-AUTH-RESTART-01`. |
| INV-0064 | Canonical bytes and hashes are independent of the provider, bucket, object key, ETag, request id, HTTP status, region, hostname, retry count and network ordering. | `DeterminismContractV1` | Enforced by the three `FX-NET-DETERMINISM-*` fixtures; a restored organism's canonical state hash equals its source's across two different object keys. |

### D014 note

INV-0060 through INV-0064 constrain the network boundary: what may cross it, what
an outage may do, and what the transport is forbidden to decide. None asserts
anything about organism behaviour, and none of them makes a claim about a
deployed service — see `governance/release-gates/R014_NETWORK_GATE.md`, section
"Not closed by this gate".

### R002 note

INV-0016 through INV-0033 are continuity invariants. None of them is a
physiological rule: they constrain what elapsed time, storage failure and
platform protection may do to canonical history, not what the organism is.

### A000 note

INV-0034 through INV-0039 are research-track invariants. They constrain the
boundary between disposable research and production, and the integrity of the
blinding and attribution machinery. None of them asserts anything about organism
behaviour, and none of them is evidence that a mechanism works.

### D009 note

INV-0040 through INV-0042 constrain the A000 research candidate. They are not
production invariants and do not authorize any R003–R009 mechanism.

### D013 note

INV-0057 through INV-0059 constrain the corrected local-storage cryptography.
They are the invariants D012's `DV-KS-ROTATION-READBACK-01` was reporting the
absence of. None asserts anything about organism behaviour.

### D012 note

INV-0055 and INV-0056 constrain the Android half of the R012 substrate: what may
justify creating an organism on a device, and where canonical state may live.
Both are about refusing an action, not about organism behaviour.

### D011 note

INV-0047 through INV-0054 constrain the R012 persistence, recovery and identity
substrate. They are production invariants and they bind the implementation, but
none of them asserts anything about organism behaviour: no R003 through R009
mechanism exists.

### D010 note

INV-0043 through INV-0046 constrain the A001 study machinery: what may cross the
variance-pilot barrier, how the activation gate is computed, which direction a
success floor may move, and what synthetic data may be used for. None of them
asserts anything about the organism, and none is evidence that it works.

