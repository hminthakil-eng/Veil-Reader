# RDL-01 — Appearance Profiles

## Problem
Named, versioned appearance profiles with optional per-book override.

## Non-goal
Do not duplicate the canonical ReaderAppearance renderer mapping.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/AppearanceProfiles.kt`

## Arena task
Design an independently implementable Appearance Profiles capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Profile merge order is deterministic; global state is not mutated; schema is versioned.

## Integration trigger
Users need reusable setups and backup schema can preserve them.

## State
`PROTOTYPE` — R&D only. No production dependency.
