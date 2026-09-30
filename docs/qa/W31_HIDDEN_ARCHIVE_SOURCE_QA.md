# W31 — Hidden Archive Source QA

Date: 2026-09-30
Branch: `alpha/w31-hidden-archive-rebuild-v1`
Parent: `alpha/w30-reader-appearance-pdf-workspace-v1`

## Canonical drift

- W31 is ahead of W30 and zero commits behind.
- No data model or locator ownership fork introduced.

## Retrieval hierarchy

- Search remains above record content.
- Notes / Highlights / Bookmarks / Echoes / Capsules remain one-tap sections.
- Query state remains preserved while switching sections.
- Folio records retain book/source identity before maintenance actions.
- Return-to-passage remains directly available when a source book exists.

## Visual-language checks

- Summary register is a ledger, not a card.
- Section tabs are a flat index rail.
- Highlight/note/echo entries are folios with registration lines.
- Bookmarks are folio marks.
- Empty state is a quiet archive field.
- Dense content remains readable without ornamental card stacking.

## Functional invariants preserved

- Search over quote, note, title and author.
- Note filtering.
- Bookmark filtering.
- Echo derivation.
- Time-capsule filtering.
- Exact locator JSON return.
- Note edit.
- Highlight delete.
- Bookmark delete.
- Revisit/memory metadata.

## Runtime gate

W30 parent CI again failed before any workflow step executed. W31 remains draft until a runner
executes real build/test steps.

## Exit status

Source-level Hidden Archive gate: PASS.
Canonical-line gate: PASS.
Runtime/build gate: BLOCKED ON EXECUTING RUNNER.
