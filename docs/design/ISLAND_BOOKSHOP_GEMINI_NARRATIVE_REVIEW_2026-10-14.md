# Veil Reader — Island Bookshop origin / Gemini 4 Argon independent consultation
**2026-10-14 | Proposed product story, not implemented**
**User-origin premise**: shipwreck survivor awakens with amnesia and no possessions/contacts on an island beach. Sick elderly bookshop owner rescues the survivor and provides refuge in his upstairs home above his old bookshop. He needs help running the old shop, and the game begins.
**Visual identity already requested**: customizable high-res anime character and facial cut-ins with a living atmospheric **pixel-art island, town, shop**; synced pixel-world player sprite.
**Live consultation**: Gemini desktop `4 Argon` responded to a source-constrained creative/architecture prompt on Windows Desktop Commander; entire reply exported in the local development computer at `E:\EyadStudio\Temp\Gemini_Bookshop_Shipwreck_Response_20261014.txt`. This memo is edited synthesis and technical critique of its **creative proposal**, not an approved story canon or a shipped app.

## Summary decision
Gemini judged the **Coastal Bookshop + Mysterious Island + Castle Exploration** integrated direction more emotionally grounded and coherent than pure Rags-to-Riches or abstract Sunken Athenaeum. The small old bookshop becomes the player's **permanent home/hub**. Rank-locked Castle and earlier Camp/Hut/House visuals are reinterpreted as landmarks on the island, but existing save-state values and rank authority **must not change**.

**Recommended pilot title:** *The Forgotten Shore: The Old Bookshop* (working English); old man's name *Master Sohrab* (Gemini's candidate; user approval pending). World *Ebony Island*, shop *Leaf & Wind* are proposal-only names, not locked canon.

## Gemini's five most important creative corrections
1. **Elder autonomy.** He should be an intelligent, somewhat sardonic artisan/bookbinder with agency and a stable chronic limitation, not an incapacitated quest dispenser or ticking death timer. He does small work on his own while player is away and never threatens/streak-shames. Character motivations first; avoid medical misinformation.
2. **Amnesia without chosen-one trope.** Memory is a theme about identity and freedom to define oneself now, not a lazy prince/secret savior reveal. Backstory clues reveal possibilities without railroading player gender/appearance or purpose.
3. **Bookshop as social/cultural archive, not a sales factory.** Books circulating, restoring water-damaged originals, shelf curation and welcoming NPCs create visible restoration. Do not force labor/check-ins or commerce grinding.
4. **Consistent island metaphysics.** Author a coherent in-world rule that written ink/memory may interact with the Grayfog — a fictional narrative conceit; never claim actual private user book contents are needed for puzzles.
5. **Real user books and in-world books separate.** Reading progress feeds optional cosmetic/repair affordances from existing durable aggregate counters; no automatic private text/annotation scanning, forced book completion, spoilers or quizzes.

## Three Gemini mystery arcs (alternatives subject to editing)
- **Unwritten Logbooks:** island harbor records mention improbable ships, perhaps linked to a floating archive. Ground clues in original in-world artifacts.
- **Cartographer's Debt:** the elder was once a conservator/architect attached to the higher castle archive; he chose free access to books over censoring history.
- **Margins of Strangers:** several other islanders washed ashore before; marginalia hints the player may have visited and left once already. Treat this as a speculative option, not forced canon; false memories and amnesia deserve sensitive writing.

## Playable 8–12 minute prologue — proposed, low-budget
1. **Beach (0–2m, pixel):** solitary shipwreck survivor wakes near flotsam, taps to move/examine. Choose one of three found objects (broken compass, rain-washed journal, carved key); other items not necessarily permanently lost. Lantern-bearing elderly bookbinder approaches and safely escorts player.
2. **Loft above shop (2–5m, pixel + anime UI):** safe warm room, cup of tea and window view of city/castle. Mirror opens anime avatar editor: same appearance immediately reflected in low-res player sprite. Player is never forced into predefined gender/history.
3. **Bookshop below (5–9m):** talk to elder, dry three storm-damaged in-world books using a short tap/swipe bookbinding interaction; choose respectful response. A folded page reveals a cryptic castle diagram.
4. **Optional reader link and hook (9–12m):** offer `Open My Library` or `Continue helping`; neither locks prologue or blocks EPUB/PDF. A mysterious customer leaves a book bearing the same mark as the shore relic, dated tomorrow. Keep narrative ambiguity and avoid spoiler auto-generation.
5. **Return rule:** close world at every stage and resume the precise existing book locator via native navigation. If no book exists/imported, prologue still completes.

## Early island footprint (Gemini proposal)
- **Hub**: two levels of one old shop (lower shop, upper loft), restoration desk, front counter and 4–6 decor positions. Basement becomes a later optional P2 mystery.
- **3 reachable conceptual locations later**: fog pier/lighthouse, glasshouse tea room, uphill gate/castle.
- **NPCs**: Mahān, a retired lighthouse keeper tracking strange storms and his missing child; Tara, a botanist/illustrator studying changing island flora; Soroush, an archive official secretly preserving rather than burning vulnerable texts. These are candidate fiction, not approved/coded.
- Early reversible choices: share/hide port ledger (changes relationship/dialogue), warm literary nook vs map-study shop theme (decor), shore relic restored vs displayed (focus on old vs new identity). Neither option permanently gates Reader.

## Existing feature compatibility
- Existing `GameRepository` remains the sole owner of durable actual reading totals; do not create a replacement accounting ledger or copy authoritative Rank/Path/library counters into a narrative save model.
- Existing `RagsToRichesAvatar` / AvatarAppearanceStore remains cosmetic identity owner. Hero portrait and pixel sprite map via a deterministic **visual catalog**, not two separate mutable character definitions.
- Existing home high-water stage can map to visible landscaping/cosmetic locale evolution; **Rank/Path remain sole authority** for Castle room gates (issue #314).
- Reader `PAGED/PAPER_CURL/SLIDE/SCROLL`, notes/TTS/PDF remain independent; no extra rendering or scene tick during Reader.
- Preserve old saves and export/restore. New prologue story flags only in a versioned additive store, with migrations, corrupt-data fallback, process-death semantics and idempotency.

## Technical corrections to Gemini's illustrative Kotlin — MUST address before implementation
- Proposed `ResonanceCalculator` increments points by `max(1, floor(deltaMinutes/5))` *per update*, and advances `lastProcessedReadingMinutes`. This is exploitable/inconsistent: five separate 1-minute updates yield 5 points whereas one 5-minute update yields 1. It also loses fractional carry across updates. **Reject as written.** A safe candidate is a pure cumulative entitlement function, e.g. `max(0, eligibleTotalMinutes / 5 - previouslyGrantedUnits)`, with one transactional/high-water persisted grant journal; define TTS eligibility explicitly from existing accounting, test exact conservation under batching/restarts/restore, and never 'grant 1' on each nonzero delta. Do not assert it passes tests without evidence.
- Proposed `IslandGameSaveV2` repeats `rank`, `path`, `readingMinutes`, `finishedBooks`, `unlockedCastleRooms` and `avatarConfig`, contradicting one-writer invariant and risking save-data split brain. **Narrative state must store only** `prologueStage`, `beachRelic`, `shopVisualState`, `storyFlags`, `version` and deduped game-specific event IDs. Read Rank/counters/cosmetics from existing owners. Do not ever silently overwrite existing saves.
- Illustrative Compose `drawImage(dstSize = canvas full screen)` stretches a pixel room to arbitrary fractional scale and violates its own pixel-crispness criterion; use controlled virtual resolution/int scale, letterbox/crop and `FilterQuality.None`. UI and in-game coordinates must remain separate across RTL.
- The proposal assumes TTS is already credited identically to visual reading. Actual repository accounting and consent need a source-backed policy before implementing that promise.
- Decorative book recommendations and mood tags must be **explicit opt-in** per book if linked to real library; never transmit real filenames or annotations to non-Reader game code without user choice.
- Giant new package tree / save table is a **design sketch**, not runnable source. Validate actual package layout, real database migrations and app contracts before any code change.
- Gemini's categorical claim that embedded Godot necessarily `explodes` app size/battery is not supported by a measured same-content benchmark. Native Compose pixel rendering is appropriate for 1-room pilot; leave 3D renderer selection open for later proven needs.

## Small vertical slice (P0 acceptance)
**One-room shop + short beach intro, no full island.** Native Compose 2D pixel scene with a deterministic sprite atlas + framed high-res anime portrait; reuse appearance editor with visual catalog; 3 meaningful clickable hotspots (mirror / book restoration desk / front lantern), elder conversation and one optional Reader CTA; current authoritative progress projection only. Zero timed consequences if user doesn't play.
- Test local offline journey, old-save migration and backup round-trip, process kill/relaunch, Persian RTL, screen reader labels and text scaling.
- Verify one reader session cannot earn more than its deterministic total across any batching; no fabricated gain by tapping or reopening world.
- Verify exact book locator on 30 repeated World -> Reader -> World transitions; no texture/animation left running in Reader.
- No full anime 3D, Godot embedding, long authored campaign, trading economy, new rank bypass, infinite scripted quests or spoiler AI in first slice.

## P1 / P2
P1: small island map with pier/tea room/gate, three genuine-character NPCs, reversible shop decoration, written original in-world story, first earned cosmetic. 
P2: backroom/subterranean archive, companion relationship arcs, full three mysteries, more intricate pixel lighting, authored branching resolution, optional ambient sounds when not conflicting with TTS.

## Editorial decisions to resolve with user
- Are the proposed names suitable? (none are locked)
- Which artifact was recovered on the beach? Allow player choice vs first item canonical?
- How supernatural should ink memory actually be? Quiet magical realism vs stronger cosmic mystery.
- Was elder's old castle relationship meaningful, or should it remain a mystery longer?
- Full city and playable episode only after proof of one-room story and reader safety.

**Status receipt:** Live Gemini response *actually received*, but this document is design research. No source implementation, tests, emulator QA, release or merge were performed in this consultation turn.
