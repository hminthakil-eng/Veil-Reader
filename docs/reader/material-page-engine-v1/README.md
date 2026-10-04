# Material Page Engine v1

Status: opt-in cylindrical mesh; **legacy Paper remains the release default**. Base:
`design/codex-grayfog-masterpiece-v1` at `77be8b473d79f80deb79c0eb141dfd8c802f8099`.
Implementation branch: `reader/material-page-engine-v1`. The requested branch already
existed at `686b1688871f34c03535b1b287fee8ecae818e93`; its earlier material renderer,
previews, tests and history are retained through a non-destructive branch merge.
No main-branch merge or force-push.

Enable **Material paper · Preview** in Settings or Reader Appearance under Paper.
Choose Glossy, Matte Book Paper, Parchment, or Papyrus. This persistent switch
selects the new cylindrical mesh. When it is off, release uses legacy Paper;
debug builds retain the earlier strip renderer's existing debug default. To A/B
against legacy in debug, also turn off the earlier Material Engine review switch.
The old debug presets (including Manuscript) and preview collection remain intact. The gate, preset and patina
also round-trip through the existing local backup; older backups default to legacy
Paper and Matte. Disable the switch to return
to the retained branch path. The switch and material survive SettingsStore recreation.
PAGED, PAPER CURL, SLIDE and SCROLL remain independent navigation modes. This
release does not promote the new Paper renderer to default without phone review.

## Audit

The former engine captures the publication, splits it across an analytically
straight fold and reflects the back region. Its material response largely comes
from shared opacity/color/age constants. Tap/fling keyframes and springs govern
release, rather than the substrate. It can communicate a fold, but cannot produce
a smoothly curved surface with angle-dependent normals and distinct mechanics.
The old `PaperCurlDraw`, geometry and release implementation remain in source.

Slide previously slowed early finger motion with a progressive response coefficient,
faded the content and used a wide shadow. That suggested a weighted sheet and
weakened mode separation. Slide now tracks the finger directly, retains full text
opacity and uses a narrow separator shadow and short, non-oscillating settles.
No Paper coefficients, fibres, backside, geometry or audio enter Slide.

## Boundaries and ownership

`ui/reader/material/` contains the new mesh and the retained strip engine. It has no Room, Notes, TTS,
publication parser or persistence dependency.

- `PageMaterialProfile`: canonical dimensionless mechanics, optics and sensory data.
- `MaterialTurnPhysics`: monotonic resistance and deterministic release/commit policy.
- `CylindricalPageMesh`: inextensible cylindrical bend, surface normals and lift.
- Earlier `MaterialPageEngine`, `MaterialPageGeometry`, `MaterialPagePhysics`,
  `MaterialPageProfiles` and `MaterialPageSensory`: retained strip implementation.
- `MaterialPageRenderer`: reusable mesh, front/back clipping, lighting and edges.
- `MaterialMotionPolicy`: eligibility and reduced-motion routing.
- `MaterialReducedMotionSurface`: still live publication and a fine settling edge.
- `MaterialSensoryPolicy`: semantic cues and quiet substrate-dependent PCM synthesis.
- `MaterialReviewStates`: named inputs to production renderer and release models.

`PaperCurlState` is a compatibility adapter for the existing Reader, not another
Reader architecture. It freezes the engine/material/age at snapshot capture and
routes manipulation/release/rendering to the chosen implementation. The previous
branch renderer selection still executes when the mesh gate is off. A preference edit cannot morph a
lifted sheet halfway through its transaction.

`PaperCurlInputListener` still participates in Readium's own input pipeline.
Neither renderer intercepts touch or owns semantic navigation. Existing hardware
and accessibility page actions use the same listener and its busy guard.

Reflowable EPUB alone is eligible. Fixed-layout EPUB retains its previous branch behavior;
Pdfium and PDF are unaffected. SCROLL, PAGED and SLIDE cannot enter the engine.
Debug review enters Paper only on explicit switch activation, rather than
continuously overriding other mode choices. Snapshot failure is recoverable: navigation can commit on release without visuals.

## Mechanics and geometry

The page is a constrained strip with a cylindrical bend and planar continuation.
The source coordinate measures arc length. Rotating the fold coordinates for
corner/edge pull preserves length rather than stretching the page like rubber.
The bound region stays flat until the advancing fold reaches it. The outside edge
lifts from its actual pull origin; vertical displacement affects the fold within
bounded limits. Projection is orthographic to protect publication legibility.

During manipulation, resistance decreases smoothly with inward displacement.
The sheet cannot outrun the finger. A short slow drag cancels; a deliberate pull
crosses a material-weighted progress threshold. A fresh inward flick can complete
with a smaller pull. An outward release cancels below the near-complete threshold.
Readium's existing deliberate-intent and stale-velocity policies are retained.

Release uses an analytic critically damped mass/stiffness model. Velocity is
bounded in the destination direction. Dropped frames do not destabilize an
integrator; positions are monotonic, with one exact terminal state. The same model
settles cancellation. Boundary feedback uses a small constrained lift, without
counting a turn.

This is a disciplined physical approximation, not finite-element paper mechanics.
It does not simulate crumpling, tearing, collision with fingers, or a loose book
spine. Those effects would undermine predictable reading behavior.

## Materials and light

| Material | Mechanics | Light/surface | Sensory identity |
| --- | --- | --- | --- |
| Glossy | light, crisp, smaller bend radius | low roughness, narrow stronger specular response, clean thin edge | short brighter filtered flick, light tick |
| Matte | balanced mass/resistance | calm diffuse shade, subdued grain, quiet show-through | softer longer rustle, virtual-key response |
| Parchment | heavier, thicker, broader bend | warm body, softer specular, slight tonal irregularity | lower-filtered heavier rustle, context click |
| Papyrus | dry resistance, high stiffness | directional fibre bands, low gloss, irregular outside edge | drier modulated fibre noise, keyboard tap |

Front and back use the same deformed surface mapping. Normal-dependent diffuse
and specular lookup tables provide crease light and dark. The back body is tinted
independently; a restrained second source draw approximates mirrored ink
show-through. Lifted silhouette shadows use a tight contact component and a
small penumbra. Only the physical outside edge gets a thickness stroke; internal
mesh edges are never shaded as sheet edges.

Patina adds a small source-anchored tonal modulation rather than dirt. Grain is
fixed in source coordinates and does not shimmer between frames. Theme colors
come from the actual Reader's Paper/sepia/dark/OLED palettes. Dark theme warmth,
patina and show-through are attenuated. The live publication remains text-first;
idle content is not covered with a new full-page texture layer.

Leather/manuscript is intentionally deferred in the new cylindrical model: a tasteful leather surface is not
well represented by an ordinary thin-sheet cylinder. Four canonical materials are
implemented rather than giving a fifth mesh preset a misleading identity. The
earlier strip renderer retains its existing Manuscript preset for comparison.

## Sensory implementation

Semantic LIFT, THRESHOLD, COMPLETE and CANCEL cues enter the app's existing
`VeilSensoryFeedback`, honoring its sound/haptic settings, volume cap and foreground
lifecycle. Completion is emitted only after successful navigation. No doubled
legacy page sound/haptic accompanies a material commit.

PCM generation and playback happen on the existing cue executor, not on the draw
thread. Material filtering, duration and modulation differ; turn speed changes
attack/duration/gain, and cancellation is softer. Lift and threshold feedback are tactile
only, reserving the acoustic channel for completion/cancellation. A bounded busy flag drops overlapping material audio instead of queueing
rustles. There is no continuous per-frame scrape or vibration stream.

Haptics use supported Android view feedback constants without ignore-preference
flags or a new vibrator permission. Four completion/lift identities are mapped
from profile weight/roughness; cancel is a quiet tick. These are semantic requests,
not promises of identical actuator waveforms across OEMs. Custom amplitude/waveform
and recorded Foley remain future device-led tuning, not implemented capabilities.

## Reduced Motion and accessibility

Reduced Motion never captures a bitmap or starts a speculative visual navigation.
The live publication stays still. A subtle outside edge reflects material
thickness; a completed/cancelled turn can settle opacity over 70 ms. No page
translation, curl, zoom or moving reveal is required. Commit thresholds still use
the material policy and exact locator ownership remains unchanged.

Material controls expose one switch and a radio group with 48 dp minimum row
height, localized English/Persian labels and standard selected/toggle semantics.
Publication TalkBack semantics remain in Readium. Decorative meshes are not
focusable and have no duplicate content descriptions. Keyboard mappings remain
progression-aware; mirrored geometry keeps publication texture coordinates in
original order, including Persian text.

## Correctness preserved

Speculative drag navigation is still suppressed from persistence/counting. The
same PAPER_COMMIT event handles successful turns. Cancels restore the exact
starting Locator, including position/progression/text, before durable close can
snapshot it. Lifecycle teardown and mode handoff retain their synchronous escape
hatch. A committed release stays committed even if its visual animation stops.

Selection and lookup ownership, atomic annotation/Notes writes, TTS sessions,
Focus Guide, ETA, Readium/Pdfium lifetime, hardware dispatch and process checkpoint
policies were not replaced. Existing regression suites cover these boundaries;
new listener contract tests additionally exercise the new engine through the
production transaction path. A unit fake navigator cannot prove WebView timing on
a physical device; see the device review checklist.

## Performance envelope

A reused snapshot is capped at 2 million pixels and an 1800-pixel long edge for
the opt-in material engine (under 8 MB ARGB storage). Existing legacy capture is
unchanged. Mesh resolution is 80 by 24, with reused vertex/normal/height/color
arrays, two silhouette/edge paths and paints, and a 256-entry material lighting table.
Front/back triangles are partitioned before projection drawing, with reused source
coordinates and colors. Their maximum reserved storage is about 0.88 MiB and is
prepared when the material Paper overlay becomes available, retained across turns,
and disposed on mode exit. There is no extra backdrop bitmap or per-frame bitmap generation,
blur filter, random generator, simulation allocation or PCM work. Animation values
are read in the Canvas draw phase. Slide reads animation state in its layer/draw
phases. The normal-horizon interpolation and vertex draws must still be profiled on GPU drivers.
Android API 26–28 uses the live-edge equivalent, because hardware-accelerated
`Canvas.drawVertices` starts at API 29; no full-frame software fallback is forced.
High Contrast reduces grain, ghost ink and shading while retaining edge structure.
Idle pixels preserve the original publication, and resized captures are not stretched.

## Review and verification

Open [the inspectable gallery](review/gallery.html). See [review.md](review.md) for the 27-state render matrix and hands-on acceptance
criteria, and [verification.md](verification.md) for exact commands, totals and
limitations. Native Skia review captures use the production renderer/Slide overlay,
with deterministic publication text fixtures, not an alternate demo Reader UI.

The [source comparison](reference-comparison.md) pins three open-source references,
records the reproduced RTL rendering defect and explains the resulting fixes.
The [current Arena review](arena-comparison-review.md) supersedes the earlier
strip-only source review for this refinement.
