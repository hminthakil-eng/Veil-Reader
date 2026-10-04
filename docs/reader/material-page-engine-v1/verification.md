# Verification — 2026-10-04

The requested remote branch already existed at
`686b1688871f34c03535b1b287fee8ecae818e93`. The current branch integrates that history
with the implementation based on `design/codex-grayfog-masterpiece-v1` at
`77be8b473d79f80deb79c0eb141dfd8c802f8099`. Both previous renderers, debug previews,
and test suites were retained; no force-push or main merge is required.

## Final results

| Gate | Exact result |
| --- | --- |
| Full unit suite | 774 tests, 134 suites, 0 failures, 0 errors, 0 skips |
| Reader-related regression subset | 385 tests, 56 suites, 0 failures/errors/skips |
| New cylindrical mesh suites | 39 tests, 6 suites, all passed |
| Lint Debug | Passed: 0 errors, 181 warnings, no lint checks disabled |
| assembleDebug | Passed |
| assembleDebugAndroidTest | Passed |
| git diff --check | Passed |
| Instrumentation/device execution | 0: no emulator or physical device attached |
| Native review media | 27 PNG frames, 8 release MP4 clips, 8 default-volume WAV cues |

The Reader subtotal selects suite names containing `Reader`, `PaperCurl`, `Slide`
or `Material` and is a subset of the full total. Other related suites (for example
Readium TTS content contracts, Notes/Room writes and restore policies) also ran in
the full suite. No tests were excluded to obtain the green result. Lint warnings
include dependency-update suggestions, existing durable SharedPreferences commit
advice, and Android KTX convenience suggestions; warnings are not hidden.

The final source-comparison Gradle invocation completed successfully in **5m 46s**,
with 95 actionable tasks (20 executed, 75 up-to-date). The previous full baseline
had 764 passing tests; this refinement adds ten tests and retains all baseline suites.
The independently compiled upstream StPageFlip geometry accepted **1,188 finite
cases**; these are separate from the Android test totals.

Evidence: [Gradle log](gradle-verification.log), [machine-readable totals](results.json),
[review gallery](review/gallery.html). XML/HTML unit reports remain under
`app/build/test-results/testDebugUnitTest` and `app/build/reports/tests/testDebugUnitTest`.
Lint XML/HTML remain under `app/build/reports`. APKs are in
`app/build/outputs/apk/debug` and `app/build/outputs/apk/androidTest/debug`.

## Exact cloud command

Toolchain: Gradle wrapper 9.6.0, AGP 9.4.0, full Temurin JDK 21, Android SDK platform
37.0 (compileSdk 37), Android build tools 36.0.0 selected by AGP. The cloud workspace
needed an SDK and a full JDK installed; its initial Java launchers lacked javac.

```sh
JAVA_HOME=/workspace/jdk21 ANDROID_HOME=/workspace/android-sdk GRADLE_OPTS='-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts' ./gradlew --max-workers=2   -I /workspace/scratch/material-test-network.init.gradle   :app:testDebugUnitTest :app:lintDebug   :app:assembleDebug :app:assembleDebugAndroidTest

git diff --check
```

[The cloud init script](cloud-test-network.init.gradle) supplies the environment's
existing proxy and CA trust to forked test JVMs so Robolectric can download its
Android runtime. It changes no test or lint policy. Ordinary local/CI environments
with direct configured network access do not need it. The wrapper, SDK, dependency
versions and toolchain were not downgraded to make tests pass.

## Resolved verification issues

- Initial Java/Robolectric downloads tried direct network connections. Passing the
  existing proxy/trust configuration to the wrapper and test JVMs resolved them.
- Compose's `UnrememberedStateDetector` crashed while inspecting a Kotlin method
  reference in a pure new test. The equivalent lambda fixed the crash without
  suppressing the detector.
- Lint found an unobserved `StateFlow.value` in the new reduced-motion representation.
  Lifecycle-aware collection corrected it.
- Reconciliation exposed a missing PageTurnStyle import in the earlier debug review
  switch. The import was restored.
- Native review exposed a disappearing sheet before its terminal drag position.
  The geometry now retains a sliver through 99% progress; a regression test protects
  that property for every material.
- Earlier debug review continuously forced Paper after choosing Slide/Scroll/Paged.
  Review activation now enters Paper once and leaves subsequent mode choices alone.

## Source-comparison refinements verified

- A temporary native probe reproduced the previous mesh's RTL front-ink leak
  (`0xFFFF0000` RED where the back-facing source was BLUE). The intentionally
  failing baseline is [recorded](evidence/legacy-rtl-leak.txt); the temporary probe
  was removed before this successful full run. Final LTR/RTL ownership tests pass.
- Source-face partitioning conserves full texture area over 984 material/direction/
  corner/progress combinations, with reusable arrays and coherent shared seams.
- Tall/narrow corner pulls preserve the binding until the fold reaches it.
- Reduced/no-capture material is frozen across preference updates; cancel cues
  arrive at release onset; resize rejects an obsolete capture without new persistence.
- Official API hardware support was checked: new triangles use API 29+; API 26–28
  receives live-edge without a snapshot/preview or expensive CPU rendering path.
- Exact idle and terminal pixels plus non-finite metrics/endpoints are protected.
- Final 27 PNG / 8 MP4 / 8 WAV review assets were regenerated and
  [hashed](evidence/review-assets.json). No device instrumentation was claimed.

See [the code comparison](reference-comparison.md) and
[current Arena review](arena-comparison-review.md) for evidence and implementation decisions.

## What remains uncertain

Native Skia software renders do not establish Android GPU driver behavior, sustained
60/120 Hz frame pacing, actual WebView snapshot/render latency, OLED comfort at low
brightness, speaker Foley quality, or OEM actuator identity. Fake-navigator tests
exercise the actual listener but cannot prove every asynchronous Readium callback
on a phone. Instrumentation APK assembly is not instrumentation execution.

Before promoting either material backend to the release default, perform the
[real-device review](review.md) on real EPUB/Persian publications with selection,
Lookup, TTS, Focus Guide, Notes, hardware keys, close/reopen and process death. PDF
must also pass its existing native fixture/reliability instrumentation on a device.
The new mesh switch stays off by default; the earlier debug preview behavior and
legacy release path remain available.
