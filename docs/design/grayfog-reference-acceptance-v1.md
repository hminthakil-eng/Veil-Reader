# Grayfog Reference Acceptance v1

Status: LOCKED visual acceptance target for the current Veil Reader fidelity pass.

## Product intent

Veil Reader should feel like a private archive beyond ordinary time: dark stone, fog, candle warmth, moon-cool depth, aged brass, editorial serif hierarchy, and parchment where reading needs calm contrast.

The reference boards define atmosphere, hierarchy, density, and interaction quality. They are not a license to copy another franchise's artwork, names, characters, logos, or prose. Veil Reader must keep an original identity.

## Acceptance surfaces

### Launch
- Dark Grayfog launch surface with Veil Reader mark.
- No bright default Android flash.
- Startup should move directly into usable app content; avoid a mandatory decorative delay.

### Home / Threshold
- Cinematic archive hero is immediately visible.
- Veil Reader identity is readable over the art.
- Continue Reading is the dominant action when a book is active.
- Continue Reading uses a parchment/folio treatment that visually separates reading from app chrome.
- Recent books and progression stay below the primary reading action.

### Library / Grayfog Archive
- Atmospheric Grayfog masthead.
- Search and reading-state filters are obvious without opening settings.
- Grid and list both remain supported.
- Book covers are treated as objects, not generic cards.
- Shelves/Collections are a first-class surface.
- Import remains obvious and local-first.

### Book Detail
- Cover, title, author, series/status, and reading progress form one strong hierarchy.
- Continue/Open is the dominant CTA.
- Favorite and metadata actions remain secondary.
- Publication details and collections use archival panels rather than generic Material cards.

### Reader
- Reading canvas is intentionally calmer than the shell.
- Paper and Sepia feel warm and print-like; Dusk and OLED remain low-glare.
- Chrome is hidden by default and can be exposed with one intentional gesture.
- Notes, bookmark, appearance/PDF controls remain reachable without leaving the book.

### Reading motion
- Paper Curl, Slide, and Scroll are distinct mutually-exclusive user choices.
- Paper Curl has tactile weight, dynamic fold/shadow, and gesture-following behavior.
- Slide never shows curl deformation.
- Scroll is continuous vertical reading.

### PDF
- Real PDF renderer remains authoritative.
- Paginated and continuous layouts remain available.
- Fit/width/zoom controls stay near the page.
- PDF behavior must not be visually faked for fidelity.

### Hidden Archive
- Notes, Highlights, and Bookmarks are distinct first-class views.
- Search spans book title, author, saved passage, and note text.
- Saved locations reopen the real publication position.

### Settings
- Grayfog archival treatment; avoid a generic settings template.
- Reader motion uses the same Paper Curl / Slide / Scroll model as the Reader sheet.
- Reading theme, typography, brightness, backup/export, privacy, and local-first behavior remain clear.

### Primary navigation
Primary chrome is:
- Home
- Library
- Archive
- More

Castle and Path remain available as secondary progression surfaces inside More, not as dominant reading navigation.

## Visual rules

- Deep blue-black / charcoal shell.
- Aged brass for hierarchy and focus, never neon gold.
- Fine rules and restrained ornamental geometry.
- Parchment only where it clarifies reading/folio hierarchy.
- Editorial serif for major display hierarchy; practical readable type for dense controls.
- Avoid universal rounded cards, glassmorphism, large glow, bounce, and decorative motion loops.
- Motion should be short, quiet, and state-driven.

## Quality gates

A fidelity slice is not complete until:
1. unit tests pass,
2. Android lint passes,
3. debug APK assembles,
4. key surfaces are inspected on a phone,
5. large text and short/landscape layouts remain usable,
6. RTL/Persian does not break navigation or reading direction,
7. EPUB/PDF reliability and durable reading position are unchanged.

## Current checkpoint

Target branch: `design/grayfog-reference-fidelity-v1`

This pass must preserve Readium/PDFium, Room persistence, reader locator durability, and local-first behavior while pushing the visible product toward the locked reference quality.
