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
Support: Reader Platform, Development
Goal: define one coherent reader architecture for EPUB, PDF, CBZ/image manga, RTL/LTR manga and vertical webtoon without breaking existing Readium boundaries.
Handoff: architecture recommendation + mode matrix + acceptance criteria.

### M-003 Veil Design System
Owner: Product & Design
Support: Visual World, Development, QA
Dependency: M-001
Goal: define semantic tokens, typography, comic/image controls, components, motion rules, reduced-motion behavior and reader-safe visual hierarchy.
Handoff: implementation-ready design spec.

### M-004 Performance Baseline — ACTIVE
Owner: Development
Support: QA
Goal: baseline cold launch, 1,000-book library, EPUB/PDF open, large manga chapter, long webtoon, settlement map and large annotation archive.
Handoff: benchmark report + regression thresholds.

### M-011 Manga Source Architecture — ACTIVE
Owner: Research
Support: Source Platform, QA
Goal: compare separate extension APKs, signed provider packages and declarative adapters for online manga catalogues. Evaluate security, maintainability, source breakage, update flow, Play-distribution constraints and legal/licensing surface.
Handoff: GO / GO WITH GUARDRAILS recommendation + provider contract.

### M-012 Manga/Webtoon Reader Vertical Slice
Owner: Reader Platform
Support: Product & Design, Development, QA
Dependency: M-002
Goal: production-quality local CBZ/image chapter slice with RTL/LTR paging, preload/cache, continuous webtoon mode, zoom/fit controls and durable progress.
Handoff: tested vertical slice + benchmarks.

## P0 — Coherent RPG system

### M-013 RPG Systems Bible — ACTIVE
Owner: Game Systems
Support: Research, Product & Design
Goal: define Origins, Paths/classes, attributes, quest taxonomy, factions, companions, Chronicle, rituals, progression economy and world-state rules as one coherent system.
Handoff: canonical RPG rules + progression formulas + content boundaries.

### M-014 Reading-to-Progression Engine
Owner: Research
Support: Game Systems, Settlement & Economy, Development, QA
Dependency: M-013
Goal: define defensible reading evidence for rewards so progress reflects genuine reading rather than page tapping or exploit loops.
Handoff: event model + anti-cheese rules + ethical progression criteria.

### M-015 Persistent World State Model
Owner: Development
Support: Game Systems, Settlement & Economy, World Narrative
Dependency: M-013, M-014
Goal: design Room-backed data model for character, Path, resources, settlement, quests, NPC/faction state, Chronicle, artifacts and world evolution with migration-safe versioning.
Handoff: schema proposal + migration plan + test matrix.

## P0 — Wasteland Reclamation

### M-020 Settlement Progression Bible — ACTIVE
Owner: Settlement & Economy
Support: Game Systems, World Narrative, Product & Design
Goal: turn the approved Wasteland → Camp → Water & Soil → Homestead → Hamlet → Village → Fortified Settlement → Castle → City arc into canonical unlocks, costs, choices and emotional beats.
Handoff: building tree + tier gates + resource model + branch rules.

### M-021 Reading Economy & Balance — ACTIVE
Owner: Settlement & Economy
Support: Research, Game Systems, QA
Dependency: M-014
Goal: determine fair Water/Coin/material/Knowledge/Influence sources and sinks for light, medium and heavy readers without grind, FOMO or fake engagement incentives.
Handoff: economy simulation + pacing bands + exploit analysis.

### M-022 Reclamation World Bible — ACTIVE
Owner: Worldbuilding & Narrative
Support: Game Systems, Visual World
Goal: explain why stories restore the land, define the Veil, first NPC arrivals, settlement-era narrative beats, factions and how the Chronicle records the user's reading journey without importing copyrighted fiction worlds.
Handoff: world premise + NPC/faction matrix + tier narrative arcs.

### M-023 Settlement Visual Evolution
Owner: Visual World & Motion
Support: Product & Design, Settlement & Economy
Dependency: M-020
Goal: design a persistent map where the same land visibly evolves from dust and tent to farms, roads, districts, Castle and city; include reduced-motion and accessibility alternatives.
Handoff: visual state matrix + map composition rules + asset plan.

### M-024 Wasteland → Homestead Vertical Slice
Owner: Development
Support: Settlement & Economy, Game Systems, World Narrative, Visual World, QA
Dependency: M-014, M-020, M-023
Goal: playable loop: read → earn Water/Coin → tent → well/cistern → first farm plot → shack → first NPC, with durable persistence and no tap-grind.
Handoff: tested vertical slice + player-flow evidence + balance telemetry plan using local test data only.

### M-025 Settlement Choice Model
Owner: Game Systems
Support: Settlement & Economy, World Narrative
Dependency: M-020
Goal: ensure equal reading progress can produce different settlements through agriculture/trade/knowledge, Path influence, NPC priorities and recoverable strategic choices.
Handoff: branching rules + respec/recovery policy + anti-optimal-path constraints.

## P1 — Source platform and library

### M-017 Source Provider Contract
Owner: Source Platform
Support: Research, Development, QA
Dependency: M-011
Goal: define `ContentProvider` boundary for catalogue, search, metadata, chapter lists, page streams, updates and downloads without coupling providers to RPG or settlement internals.
Handoff: interface/spec + failure model + versioning policy.

### M-018 Provider Trust & Security Model
Owner: QA & Release
Support: Research, Source Platform
Dependency: M-011
Goal: define trust levels, permissions, domain visibility, install/update integrity, provider health, kill-switch behavior and user warnings.
Handoff: security gate + threat model.

### M-019 Source Manager UX
Owner: Product & Design
Support: Research, Reader Platform, Development
Dependency: M-011, M-017
Goal: source discovery/connection, health state, permissions, updates, search and per-source controls without confusing the main library.
Handoff: UX spec.

## P1 — World and progression quality

### M-005 World Hub Interaction Research
Owner: Research
Support: Product & Design, Visual World
Goal: validate an accessible explorable settlement/castle map on ordinary Android hardware.
Handoff: prototype recommendation.

### M-006 Castle/City Hub MVP
Owner: Product & Design
Support: Visual World, Development
Dependency: M-005, M-013, M-020
Goal: define how late settlement tiers transition into Castle and city without becoming a detached game menu.
Handoff: UX spec → Development vertical slice.

### M-007 Ethical Progression Audit
Owner: Research
Support: QA, Game Systems, Settlement & Economy
Goal: inspect quests, resources, farming, Paths, streaks, rituals, NPC relationships, rewards and discoveries for pressure loops, FOMO or reading disruption.
Handoff: keep/change/remove decisions.

## Continuous quality gates

### M-008 Accessibility Matrix
Owner: QA & Release
Support: Product & Design, Development, Visual World
Goal: TalkBack, large text, contrast, focus, reduced motion, touch targets, RTL/mixed-script, comic gestures, webtoon and settlement-map accessibility.
Handoff: blocker matrix.

### M-009 Adaptive Layout Research
Owner: Research
Support: Product & Design, Development
Goal: phone/tablet/foldable behavior for book reading, manga double-page, webtoon and settlement/world hub.
Handoff: adaptive layout guidance.

### M-010 Data Durability Gate
Owner: QA & Release
Support: Development
Goal: migrations, backup/restore, process recreation, provider removal, corrupt/encrypted files, offline cache integrity, settlement state durability and privacy checks.
Handoff: release gate result.

## Management priorities
1. Run M-001, M-002, M-004, M-011, M-013, M-020, M-021 and M-022 in parallel where dependencies allow.
2. M-020/M-021/M-022 form the Wasteland Reclamation Pod and must converge on one shared model before M-024 enters production implementation.
3. Start M-012 after M-002 produces a safe mode boundary; do not wait for online sources.
4. Start M-014 immediately after the first RPG Systems Bible draft.
5. M-017 and M-018 follow the architecture decision from M-011 before any broad provider integration.
6. M-008 and M-010 remain continuous gates.
7. Any team may pitch improvements through the Creative Autonomy Protocol; Management may greenlight aligned reversible work.
8. No mission may claim completion without evidence appropriate to its acceptance criteria.
