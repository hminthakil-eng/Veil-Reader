# Phase 1A — Data Foundation Execution Contract

This phase changes infrastructure before behavior.

## Goal
Introduce a production-grade local persistence layer without destabilizing the green 0.6 reader.

## Rules
1. The existing reader/library UI remains behaviorally unchanged until migration tests prove parity.
2. New Room/DataStore code lands behind repository boundaries before becoming the source of truth.
3. Legacy SharedPreferences data remains readable during the migration window.
4. Every schema change requires an exported schema and migration test.
5. Imported publication files stay in app-private storage; Room stores metadata and file references, not book bytes.
6. No AI, cloud, manga, audio, or social work enters this phase.

## Slice 1A.1 — schema + tooling
- Room 2.8.x stable line, KSP compiler, exported schemas.
- Preferences DataStore stable line for small settings.
- Entities/DAOs for books, annotations, bookmarks, collections and collection membership.
- Reading-session entity reserved for trustworthy analytics.
- Pure mappers between database rows and current domain models.
- Database singleton/factory isolated from Compose.

## Slice 1A.2 — legacy import
- Parse current `veil_library_v1` JSON/SharedPreferences state.
- Import books, highlights, bookmarks and collections idempotently.
- Preserve ids, locators, timestamps, file URIs and progress.
- Record migration completion only after a successful Room transaction.
- Keep legacy preferences for rollback until a later release.

## Slice 1A.3 — repository cutover
- New repository reads Room Flows.
- Existing write operations become Room transactions.
- UI signatures remain stable where practical.
- Backup/export reads the new source of truth.

## Acceptance gate
- Existing 0.6 data fixtures migrate without loss.
- Re-running migration produces no duplicates.
- Unit tests, migration tests, lint and debug APK build all pass.
- Manual smoke confirms import/open/progress/highlight/bookmark/backup on a device before deleting legacy storage.
