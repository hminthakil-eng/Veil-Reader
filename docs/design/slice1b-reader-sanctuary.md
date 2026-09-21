# Slice 1B — Reader Sanctuary

Status: **DRAFT / stacked on Slice 1A**
Parent: `ux/slice1a-veil-identity-foundation`

## Goal

Apply Veil identity to the Reader shell without touching the reading engine.

The Reader is the **Sanctuary** realm:
- content dominates;
- chrome stays hidden until requested;
- controls are calm, precise and reversible;
- motion explains continuity but never delays reading.

## Changes in this slice

- Reader chrome uses Veil semantic motion timings.
- Chrome surfaces use the Sanctuary surface alpha and smaller panel radius.
- Heavy tonal/shadow treatment is reduced.
- Progress percentage is no longer rendered as a decorative pill.
- Close control is visually quieter while keeping a 48dp target.
- Appearance hierarchy adopts the Reader/Sanctuary language.
- Theme preset labels become clearer and more editorial: Ivory / Warm / Ink / OLED.

## Intentionally unchanged

- Readium engine and navigator.
- EPUB pagination/scroll behavior.
- PDF navigator behavior.
- persisted reading location.
- highlights, bookmarks and notes behavior.
- process-death/session architecture.
- page-turn adapter behavior.

## Verification gates

Before this PR can leave draft:

- [ ] `assembleDebug`
- [ ] unit tests
- [ ] lint
- [ ] EPUB open/resume
- [ ] EPUB paged navigation
- [ ] EPUB scroll mode
- [ ] appearance persistence
- [ ] PDF open/zoom/pan smoke
- [ ] RTL smoke
- [ ] TalkBack labels on visible chrome
- [ ] no progress-loss regression

## Design acceptance

Reader chrome must:
1. remain hidden by default;
2. never obscure more content than necessary;
3. avoid decorative container/pill proliferation;
4. preserve 48dp touch targets;
5. use literal accessibility labels;
6. feel visually distinct from Archive while sharing the same system;
7. remain fully reversible.

## Next slice

**Slice 1C — Reader Settings Architecture**

Focus:
- Quick vs Advanced settings;
- format-aware EPUB/PDF controls;
- brightness strategy;
- per-book vs global preference model;
- reset semantics;
- page-transition preference surface.
