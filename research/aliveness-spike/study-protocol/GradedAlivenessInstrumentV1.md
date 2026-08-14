# GradedAlivenessInstrumentV1

- Status: `FROZEN`
- Version: 1
- Executable portion: `research/aliveness-spike/analysis/.../A001StudyContract.kt`
  (`GradedAlivenessInstrument`)
- Rendered form: `research/aliveness-spike/evidence/A001_ACTIVATION_DRY_RUN.txt`

The exact participant-facing wording, frozen verbatim. It is read or displayed
identically to every rater, for every creature, in every arm.

---

## Structure

One scored item, administered once per creature, immediately after that
creature's 600 s session and before the next session begins.

A continuous 0–100 slider with five labelled anchors shown on the scale.
Intermediate values are allowed and the slider starts unset — there is no
default position, because a default is an anchor nobody wrote down.

---

## Preamble

> You have just spent ten minutes with this creature. There are no right or wrong
> answers, and you are not being tested. We are asking about your impression of
> the creature, not about the graphics, the sound, or the software.

The last clause is doing real work. Without it the item collects a production-
quality rating, and both cohorts share their entire presentation layer, so a
production-quality rating would be noise by construction.

## Scored item

> How alive did this creature seem to you? Move the slider to any point between
> 0 and 100.

## Anchors

| Value | Label | Shown description |
|---|---|---|
| 0 | Not alive at all | It seemed like a moving picture or a machine running a sequence. Nothing it did came from the creature itself. |
| 25 | Slightly alive | It reacted to me sometimes, but what it did next never seemed to come from anything of its own. |
| 50 | Somewhat alive | It seemed to have some states of its own — it wanted things at some moments and not others — but I could usually see what it would do. |
| 75 | Mostly alive | It seemed to have its own interests and moods. It sometimes did things I did not expect, and those things still made sense for it. |
| 100 | Completely alive | It seemed like a creature with its own inner life, going about its own business, that I happened to be visiting. |

The anchors are ordered along the thing the thesis actually claims: whether what
the creature does next comes from the creature. They deliberately do not mention
learning, memory, personality or mechanisms, because naming a mechanism would
tell the rater what to look for and would make the ablation arms unusable.

## Secondary items

Administered once, after the second creature only.

> Thinking about both creatures you met: which one seemed more alive to you?
> (First / Second)

> Did the two creatures seem like the same kind of creature, or two different
> kinds? (Same / Different)

The second is a distinctiveness question, not a preference question. It is
reported separately and is never pooled with the forced choice.

---

## Frozen properties

`A001StudyContractTest` asserts all of these, so a later edit is a visible,
deliberate change rather than a drift:

- five anchors, strictly increasing, spanning 0 to 100 exactly;
- every anchor carries both a label and a description;
- the rendered instrument contains none of the words *full*, *baseline*,
  *scripted*, *cohort*, *ablation* or *organism* — a rater must never learn that
  cohorts exist;
- the item asks about the creature, and the preamble explicitly excludes the
  graphics.

## Limitation

Not cognitively pretested. Pretesting requires participants, and the directive
that froze this instrument forbids recruiting any. The wording was written by a
party with an interest in the outcome, and the independent reviewer may require
pretesting before Attempt 1. That is a legitimate reviewer decision, and this
document exists partly so it can be made on the actual text.
