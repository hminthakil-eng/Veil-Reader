# Island Economic Recovery — Test Evidence 2026-10-14

Windows workstation `DESKTOP-GSV91KV`, Godot official standard build `4.7.2.stable.official.ed1daf0bf`, branch `game/island-economy-revival-v1-20261014`. Based on Godot greybox PR #530. This is a design and code proof, not a shippable mobile game.

## Actual local runs
- Godot `--headless --editor --path island-game --import --quit`: **exit 0**.
- Godot `--headless --path island-game --script res://tests/run_economy_tests.gd`: **30 passes / 0 failures, exit 0**.
- Godot `--headless --path island-game --scene res://scenes/economy_proof.tscn --quit-after 3`: **exit 0**, scene smoke test.
- Final combined `island-game/run_tests.ps1` run: original Godot core **19/19 PASS**, economy **30/30 PASS**, both scenes startup exit 0; runner exit **0**. Exactly **49 passed assertions** across two separate test runners.

## Exact tested scenario
Start cash `bookshop 30 + weaver 8 + civic_escrow 0 + external_trade 50 = 88`.
1. Finite outside order 12: `bookshop 42, weaver 8, civic 0, external 38`.
2. Real cash payment 8 to independent weaver: `bookshop 34, weaver 16, civic 0, external 38`.
3. Bookshop pledges 10: `bookshop 24, weaver 16, civic 10, external 38`.
4. Weaver voluntarily pledges 4: `bookshop 24, weaver 12, civic 14, external 38`; lanterns fully funded but not built.
5. Town commission pays weaver 14: `bookshop 24, weaver 26, civic 0, external 38`; model street-lantern state changes to complete. **88 remains constant**.

Test categories: finite outside buyer funds, no negative amount or overdraft, no repeated tx id, transfer recipient credited, no overfund, civic commission only funded, no second payment, versioned JSON snapshot, rejection of unknown schema/fractional or extra coins, replay after restart, indicators derived from event history. Exactly 30 assertions accepted.

## Peer review
Actual Gemini 4 Argon desktop analysis and second adversarial critique; first Gemini model draft incorrectly funded 45 coins from outside wallet with only 30 and omitted a 5-coin supplier counterparty; ChatGPT challenged and Gemini explicitly acknowledged/corrected. Joint ADR: `docs/game-studio/ADR_003_ISLAND_ECONOMIC_REVIVAL_20261014.md`.

## What is NOT proved
- Separate financial `IslandEconomyLedger` and real `IslandShopLedger` both use simulated player cash. **No integration.** They are NOT connected, not two legitimate player wallets in production. Source of authority decision still required.
- No actual paper/cloth inventory, employment model, local wages, credit/loan engine, trade replenishment, port travel, NPC pathfinding, community-owned alternative construction, visually rendered pixel city, music, art, animation, real book Reader integration, Android export or Android device performance.
- Scene smoke test is headless startup only, NOT proof interactive buttons work from touchscreen or physical scene quality.
- Money conservation models accounting correctly for the toy four-wallet world only. It is NOT proof that a complex economy is prosperous, inflation-safe, or balanced, nor a secure anti-cheat system for modified save files. Persistent JSON is in-memory roundtrip, not durable disk crash recovery.
