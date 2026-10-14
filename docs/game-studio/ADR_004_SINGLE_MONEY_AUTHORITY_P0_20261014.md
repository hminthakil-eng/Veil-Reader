# ADR-004: Isolated Single-Authority Shop + Economy Slice (2026-10-14)

**Status:** Accepted for **Godot P0 prototype only**, draft / no merge to main. Human approval of long-term feature details and mobile player QA still pending.

## Concrete two-model review and implementation
- User explicitly requested **Gemini 4 Argon to begin development** with ChatGPT as independent critic and code/testing partner.
- Gemini 4 Argon produced a code-level proposal for moving existing ShopLedger to an economy-owned cash proxy, plus a transaction coordinator and tests. Delivered directly in Gemini desktop session (local capture: `E:\EyadStudio\Temp\Gemini4_Economy_Development_P0_Response_20261014.txt`).
- ChatGPT challenged its invasive rewrite for stale references after live rollback, changing existing bool/null API to Dictionary (breaking 49 legacy assertions), hidden cash rebalance in legacy migration, assert(false) as release access control, unsafe float parsing and the wrong Godot project path.
- Gemini **explicitly accepted** those objections in peer review round two, supporting a smaller staged copy-on-write implementation and recommending cross-domain event auditing. Local review capture: `E:\EyadStudio\Temp\Gemini4_Economy_Critique_Round2_Response_20261014.txt`.
- Actual executable resolution is **ChatGPT-authored and tested**, informed by Gemini's suggestions, not falsely represented as files written by the desktop Gemini app.

## Architectural decision
- Add independent `IslandUnifiedShop` prototype with **one actual financial authority**: `IslandEconomyLedger`; copies own condition/location/transaction references but no money field.
- Money, inventory and domain events are staged as deep independent copies. If ANY precondition, payment, item update, or test-induced failure occurs, simply discard staged data. No refund or rollback of the *live* money ledger is needed and failed TX IDs remain unused.
- On success, stage commits synchronously, without `await` or notifications in between. Money from in-game book repair really credits independent local weaver; book sale in this P0 is explicitly to **named external_trade buyer wallet**, NOT to an unfunded local NPC.
- The original `IslandShopLedger` continues to exist strictly for **legacy, isolated tests and scene**; no integrated production scene instantiates it. Do not put the two demo scenes in a unified live game or combine their sums.
- Expose read-only simple balance/snapshot APIs from `IslandUnifiedShop`; avoid exporting the raw economic ledger reference. Note GDScript underscore naming alone is not enforceable private state.

## Invariants and now verified tests
- Four wallets start 30/8/0/50 = 88. Repair cost3: 27/11/0/50; export sale12: 39/11/0/38; contributions10 and4; escrow pays local weaver14: 29/21/0/38 (total88).
- Replay IDs cannot double-pay, duplicate book sales forbidden, malformed JSON and fractional coin values rejected, off-island reserve bounded.
- Forced failure AFTER staged payment causes neither book nor live money change; same TX ID may retry.
- **Cross-audit** of serialized stories, copies, money transfers and final wallet balances: reject forged paid sale, mismatched repair prices, balanced-but-unexplained cash reallocation, orphan paid book record, nonsensical civic project's completion flag or amount, event relabeling and duplicate/replayed transactions. This is consistency validation, NOT cryptographic anti-cheat or proof against a fully forged coherent save.
- Dedicated `res://scenes/unified_day.tscn` is a **text/button Godot greybox** illustrating inspection, repair, paid off-island sale, co-funding and civic street lights. It does not deliver pixel-art lighting, high-resolution anime portraits, NPC interactions, actual mobile controls or real book reader access.
- All 95 local headless assertions (legacy19 + economy30 + unified46) and separate scene startups verified on Windows with Godot 4.7.2; exact evidence in `qa/evidence/UNIFIED_SHOP_ECONOMY_P0_20261014.md`.

## Known red gates
- Existing legacy shop scene still has separate `coins` for backwards compatibility; remove only through a dedicated, test-backed migration PR that fully switches the main Godot scene to `IslandUnifiedShop`.
- State saving currently uses in-memory JSON serialization and validation, **not durable on-disk atomic save** or recovery after process death.
- No actual physical production/supply chain, local customer wallets, merchant routing, NPC autonomous decisions, quest consequences, inventory acquisition accounting or authentic 20min player loop.
- Copy-on-write swaps private-ish instance reference per commit: do not leak raw economy references to UI; production game services should expose only queries and intents, with lifecycle ownership.
- Initial reserve of 88 is a deliberately small closed **simulation fixture**, not a long-run economic model, pricing balance, or real prosperity.
- Reader privacy intact; no real ebook, annotations, titles, URI or private metadata touched.

## Next engineering gates
G1: separate real game currency authority across all Godot entry scenes + migration strategy, no legacy balances wired.
G2: atomic disk save, crash/fault injection, compatible schema migration.
G3: two real local customers with wallets and choices, six fictional books, repair/restock gameplay, authored NPC arc.
G4: original pixel-world and portrait visuals, visible nighttime lantern swap, Android performance/accessibility and player QA.
G5: standalone game accepted before any Veil Reader integration.
