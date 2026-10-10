# Clean-room competitor feature map — Moon+ Reader Pro 10.7 / Kindle 8.157

Date: 2026-10-09
Branch baseline: `f33b6a65d451f9417a9eea9f20c488ed5d16018c`

## Legal / engineering boundary

This document records only behavior, feature surface, package/class/resource names and architectural patterns observable from the supplied APKs and public/open-source references. It is **not** a source-code extraction plan. Veil Reader implementations must be original, Veil-owned, testable clean-room code. Proprietary implementation bodies, assets, secrets and service protocols are out of scope.

## Static evidence from supplied APKs

### Moon+ Reader Pro
Observed fingerprints include:
- `NewCurl3D`, `GLLayoutCurl`, `startCurl`, `setCurlPos`, `updateBitmaps`, `createMesh3Ds`, `simulateOnTouch`, `turnPageGoogleStyle`.
- Explicit page/curl bitmap/cache signals including page shots and dual-page cache state.
- Auto-scroll pipeline and page-turn integration.
- Android TTS integration with pause/prior/speed/pitch controls and TTS panel.
- OPDS/catalog, Dropbox/WebDAV, translation/dictionary hooks.
- PDF/DjVu engines and broad format support.

### Kindle
Observed fingerprints include:
- Explicit `PAGE_CURL` / page-curl setting surfaces.
- `PageNavigatorFactory`, `SelectionGestureHandler.PageTurnBox`, page-transition events and KRF native reader/rendering stack.
- Whispersync/read/listening progress, bookmarks/highlights/notes/notebooks.
- Dictionary, translation, Word Wise, X-Ray, Popular Highlights.
- Audible/listening integration, narration speed, sleep timer and reading/listening position separation.
- Collections/library/download/offline surfaces and accessibility instrumentation.

## Clean-room conclusions

### 1. Paper engine / page turn — P0
Competitor pattern:
1. page navigation mode selected explicitly;
2. renderer mounted independently from publication truth;
3. source page captured/cached;
4. curl begins only after drawable source is ready;
5. input ownership is reserved for the selected mode;
6. gesture updates physical geometry continuously;
7. navigation commit is explicit and synchronized with visual reveal;
8. renderer failure never silently degrades into a different animation.

Veil target:
- Keep Readium authoritative for locator/content.
- Keep `PAGED`, `PAPER_CURL`, `SLIDE`, `SCROLL` distinct.
- GPU renderer preferred, software mesh fallback allowed.
- Snapshot preparation + presentation acknowledgement required.
- No hidden fallback to native slide/static behavior.
- Physical-device acceptance required.

### 2. TTS / Listening — P0
Clean-room parity targets:
- dedicated listening position separate from reading position;
- previous/play-pause/next/speed mini-player;
- installed voice discovery + explicit voice selection;
- language-aware automatic voice ranking;
- sleep timer;
- background playback/checkpointing;
- clear failure states and retry;
- compact controls that remain usable at 200% text.

### 3. Annotation / notebook — P1
Targets:
- highlights, notes, bookmarks, color channels;
- full notebook by book and globally;
- jump to passage with Previous Location return;
- export/share in open formats;
- durable process-death-safe writes;
- search/filter/sort.

### 4. Dictionary / translation / context intelligence — P1
Targets:
- offline dictionary contract;
- selected-text translation;
- language-aware lookup;
- optional entity/context cards inspired by X-Ray behavior, using Veil-owned metadata pipelines;
- optional vocabulary assistance / Word-Wise-like explanations generated from lawful data sources.

### 5. Library / collections / discovery — P1
Targets:
- collections/shelves/series/status/favorites;
- metadata repair;
- gallery/shelf/index views;
- full-text + metadata search;
- OPDS/import sources where lawful;
- offline-first storage;
- reliable large-library performance.

### 6. Sync / continuity — P1
Targets:
- local-first reading position;
- explicit sync state and conflict handling;
- reading/listening position separation;
- highlights/notes/bookmarks sync;
- backup/restore;
- provider adapters rather than hard-coded proprietary protocols.

### 7. Advanced reading controls — P1/P2
Targets:
- typography, margins, line-height, alignment, columns, hyphenation/ligatures;
- brightness/theme/OLED;
- tap zones/hardware keys;
- orientation/rotation policy;
- auto-scroll;
- accessibility alternatives for gestures;
- reduced-motion behavior.

### 8. Reading intelligence / engagement — P2
Targets:
- reading stats and milestones;
- popular/public annotations only from opt-in lawful sources;
- contextual entities/people/places;
- gamification remains Veil-native and separate from Reader chrome.

## Acceptance rule

No feature becomes GREEN because a control exists. GREEN requires:
- implementation;
- unit/instrumentation coverage;
- accessibility coverage;
- process-death/durability where relevant;
- benchmark budget where relevant;
- physical-device evidence for gesture/rendering/audio features.

## Current Paper Engine status

The branch now contains a later Paper wave than the APK previously tested on-device:
- GPU material curl;
- software material curl fallback;
- stronger snapshot ownership/presentation acknowledgement;
- reserved Paper drag ownership;
- no silent fallback to slide/static;
- additional rollout/contract tests.

The prior physical test used head `7a6bea9...`; current branch head is `f33b6a65d451f9417a9eea9f20c488ed5d16018c`. A new exact-head device test is therefore mandatory before judging the current engine.
