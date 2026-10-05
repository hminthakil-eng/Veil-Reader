# Grand Forge Commercial Reader Code Audit — 2026-10-05

Branch: `grand-forge/arena-app-hardening-v1`
Head reviewed: `403e77162f7ed19f34df0e71dded854d4f5462c9`
Owner: Veil Reader Grand Forge + Arena

## Method and evidence boundary

This is a code-level audit of Veil Reader against successful commercial reader behavior.

Veil is reviewed directly from current production source and tests. Closed-source commercial apps are compared by public/observable behavior and official documentation; this report does **not** claim access to proprietary Kindle, Kobo, Apple Books, ReadEra or Moon+ source code. Earlier Moon+ 10.7 research used behavioral/resource evidence only and explicitly prohibited copying proprietary implementation code or packaged assets.

Benchmark set:
- Kindle
- Kobo Books
- Google Play Books
- Apple Books
- ReadEra / ReadEra Premium
- Moon+ Reader Pro

## Executive verdict

Veil is already unusually strong in Reader architecture, local durability, page-mode ownership, Persian/RTL-aware typography, narrative identity and deep reading-history systems.

It is **not yet commercially complete**. The largest competitive gaps are not visual. They are:
1. cloud/account sync,
2. core format breadth,
3. PDF annotation authoring/enumeration,
4. mature dictionary/translation/knowledge lookup,
5. user-font import,
6. auto-scroll and specialist power-reader controls,
7. cross-device/export annotation workflows,
8. audiobook ecosystem (if product scope expands there).

The current strategy should not replace the Reader architecture. It should fill these product-power gaps around it.

## 1. Import and format pipeline — RED

### Current code
`ReadiumEngine.formatOf()` accepts only:
- EPUB
- PDF

Anything else throws: `Veil Reader currently supports EPUB and PDF files.`

Manga has a separate CBZ-oriented subsystem, but this is not equivalent to broad general-library format support.

### Commercial comparison
ReadEra and Moon+ support substantially broader local libraries, including combinations of MOBI/AZW3/FB2/DjVu/DOCX/RTF/ODT/TXT/CBR/CBZ and archives. Moon+ also advertises OPDS/online-book support.

### Verdict
Veil loses badly on format breadth. This is one of the clearest reasons a power reader could keep Moon+/ReadEra installed even if Veil has a better Reader.

### Required direction
Create an ingestion layer in front of the canonical Reader:
- preserve EPUB/PDF as native Tier A,
- add safe conversion/normalization paths for text/document formats,
- keep comics/manga separate where image-reader semantics differ,
- never weaken Readium ownership to chase format count.

## 2. Reader movement and page material — SOURCE ADVANTAGE / DEVICE UNPROVEN

### Current code
Veil separates:
- Paged vs Scroll layout,
- Paper vs Slide vs None page-turn effect,
- PDF layout ownership,
- canonical GPU material Paper engine,
- multiple material profiles,
- page physics/sensory state,
- explicit input arbitration.

This is architecturally cleaner than conflating layout and animation.

### Commercial comparison
Apple Books exposes Curl/Fast Fade/Scroll. Moon+ exposes multiple page-flip effects and speed customization. These products have shipped runtime proof.

### Verdict
Veil has the more ambitious architecture and potentially the better material experience, but cannot claim market superiority until physical-device acceptance proves first-turn behavior, no destination flash, slow/flick/reverse/corner gestures, 60/90/120 Hz pacing, rotation/context recreation and memory stability.

## 3. Typography and appearance — GREEN/ADVANTAGE, with one important gap

### Current code
`SettingsStore` persists:
- theme,
- font scale,
- line height,
- margins,
- publisher styles,
- font family,
- font weight,
- alignment,
- column mode,
- hyphenation,
- ligatures,
- text normalization,
- paragraph spacing,
- paragraph indent,
- letter spacing,
- word spacing,
- type scale,
- dark-image treatment,
- paper patina,
- brightness.

`DesignSystem.kt` has separate Latin and Persian/Arabic typography treatment and connected-script safeguards.

### Commercial comparison
Kindle, Kobo, Apple Books and Moon+ all offer mature visual customization. Apple Books currently exposes font, bold, line/character/word spacing, margins and justification. ReadEra Premium and Moon+ support user-supplied fonts.

### Verdict
Veil is already at or above the strongest mainstream readers in **control granularity and script-aware design**, but lacks local user-font import/replacement.

### Required direction
Add user font import with:
- license-safe local files only,
- per-book/global mapping,
- publisher-font fallback,
- Arabic/Persian shaping validation,
- malformed-font isolation.

## 4. Persistence and reading durability — GREEN/ADVANTAGE

### Current code
`LocalLibraryRepository`, `ReaderProgressWriteOrder`, durable-close barriers and serialized writes provide:
- ordered progress writes,
- lifecycle durability,
- bookmarks/highlights,
- reading-session snapshots,
- progress recovery,
- locator migration,
- completion-cycle coupling,
- explicit flush barriers.

### Verdict
This is one of Veil's strongest engineering areas and should **not** be replaced for architectural neatness. Commercial readers may offer mature sync, but Veil's local durability model is unusually explicit and testable.

## 5. Sync / multi-device continuity — RED

### Current code
Current production architecture is local-first:
- Room library state,
- DataStore settings,
- local snapshot/restore/export concepts.

There is no first-class account/cloud sync subsystem in the reviewed production tree comparable to Kindle/Kobo/Play Books/ReadEra Premium.

### Commercial comparison
Kindle syncs reading position, notes and highlights across apps/devices. Kobo syncs progress, bookmarks and annotations. ReadEra Premium syncs books, documents, reading progress, bookmarks and quotes through Google Drive. Google Play Books supports uploaded EPUB/PDF across signed-in devices and cross-device annotation workflows.

### Verdict
This is the largest mainstream-product gap.

### Required direction
Design sync as an optional local-first layer:
- stable book/content identity,
- operation log rather than last-write-wins blobs,
- locator-aware conflict resolution,
- annotation tombstones,
- offline queue,
- end-to-end encrypted user data where practical,
- account remains optional.

## 6. Highlights, notes and notebook — GREEN locally / RED cross-device

### Current code
Veil has:
- bookmarks,
- highlights,
- highlight notes,
- in-book search,
- durable annotation persistence,
- Hidden Archive/Notebook presentation,
- revisit/memory concepts.

### Commercial comparison
Kobo, Kindle and Google Play Books provide mature annotation access and sync/export workflows. Google Play Books supports define/translate/search from selection and annotation export to Drive.

### Verdict
Local note architecture is strong and more narratively differentiated than most competitors. Export/share/sync maturity is behind.

### Required direction
Add:
- annotation export (Markdown/HTML/JSON),
- book-specific and all-library views,
- cross-device sync,
- quote provenance,
- stable deep links back to locators.

## 7. PDF — YELLOW

### Current code
Veil has:
- native Pdfium-backed reading,
- paged/continuous layout,
- direct zoom ownership,
- zoom normalization and retry/probe handling,
- locator migration,
- embedded-annotation rendering capability.

Existing audit concludes PDF annotation enumeration/authoring/save are not yet complete.

### Commercial comparison
Moon+ Pro advertises multiple PDF annotation support. Google Play Books now supports bookmarks/highlights/notes on uploaded PDFs (with current product constraints).

### Verdict
Veil is strong as a PDF **reader**, not yet strong enough as a PDF **annotation tool**.

### Required direction
Do not fake PDF annotations in an overlay. Extend through a real PDF-capable persistence/authoring path or clearly keep the feature out until correct.

## 8. TTS and accessibility reading — YELLOW

### Current code
`createReadiumReaderTtsSession()` currently enables TTS only for EPUB with available Readium text content. Veil also has:
- Focus Guide,
- high contrast,
- hardware-key mapping,
- reduced-motion policies,
- semantic controls.

### Commercial comparison
Kindle's assistive reading includes TTS/read-along highlighting and Reading Ruler, and Moon+ advertises TTS including PDF support.

### Verdict
Veil's accessibility architecture is strong, but TTS feature completeness is behind the best commercial implementations, especially PDF and mature background/media controls.

## 9. Dictionary / translation / knowledge lookup — RED/YELLOW

### Current code
`ReaderLookup.kt` delegates selected text through Android `ACTION_PROCESS_TEXT` to installed handlers.

### Commercial comparison
Kobo provides direct definition/Wikipedia lookup. Google Play Books exposes define/translate/search. Kindle adds Word Wise and X-Ray on supported books.

### Verdict
Current Veil lookup is clean and privacy-friendly but too thin for a premium reader.

### Required direction
Build a layered lookup sheet:
1. installed offline dictionary,
2. system handler,
3. optional Wikipedia/web lookup,
4. translation provider,
5. book-local search,
6. future Veil entity memory/X-Ray-like layer.

Keep each provider optional.

## 10. Library and retrieval — GREEN core / YELLOW breadth

### Current code
Veil has:
- Room source of truth,
- collections,
- series,
- favorites/status,
- metadata editing,
- duplicate fingerprinting,
- Persian/Arabic-normalized search,
- reading-state filters,
- reading-history/memory systems.

`LibrarySearch.kt` explicitly normalizes Arabic/Persian ya/kaf, diacritics/format marks, whitespace and digit shapes.

### Commercial comparison
ReadEra remains stronger in automatic device-document discovery and format breadth. Kobo/Apple/Kindle are stronger in store/discovery ecosystems.

### Verdict
Veil's **retrieval semantics for a known local library** are strong; acquisition/discovery breadth is weaker.

## 11. Reading stats and gamification — DIFFERENTIATOR, not yet finished

### Current code
Veil already has:
- reading sessions,
- pace profiles,
- milestones,
- cycles,
- daily goal,
- XP/level/path systems,
- memory/time-capsule/world mutation concepts.

Commercial readers commonly expose simpler reading goals/statistics; Apple Books has Reading Goals, and Kobo now integrates reading activity with StoryGraph.

### Verdict
This is a potential moat. Do not reduce it to generic badges.

Remaining gap:
- yearly/calendar statistics need a clearer product surface,
- progression durability must stay aligned with reading durability,
- no reward may corrupt or distract from reading.

## 12. Persian / RTL — SOURCE ADVANTAGE, DEVICE PROOF STILL REQUIRED

### Current code
Veil includes:
- Persian resources,
- Arabic-script font family,
- RTL/script-aware typography,
- normalized Persian/Arabic library search,
- directional navigation tests,
- localized current Grand Forge debug strings.

### Verdict
This is materially better than treating Persian as translated English UI. It can become a true competitive advantage, but mixed-script titles, 200% font scale, TalkBack and physical-device interaction still require acceptance.

## 13. Power-reader controls still missing

Previously confirmed current gaps that remain materially useful:
- auto-scroll,
- local user-font import,
- optional edge brightness gesture,
- optional edge font-size gesture,
- E-Ink mode,
- multi-touch font scaling for EPUB,
- chapter time remaining,
- chapter page estimates,
- screen-edge touch-disable option,
- richer dictionary routing.

Long-press ownership must not be implemented with a fragile overlay that breaks native selection.

## Commercial parity scorecard

| Area | Current Veil |
|---|---|
| Reader architecture | A |
| Local durability | A |
| Persian/RTL design | A- |
| Typography controls | A |
| Visual identity | A- source / device unproven |
| Library retrieval | B+ |
| EPUB annotations | B+ |
| PDF reading | B+ |
| PDF annotation authoring | D |
| Sync | D |
| Format breadth | D |
| Lookup/translation | C |
| TTS | C+ |
| Accessibility architecture | A- |
| Reading stats/world systems | A- potential |
| Commercial maturity overall | B- |

## Grand Forge priority order

### P0 — close mainstream-product holes
1. optional sync architecture
2. format-ingestion expansion plan
3. PDF annotation decision/implementation path
4. dictionary/translation/knowledge lookup

### P1 — power-reader parity
5. user fonts
6. auto-scroll
7. chapter/book time remaining
8. reading calendar/year statistics
9. annotation export/share
10. TTS background/media/PDF expansion

### P2 — specialist advantage
11. E-Ink mode
12. edge gestures
13. EPUB pinch typography
14. chapter page estimates
15. screen-edge protection

## Final team opinion

Veil is **not a weak reader with pretty UI**. Its core Reader ownership, durability, typography model and narrative-world architecture are already more deliberate than many commercial apps.

But today it would still lose some expert users to Moon+/ReadEra because they can open more files and expose more power-reader utilities, and it would lose ecosystem users to Kindle/Kobo/Play Books because Veil has no mature cross-device sync.

The correct path is therefore:
- **do not redesign the stable Reader core**,
- preserve Grayfog identity,
- finish physical-device proof,
- fill the four commercial holes: **sync, formats, PDF annotation, lookup**,
- then add power-reader controls without turning Settings into a dump.

The target is not feature-count parity. The target is to remove every reason a serious reader needs a second app.
