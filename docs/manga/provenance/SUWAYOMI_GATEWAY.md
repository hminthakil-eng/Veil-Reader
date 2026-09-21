# Suwayomi Gateway Provenance

Date: 2026-09-20

## Purpose

Veil's Suwayomi Gateway consumes a user-configured Suwayomi server as a remote GraphQL
protocol. It does not embed Suwayomi Server, WebUI, Android extension binaries, or extension
runtime code in the Veil APK.

The gateway maps a deliberately small protocol surface into Veil Source API v2:

- source discovery
- SEARCH / POPULAR / LATEST browse
- combined manga details + chapter refresh
- chapter page retrieval

## Canonical upstream

### Suwayomi Server

Repository:
- `Suwayomi/Suwayomi-Server`

Reviewed branch:
- `master`

Reviewed commit:
- `d37230ee22909414e5b4f9967eea31606bd890a4`

License:
- MPL-2.0
- license blob: `d0a1fa1482eea82e19510e7920cbe3a03e41f691`

Protocol implementation files reviewed:

- `server/src/main/kotlin/suwayomi/tachidesk/graphql/mutations/SourceMutation.kt`
  - blob: `c5428391406596d209c4905271265eba35b4fa6e`
- `server/src/main/kotlin/suwayomi/tachidesk/graphql/mutations/MangaMutation.kt`
  - blob: `ff7647ec4ef6b8bad219d52f74200fa8f8acdcad`
- `server/src/main/kotlin/suwayomi/tachidesk/graphql/mutations/ChapterMutation.kt`
  - blob: `78e16f244250e012de4e127c39a08238fd49f5ef`

Relevant protocol facts verified:
- GraphQL is exposed under `/api/graphql` in the normal server routing.
- `fetchSourceManga` exposes SEARCH / POPULAR / LATEST.
- `fetchMangaAndChapters` refreshes and returns manga + chapter records.
- `fetchChapterPages` refreshes and returns page URLs.
- source browse operations use 1-based integer pages.

### Suwayomi WebUI

Repository:
- `Suwayomi/Suwayomi-WebUI`

Reviewed commit:
- `8a72046d32ad2439ebb680a2730eb1dd05958642`

License:
- MPL-2.0
- license blob: `d0a1fa1482eea82e19510e7920cbe3a03e41f691`

Protocol usage files reviewed:

- `src/lib/graphql/source/SourceMutation.ts`
  - blob: `5d6a46f4c8f9d2eea0abe12912238dfb760f0735`
- `src/lib/graphql/manga/MangaMutation.ts`
  - blob: `96bc1482449618da74a0256a724802f942f49541`
- `src/lib/graphql/chapter/ChapterMutation.ts`
  - blob: `137e733655fd911d26e024533b8c2791afcc9219`
- `src/lib/requests/client/GraphQLClient.ts`
  - blob: `ba16cabd707a0a9c9043d75050dd9068dabccd3a`

WebUI was used to cross-check:
- `/api/graphql` endpoint construction
- Bearer access-token header behavior
- current public GraphQL operation/type/field names
- chapter page URL consumption

## License / architecture boundary

provenance_status: VERIFIED

license_status: COMPATIBLE_FOR_PROTOCOL_USE

The MPL-2.0 codebases are not copied or linked into Veil by this gateway. Veil sends
independently written GraphQL requests over HTTP to a separately running user-controlled
server. Protocol field/type names are used only to interoperate with that server.

If future work directly copies or modifies MPL-covered source files, that work must be
reviewed separately and comply with MPL-2.0 file-level source obligations.

## Security boundary

- no credentials may be embedded in the configured URL
- HTTPS is required by default
- insecure HTTP requires explicit opt-in and remains subject to Android network-security rules
- access tokens are supplied at request time through a token provider
- tokens are not part of Veil source IDs
- manga source IDs use stable `serverId + remoteSourceId`
- returned cover/page URLs must resolve to the configured Suwayomi server origin
- foreign page/cover hosts are rejected
- source filters are fail-closed until their positional protocol is explicitly mapped

## Protocol v1 exclusions

The first gateway contract intentionally does not:
- install/update extensions
- manage extension repositories
- expose WebView/challenge solving
- execute third-party extension code inside Veil
- synchronize Suwayomi's own library/progress state back into Veil
- silently map positional source filters
- discover public/shared Suwayomi servers

These can only be added as separately reviewed capabilities.
