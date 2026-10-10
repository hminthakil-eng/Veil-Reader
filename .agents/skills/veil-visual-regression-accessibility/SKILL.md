---
name: veil-visual-regression-accessibility
description: Validate premium Grayfog/Sanctuary design with deterministic screenshot tests, design-system parity, bilingual RTL typography, scalable text, reduced motion and TalkBack usability.
---

# Visual Quality, RTL and Accessibility

## Source of truth
Read current design-system tokens and user-approved Grayfog/Sanctuary direction. Compare against actual running Reader/library/settings screens, not conceptual mockups. Make typography readable on parchment and deep shell; no ornamental UI on top of essential text.

## Visual matrix
Capture controlled before/after baselines: compact/large phones, portrait/landscape, light/dark, font scale 1.0/1.3/2.0, English/Persian and mixed RTL/LTR, empty/loading/error/offline, active annotations and reader controls.
Pick exactly one screenshot-test system after toolchain compatibility proof; Roborazzi has Robolectric integration and Paparazzi provides JVM rendering. Do not simultaneously install competing golden frameworks without clear need.
Keep golden update review manual and review failures as human-visible image diffs, not bulk regenerate-on-failure.

## Accessibility gates
Interactive target sizing, TalkBack labels/focus/focus restoration, state announcements, selection actions, high contrast, color independence, screen magnifier, keyboard/hardware controls, reduced motion, and text clipping/overlap.
Exercise both Compose semantics and embedded Readium navigator/web content; test semantics do not prove practical screen-reader usability.
Require manual physical-device walkthroughs in addition to Android accessibility scans and automated tests.

## Definition of done
For changed screens provide screenshots and diffs plus device/locale/font scale, a11y walkthrough, before/after user task success and remaining discrepancies. A visually beautiful but unreadable or inaccessible screen fails.
