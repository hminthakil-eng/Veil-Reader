---
name: veil-official-android-tooling
description: Evaluate and safely adopt Google's official Android agent skills, Android CLI and specialist Compose performance guidance for the actual Veil Reader Kotlin/AGP build without bulk-installer risks or architecture churn.
---

# Official Android Skill Integration

Use for Android platform upgrades, build performance/security, profiling, adaptive Compose, design-system migration, test infrastructure, edge-to-edge, or device tooling.

## Authority and fit
- Primary external skill catalog: https://github.com/android/skills (Apache-2.0), with official developer documentation at https://developer.android.com/tools/agents/android-skills .
- Adjacent specialized library: https://github.com/skydoves/compose-performance-skills (inspect its license and exact version before adopting).
- Existing Veil Reader Gradle toolchain is Android Gradle Plugin 9.4.0, compile/target SDK 37, Kotlin Compose plugin 2.3.21, minSdk 26, Readium 3.4.0. Inspect the live branch to verify these values before proposing any migration.
- Existing project skills take priority over generic examples that would introduce unneeded Hilt, Retrofit, a new navigation framework, broad modularization, telemetry or a cloud backend.

## Prioritized official skill candidates
1. android-profiler: frame, memory and lifecycle traces for Paper and TTS.
2. r8-analyzer: source-of-truth release APK/R8 verification and profile packaging.
3. testing-setup: a bounded test gap (not replacement of the current working suite).
4. android-intent-security + android-permissions-security: Translate and system-provider interactions; least privilege.
5. Compose adaptive/theming and edge-to-edge: only for actual observed layouts/visual-system gaps.
6. android-cli: optional managed installation and device actions on an authorized development machine.
Avoid automatic installation of unrelated TV, Wear, XR, camera or billing skills unless feature scope changes.

## Installation protocol
Read repository LICENSE, target SKILL.md and every referenced file/command before adoption. Select pinned, reviewed versions and verify installer behavior; never run an unsupervised 'latest' installer or bulk install all skills by default.
Use the official Android CLI instructions to install selected skills for explicitly named project agents, on a disposable branch; capture generated paths and diff. Do not claim CLI installation occurred unless it actually did.
For any copied Apache-2.0 content preserve required attribution and license/notice. Prefer a source-linked selection record when a skill depends on unbundled reference files.

## Acceptance
Record exact source commit/tag, license, scope, paths, lint/CI and operator test evidence; reject duplicate or contradictory skills. Do not call source presence a runtime feature or a functioning plugin connection.
