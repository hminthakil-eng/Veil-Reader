# Reader Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce ReaderScreen coupling through three behavior-preserving extractions, with unchanged publication, input and durable-session ownership.

**Architecture:** Keep the existing `com.veilreader.app.ui.screens` package and internal APIs. Move renderer preference mapping, reader control components and appearance components into focused files; keep Android lifecycle, navigation, jobs and progress ownership in their existing hosts. Do not add a second coordinator or new dependency.

**Tech Stack:** Existing Kotlin/Compose/Readium 3.4.0, Room/DataStore, JUnit/Robolectric and Android instrumentation. Use the repository's pinned wrapper and declared JDK 21/SDK 37 requirements.

**Spec:** `docs/superpowers/specs/2026-10-18-reader-rebuild-design.md` (approved by owner with “Go on” on October 18).

## Global Constraints

- The first subsystem changes no Room schema, backup format, file location or application identity.
- Readium remains the sole pagination authority; Android views stay lifecycle-bound.
- Four distinct modes; no silent substitution; report unavailable capability explicitly.
- Preview views never write progress.
- No force pushes, branch deletion, bulk merges, app-data resets or public release as part of design approval.
- Keep the current Android technology and dependency versions until a separately justified upgrade passes checks.
- Existing repository budgets remain authoritative; do not relax them to produce a pass.
- Extend existing ReaderViewModel and writer leases; one writer per active generation.

## Review Focus

1. Malformed font scales: NaN and infinities retain finite fallback; out-of-range values retain the current 0.75–1.8 clamp (Task 1).
2. PDF with hidden EPUB preferences: page-turn/theme changes must not alter PDF fit, spacing or scroll mapping (Task 1).
3. Disabled Paper capability: disabled control must not invoke its callback or claim Paper is available (Task 3).
4. Persian layout at 200% font: appearance controls remain reachable and semantically selectable at 320dp (Task 3).
5. Appearance changes during interaction/close: existing input fencing, locator continuity and durable close behavior stay intact (Tasks 1–3 existing suites and final device gate).

## Source and delivery boundaries

Inspected implementation base: main `811f0d7d521fc11f110638a100c315f698d52fd5`. Design publication: `1ecfbd048cdb981dd1008a2f750bd0edaf202711`, PR #554. The plan is documentation only and does not assume the design branch's local and remote commit IDs match.

At execution time, use `using-git-worktrees`, recover the exact remote design/plan head, and create a dedicated implementation branch. Re-resolve main and assess changes if it moved. Do not incorporate #509/#542/#546 wholesale. This extraction preserves main's existing behavior; the owner's accepted native-paper APK remains a separate comparison target until its identity is established. Lack of that identity blocks claims that the extraction improves or preserves that particular installation, not source-level refactoring verification.

Prepare JDK 21/SDK 37 only after inventorying the sandbox. Missing Android toolchains are a setup requirement, not permission to replace Android checks with Python tests. Use repository Luna exact-transport policy if capabilities cannot be supplied locally. No device or Android build result is claimed by this plan.

### Task 1: Separate renderer preference mapping

**Files:** Create `app/src/main/java/com/veilreader/app/ui/screens/ReaderRendererPreferences.kt`; modify `ReaderScreen.kt` in that directory; extend `app/src/test/java/com/veilreader/app/ui/screens/ReaderPreferenceMappingTest.kt` only where coverage is missing.

**Interfaces:** Preserve existing signatures/defaults/annotations for `ReaderAppearance.toEpubPreferences(fixedLayoutSpread: ReaderFixedLayoutSpread = AUTO): EpubPreferences`, `ReaderAppearance.toPdfiumPreferences(): PdfiumPreferences`, `readiumFontSizeRatio(scale: Double): Double`, `ReaderFixedLayoutSpread.toReadiumSpread(): Spread?`, `readiumThemeColors(theme: ReaderTheme): Pair<Int, Int>`, `readerHighlightTint(theme: ReaderTheme): Int`. Preserve `readerPreferencesNeedSubmission(...)`, `readerRendererPreferencesChanged(...)` and `shouldAwaitReaderAppearanceClose(...)` exactly, copying their current signatures rather than inventing wrappers.

- [ ] Run existing characterization suites before edits: `./gradlew :app:testDebugUnitTest --tests '*ReaderPreferenceMappingTest' --tests '*ReaderPreferencesTest' --tests '*ReaderRelayoutContinuityPolicyTest' --tests '*ReaderModeHandoffPolicyTest'`. Require BUILD SUCCESSFUL and zero failed XML cases; diagnose any baseline failure before extraction.
- [ ] Add missing characterization cases to `ReaderPreferenceMappingTest`: `readiumFontSizeRatio(Double.POSITIVE_INFINITY) == 1.0`, negative infinity likewise; PDF preferences for appearances differing only in `pageTurnStyle` or theme compare equal. Use existing enum/types and JUnit equality assertions. Run the same suite on the old implementation; these are refactor characterizations and should pass without a deliberate behavior change.
- [ ] Move the named functions unchanged into `ReaderRendererPreferences.kt`, together with private `ReaderPreferenceToggle.toNullableBoolean()`. Keep package and internal visibility. Remove originals and now-unused imports. Leave relayout-anchor helpers, renderer creation and timeout constants in ReaderScreen: they belong to ongoing navigation/lifecycle work, not this mapping seam.
- [ ] Rerun the characterization command plus `./gradlew :app:compileDebugKotlin`; verify no renamed signature, changed normalization, preference, color or timeout. Inspect `git diff --color=never --find-renames` rather than relying on line-count reduction.
- [ ] Commit only this extraction and relevant characterizations: `refactor(reader): isolate renderer preference mapping`.

### Task 2: Separate reusable reader controls

**Files:** Create `app/src/main/java/com/veilreader/app/ui/screens/ReaderChromeComponents.kt`; modify the same directory's `ReaderScreen.kt`; use existing `ReaderChromePolicyTest`, `ReaderHardwareAccessibilityTest` and `ReaderListeningModeAccessibilityTest`.

**Interfaces:** Move `internal enum class ReaderAction { BACK, NOTEBOOK, BOOKMARK, FOCUS, APPEARANCE, ZOOM, SEARCH }` and existing `ReaderChromeButton(action: ReaderAction, accessibilityLabel: String, tint: Color = VeilPalette.Brass, onClick: () -> Unit)`. Move `ReaderControl(...)` preserving all current parameters/defaults, widening only from private to internal because ReaderScreen calls it. Keep `ReaderActionIcon(action: ReaderAction, modifier: Modifier, tint: Color)` private in the new file.

- [ ] Establish green baseline with `./gradlew :app:testDebugUnitTest --tests '*ReaderChromePolicyTest' --tests '*ReaderHardwareAccessibilityTest' --tests '*ReaderInputArbiterTest' --tests '*ReaderTapOwnershipTest'`. Existing tests are sufficient for this mechanical move; do not add file-layout assertions.
- [ ] Move only these components, preserving composition, modifier order, 48dp icon-button target, minimum 56dp ReaderControl height, content descriptions, RTL back-arrow behavior, enabled callbacks and existing labels. Remove unused imports from ReaderScreen. Keep remembered state and callback ownership at original call sites.
- [ ] Rerun the scoped baseline and `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`. Confirm call sites and public/internal signatures compile unchanged. Queue real TalkBack evidence for final verification; JVM success does not establish accessibility acceptance.
- [ ] Commit: `refactor(reader): isolate chrome control components`.

### Task 3: Separate appearance controls and prove the reader journey

**Files:** Create `app/src/main/java/com/veilreader/app/ui/screens/ReaderAppearanceComponents.kt`; modify `ReaderScreen.kt`; extend existing `app/src/test/java/com/veilreader/app/ui/screens/GrayfogCloudAppearanceTest.kt`; add `app/src/test/java/com/veilreader/app/ui/screens/ReaderModeSelectorInteractionTest.kt` if equivalent callback coverage does not exist.

**Interfaces:** Preserve `EpubAppearancePanel(appearance: ReaderAppearance, fixedLayout: Boolean, fixedLayoutSpread: ReaderFixedLayoutSpread, publicationLanguage: String?, onSpreadChange: (ReaderFixedLayoutSpread) -> Unit, onChange: (ReaderAppearance) -> Unit, onDone: (ReaderAppearance) -> Unit, modifier: Modifier = Modifier, initiallyAdvanced: Boolean = false, readerChromeAutoHideEnabled: Boolean = true, onReaderChromeAutoHideChange: (Boolean) -> Unit = {})`. Preserve exact signatures/defaults for `ReaderReadingModeSelector`, `ReaderPageTurnSelector`, `ReaderMotionPreview`, `ReaderCapabilityNotice`, `localizedReaderMotionSummary`, `localizedPageTurnStyleDescription` and `readerPageTurnStyleEnabled` from source.

- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*GrayfogCloudAppearanceTest' --tests '*ReaderPreferenceMappingTest' --tests '*ReaderTransitionAvailabilityTest' --tests '*ReaderInteractionPolicyDurabilityTest' --tests '*ReaderCloseDurabilityTest'` before changes. Review existing appearance fixture setup/theme/locale to reuse its real Compose harness.
- [ ] Add a Compose callback characterization: render `ReaderPageTurnSelector(selected = SLIDE, paperEnabled = false)` with a callback recorder; the localized Paper semantic node is disabled and callback stays empty after an attempted click; enabled Slide selection calls back with SLIDE. Assert behavior with Compose semantics, not screenshot OCR. This is existing intended behavior and must pass before extraction.
- [ ] Extend the appearance fixture with Persian locale, RTL layout direction, 320dp width and fontScale 2.0. Scroll to the Reading/Page-turn controls, assert controls are reachable and selected semantics match the supplied appearance; no callback changes the reading mode on initial composition. Reuse localized resources and existing capture conventions. Keep physical clipping/TalkBack acceptance separate.
- [ ] Move the complete `EpubAppearancePanel` component and its appearance-only helper chain from `ReaderAppearanceChoiceGroup` through `AppearancePreset` to the new file, retaining private helper visibility. Move the selector/capability/summary functions listed above together. These are semantic declaration boundaries, not blind line ranges. Do not move `toEpubPreferences` back from Task 1. Keep `ReaderFootnote`, focus overlay, fragment host, locator helpers and ETA formatting in ReaderScreen.
- [ ] Preserve current remember/dispose behavior, slider dispatch timing and callback order exactly. The umbrella spec's future preview/apply/cancel redesign is explicitly deferred; do not silently introduce that behavior into this refactor. Remove unused imports and original declarations, without global formatting or unrelated cleanup.
- [ ] Run `sh tools/test-policy.sh` then `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`. Expected: 39 policy checks, BUILD SUCCESSFUL, no test failures, no new lint issues. Verify test XML and artifact creation against this implementation SHA; do not reuse older reports.
- [ ] Run repository instrumentation and performance verification on the exact candidate: `./gradlew :app:connectedDebugAndroidTest`; use `.github/workflows/performance-benchmark.yml` and `.github/workflows/reader-durability-fault-injection.yml` as the existing benchmark/durability invocation contracts. Inspect those workflows at execution time before selecting inputs; do not replace them with guessed commands or claim they ran from compilation alone.
- [ ] On a physical phone, exercise import → resume → each of four modes → appearance change → select/annotate → close/reopen; repeat RTL/LTR, 100 turns per mode, reverse/cancel/chapter edges, rotation/background/process death, 100%/200% font, TalkBack and reduced motion. Match APK hash/signer/source identity. Keep physical status UNVERIFIED where hardware evidence is unavailable; never uninstall the accepted app to force installation.
- [ ] Compare native captures before/after on the same fixtures. Expected visual output is unchanged in this stage. Existing physical startup/frame budgets from the spec remain mandatory for promotion. No 3D/world handoff is added; its 30-round-trip requirement belongs to that future subsystem.
- [ ] Commit: `refactor(reader): isolate appearance controls without changing reader behavior`. Obtain independent source review, publish a draft implementation PR with exact evidence and unresolved gates. No main merge or release automatically follows.

## Coverage and handoff

The first subsystem covers the three surface/mapping seams only. Current input arbiter, writer leases, session generation, mode fences, snapshot epochs, TTS ownership and storage keep their implementations. Home/Library visuals, per-book preference migration, listening quality and stars/world are intentionally later subsystem cycles, as required by the approved umbrella spec. A smaller ReaderScreen is a maintainability outcome, not a user-visible quality certification.

Recommended execution: **Native**, sequentially in this session; all three tasks touch ReaderScreen and share visibility/import boundaries, so parallel implementation would add conflicts. One independent whole-branch review follows. **Subagent-driven** remains an option for separate implementer/reviewer gates per task, with additional context cost. Owner must review this plan and select the execution approach under the invoked Superpowers workflow before product edits.

Self-review: exact existing functions and paths identified; five review-focus conditions assigned; task interfaces preserve source signatures; no second interaction controller, schema migration or dependency changes; characterization-first testing appropriate to behavior-preserving extraction. Android/device runs above are future execution steps, not claimed receipts.
