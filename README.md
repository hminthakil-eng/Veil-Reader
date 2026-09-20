# Veil Reader 0.10.0

Veil Reader is a premium, offline-first Android EPUB/PDF reader with an optional mystery-RPG progression layer. Reading remains the primary experience; Paths, rituals and the Castle grow from real reading activity without sitting on top of the page.

**Status: 0.10 release-candidate development.** Repository verification covers reading-policy checks, unit tests, committed Room schema checks, Android lint, debug APK assembly and the performance harness. Runtime emulator/device evidence is tracked separately from host-side build evidence. Physical-device reader QA, accessibility sign-off and Play signing are still required before calling the app production-ready.

## Current capabilities

### Reader
- Real EPUB and PDF import, parsing and navigation through Readium Kotlin Toolkit 3.4.0.
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

## Backup privacy

Settings → Data & backup → Export library backup writes a local ZIP containing imported publications and reading data. The ZIP is **not encrypted**; store it somewhere you trust. Restore validates archive paths and sizes before replacing current state and rolls back if the replacement cannot be committed.

## Engineering documents

- `ROADMAP_0.9_TO_1.0.md` — product/engineering sequence from the pre-1.0 line through the first world-class 1.0 release.
- `ENGINEERING_BLUEPRINT_1.0.md` — architecture principles and earlier 1.0 foundation plan; some baseline sections predate the current Room/DataStore implementation.
- `MANUAL_QA.md` — physical-device verification checklist.
- `WORLD_CLASS_RELEASE_GATES.md` — requirements before production release.
- `RELEASE_NOTES_0.10.0.md` — current 0.10 release-candidate scope, verification state and remaining gates.

No account, advertising SDK, cloud sync, external AI processing or paid service is required by the current app.
