# AlivenessStudyProtocolV1

- Status: `FROZEN`
- Version: 1
- Attempt: A001 Attempt 1 of a maximum of 3
- Frozen under: D010, before any human data exists
- Executable portion: `research/aliveness-spike/analysis/.../A001StudyContract.kt`,
  `A001Analysis.kt`
- Synthetic dry run: `research/aliveness-spike/evidence/A001_ACTIVATION_DRY_RUN.txt`

This is the complete scored study. Everything in it was decided before any
person rated anything, which is the only condition under which a decision rule
means what it says. After the first scored participant, every value here is
subject to `MaterialChangeEligibilityV1`.

## D016-J participant scope

Scored recruitment remains blocked. If later authorized, this protocol is
limited to U.S. adults age 18+ who can provide their own informed consent;
prisoners and people unable to provide legally effective consent are excluded.
The study-owner contact and compensation terms must be supplied before consent.

---

## The question

Does the candidate organism appear more alive to a person than a strong scripted
pet, by enough to justify building R003–R009?

Not *is it alive*, and not *do people like it*. The endpoint is a rated
impression under a matched, blinded, live interaction.

---

## Design

| Decision | Value |
|---|---|
| Design | Paired, within-rater. Each rater meets two creatures in one sitting. |
| Primary pair | FULL and `ScriptedPetBaselineV1` |
| Order | Counterbalanced. Odd-numbered participants meet FULL first, even-numbered meet the comparator first. Assignment is by enrolment index, fixed before the sitting. |
| Blinding | Raters are never told that cohorts exist. Both creatures are labelled `CREATURE`. The viewer has no reference to the cohort type and cannot display one. |
| Session length | 600 s per creature, identical for every cohort, enforced by the viewer |
| Presentation | One frozen contract, `SpikeExpressionContractV1` v1, with no cohort parameter |
| Interaction surface | Identical for every cohort: touch, call, offer food, present object, withdraw attention, startle |
| Between sessions | A 60 s gap during which the first creature's rating is taken. The rating for creature one is collected before creature two begins, so it cannot be revised in the light of the comparison. |
| Rater pools | Disjoint. Nobody rates more than one pair, and nobody who took part in the variance pilot or the baseline qualification is eligible. |

### Session-start equivalence

Both creatures must start the rater's session in a comparable state, or the
paired difference measures the starting conditions instead of the mechanisms.

| Requirement | Value |
|---|---|
| Simulated age at session start | Identical for both creatures: 30 virtual days of prior life |
| Habitat | Identical object set, identical condition, identical time of day |
| Drives at session start | Within one tick of the same point in the daily cycle |
| Prior interaction history | Both creatures have the same scheduled-interaction history; neither has met this rater |
| Wall-clock start | Both sessions begin at the same phase of the virtual day |

A pair that violates any of these is a technical failure and is excluded whole.

### Permitted participant interactions

The rater may, at any time and in any order: touch the creature, call it, offer
it food, present it an object, withdraw attention, or startle it. Nothing else
is available, and the same six are available for both creatures.

The rater is not given a task, a goal, or a suggested sequence. A scripted
interaction protocol would measure how each creature handles that script rather
than what the rater does when left alone with it.

---

## Instrument

`GradedAlivenessInstrumentV1`, wording frozen verbatim in
[GradedAlivenessInstrumentV1.md](GradedAlivenessInstrumentV1.md).

One item, 0–100, five labelled anchors, administered once per creature
immediately after that creature's session.

---

## Endpoints

### Primary

```
PairedAlivenessDifference = gradedAlivenessScore(FULL)
                          - gradedAlivenessScore(ScriptedPetBaselineV1)
```

**Attempt 1 passes the primary programme endpoint only if both hold:**

1. mean `PairedAlivenessDifference >= +10.0`; and
2. the lower bound of the two-sided 95% confidence interval is `> 0`.

Both, not either. The four possible outcomes are reported as distinct
classifications, because a significant +3 and an unresolved +14 fail for
opposite reasons and imply opposite next steps:

| Classification | Meaning |
|---|---|
| `PASS` | Mean at or above the floor and the interval excludes zero |
| `FAIL_NOT_PRACTICALLY_MEANINGFUL` | Real but too small to justify the complexity |
| `FAIL_IMPRECISE` | Possibly large, not resolved by this sample |
| `FAIL_NULL_OR_NEGATIVE` | Neither condition met |

The interval is a Student t interval on the paired differences, df = n − 1.

### Secondary

| Endpoint | Test |
|---|---|
| Forced choice: which creature seemed more alive? | Exact two-sided binomial against p = 0.5 |
| Distinctiveness: same kind of creature, or two different kinds? | Reported descriptively; a separate question, never pooled with the forced choice |

**A secondary endpoint cannot rescue a failed primary endpoint.** It is reported
whatever the primary says, and it changes no classification.

### Human mechanism family

Three preregistered leave-one-out arms, each a separate paired comparison
against FULL with its own disjoint rater pool:

- `FULL − curiosity anti-convergence`
- `FULL − preference learning`
- `FULL − outcome uncertainty / directed re-exploration`

Holm-Bonferroni at FWER 0.05 across the three. The arms determine whether an
individual mechanism has earned retention. **They do not replace or rescue the
FULL-versus-scripted primary endpoint**, and an attempt whose primary fails is a
failed attempt regardless of how many arms are significant.

The third arm replaced the retired episodic arm and was preregistered by the
architect under D010. Directed re-exploration became load-bearing under D009: it
is what lets the organism return to a rejected option because its estimate went
stale rather than because a die came up.

---

## Exclusions and data quality

Complete-case, no imputation. A pair with an unusable half is excluded whole,
because imputing an endpoint this small would be a decision made after seeing
which half went missing.

Screening runs in this fixed order, so a pair is reported under the first reason
that disqualified it rather than the most flattering one:

| Order | Reason | Rule |
|---|---|---|
| 1 | `DUPLICATE_PARTICIPANT` | The same person appears twice in an arm |
| 2 | `PRIOR_NON_SCORED_POOL` | Took part in the variance pilot or the baseline qualification |
| 3 | `TECHNICAL_FAILURE` | Session-start equivalence violated, viewer fault, or interrupted session |
| 4 | `INCOMPLETE_SESSION` | Either session below 90% of its 600 s |
| 5 | `MISSING_SCORE` | One or both ratings not recorded |
| 6 | `SCORE_OUT_OF_RANGE` | A rating outside 0–100 |

Every exclusion is counted and reported by reason in the analysis output,
whether or not it favours the result.

Replacement recruitment is permitted to reach the powered sample and is
outcome-independent: replacements are enrolled to fill the preregistered target
and are never chosen after inspecting scores.

---

## Analysis

The pipeline is `A001AnalysisV1`, written and tested before any data exists:

1. Screen, in the fixed order above.
2. Paired t-test on the primary arm; two-sided 95% t interval.
3. Classify against the two-part rule.
4. Exact binomial on the forced choice.
5. Paired t-test on each ablation arm; Holm-Bonferroni across the three.
6. Emit the attempt outcome.

The dry run in `evidence/A001_ACTIVATION_DRY_RUN.txt` exercises every branch on
synthetic fixtures, including the two failure modes that are easiest to confuse
and an arm that is significant before correction and not after.

---

## Result classification

| Attempt outcome | Programme consequence |
|---|---|
| `A001_ATTEMPT_1_PASS` | The aliveness hypothesis survives Attempt 1. R003–R009 become eligible, subject to their own gates. |
| `A001_ATTEMPT_1_FAIL` | Attempt 1 is consumed. A further attempt requires a material change under `MaterialChangeEligibilityV1` and independent adjudication. |

Three scored failures terminate the hypothesis: `ALIVENESS_PROGRAM_STOP`.

A failed attempt is retained in full. It may not be deleted, reclassified as
pilot data, or selectively pooled into a later attempt.

---

## Stated limitations

Recorded here rather than discovered later.

1. **The instrument is not cognitively pretested.** The wording and anchors are
   frozen as written, by a party with an interest in the outcome. Pretesting
   would have required the participants this directive forbids recruiting. The
   independent reviewer may require pretesting before Attempt 1.
2. **One item, not a validated scale.** Chosen because the endpoint is a
   difference between two administrations of the same question; a battery adds
   places for the two administrations to diverge for reasons unrelated to the
   creature. It also means the endpoint inherits whatever that single item fails
   to capture.
3. **Ten minutes is short.** Several of the mechanisms the thesis rests on —
   skill, habit, preference, re-exploration — operate over virtual days. A rater
   sees their consequences, not their formation.
4. **The habitat is abstract.** Twelve affordances, no space, no navigation, no
   sensors.
5. **The scored comparison is not the same as the objective one.** FULL leads the
   scripted baseline on every objective diversity measure. Whether that reads as
   *aliveness* to a person is exactly what A000 could not settle.
