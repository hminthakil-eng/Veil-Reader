# RDL-13 — E-Ink

## Problem
Low-motion, grayscale/contrast-oriented display policy.

## Non-goal
Do not assume LCD/OLED animation behavior or force e-ink mode on normal screens.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/DisplayAndSecurity.kt`

## Arena task
Design an independently implementable E-Ink capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Animations default off; contrast clamps; refresh policy is explicit.

## Integration trigger
Physical e-ink device validation exists.

## State
`PROTOTYPE` — R&D only. No production dependency.
