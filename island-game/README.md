# Island Bookshop — Godot 4.7.2 Greybox (prototype)

Separate original indie game **not yet integrated into Veil Reader**.

Engine: **Godot 4.7.2-stable**, Windows standard GDScript. Verified installed on the dev PC at `E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64.exe`.

## Run locally

Open `project.godot` in Godot 4.7.2. Press F6/F5. There are no external plugins or assets. The very rough clickable prototype supports inspecting 6 fiction books with a three-layer clue, serving 2 distinct customers, selling, lending, repairing, and saving/reloading the state.

CLI:
```powershell
$godot = 'E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64.exe'
& $godot --headless --path . --script res://tests/run_headless_tests.gd
& $godot --headless --path . --editor --import --quit
& $godot --headless --path . --quit-after 20
```

## Boundaries

This is a **greybox logic / UI proof**, not a beautiful pixel RPG. There are no art assets, town maps, NPC navigation, romance, quests, anime portraits, sound, Reader bridge, shipped APK, runtime plugin or export templates.

The in-world stock is completely fictional. **Never write user EPUB/PDF titles, paths, URIs, content or locators to these game saves.** Any eventual Reader bridge must be owned and consented to by the native Reader and be separate from the RPG's saved state.

Savings use `user://island_greybox_save.json` plus fallback `.bak`; current prototype is crash-recoverable in some cases, **not a fully transactional or concurrency-safe production persistence layer**. Save tests are single-process and must be extended for interrupt/crash recovery, validation of all fields and atomic inventory transactions.

Primary doc: `../docs/game-studio/ADR-001_JOINT_DESIGN_COUNCIL_20261014.md`.
