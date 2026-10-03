# Cloud verification — Grayfog design candidate

2026-10-03. Source inputs are pinned by SHA-256 in [verification.json](verification.json); the design commit contains these inputs and this evidence. Base: `18fe4ab78b381ba44fbbfc1817a00d3f8253bfcf`.

## Executed result

| Check | Result |
|---|---|
| Full JVM suite | **682 tests, 122 suites; zero failures, errors or skips** |
| Design system / constitution / art direction | 29 tests passed within the full suite; includes script tracking, semantic contrast and usable-width/large-text policy |
| Reading policy | 39 checks passed |
| Compose/app compilation | Passed |
| Instrumentation compilation and APK packaging | Passed, including 48 new parameterized shell test cases; not executed on a device |
| Android lint | Passed: zero errors/fatal findings; 166 warnings and one hint retained in the report |
| Debug app APK | Packaged for verification only; eight exact font binaries and five font license/provenance files verified inside APK |
| Font integrity | All eight static weights/names verified; Persian/Arabic characters/digits and GSUB/GPOS shaping tables present; original licensing retained |
| Source ownership comparison | Reader owner/data/domain/navigation/schema and Paper/Slide implementation sources unchanged |
| Whitespace/source diff | `git diff --check` passed |
| Android screenshots / TalkBack / device interactions | **Pending** |
| Performance benchmarks / release | Not executed; no performance or release-ready claim |

## Commands and environment

Full JDK 21, Gradle wrapper 9.6.0, Android SDK Platform 37.0; project SDK/dependency versions unchanged. The fresh cloud machine initially had only a JRE; build tooling was installed outside the repository. No credential values were read or printed.

```sh
ANDROID_HOME=/workspace/android-sdk JAVA_HOME=/workspace/jdk21 \
  JAVA_TOOL_OPTIONS='-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts' \
  ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest \
  --no-daemon --max-workers=2
sh tools/test-policy.sh
git diff --check
```

The final combined Gradle run completed successfully in 9m 18s. See [gradle-verification.log](gradle-verification.log), [lint-results-debug.txt](lint-results-debug.txt), and [verification.json](verification.json). No source edits followed this run. Documentation/evidence additions do not change its verified source inputs.

Lint warnings were not suppressed. The durable SharedPreferences `commit()` warnings are not a reason to replace flush semantics with `apply()` in a visual pass. Dependency-update, pluralization, configuration-size, existing Compose API and unused-resource findings remain visible for separate review; this is not a warnings-clean claim.

The intermediate harness compilation initially used a private index row; the final compiled source makes that existing row module-internal for direct interaction coverage. No second implementation or fake reader was added. Initial bootstrap/intermediate attempts do not count as final verification.

No Android device is attached and `/dev/kvm` is absent. Debug previews are supplied but not rendered/approved here. Compilation, JVM policy checks and font packaging do not establish actual text shaping, page-turn fidelity, contrast over imagery, touch exploration, native PDF behavior or world-class visual quality. **DEVICE-GREEN is not established.**

Use the handoff's screenshot/device checklist before approving the design. No release build or main merge was performed.
