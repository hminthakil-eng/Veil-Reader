# W4 — Readium publication search foundation (2026-10-10)

**Scope:** internal-only, no UI, no release, no migration. This is not a finished Kindle-grade search feature.

## Reuse/provenance

Reuses the existing Readium Kotlin Toolkit 3.4.0 `Publication.search()` and
`SearchIterator` (BSD-3-Clause). Readium's built-in EPUB `SearchService`
provides language-aware text matching and returns semantic `Locator` with
before/highlight/after context. No Moon+, Kindle or book text is copied,
indexed into a separate store, uploaded, or committed.

Readium upstream (release tag `3.4.0`):
`readium/shared/src/main/java/org/readium/r2/shared/publication/services/search/SearchService.kt`

## Work in this change

- `ReaderPublicationSearch.kt`: query validation, unsupported/error results,
  one closable iterator per query; retain excess matches between UI pages.
- Bounded `nextPage` with 1–100 hits per call and max 64 Readium batches per
  invocation; yield partial results rather than monopolize a slow device.
- Reject per-resource match floods over 5,000 locators rather than silently
  truncate or allocate an unbounded match queue.
- Protect closed/stale session results, resource ownership and overlapping
  page reads; do not expose publication text in error messages.
- Eight unit tests for sanitized queries, unsupported sources, pagination,
  empty resources, oversized matching, errors, closing and cancellation.

## Integration gate still missing

Do not promote as complete until a reader can:
1. Open search from one Reader gesture with accessible field, cancel and errors.
2. See paginated search context snippets with highlighted term.
3. Navigate to exact semantic locator, saving Previous Location before jump.
4. Change/exit a query without stale results or leaked cursors.
5. Use Persian/Arabic RTL, 200% fonts, TalkBack and offline with no false page credit.
6. Search a large EPUB on real S24 Ultra without ANR, UI jank or memory spikes.
7. Receive truthful unsupported states for PDF and other formats until ready.

Paper P0 (#442) and durability P0 (#451) remain separate blockers. Keep draft
until exact-head Android CI, review and device acceptance. No user-facing
search or production release is claimed here.
