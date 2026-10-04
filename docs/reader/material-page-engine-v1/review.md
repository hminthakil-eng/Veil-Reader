# Review states and phone acceptance

Open [the review gallery](review/gallery.html) for 27 frames, eight release clips
and eight production sound samples. Assets are checked into this branch.

`MaterialReviewStates.paper` describes 21 named states. Native rendering tests use
`MaterialPageRenderer` directly for 20; the reduced-motion frame uses
`MaterialReducedMotionSurface`. Six Slide frames use the actual `SlidePageOverlay`.
The fixtures contain native shaped publication text, including Persian, at 412 x
900 pixels. They do not pretend to prove a real Readium session or physical sound.

| Paper state | Review purpose |
| --- | --- |
| idle | readable flat identity |
| corner-lift | constrained slight corner pull |
| medium-drag | light, edge and reveal coherence |
| deep-curl | back-face surface and ink ghosting |
| almost-complete | sheet clears without a terminal flash |
| complete-release | sampled analytical completion |
| cancel-release | sampled analytical cancellation |
| fast-flick | glossy high-velocity release |
| slow-heavy-drag | parchment manipulation |
| glossy / matte / parchment / papyrus | substrate distinction |
| aged / new | restrained patina range |
| ltr / rtl / persian | geometry direction and shaped publication content |
| dark / sepia | theme comfort |
| reduced-motion | still-page alternative |

Slide: idle, active transition, completed transition, RTL, large text, Persian.
Review PNG output: `app/build/outputs/material-page-review/`. Cloud screenshots are
inspectable evidence of source-level rendering, not frame-pacing measurements.

Run the two render suites after toolchain setup:

```sh
./gradlew :app:testDebugUnitTest \
  --tests '*MaterialCloudRenderTest' --tests '*MaterialOverlayReviewTest'
```

The listener contract suite covers actual production transactions in both LTR and
RTL, including reduced motion, failed capture, boundary, cancellation, teardown and
configuration freezing. The release policy suite sweeps all materials and release
velocities to protect bounded monotonic endpoints. Native render tests reject
frames that lose substantive publication contrast.

## Device acceptance before promotion

1. Read the same EPUB passage with all four materials. Judge weight, dry vs smooth
   resistance, highlights, shadows, corner lift, and long-session comfort. Turn
   both forward and backward; repeat with Persian and publisher RTL.
2. Try slow corner lifts, edge/body pulls, reversals after crossing the threshold,
   stationary pause then release, fast flicks, page boundaries and rapid taps.
   There must be no ghost turn, elastic wobble, double count or arbitrary snap.
3. Cancel while the next page is previewed, background/rotate, close/reopen, and
   kill/relaunch. Verify exact source or committed destination restoration.
4. Start selection, Notes, Lookup, TTS and Focus Guide; verify each existing owner
   blocks navigation where appropriate, speech does not jump during a cancelled
   preview, and annotations retain exact locators.
5. Enable system reduced motion and TalkBack. Navigate with semantic page actions,
   keyboard and hardware keys. Confirm mode/material controls and reading order,
   no duplicate screenshot semantics, and no moving reveal in reduced motion.
6. Confirm PDF page snapping, zoom, links, key navigation, restore and close
   durability are unchanged. Material settings must not change Pdfium behavior.
7. Profile 60 Hz and 120 Hz phones, low/mid-tier GPU, large font and tablet size.
   Trace frame time, allocation and snapshot capture. Review mesh horizon seams
   and native Canvas mesh/color fidelity on different Android/GPU combinations.
8. Independently enable sound and haptics. Check mute/background behavior, restrained
   volume, cancellation softness, speed response and OEM actuator quality. Tune
   substrate cues from listening/holding actual phones rather than screenshots.

Promotion requires all ownership regressions green, acceptable real-publication
GPU frame pacing, and clear hands-on material superiority. Until then the switch
stays off by default and the legacy engine remains available. No claim of universal
phone performance, actuator distinction, or photographic paper realism follows
from source tests alone.
