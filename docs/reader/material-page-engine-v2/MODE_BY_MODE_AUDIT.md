# Veil Reader — Arena Navigation / Page-Turn Audit

Date: 2026-10-04  
Status: **SOURCE-HARDENED / NOT BUILD-GREEN / NOT DEVICE-GREEN**  
Branch: `reader/gpu-material-page-engine-v2`  
PR: `#373`

This audit is intentionally stricter than a visual review. It compares Veil mode-by-mode and code-path-by-code-path against mature curl/navigation references, then records only what the source actually proves.

The four EPUB interaction modes are treated as separate products:

| Mode | Gesture owner | Visual owner | Readium role |
| --- | --- | --- | --- |
| Paper | Veil Paper listener | GPU Material Page Engine v2 only | locator/navigation truth |
| Slide | Veil Slide listener | Slide overlay only | locator/navigation truth |
| Paged / None | Veil Static Paged listener | no page-motion renderer | unanimated page navigation |
| Scroll | Readium renderer | Readium renderer | scroll + locator truth |

PDF remains outside the EPUB Paper engine.

---

## Reference implementations studied

### harism/android-pagecurl — Apache-2.0

Relevant source concepts:
- direct pointer-driven curl position and direction;
- `curlLength = PI * radius`;
- binding constraints that prevent the sheet from geometrically tearing away from the book;
- explicit static-left, static-right and curling-page mesh roles;
- separate front/back textures;
- separate drop-shadow and self-shadow geometry;
- radius collapse near terminal travel.

Veil uses these as architectural/math references only; no source file is copied.

### xissburg/XBPageCurl — MIT

Relevant concepts:
- virtual-cylinder vertex deformation;
- cylinder position, direction and radius as compact physical state;
- vertex-shader normals;
- explicit front/back face behavior;
- release snapping.

### albertoirurueta/irurueta-android-gl-curl — Apache-2.0

Relevant concepts:
- modern Android lifecycle around the harism family;
- render-when-dirty;
- explicit surface recreation;
- `CurlGLSurfaceView` versus `CurlTextureView`;
- `TextureView` stays in the ordinary View hierarchy and supports normal composition/transparency.

Veil already depends on `com.irurueta:irurueta-android-glutils:1.1.11`.

### hexingbo/PageFlip — Apache-2.0

Relevant concepts:
- Android OpenGL ES 2.0 page-flip implementation;
- touch-to-origin geometry;
- radius derived from touch/origin distance and semi-perimeter ratio;
- separate fold-front, fold-back, base-shadow and edge-shadow roles;
- mesh quality expressed against page geometry rather than only device class;
- explicit page-origin/corner constraints.

### oleksandrbalan/pagecurl — Apache-2.0

Useful primarily as a Compose/state reference:
- explicit gesture/state separation;
- configurable interaction regions;
- stable page state;
- backside/shadow composition.

Its Canvas renderer is not Veil's quality ceiling and is not shipped as a v2 fallback.

### Nodlik/StPageFlip — MIT

Relevant concepts:
- active corner as first-class state;
- geometry constrained to page bounds/center;
- flipping page, bottom page and shadows as independent roles;
- hard/soft page behavior.

### Readium Kotlin Toolkit 3.4.0

Readium remains Veil's publication/navigation truth.

Critical integration rule: multiple `InputListener` implementations can be attached and **consumption order matters**. Veil therefore owns product-mode selection and Readium owns progression/current locator/navigation.

---

# 1. PAPER — GPU Material Page Engine v2

Core runtime:
- `PaperCurlInputListener.kt`
- `PaperCurlState.kt`
- `MaterialPageEngine.kt`
- `MaterialPagePhysics.kt`
- `MaterialPageProfiles.kt`
- `GpuPageCurlModel.kt`
- `GpuMaterialPageCurlView.kt`

## 1.1 One Paper renderer only

The v2 tree no longer ships the old visual implementations:

- deleted `PaperCurlDraw.kt`;
- deleted `PaperCurlGeometry.kt`;
- deleted `MaterialPageGeometry.kt`;
- removed legacy `PaperVisualEngine`;
- removed Canvas `MaterialPageOverlay`;
- removed `Matrix.setPolyToPoly` strip deformation;
- removed GPU-versus-Canvas A/B switch.

Rollback remains in Git history / PR #372. It is not hidden in the APK.

**Invariant:** an APK from this branch cannot silently show the old curl and be mistaken for GPU v2.

## 1.2 P0 — three GLSL defects capable of preventing all curl rendering

Arena found three independent source-level shader errors:

1. vertex shader declared `uSideSign` twice;
2. fragment shader used `uTexelSize` without declaring it;
3. fragment shader used `uSideSign` without declaring it.

Any one of these can cause shader compilation to fail. Together they are the strongest source-level explanation found so far for the device symptom “Paper selected, but no curl ever appears.”

All three are fixed.

A new `GpuPageShaderContractTest` now locks:
- no duplicate uniform declarations within a shader;
- no `uSomething` token used without declaration;
- vertex/fragment varyings match exactly;
- renderer-critical uniforms remain present.

Static source audit after the fixes:
- 24 shader uniform names;
- 24 Kotlin uniform-location fields;
- 24 `glGetUniformLocation` bindings;
- no missing/extra mapping.

This is **not** a substitute for an actual GLES compiler/device run.

## 1.3 Gesture ownership — P0 fixed

### Defect

Paper UI could be visibly selected while `PaperCurlInputListener` independently re-checked `navigator.overflow.value.scroll`.

The Readium flow can lag accepted preference state. That created a split-brain condition:
- UI = Paper;
- listener declines ownership;
- native/default Readium swipe continues;
- user sees slide-like movement instead of curl.

### Current rule

For EPUB, product-mode ownership comes from Veil's accepted/presented Reader appearance.

Readium provides:
- reading progression;
- current locator;
- actual navigation operations.

No Paper listener independently re-decides whether Paper is enabled from the asynchronous overflow state.

Debug Material review is enforced at the Reader contract boundary as `Paged + Paper`, so an older persisted Slide preference cannot bypass the GPU review path.

## 1.4 Release rollout after legacy removal — P0 fixed

Because GPU v2 is the only Paper visual runtime, a release build with GPU rollout disabled must not leave `PAPER` unowned.

Current behavior:
- Debug + Material review enabled -> Paper GPU v2;
- Paper preference + GPU rollout disabled -> static Paged / `NONE`;
- it does **not** degrade to native swipe masquerading as Slide.

## 1.5 GL composition — P0 fixed

Old v2 host:
- transparent `GLSurfaceView`;
- `setZOrderOnTop(true)`.

Problem:
- SurfaceView has separate composition semantics and can fight Compose chrome/dialog ordering.

Current host:
- `GLTextureView` from the already-installed irurueta GL-utils dependency;
- `isOpaque = false`;
- no on-top Surface hack;
- render-when-dirty;
- ordinary View hierarchy composition.

## 1.6 GPU failure is explicit

Renderer status is first-class:
- `READY`;
- `UNSUPPORTED`;
- `FAILED`;
- `REDUCED_MOTION`.

The persistent Debug HUD exposes status and begin-attempt count.

No legacy/Canvas renderer substitutes itself when GPU fails.

If the renderer is `FAILED` or `UNSUPPORTED`, later Paper turns remain semantic navigation rather than starting invisible preview transactions.

## 1.7 Source capture lifecycle — P0 fixed

### Defect found

An earlier “prewarm” optimization actually drew Readium content into a bitmap ahead of the gesture and allowed `begin()` to reuse it.

If the locator changed between prewarm and gesture, the GPU could curl stale page content.

### Current behavior

`prepareBuffer()` preallocates bitmap memory only.

`begin()` always performs a fresh source capture from the current Readium view.

This keeps the latency benefit of allocation warm-up without caching publication content.

### Remaining P1

Capture still uses synchronous `View.draw(Canvas(bitmap))`.

Real hardware must prove this is reliable for Readium's hardware-backed WebView. If device evidence shows blank/stale/incomplete captures, replace content capture with an asynchronous PixelCopy-style pipeline/cache rather than adding renderer hacks.

## 1.8 CPU/GPU bitmap race — P0 fixed

A single reusable mutable CPU bitmap can be overwritten for the next turn while the GL thread still uploads the previous frame.

Material v2 now uses front/back **ping-pong CPU buffers**:
- two source slots;
- two destination/back slots;
- alternating capture;
- no manual recycle while a GL frame may still hold the bitmap.

GPU texture objects remain persistent and use `texSubImage2D` when dimensions are unchanged.

## 1.9 Geometry

### Current strengths

- static triangle mesh;
- real vertex-shader virtual-cylinder deformation;
- normal generation from deformed surface;
- front/cylinder/post-half-turn regimes;
- cylinder X/Y, tilt and radius;
- side-aware mirroring;
- aspect correction;
- material-dependent stiffness/mass;
- terminal radius collapse;
- vertical grip;
- diagonal finger-vector steering;
- finite/NaN guards.

### PageFlip-derived improvement

Veil now tracks normalized **physical pointer travel**:
`hypot(inward, vertical) / pageWidth`.

The cylinder radius uses this travel signal:
- small early travel -> tighter curl;
- more physical travel -> radius opens;
- terminal progress -> radius tightens again.

This is closer to mature touch/origin cylinder models than radius-from-progress alone.

### P1 open — binding solution

Harism/PageFlip solve pointer-to-cylinder placement with stronger origin/binding geometry:
- touch-to-origin distance;
- arc length;
- fold intersections;
- page-bound constraints;
- explicit adjustment when the fold would leave legal geometry.

Veil still uses a simpler normalized cylinder model plus binding clamps.

Do not replace it blindly before hardware visibility is proven. Next geometry work should be a pure/testable binding-constraint model with invariants.

## 1.10 Projection — P1 open

XB uses a real MVP projection.

Veil computes 3D curl Z and normals, then uses a restrained clip-space transform rather than perspective projection.

This is deliberate for text legibility, but hardware review should determine whether subtle perspective foreshortening improves physicality without theatrical distortion.

## 1.11 Front/back semantics — P1 device check

Reference engines model current/bottom/curling pages explicitly.

Veil integrates with Readium differently:
- Readium preview destination is visible underneath;
- current/source page becomes front GPU texture;
- settled preview destination can become back GPU texture;
- fallback backside uses restrained source ink-through.

Must be checked on device for:
- forward;
- backward;
- LTR;
- RTL/Persian;
- repeated quick turns.

The risk is semantic orientation/duplication, not locator correctness.

## 1.12 Lighting/shadow

Current:
- deformed normals;
- diffuse;
- roughness-driven specular;
- grazing Fresnel;
- backside transmission;
- material edge tint/body;
- grain/fibre;
- self-occlusion;
- three mesh-projected cast-shadow layers.

Reference gap:
- harism/PageFlip build more explicit geometric base/edge/self-shadow structures.

Priority: P2 after visibility/capture/silhouette are device-green.

## 1.13 Reduced Motion

Reduced Motion:
- keeps semantic page navigation;
- bypasses geometric curl;
- does not activate an older renderer.

This is intentional.

---

# 2. SLIDE

Core:
- `SlideNavigationInputListener.kt`
- `SlidePageState.kt`

Contract:
- direct horizontal tracking;
- no bend;
- no page mass;
- no material shader;
- narrow transient edge shadow;
- short settle.

Paper physics must never leak into Slide.

## 2.1 Ownership split-brain — P0 fixed

Removed redundant `navigator.overflow.value.scroll` mode checks from the Slide listener.

ReaderScreen's accepted EPUB appearance is now the only product-mode owner.

## 2.2 Source prewarm stale-content bug — P0 fixed

Slide had the same stale prewarm pattern as Paper.

Current:
- `prepareBuffer()` allocates only;
- `begin()` always captures fresh current content.

## 2.3 Preview restoration race — P0 fixed

`forceCancelPendingTurn()` previously restored only when `previewNavigationSucceeded` had become observable.

Navigation could complete just before coroutine cancellation and before the flag assignment.

Current:
- exact `dragStartLocator` is restored whenever an uncommitted preview transaction has an origin locator.

## 2.4 Cancel-await race — P0 fixed

`cancelPendingTurnAndAwait()` now captures the existing completion job before cancellation/reset and joins the correct job.

## 2.5 Committed visual handoff — P0 fixed

Slide previously returned early once `turnCommitted == true`, leaving the visual completion coroutine alive through mode/lifecycle handoff.

Current:
- committed navigation is preserved;
- remaining Slide visual coroutine is cancelled/cleared;
- no locator rollback occurs.

## 2.6 Comparison with Android Pager — P2

Veil Slide uses a deliberately faster/direct policy.

Potential later improvement:
- targeted snap;
- velocity-aware decay;
- one-page maximum.

Do not mix this work into Paper stabilization.

---

# 3. PAGED / NONE

Core:
- `StaticPagedNavigationInputListener.kt`.

Contract:
- reserve deliberate horizontal drag;
- no visual page-motion engine;
- release commits one unanimated navigation;
- short drag does nothing.

Removed redundant Readium overflow mode gate.

Status: source architecture is intentionally simple and correct.

---

# 4. SCROLL

Scroll is Readium-owned.

Arena found one overlap:

### P0 fixed — directional key leakage

The directional fallback listener could still handle arrow/page keys in EPUB Scroll mode.

Current:
- EPUB Scroll disables Veil directional navigation;
- edge taps and keyboard navigation return to the renderer;
- Paper, Slide and Static Paged all resolve false.

Instrumentation contract now checks both tap and key ownership for Scroll.

Readium 3.4 remains responsible for the scrolling document and locator.

---

# 5. PDF

PDF stays outside EPUB Paper/Slide ownership.

Readium 3.4 PDFium supports scroll and horizontal paginated modes.

Veil must not reuse EPUB GPU Paper until a dedicated PDF capture/render contract exists.

Status: unchanged by this branch.

---

# 6. ONE-HOT INPUT OWNERSHIP

For EPUB navigation state, exactly one high-level owner is allowed:

- Paper -> Paper listener;
- Slide -> Slide listener;
- Paged -> Static Paged listener;
- Scroll -> Readium renderer.

`ReaderInputArbiterTest` already locks one-hot ownership across page-turn styles and Scroll.

No mode listener should independently reinterpret asynchronous Readium overflow state as product mode.

---

# 7. CURRENT PRIORITY

## P0 — fixed in source

- legacy Paper runtime in APK;
- Canvas Material hidden fallback;
- GPU/Canvas A/B ambiguity;
- GLSL duplicate `uSideSign`;
- GLSL missing fragment `uTexelSize`;
- GLSL missing fragment `uSideSign`;
- SurfaceView Z-order/composition ambiguity;
- Paper ownership split-brain;
- Slide ownership split-brain;
- Static Paged ownership split-brain;
- ReaderScreen duplicate EPUB overflow gates;
- Scroll directional key leakage;
- stale Paper prewarm content;
- stale Slide prewarm content;
- mutable bitmap/GL upload race;
- Paper exact restore races from earlier hardening;
- Slide exact restore race;
- Slide cancel-await race;
- Slide committed-visual handoff race;
- invisible/ambiguous GPU failure diagnostics.

## P1 — requires build/device evidence or isolated pure model work

- prove WebView `View.draw()` source/destination capture;
- stronger touch-origin/binding geometry;
- optional subtle perspective;
- validate destination/backside orientation forward/backward + RTL;
- inspect first-frame latency on 60/90/120Hz;
- validate TextureView lifecycle/context recreation on real hardware.

## P2 — only after Paper is hardware-green

- geometric self/base/edge shadow refinement;
- curvature-error-based adaptive mesh quality;
- Slide targeted snap/decay;
- hard-page density for covers/special surfaces.

---

# 8. REQUIRED NEXT EVIDENCE

Canonical CodeMagic workflow:
`1 - VEIL UI APK`

It runs:
1. policy/parser checks;
2. `:app:testDebugUnitTest`;
3. `:app:lintDebug`;
4. `:app:assembleDebug`.

Do not call the branch build-green until those complete successfully.

After APK install, HUD interpretation:

- `PAPER · GPU v2 · READY · A0` -> renderer initialized, no turn attempted;
- swipe -> attempt count must increase;
- `ACTIVE` -> Paper transaction + GPU visual state active;
- `GPU FAILED` -> renderer init/draw failure;
- `GPU UNSUPPORTED` -> GLES capability path unavailable;
- `CAPTURE FAILED` -> current-page snapshot failed;
- `REDUCED MOTION` -> curl intentionally bypassed.

Device matrix:
- 5 materials;
- LTR + RTL/Persian;
- forward/backward;
- top/middle/bottom grip;
- slow drag;
- fast flick;
- reverse-cancel;
- boundary;
- repeated rapid turns;
- mode handoff;
- rotation/resize;
- High Contrast;
- Reduced Motion;
- 60/90/120Hz where available.

Until this evidence exists, PR #373 remains Draft.
