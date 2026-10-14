# LIVING ISLAND — Economic Recovery Game Design Bible v0.1

**Working world design / not approved canon.** The ruined economy is a major multi-act campaign, with recovery delivered through play, visible pixel scenes, distinct NPCs and a bookshop-related mystery.

## What went wrong?
Three years before the shipwreck, a storm destroyed a critical pier section. The town's financial insurers stopped honoring shipments after the harbor manifest was altered; the port's former administration passed debts to island workshops, a powerful supplier gained monopolistic credit leverage, and income/people slowly left. Official records disagree with binding scraps preserved by the elder. Some NPCs defended the scheme, others concealed evidence in an attempt to protect their families. None are reduced to caricatures, and economics cannot be fixed simply by discovering one secret letter.

How the hero helps without usurping citizens' agency: the bookshop restores vital documents, connects sellers and buyers, provides a modest social meeting venue, invests its *own* honest profits, negotiates supply chains and chooses whether to support credit cooperatives or private merchants. Resident-owned enterprises **operate on their own**, can refuse contracts, form alternatives and progress while player focuses on their private bookshop, relationships or exploration.

## Eight NPC-owned economic institutions — PROPOSED
| Business | NPC owner/team | Dependency | Distinct management & quest loop | Visible town change |
| --- | --- | --- | --- | --- |
| Textile/binding cloth | Arghavan's weavers | port fiber, dye, labor | custom contracts, fair wages, inventory textile shortages, late-game patterned covers | looms audible; cloth awnings return |
| Bakery and café supply | Nozhat's family bakery | grain, fuel, transport | barter bread for repair services, festival demand, creditor fraud | warm storefront/bread queues |
| Carpentry and construction | Roya's workshop | wood, tools, skilled labor | repair shop shelving, choose durable bridge materials, NPC crew availability | scaffolds become rebuilt bridges |
| Fisher guild and smoked-fish co-op | Mahin's neighbors (leader TBD) | tides, boats, market ice | cooperative risk-sharing, export/repair boats vs local affordable food | working pier, carts, boat sounds |
| Printshop and town notices | Younes' press | paper, ink, public permission | source authenticity, book trade, printed council notices, controversial records | printed posters, press sounds |
| Teahouse and rooms | Parisa | bakery trade, fuel, town traffic | guest events, reputation, clue witnesses, ethical rumor restraint | live conversation crowd |
| Harbor courier/warehouse | Nima's dispatch team | pier reliability, roads, credit | parcel sorting and economic route puzzles, express vs community delivery | messenger bikes, docks reopening |
| Clinic and herbarium | Simin and colleagues | supplied medicines/records | private medical records ethics, archive research/repair, public health outreach without medicine shortage guilt timers | occupied safe clinic lights |

All independent professions must have economic stories even if protagonist does NOT romance them. Some cooperatives and shop owners can challenge protagonist's development choices. When not enough import trade is available, story beats and alternate production options matter, not pure randomized money penalties.

## Diverse decisions (examples)
- **Buy cheap imported cloth versus Arghavan's local fabric:** profit margin vs stable local employment / distinctive cover customizations; customers see quality.
- **Grant private creditor a monopoly at the new port versus democratic co-op:** one player loan potentially rescues today but affects who sets tomorrow's prices; no simplistic pure good choice.
- **Restore bridge for tourists versus repair fisher cold storage:** both worthy, with different NPC shifts, food prices and routes. Choosing one does not freeze the other forever.
- **Store large rare-book profits versus endow low-cost lending library:** future town literacy, youth apprentice routines, working capital tradeoff; reading never mandatory.
- **Raise book prices versus supply affordable books for workshops:** reputation and buying power, separate from generic morality XP.
- **Refuse to fund a street project:** guild NPCs collectively finance a modest version, with their own appearance/benefits and a line of humor; player is not the only agent.

## Distinct loops by act
**Act I:** run-down store; book condition and personal customer recommendation → first export buyer order → local fabric contract → fund visible street lamps. System: supply trust and one project, one real NPC.
**Act II:** contract management, barter, apprenticeships and two business owners' independent goals. System: first purchase/supplier quality and customer access.
**Act III:** co-op/credit market, small public project board, city shop signs and a local votes; no huge spreadsheets. System: named civic proposals with 2-3 feasible routes and price consequences.
**Act IV:** port investigation, seasonal external buyers, capped trade routes, improved transport and shipping contracts; optional exploration. System: an explicit import/export supply economy that cannot mint arbitrary currency.
**Act V:** archive governance, festival trade/craft fair, restoration of school and neighborhoods, mixed public and private enterprises; visually spectacular districts. System: cumulative world projects with different supported endings and district signatures.

## How city recovery looks and sounds
Before: locked shutters, salt-rusted street lamps, empty carts, burned dock signs, patched clothing, lantern light only in elder's bookstore.
After one named project: two hand-sewn lantern shades, playable light pools, neighbors greet/work near the street and one special evening NPC event.
After local contracts: a different NPC at the loom each morning, more textile deliveries, repaired shop sign.
After credit co-op: morning market stalls, workshops with goods displayed, courier route takes new turn.
After pier reopening: tide-responsive boat animations, wharf voices, steam/water/carts, ships with distinct origins; waterfront architecture gets repairs.
After archival renaissance: open library, neighborhood book stall, school evening story circle, bright flags, preserved darker alleys; not every district must look identical or be fully luxury-renovated.

## Financial engineering and honest presentation
The demo `core/economy_ledger.gd` is deliberately **one small independently tested financial contract**, not a full city economy. Export buyer budget 50, local actor currencies integer, every ledger transfer audited, one unrepeatable civic street project. Temporary duplicate cash authorities mean **NEVER** bind it to real ShopLedger coins before a single-authority adapter. Actor production, inventory, labor, external currency supply replenishment, taxation, bank credit, employment effects and town microquests remain designs.
Three **player-facing** town signals: observed market turnover, number of autonomous contributors, and realized civic works. NOT fiction-economy health bars that jump for arbitrary narrative flags. Future data can add jobs/access/stock underneath if the game actually benefits.

## Lessons from shipped games (principles, not copying protected content)
- [My Time at Sandrock](https://blog.playstation.com/?p=378461): credible civic scarcity and townspeople-led rebuilding.
- [Littlewood](https://store.steampowered.com/app/894940/Littlewood/): visual town recovery and relationships.
- [Against the Storm](https://wiki.hoodedhorse.com/Against_the_Storm/Traderoutes): concrete source/consumer trade routes and bounded trade choices.
- [Travellers Rest](https://store.steampowered.com/app/1139980/): business operations, craft and customer service.
- [Stardew Valley](https://www.stardewvalley.net/): community restoration goals, strong social loop.
Mechanics/genre principles transferable; our story, characters, script, art and assets must be original or appropriately licensed.

## Game QA gates
1. **Money accounting**: no negative wallets, no double payments or duplicate projects, JSON version checks, 88-coin initial four-wallet proof.
2. **Narrative legibility**: test player must understand why one repair benefits a named NPC/shop without looking at five economic charts.
3. **Non-obligatory philanthropy**: player who never donates can still see community-led repairs; main quest reachable.
4. **Three-day variety**: new goals/decisions instead of copy/paste fetch tasks or +10 gold barks.
5. **Voluntary real reading**: player who never reads real EPUB can access basic civic works and all main mystery; actual book content/URI stays in Reader.
6. **Cross-ledger decision required** before any integrated wallet state; process-death atomic save and test on Android later.
