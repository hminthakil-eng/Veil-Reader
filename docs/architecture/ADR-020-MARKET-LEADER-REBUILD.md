# ADR-020 — Premium Reader Rebuild Without Destroying Proven Core

Status: PROPOSED (not accepted, not built)
Branch: `design/market-leaders-rebuild-20261017`
Date: 2026-10-17

## Context

Veil Reader uses native Kotlin/Compose, Readium EPUB/PDF, Room local state, an existing GPU material paper engine and four navigation modes. A prior HOME/Investigation visual review was RED. User rejects generic, unfinished UI. A full replacement of Readium or Room offers little demonstrated value while increasing data-loss and regression risk.

## Decision

Adopt a *strangler/reversible migration*: evolve design tokens, presentation and isolated capabilities behind a debug-only experience flag while keeping existing book import, immutable locators, annotation persistence, TTS ownership, navigation transaction logic and original reader mode engines functional. Do not replace all components in one commit.

### Layer contracts

- **Reader core**: Publication sessions, source bytes, EPUB CFIs/locators, progress durability, bookmarks, highlights, search, PDF pages. Only one authority for reader position.
- **Transition runtime**: Separate layout mode (PAGED/SCROLL), transition kind (NONE/SLIDE/PAPER_CURL), and publication capability. Existing public choices map losslessly to this internal model. Gesture ownership is exclusive per frame.
- **Rendering**: Readium text remains crisp and readable. Paper curl receives source and destination page snapshots tied to session generation and acquisition epoch; stale bitmap/render callbacks are ignored and recycled.
- **Navigation transactions**: Every asynchronous link jump carries a monotonically increasing transaction generation, source anchor, destination and lifecycle owner. Back/cancel invalidates callbacks; a commit is idempotent and locator-saving is durable before teardown.
- **Presentation**: Compose themes/tokens own identity, with a distinct archival shell and a calm reading canvas. No hardcoded mocked book progress, inferred highlights, random ranks, or default revealed private clues.
- **Experience modules**: Library and reader work without network. Audio, smart translation, parallel view, syncing and world progression are optional plugins/features, do not own the canonical reading anchor.

### Reader mode invariants

| Mode | Layout | Transition | When available |
|---|---|---|---|
| PAGED | paginated | none or instant | all supported reflowable books; fallback for fixed spreads |
| PAPER_CURL | paginated | physically deforming sheet | only reflowable EPUB with a warmed, valid, available paper renderer |
| SLIDE | paginated | horizontal translation | eligible reflowable EPUB and stable destination |
| SCROLL | continuous | none | reflowable EPUB where supported |

A material/feature gate may disable PAPER_CURL; display why, and offer a truthful, distinct fallback. Never substitute a slide while labeling it physical paper. A fixed-layout EPUB cannot pretend a full two-page spread is a single paper leaf.

### Release gates in order

0. **Design RED resolution**: Actual runtime screenshot/video from phone; editable design comparison and explicit visual approval. Respect privacy defaults and accessible data correctness.
1. **Static/unit**: Mode handoff, publication capability, transition request races, stale GPU snapshots, restore after process death; targeted JUnit results from current commit.
2. **Instrumentation**: Real EPUB LTR/Persian RTL curl images at origin/held/reversed/committed, no double turn, no blank textures or back-face text corruption. Open/close/resume and PDF zoom tests.
3. **Accessibility/adaptive**: TalkBack actions and focus, RTL, 200% font, system Back, reduced motion, foldable hinges/IME and tablet behavior.
4. **Performance**: Actual startup and reading warm/cold latency; jank and frame-time distribution on mid-tier and premium Android devices with/without baseline profile. No fixed promises based on desktop mock data.
5. **Competitive benchmark**: Identical tasks on Veil, Android Kindle, Moon+, ReadEra, and, where relevant, Apple Books/Kobo (different device platform caveat). Blinded paired usability visual assessments, qualitative notes and results. Publish methods, exceptions and confidence.
6. **Candidate promotion**: Separate reviewable PR with test evidence, licensing, rollback switch, upgrade/migration safety. Keep the current original reader install usable.

### Highest-return first implementation slices

- Slice A (Visual Baseline): Rework *only* HOME/Archive card grammar and Investigation Desk's sealed/revealed cards in an isolated branch after genuine visual target approval. Preserve production data wiring.
- Slice B (Paper Proof): Existing engine remains; produce reproducible RTL/LTR screen evidence and address the most severe measured gesture/renderer problem, not imagined ones.
- Slice C (Quick Controls): One-gesture reader appearance sheet with live preview, stable text anchor, global vs book-specific precedence, system Back and accessibility.
- Slice D (Reliable Library): Instant resume, fast filter and import error/duplicate states, offline-first robust book handling.
- Slice E (Optional Intelligence): Start with local dictionary, highlights export, TTS reliability and only later opt-in translation/sync/parallel reading.

### Explicit non-goals

- Rewriting proven Readium, implementing a new cross-platform runtime or moving core reader to Flutter/React.
- Copying Apple/Kobo/Amazon/Moon+ assets or AGPL Readest source without licensing decision.
- Publishing an unverified APK, marking visual RED green without on-device evidence, auto-adopting SkillOpt prompts, or using private reading content for unconsented telemetry.

### Risks

1. Visual direction could regress text legibility and Android accessibility; use real screen evidence, not a color palette alone.
2. GPU curl can be technically active but visually blank if snapshots or texture coordinates fail; assert actual ink in screenshots.
3. Feature sprawl can displace reliability, and users may resent forced gamification; ship in slices with opt-out.
4. Source-only checks and AI evaluations can overstate UX readiness; report separately with honest gates.
5. Current desktop has ~1.1–1.3GB RAM free, so Gradle/emulator should remain blocked pending a >=4GB safe resource budget; never kill user applications by default.

## Open decisions

- Visual reference approval and real installed screenshots remain pending.
- Official test device availability / permission to free memory or use remote build runner.
- Per-slice quality gates and budgets must be calibrated from current device benchmarks before performance promises.
