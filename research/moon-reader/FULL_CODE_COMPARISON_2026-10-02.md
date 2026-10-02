# Moon+ Reader 10.7 vs Veil Reader — Full Structural Code Comparison

Date: 2026-10-02  
Veil reference branch: `integration/reader-rc-v2`  
Research branch: `research/mimic-moon-reader-2026-10-02`  
Moon reference: user-supplied `Moon+ Reader Pro 10.7` APK  
Moon APK SHA-256: `d676b16ff18c2496ab35d1306fd195e7fba491bc8260cfd368ab973c7bc09d08`

## Executive conclusion

Moon+ Reader is substantially broader and more mature in reader feature depth, especially typography, page-turn variants, PDF annotation, gesture mapping, extra formats, TTS, auto-scroll, cloud/sync and long-tail reading utilities.

Veil Reader has the cleaner modern core architecture: Readium boundary, Room/DataStore, private-file import, process-death recovery, explicit locator durability, input arbitration, isolated paper-curl components, benchmark infrastructure, accessibility-first design rules and offline-first privacy.

**Decision:** do not copy Moon+'s architecture. Keep Veil's architecture and selectively reproduce observable behavior and interaction principles through independent implementation.

The biggest Veil gap is not architecture. It is **reader-depth + runtime polish**.

---

## Methodology and evidence boundary

This is an Android adaptation of the MIMIC workflow:

`OBSERVE → MEASURE → IMPLEMENT → CAPTURE → DIFF → FIX`

Evidence labels:
- **OBSERVED** — directly measured from APK resources/DEX metadata or Veil source.
- **INFERRED** — supported by multiple observed signals.
- **UNVERIFIABLE** — needs live runtime capture.

Important limitation: the Moon APK contains compiled DEX/native binaries, not the developer's original source tree. The environment did not provide JADX/apktool, so this pass used a custom DEX structural parser plus APK resource/native inventory. It scanned the full DEX metadata surface but does **not** claim line-for-line reconstruction of proprietary Java/Kotlin source.

---

# 1. Moon+ structural inventory

## APK / bytecode scale

**OBSERVED**

- 5 DEX files.
- 27,162 classes across the large DEX files.
- 233,692 method IDs.
- 99,056 field IDs.
- 2,283 `com.flyersoft.*` classes.
- 7,257 methods declared by `com.flyersoft.*` classes.
- 9,453 fields declared by `com.flyersoft.*` classes.

Major owned package families include:
- `com.flyersoft.moonreaderp`
- `com.flyersoft.components.cloud`
- `com.flyersoft.components.androidsvg`
- `com.flyersoft.staticlayout`
- `com.flyersoft.views`
- `com.flyersoft.books`
- `com.flyersoft.tools`
- `com.flyersoft.books.chmlib`
- `com.flyersoft.opds`

Large third-party/integrated surfaces include Dropbox, Google APIs, RxJava, Flexmark, Fresco, Apache HTTP, OkHttp, jsoup, Radaee PDF and archive/format libraries.

## Resource scale

**OBSERVED**

- 356 base layouts.
- 95 v22 layouts.
- 458 base drawables.
- 310 hdpi drawables.
- 126 xhdpi drawables.
- 114 xxhdpi drawables.
- 60 animation resources.
- 33 animator resources.
- 33 reader backgrounds.
- 12 bundled reader theme presets.
- Bundled fonts and multilingual hyphenation dictionaries.
- Packaged Android baseline profile assets.

---

# 2. Moon+ architecture findings

## Core reader concentration

**OBSERVED**

Large reader classes include:
- `ActivityTxt`: ~791 methods / 61 fields.
- `A`: ~552 methods / 814 fields.
- `ActivityMain`: ~355 methods / 69 fields.
- `T`: ~244 methods.
- `Sync`: ~97 methods.
- `BookDb`: ~90 methods.
- `PrefVisual`: ~67 methods.
- `Epub`: ~66 methods.
- `NewCurl3D`: ~50 methods.

**INFERRED**

Moon has accumulated a very mature feature set, but parts of the architecture are strongly centralized around god classes and global/static state. This is technical debt Veil should deliberately avoid.

## Page-turn engine

**OBSERVED**

`NewCurl3D` exposes:
- left/right curl states;
- multiple style signals such as Google/iBook-like modes;
- page meshes;
- field-of-view/perspective-related state;
- shadow state;
- dual-page caches;
- animation source/target/duration;
- page-curl update methods.

Moon also exposes page-turn configuration in visual preferences.

**Conclusion**

Moon's curl subsystem is visually deeper than Veil's current 2D snapshot curl. Its implementation architecture should not be copied, but its observable interaction should become a benchmark.

## EPUB/text rendering

**OBSERVED**

Moon's `Epub` subsystem tracks:
- OPF / NCX / EPUB3 TOC;
- CSS caches;
- font maps/files;
- image/audio/video files;
- footnotes;
- chapter splitting and cached chapter text.

`MRTextView` contains custom behavior for:
- RTL;
- ruby text;
- superscript;
- highlights/background spans;
- table zoom affordances;
- custom text geometry;
- pixel auto-scroll.

**Conclusion**

Moon owns much more of the text rendering stack. Veil intentionally delegates standards-heavy rendering to Readium. Veil should test Readium's ruby/footnote/fixed-layout/RTL behavior rather than reimplement an HTML/text engine unless a measured blocker appears.

## Appearance system

**OBSERVED**

Moon theme presets persist a full reading state including:
- background image/color;
- font color/family/size;
- bold/italic/shadow;
- four independent margins;
- line spacing;
- text justification/alignment;
- font spacing;
- fading edge;
- curl color.

**Conclusion**

Veil's `ReaderAppearance` is much smaller. Moon demonstrates the value of a full saved appearance profile rather than treating "theme" as mainly a color choice.

## Input / gesture model

**OBSERVED**

`PrefControl` and reader code expose:
- configurable nine-zone tap grid;
- key mapping;
- hardware media/key behavior;
- shake sensitivity;
- orientation-related controls;
- long-tap actions;
- brightness gestures.

**Conclusion**

Moon's input model is more configurable. Veil's default should stay simpler, but its internal model should become data-driven so an advanced input profile can be added without rewriting reader navigation.

## PDF

**OBSERVED**

Moon integrates a substantial Radaee-based PDF stack and exposes:
- GL/PDF layout modes;
- text extraction/selection state;
- visual bookmarks;
- notes;
- free-text/image annotations;
- annotation creation/finalization;
- CBZ/DJVU bitmap paths;
- page-level operations.

**Conclusion**

This is one of Moon's clearest functional advantages. Veil currently has strong PDF navigation/zoom/bookmarks but not equivalent PDF text annotation tooling.

## Library / metadata

**OBSERVED**

Moon's `BookDb` supports a long-lived SQLite model with books, notes, statistics, shelves/collections, favorites, ratings, series/categories and import/export utilities.

**Conclusion**

Moon has more accumulated library features. Veil's Room model is cleaner and safer to evolve. Add only high-value metadata features, not historical feature bulk.

## Cloud / sync

**OBSERVED**

Moon has a large sync surface covering files, shelves, covers, notes, positions and remote providers.

**Conclusion**

This is breadth, not a current Veil deficiency. Veil's no-account, no-INTERNET-permission, offline-first model remains a deliberate product advantage for the current release.

---

# 3. Veil Reader structural inventory

Current `integration/reader-rc-v2` app production tree:

- 54 Kotlin/Java production source files.
- ~472 KB production source.
- Current Gradle version on this branch: `0.10.0`.
- Kotlin + Compose + Material 3.
- Readium Kotlin Toolkit 3.4.
- Room + DataStore.
- Pdfium adapter + AndroidPdfViewer fallback controls.
- release minification/resource shrinking.
- Baseline Profile + Macrobenchmark infrastructure.

Key reader boundaries:

- `ReaderGateway`
- `ReadiumEngine`
- `ReaderViewModel`
- `ReaderLocatorPolicy`
- `ReaderCloseDurability`
- `ReaderInputArbiter`
- `VeilDirectionalNavigationInputListener`
- `PaperCurlInputListener`
- `PaperCurlState`
- `PaperCurlGeometry`
- `PaperCurlDraw`
- `PdfZoomControls`
- `ReaderNotebook`
- `ReaderSelectionActionMode`
- `ReaderFragmentRestoration`

This is a much more composable reader architecture than Moon's central `ActivityTxt`.

---

# 4. Direct comparison

| Area | Moon+ 10.7 | Veil current | Result |
| --- | --- | --- | --- |
| Core architecture | Mature but highly centralized | Modular boundaries + state holders | **Veil foundation is cleaner** |
| EPUB standards | Broad custom parser/render logic | Readium 3.4 | Veil should keep Readium |
| Page curl | Custom mesh/3D-style subsystem | 2D snapshot + geometry curl | **Moon deeper visually** |
| Page/scroll separation | Mature distinct modes | Explicit `scroll` + `PageTurnStyle` + arbiter | Veil architecture strong |
| Typography | Very deep | font scale, line height, margins, theme | **Moon much deeper** |
| Saved themes | Full parameter bundles | small appearance object | **Moon concept worth adopting** |
| Gestures | nine-zone + configurable actions | deterministic edge/chrome ownership | Moon deeper; Veil simpler |
| RTL navigation | mature | publication-progression-aware | Both architecturally supported |
| EPUB annotation | mature | native Readium selection + highlight/note | Veil clean, narrower |
| PDF annotation | extensive | bookmarks + zoom, no equivalent annotation | **Major Veil gap** |
| PDF zoom | mature | native pinch/double tap + manual fallback | Veil good |
| Full-book search | yes | Readium searchable publications | Veil good |
| Brightness | edge gesture + settings | reader-specific brightness + restore | Moon interaction richer |
| Reading ruler | yes | no | Moon feature advantage |
| Auto-scroll | yes | no | intentionally deferred |
| TTS | extensive | no | intentionally deferred |
| Extra formats | MOBI/FB2/CHM/DJVU/comics etc. | EPUB/PDF production support | Moon breadth advantage |
| Library DB | broad legacy SQLite feature set | Room + migrations + duplicate fingerprints | **Veil data foundation cleaner** |
| Backup/restore | extensive ecosystem | validated transactional local ZIP restore | **Veil reliability design strong** |
| Process death | mature historically, architecture opaque | explicit restore/checkpoint policies + tests | **Veil unusually strong** |
| Cloud sync | extensive | intentionally absent | not current scope |
| Privacy/offline | network/cloud integrations | no INTERNET permission, private files | **Veil product advantage** |
| Performance tooling | packaged baseline profile | Macrobenchmark + budgets + profile generator | Veil tooling strong; physical proof still required |
| Accessibility contract | runtime review still required | explicit 48dp/TalkBack/RTL/reduced-motion rules | Veil policy stronger; needs device sign-off |

---

# 5. Two confirmed Veil defects found during the comparison

## DEFECT A — first page-turn is still rejected by XP/page policy

**OBSERVED**

`ReadingPolicy.PageGate.visit()` sets:

`first = lastSeenAt < 0`

and returns only when:

`!first && unique && dwell >= 8_000`

Therefore the first accepted navigation candidate after gate initialization/pause cannot count.

`GameRepository.recordPageTurn()` directly relies on this gate.

**Impact**

The first real page turn of a session can be dropped from paced-page XP/stat accounting.

**Recommended fix**

Seed/observe the initial locator separately from a real committed page turn, or change the gate contract to distinguish:
- initial location observation;
- committed navigation event;
- dwell qualification.

Do not simply award every first event without proving it is a navigation.

## DEFECT B — page-turn style is missing from explicit backup serialization

**OBSERVED**

`SettingsStore` persists:

`reader_page_turn_style`

and `ReaderAppearance` contains:

`pageTurnStyle: PageTurnStyle`

But `LibraryExport.ReaderAppearance.toJson()` does not serialize `pageTurnStyle`, and `appearanceFromJson()` does not restore it.

**Impact**

A backup/restore round trip can lose the user's `PAPER / SLIDE` choice.

**Recommended fix**

Add a backward-compatible `pageTurnStyle` field to backup JSON and restore with a safe default for older archives. Add a round-trip test.

---

# 6. Important Veil reliability findings

## Close durability — good

Veil's close path now:
1. snapshots the current locator;
2. finalizes the session;
3. waits for `library.flushWrites()`;
4. only then clears the reader route.

This is materially better than a fire-and-forget close.

## Background locator — good

On `ON_PAUSE / ON_STOP / ON_DESTROY`, Veil directly snapshots `currentLocator` as `FINAL_SNAPSHOT` before calling `onPause()`.

This protects the latest position from the normal 500 ms locator debounce.

## Fragment recreation — good

`ReaderFragmentRestoration` explicitly handles Readium fragment restoration timing and recreates the real navigator from durable state instead of serializing live Readium objects.

These are strong production-grade choices and should not be disturbed by feature work.

---

# 7. Page-curl technical comparison

## Moon

- dedicated `NewCurl3D`;
- mesh/perspective signals;
- dual page caches;
- custom shadow and multiple style variants;
- long-lived mature behavior.

## Veil

Current curl:
- captures the live publication view into a reusable `ARGB_8888` bitmap;
- drives a 2D fold edge with Compose geometry;
- renders theme-aware back sheet/shadow;
- previews destination through Readium;
- restores the exact starting locator on cancel;
- only persists/counts after commit.

### Strength

Veil's input/persistence contract is cleaner than Moon's observed monolithic reader flow.

### Risk

`PaperCurlState.begin()` synchronously calls `view.draw(Canvas(bitmap))` for a full reader-sized bitmap at gesture start.

A high-resolution full-screen `ARGB_8888` snapshot can occupy roughly 10–20+ MiB, depending on the viewport. The reusable buffer prevents repeated allocation, but synchronous capture can still create first-frame/gesture-start jank.

**Action:** profile capture duration, main-thread frame cost and memory before replacing the algorithm. If it misses the physical frame budget, introduce a renderer abstraction before adding more effects.

---

# 8. Alpha / Beta / Gamma / Delta review

## Alpha — architecture

**Verdict**

Keep Veil architecture. Do not port Moon classes.

### Preserve
- Readium boundary.
- Room/DataStore ownership.
- private-file import.
- locator durability.
- one input arbiter.
- separate paper/slide/scroll behavior.
- testable curl geometry.
- benchmark gates.

### Refactor next
`ReaderScreen.kt` is already ~50 KB and is becoming Veil's own orchestration hotspot.

Before adding many Moon-inspired controls, extract:
- `ReaderNavigatorHost`
- `ReaderChromeController`
- `ReaderAppearanceSheet`
- `ReaderBookmarkController`
- `ReaderAnnotationController`

Avoid creating a new `ActivityTxt` equivalent.

## Beta — reader engine

Priority:
1. fix confirmed page-turn accounting bug;
2. fix backup round-trip for page-turn style;
3. benchmark current paper curl;
4. expand appearance capability without bypassing Readium;
5. create explicit reader capability model for EPUB vs PDF;
6. improve PDF annotation only behind a clean capability boundary.

Do not replace Readium to chase Moon parity.

## Gamma — UX/product

Moon proves advanced controls are valuable, but exposing all of them at once would damage Veil's product identity.

Use two layers:

**Quick**
- theme;
- type size;
- page vs scroll;
- bookmark;
- contents/search;
- brightness.

**Advanced**
- font family/weight;
- letter/paragraph spacing where Readium reliably supports it;
- hyphenation where supported;
- independent margins if justified;
- tap-zone mapping;
- reading ruler/focus mode;
- page-turn tuning.

Default reader remains calm and one-gesture accessible.

## Delta — reliability/performance/QA

Veil already has:
- Startup Macrobenchmark.
- Reader FrameTiming benchmark.
- Baseline Profile generator.
- performance budgets.
- trend/delta policy.

Physical product budgets currently target:
- baseline-profile cold TTID median <= 600 ms;
- reader frame overrun P95 <= 0 ms;
- P99 <= 8 ms;
- reader CPU frame P95 <= 16.7 ms;
- P99 <= 24 ms.

Next measurement set:
- paper curl tap start;
- paper curl drag start;
- cancel/restore;
- eight consecutive turns;
- large EPUB;
- large PDF;
- RTL EPUB;
- tablet/foldable width;
- memory/GC around full-screen bitmap capture.

---

# 9. Recommended implementation sequence

## P0 — before calling the Reader world-class

1. Fix PageGate first-turn semantics.
2. Fix backup serialization of `pageTurnStyle`.
3. Run/repair Baseline Profile generation + packaging.
4. Establish physical reader frame baseline.
5. Measure current paper-curl snapshot path.
6. Split the largest ReaderScreen orchestration responsibilities before feature expansion.
7. Complete EPUB/PDF capability matrix and surface differences clearly.
8. Harden typography/appearance live preview and restore behavior.

## P1 — high-value Moon-inspired reader depth

1. Full saved appearance profiles.
2. Font family/weight and spacing controls only through safe Readium-supported paths.
3. Data-driven tap-zone/action mapping with a simple default.
4. Reading ruler / focus aid.
5. Dual-page/tablet/foldable reading mode.
6. Better PDF annotation/highlight workflow if the underlying renderer can support it reliably.
7. Richer reading statistics presentation.

## P2 — after the stable core

- TTS.
- auto-scroll.
- extra legacy formats.
- cloud/sync.
- Readwise/network integrations.

Do not allow P2 breadth to delay Reader quality.

---

# 10. Proposed architectural additions

## Page-turn renderer boundary

Introduce a small abstraction around visual page-turn rendering without changing the existing input arbiter:

`ReaderInputArbiter → committed navigation contract → PageTurnRenderer`

Implementations:
- current `PaperCurl2DRenderer`;
- future measured `MeshCurlRenderer` only if needed.

Navigation remains Readium-owned.

## Reader capability model

Add an explicit model such as:

`ReaderCapabilities(selection, annotations, search, typography, zoom, pageTurn, scroll, dualPage, rtl)`

Populate it per opened publication/renderer.

The UI then exposes only capabilities that are actually valid.

## Appearance profile

Evolve the current `ReaderAppearance` into a versioned appearance profile while retaining current fields.

Potential additions only when supported:
- font family;
- font weight;
- letter spacing;
- paragraph spacing;
- hyphenation;
- separate horizontal/vertical margins;
- page-turn visual options.

Never add a control that cannot be applied reliably.

## Input profile

Add:
- named action enum;
- tap-zone map;
- key map;
- simple default profile;
- optional advanced custom profile.

Do not expose nine zones by default.

---

# 11. What not to copy from Moon

- proprietary code or assets;
- `ActivityTxt`-style god class;
- global/static configuration sprawl;
- cloud complexity before core release;
- duplicate renderer architecture;
- custom EPUB parser when Readium already satisfies the requirement;
- dozens of visible settings without hierarchy;
- legacy-format breadth at the expense of EPUB/PDF quality.

---

# Final decision

Moon+ should become a **behavioral reference library** for Veil, not a code donor.

Veil is already ahead in architectural cleanliness, explicit durability, privacy posture, modern Android state handling and measurable performance engineering.

Moon remains ahead in accumulated reader depth.

The route to surpass it is:

`Veil architecture + Moon's mature reader lessons + stricter UX hierarchy + measured runtime quality`

—not feature-for-feature cloning.
