# R5.01 / R5.02 — Book Detail CTA-First Hierarchy

Date: 2026-10-05
Parent: `reforge/archive-density-v2`

## Product problem

Compact Book Detail placed a 184×260dp artifact above title, author and the primary
Read/Continue action. The artifact was visually premium but delayed the actual task.

## Composition rule

### Normal compact phone
When the readable width is at least 380dp and text scale is below the dense-control
threshold:
- use a bounded 118×170dp artifact;
- place identity + primary reading CTA beside it;
- keep the CTA in the first scan path.

### Narrow / large text
- do not shrink text;
- render identity + primary CTA first;
- place a bounded 132×190dp artifact afterward.

### Expanded layouts
Retain the larger artifact + identity pair.

## Preserved

- one dominant Read / Continue / Read Again action;
- Favorite and Edit remain secondary and below the journey section;
- Delete remains separated as destructive maintenance;
- no persistence, Book Detail action, archive-history or annotation behavior changes.

## Acceptance

- unit tests for compact/large-text/expanded composition policy;
- Android CI;
- Storage rendered evidence after parent Archive pass lands;
- English/Persian;
- 100%/200% text;
- primary CTA must remain >=54dp.
