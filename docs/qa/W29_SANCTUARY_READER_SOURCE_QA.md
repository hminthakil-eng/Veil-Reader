# W29 — Sanctuary Reader Source QA

Date: 2026-09-30
Branch: `alpha/w29-sanctuary-reader-rebuild-v1`
Parent: `alpha/w28-artifact-chamber-rebuild-v1`

## Canonical drift

- W29 is ahead of W28 and zero commits behind.
- No divergent reader architecture introduced.
- Reader persistence/data ownership remains intact.

## Reader-engine preservation

Preserved:
- Readium navigator ownership;
- PaperCurlInputListener;
- SlideNavigationInputListener;
- StaticPagedNavigationInputListener;
- ReaderInputArbiter;
- ReaderModeHandoffState;
- durable close/background locator flow;
- previous-location return;
- note/highlight/selection behavior;
- accessibility and reduced motion.

## Mode ownership

- PAPER: explicit pure ownership policy, EPUB + paginated + PAPER only.
- SLIDE: existing explicit EPUB + paginated + SLIDE policy.
- PAGED: existing explicit EPUB + paginated + NONE policy.
- SCROLL: renderer continuous flow.
- Added one-hot tests: exactly one owner across supported EPUB navigation configurations.
- Added tests that Paper cannot leak into PDF or Scroll.

## Sanctuary visual hierarchy

- Reader content remains dominant.
- Top chrome flattened to edge rail.
- Bottom chrome flattened to edge rail.
- Mode selector changed from card wall to explicit selection rail.
- Auto-hide and center-tap ownership preserved.
- No persistent world-shell ornament added to the reading canvas.

## CI evidence

Android CI and Performance Benchmarks again created failed jobs with empty step lists. No build,
test, Gradle, benchmark or source step executed. Classified as runner/allocation infrastructure
evidence. Runtime gate remains blocked until a runner executes actual steps.

## Exit status

Source-level mode-ownership gate: PASS.
Sanctuary visual-hierarchy gate: PASS for first slice.
Canonical-line gate: PASS.
Runtime/build gate: BLOCKED ON EXECUTING RUNNER.
