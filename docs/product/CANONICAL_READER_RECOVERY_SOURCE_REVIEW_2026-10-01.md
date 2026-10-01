# Canonical Reader recovery and accessibility source review

Date: 2026-10-01
Base reviewed: `2f2802aabdbdc910509d72a888608a66be17da65`
Branch: `alpha/w26-arena-canonical-ui-rebuild-v1`
PR: #349, OPEN / DRAFT
Status: SOURCE-REVIEWED; reconstruction remains active.
Review extended through canonical head `e9b0ee7ccc87d186283b208276d2d34a8f468b4e` for W50 adaptive Reader controls.

This is a focused W39–W47 follow-up, not full-app or release certification.
The governing single-branch contract remains unchanged.

## Source findings

- PDF renderer discovery stops after 40 probes at 100 ms intervals. Retry restarts that bounded effect using a generation counter; navigator replacement/disposal cancels the old effect. The ongoing zoom mirror is a separate effect on the discovered PDFView, not renderer discovery.
- PDF layout callbacks copy only `scroll`. They preserve the retained EPUB `pageTurnStyle`. Paper/Slide overlays explicitly require EPUB and non-scroll presentation in ReaderScreen. The existing preference mapping tests protect EPUB mode independence.
- Missing Library records call the owned `bookOpenFailed(targetId, openRequestId)` path and show resource-based recovery copy instead of leaving the open route waiting.
- Image-viewer caption fallback, pan clamping and decode sampling have existing regression tests. Those Android/Kotlin suites were inspected, not executed locally.
- Reading mode, Page turn, spread, Advanced Appearance, Settings dense choices and Reader Notebook rails now use the existing dense-choice policy against measured container width rather than whole-screen width. PDF layout choices use the same measured-width rule.
- VeilMicroLabel detects Arabic-script labels and avoids Latin uppercase/tracking. PDF layout choices retain that primitive and localized Page/Continuous semantics.
- Shell rail width and RTL transition direction have existing policy tests. Canonical chamber SavedState restoration/exclusivity is covered by VeilAppViewModelTest.
- Sensory execution already checks foreground state and audio volume. This change adds no sensory layer or decorative Reader animation.

## Changes

- PDF Page/Continuous controls reuse `shouldStackDenseChoices` with the actual available panel width and current density font scale. Narrow panels and 200% text stack the choices; ordinary widths retain the existing inline presentation.
- Both layouts share one private PDF choice renderer, radio-button semantics, localized descriptions and a minimum 72dp target.
- Renderer connecting/delayed copy has a polite live region so the transition to explicit recovery can be announced without moving focus.
- Added a PDF preference round-trip matrix across Paper, Slide and None, checking both retained decoration and PDF fit/axis mapping.
- Reader Notebook tabs and bookmark/note action rails no longer depend on horizontal scrolling; narrow containers and large text stack controls at full width.
- Settings dense choices and Reader Appearance selectors now derive breakpoints from their actual available width. This covers constrained dialogs, tablet split layouts and foldable/windowed surfaces without changing preference semantics.
- PDF zoom header now stacks at narrow measured width or large font scale. Zoom behavior itself is unchanged.

No persistence, schema, reader-session, renderer, navigation transaction or zoom behavior was changed.

## Verification evidence

Locally executed:
- `sh tools/test-policy.sh`: PASS, 39 reading-policy checks.
- `python3 -m unittest discover -s tools -p 'test_performance*.py'`: PASS, 15 tooling tests. These are parser/dashboard/delta tests, not runtime benchmarks.
- `git diff --check`: PASS.

NOT EXECUTED locally:
- Kotlin/Android unit suite, including the new PDF matrix (Gradle and Android SDK are not provisioned in this workspace).
- compile, lint, APK/AAB packaging, instrumentation, device visuals, TalkBack, Persian/RTL runtime checks and 200% text runtime checks.

GitHub evidence checked during this review applies only to the base SHA above:
- Android CI `36836683163`: actual checkout/setup/policy/unit/schema/lint steps succeeded; debug assemble was in progress at the latest observation.
- Storage Instrumentation `36836683165`: actual setup/KVM steps succeeded; Room instrumentation remained in progress.
- Performance Benchmarks `36836683057`: actual dependency/history/KVM steps succeeded; profile/benchmark smoke remained in progress.

These runs are executing useful steps, not empty runner-allocation failures. They do not verify these new edits and do not establish BUILD GREEN, DEVICE GREEN or RELEASE READY.

## Remaining review targets

1. Device-inspect Reader Notebook tabs/action rails with long Persian labels at 200% text. Source layout is now measured-width and scroll-free, but runtime fit is not yet certified.
2. Device-inspect Settings/Reader measured-width breakpoints on phone, tablet, foldable and constrained dialog surfaces. Source-level screen-width dependency has been removed from the reviewed controls.
3. PDF zoom header/reset controls still need device inspection at narrow widths and 200% text; the header is now adaptive, but source review alone cannot prove the three-button zoom rail on every device/font combination.
4. Continue selection/live-preview/previous-location/chapter-boundary interaction review. Process-recreation ownership already has SavedState/checkpoint regression coverage; this focused pass still does not certify the complete interaction matrix.
5. Full-app visual reference fidelity and release gates #261/#262/#263 remain open. Keep #349 Draft.

## W50 follow-up — relayout continuity, Room history, and emulator determinism

Canonical implementation reviewed immediately before this documentation stamp: `438c46f31e247939139ea742b3f83dd58665ff04`.

### Reader continuity hardening

- Readium Kotlin Toolkit 3.4.0 still has upstream issue `#761`: EPUB preference-driven relayout can leave `currentLocator` stale. Upstream PR `#819` fixes this by notifying progression after content-size changes, but that fix is not yet part of the dependency used by Veil.
- Veil now records a compatibility checkpoint before a reflow-sensitive EPUB preference change. The durable anchor is selected from Readium's publication positions using the existing renderer `currentLocator.locations.position` first, with total progression as a fallback.
- `firstVisibleElementLocator()` is used only for its precise DOM `cssSelector`; that selector is merged into the stable position anchor instead of replacing the position signal. This matters because the Readium 3.4.0 implementation returns selector/text context there, not publication position metadata.
- While relayout is pending, an exact stale pre-change locator emission is suppressed and close/background final-snapshot paths cannot overwrite the compatibility anchor. After renderer settling, Veil asks for the first visible locator again and records the refreshed precise location when available.
- The position-selection policy is factored into a JVM-pure helper so regression tests do not instantiate Readium `Url`/Android `Uri` objects.
- Native EPUB selection ActionMode cleanup is unconditional: renderer/selection-query failure cannot strand the Android selection toolbar.

### Room migration history

- Room schema history is now committed for versions `1`, `2`, and `3`.
- `2.json` was generated by running Room KSP against historical database-version-2 commit `cf5d523ff61be5a5823c041be8b1573e58667699`; `3.json` was generated from the canonical version-3 source.
- The one-shot recovery workflow completed successfully and removed itself after committing the generated schemas. This closes the earlier failure mode where migration tests could not open historical schema assets.
- The committed schema history is generated evidence, not a hand-authored reconstruction.

### Emulator gate hardening

- Storage and performance workflows no longer delegate lifecycle ownership to the opaque emulator-runner step.
- `tools/run_android_ci_emulator.sh` now owns AVD creation and teardown, uses the API-35 x86_64 image with the `lavapipe` software renderer, and refuses to start tests until the emulator process remains alive, `system_server` is stable across consecutive probes, core package/activity/settings services answer, and `/data` has at least 512 MiB free.
- Gradle instrumentation runs with `--no-daemon --max-workers=2 --no-parallel`, and emulator/logcat diagnostics are retained on failure.
- Real test assertions still fail the gate. The hardening separates infrastructure death from product regression; it does not convert failures into passes.

### Verification state after this follow-up

- Room Schema Recovery run `36852781796` completed the schema-generation, artifact, commit, and cleanup steps successfully.
- Runs created directly on the schema bot commit concluded `action_required` with no jobs, so they are explicitly **not** treated as verification evidence.
- This documentation commit intentionally advances the canonical branch through the connected GitHub actor so Android CI, Storage Instrumentation, and Performance Benchmarks can execute normally against the final schema-bearing source.

Until those final-head gates complete, keep PR #349 Draft and do not claim BUILD GREEN, STORAGE GREEN, PERFORMANCE GREEN, DEVICE GREEN, or RELEASE READY.

## W51 follow-up — selection-note atomicity and no-op jump integrity

Canonical implementation reviewed through head `2f0236d497d83855006e26c982c782a26b68d5f5`.

### Selection → Note transaction

- The native EPUB selection toolbar still uses Readium's selection ownership; Veil only adds Highlight and Note actions.
- A fresh Note action no longer becomes a durable/credited highlight before the note is confirmed.
- A newly created highlight is treated as draft-owned state while the Note dialog is open. Cancelling or dismissing the draft removes that temporary highlight and flushes the deletion through the repository write barrier.
- Existing highlights are never deleted by cancelling a Note edit.
- A fresh Note requires non-blank note text before Save is enabled. An existing highlight may intentionally save an empty annotation, preserving the highlight while clearing its note.
- Session/game highlight credit is emitted only after the new Note transaction has durably saved. Note credit remains separate, preventing abandoned drafts from inflating reading history or progression.
- Pure policy regression coverage now locks draft-discard and save-eligibility behavior.

### Previous-location and saved-location jumps

- Location-backed programmatic jumps now compare the current persisted locator JSON with their target before opening a navigation transaction.
- A same-location Previous Location action collapses the stale return affordance instead of asking Readium to perform a no-op jump.
- Reader Notebook returns to the already-current saved location without starting a transaction or recording a false revisit.
- This closes the failure mode where Readium could accept a no-op `go()` without publishing a locator, leaving the transaction alive long enough for the user's next real page turn to be misclassified as the delayed jump destination.
- The pure location-jump policy is covered for same-target, missing-origin and missing/blank-target cases.

### Verification boundary

Source-level review and regression tests were added, but this follow-up does **not** establish a compiled or device-verified result by itself. The final canonical head still requires its normal Android CI, storage/instrumentation and performance gates. Manual APK/AAB packaging was not requested and was not started.

Keep PR #349 Draft until those gates and later visual/device inspection are complete. Do not claim BUILD GREEN, STORAGE GREEN, PERFORMANCE GREEN, DEVICE GREEN or RELEASE READY from this source review alone.

## W52 follow-up — overlay ownership, appearance settle, and terminal-boundary behavior

Canonical implementation reviewed through head `0078d0d9588ad9674c17adaa6bd255055e391fbc`.

### Selection and overlay ownership

- Reader-owned chrome actions now dismiss Readium's native EPUB selection before opening Notebook, Appearance/PDF View, or performing a page Bookmark action.
- The selection callback retains the exact active Android ActionMode, clears Readium selection first, and then finishes that owned toolbar even when renderer teardown races the overlay transition.
- This prevents a native selection toolbar or selection handles from floating above a Veil modal surface and keeps gesture ownership unambiguous.

### Appearance close and renderer failure

- The Appearance chamber now receives the final local draft on Done and waits for that exact effective appearance to become the presented renderer state before closing.
- A final slider commit gets an explicit propagation frame before close eligibility is evaluated.
- The wait has a defensive two-second ceiling; a renderer fault cannot trap the user in Appearance.
- Renderer preference submission now fails calm: relayout-temporary state and handoff imagery are cleared, the last successfully presented appearance remains on screen, and a localized recovery message is shown instead of allowing a preference exception to escape the Reader effect.
- English and Persian recovery copy were added.

### First/last-page boundary ownership

- Static Paged directional edge taps and keys now remain consumed when Readium reports that navigation cannot move beyond the publication boundary.
- The boundary still emits the existing throttled visual/sensory response, but the failed navigation no longer falls through to the center-tap chrome handler and unexpectedly opens Reader controls.
- Paper Curl and Slide were source-reviewed in the same pass. Their edge taps/drags already consume the handled boundary path and did not require this change.
- Android contract coverage now verifies that a failed directional page turn is consumed and emits exactly one boundary callback.

### Verification boundary

Pure JVM regression coverage was added for Appearance close synchronization. Android instrumentation coverage was extended for terminal directional ownership. These source changes still require the final-head Android CI, Storage Instrumentation and Performance gates before any GREEN claim. No manual APK/AAB build was started.

## W53 follow-up — preference rollback, spread continuity, and resize-safe paper

Canonical implementation reviewed through head `c72581e7d31c6f78c80c5520f06ab6884f6bbbfe`.

### Renderer-truthful preference state

- Reader now tracks the last renderer-accepted raw Appearance separately from the effective visual Appearance.
- If preference submission fails, the persisted/global Appearance callback is rolled back to the last accepted value instead of leaving Settings state ahead of the actual renderer.
- Fixed-layout spread state receives the same rollback discipline: failed `AUTO/SINGLE/DUAL` submission restores the last presented spread and writes that rollback through the existing settings path.
- This preserves retained global preferences such as Scroll while a fixed-layout publication temporarily forces an effective paged presentation.

### Fixed-layout spread continuity

- A fixed-layout spread change now receives a Reader handoff snapshot even when the navigation mode itself is unchanged.
- The current locator is committed as a non-page-turn checkpoint immediately before spread submission and again after the renderer settles.
- Spread changes receive the same two-frame settle budget as a navigation-mode transition, reducing one-frame `SINGLE ↔ DUAL` flashes while keeping the publication location durable.
- Regression coverage locks both the handoff decision and the heavier settle budget.

### Session-scoped Fragment ownership

- Navigator Fragment tags now include both book id and Reader session instance id.
- Reopening the same book under a fresh open request cannot accidentally identify an older navigator Fragment as the current one merely because the book id matches.
- Configuration/process restoration keeps the same session id and therefore keeps deterministic identity, while a genuinely fresh session receives a distinct tag.
- Regression coverage locks this ownership rule.

### Paper geometry and adaptive resize

- Curl line intersection now rejects non-finite inputs, degenerate vectors, and nearly parallel lines using a relative geometric tolerance before coordinates can explode into GPU clip/shadow artifacts.
- Ordinary intersections remain exact under regression coverage.
- Paper Curl drawing now refuses transient zero-size/non-finite frames and falls back to ordinary content.
- Reader parchment atmosphere likewise refuses transient invalid viewport dimensions before fibre generation performs modulo arithmetic.
- This specifically hardens orientation changes, split-window resizing, tablets and foldable transitions where a layout may briefly report zero width or height.
- JVM regression coverage protects both curl-frame and Reader-material viewport gates.

### Verification boundary

This remains source-level hardening plus regression coverage. The final canonical head still requires Android CI, Storage Instrumentation and Performance Benchmarks. No APK/AAB packaging was started.

## W54 follow-up — reduced motion and resize-owned locator accounting

Canonical implementation reviewed through head `c42a1f45c9140cfbc91a5ca425250f67c413e1fb`.

### Reduced-motion Paper contract

- Paper mode no longer captures a publication bitmap when reduced motion is enabled.
- Tap turns and drag turns continue to navigate and commit normally, but the lifted-sheet animation path is bypassed.
- Terminal Paper boundaries keep their semantic boundary feedback while the visual bounce is omitted under reduced motion.
- This removes a previously inconsistent case where a first/last-page tap could still animate a Paper boundary despite reduced-motion preference.
- Regression coverage locks snapshot ownership off under reduced motion.

### Mid-gesture adaptive resize

- Reader tracks the measured viewport and cancels any active Paper or Slide preview when the viewport dimensions change after initial measurement.
- A cancelled preview restores its drag-start locator before the resized surface continues, preventing stale snapshots from stretching across orientation, split-window, or foldable transitions.
- Reader mode-handoff imagery is dropped when dimensions change so an old-size continuity bitmap cannot be stretched over the new viewport.
- Regression coverage verifies that initial measurement is ignored, an active preview is cancelled on size change, and ordinary resize without a preview remains non-disruptive.

### Resize relayout accounting

- Viewport changes open a short relayout window because Readium may emit a new locator solely from reflow.
- Locator emissions during that window are committed as `FINAL_SNAPSHOT` checkpoints and never count as page turns.
- Paper, Slide, static Paged and directional navigation explicitly mark real user interaction, immediately cancelling the resize-relayout suppression before user navigation.
- The final post-resize locator is sampled after a quiet window and committed as a non-page-turn checkpoint.
- The locator event decision is now a pure policy with regression coverage, including the rule that an explicit programmatic jump still outranks resize classification.
- This prevents split-window/foldable reflow from generating false XP/page-turn history while preserving real navigation immediately after resize.

### Verification boundary

The current head still requires Android CI, Storage Instrumentation and Performance Benchmarks. No manual APK/AAB packaging was started.

