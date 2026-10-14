# ADR-001 — ChatGPT × Gemini 4 Argon independent indie-game council

Date: 2026-10-14 | **Status: JOINT PRINCIPLES AGREED / PARTIAL IMPLEMENTATION** | Main development: `game/island-godot-greybox-v1-20261014`

## Verifiable debate sequence

**Round 1:** ChatGPT submitted the standalone Godot game, 20 reference titles, studio phase order, DI/autoload, save, narrative, NPC, pixel + anime portraits and tools to Gemini desktop 4 Argon. Captured original text on authorized PC: `E:\Temp\Gemini_Island_Council_Round1_20261014.txt` (15,795 characters).

Gemini's strongest objections:
- Visual polish *before* proving the actual shop loop is a production trap.
- Pixel world and crisp high-resolution anime portraits need separate viewport/UI layers.
- Offscreen NPCs must progress logically without loaded nodes.
- Service-driven daily chatter differs from meaningful visual-novel story beats.
- Fiction book decision should depend on cover condition, lore blurb and real in-game physical clues.
- Initially suggested embedding Reader IDs/URIs into fictional book resources — **rejected** for privacy/data-boundary risk.
- Initially recommended Dialogue Manager but loosely cited v4.1 regardless of engine target — **version conflict found**.

**Round 2:** ChatGPT explicitly ACCEPTED loop-first, dual rendering, data schedules, layered book deduction, two-tier dialogue. ChatGPT challenged private Reader references and checked official plugin version release tags. Gemini **ACCEPTED** zero-knowledge reader port with no real book IDs or URIs in game saves, narrowed content scope and removed arbitrary fixed Autoload count. Gemini proposed a more forgiving, phase-gated game clock and recommended Godot 4.7.2.

Captured response: `E:\Temp\Gemini_Island_Council_Round2_20261014.txt` (~10,803 chars). These are real desktop Gemini replies, not prompts misattributed as replies.

**Independent web fact check**: Godot **4.7.2 stable was released 18 August 2026**, verified by official archive, so ChatGPT's earlier Godot 4.6.2 preference was older (though valid for GDQuest open RPG). Godot 4.7.2 pinned for new standalone project. Nathan Hoad Dialogue Manager **v4.1.0 targets Godot 4.7**, while v3.10.4 targets 4.6; plugin preference is **not accepted into source** until a runtime FA/RTL, save/reload and portrait-state compatibility smoke test. v4.x changes can be breaking; preserve version lock.
- https://godotengine.org/download/archive/4.7.2-stable/
- https://github.com/nathanhoad/godot_dialogue_manager/releases
- https://docs.godotengine.org/en/stable/tutorials/rendering/multiple_resolutions.html

## Accepted decisions and gates

| Principle | Joint decision | Acceptance evidence still required |
| --- | --- | --- |
| Game first / Reader integration later | **ACCEPT** standalone Godot | Reader repository stays unchanged; no game URI in save |
| Greybox management before art polish | **ACCEPT** | real book choice/repair/sell/lend and two distinct customers |
| Godot runtime | **ACCEPT** pin 4.7.2 stable standard GDScript | headless import/test; Windows binary hash logged |
| Hybrid pixel/anime visual | **ACCEPT** pixel SubViewport + separately scaled HD portrait CanvasLayer | prototype with device aspect tests and RTL legibility |
| Daily clock | **ACCEPT revised** 4 phases and actions advance time, pauses in menus/VN | no real-time health penalties; deterministic schedule simulation |
| NPC routines | **ACCEPT** separate schedule data from loaded NPC CharacterBody2D | unloaded-scene jump forward tests and conditional quest flags |
| Dialogue | **ACCEPT** compact transaction barks + authored VN portraits for important story | plugin spike; avoid shared mutable world state in plugin |
| Product identity | **ACCEPT** physically inspect books, blurb and restoration reveals clues | choices not reducible to a static blue-tag match |
| Persistence | **ACCEPT** schema-versioned JSON with unique event IDs, fallback and migrations | simulate interrupted write/replay/corrupt file and full recovery |
| Privacy | **ACCEPT** independent fiction catalog, future external opt-in ReaderBridge | games files contain ZERO real EPUB title/URI/text/notes |
| Slice budget | **ACCEPT** 2 scenes, 6 authored books, 2 customers, 1 quest, layered portrait demo | timed user playtest before widening world scope |

## Studio execution, ownership and actual receipts

**Implemented NOW in this branch**:
- Godot 4.7.2 official Windows binary at `E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64.exe`, hashed by SHA256: `731980F9608D61333E5BAF54A2EF17210ACC7A538446C0CB9969F002ACA1E953`.
- Minimal independent Godot project `island-game` with 6 original in-world book definitions, 2 distinct customer needs, one sale/loan vs repair decision, three layers of a book, in-memory typed action and unique event deduplication, isolated `user://` JSON save/restore with backup.
- Godot headless script tests actually executed: **19 PASS, 0 FAIL** on exactly the pinned engine; clean scene import and scene runtime startup exits 0. See QA report.
- No original pixel art, no character sprites, no NPC world walking, no layered portraits, no full quest or Android APK. Runtime UI is an **intentionally plain greybox**, not a polished narrative vertical slice.

**Gemini as peer**: proposed offscreen schedule, Viewport architecture, book deduction and layered dialog, and reviewed the ChatGPT initial plan. **ChatGPT as peer**: challenged scope and privacy, corrected plugin version compatibility and implemented tested core while preserving Reader. No subordination; when we disagree record the rebuttal and proof gate.

## Production risk notes

The initial persistent save is **not actually atomic** on all Windows failure modes: rotating a backup and then deleting the old target before rename leaves a power-loss window. Test/fix with a generation-number or dual-slot journal protocol before any user-facing persistent build. Godot API's `DirAccess.rename_absolute` and `FileAccess.flush` are not enough proof of a transactional commit.
The current dictionary restore validation checks core invariants but needs strict event IDs, stat ranges, book instance canonical immutable fields and migration tests. No race/network sync support is required for this prototype, but a production game needs a durable single-writer authority.
The GUI has not been subject to physical click automation/screenshots or Android emulation. Headless scene load != excellent mobile UX.
