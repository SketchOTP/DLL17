# fixtures

Canonical input fixtures and their frozen expected outputs.

- `R001/GOLDEN_VECTORS.md` — the frozen expected values for `R001-FIXTURES-V1`:
  per-fixture canonical state hashes, per-section digests, the lookup table
  digest, and the overall `R001_EVIDENCE_DIGEST`.
- `R001/desktop_jvm_report.txt` — the desktop JVM reference runner's full output.

Golden vectors survive rebuilds and platform changes by design. A change to the
implementation that moves any value here is a determinism change, and requires a
`DeterminismContractV1` version bump rather than an edit to the vectors.

The fixture definitions themselves are executable, in `core-state`,
`R001QualificationKernel.fixtures()`. Keeping them in code rather than in a data
file is deliberate: every qualification target compiles the same fixtures, so no
target can be running a stale or locally edited copy.
