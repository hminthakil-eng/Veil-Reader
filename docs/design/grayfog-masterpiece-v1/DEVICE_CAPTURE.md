# Grayfog V3 review and device capture

## Native cloud frames

The existing test stack renders actual production Composables with Robolectric API 35 and native Skia. Run:

```sh
./gradlew :app:testDebugUnitTest --tests '*GrayfogCloud*Test*' --tests '*GrayfogSystemBarsTest*'
```

Images: `app/build/outputs/grayfog-cloud/<configuration>/<surface>.png`.
Eleven configurations × thirty-one surfaces = 341 native frames. All use Reduced Motion. `GrayfogCloudRenderTest` defines the exact configurations; `GrayfogReviewSurface` defines the surface names. Dialogs use their actual separate Compose root. Native frame hashes/dimensions and the review contact sheet are recorded with the final V3 verification evidence. These are cloud layout evidence, not device screenshots or golden approval.

## Fictional device specimens

Build/install only the debug APK, then use the debug activity; it reads no real library/history and saves no preferences:

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
ADB=adb tools/grayfog-capture.sh grayfog-device-captures
```

The script launches the independent review task, verifies successful launch and saves PNGs plus launch records. Names are `<surface>-<en|fa>-<1.0|1.3|1.5|2.0>-<standard|contrast>.png`. It captures 31 surfaces × eight language/scale pairs, plus 31 Persian 200% contrast cases (279 files). All are Reduced Motion specimens. The loop uses finite settlement delays, not automatic visual approval. Run separately on phone, tablet, landscape and actual foldable devices, each into a distinct directory; retain `device.txt`. It neither rotates/resizes a connected device nor claims to model its hinge.

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
