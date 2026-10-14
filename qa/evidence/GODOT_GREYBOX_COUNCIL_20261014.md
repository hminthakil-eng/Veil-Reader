# Godot Independent RPG — exact-run QA receipt

Date: 2026-10-14; Windows PC: `DESKTOP-GSV91KV`.
Branch: `game/godot-greybox-consensus-20261014`, stacked on planning branch; DO NOT merge yet.

## Provenance and engine
Official release: https://github.com/godotengine/godot/releases/tag/4.7.2-stable

Archive: `E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64.exe.zip` (86,013,866 bytes)

Archive SHA256 measured locally: `731980F9608D61333E5BAF54A2EF17210ACC7A538446C0CB9969F002ACA1E953`.

Console executable: `E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64_console.exe`.

Actual headless engine version: `4.7.2.stable.official.ed1daf0bf`.

## Local command evidence
- `Godot --headless --editor --path island-game --import --quit`: exit **0**.
- `Godot --headless --path island-game --script res://tests/run_headless_tests.gd`: **19 PASS / 0 FAIL**, exit **0** after correction.
- `Godot --headless --path island-game --quit-after 3`: main scene startup exit **0**.

Initial run surfaced 17 PASS / 2 FAIL in JSON coin parsing because Godot JSON turns numbers into floats. Patched restore to reject NaN, fractional, negative and over-limit coins while permitting safe integral floats. Re-run was 19/19.

## Proven, not proven
**PASS**: fictional book marginalia reveal, stock/damaged-sale rejection, repair costs, duplicate transaction rejection, valid sale and single coin increase, double sale rejection, paused virtual time, data-only offscreen NPC locations, in-memory versioned JSON encode/decode/unknown-schema rejection, Godot script import, main scene startup.

**NOT BUILT/TESTED**: actual pixel world, anime portraits, customer AI, authored quest engine, remote CI, Android touch/RTL, durable atomic disk save/restore under process death, Android APK/device QA, Reader bridge, economy balancing and real user playtest. Snapshot encoding is not file-system persistence. No release acceptance.

## Debate
Two genuine live Gemini 4 Argon rounds were captured on the Windows machine and accepted as **design feedback**, not source-test evidence. Agreed and contested positions documented in `docs/game-studio/ADR_001_GODOT_COFOUNDER_CONSENSUS_2026-10-14.md`.
