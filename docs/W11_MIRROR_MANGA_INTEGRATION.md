# W11 — Mirror + Manga product integration

Branch: `alpha/w11-mirror-manga-integration`

## Scope decision

Mirror (Arrodes) and Manga are first-class Veil Reader product areas from W11 onward. They are no longer treated as detached experiments.

The text Reader remains the reliability baseline. Mirror and Manga must integrate through explicit adapters rather than weakening EPUB/PDF ownership boundaries.

## Mirror / Arrodes

### Integrated

- Native Compose `ArrodesMirrorScreen`.
- Threshold/Home entry point.
- Restorable app route: `mirror`.
- Uses only durable user-created highlights and notes.
- Every manifested fragment carries its exact saved locator.
- Activating a manifested fragment returns through the existing book-open locator path.
- Deterministic fragment prioritization favors authored notes, then archive resonance.
- Each Mirror visit keeps the strongest trace first, then uses a deterministic non-repeating session deck for the remaining echoes.
- Manifested passages use locale-correct quotation punctuation and adaptive typography for long notes.
- Echo transitions now disperse into restrained ash and coalesce into text-shaped particle bands; Reduced Motion snaps directly to the stable state.
- Persian/RTL content is preserved as stored text.
- Reduced-motion and high-contrast policies are respected.
- Empty state is truthful: Arrodes is silent when no durable trace exists.
- Unit contract covers note priority and exact-locator preservation.

### Next refinement gates

- Refine the current text-shaped ash coalescence with device-tuned density and optional glyph-mask sampling after profiling.
- Add the 30-minute ambient resurfacing policy only after lifecycle/battery review.
- Device-tune density for 60/90/120 Hz.
- Extend exact-return verification across EPUB and PDF device tests.

## Manga

### Integrated

- Mature Manga stack from `feature/manga-reader-screen-integration-v1` is now present on the canonical development line.
- Core reader state/controller.
- Local/offline cache and chapter locator.
- Image delivery planning.
- Coil 3 image pipeline.
- Very-large-image subsampling path.
- Chapter presentation/loading/navigation.
- Saved-state/process-recreation support.
- Source registry and execution coordinator.
- Source health/ranking/recovery.
- Browser challenge coordinator.
- Source replacement planner.
- Dedicated screen/view-model stack.
- Existing Manga unit test suite.
- Canonical product-session adapter that converts ordered source/offline chapter facts into one Manga reader session without guessing sort order.
- Session adapter rejects provider/source mismatch, providers without page capability, duplicate logical chapters, and missing restore targets before Reader construction.
- Source-replacement restore uses logical chapter identity rather than provider chapter key.
- Manga reader chrome/failure copy is localized in English/Persian and presentation errors no longer expose raw exception text.
- Safe transactional CBZ ingestion foundation: natural page ordering, Veil-owned cache paths, SHA-256 page hashes, zip-bomb limits, staging-directory commit, rollback on failure, and no direct trust in archive paths.
- Product-level `MangaHubScreen`.
- Library/Archive entry point.
- Restorable app route: `manga`.
- Live network sources remain OFF by default.

### Safety boundary

The current `ReadiumEngine` accepts EPUB and PDF only. W11 therefore does **not** route `BookFormat.COMIC` into the text Reader. Manga Hub exposes the product surface without creating a false or broken Readium fallback.

### Additional integrated W11 hardening

- `BookFormat.COMIC` is intercepted before Readium and opens through the dedicated Manga session repository + `MangaReaderIntegratedScreen`.
- Room schema v3 persists Manga chapter identity/order, source links, source chapter links, detailed progress, offline chapter manifests and offline page manifests while `books` remains the single catalog authority.
- Migration `2 -> 3` creates Manga tables without rewriting existing EPUB/PDF library data; cascade contracts remove Manga-owned rows when the parent Book is deleted.
- `RoomMangaProgressStore` synchronizes detailed reader progress and the Library-level progress summary transactionally.
- Close/back navigation uses a durable-close gate: the Manga screen remains open if the final progress write fails, and retry is explicit.
- Local CBZ import is wired into the document picker, performs app-private staging, content-fingerprint dedupe, safe extraction, offline-manifest persistence and first-page cover generation.
- Failed local import compensates across Room, app-private publication files and generated Manga cache; duplicate imports do not fork catalog identity.
- Manga deletion commits database ownership changes before deleting generated cache directories.
- Manga Hub now opens real local publications, uses generated covers, shows durable progress and exposes Start / Continue / Read again states.
- Backup schema v5 archives every local CBZ chapter plus compact exact Manga progress metadata, including the active chapter reading order; derived page cache/cover/fingerprint are rebuilt on restore.
- Restore remains compatible with schemas 1/2/3/4 and rollback rebuilds the previous Manga catalog/cache/progress from preserved source publications if the new restore cannot commit.
- Local Manga titles support ordered multi-CBZ batch import with natural filename ordering, duplicate chapter detection, inferred volume/chapter metadata and one Book identity for the whole work.
- Manga Hub exposes chapter management without creating a second catalog: full title/volume/chapter/language editing, reorder among added chapters, guarded deletion, and a pinned primary chapter whose source remains the Book publication authority.
- Identity-bearing chapter metadata edits migrate the offline cache to the new source-neutral chapter identity, reject collisions, preserve progress/source ownership and roll back to the previous cache identity on failure.
- Per-title storage accounting separates original CBZ source bytes from generated page-cache bytes; generated cache can be cleared independently and self-heals from local CBZ sources before reader open.
- Chapter reorder/delete re-computes the cross-product Book progress/finished summary so Library state cannot remain stale after catalog mutations.
- Derived cover/fingerprint updates use narrow Book-column writes so they cannot overwrite newer Reader progress.

### Next refinement gates

- Add whole-series metadata editing and optional grouping heuristics for independently imported CBZ titles without auto-merging ambiguous works.
- Design a reversible merge/split transaction before exposing any automatic work grouping.
- Add CBR only after a safe archive strategy is chosen and verified.
- Add source adapters only behind explicit opt-in and source-health policy.
- Keep MangaDex/live providers disabled until separately approved.
- Device-profile the Grayfog Manga chrome, image decode budgets and subsampling thresholds on 60/90/120 Hz phones/tablets.
- Execute Room migration/import/batch-management/cache-self-heal/schema-v5 backup round-trip instrumentation and full Manga reader device verification once the Android build gate is intentionally opened.

## Product invariants

1. Reader, Mirror and Manga share Veil identity but do not share unsafe state ownership.
2. Mirror never invents memories; it only resurfaces durable user data.
3. Mirror source return must preserve exact locator semantics.
4. Manga never falls back silently into EPUB/PDF navigation.
5. Network Manga sources are optional and disabled by default.
6. Reduced motion and high contrast are cross-product contracts.
7. No build/release claim is made from source integration alone.
