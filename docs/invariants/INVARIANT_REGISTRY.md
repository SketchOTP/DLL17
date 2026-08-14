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

### D010 note

INV-0043 through INV-0046 constrain the A001 study machinery: what may cross the
variance-pilot barrier, how the activation gate is computed, which direction a
success floor may move, and what synthetic data may be used for. None of them
asserts anything about the organism, and none is evidence that it works.

