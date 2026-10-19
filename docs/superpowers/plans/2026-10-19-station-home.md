# Station Home Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a real native, source-qualified Station Home review slice from the owner's supplied visual reference, using actual library data and existing navigation.

**Architecture:** A pure station model reuses `buildThresholdSnapshot`; a dedicated Compose surface owns station presentation only. Existing `VeilApp` owns requests/routes, and an existing-style feature gate keeps the current Home as the default outside dedicated QA builds. Scene assets, live text and semantic controls remain separate layers.

**Tech Stack:** Existing Kotlin/Compose/Readium/Room versions, JDK21, SDK37, Gradle9.6.0, native Robolectric and Android instrumentation. No new graphics engine or production dependency.

**Spec:** `docs/design/2026-10-19-station-home-reference.md` and the ownership/visual/verification constraints of `docs/superpowers/specs/2026-10-18-reader-rebuild-design.md`. Owner “Go” after the written station reference on October19 approves that visual direction for planning. It does not approve this newly written plan or the independent pending Library retrieval proposal.

## Global Constraints

- Preserve the owner's working reader; no reader engine/session/input/storage/schema/signing/application-identity changes.
- Preserve current READING/LIBRARY/CASTLE/PATH/PROFILE routes and the existing `gameVisible` policy; no second bottom dock inside Home.
- Notes uses the existing Archive overlay through an explicit callback. The compass ornament is decorative in this slice; current Castle navigation remains the only world entry.
- Category statistics are honest non-clickable summaries. One clearly labelled Library action opens Library; filtered category routing belongs to the separate retrieval cycle.
- No fabricated chapters, time remaining, titles, percentages or archive counts. Omit unavailable information.
- Compose supplies live text/controls; art supplies scenery. Exclude the mock phone/status bar and baked UI from scene assets.
- Motion obeys `LocalVeilReducedMotion`, resumed lifecycle and actual Home visibility; hide owned scenery and cancel animation when Home is hidden. Do not globally clear shared cover caches or manually recycle bitmaps still used by Compose.
- Readable opaque/inset panels over rich art; EN/LTR and FA/RTL at 320/390dp, 100%/200% font and tablet/landscape.
- Existing performance/quality budgets stay authoritative; no force push, bulk merge, uninstall, data reset or public release.
- Product code and generated assets begin only after plan review. This document contains no build or visual acceptance receipt.

## Review Focus

1. Sample-only/empty archives must not show an invented current book or counts (Task1).
2. Imported book at its first page must not show a fabricated chapter/ETA; malformed progress must never render NaN (Task1).
3. Persian long titles at 320dp/200% font must retain a reachable resume action without text baked into art (Task2).
4. Home behind reader/settings/archive, backgrounded app or reduced motion must not keep its scene loop running (Tasks2–3).
5. `gameVisible=false` and notes navigation must preserve real routes, avoid a duplicate dock, and never accidentally open an unrelated world destination (Task3).

## Baseline and isolation

Product baseline: `be4df10c28a67f93abd25ca5d42c8c711d157158`, #556, tree `c7cac4ca68de564afeae2a8398be384509ca36a8`. Its inspected receipts are 1,042 unit/177 emulator Android tests, zero failures/errors/skips; unchanged 174 lint warnings; emulator performance smoke green. They are not receipts for new Home code or the accepted phone installation.

Use `using-git-worktrees` at execution. Resolve #556 again; create an isolated feature worktree from that exact candidate, bringing only the approved design/plan documents as necessary. If its head or main moved, inspect the delta first. Do not wholesale merge #542 (observed visual RED at `84cb9ae9183e2534be4cbe9787a9004539b7edc2`) or station experiments #545/#546. Reuse ideas/assets only after source/provenance inspection.

Inventory JDK/SDK/adb before acquiring missing sandbox tooling. Derive proxy settings within the active command if needed; do not commit sandbox network/compiler workarounds. Record local failures separately from normal configured CI results. Preserve exact source promptly.

## File responsibilities

Paths below start at `app/src/main/java/com/veilreader/app/` unless fully qualified.

| File | Responsibility |
|---|---|
| `ui/screens/StationHomeModel.kt` | Pure imported-book statistics and safe presentation model; reuses Threshold selection |
| `ui/screens/StationScenePolicy.kt` | Pure visibility/lifecycle/reduced-motion decision |
| `ui/screens/StationScene.kt` | Bounded illustration layers, lifecycle observation and optional subtle motion |
| `ui/screens/StationHomeScreen.kt` | Adaptive live header/hero/recent/statistics/notes controls |
| `feature/VeilFeatureGates.kt` | New release-disabled STATION_HOME capability; review allowance only |
| `ui/VeilApp.kt` | Dedicated-QA gate and existing route callbacks, actual visibility/game setting |
| `app/src/debug/java/com/veilreader/app/ui/review/GrayfogReviewFixtures.kt` | Honest station review surfaces with existing data fixtures |
| `app/src/main/res/drawable-nodpi/station_home_phone.webp`, `station_home_wide.webp` | Original text-free scene artwork with documented provenance |
| `app/src/main/res/values/strings.xml`, `values-fa/strings.xml` | Localized product labels and semantic descriptions |
| `app/src/test/.../Station*Test.kt`, existing Android shell test | Model, callback, lifecycle-policy and native layout/capture tests |
| `docs/design/station-home/ASSETS.md`, `EVIDENCE.md` | Actual generated-asset provenance and exact candidate verification |

`ReadingNowScreen.kt`, `ThresholdModel.kt`, current Library retrieval/search and shared dock routing stay unchanged in this slice.

### Task 1: Honest station data and gated availability

**Files:** Create `StationHomeModel.kt`; extend `VeilFeatureGates.kt`; add `app/src/test/java/com/veilreader/app/ui/screens/StationHomeModelTest.kt`; extend `app/src/test/java/com/veilreader/app/feature/VeilFeatureGatesTest.kt`.

**Interfaces:**
- Consume existing `buildThresholdSnapshot(books: List<Book>, recentLimit: Int = 5): ThresholdSnapshot` and `Book` from `domain/Models.kt`.
- Produce `internal data class StationArchiveSummary(val all: Int, val inProgress: Int, val completed: Int, val favorites: Int)`.
- Produce `internal data class StationHomeModel(val hero: Book?, val recent: List<Book>, val archive: StationArchiveSummary, val heroProgress: Float?, val heroChapter: String?)` and `internal fun buildStationHomeModel(books: List<Book>): StationHomeModel`.
- Extend existing `VeilRiskyFeature` with STATION_HOME. `releaseEnabled(STATION_HOME)` is false; `enabled(STATION_HOME, debugReview=true)` is true, without changing another feature's behavior.

- [ ] Establish baseline: `./gradlew :app:testDebugUnitTest --tests com.veilreader.app.ui.screens.ThresholdModelTest --tests com.veilreader.app.ui.screens.ThresholdCompositionTest --tests com.veilreader.app.feature.VeilFeatureGatesTest`. Expected: BUILD SUCCESSFUL and zero failed XML cases.
- [ ] Add failing model cases: sample-only → null hero, empty recent, all counts0; imported unfinished positive-progress book → hero/recent selection matches Threshold; finished-with-zero counts completed; favorites includes only imported favorites. Duplicate identities are assumed unique as in canonical library; do not create a new deduplication policy.
- [ ] Add failing presentation cases: unopened progress0/lastOpened0/null locator → null progress and chapter; opened first-page book with confirmed lastOpened → finite0 progress; NaN/infinities → null progress; finite out-of-range → existing display clamp0..1. Show a chapter only when opened and its trimmed label is nonempty and not the legacy `Not started` sentinel. No ETA field exists in this model.
- [ ] Add failing gate assertions: release false, ordinary debugReview=false false, review true true; existing features unchanged. Run scoped tests and inspect the intended assertion failures.
- [ ] Implement the pure model/gate. Statistics use imported books: completed=`finished`, inProgress=`!finished && finite progress>0`, favorites=`favorite`, all=imported count. Reuse Threshold ordering rather than creating another resume selection policy.
- [ ] Rerun scoped tests and `:app:compileDebugKotlin`. Expected: BUILD SUCCESSFUL, all added/existing cases green. Commit `feat(home): model honest station library data behind review gate`.

### Task 2: Layered native scene and responsive interactions

**Files:** Create scene policy, scene and Home screen files from the map, `StationScenePolicyTest.kt`, `StationHomeInteractionTest.kt`, `StationHomeRenderTest.kt`; add scene assets/ASSETS.md and localized strings; extend debug review surfaces.

**Interfaces:**
- Consume Task1 `StationHomeModel` and existing cover/typography/theme components; reuse public `BookCover` from `ui/screens/Common.kt` and its asynchronous decode-lease helper. Copy its existing parameters/defaults rather than adding a second cover loader.
- Produce `internal fun stationMotionEnabled(homeVisible: Boolean, lifecycleResumed: Boolean, reducedMotion: Boolean): Boolean` (true only for visible, resumed, non-reduced state).
- Produce `@Composable internal fun StationScene(homeVisible: Boolean, modifier: Modifier = Modifier)`; observe lifecycle and existing reduced-motion local internally.
- Produce `@Composable internal fun StationHomeScreen(books: List<Book>, homeVisible: Boolean, onOpenBook: (Book) -> Unit, onOpenLibrary: () -> Unit, onOpenArchive: () -> Unit, modifier: Modifier = Modifier)`; derive Task1 model and own no persistence/navigation state.

- [ ] Add failing policy truth-table test over all eight visibility/resumed/reduced combinations. Expected: exactly one true combination.
- [ ] Add real Compose callback tests: resume and selected recent book dispatch their exact IDs once per click; Library and Notes dispatch their own callbacks; empty archive exposes Library but no resume; statistics have no click semantics. Use callback recorders and localized nodes, not screenshot OCR.
- [ ] Add native Robolectric SDK35/GraphicsMode.NATIVE render tests at EN320dp100%, FA320dp200%, EN390dp100%, FA390dp200%, tablet and short landscape. Use existing artwork-idle settling conventions. Assert primary actions scroll into view, >=48dp targets, selected IDs/progress semantics and full labels in the semantic tree. Record genuine captures; clipping/beauty requires visual inspection, not semantics alone.
- [ ] Run these tests before implementation and inspect intended missing-behavior failures.
- [ ] Inspect `grayfog_threshold_v1.webp` and existing asset provenance first. Use `imagegen` for original text-free phone/wide station art based on the supplied reference; read that skill before generation. No UI labels, covers, percentages, phone shell or brand sign lettering baked into images. Inspect actual generated files visually before accepting; preserve reference SHA and actual generation provenance in ASSETS.md. Do not claim exact visual fidelity from prompts alone.
- [ ] Implement adaptive Compose content: rich station header/backdrop; brass-framed hero; title/author; safe progress/chapter only when provided; main resume action; recent cover rail with See All; non-clickable real statistics and one explicit Library action; Notes action to the callback. At narrow/large-font sizes stack cover and text and allow labels to wrap. Keep all text live and localized. Do not add a second dock, world button or fake category filter.
- [ ] Implement bounded scene loading and lifecycle policy. Decode only the active aspect-ratio scene at an appropriate target, capped at1536px longest edge; do not preload both phone/wide full-resolution scenes. Fog/light may use lightweight native drawing; animation is optional until measurements pass. When hidden, omit owned scene content and cancel its jobs; release owned references without unsafe bitmap recycling. When reduced motion, render the static scene with no perpetual animation. No audio in this slice.
- [ ] Add debug surfaces STATION_ACTIVE, STATION_PERSIAN_LONG and STATION_EMPTY to the existing capture switch, calling the same production composable. Fixture mode remains labelled synthetic and may force reduced motion for deterministic still captures. Add a separate live-lifecycle test; do not treat static fixtures as animation evidence.
- [ ] Rerun scoped tests and `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`. Expected: BUILD SUCCESSFUL, zero failed/skipped cases. Inspect all native captures against the reference's depth, brass/light balance and legibility; fix plain-card or clipped output before calling the visual slice acceptable. Commit `feat(home): render layered station with accessible native controls`.

### Task 3: Wire real QA Home, verify return and preserve release behavior

**Files:** Modify `ui/VeilApp.kt`; extend `app/src/test/java/com/veilreader/app/feature/VeilFeatureGatesTest.kt`, `app/src/test/java/com/veilreader/app/ui/VeilShellPolicyTest.kt` and `app/src/androidTest/java/com/veilreader/app/ui/screens/GrayfogShellAccessibilityTest.kt`; create `app/src/test/java/com/veilreader/app/ui/screens/StationHomeVisibilityTest.kt`; write EVIDENCE.md from observed results. No workflow changes merely to bypass a gate.

**Interfaces:**
- Consume Task2 `StationHomeScreen` exactly as declared.
- Select the station only through `VeilFeatureGates.enabled(STATION_HOME, debugReview = BuildConfig.DEBUG && BuildConfig.FORGE_QA)` in READING. Ordinary builds retain `ReadingNowScreen`; release gate remains false.
- Pass real `books`, existing `requestOpenBook`, `routeViewModel.selectTab(LIBRARY)` and `routeViewModel.openArchive()` callbacks. Do not modify the open-request/session writer contract.
- Compute actual Home visibility from the current route: selected READING, no active reader, settings, Archive or active chamber overlay. Lifecycle resumes separately inside the scene. Existing `visibleVeilTabs(appSettings.gameVisible)` owns dock availability, unchanged.

- [ ] Add failing integration tests proving ordinary/release selection stays original, dedicated QA selects station, Notes opens existing Archive, and Library reaches existing Library. Verify one shared dock and existing PATH/PROFILE reachability, with gameVisible both true and false.
- [ ] Add failing live lifecycle/visibility tests: visible→hidden→visible and RESUMED→STOPPED→RESUMED stop/restart at most one scene job; reduced motion never starts one; opening reader/Archive/settings stops the scene. Test actual observer/disposal effects in addition to the pure policy.
- [ ] Wire the gate and callbacks; run scoped tests and actual imported-book open→read→close→Home integration. Expected: original locator retained and no scene-owned write. An opening error uses the existing host error path and leaves recovery possible.
- [ ] Run `sh tools/test-policy.sh`, `sh tools/verify-canonical-paper.sh`, `python tools/test_canonical_paper_guard.py`, then full `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`. Expected:39 policy checks,6 canonical guard tests, BUILD SUCCESSFUL; inspect XML and compare lint against baseline, not just task exit codes.
- [ ] Run existing storage instrumentation and performance workflows on the exact candidate. Inspect workflow invocation contracts before execution. Expected: native connected tests, profile packaging, absolute and baseline-relative emulator budgets pass. Do not turn emulator metrics into physical smoothness acceptance.
- [ ] Build the separate QA installation using existing `.github/workflows/forge-qa-apk.yml` contract and `-PveilForgeQa=true -PveilForgeRevision=<candidate SHA>`. Preserve its cached/pinned signing identity. If the key/capability is unavailable, record that blocker; never uninstall accepted builds or invent a successful QA APK. Verify package `com.veilreader.app.forgeqa`, signature and SHA256.
- [ ] Inspect a physical phone at EN/FA320/390dp100/200%, TalkBack, reduced motion, background/foreground and repeated reader return. Existing `performance/budgets.json` physical budgets apply: startup median<=600ms, frame-overrun p95<=0ms/p99<=8ms and CPU frame p95<=16.7ms/p99<=24ms. These are acceptance targets, not current station measurements. Keep unavailable tests UNVERIFIED and keep the branch draft; actual accepted phone APK identity remains a separate prerequisite for comparative claims.
- [ ] Record source/build/asset IDs, captures, tests, failures and deferred gates in EVIDENCE.md. Obtain one fresh whole-branch source review, address Important/Critical findings with regression tests, and publish a draft implementation PR. Commit `feat(home): integrate gated station and record verified review evidence`. No main merge or public release follows automatically.

## Self-review and handoff

Coverage: Task1 owns data truth/gate, Task2 owns art/layout/actions/motion, Task3 owns actual routes/visibility/integration/QA identity. All five Review Focus inputs have owning tests. Model and screen interfaces agree; no undefined scene activation flag or category-route promise remains. Existing scope excludes new Library filtering, 3D world, chapter/ETA computation and a navigation redesign.

Recommended method: preserve the prior **Native** sequential approach. Tasks share model/gate/UI integration contracts, so implement them in order with one independent whole-branch review. The owner must review this newly written plan before product edits; approval of the preceding visual reference did not approve a plan that did not yet exist.
