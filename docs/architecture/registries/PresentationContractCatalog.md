# PresentationContractCatalog

- Registry version: `V1`
- Status: `POPULATED_R001`
- Created: R000 (directive D004)
- Owner gate: the phase that introduces each presentation

Authoritative list of every presentation that the organism may produce. A screen
may not invent its own modal priority; priority and preemption come from this
catalog.

## Required columns

| Column | Meaning |
|---|---|
| Presentation | Named presentation |
| Canonical trigger | Canonical state or event that authorizes it |
| Permitted reversible anticipation | What may be shown before the canonical commit |
| Irreversible presentation point | The exact point after which it cannot be retracted |
| Cancellation / rollback behavior | What happens if the canonical commit fails |
| Accessibility substitute | Equivalent non-visual or non-auditory presentation |
| Replay after restart | Whether it may be shown again after process death |

## Entries

R001 produces no organism presentation. What it does define is the **gating
mechanism** every future presentation must pass through, so the rows below
describe presentation *classes*, not named organism presentations.

| Presentation class | Canonical trigger | Permitted reversible anticipation | Irreversible point | Cancellation / rollback | Accessibility substitute | Replay after restart |
|---|---|---|---|---|---|---|
| `EPHEMERAL_ACKNOWLEDGEMENT` (Class E) | any received input | n/a — it is itself the anticipation | never irreversible | nothing to roll back | required when a named presentation is defined | not replayed; it asserted nothing |
| `REVERSIBLE_ANTICIPATION` | a Class W candidate exists but is not yet durable | orienting, leaning, neutral contact settle | never irreversible | withdrawn silently | required when defined | not replayed |
| `FINAL_SEMANTIC` | durable acknowledgement of the Class W commit frame | none — anticipation has already ended | the moment it starts | cannot be rolled back; prevented instead by the gate | required when defined | **never.** The presentation token is consumed when the durable write starts |
| `NEUTRAL_CANCELLATION` | persistence failure for a Class W interaction | n/a | never irreversible | it is the rollback | required when defined | not replayed |

The gate is enforced by `GatedPresentationSink` in `core-state`, which refuses a
`FINAL_SEMANTIC` call whose commit frame is not durably acknowledged. Enforcement
lives in the sink rather than at each call site on purpose: an invariant that
depends on every caller remembering to check it is a convention, while one the
sink refuses to violate is a control.

The R000 Android shell still displays only build and phase identity. It remains
outside this catalog: it has no canonical trigger, anticipates nothing and
commits nothing, and it may not be promoted into a product surface without being
entered here first.

No organism presentation may be added until the phase that owns the behaviour
opens.
