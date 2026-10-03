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
