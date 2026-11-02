# Reader bottom chrome: minimum touch-width follow-up

**Date:** 2026-11-02
**Scope:** one-line Compose sizing change in `ReaderScreen.kt` (`ReaderControl`), proposed as a DRAFT only.
**Source branch:** `agent/antigravity-reader-control-touch-target-20261102`, based on canonical `main`.
**Reason:** The previous isolated Antigravity UX worktree (`agent/antigravity-ux-20261012`) established a candidate `defaultMinSize(minWidth = 48.dp, minHeight = 56.dp)` in `ReaderControl` to replace an explicit zero minimum-width floor. This branch transplants only that narrow candidate, not the old worktree or unrelated local changes.

## Source-level boundaries

- Preserves the weighted four-control bottom row, label rendering, active/inactive colors, click behavior, the Focus state description, navigation, and Reader gesture/locator owners.
- No new permission, package, dependency, storage schema, account, network, analytics, asset, PDF behavior, engine fallback, or release work.
- A 48dp *minimum* in source is not, by itself, evidence of effective touch bounds or a passing 200% text layout; weighted Row constraints may affect the actual geometry.
- Do not merge or label product GREEN solely because the unit test / Kotlin compiler passes.

## Prior independent baseline evidence (NOT exact-head acceptance)

The isolated desktop worktree at `ff622f4fd` compiled with the same one-line candidate; on 2026-11-02 the command
`./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests '*ReaderMenuSettingsTest' --offline --no-daemon --console=plain`
reported `BUILD SUCCESSFUL in 10m 3s`. Its XML indicated 1 test, 0 failures, 0 errors. That baseline test checks a Reader menu setting; **it does not assert touch dimensions**. The current branch is based on the newer canonical `main` and requires its own exact-head validation. The desktop subsequently went offline before any new device evidence could be captured.

## Required acceptance matrix (HOLD)

1. **Identity/CI:** immutable PR head SHA must equal checkout SHA; verify Android compile, targeted tests, and complete job results, not synthetic merge-only checks.
2. **360dp and narrower portrait:** each of the four bottom controls stays visible, distinct and tappable; no overlapping hit regions. Test at 320dp where feasible.
3. **Font scales 1.0 / 1.5 / 2.0:** Persian and English labels must not overlap, wrap into adjacent controls, or become impossible to understand. Test landscape and tablet split-screen too.
4. **TalkBack:** all four controls are focusable, have correct spoken names, disabled state when applicable and correct Focus on/off state. Confirm adjacent-target discrimination.
5. **Directionality:** RTL app UI, LTR and RTL EPUB content, and PDF show correct labels/order and do not interfere with page-turn gestures.
6. **Mode isolation:** PAGED / PAPER CURL / SLIDE / SCROLL still navigate independently. The change must not claim optical Paper acceptance.
7. **Screenshots/video:** capture exact APK SHA, emulator/device model, density, width, font scale, language, and before/after visual evidence. Record any clipping as RED; unknown as BLOCKED.

## Merge / rollback

- **Status: DRAFT / HOLD**, until exact-head Android CI and screenshot/a11y review prove the target. Prior worktree changes remain untouched.
- Rollback is to revert the single `ReaderControl` change in this PR. Do not reset or rewrite `main`, and do not copy experimental AI Studio web-prototype code into the native Reader without a separate parity review.
