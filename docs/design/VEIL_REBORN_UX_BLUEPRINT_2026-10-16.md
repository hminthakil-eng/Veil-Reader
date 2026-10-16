# Veil Reader Reborn UX v1 — 2026-10-16

## Intent and design source

Reading is the primary experience. Redesign the UX before extending RPG, TTS or AI.
Editable source: https://www.figma.com/design/wj3UC9brK7faSwuLLuxKTh
Inspected Figma metadata for all five screens and design context for Threshold (3:2).
The compositions express a direction, not a claim of Android device acceptance or pixel parity.

## Five-screen structure

| Screen / Figma node | Outcome | Composition and boundaries |
| --- | --- | --- |
| Threshold Home / 3:2 | Resume in one explicit action | Quiet welcome, real current volume, chapter and progress, dominant Continue Reading, recent shelf, optional Castle note. |
| Library / 3:58 | Retrieve and organize | Search, reading-state filters, collections, cover/title/author rows; retain import, metadata, favorite, delete and all existing callbacks. |
| Reader / 3:117 | Read without interruption | Comfortable paper canvas, restrained chapter/page context, discoverable existing chrome. Preserve EPUB/PDF capabilities, selection, search, bookmarks, annotations and all reading modes. |
| Castle / 3:127 | Visit earned progress voluntarily | World first, truthful existing progress second. No timers, forced quests or reward overlays in the reader. |
| Appearance / 3:162 | Adjust reading comfort | Preview, navigation mode, type size and theme. Bind future redesign to existing preferences; show format-specific support honestly. |

Keep the current app navigation and routes throughout staged implementation. Figma's navigation is a future information-architecture proposal, not permission to remove existing destinations.

## First demonstrable slice

`RebornThresholdScreen` is called from `ReadingNowScreen` only when both DEBUG and
REBORN_THRESHOLD are true. Enable with:

```powershell
./gradlew.bat :app:assembleDebug -PveilRebornThreshold=true
```

Run without the property (or with `false`) to restore the incumbent composition.
Release and benchmark force the flag false. No persisted flag, database migration,
new dependency, permission, account, network request or reader-engine change is introduced.
The build flag affects all debug installs built with it; it is not a per-user preference.
Do not install over an owner's daily library for demonstration; use the existing Forge QA
build option if an isolated app installation is needed.

Real `Book` objects enter from the existing screen caller. Reuse `buildThresholdSnapshot`:
imported books only; most recent unfinished hero, most recent fallback when all finished;
recent books exclude the hero and are bounded to five. Use Start Reading for unopened
volumes, Continue Reading for opened/progressed volumes, Read Again for finished ones.
Display stored chapter and sanitized progress; do not invent reading time or RPG rewards.
Use existing cached cover rendering with its fallback when artwork is unavailable.

The same `onOpenBook` callback handles hero and shelf. Existing publication opening,
resume locator, error recovery and save ownership remain outside this composition.
Library and Castle reuse their existing callbacks. Existing passage/Mirror callbacks and
all incumbent UI remain in source. The experimental home now exposes the passage callback
through Latest Clue below; Mirror remains available through the existing shell routes.
Exit the experiment to restore the incumbent home cards.

## User flows and states

- Returning: Reading tab → Continue Reading → existing publication opening and saved position → back to shell.
- New library: Reading tab → Open Library → existing import flow → real book available on return.
- Switch books: Reading tab → recent volume → existing open flow; View library reaches full retrieval tools.
- Optional world: Show Castle note → Visit Castle → existing Castle; note starts collapsed and is not stored.
- Empty/sample-only: clear import guidance, no fabricated Continue action.
- Offline: existing local book/cover handling; no network-dependent art or content.
- Opening failure/loading: existing shell owns those states and notices, with no duplicate local loading state.
- Restored/deleted books: screen derives from current book list, never retains a stale selected Book as state.

## Native design translation

Retain Figma's editorial hierarchy and quiet cover-led composition. Use current Material/Veil
theme colors and typography rather than hard-code its night palette over user settings.
Constrain content to 720dp, use a lazy vertical list and keyed bounded horizontal shelf.
Stack hero details below the cover on narrow layouts or at font scale ≥1.3; text and
buttons grow vertically. Use logical layout direction and no directional arrow assumptions.
Headings expose semantics, progress exposes native progress semantics and visible text,
decorative covers are silent, and buttons have at least 48dp height. No additional motion
beyond existing cover rendering, which retains its reduced-motion handling.
New copy is in English and Persian Android string resources; wording still needs product review.

## Stage gates and roadmap

1. **Threshold experiment (this slice):** compile opt-in and default paths; run selection and
   callback tests. Review empty, current, finished, missing-cover and recent states.
2. **UX acceptance before broader rollout:** render light/dark, 320/390/expanded widths,
   1×/2× text, RTL/mixed scripts; manually check TalkBack order, target sizes, system back,
   edge insets and foldable placement. Test EPUB and PDF resume, rotation/process death,
   offline opening and import errors against baseline. Physical-device evidence is still required.
3. **Library retrieval:** implement search/filter/collection composition incrementally behind
   another opt-in gate, preserving all actions and Room ownership. Test large local libraries.
4. **Reader and Appearance:** redesign chrome/settings presentation only after continuity
   acceptance. Preserve engines, locators, modes, annotation tools and settings serialization.
5. **Castle:** optional calm world view using existing authoritative progress. No new economy
   or feature gating until reading UX is accepted.
6. **Later features:** consider RPG depth, TTS discovery and book-grounded AI only after UX,
   durability, accessibility and privacy gates; no such expansion is part of v1.

Promotion requires explicit product review plus repository policy/unit/lint/build gates and
manual/device evidence. Automated Compose tests do not prove physical-device quality.

## Verification record

Executed in this worktree:

- `tools/test-policy.sh`: PASS, 39 reading-policy checks.
- `git diff --check`: PASS. Both resource XML files parse, translation keys match,
  and Persian copy uses Persian yeh/kaf.
- `:app:compileDebugKotlin :app:generateReleaseBuildConfig :app:generateBenchmarkBuildConfig
  -PveilRebornThreshold=true --offline`: BUILD SUCCESSFUL. Debug preview compiled;
  generated release and benchmark flags remain false despite the opt-in property.
- `:app:generateDebugBuildConfig --offline` without the property: BUILD SUCCESSFUL;
  generated debug flag is false. The worktree is left with the default-off configuration.
- Requested `:app:compileDebugKotlin :app:testDebugUnitTest --tests '*RebornThresholdScreenTest'
  --tests '*ThresholdModelTest' :app:lintDebug :app:assembleDebug -PveilRebornThreshold=true`.
  Kotlin compilation completed, but Java compilation failed with sandbox AccessDeniedException
  on SDK `core-for-system-modules.jar` and cached `core-common-2.2.0.jar`.
  Unit tests, lint and APK assembly therefore remain unverified; no APK demonstration is claimed.

Validation used the installed Gradle 9.6.0 and Android Studio JBR 25.0.3, with
`ANDROID_HOME` pointing to the installed SDK, writable Gradle/Android preferences under
`.gradle/`, and `GRADLE_RO_DEP_CACHE` pointing at the existing user cache's `caches` directory.
Initial isolated-cache attempts could not resolve AGP 9.4.0; using the cache read-only
resolved that blocker. Kotlin's daemon-marker access failed and its normal compiler fallback
completed Kotlin compilation. The supported JDK 21 environment still needs validation.

Four new Compose tests cover the hero callback with large RTL text, empty-library routing,
recent-book/optional Castle callbacks, and live book updates with Start/Read Again labels.
The RTL test uses English resources and metadata: Persian and mixed-script rendering still
need visual acceptance. Existing ThresholdModelTest covers ordering and placeholder exclusion.
A separate source review found no blocking issue; this is not build or visual acceptance.
No test or build configuration check was disabled to make the slice pass.

## Latest Clue — IMPLEMENTED IN DEBUG PROTOTYPE ONLY

Below Continue Reading, optionally show the latest valid saved `Highlight` from any current
imported `Book`, with book title, saved-highlight provenance, four visible quote lines and
one Trace in the Archive action. It forwards the exact original Book and locator through
`onOpenPassage`; text is never fetched, invented, normalized or persisted by this feature.
No valid saved highlight means the entire card is omitted. The existing build opt-in is
still default OFF; release/benchmark remain OFF. Previous UI and callbacks are preserved.

`deriveLatestClue` rejects blank IDs, quotes and locators, absent books and blank/null source
URIs. It selects descending saved timestamp then ascending highlight ID, with book ID,
locator and quote as deterministic fallback ties. Current library metadata establishes
availability; the existing open flow still handles inaccessible underlying files. Removing
a book/highlight recomputes the card. No reading-order inference or future-content access.

English/Persian copy, semantic heading, descriptive action, a 48dp minimum button and
vertical wrapping support RTL/large text. No fixed card height; long quotes/titles have
visual ellipsis without mutating the saved content. No network, schema, persistence,
Readium, AI, new dependency or reader-overlay changes. No device acceptance.

Added three selector tests and two Compose tests for absent/invalid highlights, unavailable
books, newest selection and deterministic ties, verbatim data, exact passage callback,
book deletion, 200% text and long mixed Persian/English content in RTL. The original empty
library test also asserts the entire clue is absent. These are authored coverage, not
claims that tests executed successfully.

Latest Clue verification (2026-10-16):

- `bash tools/test-policy.sh`: PASS, 39 reading-policy checks.
- `git diff --check`: PASS. Resource XML parses; English/Persian keys match;
  all four added Persian strings use Persian yeh/kaf.
- `gradle :app:compileDebugKotlin :app:generateReleaseBuildConfig
  :app:generateBenchmarkBuildConfig -PveilRebornThreshold=true --offline --console=plain`:
  BUILD SUCCESSFUL (41s). Generated debug flag true; release/benchmark false.
- `gradle :app:compileDebugKotlin --offline --console=plain` without the opt-in:
  BUILD SUCCESSFUL (32s). Generated debug, release and benchmark flags are all false;
  the worktree is left default OFF. Log: `.gradle/latest-clue-default-compile.log`.
- `gradle :app:testDebugUnitTest --tests '*LatestClueTest'
  --tests '*RebornThresholdScreenTest' --offline --console=plain`: BUILD FAILED
  at `:app:compileDebugJavaWithJavac`, sandbox `AccessDeniedException` on cached
  `androidx.arch.core/core-common/2.2.0/.../core-common-2.2.0.jar`. No tests executed;
  no red/green test result is claimed. Kotlin compilation completed using its normal
  fallback after daemon-marker access denial. No Gradle/AGP changes or bypasses.

Logs remain locally under `.gradle/latest-clue-tests.log` and
`.gradle/latest-clue-compile.log`. Environment matches the earlier verification record
(installed Gradle 9.6.0, JBR 25.0.3, read-only shared dependency cache). No APK, lint,
physical-device or TalkBack acceptance was performed for this addition.
