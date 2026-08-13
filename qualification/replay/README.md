# replay

Byte-identical replay evidence.

The R001 claim is:

```
reduce(snapshot0, orderedCommittedEvents) == replay(snapshot0, durableJournal)
```

where equality means identical canonical bytes and identical canonical state
hash, not equivalent object values.

- `R001/REPLAY_EVIDENCE.md` — per-fixture direct-reduction and replay hashes,
  journal digests, and the crash-boundary recovery sweep.

Replay across the device matrix is recorded in
`qualification/device-matrix/R001/`, because a target that replays correctly but
computes different bytes has not passed.
