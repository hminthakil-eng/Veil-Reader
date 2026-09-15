# Veil Reader — Product Blueprint 2.0

## Identity

Veil Reader is a premium Android **reading RPG**: a serious universal reader for books, manga and webtoons where real reading drives a persistent role-playing world. The reader remains calm while the world around it becomes deep, coherent and game-like.

## Three product pillars

1. **Universal Reader** — EPUB, PDF, CBZ/CBR/image chapters, manga right-to-left, webtoon vertical scroll, local/imported libraries and later compatible online source adapters.
2. **Living RPG World** — character identity, Path/class, attributes, quests, factions, companions, discoveries, Castle evolution, rituals and narrative progression tied to genuine reading behavior.
3. **Source Layer** — a replaceable provider architecture inspired by modern manga readers, with clear trust/security boundaries. Official builds should favor user-owned, licensed, open or explicitly connected sources; third-party source adapters must be separable from the core reader.

## Reading-first rule

Game systems may motivate entering or returning to a book, but must never cover pages, interrupt an active chapter, punish a missed day, require repetitive tapping, or reward fake page-turning. Reading progress must be based on durable session evidence rather than raw taps.

## Core reading modes

- Reflowable EPUB with typography, pagination, highlights, notes and search.
- PDF with robust large-file handling.
- Manga/comic mode with single page, double page, RTL/LTR direction, fit modes, preloading and image quality controls.
- Webtoon mode with continuous vertical scrolling and efficient image loading.
- Local CBZ/CBR/image folders and imported archives.
- Future source-provider mode for catalog/search/chapter retrieval without coupling source logic to the reader engine.

## RPG framework

### Player identity
The user creates a persistent reader-character with an Origin, Path, rank, traits, inventory and Chronicle. Cosmetic identity should be expressive without affecting reading access.

### Paths
Oracle, Dreamwalker, Archivist, Vanguard, Nocturne and Artificer remain, but now function as true RPG classes. Each Path gets distinct quests, progression evidence, rituals, abilities and world reactions rather than only themed XP labels.

### Attributes
Progression can develop attributes such as Insight, Resolve, Memory, Curiosity, Discipline and Imagination. Attributes are earned from meaningful reading patterns: finishing chapters/books, annotating, recalling, exploring genres, returning to difficult works and completing long-form journeys.

### Quests
- Story quests advance Veil lore.
- Reading quests are generated from the user's actual library and current books.
- Path quests express class identity.
- Exploration quests encourage healthy discovery without forcing daily engagement.
- Boss/ritual encounters represent major milestones and require evidence of reading, not combat grinding.

### World state
The Castle is the persistent hub, but it expands into a living world. Rooms, NPCs, factions, artifacts, environmental states and narrative branches react to the user's reading history. Books become journeys that leave permanent traces in the world.

### Companions and NPCs
NPC relationships progress through reading milestones and choices made outside the active reader. Companions can specialize in discovery, recall, statistics, recommendations or lore, but cannot interrupt reading sessions.

## Manga source architecture

The core app owns reader UI, library, downloads, history, tracking state and RPG progression. Source implementations sit behind a `ContentProvider` boundary that can expose catalogue search, metadata, chapter lists and page streams.

Source providers must be removable without corrupting the library. The system should support trust levels, per-provider permissions, network-domain visibility, versioning, health status, rate-limit handling and clear user control. The official app must not depend on any single scraping implementation to remain functional.

Research must compare three approaches before production integration: separate extension APKs, signed in-app provider packages, and declarative/remote adapters. Security, Play distribution, maintainability, licensing and source breakage are release gates.

## Castle and world

- Grand Library — personal collection, history, shelves and completed journeys.
- Ritual Chamber — Path advancement and milestone ceremonies.
- Observatory — reading analytics, prediction journal and long-term patterns.
- Hidden Archive — notes, highlights, entities, maps and discovered lore.
- Treasury — earned cosmetics, artifacts and environmental upgrades.
- Guild Hall — factions, NPC relationships and quest chains.
- Gatehouse — manga/source discovery and connected catalogues.
- Inner Sanctum — late-rank challenges and long-term mastery.

## Ethical progression

No paid XP, loot boxes, streak deletion, FOMO timers or mandatory daily quests. Progress cannot be purchased. The game rewards reading quality, consistency and exploration while allowing Quiet Mode to hide all RPG surfaces without weakening the reader.

## AI layer

Later AI features may include book-grounded Q&A, spoiler-safe summaries, entity indexing, recall challenges, recommendation reasoning and Chronicle narration. AI must remain grounded in user-owned/permitted text and the user's current reading position.

## Release direction

### 0.9 — Foundation of the Reading RPG
Unify design system, reader quality, measurable performance, persistent RPG data model, Castle/world prototype and manga/comic rendering foundation.

### 1.0 — Universal Reader + RPG Core
Production-grade EPUB/PDF/CBZ/manga/webtoon reading; coherent Path/class progression; quests, rituals, Chronicle and evolving Castle; accessibility, backup/restore and physical-device QA.

### 1.1 — Source Provider Platform
Provider SDK/boundary, source manager, trust/permission model, online catalogue UX, chapter update/download flow and at least one safe reference provider implementation.

### Later
Optional sync, tracking services, richer world/NPC systems, audiobooks/TTS, spoiler-safe AI and social reading systems.