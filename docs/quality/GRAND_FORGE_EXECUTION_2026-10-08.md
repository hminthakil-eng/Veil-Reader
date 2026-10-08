# Grand Forge — live Wave 0 / Wave 1 execution ledger

Evidence date: 2026-10-08 UTC. Archaeology baseline: `3cc162aeadd5c4ced809e26e110946244cc74560`. This ledger supersedes historical status claims, not existing working implementations. **No feature in this wave is product GREEN.** Source, automated tests, physical-device QA, UX QA and release availability are separate gates.

## Repository truth and reversible execution

Live GitHub inventory: 283 branches across three nonempty pages; open PRs and latest 30 workflow runs inspected. `main` is `cbf27c818409ad63d2c2fa55178bfd6c16a87642`, not the newest Reader development tip.

| Role | Live branch / head | Evidence and decision |
|---|---|---|
| Canonical Page Engine integration | `integration/page-engine-canonical-v1` / `58a8ced0839417cdb08effdfac59f8657a519370` | Ancestor of current working head; canonical Paper source guard retained |
| Canonical app integration | `grand-forge/arena-app-hardening-v1` / `c7037e193ed64855959edb155383b26ddfa6bb5a` | Existing roadmap names this branch; ancestor of current working head, open draft PR377 |
| Newest upstream Reader/TTS development | `grand-forge/tts-neural-shared-assets-hardening-v1` / `b31b7622dc74c0e9d738b785acfd697c733b4651` | PR426 → PR424 runtime → PR423 listening mode → app integration. Includes shared-asset symlink regressions; newer than standalone PR420/421 |
| Reversible execution branch | `codex/reader-quality-hardening-20261007` / `3cc162a…` | Open draft PR427 stacked on426; exact current development tip, includes upstream ancestry and prior hardening |
| Release promotion | draft PR261, Reader RC → main | Historical RC is not automatically the newest canonical Reader. No merge or public promotion performed |

There is a stacked development chain, not evidence of a merged release-ready canonical product. Continue on PR427; preserve existing integration branch ownership. PR418/419 durability changes were previously adapted into this line without adding a second journal. Do not merge old competing branches blindly. Rollback: revert each coherent execution commit on this development branch; feature gates and stable renderer paths remain available.

## Baseline evidence

- Android CI [37738431520](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37738431520): PASS for3cc162a, unit suite, instrumentation compilation, Room schema, lint, debug/release builds, optimized archive verification, performance-harness compilation. PR merge tree `096aada4263e25def5c7e929a23af8f00326c1f0` equals head source tree.
- Durability [37738431558](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37738431558): PASS, checkout exact3cc162a, API35/lavapipe, six scenarios ×18 cycles =108 abrupt-process samples. This does not measure finger gestures or GPU presentation.
- [Baseline debug APK ZIP](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37738431520/artifacts/11533651465). Artifact archive SHA256 `04e6a01f45fbcce02f558f253c31a00e2e33b17e7cc139e6495f11452b3a1a43`; archive digest is not the APK digest.
- Local existing policy checks39, canonical Paper guard and four guard regressions passed before this wave.
- `quality/physical-device-status.json` remains UNVERIFIED. No physical Android device, audible engine playback, rendered Persian/Arabic publication, TalkBack or first-turn high-refresh proof is available here.
- Exact-head performance execution was absent: only Android CI and durability workflows ran. Performance/storage workflows omitted PR427's upstream base in branch filters. This wave restores those existing lanes; no replacement harness.
- Fetch of persisted `performance-history` snapshot failed; no usable current baseline obtained. Do not fabricate a relative before/after comparison.

## Dependency / ownership map

| State or resource | Authoritative owner | Integration edge | Invariant |
|---|---|---|---|
| Reader route/book/open generation | VeilApp route + ReaderViewModel / ReaderOpenAttemptGate | ReaderScreen session-fenced callbacks | Old open/result cannot mutate a new session |
| App/Reader preferences | SettingsStore DataStore | requested appearance → capability/rollout policy → mode handoff → mapped Readium preferences | Requested and effective appearance are intentionally distinct; explain unavailable modes |
| Touch/key routing | ReaderInputArbiter | Paper → Slide → static → directional → native renderer | One registered input listener; selection/accessibility/native Scroll keep their owner |
| Paper reservation/navigation | PaperCurlInputListener + PaperTurnOperationFence | PaperCurlState → MaterialPageEngine; semantic commit to ReaderViewModel | Reservation during INITIALIZING does not grant navigation before presentation |
| Paper surfaces/rendering | MaterialPageSnapshot + GpuMaterialPageCurlView | Engine sheet serial → GL buffer presentation acknowledgement | Do not move destination until source sheet is actually presented; no Slide substitution |
| Slide preview | SlideNavigationInputListener + SlidePageState | exact-origin navigator preview/restore | Only current operation finalizer clears state |
| Static PAGED drag | StaticPagedNavigationInputListener | unanimated navigator move → bounded settlement → semantic commit | Cancelled owner cannot begin mutation; errors release busy ownership |
| Locator progress | ReaderViewModel locator deduplicator/write lease → LocalLibraryRepository | atomic crash checkpoint + serialized Room queue | Room canonical, one journal, ordering by epoch/sequence not numeric progress |
| Bookmarks/highlights/notes | separate domain models + Room DAO | repository actions → Notebook/selection feedback | UI acknowledgment must match durable result, not cache/enqueue |
| Speech text/audio | Readium content → ReaderTtsSession → Android backend; service owns separate publication | controller or foreground session, never two active audio owners | Preserve source locator/content; transforms affect speech only |
| PDF surface | Readium/Pdfium native navigator | link hit test/input helpers + zoom mirror | Viewport is not one physical leaf/spread; no fake annotations |
| Library metadata/search | Room observers + LibraryScreen state + LibrarySearch policy | normalized query/filter/sort → lazy presentation | Caches are presentation snapshots, not durable operation authority |
| Backup | LibraryExport + ordered Room snapshot + staged restore | versioned archive, rollback guards | Expanded settings are not yet all represented |

Detailed file-by-file evidence and exact inspected source/test inventories:
- [Paper / input / rendering](GRAND_FORGE_PAPER_ARCHAEOLOGY_2026-10-08.md)
- [TTS / audio / text](GRAND_FORGE_TTS_ARCHAEOLOGY_2026-10-08.md)
- [PDF / tools / library / storage](GRAND_FORGE_TOOLS_LIBRARY_ARCHAEOLOGY_2026-10-08.md)

These are targeted source inspections, not claims of exhaustive formal verification. `GpuPageCurlModel.kt` is a historical requested filename: search for its actual current owner rather than creating a duplicate model.

## File-by-file P0 task graph and order

Each linked task contains severity, subsystem, affected files, observed/expected behavior, root cause, implementation/reuse plan, regression risks, tests, performance/UX implications, acceptance criteria, dependencies and status. Race candidates remain characterization tasks until reproduced.

| Wave | Task / files | Dependency | Current status / next action |
|---|---|---|---|
|0| GF-GATE-01: performance-benchmark.yml, storage-instrumentation.yml | live PR base discovery | Source fix in this wave; run existing lanes on new head |
|1| GF-W1-STATIC-OWNER: StaticPagedNavigationInputListener.kt, StaticPagedNavigationLifecycleTest.kt | existing settlement policy / arbiter | First safe production fix; seven real-listener regressions, await CI |
|1| GF-W1-BOOKMARK-ACK: LocalLibraryRepository.kt, ReaderScreen.kt, Daos.kt | orderedWrite + session fence | RED: enqueue/cache success precedes Room acknowledgment |
|1| GF-W1-SELECTION-OWNER: ReaderSelectionActionMode.kt | replacement callback characterization | YELLOW: old destroy callback can clear current selection flag |
|1| GF-W1-SETTINGS-READ-ERROR: SettingsStore.kt | injectable failure characterization | YELLOW: IOException recovery hides storage failure behind defaults |
|1| GF-TTS-05: ReaderTtsMediaPlayer.kt, checkpoint/service | asynchronous owner characterization | YELLOW: stale close/load race must be reproduced before extraction |
|2| GF-PAPER-RELEASE: VeilFeatureGates.kt, ReaderAppearancePolicy.kt, ReaderScreen.kt | Wave1 + physical renderer evidence | RED: requested Paper can map to PAGED/NONE; release GPU gate closed; explain availability, do not flip gate |
|2| GF-PAPER-DEVICE: Paper/Material/GPU/benchmark files | representative device access | BLOCKED: first-turn, 100 turns, RTL, GL recovery, pacing evidence |
|2| GF-PAPER-SPREAD: appearance policy + future leaf ownership | spread fixtures/architecture | YELLOW: keep fixed-layout Paper/Slide explicitly unavailable |
|3| GF-TTS-01,02: ReaderScreen.kt, ReaderListeningMode.kt, ReaderTtsControls.kt | foreground/service capability contracts | RED: release sleep timer no-op; active sentence UI ignores foreground owner |
|3| GF-TTS-03,04: ReaderTtsSession.kt, ReaderTtsContent.kt, settings | pending-read characterization | RED: unbounded content read; speech transform pipeline missing |
|3| GF-TTS-06: service/backend/controller/gates | prior TTS tasks + device | BLOCKED: background release readiness; neural/network not earned |
|4| GF-W4-BOOKMARK-LABEL | bookmark durable ACK | RED: rename missing; reuse existing real Bookmark domain |
|4| GF-W4-LOOKUP-PROVIDERS | selection ownership / capability UI | RED: dictionary/translation provider and compact result experience missing |
|5| GF-W5-PDF-ANNOTATIONS | native PDF fixture/coordinates/license reuse review | RED: rendered embedded annotations ≠ authored annotations |
|6| accessibility/RTL/adaptive/visual ledger | stable owners in prior waves | YELLOW: EN/FA 100/200%, actual RTL books, device captures required |
|7| GF-W7-BACKUP-PREFERENCES / semantic cloud/fonts/search/etc | P0 gates | RED preference round-trip gap; CLOUD_SYNC remains disabled |
|8| P2 formats/auto-scroll/advanced PDF/network TTS | all applicable previous gates | BLOCKED by execution order; no premature tools exposed |

### GF-GATE-01 — Existing regression lanes unreachable for current development base

- Severity/subsystem: P0 / QA-release evidence.
- Affected files: `.github/workflows/performance-benchmark.yml`, `.github/workflows/storage-instrumentation.yml`.
- Observed: PR427 targets newest TTS-hardening branch; these two filters exclude it. Exact3cc162a has no runs in either lane. Harness compilation is the only performance evidence.
- Expected: current Reader branch receives actual existing emulator benchmark/instrumentation coverage.
- Root cause: filters did not follow stacked development branch progression.
- Implementation/reuse: add only current base to existing branch lists; reuse all existing budgets, emulator setup, profile generation and Room/UI tests unchanged.
- Risks: newly reached latent tests may fail; emulator timing is noisy; job cost increases. Do not loosen tests/budgets to turn green.
- Tests: parse workflow config, inspect runs/head/source tree, retain failures/artifacts and diagnose. No mirrored unit test for a two-line reversible filter edit.
- Performance implications: CI cost only; no runtime overhead. Emulator smoke does not earn physical performance status.
- UX implications: actual rendered tests can expose defects that compilation misses; any visual captures must be reviewed separately.
- Acceptance: both lanes trigger for current base and produce auditable pass/fail on matching source tree; physical acceptance separate.
- Dependencies: repository truth; existing enabled workflows.
- Status: YELLOW source changed, CI pending.

## Acceptance matrix

Tests below describe baseline evidence only; implementation and tests alone are never GREEN. Release-enabled means this build's user-facing capability, not public promotion.

| Feature | Implemented | Integrated | Tests | Device QA | UX QA | Release enabled | Status |
|---|---|---|---|---|---|---|---|
| Progress durability | Yes | Yes, one journal/Room | 108 emulator faults + unit | Missing | Missing | Yes | YELLOW |
| Static paged lifecycle fix | Source patch | Existing arbiter | Seven regressions pending | Missing | Missing | Existing mode yes | YELLOW |
| Paper physical animation | Substantial GPU engine | Debug/benchmark | Unit/simulated ACK | Missing | Missing | No | RED user blocker |
| Paper failure notices | Yes | Existing snackbar/mode callback | Baseline CI pass | Missing | Missing | Renderer notice only when enabled | YELLOW |
| Fixed-layout Paper/Slide | No per-leaf implementation | Explicit policy disables | Policy tests | Missing | Missing | No | BLOCKED |
| Foreground TTS | Yes | Readium/Android backend | Unit, platform smoke not audible | Missing | Missing | Yes | YELLOW |
| Background TTS | Yes service/controller | Debug review | Contracts, audio matrix missing | Missing | Missing | No | BLOCKED |
| Neural TTS | Models/runtime interfaces | No executable native synthesis established | Packaging/recovery tests | Missing | Missing | No | BLOCKED |
| Network TTS | Provider concept | No owned implementation established | Missing | Missing | Missing | No | BLOCKED |
| Bookmark | Real domain/DAO | Create/list/jump/delete | Tests flush before assertions | Missing | Missing | Yes | RED ACK / rename gap |
| Selection/lookup | Native + PROCESS_TEXT | Yes | Policy/intent | Missing | Missing | Basic actions yes | YELLOW basic / RED provider depth |
| PDF reading | Pdfium | Native input/zoom/link | Fixtures compiled; new lane pending | Missing | Missing | Yes | YELLOW |
| PDF authored annotations | No creation path established | No | Missing | Missing | Missing | No | RED |
| Backup/preferences | Existing archive + partial prefs | Yes | Existing Room/backup tests | Missing | Missing | Archive yes | YELLOW archive / RED completeness |
| Cloud sync | No proper semantic model | No | Missing | Missing | Missing | No | BLOCKED |

## Performance baseline and budgets

Reuse `performance/budgets.json` and `performance/delta-budgets.json`. Do not interpret thresholds as measured results.

| Metric | Existing smoke budget | Existing physical budget | Before/after observation |
|---|---|---|---|
| Cold startup TTID median | ≤6000ms | ≤600ms | Not measured on current head |
| Reader gfxinfo frame P95/P99 | ≤400/600ms | Native trace metrics instead | New emulator lane pending; not tactile Paper proof |
| CPU frame P95/P99 | Not applicable | ≤16.7/24ms | Missing physical samples |
| Frame overrun P95/P99 | Not applicable | ≤0/8ms | Missing physical samples |
| GPU draw / texture upload / warm capture | Presence requirements on physical | Calibration required | Existing trace sections; thresholds not calibrated |
| First-frame latency / initialization / completion / 100-turn GC+allocation | No complete current-head baseline | Define from device traces after ownership stabilization | Missing; must instrument/measure, not invent speedup |
| Library/book/PDF open, search, TTS first audio, covers | No complete required P50/P95/worst baseline | Fixture/device-specific budgets to calibrate | Open tasks; startup/Reader budgets do not cover them |

Existing physical Reader benchmark: six iterations ×8 turns; smoke: six ×12 measured turns with two warm turns. Neither equals the required100 consecutive real finger turns. Warmup captures use existing prepare/View.draw trace sections. No persistent-page-surface rewrite justified without allocation/latency evidence.

## Reuse gate / clean room

Current safe static fix reuses existing navigator, semantic commit, bounded settlement and coroutine ownership patterns. No new engine/library. Existing Bookmark, Room queue, Readium tokenizer, Android offline voice selection, Media3 service, Pdfium surface, DataStore, backup staging and normalized library search stay authoritative.

Public official behavior references consulted2026-10-08: [Android accessible48dp targets](https://developer.android.com/guide/topics/ui/accessibility/apps), [adaptive quality](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality), [Readest documentation](https://readest.com/docs), [Readest TTS](https://www.readest.com/docs/listen), [Moon+ official FAQ](https://www.moondownload.com/faq.html). These provide requirements/capability references, not device benchmarks or evidence of competitor user satisfaction. Moon+10.7 remains requested capability benchmark, not a claimed latest version or UI template. ReadEra simplicity, Apple Books polish, Kindle/Kobo mature flows and Play Books interoperability are behavioral study targets, not completed comparative tests. Competitor complaint/satisfaction research and licensed reuse evaluations must be filled before those significant features are implemented. No proprietary APK/source/assets copied; private attached book text is not published.

## Next execution boundary

Finish current exact-head CI, storage and smoke measurements; characterize bookmark acknowledged-write correctness and stale selection callbacks next in Wave1. Extract Reader orchestration only after these tests; do not rewrite giant composables. Wave2 then addresses explicit Paper availability and physical renderer acceptance. Physical checks stay BLOCKED when no device evidence is available; safe source work continues without pretending those gates passed.

## Dedicated Grand Forge continuation — bookmark creation durability

Repository truth refreshed at `b8ec35a14d0c670adc6774afba4f7b272cfb10b3`.
The dedicated `grand-forge/p0-kindle-reader-quality-20261008` branch and
`codex/reader-quality-hardening-20261007` have identical heads. Continue on the
dedicated branch specified by the user, superseding the earlier instruction in
this ledger to continue directly on PR427. Open PR427 and the TTS stacked chain
remain intact. Exact baseline Android CI37741413918, storage37741413937,
durability37741413944 and performance37741413848 all succeeded. This is baseline
evidence, not evidence for the new patch.

### GF-W1-BOOKMARK-ACK-CREATE

- Priority/subsystem: P0 / local annotation durability.
- User impact/observed: creation returns true and emits saved/haptic feedback before
  queued Room insertion; failure can leave a phantom bookmark in the cache.
- Expected: success means Room has committed; duplicate requests produce one record;
  failure produces no success feedback or optimistic cached record.
- Evidence/root cause: `LocalLibraryRepository.addBookmark` mutates StateFlow and
  enqueues a write; `ReaderScreen` immediately acknowledges its Boolean.
- Affected files/implementation: `LocalLibraryRepository.kt` makes addBookmark
  suspending and reuses orderedWrite plus Room transaction; `data/db/Daos.kt`
  queries the exact durable book/locator pair; `ui/screens/ReaderScreen.kt`
  captures the requested book/locator before suspension, acknowledges after commit,
  fences asynchronous feedback to the requesting session, preserves cancellation
  and displays a localized storage error. EN/FA strings added.
- Reuse opportunity: existing serialized queue, Room transaction and session fence;
  no second storage system, schema migration or proprietary code.
- Risks: callers now suspend; a cancelled caller can leave a committed bookmark
  without feedback, but retries detect that durable duplicate. Duplicate identity
  remains exact locator JSON; semantic normalization is separate work.
- Tests: `AnnotationDurabilityInstrumentedTest.kt` adds immediate committed-read,
  20 concurrent requests and SQLite-trigger write-failure regressions. Assertions
  deliberately omit flushWrites at the acknowledgement boundary.
- Performance: one indexed-by-book lookup and insert transaction per new bookmark;
  no main-thread blocking. Device latency not measured.
- Accessibility/RTL: existing accessible one-action control retained; localized
  EN/FA failure feedback. TalkBack and RTL rendered verification remain pending.
- Acceptance: all three Room regressions pass, Android compilation/unit/lint/build
  pass, real-device create/reopen/failure and UX verification before GREEN.
- Dependencies: Wave0 repository truth; existing orderedWrite/session ownership.
- Status: YELLOW source patch; Android evidence pending. Bookmark deletion remains
  RED under the parent GF-W1-BOOKMARK-ACK task; creation is only one coherent slice.
- Rollback: revert this coherent commit. All seven release gates unchanged.

Local checks: reading policy39 PASS, canonical Paper source contract PASS, four
Paper guard regressions PASS, git diff whitespace check PASS. Local Gradle wrapper
download failed with connection refused; this executor also has no Android SDK
or attached device. Use existing GitHub Android/storage workflows for compilation
and emulator evidence; do not treat emulator success as physical-device proof.

Next P0 graph: bookmark create acknowledgement → bookmark delete acknowledgement
→ selection mode replacement ownership → progress checkpoint failure
characterization → navigation/input/Paper regressions → typography/RTL → TTS.
Reuse the file-level graph and archaeology above; no duplicate roadmap.

### GF-W1-BOOKMARK-ACK-DELETE

- Priority/subsystem/user impact: P0 / local annotation durability; an acknowledged
  removal can reappear after process death and storage failure hides the saved record.
- Observed/root cause: deleteBookmark immediately filters the cache, then enqueues
  deletion. Neither Notebook nor Archive catches persistence failure.
- Expected/implementation: deletion suspends through existing orderedWrite; Room
  observation removes the record only after commit. ReaderScreen and VeilApp
  launch the existing UI callback asynchronously, preserve cancellation and show
  localized EN/FA failure feedback. Reader feedback is session-fenced.
- Affected files: LocalLibraryRepository.kt, ReaderScreen.kt, VeilApp.kt,
  values/strings.xml, values-fa/strings.xml, AnnotationDurabilityInstrumentedTest.kt.
- Reuse: serialized queue/Room observation/session helper; no new persistence owner.
- Risks: missing ID is idempotent success; cancellation after enqueue can still
  commit. Cloud tombstones remain separate release-disabled work.
- Tests: direct Room read after deletion returns, idempotent retry, SQLite-trigger
  rejection retains both durable and visible record; no flush at acknowledgement.
- Performance/accessibility/RTL: one indexed primary-key delete; existing controls
  retained; EN/FA error feedback; device latency/TalkBack/RTL verification pending.
- Acceptance/dependencies: creation durability plus exact-head Android and storage
  regressions; real-device create/delete/reopen/failure acceptance before GREEN.
- Status: YELLOW source patched; exact-head automated evidence pending.
- Rollback: revert this deletion commit; release gates unchanged.

### GF-W1-SELECTION-OWNER continuation

- Priority/subsystem: P0 / Reader input and semantic selection.
- Observed/root cause: onDestroyActionMode always reports false, even when Android
  destroys an old mode after onCreateActionMode has reserved a replacement mode.
  The Reader can therefore release input ownership while selection remains active.
- Expected/implementation: only the exact active ActionMode can clear activeMode
  and report selection false. Repeated/stale/unowned destroy is ignored.
- Affected files: ui/screens/ReaderSelectionActionMode.kt and
  ui/screens/ReaderSelectionActionModeTest.kt.
- Reuse: existing callback identity; no parallel selection contract or rewrite.
- Risks: async clearSelection replacement races remain a separate characterization
  task; this guard fixes only destroy ownership.
- Tests: real callback with Android ActionMode/Menu under Robolectric; current
  destroy once, old destroy after replacement, unowned destroy. The original code
  would fail each expectation; exact-head execution remains pending.
- Performance/accessibility/RTL: constant-time identity check; preserves native
  toolbar and avoids accidental tap/page routing during selection; script-neutral.
- Acceptance/dependencies: unit tests and actual selection replacement/overlay
  transitions on device; existing input arbitration integration.
- Status: YELLOW source patch; no claim of physical selection QA.
- Rollback: revert this independent selection commit.

### GF-PAP-002 — unavailable transition explanation, first implementation slice

- Priority/subsystem: P0 / page-transition capability UX.
- Observed/root cause: saved Paper is remapped to PAGED while the release gate is
  closed; renderer-failure notices only run in presented Paper mode, so the remap
  cannot reach them. Fixed-layout remaps similarly lack an entry explanation.
- Expected/implementation: ReaderAppearancePolicy.kt reports a typed reason from
  the requested mode/publication/rollout; ReaderScreen.kt shows a dismissible
  notice after session readiness and coalesces the reason per Reader session.
  No preference mutation, implicit Slide selection or release-gate promotion.
- Files: ReaderAppearancePolicy.kt, ReaderScreen.kt, EN/FA strings.xml,
  ReaderTransitionAvailabilityTest.kt.
- Reuse: existing requested/effective appearance, SnackbarHost and readiness state;
  no renderer changes or duplicate transition engine.
- Risks: notice can queue behind other Reader messages; accessibility announcements
  and first-page presentation need device UX verification. Fixed-layout takes
  precedence over rollout restriction; PDF retains its separate capability UI.
- Tests: disabled Paper resolves to static paging with explanation and unchanged
  requested preference; fixed-layout Paper/Slide/Scroll remaps; available modes,
  Scroll with dormant Paper style, PDF and enabled Paper do not falsely warn.
- Performance: constant policy work, one notice per reason/session; no snapshots,
  textures, shader initialization or animation changes.
- Accessibility/RTL: dismissible localized EN/FA notice; no gesture is required.
  Rendered/TalkBack verification remains pending.
- Acceptance/dependencies: policy tests and CI; release-disabled saved-Paper entry
  and fixed-layout transition notice manually verified before GREEN.
- Status: YELLOW source implementation; renderer readiness/performance remains
  BLOCKED on representative physical hardware.
- Rollback: revert this notice/policy commit; all seven release gates unchanged.

### GF-PERF-RAW-ARTIFACT — preserve per-scenario raw benchmark data

- Priority/subsystem: P0 / release performance evidence.
- Observed/evidence: exact06a94b57 artifact11539013553 contains the normalized
  startup median but only the final combined reader benchmarkData JSON.
  run_android_ci_emulator.sh preserves each scenario under
  benchmark/build/perf-results; the upload path omits that directory.
- Root cause/expected: repeated instrumentation overwrites the shared output
  filename; archived evidence must retain the scenario copies used by normalization.
- Implementation/file: add benchmark/build/perf-results/ to the existing
  .github/workflows/performance-benchmark.yml artifact paths. Reuse the existing
  snapshot/harness; no benchmark, budget or production behavior changes.
- Risk: slightly larger debug evidence archive; publication fixtures remain inert.
- Tests/evidence: inspected actual ZIP entries and script output paths; whitespace
  validation. The new retention path has not executed yet, so artifact recovery
  of future raw startup samples remains YELLOW.
- Performance/accessibility/RTL: CI retention only; no runtime impact.
- Acceptance/dependencies: next dispatched benchmark artifact includes separate
  startup/reader raw JSON and supports median/P95/worst-case interpretation.
- Status/rollback: YELLOW infrastructure patch; revert this one path if needed.

## Current Wave 1 verification boundary

All runtime changes in this continuation are tested at06a94b57. Exact reports,
before/after coverage, benchmark measurements, APK identity and remaining
acceptance gaps are recorded in
[the master matrix](GRAND_FORGE_MASTER_MATRIX_2026-10-08.md).
Android961, storage163 and36 abrupt-process samples passed; debug/release
compilation, archive verification and emulator profile/smoke budgets passed.
GF-W1-BOOKMARK-ACK-CREATE, GF-W1-BOOKMARK-ACK-DELETE,
GF-W1-SELECTION-OWNER and GF-PAP-002 now have automated evidence, remain
YELLOW for missing physical/UX/performance acceptance, and supersede the
earlier RED/pending-source entries in this historical ledger.

Observed emulator startup/P95/P99 values increased versus the previous unpaired
run. No speedup is claimed, no physical performance gate is earned, and cause
remains unresolved until controlled measurements. Lint debt is unchanged at174
warnings, zero errors. No failures occurred in the exercised suites. The
post-test CI raw-report retention change is independently YELLOW/unexecuted.

Physical target: user-reported S24 Ultra, manual APK installation; no remote
endpoint is connected. [Device protocol](GRAND_FORGE_S24_ULTRA_QA_2026-10-08.md).
Do not alter quality/physical-device-status.json without actual physical results.
All seven risky release gates remain false. No production release, main merge
or feature promotion occurred.

Next execution boundary: record actual S24 bookmark/selection/notice outcomes;
investigate any device failures immediately. Then continue existing
navigation/input/Paper readiness tests and controlled first-turn/100-turn,
RTL/large-font/TalkBack acceptance before renderer promotion. Deferred async
selection-clear replacement races, progress-checkpoint failure acknowledgement,
foreground TTS integration gaps, PDF/device corpus, expanded preference backup
round-trip and10k-library budgets remain open. The whole-app audit and later
waves are not claimed complete.

## GF-TYPE-001 — viewport-aware reading columns

- Priority/subsystem: P0, adaptive typesetting.
- User impact/observed: user reports no perceivable Reader improvement on S24 Ultra. Source mapping delegates AUTO/TWO directly to Readium without consulting Veil font size or Android accessibility scale. This does not establish the device's actual column behavior.
- Expected/root cause: columns should fit the text measure; the effective preference previously lacked a viewport policy.
- Files/implementation: `ui/screens/AdaptiveTypesettingPolicy.kt` resolves reflowable EPUB columns; `ReaderScreen.kt` uses it for both presentation and appearance-close acknowledgement, observing window configuration and font scale. Saved preferences remain intact. EN/FA column explanation added.
- Reuse: Readium retains rendering, locators and layout; this policy only supplies preferences. No competitor implementation or assets used.
- Acceptance: narrow windows and large text use one column; eligible wide windows use two; explicit ONE remains ONE; scroll stays single-column; PDF/fixed-layout untouched; preference restores when space returns.
- Risks/performance: width threshold is a conservative first policy, not a complete typesetting subsystem. Resize may trigger existing anchored reflow. Actual text measure, margins, RTL corpus and tablet UX still require rendered validation. No performance improvement claimed.
- Accessibility/RTL: large system scale prevents cramped columns; no physical page direction changes.
- Tests: six unit cases cover width, reader/system size, scope, scrolling, explicit ONE, invalid inputs and preference preservation. Local reading-policy39 and Paper source guard passed. Android compilation/unit/device evidence pending.
- Dependencies/status: existing appearance relayout transaction; YELLOW, not a finished Reader overhaul.

## GF-W1-PROGRESS-FAILURE — reject failed semantic checkpoints

- Priority/subsystem: P0 local durability, Reader progress and close transaction.
- Observed/evidence: `saveProgressLocked` reported a failed crash journal but still replaced the cached Book and queued Room; `saveReaderProgress` returned accepted=true; `ReaderViewModel` ignored the durability flag. Close could then acknowledge storage success.
- Expected/user impact: retain the last accepted position when synchronous recovery fails; warn the reader and permit a same-position retry rather than falsely acknowledging progress/completion.
- Files/implementation: `data/LocalLibraryRepository.kt` rejects failed checkpoints before cache, timestamp, milestones, queue or writer-order advancement. `ui/reader/ReaderLocatorPolicy.kt` rolls back the pending dedup owner and supports a durability retry after duplicate observations. `ReaderViewModel.kt` publishes failure, delays page credit until acceptance and clears failure only after a semantic durable save. `ReaderScreen.kt` observes a distinct failure flag, presents an EN/FA warning and rejects successful close before session finalization when a checkpoint remains unsaved.
- Reuse: existing AtomicFile journal, ordered Room pipeline, snackbar and anchored final-snapshot transaction; no new persistence engine.
- Acceptance/tests: block checkpoint directory with a file and verify no cache/Room acknowledgement, then remove obstruction and retry identical sequence; reject malformed completion without marking finished; dedup tests preserve saved origin, allow retry after an observation, and reject stale rollback.
- Risks: on persistent storage failure Reader stays open with an explicit warning; it does not undo the renderer's physical page movement. Scroll/relayout observation durability and real storage exhaustion remain separate acceptance gaps.
- Performance: same journal write on semantic commit, no new heartbeat-driven whole-screen updates; latency/device traces pending. Accessibility: localized non-gesture recovery instructions; TalkBack announcement pending. RTL: locators and semantic direction unchanged; real fixture acceptance pending.
- Dependencies/status: existing journal/close contracts; YELLOW, Android/storage/process tests pending. Rollback is this isolated fix commit.

## GF-NAV-003 — precise jump admission inside coarse positions

- Priority/subsystem: P0 navigation transaction, search/highlight/bookmark/return actions.
- Observed/evidence/root cause: `shouldStartReaderIdentityJump` used tolerant settlement identity as its no-op predicate. Position equality takes precedence over CSS selector and viewport progression; the existing visual-departure characterization confirms adjacent viewports can share a position chunk. A precise destination can therefore be swallowed before `nav.go` is called.
- Expected/implementation/files: `ui/reader/ReaderNavigationTransaction.kt` admits jumps when known paragraph selectors or meaningful viewport progression differ; tolerant settlement remains unchanged because first-visible renderer anchors need not equal selected paragraphs. Same fine anchor stays a no-op.
- User impact/reuse: existing notebook/search/return callers get the correction without a new navigator or interaction flow. Readium keeps navigation/layout.
- Tests: three unit cases cover paragraph targets, adjacent viewports/noise, identical fine anchors and unchanged tolerant settlement. Android CI pending.
- Risks/dependencies: a target elsewhere on the same visible page can still yield a no-op navigator emission; existing timeout remains the safety boundary. This does not implement ReadingAnchor/ExplorationLocator separation, which remains RED.
- Performance/accessibility/RTL: constant-time identity comparisons; no animation or physical direction changes. Real target focus, reflow/RTL corpus and response metrics pending.
- Acceptance/status: precise targets reach the navigator while identical anchors are ignored; YELLOW until tests/device acceptance. Rollback: isolated fix commit.

## GF-PAP-004 — lifecycle-fenced idle surface preparation

- Priority/subsystem: P0 Paper readiness/performance and shared Slide buffer lifecycle.
- Observed/root cause/evidence: idle prewarm effects observed session/mode/preview/revision but not lifecycle. A delayed capture or pause-time state change could prepare buffers after hidden-reader cleanup. Material preparation rechecked revision/viewport after its visual-state wait, but not lifecycle/session ownership.
- Expected/implementation/files: `ReaderScreen.kt` tracks lifecycle resume in the existing observer, cancels warm effects at pause, releases idle buffers, rechecks resume before allocation and prepares again on resume. `PaperCurlState.kt` forwards a current-source predicate to `MaterialPageEngine.kt`, which validates it before visual readiness, before allocation/capture and before accepting prepared pixels. Existing GL initialization, input reservation, texture leases and physics stay intact.
- Reuse: current generation/viewport/snapshot fences and idle prewarm; no new engine or fallback.
- Tests: `MaterialPagePreparationLifecycleTest.kt` exercises attached-but-hidden sources, readiness ownership change with zero capture calls, resumed preparation, and invalidation during capture using a real attached Robolectric View/provider seam. Source guard and reading-policy39 pass; Android tests pending.
- Acceptance/risks: hidden sessions must not create/retain new warm snapshots; resume earns a fresh current capture. Physical background loops, memory, first-turn latency and GL presentation remain unverified. Default predicate preserves existing non-UI test callers; Reader passes live lifecycle/session ownership.
- Performance/accessibility/RTL: avoids hidden CPU capture/allocation; no measured improvement yet. No direction/gesture or accessibility navigation changes.
- Dependencies/status: existing prewarm and upload-lease ownership; YELLOW. GPU release gate remains false. Rollback: isolated lifecycle patch.

## GF-TTS-003 — foreground sleep timer and Listening Mode text

- Priority/subsystem: P0 default system/foreground TTS integration.
- Observed/evidence/root cause: `ReaderScreen.kt` passed a sleep callback that only addressed the background service; with BACKGROUND_TTS false it silently did nothing. Listening Mode active-text input similarly read only the service despite `ReaderTtsState.activeText` already being characterized in the core session.
- Expected/implementation/files: `ui/reader/tts/ReaderTtsSession.kt` owns a cancellable foreground deadline job/flow, reuses existing sleep validation/deadline policy, pauses at expiry, retains valid timers on invalid requests, supports replacement without synthesis restart, and clears on stop/close. `ReaderScreen.kt` routes to exactly the service or foreground owner, observes its deadline and supplies foreground semantic text. `ReaderTtsControls.kt` adds the requested10-minute preset to15/30/45/60.
- Reuse: existing speech session, state, pause/audio focus behavior and sleep policy; no new playback engine.
- Tests: four deterministic virtual-time session tests cover exact expiry, no utterance restart, replacement/cancel, invalid/overflow preservation, stop/close ownership. Existing active-text lifecycle characterization is reused. Unit/build evidence pending.
- Risks/acceptance: foreground timer is session-local, not a background playback or persisted alarm promise. End-of-chapter remains unsupported/unexposed. Audible expiry, TalkBack state, multilingual highlighting, thermal and physical device acceptance pending.
- Performance/privacy/RTL: one suspended timer job, no text persistence/new network or publication mutation; semantic active text retains source script. No measured battery claim.
- Dependencies/status: default foreground session and existing Listening Mode; YELLOW. Background/neural/network gates unchanged. Rollback: isolated timer/integration commit.

## GF-REL-001 — non-destructive S24 QA delivery

- Priority/subsystem: P0 device QA and install/update continuity.
- Observed/evidence: Google apksig8.7.3 verified both debug APK v2 signatures. Historical06a certificate SHA-256 is dd01c2c1db33163000319e737b57cb82c6388870abe751d2711ad879b1a5ffd2;3539 is c41f70571eda55608f9f6413782df20eeb2a4b2313b770a67339cb4071e1436e. They differ: Android cannot update that prior installed package with this APK. Do not solve by asking the user to uninstall personal data.
- Expected/implementation/files: `app/build.gradle.kts` adds explicit `veilForgeQa` build property for com.veilreader.app.forgeqa, clear QA label/source-suffixed version and a dedicated non-production key path; default identity remains com.veilreader.app. `app/src/main/AndroidManifest.xml` resolves a configured label. `.github/workflows/forge-qa-apk.yml` runs QA-configured unit/compile/lint/build and actual emulator installation/storage tests, retains its QA key cache and verifies package, label, signature/hash before artifact upload. `tools/run_android_ci_emulator.sh` accepts an optional test package for capture/probe routing, preserving the production default and fully qualified debug class namespace.
- Durability/risk: separate installation has its own library/permissions, so the user imports a test book there; existing Veil data is retained by Android. Signing-key cache retention is not guaranteed. Once the first QA certificate is pinned, the workflow fails if the key is missing/mismatched instead of silently generating an incompatible update. This is not production signing or a stable-key recovery guarantee.
- Tests/status: shell/YAML/XML syntax and Paper guard pass locally. Actual QA/default builds, Android QA install, signer cache/pin evidence pending; YELLOW. No production release/gate promotion.
- Reuse: existing Android/Gradle/emulator/APK verification infrastructure; official Google apksig verifies local artifacts, not embedded in the app.
- Performance/accessibility/RTL: no renderer changes in this packaging task; separate QA label identifies installation. Reader/device acceptance remains missing. Dependencies: GitHub Actions cache and Android runner. Rollback: remove explicit QA property/workflow; normal defaults are preserved.

## GF-TYPE-002 — lint-correct actual window sizing

- Evidence/root cause:3539 passed980 units,165 instrumented tests and36 process-death samples, but lint grew174→175 with ConfigurationScreenWidthHeight. It reports rounded/inset-dependent screenWidthDp is not actual window size.
- Implementation/files: `ReaderScreen.kt` now converts LocalWindowInfo.containerSize pixels through current density for the adaptive policy.
- Priority/impact: P0 split-screen/foldable typography correctness; no preference mutation.
- Tests/acceptance/status: prior policy tests remain applicable; new source compile/lint and QA device rendering pending, YELLOW. This correction is not represented by the older3539 APK.

## GF-REL-002 — package-aware SAF reader assertions

- Priority/subsystem: P0 non-destructive QA installation verification.
- Observed/evidence: QA37765266653 installed and passed164 Android tests, but
  ReaderSafImportInstrumentedTest timed out looking for com.veilreader.app resource
  IDs. Unit980 and lint0 errors/174 warnings passed in the same QA configuration.
- Root cause/files/implementation: ReaderSafImportInstrumentedTest.kt hard-coded
  resource ownership while launching targetContext.packageName. Use the installed
  target package for resourcePager/webView IDs before and after rotation.
- Expected/acceptance: identical import/open/rotation assertions execute for the
  default and Forge QA packages; do not weaken the expected navigator assertions.
- Reuse/tests: existing actual SAF import and orientation test; rerun37768309693.
- Risks/performance/accessibility/RTL: test-only ID resolution; no user runtime,
  layout, gesture or persistence change. Dependencies: separate QA build.
- Status: YELLOW pending rerun; rollback isolated test commit efa3fb20.

## QA delivery evidence — efa3fb20

[QA37768309693](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37768309693) passed980 units/170 suites,165 Android tests,
zero failures/errors/skips, lint0 errors/174 warnings. The corrected SAF test
verifies import/open/rotation under com.veilreader.app.forgeqa. Signed version
0.10.0-forge-efa3fb20 and [APK artifact](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37768309693/artifacts/11547406062) verified independently;
SHA-256 e45956c76cd3720a12e7a1035ccf3ae91c92d6a3d0563c1543f808e1b2632449.

GF-REL-001/002 and GF-TYPE-002 now have build/instrumentation evidence and remain
YELLOW for manual S24/UX acceptance. Prior3539 behavior/storage/process/performance
results remain separately identified. No claim of a new process-death/performance
run against the QA package. The QA key cache save passed; public certificate pin
and both current/legacy apksigner output parsing pass locally. Subsequent cache
restore and upgrade installation remain pending. No gate promotion or production
merge. Revert isolated behavior/build commits for rollback; physical status JSON
remains UNVERIFIED. Full mission still includes ReadingAnchor/exploration,
foreground listening checkpoint, corpus/RTL/selection/PDF and device performance.
