# ADR-002 — Shared Story, Management and Reading Motivation Design
**Date:** 2026-10-14
**Status:** two live Gemini 4 Argon critique/response rounds completed; joint design consensus for preproduction, **NOT a user-approved canon, not yet implemented/tested**.
**Scope:** Original standalone Godot RPG narrative + future optional Reader engagement; no changes to Veil Reader's real EPUB/PDF UI.

## Verifiable discussion record
First Gemini independent creative challenge prompt stored on authorized desktop:
`E:\EyadStudio\Temp\Gemini_Island_DeepStory_Loops_20261014.txt`
Answer captured:
`E:\EyadStudio\Temp\Gemini_DeepStory_Loops_Response_20261014.txt`
After reading its proposal, ChatGPT challenged nine specific aspects including elder's illness as guilt pressure, privacy of records, per-day reading cap, repetitive physical book exams, and scope; counterproposal:
`E:\EyadStudio\Temp\Gemini_DeepStory_Round2_Counter_20261014.txt`
Actual Gemini acceptance/counterargument captured:
`E:\EyadStudio\Temp\Gemini_DeepStory_Round2_Response_20261014.txt`.
Gemini explicitly agreed with A/B/C/E/G/H/I, accepted a conditional rather than pre-known player identity F, and asked to test the % of curated book puzzles D. That is a real two-model deliberation, not a role-played endorsement or code test.

## Joint decisions, with important nuance
1. **Story hypothesis:** prioritize **'نسخه‌برداران فراموشی' (Bookbinders of Missing Pages)** as candidate story arc: island families maintain damaged genealogical/history books; the elderly binder sometimes removed pages in the name of peace. The decisive drama is who owns an account of the past, not a generic chosen-one boss fight.
2. **Workshop drama:** the book-binding desk is the primary site of discovery, restoration, ownership dilemmas and social clues. Pixel town exploration expands in the authored second chapter, rather than replacing the workshop or scattering 5 full gameplay loops across a small preproduction budget.
3. **The old man is a person, not a ticking timer.** His baseline medical support/health never competes with business upgrades and does not deteriorate because the player waits, reads slowly or spends shop money unwisely. He autonomously manages himself and has goals that may conflict with player decisions.
4. **Multiple defensible moral routes:** significant archival quests have at least 3 truly different player methods, including explicit consent, confidential sealed conservation, negotiated disclosure or redacted public copy. Facts remain available through alternative evidence: privacy violation is NEVER a mandatory puzzle solution.
5. **Shop management actually matters:** book quality, title/condition/genre, demand, customer personalities, supplier material availability, fair pricing, loans, repair tools, shop positioning, partnerships, working capital, and failure-recovery have practical causal consequences. No real-time debt/streak/FOMO penalty.
6. **Gameplay cadence:** not every book is a three-layer forensic puzzle. Test approximately 20–30% curated complex books, quick 2-second visual inspections for ordinary books, and later assisted restocking/delegation. Do NOT hardcode 20% without player-testing. Funny/light interactions alternate with consequential quests; do not force 3–4 chores after every dramatic event.
7. **Non-generic NPCs:** each has a private objective and schedule, values they may defend even against player, one contradiction, recognizable voice, comedic event, serious event, and dialogue changes tied to remembered player decisions. Avoid disability stereotypes and exposition-only quest dispenser NPCs.
8. **Mystery does not require abstract magic:** use material clues, archival contradictions, paper decay and possible restrained uncanny atmosphere, with a published internal rules ledger. The hero's precise pre-shipwreck role is still a PROPOSAL; elder may suspect but must not conveniently know the entire identity upfront.
9. **Real reading incentives** are strong but fair. The *future* opt-in ReaderBridge supplies no private book text/title/URI to game. Eligible cumulative engagement can earn cosmetic sets, optional restoration materials, community beauty projects and alternative skills/economic conveniences—NOT exclusive main plot clues, mandatory core skills, family documents or required story chapters. TTS and slow readers must be fairly supported. No 45-minute-per-day cap, no check-in streak, no punishing excessive reading or real-world absence. Stored high-water grant IDs prevent duplicate credit; single-player tamper-resistance should not hurt honest readers.
10. **First scope:** 6 fully original fictional books, 2 active customers, 2 small playable rooms + short coastal opening, one branching ethical quest with three choices, one layered anime portrait and one simple second portrait, one economic decision and one comic beat. Prototype in Godot 4.7.2 with headless economy tests before finished art.
11. **Replay/QA:** 3 shop days in a row must differ materially in at least one customer intent, resource shortage, relationship implication or book condition. Consequences must be legible, reversible via recovery arc where appropriate, with sensible save/load and a clear ledger. Scene/dialogue/readable RTL QA is required.
12. **No plagiarism:** obtain full learning value from games' genre mechanics, professional level design, good UX and permissible source code, but our cast, dialogue, world identity, level art and quest outcomes are original. REA-style research uses lawful public gameplay observation/open-source material without cracked protected game assets.

## Disagreements still UNRESOLVED / need experiments
- Number of complex restoration/deduction books: Gemini proposes 20–30%, ChatGPT proposes testing 15–25%. **Decide by 3 simulated shop days + 5-player playtest**, metric: boredom and perceived touch quality.
- Magical spores and altered memory are a Gemini story pitch, not lore canon. **Do not** attribute actual medical amnesia to fictional fungus in technical docs. Prefer grounded records until supernatural story rules are separately authored/approved.
- Exact full-game character names and hero background are pending user's creative decision. The original saved draft uses candidate NPCs different from Gemini's; character/cast merge requires screenplay voice tests, not unilateral replacement.
- Proposed chemical erasure as one ethical choice must not be presented as consequence-free anonymity. It can damage evidence, breach consent and worsen trust. Encourage non-destructive redaction/copy in normal ethical choices.
- Archive Light reward thresholds, formulas, eligibility or TTS definitions are untested: do NOT import Gemini's arbitrary capped-daily anti-cheat into production and do NOT invent fake minute counts.

## Story first 20m — selected concept under testing
A brief, controllable shipwreck opening and elder rescue leads into the upstairs loft and old shop. Player uses a torn, strange book to learn repair. An ordinary customer request masks a sensitive family history problem: recover two conflicting names in one spine and decide consent-first preservation, witness-mediated examination or independent public register check. One humorous workbench misunderstanding interrupts the stakes. End: an article or record shows the player's recognizable customized face, but the damaged headline keeps their prior role open. Show enough to hook the next day but not an exposition dump.
Two draft authored documents:
- `ISLAND_STORY_ECONOMY_READER_REWARDS_BIBLE_v0.2.md`
- `OPENING_20_MINUTE_BOOKS_AND_QUEST_SPEC_v0.1.md`
Gemini's alternative 'baker's debt ledger' three-way quest is another candidate; compare emotional plausibility and consent/privacy before selecting. The note that 'chemical washing can erase debts' is NOT an established legal fact or game rule.

## Three reward motivations to PROTOTYPE (noncompulsory)
1. **See a changing world:** cumulative eligible reading gradually restores lanterns, display cases, foliage, upstairs home and district soundscape. The change is visible, satisfying and opt-in.
2. **Express character mastery:** earn archival cosmetic uniforms, paper/ink styles, custom animation poses and optional more sophisticated crafting affordances without locking quests.
3. **Support the people:** earn optional community exhibits, weekend events, bonus loan programs and economic conveniences, with alternate in-game sources so story progress does not depend on actual EPUB reading.

## Delivery gates
G0: user approves canon and character voice. G1: write 6 fiction books with cover/blurb/marginalia and two complex NPC scenarios. G2: greybox 20min story and shop with 3 choices, black-box RPG economy tests. G3: 5-user playtest with anti-repetition and character voice identification. G4: art and dialogue cut-ins after mechanic proven. G5: someday ReaderBridge after offline reader and standalone Godot remain independent, with consent UX and verified accounting.
