# Veil Manga Architecture v2

Status: Accepted for implementation
Date: 2026-09-20
Supersedes: initial Manga Hub architecture notes and reuse-only policy

## Goal

Build a world-class manga/webtoon capability inside Veil Reader without creating a second app architecture, forking Mihon/Kotatsu, or embedding an untrusted extension runtime.

## Core decision

Veil keeps one product architecture:

- Room is the structured source of truth.
- DataStore owns user preferences.
- repositories own durable data access.
- ViewModels own screen/business state.
- Readium remains the EPUB/PDF engine.
- Veil owns a separate image-first Manga Reader.
- manga items participate in the same Library, reading-session, progress, and gamification backbone as books.

## Stable identity model

A followed manga receives one Veil-owned stable internal Library ID.

The external source identity is a binding, not the canonical identity:

Veil Library ID
  -> MangaDex binding
  -> source B binding
  -> source C binding

This enables:
- source replacement
- source alternatives
- progress preservation
- source migration
- duplicate prevention

The current source-scoped MangaRef remains an adapter reference only.

## Persistence model

Use BookEntity as the stable Library identity for manga/comics.

Add Room-backed manga tables incrementally:
- manga_source_bindings
- manga_chapters
- manga_chapter_bindings
- manga_downloads

Do not create a parallel file/Properties database.

BookEntity progress + locatorJson and ReadingSessionEntity remain the common reader/gamification backbone.

Filesystem responsibility:
- durable downloaded page bytes
- temporary atomic download staging
- derived image cache

Room responsibility:
- source bindings
- chapter metadata
- read/progress state
- download queue/state
- durable references to downloaded assets

## Source API v2

Veil owns the source ABI.

Required concepts:
- stable MangaProviderId
- language/variant-scoped MangaSourceId
- explicit source version
- capability declaration
- suspend-based Search / Popular / Latest
- filter definitions/selections
- combined fetchUpdate(details + chapters)
- getPages
- optional URL resolution
- typed source failures

Typed failures include:
- RateLimited
- AuthRequired
- Blocked
- NotFound
- TemporarilyUnavailable
- NetworkFailure
- ParseFailure
- SourceChanged
- Unsupported

Source adapters never own UI or persistence.

## Source execution models

### Native trusted adapters — primary

Preferred for public/official APIs and high-value stable sources.

Examples:
- MangaDex

Rules:
- feature-flagged until verified
- fixture tests
- live read-only probe
- rate limits honored
- no credentials in source code

### Veil source recipes — primary scale strategy

Reuse the design lesson from Keiyoushi lib-multisrc.

Create Veil-owned reusable source families only when at least two adapters share the same pattern.

Candidate families:
- Madara
- MangaThemesia
- FoolSlide
- MangaReader-style CMS
- other audited common engines

A recipe contains shared request/parsing behavior; a site adapter should mostly provide configuration/selectors/endpoints.

### Suwayomi gateway — optional

Support a user-owned Suwayomi server over HTTPS/GraphQL as an optional source gateway.

Do not embed the third-party extension runtime inside Veil.

### Dynamic extension APK runtime — rejected for core app

Do not load arbitrary third-party extension APK/code inside Veil's process.

Reasons:
- security/trust boundary
- Play-distribution risk
- lifecycle/ABI coupling
- maintenance burden

## Reuse decisions

### Readium
Adopt for EPUB/PDF only.

### Mihon
Selective audited Apache-2.0 reuse/reference is allowed for:
- reading-mode concepts
- navigation behavior
- chapter transitions
- preload heuristics
- process/state restoration
- download/cache invalidation
- image-reader performance patterns

Do not copy wholesale:
- ReaderViewModel
- database/domain graph
- source manager
- tracker stack
- DI graph
- full activity/UI shell

### Keiyoushi/Yuzono extension ecosystem
Use heavily as audited reference for:
- source recipes
- request/response patterns
- pagination
- GraphQL/Next.js helpers
- URL normalization
- rate limiting
- source fixtures

Translate useful logic behind Veil-owned interfaces.

### Kotatsu / Kotatsu-Redo
Clean-room behavioral/UX reference only by default.

### Miyomi
Discovery/community reference only; not a Veil runtime architecture dependency.

## Manga Reader architecture

Readium and Manga Reader share UX conventions but not rendering engines.

Manga Reader dimensions are independent:

Layout:
- Paged
- Vertical pager
- Webtoon
- Continuous vertical

Direction:
- RTL
- LTR
- Vertical

Image fit:
- screen
- width
- height
- original

Page behavior:
- single
- dual
- auto-dual

Image behavior:
- crop borders
- page gap
- background
- data saver

## Image stack

Keep Coil for image loading/cache/prefetch.

Do not treat AsyncImage alone as the final manga engine.

Run a controlled benchmark for zoom/subsampling:
1. Telephoto
2. Mihon subsampling-scale-image-view fallback/reference

Test:
- very large images
- 8K pages
- long webtoons
- low-memory Android devices
- repeated zoom/pan
- offline file resources

## Reader state

Reader composition state must not be the durable source of truth.

MangaReaderViewModel should own:
- active book ID
- active chapter ID
- page index
- reader mode/direction
- transition state
- preload state
- retry/error state

Room/LibraryRepository persists durable progress.

## Chapter transitions and preload

Adopt the proven behavior pattern:
- explicit previous/next chapter transition items
- preload next chapter near the end of the current chapter
- bounded page prefetch
- never retain an entire long webtoon in memory

## Offline/download architecture

Coil cache is evictable cache, not an offline library.

Durable downloads use:
Download use case
  -> Room queue/state
  -> WorkManager persistent execution
  -> temp files
  -> validation/checksum
  -> atomic publish
  -> Room COMPLETED

No chapter is marked downloaded until every required page is validated and atomically published.

## Migration from current prototype

Keep:
- MangaDex adapter behavior
- OkHttp transport
- typed rate limiting
- Coil
- Paged/Webtoon prototype
- source ownership checks
- external chapter filtering
- existing tests where still valid

Replace/refactor:
- FileMangaProgressStore -> Room/LibraryRepository
- Properties metadata -> Room
- FileMangaOfflineStore metadata -> Room download state; filesystem only for bytes
- source API v1 -> Source API v2
- composable-owned durable reader state -> MangaReaderViewModel
- two-value mode enum -> multidimensional reader preferences

## Implementation order

1. Source API v2.
2. Room persistence correction.
3. repository integration with BookEntity as stable manga identity.
4. durable download queue/state.
5. MangaReaderViewModel and chapter transitions.
6. zoom/subsampling controlled pilot.
7. Source Recipe Layer.
8. additional native sources.
9. optional Suwayomi gateway.
10. visible Manga Hub UX integration.

## Mandatory gates

Every slice:
Architecture fit
-> license/provenance
-> unit/fixture tests
-> lint
-> debug build
-> release/R8
-> device smoke
-> regression matrix

No merge without explicit approval.
