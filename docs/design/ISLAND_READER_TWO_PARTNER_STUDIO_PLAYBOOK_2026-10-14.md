# Eyad Studio — Veil Reader + Island RPG two-partner engineering playbook
**Date:** 2026-10-14 | **Status:** working engineering agreement / not proof of release.
Source branches: `design/premium-reader-world-v1-20261014` -> `game/island-core-v1-20261014` -> `game/island-durable-v1-20261014`.
Master issue #314, shop RPG #519, draft visual/avatar #518. Paper engine #509 **separate**.

## One product, two independent bounded experiences
- **Reader is the primary product.** Readium, PDF, manga, TTS, real EPUB/PDF content, reading history/locator, notes/highlights, navigation and library remain governed by existing owners. The game can be removed and Reader must still work.
- **Island RPG is optional.** A shipwreck survivor, memory loss, warm old two-level bookshop, independent ailing elderly bookbinder, pixel island/town with anime dialogue/character creator. Real book reading grants optional positive cosmetic/progression benefits only, never requires game play.
- **Game loop:** procure fiction book stock -> choose shelf/price or loan -> serve NPC seeker -> invest/restock/repair -> interact with old man -> explore authored coastal world -> original story mystery. User can play this loop using only in-game actions; no entitlement tied to daily real-life chores.
- **Single canonical character:** `RagsToRichesAvatar` / `AvatarAppearanceStore` for cosmetics; portrait and pixel avatar are views, never independent saves. Rank/Path remains sole authority for gated Castle rooms. `GameRepository` owns genuine reading totals, with no second active-reading ledger.

## Operational pairing: human-directed, truthful, test-first
- **ChatGPT — integrator / release gate owner:** official research verification, repository architecture, Kotlin source, database/backup code, performance and test receipts, GitHub branch/PR/issue/rollback. Does not report green if an actual suite did not finish.
- **Gemini 4 Argon — independent technical/creative reviewer:** alternate approaches to shop loop, exact-code adversarial Kotlin critique, anime/pixel content standard, engine-vs-engine debate, story and economy fairness. Submit exact source/diffs when accessible; separate proposed code from compiled patches.
- **Resolution policy:** BOTH voices produce competing ADR positions; facts and measured evidence settle disagreement, not model preference. Source code remains on GitHub as canonical. A session review cannot be called Gemini-approved if Gemini has not returned.
- **No background-autonomous-work claim:** work is performed through explicit live turns/tools. Durable issue checklists and design docs serve as handoff, not an indefinitely running unattended company.

## Knowledge/library: trusted primary sources and research cadence
| Knowledge area | Authoritative starting point | What to learn and prove |
| --- | --- | --- |
| Android offline-first | https://developer.android.com/topic/architecture/data-layer/offline-first | local-first write, StateFlow UI, repo authority, retries only for optional cloud |
| SQLite transactions | https://developer.android.com/reference/androidx/room/RoomDatabaseKt | one serial `withTransaction` around read/reduce/save; no half-sales |
| Schema evolution | https://developer.android.com/training/data-storage/room/migrating-db-versions | exported snapshots, migration paths, schema tests, no destructive fallback |
| Godot Android host | https://docs.godotengine.org/en/stable/tutorials/platform/android/android_library.html | single instance/process, host lifecycle, orientation changes, release before Reader |
| libGDX runtime | https://libgdx.com/wiki/app/the-life-cycle | create/pause/resume/dispose and low-overhead two-dimensional scenes |
| libGDX tile maps | https://libgdx.com/wiki/graphics/2d/tile-maps | tile layers, object maps, efficient sprite batching |
| Tiled maps | https://doc.mapeditor.org/en/stable/ | JSON/TMX map exports, collision and actor spawn location data |
| LDtk JSON | https://ldtk.io/json/ | level/world layouts, entities, auto-layers, schema converter |
| Kotlin/Compose | https://developer.android.com/develop/ui/compose/graphics/draw/overview | pixel-perfect atlas drawing, lifecycle-safe UI, no draw-side effects |
| Game research | Stardew Valley, Moonlighter, Recettear, Fields of Mistria, Tiny Bookshop, Potionomics | systems actually loved/hated; not proof that copies of their mechanics work on mobile |
References must be checked for exact SDK/version when used; do not introduce licenses or cut-and-paste proprietary assets without audit.

## Architectural foundation (real source vs planned modules)
```
app/ Reader ViewModels, Readium & LocalLibraryRepository    [EXISTING, untouched]
  |         GameRepository, Rank/Path/AvatarAppearanceStore [EXISTING]
  |                  read-only projection                     [PORT exists]
  v
game/island/IslandShopCore.kt                              [IMPLEMENTED; pure reducer]
game/island/IslandGamePorts.kt                              [IMPLEMENTED; engine-neutral]
game/island/IslandShopSnapshotCodec.kt                     [NEW v1, format validated]
game/island/RoomIslandShopStatePort.kt                     [NEW v1, isolated Room DB]
  |                                  \
  v                                   v
future game/world/SceneAdapter         future game/content/CatalogPipeline
  Compose 2D / Godot / libGDX           LDtk / pixel atlases / anime portraits
  (not installed)                      bilingual NPC dialogue/JSON (not installed)
```

### Persistence safety rules (must be checked by tests)
1. One shop action has a stable unique ID. All inventory/currency changes and consumed action IDs commit in ONE SQLite transaction; duplicate or invalid command does not write.
2. Store `island_shop_snapshot` in **its own Room database**, never insert new tables into the user's Reader library database just for a draft game.
3. Decoder validates version, inventory keys, uniqueness, book/loan consistency; corrupt or unsupported input fails closed rather than silently minting 30 new coins and wiping data.
4. Export/import is a **staged API**; production `LibraryExport` archive integration + rollback remains a separate P0 requirement. No restore-from-backup claim until test on device and exact archive bytes.
5. No blind `fallbackToDestructiveMigration`; versioned Room JSON schema committed to Git and replay/import tested with old fixtures.
6. Concurrent dispatch from multiple UI pathways must be serial on Room transaction executor; check one-sale-of-one-copy adversarial scenario on emulator.
7. Pixel renderer never has permission to call award/economy writes from draw callback; render immutable projections and explicitly release/pause surfaces before entering Reader.

## Capability roadmap: 4 vertical slices and bounded proof
| Gate | Real usable output | Proof required | Do not claim before |
| --- | --- | --- | --- |
| G0 (current) | Pure 6-book shop reducer + Room writer + action-id dedup | Kotlin compile, 17 reducer tests, JSON codec suite, Android Room race/restore tests, schema export | exact green receipts |
| G1 | 1 day open/close shop, 2 distinct customers, price vs loan, 1 port restock, 1 elder dialogue | actual APK, tap-to-sell + 2nd duplicate attempt, save/kill/relaunch, airplane, RTL, no private file access | device video |
| G2 | shipwreck intro, upstairs bedroom/mirror, 1 book restoration mini-game, 1 street map | all Avatar->sprite palettes sync, 30 World/Reader cycles, exact saved locator, low-memory exit, TalkBack | user/device acceptance |
| G3 | 3 neighborhood exploration, 3 NPC schedules/relationships, original mystery, decorating and companion | documented content pack/asset licenses, bilingual dialogues, sales/quest economy replay, actual thermal/perf | consistent alpha |

**Engine selection isn't a planning vote**:
- Build 1 identical 16/32px tile map, 1 anime+pixel player, 2 NPC agents, 1 sale dialog, camera/collision, return to real EPUB in both Compose Canvas and Godot embedded AAR. libGDX is backup if bridge unacceptable.
- Measure **incremental** APK size, warm/cold start, p50/p95/p99 frame, after-exit app memory, idle energy, TalkBack, RTL and 30 navigation cycles. No fabricated <100ms or <65MB universal claims.
- Native Compose UI (Reader/Home/Archive) must NEVER be coupled to a Godot or libGDX dependency merely because the optional game is installed.

## Risk/Challenge registry (open)
| ID | P | Risk | Gate/mitigation |
| --- | --- | --- | --- |
| R-01 | P0 | Reading gate regression when game gets GPU/CPU | same-book Page/Paper/Slide/Scroll regression and before/after traces |
| R-02 | P0 | Double-sale and negative inventory under concurrent actions | transaction + persistent action ID + parallel tests |
| R-03 | P0 | Restore erases existing game economy / orphan avatar | import migration + archived backup, preserve unknown IDs |
| R-04 | P0 | Real private book leaked as a game item | only in-world catalog, no user file/title passed to RPG by default |
| R-05 | P1 | Genre/price/stock game becomes tedious shop work | optional automation and meaningful seeker NPC choices, usability research |
| R-06 | P1 | Kotlin becomes a bespoke buggy game engine | engine A/B spike before adding multiple maps/pathfinding |
| R-07 | P1 | Avatar high-res/pixel inconsistent | one catalog, palette/slot mapping, screenshot tests |
| R-08 | P1 | Disabled/slow/TTS users unfairly gated | play without reading, accessible controls, no real-time deadlines |
| R-09 | P1 | Visual assets nonredistributable | audit asset-specific license and provenance before committing |
| R-10 | P2 | Content explosion and empty NPC relationships | authored 3 strong NPCs with branching text before 30 weak NPCs |

## Current proof and limitations
- Prior branch `game/island-core-v1-20261014`: **17 targeted pure-domain tests PASS** (reported with XML receipt in `qa/evidence/ISLAND_SHOP_CORE_V1_20261014.md`).
- Current branch `game/island-durable-v1-20261014`: Kotlin main + AndroidTest source **compilation successful** on Windows offline Gradle. Further test receipts are recorded separately; Android AVD currently shows `offline` and is NOT counted as a pass.
- Independent Gemini code-review prompt submitted during this turn; do not call its suggestions approved until a response is verified.
- No actual pixel city, shop UI, emulator installation, Reader real-locator gate or production backup integration implemented in this branch.
