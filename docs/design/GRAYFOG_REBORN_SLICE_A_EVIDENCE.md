# Grayfog Reborn — Visual Slice A Evidence Gate

Status: SOURCE CHANGED / COMPILE UNVERIFIED / VISUAL RED
Branch: design/market-leaders-rebuild-20261017
Date: 2026-10-17

## Scope implemented in Kotlin/Compose

- Quiet archival header with static engraved geometry using theme-derived colors and actual localized copy.
- One-current-book surface, original BookCover, real Book metadata and unchanged onOpenBook callback.
- Revised recent-books shelf, while retaining original Book object click routing.
- Investigation Desk cards: default sealed, transient reveal, lifecycle ON_STOP reseals content.
- Unopened books do not invent 0% reading progress.
- Optional Castle, empty Library, passage navigation and existing Reader engine remain untouched.
- Tests added for RTL at 200% font and unopened-book progress. Not yet executed.

## Exact next quality gates

1. With adequate Windows memory or hosted Android runner, run these Robolectric Compose classes:
   - com.veilreader.app.ui.screens.RebornThresholdScreenTest
   - com.veilreader.app.ui.screens.RebornInvestigationDeskTest
   - com.veilreader.app.ui.screens.GrayfogThresholdSurfacesTest
2. Build separate debug Forge QA variant with veilRebornThreshold=true. Do not overwrite production installation.
3. Screenshot dark/light/high-contrast, 320dp/390dp/600+dp, 100%/200% font, mixed Persian-English.
4. Test empty archive, unopened book, started book, many books, missing cover, and sealed/revealed/resealed clues.
5. Compare actual Compose screenshots against approved high-fidelity Grayfog design and documented Apple Books / ReadEra / Moon+ behavioral references.
6. Verify touch targets >=48dp, TalkBack focus, keyboard/system insets, gestures, real book metadata and no fake scores.
7. Retain PASS/FAIL evidence, build hash, device/locale/font configuration, screenshot and recording filenames.

## Suggested test command (not executed)

On Windows PowerShell, from the new branch worktree:

    .\gradlew.bat :app:testDebugUnitTest --tests com.veilreader.app.ui.screens.RebornThresholdScreenTest --tests com.veilreader.app.ui.screens.RebornInvestigationDeskTest --tests com.veilreader.app.ui.screens.GrayfogThresholdSurfacesTest --no-daemon --max-workers=1 --console=plain

Debug QA build after tests: .\gradlew.bat :app:assembleDebug -PveilForgeQa=true -PveilRebornThreshold=true --no-daemon --max-workers=1

## Release decision

NO-GO until compilation, on-device screenshot comparison, accessibility and user visual acceptance succeed. This document is not proof of runtime success. Figma/concept screenshots are not device evidence.
