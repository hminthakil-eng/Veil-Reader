# Codex Cloud — Veil Reader Master Perfection Backlog

## Mission

You are the execution engineer for Veil Reader. Turn the current W39 lineage into a world-class, premium, offline-first Android reader. Do not optimize for feature count. Optimize for reading quality, reliability, interaction ownership, accessibility, performance, visual coherence, and durable state.

Canonical working lineage:
- W36: alpha/w36-full-harmony-motion-v1
- W37: alpha/w37-reader-tap-matrix-v1
- W38: alpha/w38-reader-hardware-keys-v1
- W39: alpha/w39-focus-guide-v1

Start from the current HEAD of `alpha/w39-focus-guide-v1`.

Read first:
- `ROADMAP_0.9_TO_1.0.md`
- `docs/research/MOON_10_7_MICRO_UX_GAP_MATRIX.md`
- `docs/handoffs/CODEX_W39_FOCUS_GUIDE.md`
- `.agents/skills/veil-reader-product-designer/SKILL.md`
- `.agents/skills/veil-reader-ui/SKILL.md` if present
- `app/src/main/java/com/veilreader/app/ui/theme/DesignConstitution.kt`

## Non-negotiable product identity

Veil Reader is not a generic Material ebook reader. It is a premium reading world:
- Threshold = entry / anticipation / Continue Reading
- Sanctuary = Reader / PDF, the calmest and least decorated realm
- Archive = Library / Notes / Profile, memory and retrieval
- Castle = progression / world / utilities
- Ritual = Path / rank / unlock
- Sanctum = secrets / relics / long-term mastery

Visual direction: Grayfog archival mystery, blue-black shell, aged brass, parchment reading surfaces, editorial typography, restrained occult/astronomical motifs. Never let atmosphere reduce readability.

Reader priorities:
- ReadEra-level simplicity
- Moon+-level power and customization
- Apple Books-level polish
- Kindle/Kobo-level reading continuity
- local/offline-first
- no cheap template UI
- no unnecessary glassmorphism
- no persistent chrome over the book
- no fake capability

## Clean-room rule

Competitor behavior may inform product requirements. Never copy Moon+ or another app's DEX, code, resources, fonts, layouts, assets, native libraries, identifiers, or proprietary implementation. Reimplement behavior independently using Veil, Readium, Android, Compose, Room, DataStore, and supported engine APIs.

## Operating model

Do not implement this backlog in one giant PR.

Create sequential, reversible vertical slices:
- W40, W41, W42, ...
- one coherent feature or hardening theme per branch/PR
- each slice includes logic, UI, persistence, accessibility, tests, docs, and regression checks as applicable
- build and test every slice before starting the next
- never merge automatically
- never rewrite the Reader architecture for cleanliness
- do not replace Readium/Pdfium unless a measured blocker proves it necessary
- preserve Room as structured source of truth and DataStore for preferences
- preserve app-private imported publications and offline-first behavior

If a feature is unsupported by the renderer, document the limitation instead of building deceptive UI.

## Phase 0 — establish truth before new work

1. Checkout current W39 HEAD.
2. Run:
   - `./gradlew testDebugUnitTest`
   - `./gradlew assembleDebug`
   - lint / compile tasks already used by the project
   - instrumentation compile tasks if no emulator is available
3. If cloud Android/emulator support exists, run the relevant instrumentation tests.
4. Inspect every compiler warning relevant to correctness, lifecycle, API deprecation, state ownership, or accessibility.
5. Audit dependency versions and exact Readium/Pdfium capabilities before designing features around them.
6. Do not treat GitHub Actions jobs with `steps=null` as test evidence.
7. Produce a short baseline report: build status, unit tests, instrumentation capability, known blockers, measured APK size, dependency/runtime assumptions.

## P0 — W37/W38/W39 regression hardening

### W37 3×3 tap matrix
Verify:
- all nine zones default to Veil existing behavior, not forced page turns
- renderer passthrough does not swallow publication links, footnotes, image actions, or native selection
- explicit previous/next actions are semantic and RTL-safe
- Paper Curl keeps Paper Curl animation
- Slide keeps Slide animation
- static paged remains static
- scroll mode does not fake page-turn semantics
- accessibility/touch exploration wins over custom tap ownership
- malformed persisted grid fails calm
- process recreation preserves the grid

### W38 hardware keys
Verify:
- default is System volume
- opt-in mapping only
- Activity-level interception consumes an event only when Reader truly handles it
- mapped events never also change system volume
- held repeats are throttled
- separate key presses are not throttled as one continuous hold
- Up and Down repeat state is independent
- consumed key-down remains consumed through matching key-up even if mode changes mid-press
- selection, dialogs, modal overlays, close-in-flight, and touch exploration block new Reader actions
- stale Reader sessions cannot clear a newer handler
- scroll mode falls back to system volume when no semantic page action exists

### W39 Focus Guide
Verify:
- OFF is identical to pre-W39
- WINDOW and LINE persist
- quick toggle restores the user's previous active mode
- restart/process recreation also restores the prior mode
- overlay never owns pointer input
- native text selection remains usable
- PDF zoom/pan remains usable
- links/footnotes/images remain usable
- overlay hides during blocking/modals where appropriate
- PAPER, SEPIA, DUSK, OLED all remain readable
- high contrast and TalkBack remain sane
- no central opaque band covers text
- reduced motion does not add decorative animation
- invalid enum/numeric values fail calm

## P0 — Reader durability and correctness

Re-audit, do not assume older fixes are still intact:
- final locator is durably flushed before close returns
- ON_PAUSE / ON_STOP / process death cannot drop the final locator
- startup locator handshake never overwrites a newer user position
- rapid open-close-open cannot attach stale publication/session state
- mode changes cannot commit half-finished page gestures
- previous-location return is transactional and reversible
- external file open survives recreation
- corrupt/encrypted/unsupported publication errors are calm and recoverable
- selection state does not leak across book/session changes
- all jobs/listeners/ActionMode callbacks are disposed with the owning session
- no AccessibilityNodeInfo/test-helper leaks
- first meaningful page/session event is counted correctly; revalidate the historical “first page turn not counted” XP/pacing bug
- page progress never moves backward due to stale async writes
- final progress and reading-session duration agree after fast background/close
- clock rollback / malformed timestamps fail calm
- duplicate open attempts are gated
- low-memory/process recreation restores only valid owned state

Add focused tests for every invariant you touch.

## P0 — EPUB reading quality

Audit representative EPUBs:
- reflowable novels
- RTL Persian/Arabic
- mixed RTL/LTR
- CJK
- footnote-heavy books
- image-heavy books
- tables
- long chapters
- malformed/broken TOC
- fixed-layout EPUB
- embedded fonts
- publisher CSS conflicts

Required quality:
- Paper Curl, Slide, static paged, continuous scroll stay visibly and semantically distinct
- mode changes preserve location with no flash/jump
- typography updates do not reset location
- center-tap chrome is predictable and fast
- chrome auto-hide is calm and never traps the user
- publication links and footnotes never lose ownership to tap zones
- image viewer has reliable enter/exit and restoration
- selection/highlight/note workflow is fast and reversible
- highlights survive theme changes and process recreation
- current chapter/progress data is correct
- publisher styles vs user typography ownership is explicit
- script-aware restrictions remain conservative for RTL/CJK
- fixed-layout controls do not expose reflow-only settings
- dark image treatment never destroys meaningful diagrams
- paper patina does not reduce legibility

## P0 — PDF reading quality

Treat PDF as its own product, not an EPUB imitation.

Audit:
- small text PDFs
- large PDFs
- scanned/image PDFs
- internal links
- bookmarks
- RTL PDFs where supported
- zoom/pan
- page rotation/orientation
- large memory pressure

Fix or add:
- reliable zoom ownership
- page change while zoomed never unexpectedly resets scale unless required
- internal PDF link jumps land on the correct page and preserve previous-location return
- bookmark labels include correct PDF page where useful
- quick up/down/page navigation does not conflict with zoom/pan
- loading/error states for heavy pages
- no chrome timeout deadlocks
- no page=0/zoom=1.0 stale state loops
- PDF reader settings clearly differ from EPUB
- test the historical PDF chrome timeout failure
- test large-file memory behavior
- no fake text highlight UI if engine cannot persist real PDF annotations

## P0 — PDF annotation capability audit

Before implementing any annotation UI:
1. Inspect the exact Readium/Pdfium APIs in the current dependency.
2. Determine whether persistent PDF text annotations, highlights, ink, comments, or coordinates are supported.
3. Classify:
   A. supported safely now
   B. requires adapter/engine extension
   C. unsupported/unsafe and must not be faked
4. Only implement category A unless explicitly authorized to extend the engine.
5. Annotation state must survive reopen/process recreation and map correctly after zoom/page changes.
6. Build an annotation list only from durable, real annotations.
7. Add page number, note text, creation/update time, navigation back to annotation, delete/undo where support exists.

## P1 — missing Reader power features

### TTS
Implement only after confirming supported Readium/Android APIs.
Required:
- start / pause / resume / stop
- next/previous utterance or paragraph
- chapter boundary handling
- language/voice selection
- speech rate and pitch
- background-safe session
- media notification controls
- audio focus
- interruption/call/headphone behavior
- sleep timer
- auto-pause options
- resume from text location
- highlight/follow current spoken passage if reliable
- never corrupt reading position
- no TTS control overlay over the book when hidden
- TalkBack compatibility
- graceful missing voice/language behavior
- no network requirement for the core feature

### Auto-scroll
Add:
- opt-in only
- speed control with sane min/max
- pause-on-touch
- pause while selection/menu/dialog active
- resume affordance
- persistent user speed
- no accidental activation
- reduced-motion/accessibility policy
- stable behavior when chapter boundary loads
- no runaway scroll after backgrounding
- battery-conscious update cadence

### Dictionary routing
From native selection:
- Dictionary action
- installed-app intent routing
- configurable preferred dictionary
- optional custom URL template only with safe encoding
- fallback chooser
- no silent browser launch
- history only if explicitly useful and local
- preserve selection after returning when possible

### Reading time remaining
Provide:
- chapter time remaining
- book time remaining
- based on rolling personal reading speed, not a fixed global WPM fiction
- ignore obvious idle/background intervals
- exclude unstable early samples
- show approximate language when confidence is low
- avoid fake minute-level precision
- update without causing recomposition/jank

### Chapter page estimates
For paginated EPUB:
- derive estimates from actual pagination/appearance when possible
- cache by book + appearance profile
- invalidate on font size, margins, columns, orientation, spread, or relevant layout changes
- never present estimates as immutable print page numbers
- fast enough not to block open

### Recent books quick switch
Add a quiet Reader-adjacent recent-books surface:
- never persistent over reading
- fast return to the current book
- safe close/flush before switching
- no stale locator bleed between books

### Chapter copy/export
Where copyright/user-content semantics allow:
- copy selected text already remains native
- optionally export current chapter only when publication access and engine APIs safely provide text
- do not bypass DRM/encryption
- do not silently dump whole protected publications
- preserve Unicode/RTL correctly

## P1 — gestures and physical ergonomics

### Edge brightness
Opt-in:
- configurable left/right edge
- narrow dead zone
- activation threshold
- vertical gesture only after confidence
- haptic detents sparingly
- never steal system back gesture
- never steal horizontal page turns
- disable with TalkBack/touch exploration
- session-scoped brightness override
- restore system/auto brightness after timeout or leaving Reader
- handle orientation changes

### Edge font size
EPUB reflowable only:
- opposite edge from brightness by default
- live preview
- commit after gesture ends
- location-preserving reflow
- no fixed-layout/PDF use
- accessibility guard
- conflict tests with page turns and selection

### Pinch font scaling
Optional EPUB-only:
- must not conflict with PDF zoom or fixed-layout zoom
- threshold before owning gesture
- snap to readable scale range
- preserve location
- disabled under touch exploration unless proven accessible

### Edge-touch disable
Per-device/user option:
- protects curved displays / accidental edge touches
- never blocks Android navigation
- configurable edge width within safe bounds
- obvious escape path

### Long press
Current legacy navigator input does not safely expose long-press ownership.
Do not add a transparent touch overlay that breaks native selection.
Revisit only if the current Readium API provides safe duration/gesture ownership.
If safe API appears:
- configurable long-press action/timing
- direct highlight shortcut optional
- native handles remain accessible
- selection always has a fallback path

## P1 — typography and appearance

Current controls are strong; extend only where quality justifies it:
- local font import from user-selected files
- validate font format before accepting
- app-private copy, no broad storage permission
- per-book font substitution map
- publisher fallback
- delete/revoke imported font safely
- italic/slant control only if renderer supports it cleanly
- theme import/export only as Veil-owned settings schema, not copied competitor themes
- per-book vs global appearance overrides with clear reset/inheritance
- E-Ink mode:
  - high-contrast static paper
  - static paged default
  - no blur/translucency
  - no atmospheric loops
  - minimal invalidation
  - optional page flash policy only if measured/useful
- OLED mode must use true black where intended without crushing contrast
- appearance preview must match the actual renderer closely
- large-text accessibility must not clip controls

## P1 — selection, highlights, notes, bookmarks

Polish the entire flow:
- long selection handles remain native and stable
- highlight creation fast and reversible
- note creation from selection with minimal modal friction
- edit/delete/undo
- duplicate highlight behavior defined
- overlapping highlight behavior defined
- color/tint system accessible
- bookmarks can be copied/shared as references without exposing entire book text
- PDF bookmarks show page number
- Notebook can filter by book/type/date/tag if tags are introduced
- tap an annotation to return precisely
- previous-location return after annotation navigation
- search inside notes/highlights
- export notes/highlights to Markdown with correct RTL/Unicode
- backup/restore includes all durable annotation state
- large annotation sets remain fast

## P1 — Library / Archive quality

Target: 1,000 books remains fast and beautiful.

Implement/refine:
- Library + Shelves distinction
- smart shelves:
  - Continue Reading
  - Recently Added
  - Finished
  - Favorites
  - Series
  - Collection
- grid/list choice
- natural title/series sorting
- author/series/collection/status filters
- ratings if product value is clear
- bulk select
- bulk add/remove collection
- bulk mark finished/unread
- remove from library with clear file ownership semantics
- recovery/undo where possible
- drag/drop reorder only if accessible fallback also exists
- long title handling
- mixed-language metadata
- RTL
- duplicate detection and merge-safe behavior
- cover extraction failures fail gracefully
- missing cover visual treatment
- metadata editing
- series index sorting
- search result highlighting
- empty/no-result/error states
- import progress states for large batches
- no UI freeze during cover/fingerprint backfill

## P1 — Search and navigation

Audit:
- library search
- in-book search
- notes/highlights search
- TOC navigation
- previous-location return
- deep links/external open

Quality:
- debounced but responsive
- cancellation-safe
- no stale results after book switch
- query persists only where useful
- keyboard actions work
- RTL query rendering
- large result sets virtualized
- result snippets safe and readable
- search navigation records previous location
- broken TOC entries fail calm

## P1 — statistics and reading memory

Build from durable session data, not fake counters:
- reading calendar
- daily/weekly/monthly/yearly views
- yearly comparison
- total engaged reading time
- books finished
- pages/locations only when meaningful
- annotation counts
- most-read days/times only if evidence is valid
- per-book history
- streak display without punishment
- allow clearing/resetting stats with explicit confirmation
- restore stats through backup
- timezone/day-boundary correctness
- exclude background/idle inflation
- charts accessible and not color-only

## P1 — backup, restore, import durability

Re-audit:
- backup → wipe → restore round trip
- rollback on failed restore
- schema/version validation
- malicious/corrupt ZIP/path traversal protection
- large backup memory usage
- duplicate imported publication behavior
- source URI revocation/missing-file behavior
- app-private publication guarantees
- user understands whether “remove” deletes Veil's private copy
- all W37+ settings are included where the backup contract intends preferences
- annotation/statistics/gamification durability

Consider optional local auto-backup only after the manual path is proven:
- user-selected SAF destination
- explicit schedule
- no cloud dependency
- rotation/retention
- failure notification without spam

## P2 — Threshold / Home

Make “Continue Reading” the hero.
Required:
- one-tap resume
- current chapter where reliable
- meaningful continuation cue
- recent books shelf
- quiet Path/rank sigil
- no dashboard overload
- no generic stack of rounded Material cards
- empty state teaches import immediately
- return ritual may add atmosphere but must never delay reading materially
- adaptive phone/tablet/foldable layout
- large text/RTL support

## P2 — Veil visual system and polish

Audit every major screen against the Design Constitution.
Refine:
- semantic colors/material roles
- editorial typography
- brass rules/separators
- iconography instead of emoji
- consistent corner families
- spacing rhythm
- touch target consistency
- layer depth
- restrained mist/atmosphere
- empty/loading/error/restored states
- microcopy consistency
- no one-off components when a system component should exist
- remove remaining stock-Material-looking surfaces where they weaken identity
- keep Reader intentionally sparse
- Archive/Castle may be richer
- quality tier must degrade atmosphere before function
- no expensive blur on lower quality tier

## P2 — Castle / Ritual / Sanctum

Do not let gamification contaminate Reader.

Refine:
- Castle as a coherent map/place, not a vertical menu
- locked rooms visible as silhouettes
- Grand Library / Ritual Chamber / Observatory / Hidden Archive / Treasury / Inner Sanctum
- durable reading milestones visibly mutate the world
- no rewards for raw page tapping
- Path sigils
- rare intentional ceremonies
- skippable and reduced-motion compatible
- Quiet Mode / game visibility
- no punishment for missed days
- mysteries/discoveries from deterministic local reading evidence
- no loot boxes/FOMO
- progression survives backup/restore
- first-page/session XP evidence bug must stay fixed

## P2 — adaptive layouts and device classes

Quality matrix:
- compact phone portrait
- phone landscape
- large phone
- tablet
- foldable wide
- split posture where practical
- Chromebook/desktop-like window

Verify:
- no stretched phone UI
- dual-column/spread only when it improves reading
- correct insets
- IME behavior
- rotation preserves Reader location and modal state safely
- fold/unfold does not lose locator
- large touch/keyboard/mouse support where Android naturally provides it

## P2 — accessibility

Treat as release-blocking quality:
- TalkBack on all primary screens
- sensible focus order
- controls have meaningful labels
- decorative visuals excluded from semantics
- 48dp targets
- scalable text
- no clipped essential actions
- contrast on all themes
- non-color state cues
- reduced motion removes translation/ambient loops, not merely shortens them
- touch exploration disables gesture ownership where needed
- focus guide never becomes a semantic blocker
- hardware mappings remain optional
- error messages are actionable
- dynamic content announcements are restrained
- keyboard/DPAD where feasible on large devices

## P2 — performance and memory

Measure before optimizing.

Critical benchmarks:
1. cold launch
2. warm launch
3. open Threshold
4. scroll 1,000-book library
5. search/filter 1,000 books
6. import EPUB
7. open EPUB
8. first page render
9. mode change
10. open large PDF
11. PDF zoom/pan
12. open large Notebook/Archive
13. Castle navigation

Build:
- app-specific Baseline Profile
- startup profile where useful
- macrobenchmark that works on current platform
- release/R8 benchmark variant
- TTID/TTFD
- frame timing/jank
- memory around large PDF/image workloads

Investigate historical benchmark failure “Observed no renderthread slices”; make metrics robust rather than suppressing real failures.

Performance rules:
- no unnecessary recomposition from locator updates
- no unbounded coroutine/listener growth
- image/cache limits
- no main-thread file hashing or large JSON work
- paging/virtualization for large lists
- deterministic atmosphere work; avoid per-frame allocation
- low-tier device quality degradation affects decoration first

## P2 — import / format support

Core first: EPUB + PDF quality must remain highest.

Then evaluate:
- CBZ/CBR via Manga track
- TXT/HTML ingestion
- TXT chapter regex presets and user-defined regex
- encoding detection
- ZIP filename encoding
- FB2 only if a maintained safe parser fits architecture
- AVIF for comics if image pipeline supports it

Do not add formats merely to inflate a checklist.

## P3 — optional text transformation tools

Only after core Reader is green:
- user-defined name/text replacement rules for EPUB
- preview/compare before applying
- never mutate source publication
- per-book toggle
- reversible rules
- safe Unicode/RTL
- no replacement inside metadata/links unless explicitly designed

## P3 — Manga / comic track

Keep outside core Reader release-critical path until Reader is world-class.
When resumed:
- CBZ/CBR local first
- webtoon/continuous vertical mode
- dual-page landscape/spread
- RTL comics
- AVIF if supported
- extreme image sizes / subsampling
- durable progress
- offline cache integrity
- no live source instability allowed to regress local reading

## P3 — optional sync, only after 1.0 core stability

Do not introduce cloud sync during core hardening.
Future design:
- optional account
- progress/highlights/notes sync
- conflict resolution
- offline queue
- end-to-end durability semantics
- privacy-first
- explicit sign-in/out
- local reading never blocked by account/network
- export path always available

## Release engineering gates

Before calling the app 1.0-ready:
- real green unit tests
- instrumentation evidence
- Room migration fixtures
- backup/restore evidence
- release build with R8/resource shrinking
- verify Readium/Room keep rules
- ABI strategy documented
- AAB packaging verified
- Baseline Profile packaged if measured useful
- startup profile packaged if used
- signed release reproducibility
- no secrets in repo
- Play internal/closed test readiness
- crash/data-loss/accessibility P0 blockers = zero
- package size explained by user value
- no dead assets/dependencies
- privacy/support/store metadata ready when public release is scoped

## Required test matrix for every relevant Reader slice

Test combinations:
- PAPER / SEPIA / DUSK / OLED
- Paper Curl / Slide / static paged / scroll
- LTR / RTL / mixed script
- portrait / landscape
- compact / wide / large
- normal / large font
- reduced motion on/off
- TalkBack/touch exploration on/off where automatable
- fresh install / restored settings
- process recreation
- background/foreground
- rapid close/reopen
- malformed persisted preferences
- low-memory-like recreation where possible

For PDF:
- default zoom
- zoomed
- pan
- internal link jump
- bookmark
- rotation
- large file

## Definition of done for every slice

A slice is not done because it compiles.

It is done only when:
- user goal works end-to-end
- no fake renderer capability
- state survives recreation if it should
- failure states are calm
- accessibility impact checked
- RTL impact checked
- large text checked
- reduced motion checked
- interaction ownership explicitly documented
- regression tests added
- unit/build passes
- instrumentation compiled/run as environment allows
- performance measured if performance is part of the claim
- documentation/gap matrix updated
- PR remains small enough to review/revert

## Execution priority

Do work in this order unless new evidence changes the order:

1. Get W39 build/test green in Codex Cloud.
2. PDF annotation capability audit and PDF reliability fixes.
3. TTS.
4. Auto-scroll.
5. Dictionary routing.
6. Reading time remaining.
7. Annotation/Notebook workflow polish.
8. E-Ink + local font import/replacement.
9. Edge brightness/font gestures and brightness restore.
10. Chapter pagination estimates.
11. Reading calendar/year statistics.
12. Library 1,000-book polish.
13. Threshold/Home refinement.
14. Cross-realm design-system cleanup.
15. Adaptive/foldable/tablet pass.
16. Accessibility release pass.
17. Performance/Baseline Profile/startup hardening.
18. Castle/Ritual/Sanctum polish.
19. Import/format expansion.
20. Manga track.
21. Optional sync only after 1.0 core is stable.

## Autonomous behavior

Do not stop for ordinary implementation choices.
Use the best reversible option consistent with existing architecture.
Stop only for:
- destructive data migration without a proven rollback
- paid external services
- secrets/credentials
- legal/licensing uncertainty
- fundamental product scope change
- engine replacement

At the end of each slice report:
- branch / PR
- files changed
- user-visible behavior
- bugs found
- tests run and exact results
- remaining risks
- next recommended slice

The quality bar is not “better than the current build.” The bar is a coherent, durable, accessible, fast, premium Reader that can stand beside the best reading apps while keeping Veil's own identity.
