# Reuse gate — Sovereign/Absolute Perfection reader ideas (2026-10-15)

## Goal
Import useful _behaviors_, not the Sovereign Kotlin EPUB renderer, database, game counters, or material theme.
Readium remains the sole publication/navigator and reading-position authority. Room remains the sole
annotation source of truth; DataStore owns existing sensory/reading preferences.

## Reuse-first audit (main at 811f0d7d)
| Idea | Existing Veil capability | Decision |
|---|---|---|
| Rain / ambience | `SensorySettings` already has RAIN, LIBRARY and FIRE | REUSE; no duplicate AudioTrack |
| Focus ruler | `ReaderFocusGuideSettings` and persisted focus-mode controls | REUSE; no second overlay |
| Night brightness | `ReaderBrightnessControls` and native window brightness effect | REUSE; no uncontrolled full-screen dimmer |
| Paper curl | Readium + independent native Paper candidate PR #509 | KEEP SEPARATE; do not replace with chunked pager |
| Word Wise | OS external Lookup only | IMPLEMENT: small deterministic offline aid for selected text |
| Markdown notes export | Canonical Room highlights in ReaderNotebook | IMPLEMENT: user-initiated SAF CreateDocument export |
| Spoiler-safe X-Ray | No verified, book-sourced chapter occurrence/authoritative chronology | DESIGN/HOLD until book-scoped factual entries and monotonic read-locator locks |
| Decorative drop cap | EPUB layout owned by Readium and publisher CSS | HOLD; no DOM rewrite without real-book typography/RTL evidence |
| RSVP + bionic text transforms | Content/locator text fidelity at risk | HOLD; require accessibility and Readium selection proof |
| Per-book ambience | Existing global sensory state | HOLD; needs explicit inheritance and persistence model |

## Implementation boundaries
1. Native Readium selection ActionMode gets one additional `Word Wise` command in overflow.
2. Word Wise only renders definitions for known single Latin words from a tiny local glossary;
   unknown/multiword selection says it has no built-in definition. Lookup fallback is explicit.
3. No EPUB content injection, WebView scripts, independent annotation DB, session progress changes,
   or automatic network requests.
4. ReaderNotebook exports exactly the selected book's persisted notes to a Markdown file through
   Android's Storage Access Framework. No silent sharing, server or temporary export directory.
5. Both actions require explicit user intent, and are reversible by reverting this PR.

## Acceptance / verification (pending exact-head CI)
- Word Wise normalizations, unknown text, long/multiword and Unicode input tests.
- Markdown multiline, empty notes, cross-book isolation tests.
- Selection ownership regression suite; app resource key parity EN/FA; Android compile/lint.
- Human device QA: select text in EPUB, release ActionMode, popup, system Lookup fallback, Paper
  state unchanged, no page/scroll shift; notes export, reopen UTF-8/FA content, cancellation,
  documents-provider failure, non-EPUB reading and TalkBack.
- No production/product GREEN solely from unit tests. Review and merge only when gates pass.

**Main branch and user data are unchanged by this draft.**
