# Veil Reader 0.6.0 — Release Candidate

## What this release completes

Veil Reader 0.6.0 turns the 0.5 reader prototype into a build-verified Android release candidate and closes the last placeholder Castle rooms.

### Android build foundation
- Uses resolvable AndroidX / Compose versions aligned with Readium Kotlin Toolkit 3.3.0.
- Adds JitPack resolution required by Readium's open-source PDFium adapter.
- Fixes nullable publication-title handling for the Readium 3.3 API.
- Uses `LocalActivity` for the reader host instead of casting `LocalContext`.
- Explicitly opts in to the Flow preview API used for debounced locator persistence.

### Reader core
- Real EPUB and PDF import into app-private storage.
- Readium-backed EPUB/PDF rendering.
- Persisted reading position.
- Animated page navigation for paginated EPUB.
- Continuous-scroll option.
- Paper, sepia, dusk, and OLED reading themes.
- Font size, line height, publisher-style controls.
- Table of contents, bookmarks, highlights, notes, and saved-locator navigation.
- Reading-time inactivity protection and paced-page anti-farming rules.

### Library and Archive
- Search, filter, sort, favorites, metadata editing, and named collections.
- Hidden Archive across highlights and notes.
- Open a saved passage back at its locator.
- ZIP backup export and validated in-app restore.
- Restore stages and validates the archive, blocks unsafe ZIP paths/oversized payloads, rewrites publication URIs, and rolls preferences back if a commit fails.
- Markdown notebook export.

### Paths and progression
- Six original reading Paths with rank progression and Path-specific rituals.
- XP, streaks, quests, achievements/sigils, and Castle tiers.
- User-selectable daily reading goal (10, 20, 30, or 60 minutes) drives the reading quest.
- Advancement still requires Path ritual behavior; raw XP alone cannot advance rank.

### Castle completion
- Every awakened room is enterable.
- Treasury: equip durable milestone sigils on a persistent display pedestal.
- Inner Sanctum: select persistent Castle titles earned from rank and sigil milestones.
- Final title unlock requires both end-of-Path progression and the five core sigils.

## Verification gate
This release must pass the repository Android workflow before merge:
1. Reading-policy regression suite.
2. Debug unit tests.
3. Android lint.
4. Debug APK assembly.
5. APK artifact upload.

Physical-device behavior, accessibility review, backup round-trip on a real device/document provider, and store-release signing remain separate release gates and must not be inferred from a green CI build.
