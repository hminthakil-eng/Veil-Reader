# Grayfog reference match — pass 3

Based on d19208b. Book detail opens from the existing overflow action in grid/list while tapping a cover still reads immediately. A saveable book ID resolves the live book record; favorite updates remain reactive. Detail shows actual title/author/format/language/series/collections/chapter/progress and routes metadata editing through the existing editor. Read, read again and continue actions use the existing book callback. No invented synopsis or genre.

Hidden Archive uses one lazy viewport, real highlight records, Notes (nonempty note) and Highlights (all records) filters, keyboard Search dismissal, paper note inserts, and original locator-based passage navigation. No bookmarks tab is shown because this screen has no bookmark data source.

Settings sections are saveably expandable. Reading motion has mutually exclusive Paper curl / Slide / Scroll choices mapped to existing scroll + pageTurnStyle fields. Switching to Scroll retains the last paginated style; choosing Curl or Slide clears scroll. Persistence/draft ownership and backup/restore/reset callbacks are retained.

Validation: Kotlin tree-sitter syntax and whitespace checks for seven local sources; source comparison confirms Settings appearance ownership and backup/restore/reset blocks unchanged. Runtime, Compose type checking, TalkBack behavior and APK compilation remain unverified in this environment.

Device checks: open/close detail from grid and list; favorite state; edit/cancel/save; continue and read again; rotation/back; long title, Persian metadata and 2× fonts; archive search + notes filter + empty result + passage navigation; Settings expand/collapse/restore, each motion choice and persistence; backup/restore confirmation unchanged. Codemagic Fast Verify on design/grayfog-reference-match is the next compilation gate.
