# W27 — Grayfog Archive Source QA

Date: 2026-09-30
Branch: `alpha/w27-grayfog-archive-rebuild-v1`
Parent: `alpha/w26-arena-canonical-ui-rebuild-v1`

## Canonical drift

- W27 is ahead of W26 and not behind it.
- No historical Arena rebuild branch was merged.
- Reader/data ownership remains outside this slice.

## Retrieval hierarchy

- Archive identity first.
- Search immediately follows identity.
- Shelf/facet registers follow search.
- Gallery/Index/Shelves preserve the same underlying books and filters.
- Manga is demoted below the primary archive content as a utility.
- Import and settings do not dominate the archive title field.

## Visual-language checks

- Header uses archive architecture, brass structure and a density-aware seal.
- Shelf filters are flat registers rather than dashboard cards.
- Gallery keeps the cover as the richest repeated object.
- Index is a flat archival ledger with registration marks.
- Shelves mode reads as grouped book objects, not nested cards.
- Recent Reading is a reading slip/shelf artifact.
- Empty/import states share the archive language.
- Oxblood is sparse and used for registration/favorite emphasis.
- Parchment is reserved for exceptional reading/import actions rather than the whole shell.

## Accessibility / adaptive checks

- Critical controls keep >=48dp touch targets.
- Header reacts to font scale as well as width.
- Persian/Arabic typography continues through the shaping-safe design system.
- Long titles/authors/series use bounded lines and ellipsis.
- Search is not hidden behind decorative or manga content.
- View-mode state exposes `selected` semantics.

## Functional invariants preserved

- Search normalization.
- Shelf filters.
- Collection filters.
- Series filters.
- Sorting.
- Gallery / Shelves / Index modes.
- Favorite action.
- Book open action.
- Archive record/details action.
- Import flow.
- Metadata editing.
- Delete flow.
- Memory returns / deep shelf derivation.

## CI evidence

GitHub Android CI repeatedly produced a failed job with an empty step list. That is classified as
runner/allocation infrastructure evidence, not code-failure evidence. W27 remains draft until a
runner executes real build/test steps.

## Exit status

Source-level design and hierarchy gate: PASS.
Canonical-line gate: PASS.
Runtime/build gate: BLOCKED ON EXECUTING RUNNER, not waived.
