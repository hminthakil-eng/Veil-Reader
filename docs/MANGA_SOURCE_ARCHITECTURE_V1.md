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
14. **Room persistence + backup foundation**
   - Room schema advances additively from v1 to v2; existing EPUB/PDF tables are unchanged;
   - normalized tables persist canonical Manga works, source links, progress, offline chapter manifests and offline page records;
   - unique sourceId + sourceKey index prevents one provider work from belonging to multiple canonical works;
   - source links cascade from canonical work deletion;
   - progress and offline manifests are foreign-keyed to canonical Manga identity;
   - offline pages cascade from manifest deletion;
   - MangaCanonicalStore, MangaProgressStore and MangaOfflineCacheIndex remain domain-facing boundaries over Room;
   - canonical save replaces only that work's source-link set transactionally;
   - offline manifest updates replace their page rows transactionally;
   - deterministic SHA-256 manifest keys encode full logical OfflineChapterId;
   - backup schema advances from 2 to 3 while schema-1 and schema-2 restore remain supported;
   - backup v3 stores canonical works/source links/progress but intentionally excludes offline page files and manifests;
   - restoring an older backup yields an empty Manga library, matching full-backup replacement semantics;
   - restoring Manga metadata clears offline manifests so no restored DB row points at missing cache files;
   - backup codec bounds work/source/progress counts and rejects orphan/duplicate progress rows;
   - backup manifest app version now comes from BuildConfig.VERSION_NAME instead of a stale literal;
   - migration/instrumentation tests cover v1 data preservation, source-link uniqueness, domain round-trip and backup semantics;
   - exported Room v2 schema JSON must be generated by the real KSP/Android build; it is not hand-authored.
15. **Android challenge UI + session handoff foundation**
   - AndroidX WebKit 1.17.0 stable is the explicit WebView compatibility layer;
   - INTERNET permission is declared for remote Manga/challenge traffic;
   - app-root Compose overlay hosts challenge UI without adding a Manga route to the EPUB/PDF shell;
   - foreground host registry stores only weak opaque tokens; no Activity reference crosses into challenge orchestration;
   - host registration follows RESUME/PAUSE lifecycle and a 10-second host-loss grace protects rotation/recreation;
   - active browser sessions are process-local and single-flight; process death cannot leave a stale session marked solved;
   - bounded WebView state (256 KiB maximum) may be kept process-locally across host recreation when WebView SAVE_STATE is supported;
   - challenge WebView enables JavaScript/DOM storage but disables file/content access, mixed content and multiple windows;
   - top-level navigation is HTTPS-only and restricted to the source host or its subdomains;
   - SSL errors are never bypassed and WebView renderer loss fails the challenge closed;
   - Back cancels the challenge rather than navigating the underlying Veil screen;
   - successful user confirmation captures Cookie/User-Agent only for the source origin;
   - challenge headers are process-local, source+domain scoped, TTL-bound, CR/LF filtered, size-bounded and redacted from toString();
   - SourceRequestContext carries those headers only for execution and redacts them from logs;
   - SourceExecutionCoordinator re-reads session headers per attempt, so a solved challenge reaches the immediate source retry;
   - AndroidChallengeUiDriver clears stale headers before a new challenge and releases its slot on solve/fail/cancel/owner cancellation;
   - runtime factory wires BrowserChallengeCoordinator + AndroidChallengeUiDriver + transient session headers into SourceExecutionCoordinator;
   - tests cover solved-header handoff, expiry, injection rejection, host-loss cancellation, rotation grace, completion race, owner cancellation and origin policy.

16. **Reader device verification harness**
   - debug-only verification Activity reconstructs a complete local Manga session from Intent extras;
   - no live Manga website or production library data is required;
   - verification persistence uses a dedicated Room database separate from the user's library;
   - optional in-memory progress mode isolates SavedState for true process-death testing;
   - generated image fixtures are atomic and dimension-validated before reuse;
   - normal fixture pages are fully local/offline;
   - partial-offline fixture exposes a bounded incomplete chapter;
   - extreme fixture generates a 360x12000 local PNG with repeated visual markers;
   - stable Compose semantics tags expose reader root, page, chrome, mode, direction, partial-offline and subsampling surfaces;
   - emulator tests cover Activity recreation, RTL/LTR swipe and edge-tap direction, Paged↔Webtoon round trip, durable offline reopen, partial-offline boundary blocking and extreme-page gesture isolation;
   - extreme renderer test requires successful subsampling render and Activity recreation without OOM/crash;
   - manual protocol isolates true OS process death from Room persistence and records extreme-image visual/memory evidence;
   - existing connectedDebugAndroidTest emulator workflow is reused; no duplicate device workflow is added.

17. **Manga Hub catalog vertical slice**
   - production Manga Hub UI is source-neutral and contains Discover, Search and Library surfaces;
   - Discover consumes product-level SourceMangaRef seeds instead of inventing a fake browse method in every source adapter;
   - Search uses SourceRegistry + SourceExecutionCoordinator across installed providers;
   - search results are deduplicated only by exact source identity; equal titles from different sources are never auto-merged;
   - source details and chapter order come from the provider without title/number re-sorting;
   - canonical add is idempotent by stable sourceId + sourceKey identity;
   - Room adds a query-only canonical lookup by source identity; schema v2 is unchanged;
   - Library details and Reader session creation fall back across linked sources when a preferred source fails;
   - chapter rows open MangaReaderSession at the selected provider chapter key;
   - Read & save creates canonical identity first, then opens the integrated Reader and keeps Details state synchronized;
   - Reader close returns to the same Hub Details surface instead of the EPUB/PDF shell;
   - source issues are surfaced as a small skipped-source count while Library remains usable;
   - production Hub route is still not enabled in VeilApp;
   - debug-only fixture provider exercises SEARCH/DETAILS/CHAPTERS/PAGES contracts with two deterministic works;
   - debug Hub host uses a dedicated Room database and local generated page files, never the user's production library or a live website;
   - shared debug fixture image generation is reused by Reader and Hub verification;
   - JVM service gates cover idempotent add, no title merge, linked-source fallback, provider chapter order and discovery library flags;
   - Android E2E gates cover Discover -> Details -> Add -> Library persistence, Search -> Details, chapter tap -> selected Reader chapter and Read & save -> Reader -> Details.

18. **First live source controlled pilot — MangaDex**
   - uses Veil's current Source SDK; the older experimental MangaDex branch is reference-only and is not revived;
   - stable source identity is `mangadex`; language and API host are execution/configuration concerns, not persisted source identity;
   - official JSON API path only; no HTML scraping or parser dependency is introduced;
   - read-only capabilities are SEARCH, DETAILS, CHAPTERS and PAGES;
   - requests are paced conservatively at 4 requests/second with monotonic time;
   - network cancellation propagates through OkHttp call cancellation;
   - HTTP failures map into the existing source-failure taxonomy, including bounded Retry-After handling for 429;
   - search/details map stable MangaDex UUID identity, localized metadata and cover relationships;
   - chapter feeds keep provider order, preserve scanlation-group credit metadata, request `includeExternalUrl=0`, and defensively drop any external chapter that still appears;
   - chapter pagination advances by the raw API page size, not the filtered chapter count, preventing loops after external-entry filtering;
   - at-home page URLs must be HTTPS and preserve contiguous source page order;
   - public reverse-link resolution accepts only HTTPS MangaDex title URLs;
   - content-rating requests are limited to safe + suggestive during this pilot;
   - a 2,000-chapter safety bound prevents unbounded feed retrieval;
   - fixture compatibility tests cover search, metadata, chapter filtering/order, page URLs, rate limits, reverse links and policy guards without live network dependency;
   - current reviewed policy guard records required MangaDex/source-group credit and blocks production enablement until a fresh acceptable-use review;
   - the live provider can be selected only by the debug Manga Hub verification host using `source_mode=mangadex`;
   - the normal debug fixture remains the default so deterministic Android E2E tests are unchanged;
   - `VeilApp` still does not register or route to MangaDex, and EPUB/PDF behavior is untouched.

## Next vertical slices

1. **MangaDex live device smoke**
   - use the debug Hub host only;
   - validate Search -> Details -> chapter feed -> at-home pages on an Android device;
   - record API failure/rate-limit behavior without weakening fixture-based gates;
   - do not make live-network tests mandatory CI tests.

2. **MangaDex resilience gate**
   - exercise source-health observation, rate-limit cooldown, source removal/parser-change handling, offline reopen and replacement boundaries;
   - verify no challenge loop is introduced for an API source that does not declare browser challenge capability;
   - verify scanlation/source attribution is adequate before any product exposure.

3. **Production-enablement decision**
   - re-check current MangaDex acceptable-use terms immediately before any user-facing registration;
   - keep disabled if the intended distribution/monetization model conflicts with those terms;
   - do not add a second live source until this first source passes policy, resilience and device gates.

## Verification state

- Pure-Kotlin compilation of the source and canonical-library foundations succeeds independently.
- Local smoke execution passed canonical identity preservation, progress page remapping, cache-path safety and ambiguous chapter rejection.
- Source replacement production files compile independently and smoke execution passed with `SOURCE_REPLACEMENT_SMOKE_OK`: strong candidate selection, equal-candidate ambiguity guard and READY-only finalization.
- Source health/ranking production files compile independently and smoke execution passed with `SOURCE_HEALTH_SMOKE_OK`. The smoke run caught and fixed an initial ranking-evidence bug where zero-penalty NETWORK observations still changed confidence.
- Browser challenge orchestration compiles independently and smoke execution passed with `BROWSER_CHALLENGE_SMOKE_OK`. A race between session completion and cooldown evaluation was found and fixed before PR creation.
- Manga reader core compiles independently and smoke execution passed with `MANGA_READER_CORE_SMOKE_OK`: RTL/LTR navigation, zoom bounds, page-count clamp, webtoon restore, tall-image geometry and source-replacement-safe chapter restore.
- Reader UI reducer/SavedState logic passed local execution smoke with `MANGA_READER_UI_SMOKE_OK`; the full Android Reader stack has since passed local Gradle/device verification.
- Reader presentation orchestration passed local execution smoke with `MANGA_READER_PRESENTATION_SMOKE_OK`: complete offline reopen, HYBRID local/remote delivery, partial-offline fallback and safe chapter-boundary blocking.
- Unnumbered chapter cache identity passed `MANGA_OFFLINE_LOCATOR_SMOKE_OK`: normalized-title identity survives provider-key replacement and distinguishes different specials.
- Reader image delivery has unit gates for cache-root confinement, byte-size/hash verification, remote URL/header validation and secret redaction, and the Coil/Compose stack now compiles in the verified Android build.
- Extreme-image strategy has deterministic JVM tests for standard/local-subsampling/remote-preview decisions and dimension-probe planning. Independent pure-Kotlin smoke passed with `EXTREME_IMAGE_STRATEGY_SMOKE_OK`.
- Reader screen integration has deterministic unit gates for session identity/source ownership, progress remapping and exclusive gesture ownership and now passes the local Android device matrix.
- Room v2 persistence has an additive v1→v2 migration, DAO/repository instrumentation gates, backup schema-3 coverage and a real KSP-generated tracked `app/schemas/.../2.json`.
- Android challenge UI has process-local host/session/header boundaries plus unit/instrumentation gates for retry handoff, rotation grace, host loss, cancellation and HTTPS origin confinement; the Android build/device gate now runs locally.
- Reader device verification harness is committed and executed locally: deterministic debug host, isolated Room persistence, Compose instrumentation matrix, extreme local fixture and true external OS process-death resume verification.
- Manga Hub catalog vertical slice is committed with production source-neutral UI/service boundaries plus a debug-only local provider and Android E2E matrix. The local device gate is green, but the Hub remains unexposed in VeilApp by deliberate rollout policy.
- The local Android gate is now green on PC: unit/lint/assemble pass, connected instrumentation is 33/33, and external `am kill` process-death resume returned to the saved Manga page.
- MangaDex controlled pilot now has a fixture-verified read-only adapter and debug-only Hub selection. Live Android network smoke and fresh pre-production policy review remain required.
- GitHub Actions is currently failing before any workflow step starts: the observed jobs have no assigned runner and no step output.
- Hosted GitHub runners still fail before their first workflow step; local Android/Gradle verification is green, but stacked Manga PRs remain unmerged pending explicit approval and hosted-infrastructure resolution.

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
