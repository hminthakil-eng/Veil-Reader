# The Living Archive — Reader-first game research, architecture and delivery decision
Date: 2026-10-14
Status: **Research-informed proposal, NOT an implemented feature / not approved for merge**
Branch: `design/premium-reader-world-v1-20261014`
Parent: `RAGS_TO_RICHES_PRODUCT_ARCHITECTURE_V1_2026-10-14.md`
Reference PR: #518. Reader engine PR #509 and graphics/reading QA remain separate.

## 1. Product thesis and identity

Build **a real optional narrative/crafting/decoration game whose world develops through genuine reading**, not a second distracting task tracker. The reader is the hero product. The game is a consequence of reading, never its prerequisite.

**Fantasy:** an unknown traveler with a worn book and a small campsite restores the Lost Archive through study. Reading provides construction progress, insight and artistic choices. The traveler develops a personal identity and slowly turns a campsite into a hut, a scholar's house and ultimately a living castle. A coherent mystery unfolds through authored environmental storytelling (archive records, a hidden observatory, a letter, an old map); do not scrape copyrighted book lore into the game or generate spoilers.

**Distinctive promise:** users can actually wander through the spaces they built, place/rotate compatible furnishings, dress an expressive anime-inspired character, discover short nonmandatory story fragments, and then **return to the exact reading locator in one action**. Offline-first, no signup needed, accessibility-first, Persian/English and RTL.

Design anchors: Grayfog slate/navy, parchment, brass, low-key magical dust, honest wood/cloth/stone, sophisticated anime character design, not shiny generic mobile-RPG chrome. Animate only where interaction benefits.

## 2. What competitors teach us (evidence, not inferred rankings)

| Reference | Verified behavior / strength | Borrow | Avoid / opportunity for Veil |
| --- | --- | --- | --- |
| Finch | Goals produce pet energy, adventures and discoveries; quests, store, outfits and furniture (developer documentation) | Affectionate companion, tangible rewards, gradual world/story revelation | Do not require checklist taps, continuous shop visits or streak maintenance to keep the world alive |
| Habitica | Task-centered RPG quests, equipment, pets, parties/challenges (FAQ/API) | Concrete visual progress, optional quest structure and long-term collections | Do not copy task-as-currency farming or push guild/social pressure as a reading requirement |
| Bookly | Timed reading, book goals, speed/time projections, reports, achievements, readathons and character tracking (App Store product listing) | Useful *optional* reader statistics and realistic book/session evidence | Avoid showing timers, competition or deadlines over book content |
| ReadEra / Moon+ / Kindle / Apple Books | Reader experience baselines in Veil's existing benchmark policy; primary advantage to protect is continuity and usability | One-tap resume, effortless typography/controls, offline library | Never sacrifice Reader memory or gestures to world graphics |

User-reported concerns are not universal facts: readers on r/books report reading slump/anxiety from timed goals or statistics; r/finch includes anecdotal overload from streaks, claims and shop tasks. Design the game to work **without** daily pressure. This is a testable hypothesis; gather real Veil user evidence.

Psychology evidence: Li, Hew, Du (2024), 35 interventions / 2,500 participants, observed a statistically significant but **small average** intrinsic-motivation benefit of gamification (Hedges' g 0.257). Autonomy, meaningful progress and voluntary participation deserve more emphasis than achievement spam. This meta-analysis is educational, not direct evidence about Veil's readers.

Sources (checked 2026-10-14):
- https://help.finchcare.com/hc/en-us/articles/37780000231309-Exploring-the-Finch-Home-Page
- https://help.finchcare.com/hc/en-us/articles/42149821015693-New-User-Guide
- https://apps.apple.com/us/app/bookly-book-tracker/id1085047737
- https://habitica.vivs-tavern.art/static/faq
- https://link.springer.com/article/10.1007/s11423-023-10337-7
- https://www.reddit.com/r/books/comments/1fhkuem/reading_tracker_caused_my_reading_slump/
- https://www.reddit.com/r/books/comments/1hpxk75/do_people_enjoy_their_reading_statistics/
- https://www.reddit.com/r/finch/comments/1lcmnyy/i_am_done_with_this_app_now_i_am_not_here_to/

## 3. The actual game, designed as four concentric loops

**0 — Quiet reading (always first).** User resumes a real EPUB/PDF/manga book. Reader remains unmodified by game rendering or input. Game never covers book text, demands a check-in, or hijacks Back navigation. TTS evidence needs an explicit separate policy before it can credit progression.

**1 — Five-second delight (small loop).** After a valid reading session, queue exactly one **optional** status summary: progress toward restoration, any newly earned decorative item, or a brief story teaser. Show only after leaving Reader or explicitly opening World. May dismiss permanently.

**2 — Meaningful authorship (medium loop).** In World, choose one construction goal (restore the bookshelf / plant the lantern garden / equip an outfit). The player chooses the objective and places an earned item; no forced claim taps. No pay-to-win, gacha, upgrade timers, loss of items for inactivity, or PvP.

**3 — Narrative transformation (long loop).** Camp > Hut > House > Castle, with an evolving personal room and library. At each milestone the architecture actually changes, an authored mystery clue opens, and character clothing/furniture options broaden. Endgame is expressive decoration, characters, optional archival mysteries and collecting themes, not an escalating infinite grind.

**Player expression:** introduce three *cosmetic* thematic disciplines without giving more XP to any: Explorer (maps/astronomy), Scholar (archives/observatory) and Artisan (craft/workshop). Users can change direction; choices alter cosmetic discovery and dialogue, not their reading entitlements.

**Chapter-independent fiction:** in-world lore is original; never assume a book's contents or fetch/transform someone else's manuscript as game dialogue. Optionally provide a user-authored reading journal and private notes as decorations, with user-controlled disclosure.

## 4. Product flows

- New install: two choices, `Just Read` or `Read + Explore`; both use the same library and Reader.
- Reading Home: Continue Reading (dominant); Recent Books and Archive; small static World preview below. No live GPU scene.
- Reader: stable PAGED / PAPER CURL / SLIDE / SCROLL; clear exit and restoration to precisely the prior locator. No persistent quests, counters or 3D work.
- First return: optional unobtrusive `World changed` notice with skip option.
- World: tap decoration slot, view valid inventory, preview placement, explicit Save; navigate back to Reading Home with preserved book locator.
- Avatar: edit hair, eyes, expression, skin, clothes and accessories; compatible modular pieces; reversible and locally durable; graceful unavailable-asset fallback.
- Quiet Mode: never show progression reminders, achievement overlays, streaks, or game CTAs; world state continues to reconcile for later use.

## 5. Existing code and ownership: DO NOT build a parallel reading ledger

Verified from branch source / earlier Gemini peer review:
- `GameRepository` owns durable `minutesRead`, completed-book credit and quest/game counters; `LocalLibraryRepository` owns reading sessions and library.
- `deriveReadingHomeProgression` uses existing lifetime counters and bounded completed-book boost, with provisional thresholds 0/15/120/600.
- `RagsToRichesAvatar`, `PremiumAvatarCreatorScreen`, `PremiumWorldProgressionArt`, `WorldStageStore`, and `AvatarAppearanceStore` are prototype slices in #518.
- Cosmetics and `world.highestEarnedTier.v1` now reside in `veil_game_v1` included in existing library backup manifest v5; **restore round-trip not yet proven**.
- Previous Gemini architecture review identified split-brain reading evidence, GPU/Reader exclusivity, and the need for a coherent anime rig as P0 design hazards. Those *existing* findings informed this proposal; a fresh Gemini response was NOT obtained for this document (desktop remote connection was offline).

**Boundaries (one writer / one source of truth):**
```
Readium | PDF | Manga -> existing ReaderViewModel and LocalLibraryRepository
                                     |
                                     v
                          existing GameRepository
                          (durable cumulative evidence)
                                     |
                              read-only adapter
                                     |
                     pure ProgressionPolicy v1
                        /                 \
              stage projection      milestone candidate IDs
                        \                 /
                        GameUnlockJournal (future, versioned + idempotent)
                                     |
                   avatar/world inventory and presentation state
                                     |
           Compose 2.5D fallback OR exclusive optional 3D render host
```
**Compatibility with existing Alpha #314:** existing Path/Rank remains the **sole authority** for unlock of Castle rooms (Great Hall, Observatory, Sanctum, etc.). The reading-derived CAMP/HUT/HOUSE/CASTLE home is a cosmetic residence progression only, not a new way to enter rank-locked rooms. XP, streak, quest and decorative currencies must never bypass Rank authority. The home stage remains independent from these Path/Ritual requirements.\n\nNo world code imports Reader locator/navigation types. No duplicate `ReadingEvidenceWriter`. Never key an unlock to mutable `sessionId` alone. Use immutable milestone IDs, e.g. `world:tier:hut:v1` or `cosmetic:outfit:scholar:v1`; one durable reducer transaction for unlock + inventory. If a transaction fails, do not acknowledge reward. Never grant progress from Compose recomposition or world opening. A backup restore must preserve previously earned tier and already-claimed rewards.

**Compatibility note:** current `WorldStageStore` is a limited monotonic preference store, *not yet* a complete transactional award ledger. Do not claim the final economy exists. Existing stage value by itself can be read; no migration should lower it when rebalancing.

## 6. 2.5D vs 3D decision: separate prototype from shipped engine

Current 2.5D Compose Canvas is a low-risk, accessible, offline fallback. It is not a finished realtime anime 3D avatar.

Two official candidates:
- **Filament + glTF 2.0**: compact Android-centric scene rendering, physically based materials and glTF skin/morph animations, renderable into Android Surface. Very plausible for a premium rotating character viewer. But movement, camera, inventory interactions, animations and the world editor require custom game systems.
  Source: https://github.com/google/filament
- **Embedded Godot Android library**: native scene tree, animation controller, world/game interactions, UI scene tooling, Android AAR embedding. Plausible for an *explorable* game/world; integration, memory lifecycle, binary size, accessibility handoff and independent Reader reliability need proof. The official embedding docs warn that only **one Godot instance per Android process** is supported and automatic orientation/resizing events need explicit handling; these are hard spike checks.
  Sources: https://docs.godotengine.org/en/stable/tutorials/platform/android/android_library.html and https://docs.godotengine.org/en/stable/tutorials/platform/android/embedding_in_android_projects.html

**Provisional direction:** run two identical measured spikes *outside production Reader*, then pick ONE engine. For a truly interactive anime town/castle, prioritize trying embedded Godot first. If excessive install footprint / lifecycle / frame cost, fall back to Filament plus constrained room interactions. Keep native Compose for Home, Library, Reader, accessibility and settings. Do not ship two heavy renderers simply because both look impressive.

The spike must use the **same actual sample**: 1 humanoid canonical rig, 2 swappable garments, 2 hairstyles, 3 facial expressions, 1 material-switching toon shader, idle/reading/walk/celebrate animations, and a miniature room with 6 furniture items. A spinning cube comparison is invalid.

**Hard gate:** teardown of all game GPU resources before Reader acquires publication/Paper surfaces; verify reader return after rotation, background and repeated transitions. If release fails: show static fallback and do NOT open Reader until graphics ownership is safe. No unverified promise of instant release.

Measurements on lower-, middle-, higher-end real Android: installed APK growth, cold scene load, p50/p95/p99 frametime at 60/120Hz, RAM/GPU footprint, thermal/battery proxy, 30 World->Reader->World cycles, missing assets, 200% font, screen reader, reduced motion and English/Persian RTL. Set budgets from current baseline and real measured samples; proposed targets are not verified outcomes.

## 7. Content / asset production pipeline

1. Art bible and silhouette board approved for Traveler, Hut, House, Castle; one visual identity across stages.
2. Rig bible: stable skeleton naming, scale, body proportions, blendshape names, material slot IDs, hair/hat compatibility, modest clipping envelope, LOD levels and shader variants. Facial expressive states must preserve identity across animations.
3. Blender-built canonical character with source-controlled provenance, exporting tested glTF 2.0. Evaluate VRM only as an interchange extension after compatibility checks.
4. Shader/animation proof in both engine spikes; choose from observed anime readability, texture cohesion, asset load, frame cost, and handoff reliability.
5. Signed/versioned asset manifest maps IDs to base-game fallback. Unknown or unavailable paid/downloadable pack IDs are not destroyed by restore; show safe defaults while preserving identities.
6. World layout stores placement IDs, transforms and inventory choices, not screenshots or engine node serialization.
7. Assets cannot depend on a cloud generation API at read time. Local app must function without internet.

## 8. Measurable game UX and ethics gates

- Core: time-to-resume-book, locator accuracy after World navigation, Reader p95/p99 vs unchanged baseline, crash-free reading sessions.
- Opt-in: proportion who choose the game and later switch to Quiet Mode, time spent reading vs time trapped in reward screens, negative pressure/annoyance survey, replay desire and customizations saved.
- Correctness: exactly-once milestone credit under background/pause/reopen/restore, no stage demotion, no arbitrary XP from page tap loops, irreversible save or duplicate rewards.
- Accessibility: one-handed, 48dp semantic controls, screen reader description of world instead of unlabeled Canvas, input focus restoration, RTL layout, larger text, motion/graphics reduction.
- Quality: show four dramatically distinct scenes, readable at small screen size, tested hair/clothing without clipping; written and visual acceptance must match the shipped APK.

## 9. Execution gates (evidence before scope expansion)

**Gate A — Reader protection, in parallel.** Keep #509 and game #518 independent; fix confirmed native-paper defects, run full tests/device acceptance, export old library, verify restore with current version. Game is never released to mask broken Paper Curl.

**Gate B — Tiny truthful game loop.** Validate current store migration with actual `LibraryExport` round-trip; add monotonic durable milestone/reward contract with backup migration and unit tests. Implement first optional post-reading CAMP reward using real counters, no extra recorder.

**Gate C — Player-controlled World MVP.** Static/deterministic Compose scene: 1 traveler, 1 hut upgrade, 6 placed items, 2 outfits, obvious editing flow, original narrative discovery. Real-book session -> one unlock -> save/restart -> reader return.

**Gate D — 3D decision lab.** Godot vs Filament equal-content A/B spike with trace and physical screenshots. Adopt exactly one if it meets measured budgets, or remain polished 2.5D.

**Gate E — Full signature experience.** CAMP/HUT/HOUSE/CASTLE; anime rig + modular garment/hair/expression selection, original lore, furniture and workshops, optional short quests, higher-quality animations, localized and accessible world. No claims of “premium” without competitive visual QA and repeatable source-to-device receipts.

**Gate F — private alpha only.** Test real books and reader gestures on real devices; age-appropriate content rating, licensing, crash/ANR/energy measurements, privacy backup restore, tester feedback; do not merge or roll out unless all P0 gates pass.

## 10. Gemini co-development brief — NEXT LIVE REVIEW REQUIRED

**Connectivity record (2026-10-14):** prior Gemini architecture response is incorporated in the original architecture document. The connected Windows desktop is currently OFFLINE to remote tools, so no new Gemini consultation was executed for this research pass. Do not label this memo as Gemini-approved. Request a fresh independent review when Gemini becomes reachable.

Give Gemini this memo, original `RAGS_TO_RICHES_PRODUCT_ARCHITECTURE_V1_2026-10-14.md`, and exact latest #518 source. Require a code-specific critique, not praise:

1. List top 8 technical/product failure modes, severity, exact source path and minimal reversible fixes.
2. Propose an original 10-minute player experience through 3 reading sessions, the first cosmetic choice, a visible World improvement and instant reader resume; identify every interruption to avoid.
3. Red-team `GameRepository` + `WorldStageStore` + `AvatarAppearanceStore` + `LibraryExport` for race, restore, exactly-once grant, type migration and process death. Demonstrate tests that SHOULD fail on the present prototype.
4. Build A/B implementation outlines for the same anime rig, two outfits and a room in Godot embedded vs Filament; provide measurable memory/GPU lifecycle/release plan. No fictional speed or test claims.
5. Design minimal GameUnlockJournal record model with a deterministic milestone ID, committed atomic transaction, rollback and migration. Do not change Reader data ownership.
6. Return an explicit `APPROVE / REVISE / BLOCK` verdict for each Gate B/C/D, and one small compile-ready code patch for the highest-confidence gate after critique.
7. Show how to support static low-power fallback, RTL, screen readers and returning to a real book at its exact locator. Prohibit any forced check-ins, punishment loops, spoiler-generating AI or lootboxes.

Review rule: ChatGPT and Gemini are peers, but neither merges directly; each patch goes into isolated reviewable branches, passes same-source tests, and gets a real device evidence receipt. Counterproposal required if it conflicts with Reader/backup ownership.
