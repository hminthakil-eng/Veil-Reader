# Real Android Debug APK Export — QA Receipt
**Date:** 2026-10-15.
**Device:** authorized Windows DESKTOP-GSV91KV.
**Engine:** official Godot `4.7.2.stable.official.ed1daf0bf`.
**Source branch:** `game/android-mobile-gate-v1-20261014` (draft PR #539, stacked on #537).

## Verified artifacts (ACTUAL)
- Official standard export archive `Godot_v4.7.2-stable_export_templates.tpz`: exact published size **1,281,349,702 bytes** and SHA-256 `f298490b8d44d934be425a5a65a51bf15f422428b229a06a6e11d9ffea248011` **verified**, installed matching `android_debug.apk` (127,260,725 bytes) and `android_release.apk` (104,803,333 bytes) into private Godot app template directory.
- The first build initially FAILED with missing Android ETC2/ASTC import setting; enabled project `rendering/textures/vram_compression/import_etc2_astc=true`. Next build FAILED with missing project icon; created an original simple **QA-only placeholder** lantern/book icon `assets/qa_book_lantern_icon.png`, then exported successfully.
- `tools/build_android_qa.ps1` actually completed: `APK_QA_READY`. Result file **`E:\EyadStudio\Projects\Veil-Reader-MobileGate-20261014\island-game\build\IslandBookshop-QA.apk`**.
- **Size: 57,012,198 bytes** (~57.0 MB decimal). **SHA-256: `D4D189BAE43C38509A1EA3A97DEC27AD831B88C3CD6B8F93E6B2D577ADBCC10D`** (final rebuild 2026-10-15).
- `aapt dump badging` actual APK: package `org.eyadstudio.islandbookshop.qa`, versionCode `1`, versionName `0.1.0-qa`, minSdk `24`, targetSdk `36`, native ABI `arm64-v8a` and `x86_64`.
- `aapt dump permissions` parsed and audited by script: no declared Internet, camera, microphone, external storage or precise-location permission. **`ANDROID_MANIFEST_PERMISSION_AUDIT PASS`**.
- `apksigner verify --verbose --print-certs` on built artifact: valid **APK Signature Scheme v2 and v3**, one signer **CN=Godot**, debug key. This signing identity is for **development**, not release; the release signing key and update compatibility are separate decisions. Script enhanced to enforce `APK_SIGNATURE_AND_ABI_AUDIT PASS` on every subsequent build.
- Strict Godot Windows regression suite *after setting icon/Android rendering flags*: **257/257 PASS** (19 legacy shop + 30 economy + 25 unified commerce + 118 day-one + 45 checkpoint + 20 portrait/landscape geometry).

## Isolated emulator attempt: RED, not an app failure
- Existing unrelated emulator `emulator-5554` already **offline**, belonging to Veil Reader QA; left untouched (no kill, uninstall, data deletion or ADB global reset).
- Created independent AVD `IslandBookshopQA_API35` with system image `android-35/google_apis/x86_64`, home on **E:**, separate target `emulator-5584`. Attempted headless isolated boot twice; never reached `adb ... device` or `sys.boot_completed=1`. No APK install performed.
- Windows Application Event Log recorded repeated **`netsimd.exe` application crashes** with exception **0xc000001d**. Only ~1.83 GB free physical memory during retry; WHPX hypervisor reported operational. The exact root cause **is not proven**; emulator/host environment blocked the test before the APK ran.
- `tools/test_android_qa.ps1` refuses to install unless **only `emulator-5584`** is online and fully booted. An attempted run correctly aborted with `Isolated QA emulator is not ONLINE`. The script is a future controlled smoke path for install, launch, screenshot and relaunch, **NOT proof that those steps ran**.

## Mandatory remaining gates
1. Boot a reliable **isolated** Android device or authorized physical phone with enough resources; avoid touching the Reader QA emulator.
2. Install and launch the exact SHA-documented debug APK. Collect logcat, actual screenshot(s), render baseline and crash-free smoke evidence.
3. Inspect Persian glyph shaping, mixed number direction, safe-area/notch, 56px touch action feel, swipes vs accidental taps, screen reader/accessibility and small-screen font scaling.
4. Manual Save/Load across app force-stop/relaunch plus intentional corrupt-checkpoint recovery on Android; compare cash/inventory and NPC decisions for duplication.
5. Revisit production quality of pixel art, portrait transitions and player engagement. APK export passing is **not** gameplay quality proof or ready-to-publish status.
6. Debug APK is a local signed build; **no file was published as a GitHub release**. Source and reproducible build scripts are pushed, but an APK is not automatically downloadable from this conversation.

## CI / distribution
No authenticated `gh` session was available on Windows for creating a *private draft* binary release. The GitHub repo is public, so do **not** upload/publish the debug APK to a public release without owner's approval. User can access local machine E: path. No claim of GitHub hosted artifact or download URL.

## Verdict
**GREEN:** template provenance; real debug APK export; APK signing/ABI/manifest audit; local Godot headless regression 257/257.
**RED:** Android app launch, device QA, Persian visual layout, crash-safe lifecycle on Android, release security/performance.
