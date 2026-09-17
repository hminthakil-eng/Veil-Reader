# Library — Page Override

## Goal
Make the library feel like a personal collection, not a file browser. The first scan should answer: what am I reading, what is new, and what can I continue?

## Information hierarchy
1. Continue Reading / current book.
2. Recently added / recently opened.
3. Search and filters.
4. Full collection.
5. Secondary metadata and management actions.

## Layout
- Compact width: one dominant Continue Reading card, then list/grid collection.
- Medium/expanded: keep Continue Reading visually dominant; allow denser multi-column collection.
- Book covers keep a stable aspect ratio; do not distort artwork.
- Prefer progressive disclosure for metadata and management controls.

## Components
- Search should be obvious but visually quiet when unused.
- Filters use accessible chips/toggles with clear selected states.
- Book cards must support title wrapping, long author names, missing covers, progress, unread/new state, and accessibility descriptions.
- Context actions belong in a predictable overflow/menu, not as many always-visible icons.

## Motion
- Subtle shared continuity when opening a book is welcome.
- Reordering/filter changes should not produce distracting choreography.

## 21st.dev usage
Patterns worth referencing: editorial card grids, compact command/search patterns, tasteful segmented controls, responsive collection layouts. Translate all patterns to Material 3/Compose.

## QA
Test: empty library, 1 book, 100+ books, long RTL titles, 200% font scale, missing covers, landscape/tablet, TalkBack.
