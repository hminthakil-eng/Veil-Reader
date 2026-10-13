# Apple-guided native reading tools

Continuation of #514 from `0b178b5d2cafc7bb1d0c2ca1b65df2d118093f78`.
Canonical Reader base remains `5fdaa496c2866bb30330364db2ae420054a1ab1f` (#509).
Scope: chapters, Notebook, note editing, in-book search and return to reading.

## Result

- Opaque Material semantic surfaces and text in the entire Notebook, note editor,
  deletion confirmation and search. Saved passage content accepts the same palette;
  other archive callers retain their established palette.
- Compact pinned title/48dp exit. Two-column section choices when narrow, a native
  section menu when height and text scale leave less room. The same sections remain
  available. No decorative animation or publication overlay is introduced.
- Contents opens near the current **resource**, supports title filtering and an
  explicit return-to-current action. Filters stay above the chapter viewport. One
  effect owns scrolling after query changes; filtering shows the first match.
  Resource-level fragment matching is preserved, not misrepresented as exact
  subsection detection.
- Note Save/Cancel remain outside the scrolling body. Short wide layouts omit
  optional quote/title chrome and put actions side by side. Awaited persistence,
  disabled-saving state, failure recovery and parent ownership remain intact.
- The selection/new-note dialog uses the same semantic editor and pinned actions,
  preserves the atomic selection-note write and exact text, and shows a recoverable
  save error inside the dialog. New blank drafts remain unsaveable; existing notes
  can still be cleared. The former LIGHT text on a fixed dark input measured about
  1.2:1 and is replaced by the matched theme roles.
- Contents recenters when a delayed current **resource** arrives. Changes to a
  fragment within that resource preserve manual browsing; an active title filter
  still starts at the first match.
- Direct Search now shares Notebook's foreground-speech overlay fence. This
  corrects the foreground `FORGE_QA` path; background speech policy is unchanged.
- Both Search entries reuse the existing paged Readium search session and the same
  `SEARCH_RESULT` navigation callback. Search exploration preserves the reading
  anchor without counting a saved-passage revisit. Notes/bookmarks keep their
  existing navigation reason. The old eager 250-hit Notebook path is removed.
- Cursor acquisition now stages ownership inside IO and closes any cursor that
  cancellation prevents from reaching the UI. Query/tab/book changes still retire
  callbacks and close the active cursor. Search stays local to the publication.
- Query/status/results share one bounded scroll viewport. Clear, Cancel, result
  count, complete-empty and recoverable error states are explicit; result text uses
  script-aware typography. English/Persian resources are maintained together.

## Evidence and design sources

The initial review was source-only. Fixed dark panels paired LIGHT input text
`#2A2118` with Archive `#0D151F` (1.16:1), secondary text 2.49:1 and native text
actions 2.52:1. Unselected tab counts at 48% Mist on Ink measured 2.58:1.
These functional combinations are replaced by matched semantic roles, not a
global font/color redesign.

Used the project Apple adapter, MASTER/Reader override and pinned upstreams:

- `dickwu/apple-design-skill` @ `904b0eedc7cc778152f545506075d5bb5219ce77`:
  `accessibility.md / Vision / Mobility`, `layout.md / Visual hierarchy /
  Adaptability`, `typography.md / Supporting Dynamic Type`, `color.md / Best
  practices / Inclusive color`, `searching.md / Best practices`, `search-fields.md`,
  `sheets.md / Best practices`, designing-for-iOS and cross-platform translation.
- `Elevatormusic/apple-hig` @ `e00f1af16c71f4122e7c964563d2f563fd490d7e`:
  router, universal and design-reviewer workflow. Independent source review was
  requested as that skill directs; review of the working diff identified cursor
  transfer and scroll/filter races, addressed before publication.

Android chrome/navigation, native Back, Material/Compose and Veil's Grayfog
identity prevail. No Apple assets, fonts, icons or runtime dependencies added.
Layout decisions and source-level race mechanisms are design/engineering
judgments; they are not competitor runtime observations.

## Verification plan and actual local receipts

Local policy: 39 checks PASS. Canonical Paper contract and six guards PASS.
Existing 12 authored opaque contrast pairs PASS. `git diff --check` PASS.
Android compilation, lint and device/emulator results are pending at publication;
current exact-source CI receipts are maintained in the PR body.

New native production-composition fixtures check 100-chapter current discovery,
filtering and exact link callback; search uses its own locator callback, not a
saved-passage callback; partially consumed 83-result cursors close; explicit exit
is 48dp and remains reachable; long note text saves exactly while actions remain
visible; saving disables both actions. LIGHT English, Persian 200%, 340/520dp
height, 220dp note viewport, wide DARK and increased contrast are exercised.
Captured PNGs are fictional Compose specimens, not actual publication pixels.

Continuation fixtures cover a late null/stale chapter locator, stable browsing
across same-resource fragment changes, filter-first behavior, and the actual
selection-note Dialog in LIGHT/DARK and a Persian 200% short window. They exercise
blank-new/empty-existing note policies, exact Unicode, disabled saving and visible
live error recovery. The short Search fixture waits for loading to settle before
scrolling to the lazy result; it retains the ten-second timeout and all navigation,
cursor-close, display and exit assertions. The `52c179c1` receipt had 1,095 tests and
one failure from waiting for an offscreen lazy result. Fresh source receipts belong
to the full candidate SHA recorded in the draft PR; prior results are historical.

Search cancellation is tested during the IO cursor ownership transfer, including
an open operation that finishes after cancellation. The visible empty-state
contracts test actual shared UI and distinguish a yielded empty page from the end.
Semantic foreground/surface ratios are tested against the palette emitted by
VeilTheme in both appearances and both high-contrast variants.

## Remaining acceptance

Physical phone/tablet, actual IME and TalkBack focus/announcements, native sheet
drag/back cancellation, note-write/restart continuity, actual publication viewport
and locator continuity, 60/120Hz feel and direct current competitor comparisons
remain unverified. Keep draft and production Paper rollout OFF; no public release
or main merge follows from build or fixture success alone.
