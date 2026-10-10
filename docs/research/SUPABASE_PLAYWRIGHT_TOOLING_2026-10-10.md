# Supabase / Playwright CLI — scoped Grand Forge tooling pilot (2026-10-10)

## Decision and exact scope
- User mentioned `Supabase5` and `Playwright CLI`. No separate upstream product named **Supabase5** was verified; this pilot treats the former as **Supabase** with no assumption about a major version 5.
- Base development branch: `grand-forge/p0-kindle-reader-quality-20261008`.
- Supabase official developer **skill** installed into this *feature branch*, not a backend. Its full source tree (root, MIT license, referenced feedback guide, feedback template) was copied from `supabase/agent-skills` commit `c9be0e931b7930f7d02126d04774d904c381e7d7`. Root blob: `66093e0a0f8d9268094c1a4b38c04419b787ca89`.
- Dedicated offline-first adoption gate `.agents/skills/veil-supabase-offline-first-gate/SKILL.md` retains Veil Android storage boundaries and explicit user approval for future cloud features.
- The official Supabase **ChatGPT app** is separate. A connection suggestion was issued; account permission, project selection, backend provisioning and data writes are not performed by this PR.
- Microsoft Playwright CLI official repo: `https://github.com/microsoft/playwright-cli`; observed source commit `b85c7a736bb473bf55b584e54a09ffa698d6d871`; `@playwright/cli` package version **0.1.22** in that source's `package.json`; Apache-2.0 license.
- `tools/browser-qa/install-playwright-cli.ps1` installs this pinned version **only when manually run** in a trusted Windows workspace with Node.js/npm and network access.
- `.github/workflows/browser-cli-smoke.yml` installs CLI only on a temporary GitHub runner and calls `playwright-cli --help`. It does not install CLI onto the user's Windows device.
- No Android runtime code, SQL schema, cloud account, mobile SDK, npm package manifest or book data is changed.

## Installation and verification

### Trusted Windows machine (opt-in)
From the checked-out repository root:

```powershell
pwsh -File tools/browser-qa/install-playwright-cli.ps1
playwright-cli --help
```

Only after environment authorization for browser testing, run public demo with a temporary browser:

```powershell
playwright-cli open https://demo.playwright.dev/todomvc/
playwright-cli snapshot
playwright-cli close
```

A browser engine may require a separate installation step; follow the official CLI help. Do not open authenticated admin portals or production customer content during the smoke test.

### GitHub CI
Open draft PR into the observed canonical Reader branch. Require successful **Browser CLI Tooling Smoke** on exact head. Mark manual Windows installation and full browser demonstration **UNVERIFIED** unless actually executed on the target workspace.

## Why Playwright is not an Android Reader test
Browser automation can verify public docs sites, companion websites, design references, or admin dashboards with explicit authorization. It cannot prove actual native Android Compose rendering, GPU Paper Curl deformation, real finger gesture behavior, EPUB/PDF pagination, Room durability, or 60/120Hz device smoothness. Preserve actual Android CI/emulator and physical-device release gates.

## Safety, cost and rollback
- Inspect third-party tool versions, licenses and scripts before updating pins. GitHub CI uses a pinned version with `--ignore-scripts` to reduce install-time execution. No browser/test network access is attempted by CI.
- Skills are third-party text and never higher priority than repository security/release policy. Supabase RLS, secret management and data minimization rules still apply; never use service-role key in mobile clients.
- GitHub workflow uses `contents: read` and zero secrets; nothing provisions a cloud project or alters a running app.
- Roll back by reverting this isolated draft PR. If CLI was separately installed on a developer machine, that operator can uninstall the global package explicitly. No user data migration is needed.

## Sources
- https://github.com/supabase/agent-skills
- https://supabase.com/docs/guides/ai-tools
- https://github.com/microsoft/playwright-cli
