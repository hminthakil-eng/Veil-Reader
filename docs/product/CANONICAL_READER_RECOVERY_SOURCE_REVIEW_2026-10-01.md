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
