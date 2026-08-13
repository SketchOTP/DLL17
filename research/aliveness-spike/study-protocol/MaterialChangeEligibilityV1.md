# MaterialChangeEligibilityV1

- Status: `FROZEN`
- Version: 1

Whether a revision after a failed attempt constitutes a materially new attempt.
Adjudication is a human judgement by the independent gate reviewer; the classes
below are frozen so the judgement is against a fixed rule rather than a fresh
argument.

## Qualifying (material)

- Adding, removing or replacing a learning or memory mechanism.
- Changing a load-bearing state representation.
- Changing an update law, controller topology or eligibility structure.
- Replacing the curiosity or habituation architecture, as opposed to retuning it.
- Materially changing how history or person identity enters selection.
- Changing the action or affordance model enough to alter the organism
  hypothesis and require baseline parity review.

## Non-qualifying (not material)

- New seeds, more raters, longer sessions.
- Changed exclusions or statistics chosen after seeing results.
- Cherry-picked histories.
- Viewer or animation polish.
- Weakening the baseline.
- Rerunning the same mechanism set.
- Ordinary parameter-only retuning inside the same mechanism form, unless that
  parameter family was explicitly preregistered as hypothesis-defining.

## Default

Ambiguity resolves to `NOT_MATERIAL`.

## Consequences

A new scored attempt requires both a documented prior-failure analysis and
independent approval that the revision fits a qualifying class. Each approved
attempt receives a new preregistration before any scored data. The
program-level floor may stay equivalent or become stricter, never easier.
