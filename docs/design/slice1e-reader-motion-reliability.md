# Slice 1E — Reader Motion & Page-Turn Reliability

Status: **DRAFT / stacked on Slice 1D**
Parent: `ux/slice1d-pdf-interaction-reliability`

## Goal

Reduce the slide-like feel of paginated EPUB page turns without replacing Readium, breaking Continuous Scroll, or introducing a parallel reader engine.

## Root cause

- Veil uses Readium Kotlin Toolkit `3.4.0`.
- Reflowable EPUB page navigation ultimately uses `R2WebView.scrollLeft/scrollRight(animated = true)`.
- Readium's supported animated page turn is therefore a horizontal smooth-scroll transition.
- Readium 3.4.0 does not expose a public true page-curl renderer or a public easing/duration control for this path.
- The user-reported slide feel is therefore consistent with the current supported navigator behavior, not a stale dependency or missing flag.

## Decision

Keep Readium's supported navigation semantics and add a restrained visual depth layer instead of forking the engine or pretending to provide a physical page curl.

Paginated EPUB motion envelope:

- maximum Y rotation: `3°`
- maximum scale reduction: `0.8%`
- maximum alpha reduction: `3%`
- depth translation: `6dp`
- mirrored hinge direction for forward/back motion
- identity transform at page boundaries

The effect is intentionally subtle: it should make the page feel less like a flat slide while keeping text stable and readable.

## Isolation and accessibility

- Applied only to Readium's reflowable EPUB WebView.
- Disabled and reset when Continuous Scroll is enabled.
- PDF behavior is untouched.
- Navigation animation follows `ValueAnimator.areAnimatorsEnabled()` so reduced/system-disabled animation avoids forced animated transitions.
- The pure motion curve lives outside `ReaderScreen` and is unit tested.

## Readium boundary

A direct Kotlin reference to `R2WebView` failed because the class is published as Kotlin `internal` even though it is visible in bytecode.

The final implementation does not depend on that internal Kotlin API. It discovers the active surface as public `android.webkit.WebView` and accepts it only when the runtime class name is exactly:

`org.readium.r2.navigator.R2WebView`

No private-field reflection, engine fork, or alternate WebView implementation is introduced.

## Scroll-listener safety audit

The main Readium classes involved in EPUB navigation were decompiled and checked for known use of `setOnScrollChangeListener`:

- `R2WebView`
- `R2BasicWebView`
- `R2EpubPageFragment`
- `EpubNavigatorFragment`

No use was found in those paths, so Slice 1E does not replace a known Readium scroll-change listener.

## Verification

- ✅ `:app:assembleDebug`
- ✅ `:app:testDebugUnitTest`
- ✅ `:app:lintDebug`
- ✅ Pure motion tests cover identity, forward midpoint, backward midpoint, and invalid width
- ✅ Real EPUB card opens the Reader on emulator
- ✅ Reader chrome: Close / Notebook / Bookmark / Settings
- ✅ Forward page action produces a materially different page image
- ✅ Forward image difference vs initial: mean absolute difference `32.4425`; changed-pixel ratio `0.352735`
- ✅ Backward action returns exactly to the initial page image: mean absolute difference `0`; changed-pixel ratio `0`
- ✅ Continuous Scroll can be enabled and actual Reader content scrolls vertically
- ✅ Continuous Scroll was restored to Paginated after QA
- ✅ Settings sheet dismissed successfully after the final restore
- ✅ Reader bottom navigation remains absent while reading
- ✅ No FATAL exception or ANR observed in the emulator QA pass

## Deliberately not claimed

- This is **not** a true physical page curl.
- Emulator automation cannot judge subjective premium feel as well as a real-device human review.
- Motion quality still needs the existing real-phone visual gate before any production merge.

## Next candidate slice

**Slice 1F — Reader Gesture & RTL Motion Matrix**

Validate and harden page-direction semantics for LTR/RTL, edge taps, swipe gestures, orientation, reduced motion, and resume behavior before expanding motion further.