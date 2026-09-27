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
| #291 | Ritual | W0c reconciles phases, frozen target, repository result and reduced motion into the canonical chamber. Castle routes to Ritual. Full Android/device verification remains pending. |
| #292 | Castle mutation | W0d preserves the canonical spatial Keep/Ritual ownership and integrates factual history mutation, reread patina, return awakening and silence cooling. Android/device verification remains pending. |
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

## W0c — advancement ceremony reconciliation

Inputs: Alpha `10e6c8862748e1f23cf53e560f5db919397e50ae` and PR #291
`ec24786d8a5193a76201f1ae03ebd0e3c9f89c5f`. Canonical remained at
`7c98d6f4435762dbd2e904252109f54985d1cf4f` during this review.

Keep the current full-screen chamber, safe insets, typography, Path art and
reduced-motion entry. Selectively reuse the invocation/sealing/reveal contract
instead of installing the competing dialog from #291.

### Defects and fixes

- Castle's advancement button previously mutated rank directly. It now opens
  Path/Ritual, which owns the explicit confirmation surface.
- Freeze path ID and source rank at opening with saveable primitives. Final-rank
  advancement no longer removes the dialog because the live next rank becomes null.
- Pass that expected path/rank to GameRepository. The existing pure progression
  policy now rejects stale/replayed requests even if another rank is eligible.
- Change the Path callback to return the repository's Boolean result. Rejection
  has its own dismissible state; it cannot trigger a success reveal or sensory cue.
- Close the confirmation guard synchronously before dispatch. No effect calls
  advanceRank, including effects restarted by rotation or process recreation.
- After the 620 ms sealing interval (70 ms with reduced motion), check the current
  profile before revealing. A restored reveal is also checked against that profile.
- Leave success visible until Continue/back; no timed auto-dismiss. Use a polite
  live region for stage labels and scrolling for compact/large-text layouts.

The progression requirements, XP, rank names and persistence owner stay unchanged.
The repository still uses its existing SharedPreferences.apply write path: acceptance
and observing the new profile are not a claim of synchronously durable disk storage.
No new persistence architecture or process-death guarantee is introduced here.

### Verification

The actual Models, GamificationEngine, ceremony policy and regression test source
compiled with Kotlin 2.3.21. **8 JUnit tests passed**, covering replay after rank
change, changed path, incomplete ritual, last rank, submission/dismissal stages,
missing profile confirmation, restored-profile disagreement and confirmed targets.

The Kotlin compiler PSI parser accepted the modified PathScreen, VeilApp and
GameRepository. Syntax parsing does not resolve Compose APIs or replace Android
compilation. Full Android build, TalkBack, small-screen rendering, rapid taps,
rotation during sealing and process-recreation behavior remain device gates.

PR #291 remains open for provenance, not wholesale merge. Remaining source
reconciliations are #293–#295 and #297–#298. #292's direct-advancement ownership
was addressed here and its Castle mutation layer is reconciled in W0d below.


## W0d — Castle mutation reconciliation

Inputs: Alpha `a007703756c30f5f77291e7becdce85b228d0ce3` and PR #292
`19e44b26f0f58b7f5d2a21efd2769e299c45b7a1`.

### Decision

Preserve the canonical spatial Keep, current phone/adaptive composition, shared
foreground atmosphere and W0c Ritual ownership. Selectively port #292's factual
environmental mutation layer instead of replacing CastleScreen wholesale.

### Implementation and hardening

- Feed the Castle the existing durable ReadingCycleRecord list; rank still remains
  the only chamber-unlock authority and Castle never mutates progression.
- Finished volumes form sealed alcoves, annotations light scriptorium lamps,
  durable reading time builds foundation courses, rereads add memory rings/patina,
  and archive age contributes restrained material weathering.
- Long inactivity cools light/fog without deleting history or reducing unlocks.
- A genuine return after a 21-day gap creates a temporary foundation-up awakening.
- Fix a source defect where a second post-return session erased the awakening:
  the policy now remembers the latest qualifying return event for the recent window.
- Ignore future/corrupt timestamps when deriving latest activity so clock skew cannot
  hide valid historical evidence.
- Reuse the shared temporal sample to refresh Castle aging at world-phase changes;
  no second timer, worker, persistence layer or Reader dependency is introduced.
- The advancement CTA now routes through the existing Ritual surface via room
  navigation; there is no parallel advancement callback.

### Verification and remaining gates

The four deterministic Castle mutation tests from #292 are carried forward, plus
two Alpha regressions for multi-session return persistence and future-timestamp
filtering. Source-level integration anchors were checked against the exact W0c head.
No Android build, Compose resolution, device rendering, TalkBack, rotation or
performance run is claimed by this step. Those remain W0 acceptance gates.

PR #292 remains open as source provenance and should not be merged wholesale.
Remaining source reconciliations: #293–#295 and #297–#298.
