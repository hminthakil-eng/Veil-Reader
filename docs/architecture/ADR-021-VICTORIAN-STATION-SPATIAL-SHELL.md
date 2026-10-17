# ADR-021 — Victorian Station Spatial Shell
Date: 2026-10-17
Status: **EXPERIMENT / NO RELEASE**
Branch: `design/victorian-station-scene-architecture-20261017`
Author: Veil Reader Grand Forge

## Goal and non-goals

The owner requested a highly interactive, steampunk / Victorian **railway station** app inspired by the atmosphere of Sefirah Castle, with a large suspended station sign reading *Veil Reader*, atmospheric depth, clocks/lamps/mechanics, real gestures and a quiet first-class reader.

**Goal**: make the spatial station a coherent app *navigation surface*, not a decorative background or a 3D screenshot. Model assets and interactions must correspond to real app actions and persisted user book data.

**Non-goals**: replacing Readium/Room, committing a game engine into every EPUB page, selling generated concept art as implementation, perpetual animation loops, fake reading progress/estimates/quotes/timetables, importing copyrighted Lord of the Mysteries imagery or literal locations into the release. Narrative design may be inspired by themes but assets and labels are original.

## Architecture — source of truth

```text
Books / Highlights / ReadingSessions / Settings (Room, Readium)
             |
       VeilApp / VeilAppViewModel   <-- canonical routes and locators
             |
       RebornThresholdScreen       <-- debug-only opt-in surface
             |
       StationSceneCoordinator      <-- pure policies, no persistence
         /          |           \
 Compose2.5D      Filament3D      Static accessible UI
   fallback      (future plug-in)      fallback
      |              |                 |
        REAL call sites / navigation events
             |
         Library, Reader, Notes, World

ReaderScreen / Readium Navigator (untouched, independent compositor)
```

### 1. Presentation ownership

- `ui/station/StationScenePolicy.kt`: renderer admission and lifecycle rules; pure Kotlin, unit-testable, NO global singletons, no app data storage.
- `ui/station/VictorianStationHeader.kt`: first functional `ComposeDepth` slice, with material plane on press (perspective), decorative vaulted glass roof and hanging iron/brass sign; sign's click calls **real** `onOpenLibrary`, not a fake UI state.
- `RebornThresholdScreen.kt`: existing real Books/Highlights/Callbacks, `REBORN_THRESHOLD` gate plus separate `VICTORIAN_STATION` gate for sign. `gameVisible` remains honored. Empty shelves are empty; progress is only displayed after real opening. All existing Reader modes and PDFs unaffected.
- `app/build.gradle.kts`: flag disabled in release and benchmark; debug opt-in via `-PveilRebornThreshold=true -PveilVictorianStation=true`.

### 2. GPU renderer plug-in decision

Candidates:
- **SceneView 4.57.0** (Apache-2.0) = Compose-facing graph over Google's **Filament**. Fit for compact, life-cycle-aware glTF/GLB hotspots; before adopting, **pin compatibility** with Kotlin 2.3.21, Compose 1.12.1, AGP 9.4, minSdk 26 and app-native libs, check transitive license/NOTICE, APK size, startup, context loss, render bounds, accessibility overlays, PDF/Readium layering, native-library ABI packaging and release shrinking. Version reference: https://github.com/sceneview/sceneview.
- **Filament directly** (Apache-2.0) = smaller conceptual dependency and lower-level control but high engine/lifecycle/input boilerplate. Reference: https://google.github.io/filament/dup/intro.html.
- **Compose graphicsLayer/Canvas** = low-cost 2.5D fallback for signs, brass material, scroll parallax, animated valves/clocks and window panes; does NOT project true 3D models.

Do **NOT** yet add a new Maven dependency. `StationRendererMode.FILAMENT_3D` can be admitted **only** when `filamentInstalled`, `sceneAssetsValidated`, `hardwareGpuApproved` and `featureEnabled` are all true. None of those are enabled by the current UI call site. This explicit absence matters: the architecture is implemented, the complete true 3D scene is not.

### 3. Future rendering runtime interfaces

On a separate experimental code module `station3d` (once Play/API binary and CI tested), define:

```kotlin
interface StationSceneRenderer {
    suspend fun prepare(assetManifest: StationAssetManifest): LoadResult
    fun attach(host: SurfaceHost, events: (StationHitTarget) -> Unit)
    fun apply(state: StationSceneState) // immutable snapshot, never a repository
    fun pause() // ON_STOP, screen hidden, trim-memory
    fun dispose() // deterministic GPU release
}
data class StationHitTarget(val id: String, val action: StationSceneAction)
```

`StationSceneState` is derived only from `Book` + `ReadingSession` + `Settings`; no model or game component owns navigation or a reader locator. Mapping `StationHitTarget` to `VeilAppViewModel` callbacks happens in the app shell, never the renderer. All render assets loaded from app-private, versioned local resources; no online model download required. Prefer texture atlases + glTF2/GLB with physically-based rough metal / frosted glass, small mesh groups and authored LOD.

**Don't add this interface as fake compile-ready Kotlin until choosing exact dependency/lifecycle API and writing integration tests.**

### 4. Real interaction map

| User action | Spatial metaphor | Actual behavior | Accessibility |
| --- | --- | --- | --- |
| Tap station sign | Hanging enamel sign leans on press | Open Library tab | Button semantics and 48dp+ target |
| Tap book on platform | Book on a brass stand tilts | Existing `requestOpenBook(book)` | Described by real title |
| Swipe archive carousel | Slide a shelf cart | Scroll existing books | Standard list swipes and TalkBack |
| Tap notes emblem | Open sealed dossier | Existing passage/highlights | Hide private clue content by default |
| Tap settings mechanism | Rotate a mechanical dial | Existing reader settings / appearance | Alternative ordinary slider/checkbox |
| Enter actual text | Parchment leaves mechanical shell behind | Native Readium EPUB/PDF navigator | No blur/moving background beneath content |
| Back / cancellation | Return through arch | Existing route/ViewModel semantics | Predictable system Back and TalkBack |

Interactive affordances MUST work with screen-reader focus, keyboard/DPAD and tap; immersive 3D rays cannot replace touch targets or semantic names. Decorative fog, train and clocks are non-clickable unless backed by meaningful actions.

### 5. Motion / performance policy

- No perpetual animation in reader; shell entrance animations bounded, caused by explicit interactions, lifecycle-aware and cancelled on screen exit.
- High contrast, reduced motion (system Animator setting), power saver, memory-critical mode or active reader => STATIC.
- `ComposeDepth` input press 160ms; static mode 0ms.
- Filament render loop only when foreground and visibly mounted; pause/release resources on lifecycle and context-loss.
- Target future verified Android evidence: 60Hz capable device **p95 frame duration below 16.7ms during station interaction**, **zero non-UI navigation regressions**, no >48dp target violations; budget is an **acceptance target**, not a measured claim. On 90/120Hz screens respect refresh rate only if native performance verified.
- Device quality tiers with a **single scene** on 6GB RAM/thermal-constrained devices; don't allocate GLB materials per book cover, keep 3D title sign/pillar/lamp counts bounded and use Compose for real text. Never map opaque private book text to a scene texture.
- Ensure text remains flat, selectable, scalable, accessible; perspective only on the decorative material plane and object illustration.

### 6. Art direction & layout

The source concept image is a *moodboard*, not an Android screenshot. Visual tokens:
- Materials: iron black, oxidized brass, smoked glass, slate fog, ivory enamel, warm lamplight, restrained parchment.
- Layout: one bold suspended **VEIL READER** sign, deep station concourse behind it, real current book as first action, meaningful shelving below; avoid a wall of framing on every card.
- Motion grammar: subtle press-swing of sign, book sliding out of shelf, scene-depth on scroll, a bounded door-open for library; **never** costly full-screen fog while reading.
- Adaptive: 320/390dp, tablet, fold; Persian/RTL labels and mixed scripts; 100%/200% font. Vertical growth replaces clipping; no fixed typography height.
- Train and architectural meshes must be original or commercial-safe, licensed with per-asset provenance and baked LOD. Don't put invented canonical Lord of Mysteries artwork/novel cover in release.

### 7. Gates and rollout

| Gate | Meaning | Current |
| --- | --- | --- |
| A | Interface compiled; pure policies and Compose semantics pass | **UNVERIFIED until CI of this branch** |
| B | Screen actually reachable behind debug flags | code connected, build **unverified** |
| C | Visual compares against moodboard on actual Android | **RED / NOT RUN** |
| D | Models/filament real 3D geometry with GLB renderer | **NOT IMPLEMENTED** |
| E | Smoothness on 3 phone GPU tiers, memory and thermal | **NOT RUN** |
| F | Reader navigation, notes privacy, EPUB/PDF modes unaffected | local source unchanged; device **NOT RUN** |
| G | User visual approval | **PENDING** |

Run:
```bash
gradle :app:testDebugUnitTest --tests 'com.veilreader.app.ui.station.*' --tests 'com.veilreader.app.ui.screens.RebornThresholdScreenTest' --stacktrace
gradle :app:assembleDebug -PveilForgeQa=true -PveilRebornThreshold=true -PveilVictorianStation=true
```
To disable instantly, set `-PveilVictorianStation=false` (default), leave release untouched.

### 8. Integration quality / code review
Reject diffs that add model data to domain repositories, install a massive renderer globally, change source of truth of reading progress, intercept Reader touch handlers, regress RTL large text, leak highlights on front door, or claim true 3D because `graphicsLayer` tilts a flat surface.

Next proper engineering increment: prototype one original low-poly GLB (sign + pendulum + 2 lanterns) in SceneView **separate dependency experiment**, test native library ABI/cold start and first-contentful-render and compare against Compose 2.5D captured on device before integrating the 3D viewport.
