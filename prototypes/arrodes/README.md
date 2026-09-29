# Arrodes — standalone prototype

This prototype is intentionally isolated from Veil Reader production code. It is a visual/interaction laboratory for the Home memory mirror before any app integration.

## Locked direction

- Premium, restrained, mysterious; never game-like or permanently animated.
- Dark reflective glass, aged brass/bronze detailing, subtle celestial geometry.
- The mirror spends most of its life silent.
- Saved notes appear as if ash gathers into language, remain readable, then dissolve back into silence.
- A note always carries its source locator so a later Veil Reader adapter can jump back to the exact book location.

## Interaction states

`idle → activate → gather → reveal → hold → dissolve → idle`

The production rotation interval is 30 minutes. Add `?demo=1` to the URL for an 18-second demonstration loop. Tapping/clicking the mirror or the summon button triggers the sequence immediately.

## Accessibility and performance

- Keyboard activation (`Enter` / `Space`).
- `aria-live` for manifested note text.
- `prefers-reduced-motion` collapses motion timings and disables particle animation.
- Particle canvas caps device pixel ratio at 2.
- Animation pauses while the page is hidden.
- No external images, fonts, libraries or network calls.

## Future Veil Reader adapter contract

The visual component should receive a domain-neutral fragment object:

```ts
export type ArrodesFragment = {
  id: string
  text: string
  bookTitle: string
  kind: 'note' | 'highlight' | 'quote'
  locator: {
    bookId: string
    href?: string
    progression?: number
    positions?: number[]
  }
}
```

The mirror emits only two app-facing intents:

```ts
onRequestNextFragment()
onOpenSource(locator)
```

This keeps rendering/animation independent from Room, Readium, PDF navigation, sync, gamification, or Home screen architecture.

## Integration gate

Do **not** merge this prototype into production Home until:

1. motion feels premium at 60/90/120 Hz;
2. particle density stays smooth on target Android hardware;
3. light/dark contrast and text readability pass accessibility review;
4. empty-note, long-note, RTL and Persian text states are designed;
5. the exact-locator jump is proven for EPUB and PDF;
6. reduced-motion behavior is approved;
7. the component API remains independent of Veil Reader storage implementation.
