# Reader — Page Override

## Goal
The book is the product. App chrome should disappear when not needed and return predictably without disturbing reading position.

## Hierarchy
1. Publication content.
2. Reading position and navigation.
3. Essential reader controls.
4. Notes/highlights/search.
5. World/gamification feedback only outside uninterrupted reading moments.

## Reading surface
- Keep decorative UI outside the publication canvas.
- Reader theme/background choices must prioritize sustained comfort.
- Respect publication direction, writing mode, and metadata.
- Do not force app typography onto publication content when Readium/content styling should control it.
- Expanded layouts should cap comfortable reading width instead of stretching prose edge to edge.

## Controls
- Controls appear on intentional tap/gesture and dismiss cleanly.
- Top/bottom bars must not permanently consume reading space unless accessibility or device mode requires it.
- Progress, chapter position, TOC, search, appearance, notes, and bookmark actions need stable locations.
- Icon-only controls require TalkBack labels and minimum practical 48dp targets.

## Selection and notes
- Preserve Android/Readium selection expectations.
- Custom actions must not break copy, selection handles, accessibility focus, or system back.
- Highlight color is never the only indicator of note/highlight type.

## Motion
- UI chrome transitions: quick/standard only.
- Page-turn/scroll behavior follows the selected reading mode.
- Reduced-motion should remove decorative parallax, glow, and ceremonial transitions.
- Gamification/progression celebrations must never interrupt active reading.

## RTL
- App chrome mirrors appropriately.
- Content direction follows the publication.
- Page progression and gestures must respect publication reading progression rather than blindly following locale.

## 21st.dev usage
Reference only restrained reader/toolbars, sheets, popovers, command/search patterns, and settings layouts. Avoid marketing-style hero effects, heavy glass, animated backgrounds, and web-first interaction patterns.

## QA
Test EPUB + PDF, LTR + RTL, portrait + landscape + tablet, large font scale, TalkBack, selection/action mode, appearance changes, bookmarks, notes, system back, reduced motion, and process recreation/resume position.
