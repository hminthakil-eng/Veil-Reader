# Cloud verification — Grayfog continuation / PR #371

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
