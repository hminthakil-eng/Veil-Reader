# ADR-003 — Rebuilding the Island, Not Just the Bookshop

**2026-10-14.** Two **actual** Gemini 4 Argon desktop debate rounds; independent critique, ChatGPT counterarguments, amended co-decision. Architecture provisionally ACCEPTED for prototype, **no merge/release or canon locked**.

## User's clarified fantasy
The island's economy has collapsed; its inhabitants' shops, trade, work and civic spaces are struggling. The protagonist arrives with amnesia and gains shelter above an old bookshop. **The bookshop is the protagonist's business; the island is a shared community, not a set of properties the protagonist owns.** Over time the town visibly recovers through entrepreneurs, cooperatives, public works, social choices and investigation, not a passive prosperity meter. Story, optional adult relationships, exploration and actual books remain separate yet causally meaningful.

## Verified role of peer debate
Gemini independently challenged ChatGPT's proposal as scope-heavy and offered:
- Story-oriented, simulation-lite economy rather than a full household production simulator.
- 3 **player-facing** perceptions: market turnover/flow, civic works, mutual aid/guild participation. Underlying wallets and events remain explicit, not hidden game fudge.
- Five economic-era transformations; residents may repair infrastructure without protagonist ownership.
- Small isolated ledger + a five-step financially balanced test, not 8 businesses and a tax algorithm before a player enjoys selling a book.
- Bounded off-island trade to avert a sealed-economy liquidity trap, and optional real reading rewards for cosmetic world enhancement.

In peer **round two**, ChatGPT challenged a mathematically impossible example: the initial external trading reserve held 30 coins but two example orders spent 45; further 5 coin supply purchase had no accounted payee. Gemini **agreed explicitly** and corrected bounded external reserve and cash invariants. Both also agreed that saving two days does not logically imply zero market velocity; different wallets for ShopLedger and CityLedger cannot be active together until a source-of-truth adapter is designed. Gemini accepted restrictions around no private ebook quotations, no mandatory reading/time streak, no irreversible city restoration story block, and no artificial sub-5ms performance claim.

Raw Gemini review captures on authorized desktop:
- `E:\EyadStudio\Temp\Gemini_Island_Economic_Revival_Round1_20261014.txt`
- `E:\EyadStudio\Temp\Gemini_Island_Economic_Revival_Round2_20261014.txt`.

## Decision 1 — Design genre
**Narrative-driven 2D pixel-art town-rebuilding RPG plus hands-on bookshop economics.** Not a standalone Excel economy simulator, not generic city-builder and not an idle shop. The relationship between a neglected bookshop and damaged island trade is the game's distinctive hook.

## Decision 2 — Staged island revival
1. **Candle Era:** original shop alive amid shuttered streets; cash-poor families barter, written credit ledgers, practical repair/lending.
2. **Workshops Awake:** bakery/loom/carpentry/repair active; local procurement, craft commissions and initial market staff.
3. **Market and Trust:** neighborhood co-ops, community lending, printshop and marketplace. Disagreement over open contracts versus creditor monopoly.
4. **Harbor Reopened:** disputed shipping manifests and export contracts; player chooses community governance or private contracts. Elder's revisions and forged logistics records connect deeply to the main mystery.
5. **Archival Bloom:** full harbor + streets + school + public library, festivals, public archive and adult characters' family/community arcs; status of town ownership and knowledge depends on accumulated choices.

Stages are district-dependent, NOT an irreversible numeric tier. A town square can thrive while the dock remains struggling; districts can be improved in different orders. A restored street is shown with *light, movement, workplace sounds, NPC work shifts, shops open*, not only a banner.

## Decision 3 — Ownership and opportunity
The protagonist **owns the bookstore only**. The weaver, fisher cooperative, carpenter, bakery, tea house, printshop, courier company and clinic have separate owners, profits, spouses, children and personal choices. Player may become contractor, co-op supporter, lender, joint-venture minority partner, bookshop merchant or public benefactor, each with rights/limits and real consequences. A player who refuses contributing should still witness townspeople organize a smaller, slower restoration path.

## Decision 4 — Two compatible design layers (not two financial authorities)
- **Micro:** bookshop stock, purchasing, pricing, trade, repair condition, lending, referrals, staffing, furnishings, skills and customer needs.
- **Meso:** explicit supplier contracts, workshops, commission repairs, market stalls, neighbor credit, apprenticeships, local employment.
- **Macro:** local projects, pier insurance, port permits, tariffs/trade routes, mutually competing public/private development ideas.
- Start with **ONE** local economic transaction contract and **ONE** civic street light job. Do not simulate millions of household transactions. Three town signals are projections from actual local economic events; causal records must support each displayed metric.

## Decision 5 — Money integrity, testing and ownership gap
The tested independent Godot `IslandEconomyLedger` (RefCounted) has four named wallets: protagonist bookshop 30, NPC weaver 8, civic escrow 0 and **bounded** external buyer 50 (total initial money **88**). All tested movements are debited from a real wallet and credited to another in one synchronous code path; tx IDs reject repeats. Repair service/material stock, loan interest, demographic prices, import costs and full business income have **NOT** been implemented; the weaver contract is an abstract service for now.

A deterministic prototype scenario:
- Export order 12 moves **external 50 → 38; bookshop 30 → 42**.
- Buy local weaving service 8 moves **bookshop 42 → 34; weaver 8 → 16**.
- Bookshop civic pledge 10 moves **bookshop 34 → 24; escrow 0 → 10**.
- Weaver civic pledge 4 moves **weaver 16 → 12; escrow 10 → 14**, project FULLY FUNDED.
- Commission actual paid local work 14 moves **escrow 14 → 0; weaver 12 → 26**, project COMPLETED and street-lamp restored preview enabled.

The sum remains 88 at each step. This does not mean actual increase in total wealth/GDP; it demonstrates cash provenance and town restoration through contractual work. Future cash inflows require explicit external demand budgets, production/stocks/labor and economic causality beyond currency conservation.

**Critical blocker:** Current existing `IslandShopLedger` has its *own* player coins. The proof's separate "bookshop" wallet is not authoritative live game money. Neither can debit the other's funds; **do not put both in production UI together** until an ADR establishes one source of monetary truth and atomic book transactions. First prototype is a dedicated separate scene.

## Decision 6 — Narrative consequence and recovery
Distressed town **never** becomes free character rewards: every civic project has named NPC stakeholders and distinct means of funding (cash, collective effort, repair work, barter, political advocacy). Monopolistic creditors and the alleged shipping ledger fraud form the story, but players may investigate by witnesses, duplicate records or cooperative efforts. Selling the original documents can worsen reputation and require a longer route; it cannot hard-lock the main game. The elder's essential medical care and players' real-world reading pace must never be hostage to the town economy.

## Decision 7 — Optional Reader signals
The game and Veil Reader remain independently usable. Future opt-in aggregate reading signal can earn beautiful alternate street lantern patterns, city facades, portrait frames and optional visual festival flourishes, with no mandatory reading, page-flip farming, exposure of actual ebook text/annotations/files/quotes/titles or unsupported TTS monitoring. Functional civic basics accessible through in-game economy. No real-time check-in or FOMO.

## Decision 8 — Acceptance, constraints, and rollout
Source changes from this ADR are only a pure GDScript financial ledger, unit tests, and a **separate** small scene proving the ledger. A new dashboard/quarter, employment, raw materials, credit lines, dynamic NPC schedules, reader rewards and mobile app remain unbuilt. The only short-term GREENs must be exact headless outcomes and an actual runnable scene. Next gating: unify economic authority with ShopLedger behind a reversible adapter, one real business/day loop with 2 customers and neighborhood actor; one visually restored location, one NPC autonomy branch, recovery and on-device playtest.
