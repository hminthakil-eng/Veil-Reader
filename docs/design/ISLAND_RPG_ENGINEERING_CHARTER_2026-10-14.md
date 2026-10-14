# Veil Reader / Island RPG — Engineering Charter and Proof Gates
Date: 2026-10-14. Scope: reversible engineering infrastructure, NOT public feature/release.
Branch: game/island-core-v1-20261014, stacked on draft design/premium-reader-world-v1-20261014.
Owner's product vision: world-class reader plus optional deep shop-management indie RPG. Story: shipwreck survivor, amnesia, ailing but independent elderly bookbinder, old island bookshop beneath his upstairs home. Anime portrait/customizer + matching pixel-world sprite. Existing game Rank/Path/Castle continuity must survive.

## Operating model: two AI partners / one canonical source of truth
**ChatGPT (integrator/QA owner):** repo architecture, reader protections, merge conflicts, Kotlin integration, test receipts, migration safeguards, performance and GitHub evidence.
**Gemini 4 Argon (independent partner):** alternative game systems and art direction, engine experiments, Kotlin critique, narrative/content structures, source-level red-team. Its advice is reviewed, never applied blindly.
**Real collaboration, not background autonomy:** partners talk in actual tool sessions when available. A draft or prompt is not a returned Gemini review. Neither model can work indefinitely between chat turns without an explicitly scheduled task; GitHub issues and documents are the durable handoff.
**Working loop:** research official sources -> define player loop + P0 gates -> Gemini independent challenge -> one reversible branch -> compile -> focused tests -> backup/restore -> physical screen/video/perf evidence -> review -> merge only with approval. Every issue lists code owner, review partner, scope, evidence, rollback plan. No false green.

## Architecture / bounded contexts
- Core Reader: Readium, EPUB/PDF/TTS/navigation/notes/search/libraries. Owns book IDs, progress/locator and actual content; game NEVER accesses private contents or writes navigation.
- Existing GameRepository: **single** authoritative persisted real reading totals, Rank/Path and Castle unlock authority. Do not create replacement reading ledgers.
- Island Domain: independent fictional stock/catalog, economy, loans, customers, authored NPC and quest flags, virtual days, action IDs. No Date.now driven punishments, no gambling/forced streaks. User can play shop without reading actual books.
- Cosmetics: single RagsToRichesAvatar + AvatarAppearanceStore owner, with anime and pixel-sprite projections via versioned asset catalog; no duplicate character state.
- World scene: optional; renderer reads immutable projected state, does not award credits or persist on every frame. No game runtime in focused Reader.
- Persistence: a future versioned Room repository performs **atomic** transactional writes of changed state and processed action IDs (compare-and-swap or database-owned serial ordering), with schema migration, rollback and library backup integration. In-memory pure reducer **is not durable**.
- Content: authored in-world books, localized story, NPC schedules and tilemaps. No automatic introspection of private user EPUBs, highlights, filenames or content. Stable asset IDs and license provenance.
- Render host: native Compose is immediate 2D prototype and always available fallback. Godot Android Library and libGDX are *benchmarked candidates*; do not decide by opinions alone. Renderer teardown before Reader; low-spec/reduced-motion/static mode.

## First implemented seam (engine-independent)
'app/src/main/java/com/veilreader/app/game/island/IslandShopCore.kt'
- Six stable in-world fictional book templates, four genres and per-copy condition/place.
- Two deterministic virtual-day customers and budget/interest; no reload RNG exploit.
- Immutable domain state and command reducer: shelve, sell, lend, return, repair, port order and virtual next day.
- Duplicate action ID rejected without state changes; unaccepted transactions do not consume IDs.
- No Android/graphics/network/Reader dependencies.
- **Limitations:** currently an isolated domain prototype; it does NOT yet have a durable Room writer, UI, save migration, authored customer dialog or in-game UI. Do not claim shipping.
'app/src/test/java/com/veilreader/app/game/island/IslandShopCoreTest.kt'
- Proposes 12 focused tests for stock, price, customer choices, sale dedup, lending, repair, deliveries, overflow, corrupt schema and non-dependence on reading credits.
- Tests must be confirmed with exact-run output; no green before test XML/Gradle PASS.

## Architecture Decision Record candidates
ADR-101 Reader/game isolation and exact locator on return.
ADR-102 Authority matrix and atomic island transactions (no duplicate Rank/reading or avatar state).
ADR-103 Virtual in-game calendar, non-punishing social schedules, never real-world pay-to-wait.
ADR-104 Pixel city renderer evidence gate: Compose Canvas vs embedded Godot (libGDX fallback) on identical mini-map, 2 NPCs, 1 interactive bookshop scene.
ADR-105 Scene asset schema and dual anime/pixel avatar visual catalog; LDtk JSON map with collision/triggers, bilingual NPC/quest JSON validation.
ADR-106 Saves, migrations and recovery; backups round trip before user adoption.
ADR-107 Content provenance, original story names, licensed audio/sprites, no unaudited commercial sprite sheets in public GitHub.

## Reference documents researched 2026-10-14 (official primary docs)
- Godot stable Android embedding: https://docs.godotengine.org/en/stable/tutorials/platform/android/android_library.html (single Godot instance/process and orientation handling warning).
- Godot tilemaps: https://docs.godotengine.org/en/stable/tutorials/2d/using_tilesets.html
- libGDX tiled map: https://libgdx.com/wiki/graphics/2d/tile-maps
- libGDX lifecycle: https://libgdx.com/wiki/app/the-life-cycle
- LDtk JSON schema: https://ldtk.io/json/
- Android Compose graphics: https://developer.android.com/develop/ui/compose/graphics/draw/overview
- Room database transaction: https://developer.android.com/reference/androidx/room/RoomDatabaseKt
- Indie references and market gaps: docs/design/INDIE_BOOKSHOP_RPG_MANAGEMENT_RESEARCH_2026-10-14.md (Tiny Bookshop, Recettear, Moonlighter, Stardew, Fields of Mistria, Bookshop Simulator).

## Evidence gates, release order
G0 domain: Kotlin compile + 12 unit tests on exact commit; no real book data in Island module; idempotent economic commands.
G1 persistence: Room transactional save & migration, import/export rollback, process-kill tests, deterministic replay and concurrency tests.
G2 playable shop day: pixel floor/counter/loft, 6 books, 2 customers, shelf, sale/loan choice, single port order, elder and clue, save/relaunch.
G3 engine lab: same LDtk map and characters on Compose vs Godot; compare APK delta, p95/p99 framerate, memory after exit, 30 real Reader return cycles, bilingual/RTL and TalkBack, low-end/thermal.
G4 production indie RPG: additional NPC schedules/relationships, town maps, puzzles, optional companion, story branches and polish; source evidence and device footage.
In every gate: reader-only mode must work when game asset pack is absent, the world cannot stall page turn or TTS, and never infer availability from compile-only tests.

## Current exclusions
Do not implement a 3D engine, online economy, combat, fishing/farming padding, player social network, pay-to-win, forced reading timers or procedural AI spoilers during MVP. All new features need separate acceptance/QA proof.
