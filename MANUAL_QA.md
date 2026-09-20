# 0.10.0 release-candidate device acceptance checks

Run after the host gate succeeds: `gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug :benchmark:assembleBenchmarkBenchmark`. Host success is not device evidence; every runtime item below must record the device/API, build SHA and result.

1. Upgrade an installation with imported EPUB/PDF books, highlights, and progress. Confirm all survive without clearing app data.
2. Open an EPUB with nested contents. Open Contents & notes, select a nested chapter, and verify the visible chapter. Repeat with a PDF outline and a book lacking a table of contents.
3. Save a bookmark in both EPUB and PDF. Re-save the identical location: no duplicate. Move elsewhere and use Go to place. Restart the app and repeat. Remove it and confirm removal after restart.
4. Highlight an EPUB selection. Add a multiline note, save, restart, and verify quote and note. Search for a quote fragment and a note fragment, including mixed case. Check a query with no match.
5. Edit a note; Cancel must preserve the original. Save an empty note to clear it. Delete a highlight: Keep preserves it; Delete removes both note and decoration.
6. Re-highlight an identical selection. Verify no duplicate or extra ritual progress. Confirm another book's notes never appear in this notebook.
7. Change theme, size, line height, scroll and publisher styles. Close/reopen and restart the process; settings should persist. Test on a small screen with large system text.
8. Turn a page and immediately press Android Back, reopen, and check the locator. Repeat with the Library button. Dismiss notebook and appearance sheets with Back before exiting the reader.
9. Try empty, invalid and inaccessible import files. Verify a readable error and no new bookshelf record. Confirm a valid import still works afterward.
10. Background the reader for over a minute: reading minutes must not increase. Resume for a full minute: one minute should be recorded. Finish a book twice: only the first completion should award the completion bonus.
11. Check light/dark reading, text selection, tap navigation, TalkBack labels and bottom-sheet scrolling in both portrait and landscape. Confirm toolbar overlays do not make the first or last lines inaccessible.

The original baseline cases above remain mandatory regressions. Current RC blocking evidence additionally includes physical page-turn behavior, PDF zoom, Settings persistence, accessibility/adaptive layouts, configuration restoration and large-library durability.

## Regression cases retained from 0.5.0

12. Import `qa/fixtures/veil-smoke.epub`. Verify both chapters and the nested A Small Clue anchor; select and highlight a paragraph; confirm font size is readable at 100%.
13. Favorite a book and assign a collection. Restart the app. Search by title/author/collection, use each reading-state filter and each sort order, edit metadata, clear a collection and test no-results recovery.
14. Open Profile → Hidden Archive, search across books, open a passage, use Android Back, and verify the original archive state returns.
15. Export a Markdown notebook. Open it outside the app and compare Unicode quotes, multiline notes and book titles. Cancel the picker: no error and no export.
16. Export a library ZIP. Test its integrity externally. Compare extracted EPUB/PDF bytes to imported files and inspect locator/note/game metadata. Verify read-only or full destinations report failure. Do not treat preserved JSON as a tested automatic restore.
17. Before first advancement choose each Path. Verify only its described event advances the ritual. For note paths, 39-character notes fail, 40-character notes count once, and editing the same note cannot add credit again. Confirm midnight boundaries using device local time.
18. Flip pages quickly: no page XP. Wait at least eight seconds and move to a new location: eligible credit. Revisit a location, jump using contents, change font size, and background/resume: no immediate bonus. Check daily allowance against active minutes.
19. Earn a sigil, restart, end a streak or delete highlights and confirm earned status persists. Confirm level/rank ceilings and quest rewards are not accidentally duplicated.
20. Switch Android system light/dark mode. Compare paper, sepia, dusk and OLED reader themes. Verify text is not hidden by either toolbar and appearance controls scroll in landscape/large text.

## 0.10 reader-interaction acceptance

21. Open Profile → Reader & app settings. Verify Back returns to Profile, the route survives recreation, and Settings remains a secondary destination rather than a sixth primary tab.
22. Change app theme and EPUB paper/sepia/dusk/OLED theme, text size, line height, margins and publisher styles. Reopen the book and restart the app. Confirm 100% text is normal-sized and every theme remains readable.
23. In paginated EPUB mode test Paper curl and Simple slide separately. For Paper curl test edge tap, body swipe, slow drag, cancel, rapid repeated turns and first/last-page boundaries in both LTR and RTL content. A cancelled preview must not advance saved progress.
24. Turn on continuous scroll and verify page-turn style no longer interferes with vertical reading. Return to pagination and confirm the selected page-turn style resumes.
25. In PDF, open Zoom and test zoom in/out, slider, reset and fit-to-width. Rotate, background/foreground and reopen the PDF; navigation and rendering must remain usable without a crash or stuck scale.
26. In Settings → Data & backup, export a backup, cancel each picker once, export the notebook, and run a validated restore. Confirm the same data controls are not duplicated in Profile.
27. Repeat the reader/settings matrix with large system text, TalkBack/focus navigation, short landscape and an expanded/tablet layout. Record any clipped control, unreachable action or incorrect rail/bottom navigation transition.

Current release blockers are device evidence for the cases above, backup/restore durability on hardware, adaptive/accessibility sign-off, large-library/large-PDF behavior, and a reproducible signed release artifact.
