# Arrodes — standalone prototype

This prototype is intentionally isolated from Veil Reader production code. It is a visual/interaction laboratory for the Home memory mirror before any app integration.

## Locked direction

- Premium, restrained and mysterious; never game-like or permanently animated.
- Dark reflective glass, aged brass/bronze detailing and subtle celestial geometry.
- The mirror spends most of its life silent and visually close to a real dark mirror.
- Saved notes appear as if ash gathers into the actual letterforms, remain readable, then dissolve back into silence.
- A readable fragment is actionable: activating the mirror again emits an intent to return to the exact book location.
- Rendering and motion remain independent from Veil Reader storage, Reader engines and Home architecture.

## Interaction states

`idle → awaken → coalesce → manifest → hold → dissolve → idle`

The production rotation interval is 30 minutes. Add `?demo=1` to the URL for an 18-second demonstration loop.

- While silent, tap/click/Enter/Space awakens the mirror and requests another fragment.
- During `hold`, activating the mirror emits `arrodes:open-source` with the current fragment ID, kind and locator.
- The dedicated summon button requests another fragment only when no sequence is already running.

## V2 manifestation model

The ash effect is text-aware rather than decorative:

1. The current note is rendered into an offscreen canvas mask.
2. Opaque glyph pixels are sampled into a capped target set.
3. Particles enter from the mirror perimeter and converge on those glyph targets.
4. The real accessible text fades in over the formed ash silhouette.
5. On dissolve, particles begin on glyph targets and disperse beyond the mirror edges.

This keeps the visual effect tied to the content rather than placing generic particles around it.

## RTL and variable-length notes

- Arabic/Persian/Hebrew-range scripts are detected and rendered RTL.
- The quote card changes language/direction metadata for assistive technology.
- Short, medium and long fragments use different type scales.
- A Persian sample fragment is included so RTL is exercised in the standalone lab before integration.

## Accessibility and performance

- Keyboard activation (`Enter` / `Space`).
- Focus-visible treatment around the physical mirror silhouette.
- A separate polite live region announces user-requested fragments and source-return intent without making passive 30-minute rotation chatty.
- `prefers-reduced-motion` disables particle animation and collapses transition motion while preserving useful reading time.
- Particle count is capped and canvas device pixel ratio is capped at 2.
- The sequence aborts cleanly while the page is hidden and restarts scheduling when visible again.
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

The mirror needs only two app-facing intents:

```ts
onRequestNextFragment()
onOpenSource(locator)
```

The standalone web lab currently represents the second intent as a DOM custom event:

```js
window.addEventListener('arrodes:open-source', ({ detail }) => {
  // detail.locator is handed to the eventual EPUB/PDF adapter.
})
```

## Integration gate

Do **not** merge this prototype into production Home until:

1. motion feels premium at 60/90/120 Hz;
2. particle density stays smooth on target Android hardware;
3. light/dark contrast and text readability pass accessibility review;
4. empty-note, very-long-note, RTL and Persian text states are visually approved;
5. exact-locator return is proven independently for EPUB and PDF;
6. reduced-motion behavior is approved;
7. the component API remains independent of Veil Reader storage implementation;
8. Home integration has a no-animation fallback and does not regress launch or scroll performance.
