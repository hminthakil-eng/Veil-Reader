# Alpha × Evaluation — Moon+ → Veil Reader Capability Gap Review

Date: 2026-10-02  
Canonical Veil branch: `alpha/w26-arena-canonical-ui-rebuild-v1`  
Canonical PR: #349  
Canonical head reviewed: `45e739ef37a1d16e25d25bdef44b42d2f9710ec2`  
Reference: Moon+ Reader Pro 10.7 user-supplied APK structural audit

## Executive finding

Veil no longer lacks the basic reader features that made Moon+ compelling. The canonical W26–W60 line has already absorbed many of the right lessons in a cleaner architecture:

- advanced Readium-native typography;
- explicit Paged / Scroll separation;
- Paper / Slide / None page-turn modes;
- reflowable column modes;
- fixed-layout spread controls;
- RTL-aware input;
- keyboard parity;
- transactional notes;
- process-death and durable locator handling;
- PDF recovery / zoom;
- adaptive large-text UI;
- reduced-motion behavior;
- paper material/patina treatment;
- benchmark and baseline-profile infrastructure.

The remaining gap is now concentrated in four areas:

1. runtime performance proof;
2. backup/state completeness;
3. a few mature reading utilities;
4. structural decomposition before Reader/Library orchestration grows into Moon-like god classes.

The goal should not be feature-for-feature parity with Moon. It should be to take Moon's mature reader lessons while preserving Veil's stronger architecture, UX hierarchy, privacy, accessibility and product identity.

---

## Current gate status

At the reviewed canonical head:

- Android CI: GREEN.
- Storage Instrumentation: GREEN.
- Performance Benchmarks: RED.
- Startup baseline-profile generation itself completed successfully.
- Smoke startup TTID: PASS at ~1920 ms under the emulator smoke budget of 6000 ms.
- Reader GFX frame P99: PASS at 525 ms under the smoke budget of 600 ms.
- Reader frame count: PASS at 31.5.
- Reader GFX frame P95: FAIL at 425 ms against a 400 ms smoke budget.

This changes the old release picture: the active blocker in this canonical snapshot is not profile generation; it is reader-frame performance evidence.

---

## Capability matrix

| Area | Moon lesson | Canonical Veil now | Gap / decision | Priority |
| --- | --- | --- | --- | --- |
| Reader architecture | Mature features, but centralized god classes | Readium + explicit state/input/durability boundaries | Preserve Veil architecture | KEEP |
| Reading modes | Separate page/scroll behaviors | Paged/Scroll independent from Paper/Slide/None | Already strong | DONE |
| Typography | Deep user control | Font family/weight, alignment, columns, hyphenation, ligatures, normalization, paragraph/letter/word spacing, type scale | Feature depth largely caught up | DONE/POLISH |
| Fixed spread | Dual-page/tablet reading | Fixed-layout SINGLE/DUAL + continuity logic; reflowable columns | Device validation remains | QA |
| Paper feel | Dedicated custom curl subsystem | Paper curl, material/patina, boundary behavior, reduced-motion, adaptive resize handling | Runtime cost still too high | P0 |
| Slide | Distinct page-motion mode | Dedicated slide state/input path | Runtime cost shares full-screen snapshots | P0 |
| Appearance persistence | Complete reading state should survive | DataStore persists advanced fields | Backup does not serialize most advanced fields | **P0 BUG** |
| Appearance presets | Reusable complete theme states | Built-in reader settings/presets | No named user-saved profiles / per-book override | P1 |
| Tap/gesture mapping | Configurable 9-zone/actions | Strong ownership arbitration but mostly fixed actions/zones | Add optional data-driven InputProfile, keep simple default | P1 |
| Brightness gesture | Fast edge access | Reader brightness setting | Edge gestures risk modern system-back conflicts; don't copy blindly | LEARN ONLY |
| Reading ruler/focus | Useful concentration aid | No equivalent subsystem found | Cheap, offline, high-value addition | P1 |
| EPUB annotations | Highlight/note tools | Transactional native EPUB selection + notes | Model still shallow: no style/color/type/tags | P1/P2 |
| PDF annotations | Mature PDF marking/notes | Zoom, navigation, bookmarks; no equivalent text annotation pipeline | Major functional gap | P1 |
| Dictionary/translation | Contextual reading tools | Not found in canonical reader tool surface | Add optional local/external lookup actions | P1/P2 |
| Search | In-book search | Search with bounded result handling | Strong enough | DONE |
| TTS | Mature reading aid | No TTS subsystem found | Valuable accessibility/power-user feature, not core blocker | P2 |
| Auto-scroll | Mature power-reader feature | No auto-scroll subsystem found | Useful niche feature after core performance | P2 |
| Formats | Broad legacy support | EPUB/PDF core + separate Manga/CBZ path | Do not chase legacy formats until core is finished | P2 |
| Library organization | Shelves/tags/ratings/series | Collections, series, author, language, favorite, search, memory layers | Tags/rating are optional; retrieval quality matters more | P2 |
| Reading statistics | Broad stats | Sessions + gamification + path mastery + memory/timeline systems | Veil is conceptually stronger here | VEIL ADVANTAGE |
| Offline/privacy | Moon includes cloud integrations | Local-first, no default cloud dependency | Preserve | VEIL ADVANTAGE |
| Accessibility | Legacy app maturity varies | RTL, large text, touch exploration, reduced motion, semantic states | Continue device validation | VEIL ADVANTAGE |
| Performance engineering | Mature runtime by history | Macrobenchmark + budgets + profile generation | Current smoke P95 failure must be closed | **P0** |

---

## P0 findings

### 1. Backup/restore does not preserve the complete ReaderAppearance

Canonical `ReaderAppearance` currently includes advanced state such as:

- font family;
- font weight;
- text alignment;
- column mode;
- hyphenation;
- ligatures;
- text normalization;
- paragraph spacing;
- paragraph indent;
- letter spacing;
- word spacing;
- type scale;
- dark-image treatment;
- paper patina.

`SettingsStore` persists these values.

However, `LibraryExport.ReaderAppearance.toJson()` currently serializes only:

- theme;
- fontScale;
- lineHeight;
- pageMargins;
- scroll;
- publisherStyles;
- pageTurnStyle;
- screenBrightness.

The matching restore path also reads only those base fields.

This means advanced reader customization can silently disappear across backup/restore.

The same review found no appearance backup representation for fixed-layout spread maps.

**Action:** version and serialize the complete reader settings state, with backward-compatible defaults and a round-trip instrumentation test.

### 2. Page-turn runtime is over the current smoke P95 budget

Current canonical performance run:
- `reader_gfx_frame_p95 = 425 ms`
- budget = `<= 400 ms`

Both Paper and Slide still capture the live publication view into a full-screen `ARGB_8888` bitmap through `view.draw(Canvas(bitmap))` at turn ownership start.

The buffers are now reused/recycled responsibly, which fixes memory hygiene, but synchronous full-screen capture remains a likely source of latency.

**Action:** profile the capture phase separately before adding richer visual effects.

Possible independent Veil solutions:
- pre-warm/capture when the renderer becomes stable;
- asynchronous or staged visual handoff where correctness permits;
- lower-cost capture strategy;
- renderer-specific page cache;
- GPU-backed page-turn renderer abstraction if evidence justifies it.

Do not port Moon's proprietary `NewCurl3D` implementation.

### 3. Structural size warning

At the reviewed canonical head:
- `ReaderScreen.kt` ~222 KB.
- `LibraryScreen.kt` ~165 KB.

Veil has already extracted many helpers, so this is not the same architecture as Moon's `ActivityTxt`, but the orchestration files are becoming large enough to create the same future maintenance risk.

**Action:** split by state ownership, not arbitrary UI fragments:
- ReaderSessionHost;
- NavigatorHost;
- ReaderChrome;
- AppearanceController;
- AnnotationController;
- Jump/PreviousLocationController;
- PageTurnCoordinator;
- ReaderOverlayCoordinator.

For Library:
- query/facet state;
- collection/series grouping;
- artifact presentation;
- import/actions;
- adaptive layout.

---

## P1 features worth learning from Moon

### Saved appearance profiles

Veil has deep appearance state now, but it is still essentially one global state.

Add a versioned profile model:
- built-in profiles;
- user-named profiles;
- optional per-book override;
- reset to global;
- preview before commit.

This is a better use of Moon's theme lesson than adding more one-off sliders.

### Input profiles instead of a visible nine-zone clone

Moon's configurable zones prove the value of action mapping, but a nine-zone grid should not become Veil's default UX.

Introduce an internal, data-driven `ReaderInputProfile`:
- left / center / right taps;
- long tap;
- key actions;
- optional advanced zones;
- conflict awareness with Android back/accessibility gestures.

Default remains simple and calm.

### Reading ruler / focus mode

This is a high-value, low-architecture-cost feature:
- one or three-line focus window;
- adjustable opacity/height;
- reduced-motion safe;
- works without modifying Readium content.

It fits Sanctuary better than many decorative additions.

### PDF text annotation

This is the clearest remaining mature-reader gap.

Target capabilities:
- text selection when renderer supports it;
- highlight;
- note;
- color/style;
- annotation index;
- safe locator restoration;
- export.

Build it behind an explicit `ReaderCapabilities` contract. Never fake an EPUB feature on PDF when the renderer cannot support it.

### Annotation model depth

Current Veil annotation reliability is good, but `Highlight` is still basically quote + locator + note.

Future-safe model should consider:
- style/color;
- type;
- tags;
- created/edited timestamps;
- optional excerpt metadata;
- source format capability.

This enables richer notebook organization without changing the core selection contract.

### Contextual dictionary / translation / lookup

Moon demonstrates that readers value actions directly on a selected passage.

Veil can add a small extensible action layer:
- dictionary;
- translate;
- copy/share;
- external lookup.

Keep offline-first defaults and use Android intents or optional integrations rather than embedding a large network stack into the core reader.

---

## P2 / later

Useful but should not compete with the current P0/P1 work:

- TTS;
- auto-scroll;
- legacy ebook formats;
- cloud provider sync;
- Readwise-style integrations;
- ratings/tags if retrieval research proves demand;
- wider local comic/archive format support.

---

## What Veil should explicitly NOT copy

Moon's feature breadth is useful evidence, but these patterns should remain rejected:

- god-class reader orchestration;
- proprietary assets or code;
- custom EPUB parser/rendering when Readium already owns standards behavior;
- global/static state sprawl;
- dozens of visible controls in the primary reader UI;
- background texture libraries simply for variety;
- cloud/account complexity in the core offline-first release;
- legacy format breadth at the expense of EPUB/PDF quality;
- edge gestures that conflict with modern Android system navigation.

---

## What Veil can learn beyond individual features

### 1. Mature readers are built from edge cases

Moon's value is not merely its setting count. Its APK shows years of accumulated handling for:
- RTL;
- unusual EPUB structure;
- tables/media;
- font/hyphenation behavior;
- page modes;
- PDF behavior;
- device classes;
- input methods.

Veil should create a permanent Reader Torture Corpus and run it through every major release:
- RTL EPUB;
- Arabic/Persian ligatures;
- CJK vertical/ruby;
- footnotes;
- large tables;
- SVG/images;
- fixed-layout EPUB;
- huge chapters;
- malformed publisher CSS;
- tablet/foldable;
- 200% font scale;
- process death during navigation;
- large PDF;
- PDF rotation/zoom.

### 2. Preferences are product data

Reader settings must be treated like durable user data:
- versioned;
- exportable;
- restorable;
- migration-tested;
- renderer-acknowledged.

The current backup gap proves why this matters.

### 3. Page motion is an engine subsystem, not decoration

Paper/Slide require:
- input ownership;
- visual ownership;
- navigation commit rules;
- cancellation;
- boundary handling;
- resize handling;
- reduced motion;
- performance budgets.

Veil has the correctness pieces; now it needs the performance proof.

### 4. Capability-driven UI beats format conditionals

As EPUB/PDF/Manga diverge, introduce an explicit capabilities model instead of continuing to scatter format checks:

`ReaderCapabilities(search, textSelection, annotations, zoom, reflow, spread, pageTurn, tts, dictionary, images)`

The UI should render only truthful tools for the active publication.

### 5. Depth should live behind progressive disclosure

Veil's Quick/Advanced model is the right answer to Moon's enormous settings surface.

Keep:
- Quick = mode, page turn, theme, text size, brightness.
- Advanced = typography/layout/rendering/input.
- Expert/Accessibility only when needed.

---

## Recommended order

1. Fix complete backup/restore of advanced reader state and fixed-layout spread state.
2. Close current page-turn performance smoke failure before adding heavier reader visuals.
3. Break ReaderScreen/LibraryScreen orchestration into ownership-based controllers/hosts.
4. Add named appearance profiles and optional per-book overrides.
5. Add Reading Ruler / Focus Mode.
6. Introduce ReaderCapabilities.
7. Design PDF annotation architecture.
8. Add optional InputProfile customization.
9. Add contextual dictionary/translation actions.
10. Only then evaluate TTS, auto-scroll, more formats and cloud integrations.

---

## PR #359 disposition

PR #359 was created from the older `integration/reader-rc-v2` line.

The canonical W26–W60 branch already contains a significantly richer typography and reader-mode implementation than #359.

Therefore #359 should **not be merged as-is** into the canonical line.

Only non-overlapping findings should be ported deliberately. Its most useful finding remains the backup/restore completeness issue, but the canonical fix must serialize the full modern `ReaderAppearance`, not the smaller subset implemented in #359.

---

## Alpha decision

Use Moon+ as:
- an edge-case catalog;
- a behavioral benchmark;
- a feature-discovery source;
- a maturity reference.

Do not use it as:
- an architecture template;
- a code donor;
- a visual asset source.

The path to a better reader is now:

`Veil reliability + Veil identity + Readium standards + Moon maturity lessons + strict performance evidence`.
