# Gemini 4 Argon — independent peer review of Veil Reader Reborn Latest Clue
Date: 2026-10-16. Status: CONSULTATION / DESIGN DECISIONS ONLY; no code modifications or test claims in this document.
Local branch head: c220c54f92943bc15884b43299d50482ead4e163.
Source reviewed via pasted Kotlin excerpt: LatestClue.kt deriveLatestClue selector and the described home card. The model did not access source files or Figma directly.
Figma: https://www.figma.com/design/wj3UC9brK7faSwuLLuxKTh

## Gemini's critical findings
1. [Adopt] asSequence().sortedWith(comparator).firstOrNull() is O(N log N) for one result; switch to single-pass selection (minWithOrNull on reversed priority comparator, or explicit reducer). Recomposition is mitigated by remember(books,highlights), so this is an optimization, NOT a proven P0 performance incident.
2. [Adopt] Never show potentially sensitive or spoiler-filled full highlight text on Home by default. Small compact card can show only book title and existence of a saved clue. Clicking it returns directly to saved passage; showing saved text is an explicit action in Investigation Desk. Include optional privacy setting later.
3. [Investigate] Raw non-blank locatorJson can be corrupted, stale, or mismatched with publication. Validation should use Readium Locator parsing and publication identity before relying on it, with a safe invalid-state fallback. Never assume a non-empty string is safe.
4. [Do not adopt naively] Filtering against book.progress or totalProgression alone does NOT guarantee anti-spoiler protection during re-reading, imports, jumps, modified EPUBs or reflow. Keep *user-authored saved highlights* separate from searching unseen book text. A spoiler-safe full-text search requires version-specific stable reading-order + intrachapter locator and cannot reveal source text beyond trusted exposure history. Do not claim this is implemented.
5. [Investigate] Deterministic ordering among ties should use a stable unique ID (including corrupted duplicate IDs policy); avoid comparing large quote and JSON payloads as tiebreakers. Always handle book deletion.
6. [Reject now] A visually complex graph as first Investigation Desk implementation. Start with an accessible ordered list of clues and links, preserving text-only TalkBack/RTL navigation.
7. [Preserve] Offline-first, user data, current Book/Highlight and onOpenPassage callback, default-off debug-only flag; never touch Room or reader engine in first patch.

## Proposed scope for next coding increment
A tightly bounded private / performance patch: pure Latest Clue selector uses linear reduction; the Home version masks/omits the quote by default, while preserving a one-tap link to the exact saved locator. Keep domain-based tests separated from slow Robolectric tests. Build tests must be reported as tested only after actual passing test XML/log, not mere Kotlin compilation.

## Critique of Gemini's initial implementation suggestions
- O(N log N) under memoization is not automatically a P0 user-visible bug.
- A 16-ms performance target inside an uncontrolled JVM unit test is not a stable quality gate; prefer comparative/benchmark sampling on device plus complexity tests.
- Java/Kotlin string hashCode() is not a durable content anchor, especially across modified EPUB versions; use publication identity, locator data, quote and surrounding text as recovery hints with explicit confidence.
- A temporary parallel JSON/DataStore persistence path can introduce migrations and consistency problems. Prefer an explicit domain contract and a reviewed Room migration when actual persistence is in scope.
- 'Sealed Prediction' and link graph are later features; no release readiness claim now.

## Six acceptance gates
1. Latest selection deterministic under stable tie cases, filtered invalid/orphan input, no crash.
2. No quote rendered/accessibility-announced on the Home by default.
3. Clicking a valid card forwards the exact original Book and locator without changing reader progress itself.
4. Missing/deleted book, blank quote, invalid locator all fail safely; invalid JSON never causes crash.
5. RTL/TalkBack/200% text usable without truncating action control or reversed navigation.
6. No schema/reader engine change and release/default flags remain OFF; focused unit & UI tests actually finish before merge.

## Actual evidence available
Prior latest review confirms existing Kotlin Debug compilation succeeded and 39 reading-policy checks passed. Focused Robolectric test run was still running / inconclusive as of review. No real-phone acceptance, no APK acceptance, no merge or push.

Full first model response captured on authorized desktop:
C:\Users\Microsoft\EyadStudio\Tools\GeminiBridge\gemini_reborn_peer_review2_response_20261016.txt

This document is the council synthesis, not a verbatim tool transcript.

## Second-pass debate and corrections (Gemini 4 Argon)
A follow-up explicitly challenged unsafe assumptions in the first review. Gemini agreed:
- Reading book.progress or totalProgression is not a sufficient security boundary for rereads, imports and reflow. Do not discard legitimate user-saved highlights merely because current reading position went backwards.
- Stable anchoring for future verified spoiler-aware indexing may use publication fingerprint, Readium Locator/spine order, durable reading coverage and text context; no naive Kotlin hashCode anchoring.
- No temporary parallel JSON/DataStore just for prototype clue links. Plan schema, then use reviewed Room migration only when needed.
- Runtime assertion under 16 ms on shared JVM is not a deterministic acceptance test; validate O(N) logic and benchmark separately.
- On Home, do not include saved quote or surrounding text in the presentation model; use a neutral label plus book title, with explicit action to open the saved location. Avoid chapter titles too when they might themselves reveal spoilers.
- Future full-text spoiler-safe search must use actual publication order plus durable user exposure history / high-water mark. High-water mark by itself may be insufficient for non-contiguous reading, jumps or selective chapter access; enforce contiguous or explicitly trusted coverage, with fail-closed behavior.

### Technical cautions for implementer
The second Gemini response supplied pseudocode using a new ClueAnchor type that DOES NOT exist in current source; DO NOT paste or treat as compile-ready code. Its maxWithOrNull comparator uses ascending ID for tie breaking and therefore chooses the largest ID, while the currently tested contract chooses the smallest ID; preserve established semantics. Changing to linear selection must be verified against original comparator on randomized/duplicate input with reference oracle. Neutral Home labels must not leak quotes in visual display or TalkBack semantics. Distinguish display privacy from true spoiler-safe full-text retrieval; only latter warrants reading-history indexing.

Second-round raw review stored locally at:
C:\Users\Microsoft\EyadStudio\Tools\GeminiBridge\gemini_reborn_peer_review3_response_20261016.txt

Recommendation: accept conceptual improvements, but change no runtime reader/persistence code in this consultation-only turn. First implementation next is linear pure selector with an opaque Home summary and controlled privacy, after focused tests are diagnosable. Pending Robolectric test runtime still unverified at this consultation.

## Implemented prototype patch — 2026-10-16, following peer review

This decision has now been implemented only in branch design/veil-reborn-ux-v1-20261016; NOT released or merged:
- LatestClue selector switched from full sortedWith().firstOrNull() to filtered minWithOrNull() using the same comparator. O(N) selection, preserving descending timestamp / ascending ID deterministic ordering, with all original tie conditions preserved.
- Home clue no longer renders any saved quote. A neutral localized line replaces it; exact same one-tap Book + locator callback remains. Access to underlying highlight data stays in memory; this is presentation privacy, not encryption.
- Reborn LazyColumn was assigned a stable semantics tag; the previously failing Castle lazy-list UI test explicitly scrolls to the world item before finding its action. The failure was from an off-screen uncomposed item.
- Added a 1200-highlight oracle-comparison test (including reverse ordering, invalid orphan and a restarted reread) and a UI assertion that saved quote text is absent.
- No Room migration, Readium changes, release-flag change or new dependencies. Pure saved highlight lookup is NOT claimed to solve spoiler-safe full-book text search.
- Debug Kotlin and Debug unit-test Kotlin compilation reached the Gradle test task in initial verification. At the time of this document update the NEW test rerun had not produced fresh final XML; do NOT mark green or generate a public release from this patch without test & device QA.


## Investigation Desk MVP ? 2026-10-16

Implementation scope (debug opt-in presentation only): a nested Desk inside RebornThresholdScreen, using the existing Book/Highlight inputs and exact onOpenPassage callback. No application navigation, database, Reader, or release flag changes.

Working-code contract:
- Group real saved highlights/notes by book, with stable book-title/ID ordering and newest-first timestamp/ID ordering inside each book. Individual marks are lazy items; a 1,000-mark input does not compose 1,000 cards.
- Search book titles only, with an explicit label. Hidden quotes and notes never influence search results. No publication text search or inferred reading coverage.
- Book title and neutral saved-passage metadata precede explicit Reveal/Hide actions. Hidden quote/note composables are absent from both merged and unmerged accessibility trees. Reveal is local UI state and is not written to the database; recomposition and restoration behavior still need explicit validation.
- Return forwards the original Book object and unmodified locatorJson. Desk validation performs a minimal JSON href/type structure check without opening a publication. It cannot establish that a formerly valid chapter still exists in a modified publication; existing Reader error handling remains responsible for that case.
- Orphaned books, unavailable source URIs, blank IDs, blank text plus blank note, malformed locators, and ambiguous duplicate mark IDs are omitted consistently. Duplicate book IDs are also excluded. The empty state explains that only saved marks with available sources are listed.
- Home offers the Desk only when this validated index is nonempty. The latest Home clue remains private and unchanged. The on-screen Back button returns locally to Home. System Back integration still requires confirmation. No replacement bottom navigation.
- Controls stack vertically, have minimum 48dp targets, and text has no fixed card height. English and Persian strings are resources.

Future concepts, NOT implemented: clue-link graph, research threads, generated relationships, sealed prediction, scheduled chapter unlock, AI interpretation, and spoiler-safe full-publication retrieval. Figma node 6:2 was consulted for the archive hierarchy; its example quote, graph, prediction and duplicate bottom navigation are not shipped in this slice.

Verification evidence for this increment will be recorded below after focused offline execution. Prior ten-test evidence above belongs to the earlier Latest Clue patch and is not evidence for the Desk.

## Investigation Desk — implemented experimental vertical slice (2026-10-16)
- Actual screen: RebornInvestigationDesk.kt; pure model: InvestigationDesk.kt; integrated behind existing RebornThresholdScreen while BuildConfig.DEBUG and REBORN_THRESHOLD are both true.
- Uses saved Highlight rows and existing Book and onOpenPassage(Book, originalLocatorJson). No Room migration, scraping, AI-suggested discoveries, hidden book-content extraction, or new reader navigator.
- Groups highlights by surviving book, sorts marks latest-first with deterministic ID tie breaking, searches only the book title, and lazily renders large collections.
- Privacy contract: the home remains quote-free; in Investigation Desk quote and user note are omitted from UI semantics and the visible tree until a per-clue explicit Reveal action. Hide returns it to sealed state. Return to passage works without revealing text in the desk.
- Data hygiene: unavailable/deleted books and corrupted/malformed locators are not offered as navigable marks. Duplicate highlight IDs are rejected as ambiguous. Readium is still responsible for resolving locators against actual publications at navigation time; JSON shape checks alone are not the final link-verification gate.
- Design references: Grayfog/archival investigation inspired by the atmosphere of Lord of Mysteries without copying its character names or canon. The Investigation Desk Figma concept depicts a future graph, but links, sealed predictions, classifications and knowledge-network persistence are NOT part of this first implementation.
- Tests: InvestigationDeskTest (4 model scenarios) and RebornInvestigationDeskTest (3 interaction scenarios, including RTL and 200% font). Existing focused tests retained. Verification state must come from new Gradle XML reports; a successful compilation alone is not enough.
