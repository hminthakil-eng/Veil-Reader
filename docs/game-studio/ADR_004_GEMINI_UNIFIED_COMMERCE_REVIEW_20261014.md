# ADR-004 — Gemini 4 Argon Implementation Review: Preserve the Previous Working Gates

**Date:** 2026-10-14 | **Scope:** isolated Godot 4.7.2 game branch, not merged into reader or public game.
**Status:** Gemini's proposed destructive rewrite **REJECTED after real regression failures**. A smaller additive integration path **ACCEPTED for further QA**; not a release decision.

## Peer discussion and actual execution
The authorized Gemini 4 Argon Desktop window received the full Island Bookshop game plan and was instructed to **begin coding**, not just brainstorm. Gemini generated full code for a coordinator and destructive replacements of legacy shop/economy ledgers, plus 55 new assertions. Its source reply was captured at:
`E:\EyadStudio\Temp\Gemini4_Implementation_Unified_Commerce_Response_20261014.txt`.
The original proposed candidate was applied to separate reversible Windows worktree `game/unified-transaction-verticalslice-20261014`, without touching main.

**Candidate result:**
- Import: exit 0.
- Gemini's new integration tests: **55/55 PASS**.
- Legacy 19 test suite: **BROKEN**, due to changed `add_copy` return type (bool -> Dictionary), changed `from_snapshot` return type, and now-invalid `coins` snapshot. Only 7 assertions reached; Script errors were not caught by the old harness, which returned exit 0. This was a **false green**, not 19/19 pass.
- Legacy 30 economy test suite: **BROKEN**, missing `total_coins()`, changed snapshot return and failures in invalid save rejection; 0 passes, 3 explicit fails.
- These were observed by running actual Godot 4.7.2 headless commands, not speculative code review.

**Critical recommendation:** Never substitute a new 55/55 headline for regression compatibility. Do not hide failures by relaxing tests. Existing public return types and signatures are contracts until explicitly migrated and tested. An in-place proxy `shop.coins` with direct assert(false) assignment is not a safe first step in a working reader/game project.

## Safer incremental decision
1. Restore **both original** `core/shop_ledger.gd` and `core/economy_ledger.gd` byte-for-byte from the last known-good commit; old Godot scenes/tests retain their behavior.
2. Add `core/world_commerce_session.gd` only. It accepts **one authoritative injected `IslandEconomyLedger`** for its *new* combined path and stores **no wallet value** in its copy/inventory records. Legacy `IslandShopLedger` remains an isolated old demo, not instantiated by the new integration.
3. A book sale first checks owner=bookshop, stock, condition, source payer wallet, ID and price; only then posts an existing synchronous economy `_transfer` from buyer to bookshop. Post-transfer only direct local item state mutation and log append; no callbacks, scripts, dialogue or await. The same pattern pays the weaver for a shop-owned book repair.
4. Private collateral/consigned family books **cannot be sold** or repaired without authored permission/ownership transfer. This blocks a critical narrative/economic exploit.
5. The new session's JSON snapshot contains one economy snapshot and inventory/journal. Deserialization rejects fabricated currency supply, invalid copy schema, sold books lacking matching financial journal event, replayed transaction IDs, unsupported version. This is in-memory data consistency, **not** a crash-safe on-disk save implementation.
6. New headless tests supplement legacy tests rather than rewriting them. Strengthen the runner so a Godot SCRIPT ERROR or missing expected PASS count fails even if old SceneTree exits with 0.
7. Do not bind the old shop greybox and new world-commerce path to the same UI until the single-authority migration and explicit save-schema upgrade are reviewed.
8. Retain game/reader independence and no real EPUB titles/URIs/highlights in game data. No mandatory reading to fund town services.

## Follow-up acceptance
- Pre-existing 19/19 + 30/30 unchanged and new 25/25 assertions must all pass under the same pinned engine and a strict runner.
- Distinct book-owner rejection, insufficient buyer funds, duplicate tx, separate seller/payer events, merged snapshot + invalid save tests are P0.
- Later: consumer SceneTree scene, on-screen customer flow, standalone NPC economy branch, true durable save/process-kill recovery, mobile/RTL playtest.
- Further critique asked of Gemini Argon; its new implementation proposal cannot bypass the old regression gate.

## Known technical limitation, not hidden
`IslandEconomyLedger._transfer` is still an exposed synchronous method and the Godot environment has mutable dictionary access; this session keeps the **intended** single source of financial truth but is not fully encapsulated, hardened for hostile/modded game code, or process-failure atomic. Future transactional core can offer an immutable staged commit instead of relying on same-thread synchronous ordering.
