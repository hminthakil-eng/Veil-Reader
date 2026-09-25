# Veil Reader — Gray Fog Archive Design Direction v1

Status: design execution baseline
Figma: https://www.figma.com/design/5IlRC6CFN0PRyue0Tig3s3
Base integration target: integration/reader-rc-v2

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

## Current implementation mapping
- PR #269: Quick/Advanced EPUB appearance controls — visual polish should follow this spec.
- PR #270: PDF view controls — reuse the same hierarchy and brightness language.
- Issue #34: reader chrome simplification remains the interaction contract.
- Issue #30: deterministic app UI typography remains mandatory.
- Issue #123: page-curl is optional/feature-flagged and must preserve selection, RTL, annotations and reduced-motion behavior.
- Issue #133: visual QA matrix is required before product sign-off.

## Figma v1
Pages:
1. 01 Foundations
2. 02 Screens
3. 03 Components

Initial screens:
- Home — Gray Fog
- Library — Retrieval First
- Reader — Sanctuary
- Appearance — Quick

Starter components:
- Primary / Secondary button
- Standard icon button
- Filter chips
- Theme chip
- Reader actions
- Library book row

## Next execution order
1. Reader chrome and Appearance production polish.
2. Library retrieval states, search/filter/sort and empty states.
3. Home / Continue Reading and quiet milestones.
4. Book Detail + Notebook.
5. Responsive tablet / foldable / desktop-window variants.
6. RTL Persian/Arabic validation.
7. Accessibility / large-text / reduced-motion QA.
8. Compose implementation slice behind reversible feature gates where needed.
