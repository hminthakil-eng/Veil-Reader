# Writing Studio Installation and License Audit — 2026-10-14

Status: **source skill files installed on Windows; shared repository worktree; Gemini CLI recognition verified from global alias**, no claim of automatic use by ChatGPT web app or Gemini 4 Argon desktop.

## Reusable source selection
1. [danjdewhurst/story-skills](https://github.com/danjdewhurst/story-skills), pinned at `415181fea2a19fdeb762df4d689652072d138f54`. 21 relevant `SKILL.md` packages selected; original MIT license retained. Excluded publishing/submission/verse rather than clutter interactive RPG writing.
2. [guantoubaozi/novel-writing](https://github.com/guantoubaozi/novel-writing), pinned at `a6db1c892547fc9230b980d9bfd95ce8b92fe73f`. 1 narrative planning/continuity skill; MIT license retained.
3. 4 original Island-specific skills: adult romance/agency, branching quests/continuity, distinctive character voice/visual novel, island economy/worldbuilding.
4. [jwynia/agent-skills](https://github.com/jwynia/agent-skills), pinned downloaded source `e02ec7e226a6e4f8419fd3b88a1d8e472d421b32`. 119 SKILL files observed, more than a substantial proportion fiction. **No root LICENSE present**. Selected conceptual diagnostic principles were rewritten as original project-specific methods; NOT copied to the shared skill folder pending legal clarification.

## Validation receipts
- Local source clones in `E:\EyadStudio\Tools\writing-skills-sources`.
- Project skills: `E:\EyadStudio\Projects\Veil-Reader-StoryStudio-20261014\.agents\skills`.
- Same 26 SKILL.md files hashed/copied to `C:\Users\Microsoft\.agents\skills` to make them available as **user-scope discovery aliases** for supported local coding agents.
- Official `@google/gemini-cli` **0.64.0** installed locally under `E:\EyadStudio\Tools\GeminiCLI`, with executable at `node_modules\.bin\gemini.cmd` (NOT a claim Gemini CLI is Gemini desktop Argon).
- Invoked `gemini.cmd skills list` **successfully**: `island-romance-agency`, `island-character-voice-room`, `novel-writing`, `story-init`, `plot-structure` and other names appeared `[Enabled]` from global alias. The workspace was marked `untrusted`, therefore CLI skipped local workspace agents; global scope alias is the observed successful discovery path. Full list command logs are remote.
- Gemini Desktop Argon is a separate application and **cannot be presumed to auto-load** CLI skill folders. Shared source documents/prompts were already sent to Gemini in an actual conversation for creative collaboration. A separate desktop integration/activation test remains open.
- ChatGPT hosted app does **not** gain new installed system tools from writing markdown to Windows; these files are usable for ChatGPT-guided work via the accessible repository and linked coding-agent workflow.
- Validator: `python tools/story_skills/verify_skills.py`, expected 26 frontmatter checks and two license copies.
- No external skill executable scripts ran during curation/install. Review all instructions carefully before activation. Repo-sourced instructions can contain untrusted text.

## Scope and narrative policy
Owner requests potential romances with adult female NPCs including married women and mothers, with complex challenge and story. Every route requires character agency and consent, distinct family commitments, and a credible friendship path. In-world rejection is valid in a playthrough; no child/minor romance; no coercive financial leverage or time-pressure gifts. Romances optional; core mystery and all reader capabilities never lock behind one.
Story draft: `docs/game-studio/ISLAND_ROMANCE_AND_CHARACTER_AGENCY_BIBLE_v0.1.md`.
Initial co-written scene: `docs/game-studio/SCENE_01_CRIMSON_THREAD_ARAGHAVAN_FA_v0.1.md`.
