# W26 — Canonical Arena UI/UX Rebuild Contract

Date: 2026-09-30  
Canonical branch: `alpha/w26-arena-canonical-ui-rebuild-v1`  
Base: `alpha/w25-book-detail-hierarchy-v1`

## One-line rule

From W23 onward there is one development spine:

`W23 → W24 → W25 → W26 → ...`

No new product branch may bypass this spine. Experimental design branches are input only; they do
not become alternate products.

The old `alpha/arena-rebuild-v1` and `alpha/arena-rebuild-v2` branches are salvage sources only.
They are too divergent to merge wholesale. Reusable logic, tests, localization, and Arena tooling
may be ported selectively after comparison against the canonical spine.

## What “rebuild UI/UX from scratch” means

The presentation layer is rebuilt deliberately from first principles against the approved reference
boards. The reading/data engines are **not** thrown away. EPUB/PDF ownership, durability,
navigation, database state, backup, annotations, progression accounting, and lifecycle hardening are
preserved unless a new defect proves they must change.

Arena owns product architecture, visual direction, interaction hierarchy, and visual QA.
Alpha owns canonical integration, regression control, data/reader safety, and final merge decisions.

## Approved reference DNA

The target is the supplied Veil Reader reference set:

- blue-black gothic/cathedral/archive shell;
- antique brass/gold linework and restrained oxblood accents;
- editorial serif hierarchy for Latin UI and a dedicated readable RTL hierarchy;
- authored atmospheric imagery at Threshold/Archive/Book Detail;
- parchment as a material reserved for reading/document surfaces;
- thin architectural frames, seals, celestial/occult line motifs, lamps/candles, deep perspective;
- compact, high-information layouts rather than generic oversized cards;
- Reader as the quietest realm: typography and paper first, chrome disappears;
- no glassmorphism, no pill-everything, no universal rounded cards, no random gradients,
  no cheap fantasy ornament, no motion for decoration alone.

## Reference screen contract

1. **Splash / Welcome** — atmospheric portal, one dominant action, near-zero utility chrome.
2. **Threshold / Home** — “Library awaits” hero, current volume first, recent volumes second.
3. **Grayfog Archive / Library** — search + filters + information-dense archive grid/shelves.
4. **Book Detail / Artifact Chamber** — cover artifact, metadata, progress, primary continue action.
5. **Reader / Sanctuary** — parchment or selected reading material; paper/curl, slide and scroll are
   independent modes; settings available in one gesture.
6. **Reader Settings** — live preview, material/theme, typography, spacing, margins, page-turn mode.
7. **PDF Controls** — PDF owns zoom/fit/width/rotate controls; no EPUB control leakage.
8. **Hidden Archive** — notes/highlights/bookmarks/echoes/capsules as an archival index.
9. **Collections / Shelves** — tactile shelves without turning every row into a floating card.
10. **Settings** — architectural list, clear grouping, bilingual-safe layout.

## Rebuild order

W26 is the visual-foundation and Threshold reset. Next slices stay on the same branch lineage:
Archive → Book Detail → Reader chrome/material → Reader settings/PDF → Hidden Archive →
Collections → Settings → Castle/Ritual/Sanctum → final cross-screen harmony pass.

Each surface must satisfy five gates before moving on:
1. hierarchy/function,
2. reference fidelity,
3. RTL and large-text safety,
4. interaction/state completeness,
5. source-level regression review.

No APK/build is required during this design-development pass unless explicitly requested.
