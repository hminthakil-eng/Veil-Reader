# RDL-03 — Reading Ruler / Focus

## Problem
A focus overlay/ruler that improves sustained reading without altering publication DOM.

## Non-goal
Do not rewrite EPUB content or create selection conflicts.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/ReadingRuler.kt`

## Arena task
Design an independently implementable Reading Ruler / Focus capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Geometry clamps to viewport; reduced-motion safe; TalkBack does not treat decoration as content.

## Integration trigger
User testing shows reading value without visual fatigue.

## State
`PROTOTYPE` — R&D only. No production dependency.
