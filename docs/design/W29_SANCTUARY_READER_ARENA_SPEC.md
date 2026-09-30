# W29 — Sanctuary Reader Arena Spec

Date: 2026-09-30
Canonical branch: `alpha/w29-sanctuary-reader-rebuild-v1`
Parent: `alpha/w28-artifact-chamber-rebuild-v1`

## Product role

Sanctuary is the quietest realm in Veil Reader. The book content must dominate. Veil identity is
expressed through material, typography, restraint and interaction quality — not persistent ornament.

## Non-negotiable reliability boundary

Do not rewrite the working Reader navigation engine for visual cleanliness.

Preserve:
- Readium EPUB/PDF ownership;
- `ReaderInputArbiter`;
- distinct PaperCurl / Slide / StaticPaged listeners;
- mode-handoff snapshot protection;
- durable locator close/background behavior;
- previous-location return;
- selection/highlight/note flows;
- accessibility and reduced-motion ownership.

## Navigation modes are separate products

### Paper / Curl
- paginated only;
- owns drag from gesture start;
- weighted physical sheet;
- dynamic crease/contact shadow;
- cancel restores exact starting locator;
- no native slide leaking underneath.

### Slide
- paginated only;
- distinct sheet translation;
- separate state and listener;
- no curl geometry.

### Paged
- static page change;
- deliberate drag/tap navigation;
- no curl and no slide animation.

### Continuous Scroll
- renderer-owned continuous flow;
- no page-turn overlay;
- paged-only column controls disabled.

One selected mode must map to one ownership path.

## Reader canvas

- no persistent decorative frame;
- no archive cards over text;
- no background spectacle;
- page atmosphere stays extremely low-density;
- reading surface may be Paper / Sepia / Dusk / OLED independent of shell theme.

## Chrome

- center tap remains the primary reveal gesture;
- auto-hide remains;
- top chrome is title/location/progress only;
- bottom chrome is Notes / Bookmark / Appearance-or-Zoom only;
- use flat edge rails and hairlines, not floating rounded cards;
- controls remain >=48dp;
- reduced motion uses fade-only transitions.

## Appearance

- live preview remains.
- Quick vs Advanced remains.
- navigation modes must be visibly and semantically distinct.
- changing mode must cancel unfinished Paper/Slide preview before renderer handoff.
- mode settings must not be represented as a confusing combination of Scroll + Page Turn toggles.

## PDF

- PDF never inherits EPUB page-turn styling.
- Zoom/workspace ownership stays separate.
- page-turn preference changes cannot alter PDF behavior.

## W29 first slice

1. Flatten top and bottom reader chrome.
2. Make navigation mode selector visually explicit without card-wall styling.
3. Add/strengthen source tests that Paper, Slide, Paged and Scroll ownership are mutually exclusive.
4. Preserve all persistence and gesture code unless a test proves a defect.

## Rejection conditions

Reject if:
- chrome competes with the page;
- Paper and Slide can both own the same drag;
- Scroll can show a page-turn overlay;
- PDF changes with EPUB page-turn preference;
- redesign touches persistence without evidence;
- a visual effect adds lag to the reading surface.
