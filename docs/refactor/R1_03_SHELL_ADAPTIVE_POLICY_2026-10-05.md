# R1.03 Slice 1 — Adaptive Shell Policy

Date: 2026-10-05

Moved pure adaptive shell decisions out of `VeilApp.kt`:
- bottom navigation vs navigation rail;
- content max-width ceiling.

No route state, Reader ownership, overlays, repositories or screen composition moved.

Added focused tests for:
- compact portrait;
- medium-width / short-height landscape;
- medium-width / medium-height rail eligibility;
- expanded content width.
