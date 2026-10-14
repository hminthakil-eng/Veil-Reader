---
name: game-reference-analysis
description: Conduct clean-room competitive analysis and REA-style reverse engineering of legal gameplay footage, official demos or open-source Godot projects; document pixel art, narrative portrait animations, NPCs, quests and shop economy patterns without copying proprietary code or assets.
---
# Game Reference Research and Clean-Room REA

## Input
Game title, official/store developer link, target system to study (camera; shop; NPC routine; quests; pixel art; portrait dialogue; save), available legally obtained gameplay evidence. If no footage or playtest source exists, create a research question; do not fabricate first-hand experience.

## Process
1. Identify the game version/date/source and commercial/open-source license. A proprietary game is never an asset/code donor.
2. For each scene observed, create a timestamped **input -> world state -> UI/audio feedback -> gameplay consequence** log.
3. Map player decision points and NPC causal changes. Record at least 2 surprising cases and 2 frustrating edges.
4. Describe pixel rendering settings by observable qualities only (tile scale, parallax, occlusion, shadow/light dynamics). Do not assert inaccessible source engine, shader code or binary internals.
5. Describe portrait pipeline (head/body layers; expression swap; blink; timing; transitions; history/skip) from visible evidence, not guessed internals.
6. Make a differentiated mechanic for **Island Bookshop** from the design insight, with original names/assets/text/story.
7. Score: engagement, legibility, production cost, replay value, originality and technical risk on 1-5 scales; record rationale and uncertainty.
8. Document a tiny prototype hypothesis and how to test it on the chosen pinned Godot version.
9. Record exact URLs, license, license obligations, screenshot permissions and any accessibility/RTL implications.
10. Stop if the research method needs a cracked executable, DRM bypass, proprietary decrypted resources or unlicensed asset extraction. Switch to official demo, walkthrough, dev talk or permitted source instead.

## Output template
- Source, date/version, legal basis, evidence confidence
- Observed loop with timestamps
- Narrative / characters / microquests
- Economic or progression mechanics
- Art/UI/animation observations
- Risks and complaints
- Transferable game-design *principle*
- Original Island Bookshop experiment
- Reuse/attribution/license check
- OPEN QUESTIONS for Gemini and ChatGPT

## Focus shortlist
Fields of Mistria, Eastward, Moonlighter, Tiny Bookshop, Coffee Talk, DAVE THE DIVER; full list in `docs/game-studio/RESEARCH_20_GAMES.md`.

## Audit gate
Research is not accepted without a citation to the observed source, at least one critique and a measurable prototype recommendation.
