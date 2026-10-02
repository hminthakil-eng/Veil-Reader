# RDL-10 — Legacy Formats

## Problem
Adapter boundary for MOBI/AZW3/FB2/CHM/DJVU/DOCX/ODT/RTF/MHTML/UMD.

## Non-goal
Do not replace Readium EPUB/PDF paths or add parsers without demand.

## Current isolated prototype
`rd-reader-labs/src/main/kotlin/com/veilreader/rd/LegacyFormats.kt`

## Arena task
Design an independently implementable Legacy Formats capability for Veil Reader that preserves the canonical
Readium boundary, offline-first behavior, durability, accessibility, RTL and current Reader input
ownership. The proposal must stay isolated from production until its acceptance gate is proven.

## Acceptance gate
Detection is deterministic; adapters advertise truthful search/select/annotate capabilities.

## Integration trigger
Usage data or user demand justifies each format dependency.

## State
`PROTOTYPE` — R&D only. No production dependency.
