# RDL-05 — Context Tools

## Problem
Extensible selection actions for dictionary, translate, web search and share.

## Non-goal
Do not make network tools mandatory for core reading.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/ContextTools.kt`

## Arena task
Design an independently implementable Context Tools capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Offline provider wins in offline mode; language support is explicit; providers are replaceable.

## Integration trigger
A safe provider set and localized UX are approved.

## State
`PROTOTYPE` — R&D only. No production dependency.
