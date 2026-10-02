# RDL-15 — Widget / Shortcut

## Problem
Android home surfaces for resume/book shortcuts.

## Non-goal
Do not expose private title/cover content on lockscreen/home without user choice.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/DisplayAndSecurity.kt`

## Arena task
Design an independently implementable Widget / Shortcut capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Shortcut payload is minimal; privacy behavior and stale-book handling are defined.

## Integration trigger
Home-surface work enters product scope.

## State
`PROTOTYPE` — R&D only. No production dependency.
