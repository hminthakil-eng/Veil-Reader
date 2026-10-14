# Veil Reader — Premium Reader × Living World (collaborative build)

Worktree: `E:\EyadStudio\Projects\Veil-Reader-Premium-20261014`
Branch: `design/premium-reader-world-v1-20261014`
Base: `e37869bb68e0497b5cbb9ca9f9b56c4e53286020`
Date: 2026-10-14

## Product contract
Veil Reader is first a fast, offline-first, accessible, enjoyable ebook/PDF/manga/audio reader; its secondary world is a voluntary, genuine-reading-powered progression game. Grayfog's deep archive, antique brass, midnight ink, and parchment are the design identity. Premium means clarity, not clutter.

The existing Room/Readium/Settings/TTS and the separate PAGED/PAPER CURL/SLIDE/SCROLL contracts continue to own their data and input. This redesign must not overwrite or reset books, bookmarks, notes, highlights, reading progress, or game saves.

## Engineering partnership
- ChatGPT: product constraints, source integration, code review, tests, Git worktrees, release guards.
- Gemini 4 Argon High desktop: **write actual Kotlin/Compose source**, algorithms, complex motion and world rendering, suggest targeted tests, critique failures and produce patch revisions.
- Both must work from real symbols, file excerpts, exact diffs and test feedback. Neither partner should claim a real run it did not execute.
- Gemini desktop may propose source; a verified repository patch or file write is still necessary before it counts as integrated code.

## Current prototype and ownership
- `PremiumReadingHomeScreen.kt`: authored new, wired only from `VeilTab.READING` via `VeilApp.kt`; native Compose UI renders actual resumed title, cover, progress, recent volumes, reading evidence and castle link.
- `ReadingHomeProgression.kt`: reused existing tested no-write reading-home projection (CAMP / HUT / HOUSE / CASTLE).
- Native art module `PremiumWorldProgressionArt.kt`: assigned to Gemini for complete code generation, then integrated and compiled in this branch.
- Existing premium gateway/background may be replaced only after stable alpha/contrast/RTL/readability verification.
- Existing Reader itself is unchanged in this iteration.

## Mandatory gates
1. Product: first tap resumes real book; archive/world actions route to the actual screens; no invented XP or resources.
2. Motion: animations are short/restrained, reduced-motion supported and no per-frame allocations that could compromise low-end devices. Paper gesture performance stays independent.
3. Accessibility: meaningful controls reachable by TalkBack, all decorative canvases hidden from semantics, RTL Persian layouts, font scaling.
4. Source: Kotlin compile, localized resource compilation, relevant JUnit/Robolectric tests, Android UI/device checks before promotion.
5. Visual: evaluate current installed app (not just mockups), comparison against reading competitors, screenshot acceptance across light/dark and narrow/wide.
6. Safety: draft PR only after evidence; never merge/roll out unverified functionality; preserve native paper #509 and #517 and the existing release gates.

## Sequence
Foundation (reading home) -> motion/art (Gemini source) -> robust library retrieval -> reader control polish -> living world interactions -> evidence-driven game rewards. Only the first foundation has had a successful local Kotlin compile so far. A full instrumented/physical/UI review has **not** been completed for this redesign.
