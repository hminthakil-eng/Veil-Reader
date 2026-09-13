# Veil Reader 0.12 Preview — A Reader That Feels Like You

Veil Reader is a premium, offline-first Android EPUB/PDF reader with an optional mystery-RPG progression layer. Reading remains the primary experience; Paths, rituals and the Castle grow from real reading activity without sitting on top of the page.

**Status: release candidate / active development.** GitHub CI compiles the Android app, runs policy and unit tests, verifies the committed Room schema, runs Android lint, assembles a debug APK, and uploads verification artifacts. Storage-sensitive changes also run Room/migration/backup tests on an Android emulator. Physical-device QA, accessibility sign-off and Play signing are still required before calling the app production-ready.

## Current capabilities

### Reader
- Real EPUB and PDF import, parsing and navigation through Readium Kotlin Toolkit 3.3.0.
- Durable reading position and process/configuration restoration without serializing live Readium objects.
- Table of contents, in-book search, EPUB highlights and notes, EPUB/PDF bookmarks, and a cross-book Hidden Archive.
- Paper, sepia, dusk and OLED reading themes; font size, line height, page margins, scroll mode and publisher-style controls for EPUB.
- Animated page navigation plus compact reader chrome.
- Lifecycle-aware engaged-reading sessions; background/idle time is excluded from reading rewards.

### Grand Library
- Room-backed library with real EPUB/PDF cover extraction and app-private thumbnail caching.
- Search/filter/sort by reading state, favorites, author, title, series and collections.
- Rich metadata: multiple collections, series name/index and language tag.
- SHA-256 duplicate detection: importing the same publication reuses the existing library record instead of keeping a second private copy.
- Favorites and metadata editing.

### Data durability
- Room is the structured source of truth; DataStore owns reader preferences.
- Automatic migration from the earlier SharedPreferences library format.
- User-controlled ZIP backup and transactional restore for books, annotations, bookmarks, reader settings, game state and reading sessions.
- Older schema-1 and schema-2 backups remain readable.
- Publication cover thumbnails and content fingerprints are regenerable cache metadata and are not trusted across restore.
- Android implicit app backup is disabled so imported books/notes are not silently copied outside Veil Reader's explicit backup flow.

### Setup and comfort (0.12 preview)
- Optional welcome guide with quiet reading or a reading world; existing populated libraries skip setup.
- Settings center with General, Reading, and Backups & data sections.
- Quiet mode hides world screens while preserving the castle and engaged-reading progress.
- Independent app light/dark/system themes and a live EPUB typography preview.
- Shared reduced-motion behavior, PDF comfort controls, and navigation that adapts to large text and landscape.
- App preferences included in ZIP backups; older archives retain current app preferences.
- Native Compose interaction, activity-recreation and 200% text checks accompany the storage suite.

### A home between pages (0.11 preview)
- A permanent shack → cottage → lodge → manor → keep → castle → citadel construction campaign.
- Lorestones earned from existing engaged-reading minutes, with one-time story rewards.
- Ten authored quests, five residents with contextual dialogue, and an unlockable original lore journal.
- A native illustrated estate with saved name, three material palettes, three ground treatments and three skies.
- No missed-day penalties, construction countdowns, paid currency, or game overlays on the book.
- Explicit EPUB Slide / 3D curl (beta) / Instant / Scroll choices, saved typeface and alignment, keep-awake and reduced page motion.
- 3D curl is a bounded snapshot effect over Readium’s live page. Physical-device rendering, RTL gestures, chapter boundaries and frame pacing remain acceptance gates; Slide is the default.

### Progression
- Six original Reading Paths: Oracle, Dreamwalker, Archivist, Vanguard, Nocturne and Artificer.
- XP, streaks, daily quests, achievements/sigils, rank rituals and Castle progression.
- Castle rooms: Grand Library, Ritual Chamber, Observatory, Hidden Archive, Treasury and Inner Sanctum.
- Rank advancement requires the Path ritual; XP alone cannot advance rank.

## Build and verify

Requirements: JDK 17, Gradle 9.6 and Android SDK 37.

```sh
sh tools/test-policy.sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

For storage/migration changes, CI additionally runs:

```sh
gradle :app:connectedDebugAndroidTest
```

The repository workflows upload a debug APK and verification reports after successful runs.

## Try the preview on Android

The CI debug APK installs as **Veil Reader Preview** alongside the existing app. Download `veil-reader-debug-apk` from this branch’s successful Android workflow, extract the ZIP and open `app-debug.apk` on the phone.

To bring over your books and progress, export a library backup from the existing Veil Reader, then restore that ZIP from Profile → Settings & reading comfort → Backups & data in Veil Reader Preview. Its saved library is separate. Export a backup before replacing or reinstalling a preview; CI debug signing keys may change between builds. Production signing remains a release gate; this is a test build.

## Backup privacy

Profile → Export library backup writes a local ZIP containing imported publications and reading data. The ZIP is **not encrypted**; store it somewhere you trust. Restore validates archive paths and sizes before replacing current state and rolls back if the replacement cannot be committed.

## Engineering documents

- `docs/PRODUCT_PLAN.md` — current product priorities, campaign economy, reader modes, full feature roadmap and measurable acceptance gates.
- `docs/RELEASE_0.11.md` — current implementation scope and verification status.
- `ROADMAP_0.9_TO_1.0.md` — earlier sequence from the 0.8 baseline; preserved as project history.
- `ENGINEERING_BLUEPRINT_1.0.md` — architecture principles and earlier 1.0 foundation plan; some baseline sections predate the Room/DataStore work completed by 0.8.
- `MANUAL_QA.md` — physical-device verification checklist.
- `WORLD_CLASS_RELEASE_GATES.md` — requirements before production release.
- `RELEASE_NOTES_0.8.0.md` — this release candidate's scope and remaining gates.

No account, advertising SDK, cloud sync, external AI processing or paid service is required by the current app.

