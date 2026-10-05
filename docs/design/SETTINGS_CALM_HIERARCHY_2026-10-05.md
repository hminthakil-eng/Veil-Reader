# Settings Calm Hierarchy Pass — 2026-10-05

## Product problem

Settings already contains mature reader, accessibility, sensory, backup and world
controls, but repeated full-width brass dividers plus a large 300dp architectural
header made a utility surface feel heavier than it needed to.

## Change

- atmospheric image field: 300dp → 216dp;
- gradient field: 330dp → 244dp;
- page title: headline → title hierarchy;
- top registration rule: 92dp → 56dp;
- section dividers: full-width → short 44dp registration marks;
- section contents align directly to the page rhythm rather than adding another
  nested 8dp inset;
- semantic spacing aliases replace local scale names in the changed shell.

## Preserved

No setting is removed or hidden:
- Appearance
- Reading
- advanced typography
- tap grid
- hardware keys
- Focus Guide
- sound/haptics
- world visibility
- backup/restore/notebook export
- privacy/version
- debug Material Page review
- reset

Advanced reading disclosure remains unchanged.

## Intent

Settings should be calmer and more literal than Archive/Castle. The Veil identity
stays in typography, background and a small registration mark rather than repeated
ornamental framing.

## Acceptance

- Android CI
- English/Persian
- 100%/200% text
- TalkBack heading order
- all switch/button targets remain >=48dp
- no setting becomes unreachable
