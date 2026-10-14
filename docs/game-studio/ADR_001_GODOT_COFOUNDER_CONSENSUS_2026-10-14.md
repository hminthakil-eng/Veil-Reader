# ADR-001 — Joint ChatGPT / Gemini 4 Argon Game Studio Decision

**Date:** 2026-10-14
**State:** Joint design agreement reached in **two actual live Gemini desktop debate rounds**. Prototype partially implemented on reversible branch. NOT approved for shipping or for merging to main.

## Evidence of peer deliberation
- Independent Gemini first answer saved on the authorized Windows PC at `E:\EyadStudio\Temp\Gemini_Godot_Studio_Council_Round1_20261014.txt`.
- ChatGPT read and challenged ten specific points. Gemini's explicit **ACCEPT / COUNTERPROPOSE / TEST** response is saved as `E:\EyadStudio\Temp\Gemini_Godot_Studio_Council_Round2_20261014.txt`.
- Gemini was consulted as an equal peer, not given a task to approve. Both rounds were actually submitted and the second answer completed; distinct hypotheses and unresolved release gates remain documented here.
- Verification of official Godot 4.7.2 stable release: https://github.com/godotengine/godot/releases/tag/4.7.2-stable . Godot best practices: https://github.com/godotengine/godot-docs/blob/master/tutorials/best_practices/scene_organization.rst . Dialogue Manager 4 compatibility: https://github.com/nathanhoad/godot_dialogue_manager .

## Twelve jointly accepted technical/product decisions
1. **Game first:** shipwreck survivor and ailing but independent elder bookseller, fully independent Godot game; no Android Reader integration during preproduction.
2. **Core player fun before art:** prove a real **greybox** bookshop loop (inspect fiction book → restore → infer customer's needs → sell/loan → see consequence), then invest in production-grade pixel environments.
3. **Godot 4.7.2-stable x64 GDScript pinned** for initial Windows development. No .NET or prerelease engine, no blind addon installs.
4. **Original three-layer books:** cover/condition + small authored lore summary + hidden marginalia/clue only revealed after restoration. Six books, not 25, for first slice. All fictional.
5. **HD anime/pixel separation:** world rendered in nearest-neighbor pixel SubViewport; anime portrait/text as full-resolution CanvasLayer. Camera safe zone and dynamic overscan is Gemini's accepted extension proposal, but **still requires aspect ratio/RTL evidence**.
6. **Small authored scope:** first vertical slice targets 2 rooms, 6 fictional books, 2 meaningful customers, one consequential quest, one layered anime portrait and one lightweight second portrait. No empty giant town.
7. **Virtual-time schedule:** pause when reading dialogue, interacting, repairing or in menus. NPC whereabouts are a deterministic projection of virtual day/phase, independent of loaded physical NPC scenes. No stressful real-world elder health timer.
8. **Two-tier dialogue:** fast personality-dependent customer barks; authored portrait-VN cut-ins for consequential story and relationship beats.
9. **Dialogue Manager is preferred, NOT installed/approved:** one small 45-minute import/RTL/conditional branch/portrait/restore smoke test must pass on Godot 4.7.2. The Dialogic 2 alternative has known alpha maturity cautions.
10. **Durable game economy goal:** single writer, schema-versioned JSON, idempotent transaction IDs, atomic temp/backup/replace and explicit migration tests. *This first prototype implements in-memory ledger and JSON snapshot parsing only; disk-atomicity is NOT implemented.*
11. **Reader privacy boundary:** no private EPUB URI/title/book ID in fictional `BookDefinition` or game save. Future optional `ReaderBridgePort` and private mappings belong to Reader and require explicit consent. This was a genuine disagreement in round one resolved by Gemini accepting ChatGPT's proposal in round two.
12. **Lean scenes + test gates:** prefer local scene ownership and few truly shared services; test deterministic money, save round-trip and offscreen NPC schedules headlessly; real on-device pixel/RTL/quest/mobile QA required later.

## Deliberations and still-untested hypotheses
- Gemini challenged the old order, correctly pointing out that high-end art before testing the shop mechanic is a scope trap. **Accepted: greybox first**.
- Gemini proposed fixed 480×270 pixel viewport at 4×; ChatGPT challenged varying mobile aspect ratios. **Compromise:** safe-zone base with adaptive integer-scaled field of view and device QA; no renderer built yet.
- Gemini first favored installing Dialogue Manager 4 as unquestionably ready and ignoring Dialogic; ChatGPT requested an actual compatibility test. **Compromise: preferred only**.
- Gemini proposed reader URI/manifest fields on fictional book objects to ease future integration; ChatGPT rejected privacy coupling. **Gemini explicitly accepted independent future bridge and no private IDs**.
- Gemini requested deeper player deduction and separate Barks/VN authoring. **ChatGPT accepted**.
- Gemini proposed rapid virtual clock; ChatGPT objected to feeling rushed. **Compromise:** action-advanced clock with pause.
- Neither model may unilaterally merge; acceptance needs reproducible source, tests, evidence, mobile quality and user's input.

## Implemented in this branch (small truthful proof)
`island-game/` contains a Godot 4.7.2 project, authored fictional book 3-layer data, idempotent in-memory repair/sale ledger, action-advanced clock, data-only NPC locations, text/button greybox scene and 19 passing local headless assertions. It does not yet persist disk state, integrate a narrative addon, implement pixel art or Android export.

## Immediate next acceptance gates
- **G1:** external release checksums/signature provenance, same-version reproducible import in CI, scene launch and headless tests on clean clone; source artifact and license log.
- **G2:** real shop-day with 6 authored books, meaningful two-customer taste inference and repair; customer consequences, QA against save/reload, shopping exploits, graceful recovery.
- **G3:** prove dual viewport and layered anime portraits with actual licensed/original art; mobile scaling/RTL; evaluate Dialogue Manager condition/save states.
- **G4:** demo with 2 rooms, NPC authored microquest, real transitions and non-repetitive barks; interactive playtest before expanding world.
- **G5:** only later Android embedding/Reader integration, preserving all real-book behavior and device memory.

## Counterfactual / rollback
This is a **standalone, completely separate source subtree**; reversing the feature branch leaves every Veil Reader Android file unchanged. A broken Godot addon, save file or prototype must never prevent opening a user's EPUB/PDF.
