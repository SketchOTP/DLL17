# R001 replay evidence

Evidence for the canonical replay equivalence assertion.

```
reduce(snapshot0, orderedCommittedEvents) == replay(snapshot0, durableJournal)
```

Equality is byte identity of the canonical serialization and equality of the
canonical state hash. The tests compare hex digests rather than calling
`equals`, because an `equals` that compared fields would pass even if the
serializer were wrong, and the serializer is exactly what the cross-target
claim depends on.

- Fixture set: `R001-FIXTURES-V1` version 1
- Determinism contract: `DeterminismContractV1` version 1
- Source: `ReplayEquivalenceTest`, `CommitVisibilityTest`, and the desktop reference runner

## Direct reduction versus journal replay

| Fixture | Direct reduction hash | Journal replay hash | Identical | Frames | Journal digest |
|---|---|---|---|---|---|
| `FX-ARITHMETIC-01` | `db48cfbc775d9d632506033e908dfa1844cdaa00c46e96195c4116a8abd74f34` | `db48cfbc775d9d632506033e908dfa1844cdaa00c46e96195c4116a8abd74f34` | yes | 5 | `1e24ddb41a619365aaacb8dc9b2efd59da3929de449bb44530b8e5b7ed252756` |
| `FX-RANDOM-01` | `64a6d7799bd82eb7b7f6584fa8afd40bc6170b0fab44fce97553adc9d42b3d4c` | `64a6d7799bd82eb7b7f6584fa8afd40bc6170b0fab44fce97553adc9d42b3d4c` | yes | 5 | `65c26acc5632c97b37f757e67e242582beb8f16fa92f3d9c3a3610c5a45154e9` |
| `FX-DECAY-01` | `be3815ff9a6ace14c49cef8bed764c1f653322e8d9f99a24fd19f277fdec9989` | `be3815ff9a6ace14c49cef8bed764c1f653322e8d9f99a24fd19f277fdec9989` | yes | 6 | `b2d09b92192e064770a0bfd467482d7fba33889227619ffc0484a3b02b2f2159` |
| `FX-WITNESSED-01` | `2401c0518ec8712c33a7d40233a4cace4868a7187e7dd2720437025f7f0e4b53` | `2401c0518ec8712c33a7d40233a4cace4868a7187e7dd2720437025f7f0e4b53` | yes | 4 | `50a517b5d3cf24e5fb615b37e7020b1f7a27a949329b5ff50cd2bfb33709fc68` |
| `FX-BOUNDARY-01` | `b5c4db23664b3aa0d66b6822cc43b303cc7a5c3ccfe6952bd7c61551dd4ea729` | `b5c4db23664b3aa0d66b6822cc43b303cc7a5c3ccfe6952bd7c61551dd4ea729` | yes | 4 | `a9de6225c44c02072be297f99dd3e60165d0bac4b610561be6a81ae8ce22329e` |

Replay reads the durable medium alone. No in-memory object survives from the
run that produced the journal, and the PRNG substream counters are restored
from the serialized canonical state rather than re-derived.

## Crash boundary recovery sweep

Implementation Plan E2E work package R001.6 requires a crash at every boundary
below. Each was injected, and after each the recovered state was compared
against the last durably acknowledged frame.

| # | Boundary | Frame acknowledged before the crash | Recovered state | Final presentations emitted by recovery |
|---|---|---|---|---|
| 1 | `BEFORE_CLASSIFICATION` | no | last acknowledged frame; the interrupted event left no trace | 0 |
| 2 | `AFTER_CANDIDATE_REDUCTION` | no | last acknowledged frame; the candidate was never published | 0 |
| 3 | `BEFORE_RECEIPT_WRITE` | no | last acknowledged frame | 0 |
| 4 | `DURING_RECEIPT_WRITE` | no | last acknowledged frame; the torn write acknowledged nothing | 0 |
| 5 | `AFTER_DURABLE_RECEIPT_ACKNOWLEDGEMENT` | yes | includes the acknowledged material transition | 0 |
| 6 | `BEFORE_CANONICAL_PUBLICATION` | yes | includes the acknowledged material transition | 0 |
| 7 | `BEFORE_FINAL_SEMANTIC_PRESENTATION` | yes | includes the acknowledged material transition | 0 |
| 8 | `AFTER_PRESENTATION_TOKEN_CONSUMPTION` | yes | includes the acknowledged material transition | 0 |

The pattern is the point. Before durable acknowledgement, a crash leaves
**nothing**: no canonical change, no visible material mutation, no semantic
claim. After durable acknowledgement, the transition is part of history and
recovery restores it exactly.

The last column is zero on every row, including the rows where the crash
happened after the commit was durable. That is the at-most-once guarantee:
the presentation token is consumed when the durable write **starts**, so a
recovery can restore the canonical interaction but can never emit a delayed
reaction merely because an animation's completion was uncertain.

## Tamper detection

A journal frame whose event operand was altered after the fact is detected at
replay rather than applied: the replay reports divergence at the altered
frame's sequence number. Both the pre-state and post-state hashes are recorded
in every frame, which is what allows a divergence to be located rather than
only detected at the end.

## Cross-target replay

Every fixture above replays to these same hashes on the desktop JVM, the
x86_64 Android emulator and Tensor G4 hardware. See
`qualification/device-matrix/R001/DETERMINISM_MATRIX.md`.
