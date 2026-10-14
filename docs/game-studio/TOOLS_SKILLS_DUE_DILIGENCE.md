# Tool and skills due diligence — PROVISIONAL (2026-10-14)
Only public source/docs reviewed; installations and live compatibility must be tested before ACCEPT. Do not assume all repositories work together.

| Candidate | Job | Source | Evidence / license | Decision |
|---|---|---|---|---|
| Official Godot engine | Main standalone RPG engine | https://godotengine.org/download/windows/ | Open-source engine; exact stable 4.x version and binary installation still pending | CHOSEN ENGINE, NOT INSTALLED |
| Official Godot best practices | Scene tree, scenes vs scripts, autoload, project structure | https://docs.godotengine.org/en/stable/tutorials/best_practices/ | Official documentation | ACCEPT AS PRIMARY |
| GDQuest Godot Open RPG | Study structured maps, inventory, dialogue, turn-based reference | https://github.com/gdquest-demos/godot-open-rpg | MIT; README currently requests Godot 4.6.2 | STUDY / ENGINE MATCH |
| Dialogue Manager (Nathan Hoad) | Branching dialogue with rich portrait UI adapter | https://github.com/nathanhoad/godot_dialogue_manager | MIT; Dialogue Manager 4 supports Godot 4.6+; store lists unstable release | SPIKE |
| Dialogic 2 | Visual novel timelines, portraits, layouts, choice history | https://docs.dialogic.pro/ | MIT FAQ, documentation explicitly marks Alpha | SPIKE, ALPHA RISK |
| GdUnit4 | Automated GDScript/scene tests | https://github.com/godot-gdunit-labs/gdUnit4 | Published compatibility chart spans multiple Godot 4.x releases | SPIKE/VERIFY PIN |
| QuestSystem | Alternative reusable quest editor and serialization | https://github.com/shomykohai/quest-system | MIT declared, localization and serialization, GDUnit4 tests | SPIKE |
| Game dev agent skills (74) | Candidate Codex/Gemini reusable micro-skills | https://github.com/gamedev-skills/awesome-gamedev-agent-skills | Source declares 74 skills and Godot 4.7 baseline: **must version-match**; audit scripts before execution | SELECTIVE REVIEW, NO BLIND INSTALL |
| Gemini CLI Skills | Model-specific workspace discovery | https://github.com/google-gemini/gemini-cli/blob/main/docs/cli/tutorials/skills-getting-started.md | Workspace `.agents/skills/<name>/SKILL.md` officially recognized (alias), `/skills list` validation | SHARED FORMAT CONFIRMED, LOCAL CLI NOT FOUND |
| Google Antigravity Skills | Model-specific workspace discovery | https://www.antigravity.google/docs/skills | Official `.agents/skills` default | SHARED FORMAT CONFIRMED |
| REA agent package | Research helper, maybe website/game mechanic workflows | E:\Tools\rea-toolchain\package.json | Installed NPM dependency `rea-agents ^6.4.0` seen on Windows; implementation and suitability UNVERIFIED | HOLD TEST |
| Tiled map editor / LDtk | Author tilemaps and markers | https://www.mapeditor.org/ / https://ldtk.io/ | Research licenses, exporter and Godot plugin version before selection | TO AUDIT |
| Aseprite/LibreSprite | Pixel assets and animation frames | https://www.aseprite.org/ / https://libresprite.github.io/ | Compare workflows, license and reproducible source assets; avoid proprietary pack redistribution | TO AUDIT |

## First checks
1. `Get-Command godot` did **not** find Godot on Windows PATH. Search and verified installation pending.
2. Windows `git` and Python 3.11 are available; desktop Gemini app window has been found. `gemini` CLI was **not** available on PATH during check.
3. Common workspace skill documents committed under `.agents/skills` on this planning branch; model-specific discovery in a live Gemini CLI/Antigravity session is NOT YET CONFIRMED.
4. Existing `E:\Tools\rea-toolchain` is an installed npm package, not proof that game reversing works.
5. Do not dump 74 agent skills into production unreviewed. Prioritize 6: Godot typed GDScript, scenes/signals, TileMap, quest/save state, pixel-asset workflow, UI/accessibility. Compare source license/engine version per skill.
6. Pin plugin release SHA, dependency integrity and SPDX license; run smoke test before merging plugin into game folder.

## Optional enhancement research / hard decisions
- Dialogue architecture: Dialogic 2 alpha vs Dialogue Manager 4 vs typed small dialogue DSL; test style, RTL, portrait layers, history/skip/auto, localization, save restore, editor reliability.
- Map editor: LDtk vs Tiled; prioritize stable Godot importer and ID preservation; no two content schemas for same scene.
- Testing: headless GdUnit4 with simple deterministic economy and quest state tests; first confirm engine version matrix.
- UI/world: original sprite/tiles/color script; weigh limited lighting/particles against battery and 60 fps mobile.
- Gamification layer should have no effect on reader content/locator until explicit separate integration milestone.

## Next artifact required
License audit table with revision hash and accept/reject rationale for each imported source asset or dependency, committed alongside the first Godot project (not filled with invented results now).
