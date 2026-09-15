# Veil Reader — Initial Research Backlog

Priority is based on what can unblock or de-risk the v0.9 → 1.0 roadmap.

## R-001 — Reader visual benchmark
Compare Apple Books, Kindle, Kobo, Readwise Reader and strong Android readers for typography controls, navigation, focus, annotations, accessibility and distraction management. Deliver a pattern matrix and a Veil-specific recommendation; do not copy brand identity.

## R-002 — Castle map interaction prototype
Evaluate Compose Canvas/layered layouts for a 2D explorable Castle on ordinary Android hardware. Test touch targets, zoom/pan need, locked-room silhouettes, subtle parallax, reduced motion and accessibility semantics. Output: GO / GO WITH GUARDRAILS / alternative interaction.

## R-003 — Page-turn and pagination strategy
Assess realistic page-turn options around the existing Readium boundary. Determine which effects can be implemented without harming pagination correctness, RTL, performance, selection/highlights or accessibility. Prefer quiet, optional motion over novelty.

## R-004 — Large-library performance study
Define datasets and benchmarks for 1,000+ books, large annotation archives and large PDFs. Establish measurable thresholds for launch, scrolling, search/filter, reader-open latency, jank and memory before optimization work begins.

## R-005 — Accessibility research gate
Build a primary-flow audit plan for TalkBack, large text, focus order, contrast, touch targets, RTL/mixed scripts and reduced motion. Produce reusable acceptance criteria that every 0.9 feature can inherit.

## R-006 — Ethical gamification validation
Test whether Paths, Mysteries, rituals and Castle progression encourage reading without interrupting or pressuring it. Identify mechanics that create distraction, compulsive checking, FOMO or punishment and define banned patterns.

## R-007 — Adaptive Android layouts
Research phone/tablet/foldable layouts for Reading Now, Library, Castle and reader controls. Recommend breakpoint/window-class behavior and identify where dual-pane layouts genuinely improve reading workflows.

## Later, after 1.0 gates
- Manga / CBZ / webtoon reading architecture.
- TTS and audiobook support.
- Optional cloud sync and conflict model.
- Book-grounded spoiler-safe AI and local semantic search.
- Reading circles/social features with privacy boundaries.
