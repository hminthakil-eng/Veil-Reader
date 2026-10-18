# Eyad Studio Research Bank — Build Your Own X

**Date:** 2026-10-18  
**Upstream:** https://github.com/codecrafters-io/build-your-own-x  
**Snapshot:** `master` README blob SHA `9a553c729b29bc69296d7092fc1c314c2c811bad`  
**Status:** Research and references only; no upstream software installed and no Veil Reader production changes.

## Scope and verification

`build-your-own-x` is an index of independently hosted, step-by-step tutorials for reimplementing technologies. The reviewed README has **359 tutorial entries and 30 topical sections** (counted from its `#### Build your own` headings and linked tutorial bullets). The list is not 359 ready-to-integrate components, not a curated Android dependency catalogue, and not proof that links are currently functional. Its introduction includes the quote attributed to Richard Feynman: “What I cannot create, I do not understand.”

The repository's top-level `LICENSE` was **not found** by the GitHub file lookup. Treat all linked tutorials and source repositories as **separately licensed**. Do not assume the entire index grants redistribution or integration rights over linked content.

## Ranked implementation research index

Rows specify **learning relevance**, not verified production compatibility. The links were present in the upstream README. Every production adaptation needs a dependency/license/security review, Android-native implementation plan, and its own objective QA evidence.

| ID | Priority | Theme | Source and URL | Concrete adaptation | Caution |
|---|---|---|---|---|---|
| BYX-01 | P0 | 3D rendering fundamentals | [tinyrenderer — OpenGL in 500 lines](https://github.com/ssloy/tinyrenderer/wiki) | Study mesh transforms, face normals, lighting, shadow and texture coordinates for paper-curl QA | Tutorial code is not an Android page engine |
| BYX-02 | P0 | Rasterization | [Scratchapixel rasterization](https://www.scratchapixel.com/lessons/3d-basic-rendering/rasterization-practical-implementation/overview-rasterization-algorithm) | Diagnose curl mesh folds, clipping, z-order, overdraw and artifacts | Do not substitute a software rasterizer for GPU rendering |
| BYX-03 | P0 | Physically based lighting | [Physically Based Rendering](https://www.pbr-book.org/) | Better material response of parchment and brass; lighting reference for page surfaces | Heavy offline reference; budget actual mobile resources |
| BYX-04 | P0 | Physics integration | [Allen Chou's game physics series](http://allenchou.net/game-physics-series/) | Model spring-damper release, drag resistance and mass perception | Rigid-body tutorials are analogies, not real flexible paper simulation |
| BYX-05 | P0 | Game physics basics | [How Physics Engines Work](http://buildnewgames.com/gamephysics/) | Build a deterministic input→animation state machine, not a scripted slide | Older browser reference; verify applicability |
| BYX-06 | P0 | Search engine | [TF-IDF text indexing](https://stevenloria.com/tf-idf/) | Scored book/note search, ranking and test corpus generation | Not a replacement for Room FTS or Readium search |
| BYX-07 | P0 | Search engine | [Vector-space indexing](https://boyter.org/2010/08/build-vector-space-search-engine-python/) | Benchmark inverted/vector retrieval concepts; offline indexing strategy | Memory, indexing time and tokenization need tests |
| BYX-08 | P0 | RAG fundamentals | [LangChain RAG from scratch](https://github.com/langchain-ai/rag-from-scratch) | Local-first spoiler-aware book knowledge prototype, with verifiable citations | Avoid sending imported user books to cloud without explicit consent |
| BYX-09 | P0 | Browser layout fundamentals | [Browser Engineering](https://browser.engineering/) | Understand line boxes, flow, images and pagination geometry to reason about EPUB reading | Do not replace Readium's standards-compliant EPUB layout |
| BYX-10 | P0 | Text editing | [Kilo: Build Your Own Text Editor](https://viewsourcecode.org/snaptoken/kilo/) | Better note-editing UX, cursor/selection states, undo/redo acceptance cases | Terminal C editor is learning material only |
| BYX-11 | P1 | Character handling / regex | [Russ Cox — Regular Expression Matching Can Be Simple And Fast](https://swtch.com/~rsc/regexp/regexp1.html) | Avoid catastrophic regex backtracking in large-book search | Validate Unicode, Persian, Arabic and mixed-script behavior |
| BYX-12 | P1 | Database internals | [Build Your Own Database — B+Tree to SQL](https://build-your-own.org/database/) | Understand indexing and query plans; improve 1,000+ book library benchmark | Retain Room/SQLite as canonical storage |
| BYX-13 | P1 | Persistence | [Let's Build a Simple Database](https://cstack.github.io/db_tutorial/) | Study transactions, page layouts and corruption failure cases | Do not introduce a custom DB in Veil Reader |
| BYX-14 | P1 | Engine architecture | [Crafting Interpreters](https://craftinginterpreters.com/) | Define deterministic command parsing for optional in-app rituals, interactions and QA scripting | Avoid speculative new DSL in core reader |
| BYX-15 | P1 | 3D representation | [Computer Graphics from Scratch](https://www.gabrielgambetta.com/computer-graphics-from-scratch/introduction.html) | Prototype a low-cost 3D room or station sign under Veil aesthetic | Prototype only; evaluate performance/accessibility |
| BYX-16 | P1 | Scene/game fundamentals | [LearnOpenGL Breakout](https://learnopengl.com/In-Practice/2D-Game/Breakout) | Game-loop, animation separation and deterministic reward feedback | No new full game engine solely for gamification |
| BYX-17 | P1 | RPG systems | [Learn C# by Building a Simple RPG](http://scottlilly.com/learn-c-by-building-a-simple-rpg-index/) | State machine and progression-design educational reference for Castle/Path | The source may be dated or unavailable; re-verify link |
| BYX-18 | P1 | Git internals | [Write Yourself a Git](https://wyag.thb.lt/) | Improve agent collaboration, conflict diagnostics and safe rollback understanding | Do not replace GitHub canonical repo |
| BYX-19 | P1 | AI fundamentals | [LLMs from scratch](https://github.com/rasbt/LLMs-from-scratch) | Evaluate prompt/context constraints in Novel Factory; build small controlled educational experiments | Training from scratch is not practical for production with limited VRAM |
| BYX-20 | P1 | Diffusion | [Hugging Face Diffusion Course](https://huggingface.co/learn/diffusion-course/en/unit1/3) | Better evaluation of image-generation limitations and consistent art assets | Do not assume model weights/services are free or license-compatible |
| BYX-21 | P1 | Animation / UI frameworks | [Build your own React](https://pomb.us/build-your-own-react/) | Understand reconciliation/state and avoid excessive rerenders in website prototypes | Veil Reader Android UI uses Compose; concepts only |
| BYX-22 | P2 | Web rendering | [Let's build a browser engine](https://limpet.net/mbrubeck/2014/08/08/toy-layout-engine-1.html) | Visual QA experiments around HTML/CSS rendering | Toy browser cannot replace Readium/WebView |
| BYX-23 | P2 | CLI integration | [Command-line apps in Rust](https://rust-cli.github.io/book/index.html) | Building reusable offline benchmarking/reporting CLIs | Follow reuse-first, avoid an extra language stack unless justified |
| BYX-24 | P2 | Games | [Roguelike RPG tutorial](https://www.youtube.com/playlist?list=PLkTXsX7igf8erbWGYT4iSAhpnJLJ0Nk5G) | Optional procedural mini-events in reading-world prototype | Not a production game roadmap or core reading priority |

### Mapping to Eyad Studio projects

**Veil Reader / Paper Engine:** BYX-01–05 plus BYX-02, BYX-03: physically plausible page curl, fixed touch ownership, cancel/release, LTR/RTL, no tears, shadows, page-thickness perception. Keep `PAGED`, `PAPER CURL`, `SLIDE`, and `SCROLL` isolated. Evaluate on real hardware before any renderer replacement.

**Veil Reader / Library, notes and search:** BYX-06–13: use Room/FTS and existing Readium features first; add citation-friendly retrieval only when real book-structured evaluation proves benefits. Unicode normalization and query latency benchmarks are gates.

**Veil Reader / Castle, Grayfog, railway station:** BYX-15–17 & 21: the art/interaction language can borrow mathematics of transforms and timing while preserving one-tap continuation of reading, battery, accessibility and reduced motion.

**Novel Factory:** BYX-08, 14, 19: book-grounded retrieval, lore/continuity checks and deterministic workflow scripting; a reliable source-of-truth for story state matters more than novel model training.

**Motion/Video Studio:** BYX-03, 15, 20: material design, cameras, lighting and still-image/diffusion evaluation as preproduction ideas. No promise of video-generation capability solely from these tutorials.

**Agent/Engineering division:** BYX-12, 18, 23: internal tools, rollback, data indexes and easier system debugging. Use existing workflow, GitHub PRs and CI as acceptance gates.

### Reusable skill candidates (definitions, not installations)

1. **Material Page Physics Reviewer:** Review gesture-to-curl state, mesh projection and light response against physical and interaction constraints; produce test cases.
2. **Reader Search & Unicode Auditor:** Check mixed Persian/Arabic/English text indexing, search ranking, cancellation, large files and responsiveness.
3. **GPU Frame Budget Auditor:** Compare memory peak, allocation churn, frame pacing and texture size before/after; no unmeasured '60 fps' claims.
4. **Castle 3D Interaction Gate:** Verify world navigation is optional, low distraction, accessible, skippable and not a replacement for main library UX.
5. **Build-from-Scratch vs Reuse Gate:** For every custom engine, document industry solutions, licenses, performance tests, rollback and why custom code is necessary.

## Proposed reversible pilots

| Pilot | Artifact | Test gate |
|---|---|---|
| Pilot A — Paper feel physics | Self-contained Kotlin/Compose sandbox and paper physics traces | Reproducible drag/release, cancellation, right-to-left, frame time on device |
| Pilot B — Search benchmark | Standalone, fake-content 1,000-book dataset | p50/p95 queries, Unicode exactness, no regression vs current Room/Readium |
| Pilot C — 3D atmospheric preview | Separate Grayfog test scene, feature flag off | No impact on reader startup / accessibility / battery, fallback 2D |
| Pilot D — EPUB layout reference | Test corpus with Unicode, fonts, images, footnotes | Existing Readium behavior preserved, no parallel rendering stack |

## Exclusions and caveats

- Do not copy code without verifying each linked project's own license; the index is a bibliography rather than a blanket permission.
- Do not use tutorial renderers as production EPUB/PDF engines.
- Do not swap Room/SQLite for a hand-rolled database.
- Do not build a local LLM from scratch just because a tutorial exists: GPU budget, data, eval and maintenance argue against it.
- No modifications to `Veil Reader` app or its Android build in this research item.
- Link health and release readiness of upstream tutorials have **not** been exhaustively validated; several are old tutorials. Four sample GitHub repositories (`ssloy/tinyrenderer`, `langchain-ai/rag-from-scratch`, `rasbt/LLMs-from-scratch`, `kanaka/mal`) were checked as existing, not archived, at review time. Their runtime behavior was not tested.

## Suggested implementation order

1. Keep existing Reader v0.8/RC intact and avoid touching working page-turn UX.
2. Perform deterministic microbenchmarks of current page drag/release and search.
3. Run paper feel / shadows experiment behind a flag, then separately a visual Castle preview.
4. Only accept designs that pass device/performance/RTL/accessibility and no-data-loss gates.
5. Promote through normal PR review and CI. No experimental reference should merge on aesthetics alone.