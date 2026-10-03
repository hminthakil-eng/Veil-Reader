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

No changes to Room/database/schema, LibraryRepository, ReaderViewModel, navigation transaction or locator policy, page-effect listeners/states/geometry/drawing, Pdfium adapter, TTS session/backend, focus guide, lookup, ETA statistics, import, backup, sensory configuration or progression calculations. ReaderScreen changes only remove three ornamental Canvas calls and their import. Existing `PAGED/SCROLL` controls remain distinct from `PAPER/SLIDE/NONE` turn controls. Historical branches and main remain untouched.

Source refinements were reviewed twice: first for shared hierarchy/type/material; second for memory-column width, high-contrast image suppression, Sanctuary ornament, literal error behavior, Ritual action reachability, script tracking and wide-layout usable width. This does not substitute for rendered refinement.

Font derivatives retain original copyright/license metadata and use new internal family names to respect reserved font names. All four OFL notices and provenance ship in `app/src/main/assets/font-licenses`; source hashes and notices are also in this handoff's `fonts` directory. Font resources add approximately 2.9 MiB uncompressed. No runtime font download or external media service.

## Review facilities

`app/src/debug/java/com/veilreader/app/ui/review/GrayfogReviewPreviews.kt` exposes ten realm screens across seven preview configurations: compact phone, Persian 130%, compact 150%, Persian 200%, foldable, landscape and tablet. These are native Compose previews of actual screen code, with debug-only fictional fixtures. They have not been rendered/approved in this cloud session. Sanctuary requires actual EPUB/PDF fixtures and is deliberately not replaced with a simulated page.

`GrayfogShellAccessibilityTest` is a device harness with 16 parameter sets (English/Persian × 100/130/150/200% × standard/high contrast), three tests per set. It checks Threshold copy separation, resume/empty action reachability, 48dp Archive actions and nested click ownership. It captures entrance, resume, empty and index frames inside the test app's `files/grayfog-review` directory when executed.

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

Every major surface still requires actual Android screenshot inspection and iterative visual refinement. Bundled-font metrics can change wrapping beyond the changed screens. No claim of visual fidelity, DEVICE-GREEN, performance-green or release readiness is made from compilation. Physical paper refinements already present in canonical are retained; additional physics/shading tuning must be driven by page-turn captures rather than speculative rewrites. No literary files beyond the four images were attached; no unseen works were reviewed.

## Verification

See [VERIFICATION.md](VERIFICATION.md) for final-head commands and evidence. Verification builds are debug/instrumentation only; no release packaging. Preserve this document as the living record when device review begins.

## Reversal

Revert the design commit on this new branch or on another review branch. The starting integration branch is unchanged. Do not reset, force-push, delete historical refs, or merge this candidate into main without the outstanding visual/device gates and explicit authorization.
