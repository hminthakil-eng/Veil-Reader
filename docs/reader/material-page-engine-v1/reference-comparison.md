# Code comparison and refinement — 2026-10-04

This is a source-level comparison with published page-turn implementations, not a
claim that Veil or the reference apps have passed phone acceptance. Reference code
was inspected at the immutable revisions below; no reference engine or dependency
was imported into the production Reader.

## Pinned references

| Reference | Revision / license | Inspected source | Why relevant |
| --- | --- | --- | --- |
| [harism/android_page_curl](https://github.com/harism/android_page_curl/tree/7a2c8f152bb4f1b0de3b1aa72b3cb79e1fe8e3bd) | `7a2c8f152bb4f1b0de3b1aa72b3cb79e1fe8e3bd`, Apache 2.0 | `CurlMesh.java`, `CurlView.java`, `CurlRenderer.java` | cylindrical curl, scan-line subdivision, independent front/back rendering and shadows |
| [eschao/android-PageFlip](https://github.com/eschao/android-PageFlip/tree/6c361e0932c80c360fdf5f7de06963031e8c26d2) | `6c361e0932c80c360fdf5f7de06963031e8c26d2`, Apache 2.0 | `PageFlip.java`, `FoldBackVertexes.java`, fold-back shaders | separate curved front/back vertex streams, slope/vertical geometry, radius-dependent shadow width and texture coordinates |
| [Nodlik/StPageFlip](https://github.com/Nodlik/StPageFlip/tree/ab30ecc1d9f6d98de1a99b8e296469382f41c120) | `ab30ecc1d9f6d98de1a99b8e296469382f41c120`, MIT | `FlipCalculation.ts`, `Flip.ts`, `Render.ts`, `CanvasRender.ts` | constrained corner motion, clipping/reveal, animation serialization and resize handling; published [web example](https://nodlik.github.io/StPageFlip/) |

The Android samples were inspected, not rebuilt or installed: their old GL/Gradle
stacks are not evidence for current Veil frame pacing. StPageFlip source was
compiled with TypeScript 4.9.5 and its actual `FlipCalculation` executed for
**1,188 cases**, all accepted, no non-finite coordinates: 99 pulls × 2 directions
× 2 corners × 3 viewports (412×900, 900×412, 360×1800). This exercises geometry,
not a DOM/browser interaction or a phone. [Reproducible smoke test](reference-smoke.cjs).

## Code-to-code decisions

| Reference mechanism | Veil before this refinement | Final implementation / evidence |
| --- | --- | --- |
| harism `CurlMesh.curl`, cylinder arc mapping around lines 417–475; eschao `computeVertexesWhenVertical` and `computeVertexesWhenSlope` | orthonormal fold basis and `r*sin(angle)`, `r*(1-cos(angle))`, with planar continuation | retained: it preserves material arc length and text order; no decorative perspective transform or rubber spring substituted |
| harism separate front/back counts in `onDrawFrame`; eschao separate `mFoldFrontVertexes` / `mFoldBackVertexes`, independent back shader | both faces drew the *entire* overlapping bitmap mesh, then clipped its projected footprint | `MaterialFaceMesh` clips each source triangle at the interpolated normal horizon; separate `Canvas.drawVertices` submissions own front ink, back body and back ghost ink. Source-area conservation tested over 984 combinations |
| StPageFlip `checkPositionAtCenterLine` constrains corner reach | bounded slope could still detach the opposite binding endpoint on tall/narrow viewports | `CylindricalPageMesh` bounds slope by fold/binding reach, derived from `distance(0,y) <= 0`; corner/aspect/RTL sweep protects the remaining flat binding |
| harism shadow vertices and eschao radius-bounded base/edge shadows | silhouette shadow and thickness already independent of internal grid | retained restrained silhouette/contact approximation; no per-cell dirty seams or full-frame blur introduced |
| eschao mask/shadow texture and normal response | per-material specular/roughness LUT, source grain and restrained ghost ink | retained and strengthened High Contrast: less grain, ghost ink and tonal attenuation; structural edge remains. Idle image is now exact publication pixels |
| StPageFlip serialized `startAnimation` / `finishAnimation` | Reader already serializes navigation and separates preview from commit | preserve Reader cancellation rather than copying reference `finishAnimation` semantics, which invokes completion. Reduced/no-snapshot turns now freeze material for the whole transaction too |
| StPageFlip `Render.update` recomputes orientation and bounds | existing old strip had resize safety; new mesh could stretch an obsolete capture | new mesh rejects a changed captured viewport and reveals the live publication; cancellation still restores the exact Readium locator, with no new persistence owner |
| reference desktop/web timing and sample index navigation | analytic mass/stiffness release and Readium locator transactions | retained monotonic time-based settle, fresh flick/outward cancel and exact endpoints; sanitize release endpoints to prevent non-finite runaway animation |
| GL texture/vertex reuse in both Android implementations | grid reused per frame, but new face buffers could be constructed at each overlay re-entry | bounded primitive face arrays prepared before interaction and retained across turns while the material Paper overlay is available; cached BitmapShader follows the reused captured bitmap |
| Reference ordinary page-turn gesture feedback | optional Veil terminal sound/haptic policies | cancel cue now arrives after locator restoration at release onset, before settle. Lift/threshold remain tactile-only and cannot mask terminal audio |
| StPageFlip is a paper simulation | Slide is intentionally a separate mode | no reference curl code enters Slide. Direct finger translation, opaque content, narrow directional shadow and monotonic fast tween stay independent |

Source partitioning conserves the full texture area even when a triangle straddles
the horizon. Adjacent front/back pieces share interpolated positions, UVs and
colors; neither renders a whole overlapping mesh. The projection remains a
piecewise-linear approximation of the same inextensible cylinder. This is not
finite-element simulation, collision physics, or measured physical material.

## Reproduced defect, rather than an inferred improvement

A temporary native-Skia probe ran the **previous production mesh renderer** from
commit `2c2cb2f1` unchanged. Its source fixture used BLUE back-facing texels and RED
occluded front texels, with translucency exaggerated to 1 solely to detect face
ownership. At RTL mid-turn, the sampled backside pixel was **`-65536` = pure RED**.
The expected BLUE dominance assertion failed. The old source was not altered to
manufacture the failure.

The permanent `occludedFrontInkNeverLeaksIntoBackFaceInEitherDirection` test uses
the same geometry/fixture for both directions and passes with the face partition.
The temporary old-renderer copy and deliberately failing probe were removed from
the app test tree after the experiment. [Baseline output](evidence/legacy-rtl-leak.txt)
records the observed result. This exaggerated test does not change preset translucency.

## Platform contract checked against the official API

Android's [hardware-acceleration support table](https://developer.android.com/develop/ui/views/graphics/hardware-accel#drawing-support)
explicitly lists `drawVertices()` hardware support starting at **API 29**, whereas
colored `drawBitmapMesh()` is supported from API 18. Veil's minSdk is 26. Therefore
API 26–28 selects the still-premium live-edge path for the new mesh setting, with
no captured-page preview, full-frame CPU render or bitmap loop. API 29+ can use the
new face renderer. Old curl/strip, Slide, Scroll, Paged and PDF remain available.
No Android version is silently left with an unsupported GPU operation.

The [Canvas API](https://developer.android.com/reference/android/graphics/Canvas#drawVertices(android.graphics.Canvas.VertexMode,int,float%5B%5D,int,float%5B%5D,int,int%5B%5D,int,short%5B%5D,int,int,android.graphics.Paint))
uses a vertexCount of float entries (two per vertex); the production submission
uses `face.count * 2`. Source UVs are in bitmap coordinates and scale with the
bounded snapshot, keeping publication texture order independent of geometric RTL.

## Preservation and remaining judgments

No new Room, persistence, Readium, Pdfium, TTS or annotation ownership was added.
Both earlier renderers and the persistent opt-in gate remain. The tests exercise
exact preview/cancel locators, commit-once, close/mode teardown, failed capture,
reduced motion, configuration freeze, keyboard/mode regressions and source pixels.

Native Skia, the upstream math smoke test and full Gradle checks cannot establish
sustained GPU performance, WebView capture fidelity, OEM haptics, sound quality or
long-session visual comfort. Those remain real-device gates; no default promotion
is made from a source comparison or a claim of universal freedom from bugs.
