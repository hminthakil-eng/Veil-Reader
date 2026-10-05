# Veil Reader Grand Forge — UI/UX Scorecard

Date: 2026-10-05
Branch: `grand-forge/arena-app-hardening-v1`
Reviewed head: `01827f78e91bbcde572d1ca80d049cd1fe9accbc`

## Evidence boundary

This score uses current production source, current Grayfog/native-layout audit evidence, and the locked Veil reference boards. It is not a physical-device certification. Paper/Slide tactility, real Android text rasterization, OEM system bars, IME, TalkBack and long-session comfort still require device evidence.

## Scores

- Visual identity / art direction: 94/100
- Typography system: 92/100
- Threshold / Home: 89/100
- Library / Archive retrieval: 85/100
- Book Detail: 88/100
- Reader Sanctuary: 90/100 source design; device feel still unproven
- Notebook / Hidden Archive: 83/100
- Settings: 75/100
- Navigation / information architecture: 74/100
- Castle / Path / Profile world UX: 78/100
- Persian / RTL: 91/100
- Accessibility architecture: 85/100
- Adaptive layout: 86/100
- Motion grammar: 80/100
- Error / loading / empty-state UX: 84/100
- Manga Hub / Manga Reader UI maturity: 73/100

Weighted current score: **85/100**.

## Strongest areas

### Distinct product identity
Grayfog has a coherent material grammar: deep blue-black architecture, brass registration, parchment reserved for reading/special artifacts, editorial typography and restrained celestial motifs. The strongest decision is that Reader stays visually quieter than Archive/Castle.

### Reading-first hierarchy inside key surfaces
Threshold promotes current reading before secondary systems. Library puts retrieval before lore. Book Detail keeps Read/Continue adjacent to publication identity. Reader keeps publication content dominant.

### Typography and Persian
The shell has separate Latin and Persian/Arabic font families, zero tracking for connected scripts, larger script-aware line-height and dynamic-content helpers. Library search normalizes Persian/Arabic yeh/kaf, marks, spacing and digit shapes.

### Accessibility fundamentals
Navigation targets are 56dp. Many controls use 48dp+ minimums. Dense choice groups stack at larger font scales. Reduced Motion, High Contrast and RTL navigation direction are first-class policies.

## Highest-value defects

### P0 UX architecture — primary navigation is too world-heavy
With `gameVisible=true` (the default), `visibleVeilTabs()` exposes all five tabs:
`READING / LIBRARY / CASTLE / PATH / PROFILE`.

This promotes progression/world systems to the same level as reading and library retrieval while Notebook/Hidden Archive is not a direct primary destination. It conflicts with the reading-first hierarchy and with the locked four-destination reference grammar.

Recommended compact shell:
- Reading/Home
- Library
- Notebook/Archive
- More

Castle/Path/Profile should remain highly visible through contextual world entry points, not consume three permanent bottom-navigation slots.

### P0/P1 — Settings is still too long
`SettingsSection()` is an always-expanded heading + description + content block. Only the Reading section has Quick/Advanced disclosure. The screen therefore contains Appearance, Reading, Tap Matrix, hardware keys, Focus Guide, Sound, World, Backup, Privacy, debug controls and Reset in one vertical flow.

Recommended:
- Reader
- Accessibility
- Library & Data
- Experience
- World
- About

Keep Reader quick settings inside Reader itself; global Settings should not duplicate every fast-reading control.

### P1 — Library control density
The current order is correct (header → search → reading-state filters → view mode → collection/series/sort → books), but compact phones can still spend too much first-viewport height on controls before book objects. Hide inactive advanced filters behind one compact Refine affordance on narrow screens while preserving direct search and reading-state access.

### P1 — Notebook mixed-script leak
ReaderNotebook bookmark labels currently use ordinary `titleSmall` rather than `withVeilContentScript(...)`. Persian/user-authored bookmark labels can inherit the wrong family/tracking in an English shell. Quotes have already been corrected; bookmark labels need the same treatment.

### P1 — World surfaces remain less mature
Castle/Path/Profile have strong narrative concepts but less explicit semantic/touch/adaptive coverage than Library/Reader. The world must feel spatial and architectural rather than like premium cards with lore text.

### P1 — Reader proof gap
The Reader design is one of the strongest areas, but its defining advantage—physical Paper—cannot be scored as market-leading until real-device first turn, destination-flash absence, slow/flick/reverse/corner turns, 60/90/120Hz pacing, selection coexistence, rotation and long-session memory are demonstrated.

### P1 — Manga visual maturity
Manga architecture is substantial, but its UI layer has lower explicit semantic/adaptive coverage than the core reader shell. Treat Manga as a separate design-quality pass after the text Reader shell is frozen.

## Screen verdicts

### Threshold — 89
Strong authored entrance and continuation hierarchy. Main remaining risk is vertical narrative accumulation: Recent, Whisper, Mirror and Reading Pulse can turn the home surface into a long story feed. First viewport should answer only: what am I reading, where was I, and how do I continue?

### Library — 85
Retrieval is strong and the three presentation modes have distinct grammar. Search and Persian normalization are excellent. The remaining issue is compact control density, not missing capability.

### Book Detail — 88
Very good artifact + identity + primary-action order. Keep archive history/provenance secondary and collapsible so it never competes with Continue Reading.

### Reader — 90 source
Correct realm philosophy: quiet, parchment/material-focused, center-tap controls, Paged/Scroll separate from Paper/Slide/None. Do not import Grayfog ornament density into the reading canvas.

### Notebook / Hidden Archive — 83
Useful and distinctive, but scanning can become dense. Keep tabs/search at top, quote/note hierarchy strong, destructive actions quiet, and fix bookmark script awareness.

### Settings — 75
The weakest important core surface. It is technically rich but still behaves more like a power-user configuration document than an authored premium settings experience.

### Navigation — 74
Touch sizing and RTL motion are good; IA is the issue. Five world-oriented primary destinations dilute reading-first purpose.

### Castle / Path / Profile — 78
Distinct concept and good visual vocabulary, but they need stronger spatial interaction and less repeated panel/record grammar. They should feel like places, not dashboards wearing gothic material.

### Persian / RTL — 91
One of Veil's real competitive advantages. Continue the mixed-script audit rather than treating localization as translated strings only.

### Manga — 73
Functional architecture is ahead of visual-system maturity. Keep it secondary until Reader/Library navigation is frozen.

## 95+ gate

Veil reaches the 95-class UI/UX tier only when:
1. primary navigation becomes reading-first;
2. Settings gains real progressive disclosure;
3. compact Library reveals books faster;
4. remaining mixed-script leaks are eliminated;
5. Castle/Path/Profile become spatial rather than panel-led;
6. Manga receives its own polished visual pass;
7. current real-device screenshots match or outperform the locked reference boards;
8. Paper/Slide motion passes physical-device Arena review.

The current product is not a generic template. It already has a strong design identity. The remaining gap to world-class is mostly hierarchy, information architecture, tactile proof and disciplined subtraction—not adding more decoration.
