# Veil Reader — Gray Fog Archive Design Direction v1.1

Status: synchronized to Veil Reader 0.10.0 RC
Figma: https://www.figma.com/design/5IlRC6CFN0PRyue0Tig3s3
Canonical integration target: integration/reader-rc-v2
Synchronization date: 2026-09-25

## Intent
Create a premium, calm, mysterious reading experience inspired by the atmospheric DNA of Lord of Mysteries without copying protected artwork, symbols, characters, layouts, or branded assets.

The product identity is **Gray Fog Archive**:
- archival mystery
- industrial precision
- layered revelation
- Victorian/Edwardian material cues
- restrained occult atmosphere
- strong reading-first hierarchy

## Product rules
1. Text is the hero. Reader canvas must approach zero UI density.
2. Center tap reveals/hides reader chrome.
3. Reader bottom chrome contains Notebook, Bookmark, Appearance. Avoid duplicate prev/next and highlight actions.
4. Highlight/Note should live in text-selection context actions.
5. Settings use progressive disclosure: Quick first, Advanced second.
6. Appearance changes preview live.
7. Theme chips only change theme; no hidden bundled side effects.
8. Do not inherit OEM decorative fonts into app chrome.
9. UI targets should be >= 48dp where practical.
10. Design LTR/RTL, normal/large text, compact/expanded widths together.
11. Never show Castle/Path/progression UI over publication content.
12. No nested-card, gradient, glass, or shadow soup.

## Current app baseline
- app version: 0.10.0 / versionCode 10
- compileSdk / targetSdk: 37
- minSdk: 26
- Jetpack Compose UI: 1.10.5
- Material 3: 1.4.0
- Material 3 Adaptive: 1.3.0
- Readium Kotlin Toolkit: 3.4.0
- PDFium adapter: 3.4.0
- AndroidPdfViewer: 3.2.8

## Active product-completion alignment
### PR #268 — Library Settings shortcut
Design contract:
- Settings and Import are both one move away from Library.
- Compact width: Settings + Import share one row.
- Wider layout: discrete actions remain visible.
- Minimum action height: 48dp.

### PR #269 — EPUB Appearance Quick / Advanced
Design contract:
- Quick: Theme, Text size, Reading mode, Page turn.
- Advanced: Line height, Page margins, Publisher styling, Reset.
- Brightness remains visible in both modes.
- Theme chips change theme only.
- Appearance changes remain live-previewed.

### PR #270 — PDF View controls
Design contract:
- Surface is named PDF view, not PDF zoom.
- Layout, zoom and brightness are grouped together.
- Pinch/double-tap remain native direct-manipulation paths.
- Zoom actions use 48dp+ targets and explicit semantics.

## Color direction

### Dark shell
- Canvas: #0B0E12
- Surface: #12171E
- Surface elevated: #1B222B
- Text primary: #F2EFE7
- Text secondary: #AEB4B8
- Border: #313A45
- Antique brass: #C4A260
- Spirit teal: #74B6B8
- Moon crimson: #C25C6C

### Light shell
- Canvas: #F2EFE7
- Surface: #FBF8F1
- Surface elevated: #EEE8DD
- Text primary: #15191F
- Text secondary: #67675F
- Border: #C9C1B4
- Antique brass: #8A6A35
- Spirit teal: #2C6E73
- Moon crimson: #7B2838

### Reader
- Warm Paper: #F7F1E3
- Dusk and OLED remain independent reader themes.
- Crimson is rare emphasis, never the default action color.

## Typography
- Product UI: deterministic highly legible sans; respect system font scaling.
- Editorial headings: restrained literary serif.
- Publication typography remains independent and controlled by book/publisher + reader appearance settings.

## Density
- Home / Archive: medium-high information density.
- Library: retrieval-first, metadata-friendly rows and adaptive grids.
- Book detail / Notebook: moderate density.
- Reader: near-zero interface density.

## Motion
- Control feedback: 120–180 ms.
- Context reveal: 180–240 ms.
- Preserve reading position and spatial continuity.
- Reduced Motion: no decorative depth/curl; use immediate transition or subtle fade.
- Fog is a progressive-disclosure metaphor, not a permanent particle effect.

## Figma v1.1
Pages:
1. 01 Foundations
2. 02 Screens
3. 03 Components

Screens:
- Home — Gray Fog
- Library — Retrieval First
- Reader — Sanctuary
- Appearance — Quick
- Appearance — Advanced
- PDF View — Controls

The Library mockup now includes Settings + Import. Quick Appearance includes always-visible brightness. Advanced Appearance mirrors PR #269. PDF View mirrors PR #270.

Starter components:
- Primary / Secondary button
- Standard icon button
- Filter chips
- Theme chip
- Reader actions
- Library book row

## Code/design delta discovered during synchronization
The current 0.10.0 theme foundation still uses the earlier Amethyst/Purple identity in `Theme.kt`.
Gray Fog v1.1 therefore moves into implementation through a compatibility-first theme migration:
- introduce semantic Gray Fog palette names
- remap MaterialTheme primary/secondary/tertiary to Brass / Spirit / Crimson
- preserve legacy palette aliases temporarily so existing screens keep compiling
- keep Reader publication themes independent from app-shell palette
- no Reader engine, persistence, Room, navigation, Manga, or release-hardening change

## Implementation phase 1 — Gray Fog Theme Foundation
Goal: make the existing Compose app visually match the approved Gray Fog system without creating a second theme architecture.

Acceptance:
- Material dark/light schemes use Gray Fog colors.
- Existing code that references legacy VeilPalette names still compiles via compatibility aliases.
- Typography hierarchy stays deterministic and accessible.
- No publication typography behavior changes.
- No schema or navigation changes.
- Change remains independently reversible.

## Next execution order
1. Gray Fog Theme Foundation in Compose.
2. Reader chrome + Appearance visual polish on top of PR #269.
3. PDF View visual polish on top of PR #270.
4. Library retrieval states + Settings/Import polish on top of PR #268.
5. Home / Continue Reading and quiet milestones.
6. Book Detail + Notebook.
7. Tablet / foldable / desktop-window variants.
8. RTL Persian/Arabic validation.
9. Accessibility / large-text / reduced-motion QA.
