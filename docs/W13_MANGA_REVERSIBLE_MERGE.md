# W13 — Reversible Local Manga Merge

Status: **source-complete for this wave; build/device verification intentionally deferred**

Base: `alpha/w12-manga-series-integrity-v1`

Branch: `alpha/w13-manga-reversible-merge-v1`

## Mission

Turn W12's side-effect-free merge preflight into a real local Manga merge that remains reversible after:

- app restart;
- reading the merged target;
- backup and restore;
- deletion of the merged target;
- exact-source deduplication;
- failure during archive copy, extraction, persistence or restore.

No source work is sacrificed to make the Library look clean.

## Product model: copy + hide, never delete + move

An active merge has one visible canonical target Book and one or more intact source Books.

Source Books remain in Room with:

- their original Book ids;
- original CBZ archives;
- original chapter rows;
- Manga progress;
- history and library metadata.

They are hidden only by the user-facing Library projection while an active merge membership exists.

`BookDao.observeAll()` continues to expose all persisted Books for internal durability work.

`BookDao.observeVisible()` filters active merge sources for ordinary product surfaces.

`LocalLibraryRepository` now uses the visible projection, while `snapshot()` still reads the full catalog. Therefore backup never silently omits hidden source works.

## Room schema v4

W13 raises `VeilDatabase` from schema 3 to 4 and adds four receipt tables.

### manga_work_merges

Owns one active merge and stores:

- target Book id;
- receipt version;
- number of target chapters that existed before merge;
- target library progress before merge;
- target finished state before merge;
- target last-opened timestamp before merge;
- exact pre-merge Manga progress row when one existed.

A target Book is protected by `ON DELETE RESTRICT` while the receipt exists.

### manga_merge_members

Stores every intact hidden source Book and deterministic source order.

A source Book can belong to only one active merge and is protected by `ON DELETE RESTRICT`.

### manga_merge_original_chapters

Stores exact identity evidence for every target chapter that existed before merge:

- reading order;
- chapter id;
- target Book id;
- local CBZ chapter key.

Split validates this evidence before removing any merged copy.

### manga_merge_chapters

Maps every source chapter to the target chapter representing it.

The row records whether the target chapter was:

- `REBUILD_FROM_SOURCE_ARCHIVE`; or
- `DEDUPLICATE_EXACT_ARCHIVE`.

Composite foreign keys bind:

- each mapping to its actual merge target;
- each mapping to a real merge member;
- source chapter to source Book;
- target chapter to target Book.

The database therefore rejects ownership combinations that are impossible according to the merge contract.

## Merge planning hardening

`MangaWorkMergePlanner` now projects target chapter ids deterministically before execution.

The projected identity uses the same canonical Manga id, chapter anchor, cache layout and name-based UUID rule as the real importer.

Additional fail-closed rules include:

- projected target ids must be unique;
- exact file equality does not override contradictory chapter metadata;
- two source works sharing the same exact CBZ map to the same projected target copy;
- semantic collisions are rejected rather than guessed;
- missing stable target identity rejects the plan.

## Preflight

`MangaLocalImportCoordinator.preflightLocalMerge()` remains side-effect free.

Before a plan is returned, every participant must:

- be a local COMIC Book;
- not already participate in another active merge in any role;
- have a complete contiguous chapter topology;
- have exactly one local CBZ source for each chapter;
- have a valid SHA-256-backed chapter key;
- still have the original app-private CBZ;
- pass a fresh SHA-256 verification.

## Execution

`executeLocalMerge()` performs another integrity pass immediately before mutation.

For every chapter that requires a target copy:

1. resolve the intact source Book and chapter;
2. re-check source ownership/order;
3. re-hash the source CBZ to close the preflight/execution race;
4. derive the target canonical chapter/cache identity;
5. require it to match the planner's projected target id;
6. make a verified app-private target archive copy;
7. rebuild derived page cache under the target canonical Manga identity;
8. stage the resulting chapter and manifest.

The source Book, source chapter, source progress and source archive are never changed.

After every required copy is prepared, one Room transaction publishes:

- target chapter copies;
- target local source mappings;
- offline manifests;
- pre-merge target progress receipt;
- original target chapter evidence;
- source memberships;
- every source-to-target chapter mapping.

Only that receipt makes source Books disappear from the normal Library view.

## Compensation and filesystem integrity

A target archive copy is verified by length and SHA-256 before commit.

If extraction fails after the archive copy was made but before it enters the prepared set, that archive and its cache are removed immediately.

If merge fails before the Room commit, all prepared cache/archive copies are removed.

If a failure occurs after the receipt transaction, W13 invokes the same split path as compensating rollback.

No failure path intentionally deletes the source CBZ.

## Split

`splitLocalMerge()` fails closed before deleting anything.

It verifies:

- original-target evidence count and contiguous order;
- exact original target chapter ids and local chapter keys;
- pre-merge progress still points inside the original target boundary;
- source membership order;
- every intact source chapter has exactly one receipt mapping;
- every source mapping still owns the expected source chapter/order;
- every target mapping still owns the expected target chapter/order;
- source and target mapping chapter keys still identify the same local archive;
- every removable copy lives after the original target boundary.

Only after all validation succeeds does one transaction:

- remove the merge receipt;
- remove target chapters created by the merge;
- restore the exact pre-merge Manga progress row;
- restore the exact pre-merge Book progress / finished / last-opened summary.

Filesystem cleanup then removes only the target copied archives and their derived caches.

Source Books become visible automatically because their merge-membership rows no longer exist.

## Structural lock while merged

Chapter topology is immutable while a Book participates in an active merge.

These operations are rejected until split:

- append chapter;
- move chapter;
- delete chapter.

Identity-preserving metadata edits may remain possible because chapter ids and topology do not change.

## Permanent deletion

Direct database deletion of either an active target or source is blocked by foreign-key `RESTRICT`.

The Manga deletion coordinator treats target deletion as:

1. verified split;
2. normal target deletion.

The source Book survives intact and becomes visible.

A hidden source cannot be permanently deleted until the merge is split.

## Backup schema 6

Local backup moves from schema 5 to 6.

Schema 6 adds `mangaMerges` containing:

- merge id / target;
- original target chapter count;
- exact pre-merge target progress;
- ordered source membership;
- original target chapter identity evidence;
- every source-to-target chapter mapping and disposition.

The physical publication/archive backup remains source-authoritative. Hidden sources and merged target copies are both included because the backup snapshot reads the full Book catalog.

### Backup is fail-closed

Veil refuses to write a merge receipt if current Room truth disagrees with it.

Before serialization it verifies:

- source membership is non-empty and contiguous;
- original-target evidence is complete and matches live chapter keys;
- every intact source chapter appears exactly once;
- mapping Book ownership/order is correct;
- source and target mappings share the same chapter key.

A corrupt active merge is therefore not silently exported as a valid backup.

### Restore

Restore rebuilds all Manga Books and local chapters from CBZ archives first.

Only then does it reconstruct merge receipts by resolving Book id + reading order back to the newly rebuilt chapter rows.

Restore verifies:

- no Book participates in more than one merge in any role;
- target/source Books are COMIC;
- original-target evidence matches rebuilt target chapter keys;
- every source chapter is represented;
- source and target mapping chapter keys agree;
- removable copies never overlap the original target boundary.

Only a verified receipt is committed.

### Restore rollback

Before replacing the Library, current merge receipts are captured.

If restore fails:

1. incoming merge receipts are discarded;
2. the previous Library is restored;
3. Manga files/cache are rebuilt;
4. previous merge receipts are restored;
5. preferences are restored.

An existing hidden-source state is therefore not forgotten by a failed restore.

## Verification coverage added

W13 source includes tests for:

- Room 1 -> 4 migration;
- merge table creation;
- target/source deletion restrictions;
- receipt cascade after explicit merge removal;
- deterministic target identity projection;
- exact archive metadata contradiction rejection;
- multi-source exact archive deduplication;
- archive-integrity preflight;
- merge -> read copied chapter -> split round-trip;
- exact target progress restoration;
- source progress preservation;
- source archive preservation;
- merged-target deletion splitting first;
- schema-6 backup -> split -> restore active merge -> split again.

## Current verification status

Static source audit currently confirms:

- W13 is based exactly on W12 and is only ahead, not behind;
- Room entity and migration SQL columns/indexes/foreign keys are aligned;
- all touched Kotlin files have balanced structural braces;
- no merge-conflict markers exist in touched W13 files;
- all merge mapping constructors bind an explicit target Book;
- all merge receipt constructors include the original target chapter boundary;
- backup schema is 6;
- database schema is 4.

No Gradle compile, APK build, emulator run, instrumentation execution or physical-device GREEN claim is made yet.

## Deliberate product gate

W13 still does **not** expose automatic grouping or a user-facing Merge button.

The next gate is compile + Room migration validation + unit/instrumentation execution + device smoke verification. Only after that evidence is GREEN should the UI expose explicit merge/split controls.
