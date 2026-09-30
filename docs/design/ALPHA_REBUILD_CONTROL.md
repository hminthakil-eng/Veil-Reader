# Alpha Rebuild Control — Veil Reader

Status: ACTIVE
Branch: `alpha/arena-rebuild-v2`
Evaluation owner: Alpha
Arena skill: `.claude/skills/arena/`

## Mission
Rebuild the presentation and information architecture until Veil Reader matches or exceeds the locked Grayfog references, while preserving the advanced reading, memory, persistence, accessibility and offline systems that already work.

## Source hierarchy
1. User-locked visual references: Grayfog Library, 10-screen target board, Veil Reader brand mark.
2. `docs/design/GRAY_FOG_ARCHIVE_V1.md`.
3. Verified advanced implementation on `BUILD-WORKMODE-GRAYFOG-PERFECTION`.
4. Older integration branches only as recovery/reference material.

The old `integration/reader-rc-v2` branch is not a rebuild base. It is hundreds of commits behind the advanced Grayfog branch.

## Arena evaluation gate
Every rebuild slice is reviewed with the installed Arena rubric:
- Correctness — 30
- Completeness — 25
- Specificity — 15
- Robustness — 20
- Clarity — 10

A fatal functional/accessibility/data-loss regression fails immediately regardless of visual quality.
A slice is not eligible for merge unless its evidence is strong enough to score >= 90/100 under the rubric.

## Additional Veil gates
- No loss of Readium/PDF behavior, durable location, highlights, notes, bookmarks or imported-book access.
- Reader/Sanctuary remains the least decorated realm.
- Page-turn and scroll remain independent concepts.
- Mobile primary navigation is reading-first; world/gamification remains reachable without occupying every primary destination.
- Persian/Arabic shaping and RTL are first-class, not post-polish.
- 48dp practical touch targets remain the default.
- Reduced-motion behavior remains available.
- No nested-card / gradient / glass / shadow soup.
- Existing advanced memory systems are reused, not recreated.

## Visual acceptance
### Threshold
- Environment art and content read as one continuous scene.
- Continue-reading folio belongs to the environment rather than floating as a generic Material card.
- One-tap continuation remains dominant.
- Recent books and memory whispers are subordinate to the current book.

### Grayfog Archive
- Retrieval remains fast: search, shelf filters, sort, grid/list.
- The archive feels spatial and authored rather than a generic library grid.
- Publication covers remain the strongest repeated visual element.
- Metadata density does not bury titles or progress.

### Reader / Sanctuary
- Hidden chrome leaves only publication/paper.
- Revealed chrome is quiet and architectural.
- Appearance, Notebook and Bookmark remain one gesture away.
- PDF direct manipulation remains native.

### Notebook / Settings / Book Detail
- Use shared material/typography DNA.
- Avoid decorative density that competes with content.
- Preserve factual memory/history; never invent missing dates or semantics.

## Evidence policy
A code-only slice may be marked IMPLEMENTED but never DEVICE-GREEN.
Final GREEN requires device/emulator visual proof plus interaction/accessibility checks.
No APK is produced merely to create progress evidence while the rebuild is still visually unfinished.

## Current rebuild decisions
- Base moved to the advanced Grayfog branch rather than the obsolete integration branch.
- Arena installed on this rebuild branch.
- Mobile dock rebuilt to Home / Library / Notebook / More; Castle and Path remain reachable through world flows.
- Threshold composition is being fused into a continuous art → resume folio → recent shelf scene.
- Previously reported HighlightMemory and MemoryAtlas precedence bugs are already fixed in the advanced base and are preserved.
