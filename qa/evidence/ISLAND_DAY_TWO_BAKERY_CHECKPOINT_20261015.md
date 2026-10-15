# Day Two Verification Receipt — 2026-10-15
Branch: game/day-two-bakery-archive-20261015
Godot: portable 4.7.2-stable, Windows desktop DESKTOP-GSV91KV

## Automated tests actually executed locally
- Original day one, shop/clock, money, commerce, checkpoint: 19+30+25+118+45 = 237 PASS (unchanged).
- New 3 payers × 3 delivery routes, stock conservation, archive privacy, payer-provenance, JSON reload, forged inputs, replay guards: 169 PASS.
- New disk-save crash injection/fallback, checksum recomputed with forged grain, UI pressed signals, 52px targets, text_direction RTL flags, and first-to-second button gates: 40 PASS.
- **Strict suite total 446 PASS / 0 FAIL**; Godot clean import, legacy scenes startup, new scene startup. Strict runner fails on Parser Error or Script Error regardless of process exit code.

Run locally:
`& 'E:\EyadStudio\Projects\Veil-Reader-DurableSave-20261014\island-game\run_tests.ps1'`

Observed intermediate test failure: after locked startup, the Save/Load buttons were not re-enabled by _refresh(); fixed in second_day.gd and reran 40/40 successfully.

## Limits / not verified
- No Android device run, app/APK export, installed app smoke, or mobile touch/RTL font screenshot inspection.
- No real economic AI agents or simulation of demand/supply beyond fixed finite first recipe.
- SHA-256 checksummed JSON is for corruption detection; not a cryptographic proof against an adversary who recomputes the hash.
- No claim of power-loss atomicity on all mobile filesystems or independent Gemini 4 approval.
