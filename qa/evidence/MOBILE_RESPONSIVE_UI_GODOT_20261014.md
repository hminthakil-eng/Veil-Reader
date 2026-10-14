# QA Receipt — Responsive Grouped Godot UI, 2026-10-14

Windows DESKTOP-GSV91KV, isolated branch `game/mobile-responsive-input-20261014`.

## Exact headless results
- Baseline inherited: **237/237** passing assertions.
- Mobile/reflow categories: **25/25** passing assertions; includes signal-driven selection, no mutation of money, 48-unit minimum Control targets, valid portrait/landscape reparent.
- Virtual canvas: **28/28** passing assertions:
  - 960x540 two-pane HBox, controls 315 logical wide.
  - 1280x720 two-pane HBox, controls 315 logical wide.
  - 720x1280 VBox, controls 720 logical wide and ~941 logical tall.
  - 540x960 VBox, controls 540 logical wide and ~595 logical tall.
- **FINAL strict runner 290/290 PASS**, engine source import, existing scenes and first-day scene started, no script errors.
- Legacy ShopLedger and main story logic were not changed.

## Genuine discovered blocker
The initial test prototype launched Godot headless with CLI `--resolution`; it still exposed fixed 960x540 virtual canvas and initially reported inconsistent layout failures. We treated this as **a test assumption bug**, removed the misleading scripts, and used explicit `root.content_scale_size` virtual resizing. The accepted tests are **virtual Control structural tests only**. They do not prove that Android display rotation and text rendering work.

## Missing tests
- Android export templates and preset not currently installed; no APK or on-device input screenshots.
- ADB emulator presently offline; did not disturb any potentially active Veil Reader QA session.
- Font glyph shaping/legibility, OS physical dp mapping, touch/scroll gesture semantics, screenreader labels, accessibility settings and app process-kill recovery remain RED.
