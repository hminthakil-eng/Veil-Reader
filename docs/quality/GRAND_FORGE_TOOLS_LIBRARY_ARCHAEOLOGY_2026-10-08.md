# Grand Forge: Reader tools, PDF, library and settings archaeology

Evidence snapshot: `3cc162aeadd5c4ced809e26e110946244cc74560`, 2026-10-08. Read-only source investigation followed by this documentation-only addition. No production code or tests were changed by this investigation. Other agents share the workspace; this report makes no global working-tree cleanliness claim.

Source inspection establishes behavior and gaps, not physical-device acceptance. No tests or benchmarks were executed by this investigator. No row is product GREEN without implementation, integration, tests, device QA, UX QA and release enablement. The root coordinates Wave 1's first implementation: static-paged lifecycle ownership; tasks below are subsequent candidates, not parallel production edits.

## Ownership and dependency map

| Area | Current owner | Existing behavior/reuse | Boundary/evidence |
|---|---|---|---|
| Runtime library | `VeilApp.kt:128` creates `LocalLibraryRepository` | Room, observable cached domain lists, serialized writes, progress/session coalescing, crash checkpoints | ReaderScreen, ReaderViewModel, LibraryExport and Manga import consume the concrete class |
| Durable writes | `LocalLibraryRepository.kt:83–131,1349–1376` | Channel queue; initialization barrier; sticky prior-write failure; awaitable `orderedWrite` | Observer caches at 145–155 are eventually consistent and must not authorize durable operations |
| Intended repository interface | `LibraryRepository.kt:8–14` | Suspend/Flow API contract exists | No implementation/consumer found in main source; comment incorrectly describes SharedPreferences runtime. Do not force a wholesale migration |
| Bookmarks | `Models.kt:347–354`, `Entities.kt:54–70`, `Daos.kt:149–162` | Independent Bookmark domain, Room FK/CASCADE; labels and locators | Creation ReaderScreen3280–3307, notebook list/jump/delete569–605, Archive list/delete; not implemented through Highlights |
| Selection | `ReaderSelectionActionMode.kt` plus Readium | Native handles/action mode, Highlight/Note/Lookup additions | ReaderScreen961–1020 session/lifecycle fences; native Copy is not replaced |
| Dictionary/translation interoperability | `ReaderLookup.kt:6–31` | Read-only Unicode `ACTION_PROCESS_TEXT` chooser; failure feedback ReaderScreen977–982 | Current generic lookup is not a dictionary or translation provider/result domain |
| PDF open/location | `ReadiumEngine.kt:110–157`, `PdfiumLocatorCompat.kt:9–46` | Pdfium publication open, version-marked locator migrations | Repository878–914 transactionally rewrites matching progress/bookmark/highlight locators |
| PDF native reading | `ReaderScreen.kt:4317–4335`, `ReaderPdfInput.kt`, `PdfZoomControls.kt` | Pdfium render, pan/zoom, internal links, safe external URLs, embedded annotation appearances | Veil chrome tap is deferred until native link hit test; document page indices stay distinct from RTL view indices |
| PDF authored markup | No authored annotation path found in scoped searches | Embedded annotation rendering and native fixture available for reuse | `ANDROIDX_PDF_EDITOR` false at `VeilFeatureGates.kt:34`; this gate alone is not evidence of a complete hidden editor |
| Library presentation | `LibraryScreen.kt:161–180,259–342` | Saved filter/query/view state, transient editing/menu state, memoized normalization/ranking/filtering | File length is not itself evidence of a runtime bug. Extract only after behavior characterization |
| Backup | `LibraryExport.kt`, repository1071–1140 | Ordered Room snapshot, versioned schema1–5, transaction replacement, attempted rollback, bounded validated extraction | Backup includes ReaderAppearance and game preferences, not full AppSettings |
| Settings | `SettingsStore.kt:53–140` | DataStore app/Reader/TTS/input/accessibility preferences | IOException at128–134 emits defaults without an observable read-error state |

## Actionable tasks

### GF-W1-BOOKMARK-ACK

- Severity: **P0**.
- Subsystem: bookmark durability.
- Affected files: `app/src/main/java/com/veilreader/app/data/LocalLibraryRepository.kt:214–224`; `app/src/main/java/com/veilreader/app/data/db/Daos.kt:149–162`; `app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt:3289–3307`; `app/src/androidTest/java/com/veilreader/app/data/RoomRuntimeRepositoryInstrumentedTest.kt`.
- Observed behavior: creation modifies the cache, enqueues Room upsert and immediately returns true. Reader announces saved/haptic immediately. Queue catches later write errors and records storageFailure123–131. Duplicate checks use only eventually consistent cache. Existing persistence tests flush writes before checking rows.
- Expected behavior: saved acknowledgment follows durable insert; cold-cache/concurrent duplicate requests create one bookmark; failed writes report failure without a saved acknowledgment.
- Root cause: optimistic cache is operation authority and queue submission is mistaken for commit completion.
- Implementation plan: reuse awaitable orderedWrite1360–1376 for suspend creation; perform duplicate query and insertion inside serialized write; launch session-fenced Reader coroutine; localized error and optional pending state. Characterize current queue/error semantics before changes.
- Reuse candidate: existing Bookmark entity/domain/DAO, write queue, durable selection-note operation711 onward, Reader session/error patterns.
- Regression risks: changed call sites; double tap; deletion before queued insert; cancellation and stale Reader feedback; sticky storageFailure semantics.
- Tests required: cold-cache duplicate; concurrent duplicate; immediate Room visibility after success; injected write failure; deleted book; feedback after session change; reopen without explicit flush.
- Performance implications: indexed lookup plus one insert; no observer wait. Measure operation latency if unexpectedly delayed behind backlog.
- UX implications: truthful saved/duplicate/failure feedback and accessible pending state.
- Acceptance criteria: success survives repository recreation; failed creation never reports saved; duplicate concurrency yields one row; actual touch/TalkBack behavior verified before GREEN.
- Dependencies: existing ordered-write semantics, Reader session fence and localization.
- Status: **RED — source-verified premature acknowledgment; not fixed in this wave**.

### GF-W1-SELECTION-OWNER

- Severity: **P0**.
- Subsystem: selection input ownership.
- Affected files: `ReaderSelectionActionMode.kt:47–48,135–137`; new callback lifecycle characterization test.
- Observed behavior: destruction always sends onModeChanged(false), including when the destroyed mode differs from activeMode. Identity check protects only clearing the field.
- Expected behavior: an obsolete mode's destruction cannot unlock Reader while a replacement mode remains active.
- Root cause: active notification lacks the ownership identity fence.
- Implementation plan: reproduce createA/createB/destroyA callback order with fake ActionModes; if characterized, guard inactive notification by mode identity and preserve dismiss behavior.
- Reuse candidate: existing activeMode identity, coroutine cleanup and native selection.
- Regression risks: OEM callback ordering and selection dismissal.
- Tests required: replacement mode stays active after old destruction; current destruction clears; stale dismissal cannot clear newer selection.
- Performance implications: constant-time guard, no renderer work.
- UX implications: prevents page-navigation ownership leakage during selection.
- Acceptance criteria: lifecycle regression tests and real selection/overlay transitions pass.
- Dependencies: characterization first; root navigation ownership work.
- Status: **YELLOW — source-backed conditional race; device occurrence not demonstrated**.

### GF-W4-BOOKMARK-LABEL

- Severity: **P0**.
- Subsystem: bookmark tools.
- Affected files: `Models.kt:347–354`; `LocalLibraryRepository.kt:214–224`; `ReaderNotebook.kt:589–605`; `ArchiveScreen.kt:962` onward.
- Observed behavior: stored labels exist; notebook actions are return/remove; no label-update API/action found in scoped source search.
- Expected behavior: user can rename bookmark without changing identity/locator/creation timestamp.
- Root cause: update use case and UI action absent.
- Implementation plan: durable label-update query through orderedWrite; small notebook edit dialog; preserve domain and locator.
- Reuse candidate: existing note edit presentation and localized input/action styles.
- Regression risks: deletion/restore while dialog open, stale label update.
- Tests required: label-only update, deletion race, backup label round-trip, accessibility dialog actions.
- Performance implications: one row update.
- UX implications: secondary progressive-disclosure action; no expanded persistent toolbar.
- Acceptance criteria: create/list/jump/delete/rename durable and device/UX verified.
- Dependencies: GF-W1-BOOKMARK-ACK.
- Status: **RED — requested rename behavior missing; domain replacement unnecessary**.

### GF-W4-LOOKUP-PROVIDERS

- Severity: **P0**.
- Subsystem: dictionary and independent translation.
- Affected files: `ReaderLookup.kt`; `ReaderSelectionActionMode.kt`; `ReaderScreen.kt:977–982`; `ReaderLookupTest.kt`; new provider interfaces/results.
- Observed behavior: generic external PROCESS_TEXT chooser; no provider/language default, compact in-reader result or independent translation provider/action found. Tests cover intent construction only.
- Expected behavior: optional capability-specific providers isolated from Reader core, explicit availability and suitable in-reader results where supported.
- Root cause: current implementation is an interoperability primitive, not mature lookup/translation domain.
- Implementation plan: preserve installed-app intent path; capability-aware interfaces; separate meaningful actions; first configured installed-provider routing, then evaluate offline/result reuse. Avoid pretending all installed apps support both capabilities.
- Reuse candidate: Android PROCESS_TEXT, existing current-selection capture and failure feedback; evaluate lawful licensed providers before custom engines.
- Regression risks: Android package visibility, unavailable app, language support, network privacy and stale results.
- Tests required: no provider, launch exception, capability availability, cancellation/session fence, RTL result and explicit optional networking.
- Performance implications: bound queries/results; no blocking network in Reader core.
- UX implications: compact results and clear external launch behavior; primary/More toolbar characterization.
- Acceptance criteria: truthful capability labels/availability, real provider interoperability, device/UX/accessibility verification; no fabricated offline definition.
- Dependencies: selection ownership, provider reuse research and privacy boundary.
- Status: **RED — mature requested capabilities absent; existing generic intent lookup remains YELLOW pending product gates**.

### GF-W5-PDF-ANNOTATIONS

- Severity: **P0**.
- Subsystem: authored PDF annotation.
- Affected files: `ReaderScreen.kt:4317–4335`; `PdfZoomControls.kt`; `ReaderPdfInput.kt`; `VeilFeatureGates.kt:34`; new annotation/controller/storage paths and tests.
- Observed behavior: embedded annotation appearance rendering is enabled; native fixture compares requested rendering. No creation/edit/persist annotation tools found. Existing render, zoom and links are separate retained functionality.
- Expected behavior: Highlight/Underline/Strikethrough/Note persist against stable document/page coordinates.
- Root cause: authored markup path absent; not evidence of an existing complete disabled feature.
- Implementation plan: preserve Pdfium input/surface; evaluate licensed supported annotation APIs before design; fixture-driven sparse overlay/document identity model; expose tools only after persistence and UX acceptance.
- Reuse candidate: Pdfium public hooks, native fixtures, stable locator migrations; supported annotation component subject to verified capability/licensing.
- Regression risks: zoom/rotation coordinates, RTL document/view indices, document replacement, native writer limitations, original-file corruption.
- Tests required: author/reopen/rotate/zoom fixtures; document identity changes; link input ownership; backup/restore; real device rendering.
- Performance implications: sparse overlays and coordinate/tile budgets; measure PDF page-render P50/P95 before/after.
- UX implications: only complete basic markup set; no incomplete advanced toolbox.
- Acceptance criteria: authored fixture markup survives reopen at exact document locations, device visual/UX verification and release evidence; compilation alone insufficient.
- Dependencies: PDF reliability characterization, supported provider and persistence design.
- Status: **RED — authored annotation missing; existing reading surface YELLOW absent current execution evidence**.

### GF-W7-BACKUP-PREFERENCES

- Severity: **P1**.
- Subsystem: backup fidelity.
- Affected files: `LibraryExport.kt:85–135,457–598,732` onward; `LocalLibraryRepository.kt:1071–1097,1101–1140`; `SettingsStore.kt:53–121`; RoomRuntime backup tests.
- Observed behavior: snapshot stores only ReaderAppearance. ReaderTts, tap grid, hardware keys, focus guide, high contrast and per-publication spread overrides are not included/restored.
- Expected behavior: declared Reader/TTS preferences round-trip with versioned schema and backwards defaults.
- Root cause: snapshot preferences model predates expanded AppSettings.
- Implementation plan: versioned preference snapshot and SettingsStore adapters; schema migration; retain validated archive/rollback machinery and define cross-store failure recovery.
- Reuse candidate: current schema1–5 parser, archive bounds/path checks, Room snapshot/transaction, settings tests.
- Regression risks: partial DB/settings commits, future schema rejection and unexpected overrides.
- Tests required: all settings round-trip; older schemas; malformed payload; preferences write failure and rollback.
- Performance implications: small manifest growth, no raw database sync.
- UX implications: accurately state included settings; optional publication inclusion separately scoped.
- Acceptance criteria: portable schema round-trip and recoverable failure; later device restore/UX validation.
- Dependencies: settings serialization contract and backup atomicity characterization.
- Status: **RED — preference completeness missing; existing data/publication backup YELLOW absent all product gates**.

### GF-W1-SETTINGS-READ-ERROR

- Severity: **P0**.
- Subsystem: persistence/error transparency.
- Affected files: `SettingsStore.kt:128–134`; app/Reader settings consumers and characterization tests.
- Observed behavior: DataStore IOException emits defaults with no separate observable unavailable state; defaults can look like preference reset.
- Expected behavior: distinguish unavailable read from legitimate first-run empty preferences; preserve last-known intent where practical.
- Root cause: recovery emission conflates missing preferences and failed read.
- Implementation plan: characterize IOException versus empty store; add status flow or sealed load state; restrained localized retry feedback. Do not broad-refactor all consumers before characterization.
- Reuse candidate: DataStore catch/recovery, existing settings presentation/error patterns.
- Regression risks: API propagation, transient startup error noise and incorrect cached state.
- Tests required: injected read error, recovery, first run, last-known values and failed save.
- Performance implications: event-driven recovery; no polling.
- UX implications: actionable restrained error rather than unexplained settings change.
- Acceptance criteria: read failure distinguishable, recovery preserves intent, device failure path verified.
- Dependencies: settings presentation boundary and characterization.
- Status: **YELLOW — silent source fallback verified; device failure not reproduced**.

## Inspected files and coverage limits

Paths below are repository-relative. Full content was read for compact files; large files were inspected through targeted sections and symbol searches. Listed tests were inspected as source, not executed. A file discovered by name is not counted as a complete review. This is the tools/library/PDF slice of Wave 0, not a claim that every repository source or every test assertion has been audited.

### Main files inspected

- `app/src/main/java/com/veilreader/app/ui/screens/ReaderSelectionActionMode.kt` — full.
- `app/src/main/java/com/veilreader/app/ui/screens/ReaderLookup.kt` — full.
- `app/src/main/java/com/veilreader/app/ui/screens/ReaderPdfInput.kt` — full.
- `app/src/main/java/com/veilreader/app/data/PdfiumLocatorCompat.kt` — full.
- `app/src/main/java/com/veilreader/app/data/LibraryRepository.kt` — full and consumer search.
- `app/src/main/java/com/veilreader/app/ui/screens/LibrarySearch.kt` — full.
- `app/src/main/java/com/veilreader/app/feature/VeilFeatureGates.kt` — full.
- `app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt` — selection961–1020, PDF link symbols1701–1752, locator settlement symbols1901–1952, input symbols2376–2498, preferences2706–2709, bookmark3270–3312, notebook wiring3781–3929 and PDF factory4308–4345.
- `app/src/main/java/com/veilreader/app/ui/screens/ReaderNotebook.kt` — search/coroutine symbols and bookmark569–614.
- `app/src/main/java/com/veilreader/app/ui/screens/PdfZoomControls.kt` — imports/API, renderer probing/mirror73–117, controls symbol sections, finite helpers546–563.
- `app/src/main/java/com/veilreader/app/ui/screens/LibraryScreen.kt` — parameter/state/filter/search symbol map107–368 and bookmark/material references.
- `app/src/main/java/com/veilreader/app/ui/screens/ArchiveScreen.kt` — bookmark query/list/delete/card symbol sections108–158,476–497,630–635,962 onward.
- `app/src/main/java/com/veilreader/app/data/LocalLibraryRepository.kt` — queue/init/cache75–245, bookmark mutation, locator migrations878–914, snapshot/replace1065–1145, ordered-write1320–1390; remaining methods symbol map.
- `app/src/main/java/com/veilreader/app/data/LibraryExport.kt` — export60–155, restore450–600, bounded extraction619–673; schema/serialization symbol map732–969.
- `app/src/main/java/com/veilreader/app/data/settings/SettingsStore.kt` — settings model/keys/load1–155, save symbols.
- `app/src/main/java/com/veilreader/app/data/ReadiumEngine.kt` — PDF open/migration110–175 plus PDF format/provider symbol search.
- `app/src/main/java/com/veilreader/app/data/db/Daos.kt` — BookmarkDao149–162.
- `app/src/main/java/com/veilreader/app/data/db/Entities.kt` — BookmarkEntity50–76.
- `app/src/main/java/com/veilreader/app/domain/Models.kt` — Bookmark338–360 plus reader domain enum symbols.
- `app/src/main/java/com/veilreader/app/ui/VeilApp.kt` — concrete repository construction128 (symbol search).
- `app/src/main/java/com/veilreader/app/ui/reader/ReaderViewModel.kt` — concrete dependency37/514 (symbol search); full Reader lifecycle review belongs to root/specialist.
- `app/src/main/java/com/veilreader/app/ui/screens/ReaderFragmentRestoration.kt` — PDF restoration publication/provider symbols32–67; full behavior review belongs to lifecycle slice.

### Test source inspected

- `app/src/test/java/com/veilreader/app/ui/screens/ReaderLookupTest.kt` — full; blank/read-only/Unicode intent construction, not installed-provider launch.
- `app/src/test/java/com/veilreader/app/ui/screens/ReaderPdfInputTest.kt` — full; tap cancellation/disposal/foreground, document indices, invalid links and page extraction.
- `app/src/test/java/com/veilreader/app/ui/screens/PdfZoomPolicyTest.kt` — full; bounded probe and finite zoom.
- `app/src/test/java/com/veilreader/app/ui/screens/PdfZoomControlsTest.kt` — full; finite bounds/steps and reduced motion.
- `app/src/test/java/com/veilreader/app/ui/screens/ReaderSelectionNotePolicyTest.kt` — full; new-note text policy, not ActionMode lifecycle.
- `app/src/test/java/com/veilreader/app/ui/screens/LibrarySearchTest.kt` — first145 lines; Persian variants/marks/digits, localized decimal and ranking; remaining file not fully inspected.
- `app/src/androidTest/java/com/veilreader/app/ui/screens/ReaderPdfNativeFixtureTest.kt` — full; real Pdfium fixture embedded appearances and document link destination; source explicitly says compilation is not device execution.
- `app/src/androidTest/java/com/veilreader/app/ReaderPdfReliabilityInstrumentedTest.kt` — first160 lines; native gestures/layout/fit/internal links/rotation and optional failure screenshot; helper remainder not fully inspected.
- `app/src/androidTest/java/com/veilreader/app/data/PdfiumLocatorCompatInstrumentedTest.kt` — full; marker idempotence, JSON round-trip, identity remap and EPUB/PDF persistence distinction.
- `app/src/androidTest/java/com/veilreader/app/data/settings/SettingsStoreInstrumentedTest.kt` — method inventory32–277; recreation coverage, no read-failure assertion inspected.
- `app/src/androidTest/java/com/veilreader/app/data/RoomRuntimeRepositoryInstrumentedTest.kt` — method inventory60–918 and assertions88–115; repository, backup, locator, progress and deletion coverage. Bookmark tests inspect durability after flush, not acknowledgment boundary.
- `app/src/androidTest/java/com/veilreader/app/data/manga/MangaLocalImportCoordinatorInstrumentedTest.kt` — backup round-trip symbol sections550–614 only.

Additional test paths discovered but not substantively inspected: `LibraryCompositionTest.kt`, `GrandLibraryModelTest.kt`, `LegacyLibraryMigratorTest.kt`. They require follow-up before asserting exhaustive library characterization.

## Execution handoff

Completed: source ownership map and actionable evidence-backed task records. No runtime performance before/after measurements available; no invented metrics. Regressions: none introduced by this documentation-only slice; existing risks listed above remain.

Still RED: premature bookmark save acknowledgment; missing bookmark rename; mature provider-specific dictionary/translation; authored PDF markup; full Reader/TTS preference backup. YELLOW risks need characterization/device evidence rather than premature fixes or GREEN claims.

Next: root's static-paged lifecycle regression/fix first. Then characterize GF-W1-BOOKMARK-ACK and GF-W1-SELECTION-OWNER as small independent reliability changes; postpone PDF authored markup until its scheduled wave and verified reusable capability.
