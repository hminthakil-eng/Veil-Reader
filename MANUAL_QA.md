# 0.4.0 device acceptance checks

Run after `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` succeeds. These cases are supplied for verification; they have not been executed on a device here.

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

Outstanding baseline risks: fast page navigation can still award XP; every path currently shares the five-highlight ritual; configuration-change restoration and large-library storage need dedicated device testing.

# Additional 0.5.0 acceptance cases

12. Import `qa/fixtures/veil-smoke.epub`. Verify both chapters and the nested A Small Clue anchor; select and highlight a paragraph; confirm font size is readable at 100%.
13. Favorite a book and assign a collection. Restart the app. Search by title/author/collection, use each reading-state filter and each sort order, edit metadata, clear a collection and test no-results recovery.
14. Open Profile → Hidden Archive, search across books, open a passage, use Android Back, and verify the original archive state returns.
15. Export a Markdown notebook. Open it outside the app and compare Unicode quotes, multiline notes and book titles. Cancel the picker: no error and no export.
16. Export a library ZIP. Test its integrity externally. Compare extracted EPUB/PDF bytes to imported files and inspect locator/note/game metadata. Verify read-only or full destinations report failure. Do not treat preserved JSON as a tested automatic restore.
17. Before first advancement choose each Path. Verify only its described event advances the ritual. For note paths, 39-character notes fail, 40-character notes count once, and editing the same note cannot add credit again. Confirm midnight boundaries using device local time.
18. Flip pages quickly: no page XP. Wait at least eight seconds and move to a new location: eligible credit. Revisit a location, jump using contents, change font size, and background/resume: no immediate bonus. Check daily allowance against active minutes.
19. Earn a sigil, restart, end a streak or delete highlights and confirm earned status persists. Confirm level/rank ceilings and quest rewards are not accidentally duplicated.
20. Switch Android system light/dark mode. Compare paper, sepia, dusk and OLED reader themes. Verify text is not hidden by either toolbar and appearance controls scroll in landscape/large text.

Remaining risks listed earlier should be read with 0.5.0 changes: page farming now has bounds and the six rituals are distinct. Device verification, configuration-change restoration and large-library persistence remain outstanding.
