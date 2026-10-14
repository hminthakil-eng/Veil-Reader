# Veil Reader — Anime Identity / Living Pixel City
**Status:** Proposed art direction; user idea + live Gemini 4 Argon critique, NOT implemented, NOT approved for merge.
**Date:** 2026-10-14
**Source:** live Gemini desktop conversation on Windows; response preserved as `E:\EyadStudio\Temp\Gemini_Pixel_City_Anime_Character_Response_20261014.txt`. The feedback is design advice, not a benchmark or verified working app.

## Design constraint from owner
The customizable player **must have high-quality anime character identity** while the **interactive town/environment is beautiful pixel art**. The character must react physically to and affect the pixel city, not be a detached UI mascot.

## Live Gemini critique (faithful synthesis)
Gemini warned that placing a soft-antialiased full-resolution anime character directly onto 32px pixel architecture without deliberate framing could produce obvious asset-style mismatch. It recommended **B — synchronized dual representation**:
- **Hero / close-up / customization / dialogue:** full-detail anime portrait/body view, expressive layered face and wardrobe.
- **Overworld:** recognizable matching 32x48 illustrative-anime/chibi pixel sprite with synchronized hair, outfit, skin and accessories, in a detailed atmospheric pixel environment.
- **City camera:** 3/4 overhead view instead of fully isometric, to limit directional outfit sprite costs; animation and collisions easier to author and debug.
- **Climate:** warm brass/amber lantern lighting vs dark Grayfog blue-gray buildings, restrained pixel fog, readable silhouettes and frame boundaries between high-res portrait and world sprites.
- **Town:** preserve four progression stages as locations/visual motifs of one continuously evolving world, rather than forcibly replacing the user's beloved campsite with a castle.
- **Playable 60s story:** after genuine reading -> enter foggy library courtyard -> sprite walks to cold lamp -> high-detail anime cut-in shows character igniting a match -> lamp illuminates pixel city -> change outfit/hat in both renderings -> one tap returns to saved book.

Five suggested emotional interactions: portrait-on-rest-bench, avatar reacting to city weather, artifact high-res inspection, restoration of ruins from previously earned reading progress, and short encounters with silent pixel companions.

**Gemini's technical direction:** prototype with native Kotlin/Compose 2D canvas, sprite atlases drawn with `FilterQuality.None`, and portrait UI drawn with smooth scaling; shared visual schema; no 3D engine dependency for first slice. Its code fragment is illustrative pseudocode containing unspecified `AvatarConfig`, sprite atlas, etc.; **NOT compilable source and NOT installed**.

## ChatGPT engineering critique / refinements
- Gemini's universal dismissal of Godot for *final product* is not yet backed by an Android device memory/app-size benchmark. Start with Compose sprite prototype, but retain later Godot-vs-Compose engine evaluation if complex NPC navigation or many animated entities are actually required.
- Do **not** enforce identical per-pixel hex hair values on the pixel sprite and anime image: the same **semantic color choice and recognizability** matter; hand-authored mapped palettes preserve pixel readability. A "16ms LUT" target is hypothetical, not validated.
- Do **not** promise GPU usage exactly zero in Reader: assert all world frame scheduling and world surface resources stop, with measured frametime, memory and power deltas relative to baseline.
- Existing `RagsToRichesAvatar` v1's five cosmetic fields are the **canonical data model**; both renderers are read-only projections. Add a new versioned `AvatarVisualCatalog` mapping from slots/IDs to anime layers and pixel atlas sprites. No new authoritative player appearance store.
- Existing Path/Rank retains **sole authority for unlocking castle rooms** (issue #314). Public map/district visual staging can remain unlocked separately; protected halls must remain rank-gated. Reuse #518 high-water stage only for home/cosmetic art, and never use it to bypass gates.
- No book quotes/highlights should be sent to the game world or 3rd-party engine without explicit user choice; avoid spoiler and privacy issues.
- On phone, per-frame world drawing must not mutate user persistence and must not run while reading. Preserve real Readium locator and unaffected PAPER_CURL/SLIDE/PAGED/SCROLL rendering.

## Minimal reversible slice acceptance proposal
1. A small pixel-art **library courtyard** with three interactive objects (lantern, bench, archive door) and readable visual style at phone resolution.
2. One canonical user avatar definition drives anime portrait and **pixel-world sprite** via pure translation catalog; change hair, cloak/outfit accent and verify no mismatch on reload and restore.
3. Map click -> path to lantern -> 4–8 frame action -> warm lantern pixel lighting; animate a **short framed anime cut-in** only when touched, not on idle.
4. Keep input and scene state owned by small World component; reading only supplies existing durable `GameRepository` counters; no double credits or new receipt semantics.
5. No login, no daily coercion, local assets all offline, bilingual Persian RTL/English; Test Back and exact reading-locator resume under airplane mode.
6. Quality gate: crisp integer-scaled pixel world, no blur; player silhouette recognizably unique after hair/cloak edits; anime/overworld identity consistent; 60s interaction on target low-end device; TalkBack semantic alternatives and reduced motion; repeat World->Reader at least 30 cycles and check no regression / memory retention.
7. First 2-week prototype does **NOT** include a full city, 3D avatar, complete NPC simulation, economy migration, procedural generation, companion AI, or a new page engine.

## Artistic benchmarks
- **Fields of Mistria:** stylized expressive portrait UI over compact pixel town; https://www.fieldsofmistria.com/
- **Eastward:** anime-influenced detailed pixel world with cinematic modern lighting; https://eastwardgame.com/media/

**Next:** present one storyboard/mockup sample for user approval before editing shaders or adding a game engine. This design memo does not replace the original Rags-to-Riches product scope without the owner's decision.
