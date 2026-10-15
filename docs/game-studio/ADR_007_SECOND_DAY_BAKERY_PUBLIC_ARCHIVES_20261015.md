# ADR-007 — Day Two: Finite Bakery, Public Archives and Real Wallets
Date: 2026-10-15
Status: EXPERIMENTAL / DRAFT / NOT PRODUCTION-READY
Branch: game/day-two-bakery-archive-20261015

## Decision
Add a truly playable second day after a completed first-day checkpoint, without breaking any older scene/save/API. Keep the four existing day-one wallets (bookshop 30, weaver 8, town escrow 0, external trade 50) and the strict 88-coin supply. On first entry to day two, register ONE bakery business account with ZERO currency. New checkpoints accept either four legacy wallets or those original four plus bakery; extra arbitrary accounts are rejected.

## Player-facing loop
1. Speak to MahTab, the baker, and to a public archivist.
2. Choose among public registry, a REQUEST for private-owner consent, or respecting the seal while pursuing another clue. None unlock private documents without permission.
3. Prepay four coins through exactly one real payer: bookshop, independent weaver, or finite external buyer. Never mint coins or count Reader books as money.
4. Bake four loaves by consuming four of six grain portions and one of two textiles. Bakery pays TWO coins to weaver for the apron service.
5. Deliver exactly four loaves: all public pantry, all market stalls, or two each. The receiving channels are outcomes, NOT additional revenue.
6. Close the day, save an append-only versioned day-two checkpoint, or restore a previous verified state.

## Single authority / migration
- Money is in IslandEconomyLedger ONLY; day-two stock has no currency field. Day one still serializes and loads with the original schema.
- Second-day snapshots embed the exact completed day-one economy and all decisions. Reload validates grain, textile, bread, archive route, payer, matching transaction IDs, and the original 88-coin supply.
- Legacy first-day save files remain untouched. A separate user://island_bookshop_day2 checkpoint directory stores day-two state. Saves are checksum-verified, written to staging, then renamed, with fallback on corruption. No claim is made of malicious-edit resistance or Android power-loss atomicity.
- Entering day two from UI automatically saves the completed first day before switching scenes. It refuses to advance if that save fails.

## Independent critical review (ChatGPT)
- GOOD: Branch-preserving incremental migration, real (not generated) cash, named individual NPC interests, consistent protected ownership, state-derived outcomes, repeat-action blocking, real Godot button signal tests.
- STILL RED: This is a greybox, not a rich story or world economy. Bakery has no autonomous schedules, ingredient supplier prices, grain procurement, sales revenue, consumer willingness-to-pay, population food needs, delivery animation, or time cycle. Market delivery is explicitly PREPAID with no second sale. Multiple funders all lead to the same recipe; their strategic consequences should be stronger in day three.
- STILL RED: No native Android export/profile, touch or Persian glyph screenshot audit; single-column 52px action targets are a preliminary design measure, not verified device accessibility.
- STILL RED: Financial import validates supply totals, paired day-two events, and stock count, but does not comprehensively reconstruct ALL wallet balances from an independently authenticated ledger. Not anti-cheat.
- STILL RED: Pixel district remains abstract placeholder; no anime portrait rig, cut-scenes, ReaderBridge, relationship systems, town autonomy, or polished 15-20-minute narrative.

## Gemini 4 peer-review request
Gemini Windows app processes were detected locally, but there was NO verified interactive Gemini-4 response during this change. This document is an independent critique and a handoff request, NOT claimed Gemini approval.
Request Gemini critique: (a) bakery account migration and day1/day2 snapshot coherence; (b) strict cash/stock reconciliation and adversarial forged save attack; (c) narrative branching with persistent tradeoffs (without coercive romantic systems); (d) Android RTL and tactile UI; (e) autonomous NPC production chain, suppliers, demand and scarcity. Preserve original Reader branch and keep all proposed changes isolated and reversible.

## Next acceptance gate
Run on Android emulator/device at target portrait resolution with Persian font glyph evidence and tap/restore scenarios, then expand one chain into an actually self-directed bakery/weaver/archives schedule. No merge to production until those gates are green.
