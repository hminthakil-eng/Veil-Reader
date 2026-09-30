# W32 — Collections + Search Arena Spec

Date: 2026-09-30
Canonical branch: `alpha/w32-collections-search-rebuild-v1`
Parent: `alpha/w31-hidden-archive-rebuild-v1`

## Product role

Search is the fastest route through the Archive. Collections and Series are durable user-authored
ways of narrowing that archive. Atmosphere must never make retrieval slower.

## Search contract

- Fully local and offline.
- Persian / Arabic keyboard variants normalize identically.
- Diacritics, half-spaces and digit shapes must not hide records.
- Search covers title, author, series, language and collections.
- Default query results rank by relevance before recency.
- Explicit user sort (Title / Author / Series / Progress / Archive Depth) overrides relevance order.
- Stable ties remain deterministic.

## Relevance order

Highest to lowest:
1. exact title;
2. title prefix;
3. title contains;
4. exact/prefix/contains in author;
5. series;
6. collection;
7. language.

The exact numeric score is implementation detail; the ordering is the contract.

## Collections contract

- Collection matching is case-insensitive and whitespace tolerant.
- Display casing remains deterministic.
- Series preserve numeric order before title.
- Filtering remains reversible in one action.
- Collection/series labels remain user-authored; Veil does not rename them into lore terms.

## Visual language

- Search remains a strong instrument directly below Archive identity.
- Active facets use restrained brass registration, not pill walls.
- Collection/series wings are architectural navigation aids, not decorative cards.
- Search results reuse Gallery / Shelves / Index rather than inventing a second result screen.

## W32 implementation slice

1. Add deterministic local relevance scoring.
2. Make default queried results relevance-first with recency tie-break.
3. Preserve explicit sorts.
4. Add tests for title/author/series/collection priority and stable ties.
5. Keep all search normalization and RTL behavior.

## Rejection conditions

Reject if:
- search requires network or account;
- fuzzy behavior becomes nondeterministic;
- exact title can rank below an incidental collection match;
- user-selected sort is silently ignored;
- collection casing/identity changes unexpectedly.
