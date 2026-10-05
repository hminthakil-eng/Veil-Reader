# Veil Reader — Page Engine Architecture Arbitration

Date: 2026-10-04
Role: Independent principal engineer / architecture review board
Repository: `hminthakil-eng/Veil-Reader`

## Decision

**WINNER: GPU Material Page Engine v2 architecture — as a single GPU Paper renderer, rebuilt/converged on the current Grayfog product baseline.**

This is an architectural win for the v2 rendering model, **not approval of PR #373 as-is**.

PR #373 at current head `91b1f479824a7701ab4e27cf17a059263b3002e3` contains a source-proven P0 initialization deadlock in the GPU overlay: the composable returns while renderer status is `INITIALIZING`, before the `AndroidView` that could create the GL context and transition the renderer to `READY` is ever mounted. That implementation cannot be promoted in its current form.

The canonical convergence branch is:

`integration/page-engine-canonical-v1`

and it is intentionally based on the current Grayfog product/UI head:

`438a967a074484aec373a8edc15eeca467ffaa13`

The convergence must port the GPU architecture selectively. It must **not** merge PR #372 wholesale into PR #373, and must **not** ship more than one Paper runtime renderer.

v1 remains the strongest correctness/evidence donor. Its proven face-ownership fix, binding invariants, release-model invariants, resize guards, tests and verification methodology should be ported where applicable. Its Canvas/legacy runtime renderers should not be ported.

---

## Evidence policy

Evidence is weighted in this order:

1. real source behavior;
2. reproducible tests;
3. successful build evidence;
4. static render evidence;
5. reference comparisons;
6. architectural reasoning;
7. PR descriptions.

Labels in this document:

- **FACT** — directly established by source, repository history, checked-in reproducible evidence, or GitHub metadata.
- **INFERENCE** — engineering conclusion from the established source behavior.
- **DEVICE-REQUIRED** — cannot be honestly closed without Android hardware/runtime evidence.

No PR prose is treated as proof by itself.

---

## Repository truth at arbitration time

| Line | Current head verified | Branch relation / evidence |
| --- | --- | --- |
| Grayfog product/UI | `438a967a074484aec373a8edc15eeca467ffaa13` | current product baseline; PR #371 |
| Material Page Engine v1 | `e11365424ba54ee05fe92394118aae337da12b08` | diverged from current UI and v2; PR #372 |
| GPU Material Page Engine v2 | `91b1f479824a7701ab4e27cf17a059263b3002e3` | diverged from current UI and v1; PR #373 |

**FACT:** the three lines are not cleanly linear. A wholesale merge would combine product and renderer histories that evolved independently.

**FACT:** GitHub Actions at current UI and v2 heads are failing before execution; inspected jobs have no executed steps. Those failures are infrastructure/allocation evidence, not source pass/fail evidence.

**FACT:** v1 has checked-in successful verification evidence at its current head:
- 774 JVM tests;
- 134 suites;
- 0 failures;
- 0 errors;
- 0 skips;
- 385 Reader-related tests in 56 suites;
- 39 new-mesh tests in 6 suites;
- lint: 0 errors, 181 warnings;
- `:app:assembleDebug` passed;
- `:app:assembleDebugAndroidTest` passed;
- `git diff --check` passed;
- no instrumentation/device execution.

This evidence is materially stronger than v2's present verification evidence.

---

## Scoring rubric

Scores describe the **current candidate source**, not marketing intent.

- **5** — strong architecture plus source/test evidence; no material blocker known.
- **4** — strong and suitable, with bounded gaps.
- **3** — viable but important gaps/risk remain.
- **2** — serious blocker/risk prevents canonicalization as-is.
- **1** — structurally mismatched with the product target.

The score is accompanied by evidence so the number is not treated as an arbitrary weighted vote.

## Candidate comparison

| Criterion | v1 — Canvas cylindrical material path | GPU v2 — current implementation | Arbitration |
| --- | ---: | ---: | --- |
| Geometry | **4.4/5** | **3.9/5** | v1 currently has the stronger proven binding constraint and source-face partition. v2 has the better long-term 3D topology but its binding model is still simplified. |
| Physical realism | **3.9/5** | **4.2/5** | Both are controlled physical approximations, not sheet FEM. v1 has a strong analytic critically damped release. v2 has a better deformable 3D surface/normal/depth model and more room for material-specific behavior. |
| GPU/CPU scalability | **2.9/5** | **4.6/5 potential; 2.8 current operational confidence** | v1 performs CPU mesh/face work and depends on Canvas `drawVertices`. v2 keeps a static VBO/IBO and moves deformation/shading to shaders, but synchronous capture/upload is still a risk. |
| Material expressiveness | **3.4/5** | **4.8/5** | Programmable per-fragment roughness/specular/Fresnel/transmission/fibre/edge policies give GPU v2 the correct ceiling. v1 can approximate these but not with the same fidelity or extensibility. |
| RTL/LTR correctness | **4.6/5** | **3.3/5** | v1 reproduced the old RTL front-ink leak and fixed it with explicit source-space face partition. v2 has side-aware geometry, but rendered back-face texture orientation is not yet equivalently proven. |
| Integration risk | **2.3/5** | **2.2/5 as-is** | v1 ships multiple Paper renderers/profile stacks and still has a stale overflow gate. v2 is cleaner in ownership but currently has a P0 initialization deadlock and diverged integration history. |
| Lifecycle safety | **4.0/5** | **2.7/5** | v1 has more executed regression/build evidence. v2 context recreation, TextureView lifecycle, capture/upload timing and memory pressure still require runtime evidence. |
| Accessibility | **4.2/5** | **4.4/5** | Both preserve renderer semantics and Reduced Motion policy. v2 overlay is explicitly non-focusable and input ownership is cleaner. Device TalkBack/high-contrast proof is still required. |
| Performance potential | **3.1/5** | **4.8/5** | v2 is architecturally better for 90/120 Hz and richer optics. This is potential, not a measured frame-time result. |
| Device risk | **3.3/5** | **2.4/5 current** | Neither is device-green. v2 has additional GL driver/context/capture/upload risk, so its current device confidence is lower even though its ceiling is higher. |
| Code complexity | **2.0/5** | **3.7/5** | v1's multiple live Paper engines and duplicated material models are harder to reason about. v2 has lower product-level duplication despite normal GL complexity. |
| Long-term extensibility | **3.1/5** | **4.9/5** | shader-driven normals, true front/back surface handling, depth and material pipelines are the better multi-year substrate. |
| Testing maturity | **5.0/5** | **2.9/5** | v1 has executed full Gradle/build evidence and rendered regression work. v2 has useful contract tests but they do not catch the current initialization deadlock and have not been run through an equivalent current-head build gate. |

### Why v2 wins despite weaker current evidence

The decision is based on the long-term product target, not which branch is currently greener.

A premium material engine that must differentiate glossy paper, matte stock, parchment, papyrus and manuscript through geometry, light, surface response, transmission, shadow and future physically richer behavior needs a programmable GPU surface. v1's latest cylindrical Canvas work is technically respectable and has superior proof today, but its rendering API is the limiting layer.

v2 wins because its **rendering architecture** can support the target without needing another renderer replacement later.

v1 does **not** win merely because its test evidence is better.

v2 does **not** win merely because it uses OpenGL.

It wins because the static GPU mesh + shader material pipeline is the better fit for the required product envelope after the current defects are repaired.

---

# Detailed technical arbitration

## 1. Geometry quality

### v1

**FACT:** the newer v1 cylindrical path uses an inextensible cylindrical strip with source coordinate treated as arc length, cylindrical `sin/cos` projection, and a planar continuation after the bend.

**FACT:** its fold basis rotates for diagonal/corner pulls.

**FACT:** its binding constraint explicitly bounds fold slope from remaining binding reach, including tall/narrow pages, so an extreme diagonal pull cannot detach the opposite binding endpoint.

**FACT:** the renderer builds explicit front/back source-space faces and clips triangles at the normal horizon before drawing. This is stronger than clipping two overlapping whole meshes in projected space.

**FACT:** the branch reproduced a real historical RTL front-ink leak and records the failing pixel fixture before the final face-partition fix.

**Judgment:** v1 currently has the most convincing *proven* bounded-sheet geometry in Veil.

### v2

**FACT:** v2 uses a static triangle grid and performs virtual-cylinder deformation in the vertex shader.

**FACT:** source arc distance maps through the cylinder and then into a post-half-turn planar region. It therefore has a materially better basis than a flat 2D transform.

**FACT:** top/middle/bottom grip and diagonal pull influence cylinder position/tilt.

**FACT:** pointer travel affects radius choreography rather than using progress alone.

**P1 gap:** binding protection is still a normalized/clamped model, not the stronger touch-origin/fold-intersection solution used by mature engines such as android-pagecurl/PageFlip and not the explicit endpoint invariant already present in v1's newer mesh.

**Verdict:** v2 has the better geometric *ceiling*, but v1 currently has the better binding proof. The canonical GPU line should port the binding invariants/model, not the Canvas renderer.

---

## 2. Physical plausibility

Neither candidate is a true continuum sheet solver.

### What is real/useful

Both model meaningful substrate parameters such as mass/resistance/stiffness/settling thresholds.

v1's newer release model is especially strong:
- critically damped analytic release;
- mass/stiffness-derived angular frequency;
- bounded launch velocity;
- monotonic endpoint behavior;
- reverse-cancel policy.

v2 has:
- material-dependent drag resistance;
- material-dependent release thresholds;
- spring stiffness/damping;
- radius choreography;
- pointer-travel signal;
- anti-jelly policy.

### What is not yet physically real

**FACT/INFERENCE:** manipulation is still kinematic/parameterized. Apparent mass and friction do not emerge from a time-integrated physical system during the drag; they shape progress/radius/release policies.

That is acceptable for a reader if the perceptual result is believable, deterministic and stable. A full cloth/FEM solver is not required and could hurt predictability.

**Recommendation:** retain deterministic product physics, but port v1's analytic release invariants into the GPU canonical line before tuning more shader decoration.

---

## 3. Front/back face correctness

### v1

**Strong evidence.**

The old RTL leak was reproduced with deliberately high-contrast source data, then removed by source-space face partition. The final renderer separates front and back triangle ownership.

### v2

**FACT:** front/back selection is based on actual triangle winding/`gl_FrontFacing`, with culling disabled.

**FACT:** v2 has a source front texture and an optional separately captured back texture.

**P1 face-ownership gap:** v2 currently chooses front versus back with `gl_FrontFacing`. OpenGL defines that value per rasterized primitive, not per physical sub-region of a triangle. A triangle that straddles the cylindrical normal horizon therefore cannot be split into front/back ownership the way v1's proven `MaterialFaceMesh` does. Dense tessellation can make the error narrow, but it does not make the topology exact. This is the GPU analogue of the class of defect that produced the earlier v1 RTL ink leak and requires a rendered regression, not only a lexical shader-contract test.

**P1 semantic defect/risk:** the optional back texture is captured *after Readium preview navigation*. That means it represents the destination page currently visible underneath the lifted source page. Rendering the same destination capture on the lifted backside can show the destination simultaneously on the back of the turning leaf and underneath it.

This is not a locator bug; it is a physical page-semantic bug.

For a real codex model, these are conceptually distinct surfaces:
1. source/front of current leaf;
2. reverse side of that same leaf;
3. page revealed underneath.

The current Readium snapshot strategy only naturally supplies (1) and (3).

**Canonical rule:** until a true opposite-leaf surface can be supplied, the GPU back face should use the source-derived mirrored/attenuated ink-through material model, not duplicate the destination page as if it were the same leaf.

**DEVICE-REQUIRED:** verify backward/forward, left/right and Persian RTL texture orientation. Existing v2 shader-contract tests do not prove pixels.

---

## 4. GPU architecture

### Strong points

- GLES 2.0-compatible shader architecture;
- static VBO/IBO;
- no per-frame mesh allocation;
- render-when-dirty;
- persistent texture objects;
- `texSubImage2D` when dimensions are stable;
- explicit failure state;
- `GLTextureView` instead of an on-top SurfaceView;
- depth-based self-occlusion;
- programmable material response;
- low-RAM mesh tier;
- CPU bitmap ping-pong to reduce overwrite/upload races.

### P0 initialization deadlock

Current source in `GpuMaterialPageOverlay` does this:

1. `rendererReady` starts false;
2. status becomes `INITIALIZING`;
3. the composable returns for every status other than `READY`;
4. the `AndroidView` that creates `GpuMaterialPageCurlView` is below that return;
5. `onSurfaceCreated()`, which is the only path that can call `onRendererReady(true)`, can never run.

**Classification: P0 blocker.**

The host must remain mounted while status is `INITIALIZING`. Status may control visibility/diagnostics and whether Paper begins, but cannot prevent creation of the renderer that produces the status.

### Additional GPU risks

**P1:** `View.draw(Canvas(bitmap))` source capture is synchronous and on the interaction path.

**P1:** first/changed-size texture upload is a full bitmap upload.

**P1:** the CPU memory gate budgets four CPU bitmaps but does not include GPU texture memory. On high-resolution displays, total engine memory can be much larger than the Java-side budget implies.

Illustrative—not measured—example for 1440×3200 ARGB:
- one bitmap ≈ 17.6 MiB;
- four CPU page buffers ≈ 70.3 MiB;
- two GPU RGBA textures ≈ 35.2 MiB;
- total page-engine storage can therefore exceed ≈105 MiB before WebView/compositor costs.

**P1 / DEVICE-REQUIRED:** TextureView context loss/recreation, pause/resume, rotation and fold resize must be proven on real hardware.

**P1:** resize mismatch currently suppresses drawing; the Reader transaction must also guarantee clean cancellation/recapture so this cannot become an invisible active Paper transaction.

**P2:** current shadow is three offset renders of the page mesh, not a fully geometric base/edge/self-shadow solution.

**P2:** projection is intentionally near-orthographic rather than a full MVP perspective model. This protects text legibility but may leave some physical depth on the table.

---

## 5. Canvas / CPU architecture

### What v1 does well

- reusable arrays and paths;
- no per-frame bitmap allocation;
- explicit face partition;
- bounded snapshot scale;
- stable source-space UVs;
- strong static/native render evidence.

### Ceiling

**FACT:** the new v1 face renderer depends on `Canvas.drawVertices`.

The repository's platform review correctly notes that Android hardware acceleration for this operation is not available on all Veil-supported API levels; v1 therefore routes older APIs to a reduced live-edge path instead of full deformation.

**INFERENCE:** even when accelerated, Canvas gives substantially less control than a dedicated GPU shader pipeline over per-fragment normal response, Fresnel, transmission, depth/self-occlusion, material-specific edge/body behavior and future material evolution.

**INFERENCE:** CPU-side tessellation/face clipping/color updates also give less headroom for 90/120 Hz than a static GPU mesh whose deformation is shader-driven.

v1 is a strong fallback/reference implementation, but it is not the rendering architecture to maintain for years as Veil's defining material feature.

---

## 6. Visual material model

### v1

Good approximation of:
- matte/gloss differences;
- tint/age;
- front/back shading;
- ghost ink;
- source-anchored grain;
- directional fibre;
- edge stroke;
- high-contrast suppression.

But the visual model is primarily vertex/color/Canvas based.

Its newer cylindrical material stack implements four canonical materials; Manuscript is deferred there. The older retained v1 stack contains Manuscript, which is another example of the duplicated-model problem.

### v2

Architecturally supports:
- per-fragment diffuse response;
- material roughness;
- specular exponent/response;
- grazing Fresnel;
- backside transmission;
- separate front/back tints;
- procedural fibre/grain in page coordinates;
- material edge strength;
- depth/self-occlusion;
- layered shadows;
- high-contrast attenuation;
- dark/sepia tone policy.

This is the correct base for all five requested materials.

**P2:** current fibres/grain/edge body are still stylized shader approximations, not a finished premium material library.

---

## 7. Performance

### FACT

No candidate is currently proven at 60/90/120 Hz on representative hardware.

### v1 inference

CPU mesh update + horizon split + color preparation + Canvas vertex draws are plausible at 60 Hz on modern devices, but 90/120 Hz and large/foldable displays remain a risk.

### v2 inference

Static mesh buffers plus GPU deformation/shading are the better long-term frame-pacing architecture.

However, the interaction start currently includes:
- synchronous publication capture;
- possible full texture upload;
- potential destination capture/upload later in the transaction.

Therefore v2's steady-state draw path may be excellent while first-frame latency is poor. These must be measured separately.

### Required measurements

- gesture-to-first-visible-curl latency;
- CPU UI-thread capture duration;
- GL upload duration;
- p50/p95/p99 frame time;
- jank count;
- texture/bitmap peak memory;
- repeated-turn thermal behavior;
- 60/90/120 Hz.

---

## 8. Readium integration

The correct contract is:

- Readium remains publication/navigation truth;
- the renderer never owns the locator;
- preview navigation is reversible;
- commit emits persistence/counting once;
- cancel restores the exact captured start locator;
- durable close waits for/cancels uncommitted preview work;
- stale visual callbacks cannot mutate committed reading truth.

v2's current listener architecture is stronger than v1's current integration because product-mode ownership no longer re-checks the asynchronous Readium overflow flag for Paper/Slide.

v1 still contains:

`!navigator.overflow.value.scroll && isEnabled()`

inside Paper enablement.

That creates a possible split-brain interval where accepted UI says Paper while the asynchronous renderer state still says scroll/native ownership. This is exactly the class of defect capable of making Paper feel like Slide/native movement.

**Canonical rule:** product mode is resolved once at the Reader contract boundary. Readium supplies current locator, progression and navigation operations; it does not independently override the accepted Veil Paper/Slide/Paged mode one frame later.

PDF remains outside the EPUB Paper model.

---

## 9. Mode ownership

Canonical EPUB ownership must be one-hot:

| Product mode | Gesture owner | Visual owner |
| --- | --- | --- |
| PAPER | Veil Paper listener | one GPU Paper renderer |
| SLIDE | Veil Slide listener | Slide overlay only |
| PAGED | static Paged listener | no motion renderer |
| SCROLL | Readium | Readium |

PDF stays renderer-owned where its correctness/zoom/selection semantics require it.

Current v2 source has the cleaner ownership helpers and tests for this one-hot policy.

### Paths that can recreate “Paper behaves like Slide”

1. stale/duplicate overflow checks outside the accepted appearance contract;
2. Paper listener failing to claim the drag start and allowing native swipe to begin first;
3. renderer not reaching READY, causing semantic navigation without visible Paper;
4. persisted Slide appearance overriding debug/review Paper contract;
5. GPU rollout disabled while UI still reports Paper;
6. overlapping directional/tap/key listeners;
7. lifecycle teardown leaving a committed visual coroutine alive across mode handoff.

Several of these were already hardened in v2, but the renderer initialization deadlock remains a direct current failure path.

---

## 10. Accessibility

Good architectural choices already present:
- Paper overlay is decorative/non-focusable;
- touch exploration and selection can return ownership to the renderer;
- Reduced Motion keeps semantic navigation without requiring a curl;
- High Contrast suppresses material noise/ghosting;
- PDF is excluded.

Still DEVICE-REQUIRED:
- TalkBack touch exploration;
- semantic page actions;
- focus behavior with overlay present;
- high-contrast readability;
- TTS while Paper mode is selected;
- hardware keys;
- orientation/recreation.

A renderer that is visually premium but steals semantics is not acceptable.

---

## 11. Sensory architecture

The renderer should **not** directly own audio or vibration.

The correct boundary already exists conceptually:
- engine emits semantic events such as lift threshold, complete, cancel and boundary;
- application sensory policy decides sound/haptic enablement, foreground safety, actuator primitive, volume, repeated-trigger suppression and playback scheduling.

Current Veil sensory policy includes foreground/attachment checks and generation invalidation for stale cues.

**Decision:** retain sound/haptics as a policy layer outside rendering. Material profiles may contain sensory identity parameters, but the GPU renderer must remain silent and actuator-agnostic.

---

# Defect inventory

## v1 — Material Page Engine v1

### P0 canonicalization blockers

1. **Multiple live Paper renderer architectures in one runtime**
   - legacy Paper curl;
   - retained Material strip path;
   - newer cylindrical Canvas mesh.

   This violates the one-canonical-Paper requirement and makes device review ambiguous.

2. **Split-brain mode gate**
   - Paper enablement still re-checks `navigator.overflow.value.scroll` inside the listener.
   - Can allow product appearance and gesture owner to disagree transiently.

### P1 serious

3. Canvas `drawVertices` platform/performance ceiling and reduced older-API path.
4. CPU-side mesh/face/color work reduces 90/120 Hz headroom.
5. Material rendering ceiling compared with programmable GPU shading.
6. Duplicated material/profile/physics stacks can drift.
7. New cylindrical material path does not itself supply the required Manuscript material.
8. Synchronous publication snapshot capture remains a device reliability/latency dependency.

### P2 polish

9. orthographic/no subtle perspective.
10. approximate silhouette/contact shadow rather than a richer geometric shadow model.
11. edge body and fibre remain approximations.

### DEVICE-REQUIRED

12. sustained frame pacing.
13. WebView/Readium snapshot fidelity.
14. OLED seam/low-brightness quality.
15. haptic/sound tuning.
16. foldable/large-screen resize.
17. API 29+ driver behavior.

---

## GPU v2

### P0 blockers

1. **GPU overlay initialization deadlock**
   - `INITIALIZING` status returns before the GL `AndroidView` exists.
   - The renderer therefore cannot create its context and become `READY`.

### P1 serious

2. front/back ownership is primitive-wide through `gl_FrontFacing`; triangles crossing the physical normal horizon are not explicitly split, so a narrow front/back leak or wrong-side sampling remains possible.
3. binding/corner constraint model is weaker than the v1 proven invariant and mature reference engines.
4. destination capture is used as lifted back-face texture, risking duplicate/incorrect physical page semantics.
5. back-face texture orientation is not pixel-proven for both sides and RTL/LTR.
6. synchronous full-resolution `View.draw` capture can block the UI/input path.
7. full GPU texture upload cost is unmeasured and may dominate first-frame latency.
8. memory policy budgets CPU page buffers but not GPU texture memory.
9. TextureView context recreation/pause/resume/rotation lifecycle is unproven on hardware.
10. resize/aspect rejection can produce an invisible visual during an otherwise active transaction unless cancellation/recapture is tightly coupled.
11. current shader-contract tests are lexical contracts, not GLSL compiler/driver tests and cannot prove front/back pixel ownership.
12. manipulation physics are still parameterized/kinematic; apparent mass/friction are not yet fully conveyed through time-domain drag behavior.

### P2 polish

13. layered shadow is approximate rather than explicit geometric base/edge/self-shadow.
14. no subtle perspective/MVP yet.
15. procedural fibre/grain still need premium material art-direction.
16. edge body is shading/tint, not actual geometric sheet thickness.

### DEVICE-REQUIRED

17. real shader compile/link across target GPUs.
18. source snapshot reliability with Readium WebView.
19. front/back orientation and normal-horizon ownership.
20. GL context loss/recreation.
21. first-curl latency.
22. repeated-turn frame pacing.
23. memory pressure/GC/compositor interaction.
24. all five materials on OLED/LCD.
25. 60/90/120 Hz.
26. TalkBack/High Contrast/Reduced Motion/TTS/selection/Notes/Focus Guide/hardware keys.
27. background/process death/close-reopen.

---

# External reference conclusions

The reference engines support the architectural decision, but none should be copied.

## android-pagecurl

Useful evidence:
- real cylindrical geometry;
- explicit front/back ownership;
- curl position/direction/radius as first-class state;
- page-binding correction so the sheet cannot rip away;
- distinct drop/self-shadow geometry.

Canonical lesson: port stronger binding invariants into Veil GPU geometry.

## XBPageCurl

Useful evidence:
- GPU triangle mesh;
- cylinder deformation;
- separate front, back and next-page surfaces;
- VBO/IBO reuse;
- explicit lifecycle/resource management;
- MVP projection.

Canonical lesson: GPU is the right rendering substrate, and a physically correct book model distinguishes back-of-leaf from page-underneath.

## PageFlip

Useful evidence:
- touch-origin-driven semi-cylinder;
- fold-front/fold-back/base-shadow/edge-shadow roles;
- origin/diagonal constraints;
- dynamic geometry corrections when a fold leaves legal bounds.

Canonical lesson: improve pointer/binding geometry before adding more decorative shader complexity.

## irurueta Android GL curl

Useful evidence:
- TextureView is appropriate where normal Android view hierarchy composition/transparency matters;
- surface lifecycle is part of the engine contract, not a detail.

Canonical lesson: keep TextureView, but prove context recreation.

## Compose pagecurl

Useful evidence:
- Compose state/gesture separation;
- clear interaction zones and state ownership.

Canonical lesson: use it as a state architecture reference, not as the premium rendering ceiling.

## StPageFlip

Useful evidence:
- constrained corner geometry;
- resize handling;
- animation serialization;
- hard/soft-page product semantics.

Canonical lesson: useful geometry/test reference, not a drop-in renderer and not a substitute for Veil's RTL/Readium contracts.

---

# Canonical convergence plan

## Canonical branch

`integration/page-engine-canonical-v1`

Base:
`438a967a074484aec373a8edc15eeca467ffaa13`

This preserves the current Grayfog product line as the source of truth.

## What to port from GPU v2

Port selectively:
- single GPU Paper renderer;
- `GLTextureView` host;
- static GPU mesh/VBO/IBO;
- GPU material shader architecture;
- persistent textures;
- ping-pong CPU capture buffers;
- explicit renderer status/failure containment;
- accepted-appearance one-hot mode ownership;
- Paper/Slide/Paged/Scroll listener fixes;
- stale-prewarm fix;
- exact locator restore/handoff fixes;
- Scroll keyboard ownership fix;
- current material profiles, including Manuscript;
- renderer-independent sensory policy;
- shader lexical contract tests;
- GPU pure-model tests.

But fix the P0 host lifecycle before enabling Paper.

## What to port from v1

Port **behavioral proof/invariants**, not runtime rendering code:
- source-face ownership tests, especially the RTL leak fixture concept;
- binding constraint invariants and tall/narrow corner cases;
- analytic critically damped release invariants;
- reverse-cancel/velocity tests;
- non-finite input guards;
- resize/aspect safety contracts;
- release endpoint/terminal-clearing tests;
- high-contrast material constraints;
- the full verification discipline and review matrix.

## What must NOT be ported from v1

Do not port into the canonical runtime:
- `PaperCurlDraw.kt`;
- `PaperCurlGeometry.kt`;
- Canvas `MaterialPageRenderer`;
- Canvas `MaterialFaceMesh`;
- the retained strip renderer;
- duplicate `PageMaterialProfile` / `MaterialPageProfile` stacks;
- GPU/Canvas A/B renderer switches;
- any rollout path that can silently substitute another Paper visual.

Keep rollback in Git history, not in the APK.

## Current Grayfog changes v2 lacks

The current product line is ahead of v2 by the latest Grayfog refinements. Because the canonical branch is based on the current UI head, those changes remain present automatically.

Do not back-base the product on v2 and then attempt to reconstruct the latest UI by broad merge.

---

# Required source changes before the GPU renderer may be considered canonical-ready

1. Fix the host initialization state machine:
   - mount the GL view during INITIALIZING;
   - only suppress Paper begin/visual submission until READY;
   - keep FAILED/UNSUPPORTED explicit;
   - avoid a readiness transition that destroys/recreates the host in a loop.

2. Add a host lifecycle regression test that would fail the current deadlock.

3. Make front/back ownership physically explicit in the GPU path.
   - do not rely on primitive-wide `gl_FrontFacing` alone at the normal horizon;
   - either split crossing primitives before submission or use a two-pass/per-fragment physical-face classification with rendered proof that no wrong-side ink survives;
   - add the old v1 leak concept as a GPU pixel regression in both directions.

4. Remove destination-as-back-leaf semantics until a true opposite-leaf surface contract exists.
   - use material back tint + mirrored/attenuated source ink for the lifted back face;
   - keep the Readium destination as the page underneath.

5. Port v1 binding invariants into the pure GPU geometry model.

6. Add pixel/renderer tests for:
   - left/right;
   - forward/backward;
   - LTR/RTL;
   - Persian;
   - front/back texture orientation;
   - no front-ink leak;
   - no destination duplication on lifted back face.

7. Add capture/resize transaction contracts:
   - rotate/resize during drag cancels or recaptures deterministically;
   - no invisible active Paper transaction.

8. Add memory budget accounting that includes estimated GPU texture storage, not only Java bitmap storage.

9. Keep PDF excluded.

10. Keep Slide free of Paper mass/material behavior.

11. Keep sound/haptics outside renderer.

---

# Verification gate after convergence

Run on the canonical convergence branch:

`./gradlew :app:testDebugUnitTest`

`./gradlew :app:lintDebug`

`./gradlew :app:assembleDebug`

`./gradlew :app:assembleDebugAndroidTest`

Then explicitly report:
- total test count;
- suite count;
- failures/errors/skips;
- Reader-related count;
- GPU/shader test count;
- geometry/binding test count;
- mode-ownership count;
- RTL/LTR count;
- resize/reduced-motion count;
- lint errors/warnings;
- APK assembly result;
- AndroidTest APK assembly result;
- `git diff --check`.

Do not call the branch build-green if the runner again terminates with no executed steps.

---

# Real-device gate

No final success claim is permitted until hardware verifies:

- Paper visibly renders and never silently becomes Slide;
- forward/backward;
- LTR and RTL;
- Persian;
- Glossy;
- Matte Book;
- Parchment;
- Papyrus;
- Manuscript;
- slow drag;
- fast flick;
- reverse cancel;
- top corner;
- middle edge;
- bottom corner;
- repeated turns;
- boundary behavior;
- orientation change;
- fold/resize;
- 60/90/120 Hz where available;
- Reduced Motion;
- High Contrast;
- TalkBack;
- TTS;
- selection;
- Notes;
- Focus Guide;
- hardware keys;
- close/reopen;
- background/foreground;
- process death.

GPU-specific:
- shader compile/link;
- TextureView creation;
- context loss/recreation;
- front/back orientation;
- snapshot reliability;
- gesture-to-first-curl latency;
- frame pacing;
- memory pressure.

---

# Final board ruling

v1 is currently the more verified implementation, and some of its geometry/correctness work is better than v2's current equivalent.

It still loses the architecture decision because its Canvas/runtime multiplicity is the wrong long-term substrate for Veil's target.

GPU v2 is the architecture that deserves to survive.

The winning system is therefore:

**one GPU Paper renderer, on the current Grayfog product baseline, with v1 correctness invariants selectively transplanted, no Canvas Paper fallback, no renderer duplication, Readium remaining locator truth, and no production promotion before real-device proof.**

PR #372 should become a historical/reference donor after convergence.

PR #373 should not be merged as-is. Its renderer architecture should be transplanted and corrected on the canonical integration branch.
