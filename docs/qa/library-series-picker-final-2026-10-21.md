# Series picker final verification — 2026-10-21

Product: draft PR #558, head f43fa8cf038ca2ed3187078da057e70ef85793e3, tree 410fb2af904db5974edc0df7abb808725a3a11cf. This diagnostic branch is an evidence handoff, not a product merge candidate.

## Result

Complete canonical series retrieval, searchable in English/Persian, with counts, selected state, All series, clear and cancel. The caller's full Density is retained across the separate Dialog owner. Dialog-local focus and keyboard services remain intact. Reader/Paper engine, schema, dependencies, signing and application identity were not changed by this feature.

Observed RED before correction: actual series-label TextLayoutResult fontScale was 1.0 despite caller 2.0. Scoped GREEN after correction: 13 production interaction cases and five pure choice cases; Android test compilation succeeded. Independent source review found no Important issues. Full sandbox test/lint/debug build completed successfully in 10m46s: 1071 tests, no failures/errors/skips; zero lint errors and 174 existing warnings. Policy, canonical Paper and performance/release parser checks also passed.

All six configured workflows succeeded on this exact head:

| Workflow | Run |
| --- | --- |
| Android CI | 39541819442 |
| Storage Instrumentation | 39541819464 |
| Performance Benchmarks | 39541819557 |
| Dependency Baseline | 39541819444 |
| Dependency Review | 39541819456 |
| Quality Dashboard | 39541819467 |

## Exact artifact inspection

- Source reports 12320582154: ZIP SHA256 c036e7dd8d25133077e07ef71d1e2c5977637e8bcd46e344372534401a8e08b5. Parsed 179 JUnit XML files: 1071 tests, zero failures/errors/skips. Lint zero Fatal/Error, 174 warnings.
- Device reports/captures 12319819181: ZIP SHA256 7a5b33861913b3dcc26374c84fee29199566444aba4dba53ec707eaaad921f29. Parsed 193 tests, zero failures/errors/skips. All 16 named series-picker cases executed (English/Persian x 1/1.3/1.5/2 font scale x standard/high contrast). Each now asserts actual label layout density before capturing.
- Debug APK artifact 12320372981: ZIP SHA256 60e2fe82fb12c2c01ba0e23f98fce661b1d866439007eb01fe366f4d07b8a668. Extracted APK 50,835,376 bytes, SHA256 da7c9061d4b3cd5b1cdee78ee73b7141b22edf4fa39992be4972442fa66ecf32. Debug/test build, not a production release.

All ZIP identities and SHA256 values were checked before inspection. All sixteen PNGs were reviewed as a contact sheet, with full-size Persian 200% standard and English 200% high-contrast inspection. Long labels wrap; selected/count/search/cancel controls remain visible in these fixtures. Every 100%-versus-200% same-language/contrast pair now differs: RGB difference bounds en=(162,410,918,1965), fa=(162,428,918,1969). The earlier e182 captures were identical and remain invalid enlarged-font evidence.

## Limits and learning

The fixtures use synthetic linear Density. Real Android nonlinear system font scaling, physical-device TalkBack, the actual compact window with the keyboard open, explicit nondefault collection/sort interaction coverage and series-picker latency remain unverified. The 10,000-choice test is functional completeness evidence only. Performance workflow success is emulator smoke evidence, not phone performance acceptance.

Learning: screenshot filenames and test parameters do not prove effective accessibility configuration. Assert the actual leaf text layout across separate window/Compose-owner boundaries, then inspect rendered captures. Preserve complete density/font-conversion objects rather than rebuilding them from scalar values. Keep window-specific focus services local to their owner.

Temporary diagnostic workflows/init scripts remain retired. Workspace maintenance removed the earlier sandbox after publication; exact product source and checksummed CI artifacts were recovered for this final read-only verification. No product source edits were needed after f43fa8c.
