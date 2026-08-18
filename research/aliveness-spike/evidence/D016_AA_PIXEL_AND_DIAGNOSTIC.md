# D016-AA corrected-candidate evidence

## Scientific boundary

`D016-Z` owner result: `OWNER_ALIVENESS_FAIL_TEMPORAL_AGENCY_AND_CAUSAL_ENGAGEMENT`.
This is a valid owner aliveness failure of the D016-N-derived candidate. D016-X and
D016-Y remain presentation-invalid history. D016-AA is a new corrected candidate;
it does not reinterpret or overwrite D016-N evidence and does not establish A001.

## Root cause and correction

The owner runtime advanced one virtual-minute organism tick every 200 ms. The
pre-change voluntary commitment was 6 ticks, interaction responses were committed
for 1 tick, and maximum engagement was 15 ticks. Pending stimuli interrupted
ordinary commitment and were cleared after selection. D016-AA keeps physiological
time and the 200 ms presentation cadence unchanged, but separates bounded visible
intention duration from that clock:

- voluntary intention commitment: 30 ticks;
- owner acknowledgement episode: 12 ticks;
- per-object engagement bound: 45 ticks;
- Tier 0/1 safety and critical-physiology interruption remains immediate;
- interaction response is selected once, then continues as a bounded episode;
- prior voluntary work remains resumable when an interruption does not change the
  organism's priorities.

## Deterministic diagnostic

Source: `D016_AA_TEMPORAL_AGENCY_DIAGNOSTIC.txt`.
The diagnostic records the pre-change contract beside post-change measurements for
three seeds, exercises passive time plus TOUCH, CALL, OFFER_FOOD, PRESENT_OBJECT,
WITHDRAW_ATTENTION and STARTLE, and checks identical-seed replay. Post-change
action A-B-A oscillation was 0, 1 and 2 cases across the three seeds; the bounded
owner episodes measured 12 ticks for acknowledged interactions; STARTLE selected
`WITHDRAW` immediately; and deterministic replay passed. These metrics do not
constitute an A001 verdict.

## Pixel evidence

- Device: Google Pixel 9 Pro XL, API 36, serial `49121FDAS0025V`.
- APK SHA-256: `5C3BBE01AE9BFB881A9EB9AD43690A5D9434276AC868FDDC2C9679718EF3A9C3`.
- Launch screenshot SHA-256: `F02D414434284CBC20D98C1492222664A2B2970DF4F7B09DC0C4BDB712F3BD3A`.
- Runtime recording: 23.997989 seconds, 27,846,995 bytes,
  SHA-256 `777F09345EFD46D0D63D35787BF069814700A51FC1980020CDE4536A1CF9A37E`.
- Installation succeeded; `DebugAlivenessActivity` remained foreground and the
  process remained alive after autonomous time and owner touch, food, toy and
  withdrawal interactions.
- Inspected logcat contained no DLL17 fatal exception or ANR.

## Boundary and limitation

The corrected candidate is ready for a fresh owner encounter. The owner has not
yet supplied that fresh subjective verdict. `A001_STATUS` therefore remains
`OWNER_EVALUATION_STILL_NOT_VALID`; no A001 PASS/FAIL and no R003-R009 transition
is claimed. The unrelated Windows path-separator failure in the existing R012
`AndroidLocalKeyBootstrapTest` remains preserved and was not changed.
