# GPU Material Page Engine v2

Status: **SOURCE IMPLEMENTED / BUILD + DEVICE VERIFICATION REQUIRED**

Branch: `reader/gpu-material-page-engine-v2`

This engine replaces the Material Page Engine v1 Canvas strip-warp renderer with a GPU triangle mesh driven by a virtual cylinder. Reader correctness, locator ownership, navigation transactions, material physics, material profiles, haptics, sound, RTL/Persian behavior, Reduced Motion and High Contrast remain Veil-owned.

## Why v2 exists

Material Page Engine v1 had strong material identity but its final deformation was still produced by per-strip `Matrix.setPolyToPoly` warps on an Android Canvas. That can imitate a fold, but its silhouette, rollover, self-occlusion and front/back transition are limited by the 2D rendering model.

v2 moves only the visual deformation layer to a real GPU mesh.

## Benchmark sources studied

The implementation was informed by the architecture and mathematics of several permissively licensed page-curl projects. No source file was copied into Veil.

### harism/android-pagecurl — Apache-2.0

Useful ideas:
- dynamic triangle mesh instead of a flat page transform;
- curl radius/direction as first-class geometry;
- separate front/back-facing regions;
- drop/self-shadow derived from page deformation;
- render-when-dirty behavior.

Veil adaptation:
- no dependency or source copy;
- existing Readium navigation stays authoritative;
- Veil material profiles and interaction policy drive the mesh.

### xissburg/XBPageCurl — MIT

Useful ideas:
- virtual-cylinder page deformation;
- GPU vertex deformation rather than CPU bitmap slicing;
- front/back face shading from deformed geometry;
- cylinder position/direction/radius as the compact curl state.

Veil adaptation:
- clean-room GLES 2.0 shaders;
- normalized page coordinates with aspect correction;
- Veil-specific material optics, backside ink transmission, grain/fibre, shadow and High Contrast behavior.

### albertoirurueta/irurueta-android-gl-curl — Apache-2.0

Useful ideas:
- a GL renderer as an Android view that can coexist with ordinary application UI;
- explicit modern Android GL lifecycle and renderer separation;
- keeping page textures and deformation inside GPU-owned rendering.

Veil adaptation:
- GPU overlay sits above Readium only for Paper mode;
- v2 ships no legacy/Canvas curl fallback; unsupported GPU/reduced-motion cases keep semantic navigation without a curl visual.

### oleksandrbalan/pagecurl — Apache-2.0

Useful ideas:
- Compose-friendly gesture/state separation;
- clear front/back page semantics;
- Compose-first gesture/state separation remains useful as an architectural reference, not as a runtime fallback.

Veil adaptation:
- Veil keeps one Paper renderer in v2: the GPU path. The old Canvas implementation remains only in Git history/PR #372.

### AlShevelev/PageTurningLib — MIT

Useful ideas:
- texture reuse/caching;
- page texture lifecycle separate from curl geometry;
- front/back material handling.

Veil adaptation:
- source and destination captures use ping-pong CPU bitmap slots so the UI thread never overwrites the bitmap the GL thread may still be uploading;
- prewarm allocates memory only; page content is always captured fresh at turn start;
- persistent GPU texture storage is updated with `texSubImage2D` when dimensions are unchanged.

## v2 architecture

### 1. Reader/navigation ownership stays unchanged

`PaperCurlInputListener` still owns:
- gesture reservation;
- Readium preview navigation;
- commit/cancel decision;
- exact locator restoration;
- durable commit;
- boundary behavior.

The renderer never changes a locator.

### 2. Existing Veil physical model stays unchanged

`MaterialPagePhysics.kt` and `MaterialPageProfiles.kt` still own:
- apparent mass;
- bend stiffness;
- drag resistance;
- binding constraint;
- release thresholds;
- spring response;
- material optics;
- patina;
- translucency/ink ghosting;
- sensory identity.

### 3. New pure GPU frame model

`GpuPageCurlModel.kt` maps the current Veil state to:
- cylinder X position;
- physical pull origin;
- cylinder tilt;
- material-dependent radius;
- turn side;
- edge strength;
- shadow strength.

It is renderer-independent and unit-testable.

### 4. Triangle mesh

`GpuMaterialPageCurlView.kt` uses a static GPU grid selected by device memory tier:
- normal device: 72 × 14 quads;
- low-RAM device: 48 × 8 quads;
- vertex/index buffers created once;
- no per-frame mesh allocation.

The vertex shader performs the cylinder deformation. Future quality scaling should be driven by curvature/screen error rather than adding arbitrary density.

### 5. Real rollover

Vertices are handled in three regimes:
- before cylinder: flat front page;
- on cylinder: curved surface with changing normal;
- beyond half revolution: physical back page continuing past the cylinder.

Depth testing provides self-occlusion rather than faking the complete fold with clipped 2D strips.

### 6. Material shading

The GPU fragment path preserves:
- front material tint;
- distinct back surface;
- backside ink transmission;
- roughness/specular response;
- stable page-coordinate grain;
- directional fibre;
- material edge body;
- High Contrast noise suppression.

### 7. Cast shadow

The same deformed mesh is rendered first as a translucent offset shadow pass, then as the physical sheet. This keeps shadow motion tied to page lift without creating a second CPU geometry model.

### 8. Aspect-correct corner pull

Cylinder geometry is solved in page-width physical units, with Y multiplied by page aspect before deformation and restored afterward. This prevents diagonal corner pulls from becoming distorted on tall phone screens.

### 9. GPU warm-up

When debug Material review is enabled and Reader is in Paper mode, the transparent GL view stays resident while idle. Shader compilation, mesh buffers and GL context are therefore already ready when a drag begins.

### 10. Safety/fallback

- production rollout remains gated;
- Debug review forces `Paged + Paper`;
- devices without the required GPU path do not substitute an older curl renderer;
- Slide remains completely separate;
- PDF remains unchanged;
- GPU overlay is non-clickable, non-focusable and accessibility-neutral;
- Readium remains the only navigation source of truth.


### Shader interface contracts

Arena found three independent GLSL defects capable of preventing the renderer from appearing at all:
- duplicate vertex `uSideSign`;
- fragment use of undeclared `uTexelSize`;
- fragment use of undeclared `uSideSign`.

All are fixed. `GpuPageShaderContractTest` now verifies unique/declared uniforms, critical uniform presence, and exact vertex/fragment varying compatibility.

### Physical pointer travel

The cylinder model no longer derives radius from progress alone. `MaterialPageEngineState` tracks normalized physical pointer travel from the real drag vector. Early small travel keeps the curl tight, travel opens the cylinder, and terminal travel tightens it again. This is informed by touch-to-origin/semi-perimeter behavior in mature PageFlip engines while retaining Veil's material profiles and Readium transaction model.

### Source prewarm correctness

`prepareBuffer()` is allocation-only. It never caches publication content. `begin()` captures the current Readium surface fresh. This prevents a locator change between idle prewarm and gesture start from curling stale page content.

## Verification gates

Before promotion:

1. `:app:testDebugUnitTest`
2. `:app:lintDebug`
3. `:app:assembleDebug`
4. `:app:assembleDebugAndroidTest`
5. device checks on 60/90/120 Hz
6. LTR + RTL/Persian
7. left and right turns
8. center, top-corner and bottom-corner pulls
9. slow drag, flick, reverse-cancel, boundary
10. all five materials
11. Reduced Motion
12. High Contrast
13. light/sepia/dark/OLED
14. rotation/resizing
15. repeated turns for texture-buffer reuse and GL lifecycle
16. direct GPU v2 vs Slide/Paged separation review

Until these pass, status is **not device-green**.


## Masterpiece hardening pass

The second implementation pass incorporated additional ideas from the benchmark engines while preserving Veil's own reader contracts.

### Dual front/back page textures

Mature curl engines treat the two sides of a physical leaf separately. v2 now captures the Readium preview destination after navigation settles and uploads it into a second reusable GPU texture. The back face samples that destination with corrected horizontal orientation so it reads naturally as the leaf rolls over.

If the destination capture is unavailable, the shader falls back to mirrored source ink-through instead of showing an invalid or blank texture.

The second CPU bitmap is enabled only on non-low-RAM devices with a normal modern memory class. Both CPU buffers are reusable and manual `Bitmap.recycle()` is intentionally avoided so the GL thread can never race a recycled upload source.

### Stable GPU texture storage

Front and back texture objects are created once. When page dimensions remain unchanged, subsequent turns update storage with `texSubImage2D` instead of reallocating with `texImage2D`. A 1×1 transparent image initializes both texture objects so every sampler is complete before the first destination capture.

### Material lighting

The fragment path now uses the deformed surface normal for:
- diffuse response;
- roughness-dependent half-vector specular;
- grazing Fresnel response;
- back-face transmitted light;
- curl self-occlusion;
- material-colored free-edge body.

Papyrus/manuscript fibre and page-coordinate grain remain deterministic rather than screen-space noise.

### Layered shadow

The page mesh is projected three times with increasing offset/falloff before the physical sheet pass. This approximates the contact-to-penumbra behavior used in mature OpenGL curl engines without allocating a blur texture every frame.

### Gesture-directed cylinder

The GPU model now uses an aspect-independent diagonal pull signal derived from the actual finger vector. Pull origin, vertical displacement and diagonal slope jointly drive the cylinder position and tilt. This avoids the weak diagonal response produced by height-normalized vertical bias on tall phones.

### Radius choreography

Material stiffness and apparent mass define the base cylinder radius. Radius breathes while the page is lifted, then tightens during the final 28% of travel so terminal motion clears the viewport instead of reading as a constant plastic roll.

### Adaptive mesh quality

Low-RAM devices use a 48×8 grid. Normal devices use a 72×14 grid. Both are static GPU buffers; no mesh allocation occurs during drag frames.

### Frame delivery

Compose owns lifecycle and accessibility, but high-frequency Material state is streamed to the GL view through `snapshotFlow`. The `AndroidView` itself is not updated on every progress tick, reducing UI-tree work during 90/120Hz drags.

### GPU failure containment

v2 checks:
- GLES 2.0 availability;
- `GL_MAX_TEXTURE_SIZE`;
- texture upload errors;
- snapshot aspect compatibility after resize/rotation;
- runtime shader/program initialization.

Any renderer failure is explicit: semantic navigation remains available, but v2 does not silently substitute an older curl renderer.

### Single-renderer review

Debug Settings now targets GPU v2 only. Legacy Paper and Canvas Material v1 were removed from the v2 runtime so a real-device review cannot accidentally judge the wrong renderer. The persistent Reader HUD reports GPU Paper ownership and begin attempts directly.


## GPU-only runtime cleanup

The v2 branch intentionally removes the old runtime renderer files and paths:
- `PaperCurlDraw.kt`
- `PaperCurlGeometry.kt`
- `MaterialPageGeometry.kt`
- legacy `PaperVisualEngine`
- Canvas `MaterialPageOverlay`
- `Matrix.setPolyToPoly` strip deformation
- GPU/Canvas A/B rollout controls

The rollback source remains Git history and PR #372; it is not bundled into the v2 APK.

The GPU host was also moved from `GLSurfaceView` with an on-top Surface to `GLTextureView`, using the already-present irurueta GL utils dependency. This keeps curl rendering in the normal View hierarchy and removes the separate-surface composition/Z-order ambiguity.
