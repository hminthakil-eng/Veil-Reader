# Bundled typography provenance

Veil Reader bundles its app-shell fonts so OEM/system font overrides cannot change the UI
metrics, glyph shapes, truncation, or layout.

## Noto Sans Arabic

- Use: app chrome, body, labels, controls
- Bundled as: `app/src/main/res/font/veil_ui_sans.ttf`
- Upstream: `google/fonts/ofl/notosansarabic/NotoSansArabic[wdth,wght].ttf`
- Google Fonts blob SHA: `f1d01edce4ebaedcbe9a06fc75fec07b304ec3df`
- Upstream Noto release: NotoSansArabic v2.012
- Coverage used by Veil Reader: Latin, Latin Extended, Arabic/Persian
- License: SIL Open Font License 1.1
- Modification: none; filename only changed for Android resources

## Noto Naskh Arabic

- Use: literary/display headings
- Bundled as: `app/src/main/res/font/veil_display_serif.ttf`
- Upstream: `google/fonts/ofl/notonaskharabic/NotoNaskhArabic[wght].ttf`
- Google Fonts blob SHA: `a8d2867262dc7bb28492ad746de5d56235735c8b`
- Upstream Noto release: NotoNaskhArabic v2.021
- Coverage used by Veil Reader: Latin, Latin Extended, Arabic/Persian
- License: SIL Open Font License 1.1
- Modification: none; filename only changed for Android resources

The original OFL text for each bundled font is stored beside this file.
Publication/reader typography is intentionally unaffected; these fonts apply to Veil Reader's
own Material UI only.
