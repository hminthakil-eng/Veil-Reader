# RDL-02 — Input Profiles

## Problem
Advanced tap-zone and key mapping while preserving calm defaults.

## Non-goal
Do not bypass ReaderInputArbiter or Android accessibility/system navigation.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/InputProfiles.kt`

## Arena task
Design an independently implementable Input Profiles capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Center chrome remains reachable; actions resolve deterministically; accessibility mode can retain renderer ownership.

## Integration trigger
Power-user customization is requested and device input QA passes.

## State
`PROTOTYPE` — R&D only. No production dependency.
