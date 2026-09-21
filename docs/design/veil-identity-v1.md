# Veil Identity v1 — The Library Beyond the Veil

Status: **FOUNDATION / DRAFT**
Scope: visual + interaction identity for Veil Reader
North star: **The Library Beyond the Veil**

## 1. The identity

Veil is not a themed ebook reader. It is a modern fiction environment with three experiential realms:

### Archive
Where Veil may speak.
- library
- home / continuation
- collections
- progression
- discovery
- lore / world layer

Character: editorial, catalogued, mysterious, precise.

### Threshold
Where state changes become meaningful.
- opening a book
- returning to a saved location
- revealing navigator / settings
- completing a book
- unlocking a meaningful progression event

Character: brief, spatial, revelatory, interruptible.

### Sanctuary
Where Veil falls silent.
- EPUB reading
- PDF reading
- manga / webtoon artwork

Character: calm, content-first, nearly invisible.

## 2. Design equation

**70% contemporary editorial precision**
**20% archival materiality**
**10% uncanny revelation**

The 10% matters most. Mystery must be rare enough to remain mysterious.

## 3. Emotional attributes

Veil should feel:
- intelligent
- quiet
- tactile without skeuomorphism
- premium without luxury clichés
- old-world without looking historical
- futuristic without neon cyberpunk
- mysterious without hiding ordinary controls
- powerful without exposing configuration complexity

## 4. What Veil must never become

Do not use:
- card soup
- giant rounded rectangles as universal layout
- gold gradients as shorthand for premium
- constant animated fog
- random gothic ornament
- copied occult/tarot/franchise symbols
- glassmorphism as a default surface treatment
- bright cyberpunk neon
- gratuitous blur
- decorative motion before content
- mystical labels for basic actions
- gamification overlays on top of prose

## 5. Color philosophy

Color is semantic, not decorative.

### Dark world
- Obsidian: deepest canvas
- Ink: primary dark surface
- Slate: elevated utility surface
- Moon: primary text / focus light
- Aged Brass: archival emphasis
- Pale Moonlight: revelation / focus / selected state

### Light world
- Ivory: reading and editorial canvas
- Warm Paper: secondary surface
- Ink on Paper: primary text
- Deep Brass: archival accent with accessible contrast

Functional error/success/warning colors remain functional and are not forced into brand hues.

## 6. Geometry

Veil should look engineered, not bubbly.

Recommended radius hierarchy:
- 4dp — tiny indicators / precision details
- 8dp — standard controls
- 12dp — sheets / utility containers
- 16dp — rare feature surfaces
- pill — only when the control meaning benefits from it

Book covers stay rectangular unless the asset itself requires otherwise.

Signature geometry:
- hairline rules
- offset corner marks
- editorial grids
- narrow separators
- controlled asymmetry
- large quiet negative space

## 7. Typography

Typography is a system, not one brand font.

Roles:
- Display / Archive: restrained editorial serif
- UI: highly legible neutral sans
- Reader Latin: dedicated long-form reading face
- Reader Persian/Arabic: dedicated reading face, tested on long passages
- Reader fallback: script-appropriate system/fallback fonts

Rules:
- branding typography never overrides reading comfort
- RTL is first-class
- user can override Reader typography
- mixed-direction metadata must remain stable

## 8. Motion grammar

### Response
90–150ms
For immediate input acknowledgement.

### Continuity
180–260ms
Explains where a surface came from and where it goes.

### Threshold
260–400ms
Rare transitions that mark meaningful entry/exit/revelation.

### Ambient
Normally absent in Reader. Extremely restrained in Archive only.

Rules:
- interruptible
- state-linked
- directional
- reduced-motion equivalent required
- no motion may delay access to readable content

## 9. Haptic grammar

Haptics should be semantic and scarce.

Allowed examples:
- bookmark settled
- important mode change
- meaningful threshold confirmation

Not allowed:
- every tap
- scroll ticks everywhere
- decorative buzzing

## 10. Narrative density

| Surface | Narrative density |
|---|---:|
| Archive | High |
| Library | Medium |
| Book detail | Medium |
| Threshold transitions | High but brief |
| Reader chrome | Very low |
| Reading canvas | Near zero |
| Manga artwork canvas | Near zero |
| Settings | Low |

## 11. Core product rule

**Outside the story, Veil may speak. Inside the story, Veil falls silent.**

## 12. First implementation order

1. semantic design tokens
2. Theme Lab
3. Reader Sanctuary chrome
4. Reader quick/advanced appearance
5. Archive identity
6. Library system
7. world / progression layer
8. Manga Hub after shared system is stable

## 13. Acceptance gate for every new component

A component should ship only if applicable answers are yes:

1. Does it improve reading, navigation or comprehension?
2. Does it feel recognizably Veil rather than generic?
3. Does it avoid narrative tax?
4. Can it disappear when content needs the screen?
5. Is it accessible and understandable without visual atmosphere?
6. Is it reversible?
7. Does it preserve performance?
8. Does it work in RTL and large text where relevant?
