# Reader fixtures

`veil-smoke.epub` is the existing navigation smoke fixture.

`veil-typesetting-ltr.epub` and `veil-typesetting-rtl.epub` contain original Veil
QA text and a generated grayscale test pattern, with no network resources or
scripts. Rebuild byte-identically with `python3 tools/build_typography_fixtures.py`.
Both passed official W3C EPUBCheck5.4.0, EPUB3.4 validation rules, with zero errors
or warnings on2026-10-08. Structural validation is not rendered-output acceptance.

| Fixture | Corpus |
|---|---|
| LTR | English prose, long paragraph, poetry, dialogue, headings, nested formatting, combining accents, ligatures, emoji/symbols, Japanese/Chinese/Korean, image, wide table, semantic footnote and MathML |
| RTL | Persian, Arabic, Persian with isolated English, Persian/Latin numerals, semantic footnote, wide table; publication progression RTL |

Check each on phone portrait/landscape, split screen, tablet/foldable simulation,
normal/maximum Veil text and Android accessibility-large text, Clean/Paper/Night.
Use one/two/auto columns and publisher styling. Confirm complete text, connected
Arabic/Persian glyphs, correct bidi/numerals, no clipped controls, preserved verse
breaks, image fit/zoom/dismiss, table access, contextual footnote return and stable
position after reflow/rotation. MathML is an acceptance probe where supported,
not a guarantee that every navigator supports it.

For RTL, Next/Previous, tap zones, Paper/Slide, TOC/search/bookmark return, selection
handles and spoken mixed-language output require actual rendered checks. Record
screenshots, device/Android/window/font/theme/transition and exact source revision.
No golden or physical acceptance is claimed for these new fixtures yet.
