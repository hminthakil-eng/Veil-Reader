# Native paper rebuild — real Reader integration

Status: implementation candidate; physical quality and release remain UNVERIFIED.
Base: canonical main `bfac95f4b82e23b9f277f985bf3282f3e9c584fb`.
Branch: `reader/native-paper-rebuild-20261011`. Acceptance owner: issue #408.

## Why previous attempts failed

Source-confirmed failure paths, distinct from unperformed phone reproduction:

| Failure | Evidence | Rebuild response |
|---|---|---|
| Visual overlay intercepted publication input | Main integrated #474; negative control described in that PR | Keep `readerVisualOnlyInput` at the overlay boundary; native view never owns touch |
| Navigation during Move replaced the source WebView before touchend | #467 and `PaperCurlInputListener` release-only transaction | Reuse the corrected transaction; never navigate during a held fold |
| Vertex/fragment precision mismatch prevents shader link | Unmerged optical fix #468; identical uniforms/varyings had different default precision in main | Reuse its precision/acquired-buffer fix for explicit legacy rollback; new backend uses actual upstream triangle strips |
| A valid short drag release arrives before acquired-source acknowledgement | Main `onDragEnd` required epoch equality immediately; tap path waited | Wait up to the existing 500 ms same-epoch acquisition barrier, preserve release intent, cancel safely on lifecycle/token change |
| Native material demos did not change real book pages | #479/#506 explicitly used fictional bitmaps | Wire the licensed mesh directly into `PaperCurlOverlay` and actual Readium source snapshots |
| Leaf travel stopped inside the screen | Prior GPU target free edge clamps at -0.25 page widths | Finger tracks 1:1 during drag; accepted release extends to 2.08 widths so the entire reflected leaf exits |
| Full pull skipped its release animation | Saturated gesture progress=1 leaves no remaining animation interval | Native release uses its own time fraction so the remaining leaf exits continuously |

No single explanation is asserted for every prior APK. An installed build hash and
phone recording are required to attribute an individual report.

## Successful designs researched

| Primary source | Mechanism | Decision |
|---|---|---|
| https://github.com/harism/android-pagecurl | Adaptive cylindrical CurlMesh, front/back same sheet, drop/self shadows, pointer-based manipulation | Reuse licensed mesh and texture container, pinned below; separate geometry, acquisition and Readium state |
| https://github.com/eschao/android-PageFlip | ES2 cylinder fold, mesh density, edge/base shadows, independent draw/touch lifecycle | Existing #506/#508 remain separate evidence candidates; avoid rebuilding another demo |
| https://github.com/albertoirurueta/irurueta-android-gl-curl | Modern Android curl host and upstream lineage | Existing #479 remains a lab; main already uses its GLTextureView utility |
| https://github.com/readest/readest/issues/5288 | Prepare capture while idle; buffer release until surface ready; invalidate stale prepared surfaces | Reuse Veil's existing idle snapshot preparation; repair the unmatched drag-release wait |
| https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures | Gesture ownership/dispatch | Preserve existing publication input arbiter and visual-only overlay |

## Actual implementation

- `NativePaperGeometry`: pure cylindrical endpoint solver; partial/full roll join
  continuously; LTR/RTL mirror geometry without mirroring front-facing book text;
  full release exits, cancellation returns to the initial edge.
- `NativeMaterialPageCurlView`: licensed `CurlMesh` renders real publication
  pixels in a transparent GLTextureView; deformation/self/drop shadows come from
  mesh topology rather than translating an image. GPU work stays on GL thread.
- Same leaf is used on both surfaces, with underside tint. A destination page is
  never mislabeled as the back of the leaf. No proprietary Kindle/Moon+ code used.
- Source bitmap is borrowed under a generation/revision lease, copied once into
  upstream-owned storage, and acknowledged only after the read. Superseded reads
  are retired; submissions arriving during a draw retain their leases.
- Source presentation is acknowledged only when TextureView acquires the exact
  EGL buffer timestamp, epoch, GL context and viewport. RenderThread probes check
  acquisition after callbacks; fixed delay is never proof of presentation.
- `PaperCurlInputListener` remains the one navigation authority. No held-drag
  navigation; same-epoch first frame required; release commits at most once;
  cancellation retains the source locator; reduced motion remains static.
- Native backend is selected explicitly in debug and benchmark builds. Release
  retains the original renderer and its OFF rollout gate. No failure automatically
  switches to Slide or to the other renderer. Revert this branch to roll back.

The live source page stays underneath a held fold. After release, Readium paints
the destination under the retained source leaf. **Adjacent-destination preview
during a held drag is not implemented**; doing it through the live navigator would
reintroduce the chapter/touchend defect. Independent offscreen snapshot capture is
a separate enhancement, not a claimed capability of this candidate.

## Verification

Local: reading-policy checks (39), canonical paper guard (6), shell syntax and
`git diff --check`. Android build cannot run locally: Gradle distribution network
access unavailable and no Android SDK installed. GitHub CI is the Android oracle;
record its exact source head and test merge SHA separately.

New meaningful checks:
- `NativePaperGeometryTest`: free edge tracks finger, partial/full cylinder join,
  mirrored direction, complete exit, continuous release after saturated drag,
  cancellation, invalid samples.
- `PaperDiscreteTurnLifecycleTest`: quick release before acquisition waits then
  commits exactly once; existing timeout, cancel and chapter-ownership regressions.
- `NativePaperRealReaderInstrumentedTest`: launch production import/Readium/
  ReaderScreen with English LTR and Persian RTL EPUBs; inject actual Android touch events;
  require visible page-pixel deformation while actual locator stays put; reverse
  and release must restore; full release must change the real Readium locator;
  opposite-edge release must reduce actual book progression.
  Requires the real DOM, settled publication location and WebView visual-state
  callback and acquired on-screen paper/ink stable across frames before touch.
  DOM and accessibility alone are insufficient while the entry curtain fades.
  Captures six PNGs per book and any timeout image;
  exports fixture captures before UTP uninstalls the debug app.
- Benchmark build now exercises native backend with the existing full Reader
  performance fixture. A successful emulator test is not physical acceptance.

Required phone gates remain: exact APK/hash; Samsung/Pixel, 60/120 Hz; 100 turns;
slow corners, reverse, first turn, first/last/chapter boundaries; RTL book and UI
independently; text selection/center controls; rotation/reflow/themes/background;
memory, frame timing and subjective paper quality. Keep #408 open until evidence.

## Source provenance and adaptation

Upstream: https://github.com/harism/android-pagecurl
Pinned commit: `7a2c8f152bb4f1b0de3b1aa72b3cb79e1fe8e3bd`.
License: Apache-2.0; original copyright headers retained, full license in
`licenses/harism-android-pagecurl-Apache-2.0.txt`.
Binary builds also carry the license and modification notice in
`app/src/main/assets/open-source-licenses/`.

Copied files: `CurlMesh.java`, `CurlPage.java` only; no source app, demo artwork,
launcher or synthetic counters. `CurlPage` is unmodified. `CurlMesh` adaptations:
extract texture preparation so first upload cannot flatten current drag geometry;
use linear texture filtering for readable ink. Veil integration and endpoint
solver are separate Kotlin files. No license/authentication/content-protection
bypass and no copying of closed-source competitor methods.
