# device-matrix

The qualified hardware matrix and per-device results.

- `R000/` — Android shell launch qualification: install, cold launch, visible
  state, terminate and relaunch, on physical Tensor hardware.
- `R001/` — cross-target determinism matrix: every target must produce a
  byte-identical `R001_EVIDENCE_DIGEST`.

The two are different kinds of evidence and should not be read the same way.
R000's device evidence varies between runs — timings and process IDs differ, and
that is expected. R001's evidence must be **identical** on every run and every
target; a single differing hex digit is a qualification failure.

Device logs in both directories are filtered to this project's own logging tag.
The full logcat buffer of a physical phone inventories its owner's installed
applications and their account and telephony activity; none of that is evidence
about this project, and this repository is public.
