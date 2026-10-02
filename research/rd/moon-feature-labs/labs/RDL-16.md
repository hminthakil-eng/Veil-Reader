# RDL-16 — Reading Calendar

## Problem
Calendar aggregation on top of existing durable reading sessions.

## Non-goal
Do not create a second statistics store.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/ReadingCalendar.kt`

## Arena task
Design an independently implementable Reading Calendar capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Sessions aggregate by local day/timezone; negative/invalid events are ignored; existing session source remains canonical.

## Integration trigger
Calendar presentation is judged useful over current history UI.

## State
`PROTOTYPE` — R&D only. No production dependency.
