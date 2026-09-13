# Veil Reader — product and delivery plan

The goal is a beautiful, trustworthy Android reader with a personal fantasy world that grows through reading. “Better than Apple Books” is a product ambition to validate with readers, not a quality claim or a reason to keep adding controls. The app must be comfortable for an hour of reading and delightful for a minute between chapters.

Baseline audited: main `d4bd80177aad2681004a75bbbd086898eb2f4ad7` (0.10.0), with a successful Android CI run. The attached archive contained the much older 0.3.0 source; it is not the implementation base. Existing imports, Room/DataStore, cover extraction, organization, notes, bookmarks, backups, Paths and Castle rooms are preserved.

## Delivery order

| Order | Outcome | Scope | Exit evidence |
| --- | --- | --- | --- |
| 0.11, tested preview | A home between pages | Explicit EPUB modes; opt-in 3D curl; typography and comfort controls; illustrated home construction, design, quests, residents and lore | Policy and unit tests; lint; APK; estate/settings backup round trip; manual rendering gates below |
| 0.12, this candidate | Setup and reading comfort | Onboarding, settings center, quiet mode, app themes, shared type preview, reduced-motion throughout app, PDF comfort controls and measured navigation | Unit/storage/backup tests, Compose interactions and native screenshots; phone checks remain |
| Next | Reading feel and accessibility | Fix issues observed on the phone; gesture/selection tuning; reduce-motion throughout app; proper type previews; font presets; PDF-specific controls; font-safe labels | Paired before/after screenshots and real EPUB/PDF reading sessions; TalkBack and 200% system text |
| Next | Release reliability | Verified Gradle wrapper; reproducible signed release; import/restore/restart smoke tests; lifecycle and storage failure handling; feedback-driven fixes | Signed install/update preserves data; test-suite and release gates green |
| 1.0 scope | Complete core reader | Useful session analytics; bulk shelf tools (existing separate PR); annotation export refinements; large-library behavior | Closed test on varied devices with no unresolved crash/data-loss/accessibility blockers |
| After stable core | Manga and webtoon | CBZ import; RTL/LTR; fit-width continuous strips; spread handling; caching and large-file limits | Real multi-hundred-page comic/strip samples, process recovery and memory tests |
| After stable core | Listening | EPUB TTS first; then audiobook files, chapter/speed controls, sleep timer and audio bookmarks | Interruptions, Bluetooth, foreground service and resume testing |
| Independent optional expansion | More of the world | Furniture slots, room interiors, resident story branches, expeditions, collectibles and companion cosmetics | Clear ownership/save rules; original art; repeat-safe rewards; no reading interruption |
| Only after privacy/sync design | Cross-device and AI | Optional account; conflict-safe encrypted sync; book-grounded, spoiler-aware tools | Explicit consent, deletion/export behavior, costs and a tested conflict model |

Do not rebuild working foundations or add a backend for a feature that can work locally. Reuse the pinned Readium/Compose stack and the existing project skills. External plugins are not needed for this slice. Native scene drawing avoids remote assets and extra rendering dependencies.

## The reading contract

- Resume a book in one clear action and restore its real locator.
- Keep game messages, particles, resident dialogue and construction outside the reader.
- Keep controls quiet, with at least 48dp actions and readable, wrapping labels.
- EPUB choices: Slide (default), 3D curl (beta), Instant, Continuous scroll.
- Existing PDF rendering remains native. Do not imply reflow, font changes or 3D curl are supported for PDFs.
- Curl bends a bounded bitmap of the real EPUB viewport over the live navigator. The navigator owns pagination, links, annotations and search. It is not a second text-layout engine.
- Curl blocks overlapping turn requests, cancels on pause or settings/navigation sheets, releases its snapshot on disposal, and expires after 1.8 seconds if a destination is slow to load. Test the effect at chapter boundaries before promotion from beta.
- No animation is required for reading; the system animation switch and reader reduced-motion preference use instant custom turns. Native PDF gesture behavior needs its own review.
- Keep-awake belongs only to an open reader and restores the previous window setting on exit.

## The rags-to-riches campaign

Your home starts as a wayside shack, irrespective of Path rank. Older readers keep all earned reading minutes; those minutes fund construction. Path rituals and their useful chambers remain a separate, existing system. Basic reading, notes and the library never require a house upgrade.

One active reading minute = one lorestone. Construction spends lorestones once. Completing a story chapter awards its fixed bonus once. Story progress does not reset at midnight. No clicker rewards, background minutes, mandatory late-night reading, paid boosts, maintenance, decay or missed-day penalties.

| Home | Lifetime active reading needed | Construction cost | Arrival |
| --- | --- | --- | --- |
| Wayside shack | 0 minutes | Free | Mara, lantern keeper |
| Lamplit cottage | 15 minutes | 15 lorestones | Oren, builder |
| Reader’s lodge | 60 minutes | 45 lorestones | Sable, mapmaker |
| Wisteria manor | 150 minutes | 90 lorestones | Iona, gardener |
| Moonwatch keep | 300 minutes | 150 lorestones | Vesper, archivist |
| Lantern castle | 600 minutes | 300 lorestones | The gate and twin towers |
| Citadel of stories | 1,200 minutes | 600 lorestones | The valley’s beacon |

Story quests span 5 to 1,200 cumulative active minutes. Each asks for the previous chapter, its reading milestone and, where stated, a constructed home. All can be completed by reading EPUB or PDF; annotations or a specific book are not required. Pure domain tests traverse the entire campaign to detect unreachable gates.

Design is a real saved choice: home name; Moonstone / Amber oak / Wisteria materials; wildflowers / lantern walk / fountain grounds; Moonlight / Daybreak / Silver mist. Unlocks are permanent and switching earned decorations is free. This slice does not claim drag-and-drop furniture editing.

## World and NPC direction

The Lantern Road is an original quiet mystery about a house missing from every map, a borrowed lamp, and a valley returning to memory. Its emotional arc is shelter → community → curiosity → sanctuary, rather than conquest.

Residents offer authored contextual conversation topics about the home and discovered lore. They are not remote AI chatbots. Unclaimed lore is not revealed through dialogue. Future branching stories should retain this spoiler boundary and add choices only when their consequences can be saved and explained.

## Acceptance before calling this polished

1. On the user's Android phone: compare the shack, cottage, manor and citadel, including all three skies in light/dark UI. Inspect actual app screens, not a web mockup.
2. Change the home name, colors, grounds and sky, restart, then backup/restore. All choices, spending and claimed story IDs must survive.
3. Re-tap claim/build rapidly: no duplicate reward, unintended skipped stage, or negative balance. Read with game screens closed: minutes still fund the house.
4. Switch between all EPUB modes at a mid-chapter locator. Reopen/rotate/restart; check the same passage, selection, links, TOC, bookmarks and annotations.
5. Curl: LTR and RTL, backward and forward, chapter and book boundaries, long chapters with images, slow destination rendering, rapid gestures, process recreation and low-memory fallback. No blank page or stuck cover.
6. Scroll: no horizontal page-turn interception, usable selection and footnotes, reliable resume position.
7. Accessibility: TalkBack, large text, small phone landscape, reduced animations, contrast and focus order. Decorative artwork exposes one useful description.
8. Record release-build metrics on a midrange device: cold start, import latency, memory and page-turn frame distribution. Targets: 60Hz frame budget of 16.7ms, no recurring visible pauses, no memory growth across 200 turns. These are targets, not measurements achieved here.
9. Complete production signing and the existing `WORLD_CLASS_RELEASE_GATES.md` before a production label. A passing debug APK alone is not the final product.

## Verified API references

- Readium 3.3.0 [EPUB preferences](https://github.com/readium/kotlin-toolkit/blob/3.3.0/readium/navigator/src/main/java/org/readium/r2/navigator/epub/EpubPreferences.kt): typography and scroll configuration.
- Readium 3.3.0 [directional navigation](https://github.com/readium/kotlin-toolkit/blob/3.3.0/readium/navigator/src/main/java/org/readium/r2/navigator/util/DirectionalNavigationAdapter.kt): reading-progression-aware page navigation.
- Readium 3.3.0 [input listener](https://github.com/readium/kotlin-toolkit/blob/3.3.0/readium/navigator/src/main/java/org/readium/r2/navigator/input/InputListener.kt): handled-event contract for taps, drags and keys.
