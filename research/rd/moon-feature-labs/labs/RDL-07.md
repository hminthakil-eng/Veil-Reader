# RDL-07 — Auto-scroll

## Problem
Deterministic pixel/line/page/rolling auto-scroll policies.

## Non-goal
Do not run while selection, overlays or accessibility ownership forbids it.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/AutoScroll.kt`

## Arena task
Design an independently implementable Auto-scroll capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Paused means zero movement; speeds clamp; page mode derives from viewport; manual interaction can cancel immediately.

## Integration trigger
Frame cost and accidental-motion safety pass.

## State
`PROTOTYPE` — R&D only. No production dependency.
