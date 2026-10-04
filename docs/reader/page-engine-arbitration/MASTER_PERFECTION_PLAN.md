# Veil Reader — Canonical Paper Engine Master Perfection Plan

Date: 2026-10-04
Branch: `integration/page-engine-canonical-v1`
Scope: EPUB Paper mode only. Slide, Paged, Scroll and PDF remain separate owners.
Status: RESEARCH-CONVERGED / EXECUTION PLAN / DEVICE EVIDENCE REQUIRED

## 0. North-star contract

Veil Paper must feel like a physical sheet without becoming a physics demo.

The target is:
- immediate response to the finger;
- a page edge that visually follows the user's intent;
- developable-looking sheet deformation with no obvious rubber/stretching;
- convincing front/back ownership;
- readable text throughout the gesture;
- material identity (Glossy, Matte Book, Parchment, Papyrus, Manuscript);
- stable LTR/RTL/Persian behavior;
- no hidden Slide/Canvas fallback;
- graceful GPU/WebView lifecycle recovery;
- deterministic commit/cancel semantics;
- measured 60/90/120 Hz quality, not assumed quality.

Readium remains publication/navigation truth. The GPU renderer is a transient visual layer only.

## 1. Research basis

### Official / platform
- Android hardware-accelerated Views:
  https://developer.android.com/topic/performance/views/hardware-accel-views
- PixelCopy:
  https://developer.android.com/reference/android/view/PixelCopy
- TextureView:
  https://developer.android.com/reference/android/view/TextureView
- Choreographer:
  https://developer.android.com/reference/android/view/Choreographer
- JankStats:
  https://developer.android.com/topic/performance/jankstats
- Macrobenchmark / FrameTimingMetric:
  https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics
- Android GPU Inspector:
  https://developer.android.com/agi
- Android Performance Analyzer:
  https://developer.android.com/android-performance-analyzer/release-notes
- Adaptive refresh rate:
  https://developer.android.com/develop/ui/views/animations/adaptive-refresh-rate
- OpenGL ES 2.0 specification:
  https://registry.khronos.org/OpenGL/specs/es/2.0/
- Readium Kotlin 3.4.0:
  https://readium.org/kotlin-toolkit/3.4.0/
- Readium migration guide:
  https://readium.org/kotlin-toolkit/3.4.0/migration-guide/

### Geometry / research
- Hong, Card, Chen — Turning Pages of 3D Electronic Books:
  DOI 10.1109/VR.2006.135
- Liesaputra & Witten — Computer graphics techniques for modeling page turning:
  DOI 10.1007/s00799-009-0055-3
- Hwang & Yoon — Constructing developable surfaces by wrapping cones and cylinders:
  DOI 10.1016/j.cad.2014.08.025
- Bo & Wang — Geodesic-Controlled Developable Surfaces for Modeling Paper Bending:
  DOI 10.1111/j.1467-8659.2007.01059.x
- Wolf et al. — Physically-based Book Simulation with Freeform Developable Surfaces (Eurographics 2021).

### Reference implementations
- harism/android-pagecurl — dynamic curl mesh, pointer-following edge, two-sided page:
  https://github.com/harism/android-pagecurl
- eschao/android-PageFlip — OpenGL ES 2.0 page flip, semi-perimeter control, separate fold/base shadows:
  https://github.com/eschao/android-PageFlip
- xissburg/XBPageCurl — static grid + vertex-shader virtual cylinder:
  https://github.com/xissburg/XBPageCurl
- albertoirurueta/irurueta-android-gl-curl — modern Android GLTextureView adaptation:
  https://github.com/albertoirurueta/irurueta-android-gl-curl
- oleksandrbalan/pagecurl — Compose-native interaction/configuration reference:
  https://github.com/oleksandrbalan/pagecurl

References are donors for principles and tests, not permission to create multiple runtime engines.

## 2. Architecture ruling

Keep:
- one GPU Paper renderer;
- static indexed mesh;
- vertex-shader deformation;
- source-derived reverse face until a true opposite-leaf provider exists;
- Readium locator/navigation ownership;
- TextureView composition;
- one-hot Paper / Slide / Paged / Scroll ownership;
- five canonical material profiles.

Do not add:
- a second Canvas Paper renderer;
- silent native Slide fallback;
- full FEM/cloth simulation in the production reader;
- physics that can outrun or detach from the finger;
- a second navigation truth;
- per-frame bitmap allocation;
- unbounded renderer recovery loops.

## 3. Workstream A — Snapshot acquisition: P0

Current risk:
`View.draw(Canvas(bitmap))` is synchronous and can be the first-turn latency spike. A software bitmap Canvas can also differ from the hardware-composited WebView result.

### A1. Introduce a capture abstraction
Create:
`PaperSnapshotProvider`
with explicit result states:
- Ready(bitmap, sourceRevision, width, height)
- NotReady
- Invalidated
- Failed(reason)

The renderer must never know whether capture came from View.draw or PixelCopy.

### A2. Two capture paths, benchmarked not guessed
Path S — synchronous View capture:
- retain as compatibility baseline;
- trace the full call;
- require exact viewport and revision match.

Path H — hardware-composited window crop:
- API 26+: evaluate PixelCopy from the hosting Window cropped to `publicationView`;
- API 34+: prefer Request.Builder.ofWindow(view) + source rect;
- only capture after at least one committed draw;
- reject stale rect/window/session;
- never include Paper overlay/chrome in the source crop.

Do not switch the default until hardware/device A/B proves:
- correct WebView text/images;
- no overlay contamination;
- lower or equal gesture-to-first-curl latency;
- reliable rotation/resize behavior.

### A3. Warm snapshot strategy
Preallocate memory but do not cache stale publication pixels indefinitely.
Research/test two modes:
1. memory-only prewarm (current safe baseline);
2. revision-bound fresh snapshot prepared after Readium settles, invalidated on:
   - locator change;
   - reflow;
   - font/theme/margin change;
   - viewport resize;
   - image/resource late paint;
   - lifecycle transition.

Goal: first drag should normally need no bitmap allocation and, if safe, no full synchronous capture.

### A4. Capture instrumentation
Trace:
- paper.capture.wait_for_draw
- paper.capture.view_draw
- paper.capture.pixel_copy
- paper.capture.total
- paper.capture.age_ms
- paper.capture.invalidated

Device acceptance:
- P95 capture <= 4 ms preferred;
- P99 <= 8 ms on target high-end hardware;
- no blank/black/stale captures across the publication fixture set.

## 4. Workstream B — Finger-to-sheet geometry: P0/P1

Current GPU cylinder is a good base but still driven primarily by normalized progress.

### B1. Touch-space solver
Promote the gesture input from:
`progress + verticalBias + diagonalPull`
to a physically meaningful frame:
- touch origin;
- current pointer;
- constrained free-edge target;
- bound-edge/spine invariant;
- cylinder axis;
- radius;
- signed arc travel.

Use the user's actual pointer as the primary constraint.

### B2. Edge-follow invariant
Port the strongest principle from harism:
the free paper edge should follow the pointer continuously instead of merely correlating with it.

Add a pure solver test:
`projectedFreeEdge(pointer) <= 1.5 dp error`
through representative drag space, except at deliberate binding constraints.

### B3. Cone/cylinder blend
Do not replace the cylinder globally.

Prototype:
- center grip -> cylinder dominant;
- top/bottom corner -> restrained cone contribution;
- intermediate grip -> smooth cone/cylinder interpolation.

Adopt only if it materially improves corner turns without:
- text stretch;
- unstable normals;
- extra visible latency;
- difficult RTL symmetry.

### B4. Developability checks
Automated geometry contracts:
- finite outputs;
- no triangle inversion outside intended backside;
- no bound-edge detachment;
- monotonic arc length;
- no terminal page trapping;
- left/right mirror symmetry;
- aspect ratios 0.5–4.0;
- foldable/tablet portrait/landscape.

## 5. Workstream C — Release physics: P1

Replace heuristic end-settle where necessary with a deterministic analytic/critically-damped state model.

Inputs:
- current physical progress;
- signed release velocity;
- material apparent mass;
- stiffness;
- damping;
- completion/cancel basin.

Rules:
- reverse velocity can cancel;
- tiny high-speed twitch cannot commit a whole page;
- heavy Manuscript settles slower than Glossy;
- no rubber bounce;
- completion/cancel remains monotonic;
- duration is bounded.

Use Android/Compose VelocityTracker semantics as a reference for robust sampling rather than relying only on the most recent delta.

## 6. Workstream D — Material renderer v3: P1/P2

### D1. Lighting
Move from one generic light response to a restrained book-light rig:
- broad key light;
- low-energy ambient;
- fold-contact darkening;
- backside transmission;
- edge/body response;
- material-specific roughness/specular.

No cinematic moving spotlight.

### D2. Shadow model
Split shadows:
1. contact/base shadow near the fold;
2. soft cast shadow on the revealed page;
3. self-occlusion from page depth.

Use eschao's separate base/edge shadow idea as a structural reference.
Shadow width must derive from lift/radius/material, not fixed offsets alone.

### D3. Material microstructure
Keep procedural detail anchored in page UV space.
Add:
- low-frequency paper body variation;
- high-frequency fibre;
- subtle edge darkening/whitening;
- thickness cue;
- patina masks.

Avoid visible repeating sine patterns. Use deterministic hash/noise or a tiny tileable material texture only if profiling says it is cheaper/cleaner.

### D4. Front/back correctness
Maintain the canonical rule:
destination page is underneath, never used as the reverse side of the lifted source leaf.

Backside content:
- mirrored source;
- attenuated ink-through;
- material tint/transmission;
- orientation tests for LTR and RTL.

## 7. Workstream E — GPU lifecycle and synchronization: P0

### E1. Context lifecycle state machine
Explicit states:
UNMOUNTED -> INITIALIZING -> READY -> ACTIVE -> PAUSED -> RECREATING -> READY
and terminal UNSUPPORTED / FAILED_AFTER_RETRIES.

Test:
- background/foreground;
- screen off/on;
- rotation;
- multi-window;
- fold posture change;
- SurfaceTexture recreation;
- process recreation.

### E2. Resource generation IDs
Every GL resource generation gets an ID.
Submitted frames must carry the generation they target.
A stale frame from an old context is ignored.

### E3. CPU/GPU ownership
Never recycle/overwrite a bitmap until no GL upload can reference it.
Current ping-pong remains; add explicit upload generation/fence accounting if device traces show races.

### E4. Resize transaction
A viewport change during an active turn:
- cancels/restores uncommitted preview;
- clears old texture state;
- waits for one stable layout/draw;
- recaptures;
- returns READY.
Never leave an invisible ACTIVE transaction.

## 8. Workstream F — Frame pacing and 120 Hz: P0 evidence

### F1. Instrumentation
Add:
- JankStats state tags:
  PAPER_IDLE / CAPTURE / DRAG / RELEASE / CANCEL / GL_RECREATE
- custom Trace sections around:
  capture, texture upload, frame submit, navigation preview, restore, release.

### F2. Benchmarks
Macrobenchmark journeys:
- first Paper turn after Reader open;
- warm repeated 20 turns;
- slow drag;
- fast flick;
- reverse cancel;
- corner turn;
- LTR/RTL;
- rotation then first turn.

Collect:
- FrameTimingMetric;
- custom TraceSectionMetric;
- memory.

### F3. GPU tools
Use Android GPU Inspector / Android Performance Analyzer on physical hardware.

Inspect:
- texture upload cost;
- draw-call count;
- fragment overdraw;
- shader cost;
- GPU memory;
- stalls;
- frame timeline.

### F4. Acceptance budgets
At 60 Hz:
- P95 frame under 16.67 ms;
- P99 preferred under 20 ms;
- no repeatable visible hitch at lift-off.

At 90 Hz:
- P95 under 11.11 ms target.

At 120 Hz:
- P95 under 8.33 ms target on capable flagship hardware;
- if capture/navigation makes the first frame impossible, the curl must visually start from already-prepared state rather than claim 120 Hz falsely.

## 9. Workstream G — Memory: P0/P1

Budget all storage, not Java bitmap bytes only:
- 2 CPU source buffers;
- active GPU texture;
- temporary PixelCopy destination if used;
- GL mesh/buffers;
- Readium/WebView/compositor coexistence.

Policies:
- query GL_MAX_TEXTURE_SIZE;
- scale capture only if pixel-accuracy tests still pass;
- low-RAM tier reduces shadow/mesh before reducing navigation correctness;
- release idle resources after bounded inactivity;
- no per-frame allocation;
- stress under repeated turns and rotation.

## 10. Workstream H — Readium integration: P0

Current Readium Kotlin stable line is 3.4.0.

Keep:
- Readium currentLocator;
- goForward/goBackward/go(locator);
- publication settings;
- selection/TTS/notes ownership.

Paper preview contract:
1. capture exact start locator;
2. capture source visual;
3. visually lift;
4. preview destination underneath;
5. commit once OR restore exact locator;
6. persist only committed truth.

Never derive product-mode ownership from a lagging secondary Readium state after Veil has accepted a mode.

PDF is explicitly outside this engine.

## 11. Workstream I — RTL/Persian: P0

Matrix:
- English LTR / Persian RTL;
- forward/backward;
- left-edge/right-edge start;
- body swipe;
- tap turn;
- hardware key;
- top/center/bottom grip;
- first/last page boundary.

Assertions:
- logical direction correct;
- physical side correct;
- source ink orientation correct;
- backside mirror correct;
- destination not duplicated on backside;
- no flash of native Slide;
- locator persists once.

## 12. Workstream J — Accessibility: P0

Reduced Motion:
- never requires GPU curl;
- preserves functional page navigation;
- no forced animation.

TalkBack / touch exploration:
- Paper gesture must not steal accessibility exploration;
- semantic page actions remain available.

High Contrast:
- retain legibility;
- reduce decorative grain/specular;
- never reduce text contrast.

Large text / reflow:
- invalidates snapshot revision;
- no stale curl after typography changes.

## 13. Workstream K — WebView failure handling: P1

The WebView renderer can be killed/crash independently.

Audit Readium/WebView ownership for:
- render-process-gone recovery;
- abandoning invalid WebView instances;
- recreating publication view safely;
- invalidating Paper snapshots and GL frames after renderer recreation.

Paper must fail closed, never commit navigation from a visually invalid source.

## 14. Workstream L — Startup/Baseline profiles: after runtime correctness

Add the Paper journey to the app Baseline Profile only after the engine is stable:
- open EPUB;
- enter Paper;
- first turn;
- second turn;
- close Reader.

Keep Startup Profile focused on actual startup; do not bloat it with all Reader material classes unless measurements justify it.

## 15. Verification pyramid

### Pure unit
Geometry, physics, symmetry, thresholds, material profiles, state machine.

### Shader contract
Compile/link on real GLES, uniform/varying mapping, finite colors/normals.

### Pixel/render fixtures
High-contrast synthetic pages to expose:
- wrong-face ink;
- mirrored orientation;
- edge leaks;
- shadow discontinuity.

### Instrumentation
Real Readium publication, lifecycle, rotation, RTL, accessibility.

### Macrobenchmark
Frame timing and trace budgets.

### Physical-device review
At least:
- modern Adreno flagship high-refresh;
- Samsung/Exynos or recent Galaxy;
- Mali device;
- one constrained/low-RAM device;
- tablet/foldable if available.

## 16. Anti-regression gates

A Paper change may not merge if any of these regress:
- Paper/Slide one-hot ownership;
- exact cancel restore;
- first-page-turn accounting;
- durable close;
- RTL direction;
- selection/TTS/Notes;
- Reduced Motion;
- no Canvas runtime fallback;
- snapshot freshness;
- memory ceiling;
- frame timing budget.

## 17. Execution order

P0.1 Snapshot provider + trace instrumentation.
P0.2 Hardware PixelCopy A/B prototype, not default.
P0.3 Gesture-space geometry / pointer-edge invariant.
P0.4 GPU lifecycle generation state machine.
P0.5 RTL + pixel fixture suite.
P0.6 Macrobenchmark + JankStats + trace labels.
P1.1 Analytic release solver.
P1.2 Base/cast/self-shadow split.
P1.3 Material microstructure v3.
P1.4 WebView renderer recovery audit.
P1.5 Memory-pressure and texture-resolution policy.
P2.1 Cone/cylinder hybrid prototype.
P2.2 Perspective/MVP A/B only if readability wins.
P2.3 Baseline Profile journey.
P2.4 Sensory tuning after visual/physical behavior is locked.

## 18. Stop conditions

Do not call Paper production-ready until:
- build gates are green;
- real-device Paper is visibly distinct from Slide;
- no first-turn blank/slide fallback;
- forward/back + RTL pass;
- lifecycle/context recreation pass;
- capture pipeline has evidence;
- 60/90/120 Hz measurements exist where hardware supports them;
- P95/P99 budgets are recorded;
- memory peak is recorded;
- accessibility matrix passes.

The final engine should be judged by evidence and reader feel, not code sophistication.
