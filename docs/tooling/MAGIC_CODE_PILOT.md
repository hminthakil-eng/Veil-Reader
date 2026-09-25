# Magic Code controlled pilot — Veil Reader

## Decision

Status: CONTROLLED PILOT

Magic Code is used as a local implementation/verification agent. It does not replace ChatGPT/GitHub orchestration, the Figma design source, or the canonical Reader architecture.

Upstream project:
- https://github.com/kienbui1995/mc-code
- MIT licensed
- Rust terminal coding agent
- multi-provider
- workspace permission modes
- diff preview
- auto-test
- MCP/plugin support
- undo/rollback

As of the current evaluation, upstream reports v1.9.1 as the latest release.

## Why Veil Reader can benefit

Good pilot tasks:
- small Compose polish slices
- targeted bug fixes with clear reproduction
- local Gradle build/test loops
- diff review
- repetitive safe refactors
- local codebase search

Do not delegate unsupervised:
- Reader architecture replacement
- Room/persistence redesign
- release signing
- destructive Git operations
- production merge
- billing/credentials
- large multi-module rewrites

## Windows pilot setup

Use a clean Veil Reader workspace, preferably on the drive with sufficient free space.

1. Confirm Git and the JDK/Android toolchain already used by Veil Reader.
2. Install Magic Code using an upstream-supported method. On Windows, prefer a verified release binary if available for the current release; otherwise use Cargo:

       cargo install magic-code

3. Verify:

       magic-code --version

4. From the Veil Reader repository root:

       magic-code --init

5. Keep provider/model credentials local. Use the provider's environment variable or the local configuration created by Magic Code. Never commit a key.

6. Apply the project safety baseline from:

       .magic-code/config.example.toml

7. Run:

       magic-code --validate-config
       magic-code

8. In the TUI:

       /doctor
       /diff-preview
       /auto-test
       /plan

Keep auto-commit disabled during the pilot.

## First pilot task

Use branch:

    tooling/magic-code-pilot-v1

or create an isolated child feature branch/worktree from:

    integration/gray-fog-preview-v1

Suggested first prompt:

    Read .magic-code/instructions.md and docs/design/GRAY_FOG_ARCHIVE_V1.md.
    Inspect the current Gray Fog preview without changing architecture.
    Run the Android unit/lint/debug build gate.
    If compilation fails, identify the first code-caused failure, make only the smallest reversible fix, rerun the relevant gate, and show the diff.
    Do not merge, force-push, weaken tests, or modify secrets/signing/persistence.

## Verification gate

Required before accepting any Magic Code implementation slice:

    gradlew.bat testDebugUnitTest
    gradlew.bat lintDebug
    gradlew.bat assembleDebug

Record:
- exact commit SHA
- exact commands
- pass/fail for each command
- first meaningful failure if red
- diff summary
- whether any failure is code-level or infrastructure-level

A GitHub Actions job with zero executed steps is infrastructure evidence only and must not be reported as an app test result.

## Exit criteria for the pilot

Adopt more broadly only if Magic Code demonstrates:
- no unauthorized file changes
- small reviewable diffs
- accurate Gradle/test reporting
- no test weakening
- no architecture duplication
- useful rollback/diff behavior
- lower operator effort than manual local loops

If those conditions are not met, keep it as a limited utility rather than an autonomous development agent.
