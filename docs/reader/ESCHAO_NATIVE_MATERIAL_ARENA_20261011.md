# Veil Reader — ES2 Material Fold A/B Arena

Status: **debug-only engine candidate — physical quality NOT accepted**. Product #408 remains RED.

## Source, license, reproducibility

- Vendored native Java ES2 folding/shadow core from [eschao/android-PageFlip](https://github.com/eschao/android-PageFlip), public upstream `master` Git root tree `6c361e0932c80c360fdf5f7de06963031e8c26d2`, 18 Java files, with three shader-program classes minimally adapted to import the generated `com.veilreader.app.R` namespace.
- Source location: `app/src/debug/java/com/eschao/android/widget/pageflip/*.java`; all upstream author + Apache-2.0 headers preserved; full [Apache 2.0 license](../../app/src/debug/assets/THIRD_PARTY_ESCHAO_PAGEFLIP_LICENSE.txt).
- The upstream library also requires six GLSL shader assets from `PageFlip/src/main/res/raw/*.glsl`. They are copied unchanged to Veil's debug-only `app/src/debug/res/raw/` so the actual geometry, back-face and shadow shader programs compile and render. The first clean-CI run failed because these raw assets and namespace imports were absent; this is corrected in the child QA branch.
- No Maven/JitPack artifact: this avoids the known unavailable `com.github.eschao:android-PageFlip:1.0.2` on clean CI. No dependency added to release and no LGPL/GPL third-party code or proprietary Moon+/Kindle source copied.
- Adapter `EschaoPageFlipArenaActivity.kt` is Veil-authored, debug-only, using `PageFlip` GL API directly. Unlike the prior Slide-like Veil Paper mode, geometry and GL shaders are from a working open-source page flip implementation. **Only real-device review can establish feel.**

## Engine controls already exercised

- OpenGL ES 2.0, portrait, one sheet (independent from scroll, slide and live Readium).
- 5-pixel mesh cells for smooth bending; semi-perimeter ratio `0.70` (test setting, not tuned to phone yet).
- Independent front/back paper samples; folds with own underside ink.
- Distinct moving fold edge shadows and base shadows; fade/brightness of back paper `72/255`.
- Native touch down/move/up, directional forward/reverse and callback after successful release; canceled fold must not commit the page.
- Render loop owns GL texture uploads and recycling. Activity forwards `onPause()/onResume()` to GLSurfaceView.
- No production locator, reading time or annotations involved. Synthetic 8-page lab only.

## Physical test on a signed isolated APK

Use **only** a successful `Paper Curl Device QA APK` artifact built at a SHA containing this eschao arena. Confirm APK output, signature and SHA256 against `APK-METADATA.txt`, `SIGNATURE.txt`, `SHA256SUMS`. The app must be **Veil Paper Lab QA** / `com.veilreader.app.paperlab`, distinct from `com.veilreader.app`. Existing isolated debug signers are ephemeral and may require uninstalling only the earlier Paper Lab test package.

1. Install extracted QA APK without deleting or upgrading production Veil.
2. **Long-press the Paper Lab app icon**, choose **Material Paper** for new eschao vs **Paper Curl Lab** for existing Irurueta (if supported by your launcher). Or use ADB to open eschao directly:
   `adb shell am start -n com.veilreader.app.paperlab/com.veilreader.app.debug.EschaoPageFlipArenaActivity`
3. With the same device, compare both engines on first edge touch, quarter-turn, halfway, fold-back illumination, back texture, quick-release snap, cancelled drag, repeated 100 turns, rotation, app background/foreground, portrait/landscape, 60/120 Hz.
4. At least one actual phone screen recording for each engine, with similar swipe trajectory. Record device, Android API, panel Hz, APK source SHA and APK SHA256; do NOT use FPS guess as measured data. Capture with optional `tools/collect_paper_device_evidence.py --verify-installed-apk`. Review privacy before sharing recordings/logs.
5. Reject engine if any blank frame, teleported paper/ink, vertical no-curl slide, page index changing on cancelled drag, GPU texture leak, unresponsive finger tracking, or broken RTL gesture policy.

## Production integration fence

A beautiful synthetic fold is still not a working EPUB reader. A future production adapter must acquire immutable previous/current/next snapshots from the actual Readium location/window, associate each snapshot with the correct locator and EPUB style, use a single owner for navigation and atomically commit locator only on *successful* fold completion. Forward/back/cancel, mixed Arabic/Persian/English, font/theme changes, rotation, fragment recreation and low-memory recovery require tests. No silent fallback to Slide; no release until device acceptance.

**Rollback:** close `arena/eschao-native-pageflip-20261011` PR; nothing in production `main` or existing user library changes.
