# Grayfog V3 review and device capture

## Native cloud frames

The existing test stack renders actual production Composables with Robolectric API 35 and native Skia. Run:

```sh
./gradlew :app:testDebugUnitTest --tests '*GrayfogCloud*Test*' --tests '*GrayfogSystemBarsTest*'
```

Images: `app/build/outputs/grayfog-cloud/<configuration>/<surface>.png`. Eight focused scrolled detail captures are also produced under `app/build/outputs/grayfog-detail/<en|fa>/`; their names distinguish Gallery, Index, Shelves and unknown-date context.
Fifteen configurations × thirty-two surfaces = 480 native frames. The additional cases combine 200% text with 320dp compact, Persian landscape, foldable-width and tablet-width layouts. The debug Compose preview catalog has eleven configurations (352 instances). All use Reduced Motion. `GrayfogCloudRenderTest` defines the exact configurations; `GrayfogReviewSurface` defines the surface names. Dialogs use their actual separate Compose root. Native frame hashes/dimensions and the review contact sheet are recorded with the final V3 verification evidence. These are cloud layout evidence, not device screenshots or golden approval.

## Fictional device specimens

Build/install only the debug APK, then use the debug activity; it reads no real library/history and saves no preferences:

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
ADB=adb tools/grayfog-capture.sh grayfog-device-captures
```

The script launches the independent review task, verifies successful launch and saves PNGs plus launch records. Names are `<surface>-<en|fa>-<1.0|1.3|1.5|2.0>-<standard|contrast>.png`. It captures 32 surfaces × eight language/scale pairs, plus 32 Persian 200% contrast cases (288 files). All are Reduced Motion specimens. The loop uses finite settlement delays, not automatic visual approval. Run separately on phone, tablet, landscape and actual foldable devices, each into a distinct directory; retain `device.txt`. It neither rotates/resizes a connected device nor claims to model its hinge.

For one focused specimen:

```sh
adb shell am start -W --activity-clear-task --activity-new-task \
  -n com.veilreader.app/.ui.review.GrayfogReviewActivity \
  --es surface LIBRARY_INDEX --es locale fa --ef fontScale 2.0 --ez highContrast true
adb exec-out screencap -p > library-index-fa-2.0-contrast.png
```

Execute the existing semantic/touch-target harness on a real attached device:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.veilreader.app.ui.screens.GrayfogShellAccessibilityTest
adb exec-out run-as com.veilreader.app tar -C files -cf - grayfog-review > grayfog-review.tar
```

## Actual publication gate

The debug specimens intentionally do not open their fictional URIs. Appearance is the real instrument panel, but live preview/settlement needs an actual publication. Notebook requires a real Readium/Pdfium owner. Do not substitute a mocked page or a shell frame for this matrix.

Using authorized EPUB/PDF files and the normal app, capture these deterministic names inside a device/configuration directory:

- `epub-paper.png`, `epub-sepia.png`, `epub-dusk.png`, `epub-oled.png`
- `paper-curl-idle.png`, `paper-curl-mid-drag.png`, `slide-idle.png`, `slide-mid-drag.png`, `paged.png`, `scroll.png`
- `pdf-portrait.png`, `pdf-landscape.png`, `pdf-zoom.png`
- `appearance-quick-live.png`, `appearance-advanced-live.png`, `notebook-live.png`, `footnote-live.png`, `selection-live.png`
- `system-bars-shell.png`, `system-bars-paper.png`, `system-bars-dark-reader.png`, `ime-note-editor.png`

Capture with `adb exec-out screencap -p > <name>.png` once the stated state is visible. Mid-drag requires a held gesture rather than a settled swipe. Log actual locale, text scale, high contrast, Reduced Motion, orientation, OS theme and hinge posture beside each group. Cover Persian 100/130/150/200%, high contrast, normal/reduced motion, landscape, tablet and real foldable posture. Check TalkBack/touch exploration and focus order manually; screenshots cannot establish them.

Verify real selection, atomic note saves/exact return, Lookup, TTS/Focus Guide, keyboard/hardware input, ETA uncertainty, PDF gestures/links, progress flush, close/reopen and process recreation with the retained instrumentation/Reader checks. Mark each result independently. No device is attached in the current cloud session, so this entire actual-publication/device matrix remains pending.

## Expanded instruments and selected records

After capturing the default specimens, expand Observatory’s “Show all connections” and Appearance’s “Show reading preview” in constrained layouts. Save `observatory-connections-expanded.png` and `appearance-preview-expanded.png` alongside the same device/configuration record. Collapse and change the selected book to verify disclosure resets; activate Enter Volume to verify exact source return. Native tests cover these callback/state contracts, while actual touch exploration and live publication settlement remain pending.

At 200% landscape, native reachability assertions intentionally scroll to controls. The saved matrix PNG is the initial viewport, captured before assertions; it does not pretend all controls fit in 420dp height.

## Missing metadata and provenance

Capture `library_missing_metadata` in the registry, then switch to Index and Shelves; save `library-missing-metadata-index.png` and `library-missing-metadata-shelves.png`. The single completed fixture belongs to two collections, so its three shelf-register appearances are deliberate. Verify localized Untitled/Unknown author, complete action labels, grouping and reachable records.

In real Book Detail, inspect uncertain/imported annotation dates and positive known dates. Preserve `book-detail-date-unknown.png` and `book-detail-date-known.png` with locale/time-zone metadata. Date unknown must never appear as a manufactured January 1970 record. Use TalkBack heading navigation on Threshold, Archive, Book Detail, Appearance, Notes, Settings and world surfaces; a semantic JVM heading check is not a TalkBack execution result.


## Material-world continuation — 2026-10-05

The registry now contains 36 production surface/component specimens. `tools/grayfog-capture.sh` covers 9 configurations (324 deterministic review capture names). New component names: `reader_access_paper`, `reader_access_dusk`, `navigation_dock`, `navigation_rail`. These supplement the existing live EPUB/PDF/gesture matrix; an isolated Reader access component does not substitute for a publication capture.

First live interaction checks: ordinary-tap Reading menu opens controls; Appearance remains a separate action; disable automatic hiding in Appearance Quick and Settings, reopen the app and verify it stays disabled; dismiss controls explicitly; repeat with active selection and TalkBack. Capture all four reading modes separately and check no bottom access overlap with actual content. No invented publication or persisted fictional reading history is used.

For Castle capture both standard and High Contrast: the latter intentionally omits the illustration. Record real device density, locale, font scale, window bounds and hinge/cutout configuration. Native cloud captures approximate widths, not actual hinge geometry or GPU frame timing.


### Automated Android-emulator review evidence

Storage Instrumentation now uploads `veil-reader-grayfog-shell-review` for seven days on success or failure. Extract `grayfog-review.tar` to inspect the 80 expected inert production-component fixture captures (16 English/Persian × scale × contrast cases; entrance/resume/empty/index/gallery). PDF failure capture is optional and uses the real clean-room publication UI. CI pulls only this named review directory and preserves the original test result. Simulated Compose font scale, emulator rendering and fixture callbacks are clearly separate from the live physical-device matrix above.
