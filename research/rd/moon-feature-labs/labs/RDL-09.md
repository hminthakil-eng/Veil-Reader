# RDL-09 — Sync

## Problem
Provider-neutral synchronization for progress, notes, shelves and later settings.

## Non-goal
Do not weaken offline-first ownership or silently overwrite conflicts.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/SyncLab.kt`

## Arena task
Design an independently implementable Sync capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Conflict resolution is deterministic; tombstones are respected; records are key-scoped and provider-independent.

## Integration trigger
Optional sync/account scope is approved with privacy review.

## State
`PROTOTYPE` — R&D only. No production dependency.
