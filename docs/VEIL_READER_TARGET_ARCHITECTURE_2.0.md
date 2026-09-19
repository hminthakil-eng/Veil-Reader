# Veil Reader Target Architecture 2.0

Status: architecture target, not a claim that all modules already exist.
Date: 2026-09-20
Scope: Android reader core, library, settings, manga/webtoon, sync, accessibility, audio/TTS, gamification and future AI.

## 1. Executive decision

Veil Reader will be a native Android, offline-first, modular reading platform.

The product has one non-negotiable center: the user's publication and reading state.
Everything else — visual effects, Castle progression, cloud sync, AI, manga sources and social features — attaches around that center without becoming required for basic reading.

The architecture uses:
- Readium as the publication/rendering toolkit rather than rebuilding EPUB/PDF.
- Room as the durable local source of truth.
- DataStore for lightweight user preferences.
- WorkManager for durable deferred work.
- Compose + Material 3 + Veil Design System for UI.
- Material 3 Adaptive + Navigation 3 for multi-window/foldable/tablet navigation.
- Explicit adapter boundaries for PDF, manga sources, cloud sync and AI.
- Performance and screenshot QA as release gates, not optional polish.

## 2. Architectural invariants

These rules may not be bypassed by feature work.

1. **Local-first** — opening, reading, annotating and organizing imported books works without account or network.
2. **Single source of truth** — durable user state lives in structured local persistence, not Activity/Composable state.
3. **Navigation is authoritative; animation is decorative** — a page-turn effect can never block or own reader navigation.
4. **Reader engine isolation** — UI/domain code talks to Veil reader interfaces, never to Readium/PDF internals directly.
5. **No provider lock-in** — sync, AI and manga source implementations sit behind interfaces.
6. **No big-bang rewrite** — architecture migrates in vertical slices while the app remains buildable.
7. **Visual quality is functional quality** — visibly poor UI cannot pass release.
8. **Accessibility and RTL are architecture concerns** — not a late QA patch.
9. **Performance is measured** — benchmark release builds on representative hardware.
10. **Fun never interrupts reading** — gamification is event-driven and outside the page surface.
11. **Derived data is rebuildable** — search indexes, cached covers and analytics can be regenerated from durable truth.
12. **Secrets never live in source** — account/sync keys use secure platform facilities.

## 3. Current repository assessment

### Existing strengths

The current project already has a strong modern base:
- compile/target API 37.
- AGP 9.4, JDK 17.
- Compose + Material 3.
- stable Material 3 Adaptive 1.3.
- Room 2.8.5.
- DataStore 1.2.1.
- Readium 3.4.0.
- R8-enabled release build.
- Baseline Profile module.
- Room schemas/migration testing support.
- app-private publication copies and SHA-256 deduplication.
- persisted locators, highlights, bookmarks and reading sessions.
- product/design/release-gate documents.

### Current architecture debt

The present codebase is still shaped like a strong prototype becoming a product:
- most code is in one `:app` module.
- `LocalLibraryRepository` owns too many responsibilities and is roughly 600+ lines.
- `GameRepository` still persists important domain state in SharedPreferences.
- app navigation is a custom boolean/state router rather than a scalable back-stack model.
- `MainActivity` directly owns SettingsStore concerns.
- PDF zoom currently reaches underlying viewer implementation details.
- image/cover loading is manually decoded rather than a centralized image pipeline.
- search, sync, download and AI boundaries are not yet first-class modules.
- UI strings and semantics need systematic localization/accessibility enforcement.

The target architecture addresses these without invalidating working reader code.

## 4. System architecture

```
                 ┌──────────────────────────────┐
                 │       Veil UI / Compose      │
                 │ screens · adaptive chrome    │
                 │ design system · semantics    │
                 └──────────────┬───────────────┘
                                │ intents / state
                 ┌──────────────▼───────────────┐
                 │      Feature State Holders   │
                 │ ViewModels + UDF reducers    │
                 └──────────────┬───────────────┘
                                │ use cases
                 ┌──────────────▼───────────────┐
                 │          Domain Layer        │
                 │ models · policies · usecases │
                 │ reader contracts · sync rules│
                 └───────┬──────────┬───────────┘
                         │          │
          ┌──────────────▼───┐  ┌───▼────────────────┐
          │  Data Repos      │  │ Reader Orchestrator │
          │ Room/DataStore   │  │ publication session │
          │ Files/Search     │  │ engine adapters     │
          └───────┬──────────┘  └───┬────────────────┘
                  │                 │
      ┌───────────▼──────────┐  ┌──▼───────────────────────────────┐
      │ Local durable truth  │  │ Engines                         │
      │ Room · files · prefs │  │ Readium EPUB/PDF/Image/TTS/Audio│
      └───────────┬──────────┘  │ Veil Webtoon                    │
                  │             └──────────────────────────────────┘
      ┌───────────▼──────────┐
      │ Outbox / WorkManager │
      │ sync · index · dl    │
      └───────────┬──────────┘
                  │ optional
      ┌───────────▼─────────────────────────────────────┐
      │ Remote adapters                                 │
      │ SyncTransport · MangaSource · AIProvider        │
      └─────────────────────────────────────────────────┘
```

## 5. Target module topology

Do not create every module at once. This is the destination map.

### Application shell
- `:app`
  - Application/Activity entry points.
  - Navigation host.
  - dependency graph bootstrap.
  - no business logic.

### Core
- `:core:model`
  - immutable domain value objects shared across features.
- `:core:designsystem`
  - Veil tokens, icons, typography, reusable accessible components.
- `:core:ui`
  - generic Compose helpers, adaptive primitives, error/loading models.
- `:core:database`
  - Room DB, entities, DAOs, migrations.
- `:core:preferences`
  - DataStore schemas and preference repository.
- `:core:files`
  - publication/cover/download storage and integrity APIs.
- `:core:network`
  - shared HTTP client policies used only by remote-capable features.
- `:core:search`
  - SearchIndex interface and local implementation.
- `:core:sync`
  - outbox, merge rules, SyncTransport interface.
- `:core:testing`
  - fixtures, fake clocks, fake engines, fake repositories.

### Reader
- `:reader:api`
  - publication/session/navigation contracts used by UI.
- `:reader:readium`
  - common Readium integration/opening/locator mapping.
- `:reader:epub`
  - EPUB navigator adapter and preferences.
- `:reader:pdf`
  - PDF navigator adapter; contains all PDFium-specific access.
- `:reader:image`
  - CBZ/Divina/image-sequence adapter.
- `:reader:webtoon`
  - Veil-owned continuous vertical image reader.
- `:reader:tts`
  - Readium TTS integration.
- `:reader:audio`
  - audiobook/audio integration when enabled.
- `:reader:effects`
  - page-turn visual effects only; no navigation ownership.

### Features
- `:feature:reading`
- `:feature:library`
- `:feature:settings`
- `:feature:annotations`
- `:feature:search`
- `:feature:collections`
- `:feature:downloads`
- `:feature:manga`
- `:feature:castle`
- `:feature:path`
- `:feature:profile`
- `:feature:sync`
- `:feature:ai`

### Quality
- `:benchmark`
  - Macrobenchmark and Baseline Profile generation.
- screenshot tests may initially stay as a test source set rather than a separate module.

## 6. Dependency rules

Allowed direction:

```
app
 ├─> feature:*
feature:* ─> domain/core contracts
feature:reading ─> reader:api
reader implementations ─> reader:api + core
data implementations ─> core/domain
remote adapters ─> core network/sync interfaces
```

Forbidden:
- core depending on feature modules.
- domain depending on Android UI classes.
- feature UI importing Readium classes directly.
- settings UI reaching Room DAO directly.
- manga source adapter updating Compose state directly.
- AI provider reading arbitrary app storage.
- page-turn effect calling `goForward` or `goBackward` as its source of truth.

## 7. UI architecture

Use Unidirectional Data Flow for every feature:

```
UiAction -> ViewModel/StateHolder -> UseCase -> Repository
                                      |
                                      v
                                 StateFlow<UiState>
                                      |
                                      v
                                   Compose
```

Each screen exposes:
- one immutable `UiState`.
- explicit user actions.
- one-shot effects only when they cannot be represented as state.
- test tags/semantics where useful.
- no repository construction inside composables.

Navigation should migrate to Navigation 3 after the current acceptance cycle.
Navigation state should be typed destinations rather than booleans such as `showSettings`.

Adaptive shell:
- compact: bottom navigation / single pane.
- medium: navigation rail where useful.
- expanded/foldable: list-detail or supporting-pane layouts.
- reader text width remains bounded; don't stretch prose across a tablet.

## 8. Reader architecture

### 8.1 Contracts

`ReaderSession` is the single feature-facing abstraction.

Conceptual API:

```kotlin
interface ReaderSession {
    val state: StateFlow<ReaderState>
    val capabilities: ReaderCapabilities

    suspend fun go(location: ReadingLocation)
    suspend fun next(): NavigationResult
    suspend fun previous(): NavigationResult

    suspend fun search(query: String): Flow<SearchHit>
    suspend fun addBookmark()
    suspend fun applyPreferences(preferences: ReaderPreferences)

    fun close()
}
```

`ReaderCapabilities` describes what the current format actually supports:
- text search
- selection/highlight
- TTS
- zoom
- pagination
- scroll
- RTL
- image viewer
- audio

UI renders capabilities; it does not guess by file extension.

### 8.2 ReaderEngineFactory

```
Publication profile -> Engine adapter
EPUB                -> EpubReaderEngine
PDF                 -> PdfReaderEngine
CBZ/Divina          -> ImageReaderEngine
Webtoon chapter     -> WebtoonReaderEngine
Audiobook           -> AudioReaderEngine
TTS                 -> TtsSession
```

Readium remains the primary engine for standards it already supports.

### 8.3 Page-turn architecture

Correct model:

1. Gesture reaches the authoritative navigator.
2. Navigator decides whether a move is valid.
3. The destination state is committed.
4. A visual transition renders old-page -> new-page continuity.
5. Animation can be cancelled without corrupting reading state.

`PageTurnEffect`:
- None.
- Slide.
- Fade.
- PaperCurl.

PaperCurl receives immutable snapshots/transition progress.
It never blocks the navigator input path.
Any future GPU renderer must be input-transparent and must pass physical-device QA before activation.

### 8.4 PDF

PDF is a separate capability profile.
Do not force EPUB controls onto PDF.

The PDF adapter owns:
- zoom.
- fit width/page.
- scroll/paginated mode.
- page number.
- PDF-specific gestures.
- future text search if/when engine support allows.

Direct `PDFView` calls from Compose are temporary debt and should move behind `PdfReaderController`.

## 9. Durable data model

Room is the durable SSOT.

Recommended bounded entities:

### Library
- PublicationEntity
- CollectionEntity
- PublicationCollectionCrossRef
- ReadingPositionEntity
- ReadingSessionEntity

### Annotation
- HighlightEntity
- NoteEntity
- BookmarkEntity

Do not force a note to be only a string column on a highlight forever; a note needs its own identity if future sync/history/AI operates on it.

### Reader preferences
Global preferences live in DataStore.
Per-book overrides, if added, live in Room because they are publication state and may sync.

### Progression
- ReaderProfileEntity
- ProgressionEventEntity
- QuestStateEntity
- CosmeticUnlockEntity
- EquippedCosmeticEntity

Progression should become event-derived where practical.
Do not sync mutable aggregate counters as the only truth.

### Manga/downloads
- MangaSeriesEntity
- MangaChapterEntity
- SourceBindingEntity
- DownloadEntity
- ChapterProgressEntity

### Sync
- SyncMutationEntity
- SyncCursorEntity
- Tombstone/DeletedEntity where needed.

## 10. Repository split

Replace the current god-repository gradually.

Target repositories:
- `LibraryRepository`
- `PublicationFileRepository`
- `AnnotationRepository`
- `ReadingProgressRepository`
- `ReaderPreferencesRepository`
- `CollectionRepository`
- `ProgressionRepository`
- `DownloadRepository`
- `SearchRepository`
- `SyncRepository`

A repository exposes domain models, not Room entities.

High-frequency progress writes may still be coalesced, but the coalescer belongs to `ReadingProgressRepository`, not the whole library repository.

## 11. File storage

Database metadata and publication bytes are separate concerns.

App-private structure:

```
files/
  publications/<content-id>.<ext>
  covers/<content-id>.jpg
  manga/<series>/<chapter>/...
  backups/staging/...
  ai-index/          # only if an implementation needs private files
cache/
  images/
  temporary-imports/
```

Rules:
- materialize external imports before opening permanently.
- hash publication content.
- atomic temp-write -> fsync where appropriate -> rename/install.
- derived covers/indexes can be deleted and rebuilt.
- validate canonical paths before deleting files.
- no source adapter writes outside its assigned storage boundary.

## 12. Search architecture

Use two layers.

### Current-publication search
Readium search is preferred for EPUB where supported.
Results are locators and navigate through ReaderSession.

### Library-wide search
`SearchIndex` interface indexes:
- book metadata.
- extracted publication text chunks.
- highlights.
- notes.
- collections.

Candidate implementation:
- AppSearch LocalStorage for high-performance offline full-text search.
- validate Persian/Arabic tokenization before making it the only implementation.
- keep a Room FTS implementation/fallback boundary available.

Indexing is derived background work:
```
Book imported/changed
 -> enqueue IndexPublicationWorker
 -> extract text off-main
 -> chunk + normalize
 -> update search index
```

Deleting a publication removes its index namespace.

Future semantic embeddings live behind the same search boundary and never replace exact lexical search.

## 13. Offline-first sync architecture

Cloud is optional and never blocks local reading.

### Local-first write path

```
User action
 -> Room transaction
 -> append SyncMutation(outbox)
 -> UI immediately observes local result
 -> WorkManager schedules sync
 -> transport uploads mutations
 -> remote changes download
 -> deterministic merge
 -> Room transaction
 -> mark outbox/cursor
```

### SyncTransport

Provider-neutral interface:
```kotlin
interface SyncTransport {
    suspend fun push(batch: List<SyncMutation>): PushResult
    suspend fun pull(cursor: SyncCursor?): PullResult
}
```

This allows future backend choice without rewriting product code.

### Conflict policy

- Reading position: latest legitimate position by entity revision/time, **not maximum percentage**; rereading/backtracking is valid.
- Bookmark/highlight: stable UUID + tombstone.
- Note: note-level revision; start with last-writer-wins and preserve conflict metadata for recovery.
- Collections: membership is a first-class record so add/remove can merge.
- Settings: category-level revision; local-only options may never sync.
- Progression: sync events or deterministic durable milestones, not only counters.
- Publication binary: content-addressed by hash; file sync is optional and separate from metadata sync.

### Account

Core app stays account-free.
If sync ships:
- Credential Manager for sign-in/passkeys/federated credentials.
- secure refresh/session material protected with platform facilities.
- explicit sync toggle and data controls.

## 14. Background work

Adopt WorkManager for durable work that must survive process death:
- sync.
- publication text indexing.
- cover regeneration.
- large backup creation/validation.
- manga chapter downloads.
- cleanup/maintenance.
- optional AI index preparation.

Do not use WorkManager for immediate UI work or reader page turns.

Constraints:
- network required only where required.
- charging/unmetered constraints for expensive optional maintenance when appropriate.
- unique work names per publication/download/sync stream.
- idempotent workers.
- progress observable by UI when users care.

## 15. Manga / Webtoon architecture

Manga Hub is a feature family, not a fork of another app.

### Provider SPI

```kotlin
interface MangaSourceProvider {
    val id: SourceId
    val capabilities: SourceCapabilities
    suspend fun search(query: String, page: PageKey?): Page<MangaSummary>
    suspend fun series(id: RemoteSeriesId): MangaSeries
    suspend fun chapters(series: RemoteSeriesId): List<MangaChapter>
    suspend fun pages(chapter: RemoteChapterId): List<PageResource>
}
```

Each source adapter:
- is isolated.
- declares network domains.
- can be disabled with a feature flag.
- passes compatibility tests.
- does not leak source-specific models into domain/UI.
- is reviewed for licensing/terms/security.

### Manga UI/data
- Paging for large catalog lists.
- Coil 3 for network/local image loading, memory/disk caching and downsampling.
- WorkManager for offline chapter downloads.
- Webtoon reader: Compose LazyColumn/optimized image pipeline with prefetch.
- Page manga: image sequence reader, optionally reuse Readium ImageNavigator for CBZ/Divina.
- progress migration/source replacement lives in domain logic, not source adapter.

## 16. TTS and audio

### EPUB read-aloud
Use Readium `TtsNavigator` first.
It can run independently of the visible EPUB navigator.
Reading position synchronization goes through a shared locator mapping layer.

### Audiobooks
Pilot Readium AudioNavigator first.
Introduce Media3 only for gaps such as:
- system MediaSession.
- lock-screen/background controls.
- richer audiobook playback integration.

Do not maintain two competing playback engines without a documented responsibility split.

## 17. Gamification architecture

The Castle/Path layer listens to validated reading-domain events:

```
ReadingMinuteRecorded
PageTurnCommitted
HighlightCreated
NoteCreated
PublicationFinished
ReadingDayCompleted
```

`ProgressionEngine` consumes these events and updates durable progression state.

Benefits:
- reader UI never calls random XP mutations.
- events can be tested/replayed.
- sync conflict logic is cleaner.
- anti-cheat/pacing rules stay centralized.
- turning off gamification does not alter reading data.

The game layer may observe reading.
Reading must never depend on game availability.

## 18. Design system and UX architecture

Veil Design System owns:
- color roles.
- typography roles.
- spacing scale.
- shape scale.
- icon set.
- motion tiers.
- elevation/surface policy.
- semantic/accessibility conventions.
- adaptive component variants.

Material 3 supplies platform behavior and primitives.
Veil supplies product identity.

Required screen states:
- loading.
- empty.
- content.
- error.
- offline/degraded.
- permission/auth needed where applicable.

Reader chrome:
- quiet.
- transient.
- one-move access to reading settings.
- no permanent gamification overlays.
- no full-screen ads.

Every important composable gets previews for:
- light/dark.
- compact/expanded.
- LTR/RTL.
- normal/large font scale.
- empty/content/error where relevant.

## 19. Localization, Persian and RTL

Localization is structural:
- all user strings move to resources.
- no layout semantics based on left/right unless physically directional.
- publication reading progression comes from publication metadata.
- Persian/Arabic UI does not use destructive letter spacing.
- Persian font rendering and mixed-script metadata are part of acceptance.
- search/index implementation must be validated with Persian text.
- dates/numbers use locale-aware formatting.
- translator-safe messages avoid string concatenation.

## 20. Accessibility

Mandatory:
- TalkBack semantics and action labels.
- practical touch targets >=48dp.
- logical focus traversal.
- large text without clipping.
- contrast review.
- reduced-motion path.
- state not conveyed by color alone.
- screen magnification tolerance.
- keyboard/focus behavior on large-screen/ChromeOS scenarios.

Reader accessibility roadmap:
- dyslexia-friendly font.
- bold/font-weight.
- word/character spacing.
- line guide / reading ruler.
- TTS/read-along.

## 21. Security and privacy

Principles:
- no account required.
- minimum permissions.
- app-private storage by default.
- HTTPS-only remote traffic.
- Network Security Configuration once remote services exist.
- no secret API keys bundled for privileged backend operations.
- Android Keystore for local key material.
- validate imported archives/backup paths against traversal and size bombs.
- sanitize source adapter inputs.
- backups are versioned and verified before replacing live state.
- optional diagnostics require disclosure/consent appropriate to product policy.

Cloud/AI features must declare:
- what leaves the device.
- why.
- retention behavior.
- how to disable/delete it.

## 22. Backup and restore v2

Current backup evolves into a transactional format.

Bundle:
```
manifest.json/proto
database logical export
settings export
optional small derived assets
checksums
formatVersion
appVersion
createdAt
```

Restore:
1. copy to staging.
2. verify type/size/hash/path safety.
3. parse into isolated staging state.
4. validate references/schema.
5. migrate if needed.
6. atomically replace/merge according to restore mode.
7. rollback on failure.
8. only then remove staging.

Never partially mutate live data while parsing an untrusted backup.

## 23. AI architecture

AI is an optional capability layer behind `BookIntelligenceEngine`.

```kotlin
interface BookIntelligenceEngine {
    suspend fun summarize(scope: AllowedBookScope): AiResult<Summary>
    suspend fun ask(scope: AllowedBookScope, query: String): AiResult<Answer>
}
```

`AllowedBookScope` is crucial:
- book ID.
- current reading boundary.
- selected notes/highlights.
- explicit spoiler override flag.

Default:
- no AI dependency in reader startup/open path.
- AI unavailable = reader works normally.
- on-device preferred where capability/language/quality permits.
- cloud use requires explicit consent and data-boundary disclosure.

### On-device candidates
- ML Kit GenAI/Gemini Nano for supported devices/tasks.
- LiteRT-LM for future custom/local models.

Current ML Kit turnkey APIs have device and language limitations, so they are not a universal Persian solution.

### Future Android agent integration
AppFunctions/Android MCP is experimental.
Track it, but do not build core product architecture around it yet.
Potential future functions:
- resume last book.
- open a named book.
- search notes.
- start TTS.
All remain user-authorized and privacy-bounded.

## 24. Dependency injection

As modules split, adopt Hilt for:
- repositories.
- data sources.
- reader engine factories.
- ViewModels.
- Workers.

Hilt should make dependencies explicit, not hide architecture.
Avoid injection directly into low-level pure domain objects.

## 25. Build architecture

Introduce:
- `gradle/libs.versions.toml`.
- `build-logic` convention plugins.
- consistent Android library/Compose/testing configuration.
- dependency verification/locking policy.
- API surface discipline between modules.

Do this during module extraction, not as an unrelated mega-change.

Toolchain policy:
- stable dependencies by default.
- alpha only through documented controlled pilot.
- quarterly standards refresh.
- security/compatibility updates may trigger out-of-cycle review.

## 26. Testing strategy

### Pure unit
- domain rules.
- progression.
- conflict merge logic.
- locator conversions.
- preference transforms.
- search normalization.
- backup validators.

### Persistence integration
- Room DAO tests.
- migrations from every shipped schema.
- transaction rollback.
- process-death durability scenarios.
- backup round-trip.

### Reader integration
- open/resume.
- next/previous.
- scroll/pagination.
- RTL.
- preferences.
- annotations.
- search.
- PDF zoom.
- malformed files.

### Compose UI
- semantics.
- settings persistence.
- navigation.
- empty/error states.
- large font.
- RTL.

### Screenshot/golden
Visual regressions for:
- Library.
- Reading Now.
- Settings.
- Reader chrome/sheets.
- Castle/Path.
- light/dark.
- compact/expanded.
- RTL.
- large text.

Use a stable solution on the current AGP line; pilot official Compose screenshot testing when its stable build integration is ready.

### Performance
Macrobenchmark:
- cold start TTID/TTFD.
- 1,000-book library scroll.
- book open.
- 20 rapid page turns.
- PDF open/zoom.
- full-book search.
- settings/theme switch.

Baseline Profile CUJs mirror real high-value journeys.

### Physical device acceptance
At least:
- midrange Android.
- modern flagship/high-refresh.
- tablet or foldable.
- RTL/Persian content set.
- large EPUB.
- large PDF.
- image-heavy publication.

## 27. Performance architecture

Hard fact:
- a 60Hz frame has ~16.7ms total budget; 90Hz ~11ms; 120Hz ~8ms.

Practices:
- no heavy IO/parsing on main thread.
- use lazy layouts with stable keys.
- defer rapidly changing Compose state reads to layout/draw when possible.
- avoid full-screen bitmap churn.
- central image loader with downsampling/caching.
- bound caches by memory class.
- measure release builds, not debug.
- R8 and resource shrinking enabled.
- Baseline Profiles generated per release.
- performance regression thresholds stored in CI once measured.

Initial product targets are hypotheses until physical measurement; do not fake precision before data.

## 28. Observability

Production should eventually have:
- Play Console Android Vitals.
- crash/ANR reporting.
- release/version correlation.
- optional performance traces for critical flows.
- privacy-safe diagnostics.

Implement `DiagnosticsSink` abstraction:
- NoOp default in development/privacy-sensitive builds.
- provider implementation only after product/privacy decision.

Do not let a crash SDK become a domain dependency.

## 29. CI quality pipeline

Recommended order:

```
format/static checks
 -> unit tests
 -> Room schema/migration checks
 -> lint
 -> compile
 -> screenshot regression
 -> assemble release/debug
 -> instrumented smoke tests
 -> benchmark/performance lane
 -> artifact
```

PR gates:
- no new dependency without license/security reason.
- no UI feature without relevant visual QA.
- no Room schema change without migration.
- no reader engine change without Open/Resume/Pagination/RTL tests.
- no remote feature without offline/degraded behavior.

## 30. Migration roadmap

### Phase A — finish P7 acceptance
Do not destabilize the current branch with large architecture moves.
- verify reliable navigation.
- verify settings/visual rebuild.
- physical QA.
- keep merge approval explicit.

### Phase B — structural foundation
1. introduce version catalog/build logic.
2. extract `:core:model`, `:core:designsystem`, `:reader:api`.
3. introduce Hilt.
4. move manual route model toward Navigation 3.
5. preserve behavior with tests.

### Phase C — data split
1. split LocalLibraryRepository by responsibility.
2. move GameRepository from SharedPreferences to Room/event state.
3. create file repository.
4. create background WorkManager infrastructure.
5. backup v2.

### Phase D — reader parity
1. font families.
2. alignment/justification.
3. brightness.
4. TOC.
5. search.
6. accessibility controls.
7. isolate PDF controller.
8. TTS.

### Phase E — adaptive/global quality
1. list-detail Library.
2. tablet/foldable reader/supporting panes.
3. screenshot regression suite.
4. performance budgets.
5. full Persian/RTL matrix.

### Phase F — Manga Hub vertical slice
1. MangaSourceProvider.
2. one reviewed source adapter.
3. series/chapter domain.
4. page + webtoon reader.
5. offline download.
6. progress resume/migration.
7. compatibility regression.

### Phase G — optional account/sync
1. outbox schema.
2. deterministic merge tests.
3. Credential Manager.
4. one SyncTransport backend.
5. metadata sync.
6. optional publication file sync.

### Phase H — AI/audio expansion
Only after reader core quality is stable.
- TTS/audio polish.
- on-device AI pilot.
- spoiler-safe book intelligence.
- optional cloud intelligence.
- Android AppFunctions research.

## 31. Definition of world-class for Veil

Veil is not world-class because it has many features.

It is world-class when:
- opening and reading are boringly reliable.
- every important action is obvious.
- typography and motion feel intentional.
- no screen looks prototype-like.
- accessibility and Persian/RTL work as first-class paths.
- data survives process death, updates and restore.
- large libraries remain fast.
- optional fun makes users want to return without manipulating them.
- cloud/AI can disappear and the reader still works.
- architecture can add manga, audio and sync without rewriting EPUB/PDF.
- evidence — tests, screenshots, benchmarks and device QA — supports every GREEN status.
