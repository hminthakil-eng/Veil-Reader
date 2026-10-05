# R3 — Threshold Return-Speed Pass

Date: 2026-10-05

## Product problem

Returning phone readers were repeatedly shown the full cinematic Threshold prologue
before the active book. This made a beautiful entrance behave like recurring friction.

## Change

For a returning reader with a current book on compact phone width:
- keep the Grayfog doorway, seal, realm identity and title;
- abbreviate recurring narrative copy;
- remove the large recurring prose block;
- reduce vertical spacing using semantic tokens;
- allow the active volume to enter the first scan path earlier.

Wide layouts may retain the full authored approach when text scale/height permit,
because the architectural pair can place the current book alongside the approach.

Large text and short-height pressure still condense the approach without shrinking user text.

## Empty library

The first/empty Threshold keeps the full cinematic entrance. This change is not a
blanket flattening of Grayfog identity.

## Evidence

The Storage Instrumentation workflow now triggers for:
- ReadingNowScreen
- LibraryScreen
- ui/theme changes

This ensures Grayfog native render captures are produced for visual shell changes.

## Acceptance

- unit tests for compact/wide/large-text/empty policies;
- Android CI;
- Storage Instrumentation + Grayfog captures;
- Performance Benchmarks where triggered;
- visual inspection of English/Persian at 100%/200%.

This is GREEN-SOURCE/visual evidence work only; product acceptance remains separate.
