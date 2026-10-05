# R0.10 — Dependency, License and SBOM Governance

Date: 2026-10-05
Branch: `reforge/dependency-governance-v1`

## Goals

Veil must know what it ships and must not introduce a dependency merely because it is convenient.

This gate provides:
- a GitHub Gradle dependency graph;
- vulnerability review for dependency changes;
- license-policy review for new dependencies;
- OpenSSF scorecard signals;
- a CycloneDX aggregate SBOM in JSON and XML;
- controlled weekly Dependabot updates for Gradle and GitHub Actions.

## Tooling

### Gradle dependency submission
Uses `gradle/actions/dependency-submission@v6`.

The dependency graph feeds GitHub dependency insights and vulnerability alerts.

### Dependency Review
Uses `actions/dependency-review-action@v5`.

Policy:
- fail new **runtime** dependency changes at High/Critical vulnerability severity;
- show patched versions;
- show OpenSSF scorecard signals;
- reject newly introduced dependencies under the explicit denied-license list.

The license list is a product-policy guard, not legal advice. Any exception requires an explicit documented review.

### CycloneDX
Uses `org.cyclonedx.bom 3.4.1` from a CI-only Gradle init script.

The plugin is not added to Veil's normal build plugin graph and adds no runtime code to the Android application.

Artifacts:
- `build/reports/cyclonedx/bom.json`
- `build/reports/cyclonedx/bom.xml`

## Dependency admission rule

A new production dependency must state:
1. user problem solved;
2. why existing Android/Readium/Veil code cannot solve it adequately;
3. source repository and maintainer;
4. release freshness;
5. license;
6. transitive dependency impact;
7. APK/AAB size impact where relevant;
8. security/vulnerability state;
9. privacy/network behavior;
10. rollback/removal path.

## Rejections

Reject a dependency if:
- it duplicates an existing owner;
- it exists only to silence lint/style;
- it adds a network path without product approval;
- its license conflicts with product policy;
- it introduces High/Critical known vulnerability without an accepted exception;
- it materially increases package size for a feature that can stay optional;
- it is unmaintained and sits on a critical parser/security boundary.

## Update policy

Dependabot opens controlled weekly PRs. Updates are not auto-merged.

Every dependency update must still pass:
- Android CI
- Storage Instrumentation where triggered
- Performance Benchmarks where triggered
- Dependency Governance
- subsystem-specific Reforge gates

"Latest" is never an auto-merge reason.


## First gate finding and remediation

The first Dependency Review correctly rejected the CI SBOM toolchain because
CycloneDX Gradle Plugin 3.4.1 resolved Bouncy Castle `bcprov-jdk18on 1.80.2`,
which is affected by:
- CVE-2026-8763 / GHSA-9pwp-9qqc-pr26 (Critical)
- GHSA-qp49-qgx5-5m26 (High)

The CycloneDX plugin remains the current 3.4.1 release, so Veil's CI-only init
script now pins the aligned Bouncy Castle `bcprov/bcpkix/bcutil` family to 1.85,
the patched line. This does not add Bouncy Castle to the Android application
runtime; it hardens the SBOM-generation toolchain itself.
