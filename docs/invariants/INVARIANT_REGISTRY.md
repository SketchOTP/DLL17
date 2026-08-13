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
