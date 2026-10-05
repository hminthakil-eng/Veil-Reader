# R1.02 Slice 1 — Library Retrieval Policy Extraction

Date: 2026-10-05

## Scope

Move pure retrieval/grouping logic out of `LibraryScreen.kt` into
`LibraryRetrievalPolicy.kt`.

Extracted:
- `LibraryViewMode` and stored-value migration;
- shelf group models;
- case-insensitive named grouping;
- collection/series/author/current/completed/unopened shelf derivation.

## Explicit non-changes

This slice does not move or alter:
- Compose screen state;
- search query state;
- import/SAF flow;
- metadata editing;
- Book Detail;
- deletion;
- navigation;
- Room/repository behavior;
- Gallery/Shelves/Index rendering.

The goal is to reduce screen responsibility before UI composition is rebuilt.
