# W40 — refreshed engineering baseline

Date: 2026-10-03. Source: current canonical W39 HEAD `7d3d7e289e7e2dd895a8bf8772587ee78dd8ce0d`, independently checked against GitHub's branch ref. W39 implementation and the completed PDF audit are preserved, not restarted. W40 is a documentation-only baseline slice; it introduces no user-visible feature or new architecture.

## Actual execution

The initial `testDebugUnitTest` invocation was up-to-date. It was followed by `testDebugUnitTest --rerun` to force the test task itself to execute, then `assembleDebug`, `:app:lintDebug` and `:app:assembleDebugAndroidTest`. Gradle completed successfully in 24 seconds; two tasks executed and 90 dependency/build tasks were up-to-date. Cached compilation/package outputs are valid unchanged-source evidence; they are not represented as a clean rebuild.

- Fresh unit task: **537 tests, 0 failures, 0 errors, 0 skips**.
- Debug build: PASS. APK **46,258,159 bytes (44.12 MiB)**.
- Lint: PASS, **0 errors, 152 advisory warnings**, unchanged from W39.
- All app instrumentation tests: compile/package PASS, runtime **not executed**.
- Existing CI performance-harness task `:benchmark:assembleBenchmarkBenchmark`: PASS, 33 tasks executed in 1m 9s. Native benchmark/Perfetto libraries were packaged unstripped; this is a packaging advisory, not frame-time evidence.
- `sh tools/test-policy.sh`: **39 checks pass**.
- `python -m unittest discover -s tools -p 'test_performance_*.py'`: **15 tests pass**. These validate performance-report tooling, not device performance.
- App compilation was unchanged/up-to-date; fresh benchmark compilation emitted no correctness warning. Lint advisories were inspected by ID: plurals 56, KTX suggestions 30, unused resources 19, durable preference commits 13, dependency/tool updates 16, configuration-width guidance 6 and existing Compose/menu/log/backup-rule style advisories 12. No leak, recycle, coroutine-composition, thread/nullability or API-level error was reported. Advisory absence is not runtime lifecycle proof. No global suppression/baseline or claim that every advisory is fixed.

## Toolchain and runtime assumptions

Gradle wrapper 9.6.0; AGP 9.4.0; JDK 21; source/bytecode Java 17; compile/target Android 37, min Android 26; Build Tools 36.0.0; Kotlin/Compose and dependency versions remain pinned in Gradle files. Robolectric 4.17 needs the existing test-only JDK internal-access export for SDK 37. Linux x86_64 has two CPUs and an 8 GiB memory limit. The local network proxy/system CA configuration is external to the repository.

Readium Kotlin Toolkit shared/streamer/navigator and Pdfium adapter: **3.4.0**. AndroidPdfViewer: **3.2.8**. Transitive PdfiumAndroid: **1.9.8**. Exact public API/native evidence is available in the [completed PDF capability audit](https://github.com/hminthakil-eng/Veil-Reader/blob/research/pdf-annotation-capability-audit/docs/research/PDF_ANNOTATION_CAPABILITY_AUDIT.md).

## Instrumentation and quality blockers

`adb devices` is empty and `/dev/kvm` does not exist. W39 already attempted API 35 ATD software emulation twice: one did not boot after 12+ minutes; another transiently booted but could not sustain package-manager services (`cmd: Can't find service: package`). Repeating that failed setup is not additional verification. No device runtime pass, visual QA, TalkBack interaction pass, renderer process-kill pass, frame/jank/memory benchmark or 1.0-readiness claim is made.

The existing unit suite includes W37 tap ownership/semantic RTL policies, W38 consumed-press/session/accessibility ownership, W39 normalization and rapid-toggle restoration, locator/session durability, final close flush and navigation transaction protection. Compiled renderer/device tests remain release blockers until a usable device executes them. GitHub jobs with `steps=null` are not evidence.

## Next isolated slice

W41: PDF reliability/navigation, using only the public existing provider/configurator and Readium navigator APIs. Investigate the default native-link destination/view-page mismatch for RTL and capture previous-location history for PDF links. Do not add annotation-authoring UI: the capability audit classifies authoring/enumeration/save as adapter-extension work, outside automatic category-A implementation.

W39 PR #364 remains draft and unmerged. W40 baseline and each later slice remain independently reviewable; no automatic merge.
