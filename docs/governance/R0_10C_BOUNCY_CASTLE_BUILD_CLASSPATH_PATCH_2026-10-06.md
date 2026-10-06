# R0.10C — Patch vulnerable Bouncy Castle on the Gradle plugin classpath

Date: 2026-10-06

## Finding

Dependency Review identified `org.bouncycastle:bcprov-jdk18on:1.80.2` under the
`settings.gradle.kts` / build dependency graph, not the application runtime graph.

The affected advisories are:

- GHSA-9pwp-9qqc-pr26 / CVE-2026-8763 — Critical.
- GHSA-qp49-qgx5-5m26 / CVE-2026-13506 — High.

Both are patched by Bouncy Castle 1.85 or later.

## Source

Veil does not declare Bouncy Castle as an application dependency. The dependency is
introduced transitively through the Android Gradle Plugin toolchain. AGP 9.4.0 is the
current compatible Android plugin line for this project and still resolves the older
Bouncy Castle family upstream.

Gradle's dependency-submission guidance explicitly recommends a dependency constraint
on the buildscript `classpath` when a vulnerable transitive dependency comes from a
Gradle plugin and no suitable newer plugin release removes it.

## Remediation

The root buildscript constrains this build-time family to 1.85:

- `bcprov-jdk18on`
- `bcutil-jdk18on`
- `bcpkix-jdk18on`

The three artifacts are intentionally kept aligned. This is the minimum patched family,
which minimizes compatibility distance from AGP's current transitive version.

## Non-change

- no app `implementation` dependency is added;
- no APK runtime crypto provider is changed;
- no signing configuration is changed;
- no vulnerability allowlist or suppression is introduced;
- no AGP downgrade or speculative AGP preview upgrade is introduced.

## Acceptance

The slice is accepted only if:

1. Dependency Baseline resolves the constrained build graph successfully.
2. Dependency Review reports no newly introduced High/Critical vulnerability.
3. Android CI, lint and release/debug packaging stay green.
4. The submitted Gradle graph no longer resolves `bcprov-jdk18on:1.80.2` for the
   plugin classpath.
