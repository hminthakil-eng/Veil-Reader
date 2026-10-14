# Reader warning cleanup — 2026-10-12

The source-verified starting point is `215f2701f0c7aac5e041b333dabebda22bc0e2d2`,
PR #509 on `reader/native-paper-rebuild-20261011`. Its Android CI run
[`38232610514`](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/38232610514)
reported **0 lint errors and 176 warnings**. Those counts describe the starting
source, not the cleanup source. Current-source test and lint receipts belong in
the PR report after CI completes.

## Inventory and changes

| Starting lint category | Warnings | Correction |
| --- | ---: | --- |
| PluralsCandidate | 57 | Quantity resources and independent plural selection for compound messages; remove an unused count; express completed cycle numbers as identifiers. |
| UnusedResources | 41 | Remove unused English and Persian strings after checking production, debug and test callers. |
| UseKtx | 31 | Use AndroidX bitmap, view, URI and ordinary preference APIs; retain the checked durable adapter described below. |
| ApplySharedPref | 13 | Preserve synchronous durability and explicitly consume the commit result. |
| LogNotTimber | 9 | Owned Timber sinks preserve existing logcat tags and synchronous probe markers. |
| ModifierParameter | 7 | Put `modifier` first among optional composable parameters. |
| GradleDependency | 6 | Update stable Fragment, Lifecycle, coroutines and ZoomImage dependencies. |
| NewerVersionAvailable | 3 | Align test and production dependency versions. |
| ModifierFactoryExtensionFunction | 2 | Extend the caller's existing `Modifier` chain. |
| AppBundleLocaleChanges | 1 | Package both supported languages for offline locale changes. |
| AndroidGradlePluginVersion | 1 | Pin Gradle 9.8.1 and its verified distribution checksum in wrapper and CI. |
| ComposableNaming | 1 | Use lower case for the composable that returns a string. |
| SwitchIntDef | 1 | Explicitly ignore unrelated/future media seek commands. |
| ExportedService | 1 | Document the narrowly scoped standard media service exception below. |
| DataExtractionRules | 1 | Explicitly disable legacy full backup as well as modern backup. |
| ViewConstructor | 1 | Provide a Context constructor for layout/tool creation of the native view. |

English singular forms also correct verbs and nouns. Persian resources retain
their translations and localized number formatting. Compound restore/atlas/
chapter messages select each count independently. The manga page count, rather
than chapter position, selects the page plural. Long counts retain the original
value in formatting even when the Android quantity selector requires an Int.

Kotlin/compiler fixes include the Room override parameter name, redundant casts,
null assertions and safe calls, the controller-aware Media3 accepted-result
builder, and AndroidX checked-state access that supports older platform APIs.
Bitmap replacements preserve dimensions/configuration/filtering and the existing
Readium source revision, URL, viewport and acquisition fences.

`app` now treats lint and Kotlin warnings as errors. No lint baseline or global
warning exclusion is added. Dependency Review, Bouncy Castle minimum-version
checks, performance budgets and native Paper production activation are unchanged.

The dependency workflow also supplies the repository name and actual checkout
SHA to Syft. This removes the two directory-source metadata warnings while
making both CycloneDX root components identify their scanned source. Logging
and dependency scanning stay enabled. See the [Syft configuration reference](https://oss.anchore.com/docs/reference/syft/configuration/).

## Two precise analyzer exceptions

### Checked durable preferences (`UseKtx`)

`DurablePreferences.kt` applies `@SuppressLint("UseKtx")` only to
`SharedPreferences.commitDurably`. The ordinary AndroidX `edit` helper returns
`Unit`, including `edit(commit = true)`, so it cannot return a failed disk write
to backup-restore rollback or `flushDurably()` callers. This adapter updates one
editor, synchronously commits once, checks/logs failure, and returns the Boolean.
It does not suppress `ApplySharedPref`. Ordinary reading counter writes continue
to use asynchronous AndroidX `edit`; lifecycle barriers and rare milestones keep
their synchronous behavior.

Primary sources:
- [AndroidX SharedPreferences helper](https://github.com/androidx/androidx/blob/androidx-main/core/core-ktx/src/main/java/androidx/core/content/SharedPreferences.kt)
- [Android SharedPreferences.Editor commit contract](https://developer.android.com/reference/android/content/SharedPreferences.Editor#commit())

Tests exercise successful atomic commit ordering, failure propagation and the
synchronous failure diagnostic. Existing game, backup, lifecycle and durability
tests continue to cover their callers.

### External media controllers (`ExportedService`)

The manifest applies `tools:ignore="ExportedService"` only to
`ReaderTtsPlaybackService`. A standard Media3 session service intentionally has
`exported="true"` so system and external media controllers can connect. Marking it
private or adding an app-signature permission would break those controls.
Private publication/voice commands still require the app's package in both
`onConnect` and `onCustomCommand`; this cleanup does not widen command access.
The replacement `AcceptedResultBuilder(session, controller)` receives the actual
connecting controller while retaining the service's explicit command sets.

Primary sources:
- [Media3 background playback service manifest](https://developer.android.com/media/media3/session/background-playback)
- [Media3 1.11.0 connection result source](https://github.com/androidx/media/blob/1.11.0/libraries/session/src/main/java/androidx/media3/session/MediaSession.java)
- [AndroidX compatible checked state](https://developer.android.com/reference/androidx/core/view/accessibility/AccessibilityNodeInfoCompat#getChecked())

## Verification boundary

Run complete Android CI (including optimized APK/AAB and benchmark compilation),
Android instrumentation, reader durability, unchanged performance budgets and
complete head/base dependency review on the cleanup head. Inspect the archived
lint XML and test XML, not only workflow status. New tests cover localized counts,
independent quantities, chapter/title argument order, large catalog counts,
durable commit failure and existing diagnostic tags.

Real phones at 60/120 Hz, first/last boundaries, rotation/reflow/themes/background,
long-run memory and subjective paper quality remain a separate outstanding gate.
PR #509 stays Draft and unmerged; native Paper release activation stays OFF.
