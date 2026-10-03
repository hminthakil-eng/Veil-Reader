# Material Page Engine v1

Status: **parallel implementation / controlled rollout**

Branch: `reader/material-page-engine-v1`  
Base: `design/codex-grayfog-masterpiece-v1`

The production Paper mode still defaults to the legacy curl. Material Page Engine v1 is wired behind `MaterialPageEngineRollout.DEFAULT_ENABLED = false`. Debug builds expose a process-local A/B review control in Settings so legacy and Material can be compared on the same device without changing release behavior. This is deliberate: source-level work can mature without deleting or destabilizing the proven Reader path.

## Why the previous Paper mode is not enough

The previous implementation is robust about Reader ownership, but its physical model is still fundamentally a captured page bitmap driven by one fold edge and a mirrored polygon. It already contains useful paper cues — backside tint, contact shadow, edge thickness, sparse fibres and release regimes — yet the material identity is mostly visual configuration around a single deformation model.

That means glossy stock, matte book paper, parchment and papyrus cannot genuinely differ in mass, resistance, bend stiffness, binding behavior, release threshold, acoustic identity and haptic identity.

Material Page Engine v1 separates those concerns.

## Product invariants

The Reader modes remain separate:

- `PAGED`: static paginated navigation.
- `PAPER_CURL`: physical-material interaction path.
- `SLIDE`: fast, direct, low-latency translation with no paper metaphor.
- `SCROLL`: continuous reading.

Paper and Slide never share a visual implementation.

PDF remains on the correctness-first Pdfium/Readium navigation path. The new material engine is EPUB-only during v1 rollout.

## Canonical materials

Source of truth: `MaterialPageProfiles.kt`

- Glossy
- Matte Book Paper
- Parchment
- Papyrus
- Manuscript

Every profile supplies structured physics, optics and sensory parameters. Material identity is not represented by scattered constants.

## Rollout policy

The new engine is not promoted merely because it compiles.

Promotion requires:

1. unit/lint/build gates green;
2. Reader regressions green;
3. review previews inspected;
4. real-phone frame pacing and bitmap behavior judged;
5. touch release, sound and haptic character judged by hand;
6. no correctness regression in locator persistence, close durability, selection, TTS, Notes, hardware keys, RTL or accessibility.

Until then, the legacy Paper implementation remains intact and is the production default.

## Inspectable states

Debug previews live in:

`app/src/debug/java/com/veilreader/app/ui/review/MaterialPageEngineReviewPreviews.kt`

They exercise the real material renderer and real slide overlay with local page fixtures. Covered states include idle, slight lift, medium/deep drag, almost complete, complete/cancel release, fast flick, slow heavy drag, every material, aged/clean, LTR/RTL, Persian, dark, sepia, reduced motion, and the required Slide states.

## Reader correctness preserved

The new engine delegates navigation ownership to the existing Reader transaction path. It does not own persistence, Room, TTS, selection, Notes or PDF.

The existing Paper listener still:

- reserves the gesture before Readium can leak a native slide;
- captures the exact start locator;
- previews destination navigation only when safe;
- restores the exact locator on cancel;
- commits persistence/counting only after a real turn;
- participates in durable-close cancellation;
- respects RTL edge/direction mapping.

The engine changes the material response, not Reader truth.
