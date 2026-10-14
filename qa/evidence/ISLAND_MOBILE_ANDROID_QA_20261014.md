# Mobile and Android Export QA — 2026-10-14

**Host:** DESKTOP-GSV91KV Windows; Godot official `4.7.2.stable.official.ed1daf0bf`.
**Source:** game/android-mobile-gate-v1-20261014 stacked on durable checkpoint PR #537.

## Proven
1. Modified project portrait base dimensions 432×960, `canvas_items`/expand, Android portrait orientation. The RPG's first-day scene creates a portrait VBox with pinned story/world context and a separate scrolling choices area; landscape retains a two-column HBox.
2. Existing shop/district rendering min 620px previously overflowed phone width; removed in `scenes/pixel_district.gd`, verified stage no longer exceeds 432px in headless 432×960 layout.
3. `tests/run_mobile_layout_tests.gd` reported **MOBILE_LAYOUT_TEST_RESULTS passes=20 failures=0**, headless Windows in portrait/landscape. Same scene retains save/load buttons and all day-one economic/relationship actions.
4. Prior full QA baseline from PR #537 **237/237 passing**. Final strict runner actually executed on Windows official Godot 4.7.2 after pinned-header mobile redesign: **257/257 PASS**, runner exit 0, zero SCRIPT ERROR/ERROR: FAIL. Exact source breakdown: 19 shop + 30 economy + 25 commerce +118 first day +45 checkpoints +20 mobile layout.
5. Official Godot 4.7.2 standard export templates SHA and byte length recorded in `tools/install_export_templates.ps1`; file downloaded to **E:** with Windows BITS, progress must be verified before any build claim.
6. Windows Android SDK and OpenJDK17 are detected and global Godot editor Java/Android SDK paths configured. SDK build-tools 35.0.1 is present. Android SDK platform android-35 was subsequently observed installed, as was build-tools 35.0.1.
7. Dedicated Android QA export preset and build script added: unique package ID, arm64+x86_64, no requested Internet/camera/storage privileges, post-build manifest audit required. No Android binary claim until script reports APK_QA_READY with SHA.

## NOT proved
- No Android APK built or device installed from this branch at time of this evidence draft.
- No physical touch, safe-area/rounded corners, text glyph shaping, user-supplied Persian UI font, accessibility reader or continuous scroll UX confirmed.
- Headless UI tests only verify interface structures/bounds, not visual attractiveness or animation quality.
- No actual Android process-kill/restart checkpoint survival. Windows fault-injection QA from #537 does not establish mobile crash durability.
- No integration with private personal EPUBs or main Veil Reader Android code.

## Follow-up tests when export templates available
Run `powershell -File island-game/tools/install_export_templates.ps1` after BITS has completed and exact TPZ SHA matches; then `powershell -File island-game/tools/build_android_qa.ps1`. Audit AAPT package/permissions, install to a **separate** emulator with test-only package, check touch, screenshot, Persian numerals/RTL, save/relaunch and app logcat, then update this evidence with exact emulator/device test receipts. Never claim absent tests were performed.
