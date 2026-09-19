# Third-Party Notices

## Page Curl

Veil Reader's paper-turn geometry and curl drawing implementation includes
adapted portions of **oleksandrbalan/pagecurl**:

- Source: https://github.com/oleksandrbalan/pagecurl
- Upstream revision inspected: `a390c64`
- License: Apache License 2.0
- Veil modifications: Readium-driven gestures, reader snapshot overlay,
  RTL/LTR mirroring, theme-aware paper backing, edge highlights, lifecycle
  cleanup, and Veil-specific navigation/acceptance behavior.

The upstream Apache License 2.0 text is preserved under
`third_party/licenses/pagecurl-APACHE-2.0.txt`.

## Android Page Curl GPU Mesh

Veil Reader's optional GPU curl backend includes adapted portions of
**Harri Smatt's android-pagecurl**:

- Source: https://github.com/harism/android-pagecurl
- License: Apache License 2.0
- Veil modifications: Readium-driven external gestures, caller-owned bitmap
  lifecycle, reusable NPOT texture uploads, render-on-demand, adaptive fallback,
  zero-allocation drag hot path, and Veil-specific support/lifecycle handling.

The Apache License 2.0 text is preserved under
`third_party/licenses/harism-pagecurl-APACHE-2.0.txt`.


## Harism Android Page Curl

Veil Reader's optional GPU paper-turn path includes a modernized adaptation of
**harism/android-pagecurl** by Harri SmÃ¥tt:

- Source: https://github.com/harism/android-pagecurl
- Upstream revision inspected: `7a2c8f1`
- License: Apache License 2.0
- Veil modifications: render-on-demand transparent overlay, Readium-driven
  external gestures, direct NPOT texture uploads, caller-owned reusable page
  snapshots, linear texture filtering, adaptive GPU fallback, modern lifecycle
  handling, and Veil-specific page-turn completion behavior.

The upstream Apache License 2.0 and NOTICE are preserved under
`third_party/licenses/harism-pagecurl-APACHE-2.0.txt` and
`third_party/notices/harism-pagecurl-NOTICE.txt`.
