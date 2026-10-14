# ADR-005 — First Player-Operable Bookshop Day and Independently Funded Town Lantern

**2026-10-14 | Island Bookshop RPG | status: isolated playable Godot prototype, NOT launch-quality / not Android-tested.**
**Repo branch:** `game/first-day-community-revival-20261014`, stacked on PR #535; do not merge into app Reader main.

## User's directive
Continue actual coding with equal-peer Gemini 4 Argon consultation and ChatGPT independent QA. Prove a real shop/day game cycle and one visible town reconstruction step, not a disconnected ledger dashboard. Keep the standalone Godot game distinct from Veil Reader until later opt-in work.

## The creative/gameplay pilot actually wired
- **One entry scene:** `res://scenes/first_day.tscn` and `first_day.gd`, now the isolated project's default main scene. The old `res://scenes/main.tscn` remains accessible for regression.
- **Six wholly fictional books** with unique title, initial wear, blurb, clue and separately assigned owner; private family chronicle and privately delivered letters cannot be sold by the player.
- **Two NPC customers with distinct motives:** Darya the weaver's adult apprentice wants to investigate a family record; Bahram a visiting outside merchant asks to buy an old lantern guide with his own *bounded* trade-wallet funds. Buyer purchase or refusal is genuine; a damaged book requires repair by local workshop first.
- **Three respectful routes** for Darya's family chronicle: seek permission for a later meeting (NOT opening today), preserve sealed, or consult public archives while protecting personal pages. Each is a persisted route choice; dialogue is original and NPC motive isn't just a gold meter.
- **Three distinct financing paths** for town lanterns: shop pays 14; shop pays 10 and weaver 4; shop declines and residents act: an external trader pays an explicit 8-coin ABSTRACT weaving commission, weaver then contributes 14 itself. No phantom cash; the city escrow pays a LOCAL contractor 14 and the lamp is marked completed. This is a **mock service**, NOT verified materials / jobs simulation.
- **Two navigable visual views:** shop shelves and the harbor street. `scenes/pixel_district.gd` draws original blocky pixel placeholders. After verified commission, the street adds golden lantern glow, shop windows brighten and residents appear; the visual must read `street_is_lit()`, NEVER the post-completion empty escrow.
- **Persian UI:** first scene uses RTL text_direction on dialogue/labels/buttons in native-resolution Control UI; no final accessibility certification or confirmed Android font shaping.
- **No new Reader data:** none of the actual user's personal EPUB/PDF, titles, highlights or paths are accessed. Reading incentives remain a separate future opt-in concept.

## Actual independent peer critique from Gemini 4 Argon
Sent proposed architecture before implementation to the live desktop Gemini 4 Argon and captured its detailed response at:
`E:\EyadStudio\Temp\Gemini4_FirstDay_Peer_Response_20261014.txt`.
Gemini identified five genuine risks: family legal owner versus physical custodian; resident 14-coin solvency depending on starting funds; RTL text/touch mapping; rapid press/duplicate funding; changing project main_scene before QA. Its response is **design opinion, not its own test execution**.

### Joint resolution and technical corrections
1. **Accepted:** custody/consent still belongs to the family after any Darya dialogue. `IslandWorldCommerceSession` itself rejects `owner != bookshop`; day1 consent is a request, NOT permission to inspect private content. Separate custodian/lien/beneficial interest flags are needed in a future real credit system; they are NOT falsely declared implemented now.
2. **Accepted with correction:** all residents' cash uses the existing pinned four-wallet model `bookshop=30, weaver=8, civic=0, external=50` because 74 preexisting assertions depend on it. No change to weaver=6 as Gemini suggested; a staged, finite 8-unit external craft commission means weaver has at least 16 and can sponsor 14 without magical injection. No production claim for the cloth itself.
3. **Accepted:** RTL UI placed outside pixel art via native Control nodes. A proper dual-resolution SubViewport/native portrait pipeline, font visual inspection, safe touch transform and mobile QA are **NOT done**.
4. **Accepted:** chapter action flags and real wallet transaction IDs must prevent duplicate sale/fund; rejected repeated action leaves entire in-memory snapshot unchanged. Tests include all three funding strategies and both buyer choices plus rapid repeats and JSON restore.
5. **Accepted:** built district visual reads completed ledger event, not `town_escrow` balance. The commission pays the weaver; escrow is 0 after work.
6. **Accepted:** old 74 checks had to remain green. New day scene main-scene change occurred only after separate headless import and new scene tests succeeded; full strict runner re-executed on final project.
7. **Qualified disagreement:** Gemini proposed changing the existing wallet schema to add a named `local_workers` account and alternative 2/6/20 balances. That would invalidate pinned 88-coin historical tests and masks backward compatibility costs, so for P0 the original locally-owned **weaver as named lamp contractor** receives civic payment. In later chapters, add explicit workers and supply goods under migration tests.

## Actual QA (official Windows Godot 4.7.2)
The hardened `island-game/run_tests.ps1` expects exact signatures and rejects SCRIPT ERROR even with engine exit 0. Full results on authorized Windows:
- Prior book, clock and legacy shop: **19/19**.
- Prior independent island financial proof: **30/30**.
- Prior additive unified commerce: **25/25**.
- First-day controller + button-signal GUI + family/visitor/three funding routes + JSON and finances: **118/118**.
- **192 / 192 total passing**. Headless source import and existing old+new scene startup tested, no script-error traces.

## Production-quality RED gates still open
- Gameplay is a player-operable **short prototype**, NOT yet verified 20 minutes of fun, polished narrative, adult romance scenes, 8 full businesses, or a navigable 2D town.
- Static pixel preview is procedural placeholder, not the requested premium anime portraits / pixel art nor real pathfinding.
- Story save is a combined **in-memory JSON snapshot**, not crash-safe disk save, replay journal cryptography, cross-process atomic write or Android lifecycle recovery.
- Money ledger's internal `_transfer` and mutable wallet dictionaries are still accessible; full production transactional encapsulation and fault-injection rollback tests remain.
- No physical hand-tap touch, gamepad, phone display/font or device performance proof. No APK or AAB generated.
- Existing old `IslandShopLedger` still exists as **isolated legacy scene**; it is not active alongside new world-commerce wallet in the same new scene.
