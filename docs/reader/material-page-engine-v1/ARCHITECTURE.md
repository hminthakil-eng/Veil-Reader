# Architecture

## Boundaries

### Material preset model

`MaterialPageProfiles.kt`

Defines one canonical data model for:

- apparent mass;
- bend stiffness;
- drag resistance;
- settle damping;
- binding constraint;
- completion/cancel thresholds;
- flick velocity;
- front/back/edge optical identity;
- specular response and roughness;
- translucency and ink ghosting;
- grain, directional fibre and edge body;
- patina response;
- acoustic brightness/dryness/body/fibre;
- haptic sharpness/weight and lift threshold.

### Interaction / physics

`MaterialPagePhysics.kt`

The drag response is monotonic and constrained so the rendered page never outruns the finger. Initial pull resistance depends on material stiffness/resistance; vertical motion is limited by binding strength. Release completion is material-specific rather than a universal snap threshold.

Settling uses material-aware damping/stiffness. Heavy surfaces settle more deliberately; glossy stock releases faster and sharper.

### Geometry

`MaterialPageGeometry.kt`

The engine uses a segmented cylindrical deformation rather than one affine page transform. Each frame produces a strip mesh with:

- moving fold axis;
- per-strip projected corners;
- front/back facing classification;
- lift;
- angle-derived light response;
- binding skew;
- mirror-safe left/right geometry.

Left turns mirror geometry, not publication pixels. This avoids accidentally reversing text in RTL/previous-page turns.

### Renderer

`MaterialPageEngine.kt`

The renderer maps bitmap strips through Android `Matrix.setPolyToPoly`. It reuses matrices, paths, arrays and paints through one scratch object and reuses the page capture bitmap between turns.

Per-strip shading includes restrained:

- back-face material color;
- opacity/translucency approximation;
- angle-dependent light response;
- deterministic micro-tonal grain;
- directional fibre for fibrous profiles;
- crease/contact shadow;
- edge-body highlight.

Theme tone is adapted for light, sepia and dark Reader surfaces. Patina modulates grain/body subtly instead of permanently dirtying the page.

### Sensory policy

`MaterialPageSensory.kt`

Material-specific sound/haptic cues are modeled as data for:

- lift threshold;
- completion;
- cancellation;
- boundary contact.

The policy reacts to material and release velocity. The engine exposes a `MaterialPageSensorySink` integration hook.

**Implemented:** cue identity/policy and engine emission points.  
**Not yet promoted to production playback:** a material-specific platform audio/haptic adapter. Existing Reader sensory feedback remains authoritative until device tuning establishes safe amplitude and character. This avoids shipping synthetic cues that have not been judged on real hardware.

### Reduced Motion

Reduced Motion is a first-class engine path. When the rollout is enabled, the engine keeps a restrained page-presence/alpha transition rather than executing the full mesh motion. With the rollout disabled, the existing production reduced-motion behavior remains unchanged.

### Mode controller / rollout

`MaterialPageEngineRollout`

Default: OFF.

`PaperCurlState` retains the entire legacy implementation and delegates to Material Page Engine only when the gate is enabled. No old file or fallback path is deleted.

### Slide

`SlidePageState.kt`

Slide deliberately does not use the material engine. Its response now tracks the finger closely, keeps opacity stable, uses a narrow transition shadow and settles within a shorter latency budget.

## Performance design

The v1 renderer avoids per-frame bitmap allocation. Page capture uses a reusable ARGB buffer. Render scratch resources are remembered once. Mesh complexity is bounded to 12–36 strips (26 canonical). Geometry is O(n) with no recursive solver.

The largest remaining performance uncertainty is GPU/Canvas cost of 26 perspective strip mappings on representative mid-range Android phones. That requires real-device frame timing; source inspection cannot settle it reliably.

## Correctness ownership

Material Page Engine must never own:

- durable progress storage;
- Room transactions;
- Readium/Pdfium publication ownership;
- locator normalization;
- close durability;
- TTS lifecycle;
- text selection;
- annotation transactions;
- Notes atomicity;
- Focus Guide state;
- Lookup;
- ETA;
- hardware navigation.

Those remain in the existing Reader architecture.
