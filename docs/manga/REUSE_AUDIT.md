# Veil Manga Reuse Audit

Status: adopted for Manga Hub / Manga Reader development.
Date: 2026-09-20

## Operating rule

Before implementing a manga-reader or manga-source feature from scratch:

1. Check the approved reuse sources below.
2. Verify repository and file license.
3. Verify maintenance/activity and security/network behavior.
4. Prefer adaptation behind Veil-owned interfaces.
5. Preserve required attribution and license notices.
6. Add Veil regression tests before enabling the reused behavior.
7. If license or coupling is unsuitable, use clean-room behavioral reference only.

## Decision matrix

### Mihon — selective direct reuse allowed

Repository: `mihonapp/mihon`
License verified: Apache-2.0.
Current status: actively maintained.

Good reuse candidates:
- reading-mode model: LTR / RTL / vertical / webtoon / continuous vertical
- paged-reader navigation behaviors
- webtoon preload heuristics
- chapter-transition behavior
- reader state/process-restoration ideas
- download/cache invalidation strategies
- tap-zone and navigation-mode concepts
- image-reader performance patterns

Do not copy wholesale:
- `ReaderViewModel`
- database/domain graph
- tracker integration
- download manager
- source manager
- dependency-injection graph
- UI/activity shell

Reason: these parts are strongly coupled to Mihon's internal persistence, trackers,
source ecosystem, and DI. Reuse would increase Veil coupling and migration cost.

Preferred Veil strategy:
- keep Veil's `MangaSourceProvider`, `MangaOfflineStore`, `MangaProgressStore`
  and Compose reader shell as the stable boundaries;
- adapt isolated algorithms or small components only;
- port behavior with Veil-native state and storage.

### Yūzōnō tachiyomi-extensions — selective source-pattern reuse allowed

Repository: `yuzono/tachiyomi-extensions`
License verified: Apache-2.0.
Current status: actively maintained.

Good reuse candidates:
- source request/response patterns
- pagination strategies
- JSON/HTML extraction patterns
- common multisource abstractions
- GraphQL request patterns
- Next.js extraction ideas
- source preference patterns
- URL normalization
- source test fixtures

Important current upstream guidance:
- new sources use `KeiSource` / suspend-based APIs;
- `HttpSource` is legacy;
- `ParsedHttpSource` is deprecated.

Veil rule:
Do not import the Mihon/Keiyoushi extension runtime as a hard dependency.
Translate useful source logic into Veil-owned `MangaSourceProvider` adapters.
This keeps source modules replaceable and prevents Veil UI/core from depending
on a third-party extension ABI.

### Kotatsu / Kotatsu-Redo — clean-room reference only

Default decision: no direct GPL implementation reuse in Veil core.

Allowed:
- study UX
- study source replacement/migration behavior
- study offline/update-feed concepts
- reproduce behavior independently

Not allowed by default:
- copying implementation into Veil-owned non-GPL modules
- direct fork/dependency

### Miyomi — clean-room reference only

Default decision: no AGPL implementation reuse in Veil core.

Allowed:
- architecture/discovery/indexing ideas
- independent reimplementation

## Immediate reader decisions from Mihon audit

Veil Slice 3 should retain its lightweight Compose architecture but adopt these
validated behaviors instead of expanding blindly:

1. Keep explicit reader modes rather than one overloaded viewer.
2. Add chapter-transition objects instead of special-casing first/last pages.
3. Preload the next chapter when the reader enters the last few pages.
4. Restore current chapter + page independently from UI composition state.
5. Keep prefetch bounded; do not retain an entire long webtoon in memory.
6. Separate durable downloads from evictable image cache.
7. Treat downloaded-chapter index/cache invalidation as an explicit operation.
8. Keep RTL/LTR direction separate from viewer type.

## Immediate source decisions from Yūzōnō audit

1. Veil source APIs remain suspend-based.
2. Build reusable Veil-owned source helpers before duplicating logic across adapters.
3. Add helpers only when at least two adapters need them.
4. Avoid legacy `HttpSource` / `ParsedHttpSource` designs.
5. Every source remains behind a feature flag until fixture tests and a live read-only probe pass.
6. Prefer official/public APIs over HTML scraping whenever available.

## License hygiene for direct Apache-2.0 reuse

For any copied or substantially adapted Apache-2.0 file:
- record original repository, path, and commit SHA;
- retain applicable copyright/attribution notices;
- mark materially modified files;
- keep the Apache-2.0 license available in third-party notices;
- include upstream NOTICE content if the reused work has applicable NOTICE entries.

No code is considered approved for direct reuse solely because the repository
root is Apache-2.0; file-level provenance still must be checked.

## Quality gate

Every reuse item must pass:
License -> Provenance -> Security -> Maintenance -> Architecture fit ->
Unit/fixture tests -> Release/R8 -> Device smoke test.

If any gate fails, fall back to clean-room reimplementation.
