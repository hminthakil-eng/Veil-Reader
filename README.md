# Veil Reader 0.5.0

An Android EPUB/PDF reader with a quiet mystery-RPG layer: your reading grows a personal Castle. Original paths and lore; books stay the focus.

**Status: source alpha, not a production release.** The dependency-free reading-policy suite passes 39 checks. The Android app, UI, persistence and exports still require compilation and device verification. No APK is included.

## This release

- Search books by title, author and collection; filter reading/unread/finished/favorites.
- Sort by recency, title, author or progress. Edit metadata and group books into named collections.
- Adaptive library grid and functional system light/dark appearance.
- Reader contents, nested chapters, EPUB/PDF bookmarks, highlights and editable notes.
- Cross-book searchable Hidden Archive, with direct navigation back to a passage.
- Correct Readium font-size percentages; distinct OLED/dusk backgrounds; reserved space for reader controls; scrollable appearance controls.
- Six Path-specific rituals, increasing targets by rank, persistent earned sigils, and Castle shortcuts to working features.
- Page XP rate limits, session revisit rejection, a daily page allowance tied to reading time, and suspension of minute rewards after five minutes without a location change.
- Notebook Markdown export and ZIP export containing imported book files plus annotation/progression metadata.
- Existing v0.4.0 books, highlights, settings and bookmarks retain their storage keys; new metadata fields have defaults.

## Verify

With a Java 17 JDK:

```sh
sh tools/test-policy.sh
```

With Gradle 9.6 and Android SDK 37:

```sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The GitHub Actions workflow runs the policy checks, Kotlin unit tests, lint and APK assembly, and uploads reports and a debug APK on success. The inherited Android dependency versions still need resolution in a real build. There is no Gradle wrapper in this source distribution; CI installs the specified Gradle version.

## On your phone

1. Connect a dedicated Veil Reader GitHub repository. Put the extracted project files at its root, including `.github/workflows/android.yml`; uploading just the ZIP does not build the project.
2. Run Actions → Android Debug APK → Run workflow.
3. Download the APK artifact only after all gates succeed.
4. Install on a test device and import `qa/fixtures/veil-smoke.epub` from this package.
5. Follow `MANUAL_QA.md` before relying on this app for your only copy of reading data.

No connected GitHub repositories were visible during this delivery, so no remote build was launched.

## Backup scope

Profile → Export library backup writes a ZIP with `books/`, `manifest.json`, and recovery instructions. Books can be extracted and re-imported; **automatic restoration of annotations and progression is not implemented yet**. The manifest preserves that information for future restoration and inspection. Markdown export is a readable copy of all saved quotes and notes.

## Release documents

- `RELEASE_NOTES_0.5.0.md`: changes, validation evidence and known limitations.
- `WORLD_CLASS_RELEASE_GATES.md`: requirements before calling this production-ready.
- `MANUAL_QA.md`: concrete device checks, including upgrade and backup tests.

No accounts, external AI processing, advertising SDKs, cloud sync or paid services were added.
