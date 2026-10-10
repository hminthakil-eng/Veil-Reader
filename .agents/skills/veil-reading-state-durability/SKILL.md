---
name: veil-reading-state-durability
description: Verify crash-durable reading positions, bookmarks, notes, highlights, Room migrations, imports, backup/restore, and process-lifecycle edge cases in Veil Reader.
---

# Reading-State Durability

## Invariants
Room owns structured persisted records; DataStore owns preferences; Readium locators must be serialized through supported persisted forms, never live publication objects.
A displayed success confirmation follows a committed durable write, not a queued coroutine.
Reader session identity and retired-session ownership must reject stale callbacks; duplicate locator retries must not overwrite the previously saved stable origin.
Page-turn progress is credited exactly once, including the first completed page of a session. Idle/background time cannot masquerade as engaged reading.

## Fault injection
Test pause/close/rotation, OS process kill, duplicate/out-of-order callbacks, killed or cancelled save jobs, SQLite failure, insufficient storage, interrupted import, media missing, app update and schema migration.
For the current P0 rollback concern, preserve original persisted locator/strength when a same-position retry fails; test durability-strength upgrades independently from failed-write retries.
Run backups through full export -> validate -> restore -> reopen round trips using authentic versioned migrations and bounded malicious ZIP cases.
Test enormous books, malformed EPUB/PDF and concurrent annotation edit/delete, including visibility after cold restart.

## Proof
Record starting and final locator, committed database state, recovery behavior, fault trigger and exact-head test results. Include no private book text in logs.
Unit/Room regression tests and connected physical-device checks are both required for a durability-sensitive release.
Block release on any reproducible loss, double-counting, stale writer, corrupt import or false save acknowledgement.
