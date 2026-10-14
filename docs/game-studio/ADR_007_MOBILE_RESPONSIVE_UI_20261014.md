# ADR-007 — Mobile-responsive First-Day UI, grouped actions and honest Android readiness

**2026-10-14.** Standalone Godot 4.7.2 game branch `game/mobile-responsive-input-20261014` stacked on checkpoint PR #537. Scope is Godot UI/structure, **NOT** Android-certified touchscreen use or a packaged APK.

## Peer review: real, with disagreement
ChatGPT inspected authorized Windows prerequisites and the first-day UI after the preceding 237 green tests. Gemini Desktop **Flash-Lite** (selector verified; this was NOT an Argon review) independently raised these risks: cramped fixed two-column UI based on 600+315 minimum logical widths, sixteen-plus buttons in one long ScrollContainer, lack of verified Farsi glyph rendering/actual mobile touch, and no installed Godot export templates. It advised:
- Responsive containers instead of screen coordinates, minimum 48 logical-unit tap targets;
- Group controls by task instead of forcing a long scroll;
- Preserve Godot pixel-stage render with independently sized full-resolution dialogue controls;
- Delay Android export templates until UI mechanism is stable, and never mistake headless tests for device proof.

**Agreement:** make small, verifiable UI fixes before building more economy, story quests, Reader integration or custom TTS.
**Reasoned disagreement:** Gemini preferred **landscape-only** support for the first pilot. ChatGPT chose *landscape-first plus structural portrait reflow* without claiming phone visual QA. A 540x960 portrait virtual canvas is now tested but still needs actual rendered font and touch approval.

## Actual changes
- `scenes/first_day.gd` originally used one horizontal row, hard-coded 600-unit story pane and a 315-unit controls pane. Now it retains the same live narrative/controller/buttons and swaps between HBoxContainer and VBoxContainer based on the logical viewport aspect/available width. This uses live child reparenting rather than clones, so decisions/cash persist.
- Books, family-record choices, visitor, civic works, and save/load move into **five selectable categories**, rather than all being stacked continuously. The category selector uses RTL and original Persian labels (stored in GDScript Unicode escapes); all existing choice buttons still invoke their original business logic.
- Every action button and the book/category OptionButtons declare a minimum **48 Godot UI-unit** height. This is **not proven to equal 48 Android dp**.
- `scenes/pixel_district.gd` no longer enforces the original 620-pixel desktop-only minimum width. The pixel stage shrinks vertically in portrait while story/dialogue remain full-resolution Control labels, not rasterized inside the art.
- Signals survive rotation and grouping. Save/load and 88-coin economics are not regenerated or reset on reparent.

## Test evidence
Official Windows Godot `4.7.2.stable.official.ed1daf0bf`:
- Previous 237 tests remain unchanged.
- `tests/run_mobile_layout_tests.gd`: **25/25 PASS**, including five grouped panels, 48-unit targets, actual `OptionButton.item_selected` signals, two changes of layout, simulated UI dialogue/funding after multiple reparentings, and 88-coin conservation.
- `tests/run_virtual_viewport_tests.gd`: **28/28 PASS**; sets Godot `root.content_scale_size` to four explicit **virtual canvas** dimensions: 960×540, 1280×720, 720×1280, 540×960. Inspects actual Control hierarchy, resized panel geometry, usable scroll area, menu selection and untouched economics.
- `run_tests.ps1`: **290/290 PASS** locally, source import and old+new scene smoke pass, no `SCRIPT ERROR`.
- Explored `--headless --resolution WxH` first, discovered that it **does not actually resize the virtual canvas** in this headless Godot setup; false-positive test was rejected and exploratory scripts removed. The accepted virtual tests manipulate `root.content_scale_size` directly. They do NOT simulate an Android device, screenshot, density or multitouch.

## Android readiness diagnostic
- Windows: `adb.exe` available at `C:\Users\Microsoft\AppData\Local\Android\Sdk\platform-tools\adb.exe`, JDK 17 present, multiple API35 Android AVD profiles listed.
- `adb devices -l` reported `emulator-5554 offline` at inspection. No authorized working Android shell/test target was confirmed.
- Official Godot 4.7.2 Windows editor present, but no `export_presets.cfg` in project and no export templates found in the standard roaming-user `Godot/export_templates` location. `ANDROID_HOME` and `ANDROID_SDK_ROOT` unset.
- **No APK/AAB built**, no real phone tap test, screenshot/font shaping review, performance trace, power-loss/Android process-kill test or Persian font packaging/RTL layout certification. Export tooling remains an explicit next-gate task.

## Next production gates
1. Establish a clean Android API35 emulator separate from active Reader sessions; verify adb reports `device`, not offline. Do not forcibly close another user's running emulator.
2. Install/verifiably pin appropriate official Godot Android export templates, SDK/JDK paths and an Android debug export preset; produce an actual APK.
3. On Android: inspect *rendered* Persian letter joining, text clipping, 48dp-equivalent real touch targets, scroll and app orientation on an emulator plus a physical phone if available.
4. Run actual app lifecycle process termination and recovery from `user://` checkpoint; no claim of mobile durability until completed.
5. Then expand to chapter 2, real NPC autonomous businesses, more than one real customer sale and premium anime/pixel production art.
