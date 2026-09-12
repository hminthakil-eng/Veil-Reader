---
name: veil-world-class-app-development
description: Primary app-development orchestrator for Veil Reader. Use for substantial Android/Kotlin/Jetpack Compose feature work, architecture decisions, refactors, performance work, UI implementation, testing, or release-quality development. Applies vetted modern Android practices while preserving Veil Reader's actual architecture and dependencies.
---

# Veil World-Class App Development

This is the default engineering skill for substantial Veil Reader development. It combines the strongest reusable ideas from the public Android Agent Skills ecosystem with Veil Reader's own architecture and release requirements.

## First rule: inspect before prescribing

Before changing architecture, dependencies, state ownership, persistence, or build logic:

1. Read `README.md`.
2. Read the relevant section of `ENGINEERING_BLUEPRINT_1.0.md`.
3. Inspect the touched code and `app/build.gradle.kts`.
4. Preserve working project decisions unless there is evidence they are the source of a problem.

Do **not** introduce Hilt, Retrofit, a domain layer, multi-module architecture, a new navigation framework, or any other fashionable pattern merely because a generic Android guide recommends it. Veil Reader currently has a deliberately local/offline architecture; architecture follows product needs, not templates.

## Development loop

Use this loop for every non-trivial change:

### 1. Understand
- Identify the user-visible goal and current behavior.
- Trace the smallest relevant path through UI, state, persistence, and reader/navigation code.
- Identify invariants that must not regress: reading position, annotations, bookmarks, imported publications, backups, progression state, and offline operation.

### 2. Design
- Prefer the smallest coherent change that solves the problem.
- Keep state ownership explicit and use unidirectional data flow.
- Keep reusable Composables stateless when practical: state flows down, events flow up.
- Keep Android/framework concerns out of pure business logic when separation creates real value.
- Treat Room as the structured source of truth and DataStore as preference storage unless an existing subsystem clearly owns something else.
- Never serialize live Readium publication objects for process restoration.

### 3. Implement
- Follow existing project naming, patterns, and abstractions before inventing new ones.
- Prefer immutable UI state and lifecycle-aware Flow collection.
- Keep expensive work out of composition and the main thread.
- Use stable list keys and appropriate content types for lazy collections.
- Hoist or remember expensive calculations only when they are actually recomposition-sensitive.
- Preserve EPUB/PDF behavioral differences where their capabilities differ.
- Avoid broad rewrites unless they are necessary to remove a demonstrated architectural problem.

### 4. Accessibility
For every changed interactive screen:
- interactive targets should be at least 48dp where applicable;
- actionable controls need useful semantics/content descriptions;
- decorative visuals should not pollute TalkBack output;
- heading, state, focus order, and grouped semantics should be intentional;
- text scaling and large-font layouts must remain usable;
- important color relationships must meet accessible contrast;
- motion should degrade gracefully for users who reduce animation.

### 5. Performance: measure before optimizing
Never call a change a performance improvement based only on intuition, compiler skippability, or debug-emulator feel.

Use the sequence **Measure → Diagnose → Fix → Verify**:
- establish a baseline first;
- identify the concrete cause (recomposition, layout/draw work, allocation, I/O, startup, database work, etc.);
- make one targeted fix at a time;
- repeat the same measurement after the fix.

For release-level Compose performance work, prefer release/R8 measurements and physical-device Macrobenchmark evidence when practical. Skippability is a diagnostic, not the KPI; user-visible frame/startup metrics are the goal.

### 6. Data durability
For Room entities, migrations, backup/restore, imports, or other storage-sensitive changes:
- inspect committed schemas and all relevant migration paths;
- never use destructive migration as a shortcut unless data loss is explicitly authorized;
- preserve transactional restore behavior;
- validate archives and untrusted file input defensively;
- keep imported publications private unless the user explicitly exports them.

### 7. Testing
Choose tests based on risk, not ceremony:
- pure logic → unit test;
- ViewModel/state transformation → unit test;
- DAO/migration/backup/restore → Room/instrumented integration test;
- critical interaction or semantics → Compose/instrumented UI test where useful;
- visual behavior → screenshot/golden testing only if the project deliberately adopts and maintains it.

Do not add a new testing framework merely because a generic skill recommends one. Reuse the existing stack unless the new tool solves a real gap.

## Repository verification

Baseline checks:

```sh
sh tools/test-policy.sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Storage-, Room-, migration-, or backup-sensitive work also requires:

```sh
gradle :app:connectedDebugAndroidTest
```

Inspect GitHub Actions after pushing when available. If a required check cannot be run, state exactly what remains unverified.

## Reading-first product constraints

- Never interrupt active reading with progression celebrations, forced streak messages, quests, ads, or unrelated prompts.
- Progression systems live around the book, not on top of the text.
- Background/idle time must not count as engaged reading.
- Do not lock basic reading, bookmarks, themes, or accessibility behind monetization.
- Do not add cloud, telemetry, ads, external AI processing, or account requirements without an explicit product decision.

## Definition of done

A feature is done only when:
- the user goal works end-to-end;
- reading continuity and stored data are preserved;
- accessibility implications were checked;
- relevant tests pass;
- performance claims are supported by evidence when performance was part of the task;
- the change does not silently expand permissions, privacy exposure, or dependency surface;
- relevant items in `WORLD_CLASS_RELEASE_GATES.md` remain satisfied.

## External practice sources

This skill intentionally adapts concepts from the public `new-silvermoon/awesome-android-agent-skills` project (Apache-2.0) and the measurement-first performance methodology of `skydoves/compose-performance-skills` (Apache-2.0). Their generic dependency/version prescriptions are not copied blindly; Veil Reader's checked-in toolchain and architecture remain authoritative.
