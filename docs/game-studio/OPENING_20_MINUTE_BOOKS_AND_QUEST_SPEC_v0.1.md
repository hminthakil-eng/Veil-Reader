# Island Bookshop — First 20 Minutes: Six Fictional Books, Two Customers, One Real Dilemma
**Scope:** authored game/narrative design prototype, not runnable code. Related: story bible v0.2. Never access or import actual user ebooks.
**Status:** proposed, awaiting final 2-model peer review, narrative approval, independent player tests.

## Tone and staging contract
Opening should be poignant, lively, visually striking and *playable*, not a 15-min exposition cutscene. Warm shop lamps, cold moon-grey harbor, precise small gestures, eccentric comic banter and mystery. Rain and harbor sounds carry the first 60 seconds, then high-resolution anime portrait appears ONLY when the elder chooses to speak.

## First 20 minutes: authored sequence and control
| Time | Player interaction | Why fun / payoff | Persisted game state |
| --- | --- | --- | --- |
| 0:00–1:30 | Wake on shipwreck beach; inspect a familiar torn sleeve and pick **one of three** found objects, not a permanent morality test; elder arrives with lantern and asks whether you can stand | immediate agency, no lengthy tutorial | survivor.relief, beach.firstRelic |
| 1:30–4:00 | Arrive in upstairs loft; water drips from sleeve, companion elder sets down tea; mirror opens anime appearance customization | belonging and a beautiful portrait; pixel-world sprite must match | avatar cosmetics in game's isolated canonical identity state |
| 4:00–7:00 | Descend into bookshop, inspect a cover and blurb; demonstrate humidity damage; select restoration path at binding desk | tactile craft and discovery of a hidden *non-private in-game* page | shop.firstInspect; repair.workOrderA |
| 7:00–11:00 | First customer asks for a book not by genre but by a hidden human need; browse 3 candidate books, ask a follow-up question, recommend or lend; learn trust and money tradeoff | inference, humor, immediate feedback | customer.mahinFirstVisit, ledger txId |
| 11:00–15:00 | Second customer wants an inherited family chronicle, but its seam hides a page naming someone she no longer speaks to. Player may repair intact, request permission to open it, or offer archival safe-keeping | consequential choice without forced snooping | quest.twoNames, elder.trustBranch |
| 15:00–17:30 | Choose to invest 12 fiction coins in a repair tool, second shelf, or community reading seat; UI explains margin, demand and social outcome | recognizable management, non-generic strategy | shop.upgradeDecision |
| 17:30–20:00 | After dusk, elder points out photo/article or stamped ship page in a book; the newsprint depicts the customized protagonist, but date or caption is missing. A harbor bell rings twice at wrong hour. Player can save and choose next action | grounded mystery cliffhanger, desire to return without locked plot | prologue.complete; clues.photoA |

Fail-safe: if player refuses any recommendation or sells wrong book, NPC can retry later, no permanent quest softlock. Undo intro appearance, quicksave after every meaningful decision, skip/replay narrative if already read. Dialogue text legible 200% and keyboard/gamepad/controller/mobile touch. In-game 20 minutes are a target **to playtest**, not a tested duration.

## Six in-world books: three playable information layers
Each item has stable ID, visible cover + damage, authored 2-sentence summary, sealed margin or hidden artifact disclosed only after voluntary inspection/repair with owner's permission where appropriate, game choice potential. No real EPUB titles, files, URI, personal reading metadata.

| ID/title (original fiction) | Cover and initial state | Back blurb (short) | Optional marginalia / physical clue | What player decides |
| --- | --- | --- | --- | --- |
| `island.book.tides_ledger` **دفتر جزر خاموش** | Blue sailcloth, salt-stained, pages 30% intact | A harbor keeper charts currents few believe exist. It ends with the claim that a bell rang after the lighthouse went dark. | A pasted ferry ticket with a date inconsistent with municipal records. | Sell it to keeper, lend for study, preserve as clue; offer alternatives if owner requests privacy. |
| `island.book.bread` **نانِ فردا** | Cheap paper, grease marks, 80% condition | An elderly baker preserves recipes in stories about lost tenants. Every remembered flavor is also a map of a neighborhood. | Separate grocery bills show which household was quietly fed during a blockade. | Recommend to baker vs collector, trade money against community trust. |
| `island.book.names` **دو نام بر یک شیرازه** | Hand-stamped family chronicle, spine reopened twice, 35% condition | In a changing port town, households change names but never agree why. Three women describe the same summer differently. | A loose signed consent note—perhaps forged—naming an estranged daughter. | Preserve, ask permission, or mediate a reading; NEVER force public disclosure. |
| `island.book.lighthouse` **فانوسِ سوم** | Copper-inked map, warped corners, 60% condition | A keeper lists signals that should never be mistaken for safe passage. The last diagram has been crossed out with a blunt tool. | Traces of two different inks reveal that the route was altered after printing. | Restore/compare before giving to a customer; negotiate access. |
| `island.book.seagrass` **گیاهانِ لبهٔ نمک** | Herbarium with brittle inserts, 50% condition | An illustrator catalogues island coastal plants, both useful and misunderstood. One chapter argues that paper damage might have multiple causes. | Pressed flower with different shipping origin; no untested claims about medicinal effects. | Repair or loan for botanist fieldwork; decide whether to disclose conflicting evidence. |
| `island.book.letters` **نامه‌هایی که پست نشدند** | Plain grey cloth, stitched shut by thread of another color, 40% condition | A letter writer describes a lighthouse without giving its name. Each note is dated a day after an unrecorded storm. | A folded unopened letter **belongs to its addressee**; existence is a clue, not an automatic permission to read. | Deliver sealed, seek consent or preserve unopened; later dialogue remembers choice. |

## Two customer desires (not a genre matching minigame)
**M01, harbor keeper Mahin**: says she needs a navigational volume for an apprentice. Actually fears another shipwreck, and price or map age is not the core constraint. Her eye movement, silence and choices change if you offer the chart vs a memoir vs refer her to a personal conversation. At least 2 valid suggestions with different reasons; failing a suggestion teaches a clear preference rather than random refusal.
**M02, Mrs Nozhat, retired baker**: asks for the family binding formerly kept by her missing son. The book belongs to a family, not necessarily her legal sole property. She accepts a safe archival copy or private meeting instead of covertly tearing out the last page. A third party's interests are real; player must earn trust and consent.

## First moral microquest 'Two Names on a Spine'
**Trigger:** second customer brings old family book to restore. Two versions of one marriage record appear in the spine. The two names reflect a split family, not a simple forged-vs-true label.
- A: **Respect ownership** — restore binding without opening sealed private section and offer family-mediated conservation. Immediate lower fee, +trust, mystery remains partially unresolved but next lead available at dock.
- B: **Request informed permission** — ask rightful owner and affected daughter to inspect hidden note in a private meeting. Takes one in-game phase, reveals alternative evidence, improves future relationship, costs shop materials.
- C: **Independent verification** — inspect harmless ink/watermark, cross-check public harbor register, preserve original sealed. Immediate economic benefit from catalog work, more steps later, does not violate privacy.
Bad decision recovery: if player prematurely shares a note in another branch, later apology/return and NPC choices have a genuine but recoverable consequence. Never force harm to access chapter 2. No 'right answer' highlighted; choices show outcome in dialogue, shop ledger and an evolving relationships journal.

## Crafting, management and fun feedback
- The **repair bench** makes each quality upgrade visible: book pages unstick, seam tension changes, cover texture improves, clue becomes visible if allowed.
- The **shop front window** is a political/social statement: who walks in changes after player curates shelves, not simply +10% profit.
- The **local economy** is not high-frequency tapping: choose among supply order, acquire used copy, or commission repair; share/opening time is controllable, with lightweight automation after first tutorial.
- Elder banter is responsive to work, not a fetch-quest vending machine. E.g., after a player spends the final 3 coins repairing something: «من نگفتم پولت رو بده به شیرازه. ولی خب… شیرازه از من خوش‌حساب‌تره.»
- Skill unlock each grants a choice: compare edition prints; improve fair negotiation; restore fragile paper; safely archive a document; organize lending cooperative.

## Authored dialogue performance test
Three writers/models independently receive a scene prompt and character rules: 'rain starts leaking through the ceiling while a customer demands the same book'. They author one line each for 5 NPCs with names removed. Blind reviewer should correctly infer 4/5 voices. Reject stock fantasy exposition, generic praise after gifts and mystery revelations without planted physical clues.

## Non-repetition acceptance
Across first three virtual shop days, at least one meaningful new mechanic or human consequence must appear; the core sell/repair interaction may recur, but combination of need, scarce input, relationship consequence and available strategy changes. Repeated barks are short and do not block movement. Player may enable assisted stock and repair after competence demonstration.
