# Grand Forge durability execution — 2026-10-09

Repository truth: clean checkout of `grand-forge/p0-kindle-reader-quality-20261008`,
starting at `a67586cfa54122b60f04c1acc47265a68c07b2ff`. No AGENTS.md found.
The previous hardening branch is now `b8ec35a1`; the requested branch contains its
ancestry. Existing execution/matrix/archaeology documents are reused.

Open PR inventory inspected (latest 30): #429 tracks this branch; #430–435 are
bounded reviews of its earlier work. #436/437 contain newer visual work;
#439/440 contain Paper capture hardening; #442 contains newer input/GPU acquisition
repairs; #444 is explicitly verification-only. None was merged or copied blindly.
#441 holds additional clean-room competitor evidence. TTS #426 is the existing
shared-assets hardening ancestor; #420–424 are earlier stacked TTS work. PDF
`alpha/w41-pdf-reliability-v1` contains native link/navigation ownership work.
The GitHub PR-run lookup returned no runs for the exact starting SHA; historical
PASS records are not current-head evidence. All seven risky release gates remain OFF.

## GF-P0-DUR-02 — promote duplicate final snapshots to durable commits

- Priority/subsystem: P0, local progress durability.
- User impact: a final snapshot after scroll/opening could skip crash recovery.
- Observed: ReaderViewModel deduplicates by location before calling saveReaderProgress;
  an identical FINAL_SNAPSHOT after a coalesced checkpoint is rejected.
- Expected: the same location with stronger persistence requirements reaches the
  existing synchronous atomic checkpoint before acknowledgment, once per position.
- Evidence/root cause: ReaderLocatorDeduplicator tracked only the location key;
  ReaderScreen final-snapshot sites at the inspected baseline are lines 1583/1956.
  Three new regressions fail with baseline behavior (an unused signature parameter
  was added only to the temporary baseline copy to compile the new assertions).
- Affected files/implementation:
  - ReaderLocatorPolicy.kt: acceptCommit tracks durability strength alongside current
    and rollback keys; promotes coalesced duplicates; rejection/reset restore strength.
  - ReaderViewModel.kt: pass event.bypassProgressDebounce as requireDurability.
  - ReaderLocatorPolicyTest.kt: five regressions cover promotion, retry, movement,
    reset, rollback and suppression of already-durable duplicates.
  - debug/ReaderDurabilityProbeActivity.kt: coalesced predecessor followed immediately
    by same-position FINAL_SNAPSHOT, then self-SIGKILL; no pause/flush/wait rescue.
  - tools/run_android_ci_emulator.sh: add scroll-final/opening-final cases, bringing
    default fault injection to eight scenarios × 18 cycles = 144 samples.
  - reader-durability-fault-injection.yml: make the existing lane reachable for PRs
    targeting this branch or its hardening parent; update scenario description.
- Reuse opportunity: existing AtomicFile journal, writer lease and ordered Room queue.
  No new store, schema, dependency, renderer or proprietary implementation.
- Risks: one additional synchronous checkpoint for a coalesced position; physical
  latency must be measured. Deduplication records provisional strength; rejected
  repository saves invoke rejectCommit and remain retryable.
- Tests: 19 focused Kotlin/JUnit tests PASS using Kotlin 2.3.21/JUnit 4.13.2;
  baseline behavior fails three promotion regressions; 39 reading-policy checks,
  canonical Paper contract, 26 Python tooling tests, shell syntax and diff checks PASS.
  Standalone JVM tests do not compile ReaderViewModel or Android probe integration.
- Performance impact: unchanged ordinary duplicate suppression; a stronger final
  snapshot now incurs the existing journal cost. No device percentile measurements.
- Accessibility/RTL: unchanged semantic location/direction and non-gesture controls;
  device verification still required.
- Acceptance: promoted final snapshot returns a commit only after the existing journal
  succeeds; same-position repeated durable snapshots stay suppressed; rejection
  allows retry; new process restores destination in both added fault scenarios.
- Dependencies: Android SDK/build, exact-SHA emulator lane, physical kill/relaunch and
  checkpoint latency evidence. Full-app release acceptance remains outstanding.
- Status: YELLOW implementation and focused policy tests; Android/device gates pending.
- Rollback: revert this bounded durability change; no migrations or gate promotion.

## File-level continuation graph

GF-P0-DUR-02 → existing ReaderCloseDurability/ReaderNavigationTransactionGate
characterization → ReadingAnchor versus ExplorationLocator separation →
ReaderInputArbiter/PaperCurlInputListener ownership → evaluate #442/#444 exact-SHA
GPU acquisition evidence → MaterialPageEngine snapshot/texture performance →
ReaderAppearancePolicy adaptive typesetting + validated RTL corpus → existing
ReaderTtsSession/checkpoint/service acceptance. Do not overwrite newer draft work.

Navigation correction on 2026-10-10: the inspected baseline already classifies
search/TOC/footnotes/bookmarks and related jumps as exploration, guards lifecycle
checkpoints and maintains ReaderNavigationSessionState. The previous statement
that programmatic destinations always commit was stale. Device/UX acceptance and
continuous-scroll post-settlement behavior still require characterization.

Remaining RED: Paper release
availability and physical quality are unproven. BLOCKED pending device evidence:
TalkBack, actual mixed-language speech, 60/90/120 Hz, low-memory, battery/thermal.
No before/after latency or memory figures are available. No regressions observed in
executed focused checks; application-wide regression status is not established.

## Build/environment evidence

Gradle 9.6.0 downloaded and configured the project after applying the managed
HTTP proxy to JVM networking. The Android test task stopped at missing SDK.
Downloaded official Android command-line tools and attempted installation of
platform-tools, platforms;android-37 and build-tools;37.0.0 through the proxy.
SDK manager exited 1: `Failed to find package platforms;android-37`. The repository
requires compileSdk 37. No SDK downgrade or simulated platform was used. This
blocks Android compilation/instrumentation here; no adb/device was present at
start. Standalone JVM policy evidence remains valid within its stated scope.

No feature gate changed. No remote branch update, PR merge or release performed.

## 2026-10-10 resumed validation

SDK blocker corrected: official package is `platforms;android-37.0`. Installed it
and official build tools without changing compileSdk/targetSdk. Installed a full
Temurin JDK 21 because the preinstalled runtime lacked JAVA_COMPILER. Configured
the managed system Java CA truststore for that JDK; TLS verification remains on.
Android build/test validation resumed with unchanged repository dependencies.

## GF-GATE-02 — make verification reachable on the dedicated Forge branch

Priority P0 / QA. Android CI, Storage Instrumentation and Performance Benchmarks
exclude the dedicated branch from pull_request target filters. Add only
grand-forge/p0-kindle-reader-quality-20261008 to their existing filters; retain
all prior targets, permissions, job steps and release rules. The durability lane
is already updated in this patch. These are existing harnesses, not replacements.
Risk: additional CI resource use for real Forge PRs. Acceptance: a draft PR against
the dedicated branch schedules all four lanes; terminal results remain required.
Status YELLOW until hosted runs complete. No device/GPU promotion implied.
Rollback: revert the branch-filter additions. Accessibility/RTL/runtime performance
unchanged; hosted performance execution is now reachable for this review target.

Paper integration evidence rechecked at f7100fe9: Android CI 37982353401 and
Storage 37982353271 succeeded, Forge QA APK 37982353230 succeeded, Performance
37982353412 failed at benchmark instrumentation. This is evidence for that separate
draft branch, not for our source. Do not merge/promote Paper from compile evidence.

## Android-focused evidence — 2026-10-10

Gradle :app:testDebugUnitTest filtered to ReaderLocatorPolicyTest,
ReaderNavigationSessionStateTest and ReaderNavigationTransactionTest, plus
:app:compileDebugAndroidTestKotlin: BUILD SUCCESSFUL. XML: 20 + 6 + 33 = 59 tests,
zero failures/errors. Main app, real ViewModel and debug probe compile. Full unit
suite, debug APK and lint are the remaining local checks. The full suite includes
existing input ownership, RTL direction and listener lifecycle characterizations.
Six new policy regressions now include unchanged-location reward suppression.

ADB reports no attached device and /dev/kvm is absent. New abrupt-process scenarios
are compiled but have not executed locally. No physical/device/performance GREEN
claim is made. Existing 39 Java policy and 26 Python tests plus Paper source guard
pass; workflow branch filters and shell syntax were checked.

## Broad regression evidence and final-source boundary

Full debug unit suite on ecfe774f: 1,015 tests in 174 suites; zero failures,
errors or skips. Unit XML copied to /workspace/scratch/forge-full-unit-evidence.
The subsequent a28e41f1 change restores the original non-page-event pace-reset
condition, so a duplicate navigator emission does not reset pacing merely because
it is ineligible for another reward. Final Reader/input tests and APK/lint will
be revalidated on a28e41f1; the earlier whole-suite result is not mislabeled as
an exact-final-source execution. Full hosted checks remain required on the PR head.

## GF-P0-NAV-03 — reject repeated continuous-scroll exploration observations

- Priority/subsystem: P0, navigation/progress ownership.
- User impact: a temporary search/TOC destination can replace the reading anchor
  without actual movement when the scroll locator stream repeats that destination.
- Observed/evidence: after onProgrammaticSettlement preserves the anchor, the next
  scroll observation is classified NAVIGATOR_SCROLL_COMMIT. The old session policy
  admits that event unconditionally. The new regression fails on the baseline
  policy (an unused second parameter was added only in a temporary baseline copy).
- Expected: a repeated identical exploration locator remains uncommitted; a changed
  scroll reading location may promote the anchor only after save acceptance.
- Root cause: exploration event admission considered reason but not observed position.
- Files/implementation: ReaderNavigationSessionState.kt requires locatorJson for
  observed-event admission and rejects unchanged scroll destinations; ReaderScreen.kt
  passes its existing persisted locator at both recording sites. The state test
  covers repeated destination, changed position and failed/successful persistence.
- Reuse: existing semantic navigation policies, session state and durable write path;
  no renderer replacement, schema, new store or UI redesign.
- Risks: this guard compares the current encoded locator, not metadata-independent
  semantic equivalence. Equivalent locators with differing incidental metadata and
  delayed relayout observations still need separate characterization/device evidence.
- Tests: baseline state suite has one failure in seven tests; final Android Reader/
  input tests and app/instrumentation compilation pending at this record boundary.
- Performance: one string comparison per exploration scroll event; no measured latency.
- Accessibility/RTL: same navigation controls and semantic direction; repeated native
  scroll events cannot replace an anchor merely because TalkBack/RTL is active.
  Actual TalkBack/RTL output remains unverified.
- Acceptance: same encoded destination preserves readingAnchor and exploration;
  changed position is admitted; rejected write keeps exploration; accepted write
  promotes the anchor. Include real continuous-scroll search/TOC/jump replay on device.
- Dependencies/status: final-source Android checks and device stream evidence; YELLOW.
- Rollback: revert the bounded policy/call-site/test commit. Release gates unchanged.

Lint note: earlier whole-suite/APK invocation hit a Kotlin FIR/ExperimentalDetector
analysis crash in ReaderNavigationSessionState.kt after a signature was edited
while that invocation was active. No detector was disabled. Re-run final source
with compilation, targeted Reader/input tests, instrumentation compilation, APK
and lint; record the new terminal result separately.

## Final-source local evidence

Executable source 9a5abec3: final Reader/input suite has 145 tests in 18 suites,
zero failures/errors/skips. Main app and instrumentation compile; debug APK builds.
The repeated-scroll state regression passes (7/7 state tests); its baseline behavior
fails the new case. Lint remains pending until its current invocation terminates.
No feature-gate file changed. No physical device is attached. No latency/memory/
battery figures are fabricated; this is YELLOW, not a device-proven release.

Debug APK SHA-256: b1cdead34d8137e487548a7ab936323f4cd2edf47ed013a008f1e90a7bb97f42.
Path: app/build/outputs/apk/debug/app-debug.apk. This APK is the standard debug
variant; it is not the pinned Forge QA artifact or a release artifact.

### Exact changed paths

- `.github/workflows/android.yml`
- `.github/workflows/performance-benchmark.yml`
- `.github/workflows/reader-durability-fault-injection.yml`
- `.github/workflows/storage-instrumentation.yml`
- `app/src/debug/java/com/veilreader/app/debug/ReaderDurabilityProbeActivity.kt`
- `app/src/main/java/com/veilreader/app/ui/reader/ReaderLocatorPolicy.kt`
- `app/src/main/java/com/veilreader/app/ui/reader/ReaderNavigationSessionState.kt`
- `app/src/main/java/com/veilreader/app/ui/reader/ReaderViewModel.kt`
- `app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt`
- `app/src/test/java/com/veilreader/app/ui/reader/ReaderLocatorPolicyTest.kt`
- `app/src/test/java/com/veilreader/app/ui/reader/ReaderNavigationSessionStateTest.kt`
- `docs/quality/GRAND_FORGE_DURABILITY_2026-10-09.md`
- `docs/quality/GRAND_FORGE_MASTER_FIX_PLAN_2026-10-08.md`
- `docs/quality/GRAND_FORGE_MASTER_MATRIX_2026-10-08.md`
- `tools/run_android_ci_emulator.sh`

## Terminal final-source verification

Final source 9a5abec3: BUILD SUCCESSFUL, 3m45s, for affected Reader/input unit
tests, instrumentation compilation, debug APK and lint. XML: 145 tests / 18
suites, zero failures/errors/skips. Lint succeeds with all detectors enabled;
the earlier FIR crash did not recur after final-source compilation.
The earlier whole suite (1,015 tests) remains labeled with its preceding source.
Hosted CI must run the whole final-source suite before promotion.

Release/device decision: YELLOW. All risky gates remain OFF. Physical-device
RTL/TalkBack/kill-relaunch/checkpoint latency and Paper acceptance are still
unverified; no release, merge or device installation is claimed. No regression
was observed in the executed affected checks. The new kill scenarios are compiled
but local device execution is unavailable. Review targets the dedicated Forge
branch via a bounded draft branch rather than rewriting its shared head.
