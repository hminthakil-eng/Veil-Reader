# Veil Reader — Island Bookshop Indie RPG + Shop Management Research and Engineering Plan
**2026-10-14 | Research, not an implementation or a merge authorization**
Scope: user requests **deep management gameplay + full breadth of a high-quality indie RPG** built on an existing offline-first Android reader, shipwreck/beach amnesia prologue, independent elderly bookbinder's shop and upstairs home; anime UI portraits, matching character pixel sprite and beautiful living pixel island.
Other relevant docs: `ISLAND_BOOKSHOP_GEMINI_NARRATIVE_REVIEW_2026-10-14.md`; `ANIME_AVATAR_PIXEL_CITY_DIRECTION_2026-10-14.md`. Draft PR #518. Game #314; native Paper PR #509 independent.

## Primary sources: facts vs proposals
| Game/source | What is documented by publisher/developer | Translating to Veil, NOT a claim of implementation |
| --- | --- | --- |
| **Tiny Bookshop** — https://tinybookshopgame.com/ ; https://www.playstation.com/en-us/games/tiny-bookshop/ | Stock different book genres, make personal recommendations, decorate, know townsfolk, manage seaside shop | Our **closest direct competitor**; Veil must exceed a passive shop flow by making inventory decisions, recovery and expedition-driven sourcing, with meaningful mystery and NPC consequences |
| **Bookshop Simulator** — https://store.steampowered.com/app/3467040/Bookshop_Simulator/ | Order, stock, operate register, earn, expand, eventually hire staff | Tangible management depth, but do not force repetitive shelf-stocking every play session |
| **Recettear** — https://store.steampowered.com/app/70400/Recettear_An_Item_Shops_Tale/ | Price, stock, shop layout, adventure/dungeons, multiple playable helpers | Two complementary play activities: running the shop and seeking rare inventory; substitute lore/port expeditions and restoration for combat-heavy dungeon grind |
| **Moonlighter 2** — https://digitalsungames.com/games/moonlighter-2/ | Merchant-adventurer dual-life, loot, store and community | Shop purchasing power should support exploration and world restoration, and discoveries should refill the bookshop |
| **Stardew Valley** — https://www.stardewvalley.net/ | Skills, crafting, neighbors, exploration, housing and customization | Voluntary diverse activities, believable town; avoid copying crops, combat/relationship bars simply to claim feature parity |
| **Fields of Mistria** — https://www.fieldsofmistria.com/ | Pixel art, customizable character, seasons, 30+ villagers, relationships, magic and town renewal | Expressive anime portraits + pixel NPCs; authored schedules/relationships, town projects and coherent art; no 30-villager promise before content budget |
| **Potionomics** — https://www.gamedeveloper.com/business/how-potionomics-turned-price-haggling-into-a-card-game | Selling potions through a card-based negotiation loop | Make **gentle book consultation** and trust worthwhile without burdensome card-battle haggling for every sale |
| **Travellers Rest** — https://travellers-rest.itch.io/travellers-rest | Tavern staffing/social life, crafting and broad management | Production stations, recognizable NPC customers, hospitality, opt-in decor and later assistants; avoid kitchen/labor chores as a second job |

Community risk indicators, **anecdotal not prevalence**:
- https://www.reddit.com/r/CozyGamers/comments/1neahbu — some users overwhelmed by optimal furniture/shop layout; support curated auto-layout and a small intuitive layout.
- https://www.reddit.com/r/CozyGamers/comments/1rnjdo3/ — some players want pricing/research/customer management MORE than decoration; provide both distinct control modes rather than forcing furnishing.
- https://www.reddit.com/r/CozyGamers/comments/1bgi4hk/ — bugs, controls and shallow shop simulation are discussed as retention risks.
- https://www.reddit.com/r/CozyGamers/comments/1vhcwkj/ — genre fan recommendations differ; not proof of commercial success.

## The 4-level game loop: decisions, not fake interactions
1. **Minutes: shop decisions.** The user selects 3 shelf mixes; two in-world customers arrive with actually different needs, budgets and taste tags. Select/suggest a book, fair price or free community loan, and see response.
2. **Session: sourcing/repair.** Spend fictional currency to order crates from pier trader, restore a waterlogged in-world manuscript, classify rarity/condition, and choose sell / loan / archive.
3. **Week in game: relationships and improvements.** Talk to the elder, visit pier/tea room, choose among 2 shop reconstruction projects. Loyalty, accessibility/community trust and stock affect incoming visitors; no real-life calendar deadlines.
4. **Long term: island mysteries + social RPG.** Recover authored clues and maps from salvaged books, learn the elder's past, transform the bookshop and eventually the town/castle surroundings. A user who dislikes shop management may use assisted-store policy after the teaching session; primary Reader requires no gameplay at all.

The player must be able to **earn in-game shop money by playing the RPG alone**. Real-life reading **may optionally grant capped cosmetics/restoration boost**, but never gates the storyline, forces reading sessions or incentivizes fake page swipes. Existing `GameRepository` counters are the sole reading input, and the game is optional.

### Proposed economy (example numbers only; not balance-tested)
- Initial shop: 6 used books (2 story, 2 travel, 1 botany, 1 nautical); 2 shelf slots plus one small display; **30** fictional coins; one repair desk; elderly owner handles restock when inactive in game.
- Port crate order costs 12 coins and delivers 4 books on next **in-game** shop day. No real-world wait or shipping push alert.
- Customer A requests an illustrated hopeful journey within a modest budget; matching stock earns 11 coins and trust +1. Customer B seeks marine charts and may want a 3-day **in-game** loan; lending consumes availability until return, but gives town reputation and possibly an authored note. Prices 7–18 example coins, limited by budget and condition. Wrong suggestion is recoverable: customer can decline and return later, not permanently shame player.
- Decor upgrade 25–40 coins vs repairing leaky window ~20 coins. The choice changes ambiance, visitor mix and available display spaces. If poor, player has a baseline salvage/favor activity to recover; no irreversible debt/failure state.
- Value derives from `tomeTemplateId`, genre/mood tags, condition, rarity and customer preferences. Do not serialize or index private EPUB text. Shop items are **fictional game books** with authored abstract attributes.
- A book has three credible fates: **sell** (cash), **lend** (community goodwill + delayed return), or **archive** (story discovery/memory clue). Interesting tension without deliberately punishing helping people.
- Set price in 3 understandable presets (accessible, fair, collector) with optional advanced adjustment, not repetitive micro-negotiation on every item.
- Make in-game time **action-advanced**, pausable, no real-world clocks or daily negative decay. Offline for a month never worsens elder's health or destroys stock. `Quiet Mode` hides all rewards and RPG CTAs from reading UI.

## Mechanics inventory and staged priorities
In rows below **M** = first minimal truly playable shop day, **P1** = complete indie-RPG foundation, **P2** = enrichment. Each system has a real player action and independent saved state.

| Domain/system | Meaningful action | Persisted authority | Priority |
|---|---|---|---|
| In-world inventory | Drag a named fiction book to a shelf or hold stock | `InventoryStack`, instance condition | M |
| Book condition/rarity | Compare damaged/rebound/collector copy | item-instance catalog | M |
| Book customers/needs | Choose recommendation from actual fictional stock, see consequence | reputation + dialogue flag | M |
| Sales and pricing | Set fair/value/collector price; complete transaction | wallet+stock atomic ledger | M |
| Supply & restock | Order port crate vs recover salvaged manuscript | procurement order + acquired source | M |
| Day shift | Choose shop open/close; 2 customers; summary | virtual day+shift+sales events | M |
| Crafting/repair | Restore single water-damaged in-world book with material and gesture | inventory transaction + repaired state | M |
| Shop restoration | Choose between repaired window and expanded shelf | project unlocked/paid | M |
| Two named NPCs | Talk to elder and recurring customer with short expressive dialogue | dialogue flags, relationship | M |
| Shop layout | Place compatible furniture on valid slots; no blocking door | furniture placement | M; P1 free grid |
| Player walk & interact | Move sprite, use trigger, inspect desk/door | scene, x/y, interaction flag | M |
| Story & quests | Follow authored prologue; optional branching hint | stage, decision flags, quest log | M |
| Avatar identity | Cosmetic edit, anime portrait matches pixel sprite | existing `AvatarAppearanceStore` only | M |
| Reader integration | Optional book-open, seamless exact locator return | existing Reader owner | M |
| Port exploration | Walk boardwalk, collect salvage choice, meet lighthouse keeper | map location, loot | P1 |
| Loans & returns | Choose lend vs sell; see item return in-game days | lending ledger | P1 |
| Customer schedules | Different named people on certain in-game shifts | schedule config + day seed | P1 |
| NPC affinity | Remember book recommendation and meaningful promise | relationship event flag | P1 |
| Skill tree | Cataloging, Restoration, Community Liaison, Exploration | skill XP/tier (game only) | P1 |
| Map & navigation | 3 locations and travel/interact, rain/weather scenery | scene plus unlock flags | P1 |
| Vendor/supplier quality | Prefer rare, cheap or ethical regional supply | supplier trust | P1 |
| Town reputation | Return on community books even at low margin | shop stat | P1 |
| Furniture upgrades | Rotate/place, validate path to register | shop grid, occupancy | P1 |
| Optional companion | Cat/crow that finds items, story companionship | companion affinity, state | P1 |
| Seasonal events | Small original book festival, weather differences | in-game virtual calendar | P2 |
| Town project | Expand pier lights/library archive with town help | project progress | P2 |
| Deep NPC story | Personal quests, branching without irreversible shame | story flags | P2 |
| Expeditions & mystery | Recover lost archive docs; puzzle, no forced combat | clue/region/interaction states | P2 |
| Labor assistants | Hire NPC to restock/recommend, lightweight automation | staff roles and policy | P2 |
| Home/wardrobe | Fully customize upper loft with reused anime assets | layout/decor references | P2 |
| Rich content tools | Authoring and localization validated at build time | versioned JSON packs | P2 |
| Fishing/foraging/cooking | Only if directly serves bookbinding supplies, tea or island relations | recipes/resources | P2 optional |
| Combat | **NOT required**; avoid generic fights unless narrative research validates | none in first milestones | CUT |
| Romance & marriage | avoid initial feature inflation; relationship writing first | none in first milestones | CUT |
| Open-ended procedural world | authored small island first; don't promise infinity | none | CUT |

## Data architecture and integrity
**Reader and game are separate bounded contexts.**
```
EPUB/PDF/TTS -> existing Reader/LocalLibraryRepository/GameRepository (read only to new game)
                                              |
                               EligibleReadingCosmeticProjection
                                              |
    Island RPG separate additive database (Room, versioned)
        InWorldInventory | ShopMoneyLedger | CustomerSchedule | ShopDay
        InventoryTransactions | LoanLedger | NPCRelationship | QuestFlags
        StoryState | DecorationPlacement | NarrativeEventLog
                                              |
                             StateFlow read models
                                              |
                      Pixel game scene + Compose Android UI
```

Business invariant examples:
- A sale of 1 book and credit of 1 sum are **one atomic Room transaction**. No double sale on gesture, event replays, or process kill.
- Loans store unique item instance IDs; cannot sell or sell-again a borrowed copy. Late NPC returns are story flavor, NOT real-time fines or punitive customer shaming.
- Purchases never allow a negative balance; deterministic, seeded customer generation is based on in-game day+shift+shop config so save/reload does not reroll favorable RNG forever.
- NPC conversations have stable `dialogueNodeId`, only authored localized strings, and guarded reversible branch flags; no dynamic access to private e-book contents.
- Persist only **island-specific** state in new store. **Never copy and overwrite** Rank, Path, existing castle room unlocks, active reading minutes, saved book locator or avatar cosmetics.
- Versioned `gameSchema`, additive migration, stable asset IDs, robust corruption defaults, backup-export/restore coverage; if absent, world does not appear, but Reader opens.
- Reading reward high-water must be cumulative and transactional. Reject per-update `max(1, floor(deltaMinutes/5))`; it over-credits many 1-minute updates and drops fractional residue. Source code for TTS accounting must be reviewed before adding speech-listening credits.

## Rendering architecture decision gates
**Scope-limited Compose Canvas**: fast 1-room proof because native UI, accessibility and Reader already Compose; use pixel-perfect virtual resolution, nearest-neighbor, static rendered tile atlas, layered animated NPC sprites only when World open. But hand-writing 2D collision, NPC A*, camera, tilemap scene/crossfades, animation time, editor & event scripting for large city **will become an entire engine project**.

**Godot 4 embedded Android AAR**: official stable docs support AAR library embedding; its tile layers, nav server, animation player, collision and scene editor are strong fit for 3 neighborhoods, moving NPCs and RPG quests. BUT single-instance Android process, orientation handling, memory and GPU teardown must be measured; reading-only app must not initialize 3D/2D gameplay surfaces at startup.
- https://docs.godotengine.org/en/stable/tutorials/platform/android/android_library.html
- https://docs.godotengine.org/en/stable/tutorials/navigation/navigation_introduction_2d.html
- https://docs.godotengine.org/en/stable/tutorials/2d/using_tilemaps.html

**libGDX**: first-class Android/Java-based tiles, batching and actor input, less Android<->engine foreign-lifecycle friction; tool/editor and gameplay controller structures require build work, unlike Godot.
- https://libgdx.com/wiki/graphics/2d/tile-maps
- https://libgdx.com/wiki/graphics/2d/scene2d/scene2d

**Decision**: prepare **equivalent small playable sample in two candidates**, Compose 2D vs Godot Android library (add libGDX only if evidence of a bridge issue). Both draw the SAME Tiled map, run 1 player + 2 NPC with simple path and interaction, two customer purchases from fixed stock, save/restore, and 30 forced World->Reader->World transitions. Profile actual device low/mid/high; measure cold start, APK size delta, p95 frames, idle power proxy, memory after disposal, 2D texture retention, RTL focus and TalkBack. Select engine on evidence, never from opinion or feature checklist alone.

Tiled editor https://www.mapeditor.org/ exports TMX/JSON and offers object layers. Alternate LDtk https://ldtk.io/ has versioned JSON. Use *one canonical editor* and a build-time converted tilemap intermediate, not handwritten coordinates in Kotlin.

Art design pipeline: character bible -> approved portrait layers + matching pixel sprite variations -> 16/32 pixel authored tileset and 32x48 walk sprites -> 4/8 directional anim frames -> tilemap layers/collision/triggers -> stable catalog ID and variants -> atlas texture build -> NPC schedule and branching dialogue JSON schema + Persian/English l10n QA -> real-device snapshots. **No copyright/IP borrowing or raw assets from games.** Evaluate vendor licenses BEFORE any download or redistribution; use original artwork or correctly licensed assets and keep evidence in `assets/LICENSES.md`. CC0 prototypes: https://kenney.nl/assets

## Delivery with quality gates
**Slice 0** — playable 8–12 minute shipwreck/beach and loft & shop opening: 1 pixel town scene/loft, anime mirror customization, elder dialogue and book repair, one mystery hook. No stock economy until state model proven.

**Slice 1** — **the first real game**: complete 5–10 minute in-game shop day with **6 fiction books, 2 distinct customers, 1 shelf mix, 1 sale vs 1 community loan, 1 port order, 1 restoration choice, 1 elder story flag, saved session and recovery**. Playable without reading and with no internet. Shop finances reconcile on repeated taps and app process death.

**Slice 2** — 3 neighborhood walk with consistent world, 3 NPC schedules, evolving relationships, 1 companion, one tiny exploration mystery and authored quest. New island maps and story must not require a giant multi-branch writing backlog.

**P0 acceptance**: all game actions can stop immediately and resume book at exact real locator; no crash after process kill and restore; no double currency/stock; no duplicate reader counters; 0 game animation/scene loop while Reader active; save-state and backup round-trip; 30 handoffs; RTL/English/200% fonts/screen reader/hardware back/reduced-motion; core gameplay fun in a voluntary 5–10 minute session on actual Android; do not label this shipped until receipts.

**Current status:** Independent web research and roadmap only. Gemini Argon separately consulted; live-web access in that Gemini desktop session was self-reported unavailable, so web source verification was performed by ChatGPT and should be reconciled against Gemini's final source proposals separately. No APK, build, benchmark or working management engine claimed.

## Gemini 4 Argon consultation — provisional design review (live desktop, 2026-10-14)
This supplement records an **actually submitted, partially received** Gemini reply, not an invented endorsement and not a full tested design. Local UI text captured to `E:\EyadStudio\Temp\Gemini_Indie_RPG_Bookshop_Research_Partial_20261014.txt` on desktop. Gemini explicitly stated its live Google Search tools were inaccessible in that session. Primary-source verification above was done separately through ChatGPT web search.

**Independent ideas observed so far:**
- 24 gameplay systems prioritized around real **shop inventory, shelf genres, port orders and salvage, restoration workbench, controlled store layout, pricing + secondhand buyback, lending ledger, authorable consultation preferences, weather and two-shift customer flow, small shop balance/upgrade**, with RPG town/NPC/skills to follow. These are proposals and intended system designs, not implemented source.
- Shop book instance fields: `tomeTemplateId`, rarity, condition, genres/mood tags, procurement source; drag to shelf and optional automatic inventory filtering. A newly arrived book cannot immediately be flipped repeatedly at an exploitative markup; catalog or repair first.
- Customers split into ambient browsing customers and **deliberate seeker customers**, the latter ask narrative mood-based requests matched to fictional tag-bearing inventory.
- Loans as distinct transactions with physical copy unavailability until return, with occasional original authored clues in returned books.
- Schedules/weather are **in-game**, not punitive or real-life timed; supplier stock depends on finite daily slots, and storefront decisions can be made in 5-10-minute mobile sessions.
- Gemini's **current favorite** production route is a Kotlin grid/ECS + Compose DrawScope world and native Compose overlays, with libGDX as fallback; it cautions about Godot AAR binary/lifecycle cost. Its own suggested test uses identical map+NPC scenes in Compose versus Godot/libGDX. **Our engineering verdict remains TEST FIRST**: for a *full indie RPG* with scripted quests, schedules, collision, multiple maps and a content pipeline, writing a new engine may cost more than integrating an existing mature 2D engine.
- LDtk or Tiled and bilingual structured NPC/quest/dialogue data, atlas layers and build-time asset validation rather than manual scene coding.
- Gemini argues **no mandatory combat**, replacing fights with fog/lamp navigation, tidal puzzles and interaction with shy island wildlife. This fits cozy narrative, but is a **proposal** subject to the user's taste.
- It suggested technical budgets such as a fixed 65 MB overhead, <100ms resume and <1% janky frames. These are aspirational invented targets, **NOT verified measured device outcomes**; establish an actual baseline and feasibility before locking numbers.

**Corrections/risks:** 
1. Do not use existing `RagsToRichesStage` as a shop floor-area authority if doing so bypasses castle Rank/Path or retroactively demotes earlier user saves. Stage is cosmetic/home visual high-water, and castle rooms remain rank-locked.
2. No duplication of authoritative `GameRepository` Rank, Avatar, or Readium locator into a new narrative save.
3. Reading-based upgrades to rarity or currency require idempotent cumulative progression; avoid repeat farming by reopening screens. No real-book content or annotations may be inserted into fictional item demand without specific opt-in.
4. Claims that commercial sprite vendors' assets can be redistributed in public source after encrypting packaged sprites are not legally established. **Check each individual license** and avoid uploading proprietary sprites/raw files to public repos without rights. Kenney CC0 and original in-house artwork are safer for open-source prototype.
5. Per-frame tile rendering in Compose vs Godot/library must be benchmarked on real devices and proven to pause in Reader, not accepted by theoretical O(1) collision arguments.
6. The first live Gemini response is unusually long and still generating when this note was captured. Review/replace the supplement once a terminal response exists; do not falsely mark the Gemini web task or code implementation complete.

**Closest market gap refined:** *Tiny Bookshop* already has seaside book recommendation + town story; *Bookshop Simulator* already has strong shelving, staffing and commercial expansion. Veil must differentiate through original mystery narrative + meaningful book sourcing/repair/loan choices + real-time walkable pixel island and deep NPC bonds **without turning the primary e-reader into a mandatory farming loop**.
