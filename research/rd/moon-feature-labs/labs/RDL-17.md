# RDL-17 — Page-turn Renderer

## Problem
A measured renderer boundary for lower-latency Paper/Slide alternatives.

## Non-goal
Do not port Moon proprietary curl code or change navigation commit semantics.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/PageTurnRendererLab.kt`

## Arena task
Design an independently implementable Page-turn Renderer capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Capture/frame/memory budgets are explicit; a candidate fails if any budget fails; locator/input contracts stay canonical.

## Integration trigger
Current page-turn P95 remains over budget after targeted optimization.

## State
`PROTOTYPE` — R&D only. No production dependency.
