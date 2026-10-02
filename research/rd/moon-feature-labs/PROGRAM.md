# Eyad Studio R&D — Moon-derived Reader Feature Labs

Date: 2026-10-02
Canonical source: `alpha/w26-arena-canonical-ui-rebuild-v1`
R&D branch: `rd/moon-missing-feature-labs-2026-10-02`

## Rule

Nothing in this program is production code. The app module has no dependency on `:rd-reader-labs`.
Each missing capability is developed and tested independently. Integration is a later decision, not
an automatic consequence of a successful prototype.

## Arena gate

Every lab must be able to enter the repository Arena skill as a standalone task. A lab can move to
`INTEGRATION_CANDIDATE` only when:
1. its behavior is independently specified;
2. the prototype has deterministic tests;
3. accessibility/RTL/offline/privacy constraints are explicit where relevant;
4. its failure modes are documented;
5. it does not require replacing Readium or weakening existing durability;
6. runtime-heavy labs have a measurable performance budget;
7. a canonical integration plan can be expressed as a reversible slice.

## Labs

| Lab | Missing capability | R&D deliverable | Integration trigger |
| --- | --- | --- | --- |
| RDL-01 Appearance Profiles | Named complete themes + per-book override | profile resolution prototype | users need reusable reading setups |
| RDL-02 Input Profiles | advanced tap-zone/key mapping | data-driven mapping + conflict checks | power-user customization requested |
| RDL-03 Focus/Ruler | reading ruler + focus window | normalized ruler geometry | sustained-reading value proven |
| RDL-04 PDF Annotations | PDF highlight/note/ink/etc. | capability/annotation domain model | renderer proves truthful write/read |
| RDL-05 Context Tools | dictionary/translate/search/share extension | provider registry contract | safe provider path selected |
| RDL-06 TTS | spoken reading | chunking/filter/state-machine prototype | accessibility/power-user demand |
| RDL-07 Auto-scroll | pixel/line/page/rolling modes | deterministic scroll engine | performance + stop-safety proven |
| RDL-08 Catalogs | OPDS / Calibre-style catalogs | provider/catalog contract | online library becomes product scope |
| RDL-09 Sync | position/notes/shelf/cloud sync | deterministic conflict resolver | optional account/sync approved |
| RDL-10 Legacy Formats | MOBI/AZW3/FB2/CHM/DJVU/DOCX/ODT/RTF/MHTML/UMD adapters | adapter/capability registry | format demand justifies dependency |
| RDL-11 Library Metadata | ratings/tags/custom cover | normalized metadata model | retrieval research proves value |
| RDL-12 Name Replacement | reversible reader-only name/role transforms | deterministic text rule engine | requested as advanced reader tool |
| RDL-13 E-Ink | e-ink reading profile | display policy prototype | physical e-ink validation exists |
| RDL-14 App Lock | password/biometric privacy gate | lock-state policy contract | local privacy lock requested |
| RDL-15 Widget/Shortcut | home widget/book shortcut | surface contract | Android surface work enters scope |
| RDL-16 Reading Calendar | calendar/statistics presentation | session aggregation model | existing history needs calendar view |
| RDL-17 Page-turn Renderer Lab | lower-latency Paper/Slide rendering | renderer contract + benchmark plan | current P95 performance blocker persists |

## Explicitly not copied

No Moon+ code, assets, branding, encryption/licensing logic, credentials, or DRM/protection behavior.
Moon is used only as behavioral/maturity evidence.

## Promotion states

`DISCOVERY → PROTOTYPE → VALIDATED → ARENA_CANDIDATE → INTEGRATION_CANDIDATE → ADOPT / HOLD / REJECT`

A prototype may remain useful research forever without entering the product.
