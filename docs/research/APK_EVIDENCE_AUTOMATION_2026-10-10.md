# Veil Reader — APK evidence automation (2026-10-10)

Status: **R&D tooling; NOT a Paper Curl fix and NOT release approval.**

## Scope

This PR adds a bounded, deterministic Python 3.12 reference inventory. It reads ZIP
central-directory metadata and streams SHA-256. It **never installs, runs,
extracts, dynamically imports, decompiles or redistributes** reference APK
code, resources, fonts or artworks. No competitor APK is committed.

The tool accompanies, but does not replace, the detailed clean-room capability
matrix in PR #441. Real code-level changes and exact-source Android device
acceptance remain in PR #442 and its separate verification branches.

## Actual uploaded inputs rechecked

| Artifact | SHA-256 | DEX archives | Fonts (file paths) | Evidence |
|---|---|---:|---:|---|
| Moon+ Reader Pro 10.7 | d676b16ff18c2496ab35d1306fd195e7fba491bc8260cfd368ab973c7bc09d08 | 5 | 9 | 22 hyphenation-path entries; four ABI directories |
| Kindle 8.157.0.100 | d93730e9b1decd76d809452aaa7b50eb80fbd4ec94ab383f33990d05dd0f14bb | 8 | 63 | One arm64 ABI directory |
| Veil Reader QA debug 7a6bea9 | 3125605831945f4d76bb49bd4a6e63e719256f10827c9addaa361908bb2bb120 | 22 | 11 | Readium assets are present |

These fingerprints establish file identity and structural clues ONLY.
They neither verify genuine vendor distribution/signatures nor prove any feature
works. The Moon+ hash matches the sample previously inspected by PR #442,
so repeated analysis of the same bytes is unnecessary.

## Invocation

Run tests from the **repository root**:

    PYTHONPATH=tools python -m unittest discover -s tools/tests -p test_reader_reference_inventory.py -v

Analyze local APKs, without copying them into git:

    python tools/reader_reference_inventory.py --format markdown "/path/to/moon.apk" "/path/to/kindle.apk" "/path/to/veil.apk"
    python tools/reader_reference_inventory.py --format json "/path/to/moon.apk" "/path/to/kindle.apk"

The GitHub workflow runs the five synthetic archive tests; it does **not**
download or execute untrusted APKs. This keeps a portable regression guard
for repeatable vendor/reference evidence.

## Actual implementation path — work reuse

1. P0: Finish exact-head SVG/image-cover, animated GPU presentation,
   gesture ownership, release/cancel, and one-turn-per-gesture gate on real devices.
2. W1: Characterize existing Veil TTS rather than rewrite it.
3. W2: Upgrade the existing selection LOOKUP path to an explicit provider
   boundary and audited offline/online fallback; preserve highlight/note
   transaction semantics.
4. W3: Test language-specific hyphenation and live typography changes at
   200% text scale and in RTL.
5. W4+: Add catalogs, sync and additional formats only after reusable
   source/library licensing and end-to-end UX gates.

Legally reusable upstream code examples for investigation:
- readium/kotlin-toolkit — BSD-3-Clause; already provides Veil's reader foundation.
- harism/android-pagecurl — Apache-2.0; optional geometry/mesh reference,
  not proof it meets current Android GPU and Readium integration requirements.

**License rule:** MIT/BSD/Apache components can be integrated within their
actual license terms and attribution requirements. Proprietary vendor APK code,
private protocols, brand assets and proprietary fonts are not a copy source.
Extract behavioral requirements and independently implement in Veil modules.

No app behavior, feature gate, signing identity or release artifact changes in
this PR. Keep draft until review; the local five-test run passed, while
hosted workflow and Android runtime checks are separate statuses.
