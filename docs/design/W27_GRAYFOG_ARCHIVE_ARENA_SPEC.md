# W27 — Grayfog Archive Arena Spec

Date: 2026-09-30
Canonical branch: `alpha/w27-grayfog-archive-rebuild-v1`
Parent: `alpha/w26-arena-canonical-ui-rebuild-v1`

## Product role

Grayfog Archive is not a storefront grid and not a dashboard. It is the user's private,
searchable memory architecture for books. Retrieval must remain faster than atmosphere.

## First-glance hierarchy

1. Archive identity and count.
2. Search.
3. Current archive facet / shelf / wing.
4. Books.
5. Memory returns and deep shelf.
6. Import, settings, manga and overview as utilities.

## Visual laws

- Architecture before cards: arches, rails, shelves, index lines, seals and registration marks.
- Book covers are the richest repeated objects; their containers stay quiet.
- Antique brass is structural, not decorative fill.
- Parchment is exceptional and should not become the general archive background.
- Oxblood appears as sparse archival seal / priority registration.
- Dense information is allowed if hierarchy remains obvious.
- Avoid equal-weight action buttons floating over the hero.
- Avoid rounded containers around every filter, shelf and book.

## Interaction laws

- Search remains reachable without scrolling through decorative content.
- Shelf, collection and series filtering remains reversible in one tap.
- Gallery, Shelves and Index remain distinct views of the same archive.
- Tapping a book opens reading; record/details remains secondary.
- Import is visible but cannot dominate the library.
- All filter state survives recomposition through existing saveable state.

## Adaptive / RTL laws

- Persian/Arabic copy must not use artificial tracking.
- Long collection and series names truncate without shifting primary controls off-screen.
- At large font scale, the archive header stacks before actions become clipped.
- Minimum interactive target remains 48dp.
- Horizontal shelves may scroll; critical actions may not depend on hidden horizontal overflow.

## Performance laws

- Do not add per-item bitmap effects, blur or random animation.
- Reuse cached covers.
- Procedural ornament must be deterministic and allocation-light.
- Keep one lazy archive viewport.
- Do not introduce nested vertical lazy containers.

## W27 first slice

- Recompose archive header as an architectural title field with a seal and utility rail.
- Convert shelf filters from dashboard cards into archive registers.
- Convert Recent Reading from elevated cards into reading slips / shelf artifacts.
- Preserve all existing filtering, sorting, view-mode, metadata and opening behavior.

## Rejection conditions

Reject if the result:
- looks like a generic media library with a fantasy skin;
- makes import/settings stronger than books;
- hides search;
- adds extra navigation depth for ordinary retrieval;
- makes every shelf/filter a card;
- harms RTL, large text, or touch targets;
- changes Reader/data ownership.
