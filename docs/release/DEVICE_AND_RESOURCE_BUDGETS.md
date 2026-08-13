# Device and resource budgets

Status: `NOT ESTABLISHED`.

Implementation Plan E2E requires provisional budgets for cold/warm startup,
canonical reducer latency, Class W commit latency, reconciliation wall time,
memory and storage, battery and thermal impact, sensor cadence, frame budget,
local speech latency and asset footprint.

No budget is recorded here, because every one of them would be a guessed number
today. Budgets are hypotheses until measured, and the subsystems they describe
do not exist. Each budget is recorded when the phase that owns the subsystem can
state it against real device evidence, and each becomes a `PLATFORM_MEASURED`
entry in `ParameterRegistry` when it is frozen.
