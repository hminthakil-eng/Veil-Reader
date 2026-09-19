# Veil Reader — Competitive Reader Standard (2026 Q3)

This document converts competitor research into implementation requirements.
It is a living benchmark, reviewed under Eyad Studio's quarterly Technology & Standards Refresh.

## Reference products

- Apple Books — benchmark for calm reader chrome, typography controls, accessibility and polish.
- Kindle — benchmark for readability options, accessibility, search/read-along ecosystem and familiarity.
- Kobo — benchmark for reading settings discoverability and layout/page-transition controls.
- Google Play Books — benchmark for simple reading controls, brightness/color modes and adaptive page layouts.
- ReadEra — benchmark for local/offline simplicity and low-friction library use.
- Moon+ Reader — benchmark for advanced customization breadth.

## P0 — required before world-class reader claim

### Reader reliability
- Open / resume must always restore the correct location.
- Tap and swipe navigation must never be blocked by visual effects.
- Page-turn animation is visual-only and can never own authoritative navigation state.
- RTL/LTR physical direction must be correct.
- Scroll mode must bypass page-turn animation cleanly.
- Large EPUB/PDF and malformed-book behavior must fail safely.

### Reading controls
- Font family.
- Font size.
- Line spacing.
- Margins.
- Justification/alignment.
- Publisher-default toggle.
- Background/page theme.
- Brightness control.
- Pagination / continuous scroll.
- Page-turn style.
- Bookmark.
- Highlight / note.
- Table of contents.
- Full-book search.

### Product UX
- Settings is a first-class destination, not hidden under Profile.
- Reading controls are reachable inside the reader in one move.
- Library and Reading Now have intentional empty/loading/error/offline states.
- System / Light / Dark app appearance persists.
- Reader appearance and app-shell appearance are separate concepts.
- No full-screen ad or gamification interruption in the reading flow.

### Visual quality
- Screenshot QA required for every affected screen.
- No feature can be GREEN if visual hierarchy, spacing, typography or icon consistency is visibly poor.
- Brand color is restrained and used mainly for meaningful actions/status.
- Reader content dominates; chrome is quiet and transient.
- One icon language, shape system and spacing system.
- Light and dark modes both receive deliberate art direction.

### Accessibility and globalization
- TalkBack semantics.
- 48dp practical touch targets.
- Large font scale without clipping.
- Contrast discipline.
- Reduced-motion path.
- RTL-safe shell and publication-direction-aware reader.
- Persian/Arabic connected scripts must not use destructive letter spacing or fixed-width UI assumptions.
- Localization-ready strings and layouts.

## P1 — next differentiation layer

- Font weight / bold reading option.
- Word spacing and character spacing.
- OpenDyslexic or equivalent dyslexia-friendly option.
- Line guide / reading ruler.
- Orientation lock.
- One/two-column layout on wide displays.
- Tablet/foldable list-detail and supporting-pane layouts.
- TTS / read-along.
- Custom reader themes.
- Exportable reading presets.
- Per-book override vs global defaults.
- Better cover extraction and metadata editing.

## P2 — Veil differentiation

- Manga/Webtoon mode.
- Castle/progression that never interrupts reading.
- Optional achievements and cosmetics.
- Optional sync.
- Book-grounded AI with spoiler-safe behavior.
- Semantic note/highlight search.
- Cross-book knowledge graph.

## Release matrix

Every release must report:
Open → Resume → Pagination → Zoom → Theme → Settings → Offline → Sync →
Highlight → Note → TOC → Search → Large Book/PDF → RTL → Accessibility →
Adaptive Layout → Visual QA → Performance → Data durability.

Allowed states:
GREEN / YELLOW / RED / BLOCKED / HOLD.

Build success alone cannot set GREEN.

## Performance budgets to establish on physical midrange hardware

- cold start
- library render at 1,000 books
- book open latency
- page-turn input latency
- frame pacing during page turn
- memory during long reading session
- PDF zoom responsiveness
- battery impact in 30-minute reading session

Exact thresholds must be measured and then frozen as release budgets.

## Design review rule

Before implementation:
1. How does Apple Books solve it?
2. How does Kindle solve it?
3. How does Kobo solve it?
4. How does Google Play Books solve it?
5. What do ReadEra/Moon+ users value or complain about?
6. What is the simplest Veil-native solution that is at least as clear and more coherent with our product identity?

Copy behavior patterns, not brand assets or proprietary visual identity.
