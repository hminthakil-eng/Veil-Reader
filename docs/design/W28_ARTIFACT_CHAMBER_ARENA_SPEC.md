# W28 — Artifact Chamber Arena Spec

Date: 2026-09-30
Canonical branch: `alpha/w28-artifact-chamber-rebuild-v1`
Parent: `alpha/w27-grayfog-archive-rebuild-v1`

## Product role

Book Detail is the transition chamber between Archive and Sanctuary. It should feel like examining a
specific object with history, traces and provenance before opening it.

## First-glance hierarchy

1. The physical book artifact.
2. Title / author / series identity.
3. Reading state and current location.
4. One dominant reading action.
5. Preserved fragments and archive history.
6. Favorite / metadata / collection / delete as secondary utilities.

## Material law

- Dark archive shell and authored chamber image.
- Cover is the richest object.
- Antique brass is structural.
- Parchment is reserved for the primary reading action and preserved-document moments.
- Oxblood is reserved for favorite/seal emphasis.
- No generic bright primary button.
- No row of equally weighted dashboard actions.

## Interaction law

- Primary reading action remains obvious with one glance.
- Favorite and edit remain available but visually subordinate.
- Close/dismiss remains reachable and predictable.
- Existing metadata edit, delete, highlights, milestones, cycles and archive-memory information remain intact.
- Book open action preserves the existing imported/publication availability gate.

## Responsive / RTL law

- Compact layout stacks cover and identity.
- Wider layout places identity beside the artifact.
- Large text must not hide the main CTA.
- Persian/Arabic text remains shaping-safe.
- Long title, author, series and chapter strings are bounded.

## Performance law

- No blur.
- No per-frame random work.
- No network-loaded decorative assets.
- Reuse existing cached book covers and deterministic Canvas ornament.

## W28 first slice

- Strengthen chamber hero and artifact registration.
- Replace brass-filled primary button with parchment reading action.
- Replace equal-weight outlined utility buttons with a restrained archive utility rail.
- Recompose progress as an archive reading record.
- Convert badges and collections away from generic chip/card styling.

## Rejection conditions

Reject if:
- Book Detail looks like a shopping/product page.
- Favorite/edit compete with Continue Reading.
- The cover becomes visually secondary to decoration.
- Parchment floods the whole screen.
- Metadata/history is hidden behind unnecessary navigation.
- Reader or persistence ownership changes.
