# STATION-3D Gate Plan — Review before execution
Date: 2026-10-17
Status: **PLANNED → experimental implementation ONLY**
Parent: `design/victorian-station-scene-architecture-20261017` at `47cfef067e900bda99f758f3ff86ec36b97a30d7`
Work branch: `experiment/station3d-filament-gate-20261017`

## Design intent & acceptance target

A physical **Victorian railway station** suspended in gray fog: iron and black enamel, oxidized brass, mechanical hanging sign reading `VEIL READER`, an accurate-but-decorative railway clock, two lamplights, vaulted tracks/arches. Distinct from a generic castle UI. A visitor should immediately see a station sign, recognize the current book and be able to open it without waiting for GPU warmup. The app should feel like a responsive premium product, not a playable desktop game crammed into a phone.

Three separate planes (do not merge them):
1. **GPU environment**: actual original 3D station geometry (PBR, local source assets), camera and restrained input-reactive mechanics.
2. **Accessible interaction plane**: native Compose labels, semantic buttons, search, real book cover, recent list; independent of renderer and talkback-readable.
3. **Reader sanctuary**: existing Readium epub/pdf/curl and persistence, never redrawn in a 3D texture, no input capture by station GPU.

### Composition / first-frame storyboard
A. Launch: quiet dark shell and a usable `Open Library`/back action on first UI frame. GPU starts opportunistically; no blocking data/auth, no fog animation on loading.
B. After asset: camera sees a high-enamel iron board with brass rim suspended between two chain columns; clock/lamp motifs. Native 2D title `Veil Reader` remains sharp & accessible on top of the world.
C. Interaction: tap an explicit Compose button to rotate the physical model 15° then back, and toggle an authored spotlight; play ONCE, no infinite idle loop. Back closes lab and releases renderer resources.
D. Actual product integration occurs **only after** full device approval, never by accidentally making the Debug lab a substitute for `RebornThresholdScreen`.

### Quantitative proposed budgets (NOT measured)
- 320dp layout, RTL with 200% font and high contrast has zero clipped functional controls, at least 48dp target.
- 60Hz-capable phone: aim p95 UI station frame <16.7ms; investigate jank on launch/scroll/rotate, report actual measurement and device before claiming.
- Single visible scene; original low-poly asset under 100 KB uncompressed for first experiment; no remote downloads or copyrighted/fan imagery.
- Debug-only SceneView dependency; release classpath/manifest must not contain a new 3D Activity or Filament dependency. Check variant dependencies and APK deltas, don't infer.
- Static composition when `LocalVeilReducedMotion`, high contrast, thermal pressure, power saver or `ReaderScreen` owner. 3D lab itself remains opt-in.
- No new permissions, sensors, network access or storage of user private data.

## Technology choice (research and risks)
| Option | Benefit | Risk/decision |
|---|---|---|
| SceneView 4.56.0 (Maven Central indexed Oct 14) | Compose nodes on Filament, GLB/glTF PBR, a testable separate entry | **Experiment only**: verify Gradle 9.6/AGP 9.4/Kotlin 2.3.21/Compose 1.12.1 & ABI. Pin; do not silently upgrade project |
| Direct Filament 1.77.x | Maximum lifecycle/control, PBR | Larger bespoke glue and high swapchain/memory pitfalls; deferred |
| Current ComposeDepth | Tested 2.5D fallback, immediate readable UI | NOT real 3D. Keep as default in actual app |

Reference APIs: https://github.com/sceneview/sceneview, https://sceneview.github.io/docs/quickstart/, https://github.com/google/filament.
SceneView official README advertises 4.57.0; we pin **4.56.0 for this isolated experiment**, a dated Maven Central version. Any compatibility mismatch = stop/no promotion.

## Step order, with abort criteria

### Gate 0 — source audit and exact spec
- [x] Identify current parent head, app `REBORN_THRESHOLD` + `VICTORIAN_STATION` flags, existing source `StationScenePolicy`, Kotlin/Compose/AGP versions, release manifest, source policy and debug QA tests.
- [x] Write this plan before editing runtime code.
- [ ] Verify generation/license of 3D model is original and deterministic.

### Gate 1 — dependency resolution (fail fast)
- [ ] Add `debugImplementation("io.github.sceneview:sceneview:4.56.0")` only; inspect `releaseRuntimeClasspath` and `debugRuntimeClasspath` via Gradle. **Fail** if release contains SceneView/Filament or a release manifest exports the lab.
- [ ] Ensure no ARCore requirement and no new permissions. Build `:app:compileDebugKotlin` and `:app:compileReleaseKotlin` on CI where possible.

### Gate 2 — smallest real 3D vertical slice
- [ ] Author original low-poly glTF 2.0 for sign/clock/lamps with explicit PBR material colors, embedded buffer, geometry validation; no external texture/asset.
- [ ] A `src/debug` **3D Lab activity** so debug-only features do not leak into production. Real Filament scene, camera and input-controlled model rotation.
- [ ] Overlay unwarped native Compose title/instructions, accessible controls; keep text & navigation out of glTF and 3D viewport.
- [ ] Controlled interaction state only. No rotating forever, no refresh-thread tick for a static scene. Back activity exit via Android normal host.
- [ ] UI empty/loading/error fallback: Compose 2.5D or simple static words; no blank/crash.

### Gate 3 — review (human/device required)
- [ ] CI builds debug Forge QA APK; run dependency/manifest audit, Kotlin/tests and asset verifier.
- [ ] Collect exact APK size, dependency diff and artifact SHA. No guessed performance metrics.
- [ ] Capture real Pixel/other Android runtime image and 8-10 second interaction video; test min API 26, RTL 200%, TalkBack, orientation/window resizing, pause/resume, memory trim, no freezes.
- [ ] Device macrobenchmark/jank frame trace with 3 quality tiers. No green until device results are measured.
- [ ] Visual review by user against Victorian railway station approved concept: should read as station, not empty geometry or fake screenshot.

### Exit conditions
- **No merge to main; no release**, as long as any gate is missing. If CI resolves but hardware not tested, status `CODE GREEN / RUNTIME UNVERIFIED / VISUAL RED`.
- If SceneView API/resolution fails, repair **only** in experimental branch; don't add dependency to production.
- Source of truth remains `Book`/Readium/Room, not 3D nodes; downgrade/revert by deleting lab-only activity and debug dependency.

## Reviewer deliverables
1. This plan and a concise `STATION_3D_EXPERIMENT_EVIDENCE.md` with result vs target (no fabricated tests).
2. Original 3D source glTF plus provenance and geometry test.
3. Debug lab Compose source and tests, dependency isolation evidence.
4. GitHub Actions workflow logs and an APK if gate 2 passes.
5. An independent review before enabling the scene within the main home experience.
