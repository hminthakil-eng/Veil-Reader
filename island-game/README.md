# Island Bookshop — standalone Godot greybox

**Status: reproducible experimental foundation, not a complete playable RPG.**
This is the first code implementation of the two-model October 14 game-studio council ADR. The reader code is not imported, accessed, or modified.

## Toolchain
Official portable Godot **4.7.2-stable, standard GDScript x64**, verified on Windows at:

`E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64_console.exe`

Run from PowerShell in the repository root:

```powershell
& .\island-game\run_tests.ps1
```

Or manually:

```powershell
$godot='E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64_console.exe'
& $godot --headless --editor --path .\island-game --import --quit
& $godot --headless --path .\island-game --script res://tests/run_headless_tests.gd
& $godot --headless --path .\island-game --quit-after 3
```

## Source ownership
- `core/book_definition.gd`: entirely fictional authored game book with cover, lore and a marginalia clue.
- `core/shop_ledger.gd`: prototype single-writer state reducer: repair cost, selling, stock state, replay protection and schema-v1 snapshots. Not a fully durable file-system economy.
- `core/virtual_clock.gd`: deterministic action-driven time, pausable independent NPC location projection.
- `scenes/main.tscn` and `scenes/main.gd`: very small UI *greybox*, buttons for inspecting, repairing and selling a fictional book. Not a pixel world or finished first day.
- `tests/run_headless_tests.gd`: 19 deterministic assertions in initial accepted headless run.

## Red gates / next features
No real pixel tiles, dual viewport, HD portrait rig, customer satisfaction AI, fully implemented NPC schedules, dialogue addon, localization QA, transactional disk writes/backup, Google Play export or mobile profiling here. Future read-only ReaderBridge only after game is independently shipped/tested; private book URI IDs are deliberately excluded from all game objects.

See `docs/game-studio/ADR_001_GODOT_COFOUNDER_CONSENSUS_2026-10-14.md` and `qa/evidence/GODOT_GREYBOX_COUNCIL_20261014.md`.

## 2026-10-14 â€” Island Economic Recovery Simulation-lite (prototype)

Added `core/economy_ledger.gd` as an independent financial proof with four explicit wallets, bounded external buyer budget, local artisan income and a fully escrowed street-light project. `tests/run_economy_tests.gd` tests 30 cash integrity, replay, project and JSON state checks. `scenes/economy_proof.tscn` is a **separate** mouse/keyboard demo with five financial actions and a preview of the restored street state. It is NOT real pixel-art city scenery or the main bookshop scene.

To run: `& $godot --path .\island-game --scene res://scenes/economy_proof.tscn` (from repository root, with `$godot` set to the same Godot 4.7.2 executable as above).

**Strict boundary:** the original `ShopLedger` maintains its own `coins` account. DO NOT add both sets to the live player state. This financial prototype must remain isolated until a single monetary authority, atomic transaction adapter and reload reconciliation are developed/tested. Worldbuilding and staged city recovery documented in `docs/game-studio/ADR_003_ISLAND_ECONOMIC_REVIVAL_20261014.md` and `LIVING_ISLAND_ECONOMY_BIBLE_v0.1.md`.
## 2026-10-14 — additive integrated Bookshop + Island trade, single monetary authority

`core/world_commerce_session.gd` adds an optional **new** combined path. Pass a single `IslandEconomyLedger` instance to it. The new session holds fictional book inventory, ownership and sale/repair event IDs, but contains **no extra coins**. A valid repair transfers funds from `bookshop` to `weaver`; a valid sale transfers from a finite buyer wallet to `bookshop`. Privately owned collateral is not sellable. `snapshot()` and `from_snapshot()` combine book data and the economy in one versioned in-memory payload; disk-atomicity remains pending.

The **old** `ShopLedger` and its main scene are preserved as legacy isolated demos, not wired into the same live player state. Do not treat them as two active player wallets. Migrate only after an explicit backwards-compatible UI/save plan.

`tests/run_unified_commerce_tests.gd` asserts 25 invariants; the strict `run_tests.ps1` runs 19 + 30 + 25 = **74** headless assertions and rejects silent SCRIPT ERROR traces and missing expected test counts. Exact review evidence: `qa/evidence/ISLAND_WORLD_COMMERCE_INTEGRATION_20261014.md`. No finished NPC production, mobile UI/portrait, physical art, Android build or real EPUB reader integration.
## First playable day â€” 2026-10-14

Standalone Godot default scene is `res://scenes/first_day.tscn` on this game branch. Two pixel-placeholder views, six original fictional books, Darya's ethical family-record choices, Bahram's real bounded purchase/refusal, local book repair, and three financial routes to visible street lighting. One integrated world-commerce session owns *all* cash in the new day; the older `main.tscn` remains an isolated legacy scene. No real EPUB data or Reader integration.

The game is a short interactive mechanics pilot, NOT a finished 20-minute narrative, premium pixel city or Android release. Run `island-game/run_tests.ps1` to verify 192 current headless assertions; inspect ADR-005 and the QA evidence file for scope.
## Checkpoint persistence gate (2026-10-14)
Manual Save/Load buttons in the first-day scene persist the COMPLETE fictional bookshop-day state to versioned append-only JSON checkpoints in `user://island_bookshop_day1`. The save store validates canonicalized JSON with SHA-256, re-reads temp before rename-commit, keeps prior versions, and on Load selects the newest semantically valid completed checkpoint. Incomplete `.part` files are ignored. Restores preserve money, legal ownership of family documents, active conversations and paid town works.

The game does NOT silently autosave or auto-resume. Save data is not encrypted or authenticated; a checksum is NOT anti-cheat protection. Windows interruption-injection and corrupt-newest recovery were tested; Android hard process-kill/power-loss guarantees have NOT been tested. See `docs/game-studio/ADR_006_CHECKPOINT_RECOVERY_BEFORE_DAY2_20261014.md`.

The strict `island-game/run_tests.ps1` test suite now expects **237/237** local headless assertions: 19 old shop, 30 economy, 25 shared commerce, 118 player-day and 45 real disk recovery tests.