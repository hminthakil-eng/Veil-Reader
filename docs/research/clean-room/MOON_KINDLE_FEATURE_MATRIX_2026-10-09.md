# Veil Reader Clean-Room Benchmark — Moon+ Reader Pro 10.7 & Kindle 8.157

Date: 2026-10-09  
Branch: `grand-forge/clean-room-moon-kindle-rd-20261009`

## Scope and clean-room rule

This document records product behavior, architecture signals, public-facing feature semantics, and user-facing interaction patterns observed from the user-supplied Moon+ Reader Pro 10.7 and Amazon Kindle 8.157 APKs.

We do **not** copy proprietary implementation code, private assets, branding, strings, fonts, or native libraries. We use the observations only to define independent Veil Reader requirements and rewrite equivalent behavior with Veil-owned code.

## High-confidence APK evidence

### Moon+ Reader Pro 10.7

The bundled readme/what's-new and DEX/native inventory expose the following categories:

- Real page-turn animation with multiple flip styles, customizable speed/color/transparency.
- Dedicated curl cache lifecycle: next-page / next-chapter curl cache, cache availability, force-disable-curl path, delayed flip handling.
- Paged, smooth scroll, dual-page and RTL document reading.
- 9-zone/custom tap mapping, swipe gestures, hardware keys, volume-key page turns.
- Multiple auto-scroll modes with speed control.
- Left-edge brightness gesture.
- Typography controls: line spacing, font scale, bold/italic, margins, text alignment, hyphenation, font weight/skew, CSS font replacement/blocking.
- Reader themes, AMOLED/E-Ink modes, theme import/export.
- Highlights, annotations, bookmarks, dictionary, translation, share.
- PDF annotation/form/handwriting, annotation list, smart scroll lock, dual-page.
- TTS with optimized segmentation, pause/stop behavior, headset/Bluetooth controls and shake-to-speak.
- Reading ruler/focus tools and word-initial emphasis modes.
- Bookshelf: favorites, authors, tags, grouping, ratings, custom covers, recent books.
- Backup/restore, cloud/FTP sync, shelf sync, position sync.
- Reading statistics/calendar/year-month views.
- OPDS / online libraries and calibre server.
- Foldable/Chromebook adaptation and 16KB-page-size support.

### Kindle 8.157

DEX/native inventory exposes a reader architecture with:

- A dedicated `CurlView`, `CurledPageStateContainer`, explicit curl start/target positions and page model ownership.
- Page-curl on/off/settings state distinct from page-flip and continuous-scroll state.
- Native KRF/WebCore rendering, page models and page views.
- Non-linear page flip/navigation, location seeker/scrubber and breadcrumb state.
- Tap-to-turn and volume-button page turns.
- Continuous scroll as a first-class mode.
- Fonts and accessibility-oriented font choices, including dyslexia/hyperlegibility options.
- Font family, margins, line spacing, alignment, themes and sepia/dark modes.
- Highlights, bookmarks, notes/notebook and annotation sync.
- Dictionary/translation/lookup learning surfaces; X-Ray/Word-Wise-class feature signals are present in the reader stack.
- Strong accessibility architecture: virtual views, TalkBack modes, accessibility page turns and high-contrast/reading-ruler related surfaces.
- Brightness gesture/state, orientation lock and chapter navigation.
- Reading progress, locations, page models and return locations.
- Audible/ebook coordination and background reading/audio infrastructure.

## Veil comparison and implementation targets

| Area | Competitor behavior | Veil current state | Clean-room target | Priority |
|---|---|---|---|---|
| Paper Curl | Cached source/destination page state, dedicated curl state machine | Engine exists but physical QA shows curl not presenting reliably | Hardware-backed warm snapshot + explicit source/destination buffers + no silent fallback | P0 |
| Page mode separation | Curl, page flip, continuous scroll remain distinct | PAGED / PAPER_CURL / SLIDE / SCROLL are distinct | Preserve hard ownership boundaries and state transitions | P0 |
| Snapshot preparation | Moon+ prebuilds curl cache; Kindle owns page model/page view | Software `View.draw` warm capture | PixelCopy warm capture for WebView + software fallback + telemetry | P0 |
| Page destination cache | Next page/chapter prepared before gesture | Source snapshot only | Add adjacent destination preparation after source path is stable | P0 |
| Gesture ownership | Dedicated curl view owns curl gesture | ReaderInputArbiter + Paper listener | Paper must reserve the gesture even on renderer fault; show explicit failure | P0 |
| Tap/volume page turn | Both support non-touch alternatives | Supported in Veil | Verify against all Reader modes and RTL | P0 |
| Reader chrome auto-hide | Reading surface remains content-first | Physical QA found resting access dock conflict | Hide dock when auto-hide is enabled; center tap remains fallback | P0 |
| Continuous scroll | First-class reading mode | Supported | Keep independent settings and locator semantics | P0 |
| Foldable / dual page | Moon+ and Kindle adapt multi-page layouts | Partial/fixed-layout support | Add reflowable two-column/tablet policy independent of curl | P1 |
| RTL page direction | Dedicated RTL page-turn behavior | Reader progression aware | Device QA for curl side/start/target in RTL | P0 |
| Brightness gesture | Edge brightness gestures | Brightness setting exists | Add optional edge gesture with ownership arbitration | P1 |
| 9-zone tap grid | Fully customizable actions | ReaderTapGrid exists | Expand action catalog and in-app visual editor | P1 |
| Hardware controls | Volume/headset/Bluetooth control | Hardware key mapping exists | Add media/headset semantic actions for TTS/page navigation | P1 |
| Auto-scroll | Pixel/line/page variants | Scroll exists, no equivalent multi-mode auto-scroll | Independent AutoScrollController with safe speed control | P2 |
| Typography | Fine controls incl. font weight/skew, hyphenation, CSS font blocking | Core appearance controls exist | Add advanced typography capabilities only where Readium supports them | P1 |
| Accessible fonts | Hyperlegible/dyslexic options | Custom font work in progress | Use properly licensed equivalents sourced independently | P1 |
| Themes | Many themes, AMOLED/E-Ink, import/export | Paper/Sepia/Dusk/OLED | Add E-Ink profile and theme preset import/export | P2 |
| Reading ruler | Multiple focus styles | Focus Guide exists | Expand to ruler presets and sentence/word-leading emphasis | P1 |
| TTS segmentation | Optimized phrase/chunk division | TTS implemented | Improve segmentation, paragraph/chapter continuity and pause policy | P1 |
| TTS hardware | Headset/Bluetooth/shake controls | Partial | Media-session controls first; shake optional/off by default | P2 |
| TTS mini-player | Compact transport | Implemented | Keep concise idle copy and large-text responsive layout | P0 |
| Highlights/notes | Rich annotation surfaces | Implemented | Upgrade color model, export and navigation ergonomics | P1 |
| PDF annotation | Full annotation/form/handwriting | PDF viewing present; annotation depth limited | Separate PDF annotation roadmap using Pdfium-compatible ownership | P2 |
| Dictionary | Offline/online lookup | Limited/roadmap | Dictionary provider abstraction with offline-first path | P1 |
| Translation | Selection translation | Limited/roadmap | Pluggable translation action without blocking selection UX | P1 |
| Entity/X-Ray class features | Character/entity contextual layer | Not present | Veil "Entity Lens": local book entity index + optional AI enrichment | P3 |
| Word-Wise class features | Inline vocabulary assistance | Not present | Veil "Inline Assist": optional difficult-word hints, fully disableable | P3 |
| Location seeker | Rich progress/location scrubber | Progress HUD exists | Add chapter-aware scrubber + Previous Location stack | P1 |
| Chapter nav | Prev/next chapter controls | TOC/navigation exists | Add optional chapter controls and jump preview | P1 |
| Recent books in reader | Quick recent-book access | Home recent list exists | Optional reader switcher sheet, not persistent chrome | P2 |
| Library grouping | Tags/authors/groups/ratings/custom sort | Collections/status exist | Add tags/ratings and natural/manual sort where useful | P2 |
| Backup/restore | Local/cloud backup | Backup surfaces exist | Verify complete round-trip and versioned backup manifest | P1 |
| Position sync | Cross-device positions | Local-first; sync limited | Independent sync protocol with conflict-safe locators | P3 |
| Shelf sync | Cloud books/shelf sync | Not production-ready | Optional account-based library sync, never required | P3 |
| Reading stats/calendar | Detailed calendar/year/month charts | Stats/gamification present | Add factual reading calendar and period summaries | P2 |
| E-Ink | Dedicated display profile | Not dedicated | Add E-Ink preset with reduced animation and high contrast | P2 |
| OPDS/calibre | Remote catalogs | Not core | Optional plugin/provider, not bundled into Reader core | P4 |
| Additional formats | MOBI/AZW3/DJVU/CBZ/etc. | EPUB/PDF core | Add only when engine quality can meet acceptance bar | P4 |
| Security lock | App startup password | Not core | Optional local privacy lock if demand warrants it | P4 |

## P0 implementation wave

1. Fix Paper Curl presentation on real devices.
2. Hardware-backed WebView warm snapshot with fallback and trace evidence.
3. Add destination-page preparation only after source snapshot path is proven.
4. Verify Paper drag/tap/RTL on a physical device.
5. Keep Slide/Paged/Scroll behavior unchanged and independently testable.
6. Keep Reader auto-hide content-first.
7. Re-run Android CI, Storage, Durability and Performance on exact head.
8. Ship a device-test APK before any merge.

## P1 implementation wave

- Reader interaction editor: tap zones, hardware/media actions.
- Brightness edge gesture.
- Advanced typography capability map.
- Focus/ruler presets.
- TTS segmentation and media controls.
- Dictionary + translation provider architecture.
- Annotation export/navigation polish.
- Chapter-aware progress seeker.
- Backup round-trip hardening.
- Foldable/tablet reflowable layout policy.

## P2+ implementation wave

- Auto-scroll controller.
- E-Ink profile and theme import/export.
- Reading calendar/stats expansion.
- PDF annotation program.
- Reader recent-book switcher.
- Library tags/ratings/manual sort.
- Entity Lens / Inline Assist.
- Optional sync/account/catalog providers.

## Non-copying constraints

- Do not ship Moon+/Kindle assets, strings, fonts, native libraries, package identifiers or proprietary implementation.
- Do not use Kindle proprietary typefaces such as Bookerly as Veil assets.
- Any third-party font/library introduced into Veil must have independently verified redistribution rights.
- Competitor behavior may define an acceptance target; Veil code, naming and visual identity remain original.
