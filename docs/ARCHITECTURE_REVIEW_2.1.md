# Veil Reader Architecture Review 2.1 — Adversarial Audit

Date: 2026-09-20
Branch audited: agency/p7-ux-rebuild

Purpose: actively search for architecture defects, product ugliness, regression risks and hidden scale problems.

## Executive result

Architecture 2.0 is directionally correct, but the current implementation still contains prototype-era coupling that can produce:
- navigation context loss
- visually inconsistent screens
- difficult localization
- reader regressions
- persistence split-brain
- uncontrolled UI complexity
- format-specific implementation leakage

This review upgrades the target to Architecture 2.1 with additional guardrails.

## Findings fixed immediately

### A21-001 — Settings destroyed navigation context — FIXED
Previous behavior:
- opening Settings forced selectedTab = PROFILE.
- closing Settings left the user in Profile.

Correct behavior:
- Settings is an overlay/destination above the current context.
- Back returns to the originating tab.

### A21-002 — Reader close destroyed origin context — FIXED
Previous behavior:
- closing any Reader forced selectedTab = LIBRARY.

Correct behavior:
- close Reader returns to the tab/context from which it was opened.
- future Navigation 3 migration will model this as a true back stack.

### A21-003 — Corrupted dead navigation glyph data — FIXED
VeilTab carried unused glyph strings that had become mojibake in source.
They were not a legitimate icon system and were removed.
Navigation visuals must come from the Veil icon system, not Unicode glyph hacks.

## P0 architecture risks

### A21-004 — UI localization architecture is missing — RED
Evidence:
- no meaningful strings.xml resource inventory.
- roughly 95 direct Text("...") literals in UI.
- navigation labels live in Kotlin state.

Impact:
- Persian/Arabic cannot become first-class safely.
- translation and pluralization become fragile.
- accessibility/localization QA cannot be systematic.

Decision:
- all user-facing product copy migrates to Android resources.
- dynamic/domain content remains domain data.
- no new hard-coded user-facing strings after migration begins.

Gate:
- Persian pseudo/localized build must exist before world-class claim.

### A21-005 — Screen/god-composable size — RED
Current examples:
- LibraryScreen ~965 LOC.
- ReaderScreen ~869 LOC.
- VeilChrome ~500+ LOC.
- Path/Castle/ReadingNow each several hundred LOC.

Impact:
- visual hierarchy becomes difficult to reason about.
- recomposition boundaries are unclear.
- feature changes create visual regressions.
- screenshot coverage becomes coarse.

Decision:
Split screens by responsibility:
- ScreenRoute/state wiring.
- ScreenScaffold.
- sections.
- reusable design-system components.
- dialogs/sheets.
- pure presentation models.

Guardrail:
- >400 LOC screen file triggers decomposition review.
- >250 LOC reusable component file triggers review.
Not a mechanical failure threshold, but architecture review is mandatory.

### A21-006 — Reader UI imports engine details — RED
Current reader screen directly knows Readium navigator types.
PDF controls know PDFView.

Impact:
- format engine upgrades leak into UI.
- impossible to test reader behavior with lightweight fakes.
- PDF/EPUB feature parity logic becomes scattered.

Decision:
Create reader anti-corruption layer:
feature:reading -> reader:api only.
Readium/PDFium classes never cross into feature UI after migration.

### A21-007 — Durable state split across Room / DataStore / SharedPreferences — RED
GameRepository still uses SharedPreferences.
Backup code contains legacy preference handling.

Impact:
- non-transactional progression.
- harder sync.
- restore consistency risk.
- schema migrations spread across storage systems.

Decision:
- Room: domain/durable relational state.
- DataStore: lightweight product preferences only.
- SharedPreferences: migration input only; no new writes after progression migration.

### A21-008 — Unbounded serialized write queue — YELLOW
LocalLibraryRepository uses Channel.UNLIMITED.

Current progress/session writes are coalesced, reducing practical risk, but the queue has no explicit backpressure contract.

Decision:
During repository split:
- define write classes: high-frequency/coalesced, user-critical immediate, background bulk.
- bounded queues or suspendable ordered writer where appropriate.
- expose storage failure state to product UI.
- instrument queue depth in debug/benchmark builds.

### A21-009 — Manual boolean navigation scales poorly — YELLOW
Current route state combines selectedTab plus showArchive/showSettings/activeChamber/activeBook.

Impact:
- invalid combinations require normalization.
- back-stack semantics are encoded manually.
- future deep links/manga/search/audio become complex.

Decision:
Migrate to Navigation 3 after P7 physical acceptance.
Typed destinations + nested graphs + saved-state strategy.

## P0 visual/UX architecture risks

### V21-001 — Decorative effect density is too high — RED
Audit found many gradients, translucent surfaces, borders and high shadow elevations across:
- VeilChrome
- Castle
- ReadingNow
- Settings
- shared cards

This creates the exact risk the product standard forbids: an AI-dashboard / fantasy-dashboard look.

Decision:
Adopt Visual Restraint Budget:
- Reading surfaces: zero decorative gradients over content.
- Core shell screen: at most one atmospheric background treatment.
- Avoid nested elevated cards.
- Shadows communicate z-order only, not decoration.
- Prefer spacing/type hierarchy before borders and effects.
- Castle may be richer than Library/Settings, but must remain coherent.

### V21-002 — Hard-coded visual tokens outside theme — YELLOW
Reader paper-curl colors are hard-coded in ReaderScreen.

Decision:
Move reader paper/effect palette to design-system semantic tokens:
- paperFront
- paperBack
- paperEdge
- foldShadow
per reader theme.

### V21-003 — No automated visual regression — RED
Build/lint/unit tests can all pass while UI looks bad.

Decision:
Introduce screenshot/golden tests for:
Library, ReadingNow, Settings, Reader chrome/sheets, Castle, Path.
Matrix:
Light/Dark × Compact/Expanded × LTR/RTL × Normal/Large font.

### V21-004 — Icon system must be canonical — YELLOW
No Unicode glyph navigation.
No ad-hoc one-off hand icons unless added to the canonical Veil icon system.
Every icon-only button requires contentDescription.

### V21-005 — UI copy and typography need localization-safe hierarchy — RED
All-caps/letter-spacing aesthetics can damage Persian/Arabic presentation.

Decision:
Design tokens must support script-aware typography.
Atmospheric typography is allowed only when language-safe.

## Functional bug risks

### B21-001 — Page-turn visual effect may never be authoritative — P0
Previously proven by GPU regression.
Permanent invariant remains.

### B21-002 — PDF controller leakage — P0
Move all PDFView traversal/control behind PdfReaderController.

### B21-003 — Backup/restore is still a high-risk surface — P0
Must validate:
- ZIP traversal
- size bombs
- schema/version
- staging
- rollback
- interrupted restore
before release confidence.

### B21-004 — process-death + overlays + reader origin — P0
New origin-preserving route behavior is covered by unit tests.
Future Navigation 3 migration must preserve equivalent saved-state behavior.

### B21-005 — import/copy failure and disk pressure — P1
App-private copies are correct for durability, but storage capacity/low-disk user feedback needs explicit handling.

### B21-006 — cover decoding path is manual — P1
Manual BitmapFactory use in UI should migrate to a central Coil-backed image pipeline with size-aware decode/cache.

## Architecture 2.1 additions

### 1. Anti-corruption boundaries
Every external toolkit gets a Veil-owned boundary:
- Readium -> ReaderSession
- PDFium -> PdfReaderController
- manga parser/source -> MangaSourceProvider
- sync backend -> SyncTransport
- AI -> BookIntelligenceEngine
- image loader -> Cover/ImageRepository abstraction where domain needs it

### 2. Capability-driven UI
Do not branch UI on file extension or concrete engine class.

ReaderCapabilities becomes the only UI capability source:
- canSearch
- canSelectText
- canHighlight
- canTts
- canZoom
- supportsPagination
- supportsScroll
- supportsPageTurnEffects
- supportsTwoPage
- supportsRtl

### 3. Typed failure model
Replace arbitrary strings/exceptions crossing layers with typed domain failures:
- StorageFailure
- PublicationOpenFailure
- UnsupportedFormat
- CorruptPublication
- RestrictedPublication
- SearchFailure
- SyncFailure
- SourceFailure

UI maps these to localized user messages and recovery actions.

### 4. Product state model
Every screen must model:
Loading / Content / Empty / Error / OfflineDegraded.
No screen may fake loading by showing empty content.

### 5. Complexity budgets
Architecture review is required when:
- screen file >400 LOC.
- repository >400 LOC.
- ViewModel >300 LOC.
- function >80 LOC unless generated/parser/algorithmic with justification.
- composable has >3 independent boolean modal states.
- a feature imports more than one concrete engine implementation.

### 6. Dependency budget
Every new dependency documents:
- problem solved.
- why platform/current stack is insufficient.
- license.
- maintenance activity.
- binary/startup cost if material.
- removal/fallback path.

### 7. Visual quality as architecture
Visual quality is not a final styling pass.
Feature architecture must expose stable previewable UiState so design can be reviewed without live repositories/network/reader engines.

### 8. Localization-first API
ViewModels return semantic data, not English UI sentences.
Formatting belongs at UI/resource boundary.
Domain exceptions never become direct user-visible strings.

### 9. Backpressure and lifecycle
High-frequency streams must define:
- producer rate.
- coalescing behavior.
- cancellation.
- durability boundary.
- process-death recovery.

### 10. Offline degradation contract
Any future network-backed feature declares behavior for:
- no network.
- slow network.
- provider down.
- stale cache.
- auth expired.
Core reading remains unaffected.

## Priority order after P7

P0.1 — localization/resource foundation.
P0.2 — split ReaderScreen/LibraryScreen and create design-system sections.
P0.3 — reader:api anti-corruption boundary and PdfReaderController.
P0.4 — screenshot regression harness.
P0.5 — progression persistence migration from SharedPreferences.
P0.6 — backup/restore v2 hardening.
P1 — Navigation 3 + adaptive typed destinations.
P1 — central Coil image pipeline.
P1 — WorkManager/background infrastructure.
P1 — SearchIndex.
Then Manga vertical slice, sync and AI.

## Definition of GREEN after this audit

GREEN requires all applicable:
- functional tests.
- lint/build.
- physical-device behavior.
- visual QA.
- localized/RTL sanity.
- accessibility sanity.
- architecture boundary compliance.
- no unresolved P0 risk introduced by the change.

A build can be technically successful and still remain RED.
