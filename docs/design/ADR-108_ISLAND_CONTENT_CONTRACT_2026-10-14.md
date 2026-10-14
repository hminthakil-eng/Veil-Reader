# ADR-108 — Island Content Contract and Offline Production Pipeline

**Date:** 2026-10-14
**Status:** implemented validator prototype / runtime integrations and art assets NOT YET implemented
**Branch:** `game/island-persistence-infra-20261014`, stacked on `game/island-core-v1-20261014`
**Scope:** Fictional island RPG content ONLY. Not EPUB, PDF, locators, user notes or private real-library files.

## Decision
The game requires a **versioned, engine-neutral content interchange** instead of embedding maps, NPC dialogue and visual IDs in Compose implementation classes. The canonical prototype root is `game-content/island/v1`. The source-of-truth data is static original fiction authored in JSON and validated BEFORE an Android build.

Files:
- `manifest.json`: root schema version, world ID, scene IDs and file references, bilingual dialog files and avatar/art catalogs.
- `scenes/*.json`: small finite grid with # blocked / . walkable collision rows, named spawns, globally unique interactable/NPC/portal IDs, stable art references and dialogue pointers.
- `dialogues/fa.json` and `en.json`: user-visible authored text; node/choice IDs and next pointers MUST match across locales.
- `art_catalog.json`: art ID, medium and planned/ready status. Ready files must exist and be local image resources. Planned assets are explicitly NOT a finished game asset or a release-ready state.
- `avatar_catalog.json`: one logical player variant references an anime portrait ID plus a separate matching pixel-world sprite ID. Future outfit/hair ID variations inherit appearance from existing `AvatarAppearanceStore`; this content package cannot become the authority for user cosmetics.

## Editor and renderer separation
Author 2D level geometry in **LDtk** or **Tiled** after a map-editor spike. Their native data structures are NOT this simplified engine-neutral schema. A future versioned converter will normalize export into the present world contract and preserve stable IDs; it must report unsupported entities/layers rather than quietly dropping them.

Suggested runtime:
```
 LDtk / Tiled (art source) -- converter (future) --> normalized JSON world
 illustrator + sprite atlas (licensed) -----------> art catalog references
 authors -> bilingual NPC/quest JSON -------------> local dialogue pack
                                              |
             tools/island_content/validate.py + unittest
                                              |
                     signed/versioned Android packaged assets (future)
                                              |
            2D Compose renderer OR engine-tested Godot/libGDX port
```

The current prototype does not include converter or compiled sprite assets; do not claim a working LDtk import or a visual walkable pixel city yet.

## Implemented content gate
- Checks deterministic manifest/world/scene IDs, path traversal and missing files, scene dimensions / tile size, collision rows and spawn traversability.
- Computes BFS flood fill to reject isolated required spawns or unreachable interactive objects; catches broken scene portal or target spawn.
- Rejects missing dialogue references, missing locale nodes, inconsistent choice IDs/outgoing branches and invalid strings.
- Validates one canonical avatar variant maps to both portrait and pixel art asset IDs; rejects undefined art refs.
- Protects new level files from certain private-reader source fields (not a complete privacy audit); never introspects uploaded user books.
- Allows **planned** art only for development; `--release` explicitly fails any unresolved planned placeholders.
- Stdlib Python 3.11 only, no network access or paid tools, automatic GitHub content validation workflow.

Development:
```
python -m unittest discover -s tools/island_content/tests -v
python tools/island_content/validate.py --root game-content/island/v1
# EXPECTED FAILURE until original pixel/portrait assets exist:
python tools/island_content/validate.py --root game-content/island/v1 --release
```

## Two-model studio ownership
1. **Gemini** independently proposes alternate map/quest architecture, sprite-atlas/palette schema, game UX and failure modes; cite docs, avoid fake verified claims.
2. **ChatGPT** chooses reversible source contracts, integrates data into the active feature branch, runs validator/Gradle/Android tests, checks Reader isolation, reviews Gemini changes.
3. Neither agent edits authoritative Reader progress or unlocks from a renderer callback. Only signed-off repository writers persist economy/event changes.
4. Stable IDs are append-only once a content pack is shipped. Content changes need versioned migrations and bilingual dialog validation.
5. Repo is canonical: source, accepted ADR, issue, QA evidence, reviewer objections and rollback. Real production changes require GitHub review plus device verification.

## Safety and release caveats
- **No original art yet**: seven current catalog entries are intentionally planned placeholders.
- Scene geometry exists as lightweight fictional level fixture, not a fully decorated imported tilemap.
- Content validator is an infrastructure gate, NOT a security sandbox for untrusted mods, a complete pixel renderer, or proof of Reader perf.
- User book content, full titles, URI, locator and highlights remain under native Reader. Do not expose them in game content or internet-connected services.
- Existing castle Rank/Path and `GameRepository` cannot be duplicated into this pack. Island shop economy needs a separate atomic persistent store in the neighboring game branch.
- Existing Lang RTL text and the pixel-grid coordinate space differ: world X/Y must not flip automatically when locale changes; UI dialogue/focus does adapt.
- Release expects device image/art benchmark, sound and licensing review, 30 World↔Reader transitions, missing asset fallback, TalkBack and large fonts.

## Research baseline and official documents
- LDtk JSON format: https://ldtk.io/json/
- Tiled editor and TMX/JSON formats: https://www.mapeditor.org/docs/manual/reference/json-map-format/
- Godot Android library embedding (single-instance and orientation cautions): https://docs.godotengine.org/en/stable/tutorials/platform/android/android_library.html
- libGDX tile maps: https://libgdx.com/wiki/graphics/2d/tile-maps
- Android offline-first architecture: https://developer.android.com/topic/architecture/data-layer/offline-first
- Android Room migration testing: https://developer.android.com/training/data-storage/room/testing-db

## Next ADR gates
ADR-109: authoritative island-economy transactions and export/restore, owner game/island-durable.
ADR-110: pixel-art pipeline quality target and shared anime/pixel avatar catalog; requires REAL art.
ADR-111: renderer selection, after identical 2D map player+NPC+quest prototype Compose vs Godot, memory, 30 Reader handoffs and on-device measurements.
