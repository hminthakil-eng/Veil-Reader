# Island Bookshop — Adult Romance, Family and Character Agency Bible (v0.1)

**2026-10-14 | ORIGINAL creative draft | Companion to ADR-002 | Not implemented, not approved as final canon.**
Origin: User explicitly wants complex optional romance routes for the protagonist with adult women, including married women and mothers. Gemini 4 Argon independently proposed relationship states, five adult archetypes and original Persian scenes in a live review. ChatGPT rewrote/expanded those ideas to align with **the user's wish for potentially available routes for all named adult female NPCs**, while protecting believable relationships and agency. No source game text, artwork or proprietary character copied.

## Principle and creative scope
The game explores romances of adult characters aged 18+, not collectible characters. There is a **potential romance story** for each authored adult female route, including those already married or raising children. The player can choose which character to pursue, but consent, values and existing commitments determine **whether it is possible in a particular save**. No expensive item, charisma XP or manipulation guarantees success. Some endings are loving partnership; others are a moving refusal, mature friendship, one-sided affection, a mutually agreed non-monogamous relationship, a self-initiated separation, or a costly secret affair. What changes is character history, not a generic heart-meter.

**Never romance any minor.** Bamdad, age 14, has only nonromantic quests/mentoring. Children of adult NPCs are autonomous participants in daily life, never a romance unlock or leverage. A parent is a protagonist of her own life. No magical 'marriage-busting skill'. If an adult declines interest at one point, repeated advances cause conflict rather than points. A later potential relationship requires a genuine independently motivated change in the adult's circumstances and renewed mutually explicit interest.

## Relationship state — RPG architecture proposal, not production code
Stable `npc_id`; `adult_age_verified`; `authored_romance_route`; `initial_partner_status`; `current_partner_status`; `npc_personal_goal_flags`; `consent_and_boundaries`; `trust`; `shared_values`; `mutual_interest`; `conflict_log`; `social_repercussions`; `family_commitments`; `ongoing_quest_flags`; `player_romantic_choice`; `route_state`.

States:
`NOT_MET` → `COLLEAGUES` / `FRIENDS` → `POTENTIAL_INTEREST` → `CONSENT_CONVERSATION` → `RELATIONSHIP_PATH` (public partnership / negotiated poly / independently separated & dating / secret with consequences) → `RESOLUTION`; alternative branches `PAUSED`, `FRIENDS_ONLY_THIS_RUN`, `CONFLICT`, `PERMANENTLY_CLOSED_THIS_RUN`.

Romance route availability derives from authored flags and mutual interest, not gender/parenthood/marriage alone. Trust and desire are distinct. Choices to stop flirting never close nonromantic job/skill quest lines. Spouses have voice and independent agency; explicit non-monogamy needs consent from **everyone affected**. Player may choose cheating storyline only as adults choose with real costs and possible rejection, never presented as a free optimization. If separation occurs, NPC decides independently for reasons that exist even without the protagonist. The game must not punish honest readers for not romancing anybody.

## Seven candidate adult romance routes (ages/status proposed, exact names unapproved)

### 1. Arghavan (39), textile guild owner, married mother of two — exceptionally hard, long-burn
**Life without hero:** protects employees, negotiates maritime import duties, raises children aged 9 and 15, has a complicated relationship with sailor spouse Navid. She is highly capable and never financially dependent on the player. She cares about honest invoices more than poetic declarations.
**Arc A**: player helps audit mispriced crimson binding cloth. She sets a clear boundary on flirting while married. Respected boundary unlocks deeper nonromantic trust, not an imminent romance.
**Arc B**: professional disagreement: reveal a merchant overcharge that endangers her workshop and the bookshop's supplies; romance cannot be bought by rescuing her finances. She decides what to do about her strained marriage outside the hero's control.
**Arc C**: in one authored late-game route Arghavan initiates a fair separation of her own choosing **for independent reasons**, resolves custody and co-parenting conversations, and later may freely reciprocate the protagonist. In another she reconciles with her spouse and remains an admired friend. Both routes have original scenes and gameplay consequences.
**Gameplay:** priority orders, family-hour schedule, bulk fabric supplier contract, a crimson-bound family record; child care limits after-dark meetings without treating children as obstacles to remove.

### 2. Simin (42), coastal physician-botanist, married in an openly negotiated relationship
**Life without hero:** studies archive fungi, manages clinic supplies, volunteers at a ferry infirmary; spouse Farid has his own practice and public boundaries.
**Arc A**: mutual research into paper decay and botany; respect for data and people.
**Arc B**: she discloses an existing consensually open relationship model; only after player learns what it really means, both discuss commitment, public perception and autonomy.
**Arc C**: relationship is possible only if existing spouse's agreement and personal boundaries are genuine and up to date. A player seeking exclusive possession can politely decline and preserve collaboration. No hidden flag rewrites spouse consent.
**Gameplay:** herbarium and book-repair material studies, medicine knowledge as fictional lore not real medical instructions, clinic archive commissions and public talks.

### 3. Mahtab (33), maritime records clerk, married, risk-laden secret or independent separation route
**Life without hero:** works long hours correcting harbor records; spouse Behrouz is a visible character with his own motives, not an evil cardboard obstacle. Her career may be damaged by exposing the town register.
**Arc A**: player witnesses mismatch between two voyage logbooks; trust built through accuracy, not gifts.
**Arc B**: mutual attraction appears while her marriage is in crisis. Different choices: maintain principled boundaries, allow a secret relationship with possible social fallout, or step away while she evaluates separation on her own terms.
**Arc C**: no single safe exploitation route; illicit path may cost NPC career trust, friendship and shop supplier reputation; transparent late relationship path may become available after she independently ends her marriage and has time to recover.
**Gameplay:** legal copies of damaged registers, town gossip/contract sources, reprint ledger. The player must not be able to bribe her into falsifying evidence as a 'romance quest'.

### 4. Roya (36), divorced single mother and master carpenter
**Life without hero:** proud shop owner, repairs docks and bookcases, co-parents a 10-year-old with her ex, has no desire for a rescuer. Never asks player to babysit as proof of worth.
**Arc A**: working together silently on the bookshop window; practical skill and mutual craftsmanship.
**Arc B**: a conflict over sustainable timber and a dangerous contract. The protagonist can oppose her business decision and still grow respect by being fair.
**Arc C**: gradual reciprocal affection only if player accepts her professional schedule, co-parent boundaries and goal of owning a second workshop. Parenting doesn't make the relationship a checkbox; two adults learn how to coexist.
**Gameplay:** real furniture upgrades, layout effects, supplier routes and a skill unlock.

### 5. Tahmineh (31), unmarried island archivist and principled adversary
**Life without hero:** opposes private control of island records, has cared for an older relative, values public accountability more than local popularity.
**Arc A**: she accuses the protagonist and elder of withholding pages. Rivalry based on genuine philosophy.
**Arc B**: both independently investigate contradictory printings. Competing values produce intense arguments and quiet recognition.
**Arc C**: optional partnership built on honest confession; if player repeatedly destroys evidence, her moral boundary closes romance while town records quests continue by an alternative.
**Gameplay:** archive access, decoding historical editions, reputation and a public meeting.

### 6. Parisa (45), divorced mother of adult daughter, proprietor of tea-house
**Life without hero:** runs the town's social sanctuary, organizes affordable evening suppers and maintains deliberate privacy; adult daughter has her own coastal venture.
**Arc A**: humor and neighborhood customers; she challenges protagonist's gossip-driven recommendations.
**Arc B**: the player must decide whether to prioritize bookshop growth over hosting a quiet place for tenants. Her support cannot be purchased through donations.
**Arc C**: mature affection, disagreement and negotiated social independence; she doesn't move into the player's home or abandon the tea-house by default.
**Gameplay:** bookstore/tea-house cross-promotions, visitor schedules, neighborhood rumor accuracy.

### 7. Nozhat (61), widowed baker and mother, older-love route
**Life without hero:** keeps a bakery ledger, grieves a son who left the island, supports workers and has opinions on every bad romantic novel.
**Arc A**: protagonist repairs recipe bindings, then is laughed out of the shop for underseasoning bread. Mutual respect grows in everyday work.
**Arc B**: her son's letter poses an ethical boundary: asking consent to examine it matters more than learning its contents. This family story is completable without romance.
**Arc C**: optional deeply human late-life relationship with distinct social and age perspectives, not treated as comic novelty. She can choose platonic companionship or committed affection.
**Gameplay:** affordable store bake sales, emergency supplies in storms and a memorial exhibition.

## Example original scenes (first draft, critique needed)
**Arghavan, Act II, inside weaving studio after closing:** She sets a crimson bobbin upright between you.
> «کمکم کردی حساب‌هام رو سرِ جاش بیارم. بابتش ممنونم. ولی من با یه تشکر قرار نیست زندگی‌م رو عوض کنم. اگر می‌خوای کنارم بمونی، فردا صبح درباره سفارش نخ حرف می‌زنیم. بقیه‌اش رو بذار به انتخاب خودم.»
Response possibilities: «حق با توست؛ سفارش رو فردا بررسی می‌کنیم.» / «فکر می‌کردم بیشتر از این بینمون هست.» / «مدتی فاصله می‌گیرم.» Respectful responses preserve full business quest line; pressing after refusal decreases boundary trust and closes romance *in this save*.

**Simin, Act III, late-night bookshop on a stormy evening:** She turns off the lantern beside the clinic ledger but leaves the shop window illuminated.
> «علاقه‌ای که بینمونه رو انکار نمی‌کنم. اما رابطه من و فرید چیزی نیست که با یه دروغ حذفش کنم. اول باید هر سه نفر بدونیم داریم درباره چه چیزی حرف می‌زنیم. تو هم حق داری بگی نه.»
Player can ask for a public conversation, choose nonromantic collaboration, or step back. No shortcut to change the spouse's wishes.

**Nozhat, Act IV, bakery before dawn:** She slaps a thick book on the counter and laughs.
> «تو این کتاب یه پیرزن چهل صفحه منتظر موند یکی جرئت کنه دوستش داشته باشه. من نونم تا طلوع آماده‌ست؛ چهل صفحه فرصت ندارم. یا چای می‌خوری، یا می‌ذاری با خمیرم حرف بزنم.»
A tender scene about independence rather than mockery of age.

## Three intertwined romance + management quests
- `quest.red_thread_invoice`: Arghavan discovers two textile invoices were swapped; truthful investigation earns an original rare-cloth supplier contract but might implicate a sympathetic dock family. The player can request proper review, privately disclose, or hide evidence and lose future trust. Relationship doesn't buy discounts.
- `quest.two_signatures`: Simin requires a rare printed botanical atlas for public safety research; another customer wants to purchase it privately. Player may loan with supervised reading, make a licensed in-world copy, or sell and fund common supplies. A romance scene can occur later but the clinic quest continues without romance.
- `quest.tea_house_records`: Parisa has a rent-control record contradicting town hall claims. Player can expose only business facts with personal names redacted, offer confidential mediation, or argue for a public hearing; each choice changes town economy, bookshop visits and future relationship.

## Story/route acceptance gates
- For each authored female adult romance candidate, prove **at least one** coherent possible consenting successful ending AND at least one respectful refusal/friendship ending from a fresh game, subject to authored values/sexual orientation/identity. Player is not promised romance in every save.
- 3+ distinctive obstacles for difficult routes, minimum 2 authentic reactions when player refuses, 2 non-generic callback dialogue scenes, at least one economic consequence. No spam gift exchange counts as an obstacle.
- Married and parenting NPCs must have scenes, jobs and decisions independent of player; secrets have plausible costs and no "conquer the wife" bonus.
- Each NPC has a unique speech rhythm tested by a blind character voice identification pass. Scenes display layered anime portrait expressions that respond to dialogue, not the same smile.
- Main quest and essential skills are fully accessible for players who pursue ZERO romance.
- Decisions persist across saves, locale switches and NPC off-screen schedules; no dialogue history rewritten just because interest is high.
