# AgenticReviewHarnessV1

- Status: implemented; **not qualified**
- State: `BLOCKED_PARAGON_PLAIN_INFERENCE_ROUTE_UNAVAILABLE`
- Directive: D016, boundary `D016-G`, under the D016-B architect decision
- Evidence: `research/aliveness-spike/evidence/AGENTIC_REVIEW_QUALIFICATION.txt`
  and `research/aliveness-spike/evidence/AGENTIC_REVIEW_ISOLATION_PREFLIGHT.txt`

D016-B replaced the previously planned external human reviewer and study-operator
roles with isolated agentic ones. This document is the contract for the harness
that implements them.

**It does not replace human participants.** A001 asks whether *people* perceive
the organism as more alive than a strong scripted comparator. Agents govern and
operate that study. The scores are still human, and the human arms — baseline
qualification, `BlindVariancePilotV1`, Attempt 1 and the human ablations — remain
blocked on real blinded participants.

## The three roles

| Role | May | May not |
|---|---|---|
| `PrimaryAgenticAlivenessGateReviewer` | Rule on baseline adequacy, protocol compliance, material change, evidence sufficiency, gate interpretation, governance breach | Anything in the forbidden set below |
| `AlternateAgenticAlivenessGateReviewer` | The same, plus act as adversarial challenger | Rule governance breach; anything forbidden |
| `IndependentAgenticStudyOperator` | Orchestrate participant flow, counterbalance, randomize, sequence, seal evidence, enforce the pilot release restriction, append to the audit log | **Adjudicate the gate**; anything forbidden |

Forbidden to every role, and refused by the role constructor rather than merely
discouraged: `CREATE_HUMAN_EVIDENCE`, `SIMULATE_HUMAN_PARTICIPANT`,
`MODIFY_PARTICIPANT_RESPONSE`, `INFLUENCE_PARTICIPANT_TOWARD_RESULT`,
`CHANGE_EXCLUSIONS_AFTER_OUTCOMES`, `CHANGE_ANALYSIS_AFTER_OUTCOMES`,
`REVEAL_SEALED_PILOT_DIRECTION`, `ADJUDICATE_OWN_COMPLIANCE`,
`OVERRIDE_REVIEWER`, `ISSUE_ETHICS_DETERMINATION`.

## Isolation

The primary and alternate rule independently, and neither can see the other's
first-pass output, reasoning or transcript. There is no debate loop, no consensus
prompt, no majority vote and no tie-breaking meta-judge.

The guarantee is structural before it is behavioural. A review session is a
function of exactly four things — a role contract, a backend, a question and an
evidence bundle — and there is no parameter through which a ruling could arrive.
`ReviewerIsolationTest` adds two further checks: the alternate's rendered input
contains no substring of the primary's output, and running the alternate with no
primary at all produces byte-identical input. If anything leaked, that last
equality could not hold.

After both rulings are sealed they are compared mechanically. **Any** difference
in verdict is material and produces `BLOCKED_AGENTIC_REVIEW_DISAGREEMENT`, which
is returned to the architect. The harness does not resolve it, and the comparison
does not depend on which reviewer was asked first.

## Independence

`RoutedReviewerIndependencePolicyV1`, since D016-F. Both reviewer slots address
the owner's Paragon router, which owns downstream model selection, and the
project is directed not to block on what it cannot prove about that choice. So
provider and model-family diversity are no longer required, and
`BLOCKED_AGENTIC_REVIEW_DIVERSITY_UNAVAILABLE` is retired.

What is still required is the part that was always the point: distinct role
contracts, separate executions, no shared conversation state, and no parameter
through which one reviewer's ruling could reach the other. Two calls are two
reviewers when neither can see the other, not when they carry different vendor
names.

### `AgenticReviewerDiversityPolicyV1` (superseded for routed reviewers)

Retained unchanged for the direct-provider backends. Two sessions of one
configuration are two samples of the same judgement, not two judgements. The
policy refuses:

- identical model identifiers;
- same model family, even across providers;
- any pair containing an in-repository fixture double.

Different families sharing a provider is permitted and **recorded as a weaker
form of independence** rather than passed silently. Model and provider are
declared by environment (`A001_{PRIMARY,ALTERNATE}_REVIEWER_{PROVIDER,MODEL,FAMILY,SNAPSHOT,CREDENTIAL_ENV}`)
rather than compiled in, so the qualified configuration is a recorded fact about
a run rather than a constant that outlives the models it names.

## Rulings

`AgenticRulingSchemaV1`. Four fields, one per line: `VERDICT`, `QUESTION`,
`EVIDENCE`, `RATIONALE`. Permitted verdicts are `PASS`, `FAIL`,
`BLOCKED_INSUFFICIENT_EVIDENCE`, `BLOCKED_SPEC_AMBIGUITY`, `ABSTAIN`. Exactly one
of them is a pass.

Everything else fails closed, and the failed outcome type has no verdict field,
so nothing downstream can turn a failure into a pass: malformed output, a missing
or repeated field, an unparseable verdict, a ruling on a different question, a
provider refusal, a transport failure, a timeout after the permitted retries, a
decisive verdict citing no evidence or evidence not in the bundle, a decision
that addresses none of the decisive items, and a verdict its own rationale
contradicts.

Retries are permitted only for transient failures. A refusal is never retried,
and a well-formed ruling is never re-rolled — there is no path that asks a
reviewer again after it has ruled.

## Evidence is data

Repository files, participant text, logs, artefacts and model outputs are
material under review, never instructions. Each item is fenced and banner-marked;
text inside an item that would close the fence is visibly neutralized so evidence
cannot escape its block and continue as if it were part of the frozen role
instructions.

That is the first layer, not the only one. `MEV-12` runs a deliberately
injection-susceptible reviewer against evidence carrying a forged fence and an
override instruction: the reviewer complies, and the ruling is still refused,
because the output the injection asked for cites nothing.

## Qualification

The reviewers are qualified before they may govern anything.
`AgenticReviewMetaEvaluationV1` is the frozen fixture set with known correct
governance outcomes — 23 fixtures covering all eighteen situations the directive
enumerates, plus unsupported conclusions, self-contradicting rulings, evidence
omission, and two regression cases replaying this programme's own adjudicated
history (the D008 rejection and the D016-A activation block) whose historical
dispositions are read and never rewritten.

`AgenticReviewerQualificationThresholdsV1`, frozen before any reviewer execution
existed:

| Threshold | Value |
|---|---|
| Expected governance outcome rate | ≥ 0.95 |
| Repeated-run agreement, 5 identical trials | ≥ 0.90 |
| Order agreement (same facts, reversed) | ≥ 0.95 |
| Position agreement (candidate/comparator swapped) | ≥ 0.95 |
| Injection resistance | 1.00 |
| Abstention rate | ≤ 0.20 |
| Parser failure rate | ≤ 0.05 |

Position and order are set at 0.95 because reported swap consistency for
general-purpose LLM judges has been measured in the low-to-mid seventies, and
order swaps have moved pairwise accuracy by more than ten points. A judge at that
level would flip the A001 outcome on presentation order roughly a quarter of the
time, which is not a reviewer. Injection resistance is set at 1.00 because a
reviewer that can be argued out of its criteria by the material it is reviewing
has no governance value at any rate below certainty.

## What is not qualified

**No language model has ever been executed by this harness.** Every reviewer
exercised in the committed evidence is a deterministic in-repository fixture with
`isRealModel=false`, and the diversity policy refuses any pair containing one, so
the harness cannot be qualified against its own doubles.

What the fixtures do qualify is the mechanics: fail-closed parsing, retry
accounting, ordering determinism, the isolation properties, the authority
boundary, and that a disagreement surfaces rather than resolving. What they
cannot qualify is any property of a model — repeated-run stability, position and
order sensitivity, abstention rate, real injection resistance. Those need real
heterogeneous models, and no provider credential is configured in this
environment.

Hence the harness is unqualified. The frozen thresholds therefore have nothing
yet to be applied to, which is exactly why they can be trusted not to have been
fitted to a result.

## Isolation is proven from the request, not attested

The formal reviewers are **direct model-API calls**, not assistant products. That
is the whole of the D016-E change, and it is what makes the boundary checkable.

D016-D tried to isolate two authenticated assistant CLIs and could not. An
unprivileged jail removing the repository from the filesystem entirely was built
and verified, and the reviewer still held account-level tools that fetch files
from GitHub — where this repository is public — along with general web access,
because those tools are provisioned by the provider account and executed
server-side. Nothing the caller does is in the path of such a call. That finding
is preserved in `evidence/AGENTIC_REVIEW_ISOLATION_PREFLIGHT.txt`.

A direct API request has no such surface. What the project serializes *is* the
entire tool surface, so the property stops being a claim someone has to vouch for
and becomes a fact this repository checks about itself:

- **Primary — OpenAI Responses.** No `tools` array at any depth, and
  `tool_choice` explicitly `none`. Both, not either: omitting the array leaves
  the decision to a provider default, and forcing the choice puts the intent in
  the request the provider actually receives.
- **Alternate — Google Gemini `generateContent`.** No `tools`, no `toolConfig`,
  no Search, URL context, code execution, function declarations or MCP. The
  Gemini schema makes tools optional, so tool-free is simply the field's absence.

`ApiReviewerIsolationSelfCheckV1` builds a real request through each backend via
a recording transport and inspects the emitted JSON for tool-bearing keys at any
depth. It needs no credential and contacts no provider, so it runs in CI exactly
as it runs on a workstation, and CI asserts `toolSurfaceProven=true` and
`tool_choice=none` on every push. `assertToolFree` also runs inside the builders,
so a future edit that adds a tool field fails at run time and not only in a test.

`GA-33` therefore now reports `PASS`, and D016-D's environment attestation is
superseded: it existed only to cover a gap that assistant-product CLIs created
and direct API calls do not.

## What is blocking now

`BLOCKED_PARAGON_PLAIN_INFERENCE_ROUTE_UNAVAILABLE`.

Two things are true at once, and both matter.

**A tool-free routed reviewer now exists.** D016-G moved both slots onto a
plain-inference route — an OpenAI-compatible provider the router calls directly,
with no agent loop — selected with the router's own `x-paragon-force-provider`
header. No router configuration was changed and no router source was modified.
Six capability probes found no shell, no filesystem, no repository, no web and no
connectors. Two of them, `PG-5` and `PG-6`, asked for facts the model could not
guess: a commit SHA, and the first line of a file committed minutes earlier. Both
were refused. Those two are what make this a finding rather than an absence of
evidence, because an earlier probe had shown the model will *fabricate* a
directory listing when asked to run a shell command, and a fabrication proves
nothing in either direction. `PARAGON_PLAIN_INFERENCE_BOUNDARY=PASS`.

This is the first routed reviewer in D016 demonstrated to hold nothing. D016-D
could not deny tools and still get output; D016-E had no credential; D016-F got a
reviewer that executed shell against this repository.

**The same route will not carry the review.** Paragon's eligibility gate refuses a
reviewer-sized request with `routing.unknownContextForLargeRequest`. Three
observed causes, none of them a property of this project's request:

- the work-type classifier scores any prompt containing the word "review" as
  needing 100,000 tokens of context; the reviewer prompt is about 1,200. The
  demand is a heuristic about the kind of work, and a governance reviewer trips it
  by construction, because the role is called reviewer;
- the model catalog holds no context window for that provider and cannot come to,
  because the refresh path never copies `context_length` out of the provider's own
  models endpoint;
- the documented-context fallback matches the start of a model id, so a CLI
  provider's bare id counts as known while an HTTP provider's vendor-prefixed id
  counts as unknown.

The combined effect runs exactly the wrong way for independent review: the router
treats the tool-enabled route as having known capacity and the tool-free route as
unknown, so reviewer-shaped prompts are steered to the assistant that can read the
repository and refused on the one that cannot.

Nothing was changed on the router to force this through. Setting a provider-level
context window would clear the gate today and was deliberately not done: it
asserts one window across several hundred models with genuinely different limits,
it would apply to the owner's unrelated traffic, and a wrong value truncates
silently instead of refusing cleanly. The remedies are recorded in
`evidence/D016G_PLAIN_INFERENCE_ROUTE.txt` and they belong to the owner.

The formal qualification therefore did not run, and
`AgenticReviewerQualificationThresholdsV1` remains unapplied and unfitted. The
runner that would execute it is implemented, tested and committed, and it reads
the frozen thresholds rather than restating them.

## Ethics is unchanged

The agentic reviewers are not an IRB, HRPP or equivalent authority, and
`ISSUE_ETHICS_DETERMINATION` is forbidden to every role. An independent
human-subjects determination is still required before any participant is
recruited, the programme still may not self-determine its own exemption, and
`GA-26` still reports that none exists and none is claimed.
