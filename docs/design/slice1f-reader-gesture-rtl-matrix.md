# Slice 1F — Reader Gesture & RTL Motion Matrix

Status: **DRAFT / stacked on Slice 1E**
Parent: `ux/slice1e-reader-motion-reliability`

## Goal

Lock the directional-navigation contract before adding any more reader motion:

- LTR edge taps
- RTL edge taps
- center tap handoff
- Continuous Scroll behavior
- reduced-motion propagation
- orientation / resume follow-up gates

## Reuse decision

Veil does **not** implement its own RTL edge-direction layer.

Readium 3.4.0 `DirectionalNavigationAdapter` already derives left/right page direction from the navigator's reading progression:

| Reading progression | Left edge | Right edge |
| --- | --- | --- |
| LTR | backward | forward |
| RTL | forward | backward |

This behavior was verified from the Readium 3.4.0 bytecode and then locked with an instrumentation contract test against the real Readium adapter.

The default adapter also declines horizontal edge-tap navigation while the navigator is in Continuous Scroll mode. Veil preserves this behavior rather than inventing a second gesture system.

## Added automated contract

`app/src/androidTest/java/com/veilreader/app/ui/reader/DirectionalNavigationContractInstrumentedTest.kt`

The test uses a fake `OverflowableNavigator` with the **real** Readium `DirectionalNavigationAdapter`.

Cases:

1. LTR right edge → `goForward`
2. LTR left edge → `goBackward`
3. RTL right edge → `goBackward`
4. RTL left edge → `goForward`
5. center tap is not consumed by directional navigation
6. Continuous Scroll does not consume horizontal edge taps by default
7. `animatedTransition=false` is propagated to navigation for the reduced-motion contract

(The edge-direction pairs are exercised in two tests; targeted suite total: 5 tests.)

## Verification

- ✅ `:app:assembleDebug`
- ✅ `:app:assembleDebugAndroidTest`
- ✅ `:app:testDebugUnitTest`
- ✅ `:app:lintDebug`
- ✅ targeted emulator instrumentation: **5/5 tests**
- ✅ LTR right/left edge contract
- ✅ RTL right/left edge contract
- ✅ center-tap handoff
- ✅ Continuous Scroll edge-tap guard
- ✅ reduced-motion `animated=false` propagation

## Emulator tooling note

During follow-up UI automation, repeated `FATAL EXCEPTION` entries were traced to the **uiautomator shell process**, not Veil Reader:

`UiAutomationService ... already registered`

This is an automation-environment artifact and is not counted as an application crash.

## Still open / manual gate

The current emulator became unreliable for deterministic post-instrumentation UI automation because its task stack moved between DocumentsUI / launcher and repeated UiAutomation registrations conflicted.

Therefore the following are **not claimed as verified in Slice 1F**:

- ⬜ real-Reader orientation change and return
- ⬜ real-Reader process relaunch / exact resume
- ⬜ real-device subjective RTL swipe feel

These are verification gaps, not confirmed product failures.

## Production impact

No production directional-navigation logic was added or replaced in Slice 1F.

The only code addition is the regression contract test. This keeps Readium as the source of truth and avoids duplicated gesture semantics.

## Next candidate slice

**Slice 1G — Deterministic Reader QA Fixture & Resume Harness**

Create deterministic LTR + RTL EPUB QA fixtures and a repeatable Reader harness so orientation, process death, relaunch, exact locator resume, edge taps, and future motion regression can be tested without depending on manually imported library state.
