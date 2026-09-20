# Manga Source Recipe Provenance

Date: 2026-09-20

## Purpose

Veil's Source Recipe Layer is a clean Veil-owned implementation informed by recurring
behaviors observed in open-source manga-source ecosystems. The goal is to reduce duplicated
adapter work without embedding or depending on a third-party extension runtime.

No upstream source file is copied wholesale by this slice.

## Keiyoushi extensions-source audit

Repository:
- `keiyoushi/extensions-source`

Audited main commit:
- `9137b65daada0b328a05e7c3ce8c4490bbd90dac`

Repository license:
- Apache License 2.0

Files reviewed:

- `lib-multisrc/madara/src/eu/kanade/tachiyomi/multisrc/madara/MadaraBase.kt`
  - blob: `c41f296cff0c29cc5bd8327996ea45a9299437d5`
- `lib-multisrc/madara/src/eu/kanade/tachiyomi/multisrc/madara/Madara.kt`
  - blob: `c386297f0e800e61d3ae932d2ea84151b6fd0143`
- `lib-multisrc/mangathemesia/src/eu/kanade/tachiyomi/multisrc/mangathemesia/MangaThemesia.kt`
  - blob: `1b0787dc735663df59ab4510c848bcaa95432433`

Repository search returned the maximum 100 results for both Madara and MangaThemesia
family references, demonstrating high reuse leverage. This is evidence of 100+ matches,
not an exact consumer count.

Behaviors studied:
- family-level selector defaults
- multiple chapter request modes
- AJAX/form chapter loading
- lazy image URL extraction
- combined details/chapter refresh
- dynamic/permanent slug normalization
- shared filters and pagination patterns

Veil implementation rule:
- translate behaviors into Veil-owned request plans, parser rules, and Source API v2;
- do not use Keiyoushi's extension ABI/runtime as a dependency;
- record file provenance before any future substantial direct Apache-2.0 adaptation;
- retain required attribution/notice if code is directly adapted.

## jsoup

Dependency:
- `org.jsoup:jsoup:1.23.2`

License:
- MIT

Role:
- parse source-provided HTML and execute CSS-selector queries only.
- network requests continue through Veil's MangaHttpClient/OkHttp transport.

Veil intentionally does not use `Jsoup.connect()` in source adapters so transport,
rate limiting, headers, typed errors, and security policy remain centralized.
