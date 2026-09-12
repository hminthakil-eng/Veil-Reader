---
name: veil-efficient-task-runner
description: Default execution workflow for Veil Reader development tasks. Use when implementing features, fixing issues, refactoring, updating tests, or making repository changes. Optimizes for fast context discovery, minimal safe diffs, autonomous execution, evidence-based verification, and concise completion reporting.
---

# Veil Efficient Task Runner

Use this skill to make development faster without trading away correctness.

## Operating mode

Work autonomously once the task is clear. Do not stop for routine confirmations, stylistic choices, or implementation details that can be resolved from the repository. Ask only when a genuinely product-defining ambiguity, destructive action, missing credential, or irreversible choice blocks safe progress.

## 1. Classify before exploring

Choose the lightest workflow that fits:

- **Small** — localized fix, copy change, simple test, one-file refactor: inspect the target and direct dependencies, change, verify.
- **Medium** — feature or behavior spanning a few files: trace one end-to-end path, identify invariants, implement in coherent slices, verify focused + baseline checks.
- **Large/risky** — storage, migrations, backup/restore, reader lifecycle, architecture, security/privacy, release: read the relevant blueprint/release docs first and use specialist skills.

Do not perform broad repository archaeology for a small task.

## 2. Discover once, then act

Before editing:

1. Read the closest relevant code and tests.
2. Reuse existing project patterns and naming.
3. Identify the smallest dependency chain needed to make the change safely.
4. Record the invariants that must not regress.

Avoid repeatedly reopening the same files or rediscovering facts already established in the task.

## 3. Prefer minimal coherent diffs

- Fix root causes, not symptoms.
- Change the fewest concepts necessary, not necessarily the fewest lines.
- Avoid unrelated cleanup inside feature/fix commits.
- Do not add a new abstraction, dependency, module, framework, or architectural layer unless it solves a demonstrated problem.
- Preserve public behavior unless the task explicitly changes it.
- Keep generated or formatting-only churn out of functional changes when practical.

## 4. Build in vertical slices

For user-visible work, prefer an end-to-end thin slice over large disconnected layers. A slice should connect UI → state → persistence/domain behavior → test where applicable before expanding breadth.

## 5. Verification ladder

Run the cheapest useful check first, then broaden based on risk:

1. compile/static validation for the touched area;
2. focused unit/integration tests;
3. repository policy + lint/build baseline;
4. instrumented/device checks for storage, migration, reader interaction, or UI behavior when relevant;
5. GitHub CI status after pushing when available.

Baseline repository checks:

```sh
sh tools/test-policy.sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

For Room/migration/backup/restore/storage-sensitive changes:

```sh
gradle :app:connectedDebugAndroidTest
```

Never claim a check passed if it was not run. State unverified items precisely.

## 6. Specialist routing

Use these only when the task matches:

- Android implementation/architecture → `veil-android-engineer`
- substantial feature/refactor → `veil-world-class-app-development`
- bug/crash/unexpected behavior → `veil-systematic-debugger`
- PR/diff/final quality check → `veil-code-reviewer`
- Compose UI quality → `veil-compose-quality-reviewer`
- privacy/security/file import/backup risk → `veil-security-privacy-reviewer`
- release readiness → `veil-release-guardian`
- product/UX direction → `veil-reader-product-designer`

Do not load every specialist for every task.

## 7. Definition of efficient completion

A task is complete when:

- the requested behavior works end-to-end;
- the diff is focused and understandable;
- relevant regression coverage exists or the reason it does not is clear;
- the appropriate verification ladder has been run;
- no known critical issue is hidden behind a successful build;
- the final report states what changed, what was verified, and any real remaining risk in concise language.
