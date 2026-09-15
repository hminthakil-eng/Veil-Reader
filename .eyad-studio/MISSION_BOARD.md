# Veil Reader — Management Mission Board

Status key: QUEUED → ACTIVE → REVIEW → ACCEPTED → HANDOFF → DONE.

## P0 — Reading RPG foundations

### M-001 Reader Experience Benchmark — ACTIVE
Owner: Research
Support: Product & Design
Goal: compare leading book, manga and webtoon reader interaction patterns, navigation, typography/image controls, page-turn behavior, annotation flow, accessibility and distraction level.
Handoff: evidence brief + recommended Veil Reader interaction principles.

### M-002 Multi-Format Reading Strategy — ACTIVE
Owner: Research
Support: Development
Goal: define one coherent reader architecture for EPUB, PDF, CBZ/image manga, RTL/LTR manga and vertical webtoon without breaking existing Readium boundaries.
Handoff: architecture recommendation + mode matrix + acceptance criteria.

### M-003 Veil Design System
Owner: Product & Design
Support: Development, QA
Dependency: M-001
Goal: define semantic tokens, typography, comic/image controls, components, motion rules, reduced-motion behavior and reader-safe visual hierarchy.
Handoff: implementation-ready design spec.

### M-004 Performance Baseline — ACTIVE
Owner: Development
Support: QA
Goal: baseline cold launch, 1,000-book library, EPUB/PDF open, large manga chapter, long webtoon, Castle and large annotation archive.
Handoff: benchmark report + regression thresholds.

### M-011 Manga Source Architecture — ACTIVE
Owner: Research
Support: Software Architecture, QA
Goal: compare separate extension APKs, signed provider packages and declarative adapters for online manga catalogues. Evaluate security, maintainability, source breakage, update flow, Play-distribution constraints and legal/licensing surface.
Handoff: GO / GO WITH GUARDRAILS recommendation + provider contract.

### M-012 Manga/Webtoon Reader Vertical Slice
Owner: Development
Support: Product & Design, QA
Dependency: M-002
Goal: production-quality local CBZ/image chapter slice with RTL/LTR paging, preload/cache, continuous webtoon mode, zoom/fit controls and durable progress.
Handoff: tested vertical slice + benchmarks.

## P0 — Coherent RPG system

### M-013 RPG Systems Bible — ACTIVE
Owner: Product & Design
Support: Research
Goal: turn Paths into real classes and define Origins, attributes, quest taxonomy, factions, companions, Chronicle, rituals, progression economy and world-state rules as one coherent system.
Handoff: canonical RPG rules + progression formulas + content boundaries.

### M-014 Reading-to-Progression Engine
Owner: Research
Support: Product, Development, QA
Dependency: M-013
Goal: define defensible reading evidence for rewards so progress reflects genuine reading rather than page tapping or exploit loops.
Handoff: event model + anti-cheese rules + ethical progression criteria.

### M-015 Persistent World State Model
Owner: Development
Support: Product & Design
Dependency: M-013, M-014
Goal: design Room-backed data model for character, Path, attributes, quests, NPC/faction state, Chronicle, artifacts and world evolution with migration-safe versioning.
Handoff: schema proposal + migration plan + test matrix.

### M-016 Castle → Living World
Owner: Product & Design
Support: Research, Development
Dependency: M-013
Goal: evolve Castle from menu/cards into a navigable hub with rooms, Gatehouse, Guild Hall, NPC presence, environmental state and visible consequences of reading history.
Handoff: world map + interaction spec + evolution rules.

## P1 — Source platform and library

### M-017 Source Provider Contract
Owner: Software Architecture
Support: Research, Development, QA
Dependency: M-011
Goal: define `ContentProvider` boundary for catalogue, search, metadata, chapter lists, page streams, updates and downloads without coupling providers to RPG or reader internals.
Handoff: interface/spec + failure model + versioning policy.

### M-018 Provider Trust & Security Model
Owner: QA & Release
Support: Research, Architecture
Dependency: M-011
Goal: define trust levels, permissions, domain visibility, install/update integrity, provider health, kill-switch behavior and user warnings.
Handoff: security gate + threat model.

### M-019 Source Manager UX
Owner: Product & Design
Support: Research, Development
Dependency: M-011, M-017
Goal: source discovery/connection, health state, permissions, updates, search and per-source controls without confusing the main library.
Handoff: UX spec.

## P1 — World and progression quality

### M-005 Castle Interaction Research
Owner: Research
Support: Product & Design
Goal: validate 2D explorable hub interaction on ordinary Android hardware, including accessibility and reduced motion.
Handoff: prototype recommendation.

### M-006 Castle MVP
Owner: Product & Design
Support: Development
Dependency: M-005, M-013
Goal: produce first implementation-ready Castle/world hub.
Handoff: UX spec → Development vertical slice.

### M-007 Ethical Progression Audit
Owner: Research
Support: QA, Product
Goal: inspect quests, Paths, streaks, rituals, NPC relationships, rewards and discoveries for pressure loops, FOMO or reading disruption.
Handoff: keep/change/remove decisions.

## Continuous quality gates

### M-008 Accessibility Matrix
Owner: QA & Release
Support: Product & Design, Development
Goal: TalkBack, large text, contrast, focus, reduced motion, touch targets, RTL/mixed-script, comic gestures and webtoon accessibility.
Handoff: blocker matrix.

### M-009 Adaptive Layout Research
Owner: Research
Support: Product & Design, Development
Goal: phone/tablet/foldable behavior for book reading, manga double-page, webtoon and world hub.
Handoff: adaptive layout guidance.

### M-010 Data Durability Gate
Owner: QA & Release
Support: Development
Goal: migrations, backup/restore, process recreation, provider removal, corrupt/encrypted files, offline cache integrity and privacy checks.
Handoff: release gate result.

## Management priorities
1. Run M-001, M-002, M-004, M-011 and M-013 in parallel.
2. Start M-012 after M-002 produces a safe mode boundary; do not wait for online sources.
3. Start M-014 immediately after the first RPG Systems Bible draft.
4. M-017 and M-018 follow the architecture decision from M-011 before any broad provider integration.
5. M-008 and M-010 remain continuous gates.
6. No mission may claim completion without evidence appropriate to its acceptance criteria.