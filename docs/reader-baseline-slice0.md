# Reader Baseline — Slice 0

Status: **execution baseline**
Scope: current `main` reader before narrative UX redesign
Policy: no reader-engine rewrite, no live Manga source, preserve EPUB/PDF behavior.

## Purpose

This document freezes the current Reader behavior and defines the acceptance gates that every UX/UI change must preserve or improve. It is intentionally implementation-aware and small enough to review before Slice 1 (Design System Foundation).

## Current architecture snapshot

- Android-first, Jetpack Compose shell.
- Readium Kotlin Toolkit 3.4.0 for EPUB/PDF.
- Reader uses existing Readium fragment navigators; no migration to experimental Compose navigators.
- Reader business/session state is separated into `ReaderViewModel`.
- Reading progress and durable library state are persisted through the repository layer.
- Reader appearance is persisted and reapplied.
- PDF opens with PDFium and scroll mode enabled by default.
- Baseline Profile infrastructure is present.
- Material 3 and adaptive-layout dependencies are present.

## Confirmed strengths on current main

1. Reader chrome starts hidden, which is directionally correct for the Sanctuary model.
2. Center-tap toggles Reader chrome.
3. Progress persistence is no longer only ephemeral UI state.
4. Appearance settings are persisted instead of recreated from defaults every entry.
5. EPUB appearance includes theme, text scale, line height, margins, paged/scroll mode and publisher-style control.
6. OLED has a real black EPUB background override.
7. Highlights/bookmarks and Reader session tracking exist.
8. Readium 3.4 PDF locator compatibility work has already landed.
9. Light and dark application color schemes are distinct.
10. Baseline Profile + benchmark build plumbing already exists.

## Known UX/quality gaps that remain P0/P1

### P0 — reading integrity
- Resume must be verified after process death, rotation, appearance change and navigation away/back.
- Location must remain semantically stable when font size, margins, publisher styles or scroll mode changes.
- No progress/annotation/bookmark loss is acceptable.
- EPUB and PDF behavior must remain protected during UI redesign.

### P0 — page-turn quality
Current paged navigation still uses `DirectionalNavigationAdapter(animatedTransition = true)`. User testing reports that page turns feel like a generic slide. This must be measured and redesigned without replacing the reader engine.

Acceptance:
- no accidental double-turns;
- no gesture conflict with center-tap chrome;
- no visible blank frame between pages;
- motion can be disabled/reduced;
- page-turn option must preserve location.

### P0 — PDF interaction
PDF is currently initialized with PDFium defaults using scrolling. PDF-specific presentation controls remain thinner than EPUB controls.

Acceptance:
- pinch zoom and pan reliable;
- double-tap behavior explicitly defined;
- paginated vs continuous mode verified with Readium 3.4;
- switching mode preserves location;
- large PDF stress test does not regress memory stability.

### P0 — theme/appearance integrity
Application shell themes are distinct, but Reader appearance is a separate Readium presentation system and must be treated independently.

Acceptance:
- Paper / Sepia / Dusk / OLED are visually distinct;
- OLED uses true-black canvas where appropriate;
- theme change preserves position;
- settings survive app restart;
- contrast remains accessible;
- publisher styles do not silently override required accessibility choices.

### P0 — settings UX
Current Appearance sheet is functional but still a flat control surface.

Acceptance for next design slice:
- Quick settings: theme, text size, reading mode, brightness strategy;
- Advanced: font, weight where supported, line height, margins/content width, alignment, publisher styling, gestures/page animation;
- clear “this book vs all books” semantics before per-book overrides ship;
- Reset behavior is explicit.

### P1 — Reader Sanctuary
The Reader must become the quietest surface in the product.

Acceptance:
- no permanent gamification layer over prose/artwork;
- chrome hidden by default;
- controls are contextual and reversible;
- navigator unifies Contents / Search / Bookmarks & Highlights / location return path;
- accessibility labels remain literal and clear even when visual styling is narrative.

### P1 — RTL
RTL is a release gate, not polish.

Verify:
- Persian/Arabic prose shaping;
- mixed-direction metadata;
- next/previous semantics;
- directional icon mirroring;
- progress labels;
- search results;
- bookmarks/highlights;
- Reader settings;
- manga reading direction remains independent from UI locale when Manga ships.

## Competitive Regression Matrix

Every Reader-affecting PR should mark the applicable cells.

| Gate | EPUB | PDF | RTL | Offline | Process death | Accessibility |
|---|---|---|---|---|---|---|
| Open | ☐ | ☐ | ☐ | ☐ | — | ☐ |
| Resume | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Pagination / scroll | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Zoom | N/A/text sizing | ☐ | ☐ | ☐ | — | ☐ |
| Theme | ☐ | shell/PDF policy ☐ | ☐ | ☐ | ☐ | ☐ |
| Settings persistence | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Highlight | ☐ | capability-specific | ☐ | ☐ | ☐ | ☐ |
| Bookmark | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Large publication | ☐ | ☐ | ☐ | ☐ | — | — |
| Reduced motion | ☐ | ☐ | ☐ | ☐ | — | ☐ |

## Evidence to capture before Slice 2 changes

For the current reference build, capture:
1. EPUB open → resume.
2. EPUB paged turn.
3. EPUB continuous scroll.
4. Font-size change while mid-chapter.
5. Theme change while mid-chapter.
6. App background/foreground restore.
7. Process-death restore.
8. PDF open → zoom/pan.
9. PDF continuous → paginated if exposed in the test build.
10. RTL sample EPUB.
11. Very large EPUB/PDF.
12. TalkBack traversal through visible Reader chrome.

Each capture should record:
- build/commit SHA;
- device/API level;
- publication type;
- expected behavior;
- observed behavior;
- pass/fail;
- screenshot/video/log reference when useful.

## Design Constitution gates

A UI change must answer yes to all applicable questions:

1. Does it improve reading or navigation?
2. Does it belong to Veil rather than a generic template?
3. Does it avoid adding narrative tax?
4. Can it disappear when the story needs the screen?
5. Is it reversible?
6. Does it preserve EPUB/PDF behavior?
7. Does it remain understandable with accessibility services?
8. Does it preserve or improve performance?

## Next slice

**Slice 1 — Design System Foundation**

Do not redesign full Home/Archive yet.

Build only the reusable foundations required by Reader first:
- semantic color roles;
- typography roles;
- spacing and geometry rules;
- Reader surface tokens;
- motion duration/easing grammar;
- reduced-motion policy;
- control states;
- accessibility/touch target rules;
- RTL behavior;
- screenshot/visual-regression fixture strategy.

Then apply those foundations first to **Reader Sanctuary**, not to decorative shell surfaces.
