# Mechanism revision D009

- Status: `FROZEN`
- Version: 1
- Supersedes: the D008 candidate, retained as negative evidence in
  `research/aliveness-spike/evidence/negative/D008/`

What changed in the A000 FULL candidate, and why. Every change below was made to
resolve a specific accepted D008 failure under the **unchanged**
`CuriosityBalanceEnvelopeV1` thresholds. The programme's single allowed
threshold-only revision was not spent and remains available.

## Removed

| Mechanism | Disposition |
|---|---|
| Episodic history influence | `REMOVED` from FULL. A revised, context-conditioned, salience-retained form was measured against a five-seed matrix and still did not add history-dependent individuality. The mechanism and its cohort are retained inverted as `FULL+episodic-history` so the negative result stays reproducible. |

## Added

| Mechanism | Coalition group | What it fixes |
|---|---|---|
| Skill proficiency | `HABIT_OR_EXPECTANCY` | Long-run individuality. Bounded competence per action-and-object, grown by validated practice and faded by disuse. Two organisms that lived different lives end up good at different things, which is the only individuality a homeostatic drive model leaves room for. |
| Outcome uncertainty and directed re-exploration | `HABIT_OR_EXPECTANCY` | Re-exploration after devaluation. A per-option estimate of how stale or contradicted its value is: falls when sampled, rises while neglected, jumps on prediction error. Exploration appetite rises with frustration and is damped by stress and by fear. No random action noise is involved and the trace names the driver. |
| Action satiation | `PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE` | Anti-convergence. Diminishing marginal utility for doing the same *kind* of thing, as distinct from doing it to the same object. |
| Bounded engagement refractory | eligibility, not utility | Anti-convergence. A long enough bout with one object ends it and makes that object refractory. Canonical §7 lists refractory periods among the stability mechanisms. |
| Metabolic cost of vigorous activity | `PHYSIOLOGICAL_OR_MOTIVATIONAL_STATE` | Anti-convergence. Play and exploration cost energy, so a long bout makes the organism hungry and the day organizes itself into a cycle instead of one activity. |

## Changed

| Change | What it fixes |
|---|---|
| Rest and sleep are Tier 3 only when rest is actually low; a nocturnal preference lives in the utility term | Anti-convergence. Promoting rest for the whole night regardless of need meant Tier 3 outranked Tier 4 every night, one action took nearly half the tick budget, and the successor of almost anything was that action. |
| Each epistemic action is modulated by a different part of state and a different trait blend | Long-run individuality and anti-convergence. A single curiosity scalar changed how *much* an organism investigated but never *how*, so different personalities produced the same action-type budget. |
| Trait span widened; the social need threshold and the avoidance threshold are per-organism | Long-run individuality. Identical thresholds across a population force identical time budgets. |
| Preference updates only on consequential outcomes | Preference discrimination. Once the revised candidate looked at things far more often than it ate them, averaging over mostly-neutral glances scored a reliably good food source at zero. |
| Episodic admission is by salience, not recency | Measured, and it was not enough. A ring buffer of the last N events refills with the present, so in a matched probe it converges two organisms instead of distinguishing them. Salience retention fixed that specific defect; the mechanism still did not contribute. |
| Withdrawal is a bounded reaction to a live threat on every route into it | Anti-convergence. Damaged safety used to license Tier 0 withdrawal from everything present for as long as recovery took. |
| Only voluntary activities are resumable | Correctness. Recording a retreat as resumable created a punishment loop: withdraw from the aversive object, be interrupted, "resume" by engaging it again, be punished — eighty-seven times a virtual day. |
| Safety recovers in a couple of virtual hours rather than most of a day | Anti-convergence. |

## Habitat and fixture changes

These apply identically to every cohort, including both scripted comparators.

| Change | Why |
|---|---|
| People keep hours: one responds mostly in the first half of the day, the other in the second. The chime only rings in the small hours. | Without any circadian structure, a context-conditioned memory has no conjunction to learn and can only duplicate the context-free preference. This was needed to test the episodic mechanism at all — and with it, the mechanism still did not contribute. |
| `strictFoodContingency`: the unreliable source never succeeds | Makes which source an individual adopts a property of the protocol rather than of that individual's early tie-breaks, so the reversal fixtures ask the intended question. |
| `socialHoursShifted`: two conditioning histories differ in *when* someone responds | Produces identical context-free preferences and different context-conditioned memories, which is the only way to measure what episodic recall adds over preference. |
| `AX-REEXPLORATION-01` and `-02` | Controlled protocol for the re-exploration requirement. |
| `AX-PREFERENCE-01` and `AX-EPISODIC-02` pooled over seed matrices | A single organism's specialization is not evidence about a learning law, in either direction. |

## Comparator protection

`ScriptedPetBaselineV1`, `DegradedScriptedControlV1` and
`SpikeExpressionContractV1` are **unchanged**. The habitat and outcome changes
above are shared by every cohort, which is what parity requires.

Measured on a matched seed, habitat and window before and after the revision:

| Cohort | Entropy | Objects/day | Occupancy | Inactivity |
|---|---|---|---|---|
| `ScriptedPetBaselineV1`, D008 | 2.657 | 9.00 | 0.344 | 0.390 |
| `ScriptedPetBaselineV1`, D009 | 2.629 | 9.18 | 0.344 | 0.392 |
| `DegradedScriptedControlV1`, D009 | 1.377 | 1.00 | 0.584 | 0.703 |

The baseline is within noise of where it was, and remains far stronger than the
degraded control. What changed is that FULL now exceeds it on diversity measures
where it previously did not.
