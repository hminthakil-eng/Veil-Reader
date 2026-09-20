# Suwayomi Gateway Provenance

Date: 2026-09-20
Status: controlled optional gateway; no bundled Suwayomi runtime

## Purpose

Veil connects to a user-controlled Suwayomi server over its documented GraphQL/HTTP
surface. Veil does not embed Suwayomi's extension runtime, source implementation,
server database, or server process.

## Canonical upstreams

### Suwayomi Server

Repository:
- `Suwayomi/Suwayomi-Server`

Audited branch:
- `master`

Audited commit:
- `d37230ee22909414e5b4f9967eea31606bd890a4`

License:
- MPL-2.0

Reviewed files:

- `server/src/main/kotlin/suwayomi/tachidesk/graphql/GraphQL.kt`
  - blob: `bfafc960822545847efc62030dd5f03a76011f15`
- `server/src/main/kotlin/suwayomi/tachidesk/graphql/mutations/SourceMutation.kt`
  - blob: `c5428391406596d209c4905271265eba35b4fa6e`
- `server/src/main/kotlin/suwayomi/tachidesk/graphql/mutations/MangaMutation.kt`
  - blob: `ff7647ec4ef6b8bad219d52f74200fa8f8acdcad`
- `server/src/main/kotlin/suwayomi/tachidesk/graphql/mutations/ChapterMutation.kt`
  - blob: `78e16f244250e012de4e127c39a08238fd49f5ef`
- `server/src/main/kotlin/suwayomi/tachidesk/graphql/types/SourceType.kt`
  - blob: `5bf813dc431666945a7f55b783de8de55568374b`
- `server/src/main/kotlin/suwayomi/tachidesk/graphql/types/MangaType.kt`
  - blob: `07fd7ddd7dc7005a3ce5efcd2995f4f8b7ec32e6`
- `server/src/main/kotlin/suwayomi/tachidesk/graphql/types/ChapterType.kt`
  - blob: `7020db46767736dcfc98d2ad68ec5311308ace6b`

Protocol facts used by Veil:
- GraphQL HTTP endpoint is exposed as `/api/graphql` in the normal server layout.
- browse/search uses `fetchSourceManga`
- details + chapters use `fetchMangaAndChapters`
- page proxy paths are returned by `fetchChapterPages`
- returned page URLs are server-local `/api/v1/manga/.../page/...` paths.

### Suwayomi WebUI

Repository:
- `Suwayomi/Suwayomi-WebUI`

Audited branch:
- `master`

Audited commit:
- `8a72046d32ad2439ebb680a2730eb1dd05958642`

License:
- MPL-2.0

Reviewed files:

- `src/lib/requests/client/GraphQLClient.ts`
  - blob: `ba16cabd707a0a9c9043d75050dd9068dabccd3a`
- `src/lib/graphql/source/SourceMutation.ts`
  - blob: `5d6a46f4c8f9d2eea0abe12912238dfb760f0735`
- `src/lib/graphql/manga/MangaMutation.ts`
  - blob: `96bc1482449618da74a0256a724802f942f49541`
- `src/lib/graphql/chapter/ChapterMutation.ts`
  - blob: `137e733655fd911d26e024533b8c2791afcc9219`
- `src/lib/graphql/manga/MangaFragments.ts`
  - blob: `583a45c8b4d37d5d5332431fd111187d16caccb0`
- `src/lib/graphql/chapter/ChapterFragments.ts`
  - blob: `5fabac464da2a3316d0cdb42b8b7d1d9c495bcdd`

The WebUI was used to confirm current client-visible GraphQL field selections and
Bearer authorization behavior.

## Veil boundary

Veil implements its own Kotlin protocol client and maps the remote API into
`MangaSourceProvider`.

Not bundled:
- Suwayomi Server code
- Suwayomi WebUI code
- Tachiyomi/Mihon extension runtime
- remote extension APK/JAR files
- user credentials

Secrets:
- authentication is supplied through `SuwayomiAuthProvider`
- credentials are not embedded in `MangaSourceDescriptor`
- credentials are not encoded into source IDs
- credentials in URL user-info are rejected

Network constraints:
- HTTPS is required by default
- cleartext HTTP requires explicit opt-in
- GraphQL responses and page resources remain bound to the configured server origin
- returned page resources may not silently switch scheme, host, or effective port.

## Eyad Studio provenance gate result

- provenance_status: VERIFIED
- license_status: COMPATIBLE_FOR_PROTOCOL_INTEGRATION
- upstream license: MPL-2.0
- integration type: remote protocol consumer
- copied upstream source files: none
- recommended action: PROCEED_TO_TESTING
- unresolved gate: verify current GraphQL compatibility against a real user-controlled server
  before enabling this gateway by default.

## Verification still required

When an authorized PC/device is available:

1. JVM fixture tests
2. debug compile
3. lint
4. release/R8
5. real Suwayomi server with no auth
6. real Suwayomi server with Bearer auth
7. HTTPS server
8. optional LAN cleartext server only if Android network-security policy is explicitly enabled
9. browse -> details -> chapters -> pages
10. page proxy authentication
11. source disabled/uninstalled behavior
12. server upgrade compatibility smoke
