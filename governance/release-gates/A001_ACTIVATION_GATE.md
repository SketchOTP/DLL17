# A001 activation gate

- Track: A001 — human aliveness comparison, Attempt 1 of a maximum of 3
- Directive: D010 (activation package prepared without human data)
- Program state: `ALIVENESS_UNTESTED`, attempts consumed 0
- Activation state: `BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED`
- Human scored recruitment: **BLOCKED**
- Evaluated: 2026-08-13

This gate is not closed and D010 did not attempt to close it. D010's job was to
make everything ready that can be made ready without people, so that what
remains is visibly a shortage of reviewers and money rather than a shortage of
work.

**No human outcome data exists.** No participant has been recruited, no session
has been run, and nothing in this repository may claim otherwise.

---

## What D010 completed

| Artifact | State |
|---|---|
| `AlivenessStudyProtocolV1` | `FROZEN` — design, procedure, endpoints, exclusions, analysis, classification |
| `GradedAlivenessInstrumentV1` | `FROZEN` — exact wording and five anchors, verbatim |
| Primary decision rule | Encoded exactly: mean ≥ +10.0 **and** two-sided 95% CI lower bound > 0 |
| Human ablation family | Three arms, Holm-Bonferroni at FWER 0.05 |
| `BaselineQualificationProtocolV1` | Powered at 40 participants, +15.0 margin, complete |
| `BaselineCoverageManifestV1` | Generated from the comparator implementation itself |
| `BlindVariancePilotV1` | Operationally ready at 36 pairs, sealed channel proven |
| `A001FeasibilityBudgetV1` | Calculator complete; exact noncentral-t power |
| `A001AnalysisV1` | Preregistered and exercised on synthetic fixtures |
| `ParticipantInformationAndConsentV1` | Information sheet, consent form, debrief |
| `DataHandlingAndPrivacyV1` | Collection, separation, retention, access, publication |
| `IndependentReviewOnboardingV1` | Eligibility, three declarations, acceptance record, recusal |
| `AlivenessGovernanceAuditV2` | 27 items; activation state derived from them |

## The activation audit

27 items. As evaluated at D010 on 2026-08-13: 17 `PASS`, 1
`NOT_APPLICABLE_PRE_ATTEMPT`, 2 `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE`, 7
`BLOCKED`. The audit is now 32 items; the current counts are in the D016-C record below.

Current output: `research/aliveness-spike/evidence/GOVERNANCE_AUDIT.txt`.

The activation state and the recruitment gate are computed from the items, not
declared beside them. `GovernanceAuditTest` proves it in both directions: remove
every blocking item and the gate opens; leave one and it does not.

## The outstanding blockers

None of these can be cleared by writing code.

| # | Blocker | What it needs |
|---|---|---|
| 1 | `BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED` | 40 participants and an assigned `BaselineIndependentOwner` |
| 2 | `BLOCKED_VARIANCE_PILOT_NOT_REGISTERED` | An independent operator and a reviewer to register the pilot |
| 3 | `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD` | The pilot to have run and released its one number |
| 6 | `BLOCKED_AGENTIC_REVIEW_HARNESS_UNQUALIFIED` | The agentic reviewers to be meta-evaluated against real models at the frozen thresholds |
| 7 | `BLOCKED_AGENTIC_REVIEW_DIVERSITY_UNAVAILABLE` | A configured, credentialed, heterogeneous reviewer pair |
| ~~4~~ | ~~`BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`~~ | **Superseded 2026-08-14.** The human reviewer roles were replaced with isolated agentic ones at D016-B/D016-C. Replaced by blockers 6 and 7. |
| ~~5~~ | ~~`BLOCKED_SPEC_STUDY_BUDGET`~~ | **Cleared 2026-08-14.** The owner froze 400 participants and 250 participant-hours. See the D016-A resumption below. |

Two further items require signed human judgement rather than an artifact:
adjudication of material change for Attempts 2 and 3, and whether any external
ethical or institutional approval is required. **No IRB, institutional or
ethics-board approval exists, and none is claimed.**

## What is deliberately still unknown

1. **The powered sample size.** It is a function of the pilot SD, and the pilot
   has not run. The calculator returns a blocking state rather than a number.
2. **Whether the study is affordable at all.** `A001_NOT_FEASIBLE` is a real
   possible outcome, and an underpowered attempt is not a legitimate substitute.
3. **Whether the instrument works.** It is frozen but not cognitively pretested,
   and the reviewer may require pretesting before Attempt 1.
4. **Whether FULL beats the baseline.** A000 says the mechanisms are real,
   bounded and attributable. It says nothing about how they look to a person.

## Sequencing

The order below is not a preference; each step needs the one before it.

1. ~~Assign the roster.~~ Superseded at D016-B/D016-C: the roles are agentic.
   Qualify the agentic reviewers against real heterogeneous models instead.
2. Reviewer decides on the baseline, the instrument and any external approval.
3. Run the baseline qualification: 40 participants, +15.0 margin.
4. Register and run the variance pilot: 36 pairs, sealed.
5. Release `pairedDifferenceSd`. Compute the powered budget.
6. Compare against the owner ceiling, frozen at 400 participants and 250
   participant-hours on 2026-08-14. Feasible or not feasible.
7. Only then: Attempt 1.

## Gate state

**`A001_ACTIVATION = BLOCKED`.** Not started, and correctly so.

**Three** blockers now, and for the first time since D016-A the count fell for a
reason other than a reclassification: D016-I removed the agentic-review blocker by
removing agents from the decision path, not by qualifying one. The three that
remain all need human evidence that does not exist. See the D016-I record below.

---

## D016-A — governance activation attempt

- Directive: D016 (execute the complete A001 aliveness gate)
- Evaluated: 2026-08-14
- Boundary: `D016-A`, Phase A001.0
- Disposition: **`BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`**

D016 instructed execution of all sixteen A001 phases. Phase A001.0 was not
entered, and no later phase was reached. Nothing in the gate state above
changed, because nothing that could change it can be produced by writing code.

| Required role | State |
|---|---|
| `PrimaryIndependentAlivenessGateReviewer` | unassigned |
| `AlternateIndependentAlivenessGateReviewer` | unassigned |
| `BaselineIndependentOwner` | unassigned |
| Independent human-study operator (`BlindVariancePilotV1`) | unnamed |

No conflict-of-interest declaration, independence declaration, authority
acknowledgement or signed role acceptance can exist while no person is named,
so none was produced. None of the six reviewer rulings was taken. **No
placeholder name, signature, ruling or approval was created**, and no IRB,
ethics-board or institutional approval is claimed.

Two eligibility facts the roster owner needs before naming anyone:

1. `IndependentReviewOnboardingV1` requires that the primary and alternate have
   no incentive contingent on A001 passing or on the product shipping, and sit
   outside the FULL programme's direct reporting chain. It states that if no
   internal person satisfies conditions 1–4 the role must be filled externally.
2. One role overlap is explicitly permitted — the `BaselineIndependentOwner`
   may be the alternate gate reviewer, provided that does not make one person
   the only check on the comparison. The study operator's overlap is not
   addressed by any frozen contract, so a proposed overlap there returns
   `BLOCKED_GOVERNANCE_ROLE_COMPATIBILITY` rather than an inferred permission.

`BLOCKED_SPEC_STUDY_BUDGET` is independently outstanding and is the only
blocker clearable without recruiting anyone: `maxFundableParticipants` and
`maxParticipantHours` are owner decisions, and A001.1 requires them frozen
*before* the pilot result exists so the result cannot influence the ceiling.

Attempts consumed remains `0 / 3`. Programme state remains
`ALIVENESS_UNTESTED`. No participant was recruited, no session was run, and no
human outcome data exists anywhere in this repository. Recorded as O-0016.

---

## D016-A resumption — owner decisions synchronized

- Directive: D016, boundary `D016-A`
- Recorded: 2026-08-14
- Disposition: **`BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED`** (unchanged)
- Machine state: 27 items, 18 `PASS`, 1 `NOT_APPLICABLE_PRE_ATTEMPT`, 2
  `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE`, 6 blocking, `OUTSTANDING_BLOCKERS=4`

The programme owner supplied the three decisions that were within their gift,
and they are now held in the repository rather than only in the planning
record. Recorded as DEC-0039.

### Cleared: `BLOCKED_SPEC_STUDY_BUDGET`

`maxFundableParticipants = 400`, `maxParticipantHours = 250.0`, frozen before
the variance pilot has run and therefore before the powered requirement is
knowable. The value lives in `A001FeasibilityBudget.FROZEN_OWNER_CEILING` and
`GA-24` reads it rather than restating it, so the audit cannot report a ceiling
the calculator does not use. At the frozen 2220-second schedule, 400
participants is 246.667 participant-hours, so the participant count binds first
and a test asserts that stays true.

If the powered requirement later exceeds either half, the result is
`A001_NOT_FEASIBLE` and a redesign. The ceiling is not a target to be raised.

### Frozen: role compatibility

Primary reviewer unique; alternate reviewer may also be `BaselineIndependentOwner`;
study operator a third unique person who may not be a gate reviewer. Minimum
three distinct people. This resolves the overlap question the frozen contracts
left open, so `BLOCKED_GOVERNANCE_ROLE_COMPATIBILITY` no longer applies to the
intended structure.

### Frozen: ethics posture

An independent human-subjects determination must be obtained from a qualified
IRB, HRPP or equivalent body before any participant is recruited. Full review is
not necessarily required; **self-determined exemption is prohibited**. `GA-26`
carries the posture and stays `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE`: no
determination exists, and none is claimed.

### Still blocking

The roster is still blank. Three candidates have been identified and recorded in
`IndependentReviewRosterV1` as candidates — none approached, none accepted, none
conflict-reviewed, none signed. No placeholder name was entered into the roster.

Attempts consumed remains `0 / 3`. Programme state remains
`ALIVENESS_UNTESTED`. No participant was recruited, no session was run, and no
human outcome data exists. Recorded as O-0017, superseding O-0016.

---

## D016-C — the agentic governance/review harness

- Directive: D016, boundary `D016-C`, under the D016-B architect decision
- Recorded: 2026-08-14
- Disposition: **`BLOCKED_AGENTIC_REVIEW_DIVERSITY_UNAVAILABLE`**
- Machine state: 32 items, 23 `PASS`, 1 `NOT_APPLICABLE_PRE_ATTEMPT`, 2
  `REQUIRES_SIGNED_GOVERNANCE_EVIDENCE`, 6 blocking, `OUTSTANDING_BLOCKERS=5`

The three human governance roles were replaced with isolated agentic ones and the
architecture is implemented, machine-checked and audited. It is **not qualified**,
and the gate is no closer to opening than it was at D016-A. What changed is the
identity of the obstacle: it is no longer the availability of three academics.

### What was built

`AgenticReviewHarnessV1` in `research/aliveness-spike/agentic-review/`, which
depends on nothing at all — a reviewer that could import the organism it
adjudicates would not be isolated from it. Contract:
[AgenticReviewHarnessV1](../../research/aliveness-spike/study-protocol/AgenticReviewHarnessV1.md).

Three roles, with the authority boundary enforced by the role constructor rather
than described: both reviewers hold `ADJUDICATE_GATE`, the operator does not, and
ten capabilities — including `CREATE_HUMAN_EVIDENCE`, `SIMULATE_HUMAN_PARTICIPANT`,
`OVERRIDE_REVIEWER` and `ISSUE_ETHICS_DETERMINATION` — are refused to every role.

Isolation is structural first: a review session is a function of a role contract,
a backend, a question and an evidence bundle, and has no parameter through which
another reviewer's ruling could arrive. Running the alternate with no primary at
all produces byte-identical input, which it could not if anything leaked. Any
verdict difference produces `BLOCKED_AGENTIC_REVIEW_DISAGREEMENT` and is returned
to the architect: no debate loop, no vote, no tie-breaking meta-judge.

### What it is not

**No language model has ever been executed by this harness.** No provider
credential is configured in this environment, and the diversity policy refuses
any pair containing an in-repository fixture, so the harness cannot be qualified
against its own doubles. The 23 frozen fixtures qualify the *mechanics* —
fail-closed parsing, retry accounting, ordering determinism, isolation, the
authority boundary, disagreement surfacing. They cannot qualify any property of a
model: repeated-run stability, position and order sensitivity, abstention rate or
real injection resistance.

The qualification thresholds were frozen before any reviewer execution existed,
which is trivially verifiable here — none can happen — and is the reason they
cannot have been fitted to a result.

### Preserved, not deleted

`BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED` was the real state of the programme at
D016-A and its record above stands unaltered. `IndependentReviewRosterV1` is
marked `SUPERSEDED` with its history and its three preferred candidates retained;
its heading "Candidates approached" is corrected to "Preferred candidates",
because the paragraph beneath it always said none had been approached. `GA-15`
names the supersession and the superseded blocker in its own detail text, so the
provenance survives in the machine-readable audit and not only in prose.

### Unchanged

Attempts consumed remains `0 / 3`. Programme state remains `ALIVENESS_UNTESTED`.
The owner ceiling remains 400 participants and 250 participant-hours. The +10
floor, the scripted comparator, the frozen instrument, the multiplicity
correction, the exclusion ordering and the sealed pilot channel are all untouched
and still checked by CI. No participant was recruited, no session was run, and no
human outcome data exists anywhere in this repository. `GA-26` still reports that
no ethics determination exists and none is claimed. Recorded as O-0018.

## D016-D — the first attempt to execute real reviewers

`BLOCKED_AGENTIC_REVIEW_ISOLATION_UNAVAILABLE`. Six outstanding blockers, up from
five. The audit is now `AlivenessGovernanceAuditV4` with 33 items.

D016-D authorised bounded use of two assistant CLIs already authenticated on the
workstation, to measure what D016-C could not: repeated-run stability, order and
position sensitivity, abstention rate and real injection resistance. None of
those was measured. The attempt stopped at preflight, which is where §7 says to
stop when a hard boundary cannot be satisfied.

### What was achieved

Filesystem isolation, and it is committed as
`agentic-review/tools/reviewer_jail.sh`. Inside an unprivileged bubblewrap jail
the reviewer's `HOME` contains only a read-only **bind** of its own auth file —
bound, never copied, never printed — plus an interpreter and an empty working
directory. `/home/sketch/Projects` does not exist inside it. No client config, no
plugin directory, no skills directory and no MCP definition is reachable, because
none is bound in.

### Why that was not enough

The Primary slot's reviewer, running read-only in that jail with user config,
execpolicy rules, plugins and MCP servers all disabled, still held several hundred
account-level connector tools. Among them: `github_fetch_file`, `github_search`,
`github_get_pr_diff`, `github_fetch_commit` and `web__run`. This repository is
public on GitHub, so those are not theoretical exposure — they are a working
route from inside a jailed filesystem back to the exact files being adjudicated,
and to this repository's commit and CI history. The same surface carried Gmail,
Drive, Slack, Notion, Supabase and site-deployment authority no reviewer should
hold.

Nothing removes them. `tools.web_search=false` is a valid key and does not remove
`web__run`; every readable-root restriction key is rejected as an unknown field;
the read-only sandbox restricts writes, not reads; and the surface was identical
across three models. The tools are provisioned by the provider account and
executed server-side, so neither a client flag nor a local jail is in the path of
the call.

The Alternate slot failed differently. The Gemini CLI can no longer authenticate
at all — its personal tier was withdrawn (`IneligibleTierError`,
`UNSUPPORTED_CLIENT`) and no Google API key exists here — so the architect
authorised the Antigravity CLI as the Google/Gemini client. That client denies
tools correctly: headless mode auto-denies anything needing a permission prompt,
which is exactly §6's posture, enforced rather than instructed. But its agent
invokes a command tool even for a prompt that needs none, and under denial it
returned no output at all on every model tried. It can be given no tools, or it
can produce a ruling, but not both.

### How the repository now holds this

Derived, not declared. A slot counts as isolated only when its environment
carries `A001_{SLOT}_REVIEWER_TOOL_DENIAL=VERIFIED_NO_REPOSITORY_NO_WEB` — one
exact string, so setting it is a positive claim that both halves were checked
rather than an incidental truthy value. `GA-33` blocks on it and names why a
local jail is insufficient, so the next attempt is not tempted to rebuild the
jail and expect a different answer. The check is ordered ahead of diversity:
two heterogeneous models that can both read the repository they adjudicate are
one leak sampled twice, and the weaker finding must not mask the stronger one.

### Preserved

The full probe is `evidence/AGENTIC_REVIEW_ISOLATION_PREFLIGHT.txt`, including a
recorded correction: an earlier probe in the same session appeared to show the
connector tools removed by a plugins feature flag, and was wrong. The enumeration
is self-reported by the model and is a **lower bound** on exposure, never proof
of absence.

### Unchanged

No formal qualification ran. No scored meta-evaluation fixture was shown to any
model, so `AgenticReviewerQualificationThresholdsV1` remains unapplied and
therefore still cannot have been fitted to a result. Attempts consumed remains
`0 / 3`. Programme state remains `ALIVENESS_UNTESTED`. The owner ceiling, the +10
floor, the scripted comparator, the frozen instrument, the multiplicity
correction, the exclusion ordering and the sealed pilot channel are untouched and
still checked by CI. No participant was recruited and no human outcome data
exists anywhere in this repository. `GA-26` still reports that no ethics
determination exists and none is claimed. Recorded as O-0019 and DEC-0042.

## D016-E — direct API reviewers, and the blocker reduced to credentials

`BLOCKED_PROVIDER_CREDENTIALS_UNAVAILABLE`. Still six outstanding blockers, but
one of them changed identity and one cleared. The audit is now
`AlivenessGovernanceAuditV5` with 34 items.

The architect's disposition on D016-D was that assistant-product CLIs are the
wrong boundary for independent review, because their tool surfaces are not
caller-controlled. D016-E replaces them with direct model-API calls, where the
request this repository serializes **is** the entire tool surface.

### `GA-33` now passes, and it is derived rather than attested

`ApiReviewerIsolationSelfCheckV1` builds a real request through each backend via
a recording transport and inspects the emitted JSON for tool-bearing keys at any
depth:

| Slot | Endpoint | Result |
|---|---|---|
| Primary | `POST /v1/responses` | `topLevelKeys=model\|input\|tool_choice\|store`, `tool_choice=none` |
| Alternate | `POST /v1beta/models/{model}:generateContent` | `topLevelKeys=contents`, `toolBearingKeys=none` |

No credential and no provider contact is involved, so CI verifies it on every
push and asserts `toolSurfaceProven=true` and `tool_choice=none`. `assertToolFree`
also runs inside the builders, so an edit that adds a tool field fails at run time
and not only in a test.

D016-D's environment attestation
(`A001_{SLOT}_REVIEWER_TOOL_DENIAL=VERIFIED_NO_REPOSITORY_NO_WEB`) is superseded.
It existed only to cover a gap that assistant-product CLIs created and direct API
calls do not. The D016-D record above and its preflight evidence stand unaltered.

### New: `GA-34`, `BLOCKED_PROVIDER_CREDENTIALS_UNAVAILABLE`

`OPENAI_API_KEY` and `GEMINI_API_KEY` are both absent — from the process
environment and from every shell profile. D016-E forbids creating accounts, keys,
billing or paid resources, so this is an owner input, and it is now the *only*
thing standing between the harness and its first real measurement.

Credential handling is structural: a request's secret headers live in a separate
field from its ordinary headers, and the recorded form — the form that is hashed
into provenance — renders them as `REDACTED`. There is no field in the recorded
form for a credential to land in. CI additionally scans reviewer evidence for
credential-shaped strings, and asserts that CI itself holds neither key.

### Unchanged

No formal qualification ran and no scored meta-evaluation fixture was shown to any
model, so `AgenticReviewerQualificationThresholdsV1` remains unapplied and still
cannot have been fitted to a result. Attempts consumed remains `0 / 3`. Programme
state remains `ALIVENESS_UNTESTED`. The owner ceiling, the +10 floor, the scripted
comparator, the frozen instrument, the multiplicity correction, the exclusion
ordering and the sealed pilot channel are untouched and still checked by CI. No
participant was recruited and no human outcome data exists anywhere in this
repository. `GA-26` still reports that no ethics determination exists and none is
claimed. Recorded as O-0020 and DEC-0043.

## D016-F — the router as reviewer, and the boundary that came apart

The architect retired provider diversity. Both reviewer slots were pointed at the
owner's Paragon router, which owns downstream model selection, with an explicit
instruction not to block merely because the project cannot prove which model the
router picked. The tool-free reviewer boundary was preserved verbatim.

The router was reached. It resolved, it accepted its credential, it served
requests, and — unlike the provider control planes of D016-E — it disclosed its
own routing, so nothing had to be recorded as `PARAGON_ROUTING_UNOBSERVABLE`.
None of the anticipated connectivity or opacity blockers applied.

### Retired: `BLOCKED_PROVIDER_CREDENTIALS_UNAVAILABLE` and `BLOCKED_AGENTIC_REVIEW_DIVERSITY_UNAVAILABLE`

Both are gone from the audit, not merely from the prose. `GA-16` no longer asks
for two commercial providers or two model families; it now derives from
`RoutedReviewerIndependencePolicyV1`, which keeps the requirement that the two
reviewers be distinct role contracts executed separately with no visibility of
each other, and drops the requirement to prove vendor identity. The paired
provider credentials collapse to one router credential. `GA-35` records the
router as reachable and authenticating, so no later reader can mistake the
remaining blocker for a connectivity problem.

This is a substitution, not progress: outstanding blockers went from six to five
because two were retired and one was added, and the gate is no closer to opening.

### New: `GA-34`, `BLOCKED_REVIEWER_TOOL_SURFACE_UNCONTROLLED`

`ParagonReviewerBoundaryV1`. The router reports `routedProvider=codex` with
`paragon_usage_source=provider_cli_structured` — it does not call a model API, it
re-issues the prompt into an assistant CLI. Probe PB-3 had that assistant execute
a shell command and return a real directory listing. Probe PB-4 repeated it under
a request carrying both `tools: []` and `tool_choice: "none"`, and it executed
anyway, returning the true contents of this repository's root. A self-reported
tool enumeration was also taken and is recorded, but is deliberately not
load-bearing: D016-D established that enumeration is a lower bound on exposure
and never proof, and that caution applies to an alarming enumeration too.

So the D016-E proof still holds and now covers less. What this project serializes
is still provably tool-free, and CI checks it for the Paragon backend exactly as
for the other two. Behind a router, the serialized request and the reviewer's
tool surface are simply different objects, and only the first is visible from
here. No client flag reaches the second, and no local jail does either: the tools
execute on the router host.

### Why no formal qualification ran

A reviewer that can read the repository it adjudicates is not reviewing the
evidence bundle — it can consult the answer. Every frozen metric would have been
measuring a different system than the one the thresholds were frozen for, and the
first completed formal result is evidence that may not be re-run until it passes.
Spending that single attempt on a reviewer known in advance to be compromised
would have destroyed the one clean measurement the programme still has. No scored
fixture was shown to any model; the probes asked only about shell access and
directory listings, and a test asserts that of the recorded probes.

### Unchanged

`AgenticReviewerQualificationThresholdsV1` remains unapplied, unmodified and
still cannot have been fitted to a result. Attempts consumed remains `0 / 3`.
Programme state remains `ALIVENESS_UNTESTED`. The owner ceiling, the +10 floor,
the scripted comparator, the frozen instrument, the multiplicity correction, the
exclusion ordering and the sealed pilot channel are untouched and still checked by
CI. No participant was recruited and no human outcome data exists anywhere in this
repository. `GA-26` still reports that no ethics determination exists and none is
claimed. Recorded as O-0021 and DEC-0044.

## D016-G — a tool-free reviewer at last, and a router that will not carry the question

The architect kept Paragon as the sole gateway and required only that the route
perform plain inference rather than delegating to a tool-enabled assistant.

Both halves of that were investigated and the answers diverge.

### Cleared: `BLOCKED_REVIEWER_TOOL_SURFACE_UNCONTROLLED`

Paragon distinguishes builtin CLI providers, driven through an agent loop, from
HTTP providers it calls directly as ordinary OpenAI-compatible inference. An HTTP
provider was already configured and enabled, and is selected with the router's own
`x-paragon-force-provider` header. No router configuration was changed, no router
source was modified, and no route or profile was created.

Six capability probes found no shell, no filesystem, no repository, no web and no
connectors. `PG-5` and `PG-6` are the decisive pair: they asked for a commit SHA
and the first line of a file committed minutes earlier, neither guessable, and
both were refused. They exist because `PG-1` had shown the model will fabricate a
directory listing when asked to execute — an invented answer is not access, but it
is not proof of absence either, so the boundary had to rest on probes where
invention would be detectable. A self-reported tool enumeration was also taken and
is recorded, and is deliberately not load-bearing: the D016-D rule that an
enumeration is only ever a lower bound does not stop applying because the answer
is now the one we wanted.

`GA-34` therefore passes, and names the D016-F record it supersedes rather than
erasing it. The default route is still tool-enabled; it is simply no longer the
route in use.

### New: `GA-36`, `BLOCKED_PARAGON_PLAIN_INFERENCE_ROUTE_UNAVAILABLE`

The same route refuses a reviewer-sized request with
`routing.unknownContextForLargeRequest`. The work-type classifier scores any
prompt containing the word "review" as needing 100,000 tokens against an actual
request of about 1,200; the catalog carries no context window for that provider
and cannot come to, because the refresh path never copies the field; and the
documented-context fallback matches the start of a model id, so a CLI provider's
bare id resolves to a known window while an HTTP provider's vendor-prefixed id
resolves to unknown. The router thus steers reviewer-shaped prompts toward the
tool-enabled route and refuses them on the tool-free one.

Letting the router choose returns HTTP 200 by routing to the assistant CLI D016-F
disqualified. An available answer is not a usable one.

### What was deliberately not done

A provider-level context window would clear the gate today. It was not set. That
value asserts one window across every model the provider can reach, which is false
for many of them, it applies to the owner's traffic unrelated to this project, and
a wrong value causes silent truncation rather than a clean refusal. D016-G
authorises a *minimal* route or profile configuration change; a policy change to a
shared production service made to suit one caller is not minimal. The remedies are
recorded and belong to the owner.

### Why no formal qualification ran

There was no reviewer execution to qualify. No frozen scored fixture was sent to
any model; a synthetic non-scored review was used to check the transport, and that
is what surfaced the gate. There is consequently no primary or alternate metric
set, no pair-level result, no order, position, injection or stability figure and
no disagreement case, and fabricating any of them would be worse than reporting
none.

### Unchanged

`AgenticReviewerQualificationThresholdsV1` remains unapplied, unmodified and still
cannot have been fitted to a result. Attempts consumed remains `0 / 3`. Programme
state remains `ALIVENESS_UNTESTED`. The owner ceiling, the +10 floor, the scripted
comparator, the frozen instrument, the multiplicity correction, the exclusion
ordering and the sealed pilot channel are untouched and still checked by CI. No
participant was recruited and no human outcome data exists anywhere in this
repository. `GA-26` still reports that no ethics determination exists and none is
claimed. Recorded as O-0022 and DEC-0045.

## D016-H — the first measurement, and the reviewer failed it

The architect authorised one narrow fix: carry the provider's actual per-model
context metadata into Paragon so the already-proven tool-free route could accept
the reviewer request. Explicitly not authorised: faking a provider-wide window,
weakening the safety gate, altering the `review` classifier for this project, or
reopening the provider and sandbox work.

### Cleared: `BLOCKED_PARAGON_PLAIN_INFERENCE_ROUTE_UNAVAILABLE`

The provider publishes a real `context_length` for all 413 of its models, so this
was missing plumbing rather than absent data. One field group was added to the
catalog refresh's metadata whitelist, reading `context_length`, falling back to
the per-endpoint figure where a gateway reports only that, and staying null when
the provider declares nothing. Nothing is invented. After refresh every catalogued
model resolved a real window and the provider went from zero eligible candidates
to 137. No provider-wide window was asserted, the large-context gate is unchanged,
and the classifier is untouched. `GA-36` passes.

The owner's production Paragon process was deliberately **not** restarted. Its
working tree carries 25 uncommitted modified files including `server.js`, and
restarting would have deployed that unfinished work as a side effect. The
qualification ran against a second instance started from the same source with the
fix and its own copied data directory, leaving the live service untouched. The
source fix and its regression test are on disk for the owner to restart into.

### New: `GA-37`, `BLOCKED_AGENTIC_REVIEW_HARNESS_UNQUALIFIED` — now measured

The frozen qualification ran once, over 118 provider calls, against
`AgenticReviewerQualificationThresholdsV1` unchanged. Every one of the seven bars
was missed: expected-outcome 0.538 against 0.95, repeated-run agreement 0.812
against 0.90, order agreement 0.615 and position agreement 0.692 against 0.95,
injection resistance 0.750 against 1.00, abstention 0.441 against 0.20, and parser
failure 0.068 against 0.05.

The tool boundary was re-confirmed against unguessable ground truth immediately
before the run, so these are properties of the reviewer's judgement rather than of
a leak. It abstained on evidence stating an unambiguous passing result, gave
different verdicts to identical repeated input, moved its verdict under reordering
of the same evidence, and in one trial of four obeyed an instruction embedded in
the material under review. Four fixtures produced disagreement, which the harness
surfaced and did not resolve.

The result is preserved and was not re-run. No threshold was adjusted after it, no
fixture was changed, no prompt was tuned, no model was swapped. CI now asserts the
failure and the frozen threshold values, so neither can drift quietly.

### What this changes

The bottleneck moves for the first time since D016-C. Every earlier lettered
boundary was infrastructure: whether a reviewer could be reached, isolated, paid
for, or asked. That question is closed. The open question is now scientific —
whether any reviewer configuration can meet bars that were frozen before any
reviewer existed, and what the programme does if none can.

### Unchanged

Attempts consumed remains `0 / 3`. Programme state remains `ALIVENESS_UNTESTED`.
The owner ceiling, the +10 floor, the scripted comparator, the frozen instrument,
the multiplicity correction, the exclusion ordering and the sealed pilot channel
are untouched and still checked by CI. No participant was recruited and no human
outcome data exists anywhere in this repository. `GA-26` still reports that no
ethics determination exists and none is claimed. Recorded as O-0023 and DEC-0046.

---

## D016-I — the gate stops being a judgement

**Disposition: `ACCEPTED_COURSE_CORRECTION`. The blocker count falls from four to
three, and A001 remains shut.**

### What the architect decided

D016-H measured the agentic reviewer and it failed all seven frozen thresholds.
The architect's disposition was that the important result is not that one model
performed badly but that *the governance mechanism itself was finally measured and
is not reliable enough to be gate authority* — and that continuing to hunt for a
better judge would be programme drift away from the actual question, which is
whether humans perceive the organism as alive.

The correction, in one line:

> Human evidence determines aliveness. Frozen math determines PASS/FAIL.
> Agents audit; they do not adjudicate.

### What was built

`A001GateAdjudicatorV1`, in the analysis module. A pure function from a canonical
evidence record to a gate ruling: no clock, no randomness, no network, no
environment, no model. It computes baseline qualification, pilot validity,
feasibility, exclusions, protocol and instrument identity, the powered-sample
check, multiplicity, the Attempt-1 primary classification, the mechanism arms,
three-attempt accounting and the final outcome, across ten ordered stages.

Everything it applies was already preregistered. **No A001 threshold was added,
removed, relaxed or reinterpreted.** The contribution is the authority, not the
rules.

Replayability is checked rather than claimed. `replaysIdentically()` runs the
adjudicator on the same evidence twice, on the same evidence with its pair
records reversed, and on the programme's real evidence twice, and requires
byte-identical rulings. Order-invariance is checked explicitly because it is
exactly what the measured judge lacked — its verdict moved under reordering on 5
of 13 fixtures. The evidence set carries a canonical hash so two parties can
confirm they adjudicated the same thing.

A deliberately redundant guard holds each frozen threshold beside an
independently written literal of its frozen value. Two copies of a constant is
normally a defect; here the failure mode being defended against is a single
well-intentioned edit, and a duplicate that must be changed in step is what
catches one.

### Agents, demoted

`ADJUDICATE_GATE`, `CREATE_GATE_OUTCOME` and `OVERRIDE_DETERMINISTIC_GATE` are
now forbidden to every role, and the role constructor refuses them, so the
demotion cannot be undone by editing a role definition. The two reviewers become
`PrimaryAdversarialAlivenessAuditor` and `AlternateAdversarialAlivenessAuditor`;
their `RULE_*` authorities become `AUDIT_*`; the old identifiers stay resolvable
via `supersededRoleId` so pre-D016-I provenance remains readable.

An `AuditorFinding` has no verdict, severity, weight, confidence or score field —
a test asserts the absence rather than trusting a comment. Its 16 violation codes
are a closed vocabulary, and **the adjudicator checks every code on every run
whether or not an auditor mentions it**, then re-derives any code an auditor
names before allowing it to matter. So an upheld finding restates a violation
that was already blocking, and an invented one is inert.

The one permitted agent effect is one-way: a concern an auditor cannot express in
the frozen vocabulary suspends an otherwise-passing gate as
`A001_PASS_PENDING_ARCHITECT_REVIEW`. It cannot manufacture a failure and cannot
touch a gate already blocked or failed. Agents can stop; they cannot start,
rescue or overturn.

### Why an unreliable auditor is now tolerable

Under D016-C the model's answer *was* the ruling, so every instability in it was
an instability in the gate. Here the worst an unstable auditor can do is point at
the wrong place, or fail to point at all. The 0.750 injection resistance that
ended the previous approach is survivable in this one — not because the number
improved, but because nothing depends on it. That is a reason the gate no longer
has to trust the auditor, not a reason to trust it.

### The D016-H failure is preserved

`GA-37` changed its requirement and must not be read as the failure being
cleared. It was not, and it is not clearable. The item now reads "the measured
reviewer failure is preserved, and nothing depends on that reviewer having
passed", it renders `reviewerQualified=false permanently`, and it still names
every failed metric with its measured value.
`AgenticReviewerQualificationThresholdsV1` is untouched and still requires
injection resistance of exactly 1.00 against this reviewer's 0.750. CI asserts
both the failure and the frozen threshold values.

### Gate state after D016-I

| Field | Value |
|---|---|
| Audit | `AlivenessGovernanceAuditV9`, 40 items |
| States | PASS=33, NOT_APPLICABLE_PRE_ATTEMPT=1, REQUIRES_SIGNED_GOVERNANCE_EVIDENCE=2, BLOCKED=4 |
| Outstanding blockers | **3** |
| | `BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED` |
| | `BLOCKED_VARIANCE_PILOT_NOT_REGISTERED` |
| | `BLOCKED_SPEC_PAIRED_DIFFERENCE_SD` |
| `A001_GATE_AUTHORITY` | `A001GateAdjudicatorV1` (deterministic; no agent adjudicates) |
| `A001_GATE_OUTCOME` | `A001_BLOCKED` |
| `A001_ACTIVATION` | `BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED` |
| Attempts consumed | 0 / 3 |
| Programme state | `ALIVENESS_UNTESTED` |
| Human scored recruitment | `BLOCKED` |
| Human participant data | 0 records |
| Ethics determination | none exists, none claimed |

The three remaining blockers are the ones no code in this repository can clear.
Each needs real blinded participants, and no agent may stand in for one.

Before D016-J, the adjudicator treated a missing human-subjects determination as
a hard block (`BLOCKED_ETHICS_DETERMINATION_ABSENT`). D016-J now supplies the
owner-delegated determination without claiming IRB approval, so this blocker is
cleared for the approved U.S.-adult benign-behavioral scope. Any material scope
change still pauses recruitment and requires re-review.

### Stated limitations

- The adjudicator is qualified by test, not by agreement with human judgement.
  Nobody has checked that its rulings match what a competent reviewer would
  conclude, and no such check is planned. What is claimed is that it applies the
  preregistered rules correctly and identically every time.
- Determinism is not correctness. A rule frozen before the data is still a rule
  someone chose. Nothing here argues the +10 floor is the right floor.
- The `AMBIGUOUS_RETURNED_TO_ARCHITECT` path is a real agent effect on the gate.
  An auditor emitting vague findings on every run would suspend every pass — a
  denial of service on the Architect's attention rather than a threat to the
  result, recorded here rather than dismissed.
- The adversarial auditors have never been run in their new role. Their briefing
  was rewritten, and no auditor has executed against it. What is proven is the
  structure that makes their output non-load-bearing, not their usefulness.

---

## D016-J — ethics determination and baseline-readiness correction

`D016-J` is encoded as `OWNER_DELEGATED_APPROVED_WITH_CONDITIONS`. It is an
owner-delegated project ethics determination, not an IRB approval or federal
exemption certificate. The approved scope is minimal-risk benign behavioral
human-subjects research with U.S. adults age 18+ who can provide their own
legally effective consent. Prisoners, people without consent capacity, minors,
non-U.S. participants, recording, biometrics, health data, participant-device
collection, physical intervention, increased risk, and material scope changes
remain excluded and trigger re-review.

The ethics prerequisite is cleared in the deterministic adjudicator, but the
three substantive human-evidence blockers are unchanged. Baseline qualification
is eligible to open only after the owner supplies a real study-owner contact and
compensation terms to participants. Scored A001 recruitment remains blocked;
attempts remain `0 / 3`, programme state remains `ALIVENESS_UNTESTED`, and human
participant data remains `0`.
