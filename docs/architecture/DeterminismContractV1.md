# DeterminismContractV1

- Contract ID: `DeterminismContractV1`
- Contract version: `1`
- Status: `FROZEN`
- Frozen under: directive D006, phase R001
- Owner gate: R001. Changing any frozen clause requires a new contract version,
  an architect amendment, new golden vectors, and retention of the old vectors.

This artifact freezes, by exact algorithm and format identifier, every decision
that determines whether two runs of this organism produce the same canonical
bytes. It is the artifact that Implementation Plan E2E work package R001.0
requires to exist **before** any production serializer, hash or PRNG code is
written. Nothing in `core-math`, `core-crypto` or `core-state` may select a
determinism-relevant algorithm that is not named here.

The candidate evaluation behind each choice — cross-JVM/Android reproducibility,
performance, licensing and provenance, test-vector availability, and migration
implications — is recorded in section 12 and in `docs/decisions/DECISION_LOG.md`
entries `IMPL-0007` through `IMPL-0014`.

---

## 1. Canonical byte order

| Clause | Value |
|---|---|
| Byte order for every multi-byte integer | Big-endian (network order) |
| Mixed endianness | Prohibited |

There is exactly one byte order in the canonical format. No field, header,
length prefix, hash input or PRNG state uses little-endian.

**Why it is frozen this way.** Big-endian is the only order for which the
written bytes are lexicographically comparable in the same direction as the
numeric value for unsigned quantities, which makes the canonical map ordering
rule in section 4 implementable as a plain byte comparison. It is also the JVM's
own `DataOutput` convention, so the reference implementation and any independent
verifier written against `java.io.DataInputStream` agree without adaptation.

---

## 2. Integer widths and length-prefix encoding

### 2.1 Integer widths

| Canonical type | Width | Encoding |
|---|---|---|
| `i8` | 1 byte | Two's complement |
| `i16` | 2 bytes | Two's complement, big-endian |
| `i32` | 4 bytes | Two's complement, big-endian |
| `i64` | 8 bytes | Two's complement, big-endian |
| `u8` | 1 byte | Unsigned |
| `u16` | 2 bytes | Unsigned, big-endian |
| `u32` | 4 bytes | Unsigned, big-endian |

Every canonical integer field declares exactly one of these widths in
`CanonicalStateCatalog`. A field's width is part of its identity and may not
change without a schema version bump and a migration.

### 2.2 Length prefixes

| Clause | Value |
|---|---|
| Length prefix encoding | `u32` big-endian, fixed 4 bytes |
| Maximum length | `2_147_483_647` (`Int.MAX_VALUE`) |
| Variable-length integers (LEB128, protobuf varint, zigzag) | **Prohibited** |

**Why it is frozen this way.** Variable-length integer encodings admit
non-minimal encodings: the value `1` can be written as `0x01` or as `0x81 0x00`
in most varint schemes, and both decode to `1`. Two encoders that disagree about
minimality produce different bytes for identical state, which would silently
break the byte-identity requirement that the entire R001 gate rests on. A fixed
4-byte prefix has exactly one representation per value. The cost is four bytes
per collection instead of one to two; the benefit is that byte identity is a
property of the format rather than a property of implementation discipline.

---

## 3. Boolean and enum encoding

| Canonical type | Encoding | Illegal input |
|---|---|---|
| `bool` | `u8`: `0x00` false, `0x01` true | Any other byte is a decode fault |
| `enum` | `i32` big-endian, the immutable numeric ordinal from the owning registry | An unregistered ordinal is a decode fault |

Enum ordinals are **immutable numeric identifiers assigned in the owning
registry**, never the Kotlin declaration order and never the enum constant name.
Reordering a Kotlin `enum class` must not be able to change canonical bytes.
Removing an enum case retires its ordinal permanently; the ordinal is never
reused.

A decode fault is a hard failure. The decoder never guesses, never coerces a
non-`0x00`/`0x01` byte to a boolean, and never maps an unknown ordinal to a
default case.

---

## 4. Collection ordering rules

| Collection kind | Canonical encoding | Ordering rule |
|---|---|---|
| Canonical sequence (ordered list) | `u32` count, then each element | Declared order is preserved and is itself canonical |
| Canonical map | `u32` count, then each entry as (key bytes, value bytes) | Entries sorted **ascending** by unsigned lexicographic comparison of the serialized key bytes |
| Unordered set or map (`HashSet`, `HashMap`, `Set`, `Map`) | **Prohibited in canonical state** | — |

Rules:

- Duplicate keys in a canonical map are an encode fault, not a last-writer-wins
  merge.
- Key comparison is over the **serialized key bytes**, compared as unsigned
  values, shorter-is-smaller on a common prefix. It is never a comparison of
  decoded values, `String.compareTo`, or any locale-sensitive collator.
- A canonical sequence whose order is not itself semantically meaningful must
  still declare a total order in `CanonicalStateCatalog`; "the order the events
  happened to arrive in" is not an order.

**Why it is frozen this way.** `HashMap` iteration order depends on the hash
function, the table capacity and the insertion history, and the JDK's and ART's
`HashMap` implementations are not required to agree. Sorting by serialized key
bytes removes every dependence on hash implementation, on `String.hashCode`
stability, and on locale. It is also cheap: canonical maps in this organism are
small and are sorted once per serialization.

---

## 5. Canonical text and byte-string policy

| Clause | Value |
|---|---|
| Natural-language text in canonical state | **Prohibited** |
| Canonical identifier charset | ASCII `A`–`Z`, `a`–`z`, `0`–`9`, `.`, `_`, `-` |
| Canonical identifier length | 1 to 64 bytes inclusive |
| Canonical identifier encoding | `u32` length prefix, then raw ASCII bytes |
| Unicode normalization (NFC/NFD/NFKC/NFKD) | **Never applied**; unnecessary because the charset is ASCII |
| Case folding, collation, locale-sensitive comparison | **Prohibited** |
| Opaque byte string | `u32` length prefix, then raw bytes |

Anything outside the identifier charset is an encode fault.

**Why it is frozen this way.** This is the single largest class of
cross-platform determinism hazard that can be eliminated by policy rather than
by test. `String.toUpperCase()` is locale-sensitive and famously differs for
Turkish locales; `java.text.Normalizer` depends on the Unicode version bundled
with the platform, and Android's Unicode version advances independently of the
JDK's; `Collator` differs by ICU version. Restricting canonical identifiers to a
fixed ASCII subset makes every one of those questions unanswerable rather than
answered, which is stronger. Human-readable text belongs to presentation and
diagnostics, which are noncanonical by section 9.

---

## 6. State-hash algorithm and digest size

| Clause | Value |
|---|---|
| Algorithm ID | `HASH_SHA256_V1` |
| Algorithm | SHA-256, FIPS 180-4 |
| Digest size | 32 bytes (256 bits) |
| Implementation | Project-internal pure-Kotlin implementation in `core-crypto` |
| Platform crypto providers (`java.security.MessageDigest`, Conscrypt, BouncyCastle) | **Not used to produce canonical hashes**; used only in tests as an independent cross-check |
| Domain-separation tag | ASCII `DLL17-STATE-HASH-V1`, written as a canonical identifier (length-prefixed) |

The canonical state hash is:

```plain text
stateHash = SHA256( canonicalIdentifierBytes("DLL17-STATE-HASH-V1")
                    || canonicalEnvelopeBytes(state) )
```

where `canonicalEnvelopeBytes` is the complete envelope of section 10, header
included.

**Why it is frozen this way.** SHA-256's *output* is standardized, so any correct
implementation agrees; but which implementation runs is not standardized.
`MessageDigest.getInstance("SHA-256")` resolves through the installed provider
list, which differs between a desktop JDK and Android, changes with Android
releases as Conscrypt evolves, and can be altered at runtime by an application or
by a device vendor. Owning the implementation removes provider selection from the
canonical path entirely. The cost is throughput, which does not matter: the state
hash is computed at commit and checkpoint boundaries, not per arithmetic
operation. The mitigation for the risk of an incorrect hand-rolled
implementation is section 12.4: the implementation is verified against the
published FIPS 180-4 vectors *and* differentially against `MessageDigest` on
every qualification target, so a defect would have to be present in both
independently.

---

## 7. PRNG algorithm and serialized state layout

### 7.1 Algorithm

| Clause | Value |
|---|---|
| Algorithm ID | `PRNG_SPLITMIX64_V1` |
| Family | Counter-based SplitMix64 |
| Increment constant `GAMMA` | `0x9E3779B97F4A7C15` |
| Mixing function | Stafford variant 13 |
| Output width | 64 bits |
| Cryptographic strength | None claimed, none required |

The mixing function is exactly:

```plain text
mix64(z):
    z = z XOR (z ushr 30)
    z = z * 0xBF58476D1CE4E5B9
    z = z XOR (z ushr 27)
    z = z * 0x94D049BB133111EB
    z = z XOR (z ushr 31)
    return z
```

All multiplication is wrapping 64-bit two's complement multiplication. All
shifts are logical (unsigned) right shifts.

### 7.2 Counter-based draw rule

A substream is fully described by an immutable `seed` and a monotonic
`counter`. The draw at index `n` (zero-based) is:

```plain text
draw(seed, n) = mix64( seed + (n + 1) * GAMMA )
```

`counter` records how many draws have been consumed. Drawing increments it by
exactly one.

This is **counter-based**, not path-dependent: `draw(seed, n)` is computable
directly from `(seed, n)` without replaying draws `0..n-1`. Recovery after
process death therefore needs only the two `i64` values, and a replay that seeks
to a known logical position cannot drift.

### 7.3 Serialized state layout

| Offset | Width | Field |
|---|---|---|
| 0 | `i32` | Algorithm ID ordinal (`PRNG_SPLITMIX64_V1` = `1`) |
| 4 | `i32` | Random domain ID |
| 8 | `i64` | `seed` |
| 16 | `i64` | `counter` |

Total: 24 bytes, fixed. Substream states appear in canonical state as a
canonical map keyed by domain ID, so section 4's ordering rule applies and the
serialized set of substreams is order-independent.

### 7.4 Hot-path prohibitions

Per draw, the following are prohibited: string construction, `Object.hashCode`,
cryptographic digests, `BigInteger`, boxing, and any heap allocation. A draw is
two additions, one multiplication, three XOR-shifts and two multiplications on
primitive `Long` values.

**Why it is frozen this way.** SplitMix64 is published in Steele, Lea and Flood,
*Fast Splittable Pseudorandom Number Generators* (OOPSLA 2014), and Vigna's
reference implementation is released into the public domain (CC0), so the
constants may be used without any licence obligation and without copying
licensed source — the implementation here is written from the published
constants. Its state is 64 bits of seed plus 64 bits of counter, which is the
smallest serialized footprint of any candidate considered and matters because
every substream's state is durable canonical state. It is integer-only with no
table, so it behaves identically on any conformant JVM. Alternatives are
evaluated in section 12.3.

---

## 8. Substream seed derivation

| Clause | Value |
|---|---|
| Mixer ID | `SUBSTREAM_DERIVE_V1` |
| Inputs | Master seed (`i64`), contract version (`i32`), random domain ID (`i32`) |
| Function | Two-stage `mix64` absorb |

```plain text
substreamSeed(masterSeed, contractVersion, domainId):
    z = mix64( masterSeed + 1 * GAMMA + contractVersion )
    z = mix64( z         + 2 * GAMMA + domainId )
    return z
```

Properties this freezes:

- **Domain isolation.** `substreamSeed` depends only on
  `(masterSeed, contractVersion, domainId)`. Registering a new domain cannot
  change any existing domain's seed, because no existing domain's seed is a
  function of the registry's contents, size, or iteration order. This is the
  mechanical guarantee behind the R001 exit criterion "random-domain insertion
  leaves existing streams unchanged".
- **No string hashing.** The domain ID is an integer, so derivation never
  depends on `String.hashCode` or on any digest.
- **One-time cost.** Derivation happens once per substream at initialization or
  first use, never per draw.
- **Migrations draw no randomness.** A migration that must introduce a new
  domain derives it by this same function from the existing master seed and the
  new domain ID. It never advances an existing substream and never calls
  `draw`.

---

## 9. Fixed-point scale and rounding mode

### 9.1 Representation

| Clause | Value |
|---|---|
| Canonical numeric type | `Fixed64` |
| Backing type | `Long` (`i64`) |
| Scale | `1_000_000` (10^6, six fractional decimal digits) |
| `1.000000` | `1_000_000` |
| Legal raw range | `-9_223_372_036_854_775_807` to `9_223_372_036_854_775_807` inclusive |
| `Long.MIN_VALUE` (`-9_223_372_036_854_775_808`) | **Not a legal canonical value** |

`Long.MIN_VALUE` is deliberately excluded so that negation is total and the
saturation bounds are symmetric. Any operation whose exact result would be
`Long.MIN_VALUE` or beyond saturates to `Fixed64.MIN` and emits a diagnostic.
This removes the single most common fixed-point overflow trap, in which
`-MIN_VALUE == MIN_VALUE` silently produces a negative from a negation.

### 9.2 Rounding

| Clause | Value |
|---|---|
| Rounding mode ID | `ROUND_HALF_AWAY_FROM_ZERO_V1` |
| Applies to | `satMultiplyScaled`, `satDivide`, `satInterpolate`, `satDecay`, and every other operation that rescales |
| Does not apply to | `satAdd`, `satSubtract`, `clamp` — these are exact |

Half-away-from-zero: a result whose exact value is exactly halfway between two
representable values rounds to the one with the larger magnitude.

**Why it is frozen this way.** Floor rounding (arithmetic shift, or Kotlin's
`Long` division truncation combined with a floor adjustment) is asymmetric about
zero and injects a systematic downward bias into any repeated decay or
interpolation, which over a long organism lifetime is a drift, not a rounding
error. Banker's rounding removes bias but requires a tie-parity test that is easy
to implement inconsistently. Half-away-from-zero is symmetric, has no parity
dependence, and its bias cancels for symmetric input distributions. Kotlin's
native `Long` division truncates toward zero, so the adjustment is a single
comparison against the doubled remainder.

### 9.3 128-bit intermediates

`satMultiplyScaled` requires the exact 128-bit product of two `i64` values
before rescaling by 10^6.

| Clause | Value |
|---|---|
| Method | Explicit unsigned 32-bit limb decomposition inside `core-math` |
| `Math.multiplyHigh` | **Prohibited** |
| `BigInteger` | **Prohibited in production paths**; permitted only in the test oracle |

**Why it is frozen this way.** `Math.multiplyHigh` was added to the JDK in Java 9
but only reached the Android platform at API 31, and the frozen `minSdk` is 29.
Using it would compile against the frozen `compileSdk` and then throw
`NoSuchMethodError` on a supported device — a determinism failure that no
desktop test would ever catch. `BigInteger` is correct but allocates, and
canonical arithmetic is the hot path. Limb arithmetic is pure `Long` operations
and behaves identically on every conformant JVM and on ART at every supported
API level.

### 9.4 Prohibited in canonical arithmetic

`Float`, `Double`, `Math.pow`, `Math.exp`, `Math.log`, `Math.sin` and every other
native transcendental function, wall-clock access, unordered iteration, and
concurrent mutation. These are prohibited by the canonical architecture's
determinism boundary and are enforced by test, not only by review.

---

## 10. Canonical envelope and versioning

Every canonical artifact — snapshot, journal frame, fixture, hash input — is
written inside one envelope:

| Offset | Width | Field | Value in contract version 1 |
|---|---|---|---|
| 0 | 4 bytes | Magic | ASCII `DL17` |
| 4 | `u16` | Envelope format version | `1` |
| 6 | `i32` | Determinism contract version | `1` |
| 10 | `i32` | Payload schema ID | Per artifact kind |
| 14 | `i32` | Payload schema version | Per artifact kind |
| 18 | `u32` | Payload length in bytes | — |
| 22 | *n* | Payload | — |

The header is included in the hash input. An artifact whose magic does not match
is not a canonical artifact and is rejected without further parsing.

---

## 11. Lookup-table generation and verification

| Clause | Value |
|---|---|
| Generation | Every table is produced by a pure integer generator function with no floating-point step, and checked in as generated Kotlin source |
| Generated-source header | Must state generator ID, generator version, parameters, and element count |
| Descriptor | Every table carries `(tableId, tableVersion, length, canonicalDigest)` |
| Verification method | The table's canonical serialization is hashed with `HASH_SHA256_V1` and compared against the embedded `canonicalDigest` |
| Verification timing | At first use in production, and unconditionally in the module test suite and in the qualification run |
| Mismatch behaviour | Hard failure. Never a warning, never a fallback, never a silent regeneration |

**Why it is frozen this way.** A lookup table is a large block of constants that
no reviewer reads line by line, so an accidental edit, a bad merge or a
truncated generator run is invisible to review. Binding the table to a digest
makes corruption a startup failure rather than a slow behavioural drift. Hashing
the table with the same algorithm as canonical state means one verified hash
implementation covers both.

R001 ships the mechanism and one mechanism-proof table. R001 defines no table
with organism meaning, because organism semantics belong to R003 and later.

---

## 12. Decision record

Implementation Plan E2E requires that the selected contract carry a decision
record covering cross-JVM/Android reproducibility, performance, licensing and
provenance, test-vector availability, and migration implications. This section
is that record. Candidates were evaluated before implementation; the coder did
not select an algorithm inside implementation code.

### 12.1 Cross-JVM and Android reproducibility

Every clause above was chosen to remove a runtime-dependent behaviour rather
than to test around it. The three eliminations that carry the most risk:

1. **Provider-resolved cryptography** (section 6) — removed by owning the SHA-256
   implementation.
2. **Unicode and locale** (section 5) — removed by forbidding non-ASCII canonical
   identifiers and all natural-language canonical text.
3. **API-level-dependent intrinsics** (section 9.3) — removed by forbidding
   `Math.multiplyHigh`, which exists on the desktop JDK 17 used for development
   but not on Android API 29 and 30, both inside the frozen supported range.

The residual reproducibility assumption is that wrapping 64-bit two's complement
integer arithmetic, logical shifts and `Long` division behave identically on every
target. That is guaranteed by the Java Language Specification and is verified
directly by the cross-target qualification rather than assumed.

### 12.2 Performance

| Path | Cost under this contract |
|---|---|
| Random draw | Two additions, two 64-bit multiplications, three XOR-shifts. No allocation |
| Substream derivation | Two `mix64` calls, once per substream lifetime |
| `satMultiplyScaled` | Four 32-bit partial products plus carries and one division. No allocation |
| Canonical serialization | One byte-array growth path, no reflection |
| State hash | SHA-256 over the canonical bytes, at commit and checkpoint boundaries only |

The one deliberate performance concession is the hand-written SHA-256, which is
slower than a hardware-accelerated provider. It is not on the arithmetic hot
path, and correctness of canonical identity was judged to outweigh digest
throughput. Actual measured costs are recorded in the R001 qualification
evidence, not asserted here.

### 12.3 Licensing and provenance

| Component | Origin | Licence position |
|---|---|---|
| SHA-256 | FIPS PUB 180-4, a US government publication | Not copyrightable as a specification; implemented from the published algorithm, no third-party source copied |
| SplitMix64 constants and mixer | Steele, Lea and Flood, OOPSLA 2014; Vigna's reference implementation | Vigna's reference is released to the public domain (CC0). Implemented from published constants; no source copied |
| `GAMMA` = `0x9E3779B97F4A7C15` | Odd 64-bit approximation of the golden ratio, in wide public use | No licence claim attaches to a mathematical constant |
| Fixed-point limb multiplication | Standard schoolbook decomposition | No third-party source |
| Everything else | First-party | Proprietary, per `LICENSE` |

Every entry above is recorded in
`governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md`. The zero-inheritance
position established at R000 is unchanged: no third-party source was copied into
this repository under R001.

### 12.4 Test-vector availability

| Component | Independent vectors |
|---|---|
| SHA-256 | FIPS 180-4 published vectors for `"abc"`, the empty string, and the 448-bit multi-block case, plus a differential check against `java.security.MessageDigest` on every qualification target |
| SplitMix64 | Differential check against an independent Python reference implementation written for this project, and committed golden vectors |
| Fixed-point arithmetic | `BigInteger` arbitrary-precision oracle, per Implementation Plan E2E work package R001.2 |
| Canonical serialization | Committed golden fixtures that must survive rebuilds and platform changes |

Test-vector availability was a selection criterion, not an afterthought: an
algorithm with no published vectors cannot be shown correct independently of the
implementation that produced its own expected values.

### 12.5 Migration implications

| Change | Consequence |
|---|---|
| Any clause in sections 1–11 changes | New contract version; new artifact `DeterminismContractV2`; this one is retained, not edited |
| Payload schema changes | Schema version bump plus a registered pure migration function |
| New random domain | Additive only. New domain ID, derived by `SUBSTREAM_DERIVE_V1`. Existing substreams unchanged and unadvanced |
| Enum case removed | Ordinal retired permanently, never reused |
| Migration functions | Pure. No randomness, no wall clock, no re-execution of old behaviour logic |
| Unknown or future version encountered | Refuse to decode. Never guess, never best-effort |
| Downgrade to an older schema version | Prohibited |

Golden vectors are retained across contract versions so that a later version can
prove it did not silently change the meaning of an older artifact.

---

## 13. What this contract does not decide

This contract governs determinism only. It assigns no organism semantics. It
does not define physiology, drives, action selection, learning, memory,
relationships, development, torpor, persistence policy, reconciliation, AR or
RouteEvidence, and it may not be read as authorizing any of them. Those belong
to R002 and later phases and to their own contracts.
