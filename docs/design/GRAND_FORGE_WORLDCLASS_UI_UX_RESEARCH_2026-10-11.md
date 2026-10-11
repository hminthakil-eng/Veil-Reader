# Veil Reader Grand Forge — World-Class UI/UX Research and Acceptance Blueprint
Date: 2026-10-11 UTC
Status: RESEARCH / DESIGN PROPOSAL. Not a visual audit of a current running APK, not product acceptance.
Canonical product integration baseline: grand-forge/p0-kindle-reader-quality-20261008. Do not assume main.
Primary reader priorities: Sanctuary/Reader -> Archive/Library -> Threshold/Home -> Book Detail -> Notebook/Annotations -> Listening/PDF -> Realm polish.

## 0. Evidence legend and constraints

- CONFIRMED PRODUCT SOURCE: PR #488's exact ReaderScreen/ReaderAppearancePolicy diff and docs/design/SANCTUARY_READING_CONSOLE_RESEARCH_2026-10-11.md; PR #487; PR #466; PR #464; PR #468; PR #479; issue #408.
- HISTORICAL SOURCE: design audit dated 2026-09-26 (on BUILD-WORKMODE-GRAYFOG-PERFECTION, older SHA d25a744...), which found architectural implementations for Archive, Observatory, Castle, Echoes and Time Capsules. Never equate this with the current product branch or current device screenshots.
- EXTERNAL OFFICIAL RESEARCH: Apple, Kobo, Kindle/Amazon, Google Play Books, ReadEra, Readest, Android developer guidance, WCAG. Links below.
- COMMUNITY SIGNALS: public Reddit reports and GitHub issues; examples of friction, not prevalence estimates or controlled usability results.
- INFERENCE / DESIGN PROPOSAL: all Veil target experiences, task ordering, visuals, numerical usability targets below; these are acceptance goals, not achieved measurements.
- No real current APK was operated during this research; do not claim visual parity, device performance, accessibility compliance, or pixel-diff success.
- No licensed/proprietary app code/artwork is authorized for reuse merely because APK analysis exists. Match observable interaction contracts via original implementations or compatible licensed OSS.

## 1. Product positioning: premium archival world, silent reading surface

Veil identity consists of three layers:
1. Premium baseline: impeccable legibility, reliability, speed, adaptive layout, offline reading, accessible controls.
2. Grayfog signature: deep blue-black archival shell, restrained antique brass accents, genuinely authored dark fantasy architectural imagery, typography and paper.
3. Narrative depth: Threshold, Sanctuary, Archive, Castle, Ritual and Sanctum have distinct environmental roles. Narrative world serves the reading history, never obstructs opening a book or switching pages.

Primary experience rule: **the book, not the interface, is the hero**. As the user moves from home to reader, visual ornament quiets radically. Avoid pervasive frame ornaments, embossed controls, parallax or sound during sustained reading. Keep the old valuable architectures where present and converge their visual design; no parallel Realm implementations.

## 2. Market study: confirmed patterns and Veil design response

| Product | Documented useful interaction | Research implication for Veil | Caveat |
|---|---|---|---|
| Apple Books | Themes & Settings, themes, Customize, and Curl / Fast Fade / Scroll in one reader context | Quick themes; calm page; distinct transition modes | Do not copy proprietary art/transitions |
| Kindle mobile | Aa -> Font / Layout / Themes / More; saved reading themes; reading ruler; fine spacing | Progressive controls, retrievable presets, disability aids | Nested tabs shouldn't obscure common actions |
| Kobo mobile | Center tap -> font/appearance; tone, margins, line spacing, transition, orientation | Quiet book-first partial overlay, no full-screen takeover | Test actual book visibility during changes |
| Google Play Books | Appearance/lighting, page count/layout, TOC, notes, bookmarks and quick scrubbing | Fast navigation and clear fixed vs reflowable capabilities | Never pretend PDF has EPUB typography controls |
| ReadEra Premium | Aggregated quote/note hub, page thumbnails, personal fonts, library display types | Uncomplicated library, note retrieval and visual navigation | Some functions are premium-only; no assumption all users have them |
| Moon+ Reader | Broad visual/gesture capabilities, font/background personalization | Rich optional expert layer while keeping default calm | Community opinion on UI varies |
| Readest | Live customization, per-book and global overrides, search, notes, split reading, dictionary | Explicit settings scope; eventually one authoritative inheritance model | Features listed in README are not Veil implementation proof |

Primary URLs:
- https://support.apple.com/guide/iphone/read-books-iphc1af7c57/ios
- https://digprjsurvey.amazon.com/csad/help/node/TABlJ4ot69emTO8jJG
- https://help.kobo.com/hc/en-us/articles/360017865293-Adjust-page-margins-line-spacing-and-more-on-the-Kobo-Books-app-iOS-Android
- https://support.google.com/googleplay/answer/9755756?co=GENIE.Platform%3DAndroid&hl=en
- https://relay.readera.org/premium
- https://github.com/readest/readest
- https://www.readest.com/docs/customization

Community / competitor issue signals (examples, not representative counts):
- Users praise Moon+ personalization yet repeatedly ask for simpler/cleaner visual presentation; ReadEra wins for simple reading: https://www.reddit.com/r/androidapps/comments/1tb7r2d/suggest_me_good_ebook_reader_apps_out_there/ and https://www.reddit.com/r/androidapps/comments/12gi1i4
- Users care about reliable progress and local PDF/EPUB resume: https://www.reddit.com/r/androidapps/comments/1utgk9m/book_reader_app/
- Some large-library users report thousands of books causing crashes or clutter; treat as a stress test, not proof of all apps' behavior: https://www.reddit.com/r/androidapps/comments/1rz5yot/any_epub_reader_apps_that_can_handle_1000_files/
- Readest requested per-book settings in reader view to avoid nonvisual global settings: https://github.com/readest/readest/issues/5296
- Readest user complaint shows highlight organization, copy friction and cross-page selection matter: https://github.com/readest/readest/issues/2022
- Readest issue illustrates theme color mapping ambiguity: https://github.com/readest/readest/issues/3910
- Readest bug shows fixed-layout theme overrides can destroy publisher contrast: https://github.com/readest/readest/issues/5649

## 3. Canonical UX task journeys

J1. Cold import -> library -> open in <3 deliberate actions; persistent exact position after force-close.
J2. Home -> Continue Reading -> page is visually stable before any animation; progress/TOC and return-to-previous-location accessible without hunting.
J3. Reader center tap -> one clear toolbar -> Aa -> actual book visible while theme or font changes; cancel/dismiss without phantom position commit.
J4. Text long press -> selection action -> highlight / note / copy / define / search / translate -> exact passage restore on return; failures stay visible and retriable.
J5. Page/Slide/Paper/Scroll choice -> mode visibly and behaviorally distinct; unavailable modes disabled with reason; changing mode never corrupts bookmark.
J6. TTS/Listening -> one tap listen, trusted pause, voice/speed, lockscreen, explicit return to spoken passage; visual reading progress stays separate.
J7. Archive -> search by title/author/series/tag/status -> book + annotations surfaced in <3 actions; supports large libraries.
J8. PDF -> zoom/pan/links/thumbnail navigation work with obvious affordances and without false reflow controls.
J9. RTL Persian book under EN app and EN book under FA app -> page direction determined by publication/content semantics, not simply app locale.
J10. Large-font/200%, TalkBack, Reduced Motion, landscape/tablet/split screen -> no clipping, missing controls, hidden data, focus trap or forced animation.

Numbers in J1/J7 are proposed usability goals, not current measurements.

## 4. Sanctuary: reading console contract

This should extend PR #488, not compete with it:
- The current PR #488 creates height-bounded Material3 ModalBottomSheet (<72% of viewport, <=620dp), compact theme choices and independent Page / Slide / Paper / Scroll control. It retains Readium ownership and leaves the real page visible. Exact-head Android CI, Storage Instrumentation and Performance Benchmarks have completed successfully as observed during this research; device visual approval is STILL PENDING.
- Do not lose advanced typography: font face/weight, line-height, margins, justification, columns, hyphenation, ligatures, normalization, paragraph space/indent, letter/word spacing, type scale, paper aging, image behavior, publisher CSS.
- Three depths: Quick (theme/font size/brightness/mode), Advanced (Typography / Page / Interaction / Material), Defaults (app-wide preferences). Future per-book overrides require real persisted inheritance and reset, not a UI-only switch. PR #488 docs verify global appearance currently uses one AppSettings.readerAppearance object.
- Scope label must say what writes: Global vs This Book, only once the corresponding data model exists.
- The real publication is the preview. Test Readium viewport and exact locator before/after sheet open, live reflow, rotation, close and failed persistence.
- At 200% text and short screens, the bottom controls remain reachable and advanced area scrolls. Neither modal focus nor GPU overlay may steal book interaction after dismissal.
- Theme surfaces must retain correct contrast; do not use brass for dense body text; calm typography-first reading canvas wins over decoration.

## 5. Original Veil visual identity and component rules

- Grayfog shell: archive-depth near-black blue, atmospheric ink, restrained brass/gold functional trim; a few authored illustrations with clear hierarchy and visual purpose.
- Sanctuary: paper/sepia/night/OLED backgrounds may be customizable independently from app shell. Avoid text sitting on texture/ornament, preserve selection and link contrast, keep weather particles outside the reading canvas.
- Decorative density: Home moderate, Library low-moderate, Archive/World rich at meaningful destinations, Reader minimal. Need screenshot review rather than arbitrary percentage labels.
- Typography: separate UI display/editorial family, body-reading family, Persian/Arabic shaping-friendly family. Fantasy/handwritten details are for headings/insignia, NOT paragraphs or dense controls. Do not apply English letterspacing rules to connected Persian/Arabic glyphs.
- Motion grammar: structural transitions (short and predictable), spatial object reveals (optional), paper curl (physically convincing and direct-manipulation), reduced-motion complete substitute. No scene intro between tapping a book and usable content unless explicitly requested.
- Haptics/sound: silent by default; optional and per-category. Never punish accessibility or degrade battery for ceremony.
- Every realm shares component vocabulary, contrast/spacing, back navigation and loading/error conventions; it need not share high ornament density.

## 6. Area-by-area UX blueprint

### Threshold / Home
- Continue Reading is first meaningful element; cover, title, semantic last position and prominent Resume target.
- Recent and collections concise; rare World discoveries below or behind explicit intent, never block the resume path.
- Cold/warm loading skeleton preserves layout and accessible announcements; errors offer Retry + Locate file without losing progress.
- Metric: <=2 taps from opening app to resume last book (target, measured on device).

### Grayfog Library / Book Detail
- Retrieval first: author/series/collection/status, recent/sort, quick search; large-library virtualization/profiling.
- Three views may be useful (Gallery / Shelves / Index), but do not duplicate underlying storage. Simple grid/list first; richer shelf optional.
- Book Detail: clear Read/Resume CTA, metadata and chapters/annotations; atmosphere behind, not above primary action.
- Distinguish file metadata, synthetic literary labels, and user-created notes; never invent book facts.
- Imported cover isn't always rich: render graceful visual placeholder, readable title, long-name ellipsis and RTL support.

### Sanctuary / Reader / PDF
- Distinct Page/Slide/Paper/Scroll, predictable left/right semantics and noninterfering center tap.
- A calm reading chrome appears on request and dismisses reliably; font, contents, search, notes, TTS and back are discoverable.
- Full-screen immersive by default, with optional progress/title/time; status bars restore when expected.
- PDF gets mode-specific panel (zoom/fit/thumbnail/link/annotation when real), no unsupported EPUB knobs.
- Test cover SVG and raster, HTML/image boundaries, chapter boundaries, selection/links, keyboard/volume navigation.
- Paper goal is convincing deformation/shadow/crease with confirmed acquired source and exactly-one release navigation, not proof from shader compilation or GPU epoch alone. PRs #468 and #479 are separate unfinished lanes.

### Notebook / Hidden Archive
- Separate annotations, bookmarks, notes, search and filters; contextual copy/share/export reachable without reopening each book.
- Annotation events persist before success feedback; failed saves preserve user draft.
- Back to Passage is a first-class action from any historical item.
- Distinguish recomputed reading-memory derived data from immutable milestones/capsules.

### Castle / Ritual / Observatory / Sanctum
- Gamification never locks the Reader. Progress is factual, earned, reversible without deceptive history.
- World surfaces use authored hero assets and restrained responsive animation. Avoid endless generic cards.
- Reduced-motion representation of every ceremony; narrative reveal must be skippable.
- Tablet/landscape camera framing adapted; no phone canvas simply stretched to width.

### Listening
- Simple audible playback controls as primary UI; voice/provider setup secondary.
- Preserve spoken location and explicit return to book. Prominent pause must stop sound during loading and after background recovery; keep selected narrator stable.
- Show cloud privacy clearly if an online provider ever becomes real.

## 7. Prioritized implementation graph — existing PR first

| Order | Work packet / owner | Reuse or current dependency | Exit evidence |
|---|---|---|---|
| P0 UX-01 | Finish reading settings sheet; Android + visual design + QA | PR #488. Avoid another settings rewrite | 200% text, FA/EN, landscape/portrait, real book visibility, one-tap controls, no locator drift, TalkBack |
| P0 UX-02 | Physical Paper/Slide/Page/Scroll visual distinction; Reader + GPU + QA | #408, #468, experimental #479, no release flag change | 60/120Hz real-device slow drag/release/cancel video, no Page->Slide fallback, stable persistent position |
| P0 UX-03 | Finish Reader gesture conflict matrix; Reader + accessibility | #467, #474, #487 image zoom | Center tap, selection, link, image double tap, chrome, sheets, PDF gestures each own their events |
| P0 UX-04 | Readability and RTL typography; Android + localization | #464 tests, existing bilingual resources | Persian/Arabic ligatures/diacritics/numbers/RTL selection, EN book under FA UI, font 200%, contrast |
| P0 UX-05 | Real device visual regression evidence; QA | #466 120-frame emulator gate | Matching APK/SHA, real physical captures reviewed against user Grayfog reference; screenshots alone not accessibility acceptance |
| P1 UX-06 | Library retrieval and 1,000/3,000-book scaling; product + performance | Existing Library; do not build a second catalog | Search/sort/filter and resume usability, deterministic fast warm scroll, low-memory stress |
| P1 UX-07 | Annotation/Notebook task simplification; UX + data team | Existing Room/annotation durability fixes | Selection->save->Archive->Return to Passage paths, storage failure UX, export/copy |
| P1 UX-08 | Scope-resolved global and per-book settings | PR #488 source verifies global-only app appearance | Data contract/migration/backup/restore, true inheritance, per-book reset, no cross-book leakage |
| P1 UX-09 | PDF fit, thumbnails, controls and theme correctness | Existing Readium/Pdfium, PDF #415 | Raster/scanned/large docs, no inverted photos, zoom focus and link ownership |
| P1 UX-10 | Listening Mode a11y and reliable audio UX | #480, #482, #485 (draft lanes) | Audible device recordings, pause while loading, speaker identity, Bluetooth/screen off |
| P2 UX-11 | World narrative polish + original art | Existing Castle/Observatory/Archive from historical audit | Authored scenes, aesthetic coherence, reduced-motion, no slowdown of Reader |
| P2 UX-12 | Optional ambient personalization/e-ink modes | User demand + performance evidence | Battery/frame budgets; clear opt-in and per-format support |

These are proposed dependencies and priorities, not automatic merge instructions. Independent teams should review each packet before a PR; never stack UI experiments blindly onto changing Reader heads.

## 8. Measurable acceptance and performance strategy

### Accessibility
- Android/Compose material clickable touch targets >=48dp; distinct focus/selected/disabled state.
- WCAG 2.2 contrast: regular text >=4.5:1, large text >=3:1 in applicable interactive UI; test content/publisher overrides case-by-case.
- Text resize at 200% without clipped labels/buttons or inaccessible terminal controls.
- Screen reader labels/actions/order, focus after sheet dismissal, keyboard navigation, reduced motion/no essential gesture-only action.
- Contrast tests require actual palette token measurements, not just “looks gold on black.”
Sources: https://developer.android.com/develop/ui/compose/accessibility/api-defaults and https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum and https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/

### Devices and real content
- Reference: current Android phone (including Samsung 120Hz), one lower-tier/60Hz Android, tablet landscape and phone portrait; emulator supplemental only.
- Run same SHA/APK under EN UI/EN EPUB, FA UI/FA EPUB, FA UI/EN EPUB, EN UI/FA EPUB, large font, TalkBack and reduced motion.
- Fixture classes: simple novel, large 1,000+ chapter EPUB, image/SVG-cover EPUB, illustrated publication, Persian mixed-direction text, dense tables/MathML, native/scanned/hostile PDF and low-memory recovery.
- User's book assets are private test fixtures; do not commit copyrighted publication text or art.

### Objective usability measurements to START collecting
- Time/tap count from launch to Resume; time from a selected highlight to Return to Passage.
- Time to change theme/font/turn mode; percentage completing without opening global Settings.
- Wrong-tap / dead-tap rate on Page, chrome and image selection.
- No unintended locator movement while adjusting font/mode and no false saved-success banner.
- Scroll/pagination frame times, dropped frames, peak memory and battery per actual hardware; no blanket GREEN from software-emulator jank.
- New UX screenshots require canonical build SHA, locale, scale, device, timestamp, test sequence, expected state and actual state.

Suggested moderated study: 5–8 users per iteration with varied reading proficiency (not statistical prevalence), tasks J1-J10; compare incumbent baseline and newest Veil build on matched content. Observe rather than prime user expectations.

## 9. Safety rails and no-regression rules

1. No visual refactor removing functional advanced settings, Readium ownership, annotations, durability, PDF or TTS.
2. No active Paper selection if the engine is gated/off. Never quietly substitute Slide.
3. No fake live preview. Preview the actual book or visibly label a static specimen.
4. No moving renderer/page or reflowing during uncommitted Paper drag just to look impressive.
5. No new copy of branded art or proprietary Kindle/Moon code. Use licensed OSS with license checks, or original work.
6. No invented weather, book history, semantic lore, completed achievements, or fake reading stats.
7. No merge/release until exact-head automated and real-device acceptance relevant to that change.
8. Maintain an explicit screenshot and capability baseline so new visual work cannot hide regression.

## 10. Immediate next action for Grand Forge

Independent design/reader/QA review of PR #488 on the exact source SHA. Produce 8–12 before/after captures showing reading console states on actual EN and FA books, in 100% and 200% text, light/sepia/dark, phone portrait/landscape and tablet. Verify no locator drift and 48dp controls. Then file only reproducible concrete defects, patch within existing #488 branch or a clean follow-on. In parallel, continue physical Page-Curl gate #408; do not allow an attractive new settings UI to hide the unreleased Paper engine.

Status: evidence-based research finished, device capture still pending, all product GREEN claims prohibited.
