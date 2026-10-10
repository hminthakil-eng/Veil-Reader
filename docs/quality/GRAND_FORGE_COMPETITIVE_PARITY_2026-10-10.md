# Veil Reader — Competitive Parity & Beyond Execution Ledger
Date: 2026-10-10
Owner: Veil Reader Grand Forge
Status: IMPLEMENTATION IN PROGRESS — this document is not a release certificate

## Evidence and precedence

- Runtime integration authority: `grand-forge/p0-kindle-reader-quality-20261008` (PR #429). Default `main` is not a reliable development baseline.
- Current visual foundation: `grand-forge/visual-foundation-reader-components-v1-20261009` (PR #436).
- Current Paper fix: PR #442 on its own feature branch; verification PRs #444/#445 are not merge targets.
- Reference competence matrix: PR #441, based on the supplied Moon+ Reader Pro 10.7 and Kindle 8.157 APKs. Static APK evidence suggests components; it **does not** certify live user features.
- Repeatable non-executing APK fingerprinting: PR #448.
- Current W2 partial implementation: platform Translate action in the native Readium selection toolbar (this branch).
- Upstream interface: Readium Kotlin Toolkit 3.4.0 (BSD-3-Clause) already used by Veil; evaluate other open-source libraries individually under their own licenses.

## Rules (non-negotiable)

1. Preserve existing Reader/Storage/TTS owners. No duplicate managers or divergent reading-location sources.
2. Feature present does not equal completed. GREEN requires passing product UX, accessibility, persistence, crash recovery and real device evidence, not merely files/classes/tests.
3. Clean-room reconstruction of competitors: compare observable interaction sequences, states, errors and performance. Reuse compatible-license open-source source; **never** transplant proprietary Kindle/Moon+ classes, binaries, copyrighted artwork, DRM logic, fonts, keys or services.
4. Preserve four independent reading modes: PAGED, PAPER CURL, SLIDE, SCROLL. Never silently downgrade CURL to SLIDE.
5. Every wave is small, reversible, unit and instrumentation-tested, with exact source SHA, CI results and a physical-device checklist. Never merge a failing P0 gate.
6. Respect offline-first/privacy: external provider dispatch is **explicit**; translation and sync never silently upload excerpts.
7. UI acceptance combines readable Sanctuary canvas with restrained Grayfog identity; maintain Persian/Arabic shaping and RTL tests.

## Priority register

| Wave | Reader capability | Confirmed current evidence | Deficiency to resolve | Gate / observable proof |
|---|---|---|---|---|
| P0 | Paper Curl first gesture | GPU Paper and readium gesture owners present; PR #442 | image/SVG cover crossing, visible deformation, release transaction | physical Pixel/Samsung drag optical recording; one committed turn; cancel, boundary, chapter crossing, rotate/reopen; no SLIDE impersonation |
| P0 | Paper rendering integrity | GPU epochs, PixelCopy fallback, software mesh, tests | hardware presentation and frame stability unverified | exact acquired frame epoch before navigation, no buffer races, no stale render after pause |
| P0 | Progress / checkpoint | PR #429 background & storage hardening | device process-death + long-term save verification | no lost locator/highlight/note across kill and app resume |
| W1 | TTS / Listening | PRs #420–#426; foreground checkpoint #432 | speech quality, voice choice, positioning and runtime capability | real audible device tests; voice/locale, pause/resume, notifications, focus interruption, chapter transitions and exact checkpoint |
| W1 | Neural TTS | gated local asset and service path | payload/security/speech quality not certified | license/provenance, sandboxed assets, offline install, recovery tests; no compulsory model download |
| W2a | Selection + annotation | native Readium selection; durable note/highlight tests | context tools, actions, PDF parity, export | user can select, highlight, annotate, find, export and reopen exact quote; TalkBack correct |
| W2b | Dictionary | existing LOOKUP hands text to external app | explicit provider selection / offline dictionary ownership | no hidden upload; offline definitions where available; language support; no-handler recovery |
| W2c | Translation | **this branch adds Android ACTION_TRANSLATE integration** | no internal translator, language pair selection, offline capabilities | text selection to installed translator on API29+; no silent web fallback; future internal provider behind capability checks |
| W3 | Typography | Reader appearance policy, font settings, RTL corpus present | hyphenation, per-language shaping, exact live preview | ligatures/diacritics/CJK, Persian RTL, text-size 200%, margins/line height, zero clipped glyphs |
| W3 | Themes / visual finish | Sanctuary tokens and Grayfog atmosphere PR #436 | insufficient device/contrast validation | optical screenshots, dark/sepia/paper/OLED, high contrast, motion reduction |
| W4 | Search within publication | locator and reader search components reported in #441 | hit excerpt fidelity, fast large-book search, jump history | exact search hit location, previous-location return and thousands-of-chapters corpus |
| W4 | Navigation + page history | Readium TOC and Veil locator policy | precise paragraph navigation, landmarks/page modes | linear and non-linear links, cover, footnotes, TOC, hardware keys, back stack, restore |
| W5 | Archive/library | local library + collections/metadata components | 1000+ books search, duplicate detection, series metadata | indexed retrieval latency and restoration; safe file permissions; deterministic sort/filter |
| W6 | Export / backup | local first; some backup artifacts | complete note/highlight/book state export and recovery | versioned backup, integrity/corruption repair, local-only default |
| W6 | Catalogs / OPDS / Calibre | Readium OPDS available; no complete Veil owner verified | integrated opt-in catalog browser | pinned open-source parser, auth handling, offline/no-network states, protected download targets |
| W6 | Optional sync | no accepted multi-device conflict protocol | explicit opt-in, merging notes/locators/collections | offline simultaneous edits + deterministic conflict rules and privacy disclosure |
| W7 | PDF | EPUB/PDF core present | selection, zoom, links, pages, annotation durability | real-world PDFs incl scanned/RTL; deterministic page/zoom/back state |
| W7 | Comics / archives | Manga/CBZ ingestion already exists | no duplicate CBZ renderer, incomplete archive coverage | sample CBZ/CBR corpus, zip-bomb path guards, layout/performance and legal reusable parser |
| W7 | FB2/MOBI/CHM/DJVU | competitor APKs contain hints; Veil support not certified | mature format renderer and maintenance review | each format independent test corpus + secure import; missing variants must remain unsupported |
| W8 | Accessibility | partial device and unit contracts | gestures-only controls, TalkBack and magnifier parity | focus traversal, 48dp hit targets, 200% text, high contrast, reduced motion on device |
| W8 | Gamification / Castle | custom progression world already exists | completeness, polish and calm reading integration | correct read-time accounting, fair progression, zero Reader interruption |
| W8 | Battery/thermal/performance | benchmark harness and traces | emulator metrics don't establish device smoothness | cold/warm startup, 60/120Hz frame traces, thermal and idle battery on physical devices |

## First delivered engineering slice — native selection Translate

Files on current branch:
- `ReaderSelectionTranslation.kt`: bounded, explicit ACTION_TRANSLATE (Android API >=29), no automatic online fallback.
- `ReaderSelectionActionMode.kt`: new native menu action; reused existing Readium selection ownership and cleanup.
- `ReaderScreen.kt`: launches only for current resumed Reader session and surfaces localized failure.
- `values/strings.xml`, `values-fa/strings.xml`: labels and failure messages.
- `ReaderSelectionTranslationTest.kt`, `ReaderSelectionActionModeTest.kt`: intent/action-menu safety and behavior tests.

**Non-goals:** This slice does not supply an embedded dictionary, offline translation models, a translated-text overlay, PDF selection parity, new libraries, network APIs or a new privacy consent flow. Installed translation applications may themselves use cloud services; Veil doesn't choose the service or initiate network requests.

## Immediate next engineering order

1. Keep P0 Paper visual gate RED until the exact #442 source passes animated drag/tap tests and physical-device optical acceptance. Do not rewrite its source ownership in this W2 branch.
2. Compile/test this W2 branch with exact-head Android CI, Robolectric action tests and on-device selection/TalkBack. Keep draft if any gap.
3. Extend W2 behind a narrow definition/translation provider interface, preserving existing LOOKUP. Do not claim offline dictionary without provider data and tests.
4. Run W1 audible voice QA and W3 multilingual corpus independently, then W4/W5. Drive W6-W8 only after core reading is dependable.

## Rejection rules

- No fake GREEN because a method exists.
- No copying or rebranding competitor proprietary implementation.
- No release or production merge without real device evidence.
- No duplicate Reader states or silent fallback.
