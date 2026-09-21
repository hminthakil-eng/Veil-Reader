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

### Recovery

Failures are classified into network, timeout, rate limit, blocked/challenge, auth, not found, parser changed, source removed, unsupported and unknown.

`SourceRecoveryPolicy` returns one recovery action. Retry budgets and cooldowns belong to a future execution coordinator so the policy cannot create infinite retry loops.

### Registry

`SourceRegistry` rejects duplicate IDs, provides deterministic ordering and filters providers by capability/language/content type.

## Deliberately not in this slice

- no source website adapter;
- no network dependency;
- no WebView/Cloudflare implementation;
- no Room schema changes;
- no library migration;
- no Manga UI;
- no reader-mode changes;
- no telemetry/community service.

That keeps the existing EPUB/PDF release candidate behavior unchanged.

## Next vertical slices

1. **Execution coordinator**
   - bounded retry;
   - per-source concurrency;
   - timeout/cancellation propagation;
   - mirror/domain failover;
   - challenge adapter boundary.

2. **One reference provider**
   - search -> details -> chapters -> pages;
   - contract tests using deterministic fixtures;
   - no persistence yet.

3. **Offline cache + progress model**
   - canonical work identity separated from `SourceMangaRef`;
   - chapter/page cache metadata;
   - resume state;
   - migration-safe keys.

4. **Alternatives / source replacement**
   - title/alt-title matching;
   - chapter number/volume alignment;
   - cover similarity as secondary evidence;
   - explicit confidence and manual confirmation;
   - transactionally migrate progress/favourites/download metadata.

5. **Reader vertical slice**
   - paged manga;
   - RTL page direction;
   - webtoon continuous mode;
   - zoom;
   - orientation/process restoration;
   - offline reopen.

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
