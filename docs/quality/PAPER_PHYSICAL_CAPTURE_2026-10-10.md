# Veil Reader — Paper physical capture, 2026-10-10

## Evidence boundary

**This is a local-only device evidence aid, not a Page Engine change, release gate, or physical acceptance claim.**

- Target implementation: Paper [PR #442](https://github.com/hminthakil-eng/Veil-Reader/pull/442), examined head SHA 243888f75232fb0054a4c8253c772736554f1f42.
- Verification branch: [PR #445](https://github.com/hminthakil-eng/Veil-Reader/pull/445). [Android CI](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37985520314), [Forge QA](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37985520311), [Storage](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37985520376) and [Performance](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37985520368) completed successfully. These are emulator/CI results, NOT on-phone optical evidence.
- [Forge QA APK ZIP](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37985520311/artifacts/11644041571) is available. Verify its extracted APK hash, pinned signing certificate, package, version and source identity against the [QA verification artifact](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37985520311/artifacts/11644076558). The ZIP digest must not be represented as the APK hash, and the PR head must not be assumed equal to the compiled merge source.
- quality/physical-device-status.json remains UNVERIFIED. No authorized connected physical phone was available when this aid was prepared.

## Procedure

1. On a Windows/macOS/Linux machine with Android platform-tools, connect and authorize one physical phone over USB debugging. Confirm that `adb devices` lists it. Use a non-sensitive test EPUB. Do not uninstall the existing personal Veil app.
2. Download the Forge QA artifact, extract it and use the separate QA package com.veilreader.app.forgeqa. Verify exact APK identity/signature. Test this build, not a visually similar earlier one.
3. From the repository root invoke:

   python tools/collect_paper_device_evidence.py --source-sha 243888f75232fb0054a4c8253c772736554f1f42 --package com.veilreader.app.forgeqa --video --duration 90 --output ./private-paper-evidence

   The requested SHA is metadata only; the script **does not verify that it matches the installed executable**. Add --apk /path/to/extracted.apk for its local hash, --serial when multiple devices are connected, or --logcat only if expressly opting into private log capture.
4. Open the real test EPUB and record: image-cover edge tap; SVG cover drag; first text-page drag; slow sheet tracking; release, reverse/cancel; center-tap chrome; cross-chapter boundary; rapid repeated turns. Follow up with 100 turns, RTL, 60/120Hz where supported, large text, TalkBack, rotation, background/resume and Reduced Motion.
5. Capture per-case **observed result**, installed binary hash, OS/device/refresh settings, fixture hash, motion/video and fault log as available; separately inspect locator correctness and visual fold physics. Device videos/logs may contain copyrighted/private book content or notifications: review before sharing.
6. No automated upload, app install, force-stop, settings modification, gate promotion or device PASS occurs. Review the evidence manually against [physical gate #408](https://github.com/hminthakil-eng/Veil-Reader/issues/408).

## PASS / FAIL

A mere page-number/locator advance is **not** proof of Paper. Real-sheet deformation must follow the finger, shading/backside/fold must track physical lift, and an acquired GPU sheet must precede committed navigation. A cancelled gesture must not commit; one release may navigate once only; there must be no skipped cover chapter, destination flash, stale frame or Slide substitution. Record visual performance and sustained memory/thermal behavior on the *physical* phone. Keep RED / UNVERIFIED if any gating case fails or has no evidence.

The companion capture helper records read-only device properties and GFX metadata. Screen recording and logcat are **opt-in**, local only. Its four synthetic non-device tests may be run with:

   python -m unittest discover -s tools/tests -p test_collect_paper_device_evidence.py -v

These parser tests passed locally before committing; live ADB recording, optical inspection, and CI execution of this new helper remain unverified.
