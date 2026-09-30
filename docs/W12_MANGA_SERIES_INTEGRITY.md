# W12 — Manga Series Integrity + Mirror Accessibility Hardening

Status: **source-complete for this slice; build/device verification intentionally deferred**

Base: `alpha/w11-mirror-manga-integration`

Branch: `alpha/w12-manga-series-integrity-v1`

## Why this wave exists

W11 made local Manga a first-class offline reader, but two product-integrity gaps remained:

1. a Manga work could manage chapter metadata but not edit its whole-work identity from the Manga surface;
2. future work grouping needed a reversible merge/split contract before Veil could safely offer any automatic grouping.

W12 closes the first gap and builds the safety boundary for the second. It also fixes an Arrodes Mirror accessibility leak discovered during source review.

## Shipped in this slice

### Canonical whole-work metadata editing

The Manga Hub can now edit the canonical `Book` identity rather than creating a second Manga catalog:

- title;
- creator / author;
- series name;
- series position / volume;
- language tag.

Collections are preserved. Chapter rows, source files, progress and cache ownership remain unchanged.

Manga identity edits use `LocalLibraryRepository.editMetadataDurably()`. The in-memory library is updated only after the Room transaction succeeds, so a rejected write cannot expose a false identity in UI state.

An instrumentation regression test forces SQLite to reject the metadata update and verifies that both Room and the in-memory catalog retain the previous identity.

### Localized numeric presentation

Manga chapter and volume numbers now use Veil's shared locale-aware number formatter. Persian/Arabic-script locales no longer fall back to raw Latin-digit `Double.toString()` presentation.

### Grayfog Manga edit chambers

Generic `AlertDialog` surfaces were removed from Manga chapter editing, series editing and chapter deletion. These flows now use the same restrained Archive/Grayfog visual language as the rest of Veil:

- archive surface;
- brass/error boundary;
- Grayfog ornament;
- safe-drawing insets;
- IME-aware layout;
- minimum touch target sizing;
- explicit destructive styling.

### Arrodes Mirror accessibility hardening

Unrevealed saved memory text is no longer kept in the accessibility tree at near-zero visual alpha. During the ash transition, the mirror exposes an explicit transition description instead of falsely advertising the source passage as already openable.

This prevents TalkBack from revealing content before the visual ritual does.

## Merge/split safety contract

W12 does **not** expose a merge button yet.

`MangaWorkMergePlanner` is a pure, side-effect-free preflight contract. It enforces:

- one canonical target Book;
- distinct Book identities;
- distinct chapter identities;
- chapter ownership consistency;
- contiguous persisted chapter order;
- exact local-CBZ duplicate detection by SHA-256-backed chapter key;
- rejection of ambiguous semantic chapter collisions;
- no guessed merge when chapter identity is insufficient;
- deterministic target reading order;
- no transplant of derived cache across canonical Manga identities;
- a split-receipt seed containing source Book metadata and original chapter metadata/order;
- source Book/archive retention until the final merge has been verified.

`MangaLocalImportCoordinator.preflightLocalMerge()` connects that contract to real persisted data. Before returning a ready plan it verifies, for every participating chapter:

1. exactly one local CBZ source exists;
2. the original app-private archive still exists;
3. the durable chapter key contains a valid SHA-256 fingerprint;
4. the current archive SHA-256 still matches that fingerprint.

The preflight mutates neither Room nor the filesystem.

## Why actual merge is still gated

A chapter cache identity is derived from the canonical Manga/Book identity. Therefore an executor must not rename or copy a source work's cache directory into a target work and treat it as authoritative.

The safe executor model for the next wave is **non-destructive copy + hide**, not delete-and-move:

1. retain every source Book, source CBZ, history and source chapter until verification completes;
2. rebuild merged target chapters from verified source CBZ archives under the target canonical identity;
3. persist a durable merge receipt before hiding source works;
4. hide source works through explicit merge membership rather than deleting them;
5. split by removing target copies and restoring visibility of untouched source works;
6. include merge membership/receipts in backup/restore before the feature is exposed.

That requires a deliberate Room schema wave plus backup-schema support. W12 stops before that boundary instead of introducing an irreversible half-merge.

## Verification performed without building

Source/static verification currently confirms:

- English strings: 1198;
- Persian strings: 1198;
- no duplicate string keys in either locale;
- no one-sided English/Persian string keys;
- no undefined `R.string` references in touched Kotlin files;
- no merge-conflict markers in touched files;
- Manga Hub contains no generic `AlertDialog` usage;
- merge planner explicitly forbids early source deletion and requires derived-cache rebuild for moved chapters.

Unit and instrumentation tests were added for:

- deterministic merge planning;
- exact archive deduplication;
- semantic collision rejection;
- duplicate Book/chapter identity rejection;
- chapter-owner mismatch rejection;
- missing-identity conservative behavior;
- real archive-integrity preflight;
- corrupted-source preflight rejection without mutation;
- durable metadata failure semantics.

No compile, APK, emulator, instrumentation run, or device-green claim is made in W12 yet.

## Next gate

The next implementation wave should add the durable non-destructive merge receipt/membership schema, migration and backup representation **before** any merge UI or automatic grouping is enabled.
