# Grayfog reference match — pass 4

Base: fe328665e4adec1067f4f1dfc55b4feb421e0c82.

- Reader and global Settings share theme swatches, a labelled page sample, and accessible radio-row motion controls. Theme selection calls existing withTheme only: user font size, line height, margins and motion are no longer reset by a color selection. Publisher styles are disabled exactly as withTheme already specifies.
- Curl/Slide/Scroll update the existing persisted fields. Scroll retains the previous page-turn style. No navigator, reader persistence, lifecycle, or publication engine was replaced.
- Bottom navigation participates in Column measurement; content receives the remaining height instead of a fixed 88dp bottom reservation. This accommodates system insets and scaled labels.
- A Shelves dialog exposes real favorite/reading/finished/unread counts and user collections. Selection applies the existing library filters and clears stale search. Metadata editing remains the existing path for assigning collections.
- Removed duplicate archive heading, retained import/settings actions, flattened reader chrome shadows, corrected reader/PDF sheet heading color semantics and simplified PDF guidance.

Validation: twelve local Kotlin files parse with tree-sitter; whitespace checks pass. Source comparisons confirm reader lifecycle before chrome, appearance draft ownership, and Readium/PDF preference adapters are unchanged (ignoring final blank line). These checks are not Kotlin compilation or device verification.

Build access: Android SDK absent locally; dl.google.com SDK probe failed with proxy CONNECT timeout. Opera connector reports browser not connected. No Codemagic run was started and no APK was produced. Keep PR draft until assembleDebug + relevant unit/lint gates and on-device visual checks pass.

Device gates: appearance sample versus actual EPUB, retained typography after changing all four themes, motion selection/persistence in both settings surfaces, publisher-style override, PDF scroll/zoom, measured dock at 2× fonts and gesture/three-button navigation, long/narrow shelf rows, collection selection and reset, light/dark theme contrast, Persian/RTL and landscape.
