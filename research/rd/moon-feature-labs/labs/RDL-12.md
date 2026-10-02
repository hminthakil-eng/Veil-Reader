# RDL-12 — Name Replacement

## Problem
Reader-only reversible text replacement rules inspired by Moon power tools.

## Non-goal
Do not modify source publication files or persisted quotes silently.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/NameReplacement.kt`

## Arena task
Design an independently implementable Name Replacement capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Rules are deterministic and word-bounded; original publication bytes remain untouched.

## Integration trigger
Advanced-reader demand exists and quote/annotation semantics are solved.

## State
`PROTOTYPE` — R&D only. No production dependency.
