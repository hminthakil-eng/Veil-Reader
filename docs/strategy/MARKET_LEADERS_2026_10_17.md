# Veil Reader — market-leader comparison and rebuild brief

Evidence date: 2026-10-17. Source checkout: `3f0d92e8b`. Working branch: `design/market-leaders-rebuild-20261017`.

## Product promise

A local-first, offline-first Android reader that is effortless on the first book, deeply configurable when asked, faithful to original EPUB/PDF content, and distinctive through restrained Grayfog / archival-mystery storytelling. The UI must evoke *Lord of the Mysteries* without borrowing protected text, images, characters, logos, or assets.

Never trade reading clarity, progress durability, accessibility, or input latency for atmosphere. Gaming and investigation stay optional, never interrupt book reading, and never invent book-derived data.

## Real competitor reference matrix (public documentation, not device-tested comparisons)

| Competitor | Documented strength | What we can learn | Veil differentiation |
|---|---|---|---|
| Apple Books | Curl/Fast Fade/Scroll, themes, per-page formatting, reading goals, Line Guide | Visual restraint, reading-surface quality, fast appearance access, affordance clarity | Android-native physical paper + more controlled customization without visual noise |
| Amazon Kindle | Consistent distraction-free reading, accessibility, gestures, volume-button turns, continuous scrolling | Recoverability and attention hierarchy | Offline imported files without store lock-in; optional game world outside reader |
| Moon+ Reader Pro | Extensive gestures, customizable controls/fonts/themes; PDF forms, annotations, TTS, auto-scroll and stats | Power-reader capabilities | Keep simple default and disclose advanced settings progressively |
| ReadEra | Focused local file/library reading benchmark | Fast no-account import and retrieval | Premium identity with no forced account or marketing |
| Readest | Cross-platform reading, split/parallel reading, sync, annotations, TTS, OPDS and translation | Research / multilingual / studying workflows | Native Android performance, reliable offline and privacy-first optional smart layer |
| Kobo Books | Background modes, spacing, margin, page turns, dictionary, synced place and audiobooks | Reading/listening continuity | Optional import-first workflow and third-party catalog compatibility |

Important: feature assertions are from official public documentation, not first-hand testing of current competitor APK builds. Avoid inventing metrics, ratings, preferences, or winning claims.

References (retrieved 2026-10-17):
- Apple official: https://support.apple.com/guide/ipad/read-books-ipadc8494b6b/26/ipados/26
- Kindle accessibility: https://www.aboutamazon.com/news/books-and-authors/kindle-accessibility-features-for-all-readers
- Moon+ Reader Pro: https://play.google.com/store/apps/details?id=com.flyersoft.moonreaderp
- Readest product/docs: https://www.readest.com/ and https://www.readest.com/docs
- Readest license: https://github.com/readest/readest (AGPL-3.0)
- Kobo: https://www.kobo.com/us/en/p/apps
- Android adaptive/foldable: https://developer.android.com/develop/adaptive-apps/guides/foldables/make-your-app-fold-aware
- Android Baseline Profile: https://developer.android.com/develop/ui/compose/performance/baseline-profiles
- Android macrobenchmark: https://developer.android.com/topic/performance/baselineprofiles/measure-baselineprofile
- Harism paper curl: https://github.com/harism/android-pagecurl (Apache-2.0; check upstream NOTICE/assets separately)

## Current source evidence (NOT feature-quality proof)

- Four navigation choices: `app/src/main/java/com/veilreader/app/domain/Models.kt:135`.
- Distinct paper/slide/static listeners: `app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt:2257,2291,2324`.
- Distinct paper and slide overlays: same `ReaderScreen.kt`, around line 3100.
- Paper eligibility and publication fallback: `ReaderAppearancePolicy.kt:43+`.
- GPU rollout debug/release gates: `MaterialPageEngine.kt:46+`, `VeilFeatureGates.kt:34`.
- Real EPUB LTR/RTL instrumented curl test exists in `NativePaperRealReaderInstrumentedTest.kt` but has NOT been run in this exercise.
- Baseline and frame benchmark source exists in `benchmark/src/main/java/com/veilreader/benchmark`, yet recent measured startup / turn-frame distributions are NOT available.
- Local repository contains EPUB/PDF, library collections, highlights/bookmarks, and TTS implementation paths; actual product usability/voice quality remains unverified.
- This scoped source scan did not identify a finished OPDS/catalog integration, full translation workflow, or parallel-reading UI. These are candidate gaps, not claims of total repository absence.
- Visual RED source audit from 2026-10-17 finds HOME and Investigation Desk source-to-Figma mismatch; prior APK appearance was rejected by user and actual installed screenshots are still required.

## Priority gaps with acceptance evidence

| Order | Initiative | Competitor baseline | Product requirement | Required proof |
|---|---|---|---|---|
| P0 | Reader reliability | Kindle | Zero lost/duplicate position under rapid gesture, interrupt, process death and reflow | Unit races plus instrumented process restart / real EPUB locator |
| P0 | Authentic paper curl | Apple Books | True deforming page, correct front/underside ink, coherent lighting, cancellation, LTR and Persian RTL; never relabel slide | Real page capture during drag, reverse, release; video + frame timing |
| P0 | Visual system / home | Apple Books, Kobo | Editorial premium archive, meaningful hierarchy, accurate dynamic book data, dark/light, large fonts | Approved design compared side-by-side to actual Compose screenshot |
| P0 | Library retrieval | ReadEra | No account needed, open/import book reliably, fast resume, accurate filters/collections | Device import, corruption, duplicates, offline, permission denial tests |
| P1 | Appearance and gestures | Moon+ | Two-level quick/advanced, one gesture to adjust, per-book/global, bounded gesture ownership | Task completion, focus / TalkBack and RTL checks |
| P1 | PDF + typesetting | Kobo, Readest | Native PDF zoom ownership, fit/rotate, no clipping; rich EPUB typography, footnotes, search | Fixture screenshots on phone/tablet; progression continuity |
| P1 | TTS/listening | Readest, Kobo | Stable voice per character option, sane voices, screen-off continuation, synced locator; offline default | 30min endurance, interruption/resume, battery and audio evaluation |
| P2 | Study tools | Readest | Parallel reading, notes, dictionary, OPDS/Calibre and translation where licensed and useful | Complete tested opt-in task flows; privacy gate |
| P2 | Secret archive/gamification | Unique Veil identity | Camp-to-castle narrative world, rewarding but invisible while actively reading | Reading-session usability and opt-out tests |

## UX architecture decisions

1. **Library**: continue-reading first, then quick retrieval, then collection tools; one primary call to action; no fixed fictional progress or false statistics.
2. **Reader**: the publication and locator are authoritative. Overlays disappear by default. Settings are a compact sheet with live preview; deeper settings are explicit secondary navigation.
3. **Reader modes**: keep independent user-facing PAGED, PAPER CURL, SLIDE, SCROLL; implementation may model PAGED/SCROLL as layouts and NONE/SLIDE/CURL as transition choices, but never silently remap a requested mode without a reason.
4. **Grayfog**: ink, archival parchment, restrained brass, quiet asymmetric composition, editorial serif display balanced against legible body text. Avoid generic gold Material cards, dense ornamental borders, exaggerated animation, and inaccessible mystic jargon.
5. **Game/Investigation**: explicit opt-in, privacy-sealed clue cards, source-grounded claims only, no fabricated stats, predictions, or research graph nodes.
6. **Adaptive**: compact phones (320–430dp), tablet/foldable medium/expanded, safe insets, hinge and IME avoidance, RTL/LTR and 200% system font tests.
7. **No rewrite-by-default**: retain Room, Readium, TTS service and validated locator logic; replace bounded presentation pieces behind a reversible flag. Preserve open worktree and original code.

## Licensing and reuse

- Use commercial/proprietary apps only for behavioral and accessibility research; do not copy graphics, code, screenshot assets, characters or trademarks.
- Readest's primary repository is AGPL-3.0: use its behavioral patterns for clean-room implementation; direct code reuse requires legal review and source-sharing compliance.
- Harism OpenGL curl is Apache-2.0, already referenced in Veil; honor NOTICE and image exceptions and test actual page pixels.
- Compare library/license updates independently; do not upgrade Android, Compose, or Readium blindly to untested versions.

## Honest launch criterion

No score of "beats Apple Books/Kindle" until externally compared device builds complete identical reading tasks, blinded human visual assessment, power-reader capability tests, reliability gates, real performance measurements, and user acceptance. A generated proposal, compile pass, static scan, mockup, and simulator cannot substitute for on-device reader QA.

## Latest Readium integration check (verified in our source)

This branch already declares `org.readium.kotlin-toolkit:*:3.4.0` in `app/build.gradle.kts:141-144` (shared, streamer, navigator, PDFium adapter). **Do not propose a blind Readium upgrade.** The upstream 3.4.0 release on 2026-09-15 introduced paginated PDFium scrolling preferences, revised defaults and a position-reporting bug fix; relevant here are *regression tests* for our PDF mode/locator handling, not replacing Readium.

Source: https://blog.readium.org/release-note-kotlin-toolkit-version-3-4-0/ . See also 3.2.0 animated EPUB transitions: https://blog.readium.org/release-note-kotlin-toolkit-version-3-2-0/ . Do not confuse Readium's built-in animated navigation with Veil's physical GPU PAPER CURL; their visual/gesture ownership needs separate testing.

## User community pain signals (anecdotal; not measured market share)

- January 2026 Android reader comparison identifies **RTL direction and gesture consistency**, comic/manga edge margins, per-book settings and cloud-sync reliability as recurring deal-breakers in that author's use. Source: https://www.reddit.com/r/ereader/comments/1q4l5db/a_comparison_of_all_the_paid_as_well_as_free/
- October 2026 discussion values Moon+ Reader's customization and TTS but praises Readest's cross-platform reach. Source: https://www.reddit.com/r/androidapps/comments/1wzlbcp/moon_reader_or_alternative/
- June 2026 requests emphasize integrated dictionary, reliable selection/highlights and calm ad-free reading without jumping pages. Source: https://www.reddit.com/r/ereader/comments/1udjssa/best_ereader_app_for_androidiphone_coming_from/
- October 2026 reader discussion asks for simple, full-featured PDFs and reports mixed experiences. Source: https://www.reddit.com/r/androidapps/comments/1wzkkwl/what_is_your_favorite_ebook_reader_especially_for/

These are individual reports and should inform which usability tasks we test; they do not establish universal competitor defects. Prioritize *RTL page gestures, one-thumb controls, consistent per-book settings, error-free local imports and safe reading position* ahead of speculative AI feature counts.
