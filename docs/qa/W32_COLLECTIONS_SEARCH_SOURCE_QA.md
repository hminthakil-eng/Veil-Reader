# W32 — Collections + Search Source QA

Date: 2026-09-30
Branch: `alpha/w32-collections-search-rebuild-v1`
Parent: `alpha/w31-hidden-archive-rebuild-v1`

## Canonical drift

- W32 is ahead of W31 and zero commits behind.
- Search remains local/offline and dependency-free.

## Retrieval behavior

- Existing Persian/Arabic/Latin normalization preserved.
- Search document explicitly models title, author, series, language and collections.
- Default queried results are relevance-first with recency then stable ID tie-break.
- Explicit Title / Author / Progress / Archive Depth / Series sorts remain authoritative.
- Blank query retains recency ordering with stable ID tie-break.

## Relevance contract

- title > author > series > collection > language;
- within a field: exact > prefix > contains;
- no match returns null rather than an artificial score.

## Tests

Added coverage proving:
- exact/prefix/contains ordering;
- field-priority ordering;
- title contains outranks incidental author exact match;
- true non-match returns null.

## Collections

Existing case-insensitive, whitespace-tolerant collection identity and deterministic display casing are
preserved. Series numeric ordering remains preserved.

## Exit status

Search relevance gate: PASS.
Offline/local gate: PASS.
Canonical-line gate: PASS.
Runtime/build gate: pending an executing CI runner.
