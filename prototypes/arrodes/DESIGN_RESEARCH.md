# Arrodes — design research notes

This document records interaction principles borrowed from the broader smart-mirror / interactive-mirror medium. It is deliberately **not** a code-import plan and does not copy another product's visual identity.

## References reviewed

- MagicMirror² — modular smart-mirror platform and the basic pattern of a display living behind a reflective surface: https://docs.magicmirror.builders/
- Cornell Interactive Mirror — direct tap/swipe interaction layered onto a mirror: https://courses.ece.cornell.edu/ece5990/ECE5725_Spring2017_Projects/Interactive%20Mirror/interactive_mirror.html
- Rob Tieben, Magic Mirror — an ambient mirror/projection whose visual effects change and invite lightweight interaction: https://robtieben.com/projects/magicmirror/
- Reverse UI, Particle Text Reveal — demonstration that particles can converge onto actual letterforms rather than remain generic decoration: https://reverseui.com/components/particle-text-reveal

## Principles worth keeping

### 1. Reflection first, interface second
The default state should read as an object, not a dashboard. Arrodes should look dormant until it has something worth saying.

**Applied:** permanent nebula/orbit motion was removed from the silent state. Reflection becomes dominant while idle.

### 2. Activation should feel causal
Interactive mirrors work best when the user can understand that their action woke the surface. A small shimmer or luminance response is enough; a large fantasy animation makes the object feel like a game menu.

**Applied:** a short awaken state precedes ash coalescence.

### 3. The effect should belong to the content
Particles are more convincing when they form the content itself.

**Applied:** the prototype rasterizes each note into an offscreen glyph mask and samples real letter pixels as particle targets.

### 4. The mirror should lead somewhere
A beautiful quote is more useful when it becomes a doorway back into the book.

**Applied:** while a fragment is readable, activation emits an `arrodes:open-source` intent carrying the source locator.

### 5. Ambient does not mean constantly moving
The Home screen should remain calm. Motion is reserved for state change and discovery, not used as wallpaper.

**Applied:** the production cadence remains one random fragment every 30 minutes and idle is nearly motionless.

### 6. Accessibility is part of the illusion
Reduced motion should remove spectacle, not remove meaning or reading time. Passive auto-rotation should also avoid repeatedly interrupting screen readers.

**Applied:** particle motion has a reduced-motion fallback, the quote itself is not an always-on live region, and a separate polite status region is used for user-initiated feedback.

## Deliberately rejected ideas

- Camera / face recognition: unnecessary for a reading Home screen and introduces privacy + battery cost.
- Voice assistant behavior: changes Arrodes from a memory object into a chatbot and competes with reading.
- Continuous particle fields: visually expensive and destroys the contrast between sleep and manifestation.
- Minigames, gesture menus or dense mirror widgets: wrong product role for Veil Reader.
- Direct dependency on Room, Readium, PDF navigation or sync: integration must happen through a thin adapter only after the standalone component is approved.

## Next visual tests

1. Compare 3 ash densities on a real high-refresh Android device.
2. Tune the 600–900 ms awaken cue until it is noticeable but not theatrical.
3. Test Persian paragraphs at 60–140 characters.
4. Test bright and very dark Home backgrounds behind the module.
5. Verify that the source-return affordance is discoverable without adding permanent chrome.
