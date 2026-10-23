# Room V4: opt-in local sync journal (draft)

This change **does not provide cloud sync**. It prepares persistent local state without affecting
the reader until an authenticated, consented transport is implemented. V3 books, progress,
highlights, bookmarks, manga tables, and saved locator/checkpoint logic are preserved.

- Migration \`3→4\` only adds six tables and three indexes.
- \`sync_preferences\` has no row after migration: capture defaults to OFF.
- After explicit consent, \`configureLocalSyncCapture(true, confirmedUserConsent = true)\`
  enables atomic capture of progress, bookmarks, highlights and deletions on existing write paths.
  The API is internal and has no UI entry point in this branch.
- Outbox events contain note/quote/locator text when explicitly enabled. Do not put sync payloads
  in logs and do not enable without explaining privacy.
- Deletes persist a tombstone with deleteRevision=0 (unassigned). Never purge until a future
  authenticated server assigns a revision and every active device acknowledges that same revision.
- Backup restore clears pending sync events, tombstones, acknowledgements, devices and conflicts and
  turns capture OFF. This prevents stale pre-restore events replaying as authoritative changes.
- Local \`ReaderProgressWriteOrder\` ordering remains unchanged and MUST NOT be used as a
  multi-device/server revision.
- Fingerprint is metadata only, not proof of ownership. A future remote transport MUST enforce
  account authentication, publication identity, consent, encryption-in-transit and conflict reviews.
- No copying of BookOrbit (AGPL) source code, no UI changes, no merge into main.

## Gates before release
1. Android Gradle/KSP compile + tracked Room schema version 4 JSON (compiler-generated, never guessed).
2. \`MigrationTestHelper\` run 3→4 and validation with real v3 seeded rows.
3. Offline device QA: crash during local save, retry, clock skew, reimport/restore, volume/page turns.
4. Authenticated sync service, safe incoming apply, UI for opt-in/conflicts, retention and account removal.
