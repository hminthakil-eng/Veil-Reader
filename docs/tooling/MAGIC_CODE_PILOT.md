# Magic Code Pilot — Veil Reader

## Purpose
Use Magic Code as a local implementation/verification worker for small, reversible Veil Reader tasks. ChatGPT/GitHub/Figma remain the architecture, orchestration, review, and design source-of-truth layer.

Upstream: https://github.com/kienbui1995/mc-code

## Safety profile
Project config uses `workspace-write`. In Magic Code's current permission model, file writes inside the workspace are allowed while shell execution requires a prompt. Secrets and Git internals retain Magic Code's built-in file protections.

No provider, model, or API key is committed to this repository. Configure those locally.

## First local session
From the Veil Reader repository root:

1. Install Magic Code using an upstream-supported method for the machine.
2. Run `magic-code --validate-config`.
3. Run `magic-code`.
4. In the TUI, run `/doctor`.
5. Enable `/dry-run`, `/diff-preview`, and `/auto-test` before the first editing task.
6. Read `.magic-code/instructions.md` and `docs/design/GRAY_FOG_ARCHIVE_V1.md`.
7. First pilot task: inspect the Gray Fog integration preview, identify one small UI mismatch, propose a patch, and do not write until the diff is approved.

## Verification gate
When a patch is accepted, run:
- Windows: `gradlew.bat testDebugUnitTest lintDebug assembleDebug`
- POSIX: `./gradlew testDebugUnitTest lintDebug assembleDebug`

Do not interpret GitHub Actions jobs with zero executed steps as application test failures.

## Exit criteria for the pilot
Magic Code is useful to retain only if it:
- respects the workspace boundary,
- produces reviewable diffs,
- does not duplicate architecture,
- successfully runs the requested verification gate,
- improves implementation throughput without reducing reliability.

If those conditions are not met, remove this pilot branch/config without affecting the Reader RC.
