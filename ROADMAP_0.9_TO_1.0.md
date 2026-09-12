# Veil Reader — v0.9 → 1.0 World-Class Roadmap

## Why this roadmap exists

Veil Reader 0.8 proves the core product works: real EPUB/PDF reading, durable Room/DataStore persistence, annotations, bookmarks, backup/restore, rich library metadata, duplicate-safe imports, six Paths, rituals, and Castle progression.

The next problem is not “add more features.” The next problem is that the product experience does not yet communicate the ambition of the underlying system. The visual language is still close to stock Material, the Castle is mostly a list of cards, and progression is presented as numbers and bars rather than a world that visibly changes because the user reads.

The goal of v0.9 is therefore **The Awakening**: turn a capable reader into a distinctive reading world without compromising reading quality, accessibility, offline behavior, or data durability.

The goal of 1.0 is to earn “world-class Android reader” status through measured performance, accessibility, physical-device QA, release engineering, and a cohesive product identity.

---

## Product north star

> Opening Veil Reader should feel like entering a private library that remembers everything you have read and slowly transforms into a mysterious world of your own.

### Five pillars

1. **Reading first** — no game UI may obstruct the active book.
2. **The library is alive** — books, collections, history, and progress should visibly shape the app.
3. **The Castle is earned** — rooms evolve from real reading milestones, not arbitrary tapping.
4. **Mystery over noise** — discovery, anticipation, atmosphere, and subtle motion instead of constant reward effects.
5. **Premium Android craftsmanship** — accessible, fast, adaptive, offline-first, and reliable on ordinary hardware.

---

## Current baseline: 0.8

Already strong enough to build upon:

- Readium EPUB/PDF parsing/navigation.
- Durable reading position and lifecycle-aware sessions.
- TOC, search, highlights, notes, bookmarks, Hidden Archive.
- Room structured storage and DataStore preferences.
- Migration from older storage.
- Validated transactional ZIP backup/restore.
- Real cover extraction and private thumbnail cache.
- Rich metadata, multi-collections, series sorting/editing.
- SHA-256 duplicate detection.
- Six Paths, XP, streaks, quests, rituals, sigils, Castle rooms.
- CI with unit tests, lint, APK build, Room schema verification, and storage instrumentation.

What 0.8 does **not** yet prove:

- a distinctive visual/design system;
- a world-like Castle rather than a menu of rooms;
- deep emotional connection between reading and progression;
- large-library performance on physical midrange hardware;
- accessibility sign-off;
- tablet/foldable quality;
- signed production release pipeline and Play internal testing.

---

# v0.9 — THE AWAKENING

## 0.9A — Veil Design System

### Goal
Create a reusable visual language before redesigning individual screens.

### Deliverables

- Replace default `Typography()` with a deliberate Veil typography scale.
- Introduce semantic design tokens: background depths, parchment surfaces, mist layers, gold/jade accents, danger/ritual states, separators, elevation, corner families, spacing, icon sizing.
- Define three visual modes:
  - **Archive** — warm parchment/ink.
  - **Nocturne** — deep charcoal/violet/mist.
  - **OLED Veil** — true black with restrained luminous accents.
- Create reusable components:
  - VeilTopBar
  - VeilSectionHeader
  - VeilBookCard / VeilBookCover
  - VeilProgressSigil
  - VeilRoomCard
  - VeilAchievementToken
  - VeilEmptyState
  - VeilBottomSheet
  - VeilDialog
- Replace emoji as primary visual identity with original vector/iconography.
- Add reduced-motion behavior to every new animated component.
- Maintain 48dp targets, TalkBack semantics, scalable typography, and contrast.

### Exit gate
Reading Now, Library, Path, Castle, and Profile can all be built from the same component/token system without one-off styling.

---

## 0.9B — Reading Now becomes the “Threshold”

### Goal
Make the home screen immediately feel like Veil Reader rather than a dashboard.

### Experience

- Current book is the visual hero.
- Cover sits in a quiet atmospheric stage, not inside a generic card.
- Show only the most useful reading information: title, author, progress, current chapter when available, last reading session, estimated continuation cue.
- Primary action: **Continue**.
- Secondary layer: Path state shown as a compact sigil + rank, not a large progress dashboard.
- Daily quests move lower and become optional “Whispers,” reducing game-like pressure.
- Recent books appear as a visually lighter shelf.
- Empty state becomes an onboarding moment into the world, not a blank card.

### Motion

- 180–280ms navigation/element transitions.
- Slow ambient motion only on decorative layers.
- No motion required to understand state.
- Reduced-motion setting removes ambient drift and shortens transitions.

### Exit gate
A new user understands “import a book and read” immediately; a returning user can resume reading in one tap.

---

## 0.9C — Grand Library: from grid to personal archive

### Goal
Keep the large-library utility already built, but give it hierarchy and identity.

### Deliverables

- Two primary modes:
  - **Library** — scalable grid/list management.
  - **Shelves** — curated collections and smart reading groups.
- Smart shelves derived locally from existing data:
  - Continue Reading
  - Recently Added
  - Finished
  - Favorites
  - Series
  - By Collection
- Bulk actions and clear remove/recovery semantics.
- Stronger empty/search/no-result states.
- Long-title, RTL, mixed-language and large-text handling.
- Preserve fast search/filter/sort for large libraries.

### Exit gate
The screen remains useful at 1,000 books and still feels like a personal archive rather than a file manager.

---

## 0.9D — Castle 1.0 MVP: make it a place

### Goal
Transform Castle from a vertical list of room cards into an explorable visual hub.

### Interaction model

Use a **2D illustrated/diagrammatic Castle map**, implemented with Compose rather than a game engine. Rooms are positioned in a coherent castle layout and can be tapped. Locked rooms remain visible as silhouettes so progression has anticipation.

### Castle layers

1. **Outer Keep / Grand Library** — books, collections, completion history.
2. **Ritual Chamber** — Path advancement and ritual progress.
3. **Observatory** — real analytics from reading sessions.
4. **Hidden Archive** — highlights, notes, bookmarks, entities later.
5. **Treasury** — earned visual cosmetics and collectibles.
6. **Inner Sanctum** — late-rank challenges and long-term mastery.

### Evolution rules

Castle appearance should respond to durable milestones:

- books completed;
- total engaged reading time;
- Path rank;
- annotation milestones;
- genre/collection breadth;
- achievements/sigils;
- completed series.

Do not reward raw page tapping.

### Technical rule
Do not introduce a game engine. Start with Compose Canvas/layers, vector assets, subtle parallax, and ordinary navigation. Only add a specialized animation dependency if a concrete design cannot be achieved cleanly without it.

### Exit gate
A user can visually see that their reading history changed the Castle without reading a number.

---

## 0.9E — Path progression becomes ceremonial

### Goal
Make rank advancement memorable but rare, while keeping reading interruption-free.

### Deliverables

- Path sigil component and original icon for each Path.
- Rank names/visual states become consistent across home, Castle, and Profile.
- Ritual progress represented through a symbolic object/state where possible instead of only a percentage bar.
- Rank-up ceremony occurs only after the user intentionally enters the Ritual Chamber.
- Ceremony is skippable and reduced-motion compatible.
- Missing a day never destroys progress.
- Add game visibility / Quiet Mode.

### Exit gate
Progression feels connected to reading behavior and can be entirely hidden without harming the reader.

---

## 0.9F — Mysteries and discoveries

### Goal
Introduce the “veil” through discovery rather than more chores.

### First mystery system

Use deterministic, offline conditions based on existing local data. Examples:

- finish the first trilogy/series;
- annotate across three different books;
- revisit a finished book;
- read across three Path-related genres/collections;
- complete a ritual with unusually strong evidence;
- finish a long book after multiple sessions.

Rewards may unlock:

- Castle visual details;
- shelf treatments;
- sigils;
- reader themes;
- room artifacts;
- lore fragments.

No random loot boxes, paid keys, or FOMO expiration.

### Exit gate
At least five discoveries can occur naturally through reading, with no notification spam and no requirement to grind.

---

## 0.9G — Reader immersion polish

### Goal
Improve the part of the app where the user spends the most time, while keeping the book visually dominant.

### Deliverables

- Refine reader chrome hierarchy and tap zones.
- Improve typography controls where Readium supports them.
- Better current chapter/progress/time-left presentation.
- Faster notebook/search navigation.
- Better selection/highlight/note workflow.
- Clear EPUB/PDF capability differences.
- RTL, mixed-script, footnote, image/table, fixed-layout, and large-PDF QA matrix.
- Haptics only for meaningful controls; never on page turns by default.

### Exit gate
Reader controls feel quiet, intentional, and fast enough to disappear mentally during long sessions.

---

# 0.9H — Performance and adaptive quality

### Measure first

Add a benchmark/profile track before claiming optimization.

Critical user journeys:

1. cold app launch;
2. open Reading Now;
3. scroll a 1,000-book Library;
4. search/filter the Library;
5. import an EPUB;
6. open EPUB reader;
7. open large PDF;
8. navigate into Castle;
9. open Hidden Archive with large annotation set.

### Deliverables

- Macrobenchmark module.
- App-specific Baseline Profile.
- Release/R8 benchmark variant.
- Record TTID/TTFD, frame timing/jank, and memory where useful.
- Adaptive layouts for phone, tablet, and foldable-width classes.
- Physical-device test on at least one midrange Android device class.

### Exit gate
Performance changes are backed by before/after measurements, not intuition.

---

# 1.0 — THE FIRST TRUE VEIL RELEASE

1.0 is not a feature dump. It is the release that proves the whole product is safe, polished, accessible, and coherent.

## 1.0 release gates

### Reading quality

- representative reflowable EPUBs pass;
- fixed-layout EPUB behavior documented and verified;
- large PDFs remain usable;
- corrupt/encrypted/unsupported files fail safely;
- reading position survives process/activity recreation;
- external file-open flows work reliably.

### Accessibility

- TalkBack pass on all primary destinations;
- large text does not clip essential actions;
- contrast reviewed;
- focus order sensible;
- reduced motion respected;
- touch targets appropriate.

### Data durability

- migration fixtures from every released storage/schema state;
- backup → wipe → restore round-trip proven;
- restore failure rolls back safely;
- imported publications remain private by default.

### Performance

- release/R8 measurements captured;
- app-specific Baseline Profile shipped if measurement proves value;
- 1,000-book library tested;
- large annotation archive tested;
- no known severe jank in critical journeys.

### Release engineering

- reproducible signed release build;
- user-controlled signing credentials only;
- R8/resource shrinking verified;
- Play internal/closed test;
- privacy policy and support path;
- store icon/screenshots/copy;
- crash/data-loss/accessibility blockers resolved.

---

# Architecture guardrails for v0.9/1.0

Do not perform an architecture rewrite merely because the product is growing.

Keep:

- one app module until build/team scale proves otherwise;
- Room as structured source of truth;
- DataStore for preferences;
- imported publications in app-private filesystem;
- Readium behind the current gateway/engine boundary;
- ViewModel/state-holder approach already introduced;
- offline-first behavior as the default.

Add only when justified:

- benchmark/profile module for performance work;
- navigation/library state abstraction where current central state becomes a real problem;
- dedicated design-system package as shared UI grows;
- schema changes only with migration tests and backup compatibility analysis.

Do **not** add cloud sync, external AI, social features, advertising, telemetry, or a game engine during v0.9 unless explicitly re-scoped as a separate track.

---

# Development sequence

Work in vertical slices. Each slice should include data/logic/UI/tests/accessibility where relevant.

Recommended order:

1. Design tokens + reusable Veil components.
2. Reading Now redesign.
3. Grand Library visual hierarchy + bulk/recovery actions.
4. Castle map MVP.
5. Path sigils + Ritual Chamber ceremony.
6. Mystery/discovery engine.
7. Reader immersion polish.
8. Quiet Mode + accessibility cleanup.
9. Macrobenchmark + Baseline Profile + adaptive layouts.
10. Signed release/closed-test pipeline.
11. Physical-device release-candidate QA.

Avoid parallelizing two foundation changes in the same PR.

---

# Definition of done for every v0.9 feature

A feature is done only when:

- it solves a clear user goal end-to-end;
- reading continuity is preserved;
- data durability is preserved;
- it has empty/loading/error/restore states where applicable;
- TalkBack, large text, touch target, contrast, and reduced-motion impact were checked;
- relevant automated tests pass;
- performance claims are measured when performance is part of the change;
- no silent permission/privacy/dependency expansion occurred;
- CI is green.

---

# After 1.0

Only after the core is stable should Veil expand into independent tracks:

- manga / CBZ / webtoon;
- TTS and audiobooks;
- optional cloud sync;
- book-grounded spoiler-safe AI;
- reading circles/social;
- richer Castle environments and seasonal world events.

Each expansion gets its own privacy, migration, accessibility, and performance gate.
