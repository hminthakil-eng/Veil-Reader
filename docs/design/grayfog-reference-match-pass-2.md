# Grayfog reference match — pass 2

Based on PR #288 at 875c5c6. Reuses the existing original threshold illustration from design/grayfog-reference-fidelity-v1; its provenance is in grayfog-threshold-art-v1.md.

Changes: native home and library headers over bundled offline art; compact parchment resume folio with 96×140dp cover and large-text stacking; smaller recent covers; three muted fallback cover pigments with brass celestial linework; sharper frames; serif section headings; warm light-theme neutrals; flatter dock with selected-tab semantics and two-line labels; scrollable landscape rail; no sliding tab transition.

Library metadata actions, import, filtering, persisted reader preferences, and Reader/Room implementation are unchanged. Existing Castle/Path/quest actions remain available.

Validation: tree-sitter Kotlin syntax and whitespace checks for five changed sources; WebP decoded at 1200×800, 88,982 bytes; source review against declared Compose dependencies. These do not prove compilation or on-device rendering. Android SDK/emulator are unavailable here. No APK or runtime visual parity is claimed.

Build: Codemagic workflow veil-reader-fast-verify, branch design/grayfog-reference-match. It currently runs assembleDebug only; its name does not imply unit/lint execution.

Device gate: home/archive at 320/360/412dp and landscape; font scales 1.0/1.4/2.0; light/dark shell; long titles and Persian text; cover with/without image; empty library; import/search/filter/reset; resume/metadata/favorite; TalkBack selected tabs; navigation inset and bottom reachability. Compare actual screenshots with the supplied reference boards before visual sign-off.
