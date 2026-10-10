---
name: veil-paper-engine-physics-qa
description: Design, debug, and validate the distinct PAPER CURL mode of Veil Reader with correct input ownership, physical sheet behavior, RTL direction, EPUB resource transitions, and device-grade acceptance.
---

# Physical Paper Engine + QA

Use with the existing Reader Android Engineer and Release Guardian. The user has repeatedly reported that PAPER CURL silently behaves like SLIDE, so visual proof is obligatory.

## Characterize first
Inspect current canonical Paper implementation, feature switch, gesture stack, navigation adapter, renderer, current Paper PRs, and their unresolved gates. Capture the initial mode and current SHA. Never delete or overwrite working SLIDE, PAGED, or SCROLL implementations.

## Four separate interaction contracts
PAGED = discrete non-paper navigation; SLIDE = planar translation; PAPER CURL = sheet geometry/shadow/resistance with visible fold/back surface and gesture-dependent release; SCROLL = continuous vertical reading. No silent fallback from Paper to Slide: show explicit unavailable/failure state and preserve location.

## Verification scenarios
- Tap, short swipe, long drag, edge vs center start, rapid reversals, cancel, pointer loss, back navigation, focus interruptions, device rotation, touch slop, and split second-gesture.
- LTR and RTL forward/back orientation, mixed bidirectional content, enlarged font, margins and page count changes.
- Same chapter, across EPUB spine resources, first/last page, embedded images, footnotes, and restored bookmark.
- Cover acquisition/release, GPU/texture allocation failure, redraw after process resume, and low-memory recovery.
- No double turns, lost turns, progress jumps, delayed locator saves or book-close races.

## Definition of evidence
Record reproducible test book identifiers, setup, phone/OS/GPU, exact commit, screen capture of visible fold animation, profiler trace and navigation event log; pair with pure-geometry/unit and instrumentation tests.
Benchmarks use release-like/R8 conditions on actual hardware; compare to the previous engine and report median/tail distributions, not ungrounded targets.
PASS only when folded sheet follows input and restores state safely, not simply when an APK builds.
Stop deployment if Paper degrades to Slide, crashes, corrupts location or leaks rendering resources.
