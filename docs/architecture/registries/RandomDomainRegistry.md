# RandomDomainRegistry

- Registry version: `V1`
- Status: `SCAFFOLD_EMPTY`
- Created: R000 (directive D004)
- Owner gate: `DeterminismContractV1` freeze, then R001

Authoritative list of every isolated random domain. Every draw in the organism
belongs to exactly one registered domain so that replay is reproducible and one
subsystem's consumption cannot shift another's stream.

## Required columns

| Column | Meaning |
|---|---|
| Domain ID | Immutable numeric identifier, never reused |
| Algorithm / version | Generator algorithm and its version |
| Persisted state requirements | What must survive process death |
| Derivation contract | How the substream is derived from the root seed |

## Entries

None. R000 draws no random values and derives no substreams.

Entries may not be added until `DeterminismContractV1` is frozen and R001 opens.
