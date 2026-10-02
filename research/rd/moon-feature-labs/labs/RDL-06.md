# RDL-06 — TTS

## Problem
Chunking, filtering and playback-state foundation for spoken reading.

## Non-goal
Do not couple TTS state to visual page-turn ownership.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/TtsLab.kt`

## Arena task
Design an independently implementable TTS capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Chunks are bounded, sentence-aware and filterable; lifecycle/interrupt policy is specified before Android integration.

## Integration trigger
Accessibility/power-user demand justifies Android TTS integration.

## State
`PROTOTYPE` — R&D only. No production dependency.
