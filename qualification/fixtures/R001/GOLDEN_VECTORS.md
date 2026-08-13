# R001 golden vectors

Frozen expected values for the R001 deterministic core. These are the
numbers every qualification target must reproduce exactly.

Golden vectors survive rebuilds and platform changes by design. If a change
to the implementation moves any value here, that is a determinism change and
requires a `DeterminismContractV1` version bump, not an edit to this file.

- Fixture set: `R001-FIXTURES-V1` version 1
- Determinism contract: `DeterminismContractV1` version 1
- Canonical hash: `HASH_SHA256_V1`, 32 bytes, domain tag `DLL17-STATE-HASH-V1`
- PRNG: `PRNG_SPLITMIX64_V1`, substream mixer `SUBSTREAM_DERIVE_V1`
- Fixed point: `Long`-backed, scale `1_000_000`, `ROUND_HALF_AWAY_FROM_ZERO_V1`

## Overall evidence digest

```
R001_EVIDENCE_DIGEST = 54bc044740a4c05b41b509a7160bff559e09421f2eaa55dc36c3d3ffadc1bd86
```

Compiled into `core-state` as `R001QualificationKernel.GOLDEN_EVIDENCE_DIGEST`
and asserted by tests on every target, so a target that diverges fails there
rather than silently producing a different organism.

## Per-fixture canonical state hashes

| Fixture | Canonical state hash after reduction | Replay matches | Frames | Journal digest | Saturations |
|---|---|---|---|---|---|
| `FX-ARITHMETIC-01` | `db48cfbc775d9d632506033e908dfa1844cdaa00c46e96195c4116a8abd74f34` | true | 5 | `1e24ddb41a619365aaacb8dc9b2efd59da3929de449bb44530b8e5b7ed252756` | 0 |
| `FX-RANDOM-01` | `64a6d7799bd82eb7b7f6584fa8afd40bc6170b0fab44fce97553adc9d42b3d4c` | true | 5 | `65c26acc5632c97b37f757e67e242582beb8f16fa92f3d9c3a3610c5a45154e9` | 0 |
| `FX-DECAY-01` | `be3815ff9a6ace14c49cef8bed764c1f653322e8d9f99a24fd19f277fdec9989` | true | 6 | `b2d09b92192e064770a0bfd467482d7fba33889227619ffc0484a3b02b2f2159` | 0 |
| `FX-WITNESSED-01` | `2401c0518ec8712c33a7d40233a4cace4868a7187e7dd2720437025f7f0e4b53` | true | 4 | `50a517b5d3cf24e5fb615b37e7020b1f7a27a949329b5ff50cd2bfb33709fc68` | 0 |
| `FX-BOUNDARY-01` | `b5c4db23664b3aa0d66b6822cc43b303cc7a5c3ccfe6952bd7c61551dd4ea729` | true | 4 | `a9de6225c44c02072be297f99dd3e60165d0bac4b610561be6a81ae8ce22329e` | 1 |

`FX-BOUNDARY-01` is the only fixture that saturates, and it does so on purpose:
it exists to prove saturation is deterministic and diagnosed. Every other
fixture saturating would be an R001 exit gate failure, because saturation
inside an intended operating range is a qualification failure rather than a
warning.

## Per-section digests

| Section | Golden digest |
|---|---|
| fixed-point | `1cb3b944aa46e05f322c91d3f724ab4f0094bb59c03e333577b9cb9a9f00d967` |
| random | `b3ba5f87e4f0fd3a9f932f83ed6752a1e051dcfac2ffaeef041e9bacabc34017` |
| lookup | `951d66b7863bbf706dcedec396b02032bbf5d34dc2b17f2438bf5f4ef6eb19ab` |
| serialization | `552473e9afb48fbe61921bceedf1096aa1213dd98eb714c380bfc8bae79a43f5` |

## Lookup tables

| Table | Version | Entries | Canonical digest |
|---|---|---|---|
| `LOOKUP_UNIT_RAMP_V1` | 1 | 257 | `5e8184bbc950ad382477edd4af3f2490ad71dc0fce92e29486865cddea81bcdb` |

This digest is computed twice by independent implementations: by
`tools/generate_lookup_tables.py` in Python with `hashlib`, and by the Kotlin
canonical codec with the project's own SHA-256. They agree, which means the
codec and the hand-written digest are cross-checked against a foreign
implementation rather than only against themselves.

## Independent verification anchors

| Component | Independent source |
|---|---|
| SHA-256 | FIPS PUB 180-4 published vectors, plus a differential check against `java.security.MessageDigest` on every target |
| Fixed-point arithmetic | `BigInteger` arbitrary-precision oracle over the full operand sweep |
| Canonical codec and hash | Independent Python implementation in the lookup-table generator |
| SplitMix64 constants | Unsigned hex of `GAMMA` asserted as `9e3779b97f4a7c15` against the published constant |

