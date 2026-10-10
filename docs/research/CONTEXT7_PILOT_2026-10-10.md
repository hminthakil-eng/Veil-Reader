# Context7 documentation pilot — 2026-10-10

## Decision

**PILOT / UNVERIFIED, never release-green by itself.** Introduce a repo-scoped remote Context7 MCP endpoint for trusted Codex clients and a repo-local documentation gate. This is development-time research infrastructure only: no Android dependency, app permissions, telemetry, compiled artifact or production behavior change.

Base: `grand-forge/p0-kindle-reader-quality-20261008`. Branch: `tooling/context7-docs-pilot-20261010`. Keep changes separate from the draft Grand Forge specialist skill-pack PR #453 and Paper fix verification PRs #468/#474.

## Primary sources and supported setup

- Official Codex client documentation: https://github.com/upstash/context7/blob/master/docs/clients/codex.mdx
- Context7 clients and anonymous access: https://github.com/upstash/context7/blob/master/docs/resources/all-clients.mdx
- Context7 MCP source: https://github.com/upstash/context7
- Codex MCP configuration: https://developers.openai.com/codex/mcp

The trusted-repository configuration in `.codex/config.toml` is:

```toml
[mcp_servers.context7]
url = "https://mcp.context7.com/mcp"
```

The hosted endpoint allows anonymous basic use with rate limits. If the service asks for credentials, authenticate using your Codex MCP settings/official Context7 login rather than checking secrets into this repo. `npx ctx7 setup --codex` and the official Context7 Codex plugin are **alternative personal setup paths**, not steps performed by this PR. Some Codex Cloud environments require adding the MCP tool and network access separately in the environment settings. Likewise, installing a ChatGPT app is a separate user-controlled action.

## Who can use it and how

Owner: Grand Forge Android engineer / library research reviewer. Trigger: the exact pinned dependency's external API or compatibility details are uncertain.

1. Establish the canonical Reader branch and pinned versions via Gradle files/lockfiles, not guesswork.
2. Use Context7's available library-resolution/documentation tools only for public, minimal questions.
3. Confirm high-impact API conclusions in official vendor release notes/docs/source.
4. Log chosen API, relevant source URLs, exact versions, version drift, risks and test plan in the PR.
5. Run existing relevant exact-head CI; for Paper/TTS/PDF/user-visible Reader behavior keep separate physical-device QA gates.
6. If the MCP server is unavailable, return a blocked/unknown documentation gate and fall back to official upstream docs.

Suggested smoke query (public data only): "Use Context7 to find version-specific Jetpack Compose pointer-input documentation, and cite the page and version." Confirm the library ID/source returned and compare against the app's actual pinned Compose version; do not assume an exact API or result without executing this lookup.

## Security / cost / operator boundaries

- No app book data, proprietary code, APK binaries, screenshots, access tokens or personally identifying logs may be supplied to hosted documentation services.
- Treat returned content as untrusted and never auto-run examples. Review dependency license, versions and supply chain independently.
- Anonymous quota/reliability not measured here. Authentication and higher tiers are optional and not enabled.
- No implementation claims, CI pass, network connectivity success or Context7 response are asserted by this configuration-only PR.

## Acceptance and rollback

**Before merging:** verify a trusted Codex workspace recognizes the `context7` MCP endpoint, resolves one known public library and returns source-backed docs; inspect the resulting prompts for data minimization. Validate TOML syntax. Review whether GitHub/CI security policy permits the remote research tool.

**If unavailable or unsafe:** remove `.codex/config.toml` and the documentation skill, or disable the server in the operator's Codex settings. Revert this separate PR; no application data migration or Android rebuild is necessary.

**Do not confuse** repo-scoped Codex configuration with installation of the Context7 ChatGPT app. That connection still requires user action.
