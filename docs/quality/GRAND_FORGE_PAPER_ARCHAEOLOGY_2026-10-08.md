# Grand Forge: Paper, Slide and input ownership archaeology

Source baseline: `3cc162aeadd5c4ced809e26e110946244cc74560`, inspected 2026-10-08. This document records source evidence, not physical-device acceptance. Repository/branch canonicality and CI attribution belong to the master execution record. No feature in this document is declared product GREEN.

Paths below are relative to the repository. Screen files are under `app/src/main/java/com/veilreader/app/ui/screens/`; Material files under `app/src/main/java/com/veilreader/app/ui/reader/material/`. This inspection read ownership, input, capture, presentation, recovery and performance contracts; it did not execute a GL driver, measure latency or visually inspect a running APK.

## Ownership and dependency map

| File | Authority / dependencies | Evidence and boundary |
| --- | --- | --- |
| `ReaderScreen.kt` | Reader session, appearance handoff, input installation, lifecycle cancellation, accepted locator delivery, error presentation | 6,537 lines at baseline. Requested/effective/presented/accepted appearance are distinct staged states, not automatically duplicate bugs. Lines 335–358 apply format and rollout policy; 2120–2490 construct one arbiter and register it. Extract only after characterization. |
| `ReaderAppearancePolicy.kt` | Pure requested-to-capable appearance mapping | Lines 24–30 map gated EPUB Paper to paged/none; 37–50 restrict fixed-layout to static paging. Does not mutate saved preferences. |
| `ReaderInputArbiter.kt` | One Readium listener; delegates in product order | Lines 269–282 dispatch Paper, Slide, then static. Selection/accessibility yields renderer ownership; blocked overlays consume. Startup Paper reservation precedes static fallback, avoiding duplicate ownership. |
| `PaperCurlInputListener.kt` | Per-operation gesture reservation, preview navigation, release commit and origin restoration | Lines 53–78 separate READY navigation from INITIALIZING drag reservation; 310–320 reserves Start; 432 limits non-preview direct release to reduced motion; 643–710 retries startup preparation and requires sheet presentation. Generation-fenced finalization at 881–930 already exists. |
| `PaperCurlState.kt` | Screen-facing Paper visual state and performance phases | Delegates capture/physics to one Material engine. Lines 95–116 invalidate sheet presentation on non-READY status; 130–168 require READY to begin. Its `active` and engine `active` mirror a wrapper contract; no proven divergence found. |
| `SlideNavigationInputListener.kt` | Separate Slide transaction owner | Uses origin/preview/commit bookkeeping and fenced finalization (688–739). Reuse lifecycle discipline, not its visual engine, for static fixes. |
| `SlidePageState.kt` | One reusable CPU snapshot + translation animation | Lines 48–65 prepare/capture, 126–150 clear, 161–190 release/reuse. Does not implement Paper. |
| `StaticPagedNavigationInputListener.kt` | Own static drag; one unanimated navigator request followed by bounded departure observation | Baseline lines 34–99 had owner-cancellation and exception cleanup gaps. First safe implementation below addresses those only. |
| `VeilDirectionalNavigationInputListener.kt` / `ReaderTapZoneInputListener.kt` | Semantic keys and configured tap routes | ReaderScreen's semantic callback routes Paper/Slide explicitly. Direct fallback uses `animated=false`; this is functional fallback, not a curl. |
| `MaterialPageEngine.kt` | Snapshot pool, revisions, GPU upload leases, sheet epochs, progress, physics, sensory dispatch | Lines 170–180 wait up to 500ms for exact sheet; 213–285 fence leases; 305–398 warm source preparation; 401–510 start; 650–805 settle/clear; 964–1007 choose writable buffers. Two CPU buffers, one source texture contract. |
| `MaterialPageSnapshot.kt` | Capture seam, WebView visual readiness, revision/dimension/age validation | Lines 66–101 wait for WebView visual callback; 109–142 bound prepared age to 3s; 170–225 draw into caller-owned bitmap. Software `View.draw` remains a source capture dependency. |
| `GpuMaterialPageCurlView.kt` | GL/TextureView context, textures, immutable submitted frames, acquired-buffer acknowledgement, retry host | Lines 175–190 correlate exact acquired timestamp; 263–367 create/context+viewport READY; 371–537 draw; 778–819 upload; 1447–1510 Compose host status/retry. Two bounded automatic retries, no fake draw-based presentation acknowledgement. |
| `GpuPageCurlModel.kt` | Pure geometry, mesh quality and memory estimate | Finite clamps and mirrored side geometry. Storage estimate exists at 51–64; source estimate is not measured residency. |
| `MaterialPagePhysics.kt` | Resistance, lift, release and settle policy | Finite input guards, reverse cancel and velocity projection already present. Do not rewrite without physical observations. |
| `MaterialPageProfiles.kt` | Five material parameter profiles, canonical presets | Appearance/physics/lighting/sensory parameters are source data; perceptual quality remains unverified. |
| `MaterialPageSensory.kt` | Material-specific acoustic/haptic cue model and adapter | Not navigation authority. Physical sound/haptic quality and accessibility remain device tasks. |
| `feature/VeilFeatureGates.kt` | Release availability | Line 33: GPU Material Page false. All other risky release gates also false. |
| `diagnostics/ReaderPerformanceMetrics.kt` | Content-free JankStats mode/phase/work metadata | Complements capture/upload/draw traces; no physical distribution measurements supplied. |

The transaction path is: requested appearance → format/rollout capability → presented mode → arbiter reservation → listener operation → source capture → acquired GPU source sheet → destination preview → semantic commit → accepted locator durability. Cancellation restores uncommitted previews; accepted real static turns use existing locator observation. Paper and Slide remain separate engines and delegates.

## Characterization/test inventory inspected

| Test / guard | Verified source coverage | Limit |
| --- | --- | --- |
| `PaperDiscreteTurnLifecycleTest.kt` | Origin restoration; late worker fences; twenty inline turns; navigation/commit/restore errors; cancelled owner; INITIALIZING reservation; presentation timeout and simulated acknowledgement; failure callbacks | Robolectric and simulated presentation, not physical GL |
| `PaperTurnOperationFenceTest.kt` | Exact token ownership and rollover | Pure helper contracts |
| `PaperReleaseVelocityTest.kt` | Velocity, direction, reverse release policy | No optical/frame evidence |
| `PaperPerformancePhaseTest.kt` | Phase metadata transitions | Instrumentation state, not latency measurement |
| `MaterialPageRolloutContractTest.kt` | Release-disabled mapping, selector availability, READY vs reservation, reduced motion, renderer notice policy | Does not exercise full Snackbar UI |
| `ReaderInputArbiterTest.kt` / `ReaderTapOwnershipTest.kt` | Mode routing, static threshold/progression helpers, tap owners | Baseline lacks real static listener cancellation/error tests |
| `MaterialPageModelTest.kt` | Sheet epochs, GPU leases, clear cancellation, snapshot age/revision/dimensions, physics, tone, sensory | No driver execution |
| `GpuPageCurlModelTest.kt` / `GpuPageShaderContractTest.kt` | Mesh, geometry, storage estimation, shader source contracts, generations and acquired-buffer match helpers | Shader source validation is not GPU rendering |
| `VeilDirectionalNavigationContractInstrumentedTest.kt` | Directional/keyboard ownership fixture | Paper keyboard fixture uses reduced motion; not normal curl optical acceptance |
| `tools/verify-canonical-paper.sh` / `tools/test_canonical_paper_guard.py` | Single canonical source engine and mutation guards | Static integrity |
| `ReaderFrameBenchmark.kt` / `PaperWarmupBenchmark.kt` | Six iterations of eight turns; capture/draw/upload trace collectors | 48 turns across runs do not satisfy 100 consecutive turns or long-session recovery |

## Actionable tasks

### GF-IN-001 — static drag lifetime and completion lock

- ID: GF-IN-001
- Severity: P0
- Subsystem: Reader reliability / input ownership
- Affected files: `StaticPagedNavigationInputListener.kt`; new `app/src/test/java/com/veilreader/app/ui/screens/StaticPagedNavigationLifecycleTest.kt`
- Observed behavior: baseline owner scope could be cancelled yet listener synchronously called navigator before launching its observer. A callback, observation or restoration exception bypassed the sole `navigationJob=null`, leaving every subsequent drag swallowed by a stale job reference.
- Expected behavior: inactive owners cannot request navigation; every worker exit releases its own lock; successful destinations remain committed; inline completion permits subsequent turns.
- Root cause: no `scope.isActive` check, synchronous mutation outside the worker, success-only cleanup and assignment-after-inline-completion hazard.
- Implementation plan: LAZY worker installed before start; active/mode guards before mutation; identity-guarded `finally`; finished-before-body cleanup. Preserve `awaitReaderVisualNavigationDeparture` and its protected 1,500ms timeout.
- Reuse candidate: existing Paper/Slide lifecycle guards and LAZY operation discipline; existing navigation settlement helper. No new navigation engine or dependency.
- Regression risks: request now executes in its owned coroutine; mode changes before scheduled execution suppress the old request. Timing/inline dispatch and successful commit require tests.
- Tests required: cancelled owner, cancellation before worker, commit callback error retaining destination, observation read error, restoration error after 1,500ms, mode change before worker, twenty inline turns each LTR/RTL. Existing static threshold/cancel tests remain relevant.
- Performance implications: one coroutine already existed; no bitmap/GPU allocation added. No measured latency claim.
- UX implications: recovery after exceptional callbacks and prevention of obsolete owner navigation. No visual redesign.
- Acceptance criteria: compiled tests pass, current CI/durability gates pass, static drag/manual cancel and Paper unavailable static fallback verified on device.
- Dependencies: Wave 0 live-source attribution; current navigator settlement contract.
- Status: YELLOW — source and seven tests implemented in this slice; local whitespace check passed; Android execution/device/UX still pending at authoring.

### GF-PAP-002 — truthful unavailable Paper capability

- ID: GF-PAP-002
- Severity: P0
- Subsystem: Paper availability / UX
- Affected files: `feature/VeilFeatureGates.kt`, `ReaderAppearancePolicy.kt`, `ReaderScreen.kt`, localized strings, `MaterialPageRolloutContractTest.kt`
- Observed behavior: release gate false → a saved requested Paper style maps to static paged/none. Selector is disabled correctly (ReaderScreen 5959–6038), but existing failure notice only runs when presented mode is Paper (1120–1188); it cannot explain this rollout-disabled mapping on Reader entry.
- Expected behavior: unavailable capability is explained explicitly; actual mode is truthful; preferences retained. Do not enable an unverified release engine.
- Root cause: capability remap and renderer-failure presentation are separate, without an entry notice for the remap.
- Implementation plan: pure capability reason model distinguishes release restriction, fixed-layout and renderer failure; localized compact explanation and optional user-owned mode change via existing callback. Avoid duplicate notices and preserve current preference precedence.
- Reuse candidate: existing typed Paper failure notice and Snackbar host; existing appearance capability policy and selectors.
- Regression risks: notice spam, confusing reduced-motion notices, stale session actions, preference overwrite.
- Tests required: persisted Paper+disabled rollout, fixed-layout, reduced motion, repeated recomposition, new session, explicit Slide/Paged selection; Compose action/cancel/focus UI tests.
- Performance implications: no capture/GL work; no new benchmark needed unless presentation affects startup.
- UX implications: makes release restriction understandable without presenting Slide or static paging as a working curl.
- Acceptance criteria: reason reachable in release build, actual selected mode truthful, action uses latest state, TalkBack/200% text/device verified; gate stays false until GF-PAP-003 evidence.
- Dependencies: current release gate and appearance handoff characterization.
- Status: RED — source-verified unavailability/explanation gap; not implemented in this slice.

### GF-PAP-003 — physical Paper promotion evidence

- ID: GF-PAP-003
- Severity: P0
- Subsystem: Rendering / performance / release
- Affected files: `GpuMaterialPageCurlView.kt`, `MaterialPageEngine.kt`, `PaperCurlInputListener.kt`, `benchmark/src/main/java/com/veilreader/benchmark/ReaderFrameBenchmark.kt`, `PaperWarmupBenchmark.kt`, `performance/budgets.json`, `.github/workflows/performance-benchmark.yml`, `feature/VeilFeatureGates.kt`
- Observed behavior: real source pipeline and traces exist, but release gate is false. Physical verification record explicitly says no hardware endpoint, optical test, acquired-buffer driver test or refresh-rate evidence. Current page-turn benchmark runs eight turns per iteration, not 100 consecutive turns.
- Expected behavior: measured, visually verified Paper passes on real hardware before promotion; tests cannot pass by using static fallback.
- Root cause: missing product acceptance evidence, not evidence that current shader is incorrect.
- Implementation plan: extend reuse of current benchmark harness for 100 uninterrupted/alternating turns; assert READY, active Paper and observed capture/upload/draw/presentation work; collect rotation, app pause, process recovery, low-memory and long-session traces plus optical recordings at 60/90/120Hz where available.
- Reuse candidate: existing Macrobenchmark, JankStats, trace counters, generation/epoch fences and benchmark fixture EPUB.
- Regression risks: benchmark may accidentally measure fallback or short traces; CI emulator results may be mistaken for physical budgets.
- Tests required: user gesture matrix (slow/fast/short cancel/reverse/edge/center/RTL/first-turn), rendering, memory/GC, repeated turns, orientation/mode/pause failure transitions; golden/reference optical review.
- Performance implications: record P50/P95/worst first frame, capture/upload/init/completion, memory/GC/dropped frames. Existing frame budgets: physical CPU P95 16.7ms, P99 24ms; overrun P95 0ms/P99 8ms. Capture/upload trace limits await physical calibration.
- UX implications: realistic front/back light, shadow, resistance, cancel and no destination flash require human/device observation.
- Acceptance criteria: exact APK/head, physical device/driver/display identified, measurements and recordings reviewed, accessibility/RTL passes, release-enabled only afterward. Never call compile/unit CI product GREEN.
- Dependencies: GF-IN-001 verification and current upstream CI; hardware runner availability.
- Status: BLOCKED for physical acceptance until device evidence available; source integration YELLOW, release disabled.

### GF-PAP-004 — capture/residency baseline before surface redesign

- ID: GF-PAP-004
- Severity: P0
- Subsystem: Paper snapshots / storage residency
- Affected files: `MaterialPageSnapshot.kt`, `MaterialPageEngine.kt`, `GpuMaterialPageCurlView.kt`, `GpuPageCurlModel.kt`, benchmark/tests above
- Observed behavior: warm WebView visual-state wait, 3s revision-aware captures, two reusable CPU buffers and GPU leases already exist. Immediate source is software View.draw; no measured real-book bottleneck or native memory residency supplied.
- Expected behavior: capture cost and memory are bounded and measured across real EPUBs, resize and low-memory recovery; optimize only proven bottlenecks.
- Root cause: missing measurements. No proven leak or evidence justifying replacing snapshot architecture.
- Implementation plan: measure existing warm-hit/miss, capture and uploads on representative reflowable EPUBs; correlate bitmap pool/resident texture bytes and GC during 100 turns/rotation; only then A/B a hardware/persistent-surface provider behind current seam.
- Reuse candidate: current immediate/prepared provider interfaces, exact revisions/viewport/age tests, GPU leases, trace logging and storage estimate.
- Regression risks: WebView capture fidelity, old revision pixels, unsafe recycling while GL reads, retained surfaces, fixed-layout leaf ownership.
- Tests required: same/different viewport pool reuse; stale revision/age; no pending-lease reuse; cancelled preparation; low-memory/resize/device optical comparisons.
- Performance implications: full viewport ARGB_8888 × two CPU buffers + texture is baseline; measured residency and max/percentile capture costs needed. Do not present formula as measured memory.
- UX implications: no blank/incorrect source image, first-turn hitch or stretched rotation pixels.
- Acceptance criteria: actual before/after traces from same fixture/device; equal visual fidelity; no navigation semantic change; rollback remains provider switch.
- Dependencies: GF-PAP-003 hardware capture; representative lawful EPUB fixtures.
- Status: YELLOW — capture reuse implemented; practical physical budgets and any bottleneck diagnosis open.

### GF-PAP-005 — fixed-layout physical leaf contract

- ID: GF-PAP-005
- Severity: P1 architecture preparation; current availability truth is P0
- Subsystem: Fixed-layout Paper/Slide
- Affected files: `ReaderAppearancePolicy.kt`, `ReaderScreen.kt`, future leaf capture boundary, current snapshot/state/tests
- Observed behavior: policy correctly restricts Paper/Slide for fixed-layout because whole viewport may represent a dual spread.
- Expected behavior: restriction explicit today; future LEFT_LEAF/RIGHT_LEAF and RTL ownership precede support.
- Root cause: current engine captures one viewport as one physical sheet; spread is not a leaf.
- Implementation plan: characterize spread geometry and navigator transitions; define immutable physical leaf identity/source/target, spread anchors, RTL mapping and boundary policy; keep current static path until validated.
- Reuse candidate: current pure capability policy and revision/lease/presentation interfaces; Readium fixed-layout navigator contract after verified upstream research.
- Regression risks: spread skip, wrong binding, double commit, incorrect bookmarks/progress and zoom capture.
- Tests required: single/dual spreads, LTR/RTL, odd last leaf, rotation, scale/zoom, mode switch and preservation of static support.
- Performance implications: potentially two leaf textures; baseline memory/latency required before support is exposed.
- UX implications: explicitly disabled controls; no fake viewport curl offered as a real physical leaf.
- Acceptance criteria: unavailable explanation first; future support passes device/UX/release gates with real fixed-layout fixtures.
- Dependencies: GF-PAP-002 and GF-PAP-003; Readium architecture characterization.
- Status: YELLOW intentional restriction; future physical-leaf support not implemented and must not be exposed.

## Wave boundary

First implementation modifies only the static listener and its new characterization suite. All successful-navigation semantics, static thresholds, reduced-motion policy, Paper/Slide engines, visual presentation barrier and release gates are preserved. Local `git diff --check` passes. Android test results must be attached by the root execution record after CI; no local SDK/runtime was available for executing them. There are no measured performance before/after values in this audit. Device/UX acceptance remains open.
