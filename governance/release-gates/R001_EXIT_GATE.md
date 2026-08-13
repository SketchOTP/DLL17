# R001 exit gate

- Phase: R001 — deterministic fixed-point spike (HARD PROOF GATE)
- Evaluated under: architect directive D006
- Evaluation date: 2026-08-13
- Determinism contract: `DeterminismContractV1` version 1, `FROZEN`
- Golden evidence digest:
  `54bc044740a4c05b41b509a7160bff559e09421f2eaa55dc36c3d3ffadc1bd86`

Three canonical documents define an R001 exit gate, and they are not identical.
All three are evaluated below rather than whichever is most convenient, because
none supersedes the others and passing only one would be a partial claim.

---

## Gate A — Implementation Plan E2E, "R001 exit gate"

| # | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | Fixed-point property/boundary tests pass | `PASS` | `FixedPointOracleTest`: every operation checked against a `BigInteger` oracle across a full operand sweep including both bounds, rounding ties and half-bound values |
| 2 | No production arithmetic throws on overflow | `PASS` | All four range extremes exercised through every operation; none throws |
| 3 | Serialization is stable/versioned | `PASS` | Encode/decode/re-encode is byte-stable over five cycles per fixture; envelope carries format, contract and schema versions |
| 4 | State hashes match across required targets | `PASS` — see waiver | Desktop JVM, x86_64 Android emulator and Tensor G4 all produced the identical golden digest |
| 5 | Direct reducer execution equals journal replay for every fixture | `PASS` | `ReplayEquivalenceTest`: byte identity, not object equality, for all five fixtures |
| 6 | Random-domain insertion leaves existing streams unchanged | `PASS` | Insertion test plus the structural property that a substream seed is a pure function of `(masterSeed, contractVersion, domainId)` |
| 7 | Crash/recovery restores exactly to last acknowledged CommitFrame | `PASS` | All eight named boundaries swept; recovery reads only the durable medium |
| 8 | Presentation cannot observe an uncommitted material mutation | `PASS` | `GatedPresentationSink` refuses the call; verified at every pre-acknowledgement boundary |
| 9 | Panic witness cannot affect replay/state hash | `PASS` | Its bytes are absent from canonical serialization; replay is identical with and without it |
| 10 | Assisted payload interface proves zero dynamic physics participation | `PASS` | Both physics properties are `final false` on `AssistedPayload`; asserted by reflection |
| 11 | Intended operating ranges do not saturate | `PASS` | Zero saturations across every fixture except `FX-BOUNDARY-01`, which exists to saturate deliberately |

## Gate B — Digital Living Lifeform charter, section 18

| # | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | Identical state hashes on every target architecture | `PASS` — see waiver | `x86_64` and `arm64-v8a`, HotSpot and ART, all identical |
| 2 | Property tests pass at numeric boundaries | `PASS` | Boundary operands are explicitly in the oracle sweep |
| 3 | No production arithmetic throws on overflow | `PASS` | As Gate A criterion 2 |
| 4 | Saturation is absent from intended normal operating ranges | `PASS` | As Gate A criterion 11 |

## Gate C — charter amendment 2026-08-07, "R001 exit gate"

| # | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | Fixed-point property and boundary tests pass | `PASS` | As Gate A criterion 1 |
| 2 | Canonical serialization is stable and versioned | `PASS` | As Gate A criterion 3 |
| 3 | Identical normalized-event fixtures produce identical canonical hashes across every required target | `PASS` — see waiver | Determinism matrix |
| 4 | Journal replay equals direct reducer execution for all qualification fixtures | `PASS` | As Gate A criterion 5 |
| 5 | Process death between any two committed frames recovers exactly to the last durable canonical state | `PASS` | Eight-boundary sweep, with an acknowledged/unacknowledged assertion at each |
| 6 | No uncommitted material mutation is visible to the user | `PASS` | As Gate A criterion 8 |
| 7 | Random-domain insertion tests demonstrate substream isolation | `PASS` | As Gate A criterion 6 |
| 8 | Saturation is absent from intended normal operating ranges | `PASS` | As Gate A criterion 11 |

---

## The one criterion that rests on an architect decision

Gate A criterion 4, Gate B criterion 1 and Gate C criterion 3 are the same
requirement: identical canonical hashes across **every required target**. The
canonical determinism matrix names five targets.

| Canonical target | Result |
|---|---|
| Desktop JVM reference runner | `PASS` |
| x86 Android emulator | `PASS` |
| Tensor Android hardware | `PASS` |
| Snapdragon Android hardware | **`WAIVED BY ARCHITECT`** |
| Exynos Android hardware | `CONDITIONAL` — canonical text says "when available", and none was |

D006 instructed: *"If a required R001 target cannot be qualified, do not weaken
the matrix."* Snapdragon is listed without the `when available` qualifier that
Exynos carries, so on the directive's text alone this criterion would be
`BLOCKED` and R001 would not close.

During D006 execution the architect was asked directly and answered that the
Pixel 9 hardware is sufficient and that Snapdragon is not required. That is an
architect decision, and it is recorded here as one rather than absorbed:

- The Snapdragon row reads `WAIVED BY ARCHITECT`, **not** `PASS`. No claim is
  made that Snapdragon silicon executed anything.
- The canonical architecture page still lists Snapdragon as required. A waiver
  given in a working session is not an amendment to a frozen specification.
  Until the canonical page is amended, the matrix and the specification
  disagree, and closing that disagreement is the architect's act, not the
  implementer's.

**If the architect intended D006's "do not weaken the matrix" instruction to
survive that answer, this criterion is `BLOCKED`, all three gates reopen, and
R001 does not close.** The evidence for every other criterion is unaffected and
would stand.

## What the passing targets actually cover

The three qualified targets are not variations of one environment:

| Axis | Coverage |
|---|---|
| Runtime | HotSpot JVM **and** Android ART |
| Instruction set | `x86_64` **and** `arm64-v8a` |
| Silicon | Desktop x86, emulated x86, Google Tensor G4 |

Byte identity therefore holds across a JVM-to-ART boundary and an x86-to-ARM
boundary simultaneously. Those are the two boundaries most likely to expose a
floating-point dependency, an endianness assumption, an API-level-dependent
intrinsic, or a hash-iteration-order leak. None appeared.

## Outstanding, and not counted as gate criteria

Recorded so a later reader is not misled about what R001 did and did not settle.

1. **No Snapdragon or Exynos evidence exists.** The waiver removes the
   requirement to test; it does not create a result.
2. **The panic-witness attempt deadline is `NOT ESTABLISHED`.** R001 measured the
   write cost on two targets, as the architect's correction requires, and
   deliberately did not convert a healthy-process benchmark into a deadline that
   has to hold during a real critical suspend.
3. **Class O batching cadence and the maximum tolerated uncommitted window are
   `NOT ESTABLISHED`.** E2E R001.9 says qualification freezes them. Both are
   properties of real persistence on a real device, and R001's durable medium is
   an in-process byte log by design. They belong to R002.
4. **R001's durable medium is not a database.** R001 owns the durability
   *contract*; persistence *semantics* are R002's `ContinuityDurabilityContractV1`.
   Every R001 invariant is provable against an append-only byte log, and
   implementing storage semantics here would have pre-empted a later phase.
5. **The repository is still public while carrying a proprietary licence**, and
   the historical MIT grant remains published for revisions before `43054d0`.
   Unchanged from R000; a legal question for the copyright holder.

---

**R001 = PASS**, contingent on the architect's Snapdragon waiver recorded above.
