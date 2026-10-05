# Cloud verification — Grayfog continuation / PR #371

> Latest artistic wave: physical-book-led Threshold, calmer object-led Archive retrieval, and Castle with principal halls, side chambers and opposing tablet wings. Tested executable source: **3686fb77b5adb760b43bd51115ec68e50efc9c63**. All three GitHub gates succeeded; raw CI independently confirms **729/130**, zero failures/errors/skips, lint **175 warnings + 1 hint**, zero errors. Local policy **39**; native review **540 + 8** fresh images. **Draft PR #371; not 100/100 or physical DEVICE-GREEN.** Scroll to the authored-world sections for selected actual renders and exact limitations. Earlier evidence below is historical.


2026-10-03. Same branch `design/codex-grayfog-masterpiece-v1`; canonical base `18fe4ab78b381ba44fbbfc1817a00d3f8253bfcf`. Continuation parent `5d35d6556a0a70f9f3aa500a718031453267ee3b`. No historical branch or main changes.

## Executed checks

Final combined run: **BUILD SUCCESSFUL in 6m 43s**. The final source inputs and raw outcomes are pinned in [verification.json](verification.json). Earlier 682-test evidence belongs to the first design pass and remains in Git history; it does not substitute for this continuation's verification.

| Check | Result |
|---|---|
| Full JVM suite, including Reader regressions | 684 tests / 122 suites, zero failures, errors or skips |
| Design system / constitution / art direction | 31 tests within full suite; adds current-artifact cover hierarchy and restrained paper contrast budget |
| Reading policy | 39 checks passed |
| Compose / debug app | Compiled and packaged for verification |
| Instrumentation APK | Compiled and packaged; expanded shell harness has 64 parameterized cases, **not device-executed** |
| Android lint | Zero errors/fatal findings; 166 warnings and 1 hint retained in the raw report |
| Ownership comparison | Data/domain/Room/schema, Reader owner/TTS/navigation, geometry/listeners/states/input arbiter unchanged from continuation parent |
| Paper source boundary | Only three PaperCurlDraw fibre/highlight constants changed; no geometry, thresholds, velocity, buffers, settlement or persistence change |
| Diff whitespace | git diff --check passed |
| Font packaging | Eight existing offline font binaries and license/provenance files retained; publication typography independent |
| Device / rendered visual acceptance | **Pending**; adb devices lists no attached device; no /dev/kvm |
| Performance / release | Not executed; no release build, merge or performance-green claim |

## Commands

JDK 21, Gradle wrapper 9.6.0, SDK Platform 37.0. Project versions, dependencies, schema and build policy unchanged.

```sh
ANDROID_HOME=/workspace/android-sdk JAVA_HOME=/workspace/jdk21 \
  JAVA_TOOL_OPTIONS='-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts' \
  ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest \
  --no-daemon --max-workers=2
sh tools/test-policy.sh
git diff --check
/workspace/android-sdk/platform-tools/adb devices
```

The source was reviewed in composition, instrument/material and micro-hierarchy slices. The first slice compiled and passed the then-current full JVM suite; the second compiled. An intermediate combined run observed a shared-token edit while compiling and was not accepted. A later combined run passed but included stale-position lint quickfix exceptions after a source edit; its raw log is retained as intermediate evidence. Final acceptance uses only the repeat after the ledger paint-order correction, direct Series retrieval correction, connected Persian specimen and source freeze. No warnings were suppressed and durable commit semantics were not changed to satisfy lint.

Native previews expose thirteen actual screen/component specimens across seven review configurations. They compile but have not been rendered/approved. The Gallery specimen is an isolated real component, not a claim of a full-screen capture. Real publications are required for Sanctuary evidence; no fake Reader was introduced.

## Required Android visual and interaction gate

Every item below remains **pending**. Record device model, Android version, window bounds, locale, font scale, contrast/motion mode, fixture and reviewed screenshot paths before closing a row.

| Surface / condition | Status |
|---|---|
| Threshold active | Pending |
| Threshold empty | Pending |
| Library Gallery | Pending |
| Library Shelves | Pending |
| Library Index | Pending |
| Search / IME / sorting / filtering | Pending |
| No-results and reset | Pending |
| Book Detail / real cover ratios / unavailable publication | Pending |
| EPUB Paper | Pending |
| EPUB Sepia | Pending |
| EPUB Dusk | Pending |
| EPUB OLED | Pending |
| Paper Curl mid-drag / cancel / settle | Pending |
| Slide mid-drag / cancel / settle | Pending |
| Paged | Pending |
| Scroll | Pending |
| PDF portrait | Pending |
| PDF landscape / native links / pinch | Pending |
| Appearance Quick / live draft | Pending |
| Appearance Advanced / capability gates / settlement | Pending |
| Notes / Highlights / bookmarks / editor IME | Pending |
| Echoes / Time Capsules / Reading Memory / unknown history | Pending |
| Profile | Pending |
| Observatory connected / isolated / dense / selected | Pending |
| Castle / room routes / locked state / memory architecture | Pending |
| Path | Pending |
| Ritual / cancel / confirm / skip / Reduced Motion | Pending |
| Sanctum / earned and locked museum records | Pending |
| Loading | Pending |
| Error / localized recovery / dismissal | Pending |
| Persian 100% | Pending |
| Persian 130% | Pending |
| Persian 150% | Pending |
| Persian 200% | Pending |
| High Contrast | Pending |
| Reduced Motion | Pending |
| Landscape | Pending |
| Tablet | Pending |
| Foldable / resizing | Pending |
| TalkBack / focus order / touch exploration | Pending |
| Selection / atomic Notes / TTS / Focus Guide / Lookup / ETA | Pending |
| Close/reopen / background / process death / durable location | Pending |
| Long reading session / texture comfort / frame performance | Pending |

Source review does not establish actual shaping, visual contrast over artwork, page physics, native PDF behavior or reference fidelity. PR #371 remains Draft. **DEVICE-GREEN and visual completion are not established.**

## V3 native review continuation — 2026-10-03

V2 evidence above is historical. V3 continues from design head `0888bf036c786ee03b0c35b6aa9004aa642b5933` and preserves the subsequently fetched Codemagic-only remote commit `56bc0faa823c5295bcf1367dae2832de88edc965`. The canonical integration base remains unchanged. Final V3 results, source SHA-256 inputs, individual suite totals, APK hashes and all 330 native frame hashes/dimensions are recorded separately in [v3-verification.json](v3-verification.json). Raw final Gradle, lint XML and policy outputs accompany that manifest; V2 reports are retained.

The review now uses actual production Composables through thirty fictional debug-only states, 210 Compose preview instances and eleven native cloud configurations. Native Skia/Robolectric API 35 produced 330 PNGs across compact/wide/landscape, English/Persian, 100/130/150/200% and high contrast cases. Every fixture uses Reduced Motion. Frame production checks nonempty image dimensions; it is not a golden comparison or blanket visual approval. The native tests additionally assert immediately visible/actionable Continue Reading and Profile Settings for the matrix; focused Gallery tests check independent 48dp utility targets and callback ownership at English compact and Persian 200% text. Two window tests check both icon fields and independent dialog-window contrast. The contact sheet is [native-review-phone412.jpg](native-review-phone412.jpg); originals can be reproduced using [DEVICE_CAPTURE.md](DEVICE_CAPTURE.md).

Native review exposed and corrected real source gaps: empty fixtures masquerading as active imported books, incorrectly capturing the underlying root instead of dialogs, Profile Settings displaced by an unweighted identity column, Gallery three-column crowding, repeated per-book shelf plinths, unsupported API-27 XML navigation-bar flags on minSdk 26, and Persian metadata inheriting Latin metrics in an English shell. Intermediate compilation/lint failures were corrected, not suppressed, and are not acceptance evidence. The final frozen-source combined run is the acceptance record.

The Compose JVM test dependency reuses the existing Compose test version and Robolectric stack; no production rendering framework/dependency or duplicate UI was introduced. Data/domain/schema, Reader ownership/TTS/navigation and all PaperCurl drawing/geometry/state/input files are unchanged from this continuation's design parent. Appearance changes preserve live draft settlement and the distinct Paged/Scroll and Paper/Slide/None controls; publication typography is independent from shell typography.

**Actual device/publication acceptance remains pending for every row in the matrix above.** Native cloud rendering does not establish real Readium/Pdfium gestures, mid-drag physics, OS system pixels, TalkBack, actual hinge posture, performance or physical typography. The dense 24-book Observatory fixture deliberately exercises 276 source-derived links: optical edge hierarchy and real touch selection remain open questions. No release build, DEVICE-GREEN, final visual acceptance or merge is claimed. PR #371 remains Draft.

The last folded-width correction also scales the floor rail with text size and reserves a readable adjacent Castle record before assigning map dominance. An intermediate verification run was deliberately cancelled before editing that refinement; only the complete subsequent frozen-source run is final evidence.

### Final V3 results

Frozen-source combined run: **BUILD SUCCESSFUL in 9m**. Full JVM suite: **702 tests / 125 suites, zero failures/errors/skips**, including **34 constitution/typography/art-direction checks**. Reading policy: **39 passed**. Lint: **zero errors, 177 warnings and one hint**, retained verbatim rather than suppressed. Debug app and instrumentation APK packaged successfully. `git diff --check` and `bash -n tools/grayfog-capture.sh` passed. Eight offline shell fonts and five provenance/license assets were byte-compared with the packaged APK. No physical Android device is attached.

The final folded-width correction is visible in [native-review-castle720.png](native-review-castle720.png). All 330 original native frame dimensions and hashes are in the V3 manifest; only a compact review sheet and the targeted Castle original are checked in. Reproduce the full set using the documented native test command. Remote CI-only changes are preserved without modifying verified application source inputs.

## Next continuation — 2026-10-04: long Persian Threshold

Continues `f8300095737156f045d993ac2cc11d0b2c697c78`. Earlier V3 evidence above remains historical. The latest record is [next-verification.json](next-verification.json), with separately retained final Gradle/lint/policy reports, exact source/APK hashes and 341 native frame hashes/dimensions.

The new long Persian current-volume fixture exercises the real Threshold composition with long title/author/series and Arabic chapter metadata. Both the existing active fixture and this fixture assert an immediately displayed actionable return in all eleven native configurations. Targeted native/Gallery run passed 13 tests; the registry now produces 341 frames. The inspected [Persian 200% native capture](native-review-threshold-persian200.png) shows a fully visible reading action directly after the bounded title and before secondary metadata. This is native layout evidence; real typography, TalkBack, physical covers and all publication/device gates above remain pending.

Final frozen-source combined run: **BUILD SUCCESSFUL in 6m 28s**. **702 tests / 125 suites, zero failures/errors/skips**; 39 reading-policy checks; lint with **zero errors, 177 warnings and 1 hint** retained. Debug app and instrumentation APK packaged successfully. `git diff --check` and capture-script Bash syntax passed. Canonical Reader/data/navigation/schema and PaperCurl drawing/geometry are unchanged from this continuation’s parent. Device acceptance remains pending.

## All-out continuation — 2026-10-04

Continues published design head `77be8b473d79f80deb79c0eb141dfd8c802f8099`. Earlier evidence remains historical. Current evidence: [allout-verification.json](allout-verification.json), final Gradle/lint/policy reports and four focused native PNGs alongside this document.

Final corrected frozen-source run: **BUILD SUCCESSFUL in 8m 11s**. **713 JVM tests / 127 suites, zero failures/errors/skips**; **35 design-system/constitution/art-direction tests**; **39 reading-policy checks**. Lint: **zero errors, 177 warnings and 1 hint**, retained verbatim. Debug app and instrumentation APK builds passed. `git diff --check` and `bash -n tools/grayfog-capture.sh` passed. No release build or attached Android device.

Native matrix: **31 actual production surfaces × 15 configurations = 465 frames**. Four new combined cases exercise compact 320dp/200%, Persian landscape/200%, foldable-width/200% and tablet-width/200%. The debug preview catalog now has **341 instances**. Gallery utility/search accessibility tests: four; Appearance disclosure ownership: two; Observatory full-ledger retrieval/reset/exact reading return: two; render configurations: fifteen. Normal 100% landscape was also inspected after switching adaptive height to actual window size.

Initial captures precede scroll assertions; 200% landscape checks reachable actions through scrolling. These are nonempty native renders and targeted semantics/callback checks, not golden approval or blanket optical acceptance. Focused PNGs show compact retrieval, constrained Persian Appearance, Castle rooms and dense Observatory selection. Source inputs, all 465 frame dimensions/hashes, suite totals and both APK hashes are in the manifest.

An earlier full run exposed three added ConfigurationScreenWidthHeight warnings. All three were corrected through actual window measurement, without suppression. One intermediate rerun was explicitly cancelled before the remaining correction; only the complete subsequent run above is acceptance evidence. ReaderScreen changes are confined to the Appearance presentation/disclosure layer and its window import. Draft/slider/disposal settlement remains unchanged. Canonical data/domain/navigation/Reader ownership and Paper Curl/Slide drawing, geometry and input/state files are unchanged from this continuation’s parent.

**PR #371 stays Draft. DEVICE-GREEN and final visual acceptance remain unestablished.** Every real-publication/device gate above still applies. Next evidence must resolve compact cover balance, long Persian artifact proportions, dense node touch selection, real hinges, preview discoverability/live settlement, system bars/IME, TalkBack, paper/slide mid-drag and long-session comfort. Fictional debug histories do not establish production history.

### Published-source GitHub Actions status

Application candidate `0772c4adc7a682ee2a5a5e8f1e68e99e1ca923e6`: Android CI run `37181420749` / job `111374608440`; Storage Instrumentation run `37181420735` / job `111374608611`; Performance Benchmarks run `37181420707` / job `111374608342`. Each reports failure with **zero executed steps and no job logs**. This is recorded as an infrastructure/account blocker before code execution, not a code regression or CI-GREEN. The connector does not expose the underlying runner/billing reason. Exact returned metadata is retained in [allout-github-ci.json](allout-github-ci.json).

The cloud frozen-source tests/lint/APKs above remain independent evidence. This status-only documentation update does not alter any verified application/test input.

## Detail continuation — 2026-10-04

Continues `438a967a074484aec373a8edc15eeca467ffaa13` on the existing Draft PR. Earlier manifests/reports remain historical. Current exact source inputs, suite totals, APK hashes, 480 matrix frame hashes/dimensions and eight focused capture hashes/dimensions are in [detail-verification.json](detail-verification.json). Raw final Gradle/lint/policy outputs are retained separately.

Final frozen-source combined run: **BUILD SUCCESSFUL in 4m 24s**; **722 tests / 128 suites, zero failures/errors/skips**, including **36 design-system checks** and eight new native detail methods. **39 reading-policy checks passed**. Lint: **zero errors, 176 warnings and one hint**, retained. Debug app/instrumentation APK tasks completed successfully, with unchanged production inputs remaining up-to-date after focused capture-harness refinement. `git diff --check` and capture-script Bash syntax passed. No physical Android device is attached.

Native review: **32 production surfaces × 15 configurations = 480 matrix frames**, plus **eight focused scrolled captures** of unknown dates and missing metadata across Gallery/Index/Shelves in English and Persian. **352 Compose preview instances** and **288 deterministic future device captures**. These are actual Composables, not duplicated UI or goldens. Initial matrix frames precede scroll assertions. The new native tests check navigable headings across eleven surfaces, missing-cover caption height, truthful fallback identities in all three modes and explicit unknown annotation dates without fabricated epoch dates. Focused captures additionally assert scroll reachability.

Source review preserved the cover image decoder/cache, artwork, fade/reduced-motion behavior and accessibility de-duplication. ReaderScreen changes are only the heading import and Appearance title semantics. Canonical Reader/data/domain/navigation/schema and Paper/Slide geometry, drawing/input/state are unchanged. Display-title fallback does not write metadata or change IDs, URI, search index or callbacks. Date formatting does not write timestamps/history.

Intermediate failures exposed missing Notes/Settings headings and a mistaken one-book/one-shelf test expectation; both were corrected. A subsequent capture test attempted to scroll to a virtualized mode label; it now returns through the existing lazy viewport before selecting the mode, retaining reachability assertions. No product grouping, virtualization or ownership was altered to satisfy tests. Only the final complete run above is acceptance evidence.

**PR #371 remains Draft; final visual acceptance and DEVICE-GREEN are not claimed.** Current device/publication gates remain pending: physical glyph shaping and contrast, missing-cover proportions, real TalkBack heading traversal, true landscape/hinges, actual publication live appearance, TTS/selection/PDF, page physics and long-session comfort. The prior zero-step GitHub Actions infrastructure/account blocker remains recorded; cloud verification does not manufacture CI-GREEN.

### Detail published-source Actions observation

Source candidate `f0d571e70abb6776764ed5b3429cf0920a2b9c02`: Android CI run `37217491118` / job `111480875315` and Storage Instrumentation run `37217491147` / job `111480875242` failed before execution, with zero steps and no job logs. Performance Benchmarks run `37217491114` / job `111480875324` remained queued at this observation, also with no steps. The failed jobs are infrastructure/account-blocked before code execution; the underlying runner/billing reason is not exposed. The queued job is pending, not passed or classified as a code failure. Exact metadata is retained in [detail-github-ci.json](detail-github-ci.json). This status-only follow-up changes no verified application/test input.


## Material world / ordinary-tap Reader access — 2026-10-05

Continuation parent `b3137ee11cc7638afc1d5c9f5bd6e64c726eec98`; same design branch, canonical base `18fe4ab78b381ba44fbbfc1817a00d3f8253bfcf` and Draft PR #371. This record supersedes earlier totals only for this continuation; historical logs and manifests remain intact.

Final combined command: `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --max-workers=2`, **BUILD SUCCESSFUL in 6m**. JDK/SDK/proxy configuration remains the command documented above. [Frozen-input manifest](material-world-verification.json), [raw Gradle log](material-world-gradle-verification.log), [lint XML](material-world-lint-results-debug.xml) and [reading-policy log](material-world-reading-policy.log) pin exact inputs and outputs.

| Check | Exact result |
| --- | --- |
| Full JVM suite | 729 tests / 129 suites; zero failures, errors or skips |
| Design system / constitution / art direction | 36 checks within the full suite |
| Reader regression subset | 291 tests / 42 suites within the full run; manifest lists names and selection criterion |
| Reading policy | 39 passed |
| Lint | Zero errors/fatal findings; 174 warnings and 1 hint retained, no new suppression/baseline |
| Debug / instrumentation APK | Both assembled; exact hashes and byte counts in manifest; instrumentation **not device-executed** |
| Native review | 540 fresh frames (36 production specimens × 15 configurations), plus eight focused detail captures; all matrix file timestamps validated against this run's render-suite start |
| Compose annotations / future device script | 352 annotation preview instances (32 × 11); all 36 registered specimens available through review activity/native rendering; 324 future deterministic device capture names |
| Whitespace / capture script | git diff --check and bash -n tools/grayfog-capture.sh passed |
| Real device | adb lists no attached device; DEVICE-GREEN / physical gesture / visual acceptance remain pending |

The full suite includes persistence of the menu preference across SettingsStore recreation, its independence from publication/input preferences, the explicit auto-hide-off policy, ordinary-tap menu/Appearance ownership and 48dp targets at English 100% and Persian 200%, filter selection/dismissal, and localized Castle floor digits. Existing durable-progress, close, locator, selection/Notes, Readium/Pdfium, TTS and hardware-key regressions remain in the suite. This is source/cloud regression evidence, not a substitute for live publication/device tests.

Intermediate attempts were not accepted: a native Robolectric run aborted during `java/nio/FloatBuffer` JNI initialization; review artwork now initializes/validates its real resource on the UI thread before asynchronous BookCover loading. The exact native classloader root cause is not proven by a passing repeat. Lint then caught drawable-as-raw resource loading; a hidden asset API attempt did not compile and was replaced with public BitmapFactory decoding and PNG cache encoding. A successful complete run then exposed a one-book plural wording defect in the accessibility description; neutral count wording and removal of two unused legacy header resources were followed by this final combined rerun. No test was skipped and no warning was suppressed to manufacture green results.

Native fixtures deliberately use Reduced Motion and fictional inert records. Reader access captures contain only the real component on an empty field, never invented publication pages. Window widths simulate adaptation, not real fold hinges. Complete/cancel/flick frames, Readium/Pdfium integration, sound, haptics, GPU pacing, TalkBack touch exploration, system bars/cutouts/IME and actual imported covers require the live device matrix in [DEVICE_CAPTURE.md](DEVICE_CAPTURE.md).

Selected fresh captures: [Threshold](material-world-threshold.png), [Persian 200% Library](material-world-library-persian200.png), [Castle](material-world-castle.png), [Persian foldable Castle](material-world-castle-persian-foldable200.png), [High Contrast Castle](material-world-castle-high-contrast200.png), [Reader access component](material-world-reader-access.png). Optical questions and the source-candidate comparative critique are recorded in AUDIT and REFERENCE_REVIEW; no final visual acceptance is claimed.


## Published CI findings and isolated follow-up — 2026-10-05

The published application candidate `d9d3e52f40346d8e1b6de68126327e989386efcc` received actual allocated-runner execution. [CI evidence](material-world-published-ci.json) records IDs and exact outcomes: Android CI aborted with `java/nio/FloatBuffer` JNI failure (441 partial tests, one unfinished case marked skipped by the aborted worker); Storage Instrumentation ran 132 tests with 17 failures (16 Threshold fixture cases and one native PDF-link case). These are **not** zero-step account/infrastructure failures. The preceding local success did not establish CI-green.

The follow-up changes test/review infrastructure only: every JVM test class gets a fresh process (`forkEvery = 1`) so native bindings cannot survive between SDK 37 persistence and SDK 35 graphics sandboxes. UI resource validation alone had been insufficient. The exact upstream JNI root cause is not claimed as proven. All classes still execute; no filters, ignored tests or native rendering disablement were added. The Threshold fixture now has the same fictional imported-source marker as native fixtures; its previous missing source legitimately produced an unavailable-book action. PDF QA reacquires the attached renderer after layout/fit changes and maps link bounds with the renderer's native page/secondary offsets instead of hand-scaled coordinates. Assertions still require destination page 2 and exact return to page 0; production PDF ownership/navigation is untouched.

Local follow-up: full isolated combined run **10m 2s**, 729 tests / 129 suites, zero failures/errors/skips, including 36 design checks and 291 Reader-prefix/subtree regression cases; 540 fresh matrix frames and eight focused frames. Final packaging repeat after instrumentation-only edits **13s**, `testDebugUnitTest` correctly up-to-date, lint zero errors (174 warnings + one hint retained), both APKs assembled. Both raw runs are retained: [full run](material-world-isolated-gradle-verification.log), [packaging repeat](material-world-isolated-packaging.log), [exact frozen inputs/APKs/frames](material-world-isolated-verification.json), [lint](material-world-isolated-lint-results-debug.xml), [policy](material-world-isolated-reading-policy.log). Reading policy: 39 passed. Both capture scripts pass bash syntax checks; git diff --check passes. Fresh follow-up GitHub execution is still required at this record.

The original published candidate's Performance Benchmarks run succeeded: API 35 / lavapipe emulator, packaged Baseline Profile verified, startup median 757.815 ms, reader gfx p95/p99 200 ms, frame count 68.5, absolute **smoke** budgets passed. Relative PR delta was **skipped because no persisted main baseline exists**. These generous emulator budgets are not a real-phone frame-pacing or performance-green claim. This follow-up changes no production application/engine source.

CI now collects only the inert `files/grayfog-review` directory before emulator shutdown and uploads `veil-reader-grayfog-shell-review` for seven days even on test success. Test exit status is preserved. Expected shell fixture captures: 80 (five captures × 16 locale/scale/contrast cases); PDF QA optionally adds a real failure screenshot. These are Android-emulator evidence of production Composables, with simulated Compose font-scale configurations, not physical device acceptance or real user history.

## New zero baseline wave — 2026-10-05

Remote `73781d63922ac7c6939f22b682dc6e524265aa7a` CI is **not green**: Android CI run 37264904506 aborted native worker 91 resolving `java/nio/FloatBuffer` (724 reported tests, one abort-unfinished case); storage run 37264904513 reported 132 tests / 13 failures / 0 skips. Twelve failures require introduction copy that the production large-text adaptation deliberately omits; one is the real PDF link destination gate. Performance run 37264904507 completed successfully; this is emulator evidence, not physical-device performance acceptance.

Local targeted first check: DesignSystemTest 15 + GrayfogCloudAccessibilityTest 9 = **24 tests, 0 failures/errors/skips**, 49 seconds including instrumentation compilation. Separate APK/instrumentation compilation and assemblies succeeded in 15 seconds. Reading policy: **39 checks passed**. Shell syntax and git diff whitespace checks passed. These do not supersede the remote failures.

A follow-up native rendering/accessibility plus lint/APK wave is running; its outcome and fresh-frame totals must be recorded after completion. No remote screenshot-export success, remote JNI repair, PDF repair, or final visual acceptance is claimed yet. Full final source-head verification remains required after the remaining fixes.

Follow-up outcomes before the final working-tree run: the native/accessibility + lint/APK wave completed in 7m19s, producing 540 new frames, but visual inspection exposed fallback artwork after the fixed delay. A readiness-based check then reproduced the same JNI abort locally; it is a real failure, not infrastructure or an intentional skip. Fully decoding the debug reference resource before handing its cached file to the actual asynchronous BookCover path restored all 9 accessibility cases (35 seconds, no failures/errors/skips). The real-cover readiness render showed actual original artwork.

The final working-tree full testDebugUnitTest/lintDebug/assembleDebug/assembleDebugAndroidTest run is now underway with readiness-based captures and typographic fallback covers. No full-head success or remote repair is claimed until the corresponding results exist.

PDF gate follow-up: reviewed the actual Compose Material3 1.4.0 source (`commonMain/androidx/compose/material3/ModalBottomSheet.kt`, official Google Maven sources). Expanded sheets with a partial state respond to dismissal/Back by `partialExpand()`. The instrumentation test previously assumed one Back removed the PDF controls. It now clicks the production Back-to-reading action (`onDone` clears `showPdfZoom`) and waits for UI idle before the native link tap. This is a source-backed test correction; remote destination/return success remains unverified. The strict page 2 → previous-location → page 0 assertions, viewport mapping and failure screenshot export remain. No production Pdfium/Readium navigation or gesture change.

Final local zero-baseline wave: full `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` succeeded in **13m19s**. **729 tests / 129 suites / 0 failures / 0 errors / 0 skips**; design subset **36**, Reader regression subset **291 / 42 suites**, reading policy **39**. Lint: **0 errors, 174 warnings, 1 hint**, with no suppression. Native review: **540 freshly generated frames**, plus **8 focused detail frames**. A subsequent packaging/lint check succeeded in **6m47s**, with unit tests UP-TO-DATE, and rebuilt the final instrumentation APK after the explicit PDF return-action correction. Raw logs, lint XML, source/APK/frame SHA-256 and suite totals are under `zero-baseline-*`; shell syntax and `git diff --check` passed.

Selected new images are `zero-baseline-threshold.png`, `zero-baseline-threshold-persian200.png`, `zero-baseline-library.png`, and `zero-baseline-library-persian200.png`. Actual artwork is present; the former fixed-delay fallback is no longer accepted as a real-cover capture. These are native Skia review images, not physical Android acceptance.

Remaining source/design work: Castle still repeats chamber panels; Gallery retrieval controls consume much of the narrow 200% viewport; parchment/action object proportions and cross-screen spatial grammar need further rendered refinement. The current work is a new starting baseline, **not source-final, not 100/100 and not DEVICE-GREEN**. New remote CI JNI/PDF/capture-export results remain required. Preserve the draft PR and all reading owners; do not merge.

## Open-world wave — verification in progress, 2026-10-05

Previous remote head `ed24d8ad79209c38b445bf95ec34e1b2c1891e41`: Android CI 37276932198 failed with a native FloatBuffer JNI abort. Storage 37276932197 ran 132 cases: 64 failed in Grayfog screenshot export, 68 passed, including native PDF links/navigation. Performance 37276932196 passed on the emulator only. The local published 729-case result does not override these actual Cloud failures.

Current uncommitted source: the complete nine-case native GrayfogCloudAccessibilityTest and assembleDebugAndroidTest passed in 29 seconds after explicit Castle cover-readiness teardown. Reading policy: 39 checks passed. Full unit/lint/debug/instrumentation packaging and fresh native matrix are in progress; do not reuse previous wave totals as this source's evidence. Capture export requires the next actual Android run. No physical device has been used.

Follow-up: the first full wave reproduced FloatBuffer despite Castle readiness; it was interrupted after the native abort, so no full-green result is claimed. Search production-screen cases are now a separate native test class under existing per-class process isolation; all nine cases (seven component/chamber cases and two full-screen search cases) passed together in 40 seconds. No native test, assertion or coverage was removed. A fresh complete verification wave is required; prior totals remain historical.

The open-world source candidate `d40117310067f1dc6fc36c401f073a5a237e16d8` completed the full local wave in 12m45s: 729 tests / 130 suites / zero failures, errors or skips; 36 design checks; 291 Reader regression cases / 42 suites; 39 reading-policy checks; lint 174 warnings + 1 hint / zero errors, with no suppressions; both APK assemblies succeeded. All 540 native frames were freshly produced, plus 8 detail frames. Exact source/APK/frame hashes and raw logs are in `open-world-verification.json`, `open-world-gradle-verification.log`, `open-world-reading-policy.log`, and `open-world-lint-results-debug.xml`.

Android CI 37280887283 and Storage Instrumentation 37280887310 both succeeded at this exact tree. The tested PR merge tree `8f63a12e12668285819e64b8eefd211e839147c0` was fetched and verified equal to source tree `96a4ad13d167679cf46879c59c20da585ca0ab40`. Storage produced an actual 80-PNG artifact (11332492252; 33,423,297 bytes), downloaded and inspected. This proves the shell-owned capture export works on API35 lavapipe. Success-run XML was not retained by the historical workflows, so exact per-case remote totals are not independently claimed from missing XML. This continuation retains successful raw unit/lint and instrumentation XML for future exact accounting.

Actual emulator Gallery inspection exposed forced blank title/author/state lines in large-text mode; those are now removed without lowering maximum wrapping lines or touch targets. Full-screen native Library inspection exposed duplicate counts making retrieval utilities wrap: the extra count now appears only when search/collection/series further narrows the reading-state set. A fresh final retrieval wave is in progress. No physical-device acceptance or 100/100 claim follows from the successful emulator/source gates.

### Native search coverage ownership correction

The first retrieval verification reproduced the intermittent JNI DoubleBuffer abort in the first full-screen search case even after separating its class. It reported 728 cases / 130 suites, with one unfinished case represented as skipped after SIGABRT; it was not a green result. The native render matrix itself completed all 15 cases and regenerated 540 images; the Persian 200% Gallery now places all three utilities in one row and exposes the first publication title earlier.

An isolated `-Xcheck:jni` diagnostic completed but recorded unchecked JNI-exception warnings specifically in Robolectric's native BitmapFactory.decodeFileDescriptor binding reached by BookCover's IO coroutine. There is no evidence of a physical-Android missing FloatBuffer/DoubleBuffer or Reader-core failure. Do not change production dispatchers or graphics ownership to satisfy this shim.

Search-label assertions now use the real production missing-art path in their native JVM fixture. The actual original-art decode/filter lifecycle has an explicit new Android instrumentation test across all 16 language/font-scale/high-contrast configurations. It validates the real cached PNG, awaits actual artwork after scrolling, types into the production search field, verifies the original volume leaves the results, preserves the search label/48dp target, and captures the result. This preserves the search assertions and strengthens asynchronous cover coverage on actual Android native runtime rather than discarding it. The existing 540 native render matrix still includes original artwork. The 16 new cases must actually run remotely before this strategy is considered verified.

The debug review entry point gains only an artwork-fixture switch, defaulting to original art; no duplicate production tree, release fixture, fake history, new image dependency or production decoder change is introduced. A fresh full verification is running.

## Authored halls and physical resume composition — 2026-10-05

The user's latest instruction explicitly prioritizes aggressive artistic improvement. Actual production-component render review drove this wave: Castle's identical centered icon badges and dark chamber panels still read as reward tiles, while the compact Threshold stacked the physical book below its reading action.

Castle now gives Grand Library and Observatory full hall spans on compact layouts; smaller chambers remain beside the corridor. Wide layouts preserve their meaningful opposing wings and central stair. Room identities become left/start-aligned entrance inscriptions with unboxed instrument glyphs, natural title/body grouping, and a quiet memory mark on the threshold. The material exposes more of the original architecture; High Contrast remains opaque. A repeated Inner Keep eyebrow and redundant sealed footer are removed; actual floor state, unlock-rank body and complete TalkBack room description remain. No rank, route, relationship or progression is invented or changed.

Threshold now budgets a physical cover down to a semantic 84dp minimum before opting into the existing full-width large-text path. At normal 320dp, the book stays beside its title. A repeated Continue Reading label and divider are removed so publication identity starts the parchment directly. Side-by-side identities may wrap to four lines; stacked large-text retains its previous three-line measure and early reading action. Metadata, truthful progress, actual reading callback and minimum action size are preserved. Render inspection covers actual compact 320, phone 412, Persian 200% and tablet production compositions.

The broader native failure was reproduced in GrayfogCloudDetailTest, not the search class. That test captured or replaced a scene before actual asynchronous cover readiness. A shared test-only artwork await now covers IO completion, fade/layout advancement and a second readiness check before capture or scene replacement. The actual original-cover JVM search fixture is restored; the temporary debug artwork switch is removed. Detail and original-art search together passed all 10 targeted cases in 56s. Original-art renderer coverage and the 16 added Android search cases remain. This is a systematic fixture lifecycle correction, not a production dispatcher/decoder workaround. Full verification at the latest source is still required.

At published head 22b9074, Android CI 37284325323 and Performance 37284325278 passed; Storage 37284325152 ran 148 cases with 16 failures / zero errors or skips. All 16 were new search-fixture ActivityResultRegistryOwner failures after overriding LocalContext for language; all 132 established cases passed. The real host registry is now explicitly retained before localization. The artifact's raw JUnit XML provides exact totals. This is test-host wiring, not a Reader navigation change; the 16 cases must run again.

Not 100/100, not physical DEVICE-GREEN. Remaining optical questions include actual publication-cover contrast, room/environment separation on OLED and LCD, whole-screen spatial harmony, large-text scroll rhythm, and finger-driven reading material/frame pacing. Source proofs and future capture gates do not substitute for those judgments.

### Compact composition correction after rendered review

The 320dp Threshold capture exposed a secondary-heading defect: an unconstrained recent-books title consumed the action's width, making View all wrap awkwardly. The heading now receives the remaining weighted measure, while the real 48dp action keeps its natural width. Both title and action may grow vertically; no truncation or font-size reduction is introduced.

The Gallery capture also exposed duplicated spacing: lazy-grid row separation plus an extra top inset on both Search and Reading State. Removing the two redundant insets brings book objects forward while preserving the grid's shared rhythm, actual text-field size, filter target, and all direct retrieval controls. This is composition refinement, not hidden functionality or reduced touch geometry.

Principle: an atmospheric archive introduces the world briefly, then gives its objects and retrieval instruments clear ownership. The latest source needs its final full verification and fresh render inspection; previous-head evidence must not be relabeled as current proof.

### Android search assertion correction

Storage run 37288831351 at 751c042 executed 148 cases: 16 failures, zero errors/skips. All 132 established cases passed. The actual host registry correction worked: the new cases reached live retrieval successfully. Their remaining failure was an ambiguous test selector matching both the editable query Still and the matching publication record Still. The assertion now excludes editable controls explicitly, retaining exact result-presence and original-art-result-removal checks across all 16 configurations. This is not an app retrieval failure and no production search code is changed. Original artwork remains enabled. The fresh Android run is required before declaring this correction verified.

### Final local proof for the authored-world source candidate

Source head: `3686fb77b5adb760b43bd51115ec68e50efc9c63`; canonical base remains `18fe4ab78b381ba44fbbfc1817a00d3f8253bfcf`. `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` completed successfully in 11m56s. After the Android-only selector correction, the same four tasks completed again in 24s, retaining the successful JVM outputs and rebuilding the actual instrumentation APK. No executable-source changes occurred after that confirmation.

Exact retained results: **729 JVM cases / 130 suites; zero failures, errors or skipped cases**. This includes **36 design checks** and the explicitly defined **291 Reader regression cases / 42 suites**. Reading-policy checks: **39 passed**. Lint: **zero errors, 175 warnings, one hint**; warnings are not suppressed. The additional unused-resource warning is the retained Castle inner-keep label after removing its redundant visible eyebrow; it does not indicate a rendering or navigation failure. Build outputs include the debug APK and debug AndroidTest APK with SHA-256/size in `authored-world-verification.json`. `git diff --check` passed.

The native production-component matrix regenerated **540 images** across 15 configurations and 36 real review surfaces, plus **eight focused detail captures**. Capture freshness is checked against the actual render-suite timestamp. Selected inspected frames are linked below. High Contrast keeps full chamber contrast; ordinary Castle exposes original architecture. Gallery's first object arrives 24dp earlier after duplicate insets are removed. Normal 320dp Threshold retains a physical cover beside its complete four-line title; the 48dp View all action remains single-line while the secondary heading wraps naturally.

- [Threshold, 320dp](authored-world-threshold-active-en-100-320.png)
- [Gallery, 412dp](authored-world-library-gallery-en-100-412.png)
- [Gallery, Persian 200%](authored-world-library-gallery-fa-200-360.png)
- [Castle halls, 412dp](authored-world-castle-advanced-en-100-412.png)
- [Castle opposing wings, tablet](authored-world-castle-advanced-en-100-1280-tablet.png)
- [Castle, Persian 200% High Contrast](authored-world-castle-advanced-fa-200-360-contrast.png)

Exact failed-head Android evidence is retained separately in `authored-halls-android-failure-evidence.json`: head 751c042, run 37288831351, 148 cases / 16 ambiguous new-selector failures, 80 actual Android captures. This evidence is not relabeled as final-source success. The final-source runs are Android CI 37290421616, Storage 37290421691 and Performance 37290421672; results must be recorded when those jobs complete.

Remaining optical/device questions: original publication covers on both OLED and LCD; subtle parchment grain and age at normal reading distance; room/environment separation without crushed blacks; mixed-script publication-title proportions inside Persian chrome; whole-screen navigation-shell composition and insets; tactile Paper Curl vs Slide, gesture release feel and real frame pacing; actual hinge/rotation behavior. Native preview or emulator acceptance cannot resolve these completely. Reader modes, navigation/progression, persistence, exact locators, TTS, PDF, annotation ownership and source-grounded world state remain preserved. This candidate is neither 100/100 nor physical DEVICE-GREEN.

### Final-source Android instrumentation result

Storage run **37290421691 succeeded** at exact source head **3686fb77b5adb760b43bd51115ec68e50efc9c63**. Its log records `BUILD SUCCESSFUL in 9m27s`. Original artwork, all 16 newly added localized live-search cases, and the existing case matrix remain enabled. The unchanged matrix contains 148 expected cases and 96 expected capture names; these are source expectations, not independently re-counted current JUnit/image totals.

Artifact **11337010835** is **49,773,780 bytes**, digest `sha256:68463694b70bdab7481ca18f36f51e32d3df42ff271163e523ffa777d7991a5c`. The executor rejects transfers above 32 MiB, and the authorized direct file URL returns HTTP 403. Therefore the final-source Android archive is not claimed downloaded, extracted, or optically inspected. Its complete downloadable artifact remains available through the successful GitHub run. `authored-world-android-run.json` records this evidence boundary. The 540 fresh local native images and eight focused detail images are independently checked and selected frames are retained. This limitation is evidence-transfer infrastructure, not an app or test regression.

### Completed GitHub gates for the exact executable source

All three runs succeeded at executable source head **3686fb77b5adb760b43bd51115ec68e50efc9c63**:

- [Android CI 37290421616](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37290421616): unit tests, Room schema verification, lint, debug build, optimized release artifacts/archive verification and performance-harness compilation.
- [Storage Instrumentation 37290421691](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37290421691): actual Android emulator instrumentation, including original-art live-search cases.
- [Performance 37290421672](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37290421672): profile generation, benchmark smoke, profiled release packaging and configured emulator checks. This is emulator evidence, not physical-phone performance acceptance.

The Android CI artifact **11337072716** was downloaded and independently parsed: **729 cases / 130 suites; zero failures, errors or skips; lint 175 warnings / one hint / zero errors**. Its complete suite attributes and archive digest are retained in `authored-world-ci-verification.json`; they agree with the local run. Storage's larger archive transfer limitation remains explicitly recorded; neither its image count nor its current raw JUnit total is falsely claimed independently re-counted.

The following publication contains documentation/verification images only. Executable sources and workflows are unchanged from this tested head; the retained SHA-256 source manifest makes that equivalence reviewable. No new engine, branch, merge to main, force push, production fake history or regression ownership change is introduced. PR #371 remains Draft. Optical and tactile acceptance still requires physical-device evidence.

Performance scope: API35/lavapipe absolute smoke budgets passed and four metrics were normalized. The relative delta gate was explicitly skipped because no persisted main baseline exists; this is not claimed as a measured main-branch improvement. Profiled release verification reported APK 24,405,233 bytes, AAB 21,418,324 bytes, and packaged baseline profile 13,806 bytes. Physical-phone frame pacing remains unverified.

The tested PR merge `94bcbdb64ec56c0d7bef27b3236be1d6047bad84` and executable source head have the exact same Git tree `2cc85102b5300290d3ef0f57a5f82f1cdca0d0d0`, verified by fetching the real merge ref.
