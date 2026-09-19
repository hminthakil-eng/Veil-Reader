# Veil Reader: production release gates

These are outstanding acceptance requirements, not features claimed as finished.

## 1. Prove the Android build

Resolve every pinned dependency, produce a debug APK, run lint and tests, add a verified Gradle wrapper, and pin the verified toolchain. Deliver a reproducible signed release pipeline with user-controlled signing credentials. Never include keys in source or chat.

## 2. Prove data durability

Move large shelves and annotations into a transactional database with upgrade tests. Implement atomic backup restore with validation, size limits, safe ZIP extraction, conflict handling and rollback. Test interrupted writes, missing files, process death and migrations from every released schema. A backup is only trusted after a demonstrated restore round trip.

## 3. Prove reading quality

Test EPUB reflow/fixed layout, large PDFs, encrypted and corrupt files, RTL, mixed scripts, footnotes, images, tables and nested contents. Fix configuration-change fragment restoration and navigation lifecycle edge cases. Verify system Back, process recovery, font resizing, landscape and resumed locators on physical devices. Add full-book search and real cover extraction.

## 4. Prove accessibility and performance

Test TalkBack, large text, contrast, focus, screen magnification and reduced motion. Profile a 1,000-book library and large annotation sets. Establish and measure startup, import, page-turn latency, memory and battery budgets on midrange Android hardware. Use measurements rather than promises.

## 5. Finish the progression design

Make completion credit reflect meaningful coverage. Test midnight/time-zone changes and long-session inactivity handling. Add user-controlled reading goals and optional game visibility. Keep ordinary reading and note access independent of rank. Implement actual Treasury/Sanctum interactions before presenting them as complete features.

## 6. Release to real readers

Run a closed test with varied reading habits and devices. Resolve crashes, data-loss issues and accessibility blockers. Prepare original app icon/cover treatments, onboarding, privacy policy, store screenshots and support flow. Add consent-based diagnostics only if needed. Cloud sync, manga, TTS and optional book-grounded AI should follow a stable core, each with its own validation gate.

## Immediate unblock

Connect the Veil Reader GitHub repository so the prepared workflow can build an APK and expose actual Android compiler/runtime errors. A source ZIP and passing policy tests alone do not establish app readiness.


## 7. Prove product and visual quality

A release is not accepted because it compiles or because requested features exist.
Run screenshot QA on every affected user-facing surface and verify:
- visual hierarchy
- spacing consistency
- typography
- iconography
- light/dark parity
- RTL
- large text
- empty/loading/error/offline states
- phone/tablet/foldable adaptation
- animation continuity and interruptibility

Compare the reader against Apple Books, Kindle, Kobo, Google Play Books, ReadEra and Moon+ Reader.
A visibly cheap, inconsistent or prototype-like screen cannot be GREEN.

## 8. Prove reader feature parity

Before a world-class reader claim, Veil must have validated paths for:
font family, font size, line spacing, margins, alignment/justification,
publisher defaults, themes/backgrounds, brightness, scroll/pagination,
page-turn style, bookmarks, highlights, notes, table of contents and full-book search.

Then add advanced accessibility and wide-screen capabilities:
font weight, word/character spacing, dyslexia-friendly font,
line guide/reading ruler, orientation lock, one/two-column layout and TTS/read-along.

## 9. Preserve a Veil identity without hurting usability

Use standard Android interaction patterns and familiar controls.
Branding is restrained and content-first.
Fun belongs around reading, not on top of reading.
No decorative system may reduce legibility, touch reliability, performance or accessibility.
