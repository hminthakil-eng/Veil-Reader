# Veil Reader 0.9 — Design System

## Purpose

Veil Reader should feel like a refined personal archive that slowly reveals a mysterious world. The reading surface remains calm; atmosphere belongs primarily to the surrounding app shell, Library, Castle and Path experiences.

This document is the visual contract for the 0.9 “The Awakening” work.

## Visual vocabulary

### Core materials

- **Ink / Obsidian** — primary dark world surfaces.
- **Parchment / Warm Paper** — light-mode world surfaces.
- **Moonlight** — primary readable foreground.
- **Amethyst** — Path / Veil identity accent.
- **Old Gold** — rituals, earned milestones and important secondary emphasis.
- **Jade** — knowledge, success and calm tertiary emphasis.
- **Ash line** — quiet boundaries; avoid heavy card shadows.

Use accents sparingly. A screen should not look like a game inventory or neon fantasy dashboard.

## Typography

- Serif display/headline/title styles give the world literary character.
- Sans-serif body/label styles keep controls and metadata highly readable.
- Do not reduce body text below established readable sizes to fit more information.
- Eyebrows are short uppercase labels with tracking; they identify place/context, not decoration.

## Surfaces

- Prefer layered low-contrast panels over generic elevated cards.
- Use hairline outlines rather than strong drop shadows.
- The active Reader is exempt from decorative world surfaces; page themes own the reading background.
- One primary visual hierarchy per screen. Avoid card-inside-card nesting where a divider or spacing solves the same problem.

## Shape language

- Small controls: 8–12dp radius.
- Standard containers: ~18dp.
- Major world panels: ~26dp.
- Large ceremonial surfaces may reach ~34dp.

Rounded shapes should feel carved/soft, not bubbly.

## Spacing

The base rhythm is 4 / 8 / 12 / 16 / 20 / 24 / 32dp. Prefer these shared values before inventing one-off spacing.

## Motion

Three duration classes are defined:

- **Quick (~140ms):** local feedback and tiny state transitions.
- **Standard (~220ms):** ordinary navigation/control transitions.
- **Ceremonial (~520ms):** rare Path/Castle moments only.

Reduced-motion behavior must remain meaningful. No essential state may depend on animation.

## Accessibility rules

- Interactive targets should remain at least 48dp where applicable.
- Never encode state only through color.
- Decorative art should not create noisy TalkBack output.
- Large text must not hide primary actions or reading progress.
- Contrast matters more than atmosphere.
- Motion must degrade gracefully.

## Reader boundary

The 0.9 design system must never make active reading busier. No ambient particles, Castle animation, quests or reward overlays belong over EPUB/PDF content. Reader chrome should become quieter as world screens become richer.

## Implementation status

0.9A introduces:

- shared dark/light Veil color schemes;
- literary typography hierarchy;
- shared shapes, spacing and motion tokens;
- upgraded `ScreenHeader`;
- upgraded `MysteryCard` atmospheric panel;
- refined real/fallback book-cover framing.

These are intentionally high-leverage primitives. Subsequent 0.9 slices should reuse them rather than creating local visual constants.

## Next visual slice

0.9B should redesign **Reading Now into The Threshold** using this system:

1. one dominant current-book composition;
2. quiet reading progress and resume action;
3. Path state reduced to supporting context;
4. quests demoted from dashboard prominence;
5. first subtle connection to the evolving Castle;
6. accessible empty/loading/error states.
