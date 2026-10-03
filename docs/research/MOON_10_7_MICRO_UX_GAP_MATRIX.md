# Moon+ Reader 10.7 → Veil Reader micro-UX gap matrix

Date: 2026-10-03  
Basis: uploaded `Moon+Reader-Pro-10.7(FarsRoid.com).apk`, uploaded current `app-debug.apk`, and Veil source lineage `alpha/w36-full-harmony-motion-v1` / `alpha/w37-reader-tap-matrix-v1`.

## Method

This is behavioral/product research only. No Moon+ DEX, resources, native libraries, fonts, layouts, or proprietary implementation code are copied into Veil. Candidate features are reimplemented independently against Veil/Readium APIs.

Evidence levels:

- **Confirmed competitor behavior**: directly visible in packaged Moon+ resources.
- **Confirmed Veil gap**: absent from the uploaded Veil APK surface and absent from the W36 source path reviewed.
- **Veil already stronger / different**: existing Veil implementation covers the need with equal or stronger semantics.
- **Needs runtime verification**: static evidence is not enough to judge quality.

## Confirmed gaps

| Area | Moon+ evidence | Veil status | Veil direction |
|---|---|---|---|
| 3×3 tap customization | packaged 9-square tap customization and per-region tap labels | **W37 implemented** | Nine durable zones; semantic previous/next; renderer passthrough; RTL-safe; Paper/Slide preserved |
| Long-press actions | separate left/middle/right long-tap actions plus trigger timing | **Gap confirmed; blocked on current legacy navigator input API** | Preserve native Readium/WebView selection. Do not add a touch-overlay hack; revisit with a renderer/input path that exposes long-press ownership safely. |
| Volume-key actions | volume up/down actions | **W38 implemented** | Activity-owned opt-in mapping; System volume default; page turns preserve Veil motion; scroll/selection/dialog/TalkBack fail back to Android volume. |
| Edge brightness gesture | left-edge swipe changes brightness | Missing | Add opt-in edge gesture with dead-zone, haptic detents and conflict arbitration |
| Edge font-size gesture | right-edge swipe changes text size | Missing | Add opt-in EPUB-only gesture with live preview and accessibility guard |
| Auto-scroll | auto-scroll controls and speed UI | Missing | Reader-owned velocity engine with pause-on-touch, reduced-motion policy and persistent speed |
| Reading ruler | ruler controls and tap behavior | **W39 implemented as Veil Focus Guide** | Off/Focus Window/Reading Line, tunable position/height/dimming, quick Reader toggle, renderer-agnostic visual overlay that never owns touch. |
| Full TTS reader | start/pause/next/prior, filters, notification controls, background guidance | Missing as a reader feature | Android TTS session engine, paragraph queue, media notification, sleep timer, filters |
| Dictionary routing | dictionary selection/custom online routing/history | Missing | Selection action with installed-handler routing; optional user-defined URL template |
| Chapter time remaining | chapter/book minutes remaining strings | Missing | Estimate from personal rolling reading speed; show uncertainty rather than fake precision |
| TXT chapter regex | regular-expression chapter extraction | TXT not a supported core format | Add only after TXT/HTML ingestion contract; user-editable regex presets |
| Chapter page counts | chapter pages / chapter list page-number calculation | Missing | Paginated EPUB derived page estimates cached per appearance profile |
| E-Ink mode | explicit E-Ink screen mode | Missing | Static paged, no translucent animation, high-contrast paper palette, low-refresh invalidation |
| Arbitrary font replacement | book CSS font replacement / font folder workflows | Partial: bundled font families + weight exist | Add local font import and per-book substitution map; preserve publisher fallback |
| Calendar/year statistics | statistics by year/calendar views | Partial archive/progression data, no equivalent surface confirmed | Build reading calendar and annual comparison from durable session rows |
| PDF annotation depth | dedicated PDF annotation list / annotation controls | **W41 capability audited**: embedded annotations rendered through the public Pdfium configuration hook; bookmarks use page labels. Enumeration/authoring/save remain adapter-extension work. | [Exact A/B/C audit](PDF_ANNOTATION_CAPABILITY_AUDIT.md); no fake PDF selection/decorations or annotation UI. Device rendering/RTL/link/zoom verification pending. |
| Screen-edge touch disable | full-screen edge disable option | Missing | Useful for curved screens and gesture-nav devices; per-device override |
| Multi-touch font scaling | pinch/multi-point font size adjustment | Missing | EPUB-only opt-in pinch typography; must not conflict with fixed-layout/PDF zoom |
| Auto-brightness resume | resume system/auto brightness after inactivity | Missing | Treat custom brightness as session-scoped with configurable timeout |
| Direct long-press highlight | long-press/drag directly highlights | Selection exists, direct gesture shortcut absent | Optional selection shortcut while preserving native selection handles |

## Veil already covers or exceeds the observed competitor detail

- Reader motion is explicitly separated into **Paper Curl**, **Slide**, **static paged**, and **continuous scroll**, rather than hiding multiple semantics under one flip setting.
- EPUB typography already includes font family, font weight, alignment, columns, hyphenation, ligatures, text normalization, paragraph spacing/indent, letter/word spacing and type scaling.
- Veil contains script-aware restrictions for RTL and CJK content instead of blindly exposing unsafe typography controls.
- Dark-image treatment, paper patina, high-contrast shell mode and fixed-layout spread ownership are first-class settings.
- Reader session ownership, startup handshake, process recreation, background durability and navigation transaction integrity are much stronger than a typical feature checklist reveals.
- Image interaction, footnote handling, previous-location return, bookmarks, EPUB highlights/notes and local-first backup already exist.
- Sensory settings include haptics, interaction sounds and ambient reading rooms.

## Priority

### P0 — interaction quality
1. W37 tap matrix — **in progress / implemented**
2. Long-press ownership and timing — **research complete / implementation blocked by safe-input constraints**
3. Volume-key mapping — **W38 implemented**
4. Focus Guide / reading ruler — **W39 implemented**
5. PDF annotation capability audit — **completed; W41 public-API reliability slice, runtime matrix pending**

### P1 — reading power
6. TTS engine
7. Auto-scroll
8. Dictionary routing
9. Chapter/book time remaining
10. Reading calendar + yearly statistics

### P2 — specialist modes
11. E-Ink
12. Local font import/replacement
13. Edge brightness/font gestures
14. Multi-touch font scaling
15. TXT ingestion + regex chapter extraction
16. Appearance-dependent chapter page counts

## Quality gates for every competitor-inspired feature

1. Reimplement independently; never transplant decompiled code or packaged assets.
2. Keep Readium/publication state authoritative.
3. Preserve accessibility and touch-exploration ownership.
4. Never create a setting that claims support the renderer cannot actually provide.
5. Persist user choices and survive process recreation.
6. Define interaction conflicts explicitly before adding gestures.
7. Add LTR/RTL tests where spatial navigation is involved.
8. Add malformed-state/fail-calm tests for persistence.
9. Keep feature removal/reversion possible without schema loss.
10. Do not merge a feature merely because Moon+ has it; Veil must have a coherent reason and a better interaction contract.
