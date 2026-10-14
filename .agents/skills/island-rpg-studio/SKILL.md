---
name: island-rpg-studio
description: Research, plan, develop or review the Godot pixel-world indie RPG with anime narrative portraits, bookstore management, NPCs, quests and testable production gates. Use for all Island Bookshop game design and code reviews.
---
# Island RPG Studio Skill — shared for ChatGPT/Codex, Gemini CLI and Antigravity

## Load first
- `docs/game-studio/GAME_STUDIO_PREPRODUCTION_PLAN_2026-10-14.md`
- `docs/game-studio/RESEARCH_20_GAMES.md`
- `docs/game-studio/GEMINI_PEER_REVIEW_BRIEF.md`

## Working pact
1. Treat ChatGPT and Gemini as independent, equal design reviewers. If reviewing the other model, seek flaws and alternatives, not obedience.
2. Before editing code, write one user loop, one scene diagram, one data contract, at least 3 failure cases, and one measurable acceptance test.
3. Label statements OBSERVED / DOCUMENTED / INFERRED / PROPOSED. Never claim execution, screenshots, benchmarks, reviewer approval or installation without actual receipt.
4. Prefer official Godot best practices, engine-matched plugins, lawfully licensed assets, original game artwork and clean-room observed mechanic descriptions.
5. Defend reader independence. Game is stand-alone first; reading optional. No proprietary ebook content read without user authorization.
6. Keep changes small, reversible, on feature branches; one issue + one PR per subsystem. Build with pinned engine and restore from clean clone.
7. For each PR: link source research, test failure before/fix after, performance and accessibility/RTL checks, version/asset licenses, human test recipe.
8. Escalate ambiguous decisions using an explicit comparison: `Option A | Option B | tradeoffs | experiment | recommendation`.
9. For quests: unique NPC purpose, preconditions, consequence, fail-safe, replay test, translation parity; no generic filler fetch loop.
10. For management: show stock, price feedback, causal customer preferences, day accounting, economy exploit tests and durable save correctness.
11. For cinematic VN dialogue: limited layered animation (blink, breathing, pose/emotion swaps), timed text, skip/auto/history, font legibility, controller/touch and RTL tests.
12. Never download cracked copies, remove DRM, redistribute art/audio, or copy source from proprietary releases. Behavioral observations and licensed open source only.

## Active quality gates
G0 sources + GDD + two-model critique; G1 Godot toolchain; G2 10-minute visual spike; G3 real shop day; G4 meaningful quests; G5 content pipeline; G6 reader integration after game QA.

## Peer-review format
`Claim | Evidence | Challenge | Alternative | Test | Decision | Owner | Next review`.
A decision is JOINT only if *two actual model responses* were compared. Otherwise mark `AWAITING GEMINI` or `AWAITING CHATGPT`.

## Primary guides
https://docs.godotengine.org/en/stable/tutorials/best_practices/
https://github.com/gdquest-demos/godot-open-rpg
https://github.com/nathanhoad/godot_dialogue_manager
https://docs.dialogic.pro/
https://github.com/godot-gdunit-labs/gdUnit4
