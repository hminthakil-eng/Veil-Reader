# Eyad Studio — Coding Interview University: Engineering Reuse Bank

**Research date:** 2026-10-18  
**Source:** https://github.com/jwasham/coding-interview-university  
**Project:** `jwasham/coding-interview-university`  
**Primary-source files reviewed:** [`README.md`](https://github.com/jwasham/coding-interview-university/blob/main/README.md), [`programming-language-resources.md`](https://github.com/jwasham/coding-interview-university/blob/main/programming-language-resources.md), [`LICENSE.txt`](https://github.com/jwasham/coding-interview-university/blob/main/LICENSE.txt).  
**Veil Reader comparison:** [`ROADMAP_0.9_TO_1.0.md`](https://github.com/hminthakil-eng/Veil-Reader/blob/main/ROADMAP_0.9_TO_1.0.md), [`WORLD_CLASS_RELEASE_GATES.md`](https://github.com/hminthakil-eng/Veil-Reader/blob/main/WORLD_CLASS_RELEASE_GATES.md).  
**Status:** Curated research, original engineering recommendations. No scripts installed, no production code modified, no tests claimed.

## Executive determination

Coding Interview University (CIU) is a study roadmap, not an executable agent toolkit, compiler, AI coding plugin, or production-quality library. Its value to Eyad Studio is **systematic fundamentals training**, and to Veil Reader specifically is transforming selected CS topics into measurable implementation review criteria. Do not copy unrelated algorithms into the Android app simply because they are listed; rely on standard Kotlin/JVM collections and proven frameworks unless profiling proves otherwise.

The original work states it is aimed at technical interviews, with core data structures and algorithms plus optional systems topics. The repo is licensed **CC BY-SA 4.0**, not MIT; the external links in the curriculum may have different copyrights and licenses. This document paraphrases concepts independently and links to the original rather than reproducing copyrighted course contents. If redistributing adaptations of the original syllabus, retain attribution and comply with the ShareAlike license.

## Priorities for Veil Reader (18 selected engineering patterns)

| ID | Priority | Curriculum basis | Proposed Veil Reader application | Required proof / success gate |
|---|---|---|---|---|
| CU-01 | P0 | Algorithmic complexity / Big-O | Analyze shelf search, filtering, sorting, grouping and reading session queries under growing library sizes | Benchmark 100, 1,000, 10,000 item synthetic datasets; show p50/p95 latency, allocation and query plans where applicable |
| CU-02 | P0 | Hash tables, maps, sets | Audit lookup and dedup index for publication metadata; keep existing SHA-256 fingerprint semantics | Duplicate imports never produce duplicate logical records; collision and invalid metadata cases covered |
| CU-03 | P0 | Caches and locality | Audit EPUB render/page texture, PDF image memory, thumbnail caches, book search caches | Cache budgets, invalidation and eviction rules documented; trim-memory tested; no leaked bitmap/texture |
| CU-04 | P0 | Processes and threads | Review page-turn, render, reader-close, background write and lifecycle races | Activity pause/restart/kill tests show last committed locator survives; main thread never blocked by file I/O |
| CU-05 | P0 | Testing | Build invariants & failure-mode matrix for import, annotations, backups, restore and rendering | Unit+instrumentation tests for interrupted writes, corrupt input, process recreation and restore round trip |
| CU-06 | P0 | Unicode, string searching | Mixed Persian/English/Arabic book content, bidirectional titles, search normalization | RTL/LTR and mixed-script visual/screen reader tests; no accidental text corruption or normalization loss |
| CU-07 | P0 | Binary search / sorted ranges | Improve navigation to known progress/locator positions when source data is sorted; avoid O(n) scan in hot path | Profiling on very long books proves improvement before replacing existing Readium navigation |
| CU-08 | P1 | Tries / prefix search | Consider on-device incremental book-title/author search if profiling shows existing SQL/FTS insufficient | Keyboard-to-result latency measured; feature respects privacy and correct non-Latin tokenization |
| CU-09 | P1 | Graphs, BFS/DFS | Relationship navigator for book series, collections and optional entity/lore associations | Graph view stays optional; cyclic/missing relations handled; no invented book relationships |
| CU-10 | P1 | Heaps and priority queues | Priority scheduling for visible covers, immediate chapter loads and prefetching | Visible read content wins over speculative jobs; starvation/cancellation tests |
| CU-11 | P1 | Sorting and stable ordering | Stable ordering for shelves and series (natural sorting, tied timestamps, mixed language) | Deterministic sorting through app restarts and migrations; locale-aware requirements defined |
| CU-12 | P1 | Dynamic programming | Only investigate for a concrete layout or optimization problem; do not introduce gratuitous DP | Benchmark justification and correctness tests required before implementation |
| CU-13 | P1 | System design, scalability, data handling | Offline-first architectural review across Room/DataStore/file cache/backups/optional sync | Data ownership, transaction boundaries, reversible migrations, 1k-book QA evidence |
| CU-14 | P1 | Compression | EPUB/ZIP decompression, archive validation, bounded memory and size expansion controls | Size/path limit tests, decompression bomb defense, safe failure for corrupt files |
| CU-15 | P1 | Networking and security | Future optional account/sync and external TTS; minimize telemetry, credentials, data exfiltration | Threat model and consent gate before enabling anything that leaves device |
| CU-16 | P1 | Garbage collection / memory | Identify allocation spikes from Compose recomposition, bitmap conversion and page curl textures | Heap/memory profiler evidence from repetitive reading, orientation and import cycles |
| CU-17 | P2 | Parallel programming | Consider concurrent cover extraction, indexing and metadata parsing with bounded work | CPU, battery and thermal metrics; cancellation; maintain smooth foreground reading |
| CU-18 | P2 | Probability / combinatorics | Use for quantitative testing and sampling only, never random loot-box or misleading gamification | Deterministic tests for progression and fairness; no hidden addictive mechanics |

### Direct source topic anchors

- [Algorithmic complexity](https://github.com/jwasham/coding-interview-university#algorithmic-complexity--big-o--asymptotic-analysis)
- [Data structures](https://github.com/jwasham/coding-interview-university#data-structures)
- [Trees](https://github.com/jwasham/coding-interview-university#trees)
- [Sorting](https://github.com/jwasham/coding-interview-university#sorting)
- [Graphs](https://github.com/jwasham/coding-interview-university#graphs)
- [Caches](https://github.com/jwasham/coding-interview-university#caches)
- [Processes and Threads](https://github.com/jwasham/coding-interview-university#processes-and-threads)
- [Testing](https://github.com/jwasham/coding-interview-university#testing)
- [Unicode](https://github.com/jwasham/coding-interview-university#unicode)
- [System Design](https://github.com/jwasham/coding-interview-university#system-design-scalability-data-handling)
- [Parallel Programming](https://github.com/jwasham/coding-interview-university#parallel-programming)
- [Computer Security](https://github.com/jwasham/coding-interview-university#computer-security)
- [Compression](https://github.com/jwasham/coding-interview-university#compression)

## Five candidate reusable Engineering Skills (specifications; not installed)

### ES-01 — Algorithmic Complexity Review
- Input: feature proposal, hot-path Kotlin snippet, Room query, dataset sizes.
- Output: complexity table, alternative algorithms, assumptions, before/after benchmark template.
- Guardrail: no automatic refactor; do not replace a well-tested component without an actual bottleneck.

### ES-02 — Concurrency and Lifecycle Audit
- Input: Reader state transitions (open/bookmark/pause/close/kill), dispatcher ownership, async writes.
- Output: a state/race diagram, hazards, deterministic test scripts, rejection criteria.
- Guardrail: protect durable locator, notes and database data; no device-independent claim of fixing a race.

### ES-03 — Unicode & RTL Acceptance Matrix
- Input: sample EPUB passages (English/Persian/Arabic mixed), titles, footnotes, search terms, controls.
- Output: language-specific test set for bidirectional text, normalization, traversal order and accessibility.
- Guardrail: original user text must not be mutated just to make search easier.

### ES-04 — Memory/Cache/Rendering Audit
- Input: Android heap/profile traces, page-texture pipeline, cache sizes, render timings.
- Output: cache inventory, allocation hotspots, lifecycle cleanup plan, regression benchmarks.
- Guardrail: do not prioritize decorative animation over reliable page turning.

### ES-05 — Failure-First Test Design
- Input: user journey and data invariants (book import, annotations, migration, backup/restore).
- Output: failure matrix (crash/kill/corruption/low storage/duplicate/RTL), mapped Kotlin/JUnit/instrumentation test plan.
- Guardrail: generated test suggestions are not treated as passing tests until executed in CI/device.

## Follow-on application to Eyad Studio

- **Veil Reader Grand Forge:** P0 ES-02, ES-03, ES-04, ES-05; P1 ES-01. Embed checklists in code review and PR templates after trials.
- **Novel Factory:** apply graph/cycle checks to character and timeline continuity, but pair with editorial review rather than treating algorithms as literature quality.
- **Video pipeline:** use queue/backpressure and cache/asset-expiry principles; benchmark real project sizes and frame generation steps.
- **General agent teams:** teach invariants, complexity budgets, interface contracts, predictable state transitions and test evidence. Training curriculum is not a replacement for runtime orchestration tools.

## Product-specific acceptance criteria, aligned to existing Veil roadmap

1. **Reader remains primary.** New AI or game layers must not block ordinary offline reading or obscure book content.
2. **Durability.** Reproduce import → open → annotate → close → process kill → reopen and export/restore workflows, and compare resulting logical data.
3. **Memory.** Repeat EPUB/PDF open/close and rotation cycles with heap capture; investigate sustained growth instead of assigning an invented memory target.
4. **Large libraries.** Search, scroll, filter and sort 1,000 synthetic books on representative physical Android hardware. Record p50/p95, frames and memory.
5. **RTL and Unicode.** Test Persian/English mixed chapter names, bidirectional UI controls, note selection, search and accessibility.
6. **Paper engine separation.** Exercise PAGED, PAPER CURL, SLIDE and SCROLL individually; ensure fixes in one mode do not regress the others.
7. **Production release.** Complete unit, instrumentation, accessibility and signed-build QA before any claim of production readiness.

## Explicit exclusions and pitfalls

- Do not study/port the full curriculum as a prerequisite for shipping Veil Reader; selective concepts have a much higher payoff.
- Do not add linked interview question repositories as runtime dependencies.
- Do not copy complete lists/books/tutorials into a proprietary `SKILL.md`: license and third-party rights differ.
- Do not claim the curriculum's algorithms, complexity advice or links are Android-specific tutorials.
- Do not make performance claims based on Big-O alone; measure actual device workloads.
- Do not let AI agents silently rewrite Readium, Room or the page-turn engine.

## 3-stage adoption plan

**Stage A — Evidence capture:** record current book opening/page turn timings, 1k-book library search, process-restore traces, page texture lifecycle, Unicode/RTL bugs.

**Stage B — Read-only review pilot:** run ES-01..ES-05 against real PRs and compare agent findings against human QA; classify false positives.

**Stage C — Reversible implementation:** only proven improvements become separate feature branches and guarded PRs with CI/device evidence; merge only when acceptance criteria are met.

## Attribution and provenance

Original curriculum © respective contributors of [Coding Interview University](https://github.com/jwasham/coding-interview-university), under [CC BY-SA 4.0](https://github.com/jwasham/coding-interview-university/blob/main/LICENSE.txt). The original material is a self-study course for coding interviews. This document is an independent assessment for engineering application; most project-specific acceptance criteria here are proposed by Eyad Studio and are not claims or recommendations of the original author. External links in the original curriculum require separate terms review.

**No install performed. No code copied. No verification runs or APK builds performed.**