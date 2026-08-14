# R012 substrate gate, version 2 — epoch separation

- Phase: R012 substrate correction under `LocalStorageCryptographyContractV2`
- Directive: D013
- Fixture set: `R012-FIXTURES-V1` version 2
- Evidence digest:
  `0da0d889840c0bafe6554735cb9670d27f862870478589518383f0734aece6a5`
- Supersedes: `governance/release-gates/R012_SUBSTRATE_GATE.md`, which stays
  closed at its own commit and is not reopened
- Evaluated: 2026-08-14

**Gate state: `PASS`.** 55 of 55 fixtures hold, 13 of them new.

---

## What this gate adds to R012-QB-1

R012-QB-1 closed under D011 and remains pinned to
`afd0ecdb21bd20a00d4f3b6ae69d31e61890707c`. It was qualified against
`LocalStorageCryptographyContractV1`, and that qualification was accurate: every
fixture it ran did hold. What it did not run was a fixture that rotated the
wrapping material and then *read a record back*.

D012 ran one, on Android, and it failed. This gate closes the correction.

| Requirement | Fixture | Result |
|---|---|---|
| History readable after one wrapping rotation | `FX-EPOCH-ROTATE-ONCE-01` | `5/5` |
| History readable after repeated rotations, mixed with newer records, across restart | `FX-EPOCH-ROTATE-MANY-01` | `9/9` at wrapping epoch 6 |
| No journal byte rewritten by a rotation | `FX-EPOCH-NO-REWRITE-01` | identical |
| Data key and its identity unchanged across rotations | `FX-EPOCH-DATAKEY-STABLE-01` | `dataKeyId=1` |
| Unusable pending wrap abandoned, organism intact | `FX-EPOCH-ABANDON-01` | epoch 1 retained, `4/4` readable |
| Every plaintext header field still authenticated | `FX-EPOCH-CONTEXT-AUTHENTICATED-01` | `4/4` forgeries refused |
| Foreign data key refused at the right sequence | `FX-EPOCH-FOREIGN-KEY-01` | refused |
| V1 key state migrates, key and epoch untouched | `FX-V1-MIGRATION-01` | `dataKeyId=1` |
| Migrating twice is byte-identical to once | `FX-V1-MIGRATION-IDEMPOTENT-01` | identical |
| V1 records byte-identical after migration and a later rotation | `FX-V1-MIGRATION-RECORDS-01` | `6/6`, same canonical hash |
| Death before the migration rename | `FX-V1-MIGRATION-CRASH-STAGED-01` | readable V1, migrates on next open |
| Death after the migration rename | `FX-V1-MIGRATION-CRASH-RENAMED-01` | complete V2 |
| Unknown future schema refused | `FX-V1-MIGRATION-FUTURE-REFUSED-01` | refused |

Both migration crash fixtures use `Runtime.halt(9)` in a real child JVM, with no
shutdown hook. As everywhere else in this program, that proves file-layout
behaviour across abrupt process death and **does not** prove power-loss
durability, which remains unproven and unclaimed.

## Preserved, not superseded

| Evidence | State |
|---|---|
| R001 `54bc0447…`, R002 `556bbe49…`, A000 `9462e436…` | unchanged, byte for byte |
| R012-QB-1 | closed, pinned, verifies |
| R012DEV-QB-1 | pinned to D012's commit, `BLOCKED_DEVICE_UNAVAILABLE`, verifies |
| Canonical plaintext and state hashes | unaffected — encryption has never been inside them |

## Not closed by this gate

D012's physical-device requirement. `R012_DEVICE_GATE.md` remains
`BLOCKED_DEVICE_UNAVAILABLE`, and nothing here changes it: no physical Android
device was reachable to D013 either.
