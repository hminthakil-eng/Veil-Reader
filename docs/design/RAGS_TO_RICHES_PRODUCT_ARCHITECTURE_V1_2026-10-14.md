# Veil Reader — Rags to Riches / Premium Game Reader Architecture v1
**Status:** design proposal, architecture-first / NOT implementation approval  
**Date:** 2026-10-14  
**Branch:** `design/premium-reader-world-v1-20261014`  
**Partners:** ChatGPT architecture + integration; Gemini 4 Argon High peer architecture, Kotlin/game code, graphics and independent critique.

## North star and boundaries
A reader-first, offline-first reading app fused with an optional *rags-to-riches* progression world: a humble traveler with a worn book and tent grows into a cultured castle lord through real sustained reading. The fantasy 3D anime universe is aspirational, refined and coherent: dark navy archival shell, parchment reading canvas, muted antique brass, diffused Grayfog, disciplined typography, fine cloth/leather/stone/wood materials and expressive stylized faces. No infantilizing gacha aesthetic, lootboxes, pay-to-win, fake taps, forced check-ins or distracting effects inside a book.

Quality ambition: credible polished commercial product; **no claim** of matching large-company quality without independent performance/device/user acceptance. Reuse functional Readium / PDF / manga / TTS / Room persistence and distinct PAGED / PAPER_CURL / SLIDE / SCROLL engines, never reset current users' progress.

## Product architecture / information architecture
- **Sanctuary / Home:** continue last book above the fold; recent shelf; character/world peek below reading; one-tap navigation.
- **Reader:** the calm, fully native focused reading surface; bookmarks, notes, highlights, book-specific appearance, PDF controls, optional TTS dock. Nothing from world runtime during page-turn or TTS speech.
- **Archive:** retrieval-first library; authors, series, collections, status, search, formats including comics/audiobooks.
- **World:** explorable homestead to grand castle; construction, unlocks, furnishing, spaces for achievements/lore, light narrative encounters.
- **Character:** modular 3D anime avatar creator; presets, face/hair/eyes, skin palettes, physical traits where appropriate, outfits, garments, accessories, colors, expressions, animation states and 360° preview.
- **Identity:** reading statistics, accessibility, badges, achievements and optional identity-sharing later.
Navigation rule: primary nav reading / library / world / profile; avatar customization reached from world and profile; no extra noisy navigation tab; reader is immersive modal/destination and can return immediately.

## Core loop
1. Open book; read normally with actual Readium/renderer-derived session evidence.
2. Persist durable reading progress and session accounting locally (single owner) before acknowledging progression.
3. Project eligible reading into **WorldGrowth** and **CharacterUnlocks**; journal unlock events idempotently.
4. On exit/return from reader, show quiet optional world change, new decor, outfit or narrative discovery.
5. Player chooses where to place/equip rewards, then resumes reading; never gate access to books.

**Progression path** (narrative names/art stages, not final economy):
- Tier 0 — **Wanderer**: worn cloak, notebook, tent, campfire.
- Tier 1 — **Apprentice**: rebuilt hut, simple wardrobe, desk and shelves.
- Tier 2 — **Scholar**: house/manor, personalized library, workshop and wardrobe.
- Tier 3 — **Keeper / Lord**: castle, observatory, archive wings, elaborate outfits.
- **Endgame:** personalization and collections, not an exploitative infinite grind; no zero-sum PvP.
Current pure function `deriveReadingHomeProgression(minutesRead, booksFinished)` has CAMP/HUT/HOUSE/CASTLE thresholds 0/15/120/600 and a bounded finished-book boost. Those thresholds are **existing prototype settings**, not approved final balancing. Adapt only behind versioned balance configuration with unit tests and migration-safe projections.

## System-of-record architecture
```text
Readium / PDF / Manga / Audio -> Session Evidence Writer (durable Room ledger)
                                      |
                                      v
                              EligibleReadingProjector
                                      |
              +-----------------------+-------------------------+
              v                                                 v
     WorldProgressionReducer                            AvatarUnlockReducer
              |                                                 |
       WorldSnapshot/Events                              CosmeticEntitlements
              +-----------------------+-------------------------+
                                      v
                              Read-only UI State
                                      |
                               Renderer Port
                                      |
                        Compose UI + optional 3D scene
```
Rules:
- Reader owns locator, publication, session and input. No gamification component writes the reader locator.
- Reward reducers are pure and deterministic; writes occur only in a transactionally durable repository on unique event identifiers.
- UI recomposition NEVER credits minutes, currency, XP, book completion or unlocks.
- Anti-farming: debounced taps and background idle cannot count as sessions; no OCR or arbitrary page turn XP. Keep the existing verified-reading accounting contract. Avoid reading-speed policing that harms accessible reading.
- Offline-first; sync optional and conflicts resolved at durable event / monotonic entitlement level. No new account required.
- Local backup/export contains schema version, avatar IDs, outfit slots, world placements and entitlements. Never store just rendered images as authoritative avatar state.

### Proposed domain contracts (planning, not yet code)
`ReadingEvidence { sessionId, bookId, elapsedActiveMs, committedLocatorRef, sourceType, recordedAt, ledgerVersion }`  
`WorldState { schemaVersion, growthTier, unlockedAreaIds, placedFurnitureIds, lastAppliedEventId }`  
`AvatarBlueprint { schemaVersion, presetId, morphPresetIds, paletteIds, outfitSlotAssetIds, expressionPreset, poseId }`  
`GameProgressionEvent { id, sourceSessionId, kind, tierBefore, tierAfter, grantedAssetIds, createdAt }`  
`VisualSceneState { tier, sceneVariant, timeOfDay, decorationLayout, avatarBlueprintRef, reducedMotion }`
Use stable canonical asset IDs; do not serialize engine nodes or transient meshes. Renderer consumes a *view projection*, not domain entity objects.

## 3D / character / world rendering boundary
Current stable Android app remains Kotlin + Jetpack Compose. **3D is an optional isolated rendering feature**, never a dependency of Reader.
Interfaces:
- `CharacterPreviewPort.render(AvatarBlueprint, Pose, QualityTier)`
- `WorldScenePort.render(VisualSceneState, QualityTier)`
- `AssetCatalogPort.lookup(assetId, version)`
- `SceneLifecyclePort.pause/resume/release()`
Start with a deterministic premium 2.5D fallback/static sprites; do a small technical spike before selecting 3D runtime:
1. **Filament + glTF 2.0**: compact Android PBR rendering and controllable Compose/Surface integration; assess rigging/animation/mesh clothing as custom work.
2. **Embedded Godot Android library**: mature scene/state/animation tools, but lifecycle, AAR size, cold start, resource competition and bidirectional state bridge must be benchmarked.
Engine choice requires proof on target low/mid/high Android devices: 60/120 Hz frametime, GPU memory, initial scene loading, suspend/resume, app binary growth, Android accessibility/focus handoff, and **zero degradation in paper turn**.
Prefer glTF 2.0 avatar/material/interchange with carefully tracked license rights. Evaluate VRM 1.0 where useful for humanoid skeleton/expression metadata; do not assume a renderer supports every VRM extension automatically.

## Art and animation production architecture
Character:
- one canonical proportion/silhouette sheet; matching male/female/androgynous variants without hardlocked appearance; modular meshes around a shared rig and skeleton.
- expression/pose states: IDLE, READING, INSPECT, WALK, WORK, CELEBRATE, REST; animation controller uses finite state machines; never random uninterruptible autoplay in reading.
- customization: face, eyes, hair, palette, garments, boots, cloak, bag, accessories, recolor/material presets. User can always edit again; no real-money lock on basic identities.
- outfit legality: slot compatibility, material drawcall budgets, no clipping with locomotion, optional LODs, versioned manifests, previews.

World:
- 4 hero silhouettes correspond to the existing CAMP/HUT/HOUSE/CASTLE progression, but actual structures evolve through object-level assets: furniture, lighting, shelves, lanterns, windows, doorway, towers.
- 3D scenes loaded on demand only in World and avatar editor; Home uses a static or cached preview by default.
- deterministic world layout from saved placed decoration IDs; variant variants tied to tier and access.
- no repeated assets between states unless intentional lore continuity; world should visually tell character's increasing sophistication.

Motion grammar:
- micro / utilitarian transitions ~90-180ms;
- spatial transitions ~220-360ms;
- rare graduation ritual ~500-900ms;
- subtle at-rest motion limited to World; obey accessibility reduced-motion and battery saver; pause while screen off/background.
- no infinite particles, real-time blur/glow shaders, huge transparent overdraw or per-frame heap allocations on low-tier hardware.
- quiet reward queued for **after leaving the reader**; optional ability to skip celebration entirely.
- Persian RTL, screen-reader labels for controls and scene descriptions, large font UI and one-handed controls are acceptance targets.

## Architecture of delivery / ADRs
ADRs before implementation:
1. ADR-001 reader isolation: renderer interfaces cannot import any Readium navigation type; game state receives snapshot/repository only.
2. ADR-002 persistence: durable idempotent evidence and entitlements, stable save migrations and integrity tests.
3. ADR-003 rendering engine: benchmark Filament vs Godot before dependency selection; allow no-3D fallback.
4. ADR-004 asset pipeline: Blender + glTF/VRM candidates, licensed provenance, LOD, texture atlas, rig validation, build-time manifests.
5. ADR-005 game rewards: no cash-pressure mechanics, paywall of books, farming by fake taps or streak punishment.
6. ADR-006 state model: one writer per domain, testable reducers, unidirectional data flow to Compose.
7. ADR-007 performance/accessibility budgets: tiered assets and reduced-motion/fallback from day 1.

## Feature priority / milestone gates
**Architecture gate (P0, next):** navigation map, state ownership, entity schema, renderer contract, avatar art bible, world bible, reduced-motion rules, test plan; Gemini independent red-team critique. No new 3D SDK before this gate.

**Vertical slice 1 (P0):** 1 avatar preset with adjustable hair, eyes and outfit; CAMP world scene; one *verified* reading event -> preview progress -> single unlock; save/relaunch; Persian/English; low-end quality fallback. Proven screenshots/video and unit/instrumented tests.

**Vertical slice 2 (P1):** all four tiers (temporary art permitted), camera and character customization, modular wardrobe, decor placement, transition state machine, robust recoverable migrations and bench results.

**Reader excellence parallel (P0):** page-turn first page, hold/reverse/cancel, RTL, locator recovery, PDF zoom, TTS stop/resume correctness, per-book appearance, library query performance. Player-facing world must not block these gates.

**Scale after proof (P2):** multiple environments, rich cosmetic catalogs, expressive animation blends, companions, narrative lore, curated world seasonal art.

## Acceptance / measurable quality
- Reading and app launch remain usable with game graphics disabled, absent or corrupted.
- Crash-safe migration of existing user library/game/profile; backups round-trip and do not mutate original books.
- One session event => at most one reward; no duplicate on app restart, process death, navigation recomposition or offline reconnection.
- Character customization survives process death and device restart; no equipped item references missing assets.
- Animations cancel correctly; stop all offscreen rendering; no animation gesture input latency added inside Reader.
- Visual QA: 4 tiers, 3 quality settings, narrow phone/tablet, light/dark shell, Persian RTL/English, large text and TalkBack.
- Physical performance and power usage measured, not inferred from compile success.
- Alpha not considered premium-ready until actual user/device testing, not just screen mockups.

## Immediate next action
Gemini partner: critique this ADR and propose exactly **3 evidence-driven architectural corrections**, plus a small Kotlin domain model schema/renderer interface that can compile without a 3D engine. ChatGPT reviews and resolves conflicts. Architecture first; no unreviewed massive game engine rewrite.
