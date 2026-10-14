# Island Bookshop Godot Greybox — exact local QA receipt

Date: 2026-10-14
Machine: DESKTOP-GSV91KV
Project: `E:\EyadStudio\Projects\Veil-Reader-Island-Godot-20261014\island-game`
Engine binary: `E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64.exe`
Version stdout: `Godot Engine v4.7.2.stable.official.ed1daf0bf`
Zip SHA256: `731980F9608D61333E5BAF54A2EF17210ACC7A538446C0CB9969F002ACA1E953`.

## Verified commands
- `--headless --path <project> --script res://tests/run_headless_tests.gd`: **19 PASS, 0 FAIL**, exit 0, empty stderr.
- `--headless --path <project> --editor --import --quit`: project load/import PASS, exit 0, empty stderr.
- `--headless --path <project> --quit-after 20`: main scene loads, exit 0, empty stderr.

Core tests include: 6 authored fictional books, 2 customer profiles, wrong recommendation non-destructive, successful sale, exact coins, duplicate transaction blocked, resell blocked, repairing damaged copy and revealing original clue, duplicate repair blocked, loan earns trust, loaned copy cannot be sold, in-memory and on-disk JSON save/restore, invalid snapshot rejected, and replay rejected after restore.
Console logs stored under `E:\Temp\Island_Godot_472_*` on the Windows test machine.

## NOT GREEN
- Real device or APK, physical mouse/touch/UI walkthrough, graphics/lighting, anime portraits, world movement and responsive pixel/HD viewport.
- Genuine quest implementation, NPC offscreen schedules and character arcs.
- Crash-atomicity or full backup recovery. Prototype's file replacement contains a power-loss window between old-file deletion and new-file rename: fix before allowing production game saves.
- Reader app integration, privacy bridge device QA, game balancing and content production.

This PR provides a reproducible **standalone greybox production starting point**, not a shippable game.
