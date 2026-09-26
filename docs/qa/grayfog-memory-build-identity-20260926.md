# Grayfog memory scoring and APK identity

Base: `d25a7445360c7e9ebba2ce5b702cd3ee2287780e`.

## Problem and changes

Unparenthesized Kotlin `if` expressions swallowed later additive bonuses in Echo
resonance and Memory Atlas engagement. A completed/favorite volume could lose its
passage/session weight and be excluded by the Atlas node cap. Parenthesize each
conditional bonus. Add all 16 Echo flag combinations, four completed/favorite
combinations with passages/sessions, and a node-selection regression.

Correct the existing metadata relation assertion to 9: author 3 + series 4 + one
collection 2. Do not change production weights to satisfy an incorrect assertion.

Both Codemagic workflows now prepare `assets/veil-build.json` before assembling
the debug APK and verify the packaged identity afterward. Artifacts are copied,
without modifying the signed ZIP, to `build-evidence/Veil-Reader-<sha12>-debug.apk`.
The companion JSON records the full revision, branch, workflow/build ID, preparation
time, size, and SHA-256. It records identity verification, not device-test success.
Tracked modifications and untracked source files are rejected. Generated identity
and evidence are ignored by Git. Local builds outside these workflows do not
automatically acquire an identity.

## Validation actually executed

- Kotlin compiler 2.3.21, JUnit 4.13.2, production Models/ReadingSessionTracker/
  HighlightMemory/MemoryAtlas and their two test classes compiled directly on JVM.
- Before fixes: 12 tests, 4 failures (three added regressions and the existing
  incorrect metadata expectation).
- After fixes: 12 tests, 0 failures. This is focused JVM validation, not the full
  Android Gradle suite.
- Six Python tests cover packaged identity/hash, stale same-commit artifact,
  missing asset, dirty tracked source, changed HEAD, and untracked app source.
  APKs in these tests are small ZIP fixtures, not Android builds.
- Both Codemagic workflows parsed as YAML; all script bodies passed `bash -n`.

## Remaining build gate

Run `WORKMODE-BUILD` (display name `WORK MODE — BUILD APK`) on the fix branch.
Policy, full unit, lint, and assemble must pass. Download the named APK together
with `build-identity.json`. Verify installation, Observatory/Archive access, and
Reader behavior on a device. No APK, Android build, emulator result, screenshot,
or full-suite success is claimed by this change.
