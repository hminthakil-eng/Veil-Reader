# Moon+ Reader 10.7 — MIMIC-style reverse-engineering audit

Date: 2026-10-02  
Reference: user-supplied Android package `Moon+ Reader Pro 10.7`  
SHA-256: `d676b16ff18c2496ab35d1306fd195e7fba491bc8260cfd368ab973c7bc09d08`

## Purpose

Apply the evidence discipline and reconstruction loop from `sumanrox/mimic` to an Android reader reference without copying proprietary source code or assets.

MIMIC is browser-first, so this audit separates what can be measured statically from the APK from what still requires a live Android runtime.

Evidence labels:
- **OBSERVED** — directly present in package resources, manifest strings, assets, or embedded product documentation.
- **INFERRED** — derived from multiple observed artifacts.
- **APPROXIMATE** — design interpretation useful for Veil Reader but not a measured Moon+ value.
- **UNVERIFIABLE** — requires a live app/emulator capture before claiming fidelity.

## Static package fingerprint

**OBSERVED**

- Package identifiers include `com.flyersoft.moonreaderp`.
- Embedded version name: `10.7`.
- Resource surface is large:
  - 356 base layouts
  - 95 Android-v22 layouts
  - 458 base drawables
  - 310 hdpi drawables
  - 126 xhdpi drawables
  - 114 xxhdpi drawables
  - 60 animation resources
  - 33 animator resources
  - 33 reader background assets
  - 12 bundled reader theme presets
- Bundled fonts include Arial, Calluna, Times New Roman, and Vollkorn.
- Bundled hyphenation dictionaries cover many languages.
- Manifest file associations include EPUB, PDF, MOBI/AZW3, FB2, TXT, RTF, DOCX, ODT, CBZ/CBR, DJVU, CHM and others.

## Reader architecture signals

### Reading modes and page motion

**OBSERVED**

Resource/class names expose:
- `NewCurl3D`
- `GLLayoutCurl`
- `pdf_curl.xml`
- horizontal and vertical page controls
- dual-page layouts
- page-turn color configuration
- scroll-specific controls

Embedded product notes also describe:
- several page-flip animations
- configurable page-turn speed/color/transparency
- pixel/line/page auto-scroll variants
- right-to-left PDF/comic reading
- smart scroll locking

**INFERRED**

Moon+ treats page motion as a first-class reader subsystem rather than a single animation toggle. Page mode, scroll mode, PDF navigation, and dual-page behavior appear to have distinct implementations.

### Appearance model

**OBSERVED**

Bundled theme XML exposes persistent reader parameters including:
- background image on/off
- background color
- font color
- font name
- font size
- bold/italic/shadow
- left/right/top/bottom margins
- line spacing
- text justification
- alignment
- font spacing
- fading edge
- page-curl color

Examples of measured theme values:
- warm day surface near `#DCD7D1`
- true black night surface `#000000`
- night text near `#CCCCCC`
- one parchment/nature preset uses background near `#E6EACF`
- one dark scenic preset averages near RGB `5,11,31`

**INFERRED**

The important lesson is not the specific colors or textures. The reusable design rule is that reader appearance is modeled as a durable parameter set that can be previewed, saved and switched as a complete reading state.

### Interaction model

**OBSERVED**

Package documentation/resources indicate:
- customizable screen taps
- swipe gestures
- hardware-key actions
- a nine-zone tap grid
- left-edge brightness gesture
- quick bookmark behavior
- configurable reader bar
- bookmark, search, theme and navigation actions

**INFERRED**

Moon+ separates gesture-to-action mapping from the actions themselves. This is a stronger extensibility model than hard-coding one fixed tap layout.

### Reading tools

**OBSERVED**

The package contains UI/resources for:
- highlights
- notes
- PDF annotations and handwriting
- bookmarks
- chapter/outline navigation
- dictionary and translation hooks
- text-to-speech
- reading ruler/focus tools
- reading calendar/statistics
- Readwise integration
- theme import/export
- font replacement/blocking
- character-size-based pagination
- recent books inside the reading window
- foldable and Chromebook handling

### Library and organization

**OBSERVED**

Resources show:
- shelves
- favorites
- tags
- authors
- coverflow/grid/list variants
- arbitrary/natural sorting
- ratings
- book grouping
- cloud/backup-related UI

## Veil Reader reuse gate

Do **not** copy Moon+ assets, text, code, layouts or branded visual treatment.

Reuse only behavior, information architecture and measurable interaction principles through independent implementation.

### P0 — applicable to current Veil Reader reader polish

1. **Strictly separate page and scroll modes**
   - one explicit state model
   - no shared ambiguous control
   - independent behavior and persistence

2. **Reader appearance as a saved state object**
   - type scale
   - line height
   - margins
   - surface/theme
   - publisher-style override
   - optional future font family/weight controls
   - immediate live preview

3. **Tap-zone architecture**
   - preserve simple center-tap chrome by default
   - make the zone/action mapping internally data-driven so advanced customization can be added without rewriting reader input

4. **Page-turn fidelity**
   - define animation type, direction, duration, curl/shadow behavior and interruption rules as explicit reader state
   - benchmark first-turn behavior separately from subsequent turns
   - page and scroll must never share the same animation path

5. **Reader quick actions**
   - bookmark
   - appearance
   - contents
   - search
   - notes/highlights
   - previous location

### P1 — after the current world-class core is stable

- optional nine-zone gesture customization
- reading ruler/focus aid
- richer typography controls where Readium can support them cleanly
- dual-page/tablet/foldable reader layout
- character-based page count presentation where technically meaningful
- richer PDF annotation index

### P2 — keep out of the current release-hardening scope

- TTS
- auto-scroll
- cloud sync
- Readwise
- network catalogs

These remain useful references but should not displace the existing release gates.

## MIMIC validation plan for Android

The browser-specific MIMIC loop translates to:

`OBSERVE → MEASURE → IMPLEMENT → CAPTURE → DIFF → FIX → CAPTURE AGAIN`

For each target surface, capture on a real emulator/device:

1. reader clean canvas
2. center-tap chrome visible
3. appearance panel open
4. page mode before / during / after turn
5. scroll mode at equivalent content position
6. highlight selection state
7. bookmark state
8. PDF equivalent state where supported
9. portrait / landscape / tablet-width
10. RTL and large-text variants

Measurements to record:
- content viewport bounds
- reader chrome bounds
- text block width
- effective margins
- type size and line height
- action target sizes
- animation frame timing
- gesture thresholds
- navigation latency
- selected/unselected visual states

## Current limitation

**UNVERIFIABLE**

A live Moon+ runtime was not available during this pass, so no claim is made about pixel-perfect geometry, actual animation timing, gesture thresholds, runtime typography metrics, or page-turn frame curves.

The connected desktop/emulator should be used for the dynamic pass before implementing any fidelity-sensitive behavior.

## Decision

Use this reference as a **behavior and interaction benchmark**, not as a visual template.

For Veil Reader, the strongest portable ideas are:
- durable reader appearance state
- clean separation of page/scroll behavior
- configurable interaction mapping under a simple default
- dedicated page-turn subsystem
- deep reading tools kept behind quiet reader chrome

These align with the existing `0.9G — Reader immersion polish` roadmap and can be adopted without architecture rewrite.
