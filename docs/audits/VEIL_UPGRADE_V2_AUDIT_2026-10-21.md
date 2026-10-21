# Veil Reader v2 proposal — compatibility and risk audit (2026-10-21)

**Status: RED / blocked for direct integration.** This branch starts from `reader/native-paper-rebuild-20261011`, does not change `main`, and stages only an isolated, unconnected Persian search utility plus focused tests. It does **not** install, replace, or enable the seven proposed production modules.

## Checked against actual repository

- Current application package/namespace: `com.veilreader.app` (`app/build.gradle.kts`); proposal uses `com.veilreader.*`, and no Hilt plugin or Hilt dependency is configured.
- Existing Room persistence is `app.data.db.VeilDatabase`, schema **3**, backed by `veil_reader.db`. Existing migrations are 1→2 and 2→3; old data includes books, highlights, bookmarks, collections, reading records and manga tables. Do NOT install a competing database definition against that same filename.
- Existing backup is `app.data.LibraryExport`, schema **5**, including current and older backup schemas (1–5), reading metadata, game state and local manga content. Replacement `veil_manifest_v2.json` is not format-compatible and drops state.
- Existing import (`ReadiumEngine` / `LocalLibraryRepository`) already computes SHA-256 and reuses duplicate fingerprints while maintaining a serialized durable write queue.
- Existing in-book search uses Readium search iterators (`ReaderPublicationSearch.kt`). The release search gate is disabled. An optional FTS index needs a separately gated architecture/feature and migration.
- Existing native paper-curl prototype (`NativeMaterialPageCurlView`) uses actual bitmap snapshots and Harism curl mesh. Proposed Compose gradient is only a visual overlay and would regress fidelity.
- Android build currently uses Readium 3.4.0, Room 2.8.5, Compose 1.12.1 and Kotlin/Gradle strict warnings (`allWarningsAsErrors`). Any new code must meet these contracts.

## P0 blockers in supplied proposal

1. **DB schema replacement/data loss:** `VeilReaderDatabase` schema 2 uses the same file name as current schema 3 but unrelated `books` columns and a conflicting migration 1→2. Use additive schema 3→4 (if needed), exported schema JSON, migration tests and existing DAO adapters; never rewrite 1→2.
2. **Restore is not atomic:** `restoreBackup` copies files into real directories with overwrite **before** `withTransaction`, so DB failure leaves mutations. `Failed` currently claims zero changes incorrectly. Stage and validate everything, promote with rollback journal/atomic swaps, and verify files after recovery.
3. **Current position overwritten by high-water mark:** `max(resolvedCurrentProg, resolvedFurthestProg)` merges the two positions into one and can incorrectly jump from rereading at 25% to 80%. Preserve both values independently; do not compare positions from different reading contexts as if interchangeable.
4. **Existing backup content lost:** New manifest omits per-book overrides, jump history, collections, bookmarks, reading sessions, game state, manga and settings despite describing more coverage. Only extend existing schema-5 backup after explicit coverage matrix.
5. **OPDS resumability corruption:** 206 `Content-Range` is not validated; `If-Range` uses `Last-Modified` or ETag without strong validator semantics; partial files keyed only by URL can mix changed content; 200 should restart. Do not delete/replace an existing final content-hash file before committing a new import.
6. **Credential transport:** redirect auth forwarding avoids cross-host credentials, but allows initial plain HTTP. Authenticated OPDS must reject non-HTTPS origins unless an explicitly separate local-only trust policy is designed; redirects must never downgrade credentials.
7. **Paper engine does not fold the actual page:** `onCommitNavigatorTurn(false)` happens before an overlay is rendered. The proposed effect cannot replace the native snapshot/mesh engine and should not be activated in production.
8. **Tests do not exercise implementation:** OPDS tests reimplement `isSameTrustedOrigin` and `verifySignature` inside the test class; DB transaction, restore rollback, progress/high-water and actual downloader logic are untested. An EPUB containing only `mimetype` is not a valid EPUB publication.

## P1 concerns

- `PersianTextNormalizer.normalize` does not remove punctuation, although its supplied unit test expects punctuation removal; it maps Persian `آ` to `ا`, which also disagrees with the supplied expected string. Mapping `ئ` to `ی` can corrupt words such as `مسئولیت` in normalized search. Keep this strictly search-only. Stage a safer normalizer, then evaluate on a representative Persian corpus.
- FTS raw HTML stripping with `Regex("<[^>]+>")` is not content extraction: can index scripts/styles, miss entities and badly locate search hits. Use safe XHTML/HTML parsing and exact locator anchoring via Readium; do not treat chapter-index/paragraph-index as an exact locator.
- The indexer can mark a book fully indexed after silently skipping unavailable resources. Long chapters can allocate large batches; use bounded text/chunk sizes and transactionally commit chunk rows with checkpoint advances. Propagate cancellation (don't turn it into `Result.failure`).
- ZIP extraction flattens entry paths to basenames, silently accepts traversal-shaped names and allows duplicate overwrite. Explicitly reject invalid paths and duplicates, enforce a small manifest cap and an overall extraction/CPU/storage budget; authenticate encrypted archives before promoting files.
- The encryption path uses GCM correctly as an authenticated primitive, but streaming restore must not trust extracted data until the final authentication tag is verified. PBKDF2 cost and wrong-password/tamper tests require security sign-off.
- `writeBookFileIntoZip` catches all failures and returns false, making a claimed FULL backup silently incomplete; failures and coroutine cancellation should not be hidden.
- Celestial mode is grouped by `author`, not the intended per-book stars and deliberate multi-book constellation progression; selection, pan bounds, collision handling and per-star screen-reader actions need work. Per-frame text measurement should be cached.
- `VeilReaderEngineAndTypography.kt` contains no working bilingual live typography panel; AUTO direction detection is declared, not implemented. Paged/scroll/slide/curl navigation contracts must remain independent.

## Safe staged integration sequence

1. Keep original Room, backups, native paper engine, and user data unchanged. Commit only additive helpers/experimental tests to a dedicated feature branch.
2. Verify Kotlin helper compile and deterministic search behavior. For any FTS rollout, design schema 3→4, validate Room exported schema and migrate real fixtures on Android.
3. Extend current schema-5 backup (do not replace) with optional password-wrapped mode only after authenticated staged extraction, rollback, and backward-compatibility tests. Keep LIGHT/FULL explicit, with user-visible coverage.
4. Add HTTP Range/ETag/Content-Range state-machine tests with a mock server and hostile-redirect cases before OPDS integration.
5. Reuse the existing Readium search and real native curl pipeline. Prototype celestial UI behind debug-only feature gate, verify gesture physics and accessibility with device/video evidence.
6. Run policy checks, full JVM tests, lint, debug APK build, connected Android migration and restore tests, then physical-device QA. No green or merge claims before evidence.

## Verification evidence in this branch

- `PersianFtsQueryNormalizer.kt` is stand-alone and not wired into the product path.
- A local `kotlinc` + `java -jar` smoke runner compiled that utility and passed four direct assertions on: Arabic/Persian characters, preserving `ئ`, quoting FTS operators, and punctuation-only input. **This is not a Gradle/JUnit/Android test result.**
- Four JUnit test methods are added for CI/Gradle to run. They test the real normalizer rather than duplicated test-only helper functions.
- No instrumented tests, full Gradle build, signed APK, database migration or backup restore have been executed or verified as part of this audit.

## Merge gate

**DO NOT MERGE** until a compatible additive persistence design and hardened restore, download and reader implementations pass their relevant device and regression gates.