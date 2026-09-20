# Reader Core Strike Audit — 2026-09-20

Status: active hardening branch
Branch: agency/p7-reader-hardening
Base: UX branch at 27c0b6c

## P0 findings fixed in this strike

### RST-001 — Paper mode had split interaction behavior — FIXED
Before:
- edge tap used Veil paper curl.
- drag/swipe fell through to another path, so Paper mode could feel different depending on gesture.

After:
- PaperCurlInputListener handles tap + interactive drag inside Readium's own input pipeline.
- no full-screen touch-owning Android overlay is introduced.
- page curl follows drag progress.
- commit/cancel threshold is explicit and unit-tested.

### RST-002 — cancelled drag could transiently persist the preview destination — FIXED
Cause:
- generic locator persistence observed navigator changes while an interactive preview was active.

Fix:
- while PaperCurlState is active in Paper mode, generic locator persistence is suppressed.
- only committed paper turns persist/count via onCommittedTurn.
- cancelled previews return to the prior state without writing transient progress.

### RST-003 — cancel restoration used directional reversal instead of exact origin — FIXED
Risk:
- opposite navigation is not always equivalent to exact origin across spine/resource boundaries.

Fix:
- capture exact start Locator.
- restore exact Locator on cancel.
- directional reverse remains defensive fallback only.

### RST-004 — input listeners had no explicit removal lifecycle — FIXED
Before:
- listeners were installed from LaunchedEffect.
- ownership/removal was implicit.

After:
- DisposableEffect owns listeners.
- every registered listener is removed explicitly on dispose.

### RST-005 — directional animation policy was static — FIXED
Before:
- Readium DirectionalNavigationAdapter was always animated.
- switching Paper/Slide could leave fallback/keyboard behavior inconsistent.

After:
- VeilDirectionalNavigationInputListener reads current page-turn preference dynamically.
- Slide uses animated navigation.
- Paper fallback/keys use non-slide navigation.

## Verification

- Paper direction mapping LTR/RTL tests.
- commit threshold distance test.
- commit threshold curl-progress test.
- tentative small-drag rejection test.
- full :app:testDebugUnitTest passed.
- full :app:lintDebug passed.
- full :app:assembleDebug passed.

## P0 physical-device gates still open

These cannot be declared solved by JVM/build gates:
- slow drag from page body.
- edge drag.
- rapid back-to-back drag.
- cancel before threshold.
- cancel after long hold.
- end-of-book boundary.
- RTL drag direction.
- theme Paper/Sepia/Dusk/OLED.
- 60/90/120Hz perceived frame pacing.
- background/foreground during a drag.
- WebView snapshot correctness on target device.

## Remaining high-risk findings

### RST-006 — View.draw() WebView snapshot reliability — YELLOW
PaperCurlState captures publicationView using View.draw(Canvas(bitmap)).
Some WebView/hardware combinations can produce stale/blank captures.

Escalation:
If physical QA shows blank/stale snapshot, move capture behind a SnapshotProvider and evaluate PixelCopy/other safe capture paths.

### RST-007 — full-screen ARGB_8888 snapshot memory — YELLOW
A 1080x2400 buffer is roughly 10 MB.
Reuse avoids allocation churn but retained footprint remains.

Action:
measure memory on midrange hardware before changing quality format.

### RST-008 — tap animation duration may feel heavy — YELLOW
Current tap curl is 520 ms.
Do not tune by taste alone; compare physical-device feel with benchmark readers and record chosen timing.

### RST-009 — PDF control leaks implementation detail — RED architecture debt
PdfZoomControls still touches experimental PDF implementation details.
Move behind PdfReaderController in Architecture 2.1.

### RST-010 — no instrumented gesture regression yet — RED
Pure geometry tests cannot prove real WebView/InputListener integration.
Add instrumentation/device tests after ReaderSession extraction or with a controlled test publication.

### RST-011 — adaptive API deprecations — YELLOW
Current shell uses deprecated WindowWidthSizeClass/WindowHeightSizeClass APIs.
PXFE/Core joint migration should use current Material 3 Adaptive breakpoint APIs.

### RST-012 — page effect still coupled to ReaderScreen — YELLOW
ReaderScreen owns registration, persistence gating and visual overlay orchestration.
Target remains:
feature:reading -> ReaderSession/PageTurnCoordinator -> reader engine/effect.

## Non-negotiable invariant

No future GPU/Compose surface may sit above the reader and own Android touch dispatch.

Allowed:
Readium input event -> Veil gesture policy -> navigation/preview transaction -> visual effect.

Forbidden:
Visual overlay consumes touch -> reader navigation tries to recover underneath.

## Promotion rule

This branch must not merge into the UX integration branch until physical-device drag/tap acceptance passes.
