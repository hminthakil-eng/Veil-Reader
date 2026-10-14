# ADR-007 — Mobile-first Story UI and Reproducible Private Android QA Export

**Date:** 2026-10-14. **Branch:** game/android-mobile-gate-v1-20261014. **Status:** Godot headless UI adaptation passed; APK/device gate pending. **Scope:** standalone Godot island/bookshop game, not the Android Veil Reader repository app.

## Why
PR #537 supplied a working first-day game (237/237 Godot headless assertions) with a recoverable manual checkpoint, but a 960×540 desktop HBox and minimum 620-pixel pixel-stage **cannot** be used as a coherent portrait phone interface. Windows test results were insufficient to claim physical mobile UX, Persian shaping, or Android app viability.

## Official source references
- [Godot 4.7 multiple-resolution guidance](https://docs.godotengine.org/en/4.7/tutorials/rendering/multiple_resolutions.html): orientation, canvas_items, expand, Control containers; mobile portrait typically starts with a taller-than-wide design size. We choose smaller 432×960 design units with future physical DPI QA mandatory.
- [Godot stable Android export](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_android.html): same-version export templates, OpenJDK17, Android SDK and export settings.
- [Godot 4.7 export command](https://docs.godotengine.org/en/4.7/tutorials/export/exporting_projects.html): Android export presets and CLI debug APK.
- [Official Godot 4.7.2 release](https://github.com/godotengine/godot-builds/releases/tag/4.7.2-stable), standard export template archive SHA-256: `f298490b8d44d934be425a5a65a51bf15f422428b229a06a6e11d9ffea248011`; published asset length 1,281,349,702 bytes. Download currently ongoing on developer desktop; hash gate blocks partial/mismatched templates.

## Design decisions (first implementation)
- `project.godot`: portrait baseline 432×960, Android orientation portrait, canvas_items stretch/expand, GL Compatibility renderer.
- `scenes/first_day.gd`: choose portrait top-level VBox versus landscape HBox based on viewport dimensions at scene creation. **Portrait shows the world stage, story text and economy indicator as fixed top context**, then a lower scrolling selection deck. Reject initial idea of scrolling the *entire* page; an actual peer critique identified that this makes the RPG resemble a tedious admin form. Landscape keeps side-by-side world/story and actions.
- `scenes/pixel_district.gd`: remove unconditional 620px minimum that clipped on 432px screens. Scene art retains geometry, viewport-based scaling and ledger-driven restored lamps. Still hand-coded placeholder art, not the requested finalized world.
- `scenes/first_day.gd`: touch-target minimum 56 design pixels in portrait, initial RTL control direction for status, dialogue, choice labels. Shared ledger remains one authoritative four-wallet state; no duplicate `ShopLedger` added.
- `tests/run_mobile_layout_tests.gd`: 20 assertions covering 432×960 portrait vs 960×540 landscape composition, no old pixel minimum overflow, retained all shop/story/save action controls, explicit RTL properties, usable stage, minimum button heights and preserved 88-coins invariant. **NOT screenshot, emulator, finger interaction or Persian glyph shaping proof.**
- `export_presets.cfg`: dedicated debug QA package `org.eyadstudio.islandbookshop.qa`; arm64 + x86_64 emulator, no requested Internet/camera/storage permission; export has not been completed/AndroidManifest.xml not yet audited.
- `tools/install_export_templates.ps1`: installs only the matching official Android release/debug template files after comparing archive length and SHA-256 (keeps downloads and secrets out of Git).
- `tools/build_android_qa.ps1`: reproducible debug APK export command with JDK17 + SDK checks and a post-export `aapt` manifest audit rejecting sensitive/unnecessary permissions. A passing build script will be necessary before any APK readiness claim.
- `island-game/.gitignore`: excludes APK and built binaries, while keeping source and reproducible build instructions.

## Actual review with Gemini
In a new live Gemini Desktop consultation, selector was **Flash-Lite**, NOT Gemini 4 Argon. Review notes captured at `E:\EyadStudio\Temp\Gemini_Island_Mobile_QA_Response_20261014.txt`.
Gemini flagged:
1. Font shaping and BiDi/digit mixes require **visual screenshots**, not Control.direction alone.
2. Safe area/notch/nav-bar issues on API35 devices; physical screen shape tests required.
3. Swipe-vs-tap conflict with ScrollContainer; actual touch/drag tests required.
4. DPI and minimum target sizing at 360dp; test devices and accessibility scales.
5. Financial label direction mixing Persian + numbers must be visually verified.

**Accepted:** portrait game-world + story header pinned while choice deck scrolls, rather than an endless page. **Accepted as release blockers:** screenshot/RTL validation, touch interaction, device force-stop + checkpoint restoration, AndroidManifest permissions audit. **Disagreement:** Gemini's generic proposal to replace local Godot game logic with another event-driven framework is not necessary now; existing pure deterministic core and versioned local checkpoints already pass 237 tests. No new remote services or giant framework.

## STOP-SHIP / outstanding
- Godot official export ZIP download, validated unpack/install, API35 SDK platform availability and built debug APK confirmation.
- Emulator: existing `emulator-5554` was **offline**; do not disrupt somebody else's current reader emulator. A new isolated emulator or authorized phone is required.
- Android UI screenshot/font shaping, inset/notch safe area, real finger tap/drag, process-kill disk recovery and persisted choices; no APK has been installed or exercised in this branch yet.
- Current graphic stage and long choices menu remain a **testable greybox**, not polished premium anime/pixel RPG UI. No 20-minute playtest or user rating.
- Need verify export manifest actually has no Internet/storage permissions; preset flags alone do not prove runtime permissions.
