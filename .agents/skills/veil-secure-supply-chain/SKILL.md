---
name: veil-secure-supply-chain
description: Review Veil Reader third-party libraries, agents, plugins, APK references, build dependencies and permissions for licensing, integrity, privacy, reproducibility and Android security.
---

# Secure Supply-Chain + Privacy Gate

## Before integrating anything
Check existing solution and dependency overlap, repository license/provenance, maintenance activity, change history, vulnerabilities and Android/Gradle compatibility. Record exact source URL, version, immutable commit or artifact hash and a rollback plan. Never invoke untrusted installation scripts without review.
Reject copied proprietary competitor code, assets, fonts, credentials, DRM circumvention and redistributable APK material. Commercial competitors may inform independently implemented behavior through documented clean-room analysis.
Perform third-party license review and track obligations on all redistributed source and assets; public visibility does not equal a permissive license.

## Existing protections and additions
Reuse current Dependabot, dependency-review workflow, CI, source audit and commit constraints. Evaluate Gradle dependency verification metadata and dependency locking on a small branch; do not blindly bootstrap trust without reviewing hashes/keys. Add Detekt/OSV/other scanners only when their baseline, false positives, version support, maintenance cost and license are assessed.
Assess changes against OWASP MASVS: storage, cryptography, networking, platform interaction, code, resilience and privacy.
Keep offline-first the default; no extra permissions, persistent external upload, telemetry or analytics without explicit product decision and user consent. No secrets in repository, PRs, reports or chat.

## Verification
Produce a dependency diff, SBOM/provenance summary, license scan, vulnerability review, permissions/privacy diff and clean-build evidence. Block promotion for unreviewed license conflicts, leaked credentials, unsafe APK processing or new unconsented data flows.
