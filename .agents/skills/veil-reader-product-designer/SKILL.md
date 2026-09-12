---
name: veil-reader-product-designer
description: Design Veil Reader screens and interactions that feel premium, calm, mysterious, accessible, and reading-first without letting gamification obstruct the book.
---

# Veil Reader Product Designer

Use this skill for UX, UI, interaction design, onboarding, navigation, reader chrome, Castle screens, visual-system decisions, and design critiques.

## Product constraints

1. Read `PRODUCT_BLUEPRINT.md` before proposing major interaction changes.
2. Keep the active reading surface calm and distraction-free. Game systems belong around the book, not over the text.
3. Preserve Veil Reader's identity: refined, mysterious, original, and modern rather than a clone of another reading app or game.
4. Make progression visible through the surrounding world: library state, Castle rooms, rituals, collections, and long-term changes.
5. Avoid dark patterns: no punishment for missed days, no forced streak anxiety, no paid XP, and no interruptive reward effects while reading.
6. Treat accessibility as part of visual quality. Check contrast, touch targets, scalable text, screen-reader semantics, motion sensitivity, and focus order.
7. Reader controls should be discoverable but visually quiet. Prefer progressive disclosure over persistent clutter.
8. Respect EPUB and PDF differences instead of pretending they have identical capabilities.

## Design workflow

1. State the user goal and context: reading, library management, annotation, progression, or recovery.
2. Identify the minimum UI needed to complete that goal.
3. Check interruption risk: if the user is actively reading, defer nonessential feedback.
4. Check one-handed Android ergonomics and small-screen behavior.
5. Define states: loading, empty, success, error, offline, restored, and accessibility behavior where relevant.
6. Reuse the existing visual language before introducing a new pattern.
7. When recommending animation, specify purpose, duration class, and reduced-motion behavior.

## Review questions

- Does this make reading easier or merely add spectacle?
- Can the same goal be achieved with less chrome?
- Is the interaction understandable without tutorial text?
- Does the Castle feel connected to actual reading activity?
- Is the design original rather than derivative?
- Does it remain usable with large text and TalkBack?

Prefer precise component-level recommendations over vague aesthetic adjectives.
