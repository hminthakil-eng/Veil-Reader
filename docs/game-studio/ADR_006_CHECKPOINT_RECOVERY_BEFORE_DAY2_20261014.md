# ADR-006 — Durable Checkpoint Gate Before Neighborhood Day 2

**2026-10-14 · status: implemented and independently tested on Windows, not mobile certified · standalone Godot 4.7.2 game only.**

## Decision origin — genuine ChatGPT × Gemini debate
The owner asked to proceed with Island Bookshop RPG after draft PR #536, a first playable day with 192 passing automated assertions. ChatGPT proposed a second-day NPC economic mystery with bakery/printing/port contracts. In an actual new consultation in Gemini Desktop (current mode **Flash-Lite**, **NOT** falsely reported as Gemini 4 Argon), Gemini **disagreed on sequencing**. It argued that without a working persistence and Android RTL/touch gate, adding more quests creates lost-progress risk and narrative fatigue. ChatGPT accepted the ordering criticism, but *rejected* Gemini's proposal to create a new `local_research_pool` wallet and duplicate money authority: the game uses the already-proven 30/8/0/50 four-wallet economy; financial model migrations need explicit conservation tests.

## Implemented in this branch
- `island-game/core/day_checkpoint_store.gd`: `IslandDayCheckpointStore`, a separate store for `IslandFirstDayController`'s **combined** day, book inventory and authoritative `IslandEconomyLedger` snapshot. No Reader EPUB/PDF content or personal paths.
- Save to a staging file under `user://island_bookshop_day1`. Flush and close, read back and validate checksum **and** application state, then use a new sequence-numbered checkpoint file name and rename staging into it. Prior committed checkpoints remain unchanged. No preexisting destination file is overwritten.
- Load scans available completed checkpoints, descending by sequence, and chooses the newest with a valid JSON envelope, schema, content digest and semantic game state. Ignores incomplete `.part` files and falls back if newest is damaged.
- Digest calculated from **JSON-canonicalized** snapshot. A real test caught that Godot integer/float differences across JSON serialization caused mismatch in the initial draft; corrected and retested. Plain SHA-256 is accidental-corruption detection, **not anti-tamper authentication**. Even a recomputed hash with forged currency is rejected by the existing 88-coin economic state validator.
- Source folder input restricts path traversal; production UI writes under the game's `user://` home only, while QA uses unique isolated temporary folders and cleans them.
- **Real UI** now has two control actions: `Save progress safely` and `Load latest healthy checkpoint`. `_load_checkpoint` swaps in the fully restored day and calls `_refresh`, restoring choice state and completed-street visual. The UI does not silently auto-reload/overwrite when no valid checkpoint exists.
- Critical old Day1 shop, economy and scene behavior remains untouched, aside from adding manual save/load controls.

## Grounded QA
New `tests/run_checkpoint_tests.gd` passes **45/45 actual Godot assertions**, including:
1. Path traversal and missing checkpoint rejected honestly.
2. First and second on-disk checkpoints created, previous bytes unchanged.
3. Same book legal owner, money and story choice after save/load.
4. Inject interrupted write after temp flush but BEFORE rename; loader ignores leftover .part and retains previous committed checkpoint.
5. Corrupt newest JSON; load automatically recovers an older healthy version.
6. Forge coin supply and recompute checksum; semantic validator rejects, falls back.
7. Persist a *completed* Day 1 after residents co-funded the street lamps; reload retains completed street, escrow=0, total 88, can't pay project twice.
8. Instantiate actual Godot scene, emit SAVE / LOAD button signals and verify UI restores choice and singular financial authority.

Strict `island-game/run_tests.ps1` now verifies:
- Legacy shop & clock **19/19**,
- Economic model **30/30**,
- unified commerce **25/25**,
- Day 1 end-to-end plus real GUI signals **118/118**,
- disk checkpoint/recovery **45/45**.
**TOTAL = 237/237 PASS** under Windows Godot `4.7.2.stable.official.ed1daf0bf`. The runner scans actual stdout and stderr for `SCRIPT ERROR` and requires exact expected PASS summaries. Original and current game scenes start in headless checks.

## Explicit limitations and STOP-SHIP conditions
- This is a **best-effort append-only verified checkpoint/recovery design**. Crash BEFORE rename was simulated. A kernel panic/power failure during rename or directory metadata flush, Android app process-kill under varied filesystems, and end-to-end durability across platform/storage backends are NOT proven. Rename semantics may differ across platforms. Do not label Android crash-safe yet.
- No automatic save on each chapter choice, no data encryption, no cloud sync or multi-device merge, no save list UI/garbage collection. Checkpoints grow until a retention policy is implemented. Save data integrity does not mean cheating resistance.
- Save/load button labels and feedback were localized into Persian using Unicode escapes in GDScript, and passed headless tests. Actual RTL text shaping, visual accessibility and touchscreen/device QA remain pending. No playable Android APK produced in this sprint.
- Separate legacy `ShopLedger` still exists in isolated old scene. The new first-day scene uses only `IslandWorldCommerceSession`'s one money authority; save covers only NEW first-day model.
- NEXT: evaluate on real Android/RTL, add accessible manual resume/autosave/checkpoint pruning and controlled schema migrations, then deepen second-day economic/NPC quests. Never import real personal book content into game records.
