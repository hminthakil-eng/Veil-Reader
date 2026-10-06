# R1.03 Slice 1 — Shell Policy Extraction

Date: 2026-10-05

Extracted from `VeilApp.kt`:
- selected-visible-tab fallback;
- hidden world route fallback when gamification is disabled;
- navigation rail breakpoint policy;
- content max-width policy.

No ViewModel, route state, Reader session, repository or persistence ownership moves.

The world-route fallback is now unit-tested instead of being embedded in a
Composable effect.
