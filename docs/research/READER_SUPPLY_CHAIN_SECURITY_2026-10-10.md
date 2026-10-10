# Canonical Reader SBOM and dependency submission hardening — 2026-10-10

## Evidence-based gap
`.github/workflows/dependency-baseline.yml` did not target the active
`grand-forge/p0-kindle-reader-quality-20261008` branch for either PRs or
pushes. It previously granted `contents: write` at **workflow scope** to
PR builds even though the Gradle dependency-submission API write privilege
is needed only for trusted push/manual runs.

## Minimal reversible fix
- Canonical branch included for PR and push (with existing push path filters).
- Workflow default and PR job use `contents: read`.
- Untrusted PR builds only use the already-pinned Anchore Syft action to
  produce a **source-tree CycloneDX JSON inventory**. They do not execute
  repository Gradle scripts with a write-capable token.
- Push/manual `dependency-graph` job alone receives
  `contents: write` and retains Gradle dependency submission plus both
  existing CycloneDX JSON/XML outputs and 14-day artifact retention.
- No new third-party action, Gradle dependency, Android permission, telemetry,
  secrets, app code, or production release gate.
- `tools/verify_reader_supply_chain.py` and synthetic tests enforce job-level
  event/permission separation, canonical event targeting and pinned inventory.

## Acceptance/limits
- Require exact-head workflow `Reader Supply Chain Contract` PASS.
- Require PR `pr-source-sbom` PASS and artifact provenance check.
- A PR source-tree scan is **not** a resolved Gradle dependency graph and
  does not certify all transitive Android libraries.
- A trusted canonical push or explicit workflow dispatch must independently
  confirm the `dependency-graph` GitHub submission succeeds; PR CI alone
  cannot establish this.
- Confirm that `Dependency Review` is enabled for canonical Reader targets
  via Grand Forge skill intake PR #453; keep separate branch ownership.
- Roll back via a single PR revert and retain the old artifacts/CI reports.

Reference: GitHub Actions `GITHUB_TOKEN` least-privilege permissions;
Gradle dependency-submission action and existing pinned Anchore Syft workflow.
