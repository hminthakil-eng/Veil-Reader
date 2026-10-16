# Veil Reader Reborn — Lord of Mysteries Emotional UX Council
Date: 2026-10-16
Input: direct Gemini 4 Argon consultation with human owner and ChatGPT synthesis.
Figma: https://www.figma.com/design/wj3UC9brK7faSwuLLuxKTh
Worktree: design/veil-reborn-ux-v1-20261016
Status: DESIGN DECISIONS; Latest Clue exception below is IMPLEMENTED IN DEBUG PROTOTYPE ONLY. Gemini did not inspect the real source or live Figma. No device acceptance.

## North star
A magical and mysterious reading experience through **user behaviors and substantive features**, not an occult wallpaper, gold borders, or empty lore. Reveal useful knowledge gradually; never block the main reader. Preserve offline-first, privacy, localization, reader positions, and all four separate navigation modes. Inspiration from Lord of Mysteries: occult-era atmosphere, archive, investigation, ritual, knowledge, rank; no copyrighted names, art, dialogue, character or specific canon content reproduced.

## Review: Gemini's seven proposed signature features
| Idea | Decision | Product effect | Safety/architecture gate |
|---|---|---|---|
| Fog-Piercer Lens | ACCEPT AFTER DURABILITY & SEARCH | Lookup character/term using only *already encountered* content and user's prior notes; leave text in place | Proven reading-order / locator boundaries and chapter-level tests; currentProgression alone is insufficient; protect from future results before indexing/displaying |
| Constellation Marginalia | ACCEPT PHASE 2 | Tag highlights as clue, hypothesis, contradiction, world rule; link annotations into a local investigation board | Work as a normal list first; link graph is progressive enhancement, TalkBack alternative, independent of content rights |
| Pendulum Bookmark | OPTIONAL POLISH | Physically styled ribbon/marker conveying when passage was saved | No FOMO or negative aging; show exact date label; no animation in reading area |
| Digestion/Castle | REFRAME & ACCEPT LATER | World grows with real book reading and personal knowledge collection | No mandatory notes, daily streaks, punishments, fake idle timers, dark patterns or rewards earned by page-spam |
| Sealed Predictions | ACCEPT SIGNATURE, PHASE 2 | Save a theory now and open it at a later chapter chosen by reader; delight without AI | Existing annotation + stable locator; no premature unlock or future-book content; postpone schema change until durability proof |
| Blind Archive (three face-down books) | OPTIONAL PHASE 3 | Book selection ritual from user's own library when undecided | User-triggered button only (no motion sensor), avoid future content excerpts/spoilers; reduced-motion non-rotating alternative |
| Candlelight Reading | EXPERIMENT ONLY | Tunable low-light palette/ambient paper with quietly controlled warmth | No evidence of reduced eyestrain; use accessible static contrast-first presets; never dim the readable text or intercept gestures |

## Seven Figma screens — screen contract
1. Threshold Home: resume current book is the obvious primary action, instant first frame, no fake splash; quiet recent book rail; no Castle popups. A subtle archive sigil or note is optional.
2. Grand Archive: retrieval-first search, author/series/status, useful density; clean procedural covers only when metadata cover is missing; optionally provide Book Discovery later.
3. Sanctuary Reader: text, typography, margin and real page-turn; no decorative chrome while reading; instant, interruptible controls; no visual timers in text canvas.
4. Castle World: book-connected rooms as optional visualization, not a compulsory separate economy; world changes only after exiting voluntary reading, never intrusive.
5. Appearance: show contextual sheet with live preview from *actual book* without changing persisted locator; preserve all PAGED/PAPER/SLIDE/SCROLL distinctions.
6. Persian RTL Threshold: mirrored logical layout, stable accessible Persian typography; semantics and touch targets.
7. Persian RTL Reader: confirm gesture direction and curl origin against publication RTL, not device language alone; Persian shaping, zero extra letter spacing, longer readable line height chosen by device tests.

## Interaction grammar
- Quiet, precise materials: fog/static atmospheric depth only outside reading; warm parchment with near-flat noise; in-book high contrast.
- Paper Curl reacts to finger + valid real-page texture and commits location exactly once. Never change gestures or inertia without real-device evaluation. Avoid fake shadows.
- Haptic only if user enables or system permits; at successful semantic events, once per event; sound muted by default.
- Motion must be cancelable and must not slow navigation. Reduced motion must get usable fallback that is communicated, not silently alter an explicitly selected reading mode.
- Notes use low-saturation ink hues, with equivalent semantic labels beyond color.
- No manipulative streaks, punitive effects, lore overload, or overlays that interfere with Readium selection.

## Sprint ordering and acceptance gates
A. Stability/readability: Validate Reborn Threshold debug-gated Compose implementation, Persian/English typography, 1-hand access, font scale to 200%, actual navigation and book resume. Reader gestures/settings must continue to work, validated on real EPUB/PDF in both directions. No merge before CI.
B. Distinctive magic: Implement accessible 'seal note / mark clue' from existing highlights; then spoiler-safe lookup proof of concept; then user-authored future-unlock note with tested persistence and migration.
C. World/polish: Optional user-owned archive-linked Castle, discovery ritual, static lighting presets, haptics. All opt-in, no mandatory feature gates.

## Human validation / A-B probes
1. Fog Lens vs ordinary search: test named term from before/after active locator in long EPUB; **zero future-text leakage is mandatory**, reduction in time returning to reading is a test hypothesis not an achieved result.
2. RTL Paper vs current navigation: count wrong-way turns and cancel/hold failures on actual Persian book and physical Samsung/Pixel, compare user comfort.
3. World after exit vs world shown inside Reader: compare uninterrupted reading time, intrusive-popup count (target zero), user preference; do not reward merely prolonged sessions.

## Technical corrections to Gemini assumptions
- Readium totalProgression is not a sufficient security boundary: resolve per-resource reading order, intra-resource locator, reflow and chapter navigation, backward/forward jump and book changes before rendering snippet.
- Compose overlay over EPUB WebView can steal input events: full gesture arbitration tests before shipped.
- Moving to the next page three times while animations run must queue/commit deterministically; do not assume overlapping animation is safe.
- Reduced Motion should be implemented with accessibility policy and explicit override/fallback, not an unannounced permanent mode switch.
- Vignette / candlelight cannot claim medically supported eyestrain reduction absent controlled evidence.
- Automatic user progress should not require writing notes or play into compulsive tracking.
- Gemini estimates of difficulty are hypotheses. Grade by existing code integration and test evidence, not a 1–5 score alone.

## Immediate engineering handoff
For Codex and Gemini reviewers: check the existing new RebornThresholdScreen and strings, preserve current default UI behind opt-in, run compile and tests when Android Gradle plugin 9.4.0 resolves; do not weaken toolchain. First pass can remain a design prototype without asserting APK/device success. After this gate, implement a *single* distinctive, useful interaction (quick clue tagging) on its own branch, with full persisted-data, accessibility and reading-location QA.

References: Gemini 4 Argon consultation, 2026-10-16, captured locally on authorized PC at C:\Users\Microsoft\EyadStudio\Tools\GeminiBridge\lord_of_mysteries_ux_consult_response.txt.

## Latest Clue / Trace in the Archive — IMPLEMENTED IN DEBUG PROTOTYPE ONLY

One compact optional card below Continue Reading returns to a real user-saved highlight
through the existing `onOpenPassage(Book, String)` callback, with the original locator
unchanged. It quotes only the saved passage (four visible lines with ellipsis), identifies
its book and labels its saved-highlight provenance. It does not infer clues, show notes,
fetch publication text or claim unseen content is safe based on reading progress.

Select the newest valid highlight by `createdAtEpochMs` descending, then highlight ID
ascending; book ID, locator and quote break malformed duplicate-ID ties deterministically.
Require nonblank matching book IDs, quote, locator and a current book with a nonblank
source URI. Missing/deleted books and placeholders are excluded; no valid candidate means
no card, heading or action. Source availability uses current library metadata; opening
errors (including a missing underlying file) remain owned by the existing reader flow.

English/Persian resources, a semantic heading, descriptive passage action, 48dp minimum
action height and wrapping layouts support accessibility. Long quotes are bounded visually;
stored quote and locator remain untouched. No Room/persistence, network, AI, lore,
Readium overlays or cosmetic effects were added. Existing callbacks and default UI remain.
The existing Reborn debug build opt-in remains OFF by default and forced OFF for release
and benchmark. This is not clue tagging or any other phase-2 feature above.

Focused selector and Compose tests are authored for invalid/absent candidates, ordering,
exact callback arguments, deleted-book updates and long mixed-script RTL at 200% text.
Execution evidence and remaining limitations are recorded in the Reborn blueprint.
No APK/device, TalkBack or Persian visual acceptance is claimed.

Verification: 39 policy checks and resource/static checks PASS; opt-in Kotlin compile
PASS (41s), default-off Kotlin compile PASS (32s). Generated release/benchmark flags
remain false. Focused test execution is BLOCKED before tests by sandbox access denial
to cached `core-common-2.2.0.jar` during Java compilation. No test-pass claim or toolchain
weakening. Full commands and local log paths are in the blueprint verification record.
