# Codex Cloud — Grayfog Masterpiece V2 Mission

Continue the existing draft PR and branch:

- PR: #371
- Branch: `design/codex-grayfog-masterpiece-v1`
- Base: `integration/arena-reader-canonical-convergence-v1`

Read first:
- `docs/design/grayfog-masterpiece-v1/LITERARY_REFERENCE.md`
- `docs/design/grayfog-masterpiece-v1/AUDIT.md`
- `docs/design/grayfog-masterpiece-v1/HANDOFF.md`
- existing Grayfog/Arena design contracts and canonical convergence handoff

Do **not** create another design branch unless necessary for safety. Continue refining the current PR.

## Objective

Push the UI from a strong first pass to unmistakably authored, world-class Veil Reader quality.

Focus on **composition, material, spatial hierarchy, typography, atmosphere and visual identity**, not just accessibility polish.

## Highest-priority review order

1. Threshold
2. Library / Archive
3. Book Detail
4. Reader Sanctuary
5. Paper Curl material behavior
6. Appearance / Settings
7. Notes / Highlights / Memory
8. Observatory
9. Castle
10. Path / Ritual / Sanctum
11. Loading / empty / error states

## Required behavior

For each major surface:

1. Compare current composition against the supplied image references and literary reference.
2. Identify the single biggest visual mismatch.
3. Implement the smallest coherent correction.
4. Re-check RTL, Persian, 150% and 200% text.
5. Preserve Reader correctness and ownership.
6. Add or refine preview/device evidence coverage.
7. Repeat until hierarchy and material language are coherent.

## Visual direction

Strengthen:
- dark architectural shell
- thin brass structure
- editorial hierarchy
- parchment as meaningful material
- restrained celestial geometry
- spatial rather than card-based composition
- archive records instead of generic panels
- quiet, low-density Sanctuary
- stronger world identity in Threshold / Observatory / Castle

Reduce:
- generic rounded cards
- repeated panel frames
- unnecessary glow
- dashboard rhythm
- decorative ornament with no semantic role
- uniform density across realms

## Reader guardrail

The Reader remains the most important surface.

Do not change:
- durable progress semantics
- Readium/Pdfium ownership
- navigation transaction policy
- TTS architecture
- Paper/Slide/Paged/Scroll ownership
- selection/annotation atomicity
- focus/accessibility safeguards

Reader design must adapt around correctness.

## Paper Curl

Improve only source-safe visual material aspects:
- lifted-sheet light response
- contact shadow
- crease depth
- backside translucency
- edge thickness
- subtle fibre/grain
- paper age/patina

Do not rewrite geometry or gesture thresholds without device evidence.

## Device evidence

Keep the PR Draft until real Android screenshots are reviewed.

Required screenshot matrix should eventually include:
Threshold, Library modes, Book Detail, EPUB themes, Paper Curl mid-drag, Slide, Paged, Scroll, PDF portrait/landscape, Appearance, Notes, Profile, Observatory, Castle, Ritual, Sanctum, Persian 100/130/150/200%, high contrast, Reduced Motion, tablet, foldable and landscape.

Compilation is not visual acceptance.

## End state

The UI should be identifiable as Veil Reader even with the app name and logo removed.

Do not merge to main.
Do not force-push.
Do not delete historical branches.
Keep #371 Draft until visual/device gates are genuinely satisfied.
