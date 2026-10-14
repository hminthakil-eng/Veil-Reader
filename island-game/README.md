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
