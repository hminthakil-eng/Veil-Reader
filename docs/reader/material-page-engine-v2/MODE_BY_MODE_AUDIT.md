# Veil Reader Navigation / Page-Turn Audit

Status: **SOURCE AUDIT ACTIVE — BUILD + DEVICE EVIDENCE STILL REQUIRED**

Branch: `reader/gpu-material-page-engine-v2`

This audit compares Veil's runtime navigation modes code-by-code against current reference implementations and platform behavior. It intentionally separates:
- navigation ownership;
- gesture arbitration;
- visual renderer;
- release/commit policy;
- content/texture lifecycle;
- accessibility;
- RTL;
- lifecycle/restore correctness.

## External references

### harism/android-pagecurl — Apache-2.0
https://github.com/harism/android-pagecurl

Reference concepts used in this audit:
- `CurlView` directly owns the touch-driven curl state.
- `CurlMesh.curl(curlPos, curlDir, radius)` converts a page into flat/front, cylindrical, and flipped/back regions.
- front/back page textures are distinct.
- page-left/page-right/page-curl meshes are explicit roles.
- radius collapses as the pointer travels through terminal distance.
- `setCurlPos` constrains position/direction to keep the sheet attached to the book.
- drop shadow and self-shadow are separate geometric products.
- textures/meshes are prepared outside the high-frequency draw path.

### xissburg/XBPageCurl — MIT
https://github.com/xissburg/XBPageCurl

Reference concepts:
- virtual-cylinder deformation in a vertex shader.
- cylinder position + direction + radius are the compact physical state.
- deformed surface normals are produced by the vertex shader.
- distinct front/back rendering.
- touch selects a picking position and release snaps to explicit snapping points.

### albertoirurueta/irurueta-android-gl-curl — Apache-2.0
https://github.com/albertoirurueta/irurueta-android-gl-curl

Reference concepts:
- modern Android adaptation of the harism family.
- `CurlGLSurfaceView`: better raw surface performance but outside ordinary View composition.
- `CurlTextureView`: normal View hierarchy, transparency/composition support.
- render-when-dirty lifecycle.
- surface recreation resets GPU textures.
- touch/pointer drives curl continuously.

Veil already depends on:
`com.irurueta:irurueta-android-glutils:1.1.11`

### oleksandrbalan/pagecurl — Apache-2.0
https://github.com/oleksandrbalan/pagecurl

Reference concepts:
- Compose-first gesture/state separation.
- configurable drag/tap interaction regions.
- stable page keys/state.
- fixed four-point backside polygon to prevent 3/4-point topology artifacts.
- clipped front + mirrored/rotated back + cached shadow.

This is a useful Compose architecture reference but its renderer is a 2D illusion, not the quality ceiling for Veil Paper.

### eschao/android-PageFlip — Apache-2.0
https://github.com/eschao/android-PageFlip

Reference concepts:
- OpenGL ES 2.0 page-flip pipeline.
- touch-to-origin geometry with explicit fold points.
- configurable mesh pixels rather than a single fixed polygon budget.
- semi-cylinder perimeter ratio derived from touch distance.
- explicit fold-front, fold-back, edge-shadow and base-shadow buffers.
- constrained page-curl angle and configurable back mask.

Veil takeaway: the current static GPU grid is safe and inexpensive, but future quality scaling should be tied to screen geometry/curvature error rather than RAM tier alone.

### Nodlik/StPageFlip — MIT
https://github.com/Nodlik/StPageFlip

Reference concepts:
- active corner is a first-class state.
- geometry is constrained against center/bounds.
- flipping page, bottom page and shadows are separate roles.
- distinct soft/hard page density.
- separate inner/outer shadows.

### Android Compose Pager
https://developer.android.com/reference/kotlin/androidx/compose/foundation/pager/PagerDefaults

Reference concepts for Veil Slide:
- target-based snapping.
- positional threshold.
- velocity-aware decay/snap.
- default maximum one-page fling.

### Readium Kotlin Toolkit 3.4.0
https://github.com/readium/kotlin-toolkit
https://readium.org/kotlin-toolkit/3.4.0/

Veil currently uses Readium 3.4.0. Readium remains the location/navigation source of truth.

---

# 1. PAPER — GPU Material Page Engine v2

## Veil runtime

Core files:
- `PaperCurlInputListener.kt`
- `PaperCurlState.kt`
- `MaterialPageEngine.kt`
- `MaterialPagePhysics.kt`
- `MaterialPageProfiles.kt`
- `GpuPageCurlModel.kt`
- `GpuMaterialPageCurlView.kt`

## Old runtime removal

The v2 build no longer contains:
- `PaperCurlDraw.kt`
- `PaperCurlGeometry.kt`
- `MaterialPageGeometry.kt`
- legacy `PaperVisualEngine`
- Canvas `MaterialPageOverlay`
- `Matrix.setPolyToPoly` strip renderer
- GPU-vs-Canvas A/B switch

Rollback remains available through Git history / PR #372. It is not shipped as a hidden fallback in v2.

### Why this matters

Before removal, a real-device test could silently fall back to Canvas and make it impossible to know which renderer the user was actually seeing. The GPU-only build makes failures explicit.

## Gesture ownership

### Reference
Readium's injected gesture script calls the app's `InputListener`. Native/default drag continues unless the app returns true and causes `preventDefault()`.

### Veil defect found
Paper visible state used the accepted/presented Reader appearance, but `PaperCurlInputListener` also re-checked `navigator.overflow.value.scroll`.

That StateFlow can lag preference application. Result:
- UI says PAPER;
- Paper listener declines the drag;
- Readium default swipe continues;
- user sees a slide instead of curl.

### Fix
Paper ownership now follows the accepted Reader mode only.

Debug Material review is also enforced at the `ReaderScreen` contract boundary, not as a Settings-screen side effect.

## Release rollout safety

### Defect created by removing legacy
With no old Paper renderer, a disabled production GPU rollout could have left a persisted PAPER preference unowned, allowing native Readium swipe to appear.

### Fix
`applyMaterialPageRolloutToAppearance()` maps:
- Debug + GPU enabled -> PAPER
- PAPER + GPU disabled -> NONE/static PAGED
- never PAPER -> native slide fallback

Unit contracts lock both cases.

## GL composition

### Old Veil
`GLSurfaceView` + transparent Surface + `setZOrderOnTop(true)`.

### Reference
Irurueta explicitly distinguishes:
- GLSurfaceView: outside normal hierarchy; composition/transparency limitations.
- GLTextureView: normal hierarchy; conventional composition/transparency.

### Fix
Veil GPU curl now uses `GLTextureView` from the already-installed irurueta GL utils dependency.

This removes the separate-surface Z-order hack and keeps Paper in the normal Reader hierarchy.


GPU failure status is surfaced directly to the persistent debug HUD as `GPU FAILED` or `GPU UNSUPPORTED`; v2 never hides the failure by substituting an older renderer.

## Geometry

### Veil strengths
- real GPU triangle mesh;
- virtual-cylinder vertex deformation;
- explicit cylinder X/Y, tilt and material radius;
- front/curl/post-half-turn regimes;
- material-dependent radius;
- radius collapse near terminal travel;
- direct diagonal finger signal;
- aspect-correct page geometry;
- left/right mirroring without mirroring source text;
- finite-input guards.

### Gap vs harism/XB — P1 OPEN
Veil currently maps drag -> progress -> cylinder state with a material response function.

Harism solves pointer distance and curl arc length more directly:
- pointer distance;
- `curlLength = PI * radius`;
- translation when distance exceeds curl length;
- additional binding-line constraints so the page cannot geometrically rip away from the book.

XB also represents full cylinder direction directly.

Veil's current clamp/tilt/binding profile is stable, but the pointer-to-cylinder solution is simpler than these references.

**Action:** do not change blindly before the GPU is visible on hardware. Add a pure binding-constraint model + invariants, then tune against device footage.

## Projection — P1 OPEN

XB applies a real MVP matrix to deformed 3D vertices.

Veil currently uses an orthographic-style clip transform after computing 3D-like Z. Curvature and normals are real enough to produce rollover/light change, but perspective foreshortening is restrained.

**Action:** evaluate a subtle perspective projection after device verification; do not make reading text distort theatrically.

## Surface roles / page topology

Harism explicitly carries:
- static current/previous page;
- bottom/destination page;
- curling page.

StPageFlip similarly distinguishes flipping page and bottom page.

Veil uses:
- Readium destination rendered underneath after preview navigation;
- source page as GPU front texture;
- optional preview destination as GPU back texture.

This is efficient for integration but less explicit than a full three-page renderer.

**Risk:** backside/destination semantics must be checked on real forward/backward + RTL turns to ensure the destination is not visually duplicated or incorrectly oriented.

## Texture lifecycle

### Previous Veil defect
Only bitmap storage was preallocated. Actual `View.draw()` source capture happened in the first drag callback.

Readium's legacy EPUB gesture bridge reaches the app on the UI path, so synchronous capture at drag start can hitch the first visible curl frame.

### Fix
`prepareBuffer()` now pre-captures the settled Readium page off the gesture path.

`ReaderScreen` refreshes the warm source snapshot:
- after Paper mode settles;
- after appearance changes;
- whenever a Paper transaction returns to idle.

The active gesture consumes the prepared bitmap instead of redrawing the WebView when possible.

### Remaining snapshot risk — P1 OPEN
Veil still uses `View.draw(Canvas(bitmap))`.

Android hardware-backed content can have screenshot/snapshot compatibility limitations. If device evidence shows blank/stale source capture, move to an asynchronous PixelCopy-style capture/cache pipeline rather than adding renderer hacks.

## Front/back textures

Veil:
- persistent front/back GL texture objects;
- 1x1 valid initialization;
- `texSubImage2D` when dimensions are stable;
- destination back capture gated on memory class;
- no manual bitmap recycle while GL may still reference a frame.

Reference alignment:
- harism: separate front/back texture semantics.
- XB: separate front/back rendering.
- StPageFlip: separate flipping/bottom page roles.

Status: structurally strong; device orientation/backside-content test still required.

## Lighting and shadow

Veil:
- deformed normal;
- diffuse;
- roughness-dependent specular;
- grazing Fresnel;
- backside transmission;
- material edge tint;
- deterministic grain/fibre;
- mesh-projected layered cast shadow;
- self-occlusion term.

Reference gaps:
- harism builds distinct geometric drop-shadow and self-shadow vertex buffers.
- StPageFlip uses distinct inner/outer shadow models.

Veil's shadow is cheaper and material-aware, but not yet as geometrically explicit.

**Priority:** P2 after silhouette/ownership/capture are proven.

## Material identity

Veil is stronger than the references in this dimension:
- Glossy
- Matte Book
- Parchment
- Papyrus
- Manuscript

Profiles alter:
- apparent mass;
- bend stiffness;
- drag resistance;
- binding;
- roughness/specular;
- translucency;
- ink ghosting;
- grain/fibre;
- edge body;
- haptic/acoustic identity.

StPageFlip's hard/soft distinction is useful future inspiration for covers, but should not be mixed into ordinary EPUB text leaves yet.

## Reduced Motion

### Defect found after GPU-only migration
Canvas fallback was removed, but reduced-motion branches still performed short invisible alpha animations.

### Fix
Reduced Motion now:
- preserves semantic navigation;
- preserves sensory cue policy;
- performs no geometric curl;
- settles state immediately instead of spending 52–110ms on invisible animation.

---

# 2. SLIDE

Core files:
- `SlideNavigationInputListener.kt`
- `SlidePageState.kt`

## Product contract

Slide is intentionally not Paper:
- no bend;
- no page mass;
- no material deformation;
- direct horizontal tracking;
- short edge shadow only;
- fast settle.

This separation is correct and must remain strict.

## Ownership defect — FIXED

Slide duplicated the Paper split-brain pattern:
`isEnabled() && !navigator.overflow.value.scroll`.

The second gate could lag the accepted Reader appearance.

Fix:
`slideModeEnabled()` now trusts the accepted Reader-mode predicate supplied by ReaderScreen.

## Force-cancel race — FIXED

Previous:
`forceCancelPendingTurn()` restored only when `previewNavigationSucceeded == true`.

Race:
- navigation can complete;
- coroutine gets cancelled;
- flag assignment has not become observable;
- destination remains visible after cancellation.

Fix:
restore exact `dragStartLocator` whenever a preview transaction has a spec + origin locator.

## Await race — FIXED

Previous `cancelPendingTurnAndAwait()` could lose the completion-job reference when cancellation/reset changed it before join.

Fix:
capture existing completion job first, then join existing-or-new completion.

## Slide motion vs Android Pager — P2

Android Pager's canonical behavior is target-based:
- positional threshold;
- velocity-aware decay;
- snap animation;
- max page distance.

Veil keeps its intentionally faster direct Slide response, but the release-velocity path now uses the same high-refresh smoothing/reversal-trust strategy as hardened Paper instead of replacing history with one instantaneous sample.

The remaining gap is target animation: completion timing is still a compact Veil policy rather than Android Pager's decay + targeted-snap model.

**Action after Paper is stable:** consider a pure targeted-snap model while preserving one-page maximum and Readium locator ownership.

## Slide source texture — FIXED

Slide previously captured with `View.draw(Canvas)` on begin.

It now uses the same off-gesture warm-source strategy as Paper:
- capture after the accepted Slide mode/page has painted;
- reuse the prepared bitmap on drag start;
- refresh after each transaction returns idle.

This changes texture lifecycle only; Slide motion remains free of Paper material physics.

---

# 3. PAGED / NONE

Core:
- `StaticPagedNavigationInputListener.kt`

Contract:
- no curl;
- no slide;
- reserve horizontal drag so Readium's animated/native page motion cannot leak in;
- one unanimated page navigation on deliberate release;
- short drag does nothing.

## Ownership defect — FIXED

Removed redundant `navigator.overflow.value.scroll` mode gate.

ReaderScreen's accepted appearance is now the source of truth.

## Comparison

This is intentionally simpler than Android Pager. It is a semantic paged mode, not a motion mode.

Status: architecture correct.

---

# 4. SCROLL

Scroll remains Readium-owned.

Veil Paper/Slide/Static Paged predicates all resolve false in Scroll.

Readium 3.4.0 supports EPUB scroll/paginated preferences and keeps the navigator as the current-location source of truth.

Status:
- do not layer Paper or Slide logic on Scroll;
- do not reuse Paper resistance in Scroll;
- keep native document scrolling/selection ownership.

No code change required from this audit.

---

# 5. PDF

PDF remains separate from EPUB page-turn style.

Readium Kotlin Toolkit 3.4.0's PDFium adapter now explicitly supports:
- continuous scroll;
- horizontal paginated mode that snaps to page boundaries.

Veil must not apply the EPUB Paper GPU engine to PDF until a dedicated PDF snapshot/render contract exists.

Status: intentionally unchanged.

---

# 6. INPUT ARBITRATION

`ReaderInputArbiter` delegate order:

1. content target
2. tap matrix
3. Paper
4. Slide
5. Static Paged
6. directional
7. chrome / renderer fallback

For drag:
1. Paper
2. Slide
3. Static Paged
4. renderer fallback

Because mode predicates are now based on one accepted Reader appearance, only one navigation-mode delegate should own the gesture.

## Important invariant

No mode-specific listener should independently re-decide whether the Reader is scroll/paged from Readium's asynchronous overflow state.

Readium provides:
- current locator;
- progression;
- actual navigation.

Veil's accepted appearance provides:
- product-mode ownership.

---

# 7. PRIORITY LIST AFTER THIS AUDIT

## P0 — fixed in source
- old Legacy Paper runtime shipped beside GPU.
- Canvas Material v1 hidden fallback.
- GPU/Canvas A/B ambiguity.
- Paper ownership split-brain.
- persisted Slide bypassing Debug Paper review.
- production GPU gate potentially degrading to native Slide.
- Reader atmosphere drawn above Paper.
- GLSurfaceView composition/Z-order architecture.
- Slide ownership split-brain.
- Slide force-cancel locator restoration race.
- Slide cancel-await job-reference race.
- Static Paged duplicate overflow gate.
- stale unresolved `dropBackBufferIfCold()` compile symbol.
- invisible Reduced Motion settle latency.
- source Paper snapshot captured on first drag instead of prewarmed.
- Slide source snapshot captured on first drag instead of prewarmed.
- single-sample Slide release velocity instability at high refresh rates.
- GPU renderer failure not distinguishable from an idle/blank curl in the HUD.

## P1 — open until build/device evidence
- validate `View.draw()` WebView snapshot reliability; migrate to PixelCopy-style cache if needed.
- solve pointer-distance/curl-arc/binding geometry closer to harism without theatrical distortion.
- evaluate subtle perspective/MVP projection.
- validate destination/backside semantic mapping in forward/backward and RTL.

## P2 — after Paper hardware GREEN
- move Slide to targeted snap/decay model closer to Android Pager.
- decide whether explicit geometric self-shadow is worth GPU cost.
- consider hard-page density only for covers/special surfaces, not ordinary text pages.

---

# 8. BUILD / DEVICE MATRIX

The next APK should contain exactly one Paper visual engine: GPU v2.

Required checks:

### HUD ownership
- idle: `PAPER · GPU v2 · READY · A0`
- first deliberate horizontal drag: A increments.
- active gesture: `ACTIVE`
- if capture cannot start: `CAPTURE FAILED`
- Reduced Motion: `REDUCED MOTION` and no curl.

### Paper
- forward/backward;
- LTR/RTL/Persian;
- top/middle/bottom grip;
- slow drag;
- fast flick;
- reverse-cancel;
- boundary;
- repeated turns;
- theme changes;
- rotation/resize;
- five materials;
- 60/90/120Hz where available.

### Slide
- no bend/material lighting;
- direct finger tracking;
- forward/backward/RTL;
- cancel/commit;
- mode handoff while preview is active.

### Paged
- horizontal drag never shows animation;
- deliberate release changes exactly one page.

### Scroll
- vertical scrolling remains native and uninterrupted.

Until build + device evidence passes, PR #373 remains Draft.
