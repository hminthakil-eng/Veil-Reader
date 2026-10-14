# Island Bookshop — Day 1 code, visual and gameplay QA (2026-10-14)

**Godot 4.7.2.stable.official.ed1daf0bf | Branch game/first-day-community-revival-20261014 | Windows DESKTOP-GSV91KV**

### Verified user-driven game mechanics
- The new FIRST DAY main scene runs independently from legacy `ShopLedger`; it injects a `IslandWorldCommerceSession` tied to the ONE `IslandEconomyLedger` cash authority.
- Books are selectable in an OptionButton; inspect, paid repair, family choice, customer buy/refusal, funding and close-day actions bind to actual press signals.
- Three Darya scenarios do not alter legal family book ownership or reveal private pages. One visitor buyer may buy an eligible repaired shop book from real finite money; no duplicate purchases.
- Three end-to-end financing paths: player-only 14; 10/4 partner match; resident independent option with separate bounded external artisan order 8 then guild pledge 14. After paid commission, `town_escrow=0`, street completed and lighting visual drawn bright. Four-wallet conserved total 88.
- Two visual views: old shop and harbor street. Test instantiates actual scene, emits button pressed signals and observes actual pixel stage switching from dark to lit. This is a **test of Godot connections** not an actual finger touch test.

### Repro commands on Windows
`cd E:\EyadStudio\Projects\Veil-Reader-FirstDay-20261014`
`powershell -ExecutionPolicy Bypass -File .\island-game\run_tests.ps1`
Manual demo: `E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64.exe --path E:\EyadStudio\Projects\Veil-Reader-FirstDay-20261014\island-game`

### Final exact proof (strict gated stdout)
- `ISLAND_TEST_RESULTS passes=19 failures=0`
- `ISLAND_ECONOMY_TEST_RESULTS passes=30 failures=0`
- `WORLD_COMMERCE_TEST_RESULTS passes=25 failures=0`
- `FIRST_DAY_TEST_RESULTS passes=118 failures=0`
- `ISLAND_GODOT_GATE PASS ... 192 passing assertions`
- `run_tests.ps1` code checks `SCRIPT ERROR` or `ERROR: FAIL` and refuses false green even if Godot exits 0.

### Scope caveats
This is still an early limited *in-memory shop-day greybox*, NOT a shipped RPG, playable on tested Android hardware, fully narrated 20-minute slice, polished city art or complete economy. The 'weaver export commission' is an explicit bounded money movement but does not yet involve production stocks/wages. NPC day schedules, consent-driven future family quests, staffing, lore persistence across save files, real mobile touch/RTL font QA and production crash recovery are not implemented. Optional Veil Reader integration is not present. Gemini 4 Argon peer feedback captured and adjudicated in ADR-005.
