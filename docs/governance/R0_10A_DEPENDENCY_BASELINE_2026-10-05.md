# R0.10A — Dependency Baseline Bootstrap

Date: 2026-10-05

This is the bootstrap half of dependency governance.

## Why two phases

GitHub Dependency Review compares dependency state between a PR head and its base.
Before Veil has a committed dependency graph baseline, the first submission makes
the repository's existing build-tooling graph appear as a new delta. That caused
the initial enforcement PR to flag an already-present Bouncy Castle 1.80.2
build-tooling dependency as if it had been introduced by the governance PR.

We do not hide or allowlist that finding.

Instead:

1. commit the Gradle dependency graph + SBOM baseline first;
2. let GitHub record the canonical dependency state;
3. enable Dependency Review in a second PR;
4. future PRs are judged on actual dependency deltas.

## This PR adds

- Gradle dependency submission;
- CycloneDX JSON/XML SBOM using pinned Anchore SBOM action + Syft;
- weekly controlled Dependabot updates.

No runtime Android dependency is added.

## Follow-up gate

R0.10B will add:
- High/Critical vulnerability rejection for new runtime dependency changes;
- denied-license policy;
- OpenSSF scorecard visibility.

Existing baseline vulnerabilities remain tracked separately and must be remediated
through explicit toolchain/dependency upgrades, not silently accepted.
