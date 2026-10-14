# Island RPG Core v1 — QA evidence / 2026-10-14

Branch: game/island-core-v1-20261014, stacked on draft design/premium-reader-world-v1-20261014.
Scope: isolated pure Kotlin fictional bookshop logic; no Reader, GameRepository, AvatarAppearanceStore, database or engine code modified; no UI screen/APK delivered.

## Verified desktop receipts
- Device DESKTOP-GSV91KV, JDK17, Android SDK37, offline Gradle, no daemon, max-workers=2.
- IslandShopCore.kt: :app:compileDebugKotlin = BUILD SUCCESSFUL, exit 0 (2m10s).
- Targeted :app:testDebugUnitTest --tests com.veilreader.app.game.island.IslandShopCoreTest = BUILD SUCCESSFUL, exit 0.
- Fresh JUnit XML (app/build/test-results/testDebugUnitTest/TEST-com.veilreader.app.game.island.IslandShopCoreTest.xml): 12 tests, 0 failures, 0 errors, 0 skipped.
- After adding engine-neutral IslandGamePorts.kt: :app:compileDebugKotlin = BUILD SUCCESSFUL, exit 0 (48s).
- Warnings observed: Gradle deprecations and multiple Kotlin daemon sessions; neither fixed yet.

## Proof limitations
- The pure reducer is IN-MEMORY; future Room writer, atomic state+action IDs, process death recovery, backup/export/restore NOT IMPLEMENTED.
- No pixel city, input/pathfinding, animated avatar, NPC schedules or interactive screen yet.
- No real-device Paper, TTS, PDF, RTL or reader-locator QA in this change.
- Two deterministic customers/day are a starter template; prices are NOT balance tested.
- App size/RAM/frametime/battery NOT measured.
- Source-specific Gemini 4 Argon peer-review request submitted via desktop UI; at this note its returned critiques/acceptance are still pending.

## Next acceptance gates
1. Add corruption/concurrency/adversarial command tests.
2. Versioned Room adapter with single-writer exactly-once transaction; process kill and restore tests.
3. Backup/import migration with rollback; exact Reader preservation.
4. Native UI behind default OFF game feature flag.
5. Same-content Compose vs embedded Godot game scene proof; 30 Reader return cycles with true locator/RTL/frame evidence.
6. 5-10 minute player shop-day validation.

Release decision: DRAFT only, never merge into release/main without proof.
