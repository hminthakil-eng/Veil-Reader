# RDL-11 — Library Metadata

## Problem
Ratings, tags and custom-cover metadata without polluting core book identity.

## Non-goal
Do not make decorative metadata block book opening or imports.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/LibraryMetadata.kt`

## Arena task
Design an independently implementable Library Metadata capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Ratings clamp; tags normalize/dedupe; custom cover is optional and reversible.

## Integration trigger
Retrieval research proves metadata improves library use.

## State
`PROTOTYPE` — R&D only. No production dependency.
