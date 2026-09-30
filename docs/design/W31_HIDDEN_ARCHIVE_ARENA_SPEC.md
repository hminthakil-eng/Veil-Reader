# W31 — Hidden Archive Arena Spec

Date: 2026-09-30
Canonical branch: `alpha/w31-hidden-archive-rebuild-v1`
Parent: `alpha/w30-reader-appearance-pdf-workspace-v1`

## Product role

Hidden Archive is the reader's private research memory. It stores preserved passages, annotations,
bookmarks, returning echoes and sealed reading-time capsules. It must optimize retrieval and source
provenance before atmosphere.

## Hierarchy

1. Archive identity / privacy.
2. Search.
3. Section index: Notes / Highlights / Bookmarks / Echoes / Capsules.
4. Folio records.
5. Return to exact source passage.
6. Edit/delete as secondary record maintenance.

## Record law

Every visible record should answer:
- what was preserved;
- which book it came from;
- when / how old the trace is when known;
- whether it has been revisited;
- whether an annotation exists;
- how to return to its exact source.

## Visual language

- Records are folios / dossier entries, not elevated cards.
- Flat registration lines replace repeated borders.
- Antique brass marks provenance and hierarchy.
- Oxblood is reserved for destructive/exceptional emphasis.
- Search may remain a clear input instrument.
- Dense information is allowed, but actions must not overpower content.

## Interaction law

- Search remains global across supported record fields.
- Section switch is one tap and preserves the query.
- Passage return continues using the stored locator JSON.
- Note editing and deletion preserve existing confirmation/durability behavior.
- Empty state explains whether the archive is empty or the query simply has no match.

## Memory systems preserved

- highlight memories;
- passage revisit counts;
- archive echoes;
- reading-time capsules;
- bookmarks;
- notes;
- exact passage return.

## W31 implementation slice

1. Flatten archive register into a ledger summary.
2. Flatten section tabs into an index rail.
3. Convert highlight/note/echo records into folios.
4. Convert bookmarks into folio marks.
5. Convert empty state into a quiet archive field.
6. Preserve every retrieval and mutation path.

## Rejection conditions

Reject if:
- it reads like a social feed;
- cards dominate the preserved text;
- book/source identity becomes secondary;
- return-to-passage is hidden;
- delete/edit compete with the source-return action;
- search or locator behavior changes;
- decorative atmosphere harms dense scanning.
