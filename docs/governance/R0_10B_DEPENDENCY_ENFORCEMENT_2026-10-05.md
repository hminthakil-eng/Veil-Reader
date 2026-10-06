# R0.10B — Dependency Review Enforcement

Date: 2026-10-05

R0.10A established the canonical dependency graph and CycloneDX SBOM baseline.

This phase turns on delta enforcement.

## Reject newly introduced changes when

- vulnerability severity is High or Critical;
- license is AGPL-3.0-only/or-later, GPL-3.0-only/or-later or SSPL-1.0;
- the dependency review action itself cannot establish a trustworthy delta.

The policy intentionally does not limit checks to Android runtime scope. Build
plugins and CI dependencies are part of the software supply chain too.

## Signals

- patched-version guidance is displayed;
- OpenSSF scorecard signals are shown;
- scorecard below 3 is warned on, not auto-rejected by itself.

## Baseline debt

Pre-existing vulnerabilities are not silently declared safe. They are tracked as
baseline debt and should be removed through explicit version/toolchain upgrades.
The purpose of this gate is to prevent new debt from entering unnoticed.
