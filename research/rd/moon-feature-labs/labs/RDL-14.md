# RDL-14 — App Lock

## Problem
Optional local privacy gate using device credential/biometric/PIN policy.

## Non-goal
Do not invent custom crypto or block file recovery without clear policy.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/DisplayAndSecurity.kt`

## Arena task
Design an independently implementable App Lock capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Grace period clamps; lock method is explicit; lifecycle transition matrix is specified before integration.

## Integration trigger
User-facing privacy lock enters scope.

## State
`PROTOTYPE` — R&D only. No production dependency.
