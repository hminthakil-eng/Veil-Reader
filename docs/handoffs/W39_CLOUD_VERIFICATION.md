# W39 cloud verification baseline

Date: 2026-10-03. Canonical branch: `alpha/w39-focus-guide-v1`.
Initial source HEAD: `f1e45300`; incorporated the documentation-only advance `89c4c6c6`.
No Readium/PDF architecture, locator authority, final flush, or session lifecycle was replaced.

## Environment and reproducibility

Linux x86_64, two CPU cores, 8 GiB cgroup memory, no `/dev/kvm`.
Gradle 9.6.0 is now available through the committed official wrapper with SHA-256 verification.
Build JDK: Temurin 21. Android source/bytecode compatibility remains Java 17.
Android SDK platform 37.0; AGP 9.4.0 used Build Tools 36.0.0.
Robolectric 4.17's default SDK 37 requires JDK 21 and a test-only export of
`java.base/jdk.internal.access` for its FileDescriptor bridge. CI and README now agree.

This cloud proxy also requires Java/test processes to use the inherited proxy and system CA.
The proxy/CA overrides were environment-local; no credentials or environment paths were committed.

## Executed checks

- `./gradlew testDebugUnitTest`: 537 tests, 0 failures/errors/skips.
- `./gradlew assembleDebug`: PASS; APK 46,258,159 bytes (44.12 MiB).
- `./gradlew :app:assembleDebugAndroidTest`: PASS; all app instrumentation targets compile/package.
- `./gradlew lintDebug`: PASS; 0 errors, 152 warnings. Nonfatal findings include dependency-update
  advisories, existing modifier-order/plural recommendations, and deliberate durable SharedPreferences
  commits. No global lint baseline or error suppression was added.
- `sh tools/test-policy.sh`: 39 checks passed.
- Performance budget/delta/dashboard parser suites: 15 tests passed.
- `git diff --check`: PASS.

## Instrumentation limitation

An API 35 x86_64 AOSP ATD emulator was installed and attempted twice using TCG/software graphics.
The first attempt had not completed boot after more than 12 minutes. A second lower-memory attempt
reported `sys.boot_completed=1` but did not provide stable package/window services: APK installation
failed with `cmd: Can't find service: package`, and service readiness checks subsequently timed out
or lacked PackageManager. System-server/watchdog and prolonged dex-optimization logs corroborated
that this was an unusable runtime target. Both attempts were stopped to release memory for Gradle.
There is no hardware acceleration or attached physical device. **No instrumentation runtime pass is claimed.**

Compiled tests include `SettingsStoreInstrumentedTest`, `ReaderFocusGuideOverlayTest`,
`ReaderHardwareKeysInstrumentedTest`, PDF reliability/import/brightness/font tests, and migration tests.
The test APK contains authentic Room v1/v2/v3 schema assets. v2 was exported with Room 2.8.5 from
exact database/entity/DAO/relation sources at `cdf58632^`; every v2 entity equals its v3 counterpart.
The previously seen alternate-lineage manga schema numbered v2 was not reused.

## Fixes and regression coverage

- Fixed illegal composable theme reads in PDF Canvas callbacks and missing Settings `sp` import.
- W39 removes its Canvas immediately for OFF and blocking UI; no decorative visibility animation.
  It also defers until session readiness, during preference handoff, and during close.
- Wrong-typed persisted focus values, invalid enums, nonfinite numbers, and invalid OFF resume mode fail calm.
- Immediate focus-toggle draft prevents rapid taps from reusing stale persisted state; delayed older
  acknowledgements cannot overwrite the pending latest choice. This is preferences feedback only.
- Quick-toggle state/codec tests preserve WINDOW/LINE tuning and resume mode after state reconstruction.
  The real DataStore reopen test is compiled, not device-executed.
- Focus control announces localized mode; the Canvas has no semantics or pointer/gesture modifiers.
- W37 tests verify default DEFER behavior, renderer-zone bypass of all Veil delegates, selection and
  accessibility precedence, semantic next/previous callbacks, and existing LTR/RTL motion policies.
- W38 tests cover System default, opt-in handling, independent presses, repeat throttling, declined
  repeat consumption, mid-press mode changes, old/new handler ownership, lost key-up recovery, and
  spoken screen-reader ownership without requiring touch exploration.
- Activity press tombstones retain consumption after Reader disposal/replacement without retaining its
  callback. Only fresh downs can acquire new ownership.
- Activity dispatch overrides Android's public callback. The narrow RestrictedApi suppression documents
  AndroidX's restricted bridge annotation; it does not permit private framework or renderer APIs.

## Rendering/accessibility audit and remaining device QA

PAPER/SEPIA use warm dark dimming and restrained brown edges; DUSK/OLED use black dimming and brass
edges. Only the areas outside the normalized band are filled. OLED peripheral white text still dims
against black; the guide cannot lighten an OLED background. WINDOW retains saved height; LINE caps
its effective band at 12%. The central reading band is never tinted/filled. Shell high contrast does
not replace the publication palette or modify the undimmed reading band. Geometry is vertical and
publication-direction neutral; RTL navigation remains semantic. Focus control retains the existing
48dp target and now supplies stateDescription, so color is not the sole state signal.

`ReaderFocusGuideOverlayTest` checks OFF/WINDOW/LINE across all four themes and high-contrast states,
48 real touch injections reaching the underlying control, and untouched central pixels. It is compiled
and awaits a stable device. Physical EPUB links/footnotes/selection/highlights, Paper/Slide/static
navigation, PDF pinch/pan, large-font/RTL layout, TalkBack focus order, rotation, and process-kill
recreation still need device QA. Existing unit durability/navigation/startup/ownership tests passed;
this report does not substitute those tests for unexecuted renderer/device checks.

PR #364 remains unmerged.
