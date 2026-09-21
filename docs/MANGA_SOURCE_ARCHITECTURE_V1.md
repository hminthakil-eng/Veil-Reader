# Veil Manga Source Architecture v1

Status: architecture-first foundation, behind the existing product surface.

## Goal

Add manga/webtoon sources without coupling remote-site behavior to the EPUB/PDF reader, Room schema, or Compose UI. A source changing domain, requiring a challenge, or disappearing must not corrupt reading progress or library identity.

## Clean-room rule

Kotatsu-Redo is a high-value behavior and architecture reference only. Veil owns its interfaces, models, persistence and implementations. Do not copy or link GPL implementation code unless Eyad Studio explicitly accepts the resulting license obligations.

## Boundaries

### Source layer

`MangaSourceProvider` owns remote/local source behavior:

- search;
- details;
- chapters;
- pages;
- optional reverse-link resolution;
- declared capabilities;
- ordered primary/mirror domains.

Providers return source-neutral models and classified failures. They do not navigate UI, write Room directly, or own reader state.

### Stable identity

`SourceId` is independent from host/domain. `SourceMangaRef.key` is provider-stable and must not contain a mutable domain.

This permits:

- mirror/domain failover without identity rewrite;
- migration from a dead source to an alternative source;
- cached/offline state to survive host changes.

### Recovery and execution

Failures are classified into network, timeout, rate limit, blocked/challenge, auth, not found, parser changed, source removed, unsupported and unknown.

`SourceRecoveryPolicy` returns one recovery action. `SourceExecutionCoordinator` owns the bounded execution mechanics:

- maximum attempt budget;
- per-source semaphore/concurrency limit;
- runtime domain/mirror selection;
- rate-limit delay clamping;
- cancellation propagation;
- one challenge attempt per domain per operation;
- an explicit `SourceChallengeAdapter` boundary.

The coordinator intentionally does **not** choose a different source. Cross-source replacement requires canonical work matching and migration confidence and therefore remains a separate layer.

### Registry

`SourceRegistry` rejects duplicate IDs, provides deterministic ordering and filters providers by capability/language/content type.

## Deliberately not in this slice

- no live source website adapter;
- no network/HTML parser dependency;
- no concrete WebView/Cloudflare implementation;
- no Room schema changes;
- no library migration;
- no Manga UI;
- no reader-mode changes;
- no telemetry/community service.

That keeps the existing EPUB/PDF release candidate behavior unchanged.

## Completed foundation

1. **Source SDK contract**
   - stable source identity;
   - source-neutral manga/chapter/page models;
   - explicit capabilities;
   - classified failures.

2. **Execution coordinator**
   - bounded retry;
   - per-source concurrency;
   - cancellation propagation;
   - mirror/domain failover;
   - rate-limit clamp;
   - challenge adapter boundary;
   - anti-loop challenge guard.

3. **Fixture-backed contract proof**
   - search -> details -> chapters -> pages;
   - reverse URL resolution;
   - deterministic local provider;
   - no dependency on a live manga website.

4. **Canonical library/offline foundation**
   - source-independent `CanonicalMangaId`;
   - multiple source refs linked to one logical work;
   - source-neutral chapter anchors and reading progress;
   - page remapping by chapter progression when replacement page counts differ;
   - source-independent offline chapter/page manifests;
   - traversal-safe relative cache layout;
   - conservative chapter matcher that rejects ambiguous migrations.

5. **Alternatives / source replacement planner**
   - title-dominant work matching with metadata as secondary evidence;
   - conservative chapter mapping;
   - explicit READY / MANUAL_REVIEW / REJECTED states;
   - two-phase replacement: link new source first, unlink old source only after safe finalize;
   - no offline-cache ownership rewrite because cache belongs to `CanonicalMangaId`;
   - ambiguity-aware multi-candidate resolver with minimum lead before auto-recommendation.

## Next vertical slices

1. **Room persistence adapters for Manga Hub**
   - persist canonical works and source links;
   - persist progress and offline manifests;
   - versioned schema migration + backup compatibility;
   - only land after full Android/Gradle verification is available.

2. **Source health + ranking**
   - success/failure/latency observations;
   - circuit breaker and cooldown;
   - per-source reliability score;
   - exploration guard so new sources are not permanently starved;
   - privacy-safe local-first metrics.

3. **Reader vertical slice**
   - paged manga;
   - RTL page direction;
   - webtoon continuous mode;
   - zoom;
   - orientation/process restoration;
   - offline reopen.

## Verification state

- Pure-Kotlin compilation of the source and canonical-library foundations succeeds independently.
- Local smoke execution passed canonical identity preservation, progress page remapping, cache-path safety and ambiguous chapter rejection.
- Source replacement production files compile independently and smoke execution passed with `SOURCE_REPLACEMENT_SMOKE_OK`: strong candidate selection, equal-candidate ambiguity guard and READY-only finalization.
- GitHub Actions is currently failing before any workflow step starts: the observed jobs have no assigned runner and no step output.
- Full Android/Gradle verification remains required before merge.

## Quality gates

Before enabling Manga Hub in production:

- source failures never break EPUB/PDF reading;
- coroutine cancellation is never counted as source failure;
- no infinite challenge/retry loop;
- mirror switch preserves identity;
- source replacement preserves reading progress;
- offline chapter reopens without network;
- large/tall image zoom does not collapse or jump;
- RTL/webtoon modes restore after process death;
- parser/site changes fail visibly and recoverably;
- every source adapter has fixture-based compatibility tests.
