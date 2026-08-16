# DataHandlingAndPrivacyV1

- Status: `READY_FOR_HUMAN_EVIDENCE_WITH_D016_J_CONDITIONS`
- Version: 1
- Applies to: A001 Attempt 1, `BaselineQualificationProtocolV1`,
  `BlindVariancePilotV1`

How participant data is collected, separated, stored, released and destroyed.
Written before any data exists, because the separations below cannot be
retrofitted once a spreadsheet has both halves in it.

> No institutional review board, ethics committee or data-protection authority
> has reviewed this. None is claimed.

---

## What is collected

| Item | Class | Where it lives |
|---|---|---|
| Participant number (`P-041`) | Pseudonymous key | Study data set |
| Two graded ratings, 0–100 | Study data | Study data set |
| Forced-choice and distinctiveness answers | Study data | Study data set |
| Session interaction events and timestamps | Study data | Study data set |
| Completed-session fraction, technical-failure flag | Study data | Study data set |
| Prior-pool flag (pilot or baseline participant) | Eligibility | Enrolment register |
| Name and contact detail, only if needed to schedule or pay | Identifying | Enrolment register |
| Signed consent form, including name and signature | Identifiable consent record | Separate consent-record storage |

**Not collected at any point:** audio, video or images of the participant;
demographics; free-text about the participant; anything from the participant's
own device; any identifier beyond what scheduling and payment require.

## The separation that matters

Three separately controlled records, never joined:

- **Enrolment register.** Names, contact details, appointment times, payment
  status, prior-pool flags. Held by the study operator. Destroyed for each
  participant once payment is complete and eligibility checks are finished.
- **Study data set.** Participant number, ratings, session events. Contains no
  identifying information. Retained indefinitely.
- **Signed consent record.** Name, signature, consent initials and date. Stored
  separately from both the enrolment register and study data; it never contains
  or exposes the participant-number mapping. Retained for three years after
  completion of the A001 human-study programme, then securely destroyed. This is
  a conservative project rule, not a claim that 45 CFR 46.115 legally governs
  this currently non-covered internal determination.

The identity-to-participant-number mapping exists only inside the enrolment
register and dies with it. The signed consent record is separately identifiable
but never contains or exposes that mapping. After the enrolment register is
destroyed, a study record cannot be traced to a person — which is also why a
withdrawal request after analysis cannot be honoured, and why the information
sheet says so plainly rather than promising otherwise.

## Retention and destruction

| Data | Retention |
|---|---|
| Enrolment register entry | Until payment and eligibility checks are complete, then destroyed |
| Signed consent record | Three years after completion of the A001 human-study programme, then securely destroyed |
| Study data set | Indefinite. A negative result must stay checkable and may not be quietly deleted. |
| Withdrawn participant's data, withdrawn before analysis | Destroyed, and the destruction recorded as an exclusion count |
| Variance-pilot outcome data | Sealed through all scored attempts, then retained sealed |

## Access

| Party | Sees |
|---|---|
| Study operator | Enrolment register and full study data set |
| Study owner / ethics contact | Consent records and study records only as needed for the approved protocol and complaint handling |
| Deterministic A001 gate | The sealed baseline qualification result and freeze hash |
| **FULL team** | Scored-attempt study data after the attempt closes; from the variance pilot, only `pairedDifferenceSd` and a validity flag |

The last row is the load-bearing one and it is enforced structurally rather than
by policy: the released pilot type carries two fields, the sealed analysis type
is private, and two pilots with opposite outcomes produce byte-identical output.
See `BlindVariancePilotV1` and `BlindVariancePilotSealTest`.

## Publication

Aggregate results only: counts, means, standard deviations, intervals, p-values,
exclusion counts by reason. No participant-level record is published, and no
combination of published figures is permitted that would let a single record be
reconstructed from an arm with few participants.

Negative and failed attempts are published on the same terms as a pass. This is
a retention rule, not an aspiration: `AlivenessProgramGateV1` forbids deleting a
failed attempt, reclassifying it as pilot data, or pooling it selectively into a
later one.

## Security

| Control | Requirement |
|---|---|
| Storage | Study data set, enrolment register and signed consent record on separate controlled storage; never in one workbook or one directory |
| Transfer | No participant data leaves the operator's control except as aggregate results or the sealed pilot release |
| Backups | Preserve all three-record boundaries; a backup that merges enrolment, consent and study data defeats the separation |
| Incident | Any unauthorized joining of any two records, exposure of the consent record, or comparative pilot disclosure to the FULL team is `A001_GOVERNANCE_BREACH` and freezes the A-track |

## Honest limits

1. These are written controls. This repository enforces exactly one of them in
   code — the pilot information barrier. The rest depend on the operator doing
   what the document says, and no prose prevents an action.
2. There is no named data controller in this repository; the real study-owner contact is supplied through booking/consent before any session.
3. No jurisdiction-specific legal analysis has been done. Whether this study
   needs a lawful basis, a privacy notice in a particular form, or a data
   protection impact assessment depends on where it runs, and that is not
   determined.

## Blocked

- **Data controller and operator.** Unassigned.
- **Jurisdiction and applicable data-protection regime.** Undetermined.
- **External ethical or institutional approval.** None exists.
