# Open-source Paper Engine Arena — physical-material replacement track

**Date:** 2026-10-10  
**Status:** EXPERIMENT / NOT INTEGRATED / NOT PRODUCT-ACCEPTED  
**User evidence:** Current selected Paper on the phone still feels like Slide. Existing #468 automated CI was green but is *not* a tactile-device acceptance. User requested a ready-made Internet engine instead.

## Decisions and candidates (externally verified)

| Candidate | Rendering / controls | License | Readium integration risk | Decision |
| --- | --- | --- | --- | --- |
| [Irurueta native GL Curl](https://github.com/albertoirurueta/irurueta-android-gl-curl) v1.1.6 | Native `CurlTextureView` with independent front/back textures and interactive touch-controlled 3D mesh; Maven Central published. Supports Compose/Android View interop | Apache-2.0 | Medium–high: bitmap page provider, real previous/current/next EPUB snapshots, hardware pressure/motion, gesture arbitration, GPU lifecycle | **First live device A/B candidate**. Prototype is debug-only with invented sample page artwork; deliberately separate from the Reader |
| [eschao PageFlip](https://github.com/eschao/android-PageFlip) | OpenGL ES 2.0 curl, adjustable fold/base shadow, mesh density and click regions; uses a `GLSurfaceView` integration | Apache-2.0 | High: full surface and custom texture/render ownership, coexistence with Readium WebView | Next independent native candidate if Irurueta physical feel is insufficient |
| [oleksandrbalan pagecurl](https://github.com/oleksandrbalan/pagecurl) | Jetpack Compose-based curl, state-driven composables and drag gestures; Maven Central | Verify current root LICENSE before reuse | High for Readium: arbitrary Compose child pages not equal to live EPUB WebView pages | Alternate UX/gesture comparison; do not silently replace native Reader |
| [Peyilo libreadview](https://github.com/Peyilo/libreadview) | Multi-mode Android reading views incl. iBook and Google curl / slide | MIT | High: custom reader surface may conflict with current Readium navigation and annotations | Study interaction patterns, not a drop-in Reader replacement |

## Higher-fidelity contender added: eschao PageFlip (unverified)

**Decision hierarchy:** PageFlip is the **visual-physics candidate** because its OpenGL mesh exposes semi-cylinder radius, fine mesh pixels, independent fold-edge/base shadow color and widths, backside fold masking and exactly defined finish/cancel states. Irurueta is the **more native-integration-friendly contender** because `CurlTextureView` is part of ordinary Android composition and accepts separate front/back textures. Neither is product-green without physical evidence.

**Second, separate DEBUG launcher:** `Veil · Premium PageFlip Lab` is implemented by `PageFlipPremiumLabActivity` using the existing JitPack repository and `debugImplementation("com.github.eschao:android-PageFlip:1.0.2")`. The lab exposes original fictional paper fixtures, native GLSurfaceView surface, 6-pixel geometry spacing, 0.65 fold semi-perimeter, independent fold/base shadows and finger-driven release. In the lab, backing textures are uploaded on the GL thread, intermediate ARGB bitmaps are recycled after synchronous GL upload, and accepted/canceled backward turns are treated transactionally. This is **not** production Readium integration.

**Dependencies and risk:** upstream PageFlip 1.0.2 is from a significantly older Android/Gradle era and some developers reported JitPack dependency resolution failures. The library must resolve and compile with Veil Android Gradle versions; a green release-only lane cannot substitute for the debug variant. A dependency issue is a blocker, not permission to silently remove the premium candidate.

**Side-by-side physical scoring (record video, do not guess)**:

| Criterion | Irurueta lab | eschao PageFlip lab |
| --- | --- | --- |
| Genuine ink displacement with finger | UNVERIFIED | UNVERIFIED |
| Fold silhouette / correct corner lift | UNVERIFIED | UNVERIFIED |
| Contact shadow changing with lift | UNVERIFIED | UNVERIFIED |
| Crease-edge shadow and back-side feel | UNVERIFIED | UNVERIFIED |
| Reverse/cancel without visual jump | UNVERIFIED | UNVERIFIED |
| Texture memory bounded after 100 turns | UNVERIFIED | UNVERIFIED |
| 60/120Hz device frame pacing | UNVERIFIED | UNVERIFIED |
| EPUB Readium prefetch / durable locator | NOT CONNECTED | NOT CONNECTED |
| RTL and chapter/spine boundary | NOT CONNECTED | NOT CONNECTED |
| Compiler+device QA | PENDING | PENDING |

Treat the generated APK as *sample only*. Do not silently route the main Reader to the winning lab; use an explicit feature gate, real snapshots and clean rollback.

## Implementation in this pilot

Only the **debug variant** depends on `com.irurueta:irurueta-android-gl-curl:1.1.6`, without vendoring upstream source. Its Apache-2.0 license stays attributable to the original project.

Debug APK gains a separate launcher: **Veil · Curl Engine Lab**. The lab uses native two-sided bitmaps containing original fictional editorial paper fixtures, 20-split curl geometry and upstream shadows with default interaction. It is **not** connected to actual EPUB/PDF, library, notes, sync, user books, progress or current locator. Release, benchmark and production Reader modes remain completely unchanged. This is intentional: prove the actual prepared 3D material *before* risking user navigation or copying a renderer into the live product.

Run comparison on the same device and 60/120Hz display mode against canonical #468: slow pull, reverse mid-drag, finger tracking, crease and back-face, true geometric displacement of ink, parallax/contact shadows, front/back sides, release/cancel, ten rapid turns, resize/rotation, return from background, GPU reset, memory and thermal behaviour.

## Required Reader migration contract (no shortcuts)

1. **Snapshot bridge:** Readium retains sole ownership of locator and selection. Prepare prev/current/next *genuine* WebView content snapshots asynchronously with source revision, viewport, theme, font, chapter and RTL identity. A native GPU engine can only receive immutable validated bitmap leases. Never precommit a Readium page just to fake a texture.
2. **Gesture authority:** reader arbiter classifies edge/corner vs center chrome vs selection; do not mount an input-capturing visual `AndroidView` over the publication unless pointer routing is characterized and accessible. A turn has one DOWN → MOVE → UP/CANCEL transaction, exactly one durable Readium navigation commit **after** accepted release, and rollback on cancel.
3. **GPU/texture policy:** match acquired buffer and source revision on every generation; add timed-out/failure UI with no silent Slide fallback. Limit texture dimensions and memory; beware that upstream `CurlPage.setTexture` takes ownership/recycles bitmaps. Never hand shared or previously recycled instances to two pages. GPU resource changes must not corrupt the original Readium buffers.
4. **Visible quality:** real geometrical deformation across 10–90% progress (use patterned-ink raster comparisons), lit back side and page thickness, appropriate edge and contact shadows, no destination flash, believable tap grip, corners and opposite direction. Respect reduced motion and RTL.
5. **Device gate:** owner tactile acceptance on Pixel and Samsung + low/mid device/tablet. Keep #408 `UNVERIFIED` until actual 60/90/120Hz, frame pacing, brightness/themes, rotation, background, process death, EPUB spine navigation and accessibility evidence. Benchmarks from emulator alone are not a release pass.
6. **Rollout:** PR for real Reader adapter only after lab demonstrates a convincing fold and traceable real GPU frames; feature-switch behind a *new* named experimental renderer; do not merge or delete the existing curl candidate while comparisons are unresolved.

## Test execution

1. Build **debug APK** from this branch using Android CI. Release dependency graph must NOT include Irurueta.
2. Launch separate `Veil · Curl Engine Lab` icon. It intentionally contains **only synthetic pages**.
3. Swipe from the page's right edge to left; try mid-gesture reversal and repeated turns. Record a continuous screen capture and device/refresh/motion settings.
4. On visible slide-only, ghosting, improper back page, crashes or no turn, mark lab **RED** and investigate source/lifecycle. A green Gradle build is not a green product.
5. Compare videos to #468 and score actual material feel before beginning live Readium integration.

### Upstream/API source links

- https://github.com/albertoirurueta/irurueta-android-gl-curl
- https://raw.githubusercontent.com/albertoirurueta/irurueta-android-gl-curl/main/lib/src/main/java/com/irurueta/android/gl/curl/CurlTextureView.kt
- https://raw.githubusercontent.com/albertoirurueta/irurueta-android-gl-curl/main/lib/src/main/java/com/irurueta/android/gl/curl/CurlPage.kt
- https://github.com/eschao/android-PageFlip
- https://github.com/oleksandrbalan/pagecurl
- https://github.com/Peyilo/libreadview
