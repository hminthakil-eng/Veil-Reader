# Build Your Own X → Veil Reader / Eyad Studio Reuse Lab

Research date: 2026-10-18  
Upstream: https://github.com/codecrafters-io/build-your-own-x  
Upstream README blob SHA: `9a553c729b29bc69296d7092fc1c314c2c811bad`  
Corpus snapshot: **30 topical headings and 359 Markdown tutorial link rows** (counted programmatically on this README snapshot). Some tutorials are outdated or inaccessible; listed links are confirmed **in upstream README**, not verified as passing builds or tests.  
Owner: Veil Reader Grand Forge / Eyad Studio SID.  
Status: **research specification; no Android feature implemented and no external code installed**.

## Core decision

**Reuse-first remains mandatory.** Build Your Own X is a curated tutorial directory, not an SDK, rendering engine, code-drop, or agent installer. Learn and prototype isolated primitives; prefer proven upstream Readium (EPUB/PDF), Room/SQLite (persistent storage), Kotlin/Compose (UI), and existing Android libraries in shipping code. Do not reproduce or bundle third-party tutorial code/assets without separately checking the tutorial's own license. A repository's lack of a root `LICENSE` is not permission to reuse code.

Do not alter production page-turn behavior when running these experiments. Separate `PAGED`, `PAPER CURL`, `SLIDE`, and `SCROLL` modes. Preserve all current progress, notes, and session counters.

## Verified-in-index selection: 24 candidate tutorials

| ID | Priority | Target | Listed resource | Experiment or reuse objective | Caution |
|---|---|---|---|---|---|
| BX-01 | P0 | Veil Paper | [Software renderer and mesh basics](https://github.com/ssloy/tinyrenderer/wiki) | Prototype shaded front/back page mesh, finite UV bounds and triangle consistency | Educational wiki, NOT a drop-in Android curl engine |
| BX-02 | P0 | Veil Paper | [Rasterization fundamentals](https://www.scratchapixel.com/lessons/3d-basic-rendering/rasterization-practical-implementation/overview-rasterization-algorithm) | Check depth interpolation, clipping, backside visibility | Tutorial may not cover Android GPU or mobile performance |
| BX-03 | P1 | Veil Paper | [Physically based rendering](http://www.pbr-book.org/) | Evaluate physically plausible directional paper lighting and shadow without expensive PBR integration | Not a license to copy assets or replace Readium |
| BX-04 | P0 | Veil Paper | [2D/3D physics foundations](http://allenchou.net/game-physics-series/) | Design stable spring and damping for gesture-release, hold, cancel | Rigidity models do not directly simulate bending sheets |
| BX-05 | P0 | Veil Paper | [Physics engine concepts](http://buildnewgames.com/gamephysics/) | Derive bounded motion and deterministic gesture-state transitions | Use small purpose-built prototype only |
| BX-06 | P1 | Veil Paper | [2D physics & spatial partitioning](http://buildnewgames.com/broad-phase-collision-detection/) | Evaluate touch hit targets in layered scene | Spatial collision broadly not needed for one page |
| BX-07 | P0 | Veil Search | [Text retrieval and indexing](https://stevenloria.com/tf-idf/) | Compare tokenization and ranking with existing local search | Prefer Android SQLite FTS and measured outcomes |
| BX-08 | P1 | Veil Search | [Vector-space retrieval basics](https://boyter.org/2010/08/build-vector-space-search-engine-python/) | Benchmark ranking on mixed-script queries and large offline book sets | Old Python tutorial; do not use wholesale |
| BX-09 | P0 | Veil Search / Novel Factory | [RAG architecture](https://github.com/langchain-ai/rag-from-scratch) | Prototype grounded answers with citations and spoiler scope | Optional and privacy-gated; book files never uploaded by default |
| BX-10 | P1 | Veil Notes | [Text editor core patterns](https://viewsourcecode.org/snaptoken/kilo/) | Study editing buffer and undo/redo for notes and highlights | Terminal-editor design not fit for Android UI directly |
| BX-11 | P1 | Veil Notes | [Modern terminal editor walkthrough](https://www.flenker.blog/hecto/) | Study cursor, selection and editing edge cases | No direct Android port |
| BX-12 | P1 | Veil 3D Castle | [Graphics from first principles](http://www.gabrielgambetta.com/computer-graphics-from-scratch/introduction.html) | Prototype depth, fog layers, camera and projection for star library | Start with Compose Canvas/2.5D, opt into 3D only if justified |
| BX-13 | P1 | Veil 3D Castle | [Ray tracing basics](https://raytracing.github.io/books/RayTracingInOneWeekend.html) | Explore light/mist look-dev with static concept prototype | Offline educational render, not realtime Android runtime |
| BX-14 | P1 | Castle mini-game | [Game logic and state](https://handmadehero.org/) | Study modular game loop and deterministic progression integration | Long C tutorial; no complete engine transplant |
| BX-15 | P1 | Castle mini-game | [Roguelike game systems](http://rogueliketutorials.com/) | Model world exploration and meaningful deterministic encounters | Avoid time pressure, gambling-like loops and intrusive UX |
| BX-16 | P2 | Castle mini-game | [2D game implementation](https://learnopengl.com/In-Practice/2D-Game/Breakout) | Study update/render separation and collision only for optional mini-games | Not relevant to the reader's core navigation |
| BX-17 | P1 | Veil data/search | [Database storage internals](https://cstack.github.io/db_tutorial/) | Strengthen understanding of page indexing and durable transactions | Do not replace production Room/SQLite |
| BX-18 | P2 | Veil data/search | [B+Tree to SQL education](https://build-your-own.org/database/) | Understand large-library indexing and key lookup tradeoffs | Training only; database reimplementation unnecessary |
| BX-19 | P1 | Reader engine QA | [Browser layout and reflow concepts](https://browser.engineering) | Isolate rendering/layout invariants and invalid document handling | Readium remains canonical EPUB renderer |
| BX-20 | P2 | Reader engine QA | [Toy layout engine](https://limpet.net/mbrubeck/2014/08/08/toy-layout-engine-1.html) | Explore pagination/reflow concepts as independent test exercises | Not an EPUB layout replacement |
| BX-21 | P2 | Eyad Studio web tools | [Rebuild a frontend component framework](https://github.com/hexacta/didact) | Understand rendering/state update mechanics for QA lab prototypes | Web scope only, not Android Compose source |
| BX-22 | P2 | Eyad Studio tooling | [Git internals](https://wyag.thb.lt/) | Reason about object integrity and branch changes in agent workflows | Educational; no Git replacement |
| BX-23 | P2 | Novel Factory R&D | [LLM from scratch](https://github.com/rasbt/LLMs-from-scratch) | Understand model/tokenization/attention when evaluating providers | Not a production model or license to train on uploaded books |
| BX-24 | P2 | Veil world/3D | [Java 2D game engine introduction](https://www.youtube.com/watch?v=025QFeZfeyM) | Compare simple rendering lifecycle and input management | Legacy Java game patterns need Kotlin/Compose adaptation |

## Four proposed independent research spikes

### Spike A — Material Paper Physics (P0)

**Question:** Can we improve the perceived weight, edge bending, hold and cancel of paper curl without a regression in readability, frame time or app lifecycle?

1. Audit existing Android PAPER CURL code, visual assets and Readium ownership. Record baseline on a physical phone before modifying.
2. Isolate a pure Kotlin gesture model: `Idle → TouchDown → Dragging → Holding → SettlingForward | SettlingBack → Idle`. Clamp release speed and normalized coordinates; stabilize `dt`; support both directions.
3. Prototype bending mesh/lighting in a debug-only surface independent of the reader; use BX-01/02/04/05 as algorithmic references. Reuse proven engine fragments where legally permissible.
4. Compare prototype versus current implementation with identical books, 200 mixed forward/back gestures, slow drags, tap, hold, cancel, orientation, RTL and LTR.
5. **Gate:** no wrong-direction page jumps, blank pages, UV inversions, dropped progress writes, reader UI interference or crashes; report frame timing, time-to-first-curl, memory, temperature and failure count on actual device. No merge on aesthetics alone.

### Spike B — Sefirah Observatory / Red Stars (P1)

**Question:** Can the star library in moving fog express the books-to-constellations concept while preserving immediate usability?

1. Start with Compose Canvas/2.5D plus optional small GPU shader. Prioritize easy tap-to-resume and accessible fallback list view.
2. One imported book ↔ one distinct pulsating red star; every collection can map to a coherent constellation. Tapping a star reveals book name and opens native reader by existing identifier.
3. Investigate BX-12 and BX-13 for parallax/depth and cinematic light as *visual research only*. Use Filament/SceneView only if profiling proves Canvas insufficient.
4. **Gate:** stable book identity after process death, no missing/duplicated stars after backup/restore, accessible text labels, reduced motion, offscreen culling, no GPU work during active reading; test with 1,000 books.

### Spike C — Offline Search / Annotations (P0)

**Question:** Do indexing and retrieval approaches improve response time and relevance over current Room/SQLite search without harming privacy and batteries?

1. Measure current author/title/notes search on synthetic data with Unicode, Persian/English mixed scripts, Arabic variants, RTL, diacritics, long titles and misspellings.
2. Compare SQLite FTS and a strictly isolated prototype using BX-07/BX-08, with a fixed query suite. Avoid regex catastrophic backtracking; limit search input.
3. Keep file content offline by default; BX-09 is optional opt-in exploration for book-grounded Q&A with per-book spoiler scope, provenance and explicit data permissions.
4. **Gate:** no incorrect title/author matches after text normalization, no progress/annotation corruption, no accidental network requests, reproducible latency/memory measurements at 1,000 books and 100,000 indexed snippets.

### Spike D — Castle Game Systems (P1)

**Question:** Can reading milestones drive compelling but deterministic and optional game progression?

1. Derive world state only from durable reading events already credited by the app, never from raw page tapping. Read the existing RPG/game packages before proposing schema changes.
2. Explore BX-14/BX-15/BX-16 for update-loop separation and game-state rules. Prototype character/item/crafting structures separately; no gambling, pay-to-win or grind-pressure loops.
3. **Gate:** 24h/session rollover, time zone changes, process death, restore, and duplicate event delivery cannot create extra rewards. Quiet Mode must hide all game chrome during reading.

## Five reusable skill specifications (not installed)

1. **Paper Material QA** — physics & frame-shape check; parameters (device/build/book/direction/gesture), evidence (video/frames/trace), P0 failure rubric.
2. **Mobile 3D Performance Gate** — scene budget, profile on low/mid/high mobile, no 3D cost on reader destination.
3. **RTL and Unicode Search Gate** — normalization and ranking fixtures; Arabic/Persian canonical-equivalence vs distinct characters policy.
4. **Offline Data Durability Gate** — process-death, backup, transaction and repeat-action invariants; 1,000 book corpus.
5. **Reuse and License Gate** — original README link, upstream project, exact revision, license evidence, alternatives, benchmark, revert path.

## Decision policy

- P0: existing reader usability, search or data durability bottleneck with concrete measurements.
- P1: major product identity and opt-in game experience after P0 stability.
- P2: training only or optional future feature.
- A proof-of-concept **never** implies user-facing completion.
- Every future change: isolated experimental branch → repeatable measurement → Android tests + physical QA → independent review → reversible integration.

## Suggested acceptance artifacts

| Artifact | Expected evidence |
|---|---|
| paper-physics-test-matrix.md | 200 gestures × 2 directions × 4 modes, video, failures, expected vs actual |
| frame-profile.json | target device, refresh rate, 99th percentile frame-time, warm/cold state, failure counts |
| search-fixture.jsonl | mixed RTL/LTR queries, expected results and ranking, corpus creation reproducibility |
| accessibility-report.md | TalkBack, large font, reduced motion, high contrast, tap targets |
| license-review.md | upstream link, pinned SHA, license text & compatibility, notices and copied assets declaration |

## References and caveats

- Index: https://github.com/codecrafters-io/build-your-own-x
- Source data for this document: upstream `README.md`, blob `9a553c729b29bc69296d7092fc1c314c2c811bad`.
- Existing Veil Reader engineering north star and 0.9/1.0 roadmap: `README.md`, `ROADMAP_0.9_TO_1.0.md`, `WORLD_CLASS_RELEASE_GATES.md` in this repository.
- Existing reader engine, source and real-device state **must be re-inspected** before changing any app code; historical notes are not live build evidence.
- Root `LICENSE` / `LICENSE.md` / `LICENSE.txt` were not found in the parent tutorial index when checked; licensing of each linked source remains separate.
