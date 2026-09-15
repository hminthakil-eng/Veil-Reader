# Veil Reader — Manga Source Architecture v0.1

## Goal
Support manga discovery and reading from connected online catalogues without tying Veil Reader's core, RPG state, library or release viability to fragile or untrusted scraping code.

## Core boundary
The app owns:
- library records and reading history;
- reader UI and manga/webtoon rendering;
- local/offline files and cache;
- downloads and chapter state;
- tracking/progression events;
- RPG/world state;
- backup/restore and privacy.

A provider owns only discovery/retrieval concerns:
- catalogue browsing;
- search/filter;
- title metadata;
- chapter list;
- page/image stream resolution;
- update checks.

## Candidate implementation models
### A. Separate extension APKs
Strong isolation and independent updates, similar to extension ecosystems. Risks: install friction, trust surface, Android package policy and store distribution complications.

### B. Signed provider packages
Veil-managed install/update packages with manifest, permissions, allowed domains and signature verification. Better UX, but requires a carefully designed runtime/sandbox and secure update channel.

### C. Declarative adapters
Providers expressed as restricted schemas/rules rather than arbitrary executable code. Smaller attack surface and easier review, but may not handle complex sites or anti-bot flows.

Research must recommend one primary model and may retain another as an advanced/manual mode.

## Provider contract
A future `ContentProvider` abstraction should expose capabilities such as:
- `search(query, filters)`
- `browse(section, paging)`
- `getTitle(id)`
- `getChapters(titleId)`
- `getPages(chapterId)`
- `checkUpdates(ids)`
- `health()`

Provider-specific objects are normalized into Veil domain models before entering library/RPG logic.

## Security requirements
- explicit provider trust state;
- signed/versioned packages when executable code is involved;
- declared network domains and capabilities;
- no access to user books, notes, credentials or RPG database unless strictly required;
- provider disable/kill-switch without data loss;
- timeout/rate-limit/circuit-breaker behavior;
- visible source health and last-success state;
- user-controlled install/update/removal;
- no silent code execution from arbitrary repositories.

## Distribution and content policy
The official app should remain useful with local EPUB/PDF/CBZ and permitted/connected sources even when no third-party provider is installed. Do not make unlicensed or legally disputed catalogues a hard dependency of the official distribution. Third-party adapters must remain separable from the app core.

## Reader requirements
Manga mode needs RTL/LTR single-page and double-page behavior, crop/fit controls, preloading, chapter navigation and robust cache recovery. Webtoon mode needs continuous vertical scroll, long-chapter memory discipline, image retry and durable position restoration.

## Research gate
Before Development builds online-source integration, M-011 must return:
- selected provider model;
- threat model;
- Play/store distribution analysis;
- update/version strategy;
- provider removal behavior;
- failure/offline behavior;
- licensing/content-source boundaries;
- implementation acceptance criteria.

Local CBZ/image manga and webtoon rendering can proceed independently and should not wait for this decision.