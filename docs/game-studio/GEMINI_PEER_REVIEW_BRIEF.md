# Gemini × ChatGPT joint design council: peer-review packet
Status: **AWAITING AN ACTUAL GEMINI REVIEW**. This is an invitation to *critique*, not an instruction to endorse ChatGPT. The answer must be captured separately.

## Shared task
We're designing an original **standalone Godot 4.x indie RPG** set on a coastal island. A shipwrecked amnesiac meets an elderly independent bookbinder, lives above his bookshop, runs the shop, meets distinct NPCs with schedules and authored mini quests, explores secrets. Atmosphere: lush hand-authored **pixel art world**, charming/sombre tone and **sharp anime visual-novel-style character portraits with inexpensive subtle animations**. Later, not now, an optional integration with a separate Android book reader (never force reading).

Game references: Fields of Mistria, Eastward, Moonlighter, Tiny Bookshop, Coffee Talk, DAVE THE DIVER, Stardew Valley, VA-11 Hall-A, Travellers Rest, Chef RPG. Full 20 in `RESEARCH_20_GAMES.md`.

## Specifically request independent Gemini critique
1. Challenge the *game concept itself*: Does it mix management + RPG + narrative coherently? What is original? Where is scope creep or a boring grind?
2. Propose your best **20-minute demo loop**: precise player actions and why each feels satisfying; at least 3 meaningful choices and one unexpected NPC interaction.
3. Architecture: compare scene-tree layering, typed GDScript, data resources, quests, NPC scheduling, dialogue portraits, save serialization and modular plugin use. Show a minimal diagram and alternatives.
4. Compare **Dialogic 2 (alpha)** versus **Dialogue Manager (Godot 4.6+)** and hand-rolled narrative UI. Consider localization/RTL, voice history, expression state, tests and longevity. Don't assume latest plugins support chosen exact engine.
5. Identify 8-12 vetted GitHub codebases or tools with URL, recent version, exact license and *why* they fit. Propose a reject/accept audit. Prefer tests and small dependency surface over sheer popularity.
6. What art pipeline yields Eastward-quality atmospherics plus Fields of Mistria-style expressive portraits with only a tiny team? Provide a detailed art bible template and measurable pixel scaling/contrast/UI constraints.
7. Specify 10 consequential micro quest patterns (not fetch-quests), varied NPC roles, branching flags and state machine invariants.
8. Evaluate persistence crash safety, data versioning, anti-duplicate transactions, localization parity and skill/currency economy abuse. Give adversarial tests.
9. Propose a staged 4-week *learning/preproduction* path and a vertical slice definition. Separate essential from nice-to-have. No assumption of instant production-grade code.
10. Critique ChatGPT's proposal in `GAME_STUDIO_PREPRODUCTION_PLAN_2026-10-14.md` **point-by-point**: mark AGREE / DISAGREE / RISK / BETTER ALTERNATIVE. Please do not flatter or accept by default.

## Council deliverable format
- Highest-risk claims and evidence.
- Proposed divergences/alternative architecture.
- P0 minimum playable slice and definition-of-done.
- GitHub skill/tool shortlist, engine/plugin version compatibility.
- 5 biggest hidden risks.
- A section **Questions for ChatGPT**.
- A concise joint-decision proposal to be debated, *not* declared accepted.

## ChatGPT's preliminary counterposition, explicitly disputable
- Godot 4.x as independent engine chosen by user, exact version only after install/testing.
- Consider Dialogue Manager over Dialogic 2 for shipping reliability (Dialogic docs currently mark alpha), but evaluate features empirically.
- Use GitHub `.agents/skills` for shared prompt procedures. Do not install an unreviewed bundle of 74 skills merely because it has many.
- Require game-side data schemas, not heavy mutable global singletons. Prototype 2 scenes and only scale content after user testing.
- User experience should feel alive through character reactions and branching microquests, not merely more tasks.
- Cracked proprietary game distributions, DRM bypass and asset extraction are unnecessary and out of scope: video, legitimate copies, dev presentations and permissive code suffice.

## Status ledger (do not conflate)
ChatGPT: source research + preliminary docs / shared skill committed to planning branch.
Gemini: last earlier game discussions documented on other branches, but **no new peer answer to this brief confirmed**.
Joint decision: NONE YET; must wait for actual two-model comparison.
