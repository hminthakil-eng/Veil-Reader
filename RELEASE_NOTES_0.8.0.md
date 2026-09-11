# Veil Reader 0.8.0 — Release Candidate Notes

## What changed

0.8.0 turns the Grand Library into a substantially stronger local book-management system while preserving the Room v1 database schema.

- Real EPUB/PDF cover thumbnails are extracted through Readium and cached in app-private storage.
- Book metadata now exposes series name/index and a primary language tag when available.
- Books can belong to multiple collections; search/filter/editing understand every membership.
- Series sorting and series metadata editing are available in the Grand Library.
- Imported publications receive a SHA-256 content fingerprint.
- Duplicate imports resolve to the existing book and discard only the transient second publication/cover files.
- Existing/restored books incrementally regenerate missing fingerprints and cover cache.
- Backup schema 2 now preserves richer metadata and all collection memberships while remaining compatible with older schema-2 and schema-1 archives.
- Cover paths and fingerprints are treated as derived cache and regenerated after restore.

This release also includes the earlier 0.7/Phase 2 foundation: Room + DataStore runtime storage, transactional restore, lifecycle-aware reading sessions, process/configuration route restoration, Grand Library visual polish, and calmer Reader chrome.

## Verification gates

Every merge candidate must pass:

1. Reading-policy regression checks.
2. Kotlin/JVM unit tests.
3. Committed Room schema-drift verification.
4. Android lint.
5. Debug APK assembly and artifact upload.
6. Android emulator storage instrumentation for data-layer changes, including migration and backup/restore.

## Data compatibility

- Room schema remains version 1 in 0.8.0; the fields activated here were reserved in the existing schema.
- Legacy 0.6 SharedPreferences migration remains supported.
- Backup schema 1 and schema 2 remain restorable.
- Derived cover thumbnails and content fingerprints are not authoritative backup data.

## Still required before production 1.0

- Physical Android-device QA across representative EPUB/PDF files.
- TalkBack, large-text and accessibility sign-off.
- Tablet/foldable and rotation/process-death testing on physical devices.
- Release signing, shrinking/obfuscation verification and Play internal testing.
- Large-library performance/benchmark work.
- Additional library bulk-management and removal/recovery workflows.

The current backup ZIP can contain private publications, notes and reading history and is not encrypted. Keep exported archives in a trusted location.
