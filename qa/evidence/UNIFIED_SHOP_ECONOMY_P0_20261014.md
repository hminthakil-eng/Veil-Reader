# Godot shop+island single-money-authority prototype — local QA (2026-10-14)

**Machine:** Windows DESKTOP-GSV91KV, engine `4.7.2.stable.official.ed1daf0bf`. Branch `game/economy-single-authority-p0-20261014` (reversible, stacked above PR #533). **NOT a release or Android pass.**

## Commands
From the repository worktree:
```powershell
& '.\island-game\run_tests.ps1'
```
Or check the new scene from Godot standard console:
```powershell
& 'E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64_console.exe' --headless --path '.\island-game' --scene 'res://scenes/unified_day.tscn' --quit-after 3
```

## Verified
- Godot project clean headless import: exit0.
- Legacy original single-book greybox: **19/19**, headless PASS.
- Prior isolated island financial proof: **30/30**, headless PASS.
- NEW `IslandUnifiedShop` inventory + economic staged copy-on-write: **46/46**, headless PASS, including 8 added negative/fraud/replay-focused tests.
- 3 text demo scene startup smoke checks passed: legacy main, separate financial prototype, new unified shop/economy proof.
- Combined regression runner: **95 headless assertions, 0 failures** when re-run after latest patch (see actual combined output).

## Cross-domain scenario
Start bookshop30,weaver8,civic0,outside50 -> repair3 to weaver -> (27,11,0,50) -> external book sale12 (39,11,0,38) -> shop contributes10 (29,11,10,38) -> weaver contributes4 (29,7,14,38) -> town escrow pays weaver14 (29,21,0,38). Total88 ALWAYS.

## Safeguards actually exercised
Damaged book not sellable; must be repaired; missing item no debit; insufficient external buyer cash; TX replay rejected; duplicate sold book rejected; civic overfund/recommission rejected; inserted failure after staged debit leaves original book/money untouched with ID reusable; JSON roundtrip and balance verification; tampered wallet, fractional money, altered repair cost, orphan sale, fake civic completion, relabeled transfer and cross-domain missing ID rejected.

## Still unverified
No on-disk atomic save, no Android device or touch runtime, no actual art and NPC/autonomous economy, no playable 20min chapter, no UI accessibility/RTL test, no private Veil Reader integration. Godot headless startup does not validate real clicking buttons; manual UI playtest needed before feature acceptance. In-memory consistency cannot guarantee resistance to a coherent full save forge; no security claim is made.
