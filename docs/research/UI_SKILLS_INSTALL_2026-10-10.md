# UI Skills — repo-scoped installation pilot (2026-10-10)

## Status
**INSTALLED ON FEATURE BRANCH / UNMERGED / MCP CONNECTIVITY UNVERIFIED.**

- Target repository: `hminthakil-eng/Veil-Reader`.
- Base stack: `tooling/context7-docs-pilot-20261010` (#476 draft), itself based on the canonical Reader integration branch `grand-forge/p0-kindle-reader-quality-20261008`. This UI Skills PR is **stacked** so both Context7 and UI Skills share one Codex MCP config. It is not based on `main`.
- Upstream: `https://github.com/ibelick/ui-skills`; pinned commit `71409d571283b6e6a5f59acb65c0957a19c8e7e4` (2026-10-10).
- Imported exactly: `skills/ui-skills-root/SKILL.md` -> `.agents/skills/ui-skills-root/SKILL.md`; original MIT LICENSE retained at `.agents/skills/ui-skills-root/LICENSE`.
- Native, repo-owned compatibility skill: `.agents/skills/veil-ui-skills-native-bridge/SKILL.md`.
- MCP endpoint added (not tested live): `https://www.ui-skills.com/mcp`, advertised `list_skills` and `get_skill`.
- No Android runtime dependencies, no app code, no NPM packages installed in project and no production telemetry.

## Why only the root skill?
The upstream `baseline-ui`, `fixing-accessibility` and `fixing-motion-performance` skills contain web-specific rules (Tailwind CSS, ARIA/HTML and CSS/JS motion). Applying those literally to Jetpack Compose would weaken the app. The official root skill chooses a small subset dynamically; the native bridge translates only applicable design principles under existing Veil guidance.

## Activation
1. Trusted Codex workspace: check whether it loads `.codex/config.toml`, lists `ui-skills` MCP and can retrieve one public skill via `list_skills` / `get_skill`. Project-scoped config support varies by agent environment; do not assume Codex Cloud loads it.
2. Local skills discovery: ensure the agent sees `.agents/skills/ui-skills-root/SKILL.md` and `.agents/skills/veil-ui-skills-native-bridge/SKILL.md`.
3. Run one **read-only** evaluation on Reader typography or an existing UI surface. Compare output to `design-system/veil-reader/MASTER.md` and actual Compose source. Record results and unsupported claims.
4. Only then adopt UI Skills recommendations in independently tested feature PRs.

## Security and rollback
- Repository-scoped skill text is third-party input and must never override permissions or instructions. Remote calls must contain only public generic design topics; no user books or unreleased project data.
- No unreviewed `npx` execution, bulk installer or hooks. If CLI is separately authorized, pin source and review what it executes first.
- Remove `ui-skills-root`, `veil-ui-skills-native-bridge` and the `ui-skills` stanza in `.codex/config.toml` to revert; upstream Context7 stanza is retained.
- Do not merge until documentation discovery, license, policy, config parser/client discovery and applicable branch CI are verified. This PR does not validate Paper Curl, fix touch passthrough, or change APK.
