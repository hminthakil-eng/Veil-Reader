# Sanctuary Reading Console — competitor-driven UI/UX correction
Date: 2026-10-11
Status: RESEARCHED / FIRST IMPLEMENTATION IN DRAFT PR #488 / NOT DEVICE-ACCEPTED
Base: `grand-forge/p0-kindle-reader-quality-20261008` at `9c65997d891b2b19ccba19e5729ce82bbdff9cce`

## User problem and source-verified anatomy
Product owner reports the reading settings are ugly, bulky and cluttered. This is consistent with the existing source architecture, but no claim of a visual screenshot verification is made.

Before this PR, `ReaderScreen.kt` places `EpubAppearancePanel` in a full-screen opaque `Dialog` with a full-screen vertical gradient. It renders up to four large theme cards in two rows (each card contains a 64dp sample plus labels), a separate internal fictional `ReaderAppearancePreview`, separate reading-mode and page-turn selectors, descriptive captions, many brass rules, an auto-hide control, a persistent brightness section, a tall back-to-reading button and a scrolling Advanced state. Existing `ReaderAppearancePreview` uses resource sample chapter/body text, not the real `EpubNavigatorFragment` page. The real book is fully occluded in the existing Dialog.

Advanced options include font face/weight, line height, margins, alignment, columns, hyphenation, ligatures, normalization, paragraph spacing and indent, letter and word spacing, type scale, paper aging, dark images and publisher styles. DO NOT delete them: power-user capability is not the problem; its presentation is.

`SettingsStore.AppSettings.readerAppearance` is currently one global appearance object; per-book spread information has its own map. Do not falsely label generic appearance settings as per-book until persistent overrides actually exist.

## Research: confirmed official or directly inspected sources

| Product | Confirmed interaction | What Veil should reuse | What not to copy |
|---|---|---|---|
| Apple Books | Appearance lives in Themes & Settings panel with theme cards and a deeper Customize surface; Curl, Fast Fade, Scroll options. https://support.apple.com/en-gb/guide/iphone/iphc1af7c57/27/ios/27 | Actual book remains visible and presets are immediately legible | Restrictive hidden advanced features |
| Kobo Books (mobile) | Actual page remains visible above reading-settings bottom panel; tone/brightness, font, margins, line spacing, transition, orientation. https://help.kobo.com/hc/en-us/articles/360017865293-Adjust-page-margins-line-spacing-and-more-on-the-Kobo-Books-app-iOS-Android | Calm reading-first partial overlay and short controls | One huge scroll of settings |
| Kindle apps | Aa > Font, Layout, Themes, More tabs; custom themes and reading ruler. https://digprjsurvey.amazon.com/csad/help/node/TABlJ4ot69emTO8jJG | Progressive categorization and reusable presets | Forcing routine tasks to change tabs |
| Moon+ Reader | Very broad visual settings, day/night themes, configurable reader bar and gestures. https://www.moondownload.com/ ; https://play.google.com/store/apps/details?id=com.flyersoft.moonreaderp | Optional expert depth and custom shortcuts | Dense, visually technical form by default |
| ReadEra | Colour themes, orientation, brightness, margins, font family/weight/spacing; official Play listing. https://play.google.com/store/apps/details?id=org.readera.premium | Direct, simple default appearance | Power controls too constrained for Veil goals |
| Readest | Live changes, per-book overrides layered on global defaults and independent publisher stylesheet overrides. https://readest.com/docs/customization | True global vs per-book inheritance with meaningful reset | Exposing every finely grained adjustment on initial sheet |

User feedback (not representative controlled studies): https://www.reddit.com/r/androidapps/comments/1rhqitt/whats_a_good_ebook_reader_app/ likes Moon+ versatility; https://www.reddit.com/r/Onyx_Boox/comments/1uqq1hx/suggest_something_like_readera/ describes KOReader UI complexity vs ReadEra; https://www.reddit.com/r/androidapps/comments/12gi1i4 criticizes old Moon+ visual density. These reactions explain tradeoffs but are not universal population data.

## New hierarchy / 3 presentation depths

### Layer A: one-gesture reading console (default)
- Bottom overlay anchored to the book, not a new screen or navigation destination; upper real book stays visible.
- Heading "Reading appearance" only once; no giant title, redundant descriptions, internal fake page sample, or brass-rule repetition.
- Compact visual theme choices PAPER / SEPIA / DUSK / OLED, real colors and selected border, not oversized cards.
- A single font scale slider with value and accessible semantic label; changes apply on release to avoid paginated reflow per pointer frame.
- One four-choice mode control PAGE / SLIDE / PAPER / SCROLL; Paper disabled when production gate off. Never let selected unavailable Paper pretend it is active.
- Brightness and return-to-reading; dismissed by close/outside tap or Android Back after pending state settled.
- Ordinary use is one tap to open Aa, then one action to change a theme or size. No keyboard or nested technical menus.

### Layer B: expert reading controls (discoverable)
- Keep a clear Quick / Advanced switch in the same console. In future refactor, split Advanced by Typography, Page, Interactions and Material (without losing options).
- Hide options unsupported by fixed-layout/PDF or current script rather than showing pages of disabled controls.
- Preserve current global appearance source until an accepted per-book override data model is implemented. Clearly disclose actual scope.
- Respect RTL paragraph rendering, not merely app locale. Never use Latin letter-spacing defaults for connected Persian/Arabic scripts.

### Layer C: account/application defaults
- Dedicated Settings remains the global location for default reading behavior; it should not duplicate a full technical form at the top.
- Future per-book overrides should use one overlay row "All books / This book", inherit global fields, restore per-book defaults, and version the data format with migration tests. Do not implement cosmetic toggles without storage.
- No user content must be cleared on reset or migration.

## First implementation in #488
- Replaced full-screen Dialog with Material3 `ModalBottomSheet`, max 72% of container height (capped at 620dp), actual book visible behind.
- Added `compactSheet` mode: no redundant faux page sample and smaller typography/spacing.
- Added four compact theme chips, one independent four-mode selector with all current mode identifiers; Paper remains rollout-gated.
- Moved less-frequently-used chrome auto-hide into Advanced, keeping it reachable.
- Pure `readerConsoleVisibleMode` prevents disabled stored PAPER selection from being displayed as active. Added 4 JVM regressions; none affect persisted appearance.
- Preserved all Render/Readium/Room/TTS ownership and Advanced controls. No new feature flags activated.

## Known gaps for follow-up PRs (do not overclaim)
1. Exact-head CI Android, Storage, Performance outcomes and real phone snapshots are mandatory before acceptance.
2. Bottom sheet may need content-specific padding/height on small phones, landscape, tablets, IME, and 200% system font scaling. Check genuine touch targets and focus traps.
3. Ensure scrim and modal do not forward pointers to Readium; verify selecting text just before opening/after dismissal and no surprise page turns.
4. The slider updates local controls during drag but Readium applies only at gesture completion; we must distinguish "instant visual response" from actual `EpubNavigator` reflow. Future throttle/preview should not introduce frame jank.
5. Quick theme swatches need owner-tested icon contrast and Persian label cropping at 200% font.
6. Advanced form is still large and should receive a separate component-level design pass, grouped sections, typography sample and fewer duplicated rules.
7. Per-book overrides do not exist here; Readest style inheritance is a roadmap item with explicit Room/DataStore migration proof.
8. If physical tests find a bottom sheet breaks cursor, reflow or inline footnotes, revert this isolated branch and revise the overlay boundary.

## Acceptance contract
- No Reader line/locator jump or changed progress on open, close, theme/size application or Back.
- Correct mode after each of PAGE, SLIDE, PAPER, SCROLL; no paper-slide impersonation and no dead selection.
- Quick control reachability with one hand and up to 200% fonts, Persian/English RTL + LTR, TalkBack and landscape.
- First usable content visible while the modal settings panel is open, and advanced options are reachable with no overlap or inaccessible lower controls.
- Smooth 60 and 120 Hz on physical Samsung/Pixel. CI compilation is tech-green, not visual/product-green.
- User evaluation against actual Apple Books/Kobo reference screenshots and user design boards, not a generic Material template.

No release/main merge until these are proven.
