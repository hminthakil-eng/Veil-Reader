# Veil Reader Reborn — Gemini 4 Argon Investigation Ledger Architecture Review
Date: 2026-10-16
Scope: Offline-first user-owned clue links and sealed predictions.
Status: ARCHITECTURE DECISION, NOT IMPLEMENTED.
Branch: design/veil-reborn-ux-v1-20261016, previous tested SHA 812a21769cc9bc804fcb56679d15564086ffcbf0.
Figma: https://www.figma.com/design/wj3UC9brK7faSwuLLuxKTh
Gemini transcript on authorized PC: C:\Users\Microsoft\EyadStudio\Tools\GeminiBridge\gemini_clue_links_consult_20261016.txt
Gemini Argon request was sent with desktop confirmation, and substantive response was retrieved 2026-10-16. It did not inspect local repo or live Figma.

## Product north star
Lord of Mysteries inspires knowledge, investigation, mystery and user-earned revelations, not visual occult clutter or copyrighted character/lore imitation. Reading remains instant, quiet, offline and first-class, while optional evidence and predictions feel like a private research archive.

## Accepted from Gemini
- Theory records link IDs of saved Highlight entries without copying excerpts.
- Home, notifications and list previews never show sensitive saved quote, note or prediction text by default; explicit user Reveal is required and must be screenreader-safe.
- Start with accessible LIST and user-authored links, not a graph animation. TalkBack and Persian RTL at 200% text scale must be supported.
- Never unseal based only on Book.progress, current Readium totalProgression or currentLocator. Jumps, footnotes, rereading and reflow make these unsafe.
- Version-scope references by publication identity and fail closed on deletion, modified import or unresolvable locator.
- Implement reversible domain logic and tests before any new persisted-data schema or reader change.

## Critically modified or rejected
1. Gemini called sidecar JSON zero-risk and 100% reversible. False without audited atomic file writes, backup/restore, device storage lifecycle, lost-update concurrency, permissions, encryption and sync. Do not persist predictions in sidecar now. Compare a separately migrated Room table only after evidence gates.
2. Gemini suggested header/tail partial hashing. That can miss changed middle chapters. Prefer full streamed SHA-256 over source bytes for exact-version identity; treat book metadata as hints and require publication mismatch handling.
3. Gemini suggested Comparable tolerance threshold of 0.015. This violates transitivity in some triples and cannot be a security/ordering boundary. Never use fuzzy progression ordering to unseal.
4. A mandatory 15-second dwell timer is not proof of reading and is harmful to accessibility and reading styles. No compulsory timer or streak.
5. Automatically re-sealing previously viewed predictions on reread can confuse users; use a clear user-controlled reread privacy policy and no unsolicited reveal.
6. Blanket clearAndSetSemantics can suppress functional controls; mask actual sensitive text at presentation state, preserve accessible buttons.
7. Hiding quote text from composables does not guarantee it cannot exist in heap or debug inspection. No absolute privacy claim without audits.

## Proposed domain contract (future)
- PublicationIdentity: full-file SHA-256, stable import ID, source format, canonical version, parser version. Reimport of changed files must not silently transfer anchors.
- ClueTheory: opaque UUID, publication identity, user-authored private title/body, updated timestamp, Highlight ID references (no quote copies).
- ClueLink: UUID, source and target saved-highlight ID, user label/relationship, publication identity; orphaned references become disabled tombstones.
- SealedPrediction: UUID, publication identity, own authored text, creation anchor, target anchor, explicit seal state, never auto-displayed.
- AnchorReference: original Readium Locator JSON + scoped publication identity + validated reading-order locator. No guessed EPUB physical page. Verify PDF page convention in installed Readium.
- TrustedCoverage: initially empty; navigation jumps cannot change it. Verified section coverage requires evidence and survives only if publication identity is unchanged.
- UX states: SEALED, ELIGIBLE_MANUAL_REVEAL (still text hidden), REVEALED, UNVERIFIABLE. Missing/untrusted coverage stays SEALED or UNVERIFIABLE.

## Staged engineering and acceptance
1. First confirm all 17 focused tests for current Reborn Desk. Previous focused RTL 1/1 passed; full suite was running separately.
2. Small pure Kotlin model + eligibility evaluator and unit tests on dedicated reversible branch, no Room migration, file persistence, Readium navigator or UI overlay. Tests: publication mismatch, jump without trusted coverage, reveal only after deliberate click, reimport changed version, orphan Highlight, RTL presentation state no quote.
3. Only after approval: decide audited Room storage and reversible migration with backed-up user data, deletion and restore tests.
4. Build accessible linked-clue list. Later implement the optional prediction unseal interaction; no mandatory game layer.
5. Device QA on real EPUB/PDF, phone RTL, 200% font, screenreaders, publication reflow, interrupted writes and offline test.

## Non-negotiable boundaries
Default and release gates OFF until CI, tests and real device evidence. Preserve all four reading modes, original Book and Highlight records, Room and Readium behavior, progress and reader position. Never claim spoiler safety based on percentage or a unit-only test.
