# Veil Reader 0.4.0 — Reading Notebook

Source upgrade from the supplied 0.3.0 project. This is not a compiled APK.

## Added
- Contents & notes sheet inside the EPUB/PDF reader.
- Nested chapter list, falling back to reading order when a table of contents is absent.
- Persistent bookmarks with jump-to-place and removal.
- Search across the current book's saved quotes and notes (not full-book search).
- Add/edit notes on EPUB highlights, jump back to a passage, and confirm highlight deletion.
- Appearance settings persist across reader sessions and app restarts.

## Fixed
- Highlight decorations react to repository changes, including deletions.
- Re-saving the same highlighted selection does not award ritual progress again.
- Reader back navigation saves the current locator immediately, without waiting for the debounce.
- Non-finite progress input cannot be serialized as an invalid number.
- Failed file copies remove partially imported files.
- Removed the legacy Android Kotlin plugin and kotlinOptions block conflicting with AGP 9 built-in Kotlin.
- Updated the lifecycle-owner import and reader Material 3 experimental API opt-in.

Build migration reference: https://developer.android.com/build/migrate-to-built-in-kotlin

## Validation and limitations
- Source-level review and ZIP integrity checks performed in this environment.
- No Android SDK, Gradle executable, Kotlin compiler, or cached Android dependencies are available here.
- Attempting to reach Maven from the shell timed out. APK compilation, Android lint, automated Kotlin tests, and device UI verification have NOT run.
- Existing dependency pins were retained; their resolution still needs verification in CI.
- The inherited smoke JAR predates this upgrade and is not evidence that these changes pass.
- SharedPreferences storage is retained; no Room migration, sync, AI, manga or audiobook implementation is included.
- The existing Castle and six paths remain; path-specific rituals and stronger reading anti-farming need future work.

## Phone build
Upload the extracted project contents into a dedicated GitHub repository, with `settings.gradle.kts` at the repository root. A ZIP uploaded as a single repository file will not build. Run Actions → Android Debug APK → Run workflow. Only a successful workflow establishes compilation; download its APK artifact afterward. No repository was created or updated by this source delivery.

See MANUAL_QA.md for the acceptance checks required before treating this as device-tested.
