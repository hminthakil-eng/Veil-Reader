# Reader toolchain audit — 2026-10-20 / PR558

Purpose: diagnose Dependency Review run39502326354 without changing UI or suppressing security checks.
Source: exact e182b46cdf982383c96bbe26dfc0dc521263c08c; immutable parent of this mission commit. Product root build.gradle.kts is byte-identical to main811f0d7d and already constrains BC1.85.
Finding: dependency-review logs show settings.gradle.kts bcprov1.80.2, two GHSA advisories, and missing base snapshot (0 vs1). Actual AGP-loaded provider version remains unknown.
Operations: verify parent, copy owned audit init outside project, checkout exact source, JDK21/Gradle9.6 read-only-cache buildEnvironment plus classloader provider provenance.
Outputs: source identity, payload checksum, resolved BC versions and actual AGP classloader provider jar/version in job logs. No new artifact or dependency submission.
Permissions: contents read only; no secrets or write-capable token; checkout credentials not persisted.
Terminal state: inspect output and distinguish actual vulnerable provider from graph-only evidence; keep branch while it has debugging/handoff value. Branch is task-owned diagnostics/reader-toolchain-20261020-558. Never merge its mission files into product.
