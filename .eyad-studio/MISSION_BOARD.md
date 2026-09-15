# Veil Reader — Management Mission Board

Status key: QUEUED → ACTIVE → REVIEW → ACCEPTED → HANDOFF → DONE.

## P0 — Product foundations

### M-001 Reader Experience Benchmark
Owner: Research
Support: Product & Design
Goal: compare leading reader interaction patterns, navigation, typography controls, page-turn behavior, annotation flow, accessibility, and distraction level.
Handoff: evidence brief + recommended Veil Reader interaction principles.

### M-002 Page-Turn Strategy
Owner: Research
Support: Development
Goal: determine the best technically realistic page-turn approach for EPUB/PDF on Android without damaging performance, accessibility, or Readium boundaries.
Handoff: recommendation + prototype constraints + acceptance criteria.

### M-003 Veil Design System
Owner: Product & Design
Support: Development, QA
Dependency: M-001
Goal: define semantic tokens, typography, components, motion rules, reduced-motion behavior, and reader-safe visual hierarchy.
Handoff: implementation-ready design spec.

### M-004 Performance Baseline
Owner: Development
Support: QA
Goal: establish measurable baselines for cold launch, Reading Now, 1,000-book library, EPUB open, large PDF, Castle, and large annotation archive.
Handoff: benchmark report and regression thresholds.

## P1 — World and progression

### M-005 Castle Interaction Research
Owner: Research
Support: Product & Design
Goal: validate a 2D explorable Castle model that reflects reading history without becoming a game overlay.
Handoff: interaction recommendation + prototype brief.

### M-006 Castle MVP
Owner: Product & Design
Support: Development
Dependency: M-005
Goal: produce the first implementation-ready Castle map and evolution rules.
Handoff: UX spec → Development vertical slice.

### M-007 Ethical Progression Audit
Owner: Research
Support: QA, Product
Goal: inspect Path, streak, ritual, mystery, achievement, and reward systems for pressure loops, dark patterns, or reading disruption.
Handoff: keep/change/remove decisions.

## P1 — Quality and release readiness

### M-008 Accessibility Matrix
Owner: QA & Release
Support: Product & Design, Development
Goal: TalkBack, large text, contrast, focus order, reduced motion, touch targets, RTL/mixed-script behavior.
Handoff: blocking/non-blocking defect matrix.

### M-009 Adaptive Layout Research
Owner: Research
Support: Product & Design, Development
Goal: define phone/tablet/foldable behavior and identify layouts that need adaptive restructuring rather than simple scaling.
Handoff: breakpoint/layout guidance.

### M-010 Data Durability Gate
Owner: QA & Release
Support: Development
Goal: migration fixtures, backup-wipe-restore, process recreation, corrupt/encrypted file failure behavior, privacy checks.
Handoff: release gate result.

## Management priorities
1. Run M-001, M-002, and M-004 in parallel.
2. Start M-003 as soon as M-001 has sufficient evidence.
3. Run M-005 and M-007 in parallel after foundation research is stable.
4. Keep M-008 and M-010 as continuous quality gates, not end-of-project cleanup.
5. No mission may claim completion without evidence appropriate to its acceptance criteria.
