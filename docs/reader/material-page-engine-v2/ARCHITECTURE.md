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
- unsupported devices fall back to Material Page Engine v1 Canvas renderer.

### oleksandrbalan/pagecurl — Apache-2.0

Useful ideas:
- Compose-friendly gesture/state separation;
- clear front/back page semantics;
- a simple fallback path using clipping/mirroring.

Veil adaptation:
- the v1 Canvas renderer remains the fallback; v2 does not duplicate Reader gesture ownership.

### AlShevelev/PageTurningLib — MIT

Useful ideas:
- texture reuse/caching;
- page texture lifecycle separate from curl geometry;
- front/back material handling.

Veil adaptation:
- existing reusable snapshot buffer remains the single capture source;
- GPU uploads once per turn rather than per drag frame.

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

`GpuMaterialPageCurlView.kt` uses a static 48 × 8 quad grid:
- 441 vertices;
- 2,304 triangle indices;
- vertex/index buffers created once;
- no per-frame mesh allocation.

The vertex shader performs the cylinder deformation.

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
- devices without GLES 2.0 use the v1 Canvas renderer;
- Slide remains completely separate;
- PDF remains unchanged;
- GPU overlay is non-clickable, non-focusable and accessibility-neutral;
- Readium remains the only navigation source of truth.

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
16. direct GPU v2 vs Canvas v1 vs Slide visual comparison

Until these pass, status is **not device-green**.
