# Veil Reader — Paper Curl Lab, separate physical QA install

**Owner:** Grand Forge / Alpha / Arena. **Status:** experimental, not released, not Readium-integrated.
**Product gate:** [#408](https://github.com/hminthakil-eng/Veil-Reader/issues/408) remains **RED / UNVERIFIED**.

## Why there is a separate package

The existing **Forge QA** installation uses `com.veilreader.app.forgeqa` and a pinned signing certificate. Its [CI run 38133616437](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/38133616437) correctly failed because the legacy cached signing key was not available from a new branch. **Do not generate a different key under that existing identity.**

This experiment instead uses a wholly separate `com.veilreader.app.paperlab` **debug-only** package, clearly labeled **Veil Paper Lab QA**, with ordinary ephemeral Android debug signing on GitHub's runner. No production APK, Play signing, pinned Forge QA keystore, or Android user data is changed by creating this build.

**Important:** The temporary debug signing certificate may change across different runners or builds. This package is for **fresh installs only**, not guaranteed in-place upgrades. If replacing a previous Paper Lab APK, `adb uninstall com.veilreader.app.paperlab` removes **only this experimental package and its test-local data** before reinstalling. Never uninstall `com.veilreader.app` or `com.veilreader.app.forgeqa`.

## Artifact and identity gate

Workflow: `.github/workflows/paper-curl-device-qa.yml` on experimental Paper branch. It must finish successfully **on the exact candidate commit** and upload `veil-reader-paper-lab-device-apk` containing:

- `VeilReader-PaperLab-<SHA8>.apk` — only installable test package
- `APK-METADATA.txt` — confirms exact `com.veilreader.app.paperlab` and `Veil Paper Lab QA`
- `SIGNATURE.txt` — shows signing certificate hash; not the pinned Forge QA certificate
- `BUILD.txt` — actual source SHA, run ID and explicit fresh-install policy
- `SHA256SUMS` — compare against the downloaded **APK bytes**, not the outer artifact ZIP

Never infer success from a workflow being merely triggered. If missing an artifact, wrong package name, signature, emulator tests or mismatch, mark **BLOCKED**.

## Install and launch (physical QA operator)

1. Download the successful GitHub Actions artifact and inspect the included package/signature evidence.
2. Preserve a real book fixture and note its SHA256. It is not necessary to uninstall or modify your current production Veil Reader.
3. If a previous **Paper Lab QA** install exists and has a different debug certificate, uninstall **only** the test package:

   `adb uninstall com.veilreader.app.paperlab`

4. Install the extracted signed APK with `adb install path/to/VeilReader-PaperLab-<SHA8>.apk`. Confirm its Home icon says **Veil Paper Lab QA**.
5. The normal **Veil Paper Lab QA** app icon opens the real Reader/Library, not fake specimen pages. For convenient phone testing, **long-press the Veil Paper Lab QA app icon** and choose **Paper Curl Lab** from the Android launcher shortcut menu (availability depends on launcher support). There is no second `MAIN/LAUNCHER` icon: the added shortcut cannot hijack app launches. As a debug-only fallback on Android, open the explicit URI `veilpaperlab://curl` with a capable deep-link handler, or use ADB:

   `adb shell am start -n com.veilreader.app.paperlab/com.veilreader.app.debug.ExternalCurlArenaActivity`

   The original APK built at SHA `8208eddd` **does not include this later shortcut**; its tester must use ADB. Only choose an updated APK once its exact-head workflow completes and its signature/hash are checked.

6. Reuse the existing local-only physical evidence collector from experimental QA [#454](https://github.com/hminthakil-eng/Veil-Reader/pull/454), now available in this branch and configured for Paper Lab by default. With one authorized physical ADB device and exact extracted APK, run: `python tools/collect_paper_device_evidence.py --source-sha <40-char-source-SHA> --apk path/to/VeilReader-PaperLab-<SHA8>.apk --verify-installed-apk --video --duration 90 --output ./private-paper-evidence`. The source SHA you type is still operator metadata; the script does **not** cryptographically prove a Git commit from an APK. The new `--verify-installed-apk` option fail-closes unless the device's actual installed base APK SHA256 matches the exact `--apk` file, using read-only `adb shell pm path` and `sha256sum`. On devices where shell access to the installed APK is denied, the strict check fails rather than claiming a verified installation. Retain the signed APK metadata, certificate and hash separately. Captured content/logs remain local and private, not uploaded by the tool. Conduct the slow corner/edge drag, reverse/cancel, fast forward/backward, 100-turn stability, background/rotation and 60/120 Hz capture. Record actual fold geometry/shadow/back texture and jank. Compare with #468 on the **same device**.
7. **Do not** claim the lab displays real EPUB pages. Native CurlTextureView currently uses synthetic two-sided fixtures; a production Readium snapshot bridge remains to be implemented, with one locator commit after accepted release.

## Paper acceptance contract

The laboratory qualifies **only as a physical material candidate**. It is REJECTED if the page behaves like Slide, the crease/ink geometry does not bend, cancellation moves the locator, or GL textures leak. Record device, Android/API, refresh, locale, executable SHA, signed APK SHA256, fixture SHA, screen recording, jank P95/P99, memory and reviewer.

Passing this lab still does **not** authorize changing production Paper mode. Only then propose a reversible Readium previous/current/next immutable bitmap bridge with owner-approved physical UX and fault injection under #408.
