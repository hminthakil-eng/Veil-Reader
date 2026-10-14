# Story Studio shared writing-skill / romance preproduction — QA receipt
**Date:** 2026-10-14 | Windows DESKTOP-GSV91KV | Branch story/shared-writing-skills-romance-v1-20261014 | No Android/Reader production modifications.

## Verified downloads and selection
- Cloned `danjdewhurst/story-skills`, pinned `415181fea2a19fdeb762df4d689652072d138f54`: 24 skills observed, **21 relevant selected**, MIT license kept.
- Cloned `guantoubaozi/novel-writing`, pinned `a6db1c892547fc9230b980d9bfd95ce8b92fe73f`: 1 selected, MIT license kept.
- Cloned `jwynia/agent-skills`, pinned `e02ec7e226a6e4f8419fd3b88a1d8e472d421b32`: 119 skills observed, but NO root LICENSE file. **No files from this repository redistributed**; original custom methods produced instead.
- Authored four Island-specific skills: romance/agency, quest branch continuity, NPC voice, world economy/lore.
- Copying **22 external skills + 4 authored = 26** into `<repo>/.agents/skills` and identical `C:\Users\Microsoft\.agents\skills` files; SHA256 verified match per skill. Retained both MIT LICENSE texts.

## Test receipts
1. `python tools/story_skills/verify_skills.py`: **PASS, 26** skill frontmatter names/descriptions and license file presence, no duplicate entries.
2. `@google/gemini-cli` official package npm installed on `E:\EyadStudio\Tools\GeminiCLI`; `gemini.cmd --version` returned `0.64.0` and exit 0.
3. Running `gemini.cmd skills list` in story worktree showed installed user-scope `C:\Users\Microsoft\.agents\skills` entries as **[Enabled]**, including `island-romance-agency`, `island-character-voice-room`, `novel-writing`, `story-init`. CLI reported the project worktree itself was **untrusted**, so it did not also scan workspace agents. Global alias recognition is the verified success.
4. Gemini Desktop (different model/runtime) is NOT guaranteed to auto-discover Gemini CLI skills. Actual story/romance consultation was submitted to Gemini 4 Argon and its independent reply was captured at `E:\EyadStudio\Temp\Gemini_Island_Writing_Romance_Response_20261014.txt`.
5. An actual scene critique and proposed rewrite by the separate Gemini Desktop session was received in **Flash-Lite**, not Argon. The model menu reported **4 Argon disabled due to a usage limit** with a displayed reset time, so no fresh Argon-specific claim is made for this scene; future Argon critique remains open. Review output saved at `E:\EyadStudio\Temp\Gemini_CrimsonThread_Review_Response_20261014.txt`.

## Co-writing and critiques
- ChatGPT authored 7 adult female NPC romance seeds including married women/mothers/older women, narrative state model and 3 quest concepts in `ISLAND_ROMANCE_AND_CHARACTER_AGENCY_BIBLE_v0.1.md`.
- ChatGPT wrote the original Persian VN scene `SCENE_01_CRIMSON_THREAD_ARAGHAVAN_FA_v0.1.md`.
- Gemini Flash-Lite independently critiqued over-exposition and suggested deeper atmospheric subtext. ChatGPT wrote a revised v0.2, adopting useful atmosphere and rejecting arbitrary punitive financial penalties for an unwanted flirtation. Neither scene is playable yet.
- Gemini 4 Argon itself challenged large unreviewed skill installs and introduced substantive route/consent/state modeling in the independent consultation. We used a curated 26-skill package rather than blindly installing 100+ repositories, with source attributions.

## Not verified / next tasks
- Hosted ChatGPT **cannot gain globally installed system tools solely from these files**; repository skills can be read as source context during tool-assisted collaboration and used by supported local agents.
- No model runtime automatic skill-activation trace, beyond Gemini CLI `skills list`.
- No playwright/mobile dialogue scene, art, Godot relationship state code, chapter gameplay, human readers, character voice blind tests or finished romance routes.
- Review any external skill instructions and scripts before activation or execution. No external skill executables were deliberately run in this installation.
- Canon/character details await owner's creative sign-off. No child romance routes; adult NPC consent & independence remain mandatory.
