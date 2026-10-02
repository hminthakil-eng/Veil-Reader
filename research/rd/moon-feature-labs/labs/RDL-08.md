# RDL-08 — Catalogs

## Problem
OPDS/Calibre-style online catalog abstraction.

## Non-goal
Do not bake one vendor or remote account into the reader core.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/Catalogs.kt`

## Arena task
Design an independently implementable Catalogs capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Provider contract is network-explicit; pages are provider-neutral; acquisition URLs remain data, not auto-download actions.

## Integration trigger
Online-library scope is approved.

## State
`PROTOTYPE` — R&D only. No production dependency.
