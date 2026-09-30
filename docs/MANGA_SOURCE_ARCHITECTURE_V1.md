# Veil Manga Source Architecture v1

Status: integrated local/offline product foundation; live network sources remain opt-in and disabled.

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

## Current safety exclusions

- no enabled live source website adapter;
- no default network/HTML scraping path;
- no concrete WebView/Cloudflare challenge UI in the product path;
- no CBR ingestion until a separately reviewed safe archive strategy exists;
- no source activation without explicit opt-in;
- no telemetry/community service carrying reading payloads.

Manga now has its own Room-backed persistence, local CBZ ingestion, Hub and Reader path. EPUB/PDF ownership is still isolated: COMIC navigation is intercepted before Readium and Manga failures cannot silently fall through into the text reader.

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

6. **Source health + ranking foundation**
   - payload-free local health events: source, operation, success/failure class and latency only;
   - no query/title/chapter key/URL/user identity in health records;
   - local NETWORK failures have zero penalty and do not increase ranking confidence;
   - weighted source-attributable failures;
   - circuit breaker with OPEN -> HALF_OPEN -> CLOSED recovery;
   - latency EWMA;
   - reliability ranker with bounded exploration bonus for under-sampled sources;
   - OPEN circuits excluded from automatic best-source selection;
   - optional execution observer so Source SDK behavior is unchanged when health tracking is disabled.

7. **Browser challenge orchestration foundation**
   - one process-wide interactive challenge session at a time;
   - background callers can join an existing session but cannot launch UI;
   - callers for another source/domain wait for the global slot, then re-evaluate;
   - configurable success-recurrence and failure cooldown windows;
   - repeated challenge shortly after SOLVED is classified as ineffective and does not reopen UI;
   - cancelled/failed UI sessions complete deterministically and release waiters;
   - session lifetime belongs to an injected application scope;
   - no Activity/WebView references cross the challenge boundary.

8. **Manga reader core foundation**
   - pure-Kotlin reader state independent from Compose/Android;
   - paged and webtoon modes;
   - explicit LTR/RTL tap and swipe mapping;
   - chapter-boundary signaling without implicit navigation;
   - zoom state with finite/clamped scale and normalized focal point;
   - paged navigation resets page zoom while webtoon scroll updates preserve active zoom;
   - source-neutral process-death snapshot/restore;
   - restore clamps positions when page counts change;
   - source replacement can change provider chapter key without resetting the same logical chapter;
   - orientation policy changes preserve logical reading position;
   - decode-ready image geometry guard prevents zero-size/NaN/Infinity layout;
   - very tall webtoon pages use finite fit-width geometry.

9. **Reader UI adapter foundation**
   - pure UI intents/effects reducer on top of `MangaReaderController`;
   - tap-zone classification for left/center/right reader actions;
   - zoomed pages block accidental page-turn swipe/tap navigation;
   - double-tap zoom and transform-zoom intents;
   - webtoon viewport -> item/offset mapping;
   - fail-closed platform-neutral SavedState codec;
   - Android `SavedStateHandle` bridge stores only primitive String values;
   - renderer-agnostic Compose paged and LazyColumn webtoon surfaces;
   - image loading/transport intentionally stays outside the UI adapter;
   - no new image/network dependency.

10. **Reader presentation integration foundation**
   - offline-first chapter loader over the existing cache index and bounded source coordinator;
   - complete offline chapters reopen with no provider/network dependency;
   - partial cache + online pages merge into HYBRID delivery;
   - network/source failure falls back to a contiguous PARTIAL_OFFLINE prefix when available;
   - incomplete cache never renumbers later pages into earlier UI indices;
   - remote page lists must be unique and contiguous from index 0;
   - loading/error/ready presentation states with retryability;
   - chapter boundary navigation uses explicit provider reading order and logical chapter identity;
   - PARTIAL_OFFLINE cannot auto-cross into another chapter;
   - stale UI state never renders assets from a newly loaded chapter;
   - unnumbered special chapters use source-neutral title-based offline cache identity when possible;
   - provider chapter key is only the last-resort offline discriminator.

11. **Reader image delivery foundation**
   - Coil 3.6.3 dedicated reader image pipeline;
   - request-scoped network headers only; no global header interceptor;
   - remote URL/header values are redacted from model/resolution `toString()`;
   - HTTP(S)-only remote assets with embedded-credential and CR/LF header-injection rejection;
   - local file canonical-path confinement to the Manga cache root;
   - local non-empty/byte-size verification and optional SHA-256 integrity verification;
   - verified-file memoization keyed by path/length/mtime/expected hash;
   - dedicated memory cache with background trim;
   - Coil disk cache disabled so Veil's explicit offline cache remains the only persistent Manga cache;
   - partial image decode disabled;
   - bounded parallel bitmap decoding;
   - paged/webtoon max-bitmap policies to reduce large-decode OOM risk;
   - AsyncImage renderer with loading/error/retry UI and intrinsic-height webtoon layout;
   - no SubcomposeAsyncImage in LazyColumn.

12. **Extreme image subsampling strategy**
   - dimension-based strategy; never infer extreme status from filename/URL;
   - local bounds probe uses decode-bounds mode before expensive bitmap decode;
   - ordinary images stay on Coil;
   - verified local extreme images are eligible for ZoomImage subsampling;
   - remote extreme/unknown images stay on bounded Coil preview until localized;
   - no hidden second download/cache path is introduced for remote tiling;
   - thresholds include pixel count, long edge and tall aspect ratio;
   - ZoomImage 1.5.0 selected for Kotlin 2.3.x / Compose 1.10.x compatibility;
   - ZoomImage renderer remains isolated until reader screen transfers gesture ownership explicitly;
   - ZoomImage logger is forced to Error level because its debug pipeline can include request data.

13. **Reader screen integration foundation**
   - route-injected chapter session/catalog; no premature coupling to the EPUB/PDF app shell;
   - one canonical manga per session and duplicate logical chapters rejected;
   - ViewModel integrates presentation loader, reducer, SavedState and durable progress;
   - process-death SavedState has priority over durable progress restore;
   - durable progress remaps by chapter progression when page count changes;
   - incomplete/unknown page-count state never overwrites richer durable progress;
   - durable progress writes are serialized to prevent late stale writes;
   - stale chapter loads are cancelled and generation-checked before committing UI state;
   - unexpected loader exceptions become retryable presentation errors rather than screen crashes;
   - chapter-boundary effects resolve against explicit reading order;
   - complete/offline/hybrid/partial-offline rules remain enforced during transitions;
   - orientation policy is applied only while the Manga screen is active and previous host orientation is restored on exit;
   - explicit gesture ownership is exclusive: Veil gestures or page renderer, never both;
   - local extreme pages transfer gesture ownership to ZoomImage;
   - ZoomImage tap is bridged back only to reader chrome controls, not to page-turn navigation;
   - adaptive page rendering selects standard Coil, bounded remote preview or local subsampling from measured strategy;
   - existing EPUB/PDF ReaderScreen and VeilApp routing remain untouched.

14. **Safe local CBZ ingestion foundation**
   - pure local archive ingestion with no live-provider dependency;
   - natural numeric page ordering rather than lexicographic filename order;
   - output paths are generated by Veil and never copied from archive entry paths;
   - unsafe traversal-style image entries fail the import;
   - per-page, total-expanded-data, entry-count and archive-size limits bound zip-bomb risk;
   - each extracted page is SHA-256 hashed while streaming;
   - the complete chapter is extracted into a staging directory and swapped into place only after every page succeeds;
   - a failed import leaves the previous committed chapter directory untouched;
   - the resulting manifest remains compatible with the existing offline-first reader loader and canonical session adapter.

15. **Room-backed Manga product persistence**
   - `books` remains the single user-library catalog authority;
   - Room v3 adds normalized Manga chapter/source/progress/offline-manifest tables;
   - canonical session rehydration reads persisted ordered chapters and source links;
   - detailed Manga progress and the cross-product Book progress summary commit transactionally;
   - derived cover/fingerprint writes are narrow-column updates and cannot regress newer progress;
   - Book deletion cascades Manga-owned database state while historical general reading-session policy remains unchanged.

16. **Local CBZ product path**
   - document picker routes CBZ to the Manga importer instead of Readium;
   - app-private source copy uses size preflight, bounded streaming and partial-file staging;
   - content fingerprint deduplicates repeated imports before creating a second catalog identity;
   - one Manga Book can own multiple local CBZ chapters without creating duplicate Library titles;
   - multi-document selection is naturally filename-sorted so chapter 2 precedes chapter 10 independent of Android picker return order;
   - filename hints infer volume/chapter numbers while explicit metadata can override inferred values;
   - the primary chapter remains pinned at reading order 0 as the Book publication authority; added chapters can be safely reordered around one another;
   - chapter title/volume/number/language editing is supported; identity-bearing edits atomically migrate the derived cache to the new source-neutral OfflineChapterId and reject collisions;
   - added chapter deletion removes its source archive and generated cache, closes reading-order gaps and deterministically remaps current progress when needed;
   - safe CBZ ingestion generates the explicit offline cache + first-page cover;
   - failures compensate database rows, staged source files and generated cache;
   - COMIC open verifies/self-heals missing generated cache from local source archives before session rehydration;
   - Manga Hub exposes Start / Continue / Read again, chapter management and source-vs-cache storage ownership.

17. **Backup / restore compatibility**
   - backup schema v5 stores every local CBZ chapter, ordered chapter metadata and compact exact Manga reader progress including active chapter order;
   - derived image cache and cover are intentionally not backed up;
   - restore regenerates fingerprint, cover, ordered catalog, offline manifests and page cache from CBZ sources;
   - restored local chapter source identity is verified against the manifest fingerprint before restore commit;
   - schemas 1/2/3/4 remain accepted;
   - failed restore attempts rebuild the previous Manga state before returning failure;
   - instrumentation coverage now includes CBZ import, natural-order batch ingestion, duplicate prevention, unsafe-archive rollback, chapter metadata-migration/reorder/delete invariants, cache cleanup/self-heal, Room persistence/migration and multi-chapter Manga backup round-trip; execution remains pending the Android verification gate.

18. **Derived-cache storage policy**
   - original local CBZ archives are user-owned durable sources inside app-private publications storage;
   - extracted page files are disposable derived cache and are measured separately;
   - per-title cache cleanup never deletes source CBZ files or reader progress;
   - cache self-heal validates persisted page size/existence before deciding a chapter is usable;
   - missing/incomplete cache is re-ingested from the exact persisted local chapter source;
   - cache cleanup and catalog mutations are serialized against reader open.

## Next vertical slices

1. **Reversible Manga work grouping**
   - optional work grouping/merge UI for independently imported CBZ titles;
   - explicit split/unmerge path so grouping remains reversible;
   - ambiguity-safe candidate suggestions only; never auto-merge by filename alone.

2. **Android challenge UI driver**
   - lifecycle-safe WebView host behind `ChallengeUiDriver`;
   - cookie/user-agent handoff scoped to the source/domain;
   - foreground host registry without Activity retention;
   - process-death and host-loss recovery;
   - instrumentation coverage for rotation/background/close.

3. **Reader instrumentation + device verification**
   - rotation and process-death restore;
   - RTL/LTR swipe/tap behavior;
   - paged ↔ webtoon switching;
   - offline reopen and partial-offline boundary UX;
   - extreme local image OOM/zoom-quality gate;
   - gesture conflict checks for ZoomImage vs LazyColumn/paged navigation;
   - run only once Android/Gradle execution is available.

## Verification state

- Pure-Kotlin compilation of the source and canonical-library foundations succeeds independently.
- Local smoke execution passed canonical identity preservation, progress page remapping, cache-path safety and ambiguous chapter rejection.
- Source replacement production files compile independently and smoke execution passed with `SOURCE_REPLACEMENT_SMOKE_OK`: strong candidate selection, equal-candidate ambiguity guard and READY-only finalization.
- Source health/ranking production files compile independently and smoke execution passed with `SOURCE_HEALTH_SMOKE_OK`. The smoke run caught and fixed an initial ranking-evidence bug where zero-penalty NETWORK observations still changed confidence.
- Browser challenge orchestration compiles independently and smoke execution passed with `BROWSER_CHALLENGE_SMOKE_OK`. A race between session completion and cooldown evaluation was found and fixed before PR creation.
- Manga reader core compiles independently and smoke execution passed with `MANGA_READER_CORE_SMOKE_OK`: RTL/LTR navigation, zoom bounds, page-count clamp, webtoon restore, tall-image geometry and source-replacement-safe chapter restore.
- Reader UI reducer/SavedState logic passed local execution smoke with `MANGA_READER_UI_SMOKE_OK`: zoom-safe swipe gating, double-tap reset, boundary effects and fail-closed state restore. Compose sources remain pending full Android/Gradle verification.
- Reader presentation orchestration passed local execution smoke with `MANGA_READER_PRESENTATION_SMOKE_OK`: complete offline reopen, HYBRID local/remote delivery, partial-offline fallback and safe chapter-boundary blocking.
- Unnumbered chapter cache identity passed `MANGA_OFFLINE_LOCATOR_SMOKE_OK`: normalized-title identity survives provider-key replacement and distinguishes different specials.
- Reader image delivery now has unit gates for cache-root confinement, byte-size/hash verification, remote URL/header validation and secret redaction. Full Coil/Compose Android compilation remains pending the Android build gate.
- Extreme-image strategy has deterministic JVM tests for standard/local-subsampling/remote-preview decisions and dimension-probe planning. Independent pure-Kotlin smoke passed with `EXTREME_IMAGE_STRATEGY_SMOKE_OK`.
- Reader screen integration now has deterministic unit gates for session identity/source ownership, progress remapping, exclusive gesture ownership and durable-close success/failure retry behavior.
- New Android instrumentation coverage is present for Room v3 migration/cascades, Room Manga progress/offline round-trip, local CBZ import/dedup/rollback, natural-order batch ingestion, chapter rename/reorder/delete, cache cleanup/self-heal and schema-v5 multi-chapter Manga backup/restore round-trip. These Android instrumentation tests are source-complete but are not claimed as executed in this source-only pass.
- GitHub Actions was previously observed failing before workflow steps started because no runner was assigned; re-check infrastructure only when the Android verification gate is intentionally opened.
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
