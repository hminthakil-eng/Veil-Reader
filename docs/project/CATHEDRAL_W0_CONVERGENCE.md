# Cathedral W0 — source convergence checkpoint

Date: 2026-09-27. Master tracking: #299.

## Integration baseline

Use `CANONICAL-UIUX-PERFECTION@7c98d6f4435762dbd2e904252109f54985d1cf4f`.
GitHub compare reports this baseline **70 commits ahead, zero behind**
`BUILD-WORKMODE-GRAYFOG-PERFECTION@f7c0be0cf311b6c2067d09122c651435ab2a6779`.

PRs #290–#298 and #300 target that older base. They are separate slices, not a
stack that can be recovered by choosing the highest PR number. For example, #298
is 13 commits ahead and 70 behind the canonical UI/UX baseline.

The canonical baseline includes compile fixes, a single BookArtifact implementation,
phone layout refinements, bundled font work, full-screen chambers, local-time
atmosphere and a durable discovery policy. Preserve that work during reconciliation.

## This review slice

- Reuses #290's build identity tool and its six tests.
- Adds prepare/verify gates to both **existing canonical** Codemagic workflows.
- Retains their IDs, labels, test/lint gates, benchmark lane and complete fixed
  AndroidJUnitRunner verifier.
- Publishes the verified, SHA-labelled app APK through `build-evidence/**`.
- Carries #300's Alpha leadership and delivery map into the canonical candidate.
- Does not mark any world-feature PR as integrated or superseded.

This is W0a, not completion of W0 or a release candidate.

## Remaining convergence decisions

The following are source-review priorities, not claims that the PR implementations
have passed device QA. Compare each slice to the pinned baseline before porting.

| PR | Area | Required reconciliation |
|---|---|---|
| #291 | Ritual | Canonical PathScreen already has a full-screen ceremony. Reconcile phase, target snapshot and reduced-motion behavior under one ceremony owner. |
| #292 | Castle mutation | Preserve canonical spatial Keep and integrate factual mutation inputs without overwriting newer composition. |
| #293 | Archive material | Preserve the canonical BookArtifact model and newer notebook chambers while reconciling material strata and sealed dossiers. |
| #294 | Discoveries | Canonical already contains VeiledDiscoveryPolicy, profile data and GameRepository persistence. Compare schema, eligibility and restore semantics before choosing one owner; do not install a second ledger. |
| #295 | Reading signature | Review the new factual model and reconcile ProfileScreen with the current profile and discovery implementation. |
| #296 | Temporal atmosphere | Source reconciliation completed in W0b below: retain canonical palette/schedule, share a foreground clock, and reject the competing overlay/model. Android lifecycle verification remains pending. |
| #297 | Persian identity | Canonical already bundles shell fonts. Compare font roles, Arabic/Persian composition and provenance before replacing or adding font families. |
| #298 | Literary art | Port bounded realm motifs into current screens; preserve phone composition, Reader zero-ornament and canonical artifacts. |
| #300 | Governance | Documents carried into this review slice. |

## APK observation

The supplied `app-debug-14.apk` has SHA-256
`7866ec08afba284b693fe1ee2a23b6b56e1afe7b7856f0622036887c7ab6055c`.
Its ZIP does not contain `assets/veil-build.json`. The source commit that built it
has therefore **not been established in this session**. The filename alone is not
build evidence. No runtime or screenshot acceptance is claimed.

## Verification performed

- Six existing build-identity tests passed locally using temporary Git repositories
  and ZIP fixtures. These are not Android builds.
- Both workflows parse as YAML.
- All 15 embedded shell scripts pass `bash -n`.
- In both lanes, prepare immediately precedes assembly and verify immediately follows it.
- Canonical workflow names and the runner-verification script are retained unchanged.
- Only verified app APK output is listed; instrumentation and benchmark outputs stay intact.

## Next gates

1. Review remaining feature ownership conflicts above; continue targeting the same
   canonical integration line.
2. When the integrated source is ready for a build, run
   `UIUX-CANONICAL-DEBUG-APK` (label `01 — UIUX CANONICAL — DEBUG APK + TEST + LINT`)
   on the exact candidate branch/SHA.
3. Require unit/lint/assemble success and matching embedded/external build identity.
4. Run device/reference review on that artifact, then the full verification lane.

No Android SDK/device execution, Codemagic run, release promotion or runtime
performance measurement was performed by this W0a source slice.

## W0b — temporal atmosphere reconciliation

Source inputs rechecked on 2026-09-27:

- Canonical: `7c98d6f4435762dbd2e904252109f54985d1cf4f`.
- Alpha W0a: `38d40106f2b4380f9e470a1c03338f53f94bd9f6`.
- PR #296: `0395d0a80de804dfc99355edf071bed35cdae1a6`.

### Decision and defect

Keep the canonical four phases and color model. PR #296's five-phase model has
the same class names but different fields and hour boundaries; importing it
would create duplicate definitions and change the accepted palette schedule.
Its per-screen producer also remains active while composed rather than explicitly
stopping with the Activity lifecycle. Its extra overlay is not ported.

Canonical `currentVeilTemporalPhase()` previously sampled `LocalTime.now()` during
composition without an observable clock source. An otherwise idle page could
retain the previous phase until an unrelated recomposition.

### Implementation

- One `ProvideVeilTemporalPhase` under the existing `VeilTheme` owns live time.
- Existing screen calls consume its observable CompositionLocal; no screen timers.
- Register TIME_TICK, TIME_SET and TIMEZONE_CHANGED only while STARTED.
- Unregister on STOP, destruction or composition disposal; ignore queued callbacks
  after unregistering. Registration is idempotent across lifecycle catch-up events.
- Resample immediately on start, using the current system zone on each sample.
- Keep a stable DAY fallback for standalone previews.
- Enforce Sanctuary's constant DAY material policy at the renderer boundary.
- No new dependencies, worker, alarm, shader, persistence or Reader navigation.

The existing 10 call sites across eight screen files keep their canonical
composition and color roles. The hour-to-phase policy is extracted unchanged
into a platform-independent file so the actual production mapping can be tested.

### Verification and limits

The actual production policy and test source compile with Kotlin 2.3.21 and run
under JUnit 4.13.2: **8 tests passed**. Coverage includes all 24 hours, wrapping,
the exact dusk boundary, a timezone change at the same instant, seasonal UTC
offsets, Sanctuary invariance and propagation to shell realms.

This is a focused JVM policy run, not Android compilation or a lifecycle/device
test. MainActivity's existing VeilTheme wrapper and all canonical call sites were
checked. The Android broadcast adapter still needs verification of idle boundary
refresh, background/resume, timezone change, rotation and registration cleanup
on an actual build. No screenshot/performance acceptance is claimed.

API reference used for the context-registered receiver lifecycle and export flag:
https://developer.android.com/develop/background-work/background-tasks/broadcasts

PR #296 stays open as source provenance; it should not be merged wholesale into
the canonical model. Remaining W0 decisions (#291–#295, #297–#298) are unchanged.
