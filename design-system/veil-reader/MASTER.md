# Veil Reader — Master UI System

## Product character
Veil Reader is a quiet, premium reading utility wrapped in a mysterious fantasy world. The interface must feel calm during reading and atmospheric everywhere else. Never trade readability for decoration.

## Platform contract
- Native Android only.
- Kotlin + Jetpack Compose + Material 3.
- Use Material 3 adaptive APIs for phone, tablet, desktop-window, and foldable layouts.
- 21st.dev is a pattern/inspiration source only; React/shadcn code must never be copied into runtime code.
- UI UX Pro Max guidance must use the `jetpack-compose` stack.

## Existing visual identity — preserve
The current theme is the source of truth unless a deliberate redesign changes it:
- Ink / Obsidian / Slate surfaces for dark mode.
- Parchment / Warm Paper surfaces for light mode.
- Amethyst = primary action / magic / focus.
- Old Gold = achievement / rarity / progress.
- Jade = success / growth / calm secondary accent.
- Serif display/headline typography for story/world atmosphere.
- Sans-serif body/control typography for clarity.
- Rounded shapes: 8 / 12 / 16 / 24 / 32 dp family.

## Experience hierarchy
1. Reading clarity.
2. Navigation predictability.
3. Library discoverability.
4. Progress and motivation.
5. World-building atmosphere.
6. Decorative motion.

## Layout
- Build mobile-first but adapt beyond phones.
- Prefer content-driven width constraints over full-width text on large screens.
- Reader text column should remain comfortable at expanded widths.
- Library grids may increase columns with available width.
- Use window-size/posture information rather than hard-coded device names.
- Respect display cutouts, system bars, IME, and fold hinges.

## Spacing
Use existing VeilSpacing as the base scale:
- 4: micro separation only.
- 8: compact control internals.
- 12: related items.
- 16: default component padding.
- 20–24: section rhythm.
- 32+: major separation.
Do not create one-off spacing values without a strong reason.

## Touch and interaction
- Primary touch targets should be at least 48dp in Compose.
- Never rely on hover.
- Icon-only actions require accessible labels/content descriptions.
- Reading gestures must not conflict with system back or selection behavior.
- Destructive actions require clear differentiation and recovery where practical.

## Typography
- Reader typography is user-controlled and independent from app chrome typography.
- UI body text should normally remain >=14sp; primary reading/configuration copy >=16sp.
- Respect system font scaling; no clipped controls at large font scales.
- Use serif for atmosphere, not for dense settings/forms.
- Avoid all-caps for Persian/Arabic and avoid letter-spacing tricks that break connected scripts.

## Accessibility
Required before UI work is considered complete:
- WCAG-style contrast discipline for text/icons.
- TalkBack semantics for interactive controls and state.
- Logical traversal/focus order.
- 48dp practical touch targets.
- No information conveyed by color alone.
- Large-font testing.
- Reduced-motion path for non-essential animation.
- State changes must have visible and semantic feedback.

## RTL and localization
- All new layout work must be RTL-safe.
- Use start/end, never left/right, for layout semantics unless physical direction is intentional.
- Mirrored navigation icons must follow Android conventions.
- Reader content direction follows publication/content metadata, not app-locale assumptions.
- Avoid fixed-width labels likely to fail in Persian/Arabic.

## Motion
Use existing VeilMotion tiers as the baseline:
- Quick ~150ms: direct state feedback.
- Standard ~250ms: ordinary transitions.
- Ceremonial ~480ms: rare world/progression moments only.
Motion must communicate hierarchy or continuity; do not animate everything.

## Component sourcing workflow
For a new UI feature:
1. Define the user outcome and screen state.
2. Check this MASTER file and any page override.
3. Use UI UX Pro Max with `--stack jetpack-compose` for implementation guidance.
4. Browse 21st.dev only for visual/pattern inspiration when useful.
5. Translate the chosen pattern into native Compose primitives and Veil tokens.
6. Verify dark/light, compact/expanded, RTL, font scale, and TalkBack semantics.

## Anti-patterns
- Generic AI-dashboard look.
- Glassmorphism over reading content.
- React/shadcn copied into the Android app.
- Excess gradients, glow, or blur in core reading flows.
- Tiny icon buttons.
- Fixed phone-only layouts.
- Hard-coded LTR assumptions.
- Gamification interrupting reading.
- Color/animation used as a substitute for hierarchy.

## Page overrides
- `pages/library.md`
- `pages/reader.md`
Page files may refine this system but should not silently contradict accessibility, platform, or RTL rules.


## World-class visual quality gate — mandatory

Veil Reader is not considered ready when it merely builds, passes tests, or has the requested features.
Every user-facing release must also pass a visual and interaction quality review against first-class reading products.

Reference bar:
- Apple Books: calm reader chrome, strong reading controls, restrained branding, excellent typography and accessibility.
- Kindle: deep readability controls, accessibility, familiar reader behavior.
- Kobo: discoverable reading settings, font/layout/page-transition control.
- Google Play Books: simple reading controls, brightness/color modes, one/two-page layouts.
- ReadEra: low-friction local reading and library simplicity.
- Moon+ Reader: advanced customization breadth without making the core reader impossible to use.

### Mandatory visual standards
- No generic AI-dashboard look.
- No decorative gradient, glow, glass, card, or shadow unless it improves hierarchy.
- Branding is restrained and subordinate to book content.
- One icon language, one shape family, one spacing scale, one type hierarchy.
- Every screen must have a clear primary action and obvious visual hierarchy.
- Empty/loading/error/offline states are designed states, not placeholder text.
- Dark and light modes must be equally intentional.
- Reader chrome must disappear cleanly and never compete with the page.
- Motion must feel purposeful, short, interruptible, and never block input.
- All touch targets are practical at >=48dp.
- RTL/LTR, large font scale, TalkBack, phone/tablet/foldable must be verified.

### Screenshot acceptance
Before a UI feature can be marked GREEN, capture and review the affected screens for:
alignment, spacing, typography, contrast, density, icon consistency, hierarchy, truncation,
RTL, light/dark appearance, and small/large-window behavior.

A green build with visibly poor UI remains YELLOW or RED.

### Reader experience baseline
The in-reader settings model should be capable of supporting:
font family, font size, font weight/bold, line spacing, word/character spacing,
margins, justification/alignment, theme/background, brightness, scroll/pagination,
page-turn style, publisher defaults, and adaptive one/two-column layout where appropriate.

Advanced accessibility targets include line guide/reading ruler, dyslexia-friendly font options,
TTS/read-along, reduced motion and strong screen-reader semantics.

### Product personality
Veil must feel premium, calm, mysterious and enjoyable — not childish, noisy or game-like while reading.
Fun belongs around the reading experience: library discovery, progression, Castle, achievements and optional delight.
The page itself remains sacred.
