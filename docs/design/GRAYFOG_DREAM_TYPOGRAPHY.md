# Grayfog Dream Typography

## Purpose

Typography is part of Veil Reader's product identity, not decoration. The app shell must remain readable, calm, and editorial even when the device uses a custom system font.

## Bundled families

- Display: Playfair Display variable font.
- UI: Inter variable font.
- Lore: Cormorant Garamond Italic variable font.

All three are bundled for offline use. Their OFL license texts live under `app/src/main/assets/licenses/`.

## Hierarchy

- Major titles, book titles, chamber names, rank names: Display.
- Body copy, controls, navigation, metadata, settings, counters: UI.
- Clues, invocations, discoveries, ritual whispers: Lore.

## Hard rule

Lore type is an accent. It must not become the default voice of the interface. Target usage is roughly 10-15% of visible text on world-shell screens and near zero in the Reader chrome.

## Reader boundary

Publication typography remains independent. Bundled shell fonts must never override publisher text or user-selected reading fonts inside EPUB/PDF content.

## Quality gate

A screen fails typography QA when:
- system/device font styling leaks into Veil UI;
- more than one-third of functional UI copy uses Lore styling;
- major titles lose contrast against world surfaces;
- button or navigation labels use Display or Lore instead of UI;
- dense screens sacrifice legibility for atmosphere.
