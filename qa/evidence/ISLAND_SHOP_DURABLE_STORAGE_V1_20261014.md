# Island RPG Room Storage v1 — QA receipt
Date: 2026-10-14
Branch: `game/island-durable-v1-20261014` stacked on `game/island-core-v1-20261014`
Status: **SOURCE + COMPILE + TARGETED JVM TESTS PASS; DEVICE DURABILITY AND PRODUCTION BACKUP INTEGRATION NOT VERIFIED**.

## Files
- `IslandShopSnapshotCodec.kt`: version 1 explicit JSON encode/decode for fictional shop state, stable action IDs, fail-closed invalid/unknown/corrupt state.
- `RoomIslandShopStatePort.kt`: separate Room database `veil_island_shop_v1.db`, v1 schema export; entire state+action journal is one row. `withTransaction` read/reduce/write, rejected/duplicate commands do not write. Export snapshot and restore-if-empty APIs for later integration.
- `IslandShopCore.kt`: makes snapshot validator internal so persistence decoder can validate loaded data.
- `IslandShopSnapshotCodecTest.kt`: two Robolectric tests for full JSON roundtrip and unknown schema rejection.
- `RoomIslandShopPersistenceInstrumentedTest.kt`: 4 instrumentation cases: parallel sell, fresh DB restore, malformed backup fail-closed, rejected action does not burn ID.
- Room exported schema: `app/schemas/com.veilreader.app.game.island.IslandShopDatabase/1.json`.

## Receipts observed on Windows
Device `DESKTOP-GSV91KV` | JDK 17 | Android SDK 37 | offline Gradle 9.8.1 | max-workers=2.
1. `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`: **BUILD SUCCESSFUL** in 1m54s; exit 0; both Kotlin tasks executed after Room KSP.
2. `:app:testDebugUnitTest --tests com.veilreader.app.game.island.IslandShopCoreTest`: **BUILD SUCCESSFUL** in 1m17s; exit 0 (regression to existing core). Gradle's next filtered unit suite removes this class's XML; separate run receipt log exists at `E:\EyadStudio\Temp\IslandDurableUnit_20261014.log` and previous branch had 17-test XML.
3. `:app:testDebugUnitTest --tests com.veilreader.app.game.island.IslandShopSnapshotCodecTest`: **BUILD SUCCESSFUL** in 57s; exit 0. Fresh `TEST-com.veilreader.app.game.island.IslandShopSnapshotCodecTest.xml` reports **2 tests / 0 failed / 0 errors / 0 skipped**.
4. `git diff --check`: no whitespace errors (Windows line-ending warnings only).
5. `adb devices -l` returned `emulator-5554 offline`. **No device instrumentation suite was run or passed.**
6. No full APK packaging/installation, Reader locator return cycle, process-kill device recovery, user backup archive restore, performance or RTL visual QA completed in this slice.

## Known limits and production gates
- `RoomIslandShopStatePort` is intentionally **not wired into navigation/game UI** and is `internal`; no false player-facing feature claim.
- Isolated game database is **not yet integrated into existing `LibraryExport` full archive**. A user restore of library alone does not guarantee RPG save restoration until transactional archive integration and rollback are tested. Treat this as P0 before shipping.
- This v1 JSON single-row snapshot is deliberately small; appliedIds growth must later be bounded/migrated to a journal table after profiling. Exported schema history must stay in version control.
- `restoreIfEmpty` refuses to overwrite any existing row and validates foreign/unknown IDs. A future recover/merge flow requires explicit policy; do not auto-reset or downgrade progression.
- Official Room `withTransaction` guarantees one transaction on its database executor at a time; this is an architectural guarantee, **not** evidence that our code's concurrency test passed. Device test still required.
- Renderer/engine selection Compose vs Godot still **research decision**, not an implementation. Do not ship speculative memory/frametime claims as benchmarks.
- Gemini 4 Argon source-level consultation was submitted this turn. Do not say a new response was received unless copied/verified.
