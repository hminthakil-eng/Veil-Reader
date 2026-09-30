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
- Product-level `MangaHubScreen`.
- Library/Archive entry point.
- Restorable app route: `manga`.
- Live network sources remain OFF by default.

### Safety boundary

The current `ReadiumEngine` accepts EPUB and PDF only. W11 therefore does **not** route `BookFormat.COMIC` into the text Reader. Manga Hub exposes the product surface without creating a false or broken Readium fallback.

### Next refinement gates

- Wire the canonical Manga session adapter between Manga Hub and `MangaReaderIntegratedScreen` once durable catalog/offline facts exist.
- Add durable Manga catalog/session persistence to the main app data layer.
- Add local visual-publication ingestion (CBZ first; CBR only after a safe archive strategy).
- Connect offline cache ownership and cleanup to app storage policy.
- Add Manga-specific cover/series/chapter metadata.
- Add source adapters only behind explicit opt-in and source-health policy.
- Keep MangaDex/live providers disabled until separately approved.
- Apply Grayfog visual language to Manga chrome without harming image fidelity.

## Product invariants

1. Reader, Mirror and Manga share Veil identity but do not share unsafe state ownership.
2. Mirror never invents memories; it only resurfaces durable user data.
3. Mirror source return must preserve exact locator semantics.
4. Manga never falls back silently into EPUB/PDF navigation.
5. Network Manga sources are optional and disabled by default.
6. Reduced motion and high contrast are cross-product contracts.
7. No build/release claim is made from source integration alone.
