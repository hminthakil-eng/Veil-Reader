# Grayfog Masterpiece v1 — living design handoff

Working branch: `design/codex-grayfog-masterpiece-v1`.
Base: `integration/arena-reader-canonical-convergence-v1` at `18fe4ab78b381ba44fbbfc1817a00d3f8253bfcf`.
This is a reversible design candidate, not a main promotion or release.

## Design intent

A reader enters an archive, identifies the volume that awaits them, and crosses into a quiet publication. Brass registers ownership and structure. Parchment identifies reading and preserved documents. Blue-black architecture carries memory; it does not turn every control into a ceremonial object. The Sanctuary remains the lowest-density realm.

The supplied screen boards establish editorial hierarchy, unboxed book objects, thin engraved edges and a quiet publication. The detailed library reference establishes the exceptional parchment resume slip surrounded by dark architecture. The identity close-up informs restraint in material contrast; its logo and proprietary-looking artwork are not copied into application resources.

## What changed and what remains owned

| Change | Why / reference | Preserved behavior |
|---|---|---|
| Offline licensed shell typography: Playfair-derived editorial face, Cormorant-derived prose, Source Sans-derived utility, Vazirmatn-derived Persian/Arabic | A/B/C measured editorial identity; utility labels become legible records rather than monospace diagnostics. Persian is designed explicitly. Labels have an 11sp floor, medium labels 12sp; Latin prose has more optical room. | Publication typography is not assigned these fonts. Readium/PDF preferences and publisher styles unchanged. Existing script policy and zero Arabic tracking preserved. |
| Semantic material roles and one realm budget source | Shared backgrounds, surfaces, parchment, ink, brass, moonlight, states, text, divider, frame, ornament and depth; Arena remains canonical. | Legacy VeilPalette names delegate; no second theme architecture. Shape, spacing and motion families reused. |
| Quiet ArchivePanel: flat blue-black inset, thin frame and sparse registration | A/B frame hierarchy; repeated utility panels must not resemble ritual chambers. Removed radial glow, duplicate top/side rules and corner diamonds. | Existing panel content, adaptive sizing, callbacks and reduced-motion content-size behavior retained. |
| Threshold expandable editorial doorway | B centered literary entrance; D image-to-parchment hierarchy. Copy is laid out sequentially and determines height instead of competing in a fixed image box. Seal is smaller and identity is one coherent rail. High contrast suppresses the image. | Hero selection, last-opened ordering, recent shelf, whisper provenance, Mirror, reading ledger and one primary read action retained. |
| Resume percentage is an inscription, not a framed badge | D precise reading record. Parchment remains exceptional; the cover remains untouched. RTL chevron points along shell navigation. | Progress and chapter data remain canonical. Opening uses the same callback and return ritual. |
| Archive index entries become ruled records | A/B retrieval-first spatial grammar. Removed perimeter boxes; retained a solid archive field for predictable contrast. Title/author allow two or three lines, state metadata wraps, memory summary lives below metadata. Action rail stacks at larger text. | Gallery, Shelves and Index remain separate. Search normalization/relevance, filters, collections, series, favorite and detail actions preserved. |
| Book Detail maintenance actions become text utilities | B cover and reading action dominate the artifact chamber. Favorite still communicates its state in text. | Primary reading action, history, milestones, memory, collections, publication cover, metadata editing and terminal confirmed deletion retained. |
| Sanctuary dialogs lose corner ornament | A/B reading instrument restraint. Footnote, atomic-note and Appearance keep ordinary structural borders. | Selection ownership, draft atomicity, Appearance settlement, TTS, Focus Guide, Lookup, ETA, PDF behavior and all page-turn paths unchanged. |
| Notes/Profile/Observatory provenance labels use full semantic secondary ink | A/B archival legibility. Atmospheric alpha remains in artwork, not these records. | Exact passage locators, historical uncertainty, Echoes, Time Capsules, atlas relations, discoveries and dossier derivation retained. |
| Measured two-room composition for Threshold, Castle and Observatory | D depth and architectural adjacency. Threshold gives more width to the volume; world map/atlas gets the dominant column. At insufficient usable width or large text, reading order stacks. | One scroll owner, original rooms/selection/routes and all world progression calculations retained. Compose Row respects RTL; publication navigation is not mirrored by this shell change. |
| Ritual confirmation scrolls and reuses adaptive actions | Ceremony should be brief and dismissible at 200%, landscape and Persian. Directional rank inscription respects RTL. | Advancement gate, confirmation, cancellation, invocation, unlocks and reduced-motion ceremony unchanged. |
| Notice dialog scrolls; message uses readable ink on its actual dark material | Recovery is literal and must remain accessible. The historical raw-English audit finding was already superseded in canonical VeilApp. | Existing localized typed issue categories, error truth, dismissal and durable operations retained. |
| Reduced-motion loading uses a stationary indeterminate register | A celestial registration without unnecessary movement. No invented completion percentage. | Polite live-region label and indeterminate progress semantics retained. |

## Source review and regression boundary

No changes to Room/database/schema, LibraryRepository, ReaderViewModel, navigation transaction or locator policy, page-effect listeners/states/geometry, Pdfium adapter, TTS session/backend, focus guide, lookup, ETA statistics, import, backup, sensory configuration or progression calculations. The initial ReaderScreen pass removed three ornamental Canvas calls. The continuation additionally refines Appearance control materials and localized specimens; rendering preferences, navigation and settlement logic remain unchanged. PaperCurlDraw changes only three visual grain/highlight constants. Existing `PAGED/SCROLL` controls remain distinct from `PAPER/SLIDE/NONE` turn controls. Historical branches and main remain untouched.

Source refinements were reviewed twice: first for shared hierarchy/type/material; second for memory-column width, high-contrast image suppression, Sanctuary ornament, literal error behavior, Ritual action reachability, script tracking and wide-layout usable width. This does not substitute for rendered refinement.

Font derivatives retain original copyright/license metadata and use new internal family names to respect reserved font names. All four OFL notices and provenance ship in `app/src/main/assets/font-licenses`; source hashes and notices are also in this handoff's `fonts` directory. Font resources add approximately 2.9 MiB uncompressed. No runtime font download or external media service.

## Review facilities

`app/src/debug/java/com/veilreader/app/ui/review/GrayfogReviewPreviews.kt` exposes thirteen native screen/component specimens across seven preview configurations: compact phone, Persian 130%, compact 150%, Persian 200%, foldable, landscape and tablet. These are native Compose previews of actual screen code, with debug-only fictional fixtures. They have not been rendered/approved in this cloud session. Sanctuary requires actual EPUB/PDF fixtures and is deliberately not replaced with a simulated page.

`GrayfogShellAccessibilityTest` is a device harness with 16 parameter sets (English/Persian × 100/130/150/200% × standard/high contrast), four tests per set (64 cases). It checks Threshold copy separation, resume/empty action reachability, 48dp Index/Gallery actions and nested click ownership. It captures entrance, resume, empty, index and gallery frames inside the test app's `files/grayfog-review` directory when executed.

Run on an attached Android device:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.veilreader.app.ui.screens.GrayfogShellAccessibilityTest
adb exec-out run-as com.veilreader.app tar -C files -cf - grayfog-review > grayfog-review.tar
```

The connected runner uses the target debug application; confirm the application id if a local build uses a suffix. Screenshots are evidence to inspect, not automatic golden approval. Device harness execution is pending.

## Remaining visual gates

| Surface | Required evidence / next refinement |
|---|---|
| Threshold | Phone portrait at all scales; image crop, title-to-volume proportions, immediate resume path; tablet side-by-side balance; empty import action |
| Library / Collections | All three presentation modes, search and no-results, long title/author/collection/series in English/Persian; index action ownership; contrast against shell theme variants |
| Book Detail | Real cover aspect ratios, long metadata, parchment CTA above secondary history; deletion confirmation and safe Back |
| Sanctuary / paper | Real EPUB/PDF, Paper/Sepia/Dusk/OLED; native selection/TTS/Focus Guide; Paper, Slide, Paged, Scroll; weighted curl, fibres, ghosting, contact/crease shade and many-hour readability; reduced motion; progress across close/reopen |
| Appearance / Settings | Quick/Advanced, unsupported capability gates, 200% and landscape, font-resource metrics, theme independence, hardware-key and tap-grid utilities |
| Notes / Highlights / Memory | Real saved locators, unknown dates, filtered/empty states, Echoes and capsules; no invented history; large Persian records |
| Profile / Observatory | Dossier semantics, connected/isolated atlas, selected record on wide layout; large text restoration to stacked layout |
| Castle / Ritual / Sanctum | Tablet/foldable room proportions, TalkBack order, chamber targets, skippable Ritual at 200%, earned/locked relics, museum hierarchy |
| Loading / Errors | Static Reduced Motion progress semantics; TalkBack announcement, long Persian recovery copy, readable dismissal in landscape |

Every major surface still requires actual Android screenshot inspection and iterative visual refinement. Bundled-font metrics can change wrapping beyond the changed screens. No claim of visual fidelity, DEVICE-GREEN, performance-green or release readiness is made from compilation. Canonical paper geometry and ownership are retained. This continuation lowers source-safe material intensity; additional physics/shading tuning must be driven by page-turn captures. The curated literary reference was reviewed; no claim is made to have read unseen EPUB files.

## Verification

See [VERIFICATION.md](VERIFICATION.md) for final-head commands and evidence. Verification builds are debug/instrumentation only; no release packaging. Preserve this document as the living record when device review begins.

## Reversal

Revert the relevant design commits on this design branch in reverse order. The starting integration branch is unchanged. Do not reset, force-push, delete historical refs, or merge this candidate into main without the outstanding visual/device gates and explicit authorization.

## Continuation delivered on the same PR / branch

The curated literary reference informs controlled revelation and archival matter. Architecture surrounds the working object; it no longer competes through repeated frames. No new branch, design-system replacement, artwork extraction or engine replacement was introduced.

- **Threshold:** unframed environmental doorway, stronger current cover than recent covers, quiet paper registration, simplified explicit resume affordance. Removes an ungrounded red ornament and nested circular button. References A/B/D.
- **Archive:** results precede optional history; mode/facet rails wrap independently; Gallery becomes unboxed cover objects, Shelves gains a shallow common datum, Index stays continuously ruled. Search/relevance/filter/sort and nested action ownership retained.
- **Artifact chamber:** usable-width composition replaces the raw-phone-width breakpoint. Parchment Read/Continue sits with identity before secondary progress/history. Collections wrap; favorite/edit/delete rules remain.
- **Sanctuary instruments:** Appearance choices use cool working material rather than brown fill; theme specimens use a localized scalable sample. Settings headings are readable literal titles. Publication fonts and capabilities remain independent.
- **Reading records:** one shared ledger rule replaces perimeter boxes in Archive folios and Notebook records. Book/category and return/edit/delete rails wrap at large text. Stored sources, uncertain history, Echoes/Capsules and atomic Notes remain.
- **Physical paper:** lower mottle/oxidation/fibre/speck alpha and active age-derived backside grain; lower fallback crease-light/grain defaults. Explicit Reader theme-specific crease-light values are deliberately preserved for device-led tuning. Contact shadows, age integration and faint ink ghosting remain. No fold geometry, gesture threshold, velocity, bitmap lifetime or page settlement change.
- **Observatory / Castle:** grounded edges become more visible and thinner; selected record is quieter. Castle chambers receive full readable width below the shared usable-width threshold; sufficient widths retain alternating rooms/bridges and memory architecture. No fabricated relationships or unlock rules.
- **Path / Profile / Sanctum:** journey before statistics; wide adjacency follows the existing composition helper. Mastery labels no longer compete in a rigid row. Ordinary goal/equip actions lose gold fills; permanent-title captions and meters receive better hierarchy/wrapping. Ceremony/dismissal and progression gates retained.

New native review specimens cover Book Detail, Gallery object and Shelves. These supplement the actual Library preview; the Gallery object specimen is not evidence of a complete rendered Library screen. The expanded device harness checks Gallery utility targets and confirms that favorite/details do not open the volume, across English/Persian, 100/130/150/200% and high contrast. Compilation is the only executed harness evidence here.

Remaining weaknesses are deliberately open: Threshold crop/light and entry-to-resume distance; Gallery metadata/action baseline rhythm; continuous shelf depth; compact artifact balance; Persian shaping/wrapping; Castle corridor spatial quality; Observatory node selection; paper fold illumination and long-session texture comfort. Native screenshots are needed to choose further optical corrections responsibly. The PR remains Draft.

The large-library retrieval review adds a direct Series menu to the wrapping filter instruments. It uses existing source-derived series wings and the existing seriesFilter; choosing a series clears the collection exactly as the wing route does. All-series/reset remain immediate. Archive wings may stay below results without making series retrieval depend on scrolling through the entire library. No second filtering pipeline was added.

## V3 continuation: native-reviewed source candidate

Continued the latest fetched remote `0888bf0` on the existing branch/PR. The V3 audit above supersedes the earlier claim that only device-led improvements remained. A lightweight native rendering path was available through the existing Robolectric stack and the same Compose test dependency already used by instrumentation. Its addition is test-only; no production screenshot renderer or new UI architecture was introduced.

The strongest changes concern reading access and structural grammar: Threshold preserves a measured identity column and moves the stacked action before the cover; Library removes duplicate approach headings, retains status filters in all modes, grows Gallery/Shelf records with text scale, establishes editorial caption rhythm and a continuous shelf datum; Book Detail uses one identity/action measure. Mode choices use cool instrument plates while remaining semantically distinct. Notebook search loses its redundant perimeter. Observatory records wrap without a competing strength column. Castle gains a readable narrow stair register and adjacent memory inscriptions; Sanctum becomes a seal beside a ruled title registry; Profile gains a reachable wrapping Settings action, adaptive fact/history records and localized counts. Small missing-cover substitutes no longer carry tiny duplicate archive stamps. Original imported cover bitmaps remain unchanged.

Semantic readable measures and map proportions live in the existing design system. The architectural-pair policy now permits a readable folded-width working surface and still stacks when accessibility scaling removes that room. No hinge position is invented. Shell/dialog system-bar icon appearance follows dark architecture versus light paper, and an independent dialog cannot recolor the underlying paper window. Icon visibility, immersive gestures and navigation ownership are not changed.

### Review facilities and their scope

- `GrayfogReviewFixtures.kt`: thirty deterministic fictional surface cases, including 0/42/99/100% progress, long English/Persian identity, short title/missing author, 48 books/16 series/large collections, empty/search/no-results, populated/empty notes, highlights/bookmarks, isolated/maximum-node Observatory, low/advanced Castle, ready Ritual, locked/populated Sanctum, loading and errors. Known fixture dates and unknown annotation dates are deliberate. These records are never imported, persisted, or loaded into production history; book-opening and maintenance callbacks are inert.
- `GrayfogReviewPreviews.kt`: native previews of all thirty production specimens across seven configurations (210 preview instances). These replace the earlier sample-only review set. They use the shared debug registry rather than duplicate screen implementations.
- `GrayfogCloudRenderTest`: native API-35/Skia frames across eleven configurations, thirty cases each (330 frames). English 320/412/600dp, Persian 100/130/150/200%, contrast at Persian 200%, landscape, folded-width window and tablet. It asserts the first reading action and Profile Settings remain displayed/actionable. A dialog is captured from its own root, avoiding the blank background capture found in an intermediate iteration.
- `GrayfogCloudAccessibilityTest`: actual narrow Gallery utilities at ordinary text and Persian 200%; checks 48dp targets and that favorite/details do not trigger the book-open callback.
- `GrayfogSystemBarsTest`: icon state changes in both fields and independent dark-dialog versus underlying light-paper ownership.
- Existing device accessibility harness and all Reader regressions remain. Native cloud rendering does not replace connected instrumentation, TalkBack, actual Readium/Pdfium content or physical page turns.

See [DEVICE_CAPTURE.md](DEVICE_CAPTURE.md) for reproducible native/device commands, deterministic names and the separate live-publication matrix. Debug-only `GrayfogReviewActivity` has its own task affinity, reads only fictional review objects and never instantiates the library repository or saves preferences. The capture script clears only that review task; it does not force-stop the ordinary application. Release variants contain neither the activity nor its fixtures.

### Exact unresolved questions

1. Do real imported covers retain the intended current/recent dominance, caption baseline and continuous shelf depth on physical 320/360/412dp screens? Native substitutes cannot answer artwork crop or physical image contrast.
2. Does actual Persian shaping/hinting and TalkBack traversal match the cloud API-35 layout at all four scales, including long source metadata, dialogs and keyboard/IME overlap?
3. Are the compact Castle stair datum, chamber thresholds and folded-width atlas/record proportions optically coherent on real screens and real hinges? Native window sizes do not simulate posture or touch exploration.
4. Does the live publication keep Sanctuary quiet during selection, footnotes, TTS/Focus Guide and close/reopen? No simulated publication is used in the shell review.
5. Does paper/curl have believable weight, backside tint, ghosting and contact/crease response during real gesture frames without texture fatigue? Geometry and ownership were deliberately not retuned from shell evidence.
6. Do API-26/37 system bars, light/dark OS settings, display cutouts, edge-to-edge navigation, landscape and IME show the expected contrast? JVM window-flag checks do not capture actual system pixels.
7. Does a dense real archive remain responsive and clearly selectable in Observatory, and do memory/unlock records preserve their significance with genuine long-term data? Fictional extremes exercise composition, not historical acceptance.

These are open device/publication/optical gates. The PR remains Draft; no final visual acceptance, DEVICE-GREEN or release claim is made.

### Mixed-script metadata refinement

Native Gallery review exposed a separate typography contract: a Persian book in an English interface must still use the licensed Persian family, connected glyphs and adequate leading. `withVeilContentScript` preserves hierarchy/weight while selecting that family, zero tracking and at least 1.5× leading for Arabic-script metadata. Explicit title/author/series/quote records use it; the shared tracking helper also applies it before intentional tracking overrides. English styles and publication typography remain independent. Two pure typography regressions protect this behavior. Final physical shaping, mixed-script baseline rhythm and real cover contrast remain optical gates.

The remote advanced to `56bc0faa823c5295bcf1367dae2832de88edc965` with a Codemagic-only naming/artifact change during this continuation. That change is preserved; this continuation does not replace another contributor’s work.

Final native folded-width refinement: the 720dp Castle specimen exposed mid-word rank/floor wrapping. The existing architectural pair now supports a semantic minimum readable record width; Castle reserves 280dp at normal text scale before allocating up to 68% to the map. Its corridor floor registration rail is 84dp at normal text scale rather than 48dp, and grows with accessibility text scaling. Large text still restores stacked composition; room/unlock data and callbacks are unchanged. This is a native-render-driven correction, not a physical hinge acceptance claim.

## Next continuation — long-title reading access

Continues remote `f8300095737156f045d993ac2cc11d0b2c697c78` on the same Draft PR. In a stacked Threshold artifact, the reading action now follows the title before author/series metadata. The title remains bounded to three lines, secondary records remain visible below, and normal paired cover/identity composition is retained. Progress inscriptions receive Persian content typography when chapter metadata uses Arabic script even in English UI. No callbacks, current-volume selection, progress semantics or publication owner changes.

The review registry gains `THRESHOLD_PERSIAN_LONG`: a fictional current volume with long Persian title, author and series. The native suite asserts immediate actionable return for this fixture and the existing active fixture. The registry now has 31 surfaces, 217 preview instances, 341 native frames and 279 deterministic future device captures. Previous V3 manifests/images remain historical evidence rather than being silently replaced. Real device typography, TalkBack and publication/gesture gates remain pending.

## All-out continuation — 2026-10-04

Continues the existing design branch from `77be8b473d79f80deb79c0eb141dfd8c802f8099`; canonical base fetched as `18fe4ab78b381ba44fbbfc1817a00d3f8253bfcf`. PR #371 remains Draft.

- **Archive:** shared adaptive approach policy measures the actual window and removes optional narrative density during active retrieval, large text and short windows. Wide title/actions gain meaningful adjacency instead of stretched controls. Search uses utility/script-aware typography, short localized placeholder and an accessible label that survives input. Actual sorting/filtering/import/search ownership is unchanged.
- **Book Detail:** Read/Continue now follows identity before series, collections and historical inscriptions. Cover artwork, progress semantics, imported-source availability, terminal confirmed deletion and callbacks are preserved.
- **Appearance:** constrained layouts initially disclose the reading preview rather than crowding out controls. Normal spacious layouts retain the original visible preview. Toggle is 48dp and localized; tests protect preference and Done callbacks. Live typography draft/slider/disposal settlement, fixed-layout capabilities, Paged/Scroll and Paper/Slide separation are unchanged.
- **Observatory:** source-derived selected edges stand apart from recessed context. Reading action precedes the relationship ledger. Six connections remain a calm initial view, but all are now reachable and collapsible; selection changes reset disclosure. No graph data, positions, hit regions or relationship inference changed.
- **Castle:** optional inner-keep instruction yields space to actual rooms at large text/short height; truthful room state, descriptions and architectural layout remain. Threshold uses the same policy with equivalent prior behavior.
- **Review infrastructure:** 31 actual production surfaces × 15 native configurations = 465 frames; 31 × 11 Compose preview configurations = 341 previews. Added interaction coverage for populated search semantics, connection disclosure/reset/exact reading return, preview disclosure ownership and adaptive policy boundaries. Fictional records never become production history.

Source review and native rendering are evidence of layout/interaction, not device acceptance. Remaining optical questions: Library first-cover balance at 320dp/200%; long Persian Book Detail cover/title/action balance; Observatory dense context contrast and physical node targeting; Castle architectural depth and room ordering on actual hinges; preview disclosure discoverability and live-publication behavior. Reproduce the documented matrix, then inspect real Readium/Pdfium, paper/slide mid-drag, RTL publication progression, TalkBack, IME/system bars and long-session paper comfort. No blanket visual completion is claimed.

Published candidate GitHub Actions are infrastructure/account-blocked before execution: all three jobs have zero steps and no logs. See VERIFICATION.md and allout-github-ci.json; no CI-GREEN is claimed. Cloud source hashes and completed checks remain unchanged.

## Detail continuation — 2026-10-04

This pass addresses small details with concrete consequences: overlapping fallback-cover ornament, mixed-script typography, empty archival identity, missing heading navigation and invented epoch provenance. It continues the existing Draft PR, rather than creating a parallel product.

Generated covers use their actual spare space for registration and require readable width/height for duplicate captions. Existing cached publication artwork, cover decoder/cache, fade/reduced-motion behavior, cover semantics and artifact history layers remain. Persian title/author receive the licensed Persian family and appropriate leading even in English UI; shared labels/subtitles gain the same protection.

The shared Untitled display fallback trims presentation text without writing metadata. Gallery, Shelves and Index, recent/resume objects and Observatory selected records share it; callbacks keep the original Book/ID/URI. Known and unknown provenance remain distinct: zero timestamps now explicitly say Date unknown, while real positive dates follow the active UI locale and time zone. No annotation timestamp, schema or history is rewritten.

Heading semantics make editorial hierarchy available across eleven tested surfaces, including the separately composed Notes and Settings headers. ReaderScreen changes only import heading semantics and mark the Appearance title; publication/navigation/TTS owners, page geometry and draft settlement remain unchanged. Threshold now measures actual window height for the existing shared approach rule.

Review registry adds LIBRARY_MISSING_METADATA: one fictional whitespace-title/missing-author completed volume in two collections. Native mode switching checks Gallery, Index and all three legitimate shelf registers at English and Persian 200%. Registry: 32 surfaces, 480 native frames, 352 Compose previews and 288 future scripted device captures. Fixtures never persist or become production history.

Remaining optical questions include actual missing-cover proportions on small displays, font hinting/shaping, real TalkBack heading traversal, artwork crop/contrast, landscape/hinge behavior, live publication controls and paper/slide physics/comfort. Do not mark these complete from cloud results. Existing device matrix and Draft status remain mandatory.
