# D016-X Owner Pixel Aliveness Handoff

`D016-W-R1` remains closed as pre-execution evidence with zero formal model executions and no organism conclusion. D016-X supersedes the AI evaluator path for forward execution and stops at the owner verdict boundary.

## Frozen organism input

- Candidate: `A001_FULL_D016N_V1`
- Candidate Git SHA: `684579130bef5c820f3db9534ffb744654ebf3b4`
- Implementation starting HEAD: `086ffbc9a84f9dfe18d01349af4489de1451e95b`
- AI evaluator calls in D016-X: `0`
- Comparator displayed: `false`
- R003-R009: `BLOCKED`

## Integration boundary

- Android dependency scope: debug-only `research:aliveness-spike:cohorts`
- FULL construction: `Cohorts.create(Cohort.FULL, seed, fx)`
- Runtime: `SpikeRuntime` with `HabitatCondition.CONTROLLED_NOVELTY`
- Tick pacing: one organism tick every `200` milliseconds
- Presentation input: `SpikeExpressionContract.ExpressionFrame` only
- Android-selected organism actions: `false`
- Cloud service required: `false`

## Owner interaction mapping

| Owner action | Existing organism event |
| --- | --- |
| tap creature | `TOUCH` |
| tap person | `CALL` |
| tap food | `OFFER_FOOD` |
| tap play object | `PRESENT_OBJECT` |
| tap Look away | `WITHDRAW_ATTENTION` |
| tap red object | `STARTLE` |

## Local validation

- D016-X harness unit tests: `PASSED`
- Debug APK assembly: `PASSED`
- Release APK assembly: `PASSED`
- Debug APK SHA-256: `C00FEFA53510F67A1EA7A561099C745EA0AD8BC46E1A0446E8D602AF7E148FE6`
- Full Android unit suite: `PARTIAL`, one unrelated pre-existing persistence-path assertion failed in `AndroidLocalKeyBootstrapTest`
- Physical/emulated Android device connected: `false`
- Pixel install: `BLOCKED_DEVICE_UNAVAILABLE`
- Pixel launch: `BLOCKED_DEVICE_UNAVAILABLE`
- Owner aliveness verdict: `NOT RUN`

No A001 PASS or FAIL is claimed by this handoff.
