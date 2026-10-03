# Veil Reader — Alpha × Arena Single-Branch Full-App Reconstruction Contract

Status: **ACTIVE / CANONICAL**
Effective: 2026-10-03

Convergence authority: [2026-10-03 handoff](../convergence/2026-10-03/README.md).
The original Arena head is preserved as the convergence first parent; this does not authorize main promotion.

## 1. One active development line

There is exactly one active reconstruction branch:

`integration/arena-reader-canonical-convergence-v1`

All future product, UI/UX, accessibility, localization, reliability-safe refinement, world systems, gamification presentation, settings, reader polish, tests and source-level verification land on this line.

Historical branches such as `alpha/w27-*`, `alpha/w28-*`, `alpha/w29-*`, `alpha/w30-*`, `alpha/w31-*`, `alpha/w32-*`, `alpha/w33-*`, `alpha/w34-*`, `alpha/w35-*`, `alpha/w36-*`, older `design/*`, `feature/*`, and prior Arena rebuild branches are **reference-only**. They may be inspected for reusable work but must not become parallel product lines.

Do not delete historical branches as part of this contract. They are frozen unless an explicit recovery/forensics need appears.

## 2. Alpha × Arena operating model

**Alpha** owns execution:
- inspect current source before changing it;
- preserve working reading/data engines;
- rebuild weak presentation and incomplete feature surfaces;
- finish unfinished interactions;
- remove duplicate/obsolete paths;
- add source regression tests with each semantic change;
- keep the branch continuously recoverable.

**Arena** owns adversarial refinement:
- compare competing interaction/presentation strategies;
- challenge generic or cheap-looking solutions;
- force edge-case review;
- reject duplicated architecture;
- score against the same product North Star;
- feed one selected result back into the canonical line.

Arena is a quality process, not a second implementation branch.

## 3. Full-app reconstruction scope

Nothing user-facing is exempt from quality convergence.

### Threshold
Rebuild Home/entry into a coherent liminal threshold:
- Continue Reading as the strongest action;
- recent/library retrieval without dashboard clutter;
- narrative atmosphere without blocking reading;
- long-title, empty-library, missing-cover, RTL and large-text states.

### Grayfog Archive / Library
Bring every library mode to the same archival system:
- Gallery;
- Shelves;
- Index/search;
- collections;
- series;
- authors;
- filters/sort;
- import/add-book entry points;
- empty/error/loading states;
- large libraries and missing metadata.

### Artifact Chamber / Book Detail
Treat each book as an artifact, not a product card:
- standing cover and restrained book-specific aura;
- Current Journey;
- Preserved Memory;
- Archive History;
- collections/series identity;
- missing metadata;
- long RTL title/author;
- completed/reread/unopened states;
- calm destructive actions.

### Sanctuary / Reader
Reader remains the quietest realm:
- publication first;
- center-tap chrome;
- Notebook / Bookmark / Appearance / PDF View one gesture away;
- Paged / Scroll independent from Paper / Slide / None;
- paper-curl quality and slide quality independently refined;
- previous location;
- selection/highlight/note flow;
- PDF zoom ownership;
- fixed-layout behavior;
- reduced motion;
- TalkBack;
- landscape/tablet/foldable;
- RTL publication behavior.

Do not replace Readium/PDF architecture for visual cleanliness.

### Appearance
Quick and Advanced must be genuinely different:
- Quick: theme, text size, Reading mode, Page turn, brightness;
- Advanced: font family/weight, line height, margins, alignment, columns, publisher styles, paper patina, dark-image handling and supported script-sensitive controls;
- live preview where safe;
- capabilities must reflect publication/runtime support truthfully.

### Hidden Archive / Notebook
Build one memory language across:
- Notes;
- Highlights/passages;
- Bookmarks;
- Echoes;
- Time Capsules;
- in-book search;
- table of contents;
- return-to-passage;
- edit/delete flows;
- note-only and legacy records;
- real empty/search/error states.

Reader-side Notebook and full Hidden Archive must feel related, not like separate products.

### Settings
Settings is calm progressive disclosure, never a control dump:
- Appearance;
- Reading defaults;
- sensory/haptics/audio;
- backup/restore/export;
- privacy;
- reset;
- accessibility;
- version/engine facts;
- all destructive actions explicit and reversible where possible.

### Castle / Path / Ritual / Sanctum
Rebuild world systems to the same visual/product standard while preserving optionality:
- Castle;
- Path;
- ranks;
- progression;
- Observatory;
- Treasury;
- Ritual;
- Sanctum;
- relics/secrets/endgame;
- milestones and historical progression.

World systems may read durable reading history; they must never gate access to books or core reading.

### Profile / More
Unify:
- identity;
- reading history;
- progress facts;
- world entry points;
- backup/settings access;
- Hidden Archive access;
- privacy/local-first messaging;
- no generic account-dashboard feel.

### Manga / Comics
Keep secondary to text/PDF Reader quality, but when active:
- separate format-aware presentation;
- no live-source expansion without explicit authorization;
- preserve offline/local ownership;
- do not let manga architecture fork core library/navigation unnecessarily.

### Error / Empty / Loading / Recovery
Every state is part of the design system:
- import failures;
- open failures;
- missing files;
- renderer unavailable;
- no search results;
- empty library/archive;
- missing metadata;
- backup/restore errors;
- persistence recovery;
- interrupted navigation.

No raw technical errors in user-facing UI.

## 4. Quality requirements applied everywhere

Every surface must pass:
- English + Persian;
- LTR + RTL;
- long title/author;
- missing metadata;
- 200% text;
- TalkBack semantics;
- 48dp minimum actionable targets;
- reduced motion;
- phone portrait;
- phone landscape;
- tablet/foldable;
- offline behavior;
- process recreation where applicable;
- state truthfulness;
- no fake progress, fake intelligence or fabricated history.

## 5. Design DNA

The whole app shares one world:
- deep blue-black / ink shell;
- brass used as registration/meaning, not decoration spam;
- parchment reserved for reading/material moments;
- oxblood used sparingly for danger/ritual emphasis;
- editorial serif where narrative hierarchy benefits;
- restrained occult/astronomical/archival motifs;
- industrial precision;
- quiet geometry;
- no glassmorphism;
- no nested-card soup;
- no universal rounded-card UI;
- no gradient/shadow soup;
- no mystical renaming of basic controls.

Density gradient:
`Reader < Threshold < Book Detail < Archive < Castle/Ritual/Sanctum`.

## 6. Architecture rules

- one Reader architecture;
- one visual token system;
- one canonical branch;
- one active backlog;
- no duplicate Settings implementation;
- no duplicate Archive/Notebook data model;
- no duplicate page-turn ownership;
- no parallel persistence path for the same fact;
- no UI-only fake state;
- no destructive branch cleanup without explicit authorization.

Reuse first. Replace only when the current implementation cannot meet the product contract.

## 7. Reconstruction order on the same branch

The wave names are milestones only, never branches:

1. correctness/compile gate
2. Threshold / Archive / Artifact Chamber / Sanctuary convergence
3. Reading mode + Page turn separation
4. Hidden Archive / Notebook
5. Settings progressive disclosure
6. Profile / More
7. Castle / Path / Observatory
8. Ritual / Sanctum / Treasury
9. global localization / RTL / accessibility
10. motion / haptics / sensory harmonization
11. empty/error/loading/recovery states
12. adaptive layout / tablet / foldable / landscape
13. performance/profile/package verification
14. release-quality visual/device proof

## 8. Definition of done

A feature is not done because it exists.

It is done only when:
- behavior is complete;
- state is truthful;
- presentation matches Veil DNA;
- localization and RTL are correct;
- large text and accessibility remain usable;
- edge states are designed;
- regression coverage protects semantic behavior;
- it does not duplicate an existing system;
- it is verified at the appropriate gate.

## 9. Final rule

Veil Reader is reconstructed as one coherent product.

No screen is allowed to remain visibly legacy merely because its feature already works.
No feature is allowed to become prettier by weakening reliability.
No world feature is allowed to interfere with reading.
No new branch is created for ordinary reconstruction work.

**Alpha executes. Arena challenges. One canonical line ships.**
