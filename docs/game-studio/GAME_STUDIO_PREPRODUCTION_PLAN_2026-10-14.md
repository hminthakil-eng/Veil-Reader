# Island Bookshop RPG — Studio Preproduction Charter
Date: 2026-10-14 | Status: PROPOSAL / RESEARCH | Owner: two-model design council + human product owner

## Decision record
- Game first, reader integration later. **Godot 4.x chosen as game engine** for separate desktop/mobile project, but pin exact minor version only after actual install and plug-in compatibility verification.
- Fantasy: castaway with partial memory sheltered by an elderly bookbinder above a coastal used-book shop. A lively pixel-world with detailed *anime dialogue portraits*, subtle portrait emotion animation, character customization and narrative investigations.
- A fully playable indie RPG with meaningful shopkeeping, small authored quests, interesting NPC routines and secrets; not a passive reward screen, nor a forced reading gate.
- Reuse the prior isolated shop-core domain work (#520) as **reference/specification**, not blindly transplant Kotlin into GDScript. Do not merge game and reader until stand-alone vertical slice, save QA and mobile metrics pass.
- No unlicensed asset extraction or cracked binary acquisition. Study games via lawful copies, demos, public footage, developer presentations, manuals, reviews, and licensed code. Reverse-engineer visible *behaviors* clean-room with reproducible observations.

## Production philosophy
Research -> Spec -> Prototype -> Measure -> Review with both models -> Accept/Reject -> Implement -> Test -> User playtest -> Ship. Do not code full feature until user loop and acceptance test exist. Preserve provenance and license files. Every assertion needs link/evidence and confidence level. Red risks block "ready" labels.

## Game pillars (priority order)
1. Shopkeeping craft: acquire/order/stock/shelf/price/sell/loan/repair, customer request and feedback, day-end balance, meaningful upgrade.
2. Living island: walkable zones, town map, coherent NPC time routines, relationship memories and neighborhood secrets.
3. Anime narrative in pixel world: beautiful portrait art (blink/breath/emotion, NOT heavy fully animated cinematics), optional branching dialogue, long-term character arcs, authored short quests with consequences.
4. Exploration and identity: beach shipwreck, first item found, elder mentor's personal agency, mysteries learned through books, archives and citizens.
5. Reader bridge LATER: reading is optional; original locator, library and privacy must never be degraded.

## Engine / content architecture (PROPOSAL)
- Godot: separate `island-game/` project; scenes `World/Shop/Dialogue/Menu`, tile-based pixel-perfect rendering, camera, NPC navigation, input map, save slots.
- Data-first: authored resources or JSON with versioned IDs for NPCs, dialogue, missions, maps, items, assets, locales. Separate systems from presentation, events to connect; no giant singletons.
- Core systems: GameClock, WorldState, Inventory, ShopEconomy, NPCSchedule, Reputation, QuestJournal, Dialogue, Interactions, SaveGame, Audio and Accessibility.
- Quest states: UNDISCOVERED / OFFERED / ACTIVE / RESOLVED / FAILED_OR_EXPIRED; validation guards against missing rewards, dead ends, missing localizations and unreachable triggers.
- Save strategy: atomic write + backup, version migration, unique transaction IDs, deterministic tests for reload, duplicate actions and crash recovery.
- Art spec: pixel world at fixed base internal resolution; portrait sprites or layered 2D rigs with emotion states, consistent tone and animation durations; never apply pixel filter to portrait render target.

## Work stages and acceptance gates
### Gate 0 — Research and learning (CURRENT)
- 20 reference titles classified by demonstrated strengths; highest priority top 6 receive scene-by-scene annotated observations.
- Audit 8-12 permissively licensed open-source projects/tools, license+engine-version compatibility.
- Both models separately score architecture & gameplay assumptions. Publish disagreements and agreed tests.
- Deliver Game Design Document, art bible, content schema, risk register, executable proof plan. Gate passes when product owner accepts scope and resource budget.

### Gate 1 — Godot toolchain
- Install pinned official stable Godot; verify binary signature/download source; project creation; editor startup, headless import, export templates, Android SDK link, project clean import.
- Establish Git LFS policies for art and audio, formatter/linter, GDUnit4/GUT (select after compatibility verification), headless unit runner, CI, lint/asset provenance gate, device QA.
- Acceptance: reproducible empty project import and CI on Windows runner.

### Gate 2 — Visual experience spike
- One walkable beach + bookshop room; coherent pixel tiles, collisions, camera, 4-direction player, 2 NPCs, 1 interactive shelf.
- Anime-style dialogue overlay with 3 emotions; compare Dialogic 2 versus a minimal typed custom controller due to the plugin's alpha risk.
- Acceptance: 10-minute smooth playtest at mobile resolution, no obvious scale/animation seams, input responsive.

### Gate 3 — Shopkeeping vertical slice
- One full in-game day; 6 books, 2 customers with different preferences, pricing and transaction, repair, port delivery, profit, calendar and save/load.
- Acceptance: no duplicate money, reload preserves all state, bankruptcy/softlock handling, real choices with feedback.

### Gate 4 — Story and quest vertical slice
- Shipwreck opening, mirror customization, old bookshop, one elder arc, 3 side quests with alternate solutions, mystery clue, schedule-dependent NPC encounter.
- Acceptance: each quest completable from fresh save; branch tracked; dialogue portrait feel better than placeholder; bilingual RTL test.

### Gate 5 — Content production pipeline
- Quest editor workflow, asset import guide, localization, authored content expansion, quest review and playtest; no procedural filler that obscures authored arcs.
- Acceptance: sustained content throughput and no duplicate quest patterns; 60 fps target to be verified per supported device.

### Gate 6 — Reader integration later
- Adapter only after independent Godot build and stable reader regression; return to exact book locator, controlled resource lifecycle, backup/restore and crash boundaries.

## Small team roles
- ChatGPT: systems architect, integration/QA owner, citations/research, acceptance and release gate.
- Gemini: independent design partner, alternate architecture and edge-case challenger, hard coding spikes on isolated branches, critique (not a rubber stamp).
- Both may propose/refuse; disagreement recorded with evidence and a short test, not settled by seniority.
- Product owner: vision, priorities, aesthetic approvals, tradeoffs.
- No model-review claimed unless an actual model response is captured and attributed.

## Required deliverables
`GAME_DESIGN_BIBLE.md`, `RESEARCH_20_GAMES.md`, `OPEN_SOURCE_LICENSE_AUDIT.md`, `ART_DIRECTION.md`, `NPC_QUEST_SCHEMA.md`, `RISKS_AND_GATES.md`, `GEMINI_PEER_REVIEW.md`, `docs/qa/PLAYTEST_SCRIPT.md`.
Each task tracked in GitHub Issues/PRs with working branch, reproduction steps, tests, screenshots and measured results.

## Tool research starting points
- Godot docs: https://docs.godotengine.org/en/stable/tutorials/best_practices/
- Godot GUI & scenes: https://docs.godotengine.org/en/stable/getting_started/
- Dialogic 2 (alpha): https://docs.dialogic.pro/
- GDQuest Open RPG reference (MIT, check exact revision): https://github.com/gdquest-demos/godot-open-rpg
- Agent skills catalog (audit before installing): https://github.com/gamedev-skills/awesome-gamedev-agent-skills
- Game critic/source matrix: see RESEARCH_20_GAMES.md

## No false progress
The existing Veil reader, game prototypes and domain branches are not a validated Godot mobile game. Godot install, Gemini peer decision, game visuals, reverse-engineering observations and playtests remain OPEN until demonstrated.
