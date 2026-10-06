# R0.05 — Compose 1.12.1 Qualification

Date: 2026-10-05
Branch: `reforge/compose-1.12.1-qualification-v1`
Parent: `grand-forge/arena-app-hardening-v1`

## Scope

This qualification changes only the stable Compose UI/Foundation/tooling/test line:

- `androidx.compose.ui:ui` 1.10.6 → 1.12.1
- `androidx.compose.ui:ui-tooling-preview` 1.10.6 → 1.12.1
- `androidx.compose.foundation:foundation` 1.10.6 → 1.12.1
- `androidx.compose.ui:ui-tooling` 1.10.6 → 1.12.1
- `androidx.compose.ui:ui-test-manifest` 1.10.6 → 1.12.1
- `androidx.compose.ui:ui-test-junit4` 1.10.6 → 1.12.1

Material3 remains 1.4.0.
Material3 Adaptive remains 1.3.0.
Kotlin Compose plugin remains 2.3.21.
Readium remains 3.4.0.
No production feature behavior is intentionally changed.

## Rejection conditions

Reject this upgrade if it causes:
- build/test/lint regression;
- Reader gesture/input regression;
- Grayfog rendering drift that materially degrades composition;
- screenshot/native-render instability;
- accessibility semantics regression;
- performance regression;
- new dependency conflict with Readium or AndroidX.

## Acceptance evidence required

- Android CI green
- Storage Instrumentation green
- Performance Benchmarks green
- current Grayfog render artifact available
- no source changes required solely to paper over a behavioral regression without understanding the cause

Until these gates pass, this branch is qualification-only and must not be folded into Grand Forge.
