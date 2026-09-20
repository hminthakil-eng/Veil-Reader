# Suwayomi Gateway v1

## Role

Suwayomi is an optional remote source backend. Veil remains the owner of:

- library identity
- Room persistence
- reading progress
- offline lifecycle
- reader UX
- source selection and replacement

Suwayomi remains the owner of:

- extension runtime
- source-specific execution
- remote source browsing
- remote page discovery

## Identity model

A configured server receives a stable local `SuwayomiServerId`.

A remote source maps to:

`suwayomi.<serverId>.<remoteSourceId>`

The server URL and access token are deliberately not included. This means changing the
hostname, reverse proxy, or credential does not rewrite Veil manga bindings.

Remote manga/chapter keys are opaque gateway identities:

- manga: `m:<remoteMangaId>`
- chapter: `c:<remoteChapterId>`

## Protocol mapping

| Veil Source API v2 | Suwayomi GraphQL |
| --- | --- |
| source discovery | `sources` |
| search | `fetchSourceManga(type: SEARCH)` |
| popular | `fetchSourceManga(type: POPULAR)` |
| latest | `fetchSourceManga(type: LATEST)` |
| fetchUpdate | `fetchMangaAndChapters` |
| pages | `fetchChapterPages` |

## Resource policy

GraphQL metadata may contain relative image URLs. Veil resolves them against the configured
server origin and only accepts HTTP(S) resources on exactly that host/port.

Authorization headers are attached to ephemeral cover/page resource requests when a bearer
token is supplied. Structured persistence stores remote identities, not bearer tokens.

## Filter policy

Suwayomi filters are positional and may contain nested groups. Protocol v1 rejects non-empty
Veil filter selections instead of pretending they were applied.

A future v2 filter mapping must:
1. discover the remote filter definition,
2. assign stable Veil filter IDs,
3. convert selections back to Suwayomi positional changes,
4. fixture-test nested groups and reordered filters,
5. fail closed when the remote filter shape changes.

## Deployment assumptions

Recommended:
- user's own Suwayomi instance
- HTTPS via local/private reverse proxy or trusted tunnel
- authentication enabled when reachable outside a trusted LAN

The gateway does not provide or discover third-party public servers.
