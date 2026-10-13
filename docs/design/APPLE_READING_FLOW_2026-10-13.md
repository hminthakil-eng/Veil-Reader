# Apple-guided native reading flow

Base: canonical Reader `5fdaa496c2866bb30330364db2ae420054a1ab1f` (#509).
Candidate: `design/apple-reading-flow-20261013`; isolated and reversible.

## Outcome and scope

The old Appearance dialog covered the publication with a complete scrolling form.
Its fixed dark surface also received dark text from LIGHT app appearance. The
Library placed Resume after its complete collection and four secondary sections.

This candidate uses the existing Material 3 sheet and editor, places the most
recent active volume before Library retrieval/collection, pairs native semantic
foreground and surface colors, marks selections visibly, and adapts the four
Reader toolbar actions to two columns for narrow/large-text contexts. Functional
Reader chrome remains opaque and publication colors remain independent.

Reuses the existing Apple adapter and dock contrast work from #511. No new Apple
fonts, icons, artwork, executable dependencies, or platform-specific chrome.
Native page engines, storage, locators, TTS ownership and production Paper rollout
are not changed.

## Sources actually read

- `dickwu/apple-design-skill` at `904b0eedc7cc778152f545506075d5bb5219ce77`:
  SKILL.md; accessibility, color, layout, typography, designing-for-ios, buttons,
  sheets, settings; cross-platform translation. Android receives principles and
  foundations; Material/Compose and Android back/gesture ownership prevail.
- `Elevatormusic/apple-hig` at `e00f1af16c71f4122e7c964563d2f563fd490d7e`:
  router, universal guidance and design-reviewer workflow. Independent static
  review was used for the changes.
- Current [Apple Books task guidance](https://support.apple.com/guide/iphone/read-books-iphc1af7c57/ios):
  in-book theme, font size, brightness, style and deeper customization controls.
- Project MASTER, Library/Reader page overrides and current Compose theme.

The design choices follow `layout.md / Visual hierarchy`, `settings.md /
Task-specific options`, `typography.md / Supporting Dynamic Type`, `color.md /
Inclusive color` and `sheets.md / Best practices`. This is an independent native
implementation; it is not a claim of market superiority or iOS compliance.

## Source-backed findings and changes

| Finding at base | Native change | Verification needed |
|---|---|---|
| Appearance title in LIGHT on fixed dark surface: ~1.12:1 | Semantic opaque sheet surface + matched foregrounds | LIGHT pixels and physical readability |
| Resume after full collection; absent from INDEX | One compact Resume card before collection in all unfiltered views | Same book opens; search hides unrelated resume |
| Full-screen form; return at scroll tail | Height-bounded live-book sheet; persistent return action | Readium viewport and locator preserved; back/outside/swipe |
| Modes and menu choices rely on color alone | Native radio marks plus parent selected semantics | Selection, enabled state and TalkBack traversal |
| Small PAPER metadata ~3.43:1 | Explicit muted ink `#685B48` on opaque `#F0E4CC`: 5.25:1 | Captured composited pixels / device |
| Four fixed columns at large text | Existing dense-choice policy drives a wrapping 2-column toolbar | Compact, 200%, landscape, native touch |
| LIGHT semantic secondary/accent below 4.5 on raised surface | Darker role tokens, minimum 4.91/4.84:1 on authored light surfaces | No translucent-text regressions |

Keep Grayfog architecture in bounded dark imagery; LIGHT uses paper surfaces.
Keep cover artwork, Persian shaping, script-sensitive titles and the single
Library lazy viewport. The compact console omits its synthetic sample preview;
the actual publication stays behind the overlay. Advanced controls remain
available. Draft disposal and renderer-settle dismissal still own pending edits.

## Verification record

Executed locally before publishing:

- `sh tools/test-policy.sh`: PASS, 39 checks.
- Canonical Paper source guard: PASS; six guard tests PASS.
- `python3 tools/check_veil_ui_contrast.py`: authored opaque role ratios checked.
- `git diff --check`: PASS.

Local Gradle bootstrap was attempted but its Java network connection could not
fetch the pinned distribution. This is not a compile result. Android JVM/lint,
emulator instrumentation, screenshots and performance remain pending CI.

`AppleReadingFlowTest` and `ApplePersianReadingFlowTest` exercise real production
compositions with fictional records: 48-book resume reachability/opening in all
three views, suppression during search, compact editor theme preservation, and
return reachability after advanced scroll, LIGHT English 100% and Persian 200%.
Their native Skia captures are written into the existing `grayfog-cloud` artifact.
These are UI specimens, not actual book-page or physical-device evidence.

The existing Android, storage/UI instrumentation, durability and performance
workflow base filters now cover stacked changes to the canonical Reader branch.
Their gates/budgets are unchanged. Do not infer PASS until actual run receipts.

## Acceptance still required

Physical English/Persian, LIGHT/DARK/high contrast, 100%/200%, compact/landscape/
tablet, TalkBack order, keyboard/back/outside/swipe dismissal, pending slider
release, all 12 directed mode changes, selection ownership, locator after reflow,
and 60/120 Hz responsiveness. Owner comparison with current commercial readers
is still required before declaring better overall UX. No public release or main
merge is authorized by this candidate's build result.
