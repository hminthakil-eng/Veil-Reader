# Veil Manga Hub — Reference Composition Plan

Status: approved architecture input for Veil Reader 2.0
Date: 2026-09-20

Veil Manga Hub is not a fork of any single manga/anime application.
It is a Veil-native clean-room implementation that combines proven patterns from multiple references.

## Reference stack

### Mihon
Role: primary product/UX/reader architecture reference.

Use as reference for:
- library/category UX
- local source handling
- reader settings and image navigation patterns
- backup/restore concepts
- extension/source separation concepts
- history/favorites/update flows

Policy:
- architecture and product patterns may inspire Veil.
- do not create a hard runtime dependency unless separately approved.
- keep Veil domain models independent.

### Kotatsu-Redo
Role: primary manga source/offline/resilience reference.

Use as reference for:
- source-provider abstraction
- source alternatives/replacement
- chapter/page resolution
- offline chapter storage
- update feed
- progress/resume
- compatibility/fallback behavior

Policy:
- GPL code does not enter Veil by default.
- no direct fork/embed.
- clean-room interfaces and adapters only unless GPL implications are explicitly accepted.

### kotatsu-parsers-redo
Role: parser/source architecture reference.

Use as reference for:
- source adapter boundaries
- parser capability modeling
- source error/fallback semantics
- source replacement/migration ideas

Policy:
- same GPL clean-room rule as Kotatsu-Redo.
- no copied parser implementation in Veil's permissive core by default.

### Yūzōnō anime-extensions
Role: Anime/Media Hub source/extractor reference.

Use as reference for:
- multi-audio and subtitle capability modeling
- extractor/hoster separation
- provider adapters
- resilience when a host or extractor changes
- media source preference/fallback

Examples previously studied:
- Mapple
- AniZone
- AnimePahe
- HentaiMama

Policy:
- use adapter architecture and capability patterns.
- keep Veil interfaces independent from lib16/Core-specific APIs.
- review each source/host before adoption.

### Miyomi
Role: discovery/index/reference only.

Use as reference for:
- discovery aggregation patterns
- extension/catalog indexing
- source metadata organization

Policy:
- AGPL code is not embedded in Veil by default.
- architecture/behavioral research only unless licensing is explicitly accepted.

### AniList
Role: canonical metadata/enrichment provider candidate.

Use for:
- title metadata
- aliases
- characters/staff
- relations
- recommendations
- cover/art metadata where permitted

Policy:
- metadata is separate from chapter/content source.
- cache locally with provider IDs and provenance.

### Jikan
Role: metadata fallback.

Use for:
- MyAnimeList-derived metadata fallback where appropriate.

Policy:
- never make fallback metadata availability a requirement for reading local/offline content.

### MangaDex
Role: content/source candidate for controlled provider evaluation.

Policy:
- implement behind MangaSourceProvider only.
- review API terms, rate limits and allowed usage before production enablement.

## Combined Veil architecture

The winning architecture is a composition:

```
Mihon UX/reader patterns
        +
Kotatsu source/offline/resilience patterns
        +
Kotatsu parser capability ideas
        +
Yuzono anime/media adapter patterns
        +
Miyomi discovery/index ideas
        +
AniList/Jikan metadata enrichment
        ↓
        Veil-owned clean-room domain
```

None of the reference applications becomes Veil's domain model.

## Core Veil interfaces

```kotlin
interface MangaSourceProvider {
    val id: SourceId
    val capabilities: MangaSourceCapabilities

    suspend fun search(query: String, page: PageKey?): Page<MangaSummary>
    suspend fun details(id: RemoteSeriesId): MangaSeries
    suspend fun chapters(seriesId: RemoteSeriesId): List<MangaChapter>
    suspend fun pages(chapterId: RemoteChapterId): List<MangaPage>
}

interface MediaSourceProvider {
    val id: SourceId
    val capabilities: MediaSourceCapabilities

    suspend fun search(query: String, page: PageKey?): Page<MediaSummary>
    suspend fun episodes(seriesId: RemoteSeriesId): List<MediaEpisode>
    suspend fun streams(episodeId: RemoteEpisodeId): List<MediaStream>
}
```

The Manga and Anime providers may share:
- source identity
- health state
- rate limiting
- cache
- retry/backoff
- provenance
- feature flags
- compatibility tests

But they should not be forced into one overloaded interface.

## Domain separation

Veil Reader books and online manga are different domain aggregates.

Keep:
- `Publication/Book` for imported EPUB/PDF/CBZ and local publications.
- `MangaSeries / MangaChapter / MangaPage` for provider-backed manga.
- `MediaSeries / MediaEpisode / MediaStream` for anime/media.

A downloaded manga chapter may materialize to local files, but it remains linked to its manga identity/source binding.

## Source replacement and migration

First-class entities:
- SourceBinding
- SourceAlternative
- ChapterIdentity
- ProgressIdentity

When a source dies:
1. find alternative sources by normalized metadata/provenance.
2. present candidate matches.
3. migrate library binding.
4. map chapter progress.
5. preserve local downloads.
6. keep old source ID/provenance for audit/recovery.

This is a core Veil differentiator and should not be an afterthought.

## Offline model

```
Series metadata
 -> chapter manifest
 -> download request
 -> WorkManager
 -> bounded parallel page download
 -> checksum/validation
 -> atomic chapter install
 -> local manifest
 -> reader uses local first
```

Do not store half-installed chapters as complete.
Interrupted downloads must resume or restart safely.

## Reader modes

Manga reader:
- paged horizontal
- paged RTL
- vertical page mode
- Webtoon continuous vertical
- fit width / fit height
- double-tap/pinch zoom
- prefetch next/previous pages
- per-series/per-source reader preferences

Webtoon:
- optimized LazyColumn/image pipeline
- adjacent-page prefetch
- memory-aware bitmap sizing
- no full-resolution decode when display size does not require it

## Metadata composition

A series can combine data from multiple providers:

```
Veil MangaSeries
  sourceBinding -> content source
  metadataBindings:
    AniList
    Jikan
    MangaDex
    local override
```

Local user edits always win for display fields when explicitly overridden.

## Compatibility and quality gates

Every provider adapter must pass:
- discovery/search
- details
- chapter listing
- page resolution
- source timeout/error
- rate limiting
- retry
- offline restore
- progress resume
- source replacement
- parser/extractor change regression

Provider failure must never corrupt the user's library or progress.

## Implementation order

### Slice 1
One reviewed source:
Source -> Search -> Details -> Library -> Reader -> Progress -> Offline -> Restart/Resume.

### Slice 2
Source alternatives and migration.

### Slice 3
Multiple source adapters + compatibility matrix.

### Slice 4
AniList/Jikan metadata enrichment.

### Slice 5
Anime/Media Hub using the separate MediaSourceProvider contract.

### Slice 6
Advanced discovery/indexing inspired by Miyomi.

## Licensing boundary

Veil core should remain license-compatible with our intended distribution.

Default rule:
- Apache/MIT/permissive ideas/code may be reused only with proper notice and review.
- GPL/AGPL projects are architecture/reference sources unless we explicitly choose the licensing implications.
- do not copy source implementations across the clean-room boundary.
- preserve provenance for every adapted component.

## Final decision

Do not choose Mihon *or* Kotatsu *or* the anime extension ecosystem.
Use each where it is strongest, behind Veil-owned interfaces.

Veil owns:
- domain models
- provider contracts
- reader UX
- offline/download model
- migration
- QA
- branding
- data ownership

Reference apps contribute proven solutions, not product ownership.
