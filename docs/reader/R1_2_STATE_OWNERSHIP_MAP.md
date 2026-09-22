# R1.2 — Reader State Ownership Map

Status: architecture decision record for the Reader Reliability track  
Parent slice: R1.1 trace layer (`obs/r1-1-reader-trace`)  
Behavior change: none  
Production merge: not authorized

## Purpose

The current Reader has several places where the same user-visible concept is represented by more than one mutable state. R1.2 assigns one authoritative owner to each state before any behavioral repair is attempted.

The goal is not a rewrite. Existing Readium, Pdfium, Room, DataStore, ViewModel, and Compose surfaces stay in place. Later slices remove ambiguity in small reversible steps.

## Ownership rules

1. Every durable user preference has exactly one durable source of truth.
2. Renderer state is authoritative only for live renderer facts, not durable product preferences.
3. Compose state may cache or draft a value, but a cache/draft is never a second source of truth.
4. Active-reader persistence goes through one gateway so locator/progress ordering remains deterministic.
5. Gesture ownership must be explicit by mode and gesture class; listener registration order must not be the policy.
6. Migration/backup utilities may access repositories directly outside an active reading session, but they must not become competing runtime owners.

## Canonical ownership matrix

| State | Current representations | Canonical owner | Allowed mirrors / adapters | Current risk | Next action |
|---|---|---|---|---|---|
| Reader appearance: theme/font/spacing/scroll/publisher styles/page-turn | `SettingsStore`, `SettingsViewModel.settings`, `LocalLibraryRepository._appearance`, `ReaderScreen.appearance` | **SettingsStore / DataStore** for durable truth; **SettingsViewModel** for app-facing flow and writes | `ReaderScreen` may keep an explicitly temporary optimistic/draft value only | HIGH — duplicate runtime state and two write facades can diverge | R1.3 Appearance Convergence |
| App theme mode | `SettingsStore` → `SettingsViewModel` → `MainActivity` | **SettingsStore / DataStore** | collected `AppSettings` in Compose | LOW | Keep |
| Effective renderer preferences | `ReaderScreen.appearance` mapped to EPUB/PDF preferences | **Renderer receives values; SettingsStore remains policy owner** | mapping functions `toEpubPreferences()`, `toPdfiumPreferences()` | MEDIUM — submit return is observable, real renderer application is not acknowledged | Add verification around effective renderer state where API permits |
| Navigator instance / lifecycle | local `ReaderScreen.navigator` + Readium/Pdfium fragment | **Readium/Pdfium Navigator** for live navigation; `ReaderScreen` owns only attachment/lifecycle reference | nullable Compose reference | LOW/MEDIUM | Keep lifecycle local; never persist navigator object |
| Current live locator | navigator `currentLocator` + callbacks | **Navigator** for live position | ReaderViewModel consumes normalized locator events | MEDIUM — several event paths call `onLocatorChanged` | R1.4 Locator Event Normalization |
| Active-session progress | `ReaderViewModel.uiState.progress`, cached `Book.progress`, navigator progression | **ReaderViewModel** for active derived session progress | UI collection from `uiState` | MEDIUM — multiple representations are valid only if direction is one-way | Make Navigator → ViewModel → Repository flow explicit |
| Durable progress + locator JSON | Room-backed Book state through `LocalLibraryRepository.saveProgress` and coalesced queue | **Room** durable truth; **LocalLibraryRepository** persistence adapter | repository cache for immediate UI response | LOW/MEDIUM | Keep coalescing/flush semantics; active Reader writes only through ReaderViewModel |
| Reading session metrics | `ReadingSessionTracker`, `ReaderViewModel`, repository pending session queue | **ReaderViewModel / ReadingSessionTracker** during session; Room after flush | repository queue | LOW | Keep |
| PDF zoom | `PDFView.zoom` plus local Compose `zoom` in `PdfZoomControls` | **PDFView / renderer** for live zoom | UI value must be derived/synchronized from renderer, never an independent owner | HIGH — pinch/double-tap can mutate renderer while local slider value stays stale | R1.5 Zoom Ownership Repair |
| Page-turn mode | persisted appearance + `ReaderScreen.latestAppearance` + PaperCurl/Directional listener behavior | **SettingsStore** for selected policy; **one explicit input arbitration policy** for effective gesture behavior | listeners act as executors only | HIGH — policy is currently encoded partly by listener order | R1.6 Gesture Arbitration |
| Chrome visibility | `ReaderScreen.controlsVisible` | **ReaderScreen** | none | LOW | Keep ephemeral |
| Appearance/Zoom/Notebook sheet visibility | local Compose booleans | **ReaderScreen** | none | LOW | Keep ephemeral |
| Highlights/bookmarks | Room + repository StateFlows | **Room** durable truth | repository StateFlows | LOW | Keep |

## Confirmed duplication: Reader appearance

The app-facing path is already coherent:

`SettingsScreen / Reader Appearance panel`
→ `SettingsViewModel.saveReaderAppearance()`
→ `SettingsStore.saveReaderAppearance()`
→ DataStore
→ `SettingsViewModel.settings`
→ `MainActivity`
→ `VeilApp`
→ `ReaderScreen(readerAppearance)`

However, `LocalLibraryRepository` also owns:

- `_appearance = MutableStateFlow(ReaderAppearance())`
- a collector from `SettingsStore.settings`
- `loadAppearance()`
- `saveAppearance()`, which writes back to the same SettingsStore through the repository write queue

A repository-wide code search on the R1.1 branch found no runtime callers of `loadAppearance()` or `saveAppearance()` outside their declarations. Backup/restore and legacy migration do use `SettingsStore` directly and must remain supported.

Decision: do not create a new settings architecture. Converge on the existing SettingsStore + SettingsViewModel path and remove the unused LocalLibraryRepository appearance facade only after tests prove backup/migration behavior is unchanged.

## ReaderScreen appearance mirror

`ReaderScreen` currently initializes:

`var appearance by remember(opened.book.id) { mutableStateOf(readerAppearance) }`

and later reconciles it from `readerAppearance` in a `LaunchedEffect`. The Appearance panel mutates the local value immediately and then calls the persistent callback.

This is acceptable only as an explicitly optimistic UI mirror. It must not be treated as durable truth.

Required invariant after R1.3:

`UI intent → SettingsViewModel → SettingsStore → AppSettings flow → ReaderScreen → renderer submit`

An optimistic local update may make preview immediate, but it must reconcile to the DataStore value and must not have a separate persistence path.

## Locator and progress flow

Current healthy foundation:

`Navigator locator`
→ `ReaderViewModel.onLocatorChanged()`
→ `LocalLibraryRepository.saveProgress()`
→ immediate cached Book update
→ coalesced serialized Room write
→ explicit flush on pause/close

Canonical runtime rule:

- Navigator owns the live location.
- ReaderViewModel is the single active-reading event gateway.
- LocalLibraryRepository owns persistence mechanics, not interaction policy.
- Room owns durable progress/locator state.
- Non-reader maintenance operations such as restore/migration may call repository persistence directly outside an active session.

Risk to investigate in R1.4: `ReaderScreen` currently has multiple paths that invoke `onLocatorChanged()`. They must be classified as either authoritative navigator events or explicit commit events and deduplicated by semantics rather than timing.

## PDF zoom ownership

`PdfZoomControls` currently keeps:

`var zoom by remember(view) { mutableFloatStateOf(view?.zoom ?: 1f) }`

while the user can also pinch/double-tap the underlying `PDFView`.

The renderer can therefore change `PDFView.zoom` without changing the local Compose value. The slider/manual buttons then operate from potentially stale state.

Decision:

- `PDFView.zoom` is the live source of truth.
- Compose controls are a projection of renderer zoom and command the renderer.
- No second durable zoom state is introduced unless product requirements later explicitly require per-book zoom persistence.
- R1.5 must reuse the existing Pdfium/PDFView capability; no custom zoom engine.

## Gesture and page-turn ownership

The current input pipeline registers, in order:

1. `PaperCurlInputListener` for EPUB
2. `VeilDirectionalNavigationInputListener`
3. a chrome tap listener

This works partly because listeners consume or fall through, but registration order is currently carrying product policy.

Required explicit policy:

- Scroll mode: scrolling/pan wins; page-turn edge navigation must not interfere.
- EPUB + PAPER + paginated:
  - eligible edge tap/drag → PaperCurl executor
  - center tap → chrome
  - unsupported/non-consumed gesture → Readium fallback
- SLIDE + paginated:
  - edge navigation → directional/Readium animated navigation
  - center tap → chrome
- PDF:
  - pinch/double-tap/pan → PDF renderer first
  - page navigation controls must never steal an active zoom/pan gesture
- RTL direction is resolved by navigator reading progression, not by UI assumptions.

R1.6 should centralize this as a small arbitration policy around the existing Readium input pipeline. Do not add an Android touch overlay and do not fork Readium gesture handling.

## Renderer preference application

Current flow:

`appearance`
→ `LaunchedEffect(navigator, appearance, format)`
→ `submitPreferences(...)`

R1.1 records request/return events, but a returned call is not proof that the visual renderer applied every preference.

Rule:

- Keep submission in the existing ReaderScreen/Navigator integration.
- Do not report `theme_applied` or equivalent unless Readium/Pdfium exposes a trustworthy acknowledgment or the effect is verified by a deterministic test.
- Theme repair should first remove state ambiguity, then test renderer mapping.

## No-change zones

R1.2 does not authorize changes to:

- Room schema
- Readium or Pdfium replacement
- Manga Hub
- production branches
- backup format
- legacy migration format
- gamification behavior
- annotation persistence
- live Manga sources

## Ordered repair slices after R1.2

### R1.3 — Appearance Convergence
- remove/deprecate unused `LocalLibraryRepository` appearance facade after regression tests
- keep SettingsStore as sole durable owner
- make ReaderScreen local appearance explicitly optimistic/draft-only
- verify Theme / font / spacing / scroll / page-turn persistence and reopen

### R1.4 — Locator Event Normalization
- classify every `onLocatorChanged` call site
- establish one authoritative normal-flow event path
- preserve explicit PaperCurl commit semantics
- prove pause/close flush and process/reopen restoration

### R1.5 — PDF Zoom Ownership Repair
- derive UI zoom from `PDFView`
- synchronize manual controls with pinch/double-tap renderer state
- ensure zoom/pan gestures are not consumed by page navigation
- large-PDF regression test

### R1.6 — Gesture Arbitration
- encode mode/gesture precedence explicitly
- preserve Readium as touch authority
- test PAPER, SLIDE, SCROLL, LTR, RTL, PDF zoom/pan
- remove dependence on listener registration order as implicit product policy

### R1.7 — Theme / Renderer Verification
- verify EPUB and PDF preference mappings separately
- add deterministic persistence/reopen checks
- only then change mappings if evidence shows renderer-level defects

## Exit gate for the Reader Reliability ownership phase

A slice may proceed to behavior repair only when the following ownership chain is unambiguous:

`User intent`
→ one policy owner
→ one active-state owner
→ one renderer/persistence adapter
→ one durable owner

Target end-to-end reliability gate remains:

`Open → Settings one move → Theme immediate → Zoom correct → Page turn natural → Close/Reopen → position and settings preserved`.
