# D016-Y Owner Pet Embodiment

`D016_X_OWNER_RESULT=INCONCLUSIVE_PRESENTATION_INVALID`

`A001_STATUS=OWNER_EVALUATION_NOT_YET_VALID`

`D016_Y_STATUS=OWNER_READY_FOR_NEW_EVALUATION`

The first owner encounter is not an A001 failure. The D016-X engineering visualization did not embody selected organism behavior well enough for a valid subjective aliveness judgment.

## Frozen behavioral boundary

- Starting HEAD: `c678f64afb80e9e17c98a0fb587c04497c298b2e`
- Candidate: `A001_FULL_D016N_V1`
- Candidate Git SHA: `684579130bef5c820f3db9534ffb744654ebf3b4`
- D016-N cohort source changed: `false`
- `SpikeExpressionContractV1` changed: `false`
- AI/model calls: `0`
- Comparator displayed: `false`
- R003-R009: `BLOCKED`

## Before evidence

- Pixel: `Google Pixel 9 Pro XL`, Android API `36`
- Screenshot: `D016Y_BEFORE_PIXEL.png`
- Screenshot SHA-256: `8F22355AC036ED5DC361294A193F73FB590636C1DE114C7ED58B5BCC27228DD7`
- Finding: primitive two-oval creature, diagnostic habitat rectangles, clipped button grid, system-inset overlap and visible no-op object controls made the owner encounter presentation-invalid.

## Owner-only embodiment adapter

The debug-only `OwnerEmbodimentAdapter` consumes the actual transient `StepRecord`:

`choice.action + choice.target + ExpressionFrame`

It owns only normalized screen-space position and facing. It cannot select an action, mutate organism state, change an outcome or feed presentation state back into `SpikeRuntime`.

| Actual selected action | Owner-visible embodiment |
| --- | --- |
| `OBSERVE` | still inspection with a readable head tilt toward the target |
| `ORIENT` | head/body orientation toward the target |
| `APPROACH` | grounded locomotion to a point beside the target |
| `WITHDRAW` | retreat from the target with flattened ears and tucked tail |
| `SEEK_INTERACTION` | approach to the owner edge with attention-soliciting posture |
| `RESPOND_TO_TOUCH` | readable lean/head/tail response |
| `VOCALIZE` | open mouth and nonverbal sound arcs |
| `EXPLORE` | locomotion with lowered investigative head |
| `PLAY` | approach/paw posture and target-toy motion |
| `EAT` | approach the bowl, lower head and visibly chew |
| `REST` | move to the bed and settle |
| `SLEEP` | lie at the bed with closed eyes and sleep motion |
| `RESUME_INTERRUPTED` | purposeful resumed locomotion |
| `RETRY` | distinct head posture with renewed target approach |
| `IDLE_VARIATION` | breathing plus frozen blink/head/weight-shift/tail micro-movements |

## Direct owner interaction

| Visible affordance | Existing event |
| --- | --- |
| touch the creature | `TOUCH` |
| tap the food bowl | `OFFER_FOOD` |
| tap the ball | `PRESENT_OBJECT` |
| compact Call affordance | `CALL` |
| compact Space affordance | `WITHDRAW_ATTENTION` |
| compact Rustle affordance | `STARTLE` |

The previous habitat-object button grid and every no-op visible control are removed. Decorative habitat props are not styled as controls.

## Validation so far

- D016-Y owner harness/adapter tests: `PASSED`
- All 15 `SpikeAction` values have distinct `EmbodiedBehavior` mappings: `PASSED`
- Every visible interaction affordance maps to existing `InteractionKind`: `PASSED`
- Debug APK assembly: `PASSED`
- Debug APK SHA-256: `52FBEF912B1ADFC231FE7D42E1D1F850F6686BED67769E4E1307B70DB3C89F9C`
- Emulator launch: `PASSED`, supplementary only
- Emulator accessibility bounds: content within safe root, no clipped grid
- Emulator screenshot: `UNUSABLE_ATD_BLACK_CAPTURE`, not treated as visual evidence
- Physical Pixel install and launch: `PASSED`; `MainActivity` handed off to `DebugAlivenessActivity`
- Physical Pixel after screenshot: `D016Y_AFTER_PIXEL.png`
- Physical Pixel after screenshot SHA-256: `5ECBC32F1EC4891FF635A9D41902313FFD8725A6492E8DACD0A5FC5449D6BE4A`
- Physical Pixel screen recording: `D016Y_PIXEL_RUNTIME.mp4`; valid H.264, `1008x2244`, `10.003567` seconds
- Physical Pixel screen recording SHA-256: `9E766ADC1125A7E97470A155C37A0BA3620F6E4E572780F5135031E13711E05A`
- Physical interaction exercise: direct creature touch, bowl, ball, Call, Space and Rustle all invoked on-device
- Physical layout inspection: title, instruction, habitat, creature and all three controls are inside the Pixel system insets with no app-content clipping
- Runtime check after interaction exercise: app process alive and foreground; no DLL17 fatal exception or ANR in the inspected ten-minute log window
- Device-context limitation: a Microsoft Teams picture-in-picture call from the work profile remained visible during capture. It was moved over decorative scenery without stopping or changing the call and did not intercept the creature, bowl, ball or compact controls.
- New owner aliveness verdict: `NOT RUN`

External presentation principles were referenced from Nintendogs and Peridot only at the level of pet-first direct interaction and readable physical behavior. No layouts, assets, characters, game mechanics or runtime dependencies were copied or adopted.

No A001 PASS or FAIL is claimed by this record.
