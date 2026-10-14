# QA Receipt — Day 1 Append-Only Checkpoint and Recovery

**Date:** 2026-10-14
**Desktop:** DESKTOP-GSV91KV (Windows)
**Engine:** Official Godot 4.7.2 stable
**Branch:** game/durable-checkpoint-recovery-v1-20261014

## Verified runs
- Godot clean headless source import exit 0.
- Independent Godot test command: `--headless --path island-game --script res://tests/run_checkpoint_tests.gd`: **DAY_CHECKPOINT_TEST_RESULTS passes=45 failures=0**, exit 0.
- FULL `island-game/run_tests.ps1`: **237/237 PASS**, strict expected summaries, zero SCRIPT ERROR/ERROR: FAIL matches, runner exit 0.
- Included existing shop/clock 19, economic ledger 30, combined commerce 25, Day 1 118, checkpoint 45.
- Both separate legacy and default first-day scenes cold-start via headless smoke.

## On-disk fault scenarios run (not theoretical)
- Save A, save B, confirm A bytes unmodified; B is loaded as latest.
- Simulate application interruption **between flushing staging JSON and renaming**; the remaining `.part` does not appear as committed and B still restores.
- Commit save C then deliberately replace C with invalid JSON; loader chooses older healthy B.
- Commit save D with a recomputed checksum BUT forged extra wallet currency: internal finance invariants reject and loader falls back.
- Store a fully completed day with separate NPC funding of street; reload retains full story choice, 88 money, lamp completion and escrow payout, duplicate commission blocked.
- Instantiated actual first-day scene; emitted Save and Load `Button.pressed` signal, verified money and choices restored.

## Strong limitations
- This is not real OS/process power-cut testing or an Android test; no guarantees on Linux/Android/iOS rename durability yet.
- No adversarial authentication—plain checksum detects accidental corruption but attackers can modify saves; economic invariant provides extra sanity checks, not cryptographic authenticity.
- Not final RPG story, UI graphics, fonts, controller/gamepad nor production-ready release. These tests are green for the scoped checkpoint proof only.
