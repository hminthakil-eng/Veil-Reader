---
name: veil-android-engineer
description: Implement or review Android/Kotlin changes in Veil Reader while preserving offline-first behavior, Readium integration, Room/DataStore durability, lifecycle correctness, and the existing reading-first product architecture.
---

# Veil Android Engineer

Use this skill for implementation, refactoring, debugging, architecture changes, persistence work, reader behavior, or Android-specific reviews in this repository.

## Workflow

1. Read `README.md` and the relevant sections of `ENGINEERING_BLUEPRINT_1.0.md` before making architectural changes.
2. Treat reading as the primary product experience. Do not let progression systems, overlays, animations, or background work interfere with reading performance or page interaction.
3. Preserve the existing ownership boundaries:
   - Room is the structured source of truth.
   - DataStore owns reader preferences.
   - Imported publications remain app-private unless the user explicitly exports them.
   - Live Readium publication objects are not serialized for restoration.
4. For storage/schema work, inspect existing entities, DAOs, migrations, backup/restore logic, and committed Room schemas before editing.
5. For reader work, preserve durable reading position, configuration/process restoration, search, annotations, bookmarks, and EPUB/PDF differences.
6. Prefer small, testable changes over broad rewrites. Reuse existing abstractions and naming unless there is a demonstrated architectural problem.
7. Keep lifecycle behavior explicit. Background or idle time must never be counted as engaged reading.
8. Do not introduce cloud, account, advertising, telemetry, external AI processing, or paid-service dependencies unless the task explicitly requires a product-level change.

## Verification

Run or reason against the repository's established checks:

```sh
sh tools/test-policy.sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

For Room, migration, backup, restore, or storage-sensitive changes, also require:

```sh
gradle :app:connectedDebugAndroidTest
```

If you cannot run a required check, state exactly what remains unverified.

## Definition of done

A change is not done until it preserves reading continuity, data durability, offline behavior, and the relevant release gates; includes focused tests when behavior changes; and does not silently broaden the app's privacy or dependency surface.
