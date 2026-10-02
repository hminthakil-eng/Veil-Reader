# RDL-04 — PDF Annotations

## Problem
Truthful PDF highlight/note/ink/shape annotation capability.

## Non-goal
Do not fake support when the active PDF backend cannot round-trip annotations.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/Annotations.kt`

## Arena task
Design an independently implementable PDF Annotations capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Capability-gated drafts validate; write/read/export path must preserve locators and document integrity.

## Integration trigger
Chosen PDF backend proves durable annotation round trip.

## State
`PROTOTYPE` — R&D only. No production dependency.
