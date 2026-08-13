# PresentationContractCatalog

- Registry version: `V1`
- Status: `SCAFFOLD_EMPTY`
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

None. R000 produces no organism presentation.

The R000 Android shell displays only build and phase identity. It is not a
presentation in the catalog sense: it has no canonical trigger, it anticipates
nothing, and it commits nothing. It may not be promoted into a product surface
without first being entered here.
