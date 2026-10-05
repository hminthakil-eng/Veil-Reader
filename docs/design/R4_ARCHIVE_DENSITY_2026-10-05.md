# R4 — Archive Density / Retrieval-First Pass

Date: 2026-10-05

## Product problem

The Archive had strong retrieval capability but weak first-viewport composition.
Returning users paid for:
- a cinematic populated header;
- a large Reading State panel;
- always-visible Collection / Series / Sort controls;
- an oversized Gallery artifact;
before they reached actual books.

The feature set was not the problem. Its permanent visual footprint was.

## Changes

### Populated header
- populated Archive condenses by default;
- full cinematic header remains for empty/first-use Archive;
- at normal phone text scale actions may share the identity row;
- at large text, actions move below instead of shrinking labels.

### Retrieval rail
- Reading State becomes a compact one-row instrument;
- Gallery / Shelves / Index remain always reachable;
- secondary Collection / Series / Sort controls use progressive disclosure;
- active secondary-filter count stays visible while collapsed;
- Reset remains direct when filters are active.

### Gallery density
- physical cover object is capped independently from the text record;
- normal cover cap: 152dp;
- large-text cover cap: 144dp;
- title no longer reserves two lines when one is enough;
- author no longer reserves two lines at normal scale;
- verbose Archive Record text action becomes a 48dp semantic icon action;
- favorite remains a 48dp semantic action;
- metadata spacing uses semantic spacing tokens.

## Accessibility

This pass never solves layout by reducing user text.

At large text:
- the record may grow for metadata;
- the physical cover stays bounded;
- header actions wrap below when needed;
- controls remain >=48dp;
- labels remain full-size.

## Preserved capabilities

No feature is removed:
- Search
- Reading State
- Collection
- Series
- Sort
- Gallery
- Shelves
- Index
- Reset
- Settings
- Import

The pass changes hierarchy and disclosure, not capability.

## Visual evidence gate

Because the parent Threshold pass expands Storage Instrumentation triggers to
ReadingNow, Library and ui/theme changes, this branch must produce the Grayfog
native render artifact for English/Persian and large-text inspection.

## Acceptance

- Android CI
- Storage Instrumentation
- Grayfog capture artifact
- no clipping at 100% / 200%
- no loss of filter reachability
- first actual book visibly earlier in populated Archive
- no regression to 48dp actions
