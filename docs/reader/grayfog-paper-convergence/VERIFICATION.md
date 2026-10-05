# Exact-source verification — 2026-10-05

**SOURCE-GREEN / DEVICE-UNVERIFIED** means the final local source/build gates passed. It does not mean Definition of Done or hardware acceptance is complete. Intermittent native-test reliability remains YELLOW.

Verified code commit: `969dbe975665b99af4957bc4743364855c1c6a82`. Subsequent evidence commits only add documentation. Final branch reuses `integration/page-engine-canonical-v1` and Draft PR #374.

## Final gates

| Gate | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest` | PASS, 806 tests / 142 suites; 0 failures, errors, skips; 4m41s | `unit-gate.txt`, `unit-results.json`, XML archive |
| `:app:lintDebug` | PASS; 0 errors, 181 warnings, 2 hints | `lint-summary.json`, XML archive |
| `:app:assembleDebug` | PASS | `packaging-gate.txt`, `artifacts.json` |
| `:app:assembleDebugAndroidTest` | PASS (compiled/packaged, **not executed on hardware**) | same |
| Canonical Paper verification | PASS | `tools/verify-canonical-paper.sh` |
| Canonical guard mutation tests | PASS, 4 | `tools/test_canonical_paper_guard.py` |
| Reading policy contracts | PASS, 39 | `tools/test-policy.sh` |
| Performance budget/delta/dashboard and release-artifact parsers | PASS, 7+5+7+3 | corresponding `tools/test_*.py` |
| Diff whitespace | PASS against working baseline and both fetched branch inputs | `git diff --check`, branch diff checks |
| APK legacy name inspection | `PaperCurlDraw` and `PaperCurlGeometry` absent in every DEX; GPU engine present | `artifacts.json` |

Unit execution was separate from the packaging/lint invocation. Packaging/lint completed in 3m53s. Entire final unit suite includes the Reader navigation, mode, lifecycle, locator, material, shader and RTL contracts; see the per-suite inventory rather than treating static shader assertions as GPU execution.

Toolchain: Temurin JDK 21.0.12.1+1, Gradle 9.6.0, Android platform 37.0 and build-tools 36.0.0. Workspace network access required Java proxy/system truststore settings and test-JVM proxy settings in an external init script. Toolchain/local.properties/init scripts are not shipped in the repository or APK.

## Failures retained

`earlier-native-worker-failure.txt` records the failed combined gate: English native Grayfog search worker aborted (exit 134/SIGABRT), `JniConstants` could not find `java/nio/FloatBuffer`, and the suite reported a skipped case due to the lost worker. This is **not a passing run**, nor is it the GitHub pre-checkout infrastructure failure. Two isolated reproductions passed, including JNI diagnostics in `isolated-native-jni-diagnostics.txt`; the latter exposes native-runtime unchecked-exception warnings. The final full run passed all 806 cases. The abort's complete root cause is still unproven, so repeatability remains a known risk. No test was disabled, omitted from the final full suite or relaxed to hide this failure.

Lint warnings are unsuppressed and archived. The actual unclosed-trace and missing View constructor warnings were corrected. Remaining warnings include plural candidates, unused resources, KTX suggestions, synchronous SharedPreferences durability writes, dependency updates and structural-window configuration usage; a zero-error gate does not certify absence of debt.

## CI and device boundary

Input canonical CI failures had no executed steps (job IDs in `AUDIT.md`): **INFRA FAILURE**. They cannot establish source failure or success. Grayfog input checks were successful at its exact SHA. New-head CI must be checked separately after publication; the local results above are the fallback verification, not substituted device evidence.

`adb devices -l` returned an empty list. No physical runner/endpoint was available. No hardware test, GL driver execution, frame-pacing measurement, repeated-turn residency measurement, process-death optical review or TalkBack session was performed. Acquired-buffer ordering needs driver validation, including `EGL_ANDROID_presentation_time` support. No claim is made about real 60/90/120Hz behavior, destination flash absence, first-turn visual correctness or five-material perceptual quality.

## Subsystem status

| Subsystem | Status | Scope |
|---|---|---|
| Latest Grayfog + canonical ancestry/convergence | GREEN | both input histories retained, manual conflict intent documented |
| Mode isolation / PDF exclusion / legacy removal | GREEN | source contracts + final DEX name inspection |
| Local unit/static/build gates | GREEN | final exact code revision |
| Native test repeatability | YELLOW | earlier SIGABRT remains unresolved |
| Paper physical rendering / five materials | YELLOW | source geometry and profiles; hardware optics unverified |
| Durability/navigation/lifecycle/restoration | YELLOW | contracts pass; live process/lifecycle matrix unverified |
| Reader UX / selection / TTS / notes / accessibility / Persian RTL | YELLOW | Grayfog preserved and source tests pass; hardware interaction matrix unverified |
| Performance / GPU memory stability | YELLOW | instrumentation and parser contracts; no physical measurements |
| Physical-device acceptance | BLOCKED | no connected hardware or endpoint |

The complete physical matrix remains the one requested in the task and PR. Draft/release rollout gates remain in place. There is no evidence to certify that all P0 risks are eliminated or that the Reader is DEVICE-GREEN.
