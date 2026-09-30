# W26 Threshold — Arena Acceptance Spec

Date: 2026-09-30
Canonical branch: `alpha/w26-arena-canonical-ui-rebuild-v1`

## Purpose

Threshold is not a dashboard. It is the liminal entrance between ordinary device space and the
reader's private archive.

The user should understand three things within one glance:
1. which volume is calling them back;
2. where the rest of their archive lives;
3. that Veil Reader has a distinct world without making reading harder.

## Composition hierarchy

### 1. Liminal header
- authored atmospheric image, never a generic gradient-only hero;
- deep blue-black architectural field;
- thin antique-brass frame;
- one central liminal seal that reads as a navigational/world symbol, not a game badge;
- restrained copy; title wins over lore;
- no floating rounded-card stack.

### 2. Current artifact
- current book is the dominant interactive object;
- dark archive shell, not a giant parchment card;
- cover is the physical artifact;
- progress is precise and quiet;
- parchment is reserved for the primary reading action;
- one primary action only.

### 3. Recent volumes
- behave like objects resting on a shelf/plinth;
- no boxed card behind every volume;
- title + state + progress are enough;
- horizontal browse remains fast and obvious.

### 4. Whisper
- optional memory/prompt appears as marginalia/archival trace;
- never visually stronger than the current volume;
- saved passage can return directly to its locator.

### 5. Mirror
- presented as one unusual artifact, not another rectangular dashboard tile;
- count is supporting information;
- interaction remains explicit and accessible.

### 6. Reading record
- a ledger, not a gamification panel;
- streak, time and finished books use typographic hierarchy and rules;
- Castle is a secondary destination.

## Material law

- **World shell:** void / archive / iron / antique brass / rare oxblood.
- **Reading action:** parchment.
- **Reader canvas:** independent Sanctuary material system.
- **Gold:** linework, hierarchy, focus and ritual emphasis; never a blanket fill.
- **Oxblood:** sparse seal/alert/accent only.

## Motion law

- entrance reveals may establish depth once;
- no perpetual particle spectacle;
- no decorative bounce;
- reduced-motion mode must preserve hierarchy without relying on movement;
- reading CTA responds faster than atmospheric transitions.

## Accessibility

- primary touch targets >= 48dp;
- text cannot sit on uncontrolled bright imagery without an overlay;
- Persian/Arabic uses shaping-safe typography and zero tracking;
- screen must survive large font scaling without hiding the primary reading action;
- progress state cannot be encoded by color alone.

## Rejection conditions

Reject the implementation if any of these are true:
- looks like a generic Material dashboard with a gothic skin;
- every section is a rounded rectangle;
- parchment dominates the shell;
- lore competes with the book title or Continue action;
- progress/game systems become more prominent than reading;
- atmosphere reduces legibility;
- RTL or large text changes the intended hierarchy;
- visual work modifies Reader/data/reliability ownership without a proven need.

## Exit gate for W26

W26 is visually complete when:
- Threshold hierarchy matches this spec;
- Arena tokens are the single source of truth;
- source review finds no Reader/data ownership change;
- Android CI failure, if any, is separated into infrastructure vs code evidence;
- the next canonical branch can start from W26 without inheriting a second visual system.
