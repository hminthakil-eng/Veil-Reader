# Google Skills Pilot Validation 01 — Reader process recreation

Date: 2026-09-22
Status: completed
Pilot upstream: google/skills
Pinned upstream commit: d6b9f75668ed1450a87ea12a8de57e02f33cc7b4

## Bounded question

Does Veil Reader's current Reader restore verification prove real Android system-initiated process death and distinguish it from user-initiated force-stop/Recents dismissal?

## Skill routing result

The pinned Google Skill Finder catalog routed this Android lifecycle/state-restoration question to:

- `retrieving-developer-knowledge`

The Developer Knowledge MCP tools were not available in the current runtime. Per Eyad guardrails, no API key, gcloud token, or credential fallback was used. Official Android developer documentation was therefore consulted read-only.

## Repository evidence

### Route restoration design

`VeilAppViewModel` stores only small navigation primitives in `SavedStateHandle`:
- selected tab
- settings/archive/chamber state
- active book id
- locator override

This is architecturally aligned with Android guidance: saved state should contain only small data needed to reconstruct the UI; durable/large state belongs in persistent storage.

### Durable reader position

Reader locator/progress is persisted through `LocalLibraryRepository` into Room-backed storage, with a coalesced write and explicit `flushProgress()` from Reader lifecycle pause handling.

This separation is correct:
- SavedStateHandle: transient route/reconstruction state
- Room: durable reading position

## Validation gap found

The existing unit test `VeilAppViewModelTest.activeReaderRoute_survivesViewModelRecreation_thenClosesToLibrary` creates:

`val handle = SavedStateHandle()`

and then reuses the same handle object to construct another `VeilAppViewModel`.

That proves ViewModel recreation against the same in-memory handle. It does **not** prove Android SavedStateRegistry serialization/restoration across real system process death.

Android's SavedStateHandle API documentation explicitly notes that directly constructing `SavedStateHandle()` in tests is not bound to a `SavedStateRegistryOwner` and will not validate process-death restoration.

## QA semantic correction

Do not treat these as the same test:

### A. System-initiated process death — SavedState expected to restore

Deterministic device procedure:

1. Open an EPUB/PDF in Veil Reader.
2. Navigate to a distinctive position.
3. Wait for locator/progress update.
4. Press Home so the Activity reaches stopped state and saved-state providers get a save opportunity.
5. Run:
   `adb shell am kill com.veilreader.app`
6. Relaunch Veil Reader from the launcher.

Expected:
- active reader route is reconstructed from SavedStateHandle;
- the publication is reopened;
- durable position is reconstructed from Room/locator storage;
- no stale navigator/publication object is reused.

### B. User force-stop / Recents dismissal — SavedState is not expected to restore

Use either:
- Android Settings > Apps > Veil Reader > Force stop, or
- remove the app from Recents.

Expected:
- do **not** require SavedStateHandle route restoration;
- on normal relaunch, app-level transient route may start clean;
- after manually reopening the same book, durable Room-backed locator/progress must still resume correctly.

This is a separate durability test, not the Android system-process-death test.

## Acceptance criteria for R1.10

R1.10 is not GREEN until both behaviors are demonstrated separately on a real device/emulator:

1. system process death preserves reader reconstruction through SavedState + durable locator;
2. user dismissal/force-stop does not rely on SavedState, but durable book progress still survives and resumes after reopening.

## Pilot evaluation

Correctness: HIGH — produced a concrete Android semantics correction and identified a false-positive test boundary.

Time saved: MODERATE — Skill Finder narrowed the official source class immediately; no measurable runtime gain for Developer Knowledge retrieval because its MCP was unavailable.

Duplication: LOW — no existing Veil/Eyad skill encoded this exact Android SavedState test distinction.

Security footprint: LOW — pinned public upstream + read-only repository/docs access; no credentials used.

Reproducibility: HIGH — pinned upstream SHA, concrete repo files, deterministic ADB procedure.

Rollback: TRIVIAL — documentation/issue only; no production code or runtime changes.

## Decision

Google Skill Finder + Developer Knowledge workflow: **PASS FOR CONTINUED PILOT**.

Do not promote to production-wide auto-use yet. Continue with explicit invocation and the existing provenance/security gates until more bounded tasks pass.
