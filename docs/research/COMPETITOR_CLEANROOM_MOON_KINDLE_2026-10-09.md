# Veil Reader — Moon+ / Kindle Clean-Room Capability Matrix

Date: 2026-10-09  
Status: R&D source of truth  
Scope: behavioral/architectural analysis only. No proprietary source is copied.

## Clean-room rule

The supplied Moon+ Reader Pro and Amazon Kindle APKs are used only to identify observable capabilities, component boundaries, state-machine concepts, assets/formats, and failure/interaction behavior. Veil implementations must be authored from Veil requirements and public/platform APIs. Class names and strings below are evidence, not code to copy.

## Evidence snapshot

### Moon+ Reader Pro 10.7
- 5 DEX files; ~174k unique DEX strings.
- App-specific class clusters under `com.flyersoft.*` and `com.dozof.*`.
- Native libraries include PDF/DJVU/image pipelines.
- Assets include bundled fonts, 20+ hyphenation dictionaries, themes, FB2 CSS, MOBI helpers.

### Amazon Kindle 8.157.0.100
- 8 DEX files; ~313k unique DEX strings.
- Large reader clusters under `com.amazon.kindle.*` and `com.amazon.kcp.*`.
- Native reader stack includes Kindle reader, dictionary, PDF, font shaping, accessibility and Audible libraries.

## P0 — Page turn / Paper Curl

### Moon+ evidence
Representative components:
- `ActivityTxt$MyCurlGesture`
- `A$AfterFlipCurl`
- `NewCurl3D`
- `NewCurl3D$PageShot`
- `NewCurl3D$Mesh3D`
- `NewCurl3D$Mesh3D$ShadowVertex`
- `NewCurl3D$Render3D`

Observed identifiers include `curl3d`, `flipSpeed`, `showCurl3dCover`, `DELAY_CURL3D_TOUCH_UP`.

Clean-room lesson:
gesture ownership -> capture/page-shot -> geometric mesh/deformation -> shadow -> release/commit.

### Kindle evidence
Representative components:
- `com.amazon.kindle.pagecurl.CurlView`
- `CurledPageStateContainer`
- `CurlStartPosition`
- `CurlTargetPosition`
- `GestureService`
- `SelectionGestureHandler$PageTurnBox`
- page-turn aborted/commit events.

Observed identifiers include `PageCurlOn/Off`, `CURL_START_LEFT/RIGHT`, `mCurlMeshes`, `curledEdgePointA/B`, `hasOnGoingCurls`, `stopAnyPageTurns`.

Clean-room lesson:
gesture arbitration -> explicit curl state -> start/target positions -> deforming renderer -> abort/commit event.

### Veil requirement
Paper Curl is not GREEN until:
1. selecting Curl mounts the Paper runtime;
2. real-device drag visibly deforms the current page;
3. drag cancel restores the exact origin;
4. drag commit navigates exactly one page;
5. boundary drag bounces without locator mutation;
6. no silent fallback to Slide;
7. current and next page pixels are hardware-safe on WebView devices;
8. process death and locator durability remain green.

Current work: PR #439, exact-head verification PR #440.

## P1 — Listening / TTS

### Moon+ evidence
- `BookTtsService`
- `A$TtsLine`
- explicit play/prior/next/stop identifiers
- voice/language hooks.

### Kindle evidence
- Android TTS paths plus Read+Listen/Audible integration.
- voice, listening position, playback state and audio badge/provider clusters.

### Veil target
- one TTS session owner;
- persistent listening position separate from reading locator;
- previous/next passage;
- speed/pitch;
- installed voice selection + preview;
- background playback with explicit opt-in;
- sleep timer;
- robust language fallback;
- no truncated compact HUD copy;
- TalkBack-complete controls.

## P1 — Selection, highlights, notes, bookmarks

### Moon+ evidence
- `PrefEditBookmark`, `PrefEditNote`, `PrefSelectHighlight`
- PDF note support and footnote models.

### Kindle evidence
- `SelectionGestureHandler`
- notebook and note sync managers
- bookmark/highlight reader paths.

### Veil target
- long-press selection that never collides with Paper/Slide;
- fast highlight palette;
- note editor;
- bookmark;
- annotation search/filter/export;
- previous-location stack on nonlinear jumps;
- semantic locator durability;
- PDF and EPUB parity where platform permits.

## P1 — Dictionary, translation, context

### Moon+ evidence
- `DictPackage` integration.

### Kindle evidence
- Dictionary JNI/components
- Word Wise UI/decorations/settings
- X-Ray feature paths
- translation components.

### Veil target
- selection action: Define / Translate / Search / Note;
- provider abstraction rather than hard-coded vendor;
- offline dictionary path first where available;
- optional online translation;
- future Veil Context Cards inspired by the *problem class*, not proprietary data/models.

## P1 — Typography and reading appearance

### Moon+ evidence
- font picker;
- CSS font feature/variation settings;
- bundled fonts;
- hyphenation assets for 20+ languages;
- multiple day/night themes.

### Kindle evidence
- font manager / downloadable font infrastructure;
- font shaping libraries (FreeType/HarfBuzz);
- margin/line settings;
- OpenDyslexic;
- theme manager.

### Veil target
- publisher/default + serif/sans/mono/accessibility fonts;
- size, weight, line height, paragraph spacing, letter spacing, margins, alignment;
- hyphenation policy by language;
- paper/sepia/dusk/OLED;
- live preview;
- per-book override vs global default;
- large-text and RTL layouts as mandatory gates.

## P1 — Search and navigation

Competitor evidence includes:
- in-book search;
- chapter/TOC structures;
- page/position navigation;
- nonlinear navigation history;
- Kindle page-flip/position helpers.

Veil target:
- full-text search with hit context;
- TOC and landmarks;
- go-to percentage/page/position as format permits;
- Previous Location stack;
- mini page preview only when it improves navigation;
- Paper/Slide/Scroll input owners remain exclusive.

## P2 — Library organization

Competitor evidence:
- collections, series, sorting/filtering, large-library code paths;
- Moon+ reader bars/sort components;
- Kindle library metadata/refinement/filter infrastructure.

Veil target:
- retrieval-first Archive;
- author/series/collection/status/favorites;
- multi-select library actions;
- duplicate detection;
- reliable metadata edit;
- large-library performance budget.

## P2 — Sync, backup and catalogs

### Moon+ evidence
- Dropbox/cloud package;
- OPDS identifiers;
- Calibre IP/catalog identifiers.

### Kindle evidence
- Whispersync and metadata/note sync components.

Veil target:
- remain local/offline-first;
- backup/export before cloud;
- optional account sync with conflict-safe locator/highlight/note merge;
- OPDS/Calibre-compatible catalog connector is a strong candidate;
- vendor cloud integrations only behind explicit opt-in.

## P2 — Formats

Moon+ evidence includes EPUB, FB2, MOBI, CHM and native PDF/DJVU paths; comic/archive support is visible in reader classes/libraries. Kindle has native PDF/MOBI/Kindle reader stacks.

Veil rule:
format support is added only with a mature parser/renderer and complete QA. Never advertise a format with partial rendering.

Candidate evaluation order:
1. EPUB/PDF hardening (existing core)
2. CBZ/CBR if reusable mature libraries satisfy offline/security requirements
3. FB2
4. MOBI/AZW interoperability only where lawful and technically supportable
5. DJVU/CHM only if quality and maintenance cost pass the Work Reuse Gate.

## P2 — Accessibility

Moon+ evidence:
- accessibility helper, magnifier, contrast paths.

Kindle evidence:
- TalkBack/accessibility classes;
- magnifying views;
- OpenDyslexic;
- contrast controls.

Veil acceptance:
- 48dp targets;
- TalkBack labels/order;
- font scale 200%;
- RTL;
- high contrast;
- reduced motion;
- gesture alternatives;
- selection and Reader tools operable without hidden gesture knowledge.

## P2 — Reading statistics / motivation

Moon+ evidence:
- day/year/read statistics and goal classes.

Kindle evidence:
- Reading Streaks and Reading Insights models/widgets.

Veil target:
- quiet reading stats in core;
- streaks/goals optional;
- gamification world remains separate from the Sanctuary Reader;
- no manipulative interruption of reading.

## Implementation waves

| Wave | Scope | Entry gate | Exit gate |
|---|---|---|---|
| P0 | Real-device Paper Curl | existing renderer + physical failure evidence | real device commit/cancel/boundary + CI/perf green |
| W1 | TTS / Listening | P0 isolated | background/listening position/voice/large-text/TalkBack green |
| W2 | Selection + annotations + dictionary/translation | input arbitration stable | EPUB/PDF selection durability + export/search green |
| W3 | Typography + hyphenation + appearance | Reader presentation stable | RTL/200%/publisher-style/reflow tests green |
| W4 | Navigation + search | semantic locator stable | TOC/search/jump/previous-location durability green |
| W5 | Library organization | storage schema stable | 1k+ book performance + metadata/series/collections green |
| W6 | Backup/sync/OPDS/Calibre | conflict model specified | offline-first + conflict + failure recovery green |
| W7 | Additional formats | mature reusable renderer selected | per-format corpus + performance + accessibility green |
| W8 | Stats/insights/gamification integration | core reading stable | opt-in, non-intrusive, privacy and UX gates green |

## Explicit rejects

- Copying competitor source, resources, artwork, fonts without compatible licenses, private protocols, or proprietary data.
- Renaming Slide as Curl.
- Silent fallback from a selected user mode.
- User-facing “supported” state before device evidence.
- Adding a feature solely to match a checkbox when it worsens Veil’s calm Reader.

## Current risk register

- RED: Physical-device Paper Curl has not yet demonstrated a visible committed curl.
- YELLOW: PixelCopy hardware snapshot path needs exact-head CI + device verification.
- GREEN: Current UI Sanctuary visual branch had exact-head CI/emulator evidence before the Page Engine physical blocker was discovered.
- HOLD: production merge/promotion of Paper.
