# Eyad Studio — Two-AI Veil Reader + Island RPG Operating Runbook

Date: 2026-10-14 | Version 1 | Status: active, source-controlled instructions; NOT continuous background agent execution

## Mission
Build **two coherent experiences**, not one compromised hybrid:
- **Veil Reader Sanctuary:** excellent offline-first EPUB/PDF/manga/TTS library and reading experience with reliable locators and pure, quiet interface.
- **Island Bookshop RPG:** shipwrecked amnesiac rescued by an independent elderly bookbinder, his old seaside bookshop below his upstairs home, rich fictional shop economy, pixel-art town, anime portrait with matching pixel sprite, relationships and authored mystery.

The game may reward voluntary real reading through a read-only adapter, but users may play game with zero reading that week and read books with game fully disabled. No real book contents or filenames used in NPC gameplay by default.

## Division of labor
| Function | ChatGPT / Integration lead | Gemini 4 Argon / Independent design+engineering |
| --- | --- | --- |
| Architecture | One-writer invariants, bounded contexts, Gradle/Room/QA contracts, release gates | Alternate designs; independent worst-case critique of actual files |
| Game economy | Implement atomic game-only reducer/persistence, replay and backup tests | Balance model, NPC preferences, exploit adversary, game systems |
| UI+art | Integrate Compose/Android, accessibility, RTL, minimal GPU ownership | Pixel/portrait dual representation, critique palette, game scene and animation |
| Story | Validate consistency, localization, optional branch flags, user agency | Characters, prologue, original lore, meaningful player choices |
| Research | Official docs/licensing and current tool verification, capture sources | Parallel research and counterarguments, label access limits |
| QA/release | Real device screenshots, JUnit XML, commit SHAs, PRs, no false PASS | Independent patch review, ask for proof, mark BLOCK where needed |

A Gemini question only counts as a consultation when submitted through an actual running session and a reply is captured. A draft review prompt does not count. Both models only act on current user-directed tool sessions; source-controlled briefs are for future explicit continuation, **not a promise of background development**.

## Safe collaboration protocol (every feature)
1. Pick one GitHub issue; name concrete player outcome and P0 non-negotiable reader constraint.
2. Study two competitor examples, at least one official SDK reference and user complaints when available; separate evidence vs hypotheses.
3. ChatGPT prepares minimal reversible branch and architecture test. No simultaneous writes in another team's worktree.
4. Gemini receives current branch+source or a faithful bounded code excerpt. Ask for 3 ranked design alternatives + red-team bugs, not praise.
5. Adopt only source-backed suggestions and apply minimal patch. No private content/PII to external model without user's consent.
6. Local deterministic test → exact-head Gradle Kotlin compile/target unit suite → persistent data and migration tests → emulator and real-phone UX/perf with book.
7. Update issue/PR status GREEN/YELLOW/RED/BLOCKED, dated JUnit/device evidence and rollback. Never merge draft until P0 complete.
8. Log reusable skills/patterns, naming and valid tool commands for next feature; change no unreviewed UI/reader behaviors.

## Single authority matrix
- Real book locator / EPUB / PDF / TTS / history: existing Reader owners only.
- Actual eligible real reading minutes and Rank/Path: existing GameRepository only.
- Avatar customization: AvatarAppearanceStore single writer; pixel/portrait visuals are projections.
- Fictional stock/coins/loans/shop days/customers: isolated Island Shop state writer only (Room gate in development).
- Narrative flags, dialogue, NPC schedules and map positions: dedicated island-state writer, original authored content.
- Render scenes: read-only projections. No UI recomposition awards items, no per-frame writes.
- Backup: additive, schema-versioned and atomic restore with rollback. Never corrupt old libraries for game data.

## Planned delivery train with stop/go gates
**G0 (current):** pure game reducer + deterministic transactions and tests, followed by engine-neutral content validation, core docs, Gemini critique.
**G1:** durability + backup/import/process death + economy anti-duplication. Reader unaffected.
**G2:** 2D shop slice with 2 customers, inventory, 1 shift, loan or sale, repairs, actual authored dialog, restart.
**G3:** 2 adjacent town maps, NPC navigation/schedules, one companion, exploration and meaningful story branch.
**G4:** renderer bakeoff from same content, mobile accessibility/perf, character art consistency.
**G5:** more complete RPG neighborhoods, a full chapter, player UX tests and offline balance. Then private alpha.
QA must cover Persian/English, narrow phone and tablet, accessibility/reduced motion and real book resumes.

## Blocking risks
- Moonlighting a generic game engine inside Readium lifecycle: GPU/reader input interference -> default OFF and resource-release receipt before Reader.
- Parallel branch/worktree edits: separate worktrees for game domain, durable saves, content, renderer spike. Document stacked PR base.
- Licensing: do not commit sprites/music whose redistribution rights are unknown; prototype placeholders are not shippable assets.
- Content complexity: authored strong interactions > giant empty procedural city. Create an automated content linter before scaling NPC and quest branches.
- Gamification pressure: no compulsory timers, decaying elder health, FOMO, pay-to-win or punitive streak.
- Offline regression: no sign-in/network required for reader or island.
- Missing test receipts: compile success != Android UI pass; a claimed model response != independently verified implementation.

## Shared research sources
- Android architecture: https://developer.android.com/topic/architecture/data-layer/offline-first
- Room testing: https://developer.android.com/training/data-storage/room/testing-db
- Godot stable embedding: https://docs.godotengine.org/en/stable/tutorials/platform/android/android_library.html
- LDtk: https://ldtk.io/json/
- Tiled: https://www.mapeditor.org/
- libGDX: https://libgdx.com/wiki/graphics/2d/tile-maps
- Indie competitor research: docs/design/INDIE_BOOKSHOP_RPG_MANAGEMENT_RESEARCH_2026-10-14.md
- This branch implements only engine-neutral content gate; it has no pixel sprites, runtime map loader, test APK, gameplay UI or production engine selection.
