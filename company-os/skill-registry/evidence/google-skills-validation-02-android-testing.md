# Google/Android Skills Pilot Validation 02 — Android testing

Date: 2026-09-22
Status: PASS FOR CONTINUED PILOT
Code verification status: CI/device evidence pending
Upstream: android/skills
Pinned upstream commit: b1f707d90904129b5972b3cc6436b568583effe5
Skill: testing/testing-setup

## Bounded task

Improve Veil Reader R1.10 restore verification without changing Reader runtime architecture.

## Skill findings

The official Android testing skill recommends:

- analyze and respect the existing testing stack;
- use device-based end-to-end tests for release-candidate journeys;
- use UIAutomator when system/platform interaction is required;
- always verify UI state restoration;
- keep end-to-end coverage small and focused rather than duplicating lower-level tests.

## Existing Veil Reader stack

- JUnit4 local tests
- Android instrumentation runner
- Room device/instrumentation tests
- Compose application
- no existing R1.10 process-recreation harness
- current route restoration test directly constructs and reuses SavedStateHandle

No broad testing-stack replacement was justified.

## High-value discovery

Current AndroidX Lifecycle 2.10.0 supports the official
`androidx.lifecycle:lifecycle-viewmodel-testing` artifact.

Its `ViewModelScenario.recreate()` API explicitly simulates system process death by:

- saving state with SavedStateRegistryController.performSave;
- recreating the ViewModelStoreOwner, LifecycleOwner, and SavedStateRegistryOwner;
- recreating the ViewModelProvider.

This is a materially stronger local test than recreating VeilAppViewModel with the same directly-created SavedStateHandle.

## Implementation produced

Draft PR #249 against `integration/reader-rc-v2`:

- adds `lifecycle-viewmodel-testing:2.10.0` as a test-only dependency;
- upgrades the active-reader route test to `ViewModelScenario.recreate()`;
- adds a PowerShell/ADB process-recreation harness;
- separates `system-kill` from `force-stop`;
- asserts a changed process PID;
- captures dumpsys/logcat evidence;
- defines EPUB/PDF × system-kill/force-stop acceptance gates.

No production Reader runtime code changed.

## Security / architecture footprint

Credentials: none.
External execution: adb on an authorized QA device only.
Data reset: none.
Runtime dependency change: none.
Production behavior change: none.
Rollback: delete the QA branch / close the draft PR.

## Current verification state

PR #249 is draft and mergeable.
GitHub PR workflows were queued after creation.
Physical-device evidence remains required before R1.10 can be GREEN.

## Pilot evaluation

Correctness value: HIGH
Measurable engineering benefit: HIGH
Duplication: LOW
Security footprint: LOW
Reversibility: HIGH
Fit with Company OS: HIGH

Decision: keep `android/skills/testing/testing-setup` approved for controlled pilot.
Do not auto-install the broader Android skill set.
