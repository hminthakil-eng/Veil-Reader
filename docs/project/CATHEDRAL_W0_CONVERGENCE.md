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
| #293 | Archive material | W0e preserves canonical BookArtifact ownership and newer notebook chambers while integrating memory strata, resurfaced relic material and full-screen sealed dossiers. Android/device verification remains pending. |
| #294 | Discoveries | W0f keeps ReaderProfile/GameRepository as the sole discovery owner, adds non-fabricated recorded-at metadata, central presentation and Sanctum projection without a parallel ledger. Android/device verification remains pending. |
| #295 | Reading signature | W0g adds a derived-only factual reading signature, shares the W0b timezone context, filters corrupt future timestamps, and adapts its dossier panel for compact/large-text layouts. Android/device verification remains pending. |
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
Remaining source reconciliations: #297–#298.


## W0e — Archive material reconciliation

Inputs: Alpha `6e382ea4c823fdad956572147d4895c63c00fba3` and PR #293
`e6dca48757ff818f30b0e316005d7b0845a25879`.

### Decision

Keep the canonical Archive notebook structure, current BookArtifact owner, W0b
foreground clock and existing durable echo/capsule models. Port #293's material
language as presentation over those facts rather than creating a second archive.

### Implementation and hardening

- Highlight cards now express echo depth as physical strata/patina, exact passage
  revisits as a restrained spirit trace, and eligible resurfaced echoes as relics.
- Add the existing Memory Atlas link count to the Archive register as THREADS;
  this is derived from current books/highlights/sessions and introduces no storage.
- Preserve the canonical `com.veilreader.app.ui.books.bookArtifactState` owner.
  The source branch predates that package move and cannot be copied wholesale.
- Upgrade sealed reading records into a full-screen Archive dossier with a compact
  cycle seal, durable-vs-legacy provenance, timeline, metrics and explicit local
  preservation copy.
- Fix a W0b regression in the source dossier: its atmosphere now consumes
  `currentVeilTemporalPhase()` instead of silently falling back to constant DAY.
- Clamp negative/unknown age/revisit inputs before decorative fleck placement so
  corrupt legacy values cannot generate unstable off-field material coordinates.
- No Reader data model, note persistence, passage navigation, cycle semantics,
  deletion behavior or BookArtifact derivation is changed.

### Verification and remaining gates

The four deterministic material-policy tests from #293 are preserved and one Alpha
regression covers negative legacy age/revisit inputs. Integration was applied on
the exact W0d head. GitHub-hosted Actions are currently not assigning a runner
(`runner_id=0`, zero steps), so Android compilation/device rendering is still
unverified rather than failed by this source. Full small-screen, large-text,
TalkBack, back-navigation and dossier scroll checks remain W0 gates.

PR #293 remains source provenance and should not be merged wholesale.
Remaining source reconciliations: #297–#298.


## W0f — persistent discovery reconciliation

Inputs: W0e `529fdc72566b95f238d4d4051d15ad64ecabca30` and PR #294
`a0514e2ef31b67d73981dc13f47657d1afc38589`.

### Decision

Do not install #294's second discovery StateFlow/catalog beside the canonical
`ReaderProfile.earnedDiscoveries` + `GameRepository.publish()` owner. Evolve the
existing one-way ledger in place and treat timestamps/presentation as metadata.

### Implementation and hardening

- `VeiledDiscoveryPolicy` remains the sole qualification policy and owns stable
  catalog order plus merge-only semantics.
- Preserve the existing `earnedDiscoveries` SharedPreferences set and ReaderProfile
  field as the authoritative ID ledger.
- Record `discoveryRecordedAt:<id>` only for IDs newly observed after this version;
  historical discoveries keep a null date rather than receiving fabricated history.
- Patient Flame can be grandfathered by the already-durable `seven_days` sigil,
  so a streak reset cannot prevent recognition of prior seven-day proof.
- `GameRepository.discoveryRecords()` projects metadata from the canonical set;
  it is not a second StateFlow. Unknown future IDs remain preserved.
- Profile renders reveal state only from durable records. Symbols/titles/lore are
  centralized in one presentation catalog shared with Sanctum.
- Sanctum projects the same ledger; only known current IDs influence atmosphere,
  and W0b's shared temporal phase remains the time owner.
- No Room migration, Reader/XP/rank change, second preference namespace or second
  discovery state machine is introduced.

### Verification and remaining gates

W0f was split into compile-safe atomic slices:
`b2f27ccd2ad21c349eeabc9c969c11efbc7099f4` (policy/persistence),
`79f4838a98cf551bcb20c85985b10eafa861043c` (presentation catalog),
and `8852181667be1a2bc9e4191e9d7e15bf2e742978` (Profile projection).
Five domain regressions cover permanence/rules/gating/unknown IDs/legacy proof,
and presentation order has a dedicated test. Android CI is still blocked before
runner assignment; restart/restore, device rendering and accessibility remain gates.

PR #294 remains provenance only. Remaining source reconciliations: #297–#298.


## W0g — factual Reading Signature reconciliation

Inputs: W0f head `065e8bf6611c1b3651594688ef3d8946bb1ce21a` and PR #295
`bdd2a2ee3cdf33c06a623fbfbc58ae43d8f41604`.

### Decision

Adopt the signature only as a derived dossier view over durable sessions/cycles.
It does not persist a profile, classify personality, infer motivation/taste, or
affect Reader, XP, rank, quests, discoveries, unlocks or recommendations.

### Implementation and hardening

- Derive median positive active-session duration, timed active days, distinct
  session-touched volumes, daypart counts, factual event rates and reread share.
- Require at least three timed sessions and a unique maximum before showing a
  leading daypart; rate metrics remain hidden below 15 recorded active minutes.
- Legacy untimed sessions still contribute to duration/rates but not dayparts.
- Reject future/corrupt session starts from daypart/date evidence and future
  completion timestamps from cycle evidence.
- Extend the W0b foreground temporal context to expose both phase and ZoneId.
  Profile consumes that shared ZoneId, so ACTION_TIMEZONE_CHANGED can refresh the
  signature even when the visual phase itself stays unchanged.
- The UI states the evidence window and timezone limitation explicitly and never
  presents event ratios as traits.
- Compact phones and large-text layouts stack the clock/facts and metric cards
  instead of forcing three narrow columns; the daypart graphic has one coherent
  accessibility description.

### Verification and remaining gates

PR #295's seven pure-policy tests are preserved and one Alpha regression covers
future timestamp contamination. W0g also keeps the existing shared clock owner
rather than adding a Profile timer. GitHub-hosted Android CI is still failing
before runner assignment, so Compose resolution, device rendering, TalkBack,
timezone broadcast behavior and large-font screenshots remain W0 gates.

PR #295 remains source provenance. Remaining source reconciliations: #297–#298.
