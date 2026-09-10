# 0.5.0 — The Living Library

## Product changes

Library now supports favorites, search, reading-state filters, four sort orders, metadata editing, named collections and adaptive columns. The former inert Collections button is replaced by actual collection assignment and filtering. Continue reading prefers unfinished books.

The Hidden Archive searches saved passages and notes across all books. Its passage button opens the selected locator. Within the reader, bookmarks, contents and editable notes remain available without a rank gate. Castle rooms for Library, Ritual, Observatory and Archive link to their relevant working screens once awakened.

Achievements now derive from stored reading activity and stay earned after a streak ends or a highlight is deleted. Thresholds are one reading hour, ten highlights, a seven-day streak, ten finished books and first rank advancement.

Path rituals now differ: Oracle highlights, Dreamwalker active minutes, Vanguard paced pages, Nocturne nighttime minutes, Archivist thoughtful notes and Artificer concept notes. Targets grow with rank. Old non-Oracle highlight-based ritual progress resets once on migration; rank and XP remain. This behavior is intentional because the old counters represent the wrong activity.

## Reader reliability

Corrected the EPUB size conversion from a multiplier to a percentage, verified against Readium 3.3.0 source. Navigation signatures were also checked. Appearance values now guard non-finite numbers. Dusk and OLED have distinct backgrounds. Measured toolbar padding keeps content out from under controls. Font and line-height changes disable publisher styling so those controls can take effect.

Layout-driven page callbacks are no longer the XP source. Location observations use a paced-page gate: eight seconds between observations, no credit on initial/resumed location, session revisit suppression (bounded to 2,048 locations), and a daily allowance of six pages plus six per active minute. Explicit notebook jumps and appearance changes reset dwell accounting. EPUB and PDF use the same observation path. This discourages easy tapping exploits; it is not proof of reading or a fraud-resistant system.

Minute rewards stop after five minutes without a location change. This is an inactivity heuristic, not attention detection; unusually slow reading may undercount. Completion bonuses still depend on end-of-book progress and require stronger coverage checks before release.

## Portability

Profile exports a Markdown notebook or a ZIP with all accessible imported books, stored settings, annotations and game metadata. File copying runs off the main dispatcher. Missing book files cause an explicit error. An interrupted provider write may leave a partial export. Automatic restore is not implemented; exported EPUB/PDF files can be manually re-imported.

## Validation

- PASS: 39 assertions in the actual Java policy implementation, compiled and executed using the available JDK compiler module.
- PASS: fixture EPUB ZIP layout and XML checks; source-package ZIP integrity and Android XML parsing.
- Reviewed selected Readium 3.3.0 source contracts: Navigator, EpubNavigatorFactory and EpubSettings.
- NOT RUN: Android/Kotlin compilation, Android lint, unit tests requiring Gradle, emulator/device tests, UI rendering, storage migrations and export round trips.
- No Android SDK, Gradle or Kotlin compiler is available locally. No repositories are visible through the GitHub connection, so a remote build could not be started.

The previously bundled gamification-smoke.jar was inherited from 0.3.0. It is not used to verify this release; the reproducible `tools/test-policy.sh` command is the policy gate.

Readium font-size contract: https://github.com/readium/kotlin-toolkit/blob/3.3.0/readium/navigator/src/main/java/org/readium/r2/navigator/epub/EpubSettings.kt
Navigation contract: https://github.com/readium/kotlin-toolkit/blob/3.3.0/readium/navigator/src/main/java/org/readium/r2/navigator/Navigator.kt
