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
