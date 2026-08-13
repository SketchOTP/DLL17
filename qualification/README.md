# qualification

Qualification evidence for the Digital Living Lifeform. Each subdirectory is
populated by the phase that owns it.

- `fixtures/` — canonical input fixtures and their hashes. Populated by R001.
- `replay/` — byte-identical replay evidence across the qualified device matrix. Populated by R001.
- `fault-injection/` — process kill, storage fault, clock fault and permission fault runs.
- `longitudinal/` — long-horizon simulation runs and their canonical hashes.
- `device-matrix/` — the qualified hardware matrix and per-device results. Populated by R000 and R001.
- `red-team/` — adversarial and exploit findings against canonical guarantees.

## State after R001

| Directory | State |
|---|---|
| `fixtures/R001/` | `R001-FIXTURES-V1` golden vectors and the desktop reference report |
| `replay/R001/` | Replay equivalence evidence for every fixture |
| `device-matrix/R000/` | R000 Android launch qualification on Tensor hardware |
| `device-matrix/R001/` | R001 cross-target determinism matrix |
| `device-matrix/R002/` | R002 cross-target continuity matrix |
| `evidence/R000/`, `evidence/R001/`, `evidence/R002/` | Per-phase validation command output |
| `fault-injection/R002/` | R002 failure and exploit matrix: process death, torn writes, reboot loops, clock manipulation, storage exhaustion, thermal suspension |
| `longitudinal/` | Empty. No long-horizon run exists; there is no organism to run |
| `red-team/` | Empty. Adversarial review belongs to a later phase |

R001 qualifies a deterministic foundation and R002 a continuity foundation.
Neither qualifies an organism. No physiology, behaviour, learning, memory or
relationship evidence exists, because none of those are implemented.
