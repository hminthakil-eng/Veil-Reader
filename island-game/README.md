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
## 2026-10-14 Single-authority bookshop+town P0 spike

**New and deliberately separate live scene:** `res://scenes/unified_day.tscn` runs `core/unified_shop.gd` with one authoritative cash ledger and book-copy inventory, no independent inventory coin field. Buttons test inspect/repair, an expressly **off-island** paid sale, the bookshop and weaver's civic pledges, and the completed street-light commission. The scene is a greybox, not finished game artwork.

Both financial and inventory changes are staged on deep copies and committed only on success. Book transactions, cash movements, proof of civic work and snapshots receive cross-ledger consistency checks, including rejection of basic tampering of even-sum money or unearned city upgrades.

`tests/run_unified_shop_tests.gd` adds 46 headless assertions, with combined runner now totaling **95 PASS** (19 old shop + 30 standalone economy + 46 unified). See `docs/game-studio/ADR_004_SINGLE_MONEY_AUTHORITY_P0_20261014.md` and `qa/evidence/UNIFIED_SHOP_ECONOMY_P0_20261014.md`.

**Do not integrate two financial authorities.** The original `ShopLedger` and its old main scene remain **legacy, isolated** for backwards test coverage. The new unified scene never instantiates it. Production default scene switching, durable disk saves, real local customer money, plot, NPCs, art and mobile testing still remain necessary.