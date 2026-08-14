# D008 negative evidence — the rejected A000 candidate

Durable negative evidence, retained under `AlivenessProgramGateV1`. These are the
results of the **rejected** candidate mechanism set. They are not superseded by
D009 and must not be deleted, reclassified or selectively pooled into a later
pass.

- Candidate: A000 FULL, D008 form
- Qualified commit: `1ce1648576a52e8c9b10d0a319b3f4155910a071`
- Evidence digest: `4765e6d587347688841d34c95b5b9caede8cbf44084335302e1475c7aeaa8fc9`
- Full report: `A000_REPORT_D008.txt`
- Envelope search: `CURIOSITY_ENVELOPE_SEARCH_D008.txt`

## The five findings that did not hold

| Finding | Readout |
|---|---|
| `AX-DIFFERENTIATION-02` | `minTV=0.018646 maxTV=0.103079 floor=0.050000` |
| `AX-PREFERENCE-REVERSAL-02` | `otherMid=-0.010000 otherEnd=-0.010000` |
| `AX-EPISODIC-02` | `full=0.324722 minusEpisodic=0.402778` |
| `AX-ANTICONVERGENCE-01` | `entropy=1.588730 distinct=5.061111 occupancy=0.467535 wakingOccupancy=0.783809 revisits=32.094444 regularity=0.923790` |
| `AX-CONVERGENCE-01` | `meanFinalWindowTV=0.034323 minTV=0.005972 pairs=28` |

## The rejected configuration

The parameterization that produced the above. Recorded so the failure is
reproducible rather than merely described.

| Parameter | Rejected value |
|---|---|
| Rest and sleep tier when the habitat is in its night phase | Tier 3 unconditionally, whether or not rest was low |
| Episodic recall | context-free mean valence of recent episodes for the target, 12-hour window, weight 0.26 |
| Skill proficiency | absent |
| Outcome uncertainty and directed re-exploration | absent |
| Trait influence on need thresholds | absent; every organism shared `LOW_ENERGY` 0.52, `LOW_REST` 0.48, `LOW_SOCIAL` 0.50 |
| Preference law | error-corrected, rate 0.12 |
| Habit law | error-corrected, gain 0.09, loss 0.14 |
| Curiosity grid | base floor {0.02, 0.04, 0.08} × amplitude {0.08, 0.15, 0.26} × inhibition depth {0.06, 0.12, 0.22} |
| Envelope result | `EMPTY_THRESHOLD_INCOMPATIBILITY_CANDIDATE`, 0 of 27 points feasible |

## The envelope failure, in detail

Attribution passed at all 27 grid points with wide margin. Anti-convergence
failed at all 27 on exactly two of five criteria:

| Criterion | Range across the grid | Threshold |
|---|---|---|
| Maximum single-action occupancy | 0.462 – 0.477 | ≤ 0.450 |
| Cycle regularity | 0.803 – 0.876 | ≤ 0.550 |
| Action-type entropy | 1.685 – 1.923 | ≥ 1.600, passed |
| Distinct objects per day | 4.74 – 7.41 | ≥ 2.500, passed |
| Revisitation bouts per day | 18.0 – 57.8 | ≥ 3.000, passed |

## Architect disposition

D008 was accepted as `PASS` for A000 research. The candidate was judged not
ready for A001. The single allowed threshold-only curiosity revision was
deliberately **not** spent; the mechanism is to be fixed under the existing
thresholds. Remediation is directive D009.
