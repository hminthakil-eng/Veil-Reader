# Veil Reader 1.0 — Engineering Blueprint

## 1. Product north star

Veil Reader should become a premium, offline-first Android reading app with the polish of a first-party reader and an original, optional mystery-RPG progression layer. The book must remain the center of the experience; the Castle, Paths, quests, analytics and future AI should enrich reading without interrupting it.

### 1.0 promise
A reader can import EPUB/PDF files, trust that their library and annotations are durable, read comfortably for hours, organize a large personal library, search/navigate/annotate quickly, back up and restore their data, and use the app accessibly on a normal Android phone or tablet.

### Non-negotiables
- Reading works fully offline.
- No core reading feature is gated by progression.
- No ads or dark-pattern streak pressure.
- Imported books and annotations stay private by default.
- No feature is called finished until it has automated coverage where practical and passes its release gate.

---

## 2. Current baseline (0.6 candidate)

Already implemented or substantially implemented:
- EPUB/PDF import into app-private storage.
- Readium EPUB/PDF rendering and persisted locators.
- TOC, bookmarks, EPUB highlights/notes, cross-book archive and in-book search for searchable publications.
- Reader themes, font size, line height, pagination/scroll preference and publisher styles.
- Library search/filter/sort, favorites, metadata editing and simple collections.
- Six original Paths, rank rituals, XP/streaks/quests/sigils and anti-farming rules.
- Functional Castle rooms including Treasury and Inner Sanctum.
- ZIP export plus validated in-app restore.
- CI gate for policy tests, unit tests, lint and debug APK assembly.

What 0.6 proves: the concept works and the Android project can build. What it does not yet prove: production durability, lifecycle correctness, scale, accessibility, performance or store readiness.

---

## 3. The biggest engineering gaps

### P0 — Must fix before adding major new product surfaces

1. **Persistence is too lightweight for a real library.**
   Books, bookmarks and annotations still live in SharedPreferences-backed JSON. This is acceptable for a prototype but not for a 1,000-book shelf, relational collections, migrations or reliable partial updates.

2. **Screen state and orchestration are too centralized.**
   The app shell directly creates repositories and owns navigation/open/import/export state. We need screen-level ViewModels and explicit UI state so rotation, process recreation and future deep links do not depend on transient Compose state.

3. **No true instrumentation/device test layer.**
   Unit tests and lint exist, but there is no androidTest suite covering import → open → annotate → close → restart → restore workflows.

4. **Release pipeline stops at a debug APK.**
   We still need a reproducible release build, R8/minification checks, signing through user-controlled secrets, internal-testing distribution and release artifacts.

5. **Physical-device quality is unproven.**
   Reader lifecycle, configuration changes, large PDFs, RTL/mixed scripts, TalkBack, large text, tablets/foldables and backup round trips need real-device acceptance evidence.

### P1 — Required for a world-class 1.0 reader

- Real cover extraction and cached thumbnails.
- Better metadata extraction/editing: language, series, series index, identifiers, publisher, description and dates where available.
- Many-to-many collections/tags instead of one collection string.
- Multi-select library actions: collection, favorite, remove, export metadata.
- Reader typography expansion: font family, margins, paragraph spacing/alignment where Readium supports it.
- Selection actions: highlight color, note, share/copy, define/translate handoff where Android permits.
- Search UX with paging/cancellation/history and clear unsupported-state behavior.
- Reading history/session model and useful analytics derived from real sessions, not only counters.
- Onboarding and empty-state flow.
- Settings center: appearance, reading defaults, progression visibility, privacy/export, accessibility helpers.
- Optional "quiet mode" that hides game UI without deleting progression.
- Import conflict handling and duplicate detection by content fingerprint, not only source path.
- Remove-book flow with explicit choice about annotations/history.
- Robust backup versioning and demonstrated restore round-trip tests.
- Proper adaptive launcher icon, splash treatment and polished app identity.

### P2 — Expansion after 1.0 is stable

- CBZ/CBR manga + webtoon mode, RTL page direction, double-page/spread handling.
- Audiobooks/M4B/MP3, speed, sleep timer, audio bookmarks and chapter navigation.
- Optional TTS for EPUB.
- Optional account/cloud sync with explicit conflict rules and end-to-end privacy design.
- Book-grounded AI: spoiler-safe Q&A, entity/lore index, summaries, semantic note search.
- Social reading circles only after sync/privacy foundations are mature.

---

## 4. Target architecture for 1.0

Do **not** split into many Gradle modules yet. The project is still small enough that premature multi-module work would slow us down. Keep one app module, but enforce clean package/layer boundaries so modularization remains easy later.

### UI layer
- Compose screens are rendering functions.
- One ViewModel/state holder per navigation destination or major workflow.
- Immutable `UiState` + user `Action/Event` pattern.
- `collectAsStateWithLifecycle` for flows.
- `SavedStateHandle` only for transient navigation/UI state that must survive recreation.
- Reader gets a dedicated state holder coordinating the Readium fragment, current locator, controls, sheets and search.

### Domain layer
Use small use cases only where logic is non-trivial or reused:
- ImportPublication
- OpenPublication
- SaveReadingProgress
- CreateAnnotation
- RecordReadingSession
- AdvancePath
- ExportBackup / RestoreBackup

Do not turn every repository method into a one-line use case.

### Data layer

**Room** for relational/durable data:
- `books`
- `collections`
- `book_collection_cross_ref`
- `bookmarks`
- `annotations`
- `reading_sessions`
- `reading_events` only if needed for progression/audit; avoid logging every page forever.
- `achievements` / earned durable milestones if they require history/querying.

**DataStore** for small preferences/state:
- reader appearance defaults
- daily reading goal
- selected Path / current cosmetic choices if they do not need relational history
- progression visibility / quiet mode
- onboarding complete
- privacy/diagnostic preferences

**Filesystem** for imported publications and cached covers/thumbnails.

### Repository boundaries
- LibraryRepository
- AnnotationRepository
- ReadingRepository
- ProgressionRepository
- SettingsRepository
- BackupRepository
- ReaderEngine/PublicationGateway

Repositories expose Flows and suspend functions. UI never reads SharedPreferences, Room DAOs or raw files directly.

### Migration strategy
1. Introduce Room/DataStore beside the existing storage.
2. Build a one-time migration reader for `veil_library_v1` and `veil_game_v1`.
3. Copy/validate existing records into the new stores in a transaction where possible.
4. Mark migration complete only after verification.
5. Keep old data untouched for one release as rollback insurance.
6. Add migration tests using realistic 0.4/0.5/0.6 fixtures.
7. Remove legacy reads only after the migration release is proven.

---

## 5. Data model direction

### Book
Add stable fields for:
- id
- content fingerprint/hash
- title / sortTitle
- authors
- format / media type
- source file path
- cover path
- language
- description
- publisher
- identifiers
- series / seriesIndex
- addedAt / lastOpenedAt
- lastLocator
- totalProgression
- finishedAt
- favorite

### Annotation
Unify highlight and note concepts:
- id, bookId
- locator JSON
- selected text
- note
- color/style
- createdAt / updatedAt

### Collection
Separate collection rows and many-to-many membership. This unlocks Smart Collections later without schema pain.

### ReadingSession
Store session start/end, active reading duration, start/end progression and pages/locations credited. Derive analytics from sessions instead of expanding permanent counter logic.

---

## 6. Execution plan

### Phase 0 — Lock the baseline
**Goal:** create a clean starting point before architectural change.

Steps:
1. Merge the fully green 0.6 release candidate.
2. Tag the baseline.
3. Archive the generated APK and CI evidence.
4. Update README/release docs to match reality.
5. Freeze new feature work until Phase 1 storage/state foundations land.

Exit gate: main is green and reproducible from a clean checkout.

### Phase 1 — Production data foundation
**Goal:** eliminate the largest data-loss/scale risk.

Steps:
1. Add Room + KSP and DataStore.
2. Define entities/DAOs and repository interfaces.
3. Add SharedPreferences → Room/DataStore migration.
4. Move books, bookmarks, annotations and collections.
5. Add reading-session persistence.
6. Move reader/game/settings preferences to DataStore where appropriate.
7. Add migration and backup round-trip tests.
8. Keep storage APIs behind repositories so UI changes are minimal.

Exit gate: existing 0.6 data migrates without loss; 1,000-book synthetic shelf queries remain responsive; restore produces the same logical dataset.

### Phase 2 — State, navigation and lifecycle hardening
**Goal:** make the app survive Android reality.

Steps:
1. Introduce Navigation Compose for top-level destinations and deep-link/open-file flows.
2. Move app-shell state into ViewModels/state holders.
3. Give Reader its own lifecycle-aware state holder.
4. Preserve selected tab/search/filter/sheet state appropriately.
5. Test rotation, dark/light change, font-scale change, background/process recreation and external file-open intents.
6. Add instrumentation tests for import/open/progress/bookmark/highlight/note flows.

Exit gate: no user-visible state/data loss through Activity recreation or process restoration scenarios covered by tests.

### Phase 3 — Library 1.0 polish
**Goal:** make the shelf feel premium and useful at scale.

Steps:
1. Extract/crop/cache covers and thumbnails.
2. Expand publication metadata.
3. Replace single collection string with multi-collection membership.
4. Add multi-select/bulk actions and remove-book workflow.
5. Add duplicate detection using content fingerprint.
6. Add recent/unfinished/finished/favorites and optional smart shelves using database queries.
7. Build polished empty/import/error states.

Exit gate: a large shelf is fast, visually informative and easy to organize without editing one book at a time.

### Phase 4 — Reader 1.0 polish
**Goal:** make reading itself competitive with dedicated reader apps.

Steps:
1. Strengthen in-book search with cancellation/paging and reliable result navigation.
2. Improve typography options supported by Readium.
3. Add annotation color/style and selection actions.
4. Add current chapter/progression/time-left presentation without clutter.
5. Improve PDF behavior and clearly separate unsupported annotation capabilities.
6. Test reflowable EPUB, fixed-layout EPUB, large PDF, RTL, mixed scripts, tables, images, footnotes and corrupt files.
7. Add reader accessibility labels, focus order and large-text layout fixes.

Exit gate: manual reader matrix passes on phone + tablet class devices and no core reading flow depends on gamification.

### Phase 5 — Castle/progression 1.0
**Goal:** make the differentiator meaningful, optional and ethical.

Steps:
1. Add quiet-mode/game-visibility setting.
2. Base rewards on persisted reading sessions and durable events.
3. Make Observatory real analytics from session data.
4. Make Castle visuals evolve from durable milestones rather than only rank index.
5. Add cosmetic inventory/equipment model without monetized XP.
6. Add pause-friendly streak semantics and test time-zone/midnight behavior.
7. Keep all ordinary reading, search, notes and export available regardless of rank.

Exit gate: progression can be hidden entirely while the reader remains fully functional; enabling it adds delight without changing data integrity.

### Phase 6 — Accessibility, performance and release engineering
**Goal:** earn the right to call it 1.0.

Steps:
1. TalkBack, focus, contrast, large-text and reduced-motion audit.
2. Add Macrobenchmark for startup, shelf scrolling, import/open and reader entry.
3. Generate/measure a Baseline Profile for critical journeys.
4. Test 1,000-book / large-annotation synthetic datasets.
5. Add release build with R8/resource shrinking and user-controlled signing secrets.
6. Add versioned release artifacts and internal Play testing workflow.
7. Privacy policy, support path, store copy/screenshots and closed testing.
8. Fix crashes/data-loss/accessibility blockers before feature expansion.

Exit gate: signed candidate survives closed testing and all P0/P1 release gates.

### Phase 7 — Expansion tracks
Only start after 1.0 core is stable. Build as independent vertical slices:
1. Manga/webtoon.
2. Audiobooks/TTS.
3. Optional cloud sync.
4. Optional book-grounded AI.
5. Social reading.

Each track gets its own data/privacy/performance acceptance criteria and must not destabilize the reader core.

---

## 7. Test pyramid and CI

Every pull request should run:
- pure domain/unit tests
- Room migration tests
- backup serializer/restore tests
- Android lint
- debug compile

Nightly or release-candidate jobs should run:
- instrumentation smoke suite on emulator
- import/open/read/annotate/restart workflow
- backup → wipe data → restore round trip
- Macrobenchmark/performance regression checks

Manual release matrix should cover at least:
- small/large Android phone
- tablet/foldable-sized layout
- one lower-memory/midrange device class
- EPUB/PDF fixture matrix
- TalkBack + large font
- light/dark/system theme
- portrait/landscape

---

## 8. Smart scope rules

To keep development efficient:
- Never add two major foundations in the same PR.
- Prefer vertical slices that finish data → logic → UI → tests together.
- No cloud before local data model is stable.
- No AI before annotation/search APIs are stable.
- No social before identity/sync/privacy architecture exists.
- No multi-module rewrite until build times or team scale justify it.
- No cosmetic redesign that blocks data/lifecycle work.
- Keep Readium behind an adapter so upgrades do not leak through the whole app.
- Every new persisted field requires a migration story.
- Every destructive action requires recovery/confirmation semantics.

---

## 9. Immediate next engineering slice

The next implementation should be **Phase 1A: Room + DataStore foundation**, not another visible feature.

First pull request scope:
1. Add Room/KSP/DataStore dependencies.
2. Introduce database entities and DAOs for Book, Annotation, Bookmark, Collection and BookCollectionCrossRef.
3. Introduce settings/progression DataStore models.
4. Add repository interfaces and adapters while keeping existing UI behavior unchanged.
5. Add migration fixtures/tests before switching reads/writes.
6. Do not delete legacy SharedPreferences yet.

Success means the user should see almost no product difference—but the project becomes dramatically safer and cheaper to extend.

---

## 10. Definition of "finished"

Veil Reader 1.0 is finished only when:
- release candidate builds reproducibly and is signed through secure release secrets;
- migrations and backup/restore round trips are proven;
- device lifecycle tests pass;
- accessibility blockers are resolved;
- large-shelf performance is measured rather than assumed;
- EPUB/PDF quality matrix passes;
- ordinary reading works with progression completely hidden;
- closed testers can use the app without data-loss or crash blockers.

After that, we expand. Before that, we protect the core.
